package com.edzo.idozito;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A GPS-útvonal-fájlok sorrendje és felismerése.
 *
 * A biztonsági mentés mostantól viszi az útvonalakat is, de nem korlátlanul:
 * ha nem fér bele mind, a LEGFRISSEBB futásoké maradjon meg. Ez a sorrenden
 * múlik. A fájlnév-felismerés ugyanaz, amire a takarítás is épül – ott egy
 * elrontott felismerés meglévő edzés útvonalát törölné.
 */
public class SessionOrderTest {

    @Test public void theNewestRoutesComeFirst() {
        String[] names = {"s_1000.json", "s_3000.json", "s_2000.json"};
        assertArrayEquals(new long[]{3000, 2000, 1000}, SessionStore.timestampsOf(names));
    }

    @Test public void onlyRealRouteFilesCount() {
        String[] names = {
                "s_1700000000000.json", "jegyzet.txt", "s_.json", "s_abc.json",
                "s_12x.json", "sessions.json", "s_2000000000000.json", null,
        };
        assertArrayEquals(new long[]{2000000000000L, 1700000000000L},
                SessionStore.timestampsOf(names));
    }

    @Test public void anEmptyOrMissingListIsHandled() {
        assertEquals(0, SessionStore.timestampsOf(new String[0]).length);
        assertEquals(0, SessionStore.timestampsOf(null).length);
    }

    @Test public void theFileNameRecogniserIsStrict() {
        assertEquals(1700000000000L, SessionStore.tsOfFile("s_1700000000000.json"));
        assertEquals(-1, SessionStore.tsOfFile("s_.json"));
        assertEquals(-1, SessionStore.tsOfFile("s_12ab.json"));
        assertEquals(-1, SessionStore.tsOfFile("x_1200.json"));
        assertEquals(-1, SessionStore.tsOfFile("s_1200.txt"));
        assertEquals(-1, SessionStore.tsOfFile(null));
        // Negatív előjel sem: a „-" nem számjegy.
        assertEquals(-1, SessionStore.tsOfFile("s_-1200.json"));
    }
}
