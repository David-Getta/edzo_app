package com.edzo.idozito;

/**
 * Intervallum-beállítás EGY mondatból: „3 kör 40 mp munka 20 mp pihenő”.
 *
 * Az étkezést, az edzést és a súlyzós sorozatot már mondatból is fel lehet
 * venni – az app magját, magát az időzítőt viszont eddig csak három csúszkával
 * lehetett beállítani. Aki a teremben kap egy edzéstervet, azt szövegként
 * kapja, nem csúszkaállásként.
 *
 * Tiszta Java (nincs Context), hogy egységteszttel lefedhető legyen: egy
 * elszámolás itt rossz edzést eredményez, és az edzés közben derül ki.
 */
public final class IntervalParse {

    private IntervalParse() {
    }

    /** Épeszű határok: ezeken kívül nem állítunk be semmit. */
    static final int MAX_ROUNDS = 50, MIN_SEC = 5, MAX_SEC = 3600;

    public static final class Plan {
        public final int rounds;
        /** Munkaidő másodpercben. */
        public final int work;
        /** Pihenő másodpercben (0 = nincs). */
        public final int rest;

        Plan(int rounds, int work, int rest) {
            this.rounds = rounds; this.work = work; this.rest = rest;
        }

        /** „8 kör · 20 mp munka · 10 mp pihenő”. */
        public String label() {
            String s = rounds + " kör  ·  " + sec(work) + " munka";
            if (rest > 0) s += "  ·  " + sec(rest) + " pihenő";
            return s;
        }

        /** A teljes edzés hossza másodpercben (az utolsó pihenő is beleszámít). */
        public int totalSec() {
            return rounds * (work + rest);
        }

        private static String sec(int s) {
            if (s % 60 == 0 && s >= 60) return (s / 60) + " perc";
            if (s > 60) return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
            return s + " mp";
        }
    }

    /**
     * Ismert edzésformák. Ezeket névvel is ki lehet mondani, mert a teremben
     * is így hívják őket – a „tabata” mindenkinek ugyanazt jelenti.
     */
    private static final String[][] PRESETS = {
            {"tabata", "8", "20", "10"},
            {"emom", "10", "60", "0"},
            {"pomodoro", "1", "1500", "300"},
    };

    /**
     * @return a felismert beállítás, vagy null, ha a mondatból nem derül ki
     *         értelmes munkaidő – találgatni itt nem szabad.
     */
    public static Plan parse(String text) {
        if (text == null) return null;
        String s = Foods.norm(text).replace('\n', ' ');
        if (s.trim().isEmpty()) return null;

        // 1) Ismert forma név szerint. A kimondott körszám felülírja az alapot:
        //    „tabata 6 kör” hat kört jelent, nem nyolcat.
        for (String[] p : PRESETS)
            if (s.contains(p[0])) {
                int r = numberBefore(s, "kor");
                if (r <= 0) r = numberBefore(s, "sorozat");
                int rounds = r > 0 && r <= MAX_ROUNDS ? r : Integer.parseInt(p[1]);
                return new Plan(rounds, Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            }

        // 2) „40/20”, „45-15”, „8x20/10”: a teremben ez a rövid írásmód.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:(\\d{1,2})\\s?[x×]\\s?)?(\\d{1,3})\\s?[/\\-]\\s?(\\d{1,3})"
                        + "(?:\\s?[x×]\\s?(\\d{1,2}))?")
                .matcher(s);
        if (m.find()) {
            int rounds = m.group(1) != null ? Integer.parseInt(m.group(1))
                    : m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
            int work = Integer.parseInt(m.group(2));
            int rest = Integer.parseInt(m.group(3));
            if (rounds <= 0) {
                int r = numberBefore(s, "kor");
                if (r <= 0) r = numberBefore(s, "sorozat");
                if (r <= 0) r = numberBefore(s, "szett");
                rounds = r;
            }
            Plan p = build(rounds, work, rest);
            if (p != null) return p;
        }

        // 3) Kimondott alak: „3 kör 40 mp munka 20 mp pihenő”.
        int rounds = numberBefore(s, "kor");
        if (rounds <= 0) rounds = numberBefore(s, "sorozat");
        if (rounds <= 0) rounds = numberBefore(s, "szett");
        int work = secondsBefore(s, new String[]{"munka", "aktiv", "terheles", "gyakorlat"});
        int rest = secondsBefore(s, new String[]{"piheno", "pihenes", "szunet", "lazitas"});
        // „5 kör 30 másodperc” – ha csak egy időt mondanak, az a munka.
        if (work <= 0 && rest <= 0) work = firstSeconds(s);
        return build(rounds, work, rest);
    }

