package com.hse.visualriskassessor.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    companion object {
        val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission(launcher: ActivityResultLauncher<String>) {
        launcher.launch(CAMERA_PERMISSION)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun shouldShowRationale(permission: String, activity: androidx.activity.ComponentActivity): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }
}
