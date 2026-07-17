package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

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
            root.put("app", "my_trainer");
            root.put("ver", 1);
            root.put("ts", System.currentTimeMillis());
            root.put("data", data);
            return root.toString();
        } catch (Exception ex) {
            return "{}";
        }
    }

    /** Visszaállítás; true, ha érvényes My trainer mentésfájl volt. */
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
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
