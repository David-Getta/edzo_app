package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;

/**
 * Kézzel felvehető mozgásformák.
 *
 * A kalóriabecslés itt pontosabb lehet, mint a mért edzéseknél: ott nem tudjuk,
 * milyen mozgás történt (egységesen 6-os MET), itt viszont a felhasználó
 * megmondja. Egy óra jóga és egy óra harcművészet nem ugyanannyi.
 */
public class ActivitiesTest {

    @Test public void everyKindIsUsable() {
        HashSet<String> ids = new HashSet<>();
        for (Activities.Kind k : Activities.ALL) {
            assertTrue("üres azonosító", k.id != null && !k.id.isEmpty());
            assertTrue("kétszer szerepel: " + k.id, ids.add(k.id));
            assertTrue("üres név: " + k.id, k.name != null && !k.name.isEmpty());
            assertTrue("üres emoji: " + k.id, k.emoji != null && !k.emoji.isEmpty());
            assertTrue("életszerűtlen MET: " + k.id + " = " + k.met, k.met >= 2 && k.met <= 14);
            assertTrue("a cím nem tartalmazza a nevet", k.title().contains(k.name));
            // Az azonosító ékezet nélküli: mentésbe és beállításba kerül.
            for (char c : k.id.toCharArray())
                assertTrue("ékezetes azonosító: " + k.id, c >= 'a' && c <= 'z');
        }
        assertTrue("kevés a választható mozgásforma", Activities.ALL.length >= 12);
    }

    @Test public void thePickerLearnsTheUsersHabits() {
        // Négy kézilabda és két úszás után a lista élén a kézilabda áll,
        // másodikon az úszás; a többi az eredeti sorrendben marad.
        Activities.Kind[] ordered = Activities.orderedByHabit(new String[]{
                "kezilabda", "uszas", "kezilabda", "kezilabda", "uszas", "kezilabda"});
        assertEquals("kezilabda", ordered[0].id);
        assertEquals("uszas", ordered[1].id);
        assertEquals("futas", ordered[2].id);          // az eredeti első
        assertEquals(Activities.ALL.length, ordered.length);
        // Szokások nélkül az eredeti sorrend (és nem dob el senkit).
        Activities.Kind[] plain = Activities.orderedByHabit(new String[0]);
        for (int i = 0; i < plain.length; i++)
            assertEquals(Activities.ALL[i].id, plain[i].id);
        // Ismeretlen/üres azonosítók nem zavarnak be.
        Activities.Kind[] noisy = Activities.orderedByHabit(new String[]{"", "nincs", null});
        assertEquals(Activities.ALL[0].id, noisy[0].id);
    }

    @Test public void everySportsOwnNameResolvesToItself() {
        // Önellenőrzés: minden mozgásforma neve és minden szótöve a SAJÁT
        // sportjára essen a mondatos felismerésben – ha nem, a felhasználó
        // mást kapna, mint amit beírt. Két ismert kivétel: a puszta „sí" és
        // „kerti" szótőként túl sok szóba beleesne (a ragozott alak működik).
        StringBuilder bad = new StringBuilder();
        for (Activities.Kind k : Activities.ALL) {
            String q = k.name.split("/")[0].trim();
            if (!q.equals("Sí") && !q.equals("Kerti")) {
                Activities.Parsed p = Activities.parse(q);
                if (p.plans.size() != 1 || !p.plans.get(0).kind.id.equals(k.id))
                    bad.append("\n  név: ").append(q);
            }
            for (String w : k.words) {
                Activities.Parsed pw = Activities.parse(w);
                if (pw.plans.size() != 1 || !pw.plans.get(0).kind.id.equals(k.id))
                    bad.append("\n  tő: ").append(w);
            }
        }
        assertTrue("nem önmagára esik:" + bad, bad.length() == 0);
    }

    @Test public void theEverydaySportsAreThere() {
        for (String id : new String[]{"futas", "uszas", "kondi", "kezilabda", "foci", "kerekpar"})
            assertNotNull("hiányzik: " + id, Activities.byId(id));
    }

    @Test public void onlyDistanceSportsAskForDistance() {
        assertTrue(Activities.byId("futas").distance);
        assertTrue(Activities.byId("kerekpar").distance);
        assertTrue(Activities.byId("uszas").distance);
        // Kézilabdánál, kondinál a táv értelmetlen kérdés lenne.
        assertFalse(Activities.byId("kezilabda").distance);
        assertFalse(Activities.byId("kondi").distance);
        assertFalse(Activities.byId("joga").distance);
    }

    @Test public void anUnknownKindDoesNotBreakAnything() {
        // Régi napló: nincs „kind" mező. Ilyenkor a korábbi szabály él tovább.
        assertNull(Activities.byId(null));
        assertNull(Activities.byId(""));
        assertNull(Activities.byId("nincs-ilyen"));
        assertFalse(Activities.isCardio(null));
        assertFalse(Activities.isCardio(""));
        assertFalse(Activities.isCardio("nincs-ilyen"));
        assertTrue(Activities.isCardio("futas"));
        assertFalse(Activities.isCardio("kondi"));
        // Ismeretlen mozgásra is adunk becslést, hogy ne nulla legyen az edzés.
        assertTrue(Activities.calories(null, 70, 60) > 300);
    }

