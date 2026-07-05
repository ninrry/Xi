package luzzr.xi.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import luzzr.xi.core.datastore.SettingsDataStore
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
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import luzzr.xi.domain.model.AppError
import luzzr.xi.R

data class CorrectionResult(
    val corrections: String,
    val correctedEssay: String,
    val overallScore: String,
    val writingTips: String,
    val grammarErrorCount: Int = 0,
    val vocabularyCount: Int = 0,
    val totalScore: Int = 0,
    val grade: String = ""
)

@Singleton
class EssayRepository @Inject constructor(
    @ApplicationContext context: Context,
    settingsDataStore: SettingsDataStore
) : ApiRepository(context, settingsDataStore) {

    private suspend fun executeCorrection(request: ChatRequest): Result<CorrectionResult> {
        val response = getApi().chatCompletions(request)
        if (response.error != null) {
            return Result.failure(AppError.ApiError(response.error.message ?: "Unknown API error"))
        }
        val rawContent = response.choices?.firstOrNull()?.message?.content
        if (rawContent == null || rawContent !is String) {
            return Result.failure(AppError.EmptyResultError(context.getString(R.string.error_essay_result_empty)))
        }
        val parsed = JsonResponseParser.parseEssayCorrection(context, rawContent)
        return parsed.map { json -> buildCorrectionResult(json) }
    }

    suspend fun correctEssay(
        essay: String,
        reasoningEffort: String? = "high"
    ) = callWithRetry {
        val s = settingsDataStore.settings.first()
        val depthInstruction = getDepthInstruction(reasoningEffort)
        val prompt = buildCorrectionPrompt(essay, depthInstruction)
        val request = ChatRequest(
            model = s.model,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            temperature = 0.2,
            maxTokens = 8192,
            reasoningEffort = reasoningEffort,
            responseFormat = ResponseFormat.json()
        )
        executeCorrection(request)
    }

    suspend fun correctEssayFromImage(
        imageUri: Uri,
        reasoningEffort: String? = "high"
    ) = callWithRetry {
        val s = settingsDataStore.settings.first()
        val base64Image = uriToBase64(imageUri)
            ?: return@callWithRetry Result.failure(AppError.UnknownError(Exception("Failed to read image")))

        val depthInstruction = getDepthInstruction(reasoningEffort)
        val prompt = buildImageCorrectionPrompt(1, depthInstruction)
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
        executeCorrection(request)
    }

    suspend fun correctEssayFromImages(
        imageUris: List<Uri>,
        reasoningEffort: String? = "high"
    ) = callWithRetry {
        if (imageUris.isEmpty()) {
            return@callWithRetry Result.failure(AppError.UnknownError(Exception("No images provided")))
        }

        val s = settingsDataStore.settings.first()
        val contentParts = mutableListOf<ContentPart>()
        val depthInstruction = getDepthInstruction(reasoningEffort)
        contentParts.add(ContentPart(type = "text", text = buildMultiImagePrompt(imageUris.size, depthInstruction)))

        imageUris.forEachIndexed { _, uri ->
            val base64 = uriToBase64(uri)
                ?: return@callWithRetry Result.failure(AppError.UnknownError(Exception("Failed to read image")))
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
        executeCorrection(request)
    }

    suspend fun correctEssayFromBase64Images(
        base64Images: List<String>,
        reasoningEffort: String? = "high"
    ) = callWithRetry {
        if (base64Images.isEmpty()) {
            return@callWithRetry Result.failure(AppError.UnknownError(Exception("No images provided")))
        }

        val s = settingsDataStore.settings.first()
        val contentParts = mutableListOf<ContentPart>()
        val depthInstruction = getDepthInstruction(reasoningEffort)
        contentParts.add(ContentPart(type = "text", text = buildMultiImagePrompt(base64Images.size, depthInstruction)))

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
        executeCorrection(request)
    }

    // --- Depth instruction by thinking level ---

    private fun getDepthInstruction(effort: String?): String {
        val level = ThinkingLevel.fromId(effort ?: "high")
        return when (level) {
            ThinkingLevel.LOW ->
                "Provide a concise correction focusing only on major grammar errors. " +
                "Limit vocabulary suggestions to the top 3. Keep analysis brief."
            ThinkingLevel.MEDIUM ->
                "Provide standard correction with grammar, vocabulary, and brief structure/style notes."
            ThinkingLevel.HIGH ->
                "Provide comprehensive professor-level analysis with every grammar error explained, " +
                "nuanced vocabulary suggestions, deep structural critique, style assessment, " +
                "detailed score justification, and 3-5 personalized writing tips."
        }
    }

    // --- Image / PDF processing ---

    suspend fun uriToBase64(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = try {
                BitmapFactory.decodeStream(inputStream)
            } finally {
                try { inputStream.close() } catch (_: Exception) { Log.w("EssayRepo", "Failed to close input stream") }
            }
            if (original == null) return@withContext null

            val maxSize = 1024
            val scaled = if (original.width > maxSize || original.height > maxSize) {
                val ratio = maxSize.toFloat() / maxOf(original.width, original.height)
                Bitmap.createScaledBitmap(original,
                    (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
            } else {
                original
            }

            if (scaled !== original) {
                original.recycle()
            }

            val baos = ByteArrayOutputStream()
            val quality = getOptimalQuality(scaled.byteCount)
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val result = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            scaled.recycle()

            result
        } catch (e: Exception) {
            Log.e("EssayRepo", "Failed to convert URI to base64", e)
            null
        }
    }

    private fun getOptimalQuality(byteCount: Int): Int = when {
        byteCount < 500_000 -> 95
        byteCount < 1_500_000 -> 85
        else -> 75
    }

    // --- Prompt builders ---

    private fun buildCorrectionPrompt(essay: String, depthInstruction: String): String = buildString {
        appendLine("You are a senior English writing professor with 20 years of experience.")
        appendLine()
        appendLine("Task: Thoroughly correct and improve the following English essay.")
        appendLine()
        appendLine(depthInstruction)
        appendLine()
        appendLine("Scoring rubric (0-25 per dimension):")
        appendLine("  23-25: Excellent, near-native")
        appendLine("  18-22: Good, minor issues")
        appendLine("  13-17: Adequate, frequent errors")
        appendLine("  8-12: Limited, hard to follow")
        appendLine("  0-7: Very poor")
        appendLine()
        appendLine("Output ONLY valid JSON in this exact structure:")
        appendLine("""{""")
        appendLine("""  "grammar_errors": [""")
        appendLine("""    {"line": 1, "original": "...", "corrected": "...", "type": "tense|agreement|article|preposition|punctuation|other", "explanation": "中文说明"}""")
        appendLine("""  ],""")
        appendLine("""  "vocabulary": [""")
        appendLine("""    {"original": "...", "suggested": "...", "reason": "中文说明", "register": "academic|formal|informal"}""")
        appendLine("""  ],""")
        appendLine("""  "structure": {""")
        appendLine("""    "organization": "中文分析", "transitions": "中文分析", "logical_flow": "中文分析" """)
        appendLine("""  },""")
        appendLine("""  "style": {""")
        appendLine("""    "sentence_variety": "中文分析", "tone": "中文分析", "conciseness": "中文分析", "academic_register": "中文分析" """)
        appendLine("""  },""")
        appendLine("""  "score": {""")
        appendLine("""    "grammar": 20, "vocabulary": 18, "structure": 22, "style": 19, "total": 79, "grade": "B+" """)
        appendLine("""  },""")
        appendLine("""  "corrected_essay": "完整修正后的作文",""")
        appendLine("""  "writing_tips": ["中文建议1", "中文建议2", "中文建议3"] """)
        appendLine("""}""")
        appendLine()
        appendLine("If the input is not English, set \"error\" to \"Input is not English\" and leave other fields empty.")
        appendLine()
        appendLine("Essay to correct:")
        appendLine(essay)
    }

    private fun buildImageCorrectionPrompt(imageCount: Int, depthInstruction: String): String = buildString {
        appendLine("You are a senior English writing professor. The user has uploaded $imageCount image(s) of an English essay.")
        appendLine()
        appendLine("STEP 1: Carefully read ALL text from the image(s). Transcribe accurately.")
        appendLine("STEP 2: Correct and improve the essay following the JSON format below.")
        appendLine()
        appendLine(depthInstruction)
        appendLine()
        appendLine("If any image is unreadable, blurry, or contains no English text, set \"error\" to \"Unable to read image\" and leave other fields empty.")
        appendLine()
        appendLine("Output ONLY valid JSON in the same structure as text correction.")
        appendLine("""{""")
        appendLine("""  "grammar_errors": [...], "vocabulary": [...], "structure": {...}, "style": {...}, """)
        appendLine("""  "score": {"grammar":0,"vocabulary":0,"structure":0,"style":0,"total":0,"grade":"F"}, """)
        appendLine("""  "corrected_essay": "...", "writing_tips": [...] """)
        appendLine("""}""")
    }

    private fun buildMultiImagePrompt(imageCount: Int, depthInstruction: String): String = buildString {
        appendLine("You are a senior English writing professor. The user has uploaded $imageCount image(s) of an English essay.")
        appendLine()
        appendLine("These images are multiple pages of the same essay. Read ALL pages carefully.")
        appendLine("STEP 1: Read and combine ALL text from all $imageCount images in order.")
        appendLine("STEP 2: Correct and improve the combined essay following the JSON format below.")
        appendLine()
        appendLine(depthInstruction)
        appendLine()
        appendLine("If any image is unreadable, blurry, or contains no English text, set \"error\" to \"Unable to read image\" and leave other fields empty.")
        appendLine()
        appendLine("Output ONLY valid JSON in the same structure as text correction.")
    }

    // --- Result builder ---

    private fun buildCorrectionResult(json: EssayCorrectionJson): CorrectionResult {
        val corrections = buildString {
            val grammarErrors = json.grammarErrors
            if (!grammarErrors.isNullOrEmpty()) {
                appendLine(context.getString(R.string.essay_grammar_correction))
                grammarErrors.forEach { error ->
                    val lineInfo = if (error.line != null) "Line ${error.line}: " else ""
                    appendLine("  - ${lineInfo}\"${error.original}\" -> \"${error.corrected}\" | ${error.explanation}")
                }
                appendLine()
            }

            val vocab = json.vocabulary
            if (!vocab.isNullOrEmpty()) {
                appendLine(context.getString(R.string.essay_vocabulary_optimization))
                vocab.forEach { v ->
                    appendLine("  - \"${v.original}\" -> \"${v.suggested}\" | ${v.reason}")
                }
                appendLine()
            }

            val structure = json.structure
            if (structure != null) {
                val hasContent = listOfNotNull(structure.organization, structure.transitions, structure.logicalFlow).any { it.isNotBlank() }
                if (hasContent) {
                    appendLine(context.getString(R.string.essay_structure_analysis))
                    structure.organization?.let { appendLine("  $it") }
                    structure.transitions?.let { appendLine("  $it") }
                    structure.logicalFlow?.let { appendLine("  $it") }
                    appendLine()
                }
            }

            val style = json.style
            if (style != null) {
                val hasContent = listOfNotNull(style.sentenceVariety, style.tone, style.conciseness, style.academicRegister).any { it.isNotBlank() }
                if (hasContent) {
                    appendLine(context.getString(R.string.essay_style_suggestions))
                    style.sentenceVariety?.let { appendLine("  $it") }
                    style.tone?.let { appendLine("  $it") }
                    style.conciseness?.let { appendLine("  $it") }
                    style.academicRegister?.let { appendLine("  $it") }
                }
            }
        }.ifBlank { context.getString(R.string.essay_no_obvious_problems) }

        val score = json.score
        val overallScore = if (score != null && score.total > 0) {
            buildString {
                appendLine("Grammar: ${score.grammar}/25")
                appendLine("Vocabulary: ${score.vocabulary}/25")
                appendLine("Structure: ${score.structure}/25")
                appendLine("Style: ${score.style}/25")
                appendLine("Total: ${score.total}/100")
                score.grade?.let { appendLine("Grade: $it") }
            }.trim()
        } else {
            context.getString(R.string.essay_not_scored)
        }

        val writingTips = if (!json.writingTips.isNullOrEmpty()) {
            json.writingTips.joinToString("\n") { "- $it" }
        } else {
            context.getString(R.string.essay_no_suggestions)
        }

        return CorrectionResult(
            corrections = corrections,
            correctedEssay = json.correctedEssay ?: "",
            overallScore = overallScore,
            writingTips = writingTips,
            grammarErrorCount = json.grammarErrors?.size ?: 0,
            vocabularyCount = json.vocabulary?.size ?: 0,
            totalScore = score?.total ?: 0,
            grade = score?.grade ?: ""
        )
    }
}
