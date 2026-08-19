package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * A mondat-útbaigazító.
 *
 * Nem új felismerés: azt mondja meg, MELYIK meglévő felismerő tud a mondattal
 * mit kezdeni. A tét az, hogy a rossz képernyőn ne „ezt még nem ismerem"
 * legyen a válasz egy tökéletesen érthető mondatra.
 */
public class SentenceTest {

    private static final List<Foods.Food> FOODS = Arrays.asList(Foods.ALL);
    private static final long NOW = 1_753_869_600_000L;

    private static Sentence.Kind of(String q) { return Sentence.of(q, FOODS, NOW); }

    @Test public void everySentenceFindsItsOwnLog() {
        String[][] cases = {
                {"30 perc futás", "WORKOUT"},
                {"10 km-t bicikliztem", "WORKOUT"},
                {"tegnap este kondi", "WORKOUT"},
                {"6x1 km", "WORKOUT"},
                {"10000 lépés", "WORKOUT"},
                {"1 óra jóga", "WORKOUT"},
                {"3x10 fekvenyomás 60 kg", "STRENGTH"},
                {"guggolás 5x5 80 kg", "STRENGTH"},
                {"4 sorozat 8 fekvenyomás", "STRENGTH"},
                {"plank 3x1 perc", "STRENGTH"},
                {"100 fekvőtámasz", "STRENGTH"},
                {"3 kör 40 mp munka 20 mp pihenő", "INTERVAL"},
                {"8x20/10", "INTERVAL"},
                {"emom 10 perc", "INTERVAL"},
                {"20 perc alatt 40/20", "INTERVAL"},
                {"1:30 munka 0:30 pihenő 6 kör", "INTERVAL"},
                {"rántott hús rizzsel", "MEAL"},
                {"2 tojás", "MEAL"},
                {"150 g csirkemell 200 g rizs", "MEAL"},
                {"fél alma", "MEAL"},
                {"tegnap este pizzát ettem", "MEAL"},
                {"ittam fél liter vizet", "MEAL"},
        };
        StringBuilder bad = new StringBuilder();
        for (String[] c : cases) {
            String got = of(c[0]).name();
            if (!got.equals(c[1]))
                bad.append("\n  ").append(c[0]).append(" -> ").append(got)
                   .append(" (várt: ").append(c[1]).append(")");
        }
        assertEquals("rossz ajtó:" + bad, 0, bad.length());
    }

    /**
     * Amit egyik felismerő sem ért, arra ne találjunk ki naplót.
     *
     * A rossz útbaigazítás rosszabb a semminél: elviszi a felhasználót egy
     * képernyőre, ahol ugyanúgy nem lesz belőle bejegyzés.
     */
    @Test public void whatNobodyUnderstandsStaysUnderstoodByNobody() {
        for (String q : new String[]{"jó napom volt", "asdfgh", "ma nem edzettem",
                "holnap majd", "köszönöm szépen", "hm", ""})
            assertEquals(q, Sentence.Kind.NONE, of(q));
    }

    /**
     * A hétköznapi mondat nem lesz étkezés.
     *
     * A söprés mindegyiket élesben fogta meg: a „szeretek futni" retket, a
     * „fogás" fogast (egy halat), a „levesszük" levest, a „bőrrel" bort
     * naplózott. Itt a MONDAT szintjén nézzük ugyanezt: ha egy ilyen szó
     * bekerül egy jelentéktelen mondatba, semmi nem történhet.
     */
    @Test public void everydayTalkIsNotAMeal() {
        for (String q : new String[]{"szeretek futni és úszni", "jó fogás volt",
                "levesszük a súlyt a rúdról", "bőrrel vagy anélkül",
                "a főképernyőn nézem", "hüvelykujjszabály", "ez a szokásaim része",
                "adatmezők a kijelzőn", "beszédbuborék"})
            assertNotEquals(q, Sentence.Kind.MEAL, of(q));
    }

    /**
     * A sorozatos mondat az erősítő naplóé, akkor is, ha edzésnek is elmenne.
     *
     * A súly és az ismétlés CSAK ott őrződik meg; egy általános „kondi"
     * bejegyzésben mindkettő elveszne.
     */
    @Test public void setsBeatAPlainWorkout() {
        assertEquals(Sentence.Kind.STRENGTH, of("tegnap guggolás 4x8 100 kg"));
        assertEquals(Sentence.Kind.STRENGTH, of("30 fekvőtámasz és 20 guggolás"));
    }

