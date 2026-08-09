package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Intervallum-beállítás egy mondatból.
 *
 * Itt az elszámolás nem a naplóban derül ki, hanem edzés közben: rossz
 * munkaidővel az egész kör használhatatlan. Ezért inkább semmit nem
 * állítunk be, mint valami kitaláltat.
 */
public class IntervalParseTest {

    private static String sum(String q) {
        IntervalParse.Plan p = IntervalParse.parse(q);
        return p == null ? "—" : p.rounds + "×" + p.work + "/" + p.rest;
    }

    @Test public void theSpokenFormWorks() {
        assertEquals("3×40/20", sum("3 kör 40 mp munka 20 mp pihenő"));
        assertEquals("5×60/30", sum("5 kör 1 perc munka 30 másodperc pihenő"));
        assertEquals("6×45/15", sum("6 sorozat 45 másodperc terhelés 15 másodperc szünet"));
        // A szám a szó után is állhat.
        assertEquals("4×30/10", sum("4 kör, munka 30 mp, pihenő 10 mp"));
    }

    @Test public void theGymShorthandWorks() {
        assertEquals("8×20/10", sum("8x20/10"));
        assertEquals("6×45/15", sum("45/15 x 6"));
        assertEquals("1×40/20", sum("40/20"));
        assertEquals("10×30/30", sum("10 kör 30-30"));
    }

    @Test public void knownFormatsAreRecognisedByName() {
        assertEquals("8×20/10", sum("tabata"));
        assertEquals("8×20/10", sum("csináljunk egy tabatát"));
        // A kimondott körszám erősebb, mint az alapérték.
        assertEquals("6×20/10", sum("tabata 6 kör"));
        assertEquals("10×60/0", sum("emom 10 perc"));
        // A norvég 4x4: 4×4 perc erős, 3 perc pihenő.
        assertEquals("4×240/180", sum("norvég 4x4"));
        assertEquals("4×240/180", sum("norveg intervall"));
        assertEquals("5×240/180", sum("norvég 4x4, 5 kör"));
    }

    /** A perjeles pár PERCBEN is mehet: „2 perc / 1 perc, 5 kör". */
    @Test public void theSlashPairAlsoWorksInMinutes() {
        assertEquals("5×120/60", sum("2 perc / 1 perc, 5 kör"));
        assertEquals("4×180/60", sum("4x3 perc / 1 perc"));
        // A másodperces alak változatlan.
        assertEquals("8×20/10", sum("8x20/10"));
        assertEquals("10×40/20", sum("40 mp / 20 mp, 10 kör"));
    }

    @Test public void aSingleTimeIsTheWorkInterval() {
        assertEquals("5×30/0", sum("5 kör 30 másodperc"));
        assertEquals("1×120/0", sum("2 perc"));
    }

    @Test public void nothingIsInventedWithoutAWorkInterval() {
        for (String q : new String[]{"", "   ", "3 kör", "edzés", "jó volt",
                "kör kör kör", "kondi 60 kg", null}) {
            assertNull("kitalált beállítás: " + q, IntervalParse.parse(q));
        }
    }

    @Test public void absurdValuesAreRejected() {
        // A körszám és az idő is életszerű tartományban marad.
        for (String q : new String[]{"999 kör 40 mp munka", "3 kör 99999 mp munka",
                "0 kör 0 mp", "100x1/1"}) {
            IntervalParse.Plan p = IntervalParse.parse(q);
            if (p == null) continue;
            assertTrue("kör: " + q, p.rounds >= 1 && p.rounds <= IntervalParse.MAX_ROUNDS);
            assertTrue("munka: " + q, p.work >= IntervalParse.MIN_SEC
                    && p.work <= IntervalParse.MAX_SEC);
            assertTrue("pihenő: " + q, p.rest >= 0 && p.rest <= IntervalParse.MAX_SEC);
        }
    }

