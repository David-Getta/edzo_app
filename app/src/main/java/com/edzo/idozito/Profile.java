package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Testadatok és mérés-előzmények tárolása. A magasság és a születési dátum
 * ritkán változó "profil" adat; a testsúly/testzsír/BMI időbélyeggel mentett
 * mérésekként kerül a naplóba, hogy a változást diagramon lehessen mutatni.
 */
public final class Profile {

    private Profile() {}

    static final String PREFS = "edzo";
    static final String KEY = "measurements";
    static final int MAX = 300;

    // ---- Profil (magasság, születési dátum) ----

    public static int getHeight(Context c) {
        return prefs(c).getInt("p_height", 0);
    }
    public static void setHeight(Context c, int cm) {
        prefs(c).edit().putInt("p_height", cm).apply();
    }

    public static int getBirthY(Context c) { return prefs(c).getInt("p_by", 0); }
    public static int getBirthM(Context c) { return prefs(c).getInt("p_bm", 0); }
    public static int getBirthD(Context c) { return prefs(c).getInt("p_bd", 0); }
    public static void setBirth(Context c, int y, int m, int d) {
        prefs(c).edit().putInt("p_by", y).putInt("p_bm", m).putInt("p_bd", d).apply();
    }

    /** Kor években a születési dátumból, vagy -1, ha nincs megadva. */
    public static int ageYears(Context c) {
        int y = getBirthY(c), m = getBirthM(c), d = getBirthD(c);
        if (y < 1900 || m < 1 || m > 12 || d < 1 || d > 31) return -1;
        Calendar now = Calendar.getInstance();
        int age = now.get(Calendar.YEAR) - y;
        int mm = now.get(Calendar.MONTH) + 1;
        int dd = now.get(Calendar.DAY_OF_MONTH);
        if (mm < m || (mm == m && dd < d)) age--;
        return Math.max(0, age);
    }

    // ---- Nem ----

    public static int getSex(Context c) { return prefs(c).getInt("p_sex", 0); } // 0 = férfi, 1 = nő
    public static void setSex(Context c, int s) { prefs(c).edit().putInt("p_sex", s).apply(); }

    // ---- BMR (alap-anyagcsere, Mifflin–St Jeor) ----

    public static double bmr(int sex, double weightKg, int heightCm, int age) {
        if (weightKg <= 0 || heightCm <= 0 || age < 0) return -1;
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        return sex == 1 ? base - 161 : base + 5;
    }

    /**
     * A beszámított edzés-kalória felső határa napi 800 kcal: a becsült égetés
     * fölfelé téved a legkönnyebben, és egy elszámolt óra nem érhet meg egy
     * plusz vacsorát.
     */
    static final double MAX_CREDIT = 800;

    /**
     * A napi kalória-cél az edzéssel elégetett kalóriával megnövelve, ha a
     * felhasználó ezt kérte.
     *
     * Két iskola van, és mindkettőnek igaza van a maga módján. Aki fix célt
     * tart, annak az edzés a deficit része – neki ne mozogjon a cél. Aki
     * viszont sokat edz, annál a fix cél napokon át 800 kalóriás mínuszt
     * jelentene, és az nem fogyás, hanem éhezés. Ezért ez beállítás, nem
     * döntés helyette.
     */
    public static int effectiveGoal(int goal, double burned, boolean credit) {
        if (goal <= 0) return goal;
        if (!credit || burned <= 0) return goal;
        return goal + (int) Math.round(Math.min(MAX_CREDIT, burned));
    }

    /**
     * Aktivitási szorzó a napi szükséglethez (enyhén aktív becslés).
     *
     * Egy helyen, mert korábban három helyen szerepelt, KÉT különböző értékkel:
     * az Étrend képernyő 1,35-tel, a Profil 1,4-gyel számolt. Ugyanaz a
     * felhasználó tehát két különböző napi kalóriaszükségletet látott a két
     * képernyőn, és ami rosszabb: az Étrend ugyanabban a párbeszédablakban
     * kínálta a fenntartó (1,35) és a fogyós (1,4 − hiány) értéket, így a kettő
     * különbsége nem is a beállított kalóriahiány volt. Aki utánaszámolt, annak
     * nem jött ki.
     */
    public static final double ACTIVITY = 1.4;

    /** Napi kalóriaszükséglet a BMR-ből, vagy -1 ha a BMR sem számolható. */
    public static double tdee(double bmr) {
        return bmr > 0 ? bmr * ACTIVITY : -1;
    }

    // ---- Fogyási cél ----

    public static final double[] RATES = {0.25, 0.5, 0.75, 1.0}; // kg/hét: lassú, normál, gyors, extrém

