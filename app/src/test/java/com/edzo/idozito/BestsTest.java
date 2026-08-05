package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * Személyes csúcsok.
 *
 * Egy rosszul kiszámolt rekord évekig ott marad a kártyán – és ami rosszabb,
 * elérhetetlenné tesz egy valódi rekordot, mert soha nem lehet megdönteni.
 */
public class BestsTest {

    private static final long DAY = 86400000L;
    private static final long T0 = 1770000000000L;

    private static String find(List<Bests.Best> l, String label) {
        for (Bests.Best b : l) if (b.label.equals(label)) return b.value;
        return null;
    }

    @Test public void theBestsAreFoundWithTheirDate() {
        long[] ts = {T0, T0 - DAY, T0 - 2 * DAY};
        int[] dur = {3600, 1800, 300};
        double[] dist = {10000, 5000, 200};
        double[] cal = {700, 400, 30};
        int[] steps = {12000, 6000, 400};
        List<Bests.Best> b = Bests.of(ts, dur, dist, cal, steps, null, null);
        assertEquals("1 ó 0 p", find(b, "Leghosszabb edzés"));
        assertEquals("10,0 km", find(b, "Leghosszabb táv"));
        assertEquals("12000", find(b, "Legtöbb lépés"));
        assertEquals("700 kcal", find(b, "Legtöbb kalória"));
        // A leghosszabb edzés napja is megvan.
        for (Bests.Best x : b) if (x.label.equals("Leghosszabb edzés")) assertEquals(T0, x.ts);
    }

    @Test public void theFastestPaceIsTheSmallestNumber() {
        // 10 km 50 perc = 5:00/km; 5 km 30 perc = 6:00/km. A gyorsabb az 5:00.
        long[] ts = {T0, T0 - DAY};
        int[] dur = {3000, 1800};
        double[] dist = {10000, 5000};
        List<Bests.Best> b = Bests.of(ts, dur, dist, null, null, null, null);
        assertEquals("5:00 /km", find(b, "Leggyorsabb tempó"));
    }

    @Test public void nonsenseValuesAreNotRecords() {
        // 200 méter 3 másodperc alatt: mérési hiba, nem világrekord.
        long[] ts = {T0, T0 - DAY};
        int[] dur = {3, 3000};
        double[] dist = {200, 10000};
        List<Bests.Best> b = Bests.of(ts, dur, dist, null, null, null, null);
        assertEquals("5:00 /km", find(b, "Leggyorsabb tempó"));
        // A rövid próbálkozás nem „leghosszabb edzés".
        List<Bests.Best> only = Bests.of(new long[]{T0}, new int[]{120},
                null, null, null, null, null);
        assertEquals(null, find(only, "Leghosszabb edzés"));
        assertTrue(only.isEmpty());
    }

    @Test public void theDailyVolumeSumsTheWholeDay() {
        // Ugyanaznap három bejegyzés egy edzés: 3000 + 2000 + 1000 = 6000 kg.
        long[] lts = {T0, T0 + 3600000, T0 + 7200000, T0 - 5 * DAY};
        double[] vol = {3000, 2000, 1000, 5500};
        List<Bests.Best> b = Bests.of(null, null, null, null, null, lts, vol);
        assertEquals("6000 kg", find(b, "Legnagyobb napi volumen"));
    }

    @Test public void missingDataNeverCrashes() {
        assertTrue(Bests.of(null, null, null, null, null, null, null).isEmpty());
        assertTrue(Bests.of(new long[0], new int[0], new double[0], new double[0],
                new int[0], new long[0], new double[0]).isEmpty());
        // Eltérő hosszú tömbök: a rövidebb dönt, kivétel nélkül.
        List<Bests.Best> b = Bests.of(new long[]{T0, T0 - DAY}, new int[]{3600},
                new double[]{}, null, null, new long[]{T0}, new double[]{});
        assertEquals("1 ó 0 p", find(b, "Leghosszabb edzés"));
        // Negatív volumen nem növeli a napot.
        assertEquals(null, find(Bests.of(null, null, null, null, null,
                new long[]{T0}, new double[]{-500}), "Legnagyobb napi volumen"));
    }

    @Test public void theLabelsAreReadable() {
        assertEquals("48 perc", Bests.fmtDur(2880));
        assertEquals("1 ó 0 p", Bests.fmtDur(3600));
        assertEquals("2 ó 5 p", Bests.fmtDur(7500));
        assertEquals("5:00", Bests.fmtPace(5.0));
        assertEquals("4:30", Bests.fmtPace(4.5));
        assertEquals("6:05", Bests.fmtPace(6.0833));
    }

