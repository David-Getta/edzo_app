package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Az appban hirdetett példamondatok tényleg működnek.
 *
 * A tipp-szöveg ígéret: aki elolvassa, azt hiszi, hogy pont úgy is beírhatja.
 * Ha egy példa csendben elromlik – átnevezünk egy ételt, szigorítunk egy
 * mintán –, akkor az app maga bíztat valamire, amit aztán nem ismer fel.
 * Ez a teszt ezt fogja meg.
 */
public class ExamplesTest {

    /** Péntek dél: a hétköznap/hétvége példák így értelmesek. */
    private static long friday() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(2026, java.util.Calendar.AUGUST, 7, 12, 0, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Test public void everyMealExampleIsRecognised() {
        for (String q : Examples.MEAL) {
            List<Foods.Hit> hits = Foods.parse(Arrays.asList(Foods.ALL), q);
            assertTrue("nem ismeri fel a saját példáját: " + q, !hits.isEmpty());
            for (Foods.Hit h : hits) {
                double g = h.grams > 0 ? h.grams : h.food.portion;
                double kcal = g * h.food.kcal100 / 100.0;
                // A víz nulla kalória – az is érvényes találat.
                assertTrue("életszerűtlen kalória: " + q + " → " + Math.round(kcal),
                        kcal >= 0 && kcal <= 3000);
                assertTrue("adag nélküli étel: " + q, g > 0);
            }
        }
    }

    @Test public void everyBulkExampleIsRecognised() {
        for (String q : Examples.BULK) {
            Activities.Parsed p = Activities.parse(q, friday());
            boolean lifts = !StrengthParse.parse(q).isEmpty();
            assertTrue("sem edzésként, sem sorozatként nem érti: " + q,
                    lifts || !p.isEmpty());
        }
    }

    @Test public void everySetExampleIsRecognised() {
        for (String q : Examples.SET) {
            List<StrengthParse.Item> items = StrengthParse.parse(q);
            assertTrue("nem ismeri fel a saját példáját: " + q, !items.isEmpty());
            for (StrengthParse.Item it : items)
                assertTrue("üres sorozat: " + q, !it.sets.isEmpty());
        }
    }

    @Test public void multiExerciseExamplesReallyGiveSeveralExercises() {
        // Ha a „két gyakorlat egy mondatban" példa egyre esik össze, akkor a
        // tipp a felismerés egy olyan képességét hirdeti, ami már nincs meg.
        assertEquals(2, StrengthParse.parse("guggolás 3x10 60 kg, fekvenyomás 3x8 50 kg").size());
        assertEquals(2, StrengthParse.parse("lábgép 3x12 80 kg és vádli 4x15").size());
        assertEquals(2, StrengthParse.parse("arnold nyomás 3x10 16 kg, oldalemelés 3x15 8 kg").size());
    }

    @Test public void everyIntervalExampleIsRecognised() {
        for (String q : Examples.INTERVAL) {
            IntervalParse.Plan p = IntervalParse.parse(q);
            assertTrue("nem ismeri fel a saját példáját: " + q, p != null);
            assertTrue("üres beállítás: " + q, p.rounds >= 1 && p.work >= 5);
            assertTrue("életszerűtlen hossz: " + q, p.totalSec() <= 4 * 3600);
        }
    }

    @Test public void hintsAreWellFormedAndRotate() {
        for (String[] a : new String[][]{Examples.MEAL, Examples.BULK, Examples.SET,
                Examples.INTERVAL}) {
            Set<String> seen = new HashSet<>();
            for (String s : a) {
                assertTrue("üres példa", s.trim().length() > 2);
                // A „pl." előtagot a hint teszi hozzá – ne legyen benne kétszer.
                assertTrue("dupla előtag: " + s, !s.startsWith("pl."));
                assertTrue("ismétlődő példa: " + s, seen.add(s));
            }
            // Egy óra alatt minden példa sorra kerül.
            Set<String> shown = new HashSet<>();
            for (int min = 0; min < a.length; min++)
                shown.add(Examples.pick(a, min * 60000L));
            assertEquals("nem forog körbe", a.length, shown.size());
            assertTrue(Examples.hint(a, 0L).startsWith("pl. "));
        }
        assertTrue(Examples.mealHint(0L).startsWith("Mit ettél? (pl. "));
        assertTrue(Examples.mealHint(0L).endsWith(")"));
        // Negatív időbélyeg se dobjon indexhibát.
        assertTrue(Examples.pick(Examples.SET, -1234567L).length() > 0);
    }
}
