package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Testreszabható megjelenés és viselkedés. Minden érték a SharedPreferences-ből
 * jön, ésszerű alapértékekkel, így a felhasználó a Beállításokban mindent állíthat.
 */
public final class Theme {

    private Theme() {}

    static final String PREFS = "edzo";

    // Választható színek (paletta) – „Grit" karmazsin arculat.
    public static final int[] SWATCHES = {
            0xFFE11D2E, // karmazsin
            0xFFFF4757, // skarlát
            0xFFFF6B3D, // parázs
            0xFFFFC24D, // arany
            0xFF14B8A6, // teal
            0xFF3B82F6, // kék
            0xFFA855F7, // lila
            0xFF22E0FF, // cián
    };

    static final int DEF_ACCENT = 0xFFE11D2E;   // karmazsin
    static final int DEF_ACCENT2 = 0xFFFF4757;  // skarlát (a márka-gradiens másik vége)
    static final int DEF_WORK = 0xFFE11D2E;      // futás = karmazsin
    static final int DEF_REST = 0xFF14B8A6;      // pihenő = teal (jól elkülönül a futástól)

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int accent(Context c) { return p(c).getInt("c_accent", DEF_ACCENT); }
    /** A márka-gradiens másik vége (alapból magenta), így a cián→magenta átmenet mindenütt egységes. */
    public static int accent2(Context c) { return p(c).getInt("c_accent2", DEF_ACCENT2); }
    public static int work(Context c) { return p(c).getInt("c_work", DEF_WORK); }
    public static int rest(Context c) { return p(c).getInt("c_rest", DEF_REST); }

    public static float volume(Context c) { return p(c).getFloat("volume", 0.8f); }
    public static boolean vibrate(Context c) { return p(c).getBoolean("vibrate", true); }
    /** 0 = nincs visszaszámláló csipogás. */
    public static int countdownSecs(Context c) { return p(c).getInt("cd_secs", 3); }
    /** false = km/h, true = perc/km (tempó). */
    public static boolean paceMode(Context c) { return p(c).getBoolean("pace", false); }
    /** Heti visszatekintő értesítés be/ki. */
    public static boolean recapEnabled(Context c) { return p(c).getBoolean("recap", true); }
    /** Blaze (kabalafigura) napi motiváló értesítése be/ki. */
    public static boolean blazeNudge(Context c) { return p(c).getBoolean("blaze_nudge", true); }
    /** Blaze napi értesítésének órája (0-23). */
    public static int nudgeHour(Context c) { return p(c).getInt("blaze_hour", 18); }
    /** Hangbemondás (TTS) sebessége: 0.85 = lassú, 0.96 = normál, 1.15 = gyors. */
    public static float speechRate(Context c) { return p(c).getFloat("tts_rate", 0.96f); }

    /** Tervezett edzésnapok CSV-ben (0=hétfő .. 6=vasárnap); üres = minden nap. */
    public static String planDays(Context c) { return p(c).getString("plan_days", ""); }
    /** Beszámítsuk-e az edzéssel elégetett kalóriát a napi célba? */
    public static boolean kcalCredit(Context c) { return p(c).getBoolean("kcal_credit", false); }

    /** Heti fókusz naponként, CSV-ben (0=hétfő .. 6=vasárnap); üres = nincs terv. */
    public static String planFocus(Context c) { return p(c).getString("plan_focus", ""); }

    public static void setPlanFocus(Context c, String[] focus) {
        p(c).edit().putString("plan_focus", Weekplan.format(focus)).apply();
    }

    /** Edzésnap-e az adott nap (0=hétfő .. 6=vasárnap)? Üres terv = minden nap az. */
    public static boolean isPlanDay(Context c, int dowIdx) {
        if (dowIdx < 0 || dowIdx > 6) return false;
        return planFlags(c)[dowIdx];
    }

    // A széria- és terv-számítások naponként hívják ezt (a planWeeks egyetlen
    // futása 728-szor), ezért a vesszős listát nem bontjuk szét újra és újra:
    // a nyers szövegre kötve eltesszük a hét hét napjának kész igen/nem tábláját.
    private static String planRaw;
    private static boolean[] planCache;

