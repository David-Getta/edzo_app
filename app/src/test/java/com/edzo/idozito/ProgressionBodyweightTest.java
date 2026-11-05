package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Testsúlyos gyakorlatok progressziója.
 *
 * Súlyzónál a sáv tetejéről a tárcsa visz tovább. Testsúlynál nincs tárcsa, és
 * a javaslat korábban egyszerűen minden alkalommal egy ismétléssel többet
 * kért – megállás nélkül. Aki követte, az néhány hónap alatt 3 × 40
 * fekvőtámaszig jutott: onnantól már nem erőt edz, hanem állóképességet, és a
 * javaslat sosem szólt, hogy ideje továbblépni.
 *
 * Az itteni tesztek a JAVASLAT KÖVETÉSÉT játsszák végig, mert a hiba csak így
 * látszik: egyetlen javaslatra ránézve mindegyik értelmes volt.
 */
public class ProgressionBodyweightTest {

    private static StrengthLog.Entry entry(long ts, int sets, int reps, double w) {
        List<StrengthLog.SetEntry> ss = new ArrayList<>();
        for (int i = 0; i < sets; i++) ss.add(new StrengthLog.SetEntry(reps, w));
        return new StrengthLog.Entry(ts, "Fekvőtámasz", ss);
    }

    /** Végigjátssza, hogy hova jut az, aki mindig a javaslatot követi. */
    private static Progression.Suggestion follow(int sessions, int sets, int reps, double w) {
        List<StrengthLog.Entry> log = new ArrayList<>();     // legújabb elöl
        Progression.Suggestion s = null;
        long ts = 1_600_000_000_000L;
        for (int i = 0; i < sessions; i++) {
            log.add(0, entry(ts, sets, reps, w));
            ts += 3L * 24 * 3600 * 1000;
            s = Progression.next(log, "Fekvőtámasz");
            assertTrue("elfogyott a javaslat a " + (i + 1) + ". alkalomnál", s != null);
            assertTrue("az ismétlésszám elszaladt: " + s.reps, s.reps <= Progression.BW_MAX_REPS);
            assertTrue("a sorozatszám elszaladt: " + s.sets, s.sets <= Progression.BW_MAX_SETS);
            sets = s.sets; reps = s.reps; w = s.weight;
        }
        return s;
    }

    @Test public void followingTheAdviceDoesNotRunAwayInReps() {
        // 40 alkalom bőven elég ahhoz, hogy a régi kód 3 × 50-nél járjon.
        Progression.Suggestion s = follow(40, 3, 10, 0);
        assertEquals(Progression.BW_MAX_SETS, s.sets);
        assertEquals(Progression.BW_MAX_REPS, s.reps);
        assertTrue("nem szól a nehezebb változatról: " + s.why, s.why.contains("nehezebb változat"));
    }

    @Test public void atTheRepCapItAddsASetAndKeepsTheReps() {
        List<StrengthLog.Entry> log = new ArrayList<>();
        log.add(entry(2_000, 3, Progression.BW_MAX_REPS, 0));
        Progression.Suggestion s = Progression.next(log, "Fekvőtámasz");
        assertTrue(s.bodyweight);
        assertEquals(4, s.sets);
        assertEquals(Progression.BW_MAX_REPS, s.reps);
        assertTrue("nem magyarázza meg, miért: " + s.why, s.why.contains("állóképesség"));
    }

    @Test public void belowTheCapItStillAddsOneRep() {
        List<StrengthLog.Entry> log = new ArrayList<>();
        log.add(entry(2_000, 3, 12, 0));
        Progression.Suggestion s = Progression.next(log, "Fekvőtámasz");
        assertEquals(3, s.sets);
        assertEquals(13, s.reps);
    }

    @Test public void aStallBelowTheCapStillAsksForAnExtraSet() {
        // Aki nem tud több ismétlést, annak a volumen visz előre – ez megmarad.
        List<StrengthLog.Entry> log = new ArrayList<>();
        for (int i = 0; i < Progression.STALL_SESSIONS; i++)
            log.add(entry(9_000 - i * 1_000L, 3, 12, 0));
        Progression.Suggestion s = Progression.next(log, "Fekvőtámasz");
        assertEquals(4, s.sets);
        assertEquals(12, s.reps);
    }

    @Test public void atBothCapsTheSuggestionStopsChanging() {
        List<StrengthLog.Entry> log = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            log.add(entry(9_000 - i * 1_000L, Progression.BW_MAX_SETS, Progression.BW_MAX_REPS, 0));
        Progression.Suggestion s = Progression.next(log, "Fekvőtámasz");
        assertEquals(Progression.BW_MAX_SETS, s.sets);
        assertEquals(Progression.BW_MAX_REPS, s.reps);
        assertEquals(0, s.weight, 0.001);
    }

    @Test public void weightedExercisesAreUntouched() {
        // Súlyzónál a sáv teteje a tárcsát hozza, nem sorozatot – változatlanul.
        List<StrengthLog.Entry> log = new ArrayList<>();
        log.add(entry(2_000, 3, Progression.MAX_REPS, 40));
        Progression.Suggestion s = Progression.next(log, "Fekvőtámasz");
        assertTrue(!s.bodyweight);
        assertEquals(3, s.sets);
        assertEquals(Progression.MIN_REPS, s.reps);
        assertEquals(42.5, s.weight, 0.001);

        // És 20 ismétlés súllyal sem vált sorozatra: ott a súly a jelzés.
        List<StrengthLog.Entry> high = new ArrayList<>();
        high.add(entry(2_000, 3, 20, 40));
        Progression.Suggestion h = Progression.next(high, "Fekvőtámasz");
        assertEquals(3, h.sets);
        assertEquals(42.5, h.weight, 0.001);
    }
}
