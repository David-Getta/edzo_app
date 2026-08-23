package com.edzo.idozito;

/**
 * Testsúly és testzsír egyetlen mondatból: „ma reggel 78,4 kg”, „78 kiló
 * vagyok”, „mérleg: 81,2”, „18% testzsír”.
 *
 * A mérés a legrövidebb életű adat az appban – reggel felállsz a mérlegre, és
 * két számot látsz. Eddig ehhez a Profil képernyőre kellett menni, ott két
 * mezőt kitölteni és menteni; a mondat viszont fél másodperc.
 *
 * A felismerés szándékosan szűkszavú, mert a kilogramm a legterheltebb
 * mértékegység az appban: „80 kg” lehet munkasúly, bevásárlás és testsúly is.
 * Csak KÉT esetben mondunk mérést: ha a mondat kimondja („vagyok”, „mérleg”,
 * „testsúly”), vagy ha a számon és egy napszakon kívül nincs is más benne. Egy
 * félreértett mérés elrontja a súlytrendet, a BMI-t és a kalóriacél-ajánlást
 * is – abból inkább ne legyen bejegyzés, mint rossz.
 */
public final class BodyParse {

    private BodyParse() {}

    /** Életszerű testsúly-határok kilóban. */
    static final double MIN_KG = 30, MAX_KG = 250;
    /** Életszerű testzsír-határok százalékban. */
    static final double MIN_FAT = 3, MAX_FAT = 60;

    /**
     * Körfogatok – a mérőszalag adatai.
     *
     * A derék az egyetlen, aminek önmagában is egészség-jelentése van (a
     * derék/magasság arány), a többit azért mérik, hogy lássák: ami fogy, az
     * a has, ami nő, az a kar. A sorrend a képernyőn és a CSV-ben is ez.
     */
    public static final String[] PART_KEYS = {"waist", "hip", "chest", "thigh", "arm"};
    /** Ugyanaz magyarul, a képernyőre. */
    public static final String[] PART_NAMES = {"Derék", "Csípő", "Mellkas", "Comb", "Kar"};
    /** A mondatban keresett szótövek részenként. */
    private static final String[][] PART_STEMS = {
            // A „bőség" alapalakja is kell: a „derékbőség 82 cm" és a
            // „derékbőségem" a birtokos alakot kereső tővel nem egyezett, és
            // az egész mérés elveszett.
            {"derek", "derekam", "derekboseg", "derekbosege", "derekbosegem",
                    "has", "hasam",
                    "hasboseg", "hasbosege", "haskorfogat"},
            {"csipo", "csipom", "csipoboseg", "csipobosege", "fenek"},
            {"mellkas", "mell", "mellboseg", "mellbosege"},
            {"comb", "combom", "combboseg", "combbosege"},
            {"kar", "karom", "bicepszem", "felkar", "bicepsz"},
    };
    /** Életszerű körfogat-határok centiben. */
    static final double MIN_CM = 15, MAX_CM = 200;

    /** Egy mérés a mondatból. A hiányzó adat 0. */
    public static final class Body {
        public final double kg;
        public final double fatPct;
        /** Körfogatok centiben, a PART_KEYS sorrendjében (0 = nincs). */
        public final double[] cm;

        Body(double kg, double fatPct) { this(kg, fatPct, new double[PART_KEYS.length]); }

        Body(double kg, double fatPct, double[] cm) {
            this.kg = kg; this.fatPct = fatPct;
            this.cm = cm == null ? new double[PART_KEYS.length] : cm;
        }

        public boolean isEmpty() { return kg <= 0 && fatPct <= 0 && !hasCm(); }

        /** Van-e legalább egy körfogat. */
        public boolean hasCm() {
            for (double v : cm) if (v > 0) return true;
            return false;
        }

