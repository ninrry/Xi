package luzzr.xi.domain.model

/**
 * Supported languages for translation.
 * All OpenCode Go models (GLM, Kimi, MiMo, MiniMax, Qwen, DeepSeek)
 * natively support these languages with high quality.
 */
enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    CHINESE("zh", "Chinese", "中文"),
    ENGLISH("en", "English", "English"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    ITALIAN("it", "Italian", "Italiano"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    RUSSIAN("ru", "Russian", "Русский"),
    ARABIC("ar", "Arabic", "العربية"),
    HINDI("hi", "Hindi", "हिन्दी"),
    THAI("th", "Thai", "ไทย"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    TURKISH("tr", "Turkish", "Türkçe"),
    DUTCH("nl", "Dutch", "Nederlands"),
    POLISH("pl", "Polish", "Polski"),
    SWEDISH("sv", "Swedish", "Svenska"),
    UKRAINIAN("uk", "Ukrainian", "Українська");

    companion object {
        fun getByCode(code: String): SupportedLanguage {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}
