package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Erősítő sorozatok EGY mondatból.
 *
 * A felismerés óvatos: ismétlésszám nélkül nincs mentés, mert egy kitalált
 * sorozat a rekordokba, az 1RM-be és az izomcsoport-egyensúlyba is bekerülne.
 */
public class StrengthParseTest {

    private static String sum(String q) {
        StringBuilder sb = new StringBuilder();
        for (StrengthParse.Item it : StrengthParse.parse(q)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(it.name).append(" ").append(it.sets.size()).append("×");
            for (int i = 0; i < it.sets.size(); i++) {
                if (i > 0) sb.append("/");
                sb.append(it.sets.get(i).reps);
            }
            sb.append("@").append(Progression.kg(it.topWeight()));
        }
        return sb.toString();
    }

    @Test public void theClassicSetNotationWorks() {
        assertEquals("Fekvenyomás 3×10/10/10@60", sum("3x10 fekvenyomás 60 kg"));
        assertEquals("Guggolás 5×5/5/5/5/5@80", sum("guggolás 5x5 80 kg"));
        // Szóközzel és szorzójellel is.
        assertEquals("Guggolás 3×8/8/8@100", sum("guggolás 3 × 8 100 kg"));
    }

    @Test public void bodyweightExercisesNeedNoWeight() {
        assertEquals("Húzódzkodás 3×8/8/8@0", sum("húzódzkodás 3x8"));
        assertEquals("Fekvőtámasz 1×50@0", sum("50 fekvőtámasz"));
        // A saját testsúlyos sorozat is látszik a címkében.
        assertTrue(StrengthParse.parse("húzódzkodás 3x8").get(0).label()
                .contains("saját testsúly"));
    }

    @Test public void perSetRepsAreKept() {
        assertEquals("Bicepsz 3×12/10/8@15", sum("bicepsz 12-10-8 15 kg"));
        assertEquals("Felhúzás 4×10/8/6/4@120", sum("felhúzás 120 kg 10-8-6-4"));
    }

    @Test public void wordyFormsWork() {
        assertEquals("Vállból nyomás 3×12/12/12@20",
                sum("vállból nyomás 3 sorozat 12 ismétlés 20 kg"));
        assertEquals("Tricepsz 1×15@25", sum("tricepsz 15 ismétlés 25 kg-mal"));
        assertEquals("Evezés 4×10/10/10/10@50", sum("evezés 4 szett 10 ism 50 kg"));
    }

    @Test public void severalExercisesInOneSentence() {
        assertEquals("Guggolás 3×10/10/10@60 | Fekvenyomás 3×8/8/8@50",
                sum("guggolás 3x10 60 kg, fekvenyomás 3x8 50 kg"));
        assertEquals("Guggolás 3×10/10/10@60 | Kitörés 3×12/12/12@0",
                sum("guggolás 3x10 60 kg és kitörés 3x12"));
        // Ugyanaz a gyakorlat kétszer: egy bejegyzésbe olvad.
        assertEquals("Fekvenyomás 5×10/10/10/8/8@60",
                sum("fekvenyomás 3x10 60 kg, majd fekvenyomás 2x8 60 kg"));
    }

    @Test public void gymShorthandIsUnderstood() {
        // A tizedesvessző nem vág ketté mondatot.
        assertEquals("Bicepsz 3×12/12/12@12,5", sum("bicepsz 3x12 12,5 kg"));
        // A „3x10x60” harmadik tagja a súly, a „3x10 60” mértékegység nélkül is.
        assertEquals("Guggolás 3×10/10/10@60", sum("guggolás 3x10x60"));
        assertEquals("Vállból nyomás 3×10/10/10@30", sum("vállból nyomás 3x10 30"));
        // Kötőszó nélkül felsorolt gyakorlatok is szétválnak.
        assertEquals("Guggolás 3×10/10/10@60 | Fekvenyomás 3×8/8/8@50",
                sum("guggolás 3x10 60kg fekvenyomás 3x8 50kg"));
        // Bevezető szöveg az első gyakorlat előtt nem zavar.
        assertEquals("Guggolás 3×10/10/10@60 | Evezés 3×12/12/12@40",
                sum("két gyakorlat: guggolás 3x10 60 kg; evezés 3x12 40 kg"));
        // Ragozott alakok.
        assertEquals("Fekvenyomás 3×10/10/10@60", sum("nyomtam 3x10-et fekvenyomásban 60 kg-mal"));
        assertEquals("Guggolás 3×10/10/10@60", sum("ma guggolás 3x10 60 kilóval"));
    }