        /** „78,4 kg  ·  18% testzsír  ·  Derék 84 cm” – az előnézethez. */
        public String label() {
            StringBuilder sb = new StringBuilder();
            if (kg > 0) sb.append(Hu.kg(kg)).append(" kg");
            if (fatPct > 0) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append(Hu.kg(fatPct)).append("% testzsír");
            }
            for (int i = 0; i < cm.length; i++)
                if (cm[i] > 0) {
                    if (sb.length() > 0) sb.append("  ·  ");
                    sb.append(PART_NAMES[i]).append(" ").append(Hu.kg(cm[i])).append(" cm");
                }
            return sb.toString();
        }
    }

    /** Szavak, amelyek kimondják, hogy a saját testsúlyáról van szó. */
    private static final String[] BODY_WORDS = {
            "testsuly", "testsulyom", "sulyom", "suly", "merleg", "merlegen", "merlegre",
            // A MÉRLEGELÉS is mérés: a „reggeli mérlegelés: 88,8 kg"
            // bejegyzésből semmi nem lett – a „mérleg" tő szóhatárt várt.
            "merlegeles", "merlegeltem",
            "vagyok", "lettem", "nyomok", "fogytam", "hiztam", "leadtam", "testzsir",
            // Birtokos és összetett alakok: a szóhatáros keresés miatt a
            // „testzsírom" nem ugyanaz, mint a „testzsír" – a „22% a
            // testzsírom" eddig teljesen elveszett.
            "testzsirom", "testzsira", "zsirszazalek", "testzsirszazalek",
            // Az igekötős alakok külön: a szóhatáros keresés miatt a
            // „lefogytam" nem ugyanaz, mint a „fogytam". Huszonhat valós
            // mérés-mondattal próbálva ezek maradtak ki.
            "lefogytam", "felmentem", "lementem", "felszedtem",
            // A visszahízás is mérés-ige: a „visszahíztam 82-re" eddig
            // elveszett, mert a „híztam" csak szó elején volt meg.
            "visszahiztam", "visszahizott", "visszamentem",
            // A NAPLÓ-FEJLÉC is mérés-kontextus: a „reggeli rutin: 78,2 kg,
            // pulzus 54" súlya eddig elveszett, mert a „rutin" nem volt cue.
            "rutin", "check-in", "checkin", "check in",
            // Az RHR-es tömör naplósor („78,2 kg / 54 rhr / 7,5h alvás")
            // is reggeli mérés-kontextus.
            "rhr",
            // A JELEN IDEJŰ irány is mérés-mondat: a „végre fogyok, 74,2 kg
            // ma" hetvennégy kilója valódi mérés – eddig kiesett, mert a
            // „fogyok" se kimondásnak, se kísérőnek nem számított.
            "fogyok", "hizok",
            // A KÚRA neve is mérés-kontextus: az „a fogyókúra első hete
            // lezárult: -1,8 kg, most 92,7" mai értéke elveszett – a kúra
            // szava mellett a mértékegység nélküli szám nem volt mérés.
            "fogyokura", "fogyokuram", "kura", "dieta", "dietam",
            // A MUTATOTT szám a mérleg száma: a „vízvisszatartás miatt
            // 84 kg-ot mutatott" nyolcvannégye valódi reggeli mérés.
            "mutatott", "mutat",
            // Az ELÉRT cél már mérés: az „elértem a célsúlyom, 72 kg"
            // hetvenkettője a mai súly – a cél szava eddig az egész mondatot
            // elnémította, pedig aki elérte, az épp most állt a mérlegen.
            // (A puszta „a célsúlyom 72 kg" kívánság marad, nem mérés.)
            "elertem",
            // A HELYESBÍTÉS is mérés-mondat: a „bocs, elírtam: 78,2 kg" az
            // aznapi mérés javítása – eddig a bevezető tagmondat miatt az
            // egész kiesett, mert kimondás-szó nem volt mellette.
            "elirtam", "elgepeltem", "javitom", "javitva", "helyesbitek",
            // A KIINDULÓPONT ragozott kilója is testsúly-mondat: a „83,5
            // kilóról indultam januárban, ma 78,2" mai értéke csak akkor
            // kerülhet be, ha a mondat mérésnek látszik.
            "kilorol", "kilobol", "kilotol",
            // A mérés IGÉJE is kimondás: „reggel megmértem magam, 78,4".
            "megmertem", "mertem", "megmerve", "merem",
            // A mérés FŐNEVE is kimondás: a „reggeli mérés: 80,1 kg" eddig
            // teljesen elveszett – a szó miatt a „csak számok maradtak"
            // vizsgálat megbukott, a listán meg nem volt ott.
            "meres", "meresem", "merese", "meresek",
            // A múlt idejű létige is kimondás: a „78,2 kg voltam" ugyanaz a
            // mérés, mint a „78,2 kg vagyok" – egy hosszabb napi
            // összefoglalóban eddig elveszett.
            "voltam", "voltunk"
    };

    /**
     * MÁS emberének (vagy állatának) tagmondata törölve – még a vesszők
     * eltávolítása ELŐTT, mert a későbbi lépések tagmondat-határ nélkül
     * már nem tudnák leválasztani. Az „én 78 kg vagyok, a fiam 32" első
     * fele így megmarad, a fiú súlya nem lesz a felhasználóé.
     */
    private static String dropOthersWeight(String s) {
        return s.replaceAll("(?:(?<=[,;.])|^)[^,;.]*(?<![a-z])"
                + "(?:fiam|lanyom|ferjem|felesegem|parom|gyerek\\w{0,3}"
                + "|baba\\w{0,3}|kutya\\w{0,3}|macska\\w{0,3}"
                + "|anyu|apu|anyam|apam|tesom|nagyi)(?![a-z])[^,;.]*", " ");
    }

    /**
     * A „kg-OS" jelzős alak sosem testsúly.
     *
     * Az „elértem a 100 kg-os fekvenyomást!" száz kilós TESTSÚLYT írt a
     * trendbe – abból, ami a rúdon volt. A saját súlyát senki nem így mondja
     * („80 kg-os vagyok"), a felszerelést és a rekordot viszont mindenki:
     * „20 kg-os súlyzó", „5 kilós kézisúlyzó", „100 kg-os fekvenyomás".
     */
    private static boolean adjectiveKg(String s) {
        return s.matches("(?s).*\\d\\s?-?\\s?(?:kg|kilo)\\s?-?(?:os|s)(?![a-z]).*");
    }

    /**
     * Szavak, amelyektől a mondat biztosan NEM mérés – a súly másé.
     *
     * Mind egész szóként keresve: a rövid szótő máshol elrejtve („húsz”-ban a
     * „hús”) a legmegbízhatóbb módja annak, hogy egy jó mondat elvesszen.
     */
    private static final String[] NOT_BODY = {
            "nyomtam", "emeltem", "huztam", "toltam", "vettem", "vasaroltam", "hoztam",
            // MÁS emelése sem az én mérésem: „a srác 90 kg-ot nyomott ki"
            // kilencven kilós testsúly lett – az én tagmondatom megmarad.
            "nyomott", "nyomta", "kinyomta", "emelt", "huzott",
            // A GYAKORLAT súlya sem testsúly: az „edzés rutin: fekvenyomás
            // 3x10 60 kg" hatvana a rúdon van, nem a mérlegen.
            // A gyakorlat IGÉJE ugyanúgy nem mérés: a „leguggoltam 100-at
            // súly nélkül" mondatban a „súly" szó mérésnek mutatta a
            // százat, és száz kilós testsúly került a trendbe – abból,
            // hogy valaki száz guggolást csinált teher nélkül.
            "leguggoltam", "guggoltam", "leguggolt", "guggolt",
            "kinyomtam", "megnyomtam", "felhuztam", "kihuztam",
            "fekvenyomas", "guggolas", "felhuzas", "holtemeles",
            "huzodzkodas", "tolodzkodas", "fekvotamasz", "kitores",
            "szakitas", "lokes", "vallbol",
            // Célok és becslések: a „70 kg alatt vagyok" nem hetven kiló, a
            // „szeretnék 75 lenni" meg egyáltalán nem mérés. Egy vágyból
            // csinált bejegyzés a trendet is, a BMI-t is elrontaná.
            "alatt", "felett", "folott", "korul", "korulbelul", "kb", "kozel",
            "szeretnek", "akarok", "cel", "celom", "lenni",
            // MÁS súlya nem az enyém: „a fiam 32 kg lett a mérlegen"
            // eddig a felhasználó mérésének számított. A tagmondat-hatókör
            // miatt az „én 78, a fiam 32" első fele megmarad.
            "fiam", "lanyom", "ferjem", "felesegem", "parom", "gyerek",
            "babank", "baba", "kutyam", "kutya", "macska", "macskam",
            "anyu", "apu", "anyam", "apam", "tesom", "nagyi"
    };

    /**
     * Az IDŐ „alatt"-ja nem összehasonlítás.
     *
     * A „70 kg alatt vagyok" tényleg nem mérés – de a „79,2 kg volt a mérleg,
     * futottam 8 km-t 45 perc alatt" mondatban az „alatt" a negyvenöt PERCÉ,
     * és eddig ettől az egész mérés kiesett. Ugyanígy tűnt el a
     * „haskörfogatom 92 cm-ről 88-ra ment le fél év alatt" is.
     */
    private static String maskTimeUnder(String s) {
        return s.replaceAll("(?<![a-z])(perc|ora|orat|mp|masodperc|het|honap|ev|nap)"
                + "\\s+alatt(?![a-z])", "$1 #");
    }

    /**
     * A -ról/-re pár MÁSODIK száma a mai érték.
     *
     * A „haskörfogatom 92 cm-ről 88-ra ment le" és a „testzsír 22-ről 18
     * százalékra" mondatban két szám áll: a régi és a mai. A felismerő eddig
     * az elsőt vette – vagyis pont azt, ami már NEM igaz –, a haskörfogatnál
     * pedig a két szám együtt olyan zavaros maradt, hogy semmi nem lett
     * belőle. A régi értéket kivágjuk, a mai marad a helyén.
     */
    private static String keepTheNewValue(String s) {
        // A KÖRMÉRET változása centiben értendő: a „derékbőség lement
        // 90-ről 86-ra" egyik számán sincs cm, így a maradó új érték sem
        // volt körméretnek olvasható – a mértékegységet visszaírjuk.
        s = s.replaceAll("(derekboseg\\w*|haskorfogat\\w*)([^0-9]{0,20}?)"
                + "(\\d{2,3})\\s?-?r[oó]l(?![a-z])[^0-9]{0,12}?"
                + "(\\d{2,3})\\s?-?r[ae](?![a-z])", "$1 $4 cm");
        s = s.replaceAll(
                "(?<![\\d,.])\\d{1,3}(?:[.,]\\d{1,2})?\\s?(?:cm|centi|kg|kilo|%|szazalek)?"
                        + "\\s?-?r[o\u00f3]l\\b([^0-9]{0,12}?)"
                        // A MÉRTÉKEGYSÉG a két szám között is ott állhat: a
                        // „haskörfogat 92-ről 88 cm-re" nyolcvannyolcasa
                        // eddig nem illeszkedett, és a RÉGI érték maradt a
                        // naplóban – vagyis a fogyás napján egy hízás.
                        + "(\\d{1,3}(?:[.,]\\d{1,2})?\\s?"
                        + "(?:cm|centi|kg|kilo|%|szazalek)?\\s?-?r[ae]\\b)", "$2");
        // A „VOLTAM …, MOST" mondatban is a második szám a mai: a „80 kg
        // voltam 20% zsírral, most 76 kg" nyolcvana a múlté – mégis az
        // került a trendbe. A régi szám (és a régi százalék) kiesik, ha a
        // mondat később „most"-tal folytatódik.
        if (s.matches(".*(?<![a-z])most(?![a-z]).*\\d.*")) {
            s = s.replaceAll("(?<![\\d,.])\\d{1,3}(?:[.,]\\d{1,2})?"
                    + "(\\s?(?:kg|kilo)\\w*\\s+voltam)(?=.*(?<![a-z])most(?![a-z]))",
                    "$1");
            s = s.replaceAll("(?<![\\d,.])\\d{1,2}(?:[.,]\\d)?"
                    + "\\s?%\\s?(zsir\\w*)(?=.*(?<![a-z])most(?![a-z]))", "$1");
        }
        // A múltra utalt „volt" is a régi érték: a „derékbőség 92 cm, két
        // hete még 95 volt" kilencvenöte a két héttel ezelőtti szám – mégis
        // testsúlyként került a naplóba. A múlt-időhatározó és a „volt"
        // közti szám kiesik. Tagmondatra nem támaszkodhatunk (a dropOtherLogs
        // a vesszőket is elhagyja), ezért a távolság a korlát: a marker és a
        // szám közé csak pár rövid szó férhet („még", „csak", „kb") – a
        // „3 hete edzek rendszeresen, ma 78 volt" hetvennyolcasa messze van,
        // az marad.
        s = s.replaceAll("(?<![a-z])(?:\\d+ ?)?"
                + "(?:hete|honapja|napja|eve|hettel|honappal|evvel|nappal|"
                + "tavaly|regen|regebben|korabban|anno)"
                + "(?![a-z])[^,;.\\d]{0,12}?"
                + "\\d{1,3}(?:[.,]\\d{1,2})?\\s?"
                + "(?:kg|kilo\\w*|cm|centi\\w*|%|szazalek)?\\s?volt(?![a-z])", "");
        // A TEGNAPI szám a tegnapi: a „ma reggel 82,3 kg de tegnap 82,9 volt"
        // két száma közül eddig EGYIK sem került a naplóba – a mai mérés is
        // elveszett a tegnapi mellett. A fenti szabály csak a hetekben-
        // hónapokban mért múltat ismerte, pedig a mérleget naponta nézik.
        // Csak akkor vágunk, ha a tegnapi szám ELŐTT áll egy mai mérés –
        // a magában álló „tegnap 82,9 kg volt" mondat marad, ami volt.
        if (s.matches("(?s).*\\d\\s?(?:kg|kilo|cm|centi|%|szazalek).*"
                + "(?<![a-z])(?:tegnap|tegnapelott|multkor)(?![a-z]).*"))
            s = s.replaceAll("(?<![a-z])(?:tegnapelott|tegnap|multkor)(?![a-z])"
                    + "[^,;.\\d]{0,12}?\\d{1,3}(?:[.,]\\d{1,2})?\\s?"
                    + "(?:(?:kg|kilo|cm|centi|%|szazalek)\\w*|volt(?![a-z]))"
                    + "(?:\\s?volt(?![a-z]))?", "");
        return s;
    }

    /** A mondatban rejlő mérés, vagy egy üres Body. */
    public static Body parse(String q) {
        if (q == null) return new Body(0, 0);
        // A kiírt számnév ugyanolyan mérés: „hetvennyolc kiló vagyok". A
        // mérleget sokan hangosan olvassák fel, és úgy is írják le.
        // A maskTimeUnder a SZÁMNÉV-fordítás előtt fut: az „egy hét alatt"
        // hete a digits() után már „7", és a szabály nem ismerné fel benne az
        // időtartamot – az „alatt" pedig összehasonlításnak látszana.
        // A SORTÖRÉS tagmondat-határ: a listásan beírt reggeli adatsor
        // („78,2 kg" új sorban „52 nyugalmi pulzus") a normalizálás után
        // egyetlen szóközös sorrá olvadt, és a pulzus szavai miatt a mérés
        // kiesett. Vesszőre váltva ugyanaz, mint a vesszős beírás.
        q = q.replaceAll("[\\r\\n]+", ", ");
        // A VÉRCUKOR nem testsúly: a „14:20-kor 132-es vércukrot mértem"
        // százharminckét KILÓS mérésként került a trendbe. A vérnyomás
        // perjeles párját a maszk régóta ismeri; az egyszámos vércukor nem.
        // A KETTŐSPONTOS szám óra vagy versenyidő, sosem testsúly: a
        // „6:30-ra már az emelkedőnél voltunk" HARMINC kilós mérésként
        // került a trendbe – a perc fele levált.
        q = q.replaceAll("(?iu)(?<![\\d,.])\\d{1,2}:[0-5]\\d"
                + "(?:\\s?-?r[ae])?(?![\\d])", " ");
        q = q.replaceAll("(?iu)\\d{2,3}\\s?-?[eo]?s?\\s?"
                + "v[eé]rcuko?r\\p{L}*", " ");
        q = q.replaceAll("(?iu)v[eé]rcuko?r\\p{L}*\\s?:?\\s?"
                + "\\d{2,3}(?![\\d,.])", " ");
        // Az ESZKÖZHATÁROZÓS kiló a VÁLTOZÁS mértéke, nem mérés: a „ma reggel
        // 79,2 kg, ez 0,4 kg-mal kevesebb, mint tegnap" hetvenkilenc egész
        // két tizede némán elveszett – a különbség száma mellett a mondat
        // egésze kiesett. A mérleg számát senki nem írja „kg-mal" alakban.
        q = q.replaceAll("(?iu)(?<![\\d,.])\\d{1,3}(?:[.,]\\d{1,2})?\\s?"
                + "(?:kg|kil[oó])\\w*-?(?:mal|vel|lal|el)(?![\\p{L}])", " ");
        // A TÖMÖR napló-sor rövidítései nem kilók: a „futás 10k 52p; kondi
        // 40p; alvás 7h; súly 79,3" sorból NEGYVEN kilós mérés lett – a
        // kondi perceiből –, a valódi hetvenkilenc egész három tized meg
        // elveszett mellőle. A p perc, a h óra, a k kilométer; egyik sem a
        // mérleg száma. (A „kg" nem esik ide: ott betű követi a k-t.)
        q = q.replaceAll("(?iu)(?<![\\d,.\\p{L}])\\d{1,3}(?:[.,]\\d{1,2})?\\s?"
                + "[pkh](?![\\p{L}0-9])", " ");
        // A SZÜLETÉSNAP számai ÉVEK: a „ma volt a születésnapom, 42 lettem,
        // és 42 fekvőtámaszt csináltam" negyvenkét KILÓS mérést írt a
        // trendbe – az életkorból. A „42 éves lettem" alakot az éves szó
        // eddig is védte, a puszta „42 lettem" viszont a mérleg számának
        // látszott. Kimondott kg mellett marad a mérés (a szülinapi mérleg
        // ugyanúgy mérleg).
        if (Foods.norm(q).matches("(?s).*(?<![a-z])(?:szuletesnap|szulinap|"
                + "betoltottem|betoltotte)\\w*.*")
                && !Foods.norm(q).matches("(?s).*(?<![a-z])(?:kg|kilo)\\w*.*"))
            return new Body(0, 0);
        // A TESTKOR életkor, nem testsúly: az „a teszt szerint a testkorom
        // 42 év, pedig csak 35 vagyok" harmincöt KILÓS mérésként került a
        // trendbe. Kimondott kg mellett marad a mérés.
        if (Foods.norm(q).contains("testkor")
                && !Foods.norm(q).matches("(?s).*(?<![a-z])(?:kg|kilo)\\w*.*"))
            return new Body(0, 0);
        // A KILÓ ÉS GRAMM együtt egyetlen mérés: a „84 kilót és 300
        // grammot mutatott" nyolcvannégy egészként ment be – a háromszáz
        // gramm elveszett, pedig aki így mondja, annak pont az számít.
        java.util.regex.Matcher kgm = java.util.regex.Pattern.compile(
                "(?iu)(\\d{2,3})\\s?kil[oó]t?\\p{L}*\\s+([eé]s\\s+)?"
                + "(\\d{2,3})\\s?grammo?t?(?![\\p{L}])").matcher(q);
        if (kgm.find()) {
            int whole = Integer.parseInt(kgm.group(1));
            int gr = Integer.parseInt(kgm.group(3));
            if (gr < 1000)
                q = q.substring(0, kgm.start()) + whole + ","
                        + String.format(java.util.Locale.ROOT, "%02d",
                            (int) Math.round(gr / 10.0)) + " kg"
                        + q.substring(kgm.end());
        }
        // A GRAMMBAN mondott szám sosem testsúly: az „elértem a fehérjecélt,
        // 140 g" száznegyvene fehérje, mégis száznegyven KILÓS mérés lett
        // belőle – egy nap alatt hatvan kilós ugrás a trendben. Testsúlyt
        // senki nem grammban ír. (A „kg" g-je nem esik ide: betű előzi meg.)
        q = q.replaceAll("(?iu)(?<![\\d,.])\\d{1,4}([.,]\\d+)?\\s?"
                + "(?<![\\p{L}])(?:g|gramm)(?![\\p{L}])", " ");
        // A PERJELES HÁRMAS sem mérés: a „180/220/70" makró-sor (fehérje,
        // szénhidrát, zsír grammban) hetven kilós méréssé vált a
        // súlytrendben – az utolsó tagjából. A testsúlyt senki nem írja le
        // két másik szám mögé perjellel.
        q = q.replaceAll("(?<![\\d,.])\\d{1,3}\\s?/\\s?\\d{1,3}\\s?/\\s?"
                + "\\d{1,3}(?![\\d,.])", " ");
        // A HELYESBÍTÉS tagadott száma nem mérés, de nem is némítja el a
        // mondatot: a „78,2 kg volt, nem 87,2" hetvennyolc kilója az igazi –
        // eddig az egész bejegyzés elveszett, vagyis a nap mérése kimaradt
        // egy elütés miatt.
        if (q.matches("(?s).*\\d\\s?kg.*"))
            q = q.replaceAll("(?iu),\\s*nem\\s+\\d{1,3}(?:[.,]\\d+)?"
                    + "\\s?(?:kg|kil\u00f3\\p{L}*|kilo\\p{L}*)?[\\s.!]*$", " ");
        // A „zsír NN%" átírása MÉG a tagmondat-szűrés előtt: a más-napló
        // szűrő a „zsír" tövét étel-szónak nézte, és a „súlyom 80, zsír
        // 18%" zsír-tagmondatát eldobta – mire az alábbi testzsír-szabály
        // sorra került, már nem volt mit átírnia.
        q = q.replaceAll("(?iu)(?<![\\p{L}])zs[ií]r\\s?:?\\s?"
                + "(\\d{1,2}(?:[.,]\\d{1,2})?)\\s?(?:%|sz[aá]zal[eé]k\\w*)",
                "testzsir $1 %");
        q = q.replaceAll("(?iu)(?<![\\p{L}])zs[ií]rm[eé]r\\w*\\s+"
                + "(?:szerint\\s+)?(\\d{1,2}(?:[.,]\\d{1,2})?)\\s?"
                + "(?:%|sz[aá]zal[eé]k\\w*)", "testzsir $1 %");
        // A „MOST N" kg nélkül is a mai súly, ha a mondat kilóban beszél:
        // az „a fogyókúra első hete lezárult: -1,8 kg, most 92,7"
        // kilencvenkét és héttizede eddig elveszett – a kúra tiltószava a
        // mértékegység nélküli számot is elvitte.
        if (Foods.norm(q).matches("(?s).*(?<![a-z])(?:kg|kilo)\\w*.*"))
            q = q.replaceAll("(?iu)(?<![\\p{L}])most\\s+"
                    + "(\\d{2,3}(?:[.,]\\d{1,2})?)(?!\\d)(?![.,]\\d)"
                    + "(?!\\s?(?:kg|kil[oó]|%|sz[aá]zal|perc|km|[oó]ra"
                    + "|l[eé]p[eé]s|kcal|kalori|cm|m(?![\\p{L}])))",
                    "most $1 kg");
        String s = keepTheNewValue(dropOtherLogs(
                Hu.digits(maskTimeUnder(Hu.correction(
                        dropOthersWeight(Foods.norm(q)))))));
        // A KEZELÉS ALATTI „alatt" nem összehasonlítás: a „hormonkezelés
        // alatt híztam 3 kilót, most 68 kg" hatvannyolca valódi mérés –
        // az „alatt" tiltószava eddig az egészet elvitte.
        s = s.replaceAll("(kezeles|kura|terapia|szoptatas|terhesseg|dieta)"
                + "\\s+alatt", "$1 idejen");
        // A VÁRANDÓSSÁG hete nem testsúly: az „a 30. hétben vagyok,
        // hetente 2x úszás" harmincasa HARMINC KILÓ lett a naplóban.
        s = s.replaceAll("(?<![\\d,.])\\d{1,2}\\.?\\s?het(?:en|ben)(?![a-z])", "");
        // A PUSZTA „zsír NN%" is testzsír: a tömör naplósor („súlyom 80,
        // zsír 18%") százaléka eddig elveszett – a százalékjel mondja ki,
        // hogy nem sertészsírról van szó.
        s = s.replaceAll("(?<![a-z])zsir\\s?:?\\s?"
                + "(\\d{1,2}(?:[.,]\\d{1,2})?)\\s?(?:%|szazalek)",
                "testzsir $1 %");
        // A ZSÍRMÉRŐ mutatta százalék is testzsír: az „a zsírmérő szerint
        // 24,8 százalék" eddig elveszett – a zsír töve a műszer nevében ült,
        // a szám pedig kiírt „százalék" szóval állt.
        s = s.replaceAll("(?<![a-z])zsirmer\\w*\\s+(?:szerint\\s+)?"
                + "(\\d{1,2}(?:[.,]\\d{1,2})?)\\s?(?:%|szazalek\\w*)",
                "testzsir $1 %");
        // Az ANGOL rövidítés is testzsír: a „bf 18%" és a „body fat
        // 18,5%" az óra-app sora, és eddig üresen jött vissza. A
        // „testzsír% 17,2" jel-előre-vetett alakja ugyanígy.
        s = s.replaceAll("(?<![a-z])(?:bf|body\\s?fat)\\s?:?\\s?"
                + "(\\d{1,2}(?:[.,]\\d{1,2})?)\\s?%?", "testzsir $1 %");
        s = s.replaceAll("(testzsir\\w*)\\s?%\\s?:?\\s?"
                + "(\\d{1,2}(?:[.,]\\d{1,2})?)", "$1 $2 %");
        // A FORDÍTOTT szórendű testzsír is testzsír: az „az okosmérleg
        // szerint 22,1 a testzsírom" eddig üresen jött vissza, mert a
        // szám a szó ELŐTT áll. Egyenes szórendre írjuk át.
        s = s.replaceAll("(?<![\\d,.])(\\d{1,2}(?:[.,]\\d{1,2})?)\\s?"
                + "(?:%|szazalek)?\\s+a\\s+(testzsir\\w*|zsirszazalek\\w*)",
                "$2 $1");
        // A KÖRÜL a mérleg ingadozását mondja, nem tiltószó: a „stagnál a
        // súlyom 82 körül" nyolcvankét kiló – eddig elveszett.
        s = s.replaceAll("(?<=\\d)\\s?korul(?![a-z])", "");
        // A TESTVÍZ százaléka nem testzsír: a „testzsír 18,2 / víz 55%"
        // zsírja ötvenöt százalék lett – az okosmérleg víz-rovata kiesik.
        s = s.replaceAll("(?<![a-z])viz\\w*\\s?:?\\s?\\d{1,2}"
                + "(?:[.,]\\d{1,2})?\\s?%", " ");
        // Az ÁTLÉPETT HATÁR utáni szám a mai mérés: a „végre átléptem a
        // 80-as határt lefelé, 79,8" hetvenkilenc egész nyolc – a küszöb
        // száma nem mérés, azt eldobjuk.
        s = s.replaceAll("atlept\\w*\\s+a\\s+\\d{2,3}\\s?-?[ae]s\\s+"
                + "hatart(?:\\s+lefele|\\s+felfele)?,?\\s*", "sulyom ");
        // Az „ALATT VAGYOK" küszöbe sem mérés: a „végre 75 alatt vagyok,
        // 74,6" hetvennégy egész hat – a küszöb száma kiesik.
        s = s.replaceAll("(?<![\\d,.])\\d{2,3}\\s?(?:ala|alatt|fole|folott)"
                + "\\s+(?:vagyok|mentem|kerultem|ertem)\\W*", "sulyom ");
        // A MUNKA/PIHENŐ számpár nem testsúly: a „Reggeli rutin: 4 kör
        // 45/15" negyvenöt kilós méréssé vált a rutin-fejléc miatt. (A
        // vérnyomás 160/95-e ugyanígy kiesik.)
        s = s.replaceAll("(?<![\\d,.])\\d{1,3}\\s?/\\s?\\d{1,3}(?![\\d,.])",
                " ");
        // A KÜSZÖB száma kiesik, ha valódi mérés is áll mellette: a
        // „mérleg megint 80 fölött, 80,3" nyolcvanhármadából semmi nem
        // lett, mert az összehasonlítás elnémította a tagmondatot.
        s = s.replaceAll("(?<![\\d,.])\\d{2,3}(?:[.,]\\d)?\\s?"
                + "(?:ala|alatt|fole|folott|felett)(?![a-z])(?=[^0-9]*\\d)", " ");
        // A NAP KÉT MÉRÉSE közül a KÉSŐBBI a mai adat: a „ma reggel még
        // 79,8 volt, este már 79,2" és a „hétfőn 80,5, ma 79,4" egyaránt
        // üresen jött vissza – az első számot a múlt idő elvitte, a
        // másodikhoz meg nem tartozott mérés-szó.
        {
            java.util.regex.Matcher tm = java.util.regex.Pattern.compile(
                    "(?<![\\d,.])(\\d{2,3}(?:[.,]\\d{1,2})?)[^0-9]{1,30}?"
                    + "(?<![a-z])(?:ma|most|este|mar|estere)(?![a-z])"
                    + "[^0-9]{0,12}?(\\d{2,3}(?:[.,]\\d{1,2})?)(?![\\d,.])")
                    .matcher(s);
            if (tm.find())
                s = s.substring(0, tm.start()) + "sulyom " + tm.group(2)
                        + s.substring(tm.end());
        }
        // A PLATÓ fordulatai is mérések: a „78-on állok", a „beálltam
        // 78-ra" és a „tartom a 78-at" eddig üresen jött vissza.
        s = s.replaceAll("(?<![\\d,.])(\\d{2,3}(?:[.,]\\d{1,2})?)"
                + "\\s?-?[eoa]?n\\s+allok(?![a-z])", "sulyom $1");
        s = s.replaceAll("(?<![a-z])beall\\w*\\s+(\\d{2,3}(?:[.,]\\d{1,2})?)"
                + "\\s?-?r[ae](?![a-z])", "sulyom $1");
        s = s.replaceAll("(?<![a-z])tartom\\s+a\\s+(\\d{2,3})"
                + "\\s?-?[ae]?t(?![a-z])", "sulyom $1");
        // A RAJTAM mért szám az enyém, akárki olvasta le a mérleget: az
        // „az orvosnál 84 kg-ot mértek rajtam" eddig másénak látszott.
        s = s.replaceAll("mertek\\s+rajtam", "mertem");
        // A RENDELŐBEN mért szám is az enyém: a „a recepción mérték:
        // 78,4 kg" mérése harmadik személyű igén ült, és elveszett.
        s = s.replaceAll("(?<![a-z])mertek\\s*:?\\s*(?=\\d)", "mertem ");
        // A TÁRGYRAGOS mérés kg nélkül is súly: az „én 84-et mértem ma"
        // mértékegység híján eddig elveszett.
        s = s.replaceAll("(?<![\\d,.])(\\d{2,3})\\s?-?[ae]?t\\s+mertem",
                "$1 kg-ot mertem");
        // A HÁTRALÉVŐ kiló a cél távolsága, nem a mérleg száma: a „90 kg-ról
        // indultam, most 84,5, már csak 4,5 kiló a cél" négy és felese az
        // egyetlen kilóval írt szám volt, és a felismerő rajta akadt el –
        // a valódi 84,5-ös mérés is elveszett vele.
        s = s.replaceAll("(?<![a-z])(?:mar\\s+)?(?:csak\\s+|meg\\s+)?"
                + "\\d[\\d,.]{0,5}\\s?kil[oó]\\w*\\s+(?:van\\s+)?"
                + "(?:meg\\s+|hatra\\s+)?(?:a\\s+)?cel\\w*[^,;.]*", " ");
        s = s.replaceAll("(?<![a-z])\\d[\\d,.]{0,5}\\s?kil[oó]\\w*\\s+"
                + "hianyzik[^,;.]*", " ");
        // A VÁLTOZÁS mondatában a MÁSODIK szám a mai érték: a „derékbőség
        // lement 90-ről 86-ra" a régi számot hagyta meg (vagy semmit).
        s = s.replaceAll("(?<![\\d,.])(\\d{2,3}(?:[.,]\\d{1,2})?)\\s?-?r[o]l"
                + "(?![a-z])[^0-9]{0,12}?(\\d{2,3}(?:[.,]\\d{1,2})?)"
                + "\\s?-?r[ae](?![a-z])", "$2");
        // A FOGYÓKÚRA kiindulópontja múlt, a „ma" utáni szám a mérés: a
        // „83-ról indultam januárban, ma 76" hetvenhat kiló – eddig
        // egyik szám sem lett mérés, mert test-szó nincs a mondatban.
        java.util.regex.Matcher jm = java.util.regex.Pattern
                .compile("(?<![\\d,.])(\\d{2,3})\\s?-?rol\\s+indultam")
                .matcher(s);
        if (jm.find()) {
            int start = Integer.parseInt(jm.group(1));
            if (start >= 40 && start <= 200)
                s = s.substring(0, jm.start()) + "sulyom"
                        + s.substring(jm.end());
        }
        if (s.isEmpty()) return new Body(0, 0);
        // A tiltó szó csak a SAJÁT tagmondatát viszi el: az „elértem a
        // célsúlyom, 72 kg" hetvenkettője valódi mérés – a CÉL az első
        // tagmondatban áll, és eddig az egész mondatot elnémította. A „78 kg
        // lettem, végre 80 alá mentem" nyolcvana ugyanígy: a küszöb a másik
        // tagmondaté, a hetvennyolc a mérleg száma. Ha minden tagmondat
        // tiltott, az egész mondat az – a „a cél 75 kg" továbbra sem mérés.
        // A VÁGY a saját tagmondata, vessző nélkül is: a „60 kg vagyok
        // szeretnék 65-öt" mondatot – ahogy a legtöbben írják – az egész
        // mondatra kiterjedő tiltás elnémította, és a MAI mérés is elveszett
        // a vággyal együtt. A tiltó szó elé határt teszünk, így a mérés
        // tagmondata megmarad.
        // A CÉL szava elé is határ kerül: a „reggel 79,1 kg. Cél: 75 alá
        // szeptember végéig" mondathatára a feldolgozásban szóközzé olvadt,
        // a cél tiltószava így az egész szövegre szólt – és a valódi mérés
        // is kiesett vele.
        s = s.replaceAll("(?<=[a-z0-9]) (?=(?:szeretnek|szeretnem|akarok|"
                + "celom|celsuly)(?![a-z])|cel[ :])", ", ");
        // A KÜSZÖB-tagmondat elé is: a „reggeli súly: 90,05 kg, első
        // alkalommal 90 fölött" mérése eddig elveszett – a mondathatár a
        // feldolgozásban szóközzé olvadt, és a „fölött" az egészre szólt.
        s = s.replaceAll("(?<=[a-z0-9]) (?=\\d{2,3}\\s?"
                + "(?:ala|alatt|folott|felett)(?![a-z]))", ", ");
        // A „MOST N" elé is határ kerül: a „fogyókúra: most 92,7 kg" mai
        // értéke a kúra tiltószavával egy tagmondatban ült, és elveszett.
        s = s.replaceAll("(?<=[a-z0-9]) (?=most\\s+\\d)", ", ");
        boolean anyBlocked = adjectiveKg(s) || liftStem(s);
        for (String n : NOT_BODY) if (word(s, n)) { anyBlocked = true; break; }
        if (anyBlocked) {
            StringBuilder keepB = new StringBuilder();
            for (String cl : s.split("[,;.](?!\\d)")) {
                boolean bad = adjectiveKg(cl) || liftStem(cl);
                for (String n : NOT_BODY) if (word(cl, n)) { bad = true; break; }
                if (bad) continue;
                if (keepB.length() > 0) keepB.append(", ");
                keepB.append(cl.trim());
            }
            s = keepB.toString().trim();
            if (s.isEmpty()) return new Body(0, 0);
        }
        // A két kapu közül legalább az egyiknek nyitva kell lennie.
        // A MEG NEM TÖRTÉNT mérés nem mai adat: a „ma nem mértem meg
        // magam, de tegnap 79 volt" hetvenkilencet a MAI napra írta a
        // súlytrendbe – pedig a mondat épp azt mondja, hogy ma nem állt
        // mérlegre. A tegnapi szám a tegnapi naplóé, azt nem írjuk át.
        if (s.matches("(?s).*(?<![a-z])nem\\s+(?:mertem|merlegeltem|"
                + "alltam)\\s?(?:meg|ra)?\\s*(?:magam|merlegre)?"
                + "(?![a-z]).*")) return new Body(0, 0);
        boolean said = hasBodyWord(s);
        // A KILÓBAN mondott KÜLÖNBSÉG is testsúly-kontextus: a „reggel
        // 76,8 kg, ez már 5 kiló mínusz az induláshoz képest" méréséből
        // semmi nem lett – testsúly-szó nincs a mondatban, a „csak számok
        // maradtak" vizsgálat pedig a mínusz szavain bukott meg. Aki
        // kilóban mond különbséget, az a súlyáról beszél. A különbség
        // száma (5) a súly-sáv alatt van, így nem lehet belőle mérés.
        if (!said && s.matches("(?s).*(?<![a-z0-9])(?:kg|kilo)\\w*\\s+"
                + "(?:minusz|plusz|kevesebb|tobb|konnyebb|nehezebb)"
                + "(?![a-z]).*")) said = true;

        double[] cm = circumferences(s);
        // A tejtermék-szót a NYERS mondatban keressük: a feldolgozás a
        // kefir tagmondatát már eldobhatta, mire a százalékhoz érünk.
        double fat = bodyFat(s, Foods.norm(q).matches(
                ".*(?<![a-z])(tej|kefir|joghurt|turo|sajt|tejfol|tejszin"
                + "|kakao)\\w*.*"));
        // A kimondott testzsír-százalék maga is testről szóló mondat: a
        // „78 kg 18% zsír" mondatban egyik szó sem szerepelt a listán, pedig
        // aki százalékban zsírt ír, az magáról beszél.
        if (!said && fat > 0 && (word(s, "zsir") || word(s, "zsirom"))) said = true;
        // A megnevezett testrész is kimondás: „derék 84 cm" ugyanolyan mérés,
        // mint a „78 kg vagyok" – csak mérőszalaggal.
        boolean anyCm = false;
        for (double v : cm) if (v > 0) anyCm = true;
        if (!said && anyCm) said = true;
        if (!said && !onlyNumbersLeft(s)) return new Body(0, 0);
        double kg = weight(s, fat, cm);
        return new Body(kg, fat, cm);
    }

    /**
     * A MÁS naplóba tartozó tagmondatok le: „78,4 kg, aludtam 7 órát".
     *
     * A reggeli három adat egy mondatban érkezik – súly, alvás, pulzus –, és
     * eddig a mérés esett ki közülük: az alvás-tagmondat szavai miatt a
     * „csak számok maradtak" vizsgálat megbukott, a mondatban pedig nem volt
     * kimondott mérés-szó. A tagmondat a magyar mondat természetes határa,
     * ezért itt vágunk.
     */
    private static String dropOtherLogs(String s) {
        if (s.indexOf(',') < 0 && s.indexOf(';') < 0 && s.indexOf('.') < 0) return s;
        StringBuilder keep = new StringBuilder();
        boolean dropped = false, prevOther = false;   // súlyzós tagmondat volt-e
        int parts = 0;
        // A vessző magyarul tizedesjel is: a „78,4" NEM két tagmondat.
        for (String part : s.split("[,;.](?!\\d)")) {
            if (!part.trim().isEmpty()) parts++;
            boolean other = false;
            for (String w : new String[]{"alud", "alvas", "pulzus", "rhr", "nyugalmi",
                    "ebredtem", "keltem", "fekudtem",
                    // A MOZGÁS és az ÉTKEZÉS tagmondata ugyanígy másé: a
                    // „tegnap este 3 pohár bort ittam, ma reggel 79,8 kg"
                    // méréséből eddig semmi nem lett, mert a bor szavai
                    // miatt a „csak számok maradtak" vizsgálat megbukott.
                    // A REGGELI szándékosan hiányzik: a „reggeli mérés:
                    // 80,1 kg" épp hogy mérés.
                    "futas", "futottam", "edzes", "edzettem", "km", "lepes",
                    "perc", "ittam", "ettem", "ebed", "vacsora", "uzsonna",
                    "kcal", "kalori"})
                // Szóhatárról, igekötővel: a „megettem" étkezés, a „78 kg
                // LETTEM" viszont mérés – a puszta contains() a „lettem"
                // belsejében is megtalálta az „ettem"-et, és a mérés
                // tagmondata étkezésként esett ki.
                if (part.matches(".*(?<![a-z])(?:meg|le|be|fel|el)?" + w + ".*")) {
                    other = true; break;
                }
            // A SOROZAT tagmondata is másé: a „körfogatok és súlyok: derék
            // 84, guggolás 3x5 100" száza a rúdon van, nem a mérlegen –
            // eddig száz kilós MÉRÉS lett belőle a súlytrendben.
            if (part.matches(".*\\d\\s?x\\s?\\d.*")) other = true;
            // A MEGNEVEZETT étel tagmondata is az étrendé: a „ma reggel:
            // 40 perc futás, zuhany, zabkása fahéjjal, 79,1 kg" zabkásája
            // miatt az egész mérés elveszett – a „csak számok maradtak"
            // vizsgálat a kása szavain bukott meg. A CENTIVEL vagy kilóval
            // írt tagmondat viszont mérés marad: a „38 cm comb" a comb
            // körfogata, nem csirkecomb.
            if (!other && !part.contains("cm") && !part.contains("centi")
                    && !part.contains("kg") && !part.contains("kilo")
                    && !Foods.matches(java.util.Arrays.asList(Foods.ALL),
                            part).isEmpty()) other = true;
            // A GYAKORLATNÉV tagmondata a súlyzós naplóé: a „vállból nyomás
            // ma csak 40 kg ment, fáradt voltam" negyvenese a rúdon van, nem
            // a mérlegen – a „voltam" miatt mégis negyven kilós mérés lett
            // belőle a súlytrendben. A CENTIVEL írt tagmondat marad: a
            // „vádli 38 cm" körfogat, hiába gyakorlatnév is a vádli.
            boolean lift = part.matches(".*\\d\\s?x\\s?\\d.*");
            if (!part.contains("cm") && !part.contains("centi")
                    && StrengthParse.nameIn(part) != null) lift = true;
            if (!other && lift) other = true;
            // A CSUPASZ SZÁM az eldobott tagmondat FOLYTATÁSA: az „új PR
            // fekvenyomásban, 100 kg, és közben 82 kg vagyok" száza a rúdon
            // van – a vessző viszont külön tagmondatba tette, a
            // gyakorlatnév nélküli „100 kg" pedig átcsúszott a rostán, és
            // száz kilós TESTSÚLY került a trendbe. Szó nélküli tagmondat
            // nem kezd új gondolatot.
            if (!other && prevOther
                    && part.matches("\\s*\\d{1,3}([.,]\\d{1,2})?\\s*"
                        + "(kg|kilo\\w*)?\\s*")) other = true;
            // Csak a SÚLYZÓS tagmondat folytatódhat így: az „aludtam 8 órát,
            // 78 kg" hetvennyolca valódi mérés, ott a szomszéd tagmondat
            // egészen másról szól.
            prevOther = lift;
            // A SZÁM NÉLKÜLI, mérés-szót sem tartalmazó tagmondat nem szólhat
            // bele: a „Ma pihenőnap volt. 78,9 kg reggel." első mondatában
            // egyetlen olyan szó van („pihenőnap"), amit a „csak számok
            // maradtak" vizsgálat nem ismer – és emiatt az EGÉSZ bejegyzésből
            // semmi nem lett, a reggeli mérleg száma is elveszett. Amiben
            // nincs szám és nincs mérés-szó, abban mérés sincs.
            // Kivétel a KÖRÜLMÉNY szava: a láz, a várandósság és a kezelés
            // tagmondatában sincs szám, a szomszéd tagmondat számát mégis
            // ők magyarázzák („hőemelkedésem van, 37,8").
            if (!part.matches("(?s).*\\d.*") && !hasBodyWord(part)
                    && !part.matches("(?s).*(laz|hoemelked|homerseklet|beteg"
                        + "|terhes|varandos|szoptat|kezeles|kura|terapia"
                        + "|dieta).*")) continue;
            if (other) { dropped = true; continue; }
            if (keep.length() > 0) keep.append(' ');
            keep.append(part.trim());
        }
        String out = keep.toString().trim();
        // Ha MINDEN tagmondat másik naplóé volt, az egész bejegyzés az: a
        // „a spinning órát leadtam 55 percben, a pulzusátlag 148 volt"
        // mondatból – miután a perc és a pulzus tagmondata is kiesett – a
        // teljes szöveg jött vissza, és a pulzusátlagból 148 kilós mérés
        // lett a súlytrendben. Az EGYETLEN tagmondatos sor viszont marad:
        // a „78,2 kg / 54 rhr / 7,5h alvás" perjeles sora egyben egy
        // tagmondat, és a szám-szűrők enélkül is elbírnak vele.
        if (out.isEmpty()) return dropped && parts > 1 ? "" : s;
        // A MÁSIK napló folytatása nem mérés. A „nyugalmi pulzus reggel 47,
        // este 62" hatvankettője az esti PULZUS – eddig hatvankét kilós
        // méréssé vált a súlytrendben. Ha a mondatból eldobtunk egy másik
        // naplóba tartozó tagmondatot, a maradék csak akkor mérés, ha ki is
        // mondja: mértékegység, mérés-szó vagy tizedesjegy (a mérleg így ír).
        if (dropped && !out.matches(".*(kg|kilo|kila|cm|centi|szazalek|testzsir|suly|"
                + "merleg|mertem|megmertem|vagyok|lettem|%|\\d[.,]\\d).*"))
            return "";
        return out;
    }

    /**
     * Mértékegységgel kimondott körfogat: „comb 58 cm".
     *
     * Az útbaigazítónak kell: a comb, a mell és a kar EGYSZERRE testrész és
     * étel, és az étel-felismerő hamarabb szólal meg. Kiírt centiméterrel
     * viszont nincs kétség – csirkecombot senki nem mér mérőszalaggal.
     */
    public static boolean girthWithUnit(String q) {
        if (q == null) return false;
        String s = Foods.norm(q);
        if (!s.contains("cm") && !s.contains("centi")) return false;
        return parse(q).hasCm();
    }

    /**
     * Testzsír: „18% testzsír”, „testzsír 18”, „18 százalék”.
     *
     * A puszta százalék is elfogadható: ilyen mondatban más százalékos adat
     * nem szokott szerepelni.
     */
    private static double bodyFat(String s, boolean tejtermek) {
        // A TEJTERMÉK százaléka zsírtartalom, nem testzsír: a „kefir
        // Danone, 3%" hármasa testzsír-mérésként került a naplóba. Ha a
        // mondatban tejtermék áll és a zsír szó nincs kimondva, a puszta
        // százalék nem mérés.
        if (!tejtermek || s.contains("zsir")) {
            java.util.regex.Matcher pm = java.util.regex.Pattern
                    .compile("(\\d{1,2}([.,]\\d)?)\\s?(%|szazalek)").matcher(s);
            while (pm.find()) {
                double v = num(pm.group(1));
                if (v >= MIN_FAT && v <= MAX_FAT) return v;
            }
        }
        java.util.regex.Matcher m;
        m = java.util.regex.Pattern.compile("testzsir\\w*\\s?:?\\s?(\\d{1,2}([.,]\\d)?)")
                .matcher(s);
        if (m.find()) {
            double v = num(m.group(1));
            if (v >= MIN_FAT && v <= MAX_FAT) return v;
        }
        return 0;
    }

    /**
     * Testsúly kilóban – a testzsírként már elhasznált számot kihagyva.
     *
     * A „78,4 kg és 18% testzsír” mondatban a 18 nem lehet másodszor is súly;
     * a sáv (30–250) ezt magától is kizárja, de a fordított sorrendű mondat
     * („18% testzsír, 78,4”) miatt a kihagyás akkor is kell.
     */
    private static double weight(String s, double fat, double[] cm) {
        // A VÁLTOZÁS mondatában a MÁSODIK szám a mai súly: a „80-ról 76-ra
        // fogytam" mai értéke hetvenhat, nem nyolcvan. Eddig a régi súly
        // került a trendbe – vagyis a fogyás napján egy súlygyarapodás.
        java.util.regex.Matcher ch = java.util.regex.Pattern
                .compile("(\\d{2,3}(?:[.,]\\d{1,2})?)\\s?-?r[oó]l\\b[^0-9]{0,12}?"
                        + "(\\d{2,3}(?:[.,]\\d{1,2})?)\\s?-?r[ae]\\b").matcher(s);
        if (ch.find()) {
            double v = num(ch.group(2));
            if (v >= MIN_KG && v <= MAX_KG && !(fat > 0 && Math.abs(v - fat) < 0.001)) return v;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                // A szám két oldalán számjegy-határ kell. Enélkül a minta a
                // HOSSZABB szám elejét is elkapta: az „1500" első három
                // jegyéből százötven kiló lett, a „10000"-ből száz – vagyis
                // egy elgépelt szám nem hibaüzenetet adott, hanem egy
                // hihető, de hamis mérést a súlytrendbe.
                .compile("(?<![\\d.,])(\\d{1,3}([.,]\\d{1,2})?)(?![\\d.,]?\\d)"
                        + "\\s?-?\\s?(kg|kilogramm|kilo|kila)?").matcher(s);
        while (m.find()) {
            double v = num(m.group(1));
            if (v < MIN_KG || v > MAX_KG) continue;
            if (fat > 0 && Math.abs(v - fat) < 0.001) continue;
            // A körfogatként már elhasznált szám nem lehet másodszor kiló:
            // a „derék 84" nyolcvannégy centi, nem nyolcvannégy kiló.
            boolean used = false;
            for (double c : cm) if (c > 0 && Math.abs(v - c) < 0.001) used = true;
            if (used) continue;
            // Százalékjel után álló szám sosem kiló – még akkor sem, ha
            // testzsírnak túl nagy volt („80% testzsír”). Az elgépelt
            // százalékból nyolcvan kiló lenne. A centiméter ugyanígy: a
            // „180 cm és 80 kg vagyok" mondatban a magasság áll elöl, és a
            // testsúly sávjába is beleesik.
            // A MÉRETLEN testrész száma sem kiló: a „combom 58 cm, vádli 38"
            // harmincnyolcasa a vádli körfogata – a naplóba viszont
            // harmincnyolc kilós mérésként került, egy felnőtt súlytrendjébe.
            // (Ezekhez a testrészekhez nincs saját mező, de attól még nem a
            // mérleg száma áll mellettük.)
            if (s.substring(0, m.start()).trim().matches(".*(?:vadli|boka|nyak"
                    + "|csuklo|alkar|labszar|labfej|fejkorfogat)\\w*\\s*[:=-]?"))
                continue;
            String rest = s.substring(m.end()).trim();
            if (rest.startsWith("%") || rest.startsWith("szazalek")) continue;
            if (rest.startsWith("cm") || rest.startsWith("centi")) continue;
            // A „-kor" időpont vagy körszám, sosem kiló: a „45-kor" és a
            // „45 kör" negyvenöt kilós mérésként került volna a súlytrendbe.
            if (rest.startsWith("kor") || rest.startsWith("-kor")) continue;
            // A „-ról" a KIINDULÓPONT ragja, sosem a mai érték: a „83,5
            // kilóról indultam januárban, ma 78,2" első száma a januári súly
            // – a mai a mondat végén áll, és eddig a régi nyerte a trendet.
            if (rest.startsWith("rol") || rest.startsWith("-rol")
                    || rest.startsWith("bol") || rest.startsWith("-bol")) continue;
            // A KÜSZÖB sem a mai érték: a „végre lement a súlyom 80 alá,
            // 79.6 kg" nyolcvanasa a lélektani határ, nem a mérés – mégis
            // nyolcvan kiló került a trendbe a 79,6 helyett. Ugyanígy a
            // „fölé": az átlépett határ száma sosem a mérleg száma.
            if (rest.startsWith("ala") || rest.startsWith("-ala")
                    || rest.startsWith("fole") || rest.startsWith("folott")) continue;
            // A LÁZ nem testsúly: a „38 fokos lázam van" harmincnyolca
            // beleesik a súlysávba, és eddig harmincnyolc kilós méréssé vált
            // a trendben – pont egy olyan napon, amikor a felhasználó beteg.
            if (rest.startsWith("fok")) continue;
            // Láz-szó mellett a HŐMÉRSÉKLET-tartomány sem testsúly: a „beteg
            // vagyok, lázam van 38,5" harmincnyolc és fél kilós mérésként
            // került a súlytrendbe – pont egy olyan napon, amikor a
            // felhasználó beteg, és senki nem áll mérlegre.
            if (v >= 35 && v <= 42.5 && (s.contains("laz") || s.contains("hoemelkedes")
                    || s.contains("homerseklet") || s.contains("lazas"))) continue;
            // Az ÉLETKOR sem testsúly: a „férfi vagyok, 34 éves, 182 cm"
            // harmincnégyese az évek száma – eddig harmincnégy kilós mérésként
            // került a súlytrendbe, egy bemutatkozó mondatból.
            if (rest.startsWith("eves") || rest.startsWith("ev ")
                    || rest.equals("ev") || rest.startsWith("evesen")) continue;
            // Az IDŐ és a TÁV sem kiló. Az „este 45 perc jóga, aztán 78,9 kg
            // a mérlegen" negyvenöt PERCE lett a testsúly – a valódi mérés
            // pedig, ami ott állt a mondat másik felében, elveszett.
            for (String u : new String[]{"perc", "ora", "mp", "masodperc", "km", "lepes",
                    // A KONYHAI mértékegység sem testsúly: a „zabkása 60 g"
                    // hatvanas száma az adag, nem a mérleg száma.
                    "g ", "gramm", "dkg", "dl", "ml", "liter",
                    // Az uszodai HOSSZ sem kiló: a „lementem 30 hosszt"
                    // harmincasa a leúszott hosszak száma – a „lementem" ige
                    // mégis mérés-mondatnak mutatta, és harminc kilós mérés
                    // került tőle a súlytrendbe.
                    "hossz"})
                if (rest.startsWith(u)) { rest = "#"; break; }
            if (rest.equals("#")) continue;
            // Az IZOMTÖMEG nem a testsúly. A mérleg ugyanabban a sorban írja
            // ki mindkettőt, és a „testzsír 19,5%, izomtömeg 62 kg" hatvankét
            // kilós méréssé vált a súlytrendben – nyolcvan helyett.
            String head = s.substring(0, m.start());
            boolean other = false;
            for (String w : new String[]{"izomtomeg", "izom tomeg", "izomsuly",
                    "csonttomeg", "csont tomeg", "zsirtomeg", "zsir tomeg",
                    "zsigeri", "vizmennyiseg", "testviz"}) {
                int p = head.lastIndexOf(w);
                // Csak akkor az övé a szám, ha semmi más nem áll közöttük.
                if (p >= 0 && head.substring(p + w.length()).matches("[\\s:=-]*")) other = true;
            }
            if (other) continue;
            return v;
        }
        return 0;
    }

    /** Hány testrész illeszkedik ebben a szórendben? A többség dönt. */
    private static int styleCount(String s, java.util.regex.Pattern[][] pats) {
        int n = 0;
        for (java.util.regex.Pattern[] row : pats) {
            for (java.util.regex.Pattern p : row)
                if (p.matcher(s).find()) { n++; break; }
        }
        return n;
    }

    /**
     * Körfogatok: „derék 84 cm”, „csípő: 96”, „84 cm derék”.
     *
     * A testrész neve KÖTELEZŐ – centiméterből magasság is lehet, meg a
     * konyhapult hossza is. A szám a név után vagy előtte állhat, de csak
     * közvetlenül: a köztük álló szó (a mértékegységen és a kettősponton
     * kívül) már más állítás.
     */
    private static double[] circumferences(String s) {
        double[] out = new double[PART_KEYS.length];
        boolean numberFirst = styleCount(s, PART_BEFORE) > styleCount(s, PART_AFTER);
        for (int i = 0; i < PART_STEMS.length; i++)
            for (int j = 0; j < PART_STEMS[i].length; j++) {
                if (out[i] > 0) break;
                // A felsorolás SZÓRENDJE egységes, és a mondat egészéből
                // derül ki: a „92 cm derék, 100 cm csípő, 38 cm comb"
                // számmal kezd, a „derék 84 cm, csípő 95 cm" a testrésszel.
                // Testrészenként dönteni hibás volt: mindkét alak illeszkedik
                // a felsorolás közepén, és minden érték egyet csúszott.
                java.util.regex.Matcher m = numberFirst
                        ? PART_BEFORE[i][j].matcher(s) : PART_AFTER[i][j].matcher(s);
                if (m.find()) { out[i] = inRange(num(m.group(1))); continue; }
                m = numberFirst
                        ? PART_AFTER[i][j].matcher(s) : PART_BEFORE[i][j].matcher(s);
                if (m.find()) out[i] = inRange(num(m.group(1)));
            }
        return out;
    }

    /**
     * A testrész-minták előre lefordítva.
     *
     * A mérés-mező is minden leütésre újrakérdezi a felismerőt, ötven
     * reguláris kifejezést fordítani karakterenként fölösleges munka.
     */
    private static final java.util.regex.Pattern[][] PART_AFTER = compile(true),
            PART_BEFORE = compile(false);

    private static java.util.regex.Pattern[][] compile(boolean after) {
        java.util.regex.Pattern[][] out = new java.util.regex.Pattern[PART_STEMS.length][];
        for (int i = 0; i < PART_STEMS.length; i++) {
            out[i] = new java.util.regex.Pattern[PART_STEMS[i].length];
            for (int j = 0; j < PART_STEMS[i].length; j++) {
                String stem = PART_STEMS[i][j];
                // A „körfogat" szó közbeékelődhet („haskörfogat 92",
                // „derék körfogat: 84"), a kilós szám viszont nem körfogat –
                // a „bicepsz 20 kg" súlyzó, nem mérőszalag.
                out[i][j] = java.util.regex.Pattern.compile(after
                        // A szám után álló írásjel nem folytatás: a „derék 84,
                        // csípő 95" felsorolásában a vessző elválaszt, nem
                        // tizedesjegyet nyit. Ezért csak a SZÁMMAL folytatódó
                        // pont és vessző zárja ki a találatot – enélkül az
                        // első körfogat kiesett, és a nyolcvannégy centiből a
                        // súly-felismerőnél nyolcvannégy kiló lett.
                        // A birtokos rag a szó VÉGÉN áll, és a mérés-mondat
                        // majdnem mindig birtokos: a „derékbőségem 82 cm"
                        // eddig teljesen elveszett, mert a tő után betű állt.
                        ? "(?<![a-z])" + stem + "(?:em|ed|e|unk|etek|uk)?"
                                + "(?:\\s?korfogat\\w*)?(?![a-z])\\s?:?\\s?"
                        // A TÖBBES jelzős szám sem körfogat: a „bicepsz
                        // 21-esek 3 kör" a huszonegyes ismétlés-séma neve,
                        // nem huszonegy centis kar.
                                + "(\\d{1,3}([.,]\\d)?)(?!\\d|[.,]\\d|\\s?kg|\\s?-?[oae]sek)"
                                + "\\s?(cm|centi\\w*)?"
                        : "(\\d{1,3}([.,]\\d)?)\\s?(cm|centi\\w*)\\s?"
                                + "(?<![a-z])" + stem + "(?![a-z])");
            }
        }
        return out;
    }

    /** Életszerű körfogat, vagy 0. */
    private static double inRange(double v) {
        return v >= MIN_CM && v <= MAX_CM ? v : 0;
    }

    /** Kimondott testsúly-szó a mondatban (egész szóként). */
    private static boolean hasBodyWord(String s) {
        for (String w : BODY_WORDS) if (word(s, w)) return true;
        // A RAGOZOTT mérés-szó ugyanaz a szó: az „a reggeli MÉRÉSKOR
        // 76,2 kg" bejegyzéséből semmi nem lett, mert a lista csak a
        // ragtalan „mérés" alakot ismerte. A mérés töve elég hosszú
        // ahhoz, hogy a rag ne tegye kétértelművé.
        return s.matches("(?s).*(?<![a-z])(?:meres|merés|merlegel|merkozes)"
                + "\\w*.*")
                && !s.matches("(?s).*(?<![a-z])merkozes\\w*.*");
    }

    /**
     * A számokon, mértékegységeken és napszakokon kívül maradt-e érdemi szó.
     *
     * Az „ma reggel 78,4 kg” mérés, a „fekvenyomás 80 kg” nem. Ez a szabály
     * arra a maradékra való, amit a többi felismerő nem ért: ilyenkor a
     * mondatban a számon kívül nem maradhat semmi.
     */
    private static boolean onlyNumbersLeft(String s) {
        // Körülnéző (lookaround) határok, nem elnyelt elválasztók: két
        // szomszédos szó („ma reggel”) közül a második különben bennmaradna,
        // mert az elsőt kereső minta elvinné a köztük álló szóközt.
        String rest = s.replaceAll("\\d+([.,]\\d+)?", " ")
                // A mértékegység ragozva is mértékegység: a „79 kilóval
                // keltem" ugyanaz a mérés, mint a „79 kg". A ragos alak
                // (kilóval, kilót, kilóra) eddig bennmaradt a maradékban, és
                // ettől az egész mondat kiesett a mérések közül.
                .replaceAll("(?<![a-z])(kg\\w*|kilogramm\\w*|kilo\\w*|kila|szazalek|"
                        + "testzsir\\w*|ma|reggel|"
                        + "este|delben|delelott|delutan|ejjel|hajnalban|tegnap|most|"
                        + "eppen|epp|ebredes|felkeles|utan|kor|orakor|volt|voltam|"
                        // Az ébredés igéje is csak időpont: „79 kilóval keltem".
                        + "keltem|felkeltem|ebredtem|felebredtem|mertem|merve|"
                        // A magasság a mérés MELLETT szokott állni: a
                        // „80 kg, 180 cm" nyolcvan kiló – eddig egyik sem.
                        + "cm|centi\\w*|magassag|magas|"
                        // A mérés KÖRÜLMÉNYE is csak körülmény: az
                        // „éhgyomorra" és a „zuhany után" ugyanúgy nem adat,
                        // mint a napszak – a mérés MELLETT állnak, nem helyette.
                        + "ehgyomorra|ehgyomor|zuhany|furdes|furdo|esti|reggeli|"
                        + "utan|elott|inbody|szerint|"
                        // A mérleg többi sora is csak kíséret: az izom- és
                        // csonttömeg mellett a testsúly ugyanúgy mérés marad,
                        // csak épp nem az a szám.
                        + "izomtomeg\\w*|izomsuly\\w*|csonttomeg\\w*|zsirtomeg\\w*|"
                        + "testviz\\w*|zsigeri|vizmennyiseg\\w*|"
                        // A megnevezett nap ugyanolyan időpont, mint a napszak:
                        // a „kedden 80 kg voltam" ugyanaz a mérés, mint a „ma".
                        + "hetfon|kedden|szerdan|csutortokon|penteken|szombaton|"
                        + "vasarnap|hetfo|kedd|szerda|csutortok|pentek|szombat|"
                        // Az IDŐTÁV is csak kíséret: a „79,8 kg egy hét alatt"
                        // és a „78 kg két hónap után" mérés – eddig a mellette
                        // álló szavaktól az egész mondat kiesett.
                        + "egy|ket|harom|negy|ot|hat|het|honap|ev|nap|"
                        // A BÜSZKESÉG szava is csak kíséret: a „77,7 kg –
                        // eddigi legjobb" mérés, a jelző nem veszi el.
                        + "eddigi|legjobb|rekord|csucs|uj|vegre|kerek|"
                        // A PUSZTA KÖTŐSZÓ nem adat: a „ma reggel 82,3 kg, de
                        // tegnap 82,9 volt" mondatból a tegnapi szám kivágása
                        // után egyetlen árva „de" maradt – és ettől a MAI
                        // mérés is kiesett. A kötőszó és a hangulatszó nem
                        // mond ellent semminek, csak ott áll a mérés mellett.
                        + "de|es|viszont|azonban|pedig|illetve|mar|csak|"
                        + "sajnos|szerencsere|amugy|egyebkent|szoval|"
                        // A „pont", a „kereken" és a „kg-nál tartok" is csak
                        // kíséret: a „reggel éhgyomorra 68 kg pont" és a
                        // „82 kg-nál tartok" mérés – eddig mindkettő kiesett.
                        + "pont|pontosan|kereken|nal|nel|tartok|tartunk|"
                        // A KÜSZÖB átlépése is csak kíséret: a „78 kg alá
                        // mentem végre, 77,8" valódi mérése a 77,8.
                        + "ala|fole|folott|alatt|mentem|lementem|"
                        // A DÁTUM is csak kíséret: az „aug. 14. reggel 78 kg"
                        // jegyzetből másolt sor, a hónapnév nem veszi el.
                        + "januar|februar|marcius|aprilis|majus|junius|julius|"
                        + "augusztus|szeptember|oktober|november|december|"
                        + "jan|feb|marc|apr|maj|jun|jul|aug|szept|okt|nov|dec|"
                        + "en|an|jen|ejen|"
                        + "hete|honapja|eve|napja|hetre|honapra|evre)"
                        + "(?![a-z])", " ")
                .replaceAll("[^a-z]", " ").trim();
        return rest.isEmpty();
    }

    /**
     * RAGOZOTT gyakorlatnév a tagmondatban: „guggolásban", „fekvenyomásban".
     *
     * A tiltólista szóhatárt vár, ezért a ragos alak átcsúszott rajta: az
     * „elértem az új személyes csúcsomat guggolásban: 120 kg" SZÁZHÚSZ KILÓS
     * TESTSÚLYT írt a trendbe – a rúdon lévő súlyt. Egy ilyen bejegyzés a
     * súlygörbét, a BMI-t és a kalóriakeretet is elrontja.
     */
    private static boolean liftStem(String s) {
        // A CENTIVEL írt tagmondat körfogat marad: a „bicepszem 40 cm" a
        // kar körfogata, hiába gyakorlatnév is a bicepsz.
        if (s.contains("cm") || s.contains("centi")) return false;
        for (String w : new String[]{"fekvenyomas", "guggolas", "felhuzas",
                "holtemeles", "huzodzkodas", "tolodzkodas", "fekvotamasz",
                "kitores", "vallbol nyomas", "vallnyomas", "labtolas",
                "bicepsz", "tricepsz", "szakitas", "lokes", "evezes"})
            if (s.matches("(?s).*(?<![a-z])" + w + "\\w*.*")) return true;
        return false;
    }

    /** Egész szóként szerepel-e a mondatban. */
    private static boolean word(String s, String w) {
        int i = s.indexOf(w);
        while (i >= 0) {
            boolean l = i == 0 || !Character.isLetterOrDigit(s.charAt(i - 1));
            int e = i + w.length();
            boolean r = e >= s.length() || !Character.isLetterOrDigit(s.charAt(e));
            if (l && r) return true;
            i = s.indexOf(w, i + 1);
        }
        return false;
    }

    private static double num(String s) {
        try { return Double.parseDouble(s.replace(',', '.')); }
        catch (NumberFormatException e) { return 0; }
    }
}
