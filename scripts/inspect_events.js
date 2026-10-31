const fs = require('fs');

['BalkanRecordsArchive.kt', 'EuropeanRecordsArchive.kt'].forEach(file => {
  const content = fs.readFileSync('android/app/src/main/java/com/ecil/masterracer/data/repository/' + file, 'utf8');
  const matches = [...content.matchAll(/BenchmarkRecord\("([^"]+)"/g)].map(m => m[1]);
  console.log(file, 'unique events:', [...new Set(matches)].sort());
});
