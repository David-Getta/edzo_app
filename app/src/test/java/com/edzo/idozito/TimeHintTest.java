package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/**
 * Az időpont a mondatból.
 *
 * Egy rossz nap két helyen is elrontja a napi összesítőt: ahonnan elveszi, és
 * ahová beteszi. Ezért inkább maradjon a mostani pillanat, ha a mondat nem
 * mond semmit az időről.
 */
public class TimeHintTest {

    /** Szerda dél – innen minden hétköznapnév egyértelmű. */
    private static long wednesdayNoon() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.AUGUST, 5, 12, 0, 0);   // 2026. augusztus 5. szerda
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static int[] dayHour(String text) {
        long ts = TimeHint.from(text, wednesdayNoon());
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return new int[]{c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY)};
    }

    @Test public void yesterdayAndTheDayBeforeStillWork() {
        assertEquals(4, dayHour("tegnap este pizzát ettem")[0]);
        assertEquals(19, dayHour("tegnap este pizzát ettem")[1]);
        assertEquals(3, dayHour("tegnapelőtt ebédre gulyás")[0]);
        assertEquals(13, dayHour("tegnapelőtt ebédre gulyás")[1]);
    }

    @Test public void daysAgoIsUnderstood() {
        assertEquals(2, dayHour("3 napja ettem egy pizzát")[0]);
        assertEquals(12, dayHour("3 napja ettem egy pizzát")[1]);   // napszak nélkül dél
        assertEquals(1, dayHour("4 nappal ezelőtt reggel zabkása")[0]);
        assertEquals(8, dayHour("4 nappal ezelőtt reggel zabkása")[1]);
        // Két hétnél régebbre nem teszünk vissza semmit.
        assertEquals(0, TimeHint.daysBack("30 napja", wednesdayNoon()));
    }

    @Test public void weekdayNamesPointToTheMostRecentSuchDay() {
        // Szerdán a „hétfőn" két nappal ezelőtt volt.
        assertEquals(3, dayHour("hétfőn ettem lángost")[0]);
        // A „vasárnap" a legutóbbi vasárnap: három napja.
        assertEquals(2, dayHour("vasárnap sütöttünk")[0]);
        // A mai napnév nem visszalépés: aki szerdán ír „szerdán"-t, ma gondol.
        assertEquals(5, dayHour("szerdán ebédre leves")[0]);
    }

    @Test public void withoutATimeHintTheMomentIsKept() {
        long now = wednesdayNoon() + 37 * 60 * 1000 + 12;   // 12:37:00,012
        assertEquals(now, TimeHint.from("csirkemell rizzsel", now));
        assertEquals(now, TimeHint.from("", now));
        assertEquals(now, TimeHint.from(null, now));
    }

    @Test public void aSpokenHourWinsOverTheDefault() {
        assertEquals(19, TimeHint.hourOf("19 orakor"));
        assertEquals(7, TimeHint.hourOf("reggel 7 orakor"));
        assertEquals(20, TimeHint.hourOf("20:30-kor"));
        assertEquals(-1, TimeHint.hourOf("csirkemell"));
        // Napszak-szavak.
        assertEquals(8, TimeHint.hourOf("reggelire"));
        assertEquals(13, TimeHint.hourOf("ebedre"));
        assertEquals(16, TimeHint.hourOf("uzsonnara"));
        assertEquals(19, TimeHint.hourOf("vacsorara"));
        assertEquals(22, TimeHint.hourOf("ejjel"));
    }

    @Test public void aTimeHintAloneStillMovesTheHour() {
        // Ma este: a nap marad, az óra 19 lesz – nem a beírás pillanata.
        int[] r = dayHour("ma este ettem egy szendvicset");
        assertEquals(5, r[0]);
        assertEquals(19, r[1]);
    }

    @Test public void nothingCrashesOnStrangeInput() {
        long now = wednesdayNoon();
        for (String q : new String[]{null, "", "   ", "0 napja", "999 napja",
                "hétfőn kedden szerdán", "12:00", "🍕"}) {
            long ts = TimeHint.from(q, now);
            assertTrue("jövőbeli időpont: " + q, ts <= now + 1000);
            assertTrue("túl régi: " + q, ts >= now - 15L * 86400000L);
        }
    }

    @Test public void theSpokenNumberOfDaysCounts() {
        // A „két napja" ugyanaz, mint a „2 napja" – eddig csak az utóbbi
        // kelt át a felismerőn, a másik a mai napra tette az étkezést.
        assertEquals(3, dayHour("két napja saláta")[0]);
        assertEquals(3, dayHour("2 napja saláta")[0]);
        assertEquals(1, dayHour("négy napja pizza")[0]);
    }

    @Test public void aWeekAgoIsSevenDays() {
        assertEquals(29, dayHour("egy hete pizza")[0]);   // július 29.
        assertEquals(22, dayHour("két hete pizza")[0]);   // július 22.
        // Két hétnél régebbre nem teszünk vissza semmit.
        assertEquals(5, dayHour("három hete pizza")[0]);
    }

    @Test public void theAfternoonPushesTheClockPastNoon() {
        // Délután nincs négy óra: a napszak igazítja a 12 alatti óraszámot.
        assertEquals(16, dayHour("délután 4-kor uzsonna")[1]);
        assertEquals(19, dayHour("tegnap este 7-kor vacsora")[1]);
        // Reggel viszont marad, ahogy mondták.
        assertEquals(7, dayHour("reggel 7-kor kávé")[1]);
        // A 12 fölötti óraszámhoz nem nyúlunk.
        assertEquals(19, dayHour("tegnap 19 órakor vacsora")[1]);
    }

    @Test public void theSpokenPartOfDayWinsOverTheClock() {
        // Aki délben azt írja, „ma este pizza", a saját estéjéről beszél – a
        // mondat állítását nem írjuk felül azzal, hogy még nincs este.
        assertEquals(19, dayHour("ma este pizza")[1]);
        assertEquals(22, dayHour("éjjel ettem csokit")[1]);
        assertEquals(5, dayHour("ma este pizza")[0]);
    }

    @Test public void anExplicitDateIsUnderstood() {
        // Aki napokkal később ír be egy ebédet, gyakran a dátumot mondja,
        // nem azt, hogy „hat napja".
        assertEquals(30, dayHour("július 30-án torta")[0]);
        assertEquals(1, dayHour("aug 1-jén sütemény")[0]);
        assertEquals(3, dayHour("augusztus 3. vacsora")[0]);
        // A pótlás ablakán kívül eső dátumhoz nem nyúlunk: marad a mai nap.
        assertEquals(5, dayHour("július 4-én fagyi")[0]);
        assertEquals(5, dayHour("december 24-én bejgli")[0]);
        // Hónapnév szám nélkül nem dátum.
        assertEquals(5, dayHour("májusi eper")[0]);
    }

    @Test public void lastWeekdayGoesBackAWholeWeek() {
        // A „múlt kedden" egy héttel korábbi keddet jelent, nem a mostanit.
        assertEquals(28, dayHour("múlt kedden pizza")[0]);   // július 28.
        assertEquals(4, dayHour("kedden pizza")[0]);         // augusztus 4.
    }

    @Test public void dawnIsItsOwnPartOfTheDay() {
        assertEquals(5, dayHour("hajnalban ettem")[1]);
        assertEquals(4, dayHour("tegnap hajnalban ettem")[0]);
    }

    @Test public void aTimeWordInsideAnotherWordDoesNotCount() {
        // Ez csendben másik napra vagy másik napszakra vitte a bejegyzést.
        long now = 1_753_900_000_000L;   // 2025. július 30., szerda
        // „fejjel" nem éjjel, „teste" nem este, „kereste"/„festettem" sem.
        for (String q : new String[]{"fejjel lefelé lógás", "fájt a teste",
                "kereste a súlyzót", "festettem a kerítést"})
            assertEquals("szó belseje: " + q, now, TimeHint.from(q, now));
        // A valódi alakok viszont továbbra is jók, toldalékkal együtt.
        assertEquals(19, hourOf("ma este pizza", now));
        assertEquals(19, hourOf("estére csirke", now));
        assertEquals(8, hourOf("reggelire tojás", now));
        assertEquals(22, hourOf("éjjel ettem", now));
        // A „múlt" itt teljes szó: a multivitamin nem múlt hét.
        assertEquals(1, Days.ago(TimeHint.from("multivitamin kedden", now), now));
        assertEquals(8, Days.ago(TimeHint.from("múlt kedden", now), now));
    }

    private static int hourOf(String q, long now) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(TimeHint.from(q, now));
        return c.get(java.util.Calendar.HOUR_OF_DAY);
    }
}
