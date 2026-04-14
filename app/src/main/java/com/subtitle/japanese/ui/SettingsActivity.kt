package com.subtitle.japanese.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.subtitle.japanese.data.UserPreferences
import com.subtitle.japanese.databinding.ActivitySettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = UserPreferences(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupViews()
        loadSettings()
    }

    private fun setupViews() {
        // Model selection
        val models = arrayOf("ggml-tiny.bin (75MB, 快速)", "ggml-base.ja.bin (150MB, 精准)")
        val modelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, models)
        binding.spinnerModel.adapter = modelAdapter

        // Font size
        val fontSizes = arrayOf("14", "16", "18", "20", "22", "24")
        val fontAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fontSizes)
        binding.spinnerFontSize.adapter = fontAdapter

        // Overlay position
        val positions = arrayOf("底部", "顶部")
        val positionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, positions)
        binding.spinnerPosition.adapter = positionAdapter

        // Save button
        binding.btnSave.setOnClickListener { saveSettings() }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            preferences.baiduAppId.first().let { binding.etBaiduAppId.setText(it) }
        }
        lifecycleScope.launch {
            preferences.baiduSecretKey.first().let { binding.etBaiduSecretKey.setText(it) }
        }
        lifecycleScope.launch {
            val model = preferences.modelName.first()
            val modelIndex = if (model.contains("base")) 1 else 0
            binding.spinnerModel.setSelection(modelIndex)
        }
        lifecycleScope.launch {
            val fontSize = preferences.fontSize.first()
            val fontIndex = listOf("14", "16", "18", "20", "22", "24").indexOf(fontSize)
            if (fontIndex >= 0) binding.spinnerFontSize.setSelection(fontIndex)
        }
        lifecycleScope.launch {
            val position = preferences.overlayPosition.first()
            val posIndex = if (position == "top") 1 else 0
            binding.spinnerPosition.setSelection(posIndex)
        }
    }

    private fun saveSettings() {
        val appId = binding.etBaiduAppId.text.toString()
        val secretKey = binding.etBaiduSecretKey.text.toString()

        lifecycleScope.launch {
            preferences.saveBaiduCredentials(appId, secretKey)
            preferences.saveFontSize(binding.spinnerFontSize.selectedItem.toString())
            preferences.saveOverlayPosition(
                if (binding.spinnerPosition.selectedItemPosition == 0) "bottom" else "top"
            )
            preferences.saveModelName(
                if (binding.spinnerModel.selectedItemPosition == 0) "ggml-tiny.bin" else "ggml-base.ja.bin"
            )
            Toast.makeText(this@SettingsActivity, "设置已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
