package luzzr.xi

import android.content.Context
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.data.api.ApiProvider
import luzzr.xi.data.api.NetworkCheck
import luzzr.xi.data.api.OpenAiApi
import luzzr.xi.data.local.AppSettings
import luzzr.xi.data.local.SettingsDataStore
import luzzr.xi.data.model.ChatMessage
import luzzr.xi.data.model.ChatRequest
import luzzr.xi.data.model.ChatResponse
import luzzr.xi.data.model.Choice
import luzzr.xi.data.model.ContentPart
import luzzr.xi.data.model.ErrorBody
import luzzr.xi.data.repository.EssayRepository
import luzzr.xi.domain.model.AppError
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EssayRepositoryTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
    private val openAiApi = mockk<OpenAiApi>()

    private lateinit var repository: EssayRepository

    @Before
    fun setUp() {
        mockkObject(ApiProvider)
        mockkObject(NetworkCheck)

        every { NetworkCheck.isNetworkAvailable(any()) } returns true
        every { settingsDataStore.settings } returns flowOf(AppSettings(apiKey = "test-key-123"))
        every {
            ApiProvider.createApi(any(), any(), any(), any(), any(), any())
        } returns openAiApi

        every { context.getString(R.string.essay_grammar_correction) } returns "--- 语法纠错 ---"
        every { context.getString(R.string.essay_vocabulary_optimization) } returns "--- 词汇优化 ---"
        every { context.getString(R.string.essay_structure_analysis) } returns "--- 结构分析 ---"
        every { context.getString(R.string.essay_style_suggestions) } returns "--- 风格建议 ---"
        every { context.getString(R.string.essay_no_obvious_problems) } returns "未发现明显问题。"
        every { context.getString(R.string.essay_not_scored) } returns "未评分"
        every { context.getString(R.string.essay_no_suggestions) } returns "暂无建议"
        every { context.getString(R.string.error_essay_result_empty) } returns "批改结果为空"
        every { context.getString(R.string.error_empty_key) } returns "请先在设置中填写 API Key"
        every { context.getString(R.string.error_parse_failed) } returns "AI 返回格式异常，请重试"
        every { context.getString(R.string.error_invalid_json) } returns "AI 返回无效格式，请重试"
        every { context.getString(R.string.error_ai_not_english) } returns "输入内容不是英文，请提供英文作文"
        every { context.getString(R.string.error_ai_unreadable_image) } returns "图片无法识别，请提供更清晰的图片"
        every { context.getString(R.string.error_ai_generic) } returns "AI 处理异常: %s"

        repository = EssayRepository(context, settingsDataStore)
    }

    @After
    fun tearDown() {
        unmockkObject(ApiProvider)
        unmockkObject(NetworkCheck)
    }

    // --- Helpers ---

    private fun buildFullJsonResponse(): String = """{
        "grammar_errors": [
            {"line": 1, "original": "he go", "corrected": "he goes", "type": "agreement", "explanation": "主谓一致"},
            {"line": 3, "original": "a apple", "corrected": "an apple", "type": "article", "explanation": "冠词用法"}
        ],
        "vocabulary": [
            {"original": "good", "suggested": "excellent", "reason": "更精确的学术用词", "register": "academic"},
            {"original": "big", "suggested": "substantial", "reason": "学术搭配更自然", "register": "academic"}
        ],
        "structure": {
            "organization": "段落结构清晰，主题句明确",
            "transitions": "过渡词使用不足",
            "logical_flow": "论证逻辑递进合理"
        },
        "style": {
            "sentence_variety": "句式单一，建议混合使用简单句和复合句",
            "tone": "语调适中，符合学术写作要求",
            "conciseness": "部分表达冗余",
            "academic_register": "学术规范性良好"
        },
        "score": {
            "grammar": 20, "vocabulary": 18, "structure": 22, "style": 19,
            "total": 79, "grade": "B+"
        },
        "corrected_essay": "He goes to school every day. The weather is excellent.",
        "writing_tips": ["始终检查主谓一致性", "在元音开头的单词前使用an"]
    }"""

    private fun successResponse(content: String): ChatResponse = ChatResponse(
        id = "chatcmpl-test",
        choices = listOf(
            Choice(
                index = 0,
                message = ChatMessage(role = "assistant", content = content),
                finishReason = "stop"
            )
        ),
        error = null
    )

    // --- Tests ---

    @Test
    fun `correctEssay success with all JSON fields parsed`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(buildFullJsonResponse())

        val result = repository.correctEssay("He go to school every day.")

        assertTrue(result.isSuccess)
        val correction = result.getOrNull()!!

        assertTrue(correction.corrections.contains("--- 语法纠错 ---"))
        assertTrue(correction.corrections.contains("主谓一致"))
        assertTrue(correction.corrections.contains("--- 词汇优化 ---"))
        assertTrue(correction.corrections.contains("excellent"))
        assertTrue(correction.corrections.contains("--- 结构分析 ---"))
        assertTrue(correction.corrections.contains("--- 风格建议 ---"))

        assertEquals("He goes to school every day. The weather is excellent.", correction.correctedEssay)

        assertTrue(correction.overallScore.contains("Total: 79/100"))
        assertTrue(correction.overallScore.contains("Grammar: 20/25"))

        assertTrue(correction.writingTips.contains("主谓一致性"))

        assertEquals(2, correction.grammarErrorCount)
        assertEquals(2, correction.vocabularyCount)
        assertEquals(79, correction.totalScore)
        assertEquals("B+", correction.grade)
    }

    @Test
    fun `correctEssay handles empty response with null choices`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns ChatResponse(
            id = "chatcmpl-test", choices = null, error = null
        )

        val result = repository.correctEssay("Some essay text")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.EmptyResultError)
        assertEquals("批改结果为空", error?.message)
    }

    @Test
    fun `correctEssay handles API error response`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns ChatResponse(
            id = null, choices = null,
            error = ErrorBody(message = "Rate limit exceeded", type = "rate_limit", code = "429")
        )

        val result = repository.correctEssay("Some essay text")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ApiError)
        assertEquals("Rate limit exceeded", error?.message)
    }

    @Test
    fun `correctEssay handles minimal JSON with only required fields`() = runTest {
        val minimalJson = """{
            "grammar_errors": [],
            "vocabulary": [],
            "structure": {"organization": "", "transitions": "", "logical_flow": ""},
            "style": {"sentence_variety": "", "tone": "", "conciseness": "", "academic_register": ""},
            "score": {"grammar": 0, "vocabulary": 0, "structure": 0, "style": 0, "total": 0, "grade": "F"},
            "corrected_essay": "He goes to school.",
            "writing_tips": []
        }"""
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(minimalJson)

        val result = repository.correctEssay("He go to school.")

        assertTrue(result.isSuccess)
        val correction = result.getOrNull()!!
        assertEquals("He goes to school.", correction.correctedEssay)
        assertEquals("未发现明显问题。", correction.corrections)
        assertEquals("未评分", correction.overallScore)
        assertEquals("暂无建议", correction.writingTips)
    }

    @Test
    fun `correctEssay handles AI error field - not English`() = runTest {
        val errorJson = """{"error": "Input is not English", "grammar_errors": [], "vocabulary": [], "structure": {}, "style": {}, "score": {}, "corrected_essay": "", "writing_tips": []}"""
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(errorJson)

        val result = repository.correctEssay("这不是英文作文")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ParseError)
        assertTrue(error!!.message!!.contains("英文"))
    }

    @Test
    fun `correctEssay uses JSON response format`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(buildFullJsonResponse())

        repository.correctEssay("Test essay")

        coVerify {
            openAiApi.chatCompletions(match { it.responseFormat?.type == "json_object" })
        }
    }

    @Test
    fun `correctEssay uses temperature 0_2`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(buildFullJsonResponse())

        repository.correctEssay("Test essay")

        coVerify {
            openAiApi.chatCompletions(match { it.temperature == 0.2 })
        }
    }

    @Test
    fun `correctEssay uses maxTokens 8192`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(buildFullJsonResponse())

        repository.correctEssay("Test essay")

        coVerify {
            openAiApi.chatCompletions(match { it.maxTokens == 8192 })
        }
    }

    @Test
    fun `correctEssay uses xhigh reasoningEffort by default`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(buildFullJsonResponse())

        repository.correctEssay("Test essay")

        coVerify {
            openAiApi.chatCompletions(match { it.reasoningEffort == "high" })
        }
    }

    @Test
    fun `correctEssay handles invalid JSON response`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse("not json at all {{{")

        val result = repository.correctEssay("Test essay")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.ParseError)
    }

    // --- correctEssayFromImage tests ---

    @Test
    fun `correctEssayFromImage success with multimodal request`() = runTest {
        val repoSpy = spyk(repository)
        val imageUri = mockk<Uri>()
        val requestSlot = slot<ChatRequest>()

        coEvery { repoSpy.uriToBase64(imageUri) } returns "fakebase64data"
        coEvery { openAiApi.chatCompletions(capture(requestSlot)) } returns successResponse(buildFullJsonResponse())

        val result = repoSpy.correctEssayFromImage(imageUri, "xhigh")

        assertTrue(result.isSuccess)
        val correction = result.getOrNull()!!
        assertTrue(correction.corrections.contains("--- 语法纠错 ---"))
        assertEquals("He goes to school every day. The weather is excellent.", correction.correctedEssay)

        val request = requestSlot.captured
        val message = request.messages.first()
        assertTrue(message.content is List<*>)
        val parts = message.content as List<ContentPart>
        assertEquals(2, parts.size)

        assertEquals("text", parts[0].type)
        assertNotNull(parts[0].text)
        assertTrue(parts[0].text!!.contains("STEP 1"))

        assertEquals("image_url", parts[1].type)
        assertNotNull(parts[1].imageUrl)
        assertEquals("data:image/jpeg;base64,fakebase64data", parts[1].imageUrl!!.url)
    }

    @Test
    fun `correctEssayFromImage uses JSON response format`() = runTest {
        val repoSpy = spyk(repository)
        val imageUri = mockk<Uri>()

        coEvery { repoSpy.uriToBase64(imageUri) } returns "fakebase64data"
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(buildFullJsonResponse())

        repoSpy.correctEssayFromImage(imageUri)

        coVerify {
            openAiApi.chatCompletions(match { it.responseFormat?.type == "json_object" })
        }
    }

    @Test
    fun `correctEssayFromImage handles uriToBase64 returning null`() = runTest {
        val repoSpy = spyk(repository)
        val imageUri = mockk<Uri>()

        coEvery { repoSpy.uriToBase64(imageUri) } returns null

        val result = repoSpy.correctEssayFromImage(imageUri)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.UnknownError)
        assertTrue(error!!.message!!.contains("Failed to read image"))
    }

    @Test
    fun `correctEssayFromImage handles API error response`() = runTest {
        val repoSpy = spyk(repository)
        val imageUri = mockk<Uri>()

        coEvery { repoSpy.uriToBase64(imageUri) } returns "fakebase64data"
        coEvery { openAiApi.chatCompletions(any()) } returns ChatResponse(
            id = null, choices = null,
            error = ErrorBody(message = "Invalid image format", type = "invalid_request", code = "400")
        )

        val result = repoSpy.correctEssayFromImage(imageUri)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ApiError)
        assertEquals("Invalid image format", error?.message)
    }

    @Test
    fun `correctEssayFromImage handles empty choices`() = runTest {
        val repoSpy = spyk(repository)
        val imageUri = mockk<Uri>()

        coEvery { repoSpy.uriToBase64(imageUri) } returns "fakebase64data"
        coEvery { openAiApi.chatCompletions(any()) } returns ChatResponse(
            id = "chatcmpl-test", choices = null, error = null
        )

        val result = repoSpy.correctEssayFromImage(imageUri)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.EmptyResultError)
    }

    // --- Prompt structure tests ---

    @Test
    fun `buildCorrectionPrompt contains required JSON structure`() = runTest {
        val requestSlot = slot<ChatRequest>()
        coEvery { openAiApi.chatCompletions(capture(requestSlot)) } returns successResponse(buildFullJsonResponse())

        repository.correctEssay("My test essay text.")

        val request = requestSlot.captured
        val messageContent = request.messages.first().content as String

        assertTrue(messageContent.contains("grammar_errors"))
        assertTrue(messageContent.contains("vocabulary"))
        assertTrue(messageContent.contains("structure"))
        assertTrue(messageContent.contains("style"))
        assertTrue(messageContent.contains("score"))
        assertTrue(messageContent.contains("corrected_essay"))
        assertTrue(messageContent.contains("writing_tips"))
        assertTrue(messageContent.contains("My test essay text."))
    }

    @Test
    fun `correctEssay from JSON with only corrected essay`() = runTest {
        val minimalJson = """{
            "grammar_errors": [{"original": "go", "corrected": "goes", "explanation": "test"}],
            "vocabulary": [],
            "structure": {"organization": "ok"},
            "style": {"tone": "ok"},
            "score": {"grammar": 20, "vocabulary": 20, "structure": 20, "style": 20, "total": 80, "grade": "B+"},
            "corrected_essay": "He goes to school.",
            "writing_tips": ["Check verbs"]
        }"""
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(minimalJson)

        val result = repository.correctEssay("test")

        assertTrue(result.isSuccess)
        val correction = result.getOrNull()!!
        assertEquals("He goes to school.", correction.correctedEssay)
        assertEquals(80, correction.totalScore)
        assertEquals("B+", correction.grade)
        assertEquals(1, correction.grammarErrorCount)
    }
}
