package luzzr.xi.data.repository
import luzzr.xi.domain.model.*

import android.content.Context
import luzzr.xi.data.cache.TranslationCache
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.domain.model.ChatMessage
import luzzr.xi.domain.model.ChatRequest
import luzzr.xi.domain.model.ResponseFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import luzzr.xi.domain.model.AppError
import luzzr.xi.domain.repository.TranslationGateway
import luzzr.xi.domain.prompt.PromptBuilder

@Singleton
class TranslationRepository @Inject constructor(
    @ApplicationContext context: Context,
    settingsDataStore: SettingsDataStore,
    apiProvider: ApiProvider,
    networkCheck: NetworkCheck,
    private val translationCache: TranslationCache,
    private val jsonParser: JsonResponseParser
) : ApiRepository(context, settingsDataStore, apiProvider, networkCheck), TranslationGateway {

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
        reasoningEffort: String?
    ): Result<TranslationResult> {
        val s = settingsDataStore.settings.first()

        translationCache.get(text, sourceLang, targetLang, s.model, s.providerId)?.let {
            return Result.success(it)
        }

        return callWithRetry {
            val prompt = PromptBuilder.buildTranslatePrompt(sourceLang, targetLang, text)
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
                    val parsed = jsonParser.parseTranslation(context, content)
                    parsed.map { result ->
                        result.usage = response.usage
                        translationCache.put(text, sourceLang, targetLang, s.model, s.providerId, result)
                        result
                    }
                }
            }
        }
    }

    override fun streamTranslate(
        text: String,
        sourceLang: String,
        targetLang: String,
        reasoningEffort: String?
    ): kotlinx.coroutines.flow.Flow<Result<TranslationResult>> = kotlinx.coroutines.flow.flow {
        val s = settingsDataStore.settings.first()
        
        val cached = translationCache.get(text, sourceLang, targetLang, s.model, s.providerId)
        if (cached != null) {
            emit(Result.success(cached))
            return@flow
        }

        val accumulatedRaw = StringBuilder()
        streamWithRetry(maxRetries = 0) {
            val prompt = PromptBuilder.buildTranslatePrompt(sourceLang, targetLang, text)
            val request = ChatRequest(
                model = s.model,
                messages = listOf(ChatMessage(role = "user", content = prompt)),
                reasoningEffort = reasoningEffort,
                responseFormat = ResponseFormat.json(),
                stream = true,
                streamOptions = StreamOptions(includeUsage = true)
            )
            getApi().streamChatCompletions(request)
        }.collect { chunkResult ->
            if (chunkResult.isSuccess) {
                val chunk = chunkResult.getOrNull()
                val deltaContent = chunk?.choices?.firstOrNull()?.delta?.content
                if (deltaContent != null) {
                    accumulatedRaw.append(deltaContent)
                    val partialText = extractPartialTranslation(accumulatedRaw.toString())
                    val currentUsage = chunk.usage
                    emit(Result.success(TranslationResult(translation = partialText, usage = currentUsage)))
                } else if (chunk?.usage != null) {
                    val partialText = extractPartialTranslation(accumulatedRaw.toString())
                    emit(Result.success(TranslationResult(translation = partialText, usage = chunk.usage)))
                }
            } else {
                emit(Result.failure(chunkResult.exceptionOrNull() ?: Exception("Unknown stream error")))
            }
        }
        
        val parsed = jsonParser.parseTranslation(context, accumulatedRaw.toString())
        if (parsed.isSuccess) {
            val fullResult = parsed.getOrNull() ?: return@flow
            translationCache.put(text, sourceLang, targetLang, s.model, s.providerId, fullResult)
            emit(Result.success(fullResult))
        }
    }

    private fun extractPartialTranslation(json: String): String {
        val key = "\"translation\":"
        val idx = json.indexOf(key)
        if (idx == -1) return ""
        val afterKey = json.substring(idx + key.length).trimStart()
        if (afterKey.startsWith("\"")) {
            val start = 1
            var end = start
            while (end < afterKey.length) {
                if (afterKey[end] == '\\') {
                    end += 2
                    if (end > afterKey.length) break
                    continue
                }
                if (afterKey[end] == '"') {
                    break
                }
                end++
            }
            val rawStr = afterKey.substring(start, minOf(end, afterKey.length))
            val unicodePattern = Regex("""\\u([0-9a-fA-F]{4})""")
            return unicodePattern.replace(
                rawStr.replace("\\\\", "\\")
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\/", "/")
            ) { match ->
                match.groupValues[1].toInt(16).toChar().toString()
            }
        }
        return ""
    }

}
