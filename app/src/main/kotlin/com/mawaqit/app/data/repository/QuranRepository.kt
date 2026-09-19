package com.mawaqit.app.data.repository

import com.mawaqit.app.data.api.UmmahApiService
import com.mawaqit.app.data.api.UmmahSurahDto
import com.mawaqit.app.data.db.AyahEntity
import com.mawaqit.app.data.db.MawaqitDatabase
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.data.model.SurahDetail
import com.mawaqit.app.data.prefs.PrefsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Quran data (PHASE_6) — offline-first per Rule 8: DB first, UmmahAPI second. */
interface QuranRepository {
    /** Live list of all Surahs; fetches + caches once if the DB is empty. */
    fun getAllSurahs(): Flow<List<SurahEntity>>

    /** Search English + Arabic names (SurahDao.searchSurahs). */
    fun searchSurahs(query: String): Flow<List<SurahEntity>>

    /** Full Surah for the reading screen; fetches + caches if not yet stored. */
    suspend fun getSurahDetail(number: Int): SurahDetail

    /** One-shot metadata lookup (random-ayah picker, detail header). */
    suspend fun getSurahMetaOnce(number: Int): SurahEntity?

    /** Bootstrap the 114-surah list from UmmahAPI if the DB is empty. */
    suspend fun ensureSurahListLoaded(): Boolean
}

@Singleton
class QuranRepositoryImpl @Inject constructor(
    private val db: MawaqitDatabase,
    private val api: UmmahApiService,
    private val prefs: PrefsRepository
) : QuranRepository {

    override fun getAllSurahs(): Flow<List<SurahEntity>> =
        db.surahDao().getAllSurahs()

    override fun searchSurahs(query: String): Flow<List<SurahEntity>> =
        db.surahDao().searchSurahs(query)

    /**
     * Checks whether the Surah shows the Bismillah header BEFORE returning.
     * Rules (API_REFERENCE.md Endpoint 2.2): flag comes from the live payload;
     * if the flag is unknown, synthesize from the DataStore list learned from
     * previous fetches, else default "show except 1 & 9".
     */
    override suspend fun getSurahDetail(number: Int): SurahDetail {
        val ayahs = loadAyahs(number)

        // After a live fetch the flag is authoritative; a pure-DB read falls
        // back to the learned list, then to the verified default (1 & 9 → false).
        val bismillahPre = if (ayahs.second) {
            ayahs.first.bismillahPre
        } else {
            number !in prefs.getSurahsWithoutBismillahOnce() &&
                number != 1 && number != 9
        }

        return SurahDetail(
            surah = getSurahMetaOnce(number),
            ayahs = db.ayahDao().getAyahsForSurah(number).first(),
            bismillahPre = bismillahPre
        )
    }

    override suspend fun getSurahMetaOnce(number: Int): SurahEntity? =
        db.surahDao().getAllSurahs().first().find { it.number == number }

    /**
     * Returns (result, cameFromNetwork) — network path also refreshes the
     * surah_list metadata row and the learned bismillah flags.
     * `null` result = fetch failed AND nothing cached (caller shows error).
     */
    private suspend fun loadAyahs(number: Int): Pair<UmmahSurahDto, Boolean> {
        // Rule 8: cached text is served instantly — Quran text is immutable.
        if (db.ayahDao().countAyahsForSurah(number) > 0) {
            return UmmahSurahDto(number = number) to false
        }
        return try {
            val response = api.getSurah(number)
            val payload = response.data
            if (!response.success || payload == null || payload.verses.isNullOrEmpty()) {
                throw IllegalStateException("UmmahAPI returned no verses for $number")
            }
            val surahMeta = payload.surah
            val entities = payload.verses.mapNotNull { v ->
                val arabic = v.arabic ?: return@mapNotNull null
                if (v.ayah <= 0) return@mapNotNull null
                AyahEntity(
                    surahNumber = number,
                    ayahNumber = v.ayah,
                    arabicText = arabic,
                    translationEn = v.translations?.sahihInternational.orEmpty(),
                    translationUr = v.translations?.urdu.orEmpty(),
                    cachedAt = System.currentTimeMillis()
                )
            }
            if (entities.isEmpty()) throw IllegalStateException("No parseable verses for $number")
            db.ayahDao().insertAll(entities)

            // Keep metadata fresh (covers an offline install where the list
            // fetch failed but this surah fetch succeeded).
            if (surahMeta != null) {
                upsertSurahMeta(listOf(surahMeta))
                if (!surahMeta.bismillahPre) {
                    prefs.setSurahsWithoutBismillah(
                        prefs.getSurahsWithoutBismillahOnce() + number
                    )
                }
            }
            (surahMeta ?: UmmahSurahDto(number = number)) to true
        } catch (e: Exception) {
            if (db.ayahDao().countAyahsForSurah(number) > 0) {
                UmmahSurahDto(number = number) to false // race: cached meanwhile
            } else {
                throw e // nothing cached, network failed → caller shows error
            }
        }
    }

    /** UmmahAPI list DTOs → Room rows (mapping per API_REFERENCE.md Endpoint 2.1). */
    private suspend fun upsertSurahMeta(dtos: List<UmmahSurahDto>) {
        val entities = dtos.mapNotNull { d ->
            val n = d.number
            if (n !in 1..114) return@mapNotNull null
            SurahEntity(
                number = n,
                nameArabic = d.nameArabic.orEmpty(),
                nameEnglish = d.nameEnglish.orEmpty(),
                nameMeaning = d.nameTranslation.orEmpty(),
                numberOfAyahs = d.versesCount,
                revelationType = when (d.revelationPlace) {
                    "makkah" -> "Meccan"
                    "madinah" -> "Medinan"
                    else -> ""
                }
            )
        }
        if (entities.isNotEmpty()) db.surahDao().insertAll(entities)
    }

    /** One-time list bootstrap: fetch all 114 metadata rows if DB is empty. */
    override suspend fun ensureSurahListLoaded(): Boolean {
        if (db.surahDao().count() > 0) return true
        return try {
            val response = api.getAllSurahs()
            val surahs = response.data?.surahs
            if (!response.success || surahs.isNullOrEmpty()) {
                false
            } else {
                upsertSurahMeta(surahs)
                // Learn the full no-bismillah set from the authoritative list.
                val noBismillah = surahs.filter { !it.bismillahPre }.map { it.number }.toSet()
                if (noBismillah.isNotEmpty()) {
                    prefs.setSurahsWithoutBismillah(noBismillah)
                }
                true
            }
        } catch (e: Exception) {
            false // offline first launch → empty list + error state, retry on open
        }
    }
}
