package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

    /**
     * Az időtartam nem ismétlés.
     *
     * Az „evezés 20 perc" húsz perc evezőgép, nem húsz húzás – a puszta
     * darabszám-szabály viszont ráharapott a számra, és a kardió-mondatból
     * csendben súlyzós bejegyzés lett. Tartásnál a perc továbbra is
     * másodperc: a plank ideje maga a teljesítmény.
     */
    @Test public void aDurationIsNotARepCount() {
        assertEquals("", sum("evezés 20 perc"));
        assertEquals("", sum("evezés 1 óra"));
        assertEquals("", sum("lehúzás 20 percet"));
        // Tartásnál viszont marad, amit eddig is értett.
        assertEquals("Plank 1×120@0", sum("plank 2 perc"));
        assertEquals("Plank 3×60/60/60@0", sum("plank 3x1 perc"));
        // És a valódi sorozat sem sérül.
        assertEquals("Evezés 3×10/10/10@40", sum("evezés 3x10 40 kg"));
    }

    /**
     * A számozott lista sorszáma nem ismétlésszám.
     *
     * A leírt edzésterv gyakran számozott lista, és az „1. guggolás
     * 2. fekvenyomás 3. evezés" tervből egy KÉTismétléses guggolás és egy
     * HÁROMismétléses fekvenyomás került a naplóba – kitalált sorozatok,
     * amik a rekordba és az 1RM-be is beszámítottak.
     */
    @Test public void listMarkersAreNotReps() {
        assertEquals("", sum("1. guggolás 2. fekvenyomás 3. evezés"));
        assertEquals("", sum("1) guggolás 2) fekvenyomás"));
        // A mondatvégi pont és a tizedes szám érintetlen.
        assertEquals("Fekvőtámasz 1×50@0", sum("50 fekvőtámasz."));
        assertEquals("Guggolás 5×5/5/5/5/5@80", sum("guggolás 5x5 80 kg."));
        assertEquals("Bicepsz 3×12/12/12@12,5", sum("bicepsz 3x12 12.5 kg"));
    }

    /**
     * Elgépelt gyakorlatnévre tipp jár.
     *
     * A súlyzós mezőben eddig SEMMI visszajelzés nem jött a fel nem ismert
     * mondatra – a mező néma maradt. Ugyanaz a szigorú szabály, mint az
     * ételeknél: hat betűtől, egyező szókezdettel, egy hibával; a felcserélt
     * betű egy hiba, mert a telefonon az a jellemző elütés.
     */
    @Test public void typosInMoveNamesGetASuggestion() {
        assertEquals("Fekvenyomás", StrengthParse.closestMove("fekvenyomsá 3x10"));
        assertEquals("Vádliemelés", StrengthParse.closestMove("vádliemeles"));
        assertEquals("Bicepsz", StrengthParse.closestMove("biceps 3x12"));
        // Ami nem gyakorlat, arra nincs tipp.
        for (String q : new String[]{"valami", "asztal", "futottam", "3x10",
                "csirkemell", "szeretem", ""})
            assertNull(q, StrengthParse.closestMove(q));
        assertNull(StrengthParse.closestMove(null));
    }

    /**
     * A TERV nem napló.
     *
     * A „holnap guggolás 5x5 100 kg" és a „kellene 5x5 80 kg-ot guggolnom"
     * ugyanazokból a számokból áll, mint a megtörtént sorozat – csak épp még
     * nem történt meg. A mozgás-oldalon ez a szabály régóta megvolt, itt
     * hiányzott: a kitalált sorozat a rekordba, az 1RM-be és a
     * progresszió-javaslatba is beszámított.
     */
    @Test public void plansAreNotEntries() {
        for (String q : new String[]{"holnap guggolás 5x5 100 kg",
                "kellene 5x5 80 kg-ot guggolnom", "a terv: guggolás 5x5 100 kg",
                "jövő héten guggolás 5x5 100 kg", "szeretnék 100 kg-ot nyomni"})
            assertEquals(q, "", sum(q));
        // A megtörtént sorozat változatlan.
        assertEquals("Guggolás 5×5/5/5/5/5@100", sum("guggolás 5x5 100 kg"));
        assertEquals("Guggolás 5×5/5/5/5/5@100", sum("tegnap guggolás 5x5 100 kg"));
    }

    /**
     * A kiírt kiló a szorzójel előtt: „60 kg x 10".
     *
     * A súly×ismétlés írásmódot az app régóta érti („fekvenyomás 60x10,
     * 70x8"), csakhogy a legtöbb edzés-app ÍGY exportál, kiírt kilóval. A
     * bemásolt sorból emiatt egyáltalán nem lett bejegyzés – se gyakorlat,
     * se sorozat, pedig minden adat ott volt benne.
     */
    @Test public void theKilogramMayStandBeforeTheMultiplier() {
        assertEquals("Fekvenyomás 3×10/8/6@80",
                sum("Fekvenyomás 3 sorozat: 60kg x 10, 70kg x 8, 80kg x 6"));
        assertEquals("Guggolás 3×5/5/5@100",
                sum("Guggolás: 100 kg x 5, 100 kg x 5, 100 kg x 5"));
        // A régi alakok változatlanok.
        assertEquals("Fekvenyomás 3×10/8/6@80", sum("fekvenyomás 60x10, 70x8, 80x6"));
        assertEquals("Guggolás 3×10/10/10@60", sum("guggolás 3x10 60 kg"));
    }

    /**
     * Köredzés: a kör-szám az EGÉSZ listára vonatkozik.
     *
     * Az „5 kör – 20 burpee, 15 fekvőtámasz, 10 húzódzkodás" a chatben
     * megosztott edzés tipikus alakja. Eddig minden gyakorlatból EGY sorozat
     * lett, vagyis a napló a munka ötödét mutatta – és a rekordok, az
     * izomegyensúly meg a heti volumen is abból számolt.
     */
    @Test public void aCircuitRoundCountAppliesToEveryMove() {
        assertEquals("Fekvőtámasz 5×15/15/15/15/15@0 | Húzódzkodás 5×10/10/10/10/10@0",
                sum("5 kör - 15 fekvőtámasz, 10 húzódzkodás"));
        assertEquals("Fekvőtámasz 5×15/15/15/15/15@0 | Húzódzkodás 5×10/10/10/10/10@0",
                sum("5 kör: 15 fekvőtámasz, 10 húzódzkodás"));
        assertEquals("Fekvőtámasz 5×15/15/15/15/15@0 | Húzódzkodás 5×10/10/10/10/10@0",
                sum("5 kör 15 fekvőtámasz, 10 húzódzkodás"));
        // Ahol van saját sorozatszám, ott azt hagyjuk békén.
        assertEquals("Guggolás 3×10/10/10@60 | Fekvenyomás 3×8/8/8@50",
                sum("3 kör: guggolás 3x10 60 kg, fekvenyomás 3x8 50 kg"));
        // Egyetlen gyakorlatnál változatlan a régi viselkedés.
        assertEquals("Fekvőtámasz 3×10/10/10@0", sum("3 kör 10 fekvőtámasz"));
    }

    /**
     * Minden gyakorlat-szótő minden ragozott alakja a saját gyakorlatát adja.
     *
     * Ugyanaz a söprés, ami az ételeknél és a mozgásformáknál fut. Itt
     * jelenleg NINCS kivétel: mind a tizenkilenc rag átmegy minden tövön –
     * és ez a nulla az, amit őrizni érdemes.
     */
    @Test public void everyMoveStemSurvivesItsInflections() {
        String[] suf = {"", "t", "ba", "bol", "ban", "val", "hoz", "nak", "n",
                "ra", "rol", "tol", "nal", "os", "as", "es", "om", "unk", "ok"};
        StringBuilder bad = new StringBuilder();
        for (String[] row : StrengthParse.MOVES)
            for (int i = 1; i < row.length; i++) {
                if (row[i].indexOf(' ') >= 0) continue;
                for (String x : suf) {
                    boolean ok = false;
                    for (StrengthParse.Item it : StrengthParse.parse(row[i] + x + " 3x10"))
                        if (it.name.equals(row[0])) ok = true;
                    if (!ok) bad.append("\n  ").append(row[i]).append(x)
                            .append(" (").append(row[0]).append(")");
                }
            }
        assertEquals("elveszett ragozott gyakorlat-alak:" + bad, 0, bad.length());
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

    /**
     * Vessző nélküli felsorolás: „5 kör 10 fekvőtámasz 15 guggolás 20 hasizom".
     *
     * A megosztott köredzés így néz ki – a magyar felsorolásban a vessző
     * elmarad, mert a szám maga tagol. Eddig az EGÉSZ lista egy tagmondat
     * volt: a guggolás megkapta a hasizom ismétlésszámát, a hasprés pedig
     * egyáltalán nem került be.
     */
    @Test public void aListWithoutCommasIsStillAList() {
        assertEquals("Fekvőtámasz 5×10/10/10/10/10@0 | Guggolás 5×15/15/15/15/15@0"
                        + " | Hasprés 5×20/20/20/20/20@0",
                sum("köredzés a parkban: 5 kör 10 fekvőtámasz 15 guggolás 20 hasizom"));
        assertEquals("Fekvőtámasz 1×20@0 | Guggolás 1×30@0",
                sum("20 fekvőtámasz 30 guggolás"));
        // A súly nem nyit új tételt: a „3x10 fekvenyomás 60 kg" egy gyakorlat.
        assertEquals("Fekvenyomás 3×10/10/10@60", sum("3x10 fekvenyomás 60 kg"));
        assertEquals("Kettlebell lendítés 5×15/15/15/15/15@16", sum("kettlebell 16 kg 5x15"));
    }

    /**
     * A CSÚCS-mondat: „végre lement a 100 kg-os fekvenyomás".
     *
     * Ismétlésszám nincs benne, mert egyszeri – és pont ez az a bejegyzés,
     * amit az ember a legjobban szeretne látni a naplóban. Eddig egyik sem
     * került be: sem a rekord, sem az 1RM, sem a progresszió nem tudott róla.
     */
    @Test public void aPersonalRecordSentenceIsSaved() {
        assertEquals("Fekvenyomás 1×1@100", sum("végre lement a 100 kg-os fekvenyomás"));
        assertEquals("Felhúzás 1×1@200", sum("sikerült a 200 kg-os holtemelés"));
        assertEquals("Guggolás 1×1@150",
                sum("megdöntöttem a rekordomat guggolásban: 150 kg"));
        // Csúcs-szó nélkül továbbra sem találunk ki ismétlést.
        assertEquals(0, StrengthParse.parse("100 kg-os fekvenyomás").size());
        // A csúcs UTÁN jövő munkasorozatok sem veszhetnek el.
        assertEquals("Fekvenyomás 4×1/8/8/8@100",
                sum("végre lement a 100 kg-os fekvenyomás, és utána 3x8 80 kg-mal dolgoztam"));
    }

    /**
     * A folytatás magyar mondatban kötőszóval és igével érkezik.
     *
     * A minta szigorú marad – bármi MÁS szó azt jelenti, hogy nem sorozatról
     * van szó –, de a kötőszót és a záró igét leszedjük: az „…és utána 3x8
     * 80 kg-mal dolgoztam" ugyanaz a folytatás, mint a puszta „3x8 80".
     */
    @Test public void aContinuationMaySpeakHungarian() {
        assertEquals("Guggolás 5×10/10/10/8/8@70", sum("guggolás 3x10, aztán 2x8 70 kg"));
        // A bemelegítés utáni MUNKASOROZAT is folytatás – eddig csak a
        // felvezető sorozat került be, a valódi munka nem.
        assertEquals("Fekvenyomás 4×10/8/8/8@70",
                sum("fekvenyomás bemelegítés 40x10, munkasorozat 3x8 70"));
        // A húsz perc futás továbbra sem húsz ismétlés.
        assertEquals("Fekvenyomás 3×10/10/10@0", sum("fekvenyomás 3x10, majd 20 perc futás"));
    }

    @Test public void lyingDownIsNotBenchPressing() {
        // A „fekve" szótő a fordított szórendet fogja, de az alvás-mondat is
        // tartalmazza: a „lefekvés 23:15, ébredés 6:45" huszonhárom
        // ismétléses fekvenyomás lett a naplóban.
        assertEquals(0, StrengthParse.parse("lefekvés 23:15, ébredés 6:45").size());
        assertEquals(0, StrengthParse.parse("lefeküdtem 23-kor").size());
        // A fordított szórendű valódi sorozat változatlan.
        assertEquals("Fekvenyomás 1×5@100", sum("nyomtam 100 kilót fekve ötöt"));
    }

    @Test public void aMeasurementUnitIsNotAWeight() {
        // A „ládaugrás 4x8 60 cm" hatvanas száma a doboz MAGASSÁGA – eddig
        // hatvan kilós ládaugrás került a rekordba.
        assertEquals("Ládaugrás 4×8/8/8/8@0", sum("ládaugrás 4x8 60 cm"));
        // A mértékegység nélküli szám továbbra is súly.
        assertEquals("Guggolás 3×10/10/10@60", sum("guggolás 3x10 60"));
    }

    @Test public void aPercentageIsNotAWeight() {
        // A „@70%" a maximum arányát mondja, nem a rúdon lévő súlyt – a
        // mondat meg sem mondja, mennyi volt. Hetven kilóként viszont bekerült
        // a rekordba, az 1RM-becslésbe és a progresszió-javaslatba is.
        assertEquals("Guggolás 3×8/8/8@0", sum("guggolás 3x8 @70%"));
        assertEquals("Fekvenyomás 5×3/3/3/3/3@0", sum("fekvenyomás 5x3 85%-on"));
        // A kukac utáni szám mértékegység nélkül továbbra is súly.
        assertEquals("Guggolás 3×8/8/8@100", sum("guggolás 3x8 @ 100"));
    }

    @Test public void aSpacedRepListIsStillAList() {
        // A tagmondat-vágó a vesszőnél vág, ha szóköz követi: a „12, 10, 8"
        // tízese és nyolcasa külön, névtelen tagmondatba került, és némán
        // elveszett – a naplóban a sorozatok harmada maradt.
        assertEquals("Húzódzkodás 3×12/10/8@0", sum("húzódzkodás max ismétlés: 12, 10, 8"));
        assertEquals("Bicepsz 3×12/10/8@0", sum("bicepsz 12, 10, 8"));
        // Két szám még lehet tizedes vagy két külön dolog – ahhoz nem nyúlunk.
        assertEquals("Guggolás 3×10/10/10@60", sum("guggolás 3x10 60 kg"));
    }

    @Test public void aThreeDigitNumberOnABarbellMoveIsKilograms() {
        // A „leguggoltam 140-et" száznegyven KILÓ – száznegyven guggolás nem
        // létezik egy rúddal. Eddig ismétlésnek olvastuk, és a rekord, az 1RM
        // és a progresszió-javaslat is ebből számolt.
        assertEquals("Guggolás 1×1@140", sum("leguggoltam 140-et először életemben"));
        assertEquals("Fekvenyomás 1×1@120", sum("fekvenyomás 120"));
        assertEquals("Felhúzás 1×1@180", sum("felhúzás 180-at húztam"));
        assertEquals("Fekvenyomás 1×1@100", sum("nyomtam 100-at fekve"));
        // De ha a szám a gyakorlat nevét jelzi, az darabszám – magyarul csak
        // így mondjuk –, és a saját testsúlyos mozdulatokat sem érinti.
        assertEquals("Guggolás 1×100@0", sum("csináltam 100 guggolást"));
        assertEquals("Fekvőtámasz 1×100@0", sum("100 fekvőtámasz"));
        assertEquals("Guggolás 1×20@0", sum("guggolás 20"));
    }

    @Test public void anInjuryReportIsNotAWorkoutLog() {
        // A „vádlimba” szóban ott a „vádli” szótő, ismétlésszám viszont nincs:
        // a sérülés bejelentéséből eddig „Vádliemelés · 1 · saját testsúly”
        // lett, és az bekerült a naplóba.
        for (String q : new String[]{"kaptam egy húzódást a vádlimba futás közben",
                "fáj a vállam a tegnapi fekvenyomástól",
                "megrándult a bokám guggolás közben",
                "gyulladt a könyököm a bicepsztől"}) {
            assertEquals("kitalált sorozat panaszból: " + q, 0, StrengthParse.parse(q).size());
        }
        // De ami MEGTÖRTÉNT, az megtörtént – a fájdalom nem törli a sorozatot.
        assertEquals("Fekvenyomás 3×8/8/8@60",
                sum("fájt a vállam, mégis fekvenyomás 3x8 60 kg"));
        assertEquals("Guggolás 3×10/10/10@0",
                sum("guggolás 3x10, közben megrándult a térdem"));
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
        // A jelzős változatok többsége az alapgyakorlathoz esik – a súlyuk
        // nagyjából ugyanaz, és külön nyilvántartva csak szétaprózódna a
        // rekord.
        assertEquals("Guggolás", StrengthParse.parse("elöl guggolás 3x5 60 kg").get(0).name);
        // Ahol viszont a súly nagyságrendben más, ott KÜLÖN gyakorlat: a
        // román felhúzás jóval könnyebb a holtemelésnél, a bolgár kitörés egy
        // lábra megy, a ferde pad pedig a vállnak dolgoztat. Egy vödörbe téve
        // a progresszió a nehezebbik súlyát kínálná a könnyebbikhez, és a
        // rekord sosem dőlne meg a könnyebbikkel.
        assertEquals("Román felhúzás", StrengthParse.parse("román felhúzás 3x8 80 kg").get(0).name);
        assertEquals("Bolgár kitörés", StrengthParse.parse("bolgár kitörés 3x10 20 kg").get(0).name);
        assertEquals("Ferde fekvenyomás", StrengthParse.parse("ferde fekvenyomás 3x8 60 kg").get(0).name);
        // …de a bázis a maga nevén marad.
        assertEquals("Felhúzás", StrengthParse.parse("felhúzás 3x8 120 kg").get(0).name);
        assertEquals("Kitörés", StrengthParse.parse("kitörés 3x10 20 kg").get(0).name);
        assertEquals("Fekvenyomás", StrengthParse.parse("fekvenyomás 3x8 80 kg").get(0).name);
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
                    "medence", "izom", "gerinc", "nyak", "derék",
                    // A „combos" melléknév a comb tövét viseli: az
                    // izomcsoport-címke ára itt egy jelző, nem egy kitalált
                    // bejegyzés – a naplóba semmi nem kerül tőle.
                    "combos").contains(w)) continue;
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
        assertEquals("Ferde fekvenyomás 4×10/10/10/10@30", sum("ferde padon 4x10 30 kg"));
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
        // Egy kilónál könnyebb sorozat nincs: a „fél testsúllyal" fele nem súly.
        assertEquals(0.0, StrengthParse.parse("fekvenyomás 3x8 fél testsúllyal")
                .get(0).sets.get(0).weight, 0.001);
        // A valódi kis súly viszont megmarad.
        assertEquals(1.5, StrengthParse.parse("bicepsz 3x12 1,5 kg")
                .get(0).sets.get(0).weight, 0.001);
        assertEquals(1.5, StrengthParse.parse("bicepsz 3x12 másfél kiló")
                .get(0).sets.get(0).weight, 0.001);
        // A termi anglicizmusok a magyar nevükre futnak be.
        assertEquals("Combhajlítás", StrengthParse.parse("leg curl 3x12 40 kg").get(0).name);
        assertEquals("Lábnyújtás", StrengthParse.parse("leg extension 3x12 45 kg").get(0).name);
        assertEquals("Mellgép", StrengthParse.parse("chest press 3x10 60 kg").get(0).name);
        assertEquals("Mellgép", StrengthParse.parse("pec deck 3x12 50 kg").get(0).name);
        assertEquals("Vállból nyomás",
                StrengthParse.parse("shoulder press 3x10 40 kg").get(0).name);
        assertEquals("Evezés", StrengthParse.parse("cable row 3x10 55 kg").get(0).name);
        assertEquals("Evezés", StrengthParse.parse("pendlay row 5x5 70 kg").get(0).name);
        assertEquals("Tricepsz", StrengthParse.parse("skull crusher 3x10 25 kg").get(0).name);
        assertEquals("Bicepsz", StrengthParse.parse("hammer curl 3x12 14 kg").get(0).name);
        assertEquals("Tolódzkodás", StrengthParse.parse("dip 3x10").get(0).name);
        // A térdemelés ugyanaz a hasizom-gyakorlat, csak hajlított lábbal.
        assertEquals("Lábemelés", StrengthParse.parse("térdemelés 3x15").get(0).name);
        // A RIR a tartalék-ismétlés: RIR 2 = RPE 8. Az öt fölötti szám nem RIR.
        assertEquals(8, StrengthParse.parse("guggolás 3x10 100 kg rir 2").get(0).rpe);
        assertEquals(10, StrengthParse.parse("felhúzás 1x1 180 kg rir 0").get(0).rpe);
        assertEquals(0, StrengthParse.parse("guggolás 3x10 100 kg rir 7").get(0).rpe);
        // A kg utáni @szám a 6–10 sávban RPE; a kg nélküli „@ 100" súly marad.
        assertEquals(8, StrengthParse.parse("fekvenyomás 5x5 90 kg @8").get(0).rpe);
        StrengthParse.Item at = StrengthParse.parse("guggolás 5,5,5 @ 100").get(0);
        assertEquals(0, at.rpe);
        assertEquals(100.0, at.sets.get(0).weight, 0.01);
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
    /**
     * A gyakorlatok IGEALAKJAI is felismerhetők, ha egyértelműek.
     *
     * A „nyomtam", a „húztam" és a „toltam" szándékosan NEM: azokból nem
     * derül ki, melyik gyakorlatról van szó, és egy találgatott gyakorlatnév
     * rosszabb, mint a hiány. Az „eveztem" viszont egyértelmű.
     */
    @Test public void unambiguousVerbFormsAreUnderstood() {
        assertEquals("Evezés", StrengthParse.parse("eveztem 3x10 50 kg").get(0).name);
        assertEquals("Felülés", StrengthParse.parse("felültem 3x20").get(0).name);
        assertEquals("Kitörés", StrengthParse.parse("kitörtem 3x12").get(0).name);
        assertEquals("Fekvenyomás",
                StrengthParse.parse("mellet nyomtam 3x10 60 kg").get(0).name);
        // A többértelmű igék továbbra sem találgatnak.
        for (String q : new String[]{"nyomtam 3x10 60 kg", "húztam 3x10 60 kg",
                "toltam 3x10 60 kg", "emeltem 3x10 60 kg"})
            assertTrue(q, StrengthParse.parse(q).isEmpty());
    }
    /**
     * A „helyett" előtti gyakorlat nem történt meg.
     *
     * A „guggolás 3x10 helyett fekvenyomás 3x8" mondatból eddig MINDKÉT
     * gyakorlat bekerült a naplóba – az is, amit az ember épp kihagyott. Az
     * étkezés-oldalon ez a szabály régóta megvolt.
     */
    @Test public void whatComesBeforeInsteadOfDidNotHappen() {
        List<StrengthParse.Item> it =
                StrengthParse.parse("guggolás 3x10 helyett fekvenyomás 3x8");
        assertEquals(1, it.size());
        assertEquals("Fekvenyomás", it.get(0).name);
        // Kötőszóval felsorolva továbbra is mindkettő megvan.
        assertEquals(2, StrengthParse.parse("guggolás 3x10, fekvenyomás 3x8").size());
    }

    /**
     * „4 sorozat 8 fekvenyomás” – az „ismétlés" szó kimondatlan marad.
     *
     * A teremben senki nem mondja ki: a sorozatszám után álló szám maga az
     * ismétlés. Eddig ebből a mondatból SEMMI nem lett – a felismerő látta a
     * sorozatszámot, ismétlést nem talált hozzá, és inkább kiszállt. A „3x8"
     * alak működött, a szavakkal kimondott ugyanaz nem.
     */
    @Test public void setCountFollowedByRepsNeedsNoRepWord() {
        assertSets("4 sorozat 8 fekvenyomás", "Fekvenyomás", 4, 8, 0);
        assertSets("négy sorozat nyolc fekvenyomás", "Fekvenyomás", 4, 8, 0);
        assertSets("3 szett 12 bicepsz", "Bicepsz", 3, 12, 0);
        assertSets("5 sorozat 5 felhúzás 100 kg", "Felhúzás", 5, 5, 100);
        assertSets("3 kör 10 fekvőtámasz", "Fekvőtámasz", 3, 10, 0);
        // Két gyakorlat egy mondatban, mindkettő ilyen alakban.
        assertEquals(2, StrengthParse.parse(
                "3 sorozat 10 guggolás, 4 sorozat 8 fekvenyomás").size());
    }

    /**
     * …de a sorozatszám után álló szám nem mindig ismétlés.
     *
     * Ha súly vagy időtartam következik, akkor az ismétlésszám továbbra is
     * ismeretlen – és egy kitalált nyolcas rosszabb, mint a felismerés
     * elmaradása.
     */
    @Test public void aWeightAfterTheSetCountIsNotARepCount() {
        for (String q : new String[]{"3 sorozat guggolás", "3 sorozat 60 kg guggolás",
                "3 szett maximumig fekvenyomás", "3 sorozat 2 perc pihenő guggolás"})
            assertTrue(q, StrengthParse.parse(q).isEmpty());
        // Tartásnál viszont a másodperc az ismétlés helyén áll: az marad.
        assertSets("3 sorozat 45 mp plank", "Plank", 3, 45, 0);
        assertSets("3 sorozat 1 perc plank", "Plank", 3, 60, 0);
    }

    private static void assertSets(String q, String name, int sets, int reps, double kg) {
        List<StrengthParse.Item> it = StrengthParse.parse(q);
        assertEquals(q, 1, it.size());
        assertEquals(q, name, it.get(0).name);
        assertEquals(q, sets, it.get(0).sets.size());
        for (StrengthParse.Set s : it.get(0).sets) {
            assertEquals(q, reps, s.reps);
            assertEquals(q, kg, s.weight, 0.01);
        }
    }

    /**
     * A megosztott bejegyzés visszaolvasható – ez a megosztás értelme.
     *
     * A sorozat szövegként megy tovább (üzenetben, edzőnek), a másik
     * telefonon pedig ugyanez a felismerő teszi a naplóba. Ha a két oldal
     * elcsúszna, a kapott sorozat csendben MÁS lenne, mint a küldött:
     * más súllyal, más ismétléssel, esetleg más gyakorlat néven.
     */
    @Test public void everySharedEntryReadsBackTheSame() {
        double[] ws = {0, 20, 42.5, 60, 100, 140};
        int[][] repSets = {{10, 10, 10}, {8}, {12, 10, 8}, {5, 5, 5, 5, 5}, {20, 20},
                {60, 60, 60}};
        StringBuilder bad = new StringBuilder();
        int n = 0;
        for (String m : StrengthParse.names())
            for (double w : ws)
                for (int[] rs : repSets) {
                    java.util.List<StrengthLog.SetEntry> sets = new java.util.ArrayList<>();
                    for (int r : rs) sets.add(new StrengthLog.SetEntry(r, w));
                    String q = StrengthLog.sentence(m, sets);
                    n++;
                    List<StrengthParse.Item> it = StrengthParse.parse(q);
                    if (it.size() != 1) { bad.append("\n  „").append(q).append("” -> ")
                            .append(it.size()).append(" gyakorlat"); continue; }
                    StrengthParse.Item i2 = it.get(0);
                    if (!i2.name.equals(m)) bad.append("\n  „").append(q).append("” -> ").append(i2.name);
                    else if (i2.sets.size() != rs.length)
                        bad.append("\n  „").append(q).append("” -> ").append(i2.sets.size()).append(" sorozat");
                    else if (Math.abs(i2.topWeight() - w) > 0.01)
                        bad.append("\n  „").append(q).append("” -> ").append(i2.topWeight()).append(" kg");
                }
        assertTrue("legalább ezer bejegyzést néztünk", n > 1000);
        assertEquals("oda-vissza eltérés:" + bad, 0, bad.length());
    }

    /**
     * A hatvan mindennapi gyakorlatnévvel végigpróbált hiánylista.
     *
     * Ez a hat teljesen hiányzott, a többi négy csak más néven volt meg. Aki
     * ezeket írja be, eddig „nem ismerem" választ kapott – pedig a mondat
     * tökéletes volt.
     */
    @Test public void theNewlyAddedNamesAreUnderstood() {
        String[][] cases = {{"good morning 3x10 40 kg", "Good morning"},
                {"farmerjárás 3x30", "Farmerjárás"}, {"szakítás 5x3 60 kg", "Szakítás"},
                {"lökés 3x2 80 kg", "Lökés"}, {"mellrepülés 3x12 20 kg", "Mellgép"},
                {"pullover 3x12", "Lehúzás"}, {"áthúzás 3x12 25 kg", "Lehúzás"},
                {"elülső vállemelés 3x12 8 kg", "Oldalemelés"},
                {"hyperextension 3x15", "Hátfeszítés"},
                {"medence emelés 3x10 60 kg", "Csípőemelés"}};
        for (String[] c : cases) {
            java.util.List<StrengthParse.Item> it = StrengthParse.parse(c[0]);
            assertEquals(c[0], 1, it.size());
            assertEquals(c[0], c[1], it.get(0).name);
        }
    }

    /**
     * A csillag ugyanaz a szorzójel, mint az x.
     *
     * A telefon billentyűzetén a csillag van kéznél, az x-hez betűre kell
     * váltani. A „3*10 60 kg" eddig három ISMÉTLÉS volt súly nélkül – nem
     * hibaüzenet, csak csendben más.
     */
    @Test public void asteriskIsAMultiplicationSign() {
        java.util.List<StrengthParse.Item> it = StrengthParse.parse("fekvenyomás 3*10 60 kg");
        assertEquals(1, it.size());
        assertEquals(3, it.get(0).sets.size());
        assertEquals(10, it.get(0).sets.get(0).reps);
        assertEquals(60, it.get(0).topWeight(), 0.01);
        assertEquals(3, StrengthParse.parse("fekvenyomás 3 * 10 60 kg").get(0).sets.size());
    }

    /**
     * Ahogy a teremben tényleg leírják: kötőjellel, tagolva, hátul a súllyal.
     *
     * Három valódi alak veszett el eddig. A „3-szor 10-et" hármasa
     * ISMÉTLÉSSZÁM lett, a tíz eltűnt. A „3 sorozat, egyenként 8 ismétlés"
     * hármasa tűnt el, egyetlen nyolcas sorozat maradt. Az „5x5 guggolás
     * 100" száz kilója pedig saját testsúllyá vált – a rekordokba és a
     * heti terhelésbe is nullaként.
     */
    @Test public void theWayPeopleActuallyWriteItDown() {
        List<StrengthParse.Item> a = StrengthParse.parse("guggolás 3-szor 10-et 80 kilóval");
        assertEquals(1, a.size());
        assertEquals(3, a.get(0).sets.size());
        assertEquals(10, a.get(0).sets.get(0).reps);
        assertEquals(80.0, a.get(0).sets.get(0).weight, 0.001);

        List<StrengthParse.Item> b =
                StrengthParse.parse("guggolás 3 sorozat, egyenként 8 ismétlés, 90 kg");
        assertEquals(1, b.size());
        assertEquals(3, b.get(0).sets.size());
        assertEquals(8, b.get(0).sets.get(0).reps);
        assertEquals(90.0, b.get(0).sets.get(0).weight, 0.001);

        List<StrengthParse.Item> c = StrengthParse.parse("ma 5x5 guggolás 100");
        assertEquals(1, c.size());
        assertEquals(5, c.get(0).sets.size());
        assertEquals(100.0, c.get(0).sets.get(0).weight, 0.001);

        // A terhelés-jelölés viszont NEM súly: a „rpe 8" nyolcasa nyolc.
        List<StrengthParse.Item> d = StrengthParse.parse("guggolás 3x10 rpe 8");
        assertEquals(1, d.size());
        assertEquals(0.0, d.get(0).sets.get(0).weight, 0.001);
    }

    /**
     * Hetvenhárom gyakorlatnév végigpróbálva: ezek hiányoztak.
     *
     * A nordic curl és a madár-kutya a rehab-sorokból ismerős, a hasgurító
     * és a holt bogár a törzs klasszikusai, a kábeles keresztezés pedig
     * ugyanaz a mozgás, mint a tárogatás, csak más eszközzel. Mind a
     * naplóban is ugyanazon a néven él.
     */
    @Test public void theSecondSweepOfExerciseNames() {
        String[][] want = {
                {"kábeles keresztezés 3x12 20 kg", "Mellgép"},
                {"arcra húzás 3x15", "Fordított tárogatás"},
                {"kerékkel gurítás 3x10", "Hasgurító"},
                {"box ugrás 4x5", "Ládaugrás"},
                {"nordic curl 3x5", "Nordic curl"},
                {"dead bug 3x10", "Holt bogár"},
                {"madár-kutya 3x8", "Madár-kutya"},
                {"medvejárás 3x20", "Medvejárás"},
                {"vállgép 3x12 30 kg", "Vállból nyomás"}};
        StringBuilder bad = new StringBuilder();
        for (String[] w : want) {
            List<StrengthParse.Item> i = StrengthParse.parse(w[0]);
            String got = i.isEmpty() ? "-" : i.get(0).name;
            if (!got.equals(w[1]))
                bad.append("\n  ").append(w[0]).append(" -> ").append(got);
        }
        assertEquals("hiányzó vagy rossz gyakorlatnév:" + bad, 0, bad.length());
        // A burpee szándékosan marad kardió: az izomcsoport-kimutatásba
        // beszámítva azt hazudná, hogy a láb erősítő munkát kapott.
        assertTrue(StrengthParse.parse("burpee 3x10").isEmpty());
        // A magyar terem fordított szórenddel is mondja: „nyomtam 100 kilót
        // fekve ötöt". A „fekve" magában is fekvenyomás – a fekvőtámasz
        // szótöve más, tehát nem ütközik vele.
        List<StrengthParse.Item> f = StrengthParse.parse("nyomtam 100 kilót fekve ötöt");
        assertEquals(1, f.size());
        assertEquals("Fekvenyomás", f.get(0).name);
        assertEquals(5, f.get(0).sets.get(0).reps);
        assertEquals(100.0, f.get(0).sets.get(0).weight, 0.001);
        assertEquals("Fekvőtámasz", StrengthParse.parse("fekvőtámasz 3x20").get(0).name);
        assertEquals("Fekvőtámasz", StrengthParse.parse("3 kör 10 fekvőtámasz").get(0).name);
        // A puszta „kettlebell" más gyakorlatnév mellett nem mond semmit, de
        // egyedül mindenki a lendítésre gondol: a „kettlebell 16 kg 5x15"
        // eddig edzés-bejegyzés lett, sorozat és súly nélkül.
        List<StrengthParse.Item> kb = StrengthParse.parse("kettlebell 16 kg 5x15");
        assertEquals("Kettlebell lendítés", kb.get(0).name);
        assertEquals(5, kb.get(0).sets.size());
        assertEquals(16.0, kb.get(0).sets.get(0).weight, 0.001);
        assertEquals("Guggolás",
                StrengthParse.parse("kettlebell guggolás 3x10 20 kg").get(0).name);
        // Sorozat nélkül továbbra sincs bejegyzés.
        assertTrue(StrengthParse.parse("kettlebell 24 kg").isEmpty());
        // Perjeles súly/ismétlés: ugyanaz a piramis, amit az „60x10" alakkal
        // már értettünk – csak a teremben sokan perjellel írják.
        List<StrengthParse.Item> sl = StrengthParse.parse("fekvenyomás: 60/10, 70/8, 80/6");
        assertEquals(1, sl.size());
        assertEquals(3, sl.get(0).sets.size());
        assertEquals(10, sl.get(0).sets.get(0).reps);
        assertEquals(60.0, sl.get(0).sets.get(0).weight, 0.001);
        assertEquals(80.0, sl.get(0).topWeight(), 0.001);
        // A TEMPÓ-jelölés nem ismétlés: a „tempó 3-1-1-0" négy szakasz
        // másodperce (le, alul, fel, fent), nem négy sorozat.
        assertTrue(StrengthParse.parse("tempó 3-1-1-0 fekvenyomás").isEmpty());
        assertTrue(StrengthParse.parse("fekvenyomás 3-1-1-0 tempóval").isEmpty());
        // Tempó-szó nélkül a kötőjeles lista továbbra is piramis.
        assertEquals(3, StrengthParse.parse("bicepsz 12-10-8 15 kg").get(0).sets.size());
        assertEquals(5, StrengthParse.parse("piramis: 10-8-6-4-2 guggolás").get(0).sets.size());
        // A RITMUS-jelölés nem súly/ismétlés: a „40/20" időzítő marad.
        assertTrue(StrengthParse.parse("8x20/10").isEmpty());
        assertTrue(StrengthParse.parse("40/20 8 kör").isEmpty());
    }

    /**
     * Az óraállás nem ismétlésszám.
     *
     * Az óra-export így írja le a kardiót: „evezőgép 5000 m 21:45". A
     * huszonegy eddig ismétlésszám lett, és a húszperces evezésből
     * huszonegy ismétléses gyakorlat került az erősítő naplóba – a rekordok
     * és a progresszió-javaslat közé.
     */
    @Test public void aClockTimeIsNotARepCount() {
        assertTrue(StrengthParse.parse("evezőgép 5000 m 21:45").isEmpty());
        assertTrue(StrengthParse.parse("úszás 1500 m 32:10").isEmpty());
        // A valódi sorozat marad, időponttal együtt is.
        List<StrengthParse.Item> it = StrengthParse.parse("18:00-kor fekvenyomás 3x8 60 kg");
        assertEquals(1, it.size());
        assertEquals(3, it.get(0).sets.size());
        assertEquals(60, it.get(0).sets.get(0).weight, 0.01);
    }

    /**
     * A „holt emelés" külön írva is ugyanaz a gyakorlat.
     *
     * Sokan így írják, és eddig SEMMI nem lett a mondatból: se sorozat, se
     * edzés – a „holt emelés 1x5 140 kg" üres választ kapott.
     */
    @Test public void theDeadliftIsRecognisedSpelledApart() {
        assertEquals("Felhúzás", StrengthParse.parse("holt emelés 1x5 140 kg")
                .get(0).name);
        assertEquals("Felhúzás", StrengthParse.parse("dead lift 5x5 120 kg")
                .get(0).name);
        assertEquals("Román felhúzás",
                StrengthParse.parse("román holt emelés 3x10 80 kg").get(0).name);
        // Az egybeírt alak változatlanul működik.
        assertEquals("Felhúzás", StrengthParse.parse("holtemelés 3x5 150 kg")
                .get(0).name);
    }

    /**
     * A „megvan a 100 kg-os guggolás" is csúcs-mondat.
     *
     * A csúcs-alakok közül a legrövidebb magyar forma hiányzott, és épp ez
     * az a bejegyzés, amit az ember a legjobban szeretne látni a naplóban.
     */
    @Test public void theShortestRecordFormIsUnderstood() {
        List<StrengthParse.Item> it = StrengthParse.parse("megvan a 100 kg-os guggolás, végre");
        assertEquals(1, it.size());
        assertEquals("Guggolás", it.get(0).name);
        assertEquals(100, it.get(0).sets.get(0).weight, 0.01);
        assertEquals("Felhúzás", StrengthParse.parse("meglett a 140 kg-os felhúzás")
                .get(0).name);
    }

    /**
     * A gondolatjeles felsorolás utolsó tétele sem veszhet el.
     *
     * A sorhatár a normalizálás után eltűnik („- 10 fekvőtámasz / - 20
     * guggolás / - 30 mp plank" egyetlen sorrá olvad), és a plank elveszett
     * vele. A jel helyére vessző kerül: onnantól ugyanaz, mint a vesszős
     * felsorolás.
     */
    @Test public void aBulletListKeepsItsLastItem() {
        List<StrengthParse.Item> it = StrengthParse.parse(
                "- 10 fekvőtámasz\n- 20 guggolás\n- 30 mp plank");
        assertEquals(3, it.size());
        assertEquals("Plank", it.get(2).name);
        // A csillag NEM felsorolás-jel, a szám előtti gondolatjel sem.
        assertEquals(3, StrengthParse.parse("fekvenyomás 3 * 10 60 kg").get(0).sets.size());
    }

    /**
     * A méter nem ismétlésszám.
     *
     * Az erőgépek között ott a „pillangó", ami vizes szó is: a „pillangózás
     * 200 m" kétszáz ismétléses MELLGÉP lett a naplóban. Aki métert ír, az
     * távot mond.
     */
    @Test public void metersAreNeverReps() {
        assertTrue(StrengthParse.parse("pillangózás 200 m").isEmpty());
        assertTrue(StrengthParse.parse("farmerséta 40 m").isEmpty());
        // A kilogramm viszont marad: a szám után ott nem m betű áll.
        assertEquals(60.0, StrengthParse.parse("fekvenyomás 3x10, 60 kg")
                .get(0).topWeight(), 0.01);
    }

    /**
     * A mérőszalag nem edzés.
     *
     * A „combom 58 cm, vádli 38" egy testkörfogat-mérés – eddig harmincnyolc
     * ismétléses VÁDLIEMELÉS lett belőle, mert a vádli gyakorlatnév is.
     */
    @Test public void aTapeMeasureIsNotAWorkout() {
        assertTrue(StrengthParse.parse("combom 58 cm, vádli 38").isEmpty());
        // A valódi vádliemelés viszont marad.
        assertEquals("Vádliemelés", StrengthParse.parse("vádliemelés 4x15")
                .get(0).name);
    }

    /** A gép neve a teremben „lábtoló", nem „lábtolás". */
    @Test public void theLegPressIsCalledByItsGymName() {
        List<StrengthParse.Item> it = StrengthParse.parse("kondi 70 perc: "
                + "mellnyomás 4x8 70, húzódzkodás 4x6, lábtoló 3x12 120");
        assertEquals(3, it.size());
        assertEquals("Lábtolás", it.get(2).name);
        assertEquals(120.0, it.get(2).topWeight(), 0.01);
    }

    /**
     * A francia fekvenyomás tricepsz, nem fekvenyomás.
     *
     * A rövidebb „fekvenyom" tő eddig elvitte, és a huszonöt kilós francia a
     * FEKVENYOMÁS rekordjai közé került – ott pedig a progresszió-javaslat is
     * ebből számol tovább. A tolónyomás és a kábelhúzás egyszerűen hiányzott.
     */
    @Test public void theFrenchPressIsATricepsExercise() {
        assertEquals("Tricepsz", StrengthParse.parse("franciafekvenyomás "
                + "3x12 25 kg").get(0).name);
        assertEquals("Fekvenyomás", StrengthParse.parse("fekvenyomás 3x8 80 kg")
                .get(0).name);
        List<StrengthParse.Item> it = StrengthParse.parse("tolónyomás 3x8 40 kg, "
                + "oldalemelés 3x12 8 kg");
        assertEquals(2, it.size());
        assertEquals("Vállból nyomás", it.get(0).name);
        assertEquals("Lehúzás", StrengthParse.parse("kábelhúzás 3x15").get(0).name);
    }

    /**
     * A két kézisúlyzó nem két sorozat.
     *
     * A „2x15 kg kézisúlyzóval vállnyomás 3x10" első szorzata a FELSZERELÉS:
     * két darab tizenöt kilós súlyzó. Eddig ez lett a sorozat (2×1, 15 kg),
     * a valódi 3×10 meg elveszett.
     */
    @Test public void aPairOfDumbbellsIsNotTwoSets() {
        List<StrengthParse.Item> it = StrengthParse.parse("súlyzózás otthon: "
                + "2x15 kg kézisúlyzóval vállnyomás 3x10");
        assertEquals(1, it.size());
        assertEquals(3, it.get(0).sets.size());
        assertEquals(15.0, it.get(0).topWeight(), 0.01);
        // A valódi 2x15-ös sorozat súlyzó-szó nélkül marad sorozat.
        assertEquals(2, StrengthParse.parse("guggolás 2x15 60 kg")
                .get(0).sets.size());
    }

    /**
     * A jelzős súly is átjön a névvel a következő tagmondatba.
     *
     * A „húsz kilós kettlebell swing, 4x15" súlya az első tagmondatban
     * áll, a sorozat a másodikban – a név mellől eddig elveszett a húsz
     * kiló, és saját testsúlyos lendítés lett belőle. A tagmondat saját
     * súlya erősebb: a „guggolás 60 kg bemelegítés, aztán 3x5 100"
     * munkasorozata száz kilós marad.
     */
    @Test public void anAdjectiveWeightTravelsWithTheName() {
        List<StrengthParse.Item> it = StrengthParse.parse("húsz kilós "
                + "kettlebell swing, 4x15");
        assertEquals(1, it.size());
        assertEquals(20.0, it.get(0).topWeight(), 0.01);
        assertEquals(100.0, StrengthParse.parse("guggolás 60 kg bemelegítés, "
                + "aztán 3x5 100").get(0).topWeight(), 0.01);
    }

    /**
     * A pillangó az uszodában úszásnem, a teremben mellgép.
     *
     * A „pillangó technikát gyakoroltam, 4x50" négyszer ötven ismétléses
     * mellgép lett – egy úszóedzésből. A „pillangó gép" a teremben marad.
     */
    @Test public void butterflyStrokeIsNotAPecDeck() {
        assertTrue(StrengthParse.parse("pillangó technikát gyakoroltam, "
                + "4x50").isEmpty());
        assertEquals("Mellgép", StrengthParse.parse("pillangó gép 3x12 "
                + "40 kg").get(0).name);
    }

    /** Az angol „biceps curl" z nélkül is bicepsz. */
    @Test public void englishBicepsCurlIsRecognised() {
        List<StrengthParse.Item> it = StrengthParse.parse("biceps curl "
                + "12,5 kg-os kézisúlyzóval 3x12");
        assertEquals(1, it.size());
        assertEquals("Bicepsz", it.get(0).name);
        assertEquals(12.5, it.get(0).topWeight(), 0.01);
    }
    /**
     * A kiírt darabszámú súlyzópár súlya a jelzős szám.
     *
     * A „két 12,5-ös kézisúlyzóval" kettese darab (két súlyzó), a súly a
     * tizenkét és fél – mégis két kilós vállnyomás lett belőle. A számnév-
     * fordítás előtt futó álarcnak a „két" szót is ismernie kell.
     */
    @Test public void aSpelledOutDumbbellPairKeepsItsWeight() {
        StrengthParse.Item it = StrengthParse.parse(
                "vállból nyomás 4x10 két 12,5-ös kézisúlyzóval").get(0);
        assertEquals(12.5, it.topWeight(), 0.01);
        assertEquals(4, it.sets.size());
        assertEquals(10, it.sets.get(0).reps);
        // Az „egy 24-es kettlebell" súlya is a jelzős szám.
        assertEquals(24.0, StrengthParse.parse(
                "goblet guggolás egy 24-es kettlebell-lel 3x12").get(0)
                .topWeight(), 0.01);
    }

    /**
     * A centis bicepsz mérőszalag, nem gyakorlat.
     *
     * A „bicepszem 38 cm lett" harmincnyolc ismétléses bicepszgyakorlatként
     * is bekerült a mérés mellé – a testrész-lista nem ismerte a bicepszt.
     */
    @Test public void aBicepsMeasurementIsNotAWorkout() {
        assertTrue(StrengthParse.parse("bicepszem 38 cm lett").isEmpty());
        // A valódi bicepszgyakorlat marad.
        assertEquals(12, StrengthParse.parse(
                "bicepsz curl 3x12 a 12,5 kg-os súlyzóval")
                .get(0).sets.get(0).reps);
    }
    /**
     * Az üres rúd húsz kiló, a súlyemelés-jegyzet pedig nem ismétlésszám.
     *
     * A „szakítás technika üres rúddal 6x3" saját testsúlyosnak számított,
     * pedig a szabvány rúd húsz kiló. Az „emeltem a guggolás súlyát
     * 5 kilóval, most 85" nyolcvanöte pedig NYOLCVANÖT ISMÉTLÉS lett –
     * inkább ne kerüljön be sorozatként, mint így.
     */
    @Test public void anEmptyBarWeighsTwentyKilos() {
        StrengthParse.Item it = StrengthParse.parse(
                "szakítás technika üres rúddal 6x3").get(0);
        assertEquals(20.0, it.topWeight(), 0.01);
        assertEquals(6, it.sets.size());
        assertTrue(StrengthParse.parse("emeltem a guggolás súlyát "
                + "5 kilóval, most 85").isEmpty());
    }
    /**
     * A darabszám egy sorozat, a tricepsznyújtás nem jóga.
     *
     * Az „AMRAP fekvőtámasz: 42 db egy sorozatban" negyvenkettője
     * elveszett (a db-hez nem tartozott sorozat-jelölés, az „egy" pedig
     * súlynak látszott volna); a „tricepsznyújtás" nyújt-töve mellé pedig
     * egy 45 perces jóga került.
     */
    @Test public void aPieceCountIsASingleSet() {
        StrengthParse.Item it = StrengthParse.parse(
                "AMRAP fekvőtámasz: 42 db egy sorozatban").get(0);
        assertEquals(42, it.totalReps());
        assertEquals(0.0, it.topWeight(), 0.01);
        assertEquals(0, Activities.parse("rest-pause tricepsznyújtás "
                + "15+5+5").plans.size());
    }
    @Test public void aCompletedHeavySingleIsLogged() {
        // A „megcsináltam a 100 kilós fekvenyomást" teljesített egyes –
        // eddig üresen jött vissza, ahogy a „kihúztam 100 kilót" is.
        java.util.List<StrengthParse.Item> it =
                StrengthParse.parse("megcsináltam a 100 kilós fekvenyomást");
        assertEquals(1, it.size());
        assertEquals("Fekvenyomás", it.get(0).name);
        assertEquals(100, it.get(0).topWeight(), 0.001);
        it = StrengthParse.parse("kihúztam 100 kilót a földről");
        assertEquals("Felhúzás", it.get(0).name);
        assertEquals(100, it.get(0).topWeight(), 0.001);
        // A „kihúztam a hetet" nem felhúzás.
        assertTrue(StrengthParse.parse("kihúztam a hetet valahogy").isEmpty());
    }

    @Test public void theSingleGTypoStillSquats() {
        // Az egy g-s „gugolás" gyakori elírás – eddig semmi nem lett belőle.
        java.util.List<StrengthParse.Item> it =
                StrengthParse.parse("gugolás 3x8 a rúdon 60 kilóval");
        assertEquals("Guggolás", it.get(0).name);
        assertEquals(60, it.get(0).topWeight(), 0.001);
        assertEquals(3, it.get(0).sets.size());
    }

    @Test public void poundsBecomeKilograms() {
        // A „bench press 3x5 225 lbs" kétszázhuszonöt KILÓ lett a naplóban
        // – az angol font negyedannyi.
        java.util.List<StrengthParse.Item> it =
                StrengthParse.parse("bench press 3x5 225 lbs");
        assertEquals(102.1, it.get(0).topWeight(), 0.1);
    }

    @Test public void hungarianMachineNamesResolve() {
        // A letolás, a tarkónyomás és a csípőtolás termi nevek – eddig
        // egyik sem lett sorozat. A csigás letolás pedig nem kakaós csiga.
        assertEquals("Tricepsz",
                StrengthParse.parse("letolás csigán 3x12 25 kg").get(0).name);
        assertEquals("Vállból nyomás",
                StrengthParse.parse("tarkónyomás 4x8 40 kg").get(0).name);
        assertEquals("Csípőemelés",
                StrengthParse.parse("csípőtolás 3x10 100 kg").get(0).name);
    }

    @Test public void aChocolateCakeIsNotALunge() {
        // Az „egy nagy szelet csokitorta" belsejében ott a KITÖRés töve –
        // lunge-sorozat lett a süteményből. Az igazi kitörés marad.
        assertTrue(StrengthParse.parse("egy nagy szelet csokitorta").isEmpty());
        assertEquals("Kitörés",
                StrengthParse.parse("kitöréseket csináltam 3x10").get(0).name);
    }

    @Test public void aTimedRowingExportIsNotARepCount() {
        assertTrue(StrengthParse.parse("rowing 20 min").isEmpty());
    }

    @Test public void restMinutesDoNotBreakTheSets() {
        List<StrengthParse.Item> it = StrengthParse.parse(
                "3x10 fekvenyom\u00e1s 2 perc pihen\u0151vel 60 kg");
        assertEquals(1, it.size());
        assertEquals(3, it.get(0).sets.size());
        assertEquals(60.0, it.get(0).topWeight(), 0.01);
    }

    @Test public void aConditionalWishIsNotARecord() {
        assertTrue(StrengthParse.parse(
                "b\u00e1r tudn\u00e9k 100 kg-ot nyomni fekve").isEmpty());
    }

    @Test public void aPbHeadlineIsAOneRepRecord() {
        List<StrengthParse.Item> it = StrengthParse.parse("pb: 120 kg felh\u00faz\u00e1s");
        assertEquals(1, it.size());
        assertEquals(120.0, it.get(0).topWeight(), 0.01);
    }

    @Test public void pekDeckAndTheFrenchPressAreTheirOwnMoves() {
        assertEquals("Mellg\u00e9p", StrengthParse.parse("pek deck 3x12 40 kg")
                .get(0).name);
        assertEquals("Tricepsz", StrengthParse.parse("franciafekv\u00e9s 3x12 25 kg")
                .get(0).name);
        assertEquals("Good morning", StrengthParse.parse("goodmorning 3x10 40 kg")
                .get(0).name);
    }

    @Test public void aLyingLegRaiseIsNotABenchPress() {
        assertEquals("L\u00e1bemel\u00e9s",
                StrengthParse.parse("l\u00e1bemel\u00e9s fekve 3x15").get(0).name);
        assertEquals("Fekvenyom\u00e1s",
                StrengthParse.parse("fekvenyom\u00e1s 3x10 60 kg").get(0).name);
    }

    @Test public void theDayHeaderTellsWhichPressItIs() {
        List<StrengthParse.Item> it = StrengthParse.parse(
                "v\u00e1ll: nyom\u00e1s 3x8 40, oldalemel\u00e9s 3x15 8");
        assertEquals(2, it.size());
        assertEquals("V\u00e1llb\u00f3l nyom\u00e1s", it.get(0).name);
        assertEquals(40.0, it.get(0).topWeight(), 0.01);
        assertEquals("Fekvenyom\u00e1s",
                StrengthParse.parse("mell nap: nyom\u00e1s 3x8 60").get(0).name);
    }

}
