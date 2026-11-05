import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A kihagyás-szűrő próbája: mondhat-e valaha tévesen nemet?
 *
 * A webes változat minden mintához kikeresi azt a betűsort, aminek MINDEN
 * találatban benne kell lennie, és ha az a mondatban nincs meg, a keresést
 * el sem indítja. Ettől lett a felismerés a telefonon többszörösen gyorsabb
 * – de a gyorsítás csak akkor ér valamit, ha egyetlen találatot sem veszít.
 *
 * Ez a próba minden mintát összevet minden korpusz-mondattal: ahol a szűrő
 * kihagyná a keresést, ott a keresésnek tényleg nem szabad találnia. Több
 * millió pár, és egyetlen eltérés is bukás. A JVM saját regex-motorja az
 * igazság forrása; a minta-átírás helyességét külön az egyezés-teszt őrzi.
 *
 * Használat:  java -cp <osztályok> Szuro <minták fájl> <korpusz>
 */
public final class Szuro {

    private Szuro() { }

    public static void main(String[] args) throws Exception {
        // A kimenet magyar; a futtató alapértelmezett kódolása
        // (a CI-n POSIX) kérdőjelekké mosná a szöveget.
        PrintStream ki = new PrintStream(System.out, true, "UTF-8");
        Method kotelezo = Class.forName("com.edzo.idozito.Rx")
                .getDeclaredMethod("kotelezoSzoveg", String.class);
        kotelezo.setAccessible(true);
        List<String> mintak = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
        List<String> mondatok = Files.readAllLines(Paths.get(args[1]), StandardCharsets.UTF_8);

        long parok = 0;
        long kihagyott = 0;
        int szurheto = 0;
        int nemMinta = 0;
        int rossz = 0;
        for (String m : mintak) {
            String[] kell;
            Pattern p;
            try {
                kell = (String[]) kotelezo.invoke(null, m);
                p = Pattern.compile(m);
            } catch (Throwable e) {
                // Nem minden sztringliterál reguláris kifejezés.
                nemMinta++;
                continue;
            }
            if (kell.length > 0) szurheto++;
            for (String s : mondatok) {
                parok++;
                if (kell.length == 0 || vanBenne(s, kell)) continue;
                kihagyott++;
                if (p.matcher(s).find()) {
                    rossz++;
                    if (rossz <= 5) {
                        ki.println("  ROSSZ SZŰRÉS");
                        ki.println("    minta   : " + m);
                        ki.println("    kötelező: " + String.join(" | ", kell));
                        ki.println("    mondat  : " + s);
                    }
                }
            }
        }
        int ossz = mintak.size() - nemMinta;
        ki.println("     " + ossz + " mintából " + szurheto + " szűrhető, "
                + "a " + parok + " pár " + (100L * kihagyott / Math.max(1, parok))
                + "%-a marad el");
        if (rossz > 0) {
            ki.println("BUKÁS: " + rossz + " találat elveszett volna.");
            System.exit(1);
        }
        ki.println("     A szűrő egyetlen találatot sem veszít.");
    }

    /** Ott van-e a szövegben a követelt szavak valamelyike? */
    private static boolean vanBenne(String s, String[] kell) {
        for (String k : kell) {
            if (s.indexOf(k) >= 0) return true;
        }
        return false;
    }
}
