package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Gyakorlatnév → izomcsoport, életszerű magyar nevekkel.
 *
 * Az osztály elve, hogy amit nem ismer fel biztosan, azt inkább besorolatlanul
 * hagyja: „egy rossz címke félrevezetőbb, mint a hiányzó”. Egy próbafuttatás
 * mégis talált rossz címkét – a fordított tárogatás mellizomként szerepelt,
 * mert a rövidebb „tarogat” szótő nyert. Az ilyen a heti egyensúly-kimutatásban
 * úgy jelenik meg, hogy a váll látszólag megvan, közben hetek óta kimarad.
 */
public class MusclesNamesTest {

    @Test public void theReverseFlyIsShouldersNotChest() {
        assertEquals(Muscles.VALL, Muscles.groupOf("Fordított tárogatás"));
        assertEquals(Muscles.VALL, Muscles.groupOf("fordított pillangó"));
        assertEquals(Muscles.VALL, Muscles.groupOf("Hátsó vállemelés"));
        // A sima tárogatás viszont marad mell.
        assertEquals(Muscles.MELL, Muscles.groupOf("Tárogatás"));
    }

    @Test public void theEverydayExerciseNamesAreRecognised() {
        assertEquals(Muscles.MELL, Muscles.groupOf("Ferde padon nyomás"));
        assertEquals(Muscles.VALL, Muscles.groupOf("Előreemelés"));
        assertEquals(Muscles.TORZS, Muscles.groupOf("Orosz csavarás"));
        assertEquals(Muscles.TORZS, Muscles.groupOf("Lábemelés"));
        assertEquals(Muscles.HAT, Muscles.groupOf("Gerincnyújtás"));
        assertEquals(Muscles.LAB, Muscles.groupOf("Bolgár kitörés"));
        assertEquals(Muscles.LAB, Muscles.groupOf("Medencelökés"));
        assertEquals(Muscles.KAR, Muscles.groupOf("Kalapács hajlítás"));
    }

    @Test public void theOldMeaningsAreUnchanged() {
        assertEquals(Muscles.LAB, Muscles.groupOf("Guggolás"));
        assertEquals(Muscles.MELL, Muscles.groupOf("Fekvenyomás"));
        assertEquals(Muscles.HAT, Muscles.groupOf("Húzódzkodás"));
        assertEquals(Muscles.HAT, Muscles.groupOf("Holtemelés"));
        assertEquals(Muscles.VALL, Muscles.groupOf("Vállból nyomás"));
        assertEquals(Muscles.KAR, Muscles.groupOf("Bicepsz hajlítás"));
        assertEquals(Muscles.TORZS, Muscles.groupOf("Plank"));
    }

    @Test public void theAppsOwnBuiltInExercisesAreRecognised() {
        // Ezek a nevek a beépített programokból jönnek – itt nincs kétértelműség,
        // az app tudja, mit javasolt. A 33 beépített gyakorlatból 14 maradt
        // besorolatlan, vagyis a heti kimutatás vak volt a saját programjaira.
        assertEquals(Muscles.LAB, Muscles.groupOf("Csípőemelés (híd)"));
        assertEquals(Muscles.LAB, Muscles.groupOf("Fal-ülés"));
        assertEquals(Muscles.LAB, Muscles.groupOf("Fellépés székre"));
        assertEquals(Muscles.TORZS, Muscles.groupOf("Hegymászó"));
        assertEquals(Muscles.TORZS, Muscles.groupOf("Madár-kutya"));
        assertEquals(Muscles.HAT, Muscles.groupOf("Szuperman"));
    }

    @Test public void stretchesAndCardioStayOutOfTheMuscleBalance() {
        // Szándékos: a nyújtás és a kardió nem erősítő munka, a kimutatásba
        // beszámítva azt hazudná, hogy az adott izomcsoport megvolt.
        assertNull(Muscles.groupOf("Nyakkörzés"));
        assertNull(Muscles.groupOf("Vállnyújtás"));
        assertNull(Muscles.groupOf("Csípőnyújtás"));
        assertNull(Muscles.groupOf("Jumping jack"));
        assertNull(Muscles.groupOf("Burpee"));
        assertNull(Muscles.groupOf("Magas térd"));
    }

    @Test public void whatIsNotCertainStaysUnlabelled() {
        // Kardió és általános nevek: itt a hiányzó címke a helyes válasz.
        assertNull(Muscles.groupOf("Futás"));
        assertNull(Muscles.groupOf("Bicikli"));
        assertNull(Muscles.groupOf("Ugrókötél"));
        assertNull(Muscles.groupOf("Kabelhúzás"));
        assertNull(Muscles.groupOf(""));
        assertNull(Muscles.groupOf(null));
        // A rövid szavak csak önálló szóként számítanak.
        assertNull(Muscles.groupOf("Labdás gyakorlat"));
        assertNull(Muscles.groupOf("Hatvanas sorozat"));
    }
}
