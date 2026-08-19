package com.edzo.idozito;

/**
 * Intervallum-beállítás EGY mondatból: „3 kör 40 mp munka 20 mp pihenő”.
 *
 * Az étkezést, az edzést és a súlyzós sorozatot már mondatból is fel lehet
 * venni – az app magját, magát az időzítőt viszont eddig csak három csúszkával
 * lehetett beállítani. Aki a teremben kap egy edzéstervet, azt szövegként
 * kapja, nem csúszkaállásként.
 *
 * Tiszta Java (nincs Context), hogy egységteszttel lefedhető legyen: egy
 * elszámolás itt rossz edzést eredményez, és az edzés közben derül ki.
 */
public final class IntervalParse {

    private IntervalParse() {
    }

    /** Épeszű határok: ezeken kívül nem állítunk be semmit. */
    static final int MAX_ROUNDS = 50, MIN_SEC = 5, MAX_SEC = 3600;

    /**
     * A teljes edzés felső határa: négy óra.
     *
     * A szakaszok külön-külön hihetőek lehetnek, együtt mégsem – a „8x 60
     * perc" nyolc órás időzítő. A napló hazug bejegyzésénél is rosszabb egy
     * olyan óra, ami egész nap ketyeg.
     */
    static final int MAX_TOTAL_SEC = 4 * 3600;

    public static final class Plan {
        public final int rounds;
        /** Munkaidő másodpercben. */
        public final int work;
        /** Pihenő másodpercben (0 = nincs). */
        public final int rest;
        /** Bemelegítés másodpercben (0 = nincs). */
        public final int warm;
        /** Levezetés másodpercben (0 = nincs). */
        public final int cool;
        /**
         * Kitalált ritmus: a mondat a forma NEVÉT mondta ki, nem a számokat.
         *
         * A „hiit 20 perc" harminc-harmincas ritmusa a mi javaslatunk, nem a
         * felhasználó szava. Ezért gyengébb jel: a „ma 1 óra hiit edzés"
         * megtörtént edzés, nem időzítő-terv – az útbaigazító ezt a mezőt
         * nézve tudja megkülönböztetni a kettőt.
         */
        public boolean guessed;

        Plan(int rounds, int work, int rest) {
            this(rounds, work, rest, 0, 0);
        }

        Plan(int rounds, int work, int rest, int warm, int cool) {
            this.rounds = rounds; this.work = work; this.rest = rest;
            this.warm = warm; this.cool = cool;
        }

        /** „8 kör · 20 mp munka · 10 mp pihenő”. */
        public String label() {
            String s = rounds + " kör  ·  " + sec(work) + " munka";
            if (rest > 0) s += "  ·  " + sec(rest) + " pihenő";
            if (warm > 0) s += "  ·  " + sec(warm) + " bemelegítés";
            if (cool > 0) s += "  ·  " + sec(cool) + " levezetés";
            return s;
        }

        /**
         * A terv MONDATKÉNT – ugyanabban az alakban, amit a felismerő ért.
         *
         * Ez a megosztás formája: aki megkapja, a Grit megosztás-listáján át
         * egy koppintással beállíthatja ugyanezt. Ezért nem díszes: a
         * szöveget vissza kell tudni olvasni (a teszt oda-vissza futtatja).
         */
        public String sentence() {
            StringBuilder sb = new StringBuilder();
            if (warm > 0) sb.append(sec(warm)).append(" bemelegítés, ");
            sb.append(rounds).append(" kör ").append(sec(work)).append(" munka");
            if (rest > 0) sb.append(" ").append(sec(rest)).append(" pihenő");
            if (cool > 0) sb.append(", ").append(sec(cool)).append(" levezetés");
            return sb.toString();
        }

        /** A teljes edzés hossza másodpercben (az utolsó pihenő is beleszámít). */
        public int totalSec() {
            return warm + rounds * (work + rest) + cool;
        }

        private static String sec(int s) {
            if (s % 60 == 0 && s >= 60) return (s / 60) + " perc";
            if (s > 60) return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
            return s + " mp";
        }
    }

    /**
     * Ismert edzésformák. Ezeket névvel is ki lehet mondani, mert a teremben
     * is így hívják őket – a „tabata” mindenkinek ugyanazt jelenti.
     */
    private static final String[][] PRESETS = {
            {"tabata", "8", "20", "10"},
            {"emom", "10", "60", "0"},
            {"pomodoro", "1", "1500", "300"},
            // A norvég 4x4: négyszer négy perc erős tempó, három perc pihenő –
            // az állóképesség-kutatás legtöbbet idézett protokollja.
            {"norveg", "4", "240", "180"},
    };

