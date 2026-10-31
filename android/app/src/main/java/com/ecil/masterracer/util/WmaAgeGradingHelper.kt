package com.ecil.masterracer.util

import androidx.compose.ui.graphics.Color
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * WMA (World Masters Athletics) Yaş Faktörü (Age Grading) ve Performans Hesaplayıcı.
 *
 * Farklı yaş ve cinsiyetteki master atletlerin derecelerini Open Senior (Açık Yaş Elit 20-30 yaş)
 * seviyesine normalize eder ve Dünya Klasmanı performans yüzdesini (% WMA) hesaplar.
 */
data class WmaEvaluationResult(
    val eventName: String,
    val gender: String,
    val age: Int,
    val actualValue: Double,            // Koşularda saniye, sahada metre
    val formattedActual: String,
    val ageGradedValue: Double,         // Açık yaş karşılığı (saniye veya metre)
    val formattedEquivalent: String,    // Örn: "10.62 sn" veya "7.42 m"
    val ageFactor: Double,              // Katsayı (örn: 0.8872 veya 1.265)
    val scorePercentage: Double,        // WMA % (örn: 87.42)
    val classificationTitle: String,    // "Ulusal Klasman (National Class)"
    val classificationLevel: String,    // "National"
    val badgeColorHex: Long             // Renk kodu
)

object WmaAgeGradingHelper {

    // Dünya Açık Yaş Standartları (Erkek / Kadın) - Open Senior Benchmark
    // Koşularda saniye, atlama/atmalarda metre
    private val OPEN_STANDARDS = mapOf(
        // Koşular (saniye)
        "60m" to Pair(6.34, 6.92),
        "100m" to Pair(9.58, 10.49),
        "200m" to Pair(19.19, 21.34),
        "400m" to Pair(43.03, 47.60),
        "800m" to Pair(100.91, 113.28),
        "1500m" to Pair(206.00, 229.11),
        "3000m" to Pair(440.67, 486.11),
        "5000m" to Pair(755.36, 840.21),
        "10000m" to Pair(1571.00, 1741.03),
        "half_marathon" to Pair(3451.0, 3772.0),
        "marathon" to Pair(7235.0, 7913.0),
        // Saha Branşları (metre)
        "long_jump" to Pair(8.95, 7.52),
        "triple_jump" to Pair(18.29, 15.67),
        "high_jump" to Pair(2.45, 2.09),
        "pole_vault" to Pair(6.26, 5.06),
        "shot_put" to Pair(23.56, 22.63),
        "discus" to Pair(74.35, 76.80),
        "javelin" to Pair(98.48, 72.28),
        "hammer" to Pair(86.74, 82.98)
    )

    data class EventConfig(
        val key: String,
        val displayName: String,
        val isField: Boolean,
        val isTime: Boolean,
        val defaultDistanceMeters: Double = 0.0
    )

    val SUPPORTED_EVENTS = listOf(
        EventConfig("60m", "60 Metre (Salon)", isField = false, isTime = true),
        EventConfig("100m", "100 Metre", isField = false, isTime = true),
        EventConfig("200m", "200 Metre", isField = false, isTime = true),
        EventConfig("400m", "400 Metre", isField = false, isTime = true),
        EventConfig("800m", "800 Metre", isField = false, isTime = true),
        EventConfig("1500m", "1500 Metre", isField = false, isTime = true),
        EventConfig("3000m", "3000 Metre", isField = false, isTime = true),
        EventConfig("5000m", "5000 Metre", isField = false, isTime = true),
        EventConfig("10000m", "10.000 Metre", isField = false, isTime = true),
        EventConfig("half_marathon", "Yarı Maraton (21.1 km)", isField = false, isTime = true),
        EventConfig("marathon", "Maraton (42.2 km)", isField = false, isTime = true),
        EventConfig("long_jump", "Uzun Atlama", isField = true, isTime = false),
        EventConfig("triple_jump", "Üçadım Atlama", isField = true, isTime = false),
        EventConfig("high_jump", "Yüksek Atlama", isField = true, isTime = false),
        EventConfig("pole_vault", "Sırıkla Atlama", isField = true, isTime = false),
        EventConfig("shot_put", "Gülle Atma", isField = true, isTime = false),
        EventConfig("discus", "Disk Atma", isField = true, isTime = false),
        EventConfig("javelin", "Cirit Atma", isField = true, isTime = false),
        EventConfig("hammer", "Çekiç Atma", isField = true, isTime = false)
    )

