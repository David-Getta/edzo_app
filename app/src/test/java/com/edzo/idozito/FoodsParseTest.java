package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * A beírt szövegből kiolvasott mennyiségek. Ez működteti a „150 g csirkemell
 * 200 g rizs" típusú bevitelt: minden szám a hozzá legközelebb álló ételhez
 * tartozik, és ahol nincs szám, ott 0 marad (a hívó tölti fel tipikus adaggal
 * vagy a közös gramm elosztásával).
 */
public class FoodsParseTest {

    private static final List<Foods.Food> DB = Arrays.asList(Foods.ALL);

    private static List<Foods.Hit> hits(String q) { return Foods.parse(DB, q); }

    @Test public void gramsBeforeTheFoodAreAssigned() {
        List<Foods.Hit> h = hits("150 g csirkemell 200 g rizs");
        assertEquals(2, h.size());
        assertEquals("Csirkemell (sült/grill)", h.get(0).food.name);
        assertEquals(150.0, h.get(0).grams, 0.001);
        assertEquals("Rizs (főtt)", h.get(1).food.name);
        assertEquals(200.0, h.get(1).grams, 0.001);
    }

    @Test public void gramsAfterTheFoodAreAssigned() {
        List<Foods.Hit> h = hits("csirkemell 150g");
        assertEquals(1, h.size());
        assertEquals(150.0, h.get(0).grams, 0.001);
    }

    @Test public void unitVariantsAreUnderstood() {
        assertEquals(150.0, hits("csirkemell 150 gramm").get(0).grams, 0.001);
        assertEquals(150.0, hits("csirkemell 150 gr").get(0).grams, 0.001);
        // dekagramm: 20 dkg = 200 g
        assertEquals(200.0, hits("rizs 20 dkg").get(0).grams, 0.001);
    }

    @Test public void liquidUnitsBecomeGrams() {
        // Folyadéknál 1 ml ≈ 1 g.
        assertEquals(300.0, hits("3 dl tej").get(0).grams, 0.001);
        assertEquals(200.0, hits("2 deci tej").get(0).grams, 0.001);
        assertEquals(250.0, hits("250 ml protein turmix").get(0).grams, 0.001);
        assertEquals(1000.0, hits("1 l üdítő").get(0).grams, 0.001);
        // A magában álló „l" csak mértékegység lehet: a „2 lecsó" nem 2 liter.
        assertEquals(0.0, hits("2 lecsó").get(0).grams, 0.001);
    }

    @Test public void withoutANumberGramsStayZero() {
        List<Foods.Hit> h = hits("rántott hús rizzsel");
        assertEquals(2, h.size());
        for (Foods.Hit x : h) assertEquals(0.0, x.grams, 0.001);
    }

    @Test public void pieceCountsAreConvertedToGrams() {
        // A "2" itt darabszám, nem gramm: 2 tojás = 2 × 55 g.
        List<Foods.Hit> h = hits("2 tojás");
        assertEquals(1, h.size());
        assertEquals("Tojás", h.get(0).food.name);
        assertEquals(110.0, h.get(0).grams, 0.001);
        assertEquals(220.0, hits("4 tojás").get(0).grams, 0.001);
        assertEquals(360.0, hits("3 banán").get(0).grams, 0.001);
    }

    @Test public void hungarianNumberWordsCountToo() {
        assertEquals(110.0, hits("két tojás").get(0).grams, 0.001);
        assertEquals(110.0, hits("kettő tojás").get(0).grams, 0.001);
        assertEquals(360.0, hits("három banán").get(0).grams, 0.001);
        assertEquals(55.0, hits("egy zsemle").get(0).grams, 0.001);
        assertEquals(75.0, hits("fél alma").get(0).grams, 0.001);   // fél darab
        // Több étel, mindegyik a maga számnevével.
        List<Foods.Hit> h = hits("egy alma és két körte");
        assertEquals(2, h.size());
        assertEquals(150.0, h.get(0).grams, 0.001);
        assertEquals(300.0, h.get(1).grams, 0.001);
    }

    @Test public void bareNumbersStayIgnoredWhereTheyMakeNoSense() {
        // Rizst nem darabra számolunk.
        assertEquals(0.0, hits("2 rizs").get(0).grams, 0.001);
        // Életszerűtlen darabszám: inkább ne találgassunk.
        assertEquals(0.0, hits("50 tojás").get(0).grams, 0.001);
        // Mértékegység nélküli nagy szám sem gramm.
        assertEquals(0.0, hits("100 csirkemell").get(0).grams, 0.001);
    }

    @Test public void onlyOneAmountPerFood() {
        // Két szám, két étel – mindkettő megkapja a sajátját, egyik sem kettőt.
        List<Foods.Hit> h = hits("100 g sajt 50 g sonka");
        assertEquals(2, h.size());
        double sum = 0;
        for (Foods.Hit x : h) sum += x.grams;
        assertEquals(150.0, sum, 0.001);
        for (Foods.Hit x : h) assertTrue("mindkét étel kapott mennyiséget", x.grams > 0);
    }

    @Test public void nothingRecognisedGivesEmptyResult() {
        assertTrue(hits("zzzqqq 100 g").isEmpty());
    }
}
