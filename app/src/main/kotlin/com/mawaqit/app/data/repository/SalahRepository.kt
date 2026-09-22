package com.mawaqit.app.data.repository

import com.mawaqit.app.data.db.SalahLogDao
import com.mawaqit.app.data.db.SalahLogEntity
import com.mawaqit.app.data.model.PrayerName
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Reads/writes the salah_log table (which prayers the user marked as prayed). */
interface SalahRepository {
    fun getSalahLogForDate(dateIso: String): Flow<List<SalahLogEntity>>
    suspend fun markPrayed(prayer: PrayerName, prayed: Boolean)
    /** Inclusive date-range read — PHASE-4.5 prayer calendar. */
    suspend fun getSalahLogBetween(startDateIso: String, endDateIso: String): List<SalahLogEntity>
}

@Singleton
class SalahRepositoryImpl @Inject constructor(
    private val dao: SalahLogDao
) : SalahRepository {

    override fun getSalahLogForDate(dateIso: String): Flow<List<SalahLogEntity>> =
        dao.getSalahLogForDate(dateIso)

    override suspend fun markPrayed(prayer: PrayerName, prayed: Boolean) {
        dao.upsert(
            SalahLogEntity(
                date = LocalDate.now().toString(),           // ISO YYYY-MM-DD
                prayer = prayer.name,                        // "FAJR" | ... (schema enum)
                prayed = prayed,
                markedAt = if (prayed) System.currentTimeMillis() else null
            )
        )
    }

    override suspend fun getSalahLogBetween(startDateIso: String, endDateIso: String): List<SalahLogEntity> =
        dao.getSalahLogBetween(startDateIso, endDateIso)
}
