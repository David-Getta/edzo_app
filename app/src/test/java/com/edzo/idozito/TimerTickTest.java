package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Az időzítő ébresztési ütemezése.
 *
 * Két baja volt. Egy: fix 200 ms-onként ébredve a másodperc váltását – és vele
 * a visszaszámláló sípját – akár 200 ms-mal később vettük észre, ütemenként
 * mást, így a „3–2–1” egyenetlenül szólt. Kettő: minden szakasz a MOSTANI
 * ébredéstől mérte a végét, nem az előző szakasz tervezett végétől, így a
 * késés körről körre gyűlt – egy 20 körös edzés a végére másodpercekkel
 * hosszabb lett.
 *
 * Az itteni szimuláció végigjátssza az edzést, késleltetett ébredésekkel.
 */
public class TimerTickTest {

    @Test public void theTickAlwaysLandsOnTheSecondBoundary() {
        for (long remain = 1; remain <= 60_000; remain++) {
            long d = TimerService.nextTickDelay(remain);
            assertTrue("nem várunk semmit: " + remain, d >= 1);
            assertTrue("túl sokáig alszunk: " + remain + " -> " + d, d <= 200);
            long after = remain - d;
            // A kijelzett szám legfeljebb egyet léphet két ébredés között –
            // egy másodpercet sem ugorhatunk át.
            int before = (int) Math.ceil(remain / 1000.0), then = (int) Math.ceil(after / 1000.0);
            assertTrue("átugrottunk egy másodpercet: " + remain + " -> " + after,
                    before - then <= 1);
            // És ha vált a szám, pontosan a váltás pillanatában ébredünk,
            // nem valamivel utána – ez adja a „3–2–1” egyenletes ütemét.
            if (before != then)
                assertEquals("nem a váltáskor ébredtünk: " + remain + " -> " + after,
                        0, after % 1000);
        }
    }

    @Test public void aFinishedStepDoesNotWait() {
        assertEquals(0, TimerService.nextTickDelay(0));
        assertEquals(0, TimerService.nextTickDelay(-5));
    }

    @Test public void aNaturalStepChangeMeasuresFromThePlannedEnd() {
        // 12 ms-ot késett az ébredés: a következő szakasz mégis a tervezett
        // végtől induljon, különben ez a 12 ms minden körben újra hozzáadódna.
        assertEquals(1000, TimerService.stepBase(1012, 1000, false));
        // Az első szakasznak nincs mihez igazodnia.
        assertEquals(1012, TimerService.stepBase(1012, 1000, true));
        // Átugrásnál a szakasz vége még a jövőben van – ott a mostani pillanat kell.
        assertEquals(1012, TimerService.stepBase(1012, 5000, false));
        // Hosszabb kimaradás után sem húzzuk vissza a múltba a kezdést.
        assertEquals(9000, TimerService.stepBase(9000, 1000, false));
    }

    @Test public void aTwentyRoundWorkoutStaysOnSchedule() {
        long[] r = simulate(20, 45, 15, 3);
        assertTrue("a sípok közti szünet ingadozik: " + r[0] + " ms", r[0] <= 20);
        assertTrue("elcsúszott a terv: " + r[1] + " ms", r[1] <= 50);
        // Minden szakasz visszaszámol: a munka is, a pihenő is.
        assertEquals("kimaradt vagy megduplázódott egy síp", 20 * 2 * 3, r[2]);
    }

    @Test public void shortIntervalsStayOnScheduleToo() {
        long[] r = simulate(12, 20, 10, 5);
        assertTrue("a sípok közti szünet ingadozik: " + r[0] + " ms", r[0] <= 20);
        assertTrue("elcsúszott a terv: " + r[1] + " ms", r[1] <= 50);
        assertEquals("kimaradt vagy megduplázódott egy síp", 12 * 2 * 5, r[2]);
    }

    /**
     * Végigjátssza az edzést. Minden ébredés késik egy keveset, ahogy a valódi
     * ütemező is. Visszaadja: a sípok közti szünet legnagyobb eltérése az egy
     * másodperctől, a szakaszvégek legnagyobb csúszása a tervhez képest, és a
     * sípok száma.
     */
    private static long[] simulate(int rounds, int work, int rest, int cd) {
        Random rnd = new Random(7);
        long start = 12_345;
        long now = start;
        int[] durs = new int[rounds * 2];
        for (int i = 0; i < rounds; i++) { durs[2 * i] = work; durs[2 * i + 1] = rest; }

        long stepEnd = TimerService.stepBase(now, 0, true) + durs[0] * 1000L;
        List<Long> beeps = new ArrayList<>();
        long worstEnd = 0, planned = start;
        int idx = 0, lastShown = -1;
        while (true) {
            long remain = stepEnd - now;
            if (remain <= 0) {
                planned += durs[idx] * 1000L;
                worstEnd = Math.max(worstEnd, Math.abs(now - planned));
                if (++idx >= durs.length) break;
                stepEnd = TimerService.stepBase(now, stepEnd, false) + durs[idx] * 1000L;
                lastShown = -1;
                now += 40 + rnd.nextInt(6);
                continue;
            }
            int shown = (int) Math.ceil(remain / 1000.0);
            if (shown != lastShown) {
                lastShown = shown;
                if (shown <= cd && shown >= 1) beeps.add(now);
            }
            now += TimerService.nextTickDelay(remain) + rnd.nextInt(6);
        }
        long worstGap = 0;
        for (int i = 1; i < beeps.size(); i++) {
            long gap = beeps.get(i) - beeps.get(i - 1);
            if (gap > 2000) continue;             // két szakasz közti szünet
            worstGap = Math.max(worstGap, Math.abs(gap - 1000));
        }
        return new long[]{worstGap, worstEnd, beeps.size()};
    }
}
