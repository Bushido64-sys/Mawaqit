package com.mawaqit.app.di

import android.content.Context
import androidx.room.Room
import com.mawaqit.app.data.db.AyahDao
import com.mawaqit.app.data.db.MawaqitDatabase
import com.mawaqit.app.data.db.PrayerTimeDao
import com.mawaqit.app.data.db.SalahLogDao
import com.mawaqit.app.data.db.SurahDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PHASE_2: Room database + DAO providers (per DATA_SCHEMA.md).
 * Later phases extend this module (Quran API service in PHASE_6, etc.).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MawaqitDatabase =
        Room.databaseBuilder(context, MawaqitDatabase::class.java, MawaqitDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePrayerTimeDao(db: MawaqitDatabase): PrayerTimeDao = db.prayerTimeDao()

    @Provides fun provideSalahLogDao(db: MawaqitDatabase): SalahLogDao = db.salahLogDao()

    @Provides fun provideSurahDao(db: MawaqitDatabase): SurahDao = db.surahDao()

    @Provides fun provideAyahDao(db: MawaqitDatabase): AyahDao = db.ayahDao()
}
