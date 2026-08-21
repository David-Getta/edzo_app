package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/**
 * A dobozon írt kalória.
 *
 * Két dolgot kell tudnia: elhinni a kimondott számot, és NEM elhinni azt,
 * ami nem arról szól, amit megettünk („még 500 kcal fér bele ma").
 */
public class KcalTest {

    @Test public void readsTheStatedAmount() {
        assertEquals(650, Kcal.stated("vacsora 650 kcal"));
        assertEquals(220, Kcal.stated("energiaszelet 220 kcal"));
        assertEquals(180, Kcal.stated("müzliszelet 180 kalória"));
        assertEquals(400, Kcal.stated("reggeli 400 kalóriát"));
        assertEquals(320, Kcal.stated("320 kcal"));
        assertEquals(250, Kcal.stated("egy szelet torta 250 Kcal"));
    }

    /** Kiírt szám is: a Hu.digits ugyanúgy elébe dolgozik, mint máshol. */
    @Test public void spelledOutNumbersWork() {
        assertTrue(Kcal.stated("kétszáz kcal") > 0);
    }

    /** Több tétel egy mondatban összeadódik. */
    @Test public void severalAmountsAddUp() {
        assertEquals(350, Kcal.stated("150 kcal joghurt és 200 kcal banán"));
    }

    /** A célról szóló mondat nem étkezés. */
    @Test public void goalTalkIsNotAMeal() {
        assertEquals(-1, Kcal.stated("még 500 kcal fér bele ma"));
        assertEquals(-1, Kcal.stated("napi cél 2000 kcal"));
        assertEquals(-1, Kcal.stated("2000 kcal alatt maradtam"));
        assertEquals(-1, Kcal.stated("a célom 1800 kcal"));
        assertEquals(-1, Kcal.stated("elégettem 400 kcal-t"));
    }

    /**
     * Az elégetett kalória más szavakat tűr, mint a megevett.
     *
     * Az „elégettem 400 kcal-t" étkezésként hamis lenne, edzésként viszont
     * pont az, amit az óra mond. A cél-mondat viszont egyikként sem az.
     */
    @Test public void burnedIsLooserThanEaten() {
        assertEquals(520, Kcal.burned("futás 45 perc 520 kcal"));
        assertEquals(700, Kcal.burned("10 km futás 50 perc alatt 700 kcal"));
        assertEquals(400, Kcal.burned("elégettem 400 kcal-t"));
        assertEquals(-1, Kcal.stated("10 km futás 50 perc alatt 700 kcal"));
        assertEquals(-1, Kcal.burned("napi cél 2000 kcal"));
        assertEquals(-1, Kcal.burned("futás 45 perc"));
    }

    /** Mértékegység nélkül nincs szám, és az életszerűtlen érték sem kell. */
    @Test public void needsTheUnitAndAPlausibleValue() {
        assertEquals(-1, Kcal.stated("vacsora 650"));
        assertEquals(-1, Kcal.stated("150 g csirkemell"));
        assertEquals(-1, Kcal.stated("99999 kcal"));
        assertEquals(-1, Kcal.stated("2 kcal"));
        assertEquals(-1, Kcal.stated(""));
        assertEquals(-1, Kcal.stated(null));
    }

    /** A „cal" csak szó végén – a hosszabb szavakba nem akadhat bele. */
    @Test public void calDoesNotSwallowLongerWords() {
        assertEquals(-1, Kcal.stated("2 calvados"));
        assertEquals(-1, Kcal.stated("1 calzone"));
    }

    /** A név a mondat saját szavaiból marad, szám nélkül. */
    @Test public void labelKeepsTheUsersOwnWords() {
        assertEquals("Vacsora", Kcal.label("vacsora 650 kcal"));
        assertEquals("Müzliszelet", Kcal.label("müzliszelet 180 kalória"));
        assertEquals("Étel", Kcal.label("650 kcal"));
        assertEquals("Étel", Kcal.label(""));
        assertEquals("Étel", Kcal.label(null));
    }

