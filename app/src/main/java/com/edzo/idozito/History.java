package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Befejezett edzések naplója. A SharedPreferences-be mentett JSON-tömb;
 * a legfrissebb elöl. Legfeljebb az utolsó MAX edzést tartja meg.
 */
public final class History {

    private History() {}

    static final String PREFS = "edzo";
    static final String KEY = "history";
    /**
     * A megőrzött edzések felső korlátja. Bőven kell a fejnek: az XP (és így a
     * szint), az összes-edzés számláló és a jelvények mind ebből a naplóból
     * számolódnak, tehát ami itt kiesik, az a felhasználó eredményéből is
     * eltűnne. 1000 bejegyzés napi edzéssel is közel három év.
     */
    static final int MAX = 1000;

    /** Egyesített aktivitás-napló: időzítős edzések + erősítő bejegyzések (csak
        „ts" időbélyeggel). A széria- és „ma edzett-e" számításokhoz így az
        erősítő nap is beleszámít mindenhol (widget, értesítés, kezdőlap). */
    public static JSONArray loadAll(Context ctx) {
        // Ez a leggyakrabban hívott olvasás (széria, XP, jelvények, kihívás,
        // heti összevetés). A két forrás nyers szövege együtt a kulcs: amíg
        // egyik sem változott, ugyanazt az összefűzött tömböt adjuk vissza.
        String hRaw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "[]");
        // A hossz is a kulcs része, hogy a két szöveg határa egyértelmű legyen.
        String key = hRaw.length() + ":" + hRaw + StrengthLog.rawJson(ctx);
        JSONArray cached = cachedAll;
        if (cached != null && key.equals(cachedAllKey)) return cached;

