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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.data.repository.BenchmarkRecord
import com.ecil.masterracer.data.repository.MasterRecordsArchive
import com.ecil.masterracer.data.repository.RecordLevel
import com.ecil.masterracer.data.repository.TrackType
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent
import com.ecil.masterracer.util.AthleteRecord
import java.util.Calendar

private val MALE_CATEGORIES = listOf(
    "M35", "M40", "M45", "M50", "M55", "M60", "M65", "M70", "M75", "M80", "M85", "M90", "M95", "M100", "M105"
)

private val FEMALE_CATEGORIES = listOf(
    "W35", "W40", "W45", "W50", "W55", "W60", "W65", "W70", "W75", "W80", "W85", "W90", "W95", "W100"
)

private val MIXED_CATEGORIES = listOf(
    "X35", "X40", "X45", "X50", "X55", "X60", "X65", "X70", "X75"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterRecordsScreen(
    athleteGender: String,
    athleteCategory: String,
    athleteBirthYear: Int = 1990,
    athleteRecords: List<AthleteRecord>,
    onBack: () -> Unit
) {
    var selectedTrackTabIndex by remember { mutableIntStateOf(0) } // 0: Açık Saha, 1: Salon
    val trackType = if (selectedTrackTabIndex == 0) TrackType.OUTDOOR else TrackType.INDOOR

    // Cinsiyet seçimi ("M", "W", "X")
    var selectedGenderPrefix by remember(athleteGender, athleteCategory) {
        val initialPrefix = if (athleteCategory.startsWith("W", ignoreCase = true) || athleteGender.contains("Kadın", ignoreCase = true) || athleteGender.equals("F", ignoreCase = true)) "W" else "M"
        mutableStateOf(initialPrefix)
    }

    val currentCategoryList = remember(selectedGenderPrefix) {
        when (selectedGenderPrefix) {
            "W" -> FEMALE_CATEGORIES
            "X" -> MIXED_CATEGORIES
            else -> MALE_CATEGORIES
        }
    }

    // Seçilen yaş kategorisi
    var selectedCategory by remember(athleteCategory, currentCategoryList) {
        val initial = if (currentCategoryList.contains(athleteCategory.uppercase())) athleteCategory.uppercase() else currentCategoryList.first()
        mutableStateOf(initial)
    }

    // Cinsiyet grubu değiştiğinde geçerli kategoriyi güncelle
    LaunchedEffect(currentCategoryList) {
        if (!currentCategoryList.contains(selectedCategory)) {
            selectedCategory = currentCategoryList.first()
        }
    }

    // Açılır Menü State'leri
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var eventDropdownExpanded by remember { mutableStateOf(false) }

    // Seçilen branş
    var selectedEvent by remember { mutableStateOf("100 Metre") }

    // Seçilen yaş kategorisine, cinsiyete ve piste göre geçerli branşlar
    val availableEvents = remember(trackType, selectedCategory) {
        MasterRecordsArchive.getAvailableEvents(trackType, selectedCategory).ifEmpty {
            MasterRecordsArchive.getAvailableEvents(trackType)
        }
    }

    // Yaş kategorisi, cinsiyet veya pist değiştiğinde seçili branş bu kategoride yoksa en uygun branşa geç
    LaunchedEffect(trackType, selectedCategory, availableEvents) {
        if (selectedEvent !in availableEvents && availableEvents.isNotEmpty()) {
            val replacement: String = (if (selectedEvent.contains("Engelli", ignoreCase = true)) {
                // Engelli branşı seçiliydi; bu kategorideki geçerli engelliyi bul (ör. Kadınlar için 100m/80m Engelli, M70 için 80m/300m Engelli)
                availableEvents.firstOrNull { it.contains("Engelli", ignoreCase = true) }
            } else if (selectedEvent.contains("Bayrak", ignoreCase = true)) {
                availableEvents.firstOrNull { it.contains("Bayrak", ignoreCase = true) }
            } else if (selectedEvent.contains("Yürüyüş", ignoreCase = true)) {
                availableEvents.firstOrNull { it.contains("Yürüyüş", ignoreCase = true) }
            } else {
                null
            }) ?: availableEvents.firstOrNull { it == "100 Metre" || it == "60 Metre" }
              ?: availableEvents.firstOrNull()
              ?: "100 Metre"
            selectedEvent = replacement
        }
    }

    // Filtreleme (Kullanıcı talebi: Ayrı seviye filtrelemesine ihtiyaç yok, tüm seviyeler birlikte gösterilir)
    val recordsForEvent = remember(selectedEvent, selectedCategory, trackType) {
        MasterRecordsArchive.getRecords(
            eventName = selectedEvent,
            category = selectedCategory,
            trackType = trackType,
            recordLevel = null
        ).sortedBy { record ->
            when (record.recordLevel) {
                RecordLevel.NMR -> 0
                RecordLevel.BCR -> 1
                RecordLevel.BMR -> 2
                RecordLevel.EMR -> 3
                RecordLevel.WMR -> 4
            }
        }
    }

    // Kullanıcının bu branştaki PB derecesi (varsa)
    val userPb = remember(selectedEvent, athleteRecords) {
        athleteRecords.filter {
            MasterRecordsArchive.isEventMatch(it.eventName, selectedEvent)
        }.let { list ->
            val isField = selectedEvent.contains("Atlama") || selectedEvent.contains("Atma")
            if (isField) list.maxByOrNull { it.numericValue }
            else list.filter { it.numericValue > 0 }.minByOrNull { it.numericValue } ?: list.firstOrNull()
        }
    }

    // PB'nin yapıldığı yaş kategorisini belirleme
    val userPbCategory = remember(userPb, athleteGender, athleteBirthYear) {
        userPb?.let { pb ->
            if (pb.category.isNotBlank()) {
                pb.category
            } else if (athleteBirthYear in 1910..Calendar.getInstance().get(Calendar.YEAR) && pb.year > 0) {
                val ageAtRecord = pb.year - athleteBirthYear
                val prefix = if (athleteGender.contains("Kadın", ignoreCase = true) || athleteGender.equals("F", ignoreCase = true)) "W" else "M"
                val bracket = (ageAtRecord / 5) * 5
                "$prefix$bracket"
            } else {
                athleteCategory
            }
        } ?: athleteCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Resmi Master Rekorları",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$selectedCategory • $selectedEvent • ${trackType.displayName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))

                // 1. AÇIK SAHA / SALON SEKMESİ
                TabRow(
                    selectedTabIndex = selectedTrackTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = BluePrimary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)) }
                ) {
                    Tab(
                        selected = selectedTrackTabIndex == 0,
                        onClick = { selectedTrackTabIndex = 0 },
                        text = {
                            Text(
                                text = "☀️ Açık Saha",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTrackTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTrackTabIndex == 1,
                        onClick = { selectedTrackTabIndex = 1 },
                        text = {
                            Text(
                                text = "🏟️ Salon",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTrackTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // 2. KATEGORİ VE BRANŞ SEÇİCİ (DROPDOWN MENÜLER)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Cinsiyet Segmenti (Erkek / Kadın / Karma)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val genders = listOf("M" to "👨 Erkek (M)", "W" to "👩 Kadın (W)", "X" to "🤝 Karma (X)")
                        genders.forEach { (prefix, title) ->
                            val isSelected = selectedGenderPrefix == prefix
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedGenderPrefix = prefix },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BluePrimary else Color.Transparent
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Yan Yana Açılır Menüler: Yaş Kategorisi & Branş
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Yaş Kategorisi Seçmeli Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = categoryDropdownExpanded,
                                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Yaş Kategorisi", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false }
                                ) {
                                    currentCategoryList.forEach { cat ->
                                        val isUserCat = cat.equals(athleteCategory, ignoreCase = true)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = cat,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (isUserCat) {
                                                        Text(
                                                            text = "(Kategorin)",
                                                            fontSize = 10.sp,
                                                            color = BluePrimary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedCategory = cat
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Branş Seçmeli Dropdown
                        Box(modifier = Modifier.weight(1.3f)) {
                            ExposedDropdownMenuBox(
                                expanded = eventDropdownExpanded,
                                onExpandedChange = { eventDropdownExpanded = !eventDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedEvent,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Branş", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = eventDropdownExpanded,
                                    onDismissRequest = { eventDropdownExpanded = false }
                                ) {
                                    availableEvents.forEach { ev ->
                                        DropdownMenuItem(
                                            text = { Text(text = ev, fontSize = 13.sp) },
                                            onClick = {
                                                selectedEvent = ev
                                                eventDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. KULLANICININ BU BRANŞTAKİ PB'Sİ (YAŞ KATEGORİSİ BİLGİSİ İLE BİRLİKTE)
            userPb?.let { pb ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CrimsonAccent.copy(alpha = 0.08f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = CrimsonAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SENİN EN İYİ DERECEN (PB)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonAccent,
                                        letterSpacing = 0.6.sp
                                    )
                                    Surface(
                                        color = CrimsonAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$userPbCategory Kategorisi",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CrimsonAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = pb.result,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CrimsonAccent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${pb.competitionTitle.ifBlank { "Kayıtlı Yarış" }} (${pb.year})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. REKOR LİSTESİ (TÜM SEVİYELER SIRAYLA LİSTELENİR)
            if (recordsForEvent.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$selectedCategory kategorisi $selectedEvent için arşiv kaydı bulunamadı.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recordsForEvent) { record ->
                    RecordRowCardItem(record = record)
                }
            }

            // 5. RESMİ KAYNAK BİLGİ NOTU
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ℹ️ Resmi Rekor Kaynakları:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• NMR: Türkiye Atletizm Federasyonu (TAF) Masterlar Rekorları\n" +
                                   "• BŞR: Balkan Masters Athletics (BAMA) Şampiyona Rekorları\n" +
                                   "• BMR: Balkan Masters Athletics (BAMA) Tüm Zamanlar Balkan Rekorları\n" +
                                   "• EMR: European Masters Athletics (EMA) Resmi Avrupa Rekorları\n" +
                                   "• WMR: World Masters Athletics (WMA) Dünya Rekorları",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RecordRowCardItem(record: BenchmarkRecord) {
    val (badgeBg, badgeText) = when (record.recordLevel) {
        RecordLevel.NMR -> Pair(Color(0xFFDC2626), "NMR (Türkiye Rekoru)")
        RecordLevel.BCR -> Pair(Color(0xFF2563EB), "BŞR (Balkan Şampiyona Rekoru)")
        RecordLevel.BMR -> Pair(Color(0xFF0D9488), "BMR (Balkan Rekoru)")
        RecordLevel.EMR -> Pair(Color(0xFFD97706), "EMR (Avrupa Rekoru)")
        RecordLevel.WMR -> Pair(Color(0xFF7C3AED), "WMR (Dünya Rekoru)")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (record.isShared) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = "🤝 Ortak Rekor",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val dateVenueYear = buildString {
                    if (record.year > 0) append(record.year)
                    if (record.country.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(record.country)
                    }
                }
                if (dateVenueYear.isNotBlank()) {
                    Text(
                        text = dateVenueYear,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.athleteName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val placeAndDate = buildString {
                        if (record.venue.isNotBlank()) append(record.venue)
                        if (record.date.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(record.date)
                        }
                    }
                    if (placeAndDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = placeAndDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    if (record.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = record.note,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = record.mark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = BluePrimary
                )
            }
        }
    }
}
