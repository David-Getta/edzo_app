package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * Az edzés menetrendje. Ugyanez a szabály adja a kezdőlapon kiírt „Teljes időt"
 * is – ha a kettő szétcsúszna, az app mást ígérne, mint amit lejátszik.
 *
 * A kényes pont a pihenő: csak a munka-szakaszok KÖZÉ jár, az utolsó után nem.
 */
public class TimerPlanTest {

    private static List<TimerService.Step> plan(int warm, int prep, int work, int rest,
                                                int rounds, int exCount, int cool) {
        String[] names = exCount > 1 ? new String[exCount] : null;
        return TimerService.buildPlan(warm, prep, work, rest, rounds, names, cool);
    }

    private static int count(List<TimerService.Step> p, int type) {
        int n = 0;
        for (TimerService.Step s : p) if (s.type == type) n++;
        return n;
    }

    // --- Felépítés ---

    @Test public void aPlainIntervalWorkoutHasNoRestAfterTheLastRound() {
        List<TimerService.Step> p = plan(0, 10, 30, 15, 4, 1, 0);
        assertEquals("4 munka-szakasz", 4, count(p, TimerService.T_WORK));
        assertEquals("csak közéjük jár pihenő", 3, count(p, TimerService.T_REST));
        assertEquals(TimerService.T_PREP, p.get(0).type);
        assertEquals("az utolsó szakasz munka legyen",
                TimerService.T_WORK, p.get(p.size() - 1).type);
    }

    @Test public void warmUpAndCoolDownBracketTheWorkout() {
        List<TimerService.Step> p = plan(60, 10, 30, 15, 3, 1, 120);
        assertEquals(TimerService.T_WARMUP, p.get(0).type);
        assertEquals(TimerService.T_PREP, p.get(1).type);
        assertEquals(TimerService.T_COOLDOWN, p.get(p.size() - 1).type);
    }

    @Test public void zeroLengthSectionsAreLeftOut() {
        List<TimerService.Step> p = plan(0, 0, 30, 0, 3, 1, 0);
        assertEquals(3, p.size());
        assertEquals(0, count(p, TimerService.T_REST));
        assertEquals(0, count(p, TimerService.T_WARMUP));
        assertEquals(0, count(p, TimerService.T_PREP));
    }

    @Test public void everyExerciseOfEveryRoundGetsItsOwnSection() {
        // 6 gyakorlat × 3 kör = 18 munka-szakasz, köztük 17 pihenő.
        List<TimerService.Step> p = plan(0, 10, 40, 20, 3, 6, 0);
        assertEquals(18, count(p, TimerService.T_WORK));
        assertEquals(17, count(p, TimerService.T_REST));
    }

    @Test public void roundNumbersRunFromOneToTheLast() {
        List<TimerService.Step> p = plan(0, 0, 30, 10, 3, 2, 0);
        int firstWork = -1, lastWork = -1;
        for (TimerService.Step s : p) {
            if (s.type != TimerService.T_WORK) continue;
            if (firstWork < 0) firstWork = s.round;
            lastWork = s.round;
        }
        assertEquals(1, firstWork);
        assertEquals(3, lastWork);
    }

    @Test public void aSingleRoundIsJustOneWorkSection() {
        List<TimerService.Step> p = plan(0, 0, 30, 15, 1, 1, 0);
        assertEquals(1, p.size());
        assertEquals(0, count(p, TimerService.T_REST));
    }

    // --- A kiírt idő ---

    @Test public void theTotalIsExactlyTheSumOfTheSections() {
        int[][] setups = {
                {0, 10, 30, 15, 4, 1, 0},
                {60, 10, 30, 15, 8, 1, 120},
                {0, 0, 45, 0, 5, 1, 0},
                {30, 5, 40, 20, 3, 6, 60},
                {0, 10, 20, 10, 1, 12, 0},
        };
        for (int[] s : setups) {
            int sum = 0;
            for (TimerService.Step st : plan(s[0], s[1], s[2], s[3], s[4], s[5], s[6]))
                sum += st.dur;
            assertEquals("összeg és menetrend eltér",
                    sum, TimerService.totalSeconds(s[0], s[1], s[2], s[3], s[4], s[5], s[6]));
        }
    }

    @Test public void theTotalMatchesHandCalculation() {
        // 10 előkészület + 4×30 munka + 3×15 pihenő = 175 mp
        assertEquals(175, TimerService.totalSeconds(0, 10, 30, 15, 4, 1, 0));
        // Bemelegítéssel és levezetéssel: +60 +120
        assertEquals(355, TimerService.totalSeconds(60, 10, 30, 15, 4, 1, 120));
        // 6 gyakorlat × 3 kör: 18×40 + 17×20 = 1060, plusz 10 előkészület
        assertEquals(1070, TimerService.totalSeconds(0, 10, 40, 20, 3, 6, 0));
    }

    @Test public void moreRoundsNeverMeanLessTime() {
        int prev = 0;
        for (int r = 1; r <= 20; r++) {
            int t = TimerService.totalSeconds(0, 10, 30, 15, r, 1, 0);
            assertTrue("a " + r + ". körnél csökkent az idő", t > prev);
            prev = t;
        }
    }

    @Test public void zeroRoundsIsNotNegativeTime() {
        // Elvileg nem állítható be, de a képlet ne adjon értelmetlen eredményt.
        assertEquals(0, TimerService.totalSeconds(0, 0, 30, 15, 0, 1, 0));
        assertEquals(70, TimerService.totalSeconds(60, 10, 30, 15, 0, 1, 0));
    }
}
