package luzzr.xi

import luzzr.xi.domain.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorTest {

    @Test
    fun `NetworkError has default message`() {
        val error = AppError.NetworkError()
        assertEquals("Network Error", error.message)
    }

    @Test
    fun `NetworkError preserves cause`() {
        val cause = RuntimeException("connection timed out")
        val error = AppError.NetworkError(cause)
        assertEquals("Network Error", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `ApiError has message and code`() {
        val error = AppError.ApiError(message = "Rate limited", code = 429)
        assertEquals("Rate limited", error.message)
        assertEquals(429, error.code)
    }

    @Test
    fun `ConfigError has message`() {
        val error = AppError.ConfigError(message = "Missing API key")
        assertEquals("Missing API key", error.message)
    }

    @Test
    fun `EmptyResultError has default message`() {
        val error = AppError.EmptyResultError()
        assertEquals("Result is empty", error.message)
    }

    @Test
    fun `UnknownError wraps cause message`() {
        val cause = IllegalStateException("boom")
        val error = AppError.UnknownError(cause)
        assertEquals("Unknown error: boom", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `ParseError has message and rawResponse`() {
        val error = AppError.ParseError("Invalid JSON", """{"bad": json}""")
        assertEquals("Invalid JSON", error.message)
        assertEquals("""{"bad": json}""", error.rawResponse)
    }

    @Test
    fun `ParseError has message without rawResponse`() {
        val error = AppError.ParseError("Parse failed")
        assertEquals("Parse failed", error.message)
        assertNull(error.rawResponse)
    }

    @Test
    fun `ParseError extends AppError`() {
        val error = AppError.ParseError("test")
        assertTrue(error is AppError)
    }
}
