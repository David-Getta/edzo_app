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

    @Test public void wholeDishesAndGlassesCountAsPortions() {
        // A „2 burrito" eddig EGY adag volt: a darabszám némán elveszett.
        // Az egészben fogyasztott fogásoknál a darab egy teljes adag.
        assertEquals(2 * 300, one("2 burrito").grams, 0.01);
        assertEquals(2 * 250, one("2 hamburger").grams, 0.01);
        assertEquals(2 * 180, one("2 cordon bleu").grams, 0.01);
        assertEquals(2 * 300, one("két fröccs").grams, 0.01);
        assertEquals(2 * 300, one("2 pohár limonádé").grams, 0.01);
        // A falafel és a sushi darabja golyó/falat, nem tányér.
        assertEquals(3 * 25, one("3 db falafel").grams, 0.01);
        assertEquals(8 * 30, one("8 sushi").grams, 0.01);
        // Szám nélkül marad a szokásos adag.
        assertEquals(0, one("falafel").grams, 0.01);
        assertEquals(0, one("burrito").grams, 0.01);
        // A decis mérés erősebb a darabnál.
        assertEquals(200, one("2 dl fröccs").grams, 0.01);
    }

    @Test public void drinksCountByTheGlass() {
        // A „2 korsó sör" fele eddig némán elveszett: a sörnek nem volt
        // darabsúlya. Az italok darabja a szokásos kiszerelés.
        assertEquals(2 * 500, one("2 korsó sör").grams, 0.01);
        assertEquals(2 * 500, one("2 sör").grams, 0.01);
        assertEquals(2 * 150, one("két pohár bor").grams, 0.01);
        assertEquals(2 * 40, one("2 feles pálinka").grams, 0.01);
        assertEquals(3 * 200, one("3 kávé").grams, 0.01);
        assertEquals(2 * 250, one("2 cappuccino").grams, 0.01);
        // A mért mennyiség erősebb a darabnál.
        assertEquals(300, one("3 dl sör").grams, 0.01);
        assertEquals(500, one("fél liter tej").grams, 0.01);
        // Szám nélkül marad az adag.
        assertEquals(0, one("sör").grams, 0.01);
    }

    @Test public void aPortionWordWorksForAnyFood() {
        // Az „adag" bármely ételre megy: egy adag a tipikus adag – eddig a
        // „fél adag gyros" is teljes adagnak számított.
        Foods.Hit gy = one("fél adag gyros");
        assertEquals(gy.food.portion * 0.5, gy.grams, 0.01);
        Foods.Hit gu = one("2 adag rántott hús");
        assertEquals(gu.food.portion * 2.0, gu.grams, 0.01);
        Foods.Hit ri = one("másfél adag rizs");
        assertEquals(ri.food.portion * 1.5, ri.grams, 0.01);
        // Adag-szó nélkül marad minden a régiben.
        assertEquals(0, one("gyros").grams, 0.01);
        // A gramm erősebb: a kimondott mennyiség nem adag-szorzó.
        assertEquals(200, one("200 g gyros").grams, 0.01);
    }

    @Test public void anUnrealisticCountIsIgnored() {
        // A darabszám csak életszerű tartományban számít; fölötte marad az adag.
        assertEquals(0, one("100 alma").grams, 0.01);
        assertEquals(0, one("2026 alma").grams, 0.01);
    }
}
