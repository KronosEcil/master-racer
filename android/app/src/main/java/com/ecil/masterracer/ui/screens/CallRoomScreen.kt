package com.ecil.masterracer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.R
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

enum class RaceDisciplineGroup(
    val title: String,
    val subtitle: String,
    val defaultCallRoomMin: Int
) {
    SPRINT_HURDLES("Sprint & Engelli", "60m – 400m / Engelli", 20),
    MIDDLE_LONG("Orta & Uzun Mesafe", "800m – 10.000m / Yürüyüş", 20),
    JUMPS_THROWS("Atlama & Atma", "Uzun, Yüksek, Sırık, Gülle, Disk, Cirit", 40)
}

data class WarmupStage(
    val minutesBeforeRace: Int,
    val title: String,
    val description: String,
    val isCallRoom: Boolean = false,
    val isRaceStart: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRoomScreen(
    onBack: () -> Unit
) {
    // Varsayılan yarış saatini şu andan ~90 dakika sonrasına yuvarlayarak başlat
    val initialCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, 90)
            val roundedMin = ((get(Calendar.MINUTE) + 2) / 5) * 5
            if (roundedMin >= 60) {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            } else {
                set(Calendar.MINUTE, roundedMin)
            }
        }
    }

    var targetHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var targetMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var selectedDiscipline by remember { mutableStateOf(RaceDisciplineGroup.SPRINT_HURDLES) }
    var callRoomLeadMinutes by remember { mutableIntStateOf(selectedDiscipline.defaultCallRoomMin) }

    // Her saniye güncellenen canlı zaman
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Hedef yarış zamanını (milisaniye) hesapla
    val raceTimeMillis = remember(targetHour, targetMinute, nowMillis) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Eğer seçilen saat şu andan 2 saatten fazla gerideyse ertesi gün kabul et
        if (cal.timeInMillis < nowMillis - 2 * 3600 * 1000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        cal.timeInMillis
    }

    val callRoomTimeMillis = raceTimeMillis - (callRoomLeadMinutes * 60 * 1000L)
    val diffToRaceSec = ((raceTimeMillis - nowMillis) / 1000L).toInt()
    val diffToCallRoomSec = ((callRoomTimeMillis - nowMillis) / 1000L).toInt()

    // Branş grubuna göre dinamik ısınma ve çağrı odası aşamaları
    val stages = remember(selectedDiscipline, callRoomLeadMinutes) {
        when (selectedDiscipline) {
            RaceDisciplineGroup.SPRINT_HURDLES -> listOf(
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes + 55,
                    title = "Genel Isınma Koşusu & Mobilite",
                    description = "10-12 dk hafif jok, eklem rotasyonları ve dinamik esneme hareketleri."
                ),
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes + 30,
                    title = "Koşu Drilleri & Artırmalar (Strides)",
                    description = "A-Skip, B-Skip, High Knees drilleri ve 3x50m artırma koşuları."
                ),
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes + 15,
                    title = "Çivili Ayakkabı & Takoz / Engel Çıkışı",
                    description = "Çivili ayakkabıları giy, 2x20-30m patlayıcı çıkış veya ilk engel provası yap."
                ),
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes,
                    title = "🚨 ÇAĞRI ODASI (CALL ROOM) SON GİRİŞ",
                    description = "Göğüs numarası, çivi boyu (maks. 6mm/9mm), forma ve kimlik kontrolü için Çağrı Odası'nda ol.",
                    isCallRoom = true
                ),
                WarmupStage(
                    minutesBeforeRace = 10,
                    title = "Piste Çıkış & Son Konsantrasyon",
                    description = "Kulvar ve takoz ayarı, derin nefes ve yarış başlangıç komutu odaklanması."
                ),
                WarmupStage(
                    minutesBeforeRace = 0,
                    title = "🏁 YARIŞ BAŞLANGICI (START)",
                    description = "Yarış saati! Maksimum odak ve en iyi derecen için bol şans!",
                    isRaceStart = true
                )
            )
            RaceDisciplineGroup.MIDDLE_LONG -> listOf(
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes + 45,
                    title = "Hafif Isınma Koşusu (Jog)",
                    description = "12-15 dk düşük nabızlı ısınma koşusu ve dinamik bacak esnetme."
                ),
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes + 25,
                    title = "Yarış Temposu Açma Koşuları",
                    description = "Hedef yarış temposunda 3-4 x 80m akıcı tempo koşusu ve nabız aktivasyonu."
                ),
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes + 10,
                    title = "Yarış Ayakkabısı / Çivili & Forma Hazırlığı",
                    description = "Son sıvı alımı, yarış ayakkabılarının bağlanması ve göğüs numarası kontrolü."
                ),
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes,
                    title = "🚨 ÇAĞRI ODASI (CALL ROOM) SON GİRİŞ",
                    description = "Seri/kulvar yoklaması, çip ve ekipman kontrolü için Çağrı Odası'na giriş.",
                    isCallRoom = true
                ),
                WarmupStage(
                    minutesBeforeRace = 8,
                    title = "Başlangıç Çizgisine Geçiş",
                    description = "Start hattında yer alma, son ısınma sıçramaları ve tempo planı."
                ),
                WarmupStage(
                    minutesBeforeRace = 0,
                    title = "🏁 YARIŞ BAŞLANGICI (START)",
                    description = "Yarış saati! Dengeli tempo ve güçlü finiş için bol şans!",
                    isRaceStart = true
                )
            )
            RaceDisciplineGroup.JUMPS_THROWS -> listOf(
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes + 50,
                    title = "Genel Isınma & Eklem Aktivasyonu",
                    description = "10 dk hafif koşu, omuz/kalça/ayak bileği mobilitesi ve dinamik açma."
                ),
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes + 25,
                    title = "Teknik Driller & Pliometrik Hazırlık",
                    description = "Sıçrama drilleri veya atış rotasyon/ivmelenme provaları."
                ),
                WarmupStage(
                    minutesBeforeRace = callRoomLeadMinutes,
                    title = "🚨 ÇAĞRI ODASI (CALL ROOM) SON GİRİŞ",
                    description = "Saha branşlarında deneme atlayışları/atışları için sektöre erken geçiş kontrolü.",
                    isCallRoom = true
                ),
                WarmupStage(
                    minutesBeforeRace = 20,
                    title = "Sektörde Resmi Deneme & Adım Markajı",
                    description = "Pist/sektör içinde adım ölçümü (markaj) ve 1-2 resmi ısınma denemesi."
                ),
                WarmupStage(
                    minutesBeforeRace = 0,
                    title = "🏁 YARIŞMA BAŞLANGICI",
                    description = "İlk hakkın başlıyor! Teknik ritmine güven, bol şans!",
                    isRaceStart = true
                )
            )
        }
    }

    // Yarış Günü Kontrol Listesi (Checklist)
    val checklistItems = remember {
        listOf(
            "Göğüs numarası (Ön/Arka) ve çengelli iğneler hazır",
            "Çivili ayakkabı çivi boyu kurallara uygun (6mm / 9mm)",
            "Lisans / Kimlik / Akreditasyon kartı yanımda",
            "Su, elektrolit ve ısınma eşofmanı hazır",
            "Kulvar / Seri / Adım markaj bilgisi kontrol edildi"
        )
    }
    val checkedStates = remember { mutableStateListOf(false, false, false, false, false) }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Yarış Günü & Çağrı Odası",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Isınma Planlayıcı & Canlı Geri Sayım",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. CANLI GERİ SAYIM BANNER KARTI
            item {
                val isCallRoomUrgent = diffToCallRoomSec in 1..900 // Son 15 dk
                val isCallRoomPassed = diffToCallRoomSec <= 0 && diffToRaceSec > 0
                val bannerBrush = when {
                    isCallRoomPassed -> Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
                    isCallRoomUrgent -> Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309)))
                    else -> Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bannerBrush)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CANLI YARIŞ SAYACI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFFFBBF24)
                                )
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "Yarış: %02d:%02d", targetHour, targetMinute),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Çağrı Odasına Kalan
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ÇAĞRI ODASINA KALAN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatCountdownSeconds(diffToCallRoomSec),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (diffToCallRoomSec <= 0) Color(0xFFFDE047) else Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val callRoomCal = Calendar.getInstance().apply { timeInMillis = callRoomTimeMillis }
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "Saat %02d:%02d (-%d dk)",
                                            callRoomCal.get(Calendar.HOUR_OF_DAY),
                                            callRoomCal.get(Calendar.MINUTE),
                                            callRoomLeadMinutes
                                        ),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // Yarış Başlangıcına Kalan
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "STARTA KALAN SÜRE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatCountdownSeconds(diffToRaceSec),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "Start: %02d:%02d", targetHour, targetMinute),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. YARIŞ SAATİ & BRANŞ GRUBU AYARLARI KARTI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. YARIŞ SAATİNİ BELİRLE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = BluePrimary
                            )

                            Surface(
                                modifier = Modifier.clickable {
                                    val c = Calendar.getInstance().apply { add(Calendar.MINUTE, 90) }
                                    targetHour = c.get(Calendar.HOUR_OF_DAY)
                                    targetMinute = ((c.get(Calendar.MINUTE) + 2) / 5) * 5 % 60
                                },
                                color = BluePrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+90 dk Sıfırla",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BluePrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Saat ve Dakika Seçici
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Saat Kontrolü
                            TimeStepperBox(
                                label = "SAAT",
                                valueText = String.format(Locale.US, "%02d", targetHour),
                                onDecrement = { targetHour = (targetHour + 23) % 24 },
                                onIncrement = { targetHour = (targetHour + 1) % 24 }
                            )

                            Text(
                                text = ":",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            // Dakika Kontrolü (5'er dk ve 1'er dk hassas ayar)
                            TimeStepperBox(
                                label = "DAKİKA",
                                valueText = String.format(Locale.US, "%02d", targetMinute),
                                onDecrement = { targetMinute = (targetMinute + 55) % 60 },
                                onIncrement = { targetMinute = (targetMinute + 5) % 60 }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Branş Grubu Seçimi
                        Text(
                            text = "2. BRANŞ GRUBU & ÇAĞRI ODASI SÜRESİ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RaceDisciplineGroup.entries.forEach { group ->
                                val isSelected = selectedDiscipline == group
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedDiscipline = group
                                            callRoomLeadMinutes = group.defaultCallRoomMin
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) BluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        when (group) {
                                            RaceDisciplineGroup.SPRINT_HURDLES -> Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else BluePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            RaceDisciplineGroup.MIDDLE_LONG -> Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else BluePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            RaceDisciplineGroup.JUMPS_THROWS -> Icon(
                                                painter = painterResource(id = R.drawable.ic_event_jump),
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else BluePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = group.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Çağrı Odası Kapanış Süresi Seçenekleri (15, 20, 25, 30, 40 dk önce)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Çağrı Odası Kapanışı:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(15, 20, 30, 40).forEach { mins ->
                                    FilterChip(
                                        selected = callRoomLeadMinutes == mins,
                                        onClick = { callRoomLeadMinutes = mins },
                                        label = {
                                            Text(
                                                text = "-$mins dk",
                                                fontSize = 11.sp,
                                                fontWeight = if (callRoomLeadMinutes == mins) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrimsonAccent,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. ADIM ADIM ISINMA VE ÇAĞRI ODASI ZAMAN ÇİZELGESİ
            item {
                Text(
                    text = "ADIM ADIM ISINMA & ÇAĞRI ODASI PLANI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = BluePrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(stages) { index, stage ->
                val stageTimeMillis = raceTimeMillis - (stage.minutesBeforeRace * 60 * 1000L)
                val nextStageTimeMillis = if (index + 1 < stages.size) {
                    raceTimeMillis - (stages[index + 1].minutesBeforeRace * 60 * 1000L)
                } else {
                    raceTimeMillis + 1800 * 1000L
                }

                val isCompletedStage = nowMillis >= nextStageTimeMillis
                val isCurrentStage = nowMillis in stageTimeMillis until nextStageTimeMillis
                val stageCal = Calendar.getInstance().apply { timeInMillis = stageTimeMillis }
                val stageClockStr = String.format(
                    Locale.US,
                    "%02d:%02d",
                    stageCal.get(Calendar.HOUR_OF_DAY),
                    stageCal.get(Calendar.MINUTE)
                )

                val cardBorderColor = when {
                    stage.isCallRoom -> CrimsonAccent
                    isCurrentStage -> Color(0xFF16A34A)
                    else -> Color.Transparent
                }

                val cardBgColor = when {
                    stage.isCallRoom -> CrimsonAccent.copy(alpha = 0.10f)
                    isCurrentStage -> Color(0xFF16A34A).copy(alpha = 0.10f)
                    isCompletedStage -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    border = if (cardBorderColor != Color.Transparent) BorderStroke(1.5.dp, cardBorderColor) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Saat Rozeti
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(58.dp)
                        ) {
                            Surface(
                                color = when {
                                    stage.isCallRoom -> CrimsonAccent
                                    stage.isRaceStart -> Color(0xFF16A34A)
                                    isCurrentStage -> BluePrimary
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = stageClockStr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (stage.isCallRoom || stage.isRaceStart || isCurrentStage)
                                        Color.White
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (stage.minutesBeforeRace == 0) "START" else "-${stage.minutesBeforeRace} dk",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stage.isCallRoom) CrimsonAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Başlık ve Açıklama
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stage.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        stage.isCallRoom -> CrimsonAccent
                                        isCompletedStage -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                if (isCurrentStage) {
                                    Surface(
                                        color = Color(0xFF16A34A),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = "ŞU AN",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isCompletedStage) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Tamamlandı",
                                        tint = Color(0xFF16A34A).copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stage.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isCompletedStage) 0.45f else 0.75f),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // 4. ÇAĞRI ODASI KONTROL LİSTESİ (CHECKLIST)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📋 ÇAĞRI ODASI SON KONTROL LİSTESİ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        checklistItems.forEachIndexed { idx, itemText ->
                            val isChecked = checkedStates[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { checkedStates[idx] = !isChecked }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isChecked) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = itemText,
                                    fontSize = 13.sp,
                                    fontWeight = if (isChecked) FontWeight.Medium else FontWeight.SemiBold,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isChecked)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.onSurface
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
private fun TimeStepperBox(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Azalt",
                    tint = BluePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = valueText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Artır",
                    tint = BluePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatCountdownSeconds(totalSec: Int): String {
    if (totalSec <= 0) return "00:00:00"
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
}
