#!/usr/bin/env bash
#
# A webes (iPhone-os) változat magjának fordítása: Java → JavaScript.
#
# Az iPhone nem fogad APK-t, és natív iOS-appot sem lehet Apple-fiók meg
# macOS nélkül a telefonra tenni. A weblap viszont azonnal működik: a
# Safariból egy koppintással a kezdőképernyőre kerül, és onnantól úgy
# viselkedik, mint egy app. A felismerés ugyanaz marad, mert UGYANEBBŐL a
# forrásból fordul, amit az Androidos tesztek őriznek.
#
# Használat:  bash tools/webepites.sh
#
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$ROOT/web/.eszkozok"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

TEAVM_VER="0.14.1"
TEAVM_JAR="$TOOLS/teavm-cli-$TEAVM_VER-all.jar"

# 1) A fordító. Nem tesszük a verziókövetőbe (33 MB), letöltjük, ha kell.
if [ ! -s "$TEAVM_JAR" ]; then
  mkdir -p "$TOOLS"
  URL="https://repo1.maven.org/maven2/org/teavm/teavm-cli/$TEAVM_VER/teavm-cli-$TEAVM_VER-all.jar"
  echo "TeaVM letöltése…"
  for i in 1 2 3 4; do
    if curl -fsSL --max-time 300 -o "$TEAVM_JAR.reszleges" "$URL"; then
      mv "$TEAVM_JAR.reszleges" "$TEAVM_JAR"
      break
    fi
    # A Maven Central percenként korlátoz; várunk és újrapróbáljuk.
    sleep $((i * 15))
  done
fi
if [ ! -s "$TEAVM_JAR" ]; then
  echo "Nem sikerült letölteni a TeaVM-et: $TEAVM_JAR"
  exit 2
fi

# 2) A tiszta logikai mag – ugyanaz a kivonat, amin az egységtesztek futnak.
mkdir -p "$WORK/src"
if ! WEBCORE_OUT="$WORK/src" bash "$ROOT/tools/gyorsteszt.sh" >/dev/null; then
  echo "A mag kivonatolása nem sikerült."
  exit 1
fi
cp "$ROOT/web/src/com/edzo/idozito/"*.java "$WORK/src/"

# 2.5) A regex-minták fordítása egyszerivé: enélkül a böngészőben egyetlen
#      mondat felismerése hétszáz ezredmásodperc – gépelés közben
#      használhatatlan. Csak a másolatokat írjuk át; az eredeti forrás marad.
python3 "$ROOT/tools/regexgyorsito.py" "$WORK/src" || exit 1

# 3) Fordítás bájtkódra. A 8-as célszint azért kell, mert a TeaVM a
#    legújabb bájtkód-formátumokat nem mindig eszi meg.
mkdir -p "$WORK/classes" "$WORK/bld"
if ! javac -nowarn -encoding UTF-8 --release 11 -cp "$TEAVM_JAR" \
        -d "$WORK/classes" "$WORK/src"/*.java 2>&1 | grep -v '^Note:'; then
  :
fi
if [ ! -f "$WORK/classes/com/edzo/idozito/WebApi.class" ]; then
  echo "A Java fordítás nem sikerült."
  exit 1
fi

# 4) Bájtkód → JavaScript.
#
# A TeaVM „mindent egyben" csomagjából hiányzik a beépülők fele: a
# META-INF/services összefűzése elveszett a csomagolásnál, és csak a
# JCLPlugin maradt benne. Enélkül a fordítás rögtön elszáll (a JCLPlugin
# olyan szolgáltatást keres, amit a PlatformPlugin regisztrálna). A
# hiányzókat magunk soroljuk fel, és a keresési útvonal ELEJÉRE tesszük,
# hogy a függőségük előbb települjön.
mkdir -p "$WORK/svc/META-INF/services"
cat > "$WORK/svc/META-INF/services/org.teavm.vm.spi.TeaVMPlugin" <<'SVC'
org.teavm.platform.plugin.PlatformPlugin
org.teavm.jso.impl.JSOPlugin
org.teavm.metaprogramming.impl.MetaprogrammingPlugin
SVC
javac -nowarn -encoding UTF-8 -cp "$TEAVM_JAR" -d "$WORK/bld" \
    "$ROOT/web/build/Build.java" || exit 1
KIADAS="$ROOT/web/kiadas"
mkdir -p "$KIADAS"
java -cp "$WORK/svc:$TEAVM_JAR:$WORK/bld" Build "$WORK/classes" "$KIADAS" || exit 1

# 5) A statikus felület a mag mellé. A „kiadás" mappa az, amit ki lehet
#    tenni a webre: forrás és fordítási eredmény együtt, semmi más.
for f in index.html app.js munkas.js stilus.css manifest.webmanifest ikon.svg \
         ikon-180.png ikon-192.png ikon-512.png ikon-512-maszk.png; do
  [ -f "$ROOT/web/app/$f" ] && cp "$ROOT/web/app/$f" "$KIADAS/"
done

# 6) A kiszolgáló-dolgozó változat-kulcsa a fájlok tartalmából. Enélkül a
#    telefonon beragadna a régi változat: a gyorsítótár nevét semmi nem
#    változtatná meg, és a javítások hetekig nem érnének el a felhasználóhoz.
UJJ=$(cat "$KIADAS/mag.js" "$KIADAS/app.js" "$KIADAS/munkas.js" \
         "$KIADAS/stilus.css" "$KIADAS/index.html" 2>/dev/null | sha256sum | cut -c1-12)
sed "s/__VALTOZAT__/$UJJ/" "$ROOT/web/app/sw.js" > "$KIADAS/sw.js"
echo "változat: $UJJ"

# 5) A lefordított bájtkód átadása az egyezés-tesztnek: ugyanazokat az
#    osztályokat futtatja a JVM-en, mint amikből a JavaScript készült.
if [ -n "${WEBCORE_CLASSES:-}" ]; then
  mkdir -p "$WEBCORE_CLASSES"
  rm -rf "${WEBCORE_CLASSES:?}"/*
  cp -r "$WORK/classes/." "$WEBCORE_CLASSES/"
fi

echo "Kész: web/kiadas/ ($(du -sh "$KIADAS" | cut -f1))"
