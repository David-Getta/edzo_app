import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.teavm.backend.javascript.JSModuleType;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.vm.TeaVMOptimizationLevel;

/**
 * A webes (iPhone-os) mag fordítója: Java bájtkód → JavaScript, TeaVM-mel.
 *
 * Miért fordítunk, és miért nem írjuk át kézzel? A magyar felismerés a
 * projekt legértékesebb és legtörékenyebb része: közel húszezer sor, több
 * mint kétezer egységteszttel őrizve. Egy kézzel átírt JavaScript másolat
 * az első naptól kezdve csúszna el az eredetitől – és a hibák épp ott
 * jelennének meg, ahol senki nem keresi őket. Így viszont EGY forrás van:
 * ami az Androidon javul, az a telefonos weben is javul, ugyanabban a
 * percben.
 *
 * Használat: java Build &lt;osztályok könyvtára&gt; &lt;kimeneti könyvtár&gt;
 */
public final class Build {

    private Build() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Használat: Build <classes> <out>");
            System.exit(2);
        }
        File classes = new File(args[0]);
        File out = new File(args[1]);
        out.mkdirs();

        TeaVMTool tool = new TeaVMTool();
        tool.setClassLoader(new URLClassLoader(
                new URL[]{classes.toURI().toURL()}, Build.class.getClassLoader()));
        tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
        tool.setTargetDirectory(out);
        tool.setTargetFileName("mag.js");
        tool.setMainClass("com.edzo.idozito.WebApi");
        // UMD: ugyanaz a fájl fut a böngészőben (script-tagből) és a
        // Node-os egyezés-tesztben (require-ral) – két build helyett egy.
        tool.setJsModuleType(JSModuleType.UMD);
        // A teljes optimalizálás a telefonos letöltést és az indulást is
        // számottevően csökkenti; a fordítás pár másodperccel hosszabb.
        // Hibakereséshez a nevek megtartása: a tömörített kimenetben a
        // veremkép egybetűs függvényneveket mutat, amiből nem derül ki,
        // melyik felismerő szállt el.  WEB_DEBUG=1 bash tools/webepites.sh
        boolean debug = System.getenv("WEB_DEBUG") != null;
        // A sebesség méréséhez a KIADÁSI kód kell, olvasható nevekkel: a
        // SIMPLE szint egészen máshogy viselkedik, mint ami a telefonra
        // kerül.  WEB_NEVEK=1 bash tools/webepites.sh
        boolean nevek = debug || System.getenv("WEB_NEVEK") != null;
        tool.setOptimizationLevel(debug
                ? TeaVMOptimizationLevel.SIMPLE : TeaVMOptimizationLevel.FULL);
        tool.setObfuscated(!nevek);
        tool.setDebugInformationGenerated(debug);
        tool.setSourceMapsFileGenerated(false);
        // Szigorú mód nélkül a lebegőpontos műveletek gyorsabbak, de a
        // kerekítés eltérhetne a JVM-től. A kalóriaszám és a testsúly
        // tizedese számít, ezért a pontosságot választjuk.
        tool.setStrict(true);
        tool.setLog(new TeaVMToolLog() {
            @Override public void info(String text) { System.out.println("  " + text); }
            @Override public void debug(String text) { }
            @Override public void warning(String text) { System.out.println("  ! " + text); }
            @Override public void error(String text) { System.out.println("  HIBA " + text); }
            @Override public void info(String text, Throwable e) { info(text); }
            @Override public void debug(String text, Throwable e) { }
            @Override public void warning(String text, Throwable e) { warning(text); }
            @Override public void error(String text, Throwable e) {
                error(text);
                e.printStackTrace(System.out);
            }
        });
        tool.generate();
        // A TeaVM a fordítási gondokat nem a naplóba írja, hanem gyűjti.
        // Enélkül csak annyit látnánk, hogy „hibákkal készült" – azt nem,
        // hogy melyik osztály melyik hívása nem fordult le.
        java.util.List<org.teavm.diagnostics.Problem> problems =
                tool.getProblemProvider().getProblems();
        if (!problems.isEmpty()) {
            org.teavm.diagnostics.DefaultProblemTextConsumer c =
                    new org.teavm.diagnostics.DefaultProblemTextConsumer();
            int shown = 0;
            for (org.teavm.diagnostics.Problem p : problems) {
                c.clear();
                p.render(c);
                System.out.println("  [" + p.getSeverity() + "] " + c.getText()
                        + (p.getLocation() != null && p.getLocation().getMethod() != null
                            ? "  @ " + p.getLocation().getMethod() : ""));
                if (++shown >= 40) {
                    System.out.println("  … és még " + (problems.size() - shown));
                    break;
                }
            }
        }
        File js = new File(out, "mag.js");
        if (!js.isFile()) {
            System.err.println("A fordítás nem adott kimenetet.");
            System.exit(1);
        }
        System.out.println("mag.js: " + (js.length() / 1024) + " kB");
    }
}
