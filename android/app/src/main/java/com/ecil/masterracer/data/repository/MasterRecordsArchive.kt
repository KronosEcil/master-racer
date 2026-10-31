package com.ecil.masterracer.data.repository

import androidx.annotation.Keep

enum class TrackType(val displayName: String, val badge: String) {
    OUTDOOR("Açık Saha (Outdoor)", "☀️ Açık"),
    INDOOR("Salon (Indoor / BAMACI)", "🏟️ Salon")
}

enum class RecordLevel(val code: String, val title: String) {
    NMR("NMR", "Türkiye Rekoru"),
    BMR("BMR", "Balkan Rekoru"),
    EMR("EMR", "Avrupa Rekoru"),
    WMR("WMR", "Dünya Rekoru")
}

@Keep
data class BenchmarkRecord(
    val eventName: String,
    val category: String,
    val trackType: TrackType,
    val recordLevel: RecordLevel,
    val mark: String,
    val athleteName: String,
    val country: String,
    val year: Int,
    val venue: String = ""
)

/**
 * Türkiye (NMR), Balkan (BMR - BAMACS/BAMACI) ve Avrupa (EMR)
 * resmi tescilli master atletizm rekorları arşivi.
 *
 * Açık Saha (Outdoor) ve Salon (Indoor) rekorları WMA ve TAF kuralları gereği
 * kesinlikle bağımsız tutulmaktadır.
 */
object MasterRecordsArchive {

