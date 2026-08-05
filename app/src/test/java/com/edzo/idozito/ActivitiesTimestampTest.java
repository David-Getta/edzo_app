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

    @Test public void aSpokenTimeOfDayBeatsTheNoonDefault() {
        // A „tegnap este kondi" nem délben volt – a felhasználó megmondta.
        long now = lateEvening();
        long[] ts = Activities.timestamps(Activities.parse("tegnap este kondi"), now);
        assertEquals(19, hourOf(ts[0]));
        assertEquals(dayOf(now) - 24L * 3600 * 1000, dayOf(ts[0]));
        assertEquals(8, hourOf(Activities.timestamps(
                Activities.parse("tegnap reggel futottam"), now)[0]));
        assertEquals(16, hourOf(Activities.timestamps(
                Activities.parse("tegnapelőtt délután úszás"), now)[0]));
        // Napszak nélkül marad a semleges dél.
        assertEquals(12, Activities.parse("tegnap kondi").hour);
        assertEquals(19, Activities.parse("tegnap este kondi").hour);
        // A MAI bejegyzés a mostani pillanatot kapja akkor is, ha van napszak.
        long[] today = Activities.timestamps(Activities.parse("ma este 1 jóga"), now);
        assertTrue(now - today[0] < 30L * 60 * 1000);
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

    @Test public void theSpokenClockTimeBeatsThePartOfDay() {
        // A „reggel 6-kor" hatot jelent, nem a reggel általános nyolcát.
        assertEquals(6, Activities.parse("ma reggel 6-kor futottam").hour);
        assertEquals(18, Activities.parse("tegnap 18-kor kondi").hour);
        assertEquals(23, Activities.parse("23-kor futottam").hour);
        assertEquals(6, Activities.parse("ma 6 kor futottam").hour);
        // Este nincs nyolc óra: a délutáni napszak átteszi a 12 alattit.
        assertEquals(20, Activities.parse("este 8-kor edzettem").hour);
        assertEquals(17, Activities.parse("délután 5-kor futás").hour);
        // Napszak óraszám nélkül marad a régi.
        assertEquals(19, Activities.parse("tegnap este kondi").hour);
        assertEquals(12, Activities.parse("8 korsó sört ittam és futottam").hour);
    }

    @Test public void anHourOfTheClockIsNotADuration() {
        // A „7 órakor" időpont – korábban hét óra hosszú úszás lett belőle.
        assertEquals(7, Activities.parse("reggel 7 órakor úszás").hour);
        assertEquals(45, Activities.parse("reggel 7 órakor úszás").plans.get(0).minutes);
        // A valódi időtartam nem sérül.
        assertEquals(120, Activities.parse("2 óra úszás").plans.get(0).minutes);
    }

    @Test public void lastWeekdayGoesBackAWholeWeek() {
        // A „múlt kedden" egy héttel korábbi keddet jelent, nem a mostanit –
        // különben a bejegyzés hét nappal a helye elé kerül a naplóban.
        assertEquals(8, Activities.parse("múlt kedden futottam", WED_NOON).offset);
        assertEquals(1, Activities.parse("kedden futottam", WED_NOON).offset);
        assertEquals(9, Activities.parse("múlt hétfőn kondi", WED_NOON).offset);
        assertEquals(11, Activities.parse("múlt szombaton túra", WED_NOON).offset);
    }

    @Test public void dawnIsItsOwnPartOfTheDay() {
        assertEquals(5, Activities.parse("tegnap hajnalban futottam", WED_NOON).hour);
        assertEquals(5, Activities.parse("múlt pénteken hajnalban futás", WED_NOON).hour);
        // A többi napszak érintetlen.
        assertEquals(8, Activities.parse("tegnap reggel futottam", WED_NOON).hour);
        assertEquals(19, Activities.parse("tegnap este kondi", WED_NOON).hour);
    }

    /** 2026. augusztus 5., szerda dél. */
    private static final long WED_NOON = wedNoon();

    private static long wedNoon() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(2026, java.util.Calendar.AUGUST, 5, 12, 0, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
