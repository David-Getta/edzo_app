package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * A valaha volt leghosszabb edzésnap-sorozat. Ebből jön a „Lendület" (3),
 * a „Heti hős" (7) és a „Gyémánt rutin" (30) jelvény – ha elszámolja, a
 * felhasználó vagy nem kapja meg a kiérdemelt jelvényt, vagy olyat kap, amit
 * nem érdemelt ki.
 */
public class BadgesTest {

    private TimeZone original;

    @Before public void useBudapestTime() {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Budapest"));
    }

    @After public void restoreTimeZone() {
        TimeZone.setDefault(original);
    }

    private static long at(int y, int month, int day, int hour) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, month - 1, day, hour, 0, 0);
        return c.getTimeInMillis();
    }

    /** Egymást követő napok déli időbélyegei az adott naptól kezdve. */
    private static long[] run(int y, int month, int day, int count) {
        long[] out = new long[count];
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, month - 1, day, 12, 0, 0);
        for (int i = 0; i < count; i++) {
            out[i] = c.getTimeInMillis();
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return out;
    }

    @Test public void noWorkoutsMeanNoStreak() {
        assertEquals(0, Badges.bestDayStreak(new long[0]));
    }

    @Test public void oneDayIsAStreakOfOne() {
        assertEquals(1, Badges.bestDayStreak(new long[]{at(2026, 7, 15, 8)}));
    }

    @Test public void severalWorkoutsOnOneDayStillCountAsOneDay() {
        assertEquals(1, Badges.bestDayStreak(new long[]{
                at(2026, 7, 15, 7), at(2026, 7, 15, 12), at(2026, 7, 15, 21)}));
    }

    @Test public void consecutiveDaysAddUp() {
        assertEquals(5, Badges.bestDayStreak(run(2026, 7, 13, 5)));
        assertEquals(30, Badges.bestDayStreak(run(2026, 5, 1, 30)));
    }

    @Test public void aGapBreaksTheStreakAndTheLongestOneWins() {
        // 3 nap, kihagyás, majd 6 nap → a leghosszabb 6.
        long[] a = run(2026, 7, 1, 3);
        long[] b = run(2026, 7, 10, 6);
        long[] all = new long[a.length + b.length];
        System.arraycopy(a, 0, all, 0, a.length);
        System.arraycopy(b, 0, all, a.length, b.length);
        assertEquals(6, Badges.bestDayStreak(all));
    }

    @Test public void theOrderOfTheEntriesDoesNotMatter() {
        long[] forward = run(2026, 7, 13, 4);
        long[] shuffled = {forward[2], forward[0], forward[3], forward[1]};
        assertEquals(4, Badges.bestDayStreak(shuffled));
    }

    // --- Óraátállás ---

    @Test public void theSpringSwitchDoesNotBreakAStreak() {
        // 2026. március 29. csak 23 órás – a sorozatnak ettől nem szabad
        // kettészakadnia, se megduplázódnia.
        assertEquals(7, Badges.bestDayStreak(run(2026, 3, 26, 7)));
    }

    @Test public void theAutumnSwitchDoesNotBreakAStreak() {
        // 2026. október 25. 25 órás.
        assertEquals(7, Badges.bestDayStreak(run(2026, 10, 22, 7)));
    }

    @Test public void aYearOfDailyTrainingIsCountedExactly() {
        assertEquals(365, Badges.bestDayStreak(run(2026, 1, 1, 365)));
    }

    // --- A jelvény-küszöbök ---

    @Test public void theStreakBadgesUnlockAtTheirThresholds() {
        assertTrue("3 nap még nem heti hős", Badges.bestDayStreak(run(2026, 7, 1, 3)) < 7);
        assertTrue("7 nap már igen", Badges.bestDayStreak(run(2026, 7, 1, 7)) >= 7);
        assertTrue("29 nap még nem gyémánt", Badges.bestDayStreak(run(2026, 6, 1, 29)) < 30);
        assertTrue("30 nap már igen", Badges.bestDayStreak(run(2026, 6, 1, 30)) >= 30);
    }
}
