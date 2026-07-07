package luzzr.xi.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import luzzr.xi.domain.model.CorrectionResult
import luzzr.xi.domain.repository.EssayGateway
import luzzr.xi.domain.model.EssayError
import org.junit.Assert.assertEquals
import org.junit.Test

class CorrectEssayUseCaseTest {

    @Test
    fun `correctFromText delegates to repository`() = runTest {
        val mockRepo = mockk<EssayGateway>()
        val fakeResult = CorrectionResult(
            grammarErrors = emptyList(),
            vocabulary = emptyList(),
            structure = null,
            style = null,
            score = null,
            correctedEssay = "Test",
            writingTips = emptyList(),
            usage = null
        )
        
        coEvery { mockRepo.correctFromText("Test essay", "high") } returns Result.success(fakeResult)

        val useCase = CorrectEssayUseCase(mockRepo)
        val result = useCase.correctFromText("Test essay", "high")

        assertEquals(true, result.isSuccess)
        assertEquals("Test", result.getOrNull()?.correctedEssay)
    }

    @Test
    fun `correctFromText fails on empty text`() = runTest {
        val mockRepo = mockk<EssayGateway>()
        val useCase = CorrectEssayUseCase(mockRepo)
        val result = useCase.correctFromText("   ", "high")

        assertEquals(true, result.isFailure)
        assertEquals(EssayError.TextEmpty, result.exceptionOrNull())
    }
}