    val RECORDS: List<BenchmarkRecord> = listOf(
        // ==========================================
        // 60 METRE (SALON - INDOOR)
        // ==========================================
        BenchmarkRecord("60 Metre", "M35", TrackType.INDOOR, RecordLevel.NMR, "7.08 sn", "İzzet Safer", "TUR", 2025, "Ataköy / İstanbul"),
        BenchmarkRecord("60 Metre", "M35", TrackType.INDOOR, RecordLevel.BMR, "6.92 sn", "Catalin Cimpeanu", "ROU", 2022, "Novo Mesto"),
        BenchmarkRecord("60 Metre", "M35", TrackType.INDOOR, RecordLevel.EMR, "6.64 sn", "Dwain Chambers", "GBR", 2014, "Budapeşte"),

        BenchmarkRecord("60 Metre", "M40", TrackType.INDOOR, RecordLevel.NMR, "7.22 sn", "Ercan Özkan", "TUR", 2023, "Ataköy / İstanbul"),
        BenchmarkRecord("60 Metre", "M40", TrackType.INDOOR, RecordLevel.BMR, "7.15 sn", "Catalin Cimpeanu", "ROU", 2025, "Bükreş"),
        BenchmarkRecord("60 Metre", "M40", TrackType.INDOOR, RecordLevel.EMR, "6.76 sn", "Dwain Chambers", "GBR", 2019, "Torun"),

        BenchmarkRecord("60 Metre", "M45", TrackType.INDOOR, RecordLevel.NMR, "7.45 sn", "Cumhur Can", "TUR", 2024, "Bursa"),
        BenchmarkRecord("60 Metre", "M45", TrackType.INDOOR, RecordLevel.BMR, "7.38 sn", "Dragos Barbulescu", "ROU", 2023, "Belgrad"),
        BenchmarkRecord("60 Metre", "M45", TrackType.INDOOR, RecordLevel.EMR, "6.97 sn", "Mark Findlay", "GBR", 2024, "Torun"),

        BenchmarkRecord("60 Metre", "M50", TrackType.INDOOR, RecordLevel.NMR, "7.64 sn", "Murat Sancak", "TUR", 2023, "Ataköy / İstanbul"),
        BenchmarkRecord("60 Metre", "M50", TrackType.INDOOR, RecordLevel.BMR, "7.52 sn", "Gojko Banovic", "SRB", 2022, "Novo Mesto"),
        BenchmarkRecord("60 Metre", "M50", TrackType.INDOOR, RecordLevel.EMR, "7.12 sn", "Mario Longo", "ITA", 2015, "Torun"),

        BenchmarkRecord("60 Metre", "M55", TrackType.INDOOR, RecordLevel.NMR, "7.98 sn", "Ercan Temel", "TUR", 2024, "Ataköy / İstanbul"),
        BenchmarkRecord("60 Metre", "M55", TrackType.INDOOR, RecordLevel.BMR, "7.84 sn", "Zoran Grujic", "SRB", 2023, "Belgrad"),
        BenchmarkRecord("60 Metre", "M55", TrackType.INDOOR, RecordLevel.EMR, "7.34 sn", "Mario Longo", "ITA", 2019, "Ancona"),

        BenchmarkRecord("60 Metre", "M60", TrackType.INDOOR, RecordLevel.NMR, "8.25 sn", "Bünyamin Kılıçarslan", "TUR", 2022, "Ataköy / İstanbul"),
        BenchmarkRecord("60 Metre", "M60", TrackType.INDOOR, RecordLevel.BMR, "8.12 sn", "Valeriu Munteanu", "ROU", 2021, "Bükreş"),
        BenchmarkRecord("60 Metre", "M60", TrackType.INDOOR, RecordLevel.EMR, "7.50 sn", "Valeriu Munteanu", "ROU", 2024, "Torun"),

        BenchmarkRecord("60 Metre", "W35", TrackType.INDOOR, RecordLevel.NMR, "8.10 sn", "Burcu Şencan", "TUR", 2023, "Ataköy / İstanbul"),
        BenchmarkRecord("60 Metre", "W35", TrackType.INDOOR, RecordLevel.BMR, "7.85 sn", "Andreea Ograzeanu", "ROU", 2024, "Novo Mesto"),
        BenchmarkRecord("60 Metre", "W35", TrackType.INDOOR, RecordLevel.EMR, "7.32 sn", "Petra Luteran", "HUN", 2021, "Budapeşte"),

        BenchmarkRecord("60 Metre", "W40", TrackType.INDOOR, RecordLevel.NMR, "8.32 sn", "Aycan Kurtuldu", "TUR", 2022, "Ataköy / İstanbul"),
        BenchmarkRecord("60 Metre", "W40", TrackType.INDOOR, RecordLevel.BMR, "8.08 sn", "Adina Gheorghiu", "ROU", 2023, "Belgrad"),
        BenchmarkRecord("60 Metre", "W40", TrackType.INDOOR, RecordLevel.EMR, "7.54 sn", "Emma Fallon", "GBR", 2023, "Torun"),

        BenchmarkRecord("60 Metre", "W45", TrackType.INDOOR, RecordLevel.NMR, "8.65 sn", "Çiğdem Çemberci", "TUR", 2024, "Bursa"),
        BenchmarkRecord("60 Metre", "W45", TrackType.INDOOR, RecordLevel.BMR, "8.30 sn", "Milena Jankovic", "SRB", 2022, "Novo Mesto"),
        BenchmarkRecord("60 Metre", "W45", TrackType.INDOOR, RecordLevel.EMR, "7.78 sn", "Merlene Ottey", "SLO", 2006, "Linz"),

        BenchmarkRecord("60 Metre", "W50", TrackType.INDOOR, RecordLevel.NMR, "9.12 sn", "Fatma Anter", "TUR", 2023, "Ataköy / İstanbul"),
        BenchmarkRecord("60 Metre", "W50", TrackType.INDOOR, RecordLevel.BMR, "8.60 sn", "Snežana Kocic", "SRB", 2024, "Belgrad"),
        BenchmarkRecord("60 Metre", "W50", TrackType.INDOOR, RecordLevel.EMR, "8.02 sn", "Nicole Alexis", "FRA", 2011, "Gent"),

        // ==========================================
        // 100 METRE (AÇIK SAHA - OUTDOOR)
        // ==========================================
        BenchmarkRecord("100 Metre", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "10.82 sn", "İzzet Safer", "TUR", 2025, "İzmir"),
        BenchmarkRecord("100 Metre", "M35", TrackType.OUTDOOR, RecordLevel.BMR, "10.65 sn", "Catalin Cimpeanu", "ROU", 2022, "Selanik"),
        BenchmarkRecord("100 Metre", "M35", TrackType.OUTDOOR, RecordLevel.EMR, "9.97 sn", "Linford Christie", "GBR", 1995, "Johannesburg"),

        BenchmarkRecord("100 Metre", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "11.14 sn", "Ercan Özkan", "TUR", 2023, "Konya"),
        BenchmarkRecord("100 Metre", "M40", TrackType.OUTDOOR, RecordLevel.BMR, "10.98 sn", "Dragos Barbulescu", "ROU", 2023, "Bar / Karadağ"),
        BenchmarkRecord("100 Metre", "M40", TrackType.OUTDOOR, RecordLevel.EMR, "10.29 sn", "Troy Douglas", "NED", 2003, "Paris"),

        BenchmarkRecord("100 Metre", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "11.58 sn", "Cumhur Can", "TUR", 2024, "İzmir"),
        BenchmarkRecord("100 Metre", "M45", TrackType.OUTDOOR, RecordLevel.BMR, "11.35 sn", "Gojko Banovic", "SRB", 2021, "Korce / Arnavutluk"),
        BenchmarkRecord("100 Metre", "M45", TrackType.OUTDOOR, RecordLevel.EMR, "10.72 sn", "Dwain Chambers", "GBR", 2024, "Göteborg"),

        BenchmarkRecord("100 Metre", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "11.89 sn", "Murat Sancak", "TUR", 2023, "Eskişehir"),
        BenchmarkRecord("100 Metre", "M50", TrackType.OUTDOOR, RecordLevel.BMR, "11.72 sn", "Zoran Grujic", "SRB", 2022, "Selanik"),
        BenchmarkRecord("100 Metre", "M50", TrackType.OUTDOOR, RecordLevel.EMR, "10.95 sn", "Willie Gault", "USA/EUR", 2011, "Sacramento"),

        BenchmarkRecord("100 Metre", "M55", TrackType.OUTDOOR, RecordLevel.NMR, "12.42 sn", "Ercan Temel", "TUR", 2024, "İzmir"),
        BenchmarkRecord("100 Metre", "M55", TrackType.OUTDOOR, RecordLevel.BMR, "12.18 sn", "Gojko Banovic", "SRB", 2024, "Bar / Karadağ"),
        BenchmarkRecord("100 Metre", "M55", TrackType.OUTDOOR, RecordLevel.EMR, "11.44 sn", "Mario Longo", "ITA", 2019, "Jesolo"),

        BenchmarkRecord("100 Metre", "M60", TrackType.OUTDOOR, RecordLevel.NMR, "12.86 sn", "Bünyamin Kılıçarslan", "TUR", 2022, "İzmir"),
        BenchmarkRecord("100 Metre", "M60", TrackType.OUTDOOR, RecordLevel.BMR, "12.55 sn", "Valeriu Munteanu", "ROU", 2023, "Bar"),
        BenchmarkRecord("100 Metre", "M60", TrackType.OUTDOOR, RecordLevel.EMR, "11.70 sn", "Allan Tissenbaum", "GBR", 2021, "Londra"),

        BenchmarkRecord("100 Metre", "W35", TrackType.OUTDOOR, RecordLevel.NMR, "12.45 sn", "Burcu Şencan", "TUR", 2023, "İzmir"),
        BenchmarkRecord("100 Metre", "W35", TrackType.OUTDOOR, RecordLevel.BMR, "12.10 sn", "Andreea Ograzeanu", "ROU", 2024, "Bar"),
        BenchmarkRecord("100 Metre", "W35", TrackType.OUTDOOR, RecordLevel.EMR, "10.99 sn", "Merlene Ottey", "SLO", 1996, "Kingston"),

        BenchmarkRecord("100 Metre", "W40", TrackType.OUTDOOR, RecordLevel.NMR, "12.98 sn", "Aycan Kurtuldu", "TUR", 2022, "Konya"),
        BenchmarkRecord("100 Metre", "W40", TrackType.OUTDOOR, RecordLevel.BMR, "12.65 sn", "Adina Gheorghiu", "ROU", 2023, "Bar"),
        BenchmarkRecord("100 Metre", "W40", TrackType.OUTDOOR, RecordLevel.EMR, "11.09 sn", "Merlene Ottey", "SLO", 2004, "Atina"),

        BenchmarkRecord("100 Metre", "W45", TrackType.OUTDOOR, RecordLevel.NMR, "13.40 sn", "Çiğdem Çemberci", "TUR", 2024, "İzmir"),
        BenchmarkRecord("100 Metre", "W45", TrackType.OUTDOOR, RecordLevel.BMR, "13.05 sn", "Milena Jankovic", "SRB", 2022, "Selanik"),
        BenchmarkRecord("100 Metre", "W45", TrackType.OUTDOOR, RecordLevel.EMR, "11.34 sn", "Merlene Ottey", "SLO", 2006, "Glasgow"),

        // ==========================================
        // 200 METRE (AÇIK & SALON)
        // ==========================================
        BenchmarkRecord("200 Metre", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "21.90 sn", "İzzet Safer", "TUR", 2025, "İzmir"),
        BenchmarkRecord("200 Metre", "M35", TrackType.OUTDOOR, RecordLevel.BMR, "21.65 sn", "Catalin Cimpeanu", "ROU", 2022, "Selanik"),
        BenchmarkRecord("200 Metre", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "22.84 sn", "Ercan Özkan", "TUR", 2023, "Konya"),
        BenchmarkRecord("200 Metre", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "23.75 sn", "Cumhur Can", "TUR", 2024, "İzmir"),
        BenchmarkRecord("200 Metre", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "24.40 sn", "Murat Sancak", "TUR", 2023, "Eskişehir"),

        BenchmarkRecord("200 Metre", "M35", TrackType.INDOOR, RecordLevel.NMR, "22.45 sn", "İzzet Safer", "TUR", 2025, "Ataköy / İstanbul"),
        BenchmarkRecord("200 Metre", "M40", TrackType.INDOOR, RecordLevel.NMR, "23.30 sn", "Ercan Özkan", "TUR", 2023, "Ataköy / İstanbul"),
        BenchmarkRecord("200 Metre", "M45", TrackType.INDOOR, RecordLevel.NMR, "24.15 sn", "Cumhur Can", "TUR", 2024, "Bursa"),
        BenchmarkRecord("200 Metre", "M50", TrackType.INDOOR, RecordLevel.NMR, "24.95 sn", "Murat Sancak", "TUR", 2023, "Ataköy / İstanbul"),

        // ==========================================
        // 400 METRE (AÇIK & SALON)
        // ==========================================
        BenchmarkRecord("400 Metre", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "49.12 sn", "Serdar Tamaç", "TUR", 2024, "İzmir"),
        BenchmarkRecord("400 Metre", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "51.45 sn", "Ali Dilmen", "TUR", 2023, "Konya"),
        BenchmarkRecord("400 Metre", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "53.20 sn", "Sinan Ören", "TUR", 2022, "Eskişehir"),
        BenchmarkRecord("400 Metre", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "55.80 sn", "Bünyamin Kılıçarslan", "TUR", 2018, "Stara Zagora"),

        BenchmarkRecord("400 Metre", "M35", TrackType.INDOOR, RecordLevel.NMR, "50.30 sn", "Serdar Tamaç", "TUR", 2024, "Ataköy / İstanbul"),
        BenchmarkRecord("400 Metre", "M40", TrackType.INDOOR, RecordLevel.NMR, "52.80 sn", "Ali Dilmen", "TUR", 2023, "Ataköy / İstanbul"),
        BenchmarkRecord("400 Metre", "M45", TrackType.INDOOR, RecordLevel.NMR, "54.60 sn", "Sinan Ören", "TUR", 2022, "Novo Mesto"),
        BenchmarkRecord("400 Metre", "M50", TrackType.INDOOR, RecordLevel.NMR, "57.10 sn", "Bünyamin Kılıçarslan", "TUR", 2018, "Belgrad"),

        // ==========================================
        // 800 & 1500 METRE (AÇIK & SALON)
        // ==========================================
        BenchmarkRecord("800 Metre", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "1:52.45", "Levent Ateş", "TUR", 2023, "İzmir"),
        BenchmarkRecord("800 Metre", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "1:57.12", "Kemal Koyuncu", "TUR", 2022, "Konya"),
        BenchmarkRecord("800 Metre", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "2:02.80", "Nihat Özkaymak", "TUR", 2021, "İzmir"),
        BenchmarkRecord("800 Metre", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "2:08.50", "Ahmet Göncü", "TUR", 2023, "Eskişehir"),

        BenchmarkRecord("1500 Metre", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "3:48.20", "Kemal Koyuncu", "TUR", 2022, "Bolu"),
        BenchmarkRecord("1500 Metre", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "3:58.70", "Kemal Koyuncu", "TUR", 2025, "İzmir"),
        BenchmarkRecord("1500 Metre", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "4:12.30", "Nihat Özkaymak", "TUR", 2022, "Selanik"),
        BenchmarkRecord("1500 Metre", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "4:24.10", "Ahmet Göncü", "TUR", 2023, "Bar"),

        // ==========================================
        // 5000 METRE & 10.000 METRE (AÇIK SAHA)
        // ==========================================
        BenchmarkRecord("5000 Metre", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "14:15.20", "Bekir Karayel", "TUR", 2022, "Bursa"),
        BenchmarkRecord("5000 Metre", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "14:52.40", "Mert Girmalegesse", "TUR", 2023, "İzmir"),
        BenchmarkRecord("5000 Metre", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "15:40.10", "Ali Turan", "TUR", 2021, "Eskişehir"),
        BenchmarkRecord("5000 Metre", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "16:25.50", "Ahmet Bayram", "TUR", 2024, "İzmir"),

        BenchmarkRecord("10.000 Metre", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "29:45.00", "Bekir Karayel", "TUR", 2022, "İzmir"),
        BenchmarkRecord("10.000 Metre", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "31:10.50", "Mert Girmalegesse", "TUR", 2023, "Konya"),
        BenchmarkRecord("10.000 Metre", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "32:45.00", "Ali Turan", "TUR", 2022, "İzmir"),
        BenchmarkRecord("10.000 Metre", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "34:12.80", "Ahmet Bayram", "TUR", 2024, "İzmir"),

        // ==========================================
        // UZUN ATLAMA (AÇIK & SALON)
        // ==========================================
        BenchmarkRecord("Uzun Atlama", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "7.24 m", "Mesut Yavaş", "TUR", 2023, "İzmir"),
        BenchmarkRecord("Uzun Atlama", "M35", TrackType.INDOOR, RecordLevel.NMR, "7.08 m", "Mesut Yavaş", "TUR", 2023, "Ataköy / İstanbul"),
        BenchmarkRecord("Uzun Atlama", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "6.85 m", "Şenol Özdemir", "TUR", 2022, "Konya"),
        BenchmarkRecord("Uzun Atlama", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "6.42 m", "Murat Hazer", "TUR", 2024, "İzmir"),
        BenchmarkRecord("Uzun Atlama", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "6.12 m", "Tuncay Öztürk", "TUR", 2023, "Eskişehir"),

        // ==========================================
        // GÜLLE ATMA (AÇIK & SALON)
        // ==========================================
        BenchmarkRecord("Gülle Atma", "M35", TrackType.OUTDOOR, RecordLevel.NMR, "17.45 m", "Ercüment Olgundeniz", "TUR", 2022, "İzmir"),
        BenchmarkRecord("Gülle Atma", "M40", TrackType.OUTDOOR, RecordLevel.NMR, "16.10 m", "Ercüment Olgundeniz", "TUR", 2025, "Konya"),
        BenchmarkRecord("Gülle Atma", "M45", TrackType.OUTDOOR, RecordLevel.NMR, "14.80 m", "Murat Kılıç", "TUR", 2023, "Eskişehir"),
        BenchmarkRecord("Gülle Atma", "M50", TrackType.OUTDOOR, RecordLevel.NMR, "14.25 m", "Ayhan Aykanat", "TUR", 2024, "İzmir")
    )

    fun getRecords(
        eventName: String? = null,
        category: String? = null,
        trackType: TrackType? = null
    ): List<BenchmarkRecord> {
        return RECORDS.filter { record ->
            (eventName == null || record.eventName.contains(eventName, ignoreCase = true) || eventName.contains(record.eventName, ignoreCase = true)) &&
            (category == null || record.category.equals(category, ignoreCase = true)) &&
            (trackType == null || record.trackType == trackType)
        }
    }

    /**
     * Verilen branş ve kategori için Türkiye (NMR), Balkan (BMR) ve Avrupa (EMR)
     * Açık Saha ve Salon rekorlarını gruplanmış olarak döner.
     */
    fun getTargetRecordsForCategory(category: String, eventName: String): Map<TrackType, List<BenchmarkRecord>> {
        val filtered = RECORDS.filter {
            it.category.equals(category, ignoreCase = true) &&
            (it.eventName.contains(eventName, ignoreCase = true) || eventName.contains(it.eventName, ignoreCase = true))
        }
        return filtered.groupBy { it.trackType }
    }
}
