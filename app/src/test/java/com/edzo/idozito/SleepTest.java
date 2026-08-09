package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Alvás a mondatból.
 *
 * A regeneráció az edzés másik fele – de a kilogrammhoz hasonlóan az óra is
 * túlterhelt szó: a „8 óra kondi" edzés, a „8 órakor keltem" időpont. Ezért
 * kimondott alvás-szó kell, és életszerű sáv.
 */
public class SleepTest {

    @Test public void spokenSleepIsUnderstood() {
        assertEquals(8, Sleep.parse("aludtam 8 órát"), 0.01);
        assertEquals(7.5, Sleep.parse("aludtam 7,5 órát"), 0.01);
        assertEquals(7, Sleep.parse("7 óra alvás"), 0.01);
        assertEquals(6, Sleep.parse("ma éjjel csak 6 órát aludtam... alvás: 6"), 0.01);
        assertEquals(8, Sleep.parse("alvás: 8"), 0.01);
        // Kiírt számnév és a „és fél" is.
        assertEquals(8, Sleep.parse("nyolc órát aludtam... aludtam nyolc órát"), 0.01);
        assertEquals(7.5, Sleep.parse("aludtam hét és fél órát"), 0.01);
    }

    /**
     * A szám ELÖL is állhat: „7,5 órát aludtam".
     *
     * Ez a leggyakoribb magyar szórend, és a felismerő pont ezt nem értette –
     * csak az ige mögötti számot. Aki így írta le az éjszakáját, semmit nem
     * kapott vissza.
     */
    @Test public void theNumberMayComeFirst() {
        assertEquals(7.5, Sleep.parse("7,5 órát aludtam"), 0.01);
        assertEquals(4, Sleep.parse("csak 4 órát aludtam"), 0.01);
        assertEquals(6, Sleep.parse("tegnap 6 órát aludtam összesen"), 0.01);
        assertEquals(9, Sleep.parse("kilenc órát aludtam"), 0.01);
        assertEquals(7.5, Sleep.parse("hét és fél órát aludtam"), 0.01);
        // A másik igét nem húzza magához: a munka nem alvás.
        assertEquals(6, Sleep.parse("8 órát dolgoztam, 6 órát aludtam"), 0.01);
    }

    /**
     * Óra ÉS perc, illetve tól-ig.
     *
     * A „6 óra 30 perc alvás" fél órája eddig nemhogy elveszett: az egész
     * mondat kiesett. A „8-9 órát aludtam" párja pedig úgy nézett ki, mint egy
     * munka/pihenő ritmus, és az időzítőt állította be helyette.
     */
    @Test public void hoursWithMinutesAndRanges() {
        assertEquals(6.5, Sleep.parse("6 óra 30 perc alvás"), 0.01);
        assertEquals(7.8, Sleep.parse("aludtam 7 órát 45 percet"), 0.01);
        assertEquals(8.5, Sleep.parse("8-9 órát aludtam"), 0.01);
        assertEquals(Sentence.Kind.SLEEP,
                Sentence.of("8-9 órát aludtam", null, 1_753_869_600_000L));
        // Az időzítő-mondat érintetlen: nincs benne alvás-szó.
        assertEquals(Sentence.Kind.INTERVAL,
                Sentence.of("8 kör 40 mp munka 20 mp pihenő", null, 1_753_869_600_000L));
    }

    @Test public void notEverySentenceWithHoursIsSleep() {
        assertEquals(-1, Sleep.parse("8 óra kondi"), 0.01);
        assertEquals(-1, Sleep.parse("8 órakor keltem"), 0.01);
        assertEquals(-1, Sleep.parse("dolgoztam 8 órát"), 0.01);
        assertEquals(-1, Sleep.parse("78 kg vagyok"), 0.01);
        assertEquals(-1, Sleep.parse(null), 0.01);
        // Életszerűtlen érték nem éjszaka.
        assertEquals(-1, Sleep.parse("aludtam 20 órát"), 0.01);
        assertEquals(-1, Sleep.parse("aludtam 1 órát"), 0.01);
    }

    @Test public void verdictBandsAreSensible() {
        assertTrue(Sleep.verdict(5).contains("kevés"));
        assertTrue(Sleep.verdict(6.5).contains("hét óra alatt"));
        assertTrue(Sleep.verdict(8).contains("rendben"));
        assertTrue(Sleep.verdict(10).contains("hosszú"));
        assertEquals("", Sleep.verdict(-1));
    }

    /** Az útbaigazító a Profilba küldi – de csak a kimondott alvást. */
    @Test public void routedToTheProfile() {
        assertEquals(Sentence.Kind.SLEEP,
                Sentence.of("aludtam 8 órát", null, 1_753_869_600_000L));
        assertEquals(Sentence.Kind.WORKOUT,
                Sentence.of("8 óra kondi", null, 1_753_869_600_000L));
    }

    /**
     * A töltelékszó belefér, a feltételes mód nem.
     *
     * Az „aludtam kb 6,5 órát" hétköznapi mondat; az „aludtam volna nyolc
     * órát" viszont egy rossz éjszaka panasza – abból bejegyzést csinálni
     * pont a fordítottját rögzítené annak, ami történt.
     */
    @Test public void fillerWordsYesConditionalNo() {
        assertEquals(6.5, Sleep.parse("aludtam kb 6,5 órát"), 0.01);
        assertEquals(7, Sleep.parse("aludtam vagy 7 órát"), 0.01);
        assertEquals(6.5, Sleep.parse("jól aludtam, kb 6 és fél órát"), 0.01);
        assertEquals(-1, Sleep.parse("aludtam volna nyolc órát"), 0.01);
        assertEquals(-1, Sleep.parse("bárcsak aludtam volna 9 órát"), 0.01);
    }

    /**
     * Lefekvés és ébredés: a kivonást ne a felhasználó végezze el.
     *
     * Sokan nem a hosszat írják le, hanem két időpontot – az óra is így
     * méri. Eddig ezekből semmi nem lett: „este 11-kor feküdtem, reggel
     * 7-kor keltem" ugyanolyan üres válasz volt, mint egy értelmetlen szöveg.
     */
    @Test public void twoClockTimesBecomeALength() {
        assertEquals(8.0, Sleep.parse("este 11-kor feküdtem, reggel 7-kor keltem"), 0.01);
        assertEquals(7.8, Sleep.parse("22:30-tól 6:15-ig aludtam"), 0.05);
        assertEquals(7.5, Sleep.parse("23:00-kor aludtam el, 6:30-kor ébredtem"), 0.01);
        assertEquals(8.0, Sleep.parse("lefeküdtem 23 órakor, felkeltem 7 órakor"), 0.01);
        assertEquals(8.0, Sleep.parse("este 10 és reggel 6 között aludtam"), 0.01);
        // A magyar „fél tizenegy" tíz harminc – és este értendő.
        assertEquals(7.5, Sleep.parse("fél 11-kor feküdtem le és 6-kor keltem"), 0.01);
        // Az óra-app kiírása hossz, nem időpont: a perc sem veszhet el.
        assertEquals(6.5, Sleep.parse("alvás 6:30"), 0.01);
        // Alvás-szó nélkül nincs bejegyzés: az edzés-időpont nem éjszaka.
        assertEquals(-1, Sleep.parse("18:00-tól 19:30-ig kondi"), 0.01);
        assertEquals(-1, Sleep.parse("edzés 6-kor és 18-kor"), 0.01);
    }
}
