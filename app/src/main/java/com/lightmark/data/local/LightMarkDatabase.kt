package com.lightmark.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lightmark.data.local.dao.AlarmDao
import com.lightmark.data.local.dao.CategoryDao
import com.lightmark.data.local.dao.HabitDao
import com.lightmark.data.local.dao.InboxDao
import com.lightmark.data.local.dao.TemplateDao
import com.lightmark.data.local.dao.TodoDao
import com.lightmark.data.local.dao.SmartListDao
import com.lightmark.data.local.entity.AlarmEntity
import com.lightmark.data.local.entity.CategoryEntity
import com.lightmark.data.local.entity.GoalEntity
import com.lightmark.data.local.entity.HabitCheckEntity
import com.lightmark.data.local.entity.HabitEntity
import com.lightmark.data.local.entity.InboxEntity
import com.lightmark.data.local.entity.TemplateEntity
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.data.local.entity.SmartListEntity

/**
 * 轻刻本地数据库
 * 用于离线缓存待办数据
 */
@Database(
    entities = [
        TodoEntity::class,
        CategoryEntity::class,
        InboxEntity::class,
        AlarmEntity::class,
        HabitEntity::class,
        HabitCheckEntity::class,
        GoalEntity::class,
        TemplateEntity::class,
        SmartListEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class LightMarkDatabase : RoomDatabase() {

    abstract fun inboxDao(): InboxDao
    abstract fun alarmDao(): AlarmDao

    abstract fun todoDao(): TodoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun habitDao(): HabitDao
    abstract fun templateDao(): TemplateDao
    abstract fun smartListDao(): SmartListDao

    companion object {
        const val DATABASE_NAME = "lightmark_db"

        @Volatile
        private var INSTANCE: LightMarkDatabase? = null

        fun create(context: android.content.Context): LightMarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    LightMarkDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