    @Test public void theLabelReadsLikeAPlan() {
        IntervalParse.Plan p = IntervalParse.parse("8x20/10");
        assertNotNull(p);
        assertEquals("8 kör  ·  20 mp munka  ·  10 mp pihenő", p.label());
        assertEquals(240, p.totalSec());
        assertEquals("5 kör  ·  1 perc munka", IntervalParse.parse("5 kör 60 mp munka").label());
        assertTrue(IntervalParse.parse("3 kör 90 mp munka").label().contains("1:30"));
    }

    @Test public void randomTextNeverCrashes() {
        String[] tokens = {"kör", "3", "40", "mp", "munka", "pihenő", "/", "x", "tabata",
                "perc", "másodperc", "emom", "-", "0", "9999", "", " ", ",", "szünet",
                "8x20/10", "1,5 perc", "aaaaaaaaaaaaaaaaaaaaaa", "🏋", "sorozat", "szett"};
        for (long seed : new long[]{20260804, 7, 4242}) {
            java.util.Random rnd = new java.util.Random(seed);
            for (int i = 0; i < 4000; i++) {
                StringBuilder sb = new StringBuilder();
                int n = rnd.nextInt(10);
                for (int w = 0; w < n; w++) {
                    sb.append(tokens[rnd.nextInt(tokens.length)]);
                    if (rnd.nextInt(4) > 0) sb.append(' ');
                }
                String q = sb.toString();
                IntervalParse.Plan p = IntervalParse.parse(q);
                if (p != null) {
                    assertTrue(q, p.rounds >= 1 && p.rounds <= IntervalParse.MAX_ROUNDS);
                    assertTrue(q, p.work >= IntervalParse.MIN_SEC
                            && p.work <= IntervalParse.MAX_SEC);
                    assertTrue(q, p.rest >= 0 && p.rest <= IntervalParse.MAX_SEC);
                    assertTrue(!p.label().isEmpty());
                }
                // Ugyanaz a mondat mindig ugyanazt adja.
                IntervalParse.Plan again = IntervalParse.parse(q);
                assertEquals(p == null, again == null);
            }
        }
    }

    @Test public void warmupAndCooldownAreUnderstood() {
        IntervalParse.Plan p = IntervalParse.parse("2 perc bemelegítés, 6 kör 40/20");
        assertNotNull(p);
        assertEquals(6, p.rounds);
        assertEquals(40, p.work);
        assertEquals(20, p.rest);
        assertEquals(120, p.warm);
        assertEquals(0, p.cool);
        assertEquals(120 + 6 * 60, p.totalSec());
        assertTrue(p.label().contains("2 perc bemelegítés"));

        IntervalParse.Plan q = IntervalParse.parse("tabata, levezetés 3 perc");
        assertNotNull(q);
        assertEquals(180, q.cool);
        // Amiről a mondat nem szól, az nulla marad – a hívó ilyenkor nem
        // írja felül a meglévő beállítást.
        assertEquals(0, q.warm);
    }

    @Test public void theRoundCountCanComeFromTheTotalTime() {
        // „20 perc alatt 40/20”: 60 mp-es kör, tehát 20 kör.
        assertEquals("20×40/20", sum("20 perc alatt 40/20"));
        assertEquals("10×45/15", sum("10 percig 45/15"));
        // Jelzőszó nélkül nem találgatunk: a 30 mp itt a munka, nem a teljes idő.
        assertEquals("1×30/15", sum("30/15"));
        // A kimondott körszám erősebb a számolt értéknél.
        assertEquals("5×40/20", sum("5 kör 40/20 20 perc alatt"));
    }

    @Test public void theWordsPeopleActuallyUseAreUnderstood() {
        // Huszonegy valódi terem-megfogalmazással szondáztam a felismerőt;
        // ezek azok, amiken elhasalt.

        // Kiírt számok – a teremben senki nem ír számjegyet.
        assertEquals("4×30/0", sum("négy kör 30 másodperc"));
        assertEquals("4×60/30", sum("egy perc plank, 30 mp pihenő, 4 kör"));
        // A tizedesvessző nem tagmondathatár: a fél perc fél perc maradt.
        assertEquals("1×30/30", sum("fél perc munka fél perc pihenő"));
        // „-szor/-szer”: szorzószám a körökre.
        assertEquals("8×20/10", sum("20/10 nyolcszor"));
        // Körszám × szakaszhossz mértékegységgel.
        assertEquals("6×180/60", sum("6 x 3 perc futás 1 perc séta"));
        // Csak a pihenőt nevezik meg: a munka az első kimondott idő.
        assertEquals("10×50/10", sum("köröskénti 50 mp munka és 10 mp pihenő, 10 kör"));
    }

