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
     * Bugün ile yarışma tarihi arasındaki gün farkını hesaplar.
     * Çok günlü bir yarışma devam ediyorsa 0 döndürür; bitiş tarihi geçmişse negatif değer döndürür.
     */
    fun getDaysRemaining(): Long {
        if (start_date.isBlank()) return 0
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDateParsed = sdf.parse(start_date) ?: return 0
            val endDateParsed = if (end_date.isNotBlank()) sdf.parse(end_date) ?: startDateParsed else startDateParsed
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            if (today.time in startDateParsed.time..endDateParsed.time) {
                0L
            } else if (today.time > endDateParsed.time) {
                TimeUnit.MILLISECONDS.toDays(endDateParsed.time - today.time)
            } else {
                TimeUnit.MILLISECONDS.toDays(startDateParsed.time - today.time)
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Yarışmanın tamamlanıp arşiv statüsüne geçip geçmediğini döndürür.
     */
    fun isCompleted(): Boolean {
        return getDaysRemaining() < 0 || status.equals("completed", ignoreCase = true)
    }

    /**
     * Ekranda gösterilecek Türkçe yarışma başlığı.
     * İngilizce, Almanca, Fransızca, İtalyanca ve İspanyolca resmi yarışma başlıklarını
     * standart, akıcı Türkçe şampiyona terminolojisine çevirir.
     */
    fun getDisplayTitle(): String {
        var t = title.trim()
        if (t.isBlank()) return ""

        // Zaten Türkçe olan başlıklar (Türkiye, Balkan vb.)
        val lower = t.lowercase()
        if (lower.contains("türkiye master") || lower.contains("balkan master") || lower.contains("şampiyona") || lower.contains("kupa")) {
            // Şehir isimleri veya ufak düzeltmeler
            return t
        }

        // Dünya Masterlar Oyunları (World Masters Games)
        if (lower.contains("winter world masters games")) {
            val cityYear = t.replace(Regex("(?i)winter\\s+world\\s+masters\\s+games"), "").trim()
            return "Kış Dünya Masterlar Oyunları ${translateCityInTitle(cityYear)}".trim()
        }
        if (lower.contains("world masters games")) {
            val cityYear = t.replace(Regex("(?i)world\\s+masters\\s+games"), "").trim()
            return "Dünya Masterlar Oyunları ${translateCityInTitle(cityYear)}".trim()
        }
        if (lower.contains("european masters games")) {
            val cityYear = t.replace(Regex("(?i)european\\s+masters\\s+games"), "").trim()
            return "Avrupa Masterlar Oyunları ${translateCityInTitle(cityYear)}".trim()
        }
        if (lower.contains("pan-american masters games") || lower.contains("americas masters games")) {
            val cityYear = t.replace(Regex("(?i)(pan-american|americas)\\s+masters\\s+games"), "").trim()
            return "Pan-Amerikan Masterlar Oyunları ${translateCityInTitle(cityYear)}".trim()
        }
        if (lower.contains("asia-pacific masters games")) {
            val cityYear = t.replace(Regex("(?i)asia-pacific\\s+masters\\s+games"), "").trim()
            return "Asya-Pasifik Masterlar Oyunları ${translateCityInTitle(cityYear)}".trim()
        }

        // WMA Dünya Masterlar Atletizm Şampiyonaları
        if (lower.contains("world masters athletics championships indoor") || lower.contains("wma indoor")) {
            val rest = t.replace(Regex("(?i)(world\\s+masters\\s+athletics\\s+championships\\s+indoor|wma\\s+indoor)"), "").trim()
            return "Dünya Masterlar Salon Atletizm Şampiyonası ${translateCityInTitle(rest)}".trim()
        }
        if (lower.contains("world masters athletics championships")) {
            val rest = t.replace(Regex("(?i)world\\s+masters\\s+athletics\\s+championships(\\s+stadia)?"), "").trim()
            return "Dünya Masterlar Açık Saha Atletizm Şampiyonası ${translateCityInTitle(rest)}".trim()
        }

        // EMA Avrupa Masterlar Atletizm Şampiyonaları
        if (lower.contains("european masters athletics championships indoor") || lower.contains("emaci")) {
            val rest = t.replace(Regex("(?i)(european\\s+masters\\s+athletics\\s+championships\\s+indoor|emaci)"), "").trim()
            return "Avrupa Masterlar Salon Atletizm Şampiyonası ${translateCityInTitle(rest)}".trim()
        }
        if (lower.contains("european masters athletics championships non-stadia") || lower.contains("emacns")) {
            val rest = t.replace(Regex("(?i)(european\\s+masters\\s+athletics\\s+championships\\s+non-stadia|emacns)"), "").trim()
            return "Avrupa Masterlar Yol ve Kros Şampiyonası (Non-Stadia) ${translateCityInTitle(rest)}".trim()
        }
        if (lower.contains("european masters marathon")) {
            val rest = t.replace(Regex("(?i)european\\s+masters\\s+marathon\\s+championships?"), "").trim()
            return "Avrupa Masterlar Maraton Şampiyonası ${translateCityInTitle(rest)}".trim()
        }
        if (lower.contains("european masters athletics championships") || lower.contains("emacs")) {
            val rest = t.replace(Regex("(?i)(european\\s+masters\\s+athletics\\s+championships(\\s+stadia)?|emacs)"), "").trim()
            return "Avrupa Masterlar Açık Saha Atletizm Şampiyonası ${translateCityInTitle(rest)}".trim()
        }

        // BAMA Balkan Masterlar Şampiyonaları
        if (lower.contains("balkan masters athletics indoor") || lower.contains("bamaci")) {
            val rest = t.replace(Regex("(?i)(balkan\\s+masters\\s+athletics\\s+(indoor\\s+)?championships?\\s+indoor|bamaci)"), "").trim()
            return "Balkan Masterlar Salon Atletizm Şampiyonası ${translateCityInTitle(rest)}".trim()
        }
        if (lower.contains("balkan masters athletics championships") || lower.contains("bamacs")) {
            val rest = t.replace(Regex("(?i)(balkan\\s+masters\\s+athletics\\s+championships(\\s+stadia)?|bamacs)"), "").trim()
            return "Balkan Masterlar Açık Saha Atletizm Şampiyonası ${translateCityInTitle(rest)}".trim()
        }

        // Birleşik Krallık (BMAF)
        if (lower.contains("bmaf") || lower.contains("british masters")) {
            if (lower.contains("indoor")) {
                val rest = t.replace(Regex("(?i)bmaf\\s+british\\s+masters\\s+indoor\\s+(track\\s+&\\s+field\\s+)?championships?"), "").trim()
                return "Birleşik Krallık Masterlar Salon Atletizm Şampiyonası ${rest}".trim()
            } else {
                val rest = t.replace(Regex("(?i)bmaf\\s+british\\s+masters\\s+(track\\s+&\\s+field\\s+)?championships?"), "").trim()
                return "Birleşik Krallık Masterlar Açık Saha Atletizm Şampiyonası ${rest}".trim()
            }
        }

        // Fransa (FFA)
        if (lower.contains("championnats de france masters") || lower.contains("championnat de france masters")) {
            if (lower.contains("salle")) {
                val rest = t.replace(Regex("(?i)championnats?\\s+de\\s+france\\s+masters\\s+en\\s+salle"), "").trim()
                return "Fransa Masterlar Salon Atletizm Şampiyonası ${rest}".trim()
            } else {
                val rest = t.replace(Regex("(?i)championnats?\\s+de\\s+france\\s+masters(\\s+sur\\s+piste)?"), "").trim()
                return "Fransa Masterlar Açık Saha Atletizm Şampiyonası ${rest}".trim()
            }
        }

        // Almanya (DLV)
        if (lower.contains("deutsche hallenmeisterschaften masters") || (lower.contains("deutsche") && lower.contains("halle") && lower.contains("master"))) {
            val rest = t.replace(Regex("(?i)deutsche\\s+hallenmeisterschaften\\s+masters"), "").trim()
            return "Almanya Masterlar Salon Atletizm Şampiyonası ${rest}".trim()
        }
        if (lower.contains("deutsche masters-meisterschaften") || lower.contains("deutsche meisterschaften masters")) {
            val rest = t.replace(Regex("(?i)deutsche\\s+(masters-)?meisterschaften(\\s+masters)?"), "").trim()
            return "Almanya Masterlar Açık Saha Atletizm Şampiyonası ${rest}".trim()
        }

        // İtalya (FIDAL)
        if (lower.contains("campionati italiani") && lower.contains("master")) {
            if (lower.contains("indoor")) {
                val rest = t.replace(Regex("(?i)campionati\\s+italiani(\\s+individuali)?\\s+master\\s+indoor"), "").trim()
                return "İtalya Masterlar Salon Atletizm Şampiyonası ${rest}".trim()
            } else {
                val rest = t.replace(Regex("(?i)campionati\\s+italiani(\\s+individuali)?(\\s+su\\s+pista)?\\s+master"), "").trim()
                return "İtalya Masterlar Açık Saha Atletizm Şampiyonası ${rest}".trim()
            }
        }

        // İspanya (RFEA)
        if (lower.contains("campeonato de españa") && lower.contains("m\u00e1ster")) {
            if (lower.contains("short track") || lower.contains("pista cubierta")) {
                val rest = t.replace(Regex("(?i)campeonato\\s+de\\s+espa\u00f1a\\s+m\u00e1ster\\s+(short\\s+track|pista\\s+cubierta)"), "").trim()
                return "İspanya Masterlar Salon Atletizm Şampiyonası ${rest}".trim()
            } else {
                val rest = t.replace(Regex("(?i)campeonato\\s+de\\s+espa\u00f1a\\s+m\u00e1ster(\\s+aire\\s+libre)?"), "").trim()
                return "İspanya Masterlar Açık Saha Atletizm Şampiyonası ${rest}".trim()
            }
        }

        // Genel şablon çevirileri (Kelime bazlı zenginleştirme)
        var result = t
            .replace(Regex("(?i)\\bIndoor Track & Field Championships?\\b"), "Salon Atletizm Şampiyonası")
            .replace(Regex("(?i)\\bTrack & Field Championships?\\b"), "Açık Saha Atletizm Şampiyonası")
            .replace(Regex("(?i)\\bIndoor Championships?\\b"), "Salon Şampiyonası")
            .replace(Regex("(?i)\\bAthletics Championships?\\b"), "Atletizm Şampiyonası")
            .replace(Regex("(?i)\\bNational Championships?\\b"), "Ulusal Şampiyonası")
            .replace(Regex("(?i)\\bMasters Championships?\\b"), "Masterlar Şampiyonası")
            .replace(Regex("(?i)\\bMasters Games\\b"), "Masterlar Oyunları")
            .replace(Regex("(?i)\\bCross Country\\b"), "Kros")
            .replace(Regex("(?i)\\bRoad Running\\b"), "Yol Koşusu")
            .replace(Regex("(?i)\\bHalf Marathon\\b"), "Yarı Maraton")
            .replace(Regex("(?i)\\bMarathon\\b"), "Maraton")
            .replace(Regex("(?i)\\bTrail Running\\b"), "Patika Koşusu")
            .replace(Regex("(?i)\\bMountain Running\\b"), "Dağ Koşusu")
            .replace(Regex("(?i)\\bRace Walking\\b"), "Yürüyüş")
            .replace(Regex("(?i)\\b1st\\b"), "1.")
            .replace(Regex("(?i)\\b2nd\\b"), "2.")
            .replace(Regex("(?i)\\b3rd\\b"), "3.")
            .replace(Regex("(?i)\\b(\\d+)th\\b"), "$1.")

        return translateCityInTitle(result)
    }

    private fun translateCityInTitle(text: String): String {
        return text
            .replace(Regex("(?i)\\bRome\\b"), "Roma")
            .replace(Regex("(?i)\\bAthens\\b"), "Atina")
            .replace(Regex("(?i)\\bVienna\\b"), "Viyana")
            .replace(Regex("(?i)\\bMunich\\b"), "Münih")
            .replace(Regex("(?i)\\bBrussels\\b"), "Brüksel")
            .replace(Regex("(?i)\\bCopenhagen\\b"), "Kopenhag")
            .replace(Regex("(?i)\\bWarsaw\\b"), "Varşova")
            .replace(Regex("(?i)\\bBucharest\\b"), "Bükreş")
            .replace(Regex("(?i)\\bBelgrade\\b"), "Belgrad")
            .replace(Regex("(?i)\\bThessaloniki\\b"), "Selanik")
            .replace(Regex("(?i)\\bSofia\\b"), "Sofya")
            .replace(Regex("(?i)\\bGothenburg\\b"), "Göteborg")
            .replace(Regex("(?i)\\bBudapest\\b"), "Budapeşte")
            .replace(Regex("(?i)\\bGeneva\\b"), "Cenevre")
            .replace(Regex("(?i)\\bLondon\\b"), "Londra")
            .replace(Regex("(?i)\\bLisbon\\b"), "Lizbon")
            .replace(Regex("(?i)\\bPrague\\b"), "Prag")
            .replace(Regex("(?i)\\bSydney\\b"), "Sidney")
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
        if (tLow.contains("european masters games")) {
            return "europe"
        }
        if (sLow == "world" || sLow == "international" || tLow.contains("world") || tLow.contains("wma") || tLow.contains("wmg") || tLow.contains("kansai")) {
            return "world"
        }
        if (sLow == "balkan" || tLow.contains("balkan") || tLow.contains("bamacs") || tLow.contains("bamaci")) {
            return "balkan"
        }
        if (sLow == "europe" || sLow == "european" || tLow.contains("emacs") || tLow.contains("emaci") || tLow.contains("emamrc") || tLow.contains("emacns") ||
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

        val isStaleKansai = (titleLower.contains("masters games") || titleLower.contains("wmg")) &&
            !titleLower.contains("kansai") &&
            (cUpper == "JP" || cUpper == "JPN" || cityLower.contains("kansai"))

        val normalized = if (isStaleKansai) "" else normalizeToIso2(cUpper)
        if (normalized.length == 2 && normalized != "INT" && normalized != "EU") {
            return normalized
        }

        if (cUpper == "AL" || cUpper == "ALB" || cNameLower.contains("albania") || cNameLower.contains("arnavutluk") ||
            titleLower.contains("albania") || titleLower.contains("arnavutluk") || cityLower.contains("shkod") || cityLower.contains("tiran")
        ) {
            return "AL"
        }

        val resolvedFromText = when {
            cNameLower.contains("yeni zelanda") || cNameLower.contains("new zealand") || cityLower.contains("auckland") || titleLower.contains("auckland") || cityLower.contains("wellington") || cityLower.contains("christchurch") -> "NZ"
            cNameLower.contains("avustralya") || cNameLower.contains("australia") || cityLower.contains("sydney") || cityLower.contains("melbourne") || cityLower.contains("brisbane") || cityLower.contains("perth") || titleLower.contains("perth") || titleLower.contains("sydney") -> "AU"
            cNameLower.contains("birleşik arap") || cityLower.contains("abu dhabi") || titleLower.contains("abu dhabi") -> "AE"
            cNameLower.contains("tayvan") || cNameLower.contains("taiwan") || cityLower.contains("taipei") || titleLower.contains("taipei") -> "TW"
            cNameLower.contains("isviçre") || cNameLower.contains("switz") || cityLower.contains("lucerne") || cityLower.contains("luzern") || cityLower.contains("klosters") || cityLower.contains("zurich") || cityLower.contains("zürich") -> "CH"
            cNameLower.contains("portekiz") || cNameLower.contains("portugal") || cityLower.contains("lisboa") || cityLower.contains("lizbon") || cityLower.contains("porto") || titleLower.contains("portug") -> "PT"
            cNameLower.contains("çek") || cNameLower.contains("czech") || cityLower.contains("janske") || cityLower.contains("prag") -> "CZ"
            cNameLower.contains("sloven") || cityLower.contains("domzale") || cityLower.contains("novo mesto") || cityLower.contains("ljubljana") -> "SI"
            cNameLower.contains("norve") || cNameLower.contains("norway") || cityLower.contains("oslo") -> "NO"
            cNameLower.contains("ispan") || cNameLower.contains("spain") || cityLower.contains("madrid") || cityLower.contains("barcelona") || cityLower.contains("murcia") || cityLower.contains("ojós") || cityLower.contains("ojos") -> "ES"
            cNameLower.contains("ital") || cityLower.contains("catania") || cityLower.contains("roma") || cityLower.contains("como") || titleLower.contains("como") || titleLower.contains("lombardia") || titleLower.contains("torino") -> "IT"
            cNameLower.contains("alman") || cNameLower.contains("german") || cityLower.contains("berlin") || cityLower.contains("münchen") -> "DE"
            cNameLower.contains("türk") || cNameLower.contains("turk") || cityLower.contains("izmir") || cityLower.contains("istanbul") -> "TR"
            cNameLower.contains("hırvat") || cNameLower.contains("croat") || cityLower.contains("zagreb") -> "HR"
            cNameLower.contains("finland") || cityLower.contains("jyvaskyla") || cityLower.contains("jyväskylä") || cityLower.contains("tampere") || cityLower.contains("lahti") || titleLower.contains("lahti") || titleLower.contains("tampere") -> "FI"
            cNameLower.contains("irlanda") || cNameLower.contains("ireland") || cityLower.contains("galway") || cityLower.contains("dublin") -> "IE"
            cNameLower.contains("fransa") || cNameLower.contains("france") || cityLower.contains("paris") || titleLower.contains("nice") -> "FR"
            cNameLower.contains("romanya") || cNameLower.contains("romania") || cityLower.contains("craiova") || cityLower.contains("bükreş") || cityLower.contains("bucuresti") -> "RO"
            cNameLower.contains("polonya") || cNameLower.contains("poland") || cityLower.contains("torun") || cityLower.contains("toruń") -> "PL"
            cNameLower.contains("isveç") || cNameLower.contains("sweden") || cityLower.contains("gothenburg") || cityLower.contains("göteborg") -> "SE"
            cNameLower.contains("hollanda") || cNameLower.contains("netherland") || cityLower.contains("amsterdam") -> "NL"
            cNameLower.contains("yunan") || cNameLower.contains("greece") || cityLower.contains("atina") || cityLower.contains("athens") -> "GR"
            cNameLower.contains("avusturya") || cNameLower.contains("austria") || cityLower.contains("wien") || cityLower.contains("viyana") || titleLower.contains("innsbruck") -> "AT"
            cNameLower.contains("belçika") || cNameLower.contains("belcika") || cNameLower.contains("belgium") || cityLower.contains("brussel") || cityLower.contains("brüksel") -> "BE"
            cNameLower.contains("danimarka") || cNameLower.contains("denmark") || cityLower.contains("kopenhag") || cityLower.contains("aarhus") -> "DK"
            cNameLower.contains("macar") || cNameLower.contains("hungar") || cityLower.contains("budapest") -> "HU"
            cNameLower.contains("slovak") || cityLower.contains("bratislava") -> "SK"
            cNameLower.contains("eston") || cityLower.contains("tallinn") -> "EE"
            cNameLower.contains("leton") || cNameLower.contains("latvi") || cityLower.contains("riga") -> "LV"
            cNameLower.contains("litvan") || cNameLower.contains("lithuan") || cityLower.contains("vilnius") -> "LT"
            cNameLower.contains("kanada") || cNameLower.contains("canada") || titleLower.contains("vancouver") -> "CA"
            cNameLower.contains("güney kore") || cNameLower.contains("korea") || cityLower.contains("daegu") || cityLower.contains("seoul") || titleLower.contains("jeonbuk") -> "KR"
            titleLower.contains("cleveland") -> "US"
            else -> ""
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
            "AE" -> return "Birleşik Arap Emirlikleri"
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
        val isStaleKansai = (titleLower.contains("masters games") || titleLower.contains("wmg")) &&
            !titleLower.contains("kansai") &&
            city.lowercase().contains("kansai")

        val rawCity = if (isStaleKansai) {
            title
                .replace(Regex("(?i)(winter\\s+)?world\\s+masters\\s+games"), "")
                .replace(Regex("(?i)(open|european|pan\\s+american|asia\\s+pacific)\\s+masters\\s+games"), "")
                .replace(Regex("\\b20\\d{2}\\b"), "")
                .trim()
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