    /** Napi kalóriahiány a választott tempóhoz (1 kg zsír ≈ 7700 kcal). */
    public static double dailyDeficit(int rateIdx) {
        return RATES[Math.max(0, Math.min(RATES.length - 1, rateIdx))] * 7700.0 / 7.0;
    }

    /** Fogyáshoz javasolt napi bevitel: a szükségletből levont hiány. */
    public static double intakeForLoss(double bmr, int rateIdx) {
        double t = tdee(bmr);
        return t < 0 ? -1 : t - dailyDeficit(rateIdx);
    }

    public static float getGoalLoss(Context c) { return prefs(c).getFloat("g_loss", 0f); }
    public static void setGoalLoss(Context c, float kg) { prefs(c).edit().putFloat("g_loss", kg).apply(); }
    public static int getGoalRate(Context c) { return prefs(c).getInt("g_rate", 1); }
    public static void setGoalRate(Context c, int i) { prefs(c).edit().putInt("g_rate", i).apply(); }

    /**
     * Testsúly-trend: hány kg/hét a változás a megadott mérésekből, lineáris
     * illesztéssel (a napi ingadozás – étel, víz, napszak – ±1 kg is lehet, két
     * pont különbsége ezért félrevezet; az egyenes az egészet látja).
     *
     * @param days  a mérések ideje NAPBAN (bármilyen közös nullponthoz képest)
     * @param kg    a mért testsúlyok, azonos sorrendben
     * @return kg/hét (negatív = fogyás), vagy 0, ha nincs elég adat
     */
    public static double weeklyTrend(double[] days, double[] kg) {
        if (days == null || kg == null || days.length < 2 || days.length != kg.length) return 0;
        double n = days.length, sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int i = 0; i < days.length; i++) {
            sx += days[i]; sy += kg[i];
            sxx += days[i] * days[i]; sxy += days[i] * kg[i];
        }
        double denom = n * sxx - sx * sx;
        if (Math.abs(denom) < 1e-9) return 0;          // minden mérés egy napon
        double slopePerDay = (n * sxy - sx * sy) / denom;
        double perWeek = slopePerDay * 7;
        // Életszerű korlát: heti 3 kg fölött már nem trend, hanem elgépelés.
        if (perWeek > 3) perWeek = 3;
        if (perWeek < -3) perWeek = -3;
        return perWeek;
    }

    /**
     * Hány HÉT múlva éri el a célt a mostani ütemmel? -1, ha a cél már megvan,
     * vagy ha az ütem nem visz felé (nulla vagy ellentétes irányú).
     */
    public static double weeksToGoal(double remainingKg, double weeklyTrend) {
        if (remainingKg <= 0.05) return -1;            // megvan
        if (weeklyTrend >= -0.02) return -1;           // nem fogy (vagy hízik)
        double w = remainingKg / -weeklyTrend;
        return w > 260 ? -1 : w;                       // öt évnél távolabbit nem ígérünk
    }

    // ---- BMI ----

    /** BMI a magasságból (cm) és testsúlyból (kg), vagy -1 ha hiányos. */
    public static double bmi(int heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) return -1;
        double h = heightCm / 100.0;
        return weightKg / (h * h);
    }

    public static String bmiCategory(double bmi) {
        if (bmi < 0) return "";
        if (bmi < 18.5) return "sovány";
        if (bmi < 25) return "normál";
        if (bmi < 30) return "túlsúly";
        if (bmi < 35) return "elhízás (I)";
        return "elhízás (II+)";
    }

    // ---- Mérések (idősor) ----

    /** Új mérés a napló elejére: testsúly (kg), testzsír (%), BMI. Bármelyik < 0 = nincs adat. */
    public static void addMeasurement(Context c, long ts, double weight, double bodyFat, double bmi) {
        addMeasurement(c, ts, weight, bodyFat, bmi, null);
    }

    /**
     * Ugyanaz, körfogatokkal – a BodyParse.PART_KEYS sorrendjében, centiben.
     *
     * A körfogat ugyanabba a bejegyzésbe kerül, mint a súly: aki reggel
     * mérlegre áll és mérőszalagot fog, az EGY mérést végez. Külön idősorként
     * a két adat sosem állna egymás mellett a listában.
     */
    public static void addMeasurement(Context c, long ts, double weight, double bodyFat,
                                      double bmi, double[] cm) {
        SharedPreferences p = prefs(c);
        JSONArray arr = measurements(c);
        try {
            JSONObject o = new JSONObject();
            o.put("ts", ts);
            o.put("w", weight);
            o.put("bf", bodyFat);
            o.put("bmi", bmi);
            if (cm != null)
                for (int i = 0; i < cm.length && i < BodyParse.PART_KEYS.length; i++)
                    if (cm[i] > 0) o.put(BodyParse.PART_KEYS[i], cm[i]);
            JSONArray out = new JSONArray();
            out.put(o);
            for (int i = 0; i < arr.length() && out.length() < MAX; i++) out.put(arr.get(i));
            p.edit().putString(KEY, out.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** Legfrissebb elöl. */
    public static JSONArray measurements(Context c) {
        String s = prefs(c).getString(KEY, "[]");
        try { return new JSONArray(s); } catch (Exception e) { return new JSONArray(); }
    }

    /**
     * Egyetlen mérés törlése időbélyeg szerint.
     *
     * Eddig csak az ÖSSZES mérést lehetett törölni: egy elgépelt 87 kiló miatt
     * az egész görbét fel kellett áldozni. A javításnak nem szabad ennyibe
     * kerülnie.
     */
    public static boolean removeMeasurement(Context c, long ts) {
        JSONArray a = measurements(c);
        JSONArray out = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            if (!removed && o.optLong("ts") == ts) { removed = true; continue; }
            out.put(o);
        }
        if (removed) prefs(c).edit().putString(KEY, out.toString()).apply();
        return removed;
    }

    public static void clearMeasurements(Context c) {
        prefs(c).edit().remove(KEY).apply();
    }

    /** A legutóbb mentett testsúly, vagy -1. */
    public static double lastWeight(Context c) { return lastOf(measurements(c), "w"); }

    /**
     * Hány napja volt az utolsó mérés, vagy -1, ha még egy sincs.
     *
     * A súlytrend annyit ér, amennyi adat van benne: két mérés között eltelt
     * hat hét után a „kg/hét" már nem tendencia, csak két pont.
     */
    public static int daysSinceMeasurement(Context c, long now) {
        JSONArray a = measurements(c);
        long last = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) last = Math.max(last, o.optLong("ts"));
        }
        return last <= 0 ? -1 : Days.ago(last, now);
    }

    /** Ennyi nap után érdemes újra mérni – ekkortól szólunk is érte. */
    public static final int MEASURE_REMIND_DAYS = 10;

    /**
     * Emlékeztető szöveg a mérés koráról, vagy üres, ha friss.
     *
     * Szándékosan nem sürget: a testsúly napi ingadozása nagyobb, mint a heti
     * változás, ezért a napi mérésnek nincs értelme – tíz nap viszont már
     * lyuk a görbén.
     */
    public static String measureNudge(int daysAgo) {
        if (daysAgo < 0) return "Még nincs mentett mérésed – az elsővel indul a görbe.";
        if (daysAgo < MEASURE_REMIND_DAYS) return "";
        if (daysAgo < 30) return "⚖️ " + daysAgo + " napja mérted magad utoljára.";
        return "⚖️ Több mint egy hónapja nem mérted magad – egy friss adat "
                + "helyre teszi a görbét.";
    }

    public static double lastBodyFat(Context c) { return lastOf(measurements(c), "bf"); }

    /** A legutóbb mentett körfogatok centiben (0 = nincs), PART_KEYS sorrendben. */
    public static double[] lastCm(Context c) {
        JSONArray a = measurements(c);
        double[] out = new double[BodyParse.PART_KEYS.length];
        for (int i = 0; i < out.length; i++) {
            double v = lastOf(a, BodyParse.PART_KEYS[i]);
            out[i] = v > 0 ? v : 0;
        }
        return out;
    }

    /**
     * Derék/magasság arány – az egyetlen körfogat, aminek önmagában is van
     * egészség-jelentése.
     *
     * A szakirodalom hüvelykujjszabálya egyszerű: a derékbőség legyen a
     * testmagasság felénél kisebb. Ezért nem BMI-t közlünk mellé (azt a
     * súlyból már látja), hanem ezt az egy arányt.
     */
    public static double waistToHeight(double waistCm, int heightCm) {
        if (waistCm <= 0 || heightCm <= 0) return -1;
        return waistCm / heightCm;
    }

    /** Egysoros értékelés a derék/magasság arányhoz, vagy üres. */
    public static String waistVerdict(double ratio) {
        if (ratio < 0) return "";
        if (ratio < 0.4) return "a szokásosnál karcsúbb";
        if (ratio < 0.5) return "egészséges sávban";
        if (ratio < 0.6) return "érdemes figyelni rá";
        return "kockázati sávban";
    }

    /**
     * A legfrissebb mérés, amiben egyáltalán VAN ilyen adat (a lista legújabb
     * elöl), vagy -1. Egy mérés menthető csak testsúllyal vagy csak testzsírral,
     * ezért nem elég a legelső bejegyzést nézni: egy „csak testzsír" mérés
     * különben eltüntetné a korábban rögzített testsúlyt – és vele a BMI-t, a
     * BMR-alapú kalóriacél-ajánlást és a testsúly szerinti vízcélt is.
     */
    private static double lastOf(JSONArray a, String key) {
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            double v = o.optDouble(key, -1);
            if (v > 0) return v;
        }
        return -1;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
