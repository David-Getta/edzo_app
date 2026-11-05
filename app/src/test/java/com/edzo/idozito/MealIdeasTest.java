package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * „Mi férne még bele ma?"
 *
 * A rossz tanács itt drágább, mint a semmilyen: aki a maradék kalóriájára
 * sört vagy fél almát kap ötletként, az legközelebb rá se néz a kártyára.
 */
public class MealIdeasTest {

    @Test public void everyCandidateNameExists() {
        // Ha egy ételt átnevezünk az adatbázisban, a javaslat csendben
        // eltűnne a listából – ezt itt fogjuk meg, nem a felhasználó.
        for (String[] pool : new String[][]{MealIdeas.PROTEIN, MealIdeas.LIGHT}) {
            for (String n : pool) {
                boolean found = false;
                for (Foods.Food f : Foods.ALL) if (f.name.equals(n)) { found = true; break; }
                assertTrue("nincs ilyen étel az adatbázisban: " + n, found);
            }
        }
    }

    @Test public void proteinGapGivesProteinRichIdeas() {
        List<MealIdeas.Idea> ideas = MealIdeas.forRemaining(Foods.ALL, 600, 40, 0);
        assertEquals(3, ideas.size());
        for (MealIdeas.Idea i : ideas) {
            assertTrue("fehérje-ötlet fehérje nélkül: " + i.label(), i.protein >= 10);
            assertTrue("nem fér bele: " + i.label(), i.kcal <= 600);
            assertTrue(i.forProtein);
        }
    }

    @Test public void withoutProteinGapTheIdeasAreLight() {
        List<MealIdeas.Idea> ideas = MealIdeas.forRemaining(Foods.ALL, 300, 0, 0);
        assertFalse(ideas.isEmpty());
        Set<String> light = new HashSet<>(java.util.Arrays.asList(MealIdeas.LIGHT));
        for (MealIdeas.Idea i : ideas) {
            assertTrue("nem a könnyű listáról jött: " + i.name, light.contains(i.name));
            assertFalse(i.forProtein);
        }
    }

    @Test public void nothingIsSuggestedWhenTheDayIsFull() {
        assertTrue(MealIdeas.forRemaining(Foods.ALL, 0, 30, 0).isEmpty());
        assertTrue(MealIdeas.forRemaining(Foods.ALL, -500, 30, 0).isEmpty());
        // 40 kalóriára már nincs értelmes ötlet.
        assertTrue(MealIdeas.forRemaining(Foods.ALL, 40, 30, 0).isEmpty());
    }

    @Test public void portionsAreRealisticNotTokenBites() {
        // Szűk maradéknál inkább kevesebb ötlet legyen, mint nevetséges adag.
        for (double left = MealIdeas.MIN_KCAL; left <= 1200; left += 17) {
            for (double prot : new double[]{0, 25}) {
                for (MealIdeas.Idea i : MealIdeas.forRemaining(Foods.ALL, left, prot, 3)) {
                    assertTrue("üres adag: " + i.label(), i.grams >= 10);
                    assertTrue("túllépi a maradékot: " + i.label() + " @" + left,
                            i.kcal <= left + 0.001);
                    Foods.Food f = null;
                    for (Foods.Food c : Foods.ALL) if (c.name.equals(i.name)) f = c;
                    assertTrue("fél adag alatti ajánlás: " + i.label(),
                            i.grams >= f.portion / 2.0);
                }
            }
        }
    }

    @Test public void theSameDayAlwaysGivesTheSameIdeas() {
        List<MealIdeas.Idea> a = MealIdeas.forRemaining(Foods.ALL, 500, 30, 12345);
        List<MealIdeas.Idea> b = MealIdeas.forRemaining(Foods.ALL, 500, 30, 12345);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).name, b.get(i).name);
    }

    @Test public void theSelectionRotatesOverDays() {
        // Ugyanaz a három ötlet egy hét alatt tapétává válna.
        Set<String> seen = new HashSet<>();
        for (long day = 0; day < 9; day++)
            for (MealIdeas.Idea i : MealIdeas.forRemaining(Foods.ALL, 600, 40, day))
                seen.add(i.name);
        assertTrue("nem forog a választék: " + seen, seen.size() >= 6);
        // Negatív mag (pl. korábbi időbélyeg) se dobjon indexhibát.
        assertFalse(MealIdeas.forRemaining(Foods.ALL, 600, 40, -7).isEmpty());
    }

    @Test public void ideasNeverRepeatWithinOneDay() {
        for (long day = 0; day < 30; day++) {
            Set<String> names = new HashSet<>();
            for (MealIdeas.Idea i : MealIdeas.forRemaining(Foods.ALL, 700, 35, day))
                assertTrue("kétszer ugyanaz: " + i.name, names.add(i.name));
        }
    }

    @Test public void labelsAreReadable() {
        for (MealIdeas.Idea i : MealIdeas.forRemaining(Foods.ALL, 500, 30, 1)) {
            assertTrue(i.label().contains(" g · "));
            assertTrue(i.label().contains("kcal"));
        }
        assertTrue(MealIdeas.forRemaining(null, 500, 30, 1).isEmpty());
    }
}
