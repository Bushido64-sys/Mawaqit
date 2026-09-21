package com.mawaqit.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PrayerTimeEntity::class,
        SalahLogEntity::class,
        SurahEntity::class,
        AyahEntity::class,
        ReadingProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MawaqitDatabase : RoomDatabase() {
    abstract fun prayerTimeDao(): PrayerTimeDao
    abstract fun salahLogDao(): SalahLogDao
    abstract fun surahDao(): SurahDao
    abstract fun ayahDao(): AyahDao
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        const val DATABASE_NAME = "mawaqit_db"

        /**
         * PHASE-6.3: adds the reading_progress table. Purely additive — no
         * existing table is touched, so prayer logs, cached surahs and all
         * other user data survive the upgrade intact.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_progress (
                        surahNumber INTEGER NOT NULL PRIMARY KEY,
                        lastAyah INTEGER NOT NULL,
                        furthestAyah INTEGER NOT NULL,
                        lastReadAt INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        completedAt INTEGER,
                        bookmarkAyah INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
