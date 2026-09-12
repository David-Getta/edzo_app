/*
 * Grit – webes felület.
 *
 * A felismerést NEM ez a fájl végzi: azt a mag.js csinálja, ami az Androidos
 * app Java forrásából fordul. Itt csak az van, ami a böngészőben más: a
 * tárolás, a napok kiosztása és a megjelenítés.
 */
"use strict";

/*
 * A felismerés külön szálon fut (munkas.js), mert egy mondat elemzése
 * néhány száz ezredmásodperc – a fő szálon ez akadozó billentyűzetet
 * jelentene. Ha a böngésző nem tud szálat indítani, a magot itt helyben
 * töltjük be; a felület ugyanaz marad, csak gépelés közben lassabb.
 */
let munkas = null;
let magHelyben = null;
let kerdesSzam = 0;
const fuggo = new Map();

function munkasIndul() {
  if (typeof Worker === "undefined") return null;
  try {
    const w = new Worker("munkas.js");
    w.onmessage = (ev) => {
      const d = ev.data || {};
      if (d.kesz) return;
      const f = fuggo.get(d.id);
      if (!f) return;
      fuggo.delete(d.id);
      f(d.hiba ? null : d.json);
    };
    w.onerror = () => {
      // A szál elszállt: innentől helyben dolgozunk, hogy az app működjön.
      munkas = null;
      for (const f of fuggo.values()) f(null);
      fuggo.clear();
    };
    return w;
  } catch (e) {
    return null;
  }
}

/** Tartalék: a magot a fő szálra töltjük, ha nincs külön szál. */
function magHelybenBetolt() {
  if (magHelyben) return Promise.resolve(magHelyben);
  return new Promise((kesz, hiba) => {
    const s = document.createElement("script");
    s.src = "mag.js";
    s.onload = () => {
      magHelyben = { parse: self.parse, library: self.library };
      kesz(magHelyben);
    };
    s.onerror = () => hiba(new Error("nem tölthető be a mag"));
    document.head.append(s);
  });
}

function magHiv(mit, szoveg) {
  if (munkas) {
    const id = ++kerdesSzam;
    return new Promise((kesz) => {
      fuggo.set(id, kesz);
      munkas.postMessage({ id: id, mit: mit, szoveg: szoveg, now: Date.now() });
    });
  }
  return magHelybenBetolt().then(
      (m) => (mit === "library" ? m.library() : m.parse(szoveg, Date.now())),
      () => null);
}

const NAPLO_KULCS = "grit.naplo.v1";
const BEALLITAS_KULCS = "grit.beallitas.v1";
const VERZIO = "1.0.0";

/* ------------------------------------------------------------------ tárolás */

/**
 * A napló beolvasása.
 *
 * A tárolás sosem lehet olyan hibás, hogy az app el se induljon: egy
 * megsérült bejegyzés miatt ne vesszen el a többi hónap. Ezért minden
 * olvasás védett, és hiba esetén üres naplóval indulunk – a nyers szöveg
 * pedig megmarad a böngészőben, kimenthető.
 */
function betolt(kulcs, alap) {
  try {
    const nyers = localStorage.getItem(kulcs);
    if (!nyers) return alap;
    const ertek = JSON.parse(nyers);
    return ertek == null ? alap : ertek;
  } catch (e) {
    return alap;
  }
}

function ment(kulcs, ertek) {
  try {
    localStorage.setItem(kulcs, JSON.stringify(ertek));
    return true;
  } catch (e) {
    // Tele a tár, vagy privát böngészés. A felhasználónak szólni kell,
    // különben azt hiszi, mentett.
    pirit("Nem sikerült menteni – megtelt a tárhely?", true);
    return false;
  }
}

let naplo = betolt(NAPLO_KULCS, []);
let beallitas = betolt(BEALLITAS_KULCS, {});

/* -------------------------------------------------------------------- dátum */

