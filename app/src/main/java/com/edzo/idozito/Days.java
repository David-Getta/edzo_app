package com.edzo.idozito;

import java.util.Calendar;

/**
 * Naptári nap-számítás, óraátállás-biztosan.
 *
 * A csábító megoldás – két időbélyeg különbsége osztva 24 órával – évente
 * kétszer téved: az óraátállás napja 23, illetve 25 órás, így az egész napra
 * osztás lefelé csonkol, és a diagramok egy nappal elcsúsznak. A szériák és a
 * jelvények ezért eleve naptári léptetéssel dolgoznak; ez az osztály ugyanezt
 * adja meg azoknak a helyeknek, ahol indexre van szükség (heti/havi csíkok).
 */
public final class Days {

    private Days() {}

    private static final double DAY_MS = 24 * 3600 * 1000.0;

    /** Az időbélyeg helyi napjának kezdete (éjfél). */
    public static long startOf(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /**
     * Hány naptári nap telt el `from` napjától `to` napjáig. Azonos napon 0,
     * a jövőbe mutató irányban negatív.
     *
     * A kerekítés nyeli el az óraátállást: a 23 órás nap 0,958, a 25 órás
     * 1,042 – mindkettő 1 napra kerekedik, míg a csonkolás az elsőt 0-ra vinné.
     */
    public static int between(long fromTs, long toTs) {
        return (int) Math.round((startOf(toTs) - startOf(fromTs)) / DAY_MS);
    }

    /** Hány napja volt: ma 0, tegnap 1. Jövőbeli időpontra negatív. */
    public static int ago(long ts, long now) {
        return between(ts, now);
    }

    /**
     * Naponta eggyel növő sorszám – naponta forgó válogatásokhoz (példamondatok,
     * étel-ötletek).
     *
     * A kézenfekvő {@code ts / 86400000} UTC szerint fordul: nyáron
     * hajnali kettőkor cserélődne a „mai" válogatás, télen egykor. Nem hiba,
     * csak észrevehető – aki éjjel fél egykor nézi meg, holnapi ötletet kap.
     */
    public static long index(long ts) {
        return Math.floorDiv(startOf(ts), 86_400_000L);
    }
}
