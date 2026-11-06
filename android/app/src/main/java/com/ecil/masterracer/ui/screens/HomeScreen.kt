package com.ecil.masterracer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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

private enum class MainBottomTab(
    val title: String,
    val icon: ImageVector
) {
    CALENDAR("Takvim", Icons.Default.CalendarMonth),
    RECORDS("Rekorlar", Icons.Default.EmojiEvents),
    STANDARDS("Standartlar", Icons.Default.Straighten),
    CALL_ROOM("Çağrı Odası", Icons.Default.Timer),
    PROFILE("Profil", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: CompetitionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val targetRaceIds by viewModel.targetRaceIds.collectAsState()
    val completedRaceIds by viewModel.completedRaceIds.collectAsState()

    var currentTab by remember { mutableStateOf(MainBottomTab.CALENDAR) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

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

    BackHandler(enabled = currentTab != MainBottomTab.CALENDAR) {
        currentTab = MainBottomTab.CALENDAR
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (currentTab == MainBottomTab.CALENDAR) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Orijinal Master Athlete Logosu
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Master Athlete Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "MASTER",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ATHLETE",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                Text(
                                    text = "MASTERLAR ATLETİZM TAKVİMİ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BluePrimary
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                MainBottomTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = BluePrimary.copy(alpha = 0.14f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (currentTab == MainBottomTab.CALENDAR) innerPadding.calculateTopPadding() else 0.dp,
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            when (currentTab) {
                MainBottomTab.CALENDAR -> {
                    Column(modifier = Modifier.fillMaxSize()) {
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
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${uiState.filteredCompetitions.size} yarışma",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    val groupedByYearMonth = remember(uiState.filteredCompetitions) {
                                        uiState.filteredCompetitions.groupBy { comp ->
                                            comp.start_date.take(7).ifBlank { "Diğer" }
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        groupedByYearMonth.forEach { (yearMonth, monthCompetitions) ->
                                            stickyHeader(key = "header_$yearMonth") {
                                                MonthYearStickyHeader(
                                                    yearMonth = yearMonth,
                                                    competitionCount = monthCompetitions.size
                                                )
                                            }

                                            items(
                                                items = monthCompetitions,
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

                MainBottomTab.RECORDS -> {
                    MasterRecordsScreen(
                        athleteGender = athleteGender,
                        athleteCategory = athleteCategory,
                        athleteBirthYear = athleteBirthYear,
                        athleteRecords = athleteRecords,
                        onBack = { currentTab = MainBottomTab.CALENDAR }
                    )
                }

                MainBottomTab.STANDARDS -> {
                    WmaStandardsScreen(
                        athleteGender = athleteGender,
                        athleteCategory = athleteCategory,
                        onBack = { currentTab = MainBottomTab.CALENDAR }
                    )
                }

                MainBottomTab.CALL_ROOM -> {
                    val nearestTrackedCity = remember(targetCompetitions) {
                        targetCompetitions
                            .filter { !it.isCompleted() && it.city.isNotBlank() }
                            .minByOrNull { it.getDaysRemaining() }
                            ?.city ?: "İzmir"
                    }
                    CallRoomScreen(
                        initialCity = nearestTrackedCity,
                        onBack = { currentTab = MainBottomTab.CALENDAR }
                    )
                }

                MainBottomTab.PROFILE -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        allCompetitions = uiState.allCompetitions,
                        targetCompetitions = targetCompetitions,
                        completedCompetitions = completedCompetitions,
                        onSelectCompetition = { comp ->
                            viewModel.selectCompetitionForDetail(comp)
                        },
                        onBack = { currentTab = MainBottomTab.CALENDAR }
                    )
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

@Composable
private fun MonthYearStickyHeader(
    yearMonth: String,
    competitionCount: Int
) {
    val parts = yearMonth.split("-")
    val yearStr = parts.getOrNull(0).orEmpty()
    val monthNum = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val monthNameTr = when (monthNum) {
        1 -> "OCAK"
        2 -> "ŞUBAT"
        3 -> "MART"
        4 -> "NİSAN"
        5 -> "MAYIS"
        6 -> "HAZİRAN"
        7 -> "TEMMUZ"
        8 -> "AĞUSTOS"
        9 -> "EYLÜL"
        10 -> "EKİM"
        11 -> "KASIM"
        12 -> "ARALIK"
        else -> yearMonth.uppercase()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.22f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (yearStr.length == 4) {
                        Surface(
                            color = BluePrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = yearStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = monthNameTr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = BluePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$competitionCount Yarışma",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
