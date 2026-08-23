package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

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

    @Test public void aHundredPushupsIsOneWorkoutNotFifty() {
        // A „100 fekvőtámasz" száz ISMÉTLÉS – korábban 50 külön kondi-edzésként
        // került volna a naplóba (a darabszám-korlátig felszorozva).
        Activities.Parsed p = Activities.parse("100 fekvőtámasz");
        assertEquals(1, p.plans.get(0).count);
        assertEquals("kondi", p.plans.get(0).kind.id);
        // Az idő az ismétlésszámból jön, nem az egyórás alapból.
        assertEquals(20, Activities.parse("megcsináltam 100 fekvőtámaszt")
                .plans.get(0).minutes);
        assertEquals(1, Activities.parse("50 guggolás").plans.get(0).count);
        assertEquals(1, Activities.parse("30 felülés").plans.get(0).count);
        // A kis szám viszont alkalom marad: két fekvőtámasz-edzés az kettő.
        assertEquals(2, Activities.parse("2 fekvőtámasz edzés").plans.get(0).count);
        // Kimondott idő erősebb a becslésnél.
        assertEquals(15, Activities.parse("100 fekvőtámasz 15 perc")
                .plans.get(0).minutes);
    }

    @Test public void slangAndCasualFormsAreUnderstood() {
        // Ahogy az emberek tényleg beszélnek a mozgásról.
        assertEquals("kerekpar", Activities.parse("bicajoztam").plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("cangáztam egy órát").plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("kocogtam").plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("futkároztam fél órát").plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("sprinteltem").plans.get(0).kind.id);
        assertEquals("evezes", Activities.parse("eveztem 20 percet").plans.get(0).kind.id);
        assertEquals(20, Activities.parse("eveztem 20 percet").plans.get(0).minutes);
        assertEquals("joga", Activities.parse("meditáltam fél órát").plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("nordic walking").plans.get(0).kind.id);
        assertEquals("uszas", Activities.parse("vízilabda").plans.get(0).kind.id);
        assertEquals("uszas", Activities.parse("aquafitness").plans.get(0).kind.id);
        // A gyakorító igealakok is („úszkáltam", „futkostam", „edzegettem").
        assertEquals("uszas", Activities.parse("úszkáltam egy órát").plans.get(0).kind.id);
        assertEquals(60, Activities.parse("úszkáltam egy órát").plans.get(0).minutes);
        assertEquals("futas", Activities.parse("futkostam").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("edzegettem").plans.get(0).kind.id);
        // A terem-gépek is a maguk sportját jelentik.
        assertEquals("futas", Activities.parse("futópad 30 perc").plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("szobabicikli 45 perc").plans.get(0).kind.id);
        assertEquals("evezes", Activities.parse("evezőpad 15 perc").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("ellipszis tréner 20 perc").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("lépcsőzőgép 10 perc").plans.get(0).kind.id);
        // A „beneveztem a versenyre" nem evezés (a „nevez" vége az „evez") –
        // az igekötős „kieveztem" viszont az.
        assertTrue(Activities.parse("beneveztem a versenyre").isEmpty());
        assertEquals("evezes", Activities.parse("kieveztem a tóra").plans.get(0).kind.id);
    }

    @Test public void aBareDistanceMeansARun() {
        // A „nyomtam egy 5 km-t" magyarul futást jelent – sport szó nélkül is.
        Activities.Plan p = Activities.parse("nyomtam egy 5 km-t").plans.get(0);
        assertEquals("futas", p.kind.id);
        assertEquals(5, p.km, 0.001);
        assertEquals(30, p.minutes);
        // Ha van megnevezett sport, a táv oda tartozik, nem lesz külön futás.
        Activities.Parsed q = Activities.parse("20 km bringa");
        assertEquals(1, q.plans.size());
        assertEquals("kerekpar", q.plans.get(0).kind.id);
    }

    @Test public void someoneElsesWorkoutIsNotMine() {
        // A „néztem" csak a SAJÁT tagmondatát törli – a focit a másik
        // tagmondat mondta ki, és eddig kilencven perces bejegyzés lett belőle.
        assertEquals("1d+0: ", summary("a fiam focizott, én csak néztem"));
        assertEquals("1d+0: ", summary("a párom jógázott, én addig főztem"));
        assertEquals("1d+0: ", summary("a csapat edzett, én sérült voltam"));
        // A közös edzésben viszont benne vagyok: a birtokos ragos alak és az
        // első személyű ige is azt mondja, hogy megtörtént – velem.
        assertEquals("1d+0: 1×futas/30", summary("a fiammal futottam 5 km-t"));
        assertEquals("1d+0: 1×futas/30", summary("a fiam és én futottunk 5 km-t"));
        assertEquals("1d+0: 1×kondi/45", summary("a párom jógázott, én kondiztam 45 percet"));
    }

    @Test public void aRoundCountMultipliesTheDistance() {
        // A „3 kör 400 m" ezerkétszáz méter: eddig a kör-szám elveszett, és a
        // naplóba a táv harmada került.
        assertEquals(1.2, Activities.parse("3 kör 400 m futás").plans.get(0).km, 0.001);
        assertEquals(1.0, Activities.parse("5 kör 200 métert futottam").plans.get(0).km, 0.001);
        // Az órakor NEM szorzó: ott kötőjel áll a szám után, nem szóköz.
        assertEquals(5.0, Activities.parse("6-kor 5 km futás").plans.get(0).km, 0.001);
        assertEquals(10.0, Activities.parse("reggel 7 órakor 10 km-t futottam")
                .plans.get(0).km, 0.001);
    }

    @Test public void theStartOfAHabitIsNotAWorkout() {
        // A „három hónapja kezdtem el edzeni" arról szól, hogy MIÓTA sportol
        // az ember – eddig kilencven nappal ezelőttre bekerült egy negyvenöt
        // perces „egyéb mozgás", ami sosem történt meg.
        assertEquals("1d+0: ", summary("három hónapja kezdtem el edzeni"));
        assertEquals("1d+0: ", summary("két éve kezdtem futni"));
        // A mondat másik fele viszont megmarad, és a mai kezdés is edzés.
        assertEquals("1d+0: 1×futas/240",
                summary("két hete kezdtem el futni, azóta 40 km-t futottam"));
        assertEquals("1d+0: 1×kondi/45", summary("ma elkezdtem edzeni, 45 perc kondi"));
    }

    @Test public void everydayPhysicalWorkCounts() {
        // A kaszálás és a cipekedés ugyanaz a fizikai munka – eddig válasz
        // nélkül maradtak, pedig a napi mozgás jó része ilyen.
        assertEquals("1d+0: 1×munka/90", summary("kaszáltam a kertben 90 percet"));
        assertEquals("1d+0: 1×munka/180",
                summary("átrendeztem a bútorokat, 3 órát cipekedtem"));
        assertEquals("1d+0: 1×munka/40", summary("bepakoltam a kocsit, 40 perc"));
        assertEquals("1d+0: 1×munka/120", summary("metszettem a fákat 2 órát"));
    }

    @Test public void afterAllMeansItDidNotHappen() {
        // A „mégsem" ugyanaz a tagadás, csak megfordított szándékkal – eddig
        // negyvenöt perces futás lett belőle, vagyis pont az, ami elmaradt.
        assertEquals("1d+0: ", summary("mégsem futottam ma"));
        assertEquals("1d+0: ", summary("mégsem mentem el edzeni"));
        // A mondat másik fele viszont megtörtént.
        assertEquals("1d+0: 1×uszas/30", summary("mégsem futottam, de úsztam 30 percet"));
    }

    @Test public void aBreakIsNotAPeriodAndOccasionsCountFromBehind() {
        // A „3 hét UTÁN" a kihagyás hossza, nem időszak: a mai edzés eddig
        // huszonegy napra terült szét, vagyis a mai nap kimaradt a szériából.
        assertEquals("1d+0: 1×futas/30",
                summary("ma volt az első edzésem 3 hét után, 30 perc könnyű futás"));
        assertEquals("1d+0: 1×futas/30", summary("két hét után újra futottam 5 km-t"));
        // Az „N alkalommal" a mozgás MÖGÖTT is darabszám – eddig egyetlen
        // futásként került be, vagyis a hét kétharmada eltűnt.
        assertEquals("7d+0: 3×futas/45", summary("a héten futottam 3 alkalommal"));
        assertEquals("7d+0: 3×futas/45", summary("a héten 3 alkalommal futottam"));
        // A valódi időszak változatlan.
        assertEquals("21d+0: 6×egyeb/45", summary("az elmúlt 3 hétben 6 edzés"));
    }

    @Test public void theDistributivePairWorksWithDistanceToo() {
        // A „reggel és este is futottam 5-5 km-t" két ötkilométeres futás. Az
        // osztó alakot eddig csak az időtartamnál értettük – a napi tíz
        // kilométer fele eltűnt a naplóból, a statisztikából és az XP-ből.
        Activities.Parsed p = Activities.parse("reggel és este is futottam 5-5 km-t");
        assertEquals(1, p.plans.size());
        assertEquals(2, p.plans.get(0).count);
        assertEquals(5, p.plans.get(0).km, 0.001);
        assertEquals("1d+0: 2×futas/30", summary("reggel és este 5-5 km futás"));
        // Az időtartamos alak változatlan.
        assertEquals("1d+0: 2×kondi/30", summary("reggel és este 30-30 perc kondi"));
        // Egy napszak, egy táv: egy edzés.
        assertEquals("1d+0: 1×futas/30", summary("reggel futottam 5 km-t"));
    }

    @Test public void aClockRangeIsADuration() {
        // A naptárból másolt sor: eddig egyetlen szabály sem értette, és a
        // tizenkilenc-harmincból HARMINC darab kilencvenperces foci lett,
        // harminc napra elosztva – negyvenöt óra mozgás egyetlen sorból.
        assertEquals("1d+0: 1×foci/90", summary("18:00-19:30 foci"));
        assertEquals("1d+0: 1×foci/90", summary("18:00 - 19:30 foci"));
        assertEquals("1d+0: 1×futas/45", summary("6:30-7:15 futás"));
        // A magyar rag is ide tartozik – eddig az alapértelmezett hossz jött.
        assertEquals("1d+0: 1×kondi/90", summary("18:00-tól 19:30-ig kondi"));
        // A tempó és az idő-kijelző nem tartomány.
        assertEquals("1d+0: 1×futas/65", summary("futás 1:05:23"));
        assertEquals("1d+0: 1×futas/55", summary("10 km-t futottam 5:30-as tempóval"));
    }

    @Test public void aDateAtTheStartOfTheLineIsADate() {
        // A naplóból kimásolt sor így néz ki. Rag és évszám nélkül eddig nem
        // dátumnak számított, hanem darabszámnak: a „01.15 futás 8 km"-ből
        // TIZENÖT darab nyolckilométeres futás lett, tizenöt napra elosztva –
        // százhúsz kilométer egyetlen sorból.
        Activities.Parsed p = Activities.parse("01.15 futás 8 km");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(1, p.days);
        assertEquals(8, p.plans.get(0).km, 0.001);
        // A tizedespont viszont nem dátum: az „1.5 km futás" másfél kilométer.
        Activities.Parsed q = Activities.parse("1.5 km futás");
        assertEquals(1, q.plans.size());
        assertEquals(1.5, q.plans.get(0).km, 0.001);
        assertEquals(1, q.days);
    }

    @Test public void aHabitIsNotASingleSession() {
        // A „hetente háromszor járok kondiba" a heti rendet írja le, nem egy
        // megtörtént edzést – eddig mindegyikből teljes bejegyzés lett.
        assertEquals("1d+0: ", summary("hetente háromszor járok kondiba"));
        assertEquals("1d+0: ", summary("minden másodnap futok"));
        assertEquals("1d+0: ", summary("szoktam futni reggelente"));
        assertEquals("1d+0: ", summary("általában 5 km-t futok"));
        // A múlt idő viszont napló, gyakorisággal együtt is.
        assertEquals("7d+0: 2×futas/45", summary("hetente kétszer futottam"));
        assertEquals("1d+1: 1×kondi/60", summary("jártam kondiba tegnap"));
        assertEquals("30d+0: 4×joga/45", summary("hetente egyszer jóga egész hónapban"));
    }

    @Test public void anAbilityIsNotALog() {
        // A „fáj a térdem 2 hete, de futni TUDOK" arról szól, mi megy és mi
        // nem – eddig negyvenöt perces futás lett belőle, pont egy sérült
        // térd mellé.
        assertEquals("1d+0: ", summary("fáj a térdem 2 hete, de futni tudok"));
        assertEquals("1d+0: ", summary("már tudok futni 30 percet"));
        assertEquals("1d+0: ", summary("terhesség alatt csak sétálok"));
        // A MÚLT idejű „tudtam" nem képesség, hanem siker: az megtörtént.
        assertEquals("1d+0: 1×futas/30", summary("el tudtam menni futni, 5 km"));
        assertEquals("1d+0: 1×tura/40", summary("sétáltam 40 percet"));
    }

    @Test public void aProgressNoteIsNotAWorkout() {
        // Az „5 km-ről 10 km-re növeltem a távot" a tervről szól, nem egy
        // megtörtént futásról – eddig bejegyzés lett belőle, ráadásul a RÉGI
        // értékkel.
        assertEquals("1d+0: ", summary("5 km-ről 10 km-re növeltem a távot"));
        assertEquals("1d+0: ", summary("45 percről 60 percre nőtt az edzés"));
        // A valódi edzés változatlan.
        assertEquals("1d+0: 1×futas/60", summary("10 km futás"));
    }

    @Test public void theBackDayIsNotSixDays() {
        // A „hát nap:" edzésnap NEVE, nem hat nap: a „hát nap: húzódzkodás,
        // evezés…" hatnapos időszakká vált, és a húzódzkodás hat ismétlést
        // kapott. A kettőspont dönt.
        Activities.Parsed p = Activities.parse("hát nap: húzódzkodás, evezés, lehúzás");
        assertEquals(1, p.days);
        assertEquals(0, StrengthParse.parse("hát nap: húzódzkodás, evezés, lehúzás").size());
        // A valódi hat nap marad hat nap.
        assertEquals(6, Activities.parse("hat nap alatt 6 edzés").days);
    }

    @Test public void duringTrainingIsATimestampToo() {
        // Az „edzés ALATT ittam egy izotóniást" ugyanolyan időpont, mint az
        // „edzés után" – eddig negyvenöt perces bejegyzés lett belőle az ital
        // mellé, holott a mondat az italról szól.
        assertEquals("1d+0: ", summary("edzés alatt 7 dl izotóniás"));
        assertEquals("1d+0: ", summary("edzés során ittam 5 dl vizet"));
        // A mértékegységes „alatt" viszont a futás ideje, nem időpont.
        assertEquals("1d+0: 1×futas/30", summary("30 perc alatt futottam 5 km-t"));
        assertEquals("1d+0: 1×kerekpar/60", summary("1 óra alatt bicikliztem 20 km-t"));
    }

    @Test public void intervalSegmentsAreNotSeparateWorkouts() {
        // A „20 mp sprint 40 mp séta, 12 kör" a futásnak és a sétának is a
        // mozgásforma szokásos hosszát adta: negyvenöt plusz kilencven percet
        // egy tizenkét perces edzésre.
        assertEquals("1d+0: ", summary("20 mp sprint 40 mp séta, 12 kör"));
        // A kimondott idővel megadott mozgás viszont marad.
        assertEquals("1d+0: 1×futas/30", summary("futás 30 perc"));
    }

    @Test public void floorsBecomeMinutes() {
        // A lépcsőzést emeletben mondjuk, nem percben – az app viszont a
        // mozgásforma alapértelmezett hosszát adta hozzá, vagyis húsz
        // emeletből MÁSFÉL ÓRA gyaloglás lett.
        assertEquals("1d+0: 1×tura/10", summary("lépcsőztem 20 emeletet"));
        assertEquals("1d+0: 1×tura/8", summary("15 emeletet lépcsőztem"));
        assertEquals("1d+0: 1×tura/15", summary("30 emelet lépcsőzés"));
        // A lépcsőház nem mozgás.
        assertEquals("1d+0: ", summary("a lépcsőházban találkoztunk"));
    }

    @Test public void theWeekendAdjectiveIsNotAWeek() {
        // A „hétvégi" JELZŐ: a „hétvégi hosszú futás 18 km" tizennyolc
        // kilométere hét napra terült szét, és a heti statisztikában hétszer
        // annyi napnak látszott. A hétvége két nap, nem hét.
        // Rögzített hétköznapi nap (szerda): hétvégén írva a „hétvégi"
        // szándékosan rövidebb időszak (szombaton a ma, vasárnap kettő).
        Activities.Parsed p = Activities.parse("hétvégi hosszú futás 18 km 1:45",
                1_753_869_600_000L);
        assertEquals(2, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(18, p.plans.get(0).km, 0.001);
        // A valódi egyhetes időszak változatlan.
        assertEquals("7d+0: 3×futas/45", summary("a héten futottam 3-szor"));
    }

    @Test public void insteadOfCanCompareJustTheNumber() {
        // A „csak 5 km-t futottam 10 helyett" öt kilométere MEGTÖRTÉNT –
        // eddig az egész mondat eltűnt, mert a tíz kilométer maradt el.
        assertEquals("1d+0: 1×futas/30",
                summary("meleg volt, ezért csak 5 km-t futottam 10 helyett"));
        assertEquals("1d+0: 1×futas/40", summary("futottam 40 percet 60 helyett"));
        // A mozgásformát cserélő alak változatlan: ott a KONDI maradt el.
        assertEquals("1d+0: 1×futas/30", summary("kondi helyett futás 30 perc"));
    }

    @Test public void goalsEntriesAndConditionalsAreNotLogs() {
        // A CÉL nem napló: az „a heti célom 4 edzés" négy megtörtént edzésként
        // került be – a hét elején, amikor még egy sem volt.
        assertEquals("1d+0: ", summary("a heti célom 4 edzés"));
        assertEquals("1d+0: ", summary("célom a 100 kg-os fekvenyomás"));
        // A NEVEZÉS nem futás: a „beneveztem egy félmaratonra" huszonegy
        // kilométert írt a naplóba egy meg sem kezdett versenyről.
        assertEquals("1d+0: ", summary("beneveztem egy félmaratonra"));
        // A FELTÉTELES mondat múlt idejű ige nélkül terv.
        assertEquals("1d+0: ", summary("ha esik, futópadon futok"));
        // …de a múlt idő megvédi a valódi bejegyzést.
        assertEquals("1d+0: 1×futas/30", summary("hazafelé futottam 5 km-t"));
        assertEquals("1d+1: 1×futas/30", summary("ha jól emlékszem, 5 km-t futottam tegnap"));
    }

    @Test public void aSuggestionIsNotALog() {
        // A felszólítás terv: a „csináljunk egy tabatát" javaslat. Az
        // időzítő-terv ilyenkor is elkészül – csak a bejegyzés nem.
        assertEquals("1d+0: ", summary("csináljunk egy tabatát"));
        assertEquals("1d+0: ", summary("menjünk futni"));
        assertEquals("1d+0: ", summary("fussunk egy kört"));
        // A múlt idő viszont megtörtént.
        assertEquals("1d+0: 1×kondi/60", summary("csináltunk egy tabatát"));
    }

    @Test public void aComplaintIsNotAWorkout() {
        // A mozgás neve itt a KÖRÜLMÉNY, nem a napló: a „ropog a térdem
        // guggolásnál" eddig hatvanperces kondi-bejegyzés lett.
        assertEquals("1d+0: ", summary("ropog a térdem guggolásnál, de nem fáj"));
        assertEquals("1d+0: ", summary("edzés közben fájt a bokám"));
        assertEquals("1d+0: ", summary("sportolás közben elpattant valami a vádlimban"));
        // A szám a védőkorlát: ami időtartammal vagy távval van kimondva,
        // az megtörtént – csak fájt utána.
        assertEquals("1d+0: 1×futas/20", summary("20 perc futás után fájt a térdem"));
        assertEquals("1d+0: 1×futas/30", summary("fájt a lábam, de azért lefutottam 5 km-t"));
        assertEquals("1d+0: 1×futas/30", summary("futottam 30 percet, utána sajgott a bokám"));
    }

    @Test public void theUpperBackIsNotTheNumberSix() {
        // Ékezet nélkül a „hát" és a „hat" egybeesik: a „felső hát erősítés"
        // hat darab hatvanperces kondi-bejegyzés lett, hat napra elosztva.
        assertEquals("1d+0: 1×kondi/60", summary("felső hát erősítés"));
        assertEquals("1d+0: ", summary("alsó hát gyakorlatok"));
        // A számnév attól még szám marad, ha tényleg az.
        assertEquals("6d+0: 6×kondi/60", summary("hat kondi edzés"));
    }

    @Test public void stepCountsBecomeAWalk() {
        // A „10000 lépés" túra/gyaloglás: ~130 lépés/perc, ~75 cm/lépés.
        Activities.Plan p = Activities.parse("ma 10000 lépés").plans.get(0);
        assertEquals("tura", p.kind.id);
        assertEquals(1, p.count);
        assertEquals(77, p.minutes);
        assertEquals(7.5, p.km, 0.001);
        // Kiírva is: „tízezer", „10 ezer", „háromezer".
        assertEquals(7.5, Activities.parse("tízezer lépés").plans.get(0).km, 0.001);
        assertEquals(7.5, Activities.parse("10 ezer lépés").plans.get(0).km, 0.001);
        assertEquals(2.3, Activities.parse("háromezer lépést mentem").plans.get(0).km, 0.001);
        // Ha a séta már szerepel, kiegészíti, nem duplázza.
        Activities.Parsed q = Activities.parse("sétáltam 10000 lépést");
        assertEquals(1, q.plans.size());
        assertEquals(7.5, q.plans.get(0).km, 0.001);
        // A lépésszám a tervben is megmarad – a mentés a bejegyzésbe írja.
        assertEquals(10000, q.plans.get(0).steps);
        assertEquals(10000, Activities.parse("ma 10000 lépés").plans.get(0).steps);
        assertEquals(0, Activities.parse("5 km futás").plans.get(0).steps);
        // A kimondott idő erősebb a lépés-becslésnél.
        assertEquals(120, Activities.parse("sétáltam 2 órát, 10000 lépés")
                .plans.get(0).minutes);
        // A pici szám nem lépésszám-edzés (és nem is darabszám).
        assertTrue(Activities.parse("100 lépés").isEmpty());
    }

    @Test public void setsTimesRepsIsTheProduct() {
        // A súlyzós jelölés: „3x10" három sorozat tíz ismétlés, azaz harminc.
        Activities.Parsed p = Activities.parse("3x10 fekvőtámasz");
        assertEquals(1, p.plans.get(0).count);
        assertEquals(6, p.plans.get(0).minutes);       // 30 ismétlés / 5
        assertEquals(20, Activities.parse("5x20 felülés").plans.get(0).minutes);
        assertEquals(20, Activities.parse("10x10 fekvőtámasz").plans.get(0).minutes);
        // A „2x45 perc foci" viszont két meccs marad, nem kilencven ismétlés.
        Activities.Parsed foci = Activities.parse("2x45 perc foci");
        assertEquals(2, foci.plans.get(0).count);
        assertEquals(45, foci.plans.get(0).minutes);
    }

    @Test public void aDateWithAMonthNameIsUnderstood() {
        // 2026. július 31. péntek dél (Budapest).
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(2026, java.util.Calendar.JULY, 31, 12, 0, 0);
        long friday = c.getTimeInMillis();
        assertEquals(3, Activities.parse("július 28-án futottam", friday).offset);
        assertEquals(1, Activities.parse("július 30-án 2 kondi", friday).offset);
        assertEquals(0, Activities.parse("július 31-én 2 kondi", friday).offset);
        // A nap száma nem darabszám: a „3 futás" marad három.
        Activities.Parsed p = Activities.parse("3 futás július 30-án", friday);
        assertEquals(3, p.plans.get(0).count);
        assertEquals(1, p.offset);
        // Ha az idei dátum még nem volt meg, a tavalyi: dec. 24. 219 napja.
        assertEquals(219, Activities.parse("december 24-én síeltem", friday).offset);
        // A puszta hónapnév (nap nélkül) és a lehetetlen nap nem dátum.
        assertEquals(0, Activities.parse("júliusban 10 edzés", friday).offset);
        assertEquals(0, Activities.parse("február 30-án futottam", friday).offset);
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
        // A napok száma itt mellékes: háromnál több alkalom időszak nélkül
        // annyi napra oszlik, ahány alkalom (lásd manyWorkoutsNeedManyDays).
        assertEquals("5d+0: 5×futas/30", summary("5 futás 30 perc"));
        assertEquals("1d+0: 3×kondi/90, 2×futas/40", summary("3 kondi 90 perc és 2 futás 40 perc"));
        assertEquals("1d+0: 1×joga/60", summary("egy óra jóga"));
        assertEquals("1d+0: 2×tenisz/60", summary("2 tenisz 1 óra"));
        // Ahol nincs megadva, a mozgásforma szokásos hossza jön.
        assertEquals("1d+0: 2×kezilabda/90", summary("2 kézilabda"));
    }

    @Test public void colloquialNamesAreUnderstood() {
        assertEquals("6d+0: 6×kezilabda/90", summary("6 kézi"));
        assertEquals("1d+0: 1×kerekpar/60", summary("bringa"));
        assertEquals("1d+0: 2×kondi/60", summary("2 konditerem"));
        assertEquals("1d+0: 1×kosarlabda/60", summary("kosaraztam"));
        assertEquals("1d+0: 2×tura/90", summary("2 séta"));
        // A beszélt alak és az elütés is bringa: enélkül a puszta táv miatt
        // HÚSZ KILOMÉTERES FUTÁS lett belőle, dupla idővel és rossz kalóriával.
        assertEquals("1d+0: 1×kerekpar/60", summary("bicigliztem 20 km"));
        assertEquals("1d+0: 1×kerekpar/60", summary("biciglizteem 20 km"));
        assertEquals("1d+0: 1×kerekpar/60", summary("bicóztam 20 km"));
        // Az elgépelt squash és a magyarul ritkábban leírt sportok.
        assertEquals("1d+0: 1×tenisz/45", summary("sqash 45 perc"));
        assertEquals("1d+0: 1×evezes/60", summary("sup-oztam 1 órát"));
        assertEquals("1d+0: 1×harcmuveszet/75", summary("krav maga edzés 75 perc"));
    }

    @Test public void theDurationCanAlsoComeBeforeTheSport() {
        // Magyarul ez a természetesebb szórend, és eddig CSERÉLŐDÖTT: a futás
        // a kondi idejét kapta meg, a kondi meg az alapértelmezettet – vagyis
        // mindkét bejegyzés hibás lett.
        assertEquals("1d+0: 1×futas/30, 1×kondi/20", summary("30 perc futás, 20 perc kondi"));
        assertEquals("1d+0: 1×futas/30, 1×kondi/20", summary("30 perc futás és 20 perc kondi"));
        assertEquals("1d+0: 1×kondi/60, 1×futas/30", summary("1 óra kondi, 30 perc futás"));
        // A fordított szórend és a vegyes eset is jó marad.
        assertEquals("1d+0: 1×futas/30, 1×kondi/20", summary("futás 30 perc, kondi 20 perc"));
        assertEquals("1d+0: 1×kondi/60, 1×futas/40", summary("kondi 1 óra futás 40 perc"));
        // Egyetlen időtartam továbbra is mindenkire vonatkozik.
        assertEquals("7d+0: 3×futas/40, 2×uszas/40",
                summary("az elmúlt héten 3 futás és 2 úszás, 40 perc"));
    }

    @Test public void eachSportKeepsItsOwnDistance() {
        // A „bicikli 20 km, futás 5 km" húsz kilométerét a FUTÁS vitte el, az
        // ötöt pedig eldobtuk – két rossz bejegyzés egy mondatból.
        assertDistances("bicikli 20 km, futás 5 km", "kerekpar", 20, "futas", 5);
        assertDistances("5 km futás, 20 km bicikli", "futas", 5, "kerekpar", 20);
        // Ragozott igékkel is: itt a szótő rövidebb a szónál („úsztam"), és
        // emiatt cserélődött fel a két táv.
        assertDistances("úsztam 1 km-t, futottam 5 km-t", "uszas", 1, "futas", 5);
        // Ugyanaz a sport kétszer, külön távval: két külön edzés.
        assertDistances("reggel 5 km futás, este 8 km futás", "futas", 5, "futas", 8);
    }

    private static void assertDistances(String q, String k1, double km1,
                                        String k2, double km2) {
        java.util.List<Activities.Plan> p = Activities.parse(q).plans;
        assertEquals(q, 2, p.size());
        assertEquals(q, k1, p.get(0).kind.id);
        assertEquals(q, km1, p.get(0).km, 0.001);
        assertEquals(q, k2, p.get(1).kind.id);
        assertEquals(q, km2, p.get(1).km, 0.001);
    }

    @Test public void twoSportsNeverSwapTheirNumbers() {
        // Generatív őrszem: bármelyik két sportág, bármelyik szórend, és a
        // szám mindig a SAJÁT mozgásához tartozik. Az éjszaka két ilyen hibája
        // is előkerült (idő és táv is cserélődött), és mindkettő csendes volt:
        // a bejegyzés létrejött, csak rossz értékkel.
        String[][] sports = {{"futás", "futas"}, {"kondi", "kondi"},
                {"úszás", "uszas"}, {"kézilabda", "kezilabda"}};
        StringBuilder bad = new StringBuilder();
        for (String[] a : sports)
            for (String[] b : sports) {
                if (a[1].equals(b[1])) continue;
                String[] forms = {
                        "30 perc " + a[0] + ", 20 perc " + b[0],
                        a[0] + " 30 perc, " + b[0] + " 20 perc",
                        "30 perc " + a[0] + " és 20 perc " + b[0],
                        a[0] + " 30 perc és " + b[0] + " 20 perc"};
                for (String q : forms) {
                    java.util.List<Activities.Plan> p = Activities.parse(q).plans;
                    if (p.size() != 2 || !p.get(0).kind.id.equals(a[1])
                            || !p.get(1).kind.id.equals(b[1])
                            || p.get(0).minutes != 30 || p.get(1).minutes != 20)
                        bad.append("\n  ").append(q).append(" -> ").append(dump(p));
                }
            }
        assertEquals("elcsúszott az idő:" + bad, 0, bad.length());
    }

    @Test public void thePresentTenseIsUsuallyAPlan() {
        // A magyar jelen idő gyakran jövőt jelent: az „este megyek edzeni" és
        // a „ha lesz időm, futok" SZÁNDÉK. Eddig mindkettő bekerült a naplóba,
        // a szériába és az XP-be – egy meg sem történt edzés.
        for (String q : new String[]{"ma még megyek futni", "este megyek edzeni",
                "ha lesz időm, futok", "holnap kondi", "jövő héten 3 futás"})
            assertTrue("tervből bejegyzés lett: " + q, Activities.parse(q).isEmpty());
        // A múlt idő ragja más, azt nem érinti.
        assertEquals("1d+0: 1×futas/45", summary("futottam"));
        assertEquals("1d+0: 1×egyeb/45", summary("elmentem edzeni"));
        // A „futok" szándékosan marad futás: a „három kört futok" is futás,
        // és a szótő-teszt is ezt őrzi.
        assertEquals("1d+0: 1×futas/45", summary("majd futok"));
    }

    @Test public void theLoneDurationOnlyAppliesToAllWhenItSumsUp() {
        // Az összefoglaló idő külön tagmondat a felsorolás UTÁN: az mindenkire
        // vonatkozik. A mozgás mögé közvetlenül írt idő viszont csak az övé,
        // és az ELÖL álló szám az első mozgáshoz tartozik.
        assertEquals("7d+0: 3×futas/40, 2×uszas/40",
                summary("az elmúlt héten 3 futás és 2 úszás, 40 perc"));
        // A kondi a saját szokásos hosszát kapja, nem a futásét.
        assertEquals("1d+0: 1×futas/30, 1×kondi/60", summary("30 perc futás és kondi"));
        // És fordítva: a szám a hozzá KÖZELEBBI mozgásé, nem a soron
        // következőé – a futás így az alapértelmezettet kapja.
        assertEquals("1d+0: 1×futas/45, 1×kondi/30", summary("futás és 30 perc kondi"));
    }

    @Test public void twoSportsNeverSwapTheirDistances() {
        // Ugyanaz a táv-oldalon: a „bicikli 20 km, futás 5 km" húsz
        // kilométerét a futás vitte el, az ötöt pedig eldobtuk.
        String[][] sports = {{"futás", "futas"}, {"úszás", "uszas"},
                {"bicikli", "kerekpar"}, {"séta", "tura"}};
        StringBuilder bad = new StringBuilder();
        for (String[] a : sports)
            for (String[] b : sports) {
                if (a[1].equals(b[1])) continue;
                String[] forms = {
                        "10 km " + a[0] + ", 4 km " + b[0],
                        a[0] + " 10 km, " + b[0] + " 4 km",
                        "10 km " + a[0] + " és 4 km " + b[0],
                        a[0] + " 10 km és " + b[0] + " 4 km"};
                for (String q : forms) {
                    java.util.List<Activities.Plan> p = Activities.parse(q).plans;
                    if (p.size() != 2 || !p.get(0).kind.id.equals(a[1])
                            || !p.get(1).kind.id.equals(b[1])
                            || Math.abs(p.get(0).km - 10) > 0.001
                            || Math.abs(p.get(1).km - 4) > 0.001)
                        bad.append("\n  ").append(q).append(" -> ").append(dumpKm(p));
                }
            }
        assertEquals("elcsúszott a táv:" + bad, 0, bad.length());
    }

    private static String dumpKm(java.util.List<Activities.Plan> p) {
        StringBuilder sb = new StringBuilder();
        for (Activities.Plan pl : p)
            sb.append(pl.kind.id).append("/").append(pl.km).append("km ");
        return sb.toString();
    }

    private static String dump(java.util.List<Activities.Plan> p) {
        StringBuilder sb = new StringBuilder();
        for (Activities.Plan pl : p)
            sb.append(pl.kind.id).append("/").append(pl.minutes).append(" ");
        return sb.toString();
    }

    @Test public void manyWorkoutsNeedManyDays() {
        // Húsz edzés EGY napon tizenöt óra mozgás: a napi percek, a széria és
        // a terhelés-figyelés is elszállna tőle. Időszakot nem találunk ki a
        // semmiből – a minimális feltevés az, hogy naponta legfeljebb egy volt.
        assertEquals("20d+0: 20×egyeb/45", summary("20 edzés"));
        assertEquals("10d+0: 10×egyeb/45", summary("10 edzés"));
        // Háromig viszont életszerű egy napon belül is (reggel-este).
        assertEquals("1d+0: 2×egyeb/45", summary("2 edzés"));
        assertEquals("1d+0: 3×egyeb/45", summary("3 edzés"));
        // A kimondott időszak mindig erősebb.
        assertEquals("30d+0: 10×egyeb/45", summary("egy hónap alatt 10 edzés"));
        assertEquals("7d+0: 5×futas/45", summary("5 futás a héten"));
    }

    @Test public void oneWordCannotBecomeTwoActivities() {
        // A „futó edzés” szóban benne van az „edzés” is – abból nem lehet külön
        // „egyéb mozgás”. A puszta „edzés” viszont még menthető tartalékként.
        assertEquals("1d+0: 3×futas/45", summary("3 futó edzés"));
        assertEquals("1d+0: 2×kezilabda/90", summary("2 kézi edzés"));
        assertEquals("7d+0: 4×egyeb/45", summary("a héten 4 edzés"));
    }

    @Test public void plansForTheFutureAreNotLogged() {
        // A „jövő héten 3 futás" terv, nem megtörtént edzés – eddig hét napra
        // visszaosztva, múltként került volna a naplóba.
        assertTrue(Activities.parse("jövő héten 3 futás").isEmpty());
        assertTrue(Activities.parse("holnap futok").isEmpty());
        assertTrue(Activities.parse("holnapután úszni fogok").isEmpty());
        assertTrue(Activities.parse("jövő hónapban elkezdem a kondit").isEmpty());
        assertTrue(Activities.parse("szeretnék futni").isEmpty());
        assertTrue(Activities.parse("3 futást tervezek").isEmpty());
        // A múlt viszont marad: a „tegnap futottam" él.
        assertEquals(1, Activities.parse("tegnap futottam").plans.size());
        // A hibaüzenet meg tudja különböztetni a tervet az értetlenségtől.
        assertTrue(Activities.looksLikeFuture("jövő héten 3 futás"));
        assertTrue(Activities.looksLikeFuture("holnap futok"));
        assertFalse(Activities.looksLikeFuture("tegnap futottam"));
        assertFalse(Activities.looksLikeFuture("semmi értelmes szöveg"));
        assertFalse(Activities.looksLikeFuture(null));
        // A beszélt alakok is tervek: „el kéne menni futni", „meg kell
        // csinálnom a lábnapot", „ha lesz idő, futok egyet". Mindháromból
        // negyvenöt perces edzés került a naplóba – pont abból a mondatból,
        // ami arról szól, hogy MÉG NEM volt meg.
        for (String q : new String[]{"el kéne menni futni",
                "meg kell csinálnom a lábnapot", "ha lesz idő, futok egyet",
                "ha bírom, elmegyek úszni"}) {
            assertTrue(q, Activities.looksLikeFuture(q));
            assertTrue(q, Activities.parse(q).isEmpty());
        }
    }

    /**
     * A kimondott AKARAT sem edzés.
     *
     * Az „erősíteni akarom a bokám" mondatból hatvan perc kondi lett, az
     * „el akarok kezdeni futni"-ból negyvenöt perc futás. Ez a legrosszabb
     * fajta hiba: a napló arról állít valamit, hogy megtörtént, amiről a
     * mondat épp azt mondja, hogy még nem.
     */
    @Test public void wantingToTrainIsNotTraining() {
        for (String q : new String[]{"erősíteni akarom a bokám",
                "erősíteni fogom a vállam", "el akarok kezdeni futni",
                "holnap fogom megcsinálni a lábnapot",
                "le akarom futni a félmaratont", "úszni akarunk a hétvégén"}) {
            assertTrue(q, Activities.looksLikeFuture(q));
            assertTrue(q, Activities.parse(q).isEmpty());
        }
        // A megtörtént edzés változatlan – a múlt idő ragja mást mond.
        assertEquals(1, Activities.parse("tegnap lefutottam a félmaratont").plans.size());
        assertEquals(1, Activities.parse("úsztam a hétvégén").plans.size());
    }

    /**
     * Tizenkilenc szándék-alak öt edzéssel: egyikből sem lehet bejegyzés.
     *
     * A remény, a próbálkozás, a készülés, az elhatározás, a kötelesség és
     * az ígéret mind jövő időben beszél – az app mégis mindegyikből teljes
     * értékű, negyvenöt–hatvan perces edzést csinált.
     */
    @Test public void noFormOfIntentEverBecomesALogEntry() {
        String[] intent = {"szeretnék", "tervezem, hogy", "el kéne mennem", "majd",
                "jó lenne", "akarok", "fogok", "remélem tudok", "megpróbálok",
                "készülök", "elhatároztam, hogy", "muszáj lesz", "kötelező lesz",
                "vágyom rá, hogy", "gondolkodom rajta, hogy", "ígérem, hogy",
                "eldöntöttem, hogy", "talán", "esetleg"};
        String[] core = {"futni 30 percet", "elmenni a konditerembe", "úszni egy órát",
                "biciklizni 20 km-t", "megcsinálni a lábnapot"};
        StringBuilder bad = new StringBuilder();
        for (String p : intent)
            for (String c : core) {
                String q = p + " " + c;
                if (!Activities.parse(q).isEmpty()) bad.append("\n  ").append(q);
            }
        assertEquals("tervből bejegyzés lett:" + bad, 0, bad.length());
        // A „majd" magában viszont NEM jövő idő: főnévi igenév kell mellé.
        // A „futottam, majd úsztam" két megtörtént edzés.
        assertEquals(1, Activities.parse("majd 30 perc kondi").plans.size());
        assertEquals(2, Activities.parse("futottam, majd úsztam").plans.size());
    }

    /**
     * A ház körüli munka is mozgás.
     *
     * Aki három órát ás a kertben, többet mozgott, mint egy fél órás
     * kocogással – a naplóban eddig mégis semmi sem maradt belőle.
     */
    @Test public void workAroundTheHouseCounts() {
        for (String q : new String[]{"kertben dolgoztam 3 órát",
                "ástam a kertben egy órát", "két órát takarítottam",
                "havat lapátoltam egy órát", "füvet nyírtam 40 percet"})
            assertTrue(q, !Activities.parse(q).isEmpty());
        assertEquals("munka", Activities.parse("kertben dolgoztam 3 órát")
                .plans.get(0).kind.id);
        assertEquals(180, Activities.parse("kertben dolgoztam 3 órát")
                .plans.get(0).minutes);
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

    @Test public void compoundSpelledNumbersWorkInWorkouts() {
        // A „negyvenöt perc" eddig ismeretlen számnév volt.
        assertEquals(45, Activities.parse("negyvenöt perc kondi").plans.get(0).minutes);
        assertEquals(32, Activities.parse("harminckét perc futás").plans.get(0).minutes);
        assertEquals(25, Activities.parse("huszonöt perces jóga").plans.get(0).minutes);
        // Ismétlésként is: a huszonöt fekvőtámasz egy alkalom.
        assertEquals(1, Activities.parse("huszonöt fekvőtámasz").plans.get(0).count);
        // A régi alakok nem romolhattak el.
        assertEquals(30, Activities.parse("harminc futás").plans.get(0).count);
        assertEquals(12, Activities.parse("tizenkét perc futás").plans.get(0).minutes);
    }

    @Test public void wholeHoursAndSpelledFractionsCombine() {
        // A „két és fél óra" kettője elveszett: fél óra maradt belőle, a
        // kettes pedig darabszámmá válhatott volna.
        assertEquals("1d+0: 1×tura/150", summary("két és fél óra túra"));
        assertEquals("1d+0: 1×kerekpar/150", summary("2 és fél óra bringa"));
        assertEquals("1d+0: 1×futas/75", summary("egy és negyed óra futás"));
        assertEquals("1d+0: 1×tura/210", summary("három és fél órát túráztunk"));
        // A sima tört és a másfél nem romolhatott el.
        assertEquals("1d+0: 1×futas/30", summary("fél óra futás"));
        assertEquals("1d+0: 1×uszas/90", summary("másfél óra úszás"));
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
        assertEquals("munka", Activities.parse("füvet nyírtam 1 órát").plans.get(0).kind.id);
        // A golfKÖNYÖK panasz, nem sportág – a „golf" tő a nevében ül.
        assertTrue("golfozás lett a panaszból",
                Activities.parse("fáj a golfkönyököm").isEmpty());
        assertTrue("golfozás lett a panaszból",
                Activities.parse("golfkönyök fájdalom").isEmpty());
        assertEquals("egyeb", Activities.parse("golf 3 óra").plans.get(0).kind.id);
        // Ugyanez a teniszkönyökre: a panasz nem teniszezés.
        assertTrue("teniszezés lett a panaszból",
                Activities.parse("fáj a teniszkönyököm").isEmpty());
        assertEquals("tenisz", Activities.parse("tenisz 90 perc").plans.get(0).kind.id);
        assertEquals("munka", Activities.parse("sövényt vágtam 1 óra").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("funkcionális edzés 1 óra").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("bootcamp 45 perc").plans.get(0).kind.id);
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

    @Test public void gymSlangAndNicheSportsAreRecognized() {
        assertEquals("kondi", Activities.parse("tabata 20 perc").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("lábnap volt, 1 óra").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("akadálypálya 40 perc").plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("spartan race futam").plans.get(0).kind.id);
        assertEquals("korcsolya", Activities.parse("curling 2 óra").plans.get(0).kind.id);
        assertEquals("tenisz", Activities.parse("padel 90 perc").plans.get(0).kind.id);
        assertEquals("harcmuveszet",
                Activities.parse("önvédelmi tréning 1 óra").plans.get(0).kind.id);
        assertEquals("harcmuveszet", Activities.parse("vívás edzés").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("kettlebell edzés 30 perc").plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("parkrun szombaton").plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("megmásztuk a Kékestetőt").plans.get(0).kind.id);
        assertEquals("joga", Activities.parse("átmozgattam magam").plans.get(0).kind.id);
        // A „bringatúra" EGY biciklizés, nem bringa + túra.
        Activities.Parsed bt = Activities.parse("bringatúra a Balaton körül");
        assertEquals(1, bt.plans.size());
        assertEquals("kerekpar", bt.plans.get(0).kind.id);
        // A társasjátékok és kocsmasportok viszont nem edzések.
        for (String q : new String[]{"biliárd este", "darts a kocsmában", "sakk verseny"}) {
            Activities.Parsed p = Activities.parse(q);
            assertTrue("edzés lett belőle: " + q, p == null || p.plans.isEmpty());
        }
    }

    @Test public void multipleNamedWeekdaysGetTheirOwnDates() {
        // 2026. július 31. péntek dél (Budapest).
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(2026, java.util.Calendar.JULY, 31, 12, 0, 0);
        long friday = c.getTimeInMillis();

        // „Hétfőn és szerdán kondi": két kondi, pontosan hétfőre és szerdára.
        Activities.Parsed p = Activities.parse("hétfőn és szerdán kondi", friday);
        assertEquals(1, p.plans.size());
        assertEquals(2, p.plans.get(0).count);
        long[] ts = Activities.timestamps(p, friday);
        assertEquals(2, ts.length);
        c.setTimeInMillis(ts[0]);
        assertEquals(27, c.get(java.util.Calendar.DAY_OF_MONTH));
        c.setTimeInMillis(ts[1]);
        assertEquals(29, c.get(java.util.Calendar.DAY_OF_MONTH));

        // Két sport két napnévvel: sorrendben párosítva.
        Activities.Parsed q = Activities.parse("kedden úszás, csütörtökön futás", friday);
        assertEquals("uszas", q.plans.get(0).kind.id);
        assertEquals("futas", q.plans.get(1).kind.id);
        long[] qs = Activities.timestamps(q, friday);
        c.setTimeInMillis(qs[0]);
        assertEquals(28, c.get(java.util.Calendar.DAY_OF_MONTH));
        c.setTimeInMillis(qs[1]);
        assertEquals(30, c.get(java.util.Calendar.DAY_OF_MONTH));

        // Három napnév „1-1"-gyel: három futás, mind a maga napján.
        Activities.Parsed r = Activities.parse(
                "hétfőn, szerdán és pénteken 1-1 futás", friday);
        assertEquals(3, r.plans.get(0).count);
        assertEquals(3, Activities.timestamps(r, friday).length);

        // Egyetlen napnév a régi úton marad.
        assertEquals(3, Activities.parse("kedden 2 kondi", friday).offset);
    }

    @Test public void negatedWorkoutsAreNotLogged() {
        // Ami nem történt meg, az nem kerül a naplóba.
        for (String s : new String[]{"ma nem futottam", "nem edzettem ma",
                "kihagytam a mai edzést", "elmaradt a kondi",
                "sajnos nem tudtam úszni menni", "edzés helyett pihenő",
                "lemondtam a focit"}) {
            Activities.Parsed p = Activities.parse(s);
            assertTrue("edzés lett belőle: " + s, p == null || p.plans.isEmpty());
        }
        // A csere másik fele és a többi tagmondat viszont él.
        assertEquals("futas", Activities.parse("kondi helyett futás").plans.get(0).kind.id);
        assertEquals("tura",
                Activities.parse("ma nem futottam, csak sétáltam").plans.get(0).kind.id);
        assertEquals("uszas",
                Activities.parse("nem volt kondi, de úsztam egy órát").plans.get(0).kind.id);
        // A pihenőnap-felismerés az előnézet barátságos üzenetéhez kell.
        assertTrue(Activities.looksLikeRest("ma nem futottam"));
        assertTrue(Activities.looksLikeRest("pihenőnap volt"));
        assertFalse(Activities.looksLikeRest("tegnap futottam 5 km-t"));
    }

    @Test public void weeklyFrequenciesMultiplyOverTheSpan() {
        // A „hetente kétszer … a hónapban" heti két alkalom × négy hét.
        assertEquals("30d+0: 8×uszas/45",
                summary("hetente kétszer úsztam az elmúlt hónapban"));
        assertEquals("30d+0: 4×joga/45", summary("hetente egyszer jóga egész hónapban"));
        // Időszak nélkül maga a hét az időszak.
        assertEquals("7d+0: 2×futas/45", summary("hetente kétszer futottam"));
        // A „másnaponta" minden MÁSODIK nap – nem minden nap.
        assertEquals("14d+0: 7×uszas/45", summary("másnaponta úszás két hétig"));
        assertEquals("7d+0: 3×futas/45", summary("minden második nap futottam a héten"));
        // Éves lépték: fél évig, egy évig, idén.
        assertEquals("183d+0: 13×futas/45", summary("kéthetente futottam fél évig"));
        assertEquals("365d+0: 12×uszas/45", summary("havonta egyszer úszás egy évig"));
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(2026, java.util.Calendar.JULY, 31, 12, 0, 0);
        assertEquals(212, Activities.parse("idén 40 futás volt", c.getTimeInMillis()).days);
        // Az „idény" viszont nem időszak.
        assertEquals(1, Activities.parse("az idény jól sikerült, futottam",
                c.getTimeInMillis()).days);
    }

    @Test public void everydayMovementCounts() {
        // Nem mindenki „edz": van, aki gyalog megy be, takarít, füvet nyír.
        // Ezek is mozgásformák – a naplóban a helyük a túra és a fizikai munka.
        assertEquals("tura", Activities.parse("gyalog mentem be a munkahelyre 25 perc")
                .plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("babakocsis séta 40 perc").plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("lépcsőztem az irodában").plans.get(0).kind.id);
        assertEquals("munka", Activities.parse("takarítottam 1 órát").plans.get(0).kind.id);
        assertEquals("munka", Activities.parse("fűnyírás 45 perc").plans.get(0).kind.id);
        assertEquals("munka", Activities.parse("költözködés").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("ugrókötél 10 perc").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("trambulin 20 perc").plans.get(0).kind.id);
        assertEquals("joga", Activities.parse("légzőgyakorlat 10 perc").plans.get(0).kind.id);
    }

    @Test public void watchingBuyingAndVenuesAreNotWorkouts() {
        // A nézett meccs, a megrendelt boxzsák és a bérletvásárlás nem edzés,
        // és az ÉTterem sem kondi.
        for (String s : new String[]{"az étteremben vacsoráztunk",
                "foci vb-t néztem a tévében", "boxzsákot rendeltem",
                "jógabérletet vettem", "a foci elmaradt",
                "spinning osztály elmaradt", "kimaradt az edzés",
                "elmarad a foci"}) {
            Activities.Parsed p = Activities.parse(s);
            assertTrue("edzés lett belőle: " + s, p == null || p.plans.isEmpty());
        }
        // Az edzőterem és a részvétel viszont igazi edzés.
        assertEquals("kondi", Activities.parse("edzőteremben gyúrtam").plans.get(0).kind.id);
        assertEquals("joga",
                Activities.parse("részt vettem egy jóga edzésen").plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("meccset néztem, utána futottam 5 km-t")
                .plans.get(0).kind.id);
    }

    @Test public void abbreviatedAndNumericDatesWork() {
        // 2026. július 31. péntek dél (Budapest).
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(2026, java.util.Calendar.JULY, 31, 12, 0, 0);
        long friday = c.getTimeInMillis();
        assertEquals(3, Activities.parse("júl 28-án futottam", friday).offset);
        assertEquals(1, Activities.parse("júl. 30-án kondi", friday).offset);
        assertEquals(3, Activities.parse("07.28-án futottam", friday).offset);
        Activities.Parsed p = Activities.parse("2026.07.28 futás", friday);
        assertEquals(3, p.offset);
        assertEquals(1, p.plans.get(0).count);   // a 28 nem darabszám!
        // A tizedespont nem dátum: az „1.5 km" nem január 5-e.
        assertEquals(0, Activities.parse("1.5 km futás", friday).offset);
        // A „majd" nem május: rag/ szóhatár nélkül a rövidítés nem él.
        assertEquals(0, Activities.parse("majd 30 perc futás", friday).offset);
        // A „március óta" a hónap 1-jétől máig tartó időszak.
        assertEquals(153, Activities.parse("március óta 40 edzés", friday).days);
        // Az „amióta" nem időszak.
        assertEquals(1, Activities.parse("amióta futok, jobb a kedvem", friday).days);
    }

    @Test public void dailyAmountsAndIntervalDistancesAreUnderstood() {
        // A „napi 20 perc" naponta értendő – a héten ez hét alkalom.
        assertEquals("7d+0: 7×joga/20", summary("napi 20 perc jóga egész héten"));
        // Az intervall-jelölés össztáv, EGY edzésként: 6x1 km = 6 km.
        assertEquals("1d+0: 1×futas/36", summary("6x1 km iramfutás"));
        assertEquals("1d+0: 1×futas/19", summary("intervall: 8x400 méter"));
        // Nem hat-tíz külön alkalom!
        assertEquals(1, Activities.parse("10x100 méter úszás").plans.get(0).count);
        // A RAGOZOTT mértékegység is összevonódik: enélkül a „10x400 métert"
        // tíz külön edzés lett, az „5x1000 métert" pedig egyetlen kilométer.
        assertEquals(1, Activities.parse("10x400 métert futottam").plans.get(0).count);
        assertEquals(4.0, Activities.parse("10x400 métert futottam").plans.get(0).km, 0.01);
        assertEquals(5.0, Activities.parse("5x1000 métert").plans.get(0).km, 0.01);
        assertEquals(1, Activities.parse("5x1000 métert").plans.get(0).count);
    }

    /**
     * Az úszók hosszban mérnek: „40 hosszt úsztam" ezer méter.
     *
     * A medence MÉRETE viszont nem megtett táv – a „25 méteres medencében"
     * jelzője sosem az edzés távja.
     */
    @Test public void poolLengthsBecomeMeters() {
        assertEquals(1.0, Activities.parse("40 hosszt úsztam").plans.get(0).km, 0.01);
        assertEquals(1.5, Activities.parse("leúsztam 60 hosszt").plans.get(0).km, 0.01);
        assertEquals(0.5, Activities.parse("20 hosszt a 25 méteres medencében")
                .plans.get(0).km, 0.01);
        assertEquals("uszas", Activities.parse("30 hossz az úszómedencében").plans.get(0).kind.id);
        // Úszó szó nélkül a „hossz" bármi lehet – nem lesz belőle táv.
        assertEquals(0, Activities.parse("40 hossz futás").plans.get(0).km, 0.01);
        // A medenceemelés a súlyzós oldalé marad.
        assertEquals("Csípőemelés", StrengthParse.parse("medence emelés 3x10 60 kg").get(0).name);
    }

    /** A „három negyed óra" háromnegyed óra – nem három negyedórás edzés. */
    @Test public void threeQuartersOfAnHourIsOneWorkout() {
        Activities.Parsed p = Activities.parse("három negyed óra kondi");
        assertEquals(1, p.plans.get(0).count);
        assertEquals(45, p.plans.get(0).minutes);
        Activities.Parsed d = Activities.parse("3 negyed óra futás");
        assertEquals(1, d.plans.get(0).count);
        assertEquals(45, d.plans.get(0).minutes);
        // A valódi szorzószám marad szorzószám.
        assertEquals(3, Activities.parse("három edzés a héten").plans.get(0).count);
        assertEquals(180, Activities.parse("három óra túra").plans.get(0).minutes);
        // A súlyzós „3x10" viszont marad sorozat×ismétlés.
        assertEquals(1, Activities.parse("3x10 guggolás").plans.get(0).count);
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
                "a héten minden nap futottam",
                "hétvégén 1-1 túra",
                "tegnap este kondi",
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

    @Test public void theShortTimeNotationIsUnderstood() {
        // Az órák-appok és a chat rövidítése: „1h20”, „2h”, „45p”.
        assertEquals(80, Activities.parse("1h20 futás").plans.get(0).minutes);
        assertEquals(80, Activities.parse("1h20m futás").plans.get(0).minutes);
        assertEquals(60, Activities.parse("1h futás").plans.get(0).minutes);
        assertEquals(120, Activities.parse("2h bringa").plans.get(0).minutes);
        assertEquals(90, Activities.parse("futás 1h30").plans.get(0).minutes);
        assertEquals(20, Activities.parse("20p futás").plans.get(0).minutes);
    }

    @Test public void theShortTimeIsNotACount() {
        // Ez volt a valódi kár: az „1h20 futás” HÚSZ futássá vált, mert a 20
        // darabszámnak látszott – egy mondatból húsz naplóbejegyzés lett.
        assertEquals(1, Activities.parse("1h20 futás").plans.get(0).count);
        assertEquals(1, Activities.parse("2h30 kondi").plans.get(0).count);
    }

    @Test public void theShortNotationDoesNotEatOtherNumbers() {
        // A magában álló „m” méter, nem perc – különben az úszás távja veszne el.
        assertEquals(1.5, Activities.parse("1500 m úszás").plans.get(0).km, 0.001);
        // Betű után nem rövidítés: a „3 hét” időszak, a „2 hónap” is.
        assertEquals(21, Activities.parse("3 héten át futottam").days);
        assertEquals(45, Activities.parse("2 hónap alatt futás").plans.get(0).minutes);
        // A kiírt „perc” a régi úton megy tovább.
        assertEquals(45, Activities.parse("45 perc futás").plans.get(0).minutes);
    }

    @Test public void stretchingIsRecognisedAsAVerbToo() {
        // A „nyújtás” szótő szerepelt, az ige nem: a „nyújtottam” semmi volt.
        assertEquals("joga", Activities.parse("nyújtottam 15 percet").plans.get(0).kind.id);
        assertEquals(15, Activities.parse("negyed órát nyújtottam").plans.get(0).minutes);
    }

    @Test public void theMultiplierMayFollowTheActivity() {
        // „futottam háromszor" – magyarul ez a természetesebb szórend, és
        // eddig némán elveszett: három futásból egy lett a naplóban.
        assertEquals(3, Activities.parse("futottam háromszor a héten").plans.get(0).count);
        assertEquals(2, Activities.parse("úsztam kétszer").plans.get(0).count);
        assertEquals(3, Activities.parse("futottam 3-szor").plans.get(0).count);
        assertEquals(3, Activities.parse("kondiztam 3x a héten").plans.get(0).count);
        assertEquals(2, Activities.parse("a héten kézilabda kétszer").plans.get(0).count);
        // A tartalék ágon is: itt nincs felismert sportág, csak „edzés".
        assertEquals(4, Activities.parse("a héten edzettem négyszer").plans.get(0).count);
    }

    /**
     * Az osztó szám mértékegységgel TÁV, nem darabszám.
     *
     * Az „a héten kétszer futottam 5-5 km-t" mondatból TIZENNÉGY futás lett:
     * a kétszerest a hét napjaival is felszorozta a naponkénti szabály. Az
     * időzítő ráadásul lecsapott rá – az „5-5" munka/pihenő párnak látszott –,
     * és a mondat kétköros ötmásodperces tervként indult volna el.
     */
    @Test public void aDistributiveWithAUnitIsNotACount() {
        Activities.Parsed p = Activities.parse("a héten kétszer futottam 5-5 km-t");
        assertEquals(1, p.plans.size());
        assertEquals(2, p.plans.get(0).count);
        assertEquals(5, p.plans.get(0).km, 0.01);
        assertEquals(7, p.days);
        assertNull(IntervalParse.parse("a héten kétszer futottam 5-5 km-t"));
        // A darabszámot mondó osztó alak változatlan.
        assertEquals(2, Activities.parse("tegnap és ma 1-1 futás").plans.get(0).count);
        assertEquals(7, Activities.parse("a héten minden nap futottam").plans.get(0).count);
        // És a valódi ritmus is megmarad.
        assertEquals(10, IntervalParse.parse("10 kör 30-30 mp").rounds);
        assertEquals(6, IntervalParse.parse("45-15 x 6").rounds);
    }

    /**
     * Az „alkalom" csak SZÁMMAL edzés.
     *
     * Magában a leghétköznapibb magyar főnév: a „születésnapi alkalomból
     * tortát ettem" mondatból eddig negyvenöt perc mozgás lett – és mivel az
     * edzés felismerője az étkezés ELÉ áll az útbaigazítóban, a torta el is
     * veszett mellőle.
     */
    @Test public void anOccasionIsOnlyATrainingWithANumber() {
        assertTrue(Activities.parse("születésnapi alkalomból tortát ettem").isEmpty());
        assertTrue(Activities.parse("ebből az alkalomból pezsgőt ittunk").isEmpty());
        assertEquals(Sentence.Kind.MEAL, Sentence.of("születésnapi alkalomból tortát ettem",
                java.util.Arrays.asList(Foods.ALL), 1_753_869_600_000L));
        // Számmal viszont továbbra is edzés.
        assertEquals(3, Activities.parse("3 alkalom a héten").plans.get(0).count);
        assertEquals(3, Activities.parse("három alkalom").plans.get(0).count);
        assertEquals(2, Activities.parse("két alkalommal futottam").plans.get(0).count);
    }

    /**
     * Elgépelt mozgásformára tipp jár.
     *
     * A „nem lettem okos" üzenet igaz, de személytelen: ha a nevet csak
     * elgépelte, a leghasznosabb, amit mondhatunk, hogy MELYIKRE gondolhatott.
     * A szabály itt is szigorú – a rossz tipp bosszantóbb, mint a semmi.
     */
    @Test public void typosInSportNamesGetASuggestion() {
        assertEquals("kerekpar", Activities.closestKind("kerekpr 40 perc").id);
        assertEquals("kosarlabda", Activities.closestKind("kosárlabd").id);
        assertEquals("kondi", Activities.closestKind("konditerm").id);
        for (String q : new String[]{"asztal", "valami", "szeretem", "csirkemell", ""})
            assertNull(q, Activities.closestKind(q));
        assertNull(Activities.closestKind(null));
    }

    /**
     * Az „edzés UTÁN" nem edzés, hanem IDŐPONT.
     *
     * Az „edzés után ittam egy fehérjeturmixot" mondatból negyvenöt perc egyéb
     * mozgás lett – és mivel az edzés-felismerő az étkezés ELÉ áll az
     * útbaigazítóban, a turmix el is veszett mellőle: egyszerre került be egy
     * nem létező edzés és maradt ki egy valódi étkezés.
     */
    @Test public void beforeAndAfterTrainingIsATimePhrase() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"edzés után ittam egy fehérjeturmixot",
                "edzés előtt ettem egy banánt", "edzés közben ittam egy izotóniást",
                "edzés után túró rudi"}) {
            assertTrue(q, Activities.parse(q).isEmpty());
            assertEquals(q, Sentence.Kind.MEAL, Sentence.of(q, all, 1_753_869_600_000L));
        }
        // Ugyanez a SPORTNEVEKRE is: a „futás után turmix" a turmixról szól.
        for (String q : new String[]{"futás után turmix", "úszás után szendvics",
                "bringázás után sör", "jóga előtt tea", "futás előtt ettem egy banánt"})
            assertTrue(q, Activities.parse(q).isEmpty());
        // Kimondott számmal viszont a futás valódi – azt a szám hitelesíti.
        assertEquals(60, Activities.parse("60 perc futás után ittam egy turmixot")
                .plans.get(0).minutes);
        assertEquals(10, Activities.parse("10 km futás után szendvics")
                .plans.get(0).km, 0.01);
        // És ami a mondatban tényleg megtörtént, az megmarad.
        assertEquals("joga", Activities.parse("ma futás után jóga").plans.get(0).kind.id);
        // A valódi edzés-mondat érintetlen.
        assertEquals(45, Activities.parse("edzés 45 perc").plans.get(0).minutes);
        assertEquals(60, Activities.parse("edzettem 1 órát").plans.get(0).minutes);
        assertEquals("joga", Activities.parse("edzés után 45 perc nyújtás")
                .plans.get(0).kind.id);
    }

    /**
     * Az alvás órái nem edzés-percek.
     *
     * Az „aludtam 8 órát, reggel futottam 5 km-t" nyolc órája az éjszakáé,
     * mégis a futás hosszává vált: NYOLCÓRÁS futás került a naplóba, a hozzá
     * tartozó kalóriával és heti terheléssel együtt. A bemelegítés-szabálytól
     * abban tér el, hogy itt nincs darabszám-feltétel – az alvás akkor sem
     * edzésidő, ha ez az egyetlen időtartam a mondatban.
     */
    @Test public void sleepHoursAreNotTrainingMinutes() {
        Activities.Parsed p = Activities.parse("aludtam 8 órát, reggel futottam 5 km-t");
        assertEquals(1, p.plans.size());
        assertEquals(5, p.plans.get(0).km, 0.01);
        assertTrue("nyolcórás futás lett belőle", p.plans.get(0).minutes <= 60);
        // A valódi időtartam megmarad.
        assertEquals(45, Activities.parse("aludtam 8 órát, 45 perc kondi")
                .plans.get(0).minutes);
    }

    /**
     * A tempó perc/km, nem perc.
     *
     * A „futás 5 km 24:59 tempó 5:00" – ahogy az órád exportálja – ötszáz
     * perces futássá vált: az ötös tempó-szám lett az edzés hossza. Rosszabb,
     * hogy közben a VALÓDI huszonöt perc is kiesett, mert a tempó-szó
     * ránézett a mellette álló számra is.
     */
    @Test public void paceIsNotDuration() {
        assertEquals(25, Activities.parse("futás 5 km 24:59 tempó 5:00")
                .plans.get(0).minutes);
        assertEquals(25, Activities.parse("futás 5 km 24:59 tempó 5:00/km")
                .plans.get(0).minutes);
        assertEquals(53, Activities.parse("futás 10 km 52:30 tempó 5:15")
                .plans.get(0).minutes);
        // Ami perccel van kiírva, az sosem tempó.
        assertEquals(30, Activities.parse("tempó 30 perc kondi").plans.get(0).minutes);
        assertEquals(25, Activities.parse("futás 5 km 25 perc tempó 5:00")
                .plans.get(0).minutes);
    }

    /**
     * SUP és szinkronúszás: két hiányzó vizes sport.
     *
     * Az álló evezés (SUP) a Balatonon a legnépszerűbb vizes sport, és egyik
     * írásmódját sem ismertük. A puszta „sup" szándékosan NEM szótő: a
     * supermarket, a support és a supervisor is azzal kezdődik.
     */
    @Test public void paddleboardingAndSyncSwimmingAreKnown() {
        assertEquals("evezes", Activities.parse("supoztam 1 órát").plans.get(0).kind.id);
        assertEquals("evezes", Activities.parse("szupozás 90 perc").plans.get(0).kind.id);
        assertEquals("evezes", Activities.parse("paddleboard 1 óra").plans.get(0).kind.id);
        assertEquals("evezes", Activities.parse("álló evezés 45 perc").plans.get(0).kind.id);
        assertEquals("uszas", Activities.parse("szinkronúszás 1 óra").plans.get(0).kind.id);
        for (String q : new String[]{"supermarket", "support", "supervisor"})
            assertTrue(q, Activities.parse(q).isEmpty());
    }

    /**
     * Minden sport-szótő minden ragozott alakja megtalálja a saját mozgását.
     *
     * Ugyanaz a söprés, ami az ételeknél kimutatta, hogy a szó-belseji
     * tiltások a valódi szavakat is elnyelik. Itt tizennyolc gyakori rag ×
     * minden szótő fut le – ha egy új maszk mellékhatást okoz, itt derül ki.
     *
     * A négy tudott kivétel: a „teremt" és a „teremtől" a TEREMTÉS miatt
     * maszkolt, a „lépcsőzős" és a „lépcsőzöm" pedig a tő magánhangzó-
     * illeszkedése miatt marad ki.
     */
    @Test public void everySportStemSurvivesItsInflections() {
        java.util.Set<String> known = new java.util.HashSet<>(java.util.Arrays.asList(
                "lepcsozos", "lepcsozom", "teremt", "teremtol"));
        String[] suf = {"", "t", "ba", "bol", "ban", "val", "hoz", "nak", "n",
                "ra", "rol", "tol", "nal", "os", "as", "es", "om", "unk"};
        StringBuilder bad = new StringBuilder();
        for (Activities.Kind k : Activities.ALL)
            for (String w : k.words) {
                if (w.indexOf(' ') >= 0) continue;
                for (String x : suf) {
                    if (known.contains(w + x)) continue;
                    boolean ok = false;
                    for (Activities.Plan p : Activities.parse(w + x + " 30 perc").plans)
                        if (p.kind.id.equals(k.id)) ok = true;
                    if (!ok) bad.append("\n  ").append(w).append(x).append(" (")
                            .append(k.id).append(")");
                }
            }
        assertEquals("elveszett ragozott sport-alak:" + bad, 0, bad.length());
    }

    @Test public void theFollowingActivityKeepsItsOwnMultiplier() {
        // A „kétszer" az úszásé, nem a túráé – az úszás saját darabszámként
        // már megtalálta, tehát a túra nem veheti el.
        Activities.Parsed p = Activities.parse("hétvégén 1-1 túra és kétszer úsztam");
        assertEquals(1, p.plans.get(0).count);
        assertEquals(2, p.plans.get(1).count);
        // Fordítva viszont az elsőé: a futás egyese nem szorzószám.
        Activities.Parsed q = Activities.parse("úsztam kétszer és futottam egyszer");
        assertEquals(2, q.plans.get(0).count);
        assertEquals(1, q.plans.get(1).count);
    }

    @Test public void crossCountrySkiingKeepsItsDistanceAndVerb() {
        // A „sífutottam" a futás tövét is tartalmazza, de a sífutás MET-je a
        // síé (6,0), nem a futásé (9,8) – másfélszeres kalóriát írnánk.
        assertEquals("si", Activities.parse("sífutottam 15 km-t").plans.get(0).kind.id);
        assertEquals("si", Activities.parse("sífutás 15 km").plans.get(0).kind.id);
        // A sífutás táv-alapú: a km nem esik ki.
        assertEquals(15.0, Activities.parse("sífutás 15 km").plans.get(0).km, 0.001);
        // A rendes futás és az igekötős alakok érintetlenek.
        assertEquals("futas", Activities.parse("elfutottam 5 km-t").plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("kifutottam magam").plans.get(0).kind.id);
    }

    @Test public void anAndAfterTheNegationOpensANewStatement() {
        // A „nem futottam és kondiztam" kondija megtörtént – eddig a tagadás
        // az „és" utáni edzést is elvitte, vagyis egy valódi edzés hiányzott a
        // szériából és a statisztikából.
        assertEquals("kondi",
                Activities.parse("nem futottam és kondiztam").plans.get(0).kind.id);
        assertEquals("kondi",
                Activities.parse("nem futottam, de kondiztam").plans.get(0).kind.id);
        // Ha a másik fele is tagadva van, marad a semmi.
        assertTrue(Activities.parse("nem futottam és nem úsztam").isEmpty());
        assertTrue(Activities.parse("ma nem futottam").isEmpty());
        // A „helyett" ága érintetlen.
        assertEquals("futas", Activities.parse("kondi helyett futás").plans.get(0).kind.id);
    }

    @Test public void theNegationSparesTheAccompaniment() {
        // A „nem futottam a kondi mellett" kondija megtörtént: csak a futás
        // maradt el. A jelző előtt álló szó a kísérő, nem a tagadás tárgya.
        assertEquals("kondi",
                Activities.parse("nem futottam a kondi mellett").plans.get(0).kind.id);
        assertEquals("uszas",
                Activities.parse("nem futottam az úszás mellett").plans.get(0).kind.id);
        // Jelző nélkül a tagadás továbbra is mindent elvisz.
        assertTrue(Activities.parse("nem futottam a kondiban").isEmpty());
    }

    @Test public void twoRunsWithTheirOwnDistanceAreTwoSessions() {
        // A „reggel 5 km futás, este 8 km futás" nyolc kilométere eddig némán
        // elveszett: egy mozgásforma egyszer szerepelhetett, és a második
        // futás egyszerűen kimaradt.
        Activities.Parsed p = Activities.parse("reggel 5 km futás, este 8 km futás");
        assertEquals(2, p.plans.size());
        assertEquals(5.0, p.plans.get(0).km, 0.001);
        assertEquals(8.0, p.plans.get(1).km, 0.001);
    }

    @Test public void oneRunMentionedTwiceStaysOneRun() {
        // A „lefutottam a maratont" kétszer említi a futást, de egy futás volt:
        // a második említésnek nincs SAJÁT, eltérő távja.
        Activities.Parsed m = Activities.parse("lefutottam a maratont");
        assertEquals(1, m.plans.size());
        assertEquals(42.2, m.plans.get(0).km, 0.001);
        // A részlet sem külön edzés.
        Activities.Parsed r = Activities.parse("futottam 10 km-t, ebből 5 km tempó");
        assertEquals(1, r.plans.size());
        assertEquals(10.0, r.plans.get(0).km, 0.001);
        // Azonos táv kétszer: óvatosak vagyunk, marad egy.
        assertEquals(1, Activities.parse("reggel 5 km futás, este 5 km futás")
                .plans.size());
        // Táv nélküli második említés sem nyit új edzést.
        assertEquals(1, Activities.parse("futottam, aztán még futottam").plans.size());
    }

    @Test public void seventyIsANumberNotAWeek() {
        // A „hetven" a „hét" szótövet tartalmazza: a mondat egyhetes
        // időszakká vált, és közben a hetvenöt perc is elveszett.
        assertEquals(75, Activities.parse("hetvenöt perc kondi").plans.get(0).minutes);
        assertEquals(70, Activities.parse("hetven perc kondi").plans.get(0).minutes);
        assertEquals(72, Activities.parse("hetvenkét perc futás").plans.get(0).minutes);
        // A „hét" magában viszont marad időszak – ott a kétértelműség valódi.
        assertEquals(7, Activities.parse("egy hét alatt 3 futás").days);
        assertEquals(7, Activities.parse("a héten kétszer edzettem").days);
    }

    @Test public void theSpokenHundredsAreCounts() {
        // „száz fekvőtámasz": az ismétlésszámok itt laknak, és eddig egyszerűen
        // nem voltak számok.
        assertEquals(20, Activities.parse("száz fekvőtámasz").plans.get(0).minutes);
        assertEquals(20, Activities.parse("100 fekvőtámasz").plans.get(0).minutes);
        assertEquals(30, Activities.parse("százötven fekvőtámasz").plans.get(0).minutes);
        assertEquals(40, Activities.parse("kétszáz felülés").plans.get(0).minutes);
    }

    @Test public void aSpokenDistanceCounts() {
        // „huszonöt kilométer bringa": a táv eddig elveszett, mert csak
        // számjegyet kerestünk az egység előtt.
        assertEquals(25.0, Activities.parse("huszonöt kilométer bringa")
                .plans.get(0).km, 0.001);
        assertEquals(10.0, Activities.parse("tíz kilométert futottam")
                .plans.get(0).km, 0.001);
        assertEquals(2.0, Activities.parse("két kilométer úszás").plans.get(0).km, 0.001);
        // A számjegyes alak változatlan.
        assertEquals(5.0, Activities.parse("5 km futás").plans.get(0).km, 0.001);
    }

    /**
     * Hetvennégy mindennapi sportnévvel végigpróbálva ez a három hiányzott.
     *
     * A triatlon szándékosan saját tétel: futásként a neve hazudna, egyéb
     * mozgásként a terhelése. A darts viszont marad kocsmasport.
     */
    @Test public void theNewlyAddedSportsAreRecognized() {
        assertEquals("egyeb", Activities.parse("íjászat 45 perc").plans.get(0).kind.id);
        assertEquals("evezes", Activities.parse("sárkányhajó 1 óra").plans.get(0).kind.id);
        assertEquals("triatlon", Activities.parse("triatlon 2 óra").plans.get(0).kind.id);
        assertEquals("triatlon", Activities.parse("duatlon verseny").plans.get(0).kind.id);
        assertEquals("triatlon", Activities.parse("ironman").plans.get(0).kind.id);
        // A táv is számít: az olimpiai táv nagyjából két és fél óra.
        assertEquals(150, Activities.parse("triatlon 51,5 km").plans.get(0).minutes, 5);
        assertTrue(Activities.parse("darts a kocsmában").isEmpty());
    }

    /**
     * Százhúsz sportnév végigpróbálva: ezek hiányoztak.
     *
     * A terepkerékpár angolul él a hazai szóhasználatban („mtb", „mountain
     * bike", „gravel"), az atlétika dobó- és ugrószámai pedig egyáltalán nem
     * léteztek – pedig egy atlétika-edzés ugyanúgy másfél óra, mint bármi más.
     */
    @Test public void theSecondSweepOfSportNames() {
        String[][] want = {
                {"mountain bike 1 óra", "kerekpar"}, {"mtb 90 perc", "kerekpar"},
                {"gravel 2 óra", "kerekpar"}, {"pole dance 1 óra", "tanc"},
                {"taekwondo edzés", "harcmuveszet"}, {"atlétika 90 perc", "egyeb"},
                {"magasugrás edzés", "egyeb"}, {"súlylökés 1 óra", "egyeb"},
                {"gerelyhajítás edzés", "egyeb"}, {"aquatlon verseny", "triatlon"},
                {"kitesurf 2 óra", "uszas"}, {"snorkeling 1 óra", "uszas"}};
        StringBuilder bad = new StringBuilder();
        for (String[] w : want) {
            Activities.Parsed p = Activities.parse(w[0]);
            String got = p.isEmpty() ? "-" : p.plans.get(0).kind.id;
            if (!got.equals(w[1]))
                bad.append("\n  ").append(w[0]).append(" -> ").append(got)
                   .append(" (várt: ").append(w[1]).append(")");
        }
        assertEquals("hiányzó vagy rossz mozgásforma:" + bad, 0, bad.length());
    }

    /**
     * A „km/h" sebesség, nem táv.
     *
     * A „futás 28 km/h" huszonnyolc kilométeres futásnak számított – majdnem
     * három óra került volna a naplóba egy tempó-adat miatt. A valódi táv
     * mellett álló sebesség viszont nem zavarhatja a távot.
     */
    @Test public void speedIsNotDistance() {
        Activities.Parsed p = Activities.parse("futás 28 km/h");
        assertEquals(1, p.plans.size());
        assertEquals(0, p.plans.get(0).km, 0.01);
        assertEquals(0, Activities.parse("bringáztam 25 km/h átlaggal").plans.get(0).km, 0.01);
        assertEquals(0, Activities.parse("futás 28 kmh").plans.get(0).km, 0.01);
        // A táv marad, ha tényleg ott van.
        assertEquals(45, Activities.parse("bringa 45 km 27 km/h átlaggal").plans.get(0).km, 0.01);
        assertEquals(20, Activities.parse("20 km bringa").plans.get(0).km, 0.01);
    }

    /**
     * Verseny-idő két taggal: „5 km 22:30", „félmaraton 1:58".
     *
     * A futók írásmódja: tíz alatti első tag óra:perc, fölötte perc:mp. Csak
     * táv mellett él – e nélkül a kettőspontos szám a falon lévő órát
     * jelentheti. A „-kor", a napszak és a tempó („5:30-as tempóval")
     * továbbra sem időtartam.
     */
    @Test public void twoPartRaceTimesAreDurations() {
        assertEquals(23, Activities.parse("5 km 22:30").plans.get(0).minutes);
        assertEquals(42, Activities.parse("10 km PB 42:10").plans.get(0).minutes);
        assertEquals(118, Activities.parse("félmaraton 1:58").plans.get(0).minutes);
        assertEquals(33, Activities.parse("leúsztam 1500 m-t 32:40 alatt").plans.get(0).minutes);
        // Óra a falon, nem időtartam:
        assertEquals(60, Activities.parse("10 km-t futottam 18:30-kor").plans.get(0).minutes);
        assertEquals(60, Activities.parse("10 km reggel 7:30").plans.get(0).minutes);
        assertEquals(55, Activities.parse("10 km-t futottam 5:30-as tempóval").plans.get(0).minutes);
    }

    /**
     * A „8x 60 km" nem intervallum.
     *
     * Az intervall-ismétlés rövid: kétszáz métertől néhány kilométerig. A
     * szorzat viszont vakon összeszorzott, és a „8x 60 km"-ből
     * négyszáznyolcvan kilométeres futás lett, huszonnégy órás becsült
     * idővel. Egymillió véletlenül összerakott mondatban ez a két eset
     * maradt – a többi felismerő nulla hibával ment végig.
     */
    @Test public void anIntervalRepIsShort() {
        assertEquals(60, Activities.parse("8x 60 km").plans.get(0).km, 0.01);
        assertEquals(4, Activities.parse("10x400 métert futottam").plans.get(0).km, 0.01);
        assertEquals(6, Activities.parse("6x1 km").plans.get(0).km, 0.01);
        assertEquals(8, Activities.parse("4x2 km futás").plans.get(0).km, 0.01);
        assertEquals(15, Activities.parse("5x 3 km").plans.get(0).km, 0.01);
        for (Activities.Plan p : Activities.parse("8x 60 km").plans)
            assertTrue("életszerűtlen táv: " + p.km, p.km <= 400);
    }

    /**
     * A „k" rövidítés és a szóközös ezres tagolás.
     *
     * A „10k" nem mindig ugyanaz: lépésnél tízezer LÉPÉS, futásnál tíz
     * KILOMÉTER – és eddig egyik sem létezett. A „10 000 lépés" pedig
     * egyszerűen két számnak látszott, és a mondat mindkettőt eldobta.
     */
    @Test public void theKShorthandAndSpacedThousands() {
        assertEquals(7.5, Activities.parse("10k lépés").plans.get(0).km, 0.01);
        assertEquals(7.5, Activities.parse("10 000 lépés").plans.get(0).km, 0.01);
        assertEquals(9.0, Activities.parse("12k lépés ma").plans.get(0).km, 0.01);
        assertEquals(10.0, Activities.parse("10k futás").plans.get(0).km, 0.01);
        assertEquals(21.0, Activities.parse("21k futás").plans.get(0).km, 0.01);
        assertEquals(2.0, Activities.parse("2k úszás").plans.get(0).km, 0.01);
        // A kiírt alakok változatlanok.
        assertEquals(7.5, Activities.parse("10000 lépés").plans.get(0).km, 0.01);
        assertEquals(5.0, Activities.parse("5 km futás").plans.get(0).km, 0.01);
        assertEquals(30, Activities.parse("30 perc futás").plans.get(0).minutes);
        assertEquals(75, Activities.parse("hétfőn 1 óra 15 perc kondi").plans.get(0).minutes);
    }

    /**
     * Ami NEM az én mozgásom, az nem az én naplóm.
     *
     * A „home office, alig mozogtam" negyvenöt perces „egyéb mozgás" lett, „a
     * gyerek edzésén voltam" szintén – pedig az egyik pont az ellenkezőjét
     * mondja, a másik meg valaki másról szól. Az „alig" csak a mozgás-igével
     * együtt tagadás: az „alig bírtam végigcsinálni a 30 perc futást"
     * megtörtént edzés, csak nehéz volt.
     */
    @Test public void someoneElsesTrainingAndNotMovingAreNotEntries() {
        assertTrue(Activities.parse("home office, alig mozogtam").isEmpty());
        assertTrue(Activities.parse("alig mozogtam ma").isEmpty());
        assertTrue(Activities.parse("a gyerek edzésén voltam").isEmpty());
        // A sajátom viszont marad.
        assertEquals(60, Activities.parse("edzésen voltam 1 órát").plans.get(0).minutes);
        assertEquals(30, Activities.parse("alig bírtam végigcsinálni a 30 perc futást")
                .plans.get(0).minutes);
        assertEquals(60, Activities.parse("gyerekkel bicikliztünk 1 órát")
                .plans.get(0).minutes);
    }

    /**
     * A magyar szétszedi az összetételt: „hegyet másztunk".
     *
     * A „hegymászás" tövét ez nem fedi, így az ötórás hegymászásból semmi
     * nem került a naplóba.
     */
    @Test public void theSplitCompoundOfMountainClimbing() {
        assertEquals(300, Activities.parse("hegyet másztunk 5 órát").plans.get(0).minutes);
        assertEquals(120, Activities.parse("hegyre másztam 2 órát").plans.get(0).minutes);
        assertEquals(240, Activities.parse("hegyi túra 4 óra").plans.get(0).minutes);
        assertEquals("tura", Activities.parse("hegyet másztunk 5 órát").plans.get(0).kind.id);
    }

    /**
     * „Hétfőtől péntekig futottam": a kettő között minden nap benne van.
     *
     * Eddig kétféleképp is rosszul járt: a „hétfőtől" a HÉT szótövét adta, így
     * a mondat egyhetes időszakká vált EGYETLEN futással; a napneveket
     * felismerve pedig csak a két megnevezett napra került be egy-egy edzés.
     * Öt napból kettő.
     */
    @Test public void aWeekdayRangeCoversEveryDayBetween() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(2026, java.util.Calendar.AUGUST, 8, 12, 0, 0);   // szombat
        c.set(java.util.Calendar.MILLISECOND, 0);
        long sat = c.getTimeInMillis();
        Activities.Parsed p = Activities.parse("hétfőtől péntekig futottam", sat);
        assertEquals(1, p.plans.size());
        assertEquals(5, p.plans.get(0).count);
        assertEquals(5, p.exactDays.length);
        assertEquals(3, Activities.parse("keddtől csütörtökig kondi", sat)
                .plans.get(0).count);
        // A felsorolás változatlan: két megnevezett nap két edzés.
        assertEquals(2, Activities.parse("hétfőn és szerdán kondi", sat)
                .plans.get(0).count);
        // A „hetes bérlet" pedig továbbra sem időszak.
        assertEquals(1, Activities.parse("hetes bérlettel kondi", sat).days);
    }

    /**
     * „5 napja futottam" – IDŐPONT, nem időszak.
     *
     * A birtokos alak („napja", „hete", „hónapja") a magyar leggyakoribb
     * visszatekintője, és eddig időszaknak számított: az öt nappal ezelőtti
     * futás öt napra elosztva került a naplóba, vagyis a mai napra is. Az
     * étkezésnél ez a szabály régóta megvan.
     */
    @Test public void thePossessiveDayFormIsAPointInTime() {
        Activities.Parsed p = Activities.parse("5 napja futottam utoljára");
        assertEquals(5, p.offset);
        assertEquals(1, p.days);
        assertEquals(3, Activities.parse("3 napja úsztam").offset);
        assertEquals(14, Activities.parse("két hete kondi").offset);
        assertEquals(7, Activities.parse("1 hete futottam").offset);
        // Az „alatt" viszont továbbra is időszak.
        assertEquals(3, Activities.parse("3 nap alatt 2 futás").days);
        assertEquals(7, Activities.parse("a héten minden nap futottam").days);
    }

    /**
     * A „kétórás túra" két óra.
     *
     * A számnév ÓRÁS összetételben áll, a szótár viszont szóhatárt vár – így
     * a mondat időtartam nélkül maradt, és a túra alapértelmezett kilencven
     * perce került a naplóba a százhúsz helyett.
     */
    @Test public void theHourCompoundIsADuration() {
        assertEquals(120, Activities.parse("kétórás túra").plans.get(0).minutes);
        assertEquals(180, Activities.parse("háromórás kirándulás").plans.get(0).minutes);
        assertEquals(30, Activities.parse("félórás séta").plans.get(0).minutes);
        assertEquals(60, Activities.parse("1 órás kondi").plans.get(0).minutes);
    }

    /**
     * A sorozat mellé írt táv külön kiemelhető.
     *
     * A vegyes mondat két naplóba való. Ha az edzés-oldal mindent elment, a
     * fekvőtámaszból becsült „kondi" perc kétszer számít – egyszer
     * sorozatként, egyszer mozgásként. A szűrő azt hagyja meg, amit az
     * erősítő napló nem tud tárolni: a kimondott távot.
     */
    @Test public void onlyTheStatedDistanceCrossesOver() {
        Activities.Parsed p = Activities.parse("reggel 5 km futás, utána 20 fekvőtámasz");
        Activities.Parsed c = Activities.cardioOnly(p);
        assertEquals(1, c.plans.size());
        assertEquals("futas", c.plans.get(0).kind.id);
        assertEquals(5.0, c.plans.get(0).km, 0.01);
        // A lépésszám ugyanígy átjön: azt sem tudja tárolni a sorozat-napló.
        Activities.Parsed s = Activities.cardioOnly(
                Activities.parse("ma 12000 lépés és 3x10 fekvenyomás"));
        assertEquals(1, s.plans.size());
        assertEquals(12000, s.plans.get(0).steps);
        // Táv nélkül nem marad semmi – nincs mit átvinni.
        assertTrue(Activities.cardioOnly(Activities.parse("60 perc kondi")).isEmpty());
        assertTrue(Activities.cardioOnly(null).isEmpty());
        // A nap és az eltolás megmarad: a tegnapi futás tegnapi marad.
        assertEquals(1, Activities.cardioOnly(
                Activities.parse("tegnap 5 km futás és 20 fekvőtámasz")).offset);
    }

    /**
     * Az összesített idő nem külön edzés.
     *
     * A „ma 90 percet edzettem összesen: 30 perc kondi, 60 perc futás"
     * kilencvenese a másik két szám összege. Eddig harmadik időtartamként
     * állt sorba, a kondi kapta meg, a harminc pedig elveszett – százötven
     * perc mozgás került a naplóba kilencven helyett.
     */
    @Test public void theStatedTotalIsNotAThirdSession() {
        Activities.Parsed p = Activities.parse(
                "ma 90 percet edzettem összesen: 30 perc kondi, 60 perc futás");
        assertEquals(90, p.plans.get(0).minutes + p.plans.get(1).minutes);
        assertEquals(2, p.plans.size());
        Activities.Parsed q = Activities.parse(
                "összesen 120 perc mozgás: 45 perc futás, 45 perc bringa, 30 perc kondi");
        int sum = 0;
        for (Activities.Plan pl : q.plans) sum += pl.minutes;
        assertEquals(120, sum);
        // „Összesen" nélkül nem találgatunk: a hatvan itt a futás ideje, nem
        // a másik kettő összege.
        Activities.Parsed r = Activities.parse("60 perc futás, 30 perc kondi, 30 perc úszás");
        assertEquals(3, r.plans.size());
        assertEquals(60, r.plans.get(0).minutes);
    }

    /**
     * Az ismétlésszám nem alkalomszám.
     *
     * A „20 kettlebell swing és 10 burpee" húszasa a lendítések száma – a
     * kettlebell viszont kondi-szótő is, így húsz darab hatvanperces edzés
     * lett belőle, húsz napra szétosztva: húsz óra mozgás egy negyedórás
     * körből. Ha a mondatban felismert sorozat is van, és a szám PONTOSAN
     * annak az ismétlésszáma, akkor a szám azé.
     */
    @Test public void theRepCountIsNotAnOccasionCount() {
        Activities.Parsed p = Activities.parse("20 kettlebell swing és 10 burpee");
        assertEquals(1, p.plans.get(0).count);
        assertEquals(1, p.days);
        // A kettlebell ismétlés-szó lett: az öt kör hetvenöt lendítése
        // ismétlésből becsült időt kap (negyedóra), nem az alapértelmezett
        // hatvan percet.
        assertEquals("1d+0: 1×evezes/3, 1×kondi/15",
                summary("5 kör: 500 m evezés, 15 kettlebell swing"));
        // A kimondott alkalom megvédi magát: ott a szám után az edzés szó áll.
        assertEquals(2, Activities.parse("2 fekvőtámasz edzés").plans.get(0).count);
        // A saját nevén futó mozgás sem sérül: a „3 futás" három futás. A
        // fekvenyomás EGY kondiedzés mellé – a sorozatszáma továbbra sem
        // alkalomszám.
        assertEquals("7d+0: 3×futas/45, 1×kondi/60",
                summary("3 futás és 3x10 fekvenyomás a héten"));
    }

    /**
     * A szósöprés két új találata: a gyűrű és a kendő.
     *
     * A „gyur" tő a karikagyűrűt is elkapta, a kendó harcművészet-tő pedig a
     * kendőt – egy ékszerből és egy sálból hatvanperces edzés lett. A gyúrás
     * igealakjai és a tai chi maradnak.
     */
    @Test public void aRingAndAScarfAreNotWorkouts() {
        assertTrue(Activities.parse("elvesztettem a gyűrűmet").isEmpty());
        assertTrue(Activities.parse("kendőt vettem").isEmpty());
        assertTrue(Activities.parse("gyűrűs a kezem").isEmpty());
        // A gyúrás igealakjai maradnak – ez a konditerem szlengje.
        assertEquals("kondi", Activities.parse("ma gyúrtam egy jót").plans.get(0).kind.id);
        assertEquals(45, Activities.parse("gyúrás 45 perc").plans.get(0).minutes);
        assertEquals("harcmuveszet", Activities.parse("tai chi 40 perc").plans.get(0).kind.id);
        // A felszólítás terv marad: a „gyúrunk egyet" javaslat, nem napló.
        assertTrue(Activities.parse("gyúrunk egyet").isEmpty());
        assertTrue(Activities.parse("sportoljunk egy kicsit").isEmpty());
    }

    /**
     * A heti beosztás terv, nem napló.
     *
     * A „hétfő mell és tricepsz, kedd hát és bicepsz" azt írja le, mikor mit
     * edz az ember – egyetlen sorozatszám sincs benne. Eddig egy tricepsz-
     * gyakorlat lett belőle hat ismétléssel (a „hát" számnévként hattá vált),
     * és bekerült a rekordok közé. A „push pull legs, heti 6 edzés" pedig hat
     * negyvenöt perces bejegyzést csinált, egy hétre elosztva.
     */
    @Test public void aWeeklySplitIsAPlanNotALog() {
        assertTrue(StrengthParse.parse("hétfő mell és tricepsz, kedd hát és bicepsz")
                .isEmpty());
        assertTrue(Activities.parse("push pull legs, heti 6 edzés").isEmpty());
        // Kimondott sorozat vagy súly megvédi a valódi többnapos naplót.
        assertEquals(2, StrengthParse.parse(
                "hétfőn guggolás 3x5, szerdán fekvenyomás 4x8 60 kg").size());
        assertEquals(2, Activities.parse("hétfőn és szerdán futottam").total());
        // A múlt idő megvédi a beszámolót is.
        assertFalse(Activities.parse("heti 3 edzés volt a héten").isEmpty());
    }

    /**
     * Az óraállás mögötti szám nem ezres tagolás.
     *
     * Az óra-export természetes alakja: „túra 14,8 km 3:45:00 620 m
     * emelkedés". A „00 620" viszont szóközzel tagolt ezresnek látszott, a
     * „3:45:00620"-ból pedig már nem lett időtartam – a kimondott három és
     * háromnegyed óra helyére a tempóból becsült százhetvennyolc perc lépett.
     * Ugyanez vitte el a „21,1 km 1:52:30 320 kcal" és a „45 km 2:10:00
     * 800 m szint" idejét is.
     */
    @Test public void aNumberAfterTheClockIsNotAThousandsSeparator() {
        assertEquals(225, Activities.parse("túra 14,8 km 3:45:00 620 m emelkedés")
                .plans.get(0).minutes);
        assertEquals(113, Activities.parse("futás 21,1 km 1:52:30 320 kcal")
                .plans.get(0).minutes);
        assertEquals(130, Activities.parse("bringa 45 km 2:10:00 800 m szint")
                .plans.get(0).minutes);
        // Az ezres tagolás viszont maradjon: a „10 000 lépés" tízezer lépés.
        assertEquals(10000, Activities.parse("ma 10 000 lépést mentem")
                .plans.get(0).steps);
    }

    /**
     * A többnapos pótlás minden távja megmarad.
     *
     * A „tegnapelőtt 5 km, tegnap 8 km, ma 3 km" tipikus hétvégi pótlás. Egy
     * mozgásforma egyszer szerepel a listában, tehát a nyolc és a három
     * kilométer gazdátlanul maradt – és némán el is veszett: sem a naplóban,
     * sem a heti összegben, sem az XP-ben nem jelent meg.
     */
    @Test public void everyDistanceOfAMultiDayCatchUpSurvives() {
        Activities.Parsed p = Activities.parse("tegnapelőtt 5 km, tegnap 8 km, ma 3 km");
        assertEquals(3, p.plans.size());
        assertEquals(5.0, p.plans.get(0).km, 0.001);
        assertEquals(8.0, p.plans.get(1).km, 0.001);
        assertEquals(3.0, p.plans.get(2).km, 0.001);
        // A napok is szétnyílnak: három napról szól, nem háromszor
        // tegnapelőttről.
        assertEquals(3, p.days);
        assertEquals(2, p.offset);
        assertEquals(2, Activities.parse("futottam 5 km-t és 8 km-t").plans.size());
        // A RÉSZLET nem külön edzés: az „ebből" bontás, nem felsorolás.
        assertEquals(1, Activities.parse("futottam 10 km-t, ebből 5 km tempó")
                .plans.size());
        // A szintemelkedés méterben áll, és nem egy második séta.
        assertEquals(1, Activities.parse("túra 14,8 km 3:45:00 620 m emelkedés")
                .plans.size());
        assertEquals(1, Activities.parse("bringa 45 km 2:10:00 800 m szint")
                .plans.size());
    }

    /**
     * A kimondott nulla is tagadás.
     *
     * A „nehéz nap: 10 óra munka, semmi mozgás, este két sör" mondatból eddig
     * tízórás „egyéb mozgás" lett – pont abból a szóból, amivel az ember azt
     * mondja, hogy nem mozgott.
     */
    @Test public void anExplicitZeroIsANegation() {
        assertTrue(Activities.parse("nehéz nap: 10 óra munka, semmi mozgás, este két sör")
                .isEmpty());
        assertTrue(Activities.parse("ma semmi edzés, csak pihenés").isEmpty());
        assertTrue(Activities.parse("ma nem mozogtam semmit").isEmpty());
        // A „semmi más" viszont nem tagadás: ott a mozgás ki van mondva.
        assertEquals(45, Activities.parse("ma 45 perc kondi és semmi más")
                .plans.get(0).minutes);
    }

    /**
     * A kiírt számnév-pár is tartomány.
     *
     * A számjegyes „20-25 perc kondi" már huszonhárom perc volt, a kiírt
     * „húsz-huszonöt perc kondi" viszont HÚSZ külön edzés, húsz napra osztva,
     * egyenként huszonöt perccel: nyolc óra mozgás abból, ami húsz perc.
     */
    @Test public void aSpelledOutRangeIsARangeToo() {
        assertEquals("1d+0: 1×kondi/23", summary("húsz-huszonöt perc kondi"));
        assertEquals("1d+0: 1×futas/13", summary("tíz-tizenöt perc futás"));
        assertEquals(5.5, Activities.parse("öt-hat km futás").plans.get(0).km, 0.01);
        // A számjegyes alak változatlan.
        assertEquals("1d+0: 1×kondi/23", summary("20-25 perc kondi"));
        // Az egyszerű számnév sem sérül.
        assertEquals("1d+0: 1×futas/30", summary("harminc perc futás"));
    }

    /**
     * A program elkezdése még nem edzés.
     *
     * Az „elkezdtem a couch to 5k programot" ötkilométeres futásként került
     * be – abból a névből, ami épp azt jelenti, hogy odáig még el kell jutni.
     */
    @Test public void startingAProgrammeIsNotASession() {
        assertTrue(Activities.parse("elkezdtem a couch to 5k programot").isEmpty());
        assertTrue(Activities.parse("belevágtam a 30 napos kihívásba").isEmpty());
        // A megtörtént első alkalom viszont marad.
        assertEquals(3.0, Activities.parse("elkezdtem a programot, ma 3 km futás")
                .plans.get(0).km, 0.01);
        assertEquals(1, Activities.parse("elkezdtem futni").plans.size());
    }

    /**
     * A gazdátlan táv akkor sem veszhet el, ha van mellette edzés.
     *
     * A „nyomtam egy 5 km-t" magában már futásnak számított. A „ma reggel
     * 5 km, délután 40 perc kondi" öt kilométere viszont eltűnt: a kondi
     * elfoglalta a listát, és távot tárolni nem tud.
     */
    @Test public void anOrphanDistanceSurvivesNextToAWorkout() {
        Activities.Parsed p = Activities.parse(
                "ma reggel 5 km, délután 40 perc kondi, este 8 óra alvás");
        assertEquals(2, p.plans.size());
        assertEquals(40, p.plans.get(0).minutes);
        assertEquals("futas", p.plans.get(1).kind.id);
        assertEquals(5.0, p.plans.get(1).km, 0.001);
        // A kondi negyven perce a kondié marad – a futás hossza tempóból jön.
        assertEquals(30, p.plans.get(1).minutes);
        // Ahol a mozgásnak SAJÁT távja van, ott nincs mit pótolni.
        assertEquals(2, Activities.parse("10 km futás és 30 perc kondi").plans.size());
        assertEquals(1, Activities.parse("60 perc kondi").plans.size());
    }

    /**
     * A felsorolás sorszáma nem darabszám.
     *
     * Az „1. 5 km futás / 2. 30 perc kondi" kettese a lista második pontja,
     * és eddig KÉT kondi-edzés lett belőle. A „Nap 2:" a megosztott tervek
     * írásmódjában ugyanígy.
     */
    @Test public void aListNumberIsNotACount() {
        Activities.Parsed p = Activities.parse("1. 5 km futás\n2. 30 perc kondi");
        assertEquals(2, p.plans.size());
        assertEquals(1, p.plans.get(1).count);
        // A sorhatár helyére vessző kerül, nem üresség: a harminc perc a
        // kondié marad, nem esik gazdátlanul a mondat végére.
        assertEquals(30, p.plans.get(1).minutes);
        assertEquals(1, Activities.parse("1) 5 km futás\n2) 30 perc kondi")
                .plans.get(1).count);
        assertEquals(1, Activities.parse("Nap 1: futás 5 km. Nap 2: kondi 45 perc.")
                .plans.get(1).count);
        // A valódi darabszám marad: „2 fekvőtámasz edzés" két edzés.
        assertEquals(2, Activities.parse("2 fekvőtámasz edzés").plans.get(0).count);
        assertEquals(3, Activities.parse("3 futás a héten").plans.get(0).count);
    }

    /**
     * A helyesbítés második száma az igazi – a távnál is.
     *
     * A „nem futottam 10 km-t, csak 3-at" mondatból eddig SEMMI nem lett: a
     * tagadás elvitte az egész edzést, pedig a három kilométer megvolt.
     */
    @Test public void theCorrectedDistanceIsLogged() {
        Activities.Parsed p = Activities.parse("nem futottam 10 km-t, csak 3-at");
        assertEquals(1, p.plans.size());
        assertEquals(3.0, p.plans.get(0).km, 0.001);
        assertEquals(20, Activities.parse("nem 45 percet kondiztam, hanem 20-at")
                .plans.get(0).minutes);
        // Szám nélküli tagadásra nem él: ott a mondat tagadás marad.
        assertTrue(Activities.parse("nem futottam ma").isEmpty());
    }

    /**
     * A görgőzés levezetés, nem időzítő-terv.
     *
     * A „10 perc görgőzés edzés után" mondatra az app tíz perces ablakot
     * ajánlott – egy megtörtént levezetés helyett. A habhengerezés és a foam
     * rolling már szótő volt, a hétköznapi magyar neve nem.
     */
    @Test public void foamRollingIsACoolDown() {
        assertEquals("joga", Activities.parse("10 perc görgőzés edzés után")
                .plans.get(0).kind.id);
        assertEquals(10, Activities.parse("görgőztem 10 percet").plans.get(0).minutes);
        assertEquals("joga", Activities.parse("20 perc habhengerezés")
                .plans.get(0).kind.id);
    }

    /**
     * A többes szám harmadik személy magától elárulja magát.
     *
     * Az „ők futottak 10 km-t" és „a srácok csináltak 50 fekvőtámaszt" nem
     * az én naplóm – az utóbbi ráadásul az erősítő naplóba került, a
     * rekordok és a progresszió-javaslat közé. A magyar el is hagyja az
     * alanyt, tehát az igevégződés a jel.
     */
    @Test public void theThirdPersonPluralIsNotMine() {
        assertTrue(Activities.parse("ők futottak 10 km-t").isEmpty());
        assertTrue(StrengthParse.parse("a srácok csináltak 50 fekvőtámaszt").isEmpty());
        assertTrue(Activities.parse("vittem a gyereket edzésre").isEmpty());
        assertTrue(Activities.parse("elvittem a gyereket a meccsre").isEmpty());
        assertEquals(1, Activities.parse("hetek óta futok").plans.size());
        // Csak a CSELEKVÉS igéi számítanak. A magyar a saját testrészeimre is
        // többes szám harmadik személyt használ: az „elfáradtak a lábaim" és
        // a „jól sikerültek a sorozatok" ugyanígy néz ki, és egy általános
        // -tak/-tek szabály elvitte volna a mellettük álló valódi edzést is.
        assertEquals(10.0, Activities.parse("a lábaim elfáradtak a 10 km futás után")
                .plans.get(0).km, 0.01);
        assertEquals(1, StrengthParse.parse(
                "jól sikerültek a sorozatok, 3x10 fekvenyomás 60 kg").size());
        assertEquals(1, StrengthParse.parse(
                "a szettek között 2 perc pihi volt, guggolás 5x5 100 kg").size());
        // Az együtt végzett edzés az enyém is.
        assertEquals(10.0, Activities.parse("megcsináltuk a 10 km-t a párommal")
                .plans.get(0).km, 0.01);
        assertEquals(5.0, Activities.parse("a fiammal futottunk 5 km-t")
                .plans.get(0).km, 0.01);
    }

    /**
     * A szokás tagmondata nem viszi el a mellette álló valódi edzést.
     *
     * A „szoktam futni, ma 8 km-t futottam" nyolc kilométere és a „hetente
     * háromszor edzek, ma 45 perc kondi volt" negyvenöt perce nyomtalanul
     * eltűnt: a szokás-szabály az EGÉSZ mondatra élt.
     */
    @Test public void theHabitClauseDoesNotSwallowTheRealSession() {
        assertEquals(8.0, Activities.parse("szoktam futni, ma 8 km-t futottam")
                .plans.get(0).km, 0.01);
        assertEquals(45, Activities.parse("hetente háromszor edzek, ma 45 perc kondi volt")
                .plans.get(0).minutes);
        // Múlt idejű fél nélkül a szokás marad szokás.
        assertTrue(Activities.parse("hetente futok").isEmpty());
        assertTrue(Activities.parse("szoktam futni reggelente").isEmpty());
        assertTrue(Activities.parse("minden másodnap futok").isEmpty());
    }

    /**
     * Ahány alkalom, annyi táv – mindegyik a sajátját kapja.
     *
     * A „hétvégén két túra: szombaton 12 km, vasárnap 18 km" tizennyolc
     * kilométere elveszett, és MINDKÉT bejegyzés tizenkettőt kapott: a
     * kimondott alkalomszám csak sokszorozott, a második számot nem kereste.
     */
    @Test public void asManyDistancesAsSessions() {
        Activities.Parsed p = Activities.parse(
                "hétvégén két túra: szombaton 12 km, vasárnap 18 km");
        assertEquals(2, p.plans.size());
        assertEquals(12.0, p.plans.get(0).km, 0.01);
        assertEquals(18.0, p.plans.get(1).km, 0.01);
        Activities.Parsed r = Activities.parse("ma két futás: 5 km és 8 km");
        assertEquals(2, r.plans.size());
        assertEquals(8.0, r.plans.get(1).km, 0.01);
        // Az OSZTÓ pár nem ez: az „5-5 km" alkalmanként öt kilométer.
        Activities.Parsed d = Activities.parse("reggel és este is futottam 5-5 km-t");
        assertEquals(1, d.plans.size());
        assertEquals(2, d.plans.get(0).count);
        // Táv nélküli alkalomszám érintetlen.
        assertEquals(3, Activities.parse("3 futás a héten").plans.get(0).count);
    }

    /**
     * A „sem" ugyanolyan tagadás, mint a „nem".
     *
     * A „ma sem edzettem" mondatból negyvenöt perces „egyéb mozgás" lett –
     * vagyis pont az ellenkezője annak, amit a felhasználó leírt. A magyarban
     * a „sem" a megszokott alak, ha a tegnap is kimaradt.
     */
    @Test public void theOtherNegationWordCountsToo() {
        assertTrue(Activities.parse("ma sem edzettem").isEmpty());
        assertTrue(Activities.parse("ma sem futottam").isEmpty());
        // A rövid „se" ugyanaz: „ott se voltam a teremben".
        assertTrue(Activities.parse("ott se voltam a teremben").isEmpty());
        assertTrue(Activities.parse("el se mentem futni").isEmpty());
        // Csak előre töröl: a mondat másik fele megmarad.
        assertEquals("tura", Activities.parse("ma sem volt edzés, de 8000 lépést mentem")
                .plans.get(0).kind.id);
        // A „semmi" nem esik ide: ott a tő után betű áll, nem szóköz.
        assertEquals(45, Activities.parse("ma 45 perc kondi és semmi más")
                .plans.get(0).minutes);
    }

    /**
     * A „majdnem" nem történt meg.
     *
     * A „majdnem elmentem futni" negyvenöt perces bejegyzés lett – abból a
     * mondatból, ami épp azt mondja ki, hogy nem sikerült. A „majdnem 10
     * km-t futottam" viszont megtörtént: ott a szó a SZÁMOT pontosítja, nem
     * az igét tagadja.
     */
    @Test public void almostIsNotDone() {
        assertTrue(Activities.parse("majdnem elmentem futni").isEmpty());
        assertTrue(Activities.parse("kis híján elmentem edzeni").isEmpty());
        assertEquals(10.0, Activities.parse("majdnem 10 km-t futottam")
                .plans.get(0).km, 0.01);
        assertEquals(120, Activities.parse("majdnem 2 órát edzettem")
                .plans.get(0).minutes);
        // Csak előre töröl: a mondat másik fele megmarad.
        assertEquals(5.0, Activities.parse(
                "majdnem elaludtam a moziban, de utána futottam 5 km-t")
                .plans.get(0).km, 0.01);
    }

    /**
     * A hátravetett „nem lett" és az „éppen csak benéztem".
     *
     * A „csak átöltöztem, edzés nem lett" mondatból negyvenöt perces
     * bejegyzés lett a semmiből: a hátravetett tagadás leggyakoribb magyar
     * alakja hiányzott a listából. Az „éppen csak benéztem a terembe" pedig
     * a terem szavából csinált hatvan percet.
     */
    @Test public void theAlmostTrainingSentencesAreNotLogs() {
        assertTrue(Activities.parse("csak átöltöztem, edzés nem lett").isEmpty());
        assertTrue(Activities.parse("éppen csak benéztem a terembe").isEmpty());
        // A valódi edzés marad, ugyanabban a szórendben.
        assertEquals(1, Activities.parse("bementem a terembe és nyomtam 3x10 fekvenyomást")
                .plans.size());
    }

    /**
     * A „tervezett" nem terv – megtörtént edzésről szól.
     *
     * A „csak 3 km lett a tervezett 10 helyett" egésze kiesett: a puszta
     * „terv"/„tervez" szótő elvitte a mondatot, pedig a három kilométer
     * megvolt. A valódi terv-mondatok érintetlenek.
     */
    @Test public void thePlannedAmountIsNotAPlan() {
        assertEquals(3.0, Activities.parse("csak 3 km lett a tervezett 10 helyett")
                .plans.get(0).km, 0.01);
        // A terv marad terv.
        assertTrue(Activities.parse("a terv: guggolás 5x5 100 kg").isEmpty());
        assertTrue(Activities.parse("tervezek futni holnap").isEmpty());
        assertTrue(Activities.parse("azt tervezem, hogy elmegyek edzeni").isEmpty());
        assertTrue(Activities.parse("a tervem holnap futni").isEmpty());
    }

    /**
     * A feltételes múlt és a lefújt edzés.
     *
     * A „ha lett volna időm, futottam volna" negyvenöt perces bejegyzés lett
     * – abból a mondatból, ami épp azt mondja ki, hogy nem futott. A
     * „futást lefújtam az eső miatt" ugyanígy.
     */
    @Test public void theConditionalPastAndTheCalledOffSession() {
        assertTrue(Activities.parse("ha lett volna időm, futottam volna").isEmpty());
        assertTrue(Activities.parse("a futást lefújtam az eső miatt").isEmpty());
        assertTrue(Activities.parse("jó volna futni egyet").isEmpty());
        // A CSERE viszont megtörtént mozgás.
        assertEquals(20.0, Activities.parse("futás helyett bicikliztem 20 km-t")
                .plans.get(0).km, 0.01);
        assertEquals(45, Activities.parse("elmaradt a foci, helyette kondi 45 perc")
                .plans.get(0).minutes);
    }

    /**
     * Az aktív pihenőnap mozgás.
     *
     * Az „aktív pihenőnap: 30 perc séta" harminc perce a pihenő szavával
     * együtt eltűnt – pedig a séta megvolt, és épp az ilyen napokból áll
     * össze a heti alap.
     */
    @Test public void anActiveRestDayIsStillMovement() {
        assertEquals(30, Activities.parse("aktív pihenőnap: 30 perc séta")
                .plans.get(0).minutes);
        assertEquals("joga", Activities.parse("aktív pihenés: 20 perc jóga")
                .plans.get(0).kind.id);
        // A sima pihenőnap marad pihenőnap.
        assertTrue(Activities.parse("ma pihenőnap volt").isEmpty());
        assertTrue(Activities.looksLikeRest("ma pihenőnap volt"));
    }

    /**
     * A megnézett maraton nem lefutott maraton.
     *
     * A „megnéztem a maratont a tv-ben" NEGYVENKÉT kilométeres futás lett a
     * naplóban – a „néztem" ott volt a listán, a „megnéztem" nem. Az olvasás
     * és a rajtszám ugyanígy: a „megvan a rajtszámom a félmaratonra"
     * huszonegy kilométert írt be egy még meg nem futott versenyre.
     */
    @Test public void theWatchedMarathonIsNotRun() {
        assertTrue(Activities.parse("megnéztem a maratont a tv-ben").isEmpty());
        assertTrue(Activities.parse("olvastam egy cikket a futásról").isEmpty());
        assertTrue(Activities.parse("megvan a rajtszámom a félmaratonra").isEmpty());
        // A lefutott maraton marad maraton.
        assertEquals(42.2, Activities.parse("lefutottam a maratont").plans.get(0).km, 0.01);
        // És a nézés utáni valódi edzés is megmarad.
        assertEquals(5.0, Activities.parse("megnéztem a meccset, utána futottam 5 km-t")
                .plans.get(0).km, 0.01);
    }

    /**
     * A termi napok angol neve is edzés.
     *
     * A „lábnap" és a „tolónap" már szótő volt, a „leg day" és a „push day"
     * nem – pedig a magyar edzőtermekben legalább olyan gyakori. A „kemény
     * leg day, 75 perc" üres választ kapott.
     */
    @Test public void theEnglishGymDayNamesAreWorkoutsToo() {
        assertEquals(75, Activities.parse("kemény leg day, 75 perc").plans.get(0).minutes);
        assertEquals("kondi", Activities.parse("ma leg day volt").plans.get(0).kind.id);
        assertEquals(50, Activities.parse("pull day, 50 perc").plans.get(0).minutes);
        assertEquals(45, Activities.parse("full body 45 perc").plans.get(0).minutes);
        assertEquals("kondi", Activities.parse("push day: 4 gyakorlat")
                .plans.get(0).kind.id);
    }

    /**
     * A termi órák nevét is fel kell ismerni.
     *
     * A „spin óra" ugyanaz, mint a szobabicikli, az „aqua fitness" a vízben
     * történik, a „pole fitness" pedig tánc. Aki órára jár, az óra nevét írja
     * be, nem a mozgásformát – eddig mindhárom üres választ kapott.
     */
    @Test public void theGymClassNamesAreUnderstood() {
        assertEquals("kerekpar", Activities.parse("spin óra 50 perc")
                .plans.get(0).kind.id);
        assertEquals(50, Activities.parse("spin óra 50 perc").plans.get(0).minutes);
        assertEquals("uszas", Activities.parse("aqua fitness 45 perc")
                .plans.get(0).kind.id);
        assertEquals("tanc", Activities.parse("pole fitness 60 perc")
                .plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("indoor cycling 40 perc")
                .plans.get(0).kind.id);
    }

    /**
     * Az úszásnem neve is kimondja a sportot.
     *
     * Aki medencében edz, a hosszakat úszásnemre bontva írja: „medence:
     * 1000 m gyorson". A puszta táv magyarul futást jelent, így ez eddig
     * egykilométeres FUTÁS lett – 9,8-as MET-tel, majdnem másfélszeres
     * kalóriával.
     */
    @Test public void theSwimStrokeNamesTheSport() {
        assertEquals("uszas", Activities.parse("medence: 1000 m gyorson, 30 perc")
                .plans.get(0).kind.id);
        assertEquals(1.0, Activities.parse("medence: 1000 m gyorson, 30 perc")
                .plans.get(0).km, 0.01);
        assertEquals("uszas", Activities.parse("pillangózás 200 m")
                .plans.get(0).kind.id);
        // A mellény nem mellúszás, a „háton fekve" nem hátúszás – ezért nem
        // lett tő a puszta „mellen" és „haton".
        assertTrue(Activities.parse("vettem egy új mellényt").plans.isEmpty());
    }

    /**
     * A pihenő UTÁN már megint edzés van.
     *
     * A „pihi" a kihagyás szava, ezért a mondat innentől nem edzés – csakhogy
     * a „két hét pihi után visszaültem a bringára, 25 km" pont az ellenkezőjét
     * mondja: a szünet VÉGE után jön a mozgás. Eddig a pihi elvitte a
     * bringát, a gazdátlan huszonöt kilométerből meg FUTÁS lett, és a két
     * hetet is ráterítettük az edzésre – tizennégy napnyi bringázás egyetlen
     * délutánból.
     */
    @Test public void aBreakThatIsOverIsNotADenial() {
        Activities.Parsed p = Activities.parse("két hét pihi után visszaültem "
                + "a bringára, 25 km");
        assertEquals(1, p.plans.size());
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(25.0, p.plans.get(0).km, 0.01);
        assertEquals(1, p.days);
        assertEquals(1, Activities.parse("három hét betegség után 30 perc "
                + "könnyű futás").days);
        // A pihenőnap magában viszont marad tagadás.
        assertTrue(Activities.parse("ma pihenőnap volt").plans.isEmpty());
        // És a valódi időszak sem sérül.
        assertEquals(14, Activities.parse("az elmúlt két hétben 50 km-t "
                + "futottam").days);
    }

    /**
     * A lépcső a panasz helyszíne, nem edzés.
     *
     * A gyógytornász első kérdése az, hogy MIKOR fáj – és a térdre a válasz
     * majdnem mindig az, hogy „lépcsőn lefelé". A lépcsőzés viszont
     * mozgásforma is, így ebből eddig egy másfél órás TÚRA került a naplóba,
     * pont olyan napra, amikor a panasz miatt épp hogy nem mozgott az ember.
     */
    @Test public void theStairsInAComplaintAreNotAHike() {
        assertTrue(Activities.parse("lépcsőn lefelé fájdul a térdem")
                .plans.isEmpty());
        assertTrue(Activities.parse("lépcsőn felfelé húz a combhajlítóm")
                .plans.isEmpty());
        // A megtörtént lépcsőzés viszont marad – ott szám is van.
        assertEquals(20, Activities.parse("20 perc lépcsőzés, közben fájt "
                + "a térdem").plans.get(0).minutes);
        assertEquals(1, Activities.parse("20 emeletet lépcsőztem").plans.size());
    }

    /**
     * A gyakorlat NEVE nem külön kardió-edzés.
     *
     * A „súlyzós: guggolás 3×8 80, evezés 3×10 50" evezése egy sorozat a
     * teremben, nem félórányi evezőgépezés – eddig a hatvan perc kondi MELLÉ
     * bekerült egy harmincperces evezés is: ugyanaz a mozdulat kétszer,
     * ráadásul olyan hosszal, amit ki sem mondott senki.
     */
    @Test public void aLiftNameIsNotACardioSession() {
        List<Activities.Plan> p = Activities.parse("súlyzós: guggolás 3x8 80, "
                + "fekve 3x8 60, evezés 3x10 50").plans;
        assertEquals(1, p.size());
        assertEquals("kondi", p.get(0).kind.id);
        // A terem-bejegyzés viszont MEGMARAD: az „evezés 3x10 50 kg" egy
        // súlyzós edzés, ugyanúgy, mint a „guggolás 3x10 60 kg" – csak
        // éppen olyan gyakorlattal, aminek a neve egy kardió-gépé is.
        List<Activities.Plan> r = Activities.parse("evezés 3x10 50 kg").plans;
        assertEquals(1, r.size());
        assertEquals("kondi", r.get(0).kind.id);
        // A kimondott idő megvédi magát: az alapértéktől eltérő hossz marad.
        assertEquals(40, Activities.parse("40 perc evezőgép").plans.get(0).minutes);
        // Az ISMÉTLÉSSZÁM sem alkalomszám: a „3x12 evezés 60 kg" tizenkettese
        // az ismétlés, és eddig TIZENKÉT harmincperces evezés lett belőle –
        // hat óra mozgás egy sorozatból.
        List<Activities.Plan> q = Activities.parse("3x8 guggolás 100 kg, "
                + "3x10 fekvenyomás 70 kg, 3x12 evezés 60 kg").plans;
        assertEquals(1, q.size());
        assertEquals("kondi", q.get(0).kind.id);
    }

    /**
     * A köredzés hossza az EGÉSZ körből jön.
     *
     * A „körkörös edzés: 4 kör, 10 fekvőtámasz, 15 guggolás, 20 hasizom"
     * eddig öt percet kapott – az ELSŐ szám ötödét –, pedig ez négyszer
     * negyvenöt ismétlés, jó fél óra munka. A körszámmal csak akkor szorzunk,
     * ha az erő-felismerő még nem tette bele a sorozatokba.
     */
    @Test public void aCircuitLastsAsLongAsTheWholeCircuit() {
        assertEquals(36, Activities.parse("körkörös edzés: 4 kör, "
                + "10 fekvőtámasz, 15 guggolás, 20 hasizom").plans.get(0).minutes);
        // Ahol a sorozatok már megvannak, ott nem szorzunk újra.
        assertEquals(18, Activities.parse("3 kör: 20 guggolás, 10 fekvőtámasz")
                .plans.get(0).minutes);
        // Az egyszerű ismétlésszám becslése változatlan.
        assertEquals(5, Activities.parse("10 fekvőtámasz").plans.get(0).minutes);
        assertEquals(10, Activities.parse("50 fekvőtámasz").plans.get(0).minutes);
    }

    /**
     * A ping-pong kötőjellel is ping-pong, a sítalp is sí.
     *
     * A magyar ige a kötőjeles alakot ragozza tovább („ping-pongoztunk"), a
     * télen meg a felszerelés neve mondja ki a sportot („3 óra sítalpon").
     * Mindkettő válasz nélkül maradt.
     */
    @Test public void theHyphenAndTheGearNameTheSport() {
        assertEquals("tenisz", Activities.parse("ping-pongoztunk egy órát "
                + "a munkahelyen").plans.get(0).kind.id);
        assertEquals("si", Activities.parse("3 óra sítalpon").plans.get(0).kind.id);
        assertEquals(180, Activities.parse("3 óra sítalpon").plans.get(0).minutes);
    }

    /**
     * A gyógytornász nem torna, a kérdés nem napló.
     *
     * Két fabrikáció ugyanabból a mondatkörből: a „gyógytornász szerint
     * gyenge a középső farizmom" TORNA-szótagjából negyvenöt perces jóga
     * lett, a „terhes vagyok, milyen mozgás ajánlott?" MOZGÁS szavából meg
     * negyvenöt perc egyéb mozgás – egy olyan mondatból, ami épp azt kérdezi,
     * hogy mit lehetne csinálni.
     */
    @Test public void neitherTheTherapistNorTheQuestionIsAWorkout() {
        assertTrue(Activities.parse("a gyógytornász szerint gyenge "
                + "a középső farizmom").plans.isEmpty());
        assertTrue(Activities.parse("terhes vagyok, milyen mozgás "
                + "ajánlott?").plans.isEmpty());
        // A gyógytorna MAGA marad mozgás.
        assertEquals(20, Activities.parse("20 perc gyógytorna")
                .plans.get(0).minutes);
        // És a számmal írt kérdés mögött megtörtént edzés is állhat.
        assertEquals(30, Activities.parse("30 perc futás után fájt a térdem, "
                + "mit tegyek?").plans.get(0).minutes);
    }

    /**
     * A „már" nem visszatekintés, hanem tartam.
     *
     * A „minden reggel 10 perc nyújtás, már 2 hete" nem KÉT HETE történt
     * egyszer, hanem két hete tart. Eddig két héttel ezelőttre került a mai
     * nyújtás – vagyis a mai napra semmi, egy régi napra meg egy soha meg nem
     * történt edzés.
     */
    @Test public void theWordAlreadyMeansDurationNotADateInThePast() {
        assertEquals(0, Activities.parse("minden reggel 10 perc nyújtás, "
                + "már 2 hete").offset);
        // A puszta „2 hete" viszont marad visszatekintés.
        assertEquals(14, Activities.parse("2 hete futottam utoljára").offset);
        assertEquals(5, Activities.parse("5 napja úszás 1 km").offset);
        // A napszakkal mondott szokás sem napló.
        assertTrue(Activities.parse("minden este sétálok egyet").plans.isEmpty());
    }

    /**
     * A kukacos tempó is tempó.
     *
     * A futó-appok „10 km @ 5:30" alakja ugyanaz, mint az „5:30-as tempóval" –
     * eddig öt és fél ÓRA került a naplóba egy ötvenöt perces futásra. A
     * perjel a másik oldalon is tempót jelöl: az „5:30/km" harmincasából egy
     * harminc kilométeres MÁSODIK futás lett.
     */
    @Test public void theAtSignMarksThePaceToo() {
        assertEquals(55, Activities.parse("10 km @ 5:30").plans.get(0).minutes);
        List<Activities.Plan> p = Activities.parse("futás 10 km @ 5:30/km").plans;
        assertEquals(1, p.size());
        assertEquals(55, p.get(0).minutes);
        // A kimondott idő és a napszak nem sérül.
        assertEquals(55, Activities.parse("10 km 55:00").plans.get(0).minutes);
        assertEquals(25, Activities.parse("futás 5 km 24:59 tempó 5:00")
                .plans.get(0).minutes);
    }

    /**
     * A szakaszok közti séta nem külön túra.
     *
     * A „sprint edzés: 10x100 m, köztük séta vissza" sétája a szakaszok közti
     * visszasétálás – eddig KILENCVEN PERC gyaloglás került a naplóba egy
     * néhány perces sprint-edzés mellé. A KÖZTÜK szó pont ezt mondja ki.
     */
    @Test public void theWalkBetweenTheRepsIsNotAHike() {
        List<Activities.Plan> p = Activities.parse("sprint edzés: 10x100 m, "
                + "köztük séta vissza").plans;
        assertEquals(1, p.size());
        assertEquals("futas", p.get(0).kind.id);
        // A kimondott hosszal megadott séta viszont marad.
        assertEquals(2, Activities.parse("sprint edzés: 10x100 m, köztük "
                + "20 perc séta").plans.size());
    }

    /**
     * A meghiúsult szándék és a zárva tartó terem nem edzés.
     *
     * Az „akartam futni, de esett" negyvenöt perces futás lett, az
     * „elfelejtettem elmenni edzeni" és az „edzőterem zárva volt" pedig
     * negyvenöt, illetve hatvan perces bejegyzés – mindhárom olyan mondatból,
     * ami épp azt mondja el, hogy nem lett belőle semmi. Tagmondatra
     * szűkítve, hogy a mondat másik fele megmaradjon.
     */
    @Test public void theFailedIntentionIsNotAWorkout() {
        assertTrue(Activities.parse("akartam futni, de esett").plans.isEmpty());
        assertTrue(Activities.parse("elfelejtettem elmenni edzeni").plans.isEmpty());
        assertTrue(Activities.parse("az edzőterem zárva volt").plans.isEmpty());
        // A mondat másik fele megmarad.
        assertEquals(3.0, Activities.parse("akartam még futni, de csak 3 km-t "
                + "bírtam").plans.get(0).km, 0.01);
        assertEquals(30, Activities.parse("az edzőterem zárva volt, ezért "
                + "otthon 30 perc saját testsúly").plans.get(0).minutes);
    }

    /** A saját testsúlyos edzés is edzés – a puszta „testsúly" viszont mérleg. */
    @Test public void bodyweightTrainingIsAWorkout() {
        assertEquals("kondi", Activities.parse("otthon 30 perc saját testsúly")
                .plans.get(0).kind.id);
        assertEquals(40, Activities.parse("saját testsúllyal edzettem "
                + "40 percet").plans.get(0).minutes);
        assertTrue(Activities.parse("reggel 78,4 kg a testsúlyom").plans.isEmpty());
    }

    /**
     * A lépés és a táv ugyanaz a séta.
     *
     * A „ma 14 000 lépés, 9,8 km" a tíz és fél kilométeres gyaloglás MELLÉ
     * egy tíz kilométeres FUTÁST is beírt – húsz kilométer abból a tízből,
     * amit az ember tényleg megtett. A „14 000 lépés és futottam 5 km-t"
     * viszont két külön dolog: ott a futás ki van mondva.
     */
    @Test public void theStepsAndTheDistanceAreTheSameWalk() {
        List<Activities.Plan> p = Activities.parse("ma 14 000 lépés, 9,8 km").plans;
        assertEquals(1, p.size());
        assertEquals("tura", p.get(0).kind.id);
        assertEquals(9.8, p.get(0).km, 0.01);
        assertEquals(14000, p.get(0).steps);
        // Kimondott futás mellett marad a két bejegyzés.
        assertEquals(2, Activities.parse("ma 14 000 lépés és futottam "
                + "5 km-t").plans.size());
    }

    /**
     * A terem csak helyszín.
     *
     * A „45 perc spinning óra a teremben" a negyvenöt perces kerékpározás
     * MELLÉ egy hatvanperces kondit is beírt – ugyanannak az órának a
     * helyszínéből, kimondatlan hosszal. A teremben VÉGZETT edzés viszont
     * marad.
     */
    @Test public void theGymIsOnlyAPlace() {
        List<Activities.Plan> p = Activities.parse("45 perc spinning óra "
                + "a teremben").plans;
        assertEquals(1, p.size());
        assertEquals("kerekpar", p.get(0).kind.id);
        assertEquals(60, Activities.parse("a teremben 60 perc kondi")
                .plans.get(0).minutes);
        assertEquals("kondi", Activities.parse("teremben edzettem")
                .plans.get(0).kind.id);
    }

    /**
     * A mértékegység egyszer van kimondva, a szám kétszer.
     *
     * A „reggel és délután is futottam, 5 és 7 km" ötöse eddig némán
     * elveszett – az egyik futás teljesen hiányzott a naplóból. A magyar így
     * sorol: az egység a végén áll, és mindkét számra vonatkozik. A törtre
     * nem él, a „három és fél óra" egyetlen időtartam.
     */
    @Test public void theSharedUnitBelongsToBothNumbers() {
        List<Activities.Plan> p = Activities.parse("reggel és délután is "
                + "futottam, 5 és 7 km").plans;
        assertEquals(2, p.size());
        assertEquals(5.0, p.get(0).km, 0.01);
        assertEquals(7.0, p.get(1).km, 0.01);
        // A tört nem két szám: a „3 és fél óra futás" háromszáztíz perc.
        assertEquals(210, Activities.parse("3 és fél óra futás")
                .plans.get(0).minutes);
    }

    /**
     * A munkaóra nem edzésidő.
     *
     * A „hosszú nap, 11 óra munka, este 20 perc nyújtás" tizenegy órája a
     * MUNKÁÉ – eddig a nyújtás kapta meg, vagyis tizenegy óra jóga került a
     * naplóba, a valódi húsz perc meg elveszett. Csak a szám UTÁN álló szó
     * számít: a „munka 30 perc kondi" harminc perce a kondié.
     */
    @Test public void theWorkingHoursAreNotTrainingTime() {
        List<Activities.Plan> p = Activities.parse("hosszú nap, 11 óra munka, "
                + "este 20 perc nyújtás").plans;
        assertEquals(1, p.size());
        assertEquals(20, p.get(0).minutes);
        assertEquals(30, Activities.parse("2 óra utazás, aztán 30 perc futás")
                .plans.get(0).minutes);
        // A kerti és a fizikai munka viszont mozgás, a hossz az övé.
        assertEquals(90, Activities.parse("kerti munka 90 perc")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("munka 30 perc kondi")
                .plans.get(0).minutes);
        // A nap többi ülő órája ugyanígy: tv, ülés, tanulás, főzés, meeting.
        assertEquals(30, Activities.parse("2 óra tv, 30 perc séta")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("8 óra ülés az irodában, "
                + "este 30 perc futás").plans.get(0).minutes);
        assertEquals(25, Activities.parse("1,5 óra meeting, aztán "
                + "25 perc futópad").plans.get(0).minutes);
        // A pihenés is a mozdulatlan idő neve.
        assertEquals(30, Activities.parse("1 óra pihenés után 30 perc bringa")
                .plans.get(0).minutes);
    }

    /**
     * Ahány alkalom, annyi hossz.
     *
     * A „két edzés ma, 45 és 60 perc" hatvana eddig elveszett, és MINDKÉT
     * alkalom negyvenöt percet kapott. Ha a kimondott időtartamok száma pont
     * az alkalomszám, a hosszak a saját alkalmukhoz tartoznak – ugyanaz a
     * szabály, ami a távoknál már megvolt.
     */
    @Test public void asManySessionsAsManyDurations() {
        List<Activities.Plan> p = Activities.parse("két edzés ma, 45 és 60 perc").plans;
        assertEquals(2, p.size());
        assertEquals(45, p.get(0).minutes);
        assertEquals(60, p.get(1).minutes);
        List<Activities.Plan> q = Activities.parse("két futás ma, 30 és 45 perc").plans;
        assertEquals(2, q.size());
        assertEquals("futas", q.get(0).kind.id);
        assertEquals(30, q.get(0).minutes);
        assertEquals(45, q.get(1).minutes);
        // Egyetlen idő mellett marad az alkalomszám.
        assertEquals(2, Activities.parse("2 edzés 45 perc").plans.get(0).count);
    }

    /**
     * A névmás is alany: „ő kardiózott, én súlyzóztam".
     *
     * A kardió a párom edzése volt, mégis bekerült az én naplómba – az „ő"
     * ékezet nélkül egyetlen betű, de egész szóként a magyar mondatban
     * szinte csak névmás lehet.
     */
    @Test public void thePronounSubjectIsSomeoneElse() {
        List<Activities.Plan> p = Activities.parse("edzőterem a párommal: "
                + "ő kardiózott, én súlyzóztam").plans;
        assertEquals(1, p.size());
        assertEquals("kondi", p.get(0).kind.id);
    }

    /**
     * Az utolsó szakasz nem második futás.
     *
     * A „vasárnapi hosszú futás: 18 km, 1:45:20, utolsó 3 km tempóban" eddig
     * KÉT bejegyzés lett – tizennyolc plusz három kilométer, két napra
     * szétosztva – pedig a három kilométer a tizennyolc utolsó szakasza.
     */
    @Test public void theLastSegmentIsNotASecondRun() {
        Activities.Parsed p = Activities.parse("vasárnapi hosszú futás: 18 km, "
                + "1:45:20, utolsó 3 km tempóban");
        assertEquals(1, p.plans.size());
        assertEquals(18.0, p.plans.get(0).km, 0.01);
        assertEquals(105, p.plans.get(0).minutes);
        assertEquals(1, p.days);
    }

    /**
     * A méteres táv ideje perc:mp, nem óra:perc.
     *
     * Az „úszóverseny: 100 m gyors 1:12" hetvenkét PERCES úszás lett száz
     * méterre. Rövidtávon az 1:12 egy perc tizenkét másodperc – a kilométeres
     * táv óra-értelmezése változatlan.
     */
    @Test public void aShortRaceTimeIsMinutesAndSeconds() {
        assertEquals(1, Activities.parse("úszóverseny: 100 m gyors 1:12")
                .plans.get(0).minutes);
        assertEquals(1, Activities.parse("400 m futás 1:05").plans.get(0).minutes);
        // Kilométerrel írt távnál marad az óra:perc.
        assertEquals(105, Activities.parse("18 km futás 1:45:20")
                .plans.get(0).minutes);
        assertEquals(48, Activities.parse("futás 10 km 48:20").plans.get(0).minutes);
    }

    /**
     * A mondat közbeni sorszám is sorszám.
     *
     * A „letudtam a heti 3. futást, 7 km" hármasa a hét HARMADIK futása, nem
     * három futás – eddig három bejegyzés lett belőle. A „30 napos kihívás"
     * harmincasa pedig a kihívás hossza: az aznapi ötven guggolás eddig
     * harminc napra terült szét, és a sorozat ismétlésszáma is harmincra
     * változott.
     */
    @Test public void theMidSentenceOrdinalIsAnOrdinal() {
        Activities.Parsed p = Activities.parse("letudtam a heti 3. futást, 7 km");
        assertEquals(1, p.plans.size());
        assertEquals(7.0, p.plans.get(0).km, 0.01);
        Activities.Parsed q = Activities.parse("3. nap a 30 napos kihívásból: "
                + "50 guggolás");
        assertEquals(1, q.days);
        assertEquals(50, StrengthParse.parse("3. nap a 30 napos kihívásból: "
                + "50 guggolás").get(0).totalReps());
    }

    /**
     * A szokás melletti mai mennyiség megmenti a mondatot.
     *
     * Az „úszni járok, ma 1 km" első fele szokás, a második egy megtörtént
     * úszás – eddig az egész mondat elveszett, a kilométerrel együtt. A
     * puszta „úszni járok" marad terv-jellegű, nem bejegyzés.
     */
    @Test public void aTodayAmountRescuesTheHabitSentence() {
        Activities.Parsed p = Activities.parse("úszni járok, ma 1 km");
        assertEquals(1, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
        assertEquals(1.0, p.plans.get(0).km, 0.01);
        assertEquals(60, Activities.parse("kondiba járok, ma 60 perc")
                .plans.get(0).minutes);
        assertTrue(Activities.parse("úszni járok").plans.isEmpty());
    }

    /**
     * A visszaemlékezés nem mai bejegyzés.
     *
     * A „terhesség alatt jógáztam" és a „régen sokat futottam" hónapokkal
     * ezelőtti időkről szól – eddig mai, teljes hosszú bejegyzés lett
     * belőlük.
     */
    @Test public void aMemoryIsNotATodayEntry() {
        assertTrue(Activities.parse("terhesség alatt jógáztam, most 20 hetes "
                + "vagyok").plans.isEmpty());
        assertTrue(Activities.parse("régen sokat futottam, ma már csak "
                + "sétálok").plans.isEmpty());
        // A visszaemlékezett súly sem mai rekord.
        assertTrue(StrengthParse.parse("régebben 100 kg-ot nyomtam fekve")
                .isEmpty());
    }

    /**
     * A jelzőként álló csupasz „hét" nem időszak.
     *
     * A „ma deload hét van, könnyű súlyokkal edzettem 45 percet" mai edzése
     * hét napra terült szét, mert a „hét" szó szám nélkül is időszaknak
     * számított. A ragozott alak („a héten") marad időszak.
     */
    @Test public void aBareWeekAdjectiveIsNotASpan() {
        Activities.Parsed p = Activities.parse("ma deload hét van, könnyű "
                + "súlyokkal edzettem 45 percet");
        assertEquals(1, p.days);
        assertEquals(45, p.plans.get(0).minutes);
        assertEquals(7, Activities.parse("a héten összesen 42 km futás").days);
    }

    /**
     * A névelős „a futás után" időpont, nem edzés.
     *
     * A „jégfürdő 5 perc a futás után" öt perce a jégfürdőé – mégis egy
     * ötperces FUTÁS került a naplóba. A névelő a döntő: a „30 perc futás
     * után fájt a térdem" mondatban nincs névelő, ott a bejegyzés marad.
     */
    @Test public void theArticledSportIsATimeReference() {
        assertTrue(Activities.parse("jégfürdő 5 perc a futás után")
                .plans.isEmpty());
        List<Activities.Plan> p = Activities.parse("a futás után nyújtottam "
                + "10 percet").plans;
        assertEquals(1, p.size());
        assertEquals("joga", p.get(0).kind.id);
        // Névelő nélkül a futásé az idő.
        assertEquals(30, Activities.parse("30 perc futás után fájt a térdem, "
                + "mit tegyek?").plans.get(0).minutes);
    }

    /** A pulzuszóna száma nem darabszám: a „zóna 2 futás" egy futás. */
    @Test public void theZoneNumberIsNotACount() {
        Activities.Parsed p = Activities.parse("zóna 2 futás 40 perc, "
                + "pulzus 135 alatt");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(40, p.plans.get(0).minutes);
        assertEquals(1, Activities.parse("z2 futás 60 perc").plans.get(0).count);
    }

    /**
     * Az oda-vissza két fele ugyanaz az út.
     *
     * A „munkába biciklivel: oda 25, vissza 28 perc" eddig csak a vissza-időt
     * kapta meg – a napi ingázás fele elveszett. A két szám összege az egy
     * bejegyzés.
     */
    @Test public void thereAndBackIsOneTrip() {
        Activities.Parsed p = Activities.parse("munkába biciklivel: oda 25, "
                + "vissza 28 perc");
        assertEquals(1, p.plans.size());
        assertEquals(53, p.plans.get(0).minutes);
        assertEquals("kerekpar", p.plans.get(0).kind.id);
    }

    /**
     * A percben írt szakasz sem külön edzés.
     *
     * A „bemelegítés, aztán 3x(5 perc futás + 1 perc séta)" öt- és egyperces
     * darabjai a körök részei – eddig három ötperces futás és egy egyperces
     * túra is bekerült az időzítő-terv mellé. A „2x45 perc foci" viszont két
     * félidő pihenő-szakasz nélkül: a meccs marad bejegyzés.
     */
    @Test public void aMinuteSegmentIsNotASeparateWorkout() {
        assertTrue(Activities.parse("bemelegítés, aztán 3x(5 perc futás + "
                + "1 perc séta)").plans.isEmpty());
        assertEquals(2, Activities.parse("2x45 perc foci").plans.get(0).count);
    }

    /**
     * Az egy szóba eső azonos tövek egy találat.
     *
     * A „gyalogtúrán" elején a gyalog, a közepén a túra – mindkettő a túra
     * mozgásformájáé, mégis KÉT túra lett belőle, és a szintemelkedés métere
     * is kapott egy sajátot: a „gyalogtúrán voltunk, 14 km 600 m szint"
     * mondatból tizennégy plusz hat tized kilométer.
     */
    @Test public void twoStemsInOneWordAreOneHit() {
        List<Activities.Plan> p = Activities.parse("gyalogtúrán voltunk, "
                + "14 km 600 m szint").plans;
        assertEquals(1, p.size());
        assertEquals(14.0, p.get(0).km, 0.01);
        // Két KÜLÖNBÖZŐ sport egy mondatban marad kettő.
        assertEquals(2, Activities.parse("futottam 5 km-t és úsztam 1 km-t")
                .plans.size());
    }

    /** A lépésszám a szó mögött, kettősponttal is lépésszám. */
    @Test public void stepsAfterTheColonCount() {
        Activities.Parsed p = Activities.parse("eddigi legtöbb lépés: 21 450");
        assertEquals(1, p.plans.size());
        assertEquals(21450, p.plans.get(0).steps);
    }

    /**
     * A „közben" mért lépés nem külön séta a tevékenység mellé.
     *
     * A „takarítás közben 4000 lépés" egyetlen óra takarítás – eddig
     * takarítás PLUSZ fél óra gyaloglás lett belőle, kilencven perc mozgás
     * egy órából. A lépésszám (a lépéscél adata) marad.
     */
    @Test public void stepsDuringAChoreAreNotASecondWorkout() {
        List<Activities.Plan> p = Activities.parse("takarítás közben "
                + "4000 lépés").plans;
        assertEquals(1, p.size());
        assertEquals(4000, p.get(0).steps);
        // A kimondott idejű tevékenység mellett is EGY bejegyzés marad: a
        // két óra takarítás közben megtett lépések ugyanabban a két órában
        // történtek, két sorban viszont a mozgásidő duplázódott volna. A
        // lépésszám átköltözik a megmaradó sorra.
        List<Activities.Plan> q = Activities.parse("2 óra takarítás közben "
                + "4000 lépés").plans;
        assertEquals(1, q.size());
        assertEquals(120, q.get(0).minutes);
        assertEquals(4000, q.get(0).steps);
    }

    /**
     * A tervezett helyett a valódi.
     *
     * „A tervezett 10 km helyett csak 6 lett" hatosa a megtett táv – eddig az
     * egész mondat elveszett, mert a hat mellett nem állt mértékegység, a tíz
     * meg terv volt.
     */
    @Test public void thePlannedGivesWayToTheActual() {
        Activities.Parsed p = Activities.parse("a tervezett 10 km helyett "
                + "csak 6 lett");
        assertEquals(1, p.plans.size());
        assertEquals(6.0, p.plans.get(0).km, 0.01);
    }

    /**
     * Az eszközhatározós „6 héttel" időpont-eltolás, nem időszak.
     *
     * A „szalagszakadás után 6 héttel: óvatos guggolások saját súllyal" a mai
     * napról szól – mégis negyvenkét napra terült szét, és a hatos a guggolás
     * ismétlésszáma is lett.
     */
    @Test public void sixWeeksLaterIsNotASixWeekSpan() {
        Activities.Parsed p = Activities.parse("szalagszakadás után 6 héttel: "
                + "óvatos guggolások saját súllyal");
        assertEquals(1, p.days);
        assertTrue(StrengthParse.parse("szalagszakadás után 6 héttel: óvatos "
                + "guggolások saját súllyal").isEmpty());
    }

    /** A körhossz szorozva a körszámmal: 8 db 500 m-es kör négy kilométer. */
    @Test public void lapLengthTimesLapCount() {
        Activities.Parsed p = Activities.parse("500 m-es köröket futottam, "
                + "összesen 8-at");
        assertEquals(1, p.plans.size());
        assertEquals(4.0, p.plans.get(0).km, 0.01);
    }

    /**
     * A „30 perccel" eltolás, nem időtartam.
     *
     * A „reggeli után 30 perccel edzettem 45 percet" edzése negyvenöt perc –
     * eddig a harminc perces eltolást kapta meg, a valódi hossz meg
     * elveszett.
     */
    @Test public void thirtyMinutesLaterIsNotTheDuration() {
        assertEquals(45, Activities.parse("reggeli után 30 perccel edzettem "
                + "45 percet").plans.get(0).minutes);
    }

    /** A cél beállítása nem séta: a „10000 lépéses cél" száma terv. */
    @Test public void settingAStepGoalIsNotAWalk() {
        assertTrue(Activities.parse("beállítottam a 10000 lépéses célt")
                .plans.isEmpty());
        // A megtett lépés marad.
        assertEquals(10000, Activities.parse("ma 10000 lépés összejött")
                .plans.get(0).steps);
    }

    /** A terem mérlege sem edzés: a mérés-mondat terme csak helyszín. */
    @Test public void theGymScaleIsNotAWorkout() {
        assertTrue(Activities.parse("az edzőteremben mértem: 78,8 kg")
                .plans.isEmpty());
        assertEquals("kondi", Activities.parse("teremben edzettem")
                .plans.get(0).kind.id);
    }

    /** A -hez rag a hozzávalóé: a „monster ital edzéshez" nem edzés. */
    @Test public void aDrinkForTheWorkoutIsNotTheWorkout() {
        assertTrue(Activities.parse("monster ital edzéshez").plans.isEmpty());
        assertEquals(45, Activities.parse("edzettem ma 45 percet")
                .plans.get(0).minutes);
    }

    /**
     * A kimaradt bejegyzés nem kimaradt edzés.
     *
     * A „kimaradt a tegnapi bejegyzés: futottam 8 km-t" pótlás – a futás
     * megtörtént, csak a napló maradt le róla. Eddig a „kimaradt" szó az
     * egészet tagadásnak vette. A „3 nappal ezelőtt" visszatekintés pedig a
     * mai napra került.
     */
    @Test public void aMissedEntryIsNotAMissedWorkout() {
        Activities.Parsed p = Activities.parse("kimaradt a tegnapi bejegyzés: "
                + "futottam 8 km-t");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.offset);
        assertEquals(2, Activities.parse("elfelejtettem beírni: tegnapelőtt "
                + "úszás 1500 m").offset);
        assertEquals(3, Activities.parse("3 nappal ezelőtt futottam 10 km-t")
                .offset);
        // A kimaradt EDZÉS marad tagadás.
        assertTrue(Activities.parse("kimaradt a mai futás").plans.isEmpty());
    }

    /**
     * A megnevezett nap melletti puszta „semmi" is tagadás.
     *
     * A „hétfőn kondi 60 perc, kedden semmi" keddje eddig egy MÁSODIK
     * hatvanperces kondit kapott – pont arról a napról, amelyikről az ember
     * azt írta, hogy semmi.
     */
    @Test public void aBareNothingClearsTheNamedDay() {
        Activities.Parsed p = Activities.parse("hétfőn kondi 60 perc, "
                + "kedden semmi");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(1, p.days);
    }

    /**
     * A záró kísérő nem külön edzés, az angol warm up is bemelegítés.
     *
     * A „nyújtással zártam a 45 perces futást" nyújtása lemásolta a futás
     * negyvenöt percét – két bejegyzés lett egy edzésből. A „warm up 5
     * perc, wod 20 perc" wod-ja pedig az öt percet kapta.
     */
    @Test public void aClosingStretchIsNotASecondWorkout() {
        Activities.Parsed p = Activities.parse("nyújtással zártam a "
                + "45 perces futást");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
    }

    /**
     * Az igekötős gyalogos igék túrák, a mélység nem táv.
     *
     * A „felmásztam a kilátóhoz, 40 perc fölfelé" üresen jött vissza, a
     * „végigjártam a tanösvényt, 6 km" futás lett, a „leereszkedtünk a
     * barlangba 60 m mélyre" pedig hatvan méteres futás. A víztaposás
     * úszás-féle, nem egy pohár ásványvíz.
     */
    @Test public void prefixedWalkingVerbsAreHikes() {
        assertEquals("tura", Activities.parse("felmásztam a kilátóhoz, "
                + "40 perc fölfelé").plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("végigjártam a tanösvényt, "
                + "6 km").plans.get(0).kind.id);
        assertEquals(0, Activities.parse("leereszkedtünk a barlangba "
                + "60 m mélyre").plans.size());
        assertEquals("uszas", Activities.parse("mélyvízben tapostam a "
                + "vizet 10 percig").plans.get(0).kind.id);
    }

    /**
     * A „készülök" csak a saját tagmondatát viszi, az őszi maraton nem táv.
     *
     * Az „első 5k versenyemre készülök, ma 3 km sikerült" hárma valódi
     * futás – a felkészülés szava eddig az egészet elvitte, a súlyzós
     * változatot (5x3 140) is. Az „ősszel maraton" pedig negyvenkét
     * kilométeres MAI futást írt be, a „versenyre 10 hét" hetven napos
     * időszakot.
     */
    @Test public void preparingForARaceKeepsTodaysWork() {
        Activities.Parsed p = Activities.parse("első 5k versenyemre "
                + "készülök, ma 3 km sikerült megállás nélkül");
        assertEquals(1, p.plans.size());
        assertEquals(3.0, p.plans.get(0).km, 0.001);
        Activities.Parsed q = Activities.parse("ősszel maraton, most "
                + "építem az alapozást: 12 km hosszú futás");
        assertEquals(1, q.plans.size());
        assertEquals(12.0, q.plans.get(0).km, 0.001);
        assertEquals(1, Activities.parse("bikini fitnesz versenyre 10 hét, "
                + "ma színpadi póz gyakorlás").days);
    }

    /**
     * Az éves-havi összegző nem egy (vagy ötven) mai edzés.
     *
     * Az „összesen 1250 km futás idén" mai futást írt be 211 napra, „a
     * Garmin évi összesítője: 210 edzés" ÖTVEN egyéb mozgást, a „legjobb
     * hónapom volt: 160 km" pedig egy 160 kilométeres futást. A mai és a
     * heti összeg marad.
     */
    @Test public void aYearlySummaryIsNotOneWorkout() {
        assertEquals(0, Activities.parse("véget ért a szezon, összesen "
                + "1250 km futás idén").plans.size());
        assertEquals(0, Activities.parse("a Garmin évi összesítője: "
                + "210 edzés, 9800 perc").plans.size());
        assertEquals(0, Activities.parse("legjobb hónapom volt: 160 km és "
                + "14 edzés").plans.size());
        assertEquals(1, Activities.parse("ma összesen 12000 lépés "
                + "lett").plans.size());
    }

    /**
     * A nyújtózkodás az íróasztalnál nem nyújtás-edzés.
     */
    @Test public void stretchingAtTheDeskIsNotAWorkout() {
        assertEquals(0, Activities.parse("óránként felálltam nyújtózni "
                + "pár percre").plans.size());
        assertEquals(15, Activities.parse("nyújtottam 15 percet edzés "
                + "után").plans.get(0).minutes);
    }

    /**
     * A terem kondija átadja az ellopott percet a konkrét sportnak.
     *
     * A „falmásztam a boulder teremben 90 percet" falmászása az
     * alapértelmezett órát kapta, a terem kondija meg a kimondott
     * kilencven percet – fordítva kell. A paddle pedig evezés.
     */
    @Test public void theGymGivesBackTheStolenMinutes() {
        Activities.Parsed p = Activities.parse("falmásztam a boulder "
                + "teremben 90 percet");
        assertEquals(1, p.plans.size());
        assertEquals(90, p.plans.get(0).minutes);
        assertEquals("evezes", Activities.parse("paddleztem 5 km-t a "
                + "Dunán").plans.get(0).kind.id);
    }

    /**
     * A távirati „kardió 30, súlyzó 40" csupasz száma perc.
     *
     * Az idő-alapú sport neve utáni kis szám nem lehet más – eddig
     * mindkettő az alapértelmezett hosszt kapta. A táv-alapú sportnál
     * („futás 10") nem döntünk: az km is lehet.
     */
    @Test public void telegraphicBareNumbersAreMinutes() {
        Activities.Parsed p = Activities.parse("reggel kardió 30, este "
                + "súlyzó 40");
        assertEquals(2, p.plans.size());
        assertEquals(30, p.plans.get(0).minutes);
        assertEquals(40, p.plans.get(1).minutes);
        assertEquals(50, Activities.parse("edzés 50").plans.get(0).minutes);
    }

    /**
     * A birtokos terem és a hosszban mért szállodai medence is edzés.
     *
     * Az „a hotel edzőtermében 30 perc" terme-je elveszti a második e-t,
     * és nem illeszkedett a „terem" tőre – a fél óra elveszett. A
     * „szállodai medence 20 hossz" fél kilométere pedig FUTÁS lett, mert
     * a puszta medence (testrész is) nem úszás-tő.
     */
    @Test public void hotelGymAndPoolLengthsCount() {
        Activities.Parsed p = Activities.parse("a hotel edzőtermében "
                + "30 perc");
        assertEquals(1, p.plans.size());
        assertEquals("kondi", p.plans.get(0).kind.id);
        Activities.Parsed q = Activities.parse("szállodai medence 20 hossz");
        assertEquals(1, q.plans.size());
        assertEquals("uszas", q.plans.get(0).kind.id);
        assertEquals(0.5, q.plans.get(0).km, 0.001);
    }

    /**
     * A kihívás haladás-jegyzete nem mai edzés, a széria nem dátum.
     *
     * A „januári futókihívás: eddig 87 km a 100-ból" nyolcvanhét
     * kilométeres MAI futást írt be – egy hónap összegéből. A „megvan a
     * 10000 lépés 21 napja folyamatosan" pedig 21 nappal ezelőttre került
     * – a széria hossza nem visszatekintő dátum.
     */
    @Test public void aChallengeProgressNoteIsNotTodaysRun() {
        assertEquals(0, Activities.parse("januári futókihívás: eddig "
                + "87 km a 100-ból").plans.size());
        Activities.Parsed p = Activities.parse("megvan a 10000 lépés "
                + "21 napja folyamatosan");
        assertEquals(0, p.offset);
        assertEquals(10000, p.plans.get(0).steps);
        // A valódi visszatekintés marad.
        assertEquals(5, Activities.parse("5 napja futottam 10 km-t").offset);
    }

    /**
     * A meghiúsult terv nem edzés – a helyette végzett viszont az.
     *
     * Az „uszodába mentem de zárva volt, helyette 5 km futás" úszást IS
     * írt (a „de" előtti fél élt túl), az „elromlott a futópad, átültem a
     * biciklire" futópadja futást, a babymedencés játék úszást, a „3
     * km-nél leállt az óra" pedig külön háromkilométeres futást.
     */
    @Test public void aFoiledPlanIsNotAWorkout() {
        Activities.Parsed p = Activities.parse("uszodába mentem de zárva "
                + "volt, helyette 5 km futás");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("elromlott a futópad, "
                + "átültem a biciklire 30 percre").plans.get(0).kind.id);
        assertEquals(0, Activities.parse("az úszásoktatás elmaradt, a "
                + "gyerekkel játszottunk a babymedencében").plans.size());
        Activities.Parsed q = Activities.parse("leállt az óra 3 km-nél, "
                + "összesen kb 5 km lett");
        assertEquals(1, q.plans.size());
        assertEquals(5.0, q.plans.get(0).km, 0.001);
    }

    /**
     * A jelzős szám nem védi a panaszt, a terem nem másol percet.
     *
     * Az „a 42-es cipőm szorít futásnál" negyvenöt perces futást írt be –
     * egy cipő-panaszból: a méret száma bizonyítéknak számított. A
     * „4-es teremben volt a spinning, 45 perc" kondija pedig lemásolta a
     * spinning percét. A valódi fájós-de-megtörtént edzés marad.
     */
    @Test public void aShoeSizeDoesNotProveAWorkout() {
        assertEquals(0, Activities.parse("a 42-es cipőm szorít "
                + "futásnál").plans.size());
        Activities.Parsed p = Activities.parse("a 4-es teremben volt a "
                + "spinning, 45 perc");
        assertEquals(1, p.plans.size());
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(1, Activities.parse("20 perc futás után fájt a "
                + "térdem").plans.size());
    }

    /**
     * A „24 órás" terem a nyitvatartás – a 24 órás verseny viszont edzés.
     */
    @Test public void aTwentyFourHourGymIsNotAWorkoutLength() {
        assertEquals(40, Activities.parse("az edzőterem 24 órás, éjfélkor "
                + "mentem, 40 perc").plans.get(0).minutes);
        assertEquals(1440, Activities.parse("24 órás futóversenyen 142 km-t "
                + "tettem meg").plans.get(0).minutes);
        // A boxterem egy találat, nem box + kondi.
        assertEquals(1, Activities.parse("nyílt nap a boxteremben, "
                + "kipróbáltam, 30 perc").plans.size());
    }

    /**
     * A tizedes óra is óra: az „1,5h" másfél óra.
     */
    @Test public void decimalHoursCount() {
        assertEquals(90, Activities.parse("séta 1,5h a városban")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("futás 0,5h reggel")
                .plans.get(0).minutes);
        assertEquals(80, Activities.parse("edzés 1h 20m")
                .plans.get(0).minutes);
    }

    /**
     * A sport-tő utáni „nélkül" tagadás.
     *
     * A „hűtöttem magam a Balatonban, úszás nélkül csak lubickolás"
     * negyvenöt perc úszást írt a naplóba – pont abból a szóból, amivel
     * az ember kimondta, hogy nem úszott. A másik sport marad: a „futás
     * nélkül telt a hét, csak 2 úszás volt" úszásai bekerülnek.
     */
    @Test public void aSportFollowedByWithoutIsNegated() {
        assertEquals(0, Activities.parse("hűtöttem magam a Balatonban, "
                + "úszás nélkül csak lubickolás").plans.size());
        Activities.Parsed p = Activities.parse("futás nélkül telt a hét, "
                + "csak 2 úszás volt, 1-1 km");
        assertEquals(1, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
    }

    /**
     * A „terveztem, végül" mondat vége a valóság.
     *
     * A „30 perc futást terveztem, de végül 50 perc lett" ötven perce
     * megtörtént – a tervezés szava eddig az egészet elvitte, a mondat
     * üresen jött vissza. A puszta terv („holnapra 20 perc futást
     * tervezek") terv marad.
     */
    @Test public void thePlannedVersusActualKeepsTheActual() {
        Activities.Parsed p = Activities.parse("30 perc futást terveztem, "
                + "de végül 50 perc lett");
        assertEquals(1, p.plans.size());
        assertEquals(50, p.plans.get(0).minutes);
        assertEquals(0, Activities.parse("holnapra 20 perc futást "
                + "tervezek").plans.size());
    }

    /**
     * A „mehet a kemény edzés" engedély a jövőre, az aktív idő viszont adat.
     *
     * A „whoop recovery 85%, mehet a kemény edzés" negyvenöt perces
     * bejegyzést kapott – egy el sem kezdett napról. A tagadás csak a
     * saját tagmondatát viszi: a „megvolt a futás 8 km, mehet a pihenés"
     * futása marad. Az óra aktív ideje pedig mozgás.
     */
    @Test public void aGreenLightIsNotAWorkoutYet() {
        assertEquals(0, Activities.parse("whoop recovery 85%, mehet a "
                + "kemény edzés").plans.size());
        assertEquals(1, Activities.parse("megvolt a futás 8 km, mehet a "
                + "pihenés").plans.size());
        assertEquals(47, Activities.parse("az applikáció 47 perc aktív "
                + "időt mért").plans.get(0).minutes);
    }

    /**
     * A szorzószám utáni „is" csak nyomaték.
     */
    @Test public void twiceWithEmphasisIsStillTwice() {
        assertEquals(2, Activities.parse("a hétvégén kétszer is voltam "
                + "úszni, 1-1 km").plans.get(0).count);
    }

    /**
     * A „de" új állítást nyit, az „átlag" pedig tempó-szó.
     *
     * Az „az nem edzés de 6 km-t gyalogoltam a partig" hat kilométere
     * megtörtént – a tagadás vessző híján eddig az egész tagmondatot
     * elvitte. Az „új cipőben 12 km, átlag 5:40" öt-negyvene percenkénti
     * idő: 68 perc, nem öt óra negyven.
     */
    @Test public void butOpensANewStatementAndAtlagIsPace() {
        Activities.Parsed p = Activities.parse("hajnali 5-kor keltem "
                + "horgászni, az nem edzés de 6 km-t gyalogoltam a partig");
        assertEquals(1, p.plans.size());
        assertEquals(6.0, p.plans.get(0).km, 0.001);
        assertEquals(0, Activities.parse("nem futottam de nem is "
                + "úsztam").plans.size());
        assertEquals(68, Activities.parse("az új cipőben 12 km, semmi "
                + "panasz, átlag 5:40").plans.get(0).minutes);
    }

    /**
     * A termi gépek a saját sportjukat mondják, a terem csak helyszín.
     *
     * A „kéziergométer a rehab részlegen 10 perc" tízperces
     * KÉZILABDA-meccs lett, a „sífutógép 25 perc, jó kis kardió" mellé
     * egy második edzés került az értékelő megjegyzésből, a „futópad 5%
     * emelkedőn 40 perc séta" a séta MELLÉ egy 45 perces futást is kapott,
     * a „45 perc kardió a teremben" mellé pedig egy hatvanperces kondit.
     */
    @Test public void machinesNameTheirOwnSport() {
        Activities.Parsed p = Activities.parse("kéziergométer a rehab "
                + "részlegen 10 perc");
        assertEquals(1, p.plans.size());
        assertEquals("egyeb", p.plans.get(0).kind.id);
        assertEquals(1, Activities.parse("sífutógép 25 perc, jó kis "
                + "kardió").plans.size());
        Activities.Parsed q = Activities.parse("futópad 5% emelkedőn "
                + "40 perc séta");
        assertEquals(1, q.plans.size());
        assertEquals("tura", q.plans.get(0).kind.id);
        assertEquals(1, Activities.parse("45 perc kardió a "
                + "teremben").plans.size());
    }

    /**
     * Az akupunktúra nem túra, a vinyasa viszont jóga.
     *
     * Az „akupunktúra kezelés a hátamra" kilencven perces GYALOGTÚRÁT írt
     * a naplóba – a szó közepén ült a túra. A jóga-irányzatok neve
     * (vinyasa, napüdvözlet) és a fascia-lazítás viszont eddig üresen jött
     * vissza.
     */
    @Test public void acupunctureIsNotAHike() {
        assertEquals(0, Activities.parse("akupunktúra kezelés a "
                + "hátamra").plans.size());
        assertEquals("joga", Activities.parse("vinyasa flow 60 perc a "
                + "stúdióban").plans.get(0).kind.id);
        assertEquals("joga", Activities.parse("napüdvözlet sorozat reggel, "
                + "12 ismétlés").plans.get(0).kind.id);
    }

    /**
     * A mért tagmondat kimenti a szokás-mondatot.
     *
     * A „botokkal járok, nordic walking 3 km" második fele kimondott távú,
     * megtörtént túra – eddig a „járok" szokás-szava az egészet elvitte.
     * A gyakoriság („kondiba járok, heti 3x") szokás marad.
     */
    @Test public void aMeasuredClauseRescuesTheHabitSentence() {
        Activities.Parsed p = Activities.parse("botokkal járok, nordic "
                + "walking 3 km");
        assertEquals(1, p.plans.size());
        assertEquals(3.0, p.plans.get(0).km, 0.001);
        assertEquals(0, Activities.parse("kondiba járok, heti 3x")
                .plans.size());
    }

    /**
     * A lejátszott meccs edzés – a tévén nézett nem.
     */
    @Test public void aPlayedMatchIsAWorkout() {
        assertEquals(1, Activities.parse("a meccset megnyertük 3-1-re, "
                + "végig játszottam").plans.size());
        assertEquals(0, Activities.parse("meccset néztem a tévében "
                + "este").plans.size());
    }

    /**
     * A fél-fél óra az oda-vissza út két fele, az ugrálás pedig mozgás.
     *
     * A „sétáltunk a piacig és vissza, fél-fél óra" hatvan perc együtt –
     * eddig csak az egyik fél került be. Az „ugráltam vagy 20 percet" az
     * ugrálóvárban is edzés; a gyerek ugrálása a kanapén nem az enyém.
     */
    @Test public void halfAndHalfHourIsTheRoundTrip() {
        Activities.Parsed p = Activities.parse("nagyival sétáltunk a piacig "
                + "és vissza, fél-fél óra");
        assertEquals(60, p.plans.get(0).minutes);
        assertEquals(20, Activities.parse("gyerekszülinapon ugrálóvár, én is "
                + "ugráltam vagy 20 percet").plans.get(0).minutes);
        assertEquals(0, Activities.parse("a gyerek ugrált a kanapén egész "
                + "délután").plans.size());
    }

    /**
     * A „kajak" nyomatékosító szó nem csónak.
     *
     * A „kajak kifárasztott a mai meló" fél óra evezést írt a naplóba – a
     * beszélt nyelvben a kajak nyomatékosítás (= nagyon). A ragozott
     * alakok (kajakoztam) és a „kajak túra" a vízé maradnak – utóbbi
     * egyben tő, hogy a fej-szó szabály ne a gyalogtúrának adja.
     */
    @Test public void kajakTheSlangWordIsNotABoat() {
        assertEquals(0, Activities.parse("kajak kifárasztott a mai meló, "
                + "semmi edzés").plans.size());
        assertEquals("evezes", Activities.parse("kajakoztam a Tiszán másfél "
                + "órát").plans.get(0).kind.id);
        assertEquals("evezes", Activities.parse("kajak túra a Dunán, "
                + "12 km").plans.get(0).kind.id);
    }

    /**
     * Az intervall számpárja nem napok és nem alkalmak.
     *
     * A „30-30 intervall 10x" HARMINC napra osztott HARMINC egyéb-edzést
     * írt be – egy időzítő-beállításból. A számpár a munkára-pihenőre, a
     * „10x" a körökre tartozik. Az EMOM sétáló pihenője sem külön túra.
     */
    @Test public void anIntervalPairIsNotDaysOrOccasions() {
        Activities.Parsed p = Activities.parse("30-30 intervall 10x");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        Activities.Parsed q = Activities.parse("futás: 4x800 m 2 perc "
                + "sétáló pihenővel");
        for (Activities.Plan pl : q.plans)
            assertFalse("tura".equals(pl.kind.id));
        assertEquals(1, Activities.parse("minden második percben 15 "
                + "kettlebell swing, 20 percig").plans.size());
    }

    /**
     * A lépéscél mellett kimondott mai érték a valódi lépésszám.
     *
     * A „napi lépéscél 10000, ma 11200 lett" és a „a lépéscélt
     * teljesítettem, 10500 lépés" eddig üresen jött vissza: az első
     * lépés-szó a cél összetett szavában ült, szám nélkül, és a kereső
     * az első szónál feladta. A „léptem 14 ezret" igés alak ugyanígy.
     */
    @Test public void theStepGoalDoesNotHideTheRealCount() {
        assertEquals(11200, Activities.parse("napi lépéscél 10000, ma "
                + "11200 lett").plans.get(0).steps);
        assertEquals(10500, Activities.parse("a lépéscélt teljesítettem, "
                + "10500 lépés").plans.get(0).steps);
        assertEquals(14000, Activities.parse("léptem vagy 14 ezret a "
                + "városnézésen").plans.get(0).steps);
    }

    /**
     * Az angolul írt pihenőnap is pihenőnap.
     *
     * Az „alvás 6:45, rhr 51, edzés rest day" mellől egy 45 perces
     * „egyéb mozgás" került a naplóba – a percek ráadásul az alvás
     * órájából jöttek. A rest day melletti valódi séta viszont marad.
     */
    @Test public void restDayInEnglishIsStillARestDay() {
        assertEquals(0, Activities.parse("alvás 6:45, rhr 51, edzés "
                + "rest day").plans.size());
        assertEquals(1, Activities.parse("rest day, csak sétáltam "
                + "20 percet").plans.size());
    }

    /**
     * Az izomláz beszámolója nem új edzés.
     *
     * Az „izomláz van rendesen a tegnapi lábnaptól" egy hatvanperces
     * kondit írt TEGNAPRA – pedig azt az edzést az ember már beírta,
     * amikor megtörtént. A kimondott számmal írt edzés mellette marad:
     * az „izomláz után 45 perc kondi ment ma" valódi mai edzés.
     */
    @Test public void muscleSorenessIsNotANewWorkout() {
        assertEquals(0, Activities.parse("izomláz van rendesen a tegnapi "
                + "lábnaptól").plans.size());
        assertEquals(1, Activities.parse("izomláz után 45 perc kondi ment "
                + "ma").plans.size());
    }

    /**
     * A „körül mozog" ingadozás, nem mozgás.
     */
    @Test public void hoveringAroundAValueIsNotExercise() {
        assertEquals(0, Activities.parse("pihenőpulzus 55 körül mozog "
                + "mostanában").plans.size());
    }

    /**
     * Az úszásnem főnévi alakja is úszás.
     *
     * A „mellúszás 800 m" és a „hátúszás 800 m" nyolcszáz méteres FUTÁS
     * lett: az „uszas" tő szó belsejében szándékosan nem él, ezért az
     * összetett úszásnem-szavak saját tövet kaptak.
     */
    @Test public void strokeNounsAreSwimming() {
        for (String q : new String[]{"hátúszás 800 m bemelegítésnek",
                "mellúszás 800 m", "gyorsúszás 800 m"}) {
            Activities.Parsed p = Activities.parse(q);
            assertEquals(q, 1, p.plans.size());
            assertEquals(q, "uszas", p.plans.get(0).kind.id);
        }
    }

    /**
     * A bringás szókincs sportnév nélkül is tekerés.
     *
     * Az „országúti kör 60 km" hatvan kilométeres FUTÁS lett, az „mtb
     * túra az erdőben" gyalogtúra, a „zwift edzés" és az „e-bike" semmi.
     * A defekt is bringát mond: az a kör a nyeregben történt.
     */
    @Test public void cyclingVocabularyMeansCycling() {
        String[][] cases = {{"országúti kör 60 km 200 watt átlaggal", "kerekpar"},
                {"zwift edzés 45 perc, 25 km virtuálisan", "kerekpar"},
                {"mtb túra az erdőben, 35 km", "kerekpar"},
                {"e-bike-kal 30 km, alig fáradtam el", "kerekpar"},
                {"defekt miatt csak 15 km lett a tervezett 40-ből", "kerekpar"}};
        for (String[] c : cases) {
            Activities.Parsed p = Activities.parse(c[0]);
            assertEquals(c[0], 1, p.plans.size());
            assertEquals(c[0], c[1], p.plans.get(0).kind.id);
        }
    }

    /**
     * A tempó a szám előtt és jelzőként is tempó, nem időtartam.
     *
     * Az „átlagtempóm 5:20 volt a 10 kilométeren" öt-húsza percenkénti
     * idő – eddig öt óra húsz perces futás lett belőle. A „4:45-ös
     * kilométerekkel" ugyanígy: a nyolc kilométer a tempóval számolódik,
     * nem négy és háromnegyed órával.
     */
    @Test public void paceBeforeTheNumberIsStillPace() {
        Activities.Parsed p = Activities.parse("átlagtempóm 5:20 volt a "
                + "10 kilométeren");
        assertEquals(1, p.plans.size());
        assertEquals(53, p.plans.get(0).minutes);
        Activities.Parsed q = Activities.parse("tempó futás 4:45-ös "
                + "kilométerekkel, 8 km");
        assertEquals(1, q.plans.size());
        assertEquals(38, q.plans.get(0).minutes);
    }

    /**
     * A fát hordani és a házimunka is fizikai munka.
     *
     * A „fát hordtam be fél órát" és a „házimunka, kb 3 óra" eddig válasz
     * nélkül maradt. A puszta „munka" ülőmunka-szó marad: a „sok volt ma a
     * munka, 10 óra ülés" továbbra sem mozgás.
     */
    @Test public void houseworkIsPhysicalWork() {
        Activities.Parsed p = Activities.parse("fát hordtam be délután "
                + "fél órát");
        assertEquals(1, p.plans.size());
        assertEquals("munka", p.plans.get(0).kind.id);
        Activities.Parsed q = Activities.parse("házimunka egész délelőtt, "
                + "kb 3 óra");
        assertEquals(1, q.plans.size());
        assertEquals(180, q.plans.get(0).minutes);
        assertEquals(0, Activities.parse("sok volt ma a munka, 10 óra "
                + "ülés").plans.size());
    }

    /**
     * A pótlás jelen ideje terv – a napló pótlása viszont megtörtént edzés.
     *
     * A „hétvégén pótolom az edzést" egy majdani edzés, mégis bekerült a
     * naplóba. A „pótolom: tegnap 30 perc jóga" ellenben utólag beírt,
     * megtörtént edzés – a napló-szó és a „tegnap" felmenti. A múlt idejű
     * „bepótoltam" a ragja miatt eleve valódi edzés.
     */
    @Test public void makingUpAWorkoutLaterIsAPlan() {
        assertEquals(0, Activities.parse("a hétvégén pótolom "
                + "az edzést").plans.size());
        assertEquals(1, Activities.parse("kimaradt a szerdai edzés, ma "
                + "bepótoltam: 45 perc kondi").plans.size());
        assertEquals(1, Activities.parse("elfelejtettem beírni, pótolom: "
                + "tegnap 30 perc jóga").plans.size());
    }

    /**
     * A jelzős osztálynév egy edzés – a fej-szó dönt.
     *
     * Az „alakformáló torna 50 perc" elejéből tánc, a végéből jóga lett:
     * két bejegyzés egyetlen óráról. Magyarul a fej-szó áll hátul, ezért
     * az egymás melletti két sport-tőből az első a jelző. Kivétel az
     * általános fej-szó: a „box edzés" edzése csak annyit mond, hogy
     * edzés volt – ott a konkrét sport nyer.
     */
    @Test public void aModifierClassNameIsOneWorkout() {
        Activities.Parsed p = Activities.parse("alakformáló torna 50 perc");
        assertEquals(1, p.plans.size());
        assertEquals("joga", p.plans.get(0).kind.id);
        assertEquals(50, p.plans.get(0).minutes);
        Activities.Parsed q = Activities.parse("box edzés 60 perc");
        assertEquals(1, q.plans.size());
        assertEquals("harcmuveszet", q.plans.get(0).kind.id);
        // A vesszős felsorolás két külön edzés marad.
        assertEquals(2, Activities.parse("futás, úszás: 5 km és 1 km")
                .plans.size());
    }

    /**
     * A termek óranevei is edzések: zsírégető óra, aqua fitnesz.
     *
     * A „zsírégető órán voltam, 45 perc" étel-oldalon egy kanál OLAJ lett
     * (a zsír tövén ült), edzés-oldalon semmi. A puszta „zsírégető" nem
     * lehet tő – az időzítős „Zsírégető HIIT" program neve program marad –,
     * az óra szóval együtt viszont a terem kardió-osztálya. Az „aqua
     * fitnesz" eddig üresen jött vissza, mert a puszta „fitnesz" nem volt
     * sportszó.
     */
    @Test public void gymClassNamesAreWorkouts() {
        Activities.Parsed p = Activities.parse("zsírégető órán voltam, 45 perc");
        assertEquals(1, p.plans.size());
        assertEquals("tanc", p.plans.get(0).kind.id);
        Activities.Parsed q = Activities.parse("aqua fitnesz 45 perc");
        assertEquals(1, q.plans.size());
        assertEquals("egyeb", q.plans.get(0).kind.id);
        assertEquals(null, Activities.kindByText("Zsírégető HIIT"));
    }

    /**
     * Az edzővel TÖLTÖTT óra edzés – a vele folytatott beszélgetés nem.
     */
    @Test public void anHourSpentWithTheTrainerIsAWorkout() {
        Activities.Parsed p = Activities.parse("edzővel töltöttem egy órát, "
                + "láb nap");
        assertEquals(1, p.plans.size());
        assertEquals(0, Activities.parse("beszéltem az edzővel a heti "
                + "tervről").plans.size());
    }

    /**
     * A „toltam a vasat" a súlyzózás szlengje – edzés, nem vasalás.
     *
     * A konditerem nyelvén a vas a súlyzó: a „toltam a vasat egy órát"
     * eddig üresen jött vissza, mert egyik sportszó sem volt benne. A tő
     * a teljes szókapcsolat – a puszta „vas" a vasárnapban és a
     * vasalásban is benne van, ezért az tilos.
     */
    @Test public void pushingTheIronIsGymSlang() {
        Activities.Parsed p = Activities.parse("toltam a vasat 1 órát");
        assertEquals(1, p.plans.size());
        assertEquals("kondi", p.plans.get(0).kind.id);
        Activities.Parsed q = Activities.parse("kivasaltam az ingeket este");
        assertEquals(0, q.plans.size());
        Activities.Parsed r = Activities.parse("vasárnap pihentem");
        assertEquals(0, r.plans.size());
    }

    /**
     * A pályán lerakott körök EGY futás – nem annyi edzés, ahány kör.
     *
     * A „leraktam 10 kört a pályán" sportnév nélkül is futást jelent: a
     * kör + pálya páros az atlétikai pályát mondja ki. A tíz eddig
     * darabszámnak látszott, és tíz külön bejegyzés lett belőle – a szám
     * a körök száma egyetlen edzésen belül.
     */
    @Test public void lapsOnTheTrackAreOneRun() {
        Activities.Parsed p = Activities.parse("leraktam 10 kört a pályán");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(1, p.plans.get(0).count);
        Activities.Parsed q = Activities.parse("mentem 10 kört a gokartpályán");
        assertEquals(0, q.plans.size());
    }

    /**
     * A warm up és a cool down ideje nem az edzésé – de csak a sajátjuk.
     *
     * A „warm up 5 perc, wod 20 perc, cool down 5 perc" wod-ja öt percet
     * kapott: az idő utáni szó-vizsgálat átlépte a vesszőt, és a húsz perc
     * mögött a következő tagmondat cool szavát találta meg.
     */
    @Test public void theNextClausesCooldownDoesNotStealTheMainTime() {
        Activities.Parsed p = Activities.parse("warm up 5 perc, "
                + "wod 20 perc, cool down 5 perc");
        assertEquals(1, p.plans.size());
        assertEquals(20, p.plans.get(0).minutes);
        // Az angol szavas bemelegítés a saját tagmondatában továbbra is
        // kiesik, ahogy a magyar megfelelője is.
        Activities.Parsed q = Activities.parse("5 perc cool down a "
                + "40 perc futás után");
        assertEquals(1, q.plans.size());
        assertEquals(40, q.plans.get(0).minutes);
    }

    /**
     * A sprintek darabszáma ismétlés, nem a percek száma.
     *
     * A „sprintekből 25-öt nyomtam, összesen 30 perc" huszonöt sprintje
     * időként győzött a harminc perc fölött – 25 perc futás lett belőle.
     */
    @Test public void aSprintCountIsRepsNotMinutes() {
        Activities.Parsed p = Activities.parse("sprintekből 25-öt nyomtam, "
                + "összesen 30 perc");
        assertEquals(1, p.plans.size());
        assertEquals(30, p.plans.get(0).minutes);
    }

    /**
     * A görgőn tekerés szobabiciklizés.
     *
     * A görgő (a kinti bringát befogó edzőállvány) mondatai kimaradtak: a
     * „görgőn tekertem 90 percet" nem adott mozgást. A görgőzés szó viszont
     * NEM bicikli: az SMR-hengerezés régóta a jóga/nyújtás családba tartozik.
     */
    @Test public void ridingOnRollersIsCycling() {
        Activities.Parsed p = Activities.parse("görgőn tekertem 90 percet");
        assertEquals(1, p.plans.size());
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(90, p.plans.get(0).minutes);
        Activities.Parsed q = Activities.parse("görgőzés 15 perc");
        assertEquals("joga", q.plans.get(0).kind.id);
    }

    /**
     * A perc/km nem időtartam, hanem tempó.
     *
     * A „10 km futás 6 perc/km tempóval" hat perce lett az edzés hossza a
     * hatvan helyett: a percvadász nem nézte, hogy a perc után törtjel áll.
     */
    @Test public void minutesPerKmIsAPaceNotADuration() {
        Activities.Parsed p = Activities.parse("10 km futás 6 perc/km "
                + "tempóval");
        assertEquals(1, p.plans.size());
        assertEquals(60, p.plans.get(0).minutes);
    }

    /**
     * A mászni ige is falmászás, nem csak a mászás főnév.
     *
     * Az „elmentem falat mászni 2 órára" üresen jött vissza: a fal
     * mozgásformának csak főnévi stemjei voltak (mászás, boulder), a
     * főnévi igeneves köznyelvi alak egyiket sem tartalmazza. A hegyre
     * fölmászás viszont marad túra.
     */
    @Test public void goingWallClimbingIsClimbing() {
        Activities.Parsed p = Activities.parse("elmentem falat mászni "
                + "2 órára");
        assertEquals(1, p.plans.size());
        assertEquals("fal", p.plans.get(0).kind.id);
        assertEquals(120, p.plans.get(0).minutes);
        Activities.Parsed q = Activities.parse("felmásztam a Kékesre, "
                + "3 óra volt");
        assertEquals("tura", q.plans.get(0).kind.id);
    }
    /**
     * A ponttal tagolt ezres lépésszám ezres, nem tizedes.
     *
     * A „ma 12.500 lépés lett meg" tizenkét és fél ezer lépés – az app
     * tizenkét egész öt tized lépésnek olvasta, és négyszáz méter séta
     * lett belőle. Csak a lépés szó előtt élünk vele: a GPS-tizedes
     * („5.300 km") nem ezres tagolás.
     */
    @Test public void aDotThousandsStepCountIsThousands() {
        Activities.Parsed p = Activities.parse("ma 12.500 lépés lett meg");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(Activities.parse("12500 lépést tettem meg")
                .plans.get(0).minutes, p.plans.get(0).minutes);
    }
    /**
     * Az órakor nem köredzés.
     *
     * A „reggel 6-kor edzés" hat órája beleírta a „kor edzes" betűsort a
     * szövegbe, és HATSZOROS köredzés lett belőle. A valódi köredzés és a
     * körszám marad.
     */
    @Test public void trainingAtSixOclockIsNotSixCircuits() {
        Activities.Parsed p = Activities.parse("ma reggel 6-kor edzés: "
                + "45 perc kerékpár");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(1, Activities.parse("6-kor edzés").plans.get(0).count);
        // A körszám továbbra is szorzó, a köredzés köredzés.
        assertEquals(3, Activities.parse("3 kör edzés, mindegyik 10 perc")
                .plans.get(0).count);
        assertEquals("kondi", Activities.parse("köredzés 40 perc")
                .plans.get(0).kind.id);
    }

    /**
     * A hónap óta gyűjtött táv összegző, nem egy edzés.
     *
     * A „január óta 120 km-t futottam összesen" kétszáztizenegy napos,
     * százhúsz kilométeres bejegyzésként került volna be – hónapok összege
     * egyetlen edzésként. A konkrét januári futás viszont marad.
     */
    @Test public void aSinceJanuaryTotalIsASummary() {
        assertEquals(0, Activities.parse("január óta 120 km-t futottam "
                + "összesen").plans.size());
        assertEquals(1, Activities.parse("január elején futottam egy "
                + "10 km-est").plans.size());
    }
    /**
     * A kutyasétáltatás séta, nem futás.
     *
     * A „a kutyával mentem egy nagyot, 6 km" hat kilométere gazdátlan
     * távként futásnak számított. A kutyával futás persze futás marad.
     */
    @Test public void walkingTheDogIsAWalk() {
        Activities.Parsed p = Activities.parse("a kutyával mentem egy "
                + "nagyot, 6 km");
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals("futas", Activities.parse("kutyával futottam 5 km-t")
                .plans.get(0).kind.id);
    }

    /**
     * Az ingázás oda-vissza útja egy napi adag.
     *
     * A „biciklivel mentem dolgozni, 2x25 perc" ötven perc tekerés – eddig
     * az intervallum-olvasó vitte el a szorzatot, és huszonöt perc maradt.
     * Munkás szó nélkül a 2x25 marad intervallum.
     */
    @Test public void aCommutePairAddsUp() {
        Activities.Parsed p = Activities.parse("biciklivel mentem "
                + "dolgozni, 2x25 perc");
        assertEquals(50, p.plans.get(0).minutes);
        assertEquals(25, Activities.parse("2x25 perc intervall futás")
                .plans.get(0).minutes);
    }

    /**
     * A görkori táv-alapú, és a korizás ige is korcsolya.
     *
     * A „görkoriztam a rakparton 8 km-t" távja nem tudott a korcsolyára
     * kerülni (nem volt táv-alapú), és külön nyolc kilométeres futás lett.
     * A „koriztunk a jégpályán" pedig üresen jött vissza.
     */
    @Test public void rollerSkatingCarriesItsDistance() {
        Activities.Parsed p = Activities.parse("görkoriztam a rakparton "
                + "8 km-t");
        assertEquals(1, p.plans.size());
        assertEquals("korcsolya", p.plans.get(0).kind.id);
        assertEquals(8.0, p.plans.get(0).km, 0.01);
        assertEquals("korcsolya", Activities.parse("koriztunk a jégpályán "
                + "másfél órát").plans.get(0).kind.id);
    }
    /**
     * Az orvosi tanács nem edzés.
     *
     * A „magas volt a vérnyomásom, a doki szerint mozogjak többet"
     * negyvenöt perc egyéb mozgást írt a naplóba. A felszólító alak
     * (mozogjak, mozogjunk) tanács vagy terv; a múlt idejű „mozogtam"
     * marad.
     */
    @Test public void doctorsAdviceToMoveMoreIsNotAWorkout() {
        assertEquals(0, Activities.parse("magas volt a vérnyomásom, "
                + "a doki szerint mozogjak többet").plans.size());
        assertEquals(120, Activities.parse("sokat mozogtam ma, kb 2 órát")
                .plans.get(0).minutes);
    }
    /**
     * A sportórák nyílt vízi módja úszás.
     *
     * A „Garmin: Open Water 1,2 km 28:45" futásként került be – a táv
     * gazdátlan volt, mert az open water nem volt úszás-stem.
     */
    @Test public void openWaterIsSwimming() {
        assertEquals("uszas", Activities.parse("Garmin: Open Water 1,2 km "
                + "28:45").plans.get(0).kind.id);
        assertEquals("uszas", Activities.parse("nyílt vízi úszás a "
                + "Balatonban 2 km").plans.get(0).kind.id);
    }
    /**
     * A heti terv mondata terv, a folytatása napló.
     *
     * A „Heti terv: hétfő futás, szerda úszás. Ma a hétfői megvolt, 6 km."
     * terv-szava az egészet jövőnek minősítette, és a lefutott hat
     * kilométer is elveszett. A csak-terv bejegyzés továbbra sem edzés.
     */
    @Test public void aWeeklyPlanHeaderDoesNotEatTheDoneRun() {
        Activities.Parsed p = Activities.parse("Heti terv: hétfő futás, "
                + "szerda úszás, péntek kondi. Ma a hétfői megvolt, 6 km.");
        assertEquals(1, p.plans.size());
        assertEquals(6.0, p.plans.get(0).km, 0.01);
        assertEquals(0, Activities.parse("heti terv: 3 futás és 2 kondi")
                .plans.size());
    }
    /**
     * A körszám az „egyenként" mögötti körhosszal szorzódik.
     *
     * A „futottam 4 kört, egyenként 400 m" négyszáz méteres futás lett
     * ezerhatszáz helyett. A többtételes CrossFit-kör („5 kör: 500 m
     * evezés, 15 swing") viszont szándékosan nem szorzódik – ott a
     * szorzás csak a távot vinné, a többi tétel ismétléseit nem.
     */
    @Test public void lapsTimesEachLapLengthMultiply() {
        assertEquals(1.6, Activities.parse("futottam 4 kört, egyenként "
                + "400 m").plans.get(0).km, 0.01);
        assertEquals(1.2, Activities.parse("3 kör 400 m a pályán")
                .plans.get(0).km, 0.01);
    }
    /**
     * Az edzőtábor napjai időszak, a lesiklás sí.
     *
     * A „napi 2 edzés 4 napig" négyese négy PERC edzés lett – a távirati
     * perc-átírás nem zárta ki a nap egységet. A „sítábor egész héten,
     * napi 5 óra lesiklás" pedig üresen jött vissza, mert a lesiklás nem
     * volt sí-stem.
     */
    @Test public void aTrainingCampSpansItsDays() {
        Activities.Parsed p = Activities.parse("napi 2 edzés 4 napig");
        assertEquals(4, p.days);
        assertEquals(8, p.plans.get(0).count);
        assertEquals(45, p.plans.get(0).minutes);
        Activities.Parsed q = Activities.parse("sítábor egész héten, "
                + "napi 5 óra lesiklás");
        assertEquals("si", q.plans.get(0).kind.id);
        assertEquals(300, q.plans.get(0).minutes);
    }
    /**
     * A helyesbítés egysége nem vész el, a törlés-kérés nem új bejegyzés.
     *
     * A „nem 45, hanem 60 perc jóga volt" javító-szabálya a szám mögötti
     * rövid szót ragnak nézte és eldobta – a perc egységgel együtt, így
     * ÖTVEN jóga-alkalom lett a hatvan percből. A „duplán ment be a futás,
     * az egyiket vedd ki" pedig egy HARMADIK futást írt volna be.
     */
    @Test public void aCorrectionKeepsItsUnit() {
        Activities.Parsed p = Activities.parse("nem 45, hanem 60 perc "
                + "jóga volt");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(60, p.plans.get(0).minutes);
        assertEquals(0, Activities.parse("duplán ment be a futás, az "
                + "egyiket vedd ki").plans.size());
        // A ragos javítás változatlanul jó: az egység az első félből jön.
        assertEquals(78.0, BodyParse.parse("nem 80 kg, csak 78 kg volt "
                + "a súlyom").kg, 0.01);
    }
    /**
     * A meccs-szám a megnevezett sporté, a golf sétája maga a golf.
     *
     * Az „asztalitenisz bajnokság, 5 meccset játszottam" öt KÜLÖN egyéb
     * mozgást szült a tenisz mellé; a „golfoztam 18 lyukat, kb 4 óra séta"
     * pedig ugyanazt a délutánt kétszer írta be. A sport nélküli
     * meccs-mondat és a golf előtti önálló séta marad.
     */
    @Test public void matchesBelongToTheirNamedSport() {
        Activities.Parsed p = Activities.parse("asztalitenisz bajnokság, "
                + "5 meccset játszottam");
        assertEquals(1, p.plans.size());
        assertEquals("tenisz", p.plans.get(0).kind.id);
        assertEquals(5, Activities.parse("5 meccset játszottam ma")
                .plans.get(0).count);
        Activities.Parsed g = Activities.parse("golfoztam 18 lyukat, "
                + "kb 4 óra séta");
        assertEquals(1, g.plans.size());
        assertEquals(240, g.plans.get(0).minutes);
        assertEquals(2, Activities.parse("sétáltam egy órát, aztán "
                + "golfoztam").plans.size());
    }
    /**
     * A balett-fitnesz és a pompomcsapat is táncos óra.
     *
     * A „barre workout 50 perc" és a „cheerleading próba 2 óra" üresen
     * jött vissza – egyik szó sem volt tánc-stem.
     */
    @Test public void barreAndCheerleadingAreDanceClasses() {
        assertEquals("tanc", Activities.parse("barre workout 50 perc")
                .plans.get(0).kind.id);
        assertEquals(120, Activities.parse("cheerleading próba 2 óra")
                .plans.get(0).minutes);
    }
    /**
     * Apu edzése nem az én naplóm.
     *
     * Az „apu 10 km-t biciklizett ma" a szülő túrája volt, mégis a saját
     * naplóba került – a becézett alanyok (apu, anyu, nagyi, tesóm) is
     * mások. A saját tagmondat és a közös (tesómmal) edzés marad.
     */
    @Test public void dadsRideIsNotMyWorkout() {
        assertEquals(0, Activities.parse("apu 10 km-t biciklizett ma")
                .plans.size());
        Activities.Parsed p = Activities.parse("anyu jógázott, én "
                + "futottam 5 km-t");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals("foci", Activities.parse("a tesómmal fociztunk "
                + "egy órát").plans.get(0).kind.id);
    }
    /**
     * A pár perc tényleg pár perc, a szám előtti majdnem pedig közelítő.
     *
     * A „pár percet nyújtottam" a mozgásforma alap-negyvenöt percét kapta;
     * a „kis híján egy órán át táncoltunk" tánca pedig teljesen elveszett,
     * mert a híján tagadásnak számított. Az ige előtti majdnem („majdnem
     * elestem") marad tagadás.
     */
    @Test public void aFewMinutesIsAFewMinutes() {
        assertEquals(5, Activities.parse("pár percet nyújtottam lefekvés "
                + "előtt").plans.get(0).minutes);
        assertEquals(60, Activities.parse("kis híján egy órán át "
                + "táncoltunk").plans.get(0).minutes);
        assertEquals(60, Activities.parse("majdnem egy órát futottam")
                .plans.get(0).minutes);
        assertEquals(8.0, Activities.parse("kis híján elestem a futáson, "
                + "de megvolt a 8 km").plans.get(0).km, 0.01);
    }
    /**
     * A box breathing légzés, az alvás előtti perc a jógáé.
     *
     * A „box breathing 5 perc" ötperces HARCMŰVÉSZET lett; a „jóga nidra
     * 30 perc alvás előtt" harminc perce pedig az alvásnak tulajdonítva
     * elveszett, és az alap-45 ment be. Az igazi box és az igazi alvásidő
     * marad.
     */
    @Test public void boxBreathingIsBreathingNotBoxing() {
        Activities.Parsed p = Activities.parse("box breathing 5 perc "
                + "stressz ellen");
        assertEquals("joga", p.plans.get(0).kind.id);
        assertEquals(5, p.plans.get(0).minutes);
        assertEquals(30, Activities.parse("jóga nidra 30 perc alvás előtt")
                .plans.get(0).minutes);
        assertEquals("harcmuveszet", Activities.parse("boxoltam "
                + "30 percet a zsákon").plans.get(0).kind.id);
        assertEquals(30, Activities.parse("autogén tréning este fél óra")
                .plans.get(0).minutes);
    }
    /**
     * A terem kardió-gépeinek angol neve is sport.
     *
     * A „stairmaster 20 perc" és a „sípad gép 15 perc" üresen jött vissza,
     * az „assault bike" és a „ski erg" pedig futásnak számított.
     */
    @Test public void gymCardioMachinesResolveToTheirSports() {
        assertEquals("tura", Activities.parse("stairmaster 20 perc, "
                + "brutál volt").plans.get(0).kind.id);
        assertEquals("si", Activities.parse("sípad gép 15 perc")
                .plans.get(0).kind.id);
        assertEquals("si", Activities.parse("ski erg 1000 m")
                .plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("airbike 4 perc")
                .plans.get(0).kind.id);
    }
    /**
     * A felsorolt távok sorrendben járnak a felsorolt sportoknak.
     *
     * A „futás, úszás: 5 km és 1 km" ötöse a futásé – a közelség-alapú
     * párosítás mégis megcserélte, és öt kilométer ÚSZÁS lett belőle,
     * több mint két órával. Az egy-sportos pár (reggel és délután is
     * futottam, 5 és 7 km) változatlan.
     */
    @Test public void listedDistancesPairInOrder() {
        Activities.Parsed p = Activities.parse("futás, úszás: 5 km és 1 km");
        assertEquals(2, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(5.0, p.plans.get(0).km, 0.01);
        assertEquals("uszas", p.plans.get(1).kind.id);
        assertEquals(1.0, p.plans.get(1).km, 0.01);
        assertEquals(2, Activities.parse("reggel és délután is futottam, "
                + "5 és 7 km").plans.size());
    }
    /**
     * Az emoji is sportnév.
     *
     * A „ma: 🏊 1500m + 🚴 20km" úszása és bringája elveszett, csak egy
     * húsz kilométeres futás maradt. A kiírt sportnév melletti emoji dísz
     * (nem lesz belőle második alkalom), a nézett meccs emojival sem edzés.
     */
    @Test public void anEmojiNamesItsSport() {
        Activities.Parsed p = Activities.parse("ma: 🏊 1500m + 🚴 20km");
        assertEquals(2, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
        assertEquals("kerekpar", p.plans.get(1).kind.id);
        assertEquals(1, Activities.parse("futas 5km 🏃‍♂️").plans.size());
        assertEquals("kondi", Activities.parse("🏋️ 60 perc")
                .plans.get(0).kind.id);
        assertEquals(0, Activities.parse("jó volt a meccs ⚽ néztük a "
                + "tévében").plans.size());
    }
    /**
     * Az edzésterhelés mutatószám, nem edzés.
     *
     * Az „edzésterhelés 320, a Garmin szerint produktív" negyvenöt perc
     * egyéb mozgást írt be. A valódi edzés a maga percével marad.
     */
    @Test public void trainingLoadIsAMetricNotAWorkout() {
        assertEquals(0, Activities.parse("edzésterhelés 320, a Garmin "
                + "szerint produktív").plans.size());
        assertEquals(45, Activities.parse("edzés 45 perc, jó terhelés "
                + "volt").plans.get(0).minutes);
    }
    /**
     * A félbehagyott táv a megtett táv.
     *
     * A „10 km lett volna, de 7-nél leállítottam" hét kilométer futás –
     * a „volna" miatt az egész bejegyzés elveszett, pedig a leállásig
     * megvolt a hét. Ami tényleg elmaradt, az marad terv.
     */
    @Test public void anAbortedRunKeepsItsCoveredDistance() {
        assertEquals(7.0, Activities.parse("10 km lett volna, de 7-nél "
                + "leállítottam").plans.get(0).km, 0.01);
        Activities.Parsed p = Activities.parse("20 km lett volna a "
                + "bringatúra, de 15 km-nél leálltunk az eső miatt");
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(15.0, p.plans.get(0).km, 0.01);
        assertEquals(0, Activities.parse("10 km lett volna, de nem "
                + "mentem el").plans.size());
    }
    /**
     * A ház körüli nehéz munkák igéi is fizikai munkák.
     *
     * A kézi autómosás, az ablakpucolás, a fahasogatás és a szobafestés
     * üresen jött vissza – pedig órákig tartó valódi terhelés.
     */
    @Test public void heavyChoresArePhysicalWork() {
        assertEquals("munka", Activities.parse("autót mostam kézzel egy "
                + "órát").plans.get(0).kind.id);
        assertEquals("munka", Activities.parse("tüzifát hasogattam "
                + "délután").plans.get(0).kind.id);
        assertEquals("munka", Activities.parse("kifestettem a "
                + "gyerekszobát, egész nap ment").plans.get(0).kind.id);
        assertEquals("munka", Activities.parse("ablakot pucoltam egész "
                + "délelőtt").plans.get(0).kind.id);
    }
    /**
     * Az úszók méter nélkül írják a távot.
     *
     * A „4x100 gyors" és az „1500 vegyes" métert mond, de mértékegység
     * híján a táv elveszett, és az alap-45 perc ment be. Csak úszó-
     * mondatban és csak kerek (25-tel osztható) számra él – a bérlet ára
     * és az évszám nem táv.
     */
    @Test public void swimmersWriteMetersWithoutTheUnit() {
        assertEquals(1.5, Activities.parse("1500 vegyes az uszodában")
                .plans.get(0).km, 0.01);
        assertEquals(0.4, Activities.parse("úszás: 4x100 gyors 2:00 "
                + "indulással, 200 levezetés").plans.get(0).km, 0.01);
        assertEquals(0, Activities.parse("úszóbérlet 12000 forintba "
                + "került").plans.size());
        assertEquals("uszas", Activities.parse("delfinezést gyakoroltam "
                + "20 percig").plans.get(0).kind.id);
    }
    /**
     * A termek márka-órái a saját műfajukra esnek.
     *
     * A Body Combat comb-töve CSIRKECOMBOT írt a naplóba edzés helyett; a
     * Hot Iron, a Deepwork, a spinracing, a gerinctréning és a functional
     * training üresen jött vissza.
     */
    @Test public void brandedGymClassesResolveToTheirGenres() {
        assertEquals("harcmuveszet", Activities.parse("les mills "
                + "bodycombat 55'").plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("hot iron 60 perc")
                .plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("spinracing 45 perc")
                .plans.get(0).kind.id);
        assertEquals("joga", Activities.parse("gerinctréning 40 perc")
                .plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("functional training "
                + "60 perc").plans.get(0).kind.id);
    }
    /**
     * A feladott verseny megtett távja megmarad.
     *
     * A „feladtam a versenyt a 30. km-nél görcs miatt" harminc megtett
     * kilométer – a feladás tagadó igéje ÉS a sorszám-maszk (a „30." a
     * heti sorszámnak látszott) együtt az egészet elvitte. A tényleg
     * elmaradt edzés marad kihagyva.
     */
    @Test public void aGivenUpRaceKeepsItsKilometres() {
        assertEquals(30.0, Activities.parse("feladtam a versenyt a "
                + "30. km-nél görcs miatt").plans.get(0).km, 0.01);
        assertEquals("kerekpar", Activities.parse("feladtam a bringatúrát "
                + "a 40 km-nél defekt miatt").plans.get(0).kind.id);
        assertEquals(0, Activities.parse("feladtam, nem megyek el edzeni")
                .plans.size());
    }
    /**
     * A várandósság hete nem e heti időszak, a futóbabakocsi nem séta.
     *
     * A „kismama jóga a 28. héten" hétnapos bejegyzés lett (a sorszám
     * kiesett, a csupasz héten időszaknak látszott); a „babával kocogtam
     * a futóbabakocsival 4 km-t" pedig futás ÉS külön négy kilométeres
     * túra. A sima babakocsis séta és az e heti időszak marad.
     */
    @Test public void aPregnancyWeekIsNotThisWeek() {
        Activities.Parsed p = Activities.parse("kismama jóga a 28. héten");
        assertEquals(1, p.days);
        assertEquals("joga", p.plans.get(0).kind.id);
        Activities.Parsed b = Activities.parse("babával kocogtam a "
                + "futóbabakocsival 4 km-t");
        assertEquals(1, b.plans.size());
        assertEquals("futas", b.plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("babakocsival sétáltam "
                + "5 km-t").plans.get(0).kind.id);
        assertEquals(7, Activities.parse("a héten 3x futottam").days);
    }
    /**
     * A mozgásos videojátékok valódi izzadság, a Fifa nem az.
     *
     * A Beat Saber, a Just Dance és a Ring Fit percei elvesztek, a
     * Supernatural VR-edzés pedig a nevében lakó túra-tő miatt GYALOGTÚRA
     * lett. A táncszőnyeg ugrálása sem duplázódik, a Fifa és a sakk
     * továbbra sem edzés.
     */
    @Test public void fitnessVideoGamesCountButFifaDoesNot() {
        assertEquals("tanc", Activities.parse("Beat Saber 40 perc, jól "
                + "megizzadtam").plans.get(0).kind.id);
        assertEquals("tanc", Activities.parse("Just Dance a gyerekekkel "
                + "45 perc").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("Ring Fit Adventure "
                + "30 perc").plans.get(0).kind.id);
        assertEquals("egyeb", Activities.parse("Supernatural VR edzés "
                + "35 perc").plans.get(0).kind.id);
        assertEquals(1, Activities.parse("táncszőnyegen ugráltunk egy "
                + "órát").plans.size());
        assertEquals(0, Activities.parse("Fifát toltam 3 órát")
                .plans.size());
    }
    /**
     * Az ülő ige órái az idő ELŐTT állva sem edzésórák.
     *
     * A „monitornál görnyedtem 10 órát, este 20 perc gerinctorna" tíz
     * órája HATSZÁZ PERC jógává vált – a desk-szót csak az idő mögött
     * kerestük. Csak igealakra él: a „munka 30 perc kondi" harminc perce
     * a kondié marad.
     */
    @Test public void slouchingHoursBeforeTheNumberAreNotTraining() {
        assertEquals(20, Activities.parse("a monitornál görnyedtem "
                + "10 órát, este 20 perc gerinctorna")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("laptop előtt ültem 8 órát, "
                + "aztán 30 perc futás").plans.get(0).minutes);
        assertEquals(30, Activities.parse("munka 30 perc kondi")
                .plans.get(0).minutes);
    }
    /**
     * A buli szlengje is tánc.
     *
     * A „lagziban ropta mindenki, én is vagy 2 órát" és a „koncerten
     * pattogtam 2 órát" üresen jött vissza. A ropi rágcsa marad, a
     * serpenyőben pattogó kukorica sem edzés.
     */
    @Test public void partySlangCountsAsDancing() {
        assertEquals("tanc", Activities.parse("koncerten pattogtam "
                + "2 órát").plans.get(0).kind.id);
        assertEquals(120, Activities.parse("lagziban ropta mindenki, "
                + "én is vagy 2 órát").plans.get(0).minutes);
        assertEquals(0, Activities.parse("a kukorica pattogott a "
                + "serpenyőben").plans.size());
    }
    /**
     * A húsz fölötti km/h bringasebesség, a bringás kocogás tekerés.
     *
     * A „28 km/h átlagsebességgel 40 km" négyórás FUTÁS lett sportnév
     * híján; a „kocogtunk a bringával" kocogása pedig külön futást szült.
     * A reggeli valódi kocogás és a sétatempójú km/h marad.
     */
    @Test public void twentyPlusKmhMeansCycling() {
        assertEquals("kerekpar", Activities.parse("28 km/h "
                + "átlagsebességgel 40 km").plans.get(0).kind.id);
        Activities.Parsed p = Activities.parse("25-ös átlaggal kocogtunk "
                + "a bringával 20 km-t");
        assertEquals(1, p.plans.size());
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(2, Activities.parse("reggel kocogtam 5 km-t, "
                + "délután bringáztam 20 km-t").plans.size());
        assertEquals("tura", Activities.parse("8 km/h-val sétáltunk a "
                + "parkban 4 km-t").plans.get(0).kind.id);
    }
    /** A lépéscél -ből ragos beszámolója is lépésszám. */
    @Test public void aStepGoalReportCountsItsResult() {
        Activities.Parsed p = Activities.parse("lépéscél teljesítve: "
                + "10 000-ből 12 340");
        assertEquals(1, p.plans.size());
        assertEquals(12340, p.plans.get(0).steps);
    }
    /**
     * Az angol óra-app szavai is naplóbejegyzések.
     *
     * Az „easy run 40 min", a „steps: 12000" és a „recovery ride 45 min"
     * üresen jött vissza, a „swim 1500m" pedig FUTÁS lett. Egész szóra
     * illesztünk – a brunch-ban lakó run nem futás, az egyes szám „step"
     * a step-aerobiké marad.
     */
    @Test public void englishWatchWordsResolveToSports() {
        assertEquals(40, Activities.parse("easy run 40 min")
                .plans.get(0).minutes);
        assertEquals(12000, Activities.parse("steps: 12000")
                .plans.get(0).steps);
        assertEquals("kerekpar", Activities.parse("recovery ride 45 min "
                + "z1").plans.get(0).kind.id);
        assertEquals("uszas", Activities.parse("swim 1500m")
                .plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("walk 30 min lunch break")
                .plans.get(0).kind.id);
        assertEquals(0, Activities.parse("brunch a lányokkal")
                .plans.size());
    }
    /**
     * A tapadó ó-rövidítés óra, az óra utáni csupasz szám perc.
     *
     * A „kondi 1ó" EGYPERCES kondi lett, a „jóga 1 óra 15" tizenöt perce
     * pedig elveszett. Az órakor-időpont és az ő névmás érintetlen.
     */
    @Test public void attachedHourAbbreviationsWork() {
        assertEquals(60, Activities.parse("kondi 1ó").plans.get(0).minutes);
        assertEquals(75, Activities.parse("jóga 1 óra 15")
                .plans.get(0).minutes);
        assertEquals(90, Activities.parse("séta 1,5ó")
                .plans.get(0).minutes);
        assertEquals(45, Activities.parse("edzés 6 órakor kezdődött, "
                + "45 perc lett").plans.get(0).minutes);
    }
    /**
     * A két félidő összeadódik, a kevés futás nem a fő edzés.
     *
     * Az „edzőmeccs 2x35 perc" harmincöt percnek számított hetven
     * helyett; a „taktikai edzés 90 perc, kevés futással" pedig KILENCVEN
     * PERC FUTÁS lett. Az erőnléti és az „utat másztam a falon" üresen
     * jött vissza.
     */
    @Test public void matchHalvesAddUp() {
        assertEquals(70, Activities.parse("edzőmeccs 2x35 perc, végig "
                + "játszottam").plans.get(0).minutes);
        Activities.Parsed t = Activities.parse("taktikai edzés 90 perc, "
                + "kevés futással");
        assertEquals(1, t.plans.size());
        assertEquals("egyeb", t.plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("erőnléti a csapattal "
                + "45 perc").plans.get(0).kind.id);
        assertEquals("fal", Activities.parse("6b utat másztam a falon, "
                + "4 kör").plans.get(0).kind.id);
    }
    /**
     * A szervizbe vitt bringa és a kapott jógamatrac nem edzés.
     *
     * A „levittem a bringát szervizbe, új lánc" hatvanperces
     * kerékpározást, a „jógamatracot kaptam szülinapomra" negyvenöt perc
     * jógát írt be. A szerviz utáni próbakör és a matracon nyújtás marad.
     */
    @Test public void bikeServiceAndAGiftedMatAreNotWorkouts() {
        assertEquals(0, Activities.parse("levittem a bringát szervizbe, "
                + "új lánc").plans.size());
        assertEquals(0, Activities.parse("jógamatracot kaptam "
                + "szülinapomra").plans.size());
        assertEquals("kerekpar", Activities.parse("szerviz után tekertem "
                + "egy próbakört, 5 km").plans.get(0).kind.id);
        assertEquals(20, Activities.parse("jógamatracon nyújtottam "
                + "20 percet").plans.get(0).minutes);
    }
    /**
     * Az átfutott jegyzet, a boxba állt autó és a gyalogáldozat nem sport.
     *
     * Az „átfutottam a jegyzeteimet" futást, a „boxba raktam az autót"
     * harcművészetet, a „körbejártam a kérdést" túrát, a „gyalogáldozat"
     * gyaloglást írt be. A hídon átfutás, az igazi box és a tó körbejárása
     * marad.
     */
    @Test public void figurativeSportVerbsAreNotWorkouts() {
        assertEquals(0, Activities.parse("átfutottam a jegyzeteimet este")
                .plans.size());
        assertEquals(0, Activities.parse("boxba raktam az autót a "
                + "mélygarázsban").plans.size());
        assertEquals(0, Activities.parse("körbejártam a kérdést a "
                + "meetingen").plans.size());
        assertEquals(0, Activities.parse("gyalogáldozat a sakkpartiban")
                .plans.size());
        assertEquals("futas", Activities.parse("átfutottam a hídon a "
                + "túloldalra").plans.get(0).kind.id);
        assertEquals(6.0, Activities.parse("körbejártam a tavat, 6 km")
                .plans.get(0).km, 0.01);
        assertEquals(30, Activities.parse("boxoltam 30 percet")
                .plans.get(0).minutes);
    }
    @Test public void aSixPmClassIsNotAnEighteenHourWorkout() {
        // „a 18 órás spinningen voltam" – a hatkor kezdődő óra, nem
        // tizennyolc órányi tekerés.
        Activities.Parsed p = Activities.parse("a 18 órás spinningen voltam");
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(60, p.plans.get(0).minutes);
        p = Activities.parse("a 19 órás jógán voltam");
        assertEquals(45, p.plans.get(0).minutes);
        // Napszak-szóval a kis szám is kezdési idő.
        p = Activities.parse("az este 8 órás edzésen voltam");
        assertTrue(p.plans.get(0).minutes <= 90);
        // A valódi hosszú túra órái viszont megmaradnak.
        p = Activities.parse("10 órás túra volt a Magas-Tátrában");
        assertEquals(600, p.plans.get(0).minutes);
        p = Activities.parse("2 órás túrán voltam");
        assertEquals(120, p.plans.get(0).minutes);
    }

    @Test public void theWorkShiftHoursDoNotBecomeTheRun() {
        // „a 8 órás munkanap után futottam 5 km-t" – a futás fél óra,
        // nem nyolc: a műszak hossza nem a mozgásé.
        Activities.Parsed p =
                Activities.parse("a 8 órás munkanap után futottam 5 km-t");
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(30, p.plans.get(0).minutes);
        p = Activities.parse("8 órás alvás után futottam");
        assertEquals(45, p.plans.get(0).minutes);
        p = Activities.parse("12 órás műszak után sétáltam egy órát");
        assertEquals(60, p.plans.get(0).minutes);
    }

    @Test public void thePaceMinutesAreNotTheDuration() {
        // Az „5 perces tempóval futottam 10 km-t" ötven perc futás,
        // nem öt: a tempó perce nem az edzés hossza.
        Activities.Plan p =
                Activities.parse("5 perces tempóval futottam 10 km-t").plans.get(0);
        assertEquals(50, p.minutes);
        p = Activities.parse("4 perc 50-es tempóban mentem 10 km-t").plans.get(0);
        assertEquals(48, p.minutes);
        p = Activities.parse("6 perc per km tempóval futottam 8 km-t").plans.get(0);
        assertEquals(48, p.minutes);
        // A valódi időtartam tempó-szó mellett is megmarad.
        p = Activities.parse("futottam 40 percet jó tempóban").plans.get(0);
        assertEquals(40, p.minutes);
    }

    @Test public void theCoolDownMinutesDoNotStealTheRun() {
        // A „futás 8 km, 10 perces levezetéssel zártam" nyolc kilométere
        // eddig tízperces futás lett – a levezetés perce nem a futásé.
        Activities.Plan p = Activities
                .parse("futás 8 km, 10 perces levezetéssel zártam").plans.get(0);
        assertEquals(8, p.km, 0.001);
        assertEquals(48, p.minutes);
        p = Activities.parse("5 perces bemelegítés után futottam 8 km-t")
                .plans.get(0);
        assertEquals(48, p.minutes);
    }

    @Test public void anElectricScooterIsAVehicle() {
        // Az „elektromos rollerrel mentem munkába" nem görkorcsolya –
        // a villanyroller jármű. A gyerekkel rollerezés viszont mozgás.
        assertTrue(Activities.parse("elektromos rollerrel mentem munkába")
                .plans.isEmpty());
        assertTrue(Activities.parse("e-rollerrel mentem a boltba")
                .plans.isEmpty());
        assertEquals("korcsolya", Activities
                .parse("rollereztem a gyerekkel fél órát").plans.get(0).kind.id);
    }

    @Test public void escortingGrandmaIsNotMyWorkout() {
        // A „senior tornára kísértem a nagyit" a nagyi tornája, nem az
        // enyém – eddig negyvenöt perc jóga lett a naplómban.
        assertTrue(Activities.parse("senior tornára kísértem a nagyit")
                .plans.isEmpty());
        assertTrue(Activities.parse("elkísértem anyut a gyógytornára")
                .plans.isEmpty());
        // A saját mozgás kísérés mellett is megmarad.
        Activities.Parsed p = Activities
                .parse("elkísértem a barátnőmet futni és én is futottam 5 km-t");
        assertEquals("futas", p.plans.get(0).kind.id);
    }

    @Test public void runnerSlangNumbersAreKilometres() {
        // A „lefutottam egy tízest" tíz kilométer futás – eddig üresen
        // jött vissza. A bolti tízes viszont nem táv.
        Activities.Plan p = Activities.parse("lefutottam egy tízest").plans.get(0);
        assertEquals("futas", p.kind.id);
        assertEquals(10, p.km, 0.001);
        p = Activities.parse("tekertem egy húszast").plans.get(0);
        assertEquals("kerekpar", p.kind.id);
        assertEquals(20, p.km, 0.001);
        assertTrue(Activities.parse("vettem egy tízest a boltban").plans.isEmpty());
    }

    @Test public void goingForALapIsAWalkUnlessDriving() {
        // A „leadtam egy kört a ház körül" séta – az autós kör viszont
        // nem mozgás.
        Activities.Plan p = Activities
                .parse("leadtam egy kört a ház körül").plans.get(0);
        assertEquals("tura", p.kind.id);
        assertEquals(30, p.minutes);
        assertTrue(Activities.parse("mentem egy kört az autóval").plans.isEmpty());
    }

    @Test public void aParkrunIsAlwaysFiveK() {
        // A parkrun távja a világon mindenhol öt kilométer – kimondatlanul
        // is tudjuk. A kimondott táv viszont erősebb.
        Activities.Plan p = Activities.parse("parkrunon voltam reggel").plans.get(0);
        assertEquals("futas", p.kind.id);
        assertEquals(5, p.km, 0.001);
        p = Activities.parse("dupláztam a parkrunt, 10 km lett").plans.get(0);
        assertEquals(10, p.km, 0.001);
        // A családi futónap is futás; a gyerek kísérete viszont nem az enyém.
        assertEquals("futas", Activities
                .parse("családi futónapon voltunk a gyerekekkel").plans.get(0).kind.id);
        assertTrue(Activities.parse("vittem a gyereket a futónapra").plans.isEmpty());
    }

    @Test public void imperialUnitsConvertToMetric() {
        // A „futottam 3 mérföldet" négy-nyolc kilométer, az „5 mile run"
        // nyolc – a mérföld eddig elveszett. A „half marathon" pedig nem
        // hal-étel, hanem félmaraton.
        Activities.Plan p = Activities.parse("futottam 3 mérföldet").plans.get(0);
        assertEquals(4.8, p.km, 0.05);
        p = Activities.parse("5 mile run reggel").plans.get(0);
        assertEquals(8, p.km, 0.05);
        p = Activities.parse("half marathon done, 1:58").plans.get(0);
        assertEquals(21.1, p.km, 0.05);
    }

    @Test public void soakingInThePoolIsNotSwimming() {
        // A „meleg vizes medencében áztattam magam" pihenés – a medence
        // szava mégis háromnegyed óra úszást írt be.
        assertTrue(Activities.parse("meleg vizes medencében áztattam magam")
                .plans.isEmpty());
        assertTrue(Activities.parse("jakuzziban ültünk fél órát")
                .plans.isEmpty());
        // A valódi úszás a medencében marad.
        Activities.Plan p = Activities
                .parse("úsztam 40 hosszt a medencében").plans.get(0);
        assertEquals("uszas", p.kind.id);
    }

    @Test public void booksDreamsAndKidsRunsAreNotMyWorkout() {
        // Az „elkezdtem egy könyvet a maratonfutásról" és az „álmomban
        // futottam egy maratont" negyvenkét kilométert írt be – egyik sem
        // történt meg. A gyerek udvari futása pedig a gyereké.
        assertTrue(Activities.parse("elkezdtem egy könyvet a maratonfutásról")
                .plans.isEmpty());
        assertTrue(Activities.parse("álmomban futottam egy maratont")
                .plans.isEmpty());
        assertTrue(Activities.parse("a gyerek 5 kört futott az udvaron")
                .plans.isEmpty());
        // Az első személyű mozgás a gyerekkel viszont az enyém.
        assertEquals("foci", Activities
                .parse("a gyerekkel játszottam focit fél órát").plans.get(0).kind.id);
        // A futás közbeni hangoskönyv nem viszi el a futást.
        assertEquals(8, Activities
                .parse("futás közben hangoskönyvet hallgattam, 8 km lett")
                .plans.get(0).km, 0.001);
    }

    @Test public void rollingMusclesIsMobilityScrollingIsNot() {
        // A „hengerrel görgettem az izmaimat" izomlazítás, a hírfolyam
        // görgetése viszont nem mozgás. A McKenzie hátgyakorlat.
        assertEquals("joga", Activities
                .parse("habhengerrel görgettem az izmaimat 10 percig")
                .plans.get(0).kind.id);
        assertEquals("joga", Activities
                .parse("mckenzie gyakorlatok reggel").plans.get(0).kind.id);
        assertTrue(Activities.parse("görgettem a hírfolyamot fél órán át")
                .plans.isEmpty());
    }

    @Test public void aPracticeMatchIsAnHourOfPlay() {
        // Az „edzőmeccs 2x30 perc" hatvan perc mozgás – eddig időzítő-terv
        // lett belőle, bejegyzés nélkül.
        Activities.Plan p = Activities.parse("edzőmeccs 2x30 perc").plans.get(0);
        assertEquals(60, p.minutes);
    }

    @Test public void theCouchTo5kNameIsNotTwentyFiveKm() {
        // A „c25k week 3 day 2 kész" huszonöt kilométeres futást írt be –
        // a program neve nem táv. Az igazi 5k szleng viszont marad.
        Activities.Plan p = Activities.parse("c25k week 3 day 2 kész").plans.get(0);
        assertEquals("futas", p.kind.id);
        assertEquals(0, p.km, 0.001);
        assertEquals(5, Activities.parse("lefutottam egy 5k-t").plans.get(0).km, 0.001);
    }

    @Test public void climbingToTheEighthFloorIsMinutesNotAHike() {
        // A „lépcsőn mentem fel a 8. emeletre" kilencven perc séta lett –
        // a sorszám pontja kizárta az emelet-átváltást.
        assertEquals(4, Activities.parse("lépcsőn mentem fel a 8. emeletre")
                .plans.get(0).minutes);
        assertEquals(2, Activities.parse("a 3. emeletig lépcsőztem")
                .plans.get(0).minutes);
        assertEquals(3, Activities.parse("5 emeletet mentem lépcsőn")
                .plans.get(0).minutes);
    }

    @Test public void gymShorthandMinutesAreNotMetres() {
        // A „cardio 20m + súlyok 40m" húsz és negyven PERC – méterként
        // negyven méteres futás lett belőle. Az úszás métere marad.
        Activities.Parsed p = Activities.parse("cardio 20m + súlyok 40m");
        assertEquals(2, p.plans.size());
        assertEquals(20, p.plans.get(0).minutes);
        assertEquals(40, p.plans.get(1).minutes);
        assertEquals(0.4, Activities.parse("úsztam 400m-t").plans.get(0).km, 0.001);
    }

    @Test public void spokenDecimalsKeepTheirWholeParts() {
        // A „futottam három egész öt kilométert" 3,5 km – eddig csak az
        // öt maradt belőle, öt kilométerként.
        assertEquals(3.5, Activities.parse("futottam három egész öt kilométert")
                .plans.get(0).km, 0.001);
        assertEquals(25.5, Activities
                .parse("tekertem huszonöt egész öt kilométert")
                .plans.get(0).km, 0.001);
        assertEquals(5, Activities.parse("futottam öt kilométert")
                .plans.get(0).km, 0.001);
    }

    @Test public void theEnglishWordDistanceIsNotDancing() {
        // Az „activity: running, distance 10.02 km" sorból tánc-tétel is
        // lett – a disTANCe belsejében ott a tánc.
        Activities.Parsed p = Activities
                .parse("activity: running, distance 10.02 km, time 55:31");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
    }

    @Test public void rearrangingFurnitureIsPhysicalWork() {
        // Az „átrendeztük a nappalit" fizikai munka – eddig üresen jött
        // vissza. (A vasalás szándékosan marad kint: lásd a vas-szlenget.)
        assertEquals("munka", Activities
                .parse("átrendeztük a nappalit, sokat emelgettem")
                .plans.get(0).kind.id);
    }

    @Test public void whitewaterAndSnorkelingResolve() {
        // A „raftingoltunk a vadvízen" mellé pohár ásványvíz került, a
        // sznorkelezés pedig üresen jött vissza.
        assertEquals("evezes", Activities.parse("raftingoltunk a vadvízen")
                .plans.get(0).kind.id);
        assertEquals("uszas", Activities
                .parse("sznorkeleztünk a tengerben fél órát").plans.get(0).kind.id);
    }

    @Test public void elevationGainIsNotDistance() {
        // A „szintemelkedés 1200 m a mai túrán" egy 1,2 km-es sétává
        // zsugorodott – a szint métere magasság. Az úszás métere marad.
        Activities.Plan p = Activities
                .parse("túráztam 3 órát, szintemelkedés 800 m").plans.get(0);
        assertEquals(180, p.minutes);
        assertEquals(0, p.km, 0.001);
        assertEquals(0.8, Activities.parse("úsztam 800 m-t").plans.get(0).km, 0.001);
    }

    @Test public void beatingLazinessStillCounts() {
        // A „nem volt kedvem, de azért lefutottam 5 km-t" második fele
        // megtörtént – a kedv hiánya eddig az egészet elvitte. A puszta
        // kedvtelenség viszont továbbra sem edzés.
        assertEquals(5, Activities
                .parse("nem volt kedvem, de azért lefutottam 5 km-t")
                .plans.get(0).km, 0.001);
        assertTrue(Activities.parse("nem volt kedvem futni").plans.isEmpty());
        // Az idei rekord ma történt, nem éves összesítő.
        Activities.Parsed p = Activities
                .parse("a legjobb futásom volt idén: 15 km 1:10 alatt");
        assertEquals(1, p.days);
        assertEquals(15, p.plans.get(0).km, 0.001);
    }

    @Test public void abandonedMinutesAndPlannedDistancesCount() {
        // A „60 perc lett volna a kondi, de 40-nél abbahagytam" negyven
        // perc; „a 10 km-es futásból 6-nál feladtam" hat kilométer.
        assertEquals(40, Activities
                .parse("60 perc lett volna a kondi, de 40-nél abbahagytam")
                .plans.get(0).minutes);
        assertEquals(6, Activities
                .parse("a 10 km-es futásból 6-nál feladtam")
                .plans.get(0).km, 0.001);
    }

    @Test public void aDetailedTriathlonIsItsLegsOnly() {
        // A „triatlon: úszás 1,5 km, kerékpár 40 km, futás 10 km" mellé
        // egy külön 150 perces triatlon-tétel is került – duplán számolva.
        Activities.Parsed p = Activities
                .parse("triatlon: úszás 1,5 km, kerékpár 40 km, futás 10 km");
        assertEquals(3, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
        // A vessző nélküli lánc kötése is a saját sportjához igazodik.
        p = Activities.parse("sprint triatlon 750 m úszás 20 km bringa 5 km futás");
        assertEquals(0.75, p.plans.get(0).km, 0.001);
        assertEquals(20, p.plans.get(1).km, 0.001);
        assertEquals(5, p.plans.get(2).km, 0.001);
        // A puszta triatlon-szó marad gyűjtő.
        assertEquals("triatlon", Activities
                .parse("olimpiai távú triatlont teljesítettem").plans.get(0).kind.id);
    }

    @Test public void aRidingLessonIsRiding() {
        // A „lovas oktatáson voltam" eddig üresen jött vissza.
        assertEquals("egyeb", Activities.parse("lovas oktatáson voltam")
                .plans.get(0).kind.id);
    }

    @Test public void bagWorkAndBeltExamsAreMartialArts() {
        // A „zsákoltam 6 menetet" üresen jött vissza, az „övvizsga volt
        // karatéból" mellé pedig egy pohár ásványvíz került (öVVIZsga).
        assertEquals("harcmuveszet", Activities.parse("zsákoltam 6 menetet")
                .plans.get(0).kind.id);
        assertEquals("harcmuveszet", Activities
                .parse("övvizsga volt karatéból, sikerült").plans.get(0).kind.id);
    }

    @Test public void commonTyposStillResolve() {
        // A „kerkpároztam" (kimaradt e) futás lett a bicikli helyett.
        assertEquals("kerekpar", Activities.parse("kerkpároztam 20 km-t")
                .plans.get(0).kind.id);
    }

    @Test public void homeWorkoutPlatformsResolve() {
        // A Peloton, a Freeletics és a Chloe Ting videó-edzések eddig
        // üresen jöttek vissza.
        assertEquals("kerekpar", Activities.parse("peloton óra 30 perc")
                .plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("freeletics szett 30 perc")
                .plans.get(0).kind.id);
        assertEquals("kondi", Activities.parse("chloe ting has kihívás 15 perc")
                .plans.get(0).kind.id);
    }

    @Test public void frequentativeFormsStillCount() {
        // A „futkarásztam a gyerekek után" és a „jártam egyet a korzón"
        // eddig üresen jött vissza.
        assertEquals("futas", Activities
                .parse("futkarásztam a gyerekek után egész nap").plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("jártam egyet a korzón")
                .plans.get(0).kind.id);
    }

    @Test public void rehearsalRoomsAndTeamGamesResolve() {
        // A „doboltam a próbateremben" kétórás kondi lett a terem-tő
        // miatt; a paintball és a geocaching viszont hiányzott.
        assertTrue(Activities.parse("doboltam a próbateremben két órát")
                .plans.isEmpty());
        assertEquals("egyeb", Activities
                .parse("paintballoztunk a csapatépítésen").plans.get(0).kind.id);
        assertEquals(180, Activities
                .parse("gyalogtúra helyett geocaching volt 3 órát")
                .plans.get(0).minutes);
        assertEquals("kondi", Activities.parse("kondiztam a teremben 60 percet")
                .plans.get(0).kind.id);
    }

    @Test public void aMultiDayTourSpansItsDays() {
        // A „három napos biciklitúra, összesen 180 km" egyetlen napra
        // került; a „30 napos kihívás" viszont program-név, ott a mai
        // adag számít, nem harminc nap.
        Activities.Parsed p = Activities
                .parse("három napos biciklitúra, összesen 180 km");
        assertEquals(3, p.days);
        assertEquals(180, p.plans.get(0).km, 0.001);
        assertEquals(1, Activities
                .parse("30 napos guggolás kihívás, ma 150 guggolás").days);
    }

    @Test public void theTaughtSportBelongsToThePupil() {
        // A „megtanítottam a gyereket biciklizni, két órán át futottam
        // mellette" tekerése a gyereké – az enyém a kétórás futás.
        Activities.Parsed p = Activities.parse(
                "megtanítottam a gyereket biciklizni, két órán át futottam mellette");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(120, p.plans.get(0).minutes);
    }

    @Test public void standingByTheRinkIsNotSkating() {
        // A „punccsal melegedtünk a korcsolyapályánál" órányi korizás
        // lett – a pálya MELLETT állás nem korcsolyázás. A pályán korizás
        // marad.
        assertTrue(Activities.parse("punccsal melegedtünk a korcsolyapályánál")
                .plans.isEmpty());
        assertEquals("korcsolya", Activities
                .parse("koriztam a korcsolyapályán egy órát").plans.get(0).kind.id);
    }

    @Test public void harvestingIsWorkNotEating() {
        // A „krumplit szedtünk a földön 4 órát" negyed kiló főtt
        // burgonyát írt a naplóba – a betakarítás fizikai munka.
        Activities.Plan p = Activities
                .parse("krumplit szedtünk a földön 4 órát").plans.get(0);
        assertEquals("munka", p.kind.id);
        assertEquals(240, p.minutes);
        assertEquals("munka", Activities
                .parse("szüretelni voltunk, egész nap szedtük a szőlőt")
                .plans.get(0).kind.id);
    }

    @Test public void watchingTheKidsPracticeIsNotMyMatch() {
        assertTrue(Activities.parse("v\u00e9gign\u00e9ztem a fiam fociedz\u00e9s\u00e9t").plans.isEmpty());
    }

    @Test public void twoHalvesOfFiveASideAddUp() {
        Activities.Parsed p = Activities.parse("kisp\u00e1ly\u00e1s focin j\u00e1tszottam 2x20 percet");
        assertEquals(1, p.plans.size());
        assertEquals("foci", p.plans.get(0).kind.id);
        assertEquals(40, p.plans.get(0).minutes);
    }

    @Test public void playingTheFullNinetyStillCounts() {
        Activities.Parsed p = Activities.parse("v\u00e9gigj\u00e1tszottam a 90 percet");
        assertEquals(1, p.plans.size());
        assertEquals(90, p.plans.get(0).minutes);
    }

    @Test public void anEveningGymHourAfterTheMorningRunSurvives() {
        Activities.Parsed p = Activities.parse(
                "reggel futottam 5 km-t, este konditerem 1 \u00f3ra");
        assertEquals(2, p.plans.size());
        assertEquals("kondi", p.plans.get(1).kind.id);
        assertEquals(60, p.plans.get(1).minutes);
    }

    @Test public void beingAtTheGymForAnHourIsAStatedHour() {
        Activities.Parsed p = Activities.parse(
                "reggel futottam 5 km-t, este konditeremben voltam egy \u00f3r\u00e1t");
        assertEquals(2, p.plans.size());
        assertEquals(60, p.plans.get(1).minutes);
    }

    @Test public void theGymAsAVenueStillFallsOut() {
        Activities.Parsed p = Activities.parse("45 perc spinning \u00f3ra a teremben");
        assertEquals(1, p.plans.size());
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(45, p.plans.get(0).minutes);
    }

    @Test public void statedWalkMinutesBeatTheFloorEstimate() {
        Activities.Parsed p = Activities.parse("5 emelet, 30 perc s\u00e9ta");
        assertEquals(1, p.plans.size());
        assertEquals(30, p.plans.get(0).minutes);
    }

    @Test public void floorsAloneStillBecomeMinutes() {
        Activities.Parsed p = Activities.parse("20 emeletet l\u00e9pcs\u0151ztem");
        assertEquals(10, p.plans.get(0).minutes);
    }

    @Test public void aNamedMonthTotalSpreadsOverTheWholeMonth() {
        long now = 1_753_869_600_000L; // 2025. julius 30.
        Activities.Parsed p = Activities.parse(
                "janu\u00e1rban 100 km-t futottam \u00f6sszesen", now);
        assertEquals(31, p.days);
        assertEquals(100.0, p.plans.get(0).km, 0.01);
    }

    @Test public void theCurrentMonthTotalCoversTheMonthSoFar() {
        long now = 1_753_869_600_000L; // 2025. julius 30.
        Activities.Parsed p = Activities.parse(
                "j\u00faliusban \u00f6sszesen 60 km-t tekertem", now);
        assertEquals(30, p.days);
    }

    @Test public void aGymTotalIsNotAMonth() {
        long now = 1_753_869_600_000L;
        Activities.Parsed p = Activities.parse(
                "a teremben \u00f6sszesen 45 percet t\u00f6lt\u00f6ttem", now);
        assertEquals(1, p.days);
        assertEquals(45, p.plans.get(0).minutes);
    }

    @Test public void englishGerundExportLinesParse() {
        assertEquals("futas", Activities.parse("polar: running 48 min, avg hr 149")
                .plans.get(0).kind.id);
        assertEquals("kerekpar", Activities.parse("cycling 30 min").plans.get(0).kind.id);
        assertEquals("uszas", Activities.parse("swimming 30 min").plans.get(0).kind.id);
        assertEquals(48, Activities.parse("running 48 min").plans.get(0).minutes);
    }

    @Test public void strengthLabelIsAGymWorkout() {
        Activities.Parsed p = Activities.parse("workout complete: 45 min strength, 380 kcal");
        assertEquals(1, p.plans.size());
        assertEquals("kondi", p.plans.get(0).kind.id);
        assertEquals(45, p.plans.get(0).minutes);
    }

    @Test public void aBareWorkoutWordStillLogs() {
        Activities.Parsed p = Activities.parse("workout 50 min");
        assertEquals(1, p.plans.size());
        assertEquals(50, p.plans.get(0).minutes);
    }

    @Test public void theNorwegianFourByFourIsOneSession() {
        Activities.Parsed p = Activities.parse("norv\u00e9g 4x4 fut\u00e1s");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals("futas", p.plans.get(0).kind.id);
    }

    @Test public void aSwimIntervalIsSwimmingNotRunning() {
        Activities.Parsed p = Activities.parse("\u00fasz\u00f3 intervall 10x50 m");
        assertEquals("uszas", p.plans.get(0).kind.id);
        assertEquals(0.5, p.plans.get(0).km, 0.01);
    }

    @Test public void beingOutWithTheDogIsAWalk() {
        Activities.Parsed p = Activities.parse(
                "a kuty\u00e1val voltam kint f\u00e9l \u00f3r\u00e1t");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(30, p.plans.get(0).minutes);
    }

    @Test public void justBeingOutsideIsNotAWalk() {
        assertTrue(Activities.parse("kint voltunk a teraszon").plans.isEmpty());
    }

    @Test public void aStatedTotalSplitsAcrossTheOccasions() {
        Activities.Parsed p = Activities.parse(
                "h\u00e1romszor s\u00e9t\u00e1ltam ma, \u00f6sszesen 90 perc");
        assertEquals(3, p.plans.get(0).count);
        assertEquals(30, p.plans.get(0).minutes);
        Activities.Parsed q = Activities.parse("k\u00e9tszer \u00fasztam, \u00f6sszesen 2 km");
        assertEquals(1.0, q.plans.get(0).km, 0.01);
    }

    @Test public void megsemWithCsakKeepsTheSmallerRun() {
        Activities.Parsed p = Activities.parse(
                "m\u00e9gsem futottam le a 10 km-t, csak 6 lett");
        assertEquals(6.0, p.plans.get(0).km, 0.01);
    }

    @Test public void megsemAloneCancelsTheRun() {
        assertTrue(Activities.parse("m\u00e9gsem mentem el futni").plans.isEmpty());
    }

    @Test public void lastYearsMonthIsAMemoryNotTodaysLog() {
        assertTrue(Activities.parse("tavaly szeptemberben maraton").plans.isEmpty());
    }

    @Test public void bodyAttackIsACardioClass() {
        assertEquals("tanc", Activities.parse("body attack \u00f3ra").plans.get(0).kind.id);
    }

    @Test public void futsalIsFootballAndStreetballIsBasketball() {
        Activities.Parsed p = Activities.parse("futsal meccs 2x25 perc");
        assertEquals("foci", p.plans.get(0).kind.id);
        assertEquals(50, p.plans.get(0).minutes);
        assertEquals("kosarlabda",
                Activities.parse("streetball a t\u00e9ren").plans.get(0).kind.id);
    }

    @Test public void wimHofBreathingIsAGuidedPractice() {
        Activities.Parsed p = Activities.parse("wim hof l\u00e9gz\u00e9s 15 perc");
        assertEquals("joga", p.plans.get(0).kind.id);
        assertEquals(15, p.plans.get(0).minutes);
    }

    @Test public void apostropheMinutesAreMinutesNotACount() {
        Activities.Parsed p = Activities.parse("90' foci");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(90, p.plans.get(0).minutes);
        assertEquals(45, Activities.parse("45' fut\u00e1s").plans.get(0).minutes);
    }

    @Test public void aSwimPacePerHundredIsNotADistance() {
        Activities.Parsed p = Activities.parse("30 perc \u00fasz\u00e1s, 2:10/100m temp\u00f3");
        assertEquals(1, p.plans.size());
        assertEquals(0.0, p.plans.get(0).km, 0.01);
        assertEquals(30, p.plans.get(0).minutes);
    }

    @Test public void aMakeupRunHappenedToday() {
        Activities.Parsed p = Activities.parse("bep\u00f3toltam a tegnapi fut\u00e1st, 8 km");
        assertEquals(0, p.offset);
        assertEquals(8.0, p.plans.get(0).km, 0.01);
    }

    @Test public void famousLoopsKnowTheirDistance() {
        assertEquals(5.3, Activities.parse("k\u00f6rbefutottam a margitszigetet")
                .plans.get(0).km, 0.01);
        assertEquals(210.0, Activities.parse("k\u00f6rbetekertem a balatont")
                .plans.get(0).km, 0.01);
        // Kimondott táv mellett a kör-táv nem ír felül.
        assertEquals(3.0, Activities.parse("futottam a margitszigeten 3 km-t")
                .plans.get(0).km, 0.01);
    }

    @Test public void commonMinuteTyposStillCount() {
        assertEquals(45, Activities.parse("kondiztam 45 pecet").plans.get(0).minutes);
        assertEquals(30, Activities.parse("futottam 30 pecig").plans.get(0).minutes);
    }

    @Test public void waitingForTheKidsPracticeIsNotMyWorkout() {
        assertTrue(Activities.parse(
                "a gyerek \u00fasz\u00e1s\u00e1ra v\u00e1rtam egy \u00f3r\u00e1t").plans.isEmpty());
    }

    @Test public void theKidsMatchIsNotMyMatch() {
        Activities.Parsed p = Activities.parse(
                "a gyerek focizott, \u00e9n a p\u00e1lya mellett kocogtam 20 percet");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(20, p.plans.get(0).minutes);
    }

    @Test public void theIntervalTotalIsTheWorkoutLength() {
        Activities.Parsed p = Activities.parse(
                "hiit 15 perc, 40 mp munka 20 mp pihen\u0151");
        assertEquals(1, p.plans.size());
        assertEquals(15, p.plans.get(0).minutes);
    }

    @Test public void fartlekIsARunningSession() {
        Activities.Parsed p = Activities.parse("fartlek 40 perc");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(40, p.plans.get(0).minutes);
    }

    @Test public void twoDogWalksAddUp() {
        Activities.Parsed p = Activities.parse("kutyas\u00e9t\u00e1ltat\u00e1s 2x30 perc");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(60, p.plans.get(0).minutes);
    }

    @Test public void thereAndBackMinutesAddUp() {
        Activities.Parsed p = Activities.parse(
                "gyalog mentem a boltba, 15 perc oda \u00e9s 15 vissza");
        assertEquals(30, p.plans.get(0).minutes);
    }

    @Test public void anRpeNumberIsNotAWorkoutCount() {
        Activities.Parsed p = Activities.parse("rpe 7 kondi 45 perc");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.get(0).count);
        assertEquals(45, p.plans.get(0).minutes);
    }

    @Test public void movingMyselfALittleIsStillAWorkout() {
        Activities.Parsed p = Activities.parse("megmozgattam magam 30 percet");
        assertEquals(1, p.plans.size());
        assertEquals(30, p.plans.get(0).minutes);
    }

    @Test public void aBareNumberAfterABallSportIsMinutes() {
        Activities.Parsed p = Activities.parse("foci 30");
        assertEquals("foci", p.plans.get(0).kind.id);
        assertEquals(30, p.plans.get(0).minutes);
        assertEquals(45, Activities.parse("kos\u00e1rlabda 45").plans.get(0).minutes);
    }

    @Test public void aDistanceSportKeepsItsBareNumberAmbiguous() {
        Activities.Parsed p = Activities.parse("fut\u00e1s 10");
        assertEquals(45, p.plans.get(0).minutes);
    }

    @Test public void commonTypistErrorsStillLog() {
        assertEquals(60, Activities.parse("edzetem 1 \u00f3r\u00e1t").plans.get(0).minutes);
        Activities.Parsed p = Activities.parse("set\u00e9ltam egy \u00f3r\u00e1t");
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(60, p.plans.get(0).minutes);
    }

    @Test public void theThousandShorthandAndTrailingStepCountRead() {
        Activities.Parsed p = Activities.parse("15e l\u00e9p\u00e9s");
        assertEquals(15000, p.plans.get(0).steps);
        assertEquals(3000, Activities.parse("ma kev\u00e9s l\u00e9p\u00e9s volt, 3000")
                .plans.get(0).steps);
    }

    @Test public void cyclistSlangDistancesAreCycling() {
        Activities.Parsed p = Activities.parse("megcsavartam egy 10-est");
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertEquals(10.0, p.plans.get(0).km, 0.01);
        assertTrue(Activities.parse("megcsavartam a kupakot").plans.isEmpty());
    }

    @Test public void aDigitRunnerSlangDistanceIsRunning() {
        Activities.Parsed p = Activities.parse("lefutottam egy 10-est");
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(10.0, p.plans.get(0).km, 0.01);
    }

    @Test public void gettingOffOneStopEarlyIsAShortWalk() {
        Activities.Parsed p = Activities.parse(
                "lesz\u00e1lltam egy meg\u00e1ll\u00f3val kor\u00e1bban \u00e9s gyalogoltam");
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(10, p.plans.get(0).minutes);
        assertEquals(15, Activities.parse(
                "k\u00e9t meg\u00e1ll\u00f3val kor\u00e1bban sz\u00e1lltam le, gyalog 15 perc")
                .plans.get(0).minutes);
    }

    @Test public void strengthProgramNamesAreGymSessions() {
        assertEquals("kondi", Activities.parse("stronglifts 5x5 ma").plans.get(0).kind.id);
        assertEquals("kondi",
                Activities.parse("german volume training ma").plans.get(0).kind.id);
        assertEquals(50, Activities.parse("split nap ma 50 perc").plans.get(0).minutes);
    }

    @Test public void aBananaSplitIsNotASplitDay() {
        assertTrue(Activities.parse("ban\u00e1n split desszertnek").plans.isEmpty());
    }

    @Test public void aThousandDotDistanceStillReads() {
        Activities.Parsed p = Activities.parse("1.000 m \u00fasz\u00e1s");
        assertEquals("uszas", p.plans.get(0).kind.id);
        assertEquals(1.0, p.plans.get(0).km, 0.01);
        assertEquals(10.5, Activities.parse("futottam 10.5 km-t").plans.get(0).km, 0.01);
    }

    @Test public void aMultilineStrengthLogIsNotTwelveRowingSessions() {
        Activities.Parsed p = Activities.parse(
                "3x10 fekvenyom\u00e1s 60 kg\n3x12 evez\u00e9s 50 kg");
        assertEquals(1, p.days);
        for (Activities.Plan pl : p.plans)
            assertTrue("evezes".equals(pl.kind.id) ? pl.count == 1 : true);
    }

    @Test public void anEmptyResultNeverSpansDays() {
        assertEquals(1, Activities.parse("3x12 evez\u00e9s 50 kg").days);
    }

    @Test public void nincsEdzesIsNotAWorkout() {
        assertTrue(Activities.parse("beteg vagyok, ma nincs edz\u00e9s").plans.isEmpty());
        assertTrue(Activities.parse("ma nincs edz\u00e9s").plans.isEmpty());
    }

    @Test public void theLengthOfAnIllnessIsNotTheWorkoutPeriod() {
        Activities.Parsed p = Activities.parse(
                "beteg voltam egy hetig, ma volt az els\u0151 edz\u00e9s: 30 perc");
        assertEquals(1, p.days);
        assertEquals(30, p.plans.get(0).minutes);
        assertEquals(7, Activities.parse("a h\u00e9ten 3x edzettem").days);
    }

    @Test public void aColonClockTimeIsAnHourNotACount() {
        Activities.Parsed p = Activities.parse("20:15-kor edz\u00e9s");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(20, p.hour);
        assertEquals(18, Activities.parse("18:30-kor kezd\u0151d\u00f6tt a foci").hour);
    }

    @Test public void aRaceTimeIsNotAClockTime() {
        Activities.Parsed p = Activities.parse("fut\u00e1s 5 km 25:30 alatt");
        assertEquals(1, p.plans.size());
        assertEquals(26, p.plans.get(0).minutes);
    }

    @Test public void trackLapsMultiplyByTheLapLength() {
        Activities.Parsed p = Activities.parse("10 k\u00f6r a 400 m-es p\u00e1ly\u00e1n");
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(4.0, p.plans.get(0).km, 0.01);
        assertEquals(3.2, Activities.parse(
                "8 k\u00f6rt futottam a 400 m\u00e9teres p\u00e1ly\u00e1n").plans.get(0).km, 0.01);
    }

    @Test public void lapsOnALakeAreNotAnHourOfTheClock() {
        assertEquals(12, Activities.parse("3 k\u00f6r a tavon kajakkal").hour);
        assertEquals(6, Activities.parse("ma 6 kor futottam").hour);
    }

    @Test public void aStepGoalIsNotAWalkButAnAchievedGoalIs() {
        assertTrue(Activities.parse("minden nap 10 000 l\u00e9p\u00e9s a c\u00e9l")
                .plans.isEmpty());
        Activities.Parsed p = Activities.parse("meglett a napi c\u00e9l, 12 000 l\u00e9p\u00e9s");
        assertEquals(1, p.plans.size());
        assertEquals(12000, p.plans.get(0).steps);
        assertEquals(10000, Activities.parse("el\u00e9rtem a c\u00e9lt: 10 000 l\u00e9p\u00e9s")
                .plans.get(0).steps);
    }

    @Test public void aSecondMeterDistanceIsAlsoLogged() {
        Activities.Parsed p = Activities.parse("\u00fasztam 500 m-t, majd m\u00e9g 300 m-t");
        assertEquals(2, p.plans.size());
        assertEquals(0.5, p.plans.get(0).km, 0.01);
        assertEquals(0.3, p.plans.get(1).km, 0.01);
    }

    @Test public void elevationGainIsStillNotASecondWalk() {
        Activities.Parsed p = Activities.parse("t\u00fara 14,8 km 3:45:00 620 m emelked\u00e9s");
        assertEquals(1, p.plans.size());
        assertEquals(14.8, p.plans.get(0).km, 0.01);
    }

    @Test public void swimDrillsAndButterflyAreSwimming() {
        assertEquals("uszas", Activities.parse("l\u00e1btemp\u00f3 deszk\u00e1val 200 m")
                .plans.get(0).kind.id);
        assertEquals("uszas", Activities.parse("kartemp\u00f3 300 m").plans.get(0).kind.id);
        assertEquals("uszas", Activities.parse("pillang\u00f3 100 m").plans.get(0).kind.id);
    }

    @Test public void theChestMachineAndSkateboardStayThemselves() {
        assertTrue(Activities.parse("pillang\u00f3 g\u00e9p 3x12 40 kg").plans.isEmpty()
                || !"uszas".equals(Activities.parse("pillang\u00f3 g\u00e9p 3x12 40 kg")
                        .plans.get(0).kind.id));
        assertEquals("korcsolya", Activities.parse("g\u00f6rdeszk\u00e1ztam egy \u00f3r\u00e1t")
                .plans.get(0).kind.id);
    }

    @Test public void sleepHoursAreNotWorkoutMinutes() {
        Activities.Parsed p = Activities.parse(
                "neh\u00e9z nap, keveset aludtam (5 \u00f3ra), de az\u00e9rt lementem "
                + "30 percre a terembe");
        assertEquals(1, p.plans.size());
        assertEquals(30, p.plans.get(0).minutes);
        assertEquals(45, Activities.parse("8 \u00f3ra alv\u00e1s, 45 perc kondi")
                .plans.get(0).minutes);
        assertEquals(120, Activities.parse("edz\u00e9s 2 \u00f3ra").plans.get(0).minutes);
    }

    @Test public void stepsDuringAStatedActivityDoNotAddASecondEntry() {
        List<Activities.Plan> p = Activities.parse(
                "k\u00f6ly\u00f6kkel j\u00e1tsz\u00f3t\u00e9ren 1,5 \u00f3ra, k\u00f6zben 5000 l\u00e9p\u00e9s").plans;
        assertEquals(1, p.size());
        assertEquals(90, p.get(0).minutes);
        assertEquals(5000, p.get(0).steps);
        // A „közben" a MÁSIK tevékenységhez tartozik: az esti edzés külön sor.
        assertEquals(2, Activities.parse(
                "bev\u00e1s\u00e1rl\u00e1s k\u00f6zben 3000 l\u00e9p\u00e9s, este 40 perc kondi").plans.size());
    }

    @Test public void ebbolNarrowsTheStatedTime() {
        Activities.Parsed p = Activities.parse("az uszod\u00e1ban 45 percet voltam, "
                + "ebb\u0151l kb 30 perc \u00fasz\u00e1s volt, a t\u00f6bbi jakuzzi");
        assertEquals(1, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
        assertEquals(30, p.plans.get(0).minutes);
        // A táv MINŐSÍTÉSE marad a régi: az utolsó 3 km a tizennyolcból van.
        assertEquals(18.0, Activities.parse("18 km, ebb\u0151l az utols\u00f3 3 km temp\u00f3ban")
                .plans.get(0).km, 0.01);
    }

    @Test public void aPastRegretIsNotTheDayOfTodaysWorkout() {
        Activities.Parsed p = Activities.parse("a h\u00e9tv\u00e9g\u00e9n t\u00fal sokat ettem, "
                + "de ma vissza\u00e1lltam: sal\u00e1ta, csirke, \u00e9s 1,5 \u00f3ra bringa");
        assertEquals(0, p.offset);
        assertEquals(1, p.days);
        assertEquals(90, p.plans.get(0).minutes);
        // A tegnapi EDZÉS tegnapi marad.
        assertEquals(10.0, Activities.parse("tegnap futottam 10 km-t, de ma pihenek")
                .plans.get(0).km, 0.01);
    }

    @Test public void takingTheStairsInsteadOfTheLiftCounts() {
        Activities.Parsed p = Activities.parse("ma nem volt id\u0151m edzeni, de a "
                + "l\u00e9pcs\u0151t v\u00e1lasztottam a lift helyett, 12 emelet");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(6, p.plans.get(0).minutes);
        // A sport HELYETT tovább is kiesik: a futás elmaradt, az úszás megvolt.
        Activities.Parsed q = Activities.parse("fut\u00e1s helyett \u00fasztam 1 km-t");
        assertEquals(1, q.plans.size());
        assertEquals("uszas", q.plans.get(0).kind.id);
    }

    @Test public void aCoachsPlanIsNotAWorkout() {
        Activities.Parsed p = Activities.parse("az edz\u0151 adott egy \u00faj tervet: "
                + "3x heti kondi, de ma m\u00e9g csak 20 perc bicikli");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals("kerekpar", p.plans.get(0).kind.id);
        assertTrue(Activities.parse("3x heti kondi a terv").plans.isEmpty());
        // A tervezett helyett a valódi marad.
        assertEquals(6.0, Activities.parse("a tervezett 10 helyett 6 km lett")
                .plans.get(0).km, 0.01);
    }

    @Test public void takingTheDogOutIsAWalk() {
        Activities.Parsed p = Activities.parse(
                "a kuty\u00e1t vittem ki k\u00e9tszer, \u00f6sszesen 40 perc");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(2, p.plans.get(0).count);
        assertEquals(20, p.plans.get(0).minutes);
    }

    @Test public void bikeRepairAndAFutureClassAreNotWorkouts() {
        assertTrue(Activities.parse("kifogyott a bring\u00e1m gumija").plans.isEmpty());
        assertTrue(Activities.parse("megjav\u00edttattam a ker\u00e9kp\u00e1romat").plans.isEmpty());
        assertTrue(Activities.parse("a tanfolyam a teremben lesz").plans.isEmpty());
        // A megtett út marad.
        assertEquals(20.0, Activities.parse("tekertem 20 km-t").plans.get(0).km, 0.01);
    }

    /**
     * A f\u00e9lbehagyott edz\u00e9s annyi perc, amennyi megvolt bel\u0151le. A sport
     * nev\u00e9vel ez eddig is m\u0171k\u00f6d\u00f6tt, a semleges \u201eedz\u00e9s" sz\u00f3val nem.
     */
    @Test
    public void anAbandonedWorkoutKeepsTheMinutesItLasted() {
        Activities.Parsed p = Activities.parse("edz\u00e9s 20 perc ut\u00e1n feladtam");
        assertEquals(1, p.plans.size());
        assertEquals(20, p.plans.get(0).minutes);
        assertEquals(20, Activities.parse("az edz\u00e9st 20 perc ut\u00e1n abbahagytam")
                .plans.get(0).minutes);
    }

    /**
     * A BEFEJEZ\u00c9S tagad\u00e1sa nem az edz\u00e9s tagad\u00e1sa: a mondat m\u00e1sik fele
     * \u00e9pp azt mondja meg, mennyi lett bel\u0151le.
     */
    @Test
    public void failingToFinishStillKeepsWhatWasDone() {
        Activities.Parsed p = Activities.parse(
                "nem b\u00edrtam befejezni az edz\u00e9st, 20 perc ut\u00e1n feladtam");
        assertEquals(1, p.plans.size());
        assertEquals(20, p.plans.get(0).minutes);
    }

    /** A puszta tagad\u00e1s viszont marad tagad\u00e1s. */
    @Test
    public void aPlainNegationIsStillNoWorkout() {
        assertTrue(Activities.parse("nem b\u00edrtam futni ma").plans.isEmpty());
        assertTrue(Activities.parse("edz\u00e9s elmaradt ma").plans.isEmpty());
    }

    /**
     * A JELZ\u0150S bemeleg\u00edt\u0151/levezet\u0151 maga a mozg\u00e1s: a „10 perc levezet\u0151
     * ny\u00fajt\u00e1s" t\u00edz perc ny\u00fajt\u00e1s, nem egy m\u00e1sik edz\u00e9s f\u00fcggel\u00e9ke.
     */
    @Test
    public void anAdjectiveWarmupIsTheWorkoutItself() {
        Activities.Parsed p = Activities.parse("20 perc fut\u00e1s, 10 perc levezet\u0151 ny\u00fajt\u00e1s");
        assertEquals(2, p.plans.size());
        assertEquals(20, p.plans.get(0).minutes);
        assertEquals(10, p.plans.get(1).minutes);
    }

    /** A f\u00fcggel\u00e9k viszont f\u00fcggel\u00e9k marad. */
    @Test
    public void aStandaloneWarmupStaysAnAppendix() {
        Activities.Parsed p = Activities.parse("20 perc bemeleg\u00edt\u00e9s + 40 perc foci");
        assertEquals(1, p.plans.size());
        assertEquals(40, p.plans.get(0).minutes);
    }

    /**
     * A „TEMP\u00d3S" jelz\u0151 nem temp\u00f3-\u00e9rt\u00e9k: a „40 perc temp\u00f3s gyaloglás"
     * negyven perc s\u00e9ta – eddig negyvenperces kilom\u00e9ternek olvasta.
     */
    @Test
    public void aBriskAdjectiveIsNotAPace() {
        assertEquals(40, Activities.parse("40 perc temp\u00f3s gyalogl\u00e1s")
                .plans.get(0).minutes);
        assertEquals(50, Activities.parse("50 perc temp\u00f3s bringa")
                .plans.get(0).minutes);
        // A val\u00f3di temp\u00f3 marad temp\u00f3: \u00f6t perc/km t\u00edz kilom\u00e9teren \u00f6tven perc.
        assertEquals(50, Activities.parse("5 perces temp\u00f3val futottam 10 km-t")
                .plans.get(0).minutes);
    }

    /**
     * A S\u00cdT\u00daRA s\u00edz\u00e9s, nem gyalogl\u00e1s: a n\u00e9gy \u00f3r\u00e1t eddig a „t\u00fara" sz\u00f3t\u0151
     * vitte el, \u00e9s a napl\u00f3ba s\u00e9ta ker\u00fclt.
     */
    @Test
    public void aSkiTourIsSkiingNotHiking() {
        Activities.Parsed p = Activities.parse("s\u00edt\u00fara 4 \u00f3ra a hegyekben");
        assertEquals(1, p.plans.size());
        assertEquals(240, p.plans.get(0).minutes);
        assertEquals("si", p.plans.get(0).kind.id);
        // A sima t\u00fara marad t\u00fara.
        assertEquals("tura", Activities.parse("t\u00fara 2 \u00f3ra").plans.get(0).kind.id);
    }

    /** Az angol „surfing" is mozg\u00e1s – eddig semmi nem lett bel\u0151le. */
    @Test
    public void surfingIsAWorkout() {
        Activities.Parsed p = Activities.parse("surfing 2 \u00f3ra a Balatonon");
        assertEquals(1, p.plans.size());
        assertEquals(120, p.plans.get(0).minutes);
    }

    /**
     * A TIZEDES „k" ugyanaz a r\u00f6vid\u00edt\u00e9s: a „8,5k l\u00e9p\u00e9s" nyolcezer\u00f6tsz\u00e1z.
     * Eg\u00e9sz sz\u00e1mmal ez eddig is ment, tizedessel a bejegyz\u00e9s elveszett.
     */
    @Test
    public void aDecimalThousandSuffixWorksToo() {
        Activities.Parsed p = Activities.parse("ma l\u00e9ptem 8.5k l\u00e9p\u00e9st");
        assertEquals(1, p.plans.size());
        assertEquals(8500, p.plans.get(0).steps);
        // Fut\u00e1sn\u00e1l ugyanez t\u00e1v: az „5,5k fut\u00e1s" \u00f6t \u00e9s f\u00e9l kilom\u00e9ter.
        assertEquals(5.5, Activities.parse("5,5k fut\u00e1s").plans.get(0).km, 0.01);
        assertEquals(5.0, Activities.parse("5k fut\u00e1s").plans.get(0).km, 0.01);
    }

    /**
     * A M\u00c1SNAPOS ritmus ugyanaz a heti rend, csak m\u00e1s sz\u00f3val: a „minden
     * m\u00e1snap futok" mai, negyven\u00f6t perces fut\u00e1s lett bel\u0151le.
     */
    @Test
    public void anEveryOtherDayHabitIsNotAWorkout() {
        assertTrue(Activities.parse("minden m\u00e1snap futok").plans.isEmpty());
        assertTrue(Activities.parse("minden h\u00e9tv\u00e9g\u00e9n t\u00far\u00e1zok").plans.isEmpty());
        assertTrue(Activities.parse("hetente k\u00e9tszer \u00faszok").plans.isEmpty());
    }

    /** A megt\u00f6rt\u00e9nt edz\u00e9s a szok\u00e1s mellett is megmarad. */
    @Test
    public void arealWorkoutSurvivesNextToAHabit() {
        Activities.Parsed p = Activities.parse("minden m\u00e1snap futok, ma 5 km volt");
        assertEquals(1, p.plans.size());
        assertEquals(5.0, p.plans.get(0).km, 0.01);
    }

    /**
     * A TERVEZETT \u00e9s a MEGLETT: a „10 km-t terveztem, 12 lett bel\u0151le"
     * tizenk\u00e9t kilom\u00e9ter – eddig a tervezett t\u00edz ment be.
     */
    @Test
    public void whatCameOfItBeatsWhatWasPlanned() {
        assertEquals(12.0, Activities.parse("10 km-t terveztem, 12 lett bel\u0151le")
                .plans.get(0).km, 0.01);
        // A sport szava a megtett mennyis\u00e9g mell\u00e9 ker\u00fcl.
        Activities.Parsed p = Activities.parse("40 perc kondit terveztem, 55 lett bel\u0151le");
        assertEquals(1, p.plans.size());
        assertEquals(55, p.plans.get(0).minutes);
        assertEquals("kondi", p.plans.get(0).kind.id);
    }

    /**
     * A JELEN IDEJ\u0170 tagad\u00e1s is tagad\u00e1s: a „hask\u00f6z\u00e9p gyenge, plank nem
     * megy" mondatb\u00f3l egy hatvan perces kondi-bejegyz\u00e9s lett.
     */
    @Test
    public void aPresentTenseNegationBlocksTheEntry() {
        assertTrue(Activities.parse("hask\u00f6z\u00e9p gyenge, plank nem megy").plans.isEmpty());
        // A m\u00e1sik tagmondat val\u00f3di edz\u00e9se megmarad.
        assertEquals(1, Activities.parse("futottam 5 km-t, de a plank nem megy")
                .plans.size());
    }

    /**
     * A MUNKA/PIHEN\u0150 p\u00e1r nem alkalomsz\u00e1m: a „20/10 tabata" \u00e9s a „30/30
     * intervall 10x" perjeles p\u00e1rj\u00e1b\u00f3l t\u00edz, illetve harminc K\u00dcL\u00d6N edz\u00e9s
     * lett – ugyanannyi napra sz\u00e9tter\u00edtve, egyetlen edz\u00e9sb\u0151l.
     */
    @Test
    public void aWorkRestPairIsNotASessionCount() {
        Activities.Parsed p = Activities.parse("20/10 tabata");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(1, Activities.parse("30/30 intervall 10x").plans.get(0).count);
        assertEquals(1, Activities.parse("30/30 intervall 10x").days);
    }

    /**
     * A d\u00e9lut\u00e1ni \u00d3RA sem alkalomsz\u00e1m: a „18 kor edz\u00e9s" hat \u00f3rai edz\u00e9s,
     * nem tizennyolc k\u00fcl\u00f6n alkalom. A „3 k\u00f6r edz\u00e9s" viszont h\u00e1rom k\u00f6r.
     */
    @Test
    public void anAfternoonHourIsNotACircuitCount() {
        Activities.Parsed p = Activities.parse("18 k\u00f6r edz\u00e9s");
        assertEquals(1, p.plans.get(0).count);
        assertEquals(18, p.hour);
        assertEquals(3, Activities.parse("3 k\u00f6r edz\u00e9s, mindegyik 10 perc")
                .plans.get(0).count);
    }

    /**
     * A LIFT HELYETT haszn\u00e1lt l\u00e9pcs\u0151 napi szok\u00e1s, nem kilencven perc t\u00fara:
     * a mondatb\u00f3l m\u00e1sf\u00e9l \u00f3r\u00e1s gyalogl\u00e1s lett a mozg\u00e1sforma alapidej\u00e9b\u0151l.
     */
    @Test
    public void takingTheStairsInsteadOfTheLiftIsNotAHike() {
        assertTrue(Activities.parse("ma csak a l\u00e9pcs\u0151t haszn\u00e1ltam a lift helyett")
                .plans.isEmpty());
        // Az emeletsz\u00e1m viszont val\u00f3di adat.
        Activities.Parsed p = Activities.parse("lift helyett l\u00e9pcs\u0151, 12 emelet");
        assertEquals(1, p.plans.size());
        assertEquals(6, p.plans.get(0).minutes);
    }

    /**
     * A „3x MAX" szettsz\u00e1m, nem alkalomsz\u00e1m: a „h\u00faz\u00f3dzkod\u00e1s saj\u00e1t
     * s\u00fallyal 3x max" H\u00c1ROM k\u00fcl\u00f6n, hatvan perces edz\u00e9st \u00edrt a napl\u00f3ba.
     */
    @Test
    public void setsToFailureAreNotSeparateSessions() {
        Activities.Parsed p = Activities.parse("h\u00faz\u00f3dzkod\u00e1s saj\u00e1t s\u00fallyal 3x max");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        // A val\u00f3di alkalomsz\u00e1m marad.
        assertEquals(3, Activities.parse("3 edz\u00e9s a h\u00e9ten").plans.get(0).count);
    }

    /** A l\u00e9pcs\u0151 az emeletsz\u00e1m m\u00f6g\u00f6tt is mozg\u00e1s: „12 emelet l\u00e9pcs\u0151". */
    @Test
    public void floorsBeforeTheStairsWordCountToo() {
        assertEquals(6, Activities.parse("12 emelet l\u00e9pcs\u0151").plans.get(0).minutes);
    }

    /**
     * A legr\u00f6videbb sportnevek elg\u00e9pel\u00e9se is tipp: a „futsa" \u00e9s az „uszsa"
     * egy ujjmozdulat a fut\u00e1st\u00f3l \u00e9s az \u00fasz\u00e1st\u00f3l.
     */
    @Test
    public void shortSportTyposGetATipToo() {
        assertEquals("futas", Activities.closestKind("futsa").id);
        assertEquals("uszas", Activities.closestKind("uszsa").id);
        // A cser\u00e9lt bet\u0171 viszont t\u00fal k\u00f6zel visz: a „t\u00e9rdem" nem a terem.
        for (String w : new String[]{"t\u00e9rdem", "hasam", "l\u00e1zas", "valami",
                "fejem", "karom"})
            assertTrue(w, Activities.closestKind(w) == null);
    }

    /**
     * Egyetlen \u00e9teln\u00e9v se hozzon l\u00e9tre mozg\u00e1s-bejegyz\u00e9st.
     *
     * A keresztpr\u00f3ba egyet tal\u00e1lt: a „pre workout" ital mell\u00e9 egy
     * negyven\u00f6t perces „egy\u00e9b mozg\u00e1s" ker\u00fclt a napl\u00f3ba.
     */
    @Test
    public void noFoodNameIsAWorkout() {
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL)
            for (String w : f.nstems) {
                if (w.length() < 4) continue;
                if (!Activities.parse(w).plans.isEmpty())
                    bad.append("\n  ").append(f.name).append(" | ").append(w);
            }
        assertTrue("mozg\u00e1snak l\u00e1tszik:" + bad, bad.length() == 0);
    }

    /**
     * A L\u00c9PCS\u0150 a gyakorlat helysz\u00edne, nem a mozg\u00e1sforma: a „v\u00e1dliemel\u00e9s
     * l\u00e9pcs\u0151n 3x12" mell\u00e9 eddig egy kilencven perces t\u00fara is beker\u00fclt.
     */
    @Test
    public void stairsInsideAnExerciseNameAreOnlyTheVenue() {
        // A l\u00e9pcs\u0151 nem s\u00e9ta: a sorozat konditermi edz\u00e9s, nem gyalogl\u00e1s.
        assertEquals("kondi",
                Activities.parse("v\u00e1dliemel\u00e9s l\u00e9pcs\u0151n 3x12").plans.get(0).kind.id);
        // A l\u00e9pcs\u0151z\u00e9s mint mozg\u00e1s marad.
        assertEquals(8, Activities.parse("l\u00e9pcs\u0151z\u00e9s 15 emelet").plans.get(0).minutes);
    }

    /** A farmer-s\u00e9ta s\u00falyz\u00f3s cipel\u00e9s: nyolcvan m\u00e9teres gyalogl\u00e1s lett bel\u0151le. */
    @Test
    public void aFarmerWalkIsNotAWalk() {
        assertTrue(Activities.parse("farmer walk 2x40 m").plans.isEmpty());
        // A mondat m\u00e1sik fele megmarad.
        assertEquals(1, Activities.parse(
                "futottam 5 km-t, azt\u00e1n farmer walk 2x40 m").plans.size());
    }

    /**
     * A L\u00c1DAUGR\u00c1S nem boksz, a NORDIC CURL nem nordic walking: mindkett\u0151
     * er\u0151gyakorlat, a saj\u00e1t nev\u00e9ben hordva egy m\u00e1sik sport\u00e1g szav\u00e1t.
     */
    @Test
    public void anExerciseNameCarryingAnotherSportsWordStaysAnExercise() {
        assertTrue(Activities.parse("box jump 4x5").plans.isEmpty());
        assertTrue(Activities.parse("box ugr\u00e1s 3x8").plans.isEmpty());
        assertTrue(Activities.parse("nordic curl 3x5").plans.isEmpty());
        // A val\u00f3di sport marad.
        assertEquals("harcmuveszet",
                Activities.parse("boksz edz\u00e9s 45 perc").plans.get(0).kind.id);
        assertEquals("tura",
                Activities.parse("nordic walking 1 \u00f3ra").plans.get(0).kind.id);
    }

    /**
     * Az ISM\u00c9TL\u00c9SSZ\u00c1M nem alkalomsz\u00e1m: a „guggol\u00e1s 4x5 \u00fasz\u00e1s 40 perc"
     * \u00d6T \u00fasz\u00e1st \u00edrt a napl\u00f3ba – a sorozat m\u00e1sodik sz\u00e1ma \u00e1tsziv\u00e1rgott a
     * k\u00f6vetkez\u0151 sport alkalomsz\u00e1m\u00e1ba.
     */
    @Test
    public void aRepCountDoesNotLeakToTheNextSport() {
        Activities.Parsed p = Activities.parse("guggol\u00e1s 4x5 \u00fasz\u00e1s 40 perc");
        assertEquals(2, p.plans.size());
        for (Activities.Plan pl : p.plans) assertEquals(pl.kind.id, 1, pl.count);
        // A kondi saj\u00e1t \u00e1ga marad: a „3x10 fekv\u0151t\u00e1masz" harminc ism\u00e9tl\u00e9se
        // adja az edz\u00e9s hossz\u00e1t, a „2x45 perc foci" pedig k\u00e9t meccs.
        assertEquals(6, Activities.parse("3x10 fekv\u0151t\u00e1masz").plans.get(0).minutes);
        assertEquals(2, Activities.parse("2x45 perc foci").plans.get(0).count);
    }

    /**
     * A r\u00f6vid „h" ugyanaz az \u00f3ra: a t\u00f6m\u00f6r napi sorb\u00f3l („fut\u00e1s: 10km;
     * kondi: 45p; alv\u00e1s: 7h") MINDK\u00c9T mozg\u00e1s n\u00e9gysz\u00e1zh\u00fasz percet kapott –
     * az alv\u00e1s \u00f3r\u00e1j\u00e1b\u00f3l.
     */
    @Test
    public void aSleepHourInShortFormIsNotAWorkoutLength() {
        Activities.Parsed p = Activities.parse(
                "fut\u00e1s: 10km; kondi: 45p; alv\u00e1s: 7h");
        assertEquals(2, p.plans.size());
        for (Activities.Plan pl : p.plans)
            assertTrue(pl.kind.id + " " + pl.minutes, pl.minutes < 400);
        // A mozg\u00e1s saj\u00e1t „h"-ja marad hossz.
        assertEquals(60, Activities.parse("fut\u00e1s 1h").plans.get(0).minutes);
    }

    /**
     * A SOROZAT m\u00e1sodik sz\u00e1ma nem sorsz\u00e1m: a „guggol\u00e1s 4x5. \u00fasz\u00e1s 40 perc"
     * \u00f6t\u00f6se ism\u00e9tl\u00e9s, \u00e9s a mondatb\u00f3l „4x" maradt – abb\u00f3l pedig N\u00c9GY \u00fasz\u00e1s
     * lett a napl\u00f3ban.
     */
    @Test
    public void aSetNotationBeforeAPeriodIsNotAnOrdinal() {
        Activities.Parsed p = Activities.parse("guggol\u00e1s 4x5. \u00fasz\u00e1s 40 perc");
        assertEquals(2, p.plans.size());
        for (Activities.Plan pl : p.plans) assertEquals(pl.kind.id, 1, pl.count);
        assertEquals(1, Activities.parse("3x8. fut\u00e1s 5 km").plans.get(0).count);
        // A val\u00f3di sorsz\u00e1m marad sorsz\u00e1m.
        assertEquals(1, Activities.parse("letudtam a heti 3. fut\u00e1st")
                .plans.get(0).count);
        assertEquals(30.0, Activities.parse("feladtam a versenyt a 30. km-n\u00e9l")
                .plans.get(0).km, 0.01);
    }

    /**
     * TAGMONDATHAT\u00c1RON meg\u00e1llunk: a „kondi: 45p; alv\u00e1s: 7h" napi sor\u00e1ban
     * a negyven\u00f6t perc a kondi\u00e9 – eddig a pontosvessz\u0151 ut\u00e1ni alv\u00e1s-sz\u00f3
     * vitte el, \u00e9s a kondi az alap-hatvan percet kapta.
     */
    @Test
    public void aSleepWordBeyondTheClauseDoesNotTakeTheMinutes() {
        assertEquals(45, Activities.parse("kondi: 45p; alv\u00e1s: 7h")
                .plans.get(0).minutes);
        // A tagmondaton BEL\u00dcLI alv\u00e1s-sz\u00f3 tov\u00e1bbra is elveszi.
        Activities.Parsed p = Activities.parse("aludtam 8 \u00f3r\u00e1t, reggel futottam 5 km-t");
        assertEquals(1, p.plans.size());
        assertEquals(30, p.plans.get(0).minutes);
    }

    /**
     * A M\u0170SZAK \u00f3r\u00e1i nem edz\u00e9s\u00f3r\u00e1k rag n\u00e9lk\u00fcl sem: a „12 \u00f3ra m\u0171szak ut\u00e1n
     * 20 perc s\u00e9ta" s\u00e9t\u00e1ja tizenk\u00e9t \u00d3R\u00c1S gyalogl\u00e1s lett.
     */
    @Test
    public void shiftHoursWithoutTheAdjectiveDoNotBecomeTheWalk() {
        assertEquals(20, Activities.parse("12 \u00f3ra m\u0171szak ut\u00e1n 20 perc s\u00e9ta")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("8 \u00f3ra munka ut\u00e1n 30 perc fut\u00e1s")
                .plans.get(0).minutes);
        // A kerti munka val\u00f3di mozg\u00e1sforma: az \u00f3r\u00e1i maradnak.
        assertEquals(120, Activities.parse("2 \u00f3ra kerti munka").plans.get(0).minutes);
    }

    /**
     * A SZABADID\u0150S \u00fcl\u00e9s \u00f3r\u00e1i sem edz\u00e9s\u00f3r\u00e1k: a „2 \u00f3ra film ut\u00e1n 15 perc
     * ny\u00fajt\u00e1s" ny\u00fajt\u00e1sa k\u00e9t \u00d3R\u00c1S lett.
     */
    @Test
    public void leisureSittingHoursDoNotBecomeTheWorkout() {
        assertEquals(15, Activities.parse("2 \u00f3ra film ut\u00e1n 15 perc ny\u00fajt\u00e1s")
                .plans.get(0).minutes);
        assertEquals(20, Activities.parse("3 \u00f3ra k\u00e1rty\u00e1z\u00e1s, azt\u00e1n 20 perc torna")
                .plans.get(0).minutes);
        // A takar\u00edt\u00e1s saj\u00e1t mozg\u00e1sforma: az \u00f3r\u00e1i maradnak.
        assertEquals(120, Activities.parse("2 \u00f3ra takar\u00edt\u00e1s").plans.get(0).minutes);
    }

    /**
     * A V\u00c1RAKOZ\u00c1S \u00e9s az EDZ\u00c9S K\u00d6R\u00dcLI mozdulatlan percek nem edz\u00e9sid\u0151k:
     * a „45 perc sorban\u00e1ll\u00e1s, azt\u00e1n 30 perc kondi" negyven\u00f6t perces kondit,
     * az „5 perc szauna, 30 perc \u00fasz\u00e1s" \u00f6tperces \u00fasz\u00e1st \u00edrt a napl\u00f3ba.
     */
    @Test
    public void waitingAndSaunaMinutesAreNotTheWorkout() {
        assertEquals(30, Activities.parse("45 perc sorban\u00e1ll\u00e1s, azt\u00e1n 30 perc kondi")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("5 perc szauna, 30 perc \u00fasz\u00e1s")
                .plans.get(0).minutes);
        assertEquals(20, Activities.parse(
                "30 perc k\u00e9s\u00e9s miatt r\u00f6vid\u00edtettem, 20 perc fut\u00e1s")
                .plans.get(0).minutes);
    }

    /**
     * A J\u00c1RM\u0170VEL megtett \u00fat sem mozg\u00e1s: a „20 perc aut\u00f3val a terembe,
     * 45 perc edz\u00e9s" h\u00fasz perces kondi-edz\u00e9st \u00edrt a napl\u00f3ba.
     */
    @Test
    public void travelMinutesByVehicleAreNotTheWorkout() {
        assertEquals(45, Activities.parse("20 perc aut\u00f3val a terembe, 45 perc edz\u00e9s")
                .plans.get(0).minutes);
        // A bicikli \u00e9s a gyaloglás viszont mozg\u00e1s.
        assertEquals(10, Activities.parse("10 perc biciklivel a boltba")
                .plans.get(0).minutes);
    }

    /**
     * A J\u00c1RM\u0170VEL megtett T\u00c1V sem edz\u00e9s-t\u00e1v: a „200 km vezet\u00e9s" k\u00e9tsz\u00e1z
     * kilom\u00e9teres FUT\u00c1ST \u00edrt a napl\u00f3ba – h\u00fasz \u00f3r\u00e1t.
     */
    @Test
    public void kilometresByVehicleAreNotWorkoutDistance() {
        assertTrue(Activities.parse("200 km vezet\u00e9s, semmi mozg\u00e1s").plans.isEmpty());
        Activities.Parsed p = Activities.parse(
                "40 km aut\u00f3val a hegyekbe, ott 8 km t\u00fara");
        assertEquals(1, p.plans.size());
        assertEquals(8.0, p.plans.get(0).km, 0.01);
        assertEquals(1, Activities.parse("30 km-t vezettem, azt\u00e1n 5 km fut\u00e1s")
                .plans.size());
        // A bicikli t\u00e1vja marad.
        assertEquals(40.0, Activities.parse("40 km biciklivel").plans.get(0).km, 0.01);
    }

    /**
     * A HELY T\u00c1VOLS\u00c1GA \u00e9s a MAGASS\u00c1G nem megtett t\u00e1v: az „a terem 2 km-re
     * van t\u0151lem" k\u00e9t kilom\u00e9teres fut\u00e1st, az „a hegy teteje 700 m magasan
     * van" h\u00e9tsz\u00e1z m\u00e9terest \u00edrt a napl\u00f3ba.
     */
    @Test
    public void aPlacesDistanceIsNotACoveredDistance() {
        for (Activities.Plan p : Activities.parse("a terem 2 km-re van t\u0151lem").plans)
            assertEquals(p.kind.id, 0.0, p.km, 0.01);
        assertTrue(Activities.parse("a hegy teteje 700 m magasan van").plans.isEmpty());
        // Ha meg is tette, a t\u00e1v marad.
        assertEquals(10.0, Activities.parse(
                "10 km-re volt a start, oda is gyalogoltam").plans.get(0).km, 0.01);
    }

    /**
     * A borra tett megjegyz\u00e9s nem viheti el az edz\u00e9st: a „30 perc laza
     * bringa. 3 korty bor." bringája elt\u0171nt a napl\u00f3b\u00f3l, mert a „korty"
     * k\u00f6rsz\u00e1mnak l\u00e1tszott, \u00e9s az eg\u00e9sz mondat id\u0151z\u00edt\u0151-tervv\u00e9 v\u00e1lt.
     */
    @Test
    public void aSipOfWineDoesNotSwallowTheWorkout() {
        Activities.Parsed p = Activities.parse("30 perc laza bringa. 3 korty bor.");
        assertEquals(1, p.plans.size());
        assertEquals(30, p.plans.get(0).minutes);
        assertEquals("kerekpar", p.plans.get(0).kind.id);
    }

    /**
     * A K\u00d3RH\u00c1Z \u00e9s a rendel\u0151 \u00f3r\u00e1i is \u00fclve telnek: az „1 \u00f3ra k\u00f3rh\u00e1zban
     * voltam, ut\u00e1na 20 perc s\u00e9ta" hatvan perces s\u00e9t\u00e1t \u00edrt a napl\u00f3ba.
     */
    @Test
    public void hospitalHoursAreNotWalkingHours() {
        assertEquals(20, Activities.parse(
                "1 \u00f3ra k\u00f3rh\u00e1zban voltam, ut\u00e1na 20 perc s\u00e9ta")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("2 \u00f3ra orvosn\u00e1l, este 30 perc bringa")
                .plans.get(0).minutes);
    }

    /**
     * A M\u00c9TER ki\u00edrva is magass\u00e1g: a „t\u00fara 850 m\u00e9ter
     * szintemelked\u00e9ssel" a r\u00f6vid\u00edt\u00e9s hi\u00e1ny\u00e1ban 0,85 km-es t\u00far\u00e1v\u00e1
     * zsugorodott – egy eg\u00e9sz napos hegymenet helyett.
     */
    @Test
    public void spelledOutMetresOfClimbAreNotDistance() {
        Activities.Parsed p = Activities.parse(
                "szombaton 3 \u00f3ra 20 perc t\u00fara 850 m\u00e9ter szintemelked\u00e9ssel");
        assertEquals(1, p.plans.size());
        assertEquals(0.0, p.plans.get(0).km, 0.001);
        assertEquals(200, p.plans.get(0).minutes);
        // A r\u00f6vid\u00edtett alak eddig is j\u00f3 volt, maradjon az.
        Activities.Parsed q = Activities.parse("t\u00fara 1200 m szintemelked\u00e9s");
        assertEquals(0.0, q.plans.get(0).km, 0.001);
        // A VAL\u00d3DI t\u00e1v ford\u00edtva viszont t\u00e1v marad.
        Activities.Parsed r = Activities.parse("800 m\u00e9tert \u00fasztam");
        assertEquals(0.8, r.plans.get(0).km, 0.001);
    }

    /**
     * A H\u00c1ROM NAGY gyakorlat is edz\u00e9s: a „fekvenyom\u00e1s 5x3 100 kg" \u00e9s a
     * „holtemel\u00e9s 5x3 140 kg" bekerült ugyan az er\u0151napl\u00f3ba, de NEM lett
     * bel\u0151le edz\u00e9s – a nap \u00fcresen \u00e1llt a napt\u00e1rban. A guggol\u00e1s r\u00e9g
     * saj\u00e1t t\u0151 volt, a m\u00e1sik kett\u0151 hi\u00e1nyzott.
     */
    @Test
    public void theBigLiftsAreWorkoutsToo() {
        assertEquals("1d+0: 1\u00d7kondi/60", summary("fekvenyom\u00e1s 5x3 100 kg"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("holtemel\u00e9s 5x3 140 kg"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("v\u00e1llnyom\u00e1s 4x8 40 kg"));
        // A guggol\u00e1s v\u00e1ltozatlan.
        assertEquals("1d+0: 1\u00d7kondi/60", summary("guggol\u00e1s 5x3 140 kg"));
    }

    /**
     * A MEGNEVEZETT mozg\u00e1s a bemeleg\u00edt\u00e9s szava el\u0151tt: a „20 perc
     * szobabicikli bemeleg\u00edt\u00e9s ut\u00e1n 30 perc s\u00falyz\u00f3z\u00e1s" h\u00fasz perce
     * val\u00f3di tekeré\u0073 – eddig f\u00fcggel\u00e9kk\u00e9nt kiesett, \u00e9s a bicikli a
     * mozg\u00e1sforma \u00e1tlag\u00e1b\u00f3l kapott hatvan percet: egy h\u00faszperces
     * meleg\u00edt\u00e9sb\u0151l \u00f3r\u00e1s edz\u00e9s lett a napl\u00f3ban.
     */
    @Test
    public void aNamedWarmupKeepsItsOwnMinutes() {
        assertEquals("1d+0: 1\u00d7kerekpar/20, 1\u00d7kondi/30",
                summary("20 perc szobabicikli bemeleg\u00edt\u00e9s ut\u00e1n 30 perc s\u00falyz\u00f3z\u00e1s"));
        assertEquals("1d+0: 1\u00d7futas/10, 1\u00d7kondi/40",
                summary("10 perc fut\u00e1s bemeleg\u00edt\u00e9s ut\u00e1n 40 perc kondi"));
        // A PUSZTA bemeleg\u00edt\u00e9s v\u00e1ltozatlan: mozg\u00e1sn\u00e9v n\u00e9lk\u00fcl f\u00fcggel\u00e9k.
        assertEquals("1d+0: 1\u00d7foci/45",
                summary("15 perc bemeleg\u00edt\u00e9s ut\u00e1n 45 perc foci"));
    }

    /**
     * A MEGTERHELT sorozat hossza nem az ism\u00e9tl\u00e9ssz\u00e1mb\u00f3l j\u00f6n: az „5x5
     * guggol\u00e1s 100 kg" huszon\u00f6t ism\u00e9tl\u00e9se \u00d6T percnek l\u00e1tszott, pedig a
     * r\u00faddal v\u00e9gzett munka java a szettek k\u00f6zti pihen\u00e9s. Egy komplett
     * er\u0151edz\u00e9s ment \u00edgy \u00f6t perck\u00e9nt a napl\u00f3ba \u00e9s a heti \u00f6sszes\u00edt\u0151be.
     */
    @Test
    public void aLoadedBarbellSetIsNotFiveMinutes() {
        assertEquals("1d+0: 1\u00d7kondi/60", summary("5x5 guggol\u00e1s 100 kg"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("8x8 guggol\u00e1s 60 kg"));
        // A S\u00daLY N\u00c9LK\u00dcLI ism\u00e9tl\u00e9s marad becs\u00fclt: a „100 fekv\u0151t\u00e1masz"
        // \u00e9s a k\u00f6r\u00f6s saj\u00e1t tests\u00falyos sor nem v\u00e1ltozik.
        assertEquals("1d+0: 1\u00d7kondi/20", summary("100 fekv\u0151t\u00e1masz"));
        assertEquals("1d+0: 1\u00d7kondi/5", summary("20 kettlebell swing \u00e9s 10 burpee"));
    }

    /**
     * Az EMOM \u00e9s az AMRAP kimondott perce az EG\u00c9SZ blokk\u00e9: az „emom 12
     * perc, 10 kettlebell swing percenk\u00e9nt" tizenk\u00e9t perce a munka
     * hossza – eddig az ism\u00e9tl\u00e9ssz\u00e1mb\u00f3l becs\u00fclt \u00f6t perc ker\u00fclt a
     * napl\u00f3ba, vagyis az edz\u00e9s k\u00e9tharmada elt\u0171nt.
     */
    @Test
    public void theStatedEmomLengthBeatsTheRepEstimate() {
        assertEquals("1d+0: 1\u00d7kondi/12",
                summary("emom 12 perc, 10 kettlebell swing percenk\u00e9nt"));
        assertEquals("1d+0: 1\u00d7kondi/20",
                summary("emom 20 perc, 5 fekv\u0151t\u00e1masz percenk\u00e9nt"));
        assertEquals("1d+0: 1\u00d7kondi/15",
                summary("amrap 15 perc: 5 h\u00faz\u00f3dzkod\u00e1s, 10 fekv\u0151t\u00e1masz, 15 guggol\u00e1s"));
        // A T\u00c1VOLI id\u0151 m\u00e1s mondatban tov\u00e1bbra sem az ism\u00e9tl\u00e9ses t\u00e9tel\u00e9.
        assertEquals("1d+0: 1\u00d7futas/50, 1\u00d7kondi/20",
                summary("10 km fut\u00e1s 50 perc alatt \u00e9s 100 fekv\u0151t\u00e1masz"));
    }

    /**
     * A L\u00c9P\u00c9SSZ\u00c1M elv\u00e1laszt\u00f3 jel n\u00e9lk\u00fcl is l\u00e9p\u00e9ssz\u00e1m: a „l\u00e9p\u00e9ssz\u00e1m
     * 9842" \u00e9s a „napi l\u00e9p\u00e9ssz\u00e1m 9842" – ahogy az \u00f3r\u00e1k \u00e9s a telefonok
     * ki\u00edrj\u00e1k – kettőspont n\u00e9lk\u00fcl \u00fcresen j\u00f6tt vissza, vagyis egy eg\u00e9sz
     * nap gyalogl\u00e1sa elt\u0171nt.
     */
    @Test
    public void theStepCountNeedsNoColon() {
        assertEquals("1d+0: 1\u00d7tura/76", summary("l\u00e9p\u00e9ssz\u00e1m 9842"));
        assertEquals("1d+0: 1\u00d7tura/76", summary("napi l\u00e9p\u00e9ssz\u00e1m 9842"));
        assertEquals("1d+0: 1\u00d7tura/76", summary("l\u00e9p\u00e9ssz\u00e1mom 9842"));
        // A megszokott sorrend v\u00e1ltozatlan.
        assertEquals("1d+0: 1\u00d7tura/76", summary("9842 l\u00e9p\u00e9s"));
        // A L\u00c9P\u00c9SC\u00c9L nem megtett l\u00e9p\u00e9s.
        assertTrue(Activities.parse("l\u00e9p\u00e9sc\u00e9l 10000").plans.isEmpty());
    }

    /**
     * A saj\u00e1t tests\u00falyos klasszikusok eddig hi\u00e1nyoztak: a „20 perc
     * burpee" \u00e9s a „15 perc hasizom" \u00dcRESEN j\u00f6tt vissza – h\u00fasz perc munka
     * t\u0171nt el a napl\u00f3b\u00f3l, pedig a fekv\u0151t\u00e1masz \u00e9s a fel\u00fcl\u00e9s r\u00e9g sz\u00f3t\u0151.
     */
    @Test
    public void theBodyweightClassicsAreWorkoutsToo() {
        assertEquals("1d+0: 1\u00d7kondi/20", summary("20 perc burpee"));
        assertEquals("1d+0: 1\u00d7kondi/15", summary("15 perc hasizom"));
        assertEquals("1d+0: 1\u00d7kondi/15", summary("15 perc jumping jack"));
    }

    /**
     * A gyakorlat ANGOL neve ugyan\u00fagy edz\u00e9snap: a „3x8 benchpress 60kg"
     * beker\u00fclt az er\u0151napl\u00f3ba, de nem lett bel\u0151le edz\u00e9s – a nap \u00fcresen
     * \u00e1llt a napt\u00e1rban. A magyar nevek p\u00e1rja.
     */
    @Test
    public void theEnglishLiftNamesAreWorkoutsToo() {
        assertEquals("1d+0: 1\u00d7kondi/60", summary("3x8 benchpress 60kg"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("deadlift 5x3 140kg"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("squat 3x10 80kg"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("lat pulldown 3x12 50 kg"));
    }

    /**
     * A SZORZ\u00d3JEL is megv\u00e9di az ezres tagol\u00e1st: a „guggol\u00e1s 5x5 100 kg"
     * mondatban az „5 100" ezres tagol\u00e1snak l\u00e1tszott, \u00e9s „5x5100 kg" lett
     * bel\u0151le – onnant\u00f3l a mondat nem volt edz\u00e9s, a nap \u00fcresen \u00e1llt a
     * napt\u00e1rban. Csak a h\u00e1romjegy\u0171 s\u00falyn\u00e1l harapott, teh\u00e1t pont
     * azokn\u00e1l, akik a legt\u00f6bbet emelik.
     */
    @Test
    public void theMultiplierProtectsTheThousandsSeparator() {
        assertEquals("1d+0: 1\u00d7kondi/60", summary("guggol\u00e1s 5x5 100 kg"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("holtemel\u00e9s 3x3 140 kg"));
        // A val\u00f3di ezres tagol\u00e1s v\u00e1ltozatlan.
        assertEquals(7.5, Activities.parse("10 000 l\u00e9p\u00e9s").plans.get(0).km, 0.05);
        assertEquals(1.5, Activities.parse("le\u00fasztam 1 500 m\u00e9tert").plans.get(0).km, 0.01);
    }

    /**
     * A gyakorlat IG\u00c9JE is edz\u00e9s: a „guggoltam 5x5" \u00e9s a „h\u00faz\u00f3dzkodtam
     * 5x5-\u00f6t" beker\u00fclt az er\u0151napl\u00f3ba, de nem lett bel\u0151le edz\u00e9s – a nap
     * \u00fcresen \u00e1llt a napt\u00e1rban. A f\u0151n\u00e9vi alak („5x5 guggol\u00e1s") r\u00e9g\u00f3ta j\u00f3
     * volt: a napl\u00f3 att\u00f3l f\u00fcgg\u00f6tt, melyiket \u00edrja a felhaszn\u00e1l\u00f3.
     */
    @Test
    public void aSetIsAWorkoutEvenWithoutAMovementNoun() {
        assertEquals("1d+0: 1\u00d7kondi/5", summary("h\u00faz\u00f3dzkodtam 5x5-\u00f6t"));
        assertEquals("1d+0: 1\u00d7kondi/12", summary("fel\u00fcltem 3x20-at"));
        assertEquals("1d+0: 1\u00d7kondi/20", summary("leguggoltam 100-at s\u00falyok n\u00e9lk\u00fcl"));
    }

    /**
     * A gyakorlat NEVE n\u00e9ha nem der\u00fcl ki, az edz\u00e9s m\u00e9gis megt\u00f6rt\u00e9nt: a
     * „nyomtam 3x10-et 60 kg-mal" \u00dcRESEN j\u00f6tt vissza – se sorozat, se
     * edz\u00e9s, vagyis a nap egyetlen munk\u00e1ja nyomtalanul elt\u0171nt. A puszta
     * „nyom\u00e1s" t\u00e9nyleg lehet fekvenyom\u00e1s \u00e9s l\u00e1btol\u00e1s is, de a
     * kondiedz\u00e9s biztos, ha sorozatjel\u00f6l\u00e9s, kil\u00f3 \u00e9s emel\u0151-ige \u00e1ll
     * egym\u00e1s mellett.
     */
    @Test
    public void anUnnamedLiftIsStillAGymSession() {
        assertEquals("1d+0: 1\u00d7kondi/60", summary("nyomtam 3x10-et 60 kg-mal"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("emeltem 3x5-\u00f6t 100 kg-mal"));
        assertEquals("1d+0: 1\u00d7kondi/60", summary("h\u00faztam 5x3-at 120 kg-mal"));
        // A h\u00e1rom jel egy\u00fctt kell: a bev\u00e1s\u00e1rl\u00e1s nem edz\u00e9s.
        assertTrue(Activities.parse("vettem 2x10 kg lisztet").plans.isEmpty());
        assertTrue(Activities.parse("nyomtam a gombot").plans.isEmpty());
        // \u00c9s ami M\u00c1S\u00c9, az tov\u00e1bbra sem az \u00e9n edz\u00e9sem.
        assertTrue(Activities.parse(
                "a sr\u00e1c nyomott 3x10-et 100 kg-mal").plans.isEmpty());
    }

    /**
     * A NAPSZAKKAL mondott szok\u00e1s ugyanaz a rend: a „minden reggel futok
     * 5 km-t" mai, \u00f6t kilom\u00e9teres fut\u00e1sk\u00e9nt ker\u00fclt a napl\u00f3ba – egy heti
     * rend le\u00edr\u00e1s\u00e1b\u00f3l. A t\u00f6bbes sz\u00e1m\u00fa alak ugyan\u00edgy: a „h\u00e9tv\u00e9g\u00e9nte
     * t\u00far\u00e1zunk" egy h\u00e9tre elosztott, kilencvenperces t\u00far\u00e1t \u00edrt be.
     */
    @Test
    public void aHabitWithADaypartIsStillAHabit() {
        assertTrue(Activities.parse("minden reggel futok 5 km-t").plans.isEmpty());
        assertTrue(Activities.parse("h\u00e9tv\u00e9g\u00e9nte t\u00far\u00e1zunk").plans.isEmpty());
        assertTrue(Activities.parse("minden d\u00e9lut\u00e1n bringazunk").plans.isEmpty());
        // A MEGT\u00d6RT\u00c9NT f\u00e9l megmarad a szok\u00e1s mellett.
        assertEquals("1d+0: 1\u00d7kondi/45",
                summary("hetente h\u00e1romszor edzek, ma 45 perc kondi volt"));
        assertEquals("1d+0: 1\u00d7futas/60",
                summary("minden reggel futok 5 km-t, de ma 10 km-t futottam"));
    }

    /**
     * A -NTE / -NK\u00c9NT k\u00e9pz\u0151 maga a gyakoris\u00e1g: a „reggelente futok" \u00e9s
     * az „est\u00e9nk\u00e9nt ny\u00fajtok" negyven\u00f6t perces MAI bejegyz\u00e9s lett, pedig
     * a heti rendr\u0151l sz\u00f3l.
     */
    @Test
    public void theAdverbialHabitFormIsAHabitToo() {
        assertTrue(Activities.parse("reggelente futok").plans.isEmpty());
        assertTrue(Activities.parse("est\u00e9nk\u00e9nt ny\u00fajtok").plans.isEmpty());
        // A megt\u00f6rt\u00e9nt reggeli fut\u00e1s v\u00e1ltozatlan.
        assertEquals("1d+0: 1\u00d7futas/30", summary("reggel futottam 5 km-t"));
    }

    /**
     * K\u00c9T napszak, kimondott „is"-sel: a „reggel \u00e9s este is edzettem" EGY
     * negyven\u00f6t perces bejegyz\u00e9s lett, vagyis a nap fele elt\u0171nt a
     * napl\u00f3b\u00f3l, a statisztik\u00e1b\u00f3l \u00e9s az XP-b\u0151l. Mennyis\u00e9g n\u00e9lk\u00fcl eddig
     * egyik \u00e1g sem sz\u00e1molta k\u00e9tszer.
     */
    @Test
    public void twiceADayCountsTwiceWithoutAnAmount() {
        assertEquals(2, Activities.parse("reggel \u00e9s este is edzettem")
                .plans.get(0).count);
        assertEquals(2, Activities.parse("reggel \u00e9s este is futottam")
                .plans.get(0).count);
        assertEquals(2, Activities.parse("d\u00e9lel\u0151tt \u00e9s d\u00e9lut\u00e1n is futottam")
                .plans.get(0).count);
        // Az „is" a kulcs: k\u00e9t napszak \u00f6nmag\u00e1ban nem k\u00e9t edz\u00e9s.
        assertEquals(1, Activities.parse("reggel f\u00e1radt voltam, este edzettem")
                .plans.get(0).count);
    }

    /**
     * A JELZ\u0150 nem szak\u00edtja meg a darabsz\u00e1mot: a „k\u00e9t k\u00fcl\u00f6nb\u00f6z\u0151 edz\u00e9s"
     * \u00e9s a „h\u00e1rom r\u00f6vid fut\u00e1s" EGY alkalomk\u00e9nt ment be, mert a sz\u00e1m \u00e9s a
     * mozg\u00e1s szava k\u00f6z\u00e9 be\u00e9kel\u0151d\u00f6tt egy jelz\u0151 – a heti \u00f6sszes\u00edt\u0151b\u0151l
     * \u00edgy hi\u00e1nyzott a fele.
     */
    @Test
    public void anAdjectiveDoesNotBreakTheCount() {
        assertEquals(3, Activities.parse("ma h\u00e1rom k\u00fcl\u00f6nb\u00f6z\u0151 mozg\u00e1s volt")
                .plans.get(0).count);
        assertEquals(2, Activities.parse("k\u00e9t k\u00fcl\u00f6nb\u00f6z\u0151 edz\u00e9s ma")
                .plans.get(0).count);
        assertEquals(3, Activities.parse("h\u00e1rom r\u00f6vid fut\u00e1s a h\u00e9ten")
                .plans.get(0).count);
        // A M\u00c9RT\u00c9KEGYS\u00c9GES sz\u00e1m \u00e9rintetlen: a „3 km fut\u00e1s" egy fut\u00e1s.
        assertEquals(1, Activities.parse("3 km fut\u00e1s").plans.get(0).count);
        assertEquals(3.0, Activities.parse("3 km fut\u00e1s").plans.get(0).km, 0.001);
    }

    /**
     * A T\u00d6RT alak\u00fa mennyis\u00e9g: az „1/2 \u00f3ra fut\u00e1s" f\u00e9l \u00f3ra, a „3/4 \u00f3ra
     * kondi" negyven\u00f6t perc – eddig MINDKETT\u0150 a mozg\u00e1sforma szok\u00e1sos
     * hossz\u00e1t kapta, mert a perjeles alakb\u00f3l nem lett sz\u00e1m. Az „1/2 km
     * s\u00e9ta" t\u00e1vja ugyan\u00edgy elveszett.
     */
    @Test
    public void aFractionOfAnHourIsAFraction() {
        assertEquals("1d+0: 1\u00d7futas/30", summary("1/2 \u00f3ra fut\u00e1s"));
        assertEquals("1d+0: 1\u00d7kondi/45", summary("3/4 \u00f3ra kondi"));
        assertEquals("1d+0: 1\u00d7joga/15", summary("1/4 \u00f3ra ny\u00fajt\u00e1s"));
        assertEquals(0.5, Activities.parse("1/2 km s\u00e9ta").plans.get(0).km, 0.001);
        // A munka/pihen\u0151 p\u00e1r nem t\u00f6rt: a ritmus \u00e9rintetlen.
        assertEquals(3, IntervalParse.parse("3 k\u00f6r 40/20").rounds);
        assertEquals(40, IntervalParse.parse("3 k\u00f6r 40/20").work);
    }

    /**
     * A felsorol\u00e1s EGY SORBAN is felsorol\u00e1s: az „1. 5 km fut\u00e1s, 2. 30 perc
     * kondi" kettes\u00e9t eddig csak sor elej\u00e9n ismert\u00fck fel, vessz\u0151vel vagy
     * perjellel \u00edrva viszont darabsz\u00e1m lett bel\u0151le – K\u00c9T kondiedz\u00e9s egy
     * helyett.
     */
    @Test
    public void anInlineListMarkerIsNotACount() {
        assertEquals("1d+0: 1\u00d7futas/30, 1\u00d7kondi/30",
                summary("1. 5 km fut\u00e1s, 2. 30 perc kondi"));
        assertEquals("1d+0: 1\u00d7futas/30, 1\u00d7kondi/30",
                summary("1. 5 km fut\u00e1s / 2. 30 perc kondi"));
        // A VAL\u00d3DI darabsz\u00e1m \u00e9rintetlen.
        assertEquals("1d+0: 2\u00d7futas/45, 3\u00d7tura/90",
                summary("reggel 2 fut\u00e1s, d\u00e9lut\u00e1n 3 s\u00e9ta"));
        // \u00c9s a tizedes vessz\u0151 sem s\u00e9r\u00fcl.
        assertEquals(2.5, Activities.parse("fut\u00e1s 2,5 km, \u00fasz\u00e1s 1,5 km")
                .plans.get(0).km, 0.001);
    }

    /**
     * Csak a KIMONDOTT \u00f6sszeg oszlik: a „reggel \u00e9s este is futottam,
     * \u00f6sszesen 2 liter v\u00edz" mondatban egyetlen edz\u00e9sadat sincs kimondva,
     * a szab\u00e1ly m\u00e9gis elfelezte a mozg\u00e1sforma szok\u00e1sos hossz\u00e1t – k\u00e9t
     * huszonk\u00e9t perces fut\u00e1s lett a k\u00e9t negyven\u00f6tperces\u00e9b\u0151l, egy
     * V\u00cdZMENNYIS\u00c9G miatt.
     */
    @Test
    public void onlyAStatedTotalGetsDivided() {
        assertEquals("1d+0: 2\u00d7futas/45",
                summary("reggel \u00e9s este is futottam. \u00f6sszesen 2 liter v\u00edz."));
        // A val\u00f3di \u00f6sszeg v\u00e1ltozatlanul oszlik.
        assertEquals("1d+0: 3\u00d7tura/30", summary("h\u00e1romszor s\u00e9t\u00e1ltam, \u00f6sszesen 90 perc"));
        assertEquals(10.0, Activities.parse("h\u00e1romszor futottam, \u00f6sszesen 30 km")
                .plans.get(0).km, 0.001);
    }

    /**
     * A megnevezett bemeleg\u00edt\u00e9s FORD\u00cdTOTT sz\u00f3renddel is megtartja a
     * perceit: a „bemeleg\u00edt\u00e9s 10 perc fut\u00f3pad" t\u00edz perce val\u00f3di fut\u00e1s –
     * eddig f\u00fcggel\u00e9kk\u00e9nt esett ki, \u00e9s a fut\u00f3pad a mozg\u00e1sforma \u00e1tlag\u00e1b\u00f3l
     * kapott negyven\u00f6t percet, vagyis egy t\u00edzperces meleg\u00edt\u00e9sb\u0151l
     * h\u00e1romnegyed \u00f3r\u00e1s edz\u00e9s lett.
     */
    @Test
    public void aNamedWarmupKeepsItsMinutesInAnyWordOrder() {
        assertEquals("1d+0: 1\u00d7futas/10", summary("bemeleg\u00edt\u00e9s 10 perc fut\u00f3pad"));
        assertEquals("1d+0: 1\u00d7kerekpar/10", summary("levezet\u00e9s 10 perc bringa"));
        assertEquals("1d+0: 1\u00d7futas/10, 1\u00d7kondi/40",
                summary("bemeleg\u00edt\u00e9s 10 perc fut\u00f3pad, ut\u00e1na 40 perc kondi"));
        // A PUSZTA bemeleg\u00edt\u00e9s v\u00e1ltozatlanul f\u00fcggel\u00e9k.
        assertTrue(Activities.parse("bemeleg\u00edt\u00e9s 20 perc").plans.isEmpty());
        assertEquals("1d+0: 1\u00d7foci/40",
                summary("bemeleg\u00edt\u00e9s 20 perc, azt\u00e1n 40 perc foci"));
    }

    /**
     * A GRAMM nem m\u00e9ter: az „\u00fasz\u00e1s 1500 m, vacsora 200 g joghurt"
     * mondatban a k\u00e9tsz\u00e1z gramm joghurtb\u00f3l K\u00c9TSZ\u00c1Z M\u00c9TERES t\u00e1v lett – az
     * \u00fasz\u00f3k m\u00e9rt\u00e9kegys\u00e9g n\u00e9lk\u00fcli t\u00e1vj\u00e1nak tilt\u00f3list\u00e1j\u00e1n ott volt a kg,
     * a puszta „g" viszont nem. Egy \u00fasz\u00f3s napl\u00f3 minden 25-tel oszthat\u00f3
     * \u00e9tel-grammja t\u00e1vv\u00e1 v\u00e1lt.
     */
    @Test
    public void aGramIsNotAMetreEvenInASwimEntry() {
        Activities.Parsed p = Activities.parse(
                "\u00fasz\u00e1s 1500 m 35 perc. 40 perc gyaloglás. vacsora 200 g joghurt");
        assertEquals(2, p.plans.size());
        assertEquals(1.5, p.plans.get(0).km, 0.001);
        assertEquals(0.0, p.plans.get(1).km, 0.001);
        // Az \u00fasz\u00f3k m\u00e9rt\u00e9kegys\u00e9g n\u00e9lk\u00fcli t\u00e1vja marad.
        assertEquals(1.5, Activities.parse("\u00fasz\u00e1s 40 perc, 1500 vegyes")
                .plans.get(0).km, 0.001);
        assertEquals(0.4, Activities.parse("\u00fasz\u00e1s 4x100 gyors")
                .plans.get(0).km, 0.001);
    }

    /**
     * Csak az „\u00c9S" tud k\u00e9t mozg\u00e1st elv\u00e1lasztani: sz\u00f3k\u00f6zzel \u00edrva az „1 \u00f3ra
     * 5 perc" mindig EGY id\u0151tartam. A „szombaton 25 km bringa 1 \u00f3ra 5 perc,
     * vas\u00e1rnap 12 km t\u00fara 3 \u00f3ra" mondatban a m\u00e1sik mozg\u00e1s neve blokkolta az
     * \u00f6sszevon\u00e1st: a bringa hatvan percet kapott, az \u00d6T PERC a t\u00far\u00e1hoz
     * v\u00e1ndorolt, a t\u00fara h\u00e1rom \u00f3r\u00e1ja meg elveszett – egy h\u00e1rom\u00f3r\u00e1s hegyi
     * t\u00fara \u00d6T PERCK\u00c9NT ment a napl\u00f3ba.
     */
    @Test
    public void anHourAndMinutesIsOneDurationNextToAnotherWorkout() {
        Activities.Parsed p = Activities.parse(
                "szombaton 25 km bringa 1 \u00f3ra 5 perc, vas\u00e1rnap 12 km t\u00fara 3 \u00f3ra");
        assertEquals(2, p.plans.size());
        assertEquals(65, p.plans.get(0).minutes);
        assertEquals(180, p.plans.get(1).minutes);
        assertEquals("1d+0: 1\u00d7futas/80, 1\u00d7kondi/45",
                summary("fut\u00e1s 1 \u00f3ra 20 perc, kondi 45 perc"));
        // Az „\u00c9S" k\u00e9t mozg\u00e1s k\u00f6z\u00f6tt tov\u00e1bbra is elv\u00e1laszt.
        assertEquals("1d+0: 1\u00d7kondi/60, 1\u00d7futas/30",
                summary("kondi 1 \u00f3ra \u00e9s 30 perc fut\u00e1s"));
        assertEquals("1d+0: 1\u00d7futas/90", summary("1 \u00f3ra \u00e9s 30 perc fut\u00e1s"));
    }

    /**
     * A K\u00d6ZELEBBI id\u0151 elveheti a helyet a t\u00e1volabbit\u00f3l: a „szauna 15
     * perc. Este 1 \u00f3ra 20 perc tenisz." mondatban a szauna gazd\u00e1tlan
     * tizen\u00f6t perce foglalta el a tenisz hely\u00e9t – csak mert el\u0151bb \u00e1llt a
     * mondatban –, \u00e9s a tenisz kimondott nyolcvan perce elveszett: egy
     * m\u00e1sf\u00e9l \u00f3r\u00e1s meccs ment be tizen\u00f6t perck\u00e9nt.
     */
    @Test
    public void theNearerDurationWins() {
        assertEquals("1d+0: 1\u00d7tenisz/80",
                summary("szauna 15 perc. Este 1 \u00f3ra 20 perc tenisz."));
        assertEquals("1d+0: 1\u00d7tenisz/80", summary("tv 15 perc. Este 80 perc tenisz."));
        // Az \u00dcL\u0150 \u00d3R\u00c1K \u00e9s a tagmondat-hat\u00e1r v\u00e1ltozatlanok.
        assertEquals("1d+0: 1\u00d7joga/20",
                summary("hossz\u00fa nap, 11 \u00f3ra munka, este 20 perc ny\u00fajt\u00e1s"));
        assertEquals("1d+0: 1\u00d7tura/30", summary("2 \u00f3ra tv, 30 perc s\u00e9ta"));
        assertEquals("1d+0: 1\u00d7kondi/30", summary("munka 30 perc kondi"));
    }

    /**
     * Csak a GYALOGL\u00c1S kimondott perce \u00edrhatja fel\u00fcl az emeletsz\u00e1mot: az
     * „5 emeletet mentem fel a l\u00e9pcs\u0151n. Este 40 perc bringa." mondatban a
     * negyven perc a BRING\u00c1\u00c9, a l\u00e9pcs\u0151z\u00e9s m\u00e9gis kiesett t\u0151le, \u00e9s a
     * s\u00e9ta a mozg\u00e1sforma szok\u00e1sos hossz\u00e1t kapta: \u00f6t emeletb\u0151l M\u00c1SF\u00c9L
     * \u00d3RA gyalogl\u00e1s lett.
     */
    @Test
    public void anotherWorkoutsMinutesDoNotEatTheFloors() {
        assertEquals("1d+0: 1\u00d7tura/3, 1\u00d7kerekpar/40",
                summary("5 emeletet mentem fel a l\u00e9pcs\u0151n. Este 40 perc bringa."));
        assertEquals("1d+0: 1\u00d7tura/4, 1\u00d7kerekpar/40",
                summary("8 emeletet gyalogoltam, 40 perc bringa"));
        // A GYALOGL\u00c1S kimondott perce tov\u00e1bbra is er\u0151sebb.
        assertEquals("1d+0: 1\u00d7tura/30", summary("5 emelet, 30 perc s\u00e9ta"));
        assertEquals("1d+0: 1\u00d7tura/10", summary("l\u00e9pcs\u0151ztem 20 emeletet"));
    }

    /**
     * Az „ODA" sz\u00f3 el is maradhat: a „munk\u00e1ba menet 15 perc bicikli,
     * vissza 18 perc" visszat\u00fatja N\u00c9M\u00c1N elveszett – a napi ingáz\u00e1s fele.
     */
    @Test
    public void theReturnLegAddsUpWithoutTheWordThere() {
        assertEquals("1d+0: 1\u00d7kerekpar/33",
                summary("Munk\u00e1ba menet 15 perc bicikli, vissza 18 perc"));
        assertEquals("1d+0: 1\u00d7kerekpar/33",
                summary("bicikli 15 perc, hazafel\u00e9 18 perc"));
        // A megszokott alak v\u00e1ltozatlan.
        assertEquals("1d+0: 1\u00d7kerekpar/53",
                summary("munk\u00e1ba bicikliv\u00e9l: oda 25, vissza 28 perc"));
        // A „vissza" ut\u00e1n \u00e1ll\u00f3 M\u00c1SIK mozg\u00e1s nem a visszat\u00fat.
        assertEquals("1d+0: 1\u00d7kondi/45, 1\u00d7tura/10",
                summary("kondi 45 perc, vissza 10 perc gyalogl\u00e1s"));
    }

    /**
     * A GYALOG megtett k\u00eds\u00e9r\u00e9s az \u00c9N mozg\u00e1som: a „gyalog vittem a
     * gyereket oviba, 15 perc" tizen\u00f6t perce val\u00f3di s\u00e9ta – eddig a
     * k\u00eds\u00e9r\u00e9s szav\u00e1val egy\u00fctt elt\u0171nt. Az EDZ\u00c9SRE k\u00eds\u00e9r\u00e9s marad
     * kiz\u00e1rva: ott a gyerek mozog, nem \u00e9n.
     */
    @Test
    public void walkingTheChildToNurseryIsMyWalk() {
        assertEquals("1d+0: 1\u00d7tura/15",
                summary("gyalog vittem a gyereket oviba, 15 perc"));
        assertEquals("1d+0: 1\u00d7tura/15, 1\u00d7futas/60",
                summary("Reggel elvittem a gyereket \u00f3vod\u00e1ba gyalog, 15 perc "
                        + "oda-vissza. Ut\u00e1na 1 \u00f3ra fut\u00e1s."));
        // A gyerek edz\u00e9se tov\u00e1bbra sem az eny\u00e9m.
        assertTrue(Activities.parse("elvittem a gyereket edz\u00e9sre").plans.isEmpty());
        assertTrue(Activities.parse("a gyerek edz\u00e9s\u00e9n voltam").plans.isEmpty());
    }

    /**
     * A KETT\u0150SPONTOS fejl\u00e9c perce az eg\u00e9sz blokk\u00e9: a „20 perc otthoni
     * edz\u00e9s: 3 k\u00f6r 10 fekv\u0151t\u00e1masz 15 guggol\u00e1s" h\u00fasz perce a munka
     * hossza – eddig az ism\u00e9tl\u00e9ssz\u00e1mb\u00f3l becs\u00fclt huszonh\u00e9t perc ment a
     * napl\u00f3ba, mert a kimondott id\u0151 a mozg\u00e1sn\u00e9v EL\u0150TT \u00e1llt.
     */
    @Test
    public void theHeaderMinutesBeforeTheColonWin() {
        assertEquals(20, Activities.parse(
                "20 perc otthoni edz\u00e9s: 3 k\u00f6r 10 fekv\u0151t\u00e1masz 15 guggol\u00e1s")
                .plans.get(0).minutes);
        assertEquals(45, Activities.parse("45 perc edz\u00e9s: fut\u00e1s 5 km")
                .plans.get(0).minutes);
        // A becsl\u00e9s marad ott, ahol nincs kimondott blokk-hossz.
        assertEquals("1d+0: 1\u00d7kondi/20", summary("100 fekv\u0151t\u00e1masz"));
    }

    /**
     * A NAPSZAK szava \u00d3R\u00c1T jel\u00f6l, nem k\u00f6rt: a „reggel 7 kor 5 km fut\u00e1s"
     * hetese id\u0151pont – k\u00f6rnek olvasva HARMINC\u00d6T kilom\u00e9teres fut\u00e1s lett
     * bel\u0151le, h\u00e1rom \u00e9s f\u00e9l \u00f3r\u00e1s becs\u00fclt id\u0151vel.
     */
    @Test
    public void aDaypartMakesItAClockNotLaps() {
        Activities.Parsed p = Activities.parse("reggel 7 kor 5 km fut\u00e1s");
        assertEquals(5.0, p.plans.get(0).km, 0.001);
        assertEquals(7, p.hour);
        assertEquals(3.0, Activities.parse("este 6 kor 3 km fut\u00e1s")
                .plans.get(0).km, 0.001);
        // Napszak n\u00e9lk\u00fcl a k\u00f6r marad szorz\u00f3.
        assertEquals(1.2, Activities.parse("3 kor 400 m fut\u00e1s")
                .plans.get(0).km, 0.001);
        assertEquals(2.4, Activities.parse("6x400 m\u00e9ter").plans.get(0).km, 0.001);
    }


    /**
     * Az \u00d6SSZEGZ\u0150 fejl\u00e9ce id\u0151szak, a t\u00e1v pedig annyi edz\u00e9s\u00e9, ah\u00e1ny
     * alkalmat kimond: az \u201eez a h\u00f3nap: 12 edz\u00e9s, 145 km fut\u00e1s"
     * sz\u00e1znegyven\u00f6t kilom\u00e9tere EGYETLEN mai napra ker\u00fclt \u2013 tizenn\u00e9gy \u00e9s f\u00e9l
     * \u00f3r\u00e1s fut\u00e1sk\u00e9nt.
     */
    @Test
    public void aPeriodSummarySplitsItsTotalOverTheSessions() {
        Activities.Parsed p = Activities.parse(
                "Ez a h\u00f3nap: 12 edz\u00e9s, 145 km fut\u00e1s, 3 kg fogy\u00e1s.");
        assertEquals(30, p.days);
        assertEquals(12, p.plans.get(0).count);
        assertEquals(145.0 / 12, p.plans.get(0).km, 0.01);
        Activities.Parsed w = Activities.parse("Ez a h\u00e9t: 4 edz\u00e9s, 30 km fut\u00e1s.");
        assertEquals(7, w.days);
        assertEquals(4, w.plans.get(0).count);
        assertEquals(7.5, w.plans.get(0).km, 0.01);
        Activities.Parsed m = Activities.parse("Havi m\u00e9rleg: 18 edz\u00e9s, 200 km fut\u00e1s.");
        assertEquals(30, m.days);
        assertEquals(18, m.plans.get(0).count);
        // A JELZ\u0150K\u00c9NT \u00e1ll\u00f3 csupasz \u201eh\u00e9t" tov\u00e1bbra sem id\u0151szak.
        assertEquals(1, Activities.parse(
                "Ma deload h\u00e9t van, edzettem 45 percet.").days);
        // Egyetlen mai fut\u00e1s t\u00e1vja osztatlan marad.
        assertEquals(10.0, Activities.parse("Ma futottam 10 km-t.")
                .plans.get(0).km, 0.01);
    }


    /**
     * Az \u00d6SSZESEN oszt\u00f3ja a MOZG\u00c1SFORM\u00c1K sz\u00e1ma: a \u201ekonditerem: mellkas nap,
     * fekvenyom\u00e1s 5x5 80 kg, tol\u00f3dzkod\u00e1s 4x10. \u00d6sszesen 70 perc." k\u00e9t
     * kondi-sz\u00f3t\u00f6vet tartalmaz, de egyetlen edz\u00e9st \u2013 a hetven perc m\u00e9gis
     * harminc\u00f6tre felez\u0151d\u00f6tt.
     */
    @Test
    public void oneKindTwiceNamedDoesNotHalveTheTotal() {
        assertEquals(70, Activities.parse("Konditerem: mellkas nap, fekvenyom\u00e1s "
                + "5x5 80 kg, tol\u00f3dzkod\u00e1s 4x10. \u00d6sszesen 70 perc.")
                .plans.get(0).minutes);
        // K\u00c9T mozg\u00e1sforma k\u00f6z\u00f6tt viszont tov\u00e1bbra is oszlik.
        Activities.Parsed p = Activities.parse("Fut\u00e1s \u00e9s kondi, \u00f6sszesen 70 perc.");
        assertEquals(2, p.plans.size());
        assertEquals(35, p.plans.get(0).minutes);
        assertEquals(35, p.plans.get(1).minutes);
    }

    /**
     * A R\u00c9SZLET \u00e9s az \u00d6SSZEG ugyanaz az edz\u00e9s: az \u201e\u00fasz\u00e1s: 20x50 m\u00e9ter
     * gyorson, 20 mp pihi szettek k\u00f6zt, \u00f6sszesen 1200 m\u00e9ter meleg\u00edt\u00e9ssel"
     * egy ezer- \u00e9s egy ezerk\u00e9tsz\u00e1z m\u00e9teres \u00fasz\u00e1st is be\u00edrt.
     */
    @Test
    public void theStatedTotalReplacesTheDetail() {
        Activities.Parsed p = Activities.parse("\u00dasz\u00e1s: 20x50 m\u00e9ter gyorson, "
                + "20 mp pihi szettek k\u00f6zt, \u00f6sszesen 1200 m\u00e9ter meleg\u00edt\u00e9ssel.");
        assertEquals(1, p.plans.size());
        assertEquals(1.2, p.plans.get(0).km, 0.01);
        // K\u00e9t K\u00dcL\u00d6N edz\u00e9s megmarad, ha egyik\u00fck sem maga az \u00f6sszeg.
        assertEquals(2, Activities.parse(
                "Reggel 5 km fut\u00e1s, este 8 km fut\u00e1s, \u00f6sszesen 13 km.").plans.size());
    }


    /**
     * A N\u00c9Z\u0150 nem j\u00e1tszik: a \u201ema a gyerekkel voltam a foci edz\u00e9sen, \u00e9n csak
     * n\u00e9ztem a p\u00e1lya sz\u00e9l\u00e9r\u0151l" kilencven perc focit \u00edrt a napl\u00f3ba \u2013 a
     * tagad\u00f3 sz\u00f3 a m\u00e1sik tagmondatban \u00e1llt, \u00edgy nem \u00e9rt el a m\u00e9rk\u0151z\u00e9sig.
     */
    @Test
    public void aSpectatorLogsNothing() {
        assertEquals(0, Activities.parse("Ma a gyerekkel voltam a foci "
                + "edz\u00e9sen, \u00e9n csak n\u00e9ztem a p\u00e1lya sz\u00e9l\u00e9r\u0151l.").plans.size());
        assertEquals(0, Activities.parse("Este a meccsen szurkoltunk a fiamnak.")
                .plans.size());
        // A SAJ\u00c1T mozg\u00e1s-ige mellett a n\u00e9z\u00e9s csak a maga tagmondat\u00e1t viszi.
        assertEquals(1, Activities.parse("Csak n\u00e9ztem a telefonom, azt\u00e1n "
                + "futottam 5 km-t.").plans.size());
        assertEquals(30, Activities.parse("30 perc fut\u00e1s, k\u00f6zben n\u00e9ztem a "
                + "meccset a t\u00e9v\u00e9n.").plans.get(0).minutes);
    }

    /**
     * A K\u00d6R\u00d6NK\u00c9NT szava kimondja, hogy k\u00f6r\u00f6kr\u0151l van sz\u00f3: a \u201es\u00falyz\u00f3s edz\u00e9s
     * otthon: 3 k\u00f6r, k\u00f6r\u00f6nk\u00e9nt 15 guggol\u00e1s" hajnali h\u00e1romra tette a
     * bejegyz\u00e9st \u2013 \u00e9kezet n\u00e9lk\u00fcl a \u201ek\u00f6r" \u00e9s a \u201e-kor" egybeesik.
     */
    @Test
    public void roundsPerRoundAreNotAClock() {
        assertEquals(12, Activities.parse("S\u00falyz\u00f3s edz\u00e9s otthon: 3 k\u00f6r, "
                + "k\u00f6r\u00f6nk\u00e9nt 15 guggol\u00e1s, 12 fekv\u0151t\u00e1masz, 20 hasizom.").hour);
        // A val\u00f3di id\u0151pont marad.
        assertEquals(7, Activities.parse("reggel 7 kor 5 km fut\u00e1s").hour);
    }

    /**
     * Az EGY \u00daT t\u00e1vja a fele: a \u201ebring\u00e1val j\u00e1rtam be a mel\u00f3helyre, oda 25
     * perc, vissza 30 perc, kb 9 km egy \u00fat" kilenc kilom\u00e9tert \u00edrt a napl\u00f3ba
     * a t\u00e9nylegesen letekert tizennyolc helyett.
     */
    @Test
    public void aOneWayDistanceCountsTwiceOnARoundTrip() {
        Activities.Parsed p = Activities.parse("Bring\u00e1val j\u00e1rtam be a "
                + "mel\u00f3helyre, oda 25 perc, vissza 30 perc, kb 9 km egy \u00fat.");
        assertEquals(18.0, p.plans.get(0).km, 0.01);
        assertEquals(55, p.plans.get(0).minutes);
        assertEquals(15.0, Activities.parse("Bicikli munk\u00e1ba, 7,5 km egy \u00fat, "
                + "oda-vissza.").plans.get(0).km, 0.01);
        // Egyir\u00e1ny\u00fa \u00fat t\u00e1vja marad.
        assertEquals(3.0, Activities.parse("Hazafel\u00e9 s\u00e9t\u00e1ltam 3 km-t.")
                .plans.get(0).km, 0.01);
    }


    /**
     * A FELADOTT edz\u00e9s hossza a MEGTETT id\u0151, a megtervezett edz\u00e9s pedig nem
     * edz\u00e9s: a \u201enem b\u00edrtam v\u00e9gigcsin\u00e1lni, 20 perc ut\u00e1n feladtam a 45 perces
     * edz\u00e9st" negyven\u00f6t percet \u00edrt be, a \u201ema megint nem siker\u00fclt elmenni
     * edzeni, pedig terveztem 45 perc kondit" szint\u00e9n.
     */
    @Test
    public void anAbandonedWorkoutCountsOnlyWhatWasDone() {
        assertEquals(20, Activities.parse("Nem b\u00edrtam v\u00e9gigcsin\u00e1lni, 20 perc "
                + "ut\u00e1n feladtam a 45 perces edz\u00e9st.").plans.get(0).minutes);
        assertEquals(0, Activities.parse("Ma megint nem siker\u00fclt elmenni "
                + "edzeni, pedig terveztem 45 perc kondit.").plans.size());
        // A megval\u00f3sult terv marad.
        assertEquals(45, Activities.parse("Terveztem 45 perc kondit, \u00e9s meg "
                + "is csin\u00e1ltam 45 percet.").plans.get(0).minutes);
    }


    /**
     * A SAJ\u00c1T, elt\u00e9r\u0151 hossz\u00fa m\u00e1sodik eml\u00edt\u00e9s is k\u00fcl\u00f6n edz\u00e9s: a \u201ereggeli t\u00fara
     * 12 km 3 \u00f3ra, este k\u00f6nny\u0171 20 perc s\u00e9ta" h\u00faszperces s\u00e9t\u00e1ja beleolvadt a
     * t\u00far\u00e1ba \u2013 a k\u00e9t hossz \u00e1tlaga, sz\u00e1z perc lett mindkett\u0151, \u00e9s a napra
     * huszonn\u00e9gy kilom\u00e9ter ker\u00fclt tizenkett\u0151 helyett.
     */
    @Test
    public void aSecondMentionWithItsOwnLengthIsItsOwnSession() {
        Activities.Parsed p = Activities.parse("Vas\u00e1rnap: reggeli t\u00fara 12 km "
                + "3 \u00f3ra, d\u00e9lut\u00e1n pihi, este k\u00f6nny\u0171 20 perc s\u00e9ta.");
        assertEquals(2, p.plans.size());
        assertEquals(12.0, p.plans.get(0).km, 0.01);
        assertEquals(180, p.plans.get(0).minutes);
        assertEquals(0.0, p.plans.get(1).km, 0.01);
        assertEquals(20, p.plans.get(1).minutes);
    }


    /**
     * Az ID\u0150PONT ut\u00e1n nem \u00e1ll \u00fajabb sz\u00e1m: a \u201e4 k\u00f6r 10 burpee, 15 guggol\u00e1s"
     * hajnali n\u00e9gyre ker\u00fclt a napl\u00f3ban \u2013 \u00e9kezet n\u00e9lk\u00fcl a \u201ek\u00f6r" \u00e9s a \u201e-kor"
     * egybeesik.
     */
    @Test
    public void aRoundCountBeforeARepCountIsNotAClock() {
        assertEquals(12, Activities.parse("4 k\u00f6r 10 burpee, 15 guggol\u00e1s.").hour);
        // A napszak melletti \u00f3ra marad \u00f3ra.
        assertEquals(7, Activities.parse("reggel 7 kor 5 km fut\u00e1s").hour);
        assertEquals(18, Activities.parse("este 6 kor 3 km fut\u00e1s").hour);
    }


    /**
     * A S\u00c9TA napi t\u00f6bb k\u00f6re is \u00f6sszead\u00f3dik: a \u201ema csak s\u00e9t\u00e1ltam a kuty\u00e1val
     * 3x20 percet" h\u00fasz percet \u00edrt a napl\u00f3ba a hatvanb\u00f3l \u2013 a m\u00e1sik k\u00e9t k\u00f6r
     * nyomtalanul elt\u0171nt.
     */
    @Test
    public void severalWalkingLapsAddUp() {
        assertEquals(60, Activities.parse("Ma csak s\u00e9t\u00e1ltam a kuty\u00e1val "
                + "3x20 percet.").plans.get(0).minutes);
        assertEquals(60, Activities.parse("Kutyas\u00e9t\u00e1ltat\u00e1s 2x30 perc.")
                .plans.get(0).minutes);
        assertEquals(50, Activities.parse("Biciklivel mentem dolgozni, "
                + "2x25 perc.").plans.get(0).minutes);
    }


    /**
     * A K\u00d6R HOSSZA szorz\u00f3dik a k\u00f6r\u00f6k sz\u00e1m\u00e1val: a \u201ema 3 k\u00f6rt futottam a
     * parkban, egy k\u00f6r 2,5 km" k\u00e9t \u00e9s f\u00e9l kilom\u00e9tert \u00edrt a napl\u00f3ba a h\u00e9t \u00e9s
     * f\u00e9lb\u0151l \u2013 a m\u00e1sik k\u00e9t k\u00f6r nyomtalanul elt\u0171nt.
     */
    @Test
    public void theLapLengthMultipliesWithTheLapCount() {
        assertEquals(7.5, Activities.parse("Ma 3 k\u00f6rt futottam a parkban, "
                + "egy k\u00f6r 2,5 km.").plans.get(0).km, 0.01);
        assertEquals(7.5, Activities.parse("3 k\u00f6rt futottam, k\u00f6r\u00f6nk\u00e9nt 2,5 km.")
                .plans.get(0).km, 0.01);
        assertEquals(1.6, Activities.parse("4 k\u00f6rt \u00fasztam, egy k\u00f6r 400 m\u00e9ter.")
                .plans.get(0).km, 0.01);
        // K\u00f6r n\u00e9lk\u00fcl a t\u00e1v marad.
        assertEquals(5.0, Activities.parse("Ma futottam 5 km-t.")
                .plans.get(0).km, 0.01);
    }


    /**
     * A \u201eHETI 3-szor" ugyanaz a gyakoris\u00e1g, mint a \u201ehetente h\u00e1romszor": az
     * \u201eelm\u00falt h\u00f3napban \u00e1tlagosan heti 3-szor sportoltam" h\u00e1rom alkalmat \u00edrt
     * a napl\u00f3ba tizenh\u00e1rom helyett.
     */
    @Test
    public void aWeeklyAdjectiveWithANumberIsAFrequency() {
        Activities.Parsed p = Activities.parse(
                "Az elm\u00falt h\u00f3napban \u00e1tlagosan heti 3-szor sportoltam.");
        assertEquals(30, p.days);
        assertEquals(12, p.plans.get(0).count);
        assertEquals(4, Activities.parse("Az elm\u00falt h\u00f3napban heti 40 km fut\u00e1s.")
                .plans.get(0).count);
        // Sz\u00e1m n\u00e9lk\u00fcl a \u201eheti" csak jelz\u0151 marad.
        assertEquals(7, Activities.parse("Heti terhel\u00e9s: 60 km.").days);
    }

    /**
     * A TERV SZERINT megt\u00f6rt\u00e9nt edz\u00e9sr\u0151l sz\u00f3l: az \u201ea tervem szerint ma
     * futottam 5 km-t" \u00f6t kilom\u00e9tere n\u00e9m\u00e1n elveszett, mert a terv szava
     * j\u00f6v\u0151nek mutatta az eg\u00e9sz mondatot. A MAI nap kimond\u00e1sa pedig er\u0151sebb a
     * puszta \u201eheti" jelz\u0151n\u00e9l.
     */
    @Test
    public void aPlanFollowedIsAWorkoutDone() {
        Activities.Parsed p = Activities.parse("A tervem szerint ma futottam 5 km-t.");
        assertEquals(1, p.plans.size());
        assertEquals(5.0, p.plans.get(0).km, 0.01);
        Activities.Parsed w = Activities.parse("A heti tervem szerint ma futottam 5 km-t.");
        assertEquals(1, w.days);
        assertEquals(5.0, w.plans.get(0).km, 0.01);
        // A val\u00f3di terv marad terv.
        assertEquals(0, Activities.parse("A terv: guggol\u00e1s 5x5 100 kg.").plans.size());
        assertEquals(0, Activities.parse("A tervem szerint holnap futok 5 km-t.")
                .plans.size());
    }

    /**
     * A \u201eH\u00daSZ PERCE" id\u0151pont, nem hossz: a \u201eh\u00fasz perce j\u00f6ttem meg a
     * fut\u00e1sb\u00f3l" h\u00faszperces fut\u00e1st \u00edrt a napl\u00f3ba \u2013 abb\u00f3l a sz\u00e1mb\u00f3l, ami azt
     * mondja meg, mikor \u00e9rt haza.
     */
    @Test
    public void minutesAgoIsNotADuration() {
        assertEquals(45, Activities.parse("H\u00fasz perce j\u00f6ttem meg a fut\u00e1sb\u00f3l, "
                + "nagyon f\u00e1jt a bal t\u00e9rdem.").plans.get(0).minutes);
        assertEquals(60, Activities.parse("20 perce fejeztem be a kondit.")
                .plans.get(0).minutes);
        // A t\u00e1rgyragos alak marad hossz.
        assertEquals(20, Activities.parse("20 percet futottam.")
                .plans.get(0).minutes);
        assertEquals(20, Activities.parse("20 perc fut\u00e1s.").plans.get(0).minutes);
    }


    /**
     * A J\u00d6V\u0150 tagmondata nem viheti el a megt\u00f6rt\u00e9nt edz\u00e9st: a \u201etegnap 45
     * percet futottam, ma pihenek, holnap kondi lesz" negyven\u00f6t perce
     * nyomtalanul elt\u0171nt, mert a mondat V\u00c9G\u00c9N \u00e1ll\u00f3 terv az eg\u00e9sz bejegyz\u00e9st
     * j\u00f6v\u0151nek mutatta.
     */
    @Test
    public void aTrailingPlanDoesNotEraseWhatHappened() {
        Activities.Parsed p = Activities.parse(
                "Tegnap 45 percet futottam, ma pihenek, holnap kondi lesz.");
        assertEquals(1, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(45, p.plans.get(0).minutes);
        assertEquals(5.0, Activities.parse("Ma 5 km fut\u00e1s, holnap is tervezek "
                + "egyet.").plans.get(0).km, 0.01);
        // A tiszt\u00e1n j\u00f6v\u0151 idej\u0171 mondat tov\u00e1bbra sem napl\u00f3.
        assertEquals(0, Activities.parse("Holnap futok 5 km-t.").plans.size());
        assertEquals(0, Activities.parse("Ha lesz id\u0151m, futok.").plans.size());
        assertEquals(0, Activities.parse("Este megyek edzeni.").plans.size());
    }

    /**
     * A H\u00c1TRAVETETT \u201enem" ugyan\u00fagy tagad\u00e1s: a \u201ema szauna \u00e9s jakuzzi volt
     * csak, edz\u00e9s nem" negyven\u00f6t perces egy\u00e9b mozg\u00e1st \u00edrt a napl\u00f3ba \u2013 pont
     * abb\u00f3l a sz\u00f3b\u00f3l, amit a felhaszn\u00e1l\u00f3 \u00e9pp tagad.
     */
    @Test
    public void aTrailingNoNegatesTheActivity() {
        assertEquals(0, Activities.parse("Ma szauna \u00e9s jakuzzi volt csak, "
                + "edz\u00e9s nem.").plans.size());
        // Az \u00e1ll\u00edt\u00f3 mondat marad.
        assertEquals(1, Activities.parse("Ma edz\u00e9s volt, 45 perc.").plans.size());
    }

    /**
     * A K\u00d6ZELEBBI t\u00e1v fel\u00fcl\u00edrja a t\u00e1volabbit: a \u201ereggel 5 km, d\u00e9lben \u00fasz\u00e1s
     * 1000 m" \u00f6t kilom\u00e9tere a sportn\u00e9v n\u00e9lk\u00fcli els\u0151 tagmondat\u00e9, m\u00e9gis az
     * \u00fasz\u00e1s vitte el \u2013 \u00f6tkilom\u00e9teres \u00fasz\u00e1s lett bel\u0151le, az ezer m\u00e9ter meg
     * nyomtalanul elt\u0171nt.
     */
    @Test
    public void theNearerDistanceWins() {
        Activities.Parsed p = Activities.parse(
                "Reggel 5 km, d\u00e9lben \u00fasz\u00e1s 1000 m, este j\u00f3ga 30 perc.");
        assertEquals(2, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
        assertEquals(1.0, p.plans.get(0).km, 0.01);
        // Amit egy mozg\u00e1s K\u00d6ZELR\u0150L kapott, azt nem viszi el m\u00e1s.
        Activities.Parsed b = Activities.parse("Bicikli 20 km, fut\u00e1s 5 km.");
        assertEquals(20.0, b.plans.get(0).km, 0.01);
        assertEquals(5.0, b.plans.get(1).km, 0.01);
        // A n\u00e9vb\u0151l j\u00f6v\u0151 t\u00e1v nem \u00edrja fel\u00fcl a kimondottat.
        assertEquals(19.5, Activities.parse("f\u00e9lmaraton 19,5 km")
                .plans.get(0).km, 0.001);
    }


    /**
     * A KIMONDOTT \u00d6SSZT\u00c1V a teljes edz\u00e9s\u00e9: az \u201e\u00fasz\u00f3edz\u00e9s: 400 m bemeleg\u00edt\u00e9s,
     * 8x100 m gyors, 200 m levezet\u00e9s. \u00d6sszesen 1400 m, 45 perc." n\u00e9gysz\u00e1z
     * m\u00e9tert \u00edrt a napl\u00f3ba \u2013 a bemeleg\u00edt\u00e9st az eg\u00e9sz edz\u00e9s helyett.
     */
    @Test
    public void theStatedTotalIsTheWholeSession() {
        Activities.Parsed p = Activities.parse("\u00dasz\u00f3edz\u00e9s: 400 m bemeleg\u00edt\u00e9s, "
                + "8x100 m gyors, 200 m levezet\u00e9s. \u00d6sszesen 1400 m, 45 perc.");
        assertEquals(1, p.plans.size());
        assertEquals(1.4, p.plans.get(0).km, 0.01);
        assertEquals(45, p.plans.get(0).minutes);
        // A r\u00e9szt\u00e1vot kimond\u00f3, \u00f6sszeg n\u00e9lk\u00fcli mondat marad.
        assertEquals(5.0, Activities.parse("Fut\u00e1s: 8x400 m\u00e9ter, \u00f6sszesen 5 km.")
                .plans.get(0).km, 0.01);
    }

    /**
     * A VISSZA\u00daT T\u00c1VJA is \u00f6sszead\u00f3dik: a \u201ereggeli s\u00falyz\u00f3z\u00e1s 40 perc, azt\u00e1n
     * bicajjal munk\u00e1ba 8 km, este vissza 8 km" nyolc kilom\u00e9tert \u00edrt a
     * napl\u00f3ba a tizenhatb\u00f3l \u2013 a k\u00e9t egyforma t\u00e1v egyetlen teker\u00e9snek
     * l\u00e1tszott.
     */
    @Test
    public void theWayBackAddsItsDistance() {
        Activities.Parsed p = Activities.parse("Reggeli s\u00falyz\u00f3z\u00e1s 40 perc, "
                + "azt\u00e1n bicajjal munk\u00e1ba 8 km, este vissza 8 km.");
        assertEquals(2, p.plans.size());
        assertEquals(16.0, p.plans.get(1).km, 0.01);
        // A percek \u00f6sszead\u00e1sa v\u00e1ltozatlan.
        assertEquals(33, Activities.parse("Munk\u00e1ba menet 15 perc bicikli, "
                + "vissza 18 perc.").plans.get(0).minutes);
    }


    /**
     * A KIHAGYÁS hossza nem edzés-időszak: a „fél évig nem sportoltam, ma
     * kezdtem újra: 15 perc laza kerékpár" tizenöt perce száznyolcvanhárom
     * napra terült szét – a mai újrakezdés a szériából is kimaradt.
     */
    @Test
    public void theLengthOfABreakIsNotASpan() {
        assertEquals(1, Activities.parse("Fél évig nem sportoltam, ma "
                + "kezdtem újra: 15 perc laza kerékpár.").days);
        assertEquals(1, Activities.parse("Két hétig nem edzettem, ma "
                + "30 perc futás.").days);
        // A valódi időszak marad.
        assertEquals(7, Activities.parse("Az elmúlt héten 3 futás.").days);
        assertEquals(14, Activities.parse("Két hétig túráztunk, összesen 60 km.").days);
    }

    /**
     * A gyakorlat NEVE nem külön mozgásforma az idő szétosztásánál sem: a
     * „kondi: guggolás 5x5 90 kg, fekvenyomás 5x5 70 kg, evezés 5x5 60 kg.
     * Összesen 55 perc." ötvenöt percét kettéosztotta, és a fele egy
     * kitalált evezőgépezésre ment.
     */
    @Test
    public void aLiftNameDoesNotHalveTheStatedTime() {
        Activities.Parsed p = Activities.parse("Kondi: guggolás 5x5 90 kg, "
                + "fekvenyomás 5x5 70 kg, evezés 5x5 60 kg. Összesen 55 perc, RPE 8.");
        assertEquals(1, p.plans.size());
        assertEquals("kondi", p.plans.get(0).kind.id);
        assertEquals(55, p.plans.get(0).minutes);
        // A SAJÁT, közelről kapott idő megvédi a gépet.
        Activities.Parsed q = Activities.parse("Evezőgép 20 perc, guggolás 3x8 80 kg.");
        assertEquals(2, q.plans.size());
        assertEquals(20, q.plans.get(0).minutes);
    }


    /**
     * Az óra AKTÍV IDEJE ugyanannak a napnak a mozgása, nem külön edzés: a
     * „ma 12 000 lépés, 8,5 km, 320 kcal, 45 perc aktív idő az óra szerint"
     * HÁROM bejegyzést írt a naplóba – ugyanazt a napot háromszor.
     */
    @Test
    public void theWatchesActiveTimeIsNotAThirdWorkout() {
        Activities.Parsed p = Activities.parse("Ma 12 000 lépés, 8,5 km, "
                + "320 kcal, 45 perc aktív idő az óra szerint.");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(8.5, p.plans.get(0).km, 0.01);
        assertEquals(45, p.plans.get(0).minutes);
        // Aktív idő nélkül a lépés és a táv eddig is egy séta volt.
        assertEquals(1, Activities.parse("Ma 12 000 lépés, 8,5 km.").plans.size());
    }

    /**
     * A kimondott SZÜNET két félidőt jelent: a „ma 2 x 45 perc foci volt,
     * közte 15 perc szünet" mérkőzése némán elveszett – a mondatból
     * időzítő-terv lett, bejegyzés nélkül.
     */
    @Test
    public void aMatchWithHalvesIsOneSession() {
        Activities.Parsed p = Activities.parse("Ma 2 x 45 perc foci volt, "
                + "közte 15 perc szünet.");
        assertEquals(1, p.plans.size());
        assertEquals("foci", p.plans.get(0).kind.id);
        assertEquals(90, p.plans.get(0).minutes);
        // Szünet nélkül a „2x45 perc foci" két alkalom marad.
        assertEquals(2, Activities.parse("2x45 perc foci").plans.get(0).count);
    }


    /**
     * A KIÍRT számnév ugyanaz a körszám, és a MELLÉKES tagmondat perce nem
     * a főmozgásé: a „két kör a tó körül, egy kör 3,2 km, közben 2 perc
     * séta" hat és fél kilométere helyett három és kettő tized került a
     * naplóba – majd a két perc mellé száznyolcvan km/h-s gyaloglásként.
     */
    @Test
    public void aSpelledOutLapCountAndASanePace() {
        Activities.Parsed p = Activities.parse("Két kör a tó körül, "
                + "egy kör 3,2 km, közben 2 perc séta.");
        assertEquals(1, p.plans.size());
        assertEquals(6.4, p.plans.get(0).km, 0.01);
        assertTrue("életszerűtlen tempó", p.plans.get(0).minutes > 30);
        // A hihető tempó marad, ahogy kimondták.
        assertEquals(20, Activities.parse("Ma futottam 5 km-t 20 perc alatt.")
                .plans.get(0).minutes);
        assertEquals(80, Activities.parse("Bringa 40 km 80 perc.")
                .plans.get(0).minutes);
    }

    /**
     * A KÖZBEN tagmondata ugyanannak az edzésnek a része: az „este 40 perc
     * jóga, közben 10 perc légzőgyakorlat" tíz perce egy MÁSODIK
     * jóga-bejegyzés lett – ötven perc abból a negyvenből, ami megvolt.
     */
    @Test
    public void aDuringClauseIsPartOfTheSameSession() {
        Activities.Parsed p = Activities.parse("Este 40 perc jóga, "
                + "közben 10 perc légzőgyakorlat.");
        assertEquals(1, p.plans.size());
        assertEquals(40, p.plans.get(0).minutes);
        assertEquals(1, Activities.parse("A jógaóra 75 percig tartott, "
                + "a végén 10 perc meditációval.").plans.size());
        // A KÜLÖN alkalom marad külön.
        assertEquals(2, Activities.parse("Reggeli túra 12 km 3 óra, "
                + "este könnyű 20 perc séta.").plans.size());
    }


    /**
     * A KÖZTE tagmondat távja a szakaszok közti pihenő, nem az edzés távja:
     * a „3 kör 800 m, közte 400 m kocogás" négyszáz métert írt a naplóba a
     * kétezer-négyszázból – a levezető kocogás elvitte a teljes futás
     * helyét, mert közelebb állt a mozgás szavához.
     */
    @Test
    public void theRecoveryDistanceDoesNotReplaceTheWorkout() {
        Activities.Parsed p = Activities.parse("3 kör 800 m, közte 400 m kocogás.");
        assertEquals(1, p.plans.size());
        assertEquals(2.4, p.plans.get(0).km, 0.01);
        // A kimondott össztáv és a két külön mozgás változatlan.
        assertEquals(4.0, Activities.parse("10x400 m, közte 90 mp pihi, "
                + "összesen 4 km.").plans.get(0).km, 0.01);
        Activities.Parsed b = Activities.parse("Bicikli 20 km, futás 5 km.");
        assertEquals(20.0, b.plans.get(0).km, 0.01);
        assertEquals(5.0, b.plans.get(1).km, 0.01);
    }


    /**
     * A TAGMONDAT erősebb a puszta közelségnél: a „futás 10 km 52 perc;
     * kondi 40 perc" ötvenkét perce a KONDIHOZ állt közelebb – két karakter
     * a pontosvessző és a szóköz –, így a futás kimondott ideje elveszett,
     * és a tempóból becsült hatvan perc ment a naplóba.
     */
    @Test
    public void theClauseBeatsMereProximity() {
        Activities.Parsed p = Activities.parse("futás 10 km 52 perc; kondi 40 perc");
        assertEquals(2, p.plans.size());
        assertEquals(52, p.plans.get(0).minutes);
        assertEquals(40, p.plans.get(1).minutes);
        // Tagmondaton BELÜL a közelség dönt, ahogy eddig.
        Activities.Parsed q = Activities.parse("futás és 30 perc kondi");
        assertEquals(45, q.plans.get(0).minutes);
        assertEquals(30, q.plans.get(1).minutes);
        // A gazdátlan idő továbbra is átmehet a másik tagmondatba.
        assertEquals(80, Activities.parse("szauna 15 perc. Este 1 óra 20 perc "
                + "tenisz.").plans.get(0).minutes);
    }

    /**
     * Az „OK" az angol RENDBEN szava is: az „edzés ok, 45 perc" bejegyzése
     * némán elveszett, mert a mondat harmadik személyű alanynak látszott.
     */
    @Test
    public void theEnglishOkIsNotTheHungarianThey() {
        assertEquals(45, Activities.parse("edzés ok, 45perc, jól ment")
                .plans.get(0).minutes);
        // Az „ők" igével továbbra is más naplója.
        assertEquals(0, Activities.parse("ők futottak 10 km-t.").plans.size());
        assertEquals(0, Activities.parse("Ők edzettek, én pihentem.").plans.size());
    }


    /**
     * Az ODA és a VISSZA külön kimondott távja is összeadódik: a
     * „kirándulás: 8 km oda, 8 km vissza" nyolc kilométert írt a naplóba a
     * tizenhatból – a visszaút nyomtalanul eltűnt.
     */
    @Test
    public void bothLegsOfTheTripCount() {
        assertEquals(16.0, Activities.parse("Kirándulás: 8 km oda, 8 km vissza, "
                + "közben megálltunk ebédelni.").plans.get(0).km, 0.01);
        assertEquals(22.0, Activities.parse("10 km oda, 12 km vissza.")
                .plans.get(0).km, 0.01);
        // Az egyirányú táv marad.
        assertEquals(8.0, Activities.parse("Ma futottam 8 km-t.")
                .plans.get(0).km, 0.01);
    }


    /**
     * Az EBBŐL a teljes időből vág ki egy részt, de csak az OTT-LÉT
     * idejéből: a „karate edzés 90 perc, ebből 20 perc formagyakorlat"
     * kilencven perce maga az edzés – eddig húszperces karate került a
     * naplóba, a nyújtásos változatból meg száztíz perc.
     */
    @Test
    public void aBreakdownDoesNotShrinkTheSession() {
        assertEquals(90, Activities.parse("Karate edzés 90 perc, ebből "
                + "20 perc formagyakorlat.").plans.get(0).minutes);
        Activities.Parsed n = Activities.parse("Karate edzés 90 perc, ebből "
                + "20 perc nyújtás.");
        assertEquals(1, n.plans.size());
        assertEquals(90, n.plans.get(0).minutes);
        assertEquals(45, Activities.parse("Úszás 45 perc, ebből 10 perc "
                + "bemelegítés.").plans.get(0).minutes);
        // Az OTT-LÉT idejéből viszont továbbra is kivágjuk a mozgást.
        assertEquals(30, Activities.parse("Uszodában 45 percet voltam, ebből "
                + "kb 30 perc úszás volt.").plans.get(0).minutes);
    }

    /**
     * TÖBB mozgásforma mellett az alkalmak megoszlanak: az „elmúlt 30 napban
     * 22 edzés, 180 km futás, 6 óra kondi" egyetlen száznyolcvan
     * kilométeres futást és egy hatórás kondit írt a naplóba – egy napra.
     */
    @Test
    public void theSessionCountSpreadsOverEveryKind() {
        Activities.Parsed p = Activities.parse("Az elmúlt 30 napban 22 edzés, "
                + "180 km futás, 6 óra kondi.");
        assertEquals(30, p.days);
        assertEquals(2, p.plans.size());
        assertEquals(11, p.plans.get(0).count);
        assertEquals(11, p.plans.get(1).count);
        assertEquals(180.0 / 11, p.plans.get(0).km, 0.01);
        // Egyetlen mozgásformánál változatlan a szétosztás.
        assertEquals(12, Activities.parse("Ez a hónap: 12 edzés, 145 km futás.")
                .plans.get(0).count);
    }


    /**
     * A SZORZÓSZÁMBÓL képzett kör nem kimondott terv: a „ma 3-szor 15 perc
     * sétát iktattam be a munka között" tizenöt perce munkaként ÉS
     * pihenőként is bekerült egy időzítő-tervbe, a negyvenöt perc séta meg
     * nyomtalanul eltűnt a napló mellől.
     */
    @Test
    public void aMultiplierIsNotATimerPlan() {
        Activities.Parsed p = Activities.parse("Ma 3-szor 15 perc sétát "
                + "iktattam be a munka között.");
        assertEquals(1, p.plans.size());
        assertEquals(3, p.plans.get(0).count);
        assertEquals(15, p.plans.get(0).minutes);
        assertNull(IntervalParse.parse("ma 3-szor 15 perc sétát iktattam be"));
        // A kimondott körszám és pihenő továbbra is terv.
        assertNotNull(IntervalParse.parse("4x4 perc kemény futás, közte 3 perc pihi"));
    }


    /**
     * Az ÖSSZETETT szó is a gyerek eseménye, és az összetett „edzéstervem"
     * is terv: az „a gyerek focimeccsére vittem el, én közben 40 percet
     * sétáltam a pálya körül" kilencven perc focit írt a naplómba, az
     * „edzéstervem szerint ma pihenőnap van, de csináltam 20 perc
     * mobilitást" húsz perce meg elveszett.
     */
    @Test
    public void compoundWordsKeepTheirMeaning() {
        Activities.Parsed p = Activities.parse("A gyerek focimeccsére vittem "
                + "el, én közben 40 percet sétáltam a pálya körül.");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(40, p.plans.get(0).minutes);
        Activities.Parsed r = Activities.parse("Az edzéstervem szerint ma "
                + "pihenőnap van, de csináltam 20 perc mobilitást.");
        assertEquals(1, r.plans.size());
        assertEquals(20, r.plans.get(0).minutes);
        // A puszta pihenőnap továbbra sem edzés.
        assertEquals(0, Activities.parse("Ma pihenőnap.").plans.size());
    }

    /**
     * A „MINDEN MÁSNAP" ugyanaz a ritmus, mint a „másnaponta": az „elmúlt két
     * hétben minden másnap futottam 5 km-t" egyetlen futást írt a naplóba a
     * hétből.
     */
    @Test
    public void everyOtherDayIsAFrequency() {
        Activities.Parsed p = Activities.parse("Az elmúlt két hétben minden "
                + "másnap futottam 5 km-t.");
        assertEquals(14, p.days);
        assertEquals(7, p.plans.get(0).count);
        assertEquals(5.0, p.plans.get(0).km, 0.01);
    }


    /**
     * A KIHAGYÁS ideje nem a bejegyzés napja, és az ELÖL álló terv sem törli
     * a megtörténtet: a „két hónapja nem futottam, ma újra: 4 km, 26 perc"
     * mai futása hatvan nappal ezelőttre került, a „két hét múlva verseny
     * lesz, ma 12 km-t futottam rá készülve" tizenkét kilométere pedig
     * teljesen elveszett.
     */
    @Test
    public void theBreakAndThePlanDoNotMoveTheEntry() {
        Activities.Parsed p = Activities.parse("Két hónapja nem futottam, "
                + "ma újra: 4 km, 26 perc, kicsit nehéz volt.");
        assertEquals(0, p.offset);
        assertEquals(4.0, p.plans.get(0).km, 0.01);
        Activities.Parsed r = Activities.parse("Két hét múlva verseny lesz, "
                + "ma 12 km-t futottam rá készülve.");
        assertEquals(1, r.plans.size());
        assertEquals(12.0, r.plans.get(0).km, 0.01);
        // A valódi visszatekintés marad.
        assertEquals(5, Activities.parse("5 napja futottam 10 km-t.").offset);
        assertEquals(14, Activities.parse("Két hete kondi 45 perc.").offset);
        // A tisztán jövő idejű mondat továbbra sem napló.
        assertEquals(0, Activities.parse("Holnap futok 5 km-t.").plans.size());
    }


    /**
     * Az ODA és a VISSZA ÓRÁBAN mondott ideje is összeadódik: a „ma a hegyre
     * másztunk fel, 3 óra oda, 2 óra vissza" három órát írt a naplóba az
     * ötből – a lefelé út nyomtalanul eltűnt.
     */
    @Test
    public void bothLegsCountInHoursToo() {
        assertEquals(300, Activities.parse("Ma a hegyre másztunk fel, "
                + "3 óra oda, 2 óra vissza.").plans.get(0).minutes);
        assertEquals(170, Activities.parse("Túra: 90 perc oda, 80 perc vissza.")
                .plans.get(0).minutes);
        // A fordított sorrend változatlan.
        assertEquals(33, Activities.parse("Munkába menet 15 perc bicikli, "
                + "vissza 18 perc.").plans.get(0).minutes);
    }


    /**
     * A KIMONDOTT TÁV erősebb az emeletből számolt percnél, és az ÖSSZESEN
     * a saját tagmondatában oszt: a „12 emelet lépcső, utána 2 km gyaloglás"
     * két kilométeres sétája hat percesre zsugorodott, a „kb 3-4 km-t
     * sétálhattam ma összesen, plusz 20 perc bringa" húsz perce meg
     * tíz-tízre feleződött a két mozgás között.
     */
    @Test
    public void theStatedDistanceAndTheSumStayInTheirPlace() {
        Activities.Parsed p = Activities.parse("12 emelet lépcső, utána 2 km gyaloglás.");
        assertEquals(1, p.plans.size());
        assertEquals(2.0, p.plans.get(0).km, 0.01);
        assertEquals(24, p.plans.get(0).minutes);
        Activities.Parsed q = Activities.parse("kb 3-4 km-t sétálhattam ma "
                + "összesen, plusz 20 perc bringa");
        assertEquals(2, q.plans.size());
        assertEquals(20, q.plans.get(1).minutes);
        // Az emeletből számolt perc táv nélkül marad, és az ÖSSZESEN a saját
        // tagmondatában továbbra is oszt.
        assertEquals(6, Activities.parse("12 emelet lépcső.").plans.get(0).minutes);
        assertEquals(35, Activities.parse("Futás és kondi, összesen 70 perc.")
                .plans.get(0).minutes);
    }


    /**
     * A FELTÉTELES „lenne" javaslat, a JELEN idejű ige pedig tartamot mond:
     * az „edző szerint túl sokat futok, heti 60 km elég lenne" hatvan
     * kilométeres futást írt a naplóba, a „két hónapja edzek, azóta 12 kg-ot
     * emelkedett a fekvenyomásom" bejegyzése meg hatvan nappal ezelőttre
     * került.
     */
    @Test
    public void adviceAndDurationsAreNotEntries() {
        Activities.Parsed p = Activities.parse("Az edző szerint túl sokat "
                + "futok, heti 60 km elég lenne.");
        for (Activities.Plan pl : p.plans) assertEquals(0.0, pl.km, 0.01);
        assertEquals(0, Activities.parse("Két hónapja edzek, azóta 12 kg-ot "
                + "emelkedett a fekvenyomásom.").offset);
        // A valódi visszatekintés marad.
        assertEquals(14, Activities.parse("Két hete kondi 45 perc.").offset);
    }

    /**
     * A „N km után" a váltás pontja, nem a következő mozgás távja.
     *
     * A „futottam, de fájt a térdem, a 6. km után sétáltam haza" hat
     * kilométerét a hazasétálás vitte el – hatkilométeres túra lett belőle,
     * a megfutott táv pedig nyomtalanul eltűnt. A váltópontig megtett út
     * azé a mozgásé, amelyik ELŐTTE van kimondva.
     */
    @Test
    public void theDistanceBeforeTheSwitchBelongsToWhatCameFirst() {
        Activities.Parsed p = Activities.parse("Futottam, de fájt a térdem, "
                + "a 6. km után sétáltam haza.");
        double run = 0, walk = 0;
        for (Activities.Plan pl : p.plans) {
            if (pl.kind.id.equals("futas")) run = pl.km;
            if (pl.kind.id.equals("tura")) walk = pl.km;
        }
        assertEquals(6.0, run, 0.01);
        assertEquals(0.0, walk, 0.01);
        // Az „utána" csak sorrendet jelöl: ott marad a szomszédság joga.
        Activities.Parsed q = Activities.parse("Futottam 6 km-t, utána "
                + "sétáltam haza.");
        for (Activities.Plan pl : q.plans)
            if (pl.kind.id.equals("futas")) assertEquals(6.0, pl.km, 0.01);
    }

    /**
     * A labdajáték igéje mellett a „2x30 perc" két félidő.
     *
     * Az „este a haverokkal fociztunk a pályán, 2x30 perc" harminc percet
     * írt a naplóba a hatvanból – a másik félidő nyomtalanul eltűnt. A
     * meccs szava eddig is összeadta a félidőket, a játék igéje viszont
     * nem. A puszta „2x45 perc foci" (ige nélkül) szándékosan marad két
     * alkalom: az sorozatot is jelenthet.
     */
    @Test
    public void theSecondHalfCountsToo() {
        Activities.Parsed p = Activities.parse("Este a haverokkal fociztunk "
                + "a pályán, 2x30 perc.");
        assertEquals(1, p.plans.size());
        assertEquals(60, p.plans.get(0).minutes);
        Activities.Parsed q = Activities.parse("Kosárlabda edzés, "
                + "2x25 perc játék.");
        assertEquals(50, q.plans.get(0).minutes);
    }

    /**
     * A saját számával álló lépés-szó nem keresi a következő tagmondatét.
     *
     * A „12 000 lépés, 620 aktív kalória, 72 átlagpulzus, 6 óra 40 perc
     * alvás" órakivonatában a hatszázhúsz lett a lépésszám, és a
     * tizenkétezer lépésből félkilométeres séta maradt.
     */
    @Test
    public void theStepCountIsNotStolenByTheNextClause() {
        Activities.Parsed p = Activities.parse("12 000 lépés, 620 aktív "
                + "kalória, 72 átlagpulzus, 6 óra 40 perc alvás – Garmin.");
        assertEquals(1, p.plans.size());
        assertEquals(9.0, p.plans.get(0).km, 0.2);
        // A szám nélküli lépés-szó után álló szám marad lépésszám.
        Activities.Parsed q = Activities.parse("12000 lépés ma.");
        assertEquals(9.0, q.plans.get(0).km, 0.2);
    }

    /**
     * A kör nem óraállás, a plank ideje nem az edzés hossza.
     *
     * Ékezet nélkül a „kör" és a „-kor" egybeesik: az „otthon nyomtam egy
     * saját testsúlyos kört: 3 kör fekvőtámasz, guggolás, plank" hajnali
     * háromra tette a bejegyzést. A „saját testsúlyos edzés, 4 kör: 15
     * fekvőtámasz, 20 guggolás, 1 perc plank" pedig EGYPERCES kondiedzés
     * lett – a plank ideje vitte el az egész edzés hosszát.
     */
    @Test
    public void aRoundIsNotAnHourAndAPlankIsNotTheSession() {
        assertEquals(12, Activities.parse("Otthon nyomtam egy saját "
                + "testsúlyos kört: 3 kör fekvőtámasz, guggolás, plank.").hour);
        Activities.Parsed p = Activities.parse("Saját testsúlyos edzés, "
                + "4 kör: 15 fekvőtámasz, 20 guggolás, 1 perc plank.");
        assertEquals(60, p.plans.get(0).minutes);
        // A napszakkal kimondott óra marad óra.
        assertEquals(7, Activities.parse("Reggel 7 kor fekvőtámasz "
                + "sorozat.").hour);
        // A saját tagmondatában álló edzéshossz sem vész el.
        Activities.Parsed q = Activities.parse("Kondi 45 perc, plank 3x45 mp.");
        assertEquals(45, q.plans.get(0).minutes);
    }

    /**
     * Az „N-szer M méter" ismétlés, nem N külön edzés.
     *
     * A „ma 3-szor 1000 métert úsztam" háromezer méter, de ezer maradt
     * belőle: a rag alkalomszámnak számított, a táv meg egyszer ment be. A
     * rövid „3x1000 m" alak régóta helyesen összeadódott – ez ugyanaz,
     * csak kimondva.
     */
    @Test
    public void aSpelledOutRepeatSumsTheDistance() {
        Activities.Parsed p = Activities.parse("Ma 3-szor 1000 métert úsztam.");
        assertEquals(3.0, p.plans.get(0).km, 0.01);
        Activities.Parsed q = Activities.parse("Ma 5-ször 200 métert úsztam.");
        assertEquals(1.0, q.plans.get(0).km, 0.01);
        // A heti gyakoriság nem szorzó: ott a táv egy edzésé.
        Activities.Parsed r = Activities.parse("A héten 3-szor futottam.");
        assertEquals(3, r.plans.get(0).count);
    }

    /**
     * A hát nap nem hat nap.
     *
     * Ékezet nélkül a testrész és a számnév egybeesik: a „kondiedzés: hát
     * nap, húzódzkodás 4x6, evezés gépen 4x10 50 kg" EGYETLEN edzése hat
     * napra terült szét a naptárban.
     */
    @Test
    public void aBackDayIsNotSixDays() {
        assertEquals(1, Activities.parse("Kondiedzés: hát nap, húzódzkodás "
                + "4x6, evezés gépen 4x10 50 kg.").days);
        assertEquals(1, Activities.parse("Ma hát nap volt a "
                + "konditeremben.").days);
        // A valódi időszak marad hat nap.
        assertEquals(6, Activities.parse("Az elmúlt hat napban 3-szor "
                + "futottam.").days);
        assertEquals(6, Activities.parse("Hat nap alatt 4 edzés a "
                + "kondiban.").days);
    }

    /**
     * A kiírt körszám kör, a napszak melletti kiírt óra pedig óra.
     *
     * Az „öt kör a pályán, egyenként 400 m" kétezer métere helyett
     * négyszáz került a naplóba, öt külön futásként, öt napra osztva. A
     * „délután öt kor edzés" ötöse ugyanígy darabszám lett. Ékezet nélkül a
     * „kör" és a „-kor" egybeesik, a számnév pedig csak később válik
     * számmá.
     */
    @Test
    public void aSpelledOutLapCountIsNotAnHour() {
        Activities.Parsed p = Activities.parse("Öt kör a pályán, "
                + "egyenként 400 m.");
        assertEquals(1, p.plans.size());
        assertEquals(2.0, p.plans.get(0).km, 0.01);
        assertEquals(1, p.days);
        // A napszak melletti óra időpont marad.
        Activities.Parsed q = Activities.parse("Délután öt kor edzés.");
        assertEquals(17, q.hour);
        assertEquals(1, q.plans.get(0).count);
    }

    /**
     * A másik napszak edzése külön alkalom.
     *
     * A „ma reggel edzettem, aztán egész nap ültem, este még egy kis sétát
     * tettem a kutyával" reggeli edzése nyomtalanul eltűnt: a sportnév
     * nélküli „edzés" csak tartalékként kap bejegyzést, és a séta elvitte a
     * helyét. A napszak nélküli „ma edzettem, futottam 5 km-t" viszont
     * marad EGY edzés – ott az „edzés" csak a futás gyűjtőneve.
     */
    @Test
    public void aWorkoutInAnotherDayPartIsItsOwnSession() {
        Activities.Parsed p = Activities.parse("Ma reggel edzettem, aztán "
                + "egész nap ültem, este még egy kis sétát tettem a kutyával.");
        assertEquals(2, p.plans.size());
        boolean generic = false;
        for (Activities.Plan pl : p.plans)
            if (pl.kind.id.equals("egyeb")) generic = true;
        assertTrue(generic);
        // A gyűjtőnév nem lesz külön edzés.
        assertEquals(1, Activities.parse("Ma edzettem, futottam "
                + "5 km-t.").plans.size());
        assertEquals(1, Activities.parse("Ma reggel edzettem, "
                + "5 km futás.").plans.size());
    }

    /**
     * A heti kurzus egyetlen alkalma nem hét nap.
     *
     * Az „elmentem a heti crossfit órára, 60 perc" egyetlen órája hét napra
     * terült szét a naptárban – a „heti" ott a KURZUS neve, nem az
     * összegzés időszaka.
     */
    @Test
    public void aWeeklyClassIsOneOccasion() {
        assertEquals(1, Activities.parse("Elmentem a heti crossfit órára, "
                + "60 perc.").days);
        // A heti összegzés marad heti.
        assertEquals(7, Activities.parse("Heti terhelés: 60 km.").days);
    }

    /**
     * A megállás ideje nem az edzés ideje.
     *
     * A „futottam 5 km-t, közben 10 percet álltam" TÍZPERCES futást írt a
     * naplóba – öt kilométer tíz perc alatt harminc km/h. A megállás és a
     * várakozás perce épp az az idő, amikor nem ment az edzés.
     */
    @Test
    public void theMinutesSpentStandingAreNotTheSession() {
        Activities.Parsed p = Activities.parse("Futottam 5 km-t, közben "
                + "10 percet álltam.");
        assertEquals(5.0, p.plans.get(0).km, 0.01);
        assertTrue(p.plans.get(0).minutes > 20);
        // A buszra várt perc sem edzés.
        Activities.Parsed q = Activities.parse("Vártam 10 percet a buszra, "
                + "aztán 30 perc futás.");
        assertEquals(30, q.plans.get(0).minutes);
        // A kimondott idő attól még hitelesíti az edzést: a mondat nem
        // eshet ki azzal együtt, hogy a perc nem a futásé.
        assertEquals(1, Activities.parse("A futásom közben megállított egy "
                + "ismerős, így 10 percet álltam.").plans.size());
    }

    /**
     * A másik napszak súlyzós edzése is külön alkalom.
     *
     * A „reggel 60 kg-mal guggoltam 5x5-öt, este 8 km-t futottam" reggeli
     * terme nyomtalanul eltűnt a naptárból: a sorozat bekerült az
     * erőnaplóba, edzés viszont nem lett belőle, mert az esti futás elvitte
     * a helyét.
     */
    @Test
    public void aMorningLiftIsNotErasedByTheEveningRun() {
        Activities.Parsed p = Activities.parse("Reggel 60 kg-mal guggoltam "
                + "5x5-öt, este 8 km-t futottam.");
        assertEquals(2, p.plans.size());
        boolean gym = false;
        for (Activities.Plan pl : p.plans)
            if (pl.kind.id.equals("kondi")) gym = true;
        assertTrue(gym);
        // Egy napszakon belül a sorozat továbbra sem külön edzés.
        assertEquals(1, Activities.parse("Kondi: guggolás 5x5 100 kg, "
                + "fekvenyomás 3x8 70 kg.").plans.size());
    }

    /**
     * A heti és a havi összesítő napló, nem emlék.
     *
     * A „heti összesítőm: 42 km futás, 3 kondi, 1 úszás" bejegyzésből SEMMI
     * nem lett, pedig a „heti összesítés" ugyanezzel a tartalommal rendben
     * bement – az összesítő szava az évadra szólót is, a hetit is
     * elnémította. A „havi összesítés: 120 km futás, 12 edzés" pedig
     * EGYETLEN napra tette a százhúsz kilométert: a rendhagyó „havi" jelzőt
     * a hossz-olvasó nem ismerte.
     */
    @Test
    public void weeklyAndMonthlySummariesAreKept() {
        Activities.Parsed p = Activities.parse("A heti összesítőm: 42 km "
                + "futás, 3 kondi, 1 úszás.");
        assertEquals(7, p.days);
        assertEquals(3, p.plans.size());
        assertEquals(30, Activities.parse("Havi összesítés: 120 km futás, "
                + "12 edzés.").days);
        // Az évi összesítő marad emlék.
        assertTrue(Activities.parse("Az évi összesítő: 1200 km "
                + "futás.").plans.isEmpty());
    }

    /**
     * Külön napszakban álló mozgásoknak nincs közös hosszuk.
     *
     * A „reggel 7-kor úszás, 1500 m, délután 5-kor kondi, 1 óra" egy órája
     * a délutáni teremé – az úszás mégis megkapta, és EGYÓRÁS, másfél
     * kilométeres úszás lett belőle. Két külön alkalmat nem összegez senki
     * egyetlen számmal.
     */
    @Test
    public void twoDayPartsDoNotShareOneDuration() {
        Activities.Parsed p = Activities.parse("Reggel 7-kor úszás, 1500 m, "
                + "délután 5-kor kondi, 1 óra.");
        assertEquals(2, p.plans.size());
        assertTrue(p.plans.get(0).minutes < 50);
        assertEquals(60, p.plans.get(1).minutes);
        // Az összefoglaló hossz egy napszakon belül marad közös.
        Activities.Parsed q = Activities.parse("Ma futás és 2 úszás, 40 perc.");
        for (Activities.Plan pl : q.plans) assertEquals(40, pl.minutes);
    }

    /**
     * A „naponta" ugyanaz, mint a „napi".
     *
     * Az „a hétvégén 2 napig túráztunk a Bükkben, naponta kb 20 km" húsz
     * kilométere NYOMTALANUL eltűnt – a „napi 20 km" viszont ugyanabban a
     * mondatban rendben napi húszat írt be. Ugyanarra a bejegyzésre két
     * gyökeresen más eredmény, csak a szó alakja miatt.
     */
    @Test
    public void everyDayMeansPerDay() {
        Activities.Parsed p = Activities.parse("A hétvégén 2 napig "
                + "túráztunk a Bükkben, naponta kb 20 km.");
        assertEquals(2, p.days);
        assertEquals(20.0, p.plans.get(0).km, 0.01);
        assertEquals(2, p.plans.get(0).count);
        // Hátravetve is ugyanaz.
        assertEquals(20.0, Activities.parse("2 napig túráztunk, 20 km "
                + "naponta.").plans.get(0).km, 0.01);
    }

    /**
     * Az órától óráig tartó edzés hossza kiszámolható.
     *
     * A „ma reggel 6-tól 7-ig futottam a parkban" hatvan perce elveszett, és
     * a futás a negyvenöt perces alapértelmezést kapta – kevesebb került a
     * naplóba, mint amennyit az ember lefutott.
     */
    @Test
    public void aClockRangeGivesTheDuration() {
        assertEquals(60, Activities.parse("Ma reggel 6-tól 7-ig futottam "
                + "a parkban.").plans.get(0).minutes);
        assertEquals(60, Activities.parse("Reggel 6 órától 7 óráig "
                + "futottam.").plans.get(0).minutes);
        assertEquals(60, Activities.parse("Ma 19:00-tól 20:00-ig "
                + "kondi.").plans.get(0).minutes);
        // A munkaidő nem edzés: ott nem számolunk hosszt.
        Activities.Parsed p = Activities.parse("8-tól 16-ig dolgoztam, "
                + "este 30 perc futás.");
        assertEquals(30, p.plans.get(0).minutes);
    }

    /**
     * A „futotta" itt nem futás.
     *
     * A „ma az egész nap ülőmunka volt, este csak 20 perc sétára futotta"
     * mondatban a szó annyit tesz, hogy „ennyire tellett" – mégis egy
     * negyvenöt perces FUTÁS került tőle a naplóba, a valódi húszperces
     * séta mellé.
     */
    @Test
    public void theWordForSufficingIsNotARun() {
        Activities.Parsed p = Activities.parse("Ma az egész nap ülőmunka "
                + "volt, este csak 20 perc sétára futotta.");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(20, p.plans.get(0).minutes);
        // A lefutott maraton marad futás.
        assertEquals("futas", Activities.parse("Lefutotta a maratont.")
                .plans.get(0).kind.id);
    }

    /**
     * A kimondott hossz nem véd meg a téves darabszámtól.
     *
     * A „3x8 fekvenyomás 60kg, 3x10 evezés 50kg, 3x12 bicepsz 12kg – kb 50
     * perc" NYOLC edzésre esett szét, nyolc napra osztva: a nyolcas az
     * ismétlésszámból jött. A javítás eddig csak az alapértelmezett hosszú
     * tervre élt – pedig a kimondott ötven perc épp azt erősíti meg, hogy
     * EGY edzésről van szó.
     */
    @Test
    public void aStatedLengthDoesNotKeepTheWrongCount() {
        Activities.Parsed p = Activities.parse("3x8 fekvenyomás 60kg, "
                + "3x10 evezés 50kg, 3x12 bicepsz 12kg - kb 50 perc");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(50, p.plans.get(0).minutes);
    }

    /**
     * A nevezetes futókör is helyszín, nem óraállás.
     *
     * A „ma 3 kör a Margitszigeten, ez kb 16 km" HAJNALI HÁROMRA tette a
     * bejegyzést: ékezet nélkül a „kör" és a „-kor" egybeesik, a sziget
     * pedig nem szerepelt a helyszínek között.
     */
    @Test
    public void aFamousLoopIsAPlaceNotAnHour() {
        assertEquals(12, Activities.parse("ma 3 kör a Margitszigeten, "
                + "ez kb 16 km").hour);
        // A napszakkal kimondott óra marad óra.
        assertEquals(17, Activities.parse("Délután 5 kor 8 km futás.").hour);
    }

    /**
     * A második alkalom száma a mértékegységét az elsőtől kapja.
     *
     * A „ma délelőtt bicikliztem 25 km-t, délután még 10-et" tíz kilométere
     * NYOMTALANUL eltűnt – a nap fele kimaradt a naplóból. A magyar így
     * rövidít: a mértékegység egyszer van kimondva.
     */
    @Test
    public void theSecondNumberInheritsTheUnit() {
        Activities.Parsed p = Activities.parse("Ma délelőtt bicikliztem "
                + "25 km-t, délután még 10-et.");
        assertEquals(2, p.plans.size());
        assertEquals(25.0, p.plans.get(0).km, 0.01);
        assertEquals(10.0, p.plans.get(1).km, 0.01);
        // Sorozatjelölés mellett a puszta szám marad ismétlés.
        assertTrue(Activities.parse("Nyomtam 3x10-et, aztán még "
                + "8-at.").plans.isEmpty());
    }

    /**
     * A „N hét múlva" tagmondata terv, nem napló.
     *
     * A „két hét múlva félmaraton, ma 16 km-t futottam felkészülésként"
     * mondatból a mai tizenhat MELLÉ egy huszonegy kilométeres félmaraton
     * is bekerült – harminchét kilométer abból a tizenhatból, ami megvolt.
     * A verseny neve önmagában távot is jelent.
     */
    @Test
    public void aRaceInTwoWeeksIsNotTodaysRun() {
        Activities.Parsed p = Activities.parse("Két hét múlva félmaraton, "
                + "ma 16 km-t futottam felkészülésként.");
        assertEquals(1, p.plans.size());
        assertEquals(16.0, p.plans.get(0).km, 0.01);
        // A lefutott félmaraton marad félmaraton.
        assertEquals(21.1, Activities.parse("Ma lefutottam a félmaratont.")
                .plans.get(0).km, 0.2);
    }

    /**
     * A „kell mozognom" tanács, nem edzés.
     *
     * Az „az orvos szerint többet kell mozognom, ma elkezdtem: 20 perc
     * séta" bejegyzésbe a húszperces séta MELLÉ egy negyvenöt perces
     * „egyéb mozgás" is bekerült – abból a tagmondatból, ami épp csak
     * javasol.
     */
    @Test
    public void adviceWithAnInfinitiveIsNotASession() {
        Activities.Parsed p = Activities.parse("Az orvos szerint többet kell "
                + "mozognom, ma elkezdtem: 20 perc séta.");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals(20, p.plans.get(0).minutes);
    }

    /**
     * A maratonra készülni nem maratont futni.
     *
     * A „ma kezdődött a felkészülésem a maratonra" mondatból NEGYVENKÉT
     * KILOMÉTERES futás lett a mai napra – a verseny neve önmagában távot
     * is jelent. A -ra/-re rag célt jelöl; a lefutott maraton tárgyesetben
     * áll.
     */
    @Test
    public void preparingForARaceIsNotRunningIt() {
        assertTrue(Activities.parse("Ma kezdődött a felkészülésem a "
                + "maratonra.").plans.isEmpty());
        assertTrue(Activities.parse("Neveztem a maratonra.").plans.isEmpty());
        // A felkészülés napi futása megmarad.
        Activities.Parsed p = Activities.parse("Készülök a félmaratonra, "
                + "ma 12 km.");
        assertEquals(1, p.plans.size());
        assertEquals(12.0, p.plans.get(0).km, 0.01);
        // A lefutott maraton marad maraton.
        assertEquals(42.2, Activities.parse("Ma lefutottam a maratont.")
                .plans.get(0).km, 0.3);
    }

    /**
     * A zárva tartó terem nem edzés.
     *
     * A „ma elmentem az edzőterembe, de zárva volt, így hazamentem"
     * HATVANPERCES kondiedzést írt a naplóba – egy napra, amikor épp NEM
     * edzett az ember. Az elmaradt és a lemondott edzés ugyanez.
     */
    @Test
    public void aClosedGymIsNotASession() {
        assertTrue(Activities.parse("Ma elmentem az edzőterembe, de zárva "
                + "volt, így hazamentem.").plans.isEmpty());
        assertTrue(Activities.parse("A mai foci elmaradt.").plans.isEmpty());
        // A csere és a kimondott szám felment.
        assertEquals("futas", Activities.parse("úszodába mentem de zárva "
                + "volt, helyette 5 km futás").plans.get(0).kind.id);
        assertEquals(1, Activities.parse("Az edzőterem zárva volt, így "
                + "otthon nyomtam egy kört: 3 kör fekvőtámasz.").plans.size());
    }

    /**
     * A mai nap kimondása erősebb a visszatekintésnél, a bontás nem hossz.
     *
     * A „ma végre elmentem futni, 4 km, első alkalom 2 hónapja" mai futása
     * HATVAN NAPPAL EZELŐTTRE került: a mai nap üresen maradt, a két
     * hónappal ezelőtti pedig kapott egy soha meg nem történt edzést. A
     * „ma 90 percet edzettem, ebből 30 perc kardió" pedig HARMINC PERCES
     * bejegyzés lett a kilencvenből.
     */
    @Test
    public void todayBeatsTheLookBackAndTheBreakdown() {
        assertEquals(0, Activities.parse("Ma végre elmentem futni, 4 km, "
                + "első alkalom 2 hónapja.").offset);
        assertEquals(90, Activities.parse("Ma 90 percet edzettem, ebből "
                + "30 perc kardió.").plans.get(0).minutes);
        // A valódi visszatekintés marad.
        assertEquals(5, Activities.parse("5 napja futottam 10 km-t.").offset);
        assertEquals(14, Activities.parse("Két hete kondi 45 perc.").offset);
    }

    /**
     * A meccs sportág nélkül is mozgás.
     *
     * Az „a meccsen 60 percet játszottam, aztán lecseréltek" hatvan perce
     * NYOMTALANUL eltűnt: a mérkőzés szava magában nem sportág, a
     * bejegyzésből pedig semmi nem lett.
     */
    @Test
    public void aMatchWithoutASportIsStillMovement() {
        Activities.Parsed p = Activities.parse("A meccsen 60 percet "
                + "játszottam, aztán lecseréltek.");
        assertEquals(1, p.plans.size());
        assertEquals(60, p.plans.get(0).minutes);
        // A néző és a kísérő továbbra sem játszik.
        assertTrue(Activities.parse("A gyerek focimeccsére kísértem el, "
                + "én a lelátón ültem végig.").plans.isEmpty());
        // A megnevezett sport nyer a gyűjtőnév felett.
        assertEquals("foci", Activities.parse("A focimeccsen 60 percet "
                + "játszottam.").plans.get(0).kind.id);
    }

    /**
     * A szorzójeles intervall a napszak mellett is szorzat.
     *
     * Az „este 4x400 métert futottam" NÉGY KÜLÖN futásra esett szét, négy
     * napra osztva – csak mert napszak állt előtte. A napszak-kivétel a
     * „reggel 7 kor 5 km" órájának szólt, ahol a „kör" és a „-kor"
     * egybeesik; a szorzójel viszont félreérthetetlen.
     */
    @Test
    public void aTimesSignIsAMultiplierEvenAfterADayPart() {
        Activities.Parsed p = Activities.parse("Este 4x400 métert futottam.");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(1.6, p.plans.get(0).km, 0.01);
        Activities.Parsed q = Activities.parse("Este 4 x 500 m úsztam, "
                + "köztük 1 perc pihenő.");
        assertEquals(2.0, q.plans.get(0).km, 0.01);
        // A napszak melletti óra marad óra.
        assertEquals(7, Activities.parse("Reggel 7 kor 5 km futás.").hour);
    }

    /**
     * Az edzőterem a helyszín, nem külön edzés.
     *
     * A „ma reggel az edzőteremben 20 perc futópad, aztán 40 perc súlyzó"
     * bejegyzésbe HÁROM mozgás került: egy húszperces kondi (a teremből),
     * egy negyvenöt perces futás (alapértelmezett hosszal, mert a húsz
     * percét elvitte a terem) és egy negyvenperces kondi.
     */
    @Test
    public void theGymIsAPlaceNotAThirdSession() {
        Activities.Parsed p = Activities.parse("Ma reggel az edzőteremben "
                + "20 perc futópad, aztán 40 perc súlyzó.");
        assertEquals(2, p.plans.size());
        assertEquals("futas", p.plans.get(0).kind.id);
        assertEquals(20, p.plans.get(0).minutes);
        assertEquals("kondi", p.plans.get(1).kind.id);
        assertEquals(40, p.plans.get(1).minutes);
        // A saját tagmondatában álló terem maga az edzés.
        Activities.Parsed q = Activities.parse("reggel futottam 5 km-t, "
                + "este konditeremben voltam egy órát");
        assertEquals(2, q.plans.size());
        assertEquals(60, q.plans.get(1).minutes);
    }

    /**
     * Az egybeírt kertimunka is kerti munka.
     *
     * A „délelőtt bevásárlás, délután 1,5 óra kertimunka" másfél órája
     * NYOMTALANUL eltűnt, míg a „kerti munka" külön írva rendben bement –
     * ugyanaz a nap, két helyesírás, két gyökeresen más eredmény.
     */
    @Test
    public void gardenWorkWrittenAsOneWordCountsToo() {
        Activities.Parsed p = Activities.parse("Délelőtt bevásárlás, "
                + "délután 1,5 óra kertimunka.");
        assertEquals(1, p.plans.size());
        assertEquals(90, p.plans.get(0).minutes);
        assertEquals("munka", p.plans.get(0).kind.id);
    }

    /**
     * A defekt helye a megtett táv, a biciklitolás pedig séta.
     *
     * A „biciklim defektet kapott 20 km-nél, onnan toltam hazáig 3 km-t"
     * bejegyzésből HÁROM kilométeres tekerés lett: a húszas
     * helymegjelölésként kiesett, a hazatolás három kilométere pedig a
     * bringához tapadt. Pedig a tekerés húsz kilométer volt, a hazatolás
     * meg gyaloglás.
     */
    @Test
    public void aFlatTireMarksTheRiddenDistance() {
        Activities.Parsed p = Activities.parse("A biciklim defektet kapott "
                + "20 km-nél, onnan toltam hazáig 3 km-t.");
        double bike = 0, walk = 0;
        for (Activities.Plan pl : p.plans) {
            if (pl.kind.id.equals("kerekpar")) bike = pl.km;
            if (pl.kind.id.equals("tura")) walk = pl.km;
        }
        assertEquals(20.0, bike, 0.01);
        assertEquals(3.0, walk, 0.01);
        // A helymegjelölés kimondott össztáv mellett marad pont.
        assertEquals(15.0, Activities.parse("Futás: 10 km-nél megálltam "
                + "inni, összesen 15 km lett.").plans.get(0).km, 0.01);
    }

    /**
     * A megállások száma nem alkalomszám.
     *
     * Az „összesen 6 km-t futottam ma, de kétszer álltam meg közben" KÉT
     * darab három kilométeres futás lett: a megállások száma elvitte az
     * alkalomszámot, az „összesen" pedig elosztotta köztük a távot. Aki
     * megáll pihenni, attól még egyszer futott.
     */
    @Test
    public void theNumberOfStopsIsNotTheSessionCount() {
        Activities.Parsed p = Activities.parse("Összesen 6 km-t futottam "
                + "ma, de kétszer álltam meg közben.");
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(6.0, p.plans.get(0).km, 0.01);
        // A valódi alkalomszám marad.
        assertEquals(2, Activities.parse("Kétszer futottam ma.")
                .plans.get(0).count);
    }

    /**
     * A sőt utáni szám az igazi lépésszám.
     *
     * A „ma végre elértem a napi 10 000 lépést, sőt 12 300 lett!" hét és
     * fél kilométeres sétát írt be a kilenc-kettő helyett: a CÉL tízezrese
     * ment be, a valódi tizenkétezer-háromszáz elveszett.
     */
    @Test
    public void theNumberAfterSotWins() {
        Activities.Parsed p = Activities.parse("Ma végre elértem a napi "
                + "10 000 lépést, sőt 12 300 lett!");
        assertEquals(12300, p.plans.get(0).steps);
    }

    /**
     * A két hét múlva esedékes félmaraton nem mai futás.
     *
     * A „két hét múlva félmaraton, ma megvolt az utolsó hosszú futás:
     * 18 km" tizennyolc kilométere MELLÉ egy huszonegy kilométeres
     * félmaraton is bekerült – a „megvolt" nem számított múlt időnek, a
     * vezető terv-tagmondat állva maradt, a verseny neve pedig önmagában
     * távot jelent. A magában álló terv ugyanígy nem napló.
     */
    @Test
    public void aRaceTwoWeeksAwayIsNotTodaysRun() {
        Activities.Parsed p = Activities.parse("Két hét múlva félmaraton, "
                + "ma megvolt az utolsó hosszú futás: 18 km.");
        assertEquals(1, p.plans.size());
        assertEquals(18.0, p.plans.get(0).km, 0.01);
        assertTrue(Activities.parse("Két hét múlva félmaraton.")
                .plans.isEmpty());
        // A lefutott félmaraton marad.
        assertEquals(21.1, Activities.parse("Ma lefutottam a félmaratont.")
                .plans.get(0).km, 0.05);
    }

    /**
     * A rag nélküli óraállás időpont, a sorszámos alkalom egy nap.
     *
     * A „reggel 6:30: 20 perc mobilitás" fejlécéből HARMINC edzés lett
     * harminc napra osztva: a kettőspont utáni perc levált, és a 30
     * darabszámmá vált. Az „az első 5 km a héten" pedig hét napra terült
     * szét, pedig egyetlen mai futásról szól.
     */
    @Test
    public void aBareClockIsATimeAndAnOrdinalIsOneDay() {
        Activities.Parsed p = Activities.parse("Reggel 6:30: 20 perc "
                + "mobilitás és 10 perc légzőgyakorlat.");
        assertEquals(1, p.days);
        assertEquals(6, p.hour);
        for (Activities.Plan pl : p.plans) assertEquals(1, pl.count);
        Activities.Parsed q = Activities.parse("Két kávé után végre "
                + "lement az első 5 km a héten.");
        assertEquals(1, q.days);
        // A versenyidő és a heti összeg marad.
        assertEquals(53, Activities.parse("A 10 km-es futásom ideje 52:30 "
                + "volt, új rekord!").plans.get(0).minutes);
        assertEquals(7, Activities.parse("A héten összesen 20 km-t "
                + "futottam.").days);
    }

    /**
     * A délutáni óraszám óraállás, nem tizenhét órányi futás.
     *
     * A „17 óra körül futottam egy könnyed 7 km-t" bejegyzésbe EZERHÚSZ
     * perces futás került: a tizenhét óra időtartammá vált. A 13-23
     * közötti óraszám a „körül" mellett csak óraállás lehet.
     */
    @Test
    public void anAfternoonClockIsNotSeventeenHours() {
        Activities.Parsed p = Activities.parse("17 óra körül futottam egy "
                + "könnyed 7 km-t a folyóparton.");
        assertEquals(17, p.hour);
        assertEquals(42, p.plans.get(0).minutes);
        // A valódi óra-hossz marad.
        assertEquals(120, Activities.parse("Kb 2 órát bicikliztem.")
                .plans.get(0).minutes);
    }

    /**
     * A képesség körülmény a megtörtént edzés mellett, a vélemény meg cipő.
     *
     * A „nyári melegben csak este tudok mozogni, ma 21:30-kor futottam
     * 6 km-t" hat kilométere nyomtalanul eltűnt: az első tagmondat
     * képesség-alakja az egész bejegyzést elnémította. Az „az új cipőm…
     * sokkal kényelmesebb futni benne" pedig negyvenöt perces futást írt
     * be – egy cipő-véleményből.
     */
    @Test
    public void anAbilityClauseDoesNotSilenceThePastRun() {
        Activities.Parsed p = Activities.parse("Nyári melegben csak este "
                + "tudok mozogni, ma 21:30-kor futottam 6 km-t.");
        assertEquals(1, p.plans.size());
        assertEquals(6.0, p.plans.get(0).km, 0.01);
        assertEquals(21, p.hour);
        assertTrue(Activities.parse("Az új cipőm 42,5-es, fél mérettel "
                + "nagyobb, sokkal kényelmesebb futni benne.")
                .plans.isEmpty());
        // A puszta képesség marad a jövő-felismerőé.
        assertTrue(Activities.parse("fáj a térdem 2 hete, de futni tudok")
                .plans.isEmpty());
    }

    /**
     * A heti sorszámos vagy birtokos alkalom egyetlen nap.
     *
     * Az „a heti 4. edzésem: válogatott gyakorlatok, 55 perc" hét napra
     * terült szét, pedig a hét NEGYEDIK edzéséről szól. A „letudtam a
     * heti 3. futást" ugyanígy: a sorszámot maszk veszi ki, de a birtokos
     * és a tárgyrag kimondja, hogy egy konkrét alkalomról van szó.
     */
    @Test
    public void theWeeksNthSessionIsOneDay() {
        assertEquals(1, Activities.parse("A heti 4. edzésem: válogatott "
                + "gyakorlatok, 55 perc, jó tempóban.").days);
        assertEquals(1, Activities.parse("Letudtam a heti 3. futást, "
                + "8 km.").days);
        assertEquals(1, Activities.parse("Heti futásom: 8 km.").days);
        // A heti összeg marad heti.
        assertEquals(7, Activities.parse("Heti terhelés: 60 km.").days);
    }

    /**
     * Az „az is kardió" záró kommentár nem külön edzés.
     *
     * A „gyors tempóban toltam a babakocsit 40 percig, az is kardio"
     * negyven perce mellé egy második, negyvenöt perces „egyéb mozgás" is
     * bekerült – ugyanarról a sétáról, a rámutató kommentárból.
     */
    @Test
    public void aClosingRemarkIsNotASecondSession() {
        Activities.Parsed p = Activities.parse("Reggel gyors tempóban "
                + "toltam a babakocsit 40 percig, az is kardio.");
        assertEquals(1, p.plans.size());
        assertEquals(40, p.plans.get(0).minutes);
        // A minősítő zárszó sem szül újat.
        assertEquals(1, Activities.parse("Ma 40 perc futás, kemény "
                + "edzés.").plans.size());
    }

    /**
     * A jövő heti versenyRE készülés nem mai verseny, és a két úszás kettő.
     *
     * Az „a jövő heti maratonra ma már csak 5 km lazítás volt" öt
     * kilométere eltűnt: az egy-tagmondatos mondat egésze tervnek látszott,
     * pedig a -ra rag kimondja, hogy a verseny csak a cél. A „ma 2 úszás
     * volt: reggel 1000 m, este 1500 m" második úszása pedig elveszett – a
     * puszta méteres táv csak tárgyraggal számított.
     */
    @Test
    public void aRaceAsPurposeAndTwoSwimsInMeters() {
        Activities.Parsed p = Activities.parse("A jövő heti maratonra ma "
                + "már csak 5 km lazítás volt.");
        assertEquals(1, p.plans.size());
        assertEquals(5.0, p.plans.get(0).km, 0.01);
        Activities.Parsed q = Activities.parse("Ma 2 úszás volt: reggel "
                + "1000 m, este 1500 m.");
        assertEquals(2, q.plans.size());
        assertEquals(1.5, q.plans.get(1).km, 0.01);
        // A tiszta terv marad terv, a szakaszos úszás egy edzés.
        assertTrue(Activities.parse("Jövő héten maraton lesz.")
                .plans.isEmpty());
        assertEquals(1, Activities.parse("Úszóedzés: 400 m bemelegítés, "
                + "8x100 m gyors, 200 m levezetés. Összesen 1400 m, "
                + "45 perc.").plans.size());
    }

    /**
     * A tegnapi edzésre hivatkozás nem új bejegyzés.
     *
     * A „reggel 72-es pulzussal keltem, biztos a tegnapi edzés" egy
     * tegnapi, negyvenöt perces „egyéb mozgást" írt be – egy
     * pulzus-jegyzetből. Az „a tegnapi edzés után ma könnyű nap: 20 perc
     * séta" húsz perce pedig két napra terült szét.
     */
    @Test
    public void aReferenceToYesterdaysWorkoutIsNotAnEntry() {
        assertTrue(Activities.parse("Reggel 72-es pulzussal keltem, kicsit "
                + "magas, biztos a tegnapi edzés.").plans.isEmpty());
        Activities.Parsed p = Activities.parse("A tegnapi edzés után ma "
                + "könnyű nap: 20 perc séta.");
        assertEquals(1, p.days);
        assertEquals(0, p.offset);
        // A valódi tegnapi edzés marad tegnapi.
        assertEquals(1, Activities.parse("Tegnap edzettem, 45 perc "
                + "kondi.").offset);
    }

    /**
     * A megérkezett felszerelés nem edzés.
     *
     * A „ma reggel jött a futár az új súlyzókkal, 2x22,5 kg-osak"
     * hatvanperces kondi-bejegyzést írt be – egy csomagátvételből. A
     * kipróbálás viszont valódi edzés.
     */
    @Test
    public void deliveredEquipmentIsNotAWorkout() {
        assertTrue(Activities.parse("Ma reggel jött a futár az új "
                + "súlyzókkal, 2x22,5 kg-osak.").plans.isEmpty());
        assertTrue(Activities.parse("Megjött az új kettlebell, "
                + "16 kg-os.").plans.isEmpty());
        // A kipróbált eszköz edzés.
        assertEquals(1, Activities.parse("A Decathlonban vettem új "
                + "kettlebellt, ki is próbáltam: 3x15 swing.").plans.size());
    }

    /**
     * A gorgonzola sajt, nem görgőn tekerés.
     *
     * Az „ebédre gnocchi gorgonzolával" hatvanperces kerékpározást írt a
     * naplóba – a „görgőn" (edzőpad) töve a sajt nevébe esett.
     */
    @Test
    public void gorgonzolaIsNotABikeTrainer() {
        assertTrue(Activities.parse("Ebédre gnocchi gorgonzolával, nehéz "
                + "volt, de isteni.").plans.isEmpty());
        // A valódi görgőn tekerés marad.
        assertEquals("kerekpar", Activities.parse("90 perc a görgőn, "
                + "175 watt.").plans.get(0).kind.id);
    }

    /**
     * A nevezett táv és az égetés eszköze nem mai edzés.
     *
     * Az „a 10k-s versenyre neveztem, október 12-én lesz, ma 6 km
     * alapozás" TÍZ kilométeres futást is beírt a hat mellé. A „napi
     * mérleg: 1750 kcal evve, 320 elégetve futással" pedig negyvenöt
     * perces futást szült a kalóriasorból.
     */
    @Test
    public void aRegisteredRaceAndABurnInstrumentAreNotRuns() {
        Activities.Parsed p = Activities.parse("A 10k-s versenyre "
                + "neveztem, október 12-én lesz, ma 6 km alapozás.");
        assertEquals(1, p.plans.size());
        assertEquals(6.0, p.plans.get(0).km, 0.01);
        assertTrue(Activities.parse("Napi mérleg: 1750 kcal evve, 320 "
                + "elégetve futással.").plans.isEmpty());
        // A lefutott 10k marad futás.
        assertEquals(10.0, Activities.parse("Lefutottam a 10k-t.")
                .plans.get(0).km, 0.05);
    }

    /**
     * A salsa a quesadilla mellett szósz, a szakaszok pedig egy futás.
     *
     * A „vacsora: csirkés quesadilla házi salsával" hatvanperces TÁNCKÉNT
     * került a naplóba. A „tempófutás: 2 km bemelegítés, 5x1 km tempó,
     * 1 km levezetés" pedig három külön futás lett – a szakaszok távja
     * mostantól a fő edzéshez adódik.
     */
    @Test
    public void salsaSauceAndRunSegments() {
        assertTrue(Activities.parse("Vacsora: csirkés quesadilla házi "
                + "salsával.").plans.isEmpty());
        assertEquals("tanc", Activities.parse("Ma salsa óra volt, "
                + "60 perc.").plans.get(0).kind.id);
        Activities.Parsed p = Activities.parse("A futócsoporttal ma "
                + "tempófutás: 2 km bemelegítés, 5x1 km tempó, "
                + "1 km levezetés.");
        double sum = 0;
        for (Activities.Plan pl : p.plans) sum += pl.km;
        assertEquals(8.0, sum, 0.01);
        assertTrue(p.plans.size() <= 2);
    }

    /**
     * Az egyetlen hétvégi esemény egy nap.
     *
     * A „hétvégi meccsen 2 gólt lőttem, 60 percet játszottam" és a
     * „hétvégi túrán 22 km-t mentünk" KÉT napra terült szét, pedig egy
     * alkalomról szól – a jelzős alak utáni ragos főnév konkrét eseményt
     * nevez meg.
     */
    @Test
    public void aSingleWeekendEventIsOneDay() {
        assertEquals(1, Activities.parse("A hétvégi meccsen 2 gólt "
                + "lőttem, 60 percet játszottam.").days);
        assertEquals(1, Activities.parse("A hétvégi túrán 22 km-t "
                + "mentünk 1100 m szintemelkedéssel.").days);
        // A többnapos hétvégi túra marad kétnapos.
        assertEquals(2, Activities.parse("Hétvégén 2 napig túráztunk, "
                + "napi 20 km.").days);
    }

    /**
     * Az úszó sprint nem futás.
     *
     * A „ma reggel az uszodában 20x50 m-es sprinteket úsztam" egy
     * kilométer úszás MELLÉ egy negyvenöt perces futást is írt – a
     * sprint a futás szótöve is.
     */
    @Test
    public void aSwimSprintIsNotARun() {
        Activities.Parsed p = Activities.parse("Ma reggel az uszodában "
                + "20x50 m-es sprinteket úsztam, 15 mp pihenővel.");
        assertEquals(1, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
        // A pályás sprint marad futás.
        assertEquals("futas", Activities.parse("6x100 m sprint a pályán.")
                .plans.get(0).kind.id);
    }

    /**
     * A meghiúsult terv mellett a „csak 7 lett" a valóság.
     *
     * A „ma nem sikerült a terv szerinti 10 km, csak 7 lett, nagyon meleg
     * volt" bejegyzésből SEMMI nem lett: a ragozott „szerinti" miatt a
     * terv szava állva maradt, és az egész jövőnek minősült – pedig a hét
     * kilométer megvolt.
     */
    @Test
    public void theSevenThatDidHappenSurvivesTheFailedPlan() {
        Activities.Parsed p = Activities.parse("Ma nem sikerült a terv "
                + "szerinti 10 km, csak 7 lett, nagyon meleg volt.");
        assertEquals(1, p.plans.size());
        assertEquals(7.0, p.plans.get(0).km, 0.01);
        // A turmix hozzávalói sem duplázódnak „volt" közbevetéssel.
        // (Foods-oldali pár: aVenueNameIsNotADish mellett.)
    }

    /**
     * A „hétkor" óra, nem hét.
     *
     * A „háromnegyed hétkor keltem és lementem úszni 1000 métert" úszása
     * HÉT NAPRA terült szét, mert a „hétkor" ragozott hétnek látszott –
     * pedig csak azt mondja, hánykor kelt az ember.
     */
    @Test
    public void theClockSevenIsNotAWeek() {
        Activities.Parsed p = Activities.parse("Háromnegyed hétkor keltem "
                + "és lementem úszni 1000 métert.");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
        // A valódi heti összegzés marad hét nap.
        assertEquals(7, Activities.parse("A héten háromszor futottam, "
                + "összesen 25 km.").days);
    }

    /**
     * Az angol ugrálókötél is sport, a nagymamám tornája viszont nem az enyém.
     *
     * A „rossz idő volt, a futás helyett 45 perc jump rope a garázsban"
     * bejegyzésből SEMMI nem lett, a „nagymamám 85 évesen is mindennap
     * tornázik 20 percet" viszont húsz perc jógát írt a MI naplónkba –
     * a naGYMama közepén ülő gym miatt az alany a maszkon eltűnt.
     */
    @Test
    public void jumpRopeCountsAndGrandmasWorkoutDoesNot() {
        Activities.Parsed p = Activities.parse("Rossz idő volt, a futás "
                + "helyett 45 perc jump rope a garázsban.");
        assertEquals(1, p.plans.size());
        assertEquals(45, p.plans.get(0).minutes);
        assertTrue(Activities.parse("Nagymamám 85 évesen is mindennap "
                + "tornázik 20 percet, tőle tanulok.").plans.isEmpty());
        // A közös séta az enyém is.
        assertEquals(1, Activities.parse("Nagymamámmal sétáltam egy órát "
                + "a parkban.").plans.size());
    }

    /**
     * A „sőt" utáni szám szóközös ezressel és beékelt cél-szóval is nyer.
     *
     * A „végre elértem a napi 10 000 lépéses célt, sőt 12 431 lett!"
     * naplójába a tízezres CÉL került a valódi tizenkétezer helyett –
     * a „célt" szava megtörte a sőt-mintát.
     */
    @Test
    public void theRealStepCountBeatsTheGoalEvenWithAWordBetween() {
        Activities.Parsed p = Activities.parse("Végre elértem a napi "
                + "10 000 lépéses célt, sőt 12 431 lett!");
        assertEquals(1, p.plans.size());
        assertEquals(12431, p.plans.get(0).steps);
    }

    /**
     * A maradék tegnapról való, az evés mai; a plank perce nem az edzésé.
     *
     * Az „az ebédem maradék lasagne volt tegnapról" bejegyzése TEGNAPRA
     * került, az „otthoni edzés: … plank 3x1 perc" pedig EGYPERCES
     * kondiedzést írt be – a szám előtt álló plank ideje lett az egészé.
     */
    @Test
    public void leftoversDateTodayAndAPlankMinuteIsNotTheSession() {
        assertEquals(0, Activities.parse("Az ebédem maradék lasagne volt "
                + "tegnapról, kb 400 gramm.").offset);
        Activities.Parsed p = Activities.parse("Otthoni edzés: 4x15 kitörés, "
                + "3x20 vállemelés 2x5 kg-os kézisúlyzóval, plank 3x1 perc.");
        assertEquals(1, p.plans.size());
        assertTrue(p.plans.get(0).minutes >= 30);
        // A kimondott edzés-idő marad, a tegnapi futás is tegnapi marad.
        assertEquals(45, Activities.parse("Kondi 45 perc, plank 3x45 mp "
                + "a végén.").plans.get(0).minutes);
        assertEquals(1, Activities.parse("Tegnap futottam 10 km-t.").offset);
    }

    /**
     * A nevezés melletti mai kilométer valóság, a dupla nap pedig két futás.
     *
     * A „beneveztünk egy 10 km-es jótékonysági futásra októberben,
     * elkezdtünk készülni: ma 4 km" bejegyzéséből SEMMI nem lett. A
     * „reggel 6-kor 5 km, este 6-kor még 5 km, dupla nap volt" estéje
     * pedig a reggeli ismétlésének látszott, és némán elveszett.
     */
    @Test
    public void aRegistrationBesideTodaysRunAndADoubleDay() {
        Activities.Parsed p = Activities.parse("Beneveztünk a párommal egy "
                + "10 km-es jótékonysági futásra októberben, elkezdtünk "
                + "készülni: ma 4 km.");
        assertEquals(1, p.plans.size());
        assertEquals(4.0, p.plans.get(0).km, 0.01);
        Activities.Parsed d = Activities.parse("Reggel 6-kor 5 km, este "
                + "6-kor még 5 km, dupla nap volt.");
        assertEquals(2, d.plans.size());
        assertEquals(5.0, d.plans.get(1).km, 0.01);
        // A puszta nevezés terv marad, az ismétlés-szó nélküli azonos táv
        // pedig óvatosságból egy alkalom.
        assertTrue(Activities.parse("Beneveztem egy félmaratonra.")
                .plans.isEmpty());
        assertEquals(1, Activities.parse("reggel 5 km futás, este 5 km "
                + "futás").plans.size());
    }

    /**
     * A jövő heti sítúra és a megírt terv nem mai edzés, a mai nyújtás az.
     *
     * A „szombaton sítúrára megyünk, ma bepakoltam és 30 perc nyújtást
     * csináltam" kétórás MAI síelést írt be, a „futóedzőm új tervet írt:
     * heti 3 futás" pedig három egyéb mozgást – papírra írt tervekből. A
     * „hétfőtől új életmód… ma el is kezdtem, 40 perc" sétája pedig
     * hétfőre csúszott, pedig ma volt.
     */
    @Test
    public void plansOnPaperAreNotTodaysWorkouts() {
        Activities.Parsed p = Activities.parse("Szombaton sítúrára megyünk, "
                + "ma bepakoltam és 30 perc nyújtást csináltam.");
        boolean si = false;
        for (Activities.Plan x : p.plans) if (x.kind.id.equals("si")) si = true;
        assertFalse(si);
        boolean nyujtas = false;
        for (Activities.Plan x : p.plans)
            if (x.kind.id.equals("joga") && x.minutes == 30) nyujtas = true;
        assertTrue(nyujtas);
        assertTrue(Activities.parse("A futóedzőm új tervet írt: heti 3 "
                + "futás, kedd intervall, csütörtök tempó, vasárnap hosszú.")
                .plans.isEmpty());
        Activities.Parsed e = Activities.parse("Hétfőtől új életmód: "
                + "kevesebb cukor, több zöldség, napi séta. Ma el is "
                + "kezdtem, 40 perc.");
        assertEquals(0, e.offset);
        assertEquals(40, e.plans.get(0).minutes);
        // A megtörtént sítúra marad.
        assertEquals(240, Activities.parse("Sítúrára mentünk szombaton, "
                + "4 óra síelés volt.").plans.get(0).minutes);
    }

    /**
     * A hitetlenkedés kerete nem terv, az „ebből" futás a lépések része.
     *
     * A „sose gondoltam volna, hogy 10 km-t tudok futni, ma megtörtént!"
     * bejegyzéséből SEMMI nem lett, a „ma 12 500 lépés, ebből 5 km futás"
     * túrája pedig a TELJES lépésszámot kapta a futás mellé – a nap fele
     * kétszer számolt.
     */
    @Test
    public void disbeliefAndPartOfStepsAreSortedOut() {
        Activities.Parsed p = Activities.parse("Sose gondoltam volna, hogy "
                + "10 km-t tudok futni, ma megtörtént!");
        assertEquals(1, p.plans.size());
        assertEquals(10.0, p.plans.get(0).km, 0.01);
        Activities.Parsed st = Activities.parse("Ma 12 500 lépés, ebből "
                + "5 km futás volt reggel.");
        assertEquals(2, st.plans.size());
        double walkKm = 0;
        for (Activities.Plan x : st.plans)
            if (x.kind.id.equals("tura")) walkKm = x.km;
        assertEquals(4.4, walkKm, 0.05);
        // A puszta vágy marad terv, az „és" melletti lépés teljes marad.
        assertTrue(Activities.parse("Sose gondoltam volna, hogy 10 km-t "
                + "tudok futni.").plans.isEmpty());
        Activities.Parsed both = Activities.parse("Ma 11 000 lépés és "
                + "5 km futás.");
        double walk2 = 0;
        for (Activities.Plan x : both.plans)
            if (x.kind.id.equals("tura")) walk2 = x.km;
        assertEquals(8.3, walk2, 0.05);
    }

    /**
     * Az eltiltás nem edzés, a bontás perce nem az edzés hossza.
     *
     * Az „az orvos eltiltott a futástól 2 hétre, addig csak úszhatok"
     * TIZENNÉGY napos futást írt a naplóba – pont abból, amitől az orvos
     * eltiltott. A „3 percet plankoltam összesen, 3x1 perc bontásban"
     * pedig egyperces kondiedzés lett.
     */
    @Test
    public void aDoctorsBanAndABreakdownMinuteAreNotTheSession() {
        assertTrue(Activities.parse("Az orvos eltiltott a futástól 2 "
                + "hétre, addig csak úszhatok.").plans.isEmpty());
        // A tiltás melletti valódi úszás marad.
        assertEquals("uszas", Activities.parse("Az orvos eltiltott a "
                + "futástól, ezért ma úsztam 1500 métert.")
                .plans.get(0).kind.id);
        assertTrue(Activities.parse("3 percet plankoltam összesen, "
                + "3x1 perc bontásban.").plans.get(0).minutes >= 30);
        // A kimondott fő idő marad a bontás mellett.
        assertEquals(40, Activities.parse("Futottam 40 percet, 2x20 perc "
                + "bontásban.").plans.get(0).minutes);
    }

    /**
     * A reggeli elhatározás estére valóra vált, a hasonlítás órája nem idő.
     *
     * A „reggel eldöntöttem, hogy este futni fogok, és tényleg: 6 km
     * lett!" bejegyzéséből SEMMI nem lett. A „vibrációs tréningen voltam
     * 25 percet, állítólag felér egy órás edzéssel" pedig HATVAN perces
     * lett a huszonöt helyett.
     */
    @Test
    public void aResolutionComeTrueAndAComparisonHour() {
        Activities.Parsed p = Activities.parse("Reggel eldöntöttem, hogy "
                + "este futni fogok, és tényleg: 6 km lett!");
        assertEquals(1, p.plans.size());
        assertEquals(6.0, p.plans.get(0).km, 0.01);
        assertEquals(25, Activities.parse("A vibrációs tréningen voltam "
                + "25 percet, állítólag felér egy órás edzéssel.")
                .plans.get(0).minutes);
        // A beteljesületlen terv terv marad.
        assertTrue(Activities.parse("Eldöntöttem, hogy holnap úszni "
                + "fogok.").plans.isEmpty());
        assertTrue(Activities.parse("Úszni fogok este, és tényleg jó "
                + "lesz.").plans.isEmpty());
        // Az autogén tréning marad a jóga-lapon.
        assertEquals("joga", Activities.parse("Autogén tréning 20 perc.")
                .plans.get(0).kind.id);
    }

    /**
     * A csak-úszós mondat gazdátlan métere úszás, nem futás.
     *
     * A „a nyári táborban a gyerekek napi 3x úsznak, én is beszálltam ma
     * délután 1000 méterre" ezer métere FUTÁS lett hat perccel – a
     * gyerekek tagmondatát a más-ember szűrő kitakarta, és a táv sport
     * nélkül maradt.
     */
    @Test
    public void anOrphanDistanceInASwimOnlySentenceIsASwim() {
        Activities.Parsed p = Activities.parse("A nyári táborban a "
                + "gyerekek napi 3x úsznak, én is beszálltam ma délután "
                + "1000 méterre.");
        assertEquals(1, p.plans.size());
        assertEquals("uszas", p.plans.get(0).kind.id);
        assertEquals(1.0, p.plans.get(0).km, 0.01);
        // A futós mondat gazdátlan távja marad futás.
        assertEquals("futas", Activities.parse("Nyomtam egy 5 km-t.")
                .plans.get(0).kind.id);
    }

    /**
     * A perc-lista egysége öröklődik, a wing chun harcművészet.
     *
     * A „kutyát sétáltattam háromszor: reggel 20, délben 10, este 30 perc"
     * mindhárom alkalma HARMINC percet kapott – kilencven perc a valódi
     * hatvan helyett. A „wing chun edzés 90 perc" pedig egyéb mozgás volt.
     */
    @Test
    public void aMinuteListSharesItsUnit() {
        Activities.Parsed p = Activities.parse("A kutyát sétáltattam "
                + "háromszor: reggel 20, délben 10, este 30 perc.");
        assertEquals(3, p.plans.size());
        int total = 0;
        for (Activities.Plan x : p.plans) total += x.minutes;
        assertEquals(60, total);
        assertEquals("harcmuveszet", Activities.parse("Wing chun edzés "
                + "90 perc, páros gyakorlatok.").plans.get(0).kind.id);
        // Az igés lista is örököl.
        assertEquals(20, Activities.parse("Futottam 20, úsztam 30 percet.")
                .plans.get(0).minutes);
    }

    /**
     * A munkahelyi lépés nem külön edzés, a matrac-avatás flow-ja jóga.
     *
     * A „ma nem edzettem, de a fizikai munkám miatt így is 15 000 lépésem
     * lett" mellé egy órás fizikai-munka bejegyzés került a lépések MELLÉ.
     * A „kipróbáltam az új jógamatracot: 30 perc flow" pedig üres maradt,
     * a „talpaltam a városban, kb 7 km" meg futás lett.
     */
    @Test
    public void workStepsFlowAndCityWalking() {
        Activities.Parsed p = Activities.parse("Ma nem edzettem, de a "
                + "fizikai munkám miatt így is 15 000 lépésem lett.");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        assertEquals("joga", Activities.parse("Kipróbáltam az új "
                + "jógamatracot: 30 perc flow.").plans.get(0).kind.id);
        assertEquals("tura", Activities.parse("Talpaltam a városban "
                + "egész délelőtt, kb 7 km.").plans.get(0).kind.id);
        // A matrac-vásárlás önmagában nem jóga.
        assertTrue(Activities.parse("Vettem egy új jógamatracot.")
                .plans.isEmpty());
    }

    /**
     * Az izomláz panasza nem viheti el a meccset, a fél óra is óra.
     *
     * Az „a meccs után izomlázam lett a combomban, pedig csak 60 percet
     * játszottam" hatvan perce nyomtalanul eltűnt – a meccs a kitakart
     * panasz-tagmondatban ült. A „fél 7-től fél 8-ig úszás" pedig
     * negyvenöt perc lett a hatvan helyett.
     */
    @Test
    public void soreMusclesAndHalfPastHours() {
        Activities.Parsed p = Activities.parse("A meccs után izomlázam "
                + "lett a combomban, pedig csak 60 percet játszottam.");
        assertEquals(1, p.plans.size());
        assertEquals(60, p.plans.get(0).minutes);
        assertEquals(60, Activities.parse("Fél 7-től fél 8-ig úszás, "
                + "utána munka.").plans.get(0).minutes);
        // A tegnapi edzés izomláza továbbra sem új bejegyzés.
        assertTrue(Activities.parse("Izomláz van rendesen a tegnapi "
                + "lábnaptól.").plans.isEmpty());
        // A maraton-terv papír, nem mai táv; a lefutott félmaraton marad.
        assertTrue(Activities.parse("Végigcsináltam a 12 hetes "
                + "félmaraton-tervet, vasárnap lesz a verseny!")
                .plans.isEmpty());
        assertEquals(21.1, Activities.parse("Lefutottam a félmaratont, "
                + "1:58 lett.").plans.get(0).km, 0.01);
    }

    /**
     * A kimondott medencehossz, a wattbike és a mondatvégi „kész".
     *
     * A „45 hossz a 33-as medencében" 1125 méter lett a valódi 1485
     * helyett, a „wattbike-on 20 perc FTP teszt" üresen jött vissza, a
     * „ma nem fog: 8 km futás kész" nyolc kilométere pedig elveszett.
     */
    @Test
    public void poolSizeWattbikeAndADoneMarker() {
        assertEquals(1.485, Activities.parse("45 hossz a 33-as "
                + "medencében.").plans.get(0).km, 0.001);
        assertEquals("kerekpar", Activities.parse("A wattbike-on 20 perc "
                + "FTP teszt, 265 watt lett.").plans.get(0).kind.id);
        Activities.Parsed p = Activities.parse("A párom szerint horkolok, "
                + "ha kimarad az edzés. Ma nem fog: 8 km futás kész.");
        assertEquals(1, p.plans.size());
        assertEquals(8.0, p.plans.get(0).km, 0.01);
        // A kimerültség „kész vagyok"-ja nem siker-jel.
        assertTrue(Activities.parse("Nem fog menni a futás, kész vagyok "
                + "teljesen.").plans.isEmpty());
    }

    /**
     * A lejátszott meccs igéje nem külön mozgás a sportág mellett.
     *
     * Az „a jégkorong meccsen 3 harmadot végigjátszottam, kb 25 perc
     * jégidő" korcsolyája mellé egy egyéb mozgás is került – ugyanarról
     * az egy meccsről, dupla idővel.
     */
    @Test
    public void aPlayedMatchIsNotASecondWorkout() {
        Activities.Parsed p = Activities.parse("A jégkorong meccsen 3 "
                + "harmadot végigjátszottam, kb 25 perc jégidő.");
        assertEquals(1, p.plans.size());
        assertEquals("korcsolya", p.plans.get(0).kind.id);
        // A másik tagmondat meccse külön alkalom marad.
        assertEquals(2, Activities.parse("Reggel futás 5 km, este "
                + "meccset játszottam 60 percet.").plans.size());
    }

    /**
     * A mondat eleji óra időpont, a 3x10 perc séta három alkalom.
     *
     * A „18:30 CrossFit WOD: 21-15-9 thruster, 12:40 alatt" HARMINC
     * alkalmas, harminc napos kondi lett – a perc fele levált óraszámnak.
     * A „rövid séták voltak: 3x10 perc" pedig egyetlen tízperces sétává
     * olvadt. A nevezés melletti „ma megvolt" is valóság.
     */
    @Test
    public void aLeadingClockAndWalkRepeats() {
        Activities.Parsed p = Activities.parse("18:30 CrossFit WOD: "
                + "21-15-9 thruster és húzódzkodás, 12:40 alatt.");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(1, p.plans.get(0).count);
        assertEquals(18, p.hour);
        assertEquals(3, Activities.parse("A kutyánk megbetegedett, ezért "
                + "ma csak rövid séták voltak: 3x10 perc.")
                .plans.get(0).count);
        assertEquals(5.0, Activities.parse("Beneveztem a szeptemberi 10 "
                + "km-re. Ma megvolt az első edzés: 5 km könnyű.")
                .plans.get(0).km, 0.01);
    }

    /**
     * A bringatúra -ra vége nem rag, a napüdvözlet nem napok, a jelen
     * idejű kihagyás is kihagyás, a vízisí pedig sí.
     *
     * A „vasárnapi bringatúra: 35 km" túrája eltűnt (a szó vége
     * célhatározónak látszott), a „108 napüdvözlet" száznyolc NAPOS
     * bejegyzés lett, a „kihagyom a mai úszást" negyvenöt perc úszást írt
     * be, a „vízisí tábor, 45 perc vízen" pedig üres volt.
     */
    @Test
    public void bikeTourSunSalutationsSkipAndWaterSki() {
        assertEquals("kerekpar", Activities.parse("Vasárnapi bringatúra a "
                + "családdal: 35 km.").plans.get(0).kind.id);
        assertEquals(1, Activities.parse("Reggel meditáció 10 perc, "
                + "utána 108 napüdvözlet.").days);
        assertTrue(Activities.parse("Fáj a torkom, lázas is vagyok, "
                + "kihagyom a mai úszást.").plans.isEmpty());
        assertEquals("si", Activities.parse("Vízisí tábor 5. napja: ma "
                + "3 futam, összesen 45 perc vízen.").plans.get(0).kind.id);
        // A jövő heti versenyre készülés lazítása marad mai futás.
        assertEquals(5.0, Activities.parse("A jövő heti maratonra ma már "
                + "csak 5 km lazítás volt.").plans.get(0).km, 0.01);
    }

    /**
     * A próbálkozás szorzója nem alkalomszám, a softball is sport.
     *
     * A „wakeboardoztunk, én kétszer tudtam felállni" KÉT edzést írt be,
     * a „softballoztunk a csapatépítésen másfél órát" pedig üres volt.
     */
    @Test
    public void standUpAttemptsAndSoftball() {
        assertEquals(1, Activities.parse("A tesómékkel wakeboardoztunk a "
                + "tavon, én kétszer tudtam felállni.").plans.get(0).count);
        assertEquals(90, Activities.parse("Softballoztunk a "
                + "csapatépítésen vagy másfél órát.").plans.get(0).minutes);
        // A valódi két úszás marad kettő.
        assertEquals(2, Activities.parse("Kétszer úsztam ma, reggel és "
                + "este.").plans.get(0).count);
    }

    /**
     * A heti rend leírása nem a bejegyzés időszaka.
     *
     * Az „úszásoktatásra iratkoztam be, heti 1x60 perc, ma volt az első"
     * és az „a heti bevásárlás is 5000 lépés volt" hét napra terült szét;
     * a szüneteltetett futás pedig bejegyzés lett, az orbitrek meg semmi.
     */
    @Test
    public void aWeeklyScheduleIsNotTheEntrySpan() {
        assertEquals(1, Activities.parse("Úszásoktatásra irattam be "
                + "magam, heti 1x60 perc, ma volt az első.").days);
        assertEquals(1, Activities.parse("A heti bevásárlás is 5000 "
                + "lépés volt a plázában.").days);
        Activities.Parsed p = Activities.parse("Csípőfájdalom miatt a "
                + "futást szüneteltetem, helyette túrázom hétvégén.");
        for (Activities.Plan x : p.plans)
            assertFalse(x.kind.id.equals("futas"));
        assertEquals(45, Activities.parse("440 kcal-t égetett a 45 "
                + "perces orbitrek az óra szerint.").plans.get(0).minutes);
        // A valódi heti gyakoriság marad kibontva.
        assertEquals(30, Activities.parse("Heti 3 futás volt a múlt "
                + "hónapban.").days);
    }

    /**
     * A „terhesség alatt is" jelen, a szteppad sport.
     *
     * A „terhesség alatt is mozogtam: ma 30 perc kismama torna" tornája
     * eltűnt (a visszaemlékezés-szabály a mai edzést is elvitte), a
     * „szteppadon 30 perc" pedig üres volt – az sz-es írásmód hiányzott.
     */
    @Test
    public void pregnancyPresentAndStepBoard() {
        Activities.Parsed p = Activities.parse("Terhesség alatt is "
                + "mozogtam: ma 30 perc kismama torna.");
        assertEquals(1, p.plans.size());
        assertEquals(30, p.plans.get(0).minutes);
        assertEquals(30, Activities.parse("A szteppadon 30 perc, "
                + "föllépésekkel és oldallépésekkel.").plans.get(0).minutes);
        // A visszaemlékezés „is" nélkül marad üres.
        assertTrue(Activities.parse("Terhesség alatt jógáztam sokat.")
                .plans.isEmpty());
        // A KÖNNYEBB MOZGÁS közérzet, nem edzés; a valódi mozgás marad.
        assertTrue(Activities.parse("A csontkovácsnál voltam, utána "
                + "könnyebb lett a mozgás.").plans.isEmpty());
        assertEquals(30, Activities.parse("30 perc mozgás volt ma.")
                .plans.get(0).minutes);
    }

    /**
     * A kihívás neve, a verseny szava és a kiindulópont nem külön edzés.
     *
     * A „fitnesz kihívás 3. napja: 30 burpee…" és a „a játszótérről
     * hazafelé versenyt futottunk" egy-egy plusz egyéb mozgást kapott, a
     * „hétvégi túlórás munka miatt csak ma jutottam el futni" futása
     * pedig a hétvégére került.
     */
    @Test
    public void aChallengeNameARaceWordAndAStartingPoint() {
        assertEquals(1, Activities.parse("A fitnesz kihívás 3. napja: 30 "
                + "burpee, 30 mountain climber, 30 jumping jack.")
                .plans.size());
        assertEquals(1, Activities.parse("A két gyerekkel a játszótérről "
                + "hazafelé versenyt futottunk, én nyertem!").plans.size());
        Activities.Parsed p = Activities.parse("A hétvégi túlórás munka "
                + "miatt csak ma jutottam el futni: 6,5 km.");
        assertEquals(0, p.offset);
        assertEquals(1, p.days);
        // A valódi hétvégi futás marad hétvégi (rögzített szerdai nappal,
        // hogy a teszt ne függjön a futtatás napjától).
        assertTrue(Activities.parse("Hétvégén futottam 10 km-t.",
                1_753_869_600_000L).offset > 0);
    }

    /**
     * A húszkilométeres úszás nem hihető, a köztes séta nem külön túra.
     *
     * A „heti mérleg: 3 edzés, 21 km, 2x úszás" huszonegy kilométere az
     * úszásra tapadt, a „sprintek, köztük séta lefelé" mellé pedig egy
     * kilencven perces gyaloglás került.
     */
    @Test
    public void anImplausibleSwimAndARecoveryWalk() {
        Activities.Parsed p = Activities.parse("Heti mérleg: 3 edzés, "
                + "21 km, 2x úszás, 1 pihenőnap.");
        for (Activities.Plan x : p.plans)
            if (x.kind.id.equals("uszas")) assertEquals(0.0, x.km, 0.01);
        boolean run21 = false;
        for (Activities.Plan x : p.plans)
            if (x.kind.id.equals("futas") && Math.abs(x.km - 21) < 0.01)
                run21 = true;
        assertTrue(run21);
        assertEquals(1, Activities.parse("90 másodperces sprintek a "
                + "dombon, 6 ismétlés, köztük séta lefelé.").plans.size());
        // A Balaton-átúszás és a kimondott hosszú edzőtáv marad úszás.
        assertEquals(5.2, Activities.parse("Átúsztam a Balatont, 5,2 km "
                + "2:40 alatt.").plans.get(0).km, 0.01);
        assertEquals(10.0, Activities.parse("Nyílt vízi úszás 10 km, "
                + "3 óra.").plans.get(0).km, 0.01);
    }

    /**
     * A pálya hossza nem a lefutott táv, a felesben ivott turmix fél.
     *
     * A „400 m-es futópályán 10 kör bemelegítés után 5x1000 m" négyszáz
     * méteres futás lett az ötezer helyett; az „ittunk egy protein
     * shake-et felesben" egész adagként ment be.
     */
    @Test
    public void aTrackLengthAndASharedShake() {
        assertEquals(5.0, Activities.parse("400 m-es futópályán 10 kör "
                + "bemelegítés után 5x1000 m.").plans.get(0).km, 0.01);
        // A kör-szorzás pályahossza marad.
        assertEquals(4.0, Activities.parse("10 kör a 400 m-es pályán")
                .plans.get(0).km, 0.01);
        assertEquals(150.0, Foods.parse(java.util.Arrays.asList(Foods.ALL),
                "Ittunk egy protein shake-et felesben a párommal.")
                .get(0).grams, 0.01);
    }

    /**
     * A szokás napjai és a jövő heti esemény nem a bejegyzés napjai.
     *
     * Az „úszni járok már egy hónapja, hétfőn és csütörtökön, ma 1800 m
     * ment" ötnapos, két alkalmas bejegyzés lett, az „a szombati hosszú
     * túra előtt ma csak lazítás" mellé pedig egy szombatra keltezett
     * túra került – a tervből.
     */
    @Test
    public void habitDaysAndAFutureEventStayOut() {
        Activities.Parsed p = Activities.parse("Úszni járok már egy "
                + "hónapja, hétfőn és csütörtökön, ma 1800 m ment.");
        assertEquals(1, p.days);
        assertEquals(1, p.plans.size());
        assertEquals(1.8, p.plans.get(0).km, 0.01);
        Activities.Parsed l = Activities.parse("A szombati hosszú túra "
                + "előtt ma csak lazítás: 20 perc görgőzés.");
        assertEquals(1, l.plans.size());
        assertEquals(20, l.plans.get(0).minutes);
        // A hétfői igés tagmondat valódi úszása marad.
        assertEquals(2, Activities.parse("Hétfőn úsztam, ma 5 km futás.")
                .plans.size());
    }

    /**
     * A tagadás elé vetett igenév, a lépésszám és a tegnapi maradék.
     *
     * Az „úszni nem úsztam" negyvenöt perc úszást írt be a fürdőzésből,
     * az „a lépésszám 13 450" sétája elveszett, az „a tegnapi lecsó
     * maradékát melegítettem meg" ebédje pedig tegnapra került.
     */
    @Test
    public void aFrontedNegationStepCountAndLeftovers() {
        assertTrue(Activities.parse("A széchenyi fürdőben áztattuk "
                + "magunkat 2 órát, úszni nem úsztam.").plans.isEmpty());
        Activities.Parsed st = Activities.parse("Az okosóra szerint a mai "
                + "aktív kalóriám 612, a lépésszám 13 450.");
        assertEquals(1, st.plans.size());
        assertEquals(13450, st.plans.get(0).steps);
        assertEquals(0, Activities.parse("Reggelire rántotta, ebédre a "
                + "tegnapi lecsó maradékát melegítettem meg.").offset);
        // A fizioterápia gyógytorna-bejegyzés.
        assertEquals("joga", Activities.parse("A fizioterápiás "
                + "gyakorlatokat naponta 2x15 percben írták elő, ma "
                + "mindkettőt megcsináltam.").plans.get(0).kind.id);
    }

    /**
     * A webinar órája, a szállodai gym és az átmozgatás bevezetője.
     *
     * A „fél órás webinar után átmozgattam magam: 15 guggolás, 15
     * fekvőtámasz" harminc perc jógát írt be a webinar idejéből, a
     * „szállodában gym is volt, 40 perc futópad" futása mellé pedig egy
     * órás kondi került a megjegyzésből.
     */
    @Test
    public void aWebinarHourAndAHotelGymNote() {
        Activities.Parsed p = Activities.parse("Fél órás webinar után "
                + "átmozgattam magam: 15 guggolás, 15 fekvőtámasz.");
        assertEquals(1, p.plans.size());
        assertEquals("kondi", p.plans.get(0).kind.id);
        Activities.Parsed g = Activities.parse("A luxus szállodában gym "
                + "is volt, 40 perc futópad reggeli előtt.");
        assertEquals(1, g.plans.size());
        assertEquals(40, g.plans.get(0).minutes);
        // A fél órás futás marad harminc perc.
        assertEquals(30, Activities.parse("Fél órás futás volt ma.")
                .plans.get(0).minutes);
    }

    /**
     * Az éjjeli óra hajnali, a gépen nézett film alatt is teker az ember.
     *
     * Az „éjjel 2-kor keltem fel" kettője délután kettőre tolódott, az
     * „úszogatás a gyerekkel: ő a gumimatracon, én 20 hosszt" úszása
     * eltűnt, a „szobabiciklin néztem egy sorozatot, 50 perc lett" ötven
     * perce pedig a nézés szaván bukott el.
     */
    @Test
    public void nightHoursSharedPoolsAndCyclingWhileWatching() {
        assertEquals(2, Activities.parse("Éjjel 2-kor keltem fel, "
                + "hajnalban futottam 5 km-t.").hour);
        assertEquals(0.5, Activities.parse("Úszogatás a gyerekkel: ő a "
                + "gumimatracon, én 20 hosszt tempóban.")
                .plans.get(0).km, 0.01);
        assertEquals(50, Activities.parse("A szobabiciklin néztem egy "
                + "sorozatot, észre sem vettem, hogy 50 perc lett.")
                .plans.get(0).minutes);
        // Az este nyolc marad húsz óra, a tévézett meccs marad semmi.
        assertEquals(20, Activities.parse("Este 8-kor futottam.").hour);
        assertTrue(Activities.parse("Néztem a meccset a tv-ben.")
                .plans.isEmpty());
    }

    /**
     * A messzebb álló lépcső, a beálló játékideje és a labdázós séta.
     *
     * Az „a lift helyett mindig lépcső: ma 14 emelet összesen" üresen
     * jött vissza, a „90 perces meccs, …, kb 45 perc játék" kilencven
     * percet írt be, a „dobáltam a labdát, közben sétálgattam" sétáját
     * pedig a pihenő-maszk vitte el.
     */
    @Test
    public void distantStairsASubstituteAndABallWalk() {
        assertEquals(1, Activities.parse("A lift helyett mindig lépcső: "
                + "ma 14 emelet összesen.").plans.size());
        assertEquals(45, Activities.parse("90 perces meccs, én a második "
                + "félidőben álltam be, kb 45 perc játék.")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("Dobáltam a labdát fél óráig, "
                + "közben sétálgattam.").plans.get(0).minutes);
        // A végigjátszott meccs teljes hossza marad.
        assertEquals(90, Activities.parse("90 perces meccset játszottunk "
                + "végig.").plans.get(0).minutes);
        // Az edzés kihagyva nem edzés.
        assertTrue(Activities.parse("Tea és méz, fáj a torkom, edzés "
                + "kihagyva.").plans.isEmpty());
    }

    /**
     * A „-ból lett" átalakulás és a bérlet melletti első alkalom.
     *
     * A „a viharos szél miatt a bringázásból séta lett" sétája mellé egy
     * órás kerékpározás került, a „bérlest vettem a jógába, heti 2 óra,
     * ma volt az első alkalom" pedig üresen jött vissza.
     */
    @Test
    public void aTransformationAndAFirstYogaClass() {
        Activities.Parsed p = Activities.parse("A viharos szél miatt a "
                + "bringázásból séta lett, 5 km a gáton.");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        Activities.Parsed j = Activities.parse("Bérlest vettem a jógába, "
                + "heti 2 óra, ma volt az első alkalom.");
        assertEquals(1, j.plans.size());
        assertEquals("joga", j.plans.get(0).kind.id);
        // A puszta bérletvásárlás marad üres.
        assertTrue(Activities.parse("Bérletet vettem a konditerembe.")
                .plans.isEmpty());
    }

    /**
     * A szomszédasszony futása, a terv hete és a néző lépései.
     *
     * A „szomszédasszony 5 km-t futott, én 2 km-t sétáltam" öt
     * kilométere is bekerült, az „edzéstervem 2. hete: ma könnyű 6 km"
     * üresen jött vissza, a „csak néztük, de 8000 lépés lett" lépéseit
     * pedig a néző-szabály nyelte el a meccsel együtt.
     */
    @Test
    public void aNeighborsRunAPlanWeekAndASpectatorsSteps() {
        Activities.Parsed n = Activities.parse("A szomszédasszony 5 km-t "
                + "futott ma, én csak 2 km-t sétáltam.");
        assertEquals(1, n.plans.size());
        assertEquals("tura", n.plans.get(0).kind.id);
        assertEquals(2.0, n.plans.get(0).km, 0.01);
        Activities.Parsed t = Activities.parse("A tavaszi félmaraton "
                + "edzéstervem 2. hete: ma könnyű 6 km, holnap pihenő.");
        assertEquals(1, t.plans.size());
        assertEquals(6.0, t.plans.get(0).km, 0.01);
        Activities.Parsed s = Activities.parse("Focimeccsen voltunk, csak "
                + "néztük, de 8000 lépés lett a járkálásból.");
        assertEquals(1, s.plans.size());
        assertEquals(8000, s.plans.get(0).steps);
        // A lépés nélküli néző marad néző.
        assertTrue(Activities.parse("Focimeccsen voltunk, csak néztük.")
                .plans.isEmpty());
    }

    /**
     * A szakaszok közti kocogás táva a pihenő, nem a futás.
     *
     * A „8x200 m intervall, 200 m kocogással köztük" kétszáz méteres
     * futást írt be – az ezerhatszáz helyett.
     */
    @Test
    public void recoveryJogDistanceIsNotTheRun() {
        assertEquals(1.6, Activities.parse("8x200 m intervall 200 m "
                + "kocogással köztük.").plans.get(0).km, 0.01);
        // A sorozat nélküli kocogás táva marad az övé.
        assertEquals(2.0, Activities.parse("2 km kocogás a parkban.")
                .plans.get(0).km, 0.01);
    }

    /**
     * A lement hossz, a kutyás körök és az óra helyesbített lépése.
     *
     * A „lementem 30 hosszt a másik sávban" testsúly-mérés lett, a
     * „kutyával a szokásos köröket róttuk, majdnem 4 km" futásként
     * ment be, a „telefonom szerint 9800 lépés, de az órám 10 400-at
     * írt" pedig a telefon számát vette.
     */
    @Test
    public void swimLapsDogLapsAndACorrectedStepCount() {
        Activities.Parsed u = Activities.parse("Az úszásoktatásra "
                + "kísértem a gyereket, közben lementem 30 hosszt a "
                + "másik sávban.");
        assertEquals(1, u.plans.size());
        assertEquals("uszas", u.plans.get(0).kind.id);
        assertEquals(0.75, u.plans.get(0).km, 0.01);
        Activities.Parsed k = Activities.parse("A kutyával a szokásos "
                + "köröket róttuk, majdnem 4 km lett.");
        assertEquals("tura", k.plans.get(0).kind.id);
        assertEquals(4.0, k.plans.get(0).km, 0.01);
        assertEquals(10400, Activities.parse("A telefonom szerint 9800 "
                + "lépés, de az órám 10 400-at írt, az órának hiszek.")
                .plans.get(0).steps);
        // A futva rótt kör marad futás.
        assertEquals("futas", Activities.parse("Futva róttuk a köröket "
                + "a kutyával, 5 km lett.").plans.get(0).kind.id);
    }

    /**
     * Az azonos mozgás második ideje hozzáadódik.
     *
     * A „reggel 6-kor jógáztam 20 percet, este még 20-at" estéje némán
     * elveszett – a nap fele kimaradt a naplóból.
     */
    @Test
    public void aSecondRoundOfTheSameSportAddsItsMinutes() {
        Activities.Parsed j = Activities.parse("Reggel 6-kor jógáztam "
                + "20 percet, este még 20-at.");
        assertEquals(1, j.plans.size());
        assertEquals(40, j.plans.get(0).minutes);
        // A másik sport perce a másik sporté marad.
        Activities.Parsed f = Activities.parse("Futottam 30 percet, "
                + "utána még 15 perc nyújtás.");
        assertEquals(2, f.plans.size());
        assertEquals(30, f.plans.get(0).minutes);
        assertEquals(15, f.plans.get(1).minutes);
    }

    /**
     * A fel-és-le dupla út, a kétszer dupla kör – és a kézenállás.
     *
     * A „6 emelet fel és le, kétszer" hat emeletnyi (három perc) lépcső
     * lett huszonnégy helyett, a „kézenállást gyakoroltam 20 percet"
     * pedig üresen jött vissza.
     */
    @Test
    public void stairsUpAndDownTwiceAndAHandstand() {
        // 24 emelet ≈ 12 perc.
        assertEquals(12, Activities.parse("Az irodában lépcsőztem "
                + "ebédszünetben, 6 emelet fel és le, kétszer.")
                .plans.get(0).minutes);
        Activities.Parsed k = Activities.parse("Kézenállást gyakoroltam "
                + "20 percet.");
        assertEquals(1, k.plans.size());
        assertEquals("joga", k.plans.get(0).kind.id);
        assertEquals(20, k.plans.get(0).minutes);
        // A megállások kétszere nem szorzó.
        assertEquals(3, Activities.parse("Ma 6 emeletet másztam meg "
                + "lépcsőn, kétszer kellett megállnom.")
                .plans.get(0).minutes);
    }

    /**
     * A kimondott hosszúságú edzés az „alatt" mellett is edzés.
     *
     * A „smartwatch 132-es átlagpulzust mért a 35 perces edzés alatt"
     * bejegyzéséből semmi nem lett – az „alatt" időpontnak mutatta a
     * megtörtént edzést.
     */
    @Test
    public void aTimedWorkoutUnderneathAnAlattStillCounts() {
        Activities.Parsed p = Activities.parse("A smartwatch 132-es "
                + "átlagpulzust mért a 35 perces edzés alatt.");
        assertEquals(1, p.plans.size());
        assertEquals(35, p.plans.get(0).minutes);
        // A hossz nélküli „edzés alatt ittam" horgonya marad horgony.
        assertTrue(Activities.parse("Edzés alatt ittam egy izotóniást.")
                .plans.isEmpty());
    }

    /**
     * A tűzifa pakolása, a hazatolt bicikli és a vezetett gyerekedzés.
     *
     * A „3 köbméter tűzifát pakoltam be" üresen jött vissza, a „bicajom
     * defektet kapott, 5 km-t toltam hazáig" tolása mellé egy órás
     * tekerés került, a gyerekeknek vezetett futballedzés pedig kilencven
     * perc focit írt az edző naplójába.
     */
    @Test
    public void firewoodAFlatTireAndACoachedKidsSession() {
        Activities.Parsed t = Activities.parse("Ma 3 köbméter tűzifát "
                + "pakoltam be.");
        assertEquals(1, t.plans.size());
        assertEquals("munka", t.plans.get(0).kind.id);
        Activities.Parsed d = Activities.parse("A bicajom defektet "
                + "kapott, 5 km-t toltam hazáig.");
        assertEquals(1, d.plans.size());
        assertEquals("tura", d.plans.get(0).kind.id);
        Activities.Parsed g = Activities.parse("Futball edzés "
                + "gyerekeknek, én vezettem, közben én is mozogtam vagy "
                + "30 percet.");
        assertEquals(1, g.plans.size());
        assertEquals(30, g.plans.get(0).minutes);
        // A rock and roll tánc, nem egyéb mozgás.
        assertEquals("tanc", Activities.parse("90 perc rock and roll "
                + "edzés volt a lányommal.").plans.get(0).kind.id);
    }

    /**
     * A most kezdődő óra terv, a már elkezdődött megtörtént.
     */
    @Test
    public void aClassAboutToStartIsAPlan() {
        assertTrue(Activities.parse("Ma este 19:30-kor kezdődik a "
                + "jógaóra, már becsekkoltam.").plans.isEmpty());
        assertEquals(1, Activities.parse("A jógaóra 19:30-kor "
                + "kezdődött, végig ott voltam.").plans.size());
    }

    /**
     * A „sok X-tól" ok, nem mai edzés.
     *
     * A „nyugalmi pulzusom lement 52-re a sok futástól" futása a
     * magyarázat, mégis negyvenöt perces mai futás került tőle a naplóba.
     */
    @Test
    public void aCauseFromMuchRunningIsNotTodaysRun() {
        assertTrue(Activities.parse("Nyugalmi pulzusom lement 52-re a "
                + "sok futástól.").plans.isEmpty());
        // A valódi futás a sok emelkedővel marad.
        assertEquals(1, Activities.parse("10 km futás, a sok "
                + "emelkedőtől elfáradtam.").plans.size());
    }

    /**
     * A szóközös sorozat és az edzőterem mérlege.
     *
     * A „20 x 50 m mellen" húsz külön úszás-alkalom lett húsz napra
     * szétosztva, az „edzőterem mérlegén 84,2 voltam" mérése mellé
     * pedig egy órás kondi került.
     */
    @Test
    public void aSpacedSeriesAndTheGymScale() {
        Activities.Parsed u = Activities.parse("Könnyű regeneráló "
                + "úszás, 20 x 50 m mellen.");
        assertEquals(1, u.plans.size());
        assertEquals(1, u.plans.get(0).count);
        assertEquals(1.0, u.plans.get(0).km, 0.01);
        assertTrue(Activities.parse("Az edzőterem mérlegén 84,2 voltam "
                + "cipőben.").plans.isEmpty());
        // A kondi a terem szavával marad edzés.
        assertEquals(1, Activities.parse("Kondiban voltam, az edzőterem "
                + "új gépei jók.").plans.size());
    }

    /**
     * A kisfiam meccse az övé, a zárójeles kör pedig egy edzés.
     *
     * A „kisfiam első focimeccse volt" kilencven perc focit írt az apa
     * naplójába; az „otthoni edzés: 3 kör (10 guggolás + 10 fekvő +
     * 10 hasizom), kb 15 perc" pedig KÉT kondi-bejegyzést kapott, és a
     * zárójel mögött a guggolás sora elveszett.
     */
    @Test
    public void aSonsMatchAndAParenthesizedCircuit() {
        assertTrue(Activities.parse("A kisfiam első focimeccse volt, "
                + "büszke apa vagyok.").plans.isEmpty());
        assertEquals(1, Activities.parse("A kisfiammal fociztunk "
                + "30 percet.").plans.size());
        Activities.Parsed c = Activities.parse("Otthoni edzés: 3 kör "
                + "(10 guggolás + 10 fekvő + 10 hasizom), kb 15 perc.");
        assertEquals(1, c.plans.size());
        assertEquals(15, c.plans.get(0).minutes);
    }

    /**
     * A bemelegítéses szakaszok egy edzés részei, és összeadódnak.
     *
     * A „futóklub keddi edzése: 2 km bemelegítés, 6x400 m, 2 km
     * levezetés" hat és fél kilométeréből csak az első kettő került be,
     * az „úszásedzés: 400 bemelegítés, 8x100 gyors, 200 levezetés"
     * csupasz számai pedig félig elvesztek.
     */
    @Test
    public void warmupSegmentsAddUpToOneSession() {
        Activities.Parsed f = Activities.parse("A futóklub keddi "
                + "edzése: 2 km bemelegítés, 6x400 m, 2 km levezetés.");
        assertEquals(1, f.plans.size());
        assertEquals(6.4, f.plans.get(0).km, 0.01);
        Activities.Parsed u = Activities.parse("Úszásedzés: 400 "
                + "bemelegítés, 8x100 gyors, 200 levezetés.");
        assertEquals(1, u.plans.size());
        assertEquals(1.4, u.plans.get(0).km, 0.01);
    }

    /**
     * A lezárt lépés-cél és a lelátón ülő néző.
     *
     * A „10 000 lépéses célt 12 340-nel zártam" üresen jött vissza, a
     * „jégkorong meccsen a harmadik sorban ültünk" pedig hatvan perc
     * korcsolyát írt a naplóba.
     */
    @Test
    public void aClosedStepGoalAndARinksideSeat() {
        assertEquals(12340, Activities.parse("A 10 000 lépéses célt "
                + "12 340-nel zártam.").plans.get(0).steps);
        assertTrue(Activities.parse("A jégkorong meccsen a harmadik "
                + "sorban ültünk, óriási hangulat volt.").plans.isEmpty());
    }

    /**
     * Az „egy jó óra" is egy óra.
     */
    @Test
    public void aGoodHourIsAnHour() {
        assertEquals(60, Activities.parse("A gyerekekkel "
                + "trambulinoztunk a kertben egy jó órát.")
                .plans.get(0).minutes);
    }

    /**
     * A ragos tempó tempó, az egy hete tartó szokás mellett a ma számít.
     *
     * Az „5:20-szal mentem 8 km-t" öt óra húsz perces futás lett, az
     * „a lift szerviz miatt egy hete lépcsőzöm, ma is 9 emelet kétszer"
     * pedig egy hete keltezett dupla bejegyzést kapott.
     */
    @Test
    public void aSuffixedPaceAndAWeekOldStairHabit() {
        Activities.Parsed p = Activities.parse("A kollégám szerint túl "
                + "gyorsan futok, de ma is 5:20-szal mentem 8 km-t.");
        assertEquals(43, p.plans.get(0).minutes);
        Activities.Parsed l = Activities.parse("A lift szerviz miatt "
                + "egy hete lépcsőzöm, ma is 9 emelet kétszer.");
        assertEquals(1, l.plans.size());
        assertEquals(0, l.offset);
        assertEquals(9, l.plans.get(0).minutes);
    }

    /**
     * A jelzős kilométer és a háromszor meghúzott csúcs.
     *
     * A „bejárattam az új futócipőt, 3 könnyű kilométer" üresen jött
     * vissza, a „holtemelésem új csúcsa 180 kg, háromszor húztam meg"
     * pedig három kondi-edzés lett, három napra szétosztva.
     */
    /**
     * A félbeszakadt tekerés táva és a befizetett díj.
     *
     * A „nyári zápor szakította félbe a tekerést 32 km-nél" táv nélkül
     * maradt, az „úszásoktatás díját befizettem, 8 alkalom 24 000 Ft"
     * pedig negyvenöt perc úszást írt a naplóba – a pénztárnál.
     */
    @Test
    public void anInterruptedRideAndAPaidFee() {
        assertEquals(32.0, Activities.parse("Nyári zápor szakította "
                + "félbe a tekerést 32 km-nél, beáztam rendesen.")
                .plans.get(0).km, 0.01);
        assertTrue(Activities.parse("Az úszásoktatás díját befizettem, "
                + "8 alkalom 24 000 Ft.").plans.isEmpty());
        // A jelzőtábla melletti km-nél marad érintetlen.
        assertTrue(Activities.parse("Láttam egy 5 km-nél jelzőtáblát.")
                .plans.isEmpty());
    }

    @Test
    public void anAdjectiveKilometerAndATripleLockout() {
        assertEquals(3.0, Activities.parse("Bejárattam az új futócipőt, "
                + "3 könnyű kilométer.").plans.get(0).km, 0.01);
        Activities.Parsed h = Activities.parse("A holtemelésem új "
                + "csúcsa 180 kg, háromszor húztam meg.");
        assertEquals(1, h.plans.size());
        assertEquals(1, h.plans.get(0).count);
        assertEquals(1, h.days);
    }

    /**
     * A gép kijelzőjén álló km, a hajrá és az átrendezett súlyzó.
     *
     * A „crosstrainer 40 percet mutatott és 5,2 km-t" mellé egy külön
     * futás került, az „utolsó 2 km-t sprintben nyomtam a 12-ből"
     * kétkilométeres bejegyzés lett, a „súlyzókat átrendeztük a
     * garázsban" pedig a fizikai munka mellé egy órás kondit is írt.
     */
    @Test
    public void aMachineReadoutAFinalPushAndTidiedWeights() {
        Activities.Parsed c = Activities.parse("A crosstrainer 40 "
                + "percet mutatott és 5,2 km-t.");
        assertEquals(1, c.plans.size());
        assertEquals(40, c.plans.get(0).minutes);
        assertEquals(12.0, Activities.parse("Hajrá: az utolsó 2 km-t "
                + "sprintben nyomtam a 12-ből.").plans.get(0).km, 0.01);
        Activities.Parsed sz = Activities.parse("A súlyzókat "
                + "átrendeztük a garázsban, az is felért egy edzéssel.");
        assertEquals(1, sz.plans.size());
        assertEquals("munka", sz.plans.get(0).kind.id);
        // A futópadon futott táv marad futás a crosstrainer mellett is.
        assertEquals(5.0, Activities.parse("A futópadon futottam 5 km-t, "
                + "a crosstraineren meg 20 percet.").plans.get(0).km, 0.01);
    }

    /**
     * A második meccs-szó a valódi mérkőzés, a „3 kör futás" pedig kör.
     *
     * A „meccs előtt 30 perc bemelegítés, a meccs 2x30 perc volt"
     * bejegyzéséből semmi nem lett – az első meccs-szó időpont-horgony
     * volt. A „3 kör futást toltam le" pedig hajnali háromra került.
     */
    @Test
    public void aSecondMatchWordAndLapsThatAreNotOClock() {
        Activities.Parsed m = Activities.parse("Meccs előtt 30 perc "
                + "bemelegítés, a meccs 2x30 perc volt.");
        assertEquals(1, m.plans.size());
        assertEquals(60, m.plans.get(0).minutes);
        assertEquals(12, Activities.parse("Az edzőterem zárva volt, így "
                + "a parkban toltam le 3 kör futást, 4,5 km.").hour);
        // Az „edzés után" horgonya marad horgony.
        assertTrue(Activities.parse("Edzés után ittam egy "
                + "fehérjeturmixot.").plans.isEmpty());
        // A napszakkal kimondott óra marad óra.
        assertEquals(7, Activities.parse("Reggel 7 kor 5 km futás.").hour);
    }

    /**
     * A tegnapi táv a tegnapi naplóé, a ragtalan görgő pedig henger.
     *
     * A „tegnapi 12 km után ma csak lazítottam, 20 perc görgő" TEGNAPRA
     * írt egy tizenkét kilométeres futást, a mai húsz perc lazítás meg
     * elveszett mellőle.
     */
    @Test
    public void yesterdaysDistanceAndABareFoamRoller() {
        Activities.Parsed p = Activities.parse("A tegnapi 12 km után ma "
                + "csak lazítottam, 20 perc görgő.");
        assertEquals(1, p.plans.size());
        assertEquals("joga", p.plans.get(0).kind.id);
        assertEquals(20, p.plans.get(0).minutes);
        assertEquals(0, p.offset);
        // A kerékpáros görgő marad tekerés.
        assertEquals("kerekpar", Activities.parse("Görgőn tekertem "
                + "40 percet.").plans.get(0).kind.id);
        // A tegnapi futás magában marad tegnapi bejegyzés.
        assertEquals(1, Activities.parse("Tegnap 12 km-t futottam.")
                .offset);
    }

    /**
     * A készülés igeneve, a medence fordított hossza és a játék perce.
     *
     * A „félmaratonra készülve ma 16 km-t futottam" mellé egy huszonegy
     * kilométeres futás is bekerült, az „a medence 33 méteres, 30 hosszt
     * úsztam" a huszonötös alapértékkel számolt, a „30 perc úszás és
     * 45 perc játék" negyvenöt perce pedig nyomtalanul eltűnt.
     */
    @Test
    public void aGerundPoolLengthAndPlayMinutes() {
        Activities.Parsed m = Activities.parse("A félmaratonra készülve "
                + "ma 16 km-t futottam hosszú futásként.");
        assertEquals(1, m.plans.size());
        assertEquals(16.0, m.plans.get(0).km, 0.01);
        assertEquals(0.99, Activities.parse("A medence 33 méteres, "
                + "30 hosszt úsztam benne.").plans.get(0).km, 0.01);
        Activities.Parsed v = Activities.parse("A vízilabda edzésen "
                + "30 perc úszás és 45 perc játék volt.");
        assertEquals(1, v.plans.size());
        assertEquals(75, v.plans.get(0).minutes);
        // A beálló játékideje marad a bejegyzés hossza.
        assertEquals(45, Activities.parse("90 perces meccs, én a második "
                + "félidőben álltam be, kb 45 perc játék.")
                .plans.get(0).minutes);
        // A lefutott félmaraton marad huszonegy kilométer.
        assertEquals(21.1, Activities.parse("Lefutottam a félmaratont "
                + "1:58 alatt.").plans.get(0).km, 0.05);
    }

    /**
     * A tolt bicikli nem tekerés.
     *
     * A „gyerek biciklijét toltam fel a dombra, közben én is gyalogoltam
     * 2 km-t" mellé egy órás kerékpározás került.
     */
    @Test
    public void aPushedBikeIsNotARide() {
        Activities.Parsed p = Activities.parse("A gyerek biciklijét "
                + "toltam fel a dombra, közben én is gyalogoltam 2 km-t.");
        assertEquals(1, p.plans.size());
        assertEquals("tura", p.plans.get(0).kind.id);
        // A tekert bicikli marad tekerés.
        assertEquals("kerekpar", Activities.parse("Bicikliztem 20 km-t.")
                .plans.get(0).kind.id);
    }

    /**
     * A bemelegítés ideje nem a futás ideje – kimondott táv mellett sem.
     *
     * A „ma reggel 5 km futás előtt 10 perc bemelegítés, utána 10 perc
     * levezetés" öt kilométeres futása TÍZ percet kapott.
     */
    @Test
    public void warmupOnlyTimesNeverBecomeTheRunsTime() {
        assertEquals(30, Activities.parse("Ma reggel 5 km futás előtt "
                + "10 perc bemelegítés, utána 10 perc levezetés.")
                .plans.get(0).minutes);
        assertEquals(30, Activities.parse("10 perc bemelegítés, aztán "
                + "5 km futás.").plans.get(0).minutes);
        // A sport saját ideje marad az övé.
        assertEquals(40, Activities.parse("20 perc bemelegítés + 40 perc "
                + "foci.").plans.get(0).minutes);
        assertEquals(25, Activities.parse("5 km futás 25 perc alatt.")
                .plans.get(0).minutes);
    }

    /**
     * A napüdvözletek száma nem napok száma, a szokás jelen ideje pedig
     * kimondott táv mellett megtörtént út.
     *
     * A „108 napüdvözletet csináltunk végig, 90 perc" bejegyzése
     * SZÁZNYOLC napra terült szét, a „konditerembe biciklivel megyek,
     * oda-vissza 5 km, plusz az edzés 1 óra" pedig üresen jött vissza.
     */
    @Test
    public void sunSalutationsAndAHabitualCommute() {
        Activities.Parsed j = Activities.parse("A jógaórán ma 108 "
                + "napüdvözletet csináltunk végig, 90 perc.");
        assertEquals(1, j.days);
        assertEquals(90, j.plans.get(0).minutes);
        Activities.Parsed b = Activities.parse("A konditerembe "
                + "biciklivel megyek, oda-vissza 5 km, plusz az edzés "
                + "1 óra.");
        assertEquals(1, b.plans.size());
        assertEquals(5.0, b.plans.get(0).km, 0.01);
        // A jövő szavával kimondott terv marad terv.
        assertTrue(Activities.parse("Szombaton sítúrára megyünk.")
                .plans.isEmpty());
        assertTrue(Activities.parse("Holnap futni megyek 10 km-t.")
                .plans.isEmpty());
    }

    /**
     * A piramis szakaszai összeadódnak, a lépcsőn futás pedig lépcsőzés.
     *
     * Az „intervall edzés: 400-800-1200-800-400 m" négyszáz méteres
     * futás lett, a „lépcsőházban futottam fel-le 10-szer a 4. emeletig"
     * pedig TÍZ külön futást írt a naplóba, tíz napra szétosztva.
     */
    @Test
    public void aPyramidAddsUpAndStairRunsAreStairs() {
        assertEquals(3.6, Activities.parse("Az intervall edzés: "
                + "400-800-1200-800-400 m, köztük 2 perc pihenés.")
                .plans.get(0).km, 0.01);
        Activities.Parsed l = Activities.parse("Ma a lépcsőházban "
                + "futottam fel-le 10-szer a 4. emeletig.");
        assertEquals(1, l.plans.size());
        assertEquals(1, l.days);
        assertEquals(20, l.plans.get(0).minutes);
        // A tartomány két száma marad tartomány.
        assertEquals(1, Activities.parse("10-15 perc kondi volt csak.")
                .plans.size());
        // A lépcső nélküli futás marad futás.
        assertEquals("futas", Activities.parse("Futottam 10 km-t a "
                + "parkban.").plans.get(0).kind.id);
    }

    /**
     * Az „ebből" a részt mondja ki, és a terem mérlege sem edzés.
     *
     * Az „uszodában 45 perc, ebből 30 perc úszás, 15 perc vízitorna"
     * mellé egy második, tizenöt perces úszás is bekerült; az
     * „edzőterem saját mérlege 1,5 kilót többet mutat" pedig egy órás
     * kondit írt a naplóba.
     */
    @Test
    public void aPartOfTheTotalAndTheGymsOwnScale() {
        Activities.Parsed u = Activities.parse("Uszodában 45 perc, "
                + "ebből 30 perc úszás, 15 perc vízitorna.");
        assertEquals(1, u.plans.size());
        assertEquals(45, u.plans.get(0).minutes);
        assertTrue(Activities.parse("Az edzőterem saját mérlege 1,5 "
                + "kilót többet mutat, mint az otthoni.").plans.isEmpty());
        // A lépésekből levont futás marad két külön tétel.
        assertEquals(2, Activities.parse("Ma 12 500 lépés, ebből 5 km "
                + "futás volt.").plans.size());
    }

    /**
     * A km-lista egysége csak egyszer áll ott, és a gyerek órája alatti
     * saját úszás is megvan.
     *
     * A „hosszú hétvégén összesen 3 túra volt: 12, 18 és 9 km" mindhárom
     * túrája HAT kilométert kapott – az összeget elosztva. A „gyerek
     * úszásoktatása alatt én 20 hosszt úsztam" pedig üresen jött vissza.
     */
    @Test
    public void aSharedKilometerUnitAndMyOwnSwim() {
        Activities.Parsed t = Activities.parse("A hosszú hétvégén "
                + "összesen 3 túra volt: 12, 18 és 9 km.");
        assertEquals(3, t.plans.size());
        assertEquals(12.0, t.plans.get(0).km, 0.01);
        assertEquals(18.0, t.plans.get(1).km, 0.01);
        assertEquals(9.0, t.plans.get(2).km, 0.01);
        Activities.Parsed u = Activities.parse("A gyerek úszásoktatása "
                + "alatt én 20 hosszt úsztam a másik medencében.");
        assertEquals(1, u.plans.size());
        assertEquals(0.5, u.plans.get(0).km, 0.01);
        // A tizedes vessző nem listahatár.
        assertEquals(2.5, Activities.parse("Ma 2,5 km-t sétáltam.")
                .plans.get(0).km, 0.01);
    }

    /**
     * Az úszásnemek szakaszai egy edzés részei.
     *
     * Az „úszásom 1000 m volt gyorson, 400 m mellen, 200 m háton"
     * ezerhatszáz métere KÉT bejegyzésre esett szét, és a kétszáz méter
     * el is veszett.
     */
    @Test
    public void swimStrokeSegmentsAddUp() {
        Activities.Parsed u = Activities.parse("Az úszásom 1000 m volt "
                + "gyorson, 400 m mellen, 200 m háton.");
        assertEquals(1, u.plans.size());
        assertEquals(1.6, u.plans.get(0).km, 0.01);
        // Az egyetlen úszásnem táva marad annyi, amennyi.
        assertEquals(1.5, Activities.parse("1500 m gyorson úsztam.")
                .plans.get(0).km, 0.01);
        // A sorozatos úszás szorzata sem sérül.
        assertEquals(0.8, Activities.parse("8x100 m gyorson, köztük "
                + "30 mp pihi.").plans.get(0).km, 0.01);
    }

    /**
     * A távolabb álló szorzó is az emeleteké.
     *
     * A „ma is 5 emeletet mentem fel gyalog háromszor a lépcsőn"
     * háromszorosa HÁROM külön alkalom lett, három perccel – tizenöt
     * emelet helyett.
     */
    @Test
    public void aDistantMultiplierStillCountsFloors() {
        Activities.Parsed p = Activities.parse("Ma is 5 emeletet mentem "
                + "fel gyalog háromszor a lépcsőn.");
        assertEquals(1, p.plans.size());
        assertEquals(8, p.plans.get(0).minutes);
        // A fel-és-le dupla útja marad.
        assertEquals(12, Activities.parse("Az irodában lépcsőztem "
                + "ebédszünetben, 6 emelet fel és le, kétszer.")
                .plans.get(0).minutes);
    }

    /**
     * A rövid sprint-szakaszok percei összeadódnak.
     *
     * A „6x1 perc sprint a dombon" ÖT perces futás lett – a szorzat
     * helyett egy alapérték –, és a „köztük lesétálás" mellé egy
     * másfél órás túra is bekerült.
     */
    @Test
    public void shortSprintSegmentsAddUpTheirMinutes() {
        Activities.Parsed p = Activities.parse("Ma 6x1 perc sprint a "
                + "dombon, köztük lesétálás.");
        assertEquals(1, p.plans.size());
        assertEquals(6, p.plans.get(0).minutes);
        assertEquals(12, Activities.parse("6x2 perc sprint.")
                .plans.get(0).minutes);
        // A hosszú blokk marad egyetlen blokk.
        assertEquals(25, Activities.parse("2x25 perc intervall futás.")
                .plans.get(0).minutes);
        // A súlyzós tartás ideje a súlyzós olvasóé.
        assertEquals(60, Activities.parse("Plank 3x1 perc.")
                .plans.get(0).minutes);
    }

    /**
     * A szokás igéje mellett a mai szám a mai edzés, a hétvégi verseny
     * pedig cél, nem dátum – és a gyakorlat-felsorolás tagja nem kardió.
     *
     * A „biciklizni szoktunk, ma 12 km lett" tizenkét kilométere
     * FUTÁSKÉNT került be, a „hétvégi versenyre ma regeneráló futás"
     * szombatra csúszott, az „edzőteremben a hátam volt soron:
     * húzódzkodás, evezés, lehúzás" mellé pedig egy félórás evezőgépezés
     * is bekerült.
     */
    @Test
    public void aHabitsSportAWeekendRaceAndAGymList() {
        Activities.Parsed b = Activities.parse("Este a lányommal "
                + "biciklizni szoktunk, ma 12 km lett.");
        assertEquals(1, b.plans.size());
        assertEquals("kerekpar", b.plans.get(0).kind.id);
        assertEquals(12.0, b.plans.get(0).km, 0.01);
        Activities.Parsed v = Activities.parse("A hétvégi versenyre ma "
                + "regeneráló futás volt csak, 4 km lassan.");
        assertEquals(0, v.offset);
        assertEquals(1, v.days);
        Activities.Parsed g = Activities.parse("Az edzőteremben ma a "
                + "hátam volt soron: húzódzkodás, evezés, lehúzás.");
        assertEquals(1, g.plans.size());
        assertEquals("kondi", g.plans.get(0).kind.id);
        // A kimondott idejű evezés marad külön tétel.
        assertEquals(2, Activities.parse("Kondi és utána evezés a gépen "
                + "20 perc.").plans.size());
    }

    /**
     * A cipő kilométere nem mai futás, az összetett úszás-szó viszont
     * úszásnak mutatja a gazdátlan távot.
     *
     * A „futócipőm 800 km-t futott már" mellé egy negyvenöt perces
     * futás került, a „Balaton-ÁTúszás, ma 2500 m technikai edzés"
     * kétezer-ötszáz métere pedig futás lett.
     */
    @Test
    public void shoeMileageAndACompoundSwimWord() {
        assertTrue(Activities.parse("A futócipőm 800 km-t futott már, "
                + "ideje lecserélni.").plans.isEmpty());
        Activities.Parsed b = Activities.parse("Két hét múlva jön a "
                + "Balaton-átúszás, ma 2500 m technikai edzés.");
        assertEquals(1, b.plans.size());
        assertEquals("uszas", b.plans.get(0).kind.id);
        assertEquals(2.5, b.plans.get(0).km, 0.01);
        // A cipőben megtett saját táv marad futás.
        assertEquals(10.0, Activities.parse("Az új cipőben ma 10 km-t "
                + "futottam.").plans.get(0).km, 0.01);
    }

    /**
     * A „mindkettő" azt mondja, hogy mindkét alkalom megvolt.
     *
     * A „naponta kétszer 10 perc gyógytorna, ma is megvolt mindkettő"
     * TÍZ percet írt a naplóba a húsz helyett.
     */
    @Test
    public void bothDailySessionsCount() {
        Activities.Parsed p = Activities.parse("A gerincem miatt "
                + "naponta kétszer 10 perc gyógytorna, ma is megvolt "
                + "mindkettő.");
        assertEquals(1, p.plans.size());
        assertEquals(20, p.plans.get(0).minutes);
        // A puszta terv marad terv.
        assertTrue(Activities.parse("Naponta kétszer 10 perc gyógytorna "
                + "a terv.").plans.isEmpty());
    }

    /**
     * Az előírás, a lépés-cél és a tagadott saját mozgás.
     *
     * Az „orvos heti három úszást javasolt, ma volt az első, 800 m"
     * HÁROM úszást írt a naplóba, a „napi 10 ezer lépésből ma csak
     * 6 ezer jött össze" a CÉL tízezrét vette, az „uszodában a
     * gyerekekre vigyáztam, magam nem úsztam semmit" pedig negyvenöt
     * perc úszást.
     */
    @Test
    public void adviceAStepGoalAndADeniedSwim() {
        Activities.Parsed j = Activities.parse("Az orvos heti három "
                + "úszást javasolt, ma volt az első, 800 m.");
        assertEquals(1, j.plans.size());
        assertEquals(1, j.plans.get(0).count);
        assertEquals("uszas", j.plans.get(0).kind.id);
        assertEquals(6000, Activities.parse("A napi 10 ezer lépésből ma "
                + "csak 6 ezer jött össze.").plans.get(0).steps);
        assertTrue(Activities.parse("Az uszodában a gyerekekre "
                + "vigyáztam, magam nem úsztam semmit.").plans.isEmpty());
    }

    /**
     * A főnévi igenév + kell előírás, nem megtörtént edzés.
     *
     * A „túl feszes a combizmom, nyújtani kell" negyvenöt perces jógát
     * írt a naplóba – abból, amit a felhasználónak MAJD kellene tennie.
     */
    @Test
    public void anInfinitiveWithKellIsAPrescription() {
        assertTrue(Activities.parse("Túl feszes a combizmom, nyújtani "
                + "kell.").plans.isEmpty());
        assertTrue(Activities.parse("Az orvos szerint úszni kell a "
                + "hátamra.").plans.isEmpty());
        // A megtörtént nyújtás marad edzés.
        assertEquals(20, Activities.parse("Ma nyújtottam 20 percet.")
                .plans.get(0).minutes);
    }

    /**
     * A gyalog megtett emelet is lépcsőzés.
     *
     * A „ma három emelet gyalog" KILENCVEN perces gyaloglás lett – a
     * lépcső szava nélkül az emeletek nem váltak perccé.
     */
    @Test
    public void floorsOnFootAreStairs() {
        assertEquals(2, Activities.parse("Ma három emelet gyalog, plusz "
                + "a boltból cipeltem 10 kilót haza.")
                .plans.get(0).minutes);
        assertEquals(4, Activities.parse("Ma 8 emelet gyalog.")
                .plans.get(0).minutes);
        // A sima gyaloglás ideje marad a sajátja.
        assertEquals(20, Activities.parse("Gyalog mentem a boltba, "
                + "20 perc.").plans.get(0).minutes);
    }

    /**
     * A körök a bennük futott távot is szorozzák.
     *
     * A „reggeli edzésen 3 kör: 400 m futás, 20 guggolás, 10
     * fekvőtámasz" NÉGYSZÁZ méteres futást írt a naplóba az
     * ezerkétszáz helyett – pedig a sorozatokat már háromszorosan
     * számolta.
     */
    @Test
    public void roundsMultiplyTheDistanceInsideThem() {
        Activities.Parsed p = Activities.parse("A reggeli edzésen 3 kör: "
                + "400 m futás, 20 guggolás, 10 fekvőtámasz.");
        double km = 0;
        for (Activities.Plan pl : p.plans)
            if ("futas".equals(pl.kind.id)) km = pl.km;
        assertEquals(1.2, km, 0.01);
        // A pálya hossza körökkel szorozva marad a régi olvasat.
        assertEquals(2.0, Activities.parse("5 kör a 400 m-es pályán.")
                .plans.get(0).km, 0.01);
    }

    /**
     * A „de csak N-öt" a mai táv, a terv tagmondata pedig csak a
     * sajátját viszi.
     *
     * Az „a tegnapi 10 km után ma is futottam, de csak 5-öt" TEGNAPRA
     * írt tíz kilométert, az „úszásnál 1500 m volt a terv, de csak
     * 1000 m-t úsztam" bejegyzéséből pedig semmi nem lett.
     */
    @Test
    public void todaysShorterDistanceAndAPlanClause() {
        Activities.Parsed f = Activities.parse("A tegnapi 10 km után ma "
                + "is futottam, de csak 5-öt.");
        assertEquals(1, f.plans.size());
        assertEquals(0, f.offset);
        assertEquals(5.0, f.plans.get(0).km, 0.01);
        Activities.Parsed u = Activities.parse("Az úszásnál 1500 m volt "
                + "a terv, de csak 1000 m-t úsztam.");
        assertEquals(1, u.plans.size());
        assertEquals(1.0, u.plans.get(0).km, 0.01);
        // A puszta terv marad terv.
        assertTrue(Activities.parse("Ma 12 km volt a terv.")
                .plans.isEmpty());
    }

    /**
     * A lépcsőzés szorzója emelet nélkül sem alkalomszám, és a meccs
     * mellett elfogyasztott vacsora sem edzés.
     *
     * A „csak a lépcsőt jártam meg 5-ször" ÖT bejegyzést írt a naplóba,
     * öt napra szétosztva; a „két sör és egy pizza volt a vacsora a
     * meccs mellett" negyvenöt perc egyéb mozgást.
     */
    @Test
    public void stairCountsAndAMatchOnTheTelly() {
        Activities.Parsed l = Activities.parse("Ma nem volt időm edzeni, "
                + "csak a lépcsőt jártam meg 5-ször.");
        assertEquals(1, l.plans.size());
        assertEquals(1, l.days);
        assertTrue(Activities.parse("Két sör és egy pizza volt a vacsora "
                + "a meccs mellett.").plans.isEmpty());
        // A kondi melletti tagadás a kondit meghagyja.
        assertEquals("kondi", Activities.parse("nem futottam a kondi "
                + "mellett").plans.get(0).kind.id);
    }

}
