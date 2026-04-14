package com.subtitle.japanese.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subtitle_entries")
data class SubtitleEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String = "ja",
    val targetLang: String = "zh",
    val timestampMs: Long = System.currentTimeMillis(),
    val sessionId: String = ""
)
