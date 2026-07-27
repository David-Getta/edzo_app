package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * BMI és alap-anyagcsere. Ezekre épül a kalóriacél-ajánlás és a vízcél is,
 * tehát egy elgépelt együttható végigfut az egész étrend-részen.
 */
public class ProfileTest {

    // --- BMI ---

    @Test public void bmiIsWeightOverHeightSquared() {
        // 180 cm, 81 kg → 81 / 1,8² = 25,0
        assertEquals(25.0, Profile.bmi(180, 81), 0.01);
        assertEquals(22.86, Profile.bmi(175, 70), 0.01);
    }

    @Test public void bmiNeedsBothNumbers() {
        assertEquals(-1, Profile.bmi(0, 70), 0.001);
        assertEquals(-1, Profile.bmi(180, 0), 0.001);
        assertEquals(-1, Profile.bmi(-180, -70), 0.001);
    }

    @Test public void bmiCategoriesMatchTheUsualBoundaries() {
        assertEquals("sovány", Profile.bmiCategory(18.49));
        assertEquals("normál", Profile.bmiCategory(18.5));
        assertEquals("normál", Profile.bmiCategory(24.99));
        assertEquals("túlsúly", Profile.bmiCategory(25));
        assertEquals("elhízás (I)", Profile.bmiCategory(30));
        assertEquals("elhízás (II+)", Profile.bmiCategory(35));
        // Hiányzó adatra ne írjunk ki kategóriát.
        assertEquals("", Profile.bmiCategory(-1));
    }

    // --- BMR (Mifflin–St Jeor) ---

    @Test public void bmrFollowsMifflinStJeor() {
        // Férfi: 10·W + 6,25·H − 5·A + 5 → 30 éves, 180 cm, 80 kg
        assertEquals(10 * 80 + 6.25 * 180 - 5 * 30 + 5, Profile.bmr(0, 80, 180, 30), 0.01);
        // Nő: ugyanez −161 helyett +5
        assertEquals(10 * 65 + 6.25 * 165 - 5 * 30 - 161, Profile.bmr(1, 65, 165, 30), 0.01);
    }

    @Test public void womenGetALowerBmrThanMenWithTheSameBody() {
        assertTrue(Profile.bmr(1, 70, 175, 30) < Profile.bmr(0, 70, 175, 30));
    }

    @Test public void bmrRisesWithSizeAndFallsWithAge() {
        assertTrue(Profile.bmr(0, 90, 180, 30) > Profile.bmr(0, 70, 180, 30));
        assertTrue(Profile.bmr(0, 80, 190, 30) > Profile.bmr(0, 80, 170, 30));
        assertTrue(Profile.bmr(0, 80, 180, 60) < Profile.bmr(0, 80, 180, 20));
    }

    @Test public void bmrNeedsCompleteData() {
        assertEquals(-1, Profile.bmr(0, 0, 180, 30), 0.001);
        assertEquals(-1, Profile.bmr(0, 80, 0, 30), 0.001);
        assertEquals(-1, Profile.bmr(0, 80, 180, -1), 0.001);
    }

    @Test public void bmrStaysInAPlausibleRangeForRealPeople() {
        // Ha egy együttható elcsúszik, ez azonnal kiüt. A tartomány alja
        // szándékosan alacsony: egy alacsony, idős nőnél a képlet valóban
        // 850 körül jár – az még nem hiba, csak a szélső eset.
        for (int kg = 45; kg <= 130; kg += 5) {
            for (int cm = 150; cm <= 200; cm += 10) {
                for (int age = 18; age <= 80; age += 10) {
                    for (int sex = 0; sex <= 1; sex++) {
                        double b = Profile.bmr(sex, kg, cm, age);
                        assertTrue("irreális BMR: " + b + " (" + kg + "kg " + cm + "cm "
                                + age + "év)", b > 800 && b < 2600);
                    }
                }
            }
        }
    }

    // --- Fogyási ütemek ---

    @Test public void weightLossRatesAreOrderedAndSane() {
        double prev = 0;
        for (double r : Profile.RATES) {
            assertTrue("növekvő ütem", r > prev);
            assertTrue("heti 1 kg-nál nem javasolunk többet", r <= 1.0);
            prev = r;
        }
    }
}
