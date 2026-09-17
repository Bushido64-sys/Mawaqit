package com.mawaqit.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Phase 1: intentionally empty.
 *
 * The guidebook's original AppModule provided the Room database here, but the
 * database classes (MawaqitDatabase, DAOs) do not exist until PHASE_2 — providing
 * them now would not compile. Providers will be added incrementally:
 *   PHASE_2 → provideDatabase(...) + DAO providers (per DATA_SCHEMA.md)
 *   PHASE_2 → NetworkModule: Retrofit + OkHttp + API services (per API_REFERENCE.md)
 *   PHASE_6 → Quran API service wiring
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
