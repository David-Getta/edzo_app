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
}
