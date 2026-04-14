package com.subtitle.japanese.translation

import android.util.Log
import com.subtitle.japanese.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Baidu Translation API client.
 * Translates Japanese text to Chinese.
 * sign = MD5(appid + query + salt + secretKey)
 */
class BaiduTranslator(
    private val appId: String,
    private val secretKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val cache = TranslationCache()

    companion object {
        private const val TAG = "BaiduTranslator"
        private const val API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate"
    }

    suspend fun translate(japaneseText: String): TranslationResult? {
        if (japaneseText.isBlank()) return null
        if (appId.isBlank() || secretKey.isBlank()) {
            Log.w(TAG, "Baidu API credentials not configured")
            return null
        }

        // Check cache first
        val cached = cache.get(japaneseText)
        if (cached != null) {
            return TranslationResult(
                sourceText = japaneseText,
                translatedText = cached,
                from = Constants.BAIDU_TRANSLATE_FROM,
                to = Constants.BAIDU_TRANSLATE_TO
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val salt = System.currentTimeMillis().toString()
                val sign = Md5Util.md5("$appId$japaneseText$salt$secretKey")

                val formBody = FormBody.Builder()
                    .add("q", japaneseText)
                    .add("from", Constants.BAIDU_TRANSLATE_FROM)
                    .add("to", Constants.BAIDU_TRANSLATE_TO)
                    .add("appid", appId)
                    .add("salt", salt)
                    .add("sign", sign)
                    .build()

                val request = Request.Builder()
                    .url(API_URL)
                    .post(formBody)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext null

                if (!response.isSuccessful) {
                    Log.e(TAG, "Translation API error: ${response.code}")
                    return@withContext null
                }

                val json = JSONObject(body)

                // Check for error code
                if (json.has("error_code")) {
                    val errorCode = json.getString("error_code")
                    Log.e(TAG, "Baidu API error: $errorCode - ${json.optString("error_msg", "")}")
                    return@withContext null
                }

                val transResult = json.optJSONArray("trans_result") ?: return@withContext null
                val translatedText = StringBuilder()

                for (i in 0 until transResult.length()) {
                    val item = transResult.getJSONObject(i)
                    translatedText.append(item.optString("dst", ""))
                    if (i < transResult.length() - 1) {
                        translatedText.append("\n")
                    }
                }

                val result = translatedText.toString().trim()
                if (result.isNotBlank()) {
                    cache.put(japaneseText, result)
                }

                TranslationResult(
                    sourceText = japaneseText,
                    translatedText = result,
                    from = Constants.BAIDU_TRANSLATE_FROM,
                    to = Constants.BAIDU_TRANSLATE_TO
                )
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed: ${e.message}")
                null
            }
        }
    }
}
