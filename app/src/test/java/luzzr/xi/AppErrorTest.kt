package luzzr.xi

import luzzr.xi.domain.model.AppError
import luzzr.xi.domain.model.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorTest {

    @Test
    fun `NetworkError has default uiText`() {
        val error = AppError.NetworkError()
        assertTrue(error.uiText is UiText.StringResource)
    }

    @Test
    fun `NetworkError preserves cause`() {
        val cause = RuntimeException("connection timed out")
        val error = AppError.NetworkError(cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `ApiError has uiText and code`() {
        val error = AppError.ApiError(UiText.DynamicString("Rate limited"), code = 429)
        assertEquals(UiText.DynamicString("Rate limited"), error.uiText)
        assertEquals(429, error.code)
    }

    @Test
    fun `ConfigError has uiText`() {
        val error = AppError.ConfigError(UiText.DynamicString("Missing API key"))
        assertEquals(UiText.DynamicString("Missing API key"), error.uiText)
    }

    @Test
    fun `EmptyResultError has default uiText`() {
        val error = AppError.EmptyResultError()
        assertTrue(error.uiText is UiText.StringResource)
    }

    @Test
    fun `UnknownError wraps cause`() {
        val cause = IllegalStateException("boom")
        val error = AppError.UnknownError(cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `ParseError has uiText and rawResponse`() {
        val error = AppError.ParseError(UiText.DynamicString("Invalid JSON"), """{"bad": json}""")
        assertEquals(UiText.DynamicString("Invalid JSON"), error.uiText)
        assertEquals("""{"bad": json}""", error.rawResponse)
    }

    @Test
    fun `ParseError has uiText without rawResponse`() {
        val error = AppError.ParseError(UiText.DynamicString("Parse failed"))
        assertEquals(UiText.DynamicString("Parse failed"), error.uiText)
        assertNull(error.rawResponse)
    }

    @Test
    fun `ParseError extends AppError`() {
        val error = AppError.ParseError(UiText.DynamicString("test"))
        assertTrue(error is AppError)
    }
}
