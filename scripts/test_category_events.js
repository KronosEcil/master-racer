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
console.log('Total records:', all.length);

function eventsFor(cat, track) {
  return [...new Set(all.filter(r => r.cat.toUpperCase() === cat.toUpperCase() && r.track.includes(track)).map(r => r.event))].sort();
}

console.log('--- W35 OUTDOOR ---');
console.log(eventsFor('W35', 'OUTDOOR'));

console.log('--- M35 OUTDOOR ---');
console.log(eventsFor('M35', 'OUTDOOR'));

console.log('--- M70 OUTDOOR ---');
console.log(eventsFor('M70', 'OUTDOOR'));

console.log('--- X35 INDOOR ---');
console.log(eventsFor('X35', 'INDOOR'));