        JSONArray merged = new JSONArray();
        JSONArray h = load(ctx);
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o != null) merged.put(o);
        }
        for (long ts : oneStrengthPerDay(StrengthLog.load(ctx))) {
            try { merged.put(new JSONObject().put("ts", ts)); } catch (Exception ignored) {}
        }
        cachedAllKey = key;
        cachedAll = merged;
        return merged;
    }

    private static String cachedAllKey;
    private static JSONArray cachedAll;

    /**
     * A „rég kimaradt sport" sora az értesítésekhez és a widgethez, vagy null.
     * A JSON-ból tömböt csinál, a döntést a tesztelt Activities.missedSport hozza.
     */
    public static String missedSportLine(Context ctx) {
        try {
            JSONArray hist = load(ctx);
            String[] ks = new String[hist.length()];
            String[] ns = new String[hist.length()];
            long[] tss = new long[hist.length()];
            for (int i = 0; i < hist.length(); i++) {
                JSONObject o = hist.optJSONObject(i);
                ks[i] = o == null ? "" : o.optString("kind", "");
                ns[i] = o == null ? "x" : o.optString("name", "");
                tss[i] = o == null ? 0 : o.optLong("ts");
            }
            return Activities.missedSport(ks, ns, tss, System.currentTimeMillis());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Az erősítő bejegyzések időbélyegei, NAPONTA EGY. Egy bejegyzés egy
     * gyakorlat, nem egy edzés: aki egy teremben hat gyakorlatot rögzít, az egy
     * edzést végzett. Enélkül az „elvégzett edzések" számláló, a jelvények és az
     * XP is annyiszorosára nőne, ahány gyakorlatot valaki felír.
     *
     * A napon belül a legkorábban talált (a naplóban legfrissebb) időbélyeg
     * marad meg, hogy a napszakhoz kötött jelvények is működjenek.
     *
     * @param newestFirst az erősítő napló, legfrissebb bejegyzéssel elöl
     */
    static long[] oneStrengthPerDay(java.util.List<StrengthLog.Entry> newestFirst) {
        java.util.HashSet<Long> seenDays = new java.util.HashSet<>();
        java.util.List<Long> out = new java.util.ArrayList<>();
        java.util.Calendar c = java.util.Calendar.getInstance();
        for (StrengthLog.Entry e : newestFirst) {
            c.setTimeInMillis(e.ts);
            c.set(java.util.Calendar.HOUR_OF_DAY, 0);
            c.set(java.util.Calendar.MINUTE, 0);
            c.set(java.util.Calendar.SECOND, 0);
            c.set(java.util.Calendar.MILLISECOND, 0);
            if (seenDays.add(c.getTimeInMillis())) out.add(e.ts);
        }
        long[] arr = new long[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        return arr;
    }

    /**
     * Volt-e ma bármilyen edzés (időzítős VAGY erősítő)? A widget, a napi
     * emlékeztető és az időzített emlékeztetők is ezt kérdezik – egy helyen,
     * hogy ne csúszhasson szét a válasz.
     */
    /**
     * Napi mozgás-percek visszafelé (index 0 = ma). A súlyzós napok
     * időtartam nélkül vannak a naplóban, ezért a sorozatszámból becsüljük –
     * különben pont a vasazós hetek látszanának üresnek.
     *
     * Egy helyen: a statisztika, a heti összegzés és a jelvények ugyanezt a
     * számot használják, így nem mondhatnak egymásnak ellent.
     */
    public static double[] dailyMinutes(Context ctx, long now, int days) {
        double[] daily = new double[Math.max(0, days)];
        try {
            JSONArray h = load(ctx);
            for (int i = 0; i < h.length(); i++) {
                JSONObject o = h.optJSONObject(i);
                if (o == null) continue;
                int d = Days.ago(o.optLong("ts"), now);
                if (d >= 0 && d < daily.length) daily[d] += o.optInt("dur") / 60.0;
            }
            java.util.HashMap<Integer, Integer> setsPerDay = new java.util.HashMap<>();
            for (StrengthLog.Entry e : StrengthLog.load(ctx)) {
                int d = Days.ago(e.ts, now);
                if (d < 0 || d >= daily.length || e.sets == null) continue;
                Integer prev = setsPerDay.get(d);
                setsPerDay.put(d, (prev == null ? 0 : prev) + e.sets.size());
            }
            for (java.util.Map.Entry<Integer, Integer> e : setsPerDay.entrySet())
                daily[e.getKey()] += Load.strengthMinutes(e.getValue());
        } catch (Exception ignored) {}
        return daily;
    }

    /** A mai edzésekkel elégetett kalória (0, ha nincs mérés). */
    public static double burnedToday(Context ctx) {
        double sum = 0;
        try {
            long t0 = Days.startOf(System.currentTimeMillis());
            JSONArray h = loadAll(ctx);
            for (int i = 0; i < h.length(); i++) {
                JSONObject o = h.optJSONObject(i);
                if (o != null && o.optLong("ts") >= t0) sum += Math.max(0, o.optDouble("cal", 0));
            }
        } catch (Exception ignored) {}
        return sum;
    }

    public static boolean trainedToday(Context ctx) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        JSONArray h = loadAll(ctx);
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o != null && o.optLong("ts") >= start) return true;
        }
        return false;
    }

    /** Egy befejezett edzés hozzáadása a napló elejére. distanceM/maxSpeedKmh < 0, ha nem volt táv-mérés. */
    /**
     * Kézzel felvett edzés: olyan mozgás, amit nem az app mért (kézilabda,
     * úszás, kondi…). A bejegyzés mindenben ugyanolyan, mint egy mért edzés,
     * hogy a széria, az XP, a jelvények, a heti visszatekintő és a statisztika
     * is számoljon vele.
     *
     * Az időpont lehet MÚLTBELI, ezért nem a lista elejére tesszük, hanem a
     * helyére: a napló legfrissebb-elöl sorrendjére sok minden épül (a legutóbbi
     * edzés jegyzete, a hangulat, a listakártyák sorrendje).
     */
    public static void addManual(Context ctx, long ts, int durationSec, double distanceM,
                                 double calories, double avgSpeedKmh, String name, String kind) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = loadForEdit(ctx);
        try {
            JSONObject o = new JSONObject();
            o.put("ts", ts);
            if (name != null && !name.isEmpty()) o.put("name", name);
            if (kind != null && !kind.isEmpty()) o.put("kind", kind);
            o.put("dur", durationSec);
            o.put("dist", distanceM);
            o.put("rounds", 0);
            o.put("work", 0);
            o.put("rest", 0);
            o.put("maxspeed", -1);
            o.put("steps", 0);
            o.put("moving", 0);
            o.put("elev", 0);
            o.put("cal", calories);
            o.put("avgspeed", avgSpeedKmh);
            o.put("manual", true);

            JSONArray out = new JSONArray();
            boolean placed = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject e = arr.optJSONObject(i);
                if (e == null) continue;
                if (!placed && e.optLong("ts") <= ts) { out.put(o); placed = true; }
                out.put(e);
            }
            if (!placed) out.put(o);
            if (out.length() > MAX) {
                JSONArray capped = new JSONArray();
                for (int i = 0; i < MAX; i++) capped.put(out.get(i));
                out = capped;
            }
            p.edit().putString(KEY, out.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static void add(Context ctx, long ts, int durationSec, double distanceM,
                           int rounds, int work, int rest, double maxSpeedKmh,
                           int steps, int movingSec, double elevGainM, double calories,
                           double avgSpeedKmh, String name) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = loadForEdit(ctx);
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
        JSONArray arr = loadForEdit(ctx);
        if (arr.length() == 0) return;
        try {
            JSONObject o = arr.optJSONObject(0);
            if (o == null) return;
            o.put("mood", mood);
            p.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    // A kezdőlap egyetlen frissítése tucatnyiszor kéri el a naplót (szint, széria,
    // kihívás, jelvények, rekordok…). Mivel a napló akár ezer bejegyzés is lehet,
    // a JSON-t nem érdemes minden hívásnál újraértelmezni: eltesszük az utoljára
    // beolvasott nyers szöveget, és amíg az nem változik, ugyanazt adjuk vissza.
    private static String cachedRaw;
    private static JSONArray cachedArr;

    /** A napló olvasásra. A visszakapott tömböt NE módosítsd – lásd loadForEdit. */
    public static JSONArray load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String s = p.getString(KEY, "[]");
        JSONArray c = cachedArr;
        if (c != null && s.equals(cachedRaw)) return c;
        JSONArray parsed;
        try { parsed = new JSONArray(s); } catch (Exception e) { parsed = new JSONArray(); }
        cachedRaw = s;
        cachedArr = parsed;
        return parsed;
    }

    /**
     * Friss, saját példány módosításhoz. A módosító metódusok ezt használják, így
     * a gyorsítótárban lévő tömb soha nem változik a hátunk mögött.
     */
    private static JSONArray loadForEdit(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String s = p.getString(KEY, "[]");
        try { return new JSONArray(s); } catch (Exception e) { return new JSONArray(); }
    }

    /** Szöveges jegyzet mentése a legfrissebb edzéshez. */
    public static void setNoteForLatest(Context ctx, String note) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr = loadForEdit(ctx);
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
        JSONArray arr = loadForEdit(ctx);
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
        JSONArray arr = loadForEdit(ctx);
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
