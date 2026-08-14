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
}
