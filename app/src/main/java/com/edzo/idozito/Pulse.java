package com.edzo.idozito;

/**
 * Nyugalmi pulzus – mondatból is.
 *
 * A reggeli pihenőpulzus a legolcsóbb edzettség-mérő: ahogy nő az állóképesség,
 * úgy megy lejjebb, és a szokásosnál 5–8-cal magasabb érték korán jelzi a
 * túlterhelést vagy a kezdődő betegséget. Az óra méri, az app eddig nem
 * hallotta meg – pedig a „nyugalmi pulzus 52" ugyanolyan mondat, mint az
 * „aludtam 8 órát".
 *
 * Szándékosan minimális: napi egy szám. Az edzés közbeni pulzus nem ide
 * tartozik – arra ott az óra kijelzője.
 */
public final class Pulse {

    private Pulse() {}

    /** Életszerű nyugalmi tartomány. Ami e fölött van, az nem pihenőérték. */
    static final int MIN_BPM = 30, MAX_BPM = 110;

    /**
     * A pulzus-alakok. A szó eleje kötött („pulzus…"), mert az
     * „átlagpulzus" edzés-adat, nem reggeli mérés.
     */
    private static final java.util.regex.Pattern[] FORMS = {
            // „pulzus 52", „pulzusom: 48", „nyugalmi pulzus 55 volt"
            java.util.regex.Pattern.compile(
                    "(?<![a-z])pulzus\\w*\\s?:?\\s?(\\d{2,3})"),
            // „52-es pulzus", „48 as nyugalmi pulzus"
            java.util.regex.Pattern.compile(
                    "(\\d{2,3})[- ]?[ae]s\\s(?:nyugalmi\\s)?pulzus"),
            // Az óra-appok rövidítése.
            java.util.regex.Pattern.compile("(?<![a-z])rhr\\s?:?\\s?(\\d{2,3})"),
    };

    /**
     * A mondatban kimondott nyugalmi pulzus, vagy -1.
     *
     * Az edzés-szavas mondat („futás átlagpulzus 165") nem nyugalmi érték –
     * kivéve, ha a „nyugalmi" szó ki van mondva: az felülír minden gyanút.
     */
    public static int parse(String q) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        if (!s.contains("nyugalmi"))
            for (String g : new String[]{"atlag", "max", "kozben", "edzes", "futas",
                    "futottam", "seta", "bringa", "terheles"})
                if (s.contains(g)) return -1;
        for (java.util.regex.Pattern p : FORMS) {
            java.util.regex.Matcher m = p.matcher(s);
            if (!m.find()) continue;
            int v;
            try { v = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException e) { continue; }
            if (v >= MIN_BPM && v <= MAX_BPM) return v;
        }
        return -1;
    }

    /** Egysoros visszajelzés – tanács, nem diagnózis. */
    public static String verdict(int bpm) {
        if (bpm <= 0) return "";
        if (bpm < 50) return "sportszív-tartomány – szép munka";
        if (bpm < 60) return "kiváló – edzett pihenőpulzus";
        if (bpm < 70) return "rendben van";
        if (bpm < 80) return "átlagos – a rendszeres kardió lejjebb viszi";
        return "magas – mérd pár reggelen át, és ha marad, kérdezz orvost";
    }

    // ---------- Tárolás ----------

    static final String KEY = "pulse_log";
    /** Ennyi bejegyzést tartunk meg – bő fél év. */
    static final int MAX = 200;

    /** Pulzus mentése a MAI napra (napi egy érték: az újabb felülírja). */
    public static void add(android.content.Context c, long ts, int bpm) {
        try {
            org.json.JSONArray a = load(c);
            org.json.JSONArray out = new org.json.JSONArray();
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("ts", ts);
            o.put("b", bpm);
            out.put(o);
            long day = Days.index(ts);
            for (int i = 0; i < a.length() && out.length() < MAX; i++) {
                org.json.JSONObject e = a.optJSONObject(i);
                if (e == null) continue;
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

    /** A legutóbbi bejegyzett érték, vagy -1. */
    public static int last(android.content.Context c) {
        org.json.JSONArray a = load(c);
        org.json.JSONObject o = a.optJSONObject(0);
        return o == null ? -1 : o.optInt("b", -1);
    }

    /** Napi értékek időrendben (régi → új) az elmúlt N napból, a görbéhez. */
    public static double[] series(android.content.Context c, long now, int days) {
        org.json.JSONArray a = load(c);
        java.util.List<Double> vals = new java.util.ArrayList<>();
        for (int i = a.length() - 1; i >= 0; i--) {
            org.json.JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            int ago = Days.ago(o.optLong("ts"), now);
            if (ago < 0 || ago >= days) continue;
            int b = o.optInt("b", -1);
            if (b > 0) vals.add((double) b);
        }
        double[] out = new double[vals.size()];
        for (int i = 0; i < out.length; i++) out[i] = vals.get(i);
        return out;
    }

    /** Átlag az elmúlt N napra (csak a bejegyzett napokból), vagy -1. */
    public static double avg(android.content.Context c, long now, int days) {
        org.json.JSONArray a = load(c);
        double sum = 0;
        int n = 0;
        for (int i = 0; i < a.length(); i++) {
            org.json.JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            int ago = Days.ago(o.optLong("ts"), now);
            if (ago < 0 || ago >= days) continue;
            int b = o.optInt("b", -1);
            if (b > 0) { sum += b; n++; }
        }
        return n == 0 ? -1 : sum / n;
    }
}
