package com.edzo.idozito;

/**
 * Bemelegítés, nyújtás (izmonként legalább kettő) és hengerezés (SMR) tartalom.
 * Minden gyakorlathoz rövid technikai leírás és egy videó-keresőkifejezés
 * (YouTube), amit az app egy koppintással megnyit.
 */
public final class Mobility {

    private Mobility() {}

    public static final class Item {
        public final String name, desc, video;
        public Item(String name, String desc, String video) { this.name = name; this.desc = desc; this.video = video; }
    }

    public static final class Group {
        public final String title;
        public final Item[] items;
        public Group(String title, Item[] items) { this.title = title; this.items = items; }
    }

    static Item it(String n, String d, String v) { return new Item(n, d, v); }
    static Group g(String t, Item... i) { return new Group(t, i); }

    // ---------------- Bemelegítés ----------------
    public static final Group[] WARMUP = {
            g("Dinamikus bemelegítés",
                    it("Magas térd", "Emeld a térded csípőmagasságig, ütemesen, karral segíts.", "magas térd bemelegítés"),
                    it("Sarokemelés (fenékrúgás)", "Sarkat a fenék felé rúgd, gyors lábváltással.", "fenékrúgás bemelegítő gyakorlat"),
                    it("Karkörzés", "Nyújtott karral nagy körök előre, majd hátra.", "karkörzés bemelegítés"),
                    it("Vállkörzés", "Vállak lazán, körkörösen, előre és hátra.", "vállkörzés bemelegítés"),
                    it("Csípőkörzés", "Kezek a csípőn, rajzolj nagy köröket a csípővel.", "csípőkörzés bemelegítés"),
                    it("Törzsfordítás", "Terpeszben forgasd lazán a felsőtested oldalról oldalra.", "törzsfordítás bemelegítés"),
                    it("Láblendítés előre-hátra", "Kapaszkodj, lendítsd nyújtott lábad előre és hátra.", "láblendítés bemelegítés"),
                    it("Láblendítés oldalra", "A test előtt keresztezve lendítsd a lábad oldalra.", "oldalsó láblendítés bemelegítés"),
                    it("Dinamikus kitörés", "Sétáló kitörés nagy lépésekkel, egyenes törzzsel.", "dinamikus kitörés bemelegítés"),
                    it("Bokakörzés", "Emeld a lábfejed, körözz mindkét irányba.", "bokakörzés bemelegítés"),
                    it("Jumping jack", "Szökdelés terpeszbe, karok a fej fölé.", "jumping jack bemelegítés")),
    };

