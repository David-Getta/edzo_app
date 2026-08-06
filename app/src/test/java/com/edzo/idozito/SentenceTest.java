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

    /** Ételek nélkül (null) is működik – a hívónak nem kell listát adnia. */
    @Test public void foodsAreOptional() {
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("30 perc futás", null, NOW));
        assertEquals(Sentence.Kind.NONE, Sentence.of("2 tojás", null, NOW));
    }
}
