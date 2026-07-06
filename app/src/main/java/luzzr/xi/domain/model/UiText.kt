package luzzr.xi.domain.model

import android.content.Context
import androidx.annotation.StringRes
import luzzr.xi.R

/**
 * Type-safe wrapper for UI-visible text that may come from string resources or dynamic strings.
 * ViewModel outputs UiText; Composable layer resolves it to actual strings.
 */
sealed class UiText {
    data class StringResource(@StringRes val resId: Int, val args: Array<Any> = emptyArray()) : UiText()
    data class DynamicString(val value: String) : UiText()

    fun asString(context: Context): String = when (this) {
        is StringResource -> if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
        is DynamicString -> value
    }
}

/**
 * Map ThinkingLevel to resource-based display names.
 */
val ThinkingLevel.displayNameRes: Int
    get() = when (this) {
        ThinkingLevel.LOW -> R.string.thinking_low_name
        ThinkingLevel.MEDIUM -> R.string.thinking_medium_name
        ThinkingLevel.HIGH -> R.string.thinking_high_name
    }

val ThinkingLevel.descriptionRes: Int
    get() = when (this) {
        ThinkingLevel.LOW -> R.string.thinking_low_desc
        ThinkingLevel.MEDIUM -> R.string.thinking_medium_desc
        ThinkingLevel.HIGH -> R.string.thinking_high_desc
    }

/**
 * Map TranslationEngine to resource-based display names.
 */
val TranslationEngine.displayNameRes: Int
    get() = when (this) {
        TranslationEngine.AI -> R.string.engine_ai_name
        TranslationEngine.MLKIT -> R.string.engine_mlkit_name
    }

val TranslationEngine.descriptionRes: Int
    get() = when (this) {
        TranslationEngine.AI -> R.string.engine_ai_desc
        TranslationEngine.MLKIT -> R.string.engine_mlkit_desc
    }
