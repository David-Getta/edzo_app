package com.edzo.idozito;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A heti rehab-fókusz tárolása: melyik testtájat tűzted ki, és mikor
 * végezted el a sorát.
 *
 * A megelőzés gyenge pontja nem a tudás, hanem a rendszeresség: egy boka-sor
 * önmagában semmit nem ér, heti háromszor viszont tényleg véd. A fókusz ezt
 * teszi láthatóvá – egy kitűzött terület, és egy számláló, ami hétfőnként
 * nullázódik. A számolás tiszta logikája a Rehab-ban él (ott teszt is fut
 * rajta), itt csak a SharedPreferences-réteg van.
 */
final class RehabLog {

    private RehabLog() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("edzo", Context.MODE_PRIVATE);
    }

    /** A kitűzött testtáj azonosítója, vagy null. */
    static String focusId(Context c) {
        return p(c).getString("rehab_focus", null);
    }

    /** Fókusz kitűzése vagy levétele (null = nincs fókusz). */
    static void setFocus(Context c, String id) {
        SharedPreferences.Editor e = p(c).edit();
        if (id == null) e.remove("rehab_focus");
        else e.putString("rehab_focus", id);
        e.apply();
    }

    /** Egy elvégzett alkalom feljegyzése az adott testtájra. */
    static void addDone(Context c, String id, long ts) {
        long[] old = doneOf(c, id);
        StringBuilder sb = new StringBuilder();
        sb.append(ts);
        // Bő két hónapnyi alkalom elég – a számláló csak az e hetit nézi.
        int keep = Math.min(old.length, 59);
        for (int i = 0; i < keep; i++) sb.append(',').append(old[i]);
        p(c).edit().putString("rehab_done_" + id, sb.toString()).apply();
    }

    /** Az elvégzett alkalmak időbélyegei, legfrissebb elöl. */
    static long[] doneOf(Context c, String id) {
        String s = p(c).getString("rehab_done_" + id, "");
        if (s.isEmpty()) return new long[0];
        String[] parts = s.split(",");
        long[] out = new long[parts.length];
        int n = 0;
        for (String part : parts) {
            try {
                out[n++] = Long.parseLong(part.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return n == out.length ? out : java.util.Arrays.copyOf(out, n);
    }

    // ---------- Fájdalom-napló ----------

    /**
     * Egy fájdalom-érték (0–10) feljegyzése az adott testtájra.
     *
     * Napi egy érték: aki kétszer is beírja, annak az újabb marad – a
     * panasz napi szinten mozog, óránként nem érdemes követni.
     */
    static void addPain(Context c, String id, long ts, int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(ts).append(':').append(level);
        long day = Days.index(ts);
        int[] lv = painLevels(c, id);
        long[] ts2 = painTimes(c, id);
        int kept = 0;
        for (int i = 0; i < lv.length && kept < 59; i++) {
            if (Days.index(ts2[i]) == day) continue;
            sb.append(',').append(ts2[i]).append(':').append(lv[i]);
            kept++;
        }
        p(c).edit().putString("rehab_pain_" + id, sb.toString()).apply();
    }

    /** A fájdalom-értékek, legfrissebb elöl. */
    static int[] painLevels(Context c, String id) {
        String[] parts = painParts(c, id);
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split(":");
            try {
                out[i] = kv.length > 1 ? Integer.parseInt(kv[1].trim()) : -1;
            } catch (NumberFormatException e) {
                out[i] = -1;
            }
        }
        return out;
    }

    /** A fájdalom-bejegyzések időbélyegei, ugyanabban a sorrendben. */
    static long[] painTimes(Context c, String id) {
        String[] parts = painParts(c, id);
        long[] out = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split(":");
            try {
                out[i] = Long.parseLong(kv[0].trim());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    /** Van-e MA fájdalom-bejegyzés erre a testtájra? */
    static boolean painLoggedToday(Context c, String id) {
        long[] ts = painTimes(c, id);
        return ts.length > 0 && Days.index(ts[0]) == Days.index(System.currentTimeMillis());
    }

    private static String[] painParts(Context c, String id) {
        String s = p(c).getString("rehab_pain_" + id, "");
        return s.isEmpty() ? new String[0] : s.split(",");
    }
}
