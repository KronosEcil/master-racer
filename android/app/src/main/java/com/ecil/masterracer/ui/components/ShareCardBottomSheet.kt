package com.ecil.masterracer.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ecil.masterracer.R
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.util.AthleteRecord
import com.ecil.masterracer.util.MasterAthleticsHelper
import com.ecil.masterracer.util.WmaAgeGradingHelper
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class ShareCardTheme(
    val label: String,
    val startHex: Long,
    val midHex: Long,
    val endHex: Long,
    val accentHex: Long
) {
    NAVY_ELITE(
        label = "🌌 Elit Lacivert",
        startHex = 0xFF0F172A,
        midHex = 0xFF1E3A8A,
        endHex = 0xFF1D4ED8,
        accentHex = 0xFF38BDF8
    ),
    TRACK_CRIMSON(
        label = "🔥 Kırmızı Pist",
        startHex = 0xFF450A0A,
        midHex = 0xFF991B1B,
        endHex = 0xFFDC2626,
        accentHex = 0xFFFBBF24
    ),
    CHAMPION_GOLD(
        label = "🏆 Altın Podyum",
        startHex = 0xFF18181B,
        midHex = 0xFF27272A,
        endHex = 0xFF78350F,
        accentHex = 0xFFF59E0B
    )
}

