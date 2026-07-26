package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * Vízbevitel-követés. A napi mennyiség centiliterben, naponta külön
 * beállítás-kulcson („water_yyyyMMdd") – így napváltáskor magától nulláról
 * indul, és a biztonsági mentés is viszi. Egy pohár = 25 cl.
 */
public final class Water {

    private Water() {}

    static final String PREFS = "edzo";
    static final String DAY_PREFIX = "water_";
    static final String GOAL_KEY = "water_goal_cl";
    static final String LAST_DONE_KEY = "water_last_done";
    static final String DAYS_DONE_KEY = "water_days_done";

    public static final int GLASS_CL = 25;
    public static final int DEFAULT_GOAL_CL = 200;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Egy nap azonosítója számként: yyyyMMdd. */
    public static int dayNumber(Calendar cal) {
        return cal.get(Calendar.YEAR) * 10000
                + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
    }

    public static String dayKey(Calendar cal) { return DAY_PREFIX + dayNumber(cal); }

    public static String todayKey() { return dayKey(Calendar.getInstance()); }

    public static String dayKey(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return dayKey(c);
    }

    /**
     * Napi mennyiség-kulcs-e? Kifejezetten ellenőrizzük a nap-részt, hogy a
     * „water_goal_cl", „water_last_done" és „water_days_done" beállítások soha
     * ne essenek áldozatul a takarításnak.
     */
    public static boolean isDayKey(String key) {
        if (key == null || !key.startsWith(DAY_PREFIX)) return false;
        String rest = key.substring(DAY_PREFIX.length());
        if (rest.length() != 8) return false;
        for (int i = 0; i < 8; i++)
            if (rest.charAt(i) < '0' || rest.charAt(i) > '9') return false;
        return true;
    }

    /** A nap-kulcs dátuma számként (yyyyMMdd), vagy -1 ha nem nap-kulcs. */
    public static int dayOf(String key) {
        if (!isDayKey(key)) return -1;
        try { return Integer.parseInt(key.substring(DAY_PREFIX.length())); }
        catch (NumberFormatException e) { return -1; }
    }

    public static int goalCl(Context c) { return prefs(c).getInt(GOAL_KEY, DEFAULT_GOAL_CL); }

    public static void setGoalCl(Context c, int cl) {
        prefs(c).edit().putInt(GOAL_KEY, Math.max(50, cl)).apply();
    }

    public static int todayCl(Context c) { return prefs(c).getInt(todayKey(), 0); }

    public static int clOn(Context c, long ts) { return prefs(c).getInt(dayKey(ts), 0); }

    /** Használja-e egyáltalán a számlálót (van-e bármilyen napi bejegyzése). */
    public static boolean isUsed(Context c) {
        if (prefs(c).getInt(DAYS_DONE_KEY, 0) > 0) return true;
        for (String k : prefs(c).getAll().keySet()) if (isDayKey(k)) return true;
        return false;
    }

    /** Literben, egy tizedesre – kijelzéshez. */
    public static String liters(int cl) { return (cl / 100.0) + " l"; }

    /**
     * Hozzáad (vagy levon, negatív értékkel) mennyiséget a mai naphoz.
     * Visszaadja az új napi értéket; ha ezzel érte el a célt, a „Hidratált"
     * jelvény napszámlálóját is lépteti (naponta legfeljebb egyszer).
     */
    public static int addCl(Context c, int deltaCl) {
        SharedPreferences p = prefs(c);
        String key = todayKey();
        int before = p.getInt(key, 0);
        int after = Math.max(0, before + deltaCl);
        p.edit().putInt(key, after).apply();
        int goal = goalCl(c);
        if (before < goal && after >= goal) {
            int today = dayNumber(Calendar.getInstance());
            if (p.getInt(LAST_DONE_KEY, 0) != today)
                p.edit().putInt(LAST_DONE_KEY, today)
                        .putInt(DAYS_DONE_KEY, p.getInt(DAYS_DONE_KEY, 0) + 1).apply();
        }
        return after;
    }

    /** Hány napon volt teljesítve a napi cél (a „Hidratált" jelvényhez). */
    public static int daysDone(Context c) { return prefs(c).getInt(DAYS_DONE_KEY, 0); }
}
