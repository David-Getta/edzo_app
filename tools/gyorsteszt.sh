#!/usr/bin/env bash
#
# Gyorsteszt: a tiszta Java logika egységtesztjei Android SDK nélkül.
#
# Miért van erre szükség? A projekt tesztjei a Gradle `testDebugUnitTest`
# feladatban futnak, ahhoz viszont Android SDK kell. Ahol nincs (fejlesztői
# sandbox, gyors ellenőrzés egy laptopon), ott a CI-ra várni percekbe kerül –
# és egy elgépelt teszt is piros buildet okoz.
#
# A logikai osztályok viszont szándékosan tiszta Javák: nem hívnak Android
# API-t, hogy egységteszttel lefedhetők legyenek. Ezeket egy JDK-val itt
# helyben is le lehet futtatni, másodpercek alatt.
#
# Amit NEM vált ki: ez nem fordítja le az appot, és nem futtatja azokat a
# teszteket, amelyek Context-et vagy JSON-t igényelnek. A CI marad az igazság
# forrása – ez csak azért van, hogy a hibák többsége odáig el se jusson.
#
# Használat:  bash tools/gyorsteszt.sh
#
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/app/src/main/java/com/edzo/idozito"
TST="$ROOT/app/src/test/java/com/edzo/idozito"
WORK="$(mktemp -d)"
OUT="$WORK/out"
PKG="$WORK/src/com/edzo/idozito"
mkdir -p "$PKG" "$OUT"

# A junit a Gradle telepítésével érkezik; ha máshol van, add meg a JUNIT_JARS-ban.
if [ -z "${JUNIT_JARS:-}" ]; then
  JUNIT_JARS="$(ls -d /opt/gradle*/lib/junit-4*.jar 2>/dev/null | head -1)"
  HAM="$(ls -d /opt/gradle*/lib/hamcrest-core-*.jar 2>/dev/null | head -1)"
  [ -n "${HAM:-}" ] && JUNIT_JARS="$JUNIT_JARS:$HAM"
fi
if [ -z "${JUNIT_JARS:-}" ] || [ ! -e "${JUNIT_JARS%%:*}" ]; then
  echo "Nem találom a junit jar-t. Add meg így:  JUNIT_JARS=/út/junit.jar:/út/hamcrest.jar bash $0"
  exit 2
fi

# 0) Hiányzó import a UI-osztályokban: az Activity-k csak a CI-ben fordulnak,
#    egy elfelejtett import ott hét percbe kerül, itt egy másodpercbe.
if command -v python3 >/dev/null 2>&1; then
  python3 "$(dirname "$0")/importcheck.py" || exit 1
fi

# 1) Teljesen tiszta osztályok: mehetnek egy az egyben.
for f in Days Hu Progression Muscles Mobility Alarms Activities StrengthParse Examples Load MealIdeas IntervalParse Weekplan Bests TimeHint Habits Warmup Routines; do
  [ -f "$SRC/$f.java" ] && cp "$SRC/$f.java" "$PKG/"
done
# Az Alarms Android-része (egyszeri riasztás beállítása) nem kell a teszthez.
python3 - "$PKG/Alarms.java" <<'PY'
import sys
p = sys.argv[1]
s = open(p).read()
s = s.replace('import android.app.AlarmManager;\n', '').replace('import android.app.PendingIntent;\n', '')
i = s.find('    public static void oneShot(')
if i > 0: s = s[:i] + '}\n'
open(p, 'w').write(s)
PY

# 2) A Foods publikus, Context-es burkolói nélkül (a belső, tiszta változatok maradnak).
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

# 3) Nagy, Android-függő osztályokból csak a tesztelt, statikus számítások.
python3 - "$SRC" "$PKG" <<'PY'
import sys, re
src_dir, dst = sys.argv[1] + '/', sys.argv[2] + '/'

def grab(text, sig):
    i = text.index(sig); j = i; depth = 0; seen = False
    while j < len(text):
        if text[j] == '{': depth += 1; seen = True
        elif text[j] == '}':
            depth -= 1
            if seen and depth == 0: return text[i:j+1]
        j += 1
    raise SystemExit('nem találom: ' + sig)

ts = open(src_dir + 'TimerService.java').read()
open(dst + 'TimerService.java', 'w').write(
    "package com.edzo.idozito;\npublic class TimerService {\n"
    "    private static final long TICK_MS = 200;\n    static final double MIN_RUN_M = 300;\n    "
    + grab(ts, 'static long nextTickDelay(') + "\n    " + grab(ts, 'static long stepBase(')
    + "\n    " + grab(ts, 'static double calories(double weightKg, double distanceM, int durationSec)')
    + "\n    " + grab(ts, 'static double calories(double weightKg, double distanceM, int durationSec, String name)')
    + "\n    " + grab(ts, 'static boolean isRun(') + "\n}\n")

