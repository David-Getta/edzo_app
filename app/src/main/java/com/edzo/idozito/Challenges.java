package com.edzo.idozito;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Napi kihívás: determinisztikus (a dátumból számolt) feladat, a haladást a mai
 * edzésekből méri. A típusok a szokásokhoz igazodnak: táv-kihívást csak az kap,
 * aki mér távot; ismétlés-kihívást csak az, aki vezet súlyzós naplót.
 */
public final class Challenges {

    private Challenges() {}

    /** A mai kihívás állapota: {title(String), unit(String), cur(int), target(int), seed(int)}. */
    public static Object[] state(Context ctx) {
        Calendar now = Calendar.getInstance();
        int seed = now.get(Calendar.YEAR) * 366 + now.get(Calendar.DAY_OF_YEAR);
        Calendar d0 = Calendar.getInstance();
        d0.set(Calendar.HOUR_OF_DAY, 0);
        d0.set(Calendar.MINUTE, 0);
        d0.set(Calendar.SECOND, 0);
        d0.set(Calendar.MILLISECOND, 0);
        long dayStart = d0.getTimeInMillis();

        JSONArray h = History.load(ctx);
        int minutes = 0, bestRounds = 0;
        double distToday = 0;
        boolean everDist = false;
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o == null) continue;
            if (o.optDouble("dist", -1) > 500) everDist = true;
            if (o.optLong("ts") < dayStart) continue;
            minutes += o.optInt("dur") / 60;
            bestRounds = Math.max(bestRounds, o.optInt("rounds", 0));
            double d = o.optDouble("dist", -1);
            if (d > 0) distToday += d;
        }
        int sessions = 0;
        JSONArray all = History.loadAll(ctx);
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o != null && o.optLong("ts") >= dayStart) sessions++;
        }
        List<StrengthLog.Entry> sLog = StrengthLog.load(ctx);
        int repsToday = 0;
        for (StrengthLog.Entry e : sLog)
            if (e.ts >= dayStart) repsToday += e.totalReps();

        List<MealLog.Meal> meals = MealLog.load(ctx);
        int mealsToday = 0;
        for (MealLog.Meal m : meals) if (m.ts >= dayStart) mealsToday++;

        List<Integer> types = new ArrayList<>(Arrays.asList(0, 1, 2));
        if (everDist) types.add(3);
        if (!sLog.isEmpty()) types.add(4);
        if (!meals.isEmpty()) types.add(5); // étrend-kihívás csak annak, aki naplóz
        int type = types.get(seed % types.size());

        String title;
        int cur, target;
        String unit;
        if (type == 0) {
            target = 15 + (seed / 3 % 3) * 5; cur = minutes; unit = "perc";
            title = "Mozogj ma összesen " + target + " percet!";
        } else if (type == 1) {
            target = 8 + (seed / 3 % 3) * 2; cur = bestRounds; unit = "kör";
            title = "Csinálj " + target + " kört egyetlen edzésen belül!";
        } else if (type == 2) {
            target = 2; cur = Math.min(sessions, 2); unit = "edzés";
            title = "Fejezz be ma 2 edzést – a súlyzós is számít!";
        } else if (type == 3) {
            target = 2 + (seed / 3 % 3); cur = (int) (distToday / 1000.0); unit = "km";
            title = "Tegyél meg ma " + target + " km-t!";
        } else if (type == 4) {
            target = 40 + (seed / 3 % 3) * 20; cur = repsToday; unit = "ismétlés";
            title = "Nyomj le ma összesen " + target + " ismétlést a súlyzós naplóban!";
        } else {
            target = 2; cur = Math.min(mealsToday, 2); unit = "étkezés";
            title = "Naplózz ma legalább 2 étkezést az Étrendben!";
        }
        return new Object[]{title, unit, cur, target, seed};
    }
}
