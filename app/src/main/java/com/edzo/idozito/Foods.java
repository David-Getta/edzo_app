package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Beépített élelmiszer-adatbázis (kcal / 100 g, közelítő értékek), magyar
 * hétköznapi ételekkel. A keresés szótő-alapú, így a ragozott alakok
 * („rizzsel", „csirkemellből") is találnak.
 */
public final class Foods {

    private Foods() {}

    public static final class Food {
        public final String name;
        public final int kcal100;
        final String[] stems;
        Food(String name, int kcal100, String... stems) {
            this.name = name; this.kcal100 = kcal100; this.stems = stems;
        }
    }

    public static final Food[] ALL = {
        new Food("Rántott hús (sertés)", 320, "rantott hus", "rantotthus", "bécsi", "becsi"),
        new Food("Rántott csirkemell", 250, "rantott csirke"),
        new Food("Csirkemell (sült/grill)", 165, "csirkemell", "csirke mell", "grillcsirke"),
        new Food("Csirkecomb", 210, "csirkecomb", "comb"),
        new Food("Pulykamell", 105, "pulyka"),
        new Food("Sertéskaraj", 240, "karaj", "sertes"),
        new Food("Marhahús", 250, "marha"),
        new Food("Fasírt", 290, "fasirt"),
        new Food("Kolbász", 350, "kolbasz"),
        new Food("Virsli", 250, "virsli"),
        new Food("Sonka", 120, "sonka"),
        new Food("Szalámi", 400, "szalami"),
        new Food("Bacon", 500, "bacon", "szalonna"),
        new Food("Hal (fehér)", 120, "hal"),
        new Food("Tonhal", 130, "tonhal"),
        new Food("Lazac", 210, "lazac"),
        new Food("Tojás", 155, "tojas"),
        new Food("Rántotta", 180, "rantotta"),
        new Food("Rizs (főtt)", 130, "riz"),
        new Food("Tészta (főtt)", 150, "teszta", "spagetti", "penne"),
        new Food("Burgonya (főtt)", 87, "burgonya", "krumpli"),
        new Food("Sült krumpli", 300, "sult krumpli", "hasabburgonya", "hasáb"),
        new Food("Burgonyapüré", 110, "pure", "püré"),
        new Food("Édesburgonya", 90, "edesburgonya"),
        new Food("Bulgur (főtt)", 120, "bulgur"),
        new Food("Quinoa (főtt)", 120, "quinoa"),
        new Food("Kenyér", 250, "kenyer"),
        new Food("Zsemle", 280, "zsemle"),
        new Food("Kifli", 290, "kifli"),
        new Food("Péksütemény", 350, "peksutemeny", "croissant", "pogacsa"),
        new Food("Zabpehely", 370, "zab"),
        new Food("Müzli", 380, "muzli", "müzli", "granola"),
        new Food("Palacsinta", 220, "palacsinta"),
        new Food("Pizza", 260, "pizza"),
        new Food("Hamburger", 280, "hamburger", "burger"),
        new Food("Gyros", 220, "gyros"),
        new Food("Lángos", 320, "langos"),
        new Food("Gulyásleves", 100, "gulyas"),
        new Food("Pörkölt", 180, "porkolt"),
        new Food("Főzelék", 80, "fozelek"),
        new Food("Leves (átlag)", 50, "leves"),
        new Food("Rakott krumpli", 160, "rakott"),
        new Food("Töltött káposzta", 150, "toltott kaposzta"),
        new Food("Bab (főtt)", 120, "bab"),
        new Food("Lencse (főtt)", 115, "lencse"),
        new Food("Borsó", 80, "borso"),
        new Food("Kukorica", 90, "kukorica"),
        new Food("Brokkoli", 35, "brokkoli"),
        new Food("Karfiol", 25, "karfiol"),
        new Food("Paradicsom", 18, "paradicsom"),
        new Food("Uborka", 15, "uborka"),
        new Food("Paprika", 25, "paprika"),
        new Food("Saláta (zöld)", 15, "salata"),
        new Food("Sajt (trappista)", 360, "sajt", "trappista"),
        new Food("Mozzarella", 280, "mozzarella"),
        new Food("Túró", 100, "turo"),
        new Food("Joghurt", 60, "joghurt"),
        new Food("Görög joghurt", 120, "gorog joghurt"),
        new Food("Tej", 60, "tej"),
        new Food("Vaj", 720, "vaj"),
        new Food("Olaj", 900, "olaj"),
        new Food("Majonéz", 680, "majonez"),
        new Food("Ketchup", 110, "ketchup"),
        new Food("Alma", 52, "alma"),
        new Food("Banán", 89, "banan"),
        new Food("Narancs", 47, "narancs"),
        new Food("Szőlő", 70, "szolo"),
        new Food("Eper", 33, "eper"),
        new Food("Avokádó", 160, "avokado"),
        new Food("Dió", 650, "dio"),
        new Food("Mandula", 580, "mandula"),
        new Food("Mogyoró", 570, "mogyoro"),
        new Food("Csokoládé", 550, "csoki", "csokolade"),
        new Food("Keksz", 450, "keksz"),
        new Food("Sütemény", 400, "sutemeny", "torta"),
        new Food("Fagylalt", 200, "fagyi", "fagylalt"),
        new Food("Chips", 540, "chips"),
        new Food("Nutella", 540, "nutella"),
        new Food("Lekvár", 250, "lekvar"),
        new Food("Méz", 320, "mez"),
        new Food("Cukor", 400, "cukor"),
        new Food("Üdítő (cukros)", 42, "udito", "kola", "cola"),
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

    /** Ismert ételnevek (javaslatokhoz). */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (Food f : ALL) out.add(f.name);
        return out;
    }
}