    fun findEventConfig(eventName: String): EventConfig? {
        val lower = eventName.lowercase(Locale.ROOT)
        return when {
            lower.contains("60m") || lower.contains("60 metre") -> SUPPORTED_EVENTS.find { it.key == "60m" }
            lower.contains("100m") || lower.contains("100 metre") -> SUPPORTED_EVENTS.find { it.key == "100m" }
            lower.contains("200m") || lower.contains("200 metre") -> SUPPORTED_EVENTS.find { it.key == "200m" }
            lower.contains("400m") || lower.contains("400 metre") -> SUPPORTED_EVENTS.find { it.key == "400m" }
            lower.contains("800m") || lower.contains("800 metre") -> SUPPORTED_EVENTS.find { it.key == "800m" }
            lower.contains("1500m") || lower.contains("1500 metre") -> SUPPORTED_EVENTS.find { it.key == "1500m" }
            lower.contains("3000m") || lower.contains("3000 metre") -> SUPPORTED_EVENTS.find { it.key == "3000m" }
            lower.contains("5000m") || lower.contains("5000 metre") || lower.contains("5k") -> SUPPORTED_EVENTS.find { it.key == "5000m" }
            lower.contains("10000m") || lower.contains("10.000") || lower.contains("10k") -> SUPPORTED_EVENTS.find { it.key == "10000m" }
            lower.contains("yarı maraton") || lower.contains("half marathon") -> SUPPORTED_EVENTS.find { it.key == "half_marathon" }
            lower.contains("maraton") -> SUPPORTED_EVENTS.find { it.key == "marathon" }
            lower.contains("üçadım") || lower.contains("üç adım") || lower.contains("triple") -> SUPPORTED_EVENTS.find { it.key == "triple_jump" }
            lower.contains("uzun") || lower.contains("long jump") -> SUPPORTED_EVENTS.find { it.key == "long_jump" }
            lower.contains("yüksek") || lower.contains("high jump") -> SUPPORTED_EVENTS.find { it.key == "high_jump" }
            lower.contains("sırık") || lower.contains("pole vault") -> SUPPORTED_EVENTS.find { it.key == "pole_vault" }
            lower.contains("gülle") || lower.contains("shot put") -> SUPPORTED_EVENTS.find { it.key == "shot_put" }
            lower.contains("disk") || lower.contains("discus") -> SUPPORTED_EVENTS.find { it.key == "discus" }
            lower.contains("cirit") || lower.contains("javelin") -> SUPPORTED_EVENTS.find { it.key == "javelin" }
            lower.contains("çekiç") || lower.contains("hammer") -> SUPPORTED_EVENTS.find { it.key == "hammer" }
            else -> null
        }
    }

    /**
     * WMA Yaş Faktörü Katsayısını hesaplar.
     * Koşular için factor <= 1.0 (süre çarpılır: AgeGradedTime = Time * Factor)
     * Saha için factor >= 1.0 (mesafe çarpılır: AgeGradedDist = Dist * Factor)
     */
    fun getAgeFactor(gender: String, age: Int, eventKey: String): Double {
        val safeAge = min(100, max(30, age))
        val isFemale = gender.equals("F", ignoreCase = true) ||
                gender.equals("W", ignoreCase = true) ||
                gender.equals("K", ignoreCase = true)

        if (safeAge <= 30) return 1.0

        val config = SUPPORTED_EVENTS.find { it.key == eventKey } ?: SUPPORTED_EVENTS[1] // default 100m
        val deltaYears = (safeAge - 30).toDouble()

        return if (!config.isField) {
            // Koşu katsayısı (azalan eğri, 1.0'dan başlar 80 yaşında ~0.60'a iner)
            // Sprintler (60m, 100m, 200m) ve Uzun Koşular (5000m, maraton) hafif farklı eğrilere sahiptir.
            val decayRate = when (eventKey) {
                "60m", "100m" -> if (isFemale) 0.0075 else 0.0070
                "200m", "400m" -> if (isFemale) 0.0080 else 0.0074
                "800m", "1500m" -> if (isFemale) 0.0085 else 0.0079
                "3000m", "5000m" -> if (isFemale) 0.0088 else 0.0082
                "10000m", "half_marathon", "marathon" -> if (isFemale) 0.0092 else 0.0085
                else -> 0.0080
            }

            // Yaş ilerledikçe quadratik hızlanma (60 yaş sonrası düşüş hızlanır)
            val quadFactor = if (safeAge > 55) (safeAge - 55) * 0.00015 else 0.0
            val factor = 1.0 - (deltaYears * decayRate) - quadFactor
            max(0.35, min(1.0, factor))
        } else {
            // Saha katsayısı (artan çarpan, 1.0'dan başlar 80 yaşında ~2.10'a çıkar)
            val increaseRate = when (eventKey) {
                "long_jump", "triple_jump" -> if (isFemale) 0.018 else 0.016
                "high_jump", "pole_vault" -> if (isFemale) 0.017 else 0.015
                "shot_put", "discus" -> if (isFemale) 0.015 else 0.014
                "javelin", "hammer" -> if (isFemale) 0.016 else 0.015
                else -> 0.016
            }
            val quadFactor = if (safeAge > 55) (safeAge - 55) * 0.0004 else 0.0
            val factor = 1.0 + (deltaYears * increaseRate) + quadFactor
            max(1.0, min(3.5, factor))
        }
    }

