package com.lightmark.data.remote.openclaw

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * OpenClaw / OpenAI 兼容的对话补全接口
 *
 * 端点约定（OpenAI 兼容）：
 *   POST {baseUrl}chat/completions
 *   Header: Authorization: Bearer <apiKey>
 *   请求体: { model, messages:[{role,content}], temperature, stream:false }
 */
interface OpenClawApi {
    @POST("chat/completions")
    suspend fun chatCompletion(@Body body: OpenClawChatRequest): OpenClawChatResponse
}

data class OpenClawChatRequest(
    val model: String,
    val messages: List<OpenClawMessage>,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

data class OpenClawMessage(val role: String, val content: String)

data class OpenClawChatResponse(
    val id: String? = null,
    val `object`: String? = null,
    val choices: List<OpenClawChoice> = emptyList()
)

data class OpenClawChoice(
    val index: Int = 0,
    val message: OpenClawMessage? = null,
    val finish_reason: String? = null
)

/**
 * 根据可配置的 BaseURL 与 API Key 创建 OpenClaw 客户端。
 * 兼容任意 OpenAI 协议端点（OpenClaw / OpenAI / 本地代理等）。
 */
object OpenClawClientFactory {

    private val gson = GsonBuilder().create()

    fun create(baseUrl: String, apiKey: String): OpenClawApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OpenClawApi::class.java)
    }
}
