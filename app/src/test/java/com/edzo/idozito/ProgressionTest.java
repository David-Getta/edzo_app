package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A progresszió-javaslat. Ha ez téved, a felhasználó rossz súllyal áll neki –
 * ezért minden ága le van fedve: ismétlés-lépés, súlylépés, bemelegítő sorozat
 * kiszűrése, testsúlyos eset és a beragadás utáni visszavétel.
 */
public class ProgressionTest {

    /** Egy alkalom: „reps@weight" párokból. A napló legújabb bejegyzése az első. */
    private static StrengthLog.Entry entry(String name, double[]... repsWeight) {
        List<StrengthLog.SetEntry> sets = new ArrayList<>();
        for (double[] rw : repsWeight) sets.add(new StrengthLog.SetEntry((int) rw[0], rw[1]));
        return new StrengthLog.Entry(0, name, sets);
    }

    private static double[] set(int reps, double w) { return new double[]{reps, w}; }

    private static List<StrengthLog.Entry> log(StrengthLog.Entry... e) {
        return new ArrayList<>(Arrays.asList(e));
    }

    // --- Nincs mihez mérni ---

    @Test public void unknownExerciseHasNoSuggestion() {
        assertNull(Progression.next(log(), "Guggolás"));
        assertNull(Progression.next(log(entry("Fekvenyomás", set(10, 40))), "Guggolás"));
    }

    @Test public void anEmptySessionIsIgnored() {
        assertNull(Progression.next(log(entry("Guggolás")), "Guggolás"));
    }

    // --- Ismétlés-lépés a sávon belül ---

    @Test public void belowTheTopOfTheRangeRepsGoUp() {
        Progression.Suggestion s = Progression.next(
                log(entry("Guggolás", set(10, 50), set(10, 50), set(10, 50))), "Guggolás");
        assertEquals(50.0, s.weight, 0.001);
        assertEquals(11, s.reps);
        assertEquals(3, s.sets);
    }

    @Test public void theWeakestSetSetsThePace() {
        // 12, 12, majd 9: a szűk keresztmetszet a 9 – még nem jár súlyemelés.
        Progression.Suggestion s = Progression.next(
                log(entry("Guggolás", set(12, 50), set(12, 50), set(9, 50))), "Guggolás");
        assertEquals(50.0, s.weight, 0.001);
        assertEquals(10, s.reps);
    }

    // --- Súlylépés a sáv tetején ---

    @Test public void atTheTopOfTheRangeTheWeightGoesUp() {
        Progression.Suggestion s = Progression.next(
                log(entry("Guggolás", set(12, 50), set(12, 50), set(12, 50))), "Guggolás");
        assertEquals(52.5, s.weight, 0.001);
        assertEquals(8, s.reps);
        assertEquals("3 × 8 · 52,5 kg", s.headline());
    }

    @Test public void lightWeightsStepInSmallerPlates() {
        Progression.Suggestion s = Progression.next(
                log(entry("Bicepsz", set(12, 15), set(12, 15))), "Bicepsz");
        assertEquals(16.25, s.weight, 0.001);
        assertEquals("2 × 8 · 16,25 kg", s.headline());
    }

    // --- Bemelegítő sorozat ---

    @Test public void warmUpSetsDoNotCount() {
        // 20 kg-os bemelegítés után 40 kg a munkasúly; ott a leggyengébb sorozat 8.
        Progression.Suggestion s = Progression.next(
                log(entry("Fekvenyomás", set(15, 20), set(8, 40), set(9, 40))), "Fekvenyomás");
        assertEquals(40.0, s.weight, 0.001);
        assertEquals(9, s.reps);
    }

    // --- Testsúlyos gyakorlat ---

    @Test public void bodyweightProgressesInRepsOnly() {
        Progression.Suggestion s = Progression.next(
                log(entry("Húzódzkodás", set(8, 0), set(7, 0), set(6, 0))), "Húzódzkodás");
        assertTrue(s.bodyweight);
        assertEquals(0.0, s.weight, 0.001);
        assertEquals(7, s.reps);            // a leggyengébb sorozat (6) + 1
        assertEquals("3 × 7 ismétlés", s.headline());
    }

    // --- Beragadás ---

    @Test public void threeIdenticalSessionsTriggerADeload() {
        StrengthLog.Entry same = entry("Guggolás", set(10, 50), set(10, 50));
        Progression.Suggestion s = Progression.next(log(same, same, same), "Guggolás");
        assertEquals(45.0, s.weight, 0.001);
        assertEquals(8, s.reps);
        assertTrue(s.why.contains("Vegyél vissza"));
    }

    @Test public void twoIdenticalSessionsAreNotYetAStall() {
        StrengthLog.Entry same = entry("Guggolás", set(10, 50), set(10, 50));
        Progression.Suggestion s = Progression.next(log(same, same), "Guggolás");
        assertEquals(50.0, s.weight, 0.001);
        assertEquals(11, s.reps);
    }

    @Test public void aStallIsBrokenByADifferentSession() {
        StrengthLog.Entry same = entry("Guggolás", set(10, 50), set(10, 50));
        StrengthLog.Entry other = entry("Guggolás", set(9, 50), set(9, 50));
        // A legutóbbi kettő egyforma, a harmadik nem – nem beragadás.
        Progression.Suggestion s = Progression.next(log(same, same, other, same), "Guggolás");
        assertEquals(50.0, s.weight, 0.001);
        assertEquals(11, s.reps);
    }

    @Test public void otherExercisesBetweenDoNotBreakTheStall() {
        StrengthLog.Entry same = entry("Guggolás", set(10, 50), set(10, 50));
        StrengthLog.Entry other = entry("Evezés", set(10, 30));
        Progression.Suggestion s =
                Progression.next(log(same, other, same, other, same), "Guggolás");
        assertEquals(45.0, s.weight, 0.001);
    }

    @Test public void bodyweightStallAsksForAnExtraSet() {
        StrengthLog.Entry same = entry("Fekvőtámasz", set(20, 0), set(20, 0));
        Progression.Suggestion s = Progression.next(log(same, same, same), "Fekvőtámasz");
        assertTrue(s.bodyweight);
        assertEquals(3, s.sets);
        assertEquals(20, s.reps);
    }

    // --- Segédfüggvények ---

    @Test public void deloadRoundsToRealPlates() {
        assertEquals(45.0, Progression.deload(50), 0.001);
        assertEquals(37.5, Progression.deload(42.5), 0.001);
        // 10% kevesebb, mint egy tárcsa → legalább egy tárcsával lejjebb.
        assertEquals(3.75, Progression.deload(5), 0.001);
        // Nincs hova lejjebb.
        assertEquals(0.0, Progression.deload(1.25), 0.001);
    }

    @Test public void weightsAreFormattedHungarianStyle() {
        assertEquals("50", Progression.kg(50));
        assertEquals("52,5", Progression.kg(52.5));
        assertEquals("16,25", Progression.kg(16.25));
    }
}
