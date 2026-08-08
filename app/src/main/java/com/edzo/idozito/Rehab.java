package com.edzo.idozito;

/**
 * Megelőzés és rehab: gyógytornász-ihletésű gyakorlatsorok testtájanként.
 *
 * A sportolók fele nem az edzés hiányán bukik el, hanem egy sérülésen: a
 * kificamodott bokán, a beálló derékon, a fájó vállon, a golfkönyökön. A
 * megelőzés gyakorlatai jól ismertek és otthon elvégezhetők – csak épp
 * senki nem rakja őket össze edzéssé. Itt testtájat választasz, és kapsz
 * egy kész, 10–15 perces sort: gyakorlat, adagolás, technikai tipp.
 *
 * FONTOS, és minden lapon ki is mondjuk: ez megelőzés és általános erősítés,
 * nem orvoslás. Éles fájdalom, zsibbadás, duzzanat vagy éjszakai fájdalom
 * esetén a helyes lépés az orvos vagy a gyógytornász, nem egy app.
 *
 * A tartalom a fizioterápiában bevett, konzervatív gyakorlatokból áll
 * (excentrikus erősítés, izolált stabilizálás, fokozatos terhelés) –
 * eszközigénye legfeljebb egy gumiszalag és egy kézisúlyzó.
 */
public final class Rehab {

    private Rehab() {}

    /** Egy gyakorlat: név, adagolás, technikai tipp, videó-kereső. */
    public static final class Ex {
        public final String name, dose, cue, video;
        Ex(String name, String dose, String cue, String video) {
            this.name = name; this.dose = dose; this.cue = cue; this.video = video;
        }
    }

    /** Egy testtáj: mire jó a sor, mikor NE ezt csináld, és a gyakorlatok. */
    public static final class Area {
        public final String id, emoji, name, goal, warn;
        public final Ex[] moves;
        Area(String id, String emoji, String name, String goal, String warn, Ex... moves) {
            this.id = id; this.emoji = emoji; this.name = name;
            this.goal = goal; this.warn = warn; this.moves = moves;
        }
    }

    private static Ex ex(String n, String d, String c, String v) { return new Ex(n, d, c, v); }

    /** Az általános piros zászló – területenként kiegészülhet. */
    static final String RED_FLAG = "Ez megelőzés, nem orvoslás: éles fájdalomnál, "
            + "zsibbadásnál, duzzanatnál vagy éjszakai fájdalomnál orvoshoz, "
            + "gyógytornászhoz fordulj.";

