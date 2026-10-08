package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;

/**
 * „Mi férne még bele ma?" – konkrét étel-ötletek a napi maradékra.
 *
 * A napi kártya eddig megmondta, hány kalória és hány gramm fehérje hiányzik.
 * Ez önmagában számtan: a kérdés, amire a felhasználó választ vár, az, hogy
 * MIT egyen. Ez az osztály a maradékból ad néhány valódi ötletet, adaggal
 * együtt – abból az étel-adatbázisból, amit az app amúgy is ismer, hogy a
 * javaslatot egy koppintással fel is lehessen venni.
 *
 * A jelöltek szándékosan kézzel válogatott, rövid listák. Ha bármelyik ételt
 * megengednénk, akkor a „még 300 kalória belefér" ötletként visszajöhetne egy
 * korsó sör – formailag igaz, tanácsnak méltatlan.
 *
 * Tiszta Java (nincs Context), hogy egységteszttel lefedhető legyen.
 */
public final class MealIdeas {

    private MealIdeas() {
    }

    /** Ennél kevesebb maradékra már nincs értelmes ötlet. */
    static final double MIN_KCAL = 60;
    /** Efölött számít úgy, hogy tényleg hiányzik fehérje. */
    static final double PROTEIN_GAP = 10;
    /** Legfeljebb ennyi ötletet adunk – a hosszú lista nem választás, hanem zaj. */
    static final int MAX = 3;

    /**
     * Fehérje-pótlásra. Mind valódi, önmagában megehető étel: se fűszer, se
     * olyasmi, amit senki nem eszik magában.
     */
    static final String[] PROTEIN = {
            "Görög joghurt", "Túró", "Cottage cheese", "Skyr", "Tojás",
            "Csirkemell (sült/grill)", "Pulykamell", "Tonhal", "Hal (fehér)",
            "Sonka", "Mozzarella", "Ricotta", "Tofu", "Edamame", "Seitan",
            "Tempeh", "Lencse (főtt)", "Bab (főtt)", "Csicseriborsó (főtt)",
            "Protein turmix", "Fehérjepor", "Proteinszelet", "Csirkés saláta",
            "Tonhalsaláta", "Kefir", "Joghurt",
    };

    /** Ha csak a kalória hiányzik: laktató vagy friss, de nem nehéz. */
    static final String[] LIGHT = {
            "Alma", "Banán", "Narancs", "Körte", "Eper", "Málna", "Áfonya",
            "Kivi", "Mandarin", "Őszibarack", "Szőlő", "Görögdinnye",
            "Sárgarépa", "Paprika", "Uborka", "Paradicsom", "Zöldség (vegyes / párolt)",
            "Brokkoli", "Karfiol", "Zöldbab", "Görög saláta", "Leves (átlag)",
            "Zabpehely", "Gyümölcsturmix / smoothie", "Dió", "Mandula",
    };

    public static final class Idea {
        public final String name;
        public final double grams;
        public final double kcal;
        public final double protein;
        /** Igaz, ha fehérje-hiány miatt került be. */
        public final boolean forProtein;

        Idea(String name, double grams, double kcal, double protein, boolean forProtein) {
            this.name = name; this.grams = grams; this.kcal = kcal;
            this.protein = protein; this.forProtein = forProtein;
        }

        /** Egy sor a kártyára: „Görög joghurt 150 g · 180 kcal · 14 g fehérje". */
        public String label() {
            String s = name + " " + Math.round(grams) + " g · " + Math.round(kcal) + " kcal";
            if (protein >= 3) s += " · " + Math.round(protein) + " g fehérje";
            return s;
        }
    }

    /**
     * Ötletek a napi maradékra.
     *
     * @param all         az ismert ételek (Foods.ALL vagy a saját ételekkel bővített lista)
     * @param kcalLeft    hány kalória fér még bele; 0 vagy kevesebb esetén nincs ötlet
     * @param proteinLeft hány gramm fehérje hiányzik még (0, ha nincs cél)
     * @param seed        naponta változó szám a választékhoz (pl. a nap sorszáma)
     */
    public static List<Idea> forRemaining(Foods.Food[] all, double kcalLeft,
                                          double proteinLeft, long seed) {
        List<Idea> out = new ArrayList<>();
        if (all == null || kcalLeft < MIN_KCAL) return out;

        boolean needProtein = proteinLeft >= PROTEIN_GAP;
        String[] pool = needProtein ? PROTEIN : LIGHT;

        List<Idea> fits = new ArrayList<>();
        for (String n : pool) {
            Foods.Food f = find(all, n);
            if (f == null) continue;
            double grams = fit(f, kcalLeft);
            if (grams <= 0) continue;
            fits.add(new Idea(f.name, grams, grams * f.kcal100 / 100.0,
                    grams * f.prot100 / 100.0, needProtein));
        }
        if (fits.isEmpty()) return out;

        // Fehérje-hiánynál a legtöbb fehérjét adó ötlet a leghasznosabb;
        // egyébként az, ami a maradékot a legjobban kitölti anélkül, hogy
        // átlépné – a fél alma nem ötlet.
        final boolean prot = needProtein;
        final double left = kcalLeft;
        java.util.Collections.sort(fits, (a, b) -> {
            double sa = prot ? a.protein : a.kcal / left;
            double sb = prot ? b.protein : b.kcal / left;
            int c = Double.compare(sb, sa);
            return c != 0 ? c : a.name.compareTo(b.name);
        });

        // A legjobb néhányból naponta más hármat mutatunk: ugyanaz a három
        // ötlet egy hét alatt tapétává válna.
        int poolSize = Math.min(fits.size(), MAX * 3);
        long s = seed % poolSize;
        if (s < 0) s += poolSize;
        for (int i = 0; i < poolSize && out.size() < MAX; i++)
            out.add(fits.get((int) ((s + i) % poolSize)));
        return out;
    }

    private static Foods.Food find(Foods.Food[] all, String name) {
        for (Foods.Food f : all) if (f.name.equals(name)) return f;
        return null;
    }

    /**
     * Mekkora adag fér bele. A tipikus adagból indulunk; ha az sok, arányosan
     * csökkentjük 10 grammos lépésekben, de a felénél kisebb adagot nem
     * ajánlunk – „egyél 40 g csirkemellet" nem tanács.
     */
    private static double fit(Foods.Food f, double kcalLeft) {
        if (f.kcal100 <= 0 || f.portion <= 0) return 0;
        double portionKcal = f.portion * f.kcal100 / 100.0;
        if (portionKcal <= kcalLeft) return f.portion;
        double grams = Math.floor(kcalLeft * 100.0 / f.kcal100 / 10.0) * 10;
        if (grams < f.portion / 2.0 || grams < 10) return 0;
        return grams;
    }
}
