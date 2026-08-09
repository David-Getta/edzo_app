package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * A beírt szövegből kiolvasott mennyiségek. Ez működteti a „150 g csirkemell
 * 200 g rizs" típusú bevitelt: minden szám a hozzá legközelebb álló ételhez
 * tartozik, és ahol nincs szám, ott 0 marad (a hívó tölti fel tipikus adaggal
 * vagy a közös gramm elosztásával).
 */
public class FoodsParseTest {

    private static final List<Foods.Food> DB = Arrays.asList(Foods.ALL);

    private static List<Foods.Hit> hits(String q) { return Foods.parse(DB, q); }

    @Test public void gramsBeforeTheFoodAreAssigned() {
        List<Foods.Hit> h = hits("150 g csirkemell 200 g rizs");
        assertEquals(2, h.size());
        assertEquals("Csirkemell (sült/grill)", h.get(0).food.name);
        assertEquals(150.0, h.get(0).grams, 0.001);
        assertEquals("Rizs (főtt)", h.get(1).food.name);
        assertEquals(200.0, h.get(1).grams, 0.001);
    }

    @Test public void gramsAfterTheFoodAreAssigned() {
        List<Foods.Hit> h = hits("csirkemell 150g");
        assertEquals(1, h.size());
        assertEquals(150.0, h.get(0).grams, 0.001);
    }

    @Test public void unitVariantsAreUnderstood() {
        assertEquals(150.0, hits("csirkemell 150 gramm").get(0).grams, 0.001);
        assertEquals(150.0, hits("csirkemell 150 gr").get(0).grams, 0.001);
        // dekagramm: 20 dkg = 200 g
        assertEquals(200.0, hits("rizs 20 dkg").get(0).grams, 0.001);
    }

    @Test public void liquidUnitsBecomeGrams() {
        // Folyadéknál 1 ml ≈ 1 g.
        assertEquals(300.0, hits("3 dl tej").get(0).grams, 0.001);
        assertEquals(200.0, hits("2 deci tej").get(0).grams, 0.001);
        assertEquals(250.0, hits("250 ml protein turmix").get(0).grams, 0.001);
        assertEquals(1000.0, hits("1 l üdítő").get(0).grams, 0.001);
        // A magában álló „l" csak mértékegység lehet: a „2 lecsó" nem 2 liter,
        // hanem két adag (2 × 300 g).
        assertEquals(600.0, hits("2 lecsó").get(0).grams, 0.001);
    }

    @Test public void withoutANumberGramsStayZero() {
        List<Foods.Hit> h = hits("rántott hús rizzsel");
        assertEquals(2, h.size());
        for (Foods.Hit x : h) assertEquals(0.0, x.grams, 0.001);
    }

    @Test public void pieceCountsAreConvertedToGrams() {
        // A "2" itt darabszám, nem gramm: 2 tojás = 2 × 55 g.
        List<Foods.Hit> h = hits("2 tojás");
        assertEquals(1, h.size());
        assertEquals("Tojás", h.get(0).food.name);
        assertEquals(110.0, h.get(0).grams, 0.001);
        assertEquals(220.0, hits("4 tojás").get(0).grams, 0.001);
        assertEquals(360.0, hits("3 banán").get(0).grams, 0.001);
    }

    @Test public void theNewFoodsCanBeCountedInPiecesToo() {
        // A v28.2-ben érkezett ételek közül azok, amiket természetes darabra
        // mondani. Enélkül a „2 pogácsa" a számot egyszerűen eldobta volna.
        assertEquals(60.0, hits("2 pogácsa").get(0).grams, 0.001);
        assertEquals(100.0, hits("egy túrós batyu").get(0).grams, 0.001);
        assertEquals(120.0, hits("két bundás kenyér").get(0).grams, 0.001);
        assertEquals(80.0, hits("10 datolya").get(0).grams, 0.001);
        assertEquals(150.0, hits("három szilva").get(0).grams, 0.001);
        assertEquals(160.0, hits("2 sárgarépa").get(0).grams, 0.001);
    }

    @Test public void everyPieceWeightPointsAtARealFood() {
        // Elgépelt névnél a darabsúly csendben nem érvényesülne: a szám
        // eltűnne, és a felhasználó a tipikus adagot kapná helyette.
        for (Foods.Food f : Foods.ALL) {
            int g = Foods.pieceGrams(f);
            assertTrue("értelmetlen darabsúly: " + f.name + " = " + g, g >= 0);
        }
        // A darabra számolható ételek szótöve önmagában is megtalálja őket,
        // különben a „2 <étel>" alak sem működne.
        for (String name : new String[]{"Pogácsa", "Túrós batyu", "Bundás kenyér",
                "Datolya", "Szilva", "Sárgarépa", "Hurka"}) {
            Foods.Food f = null;
            for (Foods.Food c : Foods.ALL) if (c.name.equals(name)) f = c;
            assertTrue("nincs ilyen étel: " + name, f != null);
            assertTrue("nincs darabsúlya: " + name, Foods.pieceGrams(f) > 0);
        }
    }

    @Test public void aQuantityAfterTheFoodBelongsToThatFood() {
        // Ez a súgóban példaként szereplő alak, és pont ez cserélődött fel: a
        // 150 közelebb volt a „rizs" szó ELEJÉHEZ, mint a tíz betűs
        // „csirkemell" elejéhez, így a két mennyiség helyet cserélt.
        List<Foods.Hit> h = hits("csirkemell 150g, rizs 200 g");
        assertEquals(2, h.size());
        assertEquals("Csirkemell (sült/grill)", h.get(0).food.name);
        assertEquals(150.0, h.get(0).grams, 0.001);
        assertEquals("Rizs (főtt)", h.get(1).food.name);
        assertEquals(200.0, h.get(1).grams, 0.001);
    }

