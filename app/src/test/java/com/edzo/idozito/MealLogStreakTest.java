package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * A naplózási széria. Dátumszámítás, tehát könnyű elrontani: a mai nap hiánya
 * nem törheti meg azonnal (aki este naplóz, napközben ne veszítse el a
 * szériáját), egy kihagyott nap viszont igen.
 */
public class MealLogStreakTest {

    private static final long DAY = 24L * 3600 * 1000;

    /** Étkezés N nappal ezelőtt, dél körül (hogy az időzóna se csússzon el). */
    private static MealLog.Meal daysAgo(int n) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, 12);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        c.add(java.util.Calendar.DAY_OF_MONTH, -n);
        return new MealLog.Meal(c.getTimeInMillis(), "teszt",
                new ArrayList<MealLog.Item>(), "");
    }

    private static List<MealLog.Meal> log(int... daysAgoList) {
        List<MealLog.Meal> out = new ArrayList<>();
        for (int d : daysAgoList) out.add(daysAgo(d));
        return out;
    }

    @Test public void emptyLogHasNoStreak() {
        assertEquals(0, MealLog.logStreak(new ArrayList<MealLog.Meal>()));
    }

    @Test public void consecutiveDaysCount() {
        assertEquals(1, MealLog.logStreak(log(0)));
        assertEquals(3, MealLog.logStreak(log(0, 1, 2)));
        assertEquals(5, MealLog.logStreak(log(0, 1, 2, 3, 4)));
    }

    @Test public void severalMealsOnTheSameDayCountOnce() {
        assertEquals(1, MealLog.logStreak(log(0, 0, 0)));
        assertEquals(2, MealLog.logStreak(log(0, 0, 1, 1, 1)));
    }

    @Test public void todayMissingDoesNotBreakItYet() {
        // Tegnaptól visszafelé él a széria; ma még van idő naplózni.
        assertEquals(2, MealLog.logStreak(log(1, 2)));
    }

    @Test public void aSkippedDayBreaksIt() {
        // Ma és tegnapelőtt van, tegnap nincs → csak a mai nap számít.
        assertEquals(1, MealLog.logStreak(log(0, 2, 3)));
        // Se ma, se tegnap → nincs élő széria.
        assertEquals(0, MealLog.logStreak(log(2, 3, 4)));
    }

    @Test public void onlyOldEntriesGiveNoStreak() {
        assertEquals(0, MealLog.logStreak(log(10, 11, 12)));
    }
}
