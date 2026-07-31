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
}
