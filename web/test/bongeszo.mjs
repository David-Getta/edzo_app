/*
 * Böngészős próba: tényleg működik-e a felület.
 *
 * Az egyezés-teszt azt bizonyítja, hogy a FELISMERÉS ugyanaz, mint az
 * Androidon. Ez a próba arról szól, ami csak a böngészőben létezik: elindul-e
 * a munkás szál, megjelenik-e az előnézet, elmentődik-e a bejegyzés, és
 * túléli-e az újratöltést. iPhone-méretű ablakban fut, mert a felület oda
 * készült.
 *
 * Használat:  node web/test/bongeszo.mjs [kiadás-könyvtár]
 */
import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, resolve } from "node:path";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("playwright");

const KIADAS = resolve(process.argv[2] || "web/kiadas");

const TIPUS = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".webmanifest": "application/manifest+json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
};

function kiszolgalo() {
  return new Promise((kesz) => {
    const sz = createServer(async (kv, valasz) => {
      let ut = decodeURIComponent(new URL(kv.url, "http://x").pathname);
      if (ut === "/") ut = "/index.html";
      try {
        const test = await readFile(join(KIADAS, ut));
        valasz.writeHead(200, {
          "content-type": TIPUS[extname(ut)] || "application/octet-stream",
          // A kiszolgáló-dolgozó csak azonos eredetről telepszik; a
          // gyorsítótárat viszont kikapcsoljuk, hogy a próba mindig a
          // friss fájlokat lássa.
          "cache-control": "no-store",
        });
        valasz.end(test);
      } catch (e) {
        valasz.writeHead(404).end("nincs meg: " + ut);
      }
    });
    sz.listen(0, "127.0.0.1", () => kesz(sz));
  });
}

const allitasok = [];
function allit(mit, igaz, reszlet) {
  allitasok.push({ mit, igaz, reszlet });
  console.log((igaz ? "  ok    " : "  BUKÁS ") + mit + (reszlet ? " – " + reszlet : ""));
}

const sz = await kiszolgalo();
const cim = "http://127.0.0.1:" + sz.address().port + "/";
const bongeszo = await chromium.launch({ executablePath: "/opt/pw-browsers/chromium/chrome-linux/chrome" })
    .catch(() => chromium.launch());
const kontextus = await bongeszo.newContext(devices["iPhone 13"]);
const lap = await kontextus.newPage();

const hibak = [];
lap.on("pageerror", (e) => hibak.push(String(e)));
lap.on("console", (m) => { if (m.type() === "error") hibak.push(m.text()); });

try {
  await lap.goto(cim, { waitUntil: "load" });
  allit("a lap betölt", (await lap.title()) === "Grit");

  // 1) Felismerés gépelés közben.
  await lap.fill("#mondat", "Reggel 5 km futás 28 perc, utána két tojás");
  await lap.waitForSelector("#elonezet .sor", { timeout: 30000 });
  const elonezet = await lap.textContent("#elonezet");
  allit("az előnézet futást ismer fel", /Fut(á|a)s/.test(elonezet), elonezet.slice(0, 70));
  allit("az előnézet ételt is felismer", /Toj(á|a)s/.test(elonezet));
  allit("a mentés gomb aktív", !(await lap.isDisabled("#mentes")));

  // 2) Mentés és napi összegzés.
  await lap.click("#mentes");
  await lap.waitForSelector("#mai-lista .tetel", { timeout: 30000 });
  const osszeg = await lap.textContent("#osszegzo");
  allit("a napi összegzőben ott a táv", /5\s*kilom(é|e)ter|5\s*km/.test(osszeg), osszeg.slice(0, 80));
  allit("a mező kiürült mentés után", (await lap.inputValue("#mondat")) === "");

  // 3) Az adat túléli az újratöltést (ez a napló lényege).
  await lap.reload({ waitUntil: "load" });
  await lap.waitForSelector("#mai-lista .tetel", { timeout: 30000 });
  const maiak = await lap.locator("#mai-lista .tetel").count();
  allit("újratöltés után is megvan a bejegyzés", maiak === 1, maiak + " tétel");

  // 4) Lapváltás és példatár.
  await lap.click('.ful[data-lap="peldak"]');
  await lap.waitForSelector(".pelda", { timeout: 30000 });
  const peldakSzama = await lap.locator(".pelda").count();
  allit("a példatár feltöltődik", peldakSzama > 5, peldakSzama + " példa");
  await lap.click(".pelda");
  allit("a példa a mezőbe kerül", (await lap.inputValue("#mondat")).length > 3);

  // 5) Törlés.
  lap.on("dialog", (d) => d.accept());
  await lap.click('.ful[data-lap="ma"]');
  await lap.click("#mai-lista .torol");
  await lap.waitForSelector("#mai-lista .ures", { timeout: 30000 });
  allit("a bejegyzés törölhető", true);

  allit("nincs JavaScript-hiba a naplóban", hibak.length === 0, hibak.slice(0, 2).join(" | "));

  await lap.click('.ful[data-lap="ma"]');
  await lap.fill("#mondat", "Fekvenyomás 5x5 80 kg és 300 g csirkemell");
  // Nem elég egy sorra várni: a régi előnézet még kint lehet. A friss
  // eredményre várunk, különben a képernyőkép a előző mondatot mutatná.
  await lap.waitForFunction(
      () => {
        const d = document.querySelector("#elonezet");
        return d && !d.classList.contains("dolgozik") && /Fekvenyom/.test(d.textContent);
      },
      null, { timeout: 30000 });
  const eroElonezet = await lap.textContent("#elonezet");
  allit("az erősítő sorozat is látszik", /5×5/.test(eroElonezet), eroElonezet.slice(0, 60));
  allit("a kimondott 300 g érvényesül", /300\s*g/.test(eroElonezet));
  await lap.screenshot({ path: "/tmp/grit-web.png", fullPage: false });
} finally {
  await bongeszo.close();
  sz.close();
}

const bukott = allitasok.filter((a) => !a.igaz);
console.log("");
if (bukott.length === 0) {
  console.log("MIND A(Z) " + allitasok.length + " PRÓBA RENDBEN.");
  process.exit(0);
}
console.log(bukott.length + " próba bukott el a(z) " + allitasok.length + "-ból.");
process.exit(1);
