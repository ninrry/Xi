package luzzr.xi.data.repository
import luzzr.xi.domain.model.*

import android.content.Context
import luzzr.xi.data.cache.TranslationCache
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.ChatMessage
import luzzr.xi.domain.model.ChatRequest
import luzzr.xi.domain.model.ResponseFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import luzzr.xi.domain.model.AppError

@Singleton
class TranslationRepository @Inject constructor(
    @ApplicationContext context: Context,
    settingsDataStore: SettingsDataStore,
    private val translationCache: TranslationCache
) : ApiRepository(context, settingsDataStore) {

    suspend fun translate(
        text: String,
        sourceLang: String = "English",
        targetLang: String = "Chinese",
        reasoningEffort: String? = null
    ): Result<String> {
        val s = settingsDataStore.settings.first()

        translationCache.get(text, sourceLang, targetLang, s.model)?.let {
            return Result.success(it)
        }

        return callWithRetry {
            val prompt = buildTranslatePrompt(sourceLang, targetLang, text)
            val request = ChatRequest(
                model = s.model,
                messages = listOf(
                    ChatMessage(role = "user", content = prompt)
                ),
                reasoningEffort = reasoningEffort,
                responseFormat = ResponseFormat.json()
            )
            val response = getApi().chatCompletions(request)
            if (response.error != null) {
                Result.failure(AppError.ApiError(response.error.message ?: "Unknown API error"))
            } else {
                val rawContent = response.choices?.firstOrNull()?.message?.content
                if (rawContent == null || rawContent !is String) {
                    Result.failure(AppError.EmptyResultError(
                        context.getString(luzzr.xi.R.string.error_translate_result_empty)
                    ))
                } else {
                    val content: String = rawContent
                    val parsed = JsonResponseParser.parseTranslation(context, content)
                    parsed.onSuccess { result ->
                        result.translation?.let { translation ->
                            translationCache.put(text, sourceLang, targetLang, s.model, translation)
                        }
                    }
                    parsed.map { it.translation ?: "" }
                }
            }
        }
    }

    private fun buildTranslatePrompt(
        sourceLang: String,
        targetLang: String,
        text: String
    ): String = buildString {
        appendLine("You are a professional translator. Translate the following text from $sourceLang to $targetLang.")
        appendLine()
        appendLine("Rules:")
        appendLine("- Output ONLY valid JSON, no markdown, no explanations outside JSON")
        appendLine("- Preserve original formatting (line breaks, paragraphs)")
        appendLine("- Keep code/technical terms untranslated")
        appendLine("- Provide up to 3 alternative translations if ambiguous")
        appendLine()
        appendLine("Response format (strict JSON):")
        appendLine("""{"translation": "your translation here", "detected_language": "detected source language", "alternatives": ["alt1", "alt2"]}""")
        appendLine()
        appendLine("Text to translate:")
        appendLine(text)
    }
}
