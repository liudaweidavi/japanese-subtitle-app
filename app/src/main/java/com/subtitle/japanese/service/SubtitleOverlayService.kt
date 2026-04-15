package com.subtitle.japanese.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.subtitle.japanese.R
import com.subtitle.japanese.overlay.OverlayManager
import com.subtitle.japanese.pipeline.SubtitlePipeline
import com.subtitle.japanese.data.UserPreferences
import com.subtitle.japanese.ui.MainActivity
import com.subtitle.japanese.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SubtitleOverlayService : Service() {

    private val TAG = "SubtitleOverlayService"
    private var pipeline: SubtitlePipeline? = null
    private var overlayManager: OverlayManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var preferences: UserPreferences

    companion object {
        const val ACTION_STOP = "ACTION_STOP"

        var isRunning = false
            private set

        private val _subtitleFlow = MutableSharedFlow<String>(extraBufferCapacity = 32)
        val subtitleFlow: SharedFlow<String> = _subtitleFlow.asSharedFlow()
    }

    override fun onCreate() {
        super.onCreate()
        preferences = UserPreferences(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        // Handle stop action from notification
        if (intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(Constants.EXTRA_MEDIA_PROJECTION_RESULT_CODE, 0)
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.EXTRA_MEDIA_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Constants.EXTRA_MEDIA_PROJECTION_DATA)
        }

        if (resultCode == 0 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground service
        val notification = createNotification()
        startForeground(Constants.NOTIFICATION_ID, notification)

        // Load credentials from DataStore and start pipeline
        serviceScope.launch {
            val appId = preferences.baiduAppId.first()
            val secretKey = preferences.baiduSecretKey.first()

            if (appId.isBlank() || secretKey.isBlank()) {
                Log.e(TAG, "Baidu credentials not configured")
                stopSelf()
                return@launch
            }

            // Show overlay
            overlayManager = OverlayManager(this@SubtitleOverlayService)
            overlayManager?.show()

            // Setup and start pipeline
            pipeline = SubtitlePipeline(this@SubtitleOverlayService)
            pipeline?.configure(appId, secretKey, overlayManager!!)
            val started = pipeline?.start(resultCode, data) ?: false

            if (!started) {
                Log.e(TAG, "Failed to start pipeline")
                stopSelf()
                return@launch
            }

            isRunning = true
            Log.i(TAG, "Subtitle service started")

            // Forward subtitle updates via coroutine
            launch {
                pipeline?.subtitleText?.collect { text ->
                    if (text.isNotBlank()) {
                        _subtitleFlow.tryEmit(text)
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        pipeline?.destroy()
        pipeline = null
        overlayManager?.hide()
        overlayManager = null
        isRunning = false
        Log.i(TAG, "Subtitle service stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_text)
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SubtitleOverlayService::class.java).apply {
            action = "ACTION_STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_stop),
                stopPendingIntent
            )
            .setOngoing(true)
            .build()
    }
}