    /**
     * @return a felismert beállítás, vagy null, ha a mondatból nem derül ki
     *         értelmes munkaidő – találgatni itt nem szabad.
     */
    public static Plan parse(String text) {
        if (text == null) return null;
        String s = digits(Foods.norm(text).replace('\n', ' '));
        if (s.trim().isEmpty()) return null;
        // A VÉRNYOMÁS nem ritmus: a „160/95 a vérnyomásom" ugyanúgy néz ki,
        // mint egy munka/pihenő pár, és eddig időzítő-tervet ajánlott rá az
        // app – száznegyven másodperc munka, kilencvenöt pihenő.
        if (s.contains("vernyomas") || s.contains("ver nyomas")
                || s.contains("higanymilli") || s.contains("hgmm")) return null;
        // Az ÉTKEZÉSI IDŐPONTOK sora sem ritmus: a „7:00 zabkása, 12:30
        // csirke rizzsel" órajelei munka/pihenő párnak látszottak, és
        // időzítő-tervet ajánlott rájuk az app.
        if (s.matches(".*\\d{1,2}:\\d{2}.*")
                && !Foods.parse(java.util.Arrays.asList(Foods.ALL), text).isEmpty()
                && !s.contains("intervall") && !s.contains("hiit")
                && !s.contains("kor ") && !s.contains("sprint")
                && !s.contains("munka") && !s.contains("piheno")) return null;
        // Az ÚSZÓ-TEMPÓ sem ritmus: a „2:10/100m tempó" munka/pihenő
        // párnak látszott, és időzítő-tervet ajánlott rá az app.
        s = s.replaceAll("(?<![\\d])\\d{1,2}:\\d{2}\\s?/\\s?100\\s?m"
                + "(?![a-z])", " ");
        // A MECCS sem ritmus: az „edzőmeccs 2x30 perc" hatvan perc játék,
        // a „25-22 lett" pedig a végeredmény – eddig mindkettő időzítő-terv
        // lett. A kimondott intervall-szó viszont felment.
        // A KUTYASÉTÁLTATÁS napi két köre sem ritmus: a
        // „kutyasétáltatás 2x30 perc" egy óra séta.
        if (s.contains("setaltatas") || s.contains("kutyaset")) return null;
        if ((s.contains("meccs") || s.contains("felido")
                || s.contains("jatszottam") || s.contains("kispalyas"))
                && !s.contains("intervall") && !s.contains("hiit")) return null;
        // A PULZUS sem ritmus: az „50-55 között van a nyugalmi pulzusom"
        // tartománya munka/pihenő párnak látszott, és ötven másodperc munka,
        // ötvenöt pihenő terv lett belőle. Ha viszont a mondat ki is mondja
        // az intervall-szerkezetet („4 kör, pulzus 165"), az erősebb: ott a
        // pulzus csak megjegyzés a terv mellett.
        if (s.contains("pulzus") || s.contains("nyugalmi") || s.contains("bpm")
                || s.contains("hrv")) {
            if (!saysPlan(s)) return null;
        }
        // Az ALVÁS-NAPLÓ sem ritmus: a „reggel 5:45 ébredés, 20 perc jóga"
        // ébredés-időpontjából munkaszakasz lett, a jógából pihenő – egy
        // hajnali percből huszonöt perces időzítő.
        if ((s.contains("ebredes") || s.contains("lefekves") || s.contains("alvas")
                || s.contains("aludtam") || s.contains("keltem")
                || s.contains("fekudtem")) && !saysPlan(s)) return null;
        // A TEMPÓ sem ritmus: a „10 km @ 5:30" és a „10 km-t futottam 5:30-as
        // tempóval" órán mért futás, nem szakaszos terv – eddig mindkettőből
        // egykörös, öt és fél perces időzítő lett. A táv kimondása mellett
        // csak akkor van terv, ha a mondat ki is mondja.
        if (java.util.regex.Pattern.compile("\\d\\s?-?(?:km|kilometer)(?![a-z])")
                .matcher(s).find() && !saysPlan(s)) return null;
        // A TARTOMÁNY sem ritmus: a „10-15 perc nyújtás" egyetlen, körülbelüli
        // hossz – eddig tíz másodperc munka lett belőle tizenöt perc
        // pihenővel. A kötőjeles pár mellett csak akkor van terv, ha a mondat
        // ki is mondja.
        if (java.util.regex.Pattern
                .compile("\\d{1,3}\\s?-\\s?\\d{1,3}\\s*(?:perc|mp|masodperc|ora)")
                .matcher(s).find() && !saysPlan(s)) return null;
        // A SÚLY- és TÁV-tartomány sem ritmus: a „70-75 kg között
        // ingadozom" hetvenes párja munka/pihenő tervnek látszott.
        if (java.util.regex.Pattern
                .compile("\\d{1,3}\\s?-\\s?\\d{1,3}\\s*(?:kg|kilo|km|ezer)")
                .matcher(s).find() && !saysPlan(s)) return null;
        // A NAPIREND óra-tartománya sem ritmus: az „ebédidő: 12:30-13:00
        // séta" két időpontja munka/pihenő párnak látszott. Két kettőspontos
        // idő kötőjellel a nap órái – terv csak kimondva lehet belőle.
        if (java.util.regex.Pattern
                .compile("\\d{1,2}:\\d{2}\\s?-\\s?\\d{1,2}:\\d{2}")
                .matcher(s).find() && !saysPlan(s)) return null;
        // Az edzőtermi tábla írásmódja: „1:30 munka 0:30 pihenő”. A perc:mp
        // alakot rögtön másodpercre váltjuk, hogy a többi szabály értse.
        s = clockToSeconds(s);
        // A zárójel csak tagolás: a „10x(40s/20s)" ugyanaz, mint a „10x40/20".
        s = s.replace('(', ' ').replace(')', ' ');
        // A csillag ugyanaz a szorzójel, mint az x: „8*20/10".
        s = s.replaceAll("(\\d)\\s?\\*\\s?(\\d)", "$1x$2");

        // 0) AMRAP: „amrap 20 perc” – annyi kör, amennyi belefér. Az időzítőnek
        //    ez EGY hosszú szakasz, nem több rövid; körszámot adni neki éppen
        //    azt venné el, amiről az AMRAP szól.
        if (s.contains("amrap")) {
            int min = numberAfter(s, "amrap");
            // „20 perc amrap”: a szám a név ELŐTT, mértékegységgel.
            if (min <= 0) {
                int sec = firstSeconds(s);
                if (sec >= 60) min = sec / 60;
            }
            if (min >= 1 && min <= 60)
                return new Plan(1, min * 60, 0, warmIn(s), coolIn(s));
        }

        // 1) Ismert forma név szerint. A kimondott körszám felülírja az alapot:
        //    „tabata 6 kör” hat kört jelent, nem nyolcat.
        for (String[] p : PRESETS)
            if (s.contains(p[0])) {
                // A KIMONDOTT séma erősebb a preset-nél: a „tabata dupla:
                // 16x20/10" tizenhat kör – a névre eddig a gyári nyolc jött.
                if (s.matches(".*\\d{1,2}\\s?[x×]\\s?\\d{1,3}\\s?/\\s?"
                        + "\\d{1,3}.*")) break;
                int r = numberBefore(s, "kor");
                if (r <= 0) r = numberBefore(s, "sorozat");
                if (r <= 0) r = numberBefore(s, "round");
                // Az EMOM percenként egy kör: az „emom 12" tizenkét kör, az
                // „emom 20 perc" húsz. A szám a név UTÁN áll, nem előtte.
                if (r <= 0 && p[0].equals("emom")) r = numberAfter(s, "emom");
                // „húsz perc emom”: a szám a név ELŐTT áll, mértékegységgel.
                // Diktálva ez a természetes szórend, és eddig az alapértelmezett
                // tíz kör lett belőle – feleannyi edzés, mint amit kért.
                if (r <= 0 && p[0].equals("emom")) {
                    int sec = firstSeconds(s);
                    if (sec >= 60 && sec <= MAX_ROUNDS * 60) r = sec / 60;
                }
                int rounds = r > 0 && r <= MAX_ROUNDS ? r : Integer.parseInt(p[1]);
                return new Plan(rounds, Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                        warmIn(s), coolIn(s));
            }

        // 2) „6 x 3 perc”: körszám × egy szakasz hossza, mértékegységgel.
        java.util.regex.Matcher mm = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s?[x×]\\s?(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?"
                        + "(masodperc|mperc|mp\\b|perc)")
                .matcher(s);
        if (mm.find()) {
            try {
                int r = Integer.parseInt(mm.group(1));
                double v = Double.parseDouble(mm.group(2).replace(',', '.'));
                int w = (int) Math.round(mm.group(3).startsWith("perc") ? v * 60 : v);
                int rest = secondsBefore(s, REST_WORDS);
                // „5x(3 perc / 1 perc)”: a perjel utáni idő a pihenő. Enélkül
                // a pihenő némán elveszett, és a kör fele lett az edzésnek.
                if (rest <= 0) rest = secondsAfterSlash(s, mm.end());
                Plan p = build(r, w, rest, warmIn(s), coolIn(s));
                if (p != null) return p;
            } catch (NumberFormatException ignored) {
            }
        }

        // 3) „40/20”, „45-15”, „8x20/10”: a teremben ez a rövid írásmód. A
        //    kötőjeles alakot csak akkor fogadjuk el, ha nem sorozat: a
        //    „piramis 20-30-40 mp” nem munka/pihenő pár.
        // A TÁV nem ritmus: az „5-5 km" két ötkilométeres futás, nem öt
        // másodperc munka és öt másodperc pihenő. Az „a héten kétszer
        // futottam 5-5 km-t" mondatból eddig időzítő-terv lett, méghozzá
        // kétköros ötmásodperces – ezért a távval kimondott párt egyetlen
        // számra húzzuk össze, mielőtt a ritmus-minta ránézne.
        String noDist = s.replaceAll(
                "(\\d{1,3})\\s?[/\\-]\\s?\\d{1,3}(\\s?(?:km|kilometer\\w*|meter\\w*))", "$1$2");
        String slashable = noDist.matches(".*\\d\\s?-\\s?\\d{1,3}\\s?-\\s?\\d.*")
                ? noDist.replace('-', ' ') : noDist;
        java.util.regex.Matcher m = java.util.regex.Pattern
                // A mértékegység kiírva is állhat a pár két oldalán:
                // „40 mp / 20 mp”. Enélkül a pihenő némán elveszett.
                // A PERC is állhat a pár két oldalán („2 perc / 1 perc”):
                // enélkül a perces alak kiesett a mintából, és a pihenő némán
                // elveszett – a kör fele lett az edzésnek.
                .compile("(?:(\\d{1,2})\\s?[x×]\\s?)?(\\d{1,3})\\s?"
                        + "(masodperc|mperc|mp|perc|sec|s)?"
                        + "\\s?[/\\-]\\s?(\\d{1,3})\\s?(masodperc|mperc|mp|perc|sec|s)?"
                        + "(?:\\s?[x×]\\s?(\\d{1,2}))?")
                .matcher(slashable);
        if (m.find()) {
            int rounds = m.group(1) != null ? Integer.parseInt(m.group(1))
                    : m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;
            int work = Integer.parseInt(m.group(2)) * ("perc".equals(m.group(3)) ? 60 : 1);
            int rest = Integer.parseInt(m.group(4)) * ("perc".equals(m.group(5)) ? 60 : 1);
            // Tíz másodpercnél rövidebb MUNKA nem ritmus: az „5/3/1" egy
            // ismert erőemelő program neve, nem öt másodperc munka három
            // másodperc pihenővel. A legrövidebb valódi alak a tabata 20/10.
            if (work < 10) return null;
            if (rounds <= 0) {
                int r = numberBefore(s, "kor");
                if (r <= 0) r = numberBefore(s, "sorozat");
                if (r <= 0) r = numberBefore(s, "szett");
                if (r <= 0) r = numberBefore(s, "round");
                rounds = r;
            }
            // A pártól TÁVOLABB álló „10x" is körszám: a „30-30 intervall
            // 10x" tíz kör – eddig egy lett belőle, és az időzítő az első
            // perc után leállt.
            if (rounds <= 0) {
                java.util.regex.Matcher xm = java.util.regex.Pattern
                        .compile("(?<![\\dx(])(\\d{1,2})\\s?x(?![\\da-z])")
                        .matcher(s);
                if (xm.find()) {
                    int r = Integer.parseInt(xm.group(1));
                    if (r >= 2 && r <= MAX_ROUNDS) rounds = r;
                }
            }
            // „20 perc alatt 40/20”: a körszám a teljes időből jön ki. A
            // teremben gyakran így mondják, és fejben osztani edzés előtt a
            // legrosszabb pillanat.
            if (rounds <= 0) rounds = roundsFromTotal(s, work + rest);
            Plan p = build(rounds, work, rest, warmIn(s), coolIn(s));
            if (p != null) return p;
        }

        // 4) Kimondott alak: „3 kör 40 mp munka 20 mp pihenő”.
        int rounds = numberBefore(s, "kor");
        if (rounds <= 0) rounds = numberBefore(s, "sorozat");
        if (rounds <= 0) rounds = numberBefore(s, "szett");
        if (rounds <= 0) rounds = numberBefore(s, "round");
        // Az időzítőnél az „ismétlés" a kört jelenti, nem a súlyzós
        // ismétlésszámot: a „3 perc munka 1 perc pihenő, 6 ismétlés" hat kör.
        if (rounds <= 0) rounds = numberBefore(s, "ismetles");
        if (rounds <= 0) rounds = numberAfterColon(s, "kor");
        if (rounds <= 0) rounds = numberAfterColon(s, "round");
        if (rounds <= 0) rounds = roundsPrefix(s);
        // A magában álló „10x" is körszám: a „30-30 intervall 10x" tíz kör
        // – eddig egyetlen kör lett belőle, és az időzítő az első perc után
        // leállt. A „4x800" alakot a szám utáni számjegy zárja ki.
        if (rounds <= 0) {
            java.util.regex.Matcher xm = java.util.regex.Pattern
                    .compile("(?<![\\dx(])(\\d{1,2})\\s?x(?![\\da-z])").matcher(s);
            if (xm.find()) {
                int r = Integer.parseInt(xm.group(1));
                if (r >= 2 && r <= MAX_ROUNDS) rounds = r;
            }
        }
        // Az „on" NEM szerepel: szó belsejében is előfordul („huszonöt"),
        // és a secondsBefore nem néz szóhatárt. Az „off" mellett a munkaidő
        // úgyis az első kimondott időből jön.
        int work = secondsBefore(s, WORK_WORDS, REST_WORDS);
        int rest = secondsBefore(s, REST_WORDS, WORK_WORDS);
        if (rest <= 0) rest = bareSecondsBefore(s, REST_WORDS, spokenUnit(s));
        // Vesszővel tagolt mezőlista kettőspont nélkül: „kör 8, munka 30,
        // pihenő 30". A tagmondatban a szón és a számon KÍVÜL nincs semmi,
        // tehát nincs mit félreérteni – kettőspont nélkül eddig mégis
        // kimaradt az egész terv.
        if (work <= 0) work = fieldNumber(s, WORK_WORDS);
        if (rest <= 0) rest = fieldNumber(s, REST_WORDS);
        if (rounds <= 0) rounds = fieldNumber(s, new String[]{"kor", "round", "sorozat"});
        // 5) Idő-vezérelt formák: a mondat a HOSSZT mondja ki, nem a
        //    körszámot. Csak akkor jutunk ide, ha a kimondott számokból nem
        //    jött ki se munka, se pihenő – az explicit alak mindig erősebb.
        if (work <= 0 && rest <= 0) {
            Plan t = timedForm(s, rounds);
            if (t != null) return t;
        }

        // „5 kör 30 másodperc” – ha csak egy időt mondanak, az a munka. De
        // megnevezetlenül csak életszerű munkaidőt fogadunk el: a „minden
        // percben 1 kör, 15 percig” 15 perce nem egyetlen szakasz hossza.
        if (work <= 0) {
            // Ha csak a pihenőt nevezték meg („egy perc plank, 30 mp pihenő”),
            // a munkaidő az első kimondott idő.
            //
            // De ha a mondat EGYETLEN ideje maga a pihenő, a munkaidőt nem
            // tudjuk: a „10x400 méter 90 mp pihenővel" kilencvenese a
            // pihenő – munkaidőnek is beírva 90/90-es terv született, aminek
            // a négyszáz méterhez semmi köze, és a valódi bejegyzés (négy
            // kilométer futás) mögé szorult. Táv-alapú intervallumnál a
            // futás a napló, nem egy kitalált időzítő.
            // (A „5 körönként 1 perc pihenő" marad terv: ott a szakaszt nem
            // a táv határozza meg, csak épp nincs kimondva.)
            if (rest > 0 && timeCount(s) < 2
                    && s.matches("(?s).*\\d{1,2}\\s?[x×]\\s?\\d{2,4}\\s?"
                            + "(?:kilometer\\w*|meter\\w*|km|m)(?![a-z]).*"))
                return null;
            work = firstSeconds(s);
            if (rest <= 0 && work > 600) return null;
        }
        // A körszám a HOSSZBÓL is kijön: az „1 perc gyors, 1 perc laza,
        // felváltva 20 percig" tíz kör. Eddig egyetlen kör lett belőle,
        // vagyis a húszperces edzésből kétperces – és az időzítő a második
        // kör előtt leállt. Csak kimondott össz-idő mellett lép be
        // („percig", „alatt", „összesen"), így a szakasz-hosszal nem
        // téveszthető össze.
        if (rounds <= 0) rounds = roundsFromTotal(s, work + rest);
        // Egy szám nem terv. A „sétáltam 20 percet", a „csípő mobilitás
        // gyakorlatok 15 perc" és a „hengereltem a hátamat 10 percet"
        // egyetlen kimondott időt tartalmaz – a séta és a gyakorlat szava
        // viszont szakasz-szó is, így mindháromból egykörös időzítő-terv
        // lett. Ha se körszám, se második idő, se szakasz-szó nincs, akkor a
        // mondatban semmi nem utal szakaszokra. (A tabata, a hiit, az amrap
        // és a fartlek fentebb, a saját ágán tér vissza.)
        // A PUSZTA időtartam viszont marad: az időzítő mezőjébe beírt „2 perc"
        // épp ezt kéri – ott a mondatban semmi más nincs.
        if (rounds <= 1 && timeCount(s) < 2 && !saysPlan(s) && !onlyTime(s))
            return null;
        // Két KÜLÖNBÖZŐ tevékenység nem munka/pihenő pár: a „20 perc jóga és
        // 10 perc meditáció" húsz percéből húszperces munka lett húszperces
        // pihenővel – negyvenperces időzítő egy félórás estére. Egyenlő
        // munka és pihenő egyetlen körben sosem valódi terv.
        if (rounds <= 1 && rest > 0 && work == rest && !saysPlan(s)) return null;
        // EGY kör, pihenő nélkül nem ritmus: az „5 perc szauna, 30 perc
        // úszás" ötperces „tervet" ajánlott – két kimondott időből, amelyek
        // közül az egyik nem is mozgás. A kimondott terv (amrap, tabata,
        // „x kör") a saját ágán úgyis visszatér, a puszta időmező pedig az
        // onlyTime-on.
        if (rounds <= 1 && !saysPlan(s) && !onlyTime(s)) return null;
        // A SÚLYZÓS SZETT nem intervallum: a „guggolás 5x5 100 kg, pihi
        // 3 perc" ötször ötje sorozat és ismétlés, a pihi pedig a sorozatok
        // közti szünet. Eddig ötkörös, háromperc munka – háromperc pihenő
        // időzítő lett belőle, vagyis fél óra a semmiből egy erőedzés
        // mellé. A súly kimondása dönt: kilogrammot senki nem ír egy
        // intervall-tervbe.
        if (weightedSets(s) && !saysPlan(s)) return null;
        // A BEMELEGÍTÉS–EDZÉS–LEVEZETÉS hármas nem ritmus: a „10 perc
        // bemelegítés, 40 perc futás, 10 perc levezetés" egyetlen negyven
        // perces futás. Eddig egykörös, negyvenperc munka – tízperc pihenő
        // időzítő-terv lett belőle: a nap felépítéséből egy ötvenperces
        // ketyegő. Körszámmal együtt viszont valódi terv marad, a
        // bemelegítés pont oda való.
        if (rounds <= 1 && !saysPlan(s) && (warmIn(s) > 0 || coolIn(s) > 0))
            return null;
        return build(rounds, work, rest, warmIn(s), coolIn(s));
    }

