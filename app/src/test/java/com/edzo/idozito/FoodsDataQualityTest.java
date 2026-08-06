package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Az étel-adatbázis belső összhangja.
 *
 * Ezek a számok kézzel kerülnek be, tételenként – és egy elütött érték nem
 * fordítási hiba, hanem hamis kalória a naplóban. A szabályok szándékosan
 * lazák: nem tápanyag-táblázatot ellenőrzünk, hanem a nagyságrendi és fizikai
 * képtelenségeket fogjuk meg.
 */
public class FoodsDataQualityTest {

    @Test public void everyFoodIsPhysicallyPossible() {
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            // A tiszta olaj 900 kcal/100 g – ennél sűrűbb étel nincs.
            if (f.kcal100 < 0 || f.kcal100 > 900)
                bad.append("\n  ").append(f.name).append(": kcal100=").append(f.kcal100);
            // A legfehérjésebb tétel a fehérjepor (75 g/100 g).
            if (f.prot100 < 0 || f.prot100 > 90)
                bad.append("\n  ").append(f.name).append(": prot100=").append(f.prot100);
            // Az adag 5 g (egy kockacukornyi) és fél kiló között életszerű.
            if (f.portion < 5 || f.portion > 500)
                bad.append("\n  ").append(f.name).append(": portion=").append(f.portion);
            // 1 g fehérje = 4 kcal: a fehérje energiája nem lépheti túl az
            // összeset. (Kis tűrés a kerekített értékek miatt.)
            if (f.kcal100 > 0 && f.prot100 * 4 > f.kcal100 + 8)
                bad.append("\n  ").append(f.name).append(": ")
                   .append(Math.round(f.prot100 * 4)).append(" kcal fehérje ")
                   .append(f.kcal100).append(" kcal ételben");
        }
        assertTrue("képtelen adatok:" + bad, bad.length() == 0);
    }

    @Test public void noNameOrStemAppearsTwice() {
        // Két azonos nevű tétel közül a második soha nem érhető el a
        // felismerésből, a táblázatban viszont ott van – csendes kettősség.
        // A közös szótő ugyanígy: az egyik étel elnyeli a másikat.
        java.util.HashMap<String, String> names = new java.util.HashMap<>();
        java.util.HashMap<String, String> stems = new java.util.HashMap<>();
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            String key = Foods.norm(f.name);
            if (names.containsKey(key)) bad.append("\n  dupla név: ").append(f.name);
            else names.put(key, f.name);
            for (String s : f.stems) {
                String ns = Foods.norm(s);
                if (ns.isEmpty()) continue;
                String prev = stems.get(ns);
                // Ékezetes és ékezet nélküli írásmód ugyanarra a tételre:
                // felesleges, de nem hiba – csak a MÁS ételé az.
                if (prev != null && !prev.equals(f.name))
                    bad.append("\n  közös szótő \"").append(ns).append("\": ")
                       .append(prev).append(" / ").append(f.name);
                else if (prev != null)
                    bad.append("\n  felesleges szótő \"").append(ns).append("\": ").append(f.name);
                else stems.put(ns, f.name);
            }
        }
        assertTrue("kettősségek:" + bad, bad.length() == 0);
    }

    @Test public void everyFoodHasANameAndAStem() {
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            if (f.name == null || f.name.trim().isEmpty())
                bad.append("\n  névtelen tétel");
            boolean hasStem = false;
            for (String s : f.stems) if (s != null && !s.trim().isEmpty()) hasStem = true;
            if (!hasStem) bad.append("\n  ").append(f.name).append(": nincs szótöve");
        }
        assertTrue("hiányos tételek:" + bad, bad.length() == 0);
    }
    @Test public void everySliceMeasuredFoodExists() {
        // A szelet-tábla nevekre hivatkozik: egy átnevezés csendben kiütné.
        for (String[] row : Foods.SLICE_GRAMS) {
            boolean found = false;
            for (Foods.Food f : Foods.ALL) if (f.name.equals(row[0])) { found = true; break; }
            assertTrue("nincs ilyen étel: " + row[0], found);
            int g = Integer.parseInt(row[1]);
            assertTrue("életszerűtlen szelet: " + row[0], g >= 20 && g <= 300);
        }
    }

    /**
     * Minden szótő ÖNMAGÁRA esik – és csak arra.
     *
     * Ez a rövid tövek csapdája: a „chia" a macchiato közepén, a „rizs" a
     * párizsiban. Ilyenkor a bejegyzés csendben létrejön, csak épp más
     * ételről vagy egy fölös adaggal. Egy új szótő pontosan ezt szokta
     * elrontani, ezért a teljes szótő-készletet átfuttatjuk.
     */
    @Test public void everyStemMeansExactlyItsOwnFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL)
            for (String st : f.stems) {
                java.util.List<Foods.Hit> h = Foods.parse(all, st);
                if (h.size() == 1 && h.get(0).food.name.equals(f.name)) continue;
                bad.append("\n  ").append(f.name).append(" / \"").append(st)
                   .append("\" -> ").append(names(h));
            }
        assertEquals("ütköző szótő:" + bad, 0, bad.length());
    }

    /**
     * Ugyanaz KÉT szótővel egymás mellett: „párizsi felvágott".
     *
     * Ételenként csak a leghosszabb szótő helyét jegyezzük meg. A párizsi a
     * hosszabb „felvágott" tövön került be, és a szó elején álló „párizsi"
     * szabadon hagyta a benne rejlő „rizs"-t: kétszáz gramm rizs a felvágott
     * mellé. Egyetlen tő önmagában nem hozta elő – csak a párja.
     */
    @Test public void twoStemsOfTheSameFoodStillMeanOneFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL)
            for (int i = 0; i < f.stems.length; i++)
                for (int j = 0; j < f.stems.length; j++) {
                    if (i == j) continue;
                    String q = f.stems[i] + " " + f.stems[j];
                    java.util.List<Foods.Hit> h = Foods.parse(all, q);
                    if (h.size() == 1 && h.get(0).food.name.equals(f.name)) continue;
                    bad.append("\n  ").append(f.name).append(" / \"").append(q)
                       .append("\" -> ").append(names(h));
                }
        assertEquals("ütköző szótőpár:" + bad, 0, bad.length());
    }

    /**
     * A SÖR és a SOR: ékezet nélkül ugyanaz a szó.
     *
     * A magyar bőven gyárt „-sor" végű összetételeket (névsor, címsor,
     * gyakorlatsor, munkasorozat), és mindegyik sört írt a naplóba. Tiltólista
     * nem old meg egy végtelen szóosztályt – szó belsejében ezért az ékezet
     * dönt. Szó elején viszont marad a régi viselkedés: aki ékezet nélkül
     * gépel, attól nem vesszük el a sörét.
     */
    @Test public void beerNeedsItsAccentInsideAWord() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[] beer = {"két korsó sör", "sört ittam", "sör", "búzasör", "barna sör",
                "alkoholmentes sör", "2 üveg sör", "két korso sor"};
        for (String q : beer)
            assertEquals("elveszett a sör: " + q, "Sör",
                    Foods.parse(all, q).isEmpty() ? "—" : Foods.parse(all, q).get(0).food.name);
        String[] notBeer = {"névsora", "gyakorlatsorok", "címsorban", "fejsor", "csipsor",
                "munkasorozatokra", "3 sorozat 10 fekvenyomás", "egysoros",
                "ábécésorrendben", "sorból", "soronként", "sorompó", "idősor"};
        for (String q : notBeer)
            assertTrue("sör lett belőle: " + q, Foods.parse(all, q).isEmpty());
    }

    /**
     * Ékezetes ütközések: MÉZ/mező, KÁVÉ/falkavezér, KÓLA/csonkolás,
     * TEJ/estéjéről, BOR/bőrrel, FOGAS/fogás.
     *
     * Ugyanaz a fajta hiba, mint a sörnél, csak más betűvel: ékezet nélkül a
     * hétköznapi szó és az étel neve egybeesik. A jobb oldalon mindig az áll,
     * amit eddig naplózott.
     */
    @Test public void accentTellsTheFoodFromTheEverydayWord() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[] notFood = {"adatmezők", "falkavezér", "csonkolás", "estéjéről", "bőrrel",
                "fogás", "fogások", "fogásnak", "beszédbuborék", "szappanbuborék",
                "főképernyő", "kezdőképernyő", "domborítsd", "buktató", "ellenőrizzük",
                "fogalmaz", "durumbúza", "szeretek futni", "szeretem", "stádió",
                "szokásaim", "levesszük", "hüvelykujjszabály", "vízilabda"};
        for (String q : notFood)
            assertTrue("ételt talált benne: " + q + " -> " + names(Foods.parse(all, q)),
                    Foods.parse(all, q).isEmpty());
        // A valódi tételek viszont maradnak – ékezet nélkül gépelve is.
        String[] food = {"méz", "mez", "kávé", "kave", "kóla", "kola", "tej", "bor",
                "fogas", "vörösbor", "tejföl", "mézes", "fogassal"};
        for (String q : food)
            assertTrue("elveszett az étel: " + q, !Foods.parse(all, q).isEmpty());
    }

    /**
     * Átfedő szótöveknél a HOSSZABB dönt, nem a súlyosabb étel.
     *
     * Egy szón belül általában a súlyosabb tétel nyer (a „csirkemellsalátá"-ban
     * a csirkemell, nem a saláta). Ha viszont a két találat ugyanazokon a
     * betűkön osztozik, akkor nem két összetevőről van szó, hanem egyetlen
     * szóról: az „almáját" eddig csirkemájat naplózott.
     */
    @Test public void overlappingStemsPreferTheLongerOne() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[][] cases = {{"almáját", "Alma"}, {"almája", "Alma"},
                {"megettem az almáját", "Alma"}, {"tejföl", "Tejföl"},
                {"csirkemáj", "Csirkemáj"}, {"gránátalma", "Gránátalma"}};
        for (String[] c : cases) {
            java.util.List<Foods.Hit> h = Foods.parse(all, c[0]);
            assertEquals(c[0] + " -> " + names(h), 1, h.size());
            assertEquals(c[0], c[1], h.get(0).food.name);
        }
    }

    private static String names(java.util.List<Foods.Hit> h) {
        if (h.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (Foods.Hit x : h) sb.append(sb.length() > 0 ? ", " : "").append(x.food.name);
        return sb.toString();
    }
}