    // ---------------- Nyújtás (izmonként 2) ----------------
    public static final Group[] STRETCH = {
            g("Nyak",
                    it("Oldalsó nyaknyújtás", "Húzd a fejed a válladhoz, kézzel finoman segíts.", "nyaknyújtás oldalra"),
                    it("Tarkónyújtás", "Állat a mellkas felé, érezd a tarkó nyúlását.", "nyaknyújtás tarkó")),
            g("Váll",
                    it("Deltoid keresztnyújtás", "Húzd a kart a mellkas előtt keresztbe, tartsd 20 mp-et.", "vállnyújtás keresztbe"),
                    it("Hátsó váll nyújtás", "Könyököt a fej mögé, húzd a másik kézzel.", "hátsó vállizom nyújtás")),
            g("Mell",
                    it("Falnál mellnyújtás", "Alkar a falon, fordulj el, nyújtsd a mellizmot.", "mellizom nyújtás falnál"),
                    it("Hátul kulcsolt kéz", "Kulcsold a kezed hátul, emeld a kart, mellkas ki.", "mellkasnyújtás kulcsolt kéz")),
            g("Felső hát",
                    it("Macska-teve", "Négykézláb domborítsd, majd lazítsd a hátad ütemesen.", "macska teve gyakorlat"),
                    it("Széles hátizom nyújtás", "Nyújtózz fel és oldalra, húzd a hátad ívesen.", "széles hátizom nyújtás")),
            g("Tricepsz",
                    it("Fej mögötti tricepsz", "Könyök a fej mögött, másik kézzel húzd lefelé.", "tricepsz nyújtás fej mögött"),
                    it("Váll-tricepsz nyújtás", "Kar keresztbe, nyomd a könyököt a test felé.", "tricepsz nyújtás váll")),
            g("Törzs / oldal",
                    it("Álló oldalhajlítás", "Kar a fej fölé, hajolj oldalra, nyújtsd az oldalad.", "oldalhajlítás nyújtás"),
                    it("Ülő gerinccsavarás", "Ülve csavard a törzsed, könyököt a térdnek támaszd.", "ülő gerinccsavarás nyújtás")),
            g("Csípőhajlító",
                    it("Térdelő csípőnyújtás", "Fél térden told előre a csípőd, feszítsd a farizmot.", "csípőhajlító nyújtás térdelő"),
                    it("Kitöréses csípőnyújtás", "Mély kitörésben süllyeszd a csípőd lefelé.", "csípőhajlító nyújtás kitörés")),
            g("Comb elülső (quad)",
                    it("Álló combnyújtás", "Húzd a bokád a fenékhez, térdek egymás mellett.", "combizom nyújtás álló"),
                    it("Fekvő combnyújtás", "Oldalt fekve húzd a bokád a fenékhez.", "quad nyújtás fekve")),
            g("Combhajlító (hamstring)",
                    it("Ülő előrehajlás", "Nyújtott láb, hajolj a lábfej felé, hát egyenes.", "combhajlító nyújtás ülő"),
                    it("Álló combhajlító", "Sarok előre, hajolj a csípőből előre.", "combhajlító nyújtás álló")),
            g("Fenék (farizom)",
                    it("Fekvő 4-es nyújtás", "Boka a másik térden, húzd a combot magad felé.", "farizom nyújtás fekvő négyes"),
                    it("Galamb póz", "Elülső láb behajlítva, süllyeszd a csípőd le.", "galamb póz farizom nyújtás")),
            g("Vádli",
                    it("Falnál vádlinyújtás", "Hátsó láb nyújtva, sarok a földön, dőlj a falnak.", "vádlinyújtás falnál"),
                    it("Lépcsős vádlinyújtás", "Sarok lóg a lépcsőről, engedd le lassan.", "vádlinyújtás lépcsőn")),
            g("Belső comb (adduktor)",
                    it("Pillangó ülés", "Talpak össze, engedd a térded a föld felé.", "pillangó nyújtás belső comb"),
                    it("Oldalkitöréses nyújtás", "Egyik térd hajlik, másik láb nyújtva oldalt.", "adduktor nyújtás oldalkitörés")),
    };

    // ---------------- Hengerezés (SMR) ----------------
    public static final Group[] ROLLING = {
            g("Hengerezés (SMR – habhenger)",
                    it("Comb elülső (quad)", "Hason fekve, henger a comb alatt, gördülj csípőtől térdig.", "quad SMR foam roller"),
                    it("Combhajlító", "Ülve, henger a comb hátulján, lassan gördülj.", "combhajlító SMR foam roller"),
                    it("Vádli", "Ülve, henger a vádli alatt, a másik lábbal nyomd rá.", "vádli SMR foam roller"),
                    it("Farizom", "Ülj a hengerre, boka a másik térden, dőlj oldalra.", "farizom SMR foam roller"),
                    it("Comb külső (IT-szalag)", "Oldalt fekve, henger a comb külsején, lassan gördülj.", "IT szalag SMR foam roller"),
                    it("Belső comb (adduktor)", "Hason, comb kifordítva, henger a belső combon.", "adduktor SMR foam roller"),
                    it("Felső hát", "Hanyatt, henger a lapockák alatt, emeld a csípőd, gördülj.", "felső hát SMR foam roller"),
                    it("Széles hátizom", "Oldalt fekve, kar felnyújtva, henger a hónalj alatt.", "latissimus SMR foam roller")),
    };
}
