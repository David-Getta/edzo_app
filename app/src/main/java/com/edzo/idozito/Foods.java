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
        // A „kolbásszal" alakban a sz megkettőződik, ezért az is szótő.
        new Food("Kolbász", 350, 15, 100, "kolbasz", "kolbassz"),
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
        new Food("Burgonyapüré", 110, 2, 200, "burgonyapure", "pure", "püré"),
        new Food("Édesburgonya", 90, 1.6, 200, "edesburgonya"),
        new Food("Bulgur (főtt)", 120, 4, 200, "bulgur"),
        new Food("Quinoa (főtt)", 120, 4.4, 200, "quinoa"),
        new Food("Kenyér", 250, 8, 70, "kenyer"),
        new Food("Zsemle", 280, 9, 55, "zsemle"),
        new Food("Kifli", 290, 8, 55, "kifli"),
        new Food("Péksütemény", 350, 7, 80, "peksutemeny", "croissant"),
        new Food("Zabpehely", 370, 13, 50, "zab"),
        new Food("Müzli", 380, 9, 60, "muzli", "müzli", "granola"),
        new Food("Palacsinta", 220, 6, 150, "palacsinta"),
        new Food("Pizza", 260, 11, 300, "pizza"),
        new Food("Hamburger", 280, 13, 250, "hamburger", "burger"),
        new Food("Gyros", 220, 15, 350, "gyros"),
        new Food("Lángos", 320, 7, 200, "langos"),
        new Food("Gulyásleves", 100, 7, 400, "gulyasleves", "gulyas leves", "gulyas"),
        new Food("Pörkölt", 180, 15, 300, "porkolt"),
        new Food("Főzelék", 80, 3, 350, "fozelek"),
        new Food("Leves (átlag)", 50, 3, 400, "leves"),
        new Food("Rakott krumpli", 160, 6, 350, "rakott krumpli", "rakott"),
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
        new Food("Túrós csusza", 210, 10, 300, "turos csusza", "csusza"),
        new Food("Grízes tészta", 200, 6, 300, "grizes teszta", "griz"),
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
        new Food("Lecsó", 70, 2, 300, "lecso"),
        new Food("Töltött paprika", 130, 8, 350, "toltott paprika"),
        new Food("Székelykáposzta", 150, 9, 350, "szekelykaposzta", "szekely kaposzta"),
        new Food("Halászlé", 60, 8, 400, "halaszle"),
        new Food("Paprikás krumpli", 120, 4, 350, "paprikas krumpli"),
        new Food("Rizses hús", 160, 8, 350, "rizses hus"),
        new Food("Bolognai spagetti", 170, 8, 350, "bolognai spagetti", "bolognai"),
        new Food("Sajtos tészta", 220, 8, 300, "sajtos teszta"),
        new Food("Tojásos nokedli", 190, 7, 300, "tojasos nokedli"),
        new Food("Rizottó", 150, 5, 300, "rizotto"),
        new Food("Túrógombóc", 210, 9, 200, "turogomboc"),
        new Food("Mákos guba", 300, 7, 200, "makos guba"),
        // A teljes kifejezés is szótő, különben a „galuska" külön a nokedlire ülne.
        new Food("Somlói galuska", 260, 5, 150, "somloi galuska", "somloi"),
        new Food("Kürtőskalács", 380, 6, 120, "kurtoskalacs"),
        new Food("Rántott gomba", 220, 5, 150, "rantott gomba"),
        new Food("Gomba", 22, 3, 100, "gomba"),
        new Food("Csirkemáj", 130, 20, 120, "csirkemaj", "maj"),
        new Food("Sült oldalas", 290, 20, 200, "oldalas"),
        new Food("Csülök", 280, 22, 200, "csulok"),
        new Food("Kacsa / liba", 300, 19, 180, "kacsa", "liba"),
        new Food("Mustár", 60, 4, 10, "mustar"),
        new Food("Uborkasaláta", 40, 0.7, 150, "uborkasalata"),
        new Food("Céklasaláta", 45, 1.3, 100, "ceklasalata", "cekla"),
        new Food("Zöldbab", 35, 1.8, 150, "zoldbab"),
        new Food("Spenót / paraj", 25, 2.9, 200, "spenot", "paraj"),
        new Food("Krémleves (zöldség)", 60, 2, 350, "kremleves"),
        new Food("Kefires / joghurtos öntet", 60, 3, 40, "kefires ontet", "joghurtos ontet", "ontet"),
        new Food("Rántott hal", 230, 16, 180, "rantott hal"),
        new Food("Csirkés wrap", 200, 12, 250, "csirkes wrap"),
        // --- Zöldségek, gyümölcsök, magvak ---
        new Food("Sárgarépa", 41, 0.9, 100, "sargarepa", "repa"),
        new Food("Cukkini", 17, 1.2, 200, "cukkini"),
        new Food("Padlizsán", 25, 1, 200, "padlizsan"),
        new Food("Hagyma", 40, 1.1, 50, "hagyma"),
        new Food("Ananász", 50, 0.5, 150, "ananasz"),
        new Food("Málna", 52, 1.2, 100, "malna"),
        new Food("Áfonya", 57, 0.7, 100, "afonya"),
        new Food("Szilva", 46, 0.7, 100, "szilva"),
        new Food("Cseresznye / meggy", 60, 1, 150, "cseresznye", "meggy"),
        new Food("Datolya", 280, 2.5, 30, "datolya"),
        new Food("Tökmag / napraforgómag", 570, 22, 30, "tokmag", "napraforgomag", "napraforgo"),
        // --- Magyar klasszikusok ---
        new Food("Bundás kenyér", 260, 9, 120, "bundas kenyer", "bundaskenyer"),
        new Food("Pogácsa", 400, 8, 60, "pogacsa"),
        new Food("Zsíros kenyér", 330, 6, 100, "zsiros kenyer", "zsiroskenyer"),
        new Food("Hurka", 300, 12, 120, "hurka"),
        new Food("Csirkenugget", 300, 15, 150, "nugget"),
        new Food("Tejbegríz", 110, 4, 250, "tejbegriz"),
        new Food("Túrós batyu", 300, 7, 100, "turos batyu", "batyu"),
        new Food("Kalács / bejgli", 350, 8, 80, "kalacs", "bejgli"),
        new Food("Almás pite", 240, 3, 120, "almas pite", "almaspite"),
        new Food("Krumplisaláta", 150, 2.5, 200, "krumplisalata"),
        new Food("Frankfurti leves", 90, 4, 350, "frankfurti leves", "frankfurti"),
        new Food("Körözött", 250, 12, 80, "korozott"),
        new Food("Sajtkrém", 250, 8, 40, "sajtkrem"),
        // --- Italok ---
        new Food("Kávé (fekete)", 2, 0.2, 200, "kave", "feketekave", "eszpresszo"),
        new Food("Tejeskávé / cappuccino", 55, 3, 250, "tejeskave", "cappuccino", "latte"),
        new Food("Tea (cukor nélkül)", 1, 0, 250, "tea"),
        new Food("Bor (vörös/fehér)", 80, 0.1, 150, "bor", "vorosbor", "feherbor"),
        new Food("Pálinka / tömény", 250, 0, 40, "palinka", "tomeny", "vodka", "whisky"),
        new Food("Növényi tej (mandula/zab)", 40, 1, 250, "novenyi tej", "mandulatej",
                "zabtej", "rizstej", "szojatej"),
        new Food("Szójakocka", 340, 50, 60, "szojakocka", "szoja"),
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
        // 3) Szavankénti egyezés, ha a 2. fázis nem talált semmit: ragozott alak
        // (a szó a szótővel kezdődik, pl. "rizzsel" → "riz"), vagy gépelés közbeni
        // előtag (a szótő kezdődik a szóval). Mindkettőnél a leghosszabb szótő nyer.
        //
        // Megjegyzés: ezt szándékosan NEM engedjük a 2. fázis elé. Kipróbáltuk, és
        // rosszabb lett: a "rizs" a Rizsszeletre, a "burg" a Hamburgerre esett
        // volna a Rizs, illetve a Burgonya helyett.
        for (String tok : q.split("[ ,]+")) {
            if (tok.isEmpty()) continue;
            Food byTok = null; int tokLen = 0;
            for (Food f : list)
                for (String st : f.stems) {
                    String ns = norm(st);
                    if (ns.isEmpty()) continue;
                    boolean hit = tok.startsWith(ns)
                            || (tok.length() >= 4 && ns.startsWith(tok));
                    if (hit && ns.length() > tokLen) { byTok = f; tokLen = ns.length(); }
                }
            if (byTok != null) return byTok;
        }
        return null;
    }

    /**
     * Darabsúlyok azokhoz az ételekhez, amiket természetes darabra számolni
     * („2 tojás", „3 banán"). Csak ezeknél értelmezünk mértékegység nélküli
     * számot darabszámként – másutt egy puszta szám nem jelent semmit.
     */
    private static final String[][] PIECE_GRAMS = {
            {"Tojás", "55"}, {"Tükörtojás", "55"},
            {"Banán", "120"}, {"Alma", "150"}, {"Narancs", "150"}, {"Körte", "150"},
            {"Kivi", "80"}, {"Mandarin", "100"}, {"Őszibarack", "150"},
            {"Zsemle", "55"}, {"Kifli", "55"}, {"Kenyér", "35"},
            {"Túró rudi", "51"}, {"Müzliszelet", "30"}, {"Palacsinta", "60"},
            {"Virsli", "50"}, {"Kakaós csiga", "90"}, {"Fasírt", "60"},
            {"Szendvics", "150"}, {"Hot-dog", "150"},
            // A v28.2-ben érkezett ételek közül azok, amiket természetes darabra
            // mondani („2 pogácsa", „három szilva").
            {"Pogácsa", "30"}, {"Túrós batyu", "100"}, {"Bundás kenyér", "60"},
            {"Datolya", "8"}, {"Szilva", "50"}, {"Sárgarépa", "80"},
            {"Hurka", "120"},
    };

    /**
     * Kiírt számnevek ékezet nélkül. Csak egész szóként és csak darabra
     * számolható étel előtt érvényesek, ezért a többjelentésű alakok („hat",
     * „het", „fel") sem okoznak félreértést.
     */
    private static final String[][] NUMBER_WORDS = {
            {"egy", "1"}, {"ket", "2"}, {"ketto", "2"}, {"harom", "3"}, {"negy", "4"},
            {"ot", "5"}, {"hat", "6"}, {"het", "7"}, {"nyolc", "8"}, {"kilenc", "9"},
            {"tiz", "10"}, {"fel", "0.5"},
    };

    /** Egy darab hány gramm, vagy 0, ha ezt az ételt nem darabra számoljuk. */
    static int pieceGrams(Food f) {
        for (String[] p : PIECE_GRAMS)
            if (p[0].equals(f.name)) {
                try { return Integer.parseInt(p[1]); } catch (NumberFormatException e) { return 0; }
            }
        return 0;
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
        return parse(all(c), query);
    }

    static List<Hit> parse(List<Food> list, String query) {
        List<Match> ms = matches(list, query);
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
        List<Integer> bareNumPos = new ArrayList<>();
        List<Double> bareNumVal = new ArrayList<>();
        List<Integer> bareNumLen = new ArrayList<>();
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
            // A hosszabb mértékegység előbb, különben a rövidebb ág nyelné el.
            // Folyadéknál 1 ml ≈ 1 g, ezért a dl/ml/l is grammra váltható.
            if (q.startsWith("dkg", j)) { numPos.add(start); numVal.add(val * 10); i = j + 3; }
            else if (q.startsWith("gramm", j)) { numPos.add(start); numVal.add(val); i = j + 5; }
            else if (q.startsWith("deci", j)) { numPos.add(start); numVal.add(val * 100); i = j + 4; }
            else if (q.startsWith("dl", j)) { numPos.add(start); numVal.add(val * 100); i = j + 2; }
            else if (q.startsWith("ml", j)) { numPos.add(start); numVal.add(val); i = j + 2; }
            else if (q.startsWith("gr", j)) { numPos.add(start); numVal.add(val); i = j + 2; }
            else if (q.startsWith("g", j)
                    && (j + 1 >= q.length() || !Character.isLetter(q.charAt(j + 1)))) {
                numPos.add(start); numVal.add(val); i = j + 1;
            } else if (q.startsWith("l", j)
                    && (j + 1 >= q.length() || !Character.isLetter(q.charAt(j + 1)))) {
                numPos.add(start); numVal.add(val * 1000); i = j + 1;
            } else {
                // Mértékegység nélküli szám: darabszám lehet („2 tojás"). Csak akkor
                // vesszük annak, ha rögtön utána egy darabra számolható étel áll –
                // különben a szám nem jelent semmit, és figyelmen kívül hagyjuk.
                bareNumPos.add(start);
                bareNumVal.add(val);
                bareNumLen.add(i - start);
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
        // Kiírt számnevek is számítanak („két tojás", „fél alma"), egész szóként.
        for (String[] w : NUMBER_WORDS) {
            int at = 0;
            while (true) {
                int p = q.indexOf(w[0], at);
                if (p < 0) break;
                at = p + w[0].length();
                boolean leftOk = p == 0 || !Character.isLetter(q.charAt(p - 1));
                boolean rightOk = at >= q.length() || !Character.isLetter(q.charAt(at));
                if (leftOk && rightOk) {
                    bareNumPos.add(p);
                    bareNumVal.add(Double.parseDouble(w[1]));
                    bareNumLen.add(w[0].length());
                }
            }
        }
        // Darabszámok: „2 tojás" = 2 × egy tojás súlya. Csak akkor számít, ha a
        // szám közvetlenül egy darabra számolható étel előtt áll, az étel még nem
        // kapott grammot, és a darabszám életszerű (legfeljebb 20).
        for (int n = 0; n < bareNumPos.size(); n++) {
            double count = bareNumVal.get(n);
            if (count < 0.5 || count > 20) continue;
            int numEnd = bareNumPos.get(n) + bareNumLen.get(n);
            for (int k = 0; k < foods.size(); k++) {
                if (grams[k] > 0 || foodPos.get(k) < 0) continue;
                int gap = foodPos.get(k) - numEnd;
                if (gap < 0 || gap > 2) continue;         // közvetlenül utána álljon
                int piece = pieceGrams(foods.get(k));
                if (piece <= 0) continue;
                grams[k] = count * piece;
                break;
            }
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

    /**
     * Összetett ételek: ha a felsorolt szavak mind szerepelnek a szövegben, akkor
     * együtt EGY ételt jelentenek, nem külön-külön hozzávalókat. A „csirkemellből
     * rántott húst" enélkül Csirkemell + Rántott hús (sertés) lenne, vagyis a húst
     * kétszer számolnánk – holott a Rántott csirkemell pont ezt írja le.
     *
     * Az első elem a cél-étel neve, a többi a keresett szó.
     */
    private static final String[][] COMBOS = {
            {"Rántott csirkemell", "rantott", "csirke"},
    };

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
        applyCombos(list, q, out);
        // Rendezés a szövegbeli előfordulás szerint.
        for (int i = 0; i < out.size(); i++)
            for (int j = i + 1; j < out.size(); j++)
                if (out.get(j).pos < out.get(i).pos) {
                    Match t = out.get(i); out.set(i, out.get(j)); out.set(j, t);
                }
        return out;
    }

    /** Az összetett-étel szabályok alkalmazása a már megtalált találatokra. */
    private static void applyCombos(List<Food> list, String q, List<Match> out) {
        for (String[] combo : COMBOS) {
            int from = Integer.MAX_VALUE, to = -1;
            boolean all = true;
            for (int i = 1; i < combo.length; i++) {
                int p = q.indexOf(combo[i]);
                if (p < 0) { all = false; break; }
                from = Math.min(from, p);
                to = Math.max(to, p + combo[i].length());
            }
            if (!all) continue;
            Food target = null;
            for (Food f : list) if (f.name.equals(combo[0])) { target = f; break; }
            if (target == null) continue;
            // A szavak által lefedett szakaszra eső találatok helyébe lép az
            // összetett étel; a szakaszon kívüli körettel nem csinálunk semmit.
            for (int i = out.size() - 1; i >= 0; i--) {
                Match m = out.get(i);
                if (m.pos < to && m.pos + m.len > from) out.remove(i);
            }
            out.add(new Match(target, from, to - from));
        }
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
