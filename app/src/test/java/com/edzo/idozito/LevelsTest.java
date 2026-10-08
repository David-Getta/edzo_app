package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A szint-rendszer matematikája. Ez határozza meg a felhasználó előrehaladását,
 * ezért fontos, hogy a szint és a hozzá tartozó XP-küszöb mindig egymás inverze
 * legyen – különben a haladássáv „visszaugorhatna" szintlépéskor.
 */
public class LevelsTest {

    @Test public void levelAndThresholdAreConsistent() {
        for (int lvl = 1; lvl <= 30; lvl++) {
            long need = Levels.xpForLevel(lvl);
            assertEquals("a küszöb pontosan ezt a szintet adja", lvl, Levels.levelForXp(need));
            if (lvl > 1)
                assertEquals("egy XP-vel a küszöb alatt még az előző szint",
                        lvl - 1, Levels.levelForXp(need - 1));
        }
    }

    @Test public void thresholdsIncrease() {
        for (int lvl = 1; lvl < 30; lvl++)
            assertTrue("a következő szint mindig többe kerül",
                    Levels.xpForLevel(lvl + 1) > Levels.xpForLevel(lvl));
    }

    @Test public void startsAtLevelOne() {
        assertEquals(1, Levels.levelForXp(0));
        assertEquals(1, Levels.levelForXp(-5));       // hibás adat se rontsa el
        assertEquals(0, Levels.xpForLevel(1));
    }

    @Test public void progressStaysInRange() {
        for (long xp = 0; xp <= 5000; xp += 37) {
            float p = Levels.progress(xp);
            assertTrue("a haladás 0 és 1 közt marad (xp=" + xp + ")", p >= 0f && p <= 1f);
        }
        // Pontosan szintlépéskor újraindul a sáv.
        assertEquals(0f, Levels.progress(Levels.xpForLevel(4)), 0.0001f);
    }

    @Test public void sessionXpRewardsLongerAndFartherWorkouts() {
        long base = Levels.xpForSession(0, -1);          // erősítő bejegyzés (nincs idő/táv)
        assertTrue("minden befejezett edzés ér valamit", base > 0);
        assertTrue("a hosszabb edzés többet ér",
                Levels.xpForSession(1800, -1) > Levels.xpForSession(600, -1));
        assertTrue("a táv is számít",
                Levels.xpForSession(1800, 5000) > Levels.xpForSession(1800, -1));
    }

    // --- Erősítő XP ---

    private static StrengthLog.Entry gym(int daysAgo, int sets) {
        java.util.List<StrengthLog.SetEntry> l = new java.util.ArrayList<>();
        for (int i = 0; i < sets; i++) l.add(new StrengthLog.SetEntry(10, 50));
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, 12);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        c.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo);
        return new StrengthLog.Entry(c.getTimeInMillis(), "Guggolás", l);
    }

    @Test public void strengthXpFollowsTheWorkDone() {
        assertEquals(0, Levels.strengthXp(new java.util.ArrayList<StrengthLog.Entry>()));
        assertEquals(6, Levels.strengthXp(java.util.Arrays.asList(gym(0, 3))));
        // Több gyakorlat egy napon összeadódik.
        assertEquals(10, Levels.strengthXp(java.util.Arrays.asList(gym(0, 3), gym(0, 2))));
    }

    @Test public void aSingleDayCannotBeFarmedForever() {
        // 100 sorozat egy napon is csak a napi maximumot hozza.
        assertEquals(Levels.MAX_STRENGTH_DAY_XP, Levels.strengthXp(
                java.util.Arrays.asList(gym(0, 100))));
        // A határ NAPONKÉNT él, két nap kétszer annyit hozhat.
        assertEquals(2L * Levels.MAX_STRENGTH_DAY_XP, Levels.strengthXp(
                java.util.Arrays.asList(gym(0, 100), gym(1, 100))));
    }

    @Test public void aGymDayIsWorthAboutAsMuchAsARun() {
        // 15 sorozat (≈5 gyakorlat) + az egyesített naplóból járó alap-XP
        // legyen egy nagyságrendben egy félórás edzéssel – különben a
        // súlyzózás láthatatlan maradna a szintekben.
        long gymDay = Levels.strengthXp(java.util.Arrays.asList(gym(0, 15)))
                + Levels.xpForSession(0, -1);
        long halfHourRun = Levels.xpForSession(1800, -1);
        assertTrue("a termes nap nem érhet nagyságrenddel kevesebbet",
                gymDay >= halfHourRun / 2 && gymDay <= halfHourRun * 2);
    }

    @Test public void xpToNextCountsDownToTheThreshold() {
        long need = Levels.xpForLevel(5);
        assertEquals("egy XP hiányzik a szintlépéshez", 1, Levels.xpToNext(need - 1));
        assertTrue("az új szinten már a következő küszöb a cél", Levels.xpToNext(need) > 0);
    }
}
