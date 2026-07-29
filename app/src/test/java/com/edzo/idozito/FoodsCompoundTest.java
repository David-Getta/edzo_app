package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Összetett magyar ételnevek: egy szó = egy étel.
 *
 * A magyar ételnevek jó része összetett szó, aminek mindkét felére illeszkedik
 * egy-egy szótő: „lencsefőzelék” = lencse + főzelék, „sajtosszendvics” = sajt +
 * szendvics, „csokitorta” = csoki + sütemény. Eddig mindkét fele bekerült, azaz
 * ugyanaz az EGY fogás kétszer számított – az alábbi listából minden harmadik
 * szó rossz kalóriát adott.
 */
public class FoodsCompoundTest {

    /** Életszerű, egyszavas magyar ételnevek. */
    private static final String[] WORDS = {
            "lencsefőzelék", "krumplifőzelék", "borsófőzelék", "zöldbabfőzelék", "tökfőzelék",
            "spenótfőzelék", "kelkáposztafőzelék", "sárgaborsófőzelék", "paradicsomleves",
            "húsleves", "zöldségleves", "gombaleves", "krumplileves", "babgulyás", "gulyásleves",
            "bableves", "tejföl", "csirkepörkölt", "marhapörkölt", "sertéspörkölt",
            "krumplisaláta", "csirkesaláta", "cézársaláta", "görögsaláta", "tonhalsaláta",
            "tojássaláta", "túrókrém", "sajtkrém", "májkrém", "túrógombóc", "szilvásgombóc",
            "kakaóscsiga", "túrótorta", "sajttorta", "almatorta", "csokitorta", "gyümölcstorta",
            "mákostészta", "túróstészta", "káposztástészta", "tejbegríz", "grízestészta",
            "rántotthús", "rántottsajt", "bécsiszelet", "müzliszelet", "proteinszelet",
            "csokoládészelet", "gyümölcsjoghurt", "görögjoghurt", "banánturmix", "proteinturmix",
            "zabkása", "tejeskávé", "feketekávé", "csirkemellsaláta", "virsli", "hotdog",
            "hamburger", "sajtburger", "csirkeburger", "pizzaszelet", "szendvics",
            "sajtosszendvics", "sonkásszendvics", "melegszendvics", "bundáskenyér",
            "zsíroskenyér", "vajaskenyér", "túrósbatyu", "narancslé", "almalé",
    };

    private static List<Foods.Food> hits(String q) {
        List<Foods.Food> out = new ArrayList<>();
        for (Foods.Hit h : Foods.parse(Arrays.asList(Foods.ALL), q)) out.add(h.food);
        return out;
    }

    private static String names(List<Foods.Food> fs) {
        StringBuilder sb = new StringBuilder();
        for (Foods.Food f : fs) sb.append(sb.length() > 0 ? " + " : "").append(f.name);
        return sb.toString();
    }

    @Test public void oneWordNeverCountsTwoFoods() {
        StringBuilder bad = new StringBuilder();
        for (String w : WORDS) {
            List<Foods.Food> fs = hits(w);
            if (fs.size() > 1) bad.append("\n  ").append(w).append(" -> ").append(names(fs));
        }
        assertTrue("duplán számolt összetett szavak:" + bad, bad.length() == 0);
    }

    @Test public void theCompoundKeepsTheDishNotTheIngredient() {
        // A szó „súlyosabb” fele marad: az, amelyik egy adagban több kalóriát ad.
        assertEquals("Főzelék", names(hits("lencsefőzelék")));
        assertEquals("Szendvics", names(hits("sajtosszendvics")));
        assertEquals("Sütemény", names(hits("csokitorta")));
        assertEquals("Gulyásleves", names(hits("babgulyás")));
        assertEquals("Hamburger", names(hits("sajtburger")));
        assertEquals("Túrós batyu", names(hits("túrósbatyu")));
        // Zöldsalátának nézni egy csirkemellsalátát nagyot tévedne lefelé.
        assertEquals("Csirkemell (sült/grill)", names(hits("csirkemellsaláta")));
    }

    @Test public void separateWordsAreStillSeparateFoods() {
        // A szabály csak a szón BELÜL köt ki egy ételt: a köret nem tűnhet el.
        assertEquals(2, hits("csirkemell rizzsel").size());
        assertEquals(3, hits("kenyér vajjal és sajttal").size());
        assertEquals(2, hits("rántott hús krumplipürével").size());
    }

    @Test public void sliceIsAQuantityWordNotAMuesliBar() {
        // A „szelet” hétköznapi szó: mennyiséget jelöl. Amíg müzliszelet-szótő
        // volt, minden „két szelet kenyér” mellé bekerült egy müzliszelet is.
        assertEquals("Kenyér", names(hits("két szelet kenyér")));
        assertEquals("Sütemény", names(hits("egy szelet torta")));
        assertEquals("Sonka", names(hits("három szelet sonka")));
        // A müzliszelet magától továbbra is megvan.
        assertEquals("Müzliszelet", names(hits("müzliszelet")));
        assertEquals("Müzliszelet", names(hits("ettem egy müzliszeletet")));
    }

    @Test public void theBreadedCutletIsMeatNotACerealBar() {
        assertEquals("Rántott hús (sertés)", names(hits("bécsi szelet")));
        assertEquals("Rántott hús (sertés)", names(hits("rántott szelet")));
    }

    @Test public void kilogramsAreUnderstood() {
        // A „kg” hiányzott a mértékegységek közül: az „1 kg csirkemell” úgy
        // viselkedett, mintha oda se írták volna a mennyiséget.
        assertEquals(1000, grams("1 kg csirkemell"), 0.01);
        assertEquals(500, grams("0,5 kg burgonya"), 0.01);
        assertEquals(1500, grams("1,5 kg alma"), 0.01);
        assertEquals(2000, grams("2 kiló rizs"), 0.01);
        // A régiek nem romolhattak el:
        assertEquals(500, grams("50 dkg marhahús"), 0.01);
        assertEquals(250, grams("250 g rizs"), 0.01);
        assertEquals(200, grams("2 dl tej"), 0.01);
        assertEquals(1000, grams("1 l tej"), 0.01);
    }

    private static double grams(String q) {
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
        assertEquals("pontosan egy ételt vártam ebben: " + q, 1, hs.size());
        return hs.get(0).grams;
    }
}
