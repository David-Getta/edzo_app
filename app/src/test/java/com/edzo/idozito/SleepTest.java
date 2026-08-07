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
}