const NAPNEV = ["vasárnap", "hétfő", "kedd", "szerda", "csütörtök", "péntek", "szombat"];
const HONAP = ["jan.", "febr.", "márc.", "ápr.", "máj.", "jún.",
               "júl.", "aug.", "szept.", "okt.", "nov.", "dec."];

/** Nap-kulcs helyi idő szerint: az UTC-s vágás egy este tízes edzést tegnapra tenne. */
function napKulcs(ts) {
  const d = new Date(ts);
  return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0")
      + "-" + String(d.getDate()).padStart(2, "0");
}

function maKulcs() { return napKulcs(Date.now()); }

function napCime(kulcs) {
  const ma = maKulcs();
  if (kulcs === ma) return "Ma";
  const [e, h, n] = kulcs.split("-").map(Number);
  const d = new Date(e, h - 1, n);
  const tegnap = new Date();
  tegnap.setDate(tegnap.getDate() - 1);
  if (kulcs === napKulcs(tegnap.getTime())) return "Tegnap";
  return NAPNEV[d.getDay()] + ", " + n + ". " + HONAP[h - 1];
}

function ora(ts) {
  const d = new Date(ts);
  return String(d.getHours()).padStart(2, "0") + ":"
      + String(d.getMinutes()).padStart(2, "0");
}

/**
 * A bejegyzés időpontja a mondat szerint.
 *
 * A „tegnap este futottam" nem a beírás pillanatában történt. A mag megadja,
 * hány nappal ezelőttről van szó és hányadik órában – ezt tesszük vissza a
 * naptárba, különben a heti összesítés attól csúszna el, hogy mikor ért rá
 * az ember beírni.
 */
function idopont(elemzes, eltolasNapok) {
  const d = new Date();
  const m = elemzes.mozgas || {};
  const nap = eltolasNapok != null ? eltolasNapok : (m.eltolas || 0);
  d.setDate(d.getDate() - nap);
  if (m.ora != null && m.ora >= 0 && m.ora <= 23 && (nap > 0 || m.ora !== 12)) {
    d.setHours(m.ora, 0, 0, 0);
  }
  return d.getTime();
}

/* ------------------------------------------------------------- felismerés */

function elemez(szoveg) {
  return magHiv("parse", szoveg).then((json) => {
    try {
      return json ? JSON.parse(json) : null;
    } catch (e) {
      return null;
    }
  });
}

/** Van-e egyáltalán bármi felismerve? */
function ures(e) {
  if (!e) return true;
  return (!e.mozgas || e.mozgas.tetelek.length === 0)
      && e.ero.length === 0 && e.etel.length === 0
      && e.test.kg <= 0 && e.test.zsir <= 0 && !e.test.cm.some((v) => v > 0)
      && e.alvas <= 0 && e.pulzus <= 0
      && e.kcal <= 0 && e.egetett <= 0 && e.feherje <= 0;
}

function szam(v, tizedes) {
  const n = Number(v);
  if (!isFinite(n)) return "0";
  return (tizedes && n % 1 !== 0 ? n.toFixed(1) : String(Math.round(n)))
      .replace(".", ",");
}

