package com.ecil.masterracer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.ui.theme.BluePrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    sheetState: SheetState,
    availableCountries: List<Pair<String, String>>, // Pair(code, name)
    selectedCountries: Set<String>,
    onCountryToggle: (String) -> Unit,
    onClearCountries: () -> Unit,
    selectedVenueTypes: Set<String>,
    onVenueTypeToggle: (String) -> Unit,
    onClearVenueTypes: () -> Unit,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    onClearCategories: () -> Unit,
    hideCompleted: Boolean = false,
    onToggleHideCompleted: () -> Unit = {},
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Başlık Çubuğu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtreler",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "Sonuçları Daralt",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 1. TESİS VE YARIŞMA TÜRÜ
            SectionHeader(
                title = "Tesis Türü",
                badgeText = if (selectedVenueTypes.isNotEmpty()) "${selectedVenueTypes.size} seçili" else null
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val venues = listOf(
                    Triple("Tümü", "", null),
                    Triple("Açık Saha", "outdoor", "🏟️"),
                    Triple("Salon", "indoor", "🏛️"),
                    Triple("Yol / Maraton", "road", "🏃"),
                    Triple("Kros", "cross", "🌲")
                )

                venues.forEach { (label, key, icon) ->
                    val isSelected = if (key.isEmpty()) selectedVenueTypes.isEmpty() else selectedVenueTypes.contains(key)
                    CompactChip(
                        selected = isSelected,
                        onClick = {
                            if (key.isEmpty()) onClearVenueTypes() else onVenueTypeToggle(key)
                        },
                        label = label,
                        icon = icon
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. BRANŞ KATEGORİSİ
            SectionHeader(
                title = "Branş Grubu",
                badgeText = if (selectedCategories.isNotEmpty()) "${selectedCategories.size} seçili" else null
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactChip(
                    selected = selectedCategories.isEmpty(),
                    onClick = onClearCategories,
                    label = "Tüm Branşlar"
                )

                val categories = listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe")
                categories.forEach { cat ->
                    val isSelected = selectedCategories.contains(cat)
                    CompactChip(
                        selected = isSelected,
                        onClick = { onCategoryToggle(cat) },
                        label = cat
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. YARIŞMA DURUMU
            SectionHeader(title = "Yarışma Durumu")
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactChip(
                    selected = !hideCompleted,
                    onClick = { if (hideCompleted) onToggleHideCompleted() },
                    label = "Tümünü Göster",
                    modifier = Modifier.weight(1f)
                )

                CompactChip(
                    selected = hideCompleted,
                    onClick = { if (!hideCompleted) onToggleHideCompleted() },
                    label = "Tamamlananları Gizle",
                    selectedColor = Color(0xFFE11D48),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. ÜLKE SEÇİMİ
            SectionHeader(
                title = "Ülke",
                badgeText = if (selectedCountries.isNotEmpty()) "${selectedCountries.size} seçili" else null
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactChip(
                    selected = selectedCountries.isEmpty(),
                    onClick = onClearCountries,
                    label = "Tüm Ülkeler",
                    icon = "🌍"
                )

                availableCountries.forEach { (code, name) ->
                    val isSelected = selectedCountries.contains(code)
                    val flag = getFilterCountryFlag(code)
                    CompactChip(
                        selected = isSelected,
                        onClick = { onCountryToggle(code) },
                        label = name,
                        icon = flag
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Alt Eylem Çubuğu: Sıfırla ve Uygula Yan Yana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onResetFilters,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = "Sıfırla",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1.6f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text(
                        text = "Filtreleri Uygula",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
        if (badgeText != null) {
            Surface(
                color = BluePrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BluePrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: String? = null,
    selectedColor: Color = BluePrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) selectedColor else Color(0xFFF8FAFC),
        contentColor = if (selected) Color.White else Color(0xFF334155),
        border = if (selected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Text(text = icon, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

private fun getFilterCountryFlag(countryCode: String): String {
    return when (countryCode.uppercase()) {
        "TR", "TUR" -> "🇹🇷"
        "GR", "GRE" -> "🇬🇷"
        "DE", "GER" -> "🇩🇪"
        "IT", "ITA" -> "🇮🇹"
        "GB", "UK", "GBR" -> "🇬🇧"
        "ES", "ESP" -> "🇪🇸"
        "FR", "FRA" -> "🇫🇷"
        "PL", "POL" -> "🇵🇱"
        "BG", "BUL" -> "🇧🇬"
        "RO", "ROU" -> "🇷🇴"
        "RS", "SRB" -> "🇷🇸"
        "SE", "SWE" -> "🇸🇪"
        "NL", "NED" -> "🇳🇱"
        "PT", "POR" -> "🇵🇹"
        "KOR" -> "🇰🇷"
        "JP", "JPN" -> "🇯🇵"
        "USA" -> "🇺🇸"
        "FIN" -> "🇫🇮"
        "CH" -> "🇨🇭"
        "IE" -> "🇮🇪"
        "SI" -> "🇸🇮"
        "AL" -> "🇦🇱"
        "CZ" -> "🇨🇿"
        "NO" -> "🇳🇴"
        "DK" -> "🇩🇰"
        "IS" -> "🇮🇸"
        "LV" -> "🇱🇻"
        "LT" -> "🇱🇹"
        "HR" -> "🇭🇷"
        "AT" -> "🇦🇹"
        "BE" -> "🇧🇪"
        "EE" -> "🇪🇪"
        "EU" -> "🇪🇺"
        else -> "🏃"
    }
}

