package com.ecil.masterracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.R
import com.ecil.masterracer.data.model.Competition
import com.ecil.masterracer.ui.components.CareerSummaryShareBottomSheet
import com.ecil.masterracer.ui.components.CountdownBadge
import com.ecil.masterracer.ui.components.PerformanceAnalyticsSection
import com.ecil.masterracer.ui.components.SingleRecordShareBottomSheet
import com.ecil.masterracer.ui.components.SingleRecordShareData
import com.ecil.masterracer.ui.components.WmaCalculatorBottomSheet
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent
import com.ecil.masterracer.ui.viewmodel.CompetitionViewModel
import com.ecil.masterracer.util.AthleteRecord
import com.ecil.masterracer.util.EventFormatType
import com.ecil.masterracer.util.MasterAthleticsHelper
import com.ecil.masterracer.util.WmaAgeGradingHelper
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: CompetitionViewModel,
    allCompetitions: List<Competition> = emptyList(),
    targetCompetitions: List<Competition>,
    completedCompetitions: List<Competition> = emptyList(),
    onSelectCompetition: (Competition) -> Unit,
    onBack: () -> Unit
) {
    val athleteName by viewModel.athleteName.collectAsState()
    val athleteGender by viewModel.athleteGender.collectAsState()
    val athleteBirthYear by viewModel.athleteBirthYear.collectAsState()
    val athleteNationality by viewModel.athleteNationality.collectAsState()
    val athleteRecords by viewModel.athleteRecords.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var recordPresetCompTitle by remember { mutableStateOf("") }
    var recordPresetYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var isFixedCompetition by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<AthleteRecord?>(null) }
    val recordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Takibimdekiler", "Derecelerim", "📈 Gelişimim")
    var selectedAnalyticsEvent by remember { mutableStateOf<String?>(null) }

    val category = viewModel.calculateCategory(athleteGender, athleteBirthYear)
    val isFemale = athleteGender.equals("F", ignoreCase = true)
    val categoryBadgeColor = if (isFemale) Color(0xFFEC4899) else Color(0xFF2563EB)

    var showWmaCalculatorSheet by remember { mutableStateOf(false) }
    val wmaSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showCareerShareSheet by remember { mutableStateOf(false) }
    var sharingRecordData by remember { mutableStateOf<SingleRecordShareData?>(null) }
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        val natFlag = Competition.countryCodeToFlagEmoji(athleteNationality)
                        val natName = getNationalityDisplayName(athleteNationality)
                        val ageText = if (athleteBirthYear in 1910..currentYear) {
                            val age = currentYear - athleteBirthYear
                            "${if (isFemale) "Kadın" else "Erkek"} • $athleteBirthYear ($age Yaş) • $natFlag $natName"
                        } else {
                            "$natFlag $natName • Kariyer & Sezon Takip Paneli"
                        }

                        Text(
                            text = ageText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showCareerShareSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Kariyer Kartı Paylaş",
                                tint = categoryBadgeColor,
                                modifier = Modifier.size(20.dp)
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // WMA Yaş Faktörü & Performans Hesaplayıcı Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showWmaCalculatorSheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BluePrimary.copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BluePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "WMA Yaş Faktörü & Performans Hesaplayıcı",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Açık Yaş Elit dengi ve dünya klasman yüzdesi hesapla",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Hesaplayıcıyı Aç",
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sekmeler (Takibimdekiler | Derecelerim)
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
                            onSelectCompetition = onSelectCompetition,
                            onToggleTrack = { viewModel.toggleTargetRace(it.id) },
                            onAddRecordForCompetition = { comp ->
                                editingRecord = null
                                recordPresetCompTitle = comp.getDisplayTitle()
                                val year = comp.start_date.take(4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                                recordPresetYear = year
                                isFixedCompetition = true
                                showAddRecordDialog = true
                            }
                        )
                    }
                    1 -> {
                        // 2. DERECELERİM (Canlı PB / SB Listesi)
                        ProfileRecordsSection(
                            records = athleteRecords,
                            athleteName = athleteName,
                            athleteGender = athleteGender,
                            onAddNewRecord = {
                                editingRecord = null
                                recordPresetCompTitle = ""
                                recordPresetYear = Calendar.getInstance().get(Calendar.YEAR)
                                isFixedCompetition = false
                                showAddRecordDialog = true
                            },
                            onEditRecord = { record ->
                                editingRecord = record
                                recordPresetCompTitle = record.competitionTitle
                                recordPresetYear = record.year
                                isFixedCompetition = record.competitionTitle.isNotBlank()
                                showAddRecordDialog = true
                            },
                            onDeleteRecord = { viewModel.deleteAthleteRecord(it) },
                            onShareRecord = { shareData ->
                                sharingRecordData = shareData
                            },
                            onOpenEventAnalytics = { evName ->
                                selectedAnalyticsEvent = evName
                                selectedTab = 2
                            }
                        )
                    }
                    2 -> {
                        // 3. PERFORMANS İLERLEME GRAFİĞİ & KARİYER ANALİTİĞİ
                        PerformanceAnalyticsSection(
                            records = athleteRecords,
                            athleteGender = athleteGender,
                            initialSelectedEvent = selectedAnalyticsEvent,
                            onAddNewRecord = {
                                editingRecord = null
                                recordPresetCompTitle = ""
                                recordPresetYear = Calendar.getInstance().get(Calendar.YEAR)
                                isFixedCompetition = false
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
            currentNationality = athleteNationality,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, gender, birthYear, nationality ->
                val yearInt = birthYear.toIntOrNull() ?: 1990
                viewModel.updateAthleteProfile(name, gender, yearInt, nationality)
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
            editingRecord = editingRecord,
            onDismiss = {
                showAddRecordDialog = false
                editingRecord = null
            },
            onSave = { record ->
                viewModel.saveAthleteRecord(record)
                showAddRecordDialog = false
                editingRecord = null
                selectedTab = 1
            }
        )
    }

    // WMA Yaş Faktörü & Performans Hesaplayıcı Penceresi
    if (showWmaCalculatorSheet) {
        WmaCalculatorBottomSheet(
            sheetState = wmaSheetState,
            initialGender = athleteGender,
            initialBirthYear = athleteBirthYear,
            onDismiss = { showWmaCalculatorSheet = false },
            onSaveToProfile = { evName, resStr, numVal, cat ->
                val newRec = AthleteRecord(
                    eventName = evName,
                    category = cat,
                    result = resStr,
                    year = Calendar.getInstance().get(Calendar.YEAR),
                    competitionTitle = "Bireysel / WMA Kayıt",
                    numericValue = numVal
                )
                viewModel.saveAthleteRecord(newRec)
                selectedTab = 1
            }
        )
    }

    // Kariyer & PB Özeti Paylaşım Kartı Penceresi
    if (showCareerShareSheet) {
        CareerSummaryShareBottomSheet(
            sheetState = shareSheetState,
            athleteName = athleteName,
            athleteCategory = category,
            athleteGender = athleteGender,
            records = athleteRecords,
            onDismiss = { showCareerShareSheet = false }
        )
    }

    // Tekil Derece / Yarış Sonucu Paylaşım Kartı Penceresi
    sharingRecordData?.let { shareData ->
        SingleRecordShareBottomSheet(
            sheetState = shareSheetState,
            data = shareData,
            onDismiss = { sharingRecordData = null }
        )
    }
}

