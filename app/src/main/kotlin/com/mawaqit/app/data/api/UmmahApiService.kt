package com.mawaqit.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * UmmahAPI — Quran text, translations (API_REFERENCE.md §2, verified 2026-09-17).
 * There is NO random-ayah endpoint — Daily Ayah is composed per Endpoint 2.3.
 */
interface UmmahApiService {

    @GET("quran/surahs")
    suspend fun getAllSurahs(): UmmahSurahListResponse

    @GET("quran/surah/{number}")
    suspend fun getSurah(
        @Path("number") number: Int,
        @Query("translation") translation: String = "sahih_international,urdu"
    ): UmmahSurahDetailResponse
}
