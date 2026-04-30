package com.arcovery.refreshratemanager.utils

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.arcovery.refreshratemanager.IShellService
import com.arcovery.refreshratemanager.service.ShellService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    private const val REQUEST_CODE = 1001

    private val _isBinderAlive = MutableStateFlow(false)
    val isBinderAlive: StateFlow<Boolean> = _isBinderAlive

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected

    private var shellService: IShellService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Binder received")
        _isBinderAlive.value = true
        checkPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.i(TAG, "Binder dead")
        _isBinderAlive.value = false
        _isServiceConnected.value = false
        shellService = null
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                Log.i(TAG, "Permission result: $granted")
                _isPermissionGranted.value = granted
                if (granted) {
                    bindService()
                }
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "Service connected")
            shellService = IShellService.Stub.asInterface(service)
            _isServiceConnected.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "Service disconnected")
            shellService = null
            _isServiceConnected.value = false
        }
    }

    fun init() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun release() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        unbindService()
    }

    fun checkPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                _isPermissionGranted.value = false
                return false
            }
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            _isPermissionGranted.value = granted
            granted
        } catch (e: Exception) {
            Log.e(TAG, "Check permission failed", e)
            _isPermissionGranted.value = false
            false
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE)
            } else {
                _isPermissionGranted.value = true
                bindService()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request permission failed", e)
        }
    }

    fun bindService() {
        try {
            val args = UserServiceArgs(ComponentName("com.arcovery.refreshratemanager", ShellService::class.java.name))
                .daemon(false)
                .processNameSuffix("shell")
                .debuggable(true)
                .tag("shell_service")
                .version(1)
            Shizuku.bindUserService(args, serviceConnection)
        } catch (e: Exception) {
            Log.e(TAG, "Bind service failed", e)
        }
    }

    private fun unbindService() {
        try {
            val args = UserServiceArgs(ComponentName("com.arcovery.refreshratemanager", ShellService::class.java.name))
                .tag("shell_service")
            Shizuku.unbindUserService(args, serviceConnection, true)
        } catch (e: Exception) {
            Log.e(TAG, "Unbind service failed", e)
        }
    }

    fun execCommand(command: String): Int {
        return try {
            shellService?.execCommand(command) ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "Exec command failed: $command", e)
            -1
        }
    }

    fun execCommandWithOutput(command: String): String {
        return try {
            shellService?.execCommandWithOutput(command) ?: "error: service not connected"
        } catch (e: Exception) {
            Log.e(TAG, "Exec command failed: $command", e)
            "error: ${e.message}"
        }
    }
}
