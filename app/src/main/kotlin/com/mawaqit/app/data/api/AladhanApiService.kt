package com.mawaqit.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * AlAdhan API — https://api.aladhan.com/v1 (no key needed).
 * method=1 (University of Islamic Sciences, Karachi), school=1 (Hanafi Asr)
 * are the project-wide constants (API_REFERENCE.md).
 */
interface AladhanApiService {

    /** PREFERRED — monthly calendar, fetched once per month and cached in Room. */
    @GET("calendar/{year}/{month}")
    suspend fun getMonthlyCalendar(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 1,
        @Query("school") school: Int = 1
    ): AladhanCalendarResponse

    /** Fallback — single day by city name (manual city entry). */
    @GET("timingsByCity/{date}")
    suspend fun getTimingsByCity(
        @Path("date") date: String,   // DD-MM-YYYY
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int = 1,
        @Query("school") school: Int = 1
    ): AladhanDayResponse
}
