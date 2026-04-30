package com.arcovery.refreshratemanager.service

import android.util.Log
import androidx.annotation.Keep
import com.arcovery.refreshratemanager.IShellService
import java.io.BufferedReader
import java.io.InputStreamReader

@Keep
class ShellService : IShellService.Stub() {

    companion object {
        private const val TAG = "ShellService"
    }

    init {
        Log.i(TAG, "ShellService created")
    }

    override fun destroy() {
        Log.i(TAG, "ShellService destroy")
        System.exit(0)
    }

    override fun execCommand(command: String): Int {
        return try {
            Log.d(TAG, "Executing: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            -1
        }
    }

    override fun execCommandWithOutput(command: String): String {
        return try {
            Log.d(TAG, "Executing with output: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            reader.close()
            output.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            "error: ${e.message}"
        }
    }
}
