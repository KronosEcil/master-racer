package com.ecil.masterracer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Straighten
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.R
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.ui.theme.CrimsonAccent

data class HurdleStandardSpec(
    val categoryGroup: String,
    val categories: List<String>,
    val shortHurdleDistance: String,
    val shortHurdleHeight: String,
    val shortHurdleCount: Int,
    val shortToFirst: String,
    val shortBetween: String,
    val shortToFinish: String,
    val indoor60mFinish: String,
    val longHurdleDistance: String,
    val longHurdleHeight: String,
    val longHurdleCount: Int,
    val longToFirst: String,
    val longBetween: String,
    val longToFinish: String,
    val steepleDistance: String,
    val steepleHeight: String
)

data class ThrowStandardSpec(
    val categoryGroup: String,
    val categories: List<String>,
    val shotPut: String,
    val discus: String,
    val javelin: String,
    val hammer: String,
    val weightThrow: String
)

private val MEN_HURDLE_SPECS = listOf(
    HurdleStandardSpec(
        categoryGroup = "M35 • M40 • M45",
        categories = listOf("M35", "M40", "M45"),
        shortHurdleDistance = "110m Engelli",
        shortHurdleHeight = "0.991 m (99.1 cm)",
        shortHurdleCount = 10,
        shortToFirst = "13.72 m",
        shortBetween = "9.14 m",
        shortToFinish = "14.02 m",
        indoor60mFinish = "9.72 m (5 Engel)",
        longHurdleDistance = "400m Engelli",
        longHurdleHeight = "0.914 m (91.4 cm)",
        longHurdleCount = 10,
        longToFirst = "45.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "3000m Su Engelli",
        steepleHeight = "0.914 m"
    ),
    HurdleStandardSpec(
        categoryGroup = "M50 • M55",
        categories = listOf("M50", "M55"),
        shortHurdleDistance = "100m Engelli",
        shortHurdleHeight = "0.914 m (91.4 cm)",
        shortHurdleCount = 10,
        shortToFirst = "13.00 m",
        shortBetween = "8.50 m",
        shortToFinish = "10.50 m",
        indoor60mFinish = "13.00 m (5 Engel)",
        longHurdleDistance = "400m Engelli",
        longHurdleHeight = "0.838 m (84.0 cm)",
        longHurdleCount = 10,
        longToFirst = "45.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "3000m Su Engelli",
        steepleHeight = "0.914 m"
    ),
    HurdleStandardSpec(
        categoryGroup = "M60 • M65",
        categories = listOf("M60", "M65"),
        shortHurdleDistance = "100m Engelli",
        shortHurdleHeight = "0.838 m (84.0 cm)",
        shortHurdleCount = 10,
        shortToFirst = "12.00 m",
        shortBetween = "8.00 m",
        shortToFinish = "16.00 m",
        indoor60mFinish = "16.00 m (5 Engel)",
        longHurdleDistance = "300m Engelli",
        longHurdleHeight = "0.762 m (76.2 cm)",
        longHurdleCount = 7,
        longToFirst = "50.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "2000m Su Engelli",
        steepleHeight = "0.762 m"
    ),
    HurdleStandardSpec(
        categoryGroup = "M70 • M75",
        categories = listOf("M70", "M75"),
        shortHurdleDistance = "80m Engelli",
        shortHurdleHeight = "0.762 m (76.2 cm)",
        shortHurdleCount = 8,
        shortToFirst = "12.00 m",
        shortBetween = "7.00 m",
        shortToFinish = "19.00 m",
        indoor60mFinish = "20.00 m (5 Engel)",
        longHurdleDistance = "300m Engelli",
        longHurdleHeight = "0.686 m (68.6 cm)",
        longHurdleCount = 7,
        longToFirst = "50.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "2000m Su Engelli",
        steepleHeight = "0.762 m"
    ),
    HurdleStandardSpec(
        categoryGroup = "M80+ (M80 – M100)",
        categories = listOf("M80", "M85", "M90", "M95", "M100"),
        shortHurdleDistance = "80m Engelli",
        shortHurdleHeight = "0.686 m (68.6 cm)",
        shortHurdleCount = 8,
        shortToFirst = "12.00 m",
        shortBetween = "7.00 m",
        shortToFinish = "19.00 m",
        indoor60mFinish = "20.00 m (5 Engel)",
        longHurdleDistance = "200m Engelli",
        longHurdleHeight = "0.686 m (68.6 cm)",
        longHurdleCount = 5,
        longToFirst = "20.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "2000m Su Engelli",
        steepleHeight = "0.762 m"
    )
)

