package com.mawaqit.app.di

import com.mawaqit.app.data.repository.AyahRepository
import com.mawaqit.app.data.repository.AyahRepositoryImpl
import com.mawaqit.app.data.repository.PrayerRepository
import com.mawaqit.app.data.repository.PrayerRepositoryImpl
import com.mawaqit.app.data.repository.QuranRepository
import com.mawaqit.app.data.repository.QuranRepositoryImpl
import com.mawaqit.app.data.repository.SalahRepository
import com.mawaqit.app.data.repository.SalahRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPrayerRepository(impl: PrayerRepositoryImpl): PrayerRepository

    @Binds
    @Singleton
    abstract fun bindSalahRepository(impl: SalahRepositoryImpl): SalahRepository

    @Binds
    @Singleton
    abstract fun bindAyahRepository(impl: AyahRepositoryImpl): AyahRepository

    @Binds
    @Singleton
    abstract fun bindQuranRepository(impl: QuranRepositoryImpl): QuranRepository
}
