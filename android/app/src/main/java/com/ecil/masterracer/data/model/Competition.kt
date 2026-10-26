package com.ecil.masterracer.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Keep
@IgnoreExtraProperties
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

    @get:PropertyName("scope") @set:PropertyName("scope")
    var scope: String = "regional", // national, balkan, europe, world, ema_league, regional

    @get:PropertyName("federation") @set:PropertyName("federation")
    var federation: String = "",

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
     * Yarışmanın 35+ Master/Veteran atletizm kriterlerine tam uygunluğunu doğrular.
     */
    fun isStrictMasterAthletics(): Boolean {
        if (country.equals("DE", ignoreCase = true)) {
            val fullText = "$title $federation".lowercase()
            val hasGermanMasterKeyword = listOf("senioren", "master", "35+", "m35", "w35").any { fullText.contains(it) }
            val isRoadMarathon = fullText.contains("marathon") || fullText.contains("maraton")
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
     * Yarışmanın standart kategori (kapsam) kodunu döndürür:
     * "national", "world", "balkan", "europe", "ema_league", "regional"
     */
    fun getNormalizedScope(): String {
        val tLow = title.lowercase()
        val sLow = scope.lowercase()
        if (sLow == "ema_league" || tLow.contains("ema league") || tLow.contains("ema ligi") || tLow.contains("meeting europeen") || tLow.contains("meeting européen")) {
            return "ema_league"
        }
        if (sLow == "world" || tLow.contains("world") || tLow.contains("wma") || tLow.contains("wmg") || tLow.contains("kansai")) {
            return "world"
        }
        if (sLow == "balkan" || tLow.contains("balkan") || tLow.contains("bamacs") || tLow.contains("bamaci")) {
            return "balkan"
        }
        if (tLow.contains("emacs") || tLow.contains("emaci") || tLow.contains("emamrc") || tLow.contains("emacns") ||
            tLow.contains("emmtrc") || tLow.contains("emorrc") || tLow.contains("european masters championship") ||
            tLow.contains("european masters athletics championship") || tLow.contains("european masters mountain") ||
            tLow.contains("european masters marathon") || tLow.contains("european masters non-stadia") ||
            tLow.contains("european masters indoor")
        ) {
            return "europe"
        }
        if (tLow.contains("meeting") || tLow.contains("track night") || tLow.contains("grand prix") ||
            tLow.contains("bw-") || tLow.contains("baden") || tLow.contains("norddeutsch") ||
            tLow.contains("westfal") || tLow.contains("bayern") || tLow.contains("hessen") ||
            tLow.contains("rheinland") || tLow.contains("bölge")
        ) {
            return "regional"
        }
        if (tLow.contains("şampiyona") || tLow.contains("championship") || tLow.contains("championnat") ||
            tLow.contains("meisterschaft") || tLow.contains("campeonato") || tLow.contains("campionat") ||
            tLow.contains("mistrzostwa")
        ) {
            return if (sLow == "europe") "europe" else "national"
        }
        return when (sLow) {
            "turkey", "national" -> "national"
            "world" -> "world"
            "balkan" -> "balkan"
            "europe" -> "europe"
            "ema_league" -> "ema_league"
            else -> "regional"
        }
    }

    /**
     * Standart kategori Türkçe başlığı:
     * Ulusal, Dünya, Balkan, Avrupa, EMA Ligi, Bölgesel
     */
    fun getScopeTitle(): String {
        return when (getNormalizedScope()) {
            "national" -> "Ulusal"
            "world" -> "Dünya"
            "balkan" -> "Balkan"
            "europe" -> "Avrupa"
            "ema_league" -> "EMA Ligi"
            else -> "Bölgesel"
        }
    }

    /**
     * Ekranda ve filtrelemede kullanılacak standart ISO-2 (2 harfli) ülke kodu.
     */
    fun getDisplayCountryCode(): String {
        val titleLower = title.lowercase()
        val cityLower = city.lowercase()
        val cNameLower = country_name.lowercase()
        val cUpper = country.trim().uppercase()

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
            cNameLower.contains("yeni zelanda") || cNameLower.contains("new zealand") || cityLower.contains("auckland") || cityLower.contains("wellington") || cityLower.contains("christchurch") -> "NZ"
            cNameLower.contains("avustralya") || cNameLower.contains("australia") || cityLower.contains("sydney") || cityLower.contains("melbourne") || cityLower.contains("brisbane") || cityLower.contains("perth") -> "AU"
            cNameLower.contains("isviçre") || cNameLower.contains("switz") || cityLower.contains("lucerne") || cityLower.contains("luzern") || cityLower.contains("klosters") || cityLower.contains("zurich") || cityLower.contains("zürich") -> "CH"
            cNameLower.contains("portekiz") || cNameLower.contains("portugal") || cityLower.contains("lisboa") || cityLower.contains("lizbon") || cityLower.contains("porto") || titleLower.contains("portug") -> "PT"
            cNameLower.contains("çek") || cNameLower.contains("czech") || cityLower.contains("janske") || cityLower.contains("prag") -> "CZ"
            cNameLower.contains("sloven") || cityLower.contains("domzale") || cityLower.contains("novo mesto") || cityLower.contains("ljubljana") -> "SI"
            cNameLower.contains("norve") || cNameLower.contains("norway") || cityLower.contains("oslo") -> "NO"
            cNameLower.contains("ispan") || cNameLower.contains("spain") || cityLower.contains("madrid") || cityLower.contains("barcelona") || cityLower.contains("murcia") || cityLower.contains("ojós") || cityLower.contains("ojos") -> "ES"
            cNameLower.contains("ital") || cityLower.contains("catania") || cityLower.contains("roma") -> "IT"
            cNameLower.contains("alman") || cNameLower.contains("german") || cityLower.contains("berlin") || cityLower.contains("münchen") -> "DE"
            cNameLower.contains("türk") || cNameLower.contains("turk") || cityLower.contains("izmir") || cityLower.contains("istanbul") -> "TR"
            cNameLower.contains("hırvat") || cNameLower.contains("croat") || cityLower.contains("zagreb") -> "HR"
            cNameLower.contains("finland") || cityLower.contains("jyvaskyla") || cityLower.contains("jyväskylä") || cityLower.contains("tampere") -> "FI"
            cNameLower.contains("irlanda") || cNameLower.contains("ireland") || cityLower.contains("galway") || cityLower.contains("dublin") -> "IE"
            cNameLower.contains("fransa") || cNameLower.contains("france") || cityLower.contains("paris") -> "FR"
            cNameLower.contains("romanya") || cNameLower.contains("romania") || cityLower.contains("craiova") || cityLower.contains("bükreş") || cityLower.contains("bucuresti") -> "RO"
            cNameLower.contains("polonya") || cNameLower.contains("poland") || cityLower.contains("torun") || cityLower.contains("toruń") -> "PL"
            cNameLower.contains("isveç") || cNameLower.contains("sweden") || cityLower.contains("gothenburg") || cityLower.contains("göteborg") -> "SE"
            cNameLower.contains("hollanda") || cNameLower.contains("netherland") || cityLower.contains("amsterdam") -> "NL"
            cNameLower.contains("yunan") || cNameLower.contains("greece") || cityLower.contains("atina") || cityLower.contains("athens") -> "GR"
            cNameLower.contains("avusturya") || cNameLower.contains("austria") || cityLower.contains("wien") || cityLower.contains("viyana") -> "AT"
            cNameLower.contains("belçika") || cNameLower.contains("belcika") || cNameLower.contains("belgium") || cityLower.contains("brussel") || cityLower.contains("brüksel") -> "BE"
            cNameLower.contains("danimarka") || cNameLower.contains("denmark") || cityLower.contains("kopenhag") || cityLower.contains("aarhus") -> "DK"
            cNameLower.contains("macar") || cNameLower.contains("hungar") || cityLower.contains("budapest") -> "HU"
            cNameLower.contains("slovak") || cityLower.contains("bratislava") -> "SK"
            cNameLower.contains("eston") || cityLower.contains("tallinn") -> "EE"
            cNameLower.contains("leton") || cNameLower.contains("latvi") || cityLower.contains("riga") -> "LV"
            cNameLower.contains("litvan") || cNameLower.contains("lithuan") || cityLower.contains("vilnius") -> "LT"
            cNameLower.contains("kanada") || cNameLower.contains("canada") -> "CA"
            cNameLower.contains("güney kore") || cNameLower.contains("korea") || cityLower.contains("daegu") || cityLower.contains("seoul") -> "KR"
            else -> ""
        }
        if (resolvedFromText.isNotEmpty() && (cUpper == "EU" || cUpper == "INT" || cUpper.isBlank() || cityLower.contains("luzern") || cityLower.contains("klosters"))) {
            return resolvedFromText
        }

        val normalized = normalizeToIso2(cUpper)
        if (normalized.isNotEmpty() && normalized != "INT" && normalized != "EU") {
            return normalized
        }
        return if (resolvedFromText.isNotEmpty()) resolvedFromText else normalized.ifBlank { "TR" }
    }

    /**
     * Ekranda gösterilecek standart Türkçe ülke ismi (tüm dünya ülkeleri destekli).
     */
    fun getDisplayCountryName(): String {
        val code = normalizeToIso2(getDisplayCountryCode())
        val cNameLower = country_name.lowercase()

        when (code) {
            "TR" -> return "Türkiye"
            "AL" -> return "Arnavutluk"
            "JP" -> return "Japonya"
            "DE" -> return "Almanya"
            "FR" -> return "Fransa"
            "GR" -> return "Yunanistan"
            "IT" -> return "İtalya"
            "ES" -> return "İspanya"
            "GB" -> return "Birleşik Krallık"
            "PL" -> return "Polonya"
            "BG" -> return "Bulgaristan"
            "RO" -> return "Romanya"
            "RS" -> return "Sırbistan"
            "SE" -> return "İsveç"
            "NL" -> return "Hollanda"
            "PT" -> return "Portekiz"
            "KR" -> return "Güney Kore"
            "US" -> return "ABD"
            "FI" -> return "Finlandiya"
            "CH" -> return "İsviçre"
            "IE" -> return "İrlanda"
            "SI" -> return "Slovenya"
            "CZ" -> return "Çekya"
            "NO" -> return "Norveç"
            "DK" -> return "Danimarka"
            "IS" -> return "İzlanda"
            "LV" -> return "Letonya"
            "LT" -> return "Litvanya"
            "HR" -> return "Hırvatistan"
            "AT" -> return "Avusturya"
            "BE" -> return "Belçika"
            "EE" -> return "Estonya"
            "HU" -> return "Macaristan"
            "SK" -> return "Slovakya"
            "UA" -> return "Ukrayna"
            "BA" -> return "Bosna-Hersek"
            "ME" -> return "Karadağ"
            "MK" -> return "Kuzey Makedonya"
            "XK" -> return "Kosova"
            "MD" -> return "Moldova"
            "CY" -> return "Kıbrıs"
            "MT" -> return "Malta"
            "LU" -> return "Lüksemburg"
            "NZ" -> return "Yeni Zelanda"
            "AU" -> return "Avustralya"
            "CA" -> return "Kanada"
            "BR" -> return "Brezilya"
            "AR" -> return "Arjantin"
            "MX" -> return "Meksika"
            "PE" -> return "Peru"
            "CL" -> return "Şili"
            "CO" -> return "Kolombiya"
            "ZA" -> return "Güney Afrika"
            "MA" -> return "Fas"
            "TN" -> return "Tunus"
            "EG" -> return "Mısır"
            "CN" -> return "Çin"
            "TW" -> return "Tayvan"
            "IN" -> return "Hindistan"
            "SG" -> return "Singapur"
            "MY" -> return "Malezya"
            "TH" -> return "Tayland"
            "KZ" -> return "Kazakistan"
            "UZ" -> return "Özbekistan"
            "AZ" -> return "Azerbaycan"
            "GE" -> return "Gürcistan"
        }

        // Herhangi bir ISO-2 ülke kodu için Android ICU Türkçe ülke adı altyapısı
        if (code.length == 2 && code != "EU" && code != "INT") {
            val localeTrName = java.util.Locale("", code).getDisplayCountry(java.util.Locale("tr"))
            if (localeTrName.isNotBlank() && !localeTrName.equals(code, ignoreCase = true)) {
                return localeTrName
            }
        }

        if (cNameLower.contains("new zealand") || cNameLower.contains("yeni zelanda")) return "Yeni Zelanda"
        if (cNameLower.contains("australia") || cNameLower.contains("avustralya")) return "Avustralya"

        return country_name.ifBlank { country }
    }

    /**
     * Ekranda gösterilecek konum / şehir.
     */
    fun getDisplayLocation(): String {
        val cName = getDisplayCountryName()
        val titleLower = title.lowercase()
        val isKansai = titleLower.contains("kansai") || titleLower.contains("world masters games") || titleLower.contains("wmg") || city.lowercase().contains("kansai")
        val rawCity = if (isKansai) {
            "Kansai"
        } else {
            city.trim()
        }
        val invalidCities = setOf(
            "", "not specified", "unspecified", "tbd", "tbc", "tba", "n/a", "na", "none", "null", "unknown", "belirlenecek"
        )
        val cCity = if (rawCity.lowercase() in invalidCities || rawCity.equals(cName, ignoreCase = true) || rawCity.equals(country_name, ignoreCase = true)) {
            ""
        } else {
            rawCity
        }
        return if (cCity.isNotBlank()) "$cCity, $cName" else cName
    }

    /**
     * Evrensel Ülke Bayrak Emojisi:
     * Hangi ülke olursa olsun (Yeni Zelanda, Avustralya, Peru, Kanada, tüm Avrupa/Asya/Afrika ülkeleri)
     * ISO ülke kodunu Unicode Regional Indicator bayrak emojisine dönüştürür.
     */
    fun getCountryFlag(): String {
        return countryCodeToFlagEmoji(getDisplayCountryCode())
    }

    companion object {
        fun normalizeToIso2(rawCode: String): String {
            return when (rawCode.trim().uppercase()) {
                "TUR", "TR" -> "TR"
                "GRE", "GR" -> "GR"
                "GER", "DEU", "DE" -> "DE"
                "ITA", "IT" -> "IT"
                "GBR", "UK", "ENG", "SCO", "WAL", "NIR", "GB" -> "GB"
                "ESP", "ES" -> "ES"
                "FRA", "FR" -> "FR"
                "POL", "PL" -> "PL"
                "BUL", "BGR", "BG" -> "BG"
                "ROU", "ROM", "RO" -> "RO"
                "SRB", "RS" -> "RS"
                "SWE", "SE" -> "SE"
                "NED", "NLD", "NL" -> "NL"
                "POR", "PRT", "PT" -> "PT"
                "KOR", "KR" -> "KR"
                "JPN", "JP" -> "JP"
                "USA", "US" -> "US"
                "CAN", "CA" -> "CA"
                "FIN", "FI" -> "FI"
                "CHE", "SUI", "CH" -> "CH"
                "IRL", "IE" -> "IE"
                "SVN", "SLO", "SI" -> "SI"
                "ALB", "AL" -> "AL"
                "CZE", "CZ" -> "CZ"
                "SVK", "SK" -> "SK"
                "HUN", "HU" -> "HU"
                "NOR", "NO" -> "NO"
                "DNK", "DEN", "DK" -> "DK"
                "ISL", "IS" -> "IS"
                "LVA", "LAT", "LV" -> "LV"
                "LTU", "LT" -> "LT"
                "EST", "EE" -> "EE"
                "HRV", "CRO", "HR" -> "HR"
                "AUT", "AT" -> "AT"
                "BEL", "BE" -> "BE"
                "LUX", "LU" -> "LU"
                "MLT", "MT" -> "MT"
                "CYP", "CY" -> "CY"
                "BIH", "BA" -> "BA"
                "MNE", "ME" -> "ME"
                "MKD", "MK" -> "MK"
                "KOS", "XK" -> "XK"
                "MDA", "MD" -> "MD"
                "UKR", "UA" -> "UA"
                "NZL", "NZ" -> "NZ"
                "AUS", "AU" -> "AU"
                "BRA", "BR" -> "BR"
                "ARG", "AR" -> "AR"
                "MEX", "MX" -> "MX"
                "PER", "PE" -> "PE"
                "CHL", "CHI", "CL" -> "CL"
                "COL", "CO" -> "CO"
                "URY", "URU", "UY" -> "UY"
                "ZAF", "RSA", "ZA" -> "ZA"
                "MAR", "MA" -> "MA"
                "TUN", "TN" -> "TN"
                "EGY", "EG" -> "EG"
                "KEN", "KE" -> "KE"
                "ETH", "ET" -> "ET"
                "CHN", "CN" -> "CN"
                "TPE", "TWN", "TW" -> "TW"
                "HKG", "HK" -> "HK"
                "IND", "IN" -> "IN"
                "SGP", "SG" -> "SG"
                "MYS", "MAS", "MY" -> "MY"
                "THA", "TH" -> "TH"
                "IDN", "INA", "ID" -> "ID"
                "PHL", "PHI", "PH" -> "PH"
                "VNM", "VIE", "VN" -> "VN"
                "KAZ", "KZ" -> "KZ"
                "UZB", "UZ" -> "UZ"
                "AZE", "AZ" -> "AZ"
                "GEO", "GE" -> "GE"
                "ARM", "AM" -> "AM"
                "ISR", "IL" -> "IL"
                "ARE", "UAE", "AE" -> "AE"
                "QAT", "QA" -> "QA"
                "SAU", "KSA", "SA" -> "SA"
                else -> rawCode.trim().uppercase()
            }
        }

        fun countryCodeToFlagEmoji(rawCode: String): String {
            val iso2 = normalizeToIso2(rawCode)
            if (iso2 == "EU") return "🇪🇺"
            if (iso2 == "INT" || iso2 == "WORLD") return "🌍"
            if (iso2 == "XK") return "🇽🇰"
            if (iso2.length == 2 && iso2[0] in 'A'..'Z' && iso2[1] in 'A'..'Z') {
                val first = Character.toChars(0x1F1E6 + (iso2[0].code - 'A'.code))
                val second = Character.toChars(0x1F1E6 + (iso2[1].code - 'A'.code))
                return String(first) + String(second)
            }
            return "🌍"
        }
    }
}

