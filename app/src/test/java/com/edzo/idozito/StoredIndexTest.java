package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tárolt értékek, amiket tömb-indexként használunk.
 *
 * Ezek a beállításokból jönnek, a beállítások pedig visszaállíthatók egy
 * mentésfájlból – amit akár kézzel is szerkeszthettek, vagy egy későbbi verzió
 * írt, több választható értékkel. Egy tartományon kívüli szám a kezdőlapon
 * dobna kivételt: az app minden indításkor összeomlana, és csak az adatok
 * törlésével lehetne kilábalni belőle.
 */
public class StoredIndexTest {

    @Test public void everyStoredModeMapsToARealUnit() {
        for (int stored = -100; stored <= 100; stored++) {
            int m = MainActivity.clampMode(stored);
            assertTrue("tartományon kívül: " + stored + " -> " + m,
                    m >= 0 && m < MainActivity.GOAL_UNITS.length);
        }
    }

    @Test public void theValidValuesAreLeftAlone() {
        for (int i = 0; i < MainActivity.GOAL_UNITS.length; i++)
            assertEquals(i, MainActivity.clampMode(i));
    }

    @Test public void theSoundIndexIsAlsoSafe() {
        // Ugyanez a hangválasztásra: a beállított index a mentésből jön.
        for (int stored : new int[]{-5, -1, 0, 3, 8, 99, Integer.MAX_VALUE, Integer.MIN_VALUE})
            assertTrue("nincs hang ehhez: " + stored, Beeper.soundAt(stored) != null);
    }

    @Test public void theLevelTitleIsSafeForAnyLevel() {
        // A szint az XP-ből jön, ami elvileg nem lehet negatív – de ha a mentés
        // mégis ilyet hoz, ne omoljon össze a profil.
        for (int lvl : new int[]{-10, 0, 1, 5, 10, 50, 1000})
            assertTrue("nincs cím ehhez a szinthez: " + lvl, Levels.title(lvl) != null);
    }

    @Test public void theSectionNamesAndIdsStayInPairs() {
        // A kezdőlap szakasz-átrendezője a két tömböt párban indexeli.
        assertEquals("a szakasz-nevek és -azonosítók száma eltér",
                MainActivity.SECT_IDS.length, MainActivity.SECT_NAMES.length);
    }
}