    /** A dobozon a fehérje is ott áll. */
    @Test public void readsTheStatedProtein() {
        assertEquals(12, Kcal.protein("müzliszelet 180 kcal 12 g fehérje"));
        assertEquals(25, Kcal.protein("fehérjeszelet 25 g protein"));
        assertEquals(30, Kcal.protein("vacsora 650 kcal, fehérje: 30 g"));
        assertEquals(20, Kcal.protein("turmix 20 g fehérjét"));
    }

    /**
     * A „tojásfehérje" étel, nem tápérték-sor.
     *
     * A szó ott van benne, a szám is előtte – pont az a csapda, amibe a
     * rövid szótövek szoktak beleesni.
     */
    @Test public void eggWhiteIsNotAProteinDeclaration() {
        assertEquals(-1, Kcal.protein("3 tojásfehérje"));
        assertEquals(-1, Kcal.protein("100 g tojásfehérje"));
        assertEquals("Túró", Kcal.label("túró 250 kcal 40 g fehérje").trim());
    }

    /** Fehérje-szám sincs ott, ahol nem mondták ki. */
    @Test public void noProteinWhereNoneWasStated() {
        assertEquals(-1, Kcal.protein("vacsora 650 kcal"));
        assertEquals(-1, Kcal.protein("150 g csirkemell"));
        assertEquals(-1, Kcal.protein("napi cél 150 g fehérje"));
        assertEquals(-1, Kcal.protein(null));
        for (String q : Examples.MEAL)
            if (!q.contains("fehérje")) assertEquals("fehérjének látszik: " + q, -1, Kcal.protein(q));
    }

    /** A becslést a kimondott összegre igazítjuk. */
    @Test public void scaleHitsTheStatedTotal() {
        assertEquals(2.0, Kcal.scale(100, 200), 0.001);
        assertEquals(0.5, Kcal.scale(400, 200), 0.001);
        assertEquals(1.0, Kcal.scale(0, 200), 0.001);   // nincs mit igazítani
        assertEquals(1.0, Kcal.scale(300, -1), 0.001);
    }

    /** A kimondott kalória étkezésnek számít az útbaigazítónál is. */
    @Test public void routedToTheDiet() {
        assertEquals(Sentence.Kind.MEAL,
                Sentence.of("vacsora 650 kcal", Arrays.asList(Foods.ALL), 1_753_869_600_000L));
    }

    /**
     * A hirdetett edzés-példák egyikében sincs kimondott égetés.
     *
     * Ha volna, a mentés a saját becslésünk helyett egy oda nem illő számot
     * írna a naplóba – csendben, mert a felület ugyanúgy néz ki.
     */
    @Test public void bulkExamplesHaveNoStatedBurn() {
        for (String q : Examples.BULK)
            assertEquals("égetésnek látszik: " + q, -1, Kcal.burned(q));
    }

    /**
     * A megevett kalória nem elégetett kalória.
     *
     * Az elégetett számot csak edzés-mondatban kérdezzük, de a vegyes mondat
     * („futottam 45 percet, ebéd 750 kcal") számát az evés IGÉJE dönti el.
     */
    @Test public void whatWasEatenWasNotBurned() {
        assertEquals(-1, Kcal.burned("ma 2200 kcal-t ettem"));
        assertEquals(-1, Kcal.burned("futottam 45 percet, ebéd 750 kcal"));
        assertEquals(-1, Kcal.burned("megittam egy 250 kcal-s turmixot"));
        // A valódi égetés marad – a „reggeli" jelzőként napszak, nem étkezés.
        assertEquals(520, Kcal.burned("futás 45 perc 520 kcal"));
        assertEquals(520, Kcal.burned("reggeli futás 45 perc 520 kcal"));
    }

    /**
     * A felsorolásban a mértékegység csak egyszer szerepel.
     *
     * A „reggeli 350 kcal, ebéd 700, vacsora 600" magyarul teljesen világos,
     * eddig mégis csak az első szám került be – a napi bevitel harmada.
     */
    @Test public void aMealListSharesTheUnit() {
        assertEquals(1650, Kcal.stated("reggeli 350 kcal, ebéd 700, vacsora 600"));
        assertEquals(1050, Kcal.stated("reggeli 350 kcal, ebéd 700 kcal"));
        // Kiírt kalória nélkül nem találgatunk.
        assertEquals(-1, Kcal.stated("reggeli 350, ebéd 700"));
        // A napszak-jelző nem étkezés: a „reggeli futás 45 perc 520 kcal"
        // ötszázhúsz marad.
        assertEquals(520, Kcal.stated("reggeli futás 45 perc 520 kcal"));
    }

