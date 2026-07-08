package luzzr.xi.data.repository

import android.util.Log
import luzzr.xi.domain.model.AppError
import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.UiText
import luzzr.xi.domain.repository.LocalTranslationGateway
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MlKitTranslator @Inject constructor() : LocalTranslationGateway {

    private val translators = ConcurrentHashMap<String, Translator>()

    private val languageCodeCache = SupportedLanguage.entries.associateBy { it.displayName }

    private fun langKey(sourceCode: String, targetCode: String) = "$sourceCode->$targetCode"

    private fun getMlKitCode(lang: String): String? {
        return languageCodeCache[lang]?.mlKitLangCode
    }

    private fun getOrCreateTranslator(sourceCode: String, targetCode: String): Translator {
        val key = langKey(sourceCode, targetCode)
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()
            Translation.getClient(options)
        }
    }

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> {
        val sourceCode = getMlKitCode(sourceLang)
        val targetCode = getMlKitCode(targetLang)

        if (sourceCode == null || targetCode == null) {
            return Result.failure(Exception("Unsupported language: $sourceLang -> $targetLang"))
        }

        val translator = getOrCreateTranslator(sourceCode, targetCode)

        return try {
            val conditions = DownloadConditions.Builder().build()

            withTimeout(120_000L) {
                suspendCancellableCoroutine { cont ->
                    fun doDownload(attempt: Int) {
                        translator.downloadModelIfNeeded(conditions)
                            .addOnSuccessListener {
                                translator.translate(text)
                                    .addOnSuccessListener { res ->
                                        if (cont.isActive) cont.resume(Result.success(res))
                                    }
                                    .addOnFailureListener { e ->
                                        if (cont.isActive) cont.resume(Result.failure(e))
                                    }
                            }
                            .addOnFailureListener { e ->
                                if (attempt < 1 && cont.isActive) {
                                    doDownload(attempt + 1)
                                } else {
                                    if (cont.isActive) cont.resume(Result.failure(e))
                                }
                            }
                    }
                    doDownload(0)

                    cont.invokeOnCancellation {
                        val key = langKey(sourceCode, targetCode)
                        val removed = translators.remove(key)
                        removed?.close()
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val key = langKey(sourceCode, targetCode)
            val removed = translators.remove(key)
            removed?.close()
            Result.failure(AppError.GenericError(UiText.StringResource(luzzr.xi.R.string.error_mlkit_timeout)))
        } catch (e: kotlinx.coroutines.CancellationException) {
            val key = langKey(sourceCode, targetCode)
            val removed = translators.remove(key)
            removed?.close()
            throw e
        } catch (e: Exception) {
            Log.e("MlKitTranslator", "Translation failed", e)
            val key = langKey(sourceCode, targetCode)
            val removed = translators.remove(key)
            removed?.close()
            Result.failure(e)
        }
    }

    fun isLanguageSupported(lang: String): Boolean = getMlKitCode(lang) != null
}
