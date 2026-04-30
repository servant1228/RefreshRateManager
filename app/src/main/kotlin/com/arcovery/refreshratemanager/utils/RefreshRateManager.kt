package com.arcovery.refreshratemanager.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.WindowManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.DataOutputStream

class RefreshRateManager(private val context: Context) {

    companion object {
        private const val TAG = "RefreshRateManager"
        // SurfaceFlinger binder transaction codes
        private const val SF_CODE_SET_ACTIVE_CONFIG = 1035
        private const val SF_CODE_FRAME_RATE_FLEXIBILITY = 1036
    }

    /**
     * 检查 Root 权限是否可用
     */
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo ok"))
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val output = reader.readLine()
            process.waitFor()
            output == "ok" && process.exitValue() == 0
        } catch (e: Exception) {
            Log.d(TAG, "Root not available: ${e.message}")
            false
        }
    }

    /**
     * 检测当前使用的 Root 方案
     * @return "Magisk", "KernelSU", "APatch", 或 "SU"
     */
    fun getRootSolution(): String {
        return try {
            when {
                // KernelSU: 检查内核模块
                java.io.File("/sys/module/kernelsu").exists() -> "KernelSU"
                // APatch: 检查 apatchd 服务
                Runtime.getRuntime().exec(arrayOf("which", "apatchd")).waitFor() == 0 -> "APatch"
                // Magisk: 检查 magisk 二进制
                Runtime.getRuntime().exec(arrayOf("which", "magisk")).waitFor() == 0 -> "Magisk"
                // 尝试获取 su 的版本信息
                else -> {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                    val result = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream)).readLine()
                    process.waitFor()
                    when {
                        result?.contains("uid=0") == true -> "SU"
                        else -> "SU"
                    }
                }
            }
        } catch (e: Exception) {
            "SU"
        }
    }

    /**
     * 自适应检测设备支持的所有刷新率档位
     */
    fun getSupportedRefreshRates(): List<Int> {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            display?.supportedModes
                ?.map { it.refreshRate.toInt() }
                ?.distinct()
                ?.sortedDescending()
                ?: listOf(60)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get supported refresh rates", e)
            listOf(60)
        }
    }

    /**
     * 获取当前刷新率
     */
    fun getCurrentRefreshRate(): Float {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            display?.refreshRate ?: 60f
        } catch (e: Exception) {
            60f
        }
    }

    /**
     * 获取实时渲染帧率（FPS）
     * 使用 Choreographer 监控实际渲染帧率，会有动态起伏
     */
    fun getRealtimeFps(): Flow<Float> = callbackFlow {
        val choreographer = Choreographer.getInstance()
        var lastFrameTimeNanos = 0L
        var frameCount = 0
        var lastFpsUpdateTime = System.currentTimeMillis()
        var currentFps = 0f

        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameTimeNanos > 0) {
                    val frameDurationNanos = frameTimeNanos - lastFrameTimeNanos
                    if (frameDurationNanos > 0 && frameDurationNanos < 1_000_000_000L) {
                        frameCount++
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastFpsUpdateTime >= 500) {
                            val durationSeconds = (currentTime - lastFpsUpdateTime) / 1000.0
                            currentFps = (frameCount / durationSeconds).toFloat()
                            trySend(currentFps)
                            frameCount = 0
                            lastFpsUpdateTime = currentTime
                        }
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
                choreographer.postFrameCallback(this)
            }
        }

        choreographer.postFrameCallback(frameCallback)

        awaitClose {
            choreographer.removeFrameCallback(frameCallback)
        }
    }

    /**
     * 从 supportedModes 中找到对应 Hz 的精确 refreshRate 浮点值和 modeId
     * 返回 Pair(精确Hz字符串, modeId)
     */
    private fun findModeInfo(hz: Int): Pair<String, Int>? {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val modes = display?.supportedModes ?: return null
            val mode = modes.minByOrNull { Math.abs(it.refreshRate - hz) } ?: return null
            Pair(mode.refreshRate.toString(), mode.modeId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find mode info for $hz Hz", e)
            null
        }
    }

    fun setRefreshRate(hz: Int, useRoot: Boolean, useShizuku: Boolean) {
        when {
            useRoot -> setRefreshRateRoot(hz)
            useShizuku -> setRefreshRateShizuku(hz)
            else -> setRefreshRateRoot(hz)
        }
    }

    /**
     * Root 模式 - SurfaceFlinger backdoor 原理
     *
     * 核心逻辑：
     * 1. code 1036 acquire frame rate flexibility token（Android 11+ 需要先解除帧率限制）
     * 2. code 1035 setActiveConfig，传入 modeIndex（从 0 开始，= modeId - 1）
     * 3. code 1036 release token
     * 4. settings put 全方位覆盖（防止系统自动回调）
     */
    private fun setRefreshRateRoot(hz: Int) {
        val modeInfo = findModeInfo(hz)
        val exactHz = modeInfo?.first ?: hz.toFloat().toString()
        val modeId = modeInfo?.second ?: 1
        // SurfaceFlinger 的 modeIndex 从 0 开始，modeId 从 1 开始
        val sfIndex = (modeId - 1).toString()

        val commands = mutableListOf<String>()

        // Step 1: Android 11+ 需要先 acquire frame rate flexibility token
        // 这样 SurfaceFlinger 才会接受强制切换，不被系统帧率策略覆盖
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            commands.add("service call SurfaceFlinger $SF_CODE_FRAME_RATE_FLEXIBILITY i32 1")
        }

        // Step 2: SurfaceFlinger backdoor - 强制设置 active display config
        commands.add("service call SurfaceFlinger $SF_CODE_SET_ACTIVE_CONFIG i32 $sfIndex")

        // Step 3: Release token（让系统帧率策略重新生效，但已经切换完成）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            commands.add("service call SurfaceFlinger $SF_CODE_FRAME_RATE_FLEXIBILITY i32 0")
        }

        // Step 4: settings put 全方位覆盖，防止系统在下次策略刷新时回调
        commands.add("settings put system peak_refresh_rate $exactHz")
        commands.add("settings put system min_refresh_rate $exactHz")
        commands.add("settings put secure peak_refresh_rate $exactHz")
        commands.add("settings put secure min_refresh_rate $exactHz")
        commands.add("settings put global peak_refresh_rate $exactHz")
        commands.add("settings put global min_refresh_rate $exactHz")

        // MIUI / HyperOS 专用
        commands.add("settings put system miui_refresh_rate $hz")
        commands.add("settings put secure miui_refresh_rate $hz")
        commands.add("settings put system screen_refresh_rate_setting $hz")

        // cmd display 接口（Android 12+）
        commands.add("cmd display set-user-preferred-display-mode 0 0 $exactHz 2>/dev/null || true")

        executeAsRoot(commands.toTypedArray())
        Log.d(TAG, "Set refresh rate to ${hz}Hz (exact=$exactHz, modeId=$modeId, sfIndex=$sfIndex)")
    }

    /**
     * Shizuku 模式 - 通过 Shizuku UserService 执行 ADB 命令
     * Shizuku 运行在 shell 权限下，可以执行 service call 和 settings 命令
     */
    private fun setRefreshRateShizuku(hz: Int) {
        val modeInfo = findModeInfo(hz)
        val exactHz = modeInfo?.first ?: hz.toFloat().toString()
        val modeId = modeInfo?.second ?: 1
        val sfIndex = (modeId - 1).toString()

        val commands = mutableListOf<String>()

        // Step 1: Acquire frame rate flexibility token (Android 11+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            commands.add("service call SurfaceFlinger $SF_CODE_FRAME_RATE_FLEXIBILITY i32 1")
        }

        // Step 2: SurfaceFlinger - force set active display config
        commands.add("service call SurfaceFlinger $SF_CODE_SET_ACTIVE_CONFIG i32 $sfIndex")

        // Step 3: Release token
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            commands.add("service call SurfaceFlinger $SF_CODE_FRAME_RATE_FLEXIBILITY i32 0")
        }

        // Step 4: settings put 全方位覆盖
        commands.add("settings put system peak_refresh_rate $exactHz")
        commands.add("settings put system min_refresh_rate $exactHz")
        commands.add("settings put secure peak_refresh_rate $exactHz")
        commands.add("settings put secure min_refresh_rate $exactHz")
        commands.add("settings put global peak_refresh_rate $exactHz")
        commands.add("settings put global min_refresh_rate $exactHz")

        // MIUI / HyperOS 专用
        commands.add("settings put system miui_refresh_rate $hz")
        commands.add("settings put secure miui_refresh_rate $hz")
        commands.add("settings put system screen_refresh_rate_setting $hz")

        // cmd display 接口（Android 12+）
        commands.add("cmd display set-user-preferred-display-mode 0 0 $exactHz 2>/dev/null || true")

        try {
            commands.forEach { cmd ->
                val result = ShizukuHelper.execCommand(cmd)
                Log.d(TAG, "Shizuku exec '$cmd' result: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku execution failed", e)
        }
        Log.d(TAG, "Set refresh rate to ${hz}Hz via Shizuku (exact=$exactHz, modeId=$modeId, sfIndex=$sfIndex)")
    }

    private fun executeAsRoot(commands: Array<String>) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (command in commands) {
                os.writeBytes(command + "\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            os.close()
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute commands as root", e)
        }
    }
}