    @Test public void everyKindHasAWhereAndAHint() {
        for (Sentence.Kind k : Sentence.Kind.values()) {
            if (k == Sentence.Kind.NONE) {
                assertEquals("", Sentence.where(k));
                assertEquals("", Sentence.hint(k));
                continue;
            }
            assertTrue(k.name(), Sentence.where(k).length() > 2);
            assertTrue(k.name(), Sentence.hint(k).length() > 20);
        }
    }

    /**
     * Az étkezés-mondatból ne legyen fantom-edzés.
     *
     * Ez a rosszabbik irány: egy kitalált edzés bekerül a szériába, az XP-be,
     * a jelvényekbe és a statisztikába is – és a felhasználó a naplóban látja
     * viszont, hogy „ma edzett", pedig csak vacsorázott.
     */
    @Test public void aMealNeverBecomesAWorkout() {
        StringBuilder bad = new StringBuilder();
        for (String q : MEALS) {
            if (!StrengthParse.parse(q).isEmpty()) bad.append("\n  sorozat: ").append(q);
            if (!Activities.parse(q, NOW).isEmpty()) bad.append("\n  edzés: ").append(q);
            if (IntervalParse.parse(q) != null) bad.append("\n  időzítő: ").append(q);
            if (!BodyParse.parse(q).isEmpty()) bad.append("\n  mérés: ").append(q);
        }
        assertEquals("fantom bejegyzés:" + bad, 0, bad.length());
    }

    /** …és az edzés-mondatból se legyen véletlen étkezés. */
    @Test public void aWorkoutNeverBecomesAMeal() {
        StringBuilder bad = new StringBuilder();
        for (String q : WORKOUTS) {
            java.util.List<Foods.Hit> h = Foods.parse(FOODS, q);
            if (!h.isEmpty()) bad.append("\n  ").append(q).append(" -> ").append(h.get(0).food.name);
        }
        assertEquals("kitalált étel:" + bad, 0, bad.length());
    }

    /** Életszerű étkezés-mondatok – a felismerés MÁSIK oldalának határa. */
    private static final String[] MEALS = {
            "reggelire zabkása", "ebédre csirkemell rizzsel", "vacsorára rántotta két tojásból",
            "uzsonnára egy alma", "ettem egy szendvicset", "ittam egy kávét tejjel",
            "gyros tál", "hekk sültkrumplival", "tojásos nokedli", "grillcsirke saláta",
            "túró rudi", "egy tábla csoki", "kaptam egy szelet tortát", "ettem egy adag lecsót",
            "vettem egy kiflit", "kolbászos rántotta", "krumplifőzelék fasírttal",
            "sonkás-sajtos melegszendvics", "egy marék mandula", "reggeli: kefir és banán",
            "hamburger menü", "tejbegríz", "zsemle vajjal", "csirkepaprikás nokedlivel",
            "vegyes saláta olívaolajjal", "150 g csirkemell 200 g rizs", "2 tojás",
            "fél alma", "két szelet kenyér", "3 dl tej", "tegnap este pizzát ettem",
            "ma reggel müzli joghurttal", "1 kg alma", "20 dkg sajt", "fél adag gyros",
            "banán 2 db", "tojás (3 db)", "5 dl narancslé", "egy pohár bor",
            "két korsó sör", "10 szem mandula",
    };

    /** Életszerű mozgás-mondatok, mind a négy másik felismerőből. */
    private static final String[] WORKOUTS = {
            "30 perc futás", "10 km-t bicikliztem", "tegnap este kondi", "1 óra jóga",
            "reggel elmentem futni fél órát", "ma edzettem egy órát a teremben",
            "délután gyalogoltam egy órát", "egy óra spinning", "45 perces edzés",
            "csináltam egy 20 perces kocogást", "10000 lépés", "ma sokat sétáltam",
            "húsz perc nyújtás", "két óra foci meccs", "reggeli torna 15 perc",
            "este 30 perc szobabicikli", "1 óra 20 perc futás", "futottam 8 km-t 45 perc alatt",
            "úsztam 1000 métert", "korcsolyáztam egy órát", "3x10 fekvenyomás 60 kg",
            "guggolás 5x5 80 kg", "húzódzkodás 3x8", "plank 3x1 perc",
            "4 sorozat 8 fekvenyomás", "100 fekvőtámasz", "bicepsz 12-10-8 15 kg",
            "lábtolás 3x12 120 kg", "evezés 4x10 50 kg", "3 kör 40 mp munka 20 mp pihenő",
            "8x20/10", "tabata", "emom 10 perc", "ma reggel 78,4 kg", "78 kiló vagyok",
            "mérleg: 81,2", "18% testzsír", "kézilabda meccs", "teniszeztem egy órát",
            "asztalitenisz 40 perc", "boxoltam 30 percet", "sí 3 óra",
    };

