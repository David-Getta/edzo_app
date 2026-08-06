package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;

/**
 * Személyes csúcsok: a leghosszabb edzés, a legnagyobb táv, a leggyorsabb
 * tempó, a legtöbb lépés, a legtöbb elégetett kalória és a legnagyobb napi
 * volumen – dátummal együtt.
 *
 * A statisztika eddig átlagokat és összegeket mutatott. Az átlag arról szól,
 * hogy milyen szokott lenni; a csúcs arról, hogy mire vagy képes – és pont
 * ezért marad meg. Az „idei éved" kártya említ egy-két rekordot, de csak az
 * idei évből és csak az időzítős edzésekből.
 *
 * Szándékosan tiszta Java (nincs Context), hogy egységteszttel lefedhető
 * legyen: egy rosszul kiszámolt rekord évekig ott marad a kártyán.
 */
public final class Bests {

    private Bests() {
    }

    /** Ennél rövidebb edzés nem „leghosszabb": a fél perces próba nem rekord. */
    static final int MIN_DUR_SEC = 600;
    /** Táv- és tempó-rekordhoz legalább ennyi méter kell. */
    static final double MIN_DIST_M = 1000;
    /** Lépés-rekordhoz ennyi kell: háromszáz lépés nem „legtöbb lépés". */
    static final int MIN_STEPS = 1000;
    /** Életszerű tempó-tartomány perc/km-ben – ezen kívül mérési hiba. */
    static final double MIN_PACE = 2.0, MAX_PACE = 15.0;

    public static final class Best {
        public final String emoji, label, value;
        /** A rekord napja; 0, ha nincs értelmes időpont. */
        public final long ts;

        Best(String emoji, String label, String value, long ts) {
            this.emoji = emoji; this.label = label; this.value = value; this.ts = ts;
        }
    }

    /**
     * Ennél könnyebb sorozat nem „legnehezebb emelés": a 20 kilós bicepsz
     * rekordja senkit nem érdekel, és elnyomná a valódi csúcsot.
     */
    static final double MIN_LIFT_KG = 30;

    /**
     * Testsúlyos rekordhoz ennyi kell: öt fekvőtámasz nem rekord, húsz
     * másodperces plank sem. A küszöb nélkül az első próbálkozás beülne a
     * kártyára, és onnan csak sokára mozdulna ki.
     */
    static final int MIN_BW_REPS = 10, MIN_HOLD_SEC = 30;

