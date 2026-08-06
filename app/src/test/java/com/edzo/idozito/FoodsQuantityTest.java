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
}
