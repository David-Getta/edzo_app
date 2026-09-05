package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * „Mikor csináltad utoljára" – a naptári napszámolás könnyen csúszik el egy
 * nappal (este vs. reggel, óraátállás), és pont ez a szám jelenik meg a
 * rekordlistában meg az emlékeztetőben.
 */
public class StrengthLogTest {

    /** N nappal ezelőtt, a megadott órában. */
    private static long daysAgoAt(int n, int hour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -n);
        return c.getTimeInMillis();
    }

    private static StrengthLog.Entry at(String name, long ts) {
        List<StrengthLog.SetEntry> sets = new ArrayList<>();
        sets.add(new StrengthLog.SetEntry(10, 50));
        return new StrengthLog.Entry(ts, name, sets);
    }

    private static List<StrengthLog.Entry> log(StrengthLog.Entry... e) {
        return new ArrayList<>(Arrays.asList(e));
    }

    private static long now() { return System.currentTimeMillis(); }

    // --- daysSince ---

    @Test public void neverDoneIsMinusOne() {
        assertEquals(-1, StrengthLog.daysSince(log(), "Guggolás", now()));
        assertEquals(-1, StrengthLog.daysSince(log(at("Evezés", now())), "Guggolás", now()));
    }

    @Test public void calendarDaysNotTwentyFourHourChunks() {
        // Tegnap este 22:00 → „tegnap", akkor is, ha még nincs 24 óra.
        assertEquals(1, StrengthLog.daysSince(log(at("Guggolás", daysAgoAt(1, 22))),
                "Guggolás", daysAgoAt(0, 8)));
        // Ma reggel → 0 nap.
        assertEquals(0, StrengthLog.daysSince(log(at("Guggolás", daysAgoAt(0, 7))),
                "Guggolás", daysAgoAt(0, 20)));
    }

    @Test public void theMostRecentSessionCounts() {
        List<StrengthLog.Entry> l = log(
                at("Guggolás", daysAgoAt(2, 12)),
                at("Guggolás", daysAgoAt(30, 12)));
        assertEquals(2, StrengthLog.daysSince(l, "Guggolás", now()));
    }

    @Test public void orderInTheListDoesNotMatter() {
        // A napló elvileg legújabb-elöl, de ne dőljön be, ha mégsem az.
        List<StrengthLog.Entry> l = log(
                at("Guggolás", daysAgoAt(30, 12)),
                at("Guggolás", daysAgoAt(2, 12)));
        assertEquals(2, StrengthLog.daysSince(l, "Guggolás", now()));
    }

    // --- daysTrainedIn ---

    @Test public void trainingDaysCountDaysNotEntries() {
        List<StrengthLog.Entry> l = log(
                at("Guggolás", daysAgoAt(0, 9)),
                at("Kitörés", daysAgoAt(0, 10)),     // ugyanaz a nap
                at("Evezés", daysAgoAt(3, 12)));
        assertEquals(2, StrengthLog.daysTrainedIn(l, now(), 7));
    }

    @Test public void olderSessionsFallOutOfTheWindow() {
        List<StrengthLog.Entry> l = log(
                at("Guggolás", daysAgoAt(1, 12)),
                at("Evezés", daysAgoAt(7, 12)),      // már kívül a 7 napos ablakon
                at("Bicepsz", daysAgoAt(30, 12)));
        assertEquals(1, StrengthLog.daysTrainedIn(l, now(), 7));
        assertEquals(0, StrengthLog.daysTrainedIn(log(), now(), 7));
    }

    // --- agoLabel ---

    @Test public void agoLabelReadsNaturally() {
        assertEquals("ma", StrengthLog.agoLabel(0));
        assertEquals("tegnap", StrengthLog.agoLabel(1));
        assertEquals("5 napja", StrengthLog.agoLabel(5));
        assertEquals("", StrengthLog.agoLabel(-1));
    }

    // --- mostNeglected ---

    @Test public void nothingNeglectedWhenEverythingIsRecent() {
        List<StrengthLog.Entry> l = log(
                at("Guggolás", daysAgoAt(1, 12)),
                at("Evezés", daysAgoAt(3, 12)));
        assertNull(StrengthLog.mostNeglected(l, now(), 14));
        assertNull(StrengthLog.mostNeglected(log(), now(), 14));
    }

    @Test public void theOldestExerciseIsReported() {
        List<StrengthLog.Entry> l = log(
                at("Guggolás", daysAgoAt(1, 12)),
                at("Fekvenyomás", daysAgoAt(20, 12)),
                at("Evezés", daysAgoAt(40, 12)));
        assertEquals("Evezés", StrengthLog.mostNeglected(l, now(), 14));
    }

    @Test public void aRecentRepeatClearsTheNeglect() {
        // Az Evezés régen is volt, de tegnap újra – így a Fekvenyomás a legrégebbi.
        List<StrengthLog.Entry> l = log(
                at("Evezés", daysAgoAt(1, 12)),
                at("Guggolás", daysAgoAt(2, 12)),
                at("Fekvenyomás", daysAgoAt(20, 12)),
                at("Evezés", daysAgoAt(40, 12)));
        assertEquals("Fekvenyomás", StrengthLog.mostNeglected(l, now(), 14));
    }

    @Test public void justUnderTheThresholdStaysQuiet() {
        List<StrengthLog.Entry> l = log(at("Guggolás", daysAgoAt(13, 12)));
        assertNull(StrengthLog.mostNeglected(l, now(), 14));
        assertEquals("Guggolás",
                StrengthLog.mostNeglected(log(at("Guggolás", daysAgoAt(14, 12))), now(), 14));
    }
}
