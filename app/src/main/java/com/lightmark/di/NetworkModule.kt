package com.lightmark.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lightmark.auth.TokenHolder
import com.lightmark.data.remote.GitHubApiService
import com.lightmark.data.remote.GitHubAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * 网络层依赖注入模块（GitHub 同步 / 远程 API）。
 *
 * 仅为 GitHub 同步所需的 Retrofit 链路提供绑定：
 *   Gson -> GitHubAuthInterceptor -> OkHttpClient -> Retrofit -> GitHubApiService
 *
 * 本地/离线能力仍由 AppModule 提供；OpenClaw 客户端为按需工厂（OpenClawClientFactory），
 * 不经由 Hilt 注入，因此本模块不提供它。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val GITHUB_BASE_URL = "https://api.github.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideGitHubAuthInterceptor(tokenHolder: TokenHolder): GitHubAuthInterceptor =
        GitHubAuthInterceptor { tokenHolder.token }

    @Provides
    @Singleton
    fun provideOkHttpClient(interceptor: GitHubAuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(GITHUB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideGitHubApiService(retrofit: Retrofit): GitHubApiService =
        retrofit.create(GitHubApiService::class.java)
}