/** Emberi összefoglaló egy elemzésről – ugyanaz kell az előnézethez és a naplóhoz. */
function sorok(e) {
  const ki = [];
  const m = e.mozgas || { tetelek: [] };
  for (const t of m.tetelek) {
    const reszek = [];
    if (t.km > 0) reszek.push(szam(t.km, true) + " km");
    if (t.perc > 0) reszek.push(t.perc + " perc");
    if (t.lepes > 0) reszek.push(szam(t.lepes) + " lépés");
    ki.push({
      jel: t.emoji || "🏃",
      cim: (t.alkalom > 1 ? t.alkalom + " × " : "") + t.nev,
      reszlet: reszek.join(" · "),
    });
  }
  for (const it of e.ero) {
    const s = it.sorozatok;
    const azonos = s.length > 1 && s.every((x) => x.ism === s[0].ism);
    const ism = azonos ? s.length + "×" + s[0].ism : s.map((x) => x.ism).join("-");
    const suly = Math.max(0, ...s.map((x) => x.suly));
    ki.push({
      jel: "🏋",
      cim: it.nev,
      reszlet: ism
          + (suly > 0 ? " · " + szam(suly, true) + " kg" : " · saját testsúly"),
    });
  }
  for (const f of e.etel) {
    ki.push({
      jel: "🍽",
      cim: f.nev,
      reszlet: szam(f.gramm) + " g · " + szam(f.kcal) + " kcal"
          + (f.feherje > 0 ? " · " + szam(f.feherje, true) + " g fehérje" : ""),
    });
  }
  if (e.test.kg > 0 || e.test.zsir > 0) {
    const r = [];
    if (e.test.kg > 0) r.push(szam(e.test.kg, true) + " kg");
    if (e.test.zsir > 0) r.push(szam(e.test.zsir, true) + "% testzsír");
    ki.push({ jel: "⚖️", cim: "Mérés", reszlet: r.join(" · ") });
  }
  if (e.alvas > 0) {
    ki.push({ jel: "😴", cim: "Alvás", reszlet: szam(e.alvas, true) + " óra" });
  }
  if (e.pulzus > 0) {
    ki.push({ jel: "❤️", cim: "Nyugalmi pulzus", reszlet: e.pulzus + " bpm" });
  }
  if (e.kcal > 0) {
    ki.push({ jel: "🔥", cim: "Kimondott bevitel", reszlet: e.kcal + " kcal" });
  }
  if (e.egetett > 0) {
    ki.push({ jel: "🔥", cim: "Elégetett", reszlet: e.egetett + " kcal" });
  }
  if (e.feherje > 0 && e.etel.length === 0) {
    ki.push({ jel: "🥩", cim: "Fehérje", reszlet: e.feherje + " g" });
  }
  if (e.intervall) {
    const i = e.intervall;
    ki.push({
      jel: "⏱",
      cim: "Időzítő",
      reszlet: i.kor + " kör · " + i.munka + " mp munka"
          + (i.piheno > 0 ? " · " + i.piheno + " mp pihenő" : ""),
    });
  }
  return ki;
}

/* --------------------------------------------------------------- mentés */

/**
 * Egy mondat elmentése – szükség esetén több napra.
 *
 * A „hétfőn és szerdán is úsztam" két különböző nap. A mag megmondja, melyik
 * napokra: ha megnevezte őket, azokra tesszük; ha csak azt tudjuk, hány
 * alkalom hány napra oszlik, egyenletesen osztjuk el. Enélkül a heti kép
 * hazudna: minden a beírás napjára esne.
 */
function mentes(szoveg, e) {
  const m = e.mozgas || { tetelek: [], napjai: null, napok: 1, eltolas: 0 };
  const alkalmak = m.tetelek.reduce((a, t) => a + (t.alkalom || 1), 0);
  let eltolasok = null;
  if (m.napjai && m.napjai.length > 0) {
    eltolasok = m.napjai.slice();
  } else if (alkalmak > 1 && m.napok > 1) {
    eltolasok = [];
    for (let i = 0; i < alkalmak; i++) {
      eltolasok.push(m.eltolas + Math.floor((i * m.napok) / alkalmak));
    }
  }

  const ujak = [];
  if (!eltolasok || eltolasok.length < 2) {
    ujak.push({ id: azonosito(), ts: idopont(e), szoveg: szoveg, e: e });
  } else {
    // Alkalmanként egy-egy bejegyzés, a saját napjára. Az étel és a mérés
    // csak egyszer kerül be: azt nem osztjuk szét a napok között.
    let elso = true;
    for (const el of eltolasok) {
      const resz = JSON.parse(JSON.stringify(e));
      resz.mozgas.tetelek = resz.mozgas.tetelek.map(
          (t) => Object.assign({}, t, { alkalom: 1 }));
      if (!elso) {
        resz.etel = [];
        resz.ero = [];
        resz.test = { kg: 0, zsir: 0, cm: [0, 0, 0, 0, 0] };
        resz.alvas = -1; resz.pulzus = -1;
        resz.kcal = -1; resz.egetett = -1; resz.feherje = -1;
      }
      ujak.push({ id: azonosito(), ts: idopont(resz, el), szoveg: szoveg, e: resz });
      elso = false;
    }
  }
  naplo = naplo.concat(ujak);
  naplo.sort((a, b) => a.ts - b.ts);
  ment(NAPLO_KULCS, naplo);
  return ujak.length;
}

