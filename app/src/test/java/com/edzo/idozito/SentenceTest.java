package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * A mondat-útbaigazító.
 *
 * Nem új felismerés: azt mondja meg, MELYIK meglévő felismerő tud a mondattal
 * mit kezdeni. A tét az, hogy a rossz képernyőn ne „ezt még nem ismerem"
 * legyen a válasz egy tökéletesen érthető mondatra.
 */
public class SentenceTest {

    private static final List<Foods.Food> FOODS = Arrays.asList(Foods.ALL);
    private static final long NOW = 1_753_869_600_000L;

    private static Sentence.Kind of(String q) { return Sentence.of(q, FOODS, NOW); }

    @Test public void everySentenceFindsItsOwnLog() {
        String[][] cases = {
                {"30 perc futás", "WORKOUT"},
                {"10 km-t bicikliztem", "WORKOUT"},
                {"tegnap este kondi", "WORKOUT"},
                {"6x1 km", "WORKOUT"},
                {"10000 lépés", "WORKOUT"},
                {"1 óra jóga", "WORKOUT"},
                {"3x10 fekvenyomás 60 kg", "STRENGTH"},
                {"guggolás 5x5 80 kg", "STRENGTH"},
                {"4 sorozat 8 fekvenyomás", "STRENGTH"},
                {"plank 3x1 perc", "STRENGTH"},
                {"100 fekvőtámasz", "STRENGTH"},
                {"3 kör 40 mp munka 20 mp pihenő", "INTERVAL"},
                {"8x20/10", "INTERVAL"},
                {"emom 10 perc", "INTERVAL"},
                {"20 perc alatt 40/20", "INTERVAL"},
                {"1:30 munka 0:30 pihenő 6 kör", "INTERVAL"},
                {"rántott hús rizzsel", "MEAL"},
                {"2 tojás", "MEAL"},
                {"150 g csirkemell 200 g rizs", "MEAL"},
                {"fél alma", "MEAL"},
                {"tegnap este pizzát ettem", "MEAL"},
                {"ittam fél liter vizet", "MEAL"},
        };
        StringBuilder bad = new StringBuilder();
        for (String[] c : cases) {
            String got = of(c[0]).name();
            if (!got.equals(c[1]))
                bad.append("\n  ").append(c[0]).append(" -> ").append(got)
                   .append(" (várt: ").append(c[1]).append(")");
        }
        assertEquals("rossz ajtó:" + bad, 0, bad.length());
    }

    /**
     * Amit egyik felismerő sem ért, arra ne találjunk ki naplót.
     *
     * A rossz útbaigazítás rosszabb a semminél: elviszi a felhasználót egy
     * képernyőre, ahol ugyanúgy nem lesz belőle bejegyzés.
     */
    @Test public void whatNobodyUnderstandsStaysUnderstoodByNobody() {
        for (String q : new String[]{"jó napom volt", "asdfgh", "ma nem edzettem",
                "holnap majd", "köszönöm szépen", "hm", ""})
            assertEquals(q, Sentence.Kind.NONE, of(q));
    }

    /**
     * A sorozatos mondat az erősítő naplóé, akkor is, ha edzésnek is elmenne.
     *
     * A súly és az ismétlés CSAK ott őrződik meg; egy általános „kondi"
     * bejegyzésben mindkettő elveszne.
     */
    @Test public void setsBeatAPlainWorkout() {
        assertEquals(Sentence.Kind.STRENGTH, of("tegnap guggolás 4x8 100 kg"));
        assertEquals(Sentence.Kind.STRENGTH, of("30 fekvőtámasz és 20 guggolás"));
    }

    @Test public void everyKindHasAWhereAndAHint() {
        for (Sentence.Kind k : Sentence.Kind.values()) {
            if (k == Sentence.Kind.NONE) {
                assertEquals("", Sentence.where(k));
                assertEquals("", Sentence.hint(k));
                continue;
            }
            assertTrue(k.name(), Sentence.where(k).length() > 2);
            assertTrue(k.name(), Sentence.hint(k).length() > 20);
        }
    }

    /**
     * Az étkezés-mondatból ne legyen fantom-edzés.
     *
     * Ez a rosszabbik irány: egy kitalált edzés bekerül a szériába, az XP-be,
     * a jelvényekbe és a statisztikába is – és a felhasználó a naplóban látja
     * viszont, hogy „ma edzett", pedig csak vacsorázott.
     */
    @Test public void aMealNeverBecomesAWorkout() {
        StringBuilder bad = new StringBuilder();
        for (String q : MEALS) {
            if (!StrengthParse.parse(q).isEmpty()) bad.append("\n  sorozat: ").append(q);
            if (!Activities.parse(q, NOW).isEmpty()) bad.append("\n  edzés: ").append(q);
            if (IntervalParse.parse(q) != null) bad.append("\n  időzítő: ").append(q);
            if (!BodyParse.parse(q).isEmpty()) bad.append("\n  mérés: ").append(q);
        }
        assertEquals("fantom bejegyzés:" + bad, 0, bad.length());
    }