pr = open(src_dir + 'Profile.java').read()
open(dst + 'Profile.java', 'w').write(
    "package com.edzo.idozito;\npublic final class Profile {\n"
    "    public static final double[] RATES = {0.25, 0.5, 0.75, 1.0};\n"
    "    public static final double ACTIVITY = 1.4;\n"
    "    static final double MAX_CREDIT = 800;\n    "
    + grab(pr, 'public static int effectiveGoal(') + "\n    "
    + grab(pr, 'public static double bmr(') + "\n    " + grab(pr, 'public static double tdee(')
    + "\n    " + grab(pr, 'public static double dailyDeficit(')
    + "\n    " + grab(pr, 'public static double intakeForLoss(')
    + "\n    " + grab(pr, 'public static double weeklyTrend(')
    + "\n    " + grab(pr, 'public static double weeksToGoal(') + "\n}\n")

ss = open(src_dir + 'SessionStore.java').read()
open(dst + 'SessionStore.java', 'w').write(
    "package com.edzo.idozito;\npublic final class SessionStore {\n"
    '    static final String PREFIX = "s_", SUFFIX = ".json";\n    '
    + grab(ss, 'static long tsOfFile(') + "\n    " + grab(ss, 'static long[] timestampsOf(') + "\n}\n")

sl = open(src_dir + 'StrengthLog.java').read()
common = re.search(r'(public static final String\[\] COMMON\s*=\s*\{.*?\};)', sl, re.S).group(1)
open(dst + 'StrengthLog.java', 'w').write(
    "package com.edzo.idozito;\nimport java.util.*;\npublic final class StrengthLog {\n"
    "    public static final class SetEntry { public final int reps; public final double weight;\n"
    "        public SetEntry(int r, double w) { reps = r; weight = w; } }\n"
    "    public static final class Entry { public final long ts; public final String name;\n"
    "        public final List<SetEntry> sets; public final int rpe;\n"
    "        public Entry(long t, String n, List<SetEntry> s) { this(t, n, s, 0); }\n"
    "        public Entry(long t, String n, List<SetEntry> s, int r) { ts = t; name = n;\n"
    "            sets = s; rpe = r >= 6 && r <= 10 ? r : 0; } }\n"
    "    " + common + "\n"
    "    public static int dayDiff(long a, long b) { return (int) ((b - a) / 86400000L); }\n}\n")
PY

# 4) Azok a tesztek, amiknek a fentiek elegendők.
TESTS="ActivitiesTest ActivitiesParseTest ActivitiesIntegrationTest ActivitiesTimestampTest ActivitiesBreakdownTest ActivitiesMissedSportTest FoodsTest FoodsParseTest FoodsCompoundTest FoodsQuantityTest FoodsFitnessTest FoodsPieceTest FoodsIntegrationTest FoodsDataQualityTest ParserFuzzTest
       TimerTickTest TimerCaloriesTest TimerRunTest ProfileEnergyTest ProfileTrendTest SessionOrderTest
       MusclesTest MusclesNamesTest ProgressionTest ProgressionBodyweightTest
       DaysTest HuTest AlarmsTest MobilityTest StrengthParseTest ExamplesTest LoadTest MealIdeasTest IntervalParseTest WeekplanTest BestsTest TimeHintTest HabitsTest WarmupTest RoutinesTest"
CLASSES=""
for t in $TESTS; do
  if [ -f "$TST/$t.java" ]; then cp "$TST/$t.java" "$PKG/"; CLASSES="$CLASSES com.edzo.idozito.$t"; fi
done

javac -cp "$JUNIT_JARS" -d "$OUT" "$PKG"/*.java 2>&1 | grep -v '^Note:' || true
if [ ! -f "$OUT/com/edzo/idozito/Foods.class" ]; then
  echo "A fordítás nem sikerült."; rm -rf "$WORK"; exit 1
fi

# Amit itt NEM tudunk futtatni (Context vagy Android-osztály kell hozzá), az
# csak a CI-ben derül ki – hét perc múlva. Kiírjuk a nevüket, hogy egy közös
# képlet átírásakor eszébe jusson az embernek átnézni őket.
SKIPPED=""
RAN=" $(echo $TESTS) "          # idézőjel nélkül: a sortörések szóközzé esnek
for f in "$TST"/*.java; do
  t="$(basename "$f" .java)"
  case "$RAN" in *" $t "*) ;; *) SKIPPED="$SKIPPED $t";; esac
done

java -cp "$JUNIT_JARS:$OUT" org.junit.runner.JUnitCore $CLASSES
CODE=$?
rm -rf "$WORK"
if [ -n "$SKIPPED" ]; then
  echo
  echo "Csak a CI-ben fut (Context kell hozzá):$SKIPPED"
fi
exit $CODE