    /**
     * WMA Yaş Derecesi & Açık Yaş Karşılığı Değerlendirmesi
     */
    fun evaluatePerformance(
        gender: String,
        age: Int,
        eventKey: String,
        numericValue: Double
    ): WmaEvaluationResult? {
        if (numericValue <= 0.0) return null

        val config = SUPPORTED_EVENTS.find { it.key == eventKey } ?: return null
        val stdPair = OPEN_STANDARDS[eventKey] ?: return null
        val isFemale = gender.equals("F", ignoreCase = true) ||
                gender.equals("W", ignoreCase = true) ||
                gender.equals("K", ignoreCase = true)
        val openStandard = if (isFemale) stdPair.second else stdPair.first

        val factor = getAgeFactor(gender, age, eventKey)

        val (ageGradedValue, percentage) = if (!config.isField) {
            // Koşu: Saniye * Katsayı = Açık Yaş Dengi
            val equivTime = numericValue * factor
            val pct = (openStandard / equivTime) * 100.0
            Pair(equivTime, pct)
        } else {
            // Saha: Metre * Katsayı = Açık Yaş Dengi
            val equivDist = numericValue * factor
            val pct = (equivDist / openStandard) * 100.0
            Pair(equivDist, pct)
        }

        val clampedPct = min(110.0, max(10.0, percentage))

        val (title, level, colorHex) = when {
            clampedPct >= 90.0 -> Triple("🌟 Dünya Klasmanı (World Class)", "World", 0xFFEAB308) // Gold
            clampedPct >= 80.0 -> Triple("🥇 Ulusal Klasman (National Class)", "National", 0xFF2563EB) // Blue
            clampedPct >= 70.0 -> Triple("🥈 Bölgesel Klasman (Regional Class)", "Regional", 0xFF10B981) // Emerald
            clampedPct >= 60.0 -> Triple("🥉 Yerel Seviye (Local Class)", "Local", 0xFF8B5CF6) // Purple
            else -> Triple("🏃 Gelişmekte Olan (Developing)", "Developing", 0xFF64748B) // Slate
        }

        val formattedActual = if (!config.isField) formatDuration(numericValue) else String.format(Locale.US, "%.2f m", numericValue)
        val formattedEquiv = if (!config.isField) formatDuration(ageGradedValue) else String.format(Locale.US, "%.2f m", ageGradedValue)

        return WmaEvaluationResult(
            eventName = config.displayName,
            gender = if (isFemale) "Kadın" else "Erkek",
            age = age,
            actualValue = numericValue,
            formattedActual = formattedActual,
            ageGradedValue = ageGradedValue,
            formattedEquivalent = formattedEquiv,
            ageFactor = factor,
            scorePercentage = clampedPct,
            classificationTitle = title,
            classificationLevel = level,
            badgeColorHex = colorHex
        )
    }

    fun formatDuration(totalSeconds: Double): String {
        return when {
            totalSeconds >= 3600 -> {
                val hours = (totalSeconds / 3600).toInt()
                val rem = totalSeconds % 3600
                val mins = (rem / 60).toInt()
                val secs = rem % 60
                String.format(Locale.US, "%d:%02d:%05.2f", hours, mins, secs)
            }
            totalSeconds >= 60 -> {
                val mins = (totalSeconds / 60).toInt()
                val secs = totalSeconds % 60
                String.format(Locale.US, "%d:%05.2f", mins, secs)
            }
            else -> {
                String.format(Locale.US, "%.2f sn", totalSeconds)
            }
        }
    }

    /**
     * Kullanıcının girdiği derece metnini (Örn: "11.84", "1:24.15", "5.82") sayısal değere çevirir.
     */
    fun parseInputToNumeric(text: String, isField: Boolean): Double? {
        val clean = text.trim().replace(",", ".")
        if (clean.isBlank()) return null

        if (isField) {
            val num = clean.replace("m", "").replace("M", "").trim()
            return num.toDoubleOrNull()
        } else {
            // Koşu formatları: "11.84", "1:52.34", "1:24:15"
            val parts = clean.split(":")
            return try {
                when (parts.size) {
                    1 -> parts[0].toDoubleOrNull()
                    2 -> {
                        val mins = parts[0].toDouble()
                        val secs = parts[1].toDouble()
                        (mins * 60) + secs
                    }
                    3 -> {
                        val hours = parts[0].toDouble()
                        val mins = parts[1].toDouble()
                        val secs = parts[2].toDouble()
                        (hours * 3600) + (mins * 60) + secs
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
