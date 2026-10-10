package com.ecil.masterracer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecil.masterracer.data.model.Competition
import com.ecil.masterracer.data.repository.CompetitionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompetitionViewModel(
    private val repository: CompetitionRepository = CompetitionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompetitionUiState())
    val uiState: StateFlow<CompetitionUiState> = _uiState.asStateFlow()

    init {
        observeCompetitions()
    }

    private fun observeCompetitions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getCompetitionsFlow().collect { competitions ->
                val countries = competitions
                    .filter { it.country.isNotBlank() }
                    .map { Pair(it.country, if (it.country_name.isNotBlank()) it.country_name else it.country) }
                    .distinctBy { it.first }
                    .sortedBy { it.second }

                _uiState.update { state ->
                    state.copy(
                        allCompetitions = competitions,
                        availableCountries = countries,
                        isLoading = false
                    )
                }
                applyFilters()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onCountrySelect(countryCode: String?) {
        _uiState.update { it.copy(selectedCountry = countryCode) }
        applyFilters()
    }

    fun onVenueTypeSelect(venueType: String?) {
        _uiState.update { it.copy(selectedVenueType = venueType) }
        applyFilters()
    }

    fun onCategorySelect(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    fun onDatePresetSelect(preset: String) {
        _uiState.update { it.copy(selectedDatePreset = preset) }
        applyFilters()
    }

    fun toggleHideCompleted() {
        _uiState.update { it.copy(hideCompleted = !it.hideCompleted) }
        applyFilters()
    }

    fun setHideCompleted(hide: Boolean) {
        _uiState.update { it.copy(hideCompleted = hide) }
        applyFilters()
    }

    fun onResetFilters() {
        _uiState.update {
            it.copy(
                selectedCountry = null,
                selectedVenueType = null,
                selectedCategory = null,
                selectedDatePreset = "all",
                hideCompleted = false,
                searchQuery = ""
            )
        }
        applyFilters()
    }

    fun selectCompetitionForDetail(competition: Competition?) {
        _uiState.update { it.copy(selectedCompetitionForDetail = competition) }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val refreshed = repository.refreshCompetitions()
            val countries = refreshed
                .filter { it.country.isNotBlank() }
                .map { Pair(it.country, if (it.country_name.isNotBlank()) it.country_name else it.country) }
                .distinctBy { it.first }
                .sortedBy { it.second }

            _uiState.update { state ->
                state.copy(
                    allCompetitions = refreshed,
                    availableCountries = countries,
                    isRefreshing = false
                )
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        val query = currentState.searchQuery.trim().lowercase()
        val country = currentState.selectedCountry
        val venue = currentState.selectedVenueType
        val category = currentState.selectedCategory
        val preset = currentState.selectedDatePreset
        val hideCompleted = currentState.hideCompleted

        val filtered = currentState.allCompetitions.filter { comp ->
            val days = comp.getDaysRemaining()

            // Tamamlanan Yarışmaları Gizleme Filtresi
            if (hideCompleted && days < 0) {
                return@filter false
            }

            // Metin Arama Filtresi (Başlık, Şehir, Ülke veya Federasyon)
            val matchesQuery = if (query.isEmpty()) true else {
                comp.title.lowercase().contains(query) ||
                        comp.city.lowercase().contains(query) ||
                        comp.country_name.lowercase().contains(query) ||
                        comp.federation.lowercase().contains(query)
            }

            // Ülke Filtresi
            val matchesCountry = country == null || comp.country.equals(country, ignoreCase = true)

            // Tesis Türü Filtresi (indoor, outdoor, road, cross)
            val matchesVenue = venue == null || comp.venue_type.equals(venue, ignoreCase = true)

            // Branş Filtresi
            val matchesCategory = category == null || comp.categories.any { it.equals(category, ignoreCase = true) }

            // Tarih Önayarı Filtresi
            val matchesDatePreset = when (preset) {
                "next_30" -> days in 0..30
                "next_90" -> days in 0..90
                "season" -> days >= 0
                else -> true
            }

            matchesQuery && matchesCountry && matchesVenue && matchesCategory && matchesDatePreset
        }.sortedWith(compareBy({ it.getDaysRemaining() < 0 }, { it.start_date }))

        _uiState.update { it.copy(filteredCompetitions = filtered) }
    }
}