    /**
     * A megosztott sablon neve ne térítse el a tervet.
     *
     * A sablonok nevében gyakran ott egy sportszó („Zsírégető HIIT", „Kondi
     * kör", „Futó intervall"), és attól a megosztott szöveg megtörtént
     * edzésnek látszott – a beállítás helyett egy edzés-bejegyzést kínált.
     * A PIHENŐT is kimondó, többköros terv viszont egyértelműen időzítő.
     */
    @Test public void aSharedTemplateStaysATimerPlan() {
        for (String q : new String[]{
                "Zsírégető HIIT: 8 kör 40 mp munka 20 mp pihenő  (összesen 8 perc)",
                "Tabata: 8 kör 20 mp munka 10 mp pihenő  (összesen 4 perc)",
                "Futó intervall: 6 kör 2 perc munka 1 perc pihenő  (összesen 18 perc)",
                "Kondi kör: 10 kör 45 mp munka 15 mp pihenő  (összesen 10 perc)"})
            assertEquals(q, Sentence.Kind.INTERVAL, of(q));
        // A megtörtént edzés viszont marad edzés.
        assertEquals(Sentence.Kind.WORKOUT, of("45 perc kondi"));
        assertEquals(Sentence.Kind.WORKOUT, of("30 perc futás"));
        assertEquals(Sentence.Kind.WORKOUT, of("ma 1 óra hiit edzés"));
    }