    private static Plan build(int rounds, int work, int rest) {
        if (work < MIN_SEC || work > MAX_SEC) return null;
        if (rest < 0 || rest > MAX_SEC) rest = 0;
        if (rounds <= 0) rounds = 1;
        if (rounds > MAX_ROUNDS) return null;
        return new Plan(rounds, work, rest);
    }

    /**
     * A megnevezett szakasz hossza másodpercben. A szám állhat a szó előtt
     * („40 mp munka”) és utána is („munka 40 mp”) – mindkettőt így mondják.
     */
    private static int secondsBefore(String s, String[] words) {
        for (String w : words) {
            int p = s.indexOf(w);
            while (p >= 0) {
                // A tagmondathatár számít: a „munka 30 mp, pihenő 10 mp”
                // mondatban a pihenő elé eső 30 a MÁSIK szakaszé.
                int from = Math.max(0, p - 22);
                for (int i = p - 1; i >= from; i--)
                    if (s.charAt(i) == ',' || s.charAt(i) == ';') { from = i + 1; break; }
                int before = timeIn(s, from, p, true);
                if (before > 0) return before;
                int end = p + w.length();
                int to = Math.min(s.length(), end + 22);
                for (int i = end; i < to; i++)
                    if (s.charAt(i) == ',' || s.charAt(i) == ';') { to = i; break; }
                int after = timeIn(s, end, to, false);
                if (after > 0) return after;
                p = s.indexOf(w, p + 1);
            }
        }
        return 0;
    }

    /** Az első időtartam a mondatban, mértékegységgel együtt. */
    private static int firstSeconds(String s) {
        return timeIn(s, 0, s.length(), false);
    }

    /**
     * Időtartam a megadott szakaszban: „40 mp”, „30 masodperc”, „1 perc”,
     * „1,5 perc”. Mértékegység nélküli szám nem idő – abból lett volna a
     * „3 kör” háromszor három másodperc.
     *
     * @param last a szó ELŐTTI szakaszban az UTOLSÓ időt keressük: a
     *             „40 mp munka 20 mp pihenő” mondatban a pihenőhöz a 20
     *             tartozik, nem a szakasz elején álló 40.
     */
    private static int timeIn(String s, int from, int to, boolean last) {
        if (from < 0 || to > s.length() || from >= to) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,4}(?:[.,]\\d{1,2})?)\\s?(masodperc|mperc|mp\\b|sec\\b|s\\b|perc|min\\b)")
                .matcher(s.substring(from, to));
        int found = 0;
        while (m.find()) {
            double v;
            try {
                v = Double.parseDouble(m.group(1).replace(',', '.'));
            } catch (NumberFormatException e) {
                continue;
            }
            String u = m.group(2);
            // A „perc” hosszabb, mint az „mp”: a sorrend a regexben számít.
            int sec = (int) Math.round(u.startsWith("perc") || u.startsWith("min")
                    ? v * 60 : v);
            if (sec >= 1 && sec <= MAX_SEC) {
                if (!last) return sec;
                found = sec;
            }
        }
        return found;
    }

    /** A megadott szó ELŐTT álló szám („3 kör”), vagy 0. */
    private static int numberBefore(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            int e = p;
            while (e > 0 && s.charAt(e - 1) == ' ') e--;
            int b = e;
            while (b > 0 && Character.isDigit(s.charAt(b - 1))) b--;
            if (b < e) {
                try {
                    int v = Integer.parseInt(s.substring(b, e));
                    if (v >= 1 && v <= 999) return v;
                } catch (NumberFormatException ignored) {
                }
            }
            p = s.indexOf(word, p + 1);
        }
        return 0;
    }
}
