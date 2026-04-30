package com.arcovery.refreshratemanager

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import com.arcovery.refreshratemanager.utils.RefreshRateManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class LSPModule : IXposedHookLoadPackage {
    private val handler = Handler(Looper.getMainLooper())
    private var currentPkg: String? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.arcovery.refreshratemanager") return

        try {
            hookDisplayManager(lpparam)
        } catch (e: Exception) {
            XposedBridge.log("RefreshRateManager: Hook failed: ${e.message}")
        }
    }

    private fun hookDisplayManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        val displayManagerClass = XposedHelpers.findClass(
            "android.hardware.display.DisplayManager",
            lpparam.classLoader
        )

        XposedHelpers.findAndHookMethod(
            displayManagerClass,
            "getDisplay",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val display = param.result as? Display ?: return
                    val pkg = getForegroundApp() ?: return

                    val config = loadAppConfig(pkg)
                    if (config != null) {
                        setDisplayRefreshRate(display.displayId, config.refreshRate)
                    }
                }
            }
        )

        startForegroundChecker()
    }

    private fun startForegroundChecker() {
        handler.post(object : Runnable {
            override fun run() {
                try {
                    val pkg = getForegroundApp()
                    if (pkg != null && pkg != currentPkg) {
                        currentPkg = pkg
                        val config = loadAppConfig(pkg)
                        if (config != null) {
                            applyRefreshRate(config.refreshRate)
                        }
                    }
                } catch (e: Exception) {
                    XposedBridge.log("RefreshRateManager: Check failed: ${e.message}")
                }
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun getForegroundApp(): String? {
        val activityManager = XposedHelpers.getStaticObjectField(
            ActivityManager::class.java,
            "IActivityManagerSingleton"
        ) as? Any ?: return null

        return try {
            val runningTasks = XposedHelpers.callMethod(
                activityManager,
                "getTasks",
                Integer.MAX_VALUE
            ) as List<*>
            runningTasks.firstOrNull()?.let {
                XposedHelpers.getObjectField(it, "topActivity")?.let { act ->
                    act.javaClass.getDeclaredField("mPackageName").get(act) as? String
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadAppConfig(pkg: String): AppRefreshConfig? {
        val prefs = XposedHelpers.getStaticObjectField(
            XposedHelpers.findClass("android.app.ActivityThread", null),
            "sSystemContext"
        ) as? Context ?: return null

        val sp = prefs.getSharedPreferences("refresh_rate_config", Context.MODE_PRIVATE)
        val rate = sp.getInt("app_$pkg", 0)
        return if (rate > 0) AppRefreshConfig(rate) else null
    }

    private fun applyRefreshRate(refreshRate: Int) {
        try {
            val displayManager = XposedHelpers.getStaticObjectField(
                XposedHelpers.findClass("android.app.DisplayManagerGlobal", null),
                "sInstance"
            ) as? Any ?: return

            val displays = XposedHelpers.callMethod(displayManager, "getDisplays") as Array<*>
            for (display in displays) {
                setDisplayRefreshRate(
                    XposedHelpers.getIntField(display, "mDisplayId"),
                    refreshRate
                )
            }
        } catch (e: Exception) {
            XposedBridge.log("RefreshRateManager: Apply failed: ${e.message}")
        }
    }

    private fun setDisplayDisplayMode(displayId: Int, refreshRate: Int) {
        try {
            val displayManager = XposedHelpers.getStaticObjectField(
                XposedHelpers.findClass("android.app.DisplayManagerGlobal", null),
                "sInstance"
            ) as? Any ?: return

            val displays = XposedHelpers.callMethod(displayManager, "getDisplays") as Array<*>
            for (display in displays) {
                if (XposedHelpers.getIntField(display, "mDisplayId") == displayId) {
                    val modes = XposedHelpers.callMethod(display, "getSupportedModes") as Array<*>
                    val targetMode = modes.filter {
                        val refresh = XposedHelpers.getFloatField(it, "refreshRate")
                        (refresh - refreshRate).let { diff -> diff > -1 && diff < 1 }
                    }.maxByOrNull {
                        XposedHelpers.getFloatField(it, "refreshRate")
                    }
                    if (targetMode != null) {
                        XposedHelpers.callMethod(displayManager, "setDisplayMode", displayId, targetMode)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            XposedBridge.log("RefreshRateManager: setDisplayMode failed: ${e.message}")
        }
    }

    private fun setDisplayRefreshRate(displayId: Int, refreshRate: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val surfaceFlinger = Class.forName("android.os.SurfaceControl\$Transaction")
                val transaction = XposedHelpers.callStaticMethod(surfaceFlinger, "getInstance")
                val displayTokenClass = Class.forName("android.view.SurfaceControl\$DisplayToken")
                val displays = XposedHelpers.callMethod(
                    XposedHelpers.getStaticObjectField(
                        XposedHelpers.findClass("android.os.SurfaceControl", null),
                        "sGlobal"
                    ),
                    "getDisplays"
                ) as List<*>
                for (display in displays) {
                    val token = XposedHelpers.getObjectField(display, "token")
                    if (token != null) {
                        val modes = XposedHelpers.callMethod(display, "getSupportedModes") as Array<*>
                        val targetMode = modes.find {
                            val rate = XposedHelpers.getFloatField(it, "refreshRate")
                            kotlin.math.abs(rate - refreshRate) < 1
                        }
                        if (targetMode != null) {
                            XposedHelpers.callMethod(transaction, "setDisplayMode", token, targetMode)
                            XposedHelpers.callMethod(transaction, "apply")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                XposedBridge.log("RefreshRateManager: R+ setRefreshRate failed: ${e.message}")
                setDisplayDisplayMode(displayId, refreshRate)
            }
        }
    }

    data class AppRefreshConfig(val refreshRate: Int)
}