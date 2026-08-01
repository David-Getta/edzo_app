package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;

/**
 * Kézzel felvehető mozgásformák: amit nem az app mért.
 *
 * Az időzítő és a súlyzós napló csak azt látja, ami a telefonnal történt. Egy
 * kézilabda-edzés, egy uszodai óra vagy egy konditermi nap viszont ugyanúgy
 * edzés – és ha nem kerül a naplóba, akkor megszakad a széria, elmarad az XP,
 * és a statisztika kevesebbet mutat a valóságnál. Ezért lehet utólag is
 * bejegyezni edzést, és az mindenben ugyanolyan, mint egy mért.
 *
 * A kalóriabecslés a mozgásforma átlagos intenzitásából (MET) és a testsúlyból
 * jön – ugyanazzal a képlettel, amit az app máshol is használ. Ez becslés:
 * a valódi érték a tempótól és az egyéni adottságoktól is függ.
 */
public final class Activities {

    private Activities() {}

    public static final class Kind {
        public final String id, emoji, name;
        /** Átlagos intenzitás (MET) – ebből lesz a kalóriabecslés. */
        public final double met;
        /** Van-e értelme távot kérni hozzá (futás igen, kézilabda nem). */
        public final boolean distance;
        /** Egy szokásos alkalom hossza percben – ennyit ajánlunk fel előre. */
        public final int defaultMin;
        /** Szótövek a szöveges felismeréshez (ékezet nélkül, kisbetűvel). */
        final String[] words;

        Kind(String id, String emoji, String name, double met, boolean distance,
             int defaultMin, String... words) {
            this.id = id; this.emoji = emoji; this.name = name;
            this.met = met; this.distance = distance;
            this.defaultMin = defaultMin; this.words = words;
        }

        public String title() { return emoji + " " + name; }
    }

    /**
     * A MET-értékek a mozgásformák szokásos, közepes intenzitású átlagai
     * (Compendium of Physical Activities nagyságrendjei). A szótövek között
     * ott vannak a hétköznapi rövidítések is: „kézi", „bringa", „kondi".
     */
    public static final Kind[] ALL = {
            new Kind("futas", "🏃", "Futás", 9.8, true, 45,
                    "futas", "futo edzes", "futoedzes", "futni", "futott", "kocog", "futok",
                    "maraton", "futkaroz", "sprint"),
            new Kind("uszas", "🏊", "Úszás", 7.0, true, 45,
                    "uszas", "uszo edzes", "uszni", "uszoedzes", "uszodaz", "uszt",
                    // A vizes sportok is ide: a vízilabda és a vizitorna a
                    // medencés mozgások közül az úszáshoz áll a legközelebb.
                    "vizilabda", "aquafit", "vizitorna"),
            new Kind("kerekpar", "🚴", "Kerékpár", 7.5, true, 60,
                    "kerekpar", "bringa", "bicikli", "bicaj", "canga", "teker", "bmx"),
            new Kind("tura", "🥾", "Túra / gyaloglás", 5.3, true, 90,
                    "tura", "gyaloglas", "seta", "setalas", "kirandulas", "nordic"),
            new Kind("evezes", "🚣", "Evezés / evezőgép", 7.0, true, 30,
                    "evezes", "evezo", "evezt", "kajak", "sup deszka"),
            new Kind("kondi", "🏋", "Kondi / súlyzós edzés", 5.0, false, 60,
                    "kondi", "konditerem", "terem", "sulyzo", "gym", "gepterem", "gyur",
                    // A „tornaterem" egyben fedi a „torna" (jóga) és a „terem"
                    // (kondi) tövet is – a hosszabb tő nyer, így egy találat lesz.
                    "crossfit", "trx", "erosit", "fekvotamasz", "tornaterem", "wod",
                    "guggolas", "felules", "huzodzkodas", "plank"),
            new Kind("kezilabda", "🤾", "Kézilabda", 8.0, false, 90,
                    "kezilabda", "kezi edzes", "keziedzes", "kezi"),
            new Kind("foci", "⚽", "Foci", 7.0, false, 90,
                    "foci", "focizas", "labdarugas", "focizt", "futball"),
            new Kind("kosarlabda", "🏀", "Kosárlabda", 6.5, false, 60,
                    "kosarlabda", "kosarazas", "kosar edzes", "kosar"),
            new Kind("roplabda", "🏐", "Röplabda", 4.0, false, 60,
                    "roplabda", "roplab", "roplabdaz"),
            new Kind("tenisz", "🎾", "Tenisz / squash / tollas", 7.3, false, 60,
                    "tenisz", "squash", "fallabda", "tollaslabda", "tollas", "pingpong",
                    "ping pong", "asztalitenisz"),
            new Kind("harcmuveszet", "🥋", "Harcművészet / box", 10.0, false, 60,
                    "harcmuvesz", "kickbox", "box", "karate", "judo", "birkozas", "mma", "aikido"),
            new Kind("tanc", "💃", "Tánc / aerobik", 5.5, false, 60,
                    "tanc", "aerobik", "zumba", "spinning", "kangoo", "alakformalo"),
            new Kind("joga", "🧘", "Jóga / nyújtás / pilates", 3.0, false, 45,
                    // A „torna" fedi a gerinctornát, gyógytornát, tornázást is.
                    "joga", "yoga", "pilates", "nyujtas", "stretch", "torna", "medital",
                    "meditac"),
            new Kind("korcsolya", "⛸", "Korcsolya / görkorcsolya", 7.0, false, 60,
                    "korcsolya", "gorkorcsolya", "gorkori", "gordeszka", "roller",
                    "jegkorong", "hoki"),
            new Kind("si", "🎿", "Sí / snowboard", 6.0, false, 120,
                    "sieles", "sizes", "snowboard", "sielt", "sifutas"),
            new Kind("fal", "🧗", "Falmászás", 8.0, false, 60,
                    "falmaszas", "maszas", "boulder", "maszofal"),
            new Kind("munka", "🌳", "Kerti / fizikai munka", 4.0, false, 60,
                    "kerti munka", "fizikai munka", "kertesz", "favagas", "lapatolas"),
            new Kind("egyeb", "🤸", "Egyéb mozgás", 6.0, false, 45,
                    "egyeb mozgas", "egyeb edzes", "egyeb", "sportol", "mozog",
                    "lovagl", "lovagol", "vitorlaz", "szorf", "wakeboard", "golf"),
    };

