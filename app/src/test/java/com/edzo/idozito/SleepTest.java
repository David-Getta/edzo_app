package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Alvás a mondatból.
 *
 * A regeneráció az edzés másik fele – de a kilogrammhoz hasonlóan az óra is
 * túlterhelt szó: a „8 óra kondi" edzés, a „8 órakor keltem" időpont. Ezért
 * kimondott alvás-szó kell, és életszerű sáv.
 */
public class SleepTest {

    @Test public void spokenSleepIsUnderstood() {
        assertEquals(8, Sleep.parse("aludtam 8 órát"), 0.01);
        assertEquals(7.5, Sleep.parse("aludtam 7,5 órát"), 0.01);
        assertEquals(7, Sleep.parse("7 óra alvás"), 0.01);
        assertEquals(6, Sleep.parse("ma éjjel csak 6 órát aludtam... alvás: 6"), 0.01);
        assertEquals(8, Sleep.parse("alvás: 8"), 0.01);
        // Kiírt számnév és a „és fél" is.
        assertEquals(8, Sleep.parse("nyolc órát aludtam... aludtam nyolc órát"), 0.01);
        assertEquals(7.5, Sleep.parse("aludtam hét és fél órát"), 0.01);
    }

    /**
     * A szám ELÖL is állhat: „7,5 órát aludtam".
     *
     * Ez a leggyakoribb magyar szórend, és a felismerő pont ezt nem értette –
     * csak az ige mögötti számot. Aki így írta le az éjszakáját, semmit nem
     * kapott vissza.
     */
    @Test public void theNumberMayComeFirst() {
        assertEquals(7.5, Sleep.parse("7,5 órát aludtam"), 0.01);
        assertEquals(4, Sleep.parse("csak 4 órát aludtam"), 0.01);
        assertEquals(6, Sleep.parse("tegnap 6 órát aludtam összesen"), 0.01);
        assertEquals(9, Sleep.parse("kilenc órát aludtam"), 0.01);
        assertEquals(7.5, Sleep.parse("hét és fél órát aludtam"), 0.01);
        // A másik igét nem húzza magához: a munka nem alvás.
        assertEquals(6, Sleep.parse("8 órát dolgoztam, 6 órát aludtam"), 0.01);
    }

    /**
     * Óra ÉS perc, illetve tól-ig.
     *
     * A „6 óra 30 perc alvás" fél órája eddig nemhogy elveszett: az egész
     * mondat kiesett. A „8-9 órát aludtam" párja pedig úgy nézett ki, mint egy
     * munka/pihenő ritmus, és az időzítőt állította be helyette.
     */
    @Test public void hoursWithMinutesAndRanges() {
        assertEquals(6.5, Sleep.parse("6 óra 30 perc alvás"), 0.01);
        assertEquals(7.8, Sleep.parse("aludtam 7 órát 45 percet"), 0.01);
        assertEquals(8.5, Sleep.parse("8-9 órát aludtam"), 0.01);
        assertEquals(Sentence.Kind.SLEEP,
                Sentence.of("8-9 órát aludtam", null, 1_753_869_600_000L));
        // Az időzítő-mondat érintetlen: nincs benne alvás-szó.
        assertEquals(Sentence.Kind.INTERVAL,
                Sentence.of("8 kör 40 mp munka 20 mp pihenő", null, 1_753_869_600_000L));
    }

    @Test public void notEverySentenceWithHoursIsSleep() {
        assertEquals(-1, Sleep.parse("8 óra kondi"), 0.01);
        assertEquals(-1, Sleep.parse("8 órakor keltem"), 0.01);
        assertEquals(-1, Sleep.parse("dolgoztam 8 órát"), 0.01);
        assertEquals(-1, Sleep.parse("78 kg vagyok"), 0.01);
        assertEquals(-1, Sleep.parse(null), 0.01);
        // Életszerűtlen érték nem éjszaka.
        assertEquals(-1, Sleep.parse("aludtam 20 órát"), 0.01);
        assertEquals(-1, Sleep.parse("aludtam 1 órát"), 0.01);
    }

