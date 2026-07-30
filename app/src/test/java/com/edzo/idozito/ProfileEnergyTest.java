package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Napi kalóriaszükséglet és a fogyáshoz javasolt bevitel.
 *
 * Az aktivitási szorzó három helyen szerepelt, KÉT különböző értékkel: az
 * Étrend képernyő 1,35-tel, a Profil 1,4-gyel számolt. Ugyanaz a felhasználó
 * két különböző napi szükségletet látott a két képernyőn – és az Étrend
 * ugyanabban a párbeszédablakban kínálta a fenntartó és a fogyós értéket, így
 * a kettő különbsége nem is a beállított kalóriahiány volt.
 */
public class ProfileEnergyTest {

    /** 80 kg, 180 cm, 30 éves férfi. */
    private static final double BMR_M = Profile.bmr(0, 80, 180, 30);

    @Test public void theMifflinFormulaMatchesTheBook() {
        // 10×80 + 6,25×180 − 5×30 + 5 = 1780
        assertEquals(1780, BMR_M, 0.01);
        // Nőnél ugyanez −161: 10×65 + 6,25×168 − 5×30 − 161 = 1389
        assertEquals(1389, Profile.bmr(1, 65, 168, 30), 0.01);
    }

    @Test public void theGapBetweenMaintenanceAndLossIsExactlyTheDeficit() {
        // Ez a lényeg: aki utánaszámol, annak ki kell jönnie.
        for (int rate = 0; rate < Profile.RATES.length; rate++)
            assertEquals("nem a beállított hiány a különbség (tempó " + rate + ")",
                    Profile.dailyDeficit(rate),
                    Profile.tdee(BMR_M) - Profile.intakeForLoss(BMR_M, rate), 0.001);
    }

    @Test public void theDeficitFollowsTheChosenRate() {
        // 0,5 kg/hét → 7700/2 kcal egy hétre, elosztva héttel.
        assertEquals(0.5 * 7700 / 7, Profile.dailyDeficit(1), 0.001);
        assertEquals(0.25 * 7700 / 7, Profile.dailyDeficit(0), 0.001);
        // Gyorsabb tempó nagyobb hiány, és a bevitel monoton csökken.
        double prev = Double.MAX_VALUE;
        for (int r = 0; r < Profile.RATES.length; r++) {
            double intake = Profile.intakeForLoss(BMR_M, r);
            assertTrue("a gyorsabb tempó nem kevesebb bevitel", intake < prev);
            prev = intake;
        }
    }

    @Test public void anOutOfRangeRateIndexDoesNotCrash() {
        // A tempó a beállításokból jön, akár mentésfájlból visszaállítva.
        for (int r : new int[]{-5, -1, 0, 3, 4, 99, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            double d = Profile.dailyDeficit(r);
            assertTrue("értelmetlen kalóriahiány: " + d, d > 0 && d < 2000);
        }
    }

    @Test public void withoutTheProfileDataThereIsNoSuggestion() {
        // Hiányos adatnál a BMR -1, és onnantól nem találunk ki semmit.
        assertEquals(-1, Profile.bmr(0, 0, 180, 30), 0.01);
        assertEquals(-1, Profile.bmr(0, 80, 0, 30), 0.01);
        assertEquals(-1, Profile.tdee(-1), 0.01);
        assertEquals(-1, Profile.intakeForLoss(-1, 1), 0.01);
    }

    @Test public void theSuggestedIntakeIsInARealisticRange() {
        // Enyhén aktív felnőtt: a fenntartó érték a BMR fölött, de nem duplája.
        assertTrue(Profile.tdee(BMR_M) > BMR_M);
        assertTrue(Profile.tdee(BMR_M) < BMR_M * 2);
        assertEquals(BMR_M * Profile.ACTIVITY, Profile.tdee(BMR_M), 0.001);
    }
}
