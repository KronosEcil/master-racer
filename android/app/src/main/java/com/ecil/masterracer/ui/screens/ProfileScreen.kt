package com.ecil.masterracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.data.model.Competition
import com.ecil.masterracer.ui.components.CountdownBadge
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent
import com.ecil.masterracer.ui.viewmodel.CompetitionViewModel
import com.ecil.masterracer.util.AthleteRecord
import com.ecil.masterracer.util.EventFormatType
import com.ecil.masterracer.util.MasterAthleticsHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: CompetitionViewModel,
    allCompetitions: List<Competition> = emptyList(),
    targetCompetitions: List<Competition>,
    completedCompetitions: List<Competition>,
    onSelectCompetition: (Competition) -> Unit,
    onBack: () -> Unit
) {
    val athleteName by viewModel.athleteName.collectAsState()
    val athleteGender by viewModel.athleteGender.collectAsState()
    val athleteBirthYear by viewModel.athleteBirthYear.collectAsState()
    val athleteRecords by viewModel.athleteRecords.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var recordPresetCompTitle by remember { mutableStateOf("") }
    var recordPresetYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var isFixedCompetition by remember { mutableStateOf(false) }
    val recordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Takibimdekiler", "Derecelerim", "Geçmişim")

    val category = viewModel.calculateCategory(athleteGender, athleteBirthYear)
    val isFemale = athleteGender.equals("F", ignoreCase = true)
    val categoryBadgeColor = if (isFemale) Color(0xFFEC4899) else Color(0xFF2563EB)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = "Profilim",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Profili Düzenle",
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Sporcu Kimlik & Özet Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    if (isFemale)
                                        listOf(Color(0xFFF472B6), Color(0xFFDB2777))
                                    else
                                        listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = athleteName.ifBlank { "Master Athlete" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = categoryBadgeColor,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val ageText = if (athleteBirthYear in 1910..currentYear) {
                            val age = currentYear - athleteBirthYear
                            "${if (isFemale) "Kadın" else "Erkek"} • $athleteBirthYear ($age Yaş)"
                        } else {
                            "Kariyer & Sezon Takip Paneli"
                        }

                        Text(
                            text = ageText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzenle",
                            tint = categoryBadgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sekmeler
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = BluePrimary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sekme İçerikleri
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                when (selectedTab) {
                    0 -> {
                        // 1. TAKİBİMDEKİ YARIŞLAR
                        ProfileTrackedRacesSection(
                            trackedCompetitions = targetCompetitions,
                            completedIds = completedCompetitions.map { it.id }.toSet(),
                            onSelectCompetition = onSelectCompetition,
                            onToggleTrack = { viewModel.toggleTargetRace(it.id) },
                            onToggleCompleted = { viewModel.toggleCompletedRace(it.id) }
                        )
                    }
                    1 -> {
                        // 2. DERECELERİM (Canlı PB / SB Listesi)
                        ProfileRecordsSection(
                            records = athleteRecords,
                            onAddNewRecord = {
                                recordPresetCompTitle = ""
                                recordPresetYear = Calendar.getInstance().get(Calendar.YEAR)
                                isFixedCompetition = false
                                showAddRecordDialog = true
                            },
                            onDeleteRecord = { viewModel.deleteAthleteRecord(it) }
                        )
                    }
                    2 -> {
                        // 3. GEÇMİŞİM (Katıldığım Yarışlar)
                        ProfileCompletedHistorySection(
                            completedCompetitions = completedCompetitions,
                            records = athleteRecords,
                            onSelectCompetition = onSelectCompetition,
                            onRemoveFromCompleted = { viewModel.toggleCompletedRace(it.id) },
                            onAddRecordForCompetition = { comp ->
                                recordPresetCompTitle = comp.title
                                val year = comp.start_date.take(4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                                recordPresetYear = year
                                isFixedCompetition = true
                                showAddRecordDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Profil Düzenleme Penceresi
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = athleteName,
            currentGender = athleteGender,
            currentBirthYear = if (athleteBirthYear > 0) athleteBirthYear.toString() else "",
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, gender, birthYear ->
                val yearInt = birthYear.toIntOrNull() ?: 1990
                viewModel.updateAthleteProfile(name, gender, yearInt)
                showEditProfileDialog = false
            },
            viewModel = viewModel
        )
    }

    // Modern Derece Kaydetme Alt Menüsü (ModalBottomSheet)
    if (showAddRecordDialog) {
        RecordEntryBottomSheet(
            sheetState = recordSheetState,
            athleteGender = athleteGender,
            athleteBirthYear = athleteBirthYear,
            allCompetitions = allCompetitions,
            initialCompTitle = recordPresetCompTitle,
            initialYear = recordPresetYear,
            isFixedCompetition = isFixedCompetition,
            onDismiss = { showAddRecordDialog = false },
            onSave = { record ->
                viewModel.saveAthleteRecord(record)
                showAddRecordDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// 1. TAKİBİMDEKİ YARIŞLAR
// -------------------------------------------------------------
@Composable
private fun ProfileTrackedRacesSection(
    trackedCompetitions: List<Competition>,
    completedIds: Set<String>,
    onSelectCompetition: (Competition) -> Unit,
    onToggleTrack: (Competition) -> Unit,
    onToggleCompleted: (Competition) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TAKİBİMDEKİ YARIŞLAR (${trackedCompetitions.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = BluePrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (trackedCompetitions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Henüz Takibe Alınan Yarış Yok",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ana sayfadaki yarış kartlarında bulunan yer işareti (🔖) simgesine dokunarak yarışları tek tıkla takibe alabilirsin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            trackedCompetitions.forEach { comp ->
                val daysRemaining = comp.getDaysRemaining()
                val isCompleted = completedIds.contains(comp.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable { onSelectCompetition(comp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = comp.getCountryFlag(), fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comp.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${comp.getFormattedDate()} • ${comp.getDisplayLocation()}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            CountdownBadge(daysRemaining = daysRemaining)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onToggleTrack(comp) },
                                colors = ButtonDefaults.textButtonColors(contentColor = CrimsonAccent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkRemove,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Takipten Çıkar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (daysRemaining < 0) {
                                OutlinedButton(
                                    onClick = { onToggleCompleted(comp) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = if (isCompleted) {
                                        ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A))
                                    } else {
                                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isCompleted) "Katıldım ✓" else "Katıldım Olarak İşaretle",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Surface(
                                    color = BluePrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Katılacağım 🏃",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. DERECELERİM BÖLÜMÜ (CANLI PB/SB & MADALYA/REKOR ROZETLERİ)
// -------------------------------------------------------------
@Composable
private fun ProfileRecordsSection(
    records: List<AthleteRecord>,
    onAddNewRecord: () -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KİŞİSEL VE SEZON DERECELERİM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = BluePrimary
            )

            Button(
                onClick = onAddNewRecord,
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Derece Ekle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (records.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Henüz Kayıtlı Derece Yok",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Yukarıdaki 'Derece Ekle' butonuyla branşlarına ait kariyer derecelerini girebilir veya geçmiş yarışlarından sonuçlarını kaydedebilirsin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val groupedByEvent = records.groupBy { it.eventName }

            groupedByEvent.forEach { (eventName, eventRecords) ->
                val isField = MasterAthleticsHelper.isFieldEvent(eventName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = eventName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Kategori bazında grupla (Kullanıcı talebi: o kategoride tek derece varsa o PB'dir, yanına yaz)
                        val groupedByCategory = eventRecords.groupBy { it.category }

                        groupedByCategory.forEach { (categoryName, catRecords) ->
                            // En iyi dereceyi (PB) bul
                            val bestRecord = if (isField) {
                                catRecords.maxByOrNull { it.numericValue }
                            } else {
                                catRecords.filter { it.numericValue > 0 }.minByOrNull { it.numericValue } ?: catRecords.firstOrNull()
                            }

                            // İçinde bulunulan yıla ait en iyi derece (SB)
                            val currentYearBest = catRecords.filter { it.year == currentYear }.let { curRecords ->
                                if (isField) curRecords.maxByOrNull { it.numericValue }
                                else curRecords.filter { it.numericValue > 0 }.minByOrNull { it.numericValue } ?: curRecords.firstOrNull()
                            }

                            catRecords.sortedWith(compareByDescending<AthleteRecord> { it.year }).forEach { rec ->
                                val isPersonalBest = (rec.id == bestRecord?.id)
                                val isSeasonBest = (rec.id == currentYearBest?.id && rec.year == currentYear)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            color = if (categoryName.startsWith("F", ignoreCase = true)) Color(0xFFEC4899) else Color(0xFF2563EB),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = categoryName.ifBlank { "MASTER" },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = rec.result,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                // PB Rozeti (Kategori için tek derece varsa veya en iyisiyse)
                                                if (isPersonalBest) {
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Surface(
                                                        color = CrimsonAccent,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "PB",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }

                                                // SB Rozeti (Güncel yıl derecesi)
                                                if (isSeasonBest) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = Color(0xFF16A34A),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "SB ($currentYear)",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }

                                                // Podyum Madalyası
                                                if (rec.podium.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    val medalText = when (rec.podium) {
                                                        "1" -> "🥇 1.lik"
                                                        "2" -> "🥈 2.lik"
                                                        "3" -> "🥉 3.lük"
                                                        else -> ""
                                                    }
                                                    if (medalText.isNotBlank()) {
                                                        Surface(
                                                            color = Color(0xFFFEF3C7),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = medalText,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF92400E),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Rekor Rozeti (NMR / BMR / EMR / WMR)
                                                if (rec.recordType.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = Color(0xFF7C3AED),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = rec.recordType,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (rec.competitionTitle.isNotBlank()) {
                                                Text(
                                                    text = "${rec.year} • ${rec.competitionTitle}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteRecord(rec.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Sil",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. GEÇMİŞİM BÖLÜMÜ
// -------------------------------------------------------------
@Composable
private fun ProfileCompletedHistorySection(
    completedCompetitions: List<Competition>,
    records: List<AthleteRecord>,
    onSelectCompetition: (Competition) -> Unit,
    onRemoveFromCompleted: (Competition) -> Unit,
    onAddRecordForCompetition: (Competition) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "YARIŞ GEÇMİŞİM & KATILIMLARIM (${completedCompetitions.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = BluePrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (completedCompetitions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFEAB308),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Henüz Katıldığın Bir Yarış Yok",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Takibindeki yarışların tarihi geçtiğinde 'Katıldım' olarak işaretlendiğinde buraya aktarılır ve koştuğun dereceleri kaydedebilirsin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            completedCompetitions.forEach { comp ->
                val compRecords = records.filter { it.competitionTitle.equals(comp.title, ignoreCase = true) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable { onSelectCompetition(comp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = comp.getCountryFlag(), fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comp.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${comp.getFormattedDate()} • ${comp.getDisplayLocation()}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF16A34A).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Katıldım ✓",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (compRecords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            compRecords.forEach { rec ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                ) {
                                    Text(
                                        text = "• ${rec.eventName}:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = rec.result,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (rec.podium.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val medal = when(rec.podium) { "1" -> "🥇 1.lik"; "2" -> "🥈 2.lik"; "3" -> "🥉 3.lük"; else -> "" }
                                        Text(text = medal, fontSize = 11.sp)
                                    }
                                    if (rec.recordType.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(color = Color(0xFF7C3AED), shape = RoundedCornerShape(3.dp)) {
                                            Text(text = rec.recordType, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onRemoveFromCompleted(comp) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            ) {
                                Text(text = "İptal Et", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onAddRecordForCompetition(comp) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Derece Ekle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODERN DERECE KAYDETME PENCERESİ (YAPILANDIRILMIŞ FORMATLAR)
// -------------------------------------------------------------
// -------------------------------------------------------------
// MODERN DERECE KAYDETME ALT MENÜSÜ (MODAL BOTTOM SHEET)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordEntryBottomSheet(
    sheetState: SheetState,
    athleteGender: String,
    athleteBirthYear: Int,
    allCompetitions: List<Competition>,
    initialCompTitle: String,
    initialYear: Int,
    isFixedCompetition: Boolean,
    onDismiss: () -> Unit,
    onSave: (AthleteRecord) -> Unit
) {
    var selectedEvent by remember { mutableStateOf("") }
    var customEventName by remember { mutableStateOf("") }
    var selectedCompTitle by remember { mutableStateOf(initialCompTitle) }
    var isManualCompInput by remember { mutableStateOf(false) }
    var yearInput by remember { mutableStateOf(initialYear.toString()) }
    var selectedPodium by remember { mutableStateOf("") } // "", "1", "2", "3"
    var selectedRecordType by remember { mutableStateOf("") } // "", "NMR", "BMR", "EMR", "WMR"

    // Format Değerleri (Kullanıcı sadece boşlukları doldurur)
    var inputHour by remember { mutableStateOf("") }
    var inputMinute by remember { mutableStateOf("") }
    var inputSecond by remember { mutableStateOf("") }
    var inputMillis by remember { mutableStateOf("") }
    var inputMeter by remember { mutableStateOf("") }
    var inputCentimeter by remember { mutableStateOf("") }
    var inputCustomResult by remember { mutableStateOf("") }

    var isEventDropdownExpanded by remember { mutableStateOf(false) }
    var isCompDropdownExpanded by remember { mutableStateOf(false) }

    val yearInt = yearInput.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val applicableEvents = remember(athleteGender, athleteBirthYear, yearInt) {
        MasterAthleticsHelper.getApplicableEvents(athleteGender, athleteBirthYear, yearInt)
    }

    if (selectedEvent.isBlank() && applicableEvents.isNotEmpty()) {
        selectedEvent = applicableEvents.first()
    }

    val formatType = MasterAthleticsHelper.getEventFormatType(selectedEvent)
    val calculatedCategory = MasterAthleticsHelper.calculateCategory(athleteGender, athleteBirthYear, yearInt)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Başlık & Kategori Rozeti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Derece Kaydet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (athleteGender.equals("F", ignoreCase = true)) Color(0xFFEC4899) else Color(0xFF2563EB),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = calculatedCategory,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Yarışma Bilgisi
            if (isFixedCompetition) {
                // Kullanıcı talebi: "yarışma sabit yazmasına gerek yok yarışma ismi yazsa yeter"
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🏆", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (initialYear > 0) "$initialCompTitle ($initialYear)" else initialCompTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Derecelerim sekmesinden gelindiğinde: Listeden seç veya elle yaz
                if (!isManualCompInput) {
                    ExposedDropdownMenuBox(
                        expanded = isCompDropdownExpanded,
                        onExpandedChange = { isCompDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCompTitle.ifBlank { "Yarışma Seçiniz veya Elle Yazınız" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Yarışma") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCompDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = isCompDropdownExpanded,
                            onDismissRequest = { isCompDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("✍️ Elle Özel Yarışma Yaz...", fontWeight = FontWeight.Bold, color = BluePrimary) },
                                onClick = {
                                    isManualCompInput = true
                                    isCompDropdownExpanded = false
                                }
                            )
                            allCompetitions.take(40).forEach { comp ->
                                DropdownMenuItem(
                                    text = { Text("${comp.getCountryFlag()} ${comp.title} (${comp.start_date.take(4)})", fontSize = 13.sp) },
                                    onClick = {
                                        selectedCompTitle = comp.title
                                        comp.start_date.take(4).toIntOrNull()?.let { yearInput = it.toString() }
                                        isCompDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = selectedCompTitle,
                        onValueChange = { selectedCompTitle = it },
                        label = { Text("Yarışma Adı (Elle Giriş)") },
                        trailingIcon = {
                            TextButton(onClick = { isManualCompInput = false }) {
                                Text("Listeden Seç", fontSize = 12.sp)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Yıl Alanı
                OutlinedTextField(
                    value = yearInput,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) yearInput = it },
                    label = { Text("Yarışma Yılı") },
                    placeholder = { Text("Örn: 2026") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Branş Seçici (WMA Standartları)
            ExposedDropdownMenuBox(
                expanded = isEventDropdownExpanded,
                onExpandedChange = { isEventDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedEvent,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Branş (WMA Standartları)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEventDropdownExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = isEventDropdownExpanded,
                    onDismissRequest = { isEventDropdownExpanded = false }
                ) {
                    applicableEvents.forEach { event ->
                        DropdownMenuItem(
                            text = { Text(event, fontSize = 13.sp) },
                            onClick = {
                                selectedEvent = event
                                isEventDropdownExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Diğer / Özel Branş...", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            selectedEvent = "Diğer"
                            isEventDropdownExpanded = false
                        }
                    )
                }
            }

            if (selectedEvent == "Diğer") {
                OutlinedTextField(
                    value = customEventName,
                    onValueChange = { customEventName = it },
                    label = { Text("Özel Branş Adı") },
                    placeholder = { Text("Örn: 300 Metre") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Yapılandırılmış Derece Girişi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DERECE GİRİŞİ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when (formatType) {
                        EventFormatType.SPRINT_SHORT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedTextField(
                                    value = inputSecond,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputSecond = it },
                                    label = { Text("Saniye") },
                                    placeholder = { Text("11") },
                                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp)
                                )
                                Text(text = " . ", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                                OutlinedTextField(
                                    value = inputMillis,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputMillis = it },
                                    label = { Text("Salise") },
                                    placeholder = { Text("84") },
                                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "sn", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                        }

                        EventFormatType.MIDDLE_LONG -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedTextField(
                                    value = inputMinute,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) inputMinute = it },
                                    label = { Text("Dk") },
                                    placeholder = { Text("02") },
                                    textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(text = " : ", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                OutlinedTextField(
                                    value = inputSecond,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputSecond = it },
                                    label = { Text("Sn") },
                                    placeholder = { Text("14") },
                                    textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(text = " . ", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                OutlinedTextField(
                                    value = inputMillis,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputMillis = it },
                                    label = { Text("Sal") },
                                    placeholder = { Text("20") },
                                    textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }

                        EventFormatType.MARATHON -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedTextField(
                                    value = inputHour,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputHour = it },
                                    label = { Text("Saat") },
                                    placeholder = { Text("1") },
                                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(text = " sa ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = inputMinute,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputMinute = it },
                                    label = { Text("Dk") },
                                    placeholder = { Text("24") },
                                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(text = " d ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = inputSecond,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputSecond = it },
                                    label = { Text("Sn") },
                                    placeholder = { Text("15") },
                                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(text = " sn", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        EventFormatType.FIELD -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedTextField(
                                    value = inputMeter,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputMeter = it },
                                    label = { Text("Metre") },
                                    placeholder = { Text("5") },
                                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp)
                                )
                                Text(text = " . ", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                                OutlinedTextField(
                                    value = inputCentimeter,
                                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) inputCentimeter = it },
                                    label = { Text("Santimetre") },
                                    placeholder = { Text("84") },
                                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.width(110.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "m", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                        }

                        EventFormatType.CUSTOM -> {
                            OutlinedTextField(
                                value = inputCustomResult,
                                onValueChange = { inputCustomResult = it },
                                label = { Text("Derece") },
                                placeholder = { Text("Örn: 11.84 sn veya 5.84 m") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 4. Podyum Derecesi (Madalyalar)
            Column {
                Text(text = "Podyum Derecesi (Madalya)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("" to "Yok", "1" to "🥇 1.lik", "2" to "🥈 2.lik", "3" to "🥉 3.lük").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedPodium == code,
                            onClick = { selectedPodium = code },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedPodium == code) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFF92400E)
                            )
                        )
                    }
                }
            }

            // 5. Rekor Seçeneği (WMR, EMR, BMR, NMR) - Harfler asla alta kaymaz!
            Column {
                Text(text = "Kategori / Şampiyona Rekoru", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("" to "Yok", "NMR" to "🇹🇷 NMR", "BMR" to "🏛️ BMR", "EMR" to "🇪🇺 EMR", "WMR" to "🌍 WMR").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedRecordType == code,
                            onClick = { selectedRecordType = code },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedRecordType == code) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7C3AED),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Kaydet Butonu (DetailBottomSheet tarzında geniş ve modern)
            Button(
                onClick = {
                    val finalEvent = if (selectedEvent == "Diğer") customEventName.ifBlank { "Özel Branş" } else selectedEvent
                    val (resultStr, numVal) = when (formatType) {
                        EventFormatType.SPRINT_SHORT -> {
                            val sec = inputSecond.toIntOrNull() ?: 0
                            val ms = inputMillis.toIntOrNull() ?: 0
                            val str = "$sec.${ms.toString().padStart(2, '0')} sn"
                            val num = sec + (ms / 100.0)
                            str to num
                        }
                        EventFormatType.MIDDLE_LONG -> {
                            val min = inputMinute.toIntOrNull() ?: 0
                            val sec = inputSecond.toIntOrNull() ?: 0
                            val ms = inputMillis.toIntOrNull() ?: 0
                            val str = "$min:${sec.toString().padStart(2, '0')}.${ms.toString().padStart(2, '0')}"
                            val num = (min * 60) + sec + (ms / 100.0)
                            str to num
                        }
                        EventFormatType.MARATHON -> {
                            val hr = inputHour.toIntOrNull() ?: 0
                            val min = inputMinute.toIntOrNull() ?: 0
                            val sec = inputSecond.toIntOrNull() ?: 0
                            val str = "$hr sa $min d $sec sn"
                            val num = (hr * 3600.0) + (min * 60.0) + sec
                            str to num
                        }
                        EventFormatType.FIELD -> {
                            val meter = inputMeter.toIntOrNull() ?: 0
                            val cm = inputCentimeter.toIntOrNull() ?: 0
                            val str = "$meter.${cm.toString().padStart(2, '0')} m"
                            val num = meter + (cm / 100.0)
                            str to num
                        }
                        EventFormatType.CUSTOM -> {
                            inputCustomResult to (inputCustomResult.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0)
                        }
                    }

                    if (resultStr.isNotBlank() && numVal > 0) {
                        val newRecord = AthleteRecord(
                            eventName = finalEvent,
                            category = calculatedCategory,
                            result = resultStr,
                            year = yearInt,
                            competitionTitle = selectedCompTitle.trim(),
                            podium = selectedPodium,
                            recordType = selectedRecordType,
                            numericValue = numVal
                        )
                        onSave(newRecord)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Dereceyi Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// PROFİL DÜZENLEME DİYALOĞU
// -------------------------------------------------------------
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentGender: String,
    currentBirthYear: String,
    onDismiss: () -> Unit,
    onSave: (name: String, gender: String, birthYear: String) -> Unit,
    viewModel: CompetitionViewModel
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var genderInput by remember { mutableStateOf(if (currentGender.isNotBlank()) currentGender else "M") }
    var birthYearInput by remember { mutableStateOf(currentBirthYear) }

    val tempYear = birthYearInput.toIntOrNull() ?: 1990
    val previewCategory = viewModel.calculateCategory(genderInput, tempYear)
    val isFemale = genderInput.equals("F", ignoreCase = true)
    val badgeColor = if (isFemale) Color(0xFFEC4899) else Color(0xFF2563EB)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sporcu Bilgilerini Düzenle",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Ad Soyad") },
                    placeholder = { Text("Örn: Master Atlet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Cinsiyet",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { genderInput = "M" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (genderInput == "M") Color(0xFF2563EB).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (genderInput == "M") androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Male,
                                    contentDescription = null,
                                    tint = if (genderInput == "M") Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Erkek (M)",
                                    fontSize = 13.sp,
                                    fontWeight = if (genderInput == "M") FontWeight.Bold else FontWeight.Medium,
                                    color = if (genderInput == "M") Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { genderInput = "F" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (genderInput == "F") Color(0xFFEC4899).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (genderInput == "F") androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEC4899)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Female,
                                    contentDescription = null,
                                    tint = if (genderInput == "F") Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kadın (F)",
                                    fontSize = 13.sp,
                                    fontWeight = if (genderInput == "F") FontWeight.Bold else FontWeight.Medium,
                                    color = if (genderInput == "F") Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = birthYearInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 4) {
                            birthYearInput = input
                        }
                    },
                    label = { Text("Doğum Yılı") },
                    placeholder = { Text("Örn: 1985") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hesaplanan Master Kategori:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = previewCategory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameInput, genderInput, birthYearInput) },
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
            ) {
                Text(text = "Kaydet", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "İptal")
            }
        }
    )
}
