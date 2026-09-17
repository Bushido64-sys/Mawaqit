package com.mawaqit.app.di

import com.mawaqit.app.data.repository.PrayerRepository
import com.mawaqit.app.data.repository.PrayerRepositoryImpl
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
}
