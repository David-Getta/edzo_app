package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Sportolós alapdarabok és a tagadószavak.
 *
 * Az itteni eseteket egy próbafuttatás hozta felszínre: 61 gyakori étel közül
 * 19-et egyáltalán nem ismert az adatbázis, néhányat pedig ROSSZUL – ami a
 * kettő közül a súlyosabb, mert a hibás szám csendben bekerül a naplóba.
 */
public class FoodsFitnessTest {

    private static String names(String q) {
        StringBuilder sb = new StringBuilder();
        for (Foods.Hit h : Foods.parse(Arrays.asList(Foods.ALL), q))
            sb.append(sb.length() > 0 ? " + " : "").append(h.food.name);
        return sb.toString();
    }

    private static double kcal(String q) {
        double sum = 0;
        for (Foods.Hit h : Foods.parse(Arrays.asList(Foods.ALL), q)) {
            double g = h.grams > 0 ? h.grams : h.food.portion;
            sum += g * h.food.kcal100 / 100.0;
        }
        return sum;
    }

    @Test public void sugarFreeIsNotSugar() {
        // A „cukor” szótő beleesett a „cukormentes” szóba, és 40 kcal cukrot
        // adott hozzá – pont az ellenkezőjét annak, amit a felhasználó írt.
        assertEquals("Cukormentes / light", names("cukormentes rágó"));
        assertEquals(0, kcal("cukormentes rágó"), 0.01);
        assertEquals(0, kcal("kóla zero"), 0.01);
        // A „cukormentes kóla” a cukros üdítőt is kiváltja, nem melléteszi.
        assertEquals("Cukormentes / light", names("cukormentes kóla"));
        // A sima cukor viszont maradjon cukor.
        assertEquals("Cukor", names("két kanál cukor"));
        assertTrue(kcal("kóla") > 100);
    }

    @Test public void withoutSugarIsNotSugar() {
        // A leggyakoribb magyar alak – és eddig 40 kcal cukrot adott hozzá,
        // vagyis pont az ellenkezőjét annak, amit a felhasználó írt.
        assertEquals("Kávé (fekete) + Cukormentes / light", names("kávé cukor nélkül"));
        assertTrue("a cukormentes kávé nem lehet 40 kcal: " + kcal("kávé cukor nélkül"),
                kcal("kávé cukor nélkül") < 10);
        assertEquals("Cukormentes / light", names("cukrozatlan"));
        assertEquals("Cukormentes / light", names("édesítővel"));
        // Cukorral viszont marad a cukor.
        assertEquals("Kávé (fekete) + Cukor", names("kávé cukorral"));
    }

    @Test public void pepperIsNotWine() {
        // A „borssal" egy pohár bort naplózott: a „bor" szótő beleesett, és
        // minden borsozott étel mellé 120 kcal ital került. A bors saját,
        // nulla kalóriás tétel lett – a hosszabb tő nyer.
        assertEquals("Bors (fűszer)", names("borssal"));
        assertEquals("Marhahús + Bors (fűszer)", names("marhahús borssal"));
        assertEquals("Bors (fűszer)", names("sózva borsozva"));
        assertEquals(0, kcal("borssal"), 0.01);
        // A bor, a borsó és az uborka nem sérülhet.
        assertEquals("Bor (vörös/fehér)", names("ittam egy pohár bort"));
        assertEquals("Borsó", names("borsó"));
        assertEquals("Uborka", names("uborka"));
    }

    @Test public void assimilatedInstrumentalsAreFound() {
        // A -val/-vel hasonul: a szóvégi mássalhangzó megkettőződik. A cs-végű
        // szavaknál a „kalacs" tő nem található meg a „kalaccsal" alakban.
        assertEquals("Kalács / bejgli", names("kaláccsal"));
        assertEquals("Narancs", names("naranccsal"));
        assertEquals("Palacsinta + Kalács / bejgli", names("palacsintával kaláccsal"));
        // A teljes cs/sz/ny-végű kör: mindegyik hasonult alak a saját ételét adja.
        assertEquals("Kürtőskalács", names("kürtőskaláccsal"));
        assertEquals("Szendvics", names("szendviccsel"));
        assertEquals("Ananász", names("ananásszal"));
        assertEquals("Keksz", names("keksszel"));
        assertEquals("Kuszkusz (főtt)", names("kuszkusszal"));
        assertEquals("Sütemény", names("süteménnyel"));
        assertEquals("Péksütemény", names("péksüteménnyel"));
        // A már működő hasonulások sem romolhatnak el.
        assertEquals("Kolbász", names("kolbásszal"));
        assertEquals("Rizs (főtt)", names("rizzsel"));
        assertEquals("Méz", names("mézzel"));
    }

