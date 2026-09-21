package com.mawaqit.app.data.repository

import com.mawaqit.app.data.db.ReadingProgressDao
import com.mawaqit.app.data.db.ReadingProgressEntity
import com.mawaqit.app.data.db.SurahDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

/** Aggregated Quran-wide reading progress for the continue card. */
data class QuranProgress(
    val continueSurah: SurahProgress?,  // most recently read, not-completed surah
    val readAyahs: Int,                 // sum of furthest ayahs across surahs
    val totalAyahs: Int,                // sum of ayahs of all 114 surahs
    val completedCount: Int
) {
    /** 0..100 — whole-Quran lifetime progress. */
    val percent: Int get() = if (totalAyahs <= 0) 0 else ((readAyahs * 100) / totalAyahs).coerceIn(0, 100)
}

/**
 * PHASE-6.3 "Keep My Place" — the reading-progress memory.
 * Stores resume/bookmark positions in AYAH numbers (font-size independent);
 * the reader maps ayah → page after each re-fit. All local Room data.
 */
interface ReadingProgressRepository {
    /** Live progress rows, newest read first. */
    fun getAll(): Flow<List<ReadingProgressEntity>>

    /** Live progress for one surah (reader screen). */
    fun getForSurah(surahNumber: Int): Flow<ReadingProgressEntity?>

    /** One-shot read for resume-on-open. */
    suspend fun getForSurahOnce(surahNumber: Int): ReadingProgressEntity?

    /**
     * Record a page turn: [lastAyah] = first ayah of the new page,
     * [furthestAyah] = first ayah of the highest page reached.
     * `furthest = max(existing, furthestAyah)` so re-reading never regresses.
     * Surah completion is judged against [totalAyahs].
     */
    suspend fun recordProgress(surahNumber: Int, lastAyah: Int, furthestAyah: Int, totalAyahs: Int)

    /** Pin/unpin the bookmark (0 = unpin). */
    suspend fun setBookmark(surahNumber: Int, ayahNumber: Int)

    /** Continue-card data: newest in-progress surah + Quran-wide totals. */
    suspend fun getQuranProgress(): QuranProgress
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

    override suspend fun recordProgress(surahNumber: Int, lastAyah: Int, furthestAyah: Int, totalAyahs: Int) {
        val existing = progressDao.getForSurahOnce(surahNumber)
        val newFurthest = maxOf(existing?.furthestAyah ?: 0, furthestAyah)
        val now = System.currentTimeMillis()
        val completed = totalAyahs > 0 && newFurthest >= totalAyahs
        progressDao.upsert(
            ReadingProgressEntity(
                surahNumber = surahNumber,
                lastAyah = lastAyah,
                furthestAyah = newFurthest,
                lastReadAt = now,
                completed = completed,
                completedAt = when {
                    completed && existing?.completedAt == null -> now
                    completed -> existing?.completedAt
                    else -> null
                },
                bookmarkAyah = existing?.bookmarkAyah ?: 0
            )
        )
    }

    override suspend fun setBookmark(surahNumber: Int, ayahNumber: Int) {
        val existing = progressDao.getForSurahOnce(surahNumber)
        if (existing == null && ayahNumber == 0) return // nothing to unpin
        progressDao.upsert(
            ReadingProgressEntity(
                surahNumber = surahNumber,
                lastAyah = existing?.lastAyah ?: ayahNumber,
                furthestAyah = existing?.furthestAyah ?: ayahNumber,
                lastReadAt = existing?.lastReadAt ?: System.currentTimeMillis(),
                completed = existing?.completed ?: false,
                completedAt = existing?.completedAt,
                bookmarkAyah = ayahNumber
            )
        )
    }

    override suspend fun getQuranProgress(): QuranProgress {
        val surahs = surahDao.getAllSurahs().first()
        val rows = progressDao.getAll().first()
        val totalAyahs = surahs.sumOf { it.numberOfAyahs }
        val readAyahs = rows.sumOf { row ->
            val total = surahs.find { it.number == row.surahNumber }?.numberOfAyahs ?: 0
            minOf(row.furthestAyah, total)
        }

        val continueRow = rows
            .filter { !it.completed }
            .maxByOrNull { it.lastReadAt }
        val continueSurah = continueRow?.let { row ->
            surahs.find { it.number == row.surahNumber }?.let { s ->
                SurahProgress(
                    surahNumber = s.number,
                    nameEnglish = s.nameEnglish,
                    nameArabic = s.nameArabic,
                    totalAyahs = s.numberOfAyahs,
                    lastAyah = row.lastAyah,
                    furthestAyah = row.furthestAyah,
                    bookmarkAyah = row.bookmarkAyah,
                    completed = row.completed,
                    lastReadAt = row.lastReadAt
                )
            }
        }

        return QuranProgress(
            continueSurah = continueSurah,
            readAyahs = readAyahs,
            totalAyahs = totalAyahs,
            completedCount = rows.count { it.completed }
        )
    }
}
