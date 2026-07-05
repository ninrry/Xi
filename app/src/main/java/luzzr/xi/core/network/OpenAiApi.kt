package luzzr.xi.core.network

import luzzr.xi.domain.model.ChatRequest
import luzzr.xi.domain.model.ChatResponse
import luzzr.xi.domain.model.ModelListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApi {

    @POST("chat/completions")
    suspend fun chatCompletions(
        @Body request: ChatRequest
    ): ChatResponse

    @GET("models")
    suspend fun listModels(): ModelListResponse
}