    @Test public void verdictBandsAreSensible() {
        assertTrue(Sleep.verdict(5).contains("kevés"));
        assertTrue(Sleep.verdict(6.5).contains("hét óra alatt"));
        assertTrue(Sleep.verdict(8).contains("rendben"));
        assertTrue(Sleep.verdict(10).contains("hosszú"));
        assertEquals("", Sleep.verdict(-1));
    }

    /** Az útbaigazító a Profilba küldi – de csak a kimondott alvást. */
    @Test public void routedToTheProfile() {
        assertEquals(Sentence.Kind.SLEEP,
                Sentence.of("aludtam 8 órát", null, 1_753_869_600_000L));
        assertEquals(Sentence.Kind.WORKOUT,
                Sentence.of("8 óra kondi", null, 1_753_869_600_000L));
    }

    /**
     * A töltelékszó belefér, a feltételes mód nem.
     *
     * Az „aludtam kb 6,5 órát" hétköznapi mondat; az „aludtam volna nyolc
     * órát" viszont egy rossz éjszaka panasza – abból bejegyzést csinálni
     * pont a fordítottját rögzítené annak, ami történt.
     */
    @Test public void fillerWordsYesConditionalNo() {
        assertEquals(6.5, Sleep.parse("aludtam kb 6,5 órát"), 0.01);
        assertEquals(7, Sleep.parse("aludtam vagy 7 órát"), 0.01);
        assertEquals(6.5, Sleep.parse("jól aludtam, kb 6 és fél órát"), 0.01);
        assertEquals(-1, Sleep.parse("aludtam volna nyolc órát"), 0.01);
        assertEquals(-1, Sleep.parse("bárcsak aludtam volna 9 órát"), 0.01);
    }

    /**
     * Lefekvés és ébredés: a kivonást ne a felhasználó végezze el.
     *
     * Sokan nem a hosszat írják le, hanem két időpontot – az óra is így
     * méri. Eddig ezekből semmi nem lett: „este 11-kor feküdtem, reggel
     * 7-kor keltem" ugyanolyan üres válasz volt, mint egy értelmetlen szöveg.
     */
    @Test public void twoClockTimesBecomeALength() {
        assertEquals(8.0, Sleep.parse("este 11-kor feküdtem, reggel 7-kor keltem"), 0.01);
        assertEquals(7.8, Sleep.parse("22:30-tól 6:15-ig aludtam"), 0.05);
        assertEquals(7.5, Sleep.parse("23:00-kor aludtam el, 6:30-kor ébredtem"), 0.01);
        assertEquals(8.0, Sleep.parse("lefeküdtem 23 órakor, felkeltem 7 órakor"), 0.01);
        assertEquals(8.0, Sleep.parse("este 10 és reggel 6 között aludtam"), 0.01);
        // A magyar „fél tizenegy" tíz harminc – és este értendő.
        assertEquals(7.5, Sleep.parse("fél 11-kor feküdtem le és 6-kor keltem"), 0.01);
        // Az óra-app kiírása hossz, nem időpont: a perc sem veszhet el.
        assertEquals(6.5, Sleep.parse("alvás 6:30"), 0.01);
        // A lefekvést nem csak „feküdtem"-mel mondjuk – az „ágyban voltam" és
        // az „ágyba bújtam" ugyanaz a pillanat.
        assertEquals(8.0, Sleep.parse("este 10-re ágyban voltam, reggel 6-kor keltem"), 0.01);
        assertEquals(9.0, Sleep.parse("ágyba bújtam 22-kor, 7-kor keltem"), 0.01);
        // Az ébredés állhat elöl is – ugyanaz az éjszaka, fordított sorrendben
        // elmesélve. Eddig a különbség tizenhat óra lett, és a tizenkét órás
        // igazítás négy és negyed órányi alvást hazudott rá.
        assertEquals(7.8, Sleep.parse("ma reggel 6:30-kor keltem, 22:45-kor feküdtem le"), 0.05);
        assertEquals(7.0, Sleep.parse("6-kor ébredtem, előtte 23-kor feküdtem le"), 0.01);
        // A kettősponttal kiírt óra huszonnégy órás adat: azt nem toljuk el
        // tizenkettővel, hogy „életszerűbb" hossz jöjjön ki belőle.
        assertEquals(8.0, Sleep.parse("23:00-kor feküdtem le, 7:00-kor keltem"), 0.01);
        // Alvás-szó nélkül nincs bejegyzés: az edzés-időpont nem éjszaka.
        assertEquals(-1, Sleep.parse("18:00-tól 19:30-ig kondi"), 0.01);
        assertEquals(-1, Sleep.parse("edzés 6-kor és 18-kor"), 0.01);
    }

