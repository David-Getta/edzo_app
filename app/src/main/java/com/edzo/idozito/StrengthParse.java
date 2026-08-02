package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;

/**
 * Erősítő sorozatok felvétele EGY mondatból: „3x10 fekvenyomás 60 kg”,
 * „guggolás 5x5 80 kg-mal”, „húzódzkodás 3x8”, „bicepsz 12-10-8 15 kg”.
 *
 * A kézi felvétel (gyakorlat + sorozatonként két mező) pontos, de lassú: aki a
 * terem után gyorsan beírná, amit csinált, az egy mondatban gondolkodik. A
 * felismerés itt is óvatos: amit nem ért, azt kihagyja – a mentés előtt pedig
 * a hívó megmutatja, mit értett.
 *
 * Tisztán szöveg → adat átalakítás, Context nélkül: így tesztelhető.
 */
public final class StrengthParse {

    private StrengthParse() {}

    /** Egy sorozat: ismétlés + súly (0 = saját testsúly). */
    public static final class Set {
        public final int reps;
        public final double weight;
        public Set(int reps, double weight) { this.reps = reps; this.weight = weight; }
    }

    /** Egy gyakorlat a hozzá tartozó sorozatokkal. */
    public static final class Item {
        public final String name;
        public final List<Set> sets;
        Item(String name, List<Set> sets) { this.name = name; this.sets = sets; }
        public int totalReps() { int r = 0; for (Set s : sets) r += s.reps; return r; }
        public double topWeight() {
            double m = 0; for (Set s : sets) m = Math.max(m, s.weight); return m;
        }
        /** Emberi összefoglaló az előnézethez: „Guggolás · 3×10 · 60 kg”. */
        public String label() {
            StringBuilder sb = new StringBuilder(name);
            sb.append("  ·  ");
            boolean same = true;
            for (Set s : sets) if (s.reps != sets.get(0).reps) { same = false; break; }
            if (same && sets.size() > 1) sb.append(sets.size()).append("×").append(sets.get(0).reps);
            else {
                for (int i = 0; i < sets.size(); i++) {
                    if (i > 0) sb.append('-');
                    sb.append(sets.get(i).reps);
                }
            }
            double w = topWeight();
            if (w > 0) sb.append("  ·  ").append(Progression.kg(w)).append(" kg");
            else sb.append("  ·  saját testsúly");
            return sb.toString();
        }
    }

    /**
     * Ismert gyakorlatok: {szép név, szótövek…}. A leghosszabb illeszkedő tő
     * nyer, hogy az összetett nevek jól dőljenek el („fekvenyomás” ne legyen
     * „fekvőtámasz”). Angol alakok is, mert a teremben azok járják.
     */
    private static final String[][] MOVES = {
            {"Guggolás", "guggol", "szkvot", "squat"},
            {"Fekvenyomás", "fekvenyom", "fekve nyom", "bench"},
            {"Felhúzás", "felhuzas", "holtemel", "deadlift"},
            {"Húzódzkodás", "huzodzkod", "pull up", "pullup", "huzodzk"},
            {"Vállból nyomás", "vallbol nyom", "vallnyom", "vallbol", "ohp"},
            {"Evezés", "evezes", "evezo", "rowing"},
            {"Bicepsz", "bicepsz", "kalapacs"},
            {"Tricepsz", "tricepsz", "francia nyom"},
            {"Kitörés", "kitores", "lunge"},
            {"Lábtolás", "labtolas", "leg press", "legpress"},
            {"Vádliemelés", "vadliemel", "vadli"},
            {"Fekvőtámasz", "fekvotamasz", "push up", "pushup"},
            {"Tolódzkodás", "tolodzkod", "dipp", "dips"},
            {"Lehúzás", "lehuzas", "latpull", "lat pull"},
            {"Oldalemelés", "oldalemel"},
            {"Plank", "plank", "deszka"},
            {"Felülés", "felules", "crunch"},
            {"Hasprés", "haspres"},
            {"Lábemelés", "labemel"},
            {"Combhajlítás", "labhajlit", "combhajlit"},
            {"Lábnyújtás", "labnyujt", "combfeszit"},
            {"Csípőemelés", "csipoemel", "hipthrust", "hip thrust", "medencelok"},
    };

    /**
     * A mondat feldolgozása. Tagmondatonként (vessző, pontosvessző, „és”,
     * „majd”, „utána”) egy-egy gyakorlat; ami tagmondatban nincs felismert
     * gyakorlat VAGY nincs értelmes ismétlésszám, az kimarad.
     */
    public static List<Item> parse(String text) {
        List<Item> out = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return out;
        for (String part : splitClauses(Foods.norm(text))) {
            Item it = parseOne(part);
            if (it != null) out.add(it);
        }
        // Ugyanaz a gyakorlat kétszer: a sorozatok egy bejegyzésbe kerülnek.
        List<Item> merged = new ArrayList<>();
        for (Item it : out) {
            Item same = null;
            for (Item m : merged) if (m.name.equals(it.name)) { same = m; break; }
            if (same == null) merged.add(it);
            else same.sets.addAll(it.sets);
        }
        return merged;
    }

