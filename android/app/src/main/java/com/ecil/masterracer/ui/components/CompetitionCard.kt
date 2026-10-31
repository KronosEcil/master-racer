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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.data.model.Competition
import com.ecil.masterracer.ui.theme.BluePrimary
import com.ecil.masterracer.util.CalendarHelper

@Composable
fun CompetitionCard(
    competition: Competition,
    onClick: (Competition) -> Unit,
    modifier: Modifier = Modifier,
    isTracked: Boolean = false,
    onToggleTrack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val daysRemaining = competition.getDaysRemaining()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick(competition) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Üst Satır: Bayrak, Ülke, Federasyon & Sayaç + Takvim + Takip Simgeleri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = competition.getCountryFlag(),
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = competition.getDisplayCountryName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = competition.getScopeTitle(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                         )
                    }

                    if (competition.is_new) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF16A34A),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "YENİ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CountdownBadge(daysRemaining = daysRemaining)
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = { CalendarHelper.addEventToCalendar(context, competition) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Cihaz Takvimine Ekle",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    if (onToggleTrack != null) {
                        IconButton(
                            onClick = onToggleTrack,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isTracked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isTracked) "Takipte (Takipten Çıkar)" else "Takibime Ekle",
                                tint = if (isTracked) BluePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Yarışma Başlığı
            Text(
                text = competition.getDisplayTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tarih Satırı (Değişiklik varsa özel vurgu ve etiket ile)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (competition.date_changed) {
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                } else Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (competition.date_changed) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = competition.getFormattedDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (competition.date_changed) FontWeight.Bold else FontWeight.Medium,
                    color = if (competition.date_changed) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                )
                if (competition.date_changed) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFD97706), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Tarih Değişti",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Konum Satırı (Değişiklik varsa özel vurgu ve etiket ile)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (competition.location_changed) {
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEDD5), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                } else Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (competition.location_changed) Color(0xFFEA580C) else Color(0xFFE11D48),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = competition.getDisplayLocation(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (competition.location_changed) FontWeight.Bold else FontWeight.Normal,
                    color = if (competition.location_changed) Color(0xFF9A3412) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                if (competition.location_changed) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEA580C), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Konum Değişti",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

