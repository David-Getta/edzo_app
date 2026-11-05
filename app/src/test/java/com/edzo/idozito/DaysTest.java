package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Naptári nap-számítás óraátállással. Ez a hiba évente kétszer jelentkezik, és
 * pont olyankor, amikor senki nem keresi: a heti/havi csíkok egy nappal
 * elcsúsznak, mert az óraátállás napja 23, illetve 25 órás.
 *
 * A teszt a valódi magyar átállási dátumokkal dolgozik (2026. március 29. és
 * október 25.), nem kitalált időzónával.
 */
public class DaysTest {

    private TimeZone original;

    @Before public void useBudapestTime() {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Budapest"));
    }

    @After public void restoreTimeZone() {
        TimeZone.setDefault(original);
    }

    /** Adott nap adott órája budapesti idő szerint. */
    private static long at(int y, int month, int day, int hour) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, month - 1, day, hour, 0, 0);
        return c.getTimeInMillis();
    }

    // --- Hétköznapi eset ---

    @Test public void sameDayIsZero() {
        assertEquals(0, Days.between(at(2026, 7, 15, 7), at(2026, 7, 15, 23)));
    }

    @Test public void consecutiveDaysAreOneApart() {
        assertEquals(1, Days.between(at(2026, 7, 15, 22), at(2026, 7, 16, 6)));
        assertEquals(7, Days.between(at(2026, 7, 15, 12), at(2026, 7, 22, 12)));
    }

    @Test public void backwardsIsNegative() {
        assertEquals(-1, Days.between(at(2026, 7, 16, 12), at(2026, 7, 15, 12)));
    }

    // --- Tavaszi előreállítás: március 29. csak 23 órás ---

    @Test public void theShortSpringDayStillCountsAsAFullDay() {
        // Március 28. → 29.: az átállás miatt csak 23 óra telik el.
        assertEquals(1, Days.between(at(2026, 3, 28, 12), at(2026, 3, 29, 12)));
        // Március 28. → 30.: 47 óra. A 24-gyel osztás lefelé 1-et adna.
        assertEquals(2, Days.between(at(2026, 3, 28, 12), at(2026, 3, 30, 12)));
        // Egy teljes hét az átállás körül.
        assertEquals(7, Days.between(at(2026, 3, 26, 12), at(2026, 4, 2, 12)));
    }

    // --- Őszi visszaállítás: október 25. 25 órás ---

    @Test public void theLongAutumnDayIsAlsoOneDay() {
        assertEquals(1, Days.between(at(2026, 10, 24, 12), at(2026, 10, 25, 12)));
        assertEquals(2, Days.between(at(2026, 10, 24, 12), at(2026, 10, 26, 12)));
        assertEquals(7, Days.between(at(2026, 10, 22, 12), at(2026, 10, 29, 12)));
    }

    // --- Napszéli időpontok az átállás napján ---

    @Test public void edgesOfTheSwitchDayStayInTheirOwnDay() {
        // Az átállás napjának hajnala és estéje ugyanaz a nap.
        assertEquals(0, Days.between(at(2026, 3, 29, 1), at(2026, 3, 29, 23)));
        assertEquals(0, Days.between(at(2026, 10, 25, 1), at(2026, 10, 25, 23)));
        // És a rákövetkező nap hajnala már a következő.
        assertEquals(1, Days.between(at(2026, 3, 29, 23), at(2026, 3, 30, 1)));
        assertEquals(1, Days.between(at(2026, 10, 25, 23), at(2026, 10, 26, 1)));
    }

    // --- Egy egész év: sosem ugorhat és sosem állhat meg ---

    @Test public void everyConsecutivePairOfDaysIsExactlyOneApart() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.JANUARY, 1, 12, 0, 0);
        long prev = c.getTimeInMillis();
        for (int i = 0; i < 365; i++) {
            c.add(Calendar.DAY_OF_YEAR, 1);
            long cur = c.getTimeInMillis();
            assertEquals("elcsúszás a(z) " + i + ". napnál", 1, Days.between(prev, cur));
            prev = cur;
        }
    }

    @Test public void startOfDayIsMidnight() {
        long s = Days.startOf(at(2026, 7, 15, 17));
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(s);
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(15, c.get(Calendar.DAY_OF_MONTH));
    }

    @Test public void agoCountsBackwardsFromNow() {
        long now = at(2026, 7, 15, 20);
        assertEquals(0, Days.ago(at(2026, 7, 15, 7), now));
        assertEquals(1, Days.ago(at(2026, 7, 14, 23), now));
        assertEquals(30, Days.ago(at(2026, 6, 15, 12), now));
    }
}
