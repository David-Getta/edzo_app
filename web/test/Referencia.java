import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.edzo.idozito.WebApi;

/**
 * Az egyezés-teszt JVM-oldala.
 *
 * Ugyanazt a korpuszt futtatja, mint a Node-os oldal, és ugyanabban a
 * formában írja ki – így a két kimenet különbsége PONTOSAN az, amiben a
 * webes (iPhone-os) mag eltér az Androidostól.
 */
public final class Referencia {

    private Referencia() { }

    public static void main(String[] args) throws Exception {
        long now = Long.parseLong(args[1]);
        List<String> lines = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        for (String line : lines) {
            if (line.isEmpty()) continue;
            String json;
            try {
                json = WebApi.parse(line, now);
            } catch (RuntimeException e) {
                json = "HIBA:" + e.getClass().getName();
            }
            out.println(json);
        }
    }
}
