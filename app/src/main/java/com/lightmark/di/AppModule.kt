package com.lightmark.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.lightmark.data.local.LightMarkDatabase
import com.lightmark.data.local.dao.AlarmDao
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.InboxDao
import com.lightmark.data.local.dao.SmartListDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.repository.TodoRepository
import com.lightmark.data.repository.TodoRepositoryImpl
import com.lightmark.data.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * 应用级依赖注入模块
 *
 * 轻刻为完全离线应用，本模块只提供本地能力：
 * - Room 数据库与各 DAO
 * - DataStore 设置存储
 * - 本地仓库实现
 *
 * 不含任何网络组件（无 Retrofit / OkHttp / 远程 API）。
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
    fun provideHabitDao(database: LightMarkDatabase): com.lightmark.data.local.dao.HabitDao =
        database.habitDao()

    @Provides
    @Singleton
    fun provideTemplateDao(database: LightMarkDatabase): com.lightmark.data.local.dao.TemplateDao =
        database.templateDao()

    @Provides
    @Singleton
    fun provideInboxDao(database: LightMarkDatabase): InboxDao = database.inboxDao()

    @Provides
    @Singleton
    fun provideAlarmDao(database: LightMarkDatabase): AlarmDao = database.alarmDao()

    @Provides
    @Singleton
    fun provideSmartListDao(database: LightMarkDatabase): SmartListDao = database.smartListDao()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LightMarkDatabase {
        return LightMarkDatabase.create(context)
    }

    @Provides
    @Singleton
    fun provideTodoRepository(
        todoDao: TodoDao,
        categoryDao: CategoryDao,
        habitDao: com.lightmark.data.local.dao.HabitDao,
        templateDao: com.lightmark.data.local.dao.TemplateDao,
        alarmDao: AlarmDao,
        inboxDao: InboxDao,
        smartListDao: SmartListDao
    ): TodoRepository {
        return TodoRepositoryImpl(
            todoDao = todoDao,
            categoryDao = categoryDao,
            habitDao = habitDao,
            templateDao = templateDao,
            alarmDao = alarmDao,
            inboxDao = inboxDao,
            smartListDao = smartListDao
        )
    }
}
