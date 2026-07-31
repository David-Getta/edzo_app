package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Több edzés felvétele egyetlen mondatból.
 *
 * A felismerés szándékosan óvatos: amit nem ért, azt kihagyja, nem talál ki
 * edzést. Egy kitalált bejegyzés rosszabb a hiányzónál, mert a naplóba kerül,
 * és onnan a szériába, az XP-be és a statisztikába is – a felhasználó pedig
 * nem tudja, honnan jött. Ezért a mentés előtt előnézet is van.
 */
public class ActivitiesParseTest {

    private static String summary(String text) {
        Activities.Parsed p = Activities.parse(text);
        StringBuilder sb = new StringBuilder();
        for (Activities.Plan pl : p.plans) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pl.count).append("×").append(pl.kind.id).append("/").append(pl.minutes);
        }
        return p.days + "d+" + p.offset + ": " + sb;
    }

    @Test public void theSentenceFromTheRequestWorks() {
        assertEquals("3d+0: 3×futas/45, 6×kezilabda/90",
                summary("az elmúlt 3 nap alatt 3 futó edzés és 6 kézi edzés"));
    }

    @Test public void theTimeSpanIsUnderstood() {
        assertEquals(1, Activities.parse("3 futás").days);
        assertEquals(5, Activities.parse("az elmúlt 5 napban 3 futás").days);
        assertEquals(7, Activities.parse("az elmúlt héten 2 úszás").days);
        assertEquals(14, Activities.parse("2 hét alatt 6 futás").days);
        assertEquals(4, Activities.parse("három kézilabda edzés az elmúlt 4 napban").days);
    }

    @Test public void aNamedDayBecomesAnOffset() {
        Activities.Parsed y = Activities.parse("tegnap 1 kondi");
        assertEquals(1, y.days);
        assertEquals(1, y.offset);
        assertEquals(2, Activities.parse("tegnapelőtt 2 kondi").offset);
        assertEquals(0, Activities.parse("ma 2 futás").offset);
    }

    @Test public void weekdayNamesBecomeTheRightOffset() {
        // 2026. július 31. péntek dél (Budapest).
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(2026, java.util.Calendar.JULY, 31, 12, 0, 0);
        long friday = c.getTimeInMillis();
        assertEquals(4, Activities.parse("hétfőn futottam", friday).offset);
        assertEquals(3, Activities.parse("kedden 2 kondi", friday).offset);
        // Ha ma van az a nap, a mairól van szó.
        assertEquals(0, Activities.parse("pénteken úsztam", friday).offset);
        // A legutóbbi szombat: hat napja.
        assertEquals(6, Activities.parse("szombaton 1 túra", friday).offset);
        // A darabszám nem sérül, és egy napról van szó, nem időszakról.
        Activities.Parsed p = Activities.parse("kedden 2 kondi", friday);
        assertEquals(1, p.days);
        assertEquals(2, p.plans.get(0).count);
    }

    @Test public void similarWordsAreNotTimeSpans() {
        // A „hétfőn” és a „naplóban” tartalmazza a hét/nap szótövet, de nem
        // időszak. Enélkül a „hétfőn futottam” egy hetes szórásba került volna.
        assertEquals(1, Activities.parse("hétfőn futottam").days);
        assertEquals(1, Activities.parse("a naplóban 3 futás").days);
        // A „hétvégén" sem hetes szórás – az a saját, kétnapos szabályát követi.
        assertTrue(Activities.parse("hétvégén 2 túra").days <= 2);
    }

    @Test public void oneEachPerDayIsUnderstood() {
        // A „tegnap és ma 1-1 futás" tipikus magyar forma: naponta egy.
        assertEquals("2d+0: 2×futas/45", summary("tegnap és ma 1-1 futás"));
        assertEquals("2d+0: 2×futas/45", summary("tegnap és ma egy-egy futás"));
        assertEquals("3d+0: 3×kondi/60", summary("az elmúlt 3 napban 1-1 kondi"));
        // Két mozgásnál az „1-1" fejenként egyet jelent, nem naponta egyet.
        assertEquals("1d+0: 1×kezilabda/90, 1×foci/90", summary("1-1 kézi és foci"));
        // A „10-15 perc" tartomány nem osztó számnév.
        assertEquals(1, Activities.parse("futás 10-15 perc").plans.get(0).count);
        // A „2-2" is működik: naponta kettő.
        assertEquals("2d+0: 4×uszas/45", summary("tegnap és ma 2-2 úszás"));
    }

    @Test public void everyDayMeansOnePerDay() {
        // A „minden nap" és a „naponta" gyakoriság: a darabszám naponta értendő.
        assertEquals("7d+0: 7×futas/45", summary("a héten minden nap futottam"));
        assertEquals("7d+0: 7×egyeb/45", summary("minden nap edzettem a héten"));
        assertEquals("14d+0: 14×kondi/60", summary("2 hét alatt mindennap kondiztam"));
        // A „naponta kétszer" szorzódik: 2 × 7 nap.
        assertEquals(14, Activities.parse("naponta kétszer úsztam a héten")
                .plans.get(0).count);
        // Időszak nélkül nincs mivel szorozni: marad egy alkalom.
        assertEquals(1, Activities.parse("minden nap futottam").plans.get(0).count);
        // A „heti 3 kondi" heti összesen három, NEM naponta három.
        assertEquals("7d+0: 3×kondi/60", summary("heti 3 kondi"));
    }

    @Test public void yesterdayAndTodaySpreadOverTwoDays() {
        Activities.Parsed p = Activities.parse("tegnap és ma 1-1 futás");
        assertEquals(2, p.days);
        assertEquals(0, p.offset);
        // Az egyik bejegyzés ma, a másik tegnap – nem mindkettő tegnap.
        long now = System.currentTimeMillis();
        long[] ts = Activities.timestamps(p, now);
        assertEquals(2, ts.length);
        assertTrue("az első a mai mostani pillanat", now - ts[0] < 60_000);
        assertTrue("a második a tegnapi napra esik",
                ts[1] < now - 11L * 3600 * 1000 && ts[1] > now - 48L * 3600 * 1000);
        // A sima „tegnap"/„ma" nem sérül.
        assertEquals(1, Activities.parse("tegnap 1 kondi").offset);
        assertEquals(0, Activities.parse("ma 2 futás").offset);
        assertEquals(2, Activities.parse("tegnapelőtt 2 kondi").offset);
    }

    @Test public void theWeekendMeansLastSaturdayAndSunday() {
        // 2026. július 31. péntek dél (Budapest).
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(2026, java.util.Calendar.JULY, 31, 12, 0, 0);
        long friday = c.getTimeInMillis();
        // Pénteken írva a múlt hétvége: vasárnap 5, szombat 6 napja volt.
        Activities.Parsed p = Activities.parse("a hétvégén 2 túra", friday);
        assertEquals(2, p.days);
        assertEquals(5, p.offset);
        assertEquals(2, p.plans.get(0).count);
        // Szombaton írva a mai nap; vasárnap írva a tegnap-ma kettő.
        c.add(java.util.Calendar.DAY_OF_YEAR, 1);
        long saturday = c.getTimeInMillis();
        assertEquals(0, Activities.parse("hétvégén túráztunk", saturday).offset);
        assertEquals(1, Activities.parse("hétvégén túráztunk", saturday).days);
        c.add(java.util.Calendar.DAY_OF_YEAR, 1);
        long sunday = c.getTimeInMillis();
        assertEquals(0, Activities.parse("hétvégén túráztunk", sunday).offset);
        assertEquals(2, Activities.parse("hétvégén túráztunk", sunday).days);
        // Hétfőn írva: vasárnap tegnap volt.
        c.add(java.util.Calendar.DAY_OF_YEAR, 1);
        assertEquals(1, Activities.parse("hétvégén 1 túra", c.getTimeInMillis()).offset);
        // A darabszám és a mozgás nem sérül.
        assertEquals("tura", p.plans.get(0).kind.id);
    }

    @Test public void theDurationCanBeGivenPerActivity() {
        assertEquals("1d+0: 5×futas/30", summary("5 futás 30 perc"));
        assertEquals("1d+0: 3×kondi/90, 2×futas/40", summary("3 kondi 90 perc és 2 futás 40 perc"));
        assertEquals("1d+0: 1×joga/60", summary("egy óra jóga"));
        assertEquals("1d+0: 2×tenisz/60", summary("2 tenisz 1 óra"));
        // Ahol nincs megadva, a mozgásforma szokásos hossza jön.
        assertEquals("1d+0: 2×kezilabda/90", summary("2 kézilabda"));
    }

    @Test public void colloquialNamesAreUnderstood() {
        assertEquals("1d+0: 6×kezilabda/90", summary("6 kézi"));
        assertEquals("1d+0: 1×kerekpar/60", summary("bringa"));
        assertEquals("1d+0: 2×kondi/60", summary("2 konditerem"));
        assertEquals("1d+0: 1×kosarlabda/60", summary("kosaraztam"));
        assertEquals("1d+0: 2×tura/90", summary("2 séta"));
    }

    @Test public void oneWordCannotBecomeTwoActivities() {
        // A „futó edzés” szóban benne van az „edzés” is – abból nem lehet külön
        // „egyéb mozgás”. A puszta „edzés” viszont még menthető tartalékként.
        assertEquals("1d+0: 3×futas/45", summary("3 futó edzés"));
        assertEquals("1d+0: 2×kezilabda/90", summary("2 kézi edzés"));
        assertEquals("7d+0: 4×egyeb/45", summary("a héten 4 edzés"));
    }

    @Test public void nonsenseProducesNothing() {
        assertTrue(Activities.parse("semmi értelmes szöveg").isEmpty());
        assertTrue(Activities.parse("").isEmpty());
        assertTrue(Activities.parse(null).isEmpty());
        assertTrue(Activities.parse("   ").isEmpty());
    }

    @Test public void theCountsStayInASaneRange() {
        // Elgépelés ne írjon a naplóba száz bejegyzést.
        for (Activities.Plan p : Activities.parse("1000 futás").plans)
            assertTrue("túl sok bejegyzés: " + p.count, p.count <= 50);
        assertEquals(30, Activities.parse("harminc futás").plans.get(0).count);
        // Szám nélkül egy alkalom.
        assertEquals(1, Activities.parse("futás").plans.get(0).count);
    }

    @Test public void theTotalIsTheSumOfTheCounts() {
        Activities.Parsed p = Activities.parse("az elmúlt 3 nap alatt 3 futó edzés és 6 kézi edzés");
        assertEquals(9, p.total());
        assertEquals(2, p.plans.size());
        // A címke emberi olvasásra való – ez megy az előnézetbe.
        assertTrue(p.plans.get(0).label().contains("Futás"));
        assertTrue(p.plans.get(0).label().contains("45 perc"));
    }

    @Test public void everydaySpokenFormsWork() {
        // Ahogy az ember tényleg beszél – igék, töltelékszavak, fél óra.
        assertEquals("1d+0: 1×kondi/60", summary("gyúrtam 1 órát"));
        assertEquals("1d+0: 1×egyeb/60", summary("sportoltam egy órát"));
        assertEquals("1d+0: 1×egyeb/30", summary("mozogtam 30 percet"));
        assertEquals("1d+0: 1×tenisz/60", summary("pingpongoztam"));
        assertEquals("1d+0: 1×joga/30", summary("jógáztam fél órát"));
        assertEquals("1d+0: 1×tura/90", summary("másfél óra túra"));
        // A „meccs” és a „kb” beékelődhet a szám és a sport közé.
        assertEquals("1d+0: 2×kezilabda/90", summary("2 meccs kézilabda"));
        assertEquals("1d+0: 2×foci/90", summary("két meccs foci"));
        assertEquals(5, Activities.parse("kb 5 futás").plans.get(0).count);
    }

    @Test public void decimalHoursAreNotFiveHours() {
        // Az „1,5 óra" korábban 5 órának számított (a vessző utáni 5-öt látta),
        // az elé csúszott „1" pedig darabszám lett: a „2,5 óra túra" KÉT túrát
        // adott 300 perccel. Most egy túra 150 perccel.
        assertEquals("1d+0: 1×kerekpar/90", summary("1,5 óra bringa"));
        assertEquals("1d+0: 1×tura/150", summary("2,5 óra túra"));
        assertEquals("1d+0: 1×futas/30", summary("0,5 óra futás"));
        assertEquals("1d+0: 1×uszas/90", summary("1,5 órát úsztam"));
        // A tört órák kiírva is: negyed, fél, háromnegyed, másfél.
        assertEquals("1d+0: 1×joga/15", summary("negyed óra jóga"));
        assertEquals("1d+0: 1×joga/45", summary("háromnegyed óra jóga"));
        // Az egész órák nem romolhattak el.
        assertEquals("1d+0: 1×joga/60", summary("egy óra jóga"));
        assertEquals("1d+0: 2×tenisz/60", summary("2 tenisz 1 óra"));
    }

    @Test public void distancesAreUnderstoodAndNotMistakenForCounts() {
        // A „10 km futás” EGY tíz kilométeres futás – nem tíz darab futás.
        Activities.Plan p = Activities.parse("10 km futás").plans.get(0);
        assertEquals(1, p.count);
        assertEquals(10, p.km, 0.001);
        // Időtartam híján a tipikus tempóból jön a hossz (6 perc/km).
        assertEquals(60, p.minutes);
        // Mindkét magyar szórend, ragozva is.
        assertEquals(10, Activities.parse("futottam 10 km-t").plans.get(0).km, 0.001);
        assertEquals(10, Activities.parse("10 kilométert futottam").plans.get(0).km, 0.001);
        // Tizedes táv.
        assertEquals(2.5, Activities.parse("2,5 km úszás").plans.get(0).km, 0.001);
    }

    @Test public void aDistanceAttachesToTheRightSport() {
        Activities.Parsed p = Activities.parse("10 km futás és 20 km bringa");
        assertEquals(10, p.plans.get(0).km, 0.001);
        assertEquals(20, p.plans.get(1).km, 0.001);
        // Kézilabdához nincs útvonal: a táv ott nem jelent semmit.
        assertEquals(0, Activities.parse("5 km kézilabda").plans.get(0).km, 0.001);
        assertEquals(1, Activities.parse("5 km kézilabda").plans.get(0).count);
    }

    @Test public void anExplicitDurationBeatsThePaceEstimate() {
        Activities.Plan p = Activities.parse("futás 10 km 50 perc").plans.get(0);
        assertEquals(10, p.km, 0.001);
        assertEquals(50, p.minutes);
        // Darabszám és táv együtt: három ötkilométeres futás.
        Activities.Plan q = Activities.parse("3 futás 5 km").plans.get(0);
        assertEquals(3, q.count);
        assertEquals(5, q.km, 0.001);
    }

    @Test public void aMarathonIsItsOwnDistance() {
        // A „maraton" neve maga a táv – nem kell mellé kilométer.
        Activities.Plan p = Activities.parse("lefutottam a maratont").plans.get(0);
        assertEquals("futas", p.kind.id);
        assertEquals(42.2, p.km, 0.001);
        assertEquals(1, p.count);
        // Félmaraton egybe- és különírva.
        assertEquals(21.1, Activities.parse("félmaraton").plans.get(0).km, 0.001);
        assertEquals(21.1, Activities.parse("fél maraton").plans.get(0).km, 0.001);
        // A kimondott idő erősebb a tempóbecslésnél.
        assertEquals(240, Activities.parse("maraton 4 óra alatt").plans.get(0).minutes);
        // A kimondott táv erősebb a névnél (terep-félmaraton, rövidített kör).
        assertEquals(19.5, Activities.parse("félmaraton 19,5 km").plans.get(0).km, 0.001);
    }

    @Test public void anAbsurdDistanceIsDropped() {
        assertEquals(0, Activities.parse("1000 km bringa").plans.get(0).km, 0.001);
    }

    @Test public void justSayingITrainedIsEnough() {
        // A „ma edzettem" a leggyakoribb magyar edzésmondat – és semmit sem
        // adott: a tartalék csak az „edzés" főnevet ismerte, az igét nem.
        assertEquals("1d+0: 1×egyeb/45", summary("ma edzettem"));
        assertEquals(1, Activities.parse("tegnap edzettem 1 órát").offset);
        assertEquals(60, Activities.parse("tegnap edzettem 1 órát").plans.get(0).minutes);
    }

    @Test public void gymAndFitnessSynonymsMapToTheRightSport() {
        assertEquals("kondi", Activities.parse("crossfit").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("trx edzés").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("erősítő edzés").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("fekvőtámaszok").plans.get(0).kind.id);
        assertEquals("foci", Activities.parse("futballoztam").plans.get(0).kind.id);
        assertEquals("tenisz", Activities.parse("ping pong").plans.get(0).kind.id);
        assertEquals("korcsolya", Activities.parse("görkoriztam").plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("tekertem egy órát").plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("bmx").plans.get(0).kind.id);
        assertEquals("tanc", Activities.parse("kangoo").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("lovagoltam").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("vitorlázás").plans.get(0).kind.id);
    }

    @Test public void allKindsOfTornaAreMobility() {
        // A „torna" tő fedi a gerinctornát, a gyógytornát és a tornázást is.
        assertEquals("joga", Activities.parse("gerinctorna").plans.get(0).kind.id);
        assertEquals("joga", Activities.parse("gyógytorna").plans.get(0).kind.id);
        assertEquals("joga", Activities.parse("tornáztam fél órát").plans.get(0).kind.id);
        // A „tornaterem" viszont kondi, és NEM esik szét torna + terem párra.
        Activities.Parsed p = Activities.parse("tornateremben gyúrtam");
        assertEquals(1, p.plans.size());
        assertEquals("kondi", p.plans.get(0).kind.id);
        // A „csupán sétáltam" nem szörf (a rövid tövek nem eshetnek szavakba).
        assertEquals("tura", Activities.parse("csupán sétáltam").plans.get(0).kind.id);
    }

    @Test public void hoursAndMinutesTogetherAreOneDuration() {
        // A „futás 1 óra 15 perc" korábban 15 perc lett: a perc külön
        // időtartamnak számított, és a közelebbi nyert.
        assertEquals("1d+0: 1×futas/75", summary("futás 1 óra 15 perc"));
        assertEquals("1d+0: 1×kerekpar/90", summary("1 óra 30 perc bringa"));
        assertEquals("1d+0: 1×tura/150", summary("2 óra és 30 perc túra"));
        // Két KÜLÖN időtartam két sporthoz nem olvad össze.
        assertEquals("1d+0: 1×kondi/60, 1×futas/40", summary("kondi 1 óra futás 40 perc"));
    }

    @Test public void multiplicativeNumeralsAreCounts() {
        // A „kétszer úsztam" EGY úszás volt: a számnév-kereső szóhatárt vár,
        // a rag miatt nem találta meg a „két"-et.
        assertEquals(2, Activities.parse("kétszer úsztam").plans.get(0).count);
        assertEquals(3, Activities.parse("háromszor futottam").plans.get(0).count);
        assertEquals(3, Activities.parse("3-szor futottam a héten").plans.get(0).count);
        assertEquals(7, Activities.parse("a héten hétszer gyúrtam").plans.get(0).count);
        // Az „egyszerűen" nem darabszám-hiba: marad egy alkalom.
        assertEquals(1, Activities.parse("egyszerűen jó futás volt").plans.get(0).count);
    }

    @Test public void metersWorkForSwimming() {
        // Úszásnál a méter a természetes egység, nem a kilométer.
        Activities.Plan p = Activities.parse("leúsztam 2000 métert").plans.get(0);
        assertEquals(2.0, p.km, 0.001);
        assertEquals(1, p.count);
        assertEquals(1.5, Activities.parse("1500 m úszás").plans.get(0).km, 0.001);
        // A „3 meccs kézilabda" nem 3 méter: a szókezdő „m" nem egység.
        assertEquals(3, Activities.parse("3 meccs kézilabda").plans.get(0).count);
        // Az 5 méter nem edzéstáv – elgépelésként eldobjuk.
        assertEquals(0, Activities.parse("5 m futás").plans.get(0).km, 0.001);
    }

    @Test public void aMonthIsAThirtyDaySpan() {
        assertEquals(30, Activities.parse("egy hónap alatt 10 edzés").days);
        assertEquals(30, Activities.parse("ebben a hónapban 4 kondi").days);
        assertEquals(60, Activities.parse("2 hónap alatt 20 futás").days);
        assertEquals(10, Activities.parse("egy hónap alatt 10 edzés").plans.get(0).count);
    }

    @Test public void theFallbackWorkoutKeepsItsDuration() {
        // Az „otthoni edzés 40 perc" 45 perc lett: az egyéb-mozgás tartalék
        // nem nézte meg a kimondott időtartamot.
        assertEquals("1d+0: 1×egyeb/40", summary("otthoni edzés 40 perc"));
        assertEquals("7d+0: 4×egyeb/45", summary("a héten 4 edzés"));
    }

    @Test public void crossCountrySkiingIsNotRunning() {
        assertEquals("si", Activities.parse("sífutás 2 óra").plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("futás 1 óra").plans.get(0).kind.id);
    }

    @Test public void suffixedFillerWordsDoNotHideTheCount() {
        assertEquals(2, Activities.parse("2 meccsen kézilabdáztam").plans.get(0).count);
        assertEquals(3, Activities.parse("3 darabot futottam").plans.get(0).count);
    }

    @Test public void theExampleSentencesShownToTheUserAllParse() {
        // Ugyanezek a minták váltakoznak a beviteli mezőben – ha egy példa
        // nem működne, pont a mintamondat járatná le a felismerést.
        String[] examples = {
                "az elmúlt 3 nap alatt 3 futó edzés és 6 kézi edzés",
                "kétszer úsztam a héten",
                "tegnap 10 km futás 50 perc alatt",
                "hétfőn 1 óra 15 perc kondi",
                "leúsztam 1500 métert",
                "egy hónap alatt 10 edzés",
        };
        for (String e : examples)
            assertTrue("a mintamondat nem érthető: " + e, !Activities.parse(e).isEmpty());
        // Egy-egy jellemző részlet is stimmel.
        assertEquals(2, Activities.parse(examples[1]).plans.get(0).count);
        assertEquals(50, Activities.parse(examples[2]).plans.get(0).minutes);
        assertEquals(75, Activities.parse(examples[3]).plans.get(0).minutes);
        assertEquals(1.5, Activities.parse(examples[4]).plans.get(0).km, 0.001);
        assertEquals(30, Activities.parse(examples[5]).days);
    }

    @Test public void aNumberFarFromTheActivityIsNotItsCount() {
        // A szám és a mozgás közé nem eshet másik szó: különben a „3 nap múlva
        // futás” három futássá válna.
        assertEquals(1, Activities.parse("3 kiló fogyás után futás").plans.get(0).count);
    }
}
