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

    fun onCountryToggle(countryCode: String) {
        _uiState.update { state ->
            val updated = if (state.selectedCountries.contains(countryCode)) {
                state.selectedCountries - countryCode
            } else {
                state.selectedCountries + countryCode
            }
            state.copy(selectedCountries = updated)
        }
        applyFilters()
    }

    fun clearCountryFilters() {
        _uiState.update { it.copy(selectedCountries = emptySet()) }
        applyFilters()
    }

    fun onVenueTypeToggle(venueType: String) {
        _uiState.update { state ->
            val updated = if (state.selectedVenueTypes.contains(venueType)) {
                state.selectedVenueTypes - venueType
            } else {
                state.selectedVenueTypes + venueType
            }
            state.copy(selectedVenueTypes = updated)
        }
        applyFilters()
    }

    fun clearVenueTypeFilters() {
        _uiState.update { it.copy(selectedVenueTypes = emptySet()) }
        applyFilters()
    }

    fun onCategoryToggle(category: String) {
        _uiState.update { state ->
            val updated = if (state.selectedCategories.contains(category)) {
                state.selectedCategories - category
            } else {
                state.selectedCategories + category
            }
            state.copy(selectedCategories = updated)
        }
        applyFilters()
    }

    fun clearCategoryFilters() {
        _uiState.update { it.copy(selectedCategories = emptySet()) }
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
                selectedCountries = emptySet(),
                selectedVenueTypes = emptySet(),
                selectedCategories = emptySet(),
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
        val countries = currentState.selectedCountries
        val venues = currentState.selectedVenueTypes
        val categories = currentState.selectedCategories
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

            // Ülke Filtresi (Çoklu Seçim: Seçili ülkelerden herhangi biriyle eşleşmeli)
            val matchesCountry = countries.isEmpty() || countries.any { it.equals(comp.country, ignoreCase = true) }

            // Tesis Türü Filtresi (Çoklu Seçim: indoor, outdoor, road, cross)
            val matchesVenue = venues.isEmpty() || venues.any { it.equals(comp.venue_type, ignoreCase = true) }

            // Branş Filtresi (Çoklu Seçim: Seçili branş gruplarından en az biriyle eşleşmeli)
            val effectiveCategories = comp.getEffectiveCategories()
            val matchesCategory = categories.isEmpty() || effectiveCategories.any { cat ->
                categories.any { it.equals(cat, ignoreCase = true) }
            }

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

