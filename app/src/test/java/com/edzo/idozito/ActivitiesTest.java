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
}
