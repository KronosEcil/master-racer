package com.ecil.masterracer.ui.viewmodel

import android.content.Context
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecil.masterracer.MasterRacerApp
import com.ecil.masterracer.data.model.Competition
import com.ecil.masterracer.data.repository.CompetitionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Keep
class CompetitionViewModel(
    private val repository: CompetitionRepository = CompetitionRepository()
) : ViewModel() {

    constructor() : this(CompetitionRepository())

    private val prefs = MasterRacerApp.instance.getSharedPreferences("master_athlete_prefs", Context.MODE_PRIVATE)

    // Tema Modu: "system", "light", "dark"
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    // Sporcu Profil Bilgileri
    private val _athleteName = MutableStateFlow(prefs.getString("athlete_name", "") ?: "")
    val athleteName: StateFlow<String> = _athleteName.asStateFlow()

    private val _athleteGender = MutableStateFlow(prefs.getString("athlete_gender", "M") ?: "M")
    val athleteGender: StateFlow<String> = _athleteGender.asStateFlow()

    private val _athleteBirthYear = MutableStateFlow(prefs.getInt("athlete_birth_year", 1990))
    val athleteBirthYear: StateFlow<Int> = _athleteBirthYear.asStateFlow()

    fun updateAthleteProfile(name: String, gender: String, birthYear: Int) {
        _athleteName.value = name.trim()
        _athleteGender.value = gender
        _athleteBirthYear.value = birthYear
        prefs.edit()
            .putString("athlete_name", name.trim())
            .putString("athlete_gender", gender)
            .putInt("athlete_birth_year", birthYear)
            .apply()
    }

    fun calculateCategory(
        gender: String = _athleteGender.value,
        birthYear: Int = _athleteBirthYear.value,
        targetYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    ): String {
        return com.ecil.masterracer.util.MasterAthleticsHelper.calculateCategory(gender, birthYear, targetYear)
    }

    // Sporcu Dereceleri (Kişisel & Sezon Dereceleri)
    private val _athleteRecords = MutableStateFlow<List<com.ecil.masterracer.util.AthleteRecord>>(
        com.ecil.masterracer.util.MasterAthleticsHelper.parseRecordsJson(prefs.getString("athlete_records_json", "") ?: "")
    )
    val athleteRecords: StateFlow<List<com.ecil.masterracer.util.AthleteRecord>> = _athleteRecords.asStateFlow()

    fun saveAthleteRecord(record: com.ecil.masterracer.util.AthleteRecord) {
        val current = _athleteRecords.value.toMutableList()
        val index = current.indexOfFirst { it.id == record.id }
        if (index >= 0) {
            current[index] = record
        } else {
            current.add(0, record)
        }
        _athleteRecords.value = current
        prefs.edit().putString("athlete_records_json", com.ecil.masterracer.util.MasterAthleticsHelper.serializeRecordsJson(current)).apply()
    }

    fun deleteAthleteRecord(recordId: String) {
        val current = _athleteRecords.value.filter { it.id != recordId }
        _athleteRecords.value = current
        prefs.edit().putString("athlete_records_json", com.ecil.masterracer.util.MasterAthleticsHelper.serializeRecordsJson(current)).apply()
    }

    // Hedeflenen Yarışlar
    private val _targetRaceIds = MutableStateFlow(prefs.getStringSet("target_race_ids", emptySet()) ?: emptySet())
    val targetRaceIds: StateFlow<Set<String>> = _targetRaceIds.asStateFlow()

    fun toggleTargetRace(competitionId: String) {
        val current = _targetRaceIds.value.toMutableSet()
        if (current.contains(competitionId)) {
            current.remove(competitionId)
        } else {
            current.add(competitionId)
        }
        _targetRaceIds.value = current
        prefs.edit().putStringSet("target_race_ids", current).apply()
    }

    fun isTargetRace(competitionId: String): Boolean = _targetRaceIds.value.contains(competitionId)

    // Katılınan Yarışlar
    private val _completedRaceIds = MutableStateFlow(prefs.getStringSet("completed_race_ids", emptySet()) ?: emptySet())
    val completedRaceIds: StateFlow<Set<String>> = _completedRaceIds.asStateFlow()

    fun toggleCompletedRace(competitionId: String) {
        val current = _completedRaceIds.value.toMutableSet()
        if (current.contains(competitionId)) {
            current.remove(competitionId)
        } else {
            current.add(competitionId)
        }
        _completedRaceIds.value = current
        prefs.edit().putStringSet("completed_race_ids", current).apply()
    }

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
                    .map { Pair(it.getDisplayCountryCode(), it.getDisplayCountryName()) }
                    .filter { it.first.isNotBlank() && it.first != "INT" && it.first != "EU" && !it.second.contains("Uluslararası", ignoreCase = true) && !it.second.equals("Avrupa", ignoreCase = true) }
                    .distinctBy { it.second }
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
                selectedDatePreset = "all",
                hideCompleted = true,
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
                .map { Pair(it.getDisplayCountryCode(), it.getDisplayCountryName()) }
                .filter { it.first.isNotBlank() && it.first != "INT" && it.first != "EU" && !it.second.contains("Uluslararası", ignoreCase = true) && !it.second.equals("Avrupa", ignoreCase = true) }
                .distinctBy { it.second }
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
        val preset = currentState.selectedDatePreset
        val hideCompleted = currentState.hideCompleted

        val filtered = currentState.allCompetitions.filter { comp ->
            // Almanya ve Genel Sıkı 35+ Master Kriteri Filtresi
            if (!comp.isStrictMasterAthletics()) {
                return@filter false
            }

            val days = comp.getDaysRemaining()

            // Tamamlanan Yarışmaları Gizleme Filtresi
            if (hideCompleted && days < 0) {
                return@filter false
            }

            // Metin Arama Filtresi (Başlık, Şehir, Ülke veya Federasyon)
            val matchesQuery = if (query.isEmpty()) true else {
                comp.title.lowercase().contains(query) ||
                        comp.city.lowercase().contains(query) ||
                        comp.getDisplayLocation().lowercase().contains(query) ||
                        comp.getDisplayCountryName().lowercase().contains(query) ||
                        comp.country_name.lowercase().contains(query) ||
                        comp.federation.lowercase().contains(query)
            }

            // Ülke Filtresi (Çoklu Seçim: Seçili ülkelerden herhangi biriyle eşleşmeli)
            val matchesCountry = countries.isEmpty() || countries.any { 
                it.equals(comp.country, ignoreCase = true) || it.equals(comp.getDisplayCountryCode(), ignoreCase = true)
            }

            // Tarih Önayarı Filtresi
            val currentYearStr = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
            val nextYearStr = (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1).toString()

            val matchesDatePreset = when (preset) {
                "next_90" -> days in 0..90
                "current_year" -> comp.start_date.startsWith(currentYearStr)
                "next_year" -> comp.start_date.startsWith(nextYearStr)
                else -> true
            }

            matchesQuery && matchesCountry && matchesDatePreset
        }.sortedWith(compareBy({ it.getDaysRemaining() < 0 }, { it.start_date }))

        _uiState.update { it.copy(filteredCompetitions = filtered) }
    }
}