    @Test public void ambiguousSentencesGiveNothing() {
        // A „15 percig” a teljes idő, nem egy szakasz hossza – megnevezetlenül
        // ekkora munkaidőt nem fogadunk el, mert edzés közben derülne ki.
        assertNull(IntervalParse.parse("minden percben 1 kör, 15 percig"));
        // Távalapú intervall: nincs benne idő, tehát nincs mit beállítani.
        assertNull(IntervalParse.parse("intervall: 400 m gyors, 200 m lassú"));
    }

    @Test public void theSpokenTensAreNumbersToo() {
        // A „negyven" eddig szó maradt, ezért a munkaidő a pihenőé lett:
        // negyven másodperc helyett húsz.
        IntervalParse.Plan p = IntervalParse.parse(
                "négyszer negyven másodperc munka húsz másodperc pihenő");
        assertEquals(4, p.rounds);
        assertEquals(40, p.work);
        assertEquals(20, p.rest);
    }

    @Test public void theUnitMayStandOnBothSidesOfTheSlash() {
        // „40 mp / 20 mp": a pihenő eddig némán elveszett.
        IntervalParse.Plan p = IntervalParse.parse("40 mp / 20 mp, 10 kör");
        assertEquals(10, p.rounds);
        assertEquals(40, p.work);
        assertEquals(20, p.rest);
    }

    @Test public void theGymBoardClockNotationWorks() {
        // Perc:másodperc, ahogy a táblára írják.
        IntervalParse.Plan p = IntervalParse.parse("1:30 munka 0:30 pihenő 6 kör");
        assertEquals(6, p.rounds);
        assertEquals(90, p.work);
        assertEquals(30, p.rest);
        IntervalParse.Plan q = IntervalParse.parse("8 kör 1:00 munka 0:20 pihenő");
        assertEquals(60, q.work);
        assertEquals(20, q.rest);
    }

    @Test public void emomCountsTheMinutesAsRounds() {
        // Az EMOM percenként egy kör – a szám a név után áll.
        assertEquals(12, IntervalParse.parse("emom 12").rounds);
        assertEquals(20, IntervalParse.parse("emom 20 perc").rounds);
        assertEquals(10, IntervalParse.parse("emom 10 perc").rounds);
        // Kimondott körszám továbbra is erősebb.
        assertEquals(6, IntervalParse.parse("tabata 6 kör").rounds);
    }

    @Test public void theEnglishGymPlanIsUnderstoodToo() {
        // Az internetről másolt terv angolul érkezik – a mp/perc egységeket
        // eddig is értette, a kulcsszavakat nem.
        IntervalParse.Plan p = IntervalParse.parse("8 rounds 20 sec work 10 sec rest");
        assertEquals(8, p.rounds);
        assertEquals(20, p.work);
        assertEquals(10, p.rest);
        IntervalParse.Plan q = IntervalParse.parse("5 rounds of 30s work 30s rest");
        assertEquals(5, q.rounds);
        assertEquals(30, q.work);
        assertEquals(30, q.rest);
    }

    @Test public void bracketsAreJustPunctuation() {
        // „10x(40s/20s)" ugyanaz, mint a „10x40/20" – eddig egy kör lett belőle.
        IntervalParse.Plan p = IntervalParse.parse("10x(40s/20s)");
        assertEquals(10, p.rounds);
        assertEquals(40, p.work);
        assertEquals(20, p.rest);
    }

