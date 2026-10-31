function isEventMatch(eventA, eventB) {
    if (!eventA || !eventB) return false;
    const a = eventA.trim().toLowerCase();
    const b = eventB.trim().toLowerCase();
    if (a === b) return true;

    // 1. Marathon vs Half Marathon
    const isMarathonA = a.includes("maraton") || a.includes("marathon");
    const isMarathonB = b.includes("maraton") || b.includes("marathon");
    if (isMarathonA || isMarathonB) {
        if (!isMarathonA || !isMarathonB) return false;
        const isHalfA = a.includes("yarı") || a.includes("half") || a.includes("21");
        const isHalfB = b.includes("yarı") || b.includes("half") || b.includes("21");
        return isHalfA === isHalfB;
    }

    // 2. Pentatlon vs Atma Pentatlonu
    const isThrowPentA = a.includes("atma") || a.includes("throws");
    const isThrowPentB = b.includes("atma") || b.includes("throws");
    if (a.includes("pentatlon") && b.includes("pentatlon")) {
        return isThrowPentA === isThrowPentB;
    }

    // 3. MIX Bayrak vs Normal Bayrak
    const isMixA = a.includes("mix");
    const isMixB = b.includes("mix");
    if (isMixA !== isMixB) return false;

    // 4. Hurdles (Engelli)
    const getHurdleDist = (str) => {
        const m = str.match(/(\d+)\s*m?\s*engelli/);
        return m ? m[1] : null;
    };
    const distA = getHurdleDist(a);
    const distB = getHurdleDist(b);
    if (distA || distB) {
        return distA && distA === distB;
    }

    // 5. Pist Yürüyüşü vs Yol Yürüyüşü
    const isPistWalkA = a.includes("pist");
    const isPistWalkB = b.includes("pist");
    if ((a.includes("yürüyüş") || b.includes("yürüyüş")) && isPistWalkA !== isPistWalkB) {
        return false;
    }

    // 6. Yol Koşusu
    const isRoadA = a.includes("yol");
    const isRoadB = b.includes("yol");
    if (isRoadA !== isRoadB) return false;

    return a.includes(b) || b.includes(a);
}

console.log('Pentatlon vs Atma Pentatlonu:', isEventMatch('Pentatlon', 'Atma Pentatlonu')); // false
console.log('Atma Pentatlonu vs Pentatlon:', isEventMatch('Atma Pentatlonu', 'Pentatlon')); // false
console.log('Pentatlon vs Pentatlon:', isEventMatch('Pentatlon', 'Pentatlon')); // true
console.log('Atma Pentatlonu vs Atma Pentatlonu:', isEventMatch('Atma Pentatlonu', 'Atma Pentatlonu')); // true
console.log('Maraton vs Yarı Maraton:', isEventMatch('Maraton', 'Yarı Maraton')); // false
console.log('Yarı Maraton vs Maraton:', isEventMatch('Yarı Maraton', 'Maraton')); // false