    public static final Area[] AREAS = {
            new Area("boka", "🦶", "Boka-stabilitás",
                    "Bokaficam után vagy megelőzésére: az egyensúly-érzék (propriocepció) "
                            + "és a boka körüli izmok erősítése. Heti 3–4 alkalom elég.",
                    RED_FLAG + " Friss ficam után az első napokban pihentetés és jég a dolgod, nem ez.",
                    ex("Egylábas állás", "3×30 mp / láb", "Mezítláb, térd lazán. Ha könnyű: csukott szemmel.", "egylábas állás egyensúly gyakorlat"),
                    ex("Egylábas állás párnán", "3×30 mp / láb", "Instabil felületen (párna, összehajtott törölköző) sokkal többet dolgozik a boka.", "single leg balance pillow gyakorlat"),
                    ex("Boka-ábécé", "2× / láb", "Ülve, a lábfejjel írd le a teljes ábécét – minden irányba mozgat.", "boka ábécé gyakorlat rehab"),
                    ex("Vádliemelés lépcsőn, lassú leengedés", "3×12", "Fent két lábbal, leengedés EGY lábon, 3-4 mp alatt – az excentrikus rész a lényeg.", "excentrikus vádliemelés lépcsőn"),
                    ex("Oldalirányú szökdelés", "3×10 oda-vissza", "Kis távolság, halk érkezés, tartott térd. Csak ha az előzőek már stabilak.", "oldalirányú szökdelés boka stabilitás"),
                    ex("Szalagos kifelé fordítás (everzió)", "3×15 / láb", "Gumiszalag a lábfejen, fordítsd kifelé a bokád, lassan vissza.", "boka everzió gumiszalag gyakorlat")),

            new Area("terd", "🦵", "Térd (elülső térdfájdalom ellen)",
                    "Futótérd és patellofemorális panaszok megelőzésére: a comb és a csípő "
                            + "erősítése – a térd sokszor azért fáj, mert a csípő gyenge.",
                    RED_FLAG + " Kattogás önmagában nem baj – a duzzanat és a beszorulás az.",
                    ex("Falhoz guggolás résztartományban", "3×10", "Hát a falon, csússz le fájdalommentes mélységig, 3 mp tartás.", "wall squat térd rehab"),
                    ex("Egyenes láb emelés", "3×12 / láb", "Hanyatt, egyik térd hajlítva. A nyújtott lábat emeld 45 fokig, lassan.", "egyenes láb emelés térd gyakorlat"),
                    ex("Kagyló (clamshell) gumival", "3×15 / oldal", "Oldalfekvés, térdek hajlítva, nyisd a felső térdet – a csípő oldala dolgozzon.", "clamshell gyakorlat gumiszalag"),
                    ex("Lelépés (step-down) lassan", "3×8 / láb", "Alacsony lépcsőről engedd le a másik sarkat 3 mp alatt. A térd a lábfej fölött marad.", "step down térd gyakorlat"),
                    ex("Csípőemelés (híd)", "3×12", "Fent szorítsd meg a farizmot egy pillanatra – a hátsó lánc tehermentesíti a térdet.", "glute bridge csípőemelés"),
                    ex("Combnyújtás állva", "2×30 mp / láb", "Bokát a fenékhez, térdek egymás mellett – edzés után, nem előtte.", "combizom nyújtás álló")),

            new Area("itszalag", "🏃", "Térd külső oldala (IT-szalag)",
                    "Futótérd külső oldali fajtája (IT-szalag szindróma): a fájdalom "
                            + "a térd külsején jelentkezik, jellemzően lejtmenetben és "
                            + "hosszabb futás közben. Az ok szinte mindig a csípő "
                            + "távolítóinak gyengesége – ott is kell erősíteni.",
                    RED_FLAG + " Magát a szalagot nem érdemes hengerezni: feszes húr, "
                            + "nem izom – a TFL-t és a farizmot lazítsd helyette.",
                    ex("Oldalfekvő lábemelés", "3×15 / oldal", "Test egy vonalban, a felső láb kissé HÁTRA, lábujj enyhén lefelé – így a középső farizom dolgozik.", "oldalfekvő lábemelés farizom gyakorlat"),
                    ex("Kagyló (clamshell) gumival", "3×15 / oldal", "Sarkak együtt, a felső térd nyílik – a csípő ne dőljön hátra.", "clamshell gyakorlat gumiszalag"),
                    ex("Oldalplank csípőemeléssel", "3×10 / oldal", "Könyökön, a csípőt engedd le és emeld vissza – lassan.", "oldalplank csípőemelés gyakorlat"),
                    ex("Oldalirányú szalagos járás", "3×10 lépés oda-vissza", "Gumiszalag a boka fölött, térd enyhén hajlítva, kis lépések.", "monster walk gumiszalag gyakorlat"),
                    ex("TFL és farizom hengerezés", "2×60 mp / oldal", "A csípő elülső-oldalsó pontja és a far, NEM maga a szalag.", "tfl foam roll gyakorlat"),
                    ex("Egylábas híd", "3×10 / láb", "A csípő ne billenjen oldalra – ez a lényeg, nem a magasság.", "single leg bridge gyakorlat")),

            new Area("derek", "🧱", "Derék (nem specifikus derékfájás ellen)",
                    "A törzs mély stabilizálói és a csípő mozgékonysága: a klasszikus "
                            + "„nagy hármas\" (madár-kutya, oldalplank, curl-up) köré épül.",
                    RED_FLAG + " Lábba sugárzó fájdalomnál vagy zsibbadásnál ne tornázz – orvos.",
                    ex("Madár-kutya", "3×8 / oldal", "Négykézláb, ellentétes kar-láb nyújtás. A derék NE billegjen – ez a lényege.", "bird dog gyakorlat helyes technika"),
                    ex("Oldalplank", "3×20 mp / oldal", "Könyökön, a test egy vonal. Ha sok, térdről indítsd.", "oldalplank helyes technika"),
                    ex("Curl-up (McGill)", "3×8", "Egyik térd hajlítva, kezek a derék alatt, csak a lapockáig emelkedj.", "mcgill curl up gyakorlat"),
                    ex("Csípőemelés (híd)", "3×12", "A mozgás a csípőből jöjjön, ne a derék homorításából.", "glute bridge csípőemelés"),
                    ex("Macska-teve", "2×10", "Lassú, ütemes gerincmozgás – bemelegítésnek a sor elejére is jó.", "macska teve gyakorlat"),
                    ex("Csípőhajlító nyújtás", "2×30 mp / oldal", "Fél térden, told előre a csípőd. Az ülőmunka legjobb ellenszere.", "csípőhajlító nyújtás térdelő")),

            new Area("vall", "💪", "Váll (impingement megelőzés)",
                    "A forgatóköpeny (rotátorköpeny) és a lapocka-stabilizálók erősítése – "
                            + "a legtöbb vállfájdalom ezek gyengeségéből indul.",
                    RED_FLAG + " Ha egy mozdulat éles fájdalmat ad, azt hagyd ki – ne „edzd át\".",
                    ex("Külső rotáció gumival", "3×15 / kar", "Könyök a törzs mellett 90 fokban, forgasd kifelé az alkart. Kis ellenállás, tiszta mozgás.", "váll külső rotáció gumiszalag"),
                    ex("Arcra húzás (face pull) gumival", "3×15", "Szemmagasságból húzd az arcodhoz, könyökök magasan, lapockák össze.", "face pull gumiszalag helyes technika"),
                    ex("Lapocka-fekvőtámasz", "3×12", "Fekvőtámaszban csak a lapockát süllyeszd-emeld, a könyök végig nyújtva.", "scapular push up gyakorlat"),
                    ex("Fal-csúsztatás (wall slide)", "3×10", "Alkarok a falon, csúsztasd felfelé, a lapocka kísérje a mozgást.", "wall slide váll gyakorlat"),
                    ex("Függés rúdon", "3×20 mp", "Passzív lógás – dekompresszió a vállnak. Fokozatosan növeld az időt.", "dead hang váll gyakorlat"),
                    ex("Y-T-W emelés", "2×8 mindhárom", "Hason vagy döntött padon, hüvelykujj felfelé, kis súllyal vagy anélkül.", "ytw gyakorlat váll rehab")),

            new Area("konyok-belso", "🥎", "Belső könyök (golfkönyök ellen)",
                    "A belső könyök-ín (medialis epicondylus) túlterhelése ellen: a "
                            + "csuklóhajlítók excentrikus erősítése és fokozatos terhelés. "
                            + "Naponta vagy kétnaponta, KIS súllyal.",
                    RED_FLAG + " A cél a húzó terhelés fokozatos emelése – ha egy hét után is "
                            + "fájdalmasabb, csökkents, és kérj gyógytornász-segítséget.",
                    ex("Excentrikus csuklóhajlítás", "3×12 / kar", "Alkar a combon, tenyér felfelé, kézisúlyzóval. Felfelé segíts a másik kézzel, leengedés 3-4 mp, egy kézzel.", "excentrikus csuklóhajlítás golfkönyök"),
                    ex("Pronáció-szupináció", "3×10 / kar", "Kalapácsot vagy súlyzót fogva forgasd a tenyered fel-le, lassan.", "pronáció szupináció alkar gyakorlat"),
                    ex("Szorítás (marokerősítő)", "3×10 tartás 5 mp", "Puha labda vagy marokerősítő – mérsékelt erővel, ne görcsösen.", "marokerősítés gyakorlat"),
                    ex("Csuklóhajlító nyújtás", "2×30 mp / kar", "Nyújtott könyök, tenyér felfelé, a másik kézzel húzd az ujjakat lefelé.", "csuklóhajlító nyújtás alkar"),
                    ex("Izometrikus csuklóhajlítás", "5×30 mp", "Tartsd a súlyt mozdulatlanul középhelyzetben – fájdalomcsillapító hatású terhelés.", "izometrikus csukló gyakorlat könyök")),

            new Area("konyok-kulso", "🎾", "Külső könyök (teniszkönyök ellen)",
                    "A külső könyök-ín túlterhelése ellen: ugyanaz a logika, mint a belsőnél, "
                            + "csak a feszítő oldalon – excentrikus munka, kis súly, türelem.",
                    RED_FLAG + " Az egér-kéz és a sok gépelés is okozhatja – a gyakorlat mellett "
                            + "a terhelés forrásán is érdemes állítani.",
                    ex("Excentrikus csuklófeszítés", "3×12 / kar", "Alkar a combon, tenyér LEFELÉ. Felfelé segíts a másik kézzel, leengedés lassan.", "excentrikus csuklófeszítés teniszkönyök"),
                    ex("Gumirúd-csavarás (Tyler twist)", "3×15", "Flexbar vagy feltekert törölköző: csavard meg, és a fájós kézzel engedd vissza lassan.", "tyler twist teniszkönyök gyakorlat"),
                    ex("Csuklófeszítő nyújtás", "2×30 mp / kar", "Nyújtott könyök, tenyér lefelé, húzd a kézfejed a test felé.", "csuklófeszítő nyújtás alkar"),
                    ex("Ujj-nyitás gumival", "3×15", "Gumigyűrű az ujjak körül, nyisd szét – a feszítők másik fele.", "ujj extenzió gumi gyakorlat")),

            new Area("csuklo", "🤲", "Csukló és kéz",
                    "Egérkéz, gépelés, fekvőtámasz, mászás: a csukló apró "
                            + "ízület, ami sok terhet visz. A mozgástartomány "
                            + "megtartása és a terhelés fokozatos emelése a megelőzés – "
                            + "naponta pár perc elég.",
                    RED_FLAG + " Éjszakai zsibbadás, ujjakba sugárzó tünet esetén "
                            + "orvoshoz fordulj: az nem terhelés-kérdés.",
                    ex("Csuklókörzés", "2×10 / irány", "Lassan, teljes körrel – ez a bemelegítés, nem az edzés.", "csuklókörzés bemelegítés gyakorlat"),
                    ex("Imádkozó nyújtás", "3×20 mp", "Tenyerek össze mellkas előtt, engedd le a kezed, amíg húz.", "prayer stretch csukló nyújtás"),
                    ex("Fordított imádkozó nyújtás", "3×20 mp", "Kézfejek össze, ujjak lefelé – a másik oldal nyúlik.", "reverse prayer stretch csukló"),
                    ex("Csuklóhajlítás kis súllyal", "3×15", "1–2 kg, alkar a combon, tenyér felfelé, lassú leengedés.", "csuklóhajlítás kézisúlyzó gyakorlat"),
                    ex("Csuklófeszítés kis súllyal", "3×15", "Ugyanaz tenyérrel lefelé – a gyengébb oldal, ezért fontosabb.", "csuklófeszítés kézisúlyzó gyakorlat"),
                    ex("Ujj-nyitás gumival", "3×15", "Gumigyűrű az ujjak körül – a szorítás ellenpárja.", "ujj extenzió gumi gyakorlat"),
                    ex("Tenyértámasz terhelés", "3×20 mp", "Négykézláb, ujjak előre, majd kifelé fordítva – óvatosan told a súlyt a kézre.", "csukló terhelés négykézláb gyakorlat")),

            new Area("nyak", "🙆", "Nyak és felső hát",
                    "Az előreesett fej és a monitor-nyak ellen: mély nyakhajlítók, "
                            + "lapocka-stabilizálók és mellkasnyitás.",
                    RED_FLAG + " Karba sugárzó zsibbadásnál ne tornázz – orvos.",
                    ex("Állcsúszás (chin tuck)", "3×10", "Húzd hátra az állad vízszintesen (dupla toka), 3 mp tartás.", "chin tuck gyakorlat nyak"),
                    ex("Fal-angyal", "3×8", "Hát és alkarok a falon, csúsztasd a karod fel-le, a derék ne homorítson túl.", "wall angel gyakorlat"),
                    ex("Mellkasnyitás ajtófélfánál", "2×30 mp", "Alkar az ajtófélfán, lépj át kicsit – a mell elülső része nyúlik.", "mellizom nyújtás ajtófélfa"),
                    ex("Csuklyás nyújtás", "2×30 mp / oldal", "Ülve fogd meg a szék alját, döntsd a fejed az ellenkező vállhoz.", "csuklyásizom nyújtás"),
                    ex("Hátsó váll erősítés (T-emelés)", "3×12", "Döntött törzzsel, hüvelykujj felfelé, karok T-be – kis súly vagy semmi.", "reverse fly hátsó váll gyakorlat")),

            new Area("csipo", "🕺", "Csípő mozgékonyság és erő",
                    "Ülőmunka és guggolás-mélység: a csípő körüli forgatók, távolítók "
                            + "erősítése és a mozgástartomány visszaszerzése.",
                    RED_FLAG,
                    ex("Kagyló (clamshell)", "3×15 / oldal", "Oldalfekvés, sarkak együtt, nyisd a térdet – a farizom oldala égjen.", "clamshell gyakorlat gumiszalag"),
                    ex("Tűzcsap (fire hydrant)", "3×12 / oldal", "Négykézláb emeld a hajlított térded oldalra, a derék ne csavarodjon.", "fire hydrant gyakorlat"),
                    ex("90/90 csípőforgatás", "2×8 / irány", "Ülve mindkét térd 90 fokban – dőlj és fordulj át a másik oldalra.", "90 90 csípő mobilitás gyakorlat"),
                    ex("Csípőhajlító nyújtás", "2×30 mp / oldal", "Fél térden, farizom feszítve told előre a csípőd.", "csípőhajlító nyújtás térdelő"),
                    ex("Mély guggolás tartás", "3×20 mp", "Kapaszkodva ereszkedj mély guggolásba, sarok a földön.", "mély guggolás tartás mobilitás")),

            new Area("comb", "🍗", "Combhajlító (meghúzódás ellen)",
                    "A hátsó comb meghúzódása a sprintelők és focisták klasszikusa – a "
                            + "megelőzés kulcsa az excentrikus erő (nordic curl) és a "
                            + "fokozatos sprint-terhelés.",
                    RED_FLAG + " Friss húzódásra ne erősíts: az első napokban kímélet, utána "
                            + "fokozatosan – ha nem javul, gyógytornász.",
                    ex("Nordic curl (rész-tartomány)", "3×5", "Térdelve, bokát rögzítve dőlj előre lassan, ameddig tartani tudod – kézzel told vissza magad.", "nordic hamstring curl gyakorlat"),
                    ex("Csúszó sarok-kihúzás (slider)", "3×8 / láb", "Hanyatt, sarok törölközőn: hídból csúsztasd ki lassan a lábad.", "hamstring slider gyakorlat"),
                    ex("Egylábas híd", "3×10 / láb", "Sarok a földön, told fel a csípőd – a comb hátulja dolgozzon, ne a derék.", "single leg bridge gyakorlat"),
                    ex("Jó reggelt (good morning) könnyű súllyal", "3×10", "Csípőből dőlj, egyenes háttal – a comb hátulja adja a jelet, hol állj meg.", "good morning gyakorlat könnyű súly"),
                    ex("Combhajlító nyújtás", "2×30 mp / láb", "Sarok előre, hajolj a csípőből – edzés után.", "combhajlító nyújtás álló")),

            new Area("talp", "👣", "Talp (talpfájás ellen)",
                    "Talpfájás (plantar fasciitis) megelőzésére: a talpi ín "
                            + "tehermentesítése, a lábboltozat erősítése és a vádli-lánc "
                            + "nyújtása – a reggeli első lépések fájdalmának klasszikus ellenszere.",
                    RED_FLAG + " Makacs, hetek óta tartó talpfájásnál gyógytornász kell, "
                            + "nem több gyakorlat.",
                    ex("Talp-görgetés labdán", "2×60 mp / talp", "Teniszlabda vagy görgő a talp alatt, lassú, nyomott körökkel.", "plantar fascia labda görgetés"),
                    ex("Törölköző-húzás lábujjakkal", "3×10 / láb", "Ülve, a leterített törölközőt a lábujjaiddal gyűrd magad felé.", "towel curl láb gyakorlat"),
                    ex("Talpi fascia nyújtás", "2×30 mp / láb", "Kézzel feszítsd hátra a lábujjaid, a talp íve nyúljon.", "plantar fascia nyújtás gyakorlat"),
                    ex("Vádlinyújtás falnál", "2×30 mp / láb", "Hátsó láb nyújtva, sarok végig a földön – a feszes vádli húzza a talpat.", "vádlinyújtás falnál"),
                    ex("Emelt lábujjas vádliemelés", "3×12", "Feltekert törölköző a lábujjak alatt, úgy emelkedj lábujjhegyre – a talpi ín is dolgozik.", "heel raise emelt lábujj plantar gyakorlat")),

            new Area("sipcsont", "🦴", "Sípcsont (futó-sípcsont ellen)",
                    "Sípcsonti fájdalom (shin splint) megelőzésére futóknak: az elülső "
                            + "sípcsonti izom és a lábboltozat erősítése, nyújtott vádli – és "
                            + "fokozatosan emelt futóterhelés.",
                    RED_FLAG + " Pontszerű, csontos fájdalomnál állj le a futással – az "
                            + "fáradásos törés is lehet, azt orvos lássa.",
                    ex("Lábfej-emelés (sípcsonti izom)", "3×15", "Sarkon állva emeld a lábfejed ütemesen – elöl, a sípcsont mellett égjen.", "tibialis anterior lábfej emelés gyakorlat"),
                    ex("Sarkon járás", "3×30 mp", "Járj a sarkadon, lábujjak fent – ugyanaz az izom, más szögből.", "sarkon járás gyakorlat"),
                    ex("Vádlinyújtás falnál", "2×30 mp / láb", "Hátsó láb nyújtva, sarok a földön.", "vádlinyújtás falnál"),
                    ex("Egylábas vádliemelés", "3×10 / láb", "Teljes mozgástartomány, lassú leengedés.", "egylábas vádliemelés gyakorlat"),
                    ex("Talpboltozat-erősítés (short foot)", "3×10 / láb", "Húzd össze a talpad ívét a lábujjak begörbítése nélkül – kicsi, pontos mozgás.", "short foot gyakorlat talpboltozat")),

            new Area("achilles", "🩹", "Achilles és vádli",
                    "Achilles-panaszok megelőzése futóknak: a klasszikus excentrikus "
                            + "sarok-leengedés (Alfredson-protokoll szelleme) – lassan, sokat.",
                    RED_FLAG + " Reggeli indulási merevség jelezhet ín-túlterhelést: ilyenkor "
                            + "csökkentett futás mellett csináld, ne helyette.",
                    ex("Sarok-leengedés nyújtott térddel", "3×12 / láb", "Lépcső szélén, emelkedés két lábbal, leengedés eggyel, 3-4 mp alatt.", "excentrikus sarok leengedés achilles"),
                    ex("Sarok-leengedés hajlított térddel", "3×12 / láb", "Ugyanez enyhén hajlított térddel – így a mélyebb vádliizom (soleus) dolgozik.", "soleus vádli gyakorlat hajlított térd"),
                    ex("Vádlinyújtás falnál", "2×30 mp / láb", "Hátsó láb nyújtva, sarok végig a földön.", "vádlinyújtás falnál"),
                    ex("Egylábas vádliemelés", "3×10 / láb", "Teljes mozgástartomány, fent egy pillanat tartás.", "egylábas vádliemelés gyakorlat")),
    };

