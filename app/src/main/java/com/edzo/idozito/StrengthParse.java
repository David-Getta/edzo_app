package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;

/**
 * Erősítő sorozatok felvétele EGY mondatból: „3x10 fekvenyomás 60 kg”,
 * „guggolás 5x5 80 kg-mal”, „húzódzkodás 3x8”, „bicepsz 12-10-8 15 kg”.
 *
 * A kézi felvétel (gyakorlat + sorozatonként két mező) pontos, de lassú: aki a
 * terem után gyorsan beírná, amit csinált, az egy mondatban gondolkodik. A
 * felismerés itt is óvatos: amit nem ért, azt kihagyja – a mentés előtt pedig
 * a hívó megmutatja, mit értett.
 *
 * Tisztán szöveg → adat átalakítás, Context nélkül: így tesztelhető.
 */
public final class StrengthParse {

    private StrengthParse() {}

    /** Egy sorozat: ismétlés + súly (0 = saját testsúly). */
    public static final class Set {
        public final int reps;
        public final double weight;
        public Set(int reps, double weight) {
            this.reps = reps;
            // Egy kilónál könnyebb sorozat nem létezik: a legkisebb kézisúlyzó
            // is egy kiló. Ilyen szám csak félreolvasásból születik – a „fél
            // testsúllyal" mondat „fél"-jéből lett fél kilós fekvenyomás.
            // Testsúlyosnak vesszük, mert az legalább igaz.
            this.weight = weight > 0 && weight < 1 ? 0 : weight;
        }
    }

    /** Egy gyakorlat a hozzá tartozó sorozatokkal. */
    public static final class Item {
        public final String name;
        public final List<Set> sets;
        /** Érzett terhelés a mondatból („rpe 8”), 0 = nem mondta. */
        public int rpe;
        Item(String name, List<Set> sets) { this.name = name; this.sets = sets; }
        public int totalReps() { int r = 0; for (Set s : sets) r += s.reps; return r; }
        public double topWeight() {
            double m = 0; for (Set s : sets) m = Math.max(m, s.weight); return m;
        }
        /** Emberi összefoglaló az előnézethez: „Guggolás · 3×10 · 60 kg”. */
        public String label() {
            StringBuilder sb = new StringBuilder(name);
            sb.append("  ·  ");
            boolean same = true;
            for (Set s : sets) if (s.reps != sets.get(0).reps) { same = false; break; }
            if (same && sets.size() > 1) sb.append(sets.size()).append("×").append(sets.get(0).reps);
            else {
                for (int i = 0; i < sets.size(); i++) {
                    if (i > 0) sb.append('-');
                    sb.append(sets.get(i).reps);
                }
            }
            // Tartásnál a szám másodperc – enélkül a „3×60" ismétlésnek látszik.
            if (isTimed(name)) sb.append(" mp");
            double w = topWeight();
            if (w > 0) sb.append("  ·  ").append(Progression.kg(w)).append(" kg");
            else sb.append("  ·  saját testsúly");
            if (rpe > 0) sb.append("  ·  RPE ").append(rpe);
            return sb.toString();
        }
    }

    /**
     * Ismert gyakorlatok: {szép név, szótövek…}. A leghosszabb illeszkedő tő
     * nyer, hogy az összetett nevek jól dőljenek el („fekvenyomás” ne legyen
     * „fekvőtámasz”). Angol alakok is, mert a teremben azok járják.
     */
    // Csomag-szintű, hogy a ragozás-söprés tesztje végig tudjon menni rajta.
    static final String[][] MOVES = {
            {"Guggolás", "guggol", "szkvot", "squat"},
            // A „fekve" magában is fekvenyomás: a magyar terem fordított
            // szórenddel is mondja („nyomtam 100 kilót fekve ötöt"), és a
            // fekvőtámasz szótöve más, tehát nem ütközik vele.
            {"Fekvenyomás", "fekvenyom", "fekve nyom", "fekve", "bench", "mellet nyom"},
            // A jelzős változatok KÜLÖN gyakorlatok, nem a bázis becézései: a
            // román felhúzás jóval könnyebb súllyal megy, mint a holtemelés, a
            // bolgár kitörés pedig egy lábra. Egy vödörbe téve a
            // progresszió-javaslat a nehezebbik súlyát kínálná a könnyebbik
            // gyakorlathoz, a rekord meg sosem dőlne meg a könnyebbikkel.
            {"Román felhúzás", "roman felhuzas", "roman holtemel", "roman holt emel",
                    "roman huzas", "rdl"},
            {"Bolgár kitörés", "bolgar kitores", "bolgar guggolas", "bolgar split",
                    "bolgar szplit"},
            {"Ferde fekvenyomás", "ferde fekvenyom", "ferde pad", "ferde nyomas",
                    "incline"},
            // A „holt emelés" külön írva is ugyanaz a gyakorlat – és sokan
            // így írják. Nélküle a „holt emelés 1x5 140 kg" mondatból SEMMI
            // nem lett: se sorozat, se edzés.
            {"Felhúzás", "felhuzas", "holtemel", "holt emel",
                    "deadlift", "dead lift"},
            {"Húzódzkodás", "huzodzkod", "pull up", "pullup", "huzodzk", "chin up", "chinup",
                    "allhuzodzkodas", "all fole huzas"},
            {"Vállból nyomás", "vallbol nyom", "vallnyom", "vallbol", "ohp", "mellrol nyom",
                    "vallgep",
                    // A TOLÓNYOMÁS ugyanez a gyakorlat lendülettel (push
                    // press) – a magyar terem így hívja, és eddig válasz
                    // nélkül maradt.
                    "tolonyom", "tolo nyom", "push press", "pushpress",
                    "nyak moge nyom", "katonai nyomas", "military press", "shoulder press"},
            {"Evezés", "evezes", "evezo", "rowing", "evezt", "evezni", "evezek",
                    "cable row", "pendlay"},
            // Az angol „biceps curl" z nélkül írja a bicepszet – eddig a
            // sor teljesen elveszett. A puszta „curl" nem tő: a leg curl
            // combhajlítás.
            {"Bicepsz", "bicepsz", "biceps curl", "biceps ", "kalapacs",
                    "predikator", "scott pad", "hammer curl"},
            // A FRANCIA FEKVENYOMÁS tricepsz-gyakorlat, nem fekvenyomás: a
            // rövidebb „fekvenyom" tő eddig elvitte, és a huszonöt kilós
            // francia a fekvenyomás rekordjai közé került. A hosszabb tő nyer,
            // ezért elég felvenni ide.
            {"Tricepsz", "tricepsz", "francia nyom", "franciafekvenyom",
                    "francia fekvenyom", "skull crusher", "skullcrusher",
                    "tricepsz lenyom", "nyujtott karu lenyom"},
            {"Kitörés", "kitores", "lunge", "kitort"},
            // A gép NEVE a teremben „lábtoló", nem „lábtolás": a
            // „lábtoló 3x12 120" eddig válasz nélkül maradt.
            {"Lábtolás", "labtolas", "labtolo", "labtologep", "leg press",
                    "legpress"},
            {"Vádliemelés", "vadliemel", "vadli"},
            {"Fekvőtámasz", "fekvotamasz", "push up", "pushup"},
            {"Tolódzkodás", "tolodzkod", "dipp", "dips", "dip"},
            // A „kábelhúzás" a magyar termek gyűjtőneve a csigás húzásra – a
            // leggyakoribb változata a hátnak szóló lehúzás.
            {"Lehúzás", "lehuzas", "kabelhuz", "kabel huz",
                    "latpull", "lat pull", "lat huzas", "athuzas", "pullover",
                    "pulover"},
            {"Oldalemelés", "oldalemel", "eloreemel", "vallemel", "elulso vall"},
            {"Plank", "plank", "deszka", "oldaltamasz", "alkartamasz"},
            {"Felülés", "felules", "crunch", "felult"},
            {"Hasprés", "haspres", "hasizom", "hasgep"},
            // A „térdemelés" ugyanaz a hasizom-gyakorlat, csak hajlított lábbal.
            {"Lábemelés", "labemel", "terdemel", "terd emel"},
            {"Combhajlítás", "labhajlit", "combhajlit", "leg curl", "legcurl"},
            {"Lábnyújtás", "labnyujt", "combfeszit", "labgep", "leg extension"},
            {"Csípőemelés", "csipoemel", "hipthrust", "hip thrust", "medencelok",
                    "medenceemel", "medence emel", "farizom"},
            {"Arnold nyomás", "arnold"},
            {"Fordított tárogatás", "forditott tarogat", "hatso vall", "hatso deltoid",
                    "face pull", "facepull", "arcra huz"},
            {"Csuklyás emelés", "csuklyas", "shrug"},
            {"Hátizom gép", "hatizom", "hatgep"},
            {"Mellgép", "mellgep", "tarogat", "pillango", "mellnyom", "mellrepul", "butterfly",
                    "chest press", "pec deck", "pecdeck",
                    // A kábeles keresztezés ugyanaz a mozgás, más eszközzel.
                    "keresztez", "cable cross", "crossover", "kabelkereszt"},
            {"Hegymászó", "hegymaszo"},
            {"Hátfeszítés", "hiperextenzi", "hiperextension", "hyperextension", "hatfeszit",
                    "back extension"},
            // A „kettlebell" magában nem elég: a kettlebell-guggolás guggolás.
            {"Kettlebell lendítés", "kettlebell swing", "kettlebell lendit", "kb swing",
                    "swing"},
            {"Lábtávolítás", "labtavolit", "combtavolit", "abduktor"},
            {"Lábközelítés", "labkozelit", "combkozelit", "adduktor"},
            {"Fellépés", "fellepes", "step up", "stepup"},
            // Csak a teljes szó: az „alkartámasz" plank, nem alkarhajlítás.
            {"Alkarhajlítás", "alkarhajlit", "csuklohajlit"},
            {"Orosz csavarás", "orosz csav", "oroszcsav", "russian twist"},
            // A név a beépített programokét követi („Fal-ülés"), hogy a
            // mondatból és a programból felvett gyakorlat egy néven éljen.
            {"Fal-ülés", "fal ules", "fal-ules", "falules", "wall sit", "wallsit"},
            {"Holt függés", "holt fugges", "holtfugges", "dead hang", "deadhang",
                    "holtakasztas", "holt akasztas"},
            {"Szuperman", "szuperman", "superman"},
            // Hatvan mindennapi gyakorlatnévvel végigpróbálva ez a hat hiányzott
            // teljesen. A súlyemelő fogások („szakítás", „lökés") azért külön
            // tételek, mert a súlyuk semmilyen más gyakorlatéval nem
            // összemérhető – egy kalap alatt a rekord és a haladás is hazudna.
            // A magyar terem „jó reggelt"-nek hívja – köszönésnek hangzik, de
            // sorozat-számok nélkül úgysem lesz belőle bejegyzés.
            {"Good morning", "good morning", "gudmorning", "jo reggelt"},
            {"Farmerjárás", "farmerjaras", "farmer jaras", "farmers walk", "farmer walk"},
            {"Szakítás", "szakitas", "snatch"},
            // Hetvenhárom gyakorlatnévvel végigpróbálva ezek hiányoztak
            // teljesen. A nordic curl és a madár-kutya a rehab-sorokból is
            // ismerős – jó, ha a naplóban ugyanazon a néven él, mint a
            // gyakorlatsorban. (A burpee szándékosan marad ki: az kardió, és
            // az izomcsoport-kimutatásba beszámítva azt hazudná, hogy a láb
            // erősítő munkát kapott – erről külön teszt szól.)
            {"Ládaugrás", "ladaugras", "box jump", "boxjump", "box ugras", "dobozugras"},
            {"Hasgurító", "hasgurit", "ab wheel", "abwheel", "kerekkel gurit", "abroncs gurit"},
            {"Nordic curl", "nordic curl", "nordikus curl", "nordic hamstring"},
            {"Holt bogár", "dead bug", "deadbug", "holt bogar", "holtbogar"},
            {"Madár-kutya", "madar-kutya", "madar kutya", "bird dog", "birddog"},
            {"Medvejárás", "medvejaras", "medve jaras", "bear crawl", "bearcrawl"},
            {"Lökés", "lokes", "clean and jerk", "clean & jerk", "clean es jerk"},
    };

