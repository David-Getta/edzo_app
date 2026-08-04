package com.edzo.idozito;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Kitüntetések (badge-ek): mérföldkövek, amiket az edzés-előzményből számolunk.
 * Motiváló, gyűjthető jelvények – nincs külön tárolás, mindig az előzményből
 * derül ki, melyik van meg. Az újonnan megszerzetteket a MainActivity a
 * "badges_seen" beállítás alapján ünnepli meg.
 */
public final class Badges {

    public static final class Badge {
        public final String id, emoji, title, desc;
        Badge(String id, String emoji, String title, String desc) {
            this.id = id; this.emoji = emoji; this.title = title; this.desc = desc;
        }
    }

    /** Az összes lehetséges kitüntetés, megjelenítési sorrendben. */
    public static final Badge[] ALL = {
        new Badge("first",   "🥇", "Első lépés",   "Teljesítsd az első edzésed"),
        new Badge("c5",      "🔥", "Lendületben",  "5 elvégzett edzés"),
        new Badge("c10",     "💪", "Kitartó",      "10 elvégzett edzés"),
        new Badge("c25",     "🏆", "Bajnok",       "25 elvégzett edzés"),
        new Badge("c50",     "👑", "Legenda",      "50 elvégzett edzés"),
        new Badge("streak4", "📅", "Heti rutin",   "4 hetes sorozat egymás után"),
        new Badge("day3",    "⚡", "Lendület",     "3 egymást követő edzésnap"),
        new Badge("day7",    "🗓️", "Heti hős",     "7 egymást követő edzésnap"),
        new Badge("day30",   "💎", "Gyémánt rutin","30 egymást követő edzésnap"),
        new Badge("run5",    "🏃", "5K klub",      "Fuss le 5 km-t egy edzésen"),
        new Badge("run10",   "🚀", "10K hős",      "Fuss le 10 km-t egy edzésen"),
        new Badge("time600", "⏱️", "Órák hőse",    "Összesen 600 perc edzés"),
        new Badge("early",   "🌅", "Korán kelő",   "Edzés reggel 7 előtt"),
        new Badge("night",   "🌙", "Éjjeli bagoly","Edzés este 22 után"),
        new Badge("run21",   "🏅", "Félmaraton",   "Fuss le 21 km-t egy edzésen"),
        new Badge("dist42",  "🌍", "Maratoni táv", "Összesen 42 km megtéve"),
        new Badge("dist100", "🌟", "Százas klub",  "Összesen 100 km megtéve"),
        new Badge("streak8", "📆", "Vasakarat",    "8 hetes sorozat egymás után"),
        new Badge("c100",    "🎖️", "Századik",     "100 elvégzett edzés"),
        new Badge("ch5",     "🎯", "Kihívó",       "Teljesíts 5 napi kihívást"),
        new Badge("ch25",    "🏹", "Célvadász",    "Teljesíts 25 napi kihívást"),
        new Badge("pw4",     "🗓", "Tervkövető",   "4 hét, amikor a teljes heti terv teljesült"),
        new Badge("pw12",    "🛡", "Tervbajnok",   "12 teljesített terv-hét"),
        new Badge("meal10",  "🍽", "Naplózó",      "10 naplózott étkezés az Étrendben"),
        new Badge("meal50",  "🥗", "Tudatos étkező","50 naplózott étkezés az Étrendben"),
        new Badge("meald7",  "🧮", "Kalóriamester","Étkezés-napló 7 különböző napon"),
        new Badge("water7",  "💧", "Hidratált",    "Napi vízcél elérve 7 különböző napon"),
        new Badge("lift10",  "🏋", "Vasalapozó",   "10 gyakorlat az erősítő naplóban"),
        new Badge("vol10t",  "⚙️", "Tonnás",       "10 tonna összvolumen (ismétlés × súly)"),
        new Badge("bal4",    "🧩", "Kiegyensúlyozott", "4 izomcsoport egy héten belül"),
        new Badge("move1",   "🎽", "Heti cél",     "A heti mozgás-cél teljesítve"),
        new Badge("move4",   "🏅", "Négy hét egyben","A heti mozgás-cél négy hete sorban"),
    };

    /** Visszafelé kompatibilis változat (kihívás-számláló nélkül). */
    public static HashSet<String> earned(JSONArray arr, int bestStreakWeeks) {
        return earned(arr, bestStreakWeeks, 0);
    }

    public static HashSet<String> earned(JSONArray arr, int bestStreakWeeks, int challengesDone) {
        return earned(arr, bestStreakWeeks, challengesDone, 0);
    }

