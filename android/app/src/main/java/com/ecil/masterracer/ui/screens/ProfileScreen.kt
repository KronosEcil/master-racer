package com.ecil.masterracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Hedeflerim", "Derecelerim (PB/SB)", "Geçmişim")

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = athleteName.ifBlank { "Master Athlete" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Cinsiyete bağlı Mavi (Erkek) veya Pembe (Kadın) Arkaplanlı Kategori Rozeti
                        Surface(
                            color = categoryBadgeColor,
                            shape = RoundedCornerShape(6.dp),
                            shadowElevation = 2.dp
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
                },
                actions = {
                    // İsmin, cinsiyetin ve doğum yılının düzenlendiği simge
                    IconButton(onClick = { showEditDialog = true }) {
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

            // Profil Üst Özet Kartı
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
                    // Sporcu Avatarı
                    Box(
                        modifier = Modifier
                            .size(54.dp)
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
                            modifier = Modifier.size(30.dp)
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
                        val ageText = if (athleteBirthYear in 1920..currentYear) {
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
                        onClick = { showEditDialog = true },
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
                        // 1. HEDEF YARIŞLARIM
                        ProfileTargetRacesSection(
                            targetCompetitions = targetCompetitions,
                            onSelectCompetition = onSelectCompetition
                        )
                    }
                    1 -> {
                        // 2. DERECELERİM (PB & SB)
                        ProfilePersonalRecordsSection()
                    }
                    2 -> {
                        // 3. YARIŞ GEÇMİŞİM (KATILDIKLARIM)
                        ProfileRaceHistorySection(
                            completedCompetitions = completedCompetitions,
                            onSelectCompetition = onSelectCompetition
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Profil Bilgilerini Düzenleme Diyaloğu
    if (showEditDialog) {
        EditProfileDialog(
            currentName = athleteName,
            currentGender = athleteGender,
            currentBirthYear = if (athleteBirthYear > 0) athleteBirthYear.toString() else "",
            onDismiss = { showEditDialog = false },
            onSave = { name, gender, birthYear ->
                val yearInt = birthYear.toIntOrNull() ?: 1990
                viewModel.updateAthleteProfile(name, gender, yearInt)
                showEditDialog = false
            },
            viewModel = viewModel
        )
    }
}

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

@Composable
private fun ProfileTargetRacesSection(
    targetCompetitions: List<Competition>,
    onSelectCompetition: (Competition) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "HEDEF YARIŞLARIM (${targetCompetitions.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = BluePrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (targetCompetitions.isEmpty()) {
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
                        text = "Henüz Hedef Yarış Eklenmedi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ana sayfada yarışmaların detayına girip 'Hedeflerime Ekle' butonuna basarak katılacağın şampiyonaları buraya kaydedebilirsin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            targetCompetitions.forEach { comp ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectCompetition(comp) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = comp.getCountryFlag(), fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = comp.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "${comp.getFormattedDate()} • ${comp.getDisplayLocation()}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        CountdownBadge(daysRemaining = comp.getDaysRemaining())
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePersonalRecordsSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "KİŞİSEL VE SEZON DERECELERİM",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = BluePrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        val mockRecords = listOf(
            Triple("100 Metre", "11.84 sn", "12.05 sn (2026)"),
            Triple("200 Metre", "24.12 sn", "24.60 sn (2026)"),
            Triple("400 Metre", "54.30 sn", "55.10 sn (2026)"),
            Triple("5000 Metre / 5K", "17:45.20", "18:10.00 (2026)"),
            Triple("10K Yol Koşusu", "37:12.00", "38:05.00 (2026)"),
            Triple("Yarı Maraton", "1:22:40", "1:24:15 (2026)")
        )

        mockRecords.forEach { (event, pb, sb) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = event,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "PB (Kariyer)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(text = pb, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonAccent)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "SB (Sezon)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(text = sb, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRaceHistorySection(
    completedCompetitions: List<Competition>,
    onSelectCompetition: (Competition) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "YARIŞ GEÇMİŞİM & KATILIMLARIM",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = BluePrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFEAB308),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Katılım ve Derece Giriş Sistemi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Yarışma tarihi sona erdiğinde yarış kartlarında 'Katıldım ✓' seçeneği aktif olur ve koştuğun resmi dereceyi kaydederek PB/SB tablonu güncelleyebilirsin.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
