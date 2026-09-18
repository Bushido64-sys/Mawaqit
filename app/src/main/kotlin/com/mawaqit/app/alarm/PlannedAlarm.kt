package com.mawaqit.app.alarm

import com.mawaqit.app.data.model.PrayerName

/**
 * One concrete prayer alarm in a scheduling plan (PHASE-3.1).
 *
 * Pure data — the input to AlarmScheduler.applyAlarmPlan(). One entry == one
 * (prayer, day) pair, which is what makes multi-day scheduling possible: the
 * old model had one alarm slot per prayer, so scheduling tomorrow would have
 * overwritten today.
 *
 * Also serialized into the ACTIVE_ALARMS pref registry as
 * "PRAYER|dateIso|timeMillis|azanType" — AlarmManager cannot be enumerated,
 * so the registry is how the scheduler knows what to cancel later.
 */
data class PlannedAlarm(
    val prayer: PrayerName,
    val timeMillis: Long,
    val dateIso: String,   // "2026-09-20" — the day this alarm belongs to
    val azanType: AzanType
) {
    companion object {
        const val REGISTRY_SEPARATOR = "|"
    }
}

/** Registry string for one planned alarm (PrefsRepository.ACTIVE_ALARMS). */
internal fun PlannedAlarm.toRegistryEntry(): String =
    listOf(prayer.name, dateIso, timeMillis.toString(), azanType.name)
        .joinToString(separator = PlannedAlarm.REGISTRY_SEPARATOR)