    /** A cél nyelve nem a bevitelé: a deficit szám nem elfogyasztott kalória. */
    @Test public void aDeficitIsNotAMeal() {
        assertEquals(-1, Kcal.stated("500 kalóriás deficitben vagyok"));
        assertEquals(-1, Kcal.stated("ma 300 kcal többletben vagyok"));
    }

    /**
     * A hétköznapi étel-mondatok NEM lesznek kalóriás bejegyzések.
     *
     * A kettő ugyanabban a mezőben találkozik: ha a „150 g csirkemell rizzsel"
     * bármiért kimondott kalóriának látszana, a felismert ételt egy kitalált
     * szám írná felül.
     */
    @Test public void ordinaryMealSentencesAreUntouched() {
        for (String q : Examples.MEAL)
            if (!q.toLowerCase(new java.util.Locale("hu")).contains("kcal")
                    && !q.toLowerCase(new java.util.Locale("hu")).contains("kalór"))
                assertEquals("kalóriás bejegyzésnek látszik: " + q, -1, Kcal.stated(q));
    }

    /**
     * Kilojoule: az EU-s címke ezt írja első helyen. 4,184 kJ = 1 kcal.
     *
     * Ha a mondat kalóriát IS mond, a kJ nem adódik hozzá: a doboz mindkettőt
     * írja, és a kettő ugyanaz az érték kétszer.
     */
    @Test public void kilojoulesConvert() {
        assertEquals(287, Kcal.stated("szelet 1200 kJ"));
        assertEquals(100, Kcal.stated("ital 418 kj"));
        // Mindkettő kiírva: a kalória számít, a kJ nem duplázódik rá.
        assertEquals(287, Kcal.stated("szelet 1200 kJ / 287 kcal"));
        assertEquals("Szelet", Kcal.label("szelet 1200 kJ"));
    }

    /**
     * A tiltó szó csak a saját tagmondatát viszi el.
     *
     * A „ma 2100 kcal-t ettem, elégettem 600-at" mindkét számot kimondja, de
     * az „elégettem" eddig az egész mondatot elnémította: a kétezer-száz sehol
     * nem jelent meg, és a felhasználó nem is tudta meg, hogy elveszett.
     */
    @Test public void theBlockingWordOnlyTakesItsOwnClause() {
        assertEquals(2100, Kcal.stated("ma 2100 kcal-t ettem, elégettem 600-at"));
        assertEquals(-1, Kcal.stated("elégettem 800 kcal-t az edzésen"));
        // Ami eddig is működött, ne változzon: a felsorolás összeadódik, a
        // cél nem bejegyzés, az óra száma pedig elégetett kalória.
        assertEquals(1800, Kcal.stated("reggeli 450, ebéd 700, vacsora 650 kcal"));
        assertEquals(650, Kcal.stated("vacsora 650 kcal"));
        assertEquals(-1, Kcal.stated("a célom napi 2000 kcal"));
        assertEquals(520, Kcal.burned("futás 45 perc 520 kcal"));
    }

    /**
     * A jelöletlen tagmondat a mondat MÁSIK felével tart.
     *
     * A „ma megettem 2 tányér levest, összesen 900 kcal" kilencszáza az
     * evésé, nem elégetett kalória – hiába nincs a záró tagmondatban egyetlen
     * ige sem. A mondat eleje eldönti, miről van szó.
     */
    @Test public void anUnlabelledClauseFollowsTheRestOfTheSentence() {
        assertEquals(900, Kcal.stated(
                "ma megettem 2 tányér levest és egy nagy adag rizst, összesen kb 900 kcal"));
        assertEquals(-1, Kcal.burned(
                "ma megettem 2 tányér levest és egy nagy adag rizst, összesen kb 900 kcal"));
        assertEquals(2100, Kcal.stated("ma 2100 kcal-t ettem, elégettem 600-at"));
        assertEquals(520, Kcal.burned("futás 45 perc 520 kcal"));
    }

