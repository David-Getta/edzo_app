package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Edzésnapok tárolása.
 *
 * Egy elrontott beállítás itt az összes edzésnapot elvinné, ezért a hibás sor
 * némán kimarad, a többi megmarad. A név-ütközés cserél, nem duplázza.
 */
public class RoutinesTest {

    private static List<String> moves(String... m) {
        return new ArrayList<>(Arrays.asList(m));
    }

    @Test public void theBuiltInDaysUseKnownExerciseNames() {
        List<Routines.Routine> b = Routines.builtIn();
        assertTrue(b.size() >= 5);
        List<String> known = Arrays.asList(StrengthParse.names());
        for (Routines.Routine r : b) {
            assertTrue(r.name, !r.name.isEmpty());
            assertTrue(r.name, r.moves.size() >= 3 && r.moves.size() <= Routines.MAX_MOVES);
            for (String m : r.moves)
                // Ha egy név elgépelt, a progresszió-javaslat és a rekordok sem
                // találnák meg – akkor a sablon fél sablon.
                assertTrue(r.name + ": ismeretlen gyakorlat: " + m, known.contains(m));
        }
    }

    @Test public void aRoundTripKeepsEverything() {
        String s = Routines.add("", "Tolónap", moves("Fekvenyomás", "Tricepsz"));
        s = Routines.add(s, "Lábnap", moves("Guggolás", "Kitörés", "Vádliemelés"));
        List<Routines.Routine> r = Routines.parse(s);
        assertEquals(2, r.size());
        // A legutóbb mentett van elöl.
        assertEquals("Lábnap", r.get(0).name);
        assertEquals(3, r.get(0).moves.size());
        assertEquals("Tolónap", r.get(1).name);
        assertEquals("Fekvenyomás  ·  Tricepsz", r.get(1).summary());
        assertEquals("Tolónap  ·  2 gyakorlat", r.get(1).label());
    }

    @Test public void theSameNameIsReplacedNotDuplicated() {
        String s = Routines.add("", "Lábnap", moves("Guggolás"));
        s = Routines.add(s, "lábnap", moves("Guggolás", "Kitörés"));
        List<Routines.Routine> r = Routines.parse(s);
        assertEquals(1, r.size());
        assertEquals(2, r.get(0).moves.size());
    }

    @Test public void separatorsCannotBreakTheStorage() {
        // A név és a gyakorlat is tartalmazhat bármit – a tárolás nem törhet el.
        String s = Routines.add("", "A|B;C", moves("Gug|golás", "Kitö;rés"));
        List<Routines.Routine> r = Routines.parse(s);
        assertEquals(1, r.size());
        assertEquals("A B C", r.get(0).name);
        assertEquals(2, r.get(0).moves.size());
        // Az újraolvasás ugyanazt adja.
        assertEquals(s, Routines.format(r));
    }

    @Test public void brokenStorageLosesOnlyTheBrokenPart() {
        assertTrue(Routines.parse(null).isEmpty());
        assertTrue(Routines.parse("").isEmpty());
        assertTrue(Routines.parse("   ").isEmpty());
        // Páratlan mező: a fél sor kiesik, a teljes megmarad.
        List<Routines.Routine> r = Routines.parse("Lábnap|Guggolás;Kitörés|Csonka");
        assertEquals(1, r.size());
        assertEquals("Lábnap", r.get(0).name);
        // Gyakorlat nélküli nap nem nap.
        assertTrue(Routines.parse("Üres|").isEmpty());
        assertTrue(Routines.parse("|Guggolás").isEmpty());
    }

