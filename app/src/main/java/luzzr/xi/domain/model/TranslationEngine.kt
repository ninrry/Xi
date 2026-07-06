package luzzr.xi.domain.model

enum class TranslationEngine(
    val id: String,
    val displayName: String,
    val description: String
) {
    AI("ai", "AI", "AI translation"),
    MLKIT("mlkit", "MLKit", "Offline translation");

    companion object {
        fun fromId(id: String): TranslationEngine = entries.find { it.id == id } ?: MLKIT
    }
}
