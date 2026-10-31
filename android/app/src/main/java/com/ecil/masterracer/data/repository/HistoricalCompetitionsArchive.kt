package com.ecil.masterracer.data.repository

import com.ecil.masterracer.data.model.Competition

/**
 * 1975 - 2025 yılları arasında gerçekleştirilmiş resmi Master Atletizm Şampiyonaları
 * ve Masters Games yerel arşivi (Tamamı Türkçe başlık ve şehir adlarıyla).
 *
 * Not: 2026 ve sonrası güncel/gelecek yarışmalar GitHub Actions (AI Scraper) üzerinden
 * canlı olarak Firestore'dan çekilirken, tamamlanmış 1975-2025 geçmiş yarışmaları
 * uygulama içinde sabit arşiv olarak tutulur.
 */
object HistoricalCompetitionsArchive {

    private fun archiveItem(
        id: String,
        title: String,
        startDate: String,
        endDate: String,
        city: String,
        country: String,
        countryName: String,
        scope: String,
        federation: String,
        sourceUrl: String
    ): Competition {
        return Competition(
            id = id,
            title = title,
            start_date = startDate,
            end_date = endDate,
            city = city,
            country = country,
            country_name = countryName,
            scope = scope,
            federation = federation,
            website_url = sourceUrl,
            status = "completed"
        )
    }

    val competitions: List<Competition> by lazy {
        wmaWorldChampionshipsStadia +
            wmaWorldChampionshipsIndoor +
            worldMastersGamesArchive +
            emaEuropeanChampionshipsStadia +
            emaEuropeanChampionshipsIndoor +
            emaEuropeanNonStadiaArchive +
            balkanMastersStadiaArchive +
            balkanMastersIndoorArchive +
            tafTurkishMastersArchive +
            europeanNationalMastersArchive
    }

