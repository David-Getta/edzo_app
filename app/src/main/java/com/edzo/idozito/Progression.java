package com.edzo.idozito;

import java.util.List;

/**
 * Progresszió-javaslat: mit érdemes ma nyomni egy gyakorlatból, a korábbi
 * alkalmak alapján. „Dupla progresszió": előbb az ismétlésszám kúszik fel a
 * sáv tetejéig, utána lép a súly, és az ismétlés visszaesik a sáv aljára.
 *
 * Szándékosan tiszta Java (nincs Context), hogy egységteszttel lefedhető
 * legyen – itt egy elszámolás csendben rossz edzést eredményezne.
 */
public final class Progression {

    private Progression() {}

    /** Az ismétlés-sáv, amin belül dolgozunk. */
    static final int MIN_REPS = 8, MAX_REPS = 12;

    /** Ennyi egyforma alkalom után javaslunk visszavételt. */
    static final int STALL_SESSIONS = 3;

    /**
     * Testsúlyos gyakorlatnál efölött már inkább állóképességet edzünk, nem
     * erőt. Súlyzónál a sáv tetejéről a tárcsa visz tovább – testsúlynál nincs
     * tárcsa, ezért ott a sorozatszám lép.
     */
    static final int BW_MAX_REPS = 20;

    /** Ennél több sorozatot nem javasolunk: onnan a gyakorlat nehezítése visz előre. */
    static final int BW_MAX_SETS = 6;

    public static final class Suggestion {
        public final int sets;
        public final int reps;
        public final double weight;
        /** Testsúlyos gyakorlat: nincs értelmes súlylépés, ismétlésben haladunk. */
        public final boolean bodyweight;
        /** Egymondatos indoklás – a felhasználó lássa, miért ezt kapja. */
        public final String why;

        Suggestion(int sets, int reps, double weight, boolean bodyweight, String why) {
            this.sets = sets; this.reps = reps; this.weight = weight;
            this.bodyweight = bodyweight; this.why = why;
        }

        /** Rövid, kártyára való összefoglaló, pl. „3 × 8 · 42,5 kg". */
        public String headline() {
            if (bodyweight) return sets + " × " + reps + " ismétlés";
            return sets + " × " + reps + " · " + kg(weight) + " kg";
        }
    }

    /**
     * A következő alkalom javaslata, vagy null, ha a gyakorlatot még sosem
     * naplózta (ilyenkor nincs mihez mérni – ne találjunk ki súlyt helyette).
     *
     * @param newestFirst a teljes napló, legújabb bejegyzéssel elöl
     */
    public static Suggestion next(List<StrengthLog.Entry> newestFirst, String name) {
        if (newestFirst == null || name == null) return null;
        StrengthLog.Entry last = null;
        int sameCount = 0;                 // hány egymást követő alkalom teljesen egyforma
        double lastW = 0;
        int lastHard = 0;

        for (StrengthLog.Entry e : newestFirst) {
            if (!name.equals(e.name) || e.sets == null || e.sets.isEmpty()) continue;
            double w = working(e);
            int hard = hardestReps(e, w);
            if (last == null) {
                last = e; lastW = w; lastHard = hard; sameCount = 1;
            } else if (Math.abs(w - lastW) < 0.01 && hard == lastHard) {
                sameCount++;
            } else {
                break;                     // megtört az egyformaság, tovább nem érdekes
            }
        }
        if (last == null) return null;

        int setCount = last.sets.size();
        boolean bw = lastW <= 0;

        if (bw) return bodyweight(setCount, lastHard, sameCount);

        if (sameCount >= STALL_SESSIONS) {
            double down = deload(lastW);
            if (down > 0) {
                return new Suggestion(setCount, MIN_REPS, down, false,
                        sameCount + " alkalom óta ugyanitt állsz. Vegyél vissza kb. 10%-ot, "
                                + "és építsd fel újra – így szoktál átjutni a holtponton.");
            }
        }

        if (lastHard >= MAX_REPS) {
            return new Suggestion(setCount, MIN_REPS, lastW + step(lastW), false,
                    "Múltkor minden sorozat elment " + MAX_REPS + " ismétlésig. Emeld a súlyt, "
                            + "és kezdd újra " + MIN_REPS + " ismétléstől.");
        }
        return new Suggestion(setCount, lastHard + 1, lastW, false,
                "Maradj ezen a súlyon, és told meg egy ismétléssel. " + MAX_REPS
                        + "-nél jön a következő tárcsa.");
    }

