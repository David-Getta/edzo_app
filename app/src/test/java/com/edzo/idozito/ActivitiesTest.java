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
}
