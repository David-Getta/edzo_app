package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A napi kihívás haladásának kiírása. A csonkolás itt kétszeresen fájt: a
 * felirat („2 / 3 km") és a haladássáv is kevesebbet mutatott a valóságnál,
 * pont a hajrá pillanatában.
 */
public class ChallengesTest {

    @Test public void wholeNumbersStayWhole() {
        assertEquals("0", Challenges.fmtProgress(0));
        assertEquals("2", Challenges.fmtProgress(2));
        assertEquals("15", Challenges.fmtProgress(15.0));
    }

    @Test public void fractionsGetOneDecimalWithAComma() {
        assertEquals("2,9", Challenges.fmtProgress(2.9));
        assertEquals("0,5", Challenges.fmtProgress(0.5));
        assertEquals("12,3", Challenges.fmtProgress(12.34));
    }

    @Test public void itNeverRoundsUpToTheGoal() {
        // A legfontosabb szabály: 2,99 km ne látszódjon 3 km-nek, mert az azt
        // sugallná, hogy megvan a feladat.
        assertEquals("2,9", Challenges.fmtProgress(2.99));
        assertEquals("2,9", Challenges.fmtProgress(2.999));
        assertEquals("0,9", Challenges.fmtProgress(0.98));
    }

    @Test public void tinyProgressIsStillVisible() {
        assertEquals("0,1", Challenges.fmtProgress(0.19));
        assertEquals("0", Challenges.fmtProgress(0.04));
    }

    @Test public void theOutputIsNeverEmptyOrJavaFormatted() {
        for (double v = 0; v < 20; v += 0.07) {
            String s = Challenges.fmtProgress(v);
            assertEquals("nem lehet benne tizedespont: " + s, -1, s.indexOf('.'));
            assertEquals("nem lehet tudományos jelölés: " + s, -1, s.indexOf('E'));
        }
    }
}
