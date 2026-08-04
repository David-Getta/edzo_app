package com.edzo.idozito;

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
}
