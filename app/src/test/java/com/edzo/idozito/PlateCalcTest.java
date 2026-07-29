package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Súlytárcsa- és 1RM-kalkulátor. Itt egy hiba nem elszámolt statisztika, hanem
 * rossz súly a rúdon – ezért a kiírt tárcsáknak létező tárcsáknak kell lenniük,
 * és az összegüknek pontosan ki kell adnia a célt.
 */
public class PlateCalcTest {

    private static final double[] REAL_PLATES = {25, 20, 15, 10, 5, 2.5, 1.25};

    @Test public void theSmallestPlateIsNamedCorrectly() {
        // A rossz formázás „1,3 kg"-ot írt az 1,25 kg-os tárcsára – ilyen tárcsa
        // nem létezik, és a felhasználó hiába keresi a állványon.
        String plan = StrengthActivity.platePlan(22.5, 20);
        assertTrue("az 1,25-nek pontosan kell látszania: " + plan, plan.contains("1,25"));
        assertFalse("ne kerekítsen 1,3-ra: " + plan, plan.contains("1,3"));
    }

    @Test public void aTypicalLoadIsSplitIntoRealPlates() {
        // 100 kg, 20 kg-os rúd → oldalanként 40 = 25 + 15
        String plan = StrengthActivity.platePlan(100, 20);
        assertTrue(plan, plan.contains("1×25"));
        assertTrue(plan, plan.contains("1×15"));
    }

    @Test public void theBareBarNeedsNoPlates() {
        assertTrue(StrengthActivity.platePlan(20, 20).contains("nincs tárcsa"));
    }

    @Test public void impossibleInputsAreExplainedNotComputed() {
        assertTrue(StrengthActivity.platePlan(0, 20).contains("cél súlyt"));
        assertTrue(StrengthActivity.platePlan(-5, 20).contains("cél súlyt"));
        assertTrue(StrengthActivity.platePlan(15, 20).contains("nehezebb"));
    }

    @Test public void aWeightThatCannotBeMadeSaysSo() {
        // 21 kg egy 20 kg-os rúddal: fél kiló oldalanként nem rakható ki.
        String plan = StrengthActivity.platePlan(21, 20);
        assertTrue("jelezze, hogy nem jön ki: " + plan, plan.contains("nem jön ki"));
    }

    @Test public void everyLoadableWeightAddsUpExactly() {
        // Végigmegyünk minden kirakható súlyon 20-tól 300-ig, 2,5 kg-onként:
        // a kiírt tárcsák összege pontosan a cél legyen, maradék-figyelmeztetés nélkül.
        for (double t = 22.5; t <= 300; t += 2.5) {
            String plan = StrengthActivity.platePlan(t, 20);
            assertFalse("maradékot jelez egy kirakható súlynál (" + t + "): " + plan,
                    plan.contains("nem jön ki"));
            assertEquals("a tárcsák összege nem a célt adja (" + t + "): " + plan,
                    (t - 20) / 2.0, sumOf(plan), 0.001);
        }
    }

    /**
     * A terv sora (a fejléc és az esetleges maradék-figyelmeztetés nélkül).
     * Enélkül a „…marad 0,62 kg/oldal" szövege beleragadna a súlyok elemzésébe.
     */
    private static String planLine(String result) {
        String[] lines = result.split("\n");
        return lines.length > 1 ? lines[1] : "";
    }

    /** A tervben szereplő „n×súly" tagok összege. */
    private static double sumOf(String result) {
        double sum = 0;
        for (String part : planLine(result).split("\\+")) {
            int x = part.indexOf('×');
            if (x < 0) continue;
            sum += count(part, x) * weight(part, x);
        }
        return sum;
    }

    private static int count(String part, int x) {
        return Integer.parseInt(part.substring(0, x).trim());
    }

    private static double weight(String part, int x) {
        return Double.parseDouble(part.substring(x + 1).trim().replace(',', '.'));
    }

    @Test public void onlyRealPlatesAppear() {
        for (double t = 22.5; t <= 200; t += 1.25) {
            for (String part : planLine(StrengthActivity.platePlan(t, 20)).split("\\+")) {
                int x = part.indexOf('×');
                if (x < 0) continue;
                double w = weight(part, x);
                boolean real = false;
                for (double p : REAL_PLATES) if (Math.abs(p - w) < 0.001) real = true;
                assertTrue("nem létező tárcsa: " + w + " kg (" + t + " kg-nál)", real);
            }
        }
    }

    // --- 1RM ---

    @Test public void oneRmFollowsEpley() {
        // 100 kg × 5 ismétlés → 100 × (1 + 5/30) = 116,7
        String s = StrengthActivity.oneRmPlan(100, 5);
        assertTrue(s, s.contains("116,7"));
    }

    @Test public void oneRmOfASingleRepIsTheWeightItself() {
        String s = StrengthActivity.oneRmPlan(100, 1);
        assertTrue(s, s.contains("103,3"));   // 100 × (1 + 1/30)
    }

    @Test public void oneRmNeedsBothNumbers() {
        assertTrue(StrengthActivity.oneRmPlan(0, 5).contains("Adj meg"));
        assertTrue(StrengthActivity.oneRmPlan(100, 0).contains("Adj meg"));
    }

    @Test public void thePercentageTableGoesDownFrom95To60() {
        String s = StrengthActivity.oneRmPlan(100, 1);
        for (int p : new int[]{95, 90, 85, 80, 75, 70, 65, 60})
            assertTrue("hiányzik a " + p + "%: " + s, s.contains(p + "%"));
    }
}