    /**
     * A tárgyrag és a cél-tagmondat.
     *
     * A „ma 2200 kcalt ettem" magyarul így hangzik, és eddig egyáltalán nem
     * lett belőle szám. A „napi cél 1800 kcal, ma 1750 kcal lett" második
     * fele pedig valódi bevitel – a cél-szó eddig az egész mondatot elvitte.
     */
    @Test public void theAccusativeAndTheGoalClause() {
        assertEquals(2200, Kcal.stated("ma 2200 kcalt ettem"));
        assertEquals(750, Kcal.burned("elégettem 750 kcalt az edzésen"));
        assertEquals(1750, Kcal.stated("napi cél 1800 kcal, ma 1750 kcal lett"));
        // A puszta cél továbbra sem bejegyzés.
        assertEquals(-1, Kcal.stated("a célom napi 2000 kcal"));
        assertEquals(650, Kcal.stated("vacsora 650 kcal"));
    }

    /**
     * Az óra-export kalóriája elégetett, nem megevett.
     *
     * A „polar: 55 perc, 610 kcal, átlag hr 138" hatszáztízét eddig a napi
     * BEVITELHEZ adtuk – pont az ellenkező előjellel, mint ahogy a mondat
     * érti, és a mondat ráadásul étkezésként is kötött ki. Az evés-ige
     * erősebb: az „edzés után ettem 600 kcal-t" valódi bevitel.
     */
    @Test public void theWatchExportBurnsCaloriesItDoesNotEatThem() {
        assertEquals(-1, Kcal.stated("polar: 55 perc, 610 kcal, átlag hr 138"));
        assertEquals(610, Kcal.burned("polar: 55 perc, 610 kcal, átlag hr 138"));
        assertEquals(-1, Kcal.stated("ma reggel 6 km futás, 32 perc, "
                + "átlagpulzus 152, 420 kcal"));
        // Az evés-ige felülír.
        assertEquals(600, Kcal.stated("edzés után ettem 600 kcal-t"));
    }

    /**
     * A mértékegység a cél tagmondatában is állhat.
     *
     * A magyar egyszer mondja ki az egységet: a „napi cél 1800 kcal, ma 1750
     * lett" ezerhétszázötvene eddig elveszett, mert a kcal a KIDOBOTT
     * tagmondattal ment el. Csak akkor lép be, ha a maradékban pontosan egy
     * életszerű szám áll – találgatni itt sem szabad.
     */
    @Test public void theUnitMayStayInTheGoalClause() {
        assertEquals(1750, Kcal.stated("napi cél 1800 kcal, ma 1750 lett"));
        assertEquals(1750, Kcal.stated("napi cél 1800 kcal, ma összesen 1750"));
        // A kiírt egység változatlanul működik.
        assertEquals(1750, Kcal.stated("napi cél 1800 kcal, ma 1750 kcal lett"));
        // Cél-szó nélkül nincs mit átvinni.
        assertEquals(-1, Kcal.stated("ma 1750 lett"));
    }

    /**
     * Az „egy protein" ital, nem egy gramm fehérje.
     *
     * Az „ittam egy proteint edzés után" egyese az italok DARABSZÁMA – eddig
     * egy gramm fehérjeként bekerült a makró-naplóba. Mértékegység nélkül
     * csak életszerű makró-szám lehet fehérje; grammal kiírva a kis szám is
     * érvényes.
     */
    @Test public void oneProteinShakeIsNotOneGramOfProtein() {
        assertEquals(-1, Kcal.protein("ittam egy proteint edzés után"));
        assertEquals(-1, Kcal.protein("két proteint ittam ma"));
        assertEquals(5, Kcal.protein("5 g fehérje"));
        assertEquals(120, Kcal.protein("120 fehérje ma"));
        assertEquals(140, Kcal.protein("fehérje: 140"));
    }

