package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Beépített élelmiszer-adatbázis (kcal és fehérje / 100 g, közelítő értékek),
 * magyar hétköznapi ételekkel. A keresés szótő-alapú, így a ragozott alakok
 * („rizzsel", „csirkemellből") is találnak.
 */
public final class Foods {

    private Foods() {}

    public static final class Food {
        public final String name;
        public final int kcal100;
        public final double prot100;
        public final int portion; // tipikus adag grammban
        final String[] stems;
        Food(String name, int kcal100, double prot100, int portion, String... stems) {
            this.name = name; this.kcal100 = kcal100; this.prot100 = prot100;
            this.portion = portion; this.stems = stems;
        }
    }

    public static final Food[] ALL = {
        new Food("Rántott hús (sertés)", 320, 22, 180, "rantott hus", "rantotthus", "becsi",
                "rantott szelet", "rantottszelet"),
        new Food("Rántott csirkemell", 250, 25, 180, "rantott csirke"),
        new Food("Csirkemell (sült/grill)", 165, 31, 150, "csirkemell", "csirke mell",
                "grillcsirke", "teriyaki"),
        new Food("Csirkecomb", 210, 26, 150, "csirkecomb", "comb", "csirkeszarny"),
        // Egészben sült csirke: a bőrrel-csonttal tálalt adag zsírosabb, mint
        // a grillezett mell, és a tepsiben sült zöldség zsírja is rámegy.
        new Food("Tepsis csirke", 200, 20, 300, "tepsis csirke", "tepsiben sult csirke",
                "egeszben sult csirke"),
        new Food("Pulykamell", 105, 23, 150, "pulyka"),
        new Food("Sertéskaraj", 240, 27, 150, "karaj", "sertes", "tarja",
                "naturszelet", "natur szelet"),
        // A „steak" szóban benne van a „tea": a hosszabb tő nyeli el, így a
        // „tofu steak" nem naplóz egy csésze teát is.
        new Food("Marhahús", 250, 26, 150, "marha", "belszin", "steak", "stek",
                "rostelyos", "ozgerinc"),
        new Food("Fasírt", 290, 15, 150, "fasirt", "fasiroz", "vagdalt", "stefania"),
        // A „kolbásszal" alakban a sz megkettőződik, ezért az is szótő.
        new Food("Kolbász", 350, 15, 100, "kolbasz", "kolbassz"),
        new Food("Virsli", 250, 10, 100, "virsli"),
        new Food("Sonka", 120, 18, 50, "sonka"),
        new Food("Szalámi", 400, 22, 30, "szalami"),
        new Food("Bacon", 500, 13, 30, "bacon", "szalonna"),
        new Food("Hal (fehér)", 120, 22, 150, "hal", "pisztrang", "ponty", "harcsa",
                "keszeg", "fogas"),
        new Food("Tenger gyümölcsei", 90, 18, 150, "garnela", "kagylo", "polip",
                "tenger gyumolcsei", "rakkoktel", "rak koktel", "kaviar"),
        new Food("Tonhal", 130, 24, 100, "tonhal"),
        new Food("Lazac", 210, 20, 150, "lazac"),
        new Food("Makréla / szardínia", 220, 20, 100, "makrela", "szardinia", "sprotni"),
        new Food("Tojás", 155, 13, 110, "tojas"),
        // A „tojásfehérje" eddig egész tojás volt: 33 g helyett 55 g, és 17 kcal
        // helyett 78 – négy és félszeres túlszámolás egy sportolós alapdarabon.
        new Food("Tojásfehérje", 52, 11, 33, "tojasfeherje", "tojas feherje", "feherje tojas"),
        new Food("Rántotta", 180, 12, 150, "rantotta", "omlett", "shakshuka"),
        new Food("Rizs (főtt)", 130, 2.7, 200, "riz"),
        // A „durum" hosszabb tő, különben a benne lévő „rum" tömény italt adna.
        new Food("Tészta (főtt)", 150, 5, 250, "durum teszta", "durumteszta", "teszta", "spagetti", "penne", "durum"),
        new Food("Burgonya (főtt)", 87, 2, 250, "burgonya", "krumpli", "krumpi"),
        new Food("Sült krumpli", 300, 3.5, 150, "sult krumpli", "sultkrumpli",
                "hasabburgonya", "hasáb"),
        new Food("Burgonyapüré", 110, 2, 200, "burgonyapure", "krumplipure", "pure"),
        new Food("Édesburgonya", 90, 1.6, 200, "edesburgonya", "batata"),
        new Food("Bulgur (főtt)", 120, 4, 200, "bulgur", "arpagyongy"),
        new Food("Polenta / puliszka", 85, 2, 250, "polenta", "puliszka"),
        new Food("Quinoa (főtt)", 120, 4.4, 200, "quinoa"),
        new Food("Kenyér", 250, 8, 70, "kenyer", "piritos", "bagett",
                "ciabatta", "focaccia", "bruschetta"),
        new Food("Zsemle", 280, 9, 55, "zsemle"),
        new Food("Kifli", 290, 8, 55, "kifli"),
        new Food("Péksütemény", 350, 7, 80, "peksutemenny", "peksutemeny", "croissant",
                "brios", "molnark"),
        new Food("Zabpehely", 370, 13, 50, "zab", "kasa", "feherjes zabkasa",
                "protein zabkasa"),
        new Food("Müzli", 380, 9, 60, "muzli", "granola"),
        // A „kakaós palacsinta" teljes alakja szótő, különben a kakaó egy
        // bögre tejes kakaónak számítana a kanálnyi töltelék helyett.
        new Food("Palacsinta", 220, 6, 150, "palacsinta", "kakaos palacsinta",
                "protein palacsinta", "feherje palacsinta"),
        new Food("Pizza", 260, 11, 300, "pizza"),
        new Food("Hamburger", 280, 13, 250, "hamburger", "burger", "big mac", "bigmac",
                "whopper"),
        new Food("Gyorséttermi menü", 220, 8, 500, "gyorsettermi menu", "mcmenu", "happy meal", "mekis menu"),
        new Food("Gyros", 220, 15, 350, "gyros"),
        new Food("Lángos", 320, 7, 200, "langos"),
        new Food("Gulyásleves", 100, 7, 400, "gulyasleves", "gulyas leves", "gulyas"),
        new Food("Pörkölt", 180, 15, 300, "porkolt"),
        // A „sóska" magában is a főzeléket jelenti; a „kelkáposzta főzelék"
        // teljes alakja szótő, különben káposzta + főzelék kettőnek számolna.
        new Food("Főzelék", 80, 3, 350, "fozelek", "soska", "kelkaposzta fozelek",
                "spenot fozelek", "spenotfozelek", "zoldborso fozelek", "borsofozelek",
                "borso fozelek", "lencse fozelek", "lencsefozelek", "bab fozelek",
                "babfozelek", "krumpli fozelek", "krumplifozelek", "burgonyafozelek"),
        // A „zöldségleves" teljes alakja szótő, különben zöldség + leves
        // kettőnek számolna.
        new Food("Leves (átlag)", 50, 3, 400, "leves", "zoldsegleves",
                "tarhonyaleves", "tarhonya leves"),
        // A „vacsorára túró és zöldség" zöldsége eddig eltűnt: vegyes köret.
        new Food("Zöldség (vegyes / párolt)", 40, 2, 200, "zoldseg", "vitaminsalata",
                "pak choi", "pakchoi", "mangold", "articsoka", "edeskomeny"),
        new Food("Rakott krumpli", 160, 6, 350, "rakott krumpli", "rakott"),
        new Food("Rakott zöldbab", 110, 5, 350, "rakott zoldbab", "rakott zoldseg"),
        new Food("Töltött káposzta", 150, 8, 350, "toltott kaposzta"),
        new Food("Bab (főtt)", 120, 8, 200, "bab"),
        new Food("Lencse (főtt)", 115, 9, 200, "lencse"),
        new Food("Borsó", 80, 5, 150, "borso"),
        // A „csicseriborsó” eddig sima borsó volt: fele annyi kalória.
        new Food("Csicseriborsó (főtt)", 160, 9, 150, "csicseriborso", "csicseri"),
        new Food("Kukorica", 90, 3, 100, "kukorica"),
        // Reggeli pehely, nem főzelék-kukorica: négyszeres a különbség.
        new Food("Kukoricapehely", 380, 7, 40, "kukoricapehely", "cornflakes", "corn flakes"),
        new Food("Rizspehely", 380, 6, 40, "rizspehely"),
        new Food("Liszt", 350, 10, 30, "liszt", "rizsliszt", "zabliszt", "buzaliszt"),
        new Food("Brokkoli", 35, 2.8, 150, "brokkoli"),
        new Food("Karfiol", 25, 2, 150, "karfiol"),
        new Food("Paradicsom", 18, 0.9, 100, "paradicsom", "pari"),
        new Food("Uborka", 15, 0.7, 100, "uborka", "ubi"),
        new Food("Paprika", 25, 1, 100, "paprika"),
        new Food("Saláta (zöld)", 15, 1.4, 50, "salata", "sali",
                // A „rukkola" vége „kola" – a hosszabb tő elfedi az üdítőt.
                "rukkola", "endivia", "radicchio"),
        new Food("Sajt (trappista)", 360, 25, 30, "sajt", "trappista", "parenyica"),
        new Food("Mozzarella", 280, 22, 50, "mozzarella"),
        // A „parmezán" MÉZNEK számított (a „mez" tő beleesett a szóba).
        new Food("Parmezán", 400, 35, 20, "parmezan"),
        new Food("Camembert / brie", 300, 20, 50, "camembert", "brie"),
        new Food("Feta", 270, 14, 50, "feta"),
        new Food("Mascarpone", 435, 4, 50, "mascarpone"),
        new Food("Ricotta", 150, 11, 50, "ricotta"),
        new Food("Túró", 100, 12, 100, "turo"),
        new Food("Krémtúró / túródesszert", 180, 8, 90,
                "kremturo", "turodesszert", "turokrem"),
        new Food("Joghurt", 60, 4, 150, "joghurt"),
        new Food("Ivójoghurt", 75, 3, 200, "ivojoghurt", "joghurtital", "actimel"),
        new Food("Puding", 120, 3, 200, "puding", "csokipuding", "protein puding",
                "protein pudding", "pudding"),
        new Food("Madártej", 120, 4, 250, "madartej"),
        new Food("Tejszínhab", 300, 2, 30, "tejszinhab", "tejszin"),
        new Food("Tejszelet", 420, 5, 28, "tejszelet", "monte"),
        new Food("Milkshake", 110, 3, 300, "milkshake", "tejturmix"),
        new Food("Görög joghurt", 120, 9, 150, "gorog joghurt"),
        new Food("Tej", 60, 3.3, 200, "tej"),
        new Food("Zsírszegény tej", 38, 3.4, 200, "zsirszegeny tej", "sovany tej"),
        new Food("Vaj", 720, 0.9, 10, "vaj"),
        new Food("Olaj", 900, 0, 10, "olaj", "napraforgo olaj"),
        new Food("Olajbogyó / olívabogyó", 145, 1, 30,
                "olajbogyo", "olivabogyo", "kapribogyo"),
        new Food("Magvaj (mandula/kesu/tahini)", 600, 20, 20,
                "mogyorovaj", "mandulavaj", "kesuvaj", "tahini", "magvaj"),
        new Food("Szendvicskrém / kence", 250, 4, 30,
                "szendvicskrem", "tojaskrem", "vajkrem", "padlizsankrem", "ajvar"),
        new Food("Szirup (juhar/agavé)", 270, 0, 20, "szirup"),
        new Food("Majonéz", 680, 1, 20, "majonez"),
        new Food("Light majonéz", 240, 1, 20, "light majonez"),
        new Food("Ketchup", 110, 1.7, 20, "ketchup"),
        // Szósz-kör: a „szójaszósz" szójakockának számított (204 kcal egy
        // löttyintésnyi ~6 helyett), a mártások fele pedig ismeretlen volt.
        // Az összetett alakok („sajtszósz") teljes szótövek, hogy ne essenek
        // sajt + szósz kettőre.
        new Food("Szósz / mártás", 120, 2, 30, "szosz", "martas",
                "sajtszosz", "fokhagymaszosz", "gombamartas", "sajtmartas",
                "kapros martas", "besamel"),
        new Food("Szójaszósz", 60, 6, 10, "szojaszosz", "szoja szosz"),
        new Food("Tartármártás", 520, 1, 30, "tartarmartas", "tartar"),
        new Food("Pesto", 450, 5, 30, "pesto"),
        new Food("Guacamole", 150, 2, 50, "guacamole", "guakamole"),
        new Food("Tzatziki", 90, 3, 50, "tzatziki", "cacik"),
        new Food("Balzsamecet", 90, 0.5, 10, "balzsamecet", "balzsam"),
        new Food("Alma", 52, 0.3, 150, "alma"),
        new Food("Banán", 89, 1.1, 120, "banan"),
        new Food("Narancs", 47, 0.9, 150, "narancs", "naranccs"),
        new Food("Szőlő", 70, 0.7, 100, "szolo"),
        new Food("Eper", 33, 0.7, 100, "eper"),
        new Food("Avokádó", 160, 2, 70, "avokado"),
        new Food("Dió", 650, 15, 30, "dio"),
        new Food("Mandula", 580, 21, 30, "mandula"),
        new Food("Mogyoró", 570, 25, 30, "mogyoro"),
        new Food("Csokoládé", 550, 5, 25, "csoki", "csokolade", "kinder", "milka", "twix",
                "bounty", "snickers", "kitkat", "mars szelet", "sport szelet",
                "balaton szelet", "3bit", "milky way"),
        // A „mentos" tő szándékosan hiányzik: a „mentős"-be esne bele.
        new Food("Gumicukor / cukorka", 340, 0, 30, "gumicukor", "cukorka",
                "haribo", "skittles", "tic tac"),
        new Food("Keksz", 450, 6, 40, "kekssz", "keksz", "oreo", "linzer", "zabkeksz",
                "zabpelyhes keksz"),
        new Food("Sütemény", 400, 5, 100, "sutemenny", "sutemeny", "torta",
                "zserbo", "rigo jancsi", "isler", "puncsszelet", "mignon",
                "flodni", "macaron", "suti"),
        new Food("Muffin / brownie", 380, 5, 80, "muffin", "cupcake", "brownie"),
        new Food("Gofri", 350, 6, 100, "gofri", "waffle", "protein gofri"),
        new Food("Energiagolyó", 420, 8, 25,
                "energiagolyo", "kokuszgolyo", "zabgolyo", "proteingolyo"),
        new Food("Fagylalt", 200, 3.5, 100, "fagyi", "fagylalt", "jegkrem",
                "protein jegkrem"),
        new Food("Chips", 540, 6, 50, "chips", "nachos", "proteinchips",
                "protein chips"),
        // Nassolás-kör: a hagymakarika hagymának (20 kcal!), a szaloncukor
        // kanál cukornak, a mézeskalács kalácsnak számított.
        new Food("Hagymakarika (rántott)", 280, 4, 100, "hagymakarika"),
        new Food("Perec", 380, 9, 50, "perec", "pretzel"),
        new Food("Ropi / kréker", 400, 9, 30, "ropi", "kreker", "sajtos taller", "taller"),
        new Food("Pisztácia", 580, 20, 30, "pisztaci"),
        new Food("Mézeskalács", 400, 5, 60, "mezeskalacs"),
        new Food("Szaloncukor", 450, 3, 15, "szaloncukor"),
        new Food("Nutella", 540, 6, 30, "nutella", "mogyorokrem"),
        new Food("Lekvár", 250, 0.4, 25, "lekvar"),
        new Food("Méz", 320, 0.3, 20, "mez"),
        new Food("Cukor", 400, 0, 10, "cukor"),
        new Food("Vattacukor", 400, 0, 30, "vattacukor"),
        new Food("Sült gesztenye", 210, 2.4, 100, "sult gesztenye"),
        // A bolti jeges tea cukros – nem a cukrozatlan tea 3 kalóriája.
        new Food("Üdítő (cukros)", 42, 0, 330, "udito", "kola", "cola", "tonik",
                // A puszta „fanta" tő a „fantasztikus"-ba is beleesne – csak
                // ragozva vesszük („fantát", „fantával").
                "jeges tea", "ice tea", "gyombersor", "fantat", "fantaval", "sprite"),
        // A „cukormentes” szó eddig CUKROT jelentett: a „cukor” szótő beleesett,
        // és 40 kcal-t adott hozzá – pont az ellenkezőjét annak, amit a felhasználó
        // írt. A hosszabb szótő elnyeli a rövidebbet, így ez a nulla kalóriás
        // tétel lép a helyére; a „cukormentes kóla” alak a cukros üdítőt is kiváltja.
        new Food("Cukormentes / light", 0, 0, 330,
                "cukormentes kola", "cukormentes udito", "cukormentes ital",
                "cukormentes", "cukorment", "kola zero", "zero kola", "cola zero",
                "diet kola", "light kola", "light udito",
                // A leggyakoribb magyar alak: „kávé cukor nélkül". Enélkül a
                // „cukor" szótő beleesett, és 40 kcal cukrot adott hozzá.
                "cukor nelkul", "cukor nelkuli", "cukrozatlan", "edesitovel", "edesito"),
        new Food("Rántott sajt", 330, 18, 120, "rantott sajt"),
        new Food("Nokedli / galuska", 170, 5, 200, "nokedli", "galuska", "knedli"),
        new Food("Sztrapacska", 160, 7, 400, "sztrapacska", "haluska",
                "juhturos sztrapacska"),
        new Food("Tarhonya", 150, 5, 200, "tarhonya"),
        new Food("Káposzta", 25, 1.3, 150, "kaposzta"),
        new Food("Tükörtojás", 200, 13, 110, "tukortojas"),
        new Food("Bableves", 90, 5, 400, "bableves"),
        new Food("Palócleves", 80, 5, 400, "palocleves", "paloc leves"),
        new Food("Gyümölcsleves", 60, 1, 350, "gyumolcsleves", "meggyleves", "meggy leves"),
        new Food("Paradicsomos káposzta", 55, 2, 400,
                "paradicsomos kaposzta", "paradicsomoskaposzta"),
        new Food("Húsleves", 40, 3, 400, "husleves", "csigateszta leves", "csigateszta",
                "grizgaluska leves", "grizgaluskaleves", "grizgaluska", "majgomboc"),
        new Food("Kocsonya", 90, 12, 300, "kocsonya", "aszpik"),
        new Food("Franciakrumpli (rakott)", 140, 7, 400, "franciakrumpli"),
        // A teljes „harcsapaprikás" alak szótő, különben a harcsa (hal) és a
        // paprikás kettőnek számolna.
        new Food("Csirkepaprikás", 160, 14, 300, "paprikas", "harcsapaprikas"),
        new Food("Milánói makaróni", 180, 7, 350, "milanoi", "makaroni"),
        new Food("Lasagne", 160, 9, 350, "lasagne", "lazanya"),
        new Food("Tészta carbonara", 180, 8, 350, "teszta carbonara", "carbonara"),
        new Food("Töltött tészta (tortellini)", 180, 8, 300, "toltott teszta", "tortellini", "ravioli"),
        // A puszta „turmix" magyarul gyümölcsös: nem fehérjeturmix. A teljes
        // „protein turmix" alak szótő, így az egyben marad.
        new Food("Protein turmix", 100, 10, 300, "protein turmix", "protein", "shake",
                "kazein turmix", "whey turmix", "gainer"),
        new Food("Gyümölcsturmix / smoothie", 60, 1, 300, "turmix", "smoothie", "acai"),
        // Maga a POR, nem a kész turmix: a „30 g fehérjepor” eddig vagy semmit nem
        // talált, vagy a 100 kcal/100 g-os kész italra esett – harmadannyi kalória.
        new Food("Fehérjepor", 380, 75, 30, "feherjepor", "feherje por", "protein por",
                "proteinpor", "tejsavofeherje", "tejsavo", "whey", "kazein",
                // Teljes kifejezésként is, hogy a rövidebb „protein" tő ne
                // hozzon MELLÉ egy kész turmixot is („whey protein 30 g").
                "whey protein", "kazein protein", "kollagen"),
        new Food("Proteinszelet", 350, 30, 60, "proteinszelet", "protein szelet",
                "feherjeszelet", "energiaszelet"),
        new Food("Túró rudi", 380, 8, 51, "turo rudi", "rudi"),
        new Food("Szendvics", 250, 10, 150, "szendviccs", "szendvics", "szendo", "croque"),
        new Food("Hot-dog", 290, 10, 150, "hot-dog", "hotdog", "hot dog"),
        // A puszta „szelet” szótő itt nem lehet: hétköznapi szó, ami mennyiséget
        // jelöl („két szelet kenyér”, „egy szelet torta”), nem ételt.
        new Food("Müzliszelet", 400, 6, 30, "muzliszelet"),
        // A „felvágott" gyűjtőnév is ide fut be: kalóriában a párizsi az átlag.
        new Food("Párizsi / felvágott", 230, 12, 50, "parizsi", "felvagott", "mortadella",
                "loncshus"),
        new Food("Tejföl", 200, 3, 30, "tejfol"),
        new Food("Kefir", 55, 3.5, 200, "kefir"),
        new Food("Kakaó (tejes)", 85, 3.5, 250, "kakao", "forro csoki", "forrocsoki",
                "nesquik"),
        new Food("Tükörponty / halrudak", 220, 12, 150, "tukorponty", "halrud", "halrudak"),
        new Food("Körte", 57, 0.4, 150, "korte"),
        new Food("Őszibarack", 39, 0.9, 150, "oszibarack", "barack", "nektarin"),
        new Food("Görögdinnye", 30, 0.6, 300, "dinnye"),
        new Food("Kivi", 60, 1.1, 80, "kivi", "kiwi"),
        new Food("Mandarin", 53, 0.8, 100, "mandarin"),
        new Food("Paradicsomleves", 60, 1.5, 400, "paradicsomleves"),
        new Food("Tökfőzelék", 70, 2, 350, "tokfozelek"),
        new Food("Gnocchi", 160, 4, 250, "gnocchi", "nudli"),
        new Food("Tortilla / wrap", 250, 8, 200, "tortilla", "wrap"),
        new Food("Túrós csusza", 210, 10, 300, "turos csusza", "csusza", "turos teszta"),
        new Food("Grízes tészta", 200, 6, 300, "grizes teszta", "griz"),
        // Menza-kör: a mákos tészta mákja eddig eltűnt (csak főtt tészta lett),
        // a grenadírmars és a rántott zöldség pedig ismeretlen volt.
        new Food("Mákos tészta", 250, 7, 300, "makos teszta"),
        new Food("Tarhonyás hús", 160, 10, 400, "tarhonyas hus"),
        new Food("Grenadírmars (krumplis tészta)", 150, 4, 400,
                "grenadir", "krumplis teszta"),
        new Food("Rántott zöldség", 180, 5, 200, "rantott karfiol",
                "rantott zoldseg", "rantott brokkoli", "rantott cukkini"),
        new Food("Kakaós csiga", 380, 7, 90, "kakaos csiga", "csiga"),
        // A „meggyes rétes" két szó, egy sütemény: a teljes alak szótő, hogy a
        // gyümölcs ne számolódjon külön tételként mellé.
        new Food("Rétes", 300, 5, 100, "almas retes", "meggyes retes", "turos retes",
                "makos retes", "kapros retes", "retes"),
        new Food("Piskóta / kevert süti", 350, 6, 80, "piskota", "kevert"),
        new Food("Popcorn", 400, 12, 40, "popcorn", "pattogatott",
                "pattogatott kukorica"),
        new Food("Energiaital", 45, 0, 250, "energiaital", "energia ital",
                "red bull", "redbull", "monster"),
        new Food("Sör", 43, 0.5, 500, "sor", "radler"),
        // A „narancslé" 71 kcal-os narancsnak számított (és a „2 dl almalé"
        // 200 g almának): a gyümölcs-összetételek teljes alakja szótő.
        new Food("Gyümölcslé", 45, 0.5, 250, "gyumolcsle", "juice", "dzsussz", "dzsusz",
                "narancsle", "almale", "paradicsomle", "oszibarackle", "barackle",
                "repale", "rostos le", "rostos udito", "cappy", "hohes c"),
        new Food("Szörp (hígítva)", 45, 0, 300, "szorp", "malnaszorp", "barackszorp",
                "eperszorp"),
        new Food("Citromlé", 25, 0, 30, "citromle", "citrom leve", "citrom", "lime"),
        // A „puffasztott rizs" két ételre esett szét (puffasztott + rizs), vagyis
        // duplán számolt. A teljes alak hosszabb szótő, így elnyeli mindkettőt.
        new Food("Rizsszelet / puffasztott rizs", 380, 8, 10,
                "puffasztott rizs", "puffasztott", "rizsszelet", "abonett"),
        new Food("Cottage cheese", 100, 11, 150, "cottage"),
        new Food("Skyr", 65, 11, 150, "skyr"),
        new Food("Tofu", 120, 12, 150, "tofu"),
        new Food("Edamame", 120, 11, 100, "edamame"),
        new Food("Seitan", 140, 25, 100, "seitan", "szejtan"),
        new Food("Tempeh", 190, 19, 100, "tempeh"),
        new Food("Kókusztej", 190, 2, 100, "kokusztej"),
        new Food("Csirkés saláta", 130, 12, 300, "csirkes salata", "cezar salata", "cezar", "caesar"),
        new Food("Sushi", 150, 6, 250, "sushi"),
        // Éttermi kör: egy 57 neves próbából 18-at egyáltalán nem ismert az
        // adatbázis, a görög saláta pedig 8 kcal-os zöldsalátának számított.
        new Food("Cordon bleu", 250, 20, 180, "cordon"),
        new Food("Brassói aprópecsenye", 180, 12, 400, "brassoi"),
        new Food("Cigánypecsenye", 220, 18, 300, "ciganypecsenye", "cigany pecsenye"),
        // A teljes alak is szótő, különben a „palacsinta" külön édességnek ülne rá.
        new Food("Hortobágyi palacsinta", 160, 10, 250,
                "hortobagyi palacsinta", "hortobagyi"),
        new Food("Görög saláta", 90, 4, 250, "gorog salata"),
        new Food("Tonhalsaláta", 150, 14, 250, "tonhalsalata", "tonhal salata"),
        new Food("Curry", 150, 10, 300, "curry"),
        new Food("Ramen", 120, 6, 500, "ramen", "ramen leves", "pho leves"),
        new Food("Pad thai", 170, 8, 350, "pad thai", "padthai", "pad see"),
        new Food("Burrito", 190, 10, 300, "burrito"),
        new Food("Taco", 220, 10, 120, "taco", "enchilada", "fajita"),
        new Food("Chilis bab (con carne)", 120, 8, 400,
                "chilis bab", "chili con carne", "con carne"),
        new Food("Tavaszi tekercs / gyoza", 200, 7, 150,
                "tavaszi tekercs", "spring roll", "gyoza", "dim sum"),
        new Food("Wok (zöldséges-húsos)", 120, 10, 350, "wok", "bibimbap"),
        // Kínai büfé: a bundázott, szószos csirke messze nem a wok kalóriája.
        new Food("Kínai bundás csirke", 250, 14, 250, "kinai csirke", "bundas csirke",
                "szechuan", "szecsuani", "kung pao", "edes-savanyu csirke",
                "edes savanyu csirke"),
        new Food("Quesadilla", 250, 11, 200, "quesadilla"),
        new Food("Falafel", 300, 13, 150, "falafel"),
        new Food("Hummusz", 180, 8, 60, "hummus", "humusz"),
        new Food("Limonádé", 45, 0, 300, "limonade"),
        new Food("Fröccs", 40, 0, 300, "froccs"),
        new Food("Kombucha", 20, 0, 330, "kombucha"),
        new Food("Ayran", 30, 1.7, 250, "ayran"),
        new Food("Jégkása", 60, 0, 300, "jegkasa", "slush"),
        new Food("Lecsó", 70, 2, 300, "lecso"),
        new Food("Töltött paprika", 130, 8, 350, "toltott paprika"),
        new Food("Székelykáposzta", 150, 9, 350, "szekelykaposzta", "szekely kaposzta",
                "szekelygulyas"),
        new Food("Tokány", 190, 16, 300, "tokany", "borsos tokany", "hentes tokany"),
        new Food("Szalontüdő", 120, 12, 350, "szalontudo"),
        new Food("Halászlé", 60, 8, 400, "halaszle"),
        new Food("Hekk (sült)", 190, 16, 200, "hekk"),
        // A „rakott kelkáposzta" nem rakott krumpli + káposzta: saját fogás.
        new Food("Rakott kelkáposzta", 120, 7, 400, "rakott kelkaposzta",
                "rakott kaposzta", "kolozsvari kaposzta"),
        new Food("Paprikás krumpli", 120, 4, 350, "paprikas krumpli"),
        new Food("Rizses hús", 160, 8, 350, "rizses hus"),
        new Food("Bolognai spagetti", 170, 8, 350, "bolognai spagetti", "spagetti bolognai", "bolognai"),
        new Food("Sajtos tészta", 220, 8, 300, "sajtos teszta"),
        new Food("Tojásos nokedli", 190, 7, 300, "tojasos nokedli"),
        new Food("Rizottó", 150, 5, 300, "rizotto", "risotto", "rizsotto"),
        new Food("Túrógombóc", 210, 9, 200, "turogomboc"),
        new Food("Szilvás gombóc", 190, 3, 250, "szilvas gomboc", "szilvasgomboc"),
        new Food("Káposztás tészta", 150, 4, 330, "kaposztas teszta", "kaposztasteszta",
                "cvekedli", "kaposztas cvekedli"),
        new Food("Quiche", 300, 9, 200, "quiche"),
        new Food("Poke bowl", 120, 8, 400, "poke"),
        new Food("Caprese saláta", 130, 7, 250, "caprese salata", "caprese"),
        new Food("Mákos guba", 300, 7, 200, "makos guba"),
        // A teljes kifejezés is szótő, különben a „galuska" külön a nokedlire ülne.
        new Food("Somlói galuska", 260, 5, 150, "somloi galuska", "somloi"),
        // A „gesztenyepüré" eddig burgonyapürének számított (a „pure" szótő
        // beleesett) – édesség létére köretnek.
        new Food("Gesztenyepüré", 230, 3, 150, "gesztenyepure", "gesztenye pure", "gesztenye"),
        new Food("Tiramisu", 290, 5, 120, "tiramisu"),
        new Food("Krémes", 260, 5, 150, "kremes"),
        new Food("Vaníliás karika", 380, 6, 70, "vanilias karika", "vaniliaskarika"),
        new Food("Lekváros bukta", 300, 6, 100, "lekvaros bukta", "bukta"),
        new Food("Sajttorta", 320, 6, 120, "sajttorta", "cheesecake"),
        new Food("Fánk / churros", 400, 5, 60, "fank", "churros"),
        new Food("Kürtőskalács", 380, 6, 120, "kurtoskalaccs", "kurtoskalacs", "trdelnik"),
        new Food("Rántott gomba", 220, 5, 150, "rantott gomba"),
        new Food("Gomba", 22, 3, 100, "gomba"),
        new Food("Csirkemáj", 130, 20, 120, "csirkemaj", "maj"),
        // A „tepertő" 33 kcal-os EPERNEK számított (az „eper" tő beleesett),
        // a disznósajt sajtnak, a májkrém nyers csirkemájnak.
        new Food("Tepertő", 700, 15, 50, "teperto", "topertyu"),
        new Food("Disznósajt", 280, 15, 100, "disznosajt"),
        new Food("Májkrém / kenőmájas", 330, 12, 30,
                "majkrem", "kenomajas", "majas", "pastetom"),
        new Food("Pacalpörkölt", 120, 12, 400, "pacalporkolt", "pacal"),
        new Food("Sült oldalas", 290, 20, 200, "oldalas"),
        new Food("Csülök", 280, 22, 200, "csulok"),
        new Food("Kacsa / liba", 300, 19, 180, "kacsa", "liba"),
        new Food("Mustár", 60, 4, 10, "mustar"),
        new Food("Uborkasaláta", 40, 0.7, 150, "uborkasalata"),
        new Food("Céklasaláta", 45, 1.3, 100, "ceklasalata", "cekla salata", "cekla"),
        // A franciasaláta majonézes: nem 8 kcal-os zöldsaláta.
        // Majonézes hidegtálak: az „orosz hússaláta" és a „tojássaláta" a
        // majonéz miatt jóval sűrűbb, mint egy zöldsaláta – oda tartoznak.
        new Food("Franciasaláta / coleslaw", 170, 2.5, 150,
                "franciasalata", "francia salata", "coleslaw", "orosz hussalata",
                "orosz salata", "tojassalata", "kaszinotojas", "majonezes salata"),
        new Food("Savanyúság", 25, 1, 100, "savanyusag", "savanyu kaposzta", "kimchi"),
        new Food("Spárga", 20, 2.2, 150, "sparga"),
        new Food("Karalábé", 27, 1.7, 150, "karalabe"),
        new Food("Retek", 16, 0.7, 50, "retek", "jegcsapretek"),
        new Food("Zeller", 18, 0.7, 100, "zeller"),
        new Food("Zöldbab", 35, 1.8, 150, "zoldbab"),
        new Food("Spenót / paraj", 25, 2.9, 200, "spenot", "paraj"),
        new Food("Krémleves (zöldség)", 60, 2, 350, "kremleves",
                "brokkoli kremleves", "sutotok kremleves", "gomba kremleves"),
        new Food("Joghurtos öntet", 60, 3, 40, "kefires ontet", "joghurtos ontet", "ontet"),
        new Food("Rántott hal", 230, 16, 180, "rantott hal"),
        new Food("Csirkés wrap", 200, 12, 250, "csirkes wrap"),
        // --- Zöldségek, gyümölcsök, magvak ---
        new Food("Sárgarépa", 41, 0.9, 100, "sargarepa", "repa"),
        // A „cukkini spagetti" zöldségtészta: nem cukkini + főtt tészta.
        new Food("Cukkini", 17, 1.2, 200, "cukkini", "cukkini spagetti",
                "patisszon", "csillagtok"),
        new Food("Konjac / shirataki tészta", 10, 0, 200,
                "konjac teszta", "shirataki teszta", "konjac", "shirataki"),
        new Food("Padlizsán", 25, 1, 200, "padlizsan"),
        new Food("Hagyma", 40, 1.1, 50, "hagyma"),
        new Food("Ananász", 50, 0.5, 150, "ananassz", "ananasz"),
        new Food("Málna", 52, 1.2, 100, "malna"),
        new Food("Áfonya", 57, 0.7, 100, "afonya"),
        new Food("Szilva", 46, 0.7, 100, "szilva"),
        new Food("Cseresznye / meggy", 60, 1, 150, "cseresznye", "meggy"),
        new Food("Datolya", 280, 2.5, 30, "datolya"),
        new Food("Tökmag / napraforgómag", 570, 22, 30, "tokmag", "napraforgomag",
                "napraforgo", "fenyomag"),
        new Food("Chia / lenmag", 490, 17, 15, "chia", "lenmag", "lenmagliszt"),
        new Food("Kesudió", 580, 18, 30, "kesudio", "kesu"),
        // A „kebab” szóban benne van a „bab”: eddig 200 g főtt bab lett belőle.
        new Food("Kebab", 250, 13, 350, "kebab"),
        new Food("Kuszkusz (főtt)", 115, 4, 200, "kuszkussz", "kuszkusz", "couscous"),
        // A puszta „koles" tő a „koleszos"-ba, „koleszterin"-be is beleesne.
        new Food("Hajdina (főtt)", 130, 5, 200, "hajdina", "haricska",
                "kolest", "koleskasa", "koles kasa", "amarant"),
        new Food("Darált hús", 250, 18, 150, "daralt hus", "daralthus"),
        new Food("Kelbimbó", 40, 3, 150, "kelbimbo"),
        new Food("Margarin", 600, 0, 10, "margarin"),
        new Food("Sportital / izotóniás", 25, 0, 500, "sportital", "izotonias",
                "elektrolit"),
        // Kapszula, tabletta, por: kalóriában elhanyagolható, de ha valaki
        // beírja, ne „ismeretlen ételként" kelljen felvennie.
        new Food("Étrend-kiegészítő", 0, 0, 5, "kreatin", "bcaa", "aminosav",
                "magnezium", "vitamin", "omega 3", "omega-3", "halolaj",
                "cink", "koffein tabletta", "pre workout", "preworkout",
                "etrend-kiegeszito", "etrend kiegeszito", "etrendkiegeszito"),
        // --- Magyar klasszikusok ---
        new Food("Bundás kenyér", 260, 9, 120, "bundas kenyer", "bundaskenyer"),
        // A „sajtos pogácsa" sajtra és pogácsára esett szét: két tétel egy sütiből.
        new Food("Pogácsa", 400, 8, 60, "sajtos pogacsa", "pogacsa"),
        new Food("Zsíros kenyér", 330, 6, 100, "zsiros kenyer", "zsiroskenyer",
                "velos piritos"),
        new Food("Hurka", 300, 12, 120, "hurka"),
        new Food("Csirkenugget", 300, 15, 150, "nugget", "csirkefalat"),
        new Food("Tejbegríz", 110, 4, 250, "tejbegriz", "tejbedara"),
        new Food("Tejberizs", 120, 3.5, 250, "tejberizs"),
        // Aszalva négy-ötszörös a kalória – a „szilva" tő 46 kcal-t adott volna.
        new Food("Aszalt gyümölcs", 280, 2.5, 40,
                "aszalt szilva", "aszalt barack", "aszalt sargabarack",
                "aszalt vorosafonya", "aszalt gyumolcs", "aszalt", "mazsola",
                "goji", "kandirozott"),
        new Food("Gyümölcspüré / bébiétel", 65, 0.5, 100,
                "gyumolcspure", "almapure", "almaszosz", "bebietel"),
        new Food("Gránátalma", 83, 1, 150, "granatalma"),
        new Food("Bogyós gyümölcs", 50, 1, 100, "bogyos", "szeder", "ribizli",
                "egres", "josta", "homoktovis", "licsi"),
        new Food("Mangó", 60, 0.8, 200, "mango"),
        new Food("Datolyaszilva", 70, 0.6, 150, "datolyaszilva", "hurma"),
        new Food("Füge", 74, 0.8, 100, "fuge"),
        new Food("Befőtt / kompót", 70, 0.4, 150, "befott", "kompot"),
        new Food("Túrós batyu", 300, 7, 100, "turos batyu", "batyu"),
        // A -val/-vel hasonul: „kaláccsal". A cs+cs alakot külön tő fogja meg.
        new Food("Kalács / bejgli", 350, 8, 80, "kalacs", "kalaccs", "bejgli",
                // A töltelék benne van a kalóriában: a „diós bejgli" ne
                // számoljon még egy adag diót is mellé.
                "dios bejgli", "makos bejgli", "makos tekercs", "dios tekercs"),
        new Food("Almás pite", 240, 3, 120, "almas pite", "almaspite"),
        new Food("Krumplisaláta", 150, 2.5, 200, "krumplisalata", "krumpli salata",
                "burgonyasalata", "burgonya salata"),
        new Food("Frankfurti leves", 90, 4, 350, "frankfurti leves", "frankfurti"),
        new Food("Körözött", 250, 12, 80, "korozott"),
        new Food("Sajtkrém", 250, 8, 40, "sajtkrem"),
        // --- Italok ---
        new Food("Kávé (fekete)", 2, 0.2, 200, "kave", "feketekave", "eszpresszo",
                "espresszo", "espresso", "ristretto", "americano"),
        new Food("Tejeskávé / cappuccino", 55, 3, 250, "tejeskave", "cappuccino", "latte"),
        new Food("Tea (cukrozatlan)", 1, 0, 250, "tea", "matcha"),
        // A víz nulla kalória, de attól még értsük: az „ittam 1,5 liter
        // vizet" ne legyen „nem értem" – és a napló is teljesebb tőle.
        new Food("Víz / ásványvíz", 0, 0, 250, "viz", "asvanyviz", "szoda"),
        new Food("Bor (vörös/fehér)", 80, 0.1, 150, "bor", "vorosbor", "feherbor"),
        // A „borssal" nem egy pohár bor: a „bor" szótő beleesett, és minden
        // borsozott étel mellé 120 kcal ital került. A hosszabb tő nyer, a
        // fűszer pedig a használt mennyiségben gyakorlatilag nulla kalória.
        new Food("Bors (fűszer)", 0, 0, 5, "borsoz", "bors"),
        // A „vilmoskörte" pálinka, nem gyümölcs – a hosszabb tő elnyeli a körtét.
        new Food("Pálinka / tömény", 250, 0, 40, "palinka", "tomenny", "tomeny", "vodka",
                "whisky", "whiskey", "jager", "rum", "gin", "tequila", "unicum",
                "baileys", "martini", "likor", "vilmoskorte", "vilmos"),
        new Food("Koktél / long drink", 90, 0, 250, "koktel", "gin tonik", "gintonik",
                "mojito", "aperol", "cuba libre", "long drink", "spritz"),
        new Food("Pezsgő", 76, 0, 150, "pezsgo", "prosecco", "champagne"),
        new Food("Cider", 45, 0, 330, "cider", "almabor"),
        new Food("Növényi tej (mandula/zab)", 40, 1, 250, "novenyi tej", "mandulatej",
                "zabtej", "rizstej", "szojatej", "zabital", "mandulaital",
                "rizsital", "szojaital", "kokusztej ital"),
        new Food("Szójakocka", 340, 50, 60, "szojakocka", "szoja"),
    };

