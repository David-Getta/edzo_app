package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Étrend-napló: étkezések összetevőkkel (étel + gramm + kcal). Az „edzo"
 * SharedPreferences-ben tárolva JSON-ként, így a biztonsági mentés is viszi.
 */
public final class MealLog {

    private MealLog() {}

    static final String PREFS = "edzo";
    static final String KEY = "meal_log";
    static final int MAX = 300;

    public static final class Item {
        public final String food;
        public final double grams;
        public final double kcal;
        public final double protein; // gramm (0 = ismeretlen)
        public Item(String food, double grams, double kcal) { this(food, grams, kcal, 0); }
        public Item(String food, double grams, double kcal, double protein) {
            this.food = food; this.grams = grams; this.kcal = kcal; this.protein = protein;
        }
    }

    public static final class Meal {
        public final long ts;
        public final String name;     // étkezés címkéje (lehet üres)
        public final List<Item> items;
        public final String photo;    // fájlnév a belső tárban (lehet üres)
        public Meal(long ts, String name, List<Item> items, String photo) {
            this.ts = ts; this.name = name; this.items = items;
            this.photo = photo == null ? "" : photo;
        }
        public double kcal() { double k = 0; for (Item i : items) k += i.kcal; return k; }
        public double protein() { double p = 0; for (Item i : items) p += i.protein; return p; }
        public double grams() { double g = 0; for (Item i : items) g += i.grams; return g; }
    }

    public static List<Meal> load(Context c) {
        List<Meal> out = new ArrayList<>();
        String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                List<Item> items = new ArrayList<>();
                JSONArray ia = o.optJSONArray("items");
                if (ia != null) for (int j = 0; j < ia.length(); j++) {
                    JSONObject io = ia.optJSONObject(j);
                    if (io == null) continue;
                    items.add(new Item(io.optString("f", "Étel"),
                            io.optDouble("g", 0), io.optDouble("k", 0), io.optDouble("p", 0)));
                }
                out.add(new Meal(o.optLong("ts"), o.optString("name", ""), items,
                        o.optString("photo", "")));
            }
        } catch (Exception ignored) {}
        return out;
    }

    static void save(Context c, List<Meal> list) {
        JSONArray a = new JSONArray();
        for (Meal m : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("ts", m.ts);
                o.put("name", m.name);
                if (!m.photo.isEmpty()) o.put("photo", m.photo);
                JSONArray ia = new JSONArray();
                for (Item i : m.items) {
                    JSONObject io = new JSONObject();
                    io.put("f", i.food);
                    io.put("g", i.grams);
                    io.put("k", i.kcal);
                    if (i.protein > 0) io.put("p", i.protein);
                    ia.put(io);
                }
                o.put("items", ia);
                a.put(o);
            } catch (Exception ignored) {}
        }
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(KEY, a.toString()).apply();
    }

    public static void add(Context c, Meal m) {
        List<Meal> l = load(c);
        l.add(0, m);                       // legújabb elöl
        while (l.size() > MAX) l.remove(l.size() - 1);
        save(c, l);
    }

    /** Fotó hozzárendelése egy meglévő étkezéshez (időbélyeg alapján). */
    public static void updatePhoto(Context c, long ts, String photo) {
        List<Meal> l = load(c);
        for (int i = 0; i < l.size(); i++)
            if (l.get(i).ts == ts)
                l.set(i, new Meal(ts, l.get(i).name, l.get(i).items, photo));
        save(c, l);
    }

    /** Bejegyzés törlése időbélyeg alapján (rendezés-független). */
    public static void removeByTs(Context c, long ts) {
        List<Meal> l = load(c);
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).ts == ts) l.remove(i);
        save(c, l);
    }

    public static void removeAt(Context c, int idx) {
        List<Meal> l = load(c);
        if (idx >= 0 && idx < l.size()) { l.remove(idx); save(c, l); }
    }

    /** A mai nap összes fehérjéje (gramm). */
    public static double todayProtein(Context c) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        double p = 0;
        for (Meal m : load(c)) if (m.ts >= start) p += m.protein();
        return p;
    }

    /** A mai nap összes kalóriája. */
    public static double todayKcal(Context c) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        double k = 0;
        for (Meal m : load(c)) if (m.ts >= start) k += m.kcal();
        return k;
    }
}
