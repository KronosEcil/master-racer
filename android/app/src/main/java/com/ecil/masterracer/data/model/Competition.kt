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
     * Ekranda ve filtrelemede kullanılacak standart 2/3 harfli ülke kodu.
     */
    fun getDisplayCountryCode(): String {
        val titleLower = title.lowercase()
        val cityLower = city.lowercase()
        val cNameLower = country_name.lowercase()
        val cUpper = country.uppercase()

        if (titleLower.contains("kansai") || titleLower.contains("world masters games") || titleLower.contains("wmg") ||
            cityLower.contains("kansai") || cNameLower.contains("kansai") || cUpper == "JP" || cUpper == "JPN"
        ) {
            return "JP"
        }

        if (cUpper == "AL" || cUpper == "ALB" || cNameLower.contains("albania") || cNameLower.contains("arnavutluk") ||
            titleLower.contains("albania") || titleLower.contains("arnavutluk") || cityLower.contains("shkod") || cityLower.contains("tiran")
        ) {
            return "AL"
        }

        val resolvedFromText = when {
            cNameLower.contains("portekiz") || cNameLower.contains("portugal") || cityLower.contains("lisboa") || cityLower.contains("lizbon") || cityLower.contains("porto") || titleLower.contains("portug") -> "PT"
            cNameLower.contains("çek") || cNameLower.contains("czech") || cityLower.contains("janske") || cityLower.contains("prag") -> "CZ"
            cNameLower.contains("sloven") || cityLower.contains("domzale") || cityLower.contains("novo mesto") || cityLower.contains("ljubljana") -> "SI"
            cNameLower.contains("norve") || cNameLower.contains("norway") || cityLower.contains("oslo") -> "NO"
            cNameLower.contains("ispan") || cNameLower.contains("spain") || cityLower.contains("madrid") || cityLower.contains("barcelona") -> "ES"
            cNameLower.contains("ital") || cityLower.contains("catania") || cityLower.contains("roma") -> "IT"
            cNameLower.contains("alman") || cNameLower.contains("german") || cityLower.contains("berlin") || cityLower.contains("münchen") -> "DE"
            cNameLower.contains("türk") || cNameLower.contains("turk") || cityLower.contains("izmir") || cityLower.contains("istanbul") -> "TR"
            cNameLower.contains("hırvat") || cNameLower.contains("croat") || cityLower.contains("zagreb") -> "HR"
            cNameLower.contains("finland") || cityLower.contains("jyvaskyla") || cityLower.contains("tampere") -> "FIN"
            cNameLower.contains("isviçre") || cNameLower.contains("switz") || cityLower.contains("lucerne") -> "CHE"
            cNameLower.contains("irlanda") || cNameLower.contains("ireland") || cityLower.contains("galway") -> "IRL"
            cNameLower.contains("fransa") || cNameLower.contains("france") || cityLower.contains("paris") -> "FR"
            cNameLower.contains("romanya") || cNameLower.contains("romania") || cityLower.contains("craiova") -> "RO"
            cNameLower.contains("polonya") || cNameLower.contains("poland") || cityLower.contains("torun") -> "PL"
            cNameLower.contains("isveç") || cNameLower.contains("sweden") || cityLower.contains("gothenburg") -> "SE"
            cNameLower.contains("hollanda") || cNameLower.contains("netherland") || cityLower.contains("amsterdam") -> "NL"
            cNameLower.contains("yunan") || cNameLower.contains("greece") || cityLower.contains("atina") -> "GR"
            cNameLower.contains("avusturya") || cNameLower.contains("austria") || cityLower.contains("wien") || cityLower.contains("viyana") -> "AT"
            cNameLower.contains("belçika") || cNameLower.contains("belcika") || cNameLower.contains("belgium") || cityLower.contains("brussel") || cityLower.contains("brüksel") -> "BE"
            cNameLower.contains("danimarka") || cNameLower.contains("denmark") || cityLower.contains("kopenhag") -> "DK"
            cNameLower.contains("macar") || cNameLower.contains("hungar") || cityLower.contains("budapest") -> "HU"
            cNameLower.contains("slovak") || cityLower.contains("bratislava") -> "SK"
            cNameLower.contains("eston") || cityLower.contains("tallinn") -> "EE"
            cNameLower.contains("leton") || cNameLower.contains("latvi") || cityLower.contains("riga") -> "LV"
            cNameLower.contains("litvan") || cNameLower.contains("lithuan") || cityLower.contains("vilnius") -> "LT"
            else -> ""
        }
        if (resolvedFromText.isNotEmpty() && (cUpper == "EU" || cUpper == "INT" || cUpper.isBlank())) {
            return resolvedFromText
        }

        return when (cUpper) {
            "TUR", "TR" -> "TR"
            "GRE", "GR" -> "GR"
            "GER", "DE" -> "DE"
            "ITA", "IT" -> "IT"
            "GBR", "UK", "GB" -> "GB"
            "ESP", "ES" -> "ES"
            "FRA", "FR" -> "FR"
            "POL", "PL" -> "PL"
            "BUL", "BG" -> "BG"
            "ROU", "RO" -> "RO"
            "SRB", "RS" -> "RS"
            "SWE", "SE" -> "SE"
            "NED", "NL" -> "NL"
            "POR", "PT" -> "PT"
            "KOR" -> "KOR"
            "USA" -> "USA"
            "FIN" -> "FIN"
            "CHE", "CH" -> "CH"
            "IRL", "IE" -> "IE"
            "SVN", "SI" -> "SI"
            "ALB", "AL" -> "AL"
            "CZE", "CZ" -> "CZ"
            "NOR", "NO" -> "NO"
            "DNK", "DK" -> "DK"
            "ISL", "IS" -> "IS"
            "LVA", "LV" -> "LV"
            "LTU", "LT" -> "LT"
            "HRV", "HR" -> "HR"
            "AUT", "AT" -> "AT"
            "BEL", "BE" -> "BE"
            "EST", "EE" -> "EE"
            else -> if (resolvedFromText.isNotEmpty()) resolvedFromText else country.ifBlank { "TR" }
        }
    }

    /**
     * Ekranda gösterilecek standart Türkçe ülke ismi.
     */
    fun getDisplayCountryName(): String {
        val code = getDisplayCountryCode().uppercase()
        val cNameLower = country_name.lowercase()

        if (code == "AL" || cNameLower.contains("albania") || cNameLower.contains("arnavutluk")) {
            return "Arnavutluk"
        }
        if (code == "JP" || cNameLower.contains("japan") || cNameLower.contains("japonya") || cNameLower.contains("kansai")) {
            return "Japonya"
        }
        if (code == "TR" || cNameLower.contains("turkey") || cNameLower.contains("türkiye")) return "Türkiye"
        if (code == "DE" || cNameLower.contains("germany") || cNameLower.contains("almanya")) return "Almanya"
        if (code == "FR" || cNameLower.contains("france") || cNameLower.contains("fransa")) return "Fransa"
        if (code == "GR" || cNameLower.contains("greece") || cNameLower.contains("yunanistan")) return "Yunanistan"
        if (code == "IT" || cNameLower.contains("italy") || cNameLower.contains("italya")) return "İtalya"
        if (code == "ES" || cNameLower.contains("spain") || cNameLower.contains("ispanya")) return "İspanya"
        if (code == "GB" || cNameLower.contains("united kingdom") || cNameLower.contains("great britain") || cNameLower.contains("birleşik krallık")) return "Birleşik Krallık"
        if (code == "PL" || cNameLower.contains("poland") || cNameLower.contains("polonya")) return "Polonya"
        if (code == "BG" || cNameLower.contains("bulgaria") || cNameLower.contains("bulgaristan")) return "Bulgaristan"
        if (code == "RO" || cNameLower.contains("romania") || cNameLower.contains("romanya")) return "Romanya"
        if (code == "RS" || cNameLower.contains("serbia") || cNameLower.contains("sırbistan")) return "Sırbistan"
        if (code == "SE" || cNameLower.contains("sweden") || cNameLower.contains("isveç")) return "İsveç"
        if (code == "NL" || cNameLower.contains("netherlands") || cNameLower.contains("hollanda")) return "Hollanda"
        if (code == "PT" || cNameLower.contains("portugal") || cNameLower.contains("portekiz")) return "Portekiz"
        if (code == "KOR" || cNameLower.contains("korea") || cNameLower.contains("güney kore")) return "Güney Kore"
        if (code == "USA" || cNameLower.contains("united states") || cNameLower.contains("abd")) return "ABD"
        if (code == "FIN" || cNameLower.contains("finland") || cNameLower.contains("finlandiya")) return "Finlandiya"
        if (code == "CH" || cNameLower.contains("switzerland") || cNameLower.contains("isviçre")) return "İsviçre"
        if (code == "IE" || cNameLower.contains("ireland") || cNameLower.contains("irlanda")) return "İrlanda"
        if (code == "SI" || cNameLower.contains("slovenia") || cNameLower.contains("slovenya")) return "Slovenya"
        if (code == "CZ" || cNameLower.contains("czech") || cNameLower.contains("çekya")) return "Çekya"
        if (code == "NO" || cNameLower.contains("norway") || cNameLower.contains("norveç")) return "Norveç"
        if (code == "DK" || cNameLower.contains("denmark") || cNameLower.contains("danimarka")) return "Danimarka"
        if (code == "IS" || cNameLower.contains("iceland") || cNameLower.contains("izlanda")) return "İzlanda"
        if (code == "LV" || cNameLower.contains("latvia") || cNameLower.contains("letonya")) return "Letonya"
        if (code == "LT" || cNameLower.contains("lithuania") || cNameLower.contains("litvanya")) return "Litvanya"
        if (code == "HR" || cNameLower.contains("croatia") || cNameLower.contains("hırvatistan")) return "Hırvatistan"
        if (code == "AT" || cNameLower.contains("austria") || cNameLower.contains("avusturya")) return "Avusturya"
        if (code == "BE" || cNameLower.contains("belgium") || cNameLower.contains("belçika")) return "Belçika"
        if (code == "EE" || cNameLower.contains("estonia") || cNameLower.contains("estonya")) return "Estonya"

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
        val cCode = getDisplayCountryCode().uppercase()
        return when (cCode) {
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
            "CH", "CHE" -> "🇨🇭"
            "IE", "IRL" -> "🇮🇪"
            "SI", "SVN" -> "🇸🇮"
            "AL", "ALB" -> "🇦🇱"
            "CZ", "CZE" -> "🇨🇿"
            "NO", "NOR" -> "🇳🇴"
            "DK", "DNK" -> "🇩🇰"
            "IS", "ISL" -> "🇮🇸"
            "LV", "LVA" -> "🇱🇻"
            "LT", "LTU" -> "🇱🇹"
            "HR", "HRV" -> "🇭🇷"
            "AT", "AUT" -> "🇦🇹"
            "BE", "BEL" -> "🇧🇪"
            "EE", "EST" -> "🇪🇪"
            "EU" -> "🇪🇺"
            "INT" -> "🌍"
            else -> "🏃"
        }
    }
}

