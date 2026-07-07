package luzzr.xi.domain.model

/**
 * Supported languages for translation.
 * All OpenCode Go models (GLM, Kimi, MiMo, MiniMax, Qwen, DeepSeek)
 * natively support these languages with high quality.
 */
enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val mlKitLangCode: String? = null
) {
    AUTO("auto", "Auto Detect", "自动检测", null),
    CHINESE("zh", "Chinese", "中文", "zh"),
    ENGLISH("en", "English", "English", "en"),
    JAPANESE("ja", "Japanese", "日本語", "ja"),
    KOREAN("ko", "Korean", "한국어", "ko"),
    SPANISH("es", "Spanish", "Español", "es"),
    FRENCH("fr", "French", "Français", "fr"),
    GERMAN("de", "German", "Deutsch", "de"),
    ITALIAN("it", "Italian", "Italiano", "it"),
    PORTUGUESE("pt", "Portuguese", "Português", "pt"),
    RUSSIAN("ru", "Russian", "Русский", "ru"),
    ARABIC("ar", "Arabic", "العربية", "ar"),
    HINDI("hi", "Hindi", "हिन्दी", "hi"),
    THAI("th", "Thai", "ไทย", "th"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt", "vi"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "id"),
    TURKISH("tr", "Turkish", "Türkçe", "tr"),
    DUTCH("nl", "Dutch", "Nederlands", "nl"),
    POLISH("pl", "Polish", "Polski", "pl"),
    SWEDISH("sv", "Swedish", "Svenska", "sv"),
    UKRAINIAN("uk", "Ukrainian", "Українська", "uk");

    companion object {
        fun getByCode(code: String): SupportedLanguage {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}
