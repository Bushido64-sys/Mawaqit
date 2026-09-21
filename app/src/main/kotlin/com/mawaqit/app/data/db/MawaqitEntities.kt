package com.mawaqit.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * prayer_times — caches prayer times for the entire current month.
 * Fetched once monthly from AlAdhan (PHASE_2).
 *
 * DATE RULE (DATA_SCHEMA.md): `date` is ISO YYYY-MM-DD. AlAdhan returns
 * DD-MM-YYYY — convert at the parsing boundary BEFORE inserting. Lexicographic
 * string comparison is only correct in ISO order.
 *
 * Two helpers beyond the doc's DAO list (additive, needed by PHASE_2 fallback):
 * getLatest() — "cached data" banner path; deleteAll() — location-change invalidation.
 */
@Entity(tableName = "prayer_times")
data class PrayerTimeEntity(
    @PrimaryKey val date: String,   // "2026-09-15" (ISO)
    val fajr: String,               // "04:38"
    val dhuhr: String,              // "12:12"
    val asr: String,                // "15:31"
    val maghrib: String,            // "18:27"
    val isha: String,               // "19:46"
    val latitude: Double,
    val longitude: Double,
    val fetchedAt: Long             // System.currentTimeMillis() at insert
)

@Dao
interface PrayerTimeDao {

    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    suspend fun getPrayerTimeByDate(date: String): PrayerTimeEntity?

    /** Inclusive date-range read (ISO dates ⇒ lexicographic == chronological).
     *  Used by PHASE-3.1's AlarmRefreshManager to plan the next N days. */
    @Query("SELECT * FROM prayer_times WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getPrayerTimesBetween(startDate: String, endDate: String): List<PrayerTimeEntity>

    @Query("SELECT * FROM prayer_times WHERE date LIKE :monthPattern")
    suspend fun getPrayerTimesForMonth(monthPattern: String): List<PrayerTimeEntity>
    // monthPattern example: "2026-09-%"

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prayerTimes: List<PrayerTimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prayerTime: PrayerTimeEntity)

    @Query("DELETE FROM prayer_times WHERE date LIKE :monthPattern")
    suspend fun deleteMonth(monthPattern: String)

    @Query("SELECT COUNT(*) FROM prayer_times WHERE date LIKE :monthPattern")
    suspend fun countForMonth(monthPattern: String): Int

    /** Most recent day stored — serves as fallback when today's row is missing. */
    @Query("SELECT * FROM prayer_times ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): PrayerTimeEntity?

    /** Location change invalidates ALL cached rows (DATA_SCHEMA.md refresh triggers). */
    @Query("DELETE FROM prayer_times")
    suspend fun deleteAll()
}

/**
 * salah_log — which prayers the user marked as prayed each day.
 * (Written from PHASE_4's UI; the table exists from Phase 2 so the schema
 * is stable. Cleanup: keep only the last 30 days.)
 */
@Entity(
    tableName = "salah_log",
    primaryKeys = ["date", "prayer"],
    indices = [androidx.room.Index(value = ["date"])]
)
data class SalahLogEntity(
    val date: String,        // "2026-09-15" (ISO)
    val prayer: String,      // "FAJR" | "DHUHR" | "ASR" | "MAGHRIB" | "ISHA"
    val prayed: Boolean,
    val markedAt: Long?      // timestamp when marked, null if not marked
)

@Dao
interface SalahLogDao {

    @Query("SELECT * FROM salah_log WHERE date = :date")
    fun getSalahLogForDate(date: String): Flow<List<SalahLogEntity>>

    @Query("SELECT * FROM salah_log WHERE date = :date AND prayer = :prayer LIMIT 1")
    suspend fun getSalahStatus(date: String, prayer: String): SalahLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: SalahLogEntity)

    @Query("DELETE FROM salah_log WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
    // SAFE only because dates are ISO (lexicographic == chronological).
    // cutoffDate = LocalDate.now().minusDays(30) formatted ISO.
}

/**
 * surah_list — metadata for all 114 Surahs (no ayah text).
 * Fetched once (PHASE_6) and never again — static data.
 */
@Entity(tableName = "surah_list")
data class SurahEntity(
    @PrimaryKey val number: Int,            // 1–114
    val nameArabic: String,                 // "الفاتحة"
    val nameEnglish: String,                // "Al-Fatihah"
    val nameMeaning: String,                // "The Opening"
    val numberOfAyahs: Int,                 // 7
    val revelationType: String              // "Meccan" | "Medinan"
)

@Dao
interface SurahDao {

    @Query("SELECT * FROM surah_list ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT COUNT(*) FROM surah_list")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surahs: List<SurahEntity>)

    @Query("SELECT * FROM surah_list WHERE nameEnglish LIKE '%' || :query || '%' OR nameArabic LIKE '%' || :query || '%'")
    fun searchSurahs(query: String): Flow<List<SurahEntity>>
}

/**
 * ayah_cache — individual ayahs, lazily populated when the user opens a Surah
 * (PHASE_6). Never invalidated after fetch — Quran text is immutable.
 */
@Entity(
    tableName = "ayah_cache",
    primaryKeys = ["surahNumber", "ayahNumber"]
)
data class AyahEntity(
    val surahNumber: Int,      // 1–114
    val ayahNumber: Int,       // 1-based position within surah
    val arabicText: String,
    val translationEn: String,
    val translationUr: String,
    val cachedAt: Long
)

@Dao
interface AyahDao {

    @Query("SELECT * FROM ayah_cache WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    fun getAyahsForSurah(surahNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT COUNT(*) FROM ayah_cache WHERE surahNumber = :surahNumber")
    suspend fun countAyahsForSurah(surahNumber: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ayahs: List<AyahEntity>)

    @Query("DELETE FROM ayah_cache WHERE surahNumber = :surahNumber")
    suspend fun deleteSurah(surahNumber: Int)
}

/**
 * reading_progress (PHASE-6.3) — one row per opened Surah: where the user
 * stopped, the furthest ayah reached (auto-read progress), and the pinned
 * bookmark. Pages are font-size dependent, so progress is stored in AYAHS;
 * the reader maps ayah → page after every re-fit. Entirely local data.
 */
@Entity(tableName = "reading_progress", primaryKeys = ["surahNumber"])
data class ReadingProgressEntity(
    val surahNumber: Int,      // 1–114
    val lastAyah: Int,         // resume target — first ayah of the last page
    val furthestAyah: Int,     // highest ayah reached — drives progress %
    val lastReadAt: Long,      // ordering for the "Continue Reading" card
    val completed: Boolean,    // furthestAyah >= numberOfAyahs
    val completedAt: Long?,    // when it was completed (future celebrations)
    val bookmarkAyah: Int      // 0 = no pin; else first ayah of the pinned page
)

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress ORDER BY lastReadAt DESC")
    fun getAll(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE surahNumber = :surahNumber LIMIT 1")
    fun getForSurah(surahNumber: Int): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE surahNumber = :surahNumber LIMIT 1")
    suspend fun getForSurahOnce(surahNumber: Int): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)
}
