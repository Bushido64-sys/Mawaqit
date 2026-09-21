package com.mawaqit.app.data.repository

import com.mawaqit.app.data.db.ReadingProgressDao
import com.mawaqit.app.data.db.ReadingProgressEntity
import com.mawaqit.app.data.db.SurahDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Progress info for one surah, computed for the UI (percentages resolved). */
data class SurahProgress(
    val surahNumber: Int,
    val nameEnglish: String,
    val nameArabic: String,
    val totalAyahs: Int,
    val lastAyah: Int,
    val furthestAyah: Int,
    val bookmarkAyah: Int,
    val completed: Boolean,
    val lastReadAt: Long
) {
    /** 0..100 — how far through THIS surah the user has read. */
    val percent: Int get() = if (totalAyahs <= 0) 0 else ((furthestAyah * 100) / totalAyahs).coerceIn(0, 100)
}

/**
 * PHASE-6.5 — the surah that blocks a later one in the read-in-order chain
 * (the first, lowest-numbered surah that is not completed). Drives the
 * reader's "finish X first" lock popup.
 */
data class SurahBlocker(val surahNumber: Int, val nameEnglish: String)

/**
 * Aggregated Quran-wide reading progress for the continue card.
 *
 * PHASE-6.4 — `continueSurah` follows the last MARKED surah (browsing other
 * surahs never changes it). When every touched surah is finished, the card
 * switches to "Up next": [continueSurah] becomes the surah AFTER the newest
 * completed one and [upNext] is true.
 */
data class QuranProgress(
    val continueSurah: SurahProgress?,   // last-marked surah, or the synthetic up-next row
    val upNext: Boolean,                 // true → card header "Up next", body references lastCompletedName
    val lastCompletedName: String,       // English name of the newest finished surah ("" when not up-next)
    val readAyahs: Int,                  // sum of furthest ayahs across surahs
    val totalAyahs: Int,                 // sum of ayahs of all 114 surahs
    val completedCount: Int
) {
    /** 0..100 — whole-Quran lifetime progress. */
    val percent: Int get() = if (totalAyahs <= 0) 0 else ((readAyahs * 100) / totalAyahs).coerceIn(0, 100)
}

/**
 * PHASE-6.3 "Keep My Place" + PHASE-6.4 "Only the Button Counts" — the
 * reading-progress memory. The ONLY writer is [markPage] (the reader's
 * "Mark as read" button); opening or swiping a surah writes nothing, so
 * browsing can never disturb the Continue card.
 *
 * Positions are stored in AYAH numbers (font-size independent); the reader
 * maps ayah → page after each re-fit. All local Room data.
 */
interface ReadingProgressRepository {
    /** Live progress rows, newest marked first. */
    fun getAll(): Flow<List<ReadingProgressEntity>>

    /** Live progress for one surah (available for Phase 8 settings). */
    fun getForSurah(surahNumber: Int): Flow<ReadingProgressEntity?>

    /** One-shot read for resume-on-open. */
    suspend fun getForSurahOnce(surahNumber: Int): ReadingProgressEntity?

    /**
     * PHASE-6.4 — mark the page at [pageFirstAyah]..[pageLastAyah] as read:
     * the resume point moves to [pageFirstAyah], furthest advances to the
     * running max (re-marking an early page never regresses progress), and
     * completion is judged against [totalAyahs]. Also updates lastReadAt,
     * which is what the Continue card follows.
     */
    suspend fun markPage(surahNumber: Int, pageFirstAyah: Int, pageLastAyah: Int, totalAyahs: Int)

    /** Continue-card data: last-marked (or up-next) surah + Quran-wide totals. */
    suspend fun getQuranProgress(): QuranProgress

    /**
     * PHASE-6.5 read-in-order gate — the FIRST (lowest-numbered) surah before
     * [beforeSurahNumber] that is not completed, or null when every earlier
     * surah is done. A missing row counts as NOT completed, so a fresh reader
     * starts locked to Al-Fatihah (surah 1 is always unlocked) and each next
     * surah unlocks exactly when the previous one is 100% finished.
     */
    suspend fun findBlockerSurah(beforeSurahNumber: Int): SurahBlocker?
}

