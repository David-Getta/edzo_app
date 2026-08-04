package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Terhelés-ugrás.
 *
 * Két hibázási irány van, és mindkettő drága: ha elhallgatunk egy valódi
 * ugrást, sérülés lesz belőle; ha viszont minden lelkes hétre rászólunk, a
 * figyelmeztetés két hét alatt zajjá válik, és az igazit is átnézi majd
 * felette.
 */
public class LoadTest {

    /** 35 napos tömb: az első hét napi `acute`, a rákövetkező 28 napi `base`. */
    private static double[] days(double acutePerDay, double basePerDay) {
        double[] d = new double[35];
        for (int i = 0; i < 7; i++) d[i] = acutePerDay;
        for (int i = 7; i < 35; i++) d[i] = basePerDay;
        return d;
    }

    @Test public void steadyTrainingIsNotFlagged() {
        // Napi 20 perc négy hete, most is: pont 1,0.
        Load.Ratio r = Load.of(days(20, 20));
        assertTrue(r.known);
        assertEquals(1.0, r.ratio, 0.001);
        assertEquals(Load.STEADY, r.level);
        assertEquals(140.0, r.acute, 0.001);
        assertEquals(140.0, r.chronic, 0.001);
    }

    @Test public void aBigJumpIsFlagged() {
        // Heti 140 percről 420-ra: háromszoros.
        Load.Ratio r = Load.of(days(60, 20));
        assertEquals(Load.JUMP, r.level);
        assertEquals(3.0, r.ratio, 0.001);
        assertTrue(r.advice().contains("vegyél vissza"));
        assertEquals("⚠️", r.emoji());
    }

    @Test public void gentleProgressIsEncouragedNotScolded() {
        // +20%: ez a kívánatos tempó, nem figyelmeztetés.
        Load.Ratio r = Load.of(days(24, 20));
        assertEquals(Load.STEADY, r.level);
        // +40%: már szólunk, de nem riasztunk.
        Load.Ratio p = Load.of(days(28, 20));
        assertEquals(Load.PUSHING, p.level);
        assertEquals("📈", p.emoji());
    }

    @Test public void anEasyWeekIsCalledRestNotFailure() {
        Load.Ratio r = Load.of(days(8, 20));
        assertEquals(Load.RESTING, r.level);
        assertTrue("a pihenőhét nem bűn", r.advice().contains("pihenő"));
    }

    @Test public void beginnersAreNotScaredOff() {
        // Aki eddig alig mozgott, annál minden mozgás „sokszoros ugrás" lenne.
        double[] d = new double[35];
        for (int i = 0; i < 7; i++) d[i] = 30;   // az első heti 210 perc
        Load.Ratio r = Load.of(d);
        assertFalse(r.known);
        assertEquals(Load.STEADY, r.level);
        assertEquals("Még gyűlik az alap", r.label());

        // Két hét előzmény már elég, ha volt benne érdemi mozgás.
        double[] two = new double[35];
        for (int i = 0; i < 7; i++) two[i] = 30;
        for (int i = 7; i < 21; i++) two[i] = 20;
        assertTrue(Load.of(two).known);
    }

    @Test public void shortHistoryIsNotInflated() {
        // 2 hét × napi 20 perc = 280 perc alap. Ha a 2 hetet vetítenénk heti
        // átlagra (140), akkor a mostani 140 perc „1,0" lenne – pedig a
        // szervezetnek nincs négyhetes szokása. A teljes 4 hétre osztunk: 70.
        double[] d = new double[35];
        for (int i = 0; i < 7; i++) d[i] = 20;
        for (int i = 7; i < 21; i++) d[i] = 20;
        Load.Ratio r = Load.of(d);
        assertEquals(70.0, r.chronic, 0.001);
        assertEquals(2.0, r.ratio, 0.001);
    }

    @Test public void missingOrBrokenDataNeverCrashes() {
        for (double[] d : new double[][]{null, new double[0], new double[3],
                new double[]{-5, -5, -5, -5, -5, -5, -5}}) {
            Load.Ratio r = Load.of(d);
            assertFalse(r.known);
            assertTrue(r.acute >= 0);
            assertTrue(!r.label().isEmpty() && !r.advice().isEmpty());
        }
        // Negatív percek nem csökkentik a hetet.
        double[] d = days(20, 20);
        d[0] = -100;
        assertEquals(120.0, Load.of(d).acute, 0.001);
    }