    @Test public void kebabIsNotBeans() {
        // A „kebab” szóban benne van a „bab”: eddig főtt bab lett belőle.
        assertEquals("Kebab", names("kebab"));
        assertEquals("Bab (főtt)", names("bab"));
    }

    @Test public void proteinPowderIsNotAReadyShake() {
        // 30 g por ~114 kcal; a kész turmix 100 kcal/100 g-jával 30 kcal lenne.
        assertEquals("Fehérjepor", names("30 g fehérjepor"));
        assertTrue("a fehérjepor kalóriája irreális: " + kcal("30 g fehérjepor"),
                kcal("30 g fehérjepor") > 90 && kcal("30 g fehérjepor") < 140);
        assertEquals("Fehérjepor", names("protein por"));
        assertEquals("Fehérjepor", names("tejsavófehérje"));
        // A kész turmix külön tétel marad.
        assertEquals("Protein turmix", names("protein turmix"));
        assertEquals("Proteinszelet", names("proteinszelet"));
    }

    @Test public void chickpeasAreNotGreenPeas() {
        assertEquals("Csicseriborsó (főtt)", names("csicseriborsó"));
        assertEquals("Borsó", names("borsó"));
        assertTrue(kcal("csicseriborsó") > kcal("borsó"));
    }

    @Test public void theEverydayFitnessBasicsAreKnown() {
        String[] words = {
                "fehérjepor", "proteinszelet", "csicseriborsó", "kuszkusz", "hajdina",
                "darált hús", "kelbimbó", "margarin", "chia mag", "lenmag", "kesudió",
                "sportital", "izotóniás ital", "kebab", "cukormentes kóla",
        };
        StringBuilder missing = new StringBuilder();
        for (String w : words)
            if (Foods.parse(Arrays.asList(Foods.ALL), w).isEmpty())
                missing.append("\n  ").append(w);
        assertTrue("nem ismeri fel:" + missing, missing.length() == 0);
    }

    @Test public void twoWordDishesAreOneItem() {
        // Két szóból álló, de EGY ételt jelentő nevek. A szón belüli szabály itt
        // nem segít, ezért a teljes alaknak kell szótőnek lennie.
        assertEquals("Rizsszelet / puffasztott rizs", names("puffasztott rizs"));
        assertEquals("Pogácsa", names("sajtos pogácsa"));
    }

    @Test public void flakesAndFlourAreNotTheCookedGrain() {
        // A pehely és a liszt szárazon négyszer annyi, mint a főtt gabona.
        assertEquals("Rizspehely", names("rizspehely"));
        assertEquals("Kukoricapehely", names("kukoricapehely"));
        assertEquals("Kukoricapehely", names("cornflakes"));
        assertEquals("Liszt", names("rizsliszt"));
        // A főtt változat marad, ami volt.
        assertEquals("Rizs (főtt)", names("rizs"));
        assertEquals("Kukorica", names("kukorica"));
    }

    @Test public void aDishAndItsOwnIngredientCountOnce() {
        // A rántotta tojásból van: a „3 tojásból rántotta” eddig a tojást ÉS a
        // rántottát is elszámolta – 526 kcal egy háromtojásos reggelire.
        assertEquals("Rántotta", names("3 tojásból rántotta"));
        assertEquals("Rántotta", names("tojásos rántotta"));
        assertEquals("Rántotta", names("omlett"));
        assertTrue("a háromtojásos rántotta irreális: " + kcal("3 tojásból rántotta"),
                kcal("3 tojásból rántotta") > 250 && kcal("3 tojásból rántotta") < 350);
        // A köret viszont nem tűnhet el mellőle.
        assertEquals("Rántotta + Kenyér", names("rántotta és egy szelet kenyér"));
        // A sima tojás továbbra is tojás.
        assertEquals("Tojás", names("2 tojás"));
    }

