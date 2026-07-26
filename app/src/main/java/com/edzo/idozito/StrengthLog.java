package com.edzo.idozito;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Erősítő edzésnapló: gyakorlatonként sorozatok (ismétlés × súly) rögzítése,
 * saját rekordokkal (max súly, becsült 1RM, legnagyobb volumen). A „edzo"
 * SharedPreferences-ben tárolva, JSON-ként – így a biztonsági mentés is viszi.
 */
public final class StrengthLog {

    private StrengthLog() {}

    static final String PREFS = "edzo";
    static final String KEY = "strength_log";

    /** Gyakori gyakorlatok gyors kiválasztáshoz (a saját nevek elé fűzve). */
    public static final String[] COMMON = {
            "Guggolás", "Fekvenyomás", "Felhúzás", "Vállból nyomás", "Evezés",
            "Bicepsz", "Tricepsz", "Kitörés", "Lábtolás", "Vádliemelés"
    };

    /** Egy sorozat: ismétlésszám + súly (kg). */
    public static final class SetEntry {
        public final int reps;
        public final double weight;
        public SetEntry(int reps, double weight) { this.reps = reps; this.weight = weight; }
    }

    /** Egy naplóbejegyzés: időpont + gyakorlat + sorozatok. */
    public static final class Entry {
        public final long ts;
        public final String name;
        public final List<SetEntry> sets;
        public Entry(long ts, String name, List<SetEntry> sets) {
            this.ts = ts; this.name = name; this.sets = sets;
        }
        public double topWeight() {
            double m = 0; for (SetEntry s : sets) m = Math.max(m, s.weight); return m;
        }
        public double volume() {
            double v = 0; for (SetEntry s : sets) v += s.reps * s.weight; return v;
        }
        /** Becsült egyismétléses maximum (Epley-képlet) a legjobb sorozatból. */
        public double bestOneRm() {
            double m = 0;
            for (SetEntry s : sets) m = Math.max(m, s.weight * (1 + s.reps / 30.0));
            return m;
        }
        public int totalReps() { int r = 0; for (SetEntry s : sets) r += s.reps; return r; }
    }

    public static List<Entry> load(Context c) {
        List<Entry> out = new ArrayList<>();
        String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                List<SetEntry> sets = new ArrayList<>();
                JSONArray sa = o.optJSONArray("sets");
                if (sa != null) for (int j = 0; j < sa.length(); j++) {
                    JSONObject so = sa.optJSONObject(j);
                    if (so == null) continue;
                    sets.add(new SetEntry(so.optInt("r"), so.optDouble("w")));
                }
                out.add(new Entry(o.optLong("ts"), o.optString("name", "Gyakorlat"), sets));
            }
        } catch (Exception ignored) {}
        return out;
    }

    static void save(Context c, List<Entry> list) {
        JSONArray a = new JSONArray();
        for (Entry e : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("ts", e.ts);
                o.put("name", e.name);
                JSONArray sa = new JSONArray();
                for (SetEntry s : e.sets) {
                    JSONObject so = new JSONObject();
                    so.put("r", s.reps);
                    so.put("w", s.weight);
                    sa.put(so);
                }
                o.put("sets", sa);
                a.put(o);
            } catch (Exception ignored) {}
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }

    public static void add(Context c, Entry e) {
        List<Entry> l = load(c);
        l.add(0, e);           // legújabb elöl
        save(c, l);
    }

    public static void removeAt(Context c, int idx) {
        List<Entry> l = load(c);
        if (idx >= 0 && idx < l.size()) { l.remove(idx); save(c, l); }
    }

    /** Egy bejegyzés cseréje a helyén (szerkesztéshez – megtartja a sorrendet). */
    public static void replaceAt(Context c, int idx, Entry e) {
        List<Entry> l = load(c);
        if (idx >= 0 && idx < l.size()) { l.set(idx, e); save(c, l); }
    }

    /** Törlés időbélyeg alapján – szűrt listából is biztonságos. */
    public static void removeByTs(Context c, long ts) {
        List<Entry> l = load(c);
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).ts == ts) l.remove(i);
        save(c, l);
    }

    /** Csere időbélyeg alapján – szűrt listából is biztonságos. */
    public static void replaceByTs(Context c, long ts, Entry e) {
        List<Entry> l = load(c);
        for (int i = 0; i < l.size(); i++) if (l.get(i).ts == ts) { l.set(i, e); break; }
        save(c, l);
    }

    /** Ismert gyakorlatnevek: a korábban használtak (legutóbbi elöl) + a gyakoriak. */
    public static List<String> knownNames(Context c) {
        LinkedHashMap<String, Boolean> seen = new LinkedHashMap<>();
        for (Entry e : load(c)) if (e.name != null && !e.name.isEmpty()) seen.put(e.name, true);
        for (String s : COMMON) if (!seen.containsKey(s)) seen.put(s, true);
        return new ArrayList<>(seen.keySet());
    }

    /** Egy gyakorlat rekordjai: [max súly, becsült 1RM, legjobb heti... itt: legjobb volumen egy edzésen]. */
    public static double[] recordsFor(Context c, String name) {
        double maxW = 0, maxOrm = 0, maxVol = 0;
        for (Entry e : load(c)) {
            if (!name.equals(e.name)) continue;
            maxW = Math.max(maxW, e.topWeight());
            maxOrm = Math.max(maxOrm, e.bestOneRm());
            maxVol = Math.max(maxVol, e.volume());
        }
        return new double[]{maxW, maxOrm, maxVol};
    }
}
