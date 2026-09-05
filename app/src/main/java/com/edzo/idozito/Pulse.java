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
            // A NAPSZAK beékelődhet a pulzus-szó és a szám közé: a „nyugalmi
            // pulzus reggel 47" a legtermészetesebb alak, és eddig egyáltalán
            // nem létezett – a mérés némán elveszett. Csak a napszak fér be,
            // más szó nem: attól a szám már máshoz tartozhatna.
            // A „ma" magában is beékelődhet („nyugalmi pulzusom ma 52"), és
            // a PIHENŐPULZUS ugyanaz a mérés más néven – mindkettő némán
            // elveszett.
            // A VÁLTOZÁS igéje is beékelődhet: a „pihenőpulzusom lement
            // 52-ről 49-re" a ról-re átírás után „pulzusom lement 49".
            java.util.regex.Pattern.compile(
                    "(?<![a-z])(?:piheno)?pulzus\\w*\\s?:?\\s?"
                            + "(?:(?:ma|reggel|este|ejjel|hajnalban|delben|"
                            + "ebredeskor|ebredes utan|most|volt|lement|"
                            // A NYUGALOMBAN ugyanaz a mérés más ragozásban: az
                            // „a vérem 130/85, a pulzusom nyugalomban 58"
                            // ötvennyolca némán elveszett.
                            + "nyugalomban|pihenoben|nyugalmi allapotban|"
                            + "felment|javult|romlott)\\s){0,2}(\\d{2,3})"),
            // „52-es pulzus", „48 as nyugalmi pulzus"
            // Az Ö HANGRENDŰ toldalék is ugyanez: az „55-ös pulzussal
            // ébredtem" és a „reggel 55-ös pulzus" némán elveszett, mert a
            // minta csak az „-es" és az „-as" alakot ismerte.
            java.util.regex.Pattern.compile(
                    "(\\d{2,3})[- ]?[aeo]s\\s(?:nyugalmi\\s)?pulzus"),
            // Az óra-appok rövidítése.
            java.util.regex.Pattern.compile("(?<![a-z])rhr\\s?:?\\s?(\\d{2,3})"),
            // Fordítva is: az „54 rhr" a tömör napló-sor alakja.
            java.util.regex.Pattern.compile("(\\d{2,3})\\s?-?\\s?rhr(?![a-z])"),
            // „ma reggel 47 volt a nyugalmi pulzusom": a szám ELÖL áll, a
            // pulzus-szó a mondat végén. Ez a legtermészetesebb magyar alak,
            // és eddig nem létezett – a „nyugalmi" szó itt kötelező, mert
            // enélkül a mondat bármelyik száma odaeshetne.
            java.util.regex.Pattern.compile(
                    "(\\d{2,3})[^0-9]{0,16}?nyugalmi\\s?pulzus"),
            // A PULZUS szó el is maradhat: a „hrv 62 ms, nyugalmi 49" a
            // sportóra-leolvasás legrövidebb alakja, és eddig némán elveszett.
            // A „nyugalmi" magában is kimondja, miről van szó – más
            // nyugalmi értéket senki nem ír egy edzésnaplóba.
            // A VESSZŐ is beleférhet: a „magas volt ma a nyugalmi, 61"
            // hatvanegye eddig elveszett.
            // …de a vessző utáni szám csak akkor a pulzus, ha nem egy MÁSIK
            // mérés kezdete: az „52 nyugalmi, 80,4 kg" mondatban a nyolcvan
            // a mérleg száma, mégis pulzusként ment be – a valódi ötvenkettő
            // meg elveszett, mert ez a minta hamarabb talált, mint a
            // fordított szórendű alatta.
            java.util.regex.Pattern.compile(
                    "(?<![a-z])nyugalmi\\w*\\s?[:,]?\\s?(\\d{2,3})"
                            + "(?![0-9])(?![,.]\\d)(?!\\s?(?:kg|kilo|cm|%))"),
            // A NYUGALOMBAN és a PIHENŐBEN ugyanazt a mérést nevezi meg.
            java.util.regex.Pattern.compile(
                    "(?<![a-z])(?:nyugalomban|pihenoben)\\s?[:,]?\\s?(\\d{2,3})"
                            + "(?![0-9])(?![,.]\\d)(?!\\s?(?:kg|kilo|cm|%))"),
            // Ugyanez fordított szórenddel: „reggel 78,4 kg, 7 óra alvás,
            // 54 nyugalmi". A szám és a szó között itt semmi nem állhat –
            // távolabbról a szám már máshoz tartozhatna.
            java.util.regex.Pattern.compile(
                    "(\\d{2,3})\\s?-?[ae]?s?\\s?nyugalmi(?![a-z])"),
            // Az ÉBREDÉSKOR mért érték nyugalmi pulzus akkor is, ha a szám a
            // pulzus-szó ELŐTT áll: a „reggel ébredéskor 48 volt a pulzusom,
            // edzés után 145" mérése némán elveszett – a „nyugalmi" szót
            // ugyanis nem mondta ki senki, pedig az ébredés maga a nyugalom.
            // Az edzés utáni érték nem eshet ide: az az ébredés-szó UTÁN,
            // de a pulzus-szó után áll.
            java.util.regex.Pattern.compile(
                    "(?<![a-z])(?:ebredeskor|ebredes utan|felebredve|"
                            + "felkeleskor|kelesnel)[^0-9]{0,14}?(\\d{2,3})"
                            + "[^0-9]{0,16}?pulzus"),
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
        // A VÁLTOZÁS mondatában a MÁSODIK szám a mai érték: a „nyugalmi
        // pulzusom 48-ról 45-re javult" mai értéke negyvenöt – eddig a régi
        // került a trendbe, vagyis a javulás napján egy romlás.
        s = s.replaceAll("(\\d{2,3})\\s?-?r[o\u00f3]l\\b[^0-9]{0,12}?"
                + "(\\d{2,3})\\s?-?r[ae]\\b", "$2");
        // A K\u00dcSZ\u00d6B \u00e1tl\u00e9p\u00e9se nem m\u00e9r\u00e9s, a m\u00f6g\u00f6tte \u00e1ll\u00f3 mai sz\u00e1m az: a
        // \u201enyugalmi pulzusom lement 50 al\u00e1, ma 49" negyvenkilence eddig
        // elveszett, mert a k\u00fcsz\u00f6b-sz\u00e1m el\u00e1llta a minta \u00fatj\u00e1t.
        s = s.replaceAll("(?:lement|felment|ment)\\s+\\d{2,3}\\s?"
                + "(?:ala|fole|koze)\\b,?\\s*", "");
        // A CÉL nem mérés: a „szeretném, ha 50 lenne a nyugalmi pulzusom" és
        // az „a cél 50-es nyugalmi pulzus" ugyanúgy tartalmaz számot és
        // pulzus-szót, mint egy bejegyzés – csak épp az ellenkezőjét mondja.
        // Az alvásnál és a testsúlynál ez a szabály régóta megvan.
        for (String w : new String[]{"szeretn", "jo lenne", "kellene", "kene",
                "holnap", "fogok", "legyen", "volna"})
            if (s.contains(w)) return -1;
        for (String w : new String[]{"cel", "celom", "celja", "celt"}) {
            int i = s.indexOf(w);
            while (i >= 0) {
                boolean l = i == 0 || !Character.isLetter(s.charAt(i - 1));
                int e = i + w.length();
                boolean r = e >= s.length() || !Character.isLetter(s.charAt(e));
                if (l && r) return -1;
                i = s.indexOf(w, i + 1);
            }
        }
        // A PIHENŐPULZUS szó ugyanolyan erős, mint a „nyugalmi": aki
        // kimondja, az a reggeli mérésről beszél – akkor is, ha utána az
        // edzés közbeni maximumát is odaírja. A „pihenőpulzus 52, edzés
        // közben max 178" ötvenkettője eddig némán elveszett.
        // A REGGELI pulzus is nyugalmi mérés: a „reggel 52-es pulzus,
        // délben futás 8 km" ötvenkettője eddig elveszett, mert a futás
        // szava az egész mondatot edzésnek minősítette.
        // Az ÉBREDÉSKOR szó ugyanilyen erős: a „reggel ébredéskor 48 volt a
        // pulzusom, edzés után 145" mérése az edzés szava miatt némán
        // elveszett – pedig az ébredés maga a nyugalmi állapot.
        if (!s.contains("nyugalmi") && !s.contains("pihenopulzus")
                && !s.contains("nyugalomban") && !s.contains("pihenoben")
                && !s.contains("ebredeskor") && !s.contains("ebredes utan")
                // Az ÉBREDÉS IGÉJE ugyanilyen erős: az „edzés után 55-ös
                // pulzussal ébredtem, jól regenerálódtam" ötvenötje a
                // reggeli mérés – az edzés szava mégis elnémította. Aki
                // ébredéskor mér, az a nyugalmi értékét méri.
                && !s.matches(".*(?<![a-z])(?:ebredtem|felebredtem|keltem)"
                        + "(?![a-z]).*")
                && !s.matches(".*(?<![a-z])reggeli?\\s[^,;.]{0,10}?pulzus.*"))
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