    @Test public void dessertsAndSundayDishesResolveToOneItem() {
        // Vendéglős kör: a „gesztenyepüré" burgonyapüré volt (köret, édesség
        // helyett), a „rakott kelkáposzta" rakott krumpli + káposzta, a
        // „meggyes rétes" pedig gyümölcs + rétes duplán.
        assertEquals("Gesztenyepüré", names("gesztenyepüré"));
        assertEquals("Rakott kelkáposzta", names("rakott kelkáposzta"));
        assertEquals("Rétes", names("meggyes rétes"));
        assertEquals("Rétes", names("túrós rétes"));
        // A szomszédok maradnak: a burgonyapüré és a rakott krumpli él.
        assertEquals("Burgonyapüré", names("krumplipüré"));
        assertEquals("Rakott krumpli", names("rakott krumpli"));
        // Új tételek megtalálják magukat.
        assertEquals("Tiramisu", names("tiramisu"));
        assertEquals("Krémes", names("krémes"));
        assertEquals("Fánk / churros", names("fánk"));
        assertEquals("Fánk / churros", names("churros"));
        assertEquals("Hekk (sült) + Sült krumpli", names("hekk sült krumplival"));
    }

    @Test public void restaurantAndStreetFoodIsKnown() {
        // Egy 57 neves éttermi próbából 18 hiányzott – a leggyakoribbak pótolva.
        String[] known = {"cordon bleu", "brassói aprópecsenye", "cigánypecsenye",
                "hot dog", "hot-dog", "ramen", "pad thai", "burrito", "quesadilla", "falafel",
                "hummusz", "limonádé", "fröccs", "kombucha", "ayran"};
        StringBuilder missing = new StringBuilder();
        for (String w : known)
            if (Foods.parse(Arrays.asList(Foods.ALL), w).isEmpty())
                missing.append("\n  ").append(w);
        assertTrue("nem ismeri fel:" + missing, missing.length() == 0);
        // Márkás csokik és a vadász: a márkanév is szótő.
        assertEquals("Csokoládé", names("kinder"));
        assertEquals("Csokoládé", names("milka"));
        assertEquals("Pálinka / tömény", names("jägermeister"));
    }

    @Test public void saladsAreNotEightCalories() {
        // A görög saláta feta és olaj: nem 8 kcal-os zöldsaláta. A cézár pedig
        // duplán számolt (csirkés saláta + zöldsaláta).
        assertEquals("Görög saláta", names("görög saláta"));
        assertTrue(kcal("görög saláta") > 150);
        assertEquals("Csirkés saláta", names("cézár saláta"));
        assertEquals("Tonhalsaláta", names("tonhalsaláta"));
        // A sima saláta és a görög joghurt marad, ami volt.
        assertEquals("Saláta (zöld)", names("saláta"));
        assertEquals("Görög joghurt", names("görög joghurt"));
        assertEquals("Tonhal", names("tonhal"));
    }

    @Test public void toastAndPancakeDishesKeepTheirParts() {
        // Az „avokádós pirítós" fele eltűnt: a pirítós nem volt szótő.
        assertEquals("Avokádó + Kenyér", names("avokádós pirítós"));
        assertEquals("Kenyér", names("bagett"));
        // A hortobágyi palacsinta húsos főfogás, nem édes palacsinta – és
        // nem is kettő (Hortobágyi + Palacsinta).
        assertEquals("Hortobágyi palacsinta", names("hortobágyi palacsinta"));
        assertEquals("Palacsinta", names("palacsinta"));
        // A curry a rizs mellett külön étel.
        assertEquals("Curry + Rizs (főtt)", names("curry rizzsel"));
    }

    @Test public void theCanteenClassicsResolveCorrectly() {
        // Menza-kör: a sóska semmi volt, a kelkáposzta főzelék kettőnek
        // számolt, a mákos tészta mákja eltűnt, a rántott karfiol pedig
        // 38 kcal-os NYERS karfiol lett.
        assertEquals("Főzelék", names("sóska"));
        assertEquals("Főzelék", names("sóskafőzelék"));
        assertEquals("Főzelék", names("kelkáposzta főzelék"));
        assertEquals("Mákos tészta", names("mákos tészta"));
        assertEquals("Tarhonyás hús", names("tarhonyás hús"));
        assertEquals("Grenadírmars (krumplis tészta)", names("grenadírmars"));
        assertEquals("Grenadírmars (krumplis tészta)", names("krumplis tészta"));
        assertEquals("Rántott zöldség", names("rántott karfiol"));
        assertTrue("a rántott karfiol nem nyers karfiol",
                kcal("rántott karfiol") > 200);
        // A szomszédok élnek: tökfőzelék, sima tészta, nyers karfiol, guba.
        assertEquals("Tökfőzelék", names("tökfőzelék"));
        assertEquals("Tészta (főtt)", names("tészta"));
        assertEquals("Karfiol", names("karfiol"));
        assertEquals("Mákos guba", names("mákos guba"));
        assertEquals("Rakott kelkáposzta", names("rakott kelkáposzta"));
    }