    /**
     * Testsúlyos gyakorlat javaslata.
     *
     * Itt nincs tárcsa, amivel a sáv tetejéről tovább lehetne lépni, ezért
     * korábban egyszerűen minden alkalommal egy ismétléssel többet javasoltunk –
     * megállás nélkül. Aki követte a tanácsot, az néhány hónap alatt 3 × 40
     * fekvőtámaszig jutott: onnantól már nem erőt edz, hanem állóképességet, és
     * a javaslat sosem mondta meg, hogy ideje továbblépni.
     *
     * A haladás sorrendje most: ismétlés a sáv tetejéig, aztán sorozat, végül
     * a gyakorlat nehezítése – ezt már csak elmondani tudjuk, megtenni nem.
     */
    private static Suggestion bodyweight(int setCount, int reps, int sameCount) {
        if (reps < BW_MAX_REPS && sameCount < STALL_SESSIONS)
            return new Suggestion(setCount, reps + 1, 0, true,
                    "Testsúlyos gyakorlat: a leggyengébb sorozatodnál lépj egy ismétlést.");
        if (setCount < BW_MAX_SETS)
            return new Suggestion(setCount + 1, reps, 0, true,
                    reps >= BW_MAX_REPS
                            ? reps + " ismétlésnél már inkább az állóképesség fejlődik. Maradj "
                                    + "ennyinél, és tegyél hozzá még egy sorozatot."
                            : sameCount + " alkalom óta ugyanannyi. Adj hozzá még egy sorozatot – "
                                    + "testsúlynál a volumen visz előre.");
        return new Suggestion(setCount, reps, 0, true,
                "Ebből a gyakorlatból elérted a hasznos ismétlés- és sorozatszámot. Innen a "
                        + "nehezebb változat visz előre: lassabb levitel, megemelt láb, "
                        + "egy karral vagy lábbal.");
    }

    /** A munkasúly: az alkalom legnehezebb sorozata. */
    static double working(StrengthLog.Entry e) {
        double m = 0;
        for (StrengthLog.SetEntry s : e.sets) m = Math.max(m, s.weight);
        return m;
    }

    /**
     * A munkasúlyon elért legkevesebb ismétlés. Ez a szűk keresztmetszet: amíg
     * a leggyengébb sorozat sem éri el a sáv tetejét, addig nincs súlyemelés.
     */
    static int hardestReps(StrengthLog.Entry e, double w) {
        int min = Integer.MAX_VALUE;
        for (StrengthLog.SetEntry s : e.sets) {
            if (w > 0 && Math.abs(s.weight - w) > 0.01) continue;   // bemelegítő sorozat
            min = Math.min(min, s.reps);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /** A legkisebb értelmes súlylépés: kis súlyoknál kisebb tárcsa. */
    static double step(double w) {
        return w < 20 ? 1.25 : 2.5;
    }

    /** Kb. 10% visszavétel, a tárcsákhoz kerekítve. 0, ha nincs hova lejjebb. */
    static double deload(double w) {
        double st = step(w);
        double d = Math.round(w * 0.9 / st) * st;
        if (d >= w) d = w - st;
        return d > 0 ? d : 0;
    }

    /**
     * Súly magyar tizedesvesszővel, felesleges „,0" nélkül. Két tizedes kell:
     * az 1,25 kg-os lépés különben 16,3-ra kerekedne, ami tárcsákban értelmetlen.
     */
    static String kg(double w) {
        double r = Math.round(w * 100) / 100.0;
        if (Math.abs(r - Math.round(r)) < 0.005) return String.valueOf(Math.round(r));
        return String.valueOf(r).replace('.', ',');
    }
}
