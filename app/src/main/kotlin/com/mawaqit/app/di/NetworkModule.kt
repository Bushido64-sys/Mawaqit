package com.mawaqit.app.di

import com.mawaqit.app.data.api.AladhanApiService
import com.mawaqit.app.data.api.UmmahApiService
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val ALADHAN_BASE_URL = "https://api.aladhan.com/v1/"
    private const val UMMAH_BASE_URL = "https://ummahapi.com/api/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

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
    fun provideAladhanApiService(retrofit: Retrofit): AladhanApiService =
        retrofit.create(AladhanApiService::class.java)

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
    fun provideUmmahApiService(retrofit: Retrofit): UmmahApiService =
        retrofit.create(UmmahApiService::class.java)

    // ⚠️ Both Retrofit Builders inject the SAME OkHttpClient — fine, because the
    // OkHttp client itself holds no baseUrl. Separated into distinct Retrofit
    // instances only for the two base URLs (NetworkModule, PHASE_6).
}
