// Az egyezés-teszt JavaScript-oldala.
//
// A lefordított magot ugyanazon a korpuszon futtatja, mint a JVM, és
// soronként ugyanazt írja ki. A hívó a két kimenetet hasonlítja össze.
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const [, , magUt, korpuszUt, nowStr] = process.argv;
const mag = require(magUt);
const now = Number(nowStr);

const sorok = readFileSync(korpuszUt, "utf8").split("\n").filter((s) => s.length > 0);
const ki = [];
for (const sor of sorok) {
  let json;
  try {
    json = mag.parse(sor, now);
  } catch (e) {
    json = "HIBA:" + (e && e.name ? e.name : String(e));
  }
  ki.push(json);
}
process.stdout.write(ki.join("\n") + "\n");
