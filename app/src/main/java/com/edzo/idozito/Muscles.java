package com.edzo.idozito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Gyakorlat → izomcsoport besorolás a magyar gyakorlatnevek alapján, és ebből
 * a heti egyensúly. Cél, hogy kiderüljön, ha valami rendszeresen kimarad
 * („három hete nem volt hát").
 *
 * Szándékosan óvatos: amit nem ismer fel biztosan, azt inkább besorolatlanul
 * hagyja. Egy rossz címke félrevezetőbb, mint a hiányzó.
 */
public final class Muscles {

    private Muscles() {}

    public static final String LAB = "Láb", MELL = "Mell", HAT = "Hát",
            VALL = "Váll", KAR = "Kar", TORZS = "Törzs";

    /** Megjelenítési sorrend (nagy izomcsoportok elöl). */
    public static final String[] GROUPS = {LAB, HAT, MELL, VALL, KAR, TORZS};

    /**
     * Hosszú, önmagában egyértelmű kulcsszavak: bárhol előfordulhatnak a névben.
     * A leghosszabb találat nyer, hogy az összetett nevek jól dőljenek el.
     */
    private static final String[][] LONG = {
            // A beépített programok gyakorlatai is ide tartoznak: nélkülük a heti
            // kimutatás vak arra a munkára, amit maga az app javasolt.
            {LAB, "guggol", "kitores", "labtolas", "labnyujt", "labhajlit", "labgep",
                    "vadli", "comb", "farizom", "medencelok", "hipthrust", "lepcsozes",
                    "csipoemel", "csipo emel", "falules", "fal-ules", "wall sit",
                    "fellepes", "fellepo"},
            {HAT, "huzodzkod", "felhuzas", "holtemel", "evezes", "lehuzas", "csuklyas",
                    "hatizom", "hatgep", "gerincnyujt", "hiperextenzi", "hatfeszit",
                    "szuperman", "superman"},
            {MELL, "fekvenyom", "fekve nyom", "tarogat", "pillango", "fekvotamasz",
                    "tolodzkod", "mell", "ferde pad", "ferde nyom"},
            // A „fordított tárogatás" hátsó vállra megy, nem mellre. A rövidebb
            // „tarogat" különben mellizomnak vette – rossz címke, ami félrevisz.
            {VALL, "vallbol", "vallnyom", "vallgep", "oldalemel", "arnold",
                    "forditott tarogat", "forditott pillango", "hatso vall", "hatso deltoid",
                    "vallemel", "eloreemel"},
            {KAR, "bicepsz", "tricepsz", "kalapacs", "francia", "alkar"},
            {TORZS, "hasizom", "haspres", "plank", "deszka", "crunch", "felules",
                    "oroszcsav", "orosz csav", "oldaltamasz", "torzs", "labemel",
                    "hegymaszo", "madar-kutya", "madarkutya", "madar kutya"},
    };

    /**
     * Rövid szavak, amiket CSAK önálló szóként fogadunk el. Beágyazva túl sok a
     * téves találat („labda" ≠ láb, „hatvan" ≠ hát).
     */
    private static final String[][] SHORT = {
            {LAB, "lab"}, {HAT, "hat"}, {VALL, "vall"}, {KAR, "kar"}, {TORZS, "has"},
    };

    /** A gyakorlat izomcsoportja, vagy null, ha nem ismerjük fel biztosan. */
    public static String groupOf(String name) {
        String n = Foods.norm(name == null ? "" : name);
        if (n.isEmpty()) return null;

        String best = null;
        int bestLen = 0;
        for (String[] row : LONG) {
            for (int i = 1; i < row.length; i++) {
                if (row[i].length() > bestLen && n.contains(row[i])) {
                    best = row[0];
                    bestLen = row[i].length();
                }
            }
        }
        if (best != null) return best;

        for (String tok : n.split("[^a-z0-9]+")) {
            if (tok.isEmpty()) continue;
            for (String[] row : SHORT) if (tok.equals(row[1])) return row[0];
        }
        return null;
    }

    /**
     * Hány NAPON edzette az egyes izomcsoportokat az elmúlt `days` napban.
     * Csak azokat a csoportokat adja vissza, amelyeket valaha is csinált – a
     * sosem érintett csoport hiánya nem hiányosság, hanem döntés.
     *
     * @param newestFirst a teljes erősítő napló
     */
    public static LinkedHashMap<String, Integer> weekBalance(
            List<StrengthLog.Entry> newestFirst, long now, int days) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        if (newestFirst == null) return out;

        // Melyik csoportot csinálta valaha? (Ezekre van értelme hiányt jelezni.)
        List<String> known = new ArrayList<>();
        for (StrengthLog.Entry e : newestFirst) {
            String g = groupOf(e.name);
            if (g != null && !known.contains(g)) known.add(g);
        }
        for (String g : GROUPS) if (known.contains(g)) out.put(g, 0);

        // Egy nap egy csoportnál egyszer számít, hogy a sok bejegyzés ne csaljon.
        LinkedHashMap<String, Integer> lastDay = new LinkedHashMap<>();
        for (StrengthLog.Entry e : newestFirst) {
            String g = groupOf(e.name);
            if (g == null || !out.containsKey(g)) continue;
            int ago = StrengthLog.dayDiff(e.ts, now);
            if (ago < 0 || ago >= days) continue;
            String key = g + "#" + ago;
            if (lastDay.containsKey(key)) continue;
            lastDay.put(key, 1);
            out.put(g, out.get(g) + 1);
        }
        return out;
    }

    /**
     * Mit érdemes ma bevenni: a héten kimaradt izomcsoportokból a legrégebben
     * csinált gyakorlat.
     *
     * Az egyensúly-csipek eddig megmutatták, MI maradt ki, de nem mondták meg,
     * hogy abból mit csináljon – pedig a hiány önmagában még nem terv. Csak
     * olyan gyakorlatot ajánlunk, amit a felhasználó már csinált: így van
     * mihez mérni a súlyt, és nem küldjük neki ismeretlen mozdulatot.
     *
     * Ha semelyik csoport nem maradt ki, üres listát ad – a jó hetet nem
     * kell megjegyzéssel kiegészíteni.
     *
     * @param newestFirst a teljes erősítő napló, legújabb bejegyzéssel elöl
     */
    public static List<String> suggestForToday(List<StrengthLog.Entry> newestFirst,
                                               long now, int max) {
        return suggestForToday(newestFirst, now, max, null);
    }

    /**
     * @param preferred a mai napra tervezett izomcsoport (heti fókusz), vagy null.
     *                  A felhasználó terve erősebb, mint az egyensúly-heurisztika:
     *                  ha ma hát-nap van, akkor hát-gyakorlat jöjjön elöl, akkor
     *                  is, ha a hét eddig kiegyensúlyozott volt.
     */
    public static List<String> suggestForToday(List<StrengthLog.Entry> newestFirst,
                                               long now, int max, String preferred) {
        List<String> out = new ArrayList<>();
        if (newestFirst == null || max <= 0) return out;
        LinkedHashMap<String, Integer> bal = weekBalance(newestFirst, now, 7);
        if (bal.isEmpty()) return out;
        // A tervezett csoport akkor is számít, ha csak egy csoportot edz valaki:
        // ilyenkor az egyensúly-javaslat hallgat, a terv viszont nem.
        if (preferred == null && bal.size() < 2) return out;

        // Gyakorlatonként a legutóbbi alkalom. A napló legújabbal kezdődik,
        // de a hívó sorrendjére nem támaszkodunk.
        LinkedHashMap<String, Long> lastSeen = new LinkedHashMap<>();
        for (StrengthLog.Entry e : newestFirst) {
            if (e == null || e.name == null) continue;
            Long prev = lastSeen.get(e.name);
            if (prev == null || e.ts > prev) lastSeen.put(e.name, e.ts);
        }

        // A tervezett csoport megy elöl, utána a héten kimaradtak.
        List<String> order = new ArrayList<>();
        String plan = preferred == null ? null : groupOf(preferred);
        if (plan != null) order.add(plan);
        for (String g : GROUPS) if (!order.contains(g)) order.add(g);

        for (String g : order) {
            if (out.size() >= max) break;
            Integer n = bal.get(g);
            if (n == null) continue;               // sosem csinálta ezt a csoportot
            if (n > 0 && !g.equals(plan)) continue; // nem maradt ki, és nem is tervezett
            String best = null;
            long bestTs = Long.MAX_VALUE;
            for (java.util.Map.Entry<String, Long> e : lastSeen.entrySet()) {
                if (!g.equals(groupOf(e.getKey()))) continue;
                if (e.getValue() < bestTs) { bestTs = e.getValue(); best = e.getKey(); }
            }
            if (best != null && !out.contains(best)) out.add(best);
        }
        return out;
    }

    /** „Hát", „Hát és Váll", „Hát, Váll és Kar" – String.join nélkül (API 24). */
    public static String andList(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(i == items.size() - 1 ? " és " : ", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
