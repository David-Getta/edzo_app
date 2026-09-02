package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Mennyiség-megadási formák.
 *
 * A mennyiség nem hagyhatja el a tagmondatát – különben az „1 l víz és mandula”
 * egy kiló mandulát jelentene. Egy tagmondat viszont, amiben a szám és a
 * mértékegység mellett SEMMI más nincs, nyilván a szomszédjához tartozik:
 * a „mandula, 30 g” egy étel. Eddig azon múlt, hogy a felhasználó melyik
 * írásjelet választja: kettősponttal működött, vesszővel nem.
 */
public class FoodsQuantityTest {

    private static double grams(String q) {
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
        assertEquals("pontosan egy ételt vártam ebben: " + q, 1, hs.size());
        return hs.get(0).grams;
    }

    @Test public void wholeAndFractionCombineInQuantities() {
        // A „két és fél deci tej" kettője elveszett: fél deci maradt.
        assertEquals(250, grams("két és fél deci tej"), 0.01);
        assertEquals(250, grams("2 és fél dl olaj"), 0.01);
        assertEquals(1500, grams("egy és fél liter víz"), 0.01);
        assertEquals(750, grams("háromnegyed liter tej"), 0.01);
        // A sima tört és a másfél nem romolhatott el.
        assertEquals(150, grams("másfél deci tej"), 0.01);
        assertEquals(500, grams("fél kiló kenyér"), 0.01);
    }

    @Test public void compoundSpelledNumbersWork() {
        // A „negyvenöt gramm", „huszonöt dkg", „ötven gramm" eddig ismeretlen
        // számnév volt, és a tipikus adagra esett vissza.
        assertEquals(45, grams("negyvenöt gramm sajt"), 0.01);
        assertEquals(250, grams("huszonöt dkg liszt"), 0.01);
        assertEquals(50, grams("ötven gramm rizs"), 0.01);
        assertEquals(80, grams("nyolcvan gramm zab"), 0.01);
        assertEquals(320, grams("harminckét dkg sajt"), 0.01);
    }

    @Test public void quarterAndWrittenOutDekaWork() {
        // A „negyed kiló sajt" 30 grammos adag lett: a „negyed" nem volt
        // számnév. A kiírt „deka" pedig nem volt mértékegység (a „dkg" igen).
        assertEquals(250, grams("negyed kiló sajt"), 0.01);
        assertEquals(250, grams("negyed liter tejföl"), 0.01);
        assertEquals(750, grams("háromnegyed liter tej"), 0.01);
        assertEquals(100, grams("10 deka párizsi"), 0.01);
        assertEquals(50, grams("5 deka sajt"), 0.01);
        // A szomszédok nem sérülnek: a „négy" és a „fél" marad, ami volt.
        assertEquals(4 * 55, grams("négy tojás"), 0.01);
        assertEquals(500, grams("fél kiló kenyér"), 0.01);
        assertEquals(200, grams("20 dkg sajt"), 0.01);
    }

    @Test public void aQuantityOnlyClauseBelongsToTheFoodNextToIt() {
        assertEquals(30, grams("mandula, 30 g"), 0.01);
        assertEquals(30, grams("egy marék mandula, kb 30 g"), 0.01);
        assertEquals(30, grams("mandula kb. 30 g"), 0.01);
        assertEquals(30, grams("30 g, mandula"), 0.01);
        // Ezek eddig is működtek – nem romolhattak el.
        assertEquals(30, grams("mandula 30 g"), 0.01);
        assertEquals(30, grams("mandula: 30 g"), 0.01);
        assertEquals(30, grams("mandula (30 g)"), 0.01);
    }

