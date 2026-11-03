package com.ecil.masterracer.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.data.model.Competition
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent
import com.ecil.masterracer.util.CalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val PRESET_DEPARTURE_CITIES = listOf(
    "İstanbul",
    "Ankara",
    "İzmir",
    "Antalya",
    "Adana / Çukurova",
    "Bursa",
    "Trabzon",
    "Gaziantep",
    "Diyarbakır",
    "Kayseri",
    "Samsun",
    "Konya"
)

private data class TravelDateWindow(
    val checkInYmd: String,
    val checkOutYmd: String,
    val readableRange: String
)

private fun computeTravelDateWindow(startDateStr: String, endDateStr: String): TravelDateWindow? {
    if (startDateStr.isBlank()) return null
    return try {
        val ymdFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val trDisplayFormat = SimpleDateFormat("d MMM", Locale.forLanguageTag("tr-TR"))
        val trYearFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("tr-TR"))

        val parsedStart = ymdFormat.parse(startDateStr) ?: return null
        val parsedEnd = if (endDateStr.isNotBlank()) ymdFormat.parse(endDateStr) ?: parsedStart else parsedStart

        val calIn = Calendar.getInstance().apply {
            time = parsedStart
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val calOut = Calendar.getInstance().apply {
            time = parsedEnd
            add(Calendar.DAY_OF_MONTH, 1)
        }

        val checkInYmd = ymdFormat.format(calIn.time)
        val checkOutYmd = ymdFormat.format(calOut.time)
        val readable = "${trDisplayFormat.format(calIn.time)} Gidiş – ${trYearFormat.format(calOut.time)} Dönüş"
        TravelDateWindow(
            checkInYmd = checkInYmd,
            checkOutYmd = checkOutYmd,
            readableRange = readable
        )
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailBottomSheet(
    competition: Competition,
    sheetState: SheetState,
    isTarget: Boolean = false,
    onToggleTarget: () -> Unit = {},
    isCompleted: Boolean = false,
    onToggleCompleted: () -> Unit = {},
    departureCity: String = "İstanbul",
    onUpdateDepartureCity: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val daysRemaining = competition.getDaysRemaining()
    var showDeparturePickerDialog by remember { mutableStateOf(false) }

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
                .navigationBarsPadding()
        ) {
            // Başlık & Bayrak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = competition.getCountryFlag(), fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = competition.getDisplayCountryName(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = competition.getScopeTitle(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                CountdownBadge(daysRemaining = daysRemaining)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = competition.getDisplayTitle(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Detay Bilgi Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tarih
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tarih Aralığı",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = competition.getFormattedDate(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Konum / Stadyum / Şehir & Google Maps Butonu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { competition.openInGoogleMaps(context) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Haritada Aç",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Yarışma Yeri & Stadyum",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = competition.getVenueAndCity(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BluePrimary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BluePrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Haritada Aç ↗",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (competition.federation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Federasyon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Federasyon",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = competition.federation,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // ✈️ YARIŞMA SEYAHAT & STADYUM ÇEVRESİ KONAKLAMA ASİSTANI
            // (Yalnızca gelecek / devam eden yarışmalarda gösterilir)
            // -----------------------------------------------------------------
            if (!competition.isCompleted()) {
                Spacer(modifier = Modifier.height(14.dp))

                TravelAndAccommodationAssistantCard(
                    competition = competition,
                    departureCity = departureCity,
                    onClickChangeDeparture = { showDeparturePickerDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(18.dp))

            // Aksiyon Butonları
            // 1. Takibime Ekle / Takipten Çıkar Butonu
            Button(
                onClick = onToggleTarget,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTarget) CrimsonAccent else BluePrimary
                )
            ) {
                Icon(
                    imageVector = if (isTarget) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTarget) "Takibimde Kayıtlı ✓ (Takipten Çıkar)" else "Takibime Ekle",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Cihaz Takvimine Ekle
            OutlinedButton(
                onClick = { CalendarHelper.addEventToCalendar(context, competition) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = BluePrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cihaz Takvimine Ekle",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Resmi Web Sitesi Bağlantısı
            if (!competition.website_url.isNullOrBlank()) {
                OutlinedButton(
                    onClick = { CalendarHelper.openUrl(context, competition.website_url) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resmi Web Sayfasına Git",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeparturePickerDialog) {
        DepartureCityPickerDialog(
            currentCity = departureCity,
            onSelectCity = { chosen ->
                onUpdateDepartureCity(chosen)
                showDeparturePickerDialog = false
            },
            onDismiss = { showDeparturePickerDialog = false }
        )
    }
}

@Composable
private fun TravelAndAccommodationAssistantCard(
    competition: Competition,
    departureCity: String,
    onClickChangeDeparture: () -> Unit
) {
    val context = LocalContext.current
    val travelWindow = remember(competition.start_date, competition.end_date) {
        computeTravelDateWindow(competition.start_date, competition.end_date)
    }

    val destinationCity = competition.city.ifBlank { competition.getDisplayCountryName() }
    val trLocale = Locale.forLanguageTag("tr-TR")
    val isSameCity = remember(departureCity, destinationCity) {
        val depNorm = departureCity.lowercase(trLocale).trim()
        val destNorm = destinationCity.lowercase(trLocale).trim()
        depNorm.isNotBlank() && destNorm.isNotBlank() && (depNorm.contains(destNorm) || destNorm.contains(depNorm))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BluePrimary.copy(alpha = 0.07f)
        ),
        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Başlık & Kalkış Şehri Seçici Çip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(BluePrimary.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Seyahat & Konaklama Asistanı",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (travelWindow != null) {
                            Text(
                                text = "Önerilen: ${travelWindow.readableRange}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.clickable { onClickChangeDeparture() },
                    shape = RoundedCornerShape(8.dp),
                    color = BluePrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "🛫 Kalkış: $departureCity ▾",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. UÇUŞ / ROTA BÖLÜMÜ
            if (isSameCity) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF16A34A).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openStadiumTransitDirections(context, competition) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📍 Yarışma Kendi Şehrinizde ($destinationCity)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "Stadyuma toplu taşıma ve araç yol tarifini açmak için dokunun",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "Yol Tarifi ↗",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            } else {
                // Farklı şehir veya ülke ise Uçuş Arama Butonları
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                openGoogleFlightsSearch(
                                    context = context,
                                    departureCity = departureCity,
                                    destinationCity = destinationCity,
                                    destinationCountry = competition.getDisplayCountryName(),
                                    travelWindow = travelWindow
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "✈️ $departureCity ➔ $destinationCity Uçuş Ara",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Google Flights üzerinde gidiş-dönüş uçak biletlerini listele",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                        Text(
                            text = "Uçuşlar ↗",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. STADYUM ÇEVRESİ KONAKLAMA BUTONLARI (Harita + Booking Tarihli)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { openNearbyHotelsOnMaps(context, competition) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Stadyum Yakını",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Haritada Oteller ↗",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { openBookingHotelsSearch(context, competition, travelWindow) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hotel,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Booking.com",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Tarihli Otel Ara ↗",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. YARIŞ GÜNÜ STADYUM ÇEVRESİ HIZLI KISAYOLLAR (Ulaşım, Restoran/Market, Eczane)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StadiumPoiChip(
                    icon = Icons.Default.DirectionsTransit,
                    label = "Stadyuma Yol Tarifi",
                    onClick = { openStadiumTransitDirections(context, competition) }
                )
                StadiumPoiChip(
                    icon = Icons.Default.Restaurant,
                    label = "Sporcu Restoran & Market",
                    onClick = { openMapsPoiNearVenue(context, competition, "restoran ve market") }
                )
                StadiumPoiChip(
                    icon = Icons.Default.LocalPharmacy,
                    label = "Yakın Eczane & Sağlık",
                    onClick = { openMapsPoiNearVenue(context, competition, "eczane pharmacy") }
                )
            }
        }
    }
}

@Composable
private fun StadiumPoiChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepartureCityPickerDialog(
    currentCity: String,
    onSelectCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customCityText by remember(currentCity) { mutableStateOf(currentCity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🛫 Kalkış Şehrinizi Seçin",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Seçtiğiniz şehir hafızaya kaydedilir ve tüm yarışmalarda uçuş/rota aramalarında otomatik kullanılır.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PRESET_DEPARTURE_CITIES.forEach { cityOption ->
                        val cleanName = cityOption.substringBefore(" /").trim()
                        val isSelected = customCityText.equals(cleanName, ignoreCase = true)
                        Surface(
                            modifier = Modifier.clickable {
                                customCityText = cleanName
                                onSelectCity(cleanName)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = cityOption,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customCityText,
                    onValueChange = { customCityText = it },
                    label = { Text("Veya Şehir / Havalimanı Yazın") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectCity(customCityText.ifBlank { "İstanbul" }) },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

private fun openGoogleFlightsSearch(
    context: Context,
    departureCity: String,
    destinationCity: String,
    destinationCountry: String,
    travelWindow: TravelDateWindow?
) {
    val destQuery = if (destinationCity.isNotBlank()) destinationCity else destinationCountry
    val queryText = if (travelWindow != null) {
        "Flights from $departureCity to $destQuery on ${travelWindow.checkInYmd} through ${travelWindow.checkOutYmd}"
    } else {
        "Flights from $departureCity to $destQuery"
    }
    val url = "https://www.google.com/travel/flights?hl=tr&q=${Uri.encode(queryText)}"
    CalendarHelper.openUrl(context, url)
}

private fun openNearbyHotelsOnMaps(context: Context, competition: Competition) {
    val locationQuery = competition.getVenueAndCity()
    val gmmUri = Uri.parse("geo:0,0?q=${Uri.encode("otel near $locationQuery")}")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webUrl = "https://www.google.com/maps/search/?api=1&query=${Uri.encode("hotels near $locationQuery")}"
        CalendarHelper.openUrl(context, webUrl)
    }
}

private fun openBookingHotelsSearch(
    context: Context,
    competition: Competition,
    travelWindow: TravelDateWindow?
) {
    val dest = competition.city.ifBlank { competition.getDisplayCountryName() }
    val url = if (travelWindow != null) {
        "https://www.booking.com/searchresults.tr.html?ss=${Uri.encode(dest)}&checkin=${travelWindow.checkInYmd}&checkout=${travelWindow.checkOutYmd}"
    } else {
        "https://www.booking.com/searchresults.tr.html?ss=${Uri.encode(dest)}"
    }
    CalendarHelper.openUrl(context, url)
}

private fun openStadiumTransitDirections(context: Context, competition: Competition) {
    val dest = competition.getVenueAndCity()
    val url = "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(dest)}&travelmode=transit"
    CalendarHelper.openUrl(context, url)
}

private fun openMapsPoiNearVenue(context: Context, competition: Competition, poiKeyword: String) {
    val locationQuery = "${competition.getVenueAndCity()} yakınında $poiKeyword"
    val gmmUri = Uri.parse("geo:0,0?q=${Uri.encode(locationQuery)}")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webUrl = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(locationQuery)}"
        CalendarHelper.openUrl(context, webUrl)
    }
}
