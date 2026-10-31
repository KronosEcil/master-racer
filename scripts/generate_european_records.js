const fs = require('fs');
const path = require('path');

function cleanHtml(raw) {
    if (!raw) return '';
    let s = raw;
    s = s.replace(/\s+[a-zA-Z0-9_\-:]+="[^"]*"/g, '');
    s = s.replace(/\s+[a-zA-Z0-9_\-:]+='[^']*'/g, '');
    s = s.replace(/<br\s*[\/]?>/gi, ' • ');
    s = s.replace(/<[^>]+>/g, ' ');
    s = s.replace(/&nbsp;/g, ' ').replace(/&#160;/g, ' ').replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;/g, "'");
    s = s.replace(/\[\s*[0-9a-zA-Z]+\s*\]/g, '');
    s = s.replace(/\s+,\s*/g, ', ');
    s = s.replace(/\s+/g, ' ').trim();
    return s;
}

function escapeKt(s) {
    if (!s) return '';
    return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$');
}

const countryToCode = {
    'great britain': 'GBR',
    'united kingdom': 'GBR',
    'england': 'GBR',
    'scotland': 'GBR',
    'wales': 'GBR',
    'northern ireland': 'GBR',
    'france': 'FRA',
    'germany': 'GER',
    'west germany': 'FRG',
    'east germany': 'GDR',
    'italy': 'ITA',
    'spain': 'ESP',
    'russia': 'RUS',
    'soviet union': 'URS',
    'sweden': 'SWE',
    'finland': 'FIN',
    'norway': 'NOR',
    'poland': 'POL',
    'netherlands': 'NED',
    'belgium': 'BEL',
    'portugal': 'POR',
    'ireland': 'IRL',
    'denmark': 'DEN',
    'czech republic': 'CZE',
    'czechia': 'CZE',
    'czechoslovakia': 'TCH',
    'slovakia': 'SVK',
    'slovenia': 'SLO',
    'greece': 'GRE',
    'romania': 'ROU',
    'hungary': 'HUN',
    'austria': 'AUT',
    'switzerland': 'SUI',
    'ukraine': 'UKR',
    'belarus': 'BLR',
    'estonia': 'EST',
    'latvia': 'LAT',
    'lithuania': 'LTU',
    'croatia': 'CRO',
    'serbia': 'SRB',
    'yugoslavia': 'YUG',
    'bulgaria': 'BUL',
    'cyprus': 'CYP',
    'iceland': 'ISL',
    'luxembourg': 'LUX',
    'turkey': 'TUR',
    'türkiye': 'TUR',
    'moldova': 'MDA',
    'georgia': 'GEO',
    'armenia': 'ARM',
    'azerbaijan': 'AZE',
    'israel': 'ISR',
    'malta': 'MLT'
};

const outdoorEventMap = {
    '100 metres': '100 Metre',
    '200 metres': '200 Metre',
    '400 metres': '400 Metre',
    '800 metres': '800 Metre',
    '1500 metres': '1500 Metre',
    '3000 metres': '3000 Metre',
    '5000 metres': '5000 Metre',
    '10,000 metres': '10.000 Metre',
    '10 km (road)': '10K Yol Koşusu',
    'half marathon': 'Yarı Maraton',
    'marathon': 'Maraton',
    '3000 metres steeplechase': '3000m Engelli',
    '2000 metres steeplechase': '2000m Engelli',
    '110 metres hurdles': '110m Engelli',
    '100 metres hurdles': '100m Engelli',
    '80 metres hurdles': '80m Engelli',
    '200 metres hurdles': '200m Engelli',
    '300 metres hurdles': '300m Engelli',
    '400 metres hurdles': '400m Engelli',
    'high jump': 'Yüksek Atlama',
    'pole vault': 'Sırıkla Atlama',
    'long jump': 'Uzun Atlama',
    'triple jump': 'Üçadım Atlama',
    'shot put': 'Gülle Atma',
    'discus throw': 'Disk Atma',
    'hammer throw': 'Çekiç Atma',
    'javelin throw': 'Cirit Atma',
    'weight throw': 'Ağırlık Atma',
    'pentathlon': 'Pentatlon',
    'decathlon': 'Dekatlon',
    'heptathlon': 'Heptatlon',
    'throws pentathlon': 'Atma Pentatlonu',
    '5000 metres walk': '5000m Pist Yürüyüşü',
    '10 km race walk': '10 km Yürüyüş',
    '20 km race walk': '20 km Yürüyüş',
    '4 × 100 metres relay': '4x100m Bayrak',
    '4 × 400 metres relay': '4x400m Bayrak',
    '4 × 800 metres relay': '4x800m Bayrak',
    '100 metres women': '100 Metre',
    '200 metres women': '200 Metre',
    '400 metres women': '400 Metre',
    '800 metres women': '800 Metre',
    '1500 metres women': '1500 Metre',
    '3000 metres women': '3000 Metre',
    '5000 metres women': '5000 Metre',
    '10,000 metres women': '10.000 Metre',
    'half marathon women': 'Yarı Maraton',
    'marathon women': 'Maraton',
    '2000 metres steeplechase women': '2000m Engelli',
    '100 metres hurdles women': '100m Engelli',
    '80 metres hurdles women': '80m Engelli',
    '200 metres hurdles women': '200m Engelli',
    '300 metres hurdles women': '300m Engelli',
    '400 metres hurdles women': '400m Engelli',
    'high jump women': 'Yüksek Atlama',
    'pole vault women': 'Sırıkla Atlama',
    'long jump women': 'Uzun Atlama',
    'triple jump women': 'Üçadım Atlama',
    'shot put women': 'Gülle Atma',
    'discus throw women': 'Disk Atma',
    'hammer throw women': 'Çekiç Atma',
    'javelin throw women': 'Cirit Atma',
    'weight throw women': 'Ağırlık Atma',
    'throws pentathlon women': 'Atma Pentatlonu',
    'heptathlon women': 'Heptatlon',
    '5000 metres race walk women': '5000m Pist Yürüyüşü',
    '20 km race walk women': '20 km Yürüyüş',
    '4 × 100 metres relay women': '4x100m Bayrak',
    '4 × 400 metres relay women': '4x400m Bayrak',
    '4 × 800 metres relay women': '4x800m Bayrak'
};

const indoorEventMap = {
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

function parseIndoorPdf() {
    const inputPath = path.join(__dirname, '..', 'data', 'records', 'ema_indoor_records.ocr.txt');
    const lines = fs.readFileSync(inputPath, 'utf8').split('\n');
    const records = [];
    let currentEvent = '';

    for (let i = 0; i < lines.length; i++) {
        let line = lines[i].trim();
        if (!line) continue;
        if (indoorEventMap[line]) {
            currentEvent = indoorEventMap[line];
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
            let cName = athleteName.substring(0, pOpen).trim().toLowerCase();
            country = countryToCode[cName] || cName.substring(0, 3).toUpperCase();
        } else {
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
        if (yMatch) year = parseInt(yMatch[0], 10);

        records.push({
            eventName: currentEvent,
            category,
            trackType: 'TrackType.INDOOR',
            recordLevel: 'RecordLevel.EMR',
            mark,
            athleteName,
            country,
            year,
            venue,
            date,
            note
        });
    }

    return records;
}

function parseOutdoorWikipedia() {
    const inputPath = path.join(__dirname, '..', 'data', 'records', 'ema_outdoor_wikipedia.html');
    const html = fs.readFileSync(inputPath, 'utf8');
    const headingRegex = /<h([234])[^>]*>([\s\S]*?)<\/h\1>/gi;
    let match;
    const sections = [];

    while ((match = headingRegex.exec(html)) !== null) {
        const rawTitle = cleanHtml(match[2]);
        sections.push({
            rawTitle,
            pos: match.index
        });
    }

    const records = [];

    for (let i = 0; i < sections.length; i++) {
        const sec = sections[i];
        const lowTitle = sec.rawTitle.toLowerCase();
        if (['contents', 'see also', 'notes', 'references', 'general', 'specific', 'external links', 'men', 'women'].includes(lowTitle)) {
            continue;
        }

        const eventName = outdoorEventMap[lowTitle];
        if (!eventName) continue;

        const nextPos = (i + 1 < sections.length) ? sections[i + 1].pos : html.length;
        const secHtml = html.substring(sec.pos, nextPos);

        const tableMatch = secHtml.match(/<table[^>]*>([\s\S]*?)<\/table>/i);
        if (!tableMatch) continue;

        const tableContent = tableMatch[1];
        const rowMatches = [...tableContent.matchAll(/<tr[^>]*>([\s\S]*?)<\/tr>/gi)];
        if (rowMatches.length < 2) continue;

        const ths = [...rowMatches[0][1].matchAll(/<th[^>]*>([\s\S]*?)<\/th>/gi)].map(h => cleanHtml(h[1]).toLowerCase());
        
        const colMap = {};
        ths.forEach((th, idx) => {
            if (th.includes('age group')) colMap.ageGroup = idx;
            else if (th.includes('record')) colMap.record = idx;
            else if (th.includes('athlete')) colMap.athlete = idx;
            else if (th.includes('nationality')) colMap.nationality = idx;
            else if (th.includes('birthdate')) colMap.birthdate = idx;
            else if (th === 'date') colMap.date = idx;
            else if (th.includes('place')) colMap.place = idx;
            else if (th.includes('meet')) colMap.meet = idx;
        });

        if (colMap.ageGroup === undefined || colMap.record === undefined) continue;

        let currentAgeGroup = '';
        let ageGroupRowspan = 0;

        for (let rIdx = 1; rIdx < rowMatches.length; rIdx++) {
            const rowHtml = rowMatches[rIdx][1];
            if (rowHtml.includes('<th')) continue;

            const cellMatches = [...rowHtml.matchAll(/<td([^>]*)>([\s\S]*?)<\/td>/gi)];
            if (cellMatches.length === 0) continue;

            let cells = [];
            if (ageGroupRowspan > 0) {
                cells.push({ text: currentAgeGroup });
                ageGroupRowspan--;
                for (let c of cellMatches) {
                    cells.push({ text: cleanHtml(c[2]) });
                }
            } else {
                const firstAttr = cellMatches[0][1];
                const firstText = cleanHtml(cellMatches[0][2]);
                const rowspanM = firstAttr.match(/rowspan=["']?(\d+)["']?/i);
                if (rowspanM) {
                    ageGroupRowspan = parseInt(rowspanM[1], 10) - 1;
                }
                currentAgeGroup = firstText;
                for (let c of cellMatches) {
                    cells.push({ text: cleanHtml(c[2]) });
                }
            }

            const rawAgeText = cells[colMap.ageGroup] ? cells[colMap.ageGroup].text : '';
            const agMatch = rawAgeText.match(/\b([MWX]\s*\d+)\b/i);
            if (!agMatch) continue;
            const ageGroup = agMatch[1].replace(/\s+/g, '').toUpperCase();

            let rawMark = cells[colMap.record] ? cells[colMap.record].text : '';
            if (!rawMark) continue;

            let wind = '';
            const windMatch = rawMark.match(/\(([+\-−±]?\d+(\.\d+)?)\s*m\/s\s*\)/i);
            if (windMatch) {
                wind = windMatch[1].replace('−', '-');
                rawMark = rawMark.replace(/\s*\([+\-−±]?\d+(\.\d+)?\s*m\/s\s*\)/i, '').trim();
            }

            let mark = rawMark.replace(/["\\]/g, '').trim();
            let athleteName = cells[colMap.athlete] ? cells[colMap.athlete].text.replace(/["\\]/g, '').trim() : '';
            let natRaw = cells[colMap.nationality] ? cells[colMap.nationality].text.toLowerCase().trim() : '';
            let countryCode = countryToCode[natRaw] || natRaw.substring(0, 3).toUpperCase();

            let date = (colMap.date !== undefined && cells[colMap.date]) ? cells[colMap.date].text.replace(/["\\]/g, '').trim() : '';
            let place = (colMap.place !== undefined && cells[colMap.place]) ? cells[colMap.place].text.replace(/["\\]/g, '').trim() : '';
            let meet = (colMap.meet !== undefined && cells[colMap.meet]) ? cells[colMap.meet].text.replace(/["\\]/g, '').trim() : '';

            let year = 2024;
            let yM = date.match(/(\d{4})/);
            if (yM) year = parseInt(yM[1], 10);

            let note = wind ? `Rüzgar: ${wind} m/s` : '';
            if (meet && !note) note = meet;
            else if (meet && note) note = `${note} • ${meet}`;

            records.push({
                eventName,
                category: ageGroup,
                trackType: 'TrackType.OUTDOOR',
                recordLevel: 'RecordLevel.EMR',
                mark,
                athleteName,
                country: countryCode,
                year,
                venue: place,
                date,
                note
            });
        }
    }

    return records;
}

const indoorRecords = parseIndoorPdf();
const outdoorRecords = parseOutdoorWikipedia();

const rawRecords = [...indoorRecords, ...outdoorRecords];
console.log(`Loaded ${indoorRecords.length} Indoor and ${outdoorRecords.length} Outdoor European records. Total: ${rawRecords.length}`);

// Deduplicate exact identicals
const uniqueRecords = [];
const seenSig = new Set();
for (const r of rawRecords) {
    const sig = `${r.eventName}###${r.category}###${r.trackType}###${r.recordLevel}###${r.mark}###${r.athleteName}###${r.date}`;
    if (!seenSig.has(sig)) {
        seenSig.add(sig);
        uniqueRecords.push(r);
    }
}

// Calculate isShared
const groupCount = {};
for (const r of uniqueRecords) {
    const key = `${r.eventName}|${r.category}|${r.trackType}|${r.recordLevel}`;
    groupCount[key] = (groupCount[key] || 0) + 1;
}

const finalRecords = uniqueRecords.map(r => {
    const key = `${r.eventName}|${r.category}|${r.trackType}|${r.recordLevel}`;
    return {
        ...r,
        isShared: (groupCount[key] > 1)
    };
});

console.log(`Final European records count: ${finalRecords.length} (Shared count: ${finalRecords.filter(r => r.isShared).length})`);

// Chunk into groups of 50 functions to avoid JVM 64KB clinit limit
const CHUNK_SIZE = 50;
const chunks = [];
for (let i = 0; i < finalRecords.length; i += CHUNK_SIZE) {
    chunks.push(finalRecords.slice(i, i + CHUNK_SIZE));
}

let out = `package com.ecil.masterracer.data.repository

import androidx.annotation.Keep

/**
 * European Masters Athletics (EMA) Rekorlar Arşivi
 * Resmi Avrupa Salon ve Açık Saha Masterlar Rekorları (RecordLevel.EMR)
 * Kaynaklar:
 * - Salon (Indoor): https://european-masters-athletics.org (17.03.2026)
 * - Açık Saha (Outdoor): List of European records in masters athletics (Wikipedia / EMA)
 */
object EuropeanRecordsArchive {

`;

chunks.forEach((chunk, idx) => {
    out += `    private fun getPart${idx}(): List<BenchmarkRecord> = listOf(\n`;
    for (let r of chunk) {
        out += `        BenchmarkRecord("${escapeKt(r.eventName)}", "${escapeKt(r.category)}", ${r.trackType}, RecordLevel.EMR, "${escapeKt(r.mark)}", "${escapeKt(r.athleteName)}", "${escapeKt(r.country)}", ${r.year}, "${escapeKt(r.venue)}", "${escapeKt(r.date)}", "${escapeKt(r.note)}", ${r.isShared}),\n`;
    }
    out += `    )\n\n`;
});

out += `    val RECORDS: List<BenchmarkRecord> by lazy {\n`;
out += `        val list = ArrayList<BenchmarkRecord>(${finalRecords.length})\n`;
chunks.forEach((_, idx) => {
    out += `        list.addAll(getPart${idx}())\n`;
});
out += `        list\n`;
out += `    }\n`;
out += `}\n`;

const outputPath = path.join(__dirname, '..', 'android', 'app', 'src', 'main', 'java', 'com', 'ecil', 'masterracer', 'data', 'repository', 'EuropeanRecordsArchive.kt');
fs.writeFileSync(outputPath, out, 'utf8');
console.log(`Generated ${outputPath} with ${finalRecords.length} records in ${chunks.length} parts.`);
