package luzzr.xi.feature.overlay

import luzzr.xi.domain.model.SupportedLanguage
import luzzr.xi.domain.model.TranslationEngine

data class OverlayUiState(
    val isPanelVisible: Boolean = false,
    val inputText: String = "",
    val resultText: String = "",
    val isTranslating: Boolean = false,
    val errorMsg: String? = null,
    val sourceLang: SupportedLanguage = SupportedLanguage.ENGLISH,
    val targetLang: SupportedLanguage = SupportedLanguage.CHINESE,
    val engine: TranslationEngine = TranslationEngine.AI
)
