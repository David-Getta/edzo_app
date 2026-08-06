package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

    @Test public void theCompoundNumberWordsAreUnderstood() {
        // Ezeket mondja az ember a teremben és a konyhában: „nyolcvanöt kiló",
        // „harminckettő perc". Eddig csak a kerek tízesek mentek át.
        assertEquals("32", Hu.digits("harmincketto"));
        assertEquals("43", Hu.digits("negyvenharom"));
        assertEquals("56", Hu.digits("otvenhat"));
        assertEquals("67", Hu.digits("hatvanhet"));
        assertEquals("78", Hu.digits("hetvennyolc"));
        assertEquals("85", Hu.digits("nyolcvanot"));
        assertEquals("89", Hu.digits("nyolcvankilenc"));
        assertEquals("92", Hu.digits("kilencvenketto"));
        // Tárgyeset is.
        assertEquals("85", Hu.digits("nyolcvanotot"));
        assertEquals("32", Hu.digits("harminckettot"));
        // Százasok, tízesekkel és egyesekkel.
        assertEquals("100", Hu.digits("szaz"));
        assertEquals("125", Hu.digits("szazhuszonot"));
        assertEquals("150", Hu.digits("szazotven"));
        // A „tíz" és a „húsz" magában az alaplistában van – a százas
        // összetételekhez („százhúsz", „száztíz") külön kellett hozzáadni.
        assertEquals("120", Hu.digits("szazhusz"));
        assertEquals("110", Hu.digits("szaztiz"));
        assertEquals("220", Hu.digits("ketszazhusz"));
        assertEquals("200", Hu.digits("ketszaz"));
        assertEquals("250", Hu.digits("ketszazotven"));
        assertEquals("305", Hu.digits("haromszazot"));
        // A régiek változatlanul.
        assertEquals("11", Hu.digits("tizenegy"));
        assertEquals("25", Hu.digits("huszonot"));
        assertEquals("1,5", Hu.digits("masfel"));
        assertEquals("0,5", Hu.digits("fel"));
    }

    @Test public void everydayWordsAreNotNumbers() {
        // A szótár nőtt; a hamis pozitívok viszont nem nőhetnek vele.
        for (String w : new String[]{"hatizom", "hetes", "hetente", "szazalek",
                "harmadik", "negyedik", "otodik", "hatodik", "hetedik", "kettesben",
                "otthon", "egyetem", "hatvanas", "szazados", "kilencedik",
                "tizedik", "harmadszor", "hatarozott", "negyzet", "otletes"})
            assertEquals("nem szám: " + w, w, Hu.digits(w));
        // A „negyven év" viszont VALÓDI szám: az önálló szó cserélődik.
        assertEquals("40 ev", Hu.digits("negyven ev"));
    }

    @Test public void theGeneratedTableHasNoConflictingEntries() throws Exception {
        // A tábla generált: egy elrontott képlet ugyanazt a szót két külön
        // értékkel is felvehetné, és onnantól a sorrend döntené el, melyik
        // nyer. Az ilyen hiba némán rossz számot ír a naplóba.
        java.lang.reflect.Field f = Hu.class.getDeclaredField("NUM_WORDS");
        f.setAccessible(true);
        String[][] t = (String[][]) f.get(null);
        assertTrue("gyanúsan kicsi a tábla: " + t.length, t.length > 500);
        java.util.Map<String, String> seen = new java.util.LinkedHashMap<>();
        StringBuilder bad = new StringBuilder();
        for (String[] w : t) {
            assertTrue("üres szótő", w[0] != null && !w[0].isEmpty());
            String prev = seen.put(w[0], w[1]);
            if (prev != null && !prev.equals(w[1]))
                bad.append("\n  ").append(w[0]).append(" = ").append(prev)
                   .append(" vagy ").append(w[1]);
        }
        assertEquals("ütköző szótövek:" + bad, 0, bad.length());
        // A hosszabb alak elöl: erre épül az egész illesztés.
        for (int i = 1; i < t.length; i++)
            assertTrue("nem hossz szerint csökkenő a tábla",
                    t[i - 1][0].length() >= t[i][0].length());
    }

    @Test public void theMultiplicativeSuffixMustEndTheWord() {
        // Az „egyszerű" nem egy, a „kétszeres" nem kettő: a „-szor/-szer"
        // toldalék csak akkor tapad a számhoz, ha a szó ott véget is ér.
        assertEquals("egyszeru", Hu.digits("egyszeru"));
        assertEquals("ketszeres", Hu.digits("ketszeres"));
        assertEquals("haromszoros", Hu.digits("haromszoros"));
        assertEquals("egyszeruen jo volt", Hu.digits("egyszeruen jo volt"));
        // A valódi szorzószám viszont továbbra is szám lesz.
        assertEquals("2szer", Hu.digits("ketszer"));
        assertEquals("3szor a heten", Hu.digits("haromszor a heten"));
        assertEquals("5szor 5", Hu.digits("otszor otot"));
    }
    /**
     * Ezres tagolás: a lépésszám négy-öt jegyű, és tagolás nélkül egy
     * pillanatra minden ilyen szám egyforma – a „9870" és a „19870"
     * ránézésre ugyanaz.
     */
    @Test public void thousandsAreGrouped() {
        assertEquals("0", Hu.num(0));
        assertEquals("7", Hu.num(7));
        assertEquals("999", Hu.num(999));
        assertEquals("1\u202f000", Hu.num(1000));
        assertEquals("12\u202f345", Hu.num(12345));
        assertEquals("1\u202f234\u202f567", Hu.num(1234567));
        assertEquals("-12\u202f345", Hu.num(-12345));
        // Magyarul a tagolás SZÓKÖZ: se vessző, se pont nem kerülhet bele.
        for (long v : new long[]{1000, 12345, 1234567, Long.MAX_VALUE}) {
            String out = Hu.num(v);
            assertTrue(out, out.indexOf(',') < 0 && out.indexOf('.') < 0);
        }
    }
}
