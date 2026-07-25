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
    };

    /** Ékezet-mentesítés + kisbetű, a rugalmas kereséshez. */
    static String norm(String s) {
        if (s == null) return "";
        s = s.toLowerCase(new Locale("hu"));
        return s.replace('á','a').replace('é','e').replace('í','i').replace('ó','o')
                .replace('ö','o').replace('ő','o').replace('ú','u').replace('ü','u')
                .replace('ű','u');
    }

    /** A lekérdezéshez legjobban illő étel, vagy null. Ragozott alakokat is talál. */
    public static Food find(String query) {
        String q = norm(query).trim();
        if (q.isEmpty()) return null;
        // 1) teljes kifejezés-egyezés a szótövekkel
        for (Food f : ALL)
            for (String st : f.stems)
                if (q.equals(norm(st))) return f;
        // 2) a lekérdezés tartalmazza a szótövet (pl. "csirkemellbol" ⊃ "csirkemell")
        Food best = null; int bestLen = 0;
        for (Food f : ALL)
            for (String st : f.stems) {
                String ns = norm(st);
                if (ns.length() > bestLen && q.contains(ns)) { best = f; bestLen = ns.length(); }
            }
        if (best != null) return best;
        // 3) szavankénti előtag-egyezés (pl. "rizzsel" kezdete "riz")
        for (String tok : q.split("[ ,]+"))
            for (Food f : ALL)
                for (String st : f.stems) {
                    String ns = norm(st);
                    if (tok.startsWith(ns) || (tok.length() >= 4 && ns.startsWith(tok)))
                        return f;
                }
        return null;
    }

    /** Az összes étel, ami a szövegben felismerhető (a szöveg sorrendjében, ismétlés nélkül). */
    public static List<Food> findAll(String query) {
        String q = norm(query);
        List<Food> out = new ArrayList<>();
        List<Integer> pos = new ArrayList<>();
        for (Food f : ALL) {
            int best = -1;
            for (String st : f.stems) {
                int p = q.indexOf(norm(st));
                if (p >= 0 && (best < 0 || p < best)) best = p;
            }
            if (best >= 0) { out.add(f); pos.add(best); }
        }
        // Egyszerű rendezés előfordulási hely szerint.
        for (int i = 0; i < out.size(); i++)
            for (int j = i + 1; j < out.size(); j++)
                if (pos.get(j) < pos.get(i)) {
                    Food tf = out.get(i); out.set(i, out.get(j)); out.set(j, tf);
                    int tp = pos.get(i); pos.set(i, pos.get(j)); pos.set(j, tp);
                }
        return out;
    }

    /** Ismert ételnevek (javaslatokhoz). */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (Food f : ALL) out.add(f.name);
        return out;
    }
}
