package luzzr.xi.data.repository

import luzzr.xi.domain.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonResponseParserTest {
    private val parser = JsonResponseParser()

    @Test
    fun `parseTranslation success with plain JSON`() {
        val json = """{"translation":"Hello world"}"""
        val result = parser.parseTranslation(json)
        assertTrue(result.isSuccess)
        assertEquals("Hello world", result.getOrNull()?.translation)
    }

    @Test
    fun `parseTranslation success with markdown JSON`() {
        val json = """
            ```json
            {"translation":"Hello world"}
            ```
        """.trimIndent()
        val result = parser.parseTranslation(json)
        assertTrue(result.isSuccess)
        assertEquals("Hello world", result.getOrNull()?.translation)
    }

    @Test
    fun `parseTranslation failure with empty translation`() {
        val json = """{"translation":""}"""
        val result = parser.parseTranslation(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.ParseError)
    }

    @Test
    fun `parseTranslation failure with invalid JSON`() {
        val json = """{"translation":"Hello"""
        val result = parser.parseTranslation(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.ParseError)
    }

    @Test
    fun `parseEssayCorrection success with valid JSON`() {
        val json = """
            {
              "grammar_errors": [],
              "vocabulary": [],
              "score": {
                "total": 95,
                "grade": "A+"
              },
              "corrected_essay": "This is good.",
              "writing_tips": ["Keep it up"]
            }
        """.trimIndent()
        val result = parser.parseEssayCorrection(json)
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals("This is good.", data?.correctedEssay)
        assertEquals(95, data?.score?.total)
    }

    @Test
    fun `parseEssayCorrection extracts JSON from markdown block`() {
        val json = """
            ```json
            {
              "corrected_essay": "Extracted properly"
            }
            ```
        """.trimIndent()
        val result = parser.parseEssayCorrection(json)
        assertTrue(result.isSuccess)
        assertEquals("Extracted properly", result.getOrNull()?.correctedEssay)
    }

    @Test
    fun `parseEssayCorrection failure with error field`() {
        val json = """
            {
              "error": "Failed to parse image"
            }
        """.trimIndent()
        val result = parser.parseEssayCorrection(json)
        assertTrue(result.isFailure)
    }
}
