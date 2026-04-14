package com.subtitle.japanese.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleDao {

    @Insert
    suspend fun insert(entry: SubtitleEntry): Long

    @Query("SELECT * FROM subtitle_entries ORDER BY timestampMs DESC")
    fun getAll(): Flow<List<SubtitleEntry>>

    @Query("SELECT * FROM subtitle_entries WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getBySession(sessionId: String): Flow<List<SubtitleEntry>>

    @Query("SELECT * FROM subtitle_entries WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestampMs DESC")
    fun search(query: String): Flow<List<SubtitleEntry>>

    @Query("DELETE FROM subtitle_entries WHERE timestampMs < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long): Int

    @Query("DELETE FROM subtitle_entries")
    suspend fun deleteAll()
}
