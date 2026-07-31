package com.edzo.idozito;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * Determinisztikus fuzz a két mondat-értelmezőre (étel, edzés).
 *
 * A nyelvtan mára sokrétű: tagmondatok, kitakarás, számnevek, mértékegységek,
 * darabszám, időszak, hétköznapnevek. Minden új szabály új kölcsönhatás – ez a
 * teszt nem a helyes értelmezést ellenőrzi (arra ott vannak a célzott
 * tesztek), hanem azt, hogy SEMMILYEN bemenet nem tud kivételt vagy képtelen
 * számot kicsikarni. A vetőmag rögzített, tehát a futás megismételhető.
 */
public class ParserFuzzTest {

    /** Életszerű építőkockák: ezek kombinációi fedik a szabályok határait. */
    private static final String[] TOKENS = {
            "csirkemell", "rizs", "kenyér", "tojás", "mandula", "kóla", "futás",
            "kézi", "kondi", "úszás", "bringa", "edzés", "tegnap", "hétfőn",
            "elmúlt", "nap", "hét", "héten", "alatt", "és", "meg", "km", "g",
            "dkg", "kg", "dl", "l", "perc", "óra", "db", "szelet", "gombóc",
            "kb", "fél", "másfél", "két", "három", "tíz", "0", "1", "2", "3",
            // Az új értelmezési utak is kapjanak véletlen kombinációkat.
            "kétszer", "3-szor", "hónap", "méter", "m", "pohár", "burrito",
            "fröccs", "hétszer", "1500",
            "10", "100", "1000", "2,5", "1,5", "999999999", "0,0001", ",", ".",
            ";", "-", "(", ")", "!", "?", "…", "„", "”", "×", "🏃", "🤾",
            "", " ", "  ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "ő", "ű",
    };

    @Test public void randomSentencesNeverCrashTheParsers() {
        Random rnd = new Random(20260731);
        for (int i = 0; i < 4000; i++) {
            StringBuilder sb = new StringBuilder();
            int words = rnd.nextInt(12);
            for (int w = 0; w < words; w++) {
                sb.append(TOKENS[rnd.nextInt(TOKENS.length)]);
                if (rnd.nextInt(4) > 0) sb.append(' ');
            }
            String q = sb.toString();

            // Étel-értelmező: nincs kivétel, a gramm sosem képtelen.
            for (Foods.Hit h : Foods.parse(Arrays.asList(Foods.ALL), q)) {
                assertTrue("negatív gramm erre: " + q, h.grams >= 0);
                assertTrue("képtelen gramm (" + h.grams + ") erre: " + q,
                        h.grams <= 50_000);
            }

            // Edzés-értelmező: nincs kivétel, a terv korlátos.
            Activities.Parsed p = Activities.parse(q, 1_753_900_000_000L);
            assertTrue("napok tartományon kívül: " + q, p.days >= 1 && p.days <= 365);
            assertTrue("eltolás tartományon kívül: " + q, p.offset >= 0 && p.offset <= 366);
            for (Activities.Plan pl : p.plans) {
                assertTrue("darabszám elszaladt erre: " + q, pl.count >= 1 && pl.count <= 50);
                assertTrue("időtartam elszaladt erre: " + q,
                        pl.minutes >= 1 && pl.minutes <= 24 * 60);
                assertTrue("táv elszaladt erre: " + q, pl.km >= 0 && pl.km <= 500);
            }
            // Az időbélyeg-tervezés is korlátos: semmi a jövőben, semmi kivétel.
            for (long t : Activities.timestamps(p, 1_753_900_000_000L))
                assertTrue("jövőbeli időbélyeg erre: " + q, t <= 1_753_900_000_000L);
        }
    }

    @Test public void degenerateInputsAreHandled() {
        String[] nasty = {
                null, "", " ", "\n\t", "…", "12345678901234567890 g kenyér",
                "g g g g g", "km km km", "és és és", ",,,,,", "0 g 0 g 0 g",
                "фыва тест", "test test test", "🏃🏃🏃",
                new String(new char[5000]).replace('\0', 'a'),
                "1 2 3 4 5 6 7 8 9 10 kenyér",
        };
        for (String q : nasty) {
            if (q != null) Foods.parse(Arrays.asList(Foods.ALL), q);
            Activities.Parsed p = Activities.parse(q, 1_753_900_000_000L);
            Activities.timestamps(p, 1_753_900_000_000L);
        }
    }
}