    /**
     * Az óra-szó elhagyható: „nyolcat aludtam".
     *
     * A magyar magától értetődőnek veszi, hogy órákról van szó, és el is
     * hagyja a szót. A szám és az ige közé viszont csak a tárgyrag férhet be,
     * így a mondat többi száma nem eshet ide.
     */
    /**
     * A SZORZÓ nem óra: az ébredések száma nem az alvás hossza.
     *
     * A „7,5 órát aludtam, de 3-szor felébredtem" mondatból HÁROM óra alvás
     * lett – a legelső minta elvitte a mondatot a valódi hossz elől.
     */
    @Test public void theWakeUpCountIsNotTheLength() {
        assertEquals(7.5, Sleep.parse("reggel 7,5 órát aludtam, de 3-szor felébredtem"), 0.01);
        assertEquals(7.0, Sleep.parse("aludtam 7 órát, kétszer felébredtem"), 0.01);
        // A szám és az ige közé beférő rövid szavak változatlanok.
        assertEquals(6.5, Sleep.parse("aludtam kb 6,5 órát"), 0.01);
        assertEquals(8.0, Sleep.parse("aludtam 8 órát"), 0.01);
    }

    @Test public void theHourWordMayBeLeftOut() {
        assertEquals(8.0, Sleep.parse("nyolcat aludtam"), 0.01);
        assertEquals(8.0, Sleep.parse("kb 8-at aludtam"), 0.01);
        assertEquals(7.0, Sleep.parse("hetet aludtam"), 0.01);
        assertEquals(6.5, Sleep.parse("6,5-öt aludtam"), 0.01);
        // Az életszerűtlen érték itt sem megy át, és a feltételes mód sem.
        assertEquals(-1, Sleep.parse("20-at aludtam"), 0.01);
        assertEquals(-1, Sleep.parse("aludtam volna nyolcat"), 0.01);
    }

    /**
     * A NAPSZAK nem hossz.
     *
     * A „ma reggel 7:15-kor keltem" hét óra tizenöt perckor, nem
     * négyszázharmincöt másodperc munka – ebből eddig időzítő-terv lett, a
     * felkelés órájából.
     */
    @Test public void aClockTimeWithACaseSuffixIsNotAPlan() {
        assertNull(IntervalParse.parse("ma reggel 7:15-kor keltem"));
        assertNull(IntervalParse.parse("este 23:40-kor feküdtem le"));
        // A valódi ritmus és a verseny-idő változatlan.
        assertEquals(6, IntervalParse.parse("1:30 munka 0:30 pihenő 6 kör").rounds);
        assertEquals(7.75, Sleep.parse("22:30-tól 6:15-ig aludtam"), 0.05);
    }

    /**
     * A kimondott hossz erősebb a két időpontnál.
     *
     * A „ma reggel 5 km, délután 40 perc kondi, este 8 óra alvás" mondatban
     * a „reggel 5" és az „este 8" időpont-párnak látszott, és három óra
     * alvás került a naplóba a nyolc helyett – egy olyan mondatból, amelyik
     * kimondja a nyolcat.
     */
    @Test public void theStatedLengthBeatsTwoClockTimes() {
        assertEquals(8.0, Sleep.parse(
                "ma reggel 5 km, délután 40 perc kondi, este 8 óra alvás"), 0.01);
        assertEquals(8.0, Sleep.parse("8 óra alvás"), 0.01);
        // A két időpont változatlanul működik, ahol nincs kimondott hossz.
        assertEquals(8.0, Sleep.parse("este 11-kor feküdtem, reggel 7-kor keltem"), 0.01);
        assertEquals(7.8, Sleep.parse("22:30-tól 6:15-ig aludtam"), 0.05);
    }

