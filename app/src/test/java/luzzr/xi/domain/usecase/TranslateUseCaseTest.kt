package luzzr.xi.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import luzzr.xi.domain.model.TranslationEngine
import luzzr.xi.domain.model.TranslationResult
import luzzr.xi.data.repository.TranslationRepository
import luzzr.xi.data.repository.MlKitTranslator
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateUseCaseTest {

    @Test
    fun `translate delegates to appropriate repository`() = runTest {
        val mockRepo = mockk<TranslationRepository>()
        val mockMlKit = mockk<MlKitTranslator>()
        val fakeResult = TranslationResult(
            translation = "Test translation",
            detectedLanguage = "English",
            alternatives = emptyList(),
            usage = null
        )

        coEvery { mockRepo.streamTranslate("Test", "English", "Chinese", "high") } returns flowOf(Result.success(fakeResult))

        val useCase = TranslateUseCase(mockRepo, mockMlKit)
        val flow = useCase("Test", "English", "Chinese", TranslationEngine.AI, "high")
        val result = flow.first()

        assertEquals(true, result.isSuccess)
        assertEquals("Test translation", result.getOrNull()?.translation)
    }
}
