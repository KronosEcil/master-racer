package com.ecil.masterracer.data.repository

import com.ecil.masterracer.data.model.Competition

/**
 * 1975 - 2025 yılları arasında gerçekleştirilmiş resmi Master Atletizm Şampiyonaları
 * ve Masters Games yerel arşivi.
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
            source_url = sourceUrl,
            status = "completed"
        )
    }

    val competitions: List<Competition> by lazy {
        wmaWorldChampionshipsStadia +
            wmaWorldChampionshipsIndoor +
            worldMastersGamesArchive +
            emaEuropeanChampionshipsStadia +
            emaEuropeanChampionshipsIndoor
    }

    // =========================================================================
    // 1. WMA DÜNYA MASTERLAR ATLETİZM ŞAMPİYONALARI (AÇIK SAHA / STADIA: 1975 - 2024)
    // =========================================================================
    private val wmaWorldChampionshipsStadia = listOf(
        archiveItem("WMA_1975_TORONTO", "1st World Masters Athletics Championships Stadia 1975", "1975-08-11", "1975-08-16", "Toronto", "CA", "Kanada", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1977_GOTHENBURG", "2nd World Masters Athletics Championships Stadia 1977", "1977-08-08", "1977-08-13", "Gothenburg", "SE", "İsveç", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1979_HANNOVER", "3rd World Masters Athletics Championships Stadia 1979", "1979-07-27", "1979-08-02", "Hannover", "DE", "Almanya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1981_CHRISTCHURCH", "4th World Masters Athletics Championships Stadia 1981", "1981-01-07", "1981-01-14", "Christchurch", "NZ", "Yeni Zelanda", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1983_SANJUAN", "5th World Masters Athletics Championships Stadia 1983", "1983-09-23", "1983-09-30", "San Juan", "US", "ABD (Porto Riko)", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1985_ROME", "6th World Masters Athletics Championships Stadia 1985", "1985-06-22", "1985-06-30", "Rome", "IT", "İtalya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1987_MELBOURNE", "7th World Masters Athletics Championships Stadia 1987", "1987-11-29", "1987-12-06", "Melbourne", "AU", "Avustralya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1989_EUGENE", "8th World Masters Athletics Championships Stadia 1989", "1989-07-27", "1989-08-06", "Eugene", "US", "ABD", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1991_TURKU", "9th World Masters Athletics Championships Stadia 1991", "1991-07-18", "1991-07-28", "Turku", "FI", "Finlandiya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1993_MIYAZAKI", "10th World Masters Athletics Championships Stadia 1993", "1993-10-07", "1993-10-17", "Miyazaki", "JP", "Japonya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1995_BUFFALO", "11th World Masters Athletics Championships Stadia 1995", "1995-07-13", "1995-07-23", "Buffalo", "US", "ABD", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1997_DURBAN", "12th World Masters Athletics Championships Stadia 1997", "1997-07-17", "1997-07-27", "Durban", "ZA", "Güney Afrika", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_1999_GATESHEAD", "13th World Masters Athletics Championships Stadia 1999", "1999-07-29", "1999-08-08", "Gateshead", "GB", "Birleşik Krallık", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2001_BRISBANE", "14th World Masters Athletics Championships Stadia 2001", "2001-07-04", "2001-07-14", "Brisbane", "AU", "Avustralya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2003_CAROLINA", "15th World Masters Athletics Championships Stadia 2003", "2003-07-01", "2003-07-13", "Carolina", "US", "ABD (Porto Riko)", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2005_SANSEBASTIAN", "16th World Masters Athletics Championships Stadia 2005", "2005-08-22", "2005-09-03", "San Sebastián", "ES", "İspanya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2007_RICCIONE", "17th World Masters Athletics Championships Stadia 2007", "2007-09-04", "2007-09-15", "Riccione", "IT", "İtalya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2009_LAHTI", "18th World Masters Athletics Championships Stadia 2009", "2009-07-28", "2009-08-08", "Lahti", "FI", "Finlandiya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2011_SACRAMENTO", "19th World Masters Athletics Championships Stadia 2011", "2011-07-06", "2011-07-17", "Sacramento", "US", "ABD", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2013_PORTOALEGRE", "20th World Masters Athletics Championships Stadia 2013", "2013-10-16", "2013-10-27", "Porto Alegre", "BR", "Brezilya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2015_LYON", "21st World Masters Athletics Championships Stadia 2015", "2015-08-04", "2015-08-16", "Lyon", "FR", "Fransa", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2016_PERTH", "22nd World Masters Athletics Championships Stadia 2016", "2016-10-26", "2016-11-06", "Perth", "AU", "Avustralya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2018_MALAGA", "23rd World Masters Athletics Championships Stadia 2018", "2018-09-04", "2018-09-16", "Málaga", "ES", "İspanya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2022_TAMPERE", "24th World Masters Athletics Championships Stadia 2022", "2022-06-29", "2022-07-10", "Tampere", "FI", "Finlandiya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMA_2024_GOTHENBURG", "25th World Masters Athletics Championships Stadia 2024", "2024-08-13", "2024-08-25", "Gothenburg", "SE", "İsveç", "international", "WMA", "https://world-masters-athletics.org/")
    )

    // =========================================================================
    // 2. WMA DÜNYA MASTERLAR SALON ŞAMPİYONALARI (INDOOR: 2004 - 2025)
    // =========================================================================
    private val wmaWorldChampionshipsIndoor = listOf(
        archiveItem("WMAI_2004_SINDELFINGEN", "1st World Masters Athletics Championships Indoor 2004", "2004-03-10", "2004-03-14", "Sindelfingen", "DE", "Almanya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2006_LINZ", "2nd World Masters Athletics Championships Indoor 2006", "2006-03-15", "2006-03-20", "Linz", "AT", "Avusturya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2008_CLERMONT", "3rd World Masters Athletics Championships Indoor 2008", "2008-03-17", "2008-03-22", "Clermont-Ferrand", "FR", "Fransa", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2010_KAMLOOPS", "4th World Masters Athletics Championships Indoor 2010", "2010-03-02", "2010-03-07", "Kamloops", "CA", "Kanada", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2012_JYVASKYLA", "5th World Masters Athletics Championships Indoor 2012", "2012-04-03", "2012-04-08", "Jyväskylä", "FI", "Finlandiya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2014_BUDAPEST", "6th World Masters Athletics Championships Indoor 2014", "2014-03-25", "2014-03-30", "Budapest", "HU", "Macaristan", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2017_DAEGU", "7th World Masters Athletics Championships Indoor 2017", "2017-03-19", "2017-03-25", "Daegu", "KR", "Güney Kore", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2019_TORUN", "8th World Masters Athletics Championships Indoor 2019", "2019-03-24", "2019-03-30", "Toruń", "PL", "Polonya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2023_TORUN", "9th World Masters Athletics Championships Indoor 2023", "2023-03-26", "2023-04-01", "Toruń", "PL", "Polonya", "international", "WMA", "https://world-masters-athletics.org/"),
        archiveItem("WMAI_2025_GAINESVILLE", "10th World Masters Athletics Championships Indoor 2025", "2025-03-23", "2025-03-30", "Gainesville", "US", "ABD", "international", "WMA", "https://world-masters-athletics.org/")
    )

    // =========================================================================
    // 3. WORLD MASTERS GAMES & EUROPEAN MASTERS GAMES (IMGA: 1985 - 2025)
    // =========================================================================
    private val worldMastersGamesArchive = listOf(
        archiveItem("WMG_1985_TORONTO", "World Masters Games Toronto 1985", "1985-08-07", "1985-08-25", "Toronto", "CA", "Kanada", "international", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_1989_AALBORG", "World Masters Games Aalborg, Aarhus & Herning 1989", "1989-07-22", "1989-08-06", "Aalborg", "DK", "Danimarka", "international", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_1994_BRISBANE", "World Masters Games Brisbane 1994", "1994-09-26", "1994-10-08", "Brisbane", "AU", "Avustralya", "international", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_1998_PORTLAND", "World Masters Games Portland 1998", "1998-08-09", "1998-08-22", "Portland", "US", "ABD", "international", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2002_MELBOURNE", "World Masters Games Melbourne 2002", "2002-10-05", "2002-10-13", "Melbourne", "AU", "Avustralya", "international", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2005_EDMONTON", "World Masters Games Edmonton 2005", "2005-07-22", "2005-07-31", "Edmonton", "CA", "Kanada", "international", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2008_MALMO", "European Masters Games Malmö 2008", "2008-08-29", "2008-09-07", "Malmö", "SE", "İsveç", "european", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2009_SYDNEY", "World Masters Games Sydney 2009", "2009-10-10", "2009-10-18", "Sydney", "AU", "Avustralya", "international", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2011_LIGNANO", "European Masters Games Lignano Sabbiadoro 2011", "2011-09-10", "2011-09-20", "Lignano Sabbiadoro", "IT", "İtalya", "european", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2013_TORINO", "World Masters Games Torino 2013", "2013-08-02", "2013-08-11", "Turin", "IT", "İtalya", "international", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2015_NICE", "European Masters Games Nice 2015", "2015-10-01", "2015-10-11", "Nice", "FR", "Fransa", "european", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2017_AUCKLAND", "World Masters Games Auckland 2017", "2017-04-21", "2017-04-30", "Auckland", "NZ", "Yeni Zelanda", "international", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2019_TORINO", "European Masters Games Torino 2019", "2019-07-26", "2019-08-04", "Turin", "IT", "İtalya", "european", "IMGA", "https://imga.ch/"),
        archiveItem("EMG_2023_TAMPERE", "European Masters Games Tampere 2023", "2023-06-26", "2023-07-09", "Tampere", "FI", "Finlandiya", "european", "IMGA", "https://imga.ch/"),
        archiveItem("WMG_2025_TAIPEI", "World Masters Games Taipei & New Taipei City 2025", "2025-05-17", "2025-05-30", "Taipei", "TW", "Tayvan", "international", "IMGA", "https://imga.ch/")
    )

    // =========================================================================
    // 4. EMA AVRUPA MASTERLAR ATLETİZM ŞAMPİYONALARI (AÇIK SAHA / STADIA: 1978 - 2025)
    // =========================================================================
    private val emaEuropeanChampionshipsStadia = listOf(
        archiveItem("EMA_1978_VIAREGGIO", "1st European Masters Athletics Championships Stadia 1978", "1978-09-18", "1978-09-23", "Viareggio", "IT", "İtalya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1980_HELSINKI", "2nd European Masters Athletics Championships Stadia 1980", "1980-08-06", "1980-08-10", "Helsinki", "FI", "Finlandiya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1982_STRASBOURG", "3rd European Masters Athletics Championships Stadia 1982", "1982-07-14", "1982-07-18", "Strasbourg", "FR", "Fransa", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1984_BRIGHTON", "4th European Masters Athletics Championships Stadia 1984", "1984-08-20", "1984-08-25", "Brighton", "GB", "Birleşik Krallık", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1986_MALMO", "5th European Masters Athletics Championships Stadia 1986", "1986-07-28", "1986-08-02", "Malmö", "SE", "İsveç", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1988_VERONA", "6th European Masters Athletics Championships Stadia 1988", "1988-06-25", "1988-07-02", "Verona", "IT", "İtalya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1990_BUDAPEST", "7th European Masters Athletics Championships Stadia 1990", "1990-06-30", "1990-07-07", "Budapest", "HU", "Macaristan", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1992_KRISTIANSAND", "8th European Masters Athletics Championships Stadia 1992", "1992-06-26", "1992-07-04", "Kristiansand", "NO", "Norveç", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1994_ATHENS", "9th European Masters Athletics Championships Stadia 1994", "1994-06-03", "1994-06-12", "Athens", "GR", "Yunanistan", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1996_MALMO", "10th European Masters Athletics Championships Stadia 1996", "1996-07-19", "1996-07-27", "Malmö", "SE", "İsveç", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_1998_CESENATICO", "11th European Masters Athletics Championships Stadia 1998", "1998-09-11", "1998-09-19", "Cesenatico", "IT", "İtalya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2000_JYVASKYLA", "12th European Masters Athletics Championships Stadia 2000", "2000-07-06", "2000-07-16", "Jyväskylä", "FI", "Finlandiya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2002_POTSDAM", "13th European Masters Athletics Championships Stadia 2002", "2002-08-15", "2002-08-25", "Potsdam", "DE", "Almanya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2004_AARHUS", "14th European Masters Athletics Championships Stadia 2004", "2004-07-22", "2004-08-01", "Aarhus", "DK", "Danimarka", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2006_POZNAN", "15th European Masters Athletics Championships Stadia 2006", "2006-07-20", "2006-07-30", "Poznań", "PL", "Polonya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2008_LJUBLJANA", "16th European Masters Athletics Championships Stadia 2008", "2008-07-23", "2008-08-03", "Ljubljana", "SI", "Slovenya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2010_NYIREGYHAZA", "17th European Masters Athletics Championships Stadia 2010", "2010-07-15", "2010-07-24", "Nyíregyháza", "HU", "Macaristan", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2012_ZITTAU", "18th European Masters Athletics Championships Stadia 2012", "2012-08-16", "2012-08-25", "Zittau", "DE", "Almanya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2014_IZMIR", "19th European Masters Athletics Championships Stadia 2014", "2014-08-22", "2014-08-31", "İzmir", "TR", "Türkiye", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2017_AARHUS", "20th European Masters Athletics Championships Stadia 2017", "2017-07-27", "2017-08-06", "Aarhus", "DK", "Danimarka", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2019_JESOLO", "21st European Masters Athletics Championships Stadia 2019", "2019-09-05", "2019-09-15", "Jesolo", "IT", "İtalya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2023_PESCARA", "22nd European Masters Athletics Championships Stadia 2023", "2023-09-21", "2023-10-01", "Pescara", "IT", "İtalya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMA_2025_MADEIRA", "23rd European Masters Athletics Championships Stadia 2025", "2025-10-09", "2025-10-19", "Madeira", "PT", "Portekiz", "european", "EMA", "https://european-masters-athletics.org/")
    )

    // =========================================================================
    // 5. EMA AVRUPA MASTERLAR SALON ŞAMPİYONALARI (INDOOR: 1997 - 2024)
    // =========================================================================
    private val emaEuropeanChampionshipsIndoor = listOf(
        archiveItem("EMAI_1997_BIRMINGHAM", "1st European Masters Athletics Championships Indoor 1997", "1997-02-28", "1997-03-02", "Birmingham", "GB", "Birleşik Krallık", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_1999_MALMO", "2nd European Masters Athletics Championships Indoor 1999", "1999-03-05", "1999-03-07", "Malmö", "SE", "İsveç", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2001_BORDEAUX", "3rd European Masters Athletics Championships Indoor 2001", "2001-03-08", "2001-03-11", "Bordeaux", "FR", "Fransa", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2003_SANSEBASTIAN", "4th European Masters Athletics Championships Indoor 2003", "2003-03-06", "2003-03-09", "San Sebastián", "ES", "İspanya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2005_ESKILSTUNA", "5th European Masters Athletics Championships Indoor 2005", "2005-03-10", "2005-03-13", "Eskilstuna", "SE", "İsveç", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2007_HELSINKI", "6th European Masters Athletics Championships Indoor 2007", "2007-03-22", "2007-03-25", "Helsinki", "FI", "Finlandiya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2009_ANCONA", "7th European Masters Athletics Championships Indoor 2009", "2009-03-25", "2009-03-29", "Ancona", "IT", "İtalya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2011_GENT", "8th European Masters Athletics Championships Indoor 2011", "2011-03-16", "2011-03-20", "Ghent", "BE", "Belçika", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2013_SANSEBASTIAN", "9th European Masters Athletics Championships Indoor 2013", "2013-03-19", "2013-03-24", "San Sebastián", "ES", "İspanya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2015_TORUN", "10th European Masters Athletics Championships Indoor 2015", "2015-03-23", "2015-03-28", "Toruń", "PL", "Polonya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2016_ANCONA", "11th European Masters Athletics Championships Indoor 2016", "2016-03-29", "2016-04-03", "Ancona", "IT", "İtalya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2018_MADRID", "12th European Masters Athletics Championships Indoor 2018", "2018-03-19", "2018-03-24", "Madrid", "ES", "İspanya", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2022_BRAGA", "13th European Masters Athletics Championships Indoor 2022", "2022-02-20", "2022-02-27", "Braga", "PT", "Portekiz", "european", "EMA", "https://european-masters-athletics.org/"),
        archiveItem("EMAI_2024_TORUN", "14th European Masters Athletics Championships Indoor 2024", "2024-03-17", "2024-03-23", "Toruń", "PL", "Polonya", "european", "EMA", "https://european-masters-athletics.org/")
    )
}
