package luzzr.xi.data.repository

import android.content.Context
import android.util.Log
import luzzr.xi.R
import luzzr.xi.domain.model.SupportedLanguage
import com.google.mlkit.common.model.DownloadConditions
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
    private fun getMlKitCode(lang: String): String? {
        return SupportedLanguage.entries.find { it.displayName == lang }?.mlKitLangCode
    }

    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> {
        val sourceCode = getMlKitCode(sourceLang)
        val targetCode = getMlKitCode(targetLang)

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
                        fun doDownload(attempt: Int) {
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
                                    if (attempt < 1 && cont.isActive) {
                                        doDownload(attempt + 1)
                                    } else {
                                        if (!translatorClosed) {
                                            translator.close()
                                            translatorClosed = true
                                        }
                                        if (cont.isActive) cont.resume(Result.failure(e))
                                    }
                                }
                        }
                        doDownload(0)

                        cont.invokeOnCancellation {
                            if (!translatorClosed) {
                                translator.close()
                                translatorClosed = true
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Result.failure(Exception(context.getString(R.string.error_mlkit_timeout)))
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

    fun isLanguageSupported(lang: String): Boolean = getMlKitCode(lang) != null
}