    /**
     * Tartások: itt az „ismétlés” valójában MÁSODPERC.
     *
     * A plank sosem ismétlés – aki beírja, hogy 3 × 60, az három egyperces
     * tartásra gondol. A napló eddig mindenhol ismétlésként kezelte: „0 kg ×
     * 60”-at írt ki, a progresszió pedig egy ismétlést („61 másodpercet”)
     * javasolt, és húsz fölött már azt mondta, hogy ennyi ismétlésnél az
     * állóképesség fejlődik. Egy perc plank után ez értelmetlen tanács.
     *
     * Csak az egyértelműen tartásos mozdulatok szerepelnek itt. A „superman”
     * például kimaradt: azt sokan ismétlésre csinálják, és egy rossz besorolás
     * itt csendben rossz javaslatot adna.
     */
    private static final String[] TIMED = {
            "plank", "deszka", "oldaltamasz", "alkartamasz",
            "falules", "fal ules", "fal-ules", "wallsit", "wall sit",
            "holtfugges", "holt fugges", "deadhang", "dead hang", "holtakasztas",
            "hollow", "izometri", "statikus", "vakuum",
    };

    /**
     * Tartásos gyakorlat-e a név? A napló bármilyen saját nevet elfogad, ezért
     * a szép neveken túl a szótöveket is nézzük.
     */
    public static boolean isTimed(String name) {
        if (name == null) return false;
        String q = Foods.norm(name);
        for (String t : TIMED) if (q.contains(t)) return true;
        return false;
    }

    /** „mp” tartásnál, „ismétlés” minden másnál – kiíráshoz. */
    public static String unit(String name) {
        return isTimed(name) ? "mp" : "ismétlés";
    }

    /** Tartás hossza emberi alakban: „45 mp”, „1:30”. */
    public static String hold(int sec) {
        if (sec < 60) return sec + " mp";
        int s = sec % 60;
        return (sec / 60) + ":" + (s < 10 ? "0" : "") + s;
    }

    /** A felismerhető gyakorlatok szép nevei (teszthez és súgóhoz). */
    public static String[] names() {
        String[] out = new String[MOVES.length];
        for (int i = 0; i < MOVES.length; i++) out[i] = MOVES[i][0];
        return out;
    }

    /**
     * Sorszámozott lista jelölőinek kitakarása: „1. guggolás 2. fekvenyomás".
     *
     * A leírt edzésterv gyakran számozott lista, és a sorszám ilyenkor NEM
     * ismétlésszám. Eddig az lett belőle: az „1. guggolás 2. fekvenyomás 3.
     * evezés" tervből egy kétismétléses guggolás és egy háromismétléses
     * fekvenyomás került a naplóba – kitalált sorozatok, amik a rekordokba és
     * az 1RM-be is beszámítottak.
     *
     * A minta szűk: a szám után PONT vagy ZÁRÓJEL áll, utána szóköz és betű.
     * A tizedes szám így érintetlen („12.5 kg”), és a mondatvégi pont is az.
     */
    static String stripListMarkers(String s) {
        // A GONDOLATJELES felsorolás sorhatára a normalizálás után eltűnik
        // („- 10 fekvőtámasz / - 20 guggolás / - 30 mp plank" egyetlen sorrá
        // olvad), és az utolsó tétel elveszett vele. A jel helyére vessző
        // kerül: onnantól ugyanaz, mint a vesszős felsorolás, amit értünk.
        // A csillag NEM felsorolás-jel: a „3 * 10" szorzás. A gondolatjel
        // előtt pedig nem állhat szám, különben a „3 - 10" tartomány esne
        // szét.
        s = s.replaceAll("(?:^|(?<=[^0-9]\\s))[-–—•]\\s+(?=[a-z0-9])", ", ");
        if (s.startsWith(", ")) s = s.substring(2);
        return s.replaceAll("(?<![\\d,.])(\\d{1,2})[.)]\\s+(?=[a-z])", " ");
    }

    /**
     * „60 kg x 10" → „60x10": a mértékegység a szorzójel elől kimarad.
     *
     * A súly×ismétlés írásmódot az app régóta érti („fekvenyomás 60x10,
     * 70x8"), csakhogy a legtöbb edzés-app ÍGY exportál: kiírt kilóval. A
     * bemásolt sorból emiatt egyáltalán nem lett bejegyzés – se gyakorlat,
     * se sorozat, pedig minden adat ott volt benne.
     */
    static String kgBeforeMultiplier(String s) {
        // A S\u00daLYEMEL\u00c9S mondat\u00e1ban a \u201emost N" az \u00faj munkas\u00faly: az \u201eemeltem a
        // guggol\u00e1s s\u00faly\u00e1t 5 kil\u00f3val, most 85" nyolcvan\u00f6t KIL\u00d3 \u2013 eddig
        // nyolcvan\u00f6t ism\u00e9tl\u00e9s lett bel\u0151le, s\u00faly n\u00e9lk\u00fcl.
        if (s.contains("sulyat") || s.contains("sulyt"))
            s = s.replaceAll("(?<![a-z])most (\\d{1,3}(?:[.,]\\d)?)"
                    + "(?!\\s?(?:kg|kilo|x|:))(?![\\d,.])", "most $1 kg");
        // Az \u00dcRES R\u00daD is s\u00faly: a szabv\u00e1ny olimpiai r\u00fad h\u00fasz kil\u00f3 \u2013 eddig
        // saj\u00e1t tests\u00falyos szak\u00edt\u00e1s lett a technik\u00e1z\u00e1sb\u00f3l.
        s = s.replaceAll("ures rud", "20 kg rud");
        return s.replaceAll("(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?(?:kg|kilo)\\s?([x\u00d7])", "$1$2");
    }