function azonosito() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
}

function torol(id) {
  naplo = naplo.filter((b) => b.id !== id);
  ment(NAPLO_KULCS, naplo);
  rajzol();
}

/* ------------------------------------------------------------ összesítés */

function napiOsszeg(kulcs) {
  const o = { perc: 0, km: 0, lepes: 0, bevitel: 0, feherje: 0, egetett: 0,
              suly: 0, alvas: 0, pulzus: 0, sorozat: 0 };
  for (const b of naplo) {
    if (napKulcs(b.ts) !== kulcs) continue;
    const e = b.e;
    for (const t of (e.mozgas ? e.mozgas.tetelek : [])) {
      const n = t.alkalom || 1;
      o.perc += n * (t.perc || 0);
      o.km += n * (t.km || 0);
      o.lepes = Math.max(o.lepes, t.lepes || 0);
    }
    for (const f of e.etel) {
      o.bevitel += f.kcal || 0;
      o.feherje += f.feherje || 0;
    }
    for (const it of e.ero) o.sorozat += it.sorozatok.length;
    // A kimondott szám erősebb a becslésnél: aki beírja, hogy 2200 kcal
    // volt a nap, az pontosabban tudja, mint mi az ételekből.
    if (e.kcal > 0) o.bevitel = Math.max(o.bevitel, e.kcal);
    if (e.feherje > 0) o.feherje = Math.max(o.feherje, e.feherje);
    if (e.egetett > 0) o.egetett += e.egetett;
    if (e.test.kg > 0) o.suly = e.test.kg;
    if (e.alvas > 0) o.alvas = e.alvas;
    if (e.pulzus > 0) o.pulzus = e.pulzus;
  }
  return o;
}

/* --------------------------------------------------------------- rajzolás */

function elem(tag, osztaly, szoveg) {
  const el = document.createElement(tag);
  if (osztaly) el.className = osztaly;
  if (szoveg != null) el.textContent = szoveg;
  return el;
}

/**
 * A legutóbbi felismerés – hogy mentéskor ne kelljen újra elemezni.
 *
 * A mentés gomb csak akkor aktív, ha van kész felismerés, tehát ilyenkor
 * az eredmény már megvan. Enélkül a mentés fél másodpercet várna.
 */
let utolso = { szoveg: null, e: null };
let elonezetSzam = 0;

