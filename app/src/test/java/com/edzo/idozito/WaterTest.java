package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/**
 * A vízkövetés kulcskezelése. A napi mennyiség naponként külön beállítás-kulcsra
 * megy, és a régieket takarítjuk – ezért létfontosságú, hogy a takarítás pontosan
 * csak a napi kulcsokat ismerje fel, a célt és a jelvény-számlálót soha.
 */
public class WaterTest {

    @Test public void onlyDailyKeysAreRecognised() {
        assertTrue(Water.isDayKey("water_20260726"));
        // Ezek NEM napi kulcsok – ha annak néznénk, a takarítás kitörölné a
        // beállított célt és a Hidratált jelvény napszámlálóját.
        assertFalse(Water.isDayKey("water_goal_cl"));
        assertFalse(Water.isDayKey("water_last_done"));
        assertFalse(Water.isDayKey("water_days_done"));
        assertFalse(Water.isDayKey("kcal_goal"));
        assertFalse(Water.isDayKey("water_"));
        assertFalse(Water.isDayKey("water_2026072"));    // túl rövid
        assertFalse(Water.isDayKey("water_202607261"));  // túl hosszú
        assertFalse(Water.isDayKey(null));
    }

    @Test public void dayNumberIsSortableAndParsedBack() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.JULY, 26);
        assertEquals(20260726, Water.dayNumber(c));
        assertEquals("water_20260726", Water.dayKey(c));
        assertEquals(20260726, Water.dayOf("water_20260726"));
        assertEquals(-1, Water.dayOf("water_goal_cl"));

        // Egy korábbi nap száma mindig kisebb – erre épül a takarítás küszöbe.
        Calendar earlier = Calendar.getInstance();
        earlier.set(2026, Calendar.JANUARY, 5);
        assertTrue(Water.dayNumber(earlier) < Water.dayNumber(c));
    }

    @Test public void litersAreHumanReadable() {
        assertEquals("2.0 l", Water.liters(200));
        assertEquals("0.25 l", Water.liters(25));
        assertEquals("0.0 l", Water.liters(0));
    }

    @Test public void aGlassIsAQuarterLitre() {
        assertEquals(25, Water.GLASS_CL);
        assertEquals(200, Water.DEFAULT_GOAL_CL);   // alapból 2 liter
    }
}
