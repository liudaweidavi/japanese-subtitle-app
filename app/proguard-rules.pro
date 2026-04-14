# Whisper native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI bridge class
-keep class com.subtitle.japanese.whisper.WhisperJni { *; }

# Room entities
-keep class com.subtitle.japanese.data.SubtitleEntry { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class com.subtitle.japanese.translation.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
