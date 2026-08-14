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

    /** Rögzített pillanat az útbaigazító-teszteknek. */
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

    /**
     * Felsorolt körfogatok: a vessző elválaszt, nem tizedesjegyet nyit.
     *
     * A „derék 84, csípő 95" mondatból eddig a DERÉK esett ki – a szám után
     * álló vessző elrontotta a mintát –, és a nyolcvannégy centiből a
     * súly-felismerőnél nyolcvannégy kiló lett. Egy mérésből így egyszerre
     * lett hiányos adat és hamis testsúly.
     */
    @Test public void severalGirthsInOneSentence() {
        BodyParse.Body b = BodyParse.parse("derék 84, csípő 95");
        assertEquals(84, b.cm[0], 0.01);
        assertEquals(95, b.cm[1], 0.01);
        assertEquals(0, b.kg, 0.01);
        BodyParse.Body c = BodyParse.parse("derék 84, csípő 95, mellkas 100");
        assertEquals(100, c.cm[2], 0.01);
        // A tizedesjegy változatlanul tizedesjegy.
        assertEquals(84.5, BodyParse.parse("derék 84,5 cm").cm[0], 0.01);
    }

    /**
     * A comb és a mell egyszerre testrész és étel – a centi dönt.
     *
     * A „comb 58 cm" eddig csirkecombként ment az Étrendbe, mert az
     * étel-felismerő hamarabb szólal meg. Kiírt mértékegységgel viszont
     * nincs kétség; nélküle marad minden a régiben.
     */
    @Test public void girthWithUnitBeatsTheFood() {
        assertEquals(Sentence.Kind.BODY,
                Sentence.of("comb 58 cm", java.util.Arrays.asList(Foods.ALL), NOW));
        assertEquals(Sentence.Kind.BODY,
                Sentence.of("mellkas 100 cm", java.util.Arrays.asList(Foods.ALL), NOW));
        // Mértékegység nélkül az étel marad étel.
        assertEquals(Sentence.Kind.MEAL,
                Sentence.of("mell 20 dkg csirke", java.util.Arrays.asList(Foods.ALL), NOW));
        assertEquals(Sentence.Kind.MEAL,
                Sentence.of("2 csirkecomb", java.util.Arrays.asList(Foods.ALL), NOW));
    }

    /**
     * A hosszabb szám ELEJE nem testsúly.
     *
     * A minta számjegy-határ nélkül dolgozott, így az „1500" első három
     * jegyéből százötven kiló lett, a „10000"-ből száz. Egy elgépelt szám
     * tehát nem hibaüzenetet adott, hanem egy hihető, de hamis mérést – és
     * a súlytrend, a BMI és a kalóriacél is ebből számol tovább.
     */
    @Test public void theStartOfALongerNumberIsNotAWeight() {
        none("1500");
        none("10000");
        none("1500 kg");
        none("mérleg 1500");
        none("2500");
        // Kiírva ugyanez: a „tízezer" tízezer, nem száz.
        none("tízezer");
        none("ezerötszáz");
        // A valódi mérés változatlan.
        kg("78,4", 78.4);
        kg("78,4 kg", 78.4);
        kg("100 kg", 100);
    }

    /** Az izomtömeg nem a testsúly – a mérleg egy sorban írja ki mindkettőt. */
    @Test public void muscleMassIsNotBodyWeight() {
        BodyParse.Body b = BodyParse.parse("testzsír 19,5%, izomtömeg 62 kg");
        assertEquals(19.5, b.fatPct, 0.01);
        assertEquals(0, b.kg, 0.01);
        assertEquals(0, BodyParse.parse("csonttömeg 3,2 kg, izomtömeg 62 kg").kg, 0.01);
        // A valódi súly mellett kiírt izomtömeg nem viszi el a mérést.
        assertEquals(80.0, BodyParse.parse("80 kg, izomtömeg 62 kg").kg, 0.01);
    }

    /** A birtokos alak is mérés: „derékbőségem 82 cm", „22% a testzsírom". */
    @Test public void possessiveFormsAreStillMeasurements() {
        BodyParse.Body a = BodyParse.parse("derékbőségem 82 cm lett");
        assertEquals(82.0, a.cm[0], 0.01);
        assertEquals(82.0, BodyParse.parse("derékbőség 82 cm").cm[0], 0.01);
        assertEquals(22.0, BodyParse.parse("22% a testzsírom").fatPct, 0.01);
        assertEquals(22.0,
                BodyParse.parse("az inbody szerint 22% a testzsírom").fatPct, 0.01);
    }

    /**
     * A napi összefoglalóban is ott a mérés.
     *
     * A „78,2 kg voltam" ugyanaz a mérés, mint a „78,2 kg vagyok" – egy
     * hosszabb, több mondatos napi bejegyzésben eddig elveszett, mert a
     * létige múlt ideje nem számított kimondásnak.
     */
    @Test public void aWholeDayEntryStillCarriesTheMeasurement() {
        kg("78,2 kg voltam", 78.2);
        kg("Ma reggel 6-kor keltem, 78,2 kg voltam. Reggeli: zabkása 60 g tejjel. "
                + "Délelőtt 45 perc kondi. Este 5 km futás 28 perc.", 78.2);
        // A konyhai mértékegység nem testsúly: a „zabkása 60 g" hatvanas
        // száma az adag, nem a mérleg száma.
        none("zabkása 60 g");
        none("150 g csirkemell");
    }

    /** A változás mondatában a MÁSODIK szám a mai súly. */
    @Test public void fromToKeepsTheNewValue() {
        // A „80-ról 76-ra fogytam" mai értéke hetvenhat – eddig a RÉGI súly
        // került a trendbe, vagyis a fogyás napján egy súlygyarapodás.
        kg("80-ról 76-ra fogytam", 76);
        kg("76-ról 80-ra híztam", 80);
        kg("82,5-ről 79,8-ra fogytam", 79.8);
        // A testzsír számát nem viszi el.
        assertEquals(15, BodyParse.parse("testzsír 18-ról 15%-ra ment le").fatPct, 0.01);
    }

    /** Az idő és a táv sem kiló – az edzés száma nem a mérlegé. */
    @Test public void minutesAndKilometresAreNotKilograms() {
        // Az „este 45 perc jóga, aztán 78,9 kg a mérlegen" negyvenöt PERCE
        // lett a testsúly – a valódi mérés pedig, ami ott állt a mondat másik
        // felében, elveszett.
        kg("este 45 perc jóga, aztán 78,9 kg a mérlegen", 78.9);
        kg("50 perc kondi, mérleg 81,2", 81.2);
        none("45 perc jóga");
        none("10 km futás");
    }

    /** A másik napló folytatása nem mérés: az esti pulzus nem testsúly. */
    @Test public void theOtherLogsContinuationIsNotAWeight() {
        none("nyugalmi pulzus reggel 47, este 62");
        none("aludtam 7 órát, éjjel 3");
        // A valódi mérés a másik napló mellett is megmarad.
        kg("78,4 kg, aludtam 7 órát", 78.4);
        kg("ma reggel 78,4, aludtam 7 órát", 78.4);
        kg("aludtam 7 órát, súlyom 80 kg", 80);
    }

    /** A láz nem testsúly – pont egy beteg napon rontaná el a trendet. */
    @Test public void aFeverIsNotAWeight() {
        none("beteg vagyok, 38 fokos lázam van");
        none("38,5 fokos láz");
        none("39 fok");
        // A valódi mérés változatlan.
        kg("ma reggel 78,4 kg voltam", 78.4);
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

    /**
     * A ragozott mértékegység is mértékegység.
     *
     * A „ma reggel 79 kilóval keltem" a legtermészetesebb magyar mérés-mondat,
     * és eddig kiesett: a „kilóval" bennmaradt a maradékban, és a mondat ettől
     * nem számított mérésnek.
     */
    @Test public void anInflectedUnitIsStillAUnit() {
        kg("ma reggel 79 kilóval keltem", 79);
        kg("78 kilóval ébredtem", 78);
        kg("reggel 80,5 kilót mértem", 80.5);
        // A magasság a mérés mellett szokott állni – az sem ronthatja el.
        kg("80 kg, 180 cm", 80);
        // A mérés körülménye is csak körülmény, nem adat.
        kg("78,2 kg ma reggel éhgyomorra", 78.2);
        kg("79 kg zuhany után", 79);
        // A reggeli három adat egy mondatban: a mérés eddig kiesett közülük.
        kg("ma reggel 78,4 kg, aludtam 7 órát", 78.4);
        kg("aludtam 8 órát, 78 kg", 78);
        // A vásárlás és a munkasúly továbbra sem mérés.
        none("vettem 2 kg almát");
        none("80 kg-os súllyal nyomtam");
    }

    /**
     * A kimondott tizedes és a mérés igéje.
     *
     * Diktálva senki nem mond vesszőt: „hetvennyolc egész négy". És aki
     * megmérte magát, azt le is írja – a „megmértem magam" ugyanolyan
     * kimondás, mint a „mérleg".
     */
    @Test public void aSpokenDecimalIsAMeasurement() {
        kg("reggel megmértem magam hetvennyolc egész négy kiló voltam", 78.4);
        kg("hetvennyolc egész négy", 78.4);
        kg("megmértem magam: 79,2", 79.2);
        // A felsorolás nem tizedestört: a tört csak EGY jegyű lehet.
        none("3 egész 12 darab");
        // Az alvás is érti a kimondott törtet.
        assertEquals(7.5, Sleep.parse("hét egész öt órát aludtam"), 0.01);
    }

    /**
     * A -ról/-re pár második száma a mai érték.
     *
     * A „derékbőségem 90-ről 86-ra csökkent" két számot mond: a régit és a
     * mait. A felismerő eddig az elsőt vette – pont azt, ami már NEM igaz –,
     * a körfogatnál pedig a kettő együtt olyan zavaros maradt, hogy semmi
     * nem lett belőle.
     */
    @Test public void theSecondNumberOfAFromToPairIsTodaysValue() {
        assertEquals(86, BodyParse.parse("a derékbőségem 90-ről 86-ra csökkent").cm[0], 0.01);
        assertEquals(88, BodyParse.parse("haskörfogatom 92 cm-ről 88-ra").cm[0], 0.01);
        assertEquals(18, BodyParse.parse("testzsír 22-ről 18 százalékra").fatPct, 0.01);
        assertEquals(79.8, BodyParse.parse("82,5-ről 79,8-ra fogytam").kg, 0.01);
        // Az egyszerű mérés nem sérül.
        assertEquals(84, BodyParse.parse("derék 84 cm, csípő 95 cm").cm[0], 0.01);
        assertEquals(78, BodyParse.parse("78 kg 18% zsír").kg, 0.01);
    }

    /**
     * Az életkor nem testsúly.
     *
     * A „férfi vagyok, 34 éves, 182 cm" bemutatkozó mondat harmincnégyese az
     * évek száma – eddig harmincnégy kilós mérésként került a súlytrendbe.
     */
    @Test public void ageIsNotAWeight() {
        assertEquals(0, BodyParse.parse("férfi vagyok, 34 éves, 182 cm").kg, 0.01);
        assertEquals(0, BodyParse.parse("42 éves lettem ma").kg, 0.01);
        // A mellette álló valódi mérés viszont marad.
        assertEquals(78, BodyParse.parse("34 éves vagyok és 78 kg").kg, 0.01);
        assertEquals(78, BodyParse.parse("78 kg vagyok").kg, 0.01);
    }

    /**
     * Az idő „alatt"-ja nem összehasonlítás.
     *
     * A „70 kg alatt vagyok" tényleg nem mérés – de a „79,2 kg volt a mérleg,
     * futottam 8 km-t 45 perc alatt" mondatban az „alatt" a negyvenöt PERCÉ,
     * és eddig ettől az egész reggeli mérés kiesett.
     */
    @Test public void theUnderOfADurationIsNotAComparison() {
        assertEquals(79.2, BodyParse.parse(
                "79,2 kg volt a mérleg, futottam 8 km-t 45 perc alatt").kg, 0.01);
        assertEquals(88, BodyParse.parse(
                "a haskörfogatom 92 cm-ről 88-ra ment le fél év alatt").cm[0], 0.01);
        // A valódi összehasonlítás továbbra sem mérés.
        assertEquals(0, BodyParse.parse("70 kg alatt vagyok").kg, 0.01);
        assertEquals(0, BodyParse.parse("80 kg alatt szeretnék lenni").kg, 0.01);
    }

    /**
     * A láz nem testsúly – „fok" nélkül sem.
     *
     * A „beteg vagyok, lázam van 38,5" harmincnyolc és fél kilós mérésként
     * került a súlytrendbe. Pont egy olyan napon, amikor senki nem áll
     * mérlegre – és a görbe utána hetekig hamis maradt.
     */
    @Test public void aFeverIsNotAWeightWithoutTheDegreeWord() {
        assertEquals(0, BodyParse.parse("beteg vagyok, lázam van 38,5").kg, 0.01);
        assertEquals(0, BodyParse.parse("38 fokos lázam van").kg, 0.01);
        assertEquals(0, BodyParse.parse("hőemelkedésem van, 37,8").kg, 0.01);
        // Láz-szó nélkül ugyanez a szám valódi mérés marad.
        assertEquals(38.5, BodyParse.parse("38,5 kg vagyok").kg, 0.01);
        assertEquals(79.2, BodyParse.parse("reggel 79,2 kg").kg, 0.01);
    }

    /**
     * A helyesbítés második száma az igazi – a mérlegnél is.
     *
     * A „nem 80 kg vagyok, hanem 78" mondatból NYOLCVAN kiló került a
     * súlytrendbe: pont az, amit a mondat tagad. A hibás adat rosszabb, mint
     * a semmilyen – a görbe és a BMI is abból számol tovább.
     */
    @Test public void theCorrectedWeightWins() {
        assertEquals(78, BodyParse.parse("nem 80 kg vagyok, hanem 78").kg, 0.01);
        assertEquals(76.5, BodyParse.parse("nem 79 kg lettem, hanem 76,5").kg, 0.01);
        assertEquals(80, BodyParse.parse("80 kg vagyok").kg, 0.01);
    }

    /**
     * A körfogat-felsorolásban minden érték a SAJÁT testrészéhez tartozik.
     *
     * A „92 cm derék, 100 cm csípő, 38 cm comb, 34 cm kar" felsorolásban
     * minden szám egyet csúszott: a derék a csípő számát vitte el, a csípő a
     * combét. A felsorolás szórendje egységes, és a mondat EGÉSZÉBŐL derül
     * ki – testrészenként dönteni hibás volt, mert a lista közepén mindkét
     * alak illeszkedik.
     */
    @Test public void everyGirthKeepsItsOwnNumber() {
        BodyParse.Body b = BodyParse.parse(
                "78 kg, 18% zsír, 92 cm derék, 100 cm csípő, 38 cm comb, 34 cm kar");
        assertEquals(78, b.kg, 0.01);
        assertEquals(92, b.cm[0], 0.01);
        assertEquals(100, b.cm[1], 0.01);
        assertEquals(38, b.cm[3], 0.01);
        assertEquals(34, b.cm[4], 0.01);
        // A másik szórend is a sajátját kapja.
        BodyParse.Body c = BodyParse.parse("derék 84 cm, csípő 95 cm");
        assertEquals(84, c.cm[0], 0.01);
        assertEquals(95, c.cm[1], 0.01);
        assertEquals(84, BodyParse.parse("84 cm derék").cm[0], 0.01);
        assertEquals(92, BodyParse.parse("haskörfogatom 92").cm[0], 0.01);
    }

    /**
     * Az időtáv csak kíséret a mérés mellett.
     *
     * A „79,8 kg egy hét alatt" és a „78 kg két hónap után" mérés – eddig a
     * mellette álló szavaktól az egész mondat kiesett. (A kiírt számnév miatt
     * a maskTimeUnder a fordítás ELŐTT fut: az „egy hét" a digits() után már
     * „1 7", és ott az „alatt" összehasonlításnak látszana.)
     */
    @Test public void theTimeSpanIsOnlyCompanyForTheMeasurement() {
        assertEquals(79.8, BodyParse.parse("79,8 kg egy hét alatt").kg, 0.01);
        assertEquals(79.8, BodyParse.parse("79,8 kg, -1,2 kg egy hét alatt").kg, 0.01);
        assertEquals(78, BodyParse.parse("78 kg két hónap után").kg, 0.01);
        // A valódi összehasonlítás továbbra sem mérés.
        assertEquals(0, BodyParse.parse("70 kg alatt vagyok").kg, 0.01);
        assertEquals(0, BodyParse.parse("80 kg alatt szeretnék lenni").kg, 0.01);
    }

    /**
     * A mérés FŐNEVE is kimondás, és a mértékegység nem tolja el az új értéket.
     *
     * A „reggeli mérés: 80,1 kg" eddig teljesen elveszett: a „mérés" szó miatt
     * a „csak számok maradtak" vizsgálat megbukott, a mérés-szavak listáján
     * meg nem volt ott. A „haskörfogat 92-ről 88 cm-re" pedig a RÉGI értéket
     * tartotta meg – vagyis a fogyás napján egy hízást írt a naplóba.
     */
    @Test public void theWordMeasurementCountsAndTheNewValueWins() {
        assertEquals(80.1, BodyParse.parse("reggel mérés: 80,1 kg, "
                + "izomtömeg 35,2 kg").kg, 0.01);
        assertEquals(88.0, BodyParse.parse("haskörfogat 92-ről 88 cm-re").cm[0], 0.01);
        // A régi, egység nélküli alak sem sérül.
        assertEquals(76.0, BodyParse.parse("80-ról 76-ra fogytam").kg, 0.01);
    }

    /**
     * A méretlen testrész száma nem testsúly.
     *
     * A „combom 58 cm, vádli 38" harmincnyolcasa a vádli körfogata – a
     * naplóba viszont harmincnyolc kilós mérésként került, egy felnőtt
     * súlytrendjébe. (A vádlinak nincs saját mezője, de attól még nem a
     * mérleg száma áll mellette.)
     */
    @Test public void anUnmeasuredBodyPartIsNotAWeight() {
        BodyParse.Body b = BodyParse.parse("combom 58 cm, vádli 38");
        assertEquals(0.0, b.kg, 0.01);
        assertEquals(58.0, b.cm[3], 0.01);
    }

    /**
     * A mozgás és az étkezés tagmondata is másé.
     *
     * A napi összefoglaló egyetlen mondat: „tegnap este 3 pohár bort ittam,
     * ma reggel 79,8 kg". A mérésből eddig SEMMI nem lett, mert a bor szavai
     * miatt a „csak számok maradtak" vizsgálat megbukott – pedig a kilogramm
     * ott állt kiírva. Az alvás- és pulzus-tagmondatot már eddig is
     * elhagytuk; a mozgás és az étkezés ugyanilyen.
     */
    @Test public void theWorkoutAndMealClausesBelongElsewhereToo() {
        assertEquals(79.8, BodyParse.parse("tegnap este 3 pohár bort ittam, "
                + "ma reggel 79,8 kg").kg, 0.01);
        assertEquals(78.4, BodyParse.parse("reggel 78,4 kg, 7 óra alvás, "
                + "54 nyugalmi. Délelőtt 45 perc futás 8 km, ebédre csirke "
                + "rizzsel.").kg, 0.01);
        // A „reggeli" szándékosan nem tiltó szó: az még mérés is lehet.
        assertEquals(80.1, BodyParse.parse("reggeli mérés: 80,1 kg").kg, 0.01);
    }

    /**
     * A sorozat tagmondata is másé.
     *
     * A „körfogatok és súlyok: derék 84, guggolás 3x5 100" száza a RÚDON van,
     * nem a mérlegen – eddig száz kilós mérés lett belőle a súlytrendben, a
     * derék viszont megmaradt.
     */
    @Test public void aBarbellWeightIsNotABodyWeight() {
        BodyParse.Body b = BodyParse.parse("körfogatok és súlyok: derék 84, "
                + "guggolás 3x5 100");
        assertEquals(0.0, b.kg, 0.01);
        assertEquals(84.0, b.cm[0], 0.01);
    }

    /**
     * A „lettem" nem „ettem".
     *
     * A tagmondat-szűrő contains()-szel keresett, és a „78 kg LETTEM, végre
     * 80 alá mentem" első tagmondatában megtalálta az „ettem"-et – a mérés
     * étkezés-tagmondatként esett ki, és az egész mondatból semmi nem lett.
     * A szóhatár + igekötő szabály a „megettem"-et továbbra is étkezésnek
     * látja.
     */
    @Test public void becomingIsNotEating() {
        assertEquals(78.0, BodyParse.parse("78 kg lettem, végre 80 alá "
                + "mentem").kg, 0.01);
        assertEquals(79.8, BodyParse.parse("megettem egy pizzát, 79,8 kg "
                + "voltam reggel").kg, 0.01);
        assertEquals(77.7, BodyParse.parse("77,7 kg, eddigi legjobb").kg, 0.01);
        assertEquals(82.0, BodyParse.parse("visszahíztam 82-re").kg, 0.01);
    }

    /**
     * Az elért cél már mérés.
     *
     * Az „elértem a célsúlyom, 72 kg" hetvenkettője a mai súly – a cél szava
     * eddig az egész mondatot elnémította, pedig aki elérte, az épp most állt
     * a mérlegen. A puszta kívánság marad kívánság.
     */
    @Test public void aReachedGoalIsAMeasurement() {
        assertEquals(72.0, BodyParse.parse("elértem a célsúlyom, 72 kg").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("a cél 75 kg").kg, 0.01);
        assertEquals(0.0, BodyParse.parse("szeretnék 72 kg lenni").kg, 0.01);
    }

    /** A dátum is csak kíséret: az „aug. 14. reggel 78 kg" jegyzet-sor. */
    @Test public void aDateStampDoesNotHideTheWeight() {
        assertEquals(78.0, BodyParse.parse("aug. 14. reggel 78 kg").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("július 28-án 78 kg").kg, 0.01);
    }

    /** A „-ról" a kiindulópont ragja, sosem a mai érték. */
    @Test public void theStartingWeightIsNotToday() {
        assertEquals(78.2, BodyParse.parse("83,5 kilóról indultam januárban, "
                + "ma 78,2").kg, 0.01);
    }

    /**
     * Az étel tagmondata sem veszi el a mérést.
     *
     * A „ma reggel: 40 perc futás, zuhany, zabkása fahéjjal, 79,1 kg"
     * zabkásája miatt az egész mérés elveszett – a „csak számok maradtak"
     * vizsgálat a kása szavain bukott meg. A centivel írt tagmondat viszont
     * mérés marad: a „38 cm comb" a comb körfogata, nem csirkecomb.
     */
    @Test public void aFoodClauseDoesNotHideTheWeight() {
        assertEquals(79.1, BodyParse.parse("ma reggel: 40 perc futás, zuhany, "
                + "zabkása fahéjjal, 79,1 kg").kg, 0.01);
        assertEquals(38.0, BodyParse.parse("78 kg, 18% zsír, 92 cm derék, "
                + "100 cm csípő, 38 cm comb, 34 cm kar").cm[3], 0.01);
    }

    /** A fürdés és az esti jelző is csak kíséret a mérés mellett. */
    @Test public void aBathTimeWeighInCounts() {
        assertEquals(79.2, BodyParse.parse("esti fürdés után 79,2 kg").kg, 0.01);
    }

    /**
     * Az átlépett küszöb száma nem a mérleg száma.
     *
     * A „végre lement a súlyom 80 alá, 79.6 kg" nyolcvanasa a lélektani
     * határ – mégis nyolcvan kiló került a trendbe a 79,6 helyett: a
     * fogyás ünnepének napján egy fél kilóval nagyobb súly.
     */
    @Test public void aThresholdIsNotTheMeasurement() {
        assertEquals(79.6, BodyParse.parse("végre lement a súlyom 80 alá, "
                + "79.6 kg").kg, 0.01);
    }

    /**
     * A gyakorlat súlya a rúdon van, nem a mérlegen.
     *
     * A „vállból nyomás ma csak 40 kg ment, fáradt voltam" negyvenese a
     * súlyzós naplóé – a „voltam" miatt mégis negyven kilós mérés lett
     * belőle a súlytrendben. A centivel írt vádli viszont körfogat marad,
     * hiába gyakorlatnév is a vádli.
     */
    @Test public void anExerciseWeightIsNotTheScale() {
        assertEquals(0.0, BodyParse.parse("vállból nyomás ma csak 40 kg "
                + "ment, fáradt voltam").kg, 0.01);
        assertEquals(78.0, BodyParse.parse("evezés volt reggel, este 78 kg "
                + "a mérlegen").kg, 0.01);
    }

    /** A „pont" és a „kg-nál tartok" is csak kíséret a mérés mellett. */
    @Test public void exactAndStandingAtPhrasesAreCompany() {
        assertEquals(68.0, BodyParse.parse("reggel éhgyomorra "
                + "68 kg pont").kg, 0.01);
        assertEquals(82.0, BodyParse.parse("82 kg-nál tartok").kg, 0.01);
    }
}
