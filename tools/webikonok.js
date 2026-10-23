// A webes (iPhone-os) változat ikonjai.
//
// Ugyanaz a jelvény, mint az Androidos launcher-ikon – a rajzolót onnan
// hívjuk be, hogy a két felület ne kezdjen kettéfejlődni. Az iOS a
// kezdőképernyőn az apple-touch-icont használja (a manifestet nem nézi),
// ezért a 180 pixeles változat külön kell.
const fs = require('fs');
const path = require('path');
const { drawIcon, encodePNG } = require('./gen-icons.js');

const out = path.join(__dirname, '..', 'web', 'app');
fs.mkdirSync(out, { recursive: true });

const meretek = [
  { nev: 'ikon-180.png', size: 180, opts: {} },          // iOS kezdőképernyő
  { nev: 'ikon-192.png', size: 192, opts: {} },          // manifest
  { nev: 'ikon-512.png', size: 512, opts: {} },          // manifest, nagy
  // A „maskable" ikonból az Android bármilyen alakot kivághat, ezért
  // nagyobb biztonsági margóval rajzoljuk.
  { nev: 'ikon-512-maszk.png', size: 512, opts: { padding: 0.24 } },
];

for (const m of meretek) {
  const png = encodePNG(m.size, drawIcon(m.size, m.opts));
  fs.writeFileSync(path.join(out, m.nev), png);
  console.log('kész:', m.nev, png.length, 'bájt');
}

// Egy egyszerű SVG a böngészőfülre: éles marad minden méretben, és nem
// terheli a letöltést.
const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <rect width="64" height="64" rx="14" fill="#140B0D"/>
  <circle cx="32" cy="36" r="17" fill="none" stroke="#E11D2E" stroke-width="5"/>
  <rect x="27" y="7" width="10" height="6" rx="2" fill="#E11D2E"/>
  <rect x="30" y="24" width="4" height="14" rx="2" fill="#F5ECEE"/>
</svg>
`;
fs.writeFileSync(path.join(out, 'ikon.svg'), svg);
console.log('kész: ikon.svg');
