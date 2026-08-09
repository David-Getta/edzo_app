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
     * Étkezés-szó után álló, mértékegység NÉLKÜLI szám: „…, ebéd 700".
     *
     * A „reggeli" itt is szerepel – étkezés-szóként –, de a minta megköveteli
     * a közvetlenül utána álló számot, így a „reggeli futás 45 perc 520 kcal"
     * nem esik ide.
     */
    private static final Pattern MEAL_NUM = Pattern.compile(
            "(?<![a-z])(?:reggeli|tizorai|ebed|uzsonna|vacsora|nassolas|snack)"
                    + "\\s*:?\\s*(\\d{2,4})(?!\\d)(?![.,]\\d)"
                    + "(?!\\s?(?:kcal|kkal|k cal|kalori|cal|g|gr|gramm|%|perc|km|kg))");

    /**
     * Kilojoule: az EU-s címke ezt írja ELSŐ helyen, és van doboz, amin csak
     * ez szerepel. 4,184 kJ = 1 kcal.
     */
    private static final Pattern NUM_KJ = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kj|kilojoule)(?![a-z])");

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
            // A DEFICIT és a többlet a cél nyelve, nem a bevitelé: az „500
            // kalóriás deficitben vagyok" ötszáz elfogyasztott kalóriaként
            // került a naplóba – a napi keret negyedeként.
            "deficit", "deficitben", "deficittel", "tobbletben", "szufficit",
    };

    /**
     * Csak az ELÉGETETT kalóriánál tiltó szavak.
     *
     * Az elégetett kalóriát csak akkor kérdezzük, ha a mondatban van edzés –
     * de a vegyes mondatban („futottam 45 percet, ebéd 750 kcal") a szám az
     * evésé. Az evés IGÉJE dönt; a napszak-főnév nem, mert a „reggeli futás
     * 45 perc 520 kcal" ugyanúgy reggeli.
     */
    private static final String[] EATEN = {
            "ettem", "megettem", "eszem", "eszunk", "bevittem", "bevitel",
            "elfogyasztottam", "megittam", "ittam",
            // Az ebéd és a vacsora főnévként is étkezés – a „reggeli"
            // szándékosan kimarad, mert jelzőként a napszakot mondja
            // („reggeli futás 45 perc 520 kcal").
            "ebed", "ebedre", "vacsora", "vacsorara", "uzsonna", "tizorai",
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
        return amount(q, NOT_EATEN_P, EATEN_P);
    }

    /**
     * A mondatban kimondott ELÉGETETT kalória, vagy -1.
     *
     * Az óra pontosabban tudja, mint mi: aki leírja, hogy „futás 45 perc 520
     * kcal", annak a számát nem illik a saját becslésünkre cserélni.
     */
    public static int burned(String q) {
        return amount(q, EATEN_P, NOT_EATEN_P);
    }

    /**
     * A tiltó szavak előre lefordítva.
     *
     * Az étkezés-mező minden LEÜTÉSRE újrakérdezi a felismerőt: huszonhat
     * mintát fordítani karakterenként fölösleges munka a telefonon.
     */
    private static final Pattern[] GOAL_P = words(GOAL), NOT_EATEN_P = words(NOT_EATEN),
            EATEN_P = words(EATEN);

    private static Pattern[] words(String[] ws) {
        Pattern[] out = new Pattern[ws.length];
        for (int i = 0; i < ws.length; i++)
            out[i] = Pattern.compile("(?<![a-z])" + ws[i] + "(?![a-z])");
        return out;
    }

    /**
     * A tiltó szót tartalmazó tagmondatok elhagyva.
     *
     * Ha egyetlen tagmondat sincs tiltva, a mondat változatlanul megy tovább –
     * a felsorolások összeadása („reggeli 450, ebéd 700") így nem sérül.
     */
    private static String withoutBlockedClauses(String s, Pattern[] block, Pattern[] want) {
        boolean any = false;
        for (Pattern w : block) if (w.matcher(s).find()) { any = true; break; }
        if (!any) return s;
        // Ha a mondat egyik fele a MÁSIK értelemben beszél, a jelöletlen
        // tagmondat is azé: a „ma megettem 2 tányér levest, összesen 900
        // kcal" kilencszáza az evésé, nem elégetett kalória. Ezért ilyenkor
        // csak azt a tagmondatot tartjuk meg, amelyik a KERESETT értelmet
        // ki is mondja.
        StringBuilder out = new StringBuilder();
        for (String cl : s.split("\\s*[,;]\\s*|\\s+es\\s+|\\s+de\\s+")) {
            boolean bad = false;
            for (Pattern w : block) if (w.matcher(cl).find()) { bad = true; break; }
            if (bad) continue;
            boolean good = false;
            for (Pattern w : want) if (w.matcher(cl).find()) { good = true; break; }
            if (!good) continue;
            out.append(out.length() > 0 ? ", " : "").append(cl);
        }
        return out.toString();
    }

    private static int amount(String q, Pattern[] block, Pattern[] want) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        for (Pattern w : GOAL_P) if (w.matcher(s).find()) return -1;
        // A tiltó szó csak a SAJÁT tagmondatát viszi el. A „ma 2100 kcal-t
        // ettem, elégettem 600-at" mindkét számot kimondja, de az „elégettem"
        // eddig az egész mondatot elnémította: a kétezer-száz sehol nem jelent
        // meg, és a felhasználó nem is tudta meg, hogy elveszett.
        s = withoutBlockedClauses(s, block, want);
        if (s.isEmpty()) return -1;
        double sum = 0;
        Matcher m = NUM.matcher(s);
        while (m.find()) {
            double v;
            try { v = Double.parseDouble(m.group(1).replace(',', '.')); }
            catch (NumberFormatException e) { continue; }
            sum += v;
        }
        // A FELSOROLÁSBAN a mértékegység csak egyszer szerepel: a „reggeli
        // 350 kcal, ebéd 700, vacsora 600" magyarul teljesen világos, eddig
        // mégis csak az első szám került be – a napi bevitel harmada. Csak
        // akkor lép be, ha van legalább egy kiírt kalória, és csak étkezés-szó
        // után álló, mértékegység nélküli szám adódik hozzá.
        if (sum > 0) {
            m = MEAL_NUM.matcher(s);
            while (m.find()) {
                double v;
                try { v = Double.parseDouble(m.group(1)); }
                catch (NumberFormatException e) { continue; }
                if (v >= MIN && v <= MAX) sum += v;
            }
        }
        // Kilojoule csak akkor, ha kalória nincs: a doboz mindkettőt írja, és
        // a kettő ugyanaz az érték kétszer – összeadni dupla ebéd lenne.
        if (sum <= 0) {
            m = NUM_KJ.matcher(s);
            while (m.find()) {
                double v;
                try { v = Double.parseDouble(m.group(1).replace(',', '.')); }
                catch (NumberFormatException e) { continue; }
                sum += v / 4.184;
            }
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
            "(\\d+(?:[.,]\\d+)?)\\s*(?:g|gr|gramm)?\\s*(?<![a-z])(feherje|feherjet|protein|proteint)"
            // A „150 g protein turmix" száz-ötven grammja a TURMIXÉ, nem a
            // fehérjéé: az étel neve folytatódik, tehát nem tápérték-sor.
            + "(?![a-z])(?!\\s?(?:turmix|shake|sejk|por|italpor|szelet|pudding))");
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
        String s = q.replaceAll("(?i)\\d+(?:[.,]\\d+)?\\s*(kcal|kkal|kalóri\\w*|kalori\\w*|cal|kj|kilojoule)(?![\\p{L}])", " ");
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
