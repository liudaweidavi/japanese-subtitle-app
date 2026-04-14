package com.subtitle.japanese.translation

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory LRU cache for translation results.
 * Avoids redundant API calls for repeated phrases.
 */
class TranslationCache(private val maxSize: Int = 200) {

    private val cache = object : LinkedHashMap<String, String>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(sourceText: String): String? {
        return cache[sourceText]
    }

    @Synchronized
    fun put(sourceText: String, translatedText: String) {
        cache[sourceText] = translatedText
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    @Synchronized
    fun size(): Int = cache.size
}
