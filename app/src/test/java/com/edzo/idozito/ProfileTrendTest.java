package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Testsúly-tendencia.
 *
 * Két mérés különbsége félrevezet: a napi ingadozás (étel, víz, napszak) ±1 kg
 * is lehet, egy „rossz" reggel eltüntetne egy hét munkáját. Ezért lineáris
 * illesztés az ÖSSZES mérésre – az egyenes meredeksége a valódi irány.
 */
public class ProfileTrendTest {

    @Test public void aSteadyLossGivesTheExactRate() {
        // Napi 10 dkg fogyás = heti 0,7 kg.
        double[] days = {0, 7, 14, 21, 28};
        double[] kg = {90, 89.3, 88.6, 87.9, 87.2};
        assertEquals(-0.7, Profile.weeklyTrend(days, kg), 0.001);
    }

    @Test public void dailyNoiseDoesNotFlipTheTrend() {
        // Ugyanaz a fogyás, ±0,8 kg zajjal: az irány marad, a nagyságrend is.
        double[] days = {0, 3, 7, 10, 14, 17, 21, 24, 28};
        double[] kg = {90.4, 89.8, 89.6, 89.5, 88.5, 88.9, 88.0, 87.6, 87.4};
        double t = Profile.weeklyTrend(days, kg);
        assertTrue("nem fogyásnak látszik: " + t, t < -0.4 && t > -1.0);
        // Csak az első és az utolsó mérésből ennél nagyobb hibát is kaphatnánk.
    }

    @Test public void gainIsPositiveAndFlatIsZero() {
        assertTrue(Profile.weeklyTrend(new double[]{0, 7, 14}, new double[]{70, 70.5, 71}) > 0.4);
        assertEquals(0, Profile.weeklyTrend(new double[]{0, 7, 14},
                new double[]{80, 80, 80}), 0.0001);
    }

    @Test public void notEnoughDataGivesZero() {
        assertEquals(0, Profile.weeklyTrend(null, null), 0.0001);
        assertEquals(0, Profile.weeklyTrend(new double[]{1}, new double[]{80}), 0.0001);
        // Eltérő hosszú tömbök: inkább semmi, mint kitalált szám.
        assertEquals(0, Profile.weeklyTrend(new double[]{1, 2}, new double[]{80}), 0.0001);
        // Minden mérés ugyanazon a napon: nincs meredekség.
        assertEquals(0, Profile.weeklyTrend(new double[]{5, 5, 5},
                new double[]{80, 81, 79}), 0.0001);
    }

    @Test public void absurdRatesAreCapped() {
        // Elgépelt mérés (8 vs 80 kg) ne adjon heti 50 kg-os „trendet".
        double t = Profile.weeklyTrend(new double[]{0, 1, 2}, new double[]{80, 8, 80});
        assertTrue("nincs korlátozva: " + t, t >= -3 && t <= 3);
    }

    @Test public void theEstimateOnlyRunsWhenItActuallyHelps() {
        // 4 kg hátra, heti 0,5 kg: nyolc hét.
        assertEquals(8, Profile.weeksToGoal(4, -0.5), 0.001);
        // A cél megvan, vagy nem fogy, vagy hízik: nincs becslés.
        assertEquals(-1, Profile.weeksToGoal(0, -0.5), 0.001);
        assertEquals(-1, Profile.weeksToGoal(4, 0), 0.001);
        assertEquals(-1, Profile.weeksToGoal(4, 0.3), 0.001);
        // Öt évnél távolabbi ígéretet nem teszünk.
        assertEquals(-1, Profile.weeksToGoal(30, -0.05), 0.001);
    }
}
