package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;

/**
 * A Gyakorlat-könyvtár tartalma. Egy új gyakorlat felvétele némán leírás nélkül
 * hagyható – a könyvtárban ez csak egy üres sor lenne, amit könnyű nem észrevenni.
 */
public class ProgramsTest {

    @Test public void everyBuiltInExerciseHasFormCues() {
        for (Programs.P p : Programs.BUILT_IN) {
            for (String ex : p.ex) {
                assertFalse("nincs leírása: " + ex + " (" + p.name + ")",
                        Programs.descOf(ex).isEmpty());
            }
        }
    }

    @Test public void everyStrengthExerciseHasFormCues() {
        // Ezeket az Erősítő napló kínálja fel, itt a technika sérülés kérdése is.
        for (String ex : StrengthLog.COMMON) {
            assertFalse("nincs leírása: " + ex, Programs.descOf(ex).isEmpty());
        }
    }

    @Test public void unknownNamesGiveAnEmptyDescription() {
        assertEquals("", Programs.descOf("Valami saját gyakorlat"));
        assertEquals("", Programs.descOf(null));
    }

    @Test public void programsHaveNameEmojiAndExercises() {
        HashSet<String> names = new HashSet<>();
        for (Programs.P p : Programs.BUILT_IN) {
            assertFalse("üres programnév", p.name.trim().isEmpty());
            assertFalse("nincs emoji: " + p.name, p.emoji.trim().isEmpty());
            assertTrue("üres program: " + p.name, p.ex.length > 0);
            assertTrue("két program azonos néven: " + p.name, names.add(p.name));
        }
    }

    @Test public void noProgramRepeatsAnExerciseTwiceInARow() {
        // Egymás után kétszer ugyanaz a gyakorlat pihenő nélkül: elgépelés jele.
        for (Programs.P p : Programs.BUILT_IN) {
            for (int i = 1; i < p.ex.length; i++) {
                assertFalse("kétszer egymás után: " + p.ex[i] + " (" + p.name + ")",
                        p.ex[i].equals(p.ex[i - 1]));
            }
        }
    }
}
