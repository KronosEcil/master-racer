const fs = require('fs');
const path = require('path');

function escapeKt(s) {
    if (!s) return '';
    return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$');
}

const EVENT_MAPPING = {
    '60m': '60 Metre',
    '100m': '100 Metre',
    '100m Wind': '100 Metre',
    '200m': '200 Metre',
    '400m': '400 Metre',
    '800m': '800 Metre',
    '1500m': '1500 Metre',
    '1mile': '1 Mil',
    'One Mile': '1 Mil',
    '3000m': '3000 Metre',
    '5000m': '5000 Metre',
    '10000m': '10.000 Metre',
    'Marathon': 'Maraton',
    '3000m Steeplechase': '3000m Engelli',
    '2000m Steeplechase': '2000m Engelli',
    '60m Hurdles': '60m Engelli',
    '80m Hurdles': '80m Engelli',
    '100m Hurdles': '100m Engelli',
    '110m Hurdles': '110m Engelli',
    '200m Hurdles': '200m Engelli',
    '300m Hurdles': '300m Engelli',
    '400m Hurdles': '400m Engelli',
    'High Jump': 'Yüksek Atlama',
    'Pole Vault': 'Sırıkla Atlama',
    'Long Jump': 'Uzun Atlama',
    'Triple Jump': 'Üçadım Atlama',
    'Shot Put': 'Gülle Atma',
    'Discus': 'Disk Atma',
    'Hammer': 'Çekiç Atma',
    'Javelin': 'Cirit Atma',
    'Weight Throw': 'Ağırlık Atma',
    'Pentathlon': 'Pentatlon',
    'Heptathlon': 'Heptatlon',
    'Decathlon': 'Dekatlon',
    'Throws Pentathlon': 'Atma Pentatlonu',
    '3000m Race Walk': '3000m Pist Yürüyüşü',
    '5000m Race Walk': '5000m Pist Yürüyüşü',
    '10k Race Walk': '10 km Yürüyüş',
    '20k Race Walk': '20 km Yürüyüş',
    '50k Race Walk': '50 km Yürüyüş',
    '4x100m': '4x100m Bayrak',
    '4x200m': '4x200m Bayrak',
    '4x400m': '4x400m Bayrak',
    '4x800m': '4x800m Bayrak',
    '4x200m Mixed': '4x200m MIX Bayrak',
    '4x400m Mixed': '4x400m MIX Bayrak'
};

function parseWmaFile(filePath, trackType, defaultGender) {
    const content = fs.readFileSync(filePath, 'utf8');
    const lines = content.split('\n').map(l => l.trim()).filter(Boolean);
    const records = [];

    let currentEvent = '';
    let lastCategory = '';

    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];

        if (line.includes('WMA') || line.includes('Page') || line.includes('October') || line.includes('September')) {
            continue;
        }

        // Check if event header
        const matchedEventKey = Object.keys(EVENT_MAPPING).find(k => k.toLowerCase() === line.toLowerCase());
        if (matchedEventKey) {
            currentEvent = EVENT_MAPPING[matchedEventKey];
            lastCategory = '';
            continue;
        }

        // Ignore headers like "Javelin (gm)"
        if (line.startsWith('Javelin')) {
            currentEvent = 'Cirit Atma';
            lastCategory = '';
            continue;
        }
        if (line.startsWith('WMA RECORDS')) continue;

        // Date match: DD/MM/YY or DD/MM/YYYY
        const dateMatch = line.match(/\b(\d{1,2}\/\d{1,2}\/\d{2,4})\b/);
        if (!dateMatch) {
            continue;
        }

        const dateStr = dateMatch[1];
        const dateIdx = line.indexOf(dateStr);
        const beforeDate = line.substring(0, dateIdx).trim();
        const afterDate = line.substring(dateIdx + dateStr.length).trim();

        // Venue is after date
        let venue = afterDate.replace(/^[,\s]+/, '').trim();

        // Parse year
        const dateParts = dateStr.split('/');
        let rawYear = parseInt(dateParts[2], 10);
        let year = 2000 + rawYear;
        if (rawYear > 50 && rawYear < 100) {
            year = 1900 + rawYear;
        } else if (rawYear >= 1900) {
            year = rawYear;
        }

        let tokens = beforeDate.split(/\s+/);
        let category = '';
        let mark = '';
        let athleteName = '';
        let country = '';
        let note = '';
        let tokenIdx = 0;

        if (/^[MWX]\s*\d+$/i.test(tokens[0])) {
            category = tokens[0].toUpperCase();
            tokenIdx = 1;
        } else if (/^[MW]$/i.test(tokens[0]) && /^\d+$/.test(tokens[1])) {
            category = (tokens[0] + tokens[1]).toUpperCase();
            tokenIdx = 2;
        } else if (/^\d+$/.test(tokens[0]) && (currentEvent.includes('MIX') || currentEvent.includes('Mixed'))) {
            category = 'X' + tokens[0];
            tokenIdx = 1;
        } else if (/^X\d+$/i.test(tokens[0])) {
            category = tokens[0].toUpperCase();
            tokenIdx = 1;
        } else {
            // Tied record! Inherits lastCategory
            category = lastCategory;
        }

        if (!category) continue;
        lastCategory = category;

        mark = tokens[tokenIdx++];
        if (!mark) continue;

        let remainingTokens = tokens.slice(tokenIdx);
        let countryTokenIdx = -1;
        for (let j = remainingTokens.length - 1; j >= 0; j--) {
            const tok = remainingTokens[j].toUpperCase();
            if (/^[A-Z]{3}$/.test(tok)) {
                countryTokenIdx = j;
                break;
            }
        }

        if (countryTokenIdx === -1) continue;

        country = remainingTokens[countryTokenIdx].toUpperCase();
        let nameTokens = remainingTokens.slice(0, countryTokenIdx);

        if (nameTokens.length > 0) {
            const first = nameTokens[0];
            if (/^[+-]?\d+(\.\d+)?$/.test(first) && !first.includes(':') && (currentEvent.includes('Metre') || currentEvent.includes('Engelli') || currentEvent.includes('Atlama'))) {
                note = `Rüzgar: ${first} m/s`;
                nameTokens.shift();
            } else if (/^\d+(\.\d+)?[kg]k?$/i.test(first) || /^\.?\d+k?g?$/i.test(first) || (['Cirit Atma'].includes(currentEvent) && /^\d+g?$/i.test(first))) {
                note = `Ağırlık: ${first}`;
                nameTokens.shift();
            }
        }

        athleteName = nameTokens.join(' ').trim();
        if (athleteName.match(/^\d+(\.\d+)?[kg]k/i)) {
            const m = athleteName.match(/^(\d+(\.\d+)?[kg]k?)(.*)/i);
            note = `Ağırlık: ${m[1]}`;
            athleteName = m[3].trim();
        }

        let formattedDate = dateStr;
        if (dateParts.length === 3) {
            const d = dateParts[0].padStart(2, '0');
            const m = dateParts[1].padStart(2, '0');
            formattedDate = `${d}.${m}.${year}`;
        }

        records.push({
            eventName: currentEvent,
            category: category,
            trackType: trackType,
            recordLevel: 'RecordLevel.WMR',
            mark: mark,
            athleteName: athleteName,
            country: country,
            year: year,
            venue: venue,
            date: formattedDate,
            note: note,
            isShared: false
        });
    }

    return records;
}