    @Test public void theCalorieEstimateFollowsTheSport() {
        double w = 70, min = 60;
        double joga = Activities.calories(Activities.byId("joga"), w, (int) min);
        double kezi = Activities.calories(Activities.byId("kezilabda"), w, (int) min);
        double harc = Activities.calories(Activities.byId("harcmuveszet"), w, (int) min);
        assertTrue("a jóga nem lehet annyi, mint a kézilabda", joga < kezi);
        assertTrue("a harcművészet a legintenzívebb a háromból", kezi < harc);
        // Nagyságrend: egy óra kézilabda 70 kg-mal ~590 kcal.
        assertEquals(8.0 * 3.5 * 70 / 200.0 * 60, kezi, 0.01);
        assertTrue(kezi > 500 && kezi < 700);
    }

    @Test public void itScalesWithTimeAndWeight() {
        Activities.Kind k = Activities.byId("futas");
        assertEquals(2 * Activities.calories(k, 70, 30), Activities.calories(k, 70, 60), 0.01);
        assertTrue(Activities.calories(k, 90, 60) > Activities.calories(k, 60, 60));
        // Hiányzó testsúly: 70 kg-mal számolunk, nem nullával.
        assertEquals(Activities.calories(k, 70, 60), Activities.calories(k, 0, 60), 0.01);
        assertEquals(Activities.calories(k, 70, 60), Activities.calories(k, -5, 60), 0.01);
        // Nulla vagy negatív perc nem ad kalóriát.
        assertEquals(0, Activities.calories(k, 70, 0), 0.01);
        assertEquals(0, Activities.calories(k, 70, -30), 0.01);
    }

    @Test public void noSportStemBelongsToTwoKinds() {
        // Ha két mozgásforma ugyanazt a szótövet hirdeti, az egyik csendben
        // elnyeli a másikat – a felismerés a listasorrendtől függene.
        java.util.HashMap<String, String> owner = new java.util.HashMap<>();
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        StringBuilder bad = new StringBuilder();
        for (Activities.Kind k : Activities.ALL) {
            if (!ids.add(k.id)) bad.append("\n  dupla azonosító: ").append(k.id);
            if (k.defaultMin < 1 || k.defaultMin > 300)
                bad.append("\n  ").append(k.id).append(": alapidő=").append(k.defaultMin);
            // A MET-tartomány: séta ~3, sprint/box ~10-12 fölött már nincs sport.
            if (k.met < 1 || k.met > 16)
                bad.append("\n  ").append(k.id).append(": MET=").append(k.met);
            for (String w : k.words) {
                if (w == null || w.trim().isEmpty()) {
                    bad.append("\n  ").append(k.id).append(": üres szótő");
                    continue;
                }
                String prev = owner.get(w);
                if (prev != null)
                    bad.append("\n  közös szótő \"").append(w).append("\": ")
                       .append(prev).append(" / ").append(k.id);
                else owner.put(w, k.id);
            }
        }
        assertTrue("hibás sportág-adatok:" + bad, bad.length() == 0);
    }
    @Test public void theHistorySearchFindsWhatPeopleType() {
        // A sportág neve és a szótövei is jók: aki „bicikli"-t ír, a
        // kerékpáros edzéseket keresi, nem a „Kerékpár" pontos alakját.
        assertTrue(Activities.matches("kerekpar", "", "", "bicikli"));
        assertTrue(Activities.matches("kerekpar", "", "", "kerékpár"));
        assertTrue(Activities.matches("kerekpar", "", "", "bringa"));
        assertTrue(Activities.matches("kezilabda", "", "", "kézi"));
        // Program- és jegyzetnév is számít.
        assertTrue(Activities.matches("", "Zsírégető HIIT", "", "zsírégető"));
        assertTrue(Activities.matches("futas", "", "Fájt a térdem", "térd"));
        // Üres keresésre minden illik.
        assertTrue(Activities.matches("futas", "", "", ""));
        assertTrue(Activities.matches("futas", null, null, null));
        // Ami tényleg nem illik, az nem illik.
        assertFalse(Activities.matches("kerekpar", "", "", "úszás"));
        assertFalse(Activities.matches("futas", "Reggeli torna", "", "kondi"));
        // A kind nélküli mért bejegyzés futásnak számít – ahogy a szűrőnél is.
        assertTrue(Activities.matches("", "", "", "futás"));
    }