    private static List<String> splitClauses(String s) {
        List<String> out = new ArrayList<>();
        // A kötőszavakat is határnak vesszük, de a „3 és fél” nem az.
        String t = s.replace(';', ',').replace('.', ',');
        t = t.replace(" majd ", ",").replace(" utana ", ",");
        int from = 0;
        while (true) {
            int p = t.indexOf(" es ", from);
            if (p < 0) break;
            boolean fraction = t.startsWith(" es fel", p);
            if (!fraction) { t = t.substring(0, p) + "," + t.substring(p + 4); }
            from = p + 1;
        }
        for (String part : t.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    /** Egy tagmondat → gyakorlat + sorozatok, vagy null. */
    private static Item parseOne(String s) {
        String name = moveIn(s);
        if (name == null) return null;

        double weight = weightIn(s);
        List<Set> sets = new ArrayList<>();

        // 1) „3x10”, „3 x 10”, „3×10”: sorozat × ismétlés.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s?[x×]\\s?(\\d{1,3})(?!\\s?(?:kg|kilo))").matcher(s);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1)), r = Integer.parseInt(m.group(2));
            if (n >= 1 && n <= 20 && r >= 1 && r <= 200)
                for (int i = 0; i < n; i++) sets.add(new Set(r, weight));
        }
        // 2) Sorozatonként más ismétlés: „12-10-8”.
        if (sets.isEmpty()) {
            m = java.util.regex.Pattern
                    .compile("(\\d{1,3})-(\\d{1,3})(?:-(\\d{1,3}))?(?:-(\\d{1,3}))?(?:-(\\d{1,3}))?")
                    .matcher(s);
            if (m.find()) {
                List<Set> tmp = new ArrayList<>();
                boolean ok = true;
                for (int g = 1; g <= m.groupCount(); g++) {
                    if (m.group(g) == null) continue;
                    int r = Integer.parseInt(m.group(g));
                    if (r < 1 || r > 200) { ok = false; break; }
                    tmp.add(new Set(r, weight));
                }
                if (ok && tmp.size() >= 2) sets.addAll(tmp);
            }
        }
        // 3) „3 sorozat 10 ismétlés” / „10 ismétlés”.
        if (sets.isEmpty()) {
            int reps = numberBefore(s, "ismetles");
            if (reps <= 0) reps = numberBefore(s, "ism");
            int series = numberBefore(s, "sorozat");
            if (series <= 0) series = numberBefore(s, "szett");
            if (series <= 0) series = numberBefore(s, "set");
            if (reps > 0 && reps <= 200) {
                int n = series > 0 && series <= 20 ? series : 1;
                for (int i = 0; i < n; i++) sets.add(new Set(reps, weight));
            } else if (series > 0 && series <= 20) {
                // „3 sorozat guggolás” – ismétlés nélkül nincs mit menteni.
                return null;
            }
        }
        // 4) Puszta darabszám gyakorlatnév mellett: „50 fekvőtámasz”.
        if (sets.isEmpty()) {
            java.util.regex.Matcher bare = java.util.regex.Pattern
                    .compile("(?<![\\d,.])(\\d{1,3})(?![\\d,.])").matcher(s);
            while (bare.find()) {
                // A súly számát ne vegyük ismétlésnek.
                int e = bare.end();
                String rest = s.substring(e).trim();
                if (rest.startsWith("kg") || rest.startsWith("kilo")) continue;
                int r = Integer.parseInt(bare.group(1));
                if (r >= 1 && r <= 200) { sets.add(new Set(r, weight)); break; }
            }
        }
        if (sets.isEmpty()) return null;
        return new Item(name, sets);
    }

    /** A leghosszabb illeszkedő gyakorlat-tő szép neve, vagy null. */
    private static String moveIn(String s) {
        String best = null;
        int bestLen = 0;
        for (String[] row : MOVES)
            for (int i = 1; i < row.length; i++)
                if (row[i].length() > bestLen && s.contains(row[i])) {
                    best = row[0];
                    bestLen = row[i].length();
                }
        return best;
    }

    /** Súly kilóban: „60 kg”, „60kg”, „60 kilóval”, „60-nal”. 0 = nincs. */
    private static double weightIn(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?(kg|kilo|kilogramm)").matcher(s);
        if (m.find()) {
            try {
                double w = Double.parseDouble(m.group(1).replace(',', '.'));
                if (w > 0 && w <= 500) return w;
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /**
     * A megadott szó ELŐTT álló szám („3 sorozat”, „10 ismétlés”). A szó
     * ragozott alakja is jó, mert csak a kezdetét keressük.
     */
    private static int numberBefore(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            int e = p;
            while (e > 0 && s.charAt(e - 1) == ' ') e--;
            int b = e;
            while (b > 0 && Character.isDigit(s.charAt(b - 1))) b--;
            if (b < e) {
                try { return Integer.parseInt(s.substring(b, e)); }
                catch (NumberFormatException ignored) {}
            }
            p = s.indexOf(word, p + 1);
        }
        return 0;
    }
}
