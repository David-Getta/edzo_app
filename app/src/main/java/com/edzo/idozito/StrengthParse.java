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
            // Tartásnál a szám másodperc – enélkül a „3×60" ismétlésnek látszik.
            if (isTimed(name)) sb.append(" mp");
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
            {"Fekvenyomás", "fekvenyom", "fekve nyom", "bench", "mellet nyom"},
            // A jelzős változatok KÜLÖN gyakorlatok, nem a bázis becézései: a
            // román felhúzás jóval könnyebb súllyal megy, mint a holtemelés, a
            // bolgár kitörés pedig egy lábra. Egy vödörbe téve a
            // progresszió-javaslat a nehezebbik súlyát kínálná a könnyebbik
            // gyakorlathoz, a rekord meg sosem dőlne meg a könnyebbikkel.
            {"Román felhúzás", "roman felhuzas", "roman holtemel", "roman huzas", "rdl"},
            {"Bolgár kitörés", "bolgar kitores", "bolgar guggolas", "bolgar split",
                    "bolgar szplit"},
            {"Ferde fekvenyomás", "ferde fekvenyom", "ferde pad", "ferde nyomas",
                    "incline"},
            {"Felhúzás", "felhuzas", "holtemel", "deadlift"},
            {"Húzódzkodás", "huzodzkod", "pull up", "pullup", "huzodzk", "chin up", "chinup",
                    "allhuzodzkodas", "all fole huzas"},
            {"Vállból nyomás", "vallbol nyom", "vallnyom", "vallbol", "ohp", "mellrol nyom",
                    "nyak moge nyom", "katonai nyomas", "military press"},
            {"Evezés", "evezes", "evezo", "rowing", "evezt", "evezni", "evezek"},
            {"Bicepsz", "bicepsz", "kalapacs", "predikator", "scott pad"},
            {"Tricepsz", "tricepsz", "francia nyom"},
            {"Kitörés", "kitores", "lunge", "kitort"},
            {"Lábtolás", "labtolas", "leg press", "legpress"},
            {"Vádliemelés", "vadliemel", "vadli"},
            {"Fekvőtámasz", "fekvotamasz", "push up", "pushup"},
            {"Tolódzkodás", "tolodzkod", "dipp", "dips"},
            {"Lehúzás", "lehuzas", "latpull", "lat pull", "lat huzas"},
            {"Oldalemelés", "oldalemel", "eloreemel"},
            {"Plank", "plank", "deszka", "oldaltamasz", "alkartamasz"},
            {"Felülés", "felules", "crunch", "felult"},
            {"Hasprés", "haspres", "hasizom", "hasgep"},
            {"Lábemelés", "labemel"},
            {"Combhajlítás", "labhajlit", "combhajlit"},
            {"Lábnyújtás", "labnyujt", "combfeszit", "labgep"},
            {"Csípőemelés", "csipoemel", "hipthrust", "hip thrust", "medencelok",
                    "farizom"},
            {"Arnold nyomás", "arnold"},
            {"Fordított tárogatás", "forditott tarogat", "hatso vall", "hatso deltoid",
                    "face pull", "facepull"},
            {"Csuklyás emelés", "csuklyas", "shrug"},
            {"Hátizom gép", "hatizom", "hatgep"},
            {"Mellgép", "mellgep", "tarogat", "pillango", "mellnyom"},
            {"Hegymászó", "hegymaszo"},
            {"Hátfeszítés", "hiperextenzi", "hatfeszit", "back extension"},
            // A „kettlebell" magában nem elég: a kettlebell-guggolás guggolás.
            {"Kettlebell lendítés", "kettlebell swing", "kettlebell lendit", "kb swing",
                    "swing"},
            {"Lábtávolítás", "labtavolit", "combtavolit", "abduktor"},
            {"Lábközelítés", "labkozelit", "combkozelit", "adduktor"},
            {"Fellépés", "fellepes", "step up", "stepup"},
            // Csak a teljes szó: az „alkartámasz" plank, nem alkarhajlítás.
            {"Alkarhajlítás", "alkarhajlit", "csuklohajlit"},
            {"Orosz csavarás", "orosz csav", "oroszcsav", "russian twist"},
            // A név a beépített programokét követi („Fal-ülés"), hogy a
            // mondatból és a programból felvett gyakorlat egy néven éljen.
            {"Fal-ülés", "fal ules", "fal-ules", "falules", "wall sit", "wallsit"},
            {"Holt függés", "holt fugges", "holtfugges", "dead hang", "deadhang",
                    "holtakasztas", "holt akasztas"},
            {"Szuperman", "szuperman", "superman"},
    };

    /**
     * Tartások: itt az „ismétlés” valójában MÁSODPERC.
     *
     * A plank sosem ismétlés – aki beírja, hogy 3 × 60, az három egyperces
     * tartásra gondol. A napló eddig mindenhol ismétlésként kezelte: „0 kg ×
     * 60”-at írt ki, a progresszió pedig egy ismétlést („61 másodpercet”)
     * javasolt, és húsz fölött már azt mondta, hogy ennyi ismétlésnél az
     * állóképesség fejlődik. Egy perc plank után ez értelmetlen tanács.
     *
     * Csak az egyértelműen tartásos mozdulatok szerepelnek itt. A „superman”
     * például kimaradt: azt sokan ismétlésre csinálják, és egy rossz besorolás
     * itt csendben rossz javaslatot adna.
     */
    private static final String[] TIMED = {
            "plank", "deszka", "oldaltamasz", "alkartamasz",
            "falules", "fal ules", "fal-ules", "wallsit", "wall sit",
            "holtfugges", "holt fugges", "deadhang", "dead hang", "holtakasztas",
            "hollow", "izometri", "statikus", "vakuum",
    };

    /**
     * Tartásos gyakorlat-e a név? A napló bármilyen saját nevet elfogad, ezért
     * a szép neveken túl a szótöveket is nézzük.
     */
    public static boolean isTimed(String name) {
        if (name == null) return false;
        String q = Foods.norm(name);
        for (String t : TIMED) if (q.contains(t)) return true;
        return false;
    }

    /** „mp” tartásnál, „ismétlés” minden másnál – kiíráshoz. */
    public static String unit(String name) {
        return isTimed(name) ? "mp" : "ismétlés";
    }

    /** Tartás hossza emberi alakban: „45 mp”, „1:30”. */
    public static String hold(int sec) {
        if (sec < 60) return sec + " mp";
        int s = sec % 60;
        return (sec / 60) + ":" + (s < 10 ? "0" : "") + s;
    }

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
        String whole = stripInsteadOf(sets(Foods.norm(text)));
        // Gyakorlatnév sorozat nélkül, a sorozat meg egy tagmondattal odébb:
        // „guggolás 60 kg bemelegítés, aztán 3x5 100". Az első tagmondatban
        // nincs ismétlésszám, a másodikban nincs név – eddig az EGÉSZ mondat
        // elveszett, pedig együtt teljesen egyértelmű.
        String pending = null;
        for (String part : splitClauses(whole)) {
            Item it = parseOne(part);
            // A név ékezetes, szép alak; a tagmondat viszont már normalizált,
            // ezért a nevet is úgy adjuk hozzá.
            if (it == null && pending != null) it = parseOne(Foods.norm(pending) + " " + part);
            if (it != null) { out.add(it); pending = null; continue; }
            if (out.isEmpty() && pending == null) pending = moveIn(part);
            // Sorozatfelsorolás gyakorlatnév nélkül: „fekvenyomás 60x10, 70x8,
            // 80x6”. A vessző itt nem új gyakorlatot nyit, hanem a következő
            // sorozatot – név hiányában az előzőhöz tartozik.
            if (!out.isEmpty()) {
                List<Set> more = continuationSets(part, out.get(out.size() - 1));
                if (more != null) out.get(out.size() - 1).sets.addAll(more);
            }
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

    /**
     * Egy folytatás-tagmondat sorozatai, vagy null.
     *
     * Szándékosan szűk a minta: CSAK a puszta sorozatjelölés számít
     * folytatásnak („70x8", „2x8 70 kg"). Bármi más szó a tagmondatban azt
     * jelenti, hogy nem sorozatról van szó – a „guggolás 3x10, majd 20 perc
     * futás" húsz perce nem húsz ismétlés.
     */
    private static List<Set> continuationSets(String s, Item prev) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "^(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?[x×]\\s?(\\d{1,3})"
                        + "(?:\\s?(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?(?:kg|kilo)?)?$")
                .matcher(trimPunct(s));
        if (!m.matches()) return null;
        double a;
        try { a = Double.parseDouble(m.group(1).replace(',', '.')); }
        catch (NumberFormatException e) { return null; }
        int reps = Integer.parseInt(m.group(2));
        if (reps < 1 || reps > 200) return null;
        List<Set> out = new ArrayList<>();
        if (m.group(3) != null) {
            // „2x8 70 kg”: sorozat × ismétlés, kiírt súllyal.
            double w;
            try { w = Double.parseDouble(m.group(3).replace(',', '.')); }
            catch (NumberFormatException e) { return null; }
            int n = (int) a;
            if (a != n || n < 1 || n > 20 || w <= 0 || w > 500) return null;
            for (int i = 0; i < n; i++) out.add(new Set(reps, w));
            return out;
        }
        if (a > 20 && a <= 500) {
            // „70x8”: súly × ismétlés.
            out.add(new Set(reps, a));
            return out;
        }
        int n = (int) a;
        if (a != n || n < 1 || n > 20) return null;
        // „3x8” súly nélkül: az előző sorozat súlyával megy tovább.
        double w = prev.sets.isEmpty() ? 0 : prev.sets.get(prev.sets.size() - 1).weight;
        for (int i = 0; i < n; i++) out.add(new Set(reps, w));
        return out;
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
            if (p.isEmpty()) continue;
            // Vesszővel tagolt felsorolás: „guggoltam, 5 sorozat, 5 ismétlés,
            // 100 kg”. A darabok külön-külön értelmetlenek – a sorozatszámhoz
            // nincs ismétlés, az ismétléshez nincs gyakorlat –, ezért eddig az
            // EGÉSZ mondatból nem lett bejegyzés. Együtt viszont teljes.
            // Csak akkor folytatás, ha NINCS benne saját gyakorlatnév: a
            // „60 kg guggolás 3x8, 50 kg fekvenyomás 3x8" második fele önálló
            // gyakorlat, nem az előző adata – összeolvasztva mindkettő elveszett.
            if (!out.isEmpty() && moveIn(p) == null && p.matches(
                    "^\\d{1,3}([.,]\\d{1,2})?\\s?(sorozat|szett|set|ismetles|ism|kg|kilo)\\b.*")) {
                out.set(out.size() - 1, out.get(out.size() - 1) + " " + p);
                continue;
            }
            out.addAll(splitByMoves(p));
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
                .replaceAll("(\\d{1,2})\\s?(?:szor|szer)\\s+(\\d{1,3})", "$1x$2")
                // „3 kör 10 fekvőtámasz”: a kör itt sorozatot jelent. A szám a
                // két oldalon köti a mintát, így a „korcsolya" nem kör.
                .replaceAll("(\\d{1,2})\\s?kor\\s+(\\d{1,3})", "$1x$2");
    }

    /**
     * A tagmondat végéről a mondatzáró jelek le: a „80x6 :)" és a „80x6."
     * ugyanaz a sorozat. A minta a tagmondat VÉGÉHEZ van kötve, tehát egy
     * hangulatjel eddig elvitte az utolsó sorozatot.
     */
    private static String trimPunct(String s) {
        int e = s.length();
        while (e > 0 && !Character.isLetterOrDigit(s.charAt(e - 1))) e--;
        int b = 0;
        while (b < e && s.charAt(b) == ' ') b++;
        return s.substring(b, e);
    }

    /**
     * „Guggolás 3x10 HELYETT fekvenyomás 3x8": ami a helyett ELŐTT áll, az
     * nem történt meg. Enélkül mindkét gyakorlat bekerült a naplóba – az is,
     * amit az ember épp kihagyott.
     */
    private static String stripInsteadOf(String s) {
        int h = s.indexOf("helyett");
        while (h >= 0) {
            int a = h;
            while (a > 0 && s.charAt(a - 1) != ',' && s.charAt(a - 1) != ';'
                    && s.charAt(a - 1) != '.') a--;
            char[] c = s.toCharArray();
            for (int i = a; i < h + 7 && i < c.length; i++) c[i] = ' ';
            s = new String(c);
            h = s.indexOf("helyett", h + 1);
        }
        return s;
    }

    /**
     * Percből másodperc a tartásos mondatokban: „1 perc” → „60 mp”.
     *
     * A mértékegység szándékosan bennmarad: a puszta szám ismétlésnek
     * látszana, és a „3 sorozat 1 perc” ismétlés nélkül maradna – vagyis
     * elveszne az egész bejegyzés.
     */
    private static String holdSeconds(String s) {
        s = s.replaceAll("(?<![a-z0-9])fel ?perc", "30 mp");
        // A törtrész is számít: a „másfél perc" itt már „1,5 perc", és
        // enélkül az ötös maradt volna belőle – öt perc plank.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d,.])(\\d{1,3})(?:[.,](\\d))? ?perc").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int v = Integer.parseInt(m.group(1));
            int sec = v * 60 + (m.group(2) == null ? 0 : Integer.parseInt(m.group(2)) * 6);
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                    v > 0 && v <= 10 ? sec + " mp" : m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Egy tagmondat → gyakorlat + sorozatok, vagy null. */
    private static Item parseOne(String s) {
        String name = moveIn(s);
        if (name == null) return null;

        // Tartásnál a perc percet jelent, és a szám másodperc: a „plank 3×1
        // perc" három egyperces tartás. Átváltás nélkül a bejegyzés csendben
        // létrejön – csak hatvanszor rövidebben.
        boolean timed = isTimed(name);
        if (timed) s = holdSeconds(s);
        // Egy négyperces fal ülés hihető; négyszáz ismétlés nem. A korlát
        // ezért a mértékegységhez igazodik.
        int maxRep = timed ? 600 : 200;

        double weight = weightIn(s);
        List<Set> sets = new ArrayList<>();

        // 1) „3x10”, „3 x 10”, „3×10”: sorozat × ismétlés. A teremben szokásos
        //    „3x10x60” harmadik tagja maga a súly.
        java.util.regex.Matcher m = java.util.regex.Pattern
                // Az első tag háromjegyű is lehet: a „100x3" súlya száz kiló.
                .compile("(\\d{1,3})\\s?[x×]\\s?(\\d{1,3})(?:\\s?[x×]\\s?(\\d{1,3}(?:[.,]\\d{1,2})?))?"
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
            if (n >= 1 && n <= 20 && r >= 1 && r <= maxRep)
                for (int i = 0; i < n; i++) sets.add(new Set(r, weight));
            // „60x10”: sorozatból nem lehet hatvan, súlyból viszont igen. Ez az
            // erőemelők szokásos jelölése – súly × ismétlés.
            else if (!timed && n > 20 && n <= 500 && r >= 1 && r <= 200) {
                sets.add(new Set(r, weight > 0 ? weight : n));
                // Piramis vesszők nélkül: „fekvenyomás 60x10 70x8 80x6”. A
                // vesszős alakot már értettük, a szóközöset nem – abból egyetlen
                // sorozat lett, a másik kettő némán elveszett. Csak a súly ×
                // ismétlés alakot folytatjuk: a „3x10” hármasa sorozatszám,
                // annak a szóköz nem elválasztója.
                while (m.find()) {
                    if (m.group(3) != null) break;
                    int wn = Integer.parseInt(m.group(1));
                    int wr = Integer.parseInt(m.group(2));
                    if (wn <= 20 || wn > 500 || wr < 1 || wr > 200) break;
                    sets.add(new Set(wr, wn));
                }
            }
        }
        // 2) Sorozatonként más ismétlés: „12-10-8”, „5/5/5”.
        //
        // A per-jel ugyanolyan gyakori elválasztó, mint a kötőjel, és nem
        // ütközik semmivel a súlyzós mondatban. A VESSZŐ szándékosan nem
        // szerepel: a „10,8" tizedes szám is lehet („60,5 kg"), és egy
        // félreolvasott súly rosszabb, mint egy fel nem ismert sorozatlista.
        if (sets.isEmpty()) {
            m = java.util.regex.Pattern
                    .compile("(\\d{1,3})[-/](\\d{1,3})(?:[-/](\\d{1,3}))?"
                            + "(?:[-/](\\d{1,3}))?(?:[-/](\\d{1,3}))?")
                    .matcher(s);
            if (m.find()) {
                List<Set> tmp = new ArrayList<>();
                boolean ok = true;
                for (int g = 1; g <= m.groupCount(); g++) {
                    if (m.group(g) == null) continue;
                    int r = Integer.parseInt(m.group(g));
                    if (r < 1 || r > maxRep) { ok = false; break; }
                    tmp.add(new Set(r, weight));
                }
                if (ok && tmp.size() >= 2) sets.addAll(tmp);
            }
        }
        // 2b) Vesszővel felsorolt ismétlések: „guggolás 5,5,5”.
        //
        // Vesszőt CSAK három számtól fölfelé fogadunk el: egy tizedes számban
        // pontosan egy vessző van, tehát a „60,5” sosem téveszthető össze
        // ezzel. Enélkül az egész felsorolás kiesett, és ami maradt – például
        // a súly a „5,5,5 @ 100”-ból – ismétlésszámnak látszott.
        if (sets.isEmpty()) {
            m = java.util.regex.Pattern
                    .compile("(\\d{1,3}),(\\d{1,3}),(\\d{1,3})(?:,(\\d{1,3}))?(?:,(\\d{1,3}))?")
                    .matcher(s);
            if (m.find()) {
                List<Set> tmp = new ArrayList<>();
                boolean ok = true;
                for (int g = 1; g <= m.groupCount(); g++) {
                    if (m.group(g) == null) continue;
                    int r = Integer.parseInt(m.group(g));
                    if (r < 1 || r > maxRep) { ok = false; break; }
                    tmp.add(new Set(r, weight));
                }
                if (ok) sets.addAll(tmp);
            }
        }
        // 3) „3 sorozat 10 ismétlés” / „10 ismétlés”.
        if (sets.isEmpty()) {
            int reps = numberBefore(s, "ismetles");
            if (reps <= 0) reps = numberBefore(s, "ism");
            // Tartásnál a másodperc a „hányat", nem a súly.
            if (reps <= 0 && timed) reps = numberBefore(s, "mp");
            if (reps <= 0 && timed) reps = numberBefore(s, "masodperc");
            // A „3 kör 10 fekvőtámasz" köre is sorozat. A „kör" szóközzel, hogy
            // a „korcsolya" ne legyen kör.
            int series = 0;
            String seriesWord = null;
            for (String w : new String[]{"sorozat", "szett", "set", "kor "}) {
                series = numberBefore(s, w);
                if (series > 0) { seriesWord = w; break; }
            }
            // „4 sorozat 8 fekvenyomás”: az „ismétlés" szó kimarad – a teremben
            // senki nem mondja ki –, a szám mégis ott van a sorozatszám után.
            // Eddig ez a mondat NEM veszett el félig: egyáltalán nem lett
            // belőle bejegyzés, mert a sorozatszám ismétlés nélkül kiszállt.
            if (reps <= 0 && series > 0 && series <= 20 && seriesWord != null) {
                int after = numberAfter(s, seriesWord);
                if (after > 0 && after <= maxRep) reps = after;
            }
            if (reps > 0 && reps <= maxRep) {
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
                // A „3x max" hármasa SOROZATSZÁM: az ismétlés ismeretlen, és
                // hármat beírni helyette csendes hazugság lenne.
                if (rest.startsWith("x") || rest.startsWith("×")) continue;
                if (isWeightSuffixed(s, e)) continue;
                if (isAtWeight(s, bare.start())) continue;
                int r = Integer.parseInt(bare.group(1));
                if (r >= 1 && r <= maxRep) { sets.add(new Set(r, weight)); break; }
            }
        }
        // „fekvenyomás max 120 kg”: a legnehezebb, amit egyszer megnyomott.
        // Csak ha van súly ÉS nincs semmilyen ismétlés-adat – a „3 szett
        // maximumig” ismétlésszáma ismeretlen, abból nem találunk ki egyet.
        if (sets.isEmpty() && weight > 0 && s.matches(".*(^|[^a-z])max.*"))
            sets.add(new Set(1, weight));
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
        // A kukac az edzésnaplók nemzetközi rövidítése a súlyra: „5x5 @ 100”.
        // Mértékegység nélkül eddig ismétlésszámnak látszott, és a „5,5,5 @ 100”
        // egyetlen, száz ismétléses sorozat lett.
        m = java.util.regex.Pattern.compile("@\\s?(\\d{1,3}(?:[.,]\\d{1,2})?)").matcher(s);
        if (m.find()) {
            try {
                double w = Double.parseDouble(m.group(1).replace(',', '.'));
                if (w > 0 && w <= 500) return w;
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /** A kukac utáni szám a súly, nem ismétlés: „5x5 @ 100”. */
    private static boolean isAtWeight(String s, int start) {
        for (int i = start - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ' ') continue;
            return c == '@';
        }
        return false;
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

    /**
     * A megadott szó UTÁN álló szám („4 sorozat 8”). Csak akkor, ha a szám
     * tényleg ismétlés lehet: a mértékegységgel folytatódó számok (60 kg,
     * 2 perc pihenő) és a „3x8" szorzata nem az.
     */
    private static int numberAfter(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            int e = p + word.length();
            while (e < s.length() && Character.isLetter(s.charAt(e))) e++;   // ragozott alak
            while (e < s.length() && (s.charAt(e) == ' ' || s.charAt(e) == '-')) e++;
            int b = e;
            while (e < s.length() && Character.isDigit(s.charAt(e))) e++;
            if (b < e) {
                String rest = s.substring(e).trim();
                boolean unit = rest.startsWith("kg") || rest.startsWith("kilo")
                        || rest.startsWith("perc") || rest.startsWith("mp")
                        || rest.startsWith("masodperc") || rest.startsWith("x")
                        || rest.startsWith("×") || rest.startsWith(",");
                if (!unit) {
                    try { return Integer.parseInt(s.substring(b, e)); }
                    catch (NumberFormatException ignored) {}
                }
            }
            p = s.indexOf(word, p + 1);
        }
        return 0;
    }
}
