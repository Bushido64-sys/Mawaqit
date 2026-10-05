package com.mawaqit.app.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Mecca coordinates. Primary source: OpenStreetMap Nominatim (free, no key).
 * Hardcoded fallback if offline / fail — Qibla must render everywhere.
 */
object MeccaLocator {
    private const val NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/search?q=Mecca&format=json&limit=1"

    private const val FALLBACK_LAT = 21.4225
    private const val FALLBACK_LON = 39.8262

    private val client by lazy { OkHttpClient() }
    private val gson by lazy { Gson() }

    suspend fun getMeccaLatLon(): Pair<Double, Double> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(NOMINATIM_URL)
                .header("User-Agent", "MawaqitPrayerApp/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use FALLBACK_LAT to FALLBACK_LON
                val body = response.body?.string() ?: return@use FALLBACK_LAT to FALLBACK_LON
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val list = gson.fromJson<List<Map<String, Any>>>(body, listType)
                val first = list?.firstOrNull() ?: return@use FALLBACK_LAT to FALLBACK_LON
                val lat = (first["lat"] as? String)?.toDoubleOrNull() ?: return@use FALLBACK_LAT to FALLBACK_LON
                val lon = (first["lon"] as? String)?.toDoubleOrNull() ?: return@use FALLBACK_LAT to FALLBACK_LON
                lat to lon
            }
        } catch (e: Exception) {
            FALLBACK_LAT to FALLBACK_LON
        }
    }
}
