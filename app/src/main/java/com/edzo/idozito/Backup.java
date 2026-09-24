package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Teljes biztonsági mentés: a teljes „edzo" SharedPreferences (előzmények,
 * sablonok, saját programok, testmérések, beállítások, cél, emlékeztetők)
 * JSON-be írása és visszaolvasása – telefonváltáshoz / újratelepítéshez.
 */
public final class Backup {

    private Backup() {}

    static final String PREFS = "edzo";

    /**
     * A GPS-útvonalak nem a beállítások között vannak, hanem külön fájlokban.
     * A mentés eddig csak a beállításokat vitte, így telefonváltás vagy
     * újratelepítés után MINDEN futás útvonala, splitje és sebesség-görbéje
     * eltűnt – pedig pont erre az esetre való a mentés, és a felhasználó ezt
     * csak akkor vette volna észre, amikor már nincs miből visszaállítani.
     *
     * Az útvonalak nagyok (egy órás futás több száz kilobájt), ezért van rajtuk
     * felső korlát: a legfrissebb futásoké kerül be, amíg belefér.
     */
    static final int MAX_TRACK_CHARS = 4 * 1024 * 1024;

    /** Az összes beállítás JSON-szövegként (típusjelöléssel). */
    public static String exportJson(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONObject data = new JSONObject();
            for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
                Object v = e.getValue();
                JSONObject o = new JSONObject();
                if (v instanceof String) { o.put("t", "s"); o.put("v", (String) v); }
                else if (v instanceof Boolean) { o.put("t", "b"); o.put("v", (Boolean) v); }
                else if (v instanceof Integer) { o.put("t", "i"); o.put("v", (Integer) v); }
                else if (v instanceof Long) { o.put("t", "l"); o.put("v", (Long) v); }
                else if (v instanceof Float) { o.put("t", "f"); o.put("v", ((Float) v).doubleValue()); }
                else continue;
                data.put(e.getKey(), o);
            }
            JSONObject root = new JSONObject();
            // A formátum-jelölő szándékosan marad a régi néven: az importálás
            // ezt ellenőrzi, így a Grit előtti mentésfájlok is visszatölthetők.
            root.put("app", "my_trainer");
            root.put("ver", 1);
            root.put("ts", System.currentTimeMillis());
            root.put("data", data);
            root.put("tracks", tracks(c));
            return root.toString();
        } catch (Exception ex) {
            return "{}";
        }
    }

    /** A belefért GPS-útvonalak, időbélyeg szerint. */
    private static JSONObject tracks(Context c) {
        JSONObject out = new JSONObject();
        int used = 0;
        try {
            for (long ts : SessionStore.storedTimestamps(c)) {
                JSONArray tr = SessionStore.loadTrack(c, ts);
                if (tr.length() == 0) continue;
                int size = tr.toString().length();
                if (used + size > MAX_TRACK_CHARS) break;
                used += size;
                out.put(String.valueOf(ts), tr);
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** Visszaállítás; true, ha érvényes Grit (my_trainer) mentésfájl volt. */
    public static boolean importJson(Context c, String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (!"my_trainer".equals(root.optString("app"))) return false;
            JSONObject data = root.getJSONObject("data");
            SharedPreferences.Editor ed = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            ed.clear();
            java.util.Iterator<String> it = data.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject o = data.getJSONObject(k);
                switch (o.optString("t")) {
                    case "s": ed.putString(k, o.optString("v")); break;
                    case "b": ed.putBoolean(k, o.optBoolean("v")); break;
                    case "i": ed.putInt(k, o.optInt("v")); break;
                    case "l": ed.putLong(k, o.optLong("v")); break;
                    case "f": ed.putFloat(k, (float) o.optDouble("v")); break;
                    default: break;
                }
            }
            ed.apply();
            // Az útvonalak fájlokba mennek vissza. Régi mentésfájlban nincs
            // „tracks" – az sem hiba, csak nem lesz mit visszaállítani.
            JSONObject tr = root.optJSONObject("tracks");
            if (tr != null) {
                java.util.Iterator<String> ti = tr.keys();
                while (ti.hasNext()) {
                    String k = ti.next();
                    JSONArray one = tr.optJSONArray(k);
                    if (one == null || one.length() == 0) continue;
                    try { SessionStore.save(c, Long.parseLong(k), one); }
                    catch (NumberFormatException ignored) {}
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
