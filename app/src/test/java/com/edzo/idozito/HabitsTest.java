package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A szokásos étkezés felismerése.
 *
 * Egy rosszul felismert „szokás" naponta rossz kalóriát írna a naplóba,
 * ezért inkább hallgatunk, mint találgatunk.
 */
public class HabitsTest {

    private static List<List<String>> meals(String... joined) {
        List<List<String>> out = new ArrayList<>();
        for (String j : joined) out.add(j.isEmpty()
                ? new ArrayList<String>() : Arrays.asList(j.split(",")));
        return out;
    }

    @Test public void theRepeatedBreakfastIsFound() {
        List<List<String>> m = meals("Tojás,Kenyér", "Tojás,Kenyér", "Tojás,Kenyér",
                "Zabpehely,Banán");
        int[] hours = {8, 8, 7, 8};
        int[] ago = {1, 2, 3, 4};
        Habits.Usual u = Habits.usual(m, hours, ago, Habits.REGGEL);
        assertTrue(u != null);
        assertEquals(3, u.count);
        assertEquals(2, u.foods.size());
        assertTrue(u.foods.contains("Tojás"));
        assertTrue(u.label(Habits.REGGEL).contains("reggelid"));
        assertTrue(u.label(Habits.REGGEL).contains("3×"));
    }

    @Test public void theOrderDoesNotMatter() {
        // Aki egyszer „tojás, kenyér"-t ír, másszor „kenyér, tojás"-t, annak
        // ugyanaz a reggelije.
        List<List<String>> m = meals("Tojás,Kenyér", "Kenyér,Tojás", "Tojás,Kenyér");
        Habits.Usual u = Habits.usual(m, new int[]{8, 8, 8}, new int[]{1, 2, 3}, Habits.REGGEL);
        assertEquals(3, u.count);
    }

    @Test public void aRareCombinationIsNotAHabit() {
        List<List<String>> m = meals("Tojás,Kenyér", "Tojás,Kenyér", "Zabpehely");
        assertNull(Habits.usual(m, new int[]{8, 8, 8}, new int[]{1, 2, 3}, Habits.REGGEL));
    }

    @Test public void otherMealsOfTheDayDoNotCount() {
        // Ugyanaz az összeállítás, de vacsoraidőben: a reggelire nem számít.
        List<List<String>> m = meals("Tojás,Kenyér", "Tojás,Kenyér", "Tojás,Kenyér");
        assertNull(Habits.usual(m, new int[]{19, 20, 21}, new int[]{1, 2, 3}, Habits.REGGEL));
        Habits.Usual v = Habits.usual(m, new int[]{19, 20, 21}, new int[]{1, 2, 3},
                Habits.VACSORA);
        assertEquals(3, v.count);
    }

    @Test public void oldMealsAreNotTheCurrentHabit() {
        List<List<String>> m = meals("Tojás,Kenyér", "Tojás,Kenyér", "Tojás,Kenyér");
        assertNull(Habits.usual(m, new int[]{8, 8, 8}, new int[]{40, 50, 60}, Habits.REGGEL));
    }

    @Test public void theTimeBucketsCoverTheWholeDay() {
        assertEquals(Habits.REGGEL, Habits.bucketOf(8));
        assertEquals(Habits.EBED, Habits.bucketOf(13));
        assertEquals(Habits.UZSONNA, Habits.bucketOf(16));
        assertEquals(Habits.VACSORA, Habits.bucketOf(20));
        // Éjjel és hajnalban is kell valamit mondani.
        assertEquals(Habits.VACSORA, Habits.bucketOf(2));
        assertEquals(Habits.REGGEL, Habits.bucketOf(4));
        for (int h = 0; h < 24; h++) {
            int b = Habits.bucketOf(h);
            assertTrue(b >= 0 && b <= 3);
            assertTrue(!Habits.bucketName(b).isEmpty());
            assertTrue(!Habits.bucketMine(b).isEmpty());
        }
        // A magyar nem ragoz gépiesen: „reggeli" + „ed" nem „reggelied".
        assertEquals("reggelid", Habits.bucketMine(Habits.REGGEL));
        assertEquals("vacsorád", Habits.bucketMine(Habits.VACSORA));
    }

