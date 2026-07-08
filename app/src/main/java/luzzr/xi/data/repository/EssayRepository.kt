package luzzr.xi.data.repository

import android.content.Context
import android.net.Uri
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.domain.model.ChatMessage
import luzzr.xi.domain.model.ChatRequest
import luzzr.xi.domain.model.ContentPart
import luzzr.xi.domain.model.EssayCorrectionJson
import luzzr.xi.domain.model.ImageUrl
import luzzr.xi.domain.model.ResponseFormat
import luzzr.xi.domain.model.ThinkingLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import luzzr.xi.domain.model.AppError
import luzzr.xi.domain.model.UiText
import luzzr.xi.R
import luzzr.xi.domain.model.CorrectionResult
import luzzr.xi.domain.repository.EssayGateway
import luzzr.xi.domain.prompt.PromptBuilder

@Singleton
class EssayRepository @Inject constructor(
    @ApplicationContext context: Context,
    settingsDataStore: SettingsDataStore,
    apiProvider: ApiProvider,
    networkCheck: NetworkCheck,
    private val mediaProcessor: MediaProcessor,
    private val jsonParser: JsonResponseParser
) : ApiRepository(context, settingsDataStore, apiProvider, networkCheck), EssayGateway {

    override suspend fun correctFromText(text: String, reasoningEffort: String?): Result<CorrectionResult> =
        correctEssay(text, reasoningEffort)

    override suspend fun correctFromImage(imageUriString: String, reasoningEffort: String?): Result<CorrectionResult> =
        correctEssayFromImage(Uri.parse(imageUriString), reasoningEffort)

    override suspend fun correctFromPdf(pdfUriString: String, reasoningEffort: String?): Result<CorrectionResult> {
        val pdfUri = Uri.parse(pdfUriString)
        val base64Pages = mediaProcessor.renderPdfPagesAsBase64(pdfUri)
        if (base64Pages.isEmpty()) {
            return Result.failure(AppError.UnknownError(Exception("Unable to read PDF")))
        }
        return correctEssayFromBase64Images(base64Pages, reasoningEffort)
    }

    private suspend fun executeCorrection(request: ChatRequest): Result<CorrectionResult> {
        val response = getApi().chatCompletions(request)
        if (response.error != null) {
            return Result.failure(AppError.ApiError(UiText.DynamicString(response.error.message ?: "Unknown API error")))
        }
        val rawContent = response.choices?.firstOrNull()?.message?.content
        if (rawContent == null || rawContent !is String) {
            return Result.failure(AppError.EmptyResultError(UiText.StringResource(luzzr.xi.R.string.error_essay_result_empty)))
        }
        val parsed = withContext(Dispatchers.Default) {
            jsonParser.parseEssayCorrection(rawContent)
        }
        return parsed.map { json -> 
            json.usage = response.usage
            buildCorrectionResult(json) 
        }
    }

    suspend fun correctEssay(
        essay: String,
        reasoningEffort: String? = "high"
    ): Result<CorrectionResult> {
        val s = settingsDataStore.settings.first()
        val thinkingLevel = ThinkingLevel.fromId(reasoningEffort ?: "high")
        val prompt = PromptBuilder.buildEssayTextPrompt(essay, thinkingLevel)
        val request = ChatRequest(
            model = s.model,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            temperature = 0.2,
            maxTokens = 8192,
            reasoningEffort = reasoningEffort,
            responseFormat = ResponseFormat.json()
        )
        return callWithRetry(preloadedSettings = s) {
            executeCorrection(request)
        }
    }

    suspend fun correctEssayFromImage(
        imageUri: Uri,
        reasoningEffort: String? = "high"
    ): Result<CorrectionResult> {
        val s = settingsDataStore.settings.first()
        val base64Image = mediaProcessor.imageUriToBase64(imageUri)
            ?: return Result.failure(AppError.UnknownError(Exception("Failed to read image")))

        val thinkingLevel = ThinkingLevel.fromId(reasoningEffort ?: "high")
        val prompt = PromptBuilder.buildEssayImagePrompt(thinkingLevel)
        val contentParts = listOf(
            ContentPart(type = "text", text = prompt),
            ContentPart(type = "image_url", imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64Image"))
        )
        val request = ChatRequest(
            model = s.model,
            messages = listOf(ChatMessage(role = "user", content = contentParts)),
            temperature = 0.2,
            maxTokens = 8192,
            reasoningEffort = reasoningEffort,
            responseFormat = ResponseFormat.json()
        )
        return callWithRetry(preloadedSettings = s) {
            executeCorrection(request)
        }
    }



    suspend fun correctEssayFromBase64Images(
        base64Images: List<String>,
        reasoningEffort: String? = "high"
    ): Result<CorrectionResult> {
        if (base64Images.isEmpty()) {
            return Result.failure(AppError.UnknownError(Exception("No images provided")))
        }

        val s = settingsDataStore.settings.first()
        val contentParts = mutableListOf<ContentPart>()
        val thinkingLevel = ThinkingLevel.fromId(reasoningEffort ?: "high")
        contentParts.add(ContentPart(type = "text", text = PromptBuilder.buildEssayMultiImagePrompt(thinkingLevel, base64Images.size)))

        base64Images.forEach { base64 ->
            contentParts.add(ContentPart(
                type = "image_url",
                imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64")
            ))
        }

        val request = ChatRequest(
            model = s.model,
            messages = listOf(ChatMessage(role = "user", content = contentParts)),
            temperature = 0.2,
            maxTokens = 8192,
            reasoningEffort = reasoningEffort,
            responseFormat = ResponseFormat.json()
        )
        return callWithRetry(preloadedSettings = s) {
            executeCorrection(request)
        }
    }

    // --- Result builder ---

    private fun buildCorrectionResult(json: EssayCorrectionJson): CorrectionResult {
        val safeScore = json.score?.let { score ->
            val g = score.grammar.coerceIn(0, 25)
            val v = score.vocabulary.coerceIn(0, 25)
            val s = score.structure.coerceIn(0, 25)
            val st = score.style.coerceIn(0, 25)
            score.copy(
                grammar = g,
                vocabulary = v,
                structure = s,
                style = st,
                total = g + v + s + st
            )
        }
        return CorrectionResult(
            grammarErrors = json.grammarErrors ?: emptyList(),
            vocabulary = json.vocabulary ?: emptyList(),
            structure = json.structure,
            style = json.style,
            score = safeScore,
            correctedEssay = json.correctedEssay ?: "",
            writingTips = json.writingTips ?: emptyList(),
            usage = json.usage
        )
    }
}
