package com.subtitle.japanese.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat

object PermissionHelper {

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, Constants.OVERLAY_PERMISSION_REQUEST_CODE)
    }

    fun hasRecordAudioPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestRecordAudioPermission(activity: Activity, launcher: ActivityResultLauncher<String>) {
        launcher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(
        activity: Activity,
        launcher: ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun getMediaProjectionManager(context: Context): MediaProjectionManager {
        return context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    fun requestMediaProjection(
        activity: Activity,
        launcher: ActivityResultLauncher<Intent>
    ) {
        val manager = getMediaProjectionManager(activity)
        launcher.launch(manager.createScreenCaptureIntent())
    }

    fun hasAllPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) && hasRecordAudioPermission(context)
    }
}
