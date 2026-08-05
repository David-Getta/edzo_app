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
        /** Érzett terhelés a mondatból („rpe 8”), 0 = nem mondta. */
        public int rpe;
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
            if (rpe > 0) sb.append("  ·  RPE ").append(rpe);
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
            {"Fekvenyomás", "fekvenyom", "fekve nyom", "bench", "ferde pad"},
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
            {"Lehúzás", "lehuzas", "latpull", "lat pull", "lat huzas"},
            {"Oldalemelés", "oldalemel", "eloreemel"},
            {"Plank", "plank", "deszka", "oldaltamasz"},
            {"Felülés", "felules", "crunch"},
            {"Hasprés", "haspres", "hasizom"},
            {"Lábemelés", "labemel"},
            {"Combhajlítás", "labhajlit", "combhajlit"},
            {"Lábnyújtás", "labnyujt", "combfeszit", "labgep"},
            {"Csípőemelés", "csipoemel", "hipthrust", "hip thrust", "medencelok",
                    "farizom"},
            {"Arnold nyomás", "arnold"},
            {"Fordított tárogatás", "forditott tarogat", "hatso vall", "hatso deltoid"},
            {"Csuklyás emelés", "csuklyas", "shrug"},
            {"Hátizom gép", "hatizom", "hatgep"},
            {"Mellgép", "mellgep", "tarogat", "pillango", "mellnyom"},
            {"Hegymászó", "hegymaszo"},
            {"Hátfeszítés", "hiperextenzi", "hatfeszit", "back extension"},
    };

    /** A felismerhető gyakorlatok szép nevei (teszthez és súgóhoz). */
    public static String[] names() {
        String[] out = new String[MOVES.length];
        for (int i = 0; i < MOVES.length; i++) out[i] = MOVES[i][0];
        return out;
    }

    /**
     * A mondat feldolgozása. Tagmondatonként (vessző, pontosvessző, „és”,
     * „majd”, „utána”) egy-egy gyakorlat; ami tagmondatban nincs felismert
     * gyakorlat VAGY nincs értelmes ismétlésszám, az kimarad.
     */
    public static List<Item> parse(String text) {
        List<Item> out = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return out;
        String whole = sets(Foods.norm(text));
        for (String part : splitClauses(whole)) {
            Item it = parseOne(part);
            if (it != null) out.add(it);
        }
        // Ugyanaz a gyakorlat kétszer: a sorozatok egy bejegyzésbe kerülnek.
        List<Item> merged = new ArrayList<>();
        for (Item it : out) {
            Item same = null;
            for (Item m : merged) if (m.name.equals(it.name)) { same = m; break; }
            if (same == null) merged.add(it);
            else {
                same.sets.addAll(it.sets);
                if (same.rpe == 0) same.rpe = it.rpe;
            }
        }
        // Egyetlen gyakorlatnál az RPE a mondat bármely részében állhat: a
        // „4x12 60 kg, 7-es rpe" vesszője tagmondatot zár, de a szám ugyanarra
        // a gyakorlatra vonatkozik. Több gyakorlatnál ezt nem találgatjuk.
        if (merged.size() == 1 && merged.get(0).rpe == 0) merged.get(0).rpe = rpeIn(whole);
        return merged;
    }

    private static List<String> splitClauses(String s) {
        // A kötőszavakat is határnak vesszük, de a „3 és fél” nem az.
        StringBuilder b = new StringBuilder(s);
        for (int i = 0; i < b.length(); i++) {
            char c = b.charAt(i);
            if (c != ';' && c != ',' && c != '.') continue;
            // A tizedesjel NEM tagmondat-határ: a „12,5 kg” egy szám.
            boolean decimal = i > 0 && i + 1 < b.length()
                    && Character.isDigit(b.charAt(i - 1)) && Character.isDigit(b.charAt(i + 1));
            if (!decimal) b.setCharAt(i, '|');
        }
        String t = b.toString().replace(" majd ", "|").replace(" utana ", "|");
        int from = 0;
        while (true) {
            int p = t.indexOf(" es ", from);
            if (p < 0) break;
            if (!t.startsWith(" es fel", p)) t = t.substring(0, p) + "|" + t.substring(p + 4);
            from = p + 1;
        }
        List<String> out = new ArrayList<>();
        for (String part : t.split("\\|")) {
            String p = part.trim();
            if (!p.isEmpty()) out.addAll(splitByMoves(p));
        }
        return out;
    }

    /**
     * Kötőszó nélkül felsorolt gyakorlatok: „guggolás 3x10 60kg fekvenyomás
     * 3x8 50kg”. A második (és további) gyakorlatnév kezdeténél vágunk, hogy
     * mindegyik megkapja a saját sorozatait.
     */
    private static List<String> splitByMoves(String s) {
        List<Integer> cuts = new ArrayList<>();
        for (String[] row : MOVES) {
            int best = -1, bestLen = 0;
            for (int i = 1; i < row.length; i++) {
                int p = s.indexOf(row[i]);
                if (p >= 0 && row[i].length() > bestLen) { best = p; bestLen = row[i].length(); }
            }
            if (best >= 0) cuts.add(best);
        }
        List<String> out = new ArrayList<>();
        if (cuts.size() < 2) { out.add(s); return out; }
        java.util.Collections.sort(cuts);
        // A MÁSODIK gyakorlatnévtől vágunk: az első elé írt bevezető
        // („két gyakorlat: guggolás…”) az első darabhoz tartozik.
        int prev = 0;
        for (int i = 1; i < cuts.size(); i++) {
            int c = cuts.get(i);
            if (c <= prev) continue;
            String part = s.substring(prev, c).trim();
            if (!part.isEmpty() && moveIn(part) != null) { out.add(part); prev = c; }
        }
        String rest = s.substring(prev).trim();
        if (!rest.isEmpty()) out.add(rest);
        return out;
    }

    /**
     * Kiírt számok számjeggyé, és a „háromszor tízet” alak sorozat×ismétlésre.
     *
     * A teremben ritkán ír bárki számjegyet: „nyomtam ötször ötöt”, „háromszor
     * tizenkettőt”. Eddig ezekből nem lett bejegyzés – vagy ami rosszabb, a
     * puszta szám ismétlésszámnak látszott.
     */
    static String sets(String s) {
        return Hu.digits(s)
                .replaceAll("(\\d{1,2})\\s?(?:szor|szer)\\s+(\\d{1,3})", "$1x$2");
    }

    /** Egy tagmondat → gyakorlat + sorozatok, vagy null. */
    private static Item parseOne(String s) {
        String name = moveIn(s);
        if (name == null) return null;

        double weight = weightIn(s);
        List<Set> sets = new ArrayList<>();

        // 1) „3x10”, „3 x 10”, „3×10”: sorozat × ismétlés. A teremben szokásos
        //    „3x10x60” harmadik tagja maga a súly.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s?[x×]\\s?(\\d{1,3})(?:\\s?[x×]\\s?(\\d{1,3}(?:[.,]\\d{1,2})?))?"
                        + "(?!\\s?(?:kg|kilo))").matcher(s);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1)), r = Integer.parseInt(m.group(2));
            if (weight == 0 && m.group(3) != null) {
                try {
                    double w = Double.parseDouble(m.group(3).replace(',', '.'));
                    if (w > 0 && w <= 500) weight = w;
                } catch (NumberFormatException ignored) {}
            }
            // Mértékegység nélkül írt súly a sorozat után: „3x10 60”.
            if (weight == 0 && m.group(3) == null) {
                java.util.regex.Matcher w2 = java.util.regex.Pattern
                        .compile("^\\s*(\\d{1,3}(?:[.,]\\d{1,2})?)(?![\\dx×])")
                        .matcher(s.substring(m.end()));
                if (w2.find()) {
                    try {
                        double w = Double.parseDouble(w2.group(1).replace(',', '.'));
                        if (w > 0 && w <= 500) weight = w;
                    } catch (NumberFormatException ignored) {}
                }
            }
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
                if (isWeightSuffixed(s, e)) continue;
                int r = Integer.parseInt(bare.group(1));
                if (r >= 1 && r <= 200) { sets.add(new Set(r, weight)); break; }
            }
        }
        if (sets.isEmpty()) return null;
        Item it = new Item(name, sets);
        it.rpe = rpeIn(s);
        return it;
    }

    /**
     * Érzett terhelés a tagmondatból: „rpe 8”, „rpe8”, „8-as rpe”. Csak a
     * 6–10 sáv életszerű; ami ezen kívül esik, az nem RPE, hanem valami más
     * szám a mondatban.
     */
    private static int rpeIn(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("rpe\\s*-?\\s*(\\d{1,2})|(\\d{1,2})\\s*-?\\s*(?:as|es|os)?\\s*rpe")
                .matcher(s);
        while (m.find()) {
            String g = m.group(1) != null ? m.group(1) : m.group(2);
            if (g == null) continue;
            try {
                int v = Integer.parseInt(g);
                if (v >= 6 && v <= 10) return v;
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
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
        // Mértékegység nélküli, de eszközraggal írt súly: „100-zal", „80-nal",
        // „60-al". Enélkül a „guggoltam 100-zal ötször ötöt" száz ismétlésnek
        // olvasódott – onnantól a rekordok és az 1RM is hazudtak volna.
        m = java.util.regex.Pattern
                .compile("(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?-?\\s?(zal|val|vel|nal|nel|lal|lel|al|el)\\b")
                .matcher(s);
        if (m.find()) {
            try {
                double w = Double.parseDouble(m.group(1).replace(',', '.'));
                if (w > 0 && w <= 500) return w;
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /** A súlyt jelölő eszközragos szám („100-zal") ne legyen ismétlésszám. */
    private static boolean isWeightSuffixed(String s, int end) {
        String rest = s.substring(end);
        return rest.matches("^\\s?-?\\s?(zal|val|vel|nal|nel|lal|lel|al|el)\\b.*");
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
