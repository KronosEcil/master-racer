package com.ecil.masterracer.ui.viewmodel

import com.ecil.masterracer.data.model.Competition

data class CompetitionUiState(
    val allCompetitions: List<Competition> = emptyList(),
    val filteredCompetitions: List<Competition> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedScopes: Set<String> = emptySet(),
    val selectedCountries: Set<String> = emptySet(),
    val selectedDatePreset: String = "all", // "all", "next_30", "next_90", "season"
    val hideCompleted: Boolean = true,
    val availableCountries: List<Pair<String, String>> = emptyList(),
    val selectedCompetitionForDetail: Competition? = null
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            count += selectedScopes.size
            count += selectedCountries.size
            if (selectedDatePreset != "all") count++
            if (!hideCompleted) count++ // Varsayılan gizli olduğu için, geçmişi de gösterirse ekstra filtre sayılır
            return count
        }
}

