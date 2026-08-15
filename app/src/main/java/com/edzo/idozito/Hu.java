package com.edzo.idozito;

import java.util.Locale;

/**
 * Magyar számformázás egy helyen.
 *
 * Az app szövege magyar, a tizedeselválasztó tehát vessző: „5,2 km", nem
 * „5.2 km". Ez korábban szétcsúszott – a víz és a napi kihívás vesszővel írt,
 * a táv, a tempó, a BMI és a testsúly viszont ponttal.
 *
 * IDŐ-formátumokhoz szándékosan NEM ezt használjuk: ott nincs tizedes, viszont
 * a rögzített (US) locale garantálja az ASCII számjegyeket. A CSV-export is
 * marad ponttal, hogy bármelyik táblázatkezelő és nyelv beolvassa.
 */
public final class Hu {

    private Hu() {}

    public static final Locale LOCALE = new Locale("hu");

    /**
     * Súly kiírása: egész, ha kerek (40), egyébként egy tizedessel (42,5).
     *
     * Egy „120,0 kg"-os rekord úgy néz ki, mintha a tizedes számítana – a
     * teremben viszont senki nem mond nullát a vessző után.
     */
    public static String kg(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return String.valueOf(Math.round(v));
        return d1(v);
    }

    /**
     * Ezres tagolás magyarul, keskeny szóközzel: „12 345".
     *
     * A lépésszám négy-öt jegyű, és tagolás nélkül egy pillanatra minden
     * ilyen szám egyforma: a „9870" és a „19870" ránézésre ugyanaz. Magyarul
     * a tagolás szóköz (nem vessző és nem pont) – a nem törhető keskeny
     * szóköz tartja egyben a számot a sortörésnél.
     */
    public static String num(long v) {
        String d = String.valueOf(Math.abs(v));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            if (i > 0 && (d.length() - i) % 3 == 0) sb.append('\u202f');
            sb.append(d.charAt(i));
        }
        return (v < 0 ? "-" : "") + sb;
    }

    /** Egy tizedes, magyarul: „5,2". */
    public static String d1(double v) {
        return String.format(LOCALE, "%.1f", v);
    }

    /**
     * Kiírt számok számjeggyé: „négy kör" → „4 kor", „ötször ötöt" → „5ször 5".
     *
     * A teremben és a konyhában is ritkán ír bárki számjegyet, a felismerők
     * viszont számot keresnek. A tárgyragos alak („ötöt", „tízet") külön
     * szerepel, mert pont az a leggyakoribb: „nyomtam háromszor tízet".
     *
     * Csak önálló szót cserélünk – a „hatizom" nem hat izom, a „hetes" nem
     * hét es. Kivétel a „-szor/-szer" toldalék, ami a számhoz tapad.
     *
     * @param s ékezet nélküli, kisbetűs szöveg (Foods.norm kimenete)
     */
    public static String digits(String s) {
        if (s == null) return "";
        String out = s;
        for (String[] w : NUM_WORDS) {
            int p = out.indexOf(w[0]);
            while (p >= 0) {
                int e = p + w[0].length();
                // A „-szor/-szer" toldalék a számhoz tapad, tehát ott nem
                // számít összeragadásnak – de csak akkor, ha a szó ott VÉGET is
                // ér. Az „egyszerű" különben „1szeru" lett, a „kétszeres"
                // pedig „2szeres": egyik sem szám, csak véletlenül úgy néz ki.
                boolean mult = (out.startsWith("szor", e) || out.startsWith("szer", e))
                        && (e + 4 >= out.length() || !Character.isLetter(out.charAt(e + 4)));
                // A SZÁM UTÁNI kötőjeles toldalék nem számnév: a „6-ot" hatot
                // jelent, nem „6 öt"-öt. Ékezet nélkül az „öt" és az „-ot"
                // rag egybeesik, és a „6-ot" ebből 6-5 lett – vagyis egy
                // hatmásodperces munka ötmásodperces pihenővel, a mérleg
                // számából. („A mérleg 79,6-ot mutatott" időzítő-terv volt.)
                boolean caseSuffix = p > 1 && out.charAt(p - 1) == '-'
                        && Character.isDigit(out.charAt(p - 2));
                // A „hát" nem hat. Ékezet nélkül a testtáj és a számnév
                // egybeesik, és a „felső hát erősítés" hat darab hatvanperces
                // kondi-bejegyzés lett hat napra elosztva. A jelző dönti el:
                // a „felső/alsó hát" testrész.
                boolean bodyPart = w[0].equals("hat")
                        && ((p >= 6 && out.startsWith("felso ", p - 6))
                            || (p >= 5 && out.startsWith("also ", p - 5)));
                boolean glued = caseSuffix || bodyPart
                        || (p > 0 && Character.isLetter(out.charAt(p - 1)))
                        || (e < out.length() && Character.isLetter(out.charAt(e)) && !mult);
                if (glued) {
                    p = out.indexOf(w[0], p + 1);
                } else {
                    out = out.substring(0, p) + w[1] + out.substring(e);
                    p = out.indexOf(w[0], p + w[1].length());
                }
            }
        }
        // A kimondott tizedes: „hetvennyolc egész négy" a mérlegről leolvasott
        // 78,4. A szótár után jön, mert addigra mindkét oldal számjegy –
        // csak az EGY jegyű tört számít, hogy egy felsorolás („3 egész 12
        // darab") ne váljon tizedestörtté.
        out = out.replaceAll("(?<![\\d.,])(\\d{1,3})\\s?egesz\\s?(\\d)(?![\\d.,])", "$1,$2");
        return out;
    }

    /**
     * A magyar számnevek szótára, generálva.
     *
     * Kézzel felsorolni a tizenegytől ötszázig terjedő alakokat (tárgyraggal
     * együtt közel háromszázat) hibalehetőség: pont a ritkábbak maradnának ki,
     * és pont azok, amiket a teremben mondanak – „nyolcvanöt kiló",
     * „harminckettő perc". A generálás mellett a törtek és az egyjegyűek
     * kézzel szerepelnek, mert azoknak nincs szabálya.
     *
     * A HOSSZABB alak mindig elöl van (hossz szerint csökkenő rendezés),
     * különben a „tizenketto" tiz + enketto lenne, a „nyolcvanot" pedig
     * nyolcvan + ot.
     */
    private static final String[][] NUM_WORDS = buildNumWords();

    private static String[][] buildNumWords() {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        // Egyjegyűek és törtek: ezeknek nincs képzési szabálya.
        String[][] base = {{"masfel", "1,5"}, {"fel", "0,5"},
                {"egyet", "1"}, {"egy", "1"}, {"kettot", "2"}, {"ketto", "2"}, {"ket", "2"},
                {"harmat", "3"}, {"harom", "3"}, {"negyet", "4"}, {"negy", "4"},
                {"otot", "5"}, {"ot", "5"}, {"hatot", "6"}, {"hat", "6"},
                {"hetet", "7"}, {"het", "7"}, {"nyolcat", "8"}, {"nyolc", "8"},
                {"kilencet", "9"}, {"kilenc", "9"}, {"tizet", "10"}, {"tiz", "10"},
                {"huszat", "20"}, {"husz", "20"}};
        for (String[] b : base) out.add(b);

        // Egyesek alany- és tárgyesetben, a tízesekhez fűzve.
        String[][] units = {{"egy", "1"}, {"egyet", "1"}, {"kettot", "2"}, {"ketto", "2"},
                {"ket", "2"}, {"harom", "3"}, {"harmat", "3"}, {"negy", "4"}, {"negyet", "4"},
                {"ot", "5"}, {"otot", "5"}, {"hat", "6"}, {"hatot", "6"},
                {"het", "7"}, {"hetet", "7"}, {"nyolc", "8"}, {"nyolcat", "8"},
                {"kilenc", "9"}, {"kilencet", "9"}};
        // A „tizen"/„huszon" csak összetételben szám, a többi magában is.
        String[][] tens = {{"tizen", "10"}, {"huszon", "20"}, {"harminc", "30"},
                {"negyven", "40"}, {"otven", "50"}, {"hatvan", "60"}, {"hetven", "70"},
                {"nyolcvan", "80"}, {"kilencven", "90"}};
        String[][] tensAcc = {{"harmincat", "30"}, {"negyvenet", "40"}, {"otvenet", "50"},
                {"hatvanat", "60"}, {"hetvenet", "70"}, {"nyolcvanat", "80"},
                {"kilencvenet", "90"}};
        java.util.List<String[]> belowHundred = new java.util.ArrayList<>();
        for (String[] t : tens) {
            if (!t[0].equals("tizen") && !t[0].equals("huszon")) belowHundred.add(t);
            for (String[] u : units)
                belowHundred.add(new String[]{t[0] + u[0],
                        String.valueOf(Integer.parseInt(t[1]) + Integer.parseInt(u[1]))});
        }
        for (String[] t : tensAcc) belowHundred.add(t);
        // A „tíz" és a „húsz" magában az alaplistában van; a százas
        // összetételekhez („százhúsz", „száztíz") itt is kell.
        belowHundred.add(new String[]{"tiz", "10"});
        belowHundred.add(new String[]{"husz", "20"});
        out.addAll(belowHundred);

        // Százasok: a konyhában és a teremben is gyakoriak („százötven gramm",
        // „kétszáz méter").
        String[][] hundreds = {{"szaz", "100"}, {"szazat", "100"}, {"ketszaz", "200"},
                {"haromszaz", "300"}, {"negyszaz", "400"}, {"otszaz", "500"}};
        for (String[] h : hundreds) {
            out.add(h);
            if (h[0].endsWith("at")) continue;          // a tárgyragoshoz nem fűzünk
            for (String[] u : units)
                out.add(new String[]{h[0] + u[0],
                        String.valueOf(Integer.parseInt(h[1]) + Integer.parseInt(u[1]))});
            for (String[] b : belowHundred)
                out.add(new String[]{h[0] + b[0],
                        String.valueOf(Integer.parseInt(h[1]) + Integer.parseInt(b[1]))});
        }

        // Ezresek: „ezer lépés", „leúsztam ezerötszáz métert", „tízezer lépés".
        // A napi lépésszám és az úszástáv magyarul kimondva mindig ezres, és
        // eddig egyetlen ilyen alak sem került át számmá.
        String[][] thousands = {{"ezer", "1000"}, {"ketezer", "2000"},
                {"haromezer", "3000"}, {"negyezer", "4000"}, {"otezer", "5000"},
                {"hatezer", "6000"}, {"hetezer", "7000"}, {"nyolcezer", "8000"},
                {"kilencezer", "9000"}, {"tizezer", "10000"}, {"tizenotezer", "15000"},
                {"huszezer", "20000"}, {"harmincezer", "30000"}, {"szazezer", "100000"}};
        for (String[] t : thousands) {
            out.add(t);
            // Csak a százasokat fűzzük hozzá: az „ezerötszáz" gyakori, az
            // „ezerötszázhuszonhárom" nem – és minden felvett alak lassítja a
            // felismerést, mert mindegyiket végigkeressük.
            for (String[] h : hundreds) {
                if (h[0].endsWith("at")) continue;
                out.add(new String[]{t[0] + h[0],
                        String.valueOf(Integer.parseInt(t[1]) + Integer.parseInt(h[1]))});
            }
        }

        // Hossz szerint csökkenő: a hosszabb alak mindig előbb illeszkedjen.
        String[][] arr = out.toArray(new String[0][]);
        java.util.Arrays.sort(arr, new java.util.Comparator<String[]>() {
            @Override public int compare(String[] x, String[] y) {
                return y[0].length() - x[0].length();
            }
        });
        return arr;
    }

    /**
     * Változás az előző időszakhoz képest: „+12%", „−8%", „= 0%".
     *
     * Ha nem volt előzmény, nincs mihez viszonyítani: ilyenkor „új" a válasz,
     * nem „+100%" – az utóbbi azt sugallná, hogy a duplájára nőtt valami,
     * ami eddig nem is létezett.
     */
    public static String delta(double now, double prev) {
        if (prev <= 0) return now > 0 ? "új" : "—";
        if (now <= 0) return "−100%";
        double pct = (now - prev) / prev * 100;
        long r = Math.round(pct);
        if (r == 0) return "=";
        return (r > 0 ? "+" : "−") + Math.abs(r) + "%";
    }

    /** A hét napja magyarul, hétfőtől (0) vasárnapig (6). */
    public static String dayName(int dowIdx) {
        String[] n = {"Hétfő", "Kedd", "Szerda", "Csütörtök", "Péntek", "Szombat", "Vasárnap"};
        return dowIdx >= 0 && dowIdx <= 6 ? n[dowIdx] : "";
    }

    /** Két tizedes, magyarul: „5,23". */
    public static String d2(double v) {
        return String.format(LOCALE, "%.2f", v);
    }

    /**
     * A HELYESBÍTÉS második száma az igazi: „nem aludtam 8 órát, csak 5-öt".
     *
     * A magyar így javít: kimondja, ami nem igaz, aztán utána azt, ami igen.
     * A felismerők eddig az ELSŐ számot vették – vagyis pont azt, amit a
     * mondat tagad. Nyolc óra alvás került a naplóba öt helyett, nyolcvan
     * kiló hetvennyolc helyett: a hibás adat rosszabb, mint a semmilyen.
     *
     * A csere a helyén hagyja a mértékegységet és az igét, csak a számot
     * írja át – onnantól minden meglévő szabály érti a mondatot. Szám
     * nélküli tagadásra („nem futottam ma, csak sétáltam") nem él.
     *
     * @param s a mondat – normalizálva vagy nyersen, mindkettő jó
     */
    public static String correction(String s) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                // Ékezetre és kis-nagybetűre érzéketlen: a nyers, még
                // normalizálatlan mondaton is futnia kell (az étel-oldal a
                // saját szövegével dolgozik tovább).
                "(?iu)(?<!\\p{L})nem\\s+([^,;0-9]{0,30}?)(\\d{1,3}(?:[.,]\\d{1,2})?)"
                        + "([^,;0-9]{0,25}?)\\s*[,;]?\\s*(?:csak|hanem)\\s+"
                        + "(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*-?\\s*(\\p{L}{0,4})(?!\\p{L})")
                .matcher(s);
        if (!m.find()) return s;
        // A szám mögötti rövid szó legtöbbször rag („csak 5-öt"), az
        // eldobható – de ha a tagadó félben nem volt mértékegység, akkor ez
        // AZ EGYSÉG: a „nem 45, hanem 60 perc jóga" percét eldobva hatvan
        // jóga-alkalom lett a hatvan perc helyett.
        String unit = m.group(3).trim().isEmpty() && !m.group(5).isEmpty()
                ? " " + m.group(5) : m.group(3);
        return s.substring(0, m.start()) + m.group(1) + m.group(4) + unit
                + s.substring(m.end());
    }
}
