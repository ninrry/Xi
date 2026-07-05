package luzzr.xi.data.repository

import luzzr.xi.domain.model.*

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import luzzr.xi.R
import luzzr.xi.domain.model.EssayCorrectionJson
import luzzr.xi.domain.model.TranslationResult
import luzzr.xi.domain.model.AppError

object JsonResponseParser {

    private val gson = Gson()

    fun parseTranslation(context: Context, raw: String): Result<TranslationResult> {
        return try {
            val cleaned = cleanJsonResponse(raw)
            val result = gson.fromJson(cleaned, TranslationResult::class.java)
            when {
                result == null -> Result.failure(
                    AppError.ParseError(context.getString(R.string.error_parse_failed), raw)
                )
                result.translation?.isBlank() ?: true -> Result.failure(
                    AppError.ParseError(context.getString(R.string.error_translation_empty), raw)
                )
                else -> Result.success(result)
            }
        } catch (e: JsonSyntaxException) {
            Result.failure(AppError.ParseError(context.getString(R.string.error_invalid_json), raw))
        } catch (e: Exception) {
            Result.failure(AppError.ParseError(e.message ?: "Parse error", raw))
        }
    }

    fun parseEssayCorrection(context: Context, raw: String): Result<EssayCorrectionJson> {
        return try {
            val cleaned = cleanJsonResponse(raw)
            val result = gson.fromJson(cleaned, EssayCorrectionJson::class.java)
            when {
                result == null -> Result.failure(
                    AppError.ParseError(context.getString(R.string.error_parse_failed), raw)
                )
                result.error != null -> Result.failure(
                    AppError.ParseError(mapAiError(context, result.error))
                )
                result.correctedEssay.isNullOrBlank() -> Result.failure(
                    AppError.ParseError(context.getString(R.string.error_parse_failed), raw)
                )
                else -> Result.success(result)
            }
        } catch (e: JsonSyntaxException) {
            Result.failure(AppError.ParseError(context.getString(R.string.error_invalid_json), raw))
        } catch (e: Exception) {
            Result.failure(AppError.ParseError(e.message ?: "Parse error", raw))
        }
    }

    private val jsonCodeFenceRegex =
        """^[\s`]*```?(?:json)?\s*([\s\S]*?)\s*```?[\s`]*$""".toRegex(RegexOption.IGNORE_CASE)

    private fun cleanJsonResponse(raw: String): String {
        return jsonCodeFenceRegex.find(raw.trim())?.groupValues?.getOrNull(1)?.trim() ?: raw.trim()
    }

    private fun mapAiError(context: Context, aiError: String): String {
        return when {
            aiError.contains("not English", ignoreCase = true) ||
            aiError.contains("非英文", ignoreCase = true) ->
                context.getString(R.string.error_ai_not_english)
            aiError.contains("unreadable", ignoreCase = true) ||
            aiError.contains("blurry", ignoreCase = true) ||
            aiError.contains("Unable to read", ignoreCase = true) ->
                context.getString(R.string.error_ai_unreadable_image)
            else -> context.getString(R.string.error_ai_generic, aiError)
        }
    }
}
