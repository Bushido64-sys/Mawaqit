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
    /** Keep only the last 30 days (DATA_SCHEMA.md). ISO dates make this safe. */
    suspend fun cleanupOldEntries()
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

    override suspend fun cleanupOldEntries() {
        dao.deleteOlderThan(LocalDate.now().minusDays(30).toString())
    }
}