    /**
     * A helyesbítés második száma az igazi.
     *
     * A magyar így javít: kimondja, ami nem igaz, aztán azt, ami igen. A
     * „nem aludtam 8 órát, csak 5-öt" mondatból NYOLC óra alvás került a
     * naplóba – vagyis pont az, amit a mondat tagad.
     */
    @Test public void theCorrectedNumberWins() {
        assertEquals(5.0, Sleep.parse("nem aludtam 8 órát, csak 5-öt"), 0.01);
        assertEquals(6.0, Sleep.parse("nem 9 órát aludtam, hanem 6-ot"), 0.01);
        // A javítás nélküli mondat változatlan.
        assertEquals(8.0, Sleep.parse("aludtam 8 órát"), 0.01);
        assertEquals(-1, Sleep.parse("nem aludtam eleget"), 0.01);
    }

    /**
     * Három hétköznapi alak, ami eddig üres választ kapott.
     *
     * A „10-től 6-ig aludtam" tól-ig párját rag jelöli, nem a „-kor". Az
     * „aludtam 7h30" a sportórák írásmódja, és a perce elveszett. Az
     * „összesen talán 5 órát" pedig a harmadik tagmondatban áll – az ige
     * mellől szándékosan nem vesszük el a számot, mert az az ébredések
     * száma lenne.
     */
    @Test public void threeEverydayFormsAreUnderstood() {
        assertEquals(8.0, Sleep.parse("10-től 6-ig aludtam"), 0.01);
        assertEquals(7.5, Sleep.parse("aludtam 7h30"), 0.01);
        assertEquals(5.0, Sleep.parse(
                "rosszul aludtam, 3-szor felébredtem, összesen talán 5 órát"), 0.01);
        // Az ébredésszám továbbra sem alváshossz.
        assertEquals(7.5, Sleep.parse("reggel 7,5 órát aludtam, de 3-szor felébredtem"), 0.01);
        // Alvás-szó nélkül nincs bejegyzés.
        assertEquals(-1, Sleep.parse("18:00-tól 19:30-ig kondi"), 0.01);
    }

    /**
     * A hossz a következő tagmondatban is állhat.
     *
     * A „rosszul aludtam, kb 5 órát" a legtermészetesebb panasz-mondat, és
     * eddig SEMMI nem lett belőle: az ige melletti szám elől a vessző
     * szándékos határ (ott az ébredések száma állna), csak épp az óra-szó
     * megkülönbözteti a kettőt.
     */
    @Test public void theLengthMayFollowTheComma() {
        assertEquals(5.0, Sleep.parse("rosszul aludtam, kb 5 órát, "
                + "kétszer felébredtem"), 0.01);
        // Az ébredések száma továbbra sem alvásóra.
        assertEquals(7.5, Sleep.parse("7,5 órát aludtam, de 3-szor "
                + "felébredtem"), 0.01);
        // A „3 óra múlva" időpont, nem hossz.
        assertEquals(-1.0, Sleep.parse("aludtam, de 3 óra múlva felébredtem"), 0.01);
    }

    /**
     * Az óra-app kijelzőjéről másolt sor is alvás.
     *
     * A „22:15 lefekvés, 5:45 ébredés" FŐNÉVI alakban mondja ugyanazt, amit a
     * „feküdtem/keltem" igében – és ige nélkül eddig teljesen elveszett. Az
     * éjfél is időpont, csak nem számmal írják; a „felébredtem" pedig maga is
     * alvás-szó, így az összegző tagmondat hossza is megvan.
     */
    @Test public void theNounFormsAndMidnightWork() {
        assertEquals(7.5, Sleep.parse("22:15 lefekvés, 5:45 ébredés"), 0.01);
        assertEquals(6.0, Sleep.parse("éjfél után feküdtem, 6-kor keltem"), 0.01);
        assertEquals(5.0, Sleep.parse("éjszaka 3x felébredtem a gyerek miatt, "
                + "összesen talán 5 óra"), 0.01);
    }

