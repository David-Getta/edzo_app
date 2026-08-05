package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Mennyiség-megadási formák.
 *
 * A mennyiség nem hagyhatja el a tagmondatát – különben az „1 l víz és mandula”
 * egy kiló mandulát jelentene. Egy tagmondat viszont, amiben a szám és a
 * mértékegység mellett SEMMI más nincs, nyilván a szomszédjához tartozik:
 * a „mandula, 30 g” egy étel. Eddig azon múlt, hogy a felhasználó melyik
 * írásjelet választja: kettősponttal működött, vesszővel nem.
 */
public class FoodsQuantityTest {

    private static double grams(String q) {
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
        assertEquals("pontosan egy ételt vártam ebben: " + q, 1, hs.size());
        return hs.get(0).grams;
    }

    @Test public void wholeAndFractionCombineInQuantities() {
        // A „két és fél deci tej" kettője elveszett: fél deci maradt.
        assertEquals(250, grams("két és fél deci tej"), 0.01);
        assertEquals(250, grams("2 és fél dl olaj"), 0.01);
        assertEquals(1500, grams("egy és fél liter víz"), 0.01);
        assertEquals(750, grams("háromnegyed liter tej"), 0.01);
        // A sima tört és a másfél nem romolhatott el.
        assertEquals(150, grams("másfél deci tej"), 0.01);
        assertEquals(500, grams("fél kiló kenyér"), 0.01);
    }

    @Test public void compoundSpelledNumbersWork() {
        // A „negyvenöt gramm", „huszonöt dkg", „ötven gramm" eddig ismeretlen
        // számnév volt, és a tipikus adagra esett vissza.
        assertEquals(45, grams("negyvenöt gramm sajt"), 0.01);
        assertEquals(250, grams("huszonöt dkg liszt"), 0.01);
        assertEquals(50, grams("ötven gramm rizs"), 0.01);
        assertEquals(80, grams("nyolcvan gramm zab"), 0.01);
        assertEquals(320, grams("harminckét dkg sajt"), 0.01);
    }

    @Test public void quarterAndWrittenOutDekaWork() {
        // A „negyed kiló sajt" 30 grammos adag lett: a „negyed" nem volt
        // számnév. A kiírt „deka" pedig nem volt mértékegység (a „dkg" igen).
        assertEquals(250, grams("negyed kiló sajt"), 0.01);
        assertEquals(250, grams("negyed liter tejföl"), 0.01);
        assertEquals(750, grams("háromnegyed liter tej"), 0.01);
        assertEquals(100, grams("10 deka párizsi"), 0.01);
        assertEquals(50, grams("5 deka sajt"), 0.01);
        // A szomszédok nem sérülnek: a „négy" és a „fél" marad, ami volt.
        assertEquals(4 * 55, grams("négy tojás"), 0.01);
        assertEquals(500, grams("fél kiló kenyér"), 0.01);
        assertEquals(200, grams("20 dkg sajt"), 0.01);
    }

    @Test public void aQuantityOnlyClauseBelongsToTheFoodNextToIt() {
        assertEquals(30, grams("mandula, 30 g"), 0.01);
        assertEquals(30, grams("egy marék mandula, kb 30 g"), 0.01);
        assertEquals(30, grams("mandula kb. 30 g"), 0.01);
        assertEquals(30, grams("30 g, mandula"), 0.01);
        // Ezek eddig is működtek – nem romolhattak el.
        assertEquals(30, grams("mandula 30 g"), 0.01);
        assertEquals(30, grams("mandula: 30 g"), 0.01);
        assertEquals(30, grams("mandula (30 g)"), 0.01);
    }

    @Test public void aClauseWithItsOwnWordsKeepsItsQuantity() {
        // Az ismeretlen szóhoz írt mennyiség az övé: az 1 liter nem a
        // mandulánál landol. (A víz ma már ismert tétel, ezért itt egy
        // tényleg ismeretlen szó áll.)
        assertEquals(0, grams("1 l akármi és mandula"), 0.01);
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), "1 l akármi és 30 g mandula");
        assertEquals(1, hs.size());
        assertEquals(30, hs.get(0).grams, 0.01);
    }

    @Test public void spelledOutNumbersWorkBeforeAUnit() {
        // „fél liter tej” eddig a tipikus adagra esett vissza.
        assertEquals(500, grams("fél liter tej"), 0.01);
        assertEquals(500, grams("fél kiló alma"), 0.01);
        assertEquals(200, grams("két deci tej"), 0.01);
        assertEquals(150, grams("másfél dl tej"), 0.01);
        assertEquals(1000, grams("egy kiló csirkemell"), 0.01);
    }

    @Test public void spelledOutNumbersStillCountPiecesWithoutAUnit() {
        // Mértékegység nélkül a számnév darabszám marad – és pontosan egyszer
        // számít, nem kétszer.
        assertEquals(2 * 55, grams("két tojás"), 0.01);
        assertEquals(6 * 55, grams("hat tojás"), 0.01);
        assertEquals(0.5 * 150, grams("fél alma"), 0.01);
        assertEquals(3 * 55, grams("3 tojás"), 0.01);
    }

    @Test public void aWordThatMerelyStartsWithANumberWordIsNotANumber() {
        // A „felvágott” nem fél valamiből: egy étel, mennyiség nélkül.
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), "felvágott");
        assertEquals(1, hs.size());
        assertEquals("Párizsi / felvágott", hs.get(0).food.name);
        assertEquals(0, hs.get(0).grams, 0.01);
    }

    @Test public void severalFoodsKeepTheirOwnQuantities() {
        List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL),
                "csirkemell 200 g, rizs 150 g");
        assertEquals(2, hs.size());
        assertEquals(200, hs.get(0).grams, 0.01);
        assertEquals(150, hs.get(1).grams, 0.01);

        // A lezáró mennyiség az előtte állóhoz tartozik, nem az elsőhöz.
        List<Foods.Hit> two = Foods.parse(Arrays.asList(Foods.ALL), "alma, banán, 30 g");
        assertEquals(2, two.size());
        assertEquals(0, two.get(0).grams, 0.01);
        assertEquals(30, two.get(1).grams, 0.01);
    }

    @Test public void theHundredsAreNumbersToo() {
        // A konyhában a százas adag a leggyakoribb, és pont ez maradt ki:
        // „száz gramm rizs" helyett a tipikus adag ment a naplóba.
        assertEquals(100.0, grams("száz gramm rizs"), 0.001);
        assertEquals(150.0, grams("százötven gramm csirkemell"), 0.001);
        assertEquals(120.0, grams("százhúsz gramm rizs"), 0.001);
        assertEquals(200.0, grams("kétszáz gramm rizs"), 0.001);
        assertEquals(250.0, grams("kétszázötven gramm tészta"), 0.001);
        assertEquals(300.0, grams("háromszáz gramm burgonya"), 0.001);
    }

    @Test public void theDrinkMeasuresHaveRealVolumes() {
        // Az üveg, a kancsó és a korty nem egy pohár: ezekből lesz a napi
        // vízcél, ezért a tévedés a haladássávon is látszik.
        assertEquals(500, grams("ittam egy üveg vizet"), 0.01);
        assertEquals(1000, grams("ittam egy kancsó vizet"), 0.01);
        assertEquals(40, grams("ittam egy korty vizet"), 0.01);
        assertEquals(80, grams("két korty vizet"), 0.01);
        // A pohár marad a tipikus adag.
        assertEquals(250, grams("ittam egy pohár vizet"), 0.01);
        assertEquals(2000, grams("ittam 8 pohár vizet"), 0.01);
    }
}
