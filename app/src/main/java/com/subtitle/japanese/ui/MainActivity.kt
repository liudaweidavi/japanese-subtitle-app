package com.subtitle.japanese.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.subtitle.japanese.databinding.ActivityMainBinding
import com.subtitle.japanese.service.SubtitleOverlayService
import com.subtitle.japanese.util.Constants
import com.subtitle.japanese.util.PermissionHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mediaProjectionResultCode: Int = 0
    private var mediaProjectionData: Intent? = null

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "需要录音权限才能使用字幕功能", Toast.LENGTH_LONG).show()
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Notification permission is optional
    }

    private val requestMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            mediaProjectionResultCode = result.resultCode
            mediaProjectionData = result.data
            startSubtitleService()
        } else {
            Toast.makeText(this, "需要屏幕录制权限来捕获音频", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        checkPermissions()
    }

    private fun setupViews() {
        binding.btnStart.setOnClickListener { onStartClicked() }
        binding.btnStop.setOnClickListener { onStopClicked() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Observe subtitle updates from service
        lifecycleScope.launch {
            SubtitleOverlayService.subtitleFlow.collect { text ->
                binding.tvCurrentSubtitle.text = text
            }
        }
    }

    private fun checkPermissions() {
        if (!PermissionHelper.hasOverlayPermission(this)) {
            PermissionHelper.requestOverlayPermission(this)
        }
        if (!PermissionHelper.hasRecordAudioPermission(this)) {
            PermissionHelper.requestRecordAudioPermission(this, requestAudioPermission)
        }
        if (!PermissionHelper.hasNotificationPermission(this)) {
            PermissionHelper.requestNotificationPermission(this, requestNotificationPermission)
        }
    }

    private fun onStartClicked() {
        if (!PermissionHelper.hasAllPermissions(this)) {
            checkPermissions()
            return
        }

        if (mediaProjectionData != null) {
            startSubtitleService()
        } else {
            PermissionHelper.requestMediaProjection(this, requestMediaProjection)
        }
    }

    private fun startSubtitleService() {
        val data = mediaProjectionData ?: return

        val serviceIntent = Intent(this, SubtitleOverlayService::class.java).apply {
            putExtra(Constants.EXTRA_MEDIA_PROJECTION_RESULT_CODE, mediaProjectionResultCode)
            putExtra(Constants.EXTRA_MEDIA_PROJECTION_DATA, data)
        }

        startForegroundService(serviceIntent)

        binding.btnStart.isEnabled = false
        binding.btnStop.isEnabled = true
        binding.tvStatus.text = getString(com.subtitle.japanese.R.string.status_running)
    }

    private fun onStopClicked() {
        stopService(Intent(this, SubtitleOverlayService::class.java))

        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false
        binding.tvStatus.text = getString(com.subtitle.japanese.R.string.status_stopped)
    }

    override fun onResume() {
        super.onResume()
        val isRunning = SubtitleOverlayService.isRunning
        binding.btnStart.isEnabled = !isRunning
        binding.btnStop.isEnabled = isRunning
        binding.tvStatus.text = if (isRunning) {
            getString(com.subtitle.japanese.R.string.status_running)
        } else {
            getString(com.subtitle.japanese.R.string.status_ready)
        }
    }
}