    @Test public void breakfastDrinksAndFruitsResolveCorrectly() {
        // Reggeli-kör: a „turmix" fehérjeturmixnak számított, az „aszalt
        // szilva" friss szilvának (négyszeres különbség), a „gyümölcspüré"
        // burgonyapürének, a „gránátalma" almának.
        assertEquals("Gyümölcsturmix / smoothie", names("turmix"));
        assertEquals("Gyümölcsturmix / smoothie", names("smoothie"));
        assertEquals("Protein turmix", names("protein turmix"));
        assertEquals("Aszalt gyümölcs", names("aszalt szilva"));
        assertEquals("Aszalt gyümölcs", names("mazsola"));
        assertTrue("az aszalt szilva nem friss szilva",
                kcal("aszalt szilva") > 2 * kcal("szilva"));
        assertEquals("Gyümölcspüré / bébiétel", names("gyümölcspüré"));
        assertEquals("Gránátalma", names("gránátalma"));
        assertEquals("Kakaó (tejes)", names("forró csoki"));
        assertEquals("Tejberizs", names("tejberizs"));
        assertEquals("Tejbegríz", names("tejbedara"));
        assertEquals("Zabpehely", names("kása"));
        // Bogyósok és déligyümölcsök.
        assertEquals("Bogyós gyümölcs", names("szeder"));
        assertEquals("Bogyós gyümölcs", names("ribizli"));
        assertEquals("Őszibarack", names("nektarin"));
        assertEquals("Füge", names("füge"));
        assertEquals("Befőtt / kompót", names("kompót"));
        assertEquals("Tea (cukrozatlan)", names("matcha"));
        // A szomszédok élnek: szilva, alma, rizs, csoki, burgonyapüré.
        assertEquals("Szilva", names("szilva"));
        assertEquals("Alma", names("alma"));
        assertEquals("Rizs (főtt)", names("rizs"));
        assertEquals("Csokoládé", names("csoki"));
        assertEquals("Burgonyapüré", names("krumplipüré"));
    }

    @Test public void plainVegetablesAreAKnownSide() {
        // A „vacsorára túró és zöldség" zöldsége eddig eltűnt a naplóból.
        assertEquals("Túró + Zöldség (vegyes / párolt)", names("vacsorára túró és zöldség"));
        assertEquals("Zöldség (vegyes / párolt)", names("párolt zöldség"));
        // A zöldségleves EGY leves, a rántott zöldség EGY rántott étel.
        assertEquals("Leves (átlag)", names("zöldségleves"));
        assertEquals("Rántott zöldség", names("rántott zöldség"));
    }

    @Test public void waterIsUnderstoodAsZeroCalories() {
        // Az „ittam 1,5 liter vizet" ne legyen „nem értem" – nulla kalória,
        // de a napló teljesebb tőle.
        assertEquals("Víz / ásványvíz", names("víz"));
        assertEquals("Víz / ásványvíz", names("ittam másfél liter vizet"));
        assertEquals("Víz / ásványvíz", names("szódavíz"));
        assertEquals("Víz / ásványvíz", names("ásványvíz"));
        assertEquals(0, kcal("2 liter víz"), 0.01);
        // A cukros ital nem lett víz.
        assertTrue(kcal("kóla") > 100);
    }

    @Test public void saucesAreNotTheirMainIngredient() {
        // A „szójaszósz" szójakockának számított: 204 kcal egy löttyintésnyi
        // ~6 helyett. Az összetett nevek nem esnek két ételre.
        assertEquals("Szójaszósz", names("szójaszósz"));
        assertTrue(kcal("szójaszósz") < 20);
        assertEquals("Szósz / mártás", names("sajtszósz"));
        assertEquals("Szósz / mártás", names("fokhagymaszósz"));
        assertEquals("Szósz / mártás", names("csípős szósz"));
        assertEquals("Tartármártás", names("tartármártás"));
        assertEquals("Pesto", names("pesto"));
        assertEquals("Guacamole", names("guacamole"));
        assertEquals("Tzatziki", names("tzatziki"));
        assertEquals("Balzsamecet", names("balzsamecet"));
        // A szomszédok élnek: szójakocka, sajt, hagyma, öntet.
        assertEquals("Szójakocka", names("szójakocka"));
        assertEquals("Sajt (trappista)", names("sajt"));
        assertEquals("Hagyma", names("hagyma"));
        assertEquals("Joghurtos öntet", names("salátaöntet"));
        assertEquals("Sült krumpli + Majonéz", names("sült krumpli majonézzel"));
    }

