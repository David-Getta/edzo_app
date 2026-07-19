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
    };

    /** A megszerzett kitüntetések azonosítói az előzmény alapján. */
    public static HashSet<String> earned(JSONArray arr, int bestStreakWeeks) {
        HashSet<String> out = new HashSet<>();
        int count = arr.length();
        double totalM = 0; long totalSec = 0;
        double maxRun = 0;
        boolean early = false, night = false;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            totalSec += o.optInt("dur");
            double d = o.optDouble("dist", 0);
            if (d > 0) { totalM += d; if (d > maxRun) maxRun = d; }
            long ts = o.optLong("ts", 0);
            if (ts > 0) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.setTimeInMillis(ts);
                int h = c.get(java.util.Calendar.HOUR_OF_DAY);
                if (h < 7) early = true;
                if (h >= 22) night = true;
            }
        }
        if (count >= 1) out.add("first");
        if (count >= 5) out.add("c5");
        if (count >= 10) out.add("c10");
        if (count >= 25) out.add("c25");
        if (count >= 50) out.add("c50");
        if (count >= 100) out.add("c100");
        if (bestStreakWeeks >= 4) out.add("streak4");
        if (bestStreakWeeks >= 8) out.add("streak8");
        if (maxRun >= 5000) out.add("run5");
        if (maxRun >= 10000) out.add("run10");
        if (maxRun >= 21000) out.add("run21");
        if (totalSec >= 600 * 60) out.add("time600");
        if (early) out.add("early");
        if (night) out.add("night");
        if (totalM >= 42000) out.add("dist42");
        if (totalM >= 100000) out.add("dist100");
        return out;
    }

    /** A megszerzett badge-ek listája megjelenítési sorrendben. */
    public static List<Badge> earnedList(HashSet<String> ids) {
        List<Badge> list = new ArrayList<>();
        for (Badge b : ALL) if (ids.contains(b.id)) list.add(b);
        return list;
    }
}
