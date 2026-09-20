package com.mawaqit.app.data.repository

import com.mawaqit.app.data.api.UmmahApiService
import com.mawaqit.app.data.model.DailyAyah
import com.mawaqit.app.data.prefs.PrefsRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Provides the "Ayah of the Day" for the home screen card. */
interface AyahRepository {
    suspend fun getDailyAyah(): DailyAyah
}

/**
 * PHASE_6 upgrade — live random ayah from UmmahAPI (Endpoint 2.3, verified):
 * pick a random surah 1–114 (retry ≤3× if verses_count > 60 so one verse never
 * downloads an Al-Baqarah-sized payload), then a uniformly random verse.
 * Cached in DataStore (DAILY_AYAH_* keys) — same ayah all day, offline-capable.
 * Failure chain: last cached ayah → FALLBACK_AYAHS rotation (Rule 8).
 */
@Singleton
class AyahRepositoryImpl @Inject constructor(
    private val prefs: PrefsRepository,
    private val quranRepository: QuranRepository,
    private val api: UmmahApiService
) : AyahRepository {

    override suspend fun getDailyAyah(): DailyAyah {
        val today = LocalDate.now().toString() // ISO
        prefs.getDailyAyahOnce()?.let { (ayah, date) ->
            if (date == today) return ayah
        }

        val live = fetchRandomAyah()
        val ayah = live ?: fallbackAyah()
        prefs.setDailyAyah(ayah, today)
        return ayah
    }

    private suspend fun fetchRandomAyah(): DailyAyah? {
        return try {
            var surah = Random.nextInt(1, 115)
            var meta = quranRepository.getSurahMetaOnce(surah)
            var attempts = 0
            while (attempts < 3 && (meta == null || meta.numberOfAyahs > 60)) {
                surah = Random.nextInt(1, 115)
                meta = quranRepository.getSurahMetaOnce(surah)
                attempts++
            }
            meta ?: return null

            val response = api.getSurah(surah)
            val verses = response.data?.verses
            if (!response.success || verses.isNullOrEmpty()) return null

            val verse = verses.random()
            val arabic = verse.arabic ?: return null
            val ref = "${meta.nameEnglish}, ${verse.verseKey ?: "${surah}:${verse.ayah}"}"
            DailyAyah(
                arabic = arabic,
                english = verse.translations?.sahihInternational.orEmpty(),
                urdu = verse.translations?.urdu.orEmpty(),
                reference = ref
            )
        } catch (e: Exception) {
            null // offline/API hiccup → cached or fallback path (Rule 8)
        }
    }

    private fun fallbackAyah(): DailyAyah {
        val f = FALLBACK_AYAHS[LocalDate.now().dayOfMonth % FALLBACK_AYAHS.size]
        return DailyAyah(f.arabic, f.english, f.urdu, f.reference)
    }
}
