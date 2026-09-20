package com.mawaqit.app.ui.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.data.db.AyahEntity
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.data.repository.QuranRepository
import com.mawaqit.app.util.QuranText
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
    val pages: List<List<AyahEntity>> = emptyList(),
    val showSwipeHint: Boolean = false,
    val error: Boolean = false
)

@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: QuranRepository,
    private val prefs: PrefsRepository
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle.get<Int>("number") ?: 1

    private val _uiState = MutableStateFlow(SurahDetailUiState())
    val uiState: StateFlow<SurahDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        // Font preference loads once; the Aa sheet updates it live.
        viewModelScope.launch {
            val stored = prefs.readerFontScale
            _uiState.update { it.copy(fontScale = ReaderFontScale.from(stored.first())) }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = false) }
            _uiState.value = try {
                val detail = repo.getSurahDetail(surahNumber)
                val hintSeen = prefs.readerSwipeHintSeen.first()
                SurahDetailUiState(
                    surah = detail.surah,
                    ayahs = detail.ayahs,
                    isLoading = false,
                    displayMode = _uiState.value.displayMode,
                    bismillahPre = detail.bismillahPre,
                    fontScale = _uiState.value.fontScale,
                    pages = QuranText.chunkIntoPages(detail.ayahs),
                    showSwipeHint = !hintSeen && detail.ayahs.isNotEmpty(),
                    error = false
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
}
