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
