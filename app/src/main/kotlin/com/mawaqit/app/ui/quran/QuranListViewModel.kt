package com.mawaqit.app.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuranListUiState(
    val surahs: List<SurahEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val error: Boolean = false
)

/**
 * PHASE_6 Surah list (PHASE_6_QURAN.md). Search delegates to
 * SurahDao.searchSurahs (English + Arabic LIKE) via flatMapLatest — no
 * in-memory filtering, the DAO is the single source.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuranListViewModel @Inject constructor(
    private val repo: QuranRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val searchActive = MutableStateFlow(false)
    private val listReady = MutableStateFlow(false) // ensureSurahListLoaded() finished
    private val loadFailed = MutableStateFlow(false)

    val uiState: StateFlow<QuranListUiState> = combine(
        query.flatMapLatest { q ->
            if (q.isBlank()) repo.getAllSurahs() else repo.searchSurahs(q.trim())
        },
        query, searchActive, listReady, loadFailed
    ) { surahs, q, active, ready, failed ->
        QuranListUiState(
            surahs = surahs,
            // While the one-time bootstrap is still running, keep the spinner.
            isLoading = !ready && surahs.isEmpty(),
            searchQuery = q,
            isSearchActive = active,
            error = failed && surahs.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranListUiState())

    init {
        refresh()
    }

    /** Bootstrap the 114-surah list (no-op when DB already has it). */
    fun refresh() {
        viewModelScope.launch {
            loadFailed.value = !repo.ensureSurahListLoaded()
            listReady.value = true
        }
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onSearchActiveChange(active: Boolean) {
        searchActive.value = active
    }
}
