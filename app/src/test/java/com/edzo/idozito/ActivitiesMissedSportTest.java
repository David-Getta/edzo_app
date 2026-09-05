package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A „rég volt kézilabda" sor a napi biztatásban.
 *
 * Az elv: csak arról beszélünk, ami láthatóan szokás (legalább három alkalom
 * az elmúlt 30 napban), és tényleg kimaradt (legalább egy hete). Ebből a
 * sorból tévedni rosszabb, mint hallgatni – aki most kezdett úszni, annak ne
 * mondjuk három nap után, hogy „rég volt".
 */
public class ActivitiesMissedSportTest {

    private static final long DAY = 24L * 3600 * 1000;
    private static final long NOW = 1_753_900_000_000L;

    /** Napló-sorok: {kind, name, hány napja}. */
    private static String call(Object[][] rows) {
        String[] k = new String[rows.length];
        String[] n = new String[rows.length];
        long[] t = new long[rows.length];
        for (int i = 0; i < rows.length; i++) {
            k[i] = (String) rows[i][0];
            n[i] = (String) rows[i][1];
            t[i] = NOW - ((Integer) rows[i][2]) * DAY;
        }
        return Activities.missedSport(k, n, t, NOW);
    }

    @Test public void aRegularSportMissingForAWeekIsNamed() {
        String line = call(new Object[][]{
                {"kezilabda", "🤾 Kézilabda", 9}, {"kezilabda", "🤾 Kézilabda", 12},
                {"kezilabda", "🤾 Kézilabda", 16}, {"kezilabda", "🤾 Kézilabda", 23},
                {"futas", "🏃 Futás", 1}, {"futas", "🏃 Futás", 3},
        });
        assertTrue("nem nevezi meg a sportot: " + line, line != null && line.contains("Kézilabda"));
        assertTrue("nem mondja, mióta: " + line, line.contains("9 napja"));
    }

    @Test public void aSportDoneRecentlyStaysQuiet() {
        assertNull(call(new Object[][]{
                {"kezilabda", "x", 2}, {"kezilabda", "x", 5}, {"kezilabda", "x", 9},
        }));
    }

    @Test public void anOccasionalSportIsNotAHabit() {
        // Két alkalom nem szokás – arról nincs mit számonkérni.
        assertNull(call(new Object[][]{
                {"uszas", "x", 10}, {"uszas", "x", 20},
        }));
    }

    @Test public void measuredRunsCountAsRunning() {
        // A mért futásnak nincs kind/name mezője – attól még futás.
        String line = call(new Object[][]{
                {"", "", 8}, {"", "", 14}, {"", "", 20},
        });
        assertTrue("a mért futás nem számít: " + line, line != null && line.contains("Futás"));
    }

    @Test public void namedTimerWorkoutsAreNotASport() {
        // A programos időzítős edzés nem sportág – abból nem lesz hiány-sor.
        assertNull(call(new Object[][]{
                {"", "Zsírégető HIIT", 10}, {"", "Zsírégető HIIT", 15}, {"", "Zsírégető HIIT", 20},
        }));
    }

    @Test public void aRecognizableProgramNameCountsAsTheSport() {
        // A „Kézilabda edzés" nevű időzítős edzés kézilabda-szokás, akkor is,
        // ha nincs kind mezője – a név elárulja.
        String line = call(new Object[][]{
                {"", "Kézilabda edzés", 8}, {"", "Kézilabda edzés", 14},
                {"", "Kézilabda edzés", 20},
        });
        assertTrue("a névből felismert sport nem számít: " + line,
                line != null && line.contains("Kézilabda"));
    }

    @Test public void theMostFrequentHabitWins() {
        String line = call(new Object[][]{
                {"kezilabda", "x", 8}, {"kezilabda", "x", 12}, {"kezilabda", "x", 16},
                {"kezilabda", "x", 20}, {"joga", "x", 9}, {"joga", "x", 15}, {"joga", "x", 22},
        });
        assertTrue("nem a gyakoribb szokást választotta: " + line, line.contains("Kézilabda"));
    }

    @Test public void emptyOrOldHistoryGivesNothing() {
        assertNull(call(new Object[][]{}));
        // 60 napnál régebbi alkalmak már nem szokás.
        assertNull(call(new Object[][]{
                {"foci", "x", 70}, {"foci", "x", 80}, {"foci", "x", 90},
        }));
    }

    @Test public void theLineIsCompleteHungarian() {
        String line = call(new Object[][]{
                {"foci", "x", 10}, {"foci", "x", 17}, {"foci", "x", 24},
        });
        assertEquals("⚽ Foci: 10 napja kimaradt – ideje újra!", line);
    }
    /**
     * Hátravetett tagadás: „futni nem voltam".
     *
     * A „nem" előre töröl – ez a „nem futottam" alakra jó. Magyarul viszont
     * ugyanolyan gyakori a fordított szórend, és ott a mozgás a tagadás ELŐTT
     * áll: minden ilyen mondat bejegyzést csinált abból, amit az ember épp NEM
     * csinált meg.
     */
    @Test public void aTrailingNegationAlsoCancelsTheWorkout() {
        long now = 1_753_869_600_000L;
        for (String q : new String[]{"futni nem voltam", "kondizni nem voltam",
                "úszni nem mentem", "futni nem mentem", "edzeni nem voltam",
                "nem voltam futni", "nem mentem úszni"})
            assertTrue(q + " -> " + Activities.parse(q, now).plans,
                    Activities.parse(q, now).plans.isEmpty());
        // Az állító alakok érintetlenek.
        for (String q : new String[]{"futni voltam", "voltam futni", "futás"})
            assertEquals(q, "futas", Activities.parse(q, now).plans.get(0).kind.id);
        // A tagadás nem eszi meg az előtte álló, MEGTÖRTÉNT edzést.
        Activities.Parsed p =
                Activities.parse("reggel és este 30-30 perc kondi, de futni nem voltam", now);
        assertEquals(1, p.plans.size());
        assertEquals("kondi", p.plans.get(0).kind.id);
        assertEquals(2, p.plans.get(0).count);
        assertEquals(30, p.plans.get(0).minutes);
    }
}
