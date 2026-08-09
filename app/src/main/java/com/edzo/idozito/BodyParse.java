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
            "vagyok", "lettem", "nyomok", "fogytam", "hiztam", "leadtam", "testzsir",
            // Birtokos és összetett alakok: a szóhatáros keresés miatt a
            // „testzsírom" nem ugyanaz, mint a „testzsír" – a „22% a
            // testzsírom" eddig teljesen elveszett.
            "testzsirom", "testzsira", "zsirszazalek", "testzsirszazalek",
            // Az igekötős alakok külön: a szóhatáros keresés miatt a
            // „lefogytam" nem ugyanaz, mint a „fogytam". Huszonhat valós
            // mérés-mondattal próbálva ezek maradtak ki.
            "lefogytam", "felmentem", "lementem", "felszedtem",
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
     * Szavak, amelyektől a mondat biztosan NEM mérés – a súly másé.
     *
     * Mind egész szóként keresve: a rövid szótő máshol elrejtve („húsz”-ban a
     * „hús”) a legmegbízhatóbb módja annak, hogy egy jó mondat elvesszen.
     */
    private static final String[] NOT_BODY = {
            "nyomtam", "emeltem", "huztam", "toltam", "vettem", "vasaroltam", "hoztam",
            // Célok és becslések: a „70 kg alatt vagyok" nem hetven kiló, a
            // „szeretnék 75 lenni" meg egyáltalán nem mérés. Egy vágyból
            // csinált bejegyzés a trendet is, a BMI-t is elrontaná.
            "alatt", "felett", "folott", "korul", "korulbelul", "kb", "kozel",
            "szeretnek", "akarok", "cel", "celom", "lenni"
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
        return s.replaceAll(
                "(?<![\\d,.])\\d{1,3}(?:[.,]\\d{1,2})?\\s?(?:cm|centi|kg|kilo|%|szazalek)?"
                        + "\\s?-?r[o\u00f3]l\\b([^0-9]{0,12}?)"
                        // A MÉRTÉKEGYSÉG a két szám között is ott állhat: a
                        // „haskörfogat 92-ről 88 cm-re" nyolcvannyolcasa
                        // eddig nem illeszkedett, és a RÉGI érték maradt a
                        // naplóban – vagyis a fogyás napján egy hízás.
                        + "(\\d{1,3}(?:[.,]\\d{1,2})?\\s?"
                        + "(?:cm|centi|kg|kilo|%|szazalek)?\\s?-?r[ae]\\b)", "$2");
    }

    /** A mondatban rejlő mérés, vagy egy üres Body. */
    public static Body parse(String q) {
        if (q == null) return new Body(0, 0);
        // A kiírt számnév ugyanolyan mérés: „hetvennyolc kiló vagyok". A
        // mérleget sokan hangosan olvassák fel, és úgy is írják le.
        // A maskTimeUnder a SZÁMNÉV-fordítás előtt fut: az „egy hét alatt"
        // hete a digits() után már „7", és a szabály nem ismerné fel benne az
        // időtartamot – az „alatt" pedig összehasonlításnak látszana.
        String s = keepTheNewValue(dropOtherLogs(
                Hu.digits(maskTimeUnder(Hu.correction(Foods.norm(q))))));
        if (s.isEmpty()) return new Body(0, 0);
        for (String n : NOT_BODY) if (word(s, n)) return new Body(0, 0);
        // A két kapu közül legalább az egyiknek nyitva kell lennie.
        boolean said = hasBodyWord(s);

        double[] cm = circumferences(s);
        double fat = bodyFat(s);
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
        boolean dropped = false;
        // A vessző magyarul tizedesjel is: a „78,4" NEM két tagmondat.
        for (String part : s.split("[,;.](?!\\d)")) {
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
                if (part.contains(w)) { other = true; break; }
            // A SOROZAT tagmondata is másé: a „körfogatok és súlyok: derék
            // 84, guggolás 3x5 100" száza a rúdon van, nem a mérlegen –
            // eddig száz kilós MÉRÉS lett belőle a súlytrendben.
            if (part.matches(".*\\d\\s?x\\s?\\d.*")) other = true;
            if (other) { dropped = true; continue; }
            if (keep.length() > 0) keep.append(' ');
            keep.append(part.trim());
        }
        String out = keep.toString().trim();
        if (out.isEmpty()) return s;
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
    private static double bodyFat(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2}([.,]\\d)?)\\s?(%|szazalek)").matcher(s);
        while (m.find()) {
            double v = num(m.group(1));
            if (v >= MIN_FAT && v <= MAX_FAT) return v;
        }
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
                    "g ", "gramm", "dkg", "dl", "ml", "liter"})
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
                                + "(\\d{1,3}([.,]\\d)?)(?!\\d|[.,]\\d|\\s?kg)\\s?(cm|centi\\w*)?"
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
        return false;
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
                        + "ehgyomorra|ehgyomor|zuhany|utan|inbody|szerint|"
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
                        + "hete|honapja|eve|napja|hetre|honapra|evre)"
                        + "(?![a-z])", " ")
                .replaceAll("[^a-z]", " ").trim();
        return rest.isEmpty();
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
