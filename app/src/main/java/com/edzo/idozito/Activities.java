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
                    "maraton", "futkaroz", "futkos", "sprint", "futopad", "futogep",
                    // A verseny neve is a sportot mondja ki – a puszta „futó"
                    // viszont nem lehet tő, mert túl sok szóban benne van.
                    "futoverseny", "terepfutas", "spartan", "parkrun"),
            new Kind("uszas", "🏊", "Úszás", 7.0, true, 45,
                    "uszas", "uszo edzes", "uszni", "uszoedzes", "uszodaz", "uszt", "uszkal",
                    "uszoverseny",
                    // A vizes sportok is ide: a vízilabda és a vizitorna a
                    // medencés mozgások közül az úszáshoz áll a legközelebb.
                    "vizilabda", "aquafit", "vizitorna"),
            new Kind("kerekpar", "🚴", "Kerékpár", 7.5, true, 60,
                    "kerekpar", "bringa", "bicikli", "bicaj", "canga", "teker", "bmx",
                    // A spinning teremben zajlik, de a lába ugyanazt csinálja:
                    // a tánc MET-je alábecsülte.
                    "spinning", "szobabicikli", "spinning ora",
                    // A „bringatúra" egyben fedi a „bringa" és a „túra" tövet is.
                    "bringatura", "biciklitura", "kerekpartura"),
            new Kind("tura", "🥾", "Túra / gyaloglás", 5.3, true, 90,
                    "tura", "gyaloglas", "seta", "setalas", "kirandul", "nordic",
                    "hegymasz", "megmaszt", "gyalog", "lepcsoz", "babakocsi"),
            new Kind("evezes", "🚣", "Evezés / evezőgép", 7.0, true, 30,
                    "evezes", "evezo", "evezt", "kajak", "sup deszka"),
            new Kind("kondi", "🏋", "Kondi / súlyzós edzés", 5.0, false, 60,
                    "kondi", "konditerem", "terem", "sulyzo", "gym", "gepterem", "gyur",
                    // A „tornaterem" egyben fedi a „torna" (jóga) és a „terem"
                    // (kondi) tövet is – a hosszabb tő nyer, így egy találat lesz.
                    "crossfit", "kroszfit", "trx", "erosit", "fekvotamasz", "tornaterem", "wod",
                    "koredzes", "kor edzes",
                    "guggolas", "felules", "huzodzkodas", "plank", "tabata",
                    "labnap", "mellnap", "vallnap", "karnap", "akadalypalya",
                    // Termi óranevek és gépek: enélkül a bejegyzés elveszett.
                    // (Az elliptikus és a crosstrainer az „egyéb" alatt van.)
                    "body pump", "bodypump", "stepper",
                    "kettlebell"),
            new Kind("kezilabda", "🤾", "Kézilabda", 8.0, false, 90,
                    "kezilabda", "kezi edzes", "keziedzes", "kezi"),
            new Kind("foci", "⚽", "Foci", 7.0, false, 90,
                    "foci", "focizas", "labdarugas", "focizt", "futball"),
            new Kind("kosarlabda", "🏀", "Kosárlabda", 6.5, false, 60,
                    // A puszta „kosár" nem sport: a bevásárlókosár is az.
                    "kosarlabda", "kosaraz", "kosar edzes"),
            new Kind("roplabda", "🏐", "Röplabda", 4.0, false, 60,
                    "roplabda", "roplab", "roplabdaz"),
            new Kind("tenisz", "🎾", "Tenisz / squash / tollas", 7.3, false, 60,
                    "tenisz", "squash", "fallabda", "tollaslabda", "tollas", "pingpong",
                    "ping pong", "asztalitenisz", "padel"),
            new Kind("harcmuveszet", "🥋", "Harcművészet / box", 10.0, false, 60,
                    "harcmuvesz", "kickbox", "box", "boksz", "karate", "judo", "birkozas",
                    "birkoz", "mma", "jiu-jitsu", "jiujitsu", "jiu jitsu", "bjj", "grappling",
                    "aikido", "onvedelm", "vivas"),
            new Kind("tanc", "💃", "Tánc / aerobik", 5.5, false, 60,
                    "tanc", "aerobik", "zumba", "kangoo", "alakformalo", "balett"),
            new Kind("joga", "🧘", "Jóga / nyújtás / pilates", 3.0, false, 45,
                    // A „torna" fedi a gerinctornát, gyógytornát, tornázást is.
                    // A „nyujt" tő az igét is fedi: nyújtás, nyújtottam, nyújtok.
                    "joga", "yoga", "pilates", "nyujt", "stretch", "torna", "medital",
                    "meditac", "atmozgat", "mobiliz", "mobilitas", "legzogyakorlat",
                    "legzo gyakorlat"),
            new Kind("korcsolya", "⛸", "Korcsolya / görkorcsolya", 7.0, false, 60,
                    "korcsolya", "gorkorcsolya", "gorkori", "gordeszka", "roller",
                    "jegkorong", "hoki", "curling"),
            // A sífutás táv-alapú: a „20 km sífutás" távja is számít.
            new Kind("si", "🎿", "Sí / snowboard", 6.0, true, 120,
                    // A „sízem/síztem/sízni" alakok is: a puszta „si" nem
                    // lehet szótő (a HASIZOMban is benne van).
                    "siel", "sizes", "siztem", "sizni", "sizunk", "sizik", "sizel",
                    "snowboard", "sifutas", "sifut"),
            new Kind("fal", "🧗", "Falmászás", 8.0, false, 60,
                    "falmaszas", "falmasz", "maszas", "sziklamasz",
                    "boulder", "maszofal"),
            new Kind("munka", "🌳", "Kerti / fizikai munka", 4.0, false, 60,
                    "kerti munka", "fizikai munka", "kertesz", "favag", "fat vag", "lapatolas",
                    "takarit", "funyir", "koltoz", "asas", "kapalas", "kapal", "gereblyez",
                    "lapatol",
                    "ablakpucol", "porszivoz"),
            new Kind("egyeb", "🤸", "Egyéb mozgás", 6.0, false, 45,
                    // A „kardió" edzés-szó: enélkül a „45 perc kardió" semmi
                    // volt. (Az étel-oldalon ugyanez a szó a diót hozta.)
                    // A puszta „tekez" nem elég: az érTEKEZletben is benne van.
                    "kardio", "bowling", "tekepalya", "tekezes", "tekeztem", "tekezni",
                    "tekezunk", "tekezik",
                    "egyeb mozgas", "egyeb edzes", "egyeb", "sportol", "mozog",
                    "lovagl", "lovagol", "vitorlaz", "szorf", "wakeboard", "golf",
                    "ellipszis", "elliptikus", "crosstrainer", "cross trainer",
                    "jatszoter", "lepcsozo", "trambulin", "ugrokotel", "ugralokotel",
                    "ugralo kotel", "hulahopp", "kotelugras"),
    };

    /** A mozgásforma azonosító alapján, vagy null, ha nem ismerjük. */
    public static Kind byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Kind k : ALL) if (k.id.equals(id)) return k;
        return null;
    }

    /**
     * Illik-e egy naplóbejegyzés a keresőszóra?
     *
     * Az erősítő naplóban régóta lehet keresni, az edzés-előzményekben nem –
     * pedig ott gyűlik a legtöbb bejegyzés. A keresés a sportág nevét, a
     * program nevét és a jegyzetet is nézi, ráadásul a sportág SZÓTÖVEIT is:
     * aki „bicikli"-t ír, a kerékpáros edzéseket keresi, nem a „Kerékpár"
     * szó pontos alakját.
     *
     * @param kindId a bejegyzés sportág-azonosítója (lehet üres)
     * @param name   program- vagy sportnév a bejegyzésből (lehet null)
     * @param note   a bejegyzéshez fűzött jegyzet (lehet null)
     * @param query  a keresőmező tartalma; üresre minden illik
     */
    public static boolean matches(String kindId, String name, String note, String query) {
        String q = Foods.norm(query == null ? "" : query).trim();
        if (q.isEmpty()) return true;
        if (name != null && Foods.norm(name).contains(q)) return true;
        if (note != null && Foods.norm(note).contains(q)) return true;
        Kind k = byId(kindId);
        // Kind nélküli (mért) bejegyzés futásnak számít – ahogy a szűrőnél is.
        if (k == null && (kindId == null || kindId.isEmpty())
                && (name == null || name.isEmpty())) k = byId("futas");
        if (k == null && name != null) k = kindByText(name);
        if (k == null) return false;
        if (Foods.norm(k.name).contains(q) || k.id.contains(q)) return true;
        for (String st : k.words) if (st.contains(q) || q.contains(st)) return true;
        return false;
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
        /** Kimondott lépésszám („ma 10000 lépés"; 0 = nincs). */
        public final int steps;
        Plan(Kind kind, int count, int minutes, double km) {
            this(kind, count, minutes, km, 0);
        }
        Plan(Kind kind, int count, int minutes, double km, int steps) {
            this.kind = kind; this.count = count; this.minutes = minutes;
            this.km = km; this.steps = steps;
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
            case "si": return 5;    // sífutás: gyorsabb a gyaloglásnál
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
        /**
         * Megnevezett napok („hétfőn és szerdán"): alkalmankénti nap-eltolás,
         * a mentés sorrendjében. Null, ha nincs ilyen – akkor a days/offset
         * szerinti egyenletes elosztás él.
         */
        public final int[] exactDays;
        Parsed(List<Plan> plans, int days, int offset) {
            this(plans, days, offset, 12);
        }
        Parsed(List<Plan> plans, int days, int offset, int hour) {
            this(plans, days, offset, hour, null);
        }
        Parsed(List<Plan> plans, int days, int offset, int hour, int[] exactDays) {
            this.plans = plans; this.days = days; this.offset = offset;
            this.hour = hour; this.exactDays = exactDays;
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
                // Megnevezett napoknál („hétfőn és szerdán") alkalmanként
                // pontos nap jár; egyébként egyenletes elosztás.
                int dayBack = p.exactDays != null && i < p.exactDays.length
                        ? p.exactDays[i]
                        : p.offset + (p.days > 1 ? (k * p.days) / pl.count : 0);
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

    /**
     * A mondatban megnevezett EGYETLEN múltbeli nap időbélyege, vagy 0.
     *
     * A súlyzós mondat („tegnap guggolás 3x8 60 kg”) az erősítő naplóba megy,
     * nem az edzés-naplóba – a dátumot viszont ugyanez a mondat hordozza. Enélkül
     * a tegnapi edzés MAI dátummal került be: elcsúszott a széria, a heti kép és
     * a „mikor csináltad utoljára” is.
     *
     * Csak akkor válaszolunk, ha egyértelmű a nap. A több napra szóló mondat
     * („az elmúlt 3 napban”) egyetlen bejegyzésnél nem eldönthető, ott marad a
     * mai dátum – találgatni rosszabb, mint a látható alapértelmezés.
     */
    public static long singleDayTs(Parsed p, long now) {
        if (p == null) return 0;
        int back;
        if (p.exactDays != null) {
            if (p.exactDays.length != 1) return 0;
            back = p.exactDays[0];
        } else {
            if (p.days != 1) return 0;
            back = p.offset;
        }
        if (back <= 0 || back > 400) return 0;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.add(java.util.Calendar.DAY_OF_YEAR, -back);
        cal.set(java.util.Calendar.HOUR_OF_DAY, p.hour);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
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
        java.util.List<String[]> belowHundred = new java.util.ArrayList<>();
        for (String[] t : tens) {
            if (!t[0].equals("tizen") && !t[0].equals("huszon"))
                belowHundred.add(new String[]{t[0], t[1]});
            for (String[] u : units)
                belowHundred.add(new String[]{t[0] + u[0],
                        String.valueOf(Integer.parseInt(t[1]) + Integer.parseInt(u[1]))});
        }
        // A „tíz" és a „húsz" magában az alaplistában van; a százas
        // összetételekhez („százhúsz", „száztíz") itt is kell.
        belowHundred.add(new String[]{"tiz", "10"});
        belowHundred.add(new String[]{"husz", "20"});
        out.addAll(belowHundred);
        // Százasok: az ismétlésszámok ott laknak („száz fekvőtámasz",
        // „kétszáz felülés"), és eddig egyszerűen nem voltak számok.
        String[][] hundreds = {{"szaz", "100"}, {"ketszaz", "200"},
                {"haromszaz", "300"}, {"negyszaz", "400"}, {"otszaz", "500"}};
        for (String[] h : hundreds) {
            out.add(h);
            for (String[] u : units)
                out.add(new String[]{h[0] + u[0],
                        String.valueOf(Integer.parseInt(h[1]) + Integer.parseInt(u[1]))});
            for (String[] b : belowHundred)
                out.add(new String[]{h[0] + b[0],
                        String.valueOf(Integer.parseInt(h[1]) + Integer.parseInt(b[1]))});
        }
        // A HOSSZABB alak elöl: különben a „szazotven" szaz + otven lenne.
        String[][] arr = out.toArray(new String[0][]);
        java.util.Arrays.sort(arr, new java.util.Comparator<String[]>() {
            @Override public int compare(String[] x, String[] y) {
                return y[0].length() - x[0].length();
            }
        });
        return arr;
    }

    /**
     * Szavak, amikben egy sportág-szótő lakik, de semmi közük a mozgáshoz.
     *
     * A hosszú szótövek nem tévednek, a rövidek viszont igen: a kul-TÚRA nem
     * túra, a te-KER-cs nem kerékpár, a TORNA-cipő nem torna. A hiba csendes –
     * a bejegyzés létrejön, csak egy meg nem történt edzésről.
     *
     * Álcázás a szótő-illesztés ELŐTT, ugyanaz a megoldás, mint az ételeknél.
     * Így az összetett sportnevek (gerinctorna, hegyitúra, strandröplabda)
     * érintetlenek maradnak – azokat egy szóhatár-szabály elvágná.
     */
    private static final String[] NOT_SPORT = {
            "kultur", "struktur", "natur", "faktur", "textur", "karikatur",
            "diktatur", "temperatur", "literatur", "miniatur", "agrikultur",
            "tekercs", "tornacipo", "tornado", "kezitaska", "bevasarl",
            "boxutca", "tancsics", "kosarka",
            // Az „olvasás" közepén ott az „ásás": a fotelban töltött este
            // eddig kerti munkaként került a naplóba. A megTAKARÍTás nem
            // takarítás, a légKONDI nem kondi.
            "olvas", "megtakarit", "legkondi",
            // A naGYMama közepén a „gym", az aKARATErőben a „karate".
            "nagymama", "nagymami", "akarat", "tortura", "kardiolog",
            // Az edzésTERV és az edzésNAPLÓ nem edzés: a megírásuk nem
            // negyvenöt perc mozgás.
            "edzesterv", "edzesnaplo",
    };

    /** A sportág-felismerés elől elrejtett szavak kimaszkolása. */
    private static void maskNotSport(char[] q) {
        String s = new String(q);
        int i = 0;
        while (i < s.length()) {
            if (!Character.isLetter(s.charAt(i))) { i++; continue; }
            int j = i;
            while (j < s.length() && Character.isLetter(s.charAt(j))) j++;
            String tok = s.substring(i, j);
            for (String bad : NOT_SPORT)
                if (tok.startsWith(bad)) { blank(q, i, j); break; }
            i = j;
        }
    }

    /**
     * Ezek a szavak a „nap"/„hét" szótövet tartalmazzák, de nem időszakot
     * jelentenek. Nélkülük a „hétfőn futottam" egy hetes időszaknak látszana.
     */
    private static final String[] NOT_SPAN = {
            "napi", "naplo", "naploban", "naplot", "naptar", "napozas", "napsutes",
            "hetfo", "hetfon", "hetfoi", "hetvege", "hetvegen", "hetkoznap", "hetkoznapon",
            // A „hetes" sorszám vagy jelző, nem időszak: a „hetes bérlettel
            // kondi" és a „futás a hetes buszmegállóig" egyaránt EGY napról
            // szól, eddig viszont mindkettő egyhetes időszakra terült szét.
            "hetes", "hetesben", "hetessel", "hetedik", "hetediken", "hetedike",
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
        // A jövő nem napló: a „jövő héten 3 futás" vagy a „holnap futok"
        // terv, nem megtörtént edzés – ezekből semmit sem mentünk, különben
        // a szándék máris bekerülne a szériába és az XP-be.
        if (looksLikeFuture(new String(q))) return new Parsed(out, 1, 0, 12);
        // Hétköznapi szavak, amikben egy rövid sportág-szótő lakik: a kultúra
        // nem túra, a tekercs nem kerékpár. Mindenki más előtt kitakarva.
        maskNotSport(q);
        // Ami nem történt meg, az nem kerül a naplóba: a „nem futottam", a
        // „kihagytam", az „elmaradt" és az „X helyett" edzése kitakarva.
        stripNegated(q);
        // A „kétszer", „3-szor" alakból szám lesz, mielőtt bármi más olvasná.
        java.util.List<int[]> mults = stripMultiplicative(q);
        // A „6x1 km" intervall-jelölés össztávvá válik, még a táv-olvasó előtt.
        mergeIntervalDistances(q);
        // Gyakoriság („hetente kétszer", „kéthetente", „másnaponta"): a
        // periódus hossza napokban – az időszak-kereső előtt vesszük ki, hogy
        // a „hetente" ne váljon egyhetes időszakká a „hónapban" helyett.
        int freq = stripFrequency(q);

        // 1) Időszak: „elmúlt 3 nap”, „3 nap alatt”, „a héten”. A megtalált részt
        //    kitakarjuk, hogy a benne lévő szám ne számítson edzés-darabszámnak.
        int days = 1, offset = 0;
        java.util.List<int[]> wdBacks = null;
        int[] span = findSpan(q, now);
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
                    // Több napnév egy mondatban: „hétfőn és szerdán kondi".
                    java.util.List<int[]> wds = findWeekdays(q, now);
                    if (wds.size() >= 2) {
                        for (int[] w : wds) blank(q, w[0], w[1]);
                        wdBacks = wds;
                    } else {
                        // Konkrét nap megnevezve: „tegnap", „tegnapelőtt", „ma".
                        int[] one = findSingleDay(q, now);
                        if (one != null) { offset = one[2]; blank(q, one[0], one[1]); }
                    }
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
        // A kimondott tempó a KITAKARÁS ELŐTTI szövegben van: a „10 km futás
        // 5:30/km" perjeles alakjából a táv kitakarása után csak töredék marad.
        String beforeBlank = new String(q);
        List<double[]> kms = findKms(q);            // {pos, km, vég}
        for (double[] t : kms) blank(q, (int) t[0], (int) t[2]);
        mergeKmRanges(beforeBlank, kms, q);
        List<int[]> mins = findMinutes(q);          // {pos, perc}
        for (int[] m : mins) blank(q, m[0], m[2]);
        mergeTimeRanges(beforeBlank, mins, q);
        dropWarmupTimes(beforeBlank, mins);

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
                    // Az „úsz" tő ugyanilyen érzékeny: benne van az
                    // aug-USZ-tusban és a b-USZ-ban is. Szó belsejében csak
                    // igekötő után érvényes (leúsztam, átúsztam).
                    if (p > 0 && Character.isLetter(s.charAt(p - 1)) && w.startsWith("usz")) {
                        int a = p;
                        while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
                        if (!isVerbPrefix(s.substring(a, p))) continue;
                    }
                    // A „sífutottam” nem futás: a sífutás MET-je a síé (6,0),
                    // nem a futásé (9,8) – másfélszeres kalóriát írnánk.
                    if (p >= 2 && w.startsWith("fut") && s.startsWith("si", p - 2)
                            && (p == 2 || !Character.isLetter(s.charAt(p - 3))))
                        continue;
                    // A „terem” az ÉTterem és a MŰterem belsejében nem kondi
                    // (az edzőterem, gépterem viszont igen).
                    if (w.equals("terem") && p >= 2
                            && (s.startsWith("et", p - 2) || s.startsWith("mu", p - 2)
                                || s.startsWith("disz", p - 4)
                                || (p >= 3 && s.startsWith("tan", p - 3))))
                        continue;
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
            int best = -1, bestD = Integer.MAX_VALUE, bestPre = 2;
            for (int i = 0; i < keep.size(); i++) {
                if (!ALL[keep.get(i)[2]].distance) continue;
                // Amelyik mozgás már kapott távot, az kiesik a versenyből –
                // különben a „bicikli 20 km, futás 5 km" húsz kilométerét a
                // futás vitte el, az ötöt pedig eldobtuk, mert a futásnak már
                // volt távja. Két rossz bejegyzés egy mondatból.
                if (kmOf[i] != 0) continue;
                // A TELJES szó számít, nem csak a szótő: az „úsztam" úszás-töve
                // három betű, a szó hat – a köz különben a következő mozgáshoz
                // tűnt közelebbinek, és a két táv helyet cserélt.
                int a = wordStart(s, keep.get(i)[0]);
                int ae = wordEnd(s, keep.get(i)[0] + keep.get(i)[1] - 1);
                int ts = (int) t[0], te = (int) t[2];
                // A KÖZ számít, nem a szavak közepe – ugyanaz az elv, mint az
                // időtartamnál.
                int d = te <= a ? a - te : ts >= ae ? ts - ae : 0;
                // Egyenlő köznél az ELŐTTE álló mozgás nyer: magyarul a szám a
                // már kimondott mozgáshoz tapad („úsztam 1 km-t, futottam 5
                // km-t"), és egy karakternyi különbségen nem múlhat, hogy
                // melyik edzés kapja a másik távját.
                int pre = ae <= ts ? 0 : 1;
                if (d < bestD || (d == bestD && pre < bestPre)) {
                    bestD = d; bestPre = pre; best = i;
                }
            }
            if (best >= 0) kmOf[best] = t[1];
        }
        // Ha ugyanaz a mozgás kétszer szerepel („leFUTOTTAM a MARATONT"), a táv
        // a második találathoz is tapadhat – a terv viszont az elsőből készül.
        for (int i = 0; i < keep.size(); i++)
            for (int j = i + 1; j < keep.size(); j++)
                if (keep.get(i)[2] == keep.get(j)[2] && kmOf[i] == 0 && kmOf[j] > 0)
                    kmOf[i] = kmOf[j];

        // 5) Időtartamok hozzárendelése – ugyanaz az elv, mint a távoknál: a
        // legkisebb KÖZ nyer, és amelyik mozgás már kapott időt, az kiesik. A
        // „kondi 1 óra futás 40 perc" órája így a kondié marad, a futásnak
        // pedig a negyven perc jut.
        //
        // Időtartamonként keressük a gazdát, nem mozgásonként az első szabad
        // időt: az utóbbi mohó lenne, és a „futás és 30 perc kondi" harmincát a
        // futás vinné el, pedig az a kondihoz van közelebb.
        boolean[] used = new boolean[ALL.length];
        int[] minsOf = new int[keep.size()];
        for (int[] m : mins) {
            int best = -1, bestD = Integer.MAX_VALUE, bestPre = 2;
            for (int i = 0; i < keep.size(); i++) {
                if (minsOf[i] != 0) continue;
                int prevH = i > 0 ? keep.get(i - 1)[0] : -1;
                int nextH = i + 1 < keep.size() ? keep.get(i + 1)[0] : Integer.MAX_VALUE;
                if (m[0] <= prevH || m[0] >= nextH) continue;
                int a = wordStart(s, keep.get(i)[0]);
                int ae = wordEnd(s, keep.get(i)[0] + keep.get(i)[1] - 1);
                int d = m[2] <= a ? a - m[2] : m[0] >= ae ? m[0] - ae : 0;
                int pre = ae <= m[0] ? 0 : 1;
                if (d < bestD || (d == bestD && pre < bestPre)) {
                    bestD = d; bestPre = pre; best = i;
                }
            }
            if (best >= 0) minsOf[best] = m[1];
        }
        // Ugyanaz a mozgás kétszer megnevezve („crossfit wod 20 perc"): a
        // második említés kiesik a listából, de a hozzá tapadt idő nem veszhet
        // el vele – eddig a bejegyzés a mozgásforma szokásos hosszával ment
        // tovább, vagyis a kimondott húsz percből hatvan lett. A távnál ez a
        // szabály már megvolt.
        for (int i = 0; i < keep.size(); i++)
            for (int j = i + 1; j < keep.size(); j++)
                if (keep.get(i)[2] == keep.get(j)[2] && minsOf[i] == 0 && minsOf[j] > 0)
                    minsOf[i] = minsOf[j];
        // „Az elmúlt héten 3 futás és 2 úszás, 40 perc": az EGYETLEN időtartam
        // mindenkire vonatkozik – de csak akkor, ha a felsorolás UTÁN áll,
        // összefoglalásként. Az elöl álló szám az első mozgáshoz tartozik: a
        // „30 perc futás és kondi" kondija a saját szokásos hosszát kapja.
        int loneAfterAll = 0;
        // „Kondi és futás, összesen másfél óra": az ÖSSZESEN a teljes időt
        // mondja ki, nem fejenként annyit. Enélkül mindkét mozgás megkapta a
        // teljes időt, és a nap kétszer annyi mozgással zárult, mint amennyi
        // volt – ráadásul pont abban a mondatban, amivel az ember összegez.
        if (mins.size() == 1 && keep.size() > 1
                && (s.contains("osszesen") || s.contains("osszessegeben"))) {
            java.util.Arrays.fill(minsOf, 0);
            loneAfterAll = Math.max(1, mins.get(0)[1] / keep.size());
        } else if (mins.size() == 1 && keep.size() > 1) {
            int[] last = keep.get(keep.size() - 1);
            int lastEnd = wordEnd(s, last[0] + last[1] - 1);
            int m0 = mins.get(0)[0];
            // Írásjel is kell közé: az összefoglaló időtartam külön tagmondat
            // („…és 2 úszás, 40 perc”). A közvetlenül a mozgás mögé írt idő az
            // ÖVÉ, nem mindenkié – a „csütörtökön kondi 1 óra" órája a kondié.
            boolean sep = false;
            for (int k = lastEnd; k >= 0 && k < m0 && k < s.length(); k++)
                if (s.charAt(k) == ',' || s.charAt(k) == ';') sep = true;
            if (m0 >= lastEnd && sep) loneAfterAll = mins.get(0)[1];
        }
        for (int i = 0; i < keep.size(); i++) {
            int[] h = keep.get(i);
            if (used[h[2]] && !separateSession(out, ALL[h[2]], kmOf[i]))
                continue;                           // egy mozgásforma egyszer szerepel
            used[h[2]] = true;
            Kind kind = ALL[h[2]];
            int nextHit = i + 1 < keep.size() ? keep.get(i + 1)[0] : Integer.MAX_VALUE;
            int count = countBefore(s, h[0]);
            // „futottam háromszor a héten": a szorzószám a mozgás UTÁN is
            // állhat – magyarul ez a természetesebb szórend, és eddig némán
            // elveszett: három futásból egy lett a naplóban.
            // Csak akkor, ha a szorzószám nem a KÖVETKEZŐ mozgásé: a
            // „hétvégén 1-1 túra és kétszer úsztam" kettese az úszásé, mert az
            // úszás a saját darabszámaként már megtalálta.
            boolean nextTookIt = nextHit != Integer.MAX_VALUE && countBefore(s, nextHit) > 1;
            if (count <= 1 && !nextTookIt)
                for (int[] mu : mults)
                    if (mu[0] > h[0] && mu[0] < nextHit && mu[1] > 1) {
                        count = Math.min(50, mu[1]);
                        break;
                    }
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
            int next = nextHit;
            int minutes = minsOf[i] > 0 ? minsOf[i] : loneAfterAll;
            // Ismétlés-alapú tételnél a mondat TÁVOLI (más mozgáshoz írt)
            // időtartama nem érvényes: a „10 km futás 50 perc alatt és 100
            // fekvőtámasz" fekvőtámasza nem 50 perc – az ismétlésből becsülünk.
            if (reps > 0) {
                boolean local = false;
                for (int[] m : mins) if (m[0] > h[0] && m[0] < next) local = true;
                if (!local) minutes = 0;
            }
            if (minutes <= 0)
                // Nincs kimondott időtartam: távból vagy ismétlésből becsülünk,
                // anélkül a mozgásforma szokásos hossza jön.
                minutes = reps > 0
                        ? Math.max(5, Math.min(60, reps / 5))
                        : kmOf[i] > 0
                        ? Math.max(1, (int) Math.round(kmOf[i] * pace(beforeBlank, kind)))
                        : kind.defaultMin;
            // A távból becsült hossz is maradjon egy napon belül (100 km úszás
            // tempóból számolva 41 óra lenne).
            minutes = Math.min(minutes, 24 * 60);
            out.add(new Plan(kind, count, minutes, kmOf[i]));
        }

        // Két napszak, két kimondott idő, EGY mozgásforma: két edzés volt.
        //
        // A „reggel 30 perc futás, este 45 perc futás" második futása eddig
        // kiesett (egy mozgásforma egyszer szerepel), a „délelőtt 1 óra,
        // délután fél óra kondi" második ideje pedig gazdátlanul maradt.
        // Mindkét esetben a nap fele hiányzott a naplóból.
        if (out.size() == 1 && out.get(0).count == 1 && dayParts(s) >= 2) {
            Plan p = out.get(0);
            if (mins.size() == 2) {
                int total = mins.get(0)[1] + mins.get(1)[1];
                if (total >= 2 && total <= 24 * 60)
                    out.set(0, new Plan(p.kind, 2, Math.max(1, total / 2), p.km));
            } else if (mins.size() == 1 && distributiveBefore(beforeBlank, mins.get(0))) {
                // „reggel és este is futottam 20-20 percet": az osztó alak
                // ALKALMANKÉNT húsz percet jelent, nem összesen annyit.
                out.set(0, new Plan(p.kind, 2, mins.get(0)[1], p.km));
            }
        }

        // Ha nincs felismert mozgás, de van táv, az futás: a „nyomtam egy
        // 5 km-t" magyarul futást jelent.
        if (out.isEmpty() && !kms.isEmpty()) {
            Kind run = byId("futas");
            double km0 = kms.get(0)[1];
            out.add(new Plan(run, 1,
                    Math.min(24 * 60, Math.max(1, (int) Math.round(km0 * pace(beforeBlank, run)))),
                    km0));
        }

        // Ha semmilyen mozgásformát nem ismertünk fel, a puszta „N edzés" még
        // menthető: egyéb mozgásként. Csak tartalékként, mert a „3 futó edzés"
        // szóban is benne van az „edzés" – ott a futás a helyes válasz.
        if (out.isEmpty()) {
            // A „HIIT" és az „intervall" itt, a tartalék ágon van, nem
            // szótőként: időzítős programok nevében is gyakori szó („Zsírégető
            // HIIT"), és ott a program neve a helyes válasz, nem egy sportág.
            for (String w : new String[]{"edzes", "edzett", "edzeget", "edzeni", "alkalom",
                    "mozgas", "hiit", "intervall"}) {
                int p = s.indexOf(w);
                if (p < 0) continue;
                Kind other = byId("egyeb");
                int n = countBefore(s, p);
                // A szorzószám itt is állhat hátul: „a héten edzettem négyszer".
                if (n <= 1)
                    for (int[] mu : mults)
                        if (mu[0] > p && mu[1] > 1) { n = Math.min(50, mu[1]); break; }
                // A kimondott időtartam itt is számít („otthoni edzés 40 perc").
                if (other != null) out.add(new Plan(other, n,
                        minutesFor(mins, p, p, -1, Integer.MAX_VALUE,
                                other.defaultMin), 0));
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
            if (ti < 0) out.add(new Plan(byId("tura"), 1, smin, skm, (int) steps));
            else {
                Plan t = out.get(ti);
                // A kimondott idő (ami eltér az alapértelmezettől) erősebb.
                int m = t.minutes == t.kind.defaultMin ? smin : t.minutes;
                out.set(ti, new Plan(t.kind, t.count, m,
                        t.km > 0 ? t.km : skm, (int) steps));
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
        // Gyakoriság kibontása: a „hetente kétszer az elmúlt hónapban" heti
        // két alkalom × négy hét. Időszak nélkül maga a periódus az időszak.
        if (freq > 0 && out.size() == 1) {
            if (days <= 1) days = freq == 2 ? 7 : freq;
            Plan p0 = out.get(0);
            out.set(0, new Plan(p0.kind,
                    Math.min(50, p0.count * Math.max(1, days / freq)),
                    p0.minutes, p0.km, p0.steps));
        }
        // Megnevezett napok: a bejegyzések pontosan azokra kerülnek.
        if (wdBacks != null && !out.isEmpty()) {
            int n = wdBacks.size();
            java.util.List<Integer> ex = new java.util.ArrayList<>();
            if (out.size() == 1) {
                // „Hétfőn és szerdán kondi": naponként ennyi alkalom.
                Plan p0 = out.get(0);
                int per = Math.max(1, p0.count);
                int totalC = Math.min(50, per * n);
                out.set(0, new Plan(p0.kind, totalC, p0.minutes, p0.km, p0.steps));
                for (int[] w : wdBacks)
                    for (int k = 0; k < per && ex.size() < totalC; k++) ex.add(w[2]);
            } else if (out.size() == n) {
                // „Kedden úszás, csütörtökön futás": sorrendben párosítva.
                for (int i = 0; i < n; i++)
                    for (int k = 0; k < out.get(i).count; k++) ex.add(wdBacks.get(i)[2]);
            } else {
                // Nem egyértelmű párosítás: minden a legutóbbi megnevezett napra.
                int minB = Integer.MAX_VALUE;
                for (int[] w : wdBacks) minB = Math.min(minB, w[2]);
                offset = minB;
            }
            if (!ex.isEmpty()) {
                int minB = Integer.MAX_VALUE, maxB = 0;
                for (int[] w : wdBacks) {
                    minB = Math.min(minB, w[2]);
                    maxB = Math.max(maxB, w[2]);
                }
                int[] arr = new int[ex.size()];
                for (int i = 0; i < arr.length; i++) arr[i] = ex.get(i);
                return new Parsed(out, maxB - minB + 1, minB, findHour(s), arr);
            }
        }
        // Sok alkalom, időszak nélkül: „20 edzés", „tavaly 200 futás". Egyetlen
        // napra ennyi bejegyzés képtelen – a napi mozgáspercek, a széria és a
        // terhelés-figyelés is elszállna tőle (húsz edzés MA: tizenöt óra).
        // Nem találunk ki időszakot a semmiből: a minimális feltevés az, hogy
        // naponta legfeljebb egy volt, tehát annyi napra osztjuk, ahány
        // alkalom. Az előnézet ki is írja, hány napra kerül.
        if (days <= 1 && offset == 0 && out.size() == 1 && out.get(0).count > 3)
            days = Math.min(365, out.get(0).count);
        return new Parsed(out, days, offset, findHour(s));
    }

    /**
     * Gyakoriság-szavak: a visszaadott érték a periódus hossza napokban
     * (hetente = 7, kéthetente = 14, másnaponta = 2), 0 = nincs ilyen.
     */
    private static int stripFrequency(char[] q) {
        String s = new String(q);
        String[][] ws = {{"kethetente", "14"}, {"hetente", "7"}, {"minden heten", "7"},
                {"havonta", "30"}, {"minden honapban", "30"},
                {"ketnaponta", "2"}, {"masnaponta", "2"}, {"minden masodik nap", "2"}};
        for (String[] w : ws) {
            int p = s.indexOf(w[0]);
            if (p < 0) continue;
            if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
            int e = p + w[0].length();
            while (e < s.length() && Character.isLetter(s.charAt(e))) e++;
            blank(q, p, e);
            return Integer.parseInt(w[1]);
        }
        return 0;
    }

    /**
     * A „múlt kedden" egy héttel korábbi keddet jelent, nem a mostanit.
     *
     * A jelzőnek a napnév ELŐTT kell állnia, különben a „kedden futottam,
     * múlt heti tempóval" keddje is elcsúszna.
     */
    private static int lastWeekShift(String s, int dayPos, int back) {
        int b = Math.max(0, dayPos - 14);
        String head = s.substring(b, dayPos);
        // TELJES szó: a „multisport kedden" nem múlt heti kedd.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![a-z])mult(?![a-z])").matcher(head);
        return m.find() ? back + 7 : back;
    }

    /** Minden megnevezett hétköznap: {kezdet, vég, hány napja} a szöveg sorrendjében. */
    private static java.util.List<int[]> findWeekdays(char[] q, long now) {
        String s = new String(q);
        String[][] dows = {{"hetfo", "2"}, {"kedd", "3"}, {"szerda", "4"},
                {"csutortok", "5"}, {"pentek", "6"}, {"szombat", "7"}, {"vasarnap", "1"}};
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        int today = cal.get(java.util.Calendar.DAY_OF_WEEK);
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (String[] w : dows) {
            int from = 0;
            while (true) {
                int p = s.indexOf(w[0], from);
                if (p < 0) break;
                from = p + 1;
                if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
                int end = p + w[0].length();
                while (end < s.length() && Character.isLetter(s.charAt(end))) end++;
                int back = lastWeekShift(s, p, (today - Integer.parseInt(w[1]) + 7) % 7);
                out.add(new int[]{p, end, back});
            }
        }
        out.sort((a, b) -> a[0] - b[0]);
        return out;
    }

    private static final String[] MONTHS = {"januar", "februar", "marcius", "aprilis",
            "majus", "junius", "julius", "augusztus", "szeptember", "oktober",
            "november", "december"};

    /** Rövidített hónapnevek is („aug 1-jén", „júl. 28-án"). */
    private static final String[] MONTH_ABBR = {"jan", "feb", "marc", "apr", "maj",
            "jun", "jul", "aug", "szept", "okt", "nov", "dec"};
    private static final int[] MONTH_ABBR_IDX = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    /**
     * Konkrét dátum hónapnévvel: „július 28-án" → {kezdet, vég, hány napja}.
     * A legutóbbi ilyen dátum: ha az idei még nem volt meg, a tavalyi. A rag
     * és a nap száma is a kitakart részhez tartozik, hogy a szám ne váljon
     * darabszámmá. A puszta „júliusban" (nap nélkül) nem dátum.
     */
    private static int[] findMonthDay(char[] q, long now) {
        String s = new String(q);
        for (int mi = 0; mi < MONTHS.length; mi++) {
            int[] r = monthDayAt(s, MONTHS[mi], mi, now);
            if (r != null) return r;
        }
        for (int a = 0; a < MONTH_ABBR.length; a++) {
            int[] r = monthDayAt(s, MONTH_ABBR[a], MONTH_ABBR_IDX[a], now);
            if (r != null) return r;
        }
        // Számjegyes dátum: „2026.07.28" vagy „07.28-án". Rag vagy évszám
        // nélkül nem dátum – az „1.5 km" tizedespontja nem január 5-e.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:(20\\d{2})\\.\\s?)?(\\d{1,2})\\.(\\d{1,2})(\\.|-j?an|-j?en)?")
                .matcher(s);
        while (m.find()) {
            boolean hasYear = m.group(1) != null;
            String suf = m.group(4);
            if (!hasYear && (suf == null || !suf.startsWith("-"))) continue;
            int mo, d;
            try {
                mo = Integer.parseInt(m.group(2));
                d = Integer.parseInt(m.group(3));
            } catch (NumberFormatException e) { continue; }
            if (mo < 1 || mo > 12 || d < 1 || d > 31) continue;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            cal.set(java.util.Calendar.MONTH, mo - 1);
            if (hasYear) cal.set(java.util.Calendar.YEAR, Integer.parseInt(m.group(1)));
            if (d > cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)) continue;
            cal.set(java.util.Calendar.DAY_OF_MONTH, d);
            if (!hasYear && cal.getTimeInMillis() > now) cal.add(java.util.Calendar.YEAR, -1);
            int back = Days.between(cal.getTimeInMillis(), now);
            if (back < 0 || back > 365) continue;
            return new int[]{m.start(), m.end(), back};
        }
        return null;
    }

    private static int[] monthDayAt(String s, String name, int mi, long now) {
        int p = s.indexOf(name);
        if (p < 0) return null;
        if (p > 0 && Character.isLetter(s.charAt(p - 1))) return null;
        int i = p + name.length();
        if (i < s.length() && Character.isLetter(s.charAt(i))) return null; // „júliusban"
        int j = i;
        while (j < s.length() && (s.charAt(j) == ' ' || s.charAt(j) == '.')) j++;
        int d = 0, k = j;
        while (k < s.length() && Character.isDigit(s.charAt(k))) {
            d = d * 10 + (s.charAt(k) - '0');
            k++;
        }
        if (k == j || d < 1 || d > 31) return null;
        // A szám MÉRTÉKEGYSÉGE elárulja, hogy nem a hónap napja: a „január 30
        // perc kondi" harminc perc, nem január 30-a. A dátumnál rag vagy
        // írásjel jön („január 30-án", „január 30."), nem mértékegység.
        int u = k;
        while (u < s.length() && s.charAt(u) == ' ') u++;
        for (String unit : new String[]{"perc", "ora", "km", "meter", "masodperc",
                "mp", "kilometer", "lepes"})
            if (s.startsWith(unit, u)) return null;
        while (k < s.length() && (s.charAt(k) == '-' || s.charAt(k) == '.'
                || Character.isLetter(s.charAt(k)))) k++;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.MONTH, mi);
        if (d > cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)) return null;
        cal.set(java.util.Calendar.DAY_OF_MONTH, d);
        if (cal.getTimeInMillis() > now) cal.add(java.util.Calendar.YEAR, -1);
        int back = Days.between(cal.getTimeInMillis(), now);
        if (back < 0 || back > 365) return null;
        return new int[]{p, k, back};
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

    /**
     * Jövőre utaló mondat? Az ilyet nem mentjük – de a hibaüzenet meg tudja
     * mondani, hogy nem értetlenség az oka, hanem az, hogy a terv nem napló.
     */
    public static boolean looksLikeFuture(String text) {
        if (text == null) return false;
        String s = Foods.norm(text);
        // A magyar jelen idő gyakran jövőt jelent: az „este megyek edzeni" és a
        // „ha lesz időm, futok" SZÁNDÉK, nem megtörtént edzés – eddig mindkettő
        // bekerült a naplóba, a szériába és az XP-be. A múlt idő ragja más
        // („futottam"), így ezek a szótövek nem ütköznek vele.
        for (String w : new String[]{"holnap", "jovo het", "jovo hon", "fogok",
                "tervez", "szeretne", "megyek", "lesz idom", "majd lesz"})
            if (s.contains(w)) return true;
        // Egyes szám első személyű jelen idő. A „futok" és az „edzek"
        // SZÁNDÉKOSAN kimarad: az előbbi a futás szótöve (a „három kört futok"
        // is futás), az utóbbi pedig szinte mindig tagadásban áll („nem
        // edzek"), amit a pihenőnap-ág amúgy is kezel.
        for (String w : new String[]{"uszok", "biciklizek", "gyurok",
                "sportolok", "mozgok"}) {
            int p = s.indexOf(w);
            while (p >= 0) {
                int e = p + w.length();
                if ((p == 0 || !Character.isLetter(s.charAt(p - 1)))
                        && (e >= s.length() || !Character.isLetter(s.charAt(e)))) return true;
                p = s.indexOf(w, p + 1);
            }
        }
        return false;
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

    /** Tagadó / pihenőnapos mondat: az üres eredmény oka nem értetlenség. */
    public static boolean looksLikeRest(String text) {
        String s = Foods.norm(text == null ? "" : text);
        for (String w : new String[]{"nem ", "kihagytam", "kimaradt", "elmarad",
                "lemondtam", "pihenonap", "pihenes", "pihentem", "rest day"}) {
            int p = s.indexOf(w);
            if (p >= 0 && (p == 0 || !Character.isLetter(s.charAt(p - 1)))) return true;
        }
        return false;
    }

    /** Kötőszavak, amik vessző nélkül is ÚJ állítást nyitnak. */
    private static final String[] LINKERS = {" es ", " majd ", " utana ", " aztan ", " viszont "};

    /**
     * Tagadás és csere kitakarása. Az „X helyett" X-e a tagmondat elejétől a
     * szóig, a tagadó/kihagyó igék („nem …", „kihagytam", „elmaradt",
     * „lemondtam") a tagmondat végéig tűnnek el – a többi tagmondat él marad:
     * a „ma nem futottam, csak sétáltam" sétája bekerül.
     */
    private static void stripNegated(char[] q) {
        String s = new String(q);
        int h = s.indexOf("helyett");
        while (h >= 0) {
            int a = h;
            while (a > 0 && s.charAt(a - 1) != ',' && s.charAt(a - 1) != '.') a--;
            blank(q, a, h + 7);
            h = s.indexOf("helyett", h + 1);
        }
        s = new String(q);
        for (String w : new String[]{"nem ", "kihagytam", "kimaradt", "elmarad",
                "lemondtam", "neztem", "neztuk", "rendeltem", "vettem", "berlet",
                // A pihenőnap nem edzés. Megnevezett napok mellett ez különösen
                // fontos: a „szombaton túráztam 4 órát, vasárnap pihentem" két
                // NAPOT nevez meg, és eddig mindkettőre bekerült a négyórás
                // túra – vagyis nyolc óra mozgás abból, ami négy volt.
                "pihentem", "pihentunk", "pihenonap", "pihi"}) {
            int p = s.indexOf(w);
            while (p >= 0) {
                boolean boundary = p == 0 || !Character.isLetter(s.charAt(p - 1));
                // A „részt vettem az edzésen" NEM vásárlás – az él marad.
                if (boundary && w.equals("vettem")
                        && p >= 6 && s.startsWith("reszt ", p - 6)) boundary = false;
                // A bérlet VÁSÁRLÁSA nem edzés – a bérlettel VÉGZETT edzés
                // viszont az. A magyar eszközhatározó ragja (-vel/-val, itt
                // hasonulva) pont ezt a szerepet jelöli: „bérlettel edzettem",
                // „a bérletemmel jártam el". Enélkül az egész tagmondat
                // eltűnt, vagyis egy megtörtént edzés nem került a naplóba –
                // márpedig Magyarországon a legtöbben bérlettel járnak.
                if (boundary && w.equals("berlet")) {
                    int e2 = p;
                    while (e2 < s.length() && Character.isLetter(s.charAt(e2))) e2++;
                    String word = s.substring(p, e2);
                    if (word.length() > 6 && (word.endsWith("el") || word.endsWith("al")))
                        boundary = false;
                }
                if (boundary) {
                    // A „nem …" csak előre töröl (a következő tagmondat él);
                    // az elmaradt/nézett/vásárolt edzésnél az EGÉSZ tagmondat
                    // megy („a foci elmaradt", „foci vb-t néztem").
                    int a = p;
                    if (!w.equals("nem "))
                        while (a > 0 && s.charAt(a - 1) != ',' && s.charAt(a - 1) != '.'
                                && s.charAt(a - 1) != ';') a--;
                    int e = p;
                    while (e < s.length() && s.charAt(e) != ',' && s.charAt(e) != '.'
                            && s.charAt(e) != ';') e++;
                    // A kötőszó ÚJ állítást nyit, vessző nélkül is: a „focit
                    // néztem és futottam 30 percet" futása megtörtént, a
                    // „vettem egy cipőt és futottam 5 km-t" öt kilométere
                    // szintén. Enélkül a kötőszó utáni valódi edzés is eltűnt.
                    if (!w.equals("nem ")) {
                        for (String c : LINKERS) {
                            int k = s.indexOf(c, p);
                            if (k >= 0 && k < e) e = k;
                            k = s.lastIndexOf(c, p);
                            if (k >= 0 && k >= a) a = k + c.length();
                        }
                    }
                    // A „nem futottam és kondiztam" kondija megtörtént: az „és"
                    // ÚJ állítást nyit, nem folytatja a tagadást. Csak akkor
                    // fut tovább a törlés, ha a másik fele is tagadva van
                    // („nem futottam és nem úsztam").
                    if (w.equals("nem ")) {
                        int es = s.indexOf(" es ", p);
                        if (es >= 0 && es < e && !s.startsWith("nem ", es + 4)) e = es;
                        // A KÍSÉRŐ megmarad: a „nem futottam a kondi mellett"
                        // kondija megtörtént, csak a futás maradt el. A jelző
                        // ELŐTT álló szó a kísérő, azt kihagyjuk a törlésből.
                        for (String mk : new String[]{" mellett", " melle", " hozza"}) {
                            int m = s.indexOf(mk, p);
                            if (m < 0 || m >= e) continue;
                            int b = m;
                            while (b > p && s.charAt(b - 1) == ' ') b--;
                            while (b > p && Character.isLetter(s.charAt(b - 1))) b--;
                            if (b > p) e = Math.min(e, b);
                        }
                    }
                    blank(q, a, e);
                    s = new String(q);
                }
                p = s.indexOf(w, p + 1);
            }
        }
    }

    /** „Minden nap", „naponta", „napi 20 perc": a darabszám naponta értendő. */
    private static boolean stripDaily(char[] q) {
        String s = new String(q);
        // A napszakos alak ugyanezt jelenti: a „minden reggel 20 perc jóga a
        // héten" hét jógát jelent, nem egyet. Eddig a napszak elnyelte a
        // „minden"-t, és a heti ismétlődés elveszett.
        for (String w : new String[]{"minden nap", "mindennap", "naponta",
                "minden reggel", "minden este", "minden delutan", "minden delelott"}) {
            int p = s.indexOf(w);
            if (p >= 0) { blank(q, p, p + w.length()); return true; }
        }
        // A „napi" csak önálló szóként (a „3 napig" nem az).
        int p = s.indexOf("napi ");
        while (p >= 0) {
            if (p == 0 || !Character.isLetter(s.charAt(p - 1))) {
                blank(q, p, p + 4);
                return true;
            }
            p = s.indexOf("napi ", p + 1);
        }
        return false;
    }

    /**
     * Intervall-táv összevonása: a „6x1 km" vagy „8x400 méter" EGY edzés
     * össztávja, nem hat-nyolc külön alkalom. A szorzatot írjuk vissza a
     * szövegbe, mielőtt a táv- és darabszám-olvasók meglátnák.
     */
    private static void mergeIntervalDistances(char[] q) {
        String s = new String(q);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s?x\\s?(\\d{1,4}(?:[.,]\\d+)?)\\s?(km|meter|m)(?![a-z])")
                .matcher(s);
        while (m.find()) {
            int n;
            double d;
            try {
                n = Integer.parseInt(m.group(1));
                d = Double.parseDouble(m.group(2).replace(',', '.'));
            } catch (NumberFormatException e) { continue; }
            if (n < 2 || d <= 0) continue;
            double total = n * d;
            String rep;
            if (m.group(3).equals("km")) {
                rep = (total == Math.rint(total) ? String.valueOf((long) total)
                        : String.valueOf(total).replace('.', ',')) + " km";
            } else {
                rep = Math.round(total) + " m";
            }
            if (rep.length() <= m.end() - m.start()) {
                blank(q, m.start(), m.end());
                for (int i = 0; i < rep.length(); i++) q[m.start() + i] = rep.charAt(i);
            }
        }
    }

    /**
     * Kimondott napszak → óra. A múltbeli bejegyzés így nem a semleges délre
     * kerül, ha a felhasználó megmondta, mikor volt („tegnap este kondi").
     */
    private static int findHour(String s) {
        // A kimondott óra pontosabb minden napszaknál: a „reggel 6-kor" hatot
        // jelent, nem nyolcat. A délutáni napszak a 12 alatti órát átteszi
        // délutánra („este 8-kor" = 20 óra), mert este nincs nyolc óra.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d,.])(\\d{1,2})\\s?-?(?:kor|orakor)(?![a-z])").matcher(s);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            if (h >= 0 && h <= 23) {
                if (h < 12) {
                    int before = findHour(s.substring(0, m.start()));
                    if (before >= 15) h += 12;
                }
                return h;
            }
        }
        String[][] tod = {{"hajnal", "5"}, {"reggel", "8"}, {"delelott", "10"},
                {"delutan", "16"},
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
    private static int[] findSpan(char[] q, long now) {
        String s = new String(q);
        // Éves léptékű időszakok: a „fél évig" / „egy éven át" fix hosszú.
        String[][] years = {{"fel evig", "183"}, {"fel even at", "183"},
                {"fel ev alatt", "183"}, {"egy evig", "365"}, {"egy even at", "365"},
                {"egy ev alatt", "365"}};
        for (String[] y : years) {
            int p = s.indexOf(y[0]);
            if (p < 0) continue;
            if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
            int e = p + y[0].length();
            if (e < s.length() && Character.isLetter(s.charAt(e))) continue;
            return new int[]{p, e, Integer.parseInt(y[1])};
        }
        // Az „idén" az év elejétől máig tartó időszak.
        int ip = s.indexOf("iden");
        if (ip >= 0 && (ip == 0 || !Character.isLetter(s.charAt(ip - 1)))
                && (ip + 4 >= s.length() || !Character.isLetter(s.charAt(ip + 4)))) {
            java.util.Calendar yc = java.util.Calendar.getInstance();
            yc.setTimeInMillis(now);
            yc.set(java.util.Calendar.DAY_OF_YEAR, 1);
            yc.set(java.util.Calendar.HOUR_OF_DAY, 0);
            yc.set(java.util.Calendar.MINUTE, 0);
            yc.set(java.util.Calendar.SECOND, 0);
            yc.set(java.util.Calendar.MILLISECOND, 0);
            int back = Days.between(yc.getTimeInMillis(), now);
            if (back >= 1) return new int[]{ip, ip + 4, Math.min(365, back + 1)};
        }
        // „Január óta": a megnevezett hónap 1-jétől máig tartó időszak.
        int o = s.indexOf("ota");
        while (o >= 0) {
            boolean standalone = (o == 0 || !Character.isLetter(s.charAt(o - 1)))
                    && (o + 3 >= s.length() || !Character.isLetter(s.charAt(o + 3)));
            if (standalone) {
                int e = o;
                while (e > 0 && s.charAt(e - 1) == ' ') e--;
                int a = e;
                while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
                int mi = monthIndexOf(s.substring(a, e));
                if (mi >= 0) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTimeInMillis(now);
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                    cal.set(java.util.Calendar.MONTH, mi);
                    if (cal.getTimeInMillis() > now) cal.add(java.util.Calendar.YEAR, -1);
                    int back = Days.between(cal.getTimeInMillis(), now);
                    if (back >= 1 && back <= 365) return new int[]{a, o + 3, back + 1};
                }
            }
            o = s.indexOf("ota", o + 1);
        }
        // Egy hét = 7 nap, egy hónap = 30. A legkorábbi találat dönt.
        int[] best = null;
        for (int[] c : new int[][]{spanAt(s, "nap", 1), spanAt(s, "het", 7), spanAt(s, "honap", 30)})
            if (c != null && (best == null || c[0] < best[0])) best = c;
        return best;
    }

    private static int monthIndexOf(String w) {
        for (int i = 0; i < MONTHS.length; i++) if (MONTHS[i].equals(w)) return i;
        for (int i = 0; i < MONTH_ABBR.length; i++)
            if (MONTH_ABBR[i].equals(w)) return MONTH_ABBR_IDX[i];
        return -1;
    }

    /**
     * A „kétszer", „háromszor", „3-szor" alak darabszám, de a számnév-kereső
     * szóhatárt vár, így a rag miatt nem találta meg: a „kétszer úsztam" EGY
     * úszás lett. A ragot kitakarjuk, a szám ott marad.
     */
    private static java.util.List<int[]> stripMultiplicative(char[] q) {
        String s = new String(q);
        java.util.List<int[]> found = new ArrayList<>();
        for (String suf : new String[]{"szor", "szer"}) {
            int from = 0;
            while (true) {
                int p = s.indexOf(suf, from);
                if (p < 0) break;
                from = p + 1;
                int wordEnd = p + suf.length();
                while (wordEnd < s.length() && Character.isLetter(s.charAt(wordEnd))) wordEnd++;
                // A toldaléknak a szó VÉGÉN kell állnia: a „kétszeres" nem két
                // alkalom, a „háromszoros" nem három – ezek melléknevek.
                if (wordEnd != p + suf.length()) continue;
                if (p > 1 && s.charAt(p - 1) == '-' && Character.isDigit(s.charAt(p - 2))) {
                    blank(q, p - 1, wordEnd);          // „3-szor"
                    found.add(new int[]{digitsBackFrom(s, p - 1), digitsValue(s, p - 1)});
                } else if (p > 0 && Character.isDigit(s.charAt(p - 1))) {
                    blank(q, p, wordEnd);              // „3szor"
                    found.add(new int[]{digitsBackFrom(s, p), digitsValue(s, p)});
                } else {
                    int a = p;
                    while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
                    String prefix = s.substring(a, p); // „ketszer" → „ket"
                    for (String[] w : NUM_WORDS)
                        if (w[0].equals(prefix)) {
                            blank(q, p, wordEnd);
                            try { found.add(new int[]{a, Integer.parseInt(w[1])}); }
                            catch (NumberFormatException ignored) { }
                            break;
                        }
                }
            }
        }
        // A rövid „3x" alak is szorzószám, de csak szám NÉLKÜL utána: a
        // „3x10 fekvőtámasz" sorozat×ismétlés, nem három edzés.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d,.])(\\d{1,2})\\s?[x×](?![\\dx×])").matcher(s);
        while (m.find()) {
            try { found.add(new int[]{m.start(), Integer.parseInt(m.group(1))}); }
            catch (NumberFormatException ignored) { }
        }
        return found;
    }

    /** A pozíció előtti számjegyek kezdete. */
    private static int digitsBackFrom(String s, int end) {
        int b = end;
        while (b > 0 && Character.isDigit(s.charAt(b - 1))) b--;
        return b;
    }

    /** A pozíció előtti számjegyek értéke, vagy 0. */
    private static int digitsValue(String s, int end) {
        int b = digitsBackFrom(s, end);
        if (b >= end || end - b > 3) return 0;
        try { return Integer.parseInt(s.substring(b, end)); }
        catch (NumberFormatException e) { return 0; }
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
            // Az időszak-szónak a szó ELEJÉN kell állnia. A ragozott alak jó
            // („héten", „napban"), a szó belsejébe eső egyezés viszont nem: a
            // „lehetőség" nem egy hét, a „kanapé" nem egy nap. A hasonló
            // hangzású, szó eleji alakokat (hétfő, napló) a NOT_SPAN zárja ki.
            if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
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

    /** A szótövet tartalmazó szó eleje. */
    private static int wordStart(String s, int p) {
        int a = Math.max(0, Math.min(p, s.length()));
        while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
        return a;
    }

    /** A szótövet tartalmazó szó vége (kizárólagos). */
    private static int wordEnd(String s, int p) {
        int b = Math.max(0, Math.min(p, s.length() - 1));
        while (b < s.length() && Character.isLetter(s.charAt(b))) b++;
        return b;
    }

    /** A teljes szó a megadott pozíció körül. */
    private static String wordAt(String s, int p) {
        int a = p, b = p;
        while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
        while (b < s.length() && Character.isLetter(s.charAt(b))) b++;
        return s.substring(a, b);
    }

    private static boolean isNotSpan(String word) {
        for (String w : NOT_SPAN) if (w.equals(word)) return true;
        // A HETVEN és összetételei számok, nem hetek. A „hét" magában
        // kétértelmű (hét nap vagy hetes szám), ezért az marad időszaknak – a
        // „hetvenöt perc kondi" viszont eddig egyhetes időszakká vált, és
        // közben a hetvenöt perc is elveszett.
        if (word.startsWith("hetven")) return true;
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
            int back = lastWeekShift(s, p, (today - Integer.parseInt(w[1]) + 7) % 7);
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
                double val;
                if (numStart == numEnd) {
                    // Nincs SZÁMJEGY előtte – de lehet kiírva: „huszonöt
                    // kilométer bringa". Eddig ilyenkor a táv elveszett.
                    int[] wn = numberBefore(s, p, NUM_REACH);
                    if (wn == null) continue;
                    numStart = wn[0];
                    val = wn[2];
                } else {
                    try {
                        val = Double.parseDouble(
                                s.substring(numStart, numEnd).replace(',', '.'));
                    } catch (NumberFormatException e) { continue; }
                }
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

    /**
     * A bemelegítés és a levezetés ideje nem a sport ideje.
     *
     * A „20 perc bemelegítés + 40 perc foci" húsz perce a bemelegítésé – a
     * foci mégis ezt kapta, a negyven meg elveszett. Ha viszont ez az EGYETLEN
     * kimondott idő, marad: egy közelítő hossz jobb, mint semmi.
     */
    private static void dropWarmupTimes(String s, List<int[]> mins) {
        if (mins.size() < 2) return;
        List<int[]> keep = new ArrayList<>();
        for (int[] m : mins) if (!warmupWordAt(s, m)) keep.add(m);
        if (!keep.isEmpty() && keep.size() < mins.size()) {
            mins.clear();
            mins.addAll(keep);
        }
    }

    /**
     * A használandó tempó perc/km-ben: a kimondott, ha van, különben a
     * mozgásforma átlaga.
     *
     * „10 km-t futottam 5:30-as tempóval": ez ötvenöt perc, nem a becsült
     * hatvan. Aki kiírja a tempóját, az pontosan tudja, mennyit futott – kár
     * lenne felülírni egy átlaggal.
     */
    private static double pace(String s, Kind kind) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2}):([0-5]\\d) ?(?:-?[a-z]{0,3} ?tempo|/ ?km|per km)")
                .matcher(s);
        if (m.find()) {
            double p = Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2)) / 60.0;
            if (p >= 2 && p <= 20) return p;
        }
        return minPerKm(kind);
    }

    /**
     * A megnevezett idő SZOMSZÉDJA bemelegítés vagy levezetés-e?
     *
     * Szándékosan a szomszéd szót nézzük, nem egy karakter-ablakot: a
     * „20 perc bemelegítés + 40 perc foci" mondatban a negyven mögé is
     * beleért volna a bemelegítés szava, és akkor MINDKÉT idő kiesett volna.
     */
    private static boolean warmupWordAt(String s, int[] m) {
        int b = m[0];
        while (b > 0 && s.charAt(b - 1) == ' ') b--;
        int a = b;
        while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
        if (a < b && isWarmupWord(s.substring(a, b))) return true;
        int i = m[2];
        for (int w = 0; w < 2 && i < s.length(); w++) {
            while (i < s.length() && !Character.isLetter(s.charAt(i))) i++;
            int e = i;
            while (e < s.length() && Character.isLetter(s.charAt(e))) e++;
            if (e == i) break;
            if (isWarmupWord(s.substring(i, e))) return true;
            i = e;
        }
        return false;
    }

    private static boolean isWarmupWord(String w) {
        return w.startsWith("bemelegit") || w.startsWith("levezet");
    }

    /**
     * Óra:perc:másodperc alak: „futás 1:05:23".
     *
     * Ezt másolja ki az ember az órája kijelzőjéről. A KÉTRÉSZŰ alak
     * szándékosan kimarad: a „18:00" időpont, nem tizennyolc perc – és egy
     * időpontból számolt edzéshossz csendben rossz lenne.
     */
    private static void findClockTimes(String s, List<int[]> out) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d:])(\\d{1,2}):([0-5]\\d):([0-5]\\d)(?![\\d:])").matcher(s);
        while (m.find()) {
            int min = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
            if (Integer.parseInt(m.group(3)) >= 30) min++;
            if (min >= 1 && min <= 24 * 60) out.add(new int[]{m.start(), min, m.end(), 0});
        }
    }

    /**
     * Rövidített időtartam-jelölés: „1h20", „2h", „1h30m", „45p".
     *
     * Az órák-appok és a sportórák így írják, és chatben is így gépeli az
     * ember. Enélkül nem csak elveszne az idő: az „1h20 futás" HÚSZ futássá
     * vált, mert a 20 darabszámnak látszott – ez a naplót írja tele.
     *
     * A méter miatt a magában álló „m" SOHA nem perc („1500 m úszás"), csak
     * az órát követő percé („1h30m"). Betű nem jöhet a jelölés után, így a
     * „3 hét" és a „2 hónap" nem lesz óra.
     */
    private static void findShortTimes(String s, List<int[]> out) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) continue;
            if (i > 0 && (Character.isDigit(s.charAt(i - 1)) || Character.isLetter(s.charAt(i - 1))))
                continue;
            int a = i;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            // Húsz számjegy nem óraszám: a hosszú szám nem fér az int-be sem.
            if (i - a > 4) continue;
            int num = Integer.parseInt(s.substring(a, i));
            int j = i;
            while (j < s.length() && s.charAt(j) == ' ') j++;
            if (j >= s.length()) continue;
            char u = s.charAt(j);
            int val;
            if (u == 'h') {
                if (num > 24) continue;
                val = num * 60;
                j++;
                // Az óra utáni perc: „1h20", „1h 20m". A perc-jel elhagyható.
                int k = j;
                while (k < s.length() && s.charAt(k) == ' ') k++;
                int b = k;
                while (k < s.length() && Character.isDigit(s.charAt(k))) k++;
                if (k > b) {
                    if (k - b > 4) continue;
                    int m = Integer.parseInt(s.substring(b, k));
                    if (m >= 60) continue;
                    val += m;
                    j = k;
                    if (j < s.length() && (s.charAt(j) == 'm' || s.charAt(j) == 'p')) j++;
                }
            } else if (u == 'p') {
                val = num;
                j++;
            } else {
                continue;
            }
            // „2 hét", „3 hónap", „45 perc”: betű után nem rövidítés.
            if (j < s.length() && Character.isLetter(s.charAt(j))) continue;
            if (val < 1 || val > 24 * 60) continue;
            out.add(new int[]{a, val, j, 0});
            i = j - 1;
        }
    }

    /** „45 perc”, „másfél óra” helyett egyszerűen: szám + perc/óra. */
    private static List<int[]> findMinutes(char[] q) {
        String s = new String(q);
        List<int[]> out = new ArrayList<>();
        findClockTimes(s, out);
        findShortTimes(s, out);
        for (String unit : new String[]{"perc", "ora"}) {
            int from = 0;
            while (true) {
                int p = s.indexOf(unit, from);
                if (p < 0) break;
                from = p + 1;
                // A „7 órakor" időpont, nem hét óra hosszú edzés.
                if (unit.equals("ora") && s.startsWith("orakor", p)) continue;
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
            // „Kondi 1 óra és 30 perc futás": itt az „és" KÉT MOZGÁST választ
            // el, nem egy időtartam két felét. A jel az, hogy az első szám
            // ELŐTT is, a második UTÁN is áll mozgásforma – az „1 óra és 30
            // perc futás" előtt nem áll semmi, az tényleg másfél óra futás.
            if (kindWordIn(s, 0, a[0]) && kindWordIn(s, b[2], s.length())) continue;
            out.set(i, new int[]{a[0], a[1] + b[1], b[2], 0});
            out.remove(i + 1);
        }
        return out;
    }

    /** Van-e ismert mozgásforma-szó a szöveg megadott szakaszában? */
    private static boolean kindWordIn(String s, int from, int to) {
        if (from < 0 || to > s.length() || from >= to) return false;
        String part = s.substring(from, to);
        for (Kind k : ALL) for (String w : k.words) if (part.contains(w)) return true;
        return false;
    }

    /**
     * Kötőjeles szám az időtartam ELŐTT: „10-15 perc futás", „20-20 percet".
     *
     * A kitakarás eddig csak a második számot vitte el a mértékegységével
     * együtt, az első ott maradt – és DARABSZÁMNAK látszott: a „10-15 perc
     * futás" tíz külön futás lett, tizenöt percenként. Csendben, minden
     * ilyen mondatnál.
     *
     * Ha a két szám különbözik, az tartomány: a közepe a becslés (ahogy az
     * étkezésnél is). Ha egyezik, az osztó alak – ott az érték marad, a
     * darabszámot a napszakok döntik el.
     */
    private static void mergeTimeRanges(String s, List<int[]> mins, char[] q) {
        for (int[] m : mins) {
            int b = m[0], e = b;
            while (e < s.length() && Character.isDigit(s.charAt(e))) e++;
            if (e == b) continue;
            double hi;
            try { hi = Double.parseDouble(s.substring(b, e)); }
            catch (NumberFormatException ex) { continue; }
            int dash = b - 1;
            if (dash < 0 || s.charAt(dash) != '-') continue;
            int st = dash;
            while (st > 0 && Character.isDigit(s.charAt(st - 1))) st--;
            if (st == dash) continue;
            double lo;
            try { lo = Double.parseDouble(s.substring(st, dash)); }
            catch (NumberFormatException ex) { continue; }
            if (lo <= 0 || hi <= 0) continue;
            blank(q, st, dash + 1);
            if (lo == hi || lo > hi || hi > lo * 3) continue;
            m[1] = Math.max(1, (int) Math.round(m[1] * (lo + hi) / (2 * hi)));
        }
    }

    /**
     * Ugyanaz a távra: „5-8 km futás", „5-5 km".
     *
     * A kötőjel előtti szám itt is bennmaradt a szövegben, és darabszámnak
     * látszott: az „5-8 km futás" ÖT külön futás lett, egyenként nyolc
     * kilométerrel.
     */
    private static void mergeKmRanges(String s, List<double[]> kms, char[] q) {
        for (double[] t : kms) {
            int b = (int) t[0], e = b;
            while (e < s.length() && (Character.isDigit(s.charAt(e))
                    || ((s.charAt(e) == ',' || s.charAt(e) == '.')
                        && e + 1 < s.length() && Character.isDigit(s.charAt(e + 1))))) e++;
            if (e == b) continue;
            double hi;
            try { hi = Double.parseDouble(s.substring(b, e).replace(',', '.')); }
            catch (NumberFormatException ex) { continue; }
            int dash = b - 1;
            if (dash < 0 || s.charAt(dash) != '-') continue;
            int st = dash;
            while (st > 0 && (Character.isDigit(s.charAt(st - 1))
                    || ((s.charAt(st - 1) == ',' || s.charAt(st - 1) == '.')
                        && st - 2 >= 0 && Character.isDigit(s.charAt(st - 2))))) st--;
            if (st == dash) continue;
            double lo;
            try { lo = Double.parseDouble(s.substring(st, dash).replace(',', '.')); }
            catch (NumberFormatException ex) { continue; }
            if (lo <= 0 || hi <= 0) continue;
            blank(q, st, dash + 1);
            if (lo == hi || lo > hi || hi > lo * 3) continue;
            t[1] = t[1] * (lo + hi) / (2 * hi);
        }
    }

    /**
     * Osztó alak közvetlenül az időtartam előtt: „20-20 percet".
     *
     * Ugyanaz a szám kötőjellel megismételve magyarul azt jelenti, hogy
     * ALKALMANKÉNT ennyi – nem összesen. Az egyjegyű alakot („1-1 túra") már
     * régen értettük, de csak darabszámként; itt a szám mértékegységet visel.
     */
    private static boolean distributiveBefore(String s, int[] m) {
        int b = m[0];
        int e = b;
        while (e < s.length() && Character.isDigit(s.charAt(e))) e++;
        if (e == b) return false;
        String num = s.substring(b, e);
        int dash = b - 1;
        if (dash < 0 || s.charAt(dash) != '-') return false;
        int start = dash - num.length();
        return start >= 0 && s.substring(start, dash).equals(num)
                && (start == 0 || !Character.isDigit(s.charAt(start - 1)));
    }

    /** Hányféle napszakot említ a mondat? */
    private static int dayParts(String s) {
        int n = 0;
        for (String w : new String[]{"hajnal", "reggel", "delelott", "delben",
                "delutan", "este", "ejjel"})
            if (s.contains(w)) n++;
        return n;
    }

    /**
     * Ugyanaz a mozgásforma másodszor: külön edzés-e?
     *
     * Alapból egy mozgásforma egyszer szerepel – a „leFUTOTTAM a MARATONT"
     * kétszer említi a futást, de egy futás volt. Ha viszont a második
     * említésnek SAJÁT, az elsőtől eltérő távja van, akkor két külön edzés:
     * a „reggel 5 km futás, este 8 km futás" nyolc kilométere eddig némán
     * elveszett, mert a második futás egyszerűen kimaradt.
     *
     * A táv az egyetlen elég erős jel: a maraton-példában a második említés
     * ugyanazt a távot kapja (a táv-hozzárendelés átmásolja), tehát nem tér el.
     */
    private static boolean separateSession(List<Plan> out, Kind kind, double km) {
        if (km <= 0) return false;
        for (Plan p : out)
            if (p.kind == kind && (p.km <= 0 || Math.abs(p.km - km) < 0.001)) return false;
        return true;
    }

    /**
     * A megadott mozgáshoz tartozó időtartam, vagy az alapértelmezett.
     *
     * Az időtartam a mozgás neve UTÁN és ELŐTTE is állhat: a „futás 30 perc" és
     * a „30 perc futás" ugyanaz. Korábban csak az utána álló számított, ezért a
     * „30 perc futás, 20 perc kondi" mondatban a futás a KONDI idejét kapta
     * meg, a kondi pedig az alapértelmezettet – vagyis mindkét bejegyzés
     * hibás lett.
     *
     * A szomszédos mozgások zárják a szakaszt, azon belül a NÉVHEZ LEGKÖZELEBBI
     * időtartam nyer. Így mindkét szórend jól dől el, és az idő nem vándorol át
     * a szomszéd mozgáshoz.
     */
    private static int minutesFor(List<int[]> mins, int at, int atEnd,
                                  int prevAt, int nextAt, int fallback) {
        int bestIdx = -1, bestDist = Integer.MAX_VALUE;
        for (int k = 0; k < mins.size(); k++) {
            int[] m = mins.get(k);
            if (m[0] <= prevAt || m[0] >= nextAt) continue;
            // A KÖZ számít, nem a szavak közepe közti távolság: a „30 perc
            // futás, 20 perc kondi" harmincát egyetlen szóköz választja el a
            // futástól, a húszat viszont egy vessző és egy szóköz.
            int d = m[2] <= at ? at - m[2] : m[0] >= atEnd ? m[0] - atEnd : 0;
            if (d < bestDist) { bestDist = d; bestIdx = k; }
        }
        if (bestIdx >= 0) return mins.get(bestIdx)[1];
        // Ha az egész mondatban EGY időtartam van, és az minden mozgáson kívül
        // áll, akkor mindenkire vonatkozik („3 futás és 2 úszás, 40 perc”).
        // Amit viszont egy másik mozgás már elvitt, azt nem osztjuk szét.
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
    private static final String[] FILLER = {"db", "darab", "alkalom", "meccs", "kb", "x",
            // „háromszor voltam futni": az ige a szám és a mozgás közé ékelődik,
            // de a szám attól még a mozgáshoz tartozik – korábban egy alkalom
            // lett belőle, vagyis a hét kétharmada eltűnt.
            "voltam", "voltunk", "volt", "mentem", "mentunk", "jartam", "jartunk",
            "elmentem", "elmentunk"};

    private static boolean onlyFiller(String s, int from, int to) {
        String mid = s.substring(from, to);
        // Vessző vagy pontosvessző = tagmondathatár: a szám az ELŐZŐ
        // tagmondathoz tartozik. A „mellnyomás 4x10 50, evezés" mondatból
        // különben ötven evezés lett.
        if (mid.indexOf(',') >= 0 || mid.indexOf(';') >= 0) return false;
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
