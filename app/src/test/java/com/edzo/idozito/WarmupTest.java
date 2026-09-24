package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * Bemelegítő rámpa a munkasúlyhoz.
 *
 * Itt egy hiba nem elszámolt statisztika, hanem olyan súly, amit nem lehet
 * felrakni a rúdra – a felhasználó a teremben áll a számmal, és nem tud vele
 * mit kezdeni. Ezért minden kiírt súly 2,5 kg-os lépcsőn kell hogy álljon.
 */
public class WarmupTest {

    @Test public void aTypicalRampIsBuilt() {
        List<Warmup.Set> s = Warmup.forWork(100, 20);
        // Üres rúd, majd 50 / 70 / 85 százalék.
        assertEquals(4, s.size());
        assertEquals(20.0, s.get(0).weight, 0.001);
        assertEquals(50.0, s.get(1).weight, 0.001);
        assertEquals(70.0, s.get(2).weight, 0.001);
        assertEquals(85.0, s.get(3).weight, 0.001);
        // Felfelé egyre kevesebb ismétlés.
        for (int i = 1; i < s.size(); i++)
            assertTrue("nem csökken az ismétlés", s.get(i).reps <= s.get(i - 1).reps);
    }

    @Test public void everyWeightCanActuallyBeLoaded() {
        // A legkisebb tárcsa 1,25 kg, párban 2,5 – a rúdtól ennyi a lépcső.
        for (double work = 30; work <= 300; work += 2.5)
            for (double bar : new double[]{0, 10, 15, 20}) {
                if (bar > work) continue;
                for (Warmup.Set s : Warmup.forWork(work, bar)) {
                    double over = s.weight - bar;
                    assertTrue("nem felrakható súly: " + s.weight + " (rúd " + bar
                            + ", munka " + work + ")",
                            Math.abs(over / 2.5 - Math.round(over / 2.5)) < 1e-6);
                }
            }
    }

    @Test public void theRampNeverReachesTheWorkingWeight() {
        // A munkasúllyal egyenlő „bemelegítő" nem bemelegítés, hanem egy
        // elpazarolt munkaszéria.
        for (double work = 30; work <= 300; work += 2.5)
            for (Warmup.Set s : Warmup.forWork(work, 20))
                assertTrue("elérte a munkasúlyt: " + s.weight, s.weight < work);
    }

    @Test public void theRampOnlyGoesUpwards() {
        for (double work = 30; work <= 300; work += 2.5) {
            List<Warmup.Set> s = Warmup.forWork(work, 20);
            for (int i = 1; i < s.size(); i++)
                assertTrue("nem nő a súly " + work + " kg-nál",
                        s.get(i).weight > s.get(i - 1).weight);
            assertTrue("túl sok bemelegítő sorozat", s.size() <= Warmup.MAX_SETS);
        }
    }

    @Test public void lightWorkNeedsNoRamp() {
        // A 20 kg-os bicepsz bemelegítése maga az első sorozat.
        assertTrue(Warmup.forWork(20, 0).isEmpty());
        assertTrue(Warmup.forWork(25, 0).isEmpty());
        // Saját testsúly és képtelen bemenet: semmi.
        assertTrue(Warmup.forWork(0, 20).isEmpty());
        assertTrue(Warmup.forWork(-5, 20).isEmpty());
        assertTrue(Warmup.forWork(2000, 20).isEmpty());
        assertTrue(Warmup.forWork(50, -1).isEmpty());
        // A rúd nem lehet nehezebb a munkasúlynál.
        assertTrue(Warmup.forWork(15, 20).isEmpty());
    }

    @Test public void theBarOnlyAppearsWhenThereIsSomethingToAdd() {
        // 25 kg-os munkasúly 20-as rúddal: az üres rúd gyakorlatilag a
        // munkasúly, nincs értelme külön sorozatnak. (És 30 alatt nincs rámpa.)
        assertTrue(Warmup.forWork(25, 20).isEmpty());
        // Kézisúlyzónál nincs rúd-sorozat, de a rámpa megvan.
        List<Warmup.Set> d = Warmup.forWork(40, 0);
        assertTrue(!d.isEmpty());
        assertEquals(20.0, d.get(0).weight, 0.001);
    }

    @Test public void theLabelsAreReadable() {
        List<Warmup.Set> s = Warmup.forWork(100, 20);
        assertEquals("20 kg × 10  ·  20%", s.get(0).label());
        assertEquals("85 kg × 2  ·  85%", s.get(3).label());
        assertEquals("20×10  ·  50×5  ·  70×3  ·  85×2", Warmup.summary(s));
        assertEquals("", Warmup.summary(null));
        // A feles súly vesszővel, nem ponttal, és nem kerekítve.
        assertEquals("42,5", Warmup.kg(42.5));
        assertEquals("40", Warmup.kg(40));
    }

    @Test public void theBarbellLiftsKnowTheirBar() {
        assertEquals(20.0, Warmup.barFor("Guggolás"), 0.001);
        assertEquals(20.0, Warmup.barFor("Fekvenyomás"), 0.001);
        assertEquals(20.0, Warmup.barFor("Felhúzás"), 0.001);
        assertEquals(20.0, Warmup.barFor("Vállból nyomás"), 0.001);
        // Kézisúlyzó és gép: nincs rúd, de a rámpa attól még megvan.
        assertEquals(0.0, Warmup.barFor("Bicepsz"), 0.001);
        assertEquals(0.0, Warmup.barFor("Lábtolás"), 0.001);
        assertEquals(0.0, Warmup.barFor("Oldalemelés"), 0.001);
        assertEquals(0.0, Warmup.barFor(null), 0.001);
        assertEquals(0.0, Warmup.barFor(""), 0.001);
        // Minden felismert gyakorlatnév ad értelmes rudat, és a rámpa
        // mindegyikhez felrakható súlyokat: itt bukna ki egy elgépelt név.
        for (String n : StrengthParse.names()) {
            double bar = Warmup.barFor(n);
            assertTrue(n, bar == 0 || bar == 20);
            for (Warmup.Set st : Warmup.forWork(80, bar)) {
                double over = st.weight - bar;
                assertTrue(n + ": " + st.weight,
                        Math.abs(over / 2.5 - Math.round(over / 2.5)) < 1e-6);
            }
        }
    }
}