data class SingleRecordShareData(
    val athleteName: String,
    val category: String,
    val eventName: String,
    val result: String,
    val year: Int,
    val competitionTitle: String,
    val isPb: Boolean,
    val isSb: Boolean,
    val podium: String,
    val recordType: String,
    val wmaPercent: Double?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SingleRecordShareBottomSheet(
    sheetState: SheetState,
    data: SingleRecordShareData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(ShareCardTheme.NAVY_ELITE) }

    val lowerEvent = data.eventName.lowercase(Locale.forLanguageTag("tr-TR"))
    val isJump = lowerEvent.contains("atlama")
    val isThrow = lowerEvent.contains("atma")

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dijital Sporcu Yarışma Kartı",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sosyal medyada (Instagram Story, WhatsApp vb.) paylaşmaya hazır",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tema Seçici
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareCardTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTheme = theme },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(theme.midHex) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(theme.accentHex) else Color.Transparent
                        )
                    ) {
                        Text(
                            text = theme.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CANLI KART ÖNİZLEMESİ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.5.dp, Color(selectedTheme.accentHex).copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(selectedTheme.startHex),
                                    Color(selectedTheme.midHex),
                                    Color(selectedTheme.endHex)
                                )
                            )
                        )
                        .drawBehind {
                            drawIntoCanvas { composeCanvas ->
                                drawThemeAthleticWatermark(
                                    canvas = composeCanvas.nativeCanvas,
                                    width = size.width,
                                    height = size.height,
                                    theme = selectedTheme
                                )
                            }
                        }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Üst Bar: Logo + Başlık + Kategori Rozeti
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(9.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MASTER ATHLETE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "RESMİ DERECE KARTI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color(selectedTheme.accentHex)
                                )
                            }
                        }

                        Surface(
                            color = Color(selectedTheme.accentHex),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = data.category.ifBlank { "MASTER" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sporcu Adı
                    Text(
                        text = data.athleteName.ifBlank { "Master Atlet" }.uppercase(Locale.forLanguageTag("tr-TR")),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Orta Derece Kutusu
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when {
                                    isJump -> Icon(
                                        painter = painterResource(id = R.drawable.ic_event_jump),
                                        contentDescription = null,
                                        tint = Color(selectedTheme.accentHex),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    isThrow -> Icon(
                                        painter = painterResource(id = R.drawable.ic_event_throw),
                                        contentDescription = null,
                                        tint = Color(selectedTheme.accentHex),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    else -> Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                        contentDescription = null,
                                        tint = Color(selectedTheme.accentHex),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = data.eventName.uppercase(Locale.forLanguageTag("tr-TR")),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = Color(selectedTheme.accentHex)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = data.result,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Rozetler
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (data.isPb) {
                                    ShareBadgePill(text = "🔥 PB (Kişisel Rekor)", bgColor = Color(0xFFE11D48), textColor = Color.White)
                                }
                                if (data.isSb) {
                                    ShareBadgePill(text = "⚡ SB (${data.year})", bgColor = Color(0xFF16A34A), textColor = Color.White)
                                }
                                if (data.podium.isNotBlank()) {
                                    val medalLabel = when (data.podium) {
                                        "1" -> "🥇 1.LİK (ALTIN)"
                                        "2" -> "🥈 2.LİK (GÜMÜŞ)"
                                        "3" -> "🥉 3.LÜK (BRONZ)"
                                        else -> ""
                                    }
                                    if (medalLabel.isNotBlank()) {
                                        ShareBadgePill(text = medalLabel, bgColor = Color(0xFFFEF3C7), textColor = Color(0xFF92400E))
                                    }
                                }
                                if (data.recordType.isNotBlank()) {
                                    ShareBadgePill(text = "👑 ${data.recordType} REKORU", bgColor = Color(0xFF7C3AED), textColor = Color.White)
                                }
                                data.wmaPercent?.let { wma ->
                                    ShareBadgePill(
                                        text = String.format(Locale.US, "%%%.1f WMA", wma),
                                        bgColor = Color.White.copy(alpha = 0.2f),
                                        textColor = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alt Yarışma Bilgisi
                    Text(
                        text = if (data.competitionTitle.isNotBlank())
                            "🏆 ${data.year} • ${data.competitionTitle}"
                        else
                            "📅 ${data.year} Sezonu Resmi Derecesi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Paylaş Butonu
            Button(
                onClick = {
                    shareSingleRecordBitmap(context, data, selectedTheme)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kartı Görsel Olarak Paylaş",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerSummaryShareBottomSheet(
    sheetState: SheetState,
    athleteName: String,
    athleteCategory: String,
    athleteGender: String,
    records: List<AthleteRecord>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(ShareCardTheme.NAVY_ELITE) }

    // Her branştaki en iyi dereceyi (PB) çıkar
    val pbList = remember(records, athleteGender, athleteCategory) {
        records.groupBy { it.eventName }.mapNotNull { (eventName, list) ->
            val isField = MasterAthleticsHelper.isFieldEvent(eventName)
            val best = if (isField) list.maxByOrNull { it.numericValue }
            else list.filter { it.numericValue > 0 }.minByOrNull { it.numericValue } ?: list.firstOrNull()
            best?.let { rec ->
                val wma = if (rec.numericValue > 0.0) {
                    val cfg = WmaAgeGradingHelper.findEventConfig(eventName)
                    val age = rec.category.drop(1).toIntOrNull() ?: 40
                    if (cfg != null) {
                        WmaAgeGradingHelper.evaluatePerformance(athleteGender, age, cfg.key, rec.numericValue)?.scorePercentage
                    } else null
                } else null
                Triple(eventName, rec, wma)
            }
        }
    }

    val totalMedals = remember(records) { records.count { it.podium.isNotBlank() } }
    val bestWma = remember(pbList) { pbList.mapNotNull { it.third }.maxOrNull() }

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dijital Sporcu Kariyer & PB Kartı",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tüm branşlardaki kişisel en iyi (PB) derecelerin tek kartta",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tema Seçici
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareCardTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTheme = theme },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(theme.midHex) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) Color(theme.accentHex) else Color.Transparent
                        )
                    ) {
                        Text(
                            text = theme.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CANLI KARİYER KARTI ÖNİZLEMESİ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.5.dp, Color(selectedTheme.accentHex).copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(selectedTheme.startHex),
                                    Color(selectedTheme.midHex),
                                    Color(selectedTheme.endHex)
                                )
                            )
                        )
                        .drawBehind {
                            drawIntoCanvas { composeCanvas ->
                                drawThemeAthleticWatermark(
                                    canvas = composeCanvas.nativeCanvas,
                                    width = size.width,
                                    height = size.height,
                                    theme = selectedTheme
                                )
                            }
                        }
                        .padding(20.dp)
                ) {
                    // Üst Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = athleteName.ifBlank { "Master Atlet" }.uppercase(Locale.forLanguageTag("tr-TR")),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "MASTER ATHLETE • KARİYER PB KARTI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = Color(selectedTheme.accentHex)
                                )
                            }
                        }

                        Surface(
                            color = Color(selectedTheme.accentHex),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = athleteCategory,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Özet İstatistik Barı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CareerStatMiniBox(
                            label = "Aktif Branş",
                            value = "${pbList.size} Branş",
                            modifier = Modifier.weight(1f)
                        )
                        CareerStatMiniBox(
                            label = "Kazanılan Kürsü",
                            value = "$totalMedals Madalya",
                            modifier = Modifier.weight(1f)
                        )
                        CareerStatMiniBox(
                            label = "En İyi WMA",
                            value = bestWma?.let { String.format(Locale.US, "%%%.1f", it) } ?: "-",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // PB Listesi
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "KİŞİSEL EN İYİ DERECELER (PB)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = Color(selectedTheme.accentHex)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (pbList.isEmpty()) {
                                Text(
                                    text = "Henüz kayıtlı derece bulunmuyor.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            } else {
                                pbList.take(6).forEachIndexed { idx, (evName, rec, wmaVal) ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            color = Color.White.copy(alpha = 0.12f),
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = evName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${rec.category.ifBlank { athleteCategory }})",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            wmaVal?.let {
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.16f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "%%%.1f", it),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(selectedTheme.accentHex),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = rec.result,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    shareCareerSummaryBitmap(
                        context = context,
                        athleteName = athleteName,
                        athleteCategory = athleteCategory,
                        pbList = pbList,
                        totalMedals = totalMedals,
                        bestWma = bestWma,
                        theme = selectedTheme
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kariyer Kartını Görsel Olarak Paylaş",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ShareBadgePill(
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CareerStatMiniBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------------------------
// YÜKSEK ÇÖZÜNÜRLÜKLÜ (1080p) PNG RENDER & PAYLAŞIM MOTORU
// -------------------------------------------------------------
private fun shareSingleRecordBitmap(
    context: Context,
    data: SingleRecordShareData,
    theme: ShareCardTheme
) {
    val width = 1080
    val height = 1280
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // 1. Arka Plan Gradient
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(theme.startHex.toInt(), theme.midHex.toInt(), theme.endHex.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // 1.B Temaya Özel Transparan Pist / Stadyum Arka Plan Motifi
    drawThemeAthleticWatermark(canvas, width.toFloat(), height.toFloat(), theme)

    // Dekoratif Çerçeve
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = theme.accentHex.toInt()
        alpha = 110
    }
    canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 48f, 48f, borderPaint)

    // 2. Logo Çizimi
    val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
    if (logoBmp != null) {
        val dstRect = Rect(84, 84, 194, 194)
        canvas.drawBitmap(logoBmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 38f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("MASTER ATHLETE", 220f, 132f, titlePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentHex.toInt()
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("RESMİ DERECE & YARIŞMA KARTI", 220f, 172f, subPaint)

    // Kategori Rozeti (Sağ Üst)
    val catBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentHex.toInt()
    }
    val catRect = RectF(width - 230f, 94f, width - 84f, 174f)
    canvas.drawRoundRect(catRect, 22f, 22f, catBadgePaint)

    val catTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt()
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(data.category.ifBlank { "M35" }, catRect.centerX(), catRect.centerY() + 14f, catTextPaint)

    // 3. Sporcu Adı
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 64f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(
        data.athleteName.ifBlank { "Master Atlet" }.uppercase(Locale.forLanguageTag("tr-TR")),
        width / 2f,
        310f,
        namePaint
    )

    // 4. Orta Derece Kutusu
    val centerBoxRect = RectF(84f, 380f, width - 84f, 960f)
    val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 28
    }
    canvas.drawRoundRect(centerBoxRect, 44f, 44f, boxFillPaint)

    val boxStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = android.graphics.Color.WHITE
        alpha = 65
    }
    canvas.drawRoundRect(centerBoxRect, 44f, 44f, boxStrokePaint)

    // Branş Adı
    val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentHex.toInt()
        textSize = 46f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(
        data.eventName.uppercase(Locale.forLanguageTag("tr-TR")),
        width / 2f,
        490f,
        eventPaint
    )

    // Büyük Derece
    val resultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 132f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(data.result, width / 2f, 670f, resultPaint)

    // Rozet Metinleri
    val badges = mutableListOf<String>()
    if (data.isPb) badges.add("PB (Kişisel Rekor)")
    if (data.isSb) badges.add("SB (${data.year})")
    when (data.podium) {
        "1" -> badges.add("1.LİK (ALTIN)")
        "2" -> badges.add("2.LİK (GÜMÜŞ)")
        "3" -> badges.add("3.LÜK (BRONZ)")
    }
    if (data.recordType.isNotBlank()) badges.add("${data.recordType} REKORU")
    data.wmaPercent?.let { badges.add(String.format(Locale.US, "%%%.1f WMA", it)) }

    if (badges.isNotEmpty()) {
        val badgeStr = badges.joinToString("   •   ")
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accentHex.toInt()
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(badgeStr, width / 2f, 830f, badgePaint)
    }

    // 5. Alt Yarışma Bilgisi
    val compText = if (data.competitionTitle.isNotBlank()) {
        "${data.year} • ${data.competitionTitle}"
    } else {
        "${data.year} Sezonu Resmi Derecesi"
    }
    val compPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 230
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val truncatedComp = if (compText.length > 46) compText.take(44) + "..." else compText
    canvas.drawText(truncatedComp, width / 2f, 1080f, compPaint)

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 150
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Master Athlete • Masterlar Atletizm Takvimi & Rekor Arşivi", width / 2f, 1175f, footerPaint)

    shareBitmapViaIntent(context, bmp, "master_athlete_derece.png", "${data.eventName} - ${data.result}")
}

private fun shareCareerSummaryBitmap(
    context: Context,
    athleteName: String,
    athleteCategory: String,
    pbList: List<Triple<String, AthleteRecord, Double?>>,
    totalMedals: Int,
    bestWma: Double?,
    theme: ShareCardTheme
) {
    val width = 1080
    val height = 1350
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(theme.startHex.toInt(), theme.midHex.toInt(), theme.endHex.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Temaya Özel Transparan Pist / Stadyum Arka Plan Motifi
    drawThemeAthleticWatermark(canvas, width.toFloat(), height.toFloat(), theme)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = theme.accentHex.toInt()
        alpha = 110
    }
    canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 48f, 48f, borderPaint)

    val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
    if (logoBmp != null) {
        val dstRect = Rect(84, 84, 194, 194)
        canvas.drawBitmap(logoBmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(
        athleteName.ifBlank { "Master Atlet" }.uppercase(Locale.forLanguageTag("tr-TR")),
        220f,
        134f,
        titlePaint
    )

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentHex.toInt()
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("MASTER ATHLETE • KARİYER & PB KARTI", 220f, 174f, subPaint)

    val catBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentHex.toInt()
    }
    val catRect = RectF(width - 230f, 94f, width - 84f, 174f)
    canvas.drawRoundRect(catRect, 22f, 22f, catBadgePaint)

    val catTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt()
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(athleteCategory.ifBlank { "M35" }, catRect.centerX(), catRect.centerY() + 14f, catTextPaint)

    // İstatistik Satırı
    val statSummary = "Aktif Branş: ${pbList.size}   •   Madalya: $totalMedals   •   En İyi WMA: ${bestWma?.let { String.format(Locale.US, "%%%.1f", it) } ?: "-"}"
    val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(statSummary, width / 2f, 275f, statPaint)

    // PB Listesi Kutusu
    val boxRect = RectF(84f, 330f, width - 84f, 1140f)
    val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 28
    }
    canvas.drawRoundRect(boxRect, 36f, 36f, boxFillPaint)

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentHex.toInt()
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("KİŞİSEL EN İYİ DERECELER (PB)", 130f, 400f, headerPaint)

    val rowEventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val rowResPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentHex.toInt()
        textSize = 40f
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    var currentY = 490f
    pbList.take(6).forEach { (evName, rec, wmaVal) ->
        val leftLabel = "$evName (${rec.category.ifBlank { athleteCategory }})"
        val rightLabel = if (wmaVal != null) {
            "${rec.result}  •  ${String.format(Locale.US, "%%%.1f WMA", wmaVal)}"
        } else {
            rec.result
        }
        canvas.drawText(leftLabel, 130f, currentY, rowEventPaint)
        canvas.drawText(rightLabel, width - 130f, currentY, rowResPaint)
        currentY += 105f
    }

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 160
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Master Athlete • Masterlar Atletizm Takvimi & Rekor Arşivi", width / 2f, 1245f, footerPaint)

    shareBitmapViaIntent(context, bmp, "master_athlete_kariyer.png", "$athleteName - Master Athlete Kariyer Kartı")
}

private fun shareBitmapViaIntent(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    shareTitle: String
) {
    try {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Sporcu Kartını Paylaş"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// -------------------------------------------------------------
// TEMAYA ÖZEL TRANSPARAN PİST / STADYUM / PODYUM ARKA PLAN ÇİZİMİ
// -------------------------------------------------------------
private fun drawThemeAthleticWatermark(
    canvas: Canvas,
    width: Float,
    height: Float,
    theme: ShareCardTheme
) {
    if (width <= 0f || height <= 0f) return
    val saveCount = canvas.save()
    canvas.clipRect(0f, 0f, width, height)

    when (theme) {
        ShareCardTheme.NAVY_ELITE -> drawNavyStadiumCurveTrack(canvas, width, height, theme)
        ShareCardTheme.TRACK_CRIMSON -> drawCrimsonPerspectiveStraightTrack(canvas, width, height, theme)
        ShareCardTheme.CHAMPION_GOLD -> drawGoldChampionshipPodiumAndLaurel(canvas, width, height, theme)
    }

    canvas.restoreToCount(saveCount)
}

/**
 * 1. ELİT LACİVERT (NAVY_ELITE):
 * Olimpik 8 kulvarlı virajlı stadyum pisti, kademeli çıkış çizgileri, kulvar numaraları (1..8)
 * ve sol üstten vuran stadyum projektör ışığı hüzmeleri.
 */
private fun drawNavyStadiumCurveTrack(
    canvas: Canvas,
    width: Float,
    height: Float,
    theme: ShareCardTheme
) {
    val scale = width / 1080f

    // 1. Stadyum Projektör Işık Hüzmesi (Sol Üst Köşe)
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            width * 0.12f,
            height * 0.08f,
            width * 0.75f,
            intArrayOf(theme.accentHex.toInt() and 0x00FFFFFF or (42 shl 24), 0x00000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width, height, glowPaint)

    // 2. 8 Kulvarlı Virajlı Olimpik Pist Yayları (Sağ alt merkezli dev eşmerkezli yaylar)
    val centerX = width * 1.18f
    val centerY = height * 1.06f
    val baseRadius = width * 0.42f
    val laneWidth = width * 0.088f

    val laneLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.2f * scale
        color = android.graphics.Color.WHITE
        alpha = 34
    }

    val laneAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * scale
        color = theme.accentHex.toInt()
        alpha = 38
        pathEffect = DashPathEffect(floatArrayOf(18f * scale, 14f * scale), 0f)
    }

    val laneNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 32
        textSize = 46f * scale
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
    }

    val staggerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f * scale
        color = theme.accentHex.toInt()
        alpha = 46
    }

    for (i in 0..8) {
        val r = baseRadius + i * laneWidth
        val oval = RectF(centerX - r, centerY - r, centerX + r, centerY + r)
        canvas.drawArc(oval, 175f, 105f, false, laneLinePaint)

        if (i < 8) {
            val midR = r + laneWidth * 0.5f
            val midOval = RectF(centerX - midR, centerY - midR, centerX + midR, centerY + midR)
            canvas.drawArc(midOval, 182f, 92f, false, laneAccentPaint)

            // Kulvar Numarası (1..8) ve Kademeli Çıkış Çizgisi (Staggered Start)
            val staggerAngleDeg = 202f + i * 5.2f
            val rad = Math.toRadians(staggerAngleDeg.toDouble())
            val numX = (centerX + midR * kotlin.math.cos(rad)).toFloat()
            val numY = (centerY + midR * kotlin.math.sin(rad)).toFloat()

            canvas.save()
            canvas.rotate(staggerAngleDeg - 90f, numX, numY)
            canvas.drawText("${i + 1}", numX, numY, laneNumberPaint)
            canvas.restore()

            // Kademeli çıkış takozu çizgisi
            val innerX = (centerX + (r + 8f * scale) * kotlin.math.cos(rad)).toFloat()
            val innerY = (centerY + (r + 8f * scale) * kotlin.math.sin(rad)).toFloat()
            val outerX = (centerX + (r + laneWidth - 8f * scale) * kotlin.math.cos(rad)).toFloat()
            val outerY = (centerY + (r + laneWidth - 8f * scale) * kotlin.math.sin(rad)).toFloat()
            canvas.drawLine(innerX, innerY, outerX, outerY, staggerPaint)
        }
    }

    // 3. Sol Üst Perspektif Stadyum Işık Çizgileri
    val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * scale
        color = android.graphics.Color.WHITE
        alpha = 18
    }
    for (k in 0..5) {
        canvas.drawLine(
            0f,
            height * (0.05f + k * 0.04f),
            width * (0.55f + k * 0.08f),
            0f,
            rayPaint
        )
    }
}

