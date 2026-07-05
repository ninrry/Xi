package luzzr.xi

import luzzr.xi.data.model.ChatMessage
import luzzr.xi.data.model.ChatRequest
import luzzr.xi.data.model.ChatResponse
import luzzr.xi.data.model.Choice
import luzzr.xi.data.model.ContentPart
import luzzr.xi.data.model.ErrorBody
import luzzr.xi.data.model.ImageUrl
import luzzr.xi.data.model.ModelInfo
import luzzr.xi.data.model.ModelListResponse
import luzzr.xi.data.model.ResponseFormat
import luzzr.xi.data.model.ThinkingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApiModelsTest {

    @Test
    fun `ChatRequest has correct defaults`() {
        val request = ChatRequest(
            model = "mimo-v2.5",
            messages = listOf(ChatMessage(role = "user", content = "hello"))
        )
        assertEquals("mimo-v2.5", request.model)
        assertEquals(0.3, request.temperature, 0.01)
        assertEquals(4096, request.maxTokens)
        assertEquals(1, request.messages.size)
        assertNull(request.responseFormat)
        assertNull(request.reasoningEffort)
    }

    @Test
    fun `ChatRequest with ResponseFormat json`() {
        val request = ChatRequest(
            model = "mimo-v2.5",
            messages = listOf(ChatMessage(role = "user", content = "hello")),
            responseFormat = ResponseFormat.json()
        )
        assertNotNull(request.responseFormat)
        assertEquals("json_object", request.responseFormat?.type)
    }

    @Test
    fun `ChatResponse parses success correctly`() {
        val response = ChatResponse(
            id = "chatcmpl-123",
            choices = listOf(
                Choice(
                    index = 0,
                    message = ChatMessage(role = "assistant", content = "Hello!"),
                    finishReason = "stop"
                )
            ),
            error = null
        )
        assertNull(response.error)
        assertNotNull(response.choices)
        assertEquals("Hello!", response.choices?.first()?.message?.content)
    }

    @Test
    fun `ChatResponse parses error correctly`() {
        val response = ChatResponse(
            id = null,
            choices = null,
            error = ErrorBody(message = "Invalid key", type = "invalid_request_error", code = "401")
        )
        assertNotNull(response.error)
        assertEquals("Invalid key", response.error?.message)
        assertNull(response.choices)
    }

    @Test
    fun `ModelListResponse parses correctly`() {
        val response = ModelListResponse(
            data = listOf(
                ModelInfo(id = "mimo-v2.5", ownedBy = "opencode"),
                ModelInfo(id = "deepseek-v4-flash", ownedBy = "opencode")
            )
        )
        assertEquals(2, response.data?.size)
        assertEquals("mimo-v2.5", response.data?.first()?.id)
    }

    @Test
    fun `ModelListResponse handles empty data`() {
        val response = ModelListResponse(data = emptyList())
        assertEquals(0, response.data?.size)
    }

    @Test
    fun `ChatMessage role types`() {
        val system = ChatMessage(role = "system", content = "You are a translator")
        val user = ChatMessage(role = "user", content = "Translate hello")
        val assistant = ChatMessage(role = "assistant", content = "你好")

        assertEquals("system", system.role)
        assertEquals("user", user.role)
        assertEquals("assistant", assistant.role)
    }

    @Test
    fun `ThinkingLevel fromId returns correct level for each id`() {
        assertEquals(ThinkingLevel.LOW, ThinkingLevel.fromId("low"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId("medium"))
        assertEquals(ThinkingLevel.HIGH, ThinkingLevel.fromId("high"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId("xhigh"))
    }

    @Test
    fun `ThinkingLevel fromId returns MEDIUM for invalid id`() {
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId("invalid"))
        assertEquals(ThinkingLevel.MEDIUM, ThinkingLevel.fromId(""))
    }

    @Test
    fun `ThinkingLevel has all 3 entries`() {
        assertEquals(3, ThinkingLevel.entries.size)
    }

    @Test
    fun `ContentPart data class construction for text and image_url types`() {
        val textPart = ContentPart(type = "text", text = "Hello")
        assertEquals("text", textPart.type)
        assertEquals("Hello", textPart.text)
        assertNull(textPart.imageUrl)

        val imagePart = ContentPart(type = "image_url", imageUrl = ImageUrl(url = "https://example.com/img.png"))
        assertEquals("image_url", imagePart.type)
        assertNull(imagePart.text)
        assertEquals("https://example.com/img.png", imagePart.imageUrl?.url)
    }

    @Test
    fun `ErrorBody with nullable type and code fields`() {
        val errorWithTypeAndCode = ErrorBody(message = "Invalid request", type = "invalid_request_error", code = "400")
        assertEquals("Invalid request", errorWithTypeAndCode.message)
        assertEquals("invalid_request_error", errorWithTypeAndCode.type)
        assertEquals("400", errorWithTypeAndCode.code)

        val errorWithoutOptional = ErrorBody(message = "Something went wrong")
        assertEquals("Something went wrong", errorWithoutOptional.message)
        assertNull(errorWithoutOptional.type)
        assertNull(errorWithoutOptional.code)
    }
}
