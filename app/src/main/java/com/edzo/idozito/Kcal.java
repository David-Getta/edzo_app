package com.edzo.idozito;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A dobozon írt kalória: ha a mondat kimondja, az a szám a pontos.
 *
 * Az adatbázis becsül – száz grammra vetített átlagokkal, tisztességes
 * közelítéssel. De amikor az ember a kezében tartja a csomagolást, és ott áll
 * rajta, hogy 220 kcal, akkor a becslésnek nincs mit keresnie: a leírt szám
 * jobb nála. Eddig az app ezt a számot némán eldobta – a „müzliszelet 180
 * kcal" annyit ért, mintha csak a müzliszeletet írták volna be.
 *
 * A másik – gyakoribb – eset, hogy a mondatban nincs is felismerhető étel:
 * „vacsora 650 kcal". Ez tökéletesen értékes naplóbejegyzés, mégis „ezt még
 * nem ismerem" volt a válasz. Kalóriát számolni a lényeg, nem ételt nevesíteni.
 */
public final class Kcal {

    private Kcal() {
    }

    /** Ennél kevesebb nem naplózandó étkezés, ennél több nem egy étkezés. */
    static final int MIN = 5, MAX = 5000;

    /**
     * A szám és a mértékegység: „220 kcal", „650 kalória", „180 kalóriát".
     *
     * A „cal" is átmegy (a doboz néha így írja), de csak szó végén – a
     * lookahead nélkül a „calvados"-ba is beleakadna.
     */
    private static final Pattern NUM = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kcal|kkal|k cal|kalori[a-z]*|cal)(?![a-z])");

    /**
     * Célról szóló mondat – ott a szám nem az, amit MEGETTÜNK.
     *
     * A „még 500 kcal fér bele" és a „napi cél 2000 kcal" ugyanúgy tartalmaz
     * számot és mértékegységet, mint a naplóbejegyzés, csak épp az ellenkezőjét
     * jelenti. Ezeket inkább kihagyjuk: a hamis bejegyzés rosszabb, mint a
     * kimaradó.
     */
    private static final String[] NOT_EATEN = {
            "cel", "celom", "celt", "celja", "celig", "keret", "keretbe", "keretem",
            "limit", "maradt", "marad", "fer", "ferek", "hianyzik", "hianyzo",
            "egettem", "egetem", "elegettem", "elhasznaltam", "alatt", "felett",
            "folott", "szeretnek", "akarok", "legyen", "napi",
    };

    /**
     * A mondatban kimondott kalória, vagy -1.
     *
     * Több szám összeadódik („150 kcal joghurt és 200 kcal banán" = 350), mert
     * aki külön írja őket, az egy étkezés részeit sorolja.
     */
    public static int stated(String q) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        for (String w : NOT_EATEN)
            if (Pattern.compile("(?<![a-z])" + w + "(?![a-z])").matcher(s).find()) return -1;
        double sum = 0;
        Matcher m = NUM.matcher(s);
        while (m.find()) {
            double v;
            try { v = Double.parseDouble(m.group(1).replace(',', '.')); }
            catch (NumberFormatException e) { continue; }
            sum += v;
        }
        int r = (int) Math.round(sum);
        return r >= MIN && r <= MAX ? r : -1;
    }

    /**
     * Az étel neve a kalória nélkül – „vacsora 650 kcal" → „Vacsora".
     *
     * Az eredeti szövegből dolgozik (nem az ékezet nélküliből), hogy a naplóban
     * a felhasználó saját szavai maradjanak meg.
     */
    public static String label(String q) {
        if (q == null) return "Étel";
        String s = q.replaceAll("(?i)\\d+(?:[.,]\\d+)?\\s*(kcal|kkal|kalóri\\w*|kalori\\w*|cal)(?![\\p{L}])", " ");
        // A megmaradt kötőszavak és írásjelek a szám helyén lógva maradnának.
        s = s.replaceAll("(?i)(^|\\s)(kb\\.?|kb|körülbelül|nagyjából|volt|van|kb)(\\s|$)", " ");
        s = s.replaceAll("[\\s,;:.\\-–]+$", "").replaceAll("^[\\s,;:.\\-–]+", "");
        s = s.replaceAll("\\s{2,}", " ").trim();
        if (s.isEmpty()) return "Étel";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * A becsült kalóriák szorzója, hogy az összeg a kimondott számot adja.
     *
     * Csak a kalóriát igazítjuk, a fehérjét nem: a doboz a kalóriát mondta ki,
     * a fehérje-becslés a felismert ételekből továbbra is a jobb tudásunk.
     */
    public static double scale(double estimated, int stated) {
        if (stated <= 0 || estimated <= 0) return 1;
        return stated / estimated;
    }
}
