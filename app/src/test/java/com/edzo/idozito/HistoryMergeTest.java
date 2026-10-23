package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Egy erősítő bejegyzés egy GYAKORLAT, nem egy edzés. Ha ez elcsúszik, az
 * „elvégzett edzések" számláló, a jelvények és az XP is annyiszorosára nő,
 * ahány gyakorlatot valaki felír – ezért van külön tesztje.
 */
public class HistoryMergeTest {

    private static long dayAt(int daysAgo, int hour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return c.getTimeInMillis();
    }

    private static StrengthLog.Entry at(long ts) {
        List<StrengthLog.SetEntry> sets = new ArrayList<>();
        sets.add(new StrengthLog.SetEntry(10, 50));
        return new StrengthLog.Entry(ts, "Guggolás", sets);
    }

    private static List<StrengthLog.Entry> log(StrengthLog.Entry... e) {
        return new ArrayList<>(Arrays.asList(e));
    }

    @Test public void sixExercisesInOneDayAreOneWorkout() {
        List<StrengthLog.Entry> l = new ArrayList<>();
        for (int i = 0; i < 6; i++) l.add(at(dayAt(0, 10 + i % 4)));
        assertEquals(1, History.oneStrengthPerDay(l).length);
    }

    @Test public void separateDaysStaySeparate() {
        assertEquals(3, History.oneStrengthPerDay(
                log(at(dayAt(0, 12)), at(dayAt(1, 12)), at(dayAt(2, 12)))).length);
    }

    @Test public void morningAndEveningOnTheSameDayCountOnce() {
        assertEquals(1, History.oneStrengthPerDay(
                log(at(dayAt(1, 6)), at(dayAt(1, 21)))).length);
    }

    @Test public void theKeptTimestampIsARealEntryTime() {
        // A napszakhoz kötött jelvényekhez (korán kelő / éjjeli bagoly) az
        // eredeti időpont kell, nem éjfélre kerekítve.
        long evening = dayAt(1, 22);
        long[] out = History.oneStrengthPerDay(log(at(evening), at(dayAt(1, 8))));
        assertEquals(1, out.length);
        assertEquals(evening, out[0]);
    }

    @Test public void anEmptyLogGivesNothing() {
        assertEquals(0, History.oneStrengthPerDay(log()).length);
    }
}
