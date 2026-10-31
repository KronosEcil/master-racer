const fs = require('fs');
const path = require('path');

function mapEvent(raw) {
    if (!raw) return "";
    let s = raw.trim();
    
    // Normalization
    if (/^60m(-i)?$/i.test(s) || /^60\s*m$/i.test(s)) return "60 Metre";
    if (/^100m$/i.test(s) || /^100\s*m$/i.test(s)) return "100 Metre";
    if (/^200m(-i)?$/i.test(s) || /^200\s*m$/i.test(s)) return "200 Metre";
    if (/^400m(-i)?$/i.test(s) || /^400\s*m$/i.test(s)) return "400 Metre";
    if (/^800m(-i)?$/i.test(s) || /^800\s*m$/i.test(s)) return "800 Metre";
    if (/^1500m(-i)?$/i.test(s) || /^1500\s*m$/i.test(s)) return "1500 Metre";
    if (/^3000m(-i)?$/i.test(s) || /^3000\s*m$/i.test(s)) return "3000 Metre";
    if (/^5000m$/i.test(s) || /^5000\s*m$/i.test(s)) return "5000 Metre";
    if (/^10000m$/i.test(s) || /^10\.?000m$/i.test(s)) return "10.000 Metre";
    if (/^21km$/i.test(s)) return "Yarı Maraton";
    if (/^42km$/i.test(s)) return "Maraton";

    // Hurdles
    if (/^60mH(-i)?$/i.test(s)) return "60m Engelli";
    if (/^80mH$/i.test(s)) return "80m Engelli";
    if (/^100mH$/i.test(s)) return "100m Engelli";
    if (/^110mH$/i.test(s)) return "110m Engelli";
    if (/^200mH$/i.test(s) || /^200Mh$/i.test(s)) return "200m Engelli";
    if (/^300mH$/i.test(s)) return "300m Engelli";
    if (/^400mH$/i.test(s)) return "400m Engelli";
    if (/^2000mSt$/i.test(s)) return "2000m Engelli";
    if (/^3000mSt$/i.test(s)) return "3000m Engelli";

    // Race walk
    if (/^3000mw(-i)?$/i.test(s)) return "3000m Pist Yürüyüşü";
    if (/^5000mw$/i.test(s)) return "5000m Pist Yürüyüşü";
    if (/^10000mwR$/i.test(s)) return "10.000m Pist Yürüyüşü";
    if (/^20000mwR$/i.test(s)) return "20.000m Pist Yürüyüşü";

    // Field
    if (/^LJ(-i)?$/i.test(s)) return "Uzun Atlama";
    if (/^TJ(-i)?$/i.test(s)) return "Üçadım Atlama";
    if (/^HJ(-i)?$/i.test(s)) return "Yüksek Atlama";
    if (/^PV(-i)?$/i.test(s)) return "Sırıkla Atlama";
    if (/^SP(-i)?$/i.test(s) || /^SP-I$/i.test(s)) return "Gülle Atma";
    if (/^DT$/i.test(s)) return "Disk Atma";
    if (/^JT$/i.test(s)) return "Cirit Atma";
    if (/^HT$/i.test(s)) return "Çekiç Atma";
    if (/^WT(-i)?$/i.test(s)) return "Ağırlık Atma";

    // Combined
    if (/^PEN(-[MF]-i)?$/i.test(s)) return "Pentatlon";
    if (/^HEP$/i.test(s)) return "Heptatlon";
    if (/^DEC$/i.test(s)) return "Dekatlon";
    if (/^TH[-_]PEN$/i.test(s)) return "Atma Pentatlonu";

    // Relays
    if (/^4x100m$/i.test(s)) return "4x100m Bayrak";
    if (/^4x200m(-i)?$/i.test(s)) return "4x200m Bayrak";
    if (/^4x400m$/i.test(s)) return "4x400m Bayrak";
    if (/^8-4-2-100m?$/i.test(s) || /^8-4-2-100\s*m$/i.test(s)) return "Balkan Bayrak (8-4-2-100m)";
    if (/^4x100m\s*MIX$/i.test(s) || /^4x100\s*m\s*X/i.test(s) || /^4\s*x100\s*m\s*X/i.test(s)) return "4x100m MIX Bayrak";
    if (/^4x200m-i\s*MIX$/i.test(s) || /^4x200m\s*MIX$/i.test(s)) return "4x200m MIX Bayrak";

    return s;
}

