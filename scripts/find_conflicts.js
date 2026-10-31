const fs = require('fs');
const archive = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/MasterRecordsArchive.kt', 'utf8');
const balkan = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/BalkanRecordsArchive.kt', 'utf8');
const european = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/EuropeanRecordsArchive.kt', 'utf8');
const world = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/WorldRecordsArchive.kt', 'utf8');

function getRecords(content) {
  return [...content.matchAll(/BenchmarkRecord\("([^"]+)"/g)].map(m => m[1]);
}

const allEvents = [...new Set([...getRecords(archive), ...getRecords(balkan), ...getRecords(european), ...getRecords(world)])];
console.log('Total distinct events:', allEvents.length);

const pairs = [];
for (let i = 0; i < allEvents.length; i++) {
    for (let j = i + 1; j < allEvents.length; j++) {
        const e1 = allEvents[i];
        const e2 = allEvents[j];
        if (e1.toLowerCase().includes(e2.toLowerCase()) || e2.toLowerCase().includes(e1.toLowerCase())) {
            pairs.push([e1, e2]);
        }
    }
}

console.log('Conflicting pairs with substring contains():');
pairs.forEach(p => console.log(' ', p[0], '<--->', p[1]));
