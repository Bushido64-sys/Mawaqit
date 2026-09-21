package com.mawaqit.app.ui.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.data.db.AyahEntity
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.data.repository.QuranRepository
import com.mawaqit.app.data.repository.ReadingProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PHASE-6.1 translation toggle — segmented pill in the Reading Settings sheet.
 * The selected entry shows BESIDES the always-on Arabic.
 */
enum class DisplayMode { ARABIC_ONLY, ARABIC_ENGLISH, ARABIC_URDU }

/** Font-size options for the reader (S/M/L/XL → Arabic scale multiplier). */
enum class ReaderFontScale(val multiplier: Float, val label: String) {
    SMALL(0.85f, "S"),
    MEDIUM(1.0f, "M"),
    LARGE(1.15f, "L"),
    XLARGE(1.3f, "XL");

    companion object {
        fun from(multiplier: Float): ReaderFontScale =
            entries.minByOrNull { kotlin.math.abs(it.multiplier - multiplier) } ?: MEDIUM
    }
}

/** Why a "Mark as read" tap was refused — drives the lock pop-up's message. */
enum class LockReason { PAGE_ORDER, SURAH_ORDER }

data class SurahDetailUiState(
    val surah: SurahEntity? = null,
    val ayahs: List<AyahEntity> = emptyList(),
    val isLoading: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.ARABIC_ENGLISH,
    val bismillahPre: Boolean = true,
    val fontScale: ReaderFontScale = ReaderFontScale.MEDIUM,
    val error: Boolean = false,
    // PHASE-6.3 resume — first-open jump target (legacy pin, else last marked
    // page); completed surahs restart cleanly at page 1.
    val resumeAyah: Int? = null,
    // PHASE-6.4 — button-only progress: furthest marked-read ayah. Drives
    // the "Mark as read" ⇄ "Read ✓" button state per page.
    val furthestAyah: Int = 0,
    val totalAyahs: Int = 0,
    // PHASE-6.5 — pages unlock in order: the first ayah of the next unread
    // page (the only page whose button is active). 0 = nothing locked yet.
    val nextUnlockAyah: Int = 0,
    // PHASE-6.5 — the surah that must be finished first (null = this surah is
    // unlocked). A fresh reader is locked to every surah except Al-Fatihah.
    val blockerSurahName: String? = null,
    // PHASE-6.5 lock pop-up — shown when a locked button was tapped anyway.
    val lockReason: LockReason? = null,
    // PHASE-6.5 — the remaining coach ("One tap saves your place"), one step,
    // one prefs flag. The swipe lesson is filed for PHASE-9 onboarding.
    val coachVisible: Boolean = false
)

@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: QuranRepository,
    private val prefs: PrefsRepository,
    private val progress: ReadingProgressRepository
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle.get<Int>("number") ?: 1

    private val _uiState = MutableStateFlow(SurahDetailUiState())
    val uiState: StateFlow<SurahDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = false) }
            _uiState.value = try {
                // Font pref resolves BEFORE first data composition, so the
                // pager fits once with the final size (no late re-fit jump).
                val fontScale = ReaderFontScale.from(prefs.readerFontScale.first())
                val detail = repo.getSurahDetail(surahNumber)
                val coachDone = prefs.readerCoachDone.first()
                val row = progress.getForSurahOnce(surahNumber)

                // PHASE-6.4 — opening a surah WRITES NOTHING. Browsing other
                // surahs must never move the Continue card; progress changes
                // on exactly one event: the "Mark as read" button. This is a
                // read-only lookup for the resume jump.
                val resumeAyah = row?.let { r ->
                    when {
                        r.completed -> null
                        r.bookmarkAyah > 0 -> r.bookmarkAyah // legacy PHASE-6.3 pin
                        r.lastAyah > 1 -> r.lastAyah
                        else -> null
                    }
                }

                // PHASE-6.5 read-in-order gate — who blocks this surah?
                val blocker = progress.findBlockerSurah(surahNumber)

                SurahDetailUiState(
                    surah = detail.surah,
                    ayahs = detail.ayahs,
                    isLoading = false,
                    displayMode = _uiState.value.displayMode,
                    bismillahPre = detail.bismillahPre,
                    fontScale = fontScale,
                    error = false,
                    resumeAyah = resumeAyah,
                    furthestAyah = row?.furthestAyah ?: 0,
                    totalAyahs = detail.ayahs.size,
                    nextUnlockAyah = row?.furthestAyah?.plus(1)?.takeIf { it <= detail.ayahs.size } ?: 1,
                    blockerSurahName = blocker?.nameEnglish,
                    coachVisible = !coachDone && detail.ayahs.isNotEmpty()
                )
            } catch (e: Exception) {
                // Nothing cached + network failed (Rule 8) → friendly error, retry.
                _uiState.value.copy(isLoading = false, error = true)
            }
        }
    }

    fun setMode(mode: DisplayMode) {
        _uiState.update { it.copy(displayMode = mode) }
    }

    fun setFontScale(scale: ReaderFontScale) {
        _uiState.update { it.copy(fontScale = scale) }
        viewModelScope.launch { prefs.setReaderFontScale(scale.multiplier) }
    }

    /**
     * PHASE-6.5 — the only progress writer, now behind the one gate:
     * - Re-tapping a read page always works (re-pins the resume point).
     * - A page beyond the next unread page in THIS surah → PAGE_ORDER popup.
     * - Any page in a surah that an earlier unfinished surah blocks →
     *   SURAH_ORDER popup (real blocker name shown by the screen).
     * Allowed marks also finish the coach (the button is its last lesson).
     */
    fun markPage(pageFirstAyah: Int, pageLastAyah: Int) {
        val state = _uiState.value
        val total = state.totalAyahs
        if (total <= 0) return

        val alreadyRead = state.furthestAyah >= pageLastAyah
        val isUnlockedSurah = state.blockerSurahName == null
        val isNextInOrder = pageFirstAyah <= state.furthestAyah + 1

        when {
            alreadyRead -> Unit // re-pin: always allowed, no popup
            !isUnlockedSurah -> {
                _uiState.update { it.copy(lockReason = LockReason.SURAH_ORDER) }
                return
            }
            !isNextInOrder -> {
                _uiState.update { it.copy(lockReason = LockReason.PAGE_ORDER) }
                return
            }
        }

        val coachWasActive = state.coachVisible
        _uiState.update {
            it.copy(
                furthestAyah = maxOf(it.furthestAyah, pageLastAyah),
                nextUnlockAyah = (pageLastAyah + 1).takeIf { next -> next <= total } ?: 0,
                lockReason = null,
                coachVisible = false
            )
        }
        if (coachWasActive) {
            viewModelScope.launch { prefs.setReaderCoachDone() }
        }
        viewModelScope.launch {
            progress.markPage(surahNumber, pageFirstAyah, pageLastAyah, total)
        }
    }

    /** The lock pop-up was acknowledged — retire it until the next locked tap. */
    fun consumeLockPopup() {
        _uiState.update { it.copy(lockReason = null) }
    }

    /** The remaining coach dismissed via "Got it" — retired forever. */
    fun onCoachDone() {
        if (_uiState.value.coachVisible) {
            _uiState.update { it.copy(coachVisible = false) }
            viewModelScope.launch { prefs.setReaderCoachDone() }
        }
    }

    /** The resume toast fired once — retire it so it can't repeat. */
    fun consumeResumeToast() {
        _uiState.update { it.copy(resumeAyah = null) }
    }
}