    // =========================================================================
    // 1. WMA DÜNYA MASTERLAR AÇIK SAHA ATLETİZM ŞAMPİYONALARI (1975 - 2024)
    // =========================================================================
    private val wmaWorldChampionshipsStadia = listOf(
        archiveItem("WMA_1975_TORONTO", "1. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1975", "1975-08-11", "1975-08-16", "Toronto", "CA", "Kanada", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1977_GOTHENBURG", "2. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1977", "1977-08-08", "1977-08-13", "Göteborg", "SE", "İsveç", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1979_HANNOVER", "3. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1979", "1979-07-27", "1979-08-02", "Hannover", "DE", "Almanya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1981_CHRISTCHURCH", "4. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1981", "1981-01-07", "1981-01-14", "Christchurch", "NZ", "Yeni Zelanda", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1983_SANJUAN", "5. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1983", "1983-09-23", "1983-09-30", "San Juan", "US", "ABD (Porto Riko)", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1985_ROME", "6. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1985", "1985-06-22", "1985-06-30", "Roma", "IT", "İtalya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1987_MELBOURNE", "7. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1987", "1987-11-29", "1987-12-06", "Melbourne", "AU", "Avustralya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1989_EUGENE", "8. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1989", "1989-07-27", "1989-08-06", "Eugene", "US", "ABD", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1991_TURKU", "9. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1991", "1991-07-18", "1991-07-28", "Turku", "FI", "Finlandiya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1993_MIYAZAKI", "10. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1993", "1993-10-07", "1993-10-17", "Miyazaki", "JP", "Japonya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1995_BUFFALO", "11. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1995", "1995-07-13", "1995-07-23", "Buffalo", "US", "ABD", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1997_DURBAN", "12. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1997", "1997-07-17", "1997-07-27", "Durban", "ZA", "Güney Afrika", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1999_GATESHEAD", "13. Dünya Masterlar Açık Saha Atletizm Şampiyonası 1999", "1999-07-29", "1999-08-08", "Gateshead", "GB", "Birleşik Krallık", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2001_BRISBANE", "14. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2001", "2001-07-04", "2001-07-14", "Brisbane", "AU", "Avustralya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2003_CAROLINA", "15. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2003", "2003-07-01", "2003-07-13", "Carolina", "US", "ABD (Porto Riko)", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2005_SANSEBASTIAN", "16. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2005", "2005-08-22", "2005-09-03", "San Sebastián", "ES", "İspanya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2007_RICCIONE", "17. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2007", "2007-09-04", "2007-09-15", "Riccione", "IT", "İtalya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2009_LAHTI", "18. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2009", "2009-07-28", "2009-08-08", "Lahti", "FI", "Finlandiya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2011_SACRAMENTO", "19. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2011", "2011-07-06", "2011-07-17", "Sacramento", "US", "ABD", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2013_PORTOALEGRE", "20. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2013", "2013-10-16", "2013-10-27", "Porto Alegre", "BR", "Brezilya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2015_LYON", "21. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2015", "2015-08-04", "2015-08-16", "Lyon", "FR", "Fransa", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2016_PERTH", "22. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2016", "2016-10-26", "2016-11-06", "Perth", "AU", "Avustralya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2018_MALAGA", "23. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2018", "2018-09-04", "2018-09-16", "Málaga", "ES", "İspanya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2022_TAMPERE", "24. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2022", "2022-06-29", "2022-07-10", "Tampere", "FI", "Finlandiya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2024_GOTHENBURG", "25. Dünya Masterlar Açık Saha Atletizm Şampiyonası 2024", "2024-08-13", "2024-08-25", "Göteborg", "SE", "İsveç", "world", "WMA", "https://world-masters-athletics.org/")
    )

    // =========================================================================
    // 2. WMA DÜNYA MASTERLAR SALON ATLETİZM ŞAMPİYONALARI (2004 - 2025)
    // =========================================================================
    private val wmaWorldChampionshipsIndoor = listOf(
        archiveItem("WMAI_2004_SINDELFINGEN", "1. Dünya Masterlar Salon Atletizm Şampiyonası 2004", "2004-03-10", "2004-03-14", "Sindelfingen", "DE", "Almanya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2006_LINZ", "2. Dünya Masterlar Salon Atletizm Şampiyonası 2006", "2006-03-15", "2006-03-20", "Linz", "AT", "Avusturya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2008_CLERMONT", "3. Dünya Masterlar Salon Atletizm Şampiyonası 2008", "2008-03-17", "2008-03-22", "Clermont-Ferrand", "FR", "Fransa", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2010_KAMLOOPS", "4. Dünya Masterlar Salon Atletizm Şampiyonası 2010", "2010-03-02", "2010-03-07", "Kamloops", "CA", "Kanada", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2012_JYVASKYLA", "5. Dünya Masterlar Salon Atletizm Şampiyonası 2012", "2012-04-03", "2012-04-08", "Jyväskylä", "FI", "Finlandiya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2014_BUDAPEST", "6. Dünya Masterlar Salon Atletizm Şampiyonası 2014", "2014-03-25", "2014-03-30", "Budapeşte", "HU", "Macaristan", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2017_DAEGU", "7. Dünya Masterlar Salon Atletizm Şampiyonası 2017", "2017-03-19", "2017-03-25", "Daegu", "KR", "Güney Kore", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2019_TORUN", "8. Dünya Masterlar Salon Atletizm Şampiyonası 2019", "2019-03-24", "2019-03-30", "Toruń", "PL", "Polonya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2023_TORUN", "9. Dünya Masterlar Salon Atletizm Şampiyonası 2023", "2023-03-26", "2023-04-01", "Toruń", "PL", "Polonya", "world", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2025_GAINESVILLE", "10. Dünya Masterlar Salon Atletizm Şampiyonası 2025", "2025-03-23", "2025-03-30", "Gainesville", "US", "ABD", "world", "WMA", "https://world-masters-athletics.org/")
    )

    // =========================================================================
    // 3. DÜNYA VE KITALARARASI MASTERLAR OYUNLARI (IMGA: 1985 - 2025)
    // =========================================================================
    private val worldMastersGamesArchive = listOf(
        archiveItem("WMG_1985_TORONTO", "Dünya Masterlar Oyunları Toronto 1985", "1985-08-07", "1985-08-25", "Toronto", "CA", "Kanada", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_1989_AALBORG", "Dünya Masterlar Oyunları Aalborg 1989", "1989-07-22", "1989-08-06", "Aalborg", "DK", "Danimarka", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_1994_BRISBANE", "Dünya Masterlar Oyunları Brisbane 1994", "1994-09-26", "1994-10-08", "Brisbane", "AU", "Avustralya", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_1998_PORTLAND", "Dünya Masterlar Oyunları Portland 1998", "1998-08-09", "1998-08-22", "Portland", "US", "ABD", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2002_MELBOURNE", "Dünya Masterlar Oyunları Melbourne 2002", "2002-10-05", "2002-10-13", "Melbourne", "AU", "Avustralya", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2005_EDMONTON", "Dünya Masterlar Oyunları Edmonton 2005", "2005-07-22", "2005-07-31", "Edmonton", "CA", "Kanada", "world", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2008_MALMO", "Avrupa Masterlar Oyunları Malmö 2008", "2008-08-29", "2008-09-07", "Malmö", "SE", "İsveç", "europe", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2009_SYDNEY", "Dünya Masterlar Oyunları Sidney 2009", "2009-10-10", "2009-10-18", "Sidney", "AU", "Avustralya", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WWMG_2010_BLED", "Kış Dünya Masterlar Oyunları Bled 2010", "2010-01-25", "2010-01-31", "Bled", "SI", "Slovenya", "world", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2011_LIGNANO", "Avrupa Masterlar Oyunları Lignano Sabbiadoro 2011", "2011-09-10", "2011-09-20", "Lignano Sabbiadoro", "IT", "İtalya", "europe", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2013_TORINO", "Dünya Masterlar Oyunları Torino 2013", "2013-08-02", "2013-08-11", "Torino", "IT", "İtalya", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WWMG_2015_QUEBEC", "Kış Dünya Masterlar Oyunları Quebec 2015", "2015-01-31", "2015-02-08", "Quebec City", "CA", "Kanada", "world", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2015_NICE", "Avrupa Masterlar Oyunları Nice 2015", "2015-10-01", "2015-10-11", "Nice", "FR", "Fransa", "europe", "IMGA", "https://imga.ch/"),
        archiveItem("PAMG_2016_VANCOUVER", "Amerika Kıtası Masterlar Oyunları Vancouver 2016", "2016-08-26", "2016-09-04", "Vancouver", "CA", "Kanada", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2017_AUCKLAND", "Dünya Masterlar Oyunları Auckland 2017", "2017-04-21", "2017-04-30", "Auckland", "NZ", "Yeni Zelanda", "world", "IMGA", "https://imga.ch/"),
        archiveItem("APMG_2018_PENANG", "Asya-Pasifik Masterlar Oyunları Penang 2018", "2018-09-07", "2018-09-15", "Penang", "MY", "Malezya", "world", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2019_TORINO", "Avrupa Masterlar Oyunları Torino 2019", "2019-07-26", "2019-08-04", "Torino", "IT", "İtalya", "europe", "IMGA", "https://imga.ch/"),
        archiveItem("WWMG_2020_INNSBRUCK", "Kış Dünya Masterlar Oyunları Innsbruck 2020", "2020-01-10", "2020-01-19", "Innsbruck", "AT", "Avusturya", "world", "IMGA", "https://imga.ch/"),
        archiveItem("APMG_2023_JEONBUK", "Asya-Pasifik Masterlar Oyunları Jeonbuk 2023", "2023-05-12", "2023-05-20", "Jeonbuk", "KR", "Güney Kore", "world", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2023_TAMPERE", "Avrupa Masterlar Oyunları Tampere 2023", "2023-06-26", "2023-07-09", "Tampere", "FI", "Finlandiya", "europe", "IMGA", "https://imga.ch/"),
        archiveItem("WWMG_2024_LOMBARDIA", "Kış Dünya Masterlar Oyunları Lombardiya 2024", "2024-01-12", "2024-01-21", "Lombardiya", "IT", "İtalya", "world", "IMGA", "https://imga.ch/"),
        archiveItem("PAMG_2024_CLEVELAND", "Pan-Amerikan Masterlar Oyunları Cleveland 2024", "2024-07-12", "2024-07-21", "Cleveland", "US", "ABD", "world", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2025_TAIPEI", "Dünya Masterlar Oyunları Taipei 2025", "2025-05-17", "2025-05-30", "Taipei", "TW", "Tayvan", "world", "IMGA", "https://imga.ch/")
    )

    // =========================================================================
    // 4. EMA AVRUPA MASTERLAR AÇIK SAHA ATLETİZM ŞAMPİYONALARI (1978 - 2025)
    // =========================================================================
    private val emaEuropeanChampionshipsStadia = listOf(
        archiveItem("EMA_1978_VIAREGGIO", "1. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1978", "1978-09-18", "1978-09-23", "Viareggio", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1980_HELSINKI", "2. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1980", "1980-08-06", "1980-08-10", "Helsinki", "FI", "Finlandiya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1982_STRASBOURG", "3. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1982", "1982-07-14", "1982-07-18", "Strazburg", "FR", "Fransa", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1984_BRIGHTON", "4. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1984", "1984-08-20", "1984-08-25", "Brighton", "GB", "Birleşik Krallık", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1986_MALMO", "5. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1986", "1986-07-28", "1986-08-02", "Malmö", "SE", "İsveç", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1988_VERONA", "6. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1988", "1988-06-25", "1988-07-02", "Verona", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1990_BUDAPEST", "7. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1990", "1990-06-30", "1990-07-07", "Budapeşte", "HU", "Macaristan", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1992_KRISTIANSAND", "8. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1992", "1992-06-26", "1992-07-04", "Kristiansand", "NO", "Norveç", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1994_ATHENS", "9. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1994", "1994-06-03", "1994-06-12", "Atina", "GR", "Yunanistan", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1996_MALMO", "10. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1996", "1996-07-19", "1996-07-27", "Malmö", "SE", "İsveç", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1998_CESENATICO", "11. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 1998", "1998-09-11", "1998-09-19", "Cesenatico", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2000_JYVASKYLA", "12. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2000", "2000-07-06", "2000-07-16", "Jyväskylä", "FI", "Finlandiya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2002_POTSDAM", "13. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2002", "2002-08-15", "2002-08-25", "Potsdam", "DE", "Almanya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2004_AARHUS", "14. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2004", "2004-07-22", "2004-08-01", "Aarhus", "DK", "Danimarka", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2006_POZNAN", "15. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2006", "2006-07-20", "2006-07-30", "Poznań", "PL", "Polonya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2008_LJUBLJANA", "16. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2008", "2008-07-23", "2008-08-03", "Ljubljana", "SI", "Slovenya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2010_NYIREGYHAZA", "17. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2010", "2010-07-15", "2010-07-24", "Nyíregyháza", "HU", "Macaristan", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2012_ZITTAU", "18. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2012", "2012-08-16", "2012-08-25", "Zittau", "DE", "Almanya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2014_IZMIR", "19. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2014", "2014-08-22", "2014-08-31", "İzmir", "TR", "Türkiye", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2017_AARHUS", "20. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2017", "2017-07-27", "2017-08-06", "Aarhus", "DK", "Danimarka", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2019_JESOLO", "21. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2019", "2019-09-05", "2019-09-15", "Jesolo", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2023_PESCARA", "22. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2023", "2023-09-21", "2023-10-01", "Pescara", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2025_MADEIRA", "23. Avrupa Masterlar Açık Saha Atletizm Şampiyonası 2025", "2025-10-09", "2025-10-19", "Madeira", "PT", "Portekiz", "europe", "EMA", "https://european-masters-athletics.org/")
    )

    // =========================================================================
    // 5. EMA AVRUPA MASTERLAR SALON ATLETİZM ŞAMPİYONALARI (1997 - 2024)
    // =========================================================================
    private val emaEuropeanChampionshipsIndoor = listOf(
        archiveItem("EMAI_1997_BIRMINGHAM", "1. Avrupa Masterlar Salon Atletizm Şampiyonası 1997", "1997-02-28", "1997-03-02", "Birmingham", "GB", "Birleşik Krallık", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_1999_MALMO", "2. Avrupa Masterlar Salon Atletizm Şampiyonası 1999", "1999-03-05", "1999-03-07", "Malmö", "SE", "İsveç", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2001_BORDEAUX", "3. Avrupa Masterlar Salon Atletizm Şampiyonası 2001", "2001-03-08", "2001-03-11", "Bordeaux", "FR", "Fransa", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2003_SANSEBASTIAN", "4. Avrupa Masterlar Salon Atletizm Şampiyonası 2003", "2003-03-06", "2003-03-09", "San Sebastián", "ES", "İspanya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2005_ESKILSTUNA", "5. Avrupa Masterlar Salon Atletizm Şampiyonası 2005", "2005-03-10", "2005-03-13", "Eskilstuna", "SE", "İsveç", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2007_HELSINKI", "6. Avrupa Masterlar Salon Atletizm Şampiyonası 2007", "2007-03-22", "2007-03-25", "Helsinki", "FI", "Finlandiya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2009_ANCONA", "7. Avrupa Masterlar Salon Atletizm Şampiyonası 2009", "2009-03-25", "2009-03-29", "Ancona", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2011_GENT", "8. Avrupa Masterlar Salon Atletizm Şampiyonası 2011", "2011-03-16", "2011-03-20", "Gent", "BE", "Belçika", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2013_SANSEBASTIAN", "9. Avrupa Masterlar Salon Atletizm Şampiyonası 2013", "2013-03-19", "2013-03-24", "San Sebastián", "ES", "İspanya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2015_TORUN", "10. Avrupa Masterlar Salon Atletizm Şampiyonası 2015", "2015-03-23", "2015-03-28", "Toruń", "PL", "Polonya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2016_ANCONA", "11. Avrupa Masterlar Salon Atletizm Şampiyonası 2016", "2016-03-29", "2016-04-03", "Ancona", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2018_MADRID", "12. Avrupa Masterlar Salon Atletizm Şampiyonası 2018", "2018-03-19", "2018-03-24", "Madrid", "ES", "İspanya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2022_BRAGA", "13. Avrupa Masterlar Salon Atletizm Şampiyonası 2022", "2022-02-20", "2022-02-27", "Braga", "PT", "Portekiz", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2024_TORUN", "14. Avrupa Masterlar Salon Atletizm Şampiyonası 2024", "2024-03-17", "2024-03-23", "Toruń", "PL", "Polonya", "europe", "EMA", "https://european-masters-athletics.org/")
    )

    // =========================================================================
    // 6. EMA AVRUPA MASTERLAR YOL, YARI MARATON, KROS VE MARATON ŞAMPİYONALARI (1989 - 2025)
    // =========================================================================
    private val emaEuropeanNonStadiaArchive = listOf(
        archiveItem("EMANS_1989_BRUGES", "1. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 1989", "1989-05-13", "1989-05-14", "Brugge", "BE", "Belçika", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_1991_DOLO", "2. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 1991", "1991-05-25", "1991-05-26", "Dolo", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_1993_UPICE", "3. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 1993", "1993-05-29", "1993-05-30", "Úpice", "CZ", "Çekya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_1995_VALLADOLID", "4. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 1995", "1995-05-20", "1995-05-21", "Valladolid", "ES", "İspanya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_1997_HAGUE", "5. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 1997", "1997-05-31", "1997-06-01", "Lahey", "NL", "Hollanda", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_1999_BRUGES", "6. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 1999", "1999-06-12", "1999-06-13", "Brugge", "BE", "Belçika", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2001_QORMI", "7. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2001", "2001-05-12", "2001-05-13", "Qormi", "MT", "Malta", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2003_UPICE", "8. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2003", "2003-05-24", "2003-05-25", "Úpice", "CZ", "Çekya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2005_VILAREAL", "9. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2005", "2005-05-13", "2005-05-15", "Vila Real de Santo António", "PT", "Portekiz", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2007_REGENSBURG", "10. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2007", "2007-05-18", "2007-05-20", "Regensburg", "DE", "Almanya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2009_AARHUS", "11. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2009", "2009-05-29", "2009-05-31", "Aarhus", "DK", "Danimarka", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2011_THIONVILLE", "12. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2011", "2011-05-13", "2011-05-15", "Thionville", "FR", "Fransa", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2013_UPICE", "13. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2013", "2013-05-24", "2013-05-26", "Úpice", "CZ", "Çekya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2015_GROSSETO", "14. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2015", "2015-05-15", "2015-05-17", "Grosseto", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2016_VILAREAL", "15. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2016", "2016-05-20", "2016-05-22", "Vila Real de Santo António", "PT", "Portekiz", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2018_ALICANTE", "16. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2018", "2018-05-18", "2018-05-20", "Alicante", "ES", "İspanya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2022_GROSSETO", "18. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2022", "2022-05-12", "2022-05-15", "Grosseto", "IT", "İtalya", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMANS_2024_PORTOSANTO", "19. Avrupa Masterlar Yol ve Kros Atletizm Şampiyonası (Non-Stadia) 2024", "2024-05-16", "2024-05-19", "Porto Santo", "PT", "Portekiz", "europe", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAMR_2025_JYVASKYLA", "Avrupa Masterlar Maraton Şampiyonası Jyväskylä 2025", "2025-09-06", "2025-09-07", "Jyväskylä", "FI", "Finlandiya", "europe", "EMA", "https://european-masters-athletics.org/")
    )

    // =========================================================================
    // 7. BALKAN MASTERLAR AÇIK SAHA ATLETİZM ŞAMPİYONALARI (BAMACS: 1991 - 2025)
    // =========================================================================
    private val balkanMastersStadiaArchive = listOf(
        archiveItem("BAMACS_1991_ATHENS", "1. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1991", "1991-09-21", "1991-09-22", "Atina", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_1992_ISTANBUL", "2. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1992", "1992-09-19", "1992-09-20", "İstanbul", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_1993_BUCHAREST", "3. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1993", "1993-09-18", "1993-09-19", "Bükreş", "RO", "Romanya", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_1994_SOFIA", "4. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1994", "1994-09-17", "1994-09-18", "Sofya", "BG", "Bulgaristan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_1995_ISTANBUL", "5. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1995", "1995-09-16", "1995-09-17", "İstanbul", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_1996_ATHENS", "6. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1996", "1996-09-21", "1996-09-22", "Atina", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_1997_BUCHAREST", "7. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1997", "1997-09-20", "1997-09-21", "Bükreş", "RO", "Romanya", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_1998_THESSALONIKI", "8. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1998", "1998-09-19", "1998-09-20", "Selanik", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_1999_IZMIR", "9. Balkan Masterlar Açık Saha Atletizm Şampiyonası 1999", "1999-09-18", "1999-09-19", "İzmir", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2000_KATERINI", "10. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2000", "2000-09-16", "2000-09-17", "Katerini", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2001_BELGRADE", "11. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2001", "2001-09-15", "2001-09-16", "Belgrad", "RS", "Sırbistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2002_ISTANBUL", "12. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2002", "2002-09-21", "2002-09-22", "İstanbul", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2003_KATERINI", "13. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2003", "2003-09-20", "2003-09-21", "Katerini", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2004_ISTANBUL", "14. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2004", "2004-09-18", "2004-09-19", "İstanbul", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2005_NOVISAD", "15. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2005", "2005-09-17", "2005-09-18", "Novi Sad", "RS", "Sırbistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2006_ATHENS", "16. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2006", "2006-09-23", "2006-09-24", "Atina", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2007_PLOVDIV", "17. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2007", "2007-09-29", "2007-09-30", "Filibe", "BG", "Bulgaristan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2008_BAR", "18. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2008", "2008-09-26", "2008-09-28", "Bar", "ME", "Karadağ", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2009_IZMIR", "19. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2009", "2009-09-25", "2009-09-27", "İzmir", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2010_LARISSA", "20. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2010", "2010-09-17", "2010-09-19", "Larisa", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2011_DOMZALE", "21. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2011", "2011-09-02", "2011-09-04", "Domžale", "SI", "Slovenya", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2012_IZMIR", "22. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2012", "2012-09-21", "2012-09-23", "İzmir", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2013_ZAGREB", "23. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2013", "2013-09-06", "2013-09-08", "Zagreb", "HR", "Hırvatistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2014_BUCHAREST", "24. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2014", "2014-09-19", "2014-09-21", "Bükreş", "RO", "Romanya", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2015_THESSALONIKI", "25. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2015", "2015-09-18", "2015-09-20", "Selanik", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2016_NOVISAD", "26. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2016", "2016-09-23", "2016-09-25", "Novi Sad", "RS", "Sırbistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2017_STARAZAGORA", "27. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2017", "2017-09-22", "2017-09-24", "Eski Zağra", "BG", "Bulgaristan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2018_CELJE", "28. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2018", "2018-09-21", "2018-09-23", "Celje", "SI", "Slovenya", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2019_BUCHAREST", "29. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2019", "2019-09-19", "2019-09-22", "Bükreş", "RO", "Romanya", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2021_KORCE", "30. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2021", "2021-09-17", "2021-09-19", "Görice (Korçë)", "AL", "Arnavutluk", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2022_THESSALONIKI", "31. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2022", "2022-09-23", "2022-09-25", "Selanik", "GR", "Yunanistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2023_IZMIR", "32. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2023", "2023-09-29", "2023-10-01", "İzmir", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2024_BAR", "33. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2024", "2024-09-27", "2024-09-29", "Bar", "ME", "Karadağ", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACS_2025_VARAZDIN", "34. Balkan Masterlar Açık Saha Atletizm Şampiyonası 2025", "2025-08-29", "2025-08-31", "Varaždin", "HR", "Hırvatistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/")
    )

    // =========================================================================
    // 8. BALKAN MASTERLAR SALON ATLETİZM ŞAMPİYONALARI (BAMACI: 2015 - 2025)
    // =========================================================================
    private val balkanMastersIndoorArchive = listOf(
        archiveItem("BAMACI_2015_ISTANBUL", "1. Balkan Masterlar Salon Atletizm Şampiyonası 2015", "2015-02-21", "2015-02-22", "İstanbul", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2016_BUCHAREST", "2. Balkan Masterlar Salon Atletizm Şampiyonası 2016", "2016-03-12", "2016-03-13", "Bükreş", "RO", "Romanya", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2017_STARAZAGORA", "3. Balkan Masterlar Salon Atletizm Şampiyonası 2017", "2017-03-11", "2017-03-12", "Eski Zağra", "BG", "Bulgaristan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2018_BELGRADE", "4. Balkan Masterlar Salon Atletizm Şampiyonası 2018", "2018-03-10", "2018-03-11", "Belgrad", "RS", "Sırbistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2019_ISTANBUL", "5. Balkan Masterlar Salon Atletizm Şampiyonası 2019", "2019-03-02", "2019-03-03", "İstanbul", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2020_BELGRADE", "6. Balkan Masterlar Salon Atletizm Şampiyonası 2020", "2020-03-07", "2020-03-08", "Belgrad", "RS", "Sırbistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2022_NOVOMESTO", "7. Balkan Masterlar Salon Atletizm Şampiyonası 2022", "2022-03-12", "2022-03-13", "Novo Mesto", "SI", "Slovenya", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2023_ISTANBUL", "8. Balkan Masterlar Salon Atletizm Şampiyonası 2023", "2023-03-18", "2023-03-19", "İstanbul", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2024_ISTANBUL", "9. Balkan Masterlar Salon Atletizm Şampiyonası 2024", "2024-03-09", "2024-03-10", "İstanbul", "TR", "Türkiye", "balkan", "BAMA", "https://balkan-masters-athletics.org/"),
        archiveItem("BAMACI_2025_BELGRADE", "10. Balkan Masterlar Salon Atletizm Şampiyonası 2025", "2025-03-08", "2025-03-09", "Belgrad", "RS", "Sırbistan", "balkan", "BAMA", "https://balkan-masters-athletics.org/")
    )

    // =========================================================================
    // 9. TAF TÜRKİYE MASTERLAR ATLETİZM ŞAMPİYONALARI (SALON & AÇIK SAHA: 2010 - 2025)
    // =========================================================================
    private val tafTurkishMastersArchive = listOf(
        archiveItem("TAF_2010_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2010", "2010-06-19", "2010-06-20", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2011_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2011", "2011-06-18", "2011-06-19", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2012_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2012", "2012-02-18", "2012-02-19", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2012_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2012", "2012-06-16", "2012-06-17", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2013_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2013", "2013-02-16", "2013-02-17", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2013_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2013", "2013-06-15", "2013-06-16", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2014_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2014", "2014-02-15", "2014-02-16", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2014_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2014", "2014-06-14", "2014-06-15", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2015_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2015", "2015-02-07", "2015-02-08", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2015_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2015", "2015-06-13", "2015-06-14", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2016_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2016", "2016-02-20", "2016-02-21", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2016_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2016", "2016-08-20", "2016-08-21", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2017_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2017", "2017-02-18", "2017-02-19", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2017_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2017", "2017-07-01", "2017-07-02", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2018_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2018", "2018-02-24", "2018-02-25", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2018_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2018", "2018-06-30", "2018-07-01", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2019_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2019", "2019-02-16", "2019-02-17", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2019_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2019", "2019-07-20", "2019-07-21", "Konya", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2020_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2020", "2020-02-22", "2020-02-23", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2020_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2020", "2020-09-12", "2020-09-13", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2021_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2021", "2021-03-27", "2021-03-28", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2021_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2021", "2021-07-03", "2021-07-04", "Bursa", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2022_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2022", "2022-01-29", "2022-01-30", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2022_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2022", "2022-08-20", "2022-08-21", "Bursa", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2023_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2023", "2023-03-04", "2023-03-05", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2023_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2023", "2023-08-19", "2023-08-20", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2024_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2024", "2024-02-03", "2024-02-04", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2024_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2024", "2024-08-24", "2024-08-25", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2025_INDOOR", "Türkiye Masterlar Salon Atletizm Şampiyonası 2025", "2025-02-08", "2025-02-09", "İstanbul", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/"),
        archiveItem("TAF_2025_OUTDOOR", "Türkiye Masterlar Açık Saha Atletizm Şampiyonası 2025", "2025-07-05", "2025-07-06", "İzmir", "TR", "Türkiye", "national", "TAF", "https://www.taf.org.tr/")
    )

    // =========================================================================
    // 10. AVRUPA ULUSAL MASTER ŞAMPİYONALARI (BMAF, FFA, DLV, FIDAL, RFEA: 2018 - 2025)
    // =========================================================================
    private val europeanNationalMastersArchive = listOf(
        // Birleşik Krallık (BMAF)
        archiveItem("BMAF_2018_OUTDOOR", "Birleşik Krallık Masterlar Açık Saha Atletizm Şampiyonası 2018", "2018-08-25", "2018-08-26", "Birmingham", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),
        archiveItem("BMAF_2019_OUTDOOR", "Birleşik Krallık Masterlar Açık Saha Atletizm Şampiyonası 2019", "2019-08-10", "2019-08-11", "Birmingham", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),
        archiveItem("BMAF_2021_OUTDOOR", "Birleşik Krallık Masterlar Açık Saha Atletizm Şampiyonası 2021", "2021-09-03", "2021-09-05", "Derby", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),
        archiveItem("BMAF_2022_OUTDOOR", "Birleşik Krallık Masterlar Açık Saha Atletizm Şampiyonası 2022", "2022-08-26", "2022-08-28", "Derby", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),
        archiveItem("BMAF_2023_OUTDOOR", "Birleşik Krallık Masterlar Açık Saha Atletizm Şampiyonası 2023", "2023-09-01", "2023-09-03", "Derby", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),
        archiveItem("BMAF_2024_INDOOR", "Birleşik Krallık Masterlar Salon Atletizm Şampiyonası 2024", "2024-02-17", "2024-02-18", "Sheffield", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),
        archiveItem("BMAF_2024_OUTDOOR", "Birleşik Krallık Masterlar Açık Saha Atletizm Şampiyonası 2024", "2024-07-26", "2024-07-28", "Derby", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),
        archiveItem("BMAF_2025_INDOOR", "Birleşik Krallık Masterlar Salon Atletizm Şampiyonası 2025", "2025-02-22", "2025-02-23", "Londra", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),
        archiveItem("BMAF_2025_OUTDOOR", "Birleşik Krallık Masterlar Açık Saha Atletizm Şampiyonası 2025", "2025-08-29", "2025-08-31", "Derby", "GB", "Birleşik Krallık", "national", "BMAF", "https://bmaf.org.uk/"),

        // Fransa (FFA)
        archiveItem("FFA_2018_OUTDOOR", "Fransa Masterlar Açık Saha Atletizm Şampiyonası 2018", "2018-06-15", "2018-06-17", "Angers", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),
        archiveItem("FFA_2019_OUTDOOR", "Fransa Masterlar Açık Saha Atletizm Şampiyonası 2019", "2019-06-14", "2019-06-16", "Dreux", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),
        archiveItem("FFA_2021_OUTDOOR", "Fransa Masterlar Açık Saha Atletizm Şampiyonası 2021", "2021-09-24", "2021-09-26", "Albi", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),
        archiveItem("FFA_2022_OUTDOOR", "Fransa Masterlar Açık Saha Atletizm Şampiyonası 2022", "2022-06-10", "2022-06-12", "Châteauroux", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),
        archiveItem("FFA_2023_OUTDOOR", "Fransa Masterlar Açık Saha Atletizm Şampiyonası 2023", "2023-06-23", "2023-06-25", "Laval", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),
        archiveItem("FFA_2024_INDOOR", "Fransa Masterlar Salon Atletizm Şampiyonası 2024", "2024-03-01", "2024-03-03", "Lyon", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),
        archiveItem("FFA_2024_OUTDOOR", "Fransa Masterlar Açık Saha Atletizm Şampiyonası 2024", "2024-06-13", "2024-06-16", "Thonon-les-Bains", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),
        archiveItem("FFA_2025_INDOOR", "Fransa Masterlar Salon Atletizm Şampiyonası 2025", "2025-03-07", "2025-03-09", "Miramas", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),
        archiveItem("FFA_2025_OUTDOOR", "Fransa Masterlar Açık Saha Atletizm Şampiyonası 2025", "2025-06-20", "2025-06-22", "Saint-Étienne", "FR", "Fransa", "national", "FFA", "https://www.athle.fr/"),

        // Almanya (DLV)
        archiveItem("DLV_2018_OUTDOOR", "Almanya Masterlar Açık Saha Atletizm Şampiyonası 2018", "2018-06-29", "2018-07-01", "Mönchengladbach", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),
        archiveItem("DLV_2019_OUTDOOR", "Almanya Masterlar Açık Saha Atletizm Şampiyonası 2019", "2019-07-12", "2019-07-14", "Leinefelde-Worbis", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),
        archiveItem("DLV_2021_OUTDOOR", "Almanya Masterlar Açık Saha Atletizm Şampiyonası 2021", "2021-09-10", "2021-09-12", "Baunatal", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),
        archiveItem("DLV_2022_OUTDOOR", "Almanya Masterlar Açık Saha Atletizm Şampiyonası 2022", "2022-09-17", "2022-09-18", "Erfurt", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),
        archiveItem("DLV_2023_OUTDOOR", "Almanya Masterlar Açık Saha Atletizm Şampiyonası 2023", "2023-08-12", "2023-08-13", "Mönchengladbach", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),
        archiveItem("DLV_2024_INDOOR", "Almanya Masterlar Salon Atletizm Şampiyonası 2024", "2024-03-02", "2024-03-03", "Dortmund", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),
        archiveItem("DLV_2024_OUTDOOR", "Almanya Masterlar Açık Saha Atletizm Şampiyonası 2024", "2024-06-14", "2024-06-16", "Erding", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),
        archiveItem("DLV_2025_INDOOR", "Almanya Masterlar Salon Atletizm Şampiyonası 2025", "2025-03-01", "2025-03-02", "Frankfurt", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),
        archiveItem("DLV_2025_OUTDOOR", "Almanya Masterlar Açık Saha Atletizm Şampiyonası 2025", "2025-08-22", "2025-08-24", "Gotha", "DE", "Almanya", "national", "DLV", "https://www.leichtathletik.de/"),

        // İtalya (FIDAL)
        archiveItem("FIDAL_2018_OUTDOOR", "İtalya Masterlar Açık Saha Atletizm Şampiyonası 2018", "2018-06-29", "2018-07-01", "Arezzo", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2019_OUTDOOR", "İtalya Masterlar Açık Saha Atletizm Şampiyonası 2019", "2019-07-05", "2019-07-07", "Campi Bisenzio", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2020_OUTDOOR", "İtalya Masterlar Açık Saha Atletizm Şampiyonası 2020", "2020-10-09", "2020-10-11", "Arezzo", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2021_OUTDOOR", "İtalya Masterlar Açık Saha Atletizm Şampiyonası 2021", "2021-09-16", "2021-09-19", "Rieti", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2022_OUTDOOR", "İtalya Masterlar Açık Saha Atletizm Şampiyonası 2022", "2022-06-10", "2022-06-12", "Grosseto", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2023_OUTDOOR", "İtalya Masterlar Açık Saha Atletizm Şampiyonası 2023", "2023-06-23", "2023-06-25", "Agropoli", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2024_INDOOR", "İtalya Masterlar Salon Atletizm Şampiyonası 2024", "2024-02-22", "2024-02-25", "Ancona", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2024_OUTDOOR", "İtalya Masterlar Açık Saha Atletizm Şampiyonası 2024", "2024-06-21", "2024-06-23", "Roma", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2025_INDOOR", "İtalya Masterlar Salon Atletizm Şampiyonası 2025", "2025-02-27", "2025-03-02", "Ancona", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),
        archiveItem("FIDAL_2025_OUTDOOR", "İtalya Masterlar Açık Saha Atletizm Şampiyonası 2025", "2025-06-20", "2025-06-22", "Misano Adriatico", "IT", "İtalya", "national", "FIDAL", "https://www.fidal.it/"),

        // İspanya (RFEA)
        archiveItem("RFEA_2018_OUTDOOR", "İspanya Masterlar Açık Saha Atletizm Şampiyonası 2018", "2018-06-29", "2018-07-01", "Vitoria-Gasteiz", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/"),
        archiveItem("RFEA_2019_OUTDOOR", "İspanya Masterlar Açık Saha Atletizm Şampiyonası 2019", "2019-06-28", "2019-06-30", "Sagunto", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/"),
        archiveItem("RFEA_2021_OUTDOOR", "İspanya Masterlar Açık Saha Atletizm Şampiyonası 2021", "2021-06-18", "2021-06-20", "Málaga", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/"),
        archiveItem("RFEA_2022_OUTDOOR", "İspanya Masterlar Açık Saha Atletizm Şampiyonası 2022", "2022-06-17", "2022-06-19", "Arona", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/"),
        archiveItem("RFEA_2023_OUTDOOR", "İspanya Masterlar Açık Saha Atletizm Şampiyonası 2023", "2023-06-23", "2023-06-25", "Jerez de la Frontera", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/"),
        archiveItem("RFEA_2024_INDOOR", "İspanya Masterlar Salon Atletizm Şampiyonası 2024", "2024-03-01", "2024-03-03", "San Sebastián", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/"),
        archiveItem("RFEA_2024_OUTDOOR", "İspanya Masterlar Açık Saha Atletizm Şampiyonası 2024", "2024-06-28", "2024-06-30", "Alcobendas", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/"),
        archiveItem("RFEA_2025_INDOOR", "İspanya Masterlar Salon Atletizm Şampiyonası 2025", "2025-02-28", "2025-03-02", "Antequera", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/"),
        archiveItem("RFEA_2025_OUTDOOR", "İspanya Masterlar Açık Saha Atletizm Şampiyonası 2025", "2025-06-27", "2025-06-29", "La Nucía", "ES", "İspanya", "national", "RFEA", "https://www.rfea.es/")
    )
}