    /**
     * A legnehezebb valaha felemelt sorozat és a legjobb becsült 1RM.
     *
     * A napi volumen a munka MENNYISÉGÉT méri – ez a kettő az ERŐT, és a
     * teremben ez az, amire emlékszik az ember. Külön metódus, mert a
     * gyakorlat neve is kell hozzá, a többi csúcshoz meg nem.
     *
     * @param ts      sorozatonként a bejegyzés napja
     * @param names   sorozatonként a gyakorlat neve
     * @param weights sorozatonként a súly kg-ban
     * @param reps    sorozatonként az ismétlésszám
     */
    public static List<Best> ofLifts(long[] ts, String[] names, double[] weights, int[] reps) {
        List<Best> out = new ArrayList<>();
        if (ts == null || names == null || weights == null || reps == null) return out;
        int n = Math.min(ts.length, Math.min(names.length,
                Math.min(weights.length, reps.length)));
        double bestKg = 0, bestOrm = 0;
        long bestKgTs = 0, bestOrmTs = 0;
        String bestKgName = null, bestOrmName = null;
        int bestKgReps = 0;
        int bestBw = 0, bestHold = 0;
        long bestBwTs = 0, bestHoldTs = 0;
        String bestBwName = null, bestHoldName = null;
        for (int i = 0; i < n; i++) {
            double w = weights[i];
            int r = reps[i];
            String name = names[i];
            if (name == null || name.trim().isEmpty()) continue;
            // Testsúlyos sorozat: nincs kilója, ezért a súlyrekordokból kiesne –
            // pedig aki fekvőtámaszozik és húzódzkodik, annak PONT ez a rekordja.
            if (w <= 0) {
                if (StrengthParse.isTimed(name)) {
                    if (r >= MIN_HOLD_SEC && r <= 3600 && r > bestHold) {
                        bestHold = r; bestHoldTs = ts[i]; bestHoldName = name;
                    }
                } else if (r >= MIN_BW_REPS && r <= 1000 && r > bestBw) {
                    bestBw = r; bestBwTs = ts[i]; bestBwName = name;
                }
                continue;
            }
            if (w < MIN_LIFT_KG || w > 1000 || r < 1 || r > 100) continue;
            if (w > bestKg) {
                bestKg = w; bestKgTs = ts[i]; bestKgName = name; bestKgReps = r;
            }
            // Tíz fölött a képlet már túlbecsül, ezért ott nem becslünk.
            if (r <= 10) {
                double orm = Progression.oneRm(w, r);
                if (orm > bestOrm) { bestOrm = orm; bestOrmTs = ts[i]; bestOrmName = name; }
            }
        }
        if (bestKg > 0)
            out.add(new Best("💪", "Legnehezebb emelés  ·  " + bestKgName,
                    Hu.kg(bestKg) + " kg × " + bestKgReps, bestKgTs));
        if (bestOrm > 0)
            out.add(new Best("📊", "Legjobb becsült 1RM  ·  " + bestOrmName,
                    Math.round(bestOrm) + " kg", bestOrmTs));
        if (bestBw > 0)
            out.add(new Best("🤸", "Legtöbb ismétlés  ·  " + bestBwName,
                    bestBw + " db", bestBwTs));
        if (bestHold > 0)
            out.add(new Best("🧘", "Leghosszabb tartás  ·  " + bestHoldName,
                    StrengthParse.hold(bestHold), bestHoldTs));
        return out;
    }

    /**
     * Melyik gyakorlatban dőlt meg a rekord az adott időpont ÓTA?
     *
     * A heti összegzés eddig a hét legnehezebb emelését írta ki – abból nem
     * derült ki, hogy az mindenkori csúcs volt-e, vagy csak a hét legnehezebb
     * napja. Pedig a kettő között van a különbség aközött, hogy „ez volt a
     * hét" és „ilyet még soha".
     *
     * Csak akkor rekord, ha VOLT mihez mérni: az első alkalom nem rekord,
     * különben minden új gyakorlat rögtön ünneplést kapna.
     *
     * @return „Guggolás 120 kg" alakú sorok, a legnehezebbtől lefelé
     */
    public static List<String> newRecordsSince(long since, long[] ts, String[] names,
                                               double[] weights) {
        return newRecordsSince(since, ts, names, weights, null);
    }