/**
 * 2. KIRMIZI PİST (TRACK_CRIMSON):
 * 3D Perspektif 100m Tartan Sprint Düzlüğü, Damalı Foto-Finiş Çizgisi,
 * Kulvar Numaraları ve Salise (0.01s) Foto-Finiş Cetveli.
 */
private fun drawCrimsonPerspectiveStraightTrack(
    canvas: Canvas,
    width: Float,
    height: Float,
    theme: ShareCardTheme
) {
    val scale = width / 1080f
    val vanishX = width * 0.5f
    val vanishY = height * 0.14f
    val trackBottomY = height * 1.02f

    val lanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.2f * scale
        color = android.graphics.Color.WHITE
        alpha = 36
    }

    val dashedLanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * scale
        color = theme.accentHex.toInt()
        alpha = 35
        pathEffect = DashPathEffect(floatArrayOf(22f * scale, 16f * scale), 0f)
    }

    // 1. 8 Kulvarlı 3D Perspektif Sprint Çizgileri
    val laneCount = 8
    val bottomSpan = width * 1.65f
    val bottomStartX = (width - bottomSpan) / 2f
    val bottomStep = bottomSpan / laneCount

    for (i in 0..laneCount) {
        val bx = bottomStartX + i * bottomStep
        val topOffset = (i - laneCount / 2f) * (width * 0.022f)
        canvas.drawLine(vanishX + topOffset, vanishY + height * 0.08f, bx, trackBottomY, lanePaint)

        if (i < laneCount) {
            val midBx = bx + bottomStep * 0.5f
            val midTopOffset = (i + 0.5f - laneCount / 2f) * (width * 0.022f)
            canvas.drawLine(
                vanishX + midTopOffset,
                vanishY + height * 0.12f,
                midBx,
                trackBottomY,
                dashedLanePaint
            )
        }
    }

    // 2. Yatay Mesafe & Hız Perspektif Çizgileri
    val horizPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * scale
        color = android.graphics.Color.WHITE
        alpha = 22
    }
    val distanceFractions = listOf(0.32f, 0.44f, 0.58f, 0.74f, 0.91f)
    distanceFractions.forEach { frac ->
        val y = vanishY + (trackBottomY - vanishY) * frac
        canvas.drawLine(width * 0.04f, y, width * 0.96f, y, horizPaint)
    }

    // 3. Damalı Foto-Finiş (FINISH) Şeridi (Y = %78 hizasında)
    val finishY = height * 0.78f
    val checkerSize = 24f * scale
    val checkerWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 34
    }
    val checkerGold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.accentHex.toInt()
        alpha = 32
    }
    val cols = (width / checkerSize).toInt()
    for (c in 2 until (cols - 2)) {
        val x = c * checkerSize
        canvas.drawRect(x, finishY, x + checkerSize, finishY + checkerSize, if (c % 2 == 0) checkerWhite else checkerGold)
        canvas.drawRect(x, finishY + checkerSize, x + checkerSize, finishY + checkerSize * 2f, if (c % 2 == 1) checkerWhite else checkerGold)
    }

    // 4. Sağ ve Sol Kenarda Foto-Finiş Salise (0.01s) Kamera Cetveli
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * scale
        color = theme.accentHex.toInt()
        alpha = 45
    }
    var tickIndex = 0
    var ty = height * 0.18f
    while (ty < height * 0.88f) {
        val tickLen = if (tickIndex % 5 == 0) 26f * scale else 13f * scale
        canvas.drawLine(42f * scale, ty, 42f * scale + tickLen, ty, tickPaint)
        canvas.drawLine(width - 42f * scale - tickLen, ty, width - 42f * scale, ty, tickPaint)
        ty += 18f * scale
        tickIndex++
    }
}

