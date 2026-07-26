package com.lightmark.di

import android.content.Context
import com.lightmark.data.local.LightMarkDatabase
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.remote.GitHubApiService
import com.lightmark.data.remote.GitHubAuthInterceptor
import com.lightmark.data.repository.TodoRepository
import com.lightmark.data.repository.TodoRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 应用级依赖注入模块
 *
 * 提供：
 * - Room 数据库
 * - Retrofit + OkHttp
 * - GitHub API 服务
 * - 仓库实现
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideJson(): Json = json

    @Provides
    @Singleton
    fun provideTodoDao(database: LightMarkDatabase): TodoDao = database.todoDao()

    @Provides
    @Singleton
    fun provideCategoryDao(database: LightMarkDatabase): CategoryDao = database.categoryDao()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LightMarkDatabase {
        return LightMarkDatabase.create(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(): GitHubAuthInterceptor {
        return GitHubAuthInterceptor {
            // Token 从 AuthManager 获取，通过 SharedPreferences 传入
            null
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: GitHubAuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubApiService(okHttpClient: OkHttpClient): GitHubApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTodoRepository(
        todoDao: TodoDao,
        gitHubApiService: GitHubApiService,
        json: Json
    ): TodoRepository {
        return TodoRepositoryImpl(todoDao, gitHubApiService, json)
    }
}