    /**
     * Ugyanaz, de az ismétlésszámmal együtt – így a testsúlyos csúcsok is
     * előkerülnek.
     *
     * Aki fekvőtámaszozik és plankol, annak eddig egyetlen új csúcsa sem
     * jelent meg az összegzésben: a rekordok kiló alatt indultak, a testsúlyos
     * sorozatnak meg nincs kilója. Pedig a negyvenről negyvenötre lépő
     * fekvőtámasz ugyanúgy rekord.
     *
     * @param reps sorozatonként az ismétlésszám (tartásnál másodperc); null =
     *             csak a súlyos rekordok, mint korábban
     */
    public static List<String> newRecordsSince(long since, long[] ts, String[] names,
                                               double[] weights, int[] reps) {
        List<String> out = new ArrayList<>();
        if (ts == null || names == null || weights == null) return out;
        int n = Math.min(ts.length, Math.min(names.length, weights.length));
        if (reps != null) n = Math.min(n, reps.length);

        double[] kg = new double[n], bw = new double[n], hold = new double[n];
        for (int i = 0; i < n; i++) {
            double w = weights[i];
            if (w >= MIN_LIFT_KG && w <= 1000) { kg[i] = w; continue; }
            if (reps == null || w > 0) continue;
            int r = reps[i];
            if (StrengthParse.isTimed(names[i])) {
                if (r >= MIN_HOLD_SEC && r <= 3600) hold[i] = r;
            } else if (r >= MIN_BW_REPS && r <= 1000) {
                bw[i] = r;
            }
        }
        // Sorrend: a kilós csúcsok elöl, aztán az ismétlés, végül a tartás –
        // ezek nem hasonlíthatók össze egymással, ezért nem is keverjük őket.
        for (Object[] r : improvedSince(since, ts, names, kg, n))
            out.add(r[0] + " " + Hu.kg((Double) r[1]) + " kg");
        for (Object[] r : improvedSince(since, ts, names, bw, n))
            out.add(r[0] + " " + (int) (double) (Double) r[1] + " db");
        for (Object[] r : improvedSince(since, ts, names, hold, n))
            out.add(r[0] + " " + StrengthParse.hold((int) (double) (Double) r[1]));
        return out;
    }