    @Test public void nothingIsInventedWithoutReps() {
        // Gyakorlat ismétlés nélkül, vagy ismeretlen mozdulat: nincs találat.
        for (String q : new String[]{"guggolás", "3 sorozat guggolás", "kondiztam egyet",
                "60 kg", "", "   ", "jó edzés volt"}) {
            assertEquals("kitalált sorozat: " + q, 0, StrengthParse.parse(q).size());
        }
    }

    @Test public void similarNamesDoNotCollide() {
        // A „fekvenyomás” nem fekvőtámasz, és fordítva.
        assertEquals("Fekvenyomás", StrengthParse.parse("fekvenyomás 3x5 100 kg").get(0).name);
        assertEquals("Fekvőtámasz", StrengthParse.parse("fekvőtámasz 3x20").get(0).name);
        assertEquals("Vállból nyomás", StrengthParse.parse("vállból nyomás 3x10 30 kg")
                .get(0).name);
    }

    @Test public void absurdNumbersAreRejected() {
        // A számok életszerű tartományban maradnak – a fuzz ne tudjon hülyeséget
        // bejuttatni a naplóba.
        for (String q : new String[]{"guggolás 999x999 9999 kg", "bicepsz 0x0",
                "guggolás 300 ismétlés 900 kg", "fekvenyomás 3x10 800 kg"}) {
            for (StrengthParse.Item it : StrengthParse.parse(q)) {
                assertTrue("túl sok sorozat: " + q, it.sets.size() <= 20);
                for (StrengthParse.Set s : it.sets) {
                    assertTrue("ismétlés: " + q, s.reps >= 1 && s.reps <= 200);
                    assertTrue("súly: " + q, s.weight >= 0 && s.weight <= 500);
                }
            }
        }
    }

    @Test public void gymMachinesAndVariationsAreKnown() {
        assertEquals("Hasprés 3×20/20/20@0", sum("hasizom 3x20"));
        assertEquals("Lábnyújtás 3×12/12/12@80", sum("lábgép 3x12 80 kg"));
        assertEquals("Csuklyás emelés 3×12/12/12@20", sum("csuklyás 3x12 20 kg"));
        assertEquals("Arnold nyomás 3×10/10/10@16", sum("arnold nyomás 3x10 16 kg"));
        assertEquals("Fordított tárogatás 3×15/15/15@8", sum("fordított tárogatás 3x15 8 kg"));
        assertEquals("Mellgép 3×12/12/12@40", sum("mellgép 3x12 40 kg"));
        assertEquals("Plank 3×45/45/45@0", sum("oldaltámasz 3x45"));
        assertEquals("Csípőemelés 3×15/15/15@40", sum("farizom 3x15 40 kg"));
        // A jelzős változatok az alapgyakorlathoz esnek.
        assertEquals("Felhúzás", StrengthParse.parse("román felhúzás 3x8 80 kg").get(0).name);
        assertEquals("Guggolás", StrengthParse.parse("elöl guggolás 3x5 60 kg").get(0).name);
        assertEquals("Kitörés", StrengthParse.parse("bolgár kitörés 3x10 20 kg").get(0).name);
    }

    @Test public void quickChipNamesMatchTheParserNames() {
        // A gyors chipek és a mondat-felismerés ugyanazt a nevet használják –
        // különben ugyanaz a gyakorlat két néven élne a naplóban, és a
        // rekordok, a progresszió-javaslat meg a „mikor csináltad utoljára”
        // mind kettéválna.
        java.util.List<String> parsed = Arrays.asList(StrengthParse.names());
        for (String chip : StrengthLog.COMMON)
            assertTrue("a chip neve ismeretlen a felismerőnek: " + chip,
                    parsed.contains(chip));
    }

    @Test public void everyExerciseNameResolvesToItself() {
        // Önellenőrzés: ha egy gyakorlat SAJÁT neve máshová esik, akkor egy
        // hosszabb tő elnyelte – és a mondatból sosem lehet őt felvenni.
        for (String n : StrengthParse.names()) {
            List<StrengthParse.Item> got = StrengthParse.parse(n + " 3x10");
            assertEquals("nem önmagára esik: " + n, 1, got.size());
            assertEquals("nem önmagára esik: " + n, n, got.get(0).name);
        }
    }

    @Test public void everyKnownExerciseHasAMuscleGroup() {
        // Amit a mondat-felvétel elment, annak a heti izomcsoport-egyensúlyban
        // is látszania kell – különben a napló egy része láthatatlan marad.
        for (String n : StrengthParse.names())
            assertTrue("nincs izomcsoportja: " + n, Muscles.groupOf(n) != null);
    }

