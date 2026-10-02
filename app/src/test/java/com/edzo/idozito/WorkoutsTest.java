package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Edzés-sablonok. A tárolás JSON-ban megy (azt itt nem tudjuk futtatni, mert az
 * android.jar csonk), de a „mit jelent a hiányzó érték" szabály tiszta Java –
 * és pont az a kényes: egy régi, bemelegítés nélkül mentett sablon betöltése
 * nem törölheti le a beállított bemelegítést.
 */
public class WorkoutsTest {

    @Test public void anOldTemplateKnowsNothingAboutWarmUp() {
        // A négy paraméteres alak a régi sablonokat képviseli.
        Workouts.W old = new Workouts.W("Régi", 10, 30, 15, 8);
        assertEquals(Workouts.UNSET, old.warm);
        assertEquals(Workouts.UNSET, old.cool);
        assertFalse("a régi sablon ne állítson bemelegítést", old.hasWarmCool());
    }

    @Test public void aNewTemplateCarriesWarmUpAndCoolDown() {
        Workouts.W w = new Workouts.W("Új", 10, 30, 15, 8, 180, 120);
        assertEquals(180, w.warm);
        assertEquals(120, w.cool);
        assertTrue(w.hasWarmCool());
    }

    @Test public void zeroIsAValidChoiceNotAMissingValue() {
        // Aki tudatosan nulla bemelegítést mentett, annál a betöltés is nullázzon.
        Workouts.W w = new Workouts.W("Nulla", 10, 30, 15, 8, 0, 0);
        assertTrue("a nulla is tárolt érték", w.hasWarmCool());
        assertFalse(w.warm == Workouts.UNSET);
    }

    @Test public void theBasicFieldsAreKeptAsGiven() {
        Workouts.W w = new Workouts.W("Teszt", 5, 40, 20, 6, 60, 90);
        assertEquals("Teszt", w.name);
        assertEquals(5, w.prep);
        assertEquals(40, w.work);
        assertEquals(20, w.rest);
        assertEquals(6, w.rounds);
    }

    @Test public void theUnsetMarkerCannotCollideWithARealValue() {
        // A jelölő negatív: valós másodperc-érték sosem lehet az.
        assertTrue("a jelölő legyen negatív", Workouts.UNSET < 0);
    }
}
