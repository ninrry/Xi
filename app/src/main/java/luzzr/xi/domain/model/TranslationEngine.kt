package luzzr.xi.domain.model

enum class TranslationEngine(
    val id: String,
    val displayName: String,
    val description: String
) {
    AI("ai", "AI 翻译", "使用 AI 模型翻译，质量更高"),
    MLKIT("mlkit", "极速翻译", "本地离线翻译，速度极快");

    companion object {
        fun fromId(id: String): TranslationEngine = entries.find { it.id == id } ?: AI
    }
}