    /**
     * Szavak, amik SOHA nem ételek, viszont rövid szótövek bújnak meg bennük.
     * A klasszikus eset: a „vacsora" tartalmazza a „sör" szótövet, így minden
     * „vacsora …" kezdetű bejegyzéshez hozzászámolódott egy fél liter sör.
     *
     * Előtagként illesztünk, hogy a ragozott alakok is menjenek („vacsorára",
     * „reggelizni"). A találatot azonos hosszúságú szóközre cseréljük, hogy a
     * szövegbeli pozíciók – amikre a gramm- és darabszám-hozzárendelés épül –
     * ne csússzanak el.
     */
    private static final String[] NOT_FOOD = {
            "reggeli", "tizorai", "ebed", "uzsonna", "vacsor", "vacsi",
            "kaveskanal", "evokanal", "teaskanal",
            // Étel-tövet rejtő, gyakori NEM-étel szavak. Szó ELEJÉT nézzük,
            // ezért a ragozott igazi ételek („sajtos", „vajas", „sört")
            // érintetlenek maradnak.
            "vajon", "hallott", "hallom", "halk", "halad", "halott", "halvany",
            "sajtotaj", "borzaszt", "labor", "tabor", "borult", "borus",
            "borotva", "borit", "borzalm", "sorban", "sorba", "sorra",
            "sorozat", "sorol", "sorren", "paradicsomi", "narancssarga",
            "kolbaszol", "rumli",
            // Átvitt értelmű és összetett álca-szavak: a „narancsbőr" nem bor,
            // a „sörhas" nem sör, a „kávészünet" nem kávé.
            "borsos", "gombamod", "tejszinu", "vajszinu", "uborkaszezon",
            "lencseveg", "banankoz", "narancsbor", "kenyerkeres", "tortaform",
            "kaveszunet", "teadelutan", "olajfolt", "halaszf", "borvidek",
            "sorhas", "kolbaszujj", "almafa", "kortefa", "diofa",
            "cseresznyefa", "szilvafa", "barackfa", "eperfa", "meggyfa",
            "citromfu",
    };

