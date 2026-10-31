const fs = require('fs');

// 1. Update MasterRecordsArchive.kt
const archivePath = 'android/app/src/main/java/com/ecil/masterracer/data/repository/MasterRecordsArchive.kt';
let archiveContent = fs.readFileSync(archivePath, 'utf8').replace(/\r\n/g, '\n');

const isEventMatchKotlin = `    fun isEventMatch(eventA: String, eventB: String): Boolean {
        if (eventA.isBlank() || eventB.isBlank()) return false
        val a = eventA.trim().lowercase()
        val b = eventB.trim().lowercase()
        if (a == b) return true

        // 1. Maraton vs Yarı Maraton
        val isMarathonA = a.contains("maraton") || a.contains("marathon")
        val isMarathonB = b.contains("maraton") || b.contains("marathon")
        if (isMarathonA || isMarathonB) {
            if (!isMarathonA || !isMarathonB) return false
            val isHalfA = a.contains("yarı") || a.contains("half") || a.contains("21")
            val isHalfB = b.contains("yarı") || b.contains("half") || b.contains("21")
            return isHalfA == isHalfB
        }

        // 2. Pentatlon vs Atma Pentatlonu
        val isThrowPentA = a.contains("atma") || a.contains("throws")
        val isThrowPentB = b.contains("atma") || b.contains("throws")
        if (a.contains("pentatlon") && b.contains("pentatlon")) {
            return isThrowPentA == isThrowPentB
        }

        // 3. MIX Bayrak vs Normal Bayrak
        val isMixA = a.contains("mix")
        val isMixB = b.contains("mix")
        if (isMixA != isMixB) return false

        // 4. Engelli Koşular (Hurdles) - mesafe ayrımı
        val getHurdleDist: (String) -> String? = { str ->
            val m = Regex("(\\\\d+)\\\\s*m?\\\\s*engelli").find(str)
            m?.groupValues?.get(1)
        }
        val distA = getHurdleDist(a)
        val distB = getHurdleDist(b)
        if (distA != null || distB != null) {
            return distA != null && distA == distB
        }

        // 5. Pist Yürüyüşü vs Yol Yürüyüşü
        val isPistWalkA = a.contains("pist")
        val isPistWalkB = b.contains("pist")
        if ((a.contains("yürüyüş") || b.contains("yürüyüş")) && isPistWalkA != isPistWalkB) {
            return false
        }

        // 6. Yol Koşusu (10K Yol Koşusu vs 10.000 Metre)
        val isRoadA = a.contains("yol")
        val isRoadB = b.contains("yol")
        if (isRoadA != isRoadB) return false

        // Genel kapsama (ör. "Gülle Atma (7.26kg)" ile "Gülle Atma")
        return a.contains(b) || b.contains(a)
    }

    fun getRecords(eventName: String? = null, category: String? = null, trackType: TrackType? = null, recordLevel: RecordLevel? = null): List<BenchmarkRecord> {
        return RECORDS.filter { record ->
            (eventName == null || isEventMatch(record.eventName, eventName)) &&
            (category == null || record.category.equals(category, ignoreCase = true)) &&
            (trackType == null || record.trackType == trackType) &&
            (recordLevel == null || record.recordLevel == recordLevel)
        }
    }`;

const oldGetRecords = `    fun getRecords(eventName: String? = null, category: String? = null, trackType: TrackType? = null, recordLevel: RecordLevel? = null): List<BenchmarkRecord> {
        return RECORDS.filter { record ->
            (eventName == null || record.eventName.contains(eventName, ignoreCase = true) || eventName.contains(record.eventName, ignoreCase = true)) &&
            (category == null || record.category.equals(category, ignoreCase = true)) &&
            (trackType == null || record.trackType == trackType) &&
            (recordLevel == null || record.recordLevel == recordLevel)
        }
    }`;

if (!archiveContent.includes(oldGetRecords)) {
    console.error('oldGetRecords not found in MasterRecordsArchive.kt');
    process.exit(1);
}

archiveContent = archiveContent.replace(oldGetRecords, isEventMatchKotlin);

const oldTargetRecords = `    fun getTargetRecordsForCategory(category: String, eventName: String): Map<TrackType, List<BenchmarkRecord>> {
        val filtered = RECORDS.filter {
            it.category.equals(category, ignoreCase = true) &&
            (it.eventName.contains(eventName, ignoreCase = true) || eventName.contains(it.eventName, ignoreCase = true))
        }
        return filtered.groupBy { it.trackType }
    }`;

const newTargetRecords = `    fun getTargetRecordsForCategory(category: String, eventName: String): Map<TrackType, List<BenchmarkRecord>> {
        val filtered = RECORDS.filter {
            it.category.equals(category, ignoreCase = true) &&
            isEventMatch(it.eventName, eventName)
        }
        return filtered.groupBy { it.trackType }
    }`;

archiveContent = archiveContent.replace(oldTargetRecords, newTargetRecords);
fs.writeFileSync(archivePath, archiveContent, 'utf8');
console.log('Updated MasterRecordsArchive.kt');

// 2. Update MasterRecordsScreen.kt (userPb match)
const screenPath = 'android/app/src/main/java/com/ecil/masterracer/ui/screens/MasterRecordsScreen.kt';
let screenContent = fs.readFileSync(screenPath, 'utf8').replace(/\r\n/g, '\n');

const oldScreenPb = `    val userPb = remember(selectedEvent, athleteRecords) {
        athleteRecords.filter {
            it.eventName.contains(selectedEvent, ignoreCase = true) || selectedEvent.contains(it.eventName, ignoreCase = true)
        }.let { list ->`;

const newScreenPb = `    val userPb = remember(selectedEvent, athleteRecords) {
        athleteRecords.filter {
            MasterRecordsArchive.isEventMatch(it.eventName, selectedEvent)
        }.let { list ->`;

if (screenContent.includes(oldScreenPb)) {
    screenContent = screenContent.replace(oldScreenPb, newScreenPb);
    fs.writeFileSync(screenPath, screenContent, 'utf8');
    console.log('Updated MasterRecordsScreen.kt');
} else {
    console.log('oldScreenPb not found in MasterRecordsScreen.kt');
}

// 3. Update CategoryTargetBottomSheet.kt (userPb match)
const sheetPath = 'android/app/src/main/java/com/ecil/masterracer/ui/components/CategoryTargetBottomSheet.kt';
let sheetContent = fs.readFileSync(sheetPath, 'utf8').replace(/\r\n/g, '\n');

const oldSheetPb = `    // Kullanıcının bu branştaki PB'sini bul
    val userPb = athleteRecords.filter {
        it.eventName.contains(selectedEvent, ignoreCase = true) || selectedEvent.contains(it.eventName, ignoreCase = true)
    }.let { list ->`;

const newSheetPb = `    // Kullanıcının bu branştaki PB'sini bul
    val userPb = athleteRecords.filter {
        MasterRecordsArchive.isEventMatch(it.eventName, selectedEvent)
    }.let { list ->`;

if (sheetContent.includes(oldSheetPb)) {
    sheetContent = sheetContent.replace(oldSheetPb, newSheetPb);
    fs.writeFileSync(sheetPath, sheetContent, 'utf8');
    console.log('Updated CategoryTargetBottomSheet.kt');
} else {
    console.log('oldSheetPb not found in CategoryTargetBottomSheet.kt');
}
