package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;

/**
 * A kézzel felvett edzések időbélyegei.
 *
 * Az időbélyeg nem csak sorrend: a jelvények órát olvasnak belőle (Korán kelő,
 * Éjjeli bagoly), a széria napot, a lista pedig kiírja. A rögzítés pillanata
 * a múltbeli napokra hazugság lenne – a tegnapelőtti kézilabda nem éjjel
 * 11-kor volt, csak akkor lett beírva.
 */
public class ActivitiesTimestampTest {

    private TimeZone original;

    @Before public void useBudapestTime() {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Budapest"));
    }

    @After public void restoreTimeZone() {
        TimeZone.setDefault(original);
    }

    /** 2026. július 30., este 23:11 – ilyenkor szokás naplót pótolni. */
    private static long lateEvening() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.JULY, 30, 23, 11, 0);
        return c.getTimeInMillis();
    }

    private static int hourOf(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    private static long dayOf(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Test public void sixHandballsOverThreeDaysMeansTwoPerDay() {
        long now = lateEvening();
        Activities.Parsed p = Activities.parse("az elmúlt 3 nap alatt 6 kézi edzés");
        long[] ts = Activities.timestamps(p, now);
        assertEquals(6, ts.length);
        java.util.HashMap<Long, Integer> perDay = new java.util.HashMap<>();
        for (long t : ts) perDay.merge(dayOf(t), 1, Integer::sum);
        assertEquals("nem három napra oszlott el", 3, perDay.size());
        for (int n : perDay.values()) assertEquals("nem naponta kettő", 2, n);
    }

    @Test public void pastDaysGetNoonNotTheLoggingHour() {
        // Este 23:11-kor pótolt bejegyzések: a múltbeli napok NEM örökölhetik
        // az esti órát, különben az Éjjeli bagoly jelvény hazudna.
        long now = lateEvening();
        long[] ts = Activities.timestamps(
                Activities.parse("az elmúlt 3 nap alatt 6 kézi edzés"), now);
        for (long t : ts) {
            if (dayOf(t) == dayOf(now)) continue;      // a mai maradhat esti
            assertTrue("múltbeli nap nem délidőt kapott: " + hourOf(t) + " óra",
                    hourOf(t) >= 11 && hourOf(t) <= 12);
        }
    }

    @Test public void todayKeepsTheCurrentMoment() {
        long now = lateEvening();
        long[] ts = Activities.timestamps(Activities.parse("2 kondi"), now);
        assertEquals(2, ts.length);
        for (long t : ts) {
            assertEquals("nem a mai napra került", dayOf(now), dayOf(t));
            assertTrue("a jövőbe került", t <= now);
            assertTrue("túl messze csúszott a mostani pillanattól",
                    now - t < 30L * 60 * 1000);
        }
    }

    @Test public void yesterdayEntriesLandOnYesterdayAtNoon() {
        long now = lateEvening();
        long[] ts = Activities.timestamps(Activities.parse("tegnap 1 kondi"), now);
        assertEquals(1, ts.length);
        assertEquals(dayOf(now) - 24L * 3600 * 1000, dayOf(ts[0]));
        assertEquals(12, hourOf(ts[0]));
    }

    @Test public void everyEntryGetsADistinctTimestamp() {
        // Az időbélyeg azonosítja a bejegyzést (megnyitás, törlés): két azonos
        // időbélyegű edzést nem lehetne szétválasztani.
        long now = lateEvening();
        long[] ts = Activities.timestamps(
                Activities.parse("az elmúlt 5 napban 10 futás és 10 kondi"), now);
        assertEquals(20, ts.length);
        HashSet<Long> uniq = new HashSet<>();
        for (long t : ts) assertTrue("ismétlődő időbélyeg", uniq.add(t));
    }

    @Test public void nothingLandsInTheFuture() {
        long now = lateEvening();
        for (String q : new String[]{"3 futás", "tegnap 2 kondi",
                "az elmúlt héten 5 úszás", "ma 1 jóga"})
            for (long t : Activities.timestamps(Activities.parse(q), now))
                assertTrue("jövőbeli időbélyeg: " + q, t <= now);
    }
}