    /** Ételek nélkül (null) is működik – a hívónak nem kell listát adnia. */
    @Test public void foodsAreOptional() {
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("30 perc futás", null, NOW));
        assertEquals(Sentence.Kind.NONE, Sentence.of("2 tojás", null, NOW));
    }

    /**
     * A kérdés nem bejegyzés.
     *
     * A „mennyi kalória van a banánban?" banánt naplózott volna, a „mit
     * egyek edzés előtt?" pedig edzést – pedig aki kérdez, az épp nem evett
     * és nem edzett. A kérdőjel a legmegbízhatóbb jel erről.
     */
    @Test public void questionsAreNotEntries() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        long now = 1_753_869_600_000L;
        assertEquals(Sentence.Kind.NONE, Sentence.of("mennyi kalória van a banánban?", all, now));
        assertEquals(Sentence.Kind.NONE, Sentence.of("mit egyek edzés előtt?", all, now));
        assertEquals(Sentence.Kind.NONE, Sentence.of("hány kör legyen a tabata?", all, now));
        // A kijelentés viszont marad bejegyzés.
        assertEquals(Sentence.Kind.MEAL, Sentence.of("ettem egy banánt", all, now));
        assertEquals(Sentence.Kind.INTERVAL, Sentence.of("tabata", all, now));
    }

    /**
     * A panasz-KÉRDÉS a rehab-lapé.
     *
     * A kérdőjel általában azt jelenti, hogy nincs mit naplózni – de a
     * rehab-lap nem naplóz, hanem MUTAT. A „mit csináljak a fájó vállammal?"
     * és a „van valami gyakorlat a derékfájásra?" pont az a kérdés, amire jó
     * válaszunk van; eddig mindkettőre „nem értem" jött.
     */
    @Test public void aPainQuestionStillFindsTheRehabPage() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        long now = 1_753_869_600_000L;
        assertEquals(Sentence.Kind.REHAB, Sentence.of("mit csináljak a fájó vállammal?", all, now));
        assertEquals(Sentence.Kind.REHAB, Sentence.of("fáj a térdem, mit csináljak?", all, now));
        assertEquals(Sentence.Kind.REHAB, Sentence.of("van valami gyakorlat a derékfájásra?", all, now));
        assertEquals(Sentence.Kind.REHAB, Sentence.of("boka stabilitás gyakorlatok?", all, now));
        // A többi kérdés továbbra sem bejegyzés.
        assertEquals(Sentence.Kind.NONE, Sentence.of("mennyi kalória van a banánban?", all, now));
        assertEquals(Sentence.Kind.NONE, Sentence.of("mit egyek edzés előtt?", all, now));
        assertEquals(Sentence.Kind.NONE, Sentence.of("hány kört fussak?", all, now));
    }

    /**
     * A mondat MÁSIK fele sem veszhet el.
     *
     * A „futottam 30 percet és ettem egy banánt" banánja eddig nyomtalanul
     * eltűnt: az útbaigazító eldöntötte, hogy ez edzés, és a mondat többi
     * részét eldobta. Ez ugyanaz a csendes hiba, mint a meg nem történt
     * bejegyzés, csak fordítva – itt a napló KEVESEBBET tud a valóságnál.
     */
    @Test public void theOtherHalfOfTheSentenceIsOffered() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        long now = 1_753_869_600_000L;
        String[] mixed = {"futottam 30 percet és ettem egy banánt",
                "reggel 5 km futás, utána zabkása", "edzés és két tojás",
                "1 óra bringa, utána egy alma",
                "3x10 fekvenyomás 60 kg, utána protein turmix"};
        for (String q : mixed)
            assertEquals(q, Sentence.Kind.MEAL, Sentence.also(q, all, now));
        // A pihenés adatai egymás mellett: mindhárom a Profil naplója.
        assertEquals(Sentence.Kind.PULSE,
                Sentence.also("aludtam 8 órát, nyugalmi pulzus 52", all, now));
        assertEquals(Sentence.Kind.SLEEP,
                Sentence.also("ma reggel 78,4 kg, aludtam 7 órát", all, now));
        assertEquals(Sentence.Kind.SLEEP, Sentence.also("aludtam 8 órát, 78 kg", all, now));
        // Az edzés és az étkezés mellé is odaférhet a reggeli MÉRÉS: ezek
        // második fele eddig nyomtalanul eltűnt.
        assertEquals(Sentence.Kind.BODY,
                Sentence.also("10 km futás, 78,5 kg a mérlegen", all, now));
        assertEquals(Sentence.Kind.SLEEP,
                Sentence.also("aludtam 7 órát és futottam 10 km-t", all, now));
        assertEquals(Sentence.Kind.PULSE,
                Sentence.also("nyugalmi pulzus 50, ma 45 perc bringa", all, now));
        assertEquals(Sentence.Kind.SLEEP,
                Sentence.also("ettem egy pizzát és aludtam 9 órát", all, now));
        assertEquals(Sentence.Kind.BODY,
                Sentence.also("fáj a vállam, 78 kg vagyok", all, now));
        // Ahol nincs második napló, ott ne találgassunk. Huszonkét valódi
        // edzés-mondaton az étel-felismerő egyetlen ételt sem talált.
        for (String q : new String[]{"30 perc futás", "guggolás 5x5 100 kg",
                "8 kör 40 mp munka 20 mp pihenő", "150 g csirkemell rizzsel",
                "78,4 kg", "fáj a vállam", "Lábnap: guggolás, lábtolás, kitörés",
                "lábgép 3x12 80 kg és vádli 4x15", "mellgép 3x12",
                "combhajlítás 3x12 40 kg", "kettlebell swing 5x20 24 kg"})
            assertEquals(q, Sentence.Kind.NONE, Sentence.also(q, all, now));
        assertEquals(Sentence.Kind.NONE, Sentence.also(null, all, now));
    }

    /**
     * A sorozat mellé odaírt futás sem veszhet el.
     *
     * A vegyes edzésről egy mondatban szoktunk beszámolni: előbb a futás,
     * utána a saját testsúlyos rész. A sorozat erősebb jel, tehát a mondat az
     * erősítő naplóé – de a kilométerek ott nem férnek el sehol, és eddig
     * egyszerűen eltűntek.
     */
    @Test public void theRunNextToTheSetsIsNotLost() {
        long now = System.currentTimeMillis();
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(Sentence.Kind.STRENGTH,
                Sentence.of("reggel 5 km futás, utána 20 fekvőtámasz", all, now));
        assertEquals(Sentence.Kind.WORKOUT,
                Sentence.also("reggel 5 km futás, utána 20 fekvőtámasz", all, now));
        assertEquals(Sentence.Kind.WORKOUT,
                Sentence.also("ma 12000 lépés és 3x10 fekvenyomás 60 kg", all, now));
        // Táv nélkül nem: a puszta „edzés" szóból becsült hatvan perc kétszer
        // kerülne be, egyszer sorozatként, egyszer mozgásként.
        assertEquals(Sentence.Kind.NONE,
                Sentence.also("edzés: guggolás 5x5 100 kg", all, now));
    }

    /**
     * A fehérje-mondat is étkezés.
     *
     * A „120 g fehérjét vittem be ma" étkezés-mondat, csak épp egyetlen étel
     * nincs benne, amit az adatbázis ismerne – eddig semmi nem lett belőle.
     */
    @Test public void aProteinOnlySentenceIsAMeal() {
        long now = System.currentTimeMillis();
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals(Sentence.Kind.MEAL,
                Sentence.of("120 g fehérjét vittem be ma", all, now));
        assertEquals(Sentence.Kind.MEAL, Sentence.of("fehérje 95 g", all, now));
    }

    /**
     * A hosszú mondat MINDEN naplója előkerül.
     *
     * A „ma reggel 6-kor keltem, 79,2 kg volt a mérleg, futottam 8 km-t, utána
     * zabkása" négy adatot mond ki. Az `also` csak az elsőt adta vissza, a
     * többi nyomtalanul eltűnt – pedig a képernyő fel tudja ajánlani őket.
     */
    @Test public void everyLogOfALongSentenceIsFound() {
        long now = System.currentTimeMillis();
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String q = "ma reggel 6-kor keltem, 79,2 kg volt a mérleg, "
                + "futottam 8 km-t 45 perc alatt, utána zabkása";
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of(q, all, now));
        java.util.List<Sentence.Kind> more = Sentence.extras(q, all, now);
        assertTrue(more.contains(Sentence.Kind.MEAL));
        assertTrue(more.contains(Sentence.Kind.BODY));
        // Az `also` továbbra is a legfontosabbat adja: a lista első eleme.
        assertEquals(more.get(0), Sentence.also(q, all, now));
        // Ahol nincs második napló, ott üres a lista.
        assertTrue(Sentence.extras("30 perc futás", all, now).isEmpty());
        assertTrue(Sentence.extras(null, all, now).isEmpty());
    }

    /**
     * A panasz mellett ott lehet a megtörtént edzés is.
     *
     * A „fájt a térdem, ezért csak bicikliztem 40 percet" negyven perce
     * nyomtalanul eltűnt: a rehab-lap nem naplóz. Csak a KIMONDOTT mennyiség
     * számít – a mozgásforma szokásos hossza egy panasz-mondatban találgatás
     * lenne.
     */
    @Test public void aComplaintCanCarryARealWorkout() {
        long now = System.currentTimeMillis();
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String q = "fájt a térdem, ezért csak bicikliztem 40 percet";
        assertEquals(Sentence.Kind.REHAB, Sentence.of(q, all, now));
        assertEquals(Sentence.Kind.WORKOUT, Sentence.also(q, all, now));
        assertEquals(Sentence.Kind.WORKOUT,
                Sentence.also("húzódott a combom, de végigcsináltam a 8 km-t", all, now));
        // Kimondott mennyiség nélkül nincs mit felajánlani.
        assertEquals(Sentence.Kind.NONE, Sentence.also("fáj a térdem futás után", all, now));
        assertEquals(Sentence.Kind.NONE, Sentence.also("merev a nyakam", all, now));
    }
    @Test public void aTwoCharacterRunEntryStillRoutes() {
        assertEquals(Sentence.Kind.WORKOUT, Sentence.of("5k", null, NOW));
        assertEquals(Sentence.Kind.NONE, Sentence.of("ok", null, NOW));
    }

    /**
     * A m\u00e9r\u00e9s mell\u00e9 az \u00c9TKEZ\u00c9S is oda\u00e9r: az „eb\u00e9d: 200 g csirkemell.
     * Comb 55 cm." mondatban a m\u00e9r\u00e9s nyert, \u00e9s a k\u00e9tsz\u00e1z gramm
     * csirkemell nyomtalanul elt\u0171nt – a ford\u00edtottja (\u00e9tkez\u00e9s mellett
     * m\u00e9r\u00e9s) r\u00e9g megvolt.
     */
    @Test
    public void aMeasurementSentenceStillOffersTheMeal() {
        java.util.List<Foods.Food> db = java.util.Arrays.asList(Foods.ALL);
        long now = System.currentTimeMillis();
        assertEquals(Sentence.Kind.BODY,
                Sentence.of("Eb\u00e9d: 200 g csirkemell. Comb 55 cm.", db, now));
        assertTrue(Sentence.extras("Eb\u00e9d: 200 g csirkemell. Comb 55 cm.", db, now)
                .contains(Sentence.Kind.MEAL));
        // A tiszta m\u00e9r\u00e9s-mondat nem aj\u00e1nl \u00e9tkez\u00e9st.
        assertTrue(Sentence.extras("megm\u00e9rtem magam: 71,3 kg, comb 55 cm", db, now)
                .isEmpty());
    }

}
