package luzzr.xi.domain.model

import com.google.gson.annotations.SerializedName

enum class ThinkingLevel(
    val id: String,
    val displayName: String,
    val description: String
) {
    LOW("low", "轻度", "简单推理"),
    MEDIUM("medium", "中等", "适中思考"),
    HIGH("high", "深度", "深度分析");

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
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
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
    @SerializedName("error") val error: ErrorBody?
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
