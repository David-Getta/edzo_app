package com.edzo.idozito;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Szint- és XP-rendszer: minden edzés XP-t ad (idő + táv + alap), a szint
 * ebből számolódik. A küszöbök egyre nőnek, így a magasabb szintek ritkábbak.
 */
public final class Levels {

    private Levels() {}

    static final String[] TITLES = {
            "Kezdő", "Amatőr", "Lelkes", "Haladó", "Sportos",
            "Kitartó", "Atléta", "Profi", "Bajnok", "Legenda"
    };

    /** Egy edzés XP-je: percek + km*12 + 8 alap. */
    public static long xpForSession(int durSec, double distM) {
        long xp = Math.round(durSec / 60.0);
        if (distM > 0) xp += Math.round(distM / 1000.0 * 12);
        return xp + 8;
    }

    /** Teljes XP: egyesített napló + bónuszok (pl. teljesített napi kihívások). */
    public static long totalXp(android.content.Context c) {
        long bonus = c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE)
                .getLong("bonus_xp", 0);
        return totalXp(History.loadAll(c)) + bonus;
    }

    /** Bónusz-XP jóváírása (pl. kihívás-teljesítéskor). */
    public static void addBonus(android.content.Context c, long xp) {
        android.content.SharedPreferences p =
                c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE);
        p.edit().putLong("bonus_xp", p.getLong("bonus_xp", 0) + xp).apply();
    }

    /** Összes XP a naplóból. */
    public static long totalXp(JSONArray history) {
        long xp = 0;
        for (int i = 0; i < history.length(); i++) {
            JSONObject o = history.optJSONObject(i);
            if (o == null) continue;
            xp += xpForSession(o.optInt("dur"), o.optDouble("dist", -1));
        }
        return xp;
    }

    /** Az XP-hez tartozó szint (1-től). */
    public static int levelForXp(long xp) {
        if (xp < 0) xp = 0;
        int lvl = (int) Math.floor(Math.sqrt(xp / 50.0)) + 1;
        return Math.max(1, lvl);
    }

    /** Az adott szint eléréséhez szükséges összes XP. */
    public static long xpForLevel(int level) {
        long l = level - 1;
        return 50L * l * l;
    }

    public static String title(int level) {
        int idx = Math.min(TITLES.length - 1, Math.max(0, level - 1));
        return TITLES[idx];
    }

    /** 0..1 haladás az aktuális szinten belül a következőig. */
    public static float progress(long xp) {
        int lvl = levelForXp(xp);
        long base = xpForLevel(lvl), next = xpForLevel(lvl + 1);
        if (next <= base) return 1f;
        return Math.max(0f, Math.min(1f, (float) (xp - base) / (float) (next - base)));
    }

    public static long xpToNext(long xp) {
        int lvl = levelForXp(xp);
        return Math.max(0, xpForLevel(lvl + 1) - xp);
    }
}