    private static boolean[] planFlags(Context c) {
        String s = planDays(c);
        boolean[] cached = planCache;
        if (cached != null && s.equals(planRaw)) return cached;
        boolean[] f = new boolean[7];
        if (s.isEmpty()) {
            java.util.Arrays.fill(f, true);   // üres terv = minden nap edzésnap
        } else {
            for (String d : s.split(",")) {
                try {
                    int i = Integer.parseInt(d.trim());
                    if (i >= 0 && i <= 6) f[i] = true;
                } catch (NumberFormatException ignored) {}
            }
        }
        planRaw = s;
        planCache = f;
        return f;
    }
    public static String getStr(Context c, String key, String def) { return p(c).getString(key, def); }
    public static void setStr(Context c, String key, String v) { p(c).edit().putString(key, v).apply(); bumpRev(c); }
    /** Élő (mozgó) háttér-animáció be/ki. */
    public static boolean liveBg(Context c) { return p(c).getBoolean("livebg", true); }
    /** Zene halkítása (audio-fókusz) edzés közben be/ki. */
    public static boolean duckMusic(Context c) { return p(c).getBoolean("duck", true); }
    /** Képernyő ébren tartása edzés közben (amíg a futás-képernyőt nézed). */
    public static boolean keepScreenOn(Context c) { return p(c).getBoolean("screenon", true); }
    /** Díszítő animációk (gomb-lüktetés, konfetti, sáv-feltöltődés) be/ki. */
    public static boolean animEnabled(Context c) { return p(c).getBoolean("anim", true); }
    /** Világos mód (true) vagy a klasszikus sötét „cyber" mód (false, alapértelmezett). */
    public static boolean light(Context c) { return p(c).getBoolean("lightmode", false); }

    /**
     * Haladássávok üres részének színe. Sötét módban áttetsző fehér (ugyanaz,
     * mint eddig), világos módban áttetsző fekete – különben a fehér csík a
     * világos kártyán láthatatlan lenne.
     */
    public static int track(Context c) { return light(c) ? 0x1A000000 : 0x22FFFFFF; }

    /** Halványabb változat (pl. „nincs adat" jelzésére). */
    public static int trackFaint(Context c) { return light(c) ? 0x12000000 : 0x14FFFFFF; }

    /**
     * Finom fátyol a háttérkép fölé, kártyák kitöltéséhez.
     *
     * Sötét módban ez világosít (áttetsző fehér), világosban sötétít – ugyanaz
     * a 8%-os erősség, ellenkező irányban. Fix fehérrel a világos módban a
     * kártya gyakorlatilag eltűnt a világos háttéren: fehér a fehéren.
     */
    public static int veil(Context c) { return light(c) ? 0x14000000 : 0x14FFFFFF; }

    /** Erősebb fátyol (kiemelt kártyákhoz, naptár-cellákhoz). */
    public static int veilStrong(Context c) { return light(c) ? 0x1A000000 : 0x1AFFFFFF; }

    /** Leghalványabb fátyol (pl. jövőbeli, még üres naptár-nap). */
    public static int veilFaint(Context c) { return light(c) ? 0x0D000000 : 0x11FFFFFF; }

    /** Minden UI-t érintő változásnál nő; a MainActivity ez alapján épül újra. */
    public static int rev(Context c) { return p(c).getInt("theme_rev", 0); }
    public static void bumpRev(Context c) { p(c).edit().putInt("theme_rev", rev(c) + 1).apply(); }

    public static void setInt(Context c, String key, int v) { p(c).edit().putInt(key, v).apply(); bumpRev(c); }
    public static void setBool(Context c, String key, boolean v) { p(c).edit().putBoolean(key, v).apply(); bumpRev(c); }
    public static void setFloat(Context c, String key, float v) { p(c).edit().putFloat(key, v).apply(); bumpRev(c); }

    public static void resetAll(Context c) {
        p(c).edit()
                .remove("c_accent").remove("c_accent2").remove("c_work").remove("c_rest")
                .remove("volume").remove("vibrate").remove("cd_secs").remove("pace").remove("recap").remove("livebg").remove("duck").remove("screenon")
                .remove("anim").remove("lightmode").remove("blaze_nudge").remove("blaze_hour")
                .remove("plan_days").remove("plan_focus").remove("kcal_credit")
                .remove("tts_rate")
                .apply();
        bumpRev(c);
    }

    /** Világosabb árnyalat (gradiens-párhoz). */
    public static int lighten(int col, float f) {
        int a = (col >>> 24) & 0xff, r = (col >> 16) & 0xff, g = (col >> 8) & 0xff, b = col & 0xff;
        r = (int) (r + (255 - r) * f);
        g = (int) (g + (255 - g) * f);
        b = (int) (b + (255 - b) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
