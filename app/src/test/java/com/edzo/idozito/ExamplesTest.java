package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Az appban hirdetett példamondatok tényleg működnek.
 *
 * A tipp-szöveg ígéret: aki elolvassa, azt hiszi, hogy pont úgy is beírhatja.
 * Ha egy példa csendben elromlik – átnevezünk egy ételt, szigorítunk egy
 * mintán –, akkor az app maga bíztat valamire, amit aztán nem ismer fel.
 * Ez a teszt ezt fogja meg.
 */
public class ExamplesTest {

    /** Péntek dél: a hétköznap/hétvége példák így értelmesek. */
    private static long friday() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(2026, java.util.Calendar.AUGUST, 7, 12, 0, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Test public void everyMealExampleIsRecognised() {
        for (String q : Examples.MEAL) {
            List<Foods.Hit> hits = Foods.parse(Arrays.asList(Foods.ALL), q);
            // A kimondott kalória („vacsora 650 kcal") étel-felismerés nélkül
            // is teljes értékű bejegyzés – a példa akkor is működik.
            assertTrue("nem ismeri fel a saját példáját: " + q,
                    !hits.isEmpty() || Kcal.stated(q) > 0);
            for (Foods.Hit h : hits) {
                double g = h.grams > 0 ? h.grams : h.food.portion;
                double kcal = g * h.food.kcal100 / 100.0;
                // A víz nulla kalória – az is érvényes találat.
                assertTrue("életszerűtlen kalória: " + q + " → " + Math.round(kcal),
                        kcal >= 0 && kcal <= 3000);
                assertTrue("adag nélküli étel: " + q, g > 0);
            }
        }
    }

    @Test public void everyBulkExampleIsRecognised() {
        for (String q : Examples.BULK) {
            Activities.Parsed p = Activities.parse(q, friday());
            boolean lifts = !StrengthParse.parse(q).isEmpty();
            assertTrue("sem edzésként, sem sorozatként nem érti: " + q,
                    lifts || !p.isEmpty());
        }
    }

    @Test public void everySetExampleIsRecognised() {
        for (String q : Examples.SET) {
            List<StrengthParse.Item> items = StrengthParse.parse(q);
            assertTrue("nem ismeri fel a saját példáját: " + q, !items.isEmpty());
            for (StrengthParse.Item it : items)
                assertTrue("üres sorozat: " + q, !it.sets.isEmpty());
        }
    }

    @Test public void multiExerciseExamplesReallyGiveSeveralExercises() {
        // Ha a „két gyakorlat egy mondatban" példa egyre esik össze, akkor a
        // tipp a felismerés egy olyan képességét hirdeti, ami már nincs meg.
        assertEquals(2, StrengthParse.parse("guggolás 3x10 60 kg, fekvenyomás 3x8 50 kg").size());
        assertEquals(2, StrengthParse.parse("lábgép 3x12 80 kg és vádli 4x15").size());
        assertEquals(2, StrengthParse.parse("arnold nyomás 3x10 16 kg, oldalemelés 3x15 8 kg").size());
    }

    @Test public void everyIntervalExampleIsRecognised() {
        for (String q : Examples.INTERVAL) {
            IntervalParse.Plan p = IntervalParse.parse(q);
            assertTrue("nem ismeri fel a saját példáját: " + q, p != null);
            assertTrue("üres beállítás: " + q, p.rounds >= 1 && p.work >= 5);
            assertTrue("életszerűtlen hossz: " + q, p.totalSec() <= 4 * 3600);
        }
    }

    /** A mérés-példák is azt jelentik, aminek látszanak. */
    @Test public void everyBodyExampleIsRecognised() {
        for (String q : Examples.BODY) {
            BodyParse.Body b = BodyParse.parse(q);
            // Az alvás-példa is a Profil mezőjében él – azt a saját
            // felismerője érti.
            assertTrue("nem ismeri fel a saját példáját: " + q,
                    !b.isEmpty() || Sleep.parse(q) > 0);
            if (b.isEmpty()) continue;
            assertTrue("életszerűtlen mérés: " + q,
                    (b.kg == 0 || (b.kg >= 30 && b.kg <= 250))
                            && (b.fatPct == 0 || (b.fatPct >= 3 && b.fatPct <= 60)));
        }
    }

    /** A hirdetett edzésnapok tényleg edzésnapként olvashatók vissza. */
    @Test public void everyRoutineExampleIsRecognised() {
        for (String q : Examples.ROUTINE) {
            Routines.Routine r = Routines.parseShared(q);
            assertTrue("nem ismeri fel a saját példáját: " + q, r != null);
            assertTrue("kevés gyakorlat: " + q, r.moves.size() >= 2);
        }
    }

    /** A panasz- és cél-példák mind találnak testtájat – és a router is oda küldi őket. */
    @Test public void everyRehabExampleFindsAnArea() {
        for (String q : Examples.REHAB) {
            assertTrue("nem talál testtájat: " + q,
                    Rehab.forComplaint(q) != null || Rehab.forGoal(q) != null);
            assertEquals("nem a rehabhoz fut be: " + q, Sentence.Kind.REHAB,
                    Sentence.of(q, Arrays.asList(Foods.ALL), friday()));
        }
    }

    /**
     * Minden hirdetett példa a SAJÁT naplójához fut be az útbaigazítón.
     *
     * Három dokumentált kivétellel: a BULK-beli súlyzós mondat az erősítőé
     * (ez a jobb hely neki), a „hiit 20 perc" javasolt ritmusú terv edzésnek
     * számít (a ritmus a mi szavunk, nem a felhasználóé), és az alvás-példa
     * a mérés-mezőben lakik, de a saját felismerője viszi.
     */
    @Test public void everyAdvertisedExampleRoutesToItsOwnGroup() {
        java.util.List<Foods.Food> all = Arrays.asList(Foods.ALL);
        Object[][] groups = {
                {Examples.MEAL, Sentence.Kind.MEAL}, {Examples.BULK, Sentence.Kind.WORKOUT},
                {Examples.SET, Sentence.Kind.STRENGTH}, {Examples.INTERVAL, Sentence.Kind.INTERVAL},
                {Examples.BODY, Sentence.Kind.BODY}, {Examples.ROUTINE, Sentence.Kind.ROUTINE},
                {Examples.REHAB, Sentence.Kind.REHAB}};
        StringBuilder bad = new StringBuilder();
        for (Object[] g : groups) {
            Sentence.Kind want = (Sentence.Kind) g[1];
            for (String q : (String[]) g[0]) {
                Sentence.Kind got = Sentence.of(q, all, friday());
                if (want == Sentence.Kind.WORKOUT && got == Sentence.Kind.STRENGTH) continue;
                if (want == Sentence.Kind.INTERVAL && got == Sentence.Kind.WORKOUT) continue;
                if (want == Sentence.Kind.BODY && got == Sentence.Kind.SLEEP) continue;
                if (got != want) bad.append("\n  [").append(want).append("] ")
                        .append(q).append(" -> ").append(got);
            }
        }
        assertEquals("rossz ajtóhoz futó példa:" + bad, 0, bad.length());
    }

    /** A könyvtár minden csoportjához tartozik valódi példalista. */
    @Test public void everyLibraryGroupHasExamples() {
        for (String[] g : Examples.GROUPS) {
            String[] ex = Examples.byKey(g[2]);
            assertTrue("üres csoport: " + g[2], ex.length >= 3);
        }
    }

    @Test public void hintsAreWellFormedAndRotate() {
        for (String[] a : new String[][]{Examples.MEAL, Examples.BULK, Examples.SET,
                Examples.INTERVAL, Examples.BODY, Examples.ROUTINE}) {
            Set<String> seen = new HashSet<>();
            for (String s : a) {
                assertTrue("üres példa", s.trim().length() > 2);
                // A „pl." előtagot a hint teszi hozzá – ne legyen benne kétszer.
                assertTrue("dupla előtag: " + s, !s.startsWith("pl."));
                assertTrue("ismétlődő példa: " + s, seen.add(s));
            }
            // Egy óra alatt minden példa sorra kerül.
            Set<String> shown = new HashSet<>();
            for (int min = 0; min < a.length; min++)
                shown.add(Examples.pick(a, min * 60000L));
            assertEquals("nem forog körbe", a.length, shown.size());
            assertTrue(Examples.hint(a, 0L).startsWith("pl. "));
        }
        assertTrue(Examples.mealHint(0L).startsWith("Mit ettél? (pl. "));
        assertTrue(Examples.mealHint(0L).endsWith(")"));
        // Negatív időbélyeg se dobjon indexhibát.
        assertTrue(Examples.pick(Examples.SET, -1234567L).length() > 0);
    }

    @Test public void everyHintActuallyParses() {
        // A beviteli mezőben ezek a minták váltakoznak. Ha egy közülük nem
        // működne, pont a mintamondat járatná le a felismerést.
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String s : Examples.MEAL)
            assertTrue("étel-minta nem érthető: " + s,
                    !Foods.parse(all, s).isEmpty() || Kcal.stated(s) > 0);
        for (String s : Examples.SET)
            assertTrue("sorozat-minta nem érthető: " + s, !StrengthParse.parse(s).isEmpty());
        for (String s : Examples.INTERVAL)
            assertTrue("intervall-minta nem érthető: " + s, IntervalParse.parse(s) != null);
        for (String s : Examples.BULK) {
            // Egyetlen kivétel: a súlyzós mondat SZÁNDÉKOSAN szerepel az
            // edzés-mezőben is – ott az app az Erősítő naplót ajánlja fel,
            // ezért a mozgás-felismerő üresen tér vissza rá.
            if (!StrengthParse.parse(s).isEmpty()) continue;
            assertTrue("edzés-minta nem érthető: " + s, !Activities.parse(s).isEmpty());
        }
    }
    /**
     * A hirdetett példamondatok másképp GÉPELVE is ugyanazt jelentik.
     *
     * A tipp-szöveget az ember bemásolja vagy utánagépeli – nagybetűvel,
     * dupla szóközzel, a végén ponttal. Ha bármelyik alak mást ad, akkor az
     * app a saját példáját sem érti következetesen.
     */
    @Test public void everyExampleSurvivesADifferentTyping() {
        StringBuilder bad = new StringBuilder();
        for (String[] list : new String[][]{Examples.MEAL, Examples.BULK,
                Examples.SET, Examples.INTERVAL})
            for (String q : list) {
                String base = allFour(q);
                for (String v : new String[]{q.toUpperCase(new java.util.Locale("hu")),
                        "  " + q.replace(" ", "  ") + "  ", q + "!", q + "…",
                        q + ".", q + " :)"})
                    if (!allFour(v).equals(base))
                        bad.append("\n  ").append(v)
                           .append("\n     eredeti: ").append(base)
                           .append("\n     kapott:  ").append(allFour(v));
            }
        assertEquals("másképp gépelve mást jelent:" + bad, 0, bad.length());
    }

    /** Mind a négy felismerő eredménye egyetlen szövegként. */
    private static String allFour(String q) {
        StringBuilder s = new StringBuilder();
        for (StrengthParse.Item i : StrengthParse.parse(q)) s.append(i.label()).append(';');
        s.append('|');
        IntervalParse.Plan p = IntervalParse.parse(q);
        s.append(p == null ? "-" : p.rounds + "/" + p.work + "/" + p.rest).append('|');
        Activities.Parsed a = Activities.parse(q, friday());
        for (Activities.Plan x : a.plans)
            s.append(x.kind.id).append(x.count).append('/').append(x.minutes).append(';');
        s.append(a.days).append('+').append(a.offset).append('|');
        for (Foods.Hit h : Foods.parse(java.util.Arrays.asList(Foods.ALL), q))
            s.append(h.food.name).append('=').append(Math.round(h.grams)).append(';');
        return s.toString();
    }
}