    /** A mozgásforma azonosító alapján, vagy null, ha nem ismerjük. */
    public static Kind byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Kind k : ALL) if (k.id.equals(id)) return k;
        return null;
    }

    /**
     * Táv-alapú mozgás-e? Az előzményekben ez dönti el, hogy a bejegyzés a
     * futás vagy a terem szűrőbe kerüljön. Ismeretlen (vagy hiányzó) azonosító
     * esetén false – a régi, kézi bejegyzés előtti naplók így változatlanok.
     */
    public static boolean isCardio(String id) {
        Kind k = byId(id);
        return k != null && k.distance;
    }

    /**
     * Elégetett kalória: MET × 3,5 × testsúly / 200 × perc.
     *
     * Ugyanaz a képlet, amivel az app a mért edzéseket is becsli – csak ott
     * egységesen 6-os MET-tel, mert ott nem tudjuk, milyen mozgás történt.
     * Itt tudjuk, ezért pontosabb: egy óra jóga és egy óra harcművészet nem
     * ugyanannyi.
     */
    public static double calories(Kind k, double weightKg, int minutes) {
        double w = weightKg > 0 ? weightKg : 70;
        double met = k == null ? 6.0 : k.met;
        return met * 3.5 * w / 200.0 * Math.max(0, minutes);
    }

    /**
     * Sportágankénti összesítés a Statisztikához: cím → {alkalom, össz-mp},
     * alkalom szerint csökkenő sorrendben.
     *
     * A besorolás a bejegyzés „kind" mezőjéből jön (kézi felvétel), annak
     * híján a névből: a névtelen időzítős edzés mért futás – az a Futás
     * sorba olvad, mert a felhasználót az érdekli, mennyit futott, nem az,
     * hogy melyik gombbal rögzítette. A programmal futtatott időzítős edzés
     * a program nevén jelenik meg.
     */
    public static java.util.LinkedHashMap<String, long[]> breakdown(
            String[] kinds, String[] names, int[] durSec) {
        java.util.LinkedHashMap<String, long[]> sum = new java.util.LinkedHashMap<>();
        int n = Math.min(kinds.length, Math.min(names.length, durSec.length));
        for (int i = 0; i < n; i++) {
            Kind k = byId(kinds[i]);
            String label;
            if (k != null) label = k.title();
            else if (names[i] == null || names[i].isEmpty()) label = byId("futas").title();
            else {
                // Ha a program neve elárulja a sportot („Kézilabda edzés"),
                // az a sorba olvad – egy sport egy sor, akárhogy rögzítették.
                Kind byName = kindByText(names[i]);
                label = byName != null ? byName.title() : "⏱ " + names[i];
            }
            long[] row = sum.get(label);
            if (row == null) sum.put(label, row = new long[2]);
            row[0]++;
            row[1] += Math.max(0, durSec[i]);
        }
        // Rendezés alkalom szerint, azonos számnál idő szerint.
        java.util.ArrayList<java.util.Map.Entry<String, long[]>> rows =
                new java.util.ArrayList<>(sum.entrySet());
        java.util.Collections.sort(rows, (a, b) -> {
            if (a.getValue()[0] != b.getValue()[0])
                return Long.compare(b.getValue()[0], a.getValue()[0]);
            return Long.compare(b.getValue()[1], a.getValue()[1]);
        });
        java.util.LinkedHashMap<String, long[]> out = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<String, long[]> e : rows) out.put(e.getKey(), e.getValue());
        return out;
    }

    /**
     * A mozgásformák a felhasználó szokásai szerint rendezve: amit gyakran
     * vesz fel, az kerül a lista elejére. A kézilabdás ember kézilabdát
     * naplóz – ne kelljen minden alkalommal a lista közepére görgetnie.
     * A nem használt fajták az eredeti sorrendben maradnak (stabil rendezés).
     */
    public static Kind[] orderedByHabit(String[] recentKindIds) {
        final java.util.HashMap<String, Integer> cnt = new java.util.HashMap<>();
        if (recentKindIds != null)
            for (String id : recentKindIds)
                if (byId(id) != null) {
                    Integer c = cnt.get(id);
                    cnt.put(id, c == null ? 1 : c + 1);
                }
        Kind[] out = ALL.clone();
        java.util.Arrays.sort(out, (a, b) -> {
            Integer ca = cnt.get(a.id), cb = cnt.get(b.id);
            return (cb == null ? 0 : cb) - (ca == null ? 0 : ca);
        });
        return out;
    }

    /**
     * „Rég volt kézilabda" – a napi biztatás sport-tudatos sora, vagy null.
     *
     * Azt a sportot keressük, ami a felhasználónak láthatóan szokása (legalább
     * három alkalom az elmúlt 30 napban), de legalább egy hete kimaradt. Az
     * általános „ideje edzeni" bárkinek szólhat; az, hogy „9 napja nem volt
     * kézilabda", csak neki – és pont ettől hat.
     *
     * A mért (névtelen) futás a futás sporthoz számít, ahogy a bontásban is.
     * Ami nem sorolható be, az kimarad – ebből a sorból tévedni rosszabb,
     * mint hallgatni.
     */
    public static String missedSport(String[] kinds, String[] names, long[] ts, long now) {
        long day = 24L * 3600 * 1000;
        java.util.HashMap<String, long[]> per = new java.util.HashMap<>(); // id → {30 napi darab, utolsó ts}
        int n = Math.min(kinds.length, Math.min(names.length, ts.length));
        for (int i = 0; i < n; i++) {
            Kind k = byId(kinds[i]);
            String id;
            if (k != null) id = k.id;
            else if (names[i] == null || names[i].isEmpty()) id = "futas";
            else {
                // A program nevéből felismert sport is szokásnak számít.
                Kind byName = kindByText(names[i]);
                if (byName == null) continue;       // besorolhatatlan: kimarad
                id = byName.id;
            }
            long age = now - ts[i];
            if (age < 0 || age > 60 * day) continue;
            long[] row = per.get(id);
            if (row == null) per.put(id, row = new long[2]);
            if (age <= 30 * day) row[0]++;
            row[1] = Math.max(row[1], ts[i]);
        }
        String bestId = null;
        long bestCount = 0;
        for (java.util.Map.Entry<String, long[]> e : per.entrySet()) {
            if (e.getValue()[0] >= 3 && e.getValue()[0] > bestCount) {
                bestCount = e.getValue()[0];
                bestId = e.getKey();
            }
        }
        if (bestId == null) return null;
        int daysSince = Days.between(per.get(bestId)[1], now);
        if (daysSince < 7) return null;
        Kind k = byId(bestId);
        return k.title() + ": " + daysSince + " napja kimaradt – ideje újra!";
    }

    // ---------------- Szöveges felvétel ----------------

    /** Egy tétel a szövegből: hány alkalom, melyik mozgásból, mennyi ideig. */
    public static final class Plan {
        public final Kind kind;
        public final int count;
        public final int minutes;
        /** Egy alkalom távja km-ben (0 = nincs megadva). */
        public final double km;
        Plan(Kind kind, int count, int minutes, double km) {
            this.kind = kind; this.count = count; this.minutes = minutes; this.km = km;
        }
        /** Emberi összefoglaló: „1 × 🏃 Futás · 10 km · 60 perc”. */
        public String label() {
            String k = km <= 0 ? ""
                    : " · " + (km == Math.floor(km) ? String.valueOf((long) km)
                            : String.valueOf(km).replace('.', ',')) + " km";
            return count + " × " + kind.title() + k + " · " + minutes + " perc";
        }
    }

    /**
     * Tipikus tempó (perc/km) a táv-alapú mozgásokhoz. Ha a mondatban táv van,
     * de időtartam nincs, ebből becsüljük a hosszt – a 45 perces alapértelmezés
     * egy 10 km-es futásra 4:30/km-t jelentene, ami versenytempó.
     */
    static int minPerKm(Kind k) {
        if (k == null || !k.distance) return 0;
        switch (k.id) {
            case "futas": return 6;
            case "uszas": return 25;
            case "kerekpar": return 3;
            case "tura": return 12;
            case "evezes": return 5;
            default: return 8;
        }
    }

    /** A szövegből kiolvasott terv: mely mozgások, és hány napra elosztva. */
    public static final class Parsed {
        public final List<Plan> plans;
        /** Hány napra osztjuk szét (1 = egyetlen nap). */
        public final int days;
        /** Hány nappal ezelőtt kezdődik az időszak (0 = ma, 1 = tegnap). */
        public final int offset;
        /** A múltbeli bejegyzések órája („tegnap este" → 19); alap a dél. */
        public final int hour;
        Parsed(List<Plan> plans, int days, int offset) {
            this(plans, days, offset, 12);
        }
        Parsed(List<Plan> plans, int days, int offset, int hour) {
            this.plans = plans; this.days = days; this.offset = offset; this.hour = hour;
        }
        public boolean isEmpty() { return plans.isEmpty(); }
        public int total() {
            int n = 0;
            for (Plan p : plans) n += p.count;
            return n;
        }
    }

    /**
     * A terv bejegyzéseinek időbélyegei, a mentés sorrendjében (tervenként,
     * azon belül alkalmanként).
     *
     * A szabályok, amiken jelvény és megjelenítés múlik:
     *
     * – A MAI bejegyzés a mostani pillanatot kapja: ettől lesz igaz a
     *   „ma edzett", és nem kerül a jövőbe.
     * – A MÚLTBELI nap délidőt kap – vagy a kimondott napszakot („tegnap
     *   este" → 19 óra). A rögzítés óra-perce ott hazugság lenne: a
     *   tegnapelőtti kézilabda nem este 11-kor volt, csak akkor lett beírva.
     * – Több alkalom egyenletesen oszlik el az időszakon (6 kézi 3 napra =
     *   naponta kettő), és minden bejegyzés KÜLÖNBÖZŐ időbélyeget kap – az
     *   időbélyeg azonosítja őket megnyitáskor és törléskor.
     */
    public static long[] timestamps(Parsed p, long now) {
        int n = 0;
        for (Plan pl : p.plans) n += pl.count;
        long[] out = new long[n];
        int i = 0;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        for (Plan pl : p.plans) {
            for (int k = 0; k < pl.count; k++) {
                int dayBack = p.offset + (p.days > 1 ? (k * p.days) / pl.count : 0);
                cal.setTimeInMillis(now);
                cal.add(java.util.Calendar.DAY_OF_YEAR, -dayBack);
                if (dayBack > 0) {
                    cal.set(java.util.Calendar.HOUR_OF_DAY, p.hour);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                }
                // Másodperc-eltolás: elég az egyediséghez, de éjfél körül sem
                // csúsztatja át a bejegyzést az előző napra.
                cal.add(java.util.Calendar.SECOND, -i);
                out[i++] = cal.getTimeInMillis();
            }
        }
        return out;
    }

    private static final String[][] NUM_WORDS = buildNumWords();

    /**
     * Az alap számnevek mellett a tízesek és az összetett alakok is
     * („negyvenöt perc", „huszonöt fekvőtámasz") – generálva, mert a ~90
     * alakot kézzel felsorolni hibalehetőség lenne.
     */
    private static String[][] buildNumWords() {
        java.util.List<String[]> out = new java.util.ArrayList<>(java.util.Arrays.asList(
                new String[][]{
                        {"egy", "1"}, {"ket", "2"}, {"ketto", "2"}, {"harom", "3"},
                        {"negy", "4"}, {"ot", "5"}, {"hat", "6"}, {"het", "7"},
                        {"nyolc", "8"}, {"kilenc", "9"}, {"tiz", "10"}, {"husz", "20"},
                }));
        String[][] tens = {{"tizen", "10"}, {"huszon", "20"}, {"harminc", "30"},
                {"negyven", "40"}, {"otven", "50"}, {"hatvan", "60"},
                {"hetven", "70"}, {"nyolcvan", "80"}, {"kilencven", "90"}};
        String[][] units = {{"egy", "1"}, {"ketto", "2"}, {"ket", "2"}, {"harom", "3"},
                {"negy", "4"}, {"ot", "5"}, {"hat", "6"}, {"het", "7"},
                {"nyolc", "8"}, {"kilenc", "9"}};
        for (String[] t : tens) {
            if (!t[0].equals("tizen") && !t[0].equals("huszon"))
                out.add(new String[]{t[0], t[1]});
            for (String[] u : units)
                out.add(new String[]{t[0] + u[0],
                        String.valueOf(Integer.parseInt(t[1]) + Integer.parseInt(u[1]))});
        }
        return out.toArray(new String[0][]);
    }

    /**
     * Ezek a szavak a „nap"/„hét" szótövet tartalmazzák, de nem időszakot
     * jelentenek. Nélkülük a „hétfőn futottam" egy hetes időszaknak látszana.
     */
    private static final String[] NOT_SPAN = {
            "napi", "naplo", "naploban", "naplot", "naptar", "napozas", "napsutes",
            "hetfo", "hetfon", "hetfoi", "hetvege", "hetvegen", "hetkoznap", "hetkoznapon",
    };

    /**
     * A szövegben (pl. egy időzítős program nevében) felismert mozgásforma,
     * vagy null. A leghosszabb illeszkedő szótő nyer („sífutás" → sí, nem futás).
     */
    public static Kind kindByText(String text) {
        if (text == null || text.isEmpty()) return null;
        String s = Foods.norm(text);
        Kind best = null;
        int bestLen = 0;
        for (Kind k : ALL)
            for (String w : k.words)
                if (w.length() > bestLen && s.contains(w)) { best = k; bestLen = w.length(); }
        return best;
    }

    /** Meddig keressük visszafelé a darabszámot a mozgás neve előtt. */
    private static final int NUM_REACH = 26;

    /**
     * Több edzés felvétele egyetlen mondatból, pl.:
     * „az elmúlt 3 nap alatt 3 futó edzés és 6 kézi edzés”.
     *
     * Amit kiolvas: hány napra osztjuk szét, melyik mozgásból hány alkalom, és
     * ha meg van adva, mennyi ideig tartott egy-egy alkalom („60 perc”).
     * Ami nincs benne, arra a mozgásforma szokásos hossza jön (kézilabda 90,
     * futás 45 perc) – a felhasználó a mentés előtt látja és javíthatja.
     *
     * A felismerés szándékosan óvatos: amit nem ért, azt kihagyja, nem talál ki
     * edzést. Egy kitalált bejegyzés rosszabb, mint a hiányzó, mert a naplóba
     * kerül, és onnan a szériába, az XP-be és a statisztikába is.
     */
    public static Parsed parse(String text) {
        return parse(text, System.currentTimeMillis());
    }

    /** Tesztelhető változat: a „most" kívülről jön (a hétköznapnevekhez kell). */
    static Parsed parse(String text, long now) {
        List<Plan> out = new ArrayList<>();
        if (text == null) return new Parsed(out, 1, 0, 12);
        char[] q = Foods.norm(text).toCharArray();
        // A „kétszer", „3-szor" alakból szám lesz, mielőtt bármi más olvasná.
        stripMultiplicative(q);

        // 1) Időszak: „elmúlt 3 nap”, „3 nap alatt”, „a héten”. A megtalált részt
        //    kitakarjuk, hogy a benne lévő szám ne számítson edzés-darabszámnak.
        int days = 1, offset = 0;
        int[] span = findSpan(q);
        if (span != null) { days = span[2]; blank(q, span[0], span[1]); }
        else {
            // Konkrét dátum hónapnévvel: „július 28-án".
            int[] md = findMonthDay(q, now);
            if (md != null) { offset = md[2]; blank(q, md[0], md[1]); }
            else {
            // A „tegnap és ma" két nap: mától visszafelé oszlik el.
            int[] tm = findYesterdayAndToday(q);
            if (tm != null) {
                days = 2;
                blank(q, tm[0], tm[0] + 6);
                blank(q, tm[1], tm[1] + 2);
            } else {
                // A „hétvégén" a legutóbbi szombat–vasárnap, nem a mai nap.
                int[] we = findWeekend(q, now);
                if (we != null) { offset = we[2]; days = we[3]; blank(q, we[0], we[1]); }
                else {
                    // Konkrét nap megnevezve: „tegnap", „tegnapelőtt", „ma".
                    int[] one = findSingleDay(q, now);
                    if (one != null) { offset = one[2]; blank(q, one[0], one[1]); }
                }
            }
            }
        }
        // Az „1-1" osztó számnév és a „minden nap": naponta ennyi. A jelentésük
        // a napok számától függ, ezért csak a mozgásformák megtalálása UTÁN
        // válnak darabszámmá.
        int dist = stripDistributive(q);
        boolean daily = stripDaily(q);
        // Lépésszám: „10000 lépés", „tízezer lépést sétáltam". Kitakarjuk,
        // hogy a szám ne váljon darabszámmá; a terv a mozgások után épül rá.
        double steps = 0;
        double[] st = findSteps(q);
        if (st != null) { steps = st[2]; blank(q, (int) st[0], (int) st[1]); }

        // 2) Időtartamok: „45 perc”. Ezeket is kitakarjuk a darabszám elől,
        //    de a helyüket megjegyezzük, hogy a hozzájuk tartozó mozgáshoz
        //    rendelhessük.
        // Távok („10 km”, „2,5 km”): a mozgás-alapú sportokhoz tartoznak.
        // Kitakarjuk őket, hogy a bennük lévő szám ne legyen darabszám –
        // különben a „10 km futás” tíz külön futássá válna.
        List<double[]> kms = findKms(q);            // {pos, km, vég}
        for (double[] t : kms) blank(q, (int) t[0], (int) t[2]);
        List<int[]> mins = findMinutes(q);          // {pos, perc}
        for (int[] m : mins) blank(q, m[0], m[2]);

        // 3) Mozgásformák a maradék szövegben.
        String s = new String(q);
        List<int[]> hits = new ArrayList<>();       // {pos, len, kindIndex}
        for (int ki = 0; ki < ALL.length; ki++) {
            for (String w : ALL[ki].words) {
                int from = 0;
                while (true) {
                    int p = s.indexOf(w, from);
                    if (p < 0) break;
                    from = p + 1;
                    // Az „evez" tő a „nevez" végződése is (beNEVEZTem): az
                    // ilyen érzékeny tövek szó belsejében csak igekötő után
                    // érvényesek (kieveztem). Az összetett sportszavak
                    // (strandröplabda, gerincjóga) másik tövekkel mennek.
                    if (p > 0 && Character.isLetter(s.charAt(p - 1)) && w.startsWith("evez")) {
                        int a = p;
                        while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
                        if (!isVerbPrefix(s.substring(a, p))) continue;
                    }
                    hits.add(new int[]{p, w.length(), ki});
                }
            }
        }
        // A hosszabb találatba eső rövidebbet eldobjuk („kézi” a „kézilabda”-ban).
        List<int[]> keep = new ArrayList<>();
        for (int[] h : hits) {
            boolean covered = false;
            for (int[] o : hits) {
                if (o == h) continue;
                if (o[0] <= h[0] && o[0] + o[1] >= h[0] + h[1] && o[1] > h[1]) { covered = true; break; }
            }
            if (!covered) keep.add(h);
        }
        sortByPos(keep);

        // 4) Távok hozzárendelése: a legközelebbi táv-alapú mozgáshoz. A magyar
        //    mindkét szórendet használja („10 km futás”, „futottam 10 km-t”),
        //    ezért nem irány, hanem távolság dönt. Kézilabdához nem rendelünk
        //    távot – ott a szám nem jelent útvonalat.
        double[] kmOf = new double[keep.size()];
        for (double[] t : kms) {
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < keep.size(); i++) {
                if (!ALL[keep.get(i)[2]].distance) continue;
                double d = Math.abs(keep.get(i)[0] - t[0]);
                if (d < bestD) { bestD = d; best = i; }
            }
            if (best >= 0 && kmOf[best] == 0) kmOf[best] = t[1];
        }
        // Ha ugyanaz a mozgás kétszer szerepel („leFUTOTTAM a MARATONT"), a táv
        // a második találathoz is tapadhat – a terv viszont az elsőből készül.
        for (int i = 0; i < keep.size(); i++)
            for (int j = i + 1; j < keep.size(); j++)
                if (keep.get(i)[2] == keep.get(j)[2] && kmOf[i] == 0 && kmOf[j] > 0)
                    kmOf[i] = kmOf[j];

        // 5) Minden találathoz darabszám (előtte) és időtartam (utána).
        boolean[] used = new boolean[ALL.length];
        for (int i = 0; i < keep.size(); i++) {
            int[] h = keep.get(i);
            if (used[h[2]]) continue;               // egy mozgásforma egyszer szerepel
            used[h[2]] = true;
            Kind kind = ALL[h[2]];
            int count = countBefore(s, h[0]);
            // A „100 fekvőtámasz" száz ISMÉTLÉS, nem száz edzés – az
            // ismétlés-szavaknál a nagy szám egyetlen alkalom, és az időt is
            // az ismétlésszámból becsüljük.
            int reps = 0;
            if (isRepWord(s.substring(h[0], Math.min(s.length(), h[0] + h[1])))
                    && count > 3) {
                // A nyers szám kell: a darabszám-korlát (50) az ismétlésekre
                // nem vonatkozik – száz fekvőtámasz létezik.
                int[] raw = numberBefore(s, h[0], NUM_REACH);
                reps = raw != null ? Math.max(count, Math.min(1000, raw[2])) : count;
                // A súlyzós jelölés: „3x10" = három sorozat tíz ismétlés, azaz
                // harminc – a szorzat számít, nem csak az utolsó szám.
                if (raw != null && raw[0] >= 2 && s.charAt(raw[0] - 1) == 'x'
                        && Character.isDigit(s.charAt(raw[0] - 2))) {
                    int e = raw[0] - 1, b = e;
                    while (b > 0 && Character.isDigit(s.charAt(b - 1))) b--;
                    try {
                        reps = Math.min(1000,
                                Integer.parseInt(s.substring(b, e)) * raw[2]);
                    } catch (NumberFormatException ignore) { }
                }
                count = 1;
            }
            int next = i + 1 < keep.size() ? keep.get(i + 1)[0] : Integer.MAX_VALUE;
            int minutes = minutesFor(mins, h[0], next, 0);
            if (minutes <= 0)
                // Nincs kimondott időtartam: távból vagy ismétlésből becsülünk,
                // anélkül a mozgásforma szokásos hossza jön.
                minutes = reps > 0
                        ? Math.max(5, Math.min(60, reps / 5))
                        : kmOf[i] > 0
                        ? Math.max(1, (int) Math.round(kmOf[i] * minPerKm(kind)))
                        : kind.defaultMin;
            // A távból becsült hossz is maradjon egy napon belül (100 km úszás
            // tempóból számolva 41 óra lenne).
            minutes = Math.min(minutes, 24 * 60);
            out.add(new Plan(kind, count, minutes, kmOf[i]));
        }

        // Ha nincs felismert mozgás, de van táv, az futás: a „nyomtam egy
        // 5 km-t" magyarul futást jelent.
        if (out.isEmpty() && !kms.isEmpty()) {
            Kind run = byId("futas");
            double km0 = kms.get(0)[1];
            out.add(new Plan(run, 1,
                    Math.min(24 * 60, Math.max(1, (int) Math.round(km0 * minPerKm(run)))),
                    km0));
        }

        // Ha semmilyen mozgásformát nem ismertünk fel, a puszta „N edzés" még
        // menthető: egyéb mozgásként. Csak tartalékként, mert a „3 futó edzés"
        // szóban is benne van az „edzés" – ott a futás a helyes válasz.
        if (out.isEmpty()) {
            for (String w : new String[]{"edzes", "edzett", "edzeni", "alkalom", "mozgas"}) {
                int p = s.indexOf(w);
                if (p < 0) continue;
                Kind other = byId("egyeb");
                // A kimondott időtartam itt is számít („otthoni edzés 40 perc").
                if (other != null) out.add(new Plan(other, countBefore(s, p),
                        minutesFor(mins, p, Integer.MAX_VALUE, other.defaultMin), 0));
                break;
            }
        }
        // A lépésszám túra/gyaloglás: időt (~130 lépés/perc) és távot
        // (~75 cm/lépés) is jelent. Ha séta/túra már szerepel a mondatban,
        // azt egészíti ki – nem lesz belőle második bejegyzés.
        if (steps > 0) {
            int smin = Math.max(10, Math.min(24 * 60, (int) Math.round(steps / 130.0)));
            double skm = Math.round(steps * 0.00075 * 10) / 10.0;
            int ti = -1;
            for (int i = 0; i < out.size(); i++)
                if (out.get(i).kind.id.equals("tura")) ti = i;
            if (ti < 0) out.add(new Plan(byId("tura"), 1, smin, skm));
            else {
                Plan t = out.get(ti);
                // A kimondott idő (ami eltér az alapértelmezettől) erősebb.
                int m = t.minutes == t.kind.defaultMin ? smin : t.minutes;
                out.set(ti, new Plan(t.kind, t.count, m, t.km > 0 ? t.km : skm));
            }
        }

        // A naponkénti alakok kibontása: EGY mozgásnál a darabszám naponta
        // értendő („tegnap és ma 1-1 futás" = két futás, „a héten minden nap
        // futottam" = hét futás, „naponta kétszer" = 2 × napok). Több mozgásnál
        // fejenként egyet jelent („1-1 kézi és foci"), ott a darabszám már jó.
        if ((dist > 0 || daily) && days > 1 && out.size() == 1) {
            Plan p0 = out.get(0);
            out.set(0, new Plan(p0.kind, Math.min(50, p0.count * days), p0.minutes, p0.km));
        }
        return new Parsed(out, days, offset, findHour(s));
    }

    private static final String[] MONTHS = {"januar", "februar", "marcius", "aprilis",
            "majus", "junius", "julius", "augusztus", "szeptember", "oktober",
            "november", "december"};

    /**
     * Konkrét dátum hónapnévvel: „július 28-án" → {kezdet, vég, hány napja}.
     * A legutóbbi ilyen dátum: ha az idei még nem volt meg, a tavalyi. A rag
     * és a nap száma is a kitakart részhez tartozik, hogy a szám ne váljon
     * darabszámmá. A puszta „júliusban" (nap nélkül) nem dátum.
     */
    private static int[] findMonthDay(char[] q, long now) {
        String s = new String(q);
        for (int mi = 0; mi < MONTHS.length; mi++) {
            int p = s.indexOf(MONTHS[mi]);
            if (p < 0) continue;
            if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
            int i = p + MONTHS[mi].length();
            if (i < s.length() && Character.isLetter(s.charAt(i))) continue; // „júliusban"
            int j = i;
            while (j < s.length() && s.charAt(j) == ' ') j++;
            int d = 0, k = j;
            while (k < s.length() && Character.isDigit(s.charAt(k))) {
                d = d * 10 + (s.charAt(k) - '0');
                k++;
            }
            if (k == j || d < 1 || d > 31) continue;
            while (k < s.length() && (s.charAt(k) == '-' || s.charAt(k) == '.'
                    || Character.isLetter(s.charAt(k)))) k++;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            cal.set(java.util.Calendar.MONTH, mi);
            if (d > cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)) continue;
            cal.set(java.util.Calendar.DAY_OF_MONTH, d);
            if (cal.getTimeInMillis() > now) cal.add(java.util.Calendar.YEAR, -1);
            int back = Days.between(cal.getTimeInMillis(), now);
            if (back < 0 || back > 365) continue;
            return new int[]{p, k, back};
        }
        return null;
    }

    /**
     * „Tegnap és ma" → {tegnap kezdete, ma kezdete}. Mindkét szónak külön kell
     * szerepelnie – a „tegnapelőtt" nem ez az eset.
     */
    private static int[] findYesterdayAndToday(char[] q) {
        String s = new String(q);
        int t = s.indexOf("tegnap");
        if (t < 0 || s.startsWith("tegnapelott", t)) return null;
        // Önálló „ma" szó (nem szórészlet, és nem a „tegnap" belseje).
        int from = 0;
        while (true) {
            int m = s.indexOf("ma", from);
            if (m < 0) return null;
            from = m + 1;
            if (m >= t && m < t + 6) continue;
            if (m > 0 && Character.isLetter(s.charAt(m - 1))) continue;
            if (m + 2 < s.length() && Character.isLetter(s.charAt(m + 2))) continue;
            return new int[]{t, m};
        }
    }

    /**
     * Az „1-1" (és az „egy-egy") osztó számnév: a kötőjeles részt kitakarjuk,
     * az értékét visszaadjuk – a kibontás a mozgások ismeretében történik.
     * A „10-15 perc" tartomány nem ez: ott a két szám különbözik.
     */
    private static int stripDistributive(char[] q) {
        String s = new String(q);
        for (int i = 0; i + 2 < s.length(); i++) {
            if (Character.isDigit(s.charAt(i)) && s.charAt(i + 1) == '-'
                    && s.charAt(i + 2) == s.charAt(i)
                    && (i == 0 || !Character.isDigit(s.charAt(i - 1)))
                    && (i + 3 >= s.length() || !Character.isDigit(s.charAt(i + 3)))) {
                q[i + 1] = ' ';
                q[i + 2] = ' ';
                return s.charAt(i) - '0';
            }
        }
        int p = s.indexOf("egy-egy");
        if (p >= 0) { blank(q, p + 3, p + 7); return 1; }
        return 0;
    }

    /**
     * Lépésszám a szövegben: „10000 lépés", „10 ezer lépés", „tízezer lépés"
     * → {kezdet, vég, lépések}. 500 alatt és 100 000 felett nem hisszük el.
     */
    private static double[] findSteps(char[] q) {
        String s = new String(q);
        int p = s.indexOf("lepes");
        if (p < 0) return null;
        if (p > 0 && Character.isLetter(s.charAt(p - 1))) return null;
        int end = p + 5;
        while (end < s.length() && Character.isLetter(s.charAt(end))) end++;
        int we = p;
        while (we > 0 && s.charAt(we - 1) == ' ') we--;
        double mult = 1;
        int numEnd = we;
        if (we >= 4 && s.startsWith("ezer", we - 4)) {
            mult = 1000;
            numEnd = we - 4;
            while (numEnd > 0 && s.charAt(numEnd - 1) == ' ') numEnd--;
        }
        int numStart = numEnd;
        while (numStart > 0 && Character.isDigit(s.charAt(numStart - 1))) numStart--;
        double val;
        if (numStart < numEnd) {
            try { val = Double.parseDouble(s.substring(numStart, numEnd)); }
            catch (NumberFormatException e) { return null; }
        } else if (mult == 1000) {
            // Kiírt számnév egyben: „tízezer" (a norm után: tizezer).
            int a = numEnd;
            while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
            String w = s.substring(a, numEnd);
            val = 1;                                   // puszta „ezer lépés"
            for (String[] nw : NUM_WORDS)
                if (nw[0].equals(w)) { val = Integer.parseInt(nw[1]); numStart = a; break; }
        } else return null;
        double steps = val * mult;
        if (steps < 500 || steps > 100000) return null;
        return new double[]{numStart, end, steps};
    }

    /** Magyar igekötők: ami utánuk áll, az az ige töve (ki-eveztem). */
    private static boolean isVerbPrefix(String pre) {
        for (String v : new String[]{"le", "be", "meg", "el", "ki", "fel", "at",
                "ra", "oda", "vissza", "ossze", "szet", "vegig", "korbe"})
            if (v.equals(pre)) return true;
        return false;
    }

    /** Ismétlés-alapú gyakorlatszavak: előttük a nagy szám ismétlés, nem alkalom. */
    private static boolean isRepWord(String w) {
        for (String r : new String[]{"fekvotamasz", "guggolas", "felules",
                "huzodzkodas", "plank"})
            if (w.startsWith(r)) return true;
        return false;
    }

    /** „Minden nap", „naponta": a darabszám naponta értendő. */
    private static boolean stripDaily(char[] q) {
        String s = new String(q);
        for (String w : new String[]{"minden nap", "mindennap", "naponta"}) {
            int p = s.indexOf(w);
            if (p >= 0) { blank(q, p, p + w.length()); return true; }
        }
        return false;
    }

    /**
     * Kimondott napszak → óra. A múltbeli bejegyzés így nem a semleges délre
     * kerül, ha a felhasználó megmondta, mikor volt („tegnap este kondi").
     */
    private static int findHour(String s) {
        String[][] tod = {{"reggel", "8"}, {"delelott", "10"}, {"delutan", "16"},
                {"este", "19"}, {"esti", "19"}, {"ejszaka", "22"}, {"ejjel", "22"}};
        for (String[] w : tod) {
            int p = s.indexOf(w[0]);
            if (p < 0) continue;
            // Szó eleje legyen („napeste" nincs, de a „testes" ne találjon).
            if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
            return Integer.parseInt(w[1]);
        }
        return 12;
    }

    /** „elmúlt 3 nap”, „3 nap alatt”, „a héten”, „egy hónap alatt” → {kezdet, vég, napok}. */
    private static int[] findSpan(char[] q) {
        String s = new String(q);
        // Egy hét = 7 nap, egy hónap = 30. A legkorábbi találat dönt.
        int[] best = null;
        for (int[] c : new int[][]{spanAt(s, "nap", 1), spanAt(s, "het", 7), spanAt(s, "honap", 30)})
            if (c != null && (best == null || c[0] < best[0])) best = c;
        return best;
    }

    /**
     * A „kétszer", „háromszor", „3-szor" alak darabszám, de a számnév-kereső
     * szóhatárt vár, így a rag miatt nem találta meg: a „kétszer úsztam" EGY
     * úszás lett. A ragot kitakarjuk, a szám ott marad.
     */
    private static void stripMultiplicative(char[] q) {
        String s = new String(q);
        for (String suf : new String[]{"szor", "szer"}) {
            int from = 0;
            while (true) {
                int p = s.indexOf(suf, from);
                if (p < 0) break;
                from = p + 1;
                int wordEnd = p + suf.length();
                while (wordEnd < s.length() && Character.isLetter(s.charAt(wordEnd))) wordEnd++;
                if (p > 1 && s.charAt(p - 1) == '-' && Character.isDigit(s.charAt(p - 2)))
                    blank(q, p - 1, wordEnd);          // „3-szor"
                else if (p > 0 && Character.isDigit(s.charAt(p - 1)))
                    blank(q, p, wordEnd);              // „3szor"
                else {
                    int a = p;
                    while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
                    String prefix = s.substring(a, p); // „ketszer" → „ket"
                    for (String[] w : NUM_WORDS)
                        if (w[0].equals(prefix)) { blank(q, p, wordEnd); break; }
                }
            }
        }
    }

    private static int[] spanAt(String s, String unit, int mult) {
        int from = 0;
        while (true) {
            int p = s.indexOf(unit, from);
            if (p < 0) return null;
            from = p + 1;
            // A ragozott alak jó (napban, héten), a hasonló hangzású
            // szavak viszont nem (hétfőn, naplóban).
            int end = p + unit.length();
            while (end < s.length() && Character.isLetter(s.charAt(end))) end++;
            if (isNotSpan(wordAt(s, p))) continue;
            int[] n = numberBefore(s, p, NUM_REACH);
            if (n == null) {
                // Szám nélkül csak a hét és a hónap időszak („a héten",
                // „a hónapban") – a puszta „nap" nem.
                if (mult == 7 || mult == 30) return new int[]{p, end, mult};
                continue;
            }
            int val = Math.max(1, Math.min(365, n[2] * mult));
            return new int[]{n[0], end, val};
        }
    }

    /** „45 perc”, „másfél óra” helyett egyszerűen: szám + perc/óra. */
    /** A teljes szó a megadott pozíció körül. */
    private static String wordAt(String s, int p) {
        int a = p, b = p;
        while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
        while (b < s.length() && Character.isLetter(s.charAt(b))) b++;
        return s.substring(a, b);
    }

    private static boolean isNotSpan(String word) {
        for (String w : NOT_SPAN) if (w.equals(word)) return true;
        return false;
    }

    /**
     * „Hétvégén" → {kezdet, vég, eltolás, napok}: a legutóbbi szombat–vasárnap.
     *
     * Hétköznap írva a múlt hétvége két napja (vasárnap az eltolás, előtte a
     * szombat). Szombaton írva a ma (egy nap), vasárnap írva a tegnap-ma kettő.
     */
    private static int[] findWeekend(char[] q, long now) {
        String s = new String(q);
        int p = s.indexOf("hetveg");
        if (p < 0) return null;
        if (p > 0 && Character.isLetter(s.charAt(p - 1))) return null;
        int end = p + 6;
        while (end < s.length() && Character.isLetter(s.charAt(end))) end++;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);   // vasárnap=1 … szombat=7
        int offset, days;
        if (dow == java.util.Calendar.SATURDAY) { offset = 0; days = 1; }
        else if (dow == java.util.Calendar.SUNDAY) { offset = 0; days = 2; }
        else { offset = dow - 1; days = 2; }                 // hétfő→1 … péntek→5
        return new int[]{p, end, offset, days};
    }

    /**
     * Konkrét nap megnevezve → {kezdet, vég, hány napja}.
     *
     * A „tegnap"/„tegnapelőtt" mellett a hétköznapnevek is: a „hétfőn
     * futottam" a legutóbbi hétfőre kerül, nem a mai napra. Ha ma van az a
     * nap, akkor a mai (0) – aki pénteken írja, hogy „pénteken úsztam", az a
     * mairól beszél.
     */
    private static int[] findSingleDay(char[] q, long now) {
        String s = new String(q);
        String[][] words = {{"tegnapelott", "2"}, {"tegnapi", "1"}, {"tegnap", "1"}};
        for (String[] w : words) {
            int p = s.indexOf(w[0]);
            if (p < 0) continue;
            return new int[]{p, p + w[0].length(), Integer.parseInt(w[1])};
        }
        // Hétköznapnevek (Calendar.DAY_OF_WEEK: vasárnap=1 … szombat=7).
        String[][] dows = {{"hetfo", "2"}, {"kedd", "3"}, {"szerda", "4"},
                {"csutortok", "5"}, {"pentek", "6"}, {"szombat", "7"}, {"vasarnap", "1"}};
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        int today = cal.get(java.util.Calendar.DAY_OF_WEEK);
        for (String[] w : dows) {
            int p = s.indexOf(w[0]);
            if (p < 0) continue;
            if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
            int end = p + w[0].length();
            while (end < s.length() && Character.isLetter(s.charAt(end))) end++;
            int back = (today - Integer.parseInt(w[1]) + 7) % 7;
            return new int[]{p, end, back};
        }
        return null;
    }

    /**
     * Távok a szövegben: szám (tizedesvesszővel is) + „km” vagy „kilométer”.
     * {kezdet, km, vég} hármasok – a kezdet/vég a kitakaráshoz kell.
     */
    private static List<double[]> findKms(char[] q) {
        String s = new String(q);
        List<double[]> out = new ArrayList<>();
        // Méter is: úszásnál az a természetes egység („leúsztam 2000 métert").
        for (String unit : new String[]{"kilometer", "km", "meter", "m"}) {
            boolean meters = unit.equals("meter") || unit.equals("m");
            int from = 0;
            while (true) {
                int p = s.indexOf(unit, from);
                if (p < 0) break;
                from = p + 1;
                // A „km” ne egy szó belsejéből jöjjön.
                if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
                // A puszta „m" ne egy szó ELEJE legyen („3 meccs” nem 3 méter).
                if (unit.equals("m") && p + 1 < s.length()
                        && Character.isLetter(s.charAt(p + 1))) continue;
                int numEnd = p;
                while (numEnd > 0 && s.charAt(numEnd - 1) == ' ') numEnd--;
                int numStart = numEnd;
                boolean dot = false;
                while (numStart > 0) {
                    char c = s.charAt(numStart - 1);
                    if (Character.isDigit(c)) { numStart--; continue; }
                    if ((c == ',' || c == '.') && !dot && numStart - 1 > 0
                            && Character.isDigit(s.charAt(numStart - 2))) {
                        dot = true; numStart--; continue;
                    }
                    break;
                }
                if (numStart == numEnd) continue;      // nincs előtte szám
                double val;
                try {
                    val = Double.parseDouble(s.substring(numStart, numEnd).replace(',', '.'));
                } catch (NumberFormatException e) { continue; }
                if (meters) {
                    // 25 méter alatt nem edzés, 100 km felett elgépelés.
                    if (val < 25 || val > 100000) continue;
                    val /= 1000.0;
                }
                if (val <= 0 || val > 500) continue;
                // A ragozott vég („km-t”, „kilométert”) is a kitakart részhez tartozik.
                int end = p + unit.length();
                while (end < s.length()
                        && (Character.isLetter(s.charAt(end)) || s.charAt(end) == '-')) end++;
                out.add(new double[]{numStart, val, end});
            }
        }
        // A maraton neve maga a táv: 42,2 km, a félmaraton 21,1. A szót nem
        // takarjuk ki (kezdet = vég), mert egyben a futás szótöve is – ha
        // kimondott km is áll mellette, az nyer, mert előrébb áll a listában.
        int mp = s.indexOf("maraton");
        if (mp >= 0) {
            boolean half = mp >= 3 && s.startsWith("fel", mp - 3);
            if (!half) {
                int we = mp;
                while (we > 0 && s.charAt(we - 1) == ' ') we--;
                half = we >= 3 && s.startsWith("fel", we - 3)
                        && (we < 4 || !Character.isLetter(s.charAt(we - 4)));
            }
            out.add(new double[]{mp, half ? 21.1 : 42.2, mp});
        }
        return out;
    }

    private static List<int[]> findMinutes(char[] q) {
        String s = new String(q);
        List<int[]> out = new ArrayList<>();
        for (String unit : new String[]{"perc", "ora"}) {
            int from = 0;
            while (true) {
                int p = s.indexOf(unit, from);
                if (p < 0) break;
                from = p + 1;
                // A „fél óra" és a „másfél óra" nem egész számnév – külön ág.
                if (unit.equals("ora")) {
                    int we = p;
                    while (we > 0 && s.charAt(we - 1) == ' ') we--;
                    int wsPos = we;
                    while (wsPos > 0 && Character.isLetter(s.charAt(wsPos - 1))) wsPos--;
                    String prev = s.substring(wsPos, we);
                    int frac = prev.equals("fel") ? 30
                            : prev.equals("masfel") ? 90
                            : prev.equals("negyed") ? 15
                            : prev.equals("haromnegyed") ? 45 : 0;
                    if (frac > 0) {
                        // „Két és fél óra": az egész órák a tört elé kerülnek,
                        // „és"-sel kötve – nélkülük a kettő elveszett, és fél
                        // óra maradt.
                        int start = wsPos;
                        int b = wsPos;
                        while (b > 0 && s.charAt(b - 1) == ' ') b--;
                        if (b >= 2 && s.startsWith("es", b - 2)
                                && (b - 2 == 0 || !Character.isLetter(s.charAt(b - 3)))) {
                            int c = b - 2;
                            while (c > 0 && s.charAt(c - 1) == ' ') c--;
                            int numStart = c, whole = 0;
                            while (numStart > 0 && Character.isDigit(s.charAt(numStart - 1)))
                                numStart--;
                            if (numStart < c) {
                                try { whole = Integer.parseInt(s.substring(numStart, c)); }
                                catch (NumberFormatException ignore) { }
                            } else {
                                int a2 = c;
                                while (a2 > 0 && Character.isLetter(s.charAt(a2 - 1))) a2--;
                                String w2 = s.substring(a2, c);
                                for (String[] nw : NUM_WORDS)
                                    if (nw[0].equals(w2)) {
                                        whole = Integer.parseInt(nw[1]);
                                        numStart = a2;
                                        break;
                                    }
                            }
                            if (whole > 0 && whole <= 24) {
                                frac += whole * 60;
                                start = numStart;
                            }
                        }
                        out.add(new int[]{start, frac, p + unit.length(), 1});
                        continue;
                    }
                }
                // Tizedes is lehet („1,5 óra"): az egész-számnév-kereső a vessző
                // utáni 5-öt látta volna, és 5 órának értette – ami elé ráadásul
                // az „1" darabszámként csúszott be.
                int numEnd2 = p;
                while (numEnd2 > 0 && s.charAt(numEnd2 - 1) == ' ') numEnd2--;
                int numStart2 = numEnd2;
                boolean dot2 = false;
                while (numStart2 > 0) {
                    char c2 = s.charAt(numStart2 - 1);
                    if (Character.isDigit(c2)) { numStart2--; continue; }
                    if ((c2 == ',' || c2 == '.') && !dot2 && numStart2 - 1 > 0
                            && Character.isDigit(s.charAt(numStart2 - 2))) {
                        dot2 = true; numStart2--; continue;
                    }
                    break;
                }
                int val;
                int numPos;
                if (numStart2 < numEnd2) {
                    double d2;
                    try {
                        d2 = Double.parseDouble(
                                s.substring(numStart2, numEnd2).replace(',', '.'));
                    } catch (NumberFormatException e2) { continue; }
                    val = (int) Math.round(d2 * (unit.equals("ora") ? 60 : 1));
                    numPos = numStart2;
                } else {
                    // A leghosszabb összetett számnév („kilencvenkilenc") is
                    // beleférjen a visszanézésbe.
                    int[] n = numberBefore(s, p, 18);
                    if (n == null) continue;
                    val = unit.equals("ora") ? n[2] * 60 : n[2];
                    numPos = n[0];
                }
                if (val < 1 || val > 24 * 60) continue;
                out.add(new int[]{numPos, val, p + unit.length(), unit.equals("ora") ? 1 : 0});
            }
        }
        // Az „1 óra 15 perc" EGY időtartam: az óra utáni percet hozzáadjuk,
        // különben a perc külön (rövidebb) időtartamnak számítana.
        sortByPos(out);
        for (int i = 0; i + 1 < out.size(); i++) {
            int[] a = out.get(i), b = out.get(i + 1);
            if (a[3] != 1 || b[3] != 0 || a[2] > b[0]) continue;
            String gap = s.substring(a[2], b[0]).trim();
            if (!gap.isEmpty() && !gap.equals("es")) continue;
            out.set(i, new int[]{a[0], a[1] + b[1], b[2], 0});
            out.remove(i + 1);
        }
        return out;
    }

    /** A megadott mozgáshoz tartozó időtartam, vagy az alapértelmezett. */
    private static int minutesFor(List<int[]> mins, int at, int nextAt, int fallback) {
        for (int[] m : mins) if (m[0] > at && m[0] < nextAt) return m[1];
        // Ha az egész mondatban EGY időtartam van, az mindenkire vonatkozik
        // („az elmúlt héten 3 futás és 2 úszás, 40 perc”).
        if (mins.size() == 1) return mins.get(0)[1];
        return fallback;
    }

    /** Darabszám a mozgás neve előtt; ha nincs, egy alkalom. */
    private static int countBefore(String s, int at) {
        int[] n = numberBefore(s, at, NUM_REACH);
        if (n == null) return 1;
        return Math.max(1, Math.min(50, n[2]));
    }

    /**
     * A megadott pozíció ELŐTT álló legközelebbi szám (számjegy vagy kiírt
     * számnév), a {pos, vég, érték} hármassal. Csak akkor fogadjuk el, ha a
     * szám és a szó között nincs más betű – vagyis tényleg ahhoz tartozik.
     */
    private static int[] numberBefore(String s, int at, int reach) {
        int start = Math.max(0, at - reach);
        int best = -1, bestEnd = -1, bestVal = 0;
        for (int i = start; i < at; i++) {
            if (Character.isDigit(s.charAt(i)) && (i == 0 || !Character.isDigit(s.charAt(i - 1)))) {
                int j = i;
                while (j < at && Character.isDigit(s.charAt(j))) j++;
                if (!onlyFiller(s, j, at)) continue;
                try { bestVal = Integer.parseInt(s.substring(i, j)); } catch (Exception e) { continue; }
                best = i; bestEnd = j;
            }
        }
        if (best >= 0) return new int[]{best, bestEnd, bestVal};
        for (String[] w : NUM_WORDS) {
            int p = s.lastIndexOf(w[0], at - 1);
            if (p < start) continue;
            int end = p + w[0].length();
            if (end > at) continue;
            if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
            if (end < s.length() && Character.isLetter(s.charAt(end))) continue;
            if (!onlyFiller(s, end, at)) continue;
            return new int[]{p, end, Integer.parseInt(w[1])};
        }
        return null;
    }

    /**
     * A szám és a szó között csak szóköz, írásjel vagy jelentéktelen töltelék
     * áll? Ha valódi szó van közte, a szám nem ehhez tartozik.
     */
    private static final String[] FILLER = {"db", "darab", "alkalom", "meccs", "kb", "x"};

    private static boolean onlyFiller(String s, int from, int to) {
        String mid = s.substring(from, to);
        int i = 0;
        while (i < mid.length()) {
            if (!Character.isLetterOrDigit(mid.charAt(i))) { i++; continue; }
            int j = i;
            while (j < mid.length() && Character.isLetterOrDigit(mid.charAt(j))) j++;
            String tok = mid.substring(i, j);
            boolean ok = false;
            // A töltelékszó ragozva is az („2 meccsen kézi", „3 darabot") –
            // a rövidekre (db, x) viszont csak a pontos alak biztonságos.
            for (String f : FILLER)
                if (tok.equals(f) || (f.length() >= 3 && tok.startsWith(f))) { ok = true; break; }
            if (!ok) return false;
            i = j;
        }
        return true;
    }

    private static void blank(char[] q, int from, int to) {
        for (int i = Math.max(0, from); i < Math.min(q.length, to); i++) q[i] = ' ';
    }

    private static void sortByPos(List<int[]> list) {
        for (int i = 0; i < list.size(); i++)
            for (int j = i + 1; j < list.size(); j++)
                if (list.get(j)[0] < list.get(i)[0]) {
                    int[] t = list.get(i); list.set(i, list.get(j)); list.set(j, t);
                }
    }
}
