package com.lightmark.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * GitHub Token 认证拦截器
 * 自动为所有请求添加 Authorization 头
 */
class GitHubAuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()

        val requestBuilder = original.newBuilder()
            .header("Accept", "application/vnd.github.v3+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "LightMark-App")

        // 如果有 Token，添加认证头
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
