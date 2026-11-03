package com.ecil.masterracer.ui.components

import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.R
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent
import com.ecil.masterracer.util.AthleteRecord
import com.ecil.masterracer.util.MasterAthleticsHelper
import com.ecil.masterracer.util.WmaAgeGradingHelper
import java.util.Locale
import kotlin.math.abs

private data class ChartDataPoint(
    val record: AthleteRecord,
    val numericValue: Double,
    val wmaPercent: Double?,
    val isPb: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerformanceAnalyticsSection(
    records: List<AthleteRecord>,
    athleteGender: String,
    initialSelectedEvent: String?,
    onAddNewRecord: () -> Unit
) {
    if (records.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Gelişim Grafiği İçin Derece Ekleyin",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Branşlarınıza ait yarış derecelerinizi ekledikçe yıllara göre hız/mesafe gelişim eğriniz ve WMA yaş yüzdesi analiziniz burada oluşturulur.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAddNewRecord,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "İlk Dereceni Ekle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val groupedByEvent = remember(records) {
        records.groupBy { it.eventName }
    }
    val eventNames = remember(groupedByEvent) { groupedByEvent.keys.toList() }

    var selectedEvent by remember(eventNames) {
        mutableStateOf(
            if (initialSelectedEvent != null && eventNames.contains(initialSelectedEvent)) {
                initialSelectedEvent
            } else {
                eventNames.first()
            }
        )
    }

    LaunchedEffect(initialSelectedEvent) {
        if (initialSelectedEvent != null && eventNames.contains(initialSelectedEvent)) {
            selectedEvent = initialSelectedEvent
        }
    }

    // 0 = Derece Gelişimi (sn / m), 1 = WMA Yaş Yüzdesi (%)
    var chartMode by remember { mutableIntStateOf(0) }

    // Genel Kariyer İstatistikleri
    val goldCount = remember(records) { records.count { it.podium == "1" } }
    val silverCount = remember(records) { records.count { it.podium == "2" } }
    val bronzeCount = remember(records) { records.count { it.podium == "3" } }
    val recordBadgeCount = remember(records) { records.count { it.recordType.isNotBlank() } }

    val allWmaScores = remember(records, athleteGender) {
        records.mapNotNull { rec ->
            if (rec.numericValue > 0.0) {
                val cfg = WmaAgeGradingHelper.findEventConfig(rec.eventName)
                val age = rec.category.drop(1).toIntOrNull() ?: 40
                if (cfg != null) {
                    WmaAgeGradingHelper.evaluatePerformance(athleteGender, age, cfg.key, rec.numericValue)?.scorePercentage
                } else null
            } else null
        }
    }
    val bestWmaOverall = remember(allWmaScores) { allWmaScores.maxOrNull() }
    val avgWmaOverall = remember(allWmaScores) {
        if (allWmaScores.isNotEmpty()) allWmaScores.average() else null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. KARİYER & MADALYA ANALİZ ÖZETİ KARTI
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KARİYER & SEZON ANALİTİĞİ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = BluePrimary
                    )
                    Text(
                        text = "${records.size} Kayıt • ${eventNames.size} Branş",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsMiniStatCard(
                        title = "Madalya Dağılımı",
                        value = "🥇$goldCount  🥈$silverCount  🥉$bronzeCount",
                        subtitle = if (recordBadgeCount > 0) "👑 $recordBadgeCount Resmi Rekor" else "Toplam ${goldCount + silverCount + bronzeCount} Kürsü",
                        modifier = Modifier.weight(1.15f)
                    )
                    AnalyticsMiniStatCard(
                        title = "En İyi WMA %",
                        value = bestWmaOverall?.let { String.format(Locale.US, "%%%.1f", it) } ?: "-",
                        subtitle = avgWmaOverall?.let { String.format(Locale.US, "Ort: %%%.1f WMA", it) } ?: "WMA Klasmanı",
                        modifier = Modifier.weight(0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. BRANŞ SEÇİCİ YATAY ÇİPLER
        Text(
            text = "BRANŞ SEÇİMİ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            eventNames.forEach { evName ->
                val isSelected = evName == selectedEvent
                val count = groupedByEvent[evName]?.size ?: 0
                val lowerEv = evName.lowercase(Locale.forLanguageTag("tr-TR"))
                val isJump = lowerEv.contains("atlama")
                val isThrow = lowerEv.contains("atma")

                Surface(
                    modifier = Modifier.clickable { selectedEvent = evName },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) BluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconTint = if (isSelected) Color.White else BluePrimary
                        when {
                            isJump -> Icon(
                                painter = painterResource(id = R.drawable.ic_event_jump),
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            isThrow -> Icon(
                                painter = painterResource(id = R.drawable.ic_event_throw),
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$evName ($count)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Seçili Branş Verilerini Hazırla (Kronolojik: Eskiden Yeniye)
        val isField = MasterAthleticsHelper.isFieldEvent(selectedEvent)
        val rawEventRecords = groupedByEvent[selectedEvent].orEmpty()
            .filter { it.numericValue > 0.0 }
            .sortedWith(compareBy<AthleteRecord> { it.year })

        val bestRecordId = remember(rawEventRecords, isField) {
            if (isField) {
                rawEventRecords.maxByOrNull { it.numericValue }?.id
            } else {
                rawEventRecords.minByOrNull { it.numericValue }?.id
            }
        }

        val chartPoints = remember(rawEventRecords, selectedEvent, athleteGender, bestRecordId) {
            val cfg = WmaAgeGradingHelper.findEventConfig(selectedEvent)
            rawEventRecords.map { rec ->
                val age = rec.category.drop(1).toIntOrNull() ?: 40
                val wmaVal = if (cfg != null) {
                    WmaAgeGradingHelper.evaluatePerformance(athleteGender, age, cfg.key, rec.numericValue)?.scorePercentage
                } else null
                ChartDataPoint(
                    record = rec,
                    numericValue = rec.numericValue,
                    wmaPercent = wmaVal,
                    isPb = rec.id == bestRecordId
                )
            }
        }

        val hasWmaData = remember(chartPoints) { chartPoints.any { it.wmaPercent != null } }
        LaunchedEffect(selectedEvent, hasWmaData) {
            if (!hasWmaData && chartMode == 1) {
                chartMode = 0
            }
        }

        // 3. PERFORMANS İLERLEME GRAFİĞİ KARTI
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Grafik Başlığı & Mod Seçici (Derece vs WMA %)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$selectedEvent Gelişim Eğrisi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val directionHint = when {
                            chartMode == 1 -> "↑ Daha Yüksek WMA % (Yaş Faktörlü Klasman)"
                            isField -> "↑ Daha Uzak / Yüksek Mesafe (Daha İyi)"
                            else -> "↑ Daha Kısa Süre / Hızlı Derece (Daha İyi)"
                        }
                        Text(
                            text = directionHint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BluePrimary
                        )
                    }
                }

                if (hasWmaData) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf("⏱️ Derece Gelişimi", "📊 WMA Yaş Yüzdesi (%)")
                        modes.forEachIndexed { idx, label ->
                            val selected = chartMode == idx
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { chartMode = idx },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) BluePrimary else Color.Transparent
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ÇİZGİ & ALAN GRAFİĞİ CANVAS
                PerformanceLineChartCanvas(
                    points = chartPoints,
                    isFieldEvent = isField,
                    showWmaMode = chartMode == 1 && hasWmaData
                )

                if (chartPoints.size == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = BluePrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "💡 Bu branşta şu an 1 kayıt bulunuyor. Farklı yarış veya yıllara ait yeni dereceler eklediğinizde yıllar arası gelişim çizginiz otomatik bağlanacaktır.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. KRONOLOJİK GELİŞİM & FARK (DELTA) TABLOSU
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "KRONOLOJİK GELİŞİM & FARK ANALİZİ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = BluePrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                chartPoints.forEachIndexed { idx, pt ->
                    if (idx > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    val prevPt = if (idx > 0) chartPoints[idx - 1] else null
                    val rawDiff = if (prevPt != null) pt.numericValue - prevPt.numericValue else null
                    val isImproved = if (rawDiff != null) {
                        if (isField) rawDiff > 0.0 else rawDiff < 0.0
                    } else null

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = BluePrimary,
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = "${pt.record.year} • ${pt.record.category.ifBlank { "MASTER" }}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (pt.isPb) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = CrimsonAccent,
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = "PB",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pt.record.competitionTitle.ifBlank { "${pt.record.year} Sezon Kaydı" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = pt.record.result,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (rawDiff != null && abs(rawDiff) > 0.001) {
                                val unit = if (isField) "m" else "sn"
                                val sign = if (rawDiff > 0) "+" else "-"
                                val diffFormatted = String.format(Locale.US, "%s%.2f %s", sign, abs(rawDiff), unit)
                                val badgeColor = if (isImproved == true) Color(0xFF16A34A) else Color(0xFFD97706)
                                Text(
                                    text = if (isImproved == true) "▲ $diffFormatted (Gelişim)" else "▼ $diffFormatted",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            } else if (pt.wmaPercent != null) {
                                Text(
                                    text = String.format(Locale.US, "%%%.1f WMA", pt.wmaPercent),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceLineChartCanvas(
    points: List<ChartDataPoint>,
    isFieldEvent: Boolean,
    showWmaMode: Boolean
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = BluePrimary
    val pbAccentColor = CrimsonAccent

    val activePoints = remember(points, showWmaMode) {
        if (showWmaMode) points.filter { it.wmaPercent != null } else points
    }

    if (activePoints.isEmpty()) return

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val w = size.width
            val h = size.height

            val padLeft = 36f
            val padRight = 36f
            val padTop = 48f
            val padBottom = 54f
            val chartW = (w - padLeft - padRight).coerceAtLeast(10f)
            val chartH = (h - padTop - padBottom).coerceAtLeast(10f)

            val values = activePoints.map { pt ->
                if (showWmaMode) (pt.wmaPercent ?: 0.0) else pt.numericValue
            }

            val minVal = values.minOrNull() ?: 0.0
            val maxVal = values.maxOrNull() ?: 1.0
            val span = (maxVal - minVal).let { if (it < 0.001) 1.0 else it }

            // Y koordinatı hesaplama:
            // Koşu branşlarında (isFieldEvent == false ve !showWmaMode) düşük saniye daha iyi olduğu için yukarıda gösterilir.
            fun valueToY(v: Double): Float {
                if (activePoints.size == 1 || maxVal - minVal < 0.0001) {
                    return padTop + chartH * 0.5f
                }
                val norm = ((v - minVal) / span).toFloat().coerceIn(0f, 1f)
                val higherIsBetter = showWmaMode || isFieldEvent
                return if (higherIsBetter) {
                    padTop + chartH * (1f - norm)
                } else {
                    padTop + chartH * norm
                }
            }

            fun indexToX(idx: Int): Float {
                return if (activePoints.size == 1) {
                    padLeft + chartW * 0.5f
                } else {
                    padLeft + (idx.toFloat() / (activePoints.size - 1).toFloat()) * chartW
                }
            }

            // 1. Yatay Kılavuz Çizgileri
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = gridColor.toArgb()
                alpha = 90
                pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            }
            for (g in 0..3) {
                val gy = padTop + (chartH / 3f) * g
                canvas.drawLine(padLeft, gy, w - padRight, gy, gridPaint)
            }

            val coords = activePoints.mapIndexed { idx, pt ->
                val v = if (showWmaMode) (pt.wmaPercent ?: 0.0) else pt.numericValue
                Pair(indexToX(idx), valueToY(v))
            }

            // 2. Eğri Altı Gradient Dolgu & Bağlantı Çizgisi
            if (coords.size >= 2) {
                val areaPath = Path().apply {
                    moveTo(coords.first().first, padTop + chartH)
                    coords.forEach { (cx, cy) -> lineTo(cx, cy) }
                    lineTo(coords.last().first, padTop + chartH)
                    close()
                }
                val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        0f,
                        padTop,
                        0f,
                        padTop + chartH,
                        intArrayOf(
                            primaryColor.copy(alpha = 0.32f).toArgb(),
                            primaryColor.copy(alpha = 0.02f).toArgb()
                        ),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawPath(areaPath, areaPaint)

                val linePath = Path().apply {
                    coords.forEachIndexed { idx, (cx, cy) ->
                        if (idx == 0) moveTo(cx, cy) else lineTo(cx, cy)
                    }
                }
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 7f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    color = primaryColor.toArgb()
                }
                canvas.drawPath(linePath, linePaint)
            } else if (coords.size == 1) {
                // Tek nokta varsa yatay vurgu çizgisi
                val (cx, cy) = coords.first()
                val singleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    color = primaryColor.copy(alpha = 0.4f).toArgb()
                }
                canvas.drawLine(padLeft, cy, w - padRight, cy, singleLinePaint)
            }

            // 3. Noktalar, Değer Etiketleri ve Yıl/Kategori Etiketleri
            val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = onSurfaceColor.toArgb()
                textSize = 26f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val yearTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = onSurfaceColor.copy(alpha = 0.7f).toArgb()
                textSize = 23f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val pbTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pbAccentColor.toArgb()
            }
            val pbTagTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 20f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            coords.forEachIndexed { idx, (cx, cy) ->
                val pt = activePoints[idx]
                val dotColor = if (pt.isPb) pbAccentColor.toArgb() else primaryColor.toArgb()

                // Dış halka
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = dotColor
                    alpha = 55
                }
                canvas.drawCircle(cx, cy, if (pt.isPb) 22f else 16f, haloPaint)

                // İç daire
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = dotColor
                }
                canvas.drawCircle(cx, cy, if (pt.isPb) 12f else 9f, dotPaint)

                val centerWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = android.graphics.Color.WHITE
                }
                canvas.drawCircle(cx, cy, 4.5f, centerWhitePaint)

                // Üst Değer Etiketi
                val valLabel = if (showWmaMode) {
                    String.format(Locale.US, "%%%.1f", pt.wmaPercent ?: 0.0)
                } else {
                    pt.record.result
                }
                canvas.drawText(valLabel, cx, cy - 24f, valueTextPaint)

                // PB Rozeti (En iyi derecenin üstünde)
                if (pt.isPb) {
                    val pbRect = RectF(cx - 26f, cy - 68f, cx + 26f, cy - 42f)
                    canvas.drawRoundRect(pbRect, 8f, 8f, pbTagPaint)
                    canvas.drawText("PB", pbRect.centerX(), pbRect.centerY() + 7f, pbTagTextPaint)
                }

                // Alt Eksen Yıl Etiketi
                canvas.drawText("${pt.record.year}", cx, padTop + chartH + 36f, yearTextPaint)
            }
        }
    }
}

@Composable
private fun AnalyticsMiniStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = BluePrimary,
                maxLines = 1
            )
        }
    }
}
