package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A részletfájlok (GPS-nyomok) fájlnév-felismerése. Erre épül a takarítás,
 * tehát egy elrontott felismerés MEGLÉVŐ edzés útvonalát törölné – ez az a
 * fajta hiba, ami csak akkor derül ki, amikor már késő.
 */
public class SessionStoreTest {

    @Test public void aRealSessionFileGivesItsTimestamp() {
        assertEquals(1750000000000L, SessionStore.tsOfFile("s_1750000000000.json"));
        assertEquals(1L, SessionStore.tsOfFile("s_1.json"));
    }

    @Test public void otherFilesAreNotTouched() {
        // Bármi, ami nem pontosan a mi mintánk, maradjon érintetlen.
        assertEquals(-1, SessionStore.tsOfFile("meal_1750000000000.jpg"));
        assertEquals(-1, SessionStore.tsOfFile("s_1750000000000.txt"));
        assertEquals(-1, SessionStore.tsOfFile("session_1750000000000.json"));
        assertEquals(-1, SessionStore.tsOfFile("1750000000000.json"));
        assertEquals(-1, SessionStore.tsOfFile("backup.json"));
        assertEquals(-1, SessionStore.tsOfFile(""));
        assertEquals(-1, SessionStore.tsOfFile(null));
    }

    @Test public void aMalformedTimestampIsRejectedRatherThanGuessed() {
        assertEquals(-1, SessionStore.tsOfFile("s_.json"));
        assertEquals(-1, SessionStore.tsOfFile("s_abc.json"));
        assertEquals(-1, SessionStore.tsOfFile("s_17500000abc.json"));
        assertEquals(-1, SessionStore.tsOfFile("s_-1750000000000.json"));
        assertEquals(-1, SessionStore.tsOfFile("s_1.75e12.json"));
        // Túl hosszú szám: ne dobjon kivételt, csak ne ismerje fel.
        assertEquals(-1, SessionStore.tsOfFile("s_99999999999999999999999.json"));
    }

    @Test public void theNameRoundTripsWithTheTimestamp() {
        for (long ts : new long[]{1L, 1000L, 1750000000000L, Long.MAX_VALUE}) {
            String name = SessionStore.PREFIX + ts + SessionStore.SUFFIX;
            assertEquals("oda-vissza kellene egyeznie: " + name, ts, SessionStore.tsOfFile(name));
        }
    }
}
