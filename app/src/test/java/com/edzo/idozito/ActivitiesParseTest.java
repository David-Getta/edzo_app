package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        Activities.Parsed p = Activities.parse("hétvégi hosszú futás 18 km 1:45");
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
        assertEquals("1d+0: 1×evezes/3, 1×kondi/60",
                summary("5 kör: 500 m evezés, 15 kettlebell swing"));
        // A kimondott alkalom megvédi magát: ott a szám után az edzés szó áll.
        assertEquals(2, Activities.parse("2 fekvőtámasz edzés").plans.get(0).count);
        // A saját nevén futó mozgás sem sérül: a „3 futás" három futás.
        assertEquals("7d+0: 3×futas/45", summary("3 futás és 3x10 fekvenyomás a héten"));
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
}
