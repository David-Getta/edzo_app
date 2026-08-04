package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Izomcsoport-besorolás. Rossz címke félrevezetőbb, mint a hiányzó, ezért két
 * dolgot mérünk: a felismert nevek jó helyre kerülnek, és a bizonytalan nevek
 * inkább besorolatlanok maradnak.
 */
public class MusclesTest {

    private static long daysAgo(int n) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -n);
        return c.getTimeInMillis();
    }

    private static StrengthLog.Entry at(String name, int daysAgoN) {
        List<StrengthLog.SetEntry> sets = new ArrayList<>();
        sets.add(new StrengthLog.SetEntry(10, 50));
        return new StrengthLog.Entry(daysAgo(daysAgoN), name, sets);
    }

    private static List<StrengthLog.Entry> log(StrengthLog.Entry... e) {
        return new ArrayList<>(Arrays.asList(e));
    }

    private static long now() { return System.currentTimeMillis(); }

    // --- Besorolás ---

    @Test public void theBuiltInExercisesAreAllClassified() {
        // A COMMON lista a felkínált gyakorlatokat tartalmazza – ha ezek közül
        // bármelyik besorolatlan marad, az a kártyán azonnal látszana.
        for (String n : StrengthLog.COMMON) {
            assertTrue("besorolatlan: " + n, Muscles.groupOf(n) != null);
        }
    }

    @Test public void bigLiftsLandInTheRightGroup() {
        assertEquals(Muscles.LAB, Muscles.groupOf("Guggolás"));
        assertEquals(Muscles.LAB, Muscles.groupOf("Kitörés"));
        assertEquals(Muscles.LAB, Muscles.groupOf("Lábtolás"));
        assertEquals(Muscles.LAB, Muscles.groupOf("Vádliemelés"));
        assertEquals(Muscles.HAT, Muscles.groupOf("Felhúzás"));
        assertEquals(Muscles.HAT, Muscles.groupOf("Evezés"));
        assertEquals(Muscles.HAT, Muscles.groupOf("Húzódzkodás"));
        assertEquals(Muscles.MELL, Muscles.groupOf("Fekvenyomás"));
        assertEquals(Muscles.MELL, Muscles.groupOf("Fekvőtámasz"));
        assertEquals(Muscles.VALL, Muscles.groupOf("Vállból nyomás"));
        assertEquals(Muscles.VALL, Muscles.groupOf("Oldalemelés"));
        assertEquals(Muscles.KAR, Muscles.groupOf("Bicepsz"));
        assertEquals(Muscles.KAR, Muscles.groupOf("Tricepsz"));
        assertEquals(Muscles.TORZS, Muscles.groupOf("Plank"));
    }

    @Test public void accentsAndCaseDoNotMatter() {
        assertEquals(Muscles.LAB, Muscles.groupOf("GUGGOLAS"));
        assertEquals(Muscles.HAT, Muscles.groupOf("húzódzkodás széles fogással"));
    }

    @Test public void shortWordsOnlyCountAsWholeWords() {
        assertEquals(Muscles.HAT, Muscles.groupOf("Hát gép"));
        assertEquals(Muscles.LAB, Muscles.groupOf("Láb nap"));
        // „labda" nem láb, „hatvan" nem hát – ezek maradjanak besorolatlanul.
        assertNull(Muscles.groupOf("Labdás gyakorlat"));
        assertNull(Muscles.groupOf("Hatvanas sorozat"));
    }

    @Test public void unknownNamesStayUnclassified() {
        assertNull(Muscles.groupOf("Valami új"));
        assertNull(Muscles.groupOf(""));
        assertNull(Muscles.groupOf(null));
    }

    // --- Heti egyensúly ---

    @Test public void onlyGroupsEverTrainedAppear() {
        LinkedHashMap<String, Integer> b = Muscles.weekBalance(
                log(at("Guggolás", 1), at("Evezés", 2)), now(), 7);
        assertEquals(2, b.size());
        assertTrue(b.containsKey(Muscles.LAB));
        assertTrue(b.containsKey(Muscles.HAT));
    }

    @Test public void aGroupMissedThisWeekShowsZero() {
        // A hát csak 20 napja volt: benne van a listában, de 0-val.
        LinkedHashMap<String, Integer> b = Muscles.weekBalance(
                log(at("Guggolás", 1), at("Evezés", 20)), now(), 7);
        assertEquals(Integer.valueOf(1), b.get(Muscles.LAB));
        assertEquals(Integer.valueOf(0), b.get(Muscles.HAT));
    }

    @Test public void severalExercisesOnOneDayCountAsOneDay() {
        LinkedHashMap<String, Integer> b = Muscles.weekBalance(
                log(at("Guggolás", 1), at("Kitörés", 1), at("Lábtolás", 1)), now(), 7);
        assertEquals(Integer.valueOf(1), b.get(Muscles.LAB));
    }

    @Test public void separateDaysCountSeparately() {
        LinkedHashMap<String, Integer> b = Muscles.weekBalance(
                log(at("Guggolás", 0), at("Guggolás", 2), at("Kitörés", 5)), now(), 7);
        assertEquals(Integer.valueOf(3), b.get(Muscles.LAB));
    }

    @Test public void theOrderIsBigGroupsFirst() {
        LinkedHashMap<String, Integer> b = Muscles.weekBalance(
                log(at("Bicepsz", 1), at("Guggolás", 1), at("Evezés", 1)), now(), 7);
        assertEquals(Arrays.asList(Muscles.LAB, Muscles.HAT, Muscles.KAR),
                new ArrayList<>(b.keySet()));
    }

    @Test public void unclassifiedExercisesAreIgnored() {
        LinkedHashMap<String, Integer> b = Muscles.weekBalance(
                log(at("Valami új", 1), at("Guggolás", 1)), now(), 7);
        assertEquals(1, b.size());
        assertEquals(Integer.valueOf(1), b.get(Muscles.LAB));
    }

    @Test public void anEmptyLogGivesAnEmptyBalance() {
        assertTrue(Muscles.weekBalance(log(), now(), 7).isEmpty());
        assertTrue(Muscles.weekBalance(null, now(), 7).isEmpty());
    }

    // --- Felsorolás ---

    @Test public void theMissingGroupsReadAsASentence() {
        assertEquals("Hát", Muscles.andList(Arrays.asList("Hát")));
        assertEquals("Hát és Váll", Muscles.andList(Arrays.asList("Hát", "Váll")));
        assertEquals("Hát, Váll és Kar",
                Muscles.andList(Arrays.asList("Hát", "Váll", "Kar")));
    }
    // --- Mai ajánlat ---

    @Test public void theNeglectedGroupsGetASuggestion() {
        // Láb és mell ment a héten, hát nem – a hátból a legrégebben csinált
        // gyakorlat jön vissza.
        List<StrengthLog.Entry> log = log(
                at("Guggolás", 1), at("Fekvenyomás", 2),
                at("Húzódzkodás", 20), at("Evezés", 40));
        List<String> s = Muscles.suggestForToday(log, now(), 3);
        assertEquals(1, s.size());
        assertEquals("Evezés", s.get(0));      // régebbi, mint a húzódzkodás
    }

    @Test public void aBalancedWeekGetsNoNagging() {
        List<StrengthLog.Entry> log = log(
                at("Guggolás", 1), at("Fekvenyomás", 2), at("Evezés", 3));
        assertTrue(Muscles.suggestForToday(log, now(), 3).isEmpty());
    }

    @Test public void onlyExercisesTheUserHasDoneAreSuggested() {
        // Amit sosem csinált, azt nem ajánljuk: nincs mihez mérni a súlyt.
        List<StrengthLog.Entry> log = log(
                at("Guggolás", 1), at("Evezés", 30), at("Fekvenyomás", 30));
        List<String> s = Muscles.suggestForToday(log, now(), 5);
        assertTrue(!s.isEmpty());
        for (String n : s)
            assertTrue("sosem csinálta: " + n, n.equals("Evezés") || n.equals("Fekvenyomás"));
    }

    @Test public void theSuggestionIsCappedAndSafe() {
        List<StrengthLog.Entry> log = log(
                at("Guggolás", 30), at("Evezés", 30), at("Fekvenyomás", 30),
                at("Oldalemelés", 30), at("Bicepsz", 30), at("Plank", 30));
        assertEquals(2, Muscles.suggestForToday(log, now(), 2).size());
        assertTrue(Muscles.suggestForToday(log, now(), 0).isEmpty());
        assertTrue(Muscles.suggestForToday(null, now(), 3).isEmpty());
        // Egyetlen csoportból nincs mit egyensúlyozni.
        assertTrue(Muscles.suggestForToday(log(at("Guggolás", 30)), now(), 3).isEmpty());
    }
}
