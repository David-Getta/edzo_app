package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A rehab-tartalom minősége.
 *
 * A gyógytornász-ihletésű soroknál a hiányzó adagolás vagy egy üres
 * technikai tipp nem kozmetika: aki ez alapján tornázik, annak a
 * hiányos sor rossz tanács. Ezért itt minden mezőt számon kérünk.
 */
public class RehabTest {

    @Test public void everyAreaIsComplete() {
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        assertTrue("legalább nyolc testtáj", Rehab.AREAS.length >= 8);
        for (Rehab.Area a : Rehab.AREAS) {
            assertTrue(a.id, ids.add(a.id));                       // egyedi azonosító
            assertTrue(a.id, a.name.length() >= 4);
            assertTrue(a.id + ": cél", a.goal.length() >= 40);
            // A figyelmeztetés minden lapon ott van, és orvost mond.
            assertTrue(a.id + ": figyelmeztetés", a.warn.contains("orvos"));
            assertTrue(a.id + ": gyakorlatszám", a.moves.length >= 4);
            for (Rehab.Ex e : a.moves) {
                assertTrue(a.id + "/" + e.name, e.name.length() >= 4);
                // Az adagolásban szám is legyen („3×12", „2×30 mp").
                assertTrue(a.id + "/" + e.name + ": adagolás", e.dose.matches(".*\\d.*"));
                assertTrue(a.id + "/" + e.name + ": tipp", e.cue.length() >= 20);
                assertTrue(a.id + "/" + e.name + ": videó", e.video.length() >= 8);
            }
        }
    }

    @Test public void lookupAndDurationWork() {
        assertEquals("Boka-stabilitás", Rehab.byId("boka").name);
        assertNull(Rehab.byId("nincs-ilyen"));
        for (Rehab.Area a : Rehab.AREAS) {
            int m = Rehab.minutesOf(a);
            assertTrue(a.id + ": perc", m >= 10 && m <= 20);
        }
    }

    /**
     * A kulcs-területek, amiket a sor eredetileg megcélzott, tényleg ott
     * vannak: boka-stabilitás, váll, és a KÉT könyök (golf- és teniszkönyök).
     */
    @Test public void theRequestedAreasExist() {
        assertTrue(Rehab.byId("boka") != null);
        assertTrue(Rehab.byId("vall") != null);
        assertTrue(Rehab.byId("konyok-belso") != null);
        assertTrue(Rehab.byId("konyok-kulso") != null);
        assertTrue(Rehab.byId("derek") != null);
        // A belső könyök sora excentrikus munkát tartalmaz – ez a lényege.
        boolean ecc = false;
        for (Rehab.Ex e : Rehab.byId("konyok-belso").moves)
            if (e.name.toLowerCase(new java.util.Locale("hu")).contains("excentrikus")) ecc = true;
        assertTrue("excentrikus gyakorlat a golfkönyöknél", ecc);
    }
}
