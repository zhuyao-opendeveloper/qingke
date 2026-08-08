package com.lightmark.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.lightmark.data.local.LightMarkDatabase
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.remote.GitHubApiService
import com.lightmark.auth.TokenHolder
import com.lightmark.data.remote.GitHubAuthInterceptor
import com.lightmark.data.repository.TodoRepository
import com.lightmark.data.repository.TodoRepositoryImpl
import com.lightmark.data.settings.SettingsRepository
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
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("lightmark_settings") }
        )
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>
    ): SettingsRepository = SettingsRepository(dataStore)

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
    fun provideTokenHolder(): TokenHolder = TokenHolder()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenHolder: TokenHolder): GitHubAuthInterceptor {
        return GitHubAuthInterceptor {
            // 从 TokenHolder 读取当前 token（登录/恢复时由 AuthManager 写入）
            tokenHolder.token
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
    fun provideGson(): com.google.gson.Gson = com.google.gson.GsonBuilder()
        .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    @Provides
    @Singleton
    fun provideGitHubApiService(okHttpClient: OkHttpClient, gson: com.google.gson.Gson): GitHubApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
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
