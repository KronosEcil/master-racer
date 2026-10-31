const fs = require('fs');

function inspectRecords(name, file) {
  const content = fs.readFileSync(file, 'utf8');
  const marathons = [...content.matchAll(/BenchmarkRecord\("([^"]*Maraton[^"]*)",\s*"([^"]+)",\s*(TrackType\.[A-Z]+),\s*(RecordLevel\.[A-Z]+),\s*"([^"]+)"/g)];
  console.log(name, 'marathon matches:', marathons.length);
  marathons.forEach(m => console.log(' ', m[1], m[2], m[3], m[4], m[5]));
}

inspectRecords('Turkish', 'android/app/src/main/java/com/ecil/masterracer/data/repository/MasterRecordsArchive.kt');
inspectRecords('Balkan', 'android/app/src/main/java/com/ecil/masterracer/data/repository/BalkanRecordsArchive.kt');
inspectRecords('European', 'android/app/src/main/java/com/ecil/masterracer/data/repository/EuropeanRecordsArchive.kt');
inspectRecords('World', 'android/app/src/main/java/com/ecil/masterracer/data/repository/WorldRecordsArchive.kt');
