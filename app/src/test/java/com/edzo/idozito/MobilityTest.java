package com.edzo.idozito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A mobilitás-tartalom épsége. A levezető nyújtás a gyakorlatokat NÉV szerint
 * hivatkozza; ha egy nevet átírunk a könyvtárban, a levezetés csendben olyan
 * gyakorlatot mondana be, amihez már nincs leírás sehol.
 */
public class MobilityTest {

    private static List<Mobility.Item> allItems() {
        List<Mobility.Item> out = new ArrayList<>();
        for (Mobility.Group[] gs : new Mobility.Group[][]{
                Mobility.WARMUP, Mobility.STRETCH, Mobility.ROLLING}) {
            for (Mobility.Group g : gs) out.addAll(java.util.Arrays.asList(g.items));
        }
        return out;
    }

    @Test public void everyCooldownExerciseExistsInTheLibrary() {
        HashSet<String> known = new HashSet<>();
        for (Mobility.Item i : allItems()) known.add(i.name);
        for (String n : Mobility.COOLDOWN_NAMES) {
            assertTrue("a levezetés olyan gyakorlatot hivatkozik, ami nincs a "
                    + "könyvtárban: " + n, known.contains(n));
        }
    }

    @Test public void theCooldownIsNotEmptyAndHasNoRepeats() {
        assertTrue(Mobility.COOLDOWN_NAMES.length >= 4);
        HashSet<String> seen = new HashSet<>();
        for (String n : Mobility.COOLDOWN_NAMES)
            assertTrue("kétszer szerepel a levezetésben: " + n, seen.add(n));
    }

    @Test public void everyExerciseHasANameDescriptionAndVideoQuery() {
        for (Mobility.Item i : allItems()) {
            assertFalse("névtelen gyakorlat", i.name.trim().isEmpty());
            assertFalse("nincs leírása: " + i.name, i.desc.trim().isEmpty());
            assertFalse("nincs videó-keresése: " + i.name, i.video.trim().isEmpty());
        }
    }

    @Test public void exerciseNamesAreUnique() {
        // Azonos név két helyen: a levezetés nem tudná eldönteni, melyikre gondol.
        HashSet<String> seen = new HashSet<>();
        for (Mobility.Item i : allItems())
            assertTrue("két gyakorlat azonos néven: " + i.name, seen.add(i.name));
    }

    @Test public void everyGroupHasATitleAndAtLeastTwoExercises() {
        for (Mobility.Group[] gs : new Mobility.Group[][]{
                Mobility.WARMUP, Mobility.STRETCH, Mobility.ROLLING}) {
            for (Mobility.Group g : gs) {
                assertFalse("cím nélküli csoport", g.title.trim().isEmpty());
                assertTrue("izmonként legalább kettő kell: " + g.title, g.items.length >= 2);
            }
        }
    }
}