    @Test public void theSpokenSecondsAreUnderstood() {
        IntervalParse.Plan p = IntervalParse.parse("százhúsz másodperc munka 6 kör");
        assertEquals(120, p.work);
        assertEquals(6, p.rounds);
        IntervalParse.Plan q = IntervalParse.parse(
                "hetven másodperc munka harminc másodperc pihenő 8 kör");
        assertEquals(70, q.work);
        assertEquals(30, q.rest);
        assertEquals(8, q.rounds);
    }

    @Test public void aLeadingMultiplierIsTheRoundCount() {
        // „10x30s on 30s off": az „on" nem kulcsszó, a perjel hiányzik – a tíz
        // kör korábban csendben EGYRE olvadt, és az edzés a tizedénél véget ért.
        IntervalParse.Plan p = IntervalParse.parse("10x30s on 30s off");
        assertEquals(10, p.rounds);
        assertEquals(30, p.work);
        assertEquals(30, p.rest);
        // A szorzó CSAK végső esetben körszám: a kimondott „kör” előrébb van.
        assertEquals(8, IntervalParse.parse("30 mp munka 30 mp pihenő 8 kör").rounds);
        // És nem csinál időzítőt a súlyzós mondatból: a „3x10” tíze ismétlés,
        // nem másodperc.
        assertNull(IntervalParse.parse("guggolás 3x10"));
        assertNull(IntervalParse.parse("3x10 60 kg"));
    }

    @Test public void theFieldNotationOnTheBoardIsUnderstood() {
        // A táblára írt terv mezőkből áll, és ott a körszám a szó MÖGÖTT van.
        IntervalParse.Plan p = IntervalParse.parse("kör: 6, munka: 40mp, pihenő: 20mp");
        assertEquals(6, p.rounds);
        assertEquals(40, p.work);
        assertEquals(20, p.rest);
        // Kettőspont nélkül nem találgatunk: a „kör 40 mp munka" negyvene
        // munkaidő, nem negyven kör.
        IntervalParse.Plan q = IntervalParse.parse("kör 40 mp munka");
        assertEquals(1, q.rounds);
        assertEquals(40, q.work);
    }

    @Test public void amrapIsOneLongBlock() {
        // Az AMRAP-ban annyi kör megy, amennyi belefér – körszámot adni neki
        // pont azt venné el, amiről szól.
        IntervalParse.Plan p = IntervalParse.parse("amrap 20 perc");
        assertEquals(1, p.rounds);
        assertEquals(1200, p.work);
        assertEquals(0, p.rest);
        assertEquals(1200, IntervalParse.parse("20 perc amrap").work);
        assertEquals(720, IntervalParse.parse("amrap 12").work);
        // Idő nélkül nincs mit beállítani.
        assertNull(IntervalParse.parse("amrap"));
    }

    @Test public void theRestSurvivesInFourMoreForms() {
        // Mind a négy alak a teremben szokásos, és mind veszített valamit:
        // vagy a pihenőt, vagy a körszámot.
        IntervalParse.Plan a = IntervalParse.parse("8 kör: 20 mp sprint, 40 mp séta");
        assertEquals(8, a.rounds);
        assertEquals(20, a.work);
        assertEquals(40, a.rest);          // a „séta" is pihenő
        IntervalParse.Plan b = IntervalParse.parse("5x(3 perc / 1 perc)");
        assertEquals(5, b.rounds);
        assertEquals(180, b.work);
        assertEquals(60, b.rest);          // a perjel utáni idő
        IntervalParse.Plan c = IntervalParse.parse("30 mp on 30 mp off 10x");
        assertEquals(10, c.rounds);        // záró szorzó
        assertEquals(30, c.work);
        assertEquals(30, c.rest);
        IntervalParse.Plan d = IntervalParse.parse("3 perc munka 1 perc pihenő, 6 ismétlés");
        assertEquals(6, d.rounds);         // az „ismétlés" itt kör
        assertEquals(180, d.work);
        assertEquals(60, d.rest);
    }
    /**
     * A szakasz neve is határ, nem csak a vessző.
     *
     * Sokan nem tesznek vesszőt: a „45 másodperc munka 15 pihenő" mondatban a
     * pihenő elé eső egyetlen KIMONDOTT idő a negyvenöt volt, így a pihenő is
     * negyvenöt lett – a terv létrejött, csak háromszor hosszabb szünettel.
     */
    @Test public void thePhaseNameSeparatesTheTwoTimes() {
        IntervalParse.Plan p = IntervalParse.parse("45 másodperc munka 15 mp pihenő 8 kör");
        assertEquals(45, p.work);
        assertEquals(15, p.rest);
    }