    /**
     * Perjeles súly/ismétlés: „fekvenyomás: 60/10, 70/8, 80/6".
     *
     * Ugyanaz a piramis, amit az „60x10" alakkal már értettünk – csak a
     * teremben sokan perjellel írják. Húsz fölötti első tag (az a súly) és
     * harminc alatti második (az az ismétlés) kell hozzá, hogy a ritmus-jelölés
     * („40/20") ne essen ide.
     */
    static String slashWeightReps(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                // A záró vessző LISTA-jel, nem tizedes: a „60/10, 70/8" első
                // két párja különben kimaradt volna a cseréből.
                .compile("(?<![\\d.,/])(\\d{2,3}(?:[.,]\\d{1,2})?)\\s?/\\s?(\\d{1,2})(?![\\d/])")
                .matcher(s);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            double w = Double.parseDouble(m.group(1).replace(',', '.'));
            int r = Integer.parseInt(m.group(2));
            m.appendReplacement(b, w >= 20 && r <= 30
                    ? java.util.regex.Matcher.quoteReplacement(m.group(1) + "x" + r)
                    : java.util.regex.Matcher.quoteReplacement(m.group()));
        }
        m.appendTail(b);
        return b.toString();
    }

    /** Heti beosztás: két vagy több napnév, sorozat- és súlyadat nélkül. */
    private static boolean looksLikeSplit(String s) {
        int days = 0;
        for (String d : new String[]{"hetfo", "kedd", "szerda", "csutortok",
                "pentek", "szombat", "vasarnap"})
            if (s.contains(d)) days++;
        if (days < 2) return false;
        return !s.matches(".*\\d\\s?[x\u00d7]\\s?\\d.*") && !s.contains("kg")
                && !s.contains("ismetles") && !s.contains("sorozat");
    }

    /**
     * A mondat feldolgozása. Tagmondatonként (vessző, pontosvessző, „és”,
     * „majd”, „utána”) egy-egy gyakorlat; ami tagmondatban nincs felismert
     * gyakorlat VAGY nincs értelmes ismétlésszám, az kimarad.
     */
    public static List<Item> parse(String text) {
        List<Item> out = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return out;
        // A TERV nem napló. A „holnap guggolás 5x5 100 kg" és a „kellene 5x5
        // 80 kg-ot guggolnom" ugyanazokból a számokból áll, mint a megtörtént
        // sorozat – csak épp még nem történt meg. A mozgás-oldalon ez a
        // szabály régóta megvan; itt hiányzott, és a kitalált sorozat a
        // rekordba, az 1RM-be és a progresszió-javaslatba is beszámított.
        if (Activities.looksLikeFuture(text)) return out;
        // A HETI BEOSZTÁS sem napló: a „hétfő mell és tricepsz, kedd hát és
        // bicepsz" azt írja le, mikor mit edz az ember – sorozatszám nincs
        // benne sehol. Eddig egyetlen tricepsz-gyakorlat lett belőle hat
        // ismétléssel (a „hát" számnévként hattá vált), és bekerült a
        // rekordok közé. Kimondott sorozat vagy súly viszont megvédi a
        // valódi többnapos naplót: „hétfőn guggolás 3x5, szerdán 4x8 60 kg".
        if (looksLikeSplit(Foods.norm(text))) return out;
        // Ami MÁSÉ, az nem az én rekordom: az „a srácok csináltak 50
        // fekvőtámaszt" eddig bekerült az erősítő naplóba – és onnantól a
        // progresszió-javaslat is arra épült.
        if (Activities.someoneElsesDoing(text)) return out;
        if (looksLikeMeasurement(Foods.norm(text))) return out;
        // A VISSZAEMLÉKEZÉS nem mai sorozat: a „régebben 100 kg-ot nyomtam
        // fekve" évekkel ezelőtti erőről szól – bekerülve mai rekord lenne,
        // és a progresszió-javaslat is rá épülne. A kimondott mai fél
        // („régen 90 volt, ma 100 kg-ot nyomtam") megvédi a mondatot.
        String nrm = Foods.norm(text);
        for (String w : new String[]{"regen ", "regebben", "annak idejen",
                "fiatalkoromban", "gyerekkoromban", "evekkel ezelott"})
            if (nrm.contains(w) && !nrm.matches(".*(?<![a-z])(ma|most|tegnap)(?![a-z]).*"))
                return out;
        // A PILLANGÓ az uszodában úszásnem, a teremben mellgép: a „pillangó
        // technikát gyakoroltam, 4x50" négyszer ötven ismétléses mellgép
        // lett – egy úszóedzésből. Úszó-szó vagy a „technika" mellett a
        // pillangó a medencéé; a „pillangó gép" a teremben marad.
        if (nrm.matches(".*(pillango|butterfly).*")
                && nrm.matches(".*(?:usz|medence|technik)\\w*.*")
                && !nrm.contains("gep")) return out;
        String clean = dropDumbbellPair(maskDistance(maskClock(maskLyingDown(
                kgBeforeMultiplier(joinRepList(stripPercent(stripListMarkers(
                        Hu.correction(Foods.norm(text))))))))));
        String whole = splitBareList(stripInsteadOf(sets(slashWeightReps(clean))));
        // Gyakorlatnév sorozat nélkül, a sorozat meg egy tagmondattal odébb:
        // „guggolás 60 kg bemelegítés, aztán 3x5 100". Az első tagmondatban
        // nincs ismétlésszám, a másodikban nincs név – eddig az EGÉSZ mondat
        // elveszett, pedig együtt teljesen egyértelmű.
        String pending = null, pendingKg = null;
        for (String part : splitClauses(whole)) {
            Item it = parseOne(part);
            // A név ékezetes, szép alak; a tagmondat viszont már normalizált,
            // ezért a nevet is úgy adjuk hozzá.
            // A JELZŐS súly is átjön a névvel: a „húsz kilós kettlebell
            // swing, 4x15" súlya az első tagmondatban áll, a sorozat a
            // másodikban – a név mellől eddig elveszett a húsz kiló, és
            // saját testsúlyos lendítés lett belőle. A tagmondat SAJÁT
            // súlya erősebb: a hozott csak a végére kerül.
            if (it == null && pending != null)
                it = parseOne(Foods.norm(pending) + " " + part
                        + (pendingKg != null ? " " + pendingKg + " kg" : ""));
            if (it != null) { out.add(it); pending = null; pendingKg = null; continue; }
            if (out.isEmpty() && pending == null) {
                pending = moveIn(part);
                if (pending != null) {
                    java.util.regex.Matcher aw = java.util.regex.Pattern
                            .compile("(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?(?:kg-?os|kg os|kilos)")
                            .matcher(part);
                    if (aw.find()) pendingKg = aw.group(1);
                }
            }
            // Sorozatfelsorolás gyakorlatnév nélkül: „fekvenyomás 60x10, 70x8,
            // 80x6”. A vessző itt nem új gyakorlatot nyit, hanem a következő
            // sorozatot – név hiányában az előzőhöz tartozik.
            if (!out.isEmpty()) {
                List<Set> more = continuationSets(part, out.get(out.size() - 1));
                if (more != null) out.get(out.size() - 1).sets.addAll(more);
            }
        }
        // Ugyanaz a gyakorlat kétszer: a sorozatok egy bejegyzésbe kerülnek.
        List<Item> merged = new ArrayList<>();
        for (Item it : out) {
            Item same = null;
            for (Item m : merged) if (m.name.equals(it.name)) { same = m; break; }
            if (same == null) merged.add(it);
            else {
                same.sets.addAll(it.sets);
                if (same.rpe == 0) same.rpe = it.rpe;
            }
        }
        // Egyetlen gyakorlatnál az RPE a mondat bármely részében állhat: a
        // „4x12 60 kg, 7-es rpe" vesszője tagmondatot zár, de a szám ugyanarra
        // a gyakorlatra vonatkozik. Több gyakorlatnál ezt nem találgatjuk.
        if (merged.size() == 1 && merged.get(0).rpe == 0) merged.get(0).rpe = rpeIn(whole);
        // Köredzés: „5 kör – 20 burpee, 15 fekvőtámasz, 10 húzódzkodás".
        //
        // A kör-szám az EGÉSZ listára vonatkozik, nem csak az első
        // gyakorlatra – a chatben megosztott edzés pont így néz ki. Eddig
        // minden gyakorlatból EGY sorozat lett, vagyis a napló a munka
        // ötödét mutatta. Csak akkor lép be, ha egyik gyakorlatnak sincs
        // saját sorozatszáma: a „3 kör: guggolás 3x10" hármasa a guggolásé.
        if (merged.size() >= 2) {
            // A kör-számot az EREDETI mondatból is megnézzük: a normalizálás
            // az „5 kör 15 fekvőtámasz" alakot „5x15"-re írja át, és onnan a
            // kör szó már hiányzik.
            String raw = Foods.norm(text);
            int rounds = numberBefore(whole, "kor ");
            if (rounds <= 0) rounds = numberBefore(whole, "kor:");
            if (rounds <= 0) rounds = numberBefore(raw, "kor ");
            if (rounds <= 0) rounds = numberBefore(raw, "kor:");
            // Az első gyakorlat gyakran MÁR megkapta a kör-számot (vele egy
            // tagmondatban áll), a többi nem. Akkor bővítünk, ha minden tétel
            // vagy egy sorozatos, vagy pont ennyi sorozatos – így a saját
            // sorozatszámmal írt listákhoz („3 kör: guggolás 3x10") nem nyúlunk.
            boolean fits = rounds >= 2 && rounds <= 20, any1 = false;
            for (Item it : merged) {
                int n = it.sets.size();
                if (n == 1) any1 = true;
                else if (n != rounds) fits = false;
            }
            if (fits && any1)
                for (Item it : merged) {
                    if (it.sets.size() != 1) continue;
                    Set base = it.sets.get(0);
                    for (int i = 1; i < rounds; i++) it.sets.add(new Set(base.reps, base.weight));
                }
        }
        // A PANASZ nem gyakorlat. A „kaptam egy húzódást a vádlimba futás
        // közben" mondatban a „vádli" szótő ott van, ismétlésszám viszont
        // nincs – eddig ebből „Vádliemelés · 1 · saját testsúly" lett, vagyis
        // a sérülés bejelentéséből edzésnapló. Csak a puszta névtalálatot
        // dobjuk el: ha a mondatban tényleges sorozat van („fájt a vállam,
        // mégis nyomtam 3x8 60 kg"), az megtörtént, az marad.
        if (hurts(whole)) {
            List<Item> kept = new ArrayList<>();
            for (Item it : merged) {
                if (it.sets.size() == 1 && it.sets.get(0).reps <= 1
                        && it.sets.get(0).weight <= 0) continue;
                kept.add(it);
            }
            merged = kept;
        }
        return merged;
    }

    /**
     * Panaszmondat-e: fájdalomról, sérülésről szól.
     *
     * A „fáj" szótő egész szóként szerepel, mert a „fajta", „fájl" és „faji"
     * nem fájdalom – a részleges egyezés miatt egy receptmondat is panasznak
     * látszana.
     */
    private static boolean hurts(String s) {
        String t = " " + s.replaceAll("[^a-z0-9]", " ") + " ";
        for (String x : new String[]{"faj", "fajt", "fajnak", "fajos", "huzodas",
                "huzodast", "huzodott", "huzodtam", "megrandult", "berandult",
                "serules", "serultem", "megserult"})
            if (t.contains(" " + x + " ")) return true;
        for (String x : new String[]{"fajdalm", "megfajdul", "gyulladt", "gyulladas",
                "belenyilall", "szakadas"})
            if (s.contains(x)) return true;
        return false;
    }

    /**
     * Egy folytatás-tagmondat sorozatai, vagy null.
     *
     * Szándékosan szűk a minta: CSAK a puszta sorozatjelölés számít
     * folytatásnak („70x8", „2x8 70 kg"). Bármi más szó a tagmondatban azt
     * jelenti, hogy nem sorozatról van szó – a „guggolás 3x10, majd 20 perc
     * futás" húsz perce nem húsz ismétlés.
     */
    private static List<Set> continuationSets(String s, Item prev) {
        // A magyar mondat nem áll meg a számnál: az „…és utána 3x8 80 kg-mal
        // dolgoztam" ugyanaz a folytatás, csak van előtte kötőszó és mögötte
        // ige. Ezeket – és CSAK ezeket – leszedjük, a minta marad szigorú:
        // bármi MÁS szó azt jelenti, hogy nem sorozatról van szó.
        String t = trimPunct(s).trim();
        t = t.replaceAll("^(?:es\\s+|majd\\s+|aztan\\s+|utana\\s+|azutan\\s+|meg\\s+|"
                + "munkasorozat\\s+|munkaszett\\s+|munkasuly\\s+|bemelegites\\s+|"
                + "felvezetes\\s+|sorozatok\\s+)+", "");
        t = t.replaceAll("(?:\\s+(?:dolgoztam|nyomtam|toltam|huztam|csinaltam|mentem|"
                + "ment|jott|kovetkezett|volt))+$", "");
        t = t.replaceAll("-?(?:mal|vel|nal|nel)$", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "^(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?[x×]\\s?(\\d{1,3})"
                        + "(?:\\s?(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?(?:kg|kilo)?)?$")
                .matcher(t);
        if (!m.matches()) return null;
        double a;
        try { a = Double.parseDouble(m.group(1).replace(',', '.')); }
        catch (NumberFormatException e) { return null; }
        int reps = Integer.parseInt(m.group(2));
        if (reps < 1 || reps > 200) return null;
        List<Set> out = new ArrayList<>();
        if (m.group(3) != null) {
            // „2x8 70 kg”: sorozat × ismétlés, kiírt súllyal.
            double w;
            try { w = Double.parseDouble(m.group(3).replace(',', '.')); }
            catch (NumberFormatException e) { return null; }
            int n = (int) a;
            if (a != n || n < 1 || n > 20 || w <= 0 || w > 500) return null;
            for (int i = 0; i < n; i++) out.add(new Set(reps, w));
            return out;
        }
        if (a > 20 && a <= 500) {
            // „70x8”: súly × ismétlés.
            out.add(new Set(reps, a));
            return out;
        }
        int n = (int) a;
        if (a != n || n < 1 || n > 20) return null;
        // „3x8” súly nélkül: az előző sorozat súlyával megy tovább.
        double w = prev.sets.isEmpty() ? 0 : prev.sets.get(prev.sets.size() - 1).weight;
        for (int i = 0; i < n; i++) out.add(new Set(reps, w));
        return out;
    }

    private static List<String> splitClauses(String s) {
        // A kötőszavakat is határnak vesszük, de a „3 és fél” nem az.
        StringBuilder b = new StringBuilder(s);
        for (int i = 0; i < b.length(); i++) {
            char c = b.charAt(i);
            if (c != ';' && c != ',' && c != '.') continue;
            // A tizedesjel NEM tagmondat-határ: a „12,5 kg” egy szám.
            boolean decimal = i > 0 && i + 1 < b.length()
                    && Character.isDigit(b.charAt(i - 1)) && Character.isDigit(b.charAt(i + 1));
            if (!decimal) b.setCharAt(i, '|');
        }
        String t = b.toString().replace(" majd ", "|").replace(" utana ", "|");
        int from = 0;
        while (true) {
            int p = t.indexOf(" es ", from);
            if (p < 0) break;
            if (!t.startsWith(" es fel", p)) t = t.substring(0, p) + "|" + t.substring(p + 4);
            from = p + 1;
        }
        List<String> out = new ArrayList<>();
        for (String part : t.split("\\|")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            // Vesszővel tagolt felsorolás: „guggoltam, 5 sorozat, 5 ismétlés,
            // 100 kg”. A darabok külön-külön értelmetlenek – a sorozatszámhoz
            // nincs ismétlés, az ismétléshez nincs gyakorlat –, ezért eddig az
            // EGÉSZ mondatból nem lett bejegyzés. Együtt viszont teljes.
            // Csak akkor folytatás, ha NINCS benne saját gyakorlatnév: a
            // „60 kg guggolás 3x8, 50 kg fekvenyomás 3x8" második fele önálló
            // gyakorlat, nem az előző adata – összeolvasztva mindkettő elveszett.
            // Egy adagoló szó állhat a szám előtt: „3 sorozat, egyenként 8
            // ismétlés, 90 kg". Az „egyenként" nélkül a hármas sorozatszám
            // elveszett, és egyetlen nyolcas sorozat maradt a naplóban.
            if (!out.isEmpty() && moveIn(p) == null && p.matches(
                    "^(?:egyenkent|mindegyik|mindegyikben|darabonkent|soronkent|"
                    + "sorozatonkent|szettenkent|azaz|plusz)?\\s*"
                    + "\\d{1,3}([.,]\\d{1,2})?\\s?(sorozat|szett|set|ismetles|ism|kg|kilo)\\b.*")) {
                out.set(out.size() - 1, out.get(out.size() - 1) + " " + p);
                continue;
            }
            out.addAll(splitByMoves(p));
        }
        return out;
    }

    /**
     * Kötőszó nélkül felsorolt gyakorlatok: „guggolás 3x10 60kg fekvenyomás
     * 3x8 50kg”. A második (és további) gyakorlatnév kezdeténél vágunk, hogy
     * mindegyik megkapja a saját sorozatait.
     */
    private static List<String> splitByMoves(String s) {
        List<Integer> cuts = new ArrayList<>();
        for (String[] row : MOVES) {
            int best = -1, bestLen = 0;
            for (int i = 1; i < row.length; i++) {
                int p = s.indexOf(row[i]);
                if (p >= 0 && row[i].length() > bestLen) { best = p; bestLen = row[i].length(); }
            }
            if (best >= 0) cuts.add(best);
        }
        List<String> out = new ArrayList<>();
        if (cuts.size() < 2) { out.add(s); return out; }
        java.util.Collections.sort(cuts);
        // A MÁSODIK gyakorlatnévtől vágunk: az első elé írt bevezető
        // („két gyakorlat: guggolás…”) az első darabhoz tartozik.
        int prev = 0;
        for (int i = 1; i < cuts.size(); i++) {
            int c = cuts.get(i);
            if (c <= prev) continue;
            String part = s.substring(prev, c).trim();
            if (!part.isEmpty() && moveIn(part) != null) { out.add(part); prev = c; }
        }
        String rest = s.substring(prev).trim();
        if (!rest.isEmpty()) out.add(rest);
        return out;
    }

    /**
     * Kiírt számok számjeggyé, és a „háromszor tízet” alak sorozat×ismétlésre.
     *
     * A teremben ritkán ír bárki számjegyet: „nyomtam ötször ötöt”, „háromszor
     * tizenkettőt”. Eddig ezekből nem lett bejegyzés – vagy ami rosszabb, a
     * puszta szám ismétlésszámnak látszott.
     */
    static String sets(String s) {
        return Hu.digits(s)
                // A csillag ugyanaz a szorzójel: a telefon billentyűzetén ez
                // van kéznél, és a „3*10 60 kg" eddig három ISMÉTLÉS volt.
                .replaceAll("(\\d)\\s?\\*\\s?(\\d)", "$1x$2")
                // A kötőjeles alak ugyanaz: a „3-szor 10-et" pont úgy három
                // sorozat tíz ismétlés, mint a „háromszor tizet" – kötőjellel
                // viszont a hármas ISMÉTLÉSSZÁMMÁ vált, és a tíz elveszett.
                .replaceAll("(\\d{1,2})\\s?-?\\s?(?:szor|szer)\\s+(\\d{1,3})", "$1x$2")
                // „3 kör 10 fekvőtámasz”: a kör itt sorozatot jelent. A szám a
                // két oldalon köti a mintát, így a „korcsolya" nem kör.
                .replaceAll("(\\d{1,2})\\s?kor\\s+(\\d{1,3})", "$1x$2");
    }

    /**
     * A tagmondat végéről a mondatzáró jelek le: a „80x6 :)" és a „80x6."
     * ugyanaz a sorozat. A minta a tagmondat VÉGÉHEZ van kötve, tehát egy
     * hangulatjel eddig elvitte az utolsó sorozatot.
     */
    private static String trimPunct(String s) {
        int e = s.length();
        while (e > 0 && !Character.isLetterOrDigit(s.charAt(e - 1))) e--;
        int b = 0;
        while (b < e && s.charAt(b) == ' ') b++;
        return s.substring(b, e);
    }

    /**
     * „Guggolás 3x10 HELYETT fekvenyomás 3x8": ami a helyett ELŐTT áll, az
     * nem történt meg. Enélkül mindkét gyakorlat bekerült a naplóba – az is,
     * amit az ember épp kihagyott.
     */
    private static String stripInsteadOf(String s) {
        int h = s.indexOf("helyett");
        while (h >= 0) {
            int a = h;
            while (a > 0 && s.charAt(a - 1) != ',' && s.charAt(a - 1) != ';'
                    && s.charAt(a - 1) != '.') a--;
            char[] c = s.toCharArray();
            for (int i = a; i < h + 7 && i < c.length; i++) c[i] = ' ';
            s = new String(c);
            h = s.indexOf("helyett", h + 1);
        }
        return s;
    }

    /**
     * Percből másodperc a tartásos mondatokban: „1 perc” → „60 mp”.
     *
     * A mértékegység szándékosan bennmarad: a puszta szám ismétlésnek
     * látszana, és a „3 sorozat 1 perc” ismétlés nélkül maradna – vagyis
     * elveszne az egész bejegyzés.
     */
    private static String holdSeconds(String s) {
        s = s.replaceAll("(?<![a-z0-9])fel ?perc", "30 mp");
        // A törtrész is számít: a „másfél perc" itt már „1,5 perc", és
        // enélkül az ötös maradt volna belőle – öt perc plank.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d,.])(\\d{1,3})(?:[.,](\\d))? ?perc").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int v = Integer.parseInt(m.group(1));
            int sec = v * 60 + (m.group(2) == null ? 0 : Integer.parseInt(m.group(2)) * 6);
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                    v > 0 && v <= 10 ? sec + " mp" : m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Egy tagmondat → gyakorlat + sorozatok, vagy null. */
    private static Item parseOne(String s) {
        String name = moveIn(s);
        // A puszta „kettlebell" MÁS gyakorlatnév mellett nem mond semmit (a
        // kettlebell-guggolás guggolás), egyedül viszont mindenki a lendítésre
        // gondol: a „kettlebell 16 kg 5x15" eddig edzés-bejegyzés lett,
        // sorozat és súly nélkül.
        if (name == null && s.contains("kettlebell")) name = "Kettlebell lendítés";
        if (name == null) return null;
        // A TEMPÓ-jelölés nem ismétlés: a „tempó 3-1-1-0" négy szakasz
        // másodperce (le, alul, fel, fent), nem négy sorozat. Kimondott
        // tempó-szó nélkül a kötőjeles lista továbbra is piramis („12-10-8").
        if (s.contains("tempo"))
            s = s.replaceAll("(?<![\\d.,])\\d{1,2}[-/]\\d{1,2}[-/]\\d{1,2}([-/]\\d{1,2})?"
                    + "(?![\\d.,])", " ");

        // Tartásnál a perc percet jelent, és a szám másodperc: a „plank 3×1
        // perc" három egyperces tartás. Átváltás nélkül a bejegyzés csendben
        // létrejön – csak hatvanszor rövidebben.
        boolean timed = isTimed(name);
        if (timed) s = holdSeconds(s);
        // Egy négyperces fal ülés hihető; négyszáz ismétlés nem. A korlát
        // ezért a mértékegységhez igazodik.
        int maxRep = timed ? 600 : 200;

        double weight = weightIn(s);
        List<Set> sets = new ArrayList<>();

        // 1) „3x10”, „3 x 10”, „3×10”: sorozat × ismétlés. A teremben szokásos
        //    „3x10x60” harmadik tagja maga a súly.
        java.util.regex.Matcher m = java.util.regex.Pattern
                // Az első tag háromjegyű is lehet: a „100x3" súlya száz kiló.
                .compile("(\\d{1,3})\\s?[x×]\\s?(\\d{1,3})(?:\\s?[x×]\\s?(\\d{1,3}(?:[.,]\\d{1,2})?))?"
                        + "(?!\\s?(?:kg|kilo))").matcher(s);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1)), r = Integer.parseInt(m.group(2));
            if (weight == 0 && m.group(3) != null) {
                try {
                    double w = Double.parseDouble(m.group(3).replace(',', '.'));
                    if (w > 0 && w <= 500) weight = w;
                } catch (NumberFormatException ignored) {}
            }
            // Mértékegység nélkül írt súly a sorozat után: „3x10 60”.
            if (weight == 0 && m.group(3) == null) {
                java.util.regex.Matcher w2 = java.util.regex.Pattern
                        // A MÉRTÉKEGYSÉG kizárja: a „ládaugrás 4x8 60 cm"
                        // hatvanas száma a doboz MAGASSÁGA, nem hatvan kiló –
                        // eddig hatvan kilós ládaugrás került a rekordba.
                        .compile("^\\s*(\\d{1,3}(?:[.,]\\d{1,2})?)(?![\\dx×])"
                                + "(?!\\s?(?:cm|centi|mm|m(?![a-z])|meter|perc|mp|masodperc))")
                        .matcher(s.substring(m.end()));
                if (w2.find()) {
                    try {
                        double w = Double.parseDouble(w2.group(1).replace(',', '.'));
                        if (w > 0 && w <= 500) weight = w;
                    } catch (NumberFormatException ignored) {}
                }
                // A súly a gyakorlatnév MÖGÉ is kerülhet: „5x5 guggolás 100".
                // Csak a tagmondat végén álló, mértékegység nélküli, húsz
                // fölötti szám lehet az – ennél kisebbet a terhelés-jelölések
                // (rpe 8, rir 2) is használnak, azokat nem szabad súllyá tenni.
                if (weight == 0 && !timed) {
                    java.util.regex.Matcher w3 = java.util.regex.Pattern
                            .compile("(?<![\\dx×.,])(\\d{1,3}(?:[.,]\\d{1,2})?)\\s*$")
                            .matcher(s.trim());
                    if (w3.find()) {
                        double w = Double.parseDouble(w3.group(1).replace(',', '.'));
                        if (w >= 20 && w <= 500) weight = w;
                    }
                }
            }
            if (n >= 1 && n <= 20 && r >= 1 && r <= maxRep)
                for (int i = 0; i < n; i++) sets.add(new Set(r, weight));
            // „60x10”: sorozatból nem lehet hatvan, súlyból viszont igen. Ez az
            // erőemelők szokásos jelölése – súly × ismétlés.
            else if (!timed && n > 20 && n <= 500 && r >= 1 && r <= 200) {
                sets.add(new Set(r, weight > 0 ? weight : n));
                // Piramis vesszők nélkül: „fekvenyomás 60x10 70x8 80x6”. A
                // vesszős alakot már értettük, a szóközöset nem – abból egyetlen
                // sorozat lett, a másik kettő némán elveszett. Csak a súly ×
                // ismétlés alakot folytatjuk: a „3x10” hármasa sorozatszám,
                // annak a szóköz nem elválasztója.
                while (m.find()) {
                    if (m.group(3) != null) break;
                    int wn = Integer.parseInt(m.group(1));
                    int wr = Integer.parseInt(m.group(2));
                    if (wn <= 20 || wn > 500 || wr < 1 || wr > 200) break;
                    sets.add(new Set(wr, wn));
                }
            }
        }
        // 2) Sorozatonként más ismétlés: „12-10-8”, „5/5/5”.
        //
        // A per-jel ugyanolyan gyakori elválasztó, mint a kötőjel, és nem
        // ütközik semmivel a súlyzós mondatban. A VESSZŐ szándékosan nem
        // szerepel: a „10,8" tizedes szám is lehet („60,5 kg"), és egy
        // félreolvasott súly rosszabb, mint egy fel nem ismert sorozatlista.
        if (sets.isEmpty()) {
            m = java.util.regex.Pattern
                    .compile("(\\d{1,3})[-/](\\d{1,3})(?:[-/](\\d{1,3}))?"
                            + "(?:[-/](\\d{1,3}))?(?:[-/](\\d{1,3}))?")
                    .matcher(s);
            if (m.find()) {
                List<Set> tmp = new ArrayList<>();
                boolean ok = true;
                for (int g = 1; g <= m.groupCount(); g++) {
                    if (m.group(g) == null) continue;
                    int r = Integer.parseInt(m.group(g));
                    if (r < 1 || r > maxRep) { ok = false; break; }
                    tmp.add(new Set(r, weight));
                }
                if (ok && tmp.size() >= 2) sets.addAll(tmp);
            }
        }
        // 2b) Vesszővel felsorolt ismétlések: „guggolás 5,5,5”.
        //
        // Vesszőt CSAK három számtól fölfelé fogadunk el: egy tizedes számban
        // pontosan egy vessző van, tehát a „60,5” sosem téveszthető össze
        // ezzel. Enélkül az egész felsorolás kiesett, és ami maradt – például
        // a súly a „5,5,5 @ 100”-ból – ismétlésszámnak látszott.
        if (sets.isEmpty()) {
            m = java.util.regex.Pattern
                    .compile("(\\d{1,3}),(\\d{1,3}),(\\d{1,3})(?:,(\\d{1,3}))?(?:,(\\d{1,3}))?")
                    .matcher(s);
            if (m.find()) {
                List<Set> tmp = new ArrayList<>();
                boolean ok = true;
                for (int g = 1; g <= m.groupCount(); g++) {
                    if (m.group(g) == null) continue;
                    int r = Integer.parseInt(m.group(g));
                    if (r < 1 || r > maxRep) { ok = false; break; }
                    tmp.add(new Set(r, weight));
                }
                if (ok) sets.addAll(tmp);
            }
        }
        // 3) „3 sorozat 10 ismétlés” / „10 ismétlés”.
        if (sets.isEmpty()) {
            int reps = numberBefore(s, "ismetles");
            if (reps <= 0) reps = numberBefore(s, "ism");
            // Tartásnál a másodperc a „hányat", nem a súly.
            if (reps <= 0 && timed) reps = numberBefore(s, "mp");
            if (reps <= 0 && timed) reps = numberBefore(s, "masodperc");
            // A „3 kör 10 fekvőtámasz" köre is sorozat. A „kör" szóközzel, hogy
            // a „korcsolya" ne legyen kör.
            int series = 0;
            String seriesWord = null;
            for (String w : new String[]{"sorozat", "szett", "set", "kor ", "kor:"}) {
                series = numberBefore(s, w);
                if (series > 0) { seriesWord = w; break; }
            }
            // „4 sorozat 8 fekvenyomás”: az „ismétlés" szó kimarad – a teremben
            // senki nem mondja ki –, a szám mégis ott van a sorozatszám után.
            // Eddig ez a mondat NEM veszett el félig: egyáltalán nem lett
            // belőle bejegyzés, mert a sorozatszám ismétlés nélkül kiszállt.
            if (reps <= 0 && series > 0 && series <= 20 && seriesWord != null) {
                int after = numberAfter(s, seriesWord);
                if (after > 0 && after <= maxRep) reps = after;
            }
            if (reps > 0 && reps <= maxRep) {
                int n = series > 0 && series <= 20 ? series : 1;
                for (int i = 0; i < n; i++) sets.add(new Set(reps, weight));
            } else if (series > 0 && series <= 20) {
                // „3 sorozat guggolás” – ismétlés nélkül nincs mit menteni.
                return null;
            }
        }
        // 4) Puszta darabszám gyakorlatnév mellett: „50 fekvőtámasz”.
        if (sets.isEmpty()) {
            java.util.regex.Matcher bare = java.util.regex.Pattern
                    .compile("(?<![\\d,.])(\\d{1,3})(?![\\d,.])").matcher(s);
            while (bare.find()) {
                // A súly számát ne vegyük ismétlésnek.
                int e = bare.end();
                String rest = s.substring(e).trim();
                if (rest.startsWith("kg") || rest.startsWith("kilo")) continue;
                // A „3x max" hármasa SOROZATSZÁM: az ismétlés ismeretlen, és
                // hármat beírni helyette csendes hazugság lenne.
                if (rest.startsWith("x") || rest.startsWith("×")) continue;
                // Az IDŐ nem ismétlés. Az „evezés 20 perc" húsz perc
                // evezőgép, nem húsz húzás – tartásnál a percet ekkorra már
                // másodperccé váltottuk, tehát itt csak a valódi időtartam
                // marad. Enélkül a kardió-mondat csendben súlyzós
                // bejegyzéssé vált, és a naplóban nem is látszott, hol.
                if (rest.startsWith("perc") || rest.startsWith("ora")
                        || rest.startsWith("óra") || rest.startsWith("mp")
                        || rest.startsWith("masodperc")) continue;
                if (isWeightSuffixed(s, e)) continue;
                if (isAtWeight(s, bare.start())) continue;
                int r = Integer.parseInt(bare.group(1));
                // A rúddal terhelt gyakorlatnál a magában álló háromjegyű szám
                // kiló, nem ismétlés: a „leguggoltam 140-et" száznegyven kilós
                // guggolás, nem száznegyven guggolás, és a „nyomtam 100-at
                // fekve" sem száz fekvenyomás. Kivétel, ha a szám közvetlenül
                // a gyakorlat nevét jelzi („csináltam 100 guggolást") – az
                // tényleg darabszám, és magyarul csak így mondjuk.
                if (r >= 100 && r <= 500 && weight == 0 && isLoaded(name)) {
                    int at = moveAt(s, name);
                    boolean modifiesName = at >= e && at - e <= 2;
                    if (!modifiesName) {
                        sets.add(new Set(1, r));
                        break;
                    }
                }
                if (r >= 1 && r <= maxRep) { sets.add(new Set(r, weight)); break; }
            }
        }
        // „fekvenyomás max 120 kg”: a legnehezebb, amit egyszer megnyomott.
        // Csak ha van súly ÉS nincs semmilyen ismétlés-adat – a „3 szett
        // maximumig” ismétlésszáma ismeretlen, abból nem találunk ki egyet.
        if (sets.isEmpty() && weight > 0 && s.matches(".*(^|[^a-z])max.*"))
            sets.add(new Set(1, weight));
        // A CSÚCS-mondat ugyanez, magyarul: „végre lement a 100 kg-os
        // fekvenyomás", „sikerült a 200 kg-os holtemelés", „megdöntöttem a
        // rekordomat guggolásban: 150 kg". Ismétlésszám nincs benne, mert
        // egyszeri – és pont ez az a bejegyzés, amit az ember a legjobban
        // szeretne látni a naplóban. Eddig egyik sem került be.
        if (sets.isEmpty() && weight > 0)
            for (String w : new String[]{"rekord", "csucs", "sikerult", "lement",
                    "vegre", "megdontott", "eloszor", "elso alkalommal", "pr ",
                    // A „megvan a 100 kg-os guggolás" ugyanaz a mondat,
                    // csak a legrövidebb magyar alakjában.
                    "megvan", "meglett", "osszejott", "bevallalt"})
                if (s.contains(w)) { sets.add(new Set(1, weight)); break; }
        if (sets.isEmpty()) return null;
        Item it = new Item(name, sets);
        it.rpe = rpeIn(s);
        return it;
    }

    /**
     * Érzett terhelés a tagmondatból: „rpe 8”, „rpe8”, „8-as rpe”. Csak a
     * 6–10 sáv életszerű; ami ezen kívül esik, az nem RPE, hanem valami más
     * szám a mondatban.
     */
    private static int rpeIn(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("rpe\\s*-?\\s*(\\d{1,2})|(\\d{1,2})\\s*-?\\s*(?:as|es|os)?\\s*rpe")
                .matcher(s);
        while (m.find()) {
            String g = m.group(1) != null ? m.group(1) : m.group(2);
            if (g == null) continue;
            try {
                int v = Integer.parseInt(g);
                if (v >= 6 && v <= 10) return v;
            } catch (NumberFormatException ignored) {
            }
        }
        // RIR („reps in reserve"): a tartalék-ismétlés jelölése, RPE-re
        // váltva – RIR 2 = RPE 8. Csak a 0–4 sáv életszerű.
        m = java.util.regex.Pattern.compile("rir\\s*-?\\s*(\\d)").matcher(s);
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v <= 4) return 10 - v;
        }
        // A „90 kg @8" rövidítés: a kg UTÁN álló @szám a 6–10 sávban RPE.
        // A kg nélküli „@ 100" súly marad, aminek eddig is olvastuk.
        m = java.util.regex.Pattern.compile("kg\\s*@\\s*(\\d{1,2})(?![0-9,.])").matcher(s);
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v >= 6 && v <= 10) return v;
        }
        return 0;
    }

    /**
     * A szövegben felismert gyakorlat szép neve, vagy null.
     *
     * Nyers (még nem normalizált) szöveget vár – a megosztott edzésnapok
     * felolvasásához kell, ahol a gyakorlatok sorozat nélkül állnak.
     */
    public static String nameIn(String raw) {
        return raw == null ? null : moveIn(Foods.norm(raw));
    }

    /**
     * A legközelebbi gyakorlatnév elgépelés esetén – „guggolsá" → Guggolás.
     *
     * Ugyanaz a szabály, mint az ételeknél: hat betűtől, egyező szókezdettel,
     * egy hibával (hosszú tőnél kettővel), a felcserélt betűt egy hibának
     * számolva. A súlyzós mezőben ez különösen hiányzott: ott a fel nem
     * ismert mondatra eddig SEMMI visszajelzés nem jött.
     *
     * @return a szép név, vagy null, ha nincs elég közeli
     */
    public static String closestMove(String raw) {
        if (raw == null) return null;
        String q = Foods.norm(raw);
        String best = null;
        int bestDist = Integer.MAX_VALUE, bestLen = 0;
        for (String tok : q.split("[^a-z0-9]+")) {
            if (tok.length() < 6) continue;
            for (String[] row : MOVES)
                for (int i = 1; i < row.length; i++) {
                    String ns = row[i];
                    if (ns.length() < 6 || ns.indexOf(' ') >= 0) continue;
                    if (!ns.regionMatches(0, tok, 0, 3)) continue;
                    int max = ns.length() >= 9 ? 2 : 1;
                    if (Math.abs(ns.length() - tok.length()) > max) continue;
                    int d = Foods.editDistance(tok, ns, max);
                    if (d <= 0 || d > max) continue;
                    if (d < bestDist || (d == bestDist && ns.length() > bestLen)) {
                        best = row[0]; bestDist = d; bestLen = ns.length();
                    }
                }
        }
        return best;
    }

    /** A leghosszabb illeszkedő gyakorlat-tő szép neve, vagy null. */
    private static String moveIn(String s) {
        String best = null;
        int bestLen = 0;
        for (String[] row : MOVES)
            for (int i = 1; i < row.length; i++)
                if (row[i].length() > bestLen && s.contains(row[i])) {
                    best = row[0];
                    bestLen = row[i].length();
                }
        return best;
    }

    /**
     * Szóközös ismétlés-felsorolás összehúzása: „12, 10, 8" → „12,10,8".
     *
     * A tagmondat-vágó a vesszőnél vág, ha szóköz követi – így a „húzódzkodás
     * max ismétlés: 12, 10, 8" első száma után a tíz és a nyolc külön,
     * névtelen tagmondatba került, és NÉMÁN elveszett. A szóköz nélküli alak
     * („12,10,8") viszont mindig működött. Csak HÁROM vagy több puszta szám
     * húzódik össze: két szám még lehet tizedes vagy két külön dolog.
     */
    private static String joinRepList(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d.,])\\d{1,3}(?:,\\s+\\d{1,3}){2,}(?![\\d.,])").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find())
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                    m.group().replaceAll(",\\s+", ",")));
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * A SZÁZALÉK nem kiló és nem ismétlés.
     *
     * A „guggolás 3x8 @70%" és a „fekvenyomás 5x3 85%-on" a maximum arányát
     * mondja ki, nem a rúdon lévő súlyt – a mondat meg sem mondja, mennyi
     * volt. Hetvenöt kilós guggolásként viszont bekerült a rekordba, az
     * 1RM-becslésbe és a progresszió-javaslatba is. Kimaszkoljuk: a sorozat
     * és az ismétlés megmarad, a súly marad ismeretlen.
     */
    private static String stripPercent(String s) {
        return s.replaceAll("@?\\s?(?<![\\d.,])\\d{1,3}(?:[.,]\\d)?\\s?%(?:-?[a-z]{1,4})?", " ");
    }

    /**
     * Az ÓRAÁLLÁS nem ismétlésszám.
     *
     * Az óra-export így írja le a kardiót: „evezőgép 5000 m 21:45". A
     * huszonegy eddig ismétlésszám lett, és a húszperces evezésből
     * huszonegy ismétléses gyakorlat került az erősítő naplóba – a rekordok
     * és a progresszió-javaslat közé. Kettősponttal írt számpár sorozatot
     * sosem jelöl, tehát ez nem vesz el semmit.
     */
    private static String maskClock(String s) {
        return s.replaceAll("(?<![\\d:])\\d{1,2}:[0-5]\\d(?::[0-5]\\d)?(?![\\d:])", "#");
    }

    /**
     * A KÉT KÉZISÚLYZÓ nem két sorozat.
     *
     * A „2x15 kg kézisúlyzóval vállnyomás 3x10" első szorzata a FELSZERELÉS:
     * két darab tizenöt kilós súlyzó. Eddig ez lett a sorozat (2×1, 15 kg), a
     * valódi 3×10 meg elveszett. A súlyzó szava előtt álló szorzatból csak a
     * súly marad.
     */
    private static String dropDumbbellPair(String s) {
        s = s.replaceAll("(?<![\\d,.])[12]\\s?x\\s?(\\d{1,3})\\s?kg"
                + "(?:-os|-mal|-al|os)?\\s?(?=\\p{L}*sulyzo|kettlebell)", "$1 kg ");
        // Ugyanez X nélkül, kiírt darabszámmal: a „két 12,5-ös
        // kézisúlyzóval" kettese darab, a súly a jelzős szám – mégis két
        // kiló lett belőle, a tizenkét és fél meg elveszett. Itt még nem
        // futott le a számnév-fordítás, ezért a „két" és az „egy" szóként
        // áll a szövegben.
        s = s.replaceAll("(?<![a-z\\d,.x])(?:[12]|ket|egy)\\s"
                + "(\\d{1,3}(?:[.,]\\d{1,2})?)"
                + "\\s?(?:kg)?\\s?-?[oe]s\\s?(?=\\p{L}*sulyzo|kettlebell)",
                "$1 kg ");
        return s;
    }

    /**
     * A MÉTER nem ismétlésszám.
     *
     * Az erőgépek neve közül nem egy vizes szó is („pillangó"), és a
     * „pillangózás 200 m" kétszáz ismétléses MELLGÉP lett a naplóban. Aki
     * métert ír, az távot mond – abból ismétlés soha nem lesz. A takarás a
     * kg-ot nem érinti (a szám után ott nem m betű áll).
     */
    private static String maskDistance(String s) {
        // Az „N napos" és az „N hetes" jelző sem sorozat: a „30 napos
        // kihívás" harmincasa a kihívás hossza, nem ismétlésszám.
        s = s.replaceAll("(?<![\\d,.])\\d{1,3}\\s?(?:napos|hetes|honapos"
                + "|hettel|nappal|honappal)(?![a-z])", "#");
        return s.replaceAll("(?<![\\d,.:])\\d{1,4}(?:[.,]\\d+)?\\s*(?:km|m)(?![a-z])", "#");
    }

    /**
     * A MÉRŐSZALAG nem edzés.
     *
     * A „combom 58 cm, vádli 38" egy testkörfogat-mérés – eddig
     * harmincnyolc ismétléses VÁDLIEMELÉS lett belőle, mert a vádli
     * gyakorlatnév is. A centiméter és a megmért testrész neve együtt
     * egyértelmű: ilyen mondatot senki nem edzésnek szán.
     */
    private static boolean looksLikeMeasurement(String s) {
        if (!s.matches(".*\\d\\s?-?(?:cm|centi)\\b.*")) return false;
        // A bicepsz is testrész: a „bicepszem 38 cm lett" mérőszalag, nem
        // harmincnyolc ismétléses bicepszgyakorlat.
        for (String w : new String[]{"comb", "derek", "csipo", "mellkas", "vadli",
                "korfogat", "boseg", "felkar", "bicepsz"})
            if (s.contains(w)) return true;
        return false;
    }

    /**
     * A LEFEKVÉS nem fekvenyomás.
     *
     * A „fekve" szótő a fordított szórendet fogja („nyomtam 100 kilót fekve
     * ötöt"), de az alvás-mondat is tartalmazza: a „lefekvés 23:15, ébredés
     * 6:45" huszonhárom ismétléses fekvenyomás lett a naplóban. A takarás a
     * tagmondat-vágás ELŐTT történik, mert a névtalálat onnan is átszivárog
     * a következő tagmondatba.
     */
    private static String maskLyingDown(String s) {
        for (String w : new String[]{"lefekves", "lefekudt", "fekudni", "lefekudni",
                "fekve maradt", "fekve alszom"})
            s = s.replace(w, "#");
        // Ugyanez a „hát nap:" edzésnap-névvel: ékezet nélkül a testtáj és a
        // hatos számnév egybeesik, és a „hát nap: húzódzkodás, evezés…"
        // hatismétléses húzódzkodássá vált.
        return s.replaceAll("(?<![a-z])hat(?=\\s+nap\\s*[:–-])", "#");
    }

    /**
     * Vessző nélküli felsorolás: „5 kör 10 fekvőtámasz 15 guggolás 20 hasizom".
     *
     * A megosztott köredzés így néz ki – a magyar felsorolásban a vessző
     * elmarad, mert a szám maga tagol. A tagmondat-vágó viszont vesszőt
     * keresett, így az EGÉSZ lista egy tagmondat lett: a guggolás megkapta a
     * hasizom ismétlésszámát, a hasprés pedig egyáltalán nem került be.
     *
     * Csak akkor vágunk, ha a szám után közvetlenül gyakorlatnév áll, ÉS a
     * mondatban már volt korábban gyakorlat – az első tétel elé nem kell
     * határ, és a „3x10 fekvenyomás 60 kg" hatvanasa sem nyit új tételt.
     */
    private static String splitBareList(String s) {
        StringBuilder out = new StringBuilder();
        int last = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d.,x×])(\\d{1,3})\\s+(?=[a-z])").matcher(s);
        while (m.find()) {
            if (moveAtStart(s.substring(m.end())) == null) continue;
            if (moveIn(s.substring(0, m.start())) == null) continue;
            out.append(s, last, m.start()).append(", ");
            last = m.start();
        }
        if (last == 0) return s;
        out.append(s.substring(last));
        return out.toString();
    }

    /** A szöveg elején álló gyakorlat neve, vagy null. */
    private static String moveAtStart(String s) {
        for (String[] row : MOVES)
            for (int i = 1; i < row.length; i++)
                if (s.startsWith(row[i])) return row[0];
        return null;
    }

    /** A gyakorlatnév szótövének első helye a tagmondatban, vagy -1. */
    private static int moveAt(String s, String name) {
        int at = -1;
        for (String[] row : MOVES) {
            if (!row[0].equals(name)) continue;
            for (int i = 1; i < row.length; i++) {
                int p = s.indexOf(row[i]);
                if (p >= 0 && (at < 0 || p < at)) at = p;
            }
        }
        return at;
    }

    /**
     * Rúddal terhelt gyakorlat: itt a háromjegyű szám kiló, nem ismétlés.
     *
     * Szűk a lista: csak azok a mozdulatok, ahol a százas nagyságrend a
     * súlyban hétköznapi, az ismétlésben viszont képtelenség.
     */
    private static boolean isLoaded(String name) {
        for (String n : new String[]{"Guggolás", "Fekvenyomás", "Felhúzás",
                "Vállból nyomás", "Lábtolás", "Román felhúzás", "Ferde fekvenyomás"})
            if (n.equals(name)) return true;
        return false;
    }

    /** Súly kilóban: „60 kg”, „60kg”, „60 kilóval”, „60-nal”. 0 = nincs. */
    private static double weightIn(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?(kg|kilo|kilogramm)").matcher(s);
        if (m.find()) {
            try {
                double w = Double.parseDouble(m.group(1).replace(',', '.'));
                if (w > 0 && w <= 500) return w;
            } catch (NumberFormatException ignored) {}
        }
        // Mértékegység nélküli, de eszközraggal írt súly: „100-zal", „80-nal",
        // „60-al". Enélkül a „guggoltam 100-zal ötször ötöt" száz ismétlésnek
        // olvasódott – onnantól a rekordok és az 1RM is hazudtak volna.
        m = java.util.regex.Pattern
                .compile("(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?-?\\s?(zal|val|vel|nal|nel|lal|lel|al|el)\\b")
                .matcher(s);
        if (m.find()) {
            try {
                double w = Double.parseDouble(m.group(1).replace(',', '.'));
                if (w > 0 && w <= 500) return w;
            } catch (NumberFormatException ignored) {}
        }
        // A kukac az edzésnaplók nemzetközi rövidítése a súlyra: „5x5 @ 100”.
        // Mértékegység nélkül eddig ismétlésszámnak látszott, és a „5,5,5 @ 100”
        // egyetlen, száz ismétléses sorozat lett.
        // A SZÁZALÉK nem kiló: a „guggolás 3x8 @70%" és a „fekvenyomás 5x3
        // 85%-on" a maximum arányát mondja, nem a rúdon lévő súlyt. Hetven
        // kilós guggolásként a rekordba és a progresszió-javaslatba is
        // beszámított volna – pedig a mondat meg sem mondja, mennyi volt.
        m = java.util.regex.Pattern.compile("@\\s?(\\d{1,3}(?:[.,]\\d{1,2})?)(?!\\s?%)")
                .matcher(s);
        if (m.find()) {
            try {
                double w = Double.parseDouble(m.group(1).replace(',', '.'));
                if (w > 0 && w <= 500) return w;
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /** A kukac utáni szám a súly, nem ismétlés: „5x5 @ 100”. */
    private static boolean isAtWeight(String s, int start) {
        for (int i = start - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ' ') continue;
            return c == '@';
        }
        return false;
    }

    /** A súlyt jelölő eszközragos szám („100-zal") ne legyen ismétlésszám. */
    private static boolean isWeightSuffixed(String s, int end) {
        String rest = s.substring(end);
        return rest.matches("^\\s?-?\\s?(zal|val|vel|nal|nel|lal|lel|al|el)\\b.*");
    }

    /**
     * A megadott szó ELŐTT álló szám („3 sorozat”, „10 ismétlés”). A szó
     * ragozott alakja is jó, mert csak a kezdetét keressük.
     */
    private static int numberBefore(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            int e = p;
            while (e > 0 && s.charAt(e - 1) == ' ') e--;
            int b = e;
            while (b > 0 && Character.isDigit(s.charAt(b - 1))) b--;
            if (b < e) {
                try { return Integer.parseInt(s.substring(b, e)); }
                catch (NumberFormatException ignored) {}
            }
            p = s.indexOf(word, p + 1);
        }
        return 0;
    }

    /**
     * A megadott szó UTÁN álló szám („4 sorozat 8”). Csak akkor, ha a szám
     * tényleg ismétlés lehet: a mértékegységgel folytatódó számok (60 kg,
     * 2 perc pihenő) és a „3x8" szorzata nem az.
     */
    private static int numberAfter(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            int e = p + word.length();
            while (e < s.length() && Character.isLetter(s.charAt(e))) e++;   // ragozott alak
            while (e < s.length() && (s.charAt(e) == ' ' || s.charAt(e) == '-')) e++;
            int b = e;
            while (e < s.length() && Character.isDigit(s.charAt(e))) e++;
            if (b < e) {
                String rest = s.substring(e).trim();
                boolean unit = rest.startsWith("kg") || rest.startsWith("kilo")
                        || rest.startsWith("perc") || rest.startsWith("mp")
                        || rest.startsWith("masodperc") || rest.startsWith("x")
                        || rest.startsWith("×") || rest.startsWith(",");
                if (!unit) {
                    try { return Integer.parseInt(s.substring(b, e)); }
                    catch (NumberFormatException ignored) {}
                }
            }
            p = s.indexOf(word, p + 1);
        }
        return 0;
    }
}
