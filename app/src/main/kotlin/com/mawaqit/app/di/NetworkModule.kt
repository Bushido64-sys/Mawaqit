package com.mawaqit.app.di

import com.mawaqit.app.data.api.AladhanApiService
import com.mawaqit.app.data.api.UmmahApiService
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Name-tag for the prayer-times (AlAdhan) Retrofit client. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AladhanRetrofit

/** Name-tag for the Quran (UmmahAPI) Retrofit client. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UmmahRetrofit

/** Name-tag for the countries/cities Retrofit client (PHASE-8.1). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CountriesNowRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val ALADHAN_BASE_URL = "https://api.aladhan.com/v1/"
    private const val UMMAH_BASE_URL = "https://ummahapi.com/api/"
    private const val COUNTRIES_NOW_BASE_URL = "https://countriesnow.space/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @AladhanRetrofit
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(ALADHAN_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()

    @Provides
    @Singleton
    fun provideAladhanApiService(@AladhanRetrofit retrofit: Retrofit): AladhanApiService =
        retrofit.create(AladhanApiService::class.java)

    @UmmahRetrofit
    @Provides
    @Singleton
    fun provideUmmahRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(UMMAH_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()

    @Provides
    @Singleton
    fun provideUmmahApiService(@UmmahRetrofit retrofit: Retrofit): UmmahApiService =
        retrofit.create(UmmahApiService::class.java)

    @CountriesNowRetrofit
    @Provides
    @Singleton
    fun provideCountriesNowRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(COUNTRIES_NOW_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()

    @Provides
    @Singleton
    fun provideCountriesNowApiService(@CountriesNowRetrofit retrofit: Retrofit): com.mawaqit.app.data.api.CountriesNowApiService =
        retrofit.create(com.mawaqit.app.data.api.CountriesNowApiService::class.java)

    // ⚠️ All Retrofit Builders inject the SAME OkHttpClient — fine, because the
    // OkHttp client itself holds no baseUrl. Separated into distinct Retrofit
    // instances only for the distinct base URLs (NetworkModule, PHASE_6/8.1).
}
