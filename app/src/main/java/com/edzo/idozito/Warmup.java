package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;

/**
 * Bemelegítő sorozatok a mai munkasúlyhoz.
 *
 * A teremben ez fejben megy, és fejben szokott elromlani: vagy túl keveset
 * melegít be az ember (és az első munkaszéria lesz a bemelegítés), vagy
 * annyit, hogy a munkasorozatokra már nem marad erő. A rámpa maga triviális
 * matek – a bajt az okozza, hogy a végén nem KEREK súly jön ki, amit rá lehet
 * rakni a rúdra.
 *
 * Ezért itt minden súly a rúdra ténylegesen felrakható értékre kerekedik: a
 * legkisebb tárcsa 1,25 kg, párban 2,5 kg, tehát a rúdtól 2,5 kg-os lépcsőkben
 * lehet haladni.
 *
 * Tiszta Java (nincs Context), hogy egységteszttel lefedhető legyen.
 */
public final class Warmup {

    private Warmup() {
    }

    /** A rúdtól ekkora lépcsőkben lehet terhelni (2 × 1,25 kg-os tárcsa). */
    static final double STEP = 2.5;

    /** Ennél könnyebb munkasúlyhoz nincs értelme rámpát építeni. */
    static final double MIN_WORK = 30;

    /** Legfeljebb ennyi bemelegítő sorozat: a bemelegítés nem edzés. */
    public static final int MAX_SETS = 4;

    public static final class Set {
        /** A rúdra teendő teljes súly kg-ban (a rúddal együtt). */
        public final double weight;
        public final int reps;
        /** Az arány a munkasúlyhoz képest, 0–100. */
        public final int percent;

        Set(double weight, int reps, int percent) {
            this.weight = weight; this.reps = reps; this.percent = percent;
        }

        /** „40 kg × 5  ·  50%”. */
        public String label() {
            return kg(weight) + " kg × " + reps + "  ·  " + percent + "%";
        }
    }

    /**
     * A rámpa lépcsői: arány a munkasúlyhoz, és a hozzá tartozó ismétlésszám.
     *
     * Felfelé egyre kevesebb ismétlés: a nehezebb bemelegítő már az idegrendszert
     * hangolja, nem a keringést, és a fáradás itt még tiszta veszteség.
     */
    private static final int[][] RAMP = {{50, 5}, {70, 3}, {85, 2}};

    /**
     * Bemelegítő sorozatok a megadott munkasúlyhoz.
     *
     * @param workWeight a mai munkasúly kg-ban (0 = saját testsúly)
     * @param bar        a rúd súlya kg-ban (0 = nincs rúd, pl. kézisúlyzó)
     * @return a sorozatok könnyűtől nehézig; üres lista, ha nincs mit melegíteni
     */
    public static List<Set> forWork(double workWeight, double bar) {
        List<Set> out = new ArrayList<>();
        if (workWeight <= 0 || workWeight > 1000 || bar < 0 || bar > workWeight) return out;
        // A könnyű munkasúlyhoz a rámpa nem ad semmit: a „20 kg-os bicepsz"
        // bemelegítése maga a 20 kg-os bicepsz első sorozata.
        if (workWeight < MIN_WORK) return out;

        // Az üres rúd az első lépcső – de csak ha van rúd, és van mit rátenni.
        if (bar > 0 && workWeight - bar >= STEP * 2)
            out.add(new Set(bar, 10, (int) Math.round(bar / workWeight * 100)));

        double prev = bar;
        for (int[] r : RAMP) {
            double w = round(workWeight * r[0] / 100.0, bar);
            // Kihagyjuk azt a lépcsőt, ami nem visz feljebb, és azt is, ami már
            // gyakorlatilag a munkasúly – abból nem bemelegítés lesz, hanem
            // egy elpazarolt munkaszéria.
            if (w <= prev + 0.01 || w >= workWeight - 0.01) continue;
            out.add(new Set(w, r[1], (int) Math.round(w / workWeight * 100)));
            prev = w;
        }
        while (out.size() > MAX_SETS) out.remove(0);
        return out;
    }

    /**
     * A rúdra felrakható legközelebbi súly: a rúdtól 2,5 kg-os lépcsőkben.
     * Rúd nélkül (kézisúlyzó, gép) marad a 2,5 kg-os osztás nullától.
     */
    static double round(double w, double bar) {
        double over = Math.max(0, w - bar);
        return bar + Math.round(over / STEP) * STEP;
    }

    /** Súly kiírása: egész, ha kerek (40), egyébként egy tizedessel (42,5). */
    static String kg(double w) {
        if (Math.abs(w - Math.round(w)) < 0.05) return String.valueOf(Math.round(w));
        return Hu.d1(w);
    }

    /** Egysoros összefoglaló: „20×10 · 45×5 · 62,5×3 · 75×2”. */
    public static String summary(List<Set> sets) {
        if (sets == null || sets.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Set s : sets) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(kg(s.weight)).append("×").append(s.reps);
        }
        return sb.toString();
    }
}
