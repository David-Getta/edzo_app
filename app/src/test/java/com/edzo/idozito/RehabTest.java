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

    /**
     * Panaszból testtáj: a „fáj a vállam" a váll-sort kapja.
     *
     * A panasz az edzés-felismerő ELÉ kerül az útbaigazítóban: a „fáj a
     * térdem futás után" és a „golfkönyök fájdalom" nem edzés – hiába van
     * benne sportszó. Fájdalom-szó nélkül viszont az ág nem élhet: a
     * „vállból nyomás" gyakorlat, a „vállnap volt" edzés.
     */
    @Test public void complaintsFindTheirArea() {
        assertEquals("vall", Rehab.forComplaint("fáj a vállam").id);
        assertEquals("terd", Rehab.forComplaint("fáj a térdem futás után").id);
        assertEquals("boka", Rehab.forComplaint("kificamodott a bokám").id);
        assertEquals("konyok-belso", Rehab.forComplaint("golfkönyök fájdalom").id);
        assertEquals("konyok-kulso", Rehab.forComplaint("fáj a könyököm").id);
        assertEquals("derek", Rehab.forComplaint("derekam fáj reggel").id);
        assertEquals("comb", Rehab.forComplaint("húzódik a combom").id);
        assertEquals("achilles", Rehab.forComplaint("gyulladt az achilles inam").id);
        assertNull(Rehab.forComplaint("vállból nyomás 3x10"));
        assertNull(Rehab.forComplaint("vállnap volt"));
        assertNull(Rehab.forComplaint("30 perc futás"));
        assertNull(Rehab.forComplaint("fáj a fejem"));
        assertNull(Rehab.forComplaint(null));
        // A fájdalom-szó nélküli panasz-igék is: beállt, megrándult, sajog.
        assertEquals("derek", Rehab.forComplaint("beállt a derekam").id);
        assertEquals("boka", Rehab.forComplaint("megrándult a bokám").id);
        assertEquals("terd", Rehab.forComplaint("sajog a térdem").id);
        assertEquals("konyok-kulso", Rehab.forComplaint("fáj a csuklóm").id);
        // A zsibbadás piros zászló: arra nem sort ajánlunk, hanem hallgatunk.
        assertNull(Rehab.forComplaint("zsibbad a karom"));
        // A tagadott panasz jó hír, nem kérés.
        assertNull(Rehab.forComplaint("nem fáj a vállam"));
        assertNull(Rehab.forComplaint("már nem fáj a térdem"));
        assertNull(Rehab.forComplaint("nem fájt a bokám edzés után"));
    }

    /** Az útbaigazító is a rehabhoz küldi – az edzés-felismerő előtt. */
    @Test public void theRouterPrefersTheComplaint() {
        assertEquals(Sentence.Kind.REHAB,
                Sentence.of("fáj a térdem futás után", null, 1_753_869_600_000L));
        assertEquals(Sentence.Kind.REHAB,
                Sentence.of("golfkönyök fájdalom", null, 1_753_869_600_000L));
        assertEquals(Sentence.Kind.WORKOUT,
                Sentence.of("30 perc futás", null, 1_753_869_600_000L));
        // A birtokos comb a saját láb, nem csirkecomb.
        assertEquals(Sentence.Kind.REHAB,
                Sentence.of("húzódik a combom",
                        java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
    }

    /**
     * A cél-mondat is ajtó: „boka stabilitás" – nem kell megvárni, hogy fájjon.
     *
     * Szándék-szó ÉS testtáj kell hozzá; az „erősítés" szándékosan nem
     * szándék-szó, mert a „váll erősítés" a konditerem mondata.
     */
    @Test public void goalSentencesFindTheirArea() {
        assertEquals("boka", Rehab.forGoal("boka stabilitás").id);
        assertEquals("boka", Rehab.forGoal("bokastabilitásra szeretnék edzeni").id);
        assertEquals("vall", Rehab.forGoal("váll mobilizálás").id);
        assertEquals("derek", Rehab.forGoal("derék rehab").id);
        assertEquals("derek", Rehab.forGoal("gerinc mobilizálás").id);
        assertEquals("nyak", Rehab.forGoal("nyak gyógytorna").id);
        assertEquals("achilles", Rehab.forGoal("achilles megelőzés").id);
        assertNull(Rehab.forGoal("core stabilitás"));   // nincs testtáj
        assertNull(Rehab.forGoal("váll erősítés"));     // konditermi mondat
        assertNull(Rehab.forGoal("boka 3x10"));         // nincs szándék-szó
        assertNull(Rehab.forGoal("30 perc futás"));
        assertNull(Rehab.forGoal(""));
        assertNull(Rehab.forGoal(null));
        // A „csípős" étel és a „vállal" ige nem testtáj.
        assertNull(Rehab.forComplaint("fáj a hasam a csípős kajától"));
        assertNull(Rehab.forComplaint("sokat vállaltam és fáj a fejem"));
        assertEquals(Sentence.Kind.MEAL,
                Sentence.of("csípős csirkeszárny sült krumplival",
                        java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
        // Az útbaigazító is idehozza.
        assertEquals(Sentence.Kind.REHAB,
                Sentence.of("boka stabilitás", null, 1_753_869_600_000L));
        assertEquals(Sentence.Kind.REHAB,
                Sentence.of("váll mobilizálás",
                        java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
    }

    /**
     * A heti fókusz számlálója csak a mostani hetet számolja.
     *
     * Hétfő 0:00 a határ: a vasárnapi alkalom nem hozható át, a jövőbeli
     * időbélyeg (elállított óra) pedig nem ír jóvá előre semmit.
     */
    @Test public void theWeekCounterCountsOnlyThisWeek() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(2026, java.util.Calendar.AUGUST, 7, 12, 0, 0); // péntek dél
        c.set(java.util.Calendar.MILLISECOND, 0);
        long now = c.getTimeInMillis();
        long day = 24L * 3600 * 1000;
        // Szerda és hétfő számít; a múlt vasárnap és a holnap nem.
        assertEquals(2, Rehab.weekCount(
                new long[]{now - 2 * day, now - 4 * day, now - 5 * day, now + day}, now));
        assertEquals(0, Rehab.weekCount(new long[]{}, now));
        assertEquals(0, Rehab.weekCount(null, now));
        // Hétfő délben a hajnali alkalom már e heti.
        long monday = now - 4 * day;
        assertEquals(1, Rehab.weekCount(new long[]{monday - 6 * 3600 * 1000}, monday));
    }

    /** A fókusz-sor kimondja az állást, és a kész hétre pipát tesz. */
    @Test public void theFocusLineShowsProgress() {
        Rehab.Area a = Rehab.byId("boka");
        assertTrue(Rehab.focusLine(a, 0).contains("0/" + Rehab.WEEKLY_GOAL));
        assertTrue(Rehab.focusLine(a, 1).contains("1/" + Rehab.WEEKLY_GOAL));
        assertTrue(Rehab.focusLine(a, 1).contains(a.name));
        assertTrue(Rehab.focusLine(a, Rehab.WEEKLY_GOAL).contains("✔"));
        assertTrue(Rehab.focusLine(a, Rehab.WEEKLY_GOAL + 2).contains("✔"));
    }
}
