package com.edzo.idozito;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.LinkedHashMap;

/**
 * Sportágankénti összesítés a Statisztikához.
 *
 * A besorolás elve: a felhasználót az érdekli, mennyit futott, nem az, hogy
 * melyik gombbal rögzítette – a mért futás és a kézzel felvett futás egy
 * sorba olvad.
 */
public class ActivitiesBreakdownTest {

    @Test public void measuredAndManualRunsMergeIntoOneRow() {
        // Két mért futás (nincs kind, nincs név) + egy kézi futás.
        LinkedHashMap<String, long[]> rows = Activities.breakdown(
                new String[]{"", "", "futas"},
                new String[]{"", "", "🏃 Futás"},
                new int[]{1800, 2400, 2700});
        assertEquals(1, rows.size());
        long[] run = rows.values().iterator().next();
        assertEquals(3, run[0]);
        assertEquals(1800 + 2400 + 2700, run[1]);
        assertTrue(rows.keySet().iterator().next().contains("Futás"));
    }

    @Test public void sortedByCountThenTime() {
        LinkedHashMap<String, long[]> rows = Activities.breakdown(
                new String[]{"kezilabda", "kezilabda", "kezilabda", "futas", "joga", "joga"},
                new String[]{"x", "x", "x", "x", "x", "x"},
                new int[]{5400, 5400, 5400, 2700, 3600, 3600});
        assertArrayEquals(new String[]{"🤾 Kézilabda", "🧘 Jóga / nyújtás / pilates", "🏃 Futás"},
                rows.keySet().toArray(new String[0]));
    }

    @Test public void namedTimerWorkoutsKeepTheirProgramName() {
        LinkedHashMap<String, long[]> rows = Activities.breakdown(
                new String[]{"", ""},
                new String[]{"Zsírégető HIIT", "Zsírégető HIIT"},
                new int[]{420, 420});
        assertEquals(1, rows.size());
        assertEquals("⏱ Zsírégető HIIT", rows.keySet().iterator().next());
        assertEquals(2, rows.values().iterator().next()[0]);
    }

    @Test public void unknownKindFallsBackToTheName() {
        // Jövőbeli vagy sérült „kind" nem törhet össze semmit: a név dönt.
        LinkedHashMap<String, long[]> rows = Activities.breakdown(
                new String[]{"nincs-ilyen"},
                new String[]{"Valami edzés"},
                new int[]{600});
        assertEquals("⏱ Valami edzés", rows.keySet().iterator().next());
    }

    @Test public void emptyInputGivesEmptyOutput() {
        assertTrue(Activities.breakdown(new String[0], new String[0], new int[0]).isEmpty());
        // Eltérő hosszú tömbök sem dobnak kivételt (a rövidebbig megy).
        assertEquals(1, Activities.breakdown(
                new String[]{"futas", "joga"}, new String[]{"a"}, new int[]{60, 60}).size());
    }

    @Test public void negativeDurationsDoNotShrinkTheTotal() {
        LinkedHashMap<String, long[]> rows = Activities.breakdown(
                new String[]{"futas", "futas"}, new String[]{"", ""}, new int[]{1800, -500});
        assertEquals(1800, rows.values().iterator().next()[1]);
    }
}
