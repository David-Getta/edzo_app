package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Befejezett edzések naplója. A SharedPreferences-be mentett JSON-tömb;
 * a legfrissebb elöl. Legfeljebb az utolsó 100 edzést tartja meg.
 */
public final class History {

    private History() {}

    static final String PREFS = "edzo";
    static final String KEY = "history";
    static final int MAX = 100;

    /** Egyesített aktivitás-napló: időzítős edzések + erősítő bejegyzések (csak
        „ts" időbélyeggel). A széria- és „ma edzett-e" számításokhoz így az
        erősítő nap is beleszámít mindenhol (widget, értesítés, kezdőlap). */
    public static JSONArray loadAll(Context ctx) {
        JSONArray merged = new JSONArray();
        JSONArray h = load(ctx);
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o != null) merged.put(o);
        }
        for (StrengthLog.Entry e : StrengthLog.load(ctx)) {
            try { merged.put(new JSONObject().put("ts", e.ts)); } catch (Exception ignored) {}
        }
        return merged;
    }

    /** Egy befejezett edzés hozzáadása a napló elejére. distanceM/maxSpeedKmh < 0, ha nem volt táv-mérés. */
    public static void add(Context ctx, long ts, int durationSec, double distanceM,
                           int rounds, int work, int rest, double maxSpeedKmh,
                           int steps, int movingSec, double elevGainM, double calories,
                           double avgSpeedKmh, String name) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = load(ctx);
        try {
            JSONObject o = new JSONObject();
            o.put("ts", ts);
            if (name != null && !name.isEmpty()) o.put("name", name);
            o.put("dur", durationSec);
            o.put("dist", distanceM);
            o.put("rounds", rounds);
            o.put("work", work);
            o.put("rest", rest);
            o.put("maxspeed", maxSpeedKmh);
            o.put("steps", steps);
            o.put("moving", movingSec);
            o.put("elev", elevGainM);
            o.put("cal", calories);
            o.put("avgspeed", avgSpeedKmh);
            JSONArray out = new JSONArray();
            out.put(o);
            for (int i = 0; i < arr.length() && out.length() < MAX; i++) out.put(arr.get(i));
            p.edit().putString(KEY, out.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** Hangulat/érzés (1–4) mentése a legfrissebb edzéshez. */
    public static void setMoodForLatest(Context ctx, int mood) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = load(ctx);
        if (arr.length() == 0) return;
        try {
            JSONObject o = arr.optJSONObject(0);
            if (o == null) return;
            o.put("mood", mood);
            p.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static JSONArray load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String s = p.getString(KEY, "[]");
        try { return new JSONArray(s); } catch (Exception e) { return new JSONArray(); }
    }

    /** Szöveges jegyzet mentése a legfrissebb edzéshez. */
    public static void setNoteForLatest(Context ctx, String note) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = load(ctx);
        if (arr.length() == 0) return;
        try {
            JSONObject o = arr.optJSONObject(0);
            if (o == null) return;
            if (note == null) note = "";
            o.put("note", note.trim());
            p.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Egy adott (ts szerinti) edzés mezőjének frissítése. */
    public static void updateByTs(Context ctx, long ts, String key, Object value) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = load(ctx);
        try {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null && o.optLong("ts") == ts) {
                    o.put(key, value);
                    p.edit().putString(KEY, arr.toString()).apply();
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    /** Egy adott (ts szerinti) edzés törlése a naplóból. */
    public static void deleteByTs(Context ctx, long ts) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = load(ctx);
        JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optLong("ts") == ts) continue; // kihagyjuk
            if (o != null) out.put(o);
        }
        p.edit().putString(KEY, out.toString()).apply();
    }

    /** A legfrissebb edzés jegyzete, vagy üres string. */
    public static String latestNote(Context ctx) {
        JSONArray arr = load(ctx);
        JSONObject o = arr.optJSONObject(0);
        return o == null ? "" : o.optString("note", "");
    }

    /** Hangulat (1–4) emojija, vagy üres string, ha nincs. */
    public static String moodEmoji(int mood) {
        switch (mood) {
            case 1: return "😣";
            case 2: return "😐";
            case 3: return "🙂";
            case 4: return "💪";
            default: return "";
        }
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }
}
