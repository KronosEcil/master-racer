package com.ecil.masterracer.util

import androidx.annotation.Keep
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

@Keep
data class AthleteRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val eventName: String,               // "100 Metre", "110m Engelli", "Uzun Atlama"
    val category: String,                // "M35", "M40", "F50" vb.
    val result: String,                  // "11.84 sn", "1:24:15", "5.84 m"
    val year: Int,                       // 2026
    val competitionTitle: String = "",   // "Craiova BAMACS 2026" veya "Bireysel Derece"
    val notes: String = ""               // Rüzgar (+1.2), Madalya (Altın) vb.
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("eventName", eventName)
            put("category", category)
            put("result", result)
            put("year", year)
            put("competitionTitle", competitionTitle)
            put("notes", notes)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AthleteRecord {
            return AthleteRecord(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                eventName = json.optString("eventName", ""),
                category = json.optString("category", ""),
                result = json.optString("result", ""),
                year = json.optInt("year", Calendar.getInstance().get(Calendar.YEAR)),
                competitionTitle = json.optString("competitionTitle", ""),
                notes = json.optString("notes", "")
            )
        }
    }
}

object MasterAthleticsHelper {

    /**
     * WMA (World Masters Athletics) standart yaş kategorisi hesaplayıcı.
     * Erkek: M35, M40, M45, M50 ...
     * Kadın: F35, F40, F45, F50 ... (WMA standardı W veya F)
     */
    fun calculateCategory(
        gender: String,
        birthYear: Int,
        targetYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): String {
        val isFemale = gender.equals("F", ignoreCase = true) ||
                gender.equals("W", ignoreCase = true) ||
                gender.equals("K", ignoreCase = true)
        val prefix = if (isFemale) "F" else "M"

        if (birthYear <= 1900 || birthYear > targetYear) {
            return "${prefix}35"
        }

        val age = targetYear - birthYear
        return if (age >= 35) {
            val bracket = (age / 5) * 5
            "$prefix$bracket"
        } else {
            // 35 yaş altı kıdemli/büyükler
            "${prefix}30"
        }
    }