    @Test public void theNamedDayIsKeptForTheStrengthLogToo() {
        // A súlyzós mondat az erősítő naplóba megy, a dátumot viszont ugyanez a
        // mondat hordozza – enélkül a tegnapi edzés MAI dátummal került be.
        long now = System.currentTimeMillis();
        long ts = Activities.singleDayTs(Activities.parse("tegnap guggolás 3x8 60 kg"), now);
        assertEquals(1, Days.ago(ts, now));
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ts);
        assertEquals(12, c.get(java.util.Calendar.HOUR_OF_DAY));
        // A kimondott napszak is megmarad.
        java.util.Calendar e = java.util.Calendar.getInstance();
        e.setTimeInMillis(Activities.singleDayTs(
                Activities.parse("tegnap este fekvenyomás 5x5 100 kg"), now));
        assertEquals(19, e.get(java.util.Calendar.HOUR_OF_DAY));
        // Ma: nincs mit eltolni.
        assertEquals(0, Activities.singleDayTs(Activities.parse("guggolás 3x8 60 kg"), now));
        assertEquals(0, Activities.singleDayTs(Activities.parse("ma guggolás 3x8"), now));
        // Több napra szóló mondatnál nem találgatunk.
        assertEquals(0, Activities.singleDayTs(
                Activities.parse("az elmúlt 3 napban guggolás 3x8"), now));
        assertEquals(0, Activities.singleDayTs(null, now));
    }

    @Test public void trainingWithAPassIsStillTraining() {
        // A bérlet VÁSÁRLÁSA nem edzés – a bérlettel VÉGZETT edzés viszont az.
        // Eddig az egész tagmondat eltűnt, vagyis egy megtörtént edzés nem
        // került a naplóba. Magyarországon a legtöbben bérlettel járnak.
        long now = System.currentTimeMillis();
        assertEquals(1, Activities.parse("60 perc kondi bérlettel", now).plans.size());
        assertEquals(1, Activities.parse("bérlettel 60 perc kondi", now).plans.size());
        assertEquals(1, Activities.parse("a bérletemmel 60 perc kondi", now).plans.size());
        // A vásárlás továbbra sem edzés.
        assertTrue(Activities.parse("vettem egy bérletet", now).isEmpty());
        assertTrue(Activities.parse("bérletre költöttem", now).isEmpty());
        // Külön tagmondatban a vásárlás mellett az edzés megmarad.
        assertEquals(1, Activities.parse("bérletet vettem, aztán 60 perc kondi",
                now).plans.size());
    }

    @Test public void theVerbFormsOfTheseSportsAreRecognised() {
        // A szótő eddig főnévi alakban volt („birkózás”, „lapátolás”), az
        // emberek viszont igét írnak. Ami nem esett kindra, az elveszett vagy
        // „egyéb mozgás” lett – rossz MET-tel és rossz szűrővel.
        long now = System.currentTimeMillis();
        String[][] cases = {{"birkóztam 60 percet", "harcmuveszet"},
                {"balettoztam 60 percet", "tanc"},
                {"havat lapátoltam 45 percet", "munka"},
                {"ugrálókötél 15 perc", "egyeb"},
                {"köredzés 40 perc", "kondi"},
                {"mobilizáltam 15 percet", "joga"}};
        for (String[] c : cases) {
            Activities.Parsed p = Activities.parse(c[0], now);
            assertEquals(c[0], 1, p.plans.size());
            assertEquals(c[0], c[1], p.plans.get(0).kind.id);
        }
    }

    @Test public void noEverydayWordDistortsTheWorkout() {
        // Ugyanaz az őrszem, mint az ételeknél: hétköznapi szó + egy egyszerű
        // edzés. A szó nem változtathatja meg sem az alkalmak számát, sem az
        // időszakot. Három valódi hiba került így elő: a „lehetőség" (le-HET-
        // őség) egy hetes időszak lett, a „kétszeres" két alkalom, a „hetes"
        // szintén egy hét.
        long now = System.currentTimeMillis();
        StringBuilder bad = new StringBuilder();
        // Amiknek VAN jelentésük az edzés-naplóban, azok kimaradnak: a
        // napnevek és a hónapnevek dátumot jelölnek, az eszköznevek pedig
        // valódi mozgásformák (a futópad futás, az evezőgép evezés).
        java.util.List<String> skip = java.util.Arrays.asList("tegnap", "hétfő", "kedd",
                "szerda", "péntek", "szombat", "vasárnap", "január", "február",
                "március", "április", "május", "június", "július", "augusztus",
                "szeptember", "október", "november", "december", "nyújtás",
                "futópad", "evezőgép", "szobabicikli", "sport", "edzés",
                "délelőtt", "délután", "hajnal", "alvás", "pihenő", "labda",
                "kötél", "medence", "úszik", "biciklizik", "edz", "fut", "jár",
                "pihen", "megy");
        for (String w : FoodsTest.EVERYDAY) {
            if (skip.contains(w)) continue;
            // A bérlet VÁSÁRLÁSA szándékosan kizárja a tagmondatot.
            if (w.startsWith("bérlet")) continue;
            Activities.Parsed p = Activities.parse(w + " 30 perc kondi", now);
            if (p.plans.size() != 1 || !p.plans.get(0).kind.id.equals("kondi")
                    || p.plans.get(0).count != 1 || p.plans.get(0).minutes != 30
                    || p.days != 1 || p.offset != 0)
                bad.append("\n  ").append(w).append(" -> nap ").append(p.days)
                   .append(" eltol ").append(p.offset).append(" terv ")
                   .append(p.plans.size());
        }
        assertEquals("hétköznapi szó torzította az edzést:" + bad, 0, bad.length());
    }

    @Test public void everydayWordsHidingSportStemsAreNotSports() {
        // A rövid szótövek beleesnek hétköznapi szavakba: a kul-TÚRA nem túra,
        // a te-KER-cs nem kerékpár, a TORNA-cipő nem torna. A hiba csendes –
        // a bejegyzés létrejön, csak egy meg nem történt edzésről.
        long now = System.currentTimeMillis();
        for (String q : new String[]{"kultúra 30 perc kondi", "struktúra 30 perc kondi",
                "tornacipőben futottam 5 km-t", "tekercset ettem, 30 perc kondi",
                "bevásárlókosár, 30 perc kondi", "kézitáska, 30 perc kondi"}) {
            java.util.List<Activities.Plan> p = Activities.parse(q, now).plans;
            assertEquals("álca-szóból sport lett: " + q, 1, p.size());
        }
        // Az összetett sportnevek viszont épek maradnak – ezeket egy
        // szóhatár-szabály elvágta volna, az álcázás nem.
        String[][] ok = {{"gerinctorna 30 perc", "joga"}, {"gyógytorna 30 perc", "joga"},
                {"hegyitúra 3 óra", "tura"}, {"tornaterem 1 óra", "kondi"},
                {"strandröplabda 60 perc", "roplabda"}, {"vízitorna 45 perc", "uszas"},
                {"tekertem 20 km-t", "kerekpar"}, {"kosárlabda 60 perc", "kosarlabda"}};
        for (String[] c : ok) {
            java.util.List<Activities.Plan> p = Activities.parse(c[0], now).plans;
            assertEquals(c[0], 1, p.size());
            assertEquals(c[0], c[1], p.get(0).kind.id);
        }
    }

    @Test public void aMonthNameIsNotAlwaysADate() {
        // A szám MÉRTÉKEGYSÉGE elárulja, hogy nem a hónap napja: a „január 30
        // perc kondi" harminc perc, nem január 30-a. Eddig a fél éves
        // visszadátumozás miatt a bejegyzés a semmibe került.
        long now = System.currentTimeMillis();
        assertEquals(0, Activities.parse("január 30 perc kondi", now).offset);
        assertEquals(30, Activities.parse("január 30 perc kondi", now)
                .plans.get(0).minutes);
        assertEquals(0, Activities.parse("március 15 km futás", now).offset);
        // A ragozott dátum viszont dátum marad.
        assertTrue(Activities.parse("július 28-án 45 perc futás", now).offset > 0);
        // Az „úsz" tő az aug-USZ-tusban és a b-USZ-ban is benne van, de ott
        // nem úszás.
        assertTrue(Activities.parse("augusztus 5 perc nyújtás", now).plans.size() == 1);
        assertEquals("kondi", Activities.parse("busszal mentem, 30 perc kondi", now)
                .plans.get(0).kind.id);
        // Igekötő után viszont igen.
        assertEquals("uszas", Activities.parse("leúsztam 1500 métert", now)
                .plans.get(0).kind.id);
    }

    @Test public void theOrdinalSevenIsNotAWeek() {
        // A „hetes" sorszám vagy jelző, nem időszak: eddig a „hetes bérlettel
        // kondi" és a „futás a hetes buszmegállóig" is egyhetes időszakra
        // terült szét, vagyis egy edzésből hét nap átlaga lett.
        long now = System.currentTimeMillis();
        assertEquals(1, Activities.parse("hetes bérlettel kondi", now).days);
        assertEquals(1, Activities.parse("futás a hetes buszmegállóig", now).days);
        // A valódi időszak viszont megmarad.
        assertEquals(7, Activities.parse("a héten 3 futás", now).days);
        assertEquals(7, Activities.parse("az elmúlt héten 2 úszás", now).days);
    }

    @Test public void everyMorningMeansEveryDay() {
        // A „minden reggel 20 perc jóga a héten" hét jógát jelent, nem egyet:
        // eddig a napszak elnyelte a „minden"-t, és a heti ismétlődés elveszett.
        long now = System.currentTimeMillis();
        assertEquals(7, Activities.parse("minden reggel 20 perc jóga a héten", now)
                .plans.get(0).count);
        assertEquals(7, Activities.parse("minden este 30 perc séta a héten", now)
                .plans.get(0).count);
        // Időszak nélkül továbbra is egy alkalom.
        assertEquals(1, Activities.parse("minden nap 20 perc jóga", now)
                .plans.get(0).count);
    }

    @Test public void aRestDayDoesNotDuplicateTheWorkout() {
        // „Szombaton túráztam 4 órát, vasárnap pihentem": KÉT napot nevez meg,
        // és eddig mindkettőre bekerült a négyórás túra – nyolc óra mozgás
        // abból, ami négy volt.
        long now = System.currentTimeMillis();
        Activities.Parsed p = Activities.parse("szombaton túráztam 4 órát, vasárnap pihentem",
                now);
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(240, p.plans.get(0).minutes);
        assertEquals(1, Activities.parse("hétfőn futottam, kedden pihentem", now)
                .plans.get(0).count);
        // Két VALÓDI edzésnap viszont továbbra is kettő.
        assertEquals(2, Activities.parse("hétfőn futottam, kedden is futottam", now)
                .plans.get(0).count);
        // A puszta pihenőnapból nincs bejegyzés.
        assertTrue(Activities.parse("vasárnap pihentem", now).isEmpty());
        assertTrue(Activities.parse("pihenőnap volt", now).isEmpty());
        // A pihenés az edzés UTÁN nem viszi el az edzést.
        assertEquals(30, Activities.parse("futottam 30 percet, aztán pihentem", now)
                .plans.get(0).minutes);
    }

    @Test public void aConjunctionOpensANewStatement() {
        // A kötőszó után ÚJ állítás jön, vessző nélkül is. Eddig a vásárlás,
        // a lemondás és a meccsnézés magával vitte a mondat másik felét –
        // vagyis egy megtörtént edzés nem került a naplóba.
        long now = System.currentTimeMillis();
        for (String q : new String[]{
                "vettem egy új cipőt és futottam 5 km-t",
                "rendeltem pizzát és futottam 30 percet",
                "lemondtam az órát és úsztam 40 percet",
                "focit néztem és futottam 30 percet",
                "futottam 30 percet majd rendeltem pizzát",
                "futottam 5 km-t és vettem egy cipőt"})
            assertEquals("elveszett az edzés: " + q, 1,
                    Activities.parse(q, now).plans.size());
        // Kötőszó nélkül viszont az egész tagmondat marad kizárva.
        assertTrue(Activities.parse("vettem egy bérletet", now).isEmpty());
        assertTrue(Activities.parse("focimeccset néztem", now).isEmpty());
        assertTrue(Activities.parse("elmaradt a foci", now).isEmpty());
        assertTrue(Activities.parse("kihagytam az edzést", now).isEmpty());
    }

    @Test public void multisportIsNotLastWeek() {
        // A MultiSport bérlet neve tartalmazza a „mult"-ot. Szó belsejében az
        // nem múlt hét – egy hetet csúszott volna a bejegyzés.
        long now = System.currentTimeMillis();
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(now);
        int today = (c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;   // H=0..V=6
        int kedd = (today - 1 + 7) % 7;                                 // a legutóbbi kedd
        assertEquals(kedd, Activities.parse("multisport kedden 60 perc kondi",
                now).offset);
        // A valódi „múlt kedden" viszont továbbra is egy héttel korábbi.
        assertEquals(kedd + 7,
                Activities.parse("múlt kedden 60 perc kondi", now).offset);
    }
    /**
     * Az „összesen" a TELJES időt mondja ki, nem fejenként annyit.
     *
     * A „kondi és futás, összesen másfél óra" mindkét mozgásnak kilencven
     * percet adott: a nap háromszor annyi mozgással zárult, mint amennyi volt –
     * és pont abban a mondatban, amivel az ember összegez.
     */
    @Test public void theWordTotalSplitsTheTime() {
        Activities.Parsed p = Activities.parse("kondi + futás, összesen másfél óra", 1_753_869_600_000L);
        assertEquals(2, p.plans.size());
        assertEquals(45, p.plans.get(0).minutes);
        assertEquals(45, p.plans.get(1).minutes);
        // Vessző nélkül is: az „összesen" a szóban áll, nem a központozásban.
        Activities.Parsed q = Activities.parse("kondi és futás összesen 90 perc", 1_753_869_600_000L);
        assertEquals(45, q.plans.get(0).minutes);
        assertEquals(45, q.plans.get(1).minutes);
        // Az összesen nélküli összefoglaló idő továbbra is mindenkire vonatkozik.
        Activities.Parsed r = Activities.parse("kondi és futás, 90 perc", 1_753_869_600_000L);
        assertEquals(90, r.plans.get(0).minutes);
        assertEquals(90, r.plans.get(1).minutes);
    }

    /** A verseny neve is kimondja a sportot, a kirándulás pedig ragozódik. */
    @Test public void racesAndHikesAreRecognised() {
        assertEquals("futas", Activities.parse("futóverseny 52 perc", 1_753_869_600_000L)
                .plans.get(0).kind.id);
        assertEquals(52, Activities.parse("futóverseny 52 perc", 1_753_869_600_000L).plans.get(0).minutes);
        assertEquals("uszas", Activities.parse("úszóverseny 30 perc", 1_753_869_600_000L)
                .plans.get(0).kind.id);
        // A „kirándulás" töve eddig a főnév volt, így az igealak elveszett.
        for (String q : new String[]{"kirándultunk 5 órát", "kirándulás 5 óra",
                "kirándulni voltam 5 órát"}) {
            Activities.Parsed p = Activities.parse(q, 1_753_869_600_000L);
            assertEquals(q, 1, p.plans.size());
            assertEquals(q, "tura", p.plans.get(0).kind.id);
            assertEquals(q, 300, p.plans.get(0).minutes);
        }
    }
    /**
     * A kimondott tempó pontosabb, mint a mozgásforma átlaga.
     *
     * Tíz kilométer 5:30-as tempóval ötvenöt perc; a becslés hatvanat mondott.
     * Aki kiírja a tempóját, az pontosan tudja, mennyit futott.
     */
    @Test public void theStatedPaceWins() {
        long now = 1_753_869_600_000L;
        assertEquals(55, Activities.parse("10 km-t futottam 5:30-as tempóval", now)
                .plans.get(0).minutes);
        assertEquals(55, Activities.parse("10 km futás 5:30/km", now).plans.get(0).minutes);
        // Sportnév nélkül, pusztán távból is: az „5 km" magyarul futás.
        assertEquals(50, Activities.parse("10 km 5:00-es tempó", now).plans.get(0).minutes);
        // Tempó nélkül marad a mozgásforma átlaga.
        assertEquals(60, Activities.parse("10 km futás", now).plans.get(0).minutes);
        // Életszerűtlen tempóra nem hallgatunk: az 1:30/km nem futás.
        assertEquals(60, Activities.parse("10 km futás 1:30-as tempóval", now)
                .plans.get(0).minutes);
        // A kimondott időtartam viszont a tempónál is erősebb.
        assertEquals(52, Activities.parse("10 km futás 52 perc 5:30-as tempóval", now)
                .plans.get(0).minutes);
    }
    /**
     * Két napszak, két kimondott idő, egy mozgásforma: két edzés volt.
     *
     * A „reggel 30 perc futás, este 45 perc futás" második futása eddig
     * kiesett – egy mozgásforma egyszer szerepel –, a „délelőtt 1 óra,
     * délután fél óra kondi" második ideje pedig gazdátlanul maradt.
     * Mindkét mondatban a nap fele hiányzott a naplóból.
     */
    @Test public void twoDayPartsMeanTwoSessions() {
        long now = 1_753_869_600_000L;
        Activities.Parsed p = Activities.parse("délelőtt 1 óra, délután fél óra kondi", now);
        assertEquals(1, p.plans.size());
        assertEquals(2, p.plans.get(0).count);
        assertEquals(45, p.plans.get(0).minutes);      // 60 + 30 = 90 összesen
        Activities.Parsed q = Activities.parse("reggel 30 perc futás, este 45 perc futás", now);
        assertEquals(2, q.plans.get(0).count);
        assertEquals(37, q.plans.get(0).minutes);      // 30 + 45 = 75 összesen
        // Két KÜLÖNBÖZŐ mozgás nem esik ebbe: mindegyik a sajátját kapja.
        Activities.Parsed r = Activities.parse("reggel 30 perc futás, este 45 perc kondi", now);
        assertEquals(2, r.plans.size());
        assertEquals(30, r.plans.get(0).minutes);
        assertEquals(45, r.plans.get(1).minutes);
        // Egy napszak, egy idő: változatlan.
        assertEquals(1, Activities.parse("este 45 perc kondi", now).plans.get(0).count);
    }

    /**
     * Az „és" néha két mozgást választ el, nem egy időtartam két felét.
     *
     * A „kondi 1 óra és 30 perc futás" hatvan perc kondi és harminc perc
     * futás – eddig a kondi kapott kilencven percet, a futás meg a szokásos
     * hosszát. A jel az, hogy az első szám ELŐTT is, a második UTÁN is áll
     * mozgásforma.
     */
    @Test public void theWordAndCanSeparateTwoSports() {
        long now = 1_753_869_600_000L;
        Activities.Parsed p = Activities.parse("délelőtt kondi 1 óra és 30 perc futás", now);
        assertEquals(2, p.plans.size());
        assertEquals(60, p.plans.get(0).minutes);
        assertEquals(30, p.plans.get(1).minutes);
        // Mozgás nélkül elöl az összevonás marad: ez tényleg másfél óra futás.
        assertEquals(90, Activities.parse("1 óra és 30 perc futás", now)
                .plans.get(0).minutes);
        assertEquals(90, Activities.parse("futás 1 óra és 30 perc", now)
                .plans.get(0).minutes);
    }
    /**
     * Ugyanannak a sportnak KÉT szava egymás mellett is egy edzés.
     *
     * A „kondi konditerem" nem két edzés, és nem is más sport. A rövid tövek
     * csapdája itt is él: egy új szó beleeshet egy másikba, és a bejegyzés
     * csendben megkettőződik vagy átcsúszik. Az étel-oldalon pontosan ez
     * bújt meg a párizsiban.
     */
    @Test public void twoWordsOfTheSameSportStillMeanOneWorkout() {
        long now = 1_753_869_600_000L;
        StringBuilder bad = new StringBuilder();
        for (Activities.Kind k : Activities.ALL)
            for (int i = 0; i < k.words.length; i++)
                for (int j = 0; j < k.words.length; j++) {
                    if (i == j) continue;
                    String q = k.words[i] + " " + k.words[j] + " 30 perc";
                    Activities.Parsed p = Activities.parse(q, now);
                    if (p.plans.size() == 1 && p.plans.get(0).kind.id.equals(k.id)
                            && p.plans.get(0).count == 1) continue;
                    bad.append("\n  ").append(q).append(" -> ");
                    for (Activities.Plan pl : p.plans)
                        bad.append(pl.kind.id).append('×').append(pl.count).append(' ');
                }
        assertEquals("ütköző mozgás-szó:" + bad, 0, bad.length());
    }
    /**
     * Kötőjeles szám az időtartam előtt: „10-15 perc futás".
     *
     * A kitakarás eddig csak a második számot vitte el a mértékegységével
     * együtt; az első ott maradt, és DARABSZÁMNAK látszott. A „10-15 perc
     * futás" tíz külön futás lett, tizenöt percenként – és ugyanígy a
     * „30-30 perc kondi" harminc darab. Csendben, minden ilyen mondatnál.
     */
    @Test public void aHyphenatedNumberBeforeTheTimeIsNotACount() {
        long now = 1_753_869_600_000L;
        Activities.Parsed a = Activities.parse("10-15 perc futás", now);
        assertEquals(1, a.plans.size());
        assertEquals(1, a.plans.get(0).count);
        assertEquals(13, a.plans.get(0).minutes);      // a tartomány közepe
        Activities.Parsed b = Activities.parse("20-20 perc futás", now);
        assertEquals(1, b.plans.get(0).count);
        assertEquals(20, b.plans.get(0).minutes);
    }

    /** Ugyanez a távra: az „5-8 km futás" egy futás, nem öt. */
    @Test public void aHyphenatedNumberBeforeTheDistanceIsNotACount() {
        long now = 1_753_869_600_000L;
        Activities.Parsed p = Activities.parse("5-8 km futás", now);
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(6.5, p.plans.get(0).km, 0.001);   // a tartomány közepe
        Activities.Parsed q = Activities.parse("10-12 km bringa", now);
        assertEquals(1, q.plans.get(0).count);
        assertEquals(11, q.plans.get(0).km, 0.001);
        // Az egyszerű táv nem változott.
        assertEquals(10, Activities.parse("10 km futás", now).plans.get(0).km, 0.001);
    }

    /**
     * Az osztó alak alkalmanként értendő: „reggel és este 30-30 perc kondi"
     * két harmincperces edzés.
     */
    @Test public void theDistributiveFormMeansPerOccasion() {
        long now = 1_753_869_600_000L;
        Activities.Parsed p = Activities.parse("reggel és este 30-30 perc kondi", now);
        assertEquals(1, p.plans.size());
        assertEquals(2, p.plans.get(0).count);
        assertEquals(30, p.plans.get(0).minutes);
        Activities.Parsed q =
                Activities.parse("reggel és este is futottam 20-20 percet", now);
        assertEquals(2, q.plans.get(0).count);
        assertEquals(20, q.plans.get(0).minutes);
        // A darabszámos osztó alak nem romolhatott el.
        assertEquals(2, Activities.parse("tegnap és ma 1-1 futás", now)
                .plans.get(0).count);
    }
    /**
     * Hétköznapi szavak, amikben egy mozgásforma neve rejtőzik.
     *
     * A megTAKARÍTás nem takarítás, a légKONDI nem kondi, a tanTEREM nem
     * edzőterem, az olvASÁSban pedig ott az ásás – a fotelban töltött este
     * eddig kerti munkaként került a naplóba.
     */
    @Test public void everydayWordsHidingASportAreNotWorkouts() {
        long now = 1_753_869_600_000L;
        for (String q : new String[]{"megtakarítás", "légkondi", "tanterem", "díszterem",
                "olvasás", "kosár", "bevásárlókosár", "étterem", "műterem"})
            assertTrue(q + " -> " + Activities.parse(q, now).plans,
                    Activities.parse(q, now).plans.isEmpty());
        // A valódi alakok érintetlenek.
        for (String[] q : new String[][]{{"takarítás", "munka"}, {"kondi", "kondi"},
                {"konditerem", "kondi"}, {"edzőterem", "kondi"}, {"tornaterem", "kondi"},
                {"kosárlabda", "kosarlabda"}, {"kosaraztam", "kosarlabda"},
                {"kosár edzés", "kosarlabda"}, {"ásás", "munka"}})
            assertEquals(q[0], q[1], Activities.parse(q[0], now).plans.get(0).kind.id);
    }
    /**
     * A leggyakoribb IGEALAKOK is felismerhetők.
     *
     * Magyarul az ember igét ír, nem főnevet: „úsztam", nem „úszás". Az
     * összes mozgásforma első személyű múlt idejét végigfuttatjuk, mert egy
     * hiányzó igealaknál a bejegyzés némán elveszik.
     */
    @Test public void thePastTenseFormsAreUnderstood() {
        long now = 1_753_869_600_000L;
        String[][] cases = {
                {"futottam 30 percet", "futas"}, {"kocogtam 30 percet", "futas"},
                {"úsztam 30 percet", "uszas"}, {"bicikliztem 30 percet", "kerekpar"},
                {"kerékpároztam 30 percet", "kerekpar"}, {"gyalogoltam 30 percet", "tura"},
                {"sétáltam 30 percet", "tura"}, {"túráztam 30 percet", "tura"},
                {"jógáztam 30 percet", "joga"}, {"nyújtottam 30 percet", "joga"},
                {"táncoltam 30 percet", "tanc"}, {"kondiztam 30 percet", "kondi"},
                {"gyúrtam 30 percet", "kondi"}, {"eveztem 30 percet", "evezes"},
                {"korcsolyáztam 30 percet", "korcsolya"}, {"síztem 30 percet", "si"},
                {"fociztam 30 percet", "foci"}, {"kosaraztam 30 percet", "kosarlabda"},
                {"teniszeztem 30 percet", "tenisz"}, {"röplabdáztam 30 percet", "roplabda"},
                {"kéziztem 30 percet", "kezilabda"}, {"boxoltam 30 percet", "harcmuveszet"},
                {"karatéztam 30 percet", "harcmuveszet"},
                {"tekéztem 30 percet", "egyeb"}, {"sportoltam 30 percet", "egyeb"},
        };
        StringBuilder bad = new StringBuilder();
        for (String[] c : cases) {
            Activities.Parsed p = Activities.parse(c[0], now);
            String got = p.plans.isEmpty() ? "—" : p.plans.get(0).kind.id;
            if (!got.equals(c[1]))
                bad.append("\n  ").append(c[0]).append(" -> ").append(got)
                   .append(" (várt: ").append(c[1]).append(')');
        }
        assertEquals("elcsúszott igealak:" + bad, 0, bad.length());
    }
    /**
     * A kizárt nap nem a bejegyzés napja.
     *
     * A „vasárnap kivételével minden nap kondi" mondatban a vasárnap épp az
     * a nap, amelyiken NEM volt edzés – a bejegyzés mégis oda került. A kizárt
     * napok kibontását nem vállaljuk, de rossz napra írni rosszabb, mint nem
     * tudni a napot.
     */
    @Test public void anExcludedDayIsNotTheDayOfTheEntry() {
        long now = 1_753_869_600_000L;                 // szerda
        Activities.Parsed p = Activities.parse("vasárnap kivételével minden nap kondi", now);
        assertEquals(1, p.plans.size());
        assertEquals(0, p.offset);
        // A kizárás nélküli alak változatlan: az a legutóbbi vasárnap.
        assertEquals(3, Activities.parse("vasárnap kondi", now).offset);
    }

    /**
     * Az „mma" három betű, és magyar szavakban is ott ül.
     *
     * A dileMMA, az EMMA és a geMMA eddig harcművész edzést vitt a naplóba –
     * a bejegyzés létrejött, csak épp nem történt meg.
     */
    @Test public void threeLetterSportStemsDoNotHideInWords() {
        long now = 1_753_869_600_000L;
        for (String q : new String[]{"Emma", "Emma jött velem", "dilemma volt",
                "gamma sugárzás", "gemma"})
            assertTrue("edzés lett belőle: " + q, Activities.parse(q, now).isEmpty());
        // A valódi harcművészet marad.
        assertTrue(!Activities.parse("mma edzés 1 óra", now).isEmpty());
    }

    /**
     * A rövid tövek szó belsejében csak igekötő után élnek.
     *
     * Az „mma" MINDEN -mmal ragos szóban ott van (alkaloMMAl, száMMAl,
     * graMMAl), a „gym" az EGYMÁSban, a „kezi" a jelentKEZIkben. Mind
     * harcművész, kondi és kézilabda edzést vitt a naplóba – megtörtént
     * edzésként, a szériába és az XP-be is beleszámítva.
     */
    @Test public void shortStemsInsideWordsNeedAVerbPrefix() {
        long now = 1_753_869_600_000L;
        for (String q : new String[]{"150 grammal több", "egymás után 3 kör",
                "dátummal együtt", "számmal jelölve", "jelentkezik",
                "kézisúlyzóval 3x12", "csatornán néztem", "beolvasása"})
            assertTrue("edzés lett belőle: " + q, Activities.parse(q, now).isEmpty());
        // Igekötő után viszont valódi, és a szó elején is.
        assertTrue(!Activities.parse("leúsztam 1000 métert", now).isEmpty());
        assertTrue(!Activities.parse("kieveztem a tóra", now).isEmpty());
        assertTrue(!Activities.parse("gym-ben edzettem", now).isEmpty());
        assertTrue(!Activities.parse("kézilabda meccs", now).isEmpty());
        assertTrue(!Activities.parse("gyógytorna 20 perc", now).isEmpty());
    }

    /**
     * Egy szó – egy edzés, akkor is, ha két szótő osztozik a betűin.
     *
     * A „hegyMÁSZÁS" elején a hegymászás (túra), a végén a mászás
     * (falmászás). A két tő átfed, de egyik sem esik a másikba – így a
     * mondatból KÉT edzés lett: négy óra falmászás ÉS másfél óra túra,
     * ugyanabból a szóból, ugyanarra a napra.
     */
    @Test public void twoOverlappingStemsInOneWordMeanOneWorkout() {
        long now = 1_753_869_600_000L;
        Activities.Parsed p = Activities.parse("hegymászás 4 óra", now);
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(240, p.plans.get(0).minutes);
        // A többi összetett sportszó sem esik szét.
        for (String q : new String[]{"falmászás 1 óra", "sífutás 2 óra",
                "vízitorna 45 perc", "strandröplabda 1 óra", "tornaterem 1 óra",
                "kerékpártúra 2 óra"})
            assertEquals(q, 1, Activities.parse(q, now).plans.size());
    }

    /** A frissen felvett mozgásformák a megfelelő MET-hez kerülnek. */
    @Test public void theNewlyAddedActivitiesLandInTheRightKind() {
        long now = 1_753_869_600_000L;
        String[][] cases = {{"kenu 2 óra", "evezes"}, {"rafting 2 óra", "evezes"},
                {"búvárkodás 1 óra", "egyeb"}, {"szánkózás 1 óra", "egyeb"},
                {"parkour 1 óra", "egyeb"}, {"salsa 1 óra", "tanc"},
                {"barlangászat 2 óra", "tura"}, {"via ferrata 3 óra", "tura"},
                {"taposógép 20 perc", "egyeb"}};
        for (String[] c : cases) {
            Activities.Parsed p = Activities.parse(c[0], now);
            assertEquals(c[0], 1, p.plans.size());
            assertEquals(c[0], c[1], p.plans.get(0).kind.id);
        }
    }
}