    /**
     * Súlyzós sorozat-jelölés: „5x5 100 kg”, „4×8 70 kg”. A szorzat mindkét
     * tagja ismétlésszám (idő-egység nem áll utána), és a mondat kilogrammot
     * is kimond.
     */
    private static boolean weightedSets(String s) {
        if (!s.matches(".*\\d\\s?kg(?![a-z]).*")) return false;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d,.])(\\d{1,2})\\s?[x×]\\s?(\\d{1,3})(?![\\dx])")
                .matcher(s);
        while (m.find()) {
            String tail = s.substring(m.end());
            // A „8x400 m” és a „10x30 mp” nem szett: ott a második szám táv
            // vagy idő, nem ismétlésszám.
            if (tail.matches("(?s)\\s?(m|km|mp|perc|masodperc|min)(?![a-z]).*")) continue;
            return true;
        }
        return false;
    }

    /** Csak időtartam áll a mondatban, más szó nélkül („2 perc", „90 mp"). */
    private static boolean onlyTime(String s) {
        String rest = s.replaceAll("\\d+([.,]\\d+)?", " ")
                .replaceAll("(?<![a-z])(mp|masodperc|perc|percet|percig|min|s|ora|orat"
                        + "|orai|orara|orán)(?![a-z])", " ")
                .replaceAll("[^a-z]", " ").trim();
        return rest.isEmpty();
    }

    /** Hány KÜLÖN kimondott idő van a mondatban („30 mp", „2 perc"). */
    private static int timeCount(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d,.])\\d+(?:[.,]\\d+)?\\s*"
                        + "(?:mp|masodperc|perc|min|s)(?![a-z])").matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /**
     * Idő-vezérelt formák: a HOSSZ van kimondva, a körszám nincs.
     *
     * A „hiit 20 perc”, a „fartlek fél óra” és az „e2mom 20 perc” ugyanolyan
     * hétköznapi mondat, mint a „tabata” – csak épp nem fix körszámú forma,
     * hanem fix RITMUSÚ: a hossz mondja meg, hányszor fut le. Eddig
     * mindhárom üresen tért vissza, pedig egyikben sincs semmi kétértelmű.
     *
     * A szándékosan választott ritmusok: a HIIT a legelterjedtebb 30/30, a
     * fartlek 1 perc gyors / 1 perc könnyű, az EnMOM pedig definíció szerint
     * n percenként egy kör (pihenő nélkül, mert az a maradék idő).
     */
    private static Plan timedForm(String s, int rounds) {
        int work = 0, rest = 0;
        if (s.contains("hiit")) { work = 30; rest = 30; }
        else if (s.contains("fartlek")) { work = 60; rest = 60; }
        else {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?<![a-z])e\\s?(\\d)\\s?mom(?![a-z])").matcher(s);
            if (m.find()) work = Integer.parseInt(m.group(1)) * 60;
            else {
                // „2 percenként 10 kör”: ugyanaz magyarul kimondva.
                m = java.util.regex.Pattern
                        .compile("(\\d{1,2})\\s?perc(?:enkent|ente)").matcher(s);
                if (m.find()) work = Integer.parseInt(m.group(1)) * 60;
            }
        }
        if (work <= 0) return null;
        int r = rounds;
        if (r <= 0) {
            int total = statedMinutes(s) * 60;
            if (total >= work + rest) r = total / (work + rest);
        }
        if (r <= 0) r = 10;                       // kimondatlanul tíz kör
        if (r > MAX_ROUNDS) r = MAX_ROUNDS;
        Plan p = build(r, work, rest, warmIn(s), coolIn(s));
        if (p != null) p.guessed = true;
        return p;
    }

    /**
     * A mondatban kimondott leghosszabb időtartam percben (0, ha nincs).
     *
     * A tizedes is számít: a „fél óra” a számmá alakítás után „0,5 óra”, és
     * ha csak az egész részt néznénk, az öt óra lenne belőle – ötven kör egy
     * félórás fartlekből.
     */
    private static int statedMinutes(String s) {
        double best = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,3}(?:[.,]\\d{1,2})?)\\s?(perc|ora)(?![a-z])").matcher(s);
        while (m.find()) {
            double v;
            try { v = Double.parseDouble(m.group(1).replace(',', '.')); }
            catch (NumberFormatException e) { continue; }
            if (m.group(2).equals("ora")) v *= 60;
            if (v > best && v <= 24 * 60) best = v;
        }
        return (int) Math.round(best);
    }

    /**
     * Körszám a kimondott teljes időből: „20 perc alatt 40/20”. Csak
     * félreérthetetlen jelzőszóval („alatt”, „összesen”, „percig”), különben
     * a „3 kör 40 mp” munkaideje válna teljes idővé.
     */
    private static int roundsFromTotal(String s, int cycle) {
        if (cycle <= 0) return 0;
        // Az ÓRÁS teljes idő is teljes idő: a „30 mp sprint 90 mp séta
        // váltakozva fél órán át" tizenöt kör – eddig egyetlen kör lett,
        // mert a teljes hossz óra-szóval állt, nem perccel.
        s = s.replaceAll("(?<![a-z\\d,.])(?:fel|0,5) ora(?:n at| alatt|ig"
                + "|n keresztul)(?![a-z])", "30 percig");
        s = s.replaceAll("(?<![a-z\\d,.])masfel ora(?:n at| alatt|ig"
                + "|n keresztul)(?![a-z])", "90 percig");
        s = s.replaceAll("(?<![a-z\\d,.])(?:egy|1) ora(?:n at| alatt|ig"
                + "|n keresztul)(?![a-z])", "60 percig");
        // A „percen át" ugyanaz, mint a „percig" – csak más raggal.
        s = s.replaceAll("(\\d{1,3})\\s?perc(?:en at|en keresztul)(?![a-z])",
                "$1 percig");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,3})\\s?perc(?:ig)?(?:\\s?(?:alatt|osszesen))?")
                .matcher(s);
        while (m.find()) {
            boolean marked = m.group(0).contains("alatt") || m.group(0).contains("osszesen")
                    || m.group(0).contains("percig");
            if (!marked) continue;
            int total;
            try {
                total = Integer.parseInt(m.group(1)) * 60;
            } catch (NumberFormatException e) {
                continue;
            }
            int r = total / cycle;
            if (r >= 2 && r <= MAX_ROUNDS) return r;
        }
        // Jelölő szó nélkül is egyértelmű, ha EGYETLEN perc-adat van a
        // mondatban: a „hiit 20 perc, 30/30" húsz perce csak a terv hossza
        // lehet, más nincs, amire vonatkozhatna. Eddig egykörös terv lett
        // belőle – fél perc munka, és kész. A bemelegítés és a levezetés
        // saját szóval jelöli magát, azt kihagyjuk.
        m = java.util.regex.Pattern.compile("(?<![\\d,.])(\\d{1,3})\\s?perc").matcher(s);
        int only = 0, seen = 0;
        while (m.find()) {
            String after = s.substring(Math.min(s.length(), m.end()));
            if (after.startsWith(" bemelegites") || after.startsWith(" levezetes")
                    || after.startsWith(" pihen") || after.startsWith(" szunet")
                    || after.startsWith(" munka") || after.startsWith(" sprint")) continue;
            seen++;
            try { only = Integer.parseInt(m.group(1)) * 60; }
            catch (NumberFormatException e) { return 0; }
        }
        if (seen == 1) {
            int r = only / cycle;
            if (r >= 2 && r <= MAX_ROUNDS) return r;
        }
        return 0;
    }

    /** Bemelegítés a mondatból („2 perc bemelegítés”). */
    static int warmIn(String s) {
        return secondsBefore(Foods.norm(s), new String[]{"bemelegit", "warmup", "warm up"});
    }

    /** Levezetés a mondatból („levezetés 3 perc”). */
    static int coolIn(String s) {
        return secondsBefore(Foods.norm(s), new String[]{"levezet", "nyujtas", "cooldown",
                "cool down"});
    }

    /** „1:30” → „90 mp”. Csak érvényes perc:másodperc alak, 0–59 másodperccel. */
    static String clockToSeconds(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                // A NAPSZAK nem hossz: a „7:15-kor keltem" hét óra tizenöt,
                // nem négyszázharmincöt másodperc munka. A „-kor" (és az
                // „órakor", a „-tól", a „-ig") ragos alak időpont – abból
                // eddig időzítő-terv lett, a felkelés órájából.
                .compile("(?<![\\d:])(\\d{1,2}):([0-5]\\d)(?![\\d:])"
                        + "(?!\\s?-?\\s?(?:kor|orakor|tol|to\'l|ig|kortol|koreig))").matcher(s);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            int sec = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
            m.appendReplacement(b, sec + " mp");
        }
        m.appendTail(b);
        return b.toString();
    }

    private static Plan build(int rounds, int work, int rest) {
        return build(rounds, work, rest, 0, 0);
    }

    private static Plan build(int rounds, int work, int rest, int warm, int cool) {
        if (work < MIN_SEC || work > MAX_SEC) return null;
        if (rest < 0 || rest > MAX_SEC) rest = 0;
        if (warm < 0 || warm > MAX_SEC) warm = 0;
        if (cool < 0 || cool > MAX_SEC) cool = 0;
        if (rounds <= 0) rounds = 1;
        if (rounds > MAX_ROUNDS) return null;
        Plan p = new Plan(rounds, work, rest, warm, cool);
        // A szakaszok külön-külön hihetőek lehetnek, EGYÜTT mégsem: a
        // „8x 60 perc" nyolc órás időzítőt állítana be. Ilyen edzés nincs –
        // az ilyen mondat félreértés, és jobb rá azt mondani, hogy nem
        // értjük, mint egy egész napra elindítani az órát.
        if (p.totalSec() > MAX_TOTAL_SEC) return null;
        return p;
    }

    /**
     * A megnevezett szakasz hossza másodpercben. A szám állhat a szó előtt
     * („40 mp munka”) és utána is („munka 40 mp”) – mindkettőt így mondják.
     */
    private static int secondsBefore(String s, String[] words) {
        return secondsBefore(s, words, new String[0]);
    }

    /**
     * Ugyanaz, de a MÁSIK szakasz neve is határ.
     *
     * A vessző eddig egyedül jelölte a szakaszhatárt, márpedig sokan nem
     * tesznek vesszőt: a „45 másodperc munka 15 pihenő” mondatban a pihenő elé
     * eső egyetlen kimondott idő a negyvenöt volt – vagyis a pihenő is
     * negyvenöt másodperc lett. A terv létrejött, csak háromszor hosszabb
     * szünettel, mint amit az ember kért.
     */
    private static int secondsBefore(String s, String[] words, String[] stops) {
        for (String w : words) {
            int p = s.indexOf(w);
            while (p >= 0) {
                // A tagmondathatár számít: a „munka 30 mp, pihenő 10 mp”
                // mondatban a pihenő elé eső 30 a MÁSIK szakaszé.
                int from = Math.max(0, p - 22);
                for (int i = p - 1; i >= from; i--)
                    if (isBreak(s, i)) { from = i + 1; break; }
                for (String st : stops) {
                    int q = s.lastIndexOf(st, p - 1);
                    if (q >= from && q + st.length() <= p) from = q + st.length();
                }
                int before = timeIn(s, from, p, true);
                if (before > 0) return before;
                int end = p + w.length();
                int to = Math.min(s.length(), end + 22);
                for (int i = end; i < to; i++)
                    if (isBreak(s, i)) { to = i; break; }
                for (String st : stops) {
                    int q = s.indexOf(st, end);
                    if (q >= 0 && q < to) to = q;
                }
                int after = timeIn(s, end, to, false);
                if (after > 0) return after;
                p = s.indexOf(w, p + 1);
            }
        }
        return 0;
    }

    /**
     * Mértékegység nélküli szám a szakasz-szó előtt: „45 mp munka 15 pihenő”.
     *
     * A mértékegységet a mondatban ELSŐKÉNT kimondott időtől örökli, mert így
     * beszél az ember: ha az első szám másodperc, a többi is az. Enélkül a
     * szakasz üresen maradt, és a terv szünet nélkül indult.
     */
    private static int bareSecondsBefore(String s, String[] words, int unitSec) {
        if (unitSec <= 0) return 0;
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile("(\\d{1,3}) ?$");
        for (String w : words) {
            int p = s.indexOf(w);
            while (p >= 0) {
                java.util.regex.Matcher m =
                        pat.matcher(s.substring(Math.max(0, p - 5), p));
                if (m.find()) {
                    int v = Integer.parseInt(m.group(1)) * unitSec;
                    if (v >= MIN_SEC && v <= MAX_SEC) return v;
                }
                p = s.indexOf(w, p + 1);
            }
        }
        return 0;
    }

    /**
     * Szám a szó után, ha a TAGMONDAT csak ebből a kettőből áll: „munka 30".
     *
     * Ez a táblára írt terv alakja. Kettősponttal már értettük; anélkül
     * viszont nem találgatunk általánosan (a „kör 40 mp munka" negyvene
     * munkaidő, nem negyven kör) – itt a tagmondat üressége a garancia.
     */
    private static int fieldNumber(String s, String[] words) {
        for (String part : s.split("[,;]")) {
            // A mondatzáró jel is lekerül: a „pihenő 30." ugyanaz, mint a
            // „pihenő 30" – a minta viszont a tagmondat VÉGÉHEZ van kötve.
            String t = part.trim();
            int e = t.length();
            while (e > 0 && !Character.isLetterOrDigit(t.charAt(e - 1))) e--;
            t = t.substring(0, e);
            for (String w : words) {
                if (!t.startsWith(w)) continue;
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("^\\w*\\s*:?\\s*(\\d{1,3})$").matcher(t.substring(w.length()));
                if (!m.find()) continue;
                try {
                    int v = Integer.parseInt(m.group(1));
                    if (v >= 1 && v <= MAX_SEC) return v;
                } catch (NumberFormatException ignored) { }
            }
        }
        return 0;
    }

    /**
     * A mondatban elsőként kimondott idő MÉRTÉKEGYSÉGE másodpercben: 60, ha
     * percben beszél, 1, ha másodpercben, 0, ha egyáltalán nem mondott
     * mértékegységet.
     */
    private static int spokenUnit(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\\d{1,4}(?:[.,]\\d{1,2})? ?(masodperc|mperc|mp\\b|sec\\b|s\\b|perc|min\\b)")
                .matcher(s);
        if (!m.find()) return 0;
        String u = m.group(1);
        return u.startsWith("perc") || u.startsWith("min") ? 60 : 1;
    }

    /**
     * Tagmondathatár-e az adott karakter? A tizedesvessző nem az: a
     * „0,5 perc munka” fél perce különben öt percre ugrott volna.
     */
    private static boolean isBreak(String s, int i) {
        char c = s.charAt(i);
        if (c != ',' && c != ';') return false;
        return !(i > 0 && Character.isDigit(s.charAt(i - 1))
                && i + 1 < s.length() && Character.isDigit(s.charAt(i + 1)));
    }

    /** Az első időtartam a mondatban, mértékegységgel együtt. */
    private static int firstSeconds(String s) {
        // A bemelegítés és a levezetés tagmondata kimarad: a „bemelegítés 5
        // perc, 10 kör 1/1, levezetés 5 perc" munkaideje nem öt perc – eddig
        // viszont pont az lett, vagyis a kör ugyanolyan hosszú volt, mint a
        // bemelegítés.
        int from = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i < s.length() && s.charAt(i) != ',' && s.charAt(i) != ';') continue;
            String part = s.substring(from, i);
            from = i + 1;
            boolean edge = false;
            for (String w : new String[]{"bemelegit", "warmup", "warm up", "levezet",
                    "nyujtas", "cooldown", "cool down"})
                if (part.contains(w)) { edge = true; break; }
            if (edge) continue;
            int v = timeIn(part, 0, part.length(), false);
            if (v > 0) return v;
        }
        return 0;
    }

    /**
     * Időtartam a megadott szakaszban: „40 mp”, „30 masodperc”, „1 perc”,
     * „1,5 perc”. Mértékegység nélküli szám nem idő – abból lett volna a
     * „3 kör” háromszor három másodperc.
     *
     * @param last a szó ELŐTTI szakaszban az UTOLSÓ időt keressük: a
     *             „40 mp munka 20 mp pihenő” mondatban a pihenőhöz a 20
     *             tartozik, nem a szakasz elején álló 40.
     */
    private static int timeIn(String s, int from, int to, boolean last) {
        if (from < 0 || to > s.length() || from >= to) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,4}(?:[.,]\\d{1,2})?)\\s?(masodperc|mperc|mp\\b|sec\\b|s\\b|perc|min\\b)")
                .matcher(s.substring(from, to));
        int found = 0;
        while (m.find()) {
            double v;
            try {
                v = Double.parseDouble(m.group(1).replace(',', '.'));
            } catch (NumberFormatException e) {
                continue;
            }
            String u = m.group(2);
            // A „perc” hosszabb, mint az „mp”: a sorrend a regexben számít.
            int sec = (int) Math.round(u.startsWith("perc") || u.startsWith("min")
                    ? v * 60 : v);
            if (sec >= 1 && sec <= MAX_SEC) {
                if (!last) return sec;
                found = sec;
            }
        }
        return found;
    }

    /**
     * Kiírt számok és a „-szor/-szer” alak számjegyre váltása. A
     * szorzószám itt körszámot jelent: „20/10 nyolcszor” nyolc kör.
     */
    static String digits(String s) {
        // A kötőjel is odatartozik: a „10-szer" ugyanazt jelenti, mint a
        // „tízszer". Nélküle a terv egyszer futott le tíz helyett – a szám
        // ott volt a mondatban, csak nem jutott el a körszámig.
        // A „KÉTSZER ÉBREDTEM" viszont alvás-napló, nem két kör: az
        // ébredés-ige előtti szorzószám nem válhat körré, mert a
        // szintetikus „kör" szó a terv-őröket is kicselezte, és az
        // éjszakából kétkörös időzítő lett.
        return Hu.digits(s).replaceAll("(\\d+)\\s?-?\\s?(szor|szer)\\b"
                + "(?!\\s*(?:ebred|felebred|felkel|keltem|megszakad))", "$1 kor");
    }

    /** Az első szám a szó után: „emom 12” → 12. */
    private static int numberAfter(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            int b = p + word.length();
            while (b < s.length() && s.charAt(b) == ' ') b++;
            int e = b;
            while (e < s.length() && Character.isDigit(s.charAt(e))) e++;
            if (e > b) {
                try {
                    int v = Integer.parseInt(s.substring(b, e));
                    if (v >= 1 && v <= 999) return v;
                } catch (NumberFormatException ignored) {
                }
            }
            p = s.indexOf(word, p + 1);
        }
        return 0;
    }

    /**
     * A pihenőt jelölő szavak, EGY helyen.
     *
     * Korábban két listában éltek, és a rövidebbikből hiányzott a „séta": a
     * „8 kör: 20 mp sprint, 40 mp séta" pihenője így elveszett, az edzés
     * pedig szünet nélküli lett.
     */
    private static final String[] REST_WORDS = {"piheno", "pihenes", "szunet",
            "lazitas", "seta", "lassu", "rest", "off",
            // Ugyanez a hiány, másik szavakkal: a futó „járás"-t, „kocogás"-t
            // és „gyaloglás"-t ír a pihenő szakaszra, a magyar beszélt nyelv
            // meg „pihi"-t. Mindegyik ugyanaz a szünet – enélkül az edzés
            // szünet nélkülinek látszott, és a kör fele elveszett.
            "jaras", "gyaloglas", "kocogas", "pihi", "regeneracio",
            // Az intervall-edzés másik szótára a KÖNNYŰ és a KEMÉNY: a
            // „3 perc kemény, 2 perc könnyű" ugyanaz a ritmus, csak épp
            // egyetlen munka-szakasszá olvadt, pihenő nélkül.
            "konnyu", "laza", "konnyed", "levezetes",
            // Az internetről másolt angol terv szótára: a „recovery" és az
            // „easy" ugyanaz a szünet. A „4x4 min at threshold, 3 min
            // recovery" hármasa eddig elveszett, és pihenő nélküli tervet
            // ajánlott az app.
            "recovery", "easy", "jog", "walk", "break"};

    /**
     * Kimondja-e a mondat magát a szakaszos szerkezetet?
     *
     * A szó ELEJÉHEZ kötve: a „gyaKORlat" közepén ott a „kör", és enélkül a
     * „csípő mobilitás gyakorlatok 15 perc" tervnek látszott – egykörös,
     * tizenöt perces időzítőnek. A szó VÉGE szabad, mert a magyar ragoz
     * („körben", „sorozatot").
     */
    private static boolean saysPlan(String s) {
        // A „pihenő" a terv szava, a „pihenés" a hétköznapi szó: az „1 óra
        // pihenés után 30 perc bringa" tervnek látszott – egy óra munka, egy
        // óra pihenő. A terv-alak ragozva is „pihenő" marad.
        for (String w : new String[]{"kor", "round", "munka", "piheno", "tabata",
                "emom", "amrap", "intervall", "interval", "hiit", "szett", "sorozat",
                "fartlek", "sprint", "ismetles"}) {
            int p = s.indexOf(w);
            while (p >= 0) {
                // A PIHENŐNAP nem terv: az „aktív pihenőnap: 30 perc séta"
                // félórája egykörös, munka-pihenő párrá vált.
                boolean dayOff = w.equals("piheno") && s.startsWith("nap", p + w.length());
                // A „KORahajnali", a „korán" és a „korosztály" nem kör: a
                // szó eleji „kor" miatt a hajnali ébredésből egykörös
                // időzítő lett.
                // Az IDŐPONT „-kor" ragja sem kör: a „6:15-kor keltem" miatt
                // a mondat tervnek látszott, és a naplóból egykörös,
                // negyvenöt perces időzítő lett. A rag előtt szám áll.
                boolean clockSuffix = w.equals("kor") && p > 0
                        && (Character.isDigit(s.charAt(p - 1))
                            || (s.charAt(p - 1) == '-' && p > 1
                                && Character.isDigit(s.charAt(p - 2))));
                // A KORTY, a KORSÓ és a KÓRHÁZ sem kör: a „3 korty bor"
                // miatt a mondat kimondott tervnek számított.
                boolean notRound = clockSuffix
                        || (w.equals("kor") && !roundWord(s, p))
                        || w.equals("kor")
                        && (s.startsWith("an", p + 3) || s.startsWith("ai", p + 3)
                            || s.startsWith("abb", p + 3)
                            || s.startsWith("ahajnal", p + 3)
                            || s.startsWith("osztaly", p + 3)
                            || s.startsWith("haz", p + 3));
                // A „MUNKA előtti úszás" munkája az állás, nem a
                // munkaszakasz: miatta a 6:00-6:45 óra-tartományból
                // időzítő-terv lett.
                if (w.equals("munka")
                        && (s.startsWith(" elott", p + 5)
                            || s.startsWith(" utan", p + 5)
                            || s.startsWith(" kozben", p + 5)
                            || s.startsWith(" miatt", p + 5)
                            || s.startsWith("ba", p + 5)
                            || s.startsWith("bol", p + 5)
                            || s.startsWith("hoz", p + 5)
                            || s.startsWith("hely", p + 5)
                            || s.startsWith("nap", p + 5))) notRound = true;
                if (!dayOff && !notRound
                        && (p == 0 || !Character.isLetter(s.charAt(p - 1))))
                    return true;
                p = s.indexOf(w, p + 1);
            }
        }
        return false;
    }

    /** A munkaszakaszt jelölő szavak – a pihenőnek ez a határa, és fordítva. */
    private static final String[] WORK_WORDS = {"munka", "aktiv", "terheles",
            "gyakorlat", "work",
            // A könnyű szakasz párja: a „kemény", az „erős" és a „gyors".
            "kemeny", "eros", "gyors", "hajra",
            // Ugyanez angolul: a „hard", a „threshold" és az „on" a munka.
            "hard", "threshold", "fast", "effort"};

    /** Perjellel elválasztott második idő: „3 perc / 1 perc” → 60. */
    private static int secondsAfterSlash(String s, int from) {
        if (from < 0 || from >= s.length()) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^\\s?[/-]\\s?(\\d{1,3})\\s?(masodperc|mperc|mp|perc)?")
                .matcher(s.substring(from));
        if (!m.find()) return 0;
        int v = Integer.parseInt(m.group(1));
        String unit = m.group(2);
        int sec = unit != null && unit.startsWith("perc") ? v * 60 : v;
        return sec >= MIN_SEC && sec <= MAX_SEC ? sec : 0;
    }

    /**
     * Szám a szó után, de CSAK kettősponttal: „kör: 6”.
     *
     * A táblára írt terv gyakran mezőkből áll („kör: 6, munka: 40mp”), és ott a
     * körszám a szó mögött van. Kettőspont nélkül nem találgatunk: a puszta
     * „kör 40 mp munka” negyvene munkaidő, nem negyven kör.
     */
    private static int numberAfterColon(String s, String word) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(word + "\\w*\\s?:\\s?(\\d{1,3})").matcher(s);
        if (m.find()) {
            try {
                int v = Integer.parseInt(m.group(1));
                if (v >= 1 && v <= 999) return v;
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    /**
     * Vezető szorzó körszámként: „10x30s on 30s off” tíz kör.
     *
     * A perjeles alaknál („8x20/10”) ez már megvolt, a kiírtnál nem – ott a
     * tíz kör csendben egyre olvadt, és az edzés a tizedénél véget ért.
     */
    private static int roundsPrefix(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                // A szorzó és a szám közt több szóköz is állhat: a
                // zárójelet a felismerő már szóközre cserélte, így a
                // „8x (30 mp munka + 30 mp pihenő)" nyolcasa eddig
                // elveszett, és egyetlen kör maradt belőle.
                .compile("(?<![\\d.,])(\\d{1,2})\\s*[x×]\\s*(?=\\d)").matcher(s);
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v >= 2 && v <= MAX_ROUNDS) return v;
        }
        // Záró szorzó: „30 mp on 30 mp off 10x”. A mondatvégi írásjel nem
        // ronthatja el: aki pontot tesz a végére – vagy egy smiley-t –,
        // ugyanazt mondta, és eddig egy körre olvadt az egész edzése.
        m = java.util.regex.Pattern.compile("(?<![\\d.,])(\\d{1,2})\\s?[x×][\\s.,;:)(-]*$")
                .matcher(s.trim());
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v >= 2 && v <= MAX_ROUNDS) return v;
        }
        return 0;
    }

    /** A „kor" helyén valóban a KÖR szó áll-e (nem korty, korsó, kórház…)? */
    private static boolean roundWord(String s, int p) {
        int e = p + 3;
        StringBuilder suf = new StringBuilder();
        while (e < s.length() && Character.isLetter(s.charAt(e))) suf.append(s.charAt(e++));
        String t = suf.toString();
        return t.isEmpty() || t.equals("t") || t.equals("e") || t.equals("ok")
                || t.equals("oket") || t.equals("ben") || t.equals("re")
                || t.equals("rel") || t.equals("onkent") || t.equals("os")
                || t.equals("nkent") || t.equals("ig");
    }

    /** A megadott szó ELŐTT álló szám („3 kör”), vagy 0. */
    private static int numberBefore(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            // A „KORTY", a „KORSÓ" és a „KÓRHÁZ" nem kör: a „30 perc laza
            // bringa, 3 korty bor" hármasából háromkörös időzítő-terv lett,
            // és a bringa el is tűnt a naplóból – a mondat tervnek
            // minősült. A magyar rag viszont szabad („körben", „köröket").
            if (word.equals("kor") && !roundWord(s, p)) {
                p = s.indexOf(word, p + 1);
                continue;
            }
            int e = p;
            while (e > 0 && s.charAt(e - 1) == ' ') e--;
            int b = e;
            while (b > 0 && Character.isDigit(s.charAt(b - 1))) b--;
            if (b < e) {
                try {
                    int v = Integer.parseInt(s.substring(b, e));
                    if (v >= 1 && v <= 999) return v;
                } catch (NumberFormatException ignored) {
                }
            }
            p = s.indexOf(word, p + 1);
        }
        return 0;
    }
}
