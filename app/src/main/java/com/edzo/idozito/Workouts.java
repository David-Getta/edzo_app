package com.edzo.idozito;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Elmentett saját edzés-sablonok (név + előkészület/futás/pihenő/körök).
 */
public final class Workouts {

    private Workouts() {}

    static final String PREFS = "edzo";
    static final String KEY = "templates";

    public static final class W {
        public String name;
        public int prep, work, rest, rounds;
        public W(String name, int prep, int work, int rest, int rounds) {
            this.name = name; this.prep = prep; this.work = work; this.rest = rest; this.rounds = rounds;
        }
    }

    public static List<W> load(Context c) {
        List<W> out = new ArrayList<>();
        String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                out.add(new W(o.optString("name", "Edzés"),
                        o.optInt("prep"), o.optInt("work"), o.optInt("rest"), o.optInt("rounds")));
            }
        } catch (Exception ignored) {}
        return out;
    }

    static void save(Context c, List<W> list) {
        JSONArray a = new JSONArray();
        for (W w : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("name", w.name);
                o.put("prep", w.prep); o.put("work", w.work);
                o.put("rest", w.rest); o.put("rounds", w.rounds);
                a.put(o);
            } catch (Exception ignored) {}
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }

    public static void add(Context c, W w) {
        List<W> list = load(c);
        list.add(0, w);
        save(c, list);
    }

    public static void removeAt(Context c, int index) {
        List<W> list = load(c);
        if (index >= 0 && index < list.size()) { list.remove(index); save(c, list); }
    }
}