private val WOMEN_HURDLE_SPECS = listOf(
    HurdleStandardSpec(
        categoryGroup = "W35",
        categories = listOf("W35", "F35"),
        shortHurdleDistance = "100m Engelli",
        shortHurdleHeight = "0.838 m (84.0 cm)",
        shortHurdleCount = 10,
        shortToFirst = "13.00 m",
        shortBetween = "8.50 m",
        shortToFinish = "10.50 m",
        indoor60mFinish = "13.00 m (5 Engel)",
        longHurdleDistance = "400m Engelli",
        longHurdleHeight = "0.762 m (76.2 cm)",
        longHurdleCount = 10,
        longToFirst = "45.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "2000m Su Engelli",
        steepleHeight = "0.762 m"
    ),
    HurdleStandardSpec(
        categoryGroup = "W40 • W45",
        categories = listOf("W40", "W45", "F40", "F45"),
        shortHurdleDistance = "80m Engelli",
        shortHurdleHeight = "0.762 m (76.2 cm)",
        shortHurdleCount = 8,
        shortToFirst = "12.00 m",
        shortBetween = "8.00 m",
        shortToFinish = "12.00 m",
        indoor60mFinish = "16.00 m (5 Engel)",
        longHurdleDistance = "400m Engelli",
        longHurdleHeight = "0.762 m (76.2 cm)",
        longHurdleCount = 10,
        longToFirst = "45.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "2000m Su Engelli",
        steepleHeight = "0.762 m"
    ),
    HurdleStandardSpec(
        categoryGroup = "W50 • W55",
        categories = listOf("W50", "W55", "F50", "F55"),
        shortHurdleDistance = "80m Engelli",
        shortHurdleHeight = "0.762 m (76.2 cm)",
        shortHurdleCount = 8,
        shortToFirst = "12.00 m",
        shortBetween = "7.00 m",
        shortToFinish = "19.00 m",
        indoor60mFinish = "20.00 m (5 Engel)",
        longHurdleDistance = "300m Engelli",
        longHurdleHeight = "0.762 m (76.2 cm)",
        longHurdleCount = 7,
        longToFirst = "50.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "2000m Su Engelli",
        steepleHeight = "0.762 m"
    ),
    HurdleStandardSpec(
        categoryGroup = "W60 • W65",
        categories = listOf("W60", "W65", "F60", "F65"),
        shortHurdleDistance = "80m Engelli",
        shortHurdleHeight = "0.686 m (68.6 cm)",
        shortHurdleCount = 8,
        shortToFirst = "12.00 m",
        shortBetween = "7.00 m",
        shortToFinish = "19.00 m",
        indoor60mFinish = "20.00 m (5 Engel)",
        longHurdleDistance = "300m Engelli",
        longHurdleHeight = "0.686 m (68.6 cm)",
        longHurdleCount = 7,
        longToFirst = "50.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "2000m Su Engelli",
        steepleHeight = "0.762 m"
    ),
    HurdleStandardSpec(
        categoryGroup = "W70+ (W70 – W100)",
        categories = listOf("W70", "W75", "W80", "W85", "W90", "W95", "W100", "F70", "F75", "F80", "F85", "F90", "F95", "F100"),
        shortHurdleDistance = "80m Engelli",
        shortHurdleHeight = "0.686 m (68.6 cm)",
        shortHurdleCount = 8,
        shortToFirst = "12.00 m",
        shortBetween = "7.00 m",
        shortToFinish = "19.00 m",
        indoor60mFinish = "20.00 m (5 Engel)",
        longHurdleDistance = "200m Engelli",
        longHurdleHeight = "0.686 m (68.6 cm)",
        longHurdleCount = 5,
        longToFirst = "20.00 m",
        longBetween = "35.00 m",
        longToFinish = "40.00 m",
        steepleDistance = "2000m Su Engelli",
        steepleHeight = "0.762 m"
    )
)

private val MEN_THROW_SPECS = listOf(
    ThrowStandardSpec(
        categoryGroup = "M35 • M40 • M45",
        categories = listOf("M35", "M40", "M45"),
        shotPut = "7.26 kg",
        discus = "2.00 kg",
        javelin = "800 g",
        hammer = "7.26 kg",
        weightThrow = "15.88 kg (35 lb)"
    ),
    ThrowStandardSpec(
        categoryGroup = "M50 • M55",
        categories = listOf("M50", "M55"),
        shotPut = "6.00 kg",
        discus = "1.50 kg",
        javelin = "700 g",
        hammer = "6.00 kg",
        weightThrow = "11.34 kg (25 lb)"
    ),
    ThrowStandardSpec(
        categoryGroup = "M60 • M65",
        categories = listOf("M60", "M65"),
        shotPut = "5.00 kg",
        discus = "1.00 kg",
        javelin = "600 g",
        hammer = "5.00 kg",
        weightThrow = "9.08 kg (20 lb)"
    ),
    ThrowStandardSpec(
        categoryGroup = "M70 • M75",
        categories = listOf("M70", "M75"),
        shotPut = "4.00 kg",
        discus = "1.00 kg",
        javelin = "500 g",
        hammer = "4.00 kg",
        weightThrow = "7.26 kg (16 lb)"
    ),
    ThrowStandardSpec(
        categoryGroup = "M80+ (M80 – M100)",
        categories = listOf("M80", "M85", "M90", "M95", "M100"),
        shotPut = "3.00 kg",
        discus = "1.00 kg",
        javelin = "400 g",
        hammer = "3.00 kg",
        weightThrow = "5.45 kg (12 lb)"
    )
)