    /**
     * Panaszból testtáj: „fáj a vállam" → a váll-sor.
     *
     * Az app mondat-elvű: ha a felhasználó bármelyik mezőbe beírja, hogy mi
     * fáj, a legjobb válasz nem a „nem értem", hanem a megfelelő megelőző
     * sor felajánlása. Kimondott fájdalom-szó kell hozzá („fáj",
     * „fájdalom", „húzódik") ÉS egy testtájnév – e nélkül a „vállból
     * nyomás" is panasznak látszana.
     */
    public static Area forComplaint(String q) {
        if (q == null) return null;
        String s = Foods.norm(q);
        boolean pain = false;
        for (String w : new String[]{"faj", "fajdalom", "fajdalmas", "huzodik", "huzodas",
                "serules", "megserult", "kificamodott", "ficam", "gyullad",
                // A magyar bőven tud panaszt mondani fájdalom-szó nélkül is.
                "beallt", "megrandult", "randult", "nyilallik", "nyilall", "sajog",
                "meghuztam", "becsipodott", "belovellt", "lumbago", "merev"}) {
            int i = s.indexOf(w);
            while (i >= 0) {
                boolean l = i == 0 || !Character.isLetter(s.charAt(i - 1));
                if (l) { pain = true; break; }
                i = s.indexOf(w, i + 1);
            }
            if (pain) break;
        }
        if (!pain) return null;
        // A tagadott vagy elmúlt panasz jó hír, nem kérés: a „nem fáj a
        // vállam" és a „már nem fáj" után nincs mit ajánlani.
        for (String neg : new String[]{"nem faj", "mar nem", "elmult", "meggyogyult",
                "nem fajt"})
            if (s.contains(neg)) return null;
        // A zsibbadás piros zászló, nem torna-ügy: arra nem sort ajánlunk,
        // hanem hallgatunk – a figyelmeztetés a lapokon úgyis ott van, de
        // ide el sem visszük.
        if (s.contains("zsibbad")) return null;
        return areaOf(s);
    }

