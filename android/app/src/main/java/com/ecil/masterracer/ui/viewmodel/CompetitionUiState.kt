package com.ecil.masterracer.ui.viewmodel

import com.ecil.masterracer.data.model.Competition

data class CompetitionUiState(
    val allCompetitions: List<Competition> = emptyList(),
    val filteredCompetitions: List<Competition> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCountry: String? = null,
    val selectedVenueType: String? = null,
    val selectedCategory: String? = null,
    val selectedDatePreset: String = "all", // "all", "next_30", "next_90", "season"
    val hideCompleted: Boolean = false,
    val availableCountries: List<Pair<String, String>> = emptyList(),
    val selectedCompetitionForDetail: Competition? = null
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedCountry != null) count++
            if (selectedVenueType != null) count++
            if (selectedCategory != null) count++
            if (selectedDatePreset != "all") count++
            if (hideCompleted) count++
            return count
        }
}

