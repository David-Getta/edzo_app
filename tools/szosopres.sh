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

# 2) A vizsgáló osztály a korpusszal.
python3 - "$WORK/szavak.txt" "$PKG/Sopres.java" <<'PY'
import sys
ws = [w for w in open(sys.argv[1]).read().split('\n') if w]
esc = lambda s: s.replace('\\', '\\\\').replace('"', '\\"')
arr = ',\n'.join('            "%s"' % esc(w) for w in ws)
open(sys.argv[2], 'w').write('''package com.edzo.idozito;
import java.util.*;
public class Sopres {
    static final String[] W = {
%s
    };
    public static void main(String[] x) {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        long now = 1753869600000L;
        int n = 0;
        for (String q : W) {
            StringBuilder sb = new StringBuilder();
            for (Foods.Hit h : Foods.parse(all, q)) sb.append("etel:").append(h.food.name).append(" ");
            Activities.Parsed p = Activities.parse(q, now);
            for (Activities.Plan pl : p.plans) sb.append("sport:").append(pl.kind.id).append(" ");
            for (StrengthParse.Item i : StrengthParse.parse(q)) sb.append("sorozat:").append(i.name).append(" ");
            if (IntervalParse.parse(q) != null) sb.append("idozito ");
            BodyParse.Body b = BodyParse.parse(q);
            if (!b.isEmpty()) sb.append("meres:").append(b.label());
            if (sb.length() > 0) { System.out.println(q + "  ->  " + sb); n++; }
        }
        System.out.println("--- " + W.length + " szo, " + n + " talalat");
    }
}
''' % arr)
PY

# 3) A tiszta Java osztályok (ugyanaz a válogatás, mint a gyorsteszté).
for f in Kcal BodyParse Sentence Days Hu Muscles Activities StrengthParse IntervalParse Load Progression Warmup Mobility Routines; do
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

javac -d "$OUT" "$PKG"/*.java 2>&1 | grep -v '^Note:' || true
if [ ! -f "$OUT/com/edzo/idozito/Sopres.class" ]; then
  echo "A fordítás nem sikerült."; rm -rf "$WORK"; exit 1
fi
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp "$OUT" com.edzo.idozito.Sopres
CODE=$?
rm -rf "$WORK"
exit $CODE
