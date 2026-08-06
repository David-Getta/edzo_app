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
        /** Bemelegítés másodpercben (0 = nincs). */
        public final int warm;
        /** Levezetés másodpercben (0 = nincs). */
        public final int cool;

        Plan(int rounds, int work, int rest) {
            this(rounds, work, rest, 0, 0);
        }

        Plan(int rounds, int work, int rest, int warm, int cool) {
            this.rounds = rounds; this.work = work; this.rest = rest;
            this.warm = warm; this.cool = cool;
        }

        /** „8 kör · 20 mp munka · 10 mp pihenő”. */
        public String label() {
            String s = rounds + " kör  ·  " + sec(work) + " munka";
            if (rest > 0) s += "  ·  " + sec(rest) + " pihenő";
            if (warm > 0) s += "  ·  " + sec(warm) + " bemelegítés";
            if (cool > 0) s += "  ·  " + sec(cool) + " levezetés";
            return s;
        }

        /** A teljes edzés hossza másodpercben (az utolsó pihenő is beleszámít). */
        public int totalSec() {
            return warm + rounds * (work + rest) + cool;
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
        String s = digits(Foods.norm(text).replace('\n', ' '));
        if (s.trim().isEmpty()) return null;
        // Az edzőtermi tábla írásmódja: „1:30 munka 0:30 pihenő”. A perc:mp
        // alakot rögtön másodpercre váltjuk, hogy a többi szabály értse.
        s = clockToSeconds(s);
        // A zárójel csak tagolás: a „10x(40s/20s)" ugyanaz, mint a „10x40/20".
        s = s.replace('(', ' ').replace(')', ' ');

        // 0) AMRAP: „amrap 20 perc” – annyi kör, amennyi belefér. Az időzítőnek
        //    ez EGY hosszú szakasz, nem több rövid; körszámot adni neki éppen
        //    azt venné el, amiről az AMRAP szól.
        if (s.contains("amrap")) {
            int min = numberAfter(s, "amrap");
            // „20 perc amrap”: a szám a név ELŐTT, mértékegységgel.
            if (min <= 0) {
                int sec = firstSeconds(s);
                if (sec >= 60) min = sec / 60;
            }
            if (min >= 1 && min <= 60)
                return new Plan(1, min * 60, 0, warmIn(s), coolIn(s));
        }

        // 1) Ismert forma név szerint. A kimondott körszám felülírja az alapot:
        //    „tabata 6 kör” hat kört jelent, nem nyolcat.
        for (String[] p : PRESETS)
            if (s.contains(p[0])) {
                int r = numberBefore(s, "kor");
                if (r <= 0) r = numberBefore(s, "sorozat");
                if (r <= 0) r = numberBefore(s, "round");
                // Az EMOM percenként egy kör: az „emom 12" tizenkét kör, az
                // „emom 20 perc" húsz. A szám a név UTÁN áll, nem előtte.
                if (r <= 0 && p[0].equals("emom")) r = numberAfter(s, "emom");
                int rounds = r > 0 && r <= MAX_ROUNDS ? r : Integer.parseInt(p[1]);
                return new Plan(rounds, Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                        warmIn(s), coolIn(s));
            }

        // 2) „6 x 3 perc”: körszám × egy szakasz hossza, mértékegységgel.
        java.util.regex.Matcher mm = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s?[x×]\\s?(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?"
                        + "(masodperc|mperc|mp\\b|perc)")
                .matcher(s);
        if (mm.find()) {
            try {
                int r = Integer.parseInt(mm.group(1));
                double v = Double.parseDouble(mm.group(2).replace(',', '.'));
                int w = (int) Math.round(mm.group(3).startsWith("perc") ? v * 60 : v);
                int rest = secondsBefore(s, REST_WORDS);
                // „5x(3 perc / 1 perc)”: a perjel utáni idő a pihenő. Enélkül
                // a pihenő némán elveszett, és a kör fele lett az edzésnek.
                if (rest <= 0) rest = secondsAfterSlash(s, mm.end());
                Plan p = build(r, w, rest, warmIn(s), coolIn(s));
                if (p != null) return p;
            } catch (NumberFormatException ignored) {
            }
        }

        // 3) „40/20”, „45-15”, „8x20/10”: a teremben ez a rövid írásmód. A
        //    kötőjeles alakot csak akkor fogadjuk el, ha nem sorozat: a
        //    „piramis 20-30-40 mp” nem munka/pihenő pár.
        String slashable = s.matches(".*\\d\\s?-\\s?\\d{1,3}\\s?-\\s?\\d.*")
                ? s.replace('-', ' ') : s;
        java.util.regex.Matcher m = java.util.regex.Pattern
                // A mértékegység kiírva is állhat a pár két oldalán:
                // „40 mp / 20 mp”. Enélkül a pihenő némán elveszett.
                .compile("(?:(\\d{1,2})\\s?[x×]\\s?)?(\\d{1,3})\\s?(?:masodperc|mperc|mp|sec|s)?"
                        + "\\s?[/\\-]\\s?(\\d{1,3})\\s?(?:masodperc|mperc|mp|sec|s)?"
                        + "(?:\\s?[x×]\\s?(\\d{1,2}))?")
                .matcher(slashable);
        if (m.find()) {
            int rounds = m.group(1) != null ? Integer.parseInt(m.group(1))
                    : m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
            int work = Integer.parseInt(m.group(2));
            int rest = Integer.parseInt(m.group(3));
            if (rounds <= 0) {
                int r = numberBefore(s, "kor");
                if (r <= 0) r = numberBefore(s, "sorozat");
                if (r <= 0) r = numberBefore(s, "szett");
                if (r <= 0) r = numberBefore(s, "round");
                rounds = r;
            }
            // „20 perc alatt 40/20”: a körszám a teljes időből jön ki. A
            // teremben gyakran így mondják, és fejben osztani edzés előtt a
            // legrosszabb pillanat.
            if (rounds <= 0) rounds = roundsFromTotal(s, work + rest);
            Plan p = build(rounds, work, rest, warmIn(s), coolIn(s));
            if (p != null) return p;
        }

        // 4) Kimondott alak: „3 kör 40 mp munka 20 mp pihenő”.
        int rounds = numberBefore(s, "kor");
        if (rounds <= 0) rounds = numberBefore(s, "sorozat");
        if (rounds <= 0) rounds = numberBefore(s, "szett");
        if (rounds <= 0) rounds = numberBefore(s, "round");
        // Az időzítőnél az „ismétlés" a kört jelenti, nem a súlyzós
        // ismétlésszámot: a „3 perc munka 1 perc pihenő, 6 ismétlés" hat kör.
        if (rounds <= 0) rounds = numberBefore(s, "ismetles");
        if (rounds <= 0) rounds = numberAfterColon(s, "kor");
        if (rounds <= 0) rounds = numberAfterColon(s, "round");
        if (rounds <= 0) rounds = roundsPrefix(s);
        // Az „on" NEM szerepel: szó belsejében is előfordul („huszonöt"),
        // és a secondsBefore nem néz szóhatárt. Az „off" mellett a munkaidő
        // úgyis az első kimondott időből jön.
        int work = secondsBefore(s, WORK_WORDS, REST_WORDS);
        int rest = secondsBefore(s, REST_WORDS, WORK_WORDS);
        if (rest <= 0) rest = bareSecondsBefore(s, REST_WORDS, spokenUnit(s));
        // „5 kör 30 másodperc” – ha csak egy időt mondanak, az a munka. De
        // megnevezetlenül csak életszerű munkaidőt fogadunk el: a „minden
        // percben 1 kör, 15 percig” 15 perce nem egyetlen szakasz hossza.
        if (work <= 0) {
            // Ha csak a pihenőt nevezték meg („egy perc plank, 30 mp pihenő”),
            // a munkaidő az első kimondott idő.
            work = firstSeconds(s);
            if (rest <= 0 && work > 600) return null;
        }
        return build(rounds, work, rest, warmIn(s), coolIn(s));
    }

    /**
     * Körszám a kimondott teljes időből: „20 perc alatt 40/20”. Csak
     * félreérthetetlen jelzőszóval („alatt”, „összesen”, „percig”), különben
     * a „3 kör 40 mp” munkaideje válna teljes idővé.
     */
    private static int roundsFromTotal(String s, int cycle) {
        if (cycle <= 0) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,3})\\s?perc(?:ig)?(?:\\s?(?:alatt|osszesen))?")
                .matcher(s);
        while (m.find()) {
            boolean marked = m.group(0).contains("alatt") || m.group(0).contains("osszesen")
                    || m.group(0).contains("percig");
            if (!marked) continue;
            int total;
            try {
                total = Integer.parseInt(m.group(1)) * 60;
            } catch (NumberFormatException e) {
                continue;
            }
            int r = total / cycle;
            if (r >= 2 && r <= MAX_ROUNDS) return r;
        }
        return 0;
    }

    /** Bemelegítés a mondatból („2 perc bemelegítés”). */
    static int warmIn(String s) {
        return secondsBefore(Foods.norm(s), new String[]{"bemelegit", "warmup", "warm up"});
    }

    /** Levezetés a mondatból („levezetés 3 perc”). */
    static int coolIn(String s) {
        return secondsBefore(Foods.norm(s), new String[]{"levezet", "nyujtas", "cooldown",
                "cool down"});
    }

    /** „1:30” → „90 mp”. Csak érvényes perc:másodperc alak, 0–59 másodperccel. */
    static String clockToSeconds(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d:])(\\d{1,2}):([0-5]\\d)(?![\\d:])").matcher(s);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            int sec = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
            m.appendReplacement(b, sec + " mp");
        }
        m.appendTail(b);
        return b.toString();
    }

    private static Plan build(int rounds, int work, int rest) {
        return build(rounds, work, rest, 0, 0);
    }

    private static Plan build(int rounds, int work, int rest, int warm, int cool) {
        if (work < MIN_SEC || work > MAX_SEC) return null;
        if (rest < 0 || rest > MAX_SEC) rest = 0;
        if (warm < 0 || warm > MAX_SEC) warm = 0;
        if (cool < 0 || cool > MAX_SEC) cool = 0;
        if (rounds <= 0) rounds = 1;
        if (rounds > MAX_ROUNDS) return null;
        return new Plan(rounds, work, rest, warm, cool);
    }

    /**
     * A megnevezett szakasz hossza másodpercben. A szám állhat a szó előtt
     * („40 mp munka”) és utána is („munka 40 mp”) – mindkettőt így mondják.
     */
    private static int secondsBefore(String s, String[] words) {
        return secondsBefore(s, words, new String[0]);
    }

    /**
     * Ugyanaz, de a MÁSIK szakasz neve is határ.
     *
     * A vessző eddig egyedül jelölte a szakaszhatárt, márpedig sokan nem
     * tesznek vesszőt: a „45 másodperc munka 15 pihenő” mondatban a pihenő elé
     * eső egyetlen kimondott idő a negyvenöt volt – vagyis a pihenő is
     * negyvenöt másodperc lett. A terv létrejött, csak háromszor hosszabb
     * szünettel, mint amit az ember kért.
     */
    private static int secondsBefore(String s, String[] words, String[] stops) {
        for (String w : words) {
            int p = s.indexOf(w);
            while (p >= 0) {
                // A tagmondathatár számít: a „munka 30 mp, pihenő 10 mp”
                // mondatban a pihenő elé eső 30 a MÁSIK szakaszé.
                int from = Math.max(0, p - 22);
                for (int i = p - 1; i >= from; i--)
                    if (isBreak(s, i)) { from = i + 1; break; }
                for (String st : stops) {
                    int q = s.lastIndexOf(st, p - 1);
                    if (q >= from && q + st.length() <= p) from = q + st.length();
                }
                int before = timeIn(s, from, p, true);
                if (before > 0) return before;
                int end = p + w.length();
                int to = Math.min(s.length(), end + 22);
                for (int i = end; i < to; i++)
                    if (isBreak(s, i)) { to = i; break; }
                for (String st : stops) {
                    int q = s.indexOf(st, end);
                    if (q >= 0 && q < to) to = q;
                }
                int after = timeIn(s, end, to, false);
                if (after > 0) return after;
                p = s.indexOf(w, p + 1);
            }
        }
        return 0;
    }

    /**
     * Mértékegység nélküli szám a szakasz-szó előtt: „45 mp munka 15 pihenő”.
     *
     * A mértékegységet a mondatban ELSŐKÉNT kimondott időtől örökli, mert így
     * beszél az ember: ha az első szám másodperc, a többi is az. Enélkül a
     * szakasz üresen maradt, és a terv szünet nélkül indult.
     */
    private static int bareSecondsBefore(String s, String[] words, int unitSec) {
        if (unitSec <= 0) return 0;
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile("(\\d{1,3}) ?$");
        for (String w : words) {
            int p = s.indexOf(w);
            while (p >= 0) {
                java.util.regex.Matcher m =
                        pat.matcher(s.substring(Math.max(0, p - 5), p));
                if (m.find()) {
                    int v = Integer.parseInt(m.group(1)) * unitSec;
                    if (v >= MIN_SEC && v <= MAX_SEC) return v;
                }
                p = s.indexOf(w, p + 1);
            }
        }
        return 0;
    }

    /**
     * A mondatban elsőként kimondott idő MÉRTÉKEGYSÉGE másodpercben: 60, ha
     * percben beszél, 1, ha másodpercben, 0, ha egyáltalán nem mondott
     * mértékegységet.
     */
    private static int spokenUnit(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\\d{1,4}(?:[.,]\\d{1,2})? ?(masodperc|mperc|mp\\b|sec\\b|s\\b|perc|min\\b)")
                .matcher(s);
        if (!m.find()) return 0;
        String u = m.group(1);
        return u.startsWith("perc") || u.startsWith("min") ? 60 : 1;
    }

    /**
     * Tagmondathatár-e az adott karakter? A tizedesvessző nem az: a
     * „0,5 perc munka” fél perce különben öt percre ugrott volna.
     */
    private static boolean isBreak(String s, int i) {
        char c = s.charAt(i);
        if (c != ',' && c != ';') return false;
        return !(i > 0 && Character.isDigit(s.charAt(i - 1))
                && i + 1 < s.length() && Character.isDigit(s.charAt(i + 1)));
    }

    /** Az első időtartam a mondatban, mértékegységgel együtt. */
    private static int firstSeconds(String s) {
        // A bemelegítés és a levezetés tagmondata kimarad: a „bemelegítés 5
        // perc, 10 kör 1/1, levezetés 5 perc" munkaideje nem öt perc – eddig
        // viszont pont az lett, vagyis a kör ugyanolyan hosszú volt, mint a
        // bemelegítés.
        int from = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i < s.length() && s.charAt(i) != ',' && s.charAt(i) != ';') continue;
            String part = s.substring(from, i);
            from = i + 1;
            boolean edge = false;
            for (String w : new String[]{"bemelegit", "warmup", "warm up", "levezet",
                    "nyujtas", "cooldown", "cool down"})
                if (part.contains(w)) { edge = true; break; }
            if (edge) continue;
            int v = timeIn(part, 0, part.length(), false);
            if (v > 0) return v;
        }
        return 0;
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

    /**
     * Kiírt számok és a „-szor/-szer” alak számjegyre váltása. A
     * szorzószám itt körszámot jelent: „20/10 nyolcszor” nyolc kör.
     */
    static String digits(String s) {
        // A kötőjel is odatartozik: a „10-szer" ugyanazt jelenti, mint a
        // „tízszer". Nélküle a terv egyszer futott le tíz helyett – a szám
        // ott volt a mondatban, csak nem jutott el a körszámig.
        return Hu.digits(s).replaceAll("(\\d+)\\s?-?\\s?(szor|szer)\\b", "$1 kor");
    }

    /** Az első szám a szó után: „emom 12” → 12. */
    private static int numberAfter(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            int b = p + word.length();
            while (b < s.length() && s.charAt(b) == ' ') b++;
            int e = b;
            while (e < s.length() && Character.isDigit(s.charAt(e))) e++;
            if (e > b) {
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

    /**
     * A pihenőt jelölő szavak, EGY helyen.
     *
     * Korábban két listában éltek, és a rövidebbikből hiányzott a „séta": a
     * „8 kör: 20 mp sprint, 40 mp séta" pihenője így elveszett, az edzés
     * pedig szünet nélküli lett.
     */
    private static final String[] REST_WORDS = {"piheno", "pihenes", "szunet",
            "lazitas", "seta", "lassu", "rest", "off"};

    /** A munkaszakaszt jelölő szavak – a pihenőnek ez a határa, és fordítva. */
    private static final String[] WORK_WORDS = {"munka", "aktiv", "terheles",
            "gyakorlat", "work"};

    /** Perjellel elválasztott második idő: „3 perc / 1 perc” → 60. */
    private static int secondsAfterSlash(String s, int from) {
        if (from < 0 || from >= s.length()) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^\\s?[/-]\\s?(\\d{1,3})\\s?(masodperc|mperc|mp|perc)?")
                .matcher(s.substring(from));
        if (!m.find()) return 0;
        int v = Integer.parseInt(m.group(1));
        String unit = m.group(2);
        int sec = unit != null && unit.startsWith("perc") ? v * 60 : v;
        return sec >= MIN_SEC && sec <= MAX_SEC ? sec : 0;
    }

    /**
     * Szám a szó után, de CSAK kettősponttal: „kör: 6”.
     *
     * A táblára írt terv gyakran mezőkből áll („kör: 6, munka: 40mp”), és ott a
     * körszám a szó mögött van. Kettőspont nélkül nem találgatunk: a puszta
     * „kör 40 mp munka” negyvene munkaidő, nem negyven kör.
     */
    private static int numberAfterColon(String s, String word) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(word + "\\w*\\s?:\\s?(\\d{1,3})").matcher(s);
        if (m.find()) {
            try {
                int v = Integer.parseInt(m.group(1));
                if (v >= 1 && v <= 999) return v;
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    /**
     * Vezető szorzó körszámként: „10x30s on 30s off” tíz kör.
     *
     * A perjeles alaknál („8x20/10”) ez már megvolt, a kiírtnál nem – ott a
     * tíz kör csendben egyre olvadt, és az edzés a tizedénél véget ért.
     */
    private static int roundsPrefix(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d.,])(\\d{1,2})\\s?[x×]\\s?(?=\\d)").matcher(s);
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v >= 2 && v <= MAX_ROUNDS) return v;
        }
        // Záró szorzó: „30 mp on 30 mp off 10x”.
        m = java.util.regex.Pattern.compile("(?<![\\d.,])(\\d{1,2})\\s?[x×]\\s*$")
                .matcher(s.trim());
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v >= 2 && v <= MAX_ROUNDS) return v;
        }
        return 0;
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
