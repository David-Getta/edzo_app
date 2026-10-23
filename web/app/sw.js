/*
 * Kiszolgáló-dolgozó: ettől működik a Grit internet nélkül is.
 *
 * A telefonon ez nem kényelmi kérdés: az edzőteremben és a hegyen sokszor
 * nincs térerő, és pont akkor akarja az ember beírni, amit csinált. A mag
 * (a felismerés) másfél megabájt – egyszer töltődik le, utána a gyorsítótárból
 * indul, ezredmásodpercek alatt.
 *
 * A VALTOZAT értékét az építő szkript írja bele: minden új fordítás új
 * gyorsítótárat kap, így a frissítés nem ragad be.
 */
const VALTOZAT = "__VALTOZAT__";
const TAR = "grit-" + VALTOZAT;

const FAJLOK = [
  ".",
  "index.html",
  "app.js",
  "munkas.js",
  "stilus.css",
  "mag.js",
  "manifest.webmanifest",
  "ikon.svg",
  "ikon-180.png",
  "ikon-192.png",
  "ikon-512.png",
];

self.addEventListener("install", (ev) => {
  ev.waitUntil((async () => {
    const tar = await caches.open(TAR);
    // Egyenként, hogy egyetlen hiányzó fájl ne bukatsa el az egész
    // telepítést – a lényeg, hogy a mag és a felület bent legyen.
    await Promise.all(FAJLOK.map((f) => tar.add(f).catch(() => null)));
    self.skipWaiting();
  })());
});

self.addEventListener("activate", (ev) => {
  ev.waitUntil((async () => {
    for (const nev of await caches.keys()) {
      if (nev.startsWith("grit-") && nev !== TAR) await caches.delete(nev);
    }
    await self.clients.claim();
  })());
});

self.addEventListener("fetch", (ev) => {
  const keres = ev.request;
  if (keres.method !== "GET") return;
  const url = new URL(keres.url);
  if (url.origin !== self.location.origin) return;

  // A lapot előbb a hálózatról kérjük, hogy a friss változat elérjen;
  // ha nincs net, jön a gyorsítótárból. A többi fájl fordítva: azok a
  // változat-kulcs miatt úgyis frissek, és így azonnal indulnak.
  const lapKeres = keres.mode === "navigate";
  ev.respondWith((async () => {
    const tar = await caches.open(TAR);
    if (lapKeres) {
      try {
        const valasz = await fetch(keres);
        tar.put(keres, valasz.clone());
        return valasz;
      } catch (e) {
        return (await tar.match("index.html")) || (await tar.match(".")) || Response.error();
      }
    }
    const talalat = await tar.match(keres);
    if (talalat) return talalat;
    try {
      const valasz = await fetch(keres);
      if (valasz.ok) tar.put(keres, valasz.clone());
      return valasz;
    } catch (e) {
      return Response.error();
    }
  })());
});
