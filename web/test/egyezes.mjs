// Az egyezés-teszt JavaScript-oldala.
//
// A lefordított magot ugyanazon a korpuszon futtatja, mint a JVM, és
// soronként ugyanazt írja ki. A hívó a két kimenetet hasonlítja össze.
//
// A korpusz több ezer mondat, és a böngészőbeli motor mondatonként
// ezredmásodperceket kér – egy szálon ez percekbe telik. A munkát ezért
// szétosztjuk a magok között: mindegyik szál a saját sorait dolgozza fel,
// a sorrend pedig a helyük szerint áll össze, hogy a kimenet ugyanaz
// legyen, mint egy szálon.
import { readFileSync } from "node:fs";
import { availableParallelism } from "node:os";
import { createRequire } from "node:module";
import { resolve } from "node:path";
import { Worker, isMainThread, parentPort, workerData } from "node:worker_threads";

const require = createRequire(import.meta.url);

function futtat(magUt, sorok, now) {
  const mag = require(magUt);
  // A leglassabb mondatot is számon tartjuk. A böngésző regex-motorja egy
  // rosszul megírt mintán percekig is elszöszölhet ugyanazon a mondaton,
  // amit a JVM ezredmásodpercek alatt elintéz – a telefonon ez fagyás.
  let csucs = 0;
  let csucsSor = "";
  const ki = sorok.map((sor) => {
    const t = process.hrtime.bigint();
    let v;
    try {
      v = mag.parse(sor, now);
    } catch (e) {
      v = "HIBA:" + (e && e.name ? e.name : String(e));
    }
    const ms = Number(process.hrtime.bigint() - t) / 1e6;
    if (ms > csucs) {
      csucs = ms;
      csucsSor = sor;
    }
    return v;
  });
  return { ki, csucs, csucsSor };
}

function jelent(csucs, csucsSor) {
  process.stderr.write("LEGLASSABB " + csucs.toFixed(0) + " " + csucsSor + "\n");
}

if (!isMainThread) {
  parentPort.postMessage(futtat(workerData.magUt, workerData.sorok, workerData.now));
} else {
  const [, , magArg, korpuszUt, nowStr] = process.argv;
  // A magot útvonalként töltjük be, nem csomagnévként: relatív
  // hívásnál a require különben a node_modules között keresné.
  const magUt = resolve(magArg);
  const now = Number(nowStr);
  const sorok = readFileSync(korpuszUt, "utf8").split("\n").filter((s) => s.length > 0);

  const szalak = Math.max(1, Math.min(availableParallelism(), 8));
  if (szalak === 1 || sorok.length < 200) {
    const e = futtat(magUt, sorok, now);
    process.stdout.write(e.ki.join("\n") + "\n");
    jelent(e.csucs, e.csucsSor);
  } else {
    // Körkörös osztás: a lassú mondatok így nem egyetlen szálon torlódnak.
    const reszek = Array.from({ length: szalak }, () => []);
    sorok.forEach((sor, i) => reszek[i % szalak].push(sor));
    const eredmeny = await Promise.all(reszek.map((resz) => new Promise((kesz, hiba) => {
      const w = new Worker(new URL(import.meta.url), {
        workerData: { magUt, sorok: resz, now },
      });
      w.on("message", kesz);
      w.on("error", hiba);
      w.on("exit", (kod) => {
        if (kod !== 0) hiba(new Error("a szál " + kod + " kóddal állt le"));
      });
    })));
    const ki = new Array(sorok.length);
    let csucs = 0;
    let csucsSor = "";
    for (let sz = 0; sz < szalak; sz++) {
      eredmeny[sz].ki.forEach((sor, i) => { ki[i * szalak + sz] = sor; });
      if (eredmeny[sz].csucs > csucs) {
        csucs = eredmeny[sz].csucs;
        csucsSor = eredmeny[sz].csucsSor;
      }
    }
    process.stdout.write(ki.join("\n") + "\n");
    jelent(csucs, csucsSor);
  }
}
