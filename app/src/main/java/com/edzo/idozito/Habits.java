package com.edzo.idozito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A szokásos étkezésed felismerése napszakonként.
 *
 * A kedvencek listáját kézzel kell összerakni – ez magától veszi észre, hogy
 * reggelente ugyanazt a három dolgot eszed. Aki naponta naplóz, annak ez a
 * leggyakoribb művelet, és a leggyakoribb műveletet illik egy koppintásra
 * csökkenteni.
 *
 * Tiszta Java (nincs Context), hogy egységteszttel lefedhető legyen: egy
 * rosszul felismert „szokás" naponta rossz kalóriát írna a naplóba.
 */
public final class Habits {

    private Habits() {
    }

    /** Napszakok: reggel, ebéd, uzsonna, vacsora. */
    public static final int REGGEL = 0, EBED = 1, UZSONNA = 2, VACSORA = 3;

    /** Ennyiszer kell előfordulnia, hogy szokásnak nevezzük. */
    public static final int MIN_COUNT = 3;

    /** Ennél régebbi étkezés már nem a mostani szokásod. */
    public static final int WINDOW_DAYS = 30;

    /** Az óra melyik napszakba esik. */
    public static int bucketOf(int hour) {
        if (hour >= 4 && hour < 10) return REGGEL;
        if (hour >= 10 && hour < 15) return EBED;
        if (hour >= 15 && hour < 18) return UZSONNA;
        return VACSORA;
    }

    /** A napszak neve, ahogy a felhasználó is hívja. */
    public static String bucketName(int bucket) {
        switch (bucket) {
            case REGGEL: return "reggeli";
            case EBED: return "ebéd";
            case UZSONNA: return "uzsonna";
            default: return "vacsora";
        }
    }

    /**
     * Birtokos alak: „a szokásos reggelid".
     *
     * Nem lehet ragot fűzni a névhez, mert a magyar nem így működik: a
     * „reggeli" + „ed" nem „reggelied", hanem „reggelid".
     */
    public static String bucketMine(int bucket) {
        switch (bucket) {
            case REGGEL: return "reggelid";
            case EBED: return "ebéded";
            case UZSONNA: return "uzsonnád";
            default: return "vacsorád";
        }
    }

    public static final class Usual {
        /** Az étkezés elemei, ahogy a naplóban szerepeltek. */
        public final List<String> foods;
        /** Hányszor fordult elő pontosan ez az összeállítás. */
        public final int count;

        Usual(List<String> foods, int count) {
            this.foods = foods; this.count = count;
        }

        /** „🍳 A szokásos reggelid (5×)”. */
        public String label(int bucket) {
            String emoji = bucket == REGGEL ? "🍳" : bucket == EBED ? "🍲"
                    : bucket == UZSONNA ? "🥪" : "🍽";
            return emoji + "  A szokásos " + bucketMine(bucket) + "  ·  " + count + "×";
        }
    }

    /**
     * A megadott napszak leggyakoribb étkezés-összeállítása, vagy null.
     *
     * Az összeállítás akkor számít azonosnak, ha UGYANAZOK az ételek vannak
     * benne – a sorrend és a mennyiség nem számít. Aki reggelente két tojást
     * eszik, néha hármat, annak ugyanaz a reggeli.
     *
     * @param foodsPerMeal étkezésenként az elemek nevei
     * @param hours        az étkezés órája (0–23)
     * @param daysAgo      hány napja volt az étkezés
     * @param bucket       melyik napszakra kérdezünk
     */
    public static Usual usual(List<List<String>> foodsPerMeal, int[] hours, int[] daysAgo,
                              int bucket) {
        if (foodsPerMeal == null || hours == null || daysAgo == null) return null;
        int n = Math.min(foodsPerMeal.size(), Math.min(hours.length, daysAgo.length));
        // Kulcs: a rendezett, kisbetűs névlista. Így a „tojás, kenyér" és a
        // „kenyér, tojás" ugyanaz a reggeli.
        LinkedHashMap<String, int[]> counts = new LinkedHashMap<>();
        LinkedHashMap<String, List<String>> sample = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            if (daysAgo[i] < 0 || daysAgo[i] > WINDOW_DAYS) continue;
            if (bucketOf(hours[i]) != bucket) continue;
            List<String> items = foodsPerMeal.get(i);
            if (items == null || items.isEmpty()) continue;
            List<String> norm = new ArrayList<>();
            for (String f : items) {
                if (f == null) continue;
                String t = f.trim();
                if (!t.isEmpty() && !norm.contains(t)) norm.add(t);
            }
            if (norm.isEmpty()) continue;
            List<String> sorted = new ArrayList<>(norm);
            java.util.Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
            StringBuilder key = new StringBuilder();
            for (String f : sorted) key.append(Foods.norm(f)).append('|');
            String k = key.toString();
            int[] c = counts.get(k);
            if (c == null) { counts.put(k, c = new int[1]); sample.put(k, norm); }
            c[0]++;
        }
        String bestKey = null;
        int best = 0;
        for (Map.Entry<String, int[]> e : counts.entrySet())
            if (e.getValue()[0] > best) { best = e.getValue()[0]; bestKey = e.getKey(); }
        if (bestKey == null || best < MIN_COUNT) return null;
        return new Usual(sample.get(bestKey), best);
    }
}
