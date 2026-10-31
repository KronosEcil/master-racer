const fs = require('fs');
const path = require('path');

const eventMap = {
    '60 metres': '60 Metre',
    '200 metres': '200 Metre',
    '400 metres': '400 Metre',
    '800 metres': '800 Metre',
    '1500 metres': '1500 Metre',
    '3000 metres': '3000 Metre',
    '60m Hurdles': '60m Engelli',
    'High Jump': 'Yüksek Atlama',
    'Pole Vault': 'Sırıkla Atlama',
    'Long Jump': 'Uzun Atlama',
    'Triple Jump': 'Üçadım Atlama',
    'Shot Put': 'Gülle Atma',
    'Weight Throw': 'Ağırlık Atma',
    'Pentathlon': 'Pentatlon',
    '3000m Track Walk': '3000m Pist Yürüyüşü',
    '4x200m Bayrak': '4x200m Bayrak',
    '4x200m MIX Bayrak': '4x200m MIX Bayrak'
};

const countryMap = {
    'GREAT BRITAIN': 'GBR',
    'GERMANY': 'GER',
    'SWEDEN': 'SWE',
    'ITALY': 'ITA',
    'POLAND': 'POL',
    'FRANCE': 'FRA',
    'FINLAND': 'FIN',
    'SPAIN': 'ESP',
    'NETHERLANDS': 'NED',
    'NORWAY': 'NOR',
    'UKRAINE': 'UKR',
    'CZECH REPUBLIC': 'CZE',
    'SLOVENIA': 'SLO',
    'SLOVAKIA': 'SVK',
    'GREECE': 'GRE',
    'ROMANIA': 'ROU',
    'BELGIUM': 'BEL',
    'PORTUGAL': 'POR',
    'IRELAND': 'IRL',
    'DENMARK': 'DEN',
    'HUNGARY': 'HUN',
    'AUSTRIA': 'AUT',
    'ESTONIA': 'EST',
    'LATVIA': 'LAT',
    'LITHUANIA': 'LTU',
    'SWITZERLAND': 'SUI'
};

function escapeKt(s) {
    if (!s) return '';
    return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$');
}

const inputPath = path.join(__dirname, '..', 'data', 'records', 'ema_indoor_records.ocr.txt');
const lines = fs.readFileSync(inputPath, 'utf8').split('\n');

const rawRecords = [];
let currentEvent = '';

for (let i = 0; i < lines.length; i++) {
    let line = lines[i].trim();
    if (!line) continue;
    if (eventMap[line]) {
        currentEvent = eventMap[line];
        continue;
    }
    if (line.startsWith('Age Group')) continue;

    let m = line.match(/^([MWX]\d+)\s+([0-9:.,]+[a-zA-Z]?)\s+(.+)$/);
    if (!m) continue;

    let category = m[1];
    let mark = m[2];
    let rest = m[3].trim();

    let athleteName = '';
    let country = '';
    let venue = '';
    let date = '';
    let year = 2024;
    let note = '';

    if (rest.includes('(') && rest.includes(')')) {
        // Bayrak Takımı (Relay)
        let dates = [...rest.matchAll(/\b(\d{2}[-\.]\d{2}[-\.]\d{4})\b/g)];
        let meetDate = dates[0][1];
        let meetIdx = dates[0].index;
        let beforeMeet = rest.substring(0, meetIdx).trim();
        note = rest.substring(meetIdx + meetDate.length).trim();
        date = meetDate;
        
        let pClose = beforeMeet.lastIndexOf(')');
        athleteName = beforeMeet.substring(0, pClose + 1).trim();
        venue = beforeMeet.substring(pClose + 1).trim();
        
        let pOpen = athleteName.indexOf('(');
        let cName = athleteName.substring(0, pOpen).trim().toUpperCase();
        country = countryMap[cName] || cName.substring(0, 3);
    } else {
        // Bireysel branş
        let dates = [...rest.matchAll(/\b(\d{2}[-\.]\d{2}[-\.]\d{4})\b/g)];
        let bornDate = dates[0][1];
        let meetDate = dates[1][1];
        let bornIdx = dates[0].index;
        let meetIdx = dates[1].index;

        let nameAndCountry = rest.substring(0, bornIdx).trim();
        venue = rest.substring(bornIdx + bornDate.length, meetIdx).trim();
        date = meetDate;
        note = rest.substring(meetIdx + meetDate.length).trim();

        let ncMatch = nameAndCountry.match(/^(.*?),\s*([A-Za-z]{3})$/);
        if (ncMatch) {
            athleteName = ncMatch[1].trim();
            country = ncMatch[2].trim().toUpperCase();
        } else {
            athleteName = nameAndCountry;
            country = 'EUR';
        }
    }

    let yMatch = date.match(/\d{4}$/);
    if (yMatch) {
        year = parseInt(yMatch[0], 10);
    }

    rawRecords.push({
        eventName: currentEvent,
        category,
        trackType: 'INDOOR',
        recordLevel: 'EMR',
        mark,
        athleteName,
        country,
        year,
        venue,
        date,
        note
    });
}

// Ortak rekorları (isShared) belirle
const catCount = {};
for (let r of rawRecords) {
    let k = r.eventName + '___' + r.category;
    catCount[k] = (catCount[k] || 0) + 1;
}

const records = rawRecords.map(r => {
    let k = r.eventName + '___' + r.category;
    let isShared = (catCount[k] > 1);
    return {
        ...r,
        isShared
    };
});

console.log(`Parsed ${records.length} European records.`);

// Chunk records to prevent MethodTooLargeException in JVM clinit
const CHUNK_SIZE = 40;
const chunks = [];
for (let i = 0; i < records.length; i += CHUNK_SIZE) {
    chunks.push(records.slice(i, i + CHUNK_SIZE));
}

let out = `package com.ecil.masterracer.data.repository

import androidx.annotation.Keep

/**
 * European Masters Athletics (EMA) Rekorlar Arşivi
 * Resmi Avrupa Salon Masterlar Rekorları (RecordLevel.EMR)
 * Kaynak: https://european-masters-athletics.org (17.03.2026)
 */
object EuropeanRecordsArchive {

`;

chunks.forEach((chunk, idx) => {
    out += `    private fun getPart${idx}(): List<BenchmarkRecord> = listOf(\n`;
    for (let r of chunk) {
        out += `        BenchmarkRecord("${escapeKt(r.eventName)}", "${escapeKt(r.category)}", TrackType.INDOOR, RecordLevel.EMR, "${escapeKt(r.mark)}", "${escapeKt(r.athleteName)}", "${escapeKt(r.country)}", ${r.year}, "${escapeKt(r.venue)}", "${escapeKt(r.date)}", "${escapeKt(r.note)}", ${r.isShared}),\n`;
    }
    out += `    )\n\n`;
});

out += `    val RECORDS: List<BenchmarkRecord> by lazy {\n`;
out += `        val list = ArrayList<BenchmarkRecord>(${records.length})\n`;
chunks.forEach((_, idx) => {
    out += `        list.addAll(getPart${idx}())\n`;
});
out += `        list\n`;
out += `    }\n`;
out += `}\n`;

const outputPath = path.join(__dirname, '..', 'android', 'app', 'src', 'main', 'java', 'com', 'ecil', 'masterracer', 'data', 'repository', 'EuropeanRecordsArchive.kt');
fs.writeFileSync(outputPath, out, 'utf8');
console.log(`Generated ${outputPath} with ${records.length} records in ${chunks.length} parts.`);