    @Test public void snacksFastFoodAndPastaResolveCorrectly() {
        // Nassolás-kör: a hagymakarika 20 kcal-os nyers hagymának, a
        // szaloncukor kanál cukornak, a mézeskalács sima kalácsnak számított.
        assertEquals("Hagymakarika (rántott)", names("hagymakarika"));
        assertTrue(kcal("hagymakarika") > 200);
        assertEquals("Szaloncukor", names("szaloncukor"));
        assertEquals("Mézeskalács", names("mézeskalács"));
        assertEquals("Aszalt gyümölcs", names("aszalt vörösáfonya"));
        // Gyorskaja és márkák.
        assertEquals("Hamburger", names("big mac"));
        assertEquals("Gyorséttermi menü", names("mcmenü"));
        assertEquals("Csirkenugget", names("csirkefalatok"));
        assertEquals("Chips", names("nachos"));
        assertEquals("Csokoládé", names("snickers"));
        assertEquals("Keksz", names("oreo"));
        // Új nassok, halak, tészták.
        assertEquals("Perec", names("sós perec"));
        assertEquals("Ropi / kréker", names("ropi"));
        assertEquals("Ropi / kréker", names("sajtos tallér"));
        assertEquals("Pisztácia", names("pisztácia"));
        assertEquals("Makréla / szardínia", names("szardínia"));
        assertEquals("Lasagne", names("lasagne"));
        assertEquals("Tészta carbonara", names("carbonara"));
        assertEquals("Töltött tészta (tortellini)", names("ravioli"));
        assertEquals("Péksütemény", names("briós"));
        // Darabra is: az „5 szaloncukor" öt szemet jelent.
        // A szomszédok élnek.
        assertEquals("Hagyma", names("hagyma"));
        assertEquals("Cukor", names("két kanál cukor"));
        assertEquals("Kalács / bejgli", names("mákos bejgli"));
        assertEquals("Áfonya", names("áfonya"));
        assertEquals("Tészta (főtt)", names("tészta"));
    }

    @Test public void theMealExampleSentencesAllParse() {
        // Ugyanezek a minták váltakoznak az étel-beviteli mezőben – ha egy
        // példa nem működne, pont a mintamondat járatná le a felismerést.
        String[] examples = {
                "150 g csirkemell rizzsel",
                "2 tojás és egy pirítós vajjal",
                "fél adag gyros",
                "tegnap este pizzát ettem",
                "két korsó sör és egy hamburger",
                "negyvenöt gramm sajt",
                "ittam fél liter vizet",
                "két és fél deci tej müzlivel",
        };
        StringBuilder bad = new StringBuilder();
        for (String e : examples)
            if (Foods.parse(Arrays.asList(Foods.ALL), e).isEmpty())
                bad.append("\n  ").append(e);
        assertTrue("a mintamondat nem érthető:" + bad, bad.length() == 0);
        // Egy-egy jellemző részlet is stimmel.
        assertEquals(150, Foods.parse(Arrays.asList(Foods.ALL),
                examples[0]).get(0).grams, 0.01);
        assertEquals(45, Foods.parse(Arrays.asList(Foods.ALL),
                examples[5]).get(0).grams, 0.01);
    }

