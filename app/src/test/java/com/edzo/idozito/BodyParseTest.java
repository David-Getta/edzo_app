package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests\u00faly \u00e9s testzs\u00edr egy mondatb\u00f3l.
 *
 * A kilogramm a legterheltebb m\u00e9rt\u00e9kegys\u00e9g az appban: ugyanaz a \u201e80 kg\u201d lehet
 * munkas\u00faly, bev\u00e1s\u00e1rl\u00e1s \u00e9s tests\u00faly is. A tesztek fele ez\u00e9rt arr\u00f3l sz\u00f3l, mit
 * NEM szabad m\u00e9r\u00e9snek venni \u2013 egy f\u00e9lre\u00e9rtett adat a s\u00falytrendet, a BMI-t \u00e9s
 * a kal\u00f3riac\u00e9l-aj\u00e1nl\u00e1st is elrontja.
 */
public class BodyParseTest {

    /** R\u00f6gz\u00edtett pillanat az \u00fatbaigaz\u00edt\u00f3-teszteknek. */
    private static final long NOW = 1_753_869_600_000L;

    private static void kg(String q, double expect) {
        BodyParse.Body b = BodyParse.parse(q);
        assertEquals(q, expect, b.kg, 0.001);
    }

    private static void none(String q) {
        assertTrue(q + " -> " + BodyParse.parse(q).label(), BodyParse.parse(q).isEmpty());
    }

    @Test public void theScaleReadingIsUnderstood() {
        kg("ma reggel 78,4 kg", 78.4);
        kg("78 kil\u00f3 vagyok", 78);
        kg("m\u00e9rleg: 81,2", 81.2);
        kg("81,2 kg", 81.2);
        kg("tests\u00faly 78,4 kg", 78.4);
        kg("ma 80 kg voltam", 80);
        kg("reggel 79", 79);
        kg("85 kilo lettem", 85);
        kg("78,4", 78.4);
    }

    /** A m\u00e9rleget sokan hangosan olvass\u00e1k fel \u2013 \u00e9s \u00fagy is \u00edrj\u00e1k le. */
    @Test public void spelledOutNumbersAreMeasurementsToo() {
        kg("hetvennyolc kil\u00f3 vagyok", 78);
        kg("nyolcvan\u00f6t kil\u00f3 vagyok", 85);
        kg("ma reggel hetvennyolc kil\u00f3", 78);
        kg("sz\u00e1z kil\u00f3 vagyok", 100);
        assertEquals(18, BodyParse.parse("tizennyolc sz\u00e1zal\u00e9k testzs\u00edr").fatPct, 0.001);
        // \u2026de a ki\u00edrt sz\u00e1m sem tesz m\u00e9r\u00e9st abb\u00f3l, ami nem az.
        none("fekvenyom\u00e1s nyolcvan kil\u00f3");
        none("kett\u0151 toj\u00e1s");
    }

    @Test public void bodyFatIsUnderstoodToo() {
        assertEquals(18, BodyParse.parse("18% testzs\u00edr").fatPct, 0.001);
        assertEquals(18, BodyParse.parse("testzs\u00edr 18").fatPct, 0.001);
        BodyParse.Body b = BodyParse.parse("78,4 kg \u00e9s 18% testzs\u00edr");
        assertEquals(78.4, b.kg, 0.001);
        assertEquals(18, b.fatPct, 0.001);
        // A testzs\u00edr sz\u00e1ma nem lehet m\u00e1sodszor tests\u00faly is.
        assertEquals("78,4 kg  \u00b7  18% testzs\u00edr", b.label());
    }

    /**
     * Amit m\u00e1s mond ki kil\u00f3ban, az nem a tests\u00faly.
     *
     * A munkas\u00faly \u00e9s a bev\u00e1s\u00e1rl\u00e1s ugyanabban a m\u00e9rt\u00e9kegys\u00e9gben \u00e9rkezik \u2013 ha
     * ezekb\u0151l m\u00e9r\u00e9s lenne, a s\u00falytrend hetente ugr\u00e1lna sz\u00e1z kil\u00f3t.
     */
    @Test public void someoneElsesKilogramsAreNotYours() {
        for (String q : new String[]{"fekvenyom\u00e1s 80 kg", "guggol\u00e1s 3x10 100 kg",
                "80 kg-ot nyomtam", "vettem 2 kg alm\u00e1t", "hoztam 5 kg krumplit",
                "ma 10 km fut\u00e1s", "2 toj\u00e1s", "30 perc fut\u00e1s", "h\u00faszon\u00f6t \u00e9ves vagyok",
                "150 g csirkemell", "3x10 fekvenyom\u00e1s 60 kg"})
            none(q);
    }

    /** Az \u00e9letszer\u0171tlen \u00e9rt\u00e9k nem m\u00e9r\u00e9s, hanem elg\u00e9pel\u00e9s. */
    @Test public void impossibleNumbersAreNotMeasurements() {
        none("5 kg");
        none("300 kg");
        none("fogytam 2 kil\u00f3t");     // a 2 nem tests\u00faly \u2013 a v\u00e1ltoz\u00e1s nem m\u00e9r\u00e9s
        assertTrue(BodyParse.parse("1% testzs\u00edr").isEmpty());
        assertTrue(BodyParse.parse("80% testzs\u00edr").isEmpty());
    }

    /** \u00dcres \u00e9s \u00e9rtelmetlen bemenetre sem szabad elsz\u00e1llni. */
    @Test public void junkStaysJunk() {
        none(null);
        none("");
        none("   ");
        none("j\u00f3 napom volt");
        none("asdfgh");
    }

    /**
     * A c\u00e9l, a becsl\u00e9s \u00e9s a magass\u00e1g nem m\u00e9r\u00e9s.
     *
     * A \u201e70 kg alatt vagyok\u201d nem hetven kil\u00f3, a \u201eszeretn\u00e9k 70 kg lenni\u201d meg
     * egy\u00e1ltal\u00e1n nem adat \u2013 egy v\u00e1gyb\u00f3l csin\u00e1lt bejegyz\u00e9s a s\u00falytrendet is
     * elrontan\u00e1. A magass\u00e1g r\u00e1ad\u00e1sul beleesik a tests\u00faly s\u00e1vj\u00e1ba: a
     * \u201e180 cm \u00e9s 80 kg vagyok\u201d mondatban a 180 \u00e1ll el\u00f6l.
     */
    @Test public void goalsGuessesAndHeightsAreNotMeasurements() {
        none("70 kg alatt vagyok");
        none("szeretn\u00e9k 70 kg lenni");
        none("kb 80 kg vagyok");
        none("78 kg k\u00f6r\u00fcl vagyok");
        kg("180 cm \u00e9s 80 kg vagyok", 80);
        kg("80 kg vagyok \u00e9s 180 cm", 80);
    }

    /** A megnevezett nap ugyanolyan id\u0151pont, mint a napszak. */
    @Test public void aNamedDayIsJustATimestampToo() {
        kg("kedden 80 kg voltam", 80);
        kg("szombaton 79 kg", 79);
        kg("ma 80 kg voltam", 80);
    }

    /**
     * Az app \u00d6SSZES t\u00f6bbi p\u00e9ldamondata k\u00f6z\u00fcl egy se legyen m\u00e9r\u00e9s.
     *
     * A m\u00e9r\u00e9s-felismer\u0151 a l\u00e1nc v\u00e9g\u00e9n \u00e1ll, de a Profil k\u00e9perny\u0151n mag\u00e1ban is
     * dolgozik \u2013 ott nincs ki el\u00e9 \u00e1lljon. Ez a s\u00f6pr\u00e9s azt \u0151rzi, hogy egy
     * \u00e9tkez\u00e9s vagy egy sorozat sose csap\u00f3djon be tests\u00falyk\u00e9nt.
     */
    @Test public void noOtherExampleSentenceLooksLikeAMeasurement() {
        StringBuilder bad = new StringBuilder();
        for (String[] list : new String[][]{Examples.MEAL, Examples.BULK, Examples.SET,
                Examples.INTERVAL})
            for (String q : list) {
                BodyParse.Body b = BodyParse.parse(q);
                if (!b.isEmpty()) bad.append("\n  ").append(q).append(" -> ").append(b.label());
            }
        assertEquals("m\u00e9r\u00e9snek n\u00e9zett mondat:" + bad, 0, bad.length());
    }

