package com.edzo.idozito;

import java.util.List;

/**
 * Melyik naplóba való ez a mondat?
 *
 * Az app négy helyen ért mondatot – étrend, edzés-előzmény, erősítő sorozat,
 * időzítő –, de mindegyik csak a sajátját. Aki az étkezés-mezőbe írja be, hogy
 * „30 perc futás", annyit kap: „ezt még nem ismerem". Pedig ismeri, csak egy
 * képernyővel odébb. A mondat jó volt, csak rossz ajtón kopogott.
 *
 * Ez az osztály megmondja, hova való – hogy a képernyő, amelyik nemet mondott,
 * legalább útba tudja igazítani. Szándékosan csak akkor kérdezzük meg, ha a
 * saját felismerő már üresen tért vissza: a találgatás sosem előzheti meg azt,
 * amit a képernyő maga biztosan tud.
 */
public final class Sentence {

    private Sentence() {}

    /** Hova való a mondat. NONE = egyik felismerő sem tud vele mit kezdeni. */
    public enum Kind { NONE, MEAL, WORKOUT, STRENGTH, INTERVAL, BODY, ROUTINE, SLEEP, REHAB, PULSE }

    /** Az átirányított mondat Intent-kulcsa: a cél-képernyő ezzel nyílik meg. */
    public static final String EXTRA = "sentence";

