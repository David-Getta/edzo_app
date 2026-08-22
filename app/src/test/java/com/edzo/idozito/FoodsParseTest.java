package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    /**
     * A mennyiség a mondat MÁSIK felében: „…, két adag".
     *
     * A mennyiség szándékosan nem ugrik át tagmondat-határon, de az utolsó,
     * CSUPÁN mennyiséget tartalmazó tagmondat nem lehet másé – ott nincs mit
     * félreérteni, és eddig egy adag ment be kettő helyett.
     */
    @Test public void aTrailingAmountBelongsToTheOnlyFood() {
        java.util.List<Foods.Hit> h = hits("ebédre töltött káposzta volt, két adag");
        assertEquals(1, h.size());
        assertEquals(700.0, h.get(0).grams, 0.5);
        assertEquals(800.0, hits("gulyásleves, két tányérral").get(0).grams, 0.5);
        assertEquals(105.0, hits("reggelire kenyeret ettem, három szelet").get(0).grams, 0.5);
        assertEquals(400.0, hits("kávé, két bögrével").get(0).grams, 0.5);
        assertEquals(100.0, hits("kaptam egy fagyit, 2 gombócot").get(0).grams, 0.5);
        // Puszta darabszám is: a tárgyrag a mértékegység helyét foglalja el.
        assertEquals(360.0,
                hits("sütöttem egy adag palacsintát, megettem hatot").get(0).grams, 0.5);
        assertEquals(360.0, hits("ettem palacsintát, hatot").get(0).grams, 0.5);
        assertEquals(3000.0, hits("ittam egy sört, hatot").get(0).grams, 0.5);
        // A darab is mértékegység: „sushi vacsora, 12 darab".
        assertEquals(360.0, hits("sushi vacsora, 12 darab").get(0).grams, 0.5);
        assertEquals(180.0, hits("ettem palacsintát, 3 db").get(0).grams, 0.5);
        // Két étel mellett nem találgatunk: a „csirkemell rizzsel, 200 g"
        // kétszáz grammja nem tartozhat mindkettőhöz.
        assertEquals(2, hits("csirkemell rizzsel, 200 g").size());
    }

    /**
     * A tápérték-sor „protein"-je nem turmix.
     *
     * A „reggeli 400 kcal 25 g protein" mellé eddig háromszáz gramm
     * proteinturmix került a naplóba – a reggeli kalóriájának duplája.
     */
    @Test public void theMacroWordIsNotADrink() {
        assertTrue(hits("reggeli 400 kcal 25 g protein").isEmpty());
        assertTrue(hits("ebéd 750 kcal, 45 g protein").isEmpty());
        // A valódi étel viszont marad.
        assertEquals("Protein turmix", hits("150 g protein turmix").get(0).food.name);
        assertEquals("Proteinszelet", hits("150 g proteinszelet").get(0).food.name);
    }

    /**
     * A felsorolásban a „sör" nem mértékszó.
     *
     * Az „egy sor csoki" sora mennyiséget mond, de a „pizza, sör, fagyi"
     * vesszője új tételt nyit – a sör eddig némán kimaradt belőle.
     */
    @Test public void theBeerInAListIsStillABeer() {
        assertEquals(3, hits("pizza, sör, fagyi").size());
        assertEquals("Sör", hits("pizza, sör, fagyi").get(1).food.name);
        // A „TRX sor 3x12" sora SOROZAT, nem sör – a sorozatjelölés követi.
        assertTrue(hits("TRX sor 3x12").isEmpty());
        assertTrue(hits("kábel sor 4x10").isEmpty());
        // A mértékszó változatlan: az „egy sor csoki" egy csoki.
        assertEquals(1, hits("egy sor csoki").size());
        assertEquals("Csokoládé", hits("egy sor csoki").get(0).food.name);
    }

    /** A „bevettem" a vásárlás igéjét tartalmazza, mégis elfogyasztás. */
    @Test public void takingASupplementIsEating() {
        assertEquals("Étrend-kiegészítő",
                hits("edzés előtt bevettem egy kreatint").get(0).food.name);
        assertEquals(1, hits("bevettem a D-vitamint").size());
        // A valódi vásárlás továbbra sem étkezés.
        assertTrue(hits("vettem két kiló almát").isEmpty());
    }

    /** A szórás nem ital: a kakaópor nem két és fél deci kakaó. */
    @Test public void aSprinkleIsNotADrink() {
        assertEquals(1, hits("tejbegríz kakaóporral").size());
        assertEquals("Tejbegríz", hits("tejbegríz kakaóporral").get(0).food.name);
        assertEquals(1, hits("palacsinta porcukorral").size());
        // A valódi kakaó marad.
        assertEquals("Kakaó (tejes)", hits("ittam egy kakaót").get(0).food.name);
    }

    /** A kertÉPÍTÉSben ott a PITE, a tojásKERESÉSben a tojás. */
    @Test public void everydayWordsHidingAFoodStemAreNotMeals() {
        for (String q : new String[]{"kertépítés egész hétvégén", "izomépítés a cél",
                "testépítés edzés 60 perc", "húsvéti tojáskeresés a kertben"})
            assertTrue(q + " -> " + hits(q), hits(q).isEmpty());
        // A valódi étel marad.
        assertEquals("Pite (almás/gyümölcsös)", hits("almás pite desszertnek").get(0).food.name);
        assertEquals("Tojás", hits("ettem 2 tojást").get(0).food.name);
    }

    /** A magyar „-ában/-ába" rag BABot gyárt: a hibában, a lábában, a próbában. */
    @Test public void theBeanOnlyCountsAtTheWordStart() {
        for (String q : new String[]{"hibában voltam", "fáj a lábában",
                "próbában vettem részt"})
            assertTrue(q + " -> " + hits(q), hits(q).isEmpty());
        // A valódi bab marad – összetételben is.
        assertEquals("Bab (főtt)", hits("ettem egy tányér babot").get(0).food.name);
        assertEquals("Zöldbab", hits("zöldbab köret").get(0).food.name);
        assertEquals("Chilis bab (con carne)", hits("chilis bab vacsorára").get(0).food.name);
    }

    /** Melléknévként nem étel: „vizes lett a cipőm", „combos edzés volt". */
    @Test public void adjectivesAreNotMeals() {
        for (String q : new String[]{"vizes lett a cipőm az esőben",
                "combos edzés volt ma", "sajtoltam a gépen"})
            assertTrue(q + " -> " + hits(q), hits(q).isEmpty());
        // A valódi ivás és a valódi csirkecomb marad.
        assertEquals("Víz / ásványvíz", hits("ittam egy pohár vizet").get(0).food.name);
        assertEquals("Csirkecomb", hits("két csirkecomb").get(0).food.name);
    }

    /** A panasz szavában lakó étel-szótő nem étkezés. */
    @Test public void complaintWordsAreNotFood() {
        // A „vizesedik a térdem" ízületi folyadék, nem két és fél deci
        // ásványvíz – eddig italbejegyzés lett belőle.
        for (String q : new String[]{"vizesedik a térdem edzés után",
                "vizenyős a bokám", "vízretenció miatt nőtt a súlyom",
                "vízhajtó miatt 2 kilót estem"})
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
        // A „mégsem" ugyanaz a tagadás: a „mégsem ettem a csokit" eddig
        // huszonöt gramm csokoládét írt a naplóba.
        for (String q : new String[]{"mégsem ettem a csokit", "mégsem ittam meg a sört"})
            assertTrue(q + " -> " + Foods.parse(all, q), Foods.parse(all, q).isEmpty());
        assertEquals("Alma", Foods.parse(all, "mégsem ettem csokit, de almát igen")
                .get(0).food.name);
        // A tagadás nem eszi meg a szomszéd tagmondat ételét.
        java.util.List<Foods.Hit> h = Foods.parse(all, "csokit nem, almát igen");
        assertEquals(1, h.size());
        assertEquals("Alma", h.get(0).food.name);
        h = Foods.parse(all, "csirkemellet ettem, csokit nem");
        assertEquals(1, h.size());
        assertEquals("Csirkemell (sült/grill)", h.get(0).food.name);
    }

    /**
     * A tagadott evés-ige nem menti fel a bevásárlást.
     *
     * A „vettem egy kiflit, de nem ettem meg" mondatban ott az „ettem", és a
     * kivétel-lista eddig felmentette a vásárlást: a meg nem evett kifli
     * bekerült a naplóba.
     */
    @Test public void aNegatedEatingVerbDoesNotUndoTheShopping() {
        assertTrue(Foods.looksUneaten("vettem egy kiflit, de nem ettem meg"));
        assertTrue(Foods.looksUneaten("bevásároltam, de még semmit nem ettem"));
        // Ami tényleg megtörtént, az marad bejegyzés.
        assertFalse(Foods.looksUneaten("vettem egy kiflit és megettem"));
        assertFalse(Foods.looksUneaten("edzés előtt bevettem egy kreatint"));
        assertTrue(Foods.looksUneaten("vettem egy pizzát"));
    }

    /**
     * A kihagyás beszámolója nem étkezés.
     *
     * A „3 hetet bírtam ki cukor nélkül" büszkeség, nem adag – eddig
     * háromszázharminc gramm cukormentes étel lett belőle a naplóban.
     */
    @Test public void anAbstinenceReportIsNotAMeal() {
        assertTrue(Foods.looksUneaten("eddig 3 hetet bírtam ki cukor nélkül"));
        assertTrue(Foods.looksUneaten("lemondtam a csokiról"));
        assertTrue(Foods.looksUneaten("böjtöltem délig"));
        // A valódi étkezés marad.
        assertFalse(Foods.looksUneaten("ettem egy csokit"));
        assertFalse(Foods.looksUneaten("cukormentes üdítő 5 dl"));
    }

    /**
     * A következő étkezésig kibírni nem lemondás.
     *
     * A „reggel egy bögre kávé tejjel, semmi más, ebédig kibírtam" reggelije
     * megevett reggeli: a kitartás csak UTÁNA kezdődik. Eddig ez a fél
     * mondat az egész bejegyzést törölte – a kávé sem került a naplóba.
     */
    @Test public void holdingOutUntilLunchIsNotGivingUp() {
        assertFalse(Foods.looksUneaten("reggel egy bögre kávé tejjel, "
                + "semmi más, ebédig kibírtam"));
        assertFalse(Foods.looksUneaten("reggeli zabkása, délig kibírtam"));
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertFalse(Foods.parse(all, "reggel egy bögre kávé tejjel, "
                + "semmi más, ebédig kibírtam").isEmpty());
        // A határ nélküli kitartás marad lemondás.
        assertTrue(Foods.looksUneaten("eddig 3 hetet bírtam ki cukor nélkül"));
        assertTrue(Foods.looksUneaten("egy hónapja kibírtam csoki nélkül"));
    }

    /**
     * A helyesbítés második száma az igazi – az étrendben is.
     *
     * A „nem ittam 3 kávét, csak 1-et" mondatból eddig három kávé került a
     * naplóba: pont az, amit a mondat tagad.
     */
    @Test public void theCorrectedAmountIsWhatCounts() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        List<Foods.Hit> h = Foods.parse(all, "nem ittam 3 kávét, csak 1-et");
        assertEquals(1, h.size());
        assertEquals(200, h.get(0).grams, 1);
        assertEquals(150, Foods.parse(all, "nem 2 alma volt, hanem 1").get(0).grams, 1);
        // A javítás nélküli mondat változatlan: három kávé az három.
        assertEquals(600, Foods.parse(all, "ittam 3 kávét").get(0).grams, 1);
    }

    /**
     * A hangzóhiányos tő is ragozás: „epret", „retket", „cukrot".
     *
     * A magyar kidobja a tő utolsó magánhangzóját ragozáskor, és a szótő
     * ettől nem illeszkedik: az „ettem epret" és az „ettem 5 szaloncukrot"
     * üres választ kapott – pedig ez a szó rendes tárgyesete.
     */
    @Test public void theElidedStemIsStillTheSameFood() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertEquals("Eper", Foods.parse(all, "ettem epret").get(0).food.name);
        assertEquals("Retek", Foods.parse(all, "ettem retket").get(0).food.name);
        assertEquals("Cukor", Foods.parse(all, "tettem bele cukrot").get(0).food.name);
        assertEquals("Szaloncukor",
                Foods.parse(all, "ettem 5 szaloncukrot").get(0).food.name);
        // Az alapalak és a többi összetétel változatlan.
        assertEquals("Eper", Foods.parse(all, "eper 200 g").get(0).food.name);
        assertEquals("Szörp (hígítva)", Foods.parse(all, "eperszörp").get(0).food.name);
    }

    /**
     * A tápérték-sor zsírja nem konyhai zsír.
     *
     * A „ma 1850 kcal, 140 g fehérje, 180 g szénhidrát, 60 g zsír" hatvan
     * grammjából hatvan gramm OLAJ lett a naplóban – a makró-sor mellé, még
     * egyszer. A „100 g zsír" magában viszont valódi étel.
     */
    @Test public void theNutrientLineFatIsNotCookingFat() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all,
                "ma 1850 kcal, 140 g fehérje, 180 g szénhidrát, 60 g zsír").isEmpty());
        assertEquals("Olaj", Foods.parse(all, "100 g zsír").get(0).food.name);
    }

    /**
     * A gyümölcs neve plusz a „lé" külön étel.
     *
     * Az „egy pohár szőlőlé" a szőlő szótövére esett, és ÖT GRAMM szőlő
     * került a naplóba egy pohár lé helyett.
     */
    @Test public void theJuiceIsNotTheFruit() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        Foods.Hit h = Foods.parse(all, "egy pohár szőlőlé").get(0);
        assertEquals("Gyümölcslé", h.food.name);
        assertEquals(250, h.grams, 1);
        assertEquals("Gyümölcslé", Foods.parse(all, "meggylé").get(0).food.name);
        // A gyümölcs magában marad gyümölcs.
        assertEquals("Szőlő", Foods.parse(all, "szőlő 100 g").get(0).food.name);
    }

    /**
     * A frizbiben nincs rizs.
     *
     * A „frizbi a parkban 1 óra" kétszáz gramm főtt rizst írt a naplóba egy
     * lejátszott óra helyett – a fRIZbi közepén ott a rizs szótöve. A
     * frizura és a frizsider ugyanez.
     */
    @Test public void thereIsNoRiceInAFrisbee() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "frizbi a parkban 1 óra").isEmpty());
        assertTrue(Foods.parse(all, "új frizurám lett").isEmpty());
        // A rizs viszont marad rizs.
        assertEquals("Rizs (főtt)", Foods.parse(all, "csirke rizzsel")
                .get(1).food.name);
    }

    /**
     * A százalékban mért zsír a TESTZSÍR, nem konyhai olaj.
     *
     * A „80,2 kg, 18% zsír, derék 86 cm" mérés-mondatból eddig tíz gramm olaj
     * is bekerült az étrendbe. A grammban mért zsír viszont valódi étel.
     */
    @Test public void fatInPercentIsBodyFatNotOil() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "80,2 kg, 18% zsír, derék 86 cm").isEmpty());
        assertEquals(100.0, Foods.parse(all, "100 g zsírt használtam "
                + "a sütihez").get(0).grams, 0.01);
    }

    /**
     * A helyszín neve nem plusz menü a felsorolt étel mellé.
     *
     * A puszta „meki" tényleg menüt jelent – de „a mekiben ettem: sajtburger,
     * közepes krumpli, kóla" mondatban a tételek fel vannak sorolva, és a
     * menü MELLÉJÜK került: ötszáz kalória kétszer.
     */
    @Test public void theVenueIsNotAnExtraMenu() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        List<Foods.Hit> h = Foods.parse(all, "a mekiben ettem: sajtburger, "
                + "közepes krumpli, kóla");
        for (Foods.Hit x : h) assertFalse(x.food.name.startsWith("Gyorséttermi"));
        assertEquals(3, h.size());
        // A puszta helyszín marad menü, a kötőszó pedig hozzáadást jelent.
        assertEquals("Gyorséttermi menü", Foods.parse(all, "meki").get(0).food.name);
        assertEquals(2, Foods.parse(all, "meki és egy shake").size());
    }

    /**
     * Más tányérja nem az én naplóm.
     *
     * A „a férjem pizzát evett, én salátát" pizzája a férjé, mégis bekerült
     * az étrendbe – háromszáz kalória olyan ételből, amit más evett meg. A
     * mozgás-oldalon ez a szabály régóta megvan.
     */
    @Test public void someoneElsesPlateIsNotMyLog() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        List<Foods.Hit> h = Foods.parse(all, "a férjem pizzát evett, én salátát");
        assertEquals(1, h.size());
        assertEquals("Saláta (zöld)", h.get(0).food.name);
        assertTrue(Foods.parse(all, "a gyerekek fagyiztak, én kihagytam").isEmpty());
        // A közösen evett étel marad: az „ettünk" első személy.
        assertEquals("Pizza", Foods.parse(all, "a férjemmel pizzát ettünk")
                .get(0).food.name);
        // A főzés nem evés: az étel közös, a gulyás marad.
        assertEquals("Gulyásleves", Foods.parse(all, "a párom főzött gulyást, "
                + "két tányérral ettem").get(0).food.name);
    }

    /** A cukorbeteg nem cukor: a diagnózisból nem lesz tíz gramm az étrendben. */
    @Test public void aDiabeticIsNotSugar() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "cukorbeteg vagyok, figyelem "
                + "a szénhidrátot").isEmpty());
        assertEquals("Cukor", Foods.parse(all, "egy teáskanál cukor "
                + "a kávéba").get(0).food.name);
    }

    /**
     * A heti főzés nem mai evés.
     *
     * A „vasárnapi meal prep: 4 adag csirkés rizs a hétre" hatszáz gramm
     * csirkét írt a MAI naplóba – abból az ételből, ami a jövő hét ebédje.
     * A kimondott evés viszont marad: a „meal prepből ettem egy adagot" már
     * falat.
     */
    @Test public void mealPrepIsNotEatenToday() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "vasárnapi meal prep: 4 adag csirkés "
                + "rizs a hétre").isEmpty());
        assertFalse(Foods.parse(all, "a meal prepből ettem egy adag csirkés "
                + "rizst").isEmpty());
    }

    /**
     * Az elfelejtett étel nem került a szájba.
     *
     * Az „elfelejtettem bevenni a vitaminokat" étrend-kiegészítőt írt a
     * naplóba – pont arról, ami kimaradt. A valódi evés-ige viszont
     * felment: az „elfelejtettem fotózni, de megettem a lasagnét" falat.
     */
    @Test public void aForgottenPillWasNeverTaken() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "elfelejtettem bevenni "
                + "a vitaminokat").isEmpty());
        assertFalse(Foods.parse(all, "elfelejtettem fotózni, de megettem "
                + "a lasagnét").isEmpty());
    }

    /**
     * A szerviz végén nem ital a víz.
     */
    @Test public void aBikeShopIsNotAGlassOfWater() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "bringaszerviz után próbakör").isEmpty());
        assertTrue(Foods.parse(all, "szervizben volt az autó").isEmpty());
    }

    /**
     * A recept szerint készült étel megevett étel, az alapanyag a tálé.
     *
     * A „töltött paprika nagymama receptje szerint" vacsora – a recept
     * szava eddig receptkeresésnek nézte. A „sushi szett, lazacos" lazaca
     * a szettben van, a lecsó paprikája a lábosban.
     */
    @Test public void aRecipeDishIsEatenAndItsBaseIsIncluded() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals("Töltött paprika", Foods.parse(all, "töltött paprika "
                + "nagymama receptje szerint").get(0).food.name);
        assertTrue(Foods.parse(all, "receptet keresek zabkásához").isEmpty());
        assertEquals(1, Foods.parse(all, "sushi szett 8 darabos, "
                + "lazacos").size());
        assertEquals(1, Foods.parse(all, "házi lecsó sok paprikával, "
                + "2 tányér").size());
    }

    /** A szám utáni „fogás" a menü fogása, nem a fogas nevű hal. */
    @Test public void fiveCoursesAreNotFiveFish() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "degusztációs menü 5 fogás, kicsi "
                + "adagok").isEmpty());
        assertEquals("Hal (fehér)", Foods.parse(all, "fogast ettem roston "
                + "sütve").get(0).food.name);
    }

    /** A vas tabletta étrend-kiegészítő – a vasat tolni súlyzózás marad. */
    @Test public void anIronPillIsASupplement() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals("Étrend-kiegészítő", Foods.parse(all, "vas tabletta "
                + "reggelente, ma is bevettem").get(0).food.name);
    }

    /** A gyerekadag fél adag, a duplázott kettő. */
    @Test public void kidPortionsAndDoublesScale() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(125.0, Foods.parse(all, "gyerekadag spagettit "
                + "ettem").get(0).grams, 0.01);
        assertEquals(500.0, Foods.parse(all, "duplázott sajtburger, nagyon "
                + "éhes voltam").get(0).grams, 0.01);
    }

    /**
     * A latte egyetlen ital – az összetevői a pohárban vannak.
     *
     * A „matcha latte zabtejjel" négy tételt kapott: tea + tejeskávé +
     * növényi tej + kávé. A „tejmentes cappuccino" mellé ráadásul egy
     * pohár tej került – pont abból, amit a szó kizár. A felsorolt kávé
     * és tea külön marad.
     */
    @Test public void aLatteIsOneDrink() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(1, Foods.parse(all, "matcha latte zabtejjel a "
                + "kávézóban").size());
        assertEquals(1, Foods.parse(all, "tejmentes cappuccino "
                + "kókusztejjel").size());
        assertEquals(2, Foods.parse(all, "kávé és tea is volt ma").size());
    }

    /**
     * A fél tábla az étel után is fél tábla, a gyerek menüje a gyereké.
     *
     * A „milka csoki fél tábla" nulla grammos bejegyzés lett (a fél a
     * tört-ágon kiesett), a „happy meal a gyereknek" menüje pedig a
     * szülő naplójába került. Az angol sandwich és a mcflurry is étel.
     */
    @Test public void halfABarAfterTheFoodStillCounts() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(50.0, Foods.parse(all, "milka csoki fél tábla")
                .get(0).grams, 0.01);
        List<Foods.Hit> h = Foods.parse(all, "mcdonalds happy meal a "
                + "gyereknek, én egy mcflurryt");
        assertEquals(1, h.size());
        assertEquals("Fagylalt", h.get(0).food.name);
        assertEquals("Szendvics", Foods.parse(all, "subway ham sandwich, "
                + "15 cm").get(0).food.name);
    }

    /**
     * A sétáló-vétel azonnal a szájban landol.
     *
     * A „vettem egy fagyit a sétány végén" nem bevásárlás – a fagyit
     * senki nem viszi haza a kamrába. A „vettem 2 kg almát" bevásárlás
     * marad, a citromOS fagyi íze pedig nem külön pohár citromlé.
     */
    @Test public void anIceCreamBoughtOnAWalkIsEaten() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        List<Foods.Hit> h = Foods.parse(all, "vettem egy fagyit a sétány "
                + "végén, citromos");
        assertEquals(1, h.size());
        assertEquals("Fagylalt", h.get(0).food.name);
        assertTrue(Foods.parse(all, "vettem 2 kg almát a piacon").isEmpty());
    }

    /** A hóemberépítésben nincs pite, a lubickolásban nincs uborka. */
    @Test public void snowmanBuildingHasNoPieInIt() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "hóemberépítés és hógolyózás a "
                + "kertben").isEmpty());
        assertTrue(Foods.parse(all, "lubickoltam a Balatonban").isEmpty());
        assertFalse(Foods.parse(all, "almáspite a nagyinál").isEmpty());
        assertFalse(Foods.parse(all, "uborkasaláta ebédre").isEmpty());
    }

    /** A százalékos zsír és a milliméteres comb nem étel. */
    @Test public void bodyFatAndCaliperReadingsAreNotFood() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "15,8% zsír a mai mérésen").isEmpty());
        assertTrue(Foods.parse(all, "testzsírmérő csipesz: 12 mm has, "
                + "8 mm comb").isEmpty());
        // A valódi zsíros étel marad.
        assertFalse(Foods.parse(all, "zsíros kenyér hagymával").isEmpty());
    }

    /** A „steady state" közepén nem ital a tea. */
    @Test public void steadyStateIsNotACupOfTea() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "steady state kardió 40 perc").isEmpty());
        assertFalse(Foods.parse(all, "teát ittam a futás után").isEmpty());
    }

    /**
     * Az alanyesetű darabszám az „is" nyomatékkal is darabszám.
     *
     * A „gin tonik a bárban, kettő is" két pohár – eddig egy ment be. A
     * „három is lecsúszott" ugyanígy a darabszámot mondja.
     */
    @Test public void aNominativeCountWithEmphasisStillCounts() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(500.0, Foods.parse(all, "gin tonik a koktélbárban, "
                + "kettő is").get(0).grams, 0.01);
        assertEquals(180.0, Foods.parse(all, "palacsintát sütöttem, három "
                + "is lecsúszott").get(0).grams, 0.01);
    }

    /** Az „olajos" jelző nem egy kanál olaj – az olajos magvak dió. */
    @Test public void greasyIsNotASpoonOfOil() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (Foods.Hit h : Foods.parse(all, "ebédre rakott krumpli, eléggé "
                + "olajos volt"))
            assertFalse(h.food.name.equals("Olaj"));
        assertEquals("Dió", Foods.parse(all, "ettem egy marék olajos "
                + "magvat").get(0).food.name);
        // A ragos olaj marad: az „olajban sült" valódi olaj.
        assertEquals("Olaj", Foods.parse(all, "olajban sült hal").get(0)
                .food.name);
    }

    /** A vízi jelző sport, nem ital – a megivott víz marad. */
    @Test public void openWaterIsNotAGlassOfWater() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "nyílt vízi úszás a Balatonban").isEmpty());
        assertFalse(Foods.parse(all, "vizet ittam az edzés alatt").isEmpty());
    }

    /**
     * A wrap tortillája maga a wrap – nem külön falat.
     *
     * A „csirkés wrap teljes kiőrlésű tortillában" a wrap mellé egy egész
     * tortillát is beírt: az étel és az alapanyaga kétszer számolódott.
     * A magában evett tortilla marad.
     */
    @Test public void aWrapsTortillaIsTheWrap() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        List<Foods.Hit> h = Foods.parse(all, "csirkés wrap teljes kiőrlésű "
                + "tortillában");
        assertEquals(1, h.size());
        assertEquals("Csirkés wrap", h.get(0).food.name);
        assertEquals("Tortilla / wrap", Foods.parse(all, "tortillát ettem "
                + "tojással reggelire").get(0).food.name);
    }

    /**
     * A kelesztés és a dagasztás előkészület, nem evés.
     */
    @Test public void proofingDoughIsNotEating() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "tésztát kelesztettem délután").isEmpty());
        assertTrue(Foods.parse(all, "kenyértésztát dagasztottam").isEmpty());
    }

    /**
     * A kollagén egy l-lel sem kóla.
     *
     * A „kolagén port keverek a kávémba" a kóla tövén ült, és üdítőt írt
     * a naplóba. A kávé marad, az üdítő nem.
     */
    @Test public void misspelledCollagenIsNotACoke() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (Foods.Hit x : Foods.parse(all, "kolagén port keverek "
                + "a kávémba minden reggel"))
            assertFalse(x.food.name.startsWith("Üdítő"));
    }

    /**
     * A turmix hozzávalói nem a turmix mellé számítanak.
     *
     * A „reggel smoothie: banán, spenót, zabtej" kettőspont utáni listája
     * maga a turmix – eddig a háromszáz grammos átlag-turmix ÉS az összes
     * hozzávaló is bement, közel dupla kalóriával.
     */
    @Test public void smoothieIngredientsAreTheSmoothie() {
        List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        List<Foods.Hit> h = Foods.parse(all, "reggel smoothie: banán, spenót, "
                + "zabtej, egy kanál mogyoróvaj");
        for (Foods.Hit x : h)
            assertFalse(x.food.name.startsWith("Gyümölcsturmix"));
        assertEquals(4, h.size());
        // A puszta turmix hozzávaló-lista nélkül marad turmix.
        assertEquals(1, Foods.parse(all, "ittam egy smoothie-t").size());
        // A -BÓL ragos hozzávaló is a turmixé: egyetlen tétel megy be.
        assertEquals(1, Foods.parse(all, "gyümölcsturmix banánból "
                + "és eperből").size());
    }

    /**
     * A csoki kockája a törésrács egy négyzete, nem egy adag.
     *
     * A „2 kocka csoki" eddig ugyanannyi volt, mint az egy kocka: a kocka
     * nem volt mérőszó, a darabszám elveszett, és a tipikus adag (25 g)
     * ment be. Egy kocka viszont kb. öt gramm – ötszörös túlbecslés annál,
     * aki büszkén naplózza, hogy csak egy kockát evett.
     */
    @Test public void aSquareOfChocolateIsFiveGrams() {
        assertEquals(5.0, hits("ettem egy kocka csokit").get(0).grams, 0.001);
        assertEquals(10.0, hits("ettem 2 kocka csokit").get(0).grams, 0.001);
        // Más ételnél a kocka darabszó marad: egy kocka sajt egy adag.
        assertEquals(30.0, hits("ettem egy kocka sajtot").get(0).grams, 0.001);
        // A kockacukor és a leveskocka szava érintetlen.
        assertEquals("Cukor", hits("kockacukor a kávéba").get(0).food.name);
    }
    /**
     * A korizás szlengjében rizs lakik, de nem étel.
     *
     * A „görkoriztam a rakparton 8 km-t" mellé eddig egy adag főtt rizs is
     * került a naplóba. Az igazi rizs marad.
     */
    @Test public void skatingSlangIsNotRice() {
        assertTrue(hits("görkoriztam a rakparton 8 km-t").isEmpty());
        assertTrue(hits("koriztunk a jégpályán").isEmpty());
        assertEquals("Rizs (főtt)", hits("rizst ettem csirkével")
                .get(0).food.name);
    }
    /**
     * A vízhólyag seb, nem ásványvíz.
     *
     * A „felszakadt a vízhólyag a sarkamon a 15 km-en" mellé eddig két és
     * fél deci víz került a naplóba. Az ivott víz marad.
     */
    @Test public void aBlisterIsNotDrinkingWater() {
        assertTrue(hits("felszakadt a vízhólyag a sarkamon").isEmpty());
        assertEquals(500.0, hits("vizet ittam, 5 dl").get(0).grams, 0.001);
    }
    /**
     * A hagymás jelző fűszerezés, nem külön adag hagyma.
     *
     * A „fokhagymás csirkemell" mellé eddig ötven gramm hagyma került.
     * A hagyma főnévként marad étel, és a „gíros" elgépelés is gyros.
     */
    @Test public void aGarlickyAdjectiveIsNotAnOnionPortion() {
        for (Foods.Hit h : hits("fokhagymás csirkemell 180g rizzsel"))
            assertFalse(h.food.name.equals("Hagyma"));
        for (Foods.Hit h : hits("lilahagymás rántotta"))
            assertFalse(h.food.name.equals("Hagyma"));
        assertEquals("Hagyma", hits("hagymát pirítottam a lecsóba")
                .get(0).food.name);
        assertEquals("Gyros", hits("ettem egy gírost").get(0).food.name);
    }
    /**
     * A falat és a harapás a legkisebb mérték, nem teljes adag.
     *
     * Az „egy falat csoki" huszonöt grammnak, az „egy harapás hamburger"
     * egy egész burgernek (250 g) számított – pont annál, aki azt írja le,
     * hogy alig evett.
     */
    @Test public void aBiteIsABiteNotAWholePortion() {
        assertEquals(15.0, hits("egy falat csokit ettem csak")
                .get(0).grams, 0.001);
        assertEquals(15.0, hits("egy harapás hamburgert kaptam")
                .get(0).grams, 0.001);
        assertEquals(30.0, hits("két falat sajttorta").get(0).grams, 0.001);
        // A falatozó vendéglő: a gyros étel marad, a falat-mérték nem
        // ragad rá (a gramm 0 = a hívó tölti a tipikus adaggal).
        assertEquals("Gyros", hits("falatozóban ebédeltem, gyros tál")
                .get(0).food.name);
        assertEquals(0.0, hits("falatozóban ebédeltem, gyros tál")
                .get(0).grams, 0.001);
    }
    /**
     * A BCAA turmix aminosav-ital, nem gyümölcsturmix.
     *
     * A „BCAA turmix edzés közben" mellé egy ötszáz grammos smoothie
     * került – pár kalória helyett több száz. A banános turmix marad.
     */
    @Test public void aBcaaShakeIsNotAFruitSmoothie() {
        for (Foods.Hit h : hits("BCAA turmix edzés közben, 500 ml"))
            assertFalse(h.food.name.startsWith("Gyümölcsturmix"));
        boolean smoothie = false;
        for (Foods.Hit h : hits("banános turmix reggelire"))
            if (h.food.name.startsWith("Gyümölcsturmix")) smoothie = true;
        assertTrue(smoothie);
    }
    /**
     * A tábor belsejében lakó bor nem ital.
     *
     * Az „edzőtábor: napi 2 edzés" és a „sítábor egész héten" mellé eddig
     * másfél deci bor került a naplóba. A megivott bor marad.
     */
    @Test public void aTrainingCampIsNotWine() {
        assertTrue(hits("edzőtábor: napi 2 edzés 4 napon át").isEmpty());
        assertEquals("Bor (vörös/fehér)", hits("sítáborban voltunk, "
                + "bort is ittunk este").get(0).food.name);
    }
    /**
     * A fordított makró-sor zsírja sem olaj.
     *
     * A „fehérje 120 g, szénhidrát 180 g, zsír 60 g" hatvan grammja hatvan
     * gramm OLAJKÉNT került be – ötszáz fantom-kalória. A konyhai zsír
     * makró-szavak nélkül étel marad.
     */
    @Test public void aReversedMacroLineIsNotOil() {
        assertTrue(hits("fehérje 120 g, szénhidrát 180 g, zsír 60 g")
                .isEmpty());
        assertEquals("Olaj", hits("100 g zsír a rántáshoz").get(0).food.name);
    }
    /** A buddha bowl ugyanaz a műfaj, mint a poke bowl. */
    @Test public void aBuddhaBowlIsABowl() {
        assertEquals("Poke bowl", hits("buddha bowl falafellel")
                .get(0).food.name);
    }
    /**
     * A zöldséglé ital, a zellerleves étel.
     *
     * A léböjtös „zöldséglé 3x" kétszáz gramm párolt zöldségnek számított.
     * A zellerlé szándékosan nem stem: a zellerLEVES belsejében is ott
     * lenne a töve, és az ebéd levese pohár lévé válna.
     */
    @Test public void vegetableJuiceIsAJuiceNotSteamedVeg() {
        assertEquals("Gyümölcslé", hits("léböjt 2. nap, zöldséglé 3x")
                .get(0).food.name);
        assertEquals("Leves (átlag)", hits("zellerlevest ettem ebédre")
                .get(0).food.name);
    }
    /**
     * A szüret betakarítás, nem uzsonna.
     *
     * A „leszedtük a diót a fáról" harminc gramm dióként került a
     * naplóba. A megevett dió marad.
     */
    @Test public void harvestingIsNotEating() {
        assertTrue(hits("leszedtük a diót a fáról").isEmpty());
        assertEquals("Dió", hits("megettem egy marék diót")
                .get(0).food.name);
    }
    /** A Body Combat óra nem csirkecomb – az ebéd combja marad. */
    @Test public void bodyCombatIsNotAChickenThigh() {
        assertTrue(hits("les mills bodycombat 55 perc").isEmpty());
        assertEquals("Csirkecomb", hits("csirkecombot sütöttem ebédre")
                .get(0).food.name);
    }
    /**
     * Az ünnepi asztal szavai a saját ételükre esnek.
     *
     * A „baracklekvár" barackle-kezdete fél liter GYÜMÖLCSLÉT írt be, a
     * „rakott palacsinta" rakott krumplinak, a szüreti must nulla
     * kalóriás víznek, a toroskáposzta párolt köretnek számított, a
     * „barackleves" pedig pohár lének.
     */
    @Test public void holidayFoodsResolveToThemselves() {
        assertEquals("Lekvár", hits("farsangi fánk 2 db baracklekvárral")
                .get(1).food.name);
        assertEquals("Palacsinta", hits("névnapomra rakott palacsintát "
                + "sütöttek").get(0).food.name);
        assertEquals("Gyümölcslé", hits("szüreti mustot ittam 2 dl-t")
                .get(0).food.name);
        assertEquals("Töltött káposzta", hits("toroskáposzta és hurka")
                .get(0).food.name);
        assertEquals("Gyümölcsleves", hits("barackleves hidegen")
                .get(0).food.name);
        assertEquals("Mustár", hits("mustáros virsli").get(0).food.name);
    }
    /**
     * A név utáni ragozott szelet-szám is darab.
     *
     * A „tegnapi pizzából ettem 2 szeletet" a teljes pizza-adagot (300 g)
     * kapta: a szám és az étel közé az étel ragja és az evés-ige
     * ékelődött, a „szeletet" ragos alakot pedig nem ismerte a darabszó-
     * lista.
     */
    @Test public void slicesAfterTheFoodNameStillCount() {
        assertEquals(200.0, hits("tegnapi pizzából ettem 2 szeletet")
                .get(0).grams, 0.001);
        assertEquals(105.0, hits("kenyérből 3 szeletet vajaztam meg")
                .get(0).grams, 0.001);
    }
    /** A Cerbona müzliszelet, a Pöttyös túró rudi. */
    @Test public void hungarianSnackBrandsResolve() {
        assertEquals("Müzliszelet", hits("Cerbona szelet edzés előtt")
                .get(0).food.name);
        assertEquals("Túró rudi", hits("Pöttyös óriás guru")
                .get(0).food.name);
    }
    /**
     * A nyersen mért köret főve két és félszer nehezebb.
     *
     * A „100 g rizs nyersen" a főtt rizs kalóriájával százra számolva a
     * HARMADÁT adta a valódi bevitelnek. A főtt mérés és a nyers zöldség
     * marad.
     */
    @Test public void rawMeasuredGrainsConvertToCookedWeight() {
        assertEquals(250.0, hits("100 g rizs nyersen mérve")
                .get(0).grams, 0.001);
        assertEquals(150.0, hits("60 g száraztészta főzve")
                .get(0).grams, 0.001);
        assertEquals(200.0, hits("200 g főtt rizs a bowlba")
                .get(0).grams, 0.001);
        assertEquals(100.0, hits("100 g nyers répa rágcsálva")
                .get(0).grams, 0.001);
    }
    /**
     * A cukorteszt laborvizsgálat, a vaspótlás étrend-kiegészítő.
     *
     * A „terhességi cukorteszt után ettem" mellé tíz gramm cukor került;
     * a „vaspótlás miatt vas + C" üresen jött vissza. A kockacukor marad.
     */
    @Test public void aGlucoseTestIsNotEatenSugar() {
        for (Foods.Hit h : hits("terhességi cukorteszt után ettem egy "
                + "szendvicset"))
            assertFalse(h.food.name.equals("Cukor"));
        assertEquals("Étrend-kiegészítő", hits("vaspótlás miatt vas + C "
                + "reggel").get(0).food.name);
        assertEquals("Cukor", hits("kockacukrot tettem a kávéba")
                .get(0).food.name);
    }
    /**
     * Az egész pizza két adag, a bográcsos gulyás.
     *
     * Az „az egész pizzát megettem egyedül" egyetlen szokásos adagként
     * (300 g) ment be; a „túlettem magam a bográcsosból" üresen jött
     * vissza. A másnak főzött vacsora továbbra sem étkezés.
     */
    @Test public void eatingTheWholeThingDoublesThePortion() {
        assertEquals(600.0, hits("az egész pizzát megettem egyedül")
                .get(0).grams, 0.001);
        assertEquals("Gulyásleves", hits("túlettem magam a bográcsosból")
                .get(0).food.name);
        assertTrue(hits("az egész családnak főztem vacsorát").isEmpty());
    }
    @Test public void aMilkyCocoaIsOneDrink() {
        // „ittam egy tejes kakaót" – a tej benne van a kakaóban,
        // nem szabad külön pohár tejet is naplózni mellé.
        List<Foods.Hit> h = hits("ittam egy tejes kakaót");
        assertEquals(1, h.size());
        assertEquals("Kakaó (tejes)", h.get(0).food.name);
        // A megszórt tejbegríz kakaója nem ital.
        h = hits("menzai tejbegríz szórt kakaóval");
        assertEquals(1, h.size());
        assertEquals("Tejbegríz", h.get(0).food.name);
        // Külön felsorolva viszont mindkettő marad.
        h = hits("tejet ittam és kakaót is");
        assertEquals(2, h.size());
    }

    @Test public void aPintOfIpaIsABeer() {
        // Az „egy pint IPA-t ittam" eddig üresen jött vissza – a kocsmai
        // kézműves sör is sör, a pint nagyjából korsónyi.
        List<Foods.Hit> h = hits("egy pint IPA-t ittam a kocsmában");
        assertEquals(1, h.size());
        assertEquals("Sör", h.get(0).food.name);
        assertEquals(500, h.get(0).grams, 0.5);
        // Az „ipari" nem ital.
        h = hits("ipari mennyiségű tésztát ettem");
        assertEquals("Tészta (főtt)", h.get(0).food.name);
    }

    @Test public void aShotIsSpiritAndTheWifeIsNot() {
        // A „két felest ittunk" két kupica tömény; a „feleségem főzött"
        // viszont nem ital.
        List<Foods.Hit> h = hits("két felest ittunk a szülinapon");
        assertEquals("Pálinka / tömény", h.get(0).food.name);
        assertEquals(80, h.get(0).grams, 0.5);
        assertTrue(hits("a feleségem főzött vacsorát").isEmpty());
    }

    @Test public void thePossessiveHalfIsHalfAPortion() {
        // A „megettem a pizza felét" fél pizza, nem egész; a „feleztünk
        // egy pizzát" fejenként fél.
        assertEquals(150, hits("megettem a pizza felét").get(0).grams, 0.5);
        assertEquals(150, hits("feleztünk egy pizzát").get(0).grams, 0.5);
        // A „hat felé értem haza" nem fél hatos étel.
        List<Foods.Hit> h = hits("hat felé értem haza és ettem egy szendvicset");
        assertEquals(1, h.size());
        assertEquals("Szendvics", h.get(0).food.name);
    }

    @Test public void theLiquidMeasureBelongsToTheLiquid() {
        // A „zabkása fél liter tejjel" öt deci TEJET mond – mégis fél
        // kiló zabpehely lett belőle, a tej meg alapadag maradt.
        List<Foods.Hit> h = hits("zabkása fél liter tejjel");
        assertEquals("Zabpehely", h.get(0).food.name);
        assertTrue("zab: " + h.get(0).grams, h.get(0).grams <= 100);
        assertEquals("Tej", h.get(1).food.name);
        assertEquals(500, h.get(1).grams, 0.5);
        // A szilárd étel grammja viszont marad hátrakötve.
        h = hits("tészta 100 g sonkával");
        assertEquals(100, h.get(0).grams, 0.5);
    }

    @Test public void aPairOfSausagesIsTwoSausages() {
        // A „virsli 2 pár mustárral" négy szál virsli, nem kettő.
        List<Foods.Hit> h = hits("virsli 2 pár mustárral");
        assertEquals("Virsli", h.get(0).food.name);
        assertEquals(200, h.get(0).grams, 0.5);
        assertEquals(100, hits("egy pár virslit ettem").get(0).grams, 0.5);
        // A „pár szem szőlő" határozatlan párja nem darabszám.
        assertEquals("Szőlő", hits("pár szem szőlőt ettem").get(0).food.name);
    }

    @Test public void aStuffedDishIsNotTheDishPlusItsFilling() {
        // A „burrito marhahússal" egy burrito – a hús benne van, eddig
        // külön szelet marha is került mellé.
        List<Foods.Hit> h = hits("burrito marhahússal");
        assertEquals(1, h.size());
        assertEquals("Burrito", h.get(0).food.name);
        // A gong bao csirke teljes fogás, nem puszta csirkemell.
        h = hits("a gong bao csirkét ettem");
        assertEquals(1, h.size());
        assertEquals("Kínai bundás csirke", h.get(0).food.name);
        // A „naan kenyérrel" egyetlen pékáru.
        h = hits("indiai csirke curry naan kenyérrel");
        assertEquals(2, h.size());
        // A külön álló marha rizzsel viszont két tétel marad.
        assertEquals(2, hits("marhahús rizzsel").size());
    }

    @Test public void aTasteIsABiteNotAFullPortion() {
        // A „csak megkóstoltam a sütit" száz gramm sütemény volt, a
        // „belekóstoltam a levesbe" négy deci leves – a kóstolás egy falat.
        assertEquals(15, hits("csak megkóstoltam a sütit").get(0).grams, 0.5);
        assertEquals(15, hits("belekóstoltam a levesbe").get(0).grams, 0.5);
    }

    @Test public void cookingForTheWeekIsNotTodaysMeal() {
        // A „főztem egy nagy fazék gulyást a hétre" előkészület, a
        // „megsült a kenyerem" még nem falat. Az evés igéje viszont felment.
        assertTrue(hits("főztem egy nagy fazék gulyást a hétre").isEmpty());
        assertTrue(hits("megsült a kenyerem a sütőben").isEmpty());
        assertEquals("Gulyásleves",
                hits("főztem gulyást és ettem is egy tányérral").get(0).food.name);
        assertEquals("Csirkemell (sült/grill)",
                hits("sütőben sült csirkét ettem rizzsel").get(0).food.name);
    }

    @Test public void proteinContentDoesNotShrinkTheShake() {
        // A „fehérjeturmix edzés után 30 g fehérjével" turmixa harminc
        // grammos itallá zsugorodott – a tartalom nem az adag.
        List<Foods.Hit> h = hits("fehérjeturmix edzés után 30 g fehérjével");
        assertEquals("Protein turmix", h.get(0).food.name);
        // A nulla gramm azt mondja: nincs kimondott adag, az alap (300 g)
        // érvényes – eddig a harminc gramm tartalom kötött ide.
        assertEquals(0, h.get(0).grams, 0.5);
        assertEquals(300, h.get(0).food.portion, 0.5);
        // A kimondott turmix-mennyiség viszont marad.
        assertEquals(300, hits("300 g fehérjeturmixot ittam").get(0).grams, 0.5);
    }

    @Test public void theFoodBeforeInsteadOfWasNotEaten() {
        // A „kávézacc helyett koffein tabletta" kávét írt a naplóba,
        // pedig épp az maradt el.
        List<Foods.Hit> h = hits("kávézacc helyett koffein tabletta");
        assertEquals(1, h.size());
        assertEquals("Étrend-kiegészítő", h.get(0).food.name);
        h = hits("rizs helyett bulgurt ettem");
        assertEquals(1, h.size());
        assertEquals("Bulgur (főtt)", h.get(0).food.name);
    }

    @Test public void theDailyWaterTotalCounts() {
        // A „vizet ittam, 2 liter összejött mára" két litere elveszett –
        // az ige beszédessé tette a tiszta mennyiség-tagmondatot. A
        // „hidratálás pipa, 3 liter" pedig üresen jött vissza.
        assertEquals(2000, hits("vizet ittam, 2 liter összejött mára")
                .get(0).grams, 0.5);
        List<Foods.Hit> h = hits("hidratálás pipa, 3 liter");
        assertEquals("Víz / ásványvíz", h.get(0).food.name);
        assertEquals(3000, h.get(0).grams, 0.5);
    }

    @Test public void aLittleNutIsAHandfulNotASinglePiece() {
        // A „nassoltam egy kis mogyorót" egy grammként ment be – egyetlen
        // szemként. Az „egy kis" nem darabszám, a „pár falat" két falat.
        Foods.Hit nut = hits("nassoltam egy kis mogyorót").get(0);
        // A nulla gramm alapadagot jelent – az a maréknyi, nem egy szem.
        assertEquals(0, nut.grams, 0.5);
        assertEquals(30, nut.food.portion, 0.5);
        assertEquals(30, hits("ettem pár falatot a tortából").get(0).grams, 0.5);
    }

    @Test public void theGymPulleyIsNotAPastry() {
        // A „letolás csigán 3x12" kakaós csigát írt a naplóba – a ragozás
        // árulja el: a süteményt „csigát" esszük, nem „csigán".
        assertTrue(hits("letolás csigán 3x12 25 kg").isEmpty());
        assertEquals("Kakaós csiga",
                hits("ettem egy csigát a pékből").get(0).food.name);
    }

    @Test public void theEndOfCardioIsNotAWalnut() {
        // A „cardio 20m + súlyok 40m" mellé harminc gramm dió került – a
        // szó vége nem dió. Az igazi dió marad.
        assertTrue(hits("cardio 20m + súlyok 40m").isEmpty());
        assertEquals("Dió", hits("ettem 30 g diót").get(0).food.name);
    }

    @Test public void soletAndHortobagyiResolveCleanly() {
        // A sólet eddig hiányzott, a hortobágyi húsos palacsinta mellé
        // pedig egy sima palacsinta is került.
        assertEquals("Sólet", hits("sólet füstölt tarjával").get(0).food.name);
        List<Foods.Hit> h = hits("hortobágyi húsos palacsinta");
        assertEquals(1, h.size());
        assertEquals("Hortobágyi palacsinta", h.get(0).food.name);
        assertEquals("Palacsinta",
                hits("palacsintát ettem lekvárral").get(0).food.name);
    }


    @Test public void dekaAdjectivesAndEatenHalvesScale() {
        // A „harmincdekás steak" alapadagként ment be háromszáz gramm
        // helyett; a „20 deka párizsit vettem és megettem a felét" pedig
        // a teljes kétszáz grammot írta be száz helyett.
        assertEquals(300, hits("harmincdekás steak").get(0).grams, 0.5);
        assertEquals(100, hits("20 deka párizsit vettem és megettem a felét")
                .get(0).grams, 0.5);
    }

    @Test public void freeFromProductsResolveSensibly() {
        // A „zsírszegény tejföl" tej PLUSZ tejföl volt; az alkoholmentes
        // sör teljes sörként számított. (A cukormentes rágó szándékosan a
        // nulla kalóriás gyűjtőbe megy – azt a fitnesz-teszt őrzi.)
        List<Foods.Hit> h = hits("zsírszegény tejföl a levesbe");
        assertEquals("Tejföl", h.get(0).food.name);
        assertEquals("Alkoholmentes sör",
                hits("alkoholmentes sört ittam").get(0).food.name);
        assertEquals("Sör", hits("sört ittam a meccs alatt").get(0).food.name);
    }

    @Test public void aRowHouseIsNotABeer() {
        // A „sorház utcában találkoztunk" egy korsó sört írt a naplóba.
        // A sörházban MEGIVOTT sör viszont marad.
        assertTrue(hits("sorház utcában találkoztunk").isEmpty());
        assertEquals(1000, hits("a sörházban megittunk két sört")
                .get(0).grams, 0.5);
    }

    @Test public void fastFoodMenusAndFriesResolve() {
        // A „mekis menüt ettem sajtburgerrel" menü PLUSZ burger volt; a
        // „big mac menü nagy krumplival" főtt burgonyát írt a sült
        // krumpli helyett. A pörkölt főtt krumplija marad.
        List<Foods.Hit> h = hits("mekis menüt ettem sajtburgerrel");
        assertEquals(1, h.size());
        assertEquals("Gyorséttermi menü", h.get(0).food.name);
        h = hits("big mac menü nagy krumplival és kólával");
        assertEquals("Sült krumpli", h.get(1).food.name);
        assertEquals("Burgonya (főtt)",
                hits("főtt krumpli pörkölthöz").get(0).food.name);
    }

    @Test public void swimmingWaterIsNotADrink() {
        // A „20 fokos vízben úsztam fél órát" mellé egy pohár ásványvíz
        // került. Az úszás utáni megivott pohár víz viszont marad.
        assertTrue(hits("a 20 fokos vízben úsztam fél órát").isEmpty());
        assertEquals("Víz / ásványvíz",
                hits("úsztam és utána ittam egy pohár vizet").get(0).food.name);
    }

    @Test public void aBeltExamHasNoWaterInIt() {
        // Az övvizsga közepén a víz szótöve ül – pohár ásványvíz lett a
        // karate-vizsgából.
        assertTrue(hits("övvizsga volt karatéból, sikerült").isEmpty());
    }

    @Test public void theDanceSchoolHasNoGooseInIt() {
        // A „táncSULIBAn" hátulján a liba szótöve ül – kacsasült került a
        // cha-cha óra mellé. Az igazi libacomb marad.
        assertTrue(hits("keringő és cha-cha a táncsuliban").isEmpty());
        assertEquals("Kacsa / liba",
                hits("libacombot ettem párolt káposztával").get(0).food.name);
    }

    @Test public void raceNutritionResolves() {
        // Az energiagél, az izo ital és a szőlőcukor eddig hiányzott –
        // a szőlőcukorból ráadásul száz gramm SZŐLŐ lett.
        assertEquals(64, hits("két energiagél ment el a hosszú futáson")
                .get(0).grams, 0.5);
        assertEquals("Sportital / izotóniás",
                hits("izo italt ittam a félmaratonon").get(0).food.name);
        assertEquals("Szőlőcukor",
                hits("szőlőcukrot ettem a frissítőponton").get(0).food.name);
        assertEquals("Szőlő", hits("szőlőt ettem uzsonnára").get(0).food.name);
    }

    @Test public void blendingIsPreparationNotAnExtraDrink() {
        // A „mogyoróvaj banánnal turmixolva" mellé egy teljes smoothie is
        // került a hozzávalókon felül. A megivott turmix marad.
        List<Foods.Hit> h = hits("mogyoróvaj banánnal turmixolva");
        assertEquals(2, h.size());
        assertTrue(hits("banán turmixot ittam").stream()
                .anyMatch(x -> x.food.name.startsWith("Gyümölcsturmix")));
    }

    @Test public void bakeryAdjectivesAndDumplingsResolve() {
        // A „kakaós kalács" mellé pohár kakaó került; a szilvás gombóc
        // darabja nem volt meg; a pozsonyi kifli sima kifliként ment be.
        List<Foods.Hit> h = hits("kakaós kalács a pékségből");
        assertEquals(1, h.size());
        assertEquals("Kalács / bejgli", h.get(0).food.name);
        assertEquals(480, hits("ettem 6 szilvás gombócot").get(0).grams, 0.5);
        assertEquals("Kalács / bejgli",
                hits("pozsonyi kifli diósan").get(0).food.name);
        // A kakaós csiga és a tejes kakaó marad.
        assertEquals("Kakaós csiga", hits("kakaós csigát ettem").get(0).food.name);
        assertEquals("Kakaó (tejes)", hits("tejes kakaót ittam").get(0).food.name);
    }

    @Test public void anEffervescentTabletIsNotChampagne() {
        // A „C-vitamin pezsgőtabletta vízben oldva" mellé másfél deci
        // pezsgő került. A szilveszteri pezsgő marad.
        for (Foods.Hit h : hits("c-vitamin pezsgőtabletta vízben oldva"))
            assertFalse(h.food.name.equals("Pezsgő"));
        assertEquals("Pezsgő",
                hits("pezsgőt ittunk szilveszterkor").get(0).food.name);
    }

    @Test public void aSetRowIsNotABeer() {
        // A „gorilla sor: 5x5 fekvenyomás 100 kg" mellé fél liter sör
        // került. Az edzés utáni MEGIVOTT sör marad.
        assertTrue(hits("gorilla sor: 5x5 fekvenyomás 100 kg").isEmpty());
        assertEquals("Sör",
                hits("3x10 guggolás után megittam egy sört").get(0).food.name);
    }

    @Test public void halloumiAndBogracsResolve() {
        // A halloumit a „hall" tiltó-prefix elnyelte, a bográcsozás igéje
        // pedig hiányzott. A hallott recept továbbra sem étel.
        assertEquals("Camembert / brie",
                hits("grillezett zöldségek halloumival").get(1).food.name);
        assertEquals("Gulyásleves",
                hits("bográcsoztunk a kertben").get(0).food.name);
        assertTrue(hits("hallottam egy jó receptet halból").isEmpty());
    }

    @Test public void punchIsAWinterDrink() {
        // A puncs (és a hasonult „punccsal" alak) eddig hiányzott.
        assertEquals("Koktél / long drink",
                hits("puncsot ittam a vásárban").get(0).food.name);
        assertEquals("Koktél / long drink",
                hits("punccsal melegedtünk a korcsolyapályánál").get(0).food.name);
    }

    @Test public void harvestedPotatoesAreNotDinner() {
        // A többes számú szedés betakarítás – a vitamint szedő egyes szám
        // és a megevett krumpli marad.
        assertTrue(hits("krumplit szedtünk a földön 4 órát").isEmpty());
        assertEquals("Étrend-kiegészítő",
                hits("magnéziumot szedtem este").get(0).food.name);
        assertEquals("Burgonya (főtt)",
                hits("főtt krumplit ettem pörkölttel").get(0).food.name);
    }

    @Test public void hotChocolateIsADrinkNotABar() {
        // A „melegcsoki a hidegben" 25 gramm táblás csoki lett – a
        // melegcsoki két és fél deci kakaó. A tábla csoki marad.
        assertEquals("Kakaó (tejes)",
                hits("melegcsoki a hidegben").get(0).food.name);
        assertEquals("Csokoládé",
                hits("ettem egy tábla csokit").get(0).food.name);
    }

    @Test public void pieceCountMultipliesThePerPieceWeight() {
        List<Foods.Hit> h = hits("kaja: 2 db 300 g-os pizza");
        assertEquals(1, h.size());
        assertEquals("Pizza", h.get(0).food.name);
        assertEquals(600, h.get(0).grams, 0.01);
    }

    @Test public void dekaPerPieceWeightAlsoMultiplies() {
        List<Foods.Hit> h = hits("vacsora: 2 db 30 dkg-os pizza");
        assertEquals(600, h.get(0).grams, 0.01);
    }

    @Test public void recipeSpoonAbbreviationIsATablespoon() {
        List<Foods.Hit> h = hits("3 ek oliv\u00e1olaj a sal\u00e1t\u00e1ba");
        assertEquals("Olaj", h.get(0).food.name);
        assertEquals(30, h.get(0).grams, 0.01);
    }

    @Test public void teaspoonAbbreviationIsATeaspoon() {
        List<Foods.Hit> h = hits("2 tk cukorral ittam a k\u00e1v\u00e9t");
        assertEquals("Cukor", h.get(0).food.name);
        assertEquals(20, h.get(0).grams, 0.01);
    }

    @Test public void ekkoraIsNotASpoon() {
        assertTrue(hits("ekkora adag rizst m\u00e9g nem ettem").isEmpty());
    }

    @Test public void visceralFatIsAScaleRowNotLard() {
        assertTrue(hits("viszcer\u00e1lis zs\u00edr 9").isEmpty());
    }

    @Test public void semmiNegatesTheFoodAfterIt() {
        assertTrue(hits("mentes nap volt, semmi cukor").isEmpty());
        List<Foods.Hit> h = hits("ettem pizz\u00e1t, de semmi \u00e9dess\u00e9g");
        assertEquals(1, h.size());
        assertEquals("Pizza", h.get(0).food.name);
    }

    @Test public void blendingWithLePrefixIsAlsoJustMixing() {
        List<Foods.Hit> h = hits("leturmixoltam egy ban\u00e1nt tejjel");
        assertEquals(2, h.size());
        assertEquals("Ban\u00e1n", h.get(0).food.name);
    }

    @Test public void aTextualHalfOfAWholePizzaIsHalfAPortion() {
        List<Foods.Hit> h = hits("nem ettem meg az eg\u00e9sz pizz\u00e1t, csak a fel\u00e9t");
        assertEquals(1, h.size());
        assertEquals("Pizza", h.get(0).food.name);
        assertEquals(150, h.get(0).grams, 0.01);
    }

    @Test public void aHosszulepesIsAFroccs() {
        List<Foods.Hit> h = hits("hossz\u00fal\u00e9p\u00e9s a kertben");
        assertEquals(1, h.size());
        assertEquals("Fr\u00f6ccs", h.get(0).food.name);
    }

    @Test public void bodyWaterPercentIsNotAGlassOfWater() {
        assertTrue(hits("m\u00e9rleg 78,8 / v\u00edz 55%").isEmpty());
    }

    @Test public void aFlavouredCerealBarIsOneItem() {
        List<Foods.Hit> h = hits("kekszes-mogyor\u00f3s m\u00fczliszelet");
        assertEquals(1, h.size());
        assertEquals("M\u00fczliszelet", h.get(0).food.name);
    }

    @Test public void pringlesIsChips() {
        List<Foods.Hit> h = hits("pringles f\u00e9l doboz");
        assertEquals("Chips", h.get(0).food.name);
    }

    @Test public void foodEmojiCountAsFoods() {
        List<Foods.Hit> h = hits("\ud83c\udf55 2 szelet");
        assertEquals("Pizza", h.get(0).food.name);
        assertEquals(200, h.get(0).grams, 0.01);
        assertEquals("K\u00e1v\u00e9 (fekete)",
                hits("\u2615 \u00e9s egy croissant").get(0).food.name);
    }

    @Test public void myFatPercentIsNotLard() {
        assertTrue(hits("zs\u00edrom 18 sz\u00e1zal\u00e9k").isEmpty());
    }

    @Test public void theNextClauseLitresBelongToTheWater() {
        List<Foods.Hit> h = hits("ittam sok vizet, kb 2,5 litert");
        assertEquals(2500, h.get(0).grams, 0.01);
    }

    @Test public void dancingIsNotACola() {
        assertTrue(hits("vacsora \u00e9s t\u00e1ncol\u00e1s hajnalig").isEmpty());
        assertEquals("\u00dcd\u00edt\u0151 (cukros)", hits("ittam egy kol\u00e1t").get(0).food.name);
    }

    @Test public void roastMeatIsAFoodOfItsOwn() {
        assertEquals("Sert\u00e9skaraj", hits("s\u00fclt h\u00fas").get(0).food.name);
    }

    @Test public void cookedAheadPortionsAreNotTodaysMeal() {
        assertTrue(hits("el\u0151re f\u0151zve: 5 doboz csirke rizzsel").isEmpty());
        assertFalse(hits("el\u0151re f\u0151zve van a csirke, ettem egy adagot").isEmpty());
    }

    @Test public void tokajiAszuIsWine() {
        assertEquals("Bor (v\u00f6r\u00f6s/feh\u00e9r)",
                hits("tokaji asz\u00fa egy poh\u00e1r").get(0).food.name);
        assertEquals("Aszalt gy\u00fcm\u00f6lcs", hits("aszalt szilva").get(0).food.name);
    }

    @Test public void aFatPercentIsNotCookingFat() {
        assertTrue(hits("zs\u00edr 18%").isEmpty());
    }

    @Test public void portionAdjectivesScaleThePortion() {
        assertEquals(375, hits("nagy adag t\u00e9szta").get(0).grams, 0.01);
        assertEquals(100, hits("kis adag rizs").get(0).grams, 0.01);
        assertEquals(800, hits("dupla adag guly\u00e1s").get(0).grams, 0.01);
    }

    @Test public void aLayeredPastaIsNotLayeredPotato() {
        List<Foods.Hit> h = hits("h\u00fasos rakott t\u00e9szta");
        assertEquals(1, h.size());
        assertEquals("T\u00e9szta (f\u0151tt)", h.get(0).food.name);
        assertEquals("Rakott krumpli",
                hits("rakott krumpli vacsor\u00e1ra").get(0).food.name);
    }

    @Test public void aSipIsASipEvenInTheNyiForm() {
        assertEquals(40, hits("ittam egy kortynyi vizet").get(0).grams, 0.01);
    }

    @Test public void aBareFelesIsAShotOfPalinka() {
        List<Foods.Hit> h = hits("k\u00e9t feles \u00e9s egy s\u00f6r");
        assertEquals(2, h.size());
        assertEquals("P\u00e1linka / t\u00f6m\u00e9ny", h.get(0).food.name);
        assertEquals(80, h.get(0).grams, 0.01);
        assertTrue(hits("felesleges volt az eg\u00e9sz").isEmpty());
    }

    @Test public void aSharedDishIsSplitBetweenTheEaters() {
        List<Foods.Hit> h = hits("megosztottunk egy pizz\u00e1t ketten");
        assertEquals(1, h.size());
        assertEquals(150, h.get(0).grams, 0.01);
        assertEquals(150, hits("ketten ett\u00fck meg a pizz\u00e1t").get(0).grams, 0.01);
    }

    @Test public void beingTwoAtTheCinemaDoesNotHalveThePopcorn() {
        List<Foods.Hit> h = hits("ketten voltunk a moziban, ettem egy popcornt");
        assertEquals(40, h.get(0).grams, 0.01);
    }

    @Test public void englishGymFoodNamesResolve() {
        assertEquals("Zabpehely", hits("porridge reggelire").get(0).food.name);
        assertEquals("Proteinszelet", hits("protein bar edz\u00e9s ut\u00e1n").get(0).food.name);
        assertEquals("Csirkemell (s\u00fclt/grill)", hits("chicken salad").get(0).food.name);
        assertEquals("Csirkecomb", hits("chicken wings").get(0).food.name);
    }

    @Test public void friesIsSideOrderNotPartOfGofri() {
        List<Foods.Hit> h = hits("cheat day: burger \u00e9s fries");
        assertEquals(2, h.size());
        assertEquals("S\u00fclt krumpli", h.get(1).food.name);
        assertEquals("Gofri", hits("gofri tejsz\u00ednhabbal").get(0).food.name);
    }

    @Test public void aBeerAfterTheMatchSurvivesASetNotation() {
        List<Foods.Hit> h = hits("hossz\u00fa meccs volt, 2x30 perc k\u00e9zilabda, "
                + "ut\u00e1na 2 s\u00f6r a kocsm\u00e1ban");
        assertEquals(1, h.size());
        assertEquals("S\u00f6r", h.get(0).food.name);
        assertTrue(hits("gorilla sor: 5x5 fekvenyom\u00e1s 100 kg").isEmpty());
    }

    @Test public void takingOutTheTrashIsNotAKiwi() {
        assertTrue(hits("kivittem a szemetet").isEmpty());
        assertEquals("Kivi", hits("ettem egy kivit").get(0).food.name);
    }

    @Test public void whatIsOnlyInTheFridgeIsNotEaten() {
        assertTrue(hits("a h\u0171t\u0151ben van m\u00e9g sajt").isEmpty());
        assertEquals("Sajt (trappista)", hits("megettem a sajtot").get(0).food.name);
    }

    @Test public void whatTheKidRefusesAndWhatIsBakedForAPartyAreNotMine() {
        assertTrue(hits("a gyerek nem eszi meg a levest").isEmpty());
        assertTrue(hits("s\u00fct\u00f6ttem egy tort\u00e1t a sz\u00fclinapra").isEmpty());
        // A saját fél pizza viszont megevett étel.
        assertEquals(150, hits("nem ettem meg az eg\u00e9sz pizz\u00e1t, csak a fel\u00e9t")
                .get(0).grams, 0.01);
    }

    /**
     * A „cukormentes nap" a NAPRA vonatkozik, nem egy t\u00e1ny\u00e9rra: eddig
     * h\u00e1romsz\u00e1zharminc gramm „cukormentes / light" \u00e9tel lett bel\u0151le.
     */
    @Test
    public void aSugarFreeDayIsNotAFood() {
        assertTrue(hits("cukormentes nap volt, semmi \u00e9dess\u00e9g").isEmpty());
        assertTrue(hits("cukormentes napom van").isEmpty());
    }

    /** A jelz\u0151s \u00e9tel viszont marad \u00e9tel. */
    @Test
    public void aGlutenFreeBreadIsStillBread() {
        assertFalse(hits("glut\u00e9nmentes keny\u00e9r 2 szelet").isEmpty());
    }

    /**
     * A „cukormentes" JELZ\u0150: a „cukormentes csoki 30 g" mell\u00e9 eddig egy
     * eg\u00e9sz \u00fcd\u00edt\u0151nyi adag „cukormentes / light" t\u00e9tel is bement.
     */
    @Test
    public void sugarFreeBeforeARealFoodIsOnlyAnAdjective() {
        java.util.List<Foods.Hit> l = hits("cukormentes csoki 30 g");
        assertEquals(1, l.size());
        assertEquals("Csokol\u00e1d\u00e9", l.get(0).food.name);
        assertEquals(1, hits("cukormentes joghurt 150 g").size());
    }

    /** Saj\u00e1t t\u00e9tel n\u00e9lk\u00fcli \u00e9teln\u00e9l viszont marad a nulla kal\u00f3ri\u00e1s sor. */
    @Test
    public void sugarFreeStaysAnItemOnItsOwn() {
        assertEquals(1, hits("cukormentes r\u00e1g\u00f3").size());
        assertEquals("Cukormentes / light", hits("cukormentes r\u00e1g\u00f3").get(0).food.name);
        assertEquals("Cukormentes / light", hits("cukormentes k\u00f3la").get(0).food.name);
    }

    /**
     * A „B\u0150R\u00d6N S\u00dcLT" nem bor: \u00e9kezetek n\u00e9lk\u00fcl a „b\u0151r" \u00e9s a „bor"
     * ugyanaz a h\u00e1rom bet\u0171, \u00e9s eddig egy poh\u00e1r bor is bement a napl\u00f3ba.
     */
    @Test
    public void skinOnRoastIsNotWine() {
        java.util.List<Foods.Hit> l = hits("s\u00fclt csirkecomb b\u0151r\u00f6n s\u00fclt krumplival");
        for (Foods.Hit h : l) assertFalse(h.food.name.startsWith("Bor"));
        assertEquals(2, l.size());
        // A boros p\u00f6rk\u00f6ltben a bor a szaftban van, nem a poh\u00e1rban.
        for (Foods.Hit h : hits("boros marhap\u00f6rk\u00f6lt"))
            assertFalse(h.food.name.startsWith("Bor"));
        // A val\u00f3di poh\u00e1r viszont marad.
        assertEquals("Bor (v\u00f6r\u00f6s/feh\u00e9r)", hits("ittam k\u00e9t poh\u00e1r bort").get(0).food.name);
    }

    /** A BIRSALMASAJT nem sajt, hanem bes\u0171r\u00edtett gy\u00fcm\u00f6lcs. */
    @Test
    public void quinceCheeseIsNotCheese() {
        java.util.List<Foods.Hit> l = hits("egy szelet birsalmasajt");
        assertEquals(1, l.size());
        assertEquals("Lekv\u00e1r", l.get(0).food.name);
    }

    /**
     * A COMBIZOM-ban a comb: a „megh\u00faztam a combizmomat" mell\u00e9 eddig egy
     * sz\u00e1z\u00f6tven grammos csirkecomb ker\u00fclt a napl\u00f3ba – egy s\u00e9r\u00fcl\u00e9sb\u0151l.
     */
    @Test
    public void aThighMuscleIsNotAChickenThigh() {
        assertTrue(hits("megh\u00faztam a combizmomat").isEmpty());
        assertTrue(hits("f\u00e1j a combizom").isEmpty());
        // A val\u00f3di csirkecomb marad.
        assertFalse(hits("csirkecomb rizzsel").isEmpty());
    }

    /**
     * A K\u00c1V\u00c9BA \u00f6nt\u00f6tt tej nem egy poh\u00e1r tej: az „egy b\u00f6gre k\u00e1v\u00e9 tejjel"
     * mell\u00e9 eddig k\u00e9tdecinyi tej ker\u00fclt – sz\u00e1znegyven kal\u00f3ria egy
     * l\u00f6ttyint\u00e9sb\u0151l.
     */
    @Test
    public void aSplashOfMilkInCoffeeIsNotAGlass() {
        for (Foods.Hit h : hits("egy b\u00f6gre k\u00e1v\u00e9 tejjel"))
            if (h.food.name.equals("Tej")) assertEquals(30.0, h.grams, 0.01);
        for (Foods.Hit h : hits("k\u00e1v\u00e9t ittam tejjel"))
            if (h.food.name.equals("Tej")) assertEquals(30.0, h.grams, 0.01);
        // A poh\u00e1r tej marad poh\u00e1r tej.
        assertEquals(200.0, hits("ittam 2 dl tejet").get(0).grams, 0.01);
    }

    /** A k\u00fcl\u00f6n \u00edrt „tejes k\u00e1v\u00e9" ugyanaz a tejesk\u00e1v\u00e9. */
    @Test
    public void aSpacedMilkCoffeeIsOneDrink() {
        java.util.List<Foods.Hit> l = hits("tejes k\u00e1v\u00e9");
        assertEquals(1, l.size());
        assertTrue(l.get(0).food.name, l.get(0).food.name.startsWith("Tejesk\u00e1v\u00e9"));
    }

    /**
     * \u00d6t bet\u0171t\u0151l is j\u00e1r elg\u00e9pel\u00e9s-tipp: a „kefri" \u00e9s a „banna" ugyanolyan
     * egy-ujjmozdulatos el\u00fct\u00e9s, mint a „joghrut" – hat bet\u0171s korl\u00e1ttal
     * viszont az \u00f6tbet\u0171s \u00e9telek kimaradtak a tippekb\u0151l.
     */
    @Test
    public void fiveLetterTyposGetATipToo() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals("Kefir", Foods.closest(all, "kefri").name);
        assertEquals("Ban\u00e1n", Foods.closest(all, "banna").name);
        assertEquals("Joghurt", Foods.closest(all, "joghrut").name);
        // A h\u00e9tk\u00f6znapi szavak tov\u00e1bbra sem kapnak tippet – \u00f6t bet\u0171n\u00e9l a
        // CSER\u00c9LT bet\u0171 t\u00fal k\u00f6zel visz: a „hasam" a has\u00e1b, a „fogam" a fogas,
        // a „l\u00e1zas" a lazac egy hib\u00e1nyira van.
        for (String w : new String[]{"valami", "asztal", "szoba", "fotel",
                "tan\u00e1r", "orvos", "hasam", "fogam", "l\u00e1zas", "karom", "t\u00e9rdem"})
            assertTrue(w, Foods.closest(all, w) == null);
    }

    /**
     * Egyetlen sportn\u00e9v \u00e9s rehab-gyakorlatn\u00e9v se hozzon l\u00e9tre \u00e9tel-t\u00e9telt.
     *
     * A keresztpr\u00f3ba h\u00e1rom \u00fctk\u00f6z\u00e9st tal\u00e1lt: a „ny\u00edltv\u00edzi" \u00fasz\u00e1s mell\u00e9
     * \u00e1sv\u00e1nyv\u00edz, a „krumplit szed" kerti munka mell\u00e9 f\u0151tt burgonya, a
     * „Kagyl\u00f3 (clamshell)" rehab-gyakorlat mell\u00e9 tenger gy\u00fcm\u00f6lcsei ker\u00fclt.
     */
    @Test
    public void noSportOrRehabNameIsAFood() {
        StringBuilder bad = new StringBuilder();
        for (Activities.Kind k : Activities.ALL)
            for (String w : k.words) {
                if (w.length() < 4) continue;
                if (!Foods.parse(DB, w).isEmpty())
                    bad.append("\n  ").append(k.id).append(" | ").append(w);
            }
        for (Rehab.Area a : Rehab.AREAS)
            for (Rehab.Ex e : a.moves)
                if (!Foods.parse(DB, e.name).isEmpty())
                    bad.append("\n  ").append(a.id).append(" | ").append(e.name);
        assertTrue("\u00e9telnek l\u00e1tszik:" + bad, bad.length() == 0);
    }

    /**
     * A R\u00c9TES t\u00f6ltel\u00e9ke benne van a t\u00e9szt\u00e1ban: a „k\u00e1poszt\u00e1s r\u00e9tes" mell\u00e9
     * eddig m\u00e1sf\u00e9l deka nyers k\u00e1poszta is beker\u00fclt, a „t\u00far\u00f3s r\u00e9tes" mell\u00e9
     * egy adag t\u00far\u00f3.
     */
    @Test
    public void aStrudelsFillingIsInTheStrudel() {
        for (String q : new String[]{"k\u00e1poszt\u00e1s r\u00e9tes", "t\u00far\u00f3s r\u00e9tes",
                "alm\u00e1s r\u00e9tes"}) {
            java.util.List<Foods.Hit> l = hits(q);
            assertEquals(q, 1, l.size());
            assertEquals(q, "R\u00e9tes", l.get(0).food.name);
        }
        // A k\u00fcl\u00f6n t\u00e1lalt t\u00f6ltel\u00e9k marad \u00f6n\u00e1ll\u00f3.
        assertEquals("T\u00far\u00f3", hits("t\u00far\u00f3 200 g").get(0).food.name);
    }

    /**
     * A V\u00cdZ H\u0150FOKA nem elfogyasztott v\u00edz: az „\u00fasz\u00e1s 30 perc; v\u00edz 20 fok"
     * mell\u00e9 eddig k\u00e9t \u00e9s f\u00e9l deci iv\u00f3v\u00edz ker\u00fclt a napl\u00f3ba.
     */
    @Test
    public void theWaterTemperatureIsNotADrink() {
        assertTrue(hits("\u00fasz\u00e1s 30 perc; v\u00edz 20 fok").isEmpty());
        assertTrue(hits("a v\u00edz 24 fokos volt").isEmpty());
        // A meg\u00edvott v\u00edz marad.
        assertEquals(500.0, hits("ittam 2 poh\u00e1r vizet").get(0).grams, 0.01);
        assertEquals(300.0, hits("v\u00edz 20 fok, ittam 3 dl-t").get(0).grams, 0.01);
    }

    /**
     * A t\u00f6ltve \u00e9rkez\u0151 fog\u00e1s tartalma benne van a fog\u00e1sban: a „wrap
     * csirk\u00e9vel" mell\u00e9 eddig egy csirkemell is beker\u00fclt, a „protein shake
     * tejjel" mell\u00e9 k\u00e9t deci tej.
     */
    @Test
    public void aFilledDishDoesNotDoubleCountItsFilling() {
        java.util.List<Foods.Hit> l = hits("wrap csirk\u00e9vel");
        assertEquals(1, l.size());
        assertEquals("Tortilla / wrap", l.get(0).food.name);
        java.util.List<Foods.Hit> sh = hits("protein shake tejjel");
        assertEquals(1, sh.size());
        assertEquals("Protein turmix", sh.get(0).food.name);
        // A poh\u00e1r tej \u00e9s a k\u00fcl\u00f6n csirkemell marad.
        assertEquals("Tej", hits("ittam 2 dl tejet").get(0).food.name);
        assertEquals(150.0, hits("csirkemell 150 g").get(0).grams, 0.01);
    }

    /**
     * A KORTY az italok falatja: a „3 korty bor" n\u00e9gysz\u00e1z\u00f6tven grammk\u00e9nt
     * ment be – h\u00e1rom poh\u00e1rnyik\u00e9nt.
     */
    @Test
    public void aSipIsNotAGlass() {
        assertEquals(75.0, hits("3 korty bor").get(0).grams, 0.01);
        // A poh\u00e1r \u00e9s a kors\u00f3 marad.
        assertEquals(300.0, hits("k\u00e9t poh\u00e1r bor").get(0).grams, 0.01);
        assertEquals(500.0, hits("egy kors\u00f3 s\u00f6r").get(0).grams, 0.01);
        // A FELESPOH\u00c1R egy sz\u00f3ban is m\u00e9r\u0151sz\u00f3: a „k\u00e9t felespoh\u00e1r whisky"
        // egyetlen felesnek sz\u00e1m\u00edtott.
        assertEquals(80.0, hits("k\u00e9t felespoh\u00e1r whisky").get(0).grams, 0.01);
    }

    /**
     * Ami BELEF\u00c9R a keretbe, azt m\u00e9g nem ett\u00fck meg: a „m\u00e9g 2 szelet
     * pizza belef\u00e9r" k\u00e9tsz\u00e1z gramm pizz\u00e1t \u00edrt a napl\u00f3ba abb\u00f3l,
     * amit a felhaszn\u00e1l\u00f3 \u00e9pp csak m\u00e9rlegel.
     */
    @Test
    public void whatFitsInTheBudgetIsNotEatenYet() {
        assertTrue(hits("m\u00e9g 2 szelet pizza belef\u00e9r").isEmpty());
        assertTrue(hits("belef\u00e9rne m\u00e9g egy s\u00f6r").isEmpty());
        // De az EV\u00c9S-ige felment: a marad\u00e9k keretet meg is lehet enni.
        assertEquals(150.0,
                hits("a marad\u00e9k kal\u00f3ri\u00e1n megettem egy joghurtot").get(0).grams, 0.01);
        assertEquals(200.0, hits("megettem 2 szelet pizz\u00e1t").get(0).grams, 0.01);
    }

    /**
     * A FEH\u00c9R K\u00c1V\u00c9 magyarul tejesk\u00e1v\u00e9: az „ittam egy feh\u00e9r k\u00e1v\u00e9t"
     * FEKETE k\u00e1v\u00e9k\u00e9nt ment be, vagyis a tej kal\u00f3ri\u00e1ja csendben
     * lemaradt a napr\u00f3l.
     */
    @Test
    public void whiteCoffeeIsCoffeeWithMilk() {
        assertEquals("Tejesk\u00e1v\u00e9 / cappuccino",
                hits("ittam egy feh\u00e9r k\u00e1v\u00e9t").get(0).food.name);
        assertEquals(250.0, hits("ittam egy feh\u00e9r k\u00e1v\u00e9t").get(0).grams, 0.01);
        // A hossz\u00fa \u00e9s a r\u00f6vid k\u00e1v\u00e9 marad fekete – ott t\u00e9nyleg nincs tej.
        assertEquals("K\u00e1v\u00e9 (fekete)", hits("hossz\u00fa k\u00e1v\u00e9").get(0).food.name);
        assertEquals("K\u00e1v\u00e9 (fekete)", hits("ittam egy fekete k\u00e1v\u00e9t").get(0).food.name);
    }

    /**
     * A FOKHAGYMA nem hagyma: az „egy gerezd fokhagyma" a hagyma t\u00f6v\u00e9re
     * esett, \u00e9s \u00f6tven gramm v\u00f6r\u00f6shagyma ment a napl\u00f3ba egy n\u00e9h\u00e1ny
     * grammos gerezd helyett. A gerezd az adagja – \u00e9s m\u00e9r\u0151sz\u00f3 is: a „k\u00e9t
     * gerezd" eddig ugyanannyi volt, mint az egy.
     */
    @Test
    public void garlicIsNotOnion() {
        assertEquals("Fokhagyma", hits("egy gerezd fokhagyma").get(0).food.name);
        assertEquals(5.0, hits("egy gerezd fokhagyma").get(0).grams, 0.01);
        assertEquals(10.0, hits("k\u00e9t gerezd fokhagyma").get(0).grams, 0.01);
        // A v\u00f6r\u00f6shagyma marad hagyma.
        assertEquals("Hagyma", hits("egy fej hagyma").get(0).food.name);
    }

    /**
     * Amit v\u00e9g\u00fcl NEM ett\u00fcnk meg: a „majdnem megettem egy f\u00e1nkot, de nem"
     * hatvan gramm f\u00e1nkot \u00edrt a napl\u00f3ba, a „megk\u00edn\u00e1ltak s\u00fctivel, de nem
     * k\u00e9rtem" pedig sz\u00e1z gramm s\u00fctem\u00e9nyt – egy visszautas\u00edtott
     * k\u00edn\u00e1l\u00e1sb\u00f3l. Mindk\u00e9t mondatban ott az ev\u00e9s-ige, ez\u00e9rt a
     * kiv\u00e9tel-lista felmentette \u0151ket.
     */
    @Test
    public void whatWasAlmostEatenIsNotEaten() {
        assertTrue(hits("majdnem megettem egy f\u00e1nkot, de nem").isEmpty());
        assertTrue(hits("majdnem megittam egy s\u00f6rt").isEmpty());
        assertTrue(hits("megk\u00edn\u00e1ltak s\u00fctivel, de nem k\u00e9rtem").isEmpty());
        // A mondat KÖZEP\u00c9N \u00e1ll\u00f3 tagad\u00e1s marad tagmondat-hat\u00f3k\u00f6r\u0171.
        assertEquals(150.0,
                hits("ettem egy alm\u00e1t, de nem ettem meg az eg\u00e9szet").get(0).grams, 0.01);
        assertEquals(200.0,
                hits("nem ettem semmit reggel, csak egy k\u00e1v\u00e9t").get(0).grams, 0.01);
        assertEquals(150.0,
                hits("ettem egy joghurtot, de nem voltam \u00e9hes").get(0).grams, 0.01);
    }

    /**
     * A MAKR\u00d3-FEJL\u00c9C nem \u00e9tel: a „feh\u00e9rje/sz\u00e9nhidr\u00e1t/zs\u00edr 180/220/70"
     * sorb\u00f3l t\u00edz gramm OLAJ ker\u00fclt az \u00e9tkez\u00e9snapl\u00f3ba – a „zs\u00edr" itt a
     * t\u00e1p\u00e9rt\u00e9k neve, nem a serpeny\u0151ben l\u00e9v\u0151 zsiradék.
     */
    @Test
    public void aMacroHeaderIsNotFood() {
        assertTrue(hits("feh\u00e9rje/sz\u00e9nhidr\u00e1t/zs\u00edr 180/220/70").isEmpty());
        // A mag\u00e1ban \u00e1ll\u00f3 zs\u00edr marad val\u00f3di \u00e9tel.
        assertFalse(hits("10 g zs\u00edr").isEmpty());
    }

    /**
     * A T\u00d6RT alak\u00fa mennyis\u00e9g: az „1/2 kg csirkemell" f\u00e9l kil\u00f3 – eddig
     * K\u00c9T kil\u00f3 lett bel\u0151le, mert a perjel el\u0151tti egyes elveszett, \u00e9s a
     * nevez\u0151t vett\u00fck mennyis\u00e9gnek. N\u00e9gyszeres adag ker\u00fclt a napl\u00f3ba.
     */
    @Test
    public void aFractionIsAFraction() {
        assertEquals(500.0, hits("1/2 kg csirkemell").get(0).grams, 0.01);
        assertEquals(250.0, hits("1/4 kg t\u00far\u00f3").get(0).grams, 0.01);
        assertEquals(750.0, hits("3/4 l tej").get(0).grams, 0.01);
        assertEquals(500.0, hits("1/2 liter tej").get(0).grams, 0.01);
        // A kii\u0301rt alak v\u00e1ltozatlan.
        assertEquals(500.0, hits("f\u00e9l kil\u00f3 alma").get(0).grams, 0.01);
    }

    /**
     * A HOSSZABB alak el\u0151bb: a „300 milliliter v\u00edz" az „ml" \u00e1gra esett, a
     * marad\u00e9k „illiliter" pedig elszak\u00edtotta a sz\u00e1mot az \u00e9telt\u0151l –
     * k\u00e9tsz\u00e1z\u00f6tven grammos alapadag ment be h\u00e1romsz\u00e1z helyett.
     */
    @Test
    public void theSpelledOutMillilitreCounts() {
        assertEquals(300.0, hits("300 milliliter v\u00edz").get(0).grams, 0.01);
        assertEquals(300.0, hits("3 deciliter tej").get(0).grams, 0.01);
    }

    /**
     * A T\u00d6RT az ADAGSZ\u00d3 el\u0151tt is t\u00f6rt: az „1/2 t\u00e1bla csoki" k\u00e9tsz\u00e1z
     * gramm lett, az „1/2 pizza" hatsz\u00e1z – a nevez\u0151t vett\u00fck darabsz\u00e1mnak.
     */
    @Test
    public void aFractionOfAPortionIsAFraction() {
        assertEquals(50.0, hits("1/2 t\u00e1bla csoki").get(0).grams, 0.01);
        assertEquals(100.0, hits("1/2 adag rizs").get(0).grams, 0.01);
        assertEquals(150.0, hits("1/2 pizza").get(0).grams, 0.01);
    }

    /**
     * Az ADAG jelz\u0151je egybe\u00edrva is jelz\u0151: a „duplaadag csirkemell" egy
     * sima adag lett, a „f\u00e9ladag rizs" pedig egy eg\u00e9sz – vagyis a
     * mennyis\u00e9g \u00e9pp az ellenkez\u0151j\u00e9re fordult.
     */
    @Test
    public void theJoinedPortionAdjectiveStillCounts() {
        assertEquals(300.0, hits("duplaadag csirkemell").get(0).grams, 0.01);
        assertEquals(100.0, hits("f\u00e9ladag rizs").get(0).grams, 0.01);
        // K\u00fcl\u00f6n \u00edrva eddig is j\u00f3 volt.
        assertEquals(300.0, hits("dupla adag csirkemell").get(0).grams, 0.01);
    }

    /**
     * A NAPI V\u00cdZ t\u00f6bb r\u00e9szletben fogy: az „1 liter v\u00edz reggel, 1 liter
     * d\u00e9lut\u00e1n" EGY litert \u00edrt a napl\u00f3ba, mert egy \u00e9tel a mondatban
     * egyszer szerepelhet – a napi v\u00edzc\u00e9l \u00edgy f\u00e9lig telt meg abb\u00f3l, ami
     * val\u00f3j\u00e1ban megvolt.
     */
    @Test
    public void waterDrunkInPartsAddsUp() {
        assertEquals(2000.0, hits("1 liter v\u00edz reggel, 1 liter d\u00e9lut\u00e1n")
                .get(0).grams, 0.01);
        assertEquals(500.0, hits("2 dl v\u00edz reggel \u00e9s 3 dl v\u00edz este")
                .get(0).grams, 0.01);
        assertEquals(1000.0, hits("500 ml v\u00edz edz\u00e9s alatt \u00e9s 500 ml ut\u00e1na")
                .get(0).grams, 0.01);
        // Egyetlen adag v\u00e1ltozatlan, \u00e9s m\u00e1s \u00e9tel mellett nem \u00f6sszegz\u00fcnk.
        assertEquals(2000.0, hits("megittam 2 liter vizet ma").get(0).grams, 0.01);
        List<Foods.Hit> h = hits("2 dl tej \u00e9s 1 liter v\u00edz");
        assertEquals(2, h.size());
        assertEquals(1000.0, h.get(1).grams, 0.01);
    }

    /**
     * A M\u00c9R\u0150SZALAG adata nem \u00e9tel: a „comb 55 cm" a comb k\u00f6rfogata,
     * m\u00e9gis sz\u00e1z\u00f6tven gramm CSIRKECOMB ker\u00fclt mell\u00e9 az \u00e9tkez\u00e9snapl\u00f3ba.
     */
    @Test
    public void aTapeMeasureReadingIsNotFood() {
        assertTrue(hits("comb 55 cm").isEmpty());
        assertTrue(hits("38 cm comb").isEmpty());
        // A m\u00e9r\u00e9s mellett \u00e1ll\u00f3 VAL\u00d3DI \u00e9tel megmarad.
        List<Foods.Hit> h = hits("Eb\u00e9d: 200 g csirkemell. Comb 55 cm.");
        assertEquals(1, h.size());
        assertEquals(200.0, h.get(0).grams, 0.01);
        // A t\u00e1ny\u00e9ron l\u00e9v\u0151 csirkecomb marad \u00e9tel.
        assertEquals(200.0, hits("s\u00fclt csirkecomb 200 g").get(0).grams, 0.01);
    }

    /**
     * A z\u00e1r\u00f3 mennyis\u00e9g akkor is sz\u00e1m\u00edt, ha el\u0151tte M\u00c1SIK mondat \u00e1ll: a
     * „Sok volt a stressz, csak s\u00e9t\u00e1ltam 25 percet. Vacsi marad\u00e9k pizza,
     * 2 szelet." mondat\u00e1ban a huszon\u00f6t PERC tiltotta le a k\u00e9t szeletet,
     * pedig az a m\u00e1sik mondatban \u00e1ll – \u00edgy egy eg\u00e9sz pizza ment be k\u00e9t
     * szelet helyett. A mondatv\u00e9gi pont sem z\u00e1rhatja ki a mennyis\u00e9get.
     */
    @Test
    public void theTrailingAmountSurvivesAnEarlierSentence() {
        assertEquals(200.0, hits(
                "Sok volt a stressz, csak s\u00e9t\u00e1ltam 25 percet. "
                + "Vacsi marad\u00e9k pizza, 2 szelet.").get(0).grams, 0.01);
        assertEquals(200.0, hits("Vacsi marad\u00e9k pizza, 2 szelet").get(0).grams, 0.01);
        // A SAJ\u00c1T sz\u00e1mmal \u00edrt fej tov\u00e1bbra sem k\u00e9rdez vissza.
        assertEquals(700.0, hits("eb\u00e9dre t\u00f6lt\u00f6tt k\u00e1poszta volt, k\u00e9t adag")
                .get(0).grams, 0.01);
    }


    /**
     * A C\u00c9L nem adag: az \u201e\u00faj c\u00e9lok: heti 4 edz\u00e9s, napi 10000 l\u00e9p\u00e9s, 2 liter
     * v\u00edz, 1800 kcal" k\u00e9t liter vizet \u00edrt a MAI napl\u00f3ba \u2013 abb\u00f3l a
     * mondatb\u00f3l, ami \u00e9pp a j\u00f6v\u0151r\u0151l sz\u00f3l.
     */
    @Test
    public void aGoalIsNotAPortion() {
        assertTrue(hits("\u00daj c\u00e9lok: heti 4 edz\u00e9s, napi 10000 l\u00e9p\u00e9s, "
                + "2 liter v\u00edz, 1800 kcal.").isEmpty());
        assertTrue(hits("A c\u00e9lom napi 2 liter v\u00edz \u00e9s 1800 kcal.").isEmpty());
        assertTrue(hits("Mostant\u00f3l minden nap iszom 2 liter vizet.").isEmpty());
        // A megt\u00f6rt\u00e9nt iv\u00e1s marad.
        assertEquals(2000.0, hits("Ma megittam 2 liter vizet.").get(0).grams, 0.01);
        assertEquals(150.0, hits("Mostant\u00f3l figyelek magamra, ma ettem egy alm\u00e1t.")
                .get(0).grams, 0.01);
    }


    /**
     * A TEGNAPI poh\u00e1r nem a mai v\u00edzbe folyik: a \u201etegnap 2 liter vizet ittam,
     * ma eddig csak 8 dl-t" k\u00e9t liter nyolc decit \u00edrt a MAI napl\u00f3ba.
     */
    @Test
    public void yesterdaysWaterDoesNotJoinTodaysTotal() {
        assertEquals(2000.0, hits("Tegnap 2 liter vizet ittam, ma eddig csak 8 dl-t.")
                .get(0).grams, 0.01);
        // A mai r\u00e9szletek tov\u00e1bbra is \u00f6sszead\u00f3dnak.
        assertEquals(2000.0, hits("1 liter v\u00edz reggel, 1 liter d\u00e9lut\u00e1n.")
                .get(0).grams, 0.01);
        assertEquals(2000.0, hits("Reggel 5 dl v\u00edz, d\u00e9lben 5 dl, este 1 liter.")
                .get(0).grams, 0.01);
    }


    /**
     * A K\u00c9TSZAVAS jelz\u0151 is bef\u00e9rhet a m\u00e9r\u0151sz\u00f3 \u00e9s az \u00e9tel k\u00f6z\u00e9: a \u201e3 db
     * teljes ki\u0151rl\u00e9s\u0171 kifli" egyetlen kiflit \u00edrt a napl\u00f3ba \u2013 a h\u00e1rmas
     * elveszett, mert a szab\u00e1ly csak egy sz\u00f3nyi jelz\u0151t t\u0171rt el.
     */
    @Test
    public void aTwoWordAdjectiveKeepsThePieceCount() {
        assertEquals(165.0, hits("Eb\u00e9dre 3 db teljes ki\u0151rl\u00e9s\u0171 kifli.")
                .get(0).grams, 0.01);
        assertEquals(110.0, hits("2 db teljes ki\u0151rl\u00e9s\u0171 zsemle.").get(0).grams, 0.01);
        assertEquals(70.0, hits("2 szelet h\u00e1zi s\u00fclt keny\u00e9r.").get(0).grams, 0.01);
        // A k\u00f6t\u0151sz\u00f3 felsorol\u00e1st nyit: ott a sz\u00e1m az els\u0151\u00e9.
        java.util.List<Foods.Hit> h = hits("2 db sajt \u00e9s kifli.");
        assertEquals(2, h.size());
        assertEquals(60.0, h.get(0).grams, 0.01);
        assertEquals(0.0, h.get(1).grams, 0.01);
    }


    /**
     * Az EVÉS IGÉJE a mennyiség mögött is állhat: az „a süti, amit vittek a
     * munkahelyre, kb 3 szeletet ettem belőle" három szelete egyetlen
     * adagra zsugorodott, mert a mennyiség nem a mondat végén állt.
     */
    @Test
    public void aTrailingAmountSurvivesTheVerbAfterIt() {
        assertEquals(240.0, hits("a süti amit vittek a munkahelyre, "
                + "kb 3 szeletet ettem belőle").get(0).grams, 0.01);
        assertEquals(240.0, hits("sütit vittek a munkahelyre, "
                + "3 szeletet ettem belőle").get(0).grams, 0.01);
        // A korábbi alakok változatlanok.
        assertEquals(200.0, hits("Vacsi maradék pizza, 2 szelet.").get(0).grams, 0.01);
        assertEquals(700.0, hits("ebédre töltött káposzta volt, két adag")
                .get(0).grams, 0.01);
    }


    /**
     * A COMBFILÉ is csirke: a „sült csirke combfilé 200 g" háromszázötven
     * gramm húst írt a naplóba – a kétszázas combfilét ÉS egy százötven
     * grammos csirkemellet.
     */
    @Test
    public void theChickenIsCountedOnce() {
        java.util.List<Foods.Hit> h = hits("sült csirke combfilé 200 g");
        assertEquals(1, h.size());
        assertEquals("Csirkecomb", h.get(0).food.name);
        assertEquals(200.0, h.get(0).grams, 0.01);
        // Felsorolva viszont két külön tétel.
        assertEquals(2, hits("csirkemell 150 g és csirkecomb 200 g").size());
    }

    /**
     * A magyarázat vize nem megivott víz.
     *
     * A „ma reggel még 82 kg voltam, este már csak 81,5 – biztos a víz"
     * mondatában a víz a mérleg ingadozásának OKA, nem egy pohár ital –
     * mégis negyed liter víz került tőle a naplóba.
     */
    @Test public void theWaterOfAnExplanationIsNotADrink() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "ma reggel még 82 kg voltam, este már "
                + "csak 81,5 – biztos a víz").isEmpty());
        assertTrue(Foods.parse(all, "egy hét alatt 3 kilót fogytam, de "
                + "szerintem ez a víz").isEmpty());
        // Az ivás igéje és a kimondott mennyiség felment.
        assertFalse(Foods.parse(all, "sok vizet ittam ma").isEmpty());
        assertEquals(2000.0, Foods.parse(all, "ittam 2 liter vizet")
                .get(0).grams, 0.01);
    }

    /**
     * A gyűjtőnév a megnevezett étel visszautalása.
     *
     * A „vacsora: 2 db lazacfilé sült zöldséggel, kb 300 g hal" háromszáz
     * grammja ugyanaz a lazac – a vessző miatt mégis külön fehér halként is
     * bekerült, vagyis hatszáz gramm abból a háromszázból, amit az ember
     * megevett.
     */
    @Test public void aGenericNameCanBeABackReference() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all, "vacsora: 2 db lazacfilé "
                + "sült zöldséggel, kb 300 g hal");
        for (Foods.Hit h : hits) assertFalse(h.food.name.startsWith("Hal ("));
        // A valódi felsorolás két tétel marad.
        assertEquals(2, Foods.parse(all, "vacsora: lazac és hal").size());
    }

    /**
     * A pótlás felmenti a felejtést.
     *
     * A „reggel elfelejtettem enni, délben pótoltam: 2 szendvics" ebédje
     * megevett ebéd – eddig az egész bejegyzés elveszett a felejtés
     * szavától, a szendvicsekkel együtt.
     */
    @Test public void makingUpForItCancelsTheForgetting() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertFalse(Foods.parse(all, "reggel elfelejtettem enni, délben "
                + "pótoltam: 2 szendvics").isEmpty());
        // A pótlás nélküli felejtés marad kihagyás.
        assertTrue(Foods.looksUneaten("elfelejtettem bevenni a vitaminokat"));
    }

    /**
     * A heti ebédek megfőzése előkészület.
     *
     * A „ma este megfőztem a heti ebédeket, 5 adag csirkés rizs"
     * HÉTSZÁZÖTVEN GRAMM csirkét írt a MAI naplóba – abból az ételből, ami
     * a jövő heti ebéd.
     */
    @Test public void cookingTheWeeksLunchesIsNotEating() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "ma este megfőztem a heti ebédeket, "
                + "5 adag csirkés rizs").isEmpty());
        // A mai ebéd marad mai ebéd.
        assertFalse(Foods.parse(all, "ma ebédre csirkés rizs").isEmpty());
    }

    /**
     * A ropogós bőr nem bor.
     *
     * Ékezet nélkül a kettő egybeesik: az „este megettem egy egész
     * csirkecombot ropogós bőrrel" mellé másfél deci BOR is bekerült az
     * étrendbe. A bor eszközragja „borral" (a-hangrend), a bőré „bőrrel" –
     * ez elválasztja őket.
     */
    @Test public void crispySkinIsNotWine() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        for (Foods.Hit h : Foods.parse(all, "este megettem egy egész "
                + "csirkecombot ropogós bőrrel"))
            assertFalse(h.food.name.startsWith("Bor "));
        // A valódi bor marad bor.
        assertFalse(Foods.parse(all, "ittam egy pohár bort").isEmpty());
    }

    /**
     * A bepótolt kávé nem marad tagadva.
     *
     * A „kávé nélkül indult a nap, de dél után bepótoltam, 3 kávé lett"
     * bejegyzésből SEMMI nem lett: a felismerő ugyanahhoz az ételhez
     * egyetlen (az első) helyet jegyez, és a „nélkül" azt ölte meg – hiába
     * mondja ki a mondat vége, hogy három kávé lett.
     */
    @Test public void aFoodRepeatedAfterTheNegationSurvives() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertEquals(600.0, Foods.parse(all, "kávé nélkül indult a nap, de "
                + "dél után bepótoltam, 3 kávé lett").get(0).grams, 0.01);
        // Az egyszer, tagadva említett étel marad kihagyva.
        assertTrue(Foods.parse(all, "kávé nélkül indult a nap").isEmpty());
    }

    /**
     * A „proteines" jelző nem külön turmix.
     *
     * A „proteines palacsinta" palacsintája mellé egy háromszáz grammos
     * protein turmix is bement – háromszáz kalória egy jelzőből.
     */
    @Test public void aProteinAdjectiveIsNotAShake() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all, "proteines palacsinta");
        assertEquals(1, hits.size());
        assertEquals("Palacsinta", hits.get(0).food.name);
        // A valódi turmix marad.
        assertEquals("Protein turmix", Foods.parse(all, "protein turmix "
                + "banánnal").get(0).food.name);
    }

    /**
     * A halfilé hal, a megérkezett súlyzó nem edzés.
     *
     * A „vacsira halfilé párolt zöldséggel" hala nyomtalanul eltűnt – az
     * angol „half" (maraton) tiltója kapta el a szó elejét, és csak a
     * köret ment be.
     */
    @Test public void aFishFilletIsAFish() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all, "vacsira halfilé párolt "
                + "zöldséggel");
        boolean fish = false;
        for (Foods.Hit h : hits) if (h.food.name.startsWith("Hal")) fish = true;
        assertTrue(fish);
        // Az angol half maraton marad kitakarva.
        assertTrue(Foods.parse(all, "lefutottam a half maratont").isEmpty());
    }

    /**
     * A vendéglátóhely neve nem fogás.
     *
     * Az „ebéd a Wokbar-ból: pad thai csirkével" wokja a hely nevében ül –
     * mégis egy 350 grammos wok-tál került a pad thai MELLÉ, közel dupla
     * kalóriával.
     */
    @Test public void aVenueNameIsNotADish() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all, "ebéd a Padthai Wokbar-ból: "
                + "pad thai csirkével");
        assertEquals(1, hits.size());
        assertEquals("Pad thai", hits.get(0).food.name);
        // A valódi wok-tál marad.
        assertEquals("Wok (zöldséges-húsos)", Foods.parse(all,
                "wok zöldségekkel").get(0).food.name);
    }

    /**
     * A proteines zabkása sem zabkása plusz turmix.
     *
     * A „proteines zabkása áfonyával" mellé is bement a háromszáz grammos
     * turmix – a következő étel töve a szó belsejében ült (kása), így a
     * jelző-szűrő nem látta.
     */
    @Test public void aProteinAdjectiveBeforeACompoundIsNotAShake() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all, "Reggeli: proteines "
                + "zabkása áfonyával.");
        for (Foods.Hit h : hits)
            assertFalse(h.food.name.startsWith("Protein turmix"));
        assertEquals(2, hits.size());
    }

    /**
     * A lezárt böjt utáni első étkezés valódi étkezés.
     *
     * A „ma böjtöltem 16 órát, az első étkezés délben volt: rántotta"
     * rántottája elveszett – a böjt szava az egész bejegyzést
     * elnémította, pedig a mondat pont az evésről szól.
     */
    @Test public void theMealAfterAFinishedFastCounts() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertEquals("Rántotta", Foods.parse(all, "Ma böjtöltem 16 órát, "
                + "az első étkezés délben volt: rántotta.").get(0).food.name);
        // A puszta böjt marad üres.
        assertTrue(Foods.parse(all, "Ma böjtöltem egész nap.").isEmpty());
    }

    /**
     * A turmix rövid közbevetéssel is a hozzávalóiból áll.
     *
     * Az „a vacsorám csak egy turmix volt: mangó, joghurt, zabpehely"
     * turmixa a hozzávalók MELLÉ került – dupla vacsora –, mert a „volt"
     * elállta a kettőspont útját.
     */
    @Test public void aSmoothieWithAnInterjectionIsStillItsIngredients() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all, "A vacsorám csak egy "
                + "turmix volt: mangó, joghurt, zabpehely.");
        for (Foods.Hit h : hits)
            assertFalse(h.food.name.contains("turmix"));
        assertEquals(3, hits.size());
    }

    /**
     * A mérleg magyarázata nem ital, a tegnapi ok nem mai falat.
     *
     * Az „a mérleg 82,4-et mutatott, de tegnap este sokat ittam, szóval
     * lehet csak víz" mondatból negyed liter víz került a naplóba – pedig
     * a víz ott a súlytöbblet oka. A „fáradtan keltem, lehet a sok tegnapi
     * kávé miatt" pedig kétdecis MAI feketét írt be a tegnapi kávéból.
     */
    @Test public void anExplanationIsNeitherADrinkNorAMeal() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "A mérleg 82,4-et mutatott, de tegnap "
                + "este sokat ittam, szóval lehet csak víz.").isEmpty());
        assertTrue(Foods.parse(all, "Fáradtan keltem, lehet a sok tegnapi "
                + "kávé miatt.").isEmpty());
        // A tegnapi maradék MEGEVÉSE viszont mai étkezés.
        assertEquals("Rakott krumpli", Foods.parse(all, "Megettem a tegnapi "
                + "maradék rakott krumplit ebédre.").get(0).food.name);
        // A kimondott mennyiségű víz ivása is marad.
        assertEquals(2000.0, Foods.parse(all, "Biztos a víz miatt vagyok "
                + "nehezebb, ittam 2 liter vizet edzés után.")
                .get(0).grams, 0.01);
    }

    /**
     * A kávézó hely, nem ital.
     *
     * Az „a kávézóban dolgoztam egész nap" mellé egy kétdecis fekete
     * került, a „borozóban ünnepeltünk" mellé egy pohár bor – italok,
     * amiket senki nem mondott ki. A -zó képző helyet csinál a tőből.
     */
    @Test public void aCoffeeHouseIsAPlaceNotADrink() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "A kávézóban dolgoztam egész nap, "
                + "alig mozogtam.").isEmpty());
        assertTrue(Foods.parse(all, "A borozóban ünnepeltük a "
                + "születésnapot.").isEmpty());
        // A kávézóban MEGIVOTT kávé marad.
        assertEquals("Kávé (fekete)", Foods.parse(all, "Ittam egy kávét "
                + "a kávézóban a barátnőmmel.").get(0).food.name);
    }

    /**
     * Az özönvíz eső, a bőrig ázás nem bor, a zsírmérő nem olaj.
     *
     * Az „özönvízszerű esőben tekertem haza, bőrig áztam" mellé egy pohár
     * víz ÉS egy pohár bor került, az „a zsírmérő szerint 24,8 százalék"
     * mellé pedig egy kanál olaj – csupa ital és étel, amit senki nem
     * mondott ki.
     */
    @Test public void rainAndAFatMeterAreNotDrinks() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertTrue(Foods.parse(all, "Özönvízszerű esőben tekertem haza "
                + "12 km-t, bőrig áztam.").isEmpty());
        assertTrue(Foods.parse(all, "A mérlegen 91,2, a zsírmérő szerint "
                + "24,8 százalék.").isEmpty());
        // A kimondott bor marad bor.
        assertEquals("Bor (vörös/fehér)", Foods.parse(all, "Bort ittam "
                + "este, két pohár vöröset.").get(0).food.name);
    }

    /**
     * A csoki tej ital, nem fél kiló csokoládé.
     *
     * A „vízilabda edzés 90 perc, utána fél liter csoki tej" itala fél
     * KILÓ csokoládéként ment be – kétezer-hétszáz kalória egy kakaóból.
     */
    @Test public void chocolateMilkIsADrinkNotABar() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        List<Foods.Hit> hits = Foods.parse(all, "Vízilabda edzés 90 perc, "
                + "utána fél liter csoki tej.");
        assertEquals(1, hits.size());
        assertEquals("Kakaó (tejes)", hits.get(0).food.name);
        assertEquals(500.0, hits.get(0).grams, 0.01);
        // A tábla csoki marad csokoládé.
        assertEquals("Csokoládé", Foods.parse(all, "Ettem egy tábla "
                + "csokit.").get(0).food.name);
    }

    /**
     * A töltött káposzta darabja egy töltelék, nem egy teljes tányér.
     *
     * A „vacsora: három töltött káposzta tejföllel" bő egy KILÓ töltött
     * káposzta lett – a darabszám a 350 grammos tányér-adagot sokszorozta.
     */
    @Test public void aStuffedCabbageRollIsARollNotAPlate() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        assertEquals(600.0, Foods.parse(all, "Vacsora: három töltött "
                + "káposzta tejföllel.").get(0).grams, 0.01);
        // Darabszám nélkül marad a tányér-adag (a gramm 0 = tipikus adag).
        Foods.Hit plain = Foods.parse(all, "Töltött káposztát ettem "
                + "ebédre.").get(0);
        assertEquals(0.0, plain.grams, 0.01);
        assertEquals(350, plain.food.portion);
    }

    /**
     * A bagett darabja negyed kiló, nem egy szelet kenyér.
     *
     * A „fél bagett sajttal" tizenhét és fél GRAMM kenyér lett – egy
     * falatnyi a valódi százhuszonöt helyett.
     */
    @Test public void halfABaguetteIsHalfABaguette() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        Foods.Hit h = Foods.parse(all, "Fél bagett sajttal a vacsora.")
                .get(0);
        assertEquals("Bagett", h.food.name);
        assertEquals(125.0, h.grams, 0.01);
    }
}