const indoorMen = parseWmaFile('data/records/wma_indoor_men.ocr.txt', 'TrackType.INDOOR', 'M');
const indoorWomen = parseWmaFile('data/records/wma_indoor_women.ocr.txt', 'TrackType.INDOOR', 'W');
const outdoorMen = parseWmaFile('data/records/wma_outdoor_men.ocr.txt', 'TrackType.OUTDOOR', 'M');
const outdoorWomen = parseWmaFile('data/records/wma_outdoor_women.ocr.txt', 'TrackType.OUTDOOR', 'W');
const nonstadiaMen = parseWmaFile('data/records/wma_nonstadia_men.ocr.txt', 'TrackType.OUTDOOR', 'M');
const nonstadiaWomen = parseWmaFile('data/records/wma_nonstadia_women.ocr.txt', 'TrackType.OUTDOOR', 'W');

const allRecords = [...indoorMen, ...indoorWomen, ...outdoorMen, ...outdoorWomen, ...nonstadiaMen, ...nonstadiaWomen];

// Count shared records (tied marks in same event, category and trackType)
const countMap = {};
allRecords.forEach(r => {
    const key = `${r.eventName}|${r.category}|${r.trackType}|${r.mark}`;
    countMap[key] = (countMap[key] || 0) + 1;
});

const finalRecords = allRecords.map(r => {
    const key = `${r.eventName}|${r.category}|${r.trackType}|${r.mark}`;
    return {
        ...r,
        isShared: countMap[key] > 1
    };
});

console.log('Total World Records parsed:', finalRecords.length);
const sharedCount = finalRecords.filter(r => r.isShared).length;
console.log('Shared records count in WMR:', sharedCount);

// Generate Kotlin File with chunks of 50
const CHUNK_SIZE = 50;
const chunks = [];
for (let i = 0; i < finalRecords.length; i += CHUNK_SIZE) {
    chunks.push(finalRecords.slice(i, i + CHUNK_SIZE));
}

let kt = `package com.ecil.masterracer.data.repository

import androidx.annotation.Keep

/**
 * World Masters Athletics (WMA) Resmi Dünya Rekorları Arşivi (RecordLevel.WMR)
 * Kaynak: https://world-masters-athletics.org/world-records/
 * Toplam Rekor Sayısı: ${finalRecords.length}
 */
object WorldRecordsArchive {

`;

chunks.forEach((chunk, idx) => {
    kt += `    private fun getPart${idx}(): List<BenchmarkRecord> = listOf(\n`;
    for (const r of chunk) {
        kt += `        BenchmarkRecord("${escapeKt(r.eventName)}", "${escapeKt(r.category)}", ${r.trackType}, RecordLevel.WMR, "${escapeKt(r.mark)}", "${escapeKt(r.athleteName)}", "${escapeKt(r.country)}", ${r.year}, "${escapeKt(r.venue)}", "${escapeKt(r.date)}", "${escapeKt(r.note)}", ${r.isShared}),\n`;
    }
    kt += `    )\n\n`;
});

kt += `    val RECORDS: List<BenchmarkRecord> by lazy {\n`;
kt += `        val list = ArrayList<BenchmarkRecord>(${finalRecords.length})\n`;
chunks.forEach((_, idx) => {
    kt += `        list.addAll(getPart${idx}())\n`;
});
kt += `        list\n`;
kt += `    }\n`;
kt += `}\n`;

const outPath = 'android/app/src/main/java/com/ecil/masterracer/data/repository/WorldRecordsArchive.kt';
fs.writeFileSync(outPath, kt, 'utf8');
console.log('Successfully wrote', outPath, 'Bytes:', fs.statSync(outPath).size);