    /**
     * A napi mérleg két száma két irány.
     *
     * A „napi mérleg: 1900 kcal bevitel, 2400 kcal égetés" első száma a
     * bevitel, a második az égetés – eddig mindkét oldal a 2400-at kapta,
     * mert a „napi" cél-szónak számított, az „égetés" főnévi alakja pedig
     * hiányzott a tiltólistáról. A kimondott irány erősebb a cél szavánál.
     */
    @Test public void theDailyBalanceHasTwoDirections() {
        assertEquals(1900, Kcal.stated("napi mérleg: 1900 kcal bevitel, "
                + "2400 kcal égetés"));
        assertEquals(2400, Kcal.burned("napi mérleg: 1900 kcal bevitel, "
                + "2400 kcal égetés"));
        // A deficit mértéke se nem bevitel, se nem égetés.
        assertEquals(-1, Kcal.stated("kalóriadeficitben vagyok, kb 400 kcal"));
        assertEquals(-1, Kcal.burned("kalóriadeficitben vagyok, kb 400 kcal"));
    }

    /**
     * Az irány-szó utáni nagy szám kcal-egység nélkül is kalória.
     *
     * A „kalóriabevitel: 2100" és a „bevitel 2000, égetés 500, nettó 1500"
     * eddig teljesen elveszett: a szám mellől hiányzott a kcal, a
     * „bevitel" pedig a szó belsejében ült. A jelöletlen nettó kimarad.
     */
    @Test public void theDirectionWordCarriesTheUnit() {
        assertEquals(2100, Kcal.stated("kalóriabevitel: 2100, fehérje 140 g"));
        assertEquals(2000, Kcal.stated("bevitel 2000, égetés 500, nettó 1500"));
        assertEquals(500, Kcal.burned("bevitel 2000, égetés 500, nettó 1500"));
    }
    /**
     * A puszta „kalória" szó utáni szám is bevitel.
     *
     * A „fehérjebevitel rendben, kalória 2200" kétezer-kétszáza némán
     * elveszett – se a bevitel-, se a kcal-minta nem fedte.
     */
    @Test public void aBareCaloriesWordCarriesItsNumber() {
        assertEquals(2200, Kcal.stated("fehérjebevitel rendben, "
                + "kalória 2200"));
    }
    @Test public void theWatchDisplayIsBurnedNotEaten() {
        // „az óra 300 kcal-t mutatott az edzés után" – a kijelzőn látott
        // szám égetés, mégis a napi bevitelhez adódott.
        assertEquals(-1, Kcal.stated("az óra 300 kcal-t mutatott az edzés után"));
        assertEquals(300, Kcal.burned("az óra 300 kcal-t mutatott az edzés után"));
        // Az evés-ige viszont erősebb a kijelzőnél.
        assertEquals(600, Kcal.stated("edzés után ettem 600 kcal-t"));
    }

    @Test public void intakeWordWithMaInBetweenStillCounts() {
        assertEquals(1900, Kcal.stated("kal\u00f3riabevitel ma 1900"));
    }

    @Test public void caloriesThatWentAwayAreBurnedNotEaten() {
        assertEquals(-1, Kcal.stated("fut\u00e1s k\u00f6zben 320 kcal ment el"));
        assertEquals(320, Kcal.burned("fut\u00e1s k\u00f6zben 320 kcal ment el"));
    }

    @Test public void basalMetabolicRateIsNotIntake() {
        assertEquals(-1, Kcal.stated("bmr 1780 kcal"));
    }

    @Test public void theHungarianThousandDotIsNotADecimalPoint() {
        assertEquals(1500, Kcal.stated("1.500 kcal ma"));
        assertEquals(1200, Kcal.stated("ettem 1.200 kcal-t"));
    }

    @Test public void theSpokenKalAbbreviationCounts() {
        assertEquals(2200, Kcal.stated("2.200 kalt ettem"));
        assertEquals(-1, Kcal.stated("vettem 3 kalapot"));
    }

    @Test public void anAchievedMacroGoalIsData() {
        assertEquals(140, Kcal.protein("el\u00e9rtem a feh\u00e9rjec\u00e9lt, 140 g"));
        assertEquals(1900, Kcal.stated("el\u00e9rtem a kal\u00f3riac\u00e9lt, 1900 kcal"));
    }

    @Test public void anUnmetMacroGoalIsStillNotData() {
        assertEquals(-1, Kcal.protein("a feh\u00e9rjec\u00e9lom 140 g"));
        assertEquals(-1, Kcal.stated("napi c\u00e9l 2000 kcal"));
    }

