package com.mawaqit.app.data.repository

import com.mawaqit.app.data.model.DailyAyah
import com.mawaqit.app.data.prefs.PrefsRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Provides the "Ayah of the Day" for the home screen card. */
interface AyahRepository {
    suspend fun getDailyAyah(): DailyAyah
}

/**
 * PHASE_4: rotates the curated FALLBACK_AYAHS list (day of month % 10) and
 * caches the pick in DataStore (DAILY_AYAH_* keys, DATA_SCHEMA.md) so the same
 * ayah shows all day, offline — no network dependency this phase.
 *
 * ponytail: the UmmahAPI fetch (API_REFERENCE.md Endpoint 2.3) lands in PHASE_6
 * together with the rest of the Quran stack — building the service+DTOs twice
 * would be waste. Fallback-first also satisfies Rule 8 (offline-first).
 */
@Singleton
class AyahRepositoryImpl @Inject constructor(
    private val prefs: PrefsRepository
) : AyahRepository {

    override suspend fun getDailyAyah(): DailyAyah {
        val today = LocalDate.now().toString() // ISO
        val cached = prefs.getDailyAyahOnce()
        if (cached != null && cached.second == today) return cached.first

        val fallback = FALLBACK_AYAHS[LocalDate.now().dayOfMonth % FALLBACK_AYAHS.size]
        val ayah = DailyAyah(fallback.arabic, fallback.english, fallback.urdu, fallback.reference)
        prefs.setDailyAyah(ayah, today)
        return ayah
    }
}
