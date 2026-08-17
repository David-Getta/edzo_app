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

            new Area("hati", "🪑", "Háti gerinc (ülőmunka ellen)",
                    "A háti (mellkasi) gerinc mozgékonysága: a napi nyolc óra ülés itt "
                            + "merevíti be a hátat, és a merev háti szakasz árát a NYAK és a "
                            + "VÁLL fizeti meg – oda vándorol a mozgás, ami innen hiányzik. "
                            + "Rövid sor, napi adagra szánva.",
                    RED_FLAG + " Ha a fájdalom a derékba sugárzik vagy zsibbadás kíséri, "
                            + "az nem merevség – azt nézesd meg.",
                    ex("Nyitott könyv (open book)", "3×8 / oldal", "Oldalfekvés, térdek hajlítva, a felső kart nyisd hátra – a fejed kísérje, a térdek maradjanak együtt.", "open book gyakorlat háti gerinc"),
                    ex("Tűbefűzés (thread the needle)", "3×8 / oldal", "Négykézláb, a kart fűzd át a másik alatt, majd nyisd tágra felfelé – a mozgás a lapockák közül jöjjön.", "thread the needle gyakorlat"),
                    ex("Háti extenzió hengeren", "2×10", "Henger a lapockák alatt keresztben, kezek a tarkón, hajolj hátra – a derekat NE homorítsd, a mozgás fent történjen.", "thoracic extension foam roller gyakorlat"),
                    ex("Fal-angyal", "3×10", "Hát és alkarok a falon, csúsztasd a karokat fel-le úgy, hogy a derék végig a falhoz simuljon. Ennél nehezebb, mint amilyennek látszik.", "wall angel gyakorlat"),
                    ex("Evezés gumiszalaggal, lapocka-összehúzással", "3×15", "Húzd a könyököd a törzs mellé, és fent tartsd egy pillanatig – a lapockák dolgozzanak, ne a kar.", "gumiszalag evezés lapocka gyakorlat"),
                    ex("Mellizom-nyújtás ajtófélfánál", "2×30 mp / oldal", "Alkar a félfán könyökmagasságban, fordulj el lassan – a becsukódott mellizom a görbe hát másik fele.", "mellizom nyújtás ajtófélfa")),
            // A TÖRZS a derék, a csípő és a térd közös alapja: a legtöbb
            // panasz-sor a mély stabilizálókra hivatkozik, de saját lapja
            // eddig nem volt. A „gyenge a törzsizmom" és a „core erősítés"
            // ezért válasz nélkül maradt – vagy ami rosszabb, hatvanperces
            // kondi-edzésként került a naplóba.
            new Area("torzs", "🧍", "Törzs és medence (mélyizom-stabilitás)",
                    "A haránt hasizom és a medencefenék munkája: a törzs nem a látható "
                            + "hasizomtól stabil, hanem attól a mély rétegtől, ami a gerincet "
                            + "MOZGÁS KÖZBEN tartja meg. Ez a sor a derék, a csípő és a térd "
                            + "panaszainak a közös alapja – önmagában is, és a többi sor mellé is.",
                    RED_FLAG + " Ha a hasfal középen kidomborodik a gyakorlat alatt "
                            + "(rectus diastasis), hagyd abba, és kérj orvosi vagy "
                            + "gyógytornászi véleményt – az nem edzésmennyiség kérdése.",
                    ex("Haránt hasizom aktiválás", "3×10 lélegzet", "Hanyatt, térdek hajlítva. Kilégzésre húzd be finoman a köldököt – a bordáid NE emelkedjenek. Ennyi az egész, és ez a legnehezebb.", "haránt hasizom aktiválás gyakorlat"),
                    ex("Halott bogár (dead bug)", "3×8 / oldal", "Hanyatt, kar és láb a levegőben. Az ellentétes kar-lábat engedd le lassan – a derék végig a talajon marad.", "dead bug gyakorlat helyes technika"),
                    ex("Oldalplank térdről", "3×20 mp / oldal", "Könyökön, térdek hajlítva. A csípő ne essen le: a váll, a csípő és a térd egy vonal.", "oldalplank térdről gyakorlat"),
                    ex("Plank vállérintéssel", "3×10", "Plankben érintsd meg felváltva az ellenkező vállad – a csípő NE forduljon el. Ha billeg, tedd szélesebbre a lábad.", "plank shoulder tap gyakorlat"),
                    ex("Egykezes farmer-séta", "3×20 m / oldal", "Egy kézben súly, sétálj egyenesen, vállak vízszintben. A törzs OLDALIRÁNYÚ stabilitása ez – a legéletszerűbb core-gyakorlat.", "suitcase carry gyakorlat"),
                    ex("Medencefenék-légzés", "2×10 lélegzet", "Belégzésre engedd el, kilégzésre finoman emeld – a hasfal és a medencefenék egy rendszer, együtt dolgozik.", "medencefenék légzés gyakorlat")),
    };

    /**
     * A következő szint testtájanként: mi jön, ha a sor már könnyű.
     *
     * A fokozatosság a rehab lelke, de az „emelj az adagoláson" tanács
     * személytelen: a bokánál a KÖVETKEZŐ lépés nem több ismétlés, hanem az
     * instabil felület és az ugrás, a golfkönyöknél a nehezebb súly lassabb
     * leengedéssel. Minden területnek megvan a maga iránya – ezt mondjuk ki,
     * amikor a sor már hat alkalmat megélt.
     *
     * Sorok: [terület-azonosító, a következő lépés].
     */
    private static final String[][] NEXT = {
            {"boka", "Instabil felület és ütem: párna vagy összehajtott törölköző az "
                    + "egylábas álláshoz, csukott szemmel; a szökdelést told 3×15-re, és "
                    + "vidd oldalra-átlósan is. A halk érkezés a mérce, nem a távolság."},
            {"terd", "Mélyebb tartomány és egy láb: a falhoz guggolást engedd mélyebbre "
                    + "(fájdalommentesen), a lelépést magasabb lépcsőről, 3 mp helyett 5 mp "
                    + "alatt. A híd mehet egy lábon."},
            {"itszalag", "Terhelt oldal-lánc: az oldalfekvő lábemeléshez tegyél gumiszalagot "
                    + "a boka fölé, az oldalplankot told 3×20 mp-re, a szalagos járást "
                    + "mélyebb féltérdben. Futásnál a lejtmenet jöjjön vissza utoljára."},
            {"derek", "Tartás helyett terhelés: a madár-kutyához könyök-térd érintés "
                    + "(3×8 / oldal), az oldalplank lábbal a padra, a hídhoz egy láb. A cél "
                    + "a stabil derék mozgás közben – nem a hosszabb plank."},
            {"vall", "Nagyobb kar-emelés: a külső rotációt vidd 90 fokos elrabolt karral "
                    + "(kaszáló mozdulat), a fal-csúsztatást súllyal, a függést told "
                    + "3×40 mp-re. Az Y-T-W-hez elég 1–2 kg."},
            {"konyok-belso", "Nehezebb súly, lassabb leengedés: az excentrikus csuklóhajlításnál "
                    + "emelj fél kilót, és told a leengedést 5 mp-re. A húzó terhelés (evezés, "
                    + "húzódzkodás) fokozatosan jöhet vissza – hetente egy lépcsőt."},
            {"konyok-kulso", "Erősebb gumirúd és fogás-munka: a Tyler twistet vidd nehezebb "
                    + "rúdra, a csuklófeszítést fél kilóval, és tegyél mellé 3×10 "
                    + "marokerősítést. Az egér- és billentyűzet-magasságot is nézd meg."},
            {"csuklo", "Több súly a kézen: a tenyértámaszt vidd fekvőtámasz-helyzetbe "
                    + "(térdről is jó), a csuklóhajlítást-feszítést 2–3 kg-ra. Az imádkozó "
                    + "nyújtás maradjon meg mellette."},
            {"nyak", "Terhelés a mély hajlítóknak: az állcsúszást csináld hanyatt fekve, "
                    + "fejet kissé megemelve (3×10, 5 mp tartás), a T-emeléshez tegyél "
                    + "1–2 kg-ot. A fal-angyal maradjon a napi tétel."},
            {"csipo", "Terhelt mobilitás: a kagylót és a tűzcsapot gumiszalaggal, a 90/90-et "
                    + "kézzel nem segítve (aktív forgatás), a mély guggolás tartást "
                    + "kapaszkodás nélkül. Innen már a bolgár kitörés a következő."},
            {"comb", "Teljesebb nordic és sebesség: engedd a nordic curl-t mélyebbre "
                    + "(3×6–8), a slidert egy lábon, és a good morninghoz tegyél súlyt. "
                    + "Sprintelőnek a fokozatos gyorsítás a legfontosabb elem."},
            {"talp", "Terhelt lábboltozat: az emelt lábujjas vádliemelést vidd egy lábra "
                    + "(3×10), a short footot állva, majd egylábas állásban. A görgetés "
                    + "marad, de már inkább bemelegítésként."},
            {"sipcsont", "Ütés-tűrés fokozatosan: a sarkon járást told 3×60 mp-re, a "
                    + "lábfej-emeléshez tegyél gumiszalagot, és a futótávot hetente "
                    + "legfeljebb tíz százalékkal emeld – a sípcsont a mennyiségre érzékeny."},
            {"achilles", "Nehezebb excentrikus munka: a sarok-leengedéshez vegyél hátizsákot "
                    + "(5–10 kg), és tartsd a napi két sorozatot. Ugrás és sprint csak akkor, "
                    + "ha a reggeli merevség már elmúlt."},
            {"hati", "Terhelt nyitás: a nyitott könyvhöz vegyél kis súlyt a felső kézbe, "
                    + "a fal-angyalt csináld háttal a falnak ÁLLVA, sarokkal 10 cm-re, és "
                    + "told az evezést 3×15-ről gumiszalag-fokozattal feljebb. Napi egy "
                    + "rövid adag többet ér a heti nagynál."},
            {"torzs", "Mozgás közbeni terhelés: a halott bogárhoz vegyél 2–3 kg-os súlyt a "
                    + "kézbe, az oldalplankot vidd le lábról (3×30 mp), a farmer-sétát told "
                    + "3×40 m-re nehezebb súllyal. A haránt hasizom-aktiválás onnantól "
                    + "nem külön gyakorlat, hanem MINDEN gyakorlat része."},
    };

    /**
     * Mikorra várható javulás – testtájanként.
     *
     * A gyógytornász első mondata mindig ez, és pont ez hiányzott: aki két
     * nap után nem érez semmit, abbahagyja. Az ín-panaszok (golfkönyök,
     * Achilles, talp) hetekben mérhetők, az egyensúly-munka gyorsabban hoz
     * eredményt. A számok a konzervatív kezelés szokásos időtávjai – nem
     * ígéret, hanem türelem-mérték.
     *
     * Sorok: [terület-azonosító, a várható időtáv].
     */
    private static final String[][] EXPECT = {
            {"boka", "Az egyensúly-érzék gyorsan javul: 2–3 hét rendszeres munka után "
                    + "érezhetően stabilabb a boka, a teljes hatás 6–8 hét."},
            {"terd", "Az elülső térdfájdalom 4–6 hét alatt szokott érdemben javulni, "
                    + "ha a csípő-erősítés is megvan. Türelem kell hozzá."},
            {"itszalag", "Az IT-szalag panasz 4–8 hét: a futásmennyiséget közben "
                    + "csökkentsd, ne állítsd le teljesen."},
            {"derek", "A nem specifikus derékfájás java 2–6 hét alatt rendeződik. "
                    + "A mozgás segít, az ágy nem."},
            {"vall", "A váll 6–12 hét: a forgatóköpeny lassan épül, viszont tartósan. "
                    + "A napi kis adag többet ér a heti nagynál."},
            {"konyok-belso", "Az ín-panasz a leglassabb: 6–12 hét, néha több. Az első "
                    + "3–4 hétben a fájdalom nem csökken látványosan – ez normális, "
                    + "az ín ilyenkor épül."},
            {"konyok-kulso", "Ugyanaz, mint a belsőnél: 6–12 hét. A gumirúd-csavarás "
                    + "vizsgálatokban 4–6 hét alatt hozott mérhető javulást."},
            {"csuklo", "A mozgástartomány 2–4 hét alatt visszatér, az erő 6–8 hét. "
                    + "Az egérhasználat átállítása azonnal segít."},
            {"nyak", "A monitor-nyak 3–6 hét alatt enged, ha a napi állcsúszás megvan. "
                    + "A munkahely-beállítás nélkül visszatér."},
            {"csipo", "A mozgékonyság 2–4 hét alatt látványosan nő, az erő 6–8 hét. "
                    + "Ülőmunkánál a napi csípőhajlító-nyújtás tartja meg."},
            {"comb", "Meghúzódás után 3–6 hét a visszatérés, a megelőző nordic curl "
                    + "hatása 8–10 hét után mérhető. A sprintet fokozatosan told vissza."},
            {"talp", "A talpfájás 6–12 hét, és a reggeli első lépések javulnak először. "
                    + "A görgetés csillapít, az erősítés gyógyít."},
            {"sipcsont", "A sípcsont-panasz 2–6 hét, de csak csökkentett futómennyiség "
                    + "mellett. Pontszerű csontfájdalomnál orvos."},
            {"achilles", "Az Achilles 8–12 hét excentrikus munkával, és a napi két "
                    + "sorozat a kulcs. A merevség csökkenése az első jó jel."},
            {"hati", "A háti merevség gyorsan enged: 2–3 hét napi adag után látszik, "
                    + "a tartós változás 6–8 hét. Az íróasztal magassága nélkül visszatér."},
            {"torzs", "A mély stabilizálók 4–6 hét alatt kapcsolnak be igazán, és az első "
                    + "jel nem a hasizom, hanem az, hogy a derék kevésbé fárad el álló "
                    + "munkában. Napi rövid adag többet ér a heti nagynál."},
    };

    /** A várható javulás szövege, vagy üres, ha nincs ilyen terület. */
    public static String expected(String areaId) {
        for (String[] r : EXPECT) if (r[0].equals(areaId)) return r[1];
        return "";
    }

    /** A következő szint szövege, vagy üres, ha nincs ilyen terület. */
    public static String nextLevel(String areaId) {
        for (String[] r : NEXT) if (r[0].equals(areaId)) return r[1];
        return "";
    }

    /**
     * A „faj" szótő hétköznapi álruhái: fajta, fájl, faji, fajok.
     *
     * Ötvenezer magyar szót a testtájak mellé téve ezek maradtak: a „fájl"
     * és a „fajta" ugyanúgy a szó elején hordozza a „faj"-t, mint a „fáj".
     * Egy „milyen fajta nyújtás a vállamra" mondatból így panasz lett.
     */
    private static boolean falsePain(String s, int i) {
        for (String w : new String[]{"fajta", "fajl", "faji", "fajok", "fajank",
                "fajzat", "fajul", "fajsuly", "fajlagos", "fajkent"})
            if (s.startsWith(w, i)) return true;
        return false;
    }

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
                // A hang is panasz: a „ropog a térdem", a „recseg a vállam" és
                // a „kattog a csípőm" ugyanúgy ide hoz, mint a fájdalom – a
                // térd lapja épp azt mondja ki, hogy a kattogás önmagában nem
                // baj. A „húzódott" a „húzódik" múlt ideje.
                // A „pattant" a szakadás magyar szava: az „elpattant valami a
                // vádlimban" mondatra eddig semmi nem jött.
                "elpattant", "megpattant", "elszakadt", "beszakadt",
                // A „kiugrott a derekam" a magyar hexensussz, a „begyulladt
                // az Achillesem" igekötős gyulladás – egyik sem talált eddig.
                "kiugrott", "begyullad",
                // Az INSTABILITÁS is panasz, fájdalom-szó nélkül: a „bokám
                // gyakran kifordul futás közben" és az „instabil a térdem"
                // eddig válasz nélkül maradt – pedig pont ezekre való a sor.
                "kifordul", "kibicsaklik", "megbicsaklik", "bebicsaklik",
                // Harmadik személyű alak és a panasz FŐNEVE: a „már egy hete
                // húzza a vállam" és a „megint bejött a régi térdproblémám"
                // eddig válasz nélkül maradt.
                "huzza", "huzza a", "problema", "problemam", "panaszom",
                "baj van a", "kiujult", "elojott",
                "kicsuszik", "instabil", "bizonytalan a", "megrogyik",
                // A GYENGESÉG is panasz, csak nem fáj: a „gyenge a törzsizmom"
                // és a „nem bírja a bokám" ugyanolyan pontos leírás, mint egy
                // fájdalom-szó – és eddig egyikre sem jött válasz.
                "gyenge a", "gyengek a", "gyengeseg", "elgyengult", "nem birja",
                "nem birom megtartani", "elfarad",
                // A RÖVIDÜLÉS ugyanígy: „rövid a combhajlítóm", „feszes a vádlim".
                "rovid a", "roviduelt", "roviduelt", "beszukult",
                "ropog", "recseg", "kattog", "roppan", "huzodott", "huzodo",
                // Az igekötős húzódás is húzódás: a „meghúzódott a hátam a
                // 120 kg-os felhúzásnál" eddig válasz nélkül maradt. A
                // „behúzódott a combhajlítóm" ugyanaz b-vel.
                "meghuzod", "behuzod", "megrandult",
                // A SÉRÜLÉS kimondva is panasz: a „lesérültem focin, boka"
                // pont az a mondat, ami után a boka-lap kell.
                "leserul", "megserul", "serules", "serulest",
                "meghuztam", "becsipodott", "belovellt", "lumbago", "merev",
                // A diagnózis NEVE maga a panasz: aki azt írja, „golfkönyök",
                // az nem érdeklődik, hanem fáj neki. Eddig ezekre a mondat
                // egyáltalán nem talált semmit – pedig a lap pont róluk szól.
                "golfkonyok", "teniszkonyok", "futoterd", "ugroterd", "sarkantyu",
                "plantaris",
                // A SÉRV, a PROTÉZIS és a friss MŰTÉT ugyanígy a diagnózis
                // neve: a „csigolyasérvem van" és a „műtét után vagyok,
                // térdprotézis" eddig válasz nélkül maradt – pedig ezek a
                // mondatok kérnek a leghatározottabban óvatos tornát.
                "servem", "serve ", "porckorongserv", "csigolyaserv",
                "gerincserv", "protezis", "mutet utan", "muteti utan",
                "operacio utan", "mutottek",
                // A SÉRÜLÉS és a KOPÁS neve is diagnózis: a „meniszkusz
                // műtéten estem át", a „keresztszalag szakadás után vagyok"
                // és a „csigolya kopás a nyakamban" eddig válasz nélkül
                // maradt. A DUZZANAT pedig a legfontosabb jel: a „vizesedik
                // a térdem edzés után" pont az a mondat, amire a lap piros
                // zászlója szól.
                "kopas", "meniszkusz", "keresztszalag", "szalagszakadas",
                "diasztazis", "diastasis", "duzzad", "bedagadt", "megdagadt",
                "vizesedik", "befolyosodott"}) {
            int i = s.indexOf(w);
            while (i >= 0) {
                boolean l = i == 0 || !Character.isLetter(s.charAt(i - 1));
                if (l && !falsePain(s, i)) { pain = true; break; }
                i = s.indexOf(w, i + 1);
            }
            if (pain) break;
        }
        // Összetett panasz-főnév: a „derékfájás", a „csípőfájdalom" és a
        // „sarokfájdalom" egyetlen szó, tehát a fájdalom-szó a szó BELSEJÉBE
        // esik – a szókezdet-vizsgálat így mindet elutasította. Pedig a
        // magyar leggyakrabban pont így mondja el, mi a baj.
        if (!pain && (s.contains("fajas") || s.contains("fajdalom")
                || s.contains("fajdit")
                // A „térdproblémám" és a „vállpanaszom" ugyanígy egy szó: a
                // „megint bejött a régi térdproblémám" eddig válasz nélkül
                // maradt.
                || s.contains("problem") || s.contains("panasz"))) pain = true;
        // Panasz-szavak, amiket a magyar igekötővel mond: az „elgémberedik",
        // a „megfeszül" és a „bemerevedett" a szótő ELÉ tesz egy szótagot,
        // így a szókezdet-vizsgálat mindet elutasította. Ezek elég hosszúak
        // és elég egyediek ahhoz, hogy a szó belsejében is biztosak legyünk.
        if (!pain)
            for (String w : new String[]{"gemberedik", "gemberedett", "feszul", "gorcsol",
                    "gorcsbe", "merevedett", "merevedik", "merevedes", "merevseg",
                    "gerincferdul", "szkolioz", "zsugorod",
                    "gorbe", "gorbul"})
                if (s.contains(w)) { pain = true; break; }
        // A legrövidebb panasz-igék csak EGÉSZ szóként: a „húz" a
        // húzódzkodásban, a „szúr" a szúrópróbában lakik. „Húz a vádlim",
        // „szúr a derekam" – ennél magyarabbul nem lehet elmondani.
        if (!pain)
            for (String w : new String[]{"huz", "szur", "gorcs"}) {
                int i = s.indexOf(w);
                while (i >= 0) {
                    boolean l = i == 0 || !Character.isLetter(s.charAt(i - 1));
                    int e = i + w.length();
                    boolean r = e >= s.length() || !Character.isLetter(s.charAt(e));
                    if (l && r) { pain = true; break; }
                    i = s.indexOf(w, i + 1);
                }
                if (pain) break;
            }
        if (!pain) return null;
        // A tagadott vagy elmúlt panasz jó hír, nem kérés: a „nem fáj a
        // vállam" és a „már nem fáj" után nincs mit ajánlani.
        for (String neg : new String[]{"nem faj", "mar nem", "elmult", "meggyogyult",
                "nem fajt", "fajdalommentes", "fajdalom nelkul"})
            if (s.contains(neg)) return null;
        // A piros zászlós panaszra nem sort ajánlunk: arra a redFlag()
        // figyelmeztetése a válasz, és azt a képernyő mutatja meg.
        if (redFlag(q) != null) return null;
        return areaOf(s);
    }

    /**
     * Piros zászlós jelek: [tő, ahogy magyarul hívjuk].
     *
     * Ezek nem „erősítsd meg" ügyek. A zsibbadás idegre utal, a duzzanat
     * gyulladásra vagy sérülésre, az éjszakai fájdalom pedig arra, hogy a
     * panasz nem a terheléstől függ – ezekre a vizsgálat a helyes lépés,
     * nem egy gyakorlatsor. Rövid a lista, mert a fals riasztás is árt:
     * aki mindenre azt hallja, „menj orvoshoz", az legközelebb nem ír be
     * semmit.
     */
    private static final String[][] RED_SIGNS = {
            {"zsibbad", "zsibbadás"},
            {"bizsereg", "zsibbadás"},
            {"duzzad", "duzzanat"}, {"dagadt", "duzzanat"}, {"bedagadt", "duzzanat"},
            {"ejszaka faj", "éjszakai fájdalom"}, {"ejjel faj", "éjszakai fájdalom"},
            {"alvasbol ebreszt", "éjszakai fájdalom"},
            {"sugarzo", "sugárzó fájdalom"}, {"sugarzik", "sugárzó fájdalom"},
            {"nem tudok ralepni", "terhelhetetlenség"},
            {"nem birok ralepni", "terhelhetetlenség"},
            {"nem tudok ralepni", "terhelhetetlenség"},
            {"nem tudok lepni", "terhelhetetlenség"},
            {"nem tudok rendesen lepni", "terhelhetetlenség"},
            {"nem tudok rendesen jarni", "terhelhetetlenség"},
            {"nem tudok jarni", "terhelhetetlenség"},
            {"nem tudom mozgatni", "mozgásképtelenség"},
            // A TÖRÉS nem gyakorlat-ügy: az „eltört a lábam síelésnél"
            // eddig kétórás síelésként került a naplóba.
            {"eltort", "törésgyanú"}, {"eltorott", "törésgyanú"},
            {"csontom tort", "törésgyanú"}, {"toresem", "törésgyanú"},
    };

    /**
     * Terhelés alatti mellkasi panasz – ez nem izomügy.
     *
     * Az app eddig ezt is csendben elengedte: az „erős fájdalom a
     * mellkasomban futás közben" mondatra semmilyen válasz nem jött. Ez az
     * egyetlen panasz, ahol nem elég időpontot kérni: itt abba kell hagyni a
     * mozgást, és azonnal segítséget kérni. Szűk a lista – csak a mellkasi
     * fájdalom/szorítás és az eszméletvesztés –, hogy a hétköznapi kifulladás
     * ne riasszon feleslegesen.
     */
    private static final String[] HEART_SIGNS = {
            "faj a mellkasom", "mellkasi fajdalom", "fajdalom a mellkasomban",
            "faj a mellkasomban", "mellkasom faj", "szorit a mellkasom",
            "osszeszorul a mellkasom", "elszorul a mellkasom",
            "nyomo erzes a mellkasom", "szorito erzes a mellkasom",
            "elajultam", "elvesztettem az eszmeletem", "elsotetult a vilag",
            // A SZÉDÜLÉS is ide tartozik, ha edzés közben vagy után jön: a
            // „szédülés és hányinger edzés után" mondatra eddig egyáltalán
            // nem jött válasz, pedig ez a keringés jelzése, nem izomprobléma.
            "szedules es hanyinger", "hanyinger es szedules",
            "megszedultem edzes", "szedulok edzes",
    };

    /**
     * Idegrendszeri és gyulladásos vészjel a rögzített listán túl.
     *
     * A DERÉKFÁJÁS melletti vizelési panasz a gerincvelő alsó szakaszának
     * jelzése: erre órák alatt kell orvos, nem gyakorlatsor. Az ÉRZÉSKIESÉS
     * ugyanígy, a forró-piros-duzzadt ízület pedig gyulladás. Egyikre sem
     * jött eddig semmilyen válasz.
     *
     * @return a panasz megnevezése, vagy null
     */
    private static String otherWarning(String s) {
        // A KIMONDOTT tagadás erősebb: a „derékfájás, de vizeléssel nincs
        // baj" épp azt mondja, hogy ez a jel HIÁNYZIK.
        if (s.matches("(?s).*(nincs baj|nincs panasz|nem gond|rendben van"
                + "|nincs vele baj|nem fordult elo).*")) return null;
        boolean back = s.matches("(?s).*(derek|dereka|hat |gerinc|lumb|keresztcsont).*");
        if (back && s.matches("(?s).*(vizele\\w*|szekele\\w*|inkontinen\\w*"
                + "|nem tartom a vizelet\\w*).*"))
            return "derékfájás vizelési panasszal";
        if (s.matches("(?s).*(elvesztettem az erzest|nincs erzes|erzeskieses"
                + "|nem erzem a lab\\w*|nem erzem a kez\\w*).*"))
            return "érzéskiesés";
        boolean joint = s.matches("(?s).*(izulet\\w*|terd\\w*|boka\\w*|konyok\\w*"
                + "|csuklo\\w*|vall\\w*|csipo\\w*).*");
        if (joint && s.contains("piros")
                && s.matches("(?s).*(meleg|forro|dagadt|duzzad\\w*|belaz\\w*).*"))
            return "forró, piros, duzzadt ízület";
        return null;
    }

    /**
     * Szívre utaló panasz SZÓRENDTŐL függetlenül.
     *
     * A rögzített szókapcsolatok (HEART_SIGNS) csak a leggyakoribb alakokat
     * fedik, a magyar mondat viszont szabadon rendezi őket: a „szorító
     * fájdalom a mellkasban", a „fájt a mellkasom edzés alatt" és a
     * „belenyilallt a mellkasomba" mind ugyanazt írja le, és egyikre sem
     * jött semmilyen válasz. Itt a tévedés ára aszimmetrikus: egy fölösleges
     * figyelmeztetés kellemetlen, egy elmaradó nem javítható.
     *
     * A MELLIZOM kivétel: a „megfájdult a mellizmom fekvenyomás közben"
     * izompanasz, nem szívügy.
     */
    private static boolean heartWarning(String s) {
        boolean muscle = s.matches("(?s).*(mellizom|mellizm|borda|szegycsont"
                + "izomlaz).*");
        boolean chest = s.matches("(?s).*(?<![a-z])(mellkas\\w*|szivem tajek\\w*"
                + "|szivtaj\\w*)(?![a-z]).*");
        // SZÓHATÁRRAL: a „mellkas nap: FEKVENYOMÁS 3x10" belsejében is ott
        // a „nyomás", és a mellkasnap piros zászlót kapott tőle.
        boolean pain = s.matches("(?s).*(?<![a-z])(faj|fajt|fajdalom|fajdalmas"
                + "|szorit\\w*|szorito|nyomo|nyomas|nyilall\\w*|belenyilall\\w*"
                + "|feszul\\w*|eget|szur|szurt)(?![a-z]).*");
        if (chest && pain && !muscle) return true;
        // Az ESZMÉLETVESZTÉS önmagában is az: a „eszméletemet vesztettem" és
        // az „összeestem" nem gyakorlat-kérdés.
        if (s.matches("(?s).*(?<![a-z])(eszmeletemet vesztettem|eszmeletvesztes"
                + "|osszeestem|ajultam el|elajult\\w*)(?![a-z]).*")) return true;
        // A HIDEG VEREJTÉK a hányingerrel vagy szédüléssel együtt klasszikus
        // keringési jel.
        if (s.contains("hideg verejtek") || s.contains("hideg verite"))
            if (s.matches("(?s).*(hanyinger|szedul|mellkas|rosszul).*"))
                return true;
        // A TERHELÉS alatti szédülés is: az „elszédültem edzés közben" a
        // keringés jelzése, nem izomprobléma.
        if (s.matches("(?s).*(?<![a-z])(elszedultem|megszedultem|szedultem)"
                + "(?![a-z]).*")
                && s.matches("(?s).*(edzes|futas|mozgas|emeles|guggol|sorozat"
                + "|kozben|utan).*")) return true;
        return false;
    }

    /**
     * Figyelmeztetés a piros zászlós panaszra, vagy null.
     *
     * Az app eddig HALLGATOTT ezekre: a „zsibbad a kezem" mondatra nem jött
     * semmilyen válasz, mert gyakorlatsort nem akartunk ajánlani rá. A
     * hallgatás viszont a legrosszabb válasz – azt üzeni, hogy nem értjük,
     * pedig pont hogy értjük, és éppen ezért nem tornáztatunk.
     */
    public static String redFlag(String q) {
        if (q == null) return null;
        String s = Foods.norm(q);
        // A mellkasi panasz a tagadás-szűrő ELŐTT áll: itt a tévedés ára
        // aszimmetrikus. Egy felesleges figyelmeztetés kellemetlen, egy
        // elmaradó viszont nem javítható.
        for (String h : HEART_SIGNS)
            if (s.contains(h) || heartWarning(s))
                return "Amit leírtál (mellkasi panasz), arra semmilyen "
                        + "gyakorlatsor nem jó válasz. Hagyd abba a mozgást, ülj "
                        + "vagy feküdj le, és kérj SÜRGŐS orvosi segítséget – ha "
                        + "a panasz pár percen belül nem múlik, hívd a 112-t. Ez "
                        + "akkor is így van, ha eddig egészséges voltál, és akkor "
                        + "is, ha csak enyhének érzed.";
        for (String neg : new String[]{"nem zsibbad", "mar nem", "elmult", "nem dagadt"})
            if (s.contains(neg)) return null;
        String extra = otherWarning(s);
        for (String[] r : RED_SIGNS)
            if (s.contains(r[0]) || (extra != null && r[0].equals("zsibbad")))
                return "Amit leírtál (" + (extra != null ? extra : r[1])
                        + "), arra nem gyakorlatsor a jó válasz. "
                        + "Ez a jel azt jelenti, hogy a panaszt meg kell nézetni: "
                        + "kérj időpontot orvostól vagy gyógytornásztól, és addig ne "
                        + "terheld. Ha ez elmúlt, és csak a régi merevség maradt, "
                        + "gyere vissza – a megelőző sorok akkor a te dolgod.";
        return null;
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
                "rehab", "gyogytorna", "megeloz", "prevenc",
                // A cél melléknévvel is kimondható: „erősebb bokát szeretnék",
                // „stabilabb térdet akarok", „mozgékonyabb csípő kellene".
                "erosebb", "stabilabb", "mozgekonyabb", "rugalmasabb", "egyenesebb",
                // A TARTÁS magában is cél: a „jobb tartás" és a „helyes
                // tartás" a háti szakasz ügye. (A „plank tartás 3x60" az
                // erősítő naplóé – az a felismerő hamarabb szólal meg.)
                "tartas",
                // Ahogy az ember tényleg kéri: „váll gyakorlatok", „mit
                // csináljak a vállammal", „nyak lazítás", „váll bemelegítés".
                // A NYÚJTÁS szándékosan nincs itt: az önálló, naplózható
                // mozgásforma („45 perc nyújtás", „combnyújtás 30 perc"), és
                // cél-szóként elvenné az edzés-naplótól.
                "gyakorlat", "lazit", "bemelegit", "atmozgat",
                "mit csinaljak", "mit ajanlasz", "mit javasolsz",
                // Az ERŐSÍTÉS is ide került: a „boka erősítés" korábban egy
                // hatvanperces kondi-bejegyzés lett a naplóban – vagyis egy
                // meg nem történt edzés. A testtájnév itt is kötelező, a
                // súlyzós mondat pedig továbbra is az erősítő naplóé, mert
                // az a felismerő hamarabb szólal meg.
                "erosit",
                // Ahogy segítséget kérünk: „nyújtani kéne a combhajlítót",
                // „kell valami a talpamra". A puszta NYÚJTÁS továbbra sem
                // szándék-szó (az naplózható mozgásforma), de a kimondott
                // kérés mellett félreérthetetlen.
                "nyujtani kene", "nyujtani kell", "nyujtani kellene",
                "kell valami", "kene valami", "adj gyakorlat", "adj valami",
                "hogyan erosit", "hogy erosit"})
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
        // A reggeli ELSŐ LÉPÉS fájdalma a talpi ín klasszikus jele, nem az
        // Achillesé – a sarok szó magában viszont az Achilleshez visz. A
        // pontosabb megfogalmazás nyer.
        if ((s.contains("elso lepes") || s.contains("elso lepesnel")
                || s.contains("elso lepeskor"))
                && (s.contains("sarok") || s.contains("sarkam") || s.contains("talp")))
            return byId("talp");
        // A „csípős" étel és a „vállal" ige nem testtáj – kitakarjuk, mielőtt
        // a rövid tövek („csipo", „vall") beleakadnának. A „fáj a hasam a
        // csípős kajától" panasz, de nem csípő-ügy.
        s = s.replace("csipos", "#").replace("vallal", "#");
        String[][] map = {
                {"boka", "bokam", "bokaja", "boka"},
                // A térd KÜLSŐ oldala más panasz, mint az elülső – az
                // IT-szalag sora a csípő távolítóit erősíti. A hosszabb,
                // pontosabb megnevezés ezért előbb áll a puszta „térd"-nél.
                {"itszalag", "it szalag", "it-szalag", "itszalag", "iliotibialis", "kulso terd",
                        "terd kulso", "terdem kulso", "futoterd"},
                {"terd", "terdem", "terde", "ugroterd", "terdfaj", "terdprotezis",
                        // A térd két leggyakoribb sérülésének a NEVE is a
                        // térdre mutat, a testrész kimondása nélkül.
                        "meniszkusz", "keresztszalag", "szalagszakadas",
                        "terd"},
                // A HÁTI gerinc a derék elé kerül: a „felső hátam" és a
                // „lapockáim között" nem ágyéki panasz, és a derék-sor
                // (madár-kutya, curl-up) nem is szól róla. A puszta „hátam"
                // marad a deréknál: aki csak annyit mond, hogy fáj a háta,
                // az magyarul legtöbbször az ágyéki szakaszra gondol.
                // A „háti" jelzőként is egyértelmű („háti gyakorlat", „háti
                // mobilizálás") – szóközzel a végén, hogy a „hátizsák" ne
                // essen ide.
                {"hati", "hati ", "hati gerinc", "hati csigolya", "felso hat", "felso hatam",
                        // A TARTÁS a háti szakasz ügye: „jobb tartás",
                        // „helyes tartás", „tartásjavítás".
                        "jobb tartas", "helyes tartas", "tartasjavit", "rossz tartas",
                        // A ragozott alak is ugyanaz a kérés: „hogyan
                        // javítsam a tartásomat?" eddig válasz nélkül maradt.
                        "tartasom", "tartasod", "tartasa javul", "a tartason",
                        "tartas javit", "egyenes tartas", "roskadt", "roggyant",
                        "lapocka", "hat kozepe", "hatam kozepe", "mellkasi gerinc",
                        "gorbe hat", "gorbult hat", "gorbe a hat"},
                {"derek", "derekam", "dereka", "derek", "hatam", "hatfaj", "also hat",
                        "gerincem", "gerinc",
                        // A porckorong- és csigolyasérv a deréké: ott is a
                        // törzs stabilizálása a dolog, csak még óvatosabban.
                        "porckorong", "csigolyaserv", "gerincserv"},
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
                {"csuklo", "csuklom", "csukloja", "csuklo faj", "csuklofajas", "egerkez",
                        // A KÉZ panasza is ide fut: a reggeli elgémberedés és
                        // az ujjak merevsége ugyanarról szól, mint a csukló
                        // mozgástartománya. (A puszta „kéz" nem tő – a
                        // kezdetben és a kezelésben is benne lakik –, a ragos
                        // alak viszont félreérthetetlen.)
                        "kezem", "kezeim", "ujjam", "ujjaim",
                        // A puszta „csukló" is tő: a cél-mondat („csukló
                        // mobilizálás") enélkül az edzés-felismerőhöz futott.
                        // Panasznak továbbra is fájdalom-szó kell mellé.
                        "csuklo"},
                {"nyak", "nyakam", "nyaka", "nyak", "tarkom"},
                // A FARIZOM a csípő ügye: az „erősíteni kéne a farizmom"
                // eddig válasz nélkül maradt.
                {"csipo", "csipom", "csipoje", "csipoprotezis", "csipo",
                        "farizom", "farizmo",
                        // A TOMPOR a csípő külső csontos pontja – a futók
                        // gyakori panasza, és eddig üres választ kapott.
                        "tompor", "tomporom",
                        "farpofa", "gluteusz"},
                // A LÁBIKRA a vádli hétköznapi neve: a „görcsöl a lábikrám"
                // eddig válasz nélkül maradt, pedig az Achilles-vádli sor
                // pont erre való.
                {"achilles", "achilles", "vadlim", "vadli", "labikra", "labikram",
                        "sarkam", "sarok faj", "sarokfaj"},
                // A sarkantyú és a plantaris fasciitis a TALP sora, nem az
                // Achillesé: a fájdalom a talp elülső-belső élén ébred, és a
                // talpi szalagot kell terhelni hozzá.
                {"talp", "talpam", "talpa", "talpfaj", "plantaris", "sarkantyu", "talp"},
                // A LÁBSZÁR ugyanaz a panasz, csak hétköznapibb néven: a
                // „lábszárfájás futás után" eddig válasz nélkül maradt, pedig
                // a kezdő futók leggyakoribb baja.
                {"sipcsont", "sipcsontom", "sipcsontja", "sipcsont", "labszaram",
                        "labszarfaj", "labszar", "shin splint", "shinsplint"},
                {"comb", "combom", "combhajlito", "hatso comb", "comb hatulja"},
                // A TÖRZS a leggyakoribb cél-mondat, és eddig nem volt hova
                // vinni: a „core erősítés" hatvanperces kondi-edzés lett, a
                // „gyenge a törzsizmom" pedig válasz nélkül maradt.
                {"torzs", "torzsizm", "torzsem", "torzs stabil", "torzsstabil",
                        "core", "melyizom", "mely izom", "harant hasizom",
                        "medencefenek", "medencem", "medence stabil", "hasizmom",
                        "hasizmaim", "hasfalam", "rectus diastasis", "diastasis",
                        "diasztazis"},
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

    /** A vezetett mód munkaideje és pihenője másodpercben (a gombhoz és az időzítőnek). */
    public static final int GUIDED_WORK = 40, GUIDED_REST = 8, GUIDED_PREP = 5;

    /**
     * A vezetett sor tényleges hossza percben.
     *
     * A lap fejlécén a gyakorlatok adagolásából becsült idő áll (10–15 perc),
     * a vezetett mód viszont fix ablakokkal dolgozik – a kettő nem ugyanaz.
     * Ha a gombra írt szám nem igaz, az a legrosszabb fajta apró hazugság:
     * az ember beosztja rá az idejét.
     */
    public static int guidedMinutes(Area a) {
        int items = guidedNames(a).size() * guidedRounds(a);
        int sec = GUIDED_PREP + items * (GUIDED_WORK + GUIDED_REST);
        return Math.max(1, (int) Math.round(sec / 60.0));
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
