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

    /**
     * Hétköznapi magyar szavak, amiknek SEMMI közük az ételhez.
     *
     * Ez a lista az őrszem: minden új szótő ellen lefut – az ételeké és (a
     * StrengthParseTest-ből) a gyakorlatoké ellen is. Az ütközés ugyanis
     * csendes: a felismerés sikeresnek látszik, csak épp nem azt naplózza,
     * amit az ember csinált – és pont ezért nem derül ki magától.
     */
    static final String[] EVERYDAY = {
            "abban", "addig", "ahol", "ajtó", "akkor", "alatt", "annyi", "anya", "apa",
            "arc", "asztal", "átlag", "autó", "bal", "barát", "beszéd", "biztos",
            "busz", "cél", "cipő", "család", "csend", "csoport", "derék", "döntés",
            "edzés", "egészség", "együtt", "elég", "élet", "ellen", "előtt", "ember",
            "erő", "érzés", "fal", "fáradtság", "fejlődés", "felé", "férfi", "fény",
            "fiú", "fizetés", "fog", "folyamat", "forma", "föld", "gerinc", "gond",
            "gyakorlat", "gyerek", "gyors", "haj", "hang", "hasonló", "ház", "helyzet",
            "hiba", "hideg", "hír", "hogyan", "hosszú", "idő", "igaz", "ilyen",
            "ismét", "iskola", "iskolában", "izom", "jelenleg", "jobb", "kar",
            "kapcsolat", "kép", "kérdés", "kéz", "kicsi", "kint", "könnyű", "könyv",
            "környék", "közel", "kutya", "lakás", "lassú", "lehetőség", "lélegzet",
            "levegő", "macska", "magas", "messze", "mindig", "mozgás", "munka",
            "nagyon", "nehéz", "nélkül", "nyak", "nyár", "óra", "orvos", "összes",
            "pár", "pihenő", "pillanat", "probléma", "program", "rend", "rossz",
            "sok", "sport", "súly", "szabad", "szabadnap", "szabadidő", "szám",
            "szék", "szem", "személy", "szint", "szoba", "szükség", "talán", "tanár",
            "tavasz", "tegnap", "tél", "terv", "tévé", "tükör", "új", "ujj", "út",
            "üzenet", "váll", "változás", "város", "vér", "verseny", "vissza",
            "zene", "boka", "térd", "csukló", "könyök", "medence", "szalag",
            "ízület", "pulzus", "légzés", "nyújtás", "bemelegítés", "levezetés",
            "sorozat", "ismétlés", "súlyzó", "rúd", "tárcsa", "pad", "gép",
            "szőnyeg", "kötél", "labda", "futópad", "evezőgép", "szobabicikli",
            "hétfő", "kedd", "szerda", "péntek", "szombat", "vasárnap", "január",
            "február", "március", "április", "május", "június", "július",
            "augusztus", "szeptember", "október", "november", "december",
            "délelőtt", "délután", "hajnal", "fáj", "fájt", "húz", "nyom", "emel",
            "tol", "fut", "megy", "jár", "úszik", "biciklizik", "edz", "pihen",
            "alszik", "főz", "vásárol", "dolgozik", "tanul", "olvas", "ír",
            "beszél", "hallgat", "hallottam", "néz", "lát", "érez", "gondol",
            "tud", "akar", "kell", "lehet", "szeret", "kezd", "folytat", "befejez",
            "abbahagy", "próbál", "sikerül", "elront", "javít", "változtat",
            "motiváció", "fegyelem", "kitartás", "eredmény", "visszaesés", "plató",
            "regeneráció", "alvás", "stressz", "hangulat", "energia", "erőnlét",
            "állóképesség", "hajlékonyság", "egyensúly", "koordináció", "technika",
            "tartás", "tempó", "ritmus", "majd", "majdnem", "majom", "majális",
            "halál", "halom", "halasztás", "babona", "babérlevél", "bábu",
            "tejút", "rizikó", "sorsolás", "alkalmas", "alkalom", "bordázat",
            "uszoda", "uszodában", "edzőterem", "konditerem", "pálya", "park",
            "erdő", "koleszterin", "multisport", "bérlet", "jegy", "pénz",
            // A legrövidebb szótövek (viz, zab, riz, rum, sor, bor, vaj, mez)
            // hétköznapi szavak belsejében is illeszkednek.
            "vizsga", "vizsgálat", "vizit", "televízió", "szabály", "frizura",
            "krízis", "sorompó", "sorsjegy", "fórum", "szérum", "rumba",
            "teátrum", "centrum", "album", "vajúdik", "tábor", "labor",
            "zabál", "kombájn", "olimpia", "kabát", "borotva", "terapeuta",
            "kultúra", "struktúra", "tekercs", "tornacipő", "kézitáska",
            // Harmadik söprés (háromszáz szó, három menetben). Mind csendes
            // ütközés volt: a felismerés sikeresnek LÁTSZOTT.
            "megbeszélés", "értekezlet", "prezentáció", "határidő", "szerződés",
            "számla", "bankkártya", "átutalás", "biztosítás", "albérlet",
            // (A költözés, a takarítás, a kertészkedés és a fűnyírás
            // szándékosan hiányzik: azok valódi fizikai munkák, és a listát
            // az edzés-felismerő is használja.)
            "szekrény", "függöny", "szőnyeg", "porszívó", "mosogatás",
            "barkácsolás", "autópálya", "benzinkút",
            "parkolás", "parkoló", "forgalom", "villamos", "vonatjegy", "repülőtér",
            "poggyász", "szállás", "nyaralás", "strandolás", "napozás", "olvasás",
            "színház", "koncert", "kiállítás", "múzeum", "fényképezés", "zongora",
            "kártyázás", "videojáték", "telefonálás", "értesítés", "jelszó",
            "fogorvos", "gyógyszer", "beutaló", "megfázás", "influenza", "allergia",
            "álmatlanság", "ébresztő", "szabadság", "karácsony", "húsvét",
            "ballagás", "vizsgaidőszak", "gyakornok", "önéletrajz", "állásinterjú",
            "felmondás", "nyugdíj", "megvalósítás", "szervezés", "irányítás",
            "szavazás", "bizottság", "beszámoló", "elemzés", "fejlesztés",
            "hibajavítás", "megtakarítás", "befektetés", "árfolyam", "infláció",
            "figyelmeztetés", "rendőrség", "tűzoltó", "tanterem", "memorizálás",
            "vonalzó", "számológép", "billentyűzet", "akkumulátor", "légkondi",
            "ventilátor", "kandalló", "kilincs", "riasztó", "erkély", "garázs",
            "lépcsőház", "postaláda", "bevásárlás", "kosár", "pénztár",
            "nagymama", "nagypapa", "szomszéd", "veszekedés", "bocsánatkérés",
            "meglepetés", "vendégség", "tengerpart", "barlang", "villámlás",
            "olvadás", "naplemente", "szorongás", "szomorúság", "akaraterő",
            "lustaság", "kimerültség", "súlygyarapodás", "anyagcsere", "emésztés",
            "immunrendszer", "vérnyomás", "izomláz", "rehabilitáció", "mező",
            "hatalmas", "unalmas", "nyugalmas", "fájdalmas", "halogatás",
            "borostyán", "sörény",
            // Negyedik söprés: nevek, helynevek és a „kardió". A „dió" három
            // betű, és rengeteg szó közepén ott van – a kardió ráadásul
            // EDZÉS-szó, tehát minden ilyen bejegyzéshez járt harminc gramm
            // dió, kétszáz kalória.
            // (A „kardió" szándékosan hiányzik: az EDZÉS-szó, és a listát az
            // edzés-felismerő is futtatja. Az étel-oldali fedezete a
            // FoodsIntegrationTest-ben van.)
            "stúdió", "rádió", "audio", "periódus",
            "kardiológus", "stadion", "Gábor", "Boróka", "borúlátó", "bőrönd",
            "Salgótarján", "tortúra", "borotválkozás", "laboratórium",
            // Ötödik söprés: termi és táplálkozási szakszavak.
            "testzsír", "zsírbevitel", "edzésterv", "edzésnapló", "izomtömeg",
            "kalóriadeficit", "szálkásítás", "makró", "mikrotápanyag",
    };

    @Test public void noEverydayWordEverBecomesFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        StringBuilder bad = new StringBuilder();
        for (String w : EVERYDAY) {
            java.util.List<Foods.Hit> h = Foods.parse(all, w);
            for (Foods.Hit x : h)
                bad.append("\n  ").append(w).append(" -> ").append(x.food.name);
        }
        assertEquals("hétköznapi szóból étel lett:" + bad, 0, bad.length());
    }

    @Test public void aWholeListOfEverydayWordsStaysOutOfTheDiary() {
        // Egy 300 szavas magyar szólista végigfuttatásából jött. Mindegyik
        // ütközés csendes volt: a felismerés sikeresnek látszott.
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String w : new String[]{"hall", "hallottam", "halál", "halom",
                "halasztás", "szabad", "szabadnap", "szabadidő", "babona",
                "babérlevél", "bábu", "szobabicikli", "majom", "május",
                "majális", "tejút", "rizikó", "sorsolás", "alkalmas",
                "bordázat", "iskolában", "uszodában", "majd"})
            assertTrue("ez nem étel: " + w, Foods.parse(all, w).isEmpty());
        // A valódi ételnevek viszont megmaradnak – a maszkolás nem söpörhet
        // többet, mint amennyit kell.
        for (String[] w : new String[][]{{"halat", "Hal (fehér)"},
                {"zabpehely", "Zabpehely"}, {"bab", "Bab (főtt)"},
                {"májat", "Csirkemáj"}, {"tejet", "Tej"}, {"rizst", "Rizs (főtt)"},
                {"sört", "Sör"}, {"almát", "Alma"}, {"majonéz", "Majonéz"}})
            assertEquals(w[0], w[1], Foods.find(w[0]).name);
    }

    @Test public void theFourNewEntriesLandWhereTheyShould() {
        // A gyümölcssaláta korábban ZÖLD salátára esett: egy 200 grammos adag
        // 30 kalóriának látszott a valós ~120 helyett.
        assertEquals("Gyümölcssaláta", Foods.find("gyümölcssaláta").name);
        assertEquals("Saláta (zöld)", Foods.find("saláta").name);
        assertEquals("Görög saláta", Foods.find("görög saláta").name);
        // A puszta „köles" mostantól jó – a koleszterin viszont nem étel.
        assertEquals("Hajdina / köles (főtt)", Foods.find("köles").name);
        assertTrue(Foods.parse(java.util.Arrays.asList(Foods.ALL), "koleszterin").isEmpty());
        assertEquals("Kenyér", Foods.find("toast").name);
        assertEquals("Tökmag / napraforgómag", Foods.find("magvak").name);
    }

    @Test public void aDeclineAfterTheFoodStillCounts() {
        // „Megkínáltak tortával, de nem kértem" – a tortát nem ette meg, eddig
        // mégis bekerült. A tagadás eddig csak ELŐRE hatott.
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"megkínáltak tortával, de nem kértem",
                "hoztak sütit, nem kértem", "kínáltak pizzával, visszautasítottam",
                "elutasítottam a tortát"})
            assertTrue("ez nem került a tányérra: " + q, Foods.parse(all, q).isEmpty());
        // A visszafelé hatás SZŰK: csak akkor, ha az elutasító tagmondatban a
        // kötőszón kívül semmi más nincs. Az étel-felismerés nem tud mindent
        // (a ragozott „cukrot" alakot például nem), ezért itt marad a kávé.
        assertEquals(1, Foods.parse(all, "ittam kávét, de cukrot nem kértem").size());
        assertEquals(1, Foods.parse(all, "ittam kávét, de nem kértem cukrot").size());
        // És a máshogy fogalmazott tagadás sem viszi el a korábbi ételt.
        assertEquals(1, Foods.parse(all, "ettem egy almát, aztán nem ettem semmit").size());
        assertEquals(1, Foods.parse(all, "nem kértem sültkrumplit a hamburger mellé").size());
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
    /**
     * A „macchiato" kávé, nem chiamag.
     *
     * A „chia" szótő a szó KÖZEPÉN illeszkedett rá – a rövid tövek klasszikus
     * csapdája. A bejegyzés létrejött, csak épp tizenöt gramm chiamagként.
     */
    @Test public void aMacchiatoIsCoffee() {
        java.util.List<Foods.Hit> h =
                Foods.parse(java.util.Arrays.asList(Foods.ALL), "macchiato");
        assertEquals(1, h.size());
        assertEquals("Tejeskávé / cappuccino", h.get(0).food.name);
        // A chia magától továbbra is chia.
        assertEquals("Chia / lenmag", Foods.parse(java.util.Arrays.asList(Foods.ALL),
                "chia puding").get(0).food.name);
    }
    /**
     * A „párizsi felvágott" nem tartalmaz rizst.
     *
     * Ételenként csak a LEGHOSSZABB szótő helyét jegyezzük meg: a párizsi a
     * hosszabb „felvágott" tövön került be, a szó elején álló „párizsi" pedig
     * szabadon hagyta a benne rejlő „rizs"-t – kétszáz gramm rizs került a
     * felvágott mellé, csendben.
     */
    @Test public void aShorterStemHidingInsideALongerOneIsDropped() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"párizsi", "párizsi felvágott", "kevert süti"}) {
            java.util.List<Foods.Hit> h = Foods.parse(all, q);
            assertEquals(q + " -> " + names(h), 1, h.size());
        }
        assertEquals("Párizsi / felvágott", Foods.parse(all, "párizsi felvágott")
                .get(0).food.name);
        assertEquals("Piskóta / kevert süti", Foods.parse(all, "kevert süti")
                .get(0).food.name);
        // A rizs magától továbbra is rizs.
        assertEquals("Rizs (főtt)", Foods.parse(all, "rizs").get(0).food.name);
    }

    /** A wok adagjában benne van a zöldség: nem jár mellé külön adag. */
    @Test public void theWokAlreadyContainsItsVegetables() {
        java.util.List<Foods.Hit> h =
                Foods.parse(java.util.Arrays.asList(Foods.ALL), "zöldséges wok");
        assertEquals(names(h), 1, h.size());
        assertEquals("Wok (zöldséges-húsos)", h.get(0).food.name);
    }

    private static String names(java.util.List<Foods.Hit> h) {
        StringBuilder sb = new StringBuilder();
        for (Foods.Hit x : h) sb.append(sb.length() > 0 ? ", " : "").append(x.food.name);
        return sb.toString();
    }
}
