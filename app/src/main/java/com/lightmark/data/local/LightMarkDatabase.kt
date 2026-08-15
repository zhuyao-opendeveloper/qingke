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
import com.lightmark.data.local.dao.MoodDao
import com.lightmark.data.local.entity.AlarmEntity
import com.lightmark.data.local.entity.CategoryEntity
import com.lightmark.data.local.entity.GoalEntity
import com.lightmark.data.local.entity.HabitCheckEntity
import com.lightmark.data.local.entity.HabitEntity
import com.lightmark.data.local.entity.InboxEntity
import com.lightmark.data.local.entity.TemplateEntity
import com.lightmark.data.local.entity.TodoEntity
import com.lightmark.data.local.entity.SmartListEntity
import com.lightmark.data.local.entity.MoodEntity

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
        SmartListEntity::class,
        MoodEntity::class
    ],
    version = 11,
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
    abstract fun moodDao(): MoodDao

    companion object {
        const val DATABASE_NAME = "lightmark_db"

        /** v7 → v8：新增私密标记列，保留原有数据（#97） */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN is_private INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN is_private INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v8 → v9：新增手动排序序号列，保留原有数据（#32） */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN manual_order INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v9 → v10：新增精力/依赖/双向链接/附件列、分类父级、moods 表（Wave 6，保留旧数据） */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN energy TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE todos ADD COLUMN blockedByTaskId TEXT")
                db.execSQL("ALTER TABLE todos ADD COLUMN linkedTaskIds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todos ADD COLUMN attachments TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE categories ADD COLUMN parentId TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS moods (
                        id TEXT NOT NULL PRIMARY KEY,
                        score INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        /** v10 → v11：新增任务备注/预计耗时、习惯暂停（#7 / #87 / #93，保留旧数据） */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todos ADD COLUMN estimated_minutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN paused INTEGER NOT NULL DEFAULT 0")
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
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
