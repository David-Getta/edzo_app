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

    // Ugyanaz a gond, mint a History-nál: a kezdőlap és a Statisztika egy
    // frissítés alatt sokszor kéri el a naplót, ezért a nyers szövegre kötött
    // gyorsítótárral elkerüljük az ismételt JSON-értelmezést.
    private static String cachedRaw;
    private static List<Entry> cachedList;

    /** A napló nyers JSON-szövege (a History gyorsítótárának kulcsához). */
    static String rawJson(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
    }

    /** A napló olvasásra. A visszakapott listát NE módosítsd – lásd loadForEdit. */
    public static List<Entry> load(Context c) {
        String s = rawJson(c);
        List<Entry> cached = cachedList;
        if (cached != null && s.equals(cachedRaw)) return cached;
        List<Entry> parsed = parse(s);
        cachedRaw = s;
        cachedList = parsed;
        return parsed;
    }

    /** Friss, módosítható példány – a mentő metódusok ezt használják. */
    private static List<Entry> loadForEdit(Context c) {
        return parse(rawJson(c));
    }

    private static List<Entry> parse(String s) {
        List<Entry> out = new ArrayList<>();
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
        List<Entry> l = loadForEdit(c);
        l.add(0, e);           // legújabb elöl
        save(c, l);
    }

    public static void removeAt(Context c, int idx) {
        List<Entry> l = loadForEdit(c);
        if (idx >= 0 && idx < l.size()) { l.remove(idx); save(c, l); }
    }

    /** Egy bejegyzés cseréje a helyén (szerkesztéshez – megtartja a sorrendet). */
    public static void replaceAt(Context c, int idx, Entry e) {
        List<Entry> l = loadForEdit(c);
        if (idx >= 0 && idx < l.size()) { l.set(idx, e); save(c, l); }
    }

    /** Törlés időbélyeg alapján – szűrt listából is biztonságos. */
    public static void removeByTs(Context c, long ts) {
        List<Entry> l = loadForEdit(c);
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).ts == ts) l.remove(i);
        save(c, l);
    }

    /** Csere időbélyeg alapján – szűrt listából is biztonságos. */
    public static void replaceByTs(Context c, long ts, Entry e) {
        List<Entry> l = loadForEdit(c);
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

    /** Ennyi nap után tekintünk egy gyakorlatot elhanyagoltnak. */
    public static final int NEGLECTED_DAYS = 14;

    /**
     * Hány napja volt a gyakorlat utolsó alkalma (naptári napokban), vagy -1,
     * ha még sosem szerepelt. Naptári nap, nem 24 óra: aki tegnap este és ma
     * reggel edzett, annak „tegnap" jár, ne „0 napja".
     */
    public static int daysSince(List<Entry> log, String name, long now) {
        long last = -1;
        for (Entry e : log) if (name.equals(e.name) && e.ts > last) last = e.ts;
        return last < 0 ? -1 : dayDiff(last, now);
    }

    /** Naptári napok különbsége; a kerekítés az óraátállást is elnyeli. */
    static int dayDiff(long from, long to) {
        java.util.Calendar a = java.util.Calendar.getInstance();
        java.util.Calendar b = java.util.Calendar.getInstance();
        a.setTimeInMillis(from); zeroTime(a);
        b.setTimeInMillis(to);   zeroTime(b);
        return (int) Math.round((b.getTimeInMillis() - a.getTimeInMillis()) / 86400000.0);
    }

    private static void zeroTime(java.util.Calendar c) {
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
    }

    /**
     * Hány KÜLÖNBÖZŐ napon volt erősítő edzés az elmúlt `days` napban. Napokban
     * mérünk, nem bejegyzésekben: aki egy nap öt gyakorlatot rögzít, az egy nap.
     */
    public static int daysTrainedIn(List<Entry> log, long now, int days) {
        java.util.HashSet<Integer> seen = new java.util.HashSet<>();
        for (Entry e : log) {
            int ago = dayDiff(e.ts, now);
            if (ago >= 0 && ago < days) seen.add(ago);
        }
        return seen.size();
    }

    /** „ma" / „tegnap" / „5 napja"; ismeretlenre üres szöveg. */
    public static String agoLabel(int days) {
        if (days < 0) return "";
        if (days == 0) return "ma";
        if (days == 1) return "tegnap";
        return days + " napja";
    }

    /**
     * A legrégebben csinált gyakorlat neve, ha legalább minDays napja kimaradt –
     * különben null. Erre való a „mit hanyagolsz el" emlékeztető.
     */
    public static String mostNeglected(List<Entry> log, long now, int minDays) {
        LinkedHashMap<String, Long> lastOf = new LinkedHashMap<>();
        for (Entry e : log) {
            if (e.name == null || e.name.isEmpty()) continue;
            Long prev = lastOf.get(e.name);
            if (prev == null || e.ts > prev) lastOf.put(e.name, e.ts);
        }
        String worst = null;
        long worstTs = Long.MAX_VALUE;
        for (java.util.Map.Entry<String, Long> en : lastOf.entrySet()) {
            if (en.getValue() < worstTs) { worstTs = en.getValue(); worst = en.getKey(); }
        }
        if (worst == null || dayDiff(worstTs, now) < minDays) return null;
        return worst;
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
