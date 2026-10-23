/*
 * A felismerés külön szálon.
 *
 * Egy mondat elemzése néhány száz ezredmásodperc: ha ez a fő szálon futna,
 * a billentyűzet akadozna gépelés közben. Itt a felület mindig sima marad,
 * és a válasz akkor érkezik, amikor kész.
 *
 * A mag betöltése után rögtön lefuttatunk egy elemzést. Ez tölti fel a
 * minta-gyorsítótárat, ami egyszeri, de érezhető munka – jobb, ha az
 * indulás közben történik meg, nem az első begépelt mondatnál.
 */
"use strict";

importScripts("mag.js");

try {
  self.parse("bemelegítés: 5 km futás és egy alma", Date.now());
} catch (e) {
  // A bemelegítés hibája nem végzetes: a valódi hívás úgyis jelezni fog.
}

self.onmessage = (ev) => {
  const { id, mit, szoveg, now } = ev.data || {};
  try {
    if (mit === "library") {
      self.postMessage({ id, json: self.library() });
    } else {
      self.postMessage({ id, json: self.parse(szoveg, now) });
    }
  } catch (e) {
    self.postMessage({ id, hiba: String((e && e.message) || e) });
  }
};

// Jelezzük, hogy készen állunk – a felület addig „Felismerés indul…”-t mutat.
self.postMessage({ kesz: true });