    /** Az átlag nem egy éjszaka: a heti összefoglaló nem kerül a trendbe. */
    @Test public void aWeeklyAverageIsNotOneNight() {
        assertEquals(-1.0, Sleep.parse("az alvásátlagom 6,8 óra a héten"), 0.01);
        // Az átlagpulzus melletti valódi alvás marad.
        assertEquals(7.0, Sleep.parse("7 óra alvás, átlagpulzus 62"), 0.01);
    }

    /**
     * A puszta „ágyban" és a „kelés" főnév is időpont-pár.
     *
     * A „11-kor ágyban, fél 7-kor kelés" eddig elveszett: az „ágyban"
     * mögül hiányzott a „voltam", a „kelés" pedig nem volt ébredés-szó.
     * Egy időponttal a pár nem áll össze: az esti olvasás az ágyban nem
     * alvás-hossz.
     */
    @Test public void bareInBedAndTheWakingNounPairUp() {
        assertEquals(7.5, Sleep.parse("11-kor ágyban, fél 7-kor kelés, de "
                + "forgolódtam sokat"), 0.01);
        assertEquals(-1.0, Sleep.parse("az ágyban olvastam este, 11-kor "
                + "keltem"), 0.01);
    }

    /**
     * A „húztam" szleng is alvás – de csak alvás-szó mellett.
     *
     * A „bepótoltam az alvást, 10 órát húztam" tíz órája eddig elveszett.
     * Alvás-szó nélkül a „2 órát húztam a teremben" súlyzózás marad.
     */
    @Test public void pullingTenHoursIsSleepSlang() {
        assertEquals(10.0, Sleep.parse("hétvégén bepótoltam az alvást, "
                + "10 órát húztam"), 0.01);
        assertEquals(-1.0, Sleep.parse("2 órát húztam a teremben"), 0.01);
    }
    /**
     * A jelzős alak is alvás: a „8 órás alvás" ugyanaz, mint a „8 óra alvás".
     *
     * Az -s képző miatt a minta nem illeszkedett, és az éjszaka némán
     * elveszett. A két óránál rövidebb szundi viszont szándékosan nem kerül
     * be: a napi egy alvásérték miatt felülírná az éjszakát.
     */
    @Test public void anAdjectivalSleepHourStillCounts() {
        assertEquals(8.0, Sleep.parse("8 órás alvás után frissen keltem"), 0.01);
        assertEquals(-1.0, Sleep.parse("másfél órás délutáni alvás"), 0.01);
    }
    /**
     * Az óra+perc alvás a címke mellett is pontos.
     *
     * Az „összefoglaló: 7 óra 12 perc alvás" hét egész kettő tized – az
     * össze-tő „összesen"-szabálya elkapta a hetest, és a tizenkét perc
     * elveszett. Az igazi összesen-mondat marad.
     */
    @Test public void aSummaryLabelDoesNotEatTheMinutes() {
        assertEquals(7.2, Sleep.parse("összefoglaló: 7 óra 12 perc alvás, "
                + "62 nyugalmi"), 0.01);
        assertEquals(5.0, Sleep.parse("éjszaka 3x felébredtem, összesen "
                + "talán 5 órát aludtam"), 0.01);
    }
    /**
     * Az éjfél utáni pontos óra és a fordított alvásidő is működik.
     *
     * Az „éjfél után 1-kor feküdtem, 7-kor keltem" éjfele kettőt csinált
     * egy időpontból, és a 0:00→1:00 egyórás éjszaka kiejtette az
     * egészet. A „7:02 alvásidő" óra-app-sor pedig fordított szórendje
     * miatt veszett el.
     */
    @Test public void midnightPlusAnHourStillMakesANight() {
        assertEquals(6.0, Sleep.parse("éjfél után 1-kor feküdtem, "
                + "7-kor keltem"), 0.01);
        assertEquals(6.0, Sleep.parse("éjfél után feküdtem, 6-kor "
                + "keltem"), 0.01);
        assertEquals(7.0, Sleep.parse("82%-os alvásminőség, 7:02 "
                + "alvásidő"), 0.01);
    }
    @Test public void suffixedHourWordsAreClockTimes() {
        // Az „este tíztől reggel hatig aludtam" nyolc óra – a ragos
        // óra-számnevet a számnév-fordítás eddig nem ismerte fel.
        assertEquals(8, Sleep.parse("este tíztől reggel hatig aludtam"), 0.01);
        // A naptári hét viszont marad: a „két hétig rosszul aludtam"
        // nem héttől valameddig tartó éjszaka.
        assertEquals(-1, Sleep.parse("két hétig rosszul aludtam"), 0.01);
        assertEquals(8, Sleep.parse("egy hétig alig aludtam, ma végre 8 órát"), 0.01);
    }

