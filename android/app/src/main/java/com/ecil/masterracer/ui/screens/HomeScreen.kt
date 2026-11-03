package com.ecil.masterracer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.R
import com.ecil.masterracer.ui.components.CompetitionCard
import com.ecil.masterracer.ui.components.DetailBottomSheet
import com.ecil.masterracer.ui.components.FilterBottomSheet
import com.ecil.masterracer.ui.components.SearchAndFilterBar
import com.ecil.masterracer.ui.components.SettingsBottomSheet
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.viewmodel.CompetitionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CompetitionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val targetRaceIds by viewModel.targetRaceIds.collectAsState()
    val completedRaceIds by viewModel.completedRaceIds.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showRecordsScreen by remember { mutableStateOf(false) }

    val athleteGender by viewModel.athleteGender.collectAsState()
    val athleteBirthYear by viewModel.athleteBirthYear.collectAsState()
    val athleteRecords by viewModel.athleteRecords.collectAsState()
    val athleteCategory = remember(athleteGender, athleteBirthYear) {
        viewModel.calculateCategory(athleteGender, athleteBirthYear)
    }

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val targetCompetitions = remember(uiState.allCompetitions, targetRaceIds) {
        uiState.allCompetitions.filter { targetRaceIds.contains(it.id) }
    }
    val completedCompetitions = remember(uiState.allCompetitions, completedRaceIds) {
        uiState.allCompetitions.filter { completedRaceIds.contains(it.id) }
    }

    if (showRecordsScreen) {
        BackHandler { showRecordsScreen = false }
        MasterRecordsScreen(
            athleteGender = athleteGender,
            athleteCategory = athleteCategory,
            athleteBirthYear = athleteBirthYear,
            athleteRecords = athleteRecords,
            onBack = { showRecordsScreen = false }
        )
    } else if (showProfileScreen) {
        BackHandler { showProfileScreen = false }
        ProfileScreen(
            viewModel = viewModel,
            allCompetitions = uiState.allCompetitions,
            targetCompetitions = targetCompetitions,
            completedCompetitions = completedCompetitions,
            onSelectCompetition = { comp ->
                viewModel.selectCompetitionForDetail(comp)
            },
            onBack = { showProfileScreen = false }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Orijinal Master Athlete Logosu
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Master Athlete Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Rekorlar Butonu
                            Surface(
                                onClick = { showRecordsScreen = true },
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.65f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rekorlar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Ayarlar butonu (Koyu/Açık Tema Seçimi)
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ayarlar",
                                tint = Color.White
                            )
                        }

                        // Profil butonu (Kariyer, PB/SB, Hedef Yarışlar)
                        IconButton(onClick = { showProfileScreen = true }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profil",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BluePrimary
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Arama ve Hızlı Filtre Barı
                SearchAndFilterBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    activeDatePreset = uiState.selectedDatePreset,
                    onDatePresetSelect = { viewModel.onDatePresetSelect(it) },
                    hideCompleted = uiState.hideCompleted,
                    onToggleHideCompleted = { viewModel.toggleHideCompleted() },
                    activeFilterCount = uiState.activeFilterCount,
                    onFilterClick = { showFilterSheet = true }
                )

                // Liste Başlığı & Sayım
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yaklaşan Şampiyonalar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = "${uiState.filteredCompetitions.size} yarışma",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Ana Liste Alanı
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = BluePrimary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Yarışma takvimi güncelleniyor...",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        uiState.filteredCompetitions.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Kriterlere Uygun Yarışma Bulunamadı",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Lütfen arama teriminizi veya filtre tercihlerinizi değiştiriniz.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = uiState.filteredCompetitions,
                                    key = { it.id }
                                ) { competition ->
                                    CompetitionCard(
                                        competition = competition,
                                        onClick = { viewModel.selectCompetitionForDetail(it) },
                                        isTracked = targetRaceIds.contains(competition.id),
                                        onToggleTrack = { viewModel.toggleTargetRace(competition.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filtreleme Modal Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            sheetState = filterSheetState,
            selectedScopes = uiState.selectedScopes,
            onScopeToggle = { viewModel.onScopeToggle(it) },
            onClearScopes = { viewModel.clearScopeFilters() },
            availableCountries = uiState.availableCountries,
            selectedCountries = uiState.selectedCountries,
            onCountryToggle = { viewModel.onCountryToggle(it) },
            onClearCountries = { viewModel.clearCountryFilters() },
            hideCompleted = uiState.hideCompleted,
            onToggleHideCompleted = { viewModel.toggleHideCompleted() },
            onResetFilters = { viewModel.onResetFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Detay Modal Bottom Sheet (Hem Ana Sayfadan Hem Profil Sayfasından Açılabilir)
    uiState.selectedCompetitionForDetail?.let { selectedComp ->
        DetailBottomSheet(
            competition = selectedComp,
            sheetState = detailSheetState,
            isTarget = viewModel.isTargetRace(selectedComp.id),
            onToggleTarget = { viewModel.toggleTargetRace(selectedComp.id) },
            isCompleted = completedRaceIds.contains(selectedComp.id),
            onToggleCompleted = { viewModel.toggleCompletedRace(selectedComp.id) },
            onDismiss = { viewModel.selectCompetitionForDetail(null) }
        )
    }

    // Ayarlar Modal Bottom Sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            sheetState = settingsSheetState,
            currentThemeMode = themeMode,
            onThemeSelect = { mode ->
                viewModel.setThemeMode(mode)
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}
