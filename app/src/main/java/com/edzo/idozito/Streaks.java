package com.edzo.idozito;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;

/**
 * Közös, terv-tudatos („okos") napi széria-számítás. Ha be vannak állítva
 * edzésnapok, a pihenőnap nem töri meg a szériát – csak a kihagyott tervezett
 * edzésnap. Terv nélkül a hagyományos, naptári számítás él. A pihenőnapok nem
 * is számítanak bele a szériába, csak átugorja őket a számláló.
 */
public final class Streaks {

    private Streaks() {}

    /** Napi széria mával bezárólag; a mai nap türelmi idő (ha ma még nincs edzés, tegnaptól számol). */
    public static int current(Context c, JSONArray arr) {
        return count(c, arr, true);
    }

    /** Napi széria tegnappal bezárólag (a még élő, ma megmenthető széria). */
    public static int untilYesterday(Context c, JSONArray arr) {
        return count(c, arr, false);
    }

    private static int count(Context ctx, JSONArray arr, boolean includeToday) {
        return count(planOf(ctx), arr, includeToday);
    }

    /**
     * A tervezett edzésnapok táblája, vagy null, ha nincs terv. Külön kiemelve,
     * hogy a számítás Context nélkül is meghívható (és tesztelhető) legyen.
     */
    private static boolean[] planOf(Context ctx) {
        if (Theme.planDays(ctx).isEmpty()) return null;
        boolean[] p = new boolean[7];
        for (int i = 0; i < 7; i++) p[i] = Theme.isPlanDay(ctx, i);
        return p;
    }

    /** plan == null: nincs edzésnap-terv, ilyenkor bármely kihagyott nap megtöri. */
    static int count(boolean[] plan, JSONArray arr, boolean includeToday) {
        HashSet<Long> days = daySet(arr);
        Calendar cur = Calendar.getInstance();
        zero(cur);
        // Naptári léptetéssel megyünk visszafelé (óraátállás-biztos).
        if (!includeToday || !days.contains(cur.getTimeInMillis()))
            cur.add(Calendar.DAY_OF_YEAR, -1);
        int s = 0;
        for (int k = 0; k < 730; k++) {
            if (days.contains(cur.getTimeInMillis())) {
                s++;
            } else {
                int dowIdx = (cur.get(Calendar.DAY_OF_WEEK) + 5) % 7; // H=0..V=6
                boolean restDay = plan != null && !plan[dowIdx];
                if (!restDay) break; // kihagyott edzésnap (vagy terv nélkül bármely nap) megtöri
            }
            cur.add(Calendar.DAY_OF_YEAR, -1);
        }
        return s;
    }

    /** A valaha volt leghosszabb (terv-tudatos) napi széria az előzményekben. */
    public static int best(Context ctx, JSONArray arr) {
        HashSet<Long> days = daySet(arr);
        if (days.isEmpty()) return 0;
        boolean hasPlan = !Theme.planDays(ctx).isEmpty();
        long min = Long.MAX_VALUE;
        for (Long d : days) if (d < min) min = d;
        Calendar cur = Calendar.getInstance();
        cur.setTimeInMillis(min);
        zero(cur);
        Calendar today = Calendar.getInstance();
        zero(today);
        long end = today.getTimeInMillis();
        int run = 0, best = 0;
        for (int k = 0; k < 3700 && cur.getTimeInMillis() <= end; k++) {
            if (days.contains(cur.getTimeInMillis())) {
                run++;
                if (run > best) best = run;
            } else {
                int dowIdx = (cur.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                boolean restDay = hasPlan && !Theme.isPlanDay(ctx, dowIdx);
                if (!restDay) run = 0;
            }
            cur.add(Calendar.DAY_OF_YEAR, 1);
        }
        return best;
    }

    /** Hány héten teljesült a teljes heti edzésterv (max 2 évre visszamenőleg). */
    public static int planWeeks(Context ctx, JSONArray arr) {
        if (Theme.planDays(ctx).isEmpty()) return 0;
        HashSet<Long> days = daySet(arr);
        Calendar today = Calendar.getInstance();
        zero(today);
        Calendar wk = Calendar.getInstance();
        zero(wk);
        wk.setFirstDayOfWeek(Calendar.MONDAY);
        wk.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        int count = 0;
        for (int w = 0; w < 104; w++) {
            boolean complete = true, anyPlanned = false;
            Calendar d = (Calendar) wk.clone();
            for (int i = 0; i < 7; i++) {
                int dowIdx = (d.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                if (Theme.isPlanDay(ctx, dowIdx)) {
                    anyPlanned = true;
                    // Jövőbeli terv-nap (folyó hét): a hét még nem zárható le.
                    if (d.getTimeInMillis() > today.getTimeInMillis()
                            || !days.contains(d.getTimeInMillis())) complete = false;
                }
                d.add(Calendar.DAY_OF_YEAR, 1);
            }
            if (anyPlanned && complete) count++;
            wk.add(Calendar.DAY_OF_YEAR, -7);
        }
        return count;
    }

    private static HashSet<Long> daySet(JSONArray arr) {
        HashSet<Long> days = new HashSet<>();
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            c.setTimeInMillis(o.optLong("ts"));
            zero(c);
            days.add(c.getTimeInMillis());
        }
        return days;
    }

    private static void zero(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }
}
