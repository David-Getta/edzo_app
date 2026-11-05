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

    /** „Nincs a sablonban" – a bemelegítés/levezetés előtti sablonok jelölése. */
    public static final int UNSET = -1;

    public static final class W {
        public String name;
        public int prep, work, rest, rounds;
        /**
         * Bemelegítés és levezetés másodpercben, vagy UNSET.
         *
         * A régi sablonok ezeket még nem tárolták, és a betöltésük sem nyúlt
         * hozzájuk. Ha ilyenkor nullát írnánk be, egy régi sablon betöltése
         * csendben letörölné a beállított bemelegítést – ezért a hiányzó érték
         * UNSET marad, és a betöltés érintetlenül hagyja azt a mezőt.
         */
        public int warm = UNSET, cool = UNSET;

        public W(String name, int prep, int work, int rest, int rounds) {
            this.name = name; this.prep = prep; this.work = work; this.rest = rest; this.rounds = rounds;
        }

        public W(String name, int prep, int work, int rest, int rounds, int warm, int cool) {
            this(name, prep, work, rest, rounds);
            this.warm = warm; this.cool = cool;
        }

        /** Tartalmazza-e a sablon a bemelegítés/levezetés beállítást? */
        public boolean hasWarmCool() { return warm != UNSET || cool != UNSET; }
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
                        o.optInt("prep"), o.optInt("work"), o.optInt("rest"), o.optInt("rounds"),
                        o.optInt("warm", UNSET), o.optInt("cool", UNSET)));
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
                // Csak ha tényleg része a sablonnak – így a régi bejegyzések
                // formátuma sem változik meg a következő mentéskor.
                if (w.warm != UNSET) o.put("warm", w.warm);
                if (w.cool != UNSET) o.put("cool", w.cool);
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
