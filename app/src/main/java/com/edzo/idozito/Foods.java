package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Beépített élelmiszer-adatbázis (kcal és fehérje / 100 g, közelítő értékek),
 * magyar hétköznapi ételekkel. A keresés szótő-alapú, így a ragozott alakok
 * („rizzsel", „csirkemellből") is találnak.
 */
public final class Foods {

    private Foods() {}

    public static final class Food {
        public final String name;
        public final int kcal100;
        public final double prot100;
        public final int portion; // tipikus adag grammban
        final String[] stems;
        Food(String name, int kcal100, double prot100, int portion, String... stems) {
            this.name = name; this.kcal100 = kcal100; this.prot100 = prot100;
            this.portion = portion; this.stems = stems;
        }
    }

    public static final Food[] ALL = {
        new Food("Rántott hús (sertés)", 320, 22, 180, "rantott hus", "rantotthus", "bécsi", "becsi"),
        new Food("Rántott csirkemell", 250, 25, 180, "rantott csirke"),
        new Food("Csirkemell (sült/grill)", 165, 31, 150, "csirkemell", "csirke mell", "grillcsirke"),
        new Food("Csirkecomb", 210, 26, 150, "csirkecomb", "comb"),
        new Food("Pulykamell", 105, 23, 150, "pulyka"),
        new Food("Sertéskaraj", 240, 27, 150, "karaj", "sertes"),
        new Food("Marhahús", 250, 26, 150, "marha"),
        new Food("Fasírt", 290, 15, 150, "fasirt"),
        new Food("Kolbász", 350, 15, 100, "kolbasz"),
        new Food("Virsli", 250, 10, 100, "virsli"),
        new Food("Sonka", 120, 18, 50, "sonka"),
        new Food("Szalámi", 400, 22, 30, "szalami"),
        new Food("Bacon", 500, 13, 30, "bacon", "szalonna"),
        new Food("Hal (fehér)", 120, 22, 150, "hal"),
        new Food("Tonhal", 130, 24, 100, "tonhal"),
        new Food("Lazac", 210, 20, 150, "lazac"),
        new Food("Tojás", 155, 13, 110, "tojas"),
        new Food("Rántotta", 180, 12, 150, "rantotta"),
        new Food("Rizs (főtt)", 130, 2.7, 200, "riz"),
        new Food("Tészta (főtt)", 150, 5, 250, "teszta", "spagetti", "penne"),
        new Food("Burgonya (főtt)", 87, 2, 250, "burgonya", "krumpli"),
        new Food("Sült krumpli", 300, 3.5, 150, "sult krumpli", "hasabburgonya", "hasáb"),
        new Food("Burgonyapüré", 110, 2, 200, "pure", "püré"),
        new Food("Édesburgonya", 90, 1.6, 200, "edesburgonya"),
        new Food("Bulgur (főtt)", 120, 4, 200, "bulgur"),
        new Food("Quinoa (főtt)", 120, 4.4, 200, "quinoa"),
        new Food("Kenyér", 250, 8, 70, "kenyer"),
        new Food("Zsemle", 280, 9, 55, "zsemle"),
        new Food("Kifli", 290, 8, 55, "kifli"),
        new Food("Péksütemény", 350, 7, 80, "peksutemeny", "croissant", "pogacsa"),
        new Food("Zabpehely", 370, 13, 50, "zab"),
        new Food("Müzli", 380, 9, 60, "muzli", "müzli", "granola"),
        new Food("Palacsinta", 220, 6, 150, "palacsinta"),
        new Food("Pizza", 260, 11, 300, "pizza"),
        new Food("Hamburger", 280, 13, 250, "hamburger", "burger"),
        new Food("Gyros", 220, 15, 350, "gyros"),
        new Food("Lángos", 320, 7, 200, "langos"),
        new Food("Gulyásleves", 100, 7, 400, "gulyas"),
        new Food("Pörkölt", 180, 15, 300, "porkolt"),
        new Food("Főzelék", 80, 3, 350, "fozelek"),
        new Food("Leves (átlag)", 50, 3, 400, "leves"),
        new Food("Rakott krumpli", 160, 6, 350, "rakott"),
        new Food("Töltött káposzta", 150, 8, 350, "toltott kaposzta"),
        new Food("Bab (főtt)", 120, 8, 200, "bab"),
        new Food("Lencse (főtt)", 115, 9, 200, "lencse"),
        new Food("Borsó", 80, 5, 150, "borso"),
        new Food("Kukorica", 90, 3, 100, "kukorica"),
        new Food("Brokkoli", 35, 2.8, 150, "brokkoli"),
        new Food("Karfiol", 25, 2, 150, "karfiol"),
        new Food("Paradicsom", 18, 0.9, 100, "paradicsom"),
        new Food("Uborka", 15, 0.7, 100, "uborka"),
        new Food("Paprika", 25, 1, 100, "paprika"),
        new Food("Saláta (zöld)", 15, 1.4, 50, "salata"),
        new Food("Sajt (trappista)", 360, 25, 30, "sajt", "trappista"),
        new Food("Mozzarella", 280, 22, 50, "mozzarella"),
        new Food("Túró", 100, 12, 100, "turo"),
        new Food("Joghurt", 60, 4, 150, "joghurt"),
        new Food("Görög joghurt", 120, 9, 150, "gorog joghurt"),
        new Food("Tej", 60, 3.3, 200, "tej"),
        new Food("Vaj", 720, 0.9, 10, "vaj"),
        new Food("Olaj", 900, 0, 10, "olaj"),
        new Food("Majonéz", 680, 1, 20, "majonez"),
        new Food("Ketchup", 110, 1.7, 20, "ketchup"),
        new Food("Alma", 52, 0.3, 150, "alma"),
        new Food("Banán", 89, 1.1, 120, "banan"),
        new Food("Narancs", 47, 0.9, 150, "narancs"),
        new Food("Szőlő", 70, 0.7, 100, "szolo"),
        new Food("Eper", 33, 0.7, 100, "eper"),
        new Food("Avokádó", 160, 2, 70, "avokado"),
        new Food("Dió", 650, 15, 30, "dio"),
        new Food("Mandula", 580, 21, 30, "mandula"),
        new Food("Mogyoró", 570, 25, 30, "mogyoro"),
        new Food("Csokoládé", 550, 5, 25, "csoki", "csokolade"),
        new Food("Keksz", 450, 6, 40, "keksz"),
        new Food("Sütemény", 400, 5, 100, "sutemeny", "torta"),
        new Food("Fagylalt", 200, 3.5, 100, "fagyi", "fagylalt"),
        new Food("Chips", 540, 6, 50, "chips"),
        new Food("Nutella", 540, 6, 30, "nutella"),
        new Food("Lekvár", 250, 0.4, 25, "lekvar"),
        new Food("Méz", 320, 0.3, 20, "mez"),
        new Food("Cukor", 400, 0, 10, "cukor"),
        new Food("Üdítő (cukros)", 42, 0, 330, "udito", "kola", "cola"),
        new Food("Rántott sajt", 330, 18, 120, "rantott sajt"),
        new Food("Nokedli / galuska", 170, 5, 200, "nokedli", "galuska"),
        new Food("Tarhonya", 150, 5, 200, "tarhonya"),
        new Food("Káposzta", 25, 1.3, 150, "kaposzta"),
        new Food("Tükörtojás", 200, 13, 110, "tukortojas"),
        new Food("Bableves", 90, 5, 400, "bableves"),
        new Food("Húsleves", 40, 3, 400, "husleves"),
        new Food("Csirkepaprikás", 160, 14, 300, "paprikas"),
        new Food("Milánói makaróni", 180, 7, 350, "milanoi", "makaroni"),
        new Food("Protein turmix", 100, 10, 300, "protein", "turmix", "shake"),
        new Food("Túró rudi", 380, 8, 51, "turo rudi", "rudi"),
        new Food("Szendvics", 250, 10, 150, "szendvics"),
        new Food("Hot-dog", 290, 10, 150, "hot-dog", "hotdog"),
        new Food("Müzliszelet", 400, 6, 30, "muzliszelet", "szelet"),
        new Food("Párizsi", 230, 12, 50, "parizsi"),
        new Food("Tejföl", 200, 3, 30, "tejfol"),
        new Food("Kefir", 55, 3.5, 200, "kefir"),
        new Food("Kakaó (tejes)", 85, 3.5, 250, "kakao"),
        new Food("Tükörponty / halrudak", 220, 12, 150, "halrud", "halrudak"),
        new Food("Körte", 57, 0.4, 150, "korte"),
        new Food("Őszibarack", 39, 0.9, 150, "oszibarack", "barack"),
        new Food("Görögdinnye", 30, 0.6, 300, "dinnye"),
        new Food("Kivi", 60, 1.1, 80, "kivi"),
        new Food("Mandarin", 53, 0.8, 100, "mandarin"),
        new Food("Paradicsomleves", 60, 1.5, 400, "paradicsomleves"),
        new Food("Tökfőzelék", 70, 2, 350, "tokfozelek"),
        new Food("Gnocchi", 160, 4, 250, "gnocchi", "nudli"),
        new Food("Tortilla / wrap", 250, 8, 200, "tortilla", "wrap"),
        new Food("Túrós csusza", 210, 10, 300, "csusza"),
        new Food("Grízes tészta", 200, 6, 300, "griz"),
        new Food("Kakaós csiga", 380, 7, 90, "kakaos csiga", "csiga"),
        new Food("Rétes", 300, 5, 100, "retes"),
        new Food("Piskóta / kevert süti", 350, 6, 80, "piskota", "kevert"),
        new Food("Popcorn", 400, 12, 40, "popcorn", "pattogatott"),
        new Food("Energiaital", 45, 0, 250, "energiaital", "energia ital"),
        new Food("Sör", 43, 0.5, 500, "sor", "sör"),
        new Food("Gyümölcslé", 45, 0.5, 250, "gyumolcsle", "juice", "dzsusz"),
        new Food("Rizsszelet / puffasztott rizs", 380, 8, 10, "puffasztott", "rizsszelet"),
        new Food("Cottage cheese", 100, 11, 150, "cottage"),
        new Food("Skyr", 65, 11, 150, "skyr"),
        new Food("Tofu", 120, 12, 150, "tofu"),
        new Food("Csirkés saláta", 130, 12, 300, "csirkes salata", "cezar", "caesar"),
        new Food("Sushi", 150, 6, 250, "sushi"),
    };