function rajzolElonezet() {
  const doboz = document.getElementById("elonezet");
  const gomb = document.getElementById("mentes");
  const szoveg = document.getElementById("mondat").value.trim();
  if (!szoveg) {
    doboz.textContent = "";
    utolso = { szoveg: null, e: null };
    gomb.disabled = true;
    return Promise.resolve(null);
  }
  const sajat = ++elonezetSzam;
  // Amíg az új mondat felismerése fut, a RÉGI előnézet félrevezető lenne –
  // más mondathoz tartozik. Elhalványítjuk, és a mentést letiltjuk, amíg
  // meg nem jön a mostani válasz.
  if (utolso.szoveg !== szoveg) {
    doboz.classList.add("dolgozik");
    gomb.disabled = true;
  }
  return elemez(szoveg).then((e) => {
    // Közben tovább gépelt: ez a válasz már a múlté.
    if (sajat !== elonezetSzam) return null;
    doboz.textContent = "";
    doboz.classList.remove("dolgozik");
    if (ures(e)) {
      doboz.className = "elonezet semmi";
      doboz.textContent = "Ebből még nem ismerek fel semmit. Nézd meg a példákat!";
      utolso = { szoveg: null, e: null };
      gomb.disabled = true;
      return null;
    }
    doboz.className = "elonezet";
    for (const sor of sorok(e)) {
      const s = elem("div", "sor");
      s.append(elem("span", "jel", sor.jel));
      const t = elem("span");
      t.append(elem("b", null, sor.cim));
      if (sor.reszlet) t.append(elem("span", "reszlet", " · " + sor.reszlet));
      s.append(t);
      doboz.append(s);
    }
    utolso = { szoveg: szoveg, e: e };
    gomb.disabled = false;
    return e;
  });
}

function rajzolOsszeg() {
  const cel = document.getElementById("osszegzo");
  cel.textContent = "";
  const o = napiOsszeg(maKulcs());
  const kartyak = [];
  if (o.perc > 0) kartyak.push(["mozgas", o.perc, "perc mozgás"]);
  if (o.km > 0) kartyak.push(["mozgas", szam(o.km, true), "kilométer"]);
  if (o.lepes > 0) kartyak.push(["mozgas", szam(o.lepes), "lépés"]);
  if (o.sorozat > 0) kartyak.push(["mozgas", o.sorozat, "sorozat"]);
  if (o.bevitel > 0) kartyak.push(["etel", szam(o.bevitel), "kcal bevitel"]);
  if (o.feherje > 0) kartyak.push(["etel", szam(o.feherje), "g fehérje"]);
  if (o.egetett > 0) kartyak.push(["etel", szam(o.egetett), "kcal elégetve"]);
  if (o.suly > 0) kartyak.push(["test", szam(o.suly, true), "kg"]);
  if (o.alvas > 0) kartyak.push(["test", szam(o.alvas, true), "óra alvás"]);
  if (o.pulzus > 0) kartyak.push(["test", o.pulzus, "nyugalmi pulzus"]);

  for (const [tipus, ertek, cimke] of kartyak) {
    const d = elem("div", "szam " + tipus);
    d.append(elem("b", null, String(ertek)));
    d.append(elem("span", null, cimke));
    cel.append(d);
  }
}

function bejegyzesElem(b, mutatIdot) {
  const li = elem("li", "tetel");
  const s = sorok(b.e);
  li.append(elem("span", "jel", s.length ? s[0].jel : "📝"));
  const torzs = elem("div", "torzs");
  torzs.append(elem("div", "cim", b.szoveg));
  const reszlet = s.map((x) => x.cim + (x.reszlet ? " (" + x.reszlet + ")" : ""));
  if (reszlet.length) torzs.append(elem("div", "alcim", reszlet.join(" · ")));
  if (mutatIdot) torzs.append(elem("div", "idopont", ora(b.ts)));
  li.append(torzs);
  const x = elem("button", "torol", "×");
  x.type = "button";
  x.setAttribute("aria-label", "Bejegyzés törlése");
  x.addEventListener("click", () => {
    if (confirm("Töröljem ezt a bejegyzést?")) torol(b.id);
  });
  li.append(x);
  return li;
}

function rajzolMai() {
  const lista = document.getElementById("mai-lista");
  lista.textContent = "";
  const maiak = naplo.filter((b) => napKulcs(b.ts) === maKulcs())
      .sort((a, b) => b.ts - a.ts);
  if (maiak.length === 0) {
    const li = elem("li");
    li.append(elem("div", "ures", "Ma még nincs bejegyzés. Írj egy mondatot!"));
    lista.append(li);
    return;
  }
  for (const b of maiak) lista.append(bejegyzesElem(b, true));
}