    /**
     * Mértékegység nélküli pihenő: a mértékegységet az első kimondott időtől
     * örökli, mert így beszél az ember.
     */
    @Test public void theRestInheritsTheUnit() {
        IntervalParse.Plan a = IntervalParse.parse("45 másodperc munka 15 pihenő nyolcszor");
        assertEquals(8, a.rounds);
        assertEquals(45, a.work);
        assertEquals(15, a.rest);
        IntervalParse.Plan b = IntervalParse.parse("2 perc munka 1 pihenő 5 kör");
        assertEquals(120, b.work);
        assertEquals(60, b.rest);
    }

    /**
     * A bemelegítés ideje sosem a munkaidő.
     *
     * A „bemelegítés 5 perc, 10 kör 1/1, levezetés 5 perc" munkaideje eddig öt
     * perc lett – vagyis egy kör pontosan olyan hosszú, mint a bemelegítés.
     * A „1/1" mértékegység nélkül nem eldönthető, ezért inkább nem találgatunk.
     */
    @Test public void theWarmupIsNeverTheWorkTime() {
        assertNull(IntervalParse.parse("bemelegítés 5 perc, 10 kör 1/1, levezetés 5 perc"));
        // Ami egyértelmű, az változatlanul átmegy.
        IntervalParse.Plan p =
                IntervalParse.parse("2 perc bemelegítés 6 kör 45/15 3 perc levezetés");
        assertEquals(45, p.work);
        assertEquals(15, p.rest);
        assertEquals(120, p.warm);
        assertEquals(180, p.cool);
    }
    /**
     * A kötőjeles szorzó is körszám: a „10-szer" ugyanaz, mint a „tízszer".
     *
     * Nélküle a terv EGYSZER futott le tíz helyett – a szám ott volt a
     * mondatban, csak nem jutott el a körszámig.
     */
    @Test public void theHyphenatedMultiplierIsARoundCount() {
        assertEquals(10, IntervalParse.parse("1 perc munka és 1 perc pihenő 10-szer").rounds);
        assertEquals(8, IntervalParse.parse("20 mp gyors 10 mp lassú 8-szor").rounds);
        // A kötőjel nélküli alak nem romolhatott el.
        assertEquals(5, IntervalParse.parse("négy perc munka egy perc pihenő ötször").rounds);
    }

    /** A „lassú" is pihenő: a váltott tempójú futásban ez a szünet. */
    @Test public void theSlowPartIsTheRest() {
        IntervalParse.Plan p = IntervalParse.parse("20 mp gyors 10 mp lassú 8-szor");
        assertEquals(20, p.work);
        assertEquals(10, p.rest);
    }
    /**
     * Vesszővel tagolt mezőlista kettőspont nélkül: „kör 8, munka 30,
     * pihenő 30".
     *
     * Ez a táblára írt terv alakja. Kettősponttal már értettük; anélkül
     * viszont kimaradt az EGÉSZ terv. Általánosan nem találgatunk (a
     * „kör 40 mp munka" negyvene munkaidő, nem negyven kör) – itt az a
     * garancia, hogy a tagmondatban a szón és a számon kívül nincs semmi.
     */
    @Test public void aCommaSeparatedFieldListWorksWithoutColons() {
        IntervalParse.Plan p = IntervalParse.parse("kör 8, munka 30, pihenő 30");
        assertNotNull(p);
        assertEquals(8, p.rounds);
        assertEquals(30, p.work);
        assertEquals(30, p.rest);
        IntervalParse.Plan q = IntervalParse.parse("munka 45, pihenő 15, kör 10");
        assertEquals(10, q.rounds);
        assertEquals(45, q.work);
        assertEquals(15, q.rest);
        // A kettőspontos alak és a mondatszerű alak nem romolhatott el.
        assertEquals(6, IntervalParse.parse("kör: 6, munka: 40mp, pihenő: 20mp").rounds);
        assertEquals(40, IntervalParse.parse("3 kör 40 mp munka 20 mp pihenő").work);
        // Tagmondat nélküli mezőnév továbbra sem elég: itt a negyven munkaidő.
        assertEquals(40, IntervalParse.parse("kör 40 mp munka").work);
    }