    /** Ékezet-mentesítés + kisbetű, a rugalmas kereséshez. */
    static String norm(String s) {
        if (s == null) return "";
        s = s.toLowerCase(new Locale("hu"));
        return s.replace('á','a').replace('é','e').replace('í','i').replace('ó','o')
                .replace('ö','o').replace('ő','o').replace('ú','u').replace('ü','u')
                .replace('ű','u');
    }

    // ---------- Saját ételek (felhasználó által felvéve) ----------

    /** A felhasználó saját ételei; a szótő a saját név. */
    public static List<Food> custom(android.content.Context c) {
        List<Food> out = new ArrayList<>();
        try {
            org.json.JSONArray a = new org.json.JSONArray(
                    c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE)
                            .getString("custom_foods", "[]"));
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                String n = o.optString("n", "");
                if (n.isEmpty()) continue;
                out.add(new Food(n, o.optInt("k", 100), o.optDouble("p", 0),
                        Math.max(1, o.optInt("g", 100)), n));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void addCustom(android.content.Context c, String name, int kcal100,
                                 double prot100, int portion) {
        try {
            android.content.SharedPreferences sp =
                    c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE);
            org.json.JSONArray a = new org.json.JSONArray(sp.getString("custom_foods", "[]"));
            // Azonos nevű korábbi bejegyzés cseréje.
            org.json.JSONArray na = new org.json.JSONArray();
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.optJSONObject(i);
                if (o != null && !o.optString("n", "").equalsIgnoreCase(name)) na.put(o);
            }
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("n", name); o.put("k", kcal100); o.put("p", prot100); o.put("g", portion);
            na.put(o);
            sp.edit().putString("custom_foods", na.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static void removeCustom(android.content.Context c, String name) {
        try {
            android.content.SharedPreferences sp =
                    c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE);
            org.json.JSONArray a = new org.json.JSONArray(sp.getString("custom_foods", "[]"));
            org.json.JSONArray na = new org.json.JSONArray();
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.optJSONObject(i);
                if (o != null && !o.optString("n", "").equalsIgnoreCase(name)) na.put(o);
            }
            sp.edit().putString("custom_foods", na.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Saját + beépített ételek együtt (a sajátok elöl, így ők nyernek). */
    public static List<Food> all(android.content.Context c) {
        List<Food> out = custom(c);
        for (Food f : ALL) out.add(f);
        return out;
    }

    /** A lekérdezéshez legjobban illő étel, vagy null. Ragozott alakokat is talál. */
    public static Food find(String query) { return find(java.util.Arrays.asList(ALL), query); }

    public static Food find(android.content.Context c, String query) {
        return find(all(c), query);
    }

    static Food find(List<Food> list, String query) {
        String q = norm(query).trim();
        if (q.isEmpty()) return null;
        // 1) teljes kifejezés-egyezés a szótövekkel
        for (Food f : list)
            for (String st : f.stems)
                if (q.equals(norm(st))) return f;
        // 2) a lekérdezés tartalmazza a szótövet (pl. "csirkemellbol" ⊃ "csirkemell")
        Food best = null; int bestLen = 0;
        for (Food f : list)
            for (String st : f.stems) {
                String ns = norm(st);
                if (ns.length() > bestLen && q.contains(ns)) { best = f; bestLen = ns.length(); }
            }
        if (best != null) return best;
        // 3) szavankénti előtag-egyezés (pl. "rizzsel" kezdete "riz")
        for (String tok : q.split("[ ,]+"))
            for (Food f : list)
                for (String st : f.stems) {
                    String ns = norm(st);
                    if (tok.startsWith(ns) || (tok.length() >= 4 && ns.startsWith(tok)))
                        return f;
                }
        return null;
    }

    /** Egy felismert étel a szövegben, a hozzá tartozó grammal (0 = nem volt megadva). */
    public static final class Hit {
        public final Food food;
        public final double grams;
        Hit(Food food, double grams) { this.food = food; this.grams = grams; }
    }

    /**
     * Ételek felismerése a beírt szövegből úgy, hogy a mellettük álló
     * gramm-mennyiséget is kiolvassuk: „150 g csirkemell rizzsel",
     * „csirkemell 150g, rizs 200 g". Egy gramm-érték ahhoz az ételhez tartozik,
     * amelyik a szövegben a legközelebb áll hozzá (előtte vagy utána).
     * Ahol nincs szám, a gramm 0 marad, és a hívó dönt (közös adag / tipikus adag).
     */
    public static List<Hit> parse(android.content.Context c, String query) {
        List<Match> ms = matches(all(c), query);
        List<Hit> out = new ArrayList<>();
        if (ms.isEmpty()) return out;

        String q = norm(query);
        // Az ételek szövegbeli helye – ugyanaz, amit a felismerés használt.
        List<Food> foods = new ArrayList<>();
        List<Integer> foodPos = new ArrayList<>();
        for (Match m : ms) { foods.add(m.food); foodPos.add(m.pos); }
        // Gramm-értékek kigyűjtése: szám + (opcionális szóköz) + "g"/"gr"/"dkg".
        List<Integer> numPos = new ArrayList<>();
        List<Double> numVal = new ArrayList<>();
        int i = 0;
        while (i < q.length()) {
            if (!Character.isDigit(q.charAt(i))) { i++; continue; }
            int start = i;
            while (i < q.length() && Character.isDigit(q.charAt(i))) i++;
            double val;
            try { val = Double.parseDouble(q.substring(start, i)); }
            catch (NumberFormatException e) { continue; }
            int j = i;
            while (j < q.length() && q.charAt(j) == ' ') j++;
            // dkg előbb, különben a "g" ág nyelné el
            if (q.startsWith("dkg", j)) { numPos.add(start); numVal.add(val * 10); i = j + 3; }
            else if (q.startsWith("gramm", j)) { numPos.add(start); numVal.add(val); i = j + 5; }
            else if (q.startsWith("gr", j)) { numPos.add(start); numVal.add(val); i = j + 2; }
            else if (q.startsWith("g", j)
                    && (j + 1 >= q.length() || !Character.isLetter(q.charAt(j + 1)))) {
                numPos.add(start); numVal.add(val); i = j + 1;
            }
        }
        double[] grams = new double[foods.size()];
        // Minden gramm-érték a hozzá legközelebbi ételhez kerül, amelyiknek még nincs.
        for (int n = 0; n < numPos.size(); n++) {
            int bestIdx = -1, bestDist = Integer.MAX_VALUE;
            for (int k = 0; k < foods.size(); k++) {
                if (grams[k] > 0 || foodPos.get(k) < 0) continue;
                int d = Math.abs(foodPos.get(k) - numPos.get(n));
                if (d < bestDist) { bestDist = d; bestIdx = k; }
            }
            if (bestIdx >= 0) grams[bestIdx] = numVal.get(n);
        }
        for (int k = 0; k < foods.size(); k++) out.add(new Hit(foods.get(k), grams[k]));
        return out;
    }

    /** Az összes étel, ami a szövegben felismerhető (a szöveg sorrendjében, ismétlés nélkül). */
    public static List<Food> findAll(String query) {
        return findAll(java.util.Arrays.asList(ALL), query);
    }

    public static List<Food> findAll(android.content.Context c, String query) {
        return findAll(all(c), query);
    }

    /** Egy találat helye a szövegben (a leghosszabb illeszkedő szótő szerint). */
    static final class Match {
        final Food food; final int pos, len;
        Match(Food food, int pos, int len) { this.food = food; this.pos = pos; this.len = len; }
    }

    /**
     * A szövegben felismerhető ételek, helyükkel együtt.
     *
     * Fontos: egy rövid szótő beleeshet egy hosszabb ételnévbe („sajt" a
     * „rántott sajt"-ban, „tej" a „tejföl"-ben, „riz" a „rizsszelet"-ben).
     * Ilyenkor csak a hosszabb találat marad, különben ugyanaz a falat kétszer
     * kerülne be, és dupla kalóriát számolnánk.
     */
    static List<Match> matches(List<Food> list, String query) {
        String q = norm(query);
        List<Match> found = new ArrayList<>();
        for (Food f : list) {
            int bestPos = -1, bestLen = 0;
            for (String st : f.stems) {
                String ns = norm(st);
                if (ns.isEmpty()) continue;
                int p = q.indexOf(ns);
                // A leghosszabb illeszkedő szótő dönt; azonos hossznál a korábbi.
                if (p >= 0 && (ns.length() > bestLen || (ns.length() == bestLen && p < bestPos))) {
                    bestPos = p; bestLen = ns.length();
                }
            }
            if (bestPos >= 0) found.add(new Match(f, bestPos, bestLen));
        }
        // A hosszabb találatba beleeső rövidebbeket eldobjuk.
        List<Match> out = new ArrayList<>();
        for (Match m : found) {
            boolean covered = false;
            for (Match o : found) {
                if (o == m) continue;
                boolean inside = o.pos <= m.pos && o.pos + o.len >= m.pos + m.len;
                if (inside && o.len > m.len) { covered = true; break; }
            }
            if (!covered) out.add(m);
        }
        // Rendezés a szövegbeli előfordulás szerint.
        for (int i = 0; i < out.size(); i++)
            for (int j = i + 1; j < out.size(); j++)
                if (out.get(j).pos < out.get(i).pos) {
                    Match t = out.get(i); out.set(i, out.get(j)); out.set(j, t);
                }
        return out;
    }

    static List<Food> findAll(List<Food> list, String query) {
        List<Food> out = new ArrayList<>();
        for (Match m : matches(list, query)) out.add(m.food);
        return out;
    }

    /** Ismert ételnevek (javaslatokhoz). */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (Food f : ALL) out.add(f.name);
        return out;
    }
}
