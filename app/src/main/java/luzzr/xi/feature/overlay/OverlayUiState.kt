package luzzr.xi.feature.overlay

import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.domain.model.UiText

data class OverlayUiState(
    val isPanelVisible: Boolean = false,
    val inputText: String = "",
    val resultText: String = "",
    val isTranslating: Boolean = false,
    val isModelDownloading: Boolean = false,
    val errorMsg: UiText? = null,
    val sourceLang: SupportedLanguage = SupportedLanguage.ENGLISH,
    val targetLang: SupportedLanguage = SupportedLanguage.CHINESE,
    val thinkingLevel: luzzr.xi.domain.model.ThinkingLevel = luzzr.xi.domain.model.ThinkingLevel.LOW,
    val engine: TranslationEngine = TranslationEngine.MLKIT,
    val usage: luzzr.xi.domain.model.Usage? = null
)