    /** Az étel-felismerés elől elrejtett szavak kimaszkolása. */
    static String mask(String q) {
        StringBuilder sb = new StringBuilder(q);
        int i = 0;
        while (i < sb.length()) {
            if (!Character.isLetter(sb.charAt(i))) { i++; continue; }
            int j = i;
            while (j < sb.length() && Character.isLetter(sb.charAt(j))) j++;
            String tok = sb.substring(i, j);
            for (String bad : NOT_FOOD) {
                if (tok.startsWith(bad)) {
                    for (int k = i; k < j; k++) sb.setCharAt(k, ' ');
                    break;
                }
            }
            i = j;
        }
        return sb.toString();
    }

    /** Ékezet-mentesítés + kisbetű, a rugalmas kereséshez. */
    static String norm(String s) {
        if (s == null) return "";
        s = s.toLowerCase(new Locale("hu"));
        // Az ä nem magyar ékezet, de márkanevekben előfordul (Jägermeister).
        return s.replace('á','a').replace('é','e').replace('í','i').replace('ó','o')
                .replace('ö','o').replace('ő','o').replace('ú','u').replace('ü','u')
                .replace('ű','u').replace('ä','a');
    }

    // ---------- Saját ételek (felhasználó által felvéve) ----------

    /** A felhasználó saját ételei; a szótő a saját név. */
    public static List<Food> custom(android.content.Context c) {
        List<Food> out = new ArrayList<>();
        try {
            org.json.JSONArray a = new org.json.JSONArray(
                    c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE)
                            .getString("custom_foods", "[]"));
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                String n = o.optString("n", "");
                if (n.isEmpty()) continue;
                out.add(new Food(n, o.optInt("k", 100), o.optDouble("p", 0),
                        Math.max(1, o.optInt("g", 100)), n));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void addCustom(android.content.Context c, String name, int kcal100,
                                 double prot100, int portion) {
        try {
            android.content.SharedPreferences sp =
                    c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE);
            org.json.JSONArray a = new org.json.JSONArray(sp.getString("custom_foods", "[]"));
            // Azonos nevű korábbi bejegyzés cseréje.
            org.json.JSONArray na = new org.json.JSONArray();
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.optJSONObject(i);
                if (o != null && !o.optString("n", "").equalsIgnoreCase(name)) na.put(o);
            }
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("n", name); o.put("k", kcal100); o.put("p", prot100); o.put("g", portion);
            na.put(o);
            sp.edit().putString("custom_foods", na.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static void removeCustom(android.content.Context c, String name) {
        try {
            android.content.SharedPreferences sp =
                    c.getSharedPreferences("edzo", android.content.Context.MODE_PRIVATE);
            org.json.JSONArray a = new org.json.JSONArray(sp.getString("custom_foods", "[]"));
            org.json.JSONArray na = new org.json.JSONArray();
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.optJSONObject(i);
                if (o != null && !o.optString("n", "").equalsIgnoreCase(name)) na.put(o);
            }
            sp.edit().putString("custom_foods", na.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Saját + beépített ételek együtt (a sajátok elöl, így ők nyernek). */
    public static List<Food> all(android.content.Context c) {
        List<Food> out = custom(c);
        for (Food f : ALL) out.add(f);
        return out;
    }

    /** A lekérdezéshez legjobban illő étel, vagy null. Ragozott alakokat is talál. */
    public static Food find(String query) { return find(java.util.Arrays.asList(ALL), query); }

    public static Food find(android.content.Context c, String query) {
        return find(all(c), query);
    }

    static Food find(List<Food> list, String query) {
        String q = mask(norm(query)).trim();
        if (q.isEmpty()) return null;
        // 1) teljes kifejezés-egyezés a szótövekkel
        for (Food f : list)
            for (String st : f.stems)
                if (q.equals(norm(st))) return f;
        // 2) a lekérdezés tartalmazza a szótövet (pl. "csirkemellbol" ⊃ "csirkemell")
        Food best = null; int bestLen = 0;
        for (Food f : list)
            for (String st : f.stems) {
                String ns = norm(st);
                if (ns.length() > bestLen && q.contains(ns)) { best = f; bestLen = ns.length(); }
            }
        if (best != null) return best;
        // 3) Szavankénti egyezés, ha a 2. fázis nem talált semmit: ragozott alak
        // (a szó a szótővel kezdődik, pl. "rizzsel" → "riz"), vagy gépelés közbeni
        // előtag (a szótő kezdődik a szóval). Mindkettőnél a leghosszabb szótő nyer.
        //
        // Megjegyzés: ezt szándékosan NEM engedjük a 2. fázis elé. Kipróbáltuk, és
        // rosszabb lett: a "rizs" a Rizsszeletre, a "burg" a Hamburgerre esett
        // volna a Rizs, illetve a Burgonya helyett.
        for (String tok : q.split("[ ,]+")) {
            if (tok.isEmpty()) continue;
            Food byTok = null; int tokLen = 0;
            for (Food f : list)
                for (String st : f.stems) {
                    String ns = norm(st);
                    if (ns.isEmpty()) continue;
                    boolean hit = tok.startsWith(ns)
                            || (tok.length() >= 4 && ns.startsWith(tok));
                    if (hit && ns.length() > tokLen) { byTok = f; tokLen = ns.length(); }
                }
            if (byTok != null) return byTok;
        }
        return null;
    }

    /**
     * Darabsúlyok azokhoz az ételekhez, amiket természetes darabra számolni
     * („2 tojás", „3 banán"). Csak ezeknél értelmezünk mértékegység nélküli
     * számot darabszámként – másutt egy puszta szám nem jelent semmit.
     */
    private static final String[][] PIECE_GRAMS = {
            {"Tojás", "55"}, {"Tükörtojás", "55"},
            // Rántottánál a „darab" a felhasznált tojás: „3 tojásból rántotta".
            {"Rántotta", "55"},
            {"Banán", "120"}, {"Alma", "150"}, {"Narancs", "150"}, {"Körte", "150"},
            {"Kivi", "80"}, {"Mandarin", "100"}, {"Őszibarack", "150"},
            {"Mangó", "200"}, {"Datolyaszilva", "150"},
            {"Zsemle", "55"}, {"Kifli", "55"}, {"Kenyér", "35"},
            {"Túró rudi", "51"}, {"Müzliszelet", "30"}, {"Palacsinta", "60"},
            {"Virsli", "50"}, {"Kakaós csiga", "90"}, {"Fasírt", "60"},
            {"Szendvics", "150"}, {"Hot-dog", "150"},
            // A v28.2-ben érkezett ételek közül azok, amiket természetes darabra
            // mondani („2 pogácsa", „három szilva").
            {"Pogácsa", "30"}, {"Túrós batyu", "100"}, {"Bundás kenyér", "60"},
            {"Datolya", "8"}, {"Szilva", "50"}, {"Sárgarépa", "80"},
            {"Hurka", "120"},
            // Amit a próbafuttatás szerint természetes darabra mondani, de eddig
            // a tipikus adaggal számolt („3 keksz", „2 tortilla", „2 paradicsom").
            {"Keksz", "12"}, {"Tortilla / wrap", "60"}, {"Paradicsom", "120"},
            {"Paprika", "120"}, {"Fagylalt", "50"}, {"Proteinszelet", "60"},
            {"Péksütemény", "60"}, {"Tojásfehérje", "33"},
            // Egészben fogyasztott fogások és poharas italok: a „2 burrito"
            // vagy a „két fröccs" darabja egy teljes adag.
            {"Hamburger", "250"}, {"Burrito", "300"}, {"Quesadilla", "200"},
            {"Cordon bleu", "180"}, {"Falafel", "25"}, {"Sushi", "30"},
            {"Fröccs", "300"}, {"Limonádé", "300"}, {"Kombucha", "330"},
            {"Ayran", "250"},
            // Italok a szokásos kiszereléssel: korsó sör, pohár bor, feles.
            // A mért mennyiség („2 dl", „fél liter") erősebb a darabnál.
            {"Szaloncukor", "15"}, {"Mézeskalács", "25"},
            {"Víz / ásványvíz", "250"},
            {"Sör", "500"}, {"Bor (vörös/fehér)", "150"}, {"Pálinka / tömény", "40"},
            {"Tej", "200"}, {"Üdítő (cukros)", "330"}, {"Energiaital", "250"},
            {"Kávé (fekete)", "200"}, {"Tejeskávé / cappuccino", "250"},
            {"Tea (cukrozatlan)", "250"},
    };

    /**
     * Kiírt számnevek ékezet nélkül. Csak egész szóként és csak darabra
     * számolható étel előtt érvényesek, ezért a többjelentésű alakok („hat",
     * „het", „fel") sem okoznak félreértést.
     */
    private static final String[][] NUMBER_WORDS = buildNumberWords();

    /**
     * Az alap számnevek mellett a tízesek és az összetett alakok is
     * („negyvenöt gramm", „huszonöt dkg") – kézzel felsorolni mind a
     * nyolcvanat hibalehetőség lenne, ezért generáljuk őket.
     */
    private static String[][] buildNumberWords() {
        java.util.List<String[]> out = new java.util.ArrayList<>(java.util.Arrays.asList(
                new String[][]{
                        {"egy", "1"}, {"ket", "2"}, {"ketto", "2"}, {"harom", "3"},
                        {"negy", "4"}, {"ot", "5"}, {"hat", "6"}, {"het", "7"},
                        {"nyolc", "8"}, {"kilenc", "9"}, {"tiz", "10"},
                        {"husz", "20"}, {"fel", "0.5"}, {"masfel", "1.5"},
                        {"negyed", "0.25"}, {"haromnegyed", "0.75"},
                }));
        String[][] tens = {{"tizen", "10"}, {"huszon", "20"}, {"harminc", "30"},
                {"negyven", "40"}, {"otven", "50"}, {"hatvan", "60"},
                {"hetven", "70"}, {"nyolcvan", "80"}, {"kilencven", "90"}};
        String[][] units = {{"egy", "1"}, {"ketto", "2"}, {"ket", "2"}, {"harom", "3"},
                {"negy", "4"}, {"ot", "5"}, {"hat", "6"}, {"het", "7"},
                {"nyolc", "8"}, {"kilenc", "9"}};
        for (String[] t : tens) {
            // A „tizen"/„huszon" csak összetételben szám, a többi magában is.
            if (!t[0].equals("tizen") && !t[0].equals("huszon"))
                out.add(new String[]{t[0], t[1]});
            for (String[] u : units)
                out.add(new String[]{t[0] + u[0],
                        String.valueOf(Integer.parseInt(t[1]) + Integer.parseInt(u[1]))});
        }
        return out.toArray(new String[0][]);
    }

    /** Ha itt egész szóként kiírt számnév kezdődik, a hossza; különben 0. */
    private static int numberWordAt(String q, int at) {
        if (at > 0 && Character.isLetter(q.charAt(at - 1))) return 0;
        for (String[] w : NUMBER_WORDS) {
            int end = at + w[0].length();
            if (q.startsWith(w[0], at) && (end >= q.length() || !Character.isLetter(q.charAt(end))))
                return w[0].length();
        }
        return 0;
    }

    private static double numberWordVal(String q, int at, int len) {
        for (String[] w : NUMBER_WORDS)
            if (w[0].length() == len && q.startsWith(w[0], at)) return Double.parseDouble(w[1]);
        return 0;
    }

    /**
     * Számlálószavak: a szám és az étel közé beékelődhetnek, de nem változtatnak
     * a jelentésen. A „3 szelet kenyér” három kenyérszelet – eddig viszont a
     * szám és az étel közé beékelődött „szelet” miatt az egész nem darabszámnak
     * látszott, és a tipikus adaggal (egy szeletnyivel) számolt tovább.
     */
    private static final String[] COUNT_WORDS =
            {"db", "darab", "szelet", "gombóc", "gomboc", "pohar", "pohár",
             "korso", "korsó", "feles", "csesze", "csésze", "doboz", "uveg", "üveg",
             "kupica", "stampedli"};

    /**
     * Adag-szorzó mérőszavak: a tipikus adagot sokszorozzák. A „két tányér
     * gulyás" két teljes adag, az „egy kanál méz" pont egy adagnyi (a kencék
     * adagja eleve egy kanálnyi), az „egy marék dió" egy maréknyi (30 g).
     */
    private static final String[] PORTION_WORDS =
            {"tanyer", "tanyernyi", "bogre", "bogrenyi", "talka", "talkanyi",
             "kanal", "kanalnyi", "evokanal", "evokanalnyi", "teaskanal",
             "teaskanalnyi", "marek", "mareknyi", "csomag", "zacsko", "tal"};

    /**
     * A szám közvetlenül az étel előtt áll-e – legfeljebb egy számlálószóval
     * közte? A visszatérés a közbeékelt szó ("" ha nincs), vagy null, ha ott
     * valami más áll – akkor a szám nem ehhez az ételhez tartozik.
     */
    /** Méret-jelzők: nem mérőszavak, csak közéállnak („2 nagy alma"). */
    private static final String[] SIZE_WORDS =
            {"nagy", "kis", "kicsi", "kozepes", "szep", "hatalmas", "apro"};

    private static String countWordAt(String q, int numEnd, int foodPos) {
        if (foodPos < numEnd) return null;
        String between = q.substring(numEnd, foodPos).trim();
        // A méret-jelző nem szakítja el a számot az ételtől: a „2 nagy alma"
        // két alma, nem egy – eddig a jelző miatt elveszett a kettes.
        boolean stripped = true;
        while (stripped) {
            stripped = false;
            for (String w : SIZE_WORDS)
                if (between.equals(w)) { return ""; }
                else if (between.startsWith(w + " ")) {
                    between = between.substring(w.length() + 1).trim();
                    stripped = true;
                    break;
                }
        }
        if (between.isEmpty()) return between;
        for (String w : COUNT_WORDS) if (w.equals(between)) return between;
        for (String w : PORTION_WORDS) if (w.equals(between)) return between;
        for (String w : new String[]{"adag", "adagot", "adagnyi", "porcio", "tabla"})
            if (w.equals(between)) return between;
        // A szótő HOSSZABB szó belsejébe is eshet: a „fél tábla étcsoki"
        // csokoládé-töve az „étcsoki" közepén van, így a mérőszó mögött ott
        // maradt egy szótöredék („tabla et"), és a fél tábla elveszett –
        // a bejegyzés a tipikus adaggal, vagyis feleannyival ment tovább.
        // A szótő ugyanannak a SZÓNAK a belsejében is lehet: a „két
        // görögdinnye" dinnye-töve elé „görög" került, és a kettes elveszett.
        // Ha a szám és a tő között egyetlen, szóköz nélküli szótöredék áll,
        // az ugyanaz a szó – vagyis a szám közvetlenül az ételhez tartozik.
        String raw = q.substring(numEnd, foodPos);
        if (raw.matches("^\\s?[a-z0-9]+$")) return "";
        int sp = between.lastIndexOf(' ');
        if (sp > 0) {
            String head = between.substring(0, sp).trim();
            for (String w : COUNT_WORDS) if (w.equals(head)) return head;
            for (String w : PORTION_WORDS) if (w.equals(head)) return head;
            for (String w : new String[]{"adag", "adagot", "adagnyi", "porcio", "tabla"})
                if (w.equals(head)) return head;
        }
        return null;
    }

    /**
     * Amit SZELETRE esznek, de darabra nem mondanák. A „két szelet pizza"
     * eddig egy egész pizzának számított – több mint háromszoros kalóriának –,
     * a „fél pizza" viszont tényleg fél pizza, ezért ez a tábla csak a
     * „szelet" mérőszóra él.
     */
    static final String[][] SLICE_GRAMS = {
            {"Pizza", "100"}, {"Sajttorta", "120"}, {"Sütemény", "80"},
    };

    private static int sliceGrams(Food f) {
        for (String[] p : SLICE_GRAMS) if (p[0].equals(f.name)) return Integer.parseInt(p[1]);
        return 0;
    }

    private static boolean isPortionWord(String w) {
        for (String p : PORTION_WORDS) if (p.equals(w)) return true;
        return false;
    }

    /** Egy darab hány gramm, vagy 0, ha ezt az ételt nem darabra számoljuk. */
    static int pieceGrams(Food f) {
        for (String[] p : PIECE_GRAMS)
            if (p[0].equals(f.name)) {
                try { return Integer.parseInt(p[1]); } catch (NumberFormatException e) { return 0; }
            }
        return 0;
    }

    /**
     * Tagmondat-sorszám minden karakterre. A mennyiség csak a SAJÁT tagmondatán
     * belüli ételhez tartozhat: az „1 l víz és 30 g mandula" mondatban a vízhez
     * írt 1000 g különben a mandulára szállt volna át (majd' hatezer kalória).
     *
     * Határok: vessző, pontosvessző, pont, „+", illetve az „és" és a „meg"
     * önálló szóként.
     */
    static int[] clauses(String q) {
        int[] c = new int[q.length()];
        int idx = 0;
        for (int i = 0; i < q.length(); i++) {
            char ch = q.charAt(i);
            if (ch == ',' || ch == ';' || ch == '.' || ch == '+') {
                // Két számjegy KÖZÖTT a vessző/pont tizedeselválasztó („2,5 dl"),
                // nem tagmondat-határ – különben a szám és az étele elválna.
                boolean decimal = i > 0 && i + 1 < q.length()
                        && Character.isDigit(q.charAt(i - 1)) && Character.isDigit(q.charAt(i + 1));
                if (!decimal) idx++;
                c[i] = idx;
                continue;
            }
            if ((ch == 'e' || ch == 'm') && isWordAt(q, i, ch == 'e' ? "es" : "meg")) {
                idx++;
                int len = ch == 'e' ? 2 : 3;
                for (int k = i; k < i + len && k < q.length(); k++) c[k] = idx;
                i += len - 1;
                continue;
            }
            c[i] = idx;
        }
        return mergeQuantityOnly(q, c, idx);
    }

    /** Mértékegységek és töltelékszavak – ezek magukban nem jelentenek ételt. */
    private static final String[] QTY_WORDS = {
            "g", "gr", "gramm", "dkg", "deka", "kg", "kilo", "kilogramm",
            "dl", "deci", "deciliter", "ml", "l", "liter", "db", "darab",
            "kb", "korulbelul", "nagyjabol", "cca", "osszesen",
    };

    /**
     * A csak mennyiséget tartalmazó tagmondat a szomszédjához tartozik.
     *
     * A „mandula, 30 g” két tagmondat, de egy étel – a 30 g enélkül nem talált
     * volna vissza a mandulához. A „mandula: 30 g” és a „mandula 30 g” viszont
     * működött, vagyis a felhasználó azon múlt, melyik írásjelet választja.
     * Ha a tagmondatban a szám és a mértékegység mellett bármi más szó áll
     * („1 l víz és mandula”), akkor marad a határ: az a mennyiség nem a
     * szomszéd ételé.
     */
    private static int[] mergeQuantityOnly(String q, int[] c, int last) {
        if (last <= 0) return c;
        StringBuilder[] txt = new StringBuilder[last + 1];
        for (int i = 0; i <= last; i++) txt[i] = new StringBuilder();
        for (int i = 0; i < q.length(); i++) txt[c[i]].append(q.charAt(i));
        boolean[] qty = new boolean[last + 1];
        for (int i = 0; i <= last; i++) qty[i] = onlyQuantity(txt[i].toString());

        int[] map = new int[last + 1];
        int next = 0;
        map[0] = 0;
        for (int i = 1; i <= last; i++) map[i] = qty[i] ? map[i - 1] : ++next;
        // Ha a mennyiség áll elöl („30 g, mandula"), előre olvad be.
        if (qty[0] && last >= 1) map[0] = map[1];
        for (int i = 0; i < q.length(); i++) c[i] = map[c[i]];
        return c;
    }

    /** Szám + mértékegység (+ töltelékszó), étel nélkül. */
    private static boolean onlyQuantity(String s) {
        boolean sawNumber = false;
        int i = 0;
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (Character.isDigit(ch)) { sawNumber = true; i++; continue; }
            if (Character.isLetter(ch)) {
                int st = i;
                while (i < s.length() && Character.isLetter(s.charAt(i))) i++;
                String w = s.substring(st, i);
                boolean known = false;
                for (String k : QTY_WORDS) if (k.equals(w)) { known = true; break; }
                for (String[] k : NUMBER_WORDS) if (k[0].equals(w)) { known = true; sawNumber = true; }
                if (!known) return false;
                continue;
            }
            i++;                                  // szóköz, írásjel
        }
        return sawNumber;
    }

    /** A szó pontosan itt kezdődik és itt is ér véget (nem része hosszabb szónak). */
    private static boolean isWordAt(String q, int at, String w) {
        if (!q.startsWith(w, at)) return false;
        if (at > 0 && Character.isLetter(q.charAt(at - 1))) return false;
        int end = at + w.length();
        return end >= q.length() || !Character.isLetter(q.charAt(end));
    }

    /** Egy felismert étel a szövegben, a hozzá tartozó grammal (0 = nem volt megadva). */
    public static final class Hit {
        public final Food food;
        public final double grams;
        Hit(Food food, double grams) { this.food = food; this.grams = grams; }
    }

    /**
     * Ételek felismerése a beírt szövegből úgy, hogy a mellettük álló
     * gramm-mennyiséget is kiolvassuk: „150 g csirkemell rizzsel",
     * „csirkemell 150g, rizs 200 g". Egy gramm-érték ahhoz az ételhez tartozik,
     * amelyik a szövegben a legközelebb áll hozzá (előtte vagy utána).
     * Ahol nincs szám, a gramm 0 marad, és a hívó dönt (közös adag / tipikus adag).
     */
    public static List<Hit> parse(android.content.Context c, String query) {
        return parse(all(c), query);
    }

    static List<Hit> parse(List<Food> list, String query) {
        List<Match> ms = matches(list, query);
        List<Hit> out = new ArrayList<>();
        if (ms.isEmpty()) {
            // Az „ittam másfél litert" ital-név nélkül is vizet jelent.
            Hit w = waterOnly(list, query);
            if (w != null) out.add(w);
            return out;
        }

        String q = norm(query);
        // Az ételek szövegbeli helye – ugyanaz, amit a felismerés használt.
        List<Food> foods = new ArrayList<>();
        List<Integer> foodPos = new ArrayList<>();
        List<Integer> foodLen = new ArrayList<>();
        for (Match m : ms) { foods.add(m.food); foodPos.add(m.pos); foodLen.add(m.len); }
        // Gramm-értékek kigyűjtése: szám + (opcionális szóköz) + "g"/"gr"/"dkg".
        List<Integer> numPos = new ArrayList<>();
        List<Double> numVal = new ArrayList<>();
        List<Integer> bareNumPos = new ArrayList<>();
        List<Double> bareNumVal = new ArrayList<>();
        List<Integer> bareNumLen = new ArrayList<>();
        int i = 0;
        while (i < q.length()) {
            int start = i;
            double val;
            // Kiírt számnév is állhat mértékegység előtt: „fél liter tej”,
            // „két deci tej”, „egy kiló alma”. Csak akkor számoljuk mennyiségnek,
            // ha tényleg mértékegység követi – a puszta „fél alma” a darabszámos
            // ághoz tartozik, és nem szabad kétszer beszámítani.
            int wordLen = numberWordAt(q, i);
            if (wordLen > 0) {
                val = numberWordVal(q, i, wordLen);
                i += wordLen;
            } else if (Character.isDigit(q.charAt(i))) {
                while (i < q.length() && Character.isDigit(q.charAt(i))) i++;
                // Tizedes rész is lehet, magyarul vesszővel („2,5 dl"). Enélkül a
                // „2,5" két külön számnak látszott, és a „2" elveszett.
                if (i + 1 < q.length() && (q.charAt(i) == ',' || q.charAt(i) == '.')
                        && Character.isDigit(q.charAt(i + 1))) {
                    i++;
                    while (i < q.length() && Character.isDigit(q.charAt(i))) i++;
                }
                try { val = Double.parseDouble(q.substring(start, i).replace(',', '.')); }
                catch (NumberFormatException e) { continue; }
            } else {
                i++;
                continue;
            }
            // „Két és fél deci", „2 és fél dl": az egész és a tört összeadódik
            // – enélkül a kettő elveszett, és fél deci maradt.
            int e1 = i;
            while (e1 < q.length() && q.charAt(e1) == ' ') e1++;
            if (q.startsWith("es", e1)
                    && (e1 + 2 >= q.length() || !Character.isLetter(q.charAt(e1 + 2)))) {
                int e2 = e1 + 2;
                while (e2 < q.length() && q.charAt(e2) == ' ') e2++;
                for (String[] f : new String[][]{{"haromnegyed", "0.75"},
                        {"negyed", "0.25"}, {"fel", "0.5"}})
                    if (q.startsWith(f[0], e2)
                            && (e2 + f[0].length() >= q.length()
                            || !Character.isLetter(q.charAt(e2 + f[0].length())))) {
                        val += Double.parseDouble(f[1]);
                        i = e2 + f[0].length();
                        break;
                    }
            }
            int j = i;
            while (j < q.length() && q.charAt(j) == ' ') j++;
            // A hosszabb mértékegység előbb, különben a rövidebb ág nyelné el.
            // Folyadéknál 1 ml ≈ 1 g, ezért a dl/ml/l is grammra váltható.
            if (q.startsWith("dkg", j)) { numPos.add(start); numVal.add(val * 10); i = j + 3; }
            // Kiírva is: „10 deka párizsi" (a maradék „gramm" ártalmatlan).
            else if (q.startsWith("deka", j)) { numPos.add(start); numVal.add(val * 10); i = j + 4; }
            else if (q.startsWith("kilo", j)) { numPos.add(start); numVal.add(val * 1000); i = j + 4; }
            else if (q.startsWith("kg", j)) { numPos.add(start); numVal.add(val * 1000); i = j + 2; }
            else if (q.startsWith("gramm", j)) { numPos.add(start); numVal.add(val); i = j + 5; }
            else if (q.startsWith("deci", j)) { numPos.add(start); numVal.add(val * 100); i = j + 4; }
            else if (q.startsWith("dl", j)) { numPos.add(start); numVal.add(val * 100); i = j + 2; }
            else if (q.startsWith("ml", j)) { numPos.add(start); numVal.add(val); i = j + 2; }
            // Kiírva is: a puszta „l" ág megköveteli, hogy ne betű kövesse.
            else if (q.startsWith("liter", j)) { numPos.add(start); numVal.add(val * 1000); i = j + 5; }
            else if (q.startsWith("gr", j)) { numPos.add(start); numVal.add(val); i = j + 2; }
            else if (q.startsWith("g", j)
                    && (j + 1 >= q.length() || !Character.isLetter(q.charAt(j + 1)))) {
                numPos.add(start); numVal.add(val); i = j + 1;
            } else if (q.startsWith("l", j)
                    && (j + 1 >= q.length() || !Character.isLetter(q.charAt(j + 1)))) {
                numPos.add(start); numVal.add(val * 1000); i = j + 1;
            } else if (wordLen > 0) {
                // Kiírt számnév mértékegység nélkül: a darabszámos ág veszi fel
                // lentebb, itt nem szabad hozzányúlni (különben kétszer számítana).
                i = start + 1;
            } else {
                // Mértékegység nélküli szám: darabszám lehet („2 tojás"). Csak akkor
                // vesszük annak, ha rögtön utána egy darabra számolható étel áll –
                // különben a szám nem jelent semmit, és figyelmen kívül hagyjuk.
                bareNumPos.add(start);
                bareNumVal.add(val);
                bareNumLen.add(i - start);
            }
        }
        // Mennyiség-plafon: 50 kg fölött a szám elgépelés, nem adag. Egy
        // „9999999999 g" alakú elütés különben milliárd-kalóriás étkezésként
        // mérgezné meg a napi összesítőt, a statisztikát és a diagramokat –
        // némán. Ilyenkor inkább mennyiség nélkül hagyjuk, mint a képtelen
        // darabszámnál: ott 20 a határ.
        for (int k2 = numVal.size() - 1; k2 >= 0; k2--) {
            double v = numVal.get(k2);
            if (v <= 0 || v > 50_000) { numVal.remove(k2); numPos.remove(k2); }
        }
        double[] grams = new double[foods.size()];
        // Minden gramm-érték a hozzá legközelebbi ételhez kerül, amelyiknek még nincs.
        //
        // A távolságot az étel MINDKÉT végétől mérjük. Korábban csak a kezdetétől:
        // a „csirkemell 150g, rizs 200 g" alakban a 150 közelebb volt a „rizs"
        // szó elejéhez, mint a tíz betűs „csirkemell" elejéhez – vagyis a két
        // mennyiség felcserélődött. Épp ez a formátum szerepel példaként.
        int[] clause = clauses(q);
        for (int n = 0; n < numPos.size(); n++) {
            int bestIdx = -1, bestDist = Integer.MAX_VALUE;
            for (int k = 0; k < foods.size(); k++) {
                if (grams[k] > 0 || foodPos.get(k) < 0) continue;
                // Csak a szám saját tagmondatán belül keresünk ételt.
                if (clause[foodPos.get(k)] != clause[numPos.get(n)]) continue;
                int from = foodPos.get(k), to = from + foodLen.get(k);
                int d = Math.min(Math.abs(from - numPos.get(n)), Math.abs(to - numPos.get(n)));
                if (d < bestDist) { bestDist = d; bestIdx = k; }
            }
            if (bestIdx >= 0) grams[bestIdx] = numVal.get(n);
        }
        // Kiírt számnevek is számítanak („két tojás", „fél alma"), egész szóként.
        for (String[] w : NUMBER_WORDS) {
            int at = 0;
            while (true) {
                int p = q.indexOf(w[0], at);
                if (p < 0) break;
                at = p + w[0].length();
                boolean leftOk = p == 0 || !Character.isLetter(q.charAt(p - 1));
                boolean rightOk = at >= q.length() || !Character.isLetter(q.charAt(at));
                if (leftOk && rightOk) {
                    bareNumPos.add(p);
                    bareNumVal.add(Double.parseDouble(w[1]));
                    bareNumLen.add(w[0].length());
                }
            }
        }
        // Darabszámok: „2 tojás" = 2 × egy tojás súlya. Csak akkor számít, ha a
        // szám közvetlenül egy darabra számolható étel előtt áll, az étel még nem
        // kapott grammot, és a darabszám életszerű (legfeljebb 20).
        for (int n = 0; n < bareNumPos.size(); n++) {
            double count = bareNumVal.get(n);
            if (count < 0.5 || count > 20) continue;
            int numEnd = bareNumPos.get(n) + bareNumLen.get(n);
            for (int k = 0; k < foods.size(); k++) {
                if (grams[k] > 0 || foodPos.get(k) < 0) continue;
                String between = countWordAt(q, numEnd, foodPos.get(k));
                // A darabszám egy KÉSŐBBI előfordulás előtt is állhat
                // („sörözés: 3 korsó sör" – az étel tárolt pozíciója az első
                // említésé, a szám mégis a másodikhoz tartozik).
                if (between == null) {
                    for (String st : foods.get(k).stems) {
                        String ns = norm(st);
                        if (ns.isEmpty()) continue;
                        int p2 = q.indexOf(ns, numEnd);
                        if (p2 < 0) continue;
                        between = countWordAt(q, numEnd, p2);
                        if (between != null) break;
                    }
                }
                if (between == null) continue;
                // Az „adag" bármely ételre megy: egy adag a tipikus adag.
                // A „fél adag gyros" így 175 gramm, a „2 adag gulyás" dupla.
                double piece = between.equals("tabla") ? 100      // egy tábla csoki
                        : between.equals("szelet") && sliceGrams(foods.get(k)) > 0
                        ? sliceGrams(foods.get(k))
                        : between.startsWith("adag") || between.equals("porcio")
                                || isPortionWord(between)
                        ? foods.get(k).portion : pieceGrams(foods.get(k));
                // Aminek nincs természetes darabmérete, ott a tipikus adag a
                // darab: a „két kebab" eddig egyetlen kebabnak számított, mert
                // a számláló egyszerűen elveszett. Ez 267 ételt érintett.
                //
                // Csak életszerű adagszámra: a „két wrap" és a „fél kebab"
                // valódi bevitel, a „12 rizs" viszont inkább elgépelt gramm,
                // és három kiló rizst írni a naplóba rosszabb, mint egy adagot.
                if (piece <= 0 && count <= 6) piece = foods.get(k).portion;
                if (piece <= 0) continue;
                grams[k] = count * piece;
                break;
            }
        }
        // Az adag a név UTÁN is állhat: „grillcsirke fél adag". A számot a
        // legközelebbi megelőző, még mennyiség nélküli étel kapja – de csak a
        // saját tagmondatán belül.
        for (int n = 0; n < bareNumPos.size(); n++) {
            double count = bareNumVal.get(n);
            if (count < 0.5 || count > 20) continue;
            int numStart = bareNumPos.get(n);
            int numEnd = numStart + bareNumLen.get(n);
            String after = numEnd < q.length() ? q.substring(numEnd).trim() : "";
            if (!(after.startsWith("adag") || after.startsWith("porcio"))) continue;
            int best = -1;
            for (int k = 0; k < foods.size(); k++) {
                if (grams[k] > 0 || foodPos.get(k) < 0 || foodPos.get(k) >= numStart) continue;
                if (clause[foodPos.get(k)] != clause[numStart]) continue;
                if (best < 0 || foodPos.get(k) > foodPos.get(best)) best = k;
            }
            if (best >= 0) grams[best] = count * foods.get(best).portion;
        }
        for (int k = 0; k < foods.size(); k++) out.add(new Hit(foods.get(k), grams[k]));
        return out;
    }

    /** Az összes étel, ami a szövegben felismerhető (a szöveg sorrendjében, ismétlés nélkül). */
    public static List<Food> findAll(String query) {
        return findAll(java.util.Arrays.asList(ALL), query);
    }

    public static List<Food> findAll(android.content.Context c, String query) {
        return findAll(all(c), query);
    }

    /**
     * Összetett ételek: ha a felsorolt szavak mind szerepelnek a szövegben, akkor
     * együtt EGY ételt jelentenek, nem külön-külön hozzávalókat. A „csirkemellből
     * rántott húst" enélkül Csirkemell + Rántott hús (sertés) lenne, vagyis a húst
     * kétszer számolnánk – holott a Rántott csirkemell pont ezt írja le.
     *
     * Az első elem a cél-étel neve, a többi a keresett szó.
     */
    private static final String[][] COMBOS = {
            {"Rántott csirkemell", "rantott", "csirke"},
            // A rántotta tojásból van: a „3 tojásból rántotta" eddig a tojást ÉS a
            // rántottát is elszámolta, egy háromtojásos reggeli így 526 kcal lett
            // 270 helyett. (A „rántotta és 2 tojás" alak – ami ugyanannak a
            // fogásnak a kétszeri említése volna – szintén egy tételre olvad.)
            {"Rántotta", "tojas", "rantotta"},
    };

    /** Egy találat helye a szövegben (a leghosszabb illeszkedő szótő szerint). */
    static final class Match {
        final Food food; final int pos, len;
        Match(Food food, int pos, int len) { this.food = food; this.pos = pos; this.len = len; }
    }

    /**
     * A szövegben felismerhető ételek, helyükkel együtt.
     *
     * Fontos: egy rövid szótő beleeshet egy hosszabb ételnévbe („sajt" a
     * „rántott sajt"-ban, „tej" a „tejföl"-ben, „riz" a „rizsszelet"-ben).
     * Ilyenkor csak a hosszabb találat marad, különben ugyanaz a falat kétszer
     * kerülne be, és dupla kalóriát számolnánk.
     */
    static List<Match> matches(List<Food> list, String query) {
        String q = mask(norm(query));
        List<Match> found = new ArrayList<>();
        for (Food f : list) {
            int bestPos = -1, bestLen = 0;
            for (String st : f.stems) {
                String ns = norm(st);
                if (ns.isEmpty()) continue;
                int p = q.indexOf(ns);
                // A leghosszabb illeszkedő szótő dönt; azonos hossznál a korábbi.
                if (p >= 0 && (ns.length() > bestLen || (ns.length() == bestLen && p < bestPos))) {
                    bestPos = p; bestLen = ns.length();
                }
            }
            if (bestPos >= 0) found.add(new Match(f, bestPos, bestLen));
        }
        // A hosszabb találatba beleeső rövidebbeket eldobjuk.
        List<Match> out = new ArrayList<>();
        for (Match m : found) {
            boolean covered = false;
            for (Match o : found) {
                if (o == m) continue;
                boolean inside = o.pos <= m.pos && o.pos + o.len >= m.pos + m.len;
                if (inside && o.len > m.len) { covered = true; break; }
            }
            if (!covered) out.add(m);
        }
        out = oneFoodPerWord(q, out);
        out = dropRedundantBase(out);
        applyCombos(list, q, out);
        out = dropNegated(q, out);
        // Rendezés a szövegbeli előfordulás szerint.
        for (int i = 0; i < out.size(); i++)
            for (int j = i + 1; j < out.size(); j++)
                if (out.get(j).pos < out.get(i).pos) {
                    Match t = out.get(i); out.set(i, out.get(j)); out.set(j, t);
                }
        return out;
    }

    /**
     * A kész fogás mellől eldobjuk a benne lévő alapanyagot.
     *
     * A „bolognai tészta" két találatot adott: a bolognai spagettit ÉS a főtt
     * tésztát – vagyis egy tányér makaróniból 950 kalória lett. A jelzős
     * szerkezetnél („carbonara tészta", „milánói tészta") a fogás neve és az
     * alap külön szóban áll, ezért az átfedés-szűrő nem fogja meg őket.
     *
     * Szándékosan felsorolás, nem szabály: a „zöldséges rizs" ugyanígy két
     * külön szó, de ott a rizs tényleg külön tétel. Csak azokat a fogásokat
     * soroljuk fel, amelyek a kalóriájukban MÁR tartalmazzák az alapot.
     */
    private static final String[][] BASE_INCLUDED = {
            {"Tészta (főtt)", "Bolognai spagetti", "Tészta carbonara", "Milánói makaróni",
                    "Sajtos tészta", "Lasagne", "Túrós csusza", "Töltött tészta (tortellini)",
                    "Mákos tészta", "Grízes tészta", "Káposztás tészta",
                    "Grenadírmars (krumplis tészta)", "Pad thai"},
            {"Rizs (főtt)", "Rizottó", "Rizses hús", "Sushi", "Poke bowl", "Tejberizs"},
            {"Saláta (zöld)", "Görög saláta", "Csirkés saláta", "Tonhalsaláta",
                    "Caprese saláta", "Uborkasaláta", "Céklasaláta", "Krumplisaláta",
                    "Franciasaláta / coleslaw"},
            {"Burgonya (főtt)", "Sült krumpli", "Rakott krumpli", "Krumplisaláta",
                    "Grenadírmars (krumplis tészta)"},
    };

    private static List<Match> dropRedundantBase(List<Match> in) {
        if (in.size() < 2) return in;
        List<Match> out = new ArrayList<>(in);
        for (String[] row : BASE_INCLUDED) {
            Match base = null;
            boolean dish = false;
            for (Match m : out) {
                if (m.food.name.equals(row[0])) base = m;
                for (int i = 1; i < row.length; i++)
                    if (m.food.name.equals(row[i])) { dish = true; break; }
            }
            if (base != null && dish) out.remove(base);
        }
        return out;
    }

    /**
     * Ivás étel-találat nélkül: ha csak mennyiség van („ittam másfél litert",
     * „megittam 2 litert"), azt víznek vesszük – ebből lesz a vízcél-jóváírás.
     */
    private static Hit waterOnly(List<Food> list, String query) {
        String s = norm(query);
        if (!s.contains("ittam") && !s.contains("iszom")) return null;
        if (looksNegated(query)) return null;
        double liters = -1;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+(?:[.,]\\d+)?)\\s?(liter|l\\b|dl|deci)").matcher(s);
        if (m.find()) {
            try {
                double v = Double.parseDouble(m.group(1).replace(',', '.'));
                String u = m.group(2);
                liters = (u.equals("dl") || u.equals("deci")) ? v / 10.0 : v;
            } catch (NumberFormatException ignored) {}
        } else if (s.contains("masfel liter")) liters = 1.5;
        else if (s.contains("fel liter")) liters = 0.5;
        else if (s.contains("egy liter")) liters = 1;
        else if (s.contains("ket liter")) liters = 2;
        if (liters <= 0 || liters > 5) return null;
        for (Food f : list) if (f.name.startsWith("Víz")) return new Hit(f, liters * 1000);
        return null;
    }

    /** Tagadó bevitel: az üres találat oka nem az, hogy nem ismerjük az ételt. */
    public static boolean looksNegated(String query) {
        String s = norm(query == null ? "" : query);
        for (String w : new String[]{"nem ettem", "nem ittam", "nem eszem",
                "nem iszom", "nem kertem", "kihagytam", "helyett", "nelkul"})
            if (s.contains(w)) return true;
        return false;
    }

    /**
     * Tagadás és csere: ami nem került a tányérra, az a naplóba se kerüljön.
     * A „chips helyett almát ettem" chipse és a „csoki nélkül kértem" csokija
     * kimarad, ahogy a „nem ettem csokit" tagmondatának ételei is.
     */
    private static List<Match> dropNegated(String q, List<Match> in) {
        if (in.isEmpty()) return in;
        java.util.HashSet<Match> dead = new java.util.HashSet<>();
        // Az étel közvetlenül a „helyett"/„nélkül" előtt áll (rövid rag belefér).
        for (String w : new String[]{"helyett", "nelkul"}) {
            int p = q.indexOf(w);
            while (p >= 0) {
                Match best = null;
                for (Match m : in) {
                    int end = m.pos + m.len;
                    if (end <= p && p - end <= 6 && (best == null || m.pos > best.pos))
                        best = m;
                }
                if (best != null) dead.add(best);
                p = q.indexOf(w, p + 1);
            }
        }
        // Tagadott ige után, ugyanabban a tagmondatban álló ételek.
        int[] cls = clauses(q);
        for (String w : new String[]{"nem ettem", "nem eszem", "nem ittam",
                "nem iszom", "nem kertem", "kihagytam"}) {
            int p = q.indexOf(w);
            while (p >= 0) {
                for (Match m : in)
                    if (m.pos > p && cls[m.pos] == cls[p]) dead.add(m);
                p = q.indexOf(w, p + 1);
            }
        }
        if (dead.isEmpty()) return in;
        List<Match> out = new ArrayList<>();
        for (Match m : in) if (!dead.contains(m)) out.add(m);
        return out;
    }

    /**
     * Egy SZÓ mindig egy étel.
     *
     * A magyar összetett ételnevekben két szótő is illeszkedik, egymás mellé:
     * „lencsefőzelék” = lencse + főzelék, „sajtosszendvics” = sajt + szendvics,
     * „csokitorta” = csoki + sütemény, „babgulyás” = bab + gulyásleves. Eddig
     * mindkét fele bekerült, vagyis ugyanazt az EGY fogást kétszer számoltuk el
     * – egy tányér lencsefőzelék így közel dupla kalóriának látszott. A hiba
     * nem néhány szót érint: életszerű szavak harmadánál előjött.
     *
     * A szó melyik fele maradjon? Az, amelyik egy adagban több kalóriát ad:
     * az áll közelebb magához a fogáshoz. A „csokitorta” inkább sütemény, mint
     * csokoládé, a „csirkemellsaláta” inkább csirkemell, mint zöldsaláta.
     */
    private static List<Match> oneFoodPerWord(String q, List<Match> in) {
        List<Match> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            boolean beaten = false;
            for (int j = 0; j < in.size() && !beaten; j++) {
                if (i == j || !sameWord(q, in.get(i), in.get(j))) continue;
                beaten = beats(in.get(j), j, in.get(i), i);
            }
            if (!beaten) out.add(in.get(i));
        }
        return out;
    }

    /**
     * Szigorú sorrend, hogy egy szóból pontosan egy találat maradjon: adagnyi
     * kalória, majd a hosszabb szótő, végül a sorrend dönt.
     */
    private static boolean beats(Match a, int ai, Match b, int bi) {
        int wa = a.food.portion * a.food.kcal100, wb = b.food.portion * b.food.kcal100;
        if (wa != wb) return wa > wb;
        if (a.len != b.len) return a.len > b.len;
        return ai < bi;
    }

    /**
     * Egy szón belül van-e a két találat? Akkor igen, ha köztük (és bennük)
     * nincs szóköz vagy írásjel – így a „csirkemell rizzsel” két külön étel
     * marad, a „csirkemellsaláta” viszont egy.
     */
    private static boolean sameWord(String q, Match a, Match b) {
        int from = Math.min(a.pos, b.pos);
        int to = Math.max(a.pos + a.len, b.pos + b.len);
        if (from < 0 || to > q.length()) return false;
        for (int i = from; i < to; i++) {
            char ch = q.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != '-') return false;
        }
        return true;
    }