    /**
     * Cél-mondatból testtáj: „boka stabilitás", „váll mobilizálás".
     *
     * A rehab másik ajtaja: nemcsak az jön ide, akinek fáj valamije, hanem
     * az is, aki megelőzni akar – erre való az egész. Kimondott szándék-szó
     * kell (stabilitás, mobilizálás, rehab, gyógytorna, megelőzés) ÉS egy
     * testtájnév. Az „erősítés" szándékosan hiányzik: a „váll erősítés" a
     * konditerem szava, arra a súlyzós oldal a jó válasz.
     */
    public static Area forGoal(String q) {
        if (q == null) return null;
        String s = Foods.norm(q);
        boolean want = false;
        for (String w : new String[]{"stabilit", "stabiliz", "mobiliz", "mobilit",
                "rehab", "gyogytorna", "megeloz", "prevenc"})
            if (s.contains(w)) { want = true; break; }
        if (!want) return null;
        Area a = areaOf(s);
        // A puszta „comb" a panasz-oldalon szándékosan nem tő (csirkecomb!),
        // de cél-mondatban („comb rehab") a szó eleji találat félreérthetetlen.
        if (a == null) {
            int i = s.indexOf("comb");
            if (i >= 0 && (i == 0 || !Character.isLetter(s.charAt(i - 1))))
                a = byId("comb");
        }
        return a;
    }

