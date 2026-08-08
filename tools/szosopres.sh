#!/usr/bin/env bash
#
# Szósöprés: hétköznapi magyar szavak az összes felismerő ellen.
#
# A leggyakoribb – és legcsendesebb – hibafajta az appban az, hogy egy rövid
# szótő beleakad egy hétköznapi szóba: a KÉPERNYŐben az eper, a TARTALMAzban
# az alma, a „150 graMMAl"-ban a harcművészet. A bejegyzés ilyenkor létrejön,
# csak épp nem arról szól, amit az ember írt.
#
# Ez a szkript korpuszt csinál a saját forráskód magyar kommentjeiből (több
# ezer valódi magyar szó, ragozott alakokkal együtt), és mind az öt felismerőn
# átfuttatja. Ami találatot ad, azt VÉGIG KELL NÉZNI: a lista java része
# jogos (az „alma" tényleg alma), a maradék viszont hiba.
#
# Használat:  bash tools/szosopres.sh [> talalatok.txt]
#
# Ami kell hozzá: JDK és a junit jar (ugyanaz, mint a gyorsteszthez).
#
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/app/src/main/java/com/edzo/idozito"
WORK="$(mktemp -d)"
OUT="$WORK/out"
PKG="$WORK/src/com/edzo/idozito"
mkdir -p "$PKG" "$OUT"

# 1) Korpusz: magyar szavak a kommentekből, a kód-azonosítók nélkül.
python3 - "$SRC" "$WORK/szavak.txt" <<'PY'
import sys, re, glob
src, out = sys.argv[1], sys.argv[2]
words, ident = set(), set()
for f in glob.glob(src + '/*.java'):
    s = open(f).read()
    for m in re.finditer(r'//(.*)$|/\*(.*?)\*/', s, re.S | re.M):
        t = (m.group(1) or '') + ' ' + (m.group(2) or '')
        for w in re.findall(r'[A-Za-zÁÉÍÓÖŐÚÜŰáéíóöőúüű]{4,}', t):
            words.add(w.lower())
    # A kód (kommentek nélkül) azonosítói NEM magyar szavak.
    s = re.sub(r'//.*$', '', s, flags=re.M)
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    for w in re.findall(r'[A-Za-z_]{3,}', s):
        ident.add(w.lower())
keep = sorted(w for w in words if w not in ident)
open(out, 'w').write('\n'.join(keep))
print('korpusz: %d szó' % len(keep), file=sys.stderr)
PY

# 1b) KÜLSŐ korpusz: ötvenezer szavas magyar gyakorisági lista.
#
# A kommentekből épített korpusz a MI szókincsünk – tele étel- és
# edzés-szavakkal, és épp azok a hétköznapi szavak hiányoznak belőle,
# amikbe a rövid szótövek beleakadnak. A külső listával futtatva negyvenegy
# valódi hiba jött ki elsőre: a „meghalt" halat, a „hosszabb" zabpelyhet,
# a „szobában" babot naplózott.
#
# A listát nem tesszük a repóba (huszonötezer sor idegen adat): letöltjük,
# ha nincs meg, és hálózat nélkül csendben kihagyjuk.
LISTA="${SZOSOPRES_LISTA:-${TMPDIR:-/tmp}/hu_50k.txt}"
LISTA_URL="https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/hu/hu_50k.txt"
LISTA_SZO="${SZOSOPRES_SZO:-25000}"
if [ ! -s "$LISTA" ] && [ "${SZOSOPRES_LETOLT:-1}" = "1" ]; then
  curl -sS -m 60 -o "$LISTA" "$LISTA_URL" 2>/dev/null || true
fi
if [ -s "$LISTA" ]; then
  python3 - "$LISTA" "$WORK/szavak.txt" "$LISTA_SZO" <<'PY'
import sys
src, out, limit = sys.argv[1], sys.argv[2], int(sys.argv[3])
have = set(open(out).read().split('\n'))
add, n = [], 0
for line in open(src, encoding='utf-8'):
    w = line.split(' ')[0].strip().lower()
    if len(w) < 4:
        continue
    n += 1
    if n > limit:
        break
    if w not in have:
        add.append(w)
        have.add(w)
open(out, 'a', encoding='utf-8').write('\n' + '\n'.join(add))
print('gyakorisági lista: +%d szó' % len(add), file=sys.stderr)
PY
else
  echo "gyakorisági lista nincs meg – csak a saját korpusz fut" >&2
fi

# 2) A vizsgáló osztály. A szavakat FÁJLBÓL olvassa: huszonötezer szót egy
#    Java tömb-literálba írni túllépné a statikus inicializáló méretkorlátját.
cat > "$PKG/Sopres.java" <<'JAVA'
package com.edzo.idozito;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Egy szó, minden felismerőn. Ami találatot ad, azt VÉGIG KELL NÉZNI. */
public class Sopres {
    public static void main(String[] x) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(x[0]), StandardCharsets.UTF_8));
        PrintWriter w = new PrintWriter(new OutputStreamWriter(
                System.out, StandardCharsets.UTF_8), true);
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        long now = 1753869600000L;
        int n = 0, total = 0;
        String q;
        while ((q = r.readLine()) != null) {
            if (q.isEmpty()) continue;
            total++;
            StringBuilder sb = new StringBuilder();
            for (Foods.Hit h : Foods.parse(all, q)) sb.append("etel:").append(h.food.name).append(" ");
            Activities.Parsed p = Activities.parse(q, now);
            for (Activities.Plan pl : p.plans) sb.append("sport:").append(pl.kind.id).append(" ");
            for (StrengthParse.Item i : StrengthParse.parse(q)) sb.append("sorozat:").append(i.name).append(" ");
            if (IntervalParse.parse(q) != null) sb.append("idozito ");
            BodyParse.Body b = BodyParse.parse(q);
            if (!b.isEmpty()) sb.append("meres:").append(b.label()).append(" ");
            Rehab.Area rh = Rehab.forComplaint(q);
            if (rh != null) sb.append("rehab:").append(rh.id);
            if (sb.length() > 0) { w.println(q + "  ->  " + sb); n++; }
        }
        w.println("--- " + total + " szo, " + n + " talalat");
    }
}
JAVA

