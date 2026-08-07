package com.edzo.idozito;

/**
 * Alvás-napló: hány órát aludtál – mondatból is.
 *
 * A regeneráció az edzés másik fele: ugyanaz a terhelés hét óra alvással
 * fejlődés, néggyel csak fáradtság. Az app eddig mindent tudott a munkáról
 * (edzés, étrend, mérés), a pihenésről semmit – pedig az „aludtam 8 órát"
 * ugyanolyan egyszerű mondat, mint a „78 kg vagyok".
 *
 * Szándékosan minimális: egy szám naponta. Se alvásfázis, se időpont –
 * azt mérje az óra; itt annyi kell, hogy a heti átlag mellé lehessen tenni
 * az edzésmennyiséget.
 */
public final class Sleep {

    private Sleep() {}

    /** Életszerű alváshossz órában. A négy alatti szunyókálás, nem éjszaka. */
    static final double MIN_H = 2, MAX_H = 16;

    /** Ennyi órát tekintünk egészséges alsó határnak a visszajelzésnél. */
    static final double GOOD_H = 7;

    /**
     * Az alvás-szavak: enélkül a puszta „8 óra" bármi lehetne.
     *
     * A „8 óra alvás" és az „aludtam 8 órát" a két természetes alak; a
     * „szunyókáltam" szándékosan nincs itt – az nem éjszakai alvás.
     */
    private static final java.util.regex.Pattern[] FORMS = {
            // „aludtam 8 órát", „aludtam 7,5 órát", „ma éjjel aludtam 8-at"
            java.util.regex.Pattern.compile(
                    "aludtam\\s?(\\d{1,2}([.,]\\d)?)"),
            // „8 óra alvás", „7,5 óra alvás"
            java.util.regex.Pattern.compile(
                    "(\\d{1,2}([.,]\\d)?)\\s?ora(?:t)?\\s?alvas"),
            // „alvás: 8", „alvás 7,5 óra"
            java.util.regex.Pattern.compile(
                    "alvas\\w*\\s?:?\\s?(\\d{1,2}([.,]\\d)?)"),
    };

    /**
     * A mondatban kimondott alvásóra, vagy -1.
     *
     * A „hét és fél órát aludtam" a számnév-fordítás után „7 es 0,5 orat" –
     * a közvetlenül a szám után álló „és fél" hozzáadódik.
     */
    public static double parse(String q) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        for (java.util.regex.Pattern p : FORMS) {
            java.util.regex.Matcher m = p.matcher(s);
            if (!m.find()) continue;
            double v;
            try { v = Double.parseDouble(m.group(1).replace(',', '.')); }
            catch (NumberFormatException e) { continue; }
            // „7 es 0,5 orat aludtam": a tört külön számként áll a fő szám után.
            if (s.contains(m.group(1) + " es 0,5")) v += 0.5;
            if (v >= MIN_H && v <= MAX_H) return v;
        }
        return -1;
    }

    /** Egysoros visszajelzés a mennyiséghez – tanács, nem ítélet. */
    public static String verdict(double hours) {
        if (hours <= 0) return "";
        if (hours < 6) return "kevés – a regeneráció ennyiből nehezen megy";
        if (hours < GOOD_H) return "a hét óra alatt – ha teheted, told meg";
        if (hours <= 9.5) return "rendben – a fejlődés ilyenkor történik";
        return "hosszú éjszaka – néha pont erre van szükség";
    }

    // ---------- Tárolás ----------

    static final String KEY = "sleep_log";
    /** Ennyi bejegyzést tartunk meg – bő fél év. */
    static final int MAX = 200;

    /** Alvás mentése a MAI napra (napi egy érték: az újabb felülírja). */
    public static void add(android.content.Context c, long ts, double hours) {
        try {
            org.json.JSONArray a = load(c);
            org.json.JSONArray out = new org.json.JSONArray();
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("ts", ts);
            o.put("h", hours);
            out.put(o);
            long day = Days.index(ts);
            for (int i = 0; i < a.length() && out.length() < MAX; i++) {
                org.json.JSONObject e = a.optJSONObject(i);
                if (e == null) continue;
                // Napi egy érték: aki javít, annak a régi szám menjen.
                if (Days.index(e.optLong("ts")) == day) continue;
                out.put(e);
            }
            c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE)
                    .edit().putString(KEY, out.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** A napló, legfrissebb elöl. */
    public static org.json.JSONArray load(android.content.Context c) {
        try {
            return new org.json.JSONArray(c.getSharedPreferences("edzo",
                    android.content.Context.MODE_PRIVATE).getString(KEY, "[]"));
        } catch (Exception e) {
            return new org.json.JSONArray();
        }
    }

    /** A legutóbbi bejegyzett éjszaka órái, vagy -1. */
    public static double last(android.content.Context c) {
        org.json.JSONArray a = load(c);
        org.json.JSONObject o = a.optJSONObject(0);
        return o == null ? -1 : o.optDouble("h", -1);
    }

    /**
     * Napi értékek időrendben (régi → új) az elmúlt N napból, a görbéhez.
     *
     * Csak a bejegyzett éjszakák kerülnek bele – a kihagyott nap nem nulla
     * óra alvás, hanem ismeretlen, és egy nulla a görbét a padlóra rántaná.
     */
    public static double[] series(android.content.Context c, long now, int days) {
        org.json.JSONArray a = load(c);
        java.util.List<Double> vals = new java.util.ArrayList<>();
        for (int i = a.length() - 1; i >= 0; i--) {
            org.json.JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            int ago = Days.ago(o.optLong("ts"), now);
            if (ago < 0 || ago >= days) continue;
            double h = o.optDouble("h", -1);
            if (h > 0) vals.add(h);
        }
        double[] out = new double[vals.size()];
        for (int i = 0; i < out.length; i++) out[i] = vals.get(i);
        return out;
    }

    /** Átlag az elmúlt N napra (csak a bejegyzett éjszakákból), vagy -1. */
    public static double avg(android.content.Context c, long now, int days) {
        org.json.JSONArray a = load(c);
        double sum = 0;
        int n = 0;
        for (int i = 0; i < a.length(); i++) {
            org.json.JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            int ago = Days.ago(o.optLong("ts"), now);
            if (ago < 0 || ago >= days) continue;
            double h = o.optDouble("h", -1);
            if (h > 0) { sum += h; n++; }
        }
        return n == 0 ? -1 : sum / n;
    }
}
