package luzzr.xi.data.repository

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import luzzr.xi.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JsonResponseParserTest {
    private lateinit var context: Context
    private val parser = JsonResponseParser()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.getString(R.string.error_parse_failed) } returns "Parse failed"
        every { context.getString(R.string.error_translate_result_empty) } returns "Translation empty"
        every { context.getString(R.string.error_invalid_json) } returns "Invalid JSON"
        every { context.getString(R.string.error_ai_not_english) } returns "Input is not English"
        every { context.getString(R.string.error_ai_unreadable_image) } returns "Image is unreadable"
        every { context.getString(R.string.error_ai_generic, *anyVararg()) } answers {
            val formatArgs = args[1] as? Array<*>
            "AI processing error: ${formatArgs?.firstOrNull() ?: ""}"
        }
    }

    @Test
    fun `parseTranslation success with plain JSON`() {
        val json = """{"translation":"Hello world"}"""
        val result = parser.parseTranslation(context, json)
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
        val result = parser.parseTranslation(context, json)
        assertTrue(result.isSuccess)
        assertEquals("Hello world", result.getOrNull()?.translation)
    }

    @Test
    fun `parseTranslation failure with empty translation`() {
        val json = """{"translation":""}"""
        val result = parser.parseTranslation(context, json)
        assertTrue(result.isFailure)
        assertEquals("Translation empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `parseTranslation failure with invalid JSON`() {
        val json = """{"translation":"Hello"""
        val result = parser.parseTranslation(context, json)
        assertTrue(result.isFailure)
        assertEquals("Invalid JSON", result.exceptionOrNull()?.message)
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
        val result = parser.parseEssayCorrection(context, json)
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
        val result = parser.parseEssayCorrection(context, json)
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
        val result = parser.parseEssayCorrection(context, json)
        assertTrue(result.isFailure)
        assertEquals("AI processing error: Failed to parse image", result.exceptionOrNull()?.message)
    }
}