# 3) A tiszta Java osztályok (ugyanaz a válogatás, mint a gyorsteszté).
for f in Rehab Kcal BodyParse Sentence Days Hu Muscles Activities StrengthParse IntervalParse Load Progression Warmup Mobility Routines; do
  [ -f "$SRC/$f.java" ] && cp "$SRC/$f.java" "$PKG/"
done
python3 - "$SRC/Foods.java" "$PKG/Foods.java" <<'PY'
import sys
lines = open(sys.argv[1]).read().split('\n')
out = []; i = 0
while i < len(lines):
    if 'android.content.Context' in lines[i] and 'static' in lines[i]:
        seen = False; depth = 0
        while i < len(lines):
            depth += lines[i].count('{') - lines[i].count('}')
            if '{' in lines[i]: seen = True
            i += 1
            if seen and depth <= 0: break
        continue
    out.append(lines[i]); i += 1
open(sys.argv[2], 'w').write('\n'.join(out))
PY

# A Progression a súlyzós naplóra hivatkozik: abból csak az adatszerkezet kell.
python3 - "$SRC/StrengthLog.java" "$PKG/StrengthLog.java" <<'PY2'
import sys, re
sl = open(sys.argv[1]).read()
def grab(text, sig):
    i = text.index(sig); j = i; depth = 0; seen = False
    while j < len(text):
        if text[j] == '{': depth += 1; seen = True
        elif text[j] == '}':
            depth -= 1
            if seen and depth == 0: return text[i:j+1]
        j += 1
    raise SystemExit('nem talalom: ' + sig)
common = re.search(r'(public static final String\[\] COMMON\s*=\s*\{.*?\};)', sl, re.S).group(1)
open(sys.argv[2], 'w').write(
    "package com.edzo.idozito;\nimport java.util.*;\npublic final class StrengthLog {\n"
    "    public static final class SetEntry { public final int reps; public final double weight;\n"
    "        public SetEntry(int r, double w) { reps = r; weight = w; } }\n"
    "    public static final class Entry { public final long ts; public final String name;\n"
    "        public final List<SetEntry> sets; public final int rpe;\n"
    "        public Entry(long t, String n, List<SetEntry> s) { this(t, n, s, 0); }\n"
    "        public Entry(long t, String n, List<SetEntry> s, int r) { ts = t; name = n;\n"
    "            sets = s; rpe = r >= 6 && r <= 10 ? r : 0; }\n"
    "        public double topWeight() { double m = 0;\n"
    "            for (SetEntry x : sets) m = Math.max(m, x.weight); return m; }\n"
    "        public double volume() { double v = 0;\n"
    "            for (SetEntry x : sets) v += x.reps * x.weight; return v; }\n"
    "        public int totalReps() { int r2 = 0;\n"
    "            for (SetEntry x : sets) r2 += x.reps; return r2; } }\n"
    "    " + common + "\n"
    "    public static int dayDiff(long a, long b) { return (int) ((b - a) / 86400000L); }\n"
    "    " + grab(sl, 'public static String setLabel(') + "\n}\n")
PY2

# A Sleep tárolása Context-es; ide csak a tiszta rész kell.
python3 - "$SRC/Sleep.java" "$PKG/Sleep.java" <<'PYS'
import sys
lines = open(sys.argv[1]).read().split('\n')
out = []; i = 0
while i < len(lines):
    if 'android.content.Context' in lines[i] and 'static' in lines[i]:
        seen = False; depth = 0
        while i < len(lines):
            depth += lines[i].count('{') - lines[i].count('}')
            if '{' in lines[i]: seen = True
            i += 1
            if seen and depth <= 0: break
        continue
    out.append(lines[i]); i += 1
open(sys.argv[2], 'w').write('\n'.join(out))
PYS

# A Pulse ugyanígy: csak a tiszta parse/verdict kell.
python3 - "$SRC/Pulse.java" "$PKG/Pulse.java" <<'PYS'
import sys
lines = open(sys.argv[1]).read().split('\n')
out = []; i = 0
while i < len(lines):
    if 'android.content.Context' in lines[i] and 'static' in lines[i]:
        seen = False; depth = 0
        while i < len(lines):
            depth += lines[i].count('{') - lines[i].count('}')
            if '{' in lines[i]: seen = True
            i += 1
            if seen and depth <= 0: break
        continue
    out.append(lines[i]); i += 1
open(sys.argv[2], 'w').write('\n'.join(out))
PYS

javac -d "$OUT" "$PKG"/*.java 2>&1 | grep -v '^Note:' || true
if [ ! -f "$OUT/com/edzo/idozito/Sopres.class" ]; then
  echo "A fordítás nem sikerült."; rm -rf "$WORK"; exit 1
fi
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp "$OUT" com.edzo.idozito.Sopres "$WORK/szavak.txt"
CODE=$?
rm -rf "$WORK"
exit $CODE
