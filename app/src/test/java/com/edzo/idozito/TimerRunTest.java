package com.edzo.idozito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Mikor számít futásnak egy edzés?
 *
 * A távmérés bekapcsolható köredzéshez is, és a GPS akkor is összegyűjt pár
 * száz métert, ha a felhasználó egy helyben dolgozik. A futásra szánt
 * kimenetek ilyenkor értelmetlenné váltak: a mentett átlagsebesség (50 méter
 * negyven perc alatt = 0,08 km/h), a „leghosszabb táv” rekord, és Blaze
 * hangos összefoglalója, ami „800 perc per kilométer” tempót mondott be.
 */
public class TimerRunTest {

    @Test public void aFewStrayMetresAreNotARun() {
        assertFalse("egy köredzés kavargása futás lett", TimerService.isRun(50));
        assertFalse(TimerService.isRun(1));
        assertFalse(TimerService.isRun(299));
    }

    @Test public void aRealRunIsARun() {
        assertTrue(TimerService.isRun(300));
        assertTrue(TimerService.isRun(1000));
        assertTrue(TimerService.isRun(42195));
    }

    @Test public void noMeasurementIsNotARun() {
        // -1 = nem is volt távmérés (nincs GPS vagy nincs engedély).
        assertFalse(TimerService.isRun(-1));
        assertFalse(TimerService.isRun(0));
    }

    @Test public void thePaceWouldHaveBeenAbsurdBelowTheLimit() {
        // Ez a szám hangzott el: 40 perc munka 50 méterre.
        double workSec = 40 * 60, dist = 50;
        double secPerKm = workSec / (dist / 1000.0);
        assertTrue("nem is volt abszurd a tempó: " + secPerKm, secPerKm > 10_000);
        assertFalse("épp ezt kell kiszűrni", TimerService.isRun(dist));
        // A határ fölött viszont már életszerű marad a tempó.
        double ok = TimerService.MIN_RUN_M;
        assertTrue(workSec / (ok / 1000.0) > 0);
    }

    @Test public void theCalorieEstimateStillCountsTheWorkout() {
        // A táv kiesik a futás-számításból, de az edzés nem lesz nulla:
        // az idő-ág adja a kalóriát (ez a v31.1-ben javított eset).
        double c = TimerService.calories(70, 50, 40 * 60);
        assertTrue("a köredzés kalóriája elveszett: " + c, c > 200);
    }
}