    @Test public void spreadsOilsAndOlivesResolveCorrectly() {
        // A „humusz" (egy m-mel) egy tányér főtt csicseriborsónak számított,
        // az „olajbogyó" tiszta olajnak, a „szendvicskrém" egész szendvicsnek.
        assertEquals("Hummusz", names("humusz"));
        assertEquals("Olajbogyó / olívabogyó", names("olajbogyó"));
        assertEquals("Olajbogyó / olívabogyó", names("olívabogyó"));
        assertEquals("Szendvicskrém / kence", names("szendvicskrém"));
        assertEquals("Szendvicskrém / kence", names("tojáskrém"));
        assertEquals("Magvaj (mandula/kesu/tahini)", names("mandulavaj"));
        assertEquals("Magvaj (mandula/kesu/tahini)", names("tahini"));
        assertEquals("Nutella", names("mogyorókrém"));
        assertEquals("Szirup (juhar/agavé)", names("juharszirup"));
        assertEquals("Makréla / szardínia", names("sprotni"));
        assertEquals("Párizsi / felvágott", names("löncshús"));
        // A „napraforgó olaj" nem mag + olaj kettő.
        assertEquals("Olaj", names("napraforgó olaj"));
        // A szomszédok élnek.
        assertEquals("Olaj", names("olívaolaj"));
        assertEquals("Csicseriborsó (főtt)", names("csicseriborsó"));
        assertEquals("Mogyoró", names("mogyoró"));
        assertEquals("Vaj", names("vaj"));
    }

    @Test public void lightAndWholegrainVariantsResolveCorrectly() {
        // A „durum tészta" PÁLINKÁT is számolt (a „durum"-ban benne a „rum").
        assertEquals("Tészta (főtt)", names("durum tészta"));
        // Zsírszegény és light: a fele-harmada kalória, nem a teljes.
        assertEquals("Zsírszegény tej", names("zsírszegény tej"));
        assertEquals("Zsírszegény tej", names("sovány tej"));
        assertEquals("Light majonéz", names("light majonéz"));
        // A zöldségtészták nem búzatészták.
        assertEquals("Cukkini", names("cukkini spagetti"));
        assertEquals("Konjac / shirataki tészta", names("konjac tészta"));
        assertTrue(kcal("konjac tészta") < 30);
        // A sima tej, majonéz és tészta marad, ami volt.
        assertEquals("Tej", names("tej"));
        assertEquals("Majonéz", names("majonéz"));
        assertEquals("Tészta (főtt)", names("tészta"));
        // A laktózmentes tej kalóriája a rendesé – az jó, hogy Tej marad.
        assertEquals("Tej", names("laktózmentes tej"));
    }

    @Test public void dairyDessertsAndCheesesResolveCorrectly() {
        // A „parmezán" MÉZNEK számított (a „mez" tő beleesett), a tejszelet
        // és a madártej sima tejnek, a milkshake fehérjeturmixnak.
        assertEquals("Parmezán", names("parmezán"));
        assertEquals("Tejszelet", names("tejszelet"));
        assertEquals("Madártej", names("madártej"));
        assertEquals("Tejszínhab", names("tejszínhab"));
        assertEquals("Milkshake", names("milkshake"));
        assertEquals("Milkshake", names("tejturmix"));
        assertEquals("Puding", names("vaníliapuding"));
        assertEquals("Puding", names("csokipuding"));
        assertEquals("Krémtúró / túródesszert", names("krémtúró"));
        assertEquals("Ivójoghurt", names("actimel"));
        assertEquals("Camembert / brie", names("camembert"));
        assertEquals("Feta", names("feta"));
        assertEquals("Mascarpone", names("mascarpone"));
        // A szomszédok élnek: méz, tej, túró, joghurt, csoki.
        assertEquals("Méz", names("méz"));
        assertEquals("Tej", names("tej"));
        assertEquals("Túró", names("túró"));
        assertEquals("Joghurt", names("joghurt"));
        assertEquals("Csokoládé", names("csoki"));
    }