    /**
     * A mondat legvalószínűbb helye.
     *
     * A sorrend nem önkényes: a sorozatos mondat („3x10 fekvenyomás") a
     * legfajsúlyosabb, mert benne a súly és az ismétlés is megvan, és ezt CSAK
     * az erősítő napló őrzi meg. Utána a megtörtént edzés jön, mert az
     * időzítő-terv ugyanazokból a számokból áll („4 kör 40 mp"), de a napló
     * felismerője mozgásformát is kér hozzá – ha az megvan, akkor az edzés
     * volt, nem terv. Az étel a végén: a legrövidebb szótövek itt vannak, és
     * ezek akadnak be legkönnyebben egy edzés-mondatba.
     *
     * @param foods a felismerhető ételek (a sajátokkal együtt), vagy null
     */
    public static Kind of(String q, List<Foods.Food> foods, long now) {
        // A rövidke bejegyzés is bejegyzés, ha szám ÉS mértékegység van
        // benne: az „5k" a futók legrövidebb naplósora, mégis a három
        // karakteres alsó korláton akadt fenn – a „10k" átment, az „5k"
        // nem. A többi kétbetűs szó („ok", „hm") a felismerőkön úgyis
        // fennakad.
        if (q == null) return Kind.NONE;
        if (q.trim().length() < 3 && !q.trim().matches("\\d\\s?[a-zA-Z]"))
            return Kind.NONE;
        // A KÉRDÉS nem bejegyzés: a „mennyi kalória van a banánban?" banánt
        // naplózott volna, a „mit egyek edzés előtt?" pedig edzést. Aki
        // kérdez, az nem most evett és nem most edzett – a kérdőjel a
        // legmegbízhatóbb jel, amit egy magyar mondat adhat erről.
        if (q.trim().endsWith("?")) {
            // Egy kivétellel: a rehab-lap nem NAPLÓZ, hanem mutat. A „mit
            // csináljak a fájó vállammal?" és a „van valami gyakorlat a
            // derékfájásra?" pont az a kérdés, amire jó válaszunk van –
            // kérdőjellel is. Bejegyzés ebből sem lesz, tehát nem árthat.
            if (Rehab.forComplaint(q) != null || Rehab.forGoal(q) != null
                    || Rehab.redFlag(q) != null
                    || Rehab.vagueComplaint(q)) return Kind.REHAB;
            return Kind.NONE;
        }
        if (!StrengthParse.parse(q).isEmpty()) return Kind.STRENGTH;
        IntervalParse.Plan iv = IntervalParse.parse(q);
        // Az alvás-mondat sosem időzítő-terv. A „8-9 órát aludtam" tól-ig
        // párja pont úgy néz ki, mint egy munka/pihenő ritmus, és a felismerő
        // be is állította rá az órát. Kimondott alvás-szó kell hozzá, tehát
        // ez az ág mástól nem vesz el semmit.
        if (iv != null && Sleep.parse(q) > 0) return Kind.SLEEP;
        // A PIHENŐT is kimondó, többköros terv egyértelműen időzítő: a
        // megtörtént edzésről senki nem írja le, hogy „20 mp pihenő". Ez a
        // kivétel a megosztott sablonok miatt kell – azok nevében gyakran ott
        // egy sportszó („Zsírégető HIIT", „Kondi kör"), és attól a mondat
        // megtörtént edzésnek látszott.
        if (iv != null && !iv.guessed && iv.rest > 0 && iv.rounds >= 2) return Kind.INTERVAL;
        // Edzésnap-lista (sorozatok NÉLKÜL felsorolt gyakorlatok, névvel):
        // ez az edzés-felismerő elé kell, mert a nap neve gyakran maga is
        // sportszó („Lábnap", „Tolónap") – attól megtörtént edzésnek látszana.
        if (Routines.parseShared(q) != null) return Kind.ROUTINE;
        // A panasz az edzés-felismerő ELÉ kerül: a „fáj a térdem futás után"
        // és a „golfkönyök fájdalom" nem edzés – hiába van benne sportszó, a
        // fájdalom-szó mást mond. Kimondott fájdalom nélkül ez az ág nem él.
        // A cél-mondat („boka stabilitás") ugyanide fut: az is a rehab ajtaja.
        // A piros zászlós panasz („zsibbad a kezem") is ide tartozik: sort
        // nem ajánlunk rá, de a hallgatás rosszabb – ott a figyelmeztetés
        // vár rá, nem egy „nem értem".
        if (Rehab.forComplaint(q) != null || Rehab.forGoal(q) != null
                || Rehab.redFlag(q) != null
                // Az ÁLTALÁNOS panasz („fáj a lábam") is a rehab ajtaja:
                // testtájat nem tudunk mondani rá, de a „nem értem" a
                // legrosszabb válasz – a lap testtáj nélkül nyílik ki.
                || Rehab.vagueComplaint(q)) return Kind.REHAB;
        Activities.Parsed a = Activities.parse(q, now);
        if (a != null && !a.isEmpty()) return Kind.WORKOUT;
        if (iv != null) return Kind.INTERVAL;
        // A súlyt ÉS a testzsírt is kimondó mondat félreérthetetlenül mérés:
        // a „78 kg 18% zsír"-ban a zsír nem a konyhai zsír, hiába ismeri fel
        // az étel-oldal is. Csak ez a kettős alak előzi meg az étkezést; a
        // puszta kiló továbbra is a lista végén dől el.
        BodyParse.Body body = BodyParse.parse(q);
        if (body.kg > 0 && body.fatPct > 0) return Kind.BODY;
        // A mérőszalag adata is félreérthetetlen, ha ki van írva a centi: a
        // comb, a mell és a kar egyszerre testrész és étel, és az étel
        // hamarabb szólal meg – a „comb 58 cm" eddig csirkecomb volt.
        if (BodyParse.girthWithUnit(q)) return Kind.BODY;
        if (foods != null && !Foods.parse(foods, q).isEmpty()) return Kind.MEAL;
        // Kimondott kalória étel nélkül („vacsora 650 kcal"): ez is étkezés,
        // csak épp nincs benne olyan szó, amit az adatbázis ismerne.
        if (Kcal.stated(q) > 0) return Kind.MEAL;
        // A fehérje ugyanígy: a „120 g fehérjét vittem be ma" étkezés-mondat,
        // csak épp egyetlen étel nincs benne, amit az adatbázis ismerne. Eddig
        // semmi nem lett belőle – pedig a napi fehérje a saját sávjával együtt
        // ott van a naplóban.
        if (Kcal.protein(q) > 0) return Kind.MEAL;
        // A mérés a legvégén: a kilogramm a legterheltebb mértékegység az
        // appban, ezért a testsúly csak arra a maradékra jelentkezik, amit
        // senki más nem kért magának.
        if (!body.isEmpty()) return Kind.BODY;
        // Az alvás a legvégén: kimondott alvás-szó kell hozzá, tehát nem
        // veszélyes – de ami eddig bármi másnak elment, az maradjon az.
        if (Sleep.parse(q) > 0) return Kind.SLEEP;
        // A nyugalmi pulzus ugyanilyen biztonságos: kimondott pulzus-szó kell.
        if (Pulse.parse(q) > 0) return Kind.PULSE;
        return Kind.NONE;
    }

