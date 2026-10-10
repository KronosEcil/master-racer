package com.ecil.masterracer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecil.masterracer.ui.theme.AmberWarning
import com.ecil.masterracer.ui.theme.CrimsonAccent
import com.ecil.masterracer.ui.theme.EmeraldGreen

@Composable
fun CountdownBadge(
    daysRemaining: Long,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon, label) = when {
        daysRemaining < 0 -> {
            Tuple4(
                Color(0xFFE2E8F0),
                Color(0xFF64748B),
                Icons.Default.CheckCircle,
                "Tamamlandı"
            )
        }
        daysRemaining == 0L -> {
            Tuple4(
                CrimsonAccent.copy(alpha = 0.15f),
                CrimsonAccent,
                Icons.Default.Whatshot,
                "BUGÜN BAŞLIYOR!"
            )
        }
        daysRemaining <= 7 -> {
            Tuple4(
                CrimsonAccent.copy(alpha = 0.12f),
                CrimsonAccent,
                Icons.Default.Alarm,
                "$daysRemaining gün kaldı"
            )
        }
        daysRemaining <= 30 -> {
            Tuple4(
                AmberWarning.copy(alpha = 0.15f),
                Color(0xFFB45309),
                Icons.Default.Alarm,
                "$daysRemaining gün kaldı"
            )
        }
        else -> {
            Tuple4(
                EmeraldGreen.copy(alpha = 0.15f),
                EmeraldGreen,
                Icons.Default.Alarm,
                "$daysRemaining gün kaldı"
            )
        }
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

