package com.ecil.masterracer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTargetBottomSheet(
    sheetState: SheetState,
    currentCategory: String,
    nextCategory: String,
    athleteGender: String,
    athleteBirthYear: Int,
    athleteRecords: List<AthleteRecord>,
    onDismiss: () -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val age = if (athleteBirthYear in 1910..currentYear) currentYear - athleteBirthYear else 40
    val nextBracketAge = ((age / 5) + 1) * 5
    val yearsUntilNext = nextBracketAge - age

    var selectedTrackTabIndex by remember { mutableIntStateOf(0) } // 0: Açık Saha (Outdoor), 1: Salon (Indoor)
    var selectedCategoryView by remember { mutableStateOf(nextCategory) } // Varsayılan olarak bir sonraki kategori hedefleri
    var selectedEvent by remember { mutableStateOf("100 Metre") }
    var eventDropdownExpanded by remember { mutableStateOf(false) }
    var selectedRecordLevel by remember { mutableStateOf<RecordLevel?>(null) }

    val trackType = if (selectedTrackTabIndex == 0) TrackType.OUTDOOR else TrackType.INDOOR

    val availableEvents = remember(trackType) {
        MasterRecordsArchive.getAvailableEvents(trackType).ifEmpty {
            if (trackType == TrackType.OUTDOOR) {
                listOf("100 Metre", "200 Metre", "400 Metre", "800 Metre", "1500 Metre", "5000 Metre", "10.000 Metre", "Uzun Atlama", "Gülle Atma")
            } else {
                listOf("60 Metre", "200 Metre", "400 Metre", "Uzun Atlama")
            }
        }
    }

    // Seçili branş geçerli sekmede yoksa ilkini seç
    if (selectedEvent !in availableEvents) {
        selectedEvent = availableEvents.firstOrNull() ?: "100 Metre"
    }

    val recordsForEvent = remember(selectedEvent, selectedCategoryView, trackType, selectedRecordLevel) {
        MasterRecordsArchive.getRecords(
            eventName = selectedEvent,
            category = selectedCategoryView,
            trackType = trackType,
            recordLevel = selectedRecordLevel
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

    // Kullanıcının bu branştaki PB'sini bul
    val userPb = athleteRecords.filter {
        it.eventName.contains(selectedEvent, ignoreCase = true) || selectedEvent.contains(it.eventName, ignoreCase = true)
    }.let { list ->
        val isField = selectedEvent.contains("Atlama") || selectedEvent.contains("Atma")
        if (isField) list.maxByOrNull { it.numericValue }
        else list.filter { it.numericValue > 0 }.minByOrNull { it.numericValue } ?: list.firstOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Kategori Hedef & Rekor Projeksiyonu",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bir sonraki yaş grubuna geçiş ve resmi rekorlar",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Kategori Geri Sayım Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SONRAKİ YAŞ KATEGORİSİ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = nextCategory,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$yearsUntilNext Yıl Sonra ($nextBracketAge Yaş)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Açık Saha / Salon Sekmesi (Kullanıcının kesin talimatı)
            TabRow(
                selectedTabIndex = selectedTrackTabIndex,
                containerColor = Color.Transparent,
                contentColor = BluePrimary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
            ) {
                Tab(
                    selected = selectedTrackTabIndex == 0,
                    onClick = { selectedTrackTabIndex = 0 },
                    text = {
                        Text(
                            text = "☀️ Açık Saha",
                            fontSize = 12.sp,
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
                            fontSize = 12.sp,
                            fontWeight = if (selectedTrackTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Kategori ve Branş Filtresi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kategori Seçimi (Mevcut vs Hedef)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategoryView = currentCategory },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedCategoryView == currentCategory) BluePrimary else Color.Transparent
                    ) {
                        Text(
                            text = "Mevcut ($currentCategory)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategoryView == currentCategory) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategoryView = nextCategory },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedCategoryView == nextCategory) Color(0xFF8B5CF6) else Color.Transparent
                    ) {
                        Text(
                            text = "Hedef ($nextCategory)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategoryView == nextCategory) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Branş Açılır Menüsü
                Box(modifier = Modifier.weight(1.2f)) {
                    ExposedDropdownMenuBox(
                        expanded = eventDropdownExpanded,
                        onExpandedChange = { eventDropdownExpanded = !eventDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedEvent,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
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
                                    text = { Text(text = ev, fontSize = 12.sp) },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Kullanıcının Mevcut PB Bilgisi (Varsa)
            userPb?.let { pb ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CrimsonAccent.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Senin En İyi Derecen (PB): ${pb.result}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonAccent)
                            Text(text = "${pb.competitionTitle.ifBlank { "Kayıtlı Yarış" }} (${pb.year})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Rekor Düzeyi Filtre Hapları (Tümü, NMR, BŞR, BMR)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val levels = listOf<Pair<RecordLevel?, String>>(
                    null to "Tümü",
                    RecordLevel.NMR to "🇹🇷 Türkiye",
                    RecordLevel.BCR to "🏆 Şampiyona",
                    RecordLevel.BMR to "⚡ Balkan"
                )
                levels.forEach { (lvl, title) ->
                    val isSelected = selectedRecordLevel == lvl
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedRecordLevel = lvl },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // REKOR LİSTESİ (NMR, BCR, BMR, EMR)
            if (recordsForEvent.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$selectedCategoryView kategorisi $selectedEvent için arşiv kaydı bulunamadı.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                recordsForEvent.forEach { record ->
                    RecordRowCard(record = record)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resmi Kaynak Bilgi Notu
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ℹ️ Rekor Kaynakları:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• NMR: Türkiye Atletizm Federasyonu (TAF) Masterlar Rekorları\n" +
                               "• BŞR: Balkan Masters Athletics (BAMA) Şampiyona Rekorları\n" +
                               "• BMR: Balkan Masters Athletics (BAMA) Tüm Zamanlar Balkan Rekorları\n" +
                               "• EMR: European Masters Athletics (EMA) Avrupa Rekorları\n" +
                               "• WMR: World Masters Athletics (WMA) Dünya Rekorları",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecordRowCard(record: BenchmarkRecord) {
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
                    if (record.country.isNotBlank()) append(" • ${record.country}")
                }
                Text(
                    text = dateVenueYear,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val locationDetails = listOfNotNull(
                        record.venue.takeIf { it.isNotBlank() },
                        record.date.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (locationDetails.isNotBlank()) {
                        Text(
                            text = locationDetails,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (record.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = record.note,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (record.isShared) Color(0xFF8B5CF6) else BluePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = record.mark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = BluePrimary
                )
            }
        }
    }
}
