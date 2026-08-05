package com.edzo.idozito;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * Determinisztikus fuzz a mondat-értelmezőkre (étel, edzés, sorozat,
 * intervallum, időpont).
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
            "fröccs", "hétszer", "1500", "július", "december", "28-án", "31",
            "minden", "naponta", "1-1", "hétvégén", "lépés", "ezer", "10000",
            "és", "negyvenöt", "huszonöt", "adag", "korsó", "holnap", "jövő",
            "fekvőtámasz", "3x10", "bicaj",
            "tabata", "lábnap", "padel", "curling", "spartan", "vívás",
            "főzelék", "spenót", "palócleves", "fasírozott", "krémleves",
            "napi", "x", "8x400", "6x1", "10x100", "2x", "x2", "99x99",
            "grillcsirke", "krumpi", "sali", "adaggal", "adagot", "poke",
            "cvekedli", "rizotto",
            "nem", "helyett", "nélkül", "kihagytam", "elmaradt", "ittam",
            "hetente", "kéthetente", "havonta", "másnaponta", "fél évig",
            "idén", "néztem", "rendeltem", "vettem", "bérlet", "étteremben",
            "hétfőn és szerdán", "csütörtökön", "vasárnap", "rukkola", "mangó",
            "wok", "taco", "vajon", "sorban", "sajtótájékoztató",
            "tányér", "bögre", "kanál", "marék", "tábla", "kupica", "zacskó",
            "júl.", "aug", "07.28-án", "2026.07.28", "óta", "liter",
            "gumicukor", "red bull", "polenta", "kettlebell", "parkrun",
            "10", "100", "1000", "2,5", "1,5", "999999999", "0,0001", ",", ".",
            ";", "-", "(", ")", "!", "?", "…", "„", "”", "×", "🏃", "🤾",
            "", " ", "  ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "ő", "ű",
            // Az újabb értelmezési utak: vessző nélküli piramis, vesszős
            // ismétlés-lista, kukacos súly, táblás körjelölés, AMRAP, és az
            // új gyakorlatnevek.
            "60x10", "70x8", "80x6", "5,5,5", "12,10,8", "@", "@100", "kör:",
            "10x30s", "on", "off", "amrap", "swing", "kettlebell swing",
            "step up", "alkartámasz", "lábtávolítás", "russian twist",
            "másodperc", "munka", "pihenő", "rpe 9",
    };

    @Test public void randomSentencesNeverCrashTheParsers() {
        // Több mag: egy 42 000 mondatos külön futtatás nulla hibát talált, és
        // a magok váltogatása így is beépült – más mag más kombinációkat üt.
        for (long seed : new long[]{20260731, 987654, 42}) {
            Random rnd = new Random(seed);
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
                assertTrue("óra tartományon kívül: " + q, p.hour >= 0 && p.hour <= 23);
                for (Activities.Plan pl : p.plans) {
                    assertTrue("darabszám elszaladt erre: " + q, pl.count >= 1 && pl.count <= 50);
                    assertTrue("időtartam elszaladt erre: " + q,
                            pl.minutes >= 1 && pl.minutes <= 24 * 60);
                    assertTrue("táv elszaladt erre: " + q, pl.km >= 0 && pl.km <= 500);
                    assertTrue("lépésszám elszaladt erre: " + q,
                            pl.steps >= 0 && pl.steps <= 100_000);
                }
                // Az időbélyeg-tervezés is korlátos: semmi a jövőben, semmi kivétel.
                for (long t : Activities.timestamps(p, 1_753_900_000_000L))
                    assertTrue("jövőbeli időbélyeg erre: " + q, t <= 1_753_900_000_000L);

                // Súlyzós sorozatok: a naplóba csak életszerű szám kerülhet.
                for (StrengthParse.Item it : StrengthParse.parse(q)) {
                    assertTrue("üres sorozat erre: " + q, !it.sets.isEmpty());
                    assertTrue("RPE tartományon kívül: " + q,
                            it.rpe == 0 || (it.rpe >= 6 && it.rpe <= 10));
                    for (StrengthParse.Set st : it.sets) {
                        assertTrue("ismétlés elszaladt erre: " + q,
                                st.reps >= 1 && st.reps <= 200);
                        assertTrue("súly elszaladt erre: " + q,
                                st.weight >= 0 && st.weight <= 500);
                    }
                }

                // Intervallum: rossz munkaidővel az egész kör használhatatlan.
                IntervalParse.Plan ip = IntervalParse.parse(q);
                if (ip != null) {
                    assertTrue("kör elszaladt erre: " + q,
                            ip.rounds >= 1 && ip.rounds <= IntervalParse.MAX_ROUNDS);
                    assertTrue("munkaidő elszaladt erre: " + q,
                            ip.work >= IntervalParse.MIN_SEC && ip.work <= IntervalParse.MAX_SEC);
                    assertTrue("pihenő elszaladt erre: " + q,
                            ip.rest >= 0 && ip.rest <= IntervalParse.MAX_SEC);
                    assertTrue("bemelegítés elszaladt erre: " + q,
                            ip.warm >= 0 && ip.warm <= IntervalParse.MAX_SEC);
                    assertTrue("levezetés elszaladt erre: " + q,
                            ip.cool >= 0 && ip.cool <= IntervalParse.MAX_SEC);
                }

                // A súlyzós mondat dátuma: vagy nincs, vagy múltbeli és
                // életszerű – ez írja a bejegyzés napját az erősítő naplóban.
                long day = Activities.singleDayTs(p, 1_753_900_000_000L);
                assertTrue("képtelen dátum erre: " + q, day == 0
                        || (day <= 1_753_900_000_000L
                            && day >= 1_753_900_000_000L - 400L * 86400000L));

                // Időpont: a mai napnál nem későbbi, és két hétnél régebbre sem.
                //
                // A kimondott napszak SZÁNDÉKOSAN eshet a mostani óra utánra:
                // aki délben azt írja, „ma este pizza", a saját estéjéről
                // beszél. Ezért a mai nap vége a határ, nem a mostani perc.
                long when = TimeHint.from(q, 1_753_900_000_000L);
                assertTrue("holnapi vagy későbbi időpont erre: " + q,
                        when <= 1_753_900_000_000L + 86400000L);
                assertTrue("túl régi időpont erre: " + q,
                        when >= 1_753_900_000_000L - 15L * 86400000L);
            }
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
            StrengthParse.parse(q);
            IntervalParse.parse(q);
            TimeHint.from(q, 1_753_900_000_000L);
        }
    }
}