    /**
     * A megosztott terv visszaolvasható – ez a megosztás egész értelme.
     *
     * A sablon szövegként megy tovább (üzenetben, jegyzetben), a másik
     * telefonon pedig ugyanez a felismerő állítja vissza. Ha a két oldal
     * elcsúszna, a kapott edzés csendben MÁS lenne, mint a küldött.
     */
    @Test public void everySharedPlanReadsBackTheSame() {
        int[] works = {20, 30, 40, 45, 60, 90, 120, 180};
        int[] rests = {0, 10, 15, 20, 30, 60};
        int[] rounds = {1, 3, 6, 8, 10, 20};
        int[] warms = {0, 60, 120, 300};
        int[] cools = {0, 60, 180};
        StringBuilder bad = new StringBuilder();
        int n = 0;
        for (int w : works) for (int r : rests) for (int c : rounds)
            for (int wa : warms) for (int co : cools) {
                IntervalParse.Plan p = IntervalParse.parse(
                        (wa > 0 ? (wa / 60) + " perc bemelegítés, " : "")
                        + c + " kör " + w + " mp munka" + (r > 0 ? " " + r + " mp pihenő" : "")
                        + (co > 0 ? ", " + (co / 60) + " perc levezetés" : ""));
                if (p == null) continue;
                n++;
                IntervalParse.Plan q = IntervalParse.parse(p.sentence());
                if (q == null) { bad.append("\n  nem olvasható: ").append(p.sentence()); continue; }
                if (q.rounds != p.rounds || q.work != p.work || q.rest != p.rest
                        || q.warm != p.warm || q.cool != p.cool)
                    bad.append("\n  ").append(p.sentence()).append(" -> ").append(q.label());
            }
        assertTrue("legalább ezer tervet néztünk", n > 1000);
        assertEquals("oda-vissza eltérés:" + bad, 0, bad.length());
    }

    /**
     * Idő-vezérelt formák: a mondat a HOSSZT mondja ki, nem a körszámot.
     *
     * Huszonhat valós intervallum-mondattal végigpróbálva ez a négy fajta
     * hiányzott. Mindegyik hétköznapi terem-mondat, és egyikben sincs semmi
     * kétértelmű – csak a körszám nincs kimondva, azt a ritmus adja.
     */
    @Test public void timeDrivenFormsAreUnderstood() {
        IntervalParse.Plan hiit = IntervalParse.parse("hiit 20 perc");
        assertEquals(20, hiit.rounds);          // 20 perc / (30+30 mp)
        assertEquals(30, hiit.work);
        assertEquals(30, hiit.rest);
        assertTrue("a ritmus a mi javaslatunk", hiit.guessed);

        IntervalParse.Plan f = IntervalParse.parse("fartlek fél óra");
        assertEquals(15, f.rounds);
        assertEquals(60, f.work);

        IntervalParse.Plan e = IntervalParse.parse("e2mom 20 perc");
        assertEquals(10, e.rounds);
        assertEquals(120, e.work);
        assertEquals(0, e.rest);

        IntervalParse.Plan p = IntervalParse.parse("2 percenként 10 kör");
        assertEquals(10, p.rounds);
        assertEquals(120, p.work);

        // A kimondott számok mindig erősebbek a forma nevénél.
        IntervalParse.Plan x = IntervalParse.parse("hiit 8 kör 45 mp munka 15 mp pihenő");
        assertEquals(8, x.rounds);
        assertEquals(45, x.work);
        assertEquals(15, x.rest);
        assertTrue("kimondott szám nem javaslat", !x.guessed);
    }

