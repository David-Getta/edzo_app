package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tartások: ahol az „ismétlés” valójában másodperc.
 *
 * A plankot mindenki időre csinálja, a napló mégis ismétlésként kezelte: a
 * lista „0 kg × 60”-at írt ki, a progresszió pedig egy ismétlést – vagyis egy
 * másodpercet – javasolt, húsz fölött meg azt mondta, hogy ennyi ismétlésnél
 * már az állóképesség fejlődik. Egy perc plank után ez nem tanács, hanem zaj.
 *
 * A besorolás szándékosan szűk: egy tévesen tartásnak vett gyakorlat CSENDBEN
 * adna rossz javaslatot – tíz másodpercet ott, ahol egy ismétlés kellene.
 */
public class HoldsTest {

    private static StrengthLog.Entry entry(String name, long ts, int sets, int reps, double w) {
        List<StrengthLog.SetEntry> ss = new ArrayList<>();
        for (int i = 0; i < sets; i++) ss.add(new StrengthLog.SetEntry(reps, w));
        return new StrengthLog.Entry(ts, name, ss);
    }

    @Test public void theKnownHoldsAreRecognised() {
        for (String n : new String[]{"Plank", "plank", "alkartámasz", "Oldaltámasz",
                "fal ülés", "wall sit", "holt függés", "dead hang", "hollow hold",
                "Izometriás tartás", "statikus guggolás", "hasi vákuum"})
            assertTrue(n, StrengthParse.isTimed(n));
    }

    @Test public void theOrdinaryExercisesAreNotHolds() {
        // Minden ismert gyakorlat, ami NEM tartás – itt egy téves találat
        // rögtön rossz progresszió-javaslatot adna.
        List<String> holds = Arrays.asList("Plank", "Fal-ülés", "Holt függés");
        for (String n : StrengthParse.names()) {
            if (holds.contains(n)) continue;
            assertFalse(n, StrengthParse.isTimed(n));
        }
        for (String n : holds) assertTrue(n, StrengthParse.isTimed(n));
        for (String n : new String[]{"Guggolás", "Fekvőtámasz", "Húzódzkodás",
                "Bicepsz", "Superman", "Hasprés", "Kitörés", "Evezés", null, ""})
            assertFalse(String.valueOf(n), StrengthParse.isTimed(n));
    }

    @Test public void aHoldIsShownInSecondsNotInReps() {
        assertEquals("45 mp", StrengthLog.setLabel("Plank", new StrengthLog.SetEntry(45, 0)));
        assertEquals("1:00", StrengthLog.setLabel("Plank", new StrengthLog.SetEntry(60, 0)));
        assertEquals("2:05", StrengthLog.setLabel("Plank", new StrengthLog.SetEntry(125, 0)));
        // Testsúlyos ismétlésnél nem írunk oda nulla kilót – az nem információ.
        assertEquals("12 ism.",
                StrengthLog.setLabel("Fekvőtámasz", new StrengthLog.SetEntry(12, 0)));
        assertEquals("60 kg × 8",
                StrengthLog.setLabel("Guggolás", new StrengthLog.SetEntry(8, 60)));
    }

    @Test public void theSentencePreviewSaysSeconds() {
        List<StrengthParse.Item> it = StrengthParse.parse("alkartámasz 3x60");
        assertEquals(1, it.size());
        assertEquals("Plank  ·  3×60 mp  ·  saját testsúly", it.get(0).label());
    }

    @Test public void theHoldGrowsInSecondsNotInReps() {
        List<StrengthLog.Entry> log = new ArrayList<>();
        log.add(entry("Plank", 2_000, 3, 45, 0));
        Progression.Suggestion s = Progression.next(log, "Plank");
        assertTrue(s.timed);
        assertEquals(3, s.sets);
        assertEquals(45 + Progression.HOLD_STEP, s.reps);
        assertEquals("3 × 55 mp", s.headline());
    }

    @Test public void aboveTwoMinutesTheSetCountGrows() {
        List<StrengthLog.Entry> log = new ArrayList<>();
        log.add(entry("Plank", 2_000, 3, Progression.HOLD_MAX, 0));
        Progression.Suggestion s = Progression.next(log, "Plank");
        assertTrue(s.timed);
        assertEquals(4, s.sets);
        assertEquals(Progression.HOLD_MAX, s.reps);
        assertEquals("4 × 2:00", s.headline());
    }

    /**
     * A javaslat követése: a plank nem futhat el a végtelenbe. A testsúlyos ág
     * húsz ismétlésnél állt meg – ott ez a plank huszadik másodperce lett volna.
     */
    @Test public void followingTheAdviceStaysSane() {
        List<StrengthLog.Entry> log = new ArrayList<>();
        log.add(entry("Plank", 1_000, 3, 30, 0));
        int sets = 3, sec = 30;
        for (int i = 0; i < 40; i++) {
            Progression.Suggestion s = Progression.next(log, "Plank");
            assertTrue(s.timed);
            sets = s.sets; sec = s.reps;
            log.add(0, entry("Plank", 2_000 + i * 1_000L, sets, sec, 0));
        }
        assertTrue("nem futhat el: " + sets + "×" + sec,
                sec <= Progression.HOLD_MAX && sets <= Progression.BW_MAX_SETS);
    }

