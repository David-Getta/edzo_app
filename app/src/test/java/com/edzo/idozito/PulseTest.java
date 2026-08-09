package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    /**
     * A szám elöl, a pulzus-szó a mondat végén.
     *
     * A „ma reggel 47 volt a nyugalmi pulzusom" a legtermészetesebb magyar
     * alak, és eddig nem létezett: a felismerő csak a pulzus-szó UTÁNI
     * számot kereste. A „nyugalmi" itt kötelező, különben a mondat bármelyik
     * száma odaeshetne.
     */
    @Test public void theNumberMayComeFirst() {
        assertEquals(47, Pulse.parse("ma reggel 47 volt a nyugalmi pulzusom"));
        assertEquals(52, Pulse.parse("reggel 52 a nyugalmi pulzusom"));
        assertEquals(48, Pulse.parse("48 a nyugalmi pulzusom ma"));
        // Az életszerűtlen érték itt sem megy át.
        assertEquals(-1, Pulse.parse("165 volt a nyugalmi pulzusom"));
        // „Nyugalmi" nélkül nem talál: az edzés-szám nem pihenőérték.
        assertEquals(-1, Pulse.parse("edzés után 20 perccel 95 volt a pulzusom"));
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

    /**
     * A CÉL nem mérés.
     *
     * A „szeretném, ha 50 lenne a nyugalmi pulzusom" és az „a cél 50-es
     * nyugalmi pulzus" ugyanúgy tartalmaz számot és pulzus-szót, mint egy
     * bejegyzés – csak épp az ellenkezőjét mondja. Az alvásnál és a
     * testsúlynál ez a szabály régóta megvolt, itt hiányzott.
     */
    @Test public void goalsAreNotMeasurements() {
        assertEquals(-1, Pulse.parse("szeretném, ha 50 lenne a nyugalmi pulzusom"));
        assertEquals(-1, Pulse.parse("a cél 50-es nyugalmi pulzus"));
        assertEquals(-1, Pulse.parse("jó lenne 50-es nyugalmi pulzus"));
        assertEquals(-1, Pulse.parse("holnap megmérem a nyugalmi pulzusom"));
        // A valódi mérés változatlan.
        assertEquals(50, Pulse.parse("nyugalmi pulzus 50"));
        assertEquals(48, Pulse.parse("ma reggel 48 volt a nyugalmi pulzusom"));
    }

    /**
     * A napszak beékelődhet a pulzus-szó és a szám közé.
     *
     * A „nyugalmi pulzus reggel 47" a legtermészetesebb magyar alak, és eddig
     * egyáltalán nem létezett: a mérés némán elveszett, a szám pedig a
     * súlytrendbe csúszott át.
     */
    @Test public void theTimeOfDayMayStandBetween() {
        assertEquals(47, Pulse.parse("nyugalmi pulzus reggel 47"));
        assertEquals(47, Pulse.parse("nyugalmi pulzus reggel 47, este 62"));
        assertEquals(52, Pulse.parse("pulzus este 52"));
        assertEquals(50, Pulse.parse("pulzus volt 50"));
        // Az edzés-pulzus továbbra sem nyugalmi.
        assertEquals(-1, Pulse.parse("futás átlagpulzus 165"));
    }

    /**
     * A pulzus-tartomány nem munka/pihenő ritmus.
     *
     * Az „50-55 között van a nyugalmi pulzusom" tartománya pontosan úgy néz
     * ki, mint egy időzítő-pár, és eddig ötven másodperc munka, ötvenöt
     * pihenő tervet ajánlott rá az app.
     */
    @Test public void aPulseRangeIsNotATimerPlan() {
        assertNull(IntervalParse.parse("50-55 között van a nyugalmi pulzusom"));
        assertNull(IntervalParse.parse("a pulzusom 60-65 között mozog"));
        assertTrue(Pulse.parse("50-55 között van a nyugalmi pulzusom") > 0);
        // A kimondott terv erősebb: ott a pulzus csak megjegyzés mellette.
        assertNotNull(IntervalParse.parse("4 kör 4 perc, pulzus 165 körül"));
        assertNotNull(IntervalParse.parse("3 kör 40 mp munka 20 mp pihenő"));
    }
}
