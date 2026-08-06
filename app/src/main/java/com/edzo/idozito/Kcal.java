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
    private static final String[] GOAL = {
            "cel", "celom", "celt", "celja", "celig", "keret", "keretbe", "keretem",
            "limit", "maradt", "marad", "fer", "ferek", "hianyzik", "hianyzo",
            "szeretnek", "akarok", "legyen", "napi",
    };

    /**
     * Csak az ETT kalóriánál tiltó szavak.
     *
     * Az „elégettem 400 kcal-t" nem étkezés – az edzésnél viszont pont az a
     * mondat, amit keresünk. Az „50 perc alatt 700 kcal" ugyanígy: az étrend
     * mezőjében gyanús, az edzés-mondatban hétköznapi.
     */
    private static final String[] NOT_EATEN = {
            "egettem", "egetem", "elegettem", "elegetem", "elhasznaltam",
            "alatt", "felett", "folott",
    };

    /**
     * A mondatban kimondott kalória, vagy -1.
     *
     * Több szám összeadódik („150 kcal joghurt és 200 kcal banán" = 350), mert
     * aki külön írja őket, az egy étkezés részeit sorolja.
     */
    public static int stated(String q) {
        return amount(q, NOT_EATEN_P);
    }

    /**
     * A mondatban kimondott ELÉGETETT kalória, vagy -1.
     *
     * Az óra pontosabban tudja, mint mi: aki leírja, hogy „futás 45 perc 520
     * kcal", annak a számát nem illik a saját becslésünkre cserélni.
     */
    public static int burned(String q) {
        return amount(q, NONE_P);
    }

    /**
     * A tiltó szavak előre lefordítva.
     *
     * Az étkezés-mező minden LEÜTÉSRE újrakérdezi a felismerőt: huszonhat
     * mintát fordítani karakterenként fölösleges munka a telefonon.
     */
    private static final Pattern[] GOAL_P = words(GOAL), NOT_EATEN_P = words(NOT_EATEN),
            NONE_P = new Pattern[0];

    private static Pattern[] words(String[] ws) {
        Pattern[] out = new Pattern[ws.length];
        for (int i = 0; i < ws.length; i++)
            out[i] = Pattern.compile("(?<![a-z])" + ws[i] + "(?![a-z])");
        return out;
    }

    private static int amount(String q, Pattern[] block) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        for (Pattern w : GOAL_P) if (w.matcher(s).find()) return -1;
        for (Pattern w : block) if (w.matcher(s).find()) return -1;
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

    /** A dobozon a fehérje is ott áll – ennél kevesebb/több nem egy étkezésé. */
    static final int MIN_PROT = 1, MAX_PROT = 300;

    /**
     * A szám és a fehérje-szó: „12 g fehérje", „fehérje 12 g", „25 g protein".
     *
     * A lookbehind a „tojásfehérje" miatt kell: abban is ott a szó, de az egy
     * ÉTEL neve, nem a tápérték-táblázat sora.
     */
    private static final Pattern PROT_AFTER = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:g|gr|gramm)?\\s*(?<![a-z])(feherje|feherjet|protein|proteint)(?![a-z])");
    private static final Pattern PROT_BEFORE = Pattern.compile(
            "(?<![a-z])(?:feherje|protein)(?![a-z])\\s*:?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:g|gr|gramm)?(?![a-z])");

    /**
     * A mondatban kimondott fehérje grammban, vagy -1.
     *
     * Aki a dobozról másolja a kalóriát, a fehérjét is onnan másolja – és a
     * fehérje-célnál pont ez a szám számít. A cél-mondatot ugyanúgy kihagyjuk,
     * mint a kalóriánál.
     */
    public static int protein(String q) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        for (Pattern w : GOAL_P) if (w.matcher(s).find()) return -1;
        double sum = 0;
        Matcher m = PROT_AFTER.matcher(s);
        while (m.find()) sum += num(m.group(1));
        if (sum <= 0) {
            m = PROT_BEFORE.matcher(s);
            while (m.find()) sum += num(m.group(1));
        }
        int r = (int) Math.round(sum);
        return r >= MIN_PROT && r <= MAX_PROT ? r : -1;
    }

    private static double num(String s) {
        try { return Double.parseDouble(s.replace(',', '.')); }
        catch (NumberFormatException e) { return 0; }
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
        // A tápérték-sor másik fele is a számokhoz tartozik, nem a névhez.
        s = s.replaceAll("(?i)\\d+(?:[.,]\\d+)?\\s*(g|gr|gramm)?\\s*(?<![\\p{L}])(fehérj\\w*|feherj\\w*|protein\\w*)(?![\\p{L}])", " ");
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
