package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Az emlékeztetők időzítése.
 *
 * A régi megoldás fix 24 órás lépésközzel ismételt, ami az óraátállítás után
 * egy órát csúszott, és onnantól magától soha nem állt vissza. Az itteni
 * ellenőrzés lényege a LÁNC: mindig abból az időpontból számoljuk a
 * következőt, amikor az előző riasztás megszólalt – pontosan úgy, ahogy a
 * vevők teszik –, és megköveteljük, hogy a fali óra végig ugyanaz maradjon.
 */
public class AlarmsTest {

    private static final TimeZone BP = TimeZone.getTimeZone("Europe/Budapest");
    private static final long DAY = 24L * 60 * 60 * 1000;

    private static Calendar at(long t) {
        Calendar c = Calendar.getInstance(BP);
        c.setTimeInMillis(t);
        return c;
    }

    private static long stamp(int y, int mo, int d, int h, int mi) {
        Calendar c = Calendar.getInstance(BP);
        c.clear();
        c.set(y, mo, d, h, mi, 0);
        return c.getTimeInMillis();
    }

    private static String show(long t) {
        Calendar c = at(t);
        return String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    @Test public void aYearLongChainNeverDriftsOffTheWallClock() {
        long now = stamp(2026, Calendar.JANUARY, 1, 12, 0);
        for (int i = 0; i < 400; i++) {
            long t = Alarms.nextDaily(18, 0, now, BP);
            assertTrue("nem a jövőben: " + show(t), t > now);
            assertEquals("elcsúszott az emlékeztető: " + show(t), 18, at(t).get(Calendar.HOUR_OF_DAY));
            assertEquals("elcsúszott az emlékeztető: " + show(t), 0, at(t).get(Calendar.MINUTE));
            now = t;   // a vevő a riasztás pillanatában ütemezi a következőt
        }
    }

    @Test public void theClockChangeShortensAndLengthensExactlyOneStep() {
        // Tavasszal 23, ősszel 25 órás a lépés – éppen ezt nem tudta a fix
        // INTERVAL_DAY, és emiatt csúszott el az időpont fél évre.
        long spring = Alarms.nextDaily(18, 0, stamp(2026, Calendar.MARCH, 28, 19, 0), BP);
        assertEquals("2026-03-29 18:00", show(spring));
        assertEquals(23 * 60 * 60 * 1000L, spring - stamp(2026, Calendar.MARCH, 28, 18, 0));

        long autumn = Alarms.nextDaily(18, 0, stamp(2026, Calendar.OCTOBER, 24, 19, 0), BP);
        assertEquals("2026-10-25 18:00", show(autumn));
        assertEquals(25 * 60 * 60 * 1000L, autumn - stamp(2026, Calendar.OCTOBER, 24, 18, 0));
    }

    @Test public void theOldFixedIntervalWouldHaveDrifted() {
        // Ellenpróba: a régi módszer 18:00-ról 19:00-ra csúszott volna.
        long start = stamp(2026, Calendar.MARCH, 1, 18, 0);
        long old = start + 60 * DAY;
        assertEquals(19, at(old).get(Calendar.HOUR_OF_DAY));
        assertEquals(18, at(Alarms.nextDaily(18, 0, old - 3 * DAY, BP)).get(Calendar.HOUR_OF_DAY));
    }

    @Test public void everyTimeOfDaySurvivesBothClockChanges() {
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 15) {
                long now = stamp(2026, Calendar.JANUARY, 1, 12, 0);
                for (int i = 0; i < 400; i++) {
                    long t = Alarms.nextDaily(h, m, now, BP);
                    assertTrue("megállt a lánc " + h + ":" + m + " – " + show(t), t > now);
                    assertTrue("nem tartotta az időpontot (" + h + ":" + m + "): " + show(t),
                            at(t).get(Calendar.HOUR_OF_DAY) == h && at(t).get(Calendar.MINUTE) == m);
                    now = t;
                }
            }
        }
    }

    @Test public void theHourThatDoesNotExistIsSkippedNotDoubled() {
        // 2026-03-29-én Budapesten a 02:00–03:00 óra kimarad. Ilyenkor inkább
        // kihagyjuk aznap az emlékeztetőt, mint hogy éjjel kétszer szóljon.
        long t = Alarms.nextDaily(2, 30, stamp(2026, Calendar.MARCH, 28, 23, 0), BP);
        assertEquals("2026-03-30 02:30", show(t));
    }

    @Test public void aReminderSetForTheCurrentMinuteWaitsADay() {
        long now = stamp(2026, Calendar.JULY, 29, 18, 0);
        assertEquals("2026-07-30 18:00", show(Alarms.nextDaily(18, 0, now, BP)));
        assertEquals("2026-07-29 18:01", show(Alarms.nextDaily(18, 1, now, BP)));
        assertEquals("2026-07-30 00:00", show(Alarms.nextDaily(0, 0, now, BP)));
    }

    @Test public void theWeeklyRecapAlwaysLandsOnSundayEvening() {
        long now = stamp(2026, Calendar.JANUARY, 1, 12, 0);
        long prev = 0;
        for (int i = 0; i < 120; i++) {
            long t = Alarms.nextWeekly(Calendar.SUNDAY, 19, 0, now, BP);
            assertTrue("megállt a heti lánc: " + show(t), t > now);
            assertEquals("nem vasárnap: " + show(t), Calendar.SUNDAY, at(t).get(Calendar.DAY_OF_WEEK));
            assertEquals("nem 19:00: " + show(t), 19, at(t).get(Calendar.HOUR_OF_DAY));
            // Az első lépés a tetszőleges kezdőidőponttól számít, a többi már
            // riasztástól riasztásig: ott pontosan egy hétnek kell lennie.
            if (prev != 0) {
                long gap = t - prev;
                assertTrue("nem heti lépés: " + show(prev) + " -> " + show(t),
                        gap >= 6 * DAY && gap <= 8 * DAY);
            }
            prev = t;
            now = t;
        }
    }

    @Test public void theWeeklyRecapDoesNotDependOnThePhoneLanguage() {
        // A Calendar.set(DAY_OF_WEEK, …) eredménye a firstDayOfWeek-től, azaz a
        // nyelvi beállítástól függ – ezért nem arra építünk.
        Locale before = Locale.getDefault();
        try {
            long now = stamp(2026, Calendar.JULY, 29, 10, 0);   // szerda
            Locale.setDefault(new Locale("hu", "HU"));
            long hu = Alarms.nextWeekly(Calendar.SUNDAY, 19, 0, now, BP);
            Locale.setDefault(Locale.US);
            long us = Alarms.nextWeekly(Calendar.SUNDAY, 19, 0, now, BP);
            assertEquals("2026-08-02 19:00", show(hu));
            assertEquals(hu, us);
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test public void sundayEveningSchedulesTheNextWeekNotTheSameOne() {
        assertEquals("2026-08-09 19:00",
                show(Alarms.nextWeekly(Calendar.SUNDAY, 19, 0,
                        stamp(2026, Calendar.AUGUST, 2, 19, 0), BP)));
        assertEquals("2026-08-02 19:00",
                show(Alarms.nextWeekly(Calendar.SUNDAY, 19, 0,
                        stamp(2026, Calendar.AUGUST, 2, 18, 59), BP)));
    }
}
