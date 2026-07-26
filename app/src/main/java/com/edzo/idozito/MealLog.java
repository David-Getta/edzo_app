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

    /** Bejegyzés időpontjának módosítása (a fotó és minden más adat megmarad). */
    public static void updateTs(Context c, long oldTs, long newTs) {
        List<Meal> l = load(c);
        for (int i = 0; i < l.size(); i++)
            if (l.get(i).ts == oldTs) {
                Meal m = l.get(i);
                l.set(i, new Meal(newTs, m.name, m.items, m.photo));
            }
        save(c, l);
    }

    /** Bejegyzés törlése időbélyeg alapján (rendezés-független). A fotófájlt nem
     *  itt töröljük (szerkesztéskor ugyanaz a fotó újra hozzáadódik) – az árván
     *  maradt képeket a cleanupOrphanPhotos szedi össze. */
    public static void removeByTs(Context c, long ts) {
        List<Meal> l = load(c);
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).ts == ts) l.remove(i);
        save(c, l);
    }

    // ---------- Kedvencek ----------
    // Az étkezés pillanatképe (név + összetevők) külön listában, hogy az eredeti
    // bejegyzés törlése után is naplózható maradjon egy koppintással.

    static final String FAV_KEY = "fav_meals";

    public static List<Meal> loadFavs(Context c) {
        List<Meal> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(FAV_KEY, "[]"));
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
                out.add(new Meal(o.optLong("ts"), o.optString("name", ""), items, ""));
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** A kedvencek címkéje: a név, vagy ha üres, az első összetevő. */
    public static String favLabel(Meal m) {
        if (!m.name.isEmpty()) return m.name;
        return m.items.isEmpty() ? "Étkezés" : m.items.get(0).food;
    }

    public static boolean isFav(Context c, Meal m) {
        String lbl = favLabel(m);
        for (Meal f : loadFavs(c)) if (favLabel(f).equalsIgnoreCase(lbl)) return true;
        return false;
    }

    /** Kedvencnek jelölés (azonos címkéjű korábbit cserél). Max 8 fér el. */
    public static void addFav(Context c, Meal m) {
        List<Meal> favs = loadFavs(c);
        String lbl = favLabel(m);
        for (int i = favs.size() - 1; i >= 0; i--)
            if (favLabel(favs.get(i)).equalsIgnoreCase(lbl)) favs.remove(i);
        favs.add(0, new Meal(m.ts, m.name, m.items, ""));
        while (favs.size() > 8) favs.remove(favs.size() - 1);
        saveFavs(c, favs);
    }

    public static void removeFav(Context c, String label) {
        List<Meal> favs = loadFavs(c);
        for (int i = favs.size() - 1; i >= 0; i--)
            if (favLabel(favs.get(i)).equalsIgnoreCase(label)) favs.remove(i);
        saveFavs(c, favs);
    }

    static void saveFavs(Context c, List<Meal> favs) {
        JSONArray a = new JSONArray();
        for (Meal m : favs) {
            try {
                JSONObject o = new JSONObject();
                o.put("ts", m.ts);
                o.put("name", m.name);
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
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(FAV_KEY, a.toString()).apply();
    }

    /** Árván maradt meal_*.jpg fájlok törlése a belső tárból. */
    public static void cleanupOrphanPhotos(Context c) {
        try {
            java.util.HashSet<String> used = new java.util.HashSet<>();
            for (Meal m : load(c)) if (!m.photo.isEmpty()) used.add(m.photo);
            java.io.File[] files = c.getFilesDir().listFiles();
            if (files != null) for (java.io.File f : files)
                if (f.getName().startsWith("meal_") && f.getName().endsWith(".jpg")
                        && !used.contains(f.getName()))
                    f.delete();
        } catch (Exception ignored) {}
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
