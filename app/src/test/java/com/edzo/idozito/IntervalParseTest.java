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
}