    /** A m\u00e9r\u00e9s-mondat az \u00fatbaigaz\u00edt\u00f3ban is a Profilra mutat. */
    @Test public void theRouterSendsMeasurementsToTheProfile() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(Sentence.Kind.BODY, Sentence.of("ma reggel 78,4 kg", all, 0));
        assertEquals(Sentence.Kind.BODY, Sentence.of("78 kil\u00f3 vagyok", all, 0));
        assertEquals(Sentence.Kind.BODY, Sentence.of("18% testzs\u00edr", all, 0));
        // \u2026de a t\u00f6bbi mondat marad a saj\u00e1t napl\u00f3j\u00e1n\u00e1l.
        assertEquals(Sentence.Kind.STRENGTH, Sentence.of("3x10 fekvenyom\u00e1s 60 kg", all, 0));
        assertEquals(Sentence.Kind.MEAL, Sentence.of("150 g csirkemell", all, 0));
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("30 perc fut\u00e1s", all, 0));
    }

    /**
     * Huszonhat val\u00f3s m\u00e9r\u00e9s-mondattal v\u00e9gigpr\u00f3b\u00e1lva ez az \u00f6t maradt ki.
     *
     * Az igek\u00f6t\u0151 miatt a \u201elefogytam" nem ugyanaz a sz\u00f3, mint a \u201efogytam\u201d; a
     * \u201ekilogramm\u201d ki\u00edrva sem volt m\u00e9rt\u00e9kegys\u00e9g; a \u201es\u00faly\u201d \u00f6nmag\u00e1ban nem
     * sz\u00e1m\u00edtott kimond\u00e1snak; a sz\u00e1zal\u00e9kban megadott zs\u00edr pedig egy\u00e1ltal\u00e1n
     * nem \u2013 pedig aki \u00edgy \u00edr, mag\u00e1r\u00f3l besz\u00e9l.
     */
    @Test public void everydayPhrasingsAreUnderstood() {
        assertEquals(76, BodyParse.parse("lefogytam 76 kil\u00f3ra").kg, 0.01);
        assertEquals(85, BodyParse.parse("felmentem 85-re").kg, 0.01);
        assertEquals(80, BodyParse.parse("80 kilogramm").kg, 0.01);
        assertEquals(77, BodyParse.parse("reggeli s\u00faly 77").kg, 0.01);
        BodyParse.Body b = BodyParse.parse("78 kg 18% zs\u00edr");
        assertEquals(78, b.kg, 0.01);
        assertEquals(18, b.fatPct, 0.01);
    }

    /** Amit\u0151l eddig sem lett m\u00e9r\u00e9s, att\u00f3l ezut\u00e1n sem lesz. */
    @Test public void theseStillAreNotMeasurements() {
        for (String q : new String[]{"fekvenyom\u00e1s 80 kg", "100 g zs\u00edr", "2 kg krumpli",
                "nyomtam 100 kg-ot", "70 kg alatt szeretn\u00e9k lenni", "80 kg-os s\u00falyz\u00f3",
                "zs\u00edr\u00e9get\u0151 edz\u00e9s 40 perc", "vettem 2 kg alm\u00e1t"})
            assertTrue("m\u00e9r\u00e9s lett bel\u0151le: " + q, BodyParse.parse(q).isEmpty());
    }

    /**
     * A s\u00falyt \u00c9S a zs\u00edrt is kimond\u00f3 mondat a Profil\u00e9, nem az \u00c9trend\u00e9.
     *
     * A \u201ezs\u00edr\u201d sz\u00f3t az \u00e9teladatb\u00e1zis is ismeri (konyhai zs\u00edr), ez\u00e9rt a
     * mondat \u00e9tkez\u00e9sk\u00e9nt indult volna el.
     */
    @Test public void weightAndFatTogetherGoToTheProfile() {
        assertEquals(Sentence.Kind.BODY, Sentence.of("78 kg 18% zs\u00edr",
                java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
        // A konyhai zs\u00edr viszont marad \u00e9tel.
        assertEquals(Sentence.Kind.MEAL, Sentence.of("100 g zs\u00edr",
                java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
    }

    /**
     * K\u00f6rfogat a mondatb\u00f3l: \u201eder\u00e9k 84 cm\u201d, \u201ecs\u00edp\u0151: 96\u201d, \u201e84 cm der\u00e9k\u201d.
     *
     * A testr\u00e9sz neve k\u00f6telez\u0151 \u2013 centim\u00e9terb\u0151l magass\u00e1g is lehet, meg a
     * konyhapult hossza is. Ugyanez\u00e9rt nem lesz k\u00f6rfogat abb\u00f3l, hogy
     * \u201e180 cm magas vagyok\u201d.
     */
    @Test public void tapeMeasurementsAreUnderstood() {
        assertEquals(84, BodyParse.parse("der\u00e9k 84 cm").cm[0], 0.01);
        assertEquals(84, BodyParse.parse("der\u00e9k: 84").cm[0], 0.01);
        assertEquals(84, BodyParse.parse("84 cm der\u00e9k").cm[0], 0.01);
        assertEquals(96, BodyParse.parse("cs\u00edp\u0151 96 cm").cm[1], 0.01);
        assertEquals(102, BodyParse.parse("mellkas 102 cm").cm[2], 0.01);
        assertEquals(58, BodyParse.parse("comb 58 cm").cm[3], 0.01);
        assertEquals(40, BodyParse.parse("bicepszem 40 cm").cm[4], 0.01);
        assertEquals(90, BodyParse.parse("hasam 90 cm").cm[0], 0.01);
        // A \u201ek\u00f6rfogat" sz\u00f3 k\u00f6zbe\u00e9kel\u0151dhet, egybe- \u00e9s k\u00fcl\u00f6n\u00edrva is.
        assertEquals(92, BodyParse.parse("hask\u00f6rfogat 92 cm").cm[0], 0.01);
        assertEquals(84, BodyParse.parse("der\u00e9k k\u00f6rfogat: 84").cm[0], 0.01);
        assertEquals(36, BodyParse.parse("bicepsz k\u00f6rfogat 36 cm").cm[4], 0.01);

        // S\u00faly, zs\u00edr \u00e9s k\u00f6rfogat EGY mondatban, egy m\u00e9r\u00e9sben.
        BodyParse.Body b = BodyParse.parse("78 kg, 18% testzs\u00edr, der\u00e9k 84 cm");
        assertEquals(78, b.kg, 0.01);
        assertEquals(18, b.fatPct, 0.01);
        assertEquals(84, b.cm[0], 0.01);

        // A k\u00f6rfogatk\u00e9nt elhaszn\u00e1lt sz\u00e1m nem lehet m\u00e1sodszor kil\u00f3.
        assertEquals(0, BodyParse.parse("der\u00e9k 84 cm").kg, 0.01);
    }

    /** Ami nem k\u00f6rfogat, abb\u00f3l ne legyen az. */
    @Test public void notEveryCentimeterIsACircumference() {
        for (String q : new String[]{"180 cm magas vagyok", "magass\u00e1g 180 cm",
                "der\u00e9k 300 cm", "combhajl\u00edt\u00e1s 3x12", "fekvenyom\u00e1s 80 kg",
                // A kil\u00f3s sz\u00e1m s\u00falyz\u00f3, nem m\u00e9r\u0151szalag.
                "bicepsz 20 kg", "comb 58 kg-os l\u00e1btol\u00e1s"})
            assertTrue("k\u00f6rfogat lett bel\u0151le: " + q, !BodyParse.parse(q).hasCm());
    }

    /**
     * Felsorolt k\u00f6rfogatok: a vessz\u0151 elv\u00e1laszt, nem tizedesjegyet nyit.
     *
     * A \u201eder\u00e9k 84, cs\u00edp\u0151 95" mondatb\u00f3l eddig a DER\u00c9K esett ki \u2013 a sz\u00e1m ut\u00e1n
     * \u00e1ll\u00f3 vessz\u0151 elrontotta a mint\u00e1t \u2013, \u00e9s a nyolcvann\u00e9gy centib\u0151l a
     * s\u00faly-felismer\u0151n\u00e9l nyolcvann\u00e9gy kil\u00f3 lett. Egy m\u00e9r\u00e9sb\u0151l \u00edgy egyszerre
     * lett hi\u00e1nyos adat \u00e9s hamis tests\u00faly.
     */
    @Test public void severalGirthsInOneSentence() {
        BodyParse.Body b = BodyParse.parse("der\u00e9k 84, cs\u00edp\u0151 95");
        assertEquals(84, b.cm[0], 0.01);
        assertEquals(95, b.cm[1], 0.01);
        assertEquals(0, b.kg, 0.01);
        BodyParse.Body c = BodyParse.parse("der\u00e9k 84, cs\u00edp\u0151 95, mellkas 100");
        assertEquals(100, c.cm[2], 0.01);
        // A tizedesjegy v\u00e1ltozatlanul tizedesjegy.
        assertEquals(84.5, BodyParse.parse("der\u00e9k 84,5 cm").cm[0], 0.01);
    }

    /**
     * A comb \u00e9s a mell egyszerre testr\u00e9sz \u00e9s \u00e9tel \u2013 a centi d\u00f6nt.
     *
     * A \u201ecomb 58 cm" eddig csirkecombk\u00e9nt ment az \u00c9trendbe, mert az
     * \u00e9tel-felismer\u0151 hamarabb sz\u00f3lal meg. Ki\u00edrt m\u00e9rt\u00e9kegys\u00e9ggel viszont
     * nincs k\u00e9ts\u00e9g; n\u00e9lk\u00fcle marad minden a r\u00e9giben.
     */
    @Test public void girthWithUnitBeatsTheFood() {
        assertEquals(Sentence.Kind.BODY,
                Sentence.of("comb 58 cm", java.util.Arrays.asList(Foods.ALL), NOW));
        assertEquals(Sentence.Kind.BODY,
                Sentence.of("mellkas 100 cm", java.util.Arrays.asList(Foods.ALL), NOW));
        // M\u00e9rt\u00e9kegys\u00e9g n\u00e9lk\u00fcl az \u00e9tel marad \u00e9tel.
        assertEquals(Sentence.Kind.MEAL,
                Sentence.of("mell 20 dkg csirke", java.util.Arrays.asList(Foods.ALL), NOW));
        assertEquals(Sentence.Kind.MEAL,
                Sentence.of("2 csirkecomb", java.util.Arrays.asList(Foods.ALL), NOW));
    }

    /**
     * A hosszabb sz\u00e1m ELEJE nem tests\u00faly.
     *
     * A minta sz\u00e1mjegy-hat\u00e1r n\u00e9lk\u00fcl dolgozott, \u00edgy az \u201e1500" els\u0151 h\u00e1rom
     * jegy\u00e9b\u0151l sz\u00e1z\u00f6tven kil\u00f3 lett, a \u201e10000"-b\u0151l sz\u00e1z. Egy elg\u00e9pelt sz\u00e1m
     * teh\u00e1t nem hiba\u00fczenetet adott, hanem egy hihet\u0151, de hamis m\u00e9r\u00e9st \u2013 \u00e9s
     * a s\u00falytrend, a BMI \u00e9s a kal\u00f3riac\u00e9l is ebb\u0151l sz\u00e1mol tov\u00e1bb.
     */
    @Test public void theStartOfALongerNumberIsNotAWeight() {
        none("1500");
        none("10000");
        none("1500 kg");
        none("m\u00e9rleg 1500");
        none("2500");
        // Ki\u00edrva ugyanez: a \u201et\u00edzezer" t\u00edzezer, nem sz\u00e1z.
        none("t\u00edzezer");
        none("ezer\u00f6tsz\u00e1z");
        // A val\u00f3di m\u00e9r\u00e9s v\u00e1ltozatlan.
        kg("78,4", 78.4);
        kg("78,4 kg", 78.4);
        kg("100 kg", 100);
    }

    /** Az izomt\u00f6meg nem a tests\u00faly \u2013 a m\u00e9rleg egy sorban \u00edrja ki mindkett\u0151t. */
    @Test public void muscleMassIsNotBodyWeight() {
        BodyParse.Body b = BodyParse.parse("testzs\u00edr 19,5%, izomt\u00f6meg 62 kg");
        assertEquals(19.5, b.fatPct, 0.01);
        assertEquals(0, b.kg, 0.01);
        assertEquals(0, BodyParse.parse("csontt\u00f6meg 3,2 kg, izomt\u00f6meg 62 kg").kg, 0.01);
        // A val\u00f3di s\u00faly mellett ki\u00edrt izomt\u00f6meg nem viszi el a m\u00e9r\u00e9st.
        assertEquals(80.0, BodyParse.parse("80 kg, izomt\u00f6meg 62 kg").kg, 0.01);
    }

    /** A birtokos alak is m\u00e9r\u00e9s: \u201eder\u00e9kb\u0151s\u00e9gem 82 cm", \u201e22% a testzs\u00edrom". */
    @Test public void possessiveFormsAreStillMeasurements() {
        BodyParse.Body a = BodyParse.parse("der\u00e9kb\u0151s\u00e9gem 82 cm lett");
        assertEquals(82.0, a.cm[0], 0.01);
        assertEquals(82.0, BodyParse.parse("der\u00e9kb\u0151s\u00e9g 82 cm").cm[0], 0.01);
        assertEquals(22.0, BodyParse.parse("22% a testzs\u00edrom").fatPct, 0.01);
        assertEquals(22.0,
                BodyParse.parse("az inbody szerint 22% a testzs\u00edrom").fatPct, 0.01);
    }

    /**
     * A napi \u00f6sszefoglal\u00f3ban is ott a m\u00e9r\u00e9s.
     *
     * A \u201e78,2 kg voltam" ugyanaz a m\u00e9r\u00e9s, mint a \u201e78,2 kg vagyok" \u2013 egy
     * hosszabb, t\u00f6bb mondatos napi bejegyz\u00e9sben eddig elveszett, mert a
     * l\u00e9tige m\u00falt ideje nem sz\u00e1m\u00edtott kimond\u00e1snak.
     */
    @Test public void aWholeDayEntryStillCarriesTheMeasurement() {
        kg("78,2 kg voltam", 78.2);
        kg("Ma reggel 6-kor keltem, 78,2 kg voltam. Reggeli: zabk\u00e1sa 60 g tejjel. "
                + "D\u00e9lel\u0151tt 45 perc kondi. Este 5 km fut\u00e1s 28 perc.", 78.2);
        // A konyhai m\u00e9rt\u00e9kegys\u00e9g nem tests\u00faly: a \u201ezabk\u00e1sa 60 g" hatvanas
        // sz\u00e1ma az adag, nem a m\u00e9rleg sz\u00e1ma.
        none("zabk\u00e1sa 60 g");
        none("150 g csirkemell");
    }

    /** A v\u00e1ltoz\u00e1s mondat\u00e1ban a M\u00c1SODIK sz\u00e1m a mai s\u00faly. */
    @Test public void fromToKeepsTheNewValue() {
        // A \u201e80-r\u00f3l 76-ra fogytam" mai \u00e9rt\u00e9ke hetvenhat \u2013 eddig a R\u00c9GI s\u00faly
        // ker\u00fclt a trendbe, vagyis a fogy\u00e1s napj\u00e1n egy s\u00falygyarapod\u00e1s.
        kg("80-r\u00f3l 76-ra fogytam", 76);
        kg("76-r\u00f3l 80-ra h\u00edztam", 80);
        kg("82,5-r\u0151l 79,8-ra fogytam", 79.8);
        // A testzs\u00edr sz\u00e1m\u00e1t nem viszi el.
        assertEquals(15, BodyParse.parse("testzs\u00edr 18-r\u00f3l 15%-ra ment le").fatPct, 0.01);
    }

    /** Az id\u0151 \u00e9s a t\u00e1v sem kil\u00f3 \u2013 az edz\u00e9s sz\u00e1ma nem a m\u00e9rleg\u00e9. */
    @Test public void minutesAndKilometresAreNotKilograms() {
        // Az \u201eeste 45 perc j\u00f3ga, azt\u00e1n 78,9 kg a m\u00e9rlegen" negyven\u00f6t PERCE
        // lett a tests\u00faly \u2013 a val\u00f3di m\u00e9r\u00e9s pedig, ami ott \u00e1llt a mondat m\u00e1sik
        // fel\u00e9ben, elveszett.
        kg("este 45 perc j\u00f3ga, azt\u00e1n 78,9 kg a m\u00e9rlegen", 78.9);
        kg("50 perc kondi, m\u00e9rleg 81,2", 81.2);
        none("45 perc j\u00f3ga");
        none("10 km fut\u00e1s");
    }

    /** A m\u00e1sik napl\u00f3 folytat\u00e1sa nem m\u00e9r\u00e9s: az esti pulzus nem tests\u00faly. */
    @Test public void theOtherLogsContinuationIsNotAWeight() {
        none("nyugalmi pulzus reggel 47, este 62");
        none("aludtam 7 \u00f3r\u00e1t, \u00e9jjel 3");
        // A val\u00f3di m\u00e9r\u00e9s a m\u00e1sik napl\u00f3 mellett is megmarad.
        kg("78,4 kg, aludtam 7 \u00f3r\u00e1t", 78.4);
        kg("ma reggel 78,4, aludtam 7 \u00f3r\u00e1t", 78.4);
        kg("aludtam 7 \u00f3r\u00e1t, s\u00falyom 80 kg", 80);
    }

    /** A l\u00e1z nem tests\u00faly \u2013 pont egy beteg napon rontan\u00e1 el a trendet. */
    @Test public void aFeverIsNotAWeight() {
        none("beteg vagyok, 38 fokos l\u00e1zam van");
        none("38,5 fokos l\u00e1z");
        none("39 fok");
        // A val\u00f3di m\u00e9r\u00e9s v\u00e1ltozatlan.
        kg("ma reggel 78,4 kg voltam", 78.4);
    }

    /** A der\u00e9k/magass\u00e1g ar\u00e1ny a szakirodalom h\u00fcvelykujjszab\u00e1lya. */
    @Test public void waistToHeightRatio() {
        assertEquals(0.47, Profile.waistToHeight(84, 178), 0.005);
        assertEquals(-1, Profile.waistToHeight(0, 178), 0.001);
        assertEquals(-1, Profile.waistToHeight(84, 0), 0.001);
        assertEquals("eg\u00e9szs\u00e9ges s\u00e1vban", Profile.waistVerdict(0.47));
        assertEquals("\u00e9rdemes figyelni r\u00e1", Profile.waistVerdict(0.55));
        assertEquals("kock\u00e1zati s\u00e1vban", Profile.waistVerdict(0.62));
        assertEquals("", Profile.waistVerdict(-1));
    }

    /**
     * A ragozott m\u00e9rt\u00e9kegys\u00e9g is m\u00e9rt\u00e9kegys\u00e9g.
     *
     * A \u201ema reggel 79 kil\u00f3val keltem" a legterm\u00e9szetesebb magyar m\u00e9r\u00e9s-mondat,
     * \u00e9s eddig kiesett: a \u201ekil\u00f3val" bennmaradt a marad\u00e9kban, \u00e9s a mondat ett\u0151l
     * nem sz\u00e1m\u00edtott m\u00e9r\u00e9snek.
     */
    @Test public void anInflectedUnitIsStillAUnit() {
        kg("ma reggel 79 kil\u00f3val keltem", 79);
        kg("78 kil\u00f3val \u00e9bredtem", 78);
        kg("reggel 80,5 kil\u00f3t m\u00e9rtem", 80.5);
        // A magass\u00e1g a m\u00e9r\u00e9s mellett szokott \u00e1llni \u2013 az sem ronthatja el.
        kg("80 kg, 180 cm", 80);
        // A m\u00e9r\u00e9s k\u00f6r\u00fclm\u00e9nye is csak k\u00f6r\u00fclm\u00e9ny, nem adat.
        kg("78,2 kg ma reggel \u00e9hgyomorra", 78.2);
        kg("79 kg zuhany ut\u00e1n", 79);
        // A reggeli h\u00e1rom adat egy mondatban: a m\u00e9r\u00e9s eddig kiesett k\u00f6z\u00fcl\u00fck.
        kg("ma reggel 78,4 kg, aludtam 7 \u00f3r\u00e1t", 78.4);
        kg("aludtam 8 \u00f3r\u00e1t, 78 kg", 78);
        // A v\u00e1s\u00e1rl\u00e1s \u00e9s a munkas\u00faly tov\u00e1bbra sem m\u00e9r\u00e9s.
        none("vettem 2 kg alm\u00e1t");
        none("80 kg-os s\u00fallyal nyomtam");
    }

    /**
     * A kimondott tizedes \u00e9s a m\u00e9r\u00e9s ig\u00e9je.
     *
     * Dikt\u00e1lva senki nem mond vessz\u0151t: \u201ehetvennyolc eg\u00e9sz n\u00e9gy". \u00c9s aki
     * megm\u00e9rte mag\u00e1t, azt le is \u00edrja \u2013 a \u201emegm\u00e9rtem magam" ugyanolyan
     * kimond\u00e1s, mint a \u201em\u00e9rleg".
     */
    @Test public void aSpokenDecimalIsAMeasurement() {
        kg("reggel megm\u00e9rtem magam hetvennyolc eg\u00e9sz n\u00e9gy kil\u00f3 voltam", 78.4);
        kg("hetvennyolc eg\u00e9sz n\u00e9gy", 78.4);
        kg("megm\u00e9rtem magam: 79,2", 79.2);
        // A felsorol\u00e1s nem tizedest\u00f6rt: a t\u00f6rt csak EGY jegy\u0171 lehet.
        none("3 eg\u00e9sz 12 darab");
        // Az alv\u00e1s is \u00e9rti a kimondott t\u00f6rtet.
        assertEquals(7.5, Sleep.parse("h\u00e9t eg\u00e9sz \u00f6t \u00f3r\u00e1t aludtam"), 0.01);
    }

    /**
     * A -r\u00f3l/-re p\u00e1r m\u00e1sodik sz\u00e1ma a mai \u00e9rt\u00e9k.
     *
     * A \u201eder\u00e9kb\u0151s\u00e9gem 90-r\u0151l 86-ra cs\u00f6kkent" k\u00e9t sz\u00e1mot mond: a r\u00e9git \u00e9s a
     * mait. A felismer\u0151 eddig az els\u0151t vette \u2013 pont azt, ami m\u00e1r NEM igaz \u2013,
     * a k\u00f6rfogatn\u00e1l pedig a kett\u0151 egy\u00fctt olyan zavaros maradt, hogy semmi
     * nem lett bel\u0151le.
     */
    @Test public void theSecondNumberOfAFromToPairIsTodaysValue() {
        assertEquals(86, BodyParse.parse("a der\u00e9kb\u0151s\u00e9gem 90-r\u0151l 86-ra cs\u00f6kkent").cm[0], 0.01);
        assertEquals(88, BodyParse.parse("hask\u00f6rfogatom 92 cm-r\u0151l 88-ra").cm[0], 0.01);
        assertEquals(18, BodyParse.parse("testzs\u00edr 22-r\u0151l 18 sz\u00e1zal\u00e9kra").fatPct, 0.01);
        assertEquals(79.8, BodyParse.parse("82,5-r\u0151l 79,8-ra fogytam").kg, 0.01);
        // Az egyszer\u0171 m\u00e9r\u00e9s nem s\u00e9r\u00fcl.
        assertEquals(84, BodyParse.parse("der\u00e9k 84 cm, cs\u00edp\u0151 95 cm").cm[0], 0.01);
        assertEquals(78, BodyParse.parse("78 kg 18% zs\u00edr").kg, 0.01);
    }

    /**
     * Az \u00e9letkor nem tests\u00faly.
     *
     * A \u201ef\u00e9rfi vagyok, 34 \u00e9ves, 182 cm" bemutatkoz\u00f3 mondat harmincn\u00e9gyese az
     * \u00e9vek sz\u00e1ma \u2013 eddig harmincn\u00e9gy kil\u00f3s m\u00e9r\u00e9sk\u00e9nt ker\u00fclt a s\u00falytrendbe.
     */
    @Test public void ageIsNotAWeight() {
        assertEquals(0, BodyParse.parse("f\u00e9rfi vagyok, 34 \u00e9ves, 182 cm").kg, 0.01);
        assertEquals(0, BodyParse.parse("42 \u00e9ves lettem ma").kg, 0.01);
        // A mellette \u00e1ll\u00f3 val\u00f3di m\u00e9r\u00e9s viszont marad.
        assertEquals(78, BodyParse.parse("34 \u00e9ves vagyok \u00e9s 78 kg").kg, 0.01);
        assertEquals(78, BodyParse.parse("78 kg vagyok").kg, 0.01);
    }

    /**
     * Az id\u0151 \u201ealatt"-ja nem \u00f6sszehasonl\u00edt\u00e1s.
     *
     * A \u201e70 kg alatt vagyok" t\u00e9nyleg nem m\u00e9r\u00e9s \u2013 de a \u201e79,2 kg volt a m\u00e9rleg,
     * futottam 8 km-t 45 perc alatt" mondatban az \u201ealatt" a negyven\u00f6t PERC\u00c9,
     * \u00e9s eddig ett\u0151l az eg\u00e9sz reggeli m\u00e9r\u00e9s kiesett.
     */
    @Test public void theUnderOfADurationIsNotAComparison() {
        assertEquals(79.2, BodyParse.parse(
                "79,2 kg volt a m\u00e9rleg, futottam 8 km-t 45 perc alatt").kg, 0.01);
        assertEquals(88, BodyParse.parse(
                "a hask\u00f6rfogatom 92 cm-r\u0151l 88-ra ment le f\u00e9l \u00e9v alatt").cm[0], 0.01);
        // A val\u00f3di \u00f6sszehasonl\u00edt\u00e1s tov\u00e1bbra sem m\u00e9r\u00e9s.
        assertEquals(0, BodyParse.parse("70 kg alatt vagyok").kg, 0.01);
        assertEquals(0, BodyParse.parse("80 kg alatt szeretn\u00e9k lenni").kg, 0.01);
    }

    /**
     * A l\u00e1z nem tests\u00faly \u2013 \u201efok" n\u00e9lk\u00fcl sem.
     *
     * A \u201ebeteg vagyok, l\u00e1zam van 38,5" harmincnyolc \u00e9s f\u00e9l kil\u00f3s m\u00e9r\u00e9sk\u00e9nt
     * ker\u00fclt a s\u00falytrendbe. Pont egy olyan napon, amikor senki nem \u00e1ll
     * m\u00e9rlegre \u2013 \u00e9s a g\u00f6rbe ut\u00e1na hetekig hamis maradt.
     */
    @Test public void aFeverIsNotAWeightWithoutTheDegreeWord() {
        assertEquals(0, BodyParse.parse("beteg vagyok, l\u00e1zam van 38,5").kg, 0.01);
        assertEquals(0, BodyParse.parse("38 fokos l\u00e1zam van").kg, 0.01);
        assertEquals(0, BodyParse.parse("h\u0151emelked\u00e9sem van, 37,8").kg, 0.01);
        // L\u00e1z-sz\u00f3 n\u00e9lk\u00fcl ugyanez a sz\u00e1m val\u00f3di m\u00e9r\u00e9s marad.
        assertEquals(38.5, BodyParse.parse("38,5 kg vagyok").kg, 0.01);
        assertEquals(79.2, BodyParse.parse("reggel 79,2 kg").kg, 0.01);
    }

    /**
     * A helyesb\u00edt\u00e9s m\u00e1sodik sz\u00e1ma az igazi \u2013 a m\u00e9rlegn\u00e9l is.
     *
     * A \u201enem 80 kg vagyok, hanem 78" mondatb\u00f3l NYOLCVAN kil\u00f3 ker\u00fclt a
     * s\u00falytrendbe: pont az, amit a mondat tagad. A hib\u00e1s adat rosszabb, mint
     * a semmilyen \u2013 a g\u00f6rbe \u00e9s a BMI is abb\u00f3l sz\u00e1mol tov\u00e1bb.
     */
    @Test public void theCorrectedWeightWins() {
        assertEquals(78, BodyParse.parse("nem 80 kg vagyok, hanem 78").kg, 0.01);
        assertEquals(76.5, BodyParse.parse("nem 79 kg lettem, hanem 76,5").kg, 0.01);
        assertEquals(80, BodyParse.parse("80 kg vagyok").kg, 0.01);
    }

    /**
     * A k\u00f6rfogat-felsorol\u00e1sban minden \u00e9rt\u00e9k a SAJ\u00c1T testr\u00e9sz\u00e9hez tartozik.
     *
     * A \u201e92 cm der\u00e9k, 100 cm cs\u00edp\u0151, 38 cm comb, 34 cm kar" felsorol\u00e1sban
     * minden sz\u00e1m egyet cs\u00faszott: a der\u00e9k a cs\u00edp\u0151 sz\u00e1m\u00e1t vitte el, a cs\u00edp\u0151 a
     * comb\u00e9t. A felsorol\u00e1s sz\u00f3rendje egys\u00e9ges, \u00e9s a mondat EG\u00c9SZ\u00c9B\u0150L der\u00fcl
     * ki \u2013 testr\u00e9szenk\u00e9nt d\u00f6nteni hib\u00e1s volt, mert a lista k\u00f6zep\u00e9n mindk\u00e9t
     * alak illeszkedik.
     */
    @Test public void everyGirthKeepsItsOwnNumber() {
        BodyParse.Body b = BodyParse.parse(
                "78 kg, 18% zs\u00edr, 92 cm der\u00e9k, 100 cm cs\u00edp\u0151, 38 cm comb, 34 cm kar");
        assertEquals(78, b.kg, 0.01);
        assertEquals(92, b.cm[0], 0.01);
        assertEquals(100, b.cm[1], 0.01);
        assertEquals(38, b.cm[3], 0.01);
        assertEquals(34, b.cm[4], 0.01);
        // A m\u00e1sik sz\u00f3rend is a saj\u00e1tj\u00e1t kapja.
        BodyParse.Body c = BodyParse.parse("der\u00e9k 84 cm, cs\u00edp\u0151 95 cm");
        assertEquals(84, c.cm[0], 0.01);
        assertEquals(95, c.cm[1], 0.01);
        assertEquals(84, BodyParse.parse("84 cm der\u00e9k").cm[0], 0.01);
        assertEquals(92, BodyParse.parse("hask\u00f6rfogatom 92").cm[0], 0.01);
    }

    /**
     * Az id\u0151t\u00e1v csak k\u00eds\u00e9ret a m\u00e9r\u00e9s mellett.
     *
     * A \u201e79,8 kg egy h\u00e9t alatt" \u00e9s a \u201e78 kg k\u00e9t h\u00f3nap ut\u00e1n" m\u00e9r\u00e9s \u2013 eddig a
     * mellette \u00e1ll\u00f3 szavakt\u00f3l az eg\u00e9sz mondat kiesett. (A ki\u00edrt sz\u00e1mn\u00e9v miatt
     * a maskTimeUnder a ford\u00edt\u00e1s EL\u0150TT fut: az \u201eegy h\u00e9t" a digits() ut\u00e1n m\u00e1r
     * \u201e1 7", \u00e9s ott az \u201ealatt" \u00f6sszehasonl\u00edt\u00e1snak l\u00e1tszana.)
     */
    @Test public void theTimeSpanIsOnlyCompanyForTheMeasurement() {
        assertEquals(79.8, BodyParse.parse("79,8 kg egy h\u00e9t alatt").kg, 0.01);
        assertEquals(79.8, BodyParse.parse("79,8 kg, -1,2 kg egy h\u00e9t alatt").kg, 0.01);
        assertEquals(78, BodyParse.parse("78 kg k\u00e9t h\u00f3nap ut\u00e1n").kg, 0.01);
        // A val\u00f3di \u00f6sszehasonl\u00edt\u00e1s tov\u00e1bbra sem m\u00e9r\u00e9s.
        assertEquals(0, BodyParse.parse("70 kg alatt vagyok").kg, 0.01);
        assertEquals(0, BodyParse.parse("80 kg alatt szeretn\u00e9k lenni").kg, 0.01);
    }

    /**
     * A m\u00e9r\u00e9s F\u0150NEVE is kimond\u00e1s, \u00e9s a m\u00e9rt\u00e9kegys\u00e9g nem tolja el az \u00faj \u00e9rt\u00e9ket.
     *
     * A \u201ereggeli m\u00e9r\u00e9s: 80,1 kg" eddig teljesen elveszett: a \u201em\u00e9r\u00e9s" sz\u00f3 miatt
     * a \u201ecsak sz\u00e1mok maradtak" vizsg\u00e1lat megbukott, a m\u00e9r\u00e9s-szavak list\u00e1j\u00e1n
     * meg nem volt ott. A \u201ehask\u00f6rfogat 92-r\u0151l 88 cm-re" pedig a R\u00c9GI \u00e9rt\u00e9ket
     * tartotta meg \u2013 vagyis a fogy\u00e1s napj\u00e1n egy h\u00edz\u00e1st \u00edrt a napl\u00f3ba.
     */
    @Test public void theWordMeasurementCountsAndTheNewValueWins() {
        assertEquals(80.1, BodyParse.parse("reggel m\u00e9r\u00e9s: 80,1 kg, "
                + "izomt\u00f6meg 35,2 kg").kg, 0.01);
        assertEquals(88.0, BodyParse.parse("hask\u00f6rfogat 92-r\u0151l 88 cm-re").cm[0], 0.01);
        // A r\u00e9gi, egys\u00e9g n\u00e9lk\u00fcli alak sem s\u00e9r\u00fcl.
        assertEquals(76.0, BodyParse.parse("80-r\u00f3l 76-ra fogytam").kg, 0.01);
    }

    /**
     * A m\u00e9retlen testr\u00e9sz sz\u00e1ma nem tests\u00faly.
     *
     * A \u201ecombom 58 cm, v\u00e1dli 38" harmincnyolcasa a v\u00e1dli k\u00f6rfogata \u2013 a
     * napl\u00f3ba viszont harmincnyolc kil\u00f3s m\u00e9r\u00e9sk\u00e9nt ker\u00fclt, egy feln\u0151tt
     * s\u00falytrendj\u00e9be. (A v\u00e1dlinak nincs saj\u00e1t mez\u0151je, de att\u00f3l m\u00e9g nem a
     * m\u00e9rleg sz\u00e1ma \u00e1ll mellette.)
     */
    @Test public void anUnmeasuredBodyPartIsNotAWeight() {
        BodyParse.Body b = BodyParse.parse("combom 58 cm, v\u00e1dli 38");
        assertEquals(0.0, b.kg, 0.01);
        assertEquals(58.0, b.cm[3], 0.01);
    }

    /**
     * A mozg\u00e1s \u00e9s az \u00e9tkez\u00e9s tagmondata is m\u00e1s\u00e9.
     *
     * A napi \u00f6sszefoglal\u00f3 egyetlen mondat: \u201etegnap este 3 poh\u00e1r bort ittam,
     * ma reggel 79,8 kg". A m\u00e9r\u00e9sb\u0151l eddig SEMMI nem lett, mert a bor szavai
     * miatt a \u201ecsak sz\u00e1mok maradtak" vizsg\u00e1lat megbukott \u2013 pedig a kilogramm
     * ott \u00e1llt ki\u00edrva. Az alv\u00e1s- \u00e9s pulzus-tagmondatot m\u00e1r eddig is
     * elhagytuk; a mozg\u00e1s \u00e9s az \u00e9tkez\u00e9s ugyanilyen.
     */
    @Test public void theWorkoutAndMealClausesBelongElsewhereToo() {
        assertEquals(79.8, BodyParse.parse("tegnap este 3 poh\u00e1r bort ittam, "
                + "ma reggel 79,8 kg").kg, 0.01);
        assertEquals(78.4, BodyParse.parse("reggel 78,4 kg, 7 \u00f3ra alv\u00e1s, "
                + "54 nyugalmi. D\u00e9lel\u0151tt 45 perc fut\u00e1s 8 km, eb\u00e9dre csirke "
                + "rizzsel.").kg, 0.01);
        // A \u201ereggeli" sz\u00e1nd\u00e9kosan nem tilt\u00f3 sz\u00f3: az m\u00e9g m\u00e9r\u00e9s is lehet.
        assertEquals(80.1, BodyParse.parse("reggeli m\u00e9r\u00e9s: 80,1 kg").kg, 0.01);
    }

    /**
     * A sorozat tagmondata is m\u00e1s\u00e9.
     *
     * A \u201ek\u00f6rfogatok \u00e9s s\u00falyok: der\u00e9k 84, guggol\u00e1s 3x5 100" sz\u00e1za a R\u00daDON van,
     * nem a m\u00e9rlegen \u2013 eddig sz\u00e1z kil\u00f3s m\u00e9r\u00e9s lett bel\u0151le a s\u00falytrendben, a
     * der\u00e9k viszont megmaradt.
     */
    @Test public void aBarbellWeightIsNotABodyWeight() {
        BodyParse.Body b = BodyParse.parse("k\u00f6rfogatok \u00e9s s\u00falyok: der\u00e9k 84, "
                + "guggol\u00e1s 3x5 100");
        assertEquals(0.0, b.kg, 0.01);
        assertEquals(84.0, b.cm[0], 0.01);
    }

    /**
     * A \u201elettem" nem \u201eettem".
     *
     * A tagmondat-sz\u0171r\u0151 contains()-szel keresett, \u00e9s a \u201e78 kg LETTEM, v\u00e9gre
     * 80 al\u00e1 mentem" els\u0151 tagmondat\u00e1ban megtal\u00e1lta az \u201eettem"-et \u2013 a m\u00e9r\u00e9s
     * \u00e9tkez\u00e9s-tagmondatk\u00e9nt esett ki, \u00e9s az eg\u00e9sz mondatb\u00f3l semmi nem lett.
     * A sz\u00f3hat\u00e1r + igek\u00f6t\u0151 szab\u00e1ly a \u201emegettem"-et tov\u00e1bbra is \u00e9tkez\u00e9snek
     * l\u00e1tja.
     */
    @Test public void becomingIsNotEating() {
        assertEquals(78.0, BodyParse.parse("78 kg lettem, v\u00e9gre 80 al\u00e1 "
                + "mentem").kg, 0.01);
        assertEquals(79.8, BodyParse.parse("megettem egy pizz\u00e1t, 79,8 kg "
                + "voltam reggel").kg, 0.01);
        assertEquals(77.7, BodyParse.parse("77,7 kg, eddigi legjobb").kg, 0.01);
        assertEquals(82.0, BodyParse.parse("visszah\u00edztam 82-re").kg, 0.01);
    }

    /**
     * Az el\u00e9rt c\u00e9l m\u00e1r m\u00e9r\u00e9s.
     *
     * Az \u201eel\u00e9rtem a c\u00e9ls\u00falyom, 72 kg" hetvenkett\u0151je a mai s\u00faly \u2013 a c\u00e9l szava
     * eddig az eg\u00e9sz mondatot eln\u00e9m\u00edtotta, pedig aki el\u00e9rte, az \u00e9pp most \u00e1llt
     * a m\u00e9rlegen. A puszta k\u00edv\u00e1ns\u00e1g marad k\u00edv\u00e1ns\u00e1g.
     */
    @Test public void aReachedGoalIsAMeasurement() {
        assertEquals(72.0, BodyParse.parse("el\u00e9rtem a c\u00e9ls\u00falyom, 72 kg").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("a c\u00e9l 75 kg").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("szeretn\u00e9k 72 kg lenni").kg, 0.01);
    }

    /** A d\u00e1tum is csak k\u00eds\u00e9ret: az \u201eaug. 14. reggel 78 kg" jegyzet-sor. */
    @Test public void aDateStampDoesNotHideTheWeight() {
        assertEquals(78.0, BodyParse.parse("aug. 14. reggel 78 kg").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("j\u00falius 28-\u00e1n 78 kg").kg, 0.01);
    }

    /** A \u201e-r\u00f3l" a kiindul\u00f3pont ragja, sosem a mai \u00e9rt\u00e9k. */
    @Test public void theStartingWeightIsNotToday() {
        assertEquals(78.2, BodyParse.parse("83,5 kil\u00f3r\u00f3l indultam janu\u00e1rban, "
                + "ma 78,2").kg, 0.01);
    }

    /**
     * Az \u00e9tel tagmondata sem veszi el a m\u00e9r\u00e9st.
     *
     * A \u201ema reggel: 40 perc fut\u00e1s, zuhany, zabk\u00e1sa fah\u00e9jjal, 79,1 kg"
     * zabk\u00e1s\u00e1ja miatt az eg\u00e9sz m\u00e9r\u00e9s elveszett \u2013 a \u201ecsak sz\u00e1mok maradtak"
     * vizsg\u00e1lat a k\u00e1sa szavain bukott meg. A centivel \u00edrt tagmondat viszont
     * m\u00e9r\u00e9s marad: a \u201e38 cm comb" a comb k\u00f6rfogata, nem csirkecomb.
     */
    @Test public void aFoodClauseDoesNotHideTheWeight() {
        assertEquals(79.1, BodyParse.parse("ma reggel: 40 perc fut\u00e1s, zuhany, "
                + "zabk\u00e1sa fah\u00e9jjal, 79,1 kg").kg, 0.01);
        assertEquals(38.0, BodyParse.parse("78 kg, 18% zs\u00edr, 92 cm der\u00e9k, "
                + "100 cm cs\u00edp\u0151, 38 cm comb, 34 cm kar").cm[3], 0.01);
    }

    /** A f\u00fcrd\u00e9s \u00e9s az esti jelz\u0151 is csak k\u00eds\u00e9ret a m\u00e9r\u00e9s mellett. */
    @Test public void aBathTimeWeighInCounts() {
        assertEquals(79.2, BodyParse.parse("esti f\u00fcrd\u00e9s ut\u00e1n 79,2 kg").kg, 0.01);
    }

    /**
     * A kezel\u00e9s alatti \u201ealatt" nem k\u00fcsz\u00f6b, a jelen idej\u0171 ir\u00e1ny is m\u00e9r\u00e9s.
     *
     * A \u201ehormonkezel\u00e9s alatt h\u00edztam 3 kil\u00f3t, most 68 kg" hatvannyolca
     * val\u00f3di m\u00e9r\u00e9s \u2013 az \u201ealatt" tilt\u00f3szava eddig az eg\u00e9szet elvitte. A
     * \u201ev\u00e9gre fogyok, 74,2 kg ma" pedig se kimond\u00e1snak, se k\u00eds\u00e9r\u0151nek nem
     * sz\u00e1m\u00edtott. A \u201e70 kg alatt vagyok" k\u00fcsz\u00f6be tov\u00e1bbra sem m\u00e9r\u00e9s.
     */
    @Test public void treatmentContextDoesNotBlockTheScale() {
        assertEquals(68.0, BodyParse.parse("hormonkezel\u00e9s alatt h\u00edztam "
                + "3 kil\u00f3t, most 68 kg").kg, 0.01);
        assertEquals(74.2, BodyParse.parse("v\u00e9gre fogyok, 74,2 kg ma").kg,
                0.01);
        assertTrue(BodyParse.parse("70 kg alatt vagyok").isEmpty());
    }

    /**
     * A \u201evoltam \u2026, most" mondatban a m\u00e1sodik sz\u00e1m a mai.
     *
     * A \u201e80 kg voltam 20% zs\u00edrral, most 76 kg 16%-kal" nyolcvana a m\u00falt\u00e9 \u2013
     * m\u00e9gis az ker\u00fclt a trendbe, a h\u00fasz sz\u00e1zal\u00e9kkal egy\u00fctt.
     */
    @Test public void theCurrentValueBeatsThePastOne() {
        BodyParse.Body b = BodyParse.parse("80 kg voltam 20% zs\u00edrral, "
                + "most 76 kg 16%-kal");
        assertEquals(76.0, b.kg, 0.01);
        assertEquals(16.0, b.fatPct, 0.01);
    }

    /** A sz\u00e1zal\u00e9k ut\u00e1ni zs\u00edr test\u00e9rt\u00e9k \u2013 az inbody sora nem \u00e9tel. */
    @Test public void percentFatIsABodyValue() {
        BodyParse.Body b = BodyParse.parse("inbody m\u00e9r\u00e9s: 80,2 kg, "
                + "15,8% zs\u00edr, 38,1 kg izom");
        assertEquals(80.2, b.kg, 0.01);
        assertEquals(15.8, b.fatPct, 0.01);
    }

    /**
     * Az \u00e1tl\u00e9pett k\u00fcsz\u00f6b sz\u00e1ma nem a m\u00e9rleg sz\u00e1ma.
     *
     * A \u201ev\u00e9gre lement a s\u00falyom 80 al\u00e1, 79.6 kg" nyolcvanasa a l\u00e9lektani
     * hat\u00e1r \u2013 m\u00e9gis nyolcvan kil\u00f3 ker\u00fclt a trendbe a 79,6 helyett: a
     * fogy\u00e1s \u00fcnnep\u00e9nek napj\u00e1n egy f\u00e9l kil\u00f3val nagyobb s\u00faly.
     */
    @Test public void aThresholdIsNotTheMeasurement() {
        assertEquals(79.6, BodyParse.parse("v\u00e9gre lement a s\u00falyom 80 al\u00e1, "
                + "79.6 kg").kg, 0.01);
    }

    /**
     * A gyakorlat s\u00falya a r\u00fadon van, nem a m\u00e9rlegen.
     *
     * A \u201ev\u00e1llb\u00f3l nyom\u00e1s ma csak 40 kg ment, f\u00e1radt voltam" negyvenese a
     * s\u00falyz\u00f3s napl\u00f3\u00e9 \u2013 a \u201evoltam" miatt m\u00e9gis negyven kil\u00f3s m\u00e9r\u00e9s lett
     * bel\u0151le a s\u00falytrendben. A centivel \u00edrt v\u00e1dli viszont k\u00f6rfogat marad,
     * hi\u00e1ba gyakorlatn\u00e9v is a v\u00e1dli.
     */
    @Test public void anExerciseWeightIsNotTheScale() {
        assertEquals(0.0, BodyParse.parse("v\u00e1llb\u00f3l nyom\u00e1s ma csak 40 kg "
                + "ment, f\u00e1radt voltam").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("evez\u00e9s volt reggel, este 78 kg "
                + "a m\u00e9rlegen").kg, 0.01);
    }

    /** A \u201epont" \u00e9s a \u201ekg-n\u00e1l tartok" is csak k\u00eds\u00e9ret a m\u00e9r\u00e9s mellett. */
    @Test public void exactAndStandingAtPhrasesAreCompany() {
        assertEquals(68.0, BodyParse.parse("reggel \u00e9hgyomorra "
                + "68 kg pont").kg, 0.01);
        assertEquals(82.0, BodyParse.parse("82 kg-n\u00e1l tartok").kg, 0.01);
    }
    /**
     * A m\u00faltra utalt \u201evolt" a r\u00e9gi \u00e9rt\u00e9k, nem a mai m\u00e9r\u00e9s.
     *
     * A \u201eder\u00e9kb\u0151s\u00e9g 92 cm, k\u00e9t hete m\u00e9g 95 volt" kilencven\u00f6te a k\u00e9t h\u00e9ttel
     * ezel\u0151tti der\u00e9k \u2013 m\u00e9gis tests\u00falyk\u00e9nt ker\u00fclt a napl\u00f3ba. A m\u00falt-
     * id\u0151hat\u00e1roz\u00f3 melletti \u201eN volt" kiesik; a t\u00e1voli sz\u00e1m marad, mert a
     * \u201e3 hete edzek, ma 78 kg volt" hetvennyolcasa val\u00f3di mai m\u00e9r\u00e9s.
     */
    @Test public void aValueFromWeeksAgoIsNotTodaysWeight() {
        BodyParse.Body b = BodyParse.parse("der\u00e9kb\u0151s\u00e9g 92 cm, "
                + "k\u00e9t hete m\u00e9g 95 volt");
        assertEquals(0.0, b.kg, 0.01);
        assertTrue(b.hasCm());
        assertEquals(78.4, BodyParse.parse("78,4 kg ma reggel, "
                + "egy h\u00f3napja 81 volt").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("3 hete edzek rendszeresen, "
                + "ma 78 kg volt a s\u00falyom").kg, 0.01);
    }
    /**
     * A t\u00f6bbes jelz\u0151s sz\u00e1m nem k\u00f6rfogat.
     *
     * A \u201ebicepsz 21-esek 3 k\u00f6r" a huszonegyes ism\u00e9tl\u00e9s-s\u00e9ma neve \u2013 m\u00e9gis
     * huszonegy centis kar ker\u00fclt a napl\u00f3ba. A val\u00f3di m\u00e9r\u00e9s marad.
     */
    @Test public void theTwentyOnesSchemeIsNotAnArmSize() {
        assertFalse(BodyParse.parse("bicepsz 21-esek 3 k\u00f6r").hasCm());
        BodyParse.Body b = BodyParse.parse("bicepszem 38 cm lett");
        assertTrue(b.hasCm());
    }
    /**
     * A v\u00e1rand\u00f3ss\u00e1g hete nem tests\u00faly.
     *
     * Az \u201ea 30. h\u00e9tben vagyok, hetente 2x \u00fasz\u00e1s" harmincasa HARMINC KIL\u00d3
     * lett a napl\u00f3ban. A val\u00f3di heti m\u00e9r\u00e9s marad.
     */
    @Test public void aPregnancyWeekIsNotAWeight() {
        assertEquals(0.0, BodyParse.parse("a 30. h\u00e9tben vagyok, hetente "
                + "2x \u00fasz\u00e1s").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("78 kg voltam a h\u00e9ten").kg, 0.01);
    }
    /**
     * A tejterm\u00e9k sz\u00e1zal\u00e9ka zs\u00edrtartalom, nem testzs\u00edr.
     *
     * A \u201ekefir Danone, 3%" h\u00e1rmasa testzs\u00edr-m\u00e9r\u00e9sk\u00e9nt ker\u00fclt a napl\u00f3ba.
     * A kimondott zs\u00edr-sz\u00f3 melletti sz\u00e1zal\u00e9k m\u00e9r\u00e9s marad \u2013 tejterm\u00e9kkel
     * egy mondatban is.
     */
    @Test public void aDairyPercentageIsNotBodyFat() {
        assertEquals(0.0, BodyParse.parse("kefir Danone, 3%").fatPct, 0.01);
        assertEquals(0.0, BodyParse.parse("tejf\u00f6l 20%-os a lecs\u00f3ba")
                .fatPct, 0.01);
        assertEquals(18.0, BodyParse.parse("18% testzs\u00edr, reggel t\u00far\u00f3 "
                + "volt").fatPct, 0.01);
    }
    /** A s\u00faly-k\u00fcsz\u00f6b \u00e1tl\u00e9p\u00e9se is csak k\u00eds\u00e9ret: a 77,8 a m\u00e9r\u00e9s. */
    @Test public void goingUnderAWeightThresholdKeepsTheReading() {
        assertEquals(77.8, BodyParse.parse("78 kg al\u00e1 mentem v\u00e9gre, "
                + "77,8!").kg, 0.01);
    }
    @Test public void theReversedBodyFatIsStillBodyFat() {
        // Az \u201eaz okosm\u00e9rleg szerint 22,1 a testzs\u00edrom" eddig \u00fcresen j\u00f6tt
        // vissza, mert a sz\u00e1m a sz\u00f3 el\u0151tt \u00e1ll.
        BodyParse.Body b = BodyParse.parse("az okosm\u00e9rleg szerint 22,1 a testzs\u00edrom");
        assertEquals(22.1, b.fatPct, 0.001);
        b = BodyParse.parse("19 a testzs\u00edr sz\u00e1zal\u00e9kom");
        assertEquals(19, b.fatPct, 0.001);
    }

    @Test public void theShownWeightIsAMeasurement() {
        // A \u201ev\u00edzvisszatart\u00e1s miatt 84 kg-ot mutatott" nyolcvann\u00e9gye a
        // m\u00e9rleg reggeli sz\u00e1ma \u2013 eddig ital lett bel\u0151le, m\u00e9r\u00e9s nem.
        BodyParse.Body b = BodyParse.parse("v\u00edzvisszatart\u00e1s miatt 84 kg-ot mutatott");
        assertEquals(84, b.kg, 0.001);
    }

    @Test public void bodyWeightInPoundsConverts() {
        // A \u201es\u00falyom 180 font" nyolcvank\u00e9t kil\u00f3, nem sz\u00e1znyolcvan \u2013 a
        // p\u00e9nzbeli font (\u201efontba ker\u00fclt") viszont nem tests\u00faly.
        assertEquals(81.6, BodyParse.parse("s\u00falyom 180 font").kg, 0.1);
        assertEquals(0, BodyParse.parse("a cip\u0151 120 fontba ker\u00fclt").kg, 0.001);
    }

    @Test public void weightJourneySentencesFindTodaysNumber() {
        // A \u201estagn\u00e1l a s\u00falyom 82 k\u00f6r\u00fcl", az \u201e\u00e1tl\u00e9ptem a 80-as hat\u00e1rt
        // lefel\u00e9, 79,8" \u00e9s a \u201e83-r\u00f3l indultam janu\u00e1rban, ma 76" mind mai
        // m\u00e9r\u00e9st mond \u2013 eddig egyik sem ker\u00fclt be.
        assertEquals(82, BodyParse.parse("stagn\u00e1l a s\u00falyom 82 k\u00f6r\u00fcl").kg, 0.001);
        assertEquals(79.8, BodyParse
                .parse("v\u00e9gre \u00e1tl\u00e9ptem a 80-as hat\u00e1rt lefel\u00e9, 79,8").kg, 0.001);
        assertEquals(76, BodyParse
                .parse("83-r\u00f3l indultam janu\u00e1rban, ma 76").kg, 0.001);
        // A versenyen elfoglalt rajthely viszont nem tests\u00faly.
        assertEquals(0, BodyParse
                .parse("a 10. helyr\u0151l indultam a versenyen").kg, 0.001);
    }

    @Test public void aFamilyMembersWeightIsNotMine() {
        // \u201eA fiam 32 kg lett a m\u00e9rlegen" a gyerek s\u00falya \u2013 eddig a
        // felhaszn\u00e1l\u00f3 m\u00e9r\u00e9s\u00e9nek sz\u00e1m\u00edtott. A saj\u00e1t tagmondat megmarad.
        assertEquals(0, BodyParse.parse("a fiam 32 kg lett a m\u00e9rlegen").kg, 0.001);
        assertEquals(78, BodyParse
                .parse("\u00e9n 78 kg vagyok, a fiam 32 kg").kg, 0.001);
        assertEquals(78, BodyParse
                .parse("a p\u00e1rom szerint fogytam, 78 kg vagyok").kg, 0.001);
        assertEquals(0, BodyParse.parse("a kuty\u00e1m 28 kg-ot nyom").kg, 0.001);
    }

    @Test public void beingUnderAThresholdKeepsTheRealReading() {
        assertEquals(74.6, BodyParse.parse("v\u00e9gre 75 alatt vagyok, 74,6").kg, 0.01);
    }

    @Test public void aWeightMeasuredOnMeIsMine() {
        assertEquals(84.0, BodyParse.parse("az orvosn\u00e1l 84 kg-ot m\u00e9rtek rajtam").kg, 0.01);
    }

    @Test public void anAccusativeMeasurementCountsWithoutAUnit() {
        assertEquals(84.0, BodyParse.parse("\u00e9n 84-et m\u00e9rtem ma").kg, 0.01);
    }

    @Test public void someoneElsesBenchPressIsNotMyWeight() {
        BodyParse.Body b = BodyParse.parse(
                "a sr\u00e1c 90 kg-ot nyomott ki a teremben");
        assertTrue(b == null || b.kg == 0);
    }

    @Test public void megsemKeepsTheCorrectedWeight() {
        assertEquals(78.0, BodyParse.parse("m\u00e9gsem 80 kg vagyok, hanem 78").kg, 0.01);
    }

    @Test public void aMorningRoutineHeaderIsAWeighInContext() {
        assertEquals(78.2, BodyParse.parse("reggeli rutin: 78,2 kg, pulzus 54").kg, 0.01);
    }

    @Test public void theCompactRhrLineCarriesTheWeight() {
        assertEquals(78.2, BodyParse.parse("78,2 kg / 54 rhr / 7,5h alv\u00e1s").kg, 0.01);
    }

    @Test public void bodyWaterPercentIsNotBodyFat() {
        BodyParse.Body b = BodyParse.parse("m\u00e9rleg 78,8 / testzs\u00edr 18,2 / v\u00edz 55%");
        assertEquals(18.2, b.fatPct, 0.01);
    }

    @Test public void aBarbellWeightInARoutineIsNotBodyWeight() {
        BodyParse.Body b = BodyParse.parse("edz\u00e9s rutin: fekvenyom\u00e1s 3x10 60 kg");
        assertTrue(b == null || b.kg == 0);
    }

    @Test public void plateauIdiomsAreMeasurements() {
        assertEquals(78.0, BodyParse.parse("78-on \u00e1llok").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("be\u00e1lltam 78-ra").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("tartom a 78-at").kg, 0.01);
    }

    @Test public void standingInLineIsNotAWeight() {
        BodyParse.Body b = BodyParse.parse("sorban \u00e1llok a boltban");
        assertTrue(b == null || b.kg == 0);
    }

    @Test public void aWorkRestPairIsNotAWeight() {
        BodyParse.Body b = BodyParse.parse("Reggeli rutin: 4 k\u00f6r 45/15");
        assertTrue(b == null || b.kg == 0);
        assertEquals(82.0, BodyParse.parse("v\u00e9rnyom\u00e1s 160/95, s\u00falyom 82").kg, 0.01);
    }

    @Test public void aWaistDropKeepsTheNewCentimetres() {
        BodyParse.Body b = BodyParse.parse("der\u00e9kb\u0151s\u00e9g lement 90-r\u0151l 86-ra");
        assertEquals(86.0, b.cm[0], 0.01);
    }

    @Test public void aWeightDropKeepsTheNewKilos() {
        assertEquals(84.0, BodyParse.parse("lefogytam 90-r\u0151l 84-re").kg, 0.01);
    }

    @Test public void englishBodyFatShorthandReads() {
        assertEquals(18.0, BodyParse.parse("bf 18%").fatPct, 0.01);
        assertEquals(18.5, BodyParse.parse("body fat 18,5%").fatPct, 0.01);
        assertEquals(17.2, BodyParse.parse("testzs\u00edr% 17,2").fatPct, 0.01);
    }

    @Test public void aBareFatPercentIsBodyFat() {
        assertEquals(18.0, BodyParse.parse("zs\u00edr 18%").fatPct, 0.01);
    }

    @Test public void aMultilineMorningLogKeepsTheWeight() {
        assertEquals(78.2, BodyParse.parse("78,2 kg\n52 nyugalmi pulzus").kg, 0.01);
    }

    @Test public void theLaterOfTwoWeighInsIsTodaysNumber() {
        assertEquals(79.2, BodyParse.parse("ma reggel m\u00e9g 79,8 volt, este m\u00e1r 79,2").kg, 0.01);
        assertEquals(79.4, BodyParse.parse("h\u00e9tf\u0151n 80,5, ma 79,4 - megy lefel\u00e9").kg, 0.01);
    }

    @Test public void aThresholdNextToARealReadingFallsAway() {
        assertEquals(80.3, BodyParse.parse("a m\u00e9rleg megint 80 f\u00f6l\u00f6tt, 80,3").kg, 0.01);
        // M\u00e9r\u00e9s n\u00e9lk\u00fcl a k\u00fcsz\u00f6b marad tilt\u00f3: a c\u00e9l nem m\u00e9r\u00e9s.
        BodyParse.Body b = BodyParse.parse("70 kg alatt szeretn\u00e9k lenni");
        assertTrue(b == null || b.kg == 0);
    }

    /**
     * A GRAMMBAN mondott sz\u00e1m sosem tests\u00faly: az \u201eel\u00e9rtem a feh\u00e9rjec\u00e9lt,
     * 140 g" sz\u00e1znegyvene feh\u00e9rje \u2013 eddig sz\u00e1znegyven KIL\u00d3S m\u00e9r\u00e9s lett.
     */
    @Test
    public void aGramFigureIsNeverABodyWeight() {
        assertTrue(BodyParse.parse("el\u00e9rtem a feh\u00e9rjec\u00e9lt, 140 g").isEmpty());
        // A kil\u00f3s m\u00e9r\u00e9s marad, a \u201ekg" g-je nem esik ide.
        assertFalse(BodyParse.parse("78,4 kg reggel").isEmpty());
        assertFalse(BodyParse.parse("megettem 140 g csirk\u00e9t, s\u00falyom 78,2 kg").isEmpty());
    }

    /**
     * A HELYESB\u00cdT\u00c9S tagadott sz\u00e1ma nem n\u00e9m\u00edtja el a m\u00e9r\u00e9st: a \u201ebocs,
     * el\u00edrtam: 78,2 kg volt, nem 87,2" hetvennyolc kil\u00f3ja az igazi.
     */
    @Test
    public void aTypoFixKeepsTheRealWeight() {
        assertEquals(78.2, BodyParse.parse(
                "bocs, el\u00edrtam: 78,2 kg volt, nem 87,2").kg, 0.01);
        assertEquals(78.2, BodyParse.parse("78,2 kg volt, nem 87,2 kg").kg, 0.01);
        // A \u201ehanem"-es alak m\u00e1sodik sz\u00e1ma tov\u00e1bbra is az igazi.
        assertEquals(78, BodyParse.parse("nem 80 kg vagyok, hanem 78").kg, 0.01);
    }

    /**
     * A TEGNAPI sz\u00e1m a tegnapi, az \u00e1rva k\u00f6t\u0151sz\u00f3 meg nem adat: a \u201ema
     * reggel 82,3 kg, de tegnap 82,9 volt" k\u00e9t sz\u00e1ma k\u00f6z\u00fcl EGYIK sem
     * ker\u00fclt a napl\u00f3ba \u2013 a mai m\u00e9r\u00e9s is elveszett a tegnapi mellett.
     */
    @Test
    public void todaysNumberWinsOverYesterdays() {
        assertEquals(82.3, BodyParse.parse(
                "ma reggel 82,3 kg de tegnap 82,9 volt").kg, 0.001);
        assertEquals(82.3, BodyParse.parse(
                "ma reggel 82,3 kg tegnap 82,9 kg").kg, 0.001);
        // A puszta k\u00f6t\u0151sz\u00f3 sem n\u00e9m\u00edtja el a m\u00e9r\u00e9st.
        assertEquals(82.3, BodyParse.parse("ma reggel 82,3 kg de 82,9 volt").kg, 0.001);
        // A MAG\u00c1BAN \u00e1ll\u00f3 tegnapi m\u00e9r\u00e9s marad, ami volt.
        assertEquals(82.9, BodyParse.parse("tegnap 82,9 kg volt").kg, 0.001);
    }

    /**
     * A V\u00c1GY a saj\u00e1t tagmondata, vessz\u0151 n\u00e9lk\u00fcl is: a \u201e60 kg vagyok
     * szeretn\u00e9k 65-\u00f6t" mondatot az eg\u00e9sz mondatra kiterjed\u0151 tilt\u00e1s
     * eln\u00e9m\u00edtotta, \u00e9s a MAI m\u00e9r\u00e9s is elveszett a v\u00e1ggyal egy\u00fctt.
     */
    @Test
    public void theWishDoesNotSwallowTheMeasurement() {
        assertEquals(60.0, BodyParse.parse("60 kg vagyok szeretn\u00e9k 65-\u00f6t").kg, 0.001);
        // A puszta v\u00e1gy tov\u00e1bbra sem m\u00e9r\u00e9s.
        assertEquals(0.0, BodyParse.parse("szeretn\u00e9k 75 kg lenni").kg, 0.001);
        assertEquals(0.0, BodyParse.parse("a c\u00e9l 75 kg").kg, 0.001);
    }

    /**
     * A gyakorlat IG\u00c9JE nem m\u00e9r\u00e9s: a \u201eleguggoltam 100-at s\u00faly n\u00e9lk\u00fcl"
     * mondatban a \u201es\u00faly" sz\u00f3 m\u00e9r\u00e9snek mutatta a sz\u00e1zat, \u00e9s SZ\u00c1Z KIL\u00d3S
     * tests\u00faly ker\u00fclt a trendbe \u2013 abb\u00f3l, hogy valaki sz\u00e1z guggol\u00e1st
     * csin\u00e1lt teher n\u00e9lk\u00fcl.
     */
    @Test
    public void theVerbOfALiftIsNotAWeighIn() {
        assertEquals(0.0, BodyParse.parse("leguggoltam 100-at s\u00faly n\u00e9lk\u00fcl").kg, 0.001);
        assertEquals(0.0, BodyParse.parse("kinyomtam 100-at").kg, 0.001);
        // A val\u00f3di m\u00e9r\u00e9s v\u00e1ltozatlan.
        assertEquals(78.4, BodyParse.parse("ma reggel 78,4 kg voltam").kg, 0.001);
    }

    /**
     * A SZ\u00c1M N\u00c9LK\u00dcLI tagmondat nem ronthatja el a m\u00e9r\u00e9st: a \u201eMa
     * pihen\u0151nap volt. 78,9 kg reggel." els\u0151 mondat\u00e1ban egyetlen olyan
     * sz\u00f3 van (\u201epihen\u0151nap"), amit a \u201ecsak sz\u00e1mok maradtak" vizsg\u00e1lat nem
     * ismer \u2013 \u00e9s emiatt az EG\u00c9SZ bejegyz\u00e9sb\u0151l semmi nem lett, a reggeli
     * m\u00e9rleg sz\u00e1ma is elveszett.
     */
    @Test
    public void aClauseWithoutNumbersCannotSpoilTheMeasurement() {
        assertEquals(78.9, BodyParse.parse("Ma pihen\u0151nap volt. 78,9 kg reggel.").kg, 0.01);
        assertEquals(78.9, BodyParse.parse(
                "Ma pihen\u0151nap volt, csak 6200 l\u00e9p\u00e9st tettem meg. "
                + "Ettem egy joghurtot \u00e9s k\u00e9t szelet kenyeret sonk\u00e1val. "
                + "78,9 kg reggel.").kg, 0.01);
        // A K\u00d6R\u00dcLM\u00c9NY szava viszont sz\u00e1m n\u00e9lk\u00fcl is magyar\u00e1z: a l\u00e1z marad l\u00e1z.
        assertEquals(0.0, BodyParse.parse("h\u0151emelked\u00e9sem van, 37,8").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("beteg vagyok, l\u00e1zam van 38,5").kg, 0.01);
    }

    /**
     * A PERJELES H\u00c1RMAS sem m\u00e9r\u00e9s: a \u201e180/220/70" makr\u00f3-sor utols\u00f3
     * tagj\u00e1b\u00f3l hetven kil\u00f3s m\u00e9r\u00e9s lett a s\u00falytrendben.
     */
    @Test
    public void aSlashTripleIsNotAWeighIn() {
        assertEquals(0.0, BodyParse.parse("180/220/70").kg, 0.001);
        assertEquals(0.0, BodyParse.parse("makr\u00f3k: 180/220/70").kg, 0.001);
        // A val\u00f3di m\u00e9r\u00e9s v\u00e1ltozatlan.
        assertEquals(70.0, BodyParse.parse("70 kg vagyok").kg, 0.001);
    }


    /**
     * A \u201ekg-OS" jelz\u0151s alak sosem tests\u00faly: az \u201eel\u00e9rtem a 100 kg-os
     * fekvenyom\u00e1st!" sz\u00e1z kil\u00f3s TESTS\u00daLYT \u00edrt a trendbe \u2013 abb\u00f3l, ami a
     * r\u00fadon volt.
     */
    @Test
    public void anAdjectiveKiloIsNotBodyWeight() {
        assertEquals(0.0, BodyParse.parse("El\u00e9rtem a 100 kg-os fekvenyom\u00e1st!").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("Vettem egy 20 kg-os s\u00falyz\u00f3t.").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("5 kil\u00f3s k\u00e9zis\u00falyz\u00f3val edzettem.").kg, 0.01);
        // A val\u00f3di m\u00e9r\u00e9s marad.
        assertEquals(80.0, BodyParse.parse("Ma 80 kg vagyok.").kg, 0.01);
        assertEquals(80.4, BodyParse.parse("80,4 kg reggel.").kg, 0.01);
    }


    /**
     * A SZ\u00dcLET\u00c9SNAP sz\u00e1mai \u00c9VEK: a \u201ema volt a sz\u00fclet\u00e9snapom, 42 lettem, \u00e9s
     * 42 fekv\u0151t\u00e1maszt csin\u00e1ltam" negyvenk\u00e9t KIL\u00d3S m\u00e9r\u00e9st \u00edrt a trendbe \u2013 az
     * \u00e9letkorb\u00f3l.
     */
    @Test
    public void anAgeOnABirthdayIsNotAWeight() {
        assertEquals(0.0, BodyParse.parse("Ma volt a sz\u00fclet\u00e9snapom, 42 lettem, "
                + "\u00e9s 42 fekv\u0151t\u00e1maszt csin\u00e1ltam.").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("Bet\u00f6lt\u00f6ttem a 42-t.").kg, 0.01);
        // Kimondott kg mellett a sz\u00fclinapi m\u00e9rleg is m\u00e9rleg.
        assertEquals(78.0, BodyParse.parse("Sz\u00fclinapomon 78 kg voltam.").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("Ma 78 kg vagyok.").kg, 0.01);
    }


    /**
     * A T\u00d6M\u00d6R napl\u00f3-sor r\u00f6vid\u00edt\u00e9sei nem kil\u00f3k: a \u201efut\u00e1s 10k 52p; kondi 40p;
     * alv\u00e1s 7h; s\u00faly 79,3" sorb\u00f3l NEGYVEN kil\u00f3s m\u00e9r\u00e9s lett \u2013 a kondi
     * perceib\u0151l \u2013, a val\u00f3di hetvenkilenc eg\u00e9sz h\u00e1rom tized meg elveszett.
     */
    @Test
    public void shorthandUnitsAreNotKilos() {
        assertEquals(79.3, BodyParse.parse("fut\u00e1s 10k 52p; kondi 40p; "
                + "alv\u00e1s 7h; s\u00faly 79,3").kg, 0.01);
        assertEquals(79.3, BodyParse.parse("kondi 40p; s\u00faly 79,3").kg, 0.01);
        // A val\u00f3di m\u00e9r\u00e9s marad.
        assertEquals(78.5, BodyParse.parse("78,5 kg reggel, 22% zs\u00edr.").kg, 0.01);
        assertEquals(22.0, BodyParse.parse("78,5 kg reggel, 22% zs\u00edr.").fatPct, 0.01);
    }


    /**
     * Az ESZK\u00d6ZHAT\u00c1ROZ\u00d3S kil\u00f3 a V\u00c1LTOZ\u00c1S m\u00e9rt\u00e9ke, nem m\u00e9r\u00e9s: a \u201ema reggel
     * 79,2 kg, ez 0,4 kg-mal kevesebb, mint tegnap" hetvenkilenc eg\u00e9sz k\u00e9t
     * tizede n\u00e9m\u00e1n elveszett \u2013 a k\u00fcl\u00f6nbs\u00e9g sz\u00e1ma mellett a mondat eg\u00e9sze
     * kiesett.
     */
    @Test
    public void aDeltaInKilosIsNotAMeasurement() {
        assertEquals(79.2, BodyParse.parse("Ma reggel 79,2 kg, ez 0,4 kg-mal "
                + "kevesebb, mint tegnap.").kg, 0.01);
        assertEquals(79.2, BodyParse.parse("Ma reggel 79,2 kg, 0,4 kg-mal "
                + "kevesebb.").kg, 0.01);
        // A val\u00f3di m\u00e9r\u00e9s marad.
        assertEquals(80.0, BodyParse.parse("Ma 80 kg vagyok.").kg, 0.01);
    }

    /**
     * A ragozott gyakorlatn\u00e9v sem m\u00e9r\u00e9s.
     *
     * A tilt\u00f3lista sz\u00f3hat\u00e1rt v\u00e1r, ez\u00e9rt a ragos alak \u00e1tcs\u00faszott rajta: az
     * \u201eel\u00e9rtem az \u00faj szem\u00e9lyes cs\u00facsomat guggol\u00e1sban: 120 kg" SZ\u00c1ZH\u00daSZ
     * KIL\u00d3S tests\u00falyt \u00edrt a trendbe \u2013 a r\u00fadon l\u00e9v\u0151 s\u00falyt. A vessz\u0151 ut\u00e1ni
     * csupasz sz\u00e1m ugyanennek a tagmondatnak a folytat\u00e1sa.
     */
    @Test public void anInflectedLiftNameIsNotAMeasurement() {
        assertEquals(0.0, BodyParse.parse("Ma el\u00e9rtem az \u00faj szem\u00e9lyes "
                + "cs\u00facsomat guggol\u00e1sban: 120 kg.").kg, 0.01);
        assertEquals(82.0, BodyParse.parse("\u00daj PR fekvenyom\u00e1sban, 100 kg, "
                + "\u00e9s k\u00f6zben 82 kg vagyok.").kg, 0.01);
        // A val\u00f3di m\u00e9r\u00e9s marad, a k\u00f6rfogat is.
        assertEquals(78.0, BodyParse.parse("aludtam 8 \u00f3r\u00e1t, 78 kg").kg, 0.01);
        assertEquals(40.0, BodyParse.parse("bicepszem 40 cm").cm[4], 0.01);
    }

    /**
     * A kil\u00f3ban mondott k\u00fcl\u00f6nbs\u00e9g tests\u00faly-kontextus.
     *
     * A \u201ereggel 76,8 kg, ez m\u00e1r 5 kil\u00f3 m\u00ednusz az indul\u00e1shoz k\u00e9pest"
     * m\u00e9r\u00e9s\u00e9b\u0151l semmi nem lett: tests\u00faly-sz\u00f3 nincs a mondatban, a \u201ecsak
     * sz\u00e1mok maradtak" vizsg\u00e1lat pedig a m\u00ednusz szavain bukott meg. Aki
     * kil\u00f3ban mond k\u00fcl\u00f6nbs\u00e9get, az a s\u00faly\u00e1r\u00f3l besz\u00e9l.
     */
    @Test public void aDifferenceInKilosIsBodyContext() {
        assertEquals(76.8, BodyParse.parse("Reggel 76,8 kg, ez m\u00e1r 5 kil\u00f3 "
                + "m\u00ednusz az indul\u00e1shoz k\u00e9pest.").kg, 0.01);
        assertEquals(76.8, BodyParse.parse("Reggel 76,8 kg, ez 5 kil\u00f3val "
                + "kevesebb az indul\u00e1sn\u00e1l.").kg, 0.01);
        // A k\u00fcl\u00f6nbs\u00e9g sz\u00e1ma \u00f6nmag\u00e1ban nem m\u00e9r\u00e9s, \u00e9s a m\u00e1s\u00e9 sem az.
        assertEquals(0.0, BodyParse.parse("5 kil\u00f3 m\u00ednusz!").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("A tes\u00f3m 10 kil\u00f3val nehezebb "
                + "n\u00e1lam.").kg, 0.01);
    }

    /**
     * A c\u00e9l tagmondata nem viszi el a reggeli m\u00e9r\u00e9st.
     *
     * A \u201ereggel 79,1 kg. C\u00e9l: 75 al\u00e1 szeptember v\u00e9g\u00e9ig" m\u00e9r\u00e9s\u00e9b\u0151l semmi
     * nem lett: a mondathat\u00e1r a feldolgoz\u00e1sban sz\u00f3k\u00f6zz\u00e9 olvadt, \u00e9s a c\u00e9l
     * tilt\u00f3szava az eg\u00e9sz sz\u00f6vegre sz\u00f3lt.
     */
    @Test public void aGoalClauseDoesNotEraseTheWeighIn() {
        assertEquals(79.1, BodyParse.parse("Reggel 79,1 kg. C\u00e9l: 75 al\u00e1 "
                + "szeptember v\u00e9g\u00e9ig.").kg, 0.01);
        assertEquals(79.1, BodyParse.parse("Reggel 79,1 kg, c\u00e9l a 75.")
                .kg, 0.01);
        // A puszta c\u00e9l tov\u00e1bbra sem m\u00e9r\u00e9s.
        assertEquals(0.0, BodyParse.parse("A c\u00e9l 75 kg.").kg, 0.01);
    }

    /**
     * A m\u00e9rlegel\u00e9s is m\u00e9r\u00e9s.
     *
     * A \u201ereggeli m\u00e9rlegel\u00e9s: 88,8 kg. J\u00f3l \u00e1llok a heti tervhez k\u00e9pest"
     * bejegyz\u00e9sb\u0151l semmi nem lett \u2013 a \u201em\u00e9rleg" t\u0151 sz\u00f3hat\u00e1rt v\u00e1rt, a
     * ragozott alak \u00e1tcs\u00faszott rajta.
     */
    @Test public void weighingInIsAMeasurement() {
        assertEquals(88.8, BodyParse.parse("Reggeli m\u00e9rlegel\u00e9s: 88,8 kg. "
                + "J\u00f3l \u00e1llok a heti tervhez k\u00e9pest.").kg, 0.01);
        assertEquals(78.2, BodyParse.parse("M\u00e9rlegeltem: 78,2 kg.").kg, 0.01);
    }

    /**
     * A v\u00e9rcukor nem tests\u00faly \u00e9s nem kan\u00e1l cukor.
     *
     * A \u201e14:20-kor 132-es v\u00e9rcukrot m\u00e9rtem" sz\u00e1zharminck\u00e9t KIL\u00d3S m\u00e9r\u00e9sk\u00e9nt
     * ker\u00fclt a s\u00falytrendbe, az \u00e9trendbe pedig t\u00edz gramm cukor ment.
     */
    @Test public void bloodSugarIsNeitherWeightNorSugar() {
        assertEquals(0.0, BodyParse.parse("Ma d\u00e9lut\u00e1n 14:20-kor 132-es "
                + "v\u00e9rcukrot m\u00e9rtem, edz\u00e9s el\u0151tt.").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("V\u00e9rcukor: 98, rendben.").kg, 0.01);
    }

    /**
     * A kil\u00f3 \u00e9s a gramm egy\u00fctt egyetlen m\u00e9r\u00e9s.
     *
     * A \u201em\u00e9rleg ma reggel 84 kil\u00f3t \u00e9s 300 grammot mutatott" nyolcvann\u00e9gy
     * eg\u00e9szk\u00e9nt ment be \u2013 a h\u00e1romsz\u00e1z gramm elveszett, pedig aki \u00edgy
     * mondja, annak pont az sz\u00e1m\u00edt.
     */
    @Test public void kilosAndGramsAddUp() {
        assertEquals(84.3, BodyParse.parse("A m\u00e9rleg ma reggel 84 kil\u00f3t "
                + "\u00e9s 300 grammot mutatott.").kg, 0.01);
        assertEquals(84.3, BodyParse.parse("84 kil\u00f3 300 gramm vagyok.")
                .kg, 0.01);
    }

    /**
     * A k\u00fcsz\u00f6b-tagmondat nem viszi el a m\u00e9r\u00e9st.
     *
     * A \u201ema reggeli s\u00faly: 90,05 kg, els\u0151 alkalommal 90 f\u00f6l\u00f6tt :(" m\u00e9r\u00e9se
     * elveszett: a mondathat\u00e1r a feldolgoz\u00e1sban sz\u00f3k\u00f6zz\u00e9 olvadt, \u00e9s a
     * \u201ef\u00f6l\u00f6tt" tilt\u00f3szava az eg\u00e9sz sz\u00f6vegre sz\u00f3lt.
     */
    @Test public void aThresholdRemarkKeepsTheWeighIn() {
        assertEquals(90.05, BodyParse.parse("Ma reggeli s\u00faly: 90,05 kg, "
                + "els\u0151 alkalommal 90 f\u00f6l\u00f6tt. :(").kg, 0.01);
        // A v\u00e1gy-k\u00fcsz\u00f6b tov\u00e1bbra sem m\u00e9r\u00e9s.
        assertEquals(0.0, BodyParse.parse("70 kg alatt szeretn\u00e9k lenni.")
                .kg, 0.01);
    }

    /**
     * A zs\u00edrm\u00e9r\u0151 sz\u00e1zal\u00e9ka testzs\u00edr, a s\u00faly mellett is.
     *
     * Az \u201ea zs\u00edrm\u00e9r\u0151 szerint 24,8 sz\u00e1zal\u00e9k" elveszett (a zs\u00edr t\u00f6ve a
     * m\u0171szer nev\u00e9ben \u00fclt), a \u201es\u00falyom 80, zs\u00edr 18%" sz\u00e1zal\u00e9ka pedig a
     * m\u00e1s-napl\u00f3 sz\u0171r\u0151n bukott el, miel\u0151tt a testzs\u00edr-szab\u00e1ly l\u00e1thatta
     * volna.
     */
    @Test public void aFatMeterPercentSurvivesBesideTheWeight() {
        BodyParse.Body b = BodyParse.parse("A m\u00e9rlegen 91,2, a "
                + "zs\u00edrm\u00e9r\u0151 szerint 24,8 sz\u00e1zal\u00e9k.");
        assertEquals(91.2, b.kg, 0.01);
        assertEquals(24.8, b.fatPct, 0.01);
        BodyParse.Body c = BodyParse.parse("S\u00falyom 80, zs\u00edr 18%.");
        assertEquals(80.0, c.kg, 0.01);
        assertEquals(18.0, c.fatPct, 0.01);
    }

    /**
     * A kett\u0151spontos sz\u00e1m \u00f3ra, sosem tests\u00faly.
     *
     * A \u201e6:30-ra m\u00e1r az emelked\u0151n\u00e9l voltunk" HARMINC kil\u00f3s
     * m\u00e9r\u00e9sk\u00e9nt ker\u00fclt a trendbe \u2013 a perc fele lev\u00e1lt.
     */
    @Test public void aClockIsNeverAWeight() {
        assertEquals(0.0, BodyParse.parse("6:30-ra m\u00e1r az els\u0151 "
                + "emelked\u0151n\u00e9l voltunk a bring\u00e1val.").kg, 0.01);
    }

    /**
     * A k\u00fara mellett a \u201emost N" is m\u00e9r\u00e9s.
     *
     * Az \u201ea fogy\u00f3k\u00fara els\u0151 hete lez\u00e1rult: -1,8 kg, most 92,7"
     * kilencvenkett\u0151 eg\u00e9sz h\u00e9ttizede elveszett \u2013 a k\u00fara szava mellett
     * a m\u00e9rt\u00e9kegys\u00e9g n\u00e9lk\u00fcli sz\u00e1m nem volt m\u00e9r\u00e9s.
     */
    @Test public void aDietWeekCloseKeepsTheCurrentWeight() {
        assertEquals(92.7, BodyParse.parse("A fogy\u00f3k\u00fara els\u0151 hete "
                + "lez\u00e1rult: -1,8 kg, most 92,7.").kg, 0.01);
    }

    /**
     * A lement hossz le\u00faszott hossz, nem leadott kil\u00f3.
     *
     * A \u201element\u00fcnk 30 hosszt a m\u00e1sik s\u00e1vban" harmincasa a
     * hosszak sz\u00e1ma \u2013 a \u201elementem" ige m\u00e9gis harminc kil\u00f3s
     * m\u00e9r\u00e9st \u00edrt a s\u00falytrendbe.
     */
    @Test public void aPulseAverageAfterAWorkoutIsNotAWeight() {
        // A „spinning órát leadtam 55 percben, a pulzusátlag 148 volt"
        // pulzusátlagából 148 kilós mérés lett – miután a perc és a
        // pulzus tagmondata is kiesett, a teljes szöveg jött vissza.
        BodyParse.Body b = BodyParse.parse("A spinning órát leadtam "
                + "55 percben, a pulzusátlag 148 volt.");
        assertTrue(b == null || b.kg == 0);
    }

    /**
     * A hátralévő kiló a cél távolsága, nem a mérleg száma.
     *
     * A „90 kg-ról indultam, most 84,5, már csak 4,5 kiló a cél"
     * bejegyzéséből semmi nem lett – a felismerő a négy és felesen
     * akadt el, és a valódi mérés is elveszett.
     */
    @Test public void remainingKilosToGoalDoNotHideTheWeight() {
        assertEquals(84.5, BodyParse.parse("90 kg-ról indultam, most "
                + "84,5, már csak 4,5 kiló a cél.").kg, 0.01);
    }

    @Test public void aReceptionMeasurementIsMine() {
        // A „recepción mérték: 78,4 kg, 17,9 százalék zsír" mérése a
        // harmadik személyű igén ült, és elveszett.
        BodyParse.Body b = BodyParse.parse("A recepción mérték: 78,4 kg, "
                + "17,9 százalék zsír.");
        assertEquals(78.4, b.kg, 0.01);
        assertEquals(17.9, b.fatPct, 0.01);
    }

    @Test public void anUnmeasuredDayLogsNothing() {
        // A „ma nem mértem meg magam, de tegnap 79 volt" hetvenkilencet
        // a MAI napra írta a súlytrendbe – pedig a mondat épp azt
        // mondja, hogy ma nem állt mérlegre.
        BodyParse.Body b = BodyParse.parse("Ma nem mértem meg magam, de "
                + "tegnap 79 volt.");
        assertTrue(b == null || b.kg == 0);
        // A megtörtént mérés marad.
        assertEquals(79.0, BodyParse.parse("Ma reggel 79 kg voltam.").kg,
                0.01);
    }

    @Test public void anInflectedMeasurementWordStillCounts() {
        // Az „a reggeli MÉRÉSKOR 76,2 kg, az izomtömegem 34,1 kg"
        // mérése elveszett: a lista csak a ragtalan „mérés" alakot
        // ismerte, a ragozott alak nem számított test-szónak.
        assertEquals(76.2, BodyParse.parse("A reggeli méréskor 76,2 kg, "
                + "az izomtömegem 34,1 kg.").kg, 0.01);
        assertEquals(76.2, BodyParse.parse("A reggeli mérésnél 76,2 kg.")
                .kg, 0.01);
    }

    /**
     * Az úszásnem nem körfogat.
     *
     * Az „uszodában 1500 m gyors, 500 m mell, 200 m hát" mellúszásából
     * KÉTSZÁZ CENTIS mellbőség lett a mérés-naplóban – a méteres szám
     * távot mond, nem mérőszalagot.
     */
    /**
     * A célsúly nem mai mérés.
     *
     * A „ma reggel 84,5 kg, a célom 80 kg karácsonyig" mérése NYOMTALANUL
     * eltűnt: a cél kilója mellett két súly-szám állt a mondatban, és két
     * szám közül a mérés-olvasó inkább egyiket sem választja.
     */
    @Test public void aGoalWeightDoesNotSilenceTodaysMeasurement() {
        assertEquals(84.5, BodyParse.parse("Ma reggel 84,5 kg, a célom "
                + "80 kg karácsonyig.").kg, 0.01);
        assertEquals(79.1, BodyParse.parse("Reggel 79,1 kg. Cél: 75 alá "
                + "szeptember végéig.").kg, 0.01);
        // A puszta cél marad cél.
        assertEquals(0.0, BodyParse.parse("A célom 80 kg karácsonyig.")
                .kg, 0.01);
    }

    @Test public void aSwimStrokeIsNotAGirth() {
        BodyParse.Body b = BodyParse.parse("Az uszodában 1500 m gyors, "
                + "500 m mell, 200 m hát.");
        for (int i = 0; i < BodyParse.PART_KEYS.length; i++)
            assertEquals("kitalált körfogat: " + BodyParse.PART_KEYS[i],
                    0.0, b.cm[i], 0.01);
        // A valódi mellbőség marad mérés.
        assertEquals(100.0, BodyParse.parse("Mellbőségem 100 cm.")
                .cm[2], 0.01);
    }

    @Test public void aSwimLapCountIsNotAWeight() {
        assertEquals(0.0, BodyParse.parse("Lementem 30 hosszt a m\u00e1sik "
                + "s\u00e1vban.").kg, 0.01);
        // A kil\u00f3ra lemen\u0151 sz\u00e1m marad m\u00e9r\u00e9s.
        assertEquals(78.0, BodyParse.parse("Lementem 78 kil\u00f3ra.").kg, 0.01);
    }
}