    /** A normalizált mondatban megnevezett testtáj sora, vagy null. */
    private static Area areaOf(String s) {
        // A „csípős" étel és a „vállal" ige nem testtáj – kitakarjuk, mielőtt
        // a rövid tövek („csipo", „vall") beleakadnának. A „fáj a hasam a
        // csípős kajától" panasz, de nem csípő-ügy.
        s = s.replace("csipos", "#").replace("vallal", "#");
        String[][] map = {
                {"boka", "bokam", "bokaja", "boka"},
                // A térd KÜLSŐ oldala más panasz, mint az elülső – az
                // IT-szalag sora a csípő távolítóit erősíti. A hosszabb,
                // pontosabb megnevezés ezért előbb áll a puszta „térd"-nél.
                {"itszalag", "it szalag", "itszalag", "iliotibialis", "kulso terd",
                        "terd kulso", "terdem kulso", "futoterd"},
                {"terd", "terdem", "terde", "terd"},
                {"derek", "derekam", "dereka", "derek", "hatam faj", "also hat",
                        "gerincem", "gerinc"},
                {"vall", "vallam", "valla", "vall"},
                {"konyok-belso", "konyokom belso", "belso konyok", "golfkonyok"},
                {"konyok-kulso", "kulso konyok", "teniszkonyok"},
                // A puszta „könyök" a gyakoribb külsőre megy – a lap tetejéről
                // egy koppintás a belső.
                {"konyok-kulso", "konyokom", "konyoke", "konyok"},
                // A csuklónak saját sora van: a mozgástartomány és a kis
                // súlyos terhelés más, mint az alkar-feszítők könyök-oldali
                // munkája. (Korábban a külső könyök sorára ment – az is
                // segített, de a csukló-mobilitás onnan hiányzott.)
                {"csuklo", "csuklom", "csukloja", "csuklo faj", "csuklofajas", "egerkez"},
                {"nyak", "nyakam", "nyaka", "nyak", "tarkom"},
                {"csipo", "csipom", "csipoje", "csipo"},
                {"achilles", "achilles", "vadlim", "sarkam", "sarok faj"},
                {"talp", "talpam", "talpa", "talp"},
                {"sipcsont", "sipcsontom", "sipcsontja", "sipcsont"},
                {"comb", "combom", "combhajlito", "hatso comb", "comb hatulja"},
        };
        for (String[] m : map)
            for (int i = 1; i < m.length; i++)
                if (s.contains(m[i])) return byId(m[0]);
        return null;
    }

