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

    // ---- Fogyási cél ----

    public static final double[] RATES = {0.25, 0.5, 0.75, 1.0}; // kg/hét: lassú, normál, gyors, extrém

    public static float getGoalLoss(Context c) { return prefs(c).getFloat("g_loss", 0f); }
    public static void setGoalLoss(Context c, float kg) { prefs(c).edit().putFloat("g_loss", kg).apply(); }
    public static int getGoalRate(Context c) { return prefs(c).getInt("g_rate", 1); }
    public static void setGoalRate(Context c, int i) { prefs(c).edit().putInt("g_rate", i).apply(); }

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
        SharedPreferences p = prefs(c);
        JSONArray arr = measurements(c);
        try {
            JSONObject o = new JSONObject();
            o.put("ts", ts);
            o.put("w", weight);
            o.put("bf", bodyFat);
            o.put("bmi", bmi);
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

    public static void clearMeasurements(Context c) {
        prefs(c).edit().remove(KEY).apply();
    }

    /** A legutóbb mentett testsúly, vagy -1. */
    public static double lastWeight(Context c) {
        JSONArray a = measurements(c);
        if (a.length() == 0) return -1;
        return a.optJSONObject(0) != null ? a.optJSONObject(0).optDouble("w", -1) : -1;
    }
    public static double lastBodyFat(Context c) {
        JSONArray a = measurements(c);
        if (a.length() == 0) return -1;
        return a.optJSONObject(0) != null ? a.optJSONObject(0).optDouble("bf", -1) : -1;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
