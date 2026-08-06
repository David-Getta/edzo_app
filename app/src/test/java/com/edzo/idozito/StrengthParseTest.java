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

    @Test public void noEverydayWordEverBecomesAnExercise() {
        // Ugyanaz az őrszem-lista, mint az ételeknél (FoodsTest.EVERYDAY): egy
        // új, túl rövid gyakorlat-szótő ugyanúgy tud csendben beleesni egy
        // hétköznapi szóba. Az ismétlésszámot hozzáadjuk, hogy tényleg csak a
        // NÉV döntsön.
        StringBuilder bad = new StringBuilder();
        for (String w : FoodsTest.EVERYDAY) {
            // Az evezőgép valóban evezés: a gép neve maga a gyakorlat.
            if (w.equals("evezőgép")) continue;
            for (StrengthParse.Item it : StrengthParse.parse(w + " 3x10"))
                bad.append("\n  ").append(w).append(" -> ").append(it.name);
        }
        assertEquals("hétköznapi szóból gyakorlat lett:" + bad, 0, bad.length());
        // És izomcsoport sem lesz belőle – a testrészek nevét kivéve, mert az
        // szándékosan besorolható.
        StringBuilder mg = new StringBuilder();
        for (String w : FoodsTest.EVERYDAY) {
            if (Arrays.asList("kar", "váll", "térd", "boka", "csukló", "könyök",
                    "medence", "izom", "gerinc", "nyak", "derék").contains(w)) continue;
            String g = Muscles.groupOf(w);
            if (g != null) mg.append("\n  ").append(w).append(" -> ").append(g);
        }
        assertEquals("hétköznapi szóból izomcsoport lett:" + mg, 0, mg.length());
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
    @Test public void theFeltEffortCanComeFromTheSentence() {
        assertEquals(8, StrengthParse.parse("guggolás 3x10 100 kg rpe 8").get(0).rpe);
        assertEquals(9, StrengthParse.parse("fekvenyomás 3x5 90 kg rpe9").get(0).rpe);
        assertEquals(7, StrengthParse.parse("evezés 4x12 60 kg, 7-es rpe").get(0).rpe);
        // A címkében is látszik.
        assertTrue(StrengthParse.parse("guggolás 3x10 100 kg rpe 8").get(0).label()
                .contains("RPE 8"));
        // Ami nincs a 6–10 sávban, az nem RPE.
        assertEquals(0, StrengthParse.parse("guggolás 3x10 100 kg rpe 3").get(0).rpe);
        assertEquals(0, StrengthParse.parse("guggolás 3x10 100 kg").get(0).rpe);
        // Gyakorlatonként külön: a második mondatrész saját értéket kap.
        java.util.List<StrengthParse.Item> two =
                StrengthParse.parse("guggolás 3x10 100 kg rpe 8, fekvenyomás 3x8 60 kg rpe 10");
        assertEquals(2, two.size());
        assertEquals(8, two.get(0).rpe);
        assertEquals(10, two.get(1).rpe);
    }

    @Test public void theWeightTimesRepsNotationIsUnderstood() {
        // Az erőemelők jelölése: súly × ismétlés. A hatvan nem lehet sorozat.
        List<StrengthParse.Item> r = StrengthParse.parse("fekvenyomás 60x10, 70x8, 80x6");
        assertEquals(1, r.size());
        assertEquals(3, r.get(0).sets.size());
        assertEquals(10, r.get(0).sets.get(0).reps);
        assertEquals(60.0, r.get(0).sets.get(0).weight, 0.001);
        assertEquals(6, r.get(0).sets.get(2).reps);
        assertEquals(80.0, r.get(0).topWeight(), 0.001);
        assertEquals(24, r.get(0).totalReps());
    }

    @Test public void aPyramidWithoutCommasKeepsEverySet() {
        // Vessző nélkül is ugyanaz a mondat – korábban ebből EGY sorozat lett,
        // a másik kettő némán elveszett.
        List<StrengthParse.Item> r = StrengthParse.parse("fekvenyomás 60x10 70x8 80x6");
        assertEquals(1, r.size());
        assertEquals(3, r.get(0).sets.size());
        assertEquals(24, r.get(0).totalReps());
        assertEquals(80.0, r.get(0).topWeight(), 0.001);
        // A folytatás nem lép át a következő gyakorlatba.
        List<StrengthParse.Item> two =
                StrengthParse.parse("guggolás 60x10 70x8 majd fekvenyomás 50x10");
        assertEquals(2, two.size());
        assertEquals(2, two.get(0).sets.size());
        assertEquals(1, two.get(1).sets.size());
        assertEquals(50.0, two.get(1).topWeight(), 0.001);
        // A sorozat × ismétlés alakot a szóköz NEM folytatja: a „3x8” hármasa
        // sorozatszám, nem súly.
        List<StrengthParse.Item> mix = StrengthParse.parse("fekvenyomás 60x10 3x8");
        assertEquals(1, mix.get(0).sets.size());
        assertEquals(10, mix.get(0).totalReps());
    }

    @Test public void aCommaSeparatedRepListNeedsThreeNumbers() {
        // Három számnál a vessző már nem lehet tizedesjel: egy tizedes számban
        // pontosan egy vessző van.
        List<StrengthParse.Item> r = StrengthParse.parse("guggolás 12,10,8 60 kg");
        assertEquals(3, r.get(0).sets.size());
        assertEquals(30, r.get(0).totalReps());
        assertEquals(60.0, r.get(0).topWeight(), 0.001);
        // Kettőnél viszont igen – a „60,5 kg" súly, nem két sorozat.
        List<StrengthParse.Item> dec = StrengthParse.parse("guggolás 3x8 60,5 kg");
        assertEquals(3, dec.get(0).sets.size());
        assertEquals(60.5, dec.get(0).topWeight(), 0.001);
    }

    @Test public void theAtSignMarksTheWeight() {
        // A „@" az edzésnaplók nemzetközi rövidítése a súlyra. Korábban a
        // „5,5,5 @ 100" egyetlen, SZÁZ ismétléses sorozat lett.
        List<StrengthParse.Item> r = StrengthParse.parse("guggolás: 5,5,5 @ 100");
        assertEquals(1, r.size());
        assertEquals(3, r.get(0).sets.size());
        assertEquals(15, r.get(0).totalReps());
        assertEquals(100.0, r.get(0).topWeight(), 0.001);
        List<StrengthParse.Item> x = StrengthParse.parse("guggolás 5x5 @ 100");
        assertEquals(5, x.get(0).sets.size());
        assertEquals(100.0, x.get(0).topWeight(), 0.001);
    }

    @Test public void theNewMachineAndKettlebellNamesAreRecognised() {
        // Ezek eddig NEM léteztek a felismerőnek: az egész mondat elveszett,
        // nem csak a név.
        assertEquals("Kettlebell lendítés",
                StrengthParse.parse("kettlebell swing 5x20 24 kg").get(0).name);
        assertEquals("Kettlebell lendítés", StrengthParse.parse("swing 4x20 24 kg").get(0).name);
        // A „kettlebell" magában nem lendítés: a kettlebell-guggolás guggolás.
        assertEquals("Guggolás",
                StrengthParse.parse("kettlebell guggolás 3x10 20 kg").get(0).name);
        assertEquals("Lábtávolítás", StrengthParse.parse("lábtávolítás 3x15 40 kg").get(0).name);
        assertEquals("Lábközelítés", StrengthParse.parse("lábközelítés 3x15 35 kg").get(0).name);
        assertEquals("Fellépés", StrengthParse.parse("fellépés 3x10 20 kg").get(0).name);
        assertEquals("Alkarhajlítás", StrengthParse.parse("csuklóhajlítás 3x20 8 kg").get(0).name);
        assertEquals("Orosz csavarás", StrengthParse.parse("russian twist 3x20").get(0).name);
        // Az „alkartámasz" a plank magyar neve – nem alkarhajlítás.
        assertEquals("Plank", StrengthParse.parse("alkartámasz 3x60").get(0).name);
    }

    @Test public void aCommaSeparatedEnumerationIsOneEntry() {
        // „ma guggoltam, 5 sorozat, 5 ismétlés, 100 kg" – a darabok külön-külön
        // értelmetlenek, ezért az EGÉSZ mondatból nem lett bejegyzés.
        List<StrengthParse.Item> r =
                StrengthParse.parse("ma guggoltam, 5 sorozat, 5 ismétlés, 100 kg");
        assertEquals(1, r.size());
        assertEquals("Guggolás", r.get(0).name);
        assertEquals(5, r.get(0).sets.size());
        assertEquals(100.0, r.get(0).topWeight(), 0.001);
        // A folytatás csak a puszta mennyiség-töredékre igaz: a „majd 20 perc
        // futás" továbbra sem sorozat.
        assertEquals(3, StrengthParse.parse("guggolás 3x10, majd 20 perc futás")
                .get(0).sets.size());
        // És nem nyeli el a KÖVETKEZŐ gyakorlatot, ha az is súllyal kezdődik.
        List<StrengthParse.Item> two =
                StrengthParse.parse("60 kg guggolás 3x8, 50 kg fekvenyomás 3x8");
        assertEquals(2, two.size());
        assertEquals("Guggolás", two.get(0).name);
        assertEquals(60.0, two.get(0).topWeight(), 0.001);
        assertEquals("Fekvenyomás", two.get(1).name);
        assertEquals(50.0, two.get(1).topWeight(), 0.001);
    }

    @Test public void twoExercisesNeverSwapTheirWeights() {
        // Generatív őrszem: bármelyik két gyakorlat, öt szórendben, és a súly
        // mindig a SAJÁT gyakorlatához tartozik. Az edzés-felismerőben pont
        // ilyen csúszás fordult elő (idő és táv is), és ott is csendes volt.
        String[][] moves = {{"guggolás", "Guggolás"}, {"fekvenyomás", "Fekvenyomás"},
                {"evezés", "Evezés"}, {"bicepsz", "Bicepsz"}};
        StringBuilder bad = new StringBuilder();
        for (String[] x : moves)
            for (String[] y : moves) {
                if (x[1].equals(y[1])) continue;
                String[] forms = {
                        x[0] + " 3x8 80 kg, " + y[0] + " 3x10 40 kg",
                        "80 kg " + x[0] + " 3x8, 40 kg " + y[0] + " 3x10",
                        "3x8 " + x[0] + " 80 kg, 3x10 " + y[0] + " 40 kg",
                        x[0] + " 3x8 80 kg és " + y[0] + " 3x10 40 kg",
                        x[0] + " 80x8, " + y[0] + " 40x10"};
                for (String q : forms) {
                    List<StrengthParse.Item> p = StrengthParse.parse(q);
                    if (p.size() != 2 || !p.get(0).name.equals(x[1])
                            || !p.get(1).name.equals(y[1])
                            || Math.abs(p.get(0).topWeight() - 80) > 0.001
                            || Math.abs(p.get(1).topWeight() - 40) > 0.001) {
                        bad.append("\n  ").append(q).append(" -> ");
                        for (StrengthParse.Item i : p)
                            bad.append(i.name).append("/").append(i.topWeight()).append(" ");
                    }
                }
            }
        assertEquals("elcsúszott a súly:" + bad, 0, bad.length());
    }

    @Test public void aMaxLiftIsASingle() {
        // „fekvenyomás max 120 kg”: a legnehezebb, amit egyszer megnyomott.
        List<StrengthParse.Item> r = StrengthParse.parse("fekvenyomás max 120 kg");
        assertEquals(1, r.size());
        assertEquals(1, r.get(0).sets.size());
        assertEquals(1, r.get(0).totalReps());
        assertEquals(120.0, r.get(0).topWeight(), 0.001);
        // Súly nélkül vagy ismeretlen ismétlésszámmal nem találunk ki semmit.
        assertTrue(StrengthParse.parse("húzódzkodás 3 szett maximumig").isEmpty());
        assertTrue(StrengthParse.parse("fekvenyomás max").isEmpty());
    }

    @Test public void theThreeDigitWeightSurvives() {
        // A „100x3" súlya száz kiló – korábban a százból ismétlés lett.
        List<StrengthParse.Item> r = StrengthParse.parse("guggolás 100x3, 100x3, 100x2");
        assertEquals(3, r.get(0).sets.size());
        assertEquals(8, r.get(0).totalReps());
        assertEquals(100.0, r.get(0).topWeight(), 0.001);
    }

    @Test public void aContinuationClauseKeepsTheExercise() {
        // A vessző utáni sorozat ugyanahhoz a gyakorlathoz tartozik.
        List<StrengthParse.Item> r = StrengthParse.parse("fekvenyomás 3x10 60kg, 2x8 70kg");
        assertEquals(1, r.size());
        assertEquals(5, r.get(0).sets.size());
        assertEquals(46, r.get(0).totalReps());
        assertEquals(70.0, r.get(0).topWeight(), 0.001);
    }

    @Test public void aClauseWithOtherWordsIsNotAContinuation() {
        // A „20 perc futás" húsz perce nem húsz ismétlés.
        List<StrengthParse.Item> r = StrengthParse.parse("guggolás 3x10, majd 20 perc futás");
        assertEquals(1, r.size());
        assertEquals(3, r.get(0).sets.size());
        assertEquals(30, r.get(0).totalReps());
    }

    @Test public void aRoundIsASeries() {
        // „3 kör 10 fekvőtámasz”: a kör itt sorozatot jelent.
        List<StrengthParse.Item> r = StrengthParse.parse("3 kör 10 fekvőtámasz");
        assertEquals(3, r.get(0).sets.size());
        assertEquals(30, r.get(0).totalReps());
        assertEquals(5, StrengthParse.parse("5 kör 20 guggolás").get(0).sets.size());
    }

    @Test public void aSlashSeparatedRepListWorksLikeADash() {
        // A per-jel ugyanolyan gyakori elválasztó, mint a kötőjel.
        List<StrengthParse.Item> r = StrengthParse.parse("fekvenyomás 5/5/5 80 kg");
        assertEquals(3, r.get(0).sets.size());
        assertEquals(15, r.get(0).totalReps());
        assertEquals(80.0, r.get(0).topWeight(), 0.001);
        assertEquals(3, StrengthParse.parse("bicepsz 12/10/8 15 kg").get(0).sets.size());
        // A kötőjeles alak változatlan.
        assertEquals(3, StrengthParse.parse("fekvenyomás 5-5-5 80 kg").get(0).sets.size());
    }

    @Test public void aDecimalWeightIsNotARepList() {
        // A vessző szándékosan NEM elválasztó: a „60,5 kg" tizedes szám, és egy
        // félreolvasott súly rosszabb, mint egy fel nem ismert sorozatlista.
        List<StrengthParse.Item> r = StrengthParse.parse("guggolás 3x10 60,5 kg");
        assertEquals(3, r.get(0).sets.size());
        assertEquals(60.5, r.get(0).topWeight(), 0.001);
        assertEquals(60.5, StrengthParse.parse("guggolás 3x10 60.5kg").get(0).topWeight(),
                0.001);
    }

    @Test public void theSpokenWeightIsUnderstood() {
        // A teremben kimondva is mondják a súlyt – az összetett számnevekkel.
        assertEquals(75.0, StrengthParse.parse("guggolás 5x5 hetvenöt kiló")
                .get(0).topWeight(), 0.001);
        assertEquals(85.0, StrengthParse.parse("fekvenyomás 3x10 nyolcvanöt kg")
                .get(0).topWeight(), 0.001);
        assertEquals(120.0, StrengthParse.parse("guggolás 3x10 százhúsz kilóval")
                .get(0).topWeight(), 0.001);
        assertEquals(110.0, StrengthParse.parse("fekvenyomás 5x5 száztíz kg")
                .get(0).topWeight(), 0.001);
        assertEquals(200.0, StrengthParse.parse("guggolás ötször ötöt kétszáz kiló")
                .get(0).topWeight(), 0.001);
    }
    /**
     * Gyakorlatnév az egyik tagmondatban, sorozat a másikban.
     *
     * A „guggolás 60 kg bemelegítés, aztán 3x5 100" első tagmondatában nincs
     * ismétlésszám, a másodikban nincs név – eddig az EGÉSZ mondat elveszett,
     * pedig együtt teljesen egyértelmű.
     */
    @Test public void theNameCanStandInAnEarlierClause() {
        List<StrengthParse.Item> it =
                StrengthParse.parse("guggolás 60 kg bemelegítés, aztán 3x5 100");
        assertEquals(1, it.size());
        assertEquals("Guggolás", it.get(0).name);
        assertEquals(3, it.get(0).sets.size());
        assertEquals(5, it.get(0).sets.get(0).reps);
        assertEquals(100, it.get(0).sets.get(0).weight, 0.001);
        // A függő név nem ragad rá a KÖVETKEZŐ, saját nevű gyakorlatra.
        List<StrengthParse.Item> b =
                StrengthParse.parse("guggolás, majd fekvenyomás 3x8 60");
        assertEquals(1, b.size());
        assertEquals("Fekvenyomás", b.get(0).name);
    }

    /**
     * A „3x max" hármasa sorozatszám, nem ismétlés.
     *
     * Hármat beírni ismétlésként csendes hazugság lenne – a „3 szett
     * maximumig" alakot ugyanezért nem értjük.
     */
    @Test public void aSetCountIsNotARepCount() {
        assertTrue(StrengthParse.parse("húzódzkodás 3x max").isEmpty());
        assertTrue(StrengthParse.parse("húzódzkodás 3 szett maximumig").isEmpty());
        // A teljes alak viszont megy.
        assertEquals(3, StrengthParse.parse("húzódzkodás 3x8").get(0).sets.size());
    }
    /**
     * Két különböző gyakorlat egy mondatban: mindkettő megmarad, a
     * sorrendjükben.
     *
     * A gyakorlatnevek egymásba érhetnek („fekvenyomás" / „fekvőtámasz",
     * „alkartámasz" / „alkarhajlítás"), és egy ilyen ütközés csendben elnyeli
     * az egyik sorozatot. Az összes névpárt átfuttatjuk.
     */
    @Test public void twoExercisesInOneSentenceBothSurvive() {
        String[] names = StrengthParse.names();
        StringBuilder bad = new StringBuilder();
        for (int i = 0; i < names.length; i++)
            for (int j = 0; j < names.length; j++) {
                if (i == j) continue;
                String q = names[i] + " 3x10, " + names[j] + " 4x8";
                List<StrengthParse.Item> it = StrengthParse.parse(q);
                if (it.size() == 2 && it.get(0).name.equals(names[i])
                        && it.get(1).name.equals(names[j])) continue;
                bad.append("\n  ").append(q).append(" -> ");
                for (StrengthParse.Item x : it) bad.append(x.name).append(' ');
            }
        assertEquals("ütköző gyakorlatnév:" + bad, 0, bad.length());
    }
}