    /** Étrend-mérföldkövek hozzáadása (a hívó adja át a Context-ből számolt naplót). */
    public static HashSet<String> earned(android.content.Context ctx, JSONArray arr,
                                         int bestStreakWeeks, int challengesDone, int planWeeks) {
        HashSet<String> out = earned(arr, bestStreakWeeks, challengesDone, planWeeks);
        try {
            List<MealLog.Meal> meals = MealLog.load(ctx);
            HashSet<Long> mealDays = new HashSet<>();
            java.util.Calendar c = java.util.Calendar.getInstance();
            for (MealLog.Meal m : meals) mealDays.add(dayStart(c, m.ts));
            if (meals.size() >= 10) out.add("meal10");
            if (meals.size() >= 50) out.add("meal50");
            if (mealDays.size() >= 7) out.add("meald7");
            if (Water.daysDone(ctx) >= 7) out.add("water7");
        } catch (Exception ignored) {}
        // Súlyzós mérföldkövek: a volumen (ismétlés × súly) az a mérce, ami a
        // darabszámnál többet mond – tíz tonna elmozgatott súly komoly munka.
        try {
            List<StrengthLog.Entry> log = StrengthLog.load(ctx);
            double volume = 0;
            for (StrengthLog.Entry e : log) volume += e.volume();
            if (log.size() >= 10) out.add("lift10");
            if (volume >= 10000) out.add("vol10t");
            java.util.LinkedHashMap<String, Integer> bal =
                    Muscles.weekBalance(log, System.currentTimeMillis(), 7);
            int groups = 0;
            for (int v : bal.values()) if (v > 0) groups++;
            if (groups >= 4) out.add("bal4");
        } catch (Exception ignored) {}
        // Heti mozgás-cél: a teljesítés egyszer is jelvény, de a négy hét
        // sorban a valódi eredmény – a szokás ott kezdődik.
        try {
            int goal = ctx.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE)
                    .getInt("move_goal_min", Load.DEFAULT_WEEKLY_GOAL);
            double[] weeks = Load.weekSums(
                    History.dailyMinutes(ctx, System.currentTimeMillis(), 4 * Load.ACUTE_DAYS), 4);
            if (weeks[0] >= goal) out.add("move1");
            int inRow = 0;
            for (double v : weeks) { if (v >= goal) inRow++; else break; }
            if (inRow >= 4) out.add("move4");
        } catch (Exception ignored) {}
        return out;
    }

    /** A megszerzett kitüntetések azonosítói az előzmény alapján. */
    public static HashSet<String> earned(JSONArray arr, int bestStreakWeeks, int challengesDone,
                                         int planWeeks) {
        HashSet<String> out = new HashSet<>();
        int count = arr.length();
        double totalM = 0; long totalSec = 0;
        double maxRun = 0;
        boolean early = false, night = false;
        HashSet<Long> days = new HashSet<>();
        java.util.Calendar c = java.util.Calendar.getInstance();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            totalSec += o.optInt("dur");
            double d = o.optDouble("dist", 0);
            if (d > 0) { totalM += d; if (d > maxRun) maxRun = d; }
            long ts = o.optLong("ts", 0);
            if (ts > 0) {
                c.setTimeInMillis(ts);
                // Kézzel felvett edzésnél az időbélyeg a RÖGZÍTÉS pillanata, nem
                // az edzésé: aki este viszi fel a délutáni kézilabdát, nem lett
                // tőle éjjeli bagoly. A napszak-jelvényekbe ezért csak a mért
                // edzés számít – ott az óra valódi.
                if (!o.optBoolean("manual", false)) {
                    int h = c.get(java.util.Calendar.HOUR_OF_DAY);
                    if (h < 7) early = true;
                    if (h >= 22) night = true;
                }
                days.add(dayStart(c, ts));
            }
        }
        int bestDayStreak = bestDayStreak(days);
        if (count >= 1) out.add("first");
        if (count >= 5) out.add("c5");
        if (count >= 10) out.add("c10");
        if (count >= 25) out.add("c25");
        if (count >= 50) out.add("c50");
        if (count >= 100) out.add("c100");
        if (bestStreakWeeks >= 4) out.add("streak4");
        if (bestStreakWeeks >= 8) out.add("streak8");
        if (bestDayStreak >= 3) out.add("day3");
        if (bestDayStreak >= 7) out.add("day7");
        if (bestDayStreak >= 30) out.add("day30");
        if (maxRun >= 5000) out.add("run5");
        if (maxRun >= 10000) out.add("run10");
        if (maxRun >= 21000) out.add("run21");
        if (totalSec >= 600 * 60) out.add("time600");
        if (early) out.add("early");
        if (night) out.add("night");
        if (totalM >= 42000) out.add("dist42");
        if (totalM >= 100000) out.add("dist100");
        if (challengesDone >= 5) out.add("ch5");
        if (challengesDone >= 25) out.add("ch25");
        if (planWeeks >= 4) out.add("pw4");
        if (planWeeks >= 12) out.add("pw12");
        return out;
    }

    /**
     * A valaha volt leghosszabb egymást követő edzésnap-sorozat. Erre épül a
     * „Lendület" (3), „Heti hős" (7) és „Gyémánt rutin" (30) jelvény.
     *
     * A napléptetés óraátállás-biztos: normalizált éjfelekből indulunk, és a
     * ±12/36 órás eltolás újranormalizálva mindig a szomszéd napra esik – a 23
     * és a 25 órás napon is.
     */
    static int bestDayStreak(HashSet<Long> dayStarts) {
        int best = 0;
        for (Long day : dayStarts) {
            // Csak a sorozatok elejéről indulunk, különben minden napot
            // újraszámolnánk a saját sorozatán belül.
            if (dayStarts.contains(Days.startOf(day - 12L * 3600 * 1000))) continue;
            int s = 0;
            long cur = day;
            while (dayStarts.contains(cur)) { s++; cur = Days.startOf(cur + 36L * 3600 * 1000); }
            if (s > best) best = s;
        }
        return best;
    }

    /** Ugyanaz nyers időbélyegekből – így Android nélkül is tesztelhető. */
    static int bestDayStreak(long[] timestamps) {
        HashSet<Long> days = new HashSet<>();
        for (long ts : timestamps) days.add(Days.startOf(ts));
        return bestDayStreak(days);
    }

    /** Az adott időbélyeg helyi napjának éjféli (nap eleji) időbélyege. */
    private static long dayStart(java.util.Calendar c, long ts) {
        c.setTimeInMillis(ts);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** A megszerzett badge-ek listája megjelenítési sorrendben. */
    public static List<Badge> earnedList(HashSet<String> ids) {
        List<Badge> list = new ArrayList<>();
        for (Badge b : ALL) if (ids.contains(b.id)) list.add(b);
        return list;
    }
}
