package com.lightmark.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 8,
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

        /** v7 → v8：新增私密标记列，保留原有数据（#97） */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN is_private INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN is_private INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: LightMarkDatabase? = null

        fun create(context: android.content.Context): LightMarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    LightMarkDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
