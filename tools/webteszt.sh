#!/usr/bin/env bash
#
# Egyezés-teszt: a webes (iPhone-os) mag ugyanazt ismeri-e fel, mint az
# Androidos.
#
# A webes változat azért fordul Javából, hogy ne csússzon el az eredetitől –
# de ezt bizonyítani is kell. Ez a szkript ugyanazt a több ezer mondatot
# futtatja le a JVM-en és a lefordított JavaScripten, és a két kimenetnek
# KARAKTERRE egyeznie kell. A korpusz a meglévő egységtesztekből gyűlt: ott
# minden mondat mögött egy valódi, egyszer elveszett bejegyzés áll.
#
# Használat:  bash tools/webteszt.sh
#
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# A napszak és a „tegnap" a mai naphoz képest értendő, ezért az időt
# rögzítjük – különben a két futás önmagától is eltérhetne. Az időzóna
# ugyanezért UTC mindkét oldalon.
export TZ=UTC
NOW=1788696000000   # 2026-09-06 12:00 UTC

KORPUSZ="$ROOT/web/test/korpusz.txt"
if [ ! -s "$KORPUSZ" ]; then
  echo "Nincs korpusz: $KORPUSZ"
  exit 2
fi

echo "1/4  A mag fordítása…"
if ! WEBCORE_CLASSES="$WORK/classes" bash "$ROOT/tools/webepites.sh" >"$WORK/epites.log" 2>&1; then
  tail -20 "$WORK/epites.log"
  exit 1
fi
grep -E "^mag\.js|Classes compiled|Methods compiled" "$WORK/epites.log" | sed 's/^/     /'

echo "2/4  Futtatás a JVM-en, az EREDETI forrásból…"
# Szándékosan nem a webes fordítás osztályait futtatjuk: azokban a
# regex-hívások át vannak írva a gyorsítótárazó segédosztályra. Ha a JVM is
# az átírt kódot futtatná, egy átírási hiba mindkét oldalon egyformán
# jelentkezne – és a teszt zölden hazudna. Így viszont a JVM az érintetlen
# forrást futtatja, és a diff az átírás hibáját is megmutatja.
mkdir -p "$WORK/tiszta"
if ! WEBCORE_OUT="$WORK/tiszta/src" bash "$ROOT/tools/gyorsteszt.sh" >/dev/null; then
  echo "A tiszta mag kivonatolása nem sikerült."
  exit 1
fi
cp "$ROOT/web/src/com/edzo/idozito/WebApi.java" "$WORK/tiszta/src/"
# A WebApi a TeaVM @JSExport annotációját használja: a JVM-es fordításhoz
# is kell a fordító jarja (futni nem fut belőle semmi).
javac -nowarn -encoding UTF-8 -cp "$ROOT/web/.eszkozok/teavm-cli-0.14.1-all.jar" \
    -d "$WORK/tiszta/classes" \
    "$WORK/tiszta/src"/*.java "$ROOT/web/test/Referencia.java" 2>&1 | grep -v '^Note:'
if [ ! -f "$WORK/tiszta/classes/Referencia.class" ]; then
  echo "A JVM-oldal fordítása nem sikerült."
  exit 1
fi
java -cp "$WORK/tiszta/classes" Referencia "$KORPUSZ" "$NOW" > "$WORK/jvm.txt" 2>"$WORK/jvm.err" || {
  tail -5 "$WORK/jvm.err"; exit 1; }

echo "3/4  Futtatás a lefordított JavaScripten…"
node "$ROOT/web/test/egyezes.mjs" "$ROOT/web/kiadas/mag.js" "$KORPUSZ" "$NOW" \
    > "$WORK/js.txt" 2>"$WORK/js.err" || { tail -5 "$WORK/js.err"; exit 1; }

echo "4/4  A kihagyás-szűrő próbája…"
# A webes változat nem indítja el a keresést, ha a minta kötelező szava nincs
# a mondatban – ettől lett a felismerés a telefonon többszörösen gyorsabb. Ez
# csak akkor helyes, ha a szűrő SOHA nem mond tévesen nemet: minden mintát
# összevetünk minden mondattal, és ahol a szűrő kihagyna, ott a JVM saját
# motorja sem találhat semmit.
python3 "$ROOT/web/test/mintagyujtes.py" "$WORK/mintak.txt" > /dev/null || exit 1
javac -nowarn -encoding UTF-8 -d "$WORK/szuro" \
    "$ROOT/web/src/com/edzo/idozito/Rx.java" "$ROOT/web/test/Szuro.java" 2>&1 \
    | grep -v '^Note:'
if [ ! -f "$WORK/szuro/Szuro.class" ]; then
  echo "A szűrő-próba fordítása nem sikerült."
  exit 1
fi
java -cp "$WORK/szuro" Szuro "$WORK/mintak.txt" "$KORPUSZ" || exit 1

SOROK=$(wc -l < "$KORPUSZ")
if diff -q "$WORK/jvm.txt" "$WORK/js.txt" >/dev/null; then
  echo
  echo "EGYEZIK: mind a(z) $SOROK mondat ugyanazt adja a JVM-en és a weben."
  exit 0
fi

ELTERES=$(diff "$WORK/jvm.txt" "$WORK/js.txt" | grep -c '^<' || true)
echo
echo "ELTÉRÉS $ELTERES / $SOROK mondatnál. Az első néhány:"
paste -d'\t' "$KORPUSZ" "$WORK/jvm.txt" "$WORK/js.txt" \
  | awk -F'\t' '$2 != $3 { print "\n  mondat : " $1 "\n  android: " $2 "\n  web    : " $3 }' \
  | head -40
exit 1
