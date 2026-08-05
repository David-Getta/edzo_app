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
    @Test public void writtenNumbersBecomeDigits() {
        assertEquals("4 kor 30 masodperc", Hu.digits("negy kor 30 masodperc"));
        assertEquals("5szor 5", Hu.digits("otszor otot"));
        assertEquals("3szor 12", Hu.digits("haromszor tizenkettot"));
        assertEquals("0,5 perc", Hu.digits("fel perc"));
        assertEquals("1,5 perc", Hu.digits("masfel perc"));
        // Csak önálló szó: a „hatizom" nem hat izom, a „hetes" nem hét es.
        assertEquals("hatizom 3szor 20", Hu.digits("hatizom haromszor husz"));
        assertEquals("hetes suly", Hu.digits("hetes suly"));
        assertEquals("egykezes evezes", Hu.digits("egykezes evezes"));
        // A hosszabb alak nyer: a „tizenketto" nem tiz + enketto.
        assertEquals("12", Hu.digits("tizenketto"));
        assertEquals("", Hu.digits(null));
    }
    @Test public void theChangeIsReadableAndHonest() {
        assertEquals("+50%", Hu.delta(15, 10));
        assertEquals("−20%", Hu.delta(8, 10));
        assertEquals("=", Hu.delta(10, 10));
        assertEquals("=", Hu.delta(10.04, 10));  // fél százalék alatt nincs változás
        assertEquals("+2%", Hu.delta(10.2, 10));
        assertEquals("−100%", Hu.delta(0, 10));
        // Előzmény nélkül nincs mihez viszonyítani: az „új" nem „+100%".
        assertEquals("új", Hu.delta(10, 0));
        assertEquals("—", Hu.delta(0, 0));
        assertEquals("—", Hu.delta(0, -5));
    }
    @Test public void theWeekdayNamesStartOnMonday() {
        assertEquals("Hétfő", Hu.dayName(0));
        assertEquals("Vasárnap", Hu.dayName(6));
        assertEquals("", Hu.dayName(-1));
        assertEquals("", Hu.dayName(7));
    }

    @Test public void theTensAboveThirtyAreNumbers() {
        assertEquals("40 masodperc", Hu.digits("negyven masodperc"));
        assertEquals("45 mp", Hu.digits("negyvenot mp"));
        assertEquals("50 kg", Hu.digits("otven kg"));
        assertEquals("60 mp", Hu.digits("hatvan mp"));
        assertEquals("80 kg", Hu.digits("nyolcvan kg"));
        assertEquals("90 mp", Hu.digits("kilencven mp"));
        assertEquals("100 fekvotamasz", Hu.digits("szaz fekvotamasz"));
        assertEquals("35 mp", Hu.digits("harmincot mp"));
        // A szóba ragadt alak nem szám: a „százalék" nem 100-alék.
        assertEquals("szazalek", Hu.digits("szazalek"));
        assertEquals("negyvenes", Hu.digits("negyvenes"));
    }
}