    /**
     * WMA kurallarına göre sporcunun cinsiyetine ve yaşına uygun atletizm branşları.
     * Yaş ilerledikçe engelli mesafeleri ve atma ağırlıkları WMA kurallarına göre otomatik değişir:
     *
     * Kısa Engelli:
     * - Erkek 35-49: 110m Engelli
     * - Erkek 50-69: 100m Engelli
     * - Erkek 70+:   80m Engelli
     * - Kadın 35-39: 100m Engelli
     * - Kadın 40+:   80m Engelli
     *
     * Uzun Engelli:
     * - Erkek 35-59: 400m Engelli
     * - Erkek 60-79: 300m Engelli
     * - Erkek 80+:   200m Engelli
     * - Kadın 35-49: 400m Engelli
     * - Kadın 50-69: 300m Engelli
     * - Kadın 70+:   200m Engelli
     *
     * Su Engelli (Steeplechase):
     * - Erkek 35-59: 3000m Su Engelli
     * - Erkek 60+:   2000m Su Engelli
     * - Kadın Tüm Yaşlar: 2000m Su Engelli
     */
    fun getApplicableEvents(
        gender: String,
        birthYear: Int,
        targetYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): List<String> {
        val isFemale = gender.equals("F", ignoreCase = true) ||
                gender.equals("W", ignoreCase = true) ||
                gender.equals("K", ignoreCase = true)
        val age = if (birthYear in 1910..targetYear) targetYear - birthYear else 35

        val events = mutableListOf<String>()

        // 1. Düz Sprintler
        events.add("60 Metre (Salon)")
        events.add("100 Metre")
        events.add("200 Metre")
        events.add("400 Metre")

        // 2. Kısa Engelli (WMA Yaşa ve Cinsiyete Göre Dinamik)
        events.add("60m Engelli (Salon)")
        if (isFemale) {
            if (age < 40) {
                events.add("100m Engelli (0.84m)")
            } else {
                events.add("80m Engelli (0.76m)")
            }
        } else {
            when {
                age < 50 -> events.add("110m Engelli (0.99m)")
                age < 70 -> events.add("100m Engelli (0.91m/0.84m)")
                else -> events.add("80m Engelli (0.76m/0.69m)")
            }
        }

        // 3. Uzun Engelli (WMA Yaşa ve Cinsiyete Göre Dinamik)
        if (isFemale) {
            when {
                age < 50 -> events.add("400m Engelli (0.76m)")
                age < 70 -> events.add("300m Engelli (0.76m)")
                else -> events.add("200m Engelli (0.69m)")
            }
        } else {
            when {
                age < 60 -> events.add("400m Engelli (0.91m/0.84m)")
                age < 80 -> events.add("300m Engelli (0.76m)")
                else -> events.add("200m Engelli (0.69m)")
            }
        }

        // 4. Su Engelli (Steeplechase)
        if (isFemale || age >= 60) {
            events.add("2000m Su Engelli")
        } else {
            events.add("3000m Su Engelli")
        }

        // 5. Orta ve Uzun Mesafe Koşuları
        events.add("800 Metre")
        events.add("1500 Metre")
        events.add("3000 Metre")
        events.add("5000 Metre")
        events.add("10.000 Metre")

        // 6. Yol Koşuları
        events.add("5K Yol Koşusu")
        events.add("10K Yol Koşusu")
        events.add("Yarı Maraton (21.1K)")
        events.add("Maraton (42.2K)")

        // 7. Atlama Branşları
        events.add("Uzun Atlama")
        events.add("Üçadım Atlama")
        events.add("Yüksek Atlama")
        events.add("Sırıkla Atlama")

        // 8. Atma Branşları (WMA Ağırlık Bilgisiyle)
        val shotWeight = if (isFemale) {
            when {
                age < 50 -> "4kg"
                age < 75 -> "3kg"
                else -> "2kg"
            }
        } else {
            when {
                age < 50 -> "7.26kg"
                age < 60 -> "6kg"
                age < 70 -> "5kg"
                age < 80 -> "4kg"
                else -> "3kg"
            }
        }
        events.add("Gülle Atma ($shotWeight)")

        val discusWeight = if (isFemale) {
            if (age < 75) "1kg" else "0.75kg"
        } else {
            when {
                age < 50 -> "2kg"
                age < 60 -> "1.5kg"
                else -> "1kg"
            }
        }
        events.add("Disk Atma ($discusWeight)")

        val javelinWeight = if (isFemale) {
            when {
                age < 50 -> "600g"
                age < 75 -> "500g"
                else -> "400g"
            }
        } else {
            when {
                age < 50 -> "800g"
                age < 60 -> "700g"
                age < 70 -> "600g"
                age < 80 -> "500g"
                else -> "400g"
            }
        }
        events.add("Cirit Atma ($javelinWeight)")

        val hammerWeight = if (isFemale) {
            when {
                age < 50 -> "4kg"
                age < 75 -> "3kg"
                else -> "2kg"
            }
        } else {
            when {
                age < 50 -> "7.26kg"
                age < 60 -> "6kg"
                age < 70 -> "5kg"
                age < 80 -> "4kg"
                else -> "3kg"
            }
        }
        events.add("Çekiç Atma ($hammerWeight)")
        events.add("Ağırlık Atma (Weight Throw)")

        return events
    }

    /**
     * JSON Array string'den kayıt listesine dönüştürür.
     */
    fun parseRecordsJson(jsonStr: String): List<AthleteRecord> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<AthleteRecord>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(AthleteRecord.fromJson(obj))
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Kayıt listesini JSON Array string formatına dönüştürür.
     */
    fun serializeRecordsJson(records: List<AthleteRecord>): String {
        val array = JSONArray()
        records.forEach { array.put(it.toJson()) }
        return array.toString()
    }
}
