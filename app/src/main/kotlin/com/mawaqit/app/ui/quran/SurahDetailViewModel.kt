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
 * PHASE-6.1 translation toggle — segmented pill in the card header.
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

data class SurahDetailUiState(
    val surah: SurahEntity? = null,
    val ayahs: List<AyahEntity> = emptyList(),
    val isLoading: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.ARABIC_ENGLISH,
    val bismillahPre: Boolean = true,
    val fontScale: ReaderFontScale = ReaderFontScale.MEDIUM,
    val showSwipeHint: Boolean = false,
    val error: Boolean = false,
    // PHASE-6.3 "Keep My Place":
    val resumeAyah: Int? = null,   // first-open jump target (bookmark, else last page) — consumed after use
    val bookmarkAyah: Int = 0,     // pinned page's first ayah; 0 = none
    val totalAyahs: Int = 0        // completion math for auto-progress
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
                val hintSeen = prefs.readerSwipeHintSeen.first()
                val row = progress.getForSurahOnce(surahNumber)

                // Resume target: pinned bookmark wins, else the last-read page.
                // A completed surah restarts cleanly at page 1.
                val resumeAyah = row?.let { r ->
                    when {
                        r.completed -> null
                        r.bookmarkAyah > 0 -> r.bookmarkAyah
                        r.lastAyah > 1 -> r.lastAyah
                        else -> null
                    }
                }

                // Opening a surah counts as reading its first page: creates the
                // row on first open, refreshes lastReadAt otherwise. furthest
                // never regresses (repository keeps the max).
                if (detail.ayahs.isNotEmpty()) {
                    progress.recordProgress(
                        surahNumber = surahNumber,
                        lastAyah = resumeAyah ?: 1,
                        furthestAyah = 1,
                        totalAyahs = detail.ayahs.size
                    )
                }

                SurahDetailUiState(
                    surah = detail.surah,
                    ayahs = detail.ayahs,
                    isLoading = false,
                    displayMode = _uiState.value.displayMode,
                    bismillahPre = detail.bismillahPre,
                    fontScale = fontScale,
                    showSwipeHint = !hintSeen && detail.ayahs.isNotEmpty(),
                    error = false,
                    resumeAyah = resumeAyah,
                    bookmarkAyah = row?.bookmarkAyah ?: 0,
                    totalAyahs = detail.ayahs.size
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

    /** First swipe on the pager → the hint never shows again. */
    fun onFirstSwipe() {
        if (!_uiState.value.showSwipeHint) return
        _uiState.update { it.copy(showSwipeHint = false) }
        viewModelScope.launch { prefs.setReaderSwipeHintSeen() }
    }

    /**
     * PHASE-6.3 auto-progress: the pager settled on a page → that page is
     * "read". lastAyah = first ayah of the page (resume target), furthest =
     * last ayah of the page (repository keeps the running max → completion).
     */
    fun onPageSettled(pageFirstAyah: Int, pageLastAyah: Int) {
        val total = _uiState.value.totalAyahs
        if (total <= 0) return
        viewModelScope.launch {
            progress.recordProgress(
                surahNumber = surahNumber,
                lastAyah = pageFirstAyah,
                furthestAyah = pageLastAyah,
                totalAyahs = total
            )
        }
    }

    /** PHASE-6.3 bookmark pin: tap to pin the page, tap again to unpin. */
    fun markPage(pageFirstAyah: Int) {
        val newBookmark = if (_uiState.value.bookmarkAyah == pageFirstAyah) 0 else pageFirstAyah
        _uiState.update { it.copy(bookmarkAyah = newBookmark) }
        viewModelScope.launch { progress.setBookmark(surahNumber, newBookmark) }
    }

    /** The resume toast fired once — retire it so it can't repeat. */
    fun consumeResumeToast() {
        _uiState.update { it.copy(resumeAyah = null) }
    }
}
