const fs = require('fs');

const html = fs.readFileSync('data/records/wma_records_page.html', 'utf8');
console.log('HTML size:', html.length);

const links = [...html.matchAll(/<a[^>]+href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi)];
const recordLinks = links.filter(l => 
    l[1].includes('.pdf') || 
    l[1].includes('.xlsx') || 
    l[1].includes('.xls') || 
    l[1].includes('record') || 
    l[1].includes('wp-content')
);

console.log('Total record links found:', recordLinks.length);
recordLinks.forEach(l => {
    const text = l[2].replace(/<[^>]+>/g, '').trim();
    console.log(`[${text}] -> ${l[1]}`);
});
