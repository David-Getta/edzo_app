package com.edzo.idozito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Az emlékeztetők szövege. Egy értesítés, ami olyat állít, ami nem igaz („a mai
 * edzés még hiányzik", pedig reggel megvolt), pont az ellenkezőjét éri el:
 * ettől kapcsolják ki az emlékeztetőket.
 *
 * A buzdító sorok időfüggően váltakoznak, ezért csak olyat ellenőrzünk, ami
 * MINDEGYIK változatra igaz – különben a teszt véletlenszerűen bukna el.
 */
public class MascotTest {

    private static final String NAME = "Dávid";

    // --- Ami minden változatra igaz ---

    @Test public void everyVariantIsUsableText() {
        for (boolean trained : new boolean[]{false, true}) {
            for (int streak : new int[]{0, 1, 2, 5, 30}) {
                for (String n : new String[]{null, "", "   ", NAME}) {
                    String s = Mascot.reminderText(n, trained, streak);
                    assertFalse("üres üzenet", s.trim().isEmpty());
                    assertFalse("beszivárgott null: " + s, s.contains("null"));
                    assertFalse("üres név-lyuk: " + s, s.contains(" , "));
                }
            }
        }
    }

    // --- A determinisztikus ágak ---

    @Test public void anEndangeredStreakIsNamedByItsLength() {
        // Tegnapig 6 nap, ma még semmi: a szám legyen benne, az a tét.
        String s = Mascot.reminderText(NAME, false, 6);
        assertTrue("a széria hossza legyen benne: " + s, s.contains("6"));
        assertTrue("nevén szólítson: " + s, s.contains(NAME));
    }

    @Test public void afterTrainingItDoesNotNagAnyMore() {
        // Ez az ág mindhárom változatában elismer, nem noszogat.
        String s = Mascot.reminderText(NAME, true, 0);
        assertFalse("ne hiányoljon edzést: " + s, s.contains("hiányzik"));
        assertFalse("ne szólítson edzésre: " + s, s.contains("Ideje"));
        assertFalse("ne a kanapéval ijesztgessen: " + s, s.contains("kanapé"));
        assertTrue("nevén szólítson: " + s, s.contains(NAME));
    }

    @Test public void afterTrainingTheStreakIsCountedWithToday() {
        // Tegnapig 4 nap + a mai edzés = 5 napos széria.
        String s = Mascot.reminderText(NAME, true, 4);
        assertTrue("a mai nappal együtt számoljon: " + s, s.contains("5"));
        assertFalse("ne a tegnapi számot mutassa: " + s, s.contains("4 napos"));
    }

    @Test public void trainedAndNotTrainedReadDifferently() {
        // Élő szériánál mindkét ág determinisztikus, tehát biztosan eltérnek.
        assertFalse("a két állapot ne ugyanazt mondja",
                Mascot.reminderText(NAME, true, 4).equals(
                        Mascot.reminderText(NAME, false, 4)));
    }

    @Test public void withoutANameNobodyIsAddressedByNull() {
        // A névtelen felhasználót vagy „falkatárs"-ként szólítja meg, vagy
        // sehogy – de sosem üres helyet vagy „null"-t írunk a helyére.
        String s = Mascot.reminderText(null, false, 3);
        assertTrue("névtelenül is legyen megszólítás: " + s, s.contains("falkatárs"));
    }
}
