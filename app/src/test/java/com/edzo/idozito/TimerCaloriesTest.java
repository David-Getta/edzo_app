package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Kalóriabecslés.
 *
 * A becslésnek két lába van: a megtett út és az eltöltött idő. Amíg a táv-ág
 * győzött, amint egyáltalán volt mért méter, a bekapcsolt távmérés MEGRONTOTTA
 * a becslést minden olyan edzésnél, amit nem futva végez az ember: 40 perc
 * köredzés 50 méter kavargásból négy kalória lett. A szám nemcsak a képernyőn
 * jelent meg, hanem bement a naplóba, a napi összesítőbe és a rekordok közé is.
 */
public class TimerCaloriesTest {

    private static final double W = 70;

    @Test public void aCircuitWorkoutWithGpsOnIsNotWorthFourCalories() {
        // 40 perc munka, 50 méter helyben kavargás.
        double c = TimerService.calories(W, 50, 40 * 60);
        assertTrue("negyven perc edzés " + Math.round(c) + " kalória", c > 200);
        // A régi képlet ezt adta volna:
        assertTrue("a táv-ág önmagában ennyi lett volna", W * 0.05 * 1.036 < 5);
    }

    @Test public void aRealRunIsMeasuredByDistance() {
        // 10 km 50 perc alatt: a táv-ág a nagyobb, azt kell látni.
        double c = TimerService.calories(W, 10_000, 50 * 60);
        assertEquals(W * 10 * 1.036, c, 0.01);
        assertTrue("a futást nem szabad alulbecsülni", c > 700);
    }

    @Test public void theTimeFloorOnlyTakesOverBelowJoggingPace() {
        // Hol vált át a két ág? Kb. 6 km/h-nál. Vagyis futásnál – bármilyen
        // lassú kocogásnál – a táv-ág visz, az idő-ág csak akkor lép be, ha
        // az ember lényegében nem haladt: pont az a helyzet, amit javítunk.
        for (double kmh = 7; kmh <= 16; kmh += 0.5) {
            double dist = kmh * 1000;                    // egy óra alatt
            assertEquals("futótempónál nem a táv dönt (" + kmh + " km/h)",
                    W * kmh * 1.036, TimerService.calories(W, dist, 3600), 0.01);
        }
        for (double kmh = 0.5; kmh <= 5; kmh += 0.5) {
            double byTime = 6.0 * 3.5 * W / 200.0 * 60;
            assertEquals("egy helyben mégis a távot számolta (" + kmh + " km/h)",
                    byTime, TimerService.calories(W, kmh * 1000, 3600), 0.01);
        }
    }

    @Test public void withoutDistanceItFallsBackToTime() {
        // Táv nélküli edzés (nincs GPS vagy nincs engedély): változatlan viselkedés.
        double c = TimerService.calories(W, -1, 30 * 60);
        assertEquals(6.0 * 3.5 * W / 200.0 * 30, c, 0.01);
        assertEquals(TimerService.calories(W, 0, 30 * 60), c, 0.01);
    }

    @Test public void itIsAlwaysGrowingWithTimeAndDistance() {
        double prev = -1;
        for (int min = 0; min <= 90; min += 5) {
            double c = TimerService.calories(W, 2000, min * 60);
            assertTrue("hosszabb edzés kevesebb kalória: " + min + " perc", c >= prev);
            prev = c;
        }
        prev = -1;
        for (int m = 0; m <= 20_000; m += 500) {
            double c = TimerService.calories(W, m, 30 * 60);
            assertTrue("hosszabb táv kevesebb kalória: " + m + " m", c >= prev);
            prev = c;
        }
    }

    @Test public void aNamedProgramUsesItsOwnSportsIntensity() {
        // Egy óra „Kézilabda edzés" nem ugyanannyi, mint egy óra „Jóga" – a
        // név elárulja a mozgásformát, az idő-ág annak a MET-jével számol.
        double plain = TimerService.calories(W, 0, 3600);
        double kezi = TimerService.calories(W, 0, 3600, "Kézilabda edzés");
        double joga = TimerService.calories(W, 0, 3600, "Esti jóga");
        assertEquals(8.0 * 3.5 * W / 200.0 * 60, kezi, 0.01);
        assertEquals(3.0 * 3.5 * W / 200.0 * 60, joga, 0.01);
        assertTrue("a kézilabda intenzívebb az átlagnál", kezi > plain);
        assertTrue("a jóga nyugodtabb az átlagnál", joga < plain);
        // Ismeretlen név vagy név nélkül: marad az egyen-hatos MET.
        assertEquals(plain, TimerService.calories(W, 0, 3600, "20-10 Piramis"), 0.01);
        assertEquals(plain, TimerService.calories(W, 0, 3600, null), 0.01);
        // A Tabata viszont már ismert: kondi-intenzitással számol.
        assertEquals(5.0 * 3.5 * W / 200.0 * 60,
                TimerService.calories(W, 0, 3600, "20-10 Tabata"), 0.01);
        // Futásnál a táv-ág továbbra is erősebb a névnél.
        assertEquals(W * 10 * 1.036,
                TimerService.calories(W, 10_000, 50 * 60, "Reggeli futás"), 0.01);
        // A leghosszabb szótő nyer: a „sífutás" sí (MET 6), nem futás (9,8).
        assertEquals(6.0 * 3.5 * W / 200.0 * 60,
                TimerService.calories(W, 0, 3600, "Sífutás"), 0.01);
    }

    @Test public void aMissingBodyWeightDoesNotZeroTheResult() {
        // A testsúly a profilból jön; ha még nincs megadva, 70 kg-mal számolunk.
        assertEquals(TimerService.calories(70, 5000, 1800),
                TimerService.calories(0, 5000, 1800), 0.01);
        assertTrue(TimerService.calories(-5, 5000, 1800) > 0);
    }

    @Test public void aZeroLengthWorkoutIsZero() {
        assertEquals(0, TimerService.calories(W, 0, 0), 0.01);
        assertEquals(0, TimerService.calories(W, -1, -10), 0.01);
    }
}
