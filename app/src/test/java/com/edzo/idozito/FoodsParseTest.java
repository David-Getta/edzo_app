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

}
