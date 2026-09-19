package com.mawaqit.app.data.model

import com.mawaqit.app.data.db.AyahEntity
import com.mawaqit.app.data.db.SurahEntity

/** One Surah fully loaded (metadata + cached ayahs) — PHASE_6 reading screen. */
data class SurahDetail(
    val surah: SurahEntity?,
    val ayahs: List<AyahEntity>,
    val bismillahPre: Boolean
)
