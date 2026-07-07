# Retrofit — keep interface and method signatures
-keep,allowobfuscation interface luzzr.xi.core.network.OpenAiApi
-keepclassmembers interface luzzr.xi.core.network.OpenAiApi {
    *;
}

# Gson — keep all serialized model classes
-keep class luzzr.xi.domain.model.** { *; }
-keep class luzzr.xi.domain.model.CorrectionResult { *; }
-keep class luzzr.xi.data.cache.** { *; }

# ML Kit — only keep what's accessed
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.translate.Translator { *; }
-keep class com.google.mlkit.translate.TranslatorOptions { *; }
-keep class com.google.mlkit.translate.TranslateLanguage { *; }
-keep class com.google.mlkit.common.modeldownload.RemoteModelManager { *; }

# Hilt — allow obfuscation of internal classes
-dontwarn dagger.hilt.**
-keep class dagger.hilt.android.internal.lifecycle.** { *; }

# OkHttp — not needed, R8 handles it
-dontwarn okhttp3.**
-dontwarn okio.**

# Google Play Services
-dontwarn com.google.android.gms.**