    /**
     * A MARAD\u00c9K nem bevitel: a „m\u00e9g 40 g feh\u00e9rje kell ma" negyvene az,
     * ami HI\u00c1NYZIK a napb\u00f3l – eddig megevett feh\u00e9rjek\u00e9nt ker\u00fclt be.
     */
    @Test
    public void aRemainingProteinTargetIsNotIntake() {
        assertEquals(-1, Kcal.protein("m\u00e9g 40 g feh\u00e9rje kell ma"));
        // Az ar\u00e1ny sem mennyis\u00e9g: az 1,8 g/testsúlykil\u00f3b\u00f3l k\u00e9t gramm lett.
        assertEquals(-1, Kcal.protein("feh\u00e9rje 1,8 g/testsúlykil\u00f3"));
        // A val\u00f3di bevitel marad.
        assertEquals(150, Kcal.protein("150 gramm feh\u00e9rj\u00e9t ettem eddig"));
    }

    /**
     * A MOZG\u00c1S mell\u00e9 \u00edrt kal\u00f3ria \u00e9get\u00e9s, nem bevitel: az „edz\u00e9s 45 perc,
     * 380 kcal" sz\u00e1ma eddig a napi BEVITELHEZ ad\u00f3dott.
     */
    @Test
    public void aWorkoutsCaloriesAreBurnedNotEaten() {
        assertEquals(-1, Kcal.stated("edz\u00e9s 45 perc, 380 kcal"));
        assertEquals(380, Kcal.burned("edz\u00e9s 45 perc, 380 kcal"));
        assertEquals(-1, Kcal.stated("3200 l\u00e9p\u00e9s, 140 kcal"));
        // Ev\u00e9s-ige mellett a bevitel er\u0151sebb.
        assertEquals(600, Kcal.stated("edz\u00e9s ut\u00e1n ettem 600 kcal-t"));
        // Az \u00e9tkez\u00e9s neve is er\u0151sebb a mozg\u00e1s\u00e9n\u00e1l.
        assertEquals(450, Kcal.stated("reggeli 450 kcal, edz\u00e9s 45 perc"));
    }

    /**
     * Az IGEK\u00d6T\u0150S keret-mondat is c\u00e9l: a „bef\u00e9rek m\u00e9g 300 kcal-ba"
     * h\u00e1romsz\u00e1za a MARAD\u00c9K, m\u00e9gis megevett kal\u00f3riak\u00e9nt ker\u00fclt a napl\u00f3ba.
     */
    @Test
    public void aRemainingAllowanceIsNotIntake() {
        assertEquals(-1, Kcal.stated("bele f\u00e9rek m\u00e9g 300 kcal-ba"
                .replace("bele f", "belef")));
        // A val\u00f3di bevitel marad.
        assertEquals(1850, Kcal.stated("ma 1850 kcal-n\u00e1l j\u00e1rok"));
        assertEquals(2200, Kcal.stated("ettem 2200 kcal-t"));
    }

    /**
     * A K\u00c9T IR\u00c1NY egy tagmondatban, elv\u00e1laszt\u00f3 jel n\u00e9lk\u00fcl: a „napi
     * \u00f6sszegz\u00e9s: 2100 kcal bevitel 2600 kcal \u00e9get\u00e9s" MINDK\u00c9T sz\u00e1ma
     * elveszett – se bevitel, se \u00e9get\u00e9s nem ker\u00fclt a napl\u00f3ba, mert a k\u00e9t
     * ir\u00e1ny egyetlen tagmondaton bel\u00fcl kioltotta egym\u00e1st.
     */
    @Test
    public void bothDirectionsSurviveWithoutASeparator() {
        String s = "napi \u00f6sszegz\u00e9s: 2100 kcal bevitel 2600 kcal \u00e9get\u00e9s";
        assertEquals(2100, Kcal.stated(s));
        assertEquals(2600, Kcal.burned(s));
        // A vessz\u0151s alak v\u00e1ltozatlan.
        String c = "bevitel: 2100 kcal, \u00e9get\u00e9s: 2600 kcal";
        assertEquals(2100, Kcal.stated(c));
        assertEquals(2600, Kcal.burned(c));
    }

