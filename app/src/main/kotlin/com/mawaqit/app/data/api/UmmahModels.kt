package com.mawaqit.app.data.api

import com.google.gson.annotations.SerializedName

/**
 * UmmahAPI DTOs — paths/shapes verified live 2026-09-17 (API_REFERENCE.md §2).
 * Envelope for EVERY response: {"success": Boolean, "service": String, "data": ...}
 * Always check `success` in the JSON body, not just HTTP status.
 */
data class UmmahSurahListResponse(
    val success: Boolean = false,
    val data: UmmahSurahListData? = null
)

data class UmmahSurahListData(
    val total: Int = 0,
    val surahs: List<UmmahSurahDto>? = null
)

data class UmmahSurahDto(
    val number: Int = 0,
    @SerializedName("name_arabic") val nameArabic: String? = null,
    @SerializedName("name_english") val nameEnglish: String? = null,
    @SerializedName("name_translation") val nameTranslation: String? = null,
    @SerializedName("revelation_place") val revelationPlace: String? = null, // "makkah" | "madinah"
    @SerializedName("bismillah_pre") val bismillahPre: Boolean = true,
    @SerializedName("verses_count") val versesCount: Int = 0
)

data class UmmahSurahDetailResponse(
    val success: Boolean = false,
    val data: UmmahSurahDetailData? = null
)

data class UmmahSurahDetailData(
    val surah: UmmahSurahDto? = null,
    @SerializedName("total_verses") val totalVerses: Int = 0,
    val verses: List<UmmahVerseDto>? = null
)

data class UmmahVerseDto(
    @SerializedName("verse_key") val verseKey: String? = null, // "2:255"
    val ayah: Int = 0,
    val arabic: String? = null,
    val translations: UmmahTranslationsDto? = null
)

data class UmmahTranslationsDto(
    @SerializedName("sahih_international") val sahihInternational: String? = null,
    val urdu: String? = null
)
