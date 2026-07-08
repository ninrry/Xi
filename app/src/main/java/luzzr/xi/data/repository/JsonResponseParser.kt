package luzzr.xi.data.repository

import luzzr.xi.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonResponseParser @Inject constructor() {

    private val gson = Gson()

    fun parseTranslation(raw: String): Result<TranslationResult> {
        return try {
            val cleaned = cleanJsonResponse(raw)
            val result = gson.fromJson(cleaned, TranslationResult::class.java)
            when {
                result == null -> Result.failure(
                    AppError.ParseError(UiText.StringResource(luzzr.xi.R.string.error_parse_failed), raw)
                )
                result.translation?.isBlank() ?: true -> Result.failure(
                    AppError.ParseError(UiText.StringResource(luzzr.xi.R.string.error_translate_result_empty), raw)
                )
                else -> Result.success(result)
            }
        } catch (e: JsonSyntaxException) {
            Result.failure(AppError.ParseError(UiText.StringResource(luzzr.xi.R.string.error_invalid_json), raw))
        } catch (e: Exception) {
            Result.failure(AppError.ParseError(UiText.DynamicString(e.message ?: "Parse error"), raw))
        }
    }

    fun parseEssayCorrection(raw: String): Result<EssayCorrectionJson> {
        return try {
            val cleaned = cleanJsonResponse(raw)
            val result = gson.fromJson(cleaned, EssayCorrectionJson::class.java)
            when {
                result == null -> Result.failure(
                    AppError.ParseError(UiText.StringResource(luzzr.xi.R.string.error_parse_failed), raw)
                )
                result.error != null -> Result.failure(
                    AppError.ParseError(mapAiError(result.error))
                )
                result.correctedEssay.isNullOrBlank() -> Result.failure(
                    AppError.ParseError(UiText.StringResource(luzzr.xi.R.string.error_parse_failed), raw)
                )
                else -> Result.success(sanitizeEssayCorrection(result))
            }
        } catch (e: JsonSyntaxException) {
            Result.failure(AppError.ParseError(UiText.StringResource(luzzr.xi.R.string.error_invalid_json), raw))
        } catch (e: Exception) {
            Result.failure(AppError.ParseError(UiText.DynamicString(e.message ?: "Parse error"), raw))
        }
    }

    private val jsonCodeFenceRegex =
        """^[\s`]*```?(?:json)?\s*([\s\S]*?)\s*```?[\s`]*$""".toRegex(RegexOption.IGNORE_CASE)

    private fun cleanJsonResponse(raw: String): String {
        return jsonCodeFenceRegex.find(raw.trim())?.groupValues?.getOrNull(1)?.trim() ?: raw.trim()
    }

    private fun sanitizeEssayCorrection(result: EssayCorrectionJson): EssayCorrectionJson {
        val cleanGrammar = result.grammarErrors?.filter { g ->
            g.original as? String != null && g.corrected as? String != null && g.explanation as? String != null
        }
        val cleanVocab = result.vocabulary?.filter { v ->
            v.original as? String != null && v.suggested as? String != null && v.reason as? String != null
        }
        return result.copy(grammarErrors = cleanGrammar, vocabulary = cleanVocab)
    }

    private fun mapAiError(aiError: String): UiText {
        return when {
            aiError.contains("not English", ignoreCase = true) ||
            aiError.contains("非英文", ignoreCase = true) ->
                UiText.StringResource(luzzr.xi.R.string.error_ai_not_english)
            aiError.contains("unreadable", ignoreCase = true) ||
            aiError.contains("blurry", ignoreCase = true) ||
            aiError.contains("Unable to read", ignoreCase = true) ->
                UiText.StringResource(luzzr.xi.R.string.error_ai_unreadable_image)
            else -> UiText.StringResource(luzzr.xi.R.string.error_ai_generic, arrayOf(aiError))
        }
    }
}