    /**
     * A TERMI G\u00c9P neve ugyanolyan mozg\u00e1s-sz\u00f3: az „50 perc elliptikus
     * tr\u00e9ner, 430 kcal" n\u00e9gysz\u00e1zharminca EL\u00c9GETETT kal\u00f3ria, m\u00e9gis a napi
     * BEVITELHEZ ad\u00f3dott – egy edz\u00e9sb\u0151l lett n\u00e9gysz\u00e1zharminc megevett
     * kal\u00f3ria, vagyis a m\u00e9rleg mindk\u00e9t oldala rossz ir\u00e1nyba mozdult.
     */
    @Test
    public void aMachineNameMarksTheCaloriesAsBurned() {
        assertEquals(-1, Kcal.stated("50 perc elliptikus tr\u00e9ner, 430 kcal"));
        assertEquals(430, Kcal.burned("50 perc elliptikus tr\u00e9ner, 430 kcal"));
        assertEquals(-1, Kcal.stated("30 perc fut\u00f3pad 320 kcal"));
        assertEquals(-1, Kcal.stated("crossfit wod 25 perc 380 kcal"));
        // Az EV\u00c9S-ige tov\u00e1bbra is er\u0151sebb.
        assertEquals(600, Kcal.stated("edz\u00e9s ut\u00e1n ettem 600 kcal-t"));
        assertEquals(450, Kcal.stated("reggeli 450 kcal"));
    }


    /**
     * A BESZÉLT alakok ugyanazt az étkezést nevezik meg: a „reggeli 400
     * kcal, ebéd 700, vacsi 600, snack 200" vacsorája kimaradt az
     * összegzésből – ezerháromszáz ment be az ezerkilencszázból.
     */
    @Test
    public void colloquialMealNamesCountToo() {
        assertEquals(1900, Kcal.stated("reggeli 400kcal, ebéd 700, "
                + "vacsi 600, snack 200"));
        assertEquals(1700, Kcal.stated("reggelire 400 kcal, ebédre 700, "
                + "vacsorára 600"));
        // A lépésszám nem kalória.
        assertEquals(700, Kcal.stated("reggel 12000 lépés, ebéd 700 kcal"));
    }


    /**
     * A KÜLÖNÍRT igekötő ugyanaz a bevitel, és a mértékegység a MÁSIK
     * tagmondatban is állhat: az „összesen 2100 kcal-t vittem be, és 2600-at
     * égettem el" kétezer-száza ELÉGETETT kalóriaként ment a naplóba, a
     * valódi kétezer-hatszáz meg elveszett mellőle.
     */
    @Test
    public void bothDirectionsSurviveInOneSentence() {
        String q = "Összesen 2100 kcal-t vittem be, és 2600-at égettem el.";
        assertEquals(2100, Kcal.stated(q));
        assertEquals(2600, Kcal.burned(q));
        assertEquals(2100, Kcal.stated("2100 kcal-t vittem be."));
        assertEquals(-1, Kcal.burned("2100 kcal-t vittem be."));
        assertEquals(600, Kcal.burned("Ma 2100 kcal-t ettem, elégettem 600-at."));
    }


    /**
     * A KAJA a bevitel hétköznapi szava: a „reggel mérés: 77,8 kg. Edzés: 45
     * perc kondi. Kaja: 1900 kcal." ezerkilencszáza ELÉGETETT kalóriaként
     * ment a naplóba – az edzés szava miatt.
     */
    @Test
    public void theWordKajaMeansIntake() {
        String q = "Reggel mérés: 77,8 kg. Edzés: 45 perc kondi. Kaja: 1900 kcal.";
        assertEquals(1900, Kcal.stated(q));
        assertEquals(-1, Kcal.burned(q));
        // Az edzés melletti kalória továbbra is égetés.
        assertEquals(520, Kcal.burned("Futás 45 perc, 520 kcal."));
    }

