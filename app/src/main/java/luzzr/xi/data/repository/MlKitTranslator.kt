package luzzr.xi.data.repository

import android.content.Context
import android.util.Log
import luzzr.xi.R
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MlKitTranslator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val languageMap = mapOf(
        "Chinese" to TranslateLanguage.CHINESE,
        "English" to TranslateLanguage.ENGLISH,
        "Japanese" to TranslateLanguage.JAPANESE,
        "Korean" to TranslateLanguage.KOREAN,
        "Spanish" to TranslateLanguage.SPANISH,
        "French" to TranslateLanguage.FRENCH,
        "German" to TranslateLanguage.GERMAN,
        "Italian" to TranslateLanguage.ITALIAN,
        "Portuguese" to TranslateLanguage.PORTUGUESE,
        "Russian" to TranslateLanguage.RUSSIAN,
        "Arabic" to TranslateLanguage.ARABIC,
        "Hindi" to TranslateLanguage.HINDI,
        "Thai" to TranslateLanguage.THAI,
        "Vietnamese" to TranslateLanguage.VIETNAMESE,
        "Indonesian" to TranslateLanguage.INDONESIAN,
        "Turkish" to TranslateLanguage.TURKISH,
        "Dutch" to TranslateLanguage.DUTCH,
        "Polish" to TranslateLanguage.POLISH,
        "Swedish" to TranslateLanguage.SWEDISH,
        "Ukrainian" to TranslateLanguage.UKRAINIAN
    )

    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> {
        val sourceCode = languageMap[sourceLang]
        val targetCode = languageMap[targetLang]

        if (sourceCode == null || targetCode == null) {
            return Result.failure(Exception("Unsupported language: $sourceLang -> $targetLang"))
        }

        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()

            val translator = Translation.getClient(options)
            var translatorClosed = false

            val conditions = DownloadConditions.Builder()
                .build()

            try {
                withTimeout(25_000L) {
                    suspendCancellableCoroutine { cont ->
                        translator.downloadModelIfNeeded(conditions)
                            .addOnSuccessListener {
                                translator.translate(text)
                                    .addOnSuccessListener { result ->
                                        if (!translatorClosed) {
                                            translator.close()
                                            translatorClosed = true
                                        }
                                        if (cont.isActive) cont.resume(Result.success(result))
                                    }
                                    .addOnFailureListener { e ->
                                        if (!translatorClosed) {
                                            translator.close()
                                            translatorClosed = true
                                        }
                                        if (cont.isActive) cont.resume(Result.failure(e))
                                    }
                            }
                            .addOnFailureListener { e ->
                                if (!translatorClosed) {
                                    translator.close()
                                    translatorClosed = true
                                }
                                if (cont.isActive) cont.resume(Result.failure(e))
                            }

                        cont.invokeOnCancellation {
                            if (!translatorClosed) {
                                translator.close()
                                translatorClosed = true
                            }
                        }
                    }
                }
            } finally {
                if (!translatorClosed) {
                    translator.close()
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(Exception(context.getString(R.string.error_mlkit_timeout)))
        } catch (e: Exception) {
            Log.e("MlKitTranslator", "Translation failed", e)
            Result.failure(e)
        }
    }

    fun isLanguageSupported(lang: String): Boolean = languageMap.containsKey(lang)
}
