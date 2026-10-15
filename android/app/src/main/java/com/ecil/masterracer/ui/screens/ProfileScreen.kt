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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.data.model.Competition
import com.ecil.masterracer.ui.components.CountdownBadge
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent
import com.ecil.masterracer.ui.viewmodel.CompetitionViewModel
import com.ecil.masterracer.util.AthleteRecord
import com.ecil.masterracer.util.MasterAthleticsHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: CompetitionViewModel,
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
    var recordToEdit by remember { mutableStateOf<AthleteRecord?>(null) }
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var recordPresetCompTitle by remember { mutableStateOf("") }
    var recordPresetYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

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
                    // Kullanıcı talebi: Başlıkta doğrudan "Profilim" yazar (isim aşağıda kartta gösterilir)
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
                    // Avatar (Cinsiyete Göre Pembe / Mavi Degrade)
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

            // Sekmeler (Takibimdekiler, Derecelerim, Geçmişim)
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
                        // 2. DERECELERİM (Canlı Kayıtlar, PB ve SB)
                        ProfileRecordsSection(
                            records = athleteRecords,
                            onAddNewRecord = {
                                recordPresetCompTitle = ""
                                recordPresetYear = Calendar.getInstance().get(Calendar.YEAR)
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

    // Derece Ekleme / Güncelleme Penceresi
    if (showAddRecordDialog) {
        RecordEntryDialog(
            athleteGender = athleteGender,
            athleteBirthYear = athleteBirthYear,
            initialCompTitle = recordPresetCompTitle,
            initialYear = recordPresetYear,
            onDismiss = { showAddRecordDialog = false },
            onSave = { record ->
                viewModel.saveAthleteRecord(record)
                showAddRecordDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// 1. TAKİBİMDEKİ YARIŞLAR BÖLÜMÜ
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
                        text = "Ana sayfadaki yarış kartlarında bulunan yer işareti (🔖) simgesine dokunarak veya yarış detayından 'Takibime Ekle' butonuna basarak yarışları buraya ekleyebilirsin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

                        // Aksiyon Butonları (Takipten Çıkar & Katıldım/Katılacağım)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Takipten Çıkar
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

                            // Katılım Durumu
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
// 2. DERECELERİM BÖLÜMÜ (PB / SB & YAŞ KATEGORİSİ BAZLI)
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
                        imageVector = Icons.Default.Speed,
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
                        text = "Yukarıdaki 'Derece Ekle' butonuyla branşlarına ait kariyer derecelerini girebilir veya geçmiş yarışlarından koştuğun sonuçları kaydedebilirsin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Branşa göre grupla (Örn: 100 Metre, 110m Engelli)
            val groupedByEvent = records.groupBy { it.eventName }

            groupedByEvent.forEach { (eventName, eventRecords) ->
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
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
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

                        // Her kategori ve yıl için ayrı satır (Kullanıcı talebi: M35 derecesi ayrı, M40 ayrı satırda gösterilir)
                        eventRecords.sortedWith(compareByDescending<AthleteRecord> { it.year }.thenBy { it.category }).forEach { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Kategori Rozeti (M35, M40 vb.)
                                    Surface(
                                        color = if (rec.category.startsWith("F", ignoreCase = true)) Color(0xFFEC4899) else Color(0xFF2563EB),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = rec.category.ifBlank { "MASTER" },
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
                                            if (rec.year == currentYear) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFF16A34A).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "SB ($currentYear)",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF16A34A),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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

// -------------------------------------------------------------
// 3. GEÇMİŞİM BÖLÜMÜ (KATILDIĞIM YARIŞLAR)
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
                        text = "Takibindeki yarışların tarihi geçtiğinde 'Katıldım' olarak işaretlediğinde otomatik olarak buraya aktarılır ve koştuğun dereceleri ekleyebilirsin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            completedCompetitions.forEach { comp ->
                // Bu yarışmayla eşleşen dereceler
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

                        // Varsa bu yarışmaya ait kayıtlı dereceler
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
                                    if (rec.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(${rec.notes})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Alt Aksiyonlar
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
// DERECE EKLEME / GÜNCELLEME DİYALOĞU
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordEntryDialog(
    athleteGender: String,
    athleteBirthYear: Int,
    initialCompTitle: String,
    initialYear: Int,
    onDismiss: () -> Unit,
    onSave: (AthleteRecord) -> Unit
) {
    var selectedEvent by remember { mutableStateOf("") }
    var customEventName by remember { mutableStateOf("") }
    var resultInput by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf(initialYear.toString()) }
    var compTitleInput by remember { mutableStateOf(initialCompTitle) }
    var notesInput by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    val yearInt = yearInput.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val applicableEvents = remember(athleteGender, athleteBirthYear, yearInt) {
        MasterAthleticsHelper.getApplicableEvents(athleteGender, athleteBirthYear, yearInt)
    }

    if (selectedEvent.isBlank() && applicableEvents.isNotEmpty()) {
        selectedEvent = applicableEvents.first()
    }

    // WMA yaş kategorisi (o yılın yaşına göre otomatik)
    val calculatedCategory = MasterAthleticsHelper.calculateCategory(athleteGender, athleteBirthYear, yearInt)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Derece Kaydet",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Branş Seçici (WMA Yaşa ve Cinsiyete Göre Dinamik)
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEvent,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Branş Seçiniz (WMA)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        applicableEvents.forEach { event ->
                            DropdownMenuItem(
                                text = { Text(event, fontSize = 13.sp) },
                                onClick = {
                                    selectedEvent = event
                                    isExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Diğer / Özel Branş...", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedEvent = "Diğer"
                                isExpanded = false
                            }
                        )
                    }
                }

                if (selectedEvent == "Diğer") {
                    OutlinedTextField(
                        value = customEventName,
                        onValueChange = { customEventName = it },
                        label = { Text("Branş Adı") },
                        placeholder = { Text("Örn: 300 Metre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Derece / Zaman
                OutlinedTextField(
                    value = resultInput,
                    onValueChange = { resultInput = it },
                    label = { Text("Derece") },
                    placeholder = { Text("Örn: 11.84 sn veya 1:24:15 veya 5.84 m") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Yarışma Yılı ve Kategori Bilgisi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = yearInput,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) yearInput = it },
                        label = { Text("Yıl") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        color = if (athleteGender.equals("F", ignoreCase = true)) Color(0xFFEC4899) else Color(0xFF2563EB),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Kategori", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(text = calculatedCategory, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Şampiyona / Yarışma Adı
                OutlinedTextField(
                    value = compTitleInput,
                    onValueChange = { compTitleInput = it },
                    label = { Text("Şampiyona / Yarışma") },
                    placeholder = { Text("Örn: Craiova BAMACS 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notlar (İsteğe bağlı)
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notlar (İsteğe Bağlı)") },
                    placeholder = { Text("Örn: Rüzgar +1.2 m/s, Altın Madalya") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalEvent = if (selectedEvent == "Diğer") customEventName.ifBlank { "Özel Branş" } else selectedEvent
                    if (resultInput.isNotBlank()) {
                        val newRecord = AthleteRecord(
                            eventName = finalEvent,
                            category = calculatedCategory,
                            result = resultInput.trim(),
                            year = yearInt,
                            competitionTitle = compTitleInput.trim(),
                            notes = notesInput.trim()
                        )
                        onSave(newRecord)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
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

// -------------------------------------------------------------
// PROFİL BİLGİLERİ DÜZENLEME DİYALOĞU
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
                // İsim Alanı
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Ad Soyad") },
                    placeholder = { Text("Örn: Master Atlet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Cinsiyet Seçimi
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
                        // Erkek Butonu
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

                        // Kadın Butonu
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

                // Doğum Yılı Alanı
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

                // Canlı Kategori Önizlemesi
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