/**
 * 3. ALTIN PODYUM (CHAMPION_GOLD):
 * Şampiyonluk Defne Çelengi (Laurel Wreath), 2 - 1 - 3 Kürsü Silüeti,
 * Zafer Halkaları ve Merkezden Yayılan Altın Işık Hüzmeleri.
 */
private fun drawGoldChampionshipPodiumAndLaurel(
    canvas: Canvas,
    width: Float,
    height: Float,
    theme: ShareCardTheme
) {
    val scale = width / 1080f
    val cx = width * 0.5f
    val cy = height * 0.48f

    // 1. Merkezden Yayılan Zafer Işınları (Sunburst Rays)
    val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * scale
        color = theme.accentHex.toInt()
        alpha = 24
    }
    for (deg in 0 until 360 step 15) {
        val rad = Math.toRadians(deg.toDouble())
        val innerR = width * 0.16f
        val outerR = width * 0.62f
        canvas.drawLine(
            cx + (innerR * kotlin.math.cos(rad)).toFloat(),
            cy + (innerR * kotlin.math.sin(rad)).toFloat(),
            cx + (outerR * kotlin.math.cos(rad)).toFloat(),
            cy + (outerR * kotlin.math.sin(rad)).toFloat(),
            rayPaint
        )
    }

    // 2. Eşmerkezli Şampiyonluk Madalyon Halkaları
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * scale
        color = theme.accentHex.toInt()
        alpha = 38
    }
    val dashedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * scale
        color = android.graphics.Color.WHITE
        alpha = 28
        pathEffect = DashPathEffect(floatArrayOf(14f * scale, 12f * scale), 0f)
    }
    canvas.drawCircle(cx, cy, width * 0.34f, ringPaint)
    canvas.drawCircle(cx, cy, width * 0.38f, dashedRingPaint)
    canvas.drawCircle(cx, cy, width * 0.43f, ringPaint)

    // 3. Sol ve Sağ Simetrik Defne Çelengi Yaprakları (Victory Laurel Wreath)
    val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.accentHex.toInt()
        alpha = 36
    }
    val wreathRadius = width * 0.405f
    for (step in -5..5) {
        // Sağ dal
        val rightDeg = step * 11f
        val rightRad = Math.toRadians(rightDeg.toDouble())
        val rx = (cx + wreathRadius * kotlin.math.cos(rightRad)).toFloat()
        val ry = (cy + wreathRadius * kotlin.math.sin(rightRad)).toFloat()
        canvas.save()
        canvas.translate(rx, ry)
        canvas.rotate(rightDeg + 90f)
        canvas.drawOval(RectF(-12f * scale, -28f * scale, 12f * scale, 28f * scale), leafPaint)
        canvas.restore()

        // Sol dal
        val leftDeg = 180f - step * 11f
        val leftRad = Math.toRadians(leftDeg.toDouble())
        val lx = (cx + wreathRadius * kotlin.math.cos(leftRad)).toFloat()
        val ly = (cy + wreathRadius * kotlin.math.sin(leftRad)).toFloat()
        canvas.save()
        canvas.translate(lx, ly)
        canvas.rotate(leftDeg - 90f)
        canvas.drawOval(RectF(-12f * scale, -28f * scale, 12f * scale, 28f * scale), leafPaint)
        canvas.restore()
    }

    // 4. Alt Kısımda 2 - 1 - 3 Şampiyonluk Kürsüsü (Podium) Silüeti
    val podiumFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.accentHex.toInt()
        alpha = 24
    }
    val podiumStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * scale
        color = theme.accentHex.toInt()
        alpha = 46
    }
    val podiumNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 38
        textSize = 68f * scale
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val baseLineY = height * 0.94f
    val stepW = width * 0.22f

    // 2.lik Basamağı (Sol)
    val rect2 = RectF(cx - stepW * 1.5f, baseLineY - 120f * scale, cx - stepW * 0.5f, baseLineY)
    canvas.drawRoundRect(rect2, 12f * scale, 12f * scale, podiumFillPaint)
    canvas.drawRoundRect(rect2, 12f * scale, 12f * scale, podiumStrokePaint)
    canvas.drawText("2", rect2.centerX(), rect2.centerY() + 24f * scale, podiumNumPaint)

    // 1.lik Basamağı (Orta - En Yüksek)
    val rect1 = RectF(cx - stepW * 0.5f, baseLineY - 175f * scale, cx + stepW * 0.5f, baseLineY)
    canvas.drawRoundRect(rect1, 14f * scale, 14f * scale, podiumFillPaint)
    canvas.drawRoundRect(rect1, 14f * scale, 14f * scale, podiumStrokePaint)
    canvas.drawText("1", rect1.centerX(), rect1.centerY() + 24f * scale, podiumNumPaint)

    // 3.lük Basamağı (Sağ)
    val rect3 = RectF(cx + stepW * 0.5f, baseLineY - 95f * scale, cx + stepW * 1.5f, baseLineY)
    canvas.drawRoundRect(rect3, 12f * scale, 12f * scale, podiumFillPaint)
    canvas.drawRoundRect(rect3, 12f * scale, 12f * scale, podiumStrokePaint)
    canvas.drawText("3", rect3.centerX(), rect3.centerY() + 24f * scale, podiumNumPaint)
}
