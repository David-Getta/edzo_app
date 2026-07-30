package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Darabszámolás és a számlálószavak.
 *
 * Ha a darabszám nem talál célt, a hiba néma: az app a tipikus adaggal számol
 * tovább, vagyis a „3 szelet kenyér” egyetlen szeletnyi lesz.
 */
public class FoodsPieceTest {

    private static Foods.Hit one(String q) {
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
        assertEquals("pontosan egy ételt vártam ebben: " + q, 1, hs.size());
        return hs.get(0);
    }

    @Test public void aCountingWordMayStandBetweenTheNumberAndTheFood() {
        // „3 szelet kenyér”: eddig a közbeékelt „szelet” miatt nem darabszámnak
        // látszott, és egyetlen szeletnyi adaggal számolt.
        assertEquals(3 * 35, one("3 szelet kenyér").grams, 0.01);
        assertEquals(2 * 35, one("két szelet kenyér").grams, 0.01);
        assertEquals(3 * 150, one("3 db alma").grams, 0.01);
        assertEquals(2 * 150, one("2 darab alma").grams, 0.01);
        assertEquals(3 * 50, one("3 gombóc fagyi").grams, 0.01);
        // Szó nélkül is, ahogy eddig.
        assertEquals(2 * 55, one("2 tojás").grams, 0.01);
    }

    @Test public void anythingElseBetweenThemIsNotACount() {
        // A közbeékelt szó nem lehet akármi: ott a szám nem az ételhez tartozik.
        assertEquals(0, one("3 perc alatt kész rizs").grams, 0.01);
        // A tej megkapja a 2 decijét, a rizs viszont nem lesz „2 darab rizs”.
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), "2 dl tejjel készült rizs");
        assertEquals(2, hs.size());
        assertEquals(200, hs.get(0).grams, 0.01);
        assertEquals(0, hs.get(1).grams, 0.01);
    }

    @Test public void eggWhiteIsNotAWholeEgg() {
        // 33 g és 17 kcal, nem 55 g és 78 kcal – négy és félszeres különbség.
        Foods.Hit h = one("2 tojásfehérje");
        assertEquals("Tojásfehérje", h.food.name);
        assertEquals(2 * 33, h.grams, 0.01);
        assertEquals("Tojás", one("2 tojás").food.name);
    }

    @Test public void theNewlyCountableFoodsWork() {
        assertEquals(3 * 12, one("3 keksz").grams, 0.01);
        assertEquals(2 * 60, one("2 tortilla").grams, 0.01);
        assertEquals(2 * 120, one("2 paradicsom").grams, 0.01);
        assertEquals(2 * 60, one("2 proteinszelet").grams, 0.01);
        assertEquals(2 * 60, one("2 croissant").grams, 0.01);
        assertEquals(2 * 80, one("2 kiwi").grams, 0.01);
    }

    @Test public void anUnrealisticCountIsIgnored() {
        // A darabszám csak életszerű tartományban számít; fölötte marad az adag.
        assertEquals(0, one("100 alma").grams, 0.01);
        assertEquals(0, one("2026 alma").grams, 0.01);
    }
}