    /** A csillag itt is szorzójel: „8*20/10”. */
    @Test public void asteriskWorksInIntervalsToo() {
        IntervalParse.Plan p = IntervalParse.parse("8*20/10");
        assertEquals(8, p.rounds);
        assertEquals(20, p.work);
        assertEquals(10, p.rest);
    }

    /**
     * A pihenő szakasz neve nem csak „séta" lehet.
     *
     * A futó „járás"-t, „kocogás"-t vagy „gyaloglás"-t ír a lassú szakaszra, a
     * beszélt nyelv meg „pihi"-t. Egyik sem volt a pihenő-szavak közt, így a
     * „20 mp sprint 40 mp járás 8x" pihenője elveszett: az edzés szünet
     * nélkülinek látszott, és a kör fele eltűnt.
     */
    @Test public void theRestSegmentHasManyNames() {
        assertEquals("8×20/40", sum("20 mp sprint 40 mp járás 8x"));
        assertEquals("8×20/40", sum("20 mp sprint 40 mp gyaloglás 8x"));
        assertEquals("5×300/60", sum("5 perc futás, 1 perc járás, 5x"));
        assertEquals("6×60/30", sum("6 kör 1 perc munka 30 mp pihi"));
        // A régi alakok változatlanok.
        assertEquals("8×20/40", sum("20 mp sprint 40 mp séta 8x"));
        assertEquals("8×20/10", sum("8x20/10"));
    }

    /**
     * A KEMÉNY és a KÖNNYŰ is munka és pihenő.
     *
     * A futók és a kerékpárosok így írják le a ritmust: „3 perc kemény,
     * 2 perc könnyű". Eddig ebből egyetlen, szünet nélküli munkaszakasz
     * lett – a kör fele elveszett, és az edző órája végig azt mutatta,
     * hogy hajrá.
     */
    @Test public void hardAndEasyAreWorkAndRest() {
        IntervalParse.Plan a = IntervalParse.parse("3 perc kemény 2 perc könnyű 5x");
        assertEquals(5, a.rounds);
        assertEquals(180, a.work);
        assertEquals(120, a.rest);
        IntervalParse.Plan b = IntervalParse.parse("6 kör 3 perc erős 2 perc laza");
        assertEquals(6, b.rounds);
        assertEquals(180, b.work);
        assertEquals(120, b.rest);
    }

    /**
     * A zárójeles csoport körszáma nem veszhet el.
     *
     * A „8x (30 mp munka + 30 mp pihenő)" nyolc kör – de a zárójel helyére
     * lépő szóköz miatt a szorzó elszakadt a számtól, és egyetlen kör
     * maradt belőle. Nyolcadannyi edzés, ugyanazzal a mondattal.
     */
    @Test public void aBracketedGroupKeepsItsRoundCount() {
        IntervalParse.Plan p = IntervalParse.parse("8x (30 mp munka + 30 mp pihenő)");
        assertEquals(8, p.rounds);
        assertEquals(30, p.work);
        assertEquals(30, p.rest);
    }

    /**
     * Négy óránál hosszabb edzést nem állítunk be.
     *
     * A szakaszok külön-külön hihetőek lehetnek, együtt mégsem: a „8x 60
     * perc" nyolc órás időzítő, a „8x 22:30" hat. Egy kétszázezer mondatos
     * véletlen-futtatásban pontosan ez a kilenc eset maradt – mind olyan
     * óra, ami egész nap ketyegett volna.
     */
    @Test public void noPlanRunsLongerThanFourHours() {
        assertNull(IntervalParse.parse("8x 60 perc"));
        assertNull(IntervalParse.parse("10 kör 40 perc munka 20 perc pihenő"));
        // A határon belüli hosszú edzés viszont marad.
        IntervalParse.Plan p = IntervalParse.parse("4 kör 30 perc munka 5 perc pihenő");
        assertEquals(4, p.rounds);
        assertTrue(p.totalSec() <= 4 * 3600);
        assertEquals(20, IntervalParse.parse("amrap 20 perc").work / 60);
    }
}
