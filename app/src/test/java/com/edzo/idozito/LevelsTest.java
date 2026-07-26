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

    @Test public void xpToNextCountsDownToTheThreshold() {
        long need = Levels.xpForLevel(5);
        assertEquals("egy XP hiányzik a szintlépéshez", 1, Levels.xpToNext(need - 1));
        assertTrue("az új szinten már a következő küszöb a cél", Levels.xpToNext(need) > 0);
    }
}
