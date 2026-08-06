package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Mátrix-próbák: MINDEN tétel × több mondatforma.
 *
 * A pontonkénti tesztek azt őrzik, amit egyszer már elrontottunk. Ezek mást:
 * azt, hogy a felismerés a teljes szótáron egyenletes legyen. Egy új étel vagy
 * gyakorlat felvételekor pont az derül ki belőlük, hogy a NEVE nem működik
 * minden alakban – például mert egy másik tétel hosszabb szótöve elnyeli.
 *
 * Kétezer mondat, együtt egy másodperc alatt.
 */
public class MatrixTest {

    private static final long NOW = 1_753_869_600_000L;
    private static final Locale HU = new Locale("hu");

    /**
     * Minden étel felismerhető kiírt grammal, öt szórendben.
     *
     * A gramm szándékosan egyértelmű: itt nem a mennyiség-becslést mérjük,
     * hanem azt, hogy a NÉV mindig célba ér – és pontosan egy tételt hoz.
     */
    @Test public void everyFoodIsFoundWithAnExplicitAmount() {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        String[] pat = {"150 g %s", "%s 150 g", "%s 15 dkg", "ettem 150 g %s-t",
                "ma 150 gramm %s"};
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            // A név „beszélt" alakja: a zárójeles pontosítás és a per-jeles
            // változat nélkül („Hal (fehér)" → „hal").
            String name = f.name.replaceAll("\\(.*?\\)", "").replaceAll("/.*", "")
                    .trim().toLowerCase(HU);
            for (String p : pat) {
                String q = String.format(p, name);
                List<Foods.Hit> h = Foods.parse(all, q);
                if (h.size() != 1) { bad.append("\n  ").append(q).append(" -> ")
                        .append(h.size()).append(" találat"); continue; }
                if (!h.get(0).food.name.equals(f.name))
                    bad.append("\n  ").append(q).append(" -> ").append(h.get(0).food.name);
                else if (Math.abs(h.get(0).grams - 150) > 0.01)
                    bad.append("\n  ").append(q).append(" -> ").append(h.get(0).grams).append(" g");
            }
        }
        assertEquals("étel-mátrix:" + bad, 0, bad.length());
    }

    /** Minden gyakorlat felismerhető a szokásos sorozat-írásmódokkal. */
    @Test public void everyExerciseIsFoundInEveryNotation() {
        String[][] pat = {
                {"%s 3x10 40 kg", "3", "10", "40"},
                {"3x10 %s 40 kg", "3", "10", "40"},
                {"%s 3 sorozat 10 ismétlés 40 kg", "3", "10", "40"},
                {"%s 3 szett 10 ism 40 kg", "3", "10", "40"},
                {"%s 4 sorozat 8", "4", "8", "0"},
                {"ma %s 3x10 40 kg-mal", "3", "10", "40"},
                {"%s 3x10x40", "3", "10", "40"},
        };
        StringBuilder bad = new StringBuilder();
        for (String m : StrengthParse.names())
            for (String[] p : pat) {
                String q = String.format(p[0], m.toLowerCase(HU));
                List<StrengthParse.Item> it = StrengthParse.parse(q);
                if (it.size() != 1) { bad.append("\n  ").append(q).append(" -> ")
                        .append(it.size()).append(" gyakorlat"); continue; }
                StrengthParse.Item i = it.get(0);
                if (!i.name.equals(m)) bad.append("\n  ").append(q).append(" -> ").append(i.name);
                else if (i.sets.size() != Integer.parseInt(p[1]))
                    bad.append("\n  ").append(q).append(" -> ").append(i.sets.size()).append(" sorozat");
                else if (i.sets.get(0).reps != Integer.parseInt(p[2]))
                    bad.append("\n  ").append(q).append(" -> ").append(i.sets.get(0).reps).append(" ism");
                else if (Math.abs(i.topWeight() - Double.parseDouble(p[3])) > 0.01)
                    bad.append("\n  ").append(q).append(" -> ").append(i.topWeight()).append(" kg");
            }
        assertEquals("sorozat-mátrix:" + bad, 0, bad.length());
    }

    /** Minden mozgásforma felismerhető a szokásos idő-alakokkal. */
    @Test public void everyMovementIsFoundInEveryTimeForm() {
        String[][] words = {{"futás", "futas"}, {"kerékpár", "kerekpar"}, {"úszás", "uszas"},
                {"túra", "tura"}, {"evezés", "evezes"}, {"kondi", "kondi"},
                {"kézilabda", "kezilabda"}, {"foci", "foci"}, {"kosárlabda", "kosarlabda"},
                {"röplabda", "roplabda"}, {"tenisz", "tenisz"}, {"karate", "harcmuveszet"},
                {"tánc", "tanc"}, {"jóga", "joga"}, {"korcsolya", "korcsolya"},
                {"síelés", "si"}, {"falmászás", "fal"}, {"kertészkedés", "munka"},
                {"bowling", "egyeb"}};
        String[][] pat = {{"%s 45 perc", "45"}, {"45 perc %s", "45"}, {"ma %s 45 percet", "45"},
                {"tegnap 45 perc %s", "45"}, {"%s 1 óra", "60"}, {"reggel %s 30 perc", "30"},
                {"%s másfél óra", "90"}};
        StringBuilder bad = new StringBuilder();
        for (String[] w : words)
            for (String[] p : pat) {
                String q = String.format(p[0], w[0]);
                Activities.Parsed r = Activities.parse(q, NOW);
                if (r.plans.size() != 1) { bad.append("\n  ").append(q).append(" -> ")
                        .append(r.plans.size()).append(" edzés"); continue; }
                Activities.Plan pl = r.plans.get(0);
                if (!pl.kind.id.equals(w[1]))
                    bad.append("\n  ").append(q).append(" -> ").append(pl.kind.id);
                else if (pl.minutes != Integer.parseInt(p[1]))
                    bad.append("\n  ").append(q).append(" -> ").append(pl.minutes).append(" perc");
            }
        assertEquals("mozgás-mátrix:" + bad, 0, bad.length());
    }
}
