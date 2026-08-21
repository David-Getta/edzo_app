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
            // A tárgyrag hozzátapad: a „ma 2200 kcalt ettem" és a „elégettem
            // 750 kcalt" magyarul így hangzik, és eddig egyik sem lett szám.
            // A „kal" a beszélt rövidítés: a „2200 kalt ettem" eddig
            // elveszett. A szó-végi tiltás miatt a „3 kalapot" nem kalória.
            "(\\d+(?:[.,]\\d+)?)\\s*(kcal|kkal|k cal|kalori[a-z]*|cal|kal)"
                    + "(?:-?[oöea]?t)?(?![a-z])");

    /**
     * Étkezés-szó után álló, mértékegység NÉLKÜLI szám: „…, ebéd 700".
     *
     * A „reggeli" itt is szerepel – étkezés-szóként –, de a minta megköveteli
     * a közvetlenül utána álló számot, így a „reggeli futás 45 perc 520 kcal"
     * nem esik ide.
     */
    private static final Pattern MEAL_NUM = Pattern.compile(
            // A BESZÉLT alakok ugyanazt az étkezést nevezik meg: a „reggeli
            // 400 kcal, ebéd 700, vacsi 600, snack 200" vacsorája kimaradt az
            // összegzésből – ezerháromszáz ment be az ezerkilencszázból. A
            // ragozott alak („vacsorára 600") ugyanígy.
            "(?<![a-z])(?:reggeli|tizorai|ebed|ebi|uzsonna|uzsi|vacsora|vacsi|"
                    + "nassolas|nasi|snack|desszert)\\w*"
                    + "\\s*:?\\s*(\\d{2,4})(?!\\d)(?![.,]\\d)"
                    + "(?!\\s?(?:kcal|kkal|k cal|kalori|cal|g|gr|gramm|%|perc|km|kg"
                    + "|lepes|ml|dl|db|darab|szelet))");

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
            // Az IGEKÖTŐS alak ugyanaz a keret-mondat: a „beleférek még
            // 300 kcal-ba" háromszáza a MARADÉK, mégis megevett kalóriaként
            // került a naplóba – a szóhatáros lista a „fér"-t csak magában
            // látta meg.
            "belefer", "beleferek", "belefert", "beleferne", "beleferek meg",
            "szeretnek", "akarok", "legyen", "napi",
            // A DEFICIT és a többlet a cél nyelve, nem a bevitelé: az „500
            // kalóriás deficitben vagyok" ötszáz elfogyasztott kalóriaként
            // került a naplóba – a napi keret negyedeként.
            "deficit", "deficitben", "deficittel", "tobbletben", "szufficit",
            // Az összetett alak kicselezi a szóhatárt: a „kalóriadeficitben
            // vagyok, kb 400 kcal" négyszáza a deficit mértéke, mégis
            // bevitelként ÉS égetésként is beszámoltuk.
            "kaloriadeficit", "kaloriadeficitben", "kaloriadeficittel",
            "kaloriatobblet", "kaloriatobbletben",
            // A BMR az alapanyagcsere, nem a mai bevitel: a „bmr 1780
            // kcal" a napi keret fele-kétharmada, mégis étkezés lett.
            "bmr", "alapanyagcsere", "tdee",
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
            // A KÜLÖNÍRT igekötő ugyanaz a bevitel: az „összesen 2100 kcal-t
            // vittem be, és 2600-at égettem el" kétezer-száza ELÉGETETT
            // kalóriaként ment a naplóba, a valódi kétezer-hatszáz meg
            // elveszett mellőle.
            "vittem be", "vittunk be", "vittem bele",
            // A KAJA a bevitel hétköznapi szava: a „reggel mérés: 77,8 kg.
            // Edzés: 45 perc kondi. Kaja: 1900 kcal." ezerkilencszáza
            // ELÉGETETT kalóriaként ment a naplóba – az edzés szava miatt.
            "kaja", "etkezes", "etel", "bevive",
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
            // A FŐNÉVI alak is égetés: a „napi mérleg: 1900 kcal bevitel,
            // 2400 kcal égetés" második fele eddig a BEVITELHEZ adódott.
            "egetes", "elegetes", "egetett", "elegetett",
            // A „320 kcal ment el" is égetés: az óra kijelzőjének magyar
            // olvasata, mégis bevitelként számolt.
            "ment el", "elhasznalt",
            "alatt", "felett", "folott",
    };

    /**
     * A mondatban kimondott kalória, vagy -1.
     *
     * Több szám összeadódik („150 kcal joghurt és 200 kcal banán" = 350), mert
     * aki külön írja őket, az egy étkezés részeit sorolja.
     */
    public static int stated(String q) {
        // Az ÓRA-EXPORT kalóriája ELÉGETETT, nem megevett: a „polar: 55 perc,
        // 610 kcal, átlag hr 138" hatszáztízét eddig a napi BEVITELHEZ adtuk –
        // pont az ellenkező előjellel, mint ahogy a mondat érti. Ha viszont
        // evés-ige is van a mondatban, az erősebb: az „edzés után ettem
        // 600 kcal-t" valódi bevitel.
        if (fromWatch(q)) return -1;
        return amount(q, NOT_EATEN_P, EATEN_P);
    }

    /** Óra- vagy alkalmazás-export-e a mondat, evés-ige nélkül? */
    private static boolean fromWatch(String q) {
        if (q == null) return false;
        String s = Hu.digits(Foods.norm(q));
        for (Pattern w : EATEN_P) if (w.matcher(s).find()) return false;
        for (String w : new String[]{"polar", "garmin", "suunto", "fitbit",
                "strava", "apple watch", "coros", "aktiv kalori", "atlag hr",
                "atlagpulzus", "atlag pulzus", "elegetett",
                // A MUTATOTT/MÉRT kalória kijelzőről jön: „az óra 300 kcal-t
                // mutatott az edzés után" égetés, mégis bevitel lett belőle.
                "kcal-t mutatott", "kaloriat mutatott", "kcal-t mert",
                "kaloriat mert", "kcal-t irt", "kaloriat irt"})
            if (s.contains(w)) return true;
        // A MOZGÁS mellé írt kalória ugyanígy égetés, márkanév nélkül is: az
        // „edzés 45 perc, 380 kcal" és a „3200 lépés, 140 kcal" száma eddig a
        // napi BEVITELHEZ adódott – egy edzésből lett háromszáznyolcvan
        // megevett kalória, vagyis a mérleg mindkét oldala rossz irányba
        // mozdult. Evés-ige mellett nem él (fent kiszálltunk), és a REGGELI
        // szava is felmenti: az étkezés neve erősebb, mint a mozgásé.
        if (s.contains("reggeli")) return false;
        // A TERMI GÉP neve ugyanolyan mozgás-szó: az „50 perc elliptikus
        // tréner, 430 kcal" négyszázharminca ELÉGETETT kalória, mégis a napi
        // BEVITELHEZ adódott – egy edzésből lett négyszázharminc megevett
        // kalória, vagyis a mérleg mindkét oldala rossz irányba mozdult.
        return sportWordIn(s);
    }

    /** Mozgás-szó a mondatban – az égetés iránya e nélkül nem hihető. */
    private static boolean sportWordIn(String s) {
        return s.matches("(?s).*(?<![a-z])(edzes\\w*|edzettem|futas\\w*|futottam"
                + "|lepes\\w*|lepest|setal\\w*|bringa\\w*|kerekpar\\w*|uszas\\w*"
                + "|usztam|kondi\\w*|jogaztam|turaztam|spinning|kardio\\w*"
                + "|intervall\\w*|tabata"
                + "|elliptikus|crosstrainer|szobabicikli|evezogep|futopad"
                + "|trener|crossfit|wod|emom|amrap|hiit"
                + "|guggolas|fekvenyomas|holtemeles|fekvotamasz"
                + "|gyaloglas|kocogas|falmaszas|boxedzes)(?![a-z]).*");
    }

    /**
     * A mondatban kimondott ELÉGETETT kalória, vagy -1.
     *
     * Az óra pontosabban tudja, mint mi: aki leírja, hogy „futás 45 perc 520
     * kcal", annak a számát nem illik a saját becslésünkre cserélni.
     */
    public static int burned(String q) {
        // IRÁNY nélkül a kalória bevitel: a „reggeli 7:30-kor: 250 kcal
        // körül" kétszázötvene eddig az égetéshez IS hozzáadódott – a napi
        // mérleg mindkét oldala elmozdult egyetlen reggelitől. Égetés csak
        // akkor, ha a mondat mozgást, órát vagy égetés-szót mond.
        if (q != null) {
            String s = Hu.digits(Foods.norm(q));
            boolean cue = sportWordIn(s);
            if (!cue) for (Pattern w : NOT_EATEN_P)
                if (w.matcher(s).find()) { cue = true; break; }
            if (!cue) for (String w : new String[]{"polar", "garmin",
                    "suunto", "fitbit", "strava", "apple watch", "coros",
                    "aktiv kalori", "atlag hr", "atlagpulzus", "kal ment el"})
                if (s.contains(w)) { cue = true; break; }
            if (!cue) return -1;
        }
        return amount(exerciseClausesOnly(q), EATEN_P, NOT_EATEN_P);
    }

    /**
     * Az EDZÉS tagmondatának kalóriája az elégetett – a többié nem.
     *
     * A „deficitben vagyok, ma 1450 kcal, edzés 500 kcal" mondatban az
     * ezernégyszázötven a BEVITEL, mégis az égetéshez adódott hozzá:
     * ezerkilencszázötven elégetett kalória egy ötszázas edzésből. Csak
     * akkor szűkítünk, ha marad kalória a mozgás tagmondatában, és van
     * olyan tagmondat is, amelyik számot mond, de mozgást nem.
     */
    private static String exerciseClausesOnly(String q) {
        if (q == null) return null;
        String s = Hu.digits(Foods.norm(q));
        if (countNums(s) < 2) return q;
        StringBuilder only = new StringBuilder();
        boolean plain = false;
        for (String cl : s.split("\\s*[,;.]\\s*")) {
            if (cl.matches("(?s).*(?<![a-z])(edzes\\w*|edzettem|futas\\w*"
                    + "|futottam|kondi\\w*|uszas\\w*|usztam|bicikli\\w*"
                    + "|kerekpar\\w*|seta\\w*|setaltam|tura\\w*|aktiv"
                    + "|mozgas\\w*|sport\\w*|garmin|polar)(?![a-z]).*"))
                only.append(only.length() > 0 ? ", " : "").append(cl);
            else if (countNums(cl) > 0) plain = true;
        }
        if (!plain || countNums(only.toString()) < 1) return q;
        return only.toString();
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

    /** Hány kiírt kalória-érték van a szövegben. */
    private static int countNums(String s) {
        int n = 0;
        Matcher m = NUM.matcher(s);
        while (m.find()) n++;
        return n;
    }

    private static int amount(String q, Pattern[] block, Pattern[] want) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        // A KALÓRIABEVITEL összetett szava egyben mondja ki az egységet és
        // az irányt: a „kalóriabevitel: 2100" eddig elveszett, mert a szám
        // mellől hiányzott a kcal, a „bevitel" meg a szó belsejében ült. A
        // puszta „bevitel 2000" és „égetés 500" ugyanígy: az irány-szó után
        // álló nagy szám csak kalória lehet.
        // A „ma/tegnap" beékelődhet: a „kalóriabevitel ma 1900" eddig
        // elveszett.
        s = s.replaceAll("kaloriabevitel\\w*\\s?:?\\s?"
                + "(?:(?:ma|tegnap|most|eddig)\\s)?(\\d{3,4})(?!\\d)(?![.,]\\d)",
                "bevitel $1 kcal");
        s = s.replaceAll("(?<![a-z])(bevitel|egetes)\\s?:?\\s?(\\d{3,4})(?!\\d)"
                + "(?![.,]\\d)(?!\\s?kcal)", "$1 $2 kcal");
        // A puszta „kalória" szó utáni szám is bevitel: a „fehérjebevitel
        // rendben, kalória 2200" kétezer-kétszáza eddig némán elveszett.
        s = s.replaceAll("(?<![a-z])kaloria\\w*\\s?:?\\s?(\\d{3,4})(?!\\d)"
                + "(?![.,]\\d)(?!\\s?kcal)", "$1 kcal");
        // A KÉT IRÁNY egy tagmondatban, elválasztó jel nélkül: a „napi
        // összegzés: 2100 kcal bevitel 2600 kcal égetés" MINDKÉT száma
        // elveszett, mert a mondat egyetlen tagmondat volt, és abban a két
        // irány kioltotta egymást – se bevitel, se égetés nem került a
        // naplóba. Az irány-szó a maga elé írt számhoz tartozik, tehát
        // MÖGÉ tesszük a határt.
        s = s.replaceAll("(\\d\\s?kcal\\s+(?:bevitel|egetes|elegetes)\\w*)"
                + "\\s+(?=\\d)", "$1, ");
        // A CÉL is csak a saját tagmondatát viszi el: a „napi cél 1800 kcal,
        // ma 1750 lett" második fele valódi bevitel.
        boolean anyGoal = false;
        for (Pattern w : GOAL_P) if (w.matcher(s).find()) { anyGoal = true; break; }
        // A mértékegység a CÉL tagmondatában állhat, a valódi szám mellett
        // meg már nem: a „napi cél 1800 kcal, ma 1750 lett" ezerhétszázötvene
        // eddig elveszett, mert a kcal a kidobott tagmondattal ment el. A
        // magyar egyszer mondja ki az egységet – a második szám ugyanaz.
        // A mértékegység a MÁSIK tagmondatban is állhat: az „összesen 2100
        // kcal-t vittem be, és 2600-at égettem el" kétezer-hatszáza némán
        // elveszett – a kcal a bevitel tagmondatában maradt, a magyar meg
        // egyszer mondja ki az egységet. (Az „egyetlen jelöletlen szám"
        // feltétel alább úgyis szűkre szabja.)
        boolean unitSeen = NUM.matcher(s).find();
        if (anyGoal) {
            StringBuilder keep = new StringBuilder();
            for (String cl : s.split("\\s*[,;]\\s*")) {
                boolean bad = false;
                for (Pattern w : GOAL_P) if (w.matcher(cl).find()) { bad = true; break; }
                // A kimondott IRÁNY erősebb a cél szavánál: a „napi mérleg:
                // 1900 kcal bevitel" tagmondatában ott a „bevitel" – ez nem
                // terv, hanem beszámoló, hiába kezdődik „napi"-val.
                if (bad) for (Pattern w : want) if (w.matcher(cl).find()) bad = false;
                if (!bad) keep.append(keep.length() > 0 ? ", " : "").append(cl);
            }
            s = keep.toString();
            if (s.isEmpty()) return -1;
            // A cél mellett maradt tagmondat csak akkor adat, ha MEGTÖRTÉNTET
            // mond: a „kalóriadeficitben vagyok, kb 400 kcal" négyszáza a
            // deficit mértéke, nem bevitel és nem égetés – eddig mindkettőnek
            // beszámoltuk. A „ma 1750 lett" viszont beszámoló.
            boolean done = achieved(s) || s.matches(".*(?<![a-z])(lett|volt|ossze\\w*|ma|tegnap"
                    + "|bevitel|bevittem|megettem|ettem|ittam|elegettem|egettem"
                    + "|egetes|reggeli|ebed|vacsora)(?![a-z]).*");
            if (!done) return -1;
        }
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
        // Egyetlen jelöletlen szám a cél-tagmondat után: az a valódi érték.
        if (sum == 0 && unitSeen) {
            java.util.regex.Matcher bare = Pattern
                    .compile("(?<![\\d.,])(\\d{2,4})(?![\\d.,])").matcher(s);
            double only = 0;
            int n = 0;
            while (bare.find()) {
                double v = Double.parseDouble(bare.group(1));
                if (v < MIN || v > MAX) continue;
                only = v; n++;
            }
            if (n == 1) sum = only;
        }
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
            // A RAGOZOTT alak is fehérje: a „ma összesen 1850 kcal-t ettem,
            // 140 g fehérjével" száznegyvene eddig sehova nem került, mert
            // csak az alanyesetet és a tárgyesetet ismertük.
            "(\\d+(?:[.,]\\d+)?)\\s*(g|gr|gramm)?\\s*(?<![a-z])"
            + "(?:feherje|protein)(?:t|vel|val|bol|hez|nel|nal)?"
            // A „150 g protein turmix" száz-ötven grammja a TURMIXÉ, nem a
            // fehérjéé: az étel neve folytatódik, tehát nem tápérték-sor.
            + "(?![a-z])(?!\\s?(?:turmix|shake|sejk|por|italpor|szelet|pudding))");
    private static final Pattern PROT_BEFORE = Pattern.compile(
            // A CÉL szava is a fehérjéé: az „elértem a fehérjecélt, 140 g"
            // száma eddig sehova nem kapcsolódott. (Hogy a nem teljesült cél
            // ne kerüljön be, arról a GOAL-szűrő gondoskodik.)
            "(?<![a-z])(?:feherje|protein)(?:cel\\w*)?(?![a-z])"
                    + "[^0-9]{0,4}(\\d+(?:[.,]\\d+)?)\\s*(g|gr|gramm)?(?![a-z])");

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
        // A TELJESÍTETT cél már adat: az „elértem a fehérjecélt, 140 g"
        // eddig üresen jött vissza – a cél szava elnémította, pedig épp
        // arról szól, hogy megvan.
        if (!achieved(s)) {
            for (Pattern w : GOAL_P) if (w.matcher(s).find()) return -1;
            // Az ÖSSZETETT cél-szó ugyanígy tilt: a „fehérjecélom 140 g"
            // kívánság, nem bevitel – a szóhatáros lista nem látja meg
            // benne a célt.
            if (s.matches(".*(?:feherje|kaloria|kalori|szenhidrat|zsir"
                    + "|lepes|makro)cel\\w*.*")) return -1;
        }
        // A MARADÉK nem bevitel: a „még 40 g fehérje kell ma" negyvene az,
        // ami HIÁNYZIK a napból – eddig megevett fehérjeként került a
        // makró-naplóba, vagyis pont az ellenkezőjeként.
        if (s.matches("(?s).*(?<![a-z])(?:meg|hatra)\\s[^.;]{0,28}?"
                + "(?:kell|kellene|hianyzik|van hatra)(?![a-z]).*")) return -1;
        // Az ARÁNY nem mennyiség: az „1,8 g/testsúlykiló" a napi cél
        // szorzója, nem két gramm fehérje – és a kettő volt belőle.
        if (s.matches("(?s).*\\d\\s?g\\s?/\\s?(?:ttkg|kg|kilo|testsuly|testtomeg).*"))
            return -1;
        double sum = 0;
        // Mértékegység NÉLKÜL csak életszerű makró-szám lehet fehérje: az
        // „ittam egy proteint" egyese az italok DARABSZÁMA, nem egy gramm
        // fehérje – eddig mégis bekerült a makró-naplóba. Grammal kiírva a
        // kis szám is érvényes („5 g fehérje").
        Matcher m = PROT_AFTER.matcher(s);
        while (m.find())
            if (m.group(2) != null || num(m.group(1)) >= 10) sum += num(m.group(1));
        if (sum <= 0) {
            m = PROT_BEFORE.matcher(s);
            while (m.find())
                if (m.group(2) != null || num(m.group(1)) >= 10) sum += num(m.group(1));
        }
        int r = (int) Math.round(sum);
        return r >= MIN_PROT && r <= MAX_PROT ? r : -1;
    }

    /** Teljesítés-ige: a cél szava ilyenkor nem tilt (már megvan). */
    private static boolean achieved(String s) {
        return s.matches(".*(?<![a-z])(elertem|teljesitettem|teljesult|meglett"
                + "|megvan|osszejott|sikerult|megcsinaltam)(?![a-z]).*");
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