    @Test public void randomTextNeverCrashes() {
        // Egy külön, 120 000 mondatos futtatás nulla hibát talált; a magok
        // váltogatása így is beépült, hogy a CI minden változtatást megfogjon.
        String[] tokens = {"guggolás", "3x10", "60 kg", "fekvenyomás", "és", ",",
                "bicepsz", "12-10-8", "ismétlés", "sorozat", "húzódzkodás", "x",
                "0", "999", "-", "…", "🏋", "", " ", "kg", "szett", "50 fekvőtámasz",
                "3x10x60", "99x99", "0x0", "1x200", "21x1", "12,5 kg", "1000 kg",
                "-5 kg", "hasizom", "lábgép", "mellgép", "arnold", "csuklyás",
                "oldaltámasz", "×", "majd", "utána", ";", "nem", "helyett",
                "kilóval", "kg-mal", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"};
        for (long seed : new long[]{20260802, 42, 777}) {
            java.util.Random rnd = new java.util.Random(seed);
            for (int i = 0; i < 4000; i++) {
                StringBuilder sb = new StringBuilder();
                int n = rnd.nextInt(12);
                for (int w = 0; w < n; w++) {
                    sb.append(tokens[rnd.nextInt(tokens.length)]);
                    if (rnd.nextInt(4) > 0) sb.append(' ');
                }
                String q = sb.toString();
                List<StrengthParse.Item> items = StrengthParse.parse(q);
                for (StrengthParse.Item it : items) {
                    assertTrue(!it.name.isEmpty());
                    assertTrue(it.sets.size() >= 1 && it.sets.size() <= 60);
                    for (StrengthParse.Set s : it.sets) {
                        assertTrue(s.reps >= 1 && s.reps <= 200);
                        assertTrue(s.weight >= 0 && s.weight <= 500);
                    }
                    assertTrue(!it.label().isEmpty());
                }
                // Ugyanaz a mondat mindig ugyanazt adja (nincs rejtett állapot).
                assertEquals(items.size(), StrengthParse.parse(q).size());
            }
        }
    }
    @Test public void theGymWordsRealPeopleWriteAreUnderstood() {
        // Harmincöt valódi terem-mondattal szondáztam a felismerőt; ezek azok,
        // amiken elhasalt.
        assertEquals("Lehúzás 4×12/12/12/12@55", sum("lat húzás 4x12 55"));
        assertEquals("Mellgép 3×12/12/12@45", sum("mellnyomás gépen 3x12 45 kg"));
        assertEquals("Fekvenyomás 4×10/10/10/10@30", sum("ferde padon 4x10 30 kg"));
        assertEquals("Hátfeszítés 3×15/15/15@0", sum("hiperextenzió 3x15"));
    }

    @Test public void aWeightWrittenWithAnInstrumentalSuffixIsNotRepCount() {
        // A „100-zal" súly, nem száz ismétlés: onnantól a rekordok, az 1RM és
        // az izomcsoport-egyensúly is hazudtak volna.
        assertEquals("Guggolás 3×10/10/10@100", sum("guggolás 3x10 100-zal"));
        assertEquals("Fekvenyomás 3×8/8/8@80", sum("fekvenyomás 3x8 80-nal"));
        // Ismétlésszám nélkül inkább semmi, mint kitalált sorozat.
        assertEquals(0, StrengthParse.parse("guggoltam 100-zal").size());
    }
    @Test public void writtenOutNumbersWork() {
        // A teremben ritkán ír bárki számjegyet.
        assertEquals("Guggolás 5×5/5/5/5/5@100", sum("guggolás ötször ötöt 100 kg"));
        assertEquals("Fekvenyomás 4×8/8/8/8@70", sum("fekvenyomás négyszer nyolcat 70 kg"));
        assertEquals("Bicepsz 3×12/12/12@15", sum("bicepsz háromszor tizenkettőt 15 kg"));
        assertEquals("Fekvőtámasz 1×10@0", sum("tíz fekvőtámasz"));
        assertEquals("Húzódzkodás 5×5/5/5/5/5@0", sum("húzódzkodás ötször ötöt"));
        // Ez korábban semmit nem adott: a „100-zal" súly, az „ötször ötöt"
        // pedig öt sorozat öt ismétlés.
        assertEquals("Guggolás 5×5/5/5/5/5@100", sum("guggoltam 100-zal ötször ötöt"));
        // Gyakorlat nélkül továbbra sincs bejegyzés.
        assertEquals(0, StrengthParse.parse("nyomtam háromszor tízet 60 kg").size());
    }
}