    /**
     * Névre bontva: melyik érték javult a határidő óta? A legnagyobb elöl.
     *
     * A nulla érték nem játszik: a hívó azzal jelzi, hogy az adott sorozat
     * ebbe a fajta rekordba nem tartozik bele.
     *
     * @return {név, érték} párok
     */
    private static List<Object[]> improvedSince(long since, long[] ts, String[] names,
                                                double[] vals, int n) {
        java.util.LinkedHashMap<String, double[]> before = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, double[]> after = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            String name = names[i];
            double v = vals[i];
            if (name == null || name.trim().isEmpty() || v <= 0) continue;
            java.util.LinkedHashMap<String, double[]> m = ts[i] >= since ? after : before;
            double[] cur = m.get(name);
            if (cur == null) m.put(name, new double[]{v});
            else if (v > cur[0]) cur[0] = v;
        }
        List<Object[]> out = new ArrayList<>();
        for (java.util.Map.Entry<String, double[]> e : after.entrySet()) {
            double[] old = before.get(e.getKey());
            if (old == null || e.getValue()[0] <= old[0]) continue;
            out.add(new Object[]{e.getKey(), e.getValue()[0]});
        }
        // A legnagyobb elöl: ha több rekord is született, az a beszédesebb.
        for (int i = 0; i < out.size(); i++)
            for (int j = i + 1; j < out.size(); j++)
                if ((Double) out.get(j)[1] > (Double) out.get(i)[1]) {
                    Object[] t = out.get(i); out.set(i, out.get(j)); out.set(j, t);
                }
        return out;
    }

    /**
     * Az időzítős és kézi edzések csúcsai: leghosszabb edzés, leghosszabb táv,
     * leggyorsabb tempó, legtöbb lépés, legtöbb kalória, legnagyobb napi volumen.
     *
     * @param ts      az időzítős/kézi edzések időbélyegei
     * @param durSec  hosszuk másodpercben (azonos hosszú tömb)
     * @param distM   távjuk méterben (0 = nincs)
     * @param cal     elégetett kalória (0 = nincs)
     * @param steps   lépésszám (0 = nincs)
     * @param liftTs  a súlyzós bejegyzések időbélyegei
     * @param liftVol a súlyzós bejegyzések volumene (ismétlés × súly)
     * @return a megtalált csúcsok, megjelenítési sorrendben; üres, ha nincs egy sem
     */
    public static List<Best> of(long[] ts, int[] durSec, double[] distM, double[] cal,
                                int[] steps, long[] liftTs, double[] liftVol) {
        List<Best> out = new ArrayList<>();
        int n = ts == null ? 0 : ts.length;

        int bestDur = 0; long bestDurTs = 0;
        double bestDist = 0; long bestDistTs = 0;
        double bestPace = 0; long bestPaceTs = 0;
        int bestSteps = 0; long bestStepsTs = 0;
        double bestCal = 0; long bestCalTs = 0;

        for (int i = 0; i < n; i++) {
            int d = durSec != null && i < durSec.length ? durSec[i] : 0;
            double km = distM != null && i < distM.length ? distM[i] : 0;
            double kc = cal != null && i < cal.length ? cal[i] : 0;
            int st = steps != null && i < steps.length ? steps[i] : 0;

            if (d >= MIN_DUR_SEC && d > bestDur) { bestDur = d; bestDurTs = ts[i]; }
            if (km >= MIN_DIST_M && km > bestDist) { bestDist = km; bestDistTs = ts[i]; }
            if (st >= MIN_STEPS && st > bestSteps) { bestSteps = st; bestStepsTs = ts[i]; }
            if (kc > bestCal) { bestCal = kc; bestCalTs = ts[i]; }
            // Tempó: a KISEBB a jobb, ezért külön feltétel.
            if (km >= MIN_DIST_M && d > 0) {
                double pace = (d / 60.0) / (km / 1000.0);
                if (pace >= MIN_PACE && pace <= MAX_PACE
                        && (bestPace == 0 || pace < bestPace)) {
                    bestPace = pace; bestPaceTs = ts[i];
                }
            }
        }

        // Napi volumen: egy nap több bejegyzése egy edzés.
        double bestVol = 0; long bestVolTs = 0;
        if (liftTs != null && liftVol != null) {
            java.util.HashMap<Long, double[]> perDay = new java.util.HashMap<>();
            int m = Math.min(liftTs.length, liftVol.length);
            for (int i = 0; i < m; i++) {
                long day = Days.startOf(liftTs[i]);
                double[] row = perDay.get(day);
                if (row == null) perDay.put(day, row = new double[1]);
                row[0] += Math.max(0, liftVol[i]);
            }
            // Holtversenynél a KORÁBBI nap nyer: a rekordot akkor állítottad
            // fel, amikor először elérted. A HashMap bejárási sorrendje
            // ráadásul nem rögzített – enélkül a kártyán ugráló dátum állt.
            for (java.util.Map.Entry<Long, double[]> e : perDay.entrySet())
                if (e.getValue()[0] > bestVol
                        || (e.getValue()[0] == bestVol && bestVolTs > 0 && e.getKey() < bestVolTs)) {
                    bestVol = e.getValue()[0];
                    bestVolTs = e.getKey();
                }
        }

        if (bestDur > 0)
            out.add(new Best("⏱", "Leghosszabb edzés", fmtDur(bestDur), bestDurTs));
        if (bestDist > 0)
            out.add(new Best("📍", "Leghosszabb táv",
                    Hu.d1(bestDist / 1000.0) + " km", bestDistTs));
        if (bestPace > 0)
            out.add(new Best("⚡", "Leggyorsabb tempó", fmtPace(bestPace) + " /km", bestPaceTs));
        if (bestSteps > 0)
            out.add(new Best("👟", "Legtöbb lépés", Hu.num(bestSteps), bestStepsTs));
        if (bestCal >= 100)
            out.add(new Best("🔥", "Legtöbb kalória", Math.round(bestCal) + " kcal", bestCalTs));
        if (bestVol >= 100)
            out.add(new Best("🏋", "Legnagyobb napi volumen",
                    Math.round(bestVol) + " kg", bestVolTs));
        return out;
    }

    /** „1 ó 12 p" / „48 perc". */
    static String fmtDur(int sec) {
        int m = sec / 60;
        return m >= 60 ? (m / 60) + " ó " + (m % 60) + " p" : m + " perc";
    }

    /** Tempó perc:másodperc alakban. */
    static String fmtPace(double minPerKm) {
        int total = (int) Math.round(minPerKm * 60);
        return (total / 60) + ":" + (total % 60 < 10 ? "0" : "") + (total % 60);
    }
}