    /** …és az edzés-mondatból se legyen véletlen étkezés. */
    @Test public void aWorkoutNeverBecomesAMeal() {
        StringBuilder bad = new StringBuilder();
        for (String q : WORKOUTS) {
            java.util.List<Foods.Hit> h = Foods.parse(FOODS, q);
            if (!h.isEmpty()) bad.append("\n  ").append(q).append(" -> ").append(h.get(0).food.name);
        }
        assertEquals("kitalált étel:" + bad, 0, bad.length());
    }

    /** Életszerű étkezés-mondatok – a felismerés MÁSIK oldalának határa. */
    private static final String[] MEALS = {
            "reggelire zabkása", "ebédre csirkemell rizzsel", "vacsorára rántotta két tojásból",
            "uzsonnára egy alma", "ettem egy szendvicset", "ittam egy kávét tejjel",
            "gyros tál", "hekk sültkrumplival", "tojásos nokedli", "grillcsirke saláta",
            "túró rudi", "egy tábla csoki", "kaptam egy szelet tortát", "ettem egy adag lecsót",
            "vettem egy kiflit", "kolbászos rántotta", "krumplifőzelék fasírttal",
            "sonkás-sajtos melegszendvics", "egy marék mandula", "reggeli: kefir és banán",
            "hamburger menü", "tejbegríz", "zsemle vajjal", "csirkepaprikás nokedlivel",
            "vegyes saláta olívaolajjal", "150 g csirkemell 200 g rizs", "2 tojás",
            "fél alma", "két szelet kenyér", "3 dl tej", "tegnap este pizzát ettem",
            "ma reggel müzli joghurttal", "1 kg alma", "20 dkg sajt", "fél adag gyros",
            "banán 2 db", "tojás (3 db)", "5 dl narancslé", "egy pohár bor",
            "két korsó sör", "10 szem mandula",
    };

    /** Életszerű mozgás-mondatok, mind a négy másik felismerőből. */
    private static final String[] WORKOUTS = {
            "30 perc futás", "10 km-t bicikliztem", "tegnap este kondi", "1 óra jóga",
            "reggel elmentem futni fél órát", "ma edzettem egy órát a teremben",
            "délután gyalogoltam egy órát", "egy óra spinning", "45 perces edzés",
            "csináltam egy 20 perces kocogást", "10000 lépés", "ma sokat sétáltam",
            "húsz perc nyújtás", "két óra foci meccs", "reggeli torna 15 perc",
            "este 30 perc szobabicikli", "1 óra 20 perc futás", "futottam 8 km-t 45 perc alatt",
            "úsztam 1000 métert", "korcsolyáztam egy órát", "3x10 fekvenyomás 60 kg",
            "guggolás 5x5 80 kg", "húzódzkodás 3x8", "plank 3x1 perc",
            "4 sorozat 8 fekvenyomás", "100 fekvőtámasz", "bicepsz 12-10-8 15 kg",
            "lábtolás 3x12 120 kg", "evezés 4x10 50 kg", "3 kör 40 mp munka 20 mp pihenő",
            "8x20/10", "tabata", "emom 10 perc", "ma reggel 78,4 kg", "78 kiló vagyok",
            "mérleg: 81,2", "18% testzsír", "kézilabda meccs", "teniszeztem egy órát",
            "asztalitenisz 40 perc", "boxoltam 30 percet", "sí 3 óra",
    };

    /**
     * A megosztott sablon neve ne térítse el a tervet.
     *
     * A sablonok nevében gyakran ott egy sportszó („Zsírégető HIIT", „Kondi
     * kör", „Futó intervall"), és attól a megosztott szöveg megtörtént
     * edzésnek látszott – a beállítás helyett egy edzés-bejegyzést kínált.
     * A PIHENŐT is kimondó, többköros terv viszont egyértelműen időzítő.
     */
    @Test public void aSharedTemplateStaysATimerPlan() {
        for (String q : new String[]{
                "Zsírégető HIIT: 8 kör 40 mp munka 20 mp pihenő  (összesen 8 perc)",
                "Tabata: 8 kör 20 mp munka 10 mp pihenő  (összesen 4 perc)",
                "Futó intervall: 6 kör 2 perc munka 1 perc pihenő  (összesen 18 perc)",
                "Kondi kör: 10 kör 45 mp munka 15 mp pihenő  (összesen 10 perc)"})
            assertEquals(q, Sentence.Kind.INTERVAL, of(q));
        // A megtörtént edzés viszont marad edzés.
        assertEquals(Sentence.Kind.WORKOUT, of("45 perc kondi"));
        assertEquals(Sentence.Kind.WORKOUT, of("30 perc futás"));
        assertEquals(Sentence.Kind.WORKOUT, of("ma 1 óra hiit edzés"));
    }

    /** Ételek nélkül (null) is működik – a hívónak nem kell listát adnia. */
    @Test public void foodsAreOptional() {
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("30 perc futás", null, NOW));
        assertEquals(Sentence.Kind.NONE, Sentence.of("2 tojás", null, NOW));
    }
}
