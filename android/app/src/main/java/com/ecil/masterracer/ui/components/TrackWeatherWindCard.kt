package com.ecil.masterracer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.util.TrackWeatherForecast
import com.ecil.masterracer.util.WeatherWindHelper
import java.util.Locale

@Composable
fun TrackWeatherWindCard(
    city: String,
    countryName: String = "",
    targetStartDate: String? = null,
    allowCitySelection: Boolean = false,
    showWarmupAdvice: Boolean = false,
    onCityChanged: ((String) -> Unit)? = null
) {
    var weatherData by remember { mutableStateOf<TrackWeatherForecast?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(city, countryName, targetStartDate, allowCitySelection) {
        isLoading = true
        weatherData = if (allowCitySelection) {
            // Çağrı Odası ekranında kullanıcının o an bulunduğu konumu ("Konumum") getir
            WeatherWindHelper.fetchCurrentLocationWeather()
                ?: WeatherWindHelper.fetchTrackWeather(rawCity = city.ifBlank { "İzmir" })
        } else {
            WeatherWindHelper.fetchTrackWeather(
                rawCity = city,
                countryName = countryName,
                targetStartDate = targetStartDate,
                forceRefresh = true
            )
        }
        isLoading = false
    }

    if (!isLoading && weatherData == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = BluePrimary
                    )
                }
            } else {
                val data = weatherData!!
                val (emoji, weatherLabel) = data.getWeatherEmojiAndText()

                if (!allowCitySelection) {
                    // 1. YARIŞMA DETAY PENCERESİ (Yarışma Zamanı Gündüz Hava Sıcaklığı & Rüzgar Tahmini)
                    val daytimeTemp = data.maxTempC ?: data.temperatureC
                    val maxWind = maxOf(data.windSpeedMs, data.windGustsMs)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Yarışma Zamanı Hava & Rüzgar Tahmini",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Hava sıcaklığı %.0f°C (%s)  •  💨 Rüzgar %.1f m/s",
                                    daytimeTemp,
                                    weatherLabel,
                                    maxWind
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // 2. ÇAĞRI ODASI EKRANI (Konumum + Anlık Hava + Anlık Rüzgar + Yön + Rekor Geçerlilik)
                    val isLegalWind = data.isWindWithinLegalLimit
                    val windStatusColor = if (isLegalWind) Color(0xFF16A34A) else Color(0xFFD97706)
                    val locationLabel = if (data.city.isNotBlank()) "Konumum (${data.city})" else "Konumum"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(text = emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = String.format(Locale.US, "%.1f°C • %s", data.temperatureC, weatherLabel),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "💨 %.1f m/s  •  Yön: %s",
                                        data.windSpeedMs,
                                        data.getWindDirectionCardinal()
                                    ),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // "Konumum" Etiketi (Seçim menüsü olmadan doğrudan mevcut konum)
                        Surface(
                            color = BluePrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = locationLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rekor Geçerlilik Durumu Rozeti
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = windStatusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, windStatusColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = if (isLegalWind)
                                "🟢 ≤ +2.0 m/s • Rekor Geçerli Rüzgar"
                            else
                                "⚠️ > +2.0 m/s • Rekor Sınırı Üstü Rüzgar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = windStatusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
