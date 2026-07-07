package luzzr.xi.data.repository

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.R
import luzzr.xi.core.datastore.AppSettings
import luzzr.xi.core.datastore.SettingsDataStore
import luzzr.xi.core.network.ApiProvider
import luzzr.xi.core.network.NetworkCheck
import luzzr.xi.core.network.OpenAiApi
import luzzr.xi.core.provider.ProviderConfig
import luzzr.xi.domain.model.ChatResponse
import luzzr.xi.domain.model.Choice
import luzzr.xi.domain.model.ChatMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EssayRepositoryTest {
    private lateinit var context: Context
    private lateinit var dataStore: SettingsDataStore
    private lateinit var mediaProcessor: MediaProcessor
    private lateinit var mockApi: OpenAiApi
    private lateinit var apiProvider: ApiProvider
    private lateinit var networkCheck: NetworkCheck
    private lateinit var jsonParser: JsonResponseParser

    @Before
    fun setup() {
        context = mockk()
        every { context.getString(R.string.essay_not_scored) } returns "Not scored"
        every { context.getString(R.string.essay_no_suggestions) } returns "No suggestions"
        every { context.getString(R.string.essay_no_obvious_problems) } returns "No obvious problems"
        
        dataStore = mockk()
        every { dataStore.settings } returns flowOf(AppSettings(apiKey = "test_key", apiBaseUrl = "http://test"))
        
        mediaProcessor = mockk()
        
        mockApi = mockk()
        apiProvider = mockk()
        every { apiProvider.createApi(any<ProviderConfig>(), any(), any(), any(), any(), any()) } returns mockApi
        
        networkCheck = mockk()
        every { networkCheck.isNetworkAvailable() } returns true

        jsonParser = JsonResponseParser()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `correctEssay handles valid JSON and validates score bounds`() = runTest {
        val repo = EssayRepository(context, dataStore, apiProvider, networkCheck, mediaProcessor, jsonParser)
        
        val validJson = """
            {
              "score": {
                "grammar": 30,
                "vocabulary": -5,
                "structure": 20,
                "style": 25,
                "total": 150,
                "grade": "A"
              },
              "corrected_essay": "This is good.",
              "writing_tips": []
            }
        """.trimIndent()
        
        coEvery { mockApi.chatCompletions(any()) } returns ChatResponse(
            id = "1",
            choices = listOf(
                Choice(
                    index = 0,
                    message = ChatMessage(role = "assistant", content = validJson),
                    finishReason = "stop"
                )
            ),
            usage = null,
            error = null
        )
        
        val result = repo.correctEssay("Test essay")
        
        assertTrue(result.isSuccess)
        val correctionResult = result.getOrNull()!!
        
        assertEquals(70, correctionResult.score?.total)
        assertEquals(25, correctionResult.score?.grammar)
        assertEquals(0, correctionResult.score?.vocabulary)
        assertEquals(20, correctionResult.score?.structure)
        assertEquals(25, correctionResult.score?.style)
    }
}
