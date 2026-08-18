package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        // A csuklónak saját sora van (mobilitás + kis súlyos terhelés).
        assertEquals("csuklo", Rehab.forComplaint("fáj a csuklóm").id);
        assertEquals("konyok-kulso", Rehab.forComplaint("teniszkönyök fájdalom").id);
        // A térd külső oldala az IT-szalag sora, az elülső a térd-soré.
        assertEquals("itszalag", Rehab.forComplaint("fáj a térdem külső oldala").id);
        assertEquals("itszalag", Rehab.forComplaint("IT szalag fájdalom").id);
        assertEquals("terd", Rehab.forComplaint("fáj a térdem").id);
        // Az új futó-területek: talp és sípcsont.
        assertEquals("talp", Rehab.forComplaint("fáj a talpam reggelente").id);
        assertEquals("sipcsont", Rehab.forComplaint("fáj a sípcsontom futás után").id);
        // A sarok továbbra is az Achilles-sor felé megy.
        assertEquals("achilles", Rehab.forComplaint("fáj a sarkam").id);
        // A zsibbadás piros zászló: arra nem sort ajánlunk, hanem
        // figyelmeztetést – lásd redFlagsGetAnAnswerButNoExercises.
        assertNull(Rehab.forComplaint("zsibbad a karom"));
        // A tagadott panasz jó hír, nem kérés.
        assertNull(Rehab.forComplaint("nem fáj a vállam"));
        assertNull(Rehab.forComplaint("már nem fáj a térdem"));
        assertNull(Rehab.forComplaint("nem fájt a bokám edzés után"));
    }

    /**
     * A magyar egy szóban is elmondja, mi fáj – és a diagnózis neve is panasz.
     *
     * A „derékfájás" és a „csípőfájdalom" ÖSSZETETT szó: a fájdalom-tő a szó
     * belsejébe esik, és a szókezdet-vizsgálat mindet elutasította. A
     * „golfkönyök" pedig magában is kérés: aki ezt írja be, nem érdeklődik,
     * hanem fáj neki. Mindegyikre üres válasz jött, pedig a lap pont róluk
     * szól.
     */
    @Test public void compoundComplaintsAndDiagnosisNamesAreComplaints() {
        assertEquals("derek", Rehab.forComplaint("derékfájás").id);
        assertEquals("nyak", Rehab.forComplaint("nyakfájás").id);
        assertEquals("csipo", Rehab.forComplaint("csípőfájdalom").id);
        assertEquals("boka", Rehab.forComplaint("bokafájdalom").id);
        assertEquals("vall", Rehab.forComplaint("vállfájás").id);
        assertEquals("derek", Rehab.forComplaint("fáj a hátam").id);
        // A diagnózis neve magában is panasz.
        assertEquals("konyok-belso", Rehab.forComplaint("golfkönyök").id);
        assertEquals("konyok-kulso", Rehab.forComplaint("teniszkönyök").id);
        assertEquals("itszalag", Rehab.forComplaint("futótérd").id);
        // A sarkantyú a TALP sora: a talpi szalagot kell terhelni hozzá.
        assertEquals("talp", Rehab.forComplaint("sarkantyú").id);
        // Ami nem a mi testtájunk, arra továbbra sincs sor.
        assertNull(Rehab.forComplaint("fejfájás"));
        assertNull(Rehab.forComplaint("hasfájás"));
        assertNull(Rehab.forComplaint("torokfájás"));
        // És a nem-panasz mondat sem lesz az.
        assertNull(Rehab.forComplaint("hátnap"));
        assertNull(Rehab.forComplaint("háton úszás 30 perc"));
    }

    /**
     * A piros zászlós panaszra válasz jár – csak nem gyakorlatsor.
     *
     * A „zsibbad a kezem" mondatra az app eddig HALLGATOTT: sort nem
     * akartunk ajánlani rá, tehát semmi nem jött vissza. A hallgatás
     * viszont azt üzeni, hogy nem értjük – pedig pont hogy értjük, és
     * éppen ezért nem tornáztatunk. Mostantól megmondjuk, miért.
     */
    @Test public void redFlagsGetAnAnswerButNoExercises() {
        for (String q : new String[]{"zsibbad a kezem", "elzsibbadt a lábam",
                "bedagadt a bokám", "éjszaka fáj a vállam",
                "a lábamba sugárzik a fájdalom", "nem tudok rálépni a bokámra"}) {
            assertNotNull("nincs figyelmeztetés: " + q, Rehab.redFlag(q));
            // Sort viszont nem ajánlunk rá.
            assertNull("sort ajánlott rá: " + q, Rehab.forComplaint(q));
            // De az útbaigazító idehozza – nem hagyjuk válasz nélkül.
            assertEquals(q, Sentence.Kind.REHAB,
                    Sentence.of(q, java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
        }
        // A MELLKASI panasz külön ág: ott nem időpontot kell kérni, hanem
        // abbahagyni a mozgást és azonnal segítséget kérni.
        for (String q : new String[]{"erős fájdalom a mellkasomban futás közben",
                "szorít a mellkasom edzés közben", "fáj a mellkasom",
                "elájultam edzés közben"}) {
            String w = Rehab.redFlag(q);
            assertNotNull("nincs figyelmeztetés: " + q, w);
            assertTrue("nem sürgős a szöveg: " + q, w.contains("112"));
            assertNull("sort ajánlott rá: " + q, Rehab.forComplaint(q));
        }
        // A hétköznapi panasz továbbra is sort kap, nem figyelmeztetést.
        assertNull(Rehab.redFlag("fáj a vállam"));
        assertNull(Rehab.redFlag("derékfájás"));
        assertNull(Rehab.redFlag(null));
        assertEquals("vall", Rehab.forComplaint("fáj a vállam").id);
        // Az elmúlt jel sem zászló.
        assertNull(Rehab.redFlag("már nem zsibbad a kezem"));
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
        assertEquals("terd", Rehab.forGoal("térd stabilizálás").id);
        assertEquals("comb", Rehab.forGoal("comb rehab").id);
        // A lábszár ugyanaz a panasz, csak hétköznapibb néven – a kezdő
        // futók leggyakoribb baja eddig válasz nélkül maradt.
        assertEquals("sipcsont", Rehab.forComplaint("lábszárfájás futás után").id);
        assertEquals("sipcsont", Rehab.forComplaint("fáj a lábszáram futás közben").id);
        // Az INSTABILITÁS és a RÖVIDÜLÉS is panasz, fájdalom-szó nélkül –
        // pedig pont ezekre való a megelőző sor.
        assertEquals("boka", Rehab.forComplaint("a bokám gyakran kifordul futás közben").id);
        assertEquals("terd", Rehab.forComplaint("instabil a térdem oldalra").id);
        assertEquals("comb", Rehab.forComplaint("rövid a combhajlítóm").id);
        // Harmadik személyű alak és a panasz FŐNEVE: a „már egy hete húzza a
        // vállam" és a „megint bejött a régi térdproblémám" eddig válasz
        // nélkül maradt.
        assertEquals("vall", Rehab.forComplaint("már egy hete húzza a vállam").id);
        assertEquals("terd", Rehab.forComplaint("megint bejött a régi térdproblémám").id);
        assertEquals("vall", Rehab.forComplaint("krónikus a vállpanaszom").id);
        // Ahogy segítséget kérünk: a kimondott kérés mellett a nyújtás is
        // szándék-szó, a farizom pedig a csípő ügye.
        assertEquals("csipo", Rehab.forGoal("erősíteni kéne a farizmom").id);
        assertEquals("comb", Rehab.forGoal("nyújtani kéne a combhajlítót").id);
        assertEquals("talp", Rehab.forGoal("kell valami a talpamra").id);
        assertEquals("terd", Rehab.forGoal("hogyan erősítsem a térdem").id);
        // A puszta nyújtás továbbra sem cél – az naplózható mozgásforma.
        assertNull(Rehab.forGoal("45 perc nyújtás"));
        assertNull(Rehab.forGoal("combnyújtás 30 perc"));
        // A „háti" jelzőként is egyértelmű – így kéri, aki egész nap ül.
        assertEquals("hati", Rehab.forGoal("kéne valami háti gyakorlat").id);
        assertEquals("hati", Rehab.forGoal("háti mobilizálás kellene").id);
        // De a hátizsák nem a háti gerinc.
        assertNull(Rehab.forGoal("hátizsákos gyakorlatok"));
        assertEquals("csuklo", Rehab.forGoal("csukló mobilizálás").id);
        // A csirkecomb cél-mondatban sem testtáj.
        assertNull(Rehab.forGoal("csirkecomb rehab"));
        // A CORE-nak mostantól saját lapja van: a mély stabilizálók a derék,
        // a csípő és a térd panaszainak közös alapja.
        assertEquals("torzs", Rehab.forGoal("core stabilitás").id);
        assertEquals("torzs", Rehab.forGoal("mélyizom erősítés").id);
        // Az ERŐSÍTÉS is ide tartozik: a „boka erősítés" korábban egy
        // hatvanperces kondi-BEJEGYZÉS lett a naplóban – vagyis egy meg nem
        // történt edzés. A súlyzós mondat továbbra is az erősítő naplóé.
        assertEquals("vall", Rehab.forGoal("váll erősítés").id);
        assertEquals("boka", Rehab.forGoal("boka erősítés").id);
        assertEquals(Sentence.Kind.STRENGTH,
                Sentence.of("vállból nyomás 3x10 20 kg",
                        java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
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

    /**
     * A vezetett sor a kétoldalas gyakorlatot bal/jobb bontásban mondja,
     * és a kör-szám úgy áll be, hogy a sor a keretben maradjon.
     */
    @Test public void theGuidedListSplitsSidedExercisesAndFitsTheFrame() {
        for (Rehab.Area a : Rehab.AREAS) {
            java.util.List<String> names = Rehab.guidedNames(a);
            int rounds = Rehab.guidedRounds(a);
            assertTrue("kevés név: " + a.id, names.size() >= a.moves.length);
            assertTrue("kör-szám: " + a.id, rounds == 2 || rounds == 3);
            // 40 mp munka + 8 mp pihenő ablakonként: 8 és 25 perc között.
            int total = rounds * names.size() * 48;
            assertTrue("kicsúszik a keretből: " + a.id + " → " + total / 60 + " perc",
                    total >= 8 * 60 && total <= 25 * 60);
            // A bontott nevek párban járnak, és kimondják az oldalt.
            for (String n : names)
                if (n.endsWith(" – bal"))
                    assertTrue("hiányzó pár: " + n, names.contains(
                            n.substring(0, n.length() - 6) + " – jobb"));
        }
        // A boka-sor egylábas gyakorlatai tényleg bontva mennek.
        java.util.List<String> boka = Rehab.guidedNames(Rehab.byId("boka"));
        assertTrue(boka.contains("Egylábas állás – bal"));
        assertTrue(boka.contains("Egylábas állás – jobb"));
    }

    /**
     * A heti sorozat: hány egymást követő héten jött össze a heti adag.
     *
     * A folyamatban lévő hét csak készen számít bele; egy kihagyott hét
     * megszakítja a sorozatot.
     */
    @Test public void theWeeklyStreakCountsCompleteWeeks() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(2026, java.util.Calendar.AUGUST, 7, 12, 0, 0); // péntek dél
        c.set(java.util.Calendar.MILLISECOND, 0);
        long now = c.getTimeInMillis();
        long day = 24L * 3600 * 1000;
        long week = 7 * day;
        // Múlt hét: hétfő aug 3 előtti hét (júl 27–aug 2), 3 alkalom.
        long lastMon = now - 4 * day - week; // júl 27, péntek délből számolva
        long[] lastFull = {lastMon, lastMon + day, lastMon + 3 * day};
        assertEquals(1, Rehab.weekStreak(lastFull, now));
        // Két teljes hét egymás után.
        long[] two = {lastMon, lastMon + day, lastMon + 3 * day,
                lastMon - week, lastMon - week + day, lastMon - week + 2 * day};
        assertEquals(2, Rehab.weekStreak(two, now));
        // Az e heti kész adag hozzáadja a folyó hetet is.
        long[] three = {now, now - day, now - 2 * day,
                lastMon, lastMon + day, lastMon + 3 * day};
        assertEquals(2, Rehab.weekStreak(three, now));
        // Kihagyott hét megszakítja: a két héttel ezelőtti teljes hét nem számít.
        long[] gap = {lastMon - 2 * week, lastMon - 2 * week + day,
                lastMon - 2 * week + 2 * day};
        assertEquals(0, Rehab.weekStreak(gap, now));
        assertEquals(0, Rehab.weekStreak(new long[]{}, now));
        assertEquals(0, Rehab.weekStreak(null, now));
        // Két e heti alkalom még nem kész hét.
        assertEquals(0, Rehab.weekStreak(new long[]{now, now - day}, now));
    }

    /**
     * A fájdalom-napló iránya: javul, romlik, vagy áll.
     *
     * Kevés adatból nem mondunk trendet, az erős friss érték pedig
     * felülír mindent – ott nem a görbe a hír, hanem hogy szakember kell.
     */
    @Test public void thePainLineReadsTheDirection() {
        assertEquals("", Rehab.painLine(null));
        assertEquals("", Rehab.painLine(new int[0]));
        // Kevés adat: az érték igen, az irány még nem.
        assertTrue(Rehab.painLine(new int[]{4}).contains("4/10"));
        assertTrue(Rehab.painLine(new int[]{4}).contains("közepes"));
        assertTrue(Rehab.painLine(new int[]{4, 5, 6}).contains("irány"));
        // Javuló sor (legfrissebb elöl): 2,2,1 most – 6,6,5 régen.
        String jav = Rehab.painLine(new int[]{2, 2, 1, 5, 6, 6, 5});
        assertTrue("nem látja a javulást: " + jav, jav.contains("javul"));
        // Romló sor.
        String rossz = Rehab.painLine(new int[]{6, 6, 5, 2, 2, 1, 2});
        assertTrue("nem látja a romlást: " + rossz, rossz.contains("rosszabbodik"));
        // Egy helyben álló sor.
        String all = Rehab.painLine(new int[]{4, 4, 4, 4, 4, 4});
        assertTrue("irányt lát, ahol nincs: " + all, all.contains("nem sokat mozdult"));
        // Az erős friss érték mindent felülír.
        String eros = Rehab.painLine(new int[]{9, 2, 2, 1, 1, 1});
        assertTrue("nem szól a szakemberért: " + eros, eros.contains("gyógytorná"));
    }

    /**
     * A mondat maga is mondhatja a skálát: „fáj a vállam 6/10".
     *
     * A sorozat-alak („3x10") és a súly nem fájdalom – onnan nem szabad
     * számot hozni, mert az csendben hamis bejegyzést csinálna.
     */
    @Test public void theSentenceCanCarryTheScale() {
        assertEquals(6, Rehab.painIn("fáj a vállam 6/10"));
        assertEquals(0, Rehab.painIn("ma 0/10, semmi panasz"));
        assertEquals(10, Rehab.painIn("10/10 fájdalom"));
        assertEquals(7, Rehab.painIn("fájdalom: 7"));
        assertEquals(4, Rehab.painIn("4-es fájdalom a térdemben"));
        assertEquals(-1, Rehab.painIn("fáj a vállam"));
        assertEquals(-1, Rehab.painIn("guggolás 3x10 60 kg"));
        assertEquals(-1, Rehab.painIn("15/10"));
        assertEquals(-1, Rehab.painIn(""));
        assertEquals(-1, Rehab.painIn(null));
    }

    /** A skála szavakban is olvasható – a puszta szám semmit nem mond. */
    @Test public void thePainScaleHasWords() {
        assertEquals("nincs fájdalom", Rehab.painWord(0));
        assertEquals("enyhe", Rehab.painWord(2));
        assertEquals("közepes", Rehab.painWord(5));
        assertEquals("erős", Rehab.painWord(9));
    }

    /**
     * A „mit csináljak ma?" ajánlás: fókusz, amíg tart, aztán a legrégebbi.
     *
     * A lista maga nem segítség annak, aki fáradtan nyitja meg – egy javaslat
     * viszont igen. Ha még soha semmit nem csinált, nincs mit ajánlani.
     */
    @Test public void theNextAreaSuggestionPicksTheDueOne() {
        String[] ids = {"boka", "vall", "derek"};
        long now = 1_753_869_600_000L;
        long day = 24L * 3600 * 1000;
        long[] last = {now - day, now - 10 * day, now - 3 * day};
        // Fókusz, amíg nincs meg a heti adag.
        assertEquals("boka", Rehab.nextArea("boka", 1, ids, last));
        assertEquals("boka", Rehab.nextArea("boka", Rehab.WEEKLY_GOAL - 1, ids, last));
        // Kész hét után a legrégebben csinált sor jön.
        assertEquals("vall", Rehab.nextArea("boka", Rehab.WEEKLY_GOAL, ids, last));
        assertEquals("vall", Rehab.nextArea(null, 0, ids, last));
        // Ismeretlen fókusz-azonosító nem téríti el.
        assertEquals("vall", Rehab.nextArea("nincsilyen", 0, ids, last));
        // Amit soha nem csinált, azt nem ajánljuk – és üresből nincs javaslat.
        assertNull(Rehab.nextArea(null, 0, ids, new long[]{0, 0, 0}));
        assertNull(Rehab.nextArea(null, 0, null, null));
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

    /**
     * Minden területnek van saját „következő szintje".
     *
     * A fokozatosság a rehab lelke, de az „emelj az adagoláson" tanács
     * személytelen: a bokánál a következő lépés nem több ismétlés, hanem az
     * instabil felület és az ugrás; a golfkönyöknél nehezebb súly, lassabb
     * leengedés. Ha egy terület kimaradna a táblából, a lap némán a régi,
     * általános mondatra esne vissza.
     */
    @Test public void everyAreaHasANextLevel() {
        for (Rehab.Area a : Rehab.AREAS) {
            String nx = Rehab.nextLevel(a.id);
            assertTrue(a.id + ": nincs következő szint", nx.length() >= 60);
            // Konkrét legyen: szám vagy mértékegység is szerepeljen benne.
            assertTrue(a.id + ": nem konkrét", nx.matches(".*\\d.*"));
        }
        assertEquals("", Rehab.nextLevel("nincs-ilyen"));
    }

    /**
     * Minden területnek van „mikorra várható javulás" mondata.
     *
     * A gyógytornász első mondata mindig ez, és pont ez hiányzott: aki két
     * nap után nem érez semmit, abbahagyja – pedig az ín-panaszok (golfkönyök,
     * Achilles, talp) hetekben mérhetők, nem napokban.
     */
    @Test public void everyAreaSaysWhenToExpectResults() {
        for (Rehab.Area a : Rehab.AREAS) {
            String e = Rehab.expected(a.id);
            assertTrue(a.id + ": nincs időtáv", e.length() >= 60);
            // Konkrét hetekben legyen megadva.
            assertTrue(a.id + ": nincs benne hét", e.contains("hét"));
            assertTrue(a.id + ": nincs benne szám", e.matches(".*\\d.*"));
        }
        assertEquals("", Rehab.expected("nincs-ilyen"));
    }

    /**
     * A vezetett mód gombjára írt idő IGAZ.
     *
     * A lap fejlécén a gyakorlatok adagolásából becsült idő áll, a vezetett
     * mód viszont fix ablakokkal dolgozik – a kettő nem ugyanaz. Ha a gombra
     * írt szám nem igaz, az a legrosszabb fajta apró hazugság: az ember
     * beosztja rá az idejét.
     */
    @Test public void theGuidedButtonTellsTheRealLength() {
        for (Rehab.Area a : Rehab.AREAS) {
            int items = Rehab.guidedNames(a).size() * Rehab.guidedRounds(a);
            int sec = Rehab.GUIDED_PREP + items * (Rehab.GUIDED_WORK + Rehab.GUIDED_REST);
            assertEquals(a.id, Math.round(sec / 60.0), Rehab.guidedMinutes(a));
            // És a keret is tartható marad.
            assertTrue(a.id + ": túl hosszú vezetett sor (" + Rehab.guidedMinutes(a) + " perc)",
                    Rehab.guidedMinutes(a) >= 8 && Rehab.guidedMinutes(a) <= 20);
        }
    }

    /**
     * A magyar nem csak azt mondja, hogy „fáj".
     *
     * A „húz a vádlim", a „görcsöl a lábam", a „szúr a derekam" és a
     * reggeli „elgémberedik a kezem" ugyanolyan panasz – eddig mindegyikre
     * az jött, hogy nem értjük. A legrövidebbek (húz, szúr) csak egész
     * szóként számítanak: a húzódzkodás nem panasz.
     */
    @Test public void everydayComplaintVerbsAreUnderstood() {
        assertNotNull("húz a vádlim", Rehab.forComplaint("húz a vádlim"));
        assertNotNull("görcsöl a vádlim", Rehab.forComplaint("görcsöl a vádlim"));
        assertNotNull("szúr a derekam", Rehab.forComplaint("szúr a derekam"));
        assertNotNull("feszül a nyakam", Rehab.forComplaint("feszül a nyakam"));
        assertNotNull("bemerevedett a vállam", Rehab.forComplaint("bemerevedett a vállam"));
        assertNotNull("elgémberedik a kezem",
                Rehab.forComplaint("reggelre elgémberedik a kezem"));
        assertNotNull("gerincferdülés", Rehab.forComplaint("gerincferdülésem van"));
        // A HANG is panasz: „ropog a térdem", „recseg a vállam", „kattog a
        // csípőm". A térd lapja épp azt mondja ki, hogy a kattogás önmagában
        // nem baj – de ezt hallania kell tőlünk, nem egy „nem értem"-et.
        assertNotNull("ropog", Rehab.forComplaint("ropog a térdem guggolásnál"));
        assertNotNull("recseg", Rehab.forComplaint("recseg a vállam körzésnél"));
        assertNotNull("kattog", Rehab.forComplaint("kattog a csípőm"));
        assertNotNull("húzódott", Rehab.forComplaint("húzódott a hátsó combom sprintnél"));
        // A ROPOGÓS étel viszont nem panasz.
        assertNull(Rehab.forComplaint("ropogós csirkecomb sült krumplival"));
        // A kéz panasza a csukló sorára fut.
        assertEquals("csuklo", Rehab.forComplaint("fáj a kezem").id);
        assertEquals("csuklo", Rehab.forComplaint("merevek az ujjaim").id);
        // A gyakorlat neve nem panasz, hiába lakik benne a „húz".
        assertNull(Rehab.forComplaint("húzódzkodás 3x8"));
        assertNull(Rehab.forComplaint("lehúzás 4x12 50 kg"));
        assertNull(Rehab.forComplaint("húzódzkodás és vállból nyomás"));
    }

    /**
     * A háti gerincnek saját sora van – nem a derékkal közös.
     *
     * Az ülőmunka a HÁTI (mellkasi) szakaszt merevíti be, és a derék-sor
     * (madár-kutya, curl-up, oldalplank) erről nem szól. A merev háti
     * szakasz árát ráadásul a nyak és a váll fizeti meg: oda vándorol a
     * mozgás, ami innen hiányzik.
     */
    @Test public void theThoracicSpineHasItsOwnRow() {
        assertEquals("hati", Rehab.forComplaint("merev a felső hátam").id);
        assertEquals("hati", Rehab.forComplaint("fáj a lapockám között").id);
        assertEquals("hati", Rehab.forComplaint("görbe a hátam az ülőmunkától").id);
        assertEquals("hati", Rehab.forGoal("háti gerinc mobilizálás").id);
        // A puszta „hátam" viszont marad a deréknál: magyarul az a leggyakoribb
        // megnevezése az ágyéki panasznak.
        assertEquals("derek", Rehab.forComplaint("fáj a hátam").id);
        assertEquals("derek", Rehab.forComplaint("beállt a derekam").id);
        // A sor tartalma is a háti szakaszról szól, nem a derékról.
        Rehab.Area a = Rehab.byId("hati");
        assertNotNull(a);
        assertTrue("kevés gyakorlat", a.moves.length >= 5);
        String goal = a.goal.toLowerCase(new java.util.Locale("hu"));
        assertTrue("a leírás nem mondja ki, hova vándorol a hiányzó mozgás",
                goal.contains("nyak") && goal.contains("váll"));
    }

    /**
     * A „faj" szótő hétköznapi álruhái nem panaszok.
     *
     * Ötvenezer magyar szót a testtájak mellé téve a fájl, a fajta, a faji
     * és a fajok maradtak: mind a szó elején hordozza a „faj"-t, akárcsak a
     * „fáj". A „milyen fajta nyújtás a vállamra" így panasz lett.
     */
    @Test public void everydayWordsHidingThePainStemAreNotComplaints() {
        assertNull(Rehab.forComplaint("milyen fajta nyújtás a vállamra"));
        assertNull(Rehab.forComplaint("a fájl a vállamon"));
        assertNull(Rehab.forComplaint("fajok a térdem körül"));
        // A jó hír sem panasz.
        assertNull(Rehab.forComplaint("fájdalommentes a vállam"));
        assertNull(Rehab.forComplaint("fájdalom nélkül mozog a vállam"));
        assertNull(Rehab.forComplaint("már nem fáj a vállam"));
        // A valódi panasz változatlan.
        assertNotNull(Rehab.forComplaint("fáj a vállam"));
        assertNotNull(Rehab.forComplaint("fájás a derekamban"));
        assertNotNull(Rehab.forComplaint("derékfájás"));
    }

    /**
     * Ahogy az ember tényleg kéri a gyakorlatsort.
     *
     * A „váll gyakorlatok", a „mit csináljak a vállamra", a „nyak lazítás"
     * és a „boka erősítés" mind ugyanazt kérdezi – és eddig egyikre sem jött
     * válasz. A „boka erősítés" ráadásul hatvanperces kondi-BEJEGYZÉS lett a
     * naplóban: egy meg nem történt edzés.
     */
    @Test public void theEverydayWaysOfAskingForASequence() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        long now = 1_753_869_600_000L;
        for (String q : new String[]{"it-szalag rehab", "váll gyakorlatok",
                "mit csináljak a vállamra", "gyakorlat a derekamra", "boka erősítés",
                "nyak lazítás", "váll bemelegítés", "mit ajánlasz a bokámra"})
            assertEquals(q, Sentence.Kind.REHAB, Sentence.of(q, all, now));
        // A többi ajtó változatlan – a szándék-szó testtájnév nélkül semmi.
        assertEquals(Sentence.Kind.STRENGTH, Sentence.of("3x10 fekvenyomás 60 kg", all, now));
        assertEquals(Sentence.Kind.INTERVAL,
                Sentence.of("2 perc bemelegítés, 6 kör 40/20", all, now));
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("45 perc nyújtás", all, now));
        assertEquals(Sentence.Kind.ROUTINE,
                Sentence.of("Lábnap: guggolás, lábtolás, kitörés", all, now));
        assertNull(Rehab.forGoal("bemelegítés 10 perc"));
        assertNull(Rehab.forGoal("gyakorlatok"));
        // A NYÚJTÁS önálló, naplózható mozgásforma – nem cél-szó.
        assertNull(Rehab.forGoal("combnyújtás 30 perc"));
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("45 perc nyújtás", all, now));
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("combnyújtás 30 perc", all, now));
    }

    /**
     * MINDEN testtáj elérhető egy hétköznapi panasz-mondattal.
     *
     * Egy gyakorlatsor, amit senki nem talál meg, nem létezik. A táblázat
     * kötelezi a jövőt is: új testtájhoz ide kell írni egy mondatot, amivel
     * az ember tényleg megszólítaná – és ha egy szótő-szigorítás elvágná az
     * utat, az itt derül ki, nem a felhasználónál.
     */
    @Test public void everyAreaIsReachableByAPlainComplaint() {
        String[][] t = {
                {"boka", "fáj a bokám"}, {"terd", "fáj a térdem"},
                {"itszalag", "fáj a térdem külső oldala"}, {"derek", "fáj a derekam"},
                {"vall", "fáj a vállam"}, {"konyok-belso", "golfkönyök fájdalom"},
                {"konyok-kulso", "teniszkönyök"}, {"csuklo", "fáj a csuklóm"},
                {"nyak", "fáj a nyakam"}, {"csipo", "fáj a csípőm"},
                {"achilles", "fáj az Achillesem"}, {"talp", "fáj a talpam"},
                {"sipcsont", "fáj a sípcsontom"}, {"comb", "húzódik a combom"},
                {"hati", "merev a felső hátam"},
                {"torzs", "gyenge a törzsizmom"}};
        StringBuilder bad = new StringBuilder();
        for (String[] r : t) {
            Rehab.Area c = Rehab.forComplaint(r[1]);
            String got = c == null ? "-" : c.id;
            if (!got.equals(r[0]))
                bad.append("\n  ").append(r[1]).append(" -> ").append(got)
                   .append(" (várt: ").append(r[0]).append(")");
        }
        assertEquals("elérhetetlen testtáj:" + bad, 0, bad.length());
        // És fordítva: minden területhez tartozzon próba-mondat.
        for (Rehab.Area ar : Rehab.AREAS) {
            boolean covered = false;
            for (String[] r : t) if (r[0].equals(ar.id)) covered = true;
            assertTrue("nincs próba-mondat ehhez a testtájhoz: " + ar.id, covered);
        }
    }

    /**
     * A cél melléknévvel, a panasz a reggeli első lépéssel.
     *
     * Az „erősebb bokát szeretnék" ugyanaz a kérés, mint a „boka
     * stabilitás", csak melléknévvel – és eddig egyikre sem jött válasz. A
     * „jobb tartás" a háti szakasz ügye. A reggeli ELSŐ LÉPÉS fájdalma pedig
     * a talpi ín klasszikus jele, nem az Achillesé: a puszta sarok-szó
     * viszont az Achilleshez visz, ezért a pontosabb megfogalmazás nyer.
     */
    @Test public void adjectiveGoalsAndTheFirstStepOfTheMorning() {
        assertEquals("boka", Rehab.forGoal("erősebb bokát szeretnék").id);
        assertEquals("terd", Rehab.forGoal("stabilabb térdet akarok").id);
        assertEquals("csipo", Rehab.forGoal("mozgékonyabb csípő kellene").id);
        assertEquals("hati", Rehab.forGoal("jobb tartás").id);
        assertEquals("talp", Rehab.forComplaint("reggel az első lépés fáj a sarkamban").id);
        assertEquals("talp", Rehab.forComplaint("első lépésnél fáj a talpam").id);
        // A sarok magában továbbra is az Achillesé.
        assertEquals("achilles", Rehab.forComplaint("fáj a sarkam").id);
        // A tartásos GYAKORLAT viszont az erősítő naplóé.
        assertEquals(Sentence.Kind.STRENGTH, Sentence.of("plank tartás 3x60",
                java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
    }

    /**
     * A szédülés és a hányinger edzés körül keringési jel.
     *
     * A „szédülés és hányinger edzés után" mondatra eddig egyáltalán nem jött
     * válasz – pedig ez nem izomprobléma, és gyakorlatsor nem jó felelet rá.
     */
    @Test public void dizzinessAroundTrainingIsARedFlag() {
        assertNotNull(Rehab.redFlag("szédülés és hányinger edzés után"));
        assertNotNull(Rehab.redFlag("megszédültem edzés közben"));
        assertTrue(Rehab.redFlag("fáj a mellkasom futás közben").contains("112"));
        // A hétköznapi panasz marad gyakorlatsor.
        assertNull(Rehab.redFlag("fáj a térdem"));
        assertNotNull(Rehab.forComplaint("fáj a térdem"));
    }

    /**
     * A törzs saját lapja: a legtöbbet kért, eddig hiányzó terület.
     *
     * A derék, a csípő és a térd sora mind a mély stabilizálókra hivatkozik,
     * de saját lapjuk nem volt. A „gyenge a törzsizmom" válasz nélkül maradt,
     * a „core erősítés" pedig hatvanperces kondi-BEJEGYZÉS lett a naplóban –
     * vagyis egy meg nem történt edzés.
     */
    @Test public void theCoreHasItsOwnPage() {
        assertNotNull(Rehab.byId("torzs"));
        assertEquals("torzs", Rehab.forComplaint("gyenge a törzsizmom").id);
        assertEquals("torzs", Rehab.forComplaint("instabil a medencém futás közben").id);
        assertEquals("torzs", Rehab.forGoal("core erősítés").id);
        assertEquals("torzs", Rehab.forGoal("szeretném erősíteni a hasizmomat").id);
        // A gyengeség máshol is panasz, nem csak a törzsnél.
        assertEquals("boka", Rehab.forComplaint("gyenge a bokám").id);
        assertEquals("terd", Rehab.forComplaint("nem bírja a térdem a lépcsőt").id);
        // A kimondott hosszúságú core-edzés viszont megtörtént mozgás marad.
        assertEquals(30, Activities.parse("core edzés 30 perc").plans.get(0).minutes);
    }

    /**
     * A tartás ragozott alakja is ugyanaz a kérés.
     *
     * A „jobb tartás" már a háti szakaszhoz vitt, a „hogyan javítsam a
     * tartásomat?" viszont válasz nélkül maradt – pedig ez a leggyakoribb
     * megfogalmazás, és a kérdőjel a rehab-lapon nem akadály.
     */
    @Test public void thePostureQuestionFindsItsArea() {
        assertEquals("hati", Rehab.forGoal("hogyan javítsam a tartásomat?").id);
        assertEquals("hati", Rehab.forGoal("rossz a tartásom").id);
        assertEquals("hati", Rehab.forGoal("roskadt a tartásom a gép előtt").id);
        assertEquals("hati", Rehab.forGoal("tartásjavító gyakorlatok").id);
    }

    /**
     * A reggeli bemerevedés panasz.
     *
     * A „bemerevedett" alak megvolt, a jelen idejű „bemerevedik" nem – pedig
     * a reggeli merevség a derékpanasz egyik legjellemzőbb mondata.
     */
    @Test public void theMorningStiffnessIsAComplaint() {
        assertNotNull(Rehab.forComplaint("reggelre bemerevedik a derekam"));
        assertEquals("derek", Rehab.forComplaint("reggelre bemerevedik "
                + "a derekam").id);
        assertEquals("nyak", Rehab.forComplaint("merevség a nyakamban").id);
    }

    /**
     * A diagnózis neve maga a panasz.
     *
     * A „csigolyasérvem van, mit csinálhatok?" és a „műtét után vagyok,
     * térdprotézis" eddig válasz nélkül maradt – pedig ezek a mondatok kérik
     * a leghatározottabban az óvatos, felépítő tornát.
     */
    @Test public void theDiagnosisNameIsTheComplaint() {
        assertEquals("derek", Rehab.forComplaint("csigolyasérvem van, "
                + "mit csinálhatok?").id);
        assertEquals("derek", Rehab.forComplaint("porckorongsérvem van").id);
        assertEquals("terd", Rehab.forComplaint("műtét után vagyok, "
                + "térdprotézis").id);
    }

    /**
     * A sérülés és a kopás neve is diagnózis.
     *
     * A „meniszkusz műtéten estem át", a „keresztszalag szakadás után vagyok"
     * és a „csigolya kopás a nyakamban" eddig válasz nélkül maradt. A
     * DUZZANAT pedig a legfontosabb jel: a „vizesedik a térdem edzés után"
     * pont az a mondat, amire a lap piros zászlója szól.
     */
    @Test public void theInjuryNameFindsTheArea() {
        assertEquals("terd", Rehab.forComplaint("meniszkusz műtéten estem át").id);
        assertEquals("terd", Rehab.forComplaint("keresztszalag szakadás "
                + "után vagyok").id);
        assertEquals("nyak", Rehab.forComplaint("csigolya kopás a nyakamban").id);
        assertEquals("terd", Rehab.forComplaint("vizesedik a térdem "
                + "edzés után").id);
        assertEquals("torzs", Rehab.forComplaint("diasztázisom van, "
                + "mit csinálhatok").id);
    }

    /**
     * A sérülés és az igekötős húzódás is panasz.
     *
     * A „lesérültem focin, boka" és a „meghúzódott a hátam a 120 kg-os
     * felhúzásnál" eddig válasz nélkül maradt – pedig pont a sérülés utáni
     * hetekben kell a felépítő lap.
     */
    @Test public void injuriesAndPulledMusclesAreComplaints() {
        assertEquals("boka", Rehab.forComplaint("lesérültem focin, boka, "
                + "2 hét kihagyás jön").id);
        assertEquals("derek", Rehab.forComplaint("meghúzódott a hátam "
                + "a 120 kg-os felhúzásnál").id);
        assertEquals("boka", Rehab.forComplaint("megrándult a bokám "
                + "a focin").id);
        // A b-igekötős húzódás ugyanaz: „behúzódott a combhajlítóm".
        assertEquals("comb", Rehab.forComplaint("behúzódott a combhajlítóm "
                + "futás közben").id);
    }

    /** A becsípődött ideg panasz – és nem bécsi szelet. */
    @Test public void aPinchedNerveIsNotASchnitzel() {
        assertEquals("derek", Rehab.forComplaint("becsípődött a derekam "
                + "reggel").id);
        assertTrue(Foods.parse(java.util.Arrays.asList(Foods.ALL),
                "becsípődött a derekam reggel").isEmpty());
    }
    @Test public void aSlippedBackAndAnInflamedAchillesAreComplaints() {
        assertEquals("derek", Rehab.forComplaint("kiugrott a derekam").id);
        assertEquals("achilles", Rehab.forComplaint("begyulladt az achillesem").id);
    }

    @Test public void aBrokenLegIsARedFlagNotASkiSession() {
        assertNotNull(Rehab.redFlag("elt\u00f6rt a l\u00e1bam s\u00edel\u00e9sn\u00e9l"));
    }

    @Test public void chestPainIsARedFlagWhateverTheWordOrder() {
        assertNotNull(Rehab.redFlag("szor\u00edt\u00f3 f\u00e1jdalom a mellkasban"));
        assertNotNull(Rehab.redFlag("f\u00e1jt a mellkasom edz\u00e9s alatt"));
        assertNotNull(Rehab.redFlag("belenyilallt a mellkasomba"));
        assertNotNull(Rehab.redFlag("kalap\u00e1lt a sz\u00edvem \u00e9s f\u00e1jt a mellkasom"));
    }

    @Test public void faintingAndColdSweatAreRedFlags() {
        assertNotNull(Rehab.redFlag("eszm\u00e9letemet vesztettem"));
        assertNotNull(Rehab.redFlag("h\u00e1nyingerem lett \u00e9s hideg verejt\u00e9k"));
        assertNotNull(Rehab.redFlag("elsz\u00e9d\u00fcltem edz\u00e9s k\u00f6zben"));
    }

    @Test public void aChestDayIsNotAHeartWarning() {
        assertNull(Rehab.redFlag("mellkas nap: fekvenyom\u00e1s 3x10 60 kg"));
        assertNull(Rehab.redFlag("megf\u00e1jdult a mellizmom fekvenyom\u00e1s k\u00f6zben"));
        assertNull(Rehab.redFlag("mellkas 105 cm"));
    }

    @Test public void backPainWithBladderTroubleIsAnEmergencySign() {
        assertNotNull(Rehab.redFlag(
                "vizel\u00e9si probl\u00e9m\u00e1m van a der\u00e9kf\u00e1j\u00e1s mellett"));
        assertNull(Rehab.redFlag("der\u00e9kf\u00e1j\u00e1s, de vizel\u00e9ssel nincs baj"));
    }

    @Test public void lostSensationAndAHotRedJointAreRedFlags() {
        assertNotNull(Rehab.redFlag("elvesztettem az \u00e9rz\u00e9st a l\u00e1bfejemben"));
        assertNotNull(Rehab.redFlag("bel\u00e1z a t\u00e9rdem \u00e9s piros"));
        assertNull(Rehab.redFlag("f\u00e1j a t\u00e9rdem, piros a cip\u0151m"));
    }

    @Test public void everydayNamesForCalfAndHipResolve() {
        assertEquals("achilles", Rehab.forComplaint("g\u00f6rcs\u00f6l a l\u00e1bikr\u00e1m \u00e9jjel").id);
        assertEquals("csipo", Rehab.forComplaint("f\u00e1j a tomporom fut\u00e1s ut\u00e1n").id);
    }

    @Test public void hipOpeningIsAMobilityGoal() {
        assertEquals("csipo", Rehab.forGoal("cs\u00edp\u0151 nyit\u00e1s \u00fcl\u00e9s ut\u00e1n").id);
        assertNull(Rehab.forGoal("a bolt nyit\u00e1s ut\u00e1n futottam 5 km-t"));
    }

    /**
     * A MEGH\u00daZ\u00c1S a leggyakoribb h\u00e9tk\u00f6znapi s\u00e9r\u00fcl\u00e9s-sz\u00f3: a „bok\u00e1mat
     * h\u00faztam meg, mikor kezdhetek" eddig v\u00e1lasz n\u00e9lk\u00fcl maradt.
     */
    @Test
    public void aPulledJointIsAComplaint() {
        Rehab.Area a = Rehab.forComplaint("bok\u00e1mat h\u00faztam meg, mikor kezdhetek");
        assertNotNull(a);
        assertEquals("boka", a.id);
    }

}
