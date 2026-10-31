package com.ecil.masterracer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent
import com.ecil.masterracer.util.WmaAgeGradingHelper
import com.ecil.masterracer.util.WmaEvaluationResult
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WmaCalculatorBottomSheet(
    sheetState: SheetState,
    initialGender: String = "M",
    initialBirthYear: Int = 1985,
    onDismiss: () -> Unit,
    onSaveToProfile: ((eventName: String, resultStr: String, numericValue: Double, category: String) -> Unit)? = null
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val defaultAge = if (initialBirthYear in 1920..currentYear) currentYear - initialBirthYear else 40

    var selectedGender by remember { mutableStateOf(if (initialGender.equals("F", true)) "F" else "M") }
    var ageInput by remember { mutableStateOf(defaultAge.toString()) }
    var selectedEventConfig by remember { mutableStateOf(WmaAgeGradingHelper.SUPPORTED_EVENTS[1]) } // 100 Metre
    var eventDropdownExpanded by remember { mutableStateOf(false) }

    // Girdi formatı
    var inputResult by remember { mutableStateOf("") }
    var calculationResult by remember { mutableStateOf<WmaEvaluationResult?>(null) }
    var calculationError by remember { mutableStateOf("") }

    fun calculate() {
        calculationError = ""
        val age = ageInput.toIntOrNull()
        if (age == null || age < 30 || age > 105) {
            calculationError = "Lütfen 30 ile 105 arasında geçerli bir yaş girin."
            calculationResult = null
            return
        }

        val numeric = WmaAgeGradingHelper.parseInputToNumeric(inputResult, selectedEventConfig.isField)
        if (numeric == null || numeric <= 0.0) {
            calculationError = if (selectedEventConfig.isField) {
                "Lütfen geçerli bir mesafe girin (Örn: 5.84 veya 12.45)"
            } else {
                "Lütfen geçerli bir süre girin (Örn: 11.85, 2:04.20 veya 1:28:40)"
            }
            calculationResult = null
            return
        }

        val result = WmaAgeGradingHelper.evaluatePerformance(
            gender = selectedGender,
            age = age,
            eventKey = selectedEventConfig.key,
            numericValue = numeric
        )

        if (result == null) {
            calculationError = "Hesaplama yapılamadı, lütfen değerleri kontrol edin."
            calculationResult = null
        } else {
            calculationResult = result
        }
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
                            .background(BluePrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WMA Yaş Faktörü Hesaplayıcı",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Açık Yaş (Open Senior) & Dünya Klasmanı %",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cinsiyet ve Yaş Seçimi
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Cinsiyet
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "Cinsiyet",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedGender = "M"
                                    calculate()
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedGender == "M") Color(0xFF2563EB) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Male,
                                    contentDescription = null,
                                    tint = if (selectedGender == "M") Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Erkek",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedGender == "M") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedGender = "F"
                                    calculate()
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedGender == "F") Color(0xFFEC4899) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Female,
                                    contentDescription = null,
                                    tint = if (selectedGender == "F") Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kadın",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedGender == "F") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Yaş
                Column(modifier = Modifier.weight(0.8f)) {
                    Text(
                        text = "Yaş",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = {
                            if (it.length <= 3) {
                                ageInput = it
                                calculate()
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Branş Seçimi
            Text(
                text = "Atletizm Branşı",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = eventDropdownExpanded,
                onExpandedChange = { eventDropdownExpanded = !eventDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedEventConfig.displayName,
                    onValueChange = {},
                    readOnly = true,
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
                    WmaAgeGradingHelper.SUPPORTED_EVENTS.forEach { config ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (config.isField) "🎯 " else "🏃 ",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = config.displayName, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                selectedEventConfig = config
                                eventDropdownExpanded = false
                                calculate()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Derece Girişi
            val hintText = if (selectedEventConfig.isField) {
                "Örn: 5.84 (metre cinsinden)"
            } else {
                when (selectedEventConfig.key) {
                    "60m", "100m", "200m", "400m" -> "Örn: 11.84 veya 52.30 (saniye)"
                    "800m", "1500m", "3000m", "5000m", "10000m" -> "Örn: 2:04.50 veya 15:42.10 (dk:sn)"
                    else -> "Örn: 1:24:15 (saat:dk:sn)"
                }
            }

            Text(
                text = "Koşulan / Atlanan Derece",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = inputResult,
                onValueChange = {
                    inputResult = it
                    calculate()
                },
                placeholder = { Text(text = hintText, fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (inputResult.isNotBlank()) {
                        IconButton(onClick = {
                            inputResult = ""
                            calculationResult = null
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            if (calculationError.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = calculationError,
                    fontSize = 11.sp,
                    color = CrimsonAccent,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SONUÇ KARTI
            calculationResult?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(res.badgeColorHex).copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(res.badgeColorHex).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WMA HESAPLAMA SONUCU",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(res.badgeColorHex)
                            )

                            Surface(
                                color = Color(res.badgeColorHex),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = res.classificationLevel.uppercase(Locale.ROOT),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Büyük Yüzde
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%%%05.2f", res.scorePercentage),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(res.badgeColorHex)
                            )

                            Text(
                                text = res.classificationTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(res.badgeColorHex).copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Detaylar: Açık Yaş Karşılığı & Katsayı
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(
                                    text = "Açık Yaş (25 Yaş Elit) Dengi",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = res.formattedEquivalent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "WMA Yaş Katsayısı",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.US, "%.4f", res.ageFactor),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Profilime Kaydet Butonu
                        if (onSaveToProfile != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val prefix = if (selectedGender == "F") "W" else "M"
                                    val bracket = ((res.age / 5) * 5)
                                    val cat = "$prefix$bracket"
                                    onSaveToProfile(res.eventName, res.formattedActual, res.actualValue, cat)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Bu Dereceyi Profilime Kaydet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // WMA Standartları Bilgi Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "WMA Klasman Kriterleri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• %90.0 ve üzeri: 🌟 Dünya Klasmanı (World Class)\n" +
                               "• %80.0 - %89.9: 🥇 Ulusal Klasman (National Class)\n" +
                               "• %70.0 - %79.9: 🥈 Bölgesel Klasman (Regional Class)\n" +
                               "• %60.0 - %69.9: 🥉 Yerel Seviye (Local Class)",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