function rajzolNaplo() {
  const cel = document.getElementById("naplo");
  cel.textContent = "";
  if (naplo.length === 0) {
    cel.append(elem("div", "ures", "Üres a napló."));
    return;
  }
  const napok = new Map();
  for (const b of naplo) {
    const k = napKulcs(b.ts);
    if (!napok.has(k)) napok.set(k, []);
    napok.get(k).push(b);
  }
  const kulcsok = Array.from(napok.keys()).sort().reverse();
  for (const k of kulcsok) {
    const o = napiOsszeg(k);
    const doboz = elem("div", "nap");
    const cim = elem("h3", null, napCime(k) + " ");
    const reszek = [];
    if (o.perc > 0) reszek.push(o.perc + " perc");
    if (o.km > 0) reszek.push(szam(o.km, true) + " km");
    if (o.bevitel > 0) reszek.push(szam(o.bevitel) + " kcal");
    if (o.suly > 0) reszek.push(szam(o.suly, true) + " kg");
    if (reszek.length) cim.append(elem("span", null, "· " + reszek.join(" · ")));
    doboz.append(cim);
    const ul = elem("ul", "lista");
    for (const b of napok.get(k).sort((a, b2) => b2.ts - a.ts)) {
      ul.append(bejegyzesElem(b, true));
    }
    doboz.append(ul);
    cel.append(doboz);
  }
}

let peldakToltve = false;

function rajzolPeldak() {
  const cel = document.getElementById("peldak");
  // A betöltés aszinkron: a puszta „van-e már gyereke" vizsgálat két gyors
  // koppintásnál kétszer töltené be a listát.
  if (peldakToltve) return;
  peldakToltve = true;
  magHiv("library", "").then((json) => {
    let csoportok;
    try {
      csoportok = json ? JSON.parse(json) : [];
    } catch (e) {
      csoportok = [];
    }
    epitPeldak(cel, csoportok);
  });
}

function epitPeldak(cel, csoportok) {
  for (const cs of csoportok) {
    const doboz = elem("div", "pelda-csoport");
    doboz.append(elem("h3", null, cs.cim));
    doboz.append(elem("p", "halvany", cs.alcim));
    for (const p of cs.peldak.slice(0, 8)) {
      const gomb = elem("button", "pelda", p);
      gomb.type = "button";
      gomb.addEventListener("click", () => {
        valtLap("ma");
        const mezo = document.getElementById("mondat");
        mezo.value = p;
        rajzolElonezet();
        mezo.focus();
        window.scrollTo({ top: 0 });
      });
      doboz.append(gomb);
    }
    cel.append(doboz);
  }
}

function rajzol() {
  rajzolOsszeg();
  rajzolMai();
  rajzolNaplo();
  const a = document.getElementById("adatallapot");
  if (a) a.textContent = naplo.length + " bejegyzés a naplóban.";
}

/* ----------------------------------------------------------------- lapok */

function valtLap(nev) {
  for (const l of document.querySelectorAll(".lap")) {
    l.classList.toggle("rejtve", l.id !== "lap-" + nev);
  }
  for (const f of document.querySelectorAll(".ful")) {
    const aktiv = f.dataset.lap === nev;
    f.classList.toggle("aktiv", aktiv);
    f.setAttribute("aria-selected", aktiv ? "true" : "false");
  }
  if (nev === "peldak") rajzolPeldak();
  window.scrollTo({ top: 0 });
}

/* ------------------------------------------------------------ visszajelzés */

let piritoIdo = 0;
function pirit(uzenet, hiba) {
  const p = document.getElementById("pirito");
  p.textContent = uzenet;
  p.style.background = hiba ? "#FF4757" : "";
  p.style.color = hiba ? "#fff" : "";
  p.classList.add("lathato");
  clearTimeout(piritoIdo);
  piritoIdo = setTimeout(() => p.classList.remove("lathato"), 2200);
}