    @Test public void decimalsAreUnderstoodWithAComma() {
        assertEquals(250.0, hits("2,5 dl joghurt").get(0).grams, 0.001);
        assertEquals(500.0, hits("0,5 l tej").get(0).grams, 0.001);
        // Ponttal is, hátha úgy írja be valaki.
        assertEquals(250.0, hits("2.5 dl joghurt").get(0).grams, 0.001);
    }

    @Test public void aQuantityDoesNotJumpToAnotherClause() {
        // A vízhez írt 1 liter korábban a mandulára szállt át: 1000 g mandula
        // közel hatezer kalória lett volna. A víz ma már saját (0 kcal-os)
        // tétel, a liter az övé – a mandula a maga 30 grammját kapja.
        List<Foods.Hit> h = hits("1 l víz és 30 g mandula");
        assertEquals(2, h.size());
        assertEquals("Víz / ásványvíz", h.get(0).food.name);
        assertEquals(1000.0, h.get(0).grams, 0.001);
        assertEquals("Mandula", h.get(1).food.name);
        assertEquals(30.0, h.get(1).grams, 0.001);
    }

    @Test public void severalClausesEachKeepTheirOwnAmount() {
        List<Foods.Hit> h = hits("200 g csirkemell, 150 g rizs, 50 g saláta");
        assertEquals(3, h.size());
        assertEquals(200.0, h.get(0).grams, 0.001);
        assertEquals(150.0, h.get(1).grams, 0.001);
        assertEquals(50.0, h.get(2).grams, 0.001);
    }

    @Test public void hungarianNumberWordsCountToo() {
        assertEquals(110.0, hits("két tojás").get(0).grams, 0.001);
        assertEquals(110.0, hits("kettő tojás").get(0).grams, 0.001);
        assertEquals(360.0, hits("három banán").get(0).grams, 0.001);
        assertEquals(55.0, hits("egy zsemle").get(0).grams, 0.001);
        assertEquals(75.0, hits("fél alma").get(0).grams, 0.001);   // fél darab
        // Több étel, mindegyik a maga számnevével.
        List<Foods.Hit> h = hits("egy alma és két körte");
        assertEquals(2, h.size());
        assertEquals(150.0, h.get(0).grams, 0.001);
        assertEquals(300.0, h.get(1).grams, 0.001);
    }

    @Test public void bareNumbersStayIgnoredWhereTheyMakeNoSense() {
        // Aminek nincs darabmérete, ott az adag a darab: „2 rizs" két adag.
        assertEquals(400.0, hits("2 rizs").get(0).grams, 0.001);
        // De csak életszerű adagszámig – a „12 rizs" inkább elgépelt gramm,
        // és három kiló rizst írni a naplóba rosszabb, mint egy adagot.
        assertEquals(0.0, hits("12 rizs").get(0).grams, 0.001);
        // Életszerűtlen darabszám: inkább ne találgassunk.
        assertEquals(0.0, hits("50 tojás").get(0).grams, 0.001);
        // Mértékegység nélküli nagy szám sem gramm.
        assertEquals(0.0, hits("100 csirkemell").get(0).grams, 0.001);
    }

    @Test public void onlyOneAmountPerFood() {
        // Két szám, két étel – mindkettő megkapja a sajátját, egyik sem kettőt.
        List<Foods.Hit> h = hits("100 g sajt 50 g sonka");
        assertEquals(2, h.size());
        double sum = 0;
        for (Foods.Hit x : h) sum += x.grams;
        assertEquals(150.0, sum, 0.001);
        for (Foods.Hit x : h) assertTrue("mindkét étel kapott mennyiséget", x.grams > 0);
    }

    @Test public void nothingRecognisedGivesEmptyResult() {
        assertTrue(hits("zzzqqq 100 g").isEmpty());
    }

    /** A panasz szavában lakó étel-szótő nem étkezés. */
    @Test public void complaintWordsAreNotFood() {
        // A „vizesedik a térdem" ízületi folyadék, nem két és fél deci
        // ásványvíz – eddig italbejegyzés lett belőle.
        for (String q : new String[]{"vizesedik a térdem edzés után",
                "vizenyős a bokám", "vízretenció miatt nőtt a súlyom"})
            assertTrue(q + " -> " + hits(q), hits(q).isEmpty());
        // A valódi ivás viszont marad.
        assertEquals(1, hits("ittam két pohár vizet edzés után").size());
    }
    /**
     * Hátravetett tagadás: „csokit nem ettem".
     *
     * A tagadás eddig csak ELŐRE hatott. Magyarul viszont ugyanolyan gyakori
     * a fordított szórend, és ott az étel a tagadás ELŐTT áll: minden ilyen
     * mondat felvette azt, amit az ember épp NEM evett meg.
     */
    @Test public void aTrailingNegationAlsoCancelsTheFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"csokit nem ettem", "sört nem ittam",
                "pizzát nem kértem", "kenyeret nem eszem", "nem ettem csokit"})
            assertTrue(q + " -> " + Foods.parse(all, q), Foods.parse(all, q).isEmpty());
        // A tagadás nem eszi meg a szomszéd tagmondat ételét.
        java.util.List<Foods.Hit> h = Foods.parse(all, "csokit nem, almát igen");
        assertEquals(1, h.size());
        assertEquals("Alma", h.get(0).food.name);
        h = Foods.parse(all, "csirkemellet ettem, csokit nem");
        assertEquals(1, h.size());
        assertEquals("Csirkemell (sült/grill)", h.get(0).food.name);
    }
}
