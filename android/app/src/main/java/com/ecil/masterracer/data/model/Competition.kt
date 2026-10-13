package com.ecil.masterracer.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Keep
data class Competition(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("country") @set:PropertyName("country")
    var country: String = "",

    @get:PropertyName("country_name") @set:PropertyName("country_name")
    var country_name: String = "",

    @get:PropertyName("city") @set:PropertyName("city")
    var city: String = "",

    @get:PropertyName("start_date") @set:PropertyName("start_date")
    var start_date: String = "", // Format: YYYY-MM-DD

    @get:PropertyName("end_date") @set:PropertyName("end_date")
    var end_date: String = "",

    @get:PropertyName("venue_type") @set:PropertyName("venue_type")
    var venue_type: String = "outdoor", // indoor, outdoor, road, cross

    @get:PropertyName("categories") @set:PropertyName("categories")
    var categories: List<String> = emptyList(),

    @get:PropertyName("scope") @set:PropertyName("scope")
    var scope: String = "regional", // turkey, balkan, europe, world, regional

    @get:PropertyName("federation") @set:PropertyName("federation")
    var federation: String = "",

    @get:PropertyName("min_age") @set:PropertyName("min_age")
    var min_age: Int = 35,

    @get:PropertyName("age_category") @set:PropertyName("age_category")
    var age_category: String = "35+",

    @get:PropertyName("regulations_url") @set:PropertyName("regulations_url")
    var regulations_url: String? = null,

    @get:PropertyName("website_url") @set:PropertyName("website_url")
    var website_url: String? = null,

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "scheduled",

    @get:PropertyName("is_new") @set:PropertyName("is_new")
    var is_new: Boolean = false,

    @get:PropertyName("date_changed") @set:PropertyName("date_changed")
    var date_changed: Boolean = false,

    @get:PropertyName("location_changed") @set:PropertyName("location_changed")
    var location_changed: Boolean = false,

    @get:PropertyName("old_start_date") @set:PropertyName("old_start_date")
    var old_start_date: String? = null,

    @get:PropertyName("old_location") @set:PropertyName("old_location")
    var old_location: String? = null
) {
    /**
     * Tesis türüne göre doğrulanmış güncel branş listesini döndürür.
     * Yol koşusu veya kros yarışmalarında sprint, engel, atlama, atma bulunamaz; yalnızca Uzun Mesafe geçerlidir.
     */
    fun getEffectiveCategories(): List<String> {
        val vt = venue_type.lowercase()
        if (vt == "road" || vt == "cross") {
            return listOf("Uzun Mesafe")
        }
        return if (categories.isNotEmpty()) categories else listOf("Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe")
    }

    /**
     * Yarışmanın 35+ Master/Veteran atletizm kriterlerine tam uygunluğunu doğrular.
     * Almanya (DE) için sadece 'Meisterschaften' veya 'Meeting' olup 'Senioren'/'Master' içermeyen genel yarışmaları eler.
     * Yol maratonları veya 35+ kategorisi olan şampiyonalar geçerlidir.
     */
    fun isStrictMasterAthletics(): Boolean {
        if (country.equals("DE", ignoreCase = true)) {
            val fullText = "$title $federation".lowercase()
            val hasGermanMasterKeyword = listOf("senioren", "master", "35+", "m35", "w35").any { fullText.contains(it) }
            val isRoadMarathon = venue_type.equals("road", ignoreCase = true) || fullText.contains("marathon") || fullText.contains("maraton")
            // Almanya'da ya açıkça Senioren/Master yarışması olmalı ya da Master yaş kategorisi içeren maraton olmalı
            return hasGermanMasterKeyword || isRoadMarathon
        }
        return true
    }

    /**
     * Bugün ile başlangıç tarihi arasındaki gün farkını hesaplar.
     */
    fun getDaysRemaining(): Long {
        if (start_date.isBlank()) return 0
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetDate = sdf.parse(start_date) ?: return 0
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val diffMillis = targetDate.time - today.time
            TimeUnit.MILLISECONDS.toDays(diffMillis)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Tarih aralığını kullanıcı dostu formata dönüştürür.
     */
    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale("tr"))

            val startDateParsed = inputFormat.parse(start_date)
            if (end_date.isNotBlank() && end_date != start_date) {
                val endDateParsed = inputFormat.parse(end_date)
                val startDay = SimpleDateFormat("d", Locale("tr")).format(startDateParsed ?: Date())
                val endFormatted = outputFormat.format(endDateParsed ?: Date())
                "$startDay - $endFormatted"
            } else {
                outputFormat.format(startDateParsed ?: Date())
            }
        } catch (e: Exception) {
            "$start_date ~ $end_date"
        }
    }

    /**
     * Tesis türü Türkçe etiketi.
     */
    fun getVenueTitle(): String {
        return when (venue_type.lowercase()) {
            "indoor" -> "Salon (Indoor)"
            "outdoor" -> "Pist (Açık Saha)"
            "road" -> "Yol Koşusu / Maraton"
            "cross" -> "Kros (Arazi)"
            else -> "Açık Saha"
        }
    }

    /**
     * Kapsam Türkçe etiketi.
     */
    fun getScopeTitle(): String {
        return when (scope.lowercase()) {
            "turkey" -> "Türkiye Şampiyonası"
            "balkan" -> "Balkan Şampiyonası"
            "europe" -> "Avrupa Şampiyonası"
            "world" -> "Dünya Şampiyonası"
            else -> "Bölgesel / Özel Yarışma"
        }
    }

    /**
     * Ekranda ve filtrelemede kullanılacak ülke kodu (Kansai -> JP).
     */
    fun getDisplayCountryCode(): String {
        val titleLower = title.lowercase()
        val cityLower = city.lowercase()
        val cNameLower = country_name.lowercase()
        if (titleLower.contains("kansai") || titleLower.contains("world masters games") || titleLower.contains("wmg") ||
            cityLower.contains("kansai") || cNameLower.contains("kansai")
        ) {
            return "JP"
        }
        return country.ifBlank { "TR" }
    }

    /**
     * Ekranda gösterilecek ülke ismi (World Masters Games için Japonya önceliklendirmesi).
     */
    fun getDisplayCountryName(): String {
        val titleLower = title.lowercase()
        val cityLower = city.lowercase()
        val cNameLower = country_name.lowercase()
        if (titleLower.contains("kansai") || titleLower.contains("world masters games") || titleLower.contains("wmg") ||
            cityLower.contains("kansai") || cNameLower.contains("kansai")
        ) {
            return "Japonya"
        }
        return country_name.ifBlank { country }
    }

    /**
     * Ekranda gösterilecek konum / şehir.
     */
    fun getDisplayLocation(): String {
        val cName = getDisplayCountryName()
        val titleLower = title.lowercase()
        val isKansai = titleLower.contains("kansai") || titleLower.contains("world masters games") || titleLower.contains("wmg") || city.lowercase().contains("kansai")
        val cCity = if (isKansai) {
            "Kansai"
        } else {
            city
        }
        return if (cCity.isNotBlank()) "$cCity, $cName" else cName
    }

    /**
     * Ülke Bayrak Emojisi.
     */
    fun getCountryFlag(): String {
        val cUpper = country.uppercase()
        val titleLower = title.lowercase()
        if (cUpper == "JP" || cUpper == "JPN" ||
            titleLower.contains("kansai") ||
            titleLower.contains("japan") ||
            titleLower.contains("japonya") ||
            titleLower.contains("world masters games") ||
            titleLower.contains("wmg")
        ) {
            return "🇯🇵"
        }
        return when (cUpper) {
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
            "INT" -> "🌍"
            else -> "🏃"
        }
    }
}

