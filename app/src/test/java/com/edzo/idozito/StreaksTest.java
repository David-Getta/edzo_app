package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;

/**
 * A napi széria. Ez hajtja az egész motivációs kört (kezdőlap, widget, esti
 * értesítés, jelvények), és két apróságon áll vagy bukik: a mai nap hiánya nem
 * törheti meg azonnal, és beállított edzésnap-terv esetén a pihenőnap sem.
 */
public class StreaksTest {

    /** Edzés N nappal ezelőtt, dél körül (hogy az időzóna ne csússzon el). */
    private static long daysAgo(int n) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -n);
        return c.getTimeInMillis();
    }

    private static long[] log(int... daysAgoList) {
        long[] a = new long[daysAgoList.length];
        for (int i = 0; i < daysAgoList.length; i++) a[i] = daysAgo(daysAgoList[i]);
        return a;
    }

    /** Minden nap edzésnap. */
    private static boolean[] everyDay() {
        boolean[] p = new boolean[7];
        java.util.Arrays.fill(p, true);
        return p;
    }

    // --- Terv nélkül: minden kihagyott nap megtöri ---

    @Test public void withoutAPlanEveryGapBreaks() {
        assertEquals(0, Streaks.count(null, log(), true));
        assertEquals(1, Streaks.count(null, log(0), true));
        assertEquals(3, Streaks.count(null, log(0, 1, 2), true));
        // Tegnap kimaradt → csak a mai nap.
        assertEquals(1, Streaks.count(null, log(0, 2, 3), true));
    }

    @Test public void todayIsAGracePeriod() {
        // Ma még nincs edzés, de tegnaptól él a széria – nem veszítjük el napközben.
        assertEquals(2, Streaks.count(null, log(1, 2), true));
        // A „tegnapig tartó" változat ugyanezt adja.
        assertEquals(2, Streaks.count(null, log(1, 2), false));
        // Ha ma volt edzés, az „include today" számolja, a másik nem.
        assertEquals(3, Streaks.count(null, log(0, 1, 2), true));
        assertEquals(2, Streaks.count(null, log(0, 1, 2), false));
    }

    @Test public void severalWorkoutsOnOneDayCountOnce() {
        assertEquals(1, Streaks.count(null, log(0, 0, 0), true));
    }

    // --- Tervvel: a pihenőnap nem töri meg ---

    @Test public void aFullPlanBehavesLikeNoPlan() {
        assertEquals(3, Streaks.count(everyDay(), log(0, 1, 2), true));
        assertEquals(1, Streaks.count(everyDay(), log(0, 2, 3), true));
    }

    @Test public void restDaysDoNotBreakTheStreak() {
        // Csak a mai nap tervezett edzésnap, a többi pihenő: a régebbi edzés is
        // beleszámít, mert a köztes napokon nem is kellett edzeni.
        int today = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7;
        boolean[] plan = new boolean[7];
        plan[today] = true;
        // Ma és 3 napja edzett; a köztes két nap pihenő → mindkettő számít.
        assertEquals(2, Streaks.count(plan, log(0, 3), true));
    }

    @Test public void aMissedPlannedDayBreaksIt() {
        // Minden nap tervezett; tegnap kimaradt → a széria a mai napnál megáll.
        assertEquals(1, Streaks.count(everyDay(), log(0, 2), true));
    }
}