    /**
     * A ragozott fehérje is fehérje.
     *
     * A „ma összesen 1850 kcal-t ettem, 140 g fehérjével" száznegyvene
     * sehova nem került: a minta csak az alanyesetet és a tárgyesetet
     * ismerte, a határozóragos alakot nem.
     */
    @Test
    public void anInflectedProteinWordStillCounts() {
        assertEquals(140.0, Kcal.protein("Ma összesen 1850 kcal-t ettem, "
                + "140 g fehérjével."), 0.01);
        assertEquals(1850, Kcal.stated("Ma összesen 1850 kcal-t ettem, "
                + "140 g fehérjével."));
        // Az étel neve továbbra sem tápérték-sor.
        assertEquals(-1.0, Kcal.protein("150 g protein turmix"), 0.01);
    }

    /**
     * Az edzés tagmondatának kalóriája az elégetett – a többié nem.
     *
     * A „deficitben vagyok, ma 1450 kcal, edzés 500 kcal" mondatban az
     * ezernégyszázötven a BEVITEL, mégis az égetéshez adódott hozzá:
     * ezerkilencszázötven elégetett kalória egy ötszázas edzésből.
     */
    @Test
    public void onlyTheExerciseClauseCountsAsBurned() {
        assertEquals(500, Kcal.burned("Deficitben vagyok, ma 1450 kcal, "
                + "edzés 500 kcal."));
        // Az egyetlen mozgás-tagmondat kalóriája marad égetés.
        assertEquals(520, Kcal.burned("Futás 45 perc, 520 kcal."));
        assertEquals(610, Kcal.burned("Polar: 55 perc, 610 kcal, átlag hr 138."));
        // Az étkezés-felsorolás nem lesz égetés.
        assertEquals(-1, Kcal.burned("Reggeli 350 kcal, ebéd 700, vacsora 600."));
    }

    /**
     * Irány nélkül a kalória bevitel, nem égetés is.
     *
     * A „reggeli 7:30-kor: 2 szelet bacon, 250 kcal körül" kétszázötvene a
     * bevitel MELLETT az égetéshez is hozzáadódott – a napi mérleg mindkét
     * oldala elmozdult egyetlen reggelitől. Égetés csak mozgás-, óra- vagy
     * égetés-szó mellett hihető.
     */
    @Test public void aPlainMealCalorieIsNotAlsoBurned() {
        assertEquals(250, Kcal.stated("Reggeli 7:30-kor: 2 szelet bacon, "
                + "250 kcal körül."));
        assertEquals(-1, Kcal.burned("Reggeli 7:30-kor: 2 szelet bacon, "
                + "250 kcal körül."));
        assertEquals(-1, Kcal.burned("Tízóraira egy proteinjoghurt volt, "
                + "120 kcal."));
        // A valódi égetés marad.
        assertEquals(520, Kcal.burned("futás 45 perc 520 kcal"));
        assertEquals(500, Kcal.burned("bevitel 2000, égetés 500, nettó 1500"));
    }

    /**
     * A küszöb fölött evés is bevitel.
     *
     * A „töltőnapot tartottam, 3000 kcal fölött ettem szándékosan"
     * háromezrese elveszett: a „fölött" tiltószó az egész tagmondatot
     * elvitte, pedig az evés igéje ott áll mellette.
     */
    @Test public void eatingAboveAThresholdIsIntake() {
        assertEquals(3000, Kcal.stated("Ma töltőnapot tartottam, 3000 kcal "
                + "fölött ettem szándékosan."));
        assertEquals(2000, Kcal.stated("2000 kcal alatt ettem ma."));
        // A tempó-adat marad égetés.
        assertEquals(700, Kcal.burned("10 km futás 50 perc alatt 700 kcal"));
    }

    /**
     * Az „evve/elégetve" határozói alak is irány.
     *
     * A „napi mérleg: 1750 kcal evve, 320 elégetve futással" MINDKÉT
     * száma elveszett, ráadásul egy negyvenöt perces futás került be
     * helyettük – az égetés eszközéből.
     */
    @Test public void adverbialEatenAndBurnedCount() {
        assertEquals(1750, Kcal.stated("Napi mérleg: 1750 kcal evve, "
                + "320 elégetve futással."));
        assertEquals(320, Kcal.burned("Napi mérleg: 1750 kcal evve, "
                + "320 elégetve futással."));
    }

}
