package luzzr.xi.domain.model

import com.google.gson.annotations.SerializedName

enum class ThinkingLevel(
    val id: String,
    val displayName: String,
    val description: String
) {
    LOW("low", "Low", "Simple reasoning"),
    MEDIUM("medium", "Medium", "Moderate thinking"),
    HIGH("high", "Deep", "Deep analysis");

    companion object {
        fun fromId(id: String): ThinkingLevel = entries.find { it.id == id } ?: MEDIUM
    }
}

data class ResponseFormat(
    @SerializedName("type") val type: String = "json_object"
) {
    companion object {
        fun json() = ResponseFormat("json_object")
    }
}

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("temperature") val temperature: Double = 0.3,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    @SerializedName("reasoning_effort") val reasoningEffort: String? = null,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null,
    @SerializedName("stream") val stream: Boolean? = null,
    @SerializedName("stream_options") val streamOptions: StreamOptions? = null
)

data class StreamOptions(
    @SerializedName("include_usage") val includeUsage: Boolean
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: Any? // String or List<ContentPart> for multimodal
)

data class ContentPart(
    @SerializedName("type") val type: String, // "text" or "image_url"
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    @SerializedName("url") val url: String // data:image/jpeg;base64,... or http(s) URL
)

data class ChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<Choice>?,
    @SerializedName("error") val error: ErrorBody?,
    @SerializedName("usage") val usage: Usage? = null
)

data class Choice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ErrorBody(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String? = null,
    @SerializedName("code") val code: String? = null
)

data class ModelListResponse(
    @SerializedName("data") val data: List<ModelInfo>?
)

data class ModelInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("owned_by") val ownedBy: String?
)

data class ChatStreamChunk(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<StreamChoice>?,
    @SerializedName("usage") val usage: Usage? = null
)

data class StreamChoice(
    @SerializedName("index") val index: Int,
    @SerializedName("delta") val delta: StreamDelta,
    @SerializedName("finish_reason") val finishReason: String?
)

data class StreamDelta(
    @SerializedName("content") val content: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("completion_tokens") val completionTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)
