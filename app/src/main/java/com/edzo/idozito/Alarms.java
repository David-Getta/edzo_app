package com.edzo.idozito;

import android.app.AlarmManager;
import android.app.PendingIntent;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Az ismétlődő értesítések időzítése.
 *
 * Korábban mind a három emlékeztető {@code setInexactRepeating}-gel ment, fix
 * 24 órás (a heti visszatekintőnél 7×24 órás) lépésközzel. Ezzel két baj van:
 *
 * 1. A fix lépésköz nem tud az óraátállításról. Egy 18:00-ra kért emlékeztető
 *    a tavaszi átállítás után 19:00-kor szólt, az őszi után 17:00-kor, és
 *    magától soha nem állt vissza – csak akkor, ha a felhasználó megnyitotta
 *    az appot (az indulás újraütemez). Aki csak az értesítésre hagyatkozik,
 *    annál fél évig rossz időpontban szólalt meg.
 * 2. Az ismétlődő riasztás Doze-ban (a telefon pihen az asztalon) a következő
 *    karbantartási ablakig csúszik, ami órákat is jelenthet. Az esti „ideje
 *    edzeni” így éjjel érkezett, vagy csak akkor, amikor a felhasználó
 *    legközelebb kézbe vette a telefont.
 *
 * Helyette mindig CSAK a következő alkalomra ütemezünk, {@code
 * setAndAllowWhileIdle}-lel (ez Doze-ban is átmegy), és a vevő lefutáskor
 * kiszámolja a rákövetkezőt. Így az időpont mindig a fali órához igazodik.
 */
public final class Alarms {

    private Alarms() {}

    /** Meddig keressük a következő alkalmat, mielőtt feladjuk (végtelen ciklus ellen). */
    private static final int MAX_STEPS = 8;

    /**
     * A következő {@code h:m} időpont a megadott zóna fali órája szerint.
     *
     * A tavaszi átállítás éjszakáján egy-egy óra nem létezik (Budapesten
     * 02:00–03:00). Az ilyen napot átlépjük: jobb egyszer kihagyni az
     * emlékeztetőt, mint két furcsa időpontban (01:30 és 03:30) felverni
     * a felhasználót.
     */
    public static long nextDaily(int h, int m, long now, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        for (int i = 0; i < MAX_STEPS; i++) {
            cal.setTimeInMillis(now);
            cal.add(Calendar.DAY_OF_MONTH, i);
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long t = cal.getTimeInMillis();
            // A get() már a rendszer által feloldott értéket adja vissza: ha a kért
            // fali idő aznap nem létezett, itt más órát/percet kapunk vissza.
            if (t > now && cal.get(Calendar.HOUR_OF_DAY) == h && cal.get(Calendar.MINUTE) == m)
                return t;
        }
        return now + 24L * 60 * 60 * 1000;
    }

    /**
     * A következő adott hétköznap {@code h:m} ({@code dow} = {@link Calendar#SUNDAY} stb.).
     *
     * Szándékosan nem {@code Calendar.set(DAY_OF_WEEK, …)}-kel dolgozik: annak az
     * eredménye a naptár {@code firstDayOfWeek} beállításától, azaz a telefon
     * nyelvi beállításától függ.
     */
    public static long nextWeekly(int dow, int h, int m, long now, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        for (int i = 0; i <= MAX_STEPS; i++) {
            cal.setTimeInMillis(now);
            cal.add(Calendar.DAY_OF_MONTH, i);
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long t = cal.getTimeInMillis();
            if (t > now && cal.get(Calendar.DAY_OF_WEEK) == dow
                    && cal.get(Calendar.HOUR_OF_DAY) == h && cal.get(Calendar.MINUTE) == m)
                return t;
        }
        return now + 7L * 24 * 60 * 60 * 1000;
    }

    /**
     * A következő adott hónapnap {@code h:m} – a havi visszatekintőhöz (minden
     * hónap 1-je). Ugyanaz a lépegetős, óraátállás-biztos módszer, mint a
     * többinél: a jelölt csak akkor jó, ha a feloldott naptár tényleg a kért
     * napot és időt adja vissza.
     */
    public static long nextMonthly(int dayOfMonth, int h, int m, long now, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        for (int i = 0; i <= 62; i++) {
            cal.setTimeInMillis(now);
            cal.add(Calendar.DAY_OF_MONTH, i);
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long t = cal.getTimeInMillis();
            if (t > now && cal.get(Calendar.DAY_OF_MONTH) == dayOfMonth
                    && cal.get(Calendar.HOUR_OF_DAY) == h && cal.get(Calendar.MINUTE) == m)
                return t;
        }
        return now + 31L * 24 * 60 * 60 * 1000;
    }

    public static long nextMonthly(int dayOfMonth, int h, int m) {
        return nextMonthly(dayOfMonth, h, m, System.currentTimeMillis(), TimeZone.getDefault());
    }

    public static long nextDaily(int h, int m) {
        return nextDaily(h, m, System.currentTimeMillis(), TimeZone.getDefault());
    }

    public static long nextWeekly(int dow, int h, int m) {
        return nextWeekly(dow, h, m, System.currentTimeMillis(), TimeZone.getDefault());
    }

    /**
     * Egyszeri riasztás, ami Doze-ban is megszólal. Nem „exact” riasztás – arra
     * Android 12-től külön engedély kellene –, de a rendszer legfeljebb néhány
     * percet késik vele, nem órákat.
     */
    public static void oneShot(AlarmManager am, long at, PendingIntent pi) {
        if (am == null || pi == null) return;
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (Exception e) {
            try { am.set(AlarmManager.RTC_WAKEUP, at, pi); } catch (Exception ignored) {}
        }
    }
}
