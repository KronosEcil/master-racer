const fs = require('fs');
const archive = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/MasterRecordsArchive.kt', 'utf8');
const balkan = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/BalkanRecordsArchive.kt', 'utf8');
const european = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/EuropeanRecordsArchive.kt', 'utf8');
const world = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/WorldRecordsArchive.kt', 'utf8');

function getRecords(content) {
  return [...content.matchAll(/BenchmarkRecord\("([^"]+)",\s*"([^"]+)",\s*(TrackType\.[A-Z]+)/g)].map(m => ({
    event: m[1],
    cat: m[2],
    track: m[3]
  }));
}

const all = [...getRecords(archive), ...getRecords(balkan), ...getRecords(european), ...getRecords(world)];

const PREFERRED_EVENT_ORDER = [
    "60 Metre", "100 Metre", "200 Metre", "400 Metre",
    "800 Metre", "1500 Metre", "1 Mil", "3000 Metre", "5000 Metre", "10.000 Metre", "10K Yol Koşusu", "Yarı Maraton", "Maraton",
    "60m Engelli", "80m Engelli", "100m Engelli", "110m Engelli", "200m Engelli", "300m Engelli", "400m Engelli", "2000m Engelli", "3000m Engelli",
    "3000m Pist Yürüyüşü", "5000m Pist Yürüyüşü", "10.000m Pist Yürüyüşü", "20.000m Pist Yürüyüşü", "10 km Yürüyüş", "20 km Yürüyüş", "50 km Yürüyüş",
    "Yüksek Atlama", "Sırıkla Atlama", "Uzun Atlama", "Üçadım Atlama",
    "Gülle Atma", "Disk Atma", "Çekiç Atma", "Cirit Atma", "Ağırlık Atma",
    "Pentatlon", "Heptatlon", "Dekatlon", "Atma Pentatlonu",
    "4x100m Bayrak", "4x200m Bayrak", "4x400m Bayrak", "4x800m Bayrak",
    "4x100m MIX Bayrak", "4x200m MIX Bayrak", "4x400m MIX Bayrak",
    "Balkan Bayrak (8-4-2-100m)"
];

function sortEvents(list) {
    return list.slice().sort((a, b) => {
        let idxA = PREFERRED_EVENT_ORDER.indexOf(a);
        let idxB = PREFERRED_EVENT_ORDER.indexOf(b);
        if (idxA === -1) idxA = 999;
        if (idxB === -1) idxB = 999;
        if (idxA !== idxB) return idxA - idxB;
        return a.localeCompare(b);
    });
}

function eventsFor(cat, track) {
  const events = [...new Set(all.filter(r => r.cat.toUpperCase() === cat.toUpperCase() && r.track.includes(track)).map(r => r.event))];
  return sortEvents(events);
}

console.log('--- W35 Açık Saha ---');
console.log(eventsFor('W35', 'OUTDOOR'));

console.log('--- M35 Açık Saha ---');
console.log(eventsFor('M35', 'OUTDOOR'));

console.log('--- M70 Açık Saha ---');
console.log(eventsFor('M70', 'OUTDOOR'));

console.log('--- X35 Salon ---');
console.log(eventsFor('X35', 'INDOOR'));
