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
                    "futoverseny", "terepfutas", "spartan", "parkrun",
                    // A „LERAKTAM 10 KÖRT A PÁLYÁN" sportnév nélkül is futás:
                    // a kör + pálya páros az atlétikai pályát mondja ki. A tő
                    // a teljes szókapcsolat, mert a puszta „kör" edzésterv, a
                    // puszta „pálya" pedig gokartpálya is lehetne – a
                    // „gokartpalyan" szóban nincs benne a „ palyan" tagolás.
                    "kort a palyan", "kor a palyan", "koroket a palyan"),
            new Kind("uszas", "🏊", "Úszás", 7.0, true, 45,
                    "uszas", "uszo edzes", "uszni", "uszoedzes", "uszodaz", "uszt", "uszkal",
                    // A vizes termi óra is a medencében van: „aqua fitness
                    // 45 perc", „aqua aerobik".
                    "aqua fitness", "aquafitness", "aqua aerobik", "aquaaerobik",
                    "aqua zumba",
                    // Az USZODA maga is úszás: az „a helyi uszodában 1 km"
                    // eddig egykilométeres FUTÁS lett, mert a puszta táv
                    // magyarul futást jelent.
                    "uszoda", "tanmedence", "strandon usz",
                    "uszoverseny",
                    // A medence RAGOZOTT alakja: a puszta „medence" a súlyzós
                    // medenceemelés szava is, azt nem vesszük el tőle.
                    "medenceben", "uszomedence", "szinkronuszas",
                    // A vizes sportok is ide: a vízilabda és a vizitorna a
                    // medencés mozgások közül az úszáshoz áll a legközelebb.
                    "vizilabda", "aquafit", "vizitorna",
                    // Az ÚSZÁSNEM neve is kimondja a sportot: „medence:
                    // 1000 m gyorson" eddig egykilométeres FUTÁS lett. A
                    // puszta „mellen" és „háton" viszont nem lehet tő: az
                    // egyik a MELLÉNY-ben is benne van, a másik a „háton
                    // fekve" hasizomgyakorlatban.
                    "gyorson", "gyorsuszo", "melluszo", "hatuszo",
                    "pillangozas", "pillangoztam",
                    // A VÍZTAPOSÁS is vízben végzett munka – mindkét
                    // szórenddel.
                    "vizet tapos", "vizet tapostam", "viztaposas",
                    "tapostam a vizet",
                    // Az úszásnem FŐNÉVI alakja is: a „mellúszás 800 m" és a
                    // „hátúszás 800 m" nyolcszáz méteres FUTÁS lett – az
                    // „uszas" tő szó belsejében szándékosan nem él (a
                    // beNEVEZve-féle hibák miatt), ezért az összetett szavak
                    // saját tövet kapnak.
                    "melluszas", "hatuszas", "gyorsuszas", "pillangouszas",
                    "vegyesuszas",
                    // A felszíni és a felszín alatti vizes sportok is ide:
                    // a snorkeling és a kitesurf ugyanabban a közegben zajlik.
                    "snorkel", "kitesurf", "kiteszorf"),
            new Kind("kerekpar", "🚴", "Kerékpár", 7.5, true, 60,
                    "kerekpar", "bringa", "bicikli", "bicaj", "canga", "teker", "bmx",
                    // A GÖRGŐN (edzőpadon) tekerés kerékpár: a „175 wattos
                    // átlaggal 90 perc a görgőn" eddig üresen jött vissza.
                    // A „görgőzés" NEM ide tartozik: az az SMR-henger
                    // (jóga-oldal) bevett szava.
                    "gorgon",
                    // A beszélt alakok és a leggyakoribb elgépelés: enélkül a
                    // „biciglizteem 20 km-t" HÚSZ KILOMÉTERES FUTÁS lett, mert
                    // a puszta táv futást jelent. Egy elütés nem érhet ennyit.
                    "bicigli", "bicikl", "bicig", "bico", "bicoz", "biczik",
                    // A bringás szókincs sportnév nélkül is tekerés: az
                    // „országúti kör 60 km" eddig futás lett, a Zwift
                    // virtuális tekerés és az e-bike pedig semmi. A DEFEKT
                    // is bringát mond: a „defekt miatt csak 15 km lett" a
                    // nyeregben történt. Az „mtb tura" egyben tő, hogy a
                    // fej-szó szabály ne a gyalogtúrának adja.
                    "orszaguti", "zwift", "mtb tura", "mtb-tura",
                    "e-bike", "ebike", "e bike", "pedelec",
                    "elektromos bicikli", "defekt",

                    // A spinning teremben zajlik, de a lába ugyanazt csinálja:
                    // a tánc MET-je alábecsülte.
                    "spinning", "szobabicikli", "spinning ora",
                    // A termi óra rövid neve: „spin óra 50 perc". A puszta
                    // „spin" nem tő – az óra-szó teszi félreérthetetlenné.
                    "spin ora", "spinora", "spin-ora", "indoor cycling",
                    // A „bringatúra" egyben fedi a „bringa" és a „túra" tövet is.
                    "bringatura", "biciklitura", "kerekpartura",
                    // A terepkerékpár a hazai szóhasználatban angolul él:
                    // „mtb", „mountain bike", és újabban a „gravel".
                    "mountain bike", "mountainbike", "mtb", "gravel"),
            new Kind("tura", "🥾", "Túra / gyaloglás", 5.3, true, 90,
                    "tura", "gyaloglas", "seta", "setalas", "kirandul", "nordic",
                    "hegymasz", "megmaszt", "gyalog", "lepcsoz",
                    // A FELMÁSZTAM a kilátóhoz és a VÉGIGJÁRTAM tanösvény is
                    // túra: az egyik üresen jött vissza, a másik futás lett.
                    "felmasztam", "felmasztunk", "vegigjartam", "vegigjartuk",
                    "bejartam", "bejartuk",
                    // A ragozott lépcső is lépcsőzés – a „lépcsőház" viszont
                    // nem mozgás, ezért a puszta tő szándékosan kimarad.
                    "lepcsot", "lepcson", "lepcsomasz", "lepcsofutas", "babakocsi",
                    "barlangasz", "via ferrata",
                    // A magyar szétszedi az összetételt: „hegyet másztunk",
                    // „hegyre másztam" – a „hegymászás" tövét ez nem fedi.
                    "hegyet masz", "hegyre masz", "hegyi tura", "hegyet megmasz"),
            new Kind("evezes", "🚣", "Evezés / evezőgép", 7.0, true, 30,
                    "evezes", "evezo", "evezt", "kajak", "sup deszka", "kenu", "kenuz",
                    "raftin", "sarkanyhajo", "sarkany hajo",
                    // A „kajak túra" egyben tő, hogy a fej-szó szabály ne a
                    // gyalogtúrának adja: az a túra a vízen történt.
                    "kajak tura", "kajaktura", "kajak-tura", "kenu tura",
                    "kenutura",
                    // A PADDLE is evezés: a „paddleztem 5 km-t a Dunán"
                    // öt kilométeres futás lett. (A „padliz" tő tilos:
                    // a padlizsán nem vízisport.)
                    "paddlez",
                    // A SUP (álló evezés) a Balatonon a legnépszerűbb vizes
                    // sport – eddig egyik írásmódját sem ismertük.
                    "supozas", "supoztam", "szupozas", "paddleboard",
                    "allo evezes", "sup-ozas", "sup-oz", "supoz", "szupoz"),
            new Kind("kondi", "🏋", "Kondi / súlyzós edzés", 5.0, false, 60,
                    "kondi", "konditerem", "terem", "sulyzo", "gym", "gepterem", "gyur",
                    // A „core" a törzsizom edzése – konditermi szó, magyarul
                    // is így mondják.
                    "core edzes", "core-edzes", "coretrening",
                    // A „tornaterem" egyben fedi a „torna" (jóga) és a „terem"
                    // (kondi) tövet is – a hosszabb tő nyer, így egy találat lesz.
                    "crossfit", "kroszfit", "trx", "erosit", "fekvotamasz", "tornaterem", "wod",
                    // A FITNESZTEREM egyben fedi a „fitnesz" (egyéb) és a
                    // „terem" (kondi) tövet – a hosszabb tő nyer, egy találat.
                    "fitneszterem", "fitnessterem",
                    // A BIRTOKOS alak elveszti a második e-t: az „a hotel
                    // edzőtermében 30 perc" terme-je nem illeszkedett a
                    // „terem" tőre, és a fél óra elveszett.
                    "edzoterme", "konditerme", "tornaterme",
                    // A SAJÁT TESTSÚLYOS edzés a legolcsóbb edzésforma, és
                    // eddig nem volt szótő: az „otthon 30 perc saját testsúly"
                    // válasz nélkül maradt. A puszta „testsúly" nem lehet tő –
                    // az a mérleg szava.
                    "sajat testsul", "sajattestsul", "testsulyos edzes",
                    "testsulyos gyakorlat", "sajat testtomeg",
                    // A „TOLTAM A VASAT" a súlyzózás szlengje – a „vas" tő
                    // önmagában tilos (vasal, vasarnap, vasut), ezért csak a
                    // teljes szókapcsolat számít. A „vasaztam" múlt idejű,
                    // első személyű alak edzésnaplóban szintén a vasat jelenti.
                    "vasat tol", "vasat nyom", "toltam a vasat", "tolom a vasat",
                    "toltuk a vasat", "nyomtam a vasat", "vasaztam",
                    "koredzes", "kor edzes",
                    "guggolas", "felules", "huzodzkodas", "plank", "tabata",
                    "labnap", "mellnap", "vallnap", "karnap", "akadalypalya",
                    // A termi napok ANGOL neve legalább olyan gyakori a
                    // magyar edzők és edzettek szájában, mint a magyar: a
                    // „kemény leg day, 75 perc" eddig üres választ kapott.
                    "leg day", "legday", "push day", "pushday", "pull day",
                    "pullday", "upper body", "lower body", "full body",
                    "fullbody", "chest day", "back day", "arm day",
                    "shoulder day", "leg nap", "push nap", "pull nap",
                    // Termi óranevek és gépek: enélkül a bejegyzés elveszett.
                    // (Az elliptikus és a crosstrainer az „egyéb" alatt van.)
                    "body pump", "bodypump", "stepper",
                    "kettlebell", "funkcionalis edzes", "funkcionalis trening",
                    "bootcamp", "boot camp"),
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
                    // A squash angol írásmódja gyakran elgépelve érkezik – a
                    // „sqash 45 perc" eddig válasz nélkül maradt.
                    // A ping-pong KÖTŐJELLEL is jár, és a magyar ige is így
                    // ragozódik: a „ping-pongoztunk egy órát" eddig válasz
                    // nélkül maradt, pedig a kötőjeles alak legalább olyan
                    // gyakori, mint az egybeírt.
                    "ping pong", "ping-pong", "asztalitenisz", "padel", "sqash",
                    "skvos", "szkvos"),
            new Kind("harcmuveszet", "🥋", "Harcművészet / box", 10.0, false, 60,
                    // A BOXTEREM egyben fedi a „box" és a „terem" tövet – a
                    // hosszabb tő nyer, egy találat lesz.
                    "boxterem", "bokszterem",
                    "harcmuvesz", "kickbox", "box", "boksz", "karate", "judo", "birkozas",
                    "birkoz", "mma", "jiu-jitsu", "jiujitsu", "jiu jitsu", "bjj", "grappling",
                    "aikido", "onvedelm", "vivas", "taekwondo", "tekvondo",
                    "capoeira", "muay thai", "muaythai", "krav maga", "kravmaga",
                    // A szumó, a szambó és a kendó szándékosan hiányzik: a
                    // „kompromisszumot", a „számból" és a „kendőt" is
                    // tartalmazza őket, és egy hétköznapi szóból lett
                    // harcművészet-bejegyzés rosszabb, mint egy fel nem
                    // ismert ritka sportág.
                    "tai chi", "taichi"),
            new Kind("tanc", "💃", "Tánc / aerobik", 5.5, false, 60,
                    "tanc", "aerobik", "zumba", "kangoo", "alakformalo", "balett", "salsa",
                    "pole dance", "poledance", "rudtanc", "pole fitness",
                    "polefitness",
                    // A ZSÍRÉGETŐ ÓRA a termek kardió-osztálya – az étel-oldal
                    // ugyanezt a szót az olajtól védi (a zsír tövén ült). A
                    // puszta „zsírégető" NEM lehet tő: az időzítős „Zsírégető
                    // HIIT" program neve program marad, nem sport.
                    "zsireget ora", "zsiregeto ora", "zsirgeto ora"),
            new Kind("joga", "🧘", "Jóga / nyújtás / pilates", 3.0, false, 45,
                    // A „torna" fedi a gerinctornát, gyógytornát, tornázást is.
                    // A „nyujt" tő az igét is fedi: nyújtás, nyújtottam, nyújtok.
                    // A jóga-irányzatok neve is jóga: a „vinyasa flow 60
                    // perc" és a „napüdvözlet sorozat" eddig üresen jött
                    // vissza. A fascia-lazítás a henger rokona.
                    "vinyasa", "napudvozlet", "fascia", "mobility",
                    "joga", "yoga", "pilates", "nyujt", "stretch", "torna", "medital",
                    "meditac", "atmozgat", "mobiliz", "mobilitas", "legzogyakorlat",
                    "legzo gyakorlat",
                    // A hengerezés is regeneráció, és sokan naplózzák: eddig
                    // egyetlen alakját sem ismertük.
                    // A GÖRGŐZÉS ugyanaz, csak hétköznapibb néven – enélkül a
                    // „10 perc görgőzés edzés után" időzítő-TERVNEK látszott,
                    // és tíz perces ablakot ajánlott rá az app egy megtörtént
                    // levezetés helyett. (A „görget" scrollozás, az nem tő.)
                    "habhenger", "hengerez", "foam roll", "foamroll", "sms henger",
                    "gorgozes", "gorgoztem", "gorgozok", "gorgozni",
                    // Ugyanez a mozdulat a másik nevén: aki SMR-hengerrel
                    // dolgozik, „hengerelni" szokott. A „hengereltem a hátamat
                    // 10 percet" eddig válasz nélkül maradt.
                    "hengereles", "hengereltem", "hengerezes", "hengereztem",
                    "foam roller", "foamroller", "smr henger"),
            new Kind("korcsolya", "⛸", "Korcsolya / görkorcsolya", 7.0, false, 60,
                    "korcsolya", "gorkorcsolya", "gorkori", "gordeszka", "roller",
                    "jegkorong", "hoki", "curling"),
            // A sífutás táv-alapú: a „20 km sífutás" távja is számít.
            new Kind("si", "🎿", "Sí / snowboard", 6.0, true, 120,
                    // A „sízem/síztem/sízni" alakok is: a puszta „si" nem
                    // lehet szótő (a HASIZOMban is benne van).
                    "siel", "sizes", "siztem", "sizni", "sizunk", "sizik", "sizel",
                    // A FELSZERELÉS neve is kimondja a sportot: a „3 óra
                    // sítalpon" és a „deszkán voltunk" ugyanaz a nap.
                    "snowboard", "sifutas", "sifut", "sitalp", "sipalya",
                    "sielni", "sielt", "sielunk"),
            // A triatlon és a duatlon NEM futás: a versenytáv órákig tart, és a
            // három (két) sportág együtt más terhelés, mint bármelyik külön. A
            // saját tétele nélkül vagy elveszne, vagy hamis névvel kerülne be.
            new Kind("triatlon", "🏊", "Triatlon / duatlon", 9.0, true, 150,
                    "triatlon", "duatlon", "aquatlon", "ironman", "sprinttriatlon",
                    "olimpiai tav"),
            new Kind("fal", "🧗", "Falmászás", 8.0, false, 60,
                    "falmaszas", "falmasz", "maszas", "sziklamasz",
                    "boulder", "maszofal",
                    // A főnévi igeneves alak eddig kimaradt: az „elmentem
                    // falat mászni" nem adott mozgást, mert csak a mászás
                    // főnév volt stem, a mászni ige nem.
                    "falat masz", "falra masz", "maszoterem", "maszo terem",
                    "mentem maszni", "voltam maszni", "voltunk maszni",
                    "maszni voltam", "maszni voltunk"),
            new Kind("munka", "🌳", "Kerti / fizikai munka", 4.0, false, 60,
                    "kerti munka", "fizikai munka", "kertesz", "favag", "fat vag", "lapatolas",
                    "takarit", "funyir", "fuvet nyir", "sovenyt vag", "sovenyvag",
                    "koltoz", "asas", "kapalas", "kapal", "gereblyez",
                    "lapatol", "kertben dolgoz", "kertben melo", "astam", "asni",
                    "sepreget", "felmostam", "felmosas",
                    // A kaszálás és a cipekedés ugyanaz a fizikai munka: eddig
                    // a „kaszáltam a kertben 90 percet" és a „3 órát
                    // cipekedtem" válasz nélkül maradt.
                    "kaszal", "kaszalas", "cipeked", "cipekedes", "cipeltem",
                    "pakolas", "bepakol", "kipakol",
                    // A FÁT HORDANI és a HÁZIMUNKA is fizikai munka: a „fát
                    // hordtam be fél órát" és a „házimunka, kb 3 óra" eddig
                    // válasz nélkül maradt. A puszta „munka" ülőmunka-szó,
                    // a „házimunka" hosszabb töve viszont mozgás.
                    "fat hord", "tuzifat hord", "hazimunka", "hazimunkaz",
                    // A „metszet" szándékosan nem tő: a KERESZTmetszet nem
                    // kerti munka. A ragozott igealakok viszont igen.
                    "metszes", "metszettem", "metszeni", "fat metsz",
                    "kertepites", "kert epites", "epitkezes",
                    "ablakpucol", "porszivoz"),
            new Kind("egyeb", "🤸", "Egyéb mozgás", 6.0, false, 45,
                    // A „kardió" edzés-szó: enélkül a „45 perc kardió" semmi
                    // volt. (Az étel-oldalon ugyanez a szó a diót hozta.)
                    // A puszta „tekez" nem elég: az érTEKEZletben is benne van.
                    "kardio", "bowling", "tekepalya", "tekezes", "tekeztem", "tekezni",
                    // Az UGRÁLÁS első személyben mozgás: az „ugráltam vagy
                    // 20 percet" az ugrálóvárban is edzés. Csak a saját,
                    // múlt idejű alak – a „gyerek ugrált" nem az enyém.
                    "ugraltam", "ugraltunk",
                    // A LEJÁTSZOTT meccs is edzés: az „a meccset megnyertük
                    // 3-1-re, végig játszottam" eddig üresen jött vissza. A
                    // puszta „meccs" nem lehet tő – a tévén NÉZETT meccs nem
                    // mozgás –, ezért csak a játék igéjével együtt él.
                    "meccset jatszottam", "meccsen jatszottam",
                    "vegig jatszottam", "vegigjatszottam", "meccset nyertunk",
                    // Az óra AKTÍV IDEJE is mozgás: „az applikáció 47 perc
                    // aktív időt mért" eddig üresen jött vissza.
                    "aktiv ido", "aktiv idot", "aktiv perc",
                    // Az ERGOMÉTER gép is edzés – a KÉZI-ergométer pedig nem
                    // kézilabda: a „kéziergométer a rehab részlegen 10 perc"
                    // tízperces kézilabda-meccs lett a naplóban. A hosszabb
                    // tő nyeri az átfedést a „kezi" ellen.
                    "ergometer", "keziergometer", "kezi ergometer",
                    // A puszta FITNESZ is edzés: az „aqua fitnesz 45 perc"
                    // eddig üresen jött vissza. Az EDZŐVEL töltött óra csak a
                    // kimondott igével tő – a „beszéltem az edzővel" nem az.
                    "fitnesz", "fitness", "edzovel toltottem", "toltottem az edzovel",
                    // Termi eszközök, amik magukban is edzést jelentenek.
                    // A szabadtéri játékok is mozgás: a frizbi, a parkour és
                    // a slackline órákra viszi ki az embert a szabadba.
                    "frizbi", "ultimate frizbi", "slackline", "szlekklajn",
                    "medicinlabda", "bosu", "battle rope", "kotelezes",
                    "szankotolas", "szanko tolas", "traktorgumi", "step pad",
                    "steppad", "step ora",
                    "tekezunk", "tekezik",
                    "egyeb mozgas", "egyeb edzes", "egyeb", "sportol", "mozog",
                    "lovagl", "lovagol", "vitorlaz", "szorf", "wakeboard", "golf",

                    "ellipszis", "elliptikus", "crosstrainer", "cross trainer",
                    "jatszoter", "lepcsozo", "trambulin", "ugrokotel", "ugralokotel",
                    "ugralo kotel", "hulahopp", "kotelugras",
                    "buvarkod", "buvark", "szankoz", "parkour", "szanko",
                    // Hetvennégy mindennapi sportnévvel végigpróbálva az
                    // íjászat hiányzott. (A darts szándékosan marad kimaradva:
                    // a kocsmasportok nem edzések – erről külön teszt szól.)
                    "ijaszat", "ijasz",
                    // Az atlétika dobó- és ugrószámai: az edzés maga vegyes
                    // terhelésű, a MET a közepes sáv. Százhúsz sportnévvel
                    // végigpróbálva ezek hiányoztak.
                    "atletika", "magasugras", "tavolugras", "sulylokes",
                    "gerelyhajitas", "diszkoszvetes", "kalapacsvetes", "rudugras",
                    // A taposógép ugyanaz a gép, mint a lépcsőzőgép: kardió,
                    // nem súlyzós edzés.
                    "taposogep", "taposo gep"),
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
            // A triatlon távjának java a bringa: az olimpiai táv 51,5 km-e
            // két és fél óra körül van, ez nagyjából három perc kilométerenként.
            case "triatlon": return 3;
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
        // Ezresek: az úszástáv magyarul kimondva mindig ezres („leúsztam
        // ezerötszáz métert"), és eddig a táv némán elveszett – a bejegyzés
        // létrejött, csak épp táv nélkül.
        String[][] thousands = {{"ezer", "1000"}, {"ketezer", "2000"},
                {"haromezer", "3000"}, {"negyezer", "4000"}, {"otezer", "5000"},
                {"tizezer", "10000"}};
        for (String[] t : thousands) {
            out.add(t);
            for (String[] h : hundreds)
                out.add(new String[]{t[0] + h[0],
                        String.valueOf(Integer.parseInt(t[1]) + Integer.parseInt(h[1]))});
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
            // Az akuponk-TÚRA sem túra: az „akupunktúra kezelés a hátamra"
            // kilencven perces gyalogtúrát írt a naplóba. A manikűr-pedikűr
            // rokona, a „punktura" tő az elgépeléseket is fedi.
            "akupunktur", "punktur",
            // A BABYMEDENCE nem úszás: a „gyerekkel játszottunk a
            // babymedencében" negyvenöt perc úszást írt a szülő naplójába.
            "babymedence", "gyerekmedence", "pancsolo",
            // A FUTÓKIHÍVÁS neve nem egy futás: a haladás-jegyzet
            // („januári futókihívás: eddig 87 km") nem mai edzés.
            "futokihivas",
            // A NYÚJTÓZKODÁS az íróasztalnál nem nyújtás-edzés, a
            // SZEMTORNA pedig nem torna: mindkettő negyvenöt perces jógát
            // írt a naplóba egy-egy irodai mikroszünetből.
            "nyujtoz", "szemtorna",
            // Az immunERŐSÍTÉS közepén a kondi erősít-töve ült: a vitaminos
            // mondat mellé egy órás súlyzós edzés került.
            "immuneros",
            "tekercs", "tornacipo", "tornado", "kezitaska", "bevasarl",
            // A TEREM szótöve a hétköznapi helyiségnevekben is ott van: a
            // tárgyalóteremben töltött nap eddig hatvanperces kondi-edzés
            // lett a naplóban. (A „konditerem" és a „tornaterem" saját tő.)
            // A GYÓGYTORNÁSZ nem torna: a „gyógytornász szerint gyenge a
            // középső farizmom" panasz-mondatból eddig negyvenöt perces jóga
            // került a naplóba. (A gyógytorna MAGA marad – abból a „20 perc
            // gyógytorna" valódi mozgás.)
            "gyogytornasz",
            "targyaloterem", "targyalo", "tanterem", "eloadoterem", "varoterem",
            "szinhazterem", "konferenciaterem", "gyulesterem", "birosag",
            // A TÖRTÉNTekért közepén ott a teker.
            "tortentek",
            // A KÉZI és a JOG szótöve hétköznapi szavak elején is ott van: a
            // kézírás nem kézilabda, a jogaim nem jóga. Ötvenezer magyar szót
            // átfuttatva ezek maradtak.
            "keziras", "kezirat", "kezikonyv", "keziszer", "kezimunka", "kezifek",
            // A csirKENUggetben ott a kenu: mind a 352 ételnevet átfuttatva
            // ez az egy csinált edzést.
            "csirkenugget", "nugget",
            "jogai", "jogaim", "jogod", "joguk", "jogot", "jogok", "jogos", "jogi",
            "jogsza", "jogilag", "jogtalan", "jogert",

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
            // Az „mma" három betű, és magyar szavak közepén is ott ül:
            // dileMMA, EMMA, geMMA. Egy név vagy egy dilemma eddig harcművész
            // edzést vitt a naplóba.
            // A „csatorna" végén ott a torna, az „olvasás"-ban az ásás. Az
            // előbbi maszk kell, mert a gyógytorna és a szobatorna miatt a
            // „torna" tövet nem korlátozhatjuk szó elejére.
            "csatorna", "assistance", "importance",
            // A kézisúlyzó nem kézilabda: a „kezi" tő a nevében is ott van.
            "kezisulyzo", "kezisuly",
            // A golfKÖNYÖK és a teniszKÖNYÖK panasz, nem sportág: a „golf" és
            // a „tenisz" tő a nevükben ül. (Szósöprés találta: a „fáj a
            // golfkönyököm" golfozás, a teniszkönyök teniszezés lett volna.)
            "golfkonyok", "teniszkonyok",
            // A „részleTEKÉRT" végén a tekerés, a MEGERŐSÍTésben az erősítés,
            // a TÖLTEKEZÜNKben a tekézés, a KÖRGYŰRŰben (az app saját sávja)
            // a gyűrű.
            // A karikaGYŰRŰ sem gyúrás: a „gyűrűm", a „gyűrűt" és a „gyűrűs"
            // is a „gyur" tövön ült, és egy ékszerből hatvanperces kondi-edzés
            // lett. (Szósöprés találta.) A ragozott alakokat egyenként soroljuk
            // fel: a puszta „gyuru" a „gyúrunk"-ot is elvágná.
            "reszlet", "megerosit", "toltekez", "korgyuru",
            "gyurum", "gyurut", "gyurud", "gyuruje", "gyurus", "gyuruvel",
            "gyuruben", "gyuruk ", "gyurure",
            // Ötvenezer szavas magyar gyakorisági listával végigsöpörve.
            // A paSASban az ásás, a naGYÚRban a gyúrás, a TEREMtésben és a
            // TEREMtményben az edzőterem, az EGYÉBKÉNTben az „egyéb mozgás".
            "pasas", "nagyur", "teremt", "egyebkent", "jegyeb", "felreolvas",
            // A megÚSZTUK nem úszás, hanem szólás – és a MOZGÁSKÉPTELENSÉG
            // épp az ellenkezője a mozgásnak (a rehab piros zászlója).
            "megusztuk", "megusztak", "meguszta", "megusztad",
            "mozgaskeptelen",
    };

    /**
     * A legközelebbi mozgásforma elgépelés esetén – „futtás" → Futás.
     *
     * Ugyanaz a szigorú szabály, mint az ételeknél és a gyakorlatoknál: hat
     * betűtől, egyező szókezdettel, egy hibával (hosszú tőnél kettővel), a
     * felcserélt betűt EGY hibának számolva – a telefonon az a jellemző
     * elütés. A „nem lettem okos" üzenet így legalább tippet ad.
     *
     * @return a mozgásforma, vagy null, ha nincs elég közeli
     */
    public static Kind closestKind(String raw) {
        if (raw == null) return null;
        String q = Foods.norm(raw);
        Kind best = null;
        int bestDist = Integer.MAX_VALUE, bestLen = 0;
        for (String tok : q.split("[^a-z0-9]+")) {
            if (tok.length() < 6) continue;
            for (Kind k : ALL)
                for (String ns : k.words) {
                    if (ns.length() < 6 || ns.indexOf(' ') >= 0) continue;
                    if (!ns.regionMatches(0, tok, 0, 3)) continue;
                    int max = ns.length() >= 9 ? 2 : 1;
                    if (Math.abs(ns.length() - tok.length()) > max) continue;
                    int d = Foods.editDistance(tok, ns, max);
                    if (d <= 0 || d > max) continue;
                    if (d < bestDist || (d == bestDist && ns.length() > bestLen)) {
                        best = k; bestDist = d; bestLen = ns.length();
                    }
                }
        }
        return best;
    }

    /** Maszkolandó-e a szó – igekötővel együtt is. */
    private static boolean maskedWord(String tok) {
        if (startsWithNotSport(tok)) return true;
        for (String v : Foods.VERB_PREFIX)
            if (tok.length() > v.length() + 2 && tok.startsWith(v)
                    && startsWithNotSport(tok.substring(v.length()))) return true;
        return false;
    }

    private static boolean startsWithNotSport(String tok) {
        for (String bad : NOT_SPORT) if (tok.startsWith(bad)) return true;
        return false;
    }

    /**
     * A lépcső a PANASZ helyszíne, nem edzés.
     *
     * A gyógytornász első kérdése az, hogy MIKOR fáj – és a térdre a válasz
     * majdnem mindig az, hogy „lépcsőn lefelé". A lépcsőzés viszont mozgásforma
     * is, így a „lépcsőn lefelé fájdul a térdem" mondatból eddig egy
     * MÁSFÉLÓRÁS TÚRA került a naplóba, olyan napra, amikor a panasz miatt épp
     * hogy nem mozgott az ember.
     *
     * Csak akkor takarunk, ha a mondat panasz, és nincs benne se szám, se
     * emelet: a „20 perc lépcsőzés, közben fájt a térdem" megtörtént edzés,
     * annak a húsz perce marad.
     */
    private static void maskSymptomStairs(char[] q) {
        String s = new String(q);
        if (!s.contains("lepcso")) return;
        if (Rehab.forComplaint(s) == null) return;
        if (s.contains("emelet")) return;
        for (int i = 0; i < s.length(); i++) if (Character.isDigit(s.charAt(i))) return;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("lepcso\\p{L}*").matcher(s);
        while (m.find()) blank(q, m.start(), m.end());
    }

    /**
     * A NÉVELŐS „a futás után" időpont, nem edzés.
     *
     * A „jégfürdő 5 perc a futás után" öt perce a jégfürdőé – mégis egy
     * ötperces FUTÁS került a naplóba, a valódi jégfürdő helyett. A névelő
     * a döntő: a „30 perc futás után fájt a térdem" mondatban nincs névelő,
     * ott a harminc perc a futásé, és a bejegyzés marad.
     */
    private static void maskSportTimeReference(char[] q) {
        String s = new String(q);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![a-z])az? ([a-z]+)\\s+utan(?![a-z])").matcher(s);
        while (m.find())
            if (kindWordIn(s, m.start(1), m.end(1)))
                blank(q, m.start(1), m.end(1));
    }

    /** A sportág-felismerés elől elrejtett szavak kimaszkolása. */
    private static void maskNotSport(char[] q) {
        String s = new String(q);
        int i = 0;
        while (i < s.length()) {
            if (!Character.isLetter(s.charAt(i))) { i++; continue; }
            int j = i;
            while (j < s.length() && Character.isLetter(s.charAt(j))) j++;
            String tok = s.substring(i, j);
            // Igekötővel együtt is: a „beolvasás" ugyanaz a szó, mint az
            // „olvasás" – prefix-egyezéssel viszont átcsúszott, és kerti
            // ásásként került a naplóba.
            if (maskedWord(tok)) blank(q, i, j);
            i = j;
        }
    }

    /**
     * Rövidítések feloldása: „10k lépés", „10 000 lépés", „10k futás".
     *
     * A „k" ezerre rövidít, de nem mindig ugyanazt jelenti: lépésnél tízezer
     * LÉPÉS, futásnál tíz KILOMÉTER. A szóközös ezres tagolás („10 000") pedig
     * egyszerűen két számnak látszott, és a mondat mindkettőt eldobta.
     */
    private static String shortForms(String s) {
        // A PULZUSZÓNA száma nem darabszám: a „zóna 2 futás 40 perc" KÉT
        // futássá vált. A zóna sorszáma a terhelést nevezi meg, nem az
        // alkalmakat.
        s = s.replaceAll("(?<![a-z])zona\\s?[1-5](?![0-9])", "zona");
        s = s.replaceAll("(?<![0-9])[1-5]\\s?-?[eo]s\\s+zona", "zona");
        s = s.replaceAll("(?<![a-z0-9])z[1-5](?![0-9a-z])", "zona");
        // A KÖRHOSSZ szorozva a körszámmal: az „500 m-es köröket futottam,
        // összesen 8-at" fél kilométeres futás lett NÉGY helyett – a
        // körhossz bement távnak, a nyolc kör elveszett mellőle.
        java.util.regex.Matcher lap = java.util.regex.Pattern
                .compile("(\\d{2,4})\\s?(?:m|meter)-?es\\s+kor\\w*"
                        + "[^0-9]{0,30}?(\\d{1,2})(?!\\d)").matcher(s);
        if (lap.find()) {
            int total = Integer.parseInt(lap.group(1)) * Integer.parseInt(lap.group(2));
            if (total >= 200 && total <= 100000)
                s = s.substring(0, lap.start()) + total + " m "
                        + s.substring(lap.end());
        }
        // A ZÁRÓ KÍSÉRŐ nem külön edzés: a „nyújtással zártam a 45 perces
        // futást" nyújtása lemásolta a futás negyvenöt percét, és két
        // bejegyzés lett egy edzésből.
        s = s.replaceAll("(?<![a-z])(nyujtas|seta|kocogas|levezetes)\\w*l"
                + "\\s+zartam", "zartam");
        // A MÉLYSÉG métere nem megtett táv: a „leereszkedtünk a barlangba
        // 60 m mélyre" hatvan méteres futás lett.
        s = s.replaceAll("(?<![\\d,.])\\d{1,4}\\s?m\\s+mely\\w*", "");
        // A JÖVŐ ÉVSZAK versenye nem lefutott táv: az „ősszel maraton, most
        // építem az alapozást" negyvenkét kilométeres MAI futást írt be. A
        // versenyig hátralévő „10 hét" pedig nem tíz hetes időszak.
        s = s.replaceAll("(?<![a-z])(osszel|tavasszal|jovore|jovo evben)"
                + "\\s+(fel)?maraton\\w*", "");
        s = s.replaceAll("(?<=versenyre |versenyig )\\s?(meg )?\\d{1,2}\\s?het\\w*", "");
        // A TÁVIRATI „kardió 30, súlyzó 40" csupasz száma perc: az idő-alapú
        // sport neve utáni kis szám nem lehet más. A táv-alapú sportnál
        // (futás 10) nem merünk dönteni – az lehet km is.
        s = s.replaceAll("(?<![a-z])(kardio|sulyzo|kondi|joga|nyujtas|pilates"
                + "|hiit|edzes|gyuras)\\s+(\\d{1,3})"
                + "(?!\\d)(?![.,]\\d)(?![:%-])(?!\\s?(?:perc|ora|km|kg|kcal|mp|lepes|x|kor"
                + "|es(?![a-z])|as(?![a-z])|os(?![a-z])|m(?![a-z])|h(?![a-z])"
                + "|p(?![a-z])))", "$1 $2 perc");
        // Az „EDDIG … A 100-BÓL" halmozott összeg, nem mai edzés: a
        // „januári futókihívás: eddig 87 km a 100-ból" nyolcvanhét
        // kilométeres MAI futást írt be – egy hónap összegéből. A -ból
        // ragos cél mellett az eddig-összeg kiesik.
        if (s.matches(".*(?<![a-z])eddig(?![a-z]).*\\d\\s?-?b[o]l.*"))
            s = s.replaceAll("(?<![a-z])eddig[^,;.]*", "");
        // A „24 ÓRÁS" terem a nyitvatartás, nem az edzés hossza: „az
        // edzőterem 24 órás, éjfélkor mentem, 40 perc" ezernégyszáznegyven
        // perces kondi lett. A 24 órás futóverseny marad: ott nincs
        // terem-szó a mondatban.
        if (s.matches(".*(terem|nyitva|nonstop|non-stop|non stop).*"))
            s = s.replaceAll("(?<![\\d,.])24\\s?oras(?![a-z])", "");
        // A „TERVEZTEM, VÉGÜL" mondat vége a valóság: a „20 percet
        // terveztem, végül 45 lett" negyvenöt perce megtörtént – a tervezés
        // szava eddig az egészet elvitte. A tervezett szám kiesik, a végül
        // utáni marad.
        s = s.replaceAll("(?<![\\d.,])\\d{1,3}\\s?perc\\w*([^0-9,;.]{0,20}?)"
                + "tervez\\w+,?\\s*(?:de\\s+)?vegul\\s+(\\d{1,3})"
                + "(?:\\s?perc\\w*)?\\s+lett", "$2 perc$1lett");
        // A SZORZÓSZÁM utáni „is" csak nyomaték: a „kétszer is voltam
        // úszni" két úszás – az „is" eddig elvágta a számot a mozgástól,
        // és egy alkalom lett belőle.
        s = s.replaceAll("(szer|szor)\\s+is(?![a-z])", "$1");
        // A „JÓ KIS KARDIÓ" értékelő megjegyzés, nem második edzés: a
        // „sífutógép 25 perc, jó kis kardió" mellé egy 45 perces „egyéb
        // mozgás" került – ugyanarról a huszonöt percről.
        s = s.replaceAll("(?<![a-z])jo (?:kis )?(kardio|edzes|mozgas)\\w*", "");
        // A FUTÓPADON sétálás EGY séta: a „futópad 5% emelkedőn 40 perc
        // séta" a futópad tövéről egy 45 perces futást IS kapott a séta
        // mellé. Ha ugyanabban a tagmondatban ott a séta szava, a futópad
        // csak a helyszín.
        s = s.replaceAll("futopad\\w*(?=[^,;.]*(?<![a-z])seta)", "");
        // A „FÉL-FÉL ÓRA" az oda-vissza út két fele: a „sétáltunk a piacig
        // és vissza, fél-fél óra" hatvan perc együtt – eddig csak az egyik
        // fél került be. A számnév-fordítás után „0,5-0,5 ora" alakban áll.
        if (s.contains("vissza") || s.contains("oda"))
            s = s.replaceAll("(?<![\\d,])0,5\\s?-\\s?0,5\\s?ora", "1 ora")
                 .replaceAll("(?<![a-z])fel-fel\\s?ora", "1 ora");
        // A „KAJAK" a beszélt nyelvben nyomatékosító szó („kajak
        // kifárasztott" = nagyon), nem csónak: mellette múlt idejű igével
        // eddig fél óra evezés került a naplóba. A ragozott alakok
        // (kajakoztam, kajakkal) és a szám melletti kajak a vízé marad.
        s = s.replaceAll("(?<![a-z])kajak(?:ra)?\\s+(?=(?:[a-z]{3,}"
                + "(?:ott|ett|tam|tem|tunk|tak|tek)|jo|kemeny|durva|brutal"
                + "|nagyon|full)(?![a-z]))", "");
        // A „30-30 INTERVALL" munka-pihenő pár, nem harminc nap és nem
        // harminc alkalom: a számpár az időzítőé, a bejegyzésből ki kell
        // takarni. A „10x" utótag a körök száma – azt az intervall-elemző
        // olvassa, itt szintén nem darabszám.
        if (s.matches(".*(?<![a-z])(interval|intervall)\\w*.*")) {
            s = s.replaceAll("(?<![\\d.,])(\\d{1,3})\\s?-\\s?(\\d{1,3})"
                    + "(?=\\s?(?:mp|perc)?\\s?interval)", "");
            s = s.replaceAll("(?<![\\dx(])(\\d{1,2})\\s?x(?![\\da-z])", "");
        }
        // A SÉTÁLÓ PIHENŐ az intervall része, nem külön séta: a „4x800 m
        // 2 perc sétáló pihenővel" mellé egy kilencvenperces túra került.
        s = s.replaceAll("(?<![a-z])(setalo|kocogo)\\s?(piheno\\w*|szunet\\w*)", "$2");
        // A LÉPTEM ige is lépésszám: a „léptem vagy 14 ezret a
        // városnézésen" és a „léptem 14000-et" eddig üresen jött vissza,
        // mert a lépés-szó főnévi alakja hiányzott mellőle. Az igekötős
        // „beléptem" és „átléptem" a szóhatár miatt nem esik ide.
        s = s.replaceAll("(?<![a-z])leptem\\s+(?:vagy\\s+|kb\\s+)?(\\d{1,2})\\s?ezret",
                "$1 ezer lepest");
        s = s.replaceAll("(?<![a-z])leptem\\s+(?:vagy\\s+|kb\\s+)?(\\d{4,6})(?:-e?t)?",
                "$1 lepest");
        // A „KÖRÜL MOZOG" ingadozást jelent, nem mozgást: a „pihenőpulzus
        // 55 körül mozog" mondatból egy negyvenöt perces „egyéb mozgás"
        // lett – egy pulzus-leolvasásból.
        s = s.replaceAll("(?<![a-z])korul mozg\\w*", "korul");
        s = s.replaceAll("(?<![a-z])korul mozog\\w*", "korul");
        // A KÖRSZÁM a pálya mellett nem alkalomszám: a „leraktam 10 kört a
        // pályán" TÍZ futás-bejegyzéssé vált, mert a tíz darabszámnak
        // látszott. A szám a körök száma egy edzésen belül – kitakarjuk, a
        // „kört a pályán" szókapcsolat maga mondja ki a futást.
        s = s.replaceAll("(?<![\\d,.])\\d{1,3}\\s?(?=kor(?:t|oket)? a palyan)", "");
        // A TERVEZETT HELYETT a valódi: „a tervezett 10 km helyett csak 6
        // lett" hatosa a megtett táv – eddig az egész mondat elveszett, mert
        // a hat mellett nem állt mértékegység, a tíz meg terv volt. A magyar
        // helyesbítés mintája ez is: előbb a nem-igaz, aztán az igaz.
        s = s.replaceAll("(?i)tervezett\\s+\\d{1,3}(?:[.,]\\d)?\\s*"
                + "(km|kilometer|perc|ora)\\w*\\s+helyett\\s+(?:csak\\s+)?"
                + "(\\d{1,3}(?:[.,]\\d)?)(?!\\s*(?:km|kilometer|perc|ora))",
                "$2 $1");
        // Az ODA-VISSZA két fele ugyanaz az út: a „munkába biciklivel: oda
        // 25, vissza 28 perc" eddig csak a vissza-időt kapta meg – a napi
        // ingázás fele elveszett. A két szám összege az egy bejegyzés.
        java.util.regex.Matcher ov = java.util.regex.Pattern
                .compile("oda\\s?(\\d{1,3})\\s?(?:perc)?\\s?[,;]?\\s?(?:es\\s)?"
                        + "vissza\\s?(\\d{1,3})\\s?perc").matcher(s);
        if (ov.find()) {
            int sum = Integer.parseInt(ov.group(1)) + Integer.parseInt(ov.group(2));
            if (sum >= 2 && sum <= 24 * 60)
                s = s.substring(0, ov.start()) + sum + " perc" + s.substring(ov.end());
        }
        // Szóközzel tagolt ezres: „10 000" → „10000". A KETTŐSPONT megvédi az
        // óraállást: a „túra 14,8 km 3:45:00 620 m emelkedés" mondatban a
        // „00 620" ezres tagolásnak látszott, és a „3:45:00620"-ból már nem
        // lett időtartam – a kimondott három és háromnegyed óra helyére a
        // tempóból becsült százhetvennyolc perc lépett.
        s = s.replaceAll("(?<![\\d.,:])(\\d{1,3})\\s(\\d{3})(?![\\d.,])", "$1$2");
        // A MÉRTÉKEGYSÉG egyszer van kimondva, a szám kétszer: a „két edzés
        // ma, 45 és 60 perc" negyvenöte és a „reggel és délután is futottam,
        // 5 és 7 km" ötöse eddig némán elveszett – az egyik alkalom teljesen
        // hiányzott a naplóból. A magyar így sorol: az egység a végén áll,
        // és mindkét számra vonatkozik. A törtre nem él (a „3 és fél óra"
        // egyetlen időtartam), ezért a második szám nem kezdődhet nullával.
        s = s.replaceAll("(?<![\\d.,])(\\d{1,3}(?:[.,]\\d)?)\\s+es\\s+"
                + "(?!0[.,])(\\d{1,3}(?:[.,]\\d)?)\\s*"
                + "(perc|percet|percig|ora|orat|oraig|km|kilometer|kilometert)"
                + "(?![a-z])", "$1 $3 es $2 $3");
        // Az „N alkalommal" ugyanaz, mint az „N-szor". A szám ELŐL állva
        // eddig is darabszám volt („a héten 3 alkalommal futottam"), a
        // mozgás MÖGÉ kerülve viszont elveszett: az „a héten futottam 3
        // alkalommal" egyetlen futásként került be, vagyis a hét
        // kétharmada eltűnt a naplóból.
        s = s.replaceAll("(?<![\\d.,])(\\d{1,2})\\s?alkalo?m(?:mal|at)(?![a-z])", "$1-szor");
        // A kiírt számnév ÓRÁS összetételben: a „kétórás túra" két óra, nem a
        // túra alapértelmezett kilencven perce. A számnév-szótár szóhatárt
        // vár, így az összetételt nem látta.
        for (String[] w : new String[][]{{"felora", "0,5 ora"}, {"ketora", "2 ora"},
                {"haromora", "3 ora"}, {"negyora", "4 ora"}, {"otora", "5 ora"},
                {"hatora", "6 ora"}, {"nyolcora", "8 ora"}})
            s = s.replaceAll("(?<![a-z])" + w[0] + "(s|st|sat|sra|ban|n|t)?(?![a-z])", w[1]);
        boolean steps = s.contains("lepes") || s.contains("lepest") || s.contains("lepett");
        return s.replaceAll("(?<![\\d.,])(\\d{1,3})\\s?k(?![a-z0-9])",
                steps ? "$1000" : "$1 km");
    }

    /**
     * Ezek a szavak a „nap"/„hét" szótövet tartalmazzák, de nem időszakot
     * jelentenek. Nélkülük a „hétfőn futottam" egy hetes időszaknak látszana.
     */
    private static final String[] NOT_SPAN = {
            "napi", "naplo", "naploban", "naplot", "naptar", "napozas", "napsutes",
            "hetfo", "hetfon", "hetfoi", "hetvege", "hetvegen", "hetkoznap", "hetkoznapon",
            // A „hétvégi" JELZŐ, nem egyhetes időszak: a „hétvégi hosszú
            // futás 18 km" tizennyolc kilométere hét napra terült szét, és a
            // heti statisztikában hétszer annyi napnak látszott.
            "hetvegi", "hetvegere", "hetvegeig", "hetvegehez",
            // A „hetes" sorszám vagy jelző, nem időszak: a „hetes bérlettel
            // kondi" és a „futás a hetes buszmegállóig" egyaránt EGY napról
            // szól, eddig viszont mindkettő egyhetes időszakra terült szét.
            "hetes", "hetesben", "hetessel", "hetedik", "hetediken", "hetedike",
            // A hétfő RAGOS alakjai is napnevek, nem hetek: a „hétfőtől
            // péntekig futottam" egyhetes IDŐSZAKKÁ vált, egyetlen futással –
            // pedig öt napot nevez meg.
            "hetfotol", "hetfoig", "hetfore", "hetfoje", "hetfotol pentekig",
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

    /**
     * Csak a KIMONDOTT távot (vagy lépésszámot) tartalmazó tételek.
     *
     * A vegyes mondat („reggel 5 km futás, utána 20 fekvőtámasz") két naplóba
     * való: a sorozat az erősítőbe, a kilométer az előzményekbe. Ha mindkettőt
     * elmentjük, a fekvőtámaszból becsült „kondi" bejegyzés kétszer számítana –
     * egyszer sorozatként, egyszer mozgásként. Ez a szűrő azt hagyja meg, amit
     * az erősítő napló nem tud tárolni: a távot.
     *
     * @return üres Parsed, ha nincs ilyen tétel (null soha)
     */
    public static Parsed cardioOnly(Parsed p) {
        List<Plan> keep = new ArrayList<>();
        List<Integer> days = new ArrayList<>();
        if (p != null) {
            for (int i = 0; i < p.plans.size(); i++) {
                Plan pl = p.plans.get(i);
                if (pl.km <= 0 && pl.steps <= 0) continue;
                keep.add(pl);
                // A megnevezett napok alkalmanként állnak: ha tételt dobunk,
                // a hozzá tartozó napot is dobni kell, különben elcsúszik.
                if (p.exactDays != null && i < p.exactDays.length)
                    days.add(p.exactDays[i]);
            }
        }
        int[] ex = null;
        if (p != null && p.exactDays != null && days.size() == keep.size()
                && !days.isEmpty()) {
            ex = new int[days.size()];
            for (int i = 0; i < ex.length; i++) ex[i] = days.get(i);
        }
        return p == null ? new Parsed(keep, 1, 0)
                : new Parsed(keep, p.days, p.offset, p.hour, ex);
    }

    /** Tesztelhető változat: a „most" kívülről jön (a hétköznapnevekhez kell). */
    static Parsed parse(String text, long now) {
        List<Plan> out = new ArrayList<>();
        if (text == null) return new Parsed(out, 1, 0, 12);
        // A LISTA sorszáma nem darabszám: az „1. 5 km futás / 2. 30 perc
        // kondi" kettese a felsorolás második pontja, és eddig KÉT
        // kondi-edzés lett belőle. A sor eleje csak a normalizálás ELŐTT
        // látszik – az egymás utáni szóközöket (és a sortörést) a norm()
        // egyetlen szóközzé vonja össze.
        char[] q = shortForms(Hu.correction(Foods.norm(maskListMarkers(text)))).toCharArray();
        // A kiírt számnév-pár is tartomány: „húsz-huszonöt perc kondi". A
        // nyers alak ELŐTT fut, mert az osztó pár felismerése („öt-öt km")
        // is számjegyet keres.
        wordRangeToDigits(q);
        // A nyers, még semmilyen kimaszkolás előtti alak. Az osztó számpár
        // („2-2 óra") felismeréséhez kell: mire a mozgásokhoz érünk, a pár
        // egyik tagja már kifehérítve áll a munkapéldányban – így csak a
        // maradék szám látszik, az osztó jelentés nem.
        final String rawText = new String(q);
        // A KÉRDÉS nem napló: a „terhes vagyok, milyen mozgás ajánlott?"
        // mozgás szavából eddig negyvenöt perces „egyéb mozgás" került a
        // naplóba – egy olyan mondatból, ami épp azt kérdezi, hogy mit
        // lehetne csinálni. Szám nélküli kérdésben nincs mit felvenni; a
        // számmal írt „30 perc futás után fájt a térdem, mit tegyek?" viszont
        // megtörtént edzés is.
        if (text != null && text.indexOf('?') >= 0) {
            boolean digit = false;
            for (int i = 0; i < rawText.length(); i++)
                if (Character.isDigit(rawText.charAt(i))) { digit = true; break; }
            if (!digit) return new Parsed(out, 1, 0, 12);
        }
        // A jövő nem napló: a „jövő héten 3 futás" vagy a „holnap futok"
        // terv, nem megtörtént edzés – ezekből semmit sem mentünk, különben
        // a szándék máris bekerülne a szériába és az XP-be.
        // A SZOKÁS tagmondata nem viheti el a mellette álló valódi edzést: a
        // „szoktam futni, ma 8 km-t futottam" nyolc kilométere eddig
        // nyomtalanul eltűnt, mert a szokás-szabály az EGÉSZ mondatra élt.
        stripHabitClause(q);
        if (looksLikeFuture(new String(q))) return new Parsed(out, 1, 0, 12);
        // Az ÉVES-HAVI ÖSSZEGZŐ nem egy edzés: az „összesen 1250 km futás
        // idén", „a Garmin évi összesítője: 210 edzés" és a „legjobb
        // hónapom volt: 160 km" számai hónapok összegei – mégis egy-egy
        // mai (vagy ötven!) bejegyzés lett belőlük.
        String sm = new String(q);
        if (sm.contains("osszesito")
                || sm.matches(".*(?<![a-z])(iden|tavaly|szezonban|a szezon)"
                        + "(?![a-z]).*(?<![a-z])osszesen(?![a-z]).*")
                || sm.matches(".*(?<![a-z])osszesen(?![a-z]).*"
                        + "(?<![a-z])(iden|tavaly|szezonban)(?![a-z]).*")
                || sm.matches(".*legjobb (honap|het)\\w*.*"))
            return new Parsed(out, 1, 0, 12);
        // Hétköznapi szavak, amikben egy rövid sportág-szótő lakik: a kultúra
        // nem túra, a tekercs nem kerékpár. Mindenki más előtt kitakarva.
        maskSymptomStairs(q);
        maskSportTimeReference(q);
        maskNotSport(q);
        // A „hát" nem hat: a „felső hát erősítés" hat darab hatvanperces
        // kondi-bejegyzés lett hat napra elosztva. Ékezet nélkül a testtáj és
        // a számnév egybeesik, a jelző viszont eldönti.
        maskBackNoun(q);
        // Ami nem történt meg, az nem kerül a naplóba: a „nem futottam", a
        // „kihagytam", az „elmaradt" és az „X helyett" edzése kitakarva.
        stripNegated(q);
        // A „kétszer", „3-szor" alakból szám lesz, mielőtt bármi más olvasná.
        java.util.List<int[]> mults = stripMultiplicative(q);
        // Az óra-tartomány időtartammá válik: „18:00-19:30 foci" másfél óra.
        mergeClockRange(q);
        // A lépcsőzés emeletben mérhető: „20 emeletet lépcsőztem" tíz perc.
        mergeFloors(q);
        // A „6x1 km" intervall-jelölés össztávvá válik, még a táv-olvasó előtt.
        mergeIntervalDistances(q);
        // Az úszók hosszban mérnek: „40 hosszt úsztam" ezer méter.
        mergePoolLengths(q);
        // Gyakoriság („hetente kétszer", „kéthetente", „másnaponta"): a
        // periódus hossza napokban – az időszak-kereső előtt vesszük ki, hogy
        // a „hetente" ne váljon egyhetes időszakká a „hónapban" helyett.
        int freq = stripFrequency(q);

        // 1) Időszak: „elmúlt 3 nap”, „3 nap alatt”, „a héten”. A megtalált részt
        //    kitakarjuk, hogy a benne lévő szám ne számítson edzés-darabszámnak.
        int days = 1, offset = 0;
        java.util.List<int[]> wdBacks = null;
        // „Vasárnap KIVÉTELÉVEL minden nap": a megnevezett nap itt épp az,
        // amelyiken NEM volt edzés. A napnevet ilyenkor kitakarjuk, hogy ne
        // arra a napra kerüljön a bejegyzés – a kizárt nap kibontását nem
        // vállaljuk, de rossz napot írni rosszabb, mint nem tudni a napot.
        {
            String pre = new String(q);
            if (pre.contains("kivetel") || pre.contains("kiveve"))
                for (int[] w : findWeekdays(q, now)) blank(q, w[0], w[1]);
        }
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
                        // „Hétfőtől péntekig futottam": a kettő között MINDEN
                        // nap benne van, nem csak a két megnevezett. Eddig két
                        // futás lett belőle öt helyett.
                        if (wds.size() == 2 && rawText.matches(".*\\b\\w+tol\\b.*\\b\\w+ig\\b.*")) {
                            int b1 = wds.get(0)[2], b2 = wds.get(1)[2];
                            int lo = Math.min(b1, b2), hi = Math.max(b1, b2);
                            if (hi - lo >= 1 && hi - lo <= 6) {
                                java.util.List<int[]> all = new java.util.ArrayList<>();
                                for (int b = hi; b >= lo; b--) all.add(new int[]{0, 0, b});
                                wdBacks = all;
                            }
                        }
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
        if (st == null) st = findStepsAfter(q);
        if (st == null) st = findStepsByGoal(q);
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
        List<int[]> mins = findMinutes(q, beforeBlank); // {pos, perc}
        for (int[] m : mins) blank(q, m[0], m[2]);
        mergeTimeRanges(beforeBlank, mins, q);
        dropWarmupTimes(beforeBlank, mins);
        dropSleepTimes(beforeBlank, mins);
        dropTotalTime(beforeBlank, mins);

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
                    if (p > 0 && Character.isLetter(s.charAt(p - 1)) && isFragileStem(w)) {
                        int a = p;
                        while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
                        if (!isVerbPrefix(s.substring(a, p))) continue;
                    }
                    // A „sífutottam” nem futás: a sífutás MET-je a síé (6,0),
                    // nem a futásé (9,8) – másfélszeres kalóriát írnánk.
                    if (p >= 2 && w.startsWith("fut") && s.startsWith("si", p - 2)
                            && (p == 2 || !Character.isLetter(s.charAt(p - 3))))
                        continue;
                    // Az „ÚSZÁS NÉLKÜL" tagadás: a sport-tő utáni „nélkül"
                    // kizárja a bejegyzést – a „csak lubickoltam, úszás
                    // nélkül" nem negyvenöt perc úszás.
                    int wEnd = p + w.length();
                    while (wEnd < s.length() && Character.isLetter(s.charAt(wEnd))) wEnd++;
                    if (s.startsWith(" nelkul", wEnd)) continue;
                    // A „terem” az ÉTterem és a MŰterem belsejében nem kondi
                    // (az edzőterem, gépterem viszont igen).
                    if (w.equals("terem") && p >= 2
                            && (s.startsWith("et", p - 2) || s.startsWith("mu", p - 2)
                                || s.startsWith("disz", p - 4)
                                || (p >= 3 && s.startsWith("tan", p - 3))))
                        continue;
                    // A „futás UTÁN" nem futás, hanem IDŐPONT: a „futás után
                    // turmix" mondat a turmixról szól, mégis negyvenöt perc
                    // futás lett belőle – kitalált edzés, ami a szériába, az
                    // XP-be és a heti percbe is beszámított, ráadásul az
                    // étkezés elé állt az útbaigazítóban.
                    //
                    // Csak akkor élhet, ha a mondatban SEMMILYEN időtartam,
                    // táv vagy lépésszám nincs: a „60 perc futás után ittam"
                    // futása valódi, azt a kimondott szám hitelesíti.
                    if (mins.isEmpty() && kms.isEmpty() && steps <= 0
                            && timePhraseAfter(s, p + w.length())) continue;
                    hits.add(new int[]{p, w.length(), ki});
                }
            }
        }
        // A hosszabb találatba eső rövidebbet eldobjuk („kézi” a „kézilabda”-ban).
        //
        // Az ÁTFEDÉS is ide tartozik, nem csak a teljes tartalmazás: a
        // „hegyMÁSZÁS" elején a hegymászás (túra), a végén a mászás
        // (falmászás) – a két tő ugyanazokon a betűkön osztozik, mégsem esik
        // egyik a másikba. Így a mondatból KÉT edzés lett: négy óra falmászás
        // ÉS másfél óra túra, ugyanabból a szóból.
        List<int[]> keep = new ArrayList<>();
        for (int[] h : hits) {
            boolean covered = false;
            for (int[] o : hits) {
                if (o == h) continue;
                boolean overlap = o[0] < h[0] + h[1] && h[0] < o[0] + o[1];
                if (!overlap) continue;
                // A hosszabb tő nyer; egyenlő hossznál a korábbi.
                if (o[1] > h[1] || (o[1] == h[1] && o[0] < h[0])) { covered = true; break; }
            }
            if (!covered) keep.add(h);
        }
        sortByPos(keep);
        // Az EGY SZÓBA eső azonos mozgásformájú tövek egy találat: a
        // „gyalogtúrán" elején a gyalog, a közepén a túra – mindkettő a
        // túráé, mégis KÉT túra lett belőle, és a szintemelkedés métere is
        // kapott egy sajátot. Két különböző sport egy szóban maradhat
        // (sífutás), az ugyanaz kétszer nem.
        List<int[]> uniq = new ArrayList<>();
        for (int[] h : keep) {
            boolean dup = false;
            for (int[] o : uniq) {
                if (o[2] != h[2]) continue;
                boolean sameWord = true;
                for (int i = o[0] + o[1]; i < h[0]; i++)
                    if (!Character.isLetter(s.charAt(i))) { sameWord = false; break; }
                if (sameWord && h[0] - (o[0] + o[1]) <= 2) { dup = true; break; }
            }
            if (!dup) uniq.add(h);
        }
        keep = uniq;
        // A JELZŐS osztálynév EGY edzés: az „alakformáló torna 50 perc"
        // elejéből tánc, a végéből jóga lett – két bejegyzés egyetlen
        // óráról. Magyarul a fej-szó áll hátul („alakformáló TORNA"),
        // ezért ha két KÜLÖNBÖZŐ sport-tő közvetlenül egymás mellett áll
        // (csak szóköz vagy kötőjel van köztük), az első a jelző, és
        // kiesik. Kivétel az általános fej-szó: a „box EDZÉS" edzése csak
        // annyit mond, hogy edzés volt – ott a konkrét sport nyer, és a
        // hátsó, „egyéb" tő esik ki. A vesszős felsorolás („futás, úszás")
        // két külön edzés marad, ahogy az egy szóba írt sífutás is.
        List<int[]> heads = new ArrayList<>();
        for (int[] h : keep) {
            boolean jelzo = false;
            for (int[] o : keep) {
                if (o == h || o[2] == h[2]) continue;
                boolean hFirst = h[0] + h[1] < o[0];
                int from = hFirst ? h[0] + h[1] : o[0] + o[1];
                int to = hFirst ? o[0] : h[0];
                int gap = to - from;
                if (gap < 1 || gap > 2) continue;
                boolean sep = true;
                for (int i = from; i < to; i++)
                    if (s.charAt(i) != ' ' && s.charAt(i) != '-') { sep = false; break; }
                if (!sep) continue;
                boolean laterGeneric = ALL[hFirst ? o[2] : h[2]].id.equals("egyeb");
                if (laterGeneric ? !hFirst : hFirst) { jelzo = true; break; }
            }
            if (!jelzo) heads.add(h);
        }
        keep = heads;

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
            // A KÖREDZÉS hossza az EGÉSZ körből jön, és annyiszor, ahány kör
            // van. A „körkörös edzés: 4 kör, 10 fekvőtámasz, 15 guggolás,
            // 20 hasizom" eddig öt percet kapott – az első szám ötöde –,
            // pedig ez négyszer negyvenöt ismétlés, jó fél óra munka.
            int estReps = reps;
            if (minutes <= 0 && reps > 0) {
                int total = 0;
                boolean expanded = false;
                for (StrengthParse.Item it : StrengthParse.parse(rawText)) {
                    total += it.totalReps();
                    if (it.sets.size() > 1) expanded = true;
                }
                if (total > estReps) estReps = total;
                // A körszámmal csak akkor szorzunk, ha az erő-felismerő még
                // NEM tette bele: a „3 kör: 20 guggolás" sorozatai ott már
                // háromszor szerepelnek, itt megszorozni kilencszeres edzés
                // lenne.
                if (!expanded) estReps *= roundsSaid(beforeBlank);
            }
            if (minutes <= 0)
                // Nincs kimondott időtartam: távból vagy ismétlésből becsülünk,
                // anélkül a mozgásforma szokásos hossza jön.
                minutes = reps > 0
                        ? Math.max(5, Math.min(60, estReps / 5))
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
            } else if (mins.size() == 1 && distributiveBefore(rawText, mins.get(0))) {
                // „reggel és este is futottam 20-20 percet": az osztó alak
                // ALKALMANKÉNT húsz percet jelent, nem összesen annyit.
                out.set(0, new Plan(p.kind, 2, mins.get(0)[1], p.km));
            } else if (mins.isEmpty() && kms.size() == 1 && p.km > 0
                    && distributiveBefore(rawText, new int[]{(int) kms.get(0)[0]})) {
                // Ugyanez TÁVVAL: a „reggel és este is futottam 5-5 km-t" két
                // ötkilométeres futás. Az osztó alakot eddig csak az
                // időtartamnál értettük, a távnál nem – a napi tíz kilométer
                // fele eltűnt a naplóból, a statisztikából és az XP-ből.
                out.set(0, new Plan(p.kind, 2, p.minutes, p.km));
            }
        }

        // Osztó időtartam TÖBB mozgásformára: „futás és úszás 30-30 perc".
        //
        // Az osztó alak alkalmanként értendő, és itt az „alkalom" a két
        // különböző mozgás. Eddig csak a hozzá közelebbi kapta meg a harminc
        // percet, a másik a szokásos hosszával került be – a „futás és úszás
        // 30-30 perc" futása negyvenöt perc lett, mert annyi a futás alapja.
        if (out.size() >= 2 && mins.size() == 1
                && distributiveBefore(rawText, mins.get(0))) {
            int each = mins.get(0)[1];
            if (each >= 1 && each <= 24 * 60)
                for (int i = 0; i < out.size(); i++) {
                    Plan p = out.get(i);
                    if (p.count == 1) out.set(i, new Plan(p.kind, 1, each, p.km));
                }
        }

        // Ha nincs felismert mozgás, de van táv, az futás: a „nyomtam egy
        // 5 km-t" magyarul futást jelent.
        if (out.isEmpty() && !kms.isEmpty()) {
            Kind run = byId("futas");
            double km0 = kms.get(0)[1];
            // A kimondott idő itt is erősebb a tempó-becslésnél: az „5 km
            // 22:30" fél percre pontos, a hat perc/km csak közelítés.
            int est = Math.min(24 * 60,
                    Math.max(1, (int) Math.round(km0 * pace(beforeBlank, run))));
            int said = minutesFor(mins, (int) kms.get(0)[0], (int) kms.get(0)[2],
                    -1, Integer.MAX_VALUE, 0);
            out.add(new Plan(run, 1, said > 0 ? said : est, km0));
        }

        // A TÖBBNAPOS pótlás: „tegnapelőtt 5 km, tegnap 8 km, ma 3 km".
        //
        // Egy mozgásforma egyszer szerepel a listában, tehát a második és a
        // harmadik táv gazdátlanul maradt – és némán el is veszett. Aki
        // hétvégén pótolja a hét futásait, annak eddig a nyolc és a három
        // kilométere sehol nem jelent meg: sem a naplóban, sem a heti
        // összegben, sem az XP-ben.
        //
        // Szándékosan szűk: EGYETLEN, távval megnevezett mozgás mellett élünk
        // vele, és csak akkor, ha az alkalomszám egy. A több mozgásformás
        // mondat („bicikli 20 km, futás 5 km") már eddig is helyesen működött,
        // a „két futás: 5 km és 8 km" alkalomszáma pedig ki van mondva.
        // A RÉSZLET nem külön edzés: a „futottam 10 km-t, ebből 5 km tempó"
        // öt kilométere a tíznek a része. Egyetlen ilyen szó elég ahhoz, hogy
        // a mondat ne felsorolás legyen, hanem bontás.
        boolean partOfIt = false;
        for (String w : new String[]{"ebbol", "abbol", "amibol", "ezen belul",
                "beleertve", "kozte", "kozuluk", "ebben",
                // Az „utolsó 3 km tempóban" a futás UTOLSÓ szakasza, nem egy
                // második futás: a „18 km, 1:45:20, utolsó 3 km tempóban"
                // eddig két bejegyzés lett, huszonegy kilométerrel.
                "utolso", "a masodik fele", "a vege"})
            if (s.contains(w)) { partOfIt = true; break; }
        if (!partOfIt && out.size() == 1 && out.get(0).kind.distance
                && out.get(0).km > 0 && kms.size() > 1) {
            Plan p0 = out.get(0);
            // A kimondott alkalomszám is stimmelhet: a „hétvégén két túra:
            // szombaton 12 km, vasárnap 18 km" kettese a két túra, és a
            // tizennyolc kilométer eddig elveszett – mindkét bejegyzés
            // tizenkettőt kapott. Ha annyi táv van, ahány alkalom, akkor a
            // számok a saját alkalmukhoz tartoznak.
            int free = 0;
            for (double[] t : kms) {
                boolean taken = false;
                for (Plan p : out) if (Math.abs(p.km - t[1]) < 0.001) taken = true;
                if (!taken && t[1] > 0) free++;
            }
            if (p0.count > 1 && p0.count == free + 1) {
                out.set(0, new Plan(p0.kind, 1, p0.minutes, p0.km, p0.steps));
                p0 = out.get(0);
            }
            if (p0.count != 1) {
                // Marad a régi viselkedés: a kimondott alkalomszám erősebb.
                free = -1;
            }
            if (free >= 0)
            for (double[] t : kms) {
                double km2 = t[1];
                if (km2 <= 0) continue;
                boolean taken = false;
                for (Plan p : out) if (Math.abs(p.km - km2) < 0.001) taken = true;
                if (taken) continue;
                // A SZINTEMELKEDÉS méterben áll, és nem táv: a „túra 14,8 km
                // 3:45:00 620 m emelkedés" hatszázhúsz métere nem egy második
                // séta. A többnapos pótlást viszont mindenki kilométerben
                // írja – ezért csak a kiírt km-es adatot vesszük át.
                int te = (int) t[2];
                String tail = beforeBlank.substring(Math.min(beforeBlank.length(), (int) t[0]),
                        Math.min(beforeBlank.length(), te + 4));
                if (!tail.contains("km")) continue;
                int said2 = minutesFor(mins, (int) t[0], (int) t[2],
                        -1, Integer.MAX_VALUE, 0);
                for (Plan p : out) if (p.minutes == said2) said2 = 0;
                int est2 = Math.max(1, (int) Math.round(km2 * pace(beforeBlank, p0.kind)));
                out.add(new Plan(p0.kind, 1, Math.min(24 * 60, said2 > 0 ? said2 : est2), km2));
            }
            // A napok is szétnyílnak: a „tegnapelőtt 5 km, tegnap 8 km, ma
            // 3 km" három napról szól, nem háromszor tegnapelőttről. Csak
            // múltba nyúló mondatnál, mert a mai két futás ma volt.
            if (offset > 0 && out.size() > days) days = out.size();
        }

        // Gazdátlan táv mozgásforma nélkül: „ma reggel 5 km, délután 40 perc
        // kondi". Az öt kilométer mellett nincs sportszó, a kondi pedig nem
        // tud távot tárolni – eddig nyomtalanul eltűnt. Magában a „nyomtam
        // egy 5 km-t" már futásnak számított; itt csak az volt a különbség,
        // hogy a mondat MÁSIK felében volt egy edzés is.
        if (!partOfIt && !kms.isEmpty() && !out.isEmpty()) {
            boolean anyKm = false;
            for (Plan p : out) if (p.km > 0) anyKm = true;
            if (!anyKm) {
                Kind run = byId("futas");
                for (double[] t : kms) {
                    double km2 = t[1];
                    if (km2 <= 0) continue;
                    String tail = beforeBlank.substring(
                            Math.min(beforeBlank.length(), (int) t[0]),
                            Math.min(beforeBlank.length(), (int) t[2] + 4));
                    if (!tail.contains("km")) continue;
                    int said2 = minutesFor(mins, (int) t[0], (int) t[2],
                            -1, Integer.MAX_VALUE, 0);
                    // Amit egy másik mozgás már elvitt, azt nem vesszük el
                    // tőle: a „40 perc kondi" negyvene a kondié marad, a
                    // futás hosszát a tempóból becsüljük.
                    for (Plan p : out) if (p.minutes == said2) said2 = 0;
                    int est2 = Math.max(1, (int) Math.round(km2 * pace(beforeBlank, run)));
                    out.add(new Plan(run, 1, Math.min(24 * 60, said2 > 0 ? said2 : est2), km2));
                }
            }
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
                // Az „edzés UTÁN" nem edzés, hanem IDŐPONT. Az „edzés után
                // ittam egy fehérjeturmixot" mondatból eddig negyvenöt perc
                // egyéb mozgás lett – és mivel az edzés-felismerő az étkezés
                // elé áll, a turmix el is veszett mellőle. Ugyanez az „edzés
                // előtt" és az „edzés közben".
                if (timePhraseAfter(s, p + w.length())) continue;
                Kind other = byId("egyeb");
                int n = countBefore(s, p);
                // A szorzószám itt is állhat hátul: „a héten edzettem négyszer".
                if (n <= 1)
                    for (int[] mu : mults)
                        if (mu[0] > p && mu[1] > 1) { n = Math.min(50, mu[1]); break; }
                // Az „alkalom" csak SZÁMMAL edzés. Magában a leghétköznapibb
                // magyar főnév: a „születésnapi alkalomból tortát ettem"
                // mondatból eddig negyvenöt perc mozgás lett – az edzés
                // felismerője pedig az étkezés elé áll, tehát a torta el is
                // veszett mellőle.
                if (w.equals("alkalom") && n <= 1 && numberBefore(s, p, NUM_REACH) == null)
                    continue;
                // A kimondott időtartam itt is számít („otthoni edzés 40 perc").
                if (other != null) out.add(new Plan(other, n,
                        minutesFor(mins, p, p, -1, Integer.MAX_VALUE,
                                other.defaultMin), 0));
                break;
            }
        }
        // Ahány alkalom, annyi hossz: a „két edzés ma, 45 és 60 perc"
        // hatvana eddig elveszett, és MINDKÉT alkalom negyvenöt percet
        // kapott. Ha a kimondott időtartamok száma pont az alkalomszám, a
        // hosszak a saját alkalmukhoz tartoznak – ugyanaz a szabály, ami a
        // távoknál már megvolt („két túra: 12 km és 18 km").
        if (out.size() == 1 && out.get(0).count > 1 && out.get(0).km <= 0
                && out.get(0).steps <= 0 && mins.size() == out.get(0).count) {
            Plan p0 = out.get(0);
            boolean own = false;
            for (int[] m : mins) if (m[1] == p0.minutes) own = true;
            if (own) {
                List<Plan> split = new ArrayList<>();
                for (int[] m : mins)
                    split.add(new Plan(p0.kind, 1, m[1], 0));
                out = split;
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
        // Az INTERVALL-terv szakaszai nem külön edzések. A „20 mp sprint 40 mp
        // séta, 12 kör" a futásnak és a sétának is a MOZGÁSFORMA szokásos
        // hosszát adta – negyvenöt plusz kilencven percet egy tizenkét perces
        // edzésre. Ahol másodperces munka/pihenő pár áll, ott a hossz nem az
        // alapértelmezett: a kimondott idővel vagy távval megadott mozgás
        // viszont marad.
        if (rawText.split("(?<![a-z])\\d{1,3}\\s?mp(?![a-z])", -1).length >= 3) {
            List<Plan> kept = new ArrayList<>();
            for (Plan p : out)
                if (p.km > 0 || p.minutes != p.kind.defaultMin) kept.add(p);
            if (!kept.isEmpty() || out.size() > 1) out = kept;
        }
        // PERCBEN írt szakaszoknál az időzítő-terv a mérce: a „3x(5 perc
        // futás + 1 perc séta)" öt- és egyperces darabjai a KÖRÖK részei,
        // nem külön edzések – eddig három ötperces futás és egy egyperces
        // túra is bekerült a terv mellé. Amelyik mozgás hossza pont egy
        // szakasz hossza, az a szakasz maga.
        // Csak valódi munka/pihenő párnál: a „2x45 perc foci" két félidő,
        // ott nincs pihenő-szakasz, és a meccs marad bejegyzés.
        IntervalParse.Plan ip = IntervalParse.parse(rawText);
        if (ip != null && ip.rounds >= 2 && ip.rest > 0 && !ip.guessed) {
            List<Plan> kept = new ArrayList<>();
            for (Plan p : out) {
                int sec = p.minutes * 60;
                if (p.km <= 0 && p.steps <= 0 && (sec == ip.work || sec == ip.rest))
                    continue;
                kept.add(p);
            }
            out = kept;
        }
        // A TEREM csak HELYSZÍN: a „45 perc spinning óra a teremben" a
        // negyvenöt perces kerékpározás MELLÉ egy hatvanperces kondit is
        // beírt – ugyanannak az órának a helyszínéből, kimondatlan hosszal.
        // Ha a teremre a mondatban semmi más nem utal, és van mellette
        // kimondott hosszúságú edzés, a helyszín nem külön bejegyzés.
        // A terem MÉRLEGE sem edzés: „az edzőteremben mértem: 78,8 kg" a
        // mérésről szól, a terem csak helyszín – mégis hatvan perc kondi
        // került mellé.
        if (out.size() == 1 && onlyGymPlace(rawText)
                && "kondi".equals(out.get(0).kind.id)
                && out.get(0).minutes == out.get(0).kind.defaultMin
                && out.get(0).km <= 0 && out.get(0).steps <= 0
                && rawText.matches(".*\\d\\s?kg.*")
                && (rawText.contains("mertem") || rawText.contains("merleg")
                    || rawText.contains("meres"))) {
            out = new ArrayList<>();
        }
        if (out.size() > 1 && onlyGymPlace(rawText)) {
            boolean stated = false;
            for (Plan p : out)
                if (p.km > 0 || p.steps > 0 || p.minutes != p.kind.defaultMin)
                    stated = true;
            // A kiírt perc akkor is kimondott hossz, ha épp az alap-
            // értelmezéssel egyezik: a „45 perc kardió a teremben" mellé
            // egy hatvanperces kondi került – a terem csak helyszín.
            if (!stated && rawText.matches(".*\\d\\s?perc.*")) stated = true;
            if (stated) {
                // Ha a terem kondija LOPTA el az egyetlen kimondott percet
                // („falmásztam a boulder teremben 90 percet"), a perc a
                // konkrét sporté – a kondi átadja, mielőtt kiesik.
                Plan gymPlan = null, specific = null;
                for (Plan p : out) {
                    if ("kondi".equals(p.kind.id)) gymPlan = p;
                    else specific = p;
                }
                if (gymPlan != null && specific != null && out.size() == 2
                        && gymPlan.minutes != gymPlan.kind.defaultMin
                        && specific.minutes == specific.kind.defaultMin
                        && specific.km <= 0 && specific.steps <= 0
                        && gymPlan.km <= 0 && gymPlan.steps <= 0) {
                    out = new ArrayList<>();
                    out.add(new Plan(specific.kind, specific.count,
                            gymPlan.minutes, 0, 0));
                }
                List<Plan> kept = new ArrayList<>();
                for (Plan p : out) {
                    // A terem kondija akkor is kiesik, ha a MÁSIK edzés
                    // percét másolta le: „a 4-es teremben volt a spinning,
                    // 45 perc" kondija ugyanazt a 45 percet kapta meg.
                    boolean copied = false;
                    for (Plan o : out)
                        if (o != p && !"kondi".equals(o.kind.id)
                                && o.minutes == p.minutes) copied = true;
                    if ("kondi".equals(p.kind.id) && p.count == 1 && p.km <= 0
                            && p.steps <= 0
                            && (p.minutes == p.kind.defaultMin || copied))
                        continue;
                    kept.add(p);
                }
                if (!kept.isEmpty()) out = kept;
            }
        }
        // A „KÖZBEN" mért lépés nem külön séta a tevékenység MELLÉ: a
        // „takarítás közben 4000 lépés" egyetlen óra takarítás, nem
        // takarítás PLUSZ fél óra gyaloglás – kilencven perc mozgás lett
        // egy órából. A kimondatlan hosszú kísérő tevékenység esik ki, a
        // lépésszám (a lépéscél adata) marad.
        if (steps > 0 && rawText.contains("kozben") && out.size() >= 1) {
            List<Plan> kept = new ArrayList<>();
            for (Plan p : out) {
                if (!"tura".equals(p.kind.id) && p.km <= 0 && p.steps <= 0
                        && p.minutes == p.kind.defaultMin && p.count == 1)
                    continue;
                kept.add(p);
            }
            out = kept;
        }
        // A LÉPÉS és a TÁV ugyanaz a séta, ha a mondat egyetlen futás-szót
        // sem mond ki: a „ma 14 000 lépés, 9,8 km" a tíz és fél kilométeres
        // gyaloglás MELLÉ egy tíz kilométeres FUTÁST is beírt – húsz
        // kilométer abból a tízből, amit az ember tényleg megtett. A
        // „14 000 lépés és futottam 5 km-t" viszont két külön dolog, mert ott
        // a futás ki van mondva.
        if (out.size() == 2) {
            Plan stepPlan = null, kmPlan = null;
            for (Plan p : out) {
                if (p.steps > 0) stepPlan = p;
                else if (p.km > 0 && "futas".equals(p.kind.id)) kmPlan = p;
            }
            boolean saidRun = false;
            Kind run = byId("futas");
            if (run != null)
                for (String w : run.words) if (rawText.contains(w)) { saidRun = true; break; }
            if (stepPlan != null && kmPlan != null && !saidRun) {
                List<Plan> one = new ArrayList<>();
                one.add(new Plan(stepPlan.kind, 1, stepPlan.minutes,
                        kmPlan.km, stepPlan.steps));
                out = one;
            }
        }
        // Ugyanez TÁVBAN kiírt szakaszoknál: a „sprint edzés: 10x100 m,
        // köztük séta vissza" sétája a szakaszok közti visszasétálás, nem
        // másfél órás túra – eddig kilencven perc gyaloglás került a naplóba
        // egy néhány perces sprint-edzés mellé. A KÖZTÜK szó pont ezt mondja
        // ki; a kimondott idővel vagy távval megadott mozgás itt is marad.
        if (rawText.matches(".*\\d\\s?x\\s?\\d.*")
                && (rawText.contains("koztuk") || rawText.contains("kozte")
                    || rawText.contains("kozott"))) {
            List<Plan> kept = new ArrayList<>();
            for (Plan p : out)
                if (p.km > 0 || p.steps > 0 || p.minutes != p.kind.defaultMin)
                    kept.add(p);
            if (!kept.isEmpty()) out = kept;
        }
        // Az ISMÉTLÉSSZÁM nem alkalomszám. Az „5 kör: 500 m evezés, 15
        // kettlebell swing" tizenötöse a lendítések száma – a kettlebell
        // viszont kondi-szótő is, így tizenöt darab hatvanperces edzés lett
        // belőle: tizenöt óra mozgás egy negyedórás körből. Ha a mondatban
        // felismert SOROZAT is van, a szorzószám azé, és nem a naplóé.
        // A kimondott gyakoriság („hetente 3 futás") megvédi magát: ott a
        // gyakoriság-szó adja a napokat, nem a szám melletti mozgásforma.
        // A kimondott ALKALOM megvédi magát: a „2 fekvőtámasz edzés" két
        // edzés, mert ott a szám után az edzés szó áll, nem a gyakorlaté.
        boolean saysSessions = rawText.matches(".*\\d{1,2}\\s+\\w+\\s+edzes\\w*.*");
        List<StrengthParse.Item> lifts = StrengthParse.parse(rawText);
        if (!lifts.isEmpty() && !saysSessions) {
            List<Plan> fixed = new ArrayList<>();
            boolean any = false;
            for (Plan p : out) {
                // Csak az általános gyűjtő-mozgásformát javítjuk: a „3 futás"
                // hármasát senki nem sorozatnak szánta, a futás a saját neve
                // alatt fut. A számnak pedig EGYEZNIE kell egy felismert
                // sorozat ismétlésszámával – így csak az kerül vissza a
                // helyére, amit tényleg a gyakorlattól vettünk el.
                boolean generic = "kondi".equals(p.kind.id) || "egyeb".equals(p.kind.id);
                if (generic && p.count > 1 && p.km <= 0
                        && p.minutes == p.kind.defaultMin && repsMatch(lifts, p.count)) {
                    fixed.add(new Plan(p.kind, 1, p.minutes, p.km, p.steps));
                    // A napok száma is ebből a számból jött („20 kettlebell
                    // swing" húsz napra osztva) – az sem áll meg nélküle.
                    if (days == p.count) days = 1;
                    any = true;
                } else if (!generic && p.km <= 0 && p.steps <= 0
                        && p.minutes == p.kind.defaultMin && namedByLift(lifts, p.kind)
                        && (p.count == 1 || repsMatch(lifts, p.count))) {
                    // A GYAKORLAT NEVE nem külön kardió-edzés: a „súlyzós:
                    // guggolás 3×8 80, evezés 3×10 50" evezése egy sorozat a
                    // teremben, nem félórányi evezőgépezés. Eddig a hatvan
                    // perc kondi MELLÉ bekerült egy harmincperces evezés is –
                    // ugyanaz a mozdulat kétszer, ráadásul olyan hosszal,
                    // amit ki sem mondott senki. A kimondott idő megvédi
                    // magát: a „40 perc evezőgép" hossza nem az alapérték.
                    any = true;
                } else fixed.add(p);
            }
            if (any) out = fixed;
        }
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
            // A mondat ELEJÉN álló, mértékegység nélküli számpár dátum: a
            // naplóból kimásolt sor így néz ki („01.15 futás 8 km"). Rag és
            // évszám nélkül eddig nem dátumnak számított, hanem darabszámnak:
            // tizenöt darab nyolckilométeres futás lett belőle, tizenöt napra
            // elosztva – százhúsz kilométer egyetlen sorból.
            boolean lineStart = m.start() == 0 && !hasYear
                    && (suf == null || suf.equals("."))
                    && m.end() < s.length() && s.charAt(m.end()) == ' '
                    && !s.substring(m.end() + 1).matches("^(km|kg|m|perc|ora|dl|l|%).*");
            if (!hasYear && !lineStart && (suf == null || !suf.startsWith("-"))) continue;
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
                // Az osztó szám csak akkor DARABSZÁM, ha nincs mögötte
                // mértékegység. Az „5-5 km" alkalmankénti TÁV: a „héten
                // kétszer futottam 5-5 km-t" mondatból eddig tizennégy futás
                // lett, mert a kétszerest a hét napjaival is felszorozta.
                return unitAfter(s, i + 3) ? 0 : s.charAt(i) - '0';
            }
        }
        int p = s.indexOf("egy-egy");
        if (p >= 0) { blank(q, p + 3, p + 7); return 1; }
        return 0;
    }

    /**
     * Időpont-szó áll-e a megadott hely után: „edzés UTÁN", „edzés ELŐTT".
     *
     * A szó ragja még hozzátartozhat a tőhöz („edzés" → „edzésem"), ezért a
     * betűket átlépjük, és csak az utána álló KÜLÖN szót nézzük.
     */
    private static boolean timePhraseAfter(String s, int from) {
        // A -HEZ rag a hozzávalóé, nem az edzésé: a „monster ital edzéshez"
        // negyvenöt perces bejegyzés lett – az italból, amit MAJD az edzéshez
        // iszik az ember.
        if (s.startsWith("hez", from) || s.startsWith("hoz", from)) return true;
        int i = from;
        while (i < s.length() && Character.isLetter(s.charAt(i))) i++;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        String rest = s.substring(Math.min(i, s.length()));
        for (String u : new String[]{"utan", "elott", "kozben", "kozbeni", "utani",
                // Az „edzés ALATT ittam egy izotóniást" ugyanolyan időpont,
                // mint az „edzés után" – eddig negyvenöt perces bejegyzés
                // lett belőle az ital mellé.
                "alatt", "alatti", "soran", "vegen", "elejen",
                "elotti", "kore", "korul"})
            if (rest.startsWith(u)) return true;
        return false;
    }

    /** Áll-e mértékegység a megadott helytől (szóközöket átlépve). */
    private static boolean unitAfter(String s, int from) {
        int i = from;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        String rest = s.substring(Math.min(i, s.length()));
        for (String u : new String[]{"km", "kilometer", "meter", "perc", "ora",
                "masodperc", "mp", "hossz", "lepes"})
            if (rest.startsWith(u)) return true;
        return false;
    }

    /**
     * Lépésszám a szövegben: „10000 lépés", „10 ezer lépés", „tízezer lépés"
     * → {kezdet, vég, lépések}. 500 alatt és 100 000 felett nem hisszük el.
     */
    private static double[] findSteps(char[] q) {
        String s = new String(q);
        // Az ÖSSZES lépés-szót végignézzük, nem csak az elsőt: a „napi
        // lépéscél 10000, ma 11200 lépés lett" első lépés-szava a cél
        // összetett szavában ül, szám nélkül – és miatta a valódi számláló
        // is némán elveszett.
        for (int p = s.indexOf("lepes"); p >= 0; p = s.indexOf("lepes", p + 1)) {
            double[] one = stepsAt(s, p);
            if (one != null) return one;
        }
        return null;
    }

    /** Egy adott lépés-szó előtti számláló, vagy null. */
    private static double[] stepsAt(String s, int p) {
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
     * Lépésszám a szó MÖGÖTT, kettősponttal: „lépés: 21450".
     *
     * Az óra-app kijelzője így írja ki, és az „eddigi legtöbb lépés: 21 450"
     * eddig némán elveszett – a szám a szó után állt, a kereső meg csak
     * előtte nézte.
     */
    private static double[] findStepsAfter(char[] q) {
        String s = new String(q);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("lepes\\w*\\s?:\\s?(\\d{3,6})(?![\\d.,])").matcher(s);
        if (!m.find()) return null;
        double steps = Double.parseDouble(m.group(1));
        if (steps < 500 || steps > 100000) return null;
        return new double[]{m.start(1), m.end(1), steps};
    }

    /**
     * A lépéscél MELLETT kimondott mai érték: „napi lépéscél 10000, ma
     * 11200 lett". A cél utáni tagmondat lett-tel zárt száma a valódi
     * lépésszám – eddig az egész mondat üresen jött vissza, mert a cél
     * száma mellett nem állt lépés-szó, a mai szám mellett meg semmi.
     */
    private static double[] findStepsByGoal(char[] q) {
        String s = new String(q);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("lepescel\\w*\\s?:?\\s?\\d{3,6}\\s?[,;]"
                        + "[^0-9]{0,16}?(\\d{3,6})(?![\\d.,])"
                        + "[^0-9]{0,10}?(?<![a-z])lett(?![a-z])").matcher(s);
        if (!m.find()) return null;
        double steps = Double.parseDouble(m.group(1));
        if (steps < 500 || steps > 100000) return null;
        return new double[]{m.start(1), m.end(1), steps};
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
                // Igealakban: a „TERVEZETT 10 helyett 6 lett" megtörtént
                // edzésről szól, és a puszta „tervez" tő elvitte az egészet.
                "tervezek", "tervezem", "tervezunk", "tervezi", "tervezni",
                "szeretne", "megyek", "lesz idom", "majd lesz",
                // Szándék és VÉLEMÉNY: a „szeretek futni" nem egy futás, a
                // „jó lenne egy futás" pláne nem, és az „el kellene menni"
                // pont az ellenkezője. Mind a naplóba került, negyvenöt
                // perces alapértelmezett hosszal.
                "szeretek", "szeretem", "imadok", "utalok", "utalom",
                "kellene", "jo lenne", "jol esne", "kedvem",
                // A „kéne" a „kellene" beszélt alakja, a „kell csinálnom" és
                // a „meg kell" pedig a teendő – ezek is tervek, nem naplók.
                // A feltételes „ha lesz" ugyanígy: a „ha lesz idő, futok
                // egyet" mondatból eddig negyvenöt perc futás lett.
                // A FELSZÓLÍTÁS is terv: a „csináljunk egy tabatát" és a
                // „menjünk futni" javaslat, nem napló – az időzítő-terv
                // ilyenkor is elkészül, csak a bejegyzés nem.
                "csinaljunk", "menjunk", "fussunk", "edzunk egy", "kezdjunk",
                "nyomjunk", "tekerjunk", "usszunk", "gyakoroljunk", "vagjunk bele",
                "gyurunk egyet", "gyurjunk", "sportoljunk", "mozogjunk",
                "guggoljunk", "toljunk egyet", "huzzunk egyet", "setaljunk",
                // A FELTÉTELES MÚLT sosem megtörtént: a „ha lett volna időm,
                // futottam volna" negyvenöt perces bejegyzés lett – abból a
                // mondatból, ami épp azt mondja ki, hogy nem futott.
                "volna",
                "kene", "kell csinalnom", "kell mennem", "meg kell", "ha lesz",
                "ha lesz ido", "ha birom", "ha sikerul",
                // A kimondott AKARAT a legtisztább szándék-alak, és eddig
                // hiányzott: az „erősíteni akarom a bokám" és az „el akarok
                // kezdeni futni" mondatból egy hatvanperces kondi, illetve
                // egy negyvenöt perces futás lett – olyan edzés, ami meg sem
                // történt. A „fogom" a „fogok" párja (tárgyas ragozás).
                "akarok", "akarom", "akarunk", "akarod", "akarja",
                "fogom", "fogunk", "fogjuk", "fogja",
                // A szándék többi hétköznapi alakja. Mind ugyanarról szól:
                // a mondat egy JÖVŐBELI edzésről beszél, az app mégis
                // megtörténtként naplózta, teljes idővel, szériával, XP-vel.
                // A „KÉSZÜLÖK" nem itt: tagmondat-szintű tagadás lett, hogy
                // az „5k versenyemre készülök, ma 3 km sikerült" hárma
                // megmaradjon.
                "remelem", "megprobal", "probalok", "probalom",
                "elhataroz", "eldontottem", "muszaj", "kotelezo", "vagyom ra",
                "gondolkodom", "gondolkozom", "igerem", "eltokel", "nekiallok",
                "raveszem magam", "ossze kell szedn",
                // A kiírt TERV szó is: „a terv: guggolás 5x5 100 kg".
                // Szóhatárral: a „TERVezett 10 helyett" egy megtörtént
                // edzésről szól, és eddig az egész mondat kiesett tőle.
                "a terv ", "a terv:", "terv:", "tervem", "a tervek",
                // A CÉL sem napló: az „a heti célom 4 edzés" négy megtörtént
                // edzésként került be – a hét elején, amikor még egy sem volt.
                "celom", "celunk", "celja a", "heti cel", "napi cel", "cel:",
                // A cél BEÁLLÍTÁSA sem edzés: a „beállítottam a 10000 lépéses
                // célt" hét és fél kilométeres sétát írt a naplóba – abból a
                // számból, amennyit az ember MAJD el akar érni naponta.
                "lepeses cel", "a celt", "celt allitottam", "celt tuztem",
                // A NEVEZÉS nem futás: a „beneveztem egy félmaratonra"
                // huszonegy kilométert írt a naplóba egy olyan versenyről,
                // ami még el sem kezdődött.
                "benevez", "beneveztem", "nevezes", "jelentkeztem egy"})
            if (s.contains(w)) return true;
        // A PÓTLÁS jelen ideje terv: a „hétvégén pótolom az edzést" egy
        // majdani edzés – eddig megtörténtként került be. Két kivétel: a
        // NAPLÓ pótlása („pótolom: tegnap 30 perc jóga" – utólag beírt,
        // megtörtént edzés) és a kimondott „tegnap". A múlt idejű
        // „bepótoltam" a ragja miatt eleve nem esik a mintába.
        if (s.matches(".*(?<![a-z])(?:be)?potol(?:om|juk)(?![a-z]).*")
                && !s.matches(".*(bejegyz|beir|napl|rogzit|tegnap).*"))
            return true;
        // A mondat ELEJÉN álló „majd" és a FŐNÉVI IGENÉV együtt jövő idő:
        // „majd futni 30 percet", „talán elmenni a terembe". Külön-külön
        // egyik sem elég – a „majd 30 perc kondi" beírható a naplóba
        // utólag is, a „futottam, majd úsztam" pedig két megtörtént edzés –,
        // de a kettő együtt csak szándékot jelenthet.
        for (String w : new String[]{"majd ", "esetleg ", "talan "})
            if (s.startsWith(w) && hasInfinitive(s)) return true;
        // A KÉPESSÉG nem napló: a „fáj a térdem 2 hete, de futni TUDOK" arról
        // szól, mi megy és mi nem – eddig negyvenöt perces futás lett belőle,
        // pont egy sérült térd mellé.
        // A MÚLT idejű „tudtam" nem képesség, hanem siker: az „el tudtam menni
        // futni, 5 km" megtörtént. Csak a jelen és a feltételes alak marad.
        if (s.matches(".*(?<![a-z])(?:tudok|tudunk|tudnek|tudnank)\\s+\\w{3,}ni(?![a-z]).*")
                || s.matches(".*(?<![a-z])\\w{3,}ni\\s+(?:tudok|tudunk|tudnek|tudnank)"
                        + "(?![a-z]).*"))
            return true;
        // A HALADÁS leírása nem edzés: az „5 km-ről 10 km-re növeltem a
        // távot" a tervről szól, nem egy megtörtént futásról – eddig ötven
        // kilométeres… illetve öt kilométeres futás lett belőle, ráadásul a
        // RÉGI értékkel. A tól-ig pár és a változás-ige együtt kell hozzá.
        if (s.matches(".*\\d.*-?r[oó]l\\b.*\\d.*-?r[ae]\\b.*"))
            for (String w : new String[]{"novelt", "novelem", "emelt", "csokkent",
                    "javult", "nott", "valtott", "leptem fel"})
                if (s.contains(w)) return true;
        // FELTÉTELES mondat múlt idejű ige nélkül: a „ha esik, futópadon
        // futok" terv, nem napló – eddig negyvenöt perces futás lett belőle.
        // A múlt idő megvédi a valódi bejegyzést: a „ha jól emlékszem, 5
        // km-t futottam tegnap" megtörtént.
        if ((" " + s.replaceAll("[^a-z0-9]", " ") + " ").contains(" ha ")
                && !s.matches(".*\\b\\w{3,}(tam|tem|tunk)\\b.*")) return true;
        // Egyes szám első személyű jelen idő. A „futok" és az „edzek"
        // SZÁNDÉKOSAN kimarad: az előbbi a futás szótöve (a „három kört futok"
        // is futás), az utóbbi pedig szinte mindig tagadásban áll („nem
        // edzek"), amit a pihenőnap-ág amúgy is kezel.
        for (String w : new String[]{"uszok", "biciklizek", "gyurok",
                "sportolok", "mozgok",
                // A „majd kondizok" ugyanolyan szándék, mint a „majd úszok".
                // (A puszta „majd" NEM lehet jelzőszó: a „futottam, majd
                // úsztam" két megtörtént edzés.)
                "kondizok", "kondizom", "uszom", "biciklizem", "sportolok", "setalok",
                // A SZOKÁS soha nem egy alkalom: a „szoktam futni" és a
                // „hetente háromszor járok kondiba" arról szól, hogy MIT
                // csinál az ember általában – eddig mindkettőből egy teljes
                // bejegyzés lett, alapértelmezett hosszal.
                "szoktam", "szoktunk", "jarok", "jarunk", "jaro"}) {
            // A MAI mennyiség kimenti a mondatot: az „úszni járok, ma 1 km"
            // első fele szokás, a második egy megtörtént úszás – eddig az
            // egész mondat elveszett, a kilométerrel együtt. Ha a szokás
            // mellett kimondott napon kimondott mennyiség áll, a mondat nem
            // terv; a puszta „úszni járok" marad az.
            if (s.matches(".*(?<![a-z])(ma|tegnap|most|delelott|delutan|"
                    + "este|reggel)(?![a-z])[^,;.]*\\d.*")) break;
            // A MÉRT tagmondat is kimenti: a „botokkal járok, nordic
            // walking 3 km" második fele kimondott távú, megtörtént túra –
            // eddig a „járok" szokás-szava az egészet elvitte. A gyakoriság
            // száma („heti 3x járok") nem esik ide: az a szokás tagmondatában
            // áll, nem vessző után.
            if (s.matches(".*[,;][^,;]*\\d[^,;]*"
                    + "(?<![a-z])(km|perc|lepes|ora|m)(?![a-z]).*")) break;
            int p = s.indexOf(w);
            while (p >= 0) {
                int e = p + w.length();
                if ((p == 0 || !Character.isLetter(s.charAt(p - 1)))
                        && (e >= s.length() || !Character.isLetter(s.charAt(e)))) return true;
                p = s.indexOf(w, p + 1);
            }
        }
        // A „futok" és az „edzek" magában szándékosan NEM jelzőszó (lásd
        // fent) – GYAKORISÁG mellett viszont egyértelmű szokás: a „minden
        // másodnap futok" nem egy futás, hanem a heti rend leírása.
        boolean often = false;
        for (String w : new String[]{"hetente", "naponta", "havonta", "masodnaponta",
                "minden nap", "minden masodnap", "altalaban", "rendszeresen",
                "mostanaban", "manapsag"})
            if (s.contains(w)) { often = true; break; }
        if (often)
            for (String w : new String[]{"futok", "edzek", "megyek", "csinalom",
                    "tekerek", "jogazok", "sulyzozok", "gyakorlok"})
                if (s.matches(".*(?<![a-z])" + w + "(?![a-z]).*")) return true;
        // A HETI BEOSZTÁS is a rend leírása: a „push pull legs, heti 6 edzés"
        // hatosa a rendszer neve mellett álló ütem, nem hat megtörtént edzés –
        // eddig hat negyvenöt perces bejegyzés lett belőle, egy hétre
        // elosztva. Múlt idejű ige megvédi a valódi beszámolót: a „heti 3
        // edzés volt a héten" bejegyzés marad.
        // A PROGRAM elkezdése maga még nem edzés: az „elkezdtem a couch to 5k
        // programot" ötkilométeres futásként került be – abból a névből, ami
        // épp azt jelenti, hogy ODÁIG még el kell jutni. A megtörtént első
        // alkalom viszont marad: „elkezdtem a programot, ma 3 km".
        if (s.contains("program") || s.contains("kihivas")) {
            boolean started = false;
            for (String w : new String[]{"elkezdt", "belevagt", "nekiallt", "beneveztem",
                    "jelentkeztem", "elindult", "indul a", "csatlakoztam"})
                if (s.contains(w)) { started = true; break; }
            // A program NEVÉBEN álló szám nem teljesítmény: a „couch to 5k"
            // ötöse maga a cél, nem a ma megtett táv.
            boolean didSomething = java.util.regex.Pattern
                    .compile("\\d+\\s?(?:km|perc|ora|lepes)(?!\\s*programm?\\w*)")
                    .matcher(s).find();
            if (started && !didSomething) return true;
        }
        if (s.matches(".*heti \\d{1,2} edzes.*")
                && !s.matches(".*(?<![a-z])(volt|voltak|megvolt|sikerult"
                        + "|\\w{3,}(?:tam|tem|tunk))(?![a-z]).*")) return true;
        return false;
    }

    /**
     * Van-e a mondatban főnévi igenév („futni", „elmenni", „megcsinálni")?
     *
     * A magyar főnévi igenév sosem mond meg NEM TÖRTÉNT eseményt magában, de
     * a jövőre utaló szavakkal együtt már egyértelmű: „majd futni egyet".
     * Öt betűnél rövidebbet nem fogadunk el, mert a rövid -ni végű szavak
     * (bikini, martini) nem igék.
     */
    private static boolean hasInfinitive(String s) {
        for (String w : s.split("[^a-z0-9]+"))
            if (w.length() >= 5 && w.endsWith("ni")) return true;
        return false;
    }

    /**
     * Szó belsejében csak igekötő után érvényes szótövek.
     *
     * Mind rövid, és mind ott lakik hétköznapi szavak közepén: az „evez" a
     * beNEVEZTemben, az „úsz" az augUSZtusban és a bUSZban, a „gym" az
     * EGYMÁSban, az „mma" pedig MINDEN -mmal ragos szóban (alkaloMMAl,
     * szá­MMAl, dátuMMAl). Igekötő után viszont valódi: leúsztam, kieveztem.
     */
    private static boolean isFragileStem(String w) {
        return w.startsWith("evez") || w.startsWith("usz")
                || w.equals("gym") || w.equals("mma") || w.equals("kezi");
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
                "huzodzkodas", "plank",
                // A „minden második percben 15 kettlebell swing" tizenöt
                // ISMÉTLÉS, nem tizenöt edzés – eddig tizenöt húszperces
                // kondi került be egyetlen EMOM-ból. A burpee ugyanígy, és
                // a „10x25 sprintekkel" huszonöte is ismétlés, nem
                // huszonöt futás.
                "kettlebell", "burpee", "swing", "sprint"})
            if (w.startsWith(r)) return true;
        return false;
    }

    /** Tagadó / pihenőnapos mondat: az üres eredmény oka nem értetlenség. */
    public static boolean looksLikeRest(String text) {
        String s = Foods.norm(text == null ? "" : text);
        for (String w : new String[]{"nem ", "megsem ", "sem ", "se ",
                "kihagytam", "kimaradt", "elmarad",
                "lemondtam", "pihenonap", "pihenes", "pihentem", "rest day"}) {
            int p = s.indexOf(w);
            if (p >= 0 && (p == 0 || !Character.isLetter(s.charAt(p - 1)))) return true;
        }
        return false;
    }

    /** Kötőszavak, amik vessző nélkül is ÚJ állítást nyitnak. */
    // A „de" is új állítást nyit: az „az nem edzés de 6 km-t gyalogoltam"
    // hat kilométere megtörtént – eddig a tagadás vessző híján elvitte.
    private static final String[] LINKERS = {" es ", " majd ", " utana ",
            " aztan ", " viszont ", " de "};

    /**
     * Tagadás és csere kitakarása. Az „X helyett" X-e a tagmondat elejétől a
     * szóig, a tagadó/kihagyó igék („nem …", „kihagytam", „elmaradt",
     * „lemondtam") a tagmondat végéig tűnnek el – a többi tagmondat él marad:
     * a „ma nem futottam, csak sétáltam" sétája bekerül.
     */
    private static void stripNegated(char[] q) {
        stripBackwardNegation(q);
        stripOtherPerson(q);
        stripStartOfHabit(q);
        stripComplaintClauses(q);
        String s = new String(q);
        int h = s.indexOf("helyett");
        while (h >= 0) {
            int a = h;
            while (a > 0 && s.charAt(a - 1) != ',' && s.charAt(a - 1) != '.') a--;
            // Ha a „helyett" előtt CSAK egy szám áll, akkor az összehasonlítás
            // a számra vonatkozik, nem az egész tagmondatra: a „csak 5 km-t
            // futottam 10 helyett" öt kilométere MEGTÖRTÉNT – eddig az egész
            // mondat eltűnt, mert a tíz kilométer maradt el. A szorzójel
            // utáni szám kivétel: a „3x10 helyett" egésze a régi sorozat.
            java.util.regex.Matcher nm = java.util.regex.Pattern
                    .compile("(?<![\\dx×.,])\\d{1,3}(?:[.,]\\d{1,2})?"
                            + "(?:\\s?-?\\s?(?:km|m|perc|ora|kg|kilo)\\w*)?\\s*$")
                    .matcher(s.substring(a, h));
            if (nm.find()) a += nm.start();
            blank(q, a, h + 7);
            h = s.indexOf("helyett", h + 1);
        }
        s = new String(q);
        for (String w : new String[]{"nem ", "megsem ",
                // A „SEM" ugyanolyan tagadás, mint a „nem": a „ma sem
                // edzettem" mondatból eddig negyvenöt perces „egyéb mozgás"
                // lett – vagyis pont az ellenkezője annak, amit leírt. A
                // „semmi" nem esik ide: ott a tő után betű áll, nem szóköz.
                // A rövid „se" ugyanaz: „ott se voltam a teremben".
                "sem ", "se ",
                "kihagytam", "kimaradt", "elmarad",
                "lemondtam", "neztem", "neztuk", "rendeltem", "vettem", "berlet",
                // A MEGnéztem is nézés: a „megnéztem a maratont a tv-ben"
                // negyvenkét kilométeres futás lett a naplóban. Az OLVASÁS és
                // a RAJTSZÁM ugyanígy: a „megvan a rajtszámom a félmaratonra"
                // huszonegy kilométert írt be egy még meg nem futott versenyre.
                "megneztem", "megneztuk", "olvastam", "olvastuk", "cikket",
                "rajtszam", "rajtcsomag", "nevezesi", "streamelt",
                // Az edzés LEFÚJÁSA is elmaradás: „a futást lefújtam az eső
                // miatt" eddig negyvenöt perces futás lett.
                "lefujtam", "lefujtuk", "lemondtuk", "torolve lett",
                // A pihenőnap nem edzés. Megnevezett napok mellett ez különösen
                // fontos: a „szombaton túráztam 4 órát, vasárnap pihentem" két
                // NAPOT nevez meg, és eddig mindkettőre bekerült a négyórás
                // túra – vagyis nyolc óra mozgás abból, ami négy volt.
                "pihentem", "pihentunk", "pihenonap", "pihi",
                // Az angolul írt pihenőnap ugyanaz: az „edzés: rest day"
                // mellől eddig egy 45 perces „egyéb mozgás" került be – a
                // percek ráadásul az alvás órájából (6:45) jöttek.
                "rest day", "restday",
                // Az „alig mozogtam" pont az ellenkezőjét mondja annak, amit
                // a mozgás-szó jelent: a home office napjából eddig egy
                // negyvenöt perces „egyéb mozgás" lett a naplóban. Csak a
                // MOZGÁS-igével együtt tagadás: az „alig bírtam végigcsinálni
                // a 30 perc futást" megtörtént edzés, csak nehéz volt.
                "alig mozog", "alig mozdul", "alig edzet", "alig csinaltam",
                "semennyit", "semmit sem",
                // A kimondott NULLA is tagadás: a „nehéz nap: 10 óra munka,
                // semmi mozgás" mondatból eddig tízórás „egyéb mozgás" lett –
                // pont abból a szóból, amivel az ember azt mondja, hogy nem
                // mozgott.
                // A megnevezett nap melletti puszta „SEMMI" is tagadás: a
                // „hétfőn kondi 60 perc, kedden semmi" keddje eddig egy
                // MÁSODIK hatvanperces kondit kapott – pont arról a napról,
                // amelyikről az ember azt írta, hogy semmi.
                "semmi",
                "semmi mozgas", "semmilyen mozgas", "nulla mozgas",
                "semmi edzes", "semmilyen edzes", "nulla edzes",
                "semmi sport", "nem mozogtam", "nem mozdultam",
                // MÁS edzése nem az enyém: „a gyerek edzésén voltam" eddig
                // negyvenöt perces bejegyzés lett. A saját „edzésen voltam"
                // viszont marad.
                "gyerek edzes", "gyereket vittem", "fiam edzes", "lanyom edzes",
                "gyerek meccs",
                // A szórend szabad: a „vittem a gyereket edzésre" ugyanaz,
                // mint a „gyereket vittem" – eddig csak az egyik alak volt
                // kizárva, a másikból negyvenöt perces bejegyzés lett.
                "vittem a gyerek", "elvittem a gyerek", "kisertem a gyerek",
                "vittem a fiam", "vittem a lanyom", "edzesre vittem",
                "meccsre vittem", "gyereket kisertem",
                // A MAJDNEM nem történt meg: a „majdnem elmentem futni" és a
                // „kis híján elmentem edzeni" negyvenöt perces bejegyzés
                // lett. (A „majdnem 10 km-t futottam" viszont megtörtént –
                // ott a szó a SZÁMOT pontosítja, nem az igét tagadja.)
                "majdnem", "kis hijan", "kishijan",
                // Az „éppen csak benéztem a terembe" nem edzés: a terem szava
                // hatvanperces bejegyzést csinált belőle.
                "csak beneztem", "csak benezt", "eppen csak",
                // A VISSZAEMLÉKEZÉS nem ma történt: a „terhesség alatt
                // jógáztam" és a „régen sokat futottam" hónapokkal-évekkel
                // ezelőtti időkről szól – eddig mai bejegyzés lett belőlük.
                "regen ", "regebben", "annak idejen", "fiatalkoromban",
                "gyerekkoromban", "terhesseg alatt", "terhessegem alatt",
                // A MÚLT IDEJŰ akarat is meghiúsult szándék: az „akartam
                // futni, de esett" negyvenöt perces futás lett. Tagmondatra
                // szűkítve, hogy a mondat másik fele megmaradjon: az
                // „akartam még futni, de csak 3 km-t bírtam" három kilométere
                // valódi.
                "akartam", "akartunk", "szerettem volna", "szerettunk volna",
                // Az ELFELEJTETT edzés meg sem történt, a ZÁRVA tartó terem
                // pedig épp az oka annak, hogy nem lett belőle semmi.
                "elfelejtettem", "elfelejtettuk", "zarva", "be volt zar",
                // Az ELROMLOTT gép nem edzés: az „elromlott a futópad,
                // átültem a biciklire" futópadja negyvenöt perc futást írt
                // be – egy gépről, amin senki nem futott.
                "elromlott", "meghibasodott", "tonkrement",
                // A „MEHET a kemény edzés" engedély a jövőre, nem napló: a
                // „whoop recovery 85%, mehet a kemény edzés" negyvenöt
                // perces bejegyzést kapott – egy el sem kezdett napról. Csak
                // a saját tagmondatát viszi: a „megvolt a futás, mehet a
                // pihenés" futása marad.
                "mehet a ", "mehet egy ",
                // A „KÉSZÜLÖK" is csak a saját tagmondatát viszi: az „első
                // 5k versenyemre készülök, ma 3 km sikerült" hárma valódi
                // futás – a felkészülés szava eddig az egészet elvitte.
                "keszulok", "keszulunk"}) {
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
                // A „majdnem 10 km" mennyiséget pontosít, nem tagad.
                if (boundary && (w.equals("majdnem") || w.startsWith("kis hijan")
                        || w.equals("kishijan"))
                        && s.substring(Math.min(s.length(), p + w.length()))
                            .matches("^\\s*\\d.*")) boundary = false;
                // Az AKTÍV pihenőnap mozgás: az „aktív pihenőnap: 30 perc
                // séta" harminc perce eddig a pihenő szavával együtt eltűnt.
                if (boundary && w.startsWith("pihen")
                        && p >= 6 && s.startsWith("aktiv ", p - 6)) boundary = false;
                // A kimaradt BEJEGYZÉS nem kimaradt edzés: a „kimaradt a
                // tegnapi bejegyzés: futottam 8 km-t" pótlás – a futás
                // megtörtént, csak a napló maradt le róla. Az „elfelejtettem
                // beírni" ugyanez.
                if (boundary && (w.equals("kimaradt") || w.equals("kihagytam")
                        || w.startsWith("elfelejtettem"))
                        && s.substring(Math.min(s.length(), p))
                            .matches("^\\S+(\\s+\\S+){0,3}?\\s*"
                                + "(a\\s+)?\\w*(bejegyz|beir|napl|rogzit)\\w*.*"))
                    boundary = false;
                // A pihenő UTÁN már megint edzés van: a „két hét pihi után
                // visszaültem a bringára, 25 km" huszonöt kilométere eddig
                // gazdátlan távként FUTÁS lett, mert a pihi szava elvitte a
                // bringát. A szünet vége pont az ellenkezőjét mondja annak,
                // amit a szó önmagában.
                if (boundary && (w.startsWith("pih") || w.equals("rest day"))
                        && s.substring(Math.min(s.length(), p + w.length()))
                            .matches("^[a-z]*\\s+utan.*")) boundary = false;
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
                    boolean forward = w.equals("nem ") || w.equals("megsem ")
                            || w.equals("sem ") || w.equals("se ");
                    if (!forward)
                        while (a > 0 && s.charAt(a - 1) != ',' && s.charAt(a - 1) != '.'
                                && s.charAt(a - 1) != ';') a--;
                    int e = p;
                    while (e < s.length() && s.charAt(e) != ',' && s.charAt(e) != '.'
                            && s.charAt(e) != ';') e++;
                    // A kötőszó ÚJ állítást nyit, vessző nélkül is: a „focit
                    // néztem és futottam 30 percet" futása megtörtént, a
                    // „vettem egy cipőt és futottam 5 km-t" öt kilométere
                    // szintén. Enélkül a kötőszó utáni valódi edzés is eltűnt.
                    if (!forward) {
                        for (String c : LINKERS) {
                            int k = s.indexOf(c, p);
                            if (k >= 0 && k < e) e = k;
                            // A „de" HÁTRAFELÉ nem határ: az „uszodába
                            // mentem de zárva volt" zárva-ja az egész
                            // odautat tagadja – a de előtti fél nem élhet.
                            if (c.equals(" de ")) continue;
                            k = s.lastIndexOf(c, p);
                            if (k >= 0 && k >= a) a = k + c.length();
                        }
                    }
                    // A „nem futottam és kondiztam" kondija megtörtént: az „és"
                    // ÚJ állítást nyit, nem folytatja a tagadást. Csak akkor
                    // fut tovább a törlés, ha a másik fele is tagadva van
                    // („nem futottam és nem úsztam").
                    if (forward) {
                        int es = s.indexOf(" es ", p);
                        if (es >= 0 && es < e && !s.startsWith("nem ", es + 4)) e = es;
                        // A „de" ugyanígy új állítás: „az nem edzés de 6 km-t
                        // gyalogoltam" – a gyaloglás megtörtént.
                        int de = s.indexOf(" de ", p);
                        if (de >= 0 && de < e && !s.startsWith("nem ", de + 4)) e = de;
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
                // A mértékegység ragozott alakja is ide tartozik („8x400
                // métert futottam"): a toldalék nélkül a szorzat kiesett, és
                // a táv vagy elveszett, vagy tíz külön edzéssé esett szét.
                // A „kör" ugyanaz a szorzó, csak kimondva: a „3 kör 400 m"
                // ezerkétszáz méter. Eddig a kör-szám elveszett, és a naplóba
                // a táv harmada került. A kötőjeles „5-kor" (órakor) nem esik
                // ide: ott szóköz helyett kötőjel áll a szám után.
                .compile("(\\d{1,2})(?:\\s?[x×]\\s?|\\s+kor\\s+)"
                        + "(\\d{1,4}(?:[.,]\\d+)?)\\s?(km|meter[a-z]*|m)(?![a-z])")
                .matcher(s);
        while (m.find()) {
            int n;
            double d;
            try {
                n = Integer.parseInt(m.group(1));
                d = Double.parseDouble(m.group(2).replace(',', '.'));
            } catch (NumberFormatException e) { continue; }
            if (n < 2 || d <= 0) continue;
            // Az intervall-ismétlés RÖVID: kétszáz métertől néhány
            // kilométerig. A „8x 60 km" nem intervallum – összeszorozva
            // négyszáznyolcvan kilométeres futás lett belőle, huszonnégy
            // órás becsült idővel. Egymillió véletlen mondatból ez a
            // két eset maradt.
            if (m.group(3).equals("km") && d > 10) continue;
            double total = n * d;
            // A szorzat sem lehet életszerűtlen: egy edzés távja a
            // kerékpáros felső határig hihető, azon túl nem.
            if (total > 400 && m.group(3).equals("km")) continue;
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
     * Medencehossz → méter: „40 hosszt úsztam" ezer méter.
     *
     * Az úszók nem méterben mondják a távot, hanem hosszban, és a magyar
     * uszodák alapmérete 25 méter. Csak úszó mondatban váltunk – a „hossz"
     * önmagában bármi lehet –, és csak akkor, ha a szorzat elfér az eredeti
     * szövegrész helyén (a többi olvasó karakterpozíciókra épül).
     */
    private static void mergePoolLengths(char[] q) {
        String s = new String(q);
        if (!s.contains("usz") && !s.contains("medence")) return;
        boolean done = false;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d,.])(\\d{1,3})\\s?hossz[a-z]*(?![a-z])").matcher(s);
        while (m.find()) {
            int n;
            try { n = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException e) { continue; }
            if (n < 1 || n > 400) continue;
            String rep = (n * POOL_M) + " m";
            if (rep.length() > m.end() - m.start()) continue;
            blank(q, m.start(), m.end());
            for (int i = 0; i < rep.length(); i++) q[m.start() + i] = rep.charAt(i);
            done = true;
        }
        // A medence MÉRETE nem megtett táv: a „20 hosszt a 25 méteres
        // medencében" ötszáz méter, nem huszonöt. A jelzői alak („méteres")
        // sosem az edzés távja, ezért kivesszük.
        if (!done) return;
        java.util.regex.Matcher pm = java.util.regex.Pattern
                .compile("(?<![\\d,.])\\d{1,3}\\s?meteres(?![a-z])").matcher(new String(q));
        while (pm.find()) blank(q, pm.start(), pm.end());
        // A PUSZTA „medence" szándékosan nem úszás-tő (a medence testrész
        // is), de ha hosszban mért táv áll mellette, az uszodáé: a
        // „szállodai medence 20 hossz" fél kilométere eddig FUTÁS lett,
        // mert a méterek mellé nem került úszás-szó.
        String t = new String(q);
        int mp = t.indexOf("medence");
        if (mp >= 0 && (mp + 7 >= t.length() || !Character.isLetter(t.charAt(mp + 7)))) {
            String rep2 = "uszoda ";
            for (int i = 0; i < 7; i++) q[mp + i] = rep2.charAt(i);
        }
    }

    /** A magyar uszodák alapmérete – ennyi méter egy hossz. */
    private static final int POOL_M = 25;

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
            // Az „5 napja", a „két hete" és a „két hónapja" IDŐPONT, nem
            // időszak: nem öt napra osztjuk szét az edzést, hanem öt nappal
            // ezelőttre tesszük. A birtokos alakot itt engedjük tovább, hogy
            // a nap-kereső kaphassa meg.
            String word = wordAt(s, p);
            if (word.equals(unit + "ja") || word.equals(unit + "je")
                    || word.equals(unit + "e")) continue;
            // Az „N napos" JELZŐ, nem időszak: a „30 napos kihívás" a
            // kihívás hosszát mondja, a bejegyzés a mai napé – eddig harminc
            // napra terült szét az aznapi ötven guggolás.
            if (word.startsWith(unit + "os") || word.startsWith(unit + "es"))
                continue;
            // Az ESZKÖZHATÁROZÓS alak időpont-eltolás, nem időszak: a
            // „szalagszakadás után 6 héttel" a mai napról szól, mégis
            // negyvenkét napra terült szét a bejegyzés.
            if (word.startsWith(unit + "tel") || word.startsWith(unit + "vel")
                    || word.startsWith(unit + "pal") || word.startsWith(unit + "al"))
                continue;
            // A „3 hét UTÁN" nem időszak, hanem a kihagyás hossza: a „ma volt
            // az első edzésem 3 hét után, 30 perc könnyű futás" EGY mai edzés.
            // Eddig huszonegy napra terült szét, vagyis a mai nap kimaradt a
            // szériából, a heti terhelés meg három hétre hígult.
            // A kihagyás NEVE beékelődhet a szám és az „után" közé: „két hét
            // pihi után", „három hét betegség után", „egy hónap szabadság
            // után". Ugyanaz a mondat, ugyanaz a jelentés – egy szónyi rés
            // sem törheti meg.
            if (s.substring(end).matches(
                    "^\\s*(\\p{L}+\\s+)?(utan|mulva|kihagyas\\w*|szunet\\w*).*"))
                continue;
            int[] n = numberBefore(s, p, NUM_REACH);
            if (n == null) {
                // Szám nélkül csak a RAGOZOTT hét és hónap időszak („a héten",
                // „a hónapban") – a puszta „nap" nem, és a jelzőként álló
                // csupasz „hét" sem: a „ma deload hét van, edzettem 45
                // percet" mai edzése eddig hét napra terült szét.
                if ((mult == 7 || mult == 30) && word.length() > unit.length())
                    return new int[]{p, end, mult};
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

    /** Számjegy vagy kiírt számnév értéke egytől tízig (különben 0). */
    private static int numWord(String w) {
        String[][] map = {{"egy", "1"}, {"ket", "2"}, {"ketto", "2"}, {"harom", "3"},
                {"negy", "4"}, {"ot", "5"}, {"hat", "6"}, {"nyolc", "8"},
                {"kilenc", "9"}, {"tiz", "10"}};
        for (String[] m : map) if (m[0].equals(w)) return Integer.parseInt(m[1]);
        try { return Integer.parseInt(w); } catch (NumberFormatException e) { return 0; }
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
        // „5 napja futottam", „két hete kondi": a magyar leggyakoribb
        // visszatekintő alakja, és eddig mindegyik a MAI napra került. Az
        // étkezésnél ez régóta megvan (TimeHint), itt hiányzott.
        // A MÁR nem visszatekintés, hanem tartam: a „minden reggel 10 perc
        // nyújtás, már 2 hete" nem KÉT HETE történt egyszer, hanem két hete
        // tart. Eddig két héttel ezelőttre került a mai nyújtás – vagyis a
        // mai napra semmi, egy régi napra meg egy soha meg nem történt edzés.
        java.util.regex.Matcher ago = java.util.regex.Pattern
                .compile("(?<![\\d.,a-z])(?<!mar )(\\d{1,2}|egy|ket|ketto|harom|negy|ot|hat|"
                        // A „21 napja FOLYAMATOSAN" széria-hossz, nem dátum:
                        // a mai tízezer lépés eddig három hete ezelőttre
                        // került tőle.
                        + "nyolc|kilenc|tiz)\\s?(?:(nap|het|honap)(?:ja|je|e)"
                        + "(?!\\s*(?:folyamatosan|egymas|zsinorban|sorban))"
                        // A „3 nappal ezelőtt" ugyanaz a visszatekintés,
                        // eszközhatározóval – eddig a mai napra került.
                        + "|(nap|het|honap)(?:pal|tel|al)\\s+ezelott)\\b").matcher(s);
        if (ago.find()) {
            int n = numWord(ago.group(1));
            String unit2 = ago.group(2) != null ? ago.group(2) : ago.group(3);
            int mul = unit2.equals("nap") ? 1 : unit2.equals("het") ? 7 : 30;
            int back = n * mul;
            if (back >= 1 && back <= 365)
                return new int[]{ago.start(), ago.end(), back};
        }
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
                // A „km/h" SEBESSÉG, nem táv: a „futás 28 km/h" huszonnyolc
                // kilométeres futásnak számított – majdnem három óra a
                // naplóban egy tempó-adat miatt.
                // A PER-JEL a másik oldalon is tempót jelöl: az „5:30/km"
                // nem harminc kilométer. Eddig a perjel elé eső szám lett a
                // táv, és a „futás 10 km @ 5:30/km" mondatba bekerült egy
                // harminc kilométeres második futás is.
                if (p > 0 && s.charAt(p - 1) == '/') continue;
                int ue = p + unit.length();
                if (ue < s.length() && s.charAt(ue) == '/' ) continue;
                if (s.startsWith(" per ora", ue) || s.startsWith("h", ue)
                        && unit.equals("km")) continue;
                // A „KM-NÉL" helymegjelölés, nem megtett táv: a „leállt az
                // óra 3 km-nél, összesen kb 5 km lett" hármasa egy pont az
                // úton – mégis külön háromkilométeres futás lett belőle.
                if (s.startsWith("-nel", ue) || s.startsWith(" nel", ue)
                        || s.startsWith("nel", ue)) continue;
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

    /** Csak a terem HELYSZÍNE utal kondira, más semmi? */
    private static boolean onlyGymPlace(String s) {
        if (!s.contains("terem")) return false;
        String rest = s.replaceAll("\\p{L}*terem\\p{L}*", " ");
        Kind k = byId("kondi");
        if (k == null) return false;
        for (String w : k.words) if (rest.contains(w)) return false;
        return true;
    }

    /** Kimondott körszám a köredzésben („4 kör", „3 sorozat"), különben 1. */
    private static int roundsSaid(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![a-z0-9])(\\d{1,2})\\s*(?:kor|kort|korben|korrel"
                        + "|sorozat\\w*|szett\\w*|round\\w*)(?![a-z])").matcher(s);
        int n = m.find() ? Integer.parseInt(m.group(1)) : 1;
        return Math.max(1, Math.min(20, n));
    }

    /** A felismert gyakorlatok egyike adta-e ennek a mozgásformának a nevét? */
    private static boolean namedByLift(List<StrengthParse.Item> lifts, Kind k) {
        for (StrengthParse.Item it : lifts) {
            String n = Foods.norm(it.name);
            for (String w : k.words) if (n.contains(w)) return true;
        }
        return false;
    }

    /** Van-e olyan felismert sorozat, amelynek ennyi az ismétlésszáma? */
    private static boolean repsMatch(List<StrengthParse.Item> lifts, int n) {
        for (StrengthParse.Item it : lifts)
            for (StrengthParse.Set st : it.sets)
                if (st.reps == n) return true;
        return false;
    }

    /**
     * Az ÖSSZESÍTETT idő nem külön edzés.
     *
     * A „ma 90 percet edzettem összesen: 30 perc kondi, 60 perc futás"
     * kilencvenese a másik két szám összege – eddig mégis harmadik
     * időtartamként állt sorba, a kondi kapta meg, a harminc pedig elveszett.
     * Százötven perc mozgás került a naplóba kilencven helyett.
     *
     * Szándékosan szűk: kimondott „összesen" kell hozzá, és a számnak PONTOSAN
     * a többi összegének kell lennie. A „60 perc futás, 30 perc kondi, 30 perc
     * úszás" hatvanasa enélkül is összegnek látszana, pedig nem az.
     */
    private static void dropTotalTime(String s, List<int[]> mins) {
        if (mins.size() < 3) return;
        if (!s.contains("osszesen") && !s.contains("osszesitve")
                && !s.contains("osszesseg")) return;
        int sum = 0;
        for (int[] m : mins) sum += m[1];
        for (int i = 0; i < mins.size(); i++) {
            if (mins.get(i)[1] * 2 != sum) continue;
            mins.remove(i);
            return;
        }
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
        // A futó-appok kukacos alakja ugyanazt mondja: „10 km @ 5:30".
        m = java.util.regex.Pattern.compile("@\\s?(\\d{1,2}):([0-5]\\d)").matcher(s);
        if (m.find()) {
            double p = Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2)) / 60.0;
            if (p >= 2 && p <= 20) return p;
        }
        // A tempó-szó a szám ELŐTT is tempó: az „átlagtempóm 5:20 volt a
        // 10 kilométeren" öt-húsza percenkénti idő – enélkül a tíz
        // kilométer a mozgásforma átlagával számolódott. A jelzős
        // kilométer ugyanez: „4:45-ös kilométerekkel".
        m = java.util.regex.Pattern.compile("(?:tempo|atlag)\\w*\\s?:?\\s?"
                + "(\\d{1,2}):([0-5]\\d)"
                + "|(\\d{1,2}):([0-5]\\d)\\s?-?[oae]s\\s?(?:km|kilometer)")
                .matcher(s);
        if (m.find()) {
            String g1 = m.group(1) != null ? m.group(1) : m.group(3);
            String g2 = m.group(2) != null ? m.group(2) : m.group(4);
            double p = Integer.parseInt(g1) + Integer.parseInt(g2) / 60.0;
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
            // Tagmondathatáron megállunk: a „wod 20 perc, cool down 5 perc"
            // vesszője utáni cool már a KÖVETKEZŐ szakaszé, nem a húsz percé.
            while (i < s.length() && !Character.isLetter(s.charAt(i))) {
                char c = s.charAt(i);
                if (c == ',' || c == ';' || c == '.') return false;
                i++;
            }
            int e = i;
            while (e < s.length() && Character.isLetter(s.charAt(e))) e++;
            if (e == i) break;
            if (isWarmupWord(s.substring(i, e))) return true;
            i = e;
        }
        return false;
    }

    private static boolean isWarmupWord(String w) {
        // Az angolul naplózók warm up / cool down szavai ugyanazok: a
        // „warm up 5 perc, wod 20 perc" wod-ja eddig az öt percet kapta.
        return w.startsWith("bemelegit") || w.startsWith("levezet")
                || w.equals("warm") || w.equals("warmup")
                || w.equals("cool") || w.equals("cooldown")
                || w.equals("up") || w.equals("down");
    }

    /**
     * Ami idő-alakú, de nem az edzés hossza: az ALVÁS és a TEMPÓ.
     *
     * Az „aludtam 8 órát, reggel futottam 5 km-t" nyolc órája az éjszakáé,
     * mégis a futás hosszává vált – nyolcórás futás került a naplóba. A
     * „futás 5 km 24:59 tempó 5:00" ötös száma pedig SEBESSÉG (perc/km),
     * és ötszáz perces futássá duzzasztotta a huszonöt perceset.
     *
     * A bemelegítés-szabálytól abban tér el, hogy itt nincs darabszám-
     * feltétel: egyik sem edzésidő akkor sem, ha ez az EGYETLEN időtartam.
     */
    private static void dropSleepTimes(String s, List<int[]> mins) {
        List<int[]> keep = new ArrayList<>();
        for (int[] m : mins) if (!sleepWordAt(s, m)) keep.add(m);
        if (keep.size() < mins.size()) {
            mins.clear();
            mins.addAll(keep);
        }
    }

    /** Alvás- vagy tempó-szó áll-e az időtartam mellett. */
    private static boolean sleepWordAt(String s, int[] m) {
        int b = m[0];
        while (b > 0 && s.charAt(b - 1) == ' ') b--;
        int a = b;
        while (a > 0 && Character.isLetter(s.charAt(a - 1))) a--;
        String before = a < b ? s.substring(a, b) : "";
        if (isSleepWord(before)) return true;
        // A TEMPÓ csak ELŐLRŐL köt, és csak ÓRA-alakú számra: a „tempó 5:00"
        // öt perc egy kilométerre. Hátrafelé nem nézünk, mert a „24:59 tempó
        // 5:00" első száma a valódi idő; a „perc"-cel kiírt hossz pedig sosem
        // tempó („tempó 30 perc kondi" harminc perc kondi).
        if (isPaceWord(before) && m[0] < m[2] && m[2] <= s.length()
                && s.substring(m[0], m[2]).indexOf(':') >= 0) return true;
        int i = m[2];
        for (int w = 0; w < 2 && i < s.length(); w++) {
            while (i < s.length() && !Character.isLetter(s.charAt(i))) i++;
            int e = i;
            while (e < s.length() && Character.isLetter(s.charAt(e))) e++;
            if (e == i) break;
            if (isSleepWord(s.substring(i, e))) return true;
            if (isDeskWord(s, s.substring(i, e))) return true;
            i = e;
        }
        return false;
    }

    /**
     * Alvás- vagy ÜLŐ elfoglaltság szava: ami mellette áll, az nem edzésidő.
     *
     * A „hosszú nap, 11 óra munka, este 20 perc nyújtás" tizenegy órája a
     * MUNKÁÉ – eddig a nyújtás kapta meg, vagyis tizenegy óra jóga került a
     * naplóba, a valódi húsz perc meg elveszett. (A „kerti munka" saját
     * mozgásforma, azt a szótöve viszi.)
     */
    private static boolean isSleepWord(String w) {
        return w.startsWith("alud") || w.startsWith("alvas") || w.startsWith("alszo");
    }

    /**
     * ÜLŐ elfoglaltság szava: ami mellette áll, az nem edzésidő.
     *
     * A „hosszú nap, 11 óra munka, este 20 perc nyújtás" tizenegy órája a
     * MUNKÁÉ – eddig a nyújtás kapta meg, vagyis tizenegy óra jóga került a
     * naplóba, a valódi húsz perc meg elveszett. A KERTI és a FIZIKAI munka
     * viszont mozgás: ott a szótő a mozgásformát is kimondja, és a hossz az
     * övé.
     *
     * Csak a szám UTÁN álló szó számít – magyarul így birtokolja az időt a
     * tevékenység („11 óra munka"). Elöl állva csak zaj: a „munka 30 perc
     * kondi" harminc perce a kondié.
     */
    private static boolean isDeskWord(String s, String w) {
        boolean desk = w.equals("munka") || w.equals("munkaban") || w.equals("munkat")
                || w.equals("melo") || w.equals("meloban") || w.startsWith("utaz")
                || w.startsWith("vezetes")
                // Ugyanez a nap többi ÜLŐ órájával: a „2 óra tv, 30 perc
                // séta" két órája a tévéé, a „8 óra ülés az irodában, este
                // 30 perc futás" nyolc órája az ülésé. Mind ugyanaz a hiba:
                // a mozdulatlan idő a mozgás nevére íródott.
                || w.equals("tv") || w.startsWith("tevez") || w.startsWith("ules")
                || w.startsWith("tanulas") || w.startsWith("olvasas")
                || w.startsWith("fozes") || w.startsWith("meeting")
                || w.startsWith("ertekezlet") || w.startsWith("gepeles")
                || w.startsWith("telefonal")
                // A PIHENÉS is a mozdulatlan idő neve: az „1 óra pihenés után
                // 30 perc bringa" egy óráját eddig a bringa kapta meg.
                || w.startsWith("pihen") || w.startsWith("szunet");
        return desk && !s.contains("kerti munka") && !s.contains("fizikai munka")
                && !s.contains("haz koruli");
    }

    /** Sebességet jelölő szó: ami utána áll, az perc/km, nem perc. */
    private static boolean isPaceWord(String w) {
        return w.startsWith("tempo") || w.startsWith("iram");
    }

    /**
     * Óra:perc:másodperc alak: „futás 1:05:23".
     *
     * Ezt másolja ki az ember az órája kijelzőjéről. A KÉTRÉSZŰ alak
     * szándékosan kimarad: a „18:00" időpont, nem tizennyolc perc – és egy
     * időpontból számolt edzéshossz csendben rossz lenne.
     */
    private static void findClockTimes(String s, String beforeBlank, List<int[]> out) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d:])(\\d{1,2}):([0-5]\\d):([0-5]\\d)(?![\\d:])").matcher(s);
        while (m.find()) {
            int min = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
            if (Integer.parseInt(m.group(3)) >= 30) min++;
            if (min >= 1 && min <= 24 * 60) out.add(new int[]{m.start(), min, m.end(), 0});
        }
        // Verseny-idő két taggal: „5 km 22:30", „félmaraton 1:58". Csak táv
        // mellett merjük időtartamnak venni – e nélkül a kettőspontos szám
        // inkább órát jelent a falon. A futók írásmódja szerint a 10 alatti
        // első tag óra:perc (1:58), a többi perc:mp (22:30, 42:10).
        boolean race = beforeBlank.contains("km") || beforeBlank.contains("meter")
                || beforeBlank.contains("maraton")
                // A puszta „m" is táv, ha szám áll előtte („1500 m-t").
                || java.util.regex.Pattern.compile("\\d\\s?m(?![a-z])")
                        .matcher(beforeBlank).find();
        if (!race) return;
        m = java.util.regex.Pattern
                .compile("(?<![\\d:])(\\d{1,2}):([0-5]\\d)(?![\\d:])").matcher(s);
        while (m.find()) {
            // A napszakos vagy -kor-os alak a NAP órája, nem időtartam:
            // „10 km reggel 7:30", „10 km-t futottam 18:30-kor".
            if (s.startsWith("-kor", m.end()) || s.startsWith(" kor", m.end())) continue;
            int first = Integer.parseInt(m.group(1));
            // A tempó sem időtartam: „5:30-as tempóval", „4:45/km".
            // A KUKAC is tempót jelöl: a futó-appok „10 km @ 5:30" alakja
            // ugyanaz, mint az „5:30-as tempóval". Enélkül öt és fél ÓRA
            // került a naplóba egy ötvenöt perces futásra.
            if (m.start() >= 1 && s.substring(0, m.start()).trim().endsWith("@")) continue;
            // A TEMPÓ-SZÓ a szám ELŐTT is tempót jelent: az „átlagtempóm
            // 5:20 volt a 10 kilométeren" öt-húsza percenkénti idő – eddig
            // öt óra húsz perces futás lett belőle.
            String prevWord = "";
            int pe = m.start();
            while (pe > 0 && s.charAt(pe - 1) == ' ') pe--;
            int pa = pe;
            while (pa > 0 && Character.isLetter(s.charAt(pa - 1))) pa--;
            prevWord = s.substring(pa, pe);
            // Az „ÁTLAG" is tempó-szó a perc:mp előtt: az „új cipőben 12 km,
            // átlag 5:40" öt-negyvene percenkénti idő – eddig öt óra
            // negyven perces futás lett belőle.
            if (prevWord.contains("tempo") || prevWord.startsWith("iram")
                    || prevWord.startsWith("atlag")) continue;
            // Az „-os" rag is jelzős tempó: a „4:45-os kilométerekkel" a
            // kilométerenkénti idő, nem négy és háromnegyed óra.
            if (s.startsWith("-as", m.end()) || s.startsWith("-es", m.end())
                    || s.startsWith("-os", m.end())
                    || s.startsWith("/km", m.end())
                    // A „ tempo" utótag is csak tempó-tartományú számra:
                    // a „24:59 tempó 5:00" első száma a valódi idő, a tempó a
                    // KÖVETKEZŐ számhoz tartozik.
                    || (first < 10 && s.regionMatches(m.end(), " tempo", 0, 6))
                    // A tágabb ablak CSAK tempó-tartományú számra él (10 perc
                    // alatti perc:mp). Enélkül a „futás 5 km 24:59 tempó 5:00"
                    // huszonöt perce is kiesett – pedig az a valódi idő, és a
                    // helyére a becsült harminc perc lépett.
                    || (first < 10 && m.end() + 12 <= s.length()
                        && s.substring(m.end(), m.end() + 12).contains("tempo"))) continue;
            boolean daypart = false;
            for (String dw : new String[]{"reggel", "este", "delutan", "delelott",
                    "hajnal", "ejjel"})
                if (beforeBlank.contains(dw)) { daypart = true; break; }
            if (first < 10 && daypart) continue;
            // A MÉTERES táv mellett az óra:perc értelmezés képtelenség: az
            // „úszóverseny: 100 m gyors 1:12" hetvenkét PERCES úszás lett
            // száz méterre – a rövidtávú idő perc:mp, nem óra:perc. Csak
            // méterben (km nélkül) kiírt táv mellett él.
            boolean meterOnly = !beforeBlank.contains("km")
                    && !beforeBlank.contains("maraton")
                    && java.util.regex.Pattern.compile("\\d\\s?m(?![a-z])")
                            .matcher(beforeBlank).find();
            int min = first < 10
                    ? (meterOnly
                        ? Math.max(1, first + (Integer.parseInt(m.group(2)) >= 30 ? 1 : 0))
                        : first * 60 + Integer.parseInt(m.group(2)))
                    : first + (Integer.parseInt(m.group(2)) >= 30 ? 1 : 0);
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
            // A TIZEDES óra is óra: az „1,5h" másfél óra – eddig a vessző
            // elvágta, és az „5h"-ból háromszáz perc lett.
            int fracMin = 0;
            if (i + 1 < s.length() && (s.charAt(i) == ',' || s.charAt(i) == '.')
                    && Character.isDigit(s.charAt(i + 1))
                    && (i + 2 >= s.length() || !Character.isDigit(s.charAt(i + 2)))) {
                fracMin = (s.charAt(i + 1) - '0') * 6;
                i += 2;
            }
            int j = i;
            while (j < s.length() && s.charAt(j) == ' ') j++;
            if (j >= s.length()) continue;
            char u = s.charAt(j);
            int val;
            if (u == 'h') {
                if (num > 24) continue;
                val = num * 60 + fracMin;
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
    private static List<int[]> findMinutes(char[] q, String beforeBlank) {
        String s = new String(q);
        List<int[]> out = new ArrayList<>();
        // A verseny-idő felismeréséhez az EREDETI szöveg kell: a távokat ekkor
        // már kitakartuk, a „km" szó nélkül pedig nem tudnánk, hogy a
        // kettőspontos szám időtartam.
        findClockTimes(s, beforeBlank, out);
        findShortTimes(s, out);
        for (String unit : new String[]{"perc", "ora"}) {
            int from = 0;
            while (true) {
                int p = s.indexOf(unit, from);
                if (p < 0) break;
                from = p + 1;
                // A „7 órakor" időpont, nem hét óra hosszú edzés.
                if (unit.equals("ora") && s.startsWith("orakor", p)) continue;
                // A „30 perccel" és az „1 órával" ELTOLÁS, nem időtartam: a
                // „reggeli után 30 perccel edzettem 45 percet" edzése
                // negyvenöt perc – eddig a harminc perces eltolást kapta meg.
                if (s.startsWith(unit + "cel", p) || s.startsWith(unit + "vel", p)
                        || s.startsWith(unit + "val", p)) continue;
                // A „PERC/KM" tempó, nem időtartam: a „6 perc/km-es tempóval
                // 10 km" hatperces futás lett – tíz kilométerre.
                if (s.startsWith(unit + "/", p)) continue;
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
                    // Külön írva is ugyanaz: a „három negyed óra" háromnegyed
                    // óra, nem három darab negyedórás edzés. A „három" enélkül
                    // szorzószámként HÁROM alkalmat csinált belőle.
                    if (frac == 15) {
                        int b0 = wsPos;
                        while (b0 > 0 && s.charAt(b0 - 1) == ' ') b0--;
                        int w0 = b0;
                        while (w0 > 0 && Character.isLetter(s.charAt(w0 - 1))) w0--;
                        // Számjeggyel írva is: „3 negyed óra".
                        if (w0 == b0)
                            while (w0 > 0 && Character.isDigit(s.charAt(w0 - 1))) w0--;
                        String before = s.substring(w0, b0);
                        if (before.equals("harom") || before.equals("3")) {
                            frac = 45;
                            wsPos = w0;
                        }
                    }
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
     * A szokás tagmondata kitakarva – ha van mellette megtörtént fél.
     *
     * A „hetente háromszor edzek, ma 45 perc kondi volt" első fele a heti
     * rendet írja le, a második egy valódi edzést. A szokás-szabály eddig az
     * egész mondatra élt, és a negyvenöt perc elveszett vele. Múlt idejű
     * tagmondat nélkül semmit nem takarunk ki: a puszta „hetente futok"
     * továbbra sem napló.
     */
    private static void stripHabitClause(char[] q) {
        String s = new String(q);
        if (!s.matches(".*(?<![a-z])\\w{3,}(?:tam|tem|tunk)(?![a-z]).*")
                && !s.contains(" volt")) return;
        int a = 0;
        while (a < s.length()) {
            int e = a;
            while (e < s.length() && s.charAt(e) != ',' && s.charAt(e) != ';') e++;
            if (habitClause(s.substring(a, e))) blank(q, a, e);
            a = e + 1;
        }
    }

    /** Gyakoriság-szó + jelen idő: a tagmondat a heti rendről szól. */
    private static boolean habitClause(String cl) {
        String rest = null;
        for (String w : new String[]{"hetente", "naponta", "havonta", "masodnaponta",
                "minden nap", "minden masodnap", "szoktam", "szoktunk",
                // A NAPSZAKKAL mondott szokás ugyanaz: a „minden reggel
                // 10 perc nyújtás, már 2 hete" nem egy megtörtént edzés –
                // eddig egy tíz perces bejegyzés lett belőle, ráadásul két
                // héttel EZELŐTTRE, mert a „2 hete" időpontnak látszott.
                "minden reggel", "minden este", "minden delutan",
                "minden delelott", "minden hajnalban", "minden ebedszunet",
                "altalaban", "rendszeresen"})
            if (cl.contains(w)) { rest = cl.replace(w, " "); break; }
        // A gyakoriság-szót ki kell venni a múlt idő vizsgálata elől: a
        // „szoktam" maga is -tam végű, pedig épp a szokás szava.
        return rest != null
                && !rest.matches(".*(?<![a-z])\\w{3,}(?:tam|tem|tunk)(?![a-z]).*");
    }

    /**
     * Felsorolás-sorszám kitakarva: „1.", „2)", „Nap 3:".
     *
     * A sor elejére írt sorszám darabszámnak látszott: az „1. 5 km futás /
     * 2. 30 perc kondi" második pontjából KÉT kondi-edzés lett, a megosztott
     * terv „Nap 2:" fejlécéből pedig ugyanígy kettő.
     */
    private static String maskListMarkers(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?m)^[ \\t\\-*•]*(\\d{1,2})[.)][ \\t]").matcher(text);
        while (m.find()) {
            for (int i = m.start(1); i < m.end(1) + 1; i++) sb.setCharAt(i, ' ');
            // A sorhatár helyére VESSZŐ kerül, nem üresség: a tagmondat-határ
            // dönti el, melyik mozgáshoz tartozik a mellette álló időtartam.
            // Enélkül az „1. 5 km futás / 2. 30 perc kondi" harminc perce
            // gazdátlanul maradt, és a kondi a szokásos hatvan percét kapta.
            if (m.start(1) > 0) sb.setCharAt(m.start(1), ',');
        }
        m = java.util.regex.Pattern
                .compile("(?i)(?<![a-zöüó])nap\\s?(\\d{1,2})\\s*[:.]").matcher(text);
        while (m.find())
            for (int i = m.start(1); i < m.end(1); i++) sb.setCharAt(i, ' ');
        // A MONDAT KÖZBENI sorszám is sorszám: a „letudtam a heti 3. futást"
        // hármasa a hét HARMADIK futása, nem három futás – eddig három
        // bejegyzés lett belőle, hét napra szétosztva. A magyar a sorszámot
        // ponttal írja, a darabszámot pont nélkül; a pont utáni betű zárja
        // ki a tizedes törtet.
        m = java.util.regex.Pattern
                .compile("(?<![\\d.,])(\\d{1,2})\\.(?=\\s?\\p{L})").matcher(text);
        while (m.find())
            for (int i = m.start(1); i < m.end(1) + 1; i++) sb.setCharAt(i, ' ');
        return sb.toString();
    }

    /**
     * Kiírt számnév-pár számjeggyé: „húsz-huszonöt perc" → „20-25 perc".
     *
     * A számjegyes tartományt („20-25 perc") már értettük, a kiírtat nem: a
     * „húsz-huszonöt perc kondi" HÚSZ külön edzés lett, húsz napra osztva,
     * egyenként huszonöt perccel. Nyolc óra mozgás abból, ami húsz perc.
     *
     * A számokat jobbra igazítva írjuk be, hogy a mértékegység közvetlenül a
     * második szám mögött maradjon – a többi szabály a KÖZ-re épül.
     */
    private static void wordRangeToDigits(char[] q) {
        String s = new String(q);
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) != '-') { i++; continue; }
            int lb = i;
            while (lb > 0 && Character.isLetter(s.charAt(lb - 1))) lb--;
            int re = i + 1;
            while (re < s.length() && Character.isLetter(s.charAt(re))) re++;
            String lo = numWordDigits(s.substring(lb, i));
            String hi = numWordDigits(s.substring(i + 1, re));
            if (lo == null || hi == null) { i++; continue; }
            String rep = lo + "-" + hi;
            if (rep.length() > re - lb) { i++; continue; }
            int start = re - rep.length();
            for (int k = lb; k < start; k++) q[k] = ' ';
            for (int k = 0; k < rep.length(); k++) q[start + k] = rep.charAt(k);
            i = re;
        }
    }

    /** A számnév számjegyes alakja, vagy null, ha nem számnév. */
    private static String numWordDigits(String w) {
        if (w.isEmpty()) return null;
        for (String[] n : NUM_WORDS) if (n[0].equals(w)) return n[1];
        return null;
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
     * A PANASZ nem edzés: „ropog a térdem guggolásnál".
     *
     * A mozgás neve itt csak a körülményt mondja meg – azt, hogy MIKOR
     * jelentkezik a panasz –, nem azt, hogy megtörtént egy edzés. Eddig egy
     * hatvanperces kondi-bejegyzés lett belőle, és beleszámított a heti
     * terhelésbe is.
     *
     * A szám a védőkorlát: ha a tagmondatban ott az időtartam vagy a táv
     * („20 perc futás után fájt a térdem"), akkor az edzés tényleg
     * megtörtént, csak fájt utána – az marad.
     */
    private static void stripComplaintClauses(char[] q) {
        String s = new String(q);
        int a = 0;
        while (a < s.length()) {
            int e = a;
            while (e < s.length() && s.charAt(e) != ',' && s.charAt(e) != '.'
                    && s.charAt(e) != ';') e++;
            String cl = s.substring(a, e);
            // A JELZŐS szám (42-es cipő, 3-as szint) nem védi meg a
            // panasz-tagmondatot: az „a 42-es cipőm szorít futásnál"
            // negyvenöt perces futást írt be – egy cipő-panaszból. Csak a
            // mennyiség-szám (20 perc, 5 km) bizonyíték.
            boolean digit = java.util.regex.Pattern
                    .compile("(?>\\d+)(?![\\d])(?!\\s?-?[eao]s(?![a-z]))")
                    .matcher(cl).find();
            if (!digit && complains(cl)) blank(q, a, e);
            a = e + 1;
        }
    }

    /** Panasz-tagmondat: fájdalomról, sérülésről vagy ízületi hangról szól. */
    private static boolean complains(String cl) {
        String t = " " + cl.replaceAll("[^a-z0-9]", " ") + " ";
        for (String w : new String[]{"faj", "fajt", "fajnak", "fajos", "huzodas",
                "huzodast", "huzodott", "huzodtam", "megrandult", "berandult",
                "serules", "serultem", "megserult", "ropog", "recseg", "kattog",
                // A SZORÍTÓ cipő is panasz: „a 42-es cipőm szorít futásnál"
                // negyvenöt perces futást írt be – egy cipő-panaszból.
                "sajog", "nyilall", "nyilallt", "zsibbad", "elzsibbadt",
                "szorit"})
            if (t.contains(" " + w + " ")) return true;
        for (String w : new String[]{"fajdalm", "megfajdul", "gyulladt", "gyulladas",
                "belenyilall", "szakadas", "elpattant", "megpattant",
                // Az instabilitás és a merevség is panasz: a „gyakran húzódik
                // a combom sprintnél" eddig negyvenöt perces futás lett.
                "huzodik", "kifordul", "kibicsaklik", "megbicsaklik", "instabil",
                // Az IZOMLÁZ is panasz-mondat: az „izomláz van rendesen a
                // tegnapi lábnaptól" egy hatvanperces kondit írt TEGNAPRA –
                // pedig azt az edzést az ember már beírta, amikor megtörtént.
                "izomlaz"})
            if (cl.contains(w)) return true;
        return false;
    }

    /**
     * Óra-tartomány → időtartam: „18:00-19:30 foci" másfél óra.
     *
     * A naptárból másolt sor így néz ki. Eddig egyetlen szabály sem értette:
     * a tizenkilenc-harmincból HARMINC darab kilencvenperces foci lett,
     * harminc napra elosztva – negyvenöt óra mozgás egyetlen sorból.
     *
     * A csere csak akkor történik meg, ha az új szöveg elfér a régi helyén –
     * a többi olvasó karakterpozíciókra épül.
     */
    private static void mergeClockRange(char[] q) {
        String s = new String(q);
        java.util.regex.Matcher m = java.util.regex.Pattern
                // A magyar rag is ide tartozik: a „18:00-tól 19:30-ig kondi"
                // ugyanaz a másfél óra, csak kimondva – eddig az alapértelmezett
                // hatvan perc került a naplóba helyette.
                .compile("(?<![\\d:])(\\d{1,2}):(\\d{2})(?:-?tol)?\\s?-?\\s?"
                        + "(\\d{1,2}):(\\d{2})(?:-?ig)?(?![\\d:])")
                .matcher(s);
        while (m.find()) {
            int h1, m1, h2, m2;
            try {
                h1 = Integer.parseInt(m.group(1)); m1 = Integer.parseInt(m.group(2));
                h2 = Integer.parseInt(m.group(3)); m2 = Integer.parseInt(m.group(4));
            } catch (NumberFormatException e) { continue; }
            if (h1 > 23 || h2 > 23 || m1 > 59 || m2 > 59) continue;
            int mins = (h2 * 60 + m2) - (h1 * 60 + m1);
            // Éjfél átlépése: az esti edzés hajnalban ér véget – de csak
            // életszerű hosszig.
            if (mins < 0) mins += 24 * 60;
            if (mins < 5 || mins > 600) continue;
            String rep = mins + " perc";
            if (rep.length() > m.end() - m.start()) continue;
            blank(q, m.start(), m.end());
            for (int i = 0; i < rep.length(); i++) q[m.start() + i] = rep.charAt(i);
        }
    }

    /**
     * Emelet → perc: „lépcsőztem 20 emeletet" tíz perc.
     *
     * A lépcsőzést emeletben mondjuk, nem percben – az app viszont a
     * mozgásforma alapértelmezett hosszát adta hozzá, vagyis húsz emeletből
     * MÁSFÉL ÓRA gyaloglás lett. Egy emelet lendületes tempóval nagyjából
     * fél perc; ennél kevesebbet nem írunk, mert a mozgás akkor is megvolt.
     */
    private static void mergeFloors(char[] q) {
        String s = new String(q);
        if (!s.contains("lepcso") && !s.contains("emelet")) return;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?<![\\d.,])(\\d{1,3})\\s?emelet\\w*").matcher(s);
        while (m.find()) {
            int floors;
            try { floors = Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { continue; }
            if (floors < 1 || floors > 300) continue;
            String rep = Math.max(2, Math.round(floors * 0.5f)) + " perc";
            if (rep.length() > m.end() - m.start()) continue;
            blank(q, m.start(), m.end());
            for (int i = 0; i < rep.length(); i++) q[m.start() + i] = rep.charAt(i);
        }
    }

    /** A „felső/alsó hát" testtáj – a benne lakó számnév kitakarva. */
    private static void maskBackNoun(char[] q) {
        String s = new String(q);
        // A „hát nap:" edzésnap NEVE, nem hat nap: a „hát nap: húzódzkodás,
        // evezés…" hatnapos időszakká vált, és a húzódzkodás hat ismétlést
        // kapott. A kettőspont dönt – a „hat nap alatt 6 edzés" marad hat nap.
        java.util.regex.Matcher dm = java.util.regex.Pattern
                .compile("(?<![a-z])hat(?=\\s+nap\\s*[:–-])").matcher(s);
        while (dm.find()) blank(q, dm.start(), dm.end());
        s = new String(q);
        for (String w : new String[]{"felso hat", "also hat"}) {
            int p = s.indexOf(w);
            while (p >= 0) {
                int h = p + w.length() - 3;
                if (h + 3 >= s.length() || !Character.isLetter(s.charAt(h + 3)))
                    blank(q, h, h + 3);
                p = s.indexOf(w, p + 1);
            }
        }
    }

    /**
     * A SZOKÁS KEZDETE nem egy edzés: „három hónapja kezdtem el edzeni".
     *
     * A mondat egy időpontról szól – arról, hogy mióta sportol az ember –,
     * nem egy megtörtént alkalomról. Eddig kilencven nappal ezelőttre bekerült
     * egy negyvenöt perces „egyéb mozgás", vagyis egy soha meg nem történt
     * edzés, ráadásul a sorozat- és a heti statisztikába is.
     *
     * Csak akkor lép be, ha a tagmondat egy „ennyi ideje" alakot tartalmaz
     * (ez teszi visszatekintéssé), és nincs benne se időtartam, se táv – a
     * „két hete kezdtem el futni, azóta 40 km" második fele megmarad.
     */
    private static void stripStartOfHabit(char[] q) {
        String s = new String(q);
        if (!s.contains("kezdt")) return;
        int a = 0;
        while (a < s.length()) {
            int e = a;
            while (e < s.length() && s.charAt(e) != ',' && s.charAt(e) != '.'
                    && s.charAt(e) != ';') e++;
            String cl = s.substring(a, e);
            if (cl.contains("kezdt")
                    && cl.matches(".*\\b(\\d{1,3}|egy|ket|ketto|harom|negy|ot|hat|het|"
                            + "nyolc|kilenc|tiz)\\s?(napja|hete|honapja|eve)\\b.*")
                    && !cl.matches(".*\\d\\s?(perc|ora|km|m|meter|kilometer).*"))
                blank(q, a, e);
            a = e + 1;
        }
    }

    /** Alany, aki nem én vagyok: az ő mozgása nem az én naplóm. */
    private static final String[] OTHER_SUBJECT = {
            "fiam", "lanyom", "ferjem", "felesegem", "parom", "testverem", "ocsem",
            "batyam", "hugom", "novverem", "anyam", "apam", "anyukam", "apukam",
            "kollegam", "fonokom", "szomszedom", "kutyam", "csapat", "csapatom",
            "gyerekek", "gyerekem", "unokam", "baratom", "baratnom", "edzom",
            // A névmás a legrövidebb alany: az „ő kardiózott, én súlyzóztam"
            // kardiója a párom edzése volt, mégis bekerült a naplómba.
            // Ékezet nélkül egyetlen betű, de egész szóként a magyar
            // mondatban szinte csak névmás lehet.
            "o", "ok"};

    /**
     * MÁS mozgása: „a fiam focizott, én csak néztem".
     *
     * A „néztem" tagadó szó csak a SAJÁT tagmondatát törli – a focit a másik
     * tagmondat mondta ki, és eddig kilencven perces bejegyzés lett belőle.
     * Itt az alany dönt: ha a tagmondatban harmadik személy áll (a fiam, a
     * párom, a csapat) és NINCS benne első személyű ige, akkor a mozgás nem az
     * enyém.
     *
     * A birtokos ragos alak („a fiammal futottam") nem egész szó, tehát nem
     * esik ide; az első személyű ige („a fiam és én futottunk") pedig kivédi a
     * közös edzés törlését – abban tényleg benne vagyok.
     */
    private static void stripOtherPerson(char[] q) {
        String s = new String(q);
        int a = 0;
        while (a < s.length()) {
            int e = a;
            while (e < s.length() && s.charAt(e) != ',' && s.charAt(e) != '.'
                    && s.charAt(e) != ';') e++;
            String cl = s.substring(a, e);
            if (!firstPerson(cl) && otherSubject(cl)) blank(q, a, e);
            a = e + 1;
        }
    }

    /** Van-e a tagmondatban első személyű (rám vonatkozó) alak? */
    private static boolean firstPerson(String cl) {
        String t = " " + cl.replaceAll("[^a-z0-9]", " ") + " ";
        if (cl.matches(".*\\b\\w{3,}(tam|tem|tunk)\\b.*")) return true;
        for (String w : new String[]{"en", "velem", "engem", "nekem", "magam", "sajat"})
            if (t.contains(" " + w + " ")) return true;
        return false;
    }

    /**
     * MÁSÉ az egész mondat? („A srácok csináltak 50 fekvőtámaszt.")
     *
     * A mozgás-oldal tagmondatonként takarja ki az idegen alanyt; a sorozat
     * viszont az egész mondatból épül, ezért ott egyben kell megkérdezni.
     * Enélkül az erősítő naplóba került, amit valaki MÁS csinált – a
     * rekordok és a progresszió-javaslat közé.
     */
    public static boolean someoneElsesDoing(String text) {
        if (text == null) return false;
        String s = Foods.norm(text);
        boolean other = false;
        for (String cl : s.split("[,.;]")) {
            if (cl.trim().isEmpty()) continue;
            if (firstPerson(cl)) return false;
            if (otherSubject(cl)) other = true;
        }
        return other;
    }

    /** Harmadik személyű alany egész szóként. */
    private static boolean otherSubject(String cl) {
        String t = " " + cl.replaceAll("[^a-z0-9]", " ") + " ";
        for (String w : OTHER_SUBJECT) if (t.contains(" " + w + " ")) return true;
        // A TÖBBES SZÁM HARMADIK SZEMÉLY magától is elárulja magát: az „ők
        // futottak 10 km-t" és a „csináltak 50 fekvőtámaszt" nem az én
        // naplóm – a magyar az alanyt úgyis elhagyja, tehát az ige a jel.
        //
        // De csak a CSELEKVÉS igéi, felsorolva. A magyar a saját testrészeimre
        // is többes szám harmadik személyt használ: az „elfáradtak a lábaim"
        // és a „jól sikerültek a sorozatok" ugyanígy néz ki, és egy általános
        // -tak/-tek szabály elvitte volna a mellettük álló valódi edzést is.
        for (String v : OTHER_VERB) if (t.contains(" " + v + " ")) return true;
        return false;
    }

    /** Cselekvés-igék többes szám harmadik személyben: „futottak", „ettek". */
    private static final String[] OTHER_VERB = {
            "futottak", "futnak", "mentek", "jartak", "edzettek", "edzenek",
            "csinaltak", "nyomtak", "huztak", "toltak", "usztak", "tekertek",
            "bicikliztek", "gyalogoltak", "setaltak", "jatszottak", "tornaztak",
            "sportoltak", "gyakoroltak", "ettek", "ittak", "guggoltak", "emeltek",
            "megettek", "megittak", "leguggoltak", "kinyomtak",
    };

    /**
     * Hátravetett tagadás: „futni nem voltam", „úszni nem mentem".
     *
     * A „nem" előre töröl – ez a „nem futottam" alakra jó. Magyarul viszont
     * ugyanolyan gyakori a fordított szórend, és ott a mozgás a tagadás
     * ELŐTT áll: eddig minden ilyen mondat bejegyzést csinált abból, amit az
     * ember épp NEM csinált meg.
     *
     * Csak akkor lép működésbe, ha a „nem" után a megvalósulást tagadó ige
     * áll – a „nem szeretek futni" nem erről szól. A visszafelé törlés a
     * tagmondat elején és a kötőszónál megáll, hogy a „…kondi, de futni nem
     * voltam" kondija megmaradjon.
     */
    private static void stripBackwardNegation(char[] q) {
        String s = new String(q);
        int p = s.indexOf("nem ");
        while (p >= 0) {
            if (p == 0 || !Character.isLetter(s.charAt(p - 1))) {
                boolean undone = false;
                for (String v : new String[]{"voltam", "volt", "mentem", "ment",
                        "jutottam", "sikerult", "tudtam", "birtam", "ertem ra",
                        "jott ossze", "lett belole",
                        // „Csak átöltöztem, edzés nem lett": a hátravetett
                        // tagadás leggyakoribb magyar alakja hiányzott, és
                        // negyvenöt perces bejegyzés lett a semmiből.
                        "lett", "keszult", "valt belole"})
                    if (s.startsWith(v, p + 4)) { undone = true; break; }
                if (undone) {
                    int a = p;
                    while (a > 0 && s.charAt(a - 1) != ',' && s.charAt(a - 1) != '.'
                            && s.charAt(a - 1) != ';') a--;
                    for (String c : new String[]{" de ", " viszont ", " azonban ",
                            " majd ", " aztan ", " utana ", " es "}) {
                        int k = s.lastIndexOf(c, p);
                        if (k >= 0 && k >= a) a = k + c.length();
                    }
                    if (a < p) blank(q, a, p);
                }
            }
            p = s.indexOf("nem ", p + 1);
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
        // Előre nézve: „30-30 perc" – az időtartam a MÁSODIK tagra van kötve.
        int dash = b - 1;
        if (dash >= 0 && s.charAt(dash) == '-') {
            int start = dash - num.length();
            if (start >= 0 && s.substring(start, dash).equals(num)
                    && (start == 0 || !Character.isDigit(s.charAt(start - 1)))) return true;
        }
        // …és hátra nézve: az óránál („2-2 óra") a MÁSODIK tag tűnik el
        // korábban, és az időtartam az elsőre marad kötve. Csak visszafelé
        // nézve az osztó alak ilyenkor láthatatlan volt – a „futás és úszás
        // 2-2 óra" futása a szokásos negyvenöt perccel került be.
        if (e < s.length() && s.charAt(e) == '-') {
            int after = e + 1;
            int ae = after;
            while (ae < s.length() && Character.isDigit(s.charAt(ae))) ae++;
            if (ae > after && s.substring(after, ae).equals(num)
                    && (ae >= s.length() || !Character.isDigit(s.charAt(ae)))) return true;
        }
        return false;
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