    @Test public void theHeaviestLiftAndTheBestOneRmAreFound() {
        long d = 86400000L;
        long[] ts = {1000 * d, 1000 * d, 1010 * d, 1010 * d};
        String[] names = {"Guggolás", "Fekvenyomás", "Guggolás", "Fekvenyomás"};
        double[] w = {100, 80, 120, 85};
        int[] r = {5, 8, 1, 5};
        List<Bests.Best> b = Bests.ofLifts(ts, names, w, r);
        assertEquals(2, b.size());
        // A legnehezebb sorozat a 120 kg-os guggolás.
        assertTrue(b.get(0).label.contains("Guggolás"));
        assertEquals("120 kg × 1", b.get(0).value);
        assertEquals(1010 * d, b.get(0).ts);
        // A legjobb becsült 1RM viszont a 100×5 (Epley: 116,7) ellenében a
        // 120×1 (120) – ugyanaz a nap.
        assertTrue(b.get(1).label.contains("Guggolás"));
        assertEquals("120 kg", b.get(1).value);
    }

    @Test public void theOneRmPrefersTheBetterEstimate() {
        // 100 kg × 5 (Epley 116,7) erősebb, mint 110 kg × 1 (110) – a
        // legnehezebb sorozat viszont a 110.
        long[] ts = {1, 2};
        String[] names = {"Felhúzás", "Felhúzás"};
        List<Bests.Best> b = Bests.ofLifts(ts, names, new double[]{100, 110}, new int[]{5, 1});
        assertEquals("110 kg × 1", b.get(0).value);
        assertEquals("117 kg", b.get(1).value);
    }

    @Test public void lightSetsAreNotRecords() {
        // A 20 kilós bicepsz rekordja senkit nem érdekel.
        assertTrue(Bests.ofLifts(new long[]{1}, new String[]{"Bicepsz"},
                new double[]{20}, new int[]{10}).isEmpty());
        // Magas ismétlésszámra nem becslünk 1RM-et (a képlet ott túlbecsül),
        // de a legnehezebb sorozat attól még megvan.
        List<Bests.Best> b = Bests.ofLifts(new long[]{1}, new String[]{"Guggolás"},
                new double[]{60}, new int[]{20});
        assertEquals(1, b.size());
        assertTrue(b.get(0).label.contains("Legnehezebb"));
    }

    @Test public void brokenLiftInputNeverCrashes() {
        assertTrue(Bests.ofLifts(null, null, null, null).isEmpty());
        assertTrue(Bests.ofLifts(new long[]{1}, new String[]{null},
                new double[]{100}, new int[]{5}).isEmpty());
        assertTrue(Bests.ofLifts(new long[]{1}, new String[]{"  "},
                new double[]{100}, new int[]{5}).isEmpty());
        // Eltérő hosszú tömbök: a rövidebb dönt.
        assertTrue(Bests.ofLifts(new long[]{1, 2, 3}, new String[]{"Guggolás"},
                new double[]{}, new int[]{5}).isEmpty());
        // Képtelen értékek kiesnek.
        assertTrue(Bests.ofLifts(new long[]{1}, new String[]{"Guggolás"},
                new double[]{5000}, new int[]{5}).isEmpty());
        assertTrue(Bests.ofLifts(new long[]{1}, new String[]{"Guggolás"},
                new double[]{100}, new int[]{0}).isEmpty());
    }

    @Test public void aNewAllTimeRecordIsRecognised() {
        long week = 7 * 86400000L;
        long since = 100 * week;
        long[] ts = {50 * week, 50 * week, 101 * week, 101 * week};
        String[] names = {"Guggolás", "Fekvenyomás", "Guggolás", "Fekvenyomás"};
        double[] w = {100, 80, 120, 75};
        List<String> r = Bests.newRecordsSince(since, ts, names, w);
        // Csak a guggolás dőlt meg; a fekvenyomás 75-je nem éri el a 80-at.
        assertEquals(1, r.size());
        assertEquals("Guggolás 120 kg", r.get(0));
    }

    @Test public void theFirstTimeIsNotARecord() {
        // Nem volt mihez mérni: az első alkalom nem rekord, különben minden
        // új gyakorlat rögtön ünneplést kapna.
        assertTrue(Bests.newRecordsSince(10, new long[]{20}, new String[]{"Guggolás"},
                new double[]{100}).isEmpty());
        // Az azonos súly sem rekord – megismételni nem megdönteni.
        assertTrue(Bests.newRecordsSince(10, new long[]{1, 20},
                new String[]{"Guggolás", "Guggolás"}, new double[]{100, 100}).isEmpty());
    }

    @Test public void theHeaviestRecordComesFirst() {
        List<String> r = Bests.newRecordsSince(10,
                new long[]{1, 1, 20, 20},
                new String[]{"Bicepsz", "Guggolás", "Bicepsz", "Guggolás"},
                new double[]{30, 100, 40, 120});
        assertEquals(2, r.size());
        assertEquals("Guggolás 120 kg", r.get(0));
        assertEquals("Bicepsz 40 kg", r.get(1));
    }

    @Test public void brokenRecordInputNeverCrashes() {
        assertTrue(Bests.newRecordsSince(0, null, null, null).isEmpty());
        assertTrue(Bests.newRecordsSince(0, new long[]{1, 2}, new String[]{null, "  "},
                new double[]{100, 100}).isEmpty());
        // Könnyű sorozatból nincs rekord.
        assertTrue(Bests.newRecordsSince(10, new long[]{1, 20},
                new String[]{"Bicepsz", "Bicepsz"}, new double[]{10, 20}).isEmpty());
    }
}
