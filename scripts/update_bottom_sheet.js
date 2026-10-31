const fs = require('fs');

const path = 'android/app/src/main/java/com/ecil/masterracer/ui/components/CategoryTargetBottomSheet.kt';
let content = fs.readFileSync(path, 'utf8');

const target = `    val availableEvents = remember(trackType) {
        MasterRecordsArchive.getAvailableEvents(trackType).ifEmpty {
            if (trackType == TrackType.OUTDOOR) {
                listOf("100 Metre", "200 Metre", "400 Metre", "800 Metre", "1500 Metre", "5000 Metre", "10.000 Metre", "Uzun Atlama", "Gülle Atma")
            } else {
                listOf("60 Metre", "200 Metre", "400 Metre", "Uzun Atlama")
            }
        }
    }

    // Seçili branş geçerli sekmede yoksa ilkini seç
    if (selectedEvent !in availableEvents) {
        selectedEvent = availableEvents.firstOrNull() ?: "100 Metre"
    }`;

const replacement = `    val availableEvents = remember(trackType, selectedCategoryView) {
        MasterRecordsArchive.getAvailableEvents(trackType, selectedCategoryView).ifEmpty {
            MasterRecordsArchive.getAvailableEvents(trackType)
        }
    }

    // Seçili branş geçerli kategoride yoksa en uygun olanı seç
    if (selectedEvent !in availableEvents && availableEvents.isNotEmpty()) {
        val replacement = if (selectedEvent.contains("Engelli", ignoreCase = true)) {
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
    }`;

const normalizedContent = content.replace(/\r\n/g, '\n');
if (!normalizedContent.includes(target.replace(/\r\n/g, '\n'))) {
    console.error('Target not found in CategoryTargetBottomSheet.kt!');
    process.exit(1);
}

const updatedContent = normalizedContent.replace(target.replace(/\r\n/g, '\n'), replacement);
fs.writeFileSync(path, updatedContent, 'utf8');
console.log('Successfully updated CategoryTargetBottomSheet.kt');
