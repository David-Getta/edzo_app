package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/**
 * Az étkezés időpontja a mondatból.
 *
 * Egy rossz nap két helyen is elrontja a napi kalóriát: ahonnan elveszi, és
 * ahová beteszi. Ezért inkább maradjon a mostani pillanat, ha a mondat nem
 * mond semmit az időről.
 */
public class MealTimeTest {

    /** Szerda dél – innen minden hétköznapnév egyértelmű. */
    private static long wednesdayNoon() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.AUGUST, 5, 12, 0, 0);   // 2026. augusztus 5. szerda
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static int[] dayHour(String text) {
        long ts = MealTime.from(text, wednesdayNoon());
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
        assertEquals(0, MealTime.daysBack("30 napja", wednesdayNoon()));
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
        assertEquals(now, MealTime.from("csirkemell rizzsel", now));
        assertEquals(now, MealTime.from("", now));
        assertEquals(now, MealTime.from(null, now));
    }

    @Test public void aSpokenHourWinsOverTheDefault() {
        assertEquals(19, MealTime.hourOf("19 orakor"));
        assertEquals(7, MealTime.hourOf("reggel 7 orakor"));
        assertEquals(20, MealTime.hourOf("20:30-kor"));
        assertEquals(-1, MealTime.hourOf("csirkemell"));
        // Napszak-szavak.
        assertEquals(8, MealTime.hourOf("reggelire"));
        assertEquals(13, MealTime.hourOf("ebedre"));
        assertEquals(16, MealTime.hourOf("uzsonnara"));
        assertEquals(19, MealTime.hourOf("vacsorara"));
        assertEquals(22, MealTime.hourOf("ejjel"));
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
            long ts = MealTime.from(q, now);
            assertTrue("jövőbeli időpont: " + q, ts <= now + 1000);
            assertTrue("túl régi: " + q, ts >= now - 15L * 86400000L);
        }
    }
}
