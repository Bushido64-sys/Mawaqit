package com.mawaqit.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PrayerTimeEntity::class,
        SalahLogEntity::class,
        SurahEntity::class,
        AyahEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MawaqitDatabase : RoomDatabase() {
    abstract fun prayerTimeDao(): PrayerTimeDao
    abstract fun salahLogDao(): SalahLogDao
    abstract fun surahDao(): SurahDao
    abstract fun ayahDao(): AyahDao

    companion object {
        const val DATABASE_NAME = "mawaqit_db"
    }
}
