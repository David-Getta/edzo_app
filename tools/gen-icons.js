// Minimal dependency-free PNG icon generator for the Edző interval-timer PWA.
// Draws a rounded-square badge with a stopwatch glyph. Outputs 192 & 512 px
// plus a maskable 512 with extra safe-area padding. Uses only Node's zlib.
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

function lerp(a, b, t) { return a + (b - a) * t; }

// Draw one RGBA icon into a flat Uint8 buffer.
function drawIcon(size, { padding = 0.14 } = {}) {
  const buf = Buffer.alloc(size * size * 4);
  const c = size / 2;
  const radius = size * (0.5 - padding * 0.0); // full-bleed badge
  const corner = size * 0.22;

  // Stopwatch geometry (relative to a padded content box).
  const box = size * (1 - padding * 2);
  const off = (size - box) / 2;
  const dialCx = c;
  const dialCy = off + box * 0.56;
  const dialR = box * 0.34;
  const ringW = box * 0.075;

  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const i = (y * size + x) * 4;
      let r = 0, g = 0, b = 0, a = 0;

      // Rounded-square badge mask.
      const dxC = Math.max(Math.abs(x - c) - (size / 2 - corner), 0);
      const dyC = Math.max(Math.abs(y - c) - (size / 2 - corner), 0);
      const cornerDist = Math.sqrt(dxC * dxC + dyC * dyC);
      const inBadge = cornerDist <= corner;

      if (inBadge) {
        // Átlós márka-gradiens: cián (#22E0FF) -> magenta (#FF3DDB).
        const t = (x + y) / (2 * size);
        r = Math.round(lerp(34, 255, t));
        g = Math.round(lerp(224, 61, t));
        b = Math.round(lerp(255, 219, t));
        a = 255;
      }

      // Stopwatch dial (white ring + white top button + stem).
      const dx = x - dialCx;
      const dy = y - dialCy;
      const dist = Math.sqrt(dx * dx + dy * dy);

      // Ring.
      if (Math.abs(dist - dialR) <= ringW / 2) {
        r = g = b = 255; a = 255;
      }
      // Top button (small rounded rect above dial).
      const btnW = box * 0.11, btnH = box * 0.07;
      const btnCy = dialCy - dialR - box * 0.05;
      if (Math.abs(x - dialCx) <= btnW / 2 && Math.abs(y - btnCy) <= btnH / 2) {
        r = g = b = 255; a = 255;
      }
      // Stem connecting button to dial.
      const stemH = box * 0.05;
      if (Math.abs(x - dialCx) <= btnW * 0.28 &&
          y >= btnCy && y <= btnCy + stemH + box * 0.04) {
        r = g = b = 255; a = 255;
      }
      // Clock hand (points to 1 o'clock).
      const ang = -Math.PI / 3;
      const handLen = dialR * 0.62;
      // distance from point to the hand segment from center outward.
      const hx = Math.cos(ang), hy = Math.sin(ang);
      const proj = dx * hx + dy * hy;
      if (proj > 0 && proj < handLen) {
        const perp = Math.abs(dx * (-hy) + dy * hx);
        if (perp <= box * 0.018) { r = g = b = 255; a = 255; }
      }
      // Center hub.
      if (dist <= box * 0.03) { r = g = b = 255; a = 255; }

      buf[i] = r; buf[i + 1] = g; buf[i + 2] = b; buf[i + 3] = a;
    }
  }
  return buf;
}

function encodePNG(size, rgba) {
  // Build raw image data with filter byte 0 per scanline.
  const raw = Buffer.alloc(size * (size * 4 + 1));
  for (let y = 0; y < size; y++) {
    raw[y * (size * 4 + 1)] = 0;
    rgba.copy(raw, y * (size * 4 + 1) + 1, y * size * 4, (y + 1) * size * 4);
  }
  const idat = zlib.deflateSync(raw, { level: 9 });

  function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length, 0);
    const typeBuf = Buffer.from(type, 'ascii');
    const body = Buffer.concat([typeBuf, data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(body) >>> 0, 0);
    return Buffer.concat([len, body, crc]);
  }

  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8;   // bit depth
  ihdr[9] = 6;   // color type RGBA
  ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;

  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  return Buffer.concat([
    sig,
    chunk('IHDR', ihdr),
    chunk('IDAT', idat),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

// CRC32 (PNG spec).
const crcTable = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = crcTable[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

// Android launcher icons: one ic_launcher.png per density bucket.
const resDir = path.join(__dirname, '..', 'app', 'src', 'main', 'res');
const buckets = [
  { dir: 'mipmap-mdpi', size: 48 },
  { dir: 'mipmap-hdpi', size: 72 },
  { dir: 'mipmap-xhdpi', size: 96 },
  { dir: 'mipmap-xxhdpi', size: 144 },
  { dir: 'mipmap-xxxhdpi', size: 192 },
];

for (const b of buckets) {
  const dir = path.join(resDir, b.dir);
  fs.mkdirSync(dir, { recursive: true });
  const rgba = drawIcon(b.size, {});
  const png = encodePNG(b.size, rgba);
  fs.writeFileSync(path.join(dir, 'ic_launcher.png'), png);
  // Round variant (same art) so devices requesting @mipmap/ic_launcher_round also resolve.
  fs.writeFileSync(path.join(dir, 'ic_launcher_round.png'), png);
  console.log('wrote', b.dir + '/ic_launcher.png', png.length, 'bytes');
}