    /** Terület azonosító alapján, vagy null. */
    public static Area byId(String id) {
        for (Area a : AREAS) if (a.id.equals(id)) return a;
        return null;
    }

    /** A sor becsült hossza percben – a naplóba ezzel kerül. */
    public static int minutesOf(Area a) {
        // Gyakorlatonként nagyjából két perc a sorozatokkal és pihenőkkel.
        return Math.max(10, Math.min(20, a.moves.length * 2));
    }

    // ---------- Vezetett sor ----------

    /**
     * A vezetett sor nevei egy körre: a kétoldalas gyakorlat bal/jobb
     * bontásban.
     *
     * Az adagolásban a „/ láb", „/ oldal", „/ kar" azt jelenti, hogy a
     * gyakorlatot mindkét oldalra el kell végezni – a vezetett módban ez
     * eddig egyetlen 40 mp-es ablakba volt gyömöszölve. Külön ablakot kap
     * a két oldal, a bemondás pedig ki is mondja, melyik jön.
     */
    public static java.util.List<String> guidedNames(Area a) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (Ex e : a.moves) {
            boolean sided = e.dose.contains("/ láb") || e.dose.contains("/ oldal")
                    || e.dose.contains("/ kar") || e.dose.contains("/ irány")
                    || e.dose.contains("/ talp");
            if (sided) {
                out.add(e.name + " – bal");
                out.add(e.name + " – jobb");
            } else {
                out.add(e.name);
            }
        }
        return out;
    }

    /** Ennyi kör fér a 10–20 perces keretbe: bontott sornál kettő. */
    public static int guidedRounds(Area a) {
        return guidedNames(a).size() > a.moves.length ? 2 : 3;
    }

    // ---------- Fájdalom-napló ----------

    /**
     * A 0–10-es skála szavakban.
     *
     * A szám önmagában nem mond semmit annak, aki most írja be először:
     * a „4" akkor lesz értelmes, ha oda van írva, hogy az közepes.
     */
    public static String painWord(int level) {
        if (level <= 0) return "nincs fájdalom";
        if (level <= 3) return "enyhe";
        if (level <= 6) return "közepes";
        return "erős";
    }

    /**
     * Merre tart a panasz? – a legfrissebb és a legrégebbi napok átlaga.
     *
     * A tömb legfrissebb elöl. Négy bejegyzés alatt nem mondunk irányt: két
     * adatból trendet olvasni önbecsapás, a rossz nap pedig mindenkinél van.
     * Az erős (8 fölötti) friss érték felülír mindent – ott nem a görbe a
     * hír, hanem az, hogy szakember kell.
     */
    public static String painLine(int[] newestFirst) {
        if (newestFirst == null || newestFirst.length == 0) return "";
        int last = newestFirst[0];
        if (last >= 8)
            return "Erős fájdalom – ezt nézesd meg orvossal vagy gyógytornásszal.";
        if (newestFirst.length < 4)
            return "Legutóbb: " + last + "/10 (" + painWord(last)
                    + ") – pár nap után látszik majd az irány.";
        int win = Math.min(3, newestFirst.length / 2);
        double now = 0, then = 0;
        for (int i = 0; i < win; i++) now += newestFirst[i];
        for (int i = 0; i < win; i++) then += newestFirst[newestFirst.length - 1 - i];
        now /= win;
        then /= win;
        double diff = then - now;
        String head = "Legutóbb: " + last + "/10 (" + painWord(last) + ") · ";
        if (diff >= 1.5) return head + "javul – " + Hu.kg(then) + "-ról " + Hu.kg(now) + "-ra";
        if (diff <= -1.5)
            return head + "rosszabbodik – ha egy hét után sem fordul, kérj gyógytornász-segítséget";
        return head + "nem sokat mozdult – tartsd a sort, és nézd meg a terhelést is";
    }

    /**
     * A mondatban kimondott fájdalom-érték („fáj a vállam 6/10"), vagy -1.
     *
     * Aki egyszer megszokta a skálát, az le is írja – kár lenne még egyszer
     * megkérdezni tőle. A tíz per tíz alak a beszédes, mert magát a skálát
     * is kimondja; a „7-es fájdalom" ugyanaz más szórenddel.
     */
    public static int painIn(String q) {
        if (q == null) return -1;
        String s = Hu.digits(Foods.norm(q));
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s?/\\s?10(?![0-9])").matcher(s);
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v >= 0 && v <= 10) return v;
        }
        m = java.util.regex.Pattern
                .compile("fajdalom\\w*\\s?:?\\s*(\\d{1,2})(?![0-9])"
                        + "|(\\d{1,2})[- ]?[oe]s\\s?fajdalom").matcher(s);
        if (m.find()) {
            String g = m.group(1) != null ? m.group(1) : m.group(2);
            int v = Integer.parseInt(g);
            if (v >= 0 && v <= 10) return v;
        }
        return -1;
    }

    // ---------- Heti fókusz ----------

    /**
     * Ennyi alkalom egy hét megelőző adagja.
     *
     * A területek leírásai heti 3–4 alkalmat mondanak; a fókusz-számláló a
     * hármat veszi célnak – ami fölötte van, az már ráadás, nem tartozás.
     */
    public static final int WEEKLY_GOAL = 3;

    /**
     * Hány időbélyeg esik a mostani naptári hétre (hétfő 0:00-tól máig)?
     *
     * A jövőbeli bélyeg nem számít – elrontott óra vagy kézi dátum ne
     * írjon jóvá előre alkalmakat.
     */
    public static int weekCount(long[] ts, long now) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        // Calendar-ban a vasárnap az 1 – a magyar hét hétfőn kezdődik.
        int back = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(java.util.Calendar.DAY_OF_MONTH, -back);
        long monday = cal.getTimeInMillis();
        int n = 0;
        if (ts != null) for (long t : ts) if (t >= monday && t <= now) n++;
        return n;
    }

    /**
     * Melyik sor jön ma? – döntés helyett ajánlás.
     *
     * Aki fájdalommal vagy fáradtan nyitja meg a lapot, annak a tizenhárom
     * terület listája nem segítség, hanem újabb döntés. A sorrend: a heti
     * fókusz, amíg nincs meg a heti adag; utána az, amit a legrégebben
     * csináltál (mert az a leginkább esedékes).
     *
     * @param ids      a területek azonosítói
     * @param lastDone területenként a legutóbbi alkalom ideje (0 = soha)
     * @return a javasolt terület azonosítója, vagy null, ha nincs mit ajánlani
     */
    public static String nextArea(String focusId, int focusDone, String[] ids, long[] lastDone) {
        if (focusId != null && byId(focusId) != null && focusDone < WEEKLY_GOAL) return focusId;
        if (ids == null || lastDone == null) return null;
        String best = null;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < ids.length && i < lastDone.length; i++) {
            if (lastDone[i] <= 0) continue;
            if (lastDone[i] < oldest) { oldest = lastDone[i]; best = ids[i]; }
        }
        return best;
    }

    /** A fókusz-kártya sora: hol tartasz a héten. */
    public static String focusLine(Area a, int done) {
        if (done >= WEEKLY_GOAL)
            return a.name + " – e heti " + WEEKLY_GOAL + " alkalom megvan ✔";
        return a.name + " – a héten " + done + "/" + WEEKLY_GOAL + " alkalom";
    }

    /**
     * Hány egymást követő héten jött össze a heti adag?
     *
     * A mostani hét csak akkor számít bele, ha már megvan – amíg tart, a
     * sorozatot az előző hetek adják. A hét itt fix hétnapos ablak a hétfő
     * 0:00-tól visszafelé; az óraátállítás egy-egy óra csúszást okozhat a
     * határon, ami egy heti darabszámnál nem oszt, nem szoroz.
     */
    public static int weekStreak(long[] ts, long now) {
        if (ts == null || ts.length == 0) return 0;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        cal.add(java.util.Calendar.DAY_OF_MONTH,
                -((cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7));
        long monday = cal.getTimeInMillis();
        long week = 7L * 24 * 3600 * 1000;
        int streak = 0;
        if (countIn(ts, monday, now + 1) >= WEEKLY_GOAL) streak++;
        long s = monday - week;
        while (streak < 520 && countIn(ts, s, s + week) >= WEEKLY_GOAL) {
            streak++;
            s -= week;
        }
        return streak;
    }

    private static int countIn(long[] ts, long from, long to) {
        int n = 0;
        for (long t : ts) if (t >= from && t < to) n++;
        return n;
    }
}