    @Test public void theHoldRestIsAMinute() {
        assertEquals(60, Progression.restSeconds(60, true, true));
        // Tartás nélkül a régi sáv marad érvényben.
        assertEquals(Progression.restSeconds(8, false),
                Progression.restSeconds(8, false, false));
        assertTrue(Progression.restWhy(60, true).contains("Tartás"));
        assertEquals(Progression.restWhy(8), Progression.restWhy(8, false));
    }

    @Test public void theBodyweightRecordsAreFound() {
        List<Bests.Best> b = Bests.ofLifts(
                new long[]{1_000, 2_000, 3_000, 4_000},
                new String[]{"Fekvőtámasz", "Fekvőtámasz", "Plank", "Plank"},
                new double[]{0, 0, 0, 0},
                new int[]{30, 42, 60, 95});
        assertEquals(2, b.size());
        assertEquals("Legtöbb ismétlés  ·  Fekvőtámasz", b.get(0).label);
        assertEquals("42 db", b.get(0).value);
        assertEquals(2_000, b.get(0).ts);
        assertEquals("Leghosszabb tartás  ·  Plank", b.get(1).label);
        assertEquals("1:35", b.get(1).value);
        assertEquals(4_000, b.get(1).ts);
    }

    @Test public void theFirstTryIsNotARecord() {
        // Öt fekvőtámasz és húsz másodperc plank nem kerül a kártyára.
        assertTrue(Bests.ofLifts(new long[]{1_000, 2_000},
                new String[]{"Fekvőtámasz", "Plank"}, new double[]{0, 0},
                new int[]{5, 20}).isEmpty());
    }

    /**
     * A tartás és az ismétlés nem versenyez egymással: egy két perces plank
     * nem verheti meg az ötven fekvőtámaszt, mert nem ugyanaz a mértékegység.
     */
    @Test public void theHoldNeverBeatsTheReps() {
        List<Bests.Best> b = Bests.ofLifts(
                new long[]{1_000, 2_000},
                new String[]{"Plank", "Fekvőtámasz"},
                new double[]{0, 0},
                new int[]{180, 50});
        assertEquals(2, b.size());
        assertEquals("50 db", b.get(0).value);
        assertEquals("3:00", b.get(1).value);
    }

    @Test public void theWeightedRecordsStillWork() {
        List<Bests.Best> b = Bests.ofLifts(
                new long[]{1_000, 2_000},
                new String[]{"Guggolás", "Fekvőtámasz"},
                new double[]{120, 0},
                new int[]{3, 40});
        assertEquals(3, b.size());
        assertTrue(b.get(0).label.startsWith("Legnehezebb emelés"));
        assertTrue(b.get(1).label.startsWith("Legjobb becsült 1RM"));
        assertEquals("40 db", b.get(2).value);
    }

    /**
     * A heti és havi összegzés is lássa a testsúlyos csúcsot: aki csak
     * fekvőtámaszozik és plankol, annak eddig egyetlen új rekordja sem jelent
     * meg, mert a rekordok kiló alatt indultak.
     */
    @Test public void theSummaryNoticesBodyweightRecords() {
        java.util.List<String> r = Bests.newRecordsSince(100,
                new long[]{1, 1, 1, 200, 200, 200},
                new String[]{"Guggolás", "Fekvőtámasz", "Plank",
                        "Guggolás", "Fekvőtámasz", "Plank"},
                new double[]{100, 0, 0, 110, 0, 0},
                new int[]{5, 40, 60, 5, 45, 90});
        assertEquals(3, r.size());
        assertEquals("Guggolás 110 kg", r.get(0));
        assertEquals("Fekvőtámasz 45 db", r.get(1));
        assertEquals("Plank 1:30", r.get(2));
    }

    @Test public void theSummaryStillNeedsSomethingToBeat() {
        // Első alkalom nem rekord, és a megismételt szám sem az.
        assertTrue(Bests.newRecordsSince(100, new long[]{200, 1, 200},
                new String[]{"Fekvőtámasz", "Plank", "Plank"},
                new double[]{0, 0, 0}, new int[]{40, 60, 60}).isEmpty());
        // Ismétlésszám nélkül hívva a régi viselkedés marad.
        assertTrue(Bests.newRecordsSince(100, new long[]{1, 200},
                new String[]{"Fekvőtámasz", "Fekvőtámasz"},
                new double[]{0, 0}).isEmpty());
    }
    /**
     * A lépés-rekordnak is kell alsó határ, és a napi volumen holtversenyét
     * nem a HashMap bejárási sorrendje dönti el.
     */
    @Test public void theStepAndVolumeRecordsAreSane() {
        // Háromszáz lépés nem „legtöbb lépés".
        assertTrue(Bests.of(new long[]{1_000}, new int[]{0}, new double[]{0},
                new double[]{0}, new int[]{300}, null, null).isEmpty());
        List<Bests.Best> b = Bests.of(new long[]{1_000}, new int[]{0}, new double[]{0},
                new double[]{0}, new int[]{Bests.MIN_STEPS}, null, null);
        assertEquals(1, b.size());
        assertEquals(Hu.num(Bests.MIN_STEPS), b.get(0).value);
        // Azonos napi volumennél a KORÁBBI nap a rekord: akkor érted el először.
        long d1 = Days.startOf(1_000_000_000_000L), d2 = d1 + 86_400_000L * 5;
        List<Bests.Best> v = Bests.of(new long[0], new int[0], new double[0],
                new double[0], new int[0], new long[]{d2, d1}, new double[]{5_000, 5_000});
        assertEquals(1, v.size());
        assertEquals(d1, v.get(0).ts);
    }
}
