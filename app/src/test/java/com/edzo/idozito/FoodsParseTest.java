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

    @Test public void withoutANumberGramsStayZero() {
        List<Foods.Hit> h = hits("rántott hús rizzsel");
        assertEquals(2, h.size());
        for (Foods.Hit x : h) assertEquals(0.0, x.grams, 0.001);
    }

    @Test public void aBareNumberWithoutUnitIsIgnored() {
        // A "2" itt darabszám, nem gramm – nem szabad 2 grammnak venni.
        List<Foods.Hit> h = hits("2 tojás");
        assertEquals(1, h.size());
        assertEquals("Tojás", h.get(0).food.name);
        assertEquals(0.0, h.get(0).grams, 0.001);
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
