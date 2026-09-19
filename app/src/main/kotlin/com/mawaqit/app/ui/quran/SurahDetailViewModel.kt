package com.mawaqit.app.ui.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.data.db.AyahEntity
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Language toggle — cycles Arabic-only → +English → +Urdu (PHASE_6_QURAN.md). */
enum class DisplayMode { ARABIC_ONLY, ARABIC_ENGLISH, ARABIC_URDU }

data class SurahDetailUiState(
    val surah: SurahEntity? = null,
    val ayahs: List<AyahEntity> = emptyList(),
    val isLoading: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.ARABIC_ENGLISH,
    val bismillahPre: Boolean = true,
    val error: Boolean = false
)

@HiltViewModel
class SurahDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: QuranRepository
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
                val detail = repo.getSurahDetail(surahNumber)
                SurahDetailUiState(
                    surah = detail.surah,
                    ayahs = detail.ayahs,
                    isLoading = false,
                    displayMode = _uiState.value.displayMode,
                    bismillahPre = detail.bismillahPre
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
}
