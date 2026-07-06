package luzzr.xi

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.core.network.OpenAiApi
import luzzr.xi.data.cache.TranslationCache
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.domain.model.ChatMessage
import luzzr.xi.domain.model.ChatResponse
import luzzr.xi.domain.model.Choice
import luzzr.xi.domain.model.ErrorBody
import luzzr.xi.data.repository.TranslationRepository
import luzzr.xi.domain.model.AppError
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationRepositoryTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
    private val openAiApi = mockk<OpenAiApi>()
    private val translationCache = TranslationCache()

    private lateinit var repository: TranslationRepository

    @Before
    fun setUp() {
        mockkObject(ApiProvider)
        mockkObject(NetworkCheck)

        every { NetworkCheck.isNetworkAvailable(any()) } returns true

        every { settingsDataStore.settings } returns flowOf(
            AppSettings(apiKey = "test-api-key", model = "test-model")
        )

        every { context.getString(R.string.error_translate_result_empty) } returns "翻译结果为空"
        every { context.getString(R.string.error_empty_key) } returns "请先在设置中填写 API Key"
        every { context.getString(R.string.error_parse_failed) } returns "AI 返回格式异常，请重试"
        every { context.getString(R.string.error_translation_empty) } returns "翻译结果为空，请重试"
        every { context.getString(R.string.error_invalid_json) } returns "AI 返回无效格式，请重试"

        every {
            ApiProvider.createApi(any(), any(), any(), any(), any(), any())
        } returns openAiApi

        translationCache.clear()
        repository = TranslationRepository(context, settingsDataStore, translationCache)
    }

    @After
    fun tearDown() {
        unmockkObject(ApiProvider)
        unmockkObject(NetworkCheck)
    }

    // --- Helpers ---

    private fun successResponse(content: String): ChatResponse = ChatResponse(
        id = "chatcmpl-translate-test",
        choices = listOf(
            Choice(
                index = 0,
                message = ChatMessage(role = "assistant", content = content),
                finishReason = "stop"
            )
        ),
        error = null
    )

    private fun jsonTranslationResponse(
        translation: String = "你好世界",
        detectedLanguage: String = "English",
        alternatives: List<String>? = null
    ): String {
        val alt = alternatives?.joinToString(", ") { "\"$it\"" } ?: ""
        return """{"translation": "$translation", "detected_language": "$detectedLanguage", "alternatives": [$alt]}"""
    }

    // --- Tests ---

    @Test
    fun `translate returns success with parsed translation`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("你好世界")
        )

        val result = repository.translate("Hello World")

        assertTrue("Expected success but got $result", result.isSuccess)
        assertEquals("你好世界", result.getOrNull())
    }

    @Test
    fun `translate returns EmptyResultError when choices is null`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns ChatResponse(
            id = "chatcmpl-empty",
            choices = null,
            error = null
        )

        val result = repository.translate("Hello")

        assertTrue("Expected failure but got $result", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("Expected EmptyResultError but got ${error?.javaClass}", error is AppError.EmptyResultError)
    }

    @Test
    fun `translate returns EmptyResultError when choices is empty list`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns ChatResponse(
            id = "chatcmpl-empty-list",
            choices = emptyList(),
            error = null
        )

        val result = repository.translate("Hello")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.EmptyResultError)
    }

    @Test
    fun `translate returns ApiError when response contains error body`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns ChatResponse(
            id = null,
            choices = null,
            error = ErrorBody(message = "Rate limit exceeded", type = "rate_limit", code = "429")
        )

        val result = repository.translate("Hello")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("Expected ApiError but got ${error?.javaClass}", error is AppError.ApiError)
        assertEquals("Rate limit exceeded", error?.message)
    }

    @Test
    fun `translate forwards reasoningEffort to the API request`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("translated")
        )

        repository.translate("Hello", reasoningEffort = "high")

        coVerify {
            openAiApi.chatCompletions(match { it.reasoningEffort == "high" })
        }
    }

    @Test
    fun `translate sends null reasoningEffort when not provided`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("translated")
        )

        repository.translate("Hello")

        coVerify {
            openAiApi.chatCompletions(match { it.reasoningEffort == null })
        }
    }

    @Test
    fun `translate caches API instance across calls`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("translated")
        )

        repository.translate("Hello")
        repository.translate("World")

        verify(exactly = 1) {
            ApiProvider.createApi(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `translate recreates API when settings change`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("translated")
        )
        repository.translate("Hello")

        every { settingsDataStore.settings } returns flowOf(
            AppSettings(apiKey = "new-api-key", model = "test-model")
        )
        repository.translate("World")

        verify(exactly = 2) {
            ApiProvider.createApi(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `translate uses JSON response format`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("你好")
        )

        repository.translate("Hello")

        coVerify {
            openAiApi.chatCompletions(match { it.responseFormat?.type == "json_object" })
        }
    }

    @Test
    fun `translate caches result for repeated calls`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("你好世界")
        )

        repository.translate("Hello World")
        repository.translate("Hello World")

        coVerify(exactly = 1) { openAiApi.chatCompletions(any()) }
    }

    @Test
    fun `translate prompt contains source and target language`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("translated")
        )

        repository.translate("Hello", sourceLang = "English", targetLang = "Japanese")

        coVerify {
            openAiApi.chatCompletions(match {
                val content = it.messages.first().content as String
                content.contains("English") && content.contains("Japanese")
            })
        }
    }

    @Test
    fun `translate prompt contains the input text`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            jsonTranslationResponse("translated")
        )

        repository.translate("Hello World")

        coVerify {
            openAiApi.chatCompletions(match {
                val content = it.messages.first().content as String
                content.contains("Hello World")
            })
        }
    }

    @Test
    fun `translate returns ParseError for invalid JSON`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse("not valid json {{{")

        val result = repository.translate("Hello")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.ParseError)
    }

    @Test
    fun `translate returns ParseError for empty translation in JSON`() = runTest {
        coEvery { openAiApi.chatCompletions(any()) } returns successResponse(
            """{"translation": "", "detected_language": "English", "alternatives": []}"""
        )

        val result = repository.translate("Hello")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.ParseError)
    }
}
