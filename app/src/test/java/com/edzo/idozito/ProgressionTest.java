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
    @Test public void theRestSuggestionFollowsTheRepRange() {
        // Nehéz sorozat: hosszabb pihenő; tömegépítő sáv: rövidebb.
        assertEquals(180, Progression.restSeconds(5, false));
        assertEquals(150, Progression.restSeconds(8, false));
        assertEquals(90, Progression.restSeconds(12, false));
        assertEquals(60, Progression.restSeconds(20, false));
        // Testsúlyosnál kisebb a terhelés, rövidebb a pihenő.
        assertEquals(150, Progression.restSeconds(5, true));
        assertEquals(60, Progression.restSeconds(12, true));
        // Ismeretlen ismétlésszámra biztonságos alapérték.
        assertEquals(90, Progression.restSeconds(0, false));
        // Minden sávhoz tartozik indoklás, és a pihenő életszerű marad.
        for (int r = 1; r <= 50; r++) {
            int s = Progression.restSeconds(r, r % 2 == 0);
            assertTrue("pihenő: " + r + " → " + s, s >= 45 && s <= 180);
            assertTrue(!Progression.restWhy(r).isEmpty());
        }
        assertEquals("", Progression.restWhy(0));
    }
    @Test public void theFeltEffortSteersTheNextStep() {
        // Könnyű nap (RPE 7): ne ismétléssel araszoljunk, jöjjön a tárcsa.
        List<StrengthLog.Entry> easy = new java.util.ArrayList<>();
        easy.add(entry("Guggolás", 0, 7, 3, 8, 100));
        Progression.Suggestion s = Progression.next(easy, "Guggolás");
        assertEquals(102.5, s.weight, 0.001);
        assertEquals(8, s.reps);
        assertTrue(s.why.contains("maradt a tankban"));

        // A határon (RPE 10): ugyanez jöjjön újra, ne toljuk tovább.
        List<StrengthLog.Entry> hard = new java.util.ArrayList<>();
        hard.add(entry("Guggolás", 0, 10, 3, 8, 100));
        Progression.Suggestion h = Progression.next(hard, "Guggolás");
        assertEquals(100.0, h.weight, 0.001);
        assertEquals(8, h.reps);
        assertTrue(h.why.contains("határon"));

        // RPE nélkül a régi viselkedés: egy ismétléssel több.
        List<StrengthLog.Entry> plain = new java.util.ArrayList<>();
        plain.add(entry("Guggolás", 0, 0, 3, 8, 100));
        Progression.Suggestion p = Progression.next(plain, "Guggolás");
        assertEquals(100.0, p.weight, 0.001);
        assertEquals(9, p.reps);
    }

    /** Egy bejegyzés adott napra, RPE-vel és azonos sorozatokkal. */
    private static StrengthLog.Entry entry(String name, int daysAgo, int rpe,
                                           int sets, int reps, double kg) {
        java.util.List<StrengthLog.SetEntry> l = new java.util.ArrayList<>();
        for (int i = 0; i < sets; i++) l.add(new StrengthLog.SetEntry(reps, kg));
        long ts = System.currentTimeMillis() - daysAgo * 86400000L;
        return new StrengthLog.Entry(ts, name, l, rpe);
    }

    @Test public void theOneRepMaxIsHonestAtOneRep() {
        // Egy ismétlésnél a súly MAGA az egy ismétléses maximum – az eredeti
        // Epley ott 3,3%-kal fölé lőne, és egy valódi szingli rekordját írná
        // felül egy kitalált, nagyobb számmal.
        assertEquals(120.0, Progression.oneRm(120, 1), 0.001);
        // Efölött a szokásos képlet.
        assertEquals(100 * (1 + 5 / 30.0), Progression.oneRm(100, 5), 0.001);
        assertEquals(80 * (1 + 10 / 30.0), Progression.oneRm(80, 10), 0.001);
        // Több ismétlés ugyanazzal a súllyal mindig nagyobb becslés.
        for (int r = 1; r < 12; r++)
            assertTrue(Progression.oneRm(100, r + 1) > Progression.oneRm(100, r));
        // Képtelen bemenet: nulla, nem kivétel.
        assertEquals(0.0, Progression.oneRm(0, 5), 0.001);
        assertEquals(0.0, Progression.oneRm(100, 0), 0.001);
        assertEquals(0.0, Progression.oneRm(-10, 5), 0.001);
    }

    @Test public void theTrendComparesTheFirstAndLastThird() {
        // A rekord azt mondja meg, hol a plafon; ez azt, hogy MERRE tartasz.
        long d = 86400000L, now = 1000 * d;
        java.util.List<StrengthLog.Entry> log = new java.util.ArrayList<>();
        // Legújabb elöl: 100, 97,5, 95, 90, 85, 80.
        double[] w = {100, 97.5, 95, 90, 85, 80};
        for (int i = 0; i < w.length; i++)
            log.add(entry(now - i * 7L * d, "Guggolás", 3, 5, w[i]));
        // Hat alkalom → kettesével átlagol: a két legrégebbi (80, 85) és a két
        // legújabb (100, 97,5) átlaga. Egyetlen jó vagy rossz nap így nem
        // billenti el a képet.
        double[] p = Progression.progress(log, "Guggolás", now, 90);
        assertEquals(82.5, p[0], 0.001);
        assertEquals(98.75, p[1], 0.001);
        assertTrue("felfelé tart", p[1] > p[0]);
    }

    @Test public void theTrendNeedsEnoughSessions() {
        long d = 86400000L, now = 1000 * d;
        java.util.List<StrengthLog.Entry> log = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++)
            log.add(entry(now - i * 7L * d, "Guggolás", 3, 5, 80 + i));
        // Két-három pont nem tendencia.
        assertNull(Progression.progress(log, "Guggolás", now, 90));
        // Ablakon kívüli alkalmak nem számítanak.
        java.util.List<StrengthLog.Entry> old = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++)
            old.add(entry(now - (200L + i) * d, "Guggolás", 3, 5, 80));
        assertNull(Progression.progress(old, "Guggolás", now, 90));
        // Képtelen bemenet: null, nem kivétel.
        assertNull(Progression.progress(null, "Guggolás", now, 90));
        assertNull(Progression.progress(log, null, now, 90));
        assertNull(Progression.progress(log, "Guggolás", now, 0));
        // Testsúlyos (0 kg) alkalmakból nincs tendencia.
        java.util.List<StrengthLog.Entry> bw = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++)
            bw.add(entry(now - i * 7L * d, "Fekvőtámasz", 3, 20, 0));
        assertNull(Progression.progress(bw, "Fekvőtámasz", now, 90));
    }

    private static StrengthLog.Entry entry(long ts, String name, int sets, int reps,
                                           double weight) {
        java.util.List<StrengthLog.SetEntry> l = new java.util.ArrayList<>();
        for (int i = 0; i < sets; i++) l.add(new StrengthLog.SetEntry(reps, weight));
        return new StrengthLog.Entry(ts, name, l);
    }
}