/* ------------------------------------------------------------------ indulás */

function indul() {
  munkas = munkasIndul();
  const mezo = document.getElementById("mondat");
  let ido = 0;
  mezo.addEventListener("input", () => {
    clearTimeout(ido);
    // A felismerés minden leütésre lefut; a rövid késleltetés attól óv,
    // hogy gépelés közben akadjon a mező a régebbi telefonokon.
    ido = setTimeout(rajzolElonezet, 120);
  });

  document.getElementById("urlap").addEventListener("submit", (ev) => {
    ev.preventDefault();
    const szoveg = mezo.value.trim();
    if (!szoveg) return;
    const kesz = (e) => {
      if (ures(e)) return;
      const db = mentes(szoveg, e);
      mezo.value = "";
      mezo.blur();
      rajzolElonezet();
      rajzol();
      pirit(db > 1 ? db + " bejegyzés mentve" : "Mentve");
    };
    // Az előnézet eredményét használjuk, ha ugyanahhoz a szöveghez tartozik.
    if (utolso.szoveg === szoveg && utolso.e) kesz(utolso.e);
    else elemez(szoveg).then(kesz);
  });

  for (const f of document.querySelectorAll(".ful")) {
    f.addEventListener("click", () => valtLap(f.dataset.lap));
  }
  document.getElementById("sugoGomb")
      .addEventListener("click", () => valtLap("peldak"));

  // Beállítások
  for (const [id, kulcs] of [["magassag", "magassag"], ["celsuly", "celsuly"]]) {
    const el = document.getElementById(id);
    if (beallitas[kulcs] != null) el.value = beallitas[kulcs];
    el.addEventListener("change", () => {
      beallitas[kulcs] = el.value === "" ? null : Number(el.value);
      ment(BEALLITAS_KULCS, beallitas);
    });
  }

  document.getElementById("kiment").addEventListener("click", () => {
    const csomag = { verzio: VERZIO, mentve: new Date().toISOString(),
                     naplo: naplo, beallitas: beallitas };
    const blob = new Blob([JSON.stringify(csomag, null, 1)],
        { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "grit-naplo-" + maKulcs() + ".json";
    document.body.append(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  });

  document.getElementById("behoz").addEventListener("change", (ev) => {
    const f = ev.target.files && ev.target.files[0];
    if (!f) return;
    const olvaso = new FileReader();
    olvaso.onload = () => {
      try {
        const csomag = JSON.parse(String(olvaso.result));
        const lista = Array.isArray(csomag) ? csomag : csomag.naplo;
        if (!Array.isArray(lista)) throw new Error("rossz formátum");
        // Az azonosító alapján fűzzük össze: a kétszeri visszatöltés ne
        // duplázza meg a hónapokat.
        const megvan = new Set(naplo.map((b) => b.id));
        let uj = 0;
        for (const b of lista) {
          if (b && b.id && !megvan.has(b.id)) { naplo.push(b); uj++; }
        }
        naplo.sort((a, b) => a.ts - b.ts);
        ment(NAPLO_KULCS, naplo);
        if (csomag.beallitas) {
          beallitas = Object.assign(beallitas, csomag.beallitas);
          ment(BEALLITAS_KULCS, beallitas);
        }
        rajzol();
        pirit(uj + " új bejegyzés visszatöltve");
      } catch (e) {
        pirit("Ez a fájl nem Grit-mentés", true);
      }
      ev.target.value = "";
    };
    olvaso.readAsText(f);
  });

  document.getElementById("verzio").textContent =
      "Grit web " + VERZIO + " · a felismerés az Androidos appéval azonos";

  rajzol();

  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("sw.js").catch(() => { /* nem baj */ });
  }
}

document.addEventListener("DOMContentLoaded", indul);
