package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Az ételfelismerés egységtesztjei. Ez a logika adja az Étrend lelkét („írd be,
 * mit ettél"), és két valódi hiba is elbújt már benne: a rövid szótövek
 * beleragadtak a hosszabb ételnevekbe (dupla kalória), és a szavankénti keresés
 * az elsőként talált ételt adta vissza a legjobb helyett.
 *
 * Csak a beépített listával dolgozunk (Context nélküli utak), így sima JUnit-tal
 * fut, emulátor nélkül.
 */
public class FoodsTest {

    private static final List<Foods.Food> DB = Arrays.asList(Foods.ALL);

    private static List<String> names(String query) {
        List<String> out = new ArrayList<>();
        for (Foods.Food f : Foods.findAll(DB, query)) out.add(f.name);
        return out;
    }

    private static String one(String query) {
        Foods.Food f = Foods.find(DB, query);
        return f == null ? null : f.name;
    }

    // --- Rövid szótő ne ragadjon bele a hosszabb ételnévbe (v24.6) ---

    @Test public void shortStemDoesNotDuplicateLongerDish() {
        assertEquals(Arrays.asList("Rántott sajt"), names("rántott sajt"));
        assertEquals(Arrays.asList("Tejföl"), names("tejföl"));
        assertEquals(Arrays.asList("Bableves"), names("bableves"));
        assertEquals(Arrays.asList("Tükörtojás"), names("tükörtojás"));
        assertEquals(Arrays.asList("Görög joghurt"), names("görög joghurt"));
        assertEquals(Arrays.asList("Túró rudi"), names("túró rudi"));
        assertEquals(Arrays.asList("Müzliszelet"), names("müzliszelet"));
    }

    // --- Több hozzávaló egy mondatban ---

    @Test public void separateIngredientsAreAllFound() {
        assertEquals(Arrays.asList("Rántott hús (sertés)", "Rizs (főtt)"),
                names("rántott hús rizzsel"));
        assertEquals(Arrays.asList("Csirkemell (sült/grill)", "Rizs (főtt)"),
                names("150 g csirkemell 200 g rizs"));
        assertEquals(Arrays.asList("Csirkecomb", "Sült krumpli"),
                names("csirkecomb sült krumplival"));
    }

    // --- Összetett étel: a felhasználó eredeti példamondata (v24.8) ---

    @Test public void breadedChickenIsOneDishNotTwo() {
        assertEquals(Arrays.asList("Rántott csirkemell", "Rizs (főtt)"),
                names("csirkemellből rántotthúst rízzsel"));
        // A sertésből készült rántott hús viszont maradjon az, ami.
        assertEquals(Arrays.asList("Rántott hús (sertés)", "Rizs (főtt)"),
                names("rántott hús rizzsel"));
    }

    // --- Egyetlen étel keresése: a leghosszabb egyezés nyerjen (v24.7) ---

    /**
     * Ragozott alakok feloldása. Szándékosan csak egyértelmű eseteket
     * ellenőrzünk: egy néhány betűs töredék („burg") több ételre is illik
     * (burgonya, burgonyapüré, burger), ott nincs egyetlen helyes válasz.
     */
    @Test public void inflectedFormsResolveToTheRightFood() {
        assertEquals("Rizs (főtt)", one("rizs"));
        assertEquals("Rizs (főtt)", one("rizzsel"));
        assertEquals("Burgonya (főtt)", one("burgonyával"));
        assertEquals("Burgonya (főtt)", one("krumplival"));
        assertEquals("Csirkemell (sült/grill)", one("csirkemellből"));
        assertEquals("Kenyér", one("kenyérrel"));
        assertEquals("Tojás", one("tojással"));
        assertEquals("Zabpehely", one("zabpehellyel"));
        assertEquals("Tejföl", one("tejföl"));
        assertEquals("Alma", one("almát"));
    }

    /**
     * Minden étel elsődleges szótöve pontosan önmagát adja vissza – se többet,
     * se mást. Ez fogja meg új étel felvételekor, ha a szótöve beleütközik egy
     * másikba (így derült ki például, hogy a „burgonyapüré" a Burgonyát is,
     * a „gulyásleves" a Levest is felvette volna).
     */
    @Test public void everyFoodResolvesToItselfByItsPrimaryStem() {
        for (Foods.Food f : Foods.ALL) {
            List<String> got = names(f.stems[0]);
            assertEquals("ütköző szótő: \"" + f.stems[0] + "\" (" + f.name + ")",
                    Arrays.asList(f.name), got);
        }
    }

    @Test public void unknownFoodReturnsNothing() {
        assertEquals(new ArrayList<String>(), names("zzzqqq"));
        assertEquals(null, one("zzzqqq"));
    }

    // --- Ékezet-mentesítés ---

    @Test public void accentsAreIgnored() {
        assertEquals(one("túró"), one("turo"));
        assertEquals(names("rántott hús"), names("rantott hus"));
    }
}
