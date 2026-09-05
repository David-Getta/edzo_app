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

    /**
     * A „nyugalmi" magában is kimondja, miről van szó.
     *
     * A „hrv 62 ms, nyugalmi 49" a sportóra-leolvasás legrövidebb alakja, és
     * eddig némán elveszett – más nyugalmi értéket senki nem ír egy
     * edzésnaplóba.
     */
    @Test public void theWordRestingIsEnoughOnItsOwn() {
        assertEquals(49, Pulse.parse("hrv 62 ms, nyugalmi 49"));
        assertEquals(49, Pulse.parse("nyugalmi 49"));
        // Szám nélkül nincs mérés.
        assertEquals(-1, Pulse.parse("nyugalmi állapotban voltam"));
    }

    /** A szórend fordítva is jó a pulzus-szó nélkül: „54 nyugalmi". */
    @Test public void theNumberMayComeFirstWithoutTheWordPulse() {
        assertEquals(54, Pulse.parse("reggel 78,4 kg, 7 óra alvás, 54 nyugalmi"));
        assertEquals(47, Pulse.parse("ma reggel 47 volt a nyugalmi pulzusom"));
    }

    /** A változás mondatában a második szám a mai érték. */
    @Test public void theNewRestingValueWins() {
        assertEquals(45, Pulse.parse("nyugalmi pulzusom 48-ról 45-re javult "
                + "egy hónap alatt"));
    }

    /**
     * A „ma" beékelődhet, és a pihenőpulzus is pulzus.
     *
     * A „nyugalmi pulzusom ma 52" és a „pihenőpulzus 55 körül mozog"
     * eddig némán elveszett – az elsőben a „ma" nem fért a szó és a szám
     * közé, a másodikban a pihenő-előtag takarta a pulzus-szót.
     */
    @Test public void todayFitsBetweenTheWordAndTheNumber() {
        assertEquals(52, Pulse.parse("nyugalmi pulzusom ma 52"));
        assertEquals(55, Pulse.parse("pihenőpulzus 55 körül mozog mostanában"));
        // Az edzés-pulzus továbbra sem nyugalmi.
        assertEquals(-1, Pulse.parse("átlagpulzus 145 a futáson, max 172"));
    }
    /**
     * A kimondott pihenőpulzus akkor is mérés, ha a mondat edzésről is szól.
     *
     * A „pihenőpulzus 52, edzés közben max 178" ötvenkettője eddig némán
     * elveszett: az edzés-szavas tiltás csak a „nyugalmi" szót engedte át,
     * a vele egyenértékű pihenőpulzust nem.
     */
    @Test public void aStatedRestingPulseSurvivesATrainingClause() {
        assertEquals(52, Pulse.parse("pihenőpulzus 52, edzés közben max 178"));
        // Edzés-pulzus pihenő-szó nélkül továbbra sem nyugalmi.
        assertEquals(-1, Pulse.parse("edzés közben max 178"));
    }
    /**
     * A küszöb átlépése után a mai szám a mérés.
     *
     * A „nyugalmi pulzusom lement 50 alá, ma 49" negyvenkilence eddig
     * elveszett – a küszöb-szám elállta a minta útját.
     */
    @Test public void crossingAThresholdKeepsTodaysReading() {
        assertEquals(49, Pulse.parse("nyugalmi pulzusom lement 50 alá, "
                + "ma 49!"));
    }
    @Test public void theMorningPulseSurvivesTheDaysWorkout() {
        // A „reggel 52-es pulzus, délben futás 8 km" ötvenkettője eddig
        // elveszett – a futás szava az egészet edzésnek minősítette. Az
        // edzés közbeni pulzus továbbra sem nyugalmi.
        assertEquals(52, Pulse.parse(
                "reggel 52-es pulzus, délben futás 8 km, vacsora csirke rizzsel"));
        assertEquals(-1, Pulse.parse("futás közben 165 volt a pulzusom"));
    }

    @Test public void aDropInRestingPulseKeepsTheNewValue() {
        assertEquals(49, Pulse.parse("a pihen\u0151pulzusom lement 52-r\u0151l 49-re"));
    }

    @Test public void aCommaAfterNyugalmiStillBinds() {
        assertEquals(61, Pulse.parse("magas volt ma a nyugalmi, 61"));
    }

    @Test public void aPostWorkoutPulseStaysOut() {
        assertEquals(-1, Pulse.parse("edz\u00e9s ut\u00e1ni pulzus 130"));
    }

    @Test public void aReversedRhrReads() {
        assertEquals(54, Pulse.parse("78,2 kg / 54 rhr / 7,5h alv\u00e1s"));
    }

    /**
     * A vessz\u0151 ut\u00e1ni sz\u00e1m csak akkor a pulzus, ha nem egy M\u00c1SIK m\u00e9r\u00e9s
     * kezdete: az „52 nyugalmi, 80,4 kg" mondatban a nyolcvan a m\u00e9rleg
     * sz\u00e1ma, m\u00e9gis pulzusk\u00e9nt ment be – a val\u00f3di \u00f6tvenkett\u0151 elveszett.
     */
    @Test
    public void theWeightAfterTheCommaIsNotThePulse() {
        assertEquals(52, Pulse.parse("52 nyugalmi, 80,4 kg"));
        assertEquals(52, Pulse.parse("Ma: 6:20 alv\u00e1s, 52 nyugalmi, 80,4 kg"));
        // A megszokott alakok v\u00e1ltozatlanok.
        assertEquals(49, Pulse.parse("hrv 62 ms, nyugalmi 49"));
        assertEquals(61, Pulse.parse("magas volt ma a nyugalmi, 61"));
        assertEquals(52, Pulse.parse("nyugalmi pulzus 52, 80,4 kg"));
    }


    /**
     * Az \u00c9BRED\u00c9SKOR m\u00e9rt \u00e9rt\u00e9k nyugalmi pulzus akkor is, ha a sz\u00e1m a
     * pulzus-sz\u00f3 EL\u0150TT \u00e1ll: a \u201ereggel \u00e9bred\u00e9skor 48 volt a pulzusom, edz\u00e9s
     * ut\u00e1n 145" m\u00e9r\u00e9se n\u00e9m\u00e1n elveszett.
     */
    @Test
    public void theWakeupReadingIsARestingPulse() {
        assertEquals(48, Pulse.parse("Reggel \u00e9bred\u00e9skor 48 volt a pulzusom, "
                + "edz\u00e9s ut\u00e1n 145."));
        assertEquals(48, Pulse.parse("\u00c9bred\u00e9s ut\u00e1n 48 a pulzusom."));
        // A kimondott nyugalmi alak marad.
        assertEquals(48, Pulse.parse("Nyugalmi pulzus 48."));
    }


    /**
     * A NYUGALOMBAN ugyanaz a m\u00e9r\u00e9s m\u00e1s ragoz\u00e1sban: az \u201ea v\u00e9rem 130/85, a
     * pulzusom nyugalomban 58" \u00f6tvennyolca n\u00e9m\u00e1n elveszett.
     */
    @Test
    public void restingInAnyInflectionCounts() {
        assertEquals(58, Pulse.parse("A v\u00e9rem 130/85, a pulzusom nyugalomban 58."));
        assertEquals(58, Pulse.parse("Nyugalomban 58 a pulzusom."));
        assertEquals(52, Pulse.parse("Pihen\u0151ben 52."));
        // Az edz\u00e9s k\u00f6zbeni \u00e9rt\u00e9k tov\u00e1bbra sem nyugalmi.
        assertEquals(-1, Pulse.parse("Fut\u00e1s k\u00f6zben 165 volt a pulzusom."));
    }

    /**
     * Az \u00f6 hangrend\u0171 toldal\u00e9k \u00e9s az \u00e9bred\u00e9s ig\u00e9je.
     *
     * Az \u201e55-\u00f6s pulzussal \u00e9bredtem" n\u00e9m\u00e1n elveszett: a minta csak az \u201e-es"
     * \u00e9s az \u201e-as" alakot ismerte. Az \u201eedz\u00e9s ut\u00e1n 55-\u00f6s pulzussal
     * \u00e9bredtem, j\u00f3l regener\u00e1l\u00f3dtam" \u00f6tven\u00f6tj\u00e9t r\u00e1ad\u00e1sul az edz\u00e9s szava is
     * eln\u00e9m\u00edtotta \u2013 pedig aki \u00e9bred\u00e9skor m\u00e9r, az a nyugalmi \u00e9rt\u00e9k\u00e9t m\u00e9ri.
     */
    @Test
    public void anOInflectionAndTheWakingVerbCount() {
        assertEquals(55, Pulse.parse("55-\u00f6s pulzussal \u00e9bredtem."));
        assertEquals(55, Pulse.parse("Reggel 55-\u00f6s pulzus."));
        assertEquals(55, Pulse.parse("Edz\u00e9s ut\u00e1n 55-\u00f6s pulzussal \u00e9bredtem, "
                + "j\u00f3l regener\u00e1l\u00f3dtam."));
        // Az edz\u00e9s k\u00f6zbeni \u00e9rt\u00e9k tov\u00e1bbra sem nyugalmi.
        assertEquals(-1, Pulse.parse("Fut\u00e1s k\u00f6zben 165 volt az "
                + "\u00e1tlagpulzusom."));
    }

}
