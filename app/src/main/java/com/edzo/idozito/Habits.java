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
     * Ennyi hétre visszamenőleg nézzük a heti szokást. Nyolc hét elég ahhoz,
     * hogy egy szokás kirajzolódjon, és rövid ahhoz, hogy a tavalyi
     * szokásokat ne emlegessük.
     */
    public static final int SPORT_WEEKS = 8;

    /**
     * Melyik sportágat szoktad ezen a napon? A hétköznaphoz kötött szokás
     * személyesebb minden általános biztatásnál: „kedd van – ilyenkor
     * általában úszni jársz".
     *
     * @param weekdays   bejegyzésenként a hét napja (0 = hétfő)
     * @param kindIds    bejegyzésenként a sportág azonosítója
     * @param daysAgo    hány napja volt
     * @param weekday    melyik napra kérdezünk
     * @return a sportág azonosítója, vagy null, ha nincs elég egyértelmű szokás
     */
    public static String usualSportOn(int[] weekdays, String[] kindIds, int[] daysAgo,
                                      int weekday) {
        if (weekdays == null || kindIds == null || daysAgo == null) return null;
        int n = Math.min(weekdays.length, Math.min(kindIds.length, daysAgo.length));
        LinkedHashMap<String, int[]> counts = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            if (weekdays[i] != weekday) continue;
            if (daysAgo[i] < 0 || daysAgo[i] > SPORT_WEEKS * 7) continue;
            String id = kindIds[i];
            if (id == null || id.trim().isEmpty()) continue;
            int[] c = counts.get(id);
            if (c == null) counts.put(id, c = new int[1]);
            c[0]++;
        }
        String best = null;
        int bestN = 0, secondN = 0;
        for (Map.Entry<String, int[]> e : counts.entrySet()) {
            int v = e.getValue()[0];
            if (v > bestN) { secondN = bestN; bestN = v; best = e.getKey(); }
            else if (v > secondN) secondN = v;
        }
        // Szokás akkor, ha elég sokszor volt ÉS egyértelműen kiemelkedik: két
        // egyforma gyakoriságú sportágból nem lehet megmondani, melyik a mai.
        if (bestN < MIN_COUNT || bestN == secondN) return null;
        return best;
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
            // [0] = hányszor volt, [1] = hány napja a legutóbbi.
            if (c == null) {
                counts.put(k, c = new int[]{0, Integer.MAX_VALUE});
                sample.put(k, norm);
            }
            c[0]++;
            c[1] = Math.min(c[1], daysAgo[i]);
        }
        String bestKey = null;
        int best = 0, bestFresh = Integer.MAX_VALUE;
        for (Map.Entry<String, int[]> e : counts.entrySet()) {
            int cnt = e.getValue()[0], fresh = e.getValue()[1];
            // Azonos gyakoriságnál a FRISSEBB nyer. Enélkül a tárolási sorrend
            // döntött, vagyis véletlenszerűen – és egy rossz „szokásos reggeli"
            // egy koppintással rossz kalóriát ír a naplóba.
            if (cnt > best || (cnt == best && fresh < bestFresh)) {
                best = cnt; bestFresh = fresh; bestKey = e.getKey();
            }
        }
        if (bestKey == null || best < MIN_COUNT) return null;
        return new Usual(sample.get(bestKey), best);
    }
}