    /**
     * A mondat MÁSODIK naplója, vagy NONE.
     *
     * Egy mondat gyakran két dologról szól: „futottam 30 percet és ettem egy
     * banánt", „aludtam 8 órát, nyugalmi pulzus 52". Az útbaigazító eddig
     * eldöntötte, melyik a fontosabb, a másikat pedig csendben eldobta – a
     * banán sehol nem jelent meg, és a felhasználó nem is tudta meg, hogy
     * elveszett. Ez ugyanaz a csendes hiba, mint a meg nem történt bejegyzés,
     * csak fordítva.
     *
     * Szándékosan szűk: csak azokat a párokat adjuk vissza, amelyek nem
     * eshetnek egymás rovására. Az étel-felismerő huszonkét valódi
     * edzés-mondaton egyetlen ételt sem talált, tehát ha talál, az tényleg
     * ott van.
     */
    public static Kind also(String q, List<Foods.Food> foods, long now) {
        List<Kind> all = extras(q, foods, now);
        return all.isEmpty() ? Kind.NONE : all.get(0);
    }

    /**
     * A mondat ÖSSZES további naplója, fontossági sorrendben.
     *
     * Az {@link #also} csak az elsőt adja vissza, és egy hosszabb mondat
     * ennél többről szól: a „ma reggel 6-kor keltem, 79,2 kg volt a mérleg,
     * futottam 8 km-t, utána zabkása" négy adatot mond ki, és eddig kettő
     * közülük nyomtalanul eltűnt. A képernyő ebből annyit ajánl fel, amennyi
     * elfér – de legalább tudja, mi van még a mondatban.
     */
    public static List<Kind> extras(String q, List<Foods.Food> foods, long now) {
        List<Kind> out = new java.util.ArrayList<>();
        Kind k = of(q, foods, now);
        switch (k) {
            case WORKOUT: case STRENGTH: case INTERVAL: case ROUTINE:
                // A sorozat mellé odaírt FUTÁS is elveszett eddig: a „reggel 5
                // km futás, utána 20 fekvőtámasz" mondatból csak a fekvőtámasz
                // maradt meg, a kilométerek nyomtalanul eltűntek. Az erősítő
                // napló nem tud távot tárolni, tehát ezt csak az előzmények
                // őrizhetik meg. Szándékosan csak a KIMONDOTT táv (vagy
                // lépésszám) számít: a puszta „edzés" szóból becsült hatvan
                // perc kétszer kerülne be, egyszer sorozatként, egyszer
                // mozgásként.
                if (k == Kind.STRENGTH && hasDistance(q, now)) out.add(Kind.WORKOUT);
                if (foods != null && !Foods.parse(foods, q).isEmpty()) out.add(Kind.MEAL);
                // Az edzés mellé a reggeli MÉRÉS is odaférhet: a „10 km futás,
                // 78,5 kg a mérlegen" és az „aludtam 7 órát és futottam 10
                // km-t" második fele eddig nyomtalanul eltűnt. A sorrend a
                // biztosból a bizonytalan felé megy: a mérleg száma a
                // legegyértelműbb, a pulzus a legrövidebb.
                if (!BodyParse.parse(q).isEmpty()) out.add(Kind.BODY);
                if (Sleep.parse(q) > 0) out.add(Kind.SLEEP);
                if (Pulse.parse(q) > 0) out.add(Kind.PULSE);
                break;
            // A reggeli három adat egy mondatban: „ma reggel 78,4 kg, aludtam
            // 7 órát, nyugalmi pulzus 52". Mindhárom a Profil naplója, és
            // eddig csak egy került be közülük.
            case SLEEP:
                if (Pulse.parse(q) > 0) out.add(Kind.PULSE);
                if (!BodyParse.parse(q).isEmpty()) out.add(Kind.BODY);
                break;
            case PULSE:
                if (Sleep.parse(q) > 0) out.add(Kind.SLEEP);
                if (!BodyParse.parse(q).isEmpty()) out.add(Kind.BODY);
                break;
            case BODY:
                if (Sleep.parse(q) > 0) out.add(Kind.SLEEP);
                if (Pulse.parse(q) > 0) out.add(Kind.PULSE);
                break;
            // Az étkezés mellé is odaférhet a mérés: az „ettem egy pizzát és
            // aludtam 9 órát" kilenc órája eddig sehol nem jelent meg.
            case MEAL:
                if (Sleep.parse(q) > 0) out.add(Kind.SLEEP);
                if (!BodyParse.parse(q).isEmpty()) out.add(Kind.BODY);
                if (Pulse.parse(q) > 0) out.add(Kind.PULSE);
                break;
            // A panasz mellett is ott lehet a napi mérés.
            case REHAB:
                // A panasz mellett ott lehet a megtörtént edzés is: a „fájt a
                // térdem, ezért csak bicikliztem 40 percet" negyven perce
                // eddig nyomtalanul eltűnt – a rehab-lap nem naplóz. Csak a
                // KIMONDOTT mennyiség számít: a mozgásforma szokásos hossza
                // ilyenkor találgatás lenne egy panasz-mondatban.
                if (hasRealAmount(q, now)) out.add(Kind.WORKOUT);
                if (!BodyParse.parse(q).isEmpty()) out.add(Kind.BODY);
                if (Sleep.parse(q) > 0) out.add(Kind.SLEEP);
                if (Pulse.parse(q) > 0) out.add(Kind.PULSE);
                break;
            default:
                break;
        }
        return out;
    }

