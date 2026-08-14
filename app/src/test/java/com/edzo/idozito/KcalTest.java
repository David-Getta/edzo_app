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
}
