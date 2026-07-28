package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * Magyar számformázás. Az app szövege magyar, a tizedeselválasztó tehát
 * vessző – ez korábban szétcsúszott: a víz vesszővel írt, a táv, a tempó, a
 * BMI és a testsúly viszont angol ponttal.
 */
public class HuTest {

    @Test public void oneDecimalUsesAComma() {
        assertEquals("5,2", Hu.d1(5.2));
        assertEquals("0,0", Hu.d1(0));
        // 12,46-ot írunk 12,45 helyett: a formázó a double PONTOS értékén
        // kerekít, és a 12,45 kettes számrendszerben épp 12,4499…, tehát
        // lefelé menne – az ilyen érték nem alkalmas a kerekítés bemutatására.
        assertEquals("12,5", Hu.d1(12.46));
        assertEquals("-3,4", Hu.d1(-3.44));
    }

    @Test public void twoDecimalsUseACommaToo() {
        assertEquals("5,23", Hu.d2(5.234));
        assertEquals("0,50", Hu.d2(0.5));
        assertEquals("10,00", Hu.d2(10));
    }

    @Test public void noOutputEverContainsADecimalPoint() {
        for (double v = -50; v <= 50; v += 0.37) {
            assertFalse("tizedespont a kimenetben: " + Hu.d1(v), Hu.d1(v).contains("."));
            assertFalse("tizedespont a kimenetben: " + Hu.d2(v), Hu.d2(v).contains("."));
        }
    }

    @Test public void theSameLocaleIsReusedNotRebuilt() {
        // Egy közös példány: a formázás sok helyen, gyakran fut (listák, diagramok).
        assertEquals(Hu.LOCALE, Hu.LOCALE);
        assertEquals("hu", Hu.LOCALE.getLanguage());
    }

    @Test public void formattingLargeAndTinyValuesStaysReadable() {
        assertEquals("1000,0", Hu.d1(1000));
        assertEquals("0,1", Hu.d1(0.05));      // felfelé kerekít
        assertEquals("0,0", Hu.d1(0.04));
    }
}