// -------------------------------------------------------------
// 1. TAKİBİMDEKİ YARIŞLAR
// -------------------------------------------------------------
@Composable
private fun ProfileTrackedRacesSection(
    trackedCompetitions: List<Competition>,
    onSelectCompetition: (Competition) -> Unit,
    onToggleTrack: (Competition) -> Unit,
    onAddRecordForCompetition: (Competition) -> Unit
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
                                    text = comp.getDisplayTitle(),
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
                                Button(
                                    onClick = { onAddRecordForCompetition(comp) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Derece Ekle",
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileRecordsSection(
    records: List<AthleteRecord>,
    athleteName: String,
    athleteGender: String,
    onAddNewRecord: () -> Unit,
    onEditRecord: (AthleteRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onShareRecord: (SingleRecordShareData) -> Unit,
    onOpenEventAnalytics: (String) -> Unit
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
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
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
                val lowerEvent = eventName.lowercase(Locale.forLanguageTag("tr-TR"))
                val isJump = lowerEvent.contains("atlama")
                val isThrow = lowerEvent.contains("atma")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Branş Başlığı & Branşa Özel İkon (Atlama / Atma / Koşu) + Hızlı Gelişim Grafiği Butonu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(BluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isJump -> Icon(
                                            painter = painterResource(id = R.drawable.ic_event_jump),
                                            contentDescription = "Atlama Branşı",
                                            tint = BluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        isThrow -> Icon(
                                            painter = painterResource(id = R.drawable.ic_event_throw),
                                            contentDescription = "Atma Branşı",
                                            tint = BluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        else -> Icon(
                                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                            contentDescription = "Koşu Branşı",
                                            tint = BluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = eventName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                modifier = Modifier.clickable { onOpenEventAnalytics(eventName) },
                                color = BluePrimary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Gelişim Grafiği",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gelişim",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Kategori bazında grupla
                        val groupedByCategory = eventRecords.groupBy { it.category }
                        var recordIndex = 0

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
                                if (recordIndex > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                recordIndex++

                                val isPersonalBest = (rec.id == bestRecord?.id)
                                val isSeasonBest = (rec.id == currentYearBest?.id && rec.year == currentYear)

                                val wmaResult = remember(rec.numericValue, eventName, categoryName) {
                                    if (rec.numericValue > 0.0) {
                                        val eventConfig = WmaAgeGradingHelper.findEventConfig(eventName)
                                        if (eventConfig != null) {
                                            val recordAge = if (categoryName.length >= 3) {
                                                categoryName.substring(1).toIntOrNull() ?: 40
                                            } else 40
                                            WmaAgeGradingHelper.evaluatePerformance(
                                                gender = athleteGender,
                                                age = recordAge,
                                                eventKey = eventConfig.key,
                                                numericValue = rec.numericValue
                                            )
                                        } else null
                                    } else null
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // 1. Satır: Kategori + Derece + Düzenle/Sil Butonları
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                color = if (categoryName.startsWith("F", ignoreCase = true) || categoryName.startsWith("W", ignoreCase = true))
                                                    Color(0xFFEC4899)
                                                else
                                                    Color(0xFF2563EB),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = categoryName.ifBlank { "MASTER" },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Text(
                                                text = rec.result,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    onShareRecord(
                                                        SingleRecordShareData(
                                                            athleteName = athleteName,
                                                            category = categoryName.ifBlank { "MASTER" },
                                                            eventName = eventName,
                                                            result = rec.result,
                                                            year = rec.year,
                                                            competitionTitle = rec.competitionTitle,
                                                            isPb = isPersonalBest,
                                                            isSb = isSeasonBest,
                                                            podium = rec.podium,
                                                            recordType = rec.recordType,
                                                            wmaPercent = wmaResult?.scorePercentage
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Dereceyi Paylaş",
                                                    tint = BluePrimary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onEditRecord(rec) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Düzenle",
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteRecord(rec.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Sil",
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 2. Satır: Rozetler (PB, SB, Madalya, Rekor, WMA Yüzdesi) - Ferah FlowRow
                                    val hasAnyBadge = isPersonalBest || isSeasonBest || rec.podium.isNotBlank() || rec.recordType.isNotBlank() || wmaResult != null
                                    if (hasAnyBadge) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isPersonalBest) {
                                                Surface(
                                                    color = CrimsonAccent,
                                                    shape = RoundedCornerShape(5.dp)
                                                ) {
                                                    Text(
                                                        text = "PB",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (isSeasonBest) {
                                                Surface(
                                                    color = Color(0xFF16A34A),
                                                    shape = RoundedCornerShape(5.dp)
                                                ) {
                                                    Text(
                                                        text = "SB ($currentYear)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (rec.podium.isNotBlank()) {
                                                val medalText = when (rec.podium) {
                                                    "1" -> "🥇 1.lik"
                                                    "2" -> "🥈 2.lik"
                                                    "3" -> "🥉 3.lük"
                                                    else -> ""
                                                }
                                                if (medalText.isNotBlank()) {
                                                    Surface(
                                                        color = Color(0xFFFEF3C7),
                                                        shape = RoundedCornerShape(5.dp)
                                                    ) {
                                                        Text(
                                                            text = medalText,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF92400E),
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (rec.recordType.isNotBlank()) {
                                                Surface(
                                                    color = Color(0xFF7C3AED),
                                                    shape = RoundedCornerShape(5.dp)
                                                ) {
                                                    Text(
                                                        text = rec.recordType,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            wmaResult?.let { wma ->
                                                Surface(
                                                    color = Color(wma.badgeColorHex).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(5.dp)
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "%%%.1f WMA", wma.scorePercentage),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(wma.badgeColorHex),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 3. Satır: Yarışma Yılı ve Adı
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val subtitleText = if (rec.competitionTitle.isNotBlank()) {
                                        "${rec.year} • ${rec.competitionTitle}"
                                    } else {
                                        "${rec.year}"
                                    }
                                    Text(
                                        text = subtitleText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
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
// MODERN DERECE KAYDETME ALT MENÜSÜ (MODAL BOTTOM SHEET)
// -------------------------------------------------------------
@Composable
private fun PermanentLabeledInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLength: Int = 2,
    width: androidx.compose.ui.unit.Dp = 95.dp,
    containerBgColor: Color
) {
    Box(
        modifier = Modifier.width(width),
        contentAlignment = Alignment.TopStart
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= maxLength) {
                    onValueChange(input)
                }
            },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BluePrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                focusedContainerColor = containerBgColor,
                unfocusedContainerColor = containerBgColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Surface(
            color = containerBgColor,
            modifier = Modifier
                .padding(start = 12.dp)
                .offset(y = 0.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

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
    editingRecord: AthleteRecord? = null,
    onDismiss: () -> Unit,
    onSave: (AthleteRecord) -> Unit
) {
    var selectedEvent by remember(editingRecord) { mutableStateOf(editingRecord?.eventName ?: "") }
    var customEventName by remember(editingRecord) { mutableStateOf("") }
    var selectedCompTitle by remember(editingRecord) { mutableStateOf(editingRecord?.competitionTitle ?: initialCompTitle) }
    var isManualCompInput by remember(editingRecord) {
        mutableStateOf(editingRecord?.competitionTitle?.isNotBlank() == true && allCompetitions.none { it.title == editingRecord.competitionTitle })
    }
    var yearInput by remember(editingRecord) { mutableStateOf((editingRecord?.year ?: initialYear).toString()) }
    var selectedPodium by remember(editingRecord) { mutableStateOf(editingRecord?.podium ?: "") }
    var selectedRecordType by remember(editingRecord) { mutableStateOf(editingRecord?.recordType ?: "") }

    val initialParsed = remember(editingRecord) {
        val rec = editingRecord
        if (rec != null) {
            val res = rec.result
            when (MasterAthleticsHelper.getEventFormatType(rec.eventName)) {
                EventFormatType.SPRINT_SHORT -> {
                    val clean = res.replace("sn", "").trim()
                    val parts = clean.split(".")
                    val s = parts.getOrNull(0) ?: ""
                    val ms = parts.getOrNull(1) ?: ""
                    mapOf("sec" to s, "ms" to ms)
                }
                EventFormatType.FIELD -> {
                    val clean = res.replace("m", "").trim()
                    val parts = clean.split(".")
                    val m = parts.getOrNull(0) ?: ""
                    val cm = parts.getOrNull(1) ?: ""
                    mapOf("meter" to m, "cm" to cm)
                }
                EventFormatType.MIDDLE_LONG -> {
                    val parts = res.split(":")
                    val min = parts.getOrNull(0) ?: ""
                    val secParts = parts.getOrNull(1)?.split(".") ?: emptyList()
                    val s = secParts.getOrNull(0) ?: ""
                    val ms = secParts.getOrNull(1) ?: ""
                    mapOf("min" to min, "sec" to s, "ms" to ms)
                }
                EventFormatType.MARATHON -> {
                    if (res.contains("sa")) {
                        val hr = res.substringBefore("sa").trim()
                        val min = res.substringAfter("sa").substringBefore("d").trim()
                        val sec = res.substringAfter("d").replace("sn", "").trim()
                        mapOf("hr" to hr, "min" to min, "sec" to sec)
                    } else if (res.contains(":")) {
                        val parts = res.split(":")
                        mapOf("hr" to (parts.getOrNull(0) ?: ""), "min" to (parts.getOrNull(1) ?: ""), "sec" to (parts.getOrNull(2) ?: ""))
                    } else {
                        emptyMap()
                    }
                }
                EventFormatType.CUSTOM -> {
                    mapOf("custom" to res)
                }
            }
        } else {
            emptyMap()
        }
    }

    var inputHour by remember(editingRecord) { mutableStateOf(initialParsed["hr"] ?: "") }
    var inputMinute by remember(editingRecord) { mutableStateOf(initialParsed["min"] ?: "") }
    var inputSecond by remember(editingRecord) { mutableStateOf(initialParsed["sec"] ?: "") }
    var inputMillis by remember(editingRecord) { mutableStateOf(initialParsed["ms"] ?: "") }
    var inputMeter by remember(editingRecord) { mutableStateOf(initialParsed["meter"] ?: "") }
    var inputCentimeter by remember(editingRecord) { mutableStateOf(initialParsed["cm"] ?: "") }
    var inputCustomResult by remember(editingRecord) { mutableStateOf(initialParsed["custom"] ?: "") }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingRecord != null) "Dereceyi Düzenle" else "Derece Kaydet",
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
                                    text = { Text("${comp.getCountryFlag()} ${comp.getDisplayTitle()} (${comp.start_date.take(4)})", fontSize = 13.sp) },
                                    onClick = {
                                        selectedCompTitle = comp.getDisplayTitle()
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
            val isDark = isSystemInDarkTheme()
            val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
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
                    Spacer(modifier = Modifier.height(6.dp))

                    when (formatType) {
                        EventFormatType.SPRINT_SHORT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PermanentLabeledInput(
                                    value = inputSecond,
                                    onValueChange = { inputSecond = it },
                                    label = "Saniye",
                                    maxLength = 2,
                                    width = 100.dp,
                                    containerBgColor = cardBg
                                )
                                Text(
                                    text = " . ",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                                )
                                PermanentLabeledInput(
                                    value = inputMillis,
                                    onValueChange = { inputMillis = it },
                                    label = "Salise",
                                    maxLength = 2,
                                    width = 100.dp,
                                    containerBgColor = cardBg
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "sn",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        EventFormatType.MIDDLE_LONG -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PermanentLabeledInput(
                                    value = inputMinute,
                                    onValueChange = { inputMinute = it },
                                    label = "Dakika",
                                    maxLength = 3,
                                    width = 85.dp,
                                    containerBgColor = cardBg
                                )
                                Text(
                                    text = " : ",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)
                                )
                                PermanentLabeledInput(
                                    value = inputSecond,
                                    onValueChange = { inputSecond = it },
                                    label = "Saniye",
                                    maxLength = 2,
                                    width = 85.dp,
                                    containerBgColor = cardBg
                                )
                                Text(
                                    text = " . ",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)
                                )
                                PermanentLabeledInput(
                                    value = inputMillis,
                                    onValueChange = { inputMillis = it },
                                    label = "Salise",
                                    maxLength = 2,
                                    width = 85.dp,
                                    containerBgColor = cardBg
                                )
                            }
                        }

                        EventFormatType.MARATHON -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PermanentLabeledInput(
                                    value = inputHour,
                                    onValueChange = { inputHour = it },
                                    label = "Saat",
                                    maxLength = 2,
                                    width = 85.dp,
                                    containerBgColor = cardBg
                                )
                                Text(
                                    text = " : ",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)
                                )
                                PermanentLabeledInput(
                                    value = inputMinute,
                                    onValueChange = { inputMinute = it },
                                    label = "Dakika",
                                    maxLength = 2,
                                    width = 85.dp,
                                    containerBgColor = cardBg
                                )
                                Text(
                                    text = " : ",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)
                                )
                                PermanentLabeledInput(
                                    value = inputSecond,
                                    onValueChange = { inputSecond = it },
                                    label = "Saniye",
                                    maxLength = 2,
                                    width = 85.dp,
                                    containerBgColor = cardBg
                                )
                            }
                        }

                        EventFormatType.FIELD -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PermanentLabeledInput(
                                    value = inputMeter,
                                    onValueChange = { inputMeter = it },
                                    label = "Metre",
                                    maxLength = 3,
                                    width = 100.dp,
                                    containerBgColor = cardBg
                                )
                                Text(
                                    text = " . ",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                                )
                                PermanentLabeledInput(
                                    value = inputCentimeter,
                                    onValueChange = { inputCentimeter = it },
                                    label = "Santimetre",
                                    maxLength = 2,
                                    width = 120.dp,
                                    containerBgColor = cardBg
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "m",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        EventFormatType.CUSTOM -> {
                            OutlinedTextField(
                                value = inputCustomResult,
                                onValueChange = { inputCustomResult = it },
                                label = { Text("Derece") },
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

            // 5. Rekor Seçeneği (WMR, EMR, BMR, NMR)
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

            // Kaydet Butonu
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
                        val savedRecord = editingRecord?.copy(
                            eventName = finalEvent,
                            category = calculatedCategory,
                            result = resultStr,
                            year = yearInt,
                            competitionTitle = (if (isFixedCompetition) initialCompTitle else selectedCompTitle).trim(),
                            podium = selectedPodium,
                            recordType = selectedRecordType,
                            numericValue = numVal
                        ) ?: AthleteRecord(
                            eventName = finalEvent,
                            category = calculatedCategory,
                            result = resultStr,
                            year = yearInt,
                            competitionTitle = (if (isFixedCompetition) initialCompTitle else selectedCompTitle).trim(),
                            podium = selectedPodium,
                            recordType = selectedRecordType,
                            numericValue = numVal
                        )
                        onSave(savedRecord)
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
                Text(
                    text = if (editingRecord != null) "Değişiklikleri Kaydet" else "Dereceyi Kaydet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// VATANDAŞLIK (ÜLKE) LİSTESİ & YARDIMCI FONKSİYONLAR
// -------------------------------------------------------------
private val NATIONALITY_COUNTRIES: List<Pair<String, String>> by lazy {
    val raw = listOf(
        "TR" to "Türkiye",
        "US" to "ABD",
        "DE" to "Almanya",
        "AL" to "Arnavutluk",
        "AU" to "Avustralya",
        "AT" to "Avusturya",
        "AZ" to "Azerbaycan",
        "BE" to "Belçika",
        "GB" to "Birleşik Krallık",
        "BA" to "Bosna-Hersek",
        "BR" to "Brezilya",
        "BG" to "Bulgaristan",
        "CZ" to "Çekya",
        "DK" to "Danimarka",
        "EE" to "Estonya",
        "FI" to "Finlandiya",
        "FR" to "Fransa",
        "ZA" to "Güney Afrika",
        "KR" to "Güney Kore",
        "GE" to "Gürcistan",
        "HR" to "Hırvatistan",
        "NL" to "Hollanda",
        "IE" to "İrlanda",
        "ES" to "İspanya",
        "SE" to "İsveç",
        "CH" to "İsviçre",
        "IT" to "İtalya",
        "IS" to "İzlanda",
        "JP" to "Japonya",
        "CA" to "Kanada",
        "ME" to "Karadağ",
        "XK" to "Kosova",
        "CY" to "Kuzey Kıbrıs / Kıbrıs",
        "MK" to "Kuzey Makedonya",
        "LV" to "Letonya",
        "LT" to "Litvanya",
        "LU" to "Lüksemburg",
        "HU" to "Macaristan",
        "MT" to "Malta",
        "MD" to "Moldova",
        "NO" to "Norveç",
        "PL" to "Polonya",
        "PT" to "Portekiz",
        "RO" to "Romanya",
        "RS" to "Sırbistan",
        "SK" to "Slovakya",
        "SI" to "Slovenya",
        "UA" to "Ukrayna",
        "NZ" to "Yeni Zelanda",
        "GR" to "Yunanistan"
    )
    val trCollator = java.text.Collator.getInstance(Locale("tr", "TR"))
    val trItem = raw.first { it.first == "TR" }
    val othersSorted = raw.filter { it.first != "TR" }.sortedWith(compareBy(trCollator) { it.second })
    listOf(trItem) + othersSorted
}

private fun getNationalityDisplayName(code: String): String {
    val clean = code.trim().uppercase()
    return NATIONALITY_COUNTRIES.firstOrNull { it.first == clean }?.second ?: "Türkiye"
}

// -------------------------------------------------------------
// PROFİL DÜZENLEME DİYALOĞU
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentGender: String,
    currentBirthYear: String,
    currentNationality: String = "TR",
    onDismiss: () -> Unit,
    onSave: (name: String, gender: String, birthYear: String, nationality: String) -> Unit,
    viewModel: CompetitionViewModel
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var genderInput by remember { mutableStateOf(if (currentGender.isNotBlank()) currentGender else "M") }
    var birthYearInput by remember { mutableStateOf(currentBirthYear) }
    var nationalityInput by remember { mutableStateOf(currentNationality.ifBlank { "TR" }) }
    var isNationalityExpanded by remember { mutableStateOf(false) }

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

                // Vatandaşlık Seçici (Varsayılan: Türkiye)
                ExposedDropdownMenuBox(
                    expanded = isNationalityExpanded,
                    onExpandedChange = { isNationalityExpanded = it }
                ) {
                    val selectedNatName = getNationalityDisplayName(nationalityInput)
                    val selectedNatFlag = Competition.countryCodeToFlagEmoji(nationalityInput)
                    OutlinedTextField(
                        value = "$selectedNatFlag $selectedNatName",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vatandaşlık") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNationalityExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isNationalityExpanded,
                        onDismissRequest = { isNationalityExpanded = false }
                    ) {
                        NATIONALITY_COUNTRIES.forEach { (code, name) ->
                            val flag = Competition.countryCodeToFlagEmoji(code)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$flag $name",
                                        fontSize = 14.sp,
                                        fontWeight = if (code == nationalityInput) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    nationalityInput = code
                                    isNationalityExpanded = false
                                }
                            )
                        }
                    }
                }

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
                onClick = { onSave(nameInput, genderInput, birthYearInput, nationalityInput) },
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