    @Test public void dailyBucketsComeFromTimestamps() {
        long now = 1770000000000L;      // tetszőleges nap dele környéke
        long day = 86400000L;
        long[] ts = {now, now, now - day, now - 10 * day, now - 100 * day, now + day};
        double[] min = {30, 15, 40, 25, 60, 99};
        double[] d = Load.daysFrom(ts, min, now, 35);
        assertEquals("a mai két edzés összeadódik", 45.0, d[0], 0.001);
        assertEquals(40.0, d[1], 0.001);
        assertEquals(25.0, d[10], 0.001);
        // A 100 napos és a jövőbeli bejegyzés kimarad.
        double sum = 0;
        for (double v : d) sum += v;
        assertEquals(110.0, sum, 0.001);
        assertEquals(35, Load.daysFrom(null, null, now, 35).length);
    }

    @Test public void theWholeChainHoldsTogether() {
        // Négy hete heti 3 × 45 perc, ezen a héten 5 × 60 perc.
        long now = 1770000000000L;
        long day = 86400000L;
        java.util.List<Long> ts = new java.util.ArrayList<>();
        java.util.List<Double> mins = new java.util.ArrayList<>();
        for (int d = 7; d < 35; d += 2) { ts.add(now - d * day); mins.add(45.0); }
        for (int i = 0; i < 5; i++) { ts.add(now - i * day); mins.add(60.0); }
        long[] t = new long[ts.size()];
        double[] m = new double[mins.size()];
        for (int i = 0; i < t.length; i++) { t[i] = ts.get(i); m[i] = mins.get(i); }
        Load.Ratio r = Load.of(Load.daysFrom(t, m, now, 35));
        assertTrue(r.known);
        assertEquals(300.0, r.acute, 0.001);
        assertEquals(157.5, r.chronic, 0.001);   // 14 alkalom × 45 / 4 hét
        assertEquals(Load.JUMP, r.level);
        assertTrue(r.label().startsWith("1,9"));
    }

    // --- Heti mozgás-cél ---

    @Test public void theWeeklyGoalCountsOnlyTheLastSevenDays() {
        // A régebbi hetek nem számítanak bele: a cél HETI.
        Load.Weekly w = Load.weekly(days(20, 60), 150);
        assertEquals(140.0, w.minutes, 0.001);
        assertEquals(150, w.goal);
        assertFalse(w.done);
        assertEquals(93, w.percent);
        assertEquals("140 / 150 perc", w.label());
        assertTrue(w.note().contains("Még 10 perc"));
    }

    @Test public void theDefaultGoalIsTheHealthRecommendation() {
        assertEquals(150, Load.DEFAULT_WEEKLY_GOAL);
        assertEquals(150, Load.weekly(days(10, 10), 0).goal);
        assertEquals(150, Load.weekly(days(10, 10), -5).goal);
    }

    @Test public void aFinishedGoalIsCelebratedNotOverflowed() {
        Load.Weekly w = Load.weekly(days(40, 10), 150);
        assertTrue(w.done);
        assertEquals(100, w.percent);       // a sáv nem lóg ki
        assertTrue(w.note().contains("megvan"));
        // Bőven túl: a többlet is látszik.
        assertTrue(Load.weekly(days(60, 10), 150).note().contains("perccel túl"));
    }

    @Test public void theMissingMinutesAreBrokenDownReadably() {
        assertTrue(Load.weekly(days(0, 0), 150).note().contains("napi húsz perc"));
        assertTrue(Load.weekly(days(20, 0), 150).note().contains("egy séta"));
        for (int have = 0; have <= 150; have += 7) {
            double[] d = new double[35];
            d[0] = have;
            Load.Weekly w = Load.weekly(d, 150);
            assertTrue(!w.note().isEmpty() && !w.label().isEmpty());
            assertTrue(w.percent >= 0 && w.percent <= 100);
        }
        assertEquals(0, Load.weekly(null, 150).percent);
    }
}