@Singleton
class ReadingProgressRepositoryImpl @Inject constructor(
    private val progressDao: ReadingProgressDao,
    private val surahDao: SurahDao
) : ReadingProgressRepository {

    override fun getAll(): Flow<List<ReadingProgressEntity>> = progressDao.getAll()

    override fun getForSurah(surahNumber: Int): Flow<ReadingProgressEntity?> =
        progressDao.getForSurah(surahNumber)

    override suspend fun getForSurahOnce(surahNumber: Int): ReadingProgressEntity? =
        progressDao.getForSurahOnce(surahNumber)

    override suspend fun markPage(surahNumber: Int, pageFirstAyah: Int, pageLastAyah: Int, totalAyahs: Int) {
        val existing = progressDao.getForSurahOnce(surahNumber)
        val newFurthest = maxOf(existing?.furthestAyah ?: 0, pageLastAyah)
        val now = System.currentTimeMillis()
        val completed = totalAyahs > 0 && newFurthest >= totalAyahs
        progressDao.upsert(
            ReadingProgressEntity(
                surahNumber = surahNumber,
                lastAyah = pageFirstAyah,
                furthestAyah = newFurthest,
                lastReadAt = now,
                completed = completed,
                completedAt = when {
                    completed && existing?.completedAt == null -> now
                    completed -> existing?.completedAt
                    else -> null
                },
                bookmarkAyah = existing?.bookmarkAyah ?: 0  // legacy column (PHASE-6.3), no longer written
            )
        )
    }

    override suspend fun findBlockerSurah(beforeSurahNumber: Int): SurahBlocker? {
        if (beforeSurahNumber <= 1) return null
        val rowsBySurah = progressDao.getAll().first()
            .filter { it.surahNumber < beforeSurahNumber }
            .associateBy { it.surahNumber }
        val blockerNumber = (1 until beforeSurahNumber).firstOrNull { n ->
            rowsBySurah[n]?.completed != true
        } ?: return null
        val name = surahDao.getAllSurahs().first()
            .find { it.number == blockerNumber }?.nameEnglish.orEmpty()
        return SurahBlocker(blockerNumber, name)
    }

    override suspend fun getQuranProgress(): QuranProgress {
        val surahs = surahDao.getAllSurahs().first()
        val rows = progressDao.getAll().first()
        val totalAyahs = surahs.sumOf { it.numberOfAyahs }
        val readAyahs = rows.sumOf { row ->
            val total = surahs.find { it.number == row.surahNumber }?.numberOfAyahs ?: 0
            minOf(row.furthestAyah, total)
        }
        val completedCount = rows.count { it.completed }

        // Continue target: the newest NOT-completed surah the user marked.
        // When everything touched is finished → "Up next" = the surah after
        // the newest completed one (the disappearing-card bug fix).
        val newest = rows.maxByOrNull { it.lastReadAt }
            ?: return QuranProgress(null, false, "", readAyahs, totalAyahs, completedCount)
        val inProgress = rows.filter { !it.completed }.maxByOrNull { it.lastReadAt }

        if (inProgress != null) {
            val continueSurah = surahs.find { it.number == inProgress.surahNumber }?.let { s ->
                SurahProgress(
                    surahNumber = s.number,
                    nameEnglish = s.nameEnglish,
                    nameArabic = s.nameArabic,
                    totalAyahs = s.numberOfAyahs,
                    lastAyah = inProgress.lastAyah,
                    furthestAyah = inProgress.furthestAyah,
                    bookmarkAyah = inProgress.bookmarkAyah,
                    completed = inProgress.completed,
                    lastReadAt = inProgress.lastReadAt
                )
            }
            return QuranProgress(continueSurah, false, "", readAyahs, totalAyahs, completedCount)
        }

        // Up-next mode: everything marked is completed.
        val lastCompletedName = surahs.find { it.number == newest.surahNumber }?.nameEnglish ?: ""
        if (newest.surahNumber >= 114) {
            // Whole Quran finished — card hides until a future celebration phase.
            return QuranProgress(null, true, lastCompletedName, readAyahs, totalAyahs, completedCount)
        }
        val upNextSurah = surahs.find { it.number == minOf(newest.surahNumber + 1, 114) }?.let { s ->
            SurahProgress(
                surahNumber = s.number,
                nameEnglish = s.nameEnglish,
                nameArabic = s.nameArabic,
                totalAyahs = s.numberOfAyahs,
                lastAyah = 1,
                furthestAyah = 0,
                bookmarkAyah = 0,
                completed = false,
                lastReadAt = 0
            )
        }
        return QuranProgress(upNextSurah, true, lastCompletedName, readAyahs, totalAyahs, completedCount)
    }
}
