package com.subtitle.japanese.data

import kotlinx.coroutines.flow.Flow

class SubtitleRepository(private val dao: SubtitleDao) {

    fun getAll(): Flow<List<SubtitleEntry>> = dao.getAll()

    fun getBySession(sessionId: String): Flow<List<SubtitleEntry>> = dao.getBySession(sessionId)

    fun search(query: String): Flow<List<SubtitleEntry>> = dao.search(query)

    suspend fun insert(entry: SubtitleEntry): Long = dao.insert(entry)

    suspend fun deleteOlderThan(beforeMs: Long): Int = dao.deleteOlderThan(beforeMs)

    suspend fun deleteAll() = dao.deleteAll()
}
