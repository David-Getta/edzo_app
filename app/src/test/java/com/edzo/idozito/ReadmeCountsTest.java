package com.edzo.idozito;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * A README számai igazak-e.
 *
 * A leírás a termék ígérete: aki elolvassa, elhiszi, hogy 352 ételt ismer az
 * app és tizenöt testtájhoz van gyakorlatsor. Ezek a számok kézzel íródnak, a
 * kód viszont magától nő – a kettő némán szétcsúszik, és a README egy idő után
 * olyat állít, ami nem igaz. Pont az a fajta csendes hiba, amit az app maga is
 * kerül a naplóban.
 *
 * Ha a fájl nem található (más munkakönyvtárból futtatva), a teszt csendben
 * kihagyja magát: a hiányzó fájl nem hiba, a hazug szám az.
 */
public class ReadmeCountsTest {

    /** A README szövege, vagy null, ha innen nem érhető el. */
    private static String readme() {
        for (String path : new String[]{"README.md", "../README.md", "../../README.md"}) {
            File f = new File(path);
            if (!f.isFile()) continue;
            try (Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8")) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[8192];
                int n;
                while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
                return sb.toString();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    @Test public void theAdvertisedCountsMatchTheCode() {
        String s = readme();
        if (s == null) return;
        int rehabMoves = 0;
        for (Rehab.Area a : Rehab.AREAS) rehabMoves += a.moves.length;
        String[][] want = {
                {"étel", "**" + Foods.ALL.length + " étel**"},
                {"mozgásforma", Activities.ALL.length + " mozgásforma"},
                {"gyakorlat és gép", StrengthParse.names().length + " gyakorlat és gép"},
                {"testtáj", "**" + Rehab.AREAS.length + " testtáj, " + rehabMoves
                        + " gyógytornász-ihletésű gyakorlat**"},
        };
        StringBuilder bad = new StringBuilder();
        for (String[] w : want)
            if (!s.contains(w[1]))
                bad.append("\n  ").append(w[0]).append(": a README nem mondja, hogy „")
                   .append(w[1]).append("”");
        assertTrue("a README száma elavult:" + bad, bad.length() == 0);
    }
}
