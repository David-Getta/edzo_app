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
}
