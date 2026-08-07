package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/**
 * A nyugalmi pulzus mondatból.
 *
 * Két dolgot kell tudnia: elhinni a reggeli pihenőértéket, és NEM elhinni
 * az edzés-adatot – a „futás átlagpulzus 165" nem nyugalmi érték, és ha
 * azzá válna, a napló egy csapásra betegnek mutatna egy egészséges embert.
 */
public class PulseTest {

    @Test public void readsTheRestingRate() {
        assertEquals(52, Pulse.parse("nyugalmi pulzus 52"));
        assertEquals(48, Pulse.parse("pulzusom: 48"));
        assertEquals(55, Pulse.parse("reggel pulzus 55"));
        assertEquals(60, Pulse.parse("60-as pulzus"));
        assertEquals(47, Pulse.parse("rhr 47"));
        assertEquals(58, Pulse.parse("ma reggel a pulzusom 58 volt"));
    }

    /** Az edzés-adat nem pihenőérték. */
    @Test public void trainingHeartRateIsNotResting() {
        assertEquals(-1, Pulse.parse("futás átlagpulzus 165"));
        assertEquals(-1, Pulse.parse("edzés közben pulzus 150"));
        assertEquals(-1, Pulse.parse("max pulzus 185"));
        assertEquals(-1, Pulse.parse("bringa, pulzus 140"));
        // Az „átlagpulzus" szó belsejében álló „pulzus" sem talál.
        assertEquals(-1, Pulse.parse("átlagpulzus 132"));
        // A kimondott „nyugalmi" viszont felülírja a gyanút.
        assertEquals(54, Pulse.parse("edzés előtt nyugalmi pulzus 54"));
    }

    /** Életszerűtlen érték és hiányzó szám nem bejegyzés. */
    @Test public void needsAPlausibleValue() {
        assertEquals(-1, Pulse.parse("pulzus 20"));
        assertEquals(-1, Pulse.parse("pulzus 150"));
        assertEquals(-1, Pulse.parse("jó a pulzusom"));
        assertEquals(-1, Pulse.parse(""));
        assertEquals(-1, Pulse.parse(null));
    }

    /** A visszajelzés minden sávban mond valamit, diagnózis nélkül. */
    @Test public void verdictCoversTheBands() {
        assertTrue(Pulse.verdict(45).length() > 0);
        assertTrue(Pulse.verdict(55).length() > 0);
        assertTrue(Pulse.verdict(65).length() > 0);
        assertTrue(Pulse.verdict(75).length() > 0);
        assertTrue(Pulse.verdict(90).contains("orvos"));
        assertEquals("", Pulse.verdict(0));
    }

    /** Az útbaigazító a Profilhoz küldi – és nem tolakszik mások elé. */
    @Test public void routedToTheProfile() {
        assertEquals(Sentence.Kind.PULSE,
                Sentence.of("nyugalmi pulzus 52", Arrays.asList(Foods.ALL), 1_753_869_600_000L));
        assertEquals(Sentence.Kind.WORKOUT,
                Sentence.of("30 perc futás, átlagpulzus 150",
                        Arrays.asList(Foods.ALL), 1_753_869_600_000L));
        assertEquals(Sentence.Kind.SLEEP,
                Sentence.of("aludtam 8 órát, pulzus 52",
                        Arrays.asList(Foods.ALL), 1_753_869_600_000L));
    }
}
