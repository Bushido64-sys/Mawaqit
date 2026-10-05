package com.mawaqit.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mecca coordinates. Primary source: OpenStreetMap Nominatim (free, no key).
 * Hardcoded fallback if offline / fail — Qibla must render everywhere.
 */
@Singleton
class MeccaLocator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private const val NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/search?q=Mecca&format=json&limit=1"

    private const val FALLBACK_LAT = 21.4225
    private const val FALLBACK_LON = 39.8262

    private val client by lazy { OkHttpClient() }
    private val gson by lazy { Gson() }

    suspend fun getMeccaLatLon(): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val cache = context.getSharedPreferences("mecca_cache", 0)
        cache.getString("lat", null)?.let { latStr ->
            cache.getString("lon", null)?.let { lonStr ->
                latStr.toDoubleOrNull()?.let { lat ->
                    lonStr.toDoubleOrNull()?.let { lon -> return@withContext lat to lon }
                }
            }
        }
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
                cache.edit().putString("lat", lat.toString()).putString("lon", lon.toString()).apply()
                lat to lon
            }
        } catch (e: Exception) {
            FALLBACK_LAT to FALLBACK_LON
        }
    }
}