    /** Az összetett-étel szabályok alkalmazása a már megtalált találatokra. */
    private static void applyCombos(List<Food> list, String q, List<Match> out) {
        for (String[] combo : COMBOS) {
            int from = Integer.MAX_VALUE, to = -1;
            boolean all = true;
            for (int i = 1; i < combo.length; i++) {
                int p = q.indexOf(combo[i]);
                if (p < 0) { all = false; break; }
                from = Math.min(from, p);
                to = Math.max(to, p + combo[i].length());
            }
            if (!all) continue;
            Food target = null;
            for (Food f : list) if (f.name.equals(combo[0])) { target = f; break; }
            if (target == null) continue;
            // A szavak által lefedett szakaszra eső találatok helyébe lép az
            // összetett étel; a szakaszon kívüli körettel nem csinálunk semmit.
            for (int i = out.size() - 1; i >= 0; i--) {
                Match m = out.get(i);
                if (m.pos < to && m.pos + m.len > from) out.remove(i);
            }
            out.add(new Match(target, from, to - from));
        }
    }

    static List<Food> findAll(List<Food> list, String query) {
        List<Food> out = new ArrayList<>();
        for (Match m : matches(list, query)) out.add(m.food);
        return out;
    }

    /** Ismert ételnevek (javaslatokhoz). */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (Food f : ALL) out.add(f.name);
        return out;
    }
}
