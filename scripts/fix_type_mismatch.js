const fs = require('fs');

function updateFile(path) {
    let content = fs.readFileSync(path, 'utf8');
    const oldBlock = `            } ?: availableEvents.firstOrNull { it == "100 Metre" || it == "60 Metre" }
              ?: availableEvents.first()
            selectedEvent = replacement`;
    
    // Also check 8 space indentation in BottomSheet
    const oldBlockSheet = `        } ?: availableEvents.firstOrNull { it == "100 Metre" || it == "60 Metre" }
          ?: availableEvents.first()
        selectedEvent = replacement`;

    if (content.includes(oldBlock)) {
        content = content.replace(oldBlock, `            }) ?: availableEvents.firstOrNull { it == "100 Metre" || it == "60 Metre" }
              ?: availableEvents.firstOrNull()
              ?: "100 Metre"
            selectedEvent = replacement`);
        content = content.replace('val replacement = if', 'val replacement: String = (if');
        fs.writeFileSync(path, content, 'utf8');
        console.log('Updated', path);
    } else if (content.includes(oldBlockSheet)) {
        content = content.replace(oldBlockSheet, `        }) ?: availableEvents.firstOrNull { it == "100 Metre" || it == "60 Metre" }
          ?: availableEvents.firstOrNull()
          ?: "100 Metre"
        selectedEvent = replacement`);
        content = content.replace('val replacement = if', 'val replacement: String = (if');
        fs.writeFileSync(path, content, 'utf8');
        console.log('Updated', path);
    } else {
        console.log('Block not found in', path);
    }
}

updateFile('android/app/src/main/java/com/ecil/masterracer/ui/screens/MasterRecordsScreen.kt');
updateFile('android/app/src/main/java/com/ecil/masterracer/ui/components/CategoryTargetBottomSheet.kt');
