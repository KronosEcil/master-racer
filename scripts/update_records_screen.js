const fs = require('fs');

const path = 'android/app/src/main/java/com/ecil/masterracer/ui/screens/MasterRecordsScreen.kt';
let content = fs.readFileSync(path, 'utf8');

const target = `    val availableEvents = remember(trackType) {
        MasterRecordsArchive.getAvailableEvents(trackType).ifEmpty {
            if (trackType == TrackType.OUTDOOR) {
                listOf("100 Metre", "200 Metre", "400 Metre", "800 Metre", "1500 Metre", "5000 Metre", "10.000 Metre", "110m Engelli", "Uzun Atlama", "Gülle Atma")
            } else {
                listOf("60 Metre", "200 Metre", "400 Metre", "60m Engelli", "Uzun Atlama", "Gülle Atma")
            }
        }
    }

    // Pist değiştiğinde seçili branş yoksa varsayılana geç
    LaunchedEffect(trackType, availableEvents) {
        if (selectedEvent !in availableEvents) {
            selectedEvent = availableEvents.firstOrNull() ?: if (trackType == TrackType.OUTDOOR) "100 Metre" else "60 Metre"
        }
    }`;

const replacement = `    // Seçilen yaş kategorisine, cinsiyete ve piste göre geçerli branşlar
    val availableEvents = remember(trackType, selectedCategory) {
        MasterRecordsArchive.getAvailableEvents(trackType, selectedCategory).ifEmpty {
            MasterRecordsArchive.getAvailableEvents(trackType)
        }
    }

    // Yaş kategorisi, cinsiyet veya pist değiştiğinde seçili branş bu kategoride yoksa en uygun branşa geç
    LaunchedEffect(trackType, selectedCategory, availableEvents) {
        if (selectedEvent !in availableEvents && availableEvents.isNotEmpty()) {
            val replacement = if (selectedEvent.contains("Engelli", ignoreCase = true)) {
                // Engelli branşı seçiliydi; bu kategorideki geçerli engelliyi bul (ör. Kadınlar için 100m/80m Engelli, M70 için 80m/300m Engelli)
                availableEvents.firstOrNull { it.contains("Engelli", ignoreCase = true) }
            } else if (selectedEvent.contains("Bayrak", ignoreCase = true)) {
                availableEvents.firstOrNull { it.contains("Bayrak", ignoreCase = true) }
            } else if (selectedEvent.contains("Yürüyüş", ignoreCase = true)) {
                availableEvents.firstOrNull { it.contains("Yürüyüş", ignoreCase = true) }
            } else {
                null
            } ?: availableEvents.firstOrNull { it == "100 Metre" || it == "60 Metre" }
              ?: availableEvents.first()
            selectedEvent = replacement
        }
    }`;

const normalizedContent = content.replace(/\r\n/g, '\n');
if (!normalizedContent.includes(target.replace(/\r\n/g, '\n'))) {
    console.error('Target not found in MasterRecordsScreen.kt!');
    process.exit(1);
}

const updatedContent = normalizedContent.replace(target.replace(/\r\n/g, '\n'), replacement);
fs.writeFileSync(path, updatedContent, 'utf8');
console.log('Successfully updated MasterRecordsScreen.kt');
