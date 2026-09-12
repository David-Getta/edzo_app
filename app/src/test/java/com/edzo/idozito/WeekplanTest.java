package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Heti fókusz.
 *
 * A tárolt szöveg elrontása a heti tervet némítaná el, ezért a
 * körbeírás–visszaolvasás mindig ugyanazt kell adja.
 */
public class WeekplanTest {

    @Test public void aPlanSurvivesSavingAndLoading() {
        String[] in = {"Láb", "", "Hát", "", "Mell", "", ""};
        String csv = Weekplan.format(in);
        String[] back = Weekplan.parse(csv);
        assertEquals(7, back.length);
        for (int i = 0; i < 7; i++) assertEquals(in[i], back[i]);
        assertEquals("Láb", Weekplan.forDay(csv, 0));
        assertEquals("Mell", Weekplan.forDay(csv, 4));
        assertEquals("", Weekplan.forDay(csv, 6));
    }

    @Test public void anEmptyPlanIsReallyEmpty() {
        // Hat vessző nem terv: a „van-e terv" egy isEmpty()-vel eldönthető.
        assertEquals("", Weekplan.format(new String[]{"", "", "", "", "", "", ""}));
        assertEquals("", Weekplan.format(null));
        assertFalse(Weekplan.any(""));
        assertFalse(Weekplan.any(null));
        assertFalse(Weekplan.any(",,,,,,"));
        assertTrue(Weekplan.any(Weekplan.format(new String[]{"", "", "Hát", "", "", "", ""})));
    }

    @Test public void theSeparatorCannotBreakThePlan() {
        // A vessző elválasztó: ha bekerülne a szövegbe, egy nap kettévágná a hetet.
        String csv = Weekplan.format(new String[]{"Láb, hát", "", "", "", "", "", ""});
        assertEquals("Láb hát", Weekplan.forDay(csv, 0));
        assertEquals(7, Weekplan.parse(csv).length);
        // Sortörés és dupla szóköz sem marad benne.
        assertEquals("Mell tricepsz",
                Weekplan.forDay(Weekplan.format(new String[]{"Mell\n  tricepsz",
                        "", "", "", "", "", ""}), 0));
        // A túl hosszú szöveg levágódik – a kártyán egy sorba kell férnie.
        String longText = "Láb és hát és mell és váll és kar és törzs";
        assertTrue(Weekplan.forDay(Weekplan.format(new String[]{longText,
                "", "", "", "", "", ""}), 0).length() <= Weekplan.MAX_LEN);
    }

    @Test public void brokenInputNeverCrashes() {
        for (String csv : new String[]{null, "", "   ", "Láb", "a,b,c",
                "1,2,3,4,5,6,7,8,9,10", ",,,,,,,,,,"}) {
            String[] f = Weekplan.parse(csv);
            assertEquals(7, f.length);
            for (String s : f) assertTrue(s != null);
            assertTrue(Weekplan.summary(csv) != null);
            for (int d = -2; d < 9; d++) assertTrue(Weekplan.forDay(csv, d) != null);
        }
    }

    @Test public void theSummaryListsOnlyTheFilledDays() {
        String csv = Weekplan.format(new String[]{"Láb", "", "Hát", "", "Mell", "", ""});
        assertEquals("H: Láb  ·  Sze: Hát  ·  P: Mell", Weekplan.summary(csv));
        assertEquals("", Weekplan.summary(""));
    }

    @Test public void theHomeLineFallsBackToTomorrow() {
        String csv = Weekplan.format(new String[]{"Láb", "", "Hát", "", "Mell", "", ""});
        assertEquals("📋  Ma: Láb", Weekplan.todayLine(csv, 0));
        // Kedden nincs fókusz, de szerdán van: a „mire készülj" is ér annyit.
        assertEquals("📋  Holnap: Hát", Weekplan.todayLine(csv, 1));
        // Vasárnap után hétfő jön – a hét körbeér.
        assertEquals("📋  Holnap: Láb", Weekplan.todayLine(csv, 6));
        // Csütörtökön sincs, és pénteken van.
        assertEquals("📋  Holnap: Mell", Weekplan.todayLine(csv, 3));
        assertEquals("", Weekplan.todayLine("", 0));
    }

    @Test public void thePlanAdherenceCountsWhatWasActuallyTrained() {
        String csv = Weekplan.format(new String[]{"Láb", "", "Hát", "", "Mell", "", ""});
        String[] groups = {"Láb,Kar", "", "Váll", "", "Mell", "", ""};
        boolean[] trained = {true, false, true, false, true, false, false};
        int[] a = Weekplan.adherence(csv, groups, trained);
        // Hétfő: láb megvolt (kar mellé is fért) ✔; szerda: vállat edzett ✘;
        // péntek: mell ✔.
        assertEquals(2, a[0]);
        assertEquals(3, a[1]);
    }

    @Test public void aNonMuscleFocusCountsAsAnyTraining() {
        // A „Kardió" nem izomcsoport: ott elég, hogy volt edzés.
        String csv = Weekplan.format(new String[]{"Kardió", "", "", "", "", "", ""});
        assertEquals(1, Weekplan.adherence(csv, new String[]{""},
                new boolean[]{true, false, false, false, false, false, false})[0]);
        assertEquals(0, Weekplan.adherence(csv, new String[]{""},
                new boolean[]{false, false, false, false, false, false, false})[0]);
    }

    @Test public void adherenceIsSafeWithoutData() {
        assertEquals(0, Weekplan.adherence("", null, null)[1]);
        String csv = Weekplan.format(new String[]{"Láb", "", "", "", "", "", ""});
        int[] a = Weekplan.adherence(csv, null, null);
        assertEquals(0, a[0]);
        assertEquals(1, a[1]);
        // Rövidebb tömbök sem dobnak hibát.
        assertEquals(1, Weekplan.adherence(csv, new String[]{"Láb"}, new boolean[]{true})[0]);
    }
}
