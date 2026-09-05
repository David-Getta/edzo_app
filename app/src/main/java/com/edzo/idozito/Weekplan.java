package com.edzo.idozito;

/**
 * Heti fókusz: melyik napon mit edzel („H: Láb, Sze: Hát, P: Mell”).
 *
 * A tervezett edzésnapok eddig csak azt mondták meg, MIKOR van edzés – azt
 * nem, hogy MIT. Aki osztott edzést csinál, annak ez a fontosabbik fele, és
 * eddig fejben (vagy egy papíron) tartotta.
 *
 * Tiszta Java (nincs Context), hogy egységteszttel lefedhető legyen: a tárolt
 * szöveg elrontása a heti tervet némítaná el.
 */
public final class Weekplan {

    private Weekplan() {
    }

    /** Hétfőtől vasárnapig. */
    public static final String[] DAY_ABBR = {"H", "K", "Sze", "Cs", "P", "Szo", "V"};

    /** Egy nap fókusza legfeljebb ennyi karakter – a kártyán egy sorba kell férnie. */
    static final int MAX_LEN = 24;

    /**
     * A tárolt CSV hét elemre bontva. A hiányzó vagy hibás érték üres.
     * Sosem ad null elemet: a hívók egyszerű szövegként kezelhetik.
     */
    public static String[] parse(String csv) {
        String[] out = new String[7];
        java.util.Arrays.fill(out, "");
        if (csv == null) return out;
        String[] parts = csv.split(",", -1);
        for (int i = 0; i < 7 && i < parts.length; i++) out[i] = clean(parts[i]);
        return out;
    }

    /** Hét elem CSV-vé. A vessző elválasztó, ezért a szövegből kikerül. */
    public static String format(String[] focus) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (i > 0) sb.append(',');
            sb.append(focus != null && i < focus.length ? clean(focus[i]) : "");
        }
        // A csupa üres terv üres szöveg: így a „van-e terv" kérdés egy
        // isEmpty()-vel eldönthető, és nem marad hat vessző a beállításokban.
        return sb.toString().replace(",", "").isEmpty() ? "" : sb.toString();
    }

    private static String clean(String s) {
        if (s == null) return "";
        String t = s.replace(',', ' ').replace('\n', ' ').trim();
        while (t.contains("  ")) t = t.replace("  ", " ");
        return t.length() > MAX_LEN ? t.substring(0, MAX_LEN).trim() : t;
    }

    /** Az adott nap fókusza (0 = hétfő), vagy üres. */
    public static String forDay(String csv, int dowIdx) {
        if (dowIdx < 0 || dowIdx > 6) return "";
        return parse(csv)[dowIdx];
    }

    /** Van-e egyáltalán beállított fókusz? */
    public static boolean any(String csv) {
        for (String s : parse(csv)) if (!s.isEmpty()) return true;
        return false;
    }

    /**
     * Egy soros összefoglaló a beállított napokról: „H: Láb · Sze: Hát · P: Mell”.
     * Üres terv esetén üres szöveg – nincs mit mondani.
     */
    public static String summary(String csv) {
        String[] f = parse(csv);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (f[i].isEmpty()) continue;
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(DAY_ABBR[i]).append(": ").append(f[i]);
        }
        return sb.toString();
    }

    /**
     * Teljesült-e a heti fókusz? Egy tervezett nap akkor számít teljesítettnek,
     * ha aznap tényleg azt edzette, amit odaírt – vagy ha a fókusz nem
     * izomcsoport („Kardió", „Pihenő"), akkor ha aznap volt bármilyen edzés.
     *
     * Szándékosan elnéző: aki lábnapon a láb mellé kart is csinált, az
     * teljesítette a tervet. A terv nem tiltólista.
     *
     * @param dayGroups  naponként az aznap edzett izomcsoportok, vesszővel
     *                   összefűzve (0 = hétfő); null vagy üres, ha nem volt
     * @param trainedDay volt-e aznap bármilyen edzés
     * @return {teljesült, tervezett} – tervezett 0, ha nincs fókusz
     */
    public static int[] adherence(String csv, String[] dayGroups, boolean[] trainedDay) {
        String[] f = parse(csv);
        int planned = 0, done = 0;
        for (int i = 0; i < 7; i++) {
            if (f[i].isEmpty()) continue;
            planned++;
            String g = Muscles.groupOf(f[i]);
            if (g == null) {
                if (trainedDay != null && i < trainedDay.length && trainedDay[i]) done++;
            } else if (dayGroups != null && i < dayGroups.length && dayGroups[i] != null
                    && dayGroups[i].contains(g)) {
                done++;
            }
        }
        return new int[]{done, planned};
    }

    /**
     * A mai sor a kezdőlapra, vagy üres. A holnapi fókuszt is megmutatjuk, ha
     * ma nincs: a „mire készülj" ugyanannyit ér, mint a „mi van ma”.
     */
    public static String todayLine(String csv, int dowIdx) {
        String today = forDay(csv, dowIdx);
        if (!today.isEmpty()) return "📋  Ma: " + today;
        String tomorrow = forDay(csv, (dowIdx + 1) % 7);
        if (!tomorrow.isEmpty()) return "📋  Holnap: " + tomorrow;
        return "";
    }
}