    /**
     * Van-e a mondatban kimondott táv vagy lépésszám?
     *
     * Ez a legszigorúbb jele annak, hogy a mondatban egy önálló kardió-mozgás
     * is van: a becsült időtartam még nem az, mert azt a mozgásforma neve
     * magától is megadja.
     */
    private static boolean hasDistance(String q, long now) {
        Activities.Parsed a = Activities.parse(q, now);
        if (a == null) return false;
        for (Activities.Plan p : a.plans)
            if (p.km > 0 || p.steps > 0) return true;
        return false;
    }

    /**
     * Van-e a mondatban KIMONDOTT mennyiségű mozgás?
     *
     * A táv, a lépésszám és a kimondott időtartam számít – a mozgásforma
     * szokásos hossza nem: abból egy panasz-mondat mellé negyvenöt perces
     * bejegyzés lenne, ami meg sem történt.
     */
    private static boolean hasRealAmount(String q, long now) {
        Activities.Parsed a = Activities.parse(q, now);
        if (a == null) return false;
        for (Activities.Plan p : a.plans)
            if (p.km > 0 || p.steps > 0 || p.minutes != p.kind.defaultMin) return true;
        return false;
    }

    /** A napló neve, ahová a mondat való („Erősítő napló"). */
    public static String where(Kind k) {
        switch (k) {
            case MEAL: return "Étrend";
            case WORKOUT: return "Edzés-előzmények";
            case STRENGTH: return "Erősítő napló";
            case INTERVAL: return "Időzítő";
            case BODY: return "Profil";
            case ROUTINE: return "Edzésnapok";
            case SLEEP: return "Profil";
            case PULSE: return "Profil";
            case REHAB: return "Nyújtás & rehab";
            default: return "";
        }
    }

    /**
     * Egysoros útbaigazítás a rossz képernyőn – koppintható felirathoz.
     *
     * A hangnem szándékosan nem mentegetőzik: a felhasználó nem hibázott,
     * csak egy ajtóval odébb van, amit keres.
     */
    public static String hint(Kind k) {
        switch (k) {
            case MEAL: return "🍽 Ez inkább étkezésnek tűnik – koppints, és az Étrendbe viszem.";
            case WORKOUT: return "🏃 Ez inkább edzésnek tűnik – koppints, és az Előzményekbe viszem.";
            case STRENGTH: return "🏋️ Ez erősítő sorozatnak tűnik – koppints, és az Erősítő naplóba viszem.";
            case INTERVAL: return "⏱️ Ez időzítő-tervnek tűnik – koppints, és beállítom.";
            case BODY: return "⚖️ Ez mérésnek tűnik – koppints, és a Profilba viszem.";
            case ROUTINE: return "📅 Ez edzésnapnak tűnik – koppints, és felveszem.";
            case SLEEP: return "😴 Ez alvásnak tűnik – koppints, és a Profilba jegyzem.";
            case PULSE: return "❤️ Ez nyugalmi pulzusnak tűnik – koppints, és a Profilba jegyzem.";
            case REHAB: return "🩹 Erre van egy megelőző gyakorlatsorom – koppints, és mutatom.";
            default: return "";
        }
    }
}
