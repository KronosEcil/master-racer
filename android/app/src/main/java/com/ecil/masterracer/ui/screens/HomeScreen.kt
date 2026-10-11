package com.ecil.masterracer.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.ui.components.CompetitionCard
import com.ecil.masterracer.ui.components.DetailBottomSheet
import com.ecil.masterracer.ui.components.FilterBottomSheet
import com.ecil.masterracer.ui.components.SearchAndFilterBar
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.viewmodel.CompetitionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CompetitionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Master Racer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Masterlar Atletizm Takvimi",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    // Yenile butonu
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
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
                                    onClick = { viewModel.selectCompetitionForDetail(it) }
                                )
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
            availableCountries = uiState.availableCountries,
            selectedCountries = uiState.selectedCountries,
            onCountryToggle = { viewModel.onCountryToggle(it) },
            onClearCountries = { viewModel.clearCountryFilters() },
            selectedVenueTypes = uiState.selectedVenueTypes,
            onVenueTypeToggle = { viewModel.onVenueTypeToggle(it) },
            onClearVenueTypes = { viewModel.clearVenueTypeFilters() },
            selectedCategories = uiState.selectedCategories,
            onCategoryToggle = { viewModel.onCategoryToggle(it) },
            onClearCategories = { viewModel.clearCategoryFilters() },
            hideCompleted = uiState.hideCompleted,
            onToggleHideCompleted = { viewModel.toggleHideCompleted() },
            onResetFilters = { viewModel.onResetFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Detay Modal Bottom Sheet
    uiState.selectedCompetitionForDetail?.let { selectedComp ->
        DetailBottomSheet(
            competition = selectedComp,
            sheetState = detailSheetState,
            onDismiss = { viewModel.selectCompetitionForDetail(null) }
        )
    }
}

