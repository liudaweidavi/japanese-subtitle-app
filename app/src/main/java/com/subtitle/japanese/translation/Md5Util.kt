package com.subtitle.japanese.translation

import java.security.MessageDigest

object Md5Util {
    fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