function parseFile(filePath, trackTypeStr, recordLevelStr) {
    const lines = fs.readFileSync(filePath, 'utf8').split('\n');
    const records = [];

    let currentEvent = "";

    for (let rawLine of lines) {
        let line = rawLine.trim();
        if (!line) continue;
        if (line.startsWith("BALKAN RECORDS") || line.startsWith("BAMAC") || line.startsWith("Event") || line.startsWith("Updated")) continue;
        if (line.includes("Balkan Masters Athletics") || line.includes("Updated by") || line.includes("Alltimes")) continue;

        let parts = line.split(/\s+/);
        if (parts.length < 5) continue;

        let evToken = parts[0];
        let categIdx = 1;

        if (/^(M\d+|F\d+|W\d+|X\d+)$/i.test(evToken)) {
            categIdx = 0;
        } else {
            if (/^\d+([,\.]\d+)?(m|kg|g)$/i.test(parts[1])) {
                categIdx = 2;
            } else if (/^(M\d+|F\d+|W\d+|X\d+)$/i.test(parts[1])) {
                categIdx = 1;
            } else {
                continue;
            }
            currentEvent = mapEvent(evToken);
        }

        let category = parts[categIdx].toUpperCase();
        if (category.startsWith('F')) {
            category = 'W' + category.substring(1);
        }

        let result = parts[categIdx + 1];
        if (!result) continue;
        result = result.replace(',', '.');

        let rest = parts.slice(categIdx + 2);
        let countryIdx = -1;
        const validCountries = ['TUR', 'GRE', 'BUL', 'ROU', 'ROM', 'SLO', 'CRO', 'SRB', 'MNE', 'SMNE', 'ALB', 'BIH', 'MDA', 'KOS', 'YUG', 'TURKEY', 'GREECE', 'BULGARIA', 'SLOVENIA', 'ROMANIA'];
        for (let i = 0; i < rest.length; i++) {
            if (validCountries.includes(rest[i].toUpperCase())) {
                countryIdx = i;
                break;
            }
        }

        let athleteName = "";
        let country = "TUR";
        let date = "";
        let venue = "";
        let year = 2024;
        let note = "";

        if (countryIdx !== -1) {
            athleteName = rest.slice(0, countryIdx).join(" ");
            let c = rest[countryIdx].toUpperCase();
            if (c === "ROM" || c === "ROMANIA") country = "ROU";
            else if (c === "GREECE") country = "GRE";
            else if (c === "BULGARIA") country = "BUL";
            else if (c === "SLOVENIA") country = "SLO";
            else if (c === "TURKEY") country = "TUR";
            else country = c;

            let afterCountry = rest.slice(countryIdx + 1);
            const dateRegex = /\b(\d{1,2}[\.\/]\d{1,2}[\.\/]\d{4}|\d{4}-\d{2}-\d{2})\b/;
            let dateMatches = [];
            for (let j = 0; j < afterCountry.length; j++) {
                if (dateRegex.test(afterCountry[j])) {
                    dateMatches.push({ idx: j, val: afterCountry[j] });
                }
            }

            if (dateMatches.length >= 2) {
                date = dateMatches[1].val;
                let locTokens = afterCountry.slice(dateMatches[1].idx + 1);
                let locStr = locTokens.join(" ");
                let commaPos = locStr.indexOf(',');
                if (commaPos !== -1 && commaPos < 25) {
                    let endIdx = locStr.indexOf(' ', commaPos + 6);
                    venue = locStr.substring(0, endIdx !== -1 ? endIdx : locStr.length).trim();
                } else {
                    venue = locTokens.slice(0, 2).join(" ");
                }
            } else if (dateMatches.length === 1) {
                date = dateMatches[0].val;
                let locTokens = afterCountry.slice(dateMatches[0].idx + 1);
                venue = locTokens.slice(0, 2).join(" ");
            }

            let yearMatch = date.match(/(\d{4})/);
            if (yearMatch) {
                year = parseInt(yearMatch[1], 10);
            }
        } else {
            athleteName = rest.slice(0, Math.min(3, rest.length)).join(" ");
        }

        athleteName = athleteName.replace(/["\\]/g, '').trim();

        let lastTokens = rest.slice(-3).join(" ");
        if (/w\s*[\+\-]?\d+/i.test(lastTokens) || /WR/i.test(lastTokens)) {
            note = lastTokens.replace(/["\\]/g, '').trim();
        }

        if (venue) {
            venue = venue.replace(/["\\]/g, '').trim();
        }

        records.push({
            eventName: currentEvent,
            category: category,
            trackType: trackTypeStr,
            recordLevel: recordLevelStr,
            mark: result.replace(/["\\]/g, '').trim(),
            athleteName: athleteName,
            country: country,
            year: year,
            venue: venue,
            date: date,
            note: note,
            isShared: false
        });
    }

    return records;
}

const dir = 'data/records';

const allBalkanRecords = [
    ...parseFile(path.join(dir, 'bamaci_indoor_men.ocr.txt'), 'TrackType.INDOOR', 'RecordLevel.BCR'),
    ...parseFile(path.join(dir, 'bamaci_indoor_women.ocr.txt'), 'TrackType.INDOOR', 'RecordLevel.BCR'),
    ...parseFile(path.join(dir, 'bamacs_outdoor_men.ocr.txt'), 'TrackType.OUTDOOR', 'RecordLevel.BCR'),
    ...parseFile(path.join(dir, 'bamacs_outdoor_women.ocr.txt'), 'TrackType.OUTDOOR', 'RecordLevel.BCR'),
    ...parseFile(path.join(dir, 'balkan_alltime_indoor_men.ocr.txt'), 'TrackType.INDOOR', 'RecordLevel.BMR'),
    ...parseFile(path.join(dir, 'balkan_alltime_indoor_women.ocr.txt'), 'TrackType.INDOOR', 'RecordLevel.BMR'),
    ...parseFile(path.join(dir, 'balkan_alltime_outdoor_men.ocr.txt'), 'TrackType.OUTDOOR', 'RecordLevel.BMR'),
    ...parseFile(path.join(dir, 'balkan_alltime_outdoor_women.ocr.txt'), 'TrackType.OUTDOOR', 'RecordLevel.BMR'),
];

console.log('Total Balkan Records to output:', allBalkanRecords.length);

// Chunk into groups of 50 functions to completely avoid JVM 64KB method/clinit limit
const CHUNK_SIZE = 50;
const chunks = [];
for (let i = 0; i < allBalkanRecords.length; i += CHUNK_SIZE) {
    chunks.push(allBalkanRecords.slice(i, i + CHUNK_SIZE));
}

let kt = `package com.ecil.masterracer.data.repository

import androidx.annotation.Keep

/**
 * Balkan Masters Athletics (BAMA) Rekorlar Arşivi
 * BAMACS / BAMACI Şampiyona Rekorları (RecordLevel.BCR / BŞR)
 * BALKAN All-Time Tüm Zamanlar Balkan Rekorları (RecordLevel.BMR)
 */
object BalkanRecordsArchive {

`;

chunks.forEach((chunk, idx) => {
    kt += `    private fun getPart${idx}(): List<BenchmarkRecord> = listOf(\n`;
    for (const r of chunk) {
        kt += `        BenchmarkRecord("${r.eventName}", "${r.category}", ${r.trackType}, ${r.recordLevel}, "${r.mark}", "${r.athleteName}", "${r.country}", ${r.year}, "${r.venue}", "${r.date}", "${r.note}", ${r.isShared}),\n`;
    }
    kt += `    )\n\n`;
});

kt += `    val RECORDS: List<BenchmarkRecord> by lazy {\n`;
kt += `        val list = ArrayList<BenchmarkRecord>(${allBalkanRecords.length})\n`;
chunks.forEach((_, idx) => {
    kt += `        list.addAll(getPart${idx}())\n`;
});
kt += `        list\n`;
kt += `    }\n`;
kt += `}\n`;

const outPath = 'android/app/src/main/java/com/ecil/masterracer/data/repository/BalkanRecordsArchive.kt';
fs.writeFileSync(outPath, kt, 'utf8');
console.log('Successfully wrote', outPath, 'Bytes:', fs.statSync(outPath).size);
