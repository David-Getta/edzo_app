package com.edzo.idozito;

import java.util.Locale;

/**
 * Magyar számformázás egy helyen.
 *
 * Az app szövege magyar, a tizedeselválasztó tehát vessző: „5,2 km", nem
 * „5.2 km". Ez korábban szétcsúszott – a víz és a napi kihívás vesszővel írt,
 * a táv, a tempó, a BMI és a testsúly viszont ponttal.
 *
 * IDŐ-formátumokhoz szándékosan NEM ezt használjuk: ott nincs tizedes, viszont
 * a rögzített (US) locale garantálja az ASCII számjegyeket. A CSV-export is
 * marad ponttal, hogy bármelyik táblázatkezelő és nyelv beolvassa.
 */
public final class Hu {

    private Hu() {}

    public static final Locale LOCALE = new Locale("hu");

    /**
     * Súly kiírása: egész, ha kerek (40), egyébként egy tizedessel (42,5).
     *
     * Egy „120,0 kg"-os rekord úgy néz ki, mintha a tizedes számítana – a
     * teremben viszont senki nem mond nullát a vessző után.
     */
    public static String kg(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return String.valueOf(Math.round(v));
        return d1(v);
    }

    /** Egy tizedes, magyarul: „5,2". */
    public static String d1(double v) {
        return String.format(LOCALE, "%.1f", v);
    }

    /**
     * Kiírt számok számjeggyé: „négy kör" → „4 kor", „ötször ötöt" → „5ször 5".
     *
     * A teremben és a konyhában is ritkán ír bárki számjegyet, a felismerők
     * viszont számot keresnek. A tárgyragos alak („ötöt", „tízet") külön
     * szerepel, mert pont az a leggyakoribb: „nyomtam háromszor tízet".
     *
     * Csak önálló szót cserélünk – a „hatizom" nem hat izom, a „hetes" nem
     * hét es. Kivétel a „-szor/-szer" toldalék, ami a számhoz tapad.
     *
     * @param s ékezet nélküli, kisbetűs szöveg (Foods.norm kimenete)
     */
    public static String digits(String s) {
        if (s == null) return "";
        String out = s;
        for (String[] w : NUM_WORDS) {
            int p = out.indexOf(w[0]);
            while (p >= 0) {
                int e = p + w[0].length();
                boolean glued = (p > 0 && Character.isLetter(out.charAt(p - 1)))
                        || (e < out.length() && Character.isLetter(out.charAt(e))
                            && !out.startsWith("szor", e) && !out.startsWith("szer", e));
                if (glued) {
                    p = out.indexOf(w[0], p + 1);
                } else {
                    out = out.substring(0, p) + w[1] + out.substring(e);
                    p = out.indexOf(w[0], p + w[1].length());
                }
            }
        }
        return out;
    }

    /**
     * A hosszabb alak elöl: különben a „tizenketto" tiz + enketto lenne.
     * A tárgyragos változat is szerepel, mert a felismerő azt is látja.
     */
    private static final String[][] NUM_WORDS = {
            {"tizenkettot", "12"}, {"tizenketto", "12"}, {"tizenket", "12"},
            {"tizenkilenc", "19"}, {"tizennyolcat", "18"}, {"tizennyolc", "18"},
            {"tizenhetet", "17"}, {"tizenhet", "17"}, {"tizenhatot", "16"},
            {"tizenhat", "16"}, {"tizenotot", "15"}, {"tizenot", "15"},
            {"tizennegyet", "14"}, {"tizennegy", "14"}, {"tizenharmat", "13"},
            {"tizenharom", "13"}, {"tizenegyet", "11"}, {"tizenegy", "11"},
            {"huszonotot", "25"}, {"huszonot", "25"}, {"huszat", "20"}, {"husz", "20"},
            // A tízesek negyventől: az intervall-időket („negyven másodperc
            // munka”) és a súlyokat („nyolcvan kiló”) ezekkel mondja az ember.
            {"harmincotot", "35"}, {"harmincot", "35"}, {"harmincat", "30"}, {"harminc", "30"},
            {"negyvenotot", "45"}, {"negyvenot", "45"}, {"negyvenet", "40"}, {"negyven", "40"},
            {"otvenotot", "55"}, {"otvenot", "55"}, {"otvenet", "50"}, {"otven", "50"},
            {"hatvanat", "60"}, {"hatvan", "60"}, {"hetvenet", "70"}, {"hetven", "70"},
            {"nyolcvanat", "80"}, {"nyolcvan", "80"},
            {"kilencvenet", "90"}, {"kilencven", "90"},
            {"szazat", "100"}, {"szaz", "100"},
            {"tizet", "10"}, {"tiz", "10"}, {"kilencet", "9"}, {"kilenc", "9"},
            {"nyolcat", "8"}, {"nyolc", "8"}, {"hetet", "7"}, {"het", "7"},
            {"hatot", "6"}, {"hat", "6"}, {"otot", "5"}, {"ot", "5"},
            {"negyet", "4"}, {"negy", "4"}, {"harmat", "3"}, {"harom", "3"},
            {"kettot", "2"}, {"ketto", "2"}, {"ket", "2"},
            {"masfel", "1,5"}, {"fel", "0,5"}, {"egyet", "1"}, {"egy", "1"},
    };

    /**
     * Változás az előző időszakhoz képest: „+12%", „−8%", „= 0%".
     *
     * Ha nem volt előzmény, nincs mihez viszonyítani: ilyenkor „új" a válasz,
     * nem „+100%" – az utóbbi azt sugallná, hogy a duplájára nőtt valami,
     * ami eddig nem is létezett.
     */
    public static String delta(double now, double prev) {
        if (prev <= 0) return now > 0 ? "új" : "—";
        if (now <= 0) return "−100%";
        double pct = (now - prev) / prev * 100;
        long r = Math.round(pct);
        if (r == 0) return "=";
        return (r > 0 ? "+" : "−") + Math.abs(r) + "%";
    }

    /** A hét napja magyarul, hétfőtől (0) vasárnapig (6). */
    public static String dayName(int dowIdx) {
        String[] n = {"Hétfő", "Kedd", "Szerda", "Csütörtök", "Péntek", "Szombat", "Vasárnap"};
        return dowIdx >= 0 && dowIdx <= 6 ? n[dowIdx] : "";
    }

    /** Két tizedes, magyarul: „5,23". */
    public static String d2(double v) {
        return String.format(LOCALE, "%.2f", v);
    }
}