private val WOMEN_THROW_SPECS = listOf(
    ThrowStandardSpec(
        categoryGroup = "W35 • W40 • W45",
        categories = listOf("W35", "W40", "W45", "F35", "F40", "F45"),
        shotPut = "4.00 kg",
        discus = "1.00 kg",
        javelin = "600 g",
        hammer = "4.00 kg",
        weightThrow = "9.08 kg (20 lb)"
    ),
    ThrowStandardSpec(
        categoryGroup = "W50 • W55",
        categories = listOf("W50", "W55", "F50", "F55"),
        shotPut = "3.00 kg",
        discus = "1.00 kg",
        javelin = "500 g",
        hammer = "3.00 kg",
        weightThrow = "7.26 kg (16 lb)"
    ),
    ThrowStandardSpec(
        categoryGroup = "W60 • W65 • W70",
        categories = listOf("W60", "W65", "W70", "F60", "F65", "F70"),
        shotPut = "3.00 kg",
        discus = "1.00 kg",
        javelin = "500 g",
        hammer = "3.00 kg",
        weightThrow = "5.45 kg (12 lb)"
    ),
    ThrowStandardSpec(
        categoryGroup = "W75+ (W75 – W100)",
        categories = listOf("W75", "W80", "W85", "W90", "W95", "W100", "F75", "F80", "F85", "F90", "F95", "F100"),
        shotPut = "2.00 kg",
        discus = "0.75 kg",
        javelin = "400 g",
        hammer = "2.00 kg",
        weightThrow = "4.00 kg (8.8 lb)"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WmaStandardsScreen(
    athleteGender: String,
    athleteCategory: String,
    onBack: (() -> Unit)? = null
) {
    val initialNormalizedCat = remember(athleteCategory, athleteGender) {
        val clean = athleteCategory.uppercase().replace("F", "W")
        if (clean.startsWith("M") || clean.startsWith("W")) clean else "M35"
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Engelli Ölçüleri, 1 = Atma Ağırlıkları
    var selectedCategoryFilter by remember { mutableStateOf("Tümü") }
    var isGenderFemale by remember {
        mutableStateOf(athleteGender.equals("F", ignoreCase = true) || athleteGender.equals("W", ignoreCase = true))
    }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val availableCategories = remember(isGenderFemale) {
        val prefix = if (isGenderFemale) "W" else "M"
        listOf("Tümü") + listOf(35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90).map { "$prefix$it" }
    }

    val hurdleSpecs = remember(isGenderFemale, selectedCategoryFilter) {
        val base = if (isGenderFemale) WOMEN_HURDLE_SPECS else MEN_HURDLE_SPECS
        if (selectedCategoryFilter == "Tümü") base
        else base.filter { it.categories.contains(selectedCategoryFilter) }
    }

    val throwSpecs = remember(isGenderFemale, selectedCategoryFilter) {
        val base = if (isGenderFemale) WOMEN_THROW_SPECS else MEN_THROW_SPECS
        if (selectedCategoryFilter == "Tümü") base
        else base.filter { it.categories.contains(selectedCategoryFilter) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = Color.White
                            )
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "WMA Branş Standartları",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Resmi Engel Ölçüleri & Atma Ağırlıkları",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Cinsiyet ve Yaş Kategorisi Seçici Üst Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Erkek / Kadın Seçici
                Row(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(50.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = {
                            isGenderFemale = false
                            if (selectedCategoryFilter != "Tümü") {
                                selectedCategoryFilter = selectedCategoryFilter.replace("W", "M")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(9.dp),
                        color = if (!isGenderFemale) Color(0xFF2563EB) else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Erkek (M)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isGenderFemale) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            isGenderFemale = true
                            if (selectedCategoryFilter != "Tümü") {
                                selectedCategoryFilter = selectedCategoryFilter.replace("M", "W")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(9.dp),
                        color = if (isGenderFemale) Color(0xFFEC4899) else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Kadın (W)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGenderFemale) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Yaş Kategorisi Seçmeli Kutusu
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                    modifier = Modifier.weight(0.9f)
                ) {
                    OutlinedTextField(
                        value = if (selectedCategoryFilter == "Tümü") "Tüm Yaşlar" else selectedCategoryFilter,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = BluePrimary.copy(alpha = 0.4f)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (cat == "Tümü") "Tüm Yaş Kategorileri" else "$cat Kategorisi",
                                        fontWeight = if (cat == selectedCategoryFilter) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedCategoryFilter = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Sekmeler: Engelli Koşu Ölçüleri | Atma Ağırlıkları
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = BluePrimary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Engelli & Su Engelli",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_event_throw),
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Atma Ağırlıkları",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
            }

            // İçerik Listesi
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    items(hurdleSpecs) { spec ->
                        val isAthleteGroup = spec.categories.contains(initialNormalizedCat)
                        HurdleStandardCard(spec = spec, isHighlighted = isAthleteGroup, isFemale = isGenderFemale)
                    }
                } else {
                    items(throwSpecs) { spec ->
                        val isAthleteGroup = spec.categories.contains(initialNormalizedCat)
                        ThrowStandardCard(spec = spec, isHighlighted = isAthleteGroup, isFemale = isGenderFemale)
                    }
                }
            }
        }
    }
}

@Composable
private fun HurdleStandardCard(
    spec: HurdleStandardSpec,
    isHighlighted: Boolean,
    isFemale: Boolean
) {
    val accentColor = if (isFemale) Color(0xFFEC4899) else Color(0xFF2563EB)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                accentColor.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (isHighlighted) BorderStroke(1.5.dp, accentColor) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = spec.categoryGroup,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (isHighlighted) {
                    Surface(
                        color = Color(0xFF16A34A).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Senin Kategorin",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Kısa Engelli (Açık Saha & Salon 60m)
            Text(
                text = "Kısa Engelli: ${spec.shortHurdleDistance} & Salon 60m Engelli",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecMetricBox(
                    label = "Yükseklik",
                    value = spec.shortHurdleHeight,
                    highlight = true,
                    modifier = Modifier.weight(1.2f)
                )
                SpecMetricBox(
                    label = "1. Engele",
                    value = spec.shortToFirst,
                    modifier = Modifier.weight(1f)
                )
                SpecMetricBox(
                    label = "Engel Arası",
                    value = spec.shortBetween,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecMetricBox(
                    label = "Açık Saha Finiş",
                    value = "${spec.shortToFinish} (${spec.shortHurdleCount} Engel)",
                    modifier = Modifier.weight(1f)
                )
                SpecMetricBox(
                    label = "Salon 60m Finiş",
                    value = spec.indoor60mFinish,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // 2. Uzun Engelli
            Text(
                text = "Uzun Engelli: ${spec.longHurdleDistance} (${spec.longHurdleCount} Engel)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecMetricBox(
                    label = "Yükseklik",
                    value = spec.longHurdleHeight,
                    highlight = true,
                    modifier = Modifier.weight(1.2f)
                )
                SpecMetricBox(
                    label = "1. Engele",
                    value = spec.longToFirst,
                    modifier = Modifier.weight(0.9f)
                )
                SpecMetricBox(
                    label = "Arası / Finiş",
                    value = "${spec.longBetween} / ${spec.longToFinish}",
                    modifier = Modifier.weight(1.1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Su Engelli
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Su Engelli: ${spec.steepleDistance}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = "Engel Yüksekliği: ${spec.steepleHeight}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }
        }
    }
}

@Composable
private fun ThrowStandardCard(
    spec: ThrowStandardSpec,
    isHighlighted: Boolean,
    isFemale: Boolean
) {
    val accentColor = if (isFemale) Color(0xFFEC4899) else Color(0xFF2563EB)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                accentColor.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (isHighlighted) BorderStroke(1.5.dp, accentColor) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = spec.categoryGroup,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (isHighlighted) {
                    Surface(
                        color = Color(0xFF16A34A).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Senin Kategorin",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecMetricBox(
                    label = "Gülle Atma",
                    value = spec.shotPut,
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
                SpecMetricBox(
                    label = "Disk Atma",
                    value = spec.discus,
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
                SpecMetricBox(
                    label = "Cirit Atma",
                    value = spec.javelin,
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecMetricBox(
                    label = "Çekiç Atma",
                    value = spec.hammer,
                    modifier = Modifier.weight(1f)
                )
                SpecMetricBox(
                    label = "Ağırlık Atma (Weight)",
                    value = spec.weightThrow,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpecMetricBox(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (highlight) BluePrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlight) CrimsonAccent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