    @Test public void theAlarmClockRingingIsWakingUp() {
        // A „tizenegy óra körül alhattam el, hatkor csörgött az óra" hét
        // óra alvás – a csörgő óra az ébredés pillanata.
        assertEquals(7, Sleep.parse(
                "tizenegy óra körül alhattam el, hatkor csörgött az óra"), 0.01);
        assertEquals(6, Sleep.parse(
                "csak hajnali 2-kor kerültem ágyba, 8-kor keltem"), 0.01);
    }

    @Test public void repeatedWakingsStillSumTheNight() {
        // „a gyerek miatt háromszor keltem fel, összesen 6 óra lett" –
        // a „fel" a számnév-fordítás után 0,5, mégis éjszakáról szól.
        assertEquals(6, Sleep.parse(
                "a gyerek miatt háromszor keltem fel, összesen 6 óra lett"), 0.01);
    }

    @Test public void englishWatchSleepLinesParse() {
        // Az óra angol exportja: a „sleep score 78, 7h12m" hét óra
        // tizenkét perc, a „sleep 6:45" hat és háromnegyed – eddig
        // egyik sem került be. A pontszám nem alvásóra.
        assertEquals(7.2, Sleep.parse("sleep score 78, 7h12m"), 0.01);
        assertEquals(6.8, Sleep.parse("sleep 6:45"), 0.01);
        assertEquals(-1, Sleep.parse("alvás pontszám 85"), 0.01);
    }

    @Test public void aPhoneticTypoStillSleeps() {
        // Az „aluttam 8 órát" gyakori fonetikus elütés – eddig elveszett.
        assertEquals(8, Sleep.parse("aluttam 8 órát"), 0.01);
    }

    @Test public void shiftWorkersDaytimeSleepCounts() {
        // Az „éjjeli műszakból jöttem, délben feküdtem és 19-kor keltem"
        // hét óra nappali alvás; a „csak 4 órát tudtam aludni" négy.
        assertEquals(7, Sleep.parse(
                "éjjeli műszakból jöttem, délben feküdtem és 19-kor keltem"), 0.01);
        assertEquals(4, Sleep.parse(
                "két műszak között csak 4 órát tudtam aludni"), 0.01);
    }

    @Test public void deepSleepPhaseDoesNotOverwriteTheTotal() {
        // A „mélyalvás 2 óra 10 perc, összesen 7 óra 30 perc alvás" két
        // óra tízként ment be – a fázis nem a teljes éjszaka.
        assertEquals(7.5, Sleep.parse(
                "mélyalvás 2 óra 10 perc, összesen 7 óra 30 perc alvás"), 0.01);
        assertEquals(7.8, Sleep.parse("alvás 7:45, ebből mély 2:05"), 0.01);
    }

    @Test public void hoursAndMinutesJoinedByEsAddUp() {
        assertEquals(6.8, Sleep.parse("aludtam 6 \u00f3r\u00e1t \u00e9s 45 percet"), 0.01);
    }

    @Test public void stayingUpUntilTwoIsFallingAsleepAtTwo() {
        assertEquals(7.0, Sleep.parse("hajnali 2-ig fent voltam, 9-kor keltem"), 0.01);
    }

    @Test public void approximateBedAndWakeTimesStillCount() {
        assertEquals(7.5, Sleep.parse("f\u00e9l 12 ut\u00e1n alhattam el, 7 el\u0151tt \u00e9bredtem"), 0.01);
    }

    @Test public void wakingBeforeNineAloneIsNotANight() {
        assertEquals(-1.0, Sleep.parse("9 el\u0151tt keltem"), 0.01);
    }

