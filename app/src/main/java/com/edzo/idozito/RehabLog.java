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
}
