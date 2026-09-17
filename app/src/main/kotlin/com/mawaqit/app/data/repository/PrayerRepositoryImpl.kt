package com.mawaqit.app.data.repository

import com.mawaqit.app.data.api.AladhanApiService
import com.mawaqit.app.data.db.MawaqitDatabase
import com.mawaqit.app.data.db.PrayerTimeEntity
import com.mawaqit.app.data.model.NextPrayer
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.model.PrayerTimings
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.util.parseTimeToMillis
import com.mawaqit.app.util.toHhMm
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val db: MawaqitDatabase,
    private val api: AladhanApiService,
    private val prefs: PrefsRepository
) : PrayerRepository {

    override suspend fun setLocation(latitude: Double, longitude: Double, cityName: String?) {
        prefs.saveLocation(latitude, longitude, cityName)
        prefs.setLastMonthFetched("") // invalidate the month marker
        db.prayerTimeDao().deleteAll() // old location's times are now wrong
    }

    override suspend fun refreshIfNeeded(): Boolean {
        val coords = prefs.getLocationOnce() ?: return false
        val ym = YearMonth.now()                      // toString() == "2026-09" (ISO)
        val monthPattern = "${ym}-%"                 // "2026-09-%" (ISO year-month prefix)
        if (prefs.getLastMonthFetchedOnce() == ym.toString() &&
            db.prayerTimeDao().countForMonth(monthPattern) > 0
        ) {
            return true
        }
        return try {
            val response = api.getMonthlyCalendar(
                year = ym.year,
                month = ym.monthValue,
                latitude = coords.first,
                longitude = coords.second
            )
            if (response.code != 200) return false    // body code, not just HTTP (API_REFERENCE.md)
            val days = response.data.orEmpty().mapNotNull { it.toEntity(coords) }
            if (days.isEmpty()) {
                false
            } else {
                db.prayerTimeDao().insertAll(days)
                prefs.setLastMonthFetched(ym.toString())
                true
            }
        } catch (e: Exception) {
            false // offline / timeout → caller serves cache (data flow diagram)
        }
    }

    override suspend fun getTodayPrayerTimes(): PrayerTimings? {
        val dao = db.prayerTimeDao()
        dao.getPrayerTimeByDate(LocalDate.now().toString())?.let { return it.toDomain() }
        // Today's row missing (offline install mid-month, clock change, ...):
        // serve the latest cached day + let the UI show the "Cached data" banner.
        return dao.getLatest()?.toDomain()
    }

    override suspend fun getNextPrayer(today: PrayerTimings): NextPrayer? {
        val date = LocalDate.parse(today.date) // ISO
        val now = System.currentTimeMillis()
        val todays = listOf(
            PrayerName.FAJR to today.fajr,
            PrayerName.DHUHR to today.dhuhr,
            PrayerName.ASR to today.asr,
            PrayerName.MAGHRIB to today.maghrib,
            PrayerName.ISHA to today.isha
        )
        todays.forEach { (name, timeStr) ->
            val millis = parseTimeToMillis(timeStr, date)
            if (millis > now) return NextPrayer(name, timeStr, millis)
        }
        // All of today's prayers passed → tomorrow's Fajr (row exists except on
        // the month's last day, where we approximate with today's Fajr time).
        val tomorrow = date.plusDays(1)
        val fajrTomorrow = db.prayerTimeDao()
            .getPrayerTimeByDate(tomorrow.toString())?.fajr ?: today.fajr
        return NextPrayer(PrayerName.FAJR, fajrTomorrow, parseTimeToMillis(fajrTomorrow, tomorrow))
    }

    override fun cityName(): Flow<String?> = prefs.savedCityName

    override suspend fun hasSavedLocation(): Boolean = prefs.getLocationOnce() != null

    // ── mappers ───────────────────────────────────────────────────────────────

    /** AlAdhan day → Room row. DD-MM-YYYY → ISO; strips " (PKT)" suffixes. */
    private fun com.mawaqit.app.data.api.AladhanDay.toEntity(
        coords: Pair<Double, Double>
    ): PrayerTimeEntity? {
        val timings = timings ?: return null
        val gregorianDate = date?.gregorian?.date ?: return null   // "15-09-2026"
        val parts = gregorianDate.split("-")
        if (parts.size != 3) return null
        val iso = "${parts[2]}-${parts[1]}-${parts[0]}"            // "2026-09-15"
        return try {
            LocalDate.parse(iso) // validation only — throws if malformed
            PrayerTimeEntity(
                date = iso,
                fajr = timings.fajr.orEmpty().toHhMm(),
                dhuhr = timings.dhuhr.orEmpty().toHhMm(),
                asr = timings.asr.orEmpty().toHhMm(),
                maghrib = timings.maghrib.orEmpty().toHhMm(),
                isha = timings.isha.orEmpty().toHhMm(),
                latitude = coords.first,
                longitude = coords.second,
                fetchedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun PrayerTimeEntity.toDomain() = PrayerTimings(
        date = date,
        fajr = fajr,
        dhuhr = dhuhr,
        asr = asr,
        maghrib = maghrib,
        isha = isha,
        latitude = latitude,
        longitude = longitude
    )
}