    @Test public void aDecimalHourNextToTheSleepWordCounts() {
        assertEquals(7.5, Sleep.parse("78,2 kg / 54 rhr / 7,5h alv\u00e1s"), 0.01);
    }

    @Test public void aTrailingHourCountAfterAClauseStillCounts() {
        assertEquals(6.0, Sleep.parse(
                "nappal aludtam, mert \u00e9jjel dolgoztam: 6 \u00f3ra"), 0.01);
        assertEquals(6.5, Sleep.parse("m\u0171szak ut\u00e1n aludtam 6,5 \u00f3r\u00e1t"), 0.01);
    }

    @Test public void sleepWordBeforeTheNumberWithADayWordReads() {
        assertEquals(7.0, Sleep.parse("alv\u00e1s tegnap 7 \u00f3ra volt"), 0.01);
    }

    @Test public void theTransposedSleepTypoStillReads() {
        assertEquals(8.0, Sleep.parse("aludtma 8 \u00f3r\u00e1t"), 0.01);
    }

    @Test public void verbFirstBareHourCountReads() {
        assertEquals(8.0, Sleep.parse("aludtam 8-at"), 0.01);
    }

    @Test public void aMistypedAludtamStillCounts() {
        assertEquals(7.0, Sleep.parse("alutam 7 \u00f3r\u00e1t"), 0.01);
    }


    /**
     * A PERC is hozz\u00e1tartozik: a \u201eGarmin: 12500 l\u00e9p\u00e9s, alv\u00e1s 7 \u00f3ra 20 perc"
     * h\u00e9t \u00f3r\u00e1t \u00edrt be \u2013 a h\u00fasz perc minden \u00e9jszaka elveszett, mert az
     * alv\u00e1s-sz\u00f3 UT\u00c1NI \u00e1g az \u00f3ra-sz\u00e1mn\u00e1l meg\u00e1llt.
     */
    @Test
    public void theMinutesAfterTheSleepWordCount() {
        assertEquals(7.3, Sleep.parse("alv\u00e1s 7 \u00f3ra 20 perc"), 0.01);
        assertEquals(7.3, Sleep.parse("Garmin: 2450 kcal akt\u00edv kal\u00f3ria, "
                + "12500 l\u00e9p\u00e9s, 68 \u00e1tlagpulzus, alv\u00e1s 7 \u00f3ra 20 perc."), 0.01);
        // A perc n\u00e9lk\u00fcli alak marad.
        assertEquals(7.0, Sleep.parse("alv\u00e1s 7 \u00f3ra volt"), 0.01);
    }

    /**
     * Aki k\u00fcl\u00f6n kimondja, mikor aludt el, annak ott kezd\u0151dik az alv\u00e1sa.
     *
     * Az \u201eeste 10-kor lefek\u00fcdtem, de csak f\u00e9l 12-kor aludtam el, 6-kor
     * cs\u00f6rg\u00f6tt az \u00f3ra" tizenh\u00e1rom \u00e9s f\u00e9l \u00f3r\u00e1s alv\u00e1s lett: a beolvas\u00f3 az
     * els\u0151 k\u00e9t id\u0151pontot vette \u2013 a lefekv\u00e9st \u00e9s az elalv\u00e1st \u2013, a
     * felkel\u00e9st pedig eldobta.
     */
    @Test
    public void theThirdTimeIsNotIgnored() {
        assertEquals(6.5, Sleep.parse("Este 10-kor lefek\u00fcdtem, de csak f\u00e9l 12-kor aludtam el, "
                + "6-kor cs\u00f6rg\u00f6tt az \u00f3ra."), 0.01);
        assertEquals(7.0, Sleep.parse("Este 11-kor fek\u00fcdtem le, de csak \u00e9jf\u00e9lkor aludtam el, "
                + "reggel 7-kor keltem."), 0.01);
        // A k\u00e9t id\u0151pontos alak v\u00e1ltozatlan.
        assertEquals(8.0, Sleep.parse("Este 10-kor lefek\u00fcdtem, 6-kor cs\u00f6rg\u00f6tt az \u00f3ra."), 0.01);
        assertEquals(6.5, Sleep.parse("F\u00e9l 12-kor aludtam el, 6-kor keltem."), 0.01);
    }

}
