const fs = require('fs');

const path = 'android/app/src/main/java/com/ecil/masterracer/data/repository/MasterRecordsArchive.kt';
let content = fs.readFileSync(path, 'utf8');

const replacements = [
    ['"10000m Yürüyüş"', '"10.000m Pist Yürüyüşü"'],
    ['"20000m Yürüyüş"', '"20.000m Pist Yürüyüşü"'],
    ['"5000m Yürüyüş"', '"5000m Pist Yürüyüşü"'],
    ['"3000m Yürüyüş"', '"3000m Pist Yürüyüşü"'],
    ['"3000m Su Engelli"', '"3000m Engelli"'],
    ['"2000m Su Engelli"', '"2000m Engelli"'],
    ['"Atmalar Pentatlonu"', '"Atma Pentatlonu"'],
    ['"Balkan Bayrak"', '"Balkan Bayrak (8-4-2-100m)"'],
    ['"4x100m Mix Bayrak"', '"4x100m MIX Bayrak"'],
    ['"4x200m Bayrak MIX"', '"4x200m MIX Bayrak"']
];

replacements.forEach(([from, to]) => {
    const count = (content.match(new RegExp(from, 'g')) || []).length;
    console.log(`${from} -> ${to}: ${count} occurrences`);
    content = content.split(from).join(to);
});

fs.writeFileSync(path, content, 'utf8');
console.log('MasterRecordsArchive.kt updated.');
