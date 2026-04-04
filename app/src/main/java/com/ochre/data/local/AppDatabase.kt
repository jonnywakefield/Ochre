package com.ochre.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ochre.data.local.dao.AloneDao
import com.ochre.data.local.dao.EventDao
import com.ochre.data.local.dao.FoodDao
import com.ochre.data.local.dao.ReminderDao
import com.ochre.data.local.dao.WalkDao
import com.ochre.data.local.entity.AloneSessionEntity
import com.ochre.data.local.entity.EventEntity
import com.ochre.data.local.entity.FoodStockEntity
import com.ochre.data.local.entity.MealScheduleEntity
import com.ochre.data.local.entity.ReminderEntity
import com.ochre.data.local.entity.WalkScheduleConfigEntity
import com.ochre.data.local.entity.WalkScheduleEntryEntity
import com.ochre.data.local.entity.WalkSessionEntity

@Database(
    entities = [
        EventEntity::class,
        WalkSessionEntity::class,
        WalkScheduleEntryEntity::class,
        WalkScheduleConfigEntity::class,
        AloneSessionEntity::class,
        MealScheduleEntity::class,
        FoodStockEntity::class,
        ReminderEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val eventDao: EventDao
    abstract val walkDao: WalkDao
    abstract val aloneDao: AloneDao
    abstract val foodDao: FoodDao
    abstract val reminderDao: ReminderDao

    companion object {
        const val DATABASE_NAME = "ochre_db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE walk_sessions ADD COLUMN peeEventsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS walk_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startMillis INTEGER NOT NULL,
                        endMillis INTEGER,
                        pooEventsJson TEXT NOT NULL DEFAULT ''
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS walk_schedule_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        targetHour INTEGER NOT NULL,
                        targetMinute INTEGER NOT NULL,
                        toleranceMinutes INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS walk_schedule_config (
                        id INTEGER PRIMARY KEY NOT NULL,
                        maxGapMinutes INTEGER NOT NULL,
                        quietFromHour INTEGER NOT NULL,
                        quietFromMinute INTEGER NOT NULL,
                        quietToHour INTEGER NOT NULL,
                        quietToMinute INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS alone_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startMillis INTEGER NOT NULL,
                        endMillis INTEGER
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_schedule (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        targetHour INTEGER NOT NULL,
                        targetMinute INTEGER NOT NULL,
                        windowMinutes INTEGER NOT NULL,
                        defaultGrams INTEGER NOT NULL,
                        varyAmount INTEGER NOT NULL,
                        minGrams INTEGER NOT NULL,
                        maxGrams INTEGER NOT NULL,
                        randomReminderEnabled INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS food_stock (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestampMillis INTEGER NOT NULL,
                        deltaGrams INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        timestampMillis INTEGER NOT NULL,
                        notifyBeforeMinutes INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
