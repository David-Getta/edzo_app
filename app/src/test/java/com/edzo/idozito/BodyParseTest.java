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

    /** A mérleget sokan hangosan olvassák fel – és úgy is írják le. */
    @Test public void spelledOutNumbersAreMeasurementsToo() {
        kg("hetvennyolc kiló vagyok", 78);
        kg("nyolcvanöt kiló vagyok", 85);
        kg("ma reggel hetvennyolc kiló", 78);
        kg("száz kiló vagyok", 100);
        assertEquals(18, BodyParse.parse("tizennyolc százalék testzsír").fatPct, 0.001);
        // …de a kiírt szám sem tesz mérést abból, ami nem az.
        none("fekvenyomás nyolcvan kiló");
        none("kettő tojás");
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

    /**
     * A cél, a becslés és a magasság nem mérés.
     *
     * A „70 kg alatt vagyok” nem hetven kiló, a „szeretnék 70 kg lenni” meg
     * egyáltalán nem adat – egy vágyból csinált bejegyzés a súlytrendet is
     * elrontaná. A magasság ráadásul beleesik a testsúly sávjába: a
     * „180 cm és 80 kg vagyok” mondatban a 180 áll elöl.
     */
    @Test public void goalsGuessesAndHeightsAreNotMeasurements() {
        none("70 kg alatt vagyok");
        none("szeretnék 70 kg lenni");
        none("kb 80 kg vagyok");
        none("78 kg körül vagyok");
        kg("180 cm és 80 kg vagyok", 80);
        kg("80 kg vagyok és 180 cm", 80);
    }

    /** A megnevezett nap ugyanolyan időpont, mint a napszak. */
    @Test public void aNamedDayIsJustATimestampToo() {
        kg("kedden 80 kg voltam", 80);
        kg("szombaton 79 kg", 79);
        kg("ma 80 kg voltam", 80);
    }

    /**
     * Az app ÖSSZES többi példamondata közül egy se legyen mérés.
     *
     * A mérés-felismerő a lánc végén áll, de a Profil képernyőn magában is
     * dolgozik – ott nincs ki elé álljon. Ez a söprés azt őrzi, hogy egy
     * étkezés vagy egy sorozat sose csapódjon be testsúlyként.
     */
    @Test public void noOtherExampleSentenceLooksLikeAMeasurement() {
        StringBuilder bad = new StringBuilder();
        for (String[] list : new String[][]{Examples.MEAL, Examples.BULK, Examples.SET,
                Examples.INTERVAL})
            for (String q : list) {
                BodyParse.Body b = BodyParse.parse(q);
                if (!b.isEmpty()) bad.append("\n  ").append(q).append(" -> ").append(b.label());
            }
        assertEquals("mérésnek nézett mondat:" + bad, 0, bad.length());
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

    /**
     * Huszonhat valós mérés-mondattal végigpróbálva ez az öt maradt ki.
     *
     * Az igekötő miatt a „lefogytam" nem ugyanaz a szó, mint a „fogytam”; a
     * „kilogramm” kiírva sem volt mértékegység; a „súly” önmagában nem
     * számított kimondásnak; a százalékban megadott zsír pedig egyáltalán
     * nem – pedig aki így ír, magáról beszél.
     */
    @Test public void everydayPhrasingsAreUnderstood() {
        assertEquals(76, BodyParse.parse("lefogytam 76 kilóra").kg, 0.01);
        assertEquals(85, BodyParse.parse("felmentem 85-re").kg, 0.01);
        assertEquals(80, BodyParse.parse("80 kilogramm").kg, 0.01);
        assertEquals(77, BodyParse.parse("reggeli súly 77").kg, 0.01);
        BodyParse.Body b = BodyParse.parse("78 kg 18% zsír");
        assertEquals(78, b.kg, 0.01);
        assertEquals(18, b.fatPct, 0.01);
    }

    /** Amitől eddig sem lett mérés, attól ezután sem lesz. */
    @Test public void theseStillAreNotMeasurements() {
        for (String q : new String[]{"fekvenyomás 80 kg", "100 g zsír", "2 kg krumpli",
                "nyomtam 100 kg-ot", "70 kg alatt szeretnék lenni", "80 kg-os súlyzó",
                "zsírégető edzés 40 perc", "vettem 2 kg almát"})
            assertTrue("mérés lett belőle: " + q, BodyParse.parse(q).isEmpty());
    }

    /**
     * A súlyt ÉS a zsírt is kimondó mondat a Profilé, nem az Étrendé.
     *
     * A „zsír” szót az ételadatbázis is ismeri (konyhai zsír), ezért a
     * mondat étkezésként indult volna el.
     */
    @Test public void weightAndFatTogetherGoToTheProfile() {
        assertEquals(Sentence.Kind.BODY, Sentence.of("78 kg 18% zsír",
                java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
        // A konyhai zsír viszont marad étel.
        assertEquals(Sentence.Kind.MEAL, Sentence.of("100 g zsír",
                java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
    }

    /**
     * Körfogat a mondatból: „derék 84 cm”, „csípő: 96”, „84 cm derék”.
     *
     * A testrész neve kötelező – centiméterből magasság is lehet, meg a
     * konyhapult hossza is. Ugyanezért nem lesz körfogat abból, hogy
     * „180 cm magas vagyok”.
     */
    @Test public void tapeMeasurementsAreUnderstood() {
        assertEquals(84, BodyParse.parse("derék 84 cm").cm[0], 0.01);
        assertEquals(84, BodyParse.parse("derék: 84").cm[0], 0.01);
        assertEquals(84, BodyParse.parse("84 cm derék").cm[0], 0.01);
        assertEquals(96, BodyParse.parse("csípő 96 cm").cm[1], 0.01);
        assertEquals(102, BodyParse.parse("mellkas 102 cm").cm[2], 0.01);
        assertEquals(58, BodyParse.parse("comb 58 cm").cm[3], 0.01);
        assertEquals(40, BodyParse.parse("bicepszem 40 cm").cm[4], 0.01);
        assertEquals(90, BodyParse.parse("hasam 90 cm").cm[0], 0.01);
        // A „körfogat" szó közbeékelődhet, egybe- és különírva is.
        assertEquals(92, BodyParse.parse("haskörfogat 92 cm").cm[0], 0.01);
        assertEquals(84, BodyParse.parse("derék körfogat: 84").cm[0], 0.01);
        assertEquals(36, BodyParse.parse("bicepsz körfogat 36 cm").cm[4], 0.01);

        // Súly, zsír és körfogat EGY mondatban, egy mérésben.
        BodyParse.Body b = BodyParse.parse("78 kg, 18% testzsír, derék 84 cm");
        assertEquals(78, b.kg, 0.01);
        assertEquals(18, b.fatPct, 0.01);
        assertEquals(84, b.cm[0], 0.01);

        // A körfogatként elhasznált szám nem lehet másodszor kiló.
        assertEquals(0, BodyParse.parse("derék 84 cm").kg, 0.01);
    }

    /** Ami nem körfogat, abból ne legyen az. */
    @Test public void notEveryCentimeterIsACircumference() {
        for (String q : new String[]{"180 cm magas vagyok", "magasság 180 cm",
                "derék 300 cm", "combhajlítás 3x12", "fekvenyomás 80 kg",
                // A kilós szám súlyzó, nem mérőszalag.
                "bicepsz 20 kg", "comb 58 kg-os lábtolás"})
            assertTrue("körfogat lett belőle: " + q, !BodyParse.parse(q).hasCm());
    }

    /** A derék/magasság arány a szakirodalom hüvelykujjszabálya. */
    @Test public void waistToHeightRatio() {
        assertEquals(0.47, Profile.waistToHeight(84, 178), 0.005);
        assertEquals(-1, Profile.waistToHeight(0, 178), 0.001);
        assertEquals(-1, Profile.waistToHeight(84, 0), 0.001);
        assertEquals("egészséges sávban", Profile.waistVerdict(0.47));
        assertEquals("érdemes figyelni rá", Profile.waistVerdict(0.55));
        assertEquals("kockázati sávban", Profile.waistVerdict(0.62));
        assertEquals("", Profile.waistVerdict(-1));
    }
}
