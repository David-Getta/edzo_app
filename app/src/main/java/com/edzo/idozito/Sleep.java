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
            // „aludtam 8 órát", „aludtam kb 6,5 órát", „aludtam vagy 7 órát" –
            // a szám és az ige közé pár rövid szó beférhet, de csak kevés:
            // messzebbről a szám már másról szólhat.
            // A tagmondat-határ és az ÓRA-SZÓ is kell: a „7,5 órát aludtam, de
            // 3-szor felébredtem" hármasa az ébredések száma, nem az alvás
            // hossza – eddig három órás éjszaka került a naplóba a hét és fél
            // helyett.
            java.util.regex.Pattern.compile(
                    "aludtam[^0-9,;.]{0,12}?(\\d{1,2}([.,]\\d)?)"
                            + "\\s?(?:ora|h(?![a-z])|$|[,.;])"),
            // „8 óra alvás", „7,5 óra alvás"
            java.util.regex.Pattern.compile(
                    "(\\d{1,2}([.,]\\d)?)\\s?ora(?:t)?\\s?alvas"),
            // „alvás: 8", „alvás 7,5 óra"
            java.util.regex.Pattern.compile(
                    "alvas\\w*\\s?:?\\s?(\\d{1,2}([.,]\\d)?)"),
            // „7,5 órát aludtam", „csak 4 órát aludtam", „tegnap 6 órát
            // aludtam összesen” – a szám ELÖL áll, az ige mögötte. Ez a
            // leggyakoribb magyar szórend, és eddig egyszerűen kiesett: aki
            // így írta le, semmit nem kapott vissza.
            java.util.regex.Pattern.compile(
                    "(\\d{1,2}([.,]\\d)?)\\s?ora\\w*[^0-9]{0,14}?aludtam"),
            // Óra-szó NÉLKÜL is: „nyolcat aludtam", „kb 8-at aludtam". A
            // magyar az órát ilyenkor elhagyja, mert magától értetődik – a
            // szám és az ige közé viszont csak a tárgyrag férhet be.
            java.util.regex.Pattern.compile(
                    "(\\d{1,2}([.,]\\d)?)\\s?-?(?:[oae]t)?\\s*aludtam"),
    };

    /** Alvásról szól-e egyáltalán a mondat. */
    private static boolean saysSleep(String s) {
        return s.contains("alud") || s.contains("alvas") || s.contains("aludt");
    }

    /**
     * A mondatban kimondott alvásóra, vagy -1.
     *
     * A „hét és fél órát aludtam" a számnév-fordítás után „7 es 0,5 orat" –
     * a közvetlenül a szám után álló „és fél" hozzáadódik.
     */
    public static double parse(String q) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        // A feltételes mód pont az ellenkezőjét jelenti: az „aludtam volna
        // nyolc órát" egy rossz éjszaka panasza, nem nyolc óra alvás.
        if (s.contains("volna") || s.contains("kellett volna") || s.contains("szerettem"))
            return -1;
        // Lefekvés és ébredés: „este 11-kor feküdtem, reggel 7-kor keltem".
        // Sokan nem hosszat írnak, hanem két időpontot – az óra is így méri.
        double span = betweenTimes(s);
        if (span > 0) return span;
        // Óra ÉS perc: a „6 óra 30 perc alvás" fél órája eddig elveszett –
        // sőt az egész mondat, mert a perc a szám mellé állva elrontotta a
        // mintát. Alvás-szó nélkül ez az ág nem él.
        if (saysSleep(s)) {
            // Az „alvás 6:30" hossz, nem időpont: az óra-appok így írják ki.
            // A perc eddig elveszett belőle – fél óra, minden éjszakán.
            java.util.regex.Matcher cm = java.util.regex.Pattern
                    .compile("alvas\\w*\\s?:?\\s?(\\d{1,2}):(\\d{2})").matcher(s);
            if (cm.find()) {
                double v = Integer.parseInt(cm.group(1)) + Integer.parseInt(cm.group(2)) / 60.0;
                if (v >= MIN_H && v <= MAX_H) return Math.round(v * 10) / 10.0;
            }
            java.util.regex.Matcher hm = java.util.regex.Pattern
                    .compile("(\\d{1,2})\\s?ora\\w*\\s?(\\d{1,2})\\s?perc").matcher(s);
            if (hm.find()) {
                double v = Integer.parseInt(hm.group(1)) + Integer.parseInt(hm.group(2)) / 60.0;
                if (v >= MIN_H && v <= MAX_H) return Math.round(v * 10) / 10.0;
            }
            // „hét és fél órát aludtam": a számnév-fordítás után „7 es 0,5
            // orat", ahol a fél KÜLÖN számként áll. A régi összeadás csak az
            // ige mögötti alakra élt, az elöl álló számra nem.
            java.util.regex.Matcher fm = java.util.regex.Pattern
                    .compile("(\\d{1,2})\\s?es\\s?0,5\\s?ora").matcher(s);
            if (fm.find()) {
                double v = Integer.parseInt(fm.group(1)) + 0.5;
                if (v >= MIN_H && v <= MAX_H) return v;
            }
            // Tól-ig: a „8-9 órát aludtam" közepét vesszük. Enélkül a pár úgy
            // nézett ki, mint egy munka/pihenő ritmus, és az időzítőbe ment.
            java.util.regex.Matcher rm = java.util.regex.Pattern
                    .compile("(\\d{1,2})\\s?-\\s?(\\d{1,2})\\s?ora").matcher(s);
            if (rm.find()) {
                double lo = Integer.parseInt(rm.group(1)), hi = Integer.parseInt(rm.group(2));
                if (hi > lo) {
                    double v = (lo + hi) / 2.0;
                    if (v >= MIN_H && v <= MAX_H) return v;
                }
            }
        }
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

    /**
     * Két időpont közti alvás, vagy -1: „este 11-kor feküdtem, 7-kor keltem".
     *
     * Sokan nem a hosszat írják le, hanem a lefekvést és az ébredést – az óra
     * is így méri, és a fejben kivonás pont az a lépés, amit egy appnak el
     * kellene végeznie. A kimondott lefekvés-ébredés páros kötelező, hogy egy
     * edzés-időpont („18:00-tól 19:30-ig kondi") ne váljon alvássá.
     *
     * A délelőtti óraszám (a „fél 11-kor feküdtem" tíz-harmincja) este
     * értendő: ha az első időpontból képtelen hossz jön ki, tizenkét órát
     * hozzáadva próbáljuk újra.
     */
    private static double betweenTimes(String s) {
        // A lefekvést nem csak „feküdtem"-mel mondjuk: az „ágyban voltam", a
        // „lefeküdtem" és az „ágyba bújtam" ugyanaz a pillanat. Enélkül az
        // „este 10-re ágyban voltam, reggel 6-kor keltem" egésze elveszett,
        // pedig a nyolc óra ki van mondva benne.
        boolean bed = s.contains("fekudtem") || s.contains("fekszem")
                || s.contains("lefeku") || s.contains("agyban volt")
                || s.contains("agyba bujt") || s.contains("agyban vagyok");
        boolean up = s.contains("keltem") || s.contains("ebredtem");
        boolean ctx = s.contains("alud") || s.contains("alvas") || (bed && up);
        if (!ctx) return -1;
        java.util.List<Integer> mins = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                // „22:30" – óra és perc kettősponttal
                "(?<![\\d,])(\\d{1,2}):(\\d{2})"
                // „11-kor", „23 órakor", „este 10"
                + "|(?<![\\d,:])(\\d{1,2})\\s?-?\\s?(?:orakor|kor)"
                + "|(?:este|reggel|ejjel|hajnalban|delelott)\\s(\\d{1,2})(?![\\d:,])")
                .matcher(s);
        while (m.find() && mins.size() < 2) {
            int h, mi = 0;
            if (m.group(1) != null) { h = Integer.parseInt(m.group(1)); mi = Integer.parseInt(m.group(2)); }
            else if (m.group(3) != null) h = Integer.parseInt(m.group(3));
            else h = Integer.parseInt(m.group(4));
            if (h > 24) continue;
            // A magyar „fél tizenegy" tíz óra harminc – a számnév-fordítás
            // után a „0,5" külön számként áll az óra ELŐTT.
            int b = m.start();
            if (b >= 4 && s.startsWith("0,5 ", b - 4)) { h = (h + 23) % 24; mi = 30; }
            mins.add((h % 24) * 60 + mi);
        }
        if (mins.size() < 2) return -1;
        for (int shift : new int[]{0, 12 * 60}) {
            int from = (mins.get(0) + shift) % (24 * 60);
            int to = mins.get(1);
            int diff = to - from;
            if (diff <= 0) diff += 24 * 60;
            double v = Math.round(diff / 6.0) / 10.0;
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