    @Test public void everyFoodsOwnNameResolvesToItself() {
        // Önellenőrzés: minden tétel SAJÁT neve (a zárójeles minősítés és a
        // „/" változatok nélkül) önmagára essen – ha a kalóriatáblázatból
        // kimásolt név mást adna, az némán hibás naplót jelentene.
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            String q = f.name.replaceAll("\\(.*\\)", "").split("/")[0].trim();
            List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
            if (hs.size() == 1 && hs.get(0).food.name.equals(f.name)) continue;
            bad.append("\n  ").append(q);
        }
        assertTrue("nem önmagára esik:" + bad, bad.length() == 0);
        // A javított esetek: a „tészta carbonara" nem duplázik, a
        // „pacalpörkölt" nem sima pörkölt.
        assertEquals("Tészta carbonara", names("tészta carbonara"));
        assertEquals("Pacalpörkölt", names("pacalpörkölt"));
        assertEquals("Töltött tészta (tortellini)", names("töltött tészta"));
        assertEquals("Tükörponty / halrudak", names("tükörponty"));
        assertEquals("Bogyós gyümölcs", names("bogyós gyümölcs"));
    }

    @Test public void juicesAreNotTheFruitItself() {
        // A „narancslé" 71 kcal-os narancsnak, a „2 dl almalé" 200 g almának
        // számított – a lé folyadék, a maga kalóriájával.
        assertEquals("Gyümölcslé", names("narancslé"));
        assertEquals("Gyümölcslé", names("almalé"));
        assertEquals("Gyümölcslé", names("friss facsart narancslé"));
        assertEquals("Gyümölcslé", names("cappy"));
        assertEquals("Szörp (hígítva)", names("málnaszörp"));
        assertEquals("Szörp (hígítva)", names("bodzaszörp"));
        assertEquals("Citromlé", names("citromlé"));
        assertTrue(kcal("citromlé") < 15);
        // A gyümölcsök maradnak gyümölcsök.
        assertEquals("Narancs", names("narancs"));
        assertEquals("Alma", names("alma"));
        assertEquals("Málna", names("málna"));
        assertEquals("Sárgarépa", names("répa"));
    }

    @Test public void cocktailsAndSpiritsAreKnown() {
        // A „vilmoskörte" pálinka volt gyümölcs helyett – illetve fordítva:
        // 86 kcal-os körtének számított.
        assertEquals("Pálinka / tömény", names("vilmoskörte"));
        assertEquals("Pálinka / tömény", names("rum"));
        assertEquals("Pálinka / tömény", names("unicum"));
        assertEquals("Koktél / long drink", names("gin tonik"));
        assertEquals("Koktél / long drink", names("mojito"));
        assertEquals("Koktél / long drink", names("aperol spritz"));
        assertEquals("Pezsgő", names("prosecco"));
        assertEquals("Cider", names("cider"));
        assertEquals("Sör", names("radler"));
        // A bolti jeges tea cukros, nem 3 kcal-os tea.
        assertEquals("Üdítő (cukros)", names("jeges tea"));
        assertTrue(kcal("jeges tea") > 100);
        // A szomszédok élnek: körte, krumpli, tea – és a „rum" nem esik a
        // krumpliba (a hosszabb tő elnyeli).
        assertEquals("Körte", names("körte"));
        assertEquals("Burgonya (főtt)", names("krumpli"));
        assertEquals("Tea (cukrozatlan)", names("tea"));
    }

    @Test public void pastriesResolveCorrectly() {
        // A „kakaós palacsinta" egy bögre tejes kakaót is számolt a kanálnyi
        // töltelék helyett; a klasszikus cukrászsütik fele hiányzott.
        assertEquals("Palacsinta", names("kakaós palacsinta"));
        assertEquals("Sütemény", names("zserbó"));
        assertEquals("Sütemény", names("rigó jancsi"));
        assertEquals("Sütemény", names("mignon"));
        assertEquals("Keksz", names("linzer"));
        assertEquals("Muffin / brownie", names("muffin"));
        assertEquals("Muffin / brownie", names("brownie"));
        assertEquals("Gofri", names("gofri"));
        assertEquals("Kürtőskalács", names("trdelník"));
        assertEquals("Energiagolyó", names("kókuszgolyó"));
        assertEquals("Energiagolyó", names("zabgolyó"));
        // A szomszédok élnek: a kakaó ital, a zab zab, a torta sütemény.
        assertEquals("Kakaó (tejes)", names("kakaó"));
        assertEquals("Zabpehely", names("zab"));
        assertEquals("Sütemény", names("egy szelet torta"));
        assertEquals("Fánk / churros", names("képviselőfánk"));
    }

    @Test public void steakIsNotTeaAndFishAreKnown() {
        // A „steak" szóban benne van a „tea": a „tofu steak" egy csésze teát
        // is naplózott. A hosszabb tő elnyeli.
        assertEquals("Tofu + Marhahús", names("tofu steak"));
        assertEquals("Marhahús", names("steak"));
        assertEquals("Tea (cukrozatlan)", names("tea"));
        // Hazai halak és tenger gyümölcsei.
        assertEquals("Hal (fehér)", names("pisztráng"));
        assertEquals("Hal (fehér)", names("sült ponty"));
        assertEquals("Hal (fehér)", names("harcsa"));
        assertEquals("Tenger gyümölcsei", names("garnéla"));
        assertEquals("Tenger gyümölcsei", names("kagyló"));
        // A harcsapaprikás EGY fogás, nem hal + paprikás.
        assertEquals("Csirkepaprikás", names("harcsapaprikás"));
        // Növényi kör.
        assertEquals("Edamame", names("edamame"));
        assertEquals("Seitan", names("seitan"));
        assertEquals("Tempeh", names("tempeh"));
        assertEquals("Kókusztej", names("kókusztej"));
        assertEquals("Növényi tej (mandula/zab)", names("zabtej"));
    }

    @Test public void saladsAndVegetablesResolveCorrectly() {
        // A franciasaláta majonézes (~270 kcal), nem 8 kcal-os zöldsaláta.
        assertEquals("Franciasaláta / coleslaw", names("franciasaláta"));
        assertTrue(kcal("franciasaláta") > 200);
        assertEquals("Franciasaláta / coleslaw", names("coleslaw"));
        // A „cékla saláta" külön írva sem esik kettőre.
        assertEquals("Céklasaláta", names("cékla saláta"));
        assertEquals("Savanyúság", names("savanyúság"));
        assertEquals("Savanyúság", names("savanyú káposzta"));
        // Alap zöldségek, amik eddig hiányoztak.
        assertEquals("Spárga", names("spárga"));
        assertEquals("Karalábé", names("karalábé"));
        assertEquals("Retek", names("retek"));
        assertEquals("Zeller", names("zeller"));
        assertEquals("Édesburgonya", names("sült batáta"));
        assertEquals("Zöldség (vegyes / párolt)", names("vitaminsaláta"));
        // A szomszédok élnek.
        assertEquals("Saláta (zöld)", names("saláta"));
        assertEquals("Káposzta", names("káposzta"));
        assertEquals("Uborka", names("kovászos uborka"));
    }

    @Test public void porkDelicaciesAreNotFruitOrCheese() {
        // A „tepertő" 33 kcal-os EPERNEK számított (az „eper" tő beleesett),
        // a disznósajt trappistának, a májkrém nyers csirkemájnak.
        assertEquals("Tepertő", names("tepertő"));
        assertTrue("a tepertő nem eper", kcal("tepertő") > 300);
        assertEquals("Disznósajt", names("disznósajt"));
        assertEquals("Májkrém / kenőmájas", names("májkrém"));
        assertEquals("Májkrém / kenőmájas", names("kenőmájas"));
        assertEquals("Májkrém / kenőmájas", names("pástétom"));
        assertEquals("Pacalpörkölt", names("pacal"));
        assertEquals("Fasírt", names("stefánia vagdalt"));
        assertEquals("Sertéskaraj", names("tarja"));
        assertEquals("Marhahús", names("bélszín"));
        assertEquals("Csirkecomb", names("csirkeszárny"));
        // A szomszédok élnek: az eper eper, a sajt sajt, a máj máj.
        assertEquals("Eper", names("eper"));
        assertEquals("Sajt (trappista)", names("sajt"));
        assertEquals("Csirkemáj", names("csirkemáj"));
        assertEquals("Bacon", names("szalonna"));
    }

    @Test public void theNewEntriesDidNotBreakTheirNeighbours() {
        // A hosszabb szótő elnyeli a rövidebbet – ellenőrizzük, hogy tényleg
        // egy tétel lesz belőlük, nem kettő.
        List<String> singles = Arrays.asList("csicseriborsó", "kesudió", "kebab",
                "tejsavófehérje", "proteinszelet", "cukormentes kóla", "energiaszelet");
        for (String s : singles)
            assertEquals("két ételre esett szét: " + s + " -> " + names(s),
                    1, Foods.parse(Arrays.asList(Foods.ALL), s).size());
        // A régi jelentések megmaradtak.
        assertEquals("Dió", names("dió"));
        assertEquals("Gyros", names("gyros"));
    }

    /** A márkanév is menü: „mekis kaja", „mcdonalds", „kfc kosár". */
    @Test public void brandNamesMeanTheUsualTray() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"mekis kaja", "mcdonalds", "kfc kosár"})
            assertEquals(q, "Gyorséttermi menü", Foods.parse(all, q).get(0).food.name);
        assertEquals("Rántott csirkemell",
                Foods.parse(all, "kfc csirkecsíkok").get(0).food.name);
    }
}