    @Test public void brokenInputNeverCrashes() {
        assertNull(Habits.usual(null, null, null, Habits.REGGEL));
        assertNull(Habits.usual(meals(""), new int[]{8}, new int[]{1}, Habits.REGGEL));
        // Eltérő hosszú tömbök: a rövidebb dönt.
        assertNull(Habits.usual(meals("Tojás", "Tojás", "Tojás"),
                new int[]{8}, new int[]{1}, Habits.REGGEL));
        // Üres és null elemek kiesnek, a maradék még lehet szokás.
        List<List<String>> m = new ArrayList<>();
        for (int i = 0; i < 3; i++) m.add(Arrays.asList("Tojás", "", null, "Tojás"));
        Habits.Usual u = Habits.usual(m, new int[]{8, 8, 8}, new int[]{1, 2, 3}, Habits.REGGEL);
        assertEquals(1, u.foods.size());
        assertEquals(3, u.count);
    }
    @Test public void theWeekdaySportHabitIsFound() {
        // Kedden háromszor úszás, egyszer futás: a kedd az úszásé.
        int[] wd = {1, 1, 1, 1, 3};
        String[] kinds = {"uszas", "uszas", "uszas", "futas", "uszas"};
        int[] ago = {7, 14, 21, 28, 5};
        assertEquals("uszas", Habits.usualSportOn(wd, kinds, ago, 1));
        // Szerdán nincs elég adat.
        assertNull(Habits.usualSportOn(wd, kinds, ago, 2));
    }

    @Test public void anAmbiguousHabitIsNoHabit() {
        // Háromszor úszás, háromszor futás ugyanazon a napon: nem lehet
        // megmondani, melyik a mai.
        int[] wd = {1, 1, 1, 1, 1, 1};
        String[] kinds = {"uszas", "uszas", "uszas", "futas", "futas", "futas"};
        int[] ago = {7, 14, 21, 28, 35, 42};
        assertNull(Habits.usualSportOn(wd, kinds, ago, 1));
    }

    @Test public void oldSportHabitsAreForgotten() {
        int[] wd = {1, 1, 1};
        String[] kinds = {"uszas", "uszas", "uszas"};
        int[] ago = {100, 200, 300};
        assertNull(Habits.usualSportOn(wd, kinds, ago, 1));
    }

    @Test public void theSportHabitSurvivesBrokenInput() {
        assertNull(Habits.usualSportOn(null, null, null, 1));
        assertNull(Habits.usualSportOn(new int[]{1}, new String[]{""}, new int[]{7}, 1));
        assertNull(Habits.usualSportOn(new int[]{1, 1, 1}, new String[]{null, "", "  "},
                new int[]{7, 14, 21}, 1));
        // Eltérő hosszú tömbök: a rövidebb dönt.
        assertNull(Habits.usualSportOn(new int[]{1, 1, 1}, new String[]{"uszas"},
                new int[]{7}, 1));
    }

    @Test public void aTieIsBrokenByRecency() {
        // Két összeállítás ugyanannyiszor: eddig a tárolási sorrend döntött,
        // vagyis véletlenszerűen. Egy rossz „szokásos reggeli" egy
        // koppintással rossz kalóriát ír a naplóba.
        java.util.List<java.util.List<String>> meals = new java.util.ArrayList<>();
        java.util.List<Integer> hours = new java.util.ArrayList<>();
        java.util.List<Integer> ago = new java.util.ArrayList<>();
        // A RÉGEBBI összeállítás háromszor, 20–22 napja.
        for (int d : new int[]{20, 21, 22}) {
            meals.add(java.util.Arrays.asList("Zabpehely", "Banán"));
            hours.add(8); ago.add(d);
        }
        // Az ÚJABB ugyanannyiszor, 1–3 napja.
        for (int d : new int[]{1, 2, 3}) {
            meals.add(java.util.Arrays.asList("Tojás", "Kenyér"));
            hours.add(8); ago.add(d);
        }
        int[] h = new int[hours.size()], a = new int[ago.size()];
        for (int i = 0; i < h.length; i++) { h[i] = hours.get(i); a[i] = ago.get(i); }
        Habits.Usual u = Habits.usual(meals, h, a, Habits.REGGEL);
        assertNotNull(u);
        assertEquals(3, u.count);
        assertTrue("a frissebb szokás nyer: " + u.foods, u.foods.contains("Tojás"));
    }
}
