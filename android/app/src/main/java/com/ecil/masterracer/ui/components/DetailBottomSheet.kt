package com.ecil.masterracer.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.data.model.Competition
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.util.CalendarHelper

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import com.ecil.masterracer.ui.theme.CrimsonAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomSheet(
    competition: Competition,
    sheetState: SheetState,
    isTarget: Boolean = false,
    onToggleTarget: () -> Unit = {},
    isCompleted: Boolean = false,
    onToggleCompleted: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val daysRemaining = competition.getDaysRemaining()

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
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = competition.getScopeTitle(),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                CountdownBadge(daysRemaining = daysRemaining)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = competition.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Detay Bilgi Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
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
                            Text(text = "Tarih Aralığı", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = competition.getFormattedDate(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Konum / Şehir
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Yarışma Yeri", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = competition.getDisplayLocation(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
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
                                Text(text = "Federasyon", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(
                                    text = competition.federation,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(20.dp))

            // Aksiyon Butonları
            // 1. Hedeflerime Ekle / Hedeflerimden Çıkar Butonu
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

            // 2. Yarış Tamamlandıysa: Katıldım Olarak İşaretle Butonu
            if (daysRemaining < 0) {
                OutlinedButton(
                    onClick = onToggleCompleted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, if (isCompleted) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isCompleted) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompleted) "Bu Yarışa Katıldım ✓" else "Bu Yarışa Katıldım Olarak İşaretle",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Cihaz Takvimine Ekle
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

            // 4. Resmi Web Sitesi Bağlantısı
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
}

