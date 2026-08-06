package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Testsúly és testzsír egy mondatból.
 *
 * A kilogramm a legterheltebb mértékegység az appban: ugyanaz a „80 kg” lehet
 * munkasúly, bevásárlás és testsúly is. A tesztek fele ezért arról szól, mit
 * NEM szabad mérésnek venni – egy félreértett adat a súlytrendet, a BMI-t és
 * a kalóriacél-ajánlást is elrontja.
 */
public class BodyParseTest {

    private static void kg(String q, double expect) {
        BodyParse.Body b = BodyParse.parse(q);
        assertEquals(q, expect, b.kg, 0.001);
    }

    private static void none(String q) {
        assertTrue(q + " -> " + BodyParse.parse(q).label(), BodyParse.parse(q).isEmpty());
    }

    @Test public void theScaleReadingIsUnderstood() {
        kg("ma reggel 78,4 kg", 78.4);
        kg("78 kiló vagyok", 78);
        kg("mérleg: 81,2", 81.2);
        kg("81,2 kg", 81.2);
        kg("testsúly 78,4 kg", 78.4);
        kg("ma 80 kg voltam", 80);
        kg("reggel 79", 79);
        kg("85 kilo lettem", 85);
        kg("78,4", 78.4);
    }

    @Test public void bodyFatIsUnderstoodToo() {
        assertEquals(18, BodyParse.parse("18% testzsír").fatPct, 0.001);
        assertEquals(18, BodyParse.parse("testzsír 18").fatPct, 0.001);
        BodyParse.Body b = BodyParse.parse("78,4 kg és 18% testzsír");
        assertEquals(78.4, b.kg, 0.001);
        assertEquals(18, b.fatPct, 0.001);
        // A testzsír száma nem lehet másodszor testsúly is.
        assertEquals("78,4 kg  ·  18% testzsír", b.label());
    }

    /**
     * Amit más mond ki kilóban, az nem a testsúly.
     *
     * A munkasúly és a bevásárlás ugyanabban a mértékegységben érkezik – ha
     * ezekből mérés lenne, a súlytrend hetente ugrálna száz kilót.
     */
    @Test public void someoneElsesKilogramsAreNotYours() {
        for (String q : new String[]{"fekvenyomás 80 kg", "guggolás 3x10 100 kg",
                "80 kg-ot nyomtam", "vettem 2 kg almát", "hoztam 5 kg krumplit",
                "ma 10 km futás", "2 tojás", "30 perc futás", "húszonöt éves vagyok",
                "150 g csirkemell", "3x10 fekvenyomás 60 kg"})
            none(q);
    }

    /** Az életszerűtlen érték nem mérés, hanem elgépelés. */
    @Test public void impossibleNumbersAreNotMeasurements() {
        none("5 kg");
        none("300 kg");
        none("fogytam 2 kilót");     // a 2 nem testsúly – a változás nem mérés
        assertTrue(BodyParse.parse("1% testzsír").isEmpty());
        assertTrue(BodyParse.parse("80% testzsír").isEmpty());
    }

    /** Üres és értelmetlen bemenetre sem szabad elszállni. */
    @Test public void junkStaysJunk() {
        none(null);
        none("");
        none("   ");
        none("jó napom volt");
        none("asdfgh");
    }

    /** A mérés-mondat az útbaigazítóban is a Profilra mutat. */
    @Test public void theRouterSendsMeasurementsToTheProfile() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(Sentence.Kind.BODY, Sentence.of("ma reggel 78,4 kg", all, 0));
        assertEquals(Sentence.Kind.BODY, Sentence.of("78 kiló vagyok", all, 0));
        assertEquals(Sentence.Kind.BODY, Sentence.of("18% testzsír", all, 0));
        // …de a többi mondat marad a saját naplójánál.
        assertEquals(Sentence.Kind.STRENGTH, Sentence.of("3x10 fekvenyomás 60 kg", all, 0));
        assertEquals(Sentence.Kind.MEAL, Sentence.of("150 g csirkemell", all, 0));
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("30 perc futás", all, 0));
    }
}
