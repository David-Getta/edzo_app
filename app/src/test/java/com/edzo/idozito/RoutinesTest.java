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
}
