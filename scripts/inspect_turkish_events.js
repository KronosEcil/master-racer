const fs = require('fs');
const archive = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/MasterRecordsArchive.kt', 'utf8');
const turkishOnly = [...archive.matchAll(/BenchmarkRecord\("([^"]+)"/g)].map(m => m[1]);
console.log('Turkish records event names:', [...new Set(turkishOnly)].sort());
