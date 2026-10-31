const fs = require('fs');

const path = 'android/app/src/main/java/com/ecil/masterracer/data/repository/MasterRecordsArchive.kt';
let content = fs.readFileSync(path, 'utf8');

const target = `    fun getAvailableEvents(trackType: TrackType): List<String> {
        return RECORDS.filter { it.trackType == trackType }.map { it.eventName }.distinct().sorted()
    }`;

const replacement = `    val PREFERRED_EVENT_ORDER: List<String> = listOf(
        // Kısa Mesafe
        "60 Metre", "100 Metre", "200 Metre", "400 Metre",
        // Orta ve Uzun Mesafe
        "800 Metre", "1500 Metre", "1 Mil", "3000 Metre", "5000 Metre", "10.000 Metre", "10K Yol Koşusu", "Yarı Maraton", "Maraton",
        // Engelli Koşular & Su Engelli
        "60m Engelli", "80m Engelli", "100m Engelli", "110m Engelli", "200m Engelli", "300m Engelli", "400m Engelli", "2000m Engelli", "3000m Engelli",
        // Yürüyüş
        "3000m Pist Yürüyüşü", "5000m Pist Yürüyüşü", "10.000m Pist Yürüyüşü", "20.000m Pist Yürüyüşü", "10 km Yürüyüş", "20 km Yürüyüş", "50 km Yürüyüş",
        // Atlamalar
        "Yüksek Atlama", "Sırıkla Atlama", "Uzun Atlama", "Üçadım Atlama",
        // Atmalar
        "Gülle Atma", "Disk Atma", "Çekiç Atma", "Cirit Atma", "Ağırlık Atma",
        // Çoklu Branşlar
        "Pentatlon", "Heptatlon", "Dekatlon", "Atma Pentatlonu",
        // Bayrak Koşuları
        "4x100m Bayrak", "4x200m Bayrak", "4x400m Bayrak", "4x800m Bayrak",
        "4x100m MIX Bayrak", "4x200m MIX Bayrak", "4x400m MIX Bayrak",
        "Balkan Bayrak (8-4-2-100m)"
    )

    fun sortEvents(events: List<String>): List<String> {
        return events.sortedWith(compareBy({
            val idx = PREFERRED_EVENT_ORDER.indexOf(it)
            if (idx >= 0) idx else 999
        }, { it }))
    }

    fun getAvailableEvents(trackType: TrackType, category: String? = null): List<String> {
        val records = if (category.isNullOrBlank()) {
            RECORDS.filter { it.trackType == trackType }
        } else {
            RECORDS.filter { it.trackType == trackType && it.category.equals(category, ignoreCase = true) }
        }
        val distinctEvents = records.map { it.eventName }.distinct()
        return sortEvents(distinctEvents)
    }`;

// Normalize line endings to test
const normalizedContent = content.replace(/\r\n/g, '\n');
if (!normalizedContent.includes(target.replace(/\r\n/g, '\n'))) {
    console.error('Target not found!');
    process.exit(1);
}

const updatedContent = normalizedContent.replace(target.replace(/\r\n/g, '\n'), replacement);
fs.writeFileSync(path, updatedContent, 'utf8');
console.log('Successfully updated MasterRecordsArchive.kt');