    @Test public void theLimitsHold() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 30; i++) many.add("Gyakorlat" + i);
        String s = Routines.add("", "Sok", many);
        assertEquals(Routines.MAX_MOVES, Routines.parse(s).get(0).moves.size());
        // Hosszú név levágva.
        String longName = "Nagyon hosszú edzésnap-név ami sosem férne ki";
        s = Routines.add("", longName, moves("Guggolás"));
        assertTrue(Routines.parse(s).get(0).name.length() <= Routines.MAX_NAME);
        // Ismétlődő gyakorlat egyszer szerepel.
        s = Routines.add("", "Nap", moves("Guggolás", "Guggolás", "Kitörés"));
        assertEquals(2, Routines.parse(s).get(0).moves.size());
    }

    @Test public void removingWorksAndIsHarmlessWhenMissing() {
        String s = Routines.add(Routines.add("", "A", moves("Guggolás")), "B", moves("Kitörés"));
        s = Routines.remove(s, "a");
        assertEquals(1, Routines.parse(s).size());
        assertEquals("B", Routines.parse(s).get(0).name);
        // Nem létező név: marad minden.
        assertEquals(s, Routines.remove(s, "nincs ilyen"));
        assertEquals("", Routines.remove("", "akármi"));
    }

    @Test public void ownDaysComeFirstAndShadowTheBuiltInOnes() {
        String s = Routines.add("", "Lábnap", moves("Guggolás"));
        List<Routines.Routine> all = Routines.all(s);
        assertEquals("Lábnap", all.get(0).name);
        // Csak EGY Lábnap van: a saját elnyomja a beépítettet.
        int n = 0;
        for (Routines.Routine r : all) if (r.name.equalsIgnoreCase("Lábnap")) n++;
        assertEquals(1, n);
        assertEquals(1, Routines.all(s).get(0).moves.size());
        // A többi beépített megmarad.
        assertEquals(Routines.builtIn().size(), all.size());
    }

    @Test public void lookupFindsOwnAndBuiltInDays() {
        String s = Routines.add("", "Saját", moves("Guggolás"));
        assertEquals("Saját", Routines.byName(s, "saját").name);
        assertTrue(Routines.byName(s, "Tolónap").moves.contains("Fekvenyomás"));
        assertNull(Routines.byName(s, "Nincs ilyen"));
        assertNull(Routines.byName(s, ""));
        assertNull(Routines.byName(s, null));
    }

    @Test public void theLastTimeTheDayWasDoneIsFound() {
        long d = 86400000L;
        long now = 1000 * d + 12 * 3600000L;
        List<String> moves = moves("Guggolás", "Kitörés", "Vádliemelés");
        // Két nappal ezelőtt kettő megvolt a háromból: ez fél fölött van.
        long[] ts = {998 * d, 998 * d, 990 * d, 990 * d, 990 * d};
        String[] names = {"Guggolás", "Kitörés", "Guggolás", "Kitörés", "Vádliemelés"};
        assertEquals(2, Routines.lastDone(moves, ts, names, now));
        assertEquals("2 napja", Routines.lastDoneLabel(2));
        assertEquals("ma", Routines.lastDoneLabel(0));
        assertEquals("tegnap", Routines.lastDoneLabel(1));
        assertEquals("", Routines.lastDoneLabel(-1));
    }

    @Test public void oneExerciseIsNotAWholeDay() {
        long d = 86400000L;
        long now = 1000 * d;
        List<String> moves = moves("Guggolás", "Kitörés", "Vádliemelés", "Lábtolás");
        // Egyetlen gyakorlat a négyből nem edzésnap.
        assertEquals(-1, Routines.lastDone(moves,
                new long[]{999 * d}, new String[]{"Guggolás"}, now));
        // Kettő a négyből viszont igen (a fele elég).
        assertEquals(1, Routines.lastDone(moves,
                new long[]{999 * d, 999 * d}, new String[]{"Guggolás", "Kitörés"}, now));
        // Ugyanaz a gyakorlat kétszer egy napon nem két gyakorlat.
        assertEquals(-1, Routines.lastDone(moves,
                new long[]{999 * d, 999 * d}, new String[]{"Guggolás", "guggolás"}, now));
    }

    @Test public void brokenLastDoneInputNeverCrashes() {
        assertEquals(-1, Routines.lastDone(null, null, null, 0));
        assertEquals(-1, Routines.lastDone(moves("Guggolás"), null, null, 0));
        assertEquals(-1, Routines.lastDone(new ArrayList<String>(),
                new long[]{1}, new String[]{"Guggolás"}, 2));
        // Eltérő hosszú tömbök: a rövidebb dönt.
        assertEquals(-1, Routines.lastDone(moves("Guggolás"),
                new long[]{1, 2, 3}, new String[]{}, 4));
        // Null név nem borít.
        assertEquals(-1, Routines.lastDone(moves("Guggolás"),
                new long[]{1}, new String[]{null}, 2));
    }

    @Test public void theShortSummaryFitsANotification() {
        Routines.Routine r = Routines.byName("", "Lábnap");
        assertEquals(5, r.moves.size());
        assertEquals("Guggolás  ·  Lábtolás  ·  Kitörés +2", r.shortSummary(3));
        assertEquals("Guggolás +4", r.shortSummary(1));
        // Nulla vagy negatív kérésre is marad legalább egy név.
        assertEquals("Guggolás +4", r.shortSummary(0));
        // Ha minden kifér, nincs plusz-jelzés.
        Routines.Routine t = Routines.byName("", "Teljes test");
        assertEquals(t.summary(), t.shortSummary(t.moves.size()));
        assertEquals(t.summary(), t.shortSummary(99));
    }

    @Test public void everyBuiltInExerciseHasAMuscleGroup() {
        // Az izomcsoport-egyensúly és a „mai ajánlat" ezen múlik: egy
        // besorolatlan gyakorlat csendben kiesne a heti képből.
        for (Routines.Routine r : Routines.builtIn())
            for (String m : r.moves) {
                String g = Muscles.groupOf(m);
                assertTrue(r.name + " / " + m + ": nincs izomcsoport",
                        g != null && !g.isEmpty());
            }
    }

    @Test public void theBuiltInDaysCoverEveryMuscleGroup() {
        // Ha egy csoportra egyetlen beépített nap sem jut, azt a felhasználó
        // sosem kapja meg sablonból.
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (Routines.Routine r : Routines.builtIn())
            for (String m : r.moves) seen.add(Muscles.groupOf(m));
        for (String g : new String[]{"Láb", "Hát", "Mell", "Váll", "Kar", "Törzs"})
            assertTrue("egyetlen beépített napban sincs: " + g, seen.contains(g));
    }

    @Test public void theNumberOfTimesTheDayWasDoneIsCounted() {
        long d = 86400000L;
        long now = 1000 * d + 12 * 3600000L;
        List<String> m = moves("Guggolás", "Kitörés");
        long[] ts = {999 * d, 999 * d, 995 * d, 995 * d, 960 * d, 960 * d};
        String[] names = {"Guggolás", "Kitörés", "Guggolás", "Kitörés",
                "Guggolás", "Kitörés"};
        assertEquals(2, Routines.doneDays(m, ts, names, now, 30));
        assertEquals(3, Routines.doneDays(m, ts, names, now, 60));
        assertEquals(1, Routines.doneDays(m, ts, names, now, 3));
        // Az ablakon kívüli és a képtelen kérés nem számít.
        assertEquals(0, Routines.doneDays(m, ts, names, now, 0));
        assertEquals(0, Routines.doneDays(m, ts, names, now, -5));
        assertEquals(0, Routines.doneDays(null, ts, names, now, 30));
        // A legutóbbi továbbra is stimmel.
        assertEquals(1, Routines.lastDone(m, ts, names, now));
    }

    @Test public void theLongestUnusedDayIsTheOneComingUp() {
        long d = 86400000L;
        long now = 1000 * d + 12 * 3600000L;
        List<Routines.Routine> all = Routines.all("");
        // Tolónap 1 napja, Lábnap 5 napja, Húzónap 3 napja – a láb van soron.
        long[] ts = {999 * d, 999 * d, 997 * d, 997 * d, 995 * d, 995 * d, 995 * d};
        String[] names = {"Fekvenyomás", "Vállból nyomás",
                "Felhúzás", "Húzódzkodás",
                "Guggolás", "Lábtolás", "Kitörés"};
        assertEquals("Lábnap", Routines.nextUp(all, ts, names, now));
        // Egyetlen használt nappal nincs mit sorba rakni.
        assertNull(Routines.nextUp(all, new long[]{999 * d, 999 * d},
                new String[]{"Fekvenyomás", "Vállból nyomás"}, now));
        // Amit két hónapnál régebben csinált, az nem a rotáció része.
        long[] old = {999 * d, 999 * d, 900 * d, 900 * d, 900 * d};
        String[] oldNames = {"Fekvenyomás", "Vállból nyomás",
                "Guggolás", "Lábtolás", "Kitörés"};
        assertNull(Routines.nextUp(all, old, oldNames, now));
        // Hibás bemenet nem borít.
        assertNull(Routines.nextUp(null, ts, names, now));
        assertNull(Routines.nextUp(all, null, null, now));
    }
}
