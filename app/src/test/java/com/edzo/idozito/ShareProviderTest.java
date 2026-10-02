package com.edzo.idozito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A megosztó szolgáltatás fájlnév-ellenőrzése. Az URI utolsó szakasza a
 * hívótól jön, és dekódolás után könyvtár-elválasztót is tartalmazhat – egy
 * ellenőrizetlen név a megosztó mappán kívülre mutatna.
 */
public class ShareProviderTest {

    @Test public void theNamesWeActuallyShareAreAccepted() {
        for (String n : new String[]{
                "grit-haladas.png", "grit-kituntetesek.png", "grit-edzes.png",
                "grit-statisztika.png", "grit-aktivitas.png",
                "grit_elozmenyek.csv", "grit_etrend.csv", "grit_erosito.csv",
                "grit_mentes.json", "edzes_1750000000000.png"}) {
            assertTrue("jogos nevet utasított el: " + n, ShareProvider.isSafeName(n));
        }
    }

    @Test public void pathSeparatorsAreRejected() {
        assertFalse(ShareProvider.isSafeName("../edzo.xml"));
        assertFalse(ShareProvider.isSafeName("../../shared_prefs/edzo.xml"));
        assertFalse(ShareProvider.isSafeName("/etc/passwd"));
        assertFalse(ShareProvider.isSafeName("sub/dir.png"));
        assertFalse(ShareProvider.isSafeName("..\\edzo.xml"));
    }

    @Test public void emptyAndDotNamesAreRejected() {
        assertFalse(ShareProvider.isSafeName(null));
        assertFalse(ShareProvider.isSafeName(""));
        assertFalse(ShareProvider.isSafeName("."));
        assertFalse(ShareProvider.isSafeName(".."));
    }

    @Test public void aNulByteCannotTruncateTheName() {
        // A %00 dekódolva NUL lesz; a natív rétegben ez levághatná a nevet,
        // így a kiterjesztés-ellenőrzést meg lehetne kerülni.
        assertFalse(ShareProvider.isSafeName("kep.png\u0000.txt"));
        assertFalse(ShareProvider.isSafeName("\u0000"));
    }

    @Test public void ordinaryDotsInsideTheNameAreFine() {
        // A „.." tiltása ne csapja agyon a szokásos pontos neveket.
        assertTrue(ShareProvider.isSafeName("grit.haladas.png"));
        assertTrue(ShareProvider.isSafeName("a..b.png"));
    }
}
