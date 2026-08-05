package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    @Test public void everyStemResolvesToItsOwnFoodAndNothingElse() {
        // Nem csak az elsődleges szótő: MINDEGYIK. Egy új étel szótöve könnyen
        // beleesik egy másik nevébe, és onnantól minden ilyen bejegyzés két
        // ételként számolódna – ránézésre észrevehetetlenül.
        for (Foods.Food f : Foods.ALL) {
            for (String st : f.stems) {
                assertEquals("ütköző szótő: \"" + st + "\" (" + f.name + ")",
                        Arrays.asList(f.name), names(st));
            }
        }
    }

    @Test public void noTwoFoodsShareAStem() {
        // Azonos szótő KÉT KÜLÖNBÖZŐ ételnél döntetlen: egyik sem esik a másikba,
        // tehát mindkettő megmaradna – dupla kalória ugyanarra a falatra.
        //
        // Egy ételen BELÜL viszont rendben van két azonosra normalizálódó alak
        // (pl. „bécsi" és „becsi"), az csak felesleges, nem káros.
        java.util.HashMap<String, String> owner = new java.util.HashMap<>();
        for (Foods.Food f : Foods.ALL) {
            for (String st : f.stems) {
                String prev = owner.put(Foods.norm(st), f.name);
                if (prev != null)
                    assertEquals("\"" + st + "\" két ételnél is szerepel", prev, f.name);
            }
        }
    }

    // --- Az összetett nevű fogások egy tételként számítanak ---

    @Test public void multiWordDishesAreNotSplitInTwo() {
        // A „leves" és az „öntet" külön is étel, ezért az összetett nevüknek át
        // kell fognia mindkét szót – különben kétszer számolnánk ugyanazt.
        assertEquals(Arrays.asList("Frankfurti leves"), names("frankfurti leves"));
        assertEquals(Arrays.asList("Gulyásleves"), names("gulyás leves"));
        assertEquals(Arrays.asList("Gulyásleves"), names("gulyásleves"));
        assertEquals(Arrays.asList("Joghurtos öntet"), names("kefires öntet"));
        // A puszta „leves" viszont maradjon az átlagos leves.
        assertEquals(Arrays.asList("Leves (átlag)"), names("leves"));
    }

    // --- Italok és a friss bővítés ---

    @Test public void drinksAreRecognised() {
        assertEquals(Arrays.asList("Kávé (fekete)"), names("kávé"));
        assertEquals(Arrays.asList("Tejeskávé / cappuccino"), names("tejeskávé"));
        assertEquals(Arrays.asList("Tea (cukrozatlan)"), names("tea"));
        // A „bor" benne van az „uborka" és a „borsó" szóban is – ott nem szabad
        // külön italként megjelennie.
        assertEquals(Arrays.asList("Bor (vörös/fehér)"), names("ittam egy pohár bort"));
        assertEquals(Arrays.asList("Uborka"), names("uborka"));
        assertEquals(Arrays.asList("Borsó"), names("borsó"));
    }

    @Test public void newHungarianDishesAreFound() {
        assertEquals(Arrays.asList("Bundás kenyér"), names("bundás kenyér"));
        assertEquals(Arrays.asList("Zsíros kenyér", "Hagyma"), names("zsíros kenyér hagymával"));
        assertEquals(Arrays.asList("Pogácsa"), names("pogácsa"));
        // A kürtőskalács ne essen szét kaláccsá.
        assertEquals(Arrays.asList("Kürtőskalács"), names("kürtőskalács"));
        assertEquals(Arrays.asList("Kalács / bejgli"), names("kalács"));
        // Az almás pite ne számítson almának is.
        assertEquals(Arrays.asList("Almás pite"), names("almás pite"));
        assertEquals(Arrays.asList("Alma"), names("alma"));
        // A növényi tej ne legyen tej + mandula.
        assertEquals(Arrays.asList("Növényi tej (mandula/zab)", "Müzli"),
                names("mandulatej müzlivel"));
    }

    // --- Az étkezés-megnevezések nem ételek ---

    @Test public void mealTimeWordsDoNotSmuggleInFood() {
        // A „vacsora" tartalmazza a „sör" szótövet: enélkül minden vacsora-
        // bejegyzéshez hozzászámolódott egy fél liter sör.
        assertEquals(Arrays.asList("Csirkemell (sült/grill)", "Saláta (zöld)"),
                names("vacsora sült csirkemell salátával"));
        assertEquals(Arrays.asList("Alma"), names("tízórai: alma"));
        assertEquals(Arrays.asList("Túró rudi"), names("uzsonnára túró rudi"));
        assertEquals(Arrays.asList("Zabpehely", "Tej"), names("reggelire zabpehely tejjel"));
        // A mérőszavak sem: a „kávéskanál" nem kávé.
        assertEquals(Arrays.asList("Méz"), names("egy kávéskanál méz"));
    }

    @Test public void theRealFoodStillCountsNextToTheMealTimeWord() {
        // A kimaszkolás csak magát a szót éri – ami mellette áll, az megmarad.
        assertEquals(Arrays.asList("Sör"), names("vacsorára sört ittam"));
        assertEquals(Arrays.asList("Sör"), names("sör"));
        assertEquals(Arrays.asList("Kávé (fekete)"), names("kávé"));
    }

    // --- Összetett ételnevek ne számítsanak kétszer ---

    @Test public void compoundDishesCountOnce() {
        // „krumplipüré" = krumpli + püré két külön tételként jött ki.
        assertEquals(Arrays.asList("Burgonyapüré"), names("krumplipüré"));
        assertEquals(Arrays.asList("Burgonyapüré"), names("püré"));
        assertEquals(Arrays.asList("Burgonya (főtt)"), names("krumpli"));
        // A spagetti bolognai mindkét szórendben egy tétel.
        assertEquals(Arrays.asList("Bolognai spagetti"), names("bolognai spagetti"));
        assertEquals(Arrays.asList("Bolognai spagetti"), names("spagetti bolognai szósszal"));
        assertEquals(Arrays.asList("Tészta (főtt)"), names("spagetti"));
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

    @Test public void everydayWordsWithAFoodStemInsideAreNotFood() {
        // Mind a három valódi eset volt, és mind észrevétlen: a felismerés
        // sikeresnek látszott, csak épp nem azt naplózta, amit az ember evett.
        assertTrue("a majd nem csirkemáj",
                Foods.parse(java.util.Arrays.asList(Foods.ALL), "majd").isEmpty());
        assertTrue("az iskolában nem kóla",
                Foods.parse(java.util.Arrays.asList(Foods.ALL), "iskolában").isEmpty());
        assertTrue("az uszodában nem szódavíz",
                Foods.parse(java.util.Arrays.asList(Foods.ALL), "uszodában").isEmpty());
        // A valódi alakok viszont megmaradnak.
        assertEquals("Csirkemáj", Foods.find("májat").name);
        assertEquals("Üdítő (cukros)", Foods.find("kólát").name);
    }

    @Test public void aContrastiveConjunctionEndsTheNegation() {
        // A „de" ellentétet nyit: ami utána jön, azt megette az ember. Az „és"
        // viszont folytatja a tagadást – ott mindkét tétel kimarad.
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        java.util.List<Foods.Hit> h = Foods.parse(all, "nem ettem csokit de almát igen");
        assertEquals(1, h.size());
        assertEquals("Alma", h.get(0).food.name);
        assertTrue(Foods.parse(all, "nem ettem csokit és chipset").isEmpty());
    }
}
