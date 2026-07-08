package luzzr.xi.domain.repository

interface LocalTranslationGateway {
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String>
}