    @Test public void aClauseWithItsOwnWordsKeepsItsQuantity() {
        // Az ismeretlen szóhoz írt mennyiség az övé: az 1 liter nem a
        // mandulánál landol. (A víz ma már ismert tétel, ezért itt egy
        // tényleg ismeretlen szó áll.)
        assertEquals(0, grams("1 l akármi és mandula"), 0.01);
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), "1 l akármi és 30 g mandula");
        assertEquals(1, hs.size());
        assertEquals(30, hs.get(0).grams, 0.01);
    }

    @Test public void spelledOutNumbersWorkBeforeAUnit() {
        // „fél liter tej” eddig a tipikus adagra esett vissza.
        assertEquals(500, grams("fél liter tej"), 0.01);
        assertEquals(500, grams("fél kiló alma"), 0.01);
        assertEquals(200, grams("két deci tej"), 0.01);
        assertEquals(150, grams("másfél dl tej"), 0.01);
        assertEquals(1000, grams("egy kiló csirkemell"), 0.01);
    }

    @Test public void spelledOutNumbersStillCountPiecesWithoutAUnit() {
        // Mértékegység nélkül a számnév darabszám marad – és pontosan egyszer
        // számít, nem kétszer.
        assertEquals(2 * 55, grams("két tojás"), 0.01);
        assertEquals(6 * 55, grams("hat tojás"), 0.01);
        assertEquals(0.5 * 150, grams("fél alma"), 0.01);
        assertEquals(3 * 55, grams("3 tojás"), 0.01);
    }

    @Test public void aWordThatMerelyStartsWithANumberWordIsNotANumber() {
        // A „felvágott” nem fél valamiből: egy étel, mennyiség nélkül.
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), "felvágott");
        assertEquals(1, hs.size());
        assertEquals("Párizsi / felvágott", hs.get(0).food.name);
        assertEquals(0, hs.get(0).grams, 0.01);
    }

    @Test public void severalFoodsKeepTheirOwnQuantities() {
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL),
                "csirkemell 200 g, rizs 150 g");
        assertEquals(2, hs.size());
        assertEquals(200, hs.get(0).grams, 0.01);
        assertEquals(150, hs.get(1).grams, 0.01);

        // A lezáró mennyiség az előtte állóhoz tartozik, nem az elsőhöz.
        List<Foods.Hit> two = Foods.parse(Arrays.asList(Foods.ALL), "alma, banán, 30 g");
        assertEquals(2, two.size());
        assertEquals(0, two.get(0).grams, 0.01);
        assertEquals(30, two.get(1).grams, 0.01);
    }

    @Test public void theHundredsAreNumbersToo() {
        // A konyhában a százas adag a leggyakoribb, és pont ez maradt ki:
        // „száz gramm rizs" helyett a tipikus adag ment a naplóba.
        assertEquals(100.0, grams("száz gramm rizs"), 0.001);
        assertEquals(150.0, grams("százötven gramm csirkemell"), 0.001);
        assertEquals(120.0, grams("százhúsz gramm rizs"), 0.001);
        assertEquals(200.0, grams("kétszáz gramm rizs"), 0.001);
        assertEquals(250.0, grams("kétszázötven gramm tészta"), 0.001);
        assertEquals(300.0, grams("háromszáz gramm burgonya"), 0.001);
    }

    @Test public void theDrinkMeasuresHaveRealVolumes() {
        // Az üveg, a kancsó és a korty nem egy pohár: ezekből lesz a napi
        // vízcél, ezért a tévedés a haladássávon is látszik.
        assertEquals(500, grams("ittam egy üveg vizet"), 0.01);
        assertEquals(1000, grams("ittam egy kancsó vizet"), 0.01);
        assertEquals(40, grams("ittam egy korty vizet"), 0.01);
        assertEquals(80, grams("két korty vizet"), 0.01);
        // A pohár marad a tipikus adag.
        assertEquals(250, grams("ittam egy pohár vizet"), 0.01);
        assertEquals(2000, grams("ittam 8 pohár vizet"), 0.01);
    }

    @Test public void theCountMayFollowTheFoodName() {
        // Bevásárlólista-szórend: „banán 2 db". Legalább olyan gyakori, mint a
        // fordítottja – eddig mégis egyetlen adag lett belőle.
        assertEquals(240, grams("banán 2 db"), 0.01);
        assertEquals(240, grams("banán (2 db)"), 0.01);
        assertEquals(240, grams("2 db banán"), 0.01);
        assertEquals(165, grams("tojás (3 db)"), 0.01);
        assertEquals(70, grams("kenyér (2 szelet)"), 0.01);
        // Gramm mindkét irányban megy, ezen nem változtattunk.
        assertEquals(150, grams("csirkemell (150 g)"), 0.01);
        assertEquals(200, grams("rizs (200 g)"), 0.01);
    }

    @Test public void aCountAfterTheNameNeedsToBelongToIt() {
        // Mérőszó nélküli szám nem darabszám: a „banán 2" bármi lehet.
        assertEquals(0, grams("banán 2"), 0.01);
        // Közbeékelt SZÓ elszakítja: a „banán és rizs 200 g" grammja a rizsé.
        List<Foods.Hit> two = Foods.parse(Arrays.asList(Foods.ALL), "banán és rizs 200 g");
        assertEquals(2, two.size());
        assertEquals(0, two.get(0).grams, 0.01);
        assertEquals(200, two.get(1).grams, 0.01);
    }

    @Test public void wholeAndEntireAreJustAdjectives() {
        // Az „egy EGÉSZ tábla csoki" egy tábla (100 g), nem egy adag (25 g).
        // A jelző elszakította a számot a mérőszótól – pont annál a mondatnál,
        // amit akkor ír le az ember, amikor sokat evett.
        assertEquals(100, grams("egy egész tábla csoki"), 0.01);
        assertEquals(100, grams("egy tábla csoki"), 0.01);
        assertEquals(50, grams("fél tábla csoki"), 0.01);
        assertEquals(400, grams("egy teljes adag gulyás"), 0.01);
        // A jelző nélküli alakok változatlanok.
        assertEquals(150, grams("egy nagy alma"), 0.01);
        assertEquals(110, grams("két egész tojás"), 0.01);
    }

    @Test public void twoFoodsNeverSwapTheirQuantities() {
        // Generatív őrszem, ugyanaz, mint az edzés- és a súlyzós oldalon: négy
        // étel minden párosítása, négy szórendben. A mennyiség nem
        // vándorolhat át a szomszéd ételhez – az elcsúszott gramm csendben
        // rossz kalóriát ír a napi összegbe.
        String[][] foods = {{"csirkemell", "Csirkemell (sült/grill)"},
                {"rizs", "Rizs (főtt)"}, {"sajt", "Sajt (trappista)"}, {"túró", "Túró"}};
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        StringBuilder bad = new StringBuilder();
        for (String[] x : foods)
            for (String[] y : foods) {
                if (x[1].equals(y[1])) continue;
                String[] forms = {
                        "150 g " + x[0] + ", 200 g " + y[0],
                        x[0] + " 150 g, " + y[0] + " 200 g",
                        "150 g " + x[0] + " és 200 g " + y[0],
                        x[0] + " 150 g és " + y[0] + " 200 g"};
                for (String q : forms) {
                    List<Foods.Hit> h = Foods.parse(all, q);
                    if (h.size() != 2 || !h.get(0).food.name.equals(x[1])
                            || !h.get(1).food.name.equals(y[1])
                            || Math.abs(h.get(0).grams - 150) > 0.001
                            || Math.abs(h.get(1).grams - 200) > 0.001) {
                        bad.append("\n  ").append(q).append(" -> ");
                        for (Foods.Hit i : h)
                            bad.append(i.food.name).append("/").append(i.grams).append(" ");
                    }
                }
            }
        assertEquals("elcsúszott a mennyiség:" + bad, 0, bad.length());
    }
    /**
     * A negyed valódi mennyiség.
     *
     * A darabszámos ág fél alatt nem számolt: a „negyed pizza" némán kiesett,
     * és az EGÉSZ adag ment a naplóba – négyszer annyi. A fél és a
     * háromnegyed közben végig működött, ezért a hiba nem tűnt fel.
     */
    @Test public void aQuarterCounts() {
        assertEquals(75, grams("negyed pizza"), 0.01);
        assertEquals(50, grams("negyed adag rizs"), 0.01);
        assertEquals(25, grams("negyed tábla csoki"), 0.01);
        // A fél és a háromnegyed nem romolhatott el.
        assertEquals(150, grams("fél pizza"), 0.01);
        assertEquals(225, grams("háromnegyed pizza"), 0.01);
    }

    /** A „dupla adag" két adag, a „tripla" három. */
    @Test public void doubleAndTripleAreNumbers() {
        assertEquals(400, grams("dupla adag rizs"), 0.01);
        assertEquals(600, grams("tripla adag rizs"), 0.01);
        assertEquals(200, grams("adag rizs"), 0.01);
    }

    /**
     * Az egész és a tört a DARABSZÁMOS ágon is egy szám.
     *
     * A mértékegységes ág ezt már értette („két és fél deci"), a darabszámos
     * nem: a kettes és a fél két külön számként került a listába, és a fél ért
     * oda előbb – a két és fél szeletből fél szelet lett, vagyis ötödannyi.
     */
    @Test public void wholeAndFractionCombineInPieceCounts() {
        assertEquals(87.5, grams("két és fél szelet kenyér"), 0.01);
        assertEquals(87.5, grams("2,5 szelet kenyér"), 0.01);
        assertEquals(350, grams("három és fél szelet pizza"), 0.01);
    }

    /**
     * Mérőszó szám nélkül: a „tábla csoki" egy tábla.
     *
     * Enélkül a tipikus adag ment be – csokinál huszonöt gramm száz helyett,
     * vagyis negyedannyi, mint amit az ember megevett.
     */
    @Test public void aMeasureWordWithoutANumberMeansOne() {
        assertEquals(100, grams("tábla csoki"), 0.01);
        assertEquals(100, grams("tábla étcsoki"), 0.01);
        assertEquals(35, grams("szelet kenyér"), 0.01);
        assertEquals(30, grams("marék dió"), 0.01);
        // A számos alakok változatlanok.
        assertEquals(50, grams("fél tábla csoki"), 0.01);
        assertEquals(70, grams("két szelet kenyér"), 0.01);
    }
    /**
     * „2-3 szelet kenyér": a tartomány KÖZEPE a becslés.
     *
     * Eddig a nagyobbik nyert – az állt közelebb az ételhez –, vagyis a
     * bizonytalanul megadott mennyiség rendszeresen felfelé csúszott. Aki
     * naponta többször ír be tartományt, annak ez a napi összegen is látszik.
     */
    @Test public void aRangeMeansItsMiddle() {
        assertEquals(87.5, grams("2-3 szelet kenyér"), 0.01);
        assertEquals(375, grams("2-3 alma"), 0.01);
        assertEquals(82.5, grams("1-2 tojás"), 0.01);
        // Az „1-1" osztó alak nem tartomány – egy darab marad.
        assertEquals(150, grams("1-1 alma"), 0.01);
        // A sima darabszám nem változott.
        assertEquals(70, grams("2 szelet kenyér"), 0.01);
        assertEquals(105, grams("3 szelet kenyér"), 0.01);
    }

    /** Ugyanez, ha a felső tag viseli a mértékegységet. */
    @Test public void aRangeWithAUnitAlsoMeansItsMiddle() {
        assertEquals(35, grams("3-4 dkg sajt"), 0.01);
        assertEquals(125, grams("100-150 g rizs"), 0.01);
        assertEquals(250, grams("2-3 dl tej"), 0.01);
        // Háromszorosnál nagyobb ugrás nem tartomány, hanem két külön adat –
        // a harminc darab pedig életszerűtlen, így mennyiség nélkül marad
        // (a tipikus adaggal megy tovább).
        assertEquals(0, grams("2-30 alma"), 0.01);
        // Az egyszerű mennyiségek nem változtak.
        assertEquals(150, grams("150 g csirkemell"), 0.01);
        assertEquals(500, grams("fél liter tej"), 0.01);
    }
    /**
     * A birtokos „fele" is fél – a név előtt és a név UTÁN is.
     *
     * A puszta „fél" szótő ezt nem fogta, mert betű követi. Az „az alma fele"
     * és a „pizza fele" így EGÉSZ adagként ment be: kétszer annyi.
     */
    @Test public void thePossessiveHalfIsAlsoHalf() {
        assertEquals(100, grams("a fele adag rizs"), 0.01);
        assertEquals(75, grams("az alma fele"), 0.01);
        assertEquals(150, grams("a pizza fele"), 0.01);
        // A sima fél nem romolhatott el.
        assertEquals(100, grams("fél adag rizs"), 0.01);
        assertEquals(150, grams("fél pizza"), 0.01);
        // A név utáni EGÉSZ szám továbbra sem adagszorzó (mértékegység nélkül
        // nem tudjuk, mit jelent), tehát a tipikus adaggal megy tovább.
        assertEquals(0, grams("csirkemell 150"), 0.01);
        // A tört a saját tagmondatát is zárhatja.
        java.util.List<Foods.Hit> h = Foods.parse(java.util.Arrays.asList(Foods.ALL),
                "az alma fele és egy szelet kenyér");
        assertEquals(2, h.size());
        assertEquals(75, h.get(0).grams, 0.01);
        assertEquals(35, h.get(1).grams, 0.01);
    }

    /**
     * A „szem" darabszó: „tíz szem mandula”, „öt szem szőlő”.
     *
     * Magyarul a szemes ételeket így mondjuk – az app viszont nem ismerte a
     * szót, így a szám elveszett, és a szokásos adag ment be: tíz mandula
     * helyett egy egész marék, háromszoros kalóriával.
     */
    @Test public void aPieceIsCountedWhenSaidAsSzem() {
        assertEquals(10, grams("10 szem mandula"), 0.01);
        assertEquals(20, grams("20 szem mandula"), 0.01);
        assertEquals(25, grams("5 szem szőlő"), 0.01);
        assertEquals(15, grams("3 szem dió"), 0.01);
        // Szám nélkül továbbra is a szokásos adag (0 = nincs kimondva).
        assertEquals(0, grams("mandula"), 0.01);
        // A marék viszont kimondott mennyiség: egy marék harminc gramm.
        assertEquals(30, grams("egy marék mandula"), 0.01);
        // A kimondott gramm erősebb marad.
        assertEquals(30, grams("30 g mandula"), 0.01);
        // A koktélparadicsom szeme hatoda a nagyénak – és nem koktél-ital.
        assertEquals(200, grams("koktélparadicsom 10 szem"), 0.01);
        assertEquals("Koktélparadicsom",
                Foods.parse(java.util.Arrays.asList(Foods.ALL),
                        "koktélparadicsom 10 szem").get(0).food.name);
        // A ringló szilvaként számít.
        assertEquals("Szilva", Foods.parse(java.util.Arrays.asList(Foods.ALL),
                "két ringló").get(0).food.name);
    }

    /** A kenyér KARÉJ, a virsli és a kolbász SZÁL – mindkettő darabszó. */
    @Test public void breadIsSlicedAndSausageComesInLinks() {
        assertEquals(35, grams("egy karéj kenyér"), 0.01);
        assertEquals(70, grams("2 karéj kenyér"), 0.01);
        assertEquals(100, grams("2 szál virsli"), 0.01);
        assertEquals(240, grams("3 szál kolbász"), 0.01);
    }

    /**
     * „…de csak a felét ettem meg”: a hátravetett tört az étkezésre vonatkozik.
     *
     * Eddig a teljes adag ment a naplóba – pont a duplája annak, amit az
     * ember mondott. A tükörkép is működik: amit otthagyott, azt NEM ette meg.
     */
    @Test public void trailingFractionScalesTheMeal() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(150, Foods.parse(all, "egy pizza, de csak a felét ettem meg").get(0).grams, 0.5);
        assertEquals(200, Foods.parse(all, "a gulyás felét ettem meg").get(0).grams, 0.5);
        assertEquals(125, Foods.parse(all, "egy hamburger, a felét otthagytam").get(0).grams, 0.5);
        // Két ételnél nem találgatunk: nem tudni, melyikre gondolt.
        java.util.List<Foods.Hit> two = Foods.parse(all, "pizza és kóla, a felét ettem meg");
        for (Foods.Hit h : two)
            assertEquals("két ételnél nem skálázunk: " + h.food.name, 0, h.grams, 0.001);
        // Az elöl álló tört marad a mennyiség-felismerőé.
        assertEquals(150, Foods.parse(all, "fél pizza").get(0).grams, 0.5);
        // Ige nélkül is egyértelmű, ha a „csak" ott van: a kimondott grammot
        // is felezi. Enélkül a teljes adag ment a naplóba.
        assertEquals(50, Foods.parse(all, "100 g rizs, de csak a felét").get(0).grams, 0.5);
        assertEquals(75, Foods.parse(all, "egy pizza, csak a negyedét").get(0).grams, 0.5);
        // A puszta „a felét" kevés: abból nem derül ki, megette vagy meghagyta.
        assertEquals(300, Foods.parse(all, "egy pizza, a felét").get(0).grams, 0.5);
    }

    /** A kulacs fél liter – és a „fél kulacs" a fele. */
    @Test public void aBottleIsHalfALiter() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(500, Foods.parse(all, "egy kulacs víz").get(0).grams, 0.5);
        assertEquals(250, Foods.parse(all, "fél kulacs víz").get(0).grams, 0.5);
        // Az „egy shaker turmix" egy ital, nem kettő.
        assertEquals(1, Foods.parse(all, "egy shaker turmix").size());
    }

    /**
     * A tápérték-sor grammja nem az étel súlya.
     *
     * A dobozról vagy egy másik appból bemásolt sor így néz ki: „Protein
     * turmix 1 adag – 120 kcal 24 g fehérje". A huszonnégy gramm a FEHÉRJE,
     * mégis a turmix adagja lett belőle – huszonnégy gramm turmix, ötödannyi,
     * mint a valóság.
     *
     * A szabály óvatos: csak akkor él, ha kalória is ki van írva a mondatban.
     * Enélkül a „150 gramm protein turmix" is tápérték-sornak látszana, pedig
     * ott a protein a NÉV része.
     */
    @Test public void aNutritionLineGramIsNotTheFoodWeight() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        java.util.List<Foods.Hit> h = Foods.parse(all,
                "Protein turmix 1 adag - 120 kcal 24 g fehérje");
        assertEquals(1, h.size());
        assertEquals(300.0, h.get(0).grams, 0.01);
        // Ahol a gramm tényleg az ételé, ott marad.
        assertEquals(150.0, Foods.parse(all, "Csirkemell 150 g - 248 kcal, 46 g fehérje")
                .get(0).grams, 0.01);
        assertEquals(150.0, Foods.parse(all, "150 gramm protein turmix").get(0).grams, 0.01);
        assertEquals(250.0, Foods.parse(all, "250 ml protein turmix").get(0).grams, 0.01);
    }

    /**
     * A birtokos tört: „a pizza negyede", „a szendvics harmada".
     *
     * A FELE régóta ment, a többi tört nem: a „pizza negyede" egész pizzának
     * számított – négyszerese annak, amit megevett. A negyedév és a
     * harmadik viszont nem tört.
     */
    @Test public void possessiveFractionsAreUnderstood() {
        assertEquals(75, grams("a pizza negyede"), 0.01);
        assertEquals(150, grams("a pizza fele"), 0.01);
        assertEquals(225, grams("a pizza háromnegyede"), 0.01);
        assertEquals(51, grams("a szendvics harmada"), 0.5);
        // Ami nem tört, az nem is lesz az: a negyedév nem mennyiség, tehát
        // marad a tipikus adag (a nulla itt azt jelenti: nincs kimondva).
        assertEquals(0, grams("negyedévente pizza"), 0.01);
        assertEquals(100, grams("harmadik szelet pizza"), 0.01);
    }

    /**
     * A kanál csak a kencéknél egy adag.
     *
     * A méz, a mogyoróvaj és a tejföl adagja eleve kanálnyi – a nagyobb adagú
     * ételeknél viszont a kanál a KISEBB mérték. A „3 evőkanál zabpehely"
     * eddig három ADAGOT jelentett, vagyis százötven grammot: ötszáz kalória
     * egy százas helyett.
     */
    @Test public void aSpoonOfOatsIsNotThreePortions() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(45.0, Foods.parse(all, "3 evőkanál zabpehely").get(0).grams, 0.01);
        assertEquals(30.0, Foods.parse(all, "két kanál rizs").get(0).grams, 0.01);
        // A kencéknél marad az adag: egy kanál méz pont egy adagnyi.
        assertEquals(20.0, Foods.parse(all, "egy kanál méz").get(0).grams, 0.01);
        // A tányér és a marék sem változik.
        assertEquals(30.0, Foods.parse(all, "egy marék dió").get(0).grams, 0.01);
    }

    /**
     * Az ige a szám mögött is állhat.
     *
     * A „sütit sütöttem, kettőt megettem" ugyanaz a mondat, mint a
     * „megettem kettőt", csak fordított szórenddel – eddig csak az elöl álló
     * igés alak működött, és egy süti ment be kettő helyett.
     */
    @Test public void theVerbMayFollowTheCount() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(200.0, Foods.parse(all, "sütit sütöttem, kettőt megettem")
                .get(0).grams, 0.01);
        assertEquals(180.0, Foods.parse(all, "palacsintát sütöttem, hármat "
                + "ettem meg").get(0).grams, 0.01);
    }

    /** A „néhány szem" kis maréknyi, nem egyetlen szem. */
    @Test public void aFewPiecesAreNotOnePiece() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(25.0, Foods.parse(all, "esti nass: néhány szem szőlő")
                .get(0).grams, 0.01);
        assertEquals(25.0, Foods.parse(all, "pár szem szőlő").get(0).grams, 0.01);
    }

    /**
     * A mennyiség tagmondata nem kötelezően zárja a mondatot.
     *
     * Az „este pizza volt, én 4 szeletet ettem, plusz egy sör" négy szelete
     * eddig egyetlen adaggá zsugorodott: a mennyiséget csak a mondat végén
     * kereste a beolvasó, itt pedig a sör tagmondata állt mögötte. Az „én"
     * kimondása külön is elrontotta – a közös tálból evő ember pedig
     * rendszerint kiteszi az alanyt.
     */
    @Test public void aTrailingClauseDoesNotHideTheCount() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all,
                "este pizza volt, én 4 szeletet ettem, plusz egy sör");
        assertEquals(400.0, hits.get(0).grams, 0.01);
        // A mögötte álló tagmondat nem veszhet el a mennyiséggel együtt.
        assertEquals(2, hits.size());
        // Az alany önmagában sem üti el a számot.
        assertEquals(400.0, Foods.parse(all, "este pizza volt, én 4 szeletet "
                + "ettem").get(0).grams, 0.01);
        // A puszta darabszám ugyanígy viselkedik.
        assertEquals(360.0, Foods.parse(all, "sütöttem palacsintát, én hatot "
                + "ettem meg, a többit a gyerekek").get(0).grams, 0.01);
    }

    /**
     * A tiltó szám az étel saját tagmondatában számít.
     *
     * Az „este 8 után már nem ettem semmit, csak vizet ittam, kb 1,5 litert"
     * nyolcasa a másik tagmondatban áll, mégis letiltotta a másfél litert –
     * negyed liter víz ment be másfél helyett. A liter ráadásul nem is
     * szerepelt a mértékegységek közt.
     */
    @Test public void aNumberInAnotherClauseDoesNotBlockTheAmount() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(1500.0, Foods.parse(all, "este 8 után már nem ettem "
                + "semmit, csak vizet ittam, kb 1,5 litert").get(0).grams, 0.01);
        assertEquals(1500.0, Foods.parse(all, "csak vizet ittam, kb 1,5 "
                + "litert").get(0).grams, 0.01);
    }

    /**
     * Több étel közül a legközelebbi kapja a mennyiséget.
     *
     * Az „ebédre levest ettem, utána palacsintát, 3 db-ot" három
     * palacsintája egyetlen adagra zsugorodott: a leves miatt a mondat
     * „többértelműnek" számított, és a szám sehova nem kapcsolódott.
     */
    @Test public void theNearestFoodTakesTheAmount() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all, "ebédre levest ettem, "
                + "utána palacsintát, 3 db-ot");
        double palacsinta = 0;
        for (Foods.Hit h : hits)
            if (h.food.name.startsWith("Palacsinta")) palacsinta = h.grams;
        assertEquals(180.0, palacsinta, 0.01);
    }

    /**
     * A ráadás hozzáadódik.
     *
     * Az „ebédre 2 szelet pizzát ettem, este még egyet" HÁROM szelet, de
     * kettő ment be: a záró tagmondat darabszáma nyomtalanul eltűnt. A
     * magyar így toldja meg a mennyiséget.
     */
    @Test public void aSecondHelpingIsAddedToTheFirst() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(300.0, Foods.parse(all, "ebédre 2 szelet pizzát ettem, "
                + "este még egyet").get(0).grams, 0.01);
        assertEquals(165.0, Foods.parse(all, "reggel 1 tojást ettem, délben "
                + "még kettőt").get(0).grams, 0.01);
        // A ráadás MÁSIK étele nem a mennyiség: ott két tétel van.
        assertEquals(2, Foods.parse(all, "ma 2 tojást ettem, meg 1 kávé").size());
    }

    /**
     * A vitamin nevében a szám nem darabszám.
     *
     * A „reggel D3 vitamin és egy kávé" HÁROM adag étrend-kiegészítőt írt a
     * naplóba: a hármas a betűhöz tapadva is számnak látszott.
     */
    @Test public void aVitaminNumberIsNotACount() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        double supp = -1;
        for (Foods.Hit h : Foods.parse(all, "reggel D3 vitamin és egy kávé"))
            if (h.food.name.contains("kieg"))
                supp = h.grams > 0 ? h.grams : h.food.portion;
        assertEquals(5.0, supp, 0.01);
    }

    /**
     * A „gr" rövidítés mögött nem állhat betű.
     *
     * A „reggel csak egy fél grapefruitot ettem" fél GRAMM grapefruit lett:
     * a rövidítés a gyümölcs nevének elejét kapta el, és a darabszám
     * grammként ment be. A „2 granola" ugyanígy két gramm müzli volt.
     */
    @Test public void theGrAbbreviationNeedsAWordBoundary() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(125.0, Foods.parse(all, "reggel csak egy fél "
                + "grapefruitot ettem").get(0).grams, 0.01);
        assertEquals(500.0, Foods.parse(all, "2 grapefruit").get(0).grams, 0.01);
        // A valódi gramm-rövidítés marad.
        assertEquals(25.0, Foods.parse(all, "25 gr fehérjepor")
                .get(0).grams, 0.01);
    }

    /**
     * A név után álló csésze-szám is darabszám.
     *
     * A „kávéból ma 4 csészével ittam" és a „kávé, 4 csésze" négy
     * csészéje elveszett: nyolcszáz helyett kétszáz milliliter ment a
     * naplóba. Az eszköz- és tárgyragos edény-szavak, a beékelt napszó
     * és a vessző utáni puszta darabszám-tagmondat mind ide tartozik.
     */
    @Test public void aCupCountAfterTheNameCounts() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(800.0, Foods.parse(all, "Kávéból ma 4 csészével "
                + "ittam.").get(0).grams, 0.1);
        assertEquals(800.0, Foods.parse(all, "Kávéból 4 csészét "
                + "ittam.").get(0).grams, 0.1);
        assertEquals(800.0, Foods.parse(all, "Kávé, 4 csésze.")
                .get(0).grams, 0.1);
    }
}
