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
        /**
         * A szótövek ékezet nélküli alakja, EGYSZER kiszámolva.
         *
         * A felismerő minden leütésre végigmegy az egész adatbázison – ezer
         * fölötti szótövön –, és eddig mindegyiket ott helyben normalizálta:
         * kisbetű, ékezet, írásjel. Ez tette a felismerést negyvenszer
         * lassabbá a többi felismerőnél; a mérés szerint a hatszázhúsz
         * mikroszekundum java része ez volt.
         */
        final String[] nstems;

        Food(String name, int kcal100, double prot100, int portion, String... stems) {
            this.name = name; this.kcal100 = kcal100; this.prot100 = prot100;
            this.portion = portion; this.stems = stems;
            this.nstems = new String[stems.length];
            for (int i = 0; i < stems.length; i++) this.nstems[i] = norm(stems[i]);
        }
    }

    public static final Food[] ALL = {
        new Food("Rántott hús (sertés)", 320, 22, 180, "rantott hus", "rantotthus", "becsi",
                "rantott szelet", "rantottszelet"),
        new Food("Rántott csirkemell", 250, 25, 180, "rantott csirke",
                // A gyorséttermi csíkok is bundásak – nem grillmell. (A
                // „csirkefalat" a nuggeté.)
                "csirkecsik", "csirke csik", "kfc csirke"),
        new Food("Csirkemell (sült/grill)", 165, 31, 150, "csirkemell", "csirke mell", "csirke",
                "grillcsirke", "teriyaki"),
        new Food("Csirkecomb", 210, 26, 150, "csirkecomb", "comb", "csirkeszarny"),
        // Egészben sült csirke: a bőrrel-csonttal tálalt adag zsírosabb, mint
        // a grillezett mell, és a tepsiben sült zöldség zsírja is rámegy.
        new Food("Tepsis csirke", 200, 20, 300, "tepsis csirke", "tepsiben sult csirke",
                "egeszben sult csirke"),
        new Food("Pulykamell", 105, 23, 150, "pulyka"),
        new Food("Sertéskaraj", 240, 27, 150, "karaj", "sertes", "tarja",
                "naturszelet", "natur szelet", "szuzerme", "szuzpecsenye", "flekken"),
        // A „steak" szóban benne van a „tea": a hosszabb tő nyeli el, így a
        // „tofu steak" nem naplóz egy csésze teát is.
        // Vad és bárány: a magyar konyha rendszeres vendégei, de eddig
        // egyetlen tő sem fogta őket. A vadhús soványabb a marhánál, a bárány
        // zsírosabb – egy kalapba téve mindkettő hazudna.
        new Food("Vadhús (szarvas, vaddisznó, nyúl)", 160, 30, 150,
                "vadhus", "vad hus", "szarvashus", "szarvas hus", "ozhus", "oz hus",
                "vaddiszno", "nyulhus", "nyul hus", "facan", "fogolyhus"),
        new Food("Bárány / birka", 250, 25, 150, "barany", "baranyhus", "birka",
                "birkahus", "birkaporkolt"),
        new Food("Marhahús", 250, 26, 150, "marha", "belszin", "steak", "stek",
                "rostelyos", "ozgerinc"),
        new Food("Fasírt", 290, 15, 150, "fasirt", "fasiroz", "vagdalt", "stefania"),
        // Vadas: marhahús tejfölös-zöldséges mártásban, jellemzően zsemlegombóccal.
        new Food("Vadas hús", 150, 14, 350, "vadas", "vadas hus", "vadashus"),
        // A „kolbásszal" alakban a sz megkettőződik, ezért az is szótő.
        new Food("Kolbász", 350, 15, 100, "kolbasz", "kolbassz",
                // A magyar kolbászfajták neve önmagában is kolbászt jelent.
                "gyulai", "csabai", "debreceni", "szegedi", "lecsokolbasz"),
        // A „bécsi virsli" NEM bécsi szelet: a hosszabb szótő menti meg a
        // rántott hústól (320 kcal helyett 250).
        new Food("Virsli", 250, 10, 100, "virsli", "becsi virsli", "frankfurti virsli"),
        new Food("Sonka", 120, 18, 50, "sonka"),
        new Food("Szalámi", 400, 22, 30, "szalami"),
        new Food("Bacon", 500, 13, 30, "bacon", "szalonna"),
        new Food("Hal (fehér)", 120, 22, 150, "hal", "pisztrang", "ponty", "harcsa",
                // A boltok pultjának többi gyakori fehér hala is.
                // Az „amur" szándékosan hiányzik: a szaMURáj-szerű szavak
                // belsejébe esne. Aki amurt eszik, írja körül („fehér hal").
                "keszeg", "fogas", "sullo", "busa", "tilapia", "pangasius", "tokehal"),
        new Food("Tenger gyümölcsei", 90, 18, 150, "garnela", "kagylo", "polip",
                "tenger gyumolcsei", "rakkoktel", "rak koktel", "kaviar",
                // A „rák" magában is étel – a hosszabb tövek (rákkoktél)
                // továbbra is elsőbbséget kapnak.
                "rakhus", "garnelarak", "scampi"),
        new Food("Tonhal", 130, 24, 100, "tonhal"),
        new Food("Lazac", 210, 20, 150, "lazac"),
        new Food("Makréla / szardínia", 220, 20, 100, "makrela", "szardinia", "sprotni"),
        new Food("Tojás", 155, 13, 110, "tojas", "tojcsi"),
        // A „tojásfehérje" eddig egész tojás volt: 33 g helyett 55 g, és 17 kcal
        // helyett 78 – négy és félszeres túlszámolás egy sportolós alapdarabon.
        new Food("Tojásfehérje", 52, 11, 33, "tojasfeherje", "tojas feherje", "feherje tojas"),
        new Food("Rántotta", 180, 12, 150, "rantotta", "omlett", "shakshuka"),
        new Food("Rizs (főtt)", 130, 2.7, 200, "riz"),
        // A „durum" hosszabb tő, különben a benne lévő „rum" tömény italt adna.
        // A puszta „durum" a kebabos tekercs (dürüm), nem durumbúza tészta:
        // aki tésztát naplóz, azt „tésztának", „spagettinek" írja. A „durum
        // tészta" teljes alakja viszont egyértelmű, és hosszabb tőként nyer.
        new Food("Tészta (főtt)", 150, 5, 250, "durum teszta", "durumteszta",
                "teszta", "spagetti", "penne"),
        new Food("Burgonya (főtt)", 87, 2, 250, "burgonya", "krumpli", "krumpi"),
        new Food("Sült krumpli", 300, 3.5, 150, "sult krumpli", "sultkrumpli",
                "hasabburgonya", "hasáb"),
        new Food("Burgonyapüré", 110, 2, 200, "burgonyapure", "krumplipure", "pure"),
        new Food("Édesburgonya", 90, 1.6, 200, "edesburgonya", "batata"),
        new Food("Bulgur (főtt)", 120, 4, 200, "bulgur", "arpagyongy"),
        new Food("Polenta / puliszka", 85, 2, 250, "polenta", "puliszka"),
        new Food("Quinoa (főtt)", 120, 4.4, 200, "quinoa"),
        new Food("Kenyér", 250, 8, 70, "kenyer", "piritos", "bagett",
                "ciabatta", "focaccia", "bruschetta", "toast", "tosztkenyer"),
        new Food("Zsemle", 280, 9, 55, "zsemle", "zsemi"),
        new Food("Kifli", 290, 8, 55, "kifli"),
        new Food("Péksütemény", 350, 7, 80, "peksutemenny", "peksutemeny", "croissant",
                "brios", "molnark", "bagel"),
        new Food("Zabpehely", 370, 13, 50, "zab", "kasa", "feherjes zabkasa",
                "protein zabkasa", "overnight oats", "oats"),
        new Food("Müzli", 380, 9, 60, "muzli", "granola",
                // Ékezet nélkül gépelve leggyakrabban így: „musli".
                "musli"),
        // A „kakaós palacsinta" teljes alakja szótő, különben a kakaó egy
        // bögre tejes kakaónak számítana a kanálnyi töltelék helyett.
        new Food("Palacsinta", 220, 6, 150, "palacsinta", "kakaos palacsinta",
                "protein palacsinta", "feherje palacsinta", "pancake"),
        new Food("Pizza", 260, 11, 300, "pizza", "calzone", "quattro formaggi",
                "quattro stagioni"),
        new Food("Hamburger", 280, 13, 250, "hamburger", "burger", "big mac", "bigmac",
                "whopper", "cheeseburger", "hambi"),
        new Food("Gyorséttermi menü", 220, 8, 500, "gyorsettermi menu", "mcmenu", "happy meal",
                // Ahogy a magyar mondja: „mekis kaja", „kfc kosár". A puszta
                // márkanév is menüt jelent – aki csak a helyet írja le, az a
                // szokásos tálcára gondol.
                "mekis menu", "mekis kaja", "mcdonalds", "mcdonald's", "meki",
                "kfc kosar", "kfc menu", "burger king menu"),
        new Food("Gyros", 220, 15, 350, "gyros", "souvlaki"),
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
                "tarhonyaleves", "tarhonya leves", "minestrone", "gazpacho",
                "tom yum", "laksa", "harira"),
        // A „vacsorára túró és zöldség" zöldsége eddig eltűnt: vegyes köret.
        new Food("Zöldség (vegyes / párolt)", 40, 2, 200, "zoldseg", "vitaminsalata",
                "pak choi", "pakchoi", "mangold", "articsoka", "edeskomeny",
                // A ritkább kerti zöldségek is ebbe a sávba esnek. Az „okra"
                // szándékosan hiányzik: a magyar -okra rag (sorozatOKRA,
                // dolgOKRA) minden második mondatban ott van – a saját
                // tesztünk fogta meg.
                "csicsoka"),
        // A torma és a gyömbér FŰSZERNYI mennyiség: kanálnyi adag, nem
        // kétszáz grammos zöldség-köret. A „gyömbéres tea" zöldségadagként
        // nyolcvan kalóriát tett volna a nulla kalóriás tea mellé.
        new Food("Torma / gyömbér", 60, 1, 10, "torma", "gyomber"),
        new Food("Rakott krumpli", 160, 6, 350, "rakott krumpli", "rakott", "moussaka"),
        // Disznótoros: hurka, kolbász és sült hús egy tányéron – a magyar
        // konyha egyik legnehezebb fogása, egy adag közel ezer kalória.
        new Food("Disznótoros", 330, 16, 300, "disznotoros", "disznotor"),
        // Tócsni (lapcsánka, tócsi): olajban sült reszelt krumpli.
        new Food("Tócsni / lapcsánka", 250, 4, 150, "tocsni", "lapcsanka", "tocsi",
                "krumplilangos", "berthake"),
        new Food("Töltött dagadó", 330, 18, 200, "toltott dagado", "dagado"),
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
        new Food("Liszt", 350, 10, 30, "liszt", "rizsliszt", "zabliszt", "buzaliszt",
                // A zsemlemorzsa egy kanálnyi panír, nem egy egész zsemle.
                "zsemlemorzsa", "panirmorzsa", "buzadara", "kukoricadara"),
        new Food("Brokkoli", 35, 2.8, 150, "brokkoli"),
        new Food("Karfiol", 25, 2, 150, "karfiol"),
        new Food("Paradicsom", 18, 0.9, 100, "paradicsom", "pari"),
        // Külön sor, hogy a koktél-ital rövidebb töve ne nyerje el, ÉS mert a
        // szeme hatoda a nagy paradicsoménak – a „10 szem" csak így ad jó számot.
        new Food("Koktélparadicsom", 18, 0.9, 100, "koktelparadicsom", "koktel paradicsom"),
        new Food("Uborka", 15, 0.7, 100, "uborka", "ubi"),
        new Food("Paprika", 25, 1, 100, "paprika"),
        // A „gyümölcssaláta" korábban zöld salátára esett: 200 grammból 30
        // kalória lett a valós ~120 helyett, mert a rövidebb „salata" tő nyert.
        new Food("Gyümölcssaláta", 60, 0.8, 200, "gyumolcssalata",
                "gyumolcs salata"),
        new Food("Saláta (zöld)", 15, 1.4, 50, "salata", "sali",
                // A „rukkola" vége „kola" – a hosszabb tő elfedi az üdítőt.
                "rukkola", "endivia", "radicchio", "lollo rosso", "cikoria"),
        new Food("Sajt (trappista)", 360, 25, 30, "sajt", "trappista", "parenyica",
                // A gouda ugyanabban a sávban van, és a boltok polcán ott a
                // trappista mellett – eddig egyetlen tő sem fogta.
                "gouda", "eidami", "edami"),
        new Food("Mozzarella", 280, 22, 50, "mozzarella"),
        // A „parmezán" MÉZNEK számított (a „mez" tő beleesett a szóba).
        new Food("Parmezán", 400, 35, 20, "parmezan"),
        new Food("Camembert / brie", 300, 20, 50, "camembert", "brie"),
        new Food("Feta", 270, 14, 50, "feta"),
        new Food("Mascarpone", 435, 4, 50, "mascarpone"),
        new Food("Ricotta", 150, 11, 50, "ricotta"),
        new Food("Túró", 100, 12, 100, "turo", "turcsi"),
        new Food("Krémtúró / túródesszert", 180, 8, 90,
                "kremturo", "turodesszert", "turokrem"),
        new Food("Joghurt", 60, 4, 150, "joghurt", "joghi"),
        new Food("Ivójoghurt", 75, 3, 200, "ivojoghurt", "joghurtital", "actimel"),
        new Food("Puding", 120, 3, 200, "puding", "csokipuding", "protein puding",
                "vaniliasodo", "vanilias sodo", "madartejsodo",
                "protein pudding", "pudding"),
        new Food("Madártej", 120, 4, 250, "madartej"),
        new Food("Tejszínhab", 300, 2, 30, "tejszinhab", "tejszin"),
        new Food("Tejszelet", 420, 5, 28, "tejszelet", "monte"),
        new Food("Milkshake", 110, 3, 300, "milkshake", "tejturmix"),
        new Food("Görög joghurt", 120, 9, 150, "gorog joghurt"),
        new Food("Tej", 60, 3.3, 200, "tej"),
        new Food("Zsírszegény tej", 38, 3.4, 200, "zsirszegeny tej", "sovany tej"),
        new Food("Vaj", 720, 0.9, 10, "vaj"),
        // A napraforgóOLAJ 900 kcal, a napraforgóMAG 580 – egy szótő
        // különbség, másfélszeres kalória.
        new Food("Olaj", 900, 0, 10, "olaj", "napraforgo olaj", "napraforgoolaj",
                "sertes zsir", "serteszsir", "zsir", "libazsir", "kacsazsir"),
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
        // A grapefruit a fogyókúrás reggelik klasszikusa – eddig nem létezett.
        new Food("Grapefruit", 42, 0.8, 200, "grapefruit", "grepfrut", "greipfrut"),
        // Sütőtök: az őszi konyha alapja, és a „tök" magában túl rövid tő
        // (a „tökmag" és a „tökéletes" is tartalmazza).
        new Food("Sütőtök", 40, 1, 200, "sutotok", "suto tok", "tokfozelek nyers"),
        new Food("Szőlő", 70, 0.7, 100, "szolo"),
        new Food("Eper", 33, 0.7, 100, "eper"),
        new Food("Avokádó", 160, 2, 70, "avokado"),
        new Food("Dió", 650, 15, 30, "dio", "makadamia", "makadamdio"),
        new Food("Mandula", 580, 21, 30, "mandula"),
        new Food("Mogyoró", 570, 25, 30, "mogyoro"),
        new Food("Csokoládé", 550, 5, 25, "csoki", "csokolade", "kinder", "milka", "twix",
                "bounty", "snickers", "kitkat", "mars szelet", "sport szelet",
                "balaton szelet", "3bit", "milky way"),
        // A „mentos" tő szándékosan hiányzik: a „mentős"-be esne bele.
        new Food("Gumicukor / cukorka", 340, 0, 30, "gumicukor", "cukorka",
                "haribo", "skittles", "tic tac", "nyaloka", "savanyu cukor"),
        new Food("Keksz", 450, 6, 40, "kekssz", "keksz", "oreo", "linzer", "zabkeksz",
                // A „csokis keksz" EGY süti: enélkül a csoki és a keksz külön
                // tételként, kétszeres kalóriával került a naplóba.
                "zabpelyhes keksz", "csokis keksz", "csokis kekssz",
                "haztartasi keksz"),
        new Food("Sütemény", 400, 5, 100, "sutemenny", "sutemeny", "torta", "baklava",
                "zserbo", "rigo jancsi", "isler", "puncsszelet", "mignon",
                "flodni", "macaron", "suti", "eszterhazy", "dobostorta",
                "dobos torta", "somloi kocka", "eclair", "ekler", "profiterol",
                "rakoczi turos", "rakoczi"),
        new Food("Muffin / brownie", 380, 5, 80, "muffin", "cupcake", "brownie"),
        new Food("Gofri", 350, 6, 100, "gofri", "waffle", "protein gofri"),
        new Food("Energiagolyó", 420, 8, 25,
                "energiagolyo", "kokuszgolyo", "zabgolyo", "proteingolyo"),
        new Food("Fagylalt", 200, 3.5, 100, "fagyi", "fagylalt", "jegkrem",
                "protein jegkrem", "sundae"),
        new Food("Chips", 540, 6, 50, "chips", "nachos", "proteinchips",
                // A „tortilla chips" eddig KÉT tétel volt (tortilla + chips):
                // egy lapos kenyér kalóriája a nassolnivaló mellé.
                "protein chips", "tortilla chips", "tortillachips", "nacho"),
        // Nassolás-kör: a hagymakarika hagymának (20 kcal!), a szaloncukor
        // kanál cukornak, a mézeskalács kalácsnak számított.
        new Food("Hagymakarika (rántott)", 280, 4, 100, "hagymakarika"),
        new Food("Perec", 380, 9, 50, "perec", "pretzel"),
        new Food("Ropi / kréker", 400, 9, 30, "ropi", "kreker", "sajtos taller", "taller",
                "sos rud", "sospalcika", "sos palcika"),
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
                "cukor nelkul", "cukor nelkuli", "cukrozatlan", "edesitovel", "edesito",
                // A boltok polcán így írják: coke zero, pepsi max, diet coke,
                // diétás kóla. A „zéró" ékezettel is gyakori. Az édesítőszerek
                // (eritrit, stevia, xilit) szintén nulla kalória.
                "coke zero", "zero coke", "pepsi max", "diet coke", "dietas kola",
                "dietas udito", "zero cukros", "zero udito", "zero szorp",
                "eritrit", "stevia", "xilit", "nyirfacukor"),
        new Food("Rántott sajt", 330, 18, 120, "rantott sajt"),
        new Food("Nokedli / galuska", 170, 5, 200, "nokedli", "galuska", "knedli",
                "zsemlegomboc", "zsemle gomboc"),
        // Krumplis tészta zsírban sütve, tejföllel – nem könnyű köret.
        new Food("Dödölle", 200, 4, 300, "dodolle", "dodolye"),
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
                "grizgaluska leves", "grizgaluskaleves", "grizgaluska", "majgomboc",
                // A teljes alak szótő, különben a „csirke" tő vinné el a
                // levest sült csirkemellnek.
                "csirkeleves", "csirke leves", "tyukhusleves", "tyukleves",
                // Az erőleves tiszta húsleves: az „átlagos" leves ötszörös
                // kalóriát írt rá (200 helyett 40 kcal/100 g).
                "eroleves", "ero leves", "csontleves", "bouillon",
                // A miszó leves is tiszta lé: 35 kcal/100 g körül.
                "miso leves", "misoleves", "miszo leves"),
        new Food("Kocsonya", 90, 12, 300, "kocsonya", "aszpik"),
        new Food("Franciakrumpli (rakott)", 140, 7, 400, "franciakrumpli"),
        // A teljes „harcsapaprikás" alak szótő, különben a harcsa (hal) és a
        // paprikás kettőnek számolna.
        // A gombapaprikás hústalan: a „paprikás" tő eddig a csirkéset hozta rá,
        // vagyis a húsmentes fogás a csirke kalóriáját és fehérjéjét kapta.
        new Food("Gombapaprikás", 95, 3.5, 300, "gombapaprikas", "gomba paprikas"),
        new Food("Csirkepaprikás", 160, 14, 300, "paprikas", "harcsapaprikas"),
        new Food("Milánói makaróni", 180, 7, 350, "milanoi", "makaroni"),
        new Food("Lasagne", 160, 9, 350, "lasagne", "lazanya"),
        new Food("Tészta carbonara", 180, 8, 350, "teszta carbonara", "carbonara",
                "fettuccine alfredo", "alfredo"),
        new Food("Töltött tészta (tortellini)", 180, 8, 300, "toltott teszta", "tortellini",
                "ravioli", "cannelloni"),
        // A puszta „turmix" magyarul gyümölcsös: nem fehérjeturmix. A teljes
        // „protein turmix" alak szótő, így az egyben marad.
        // A „fehérjeturmix" magyar alakja is IDE tartozik: a rövidebb „turmix"
        // tő a gyümölcsöset vitte, és tíz gramm fehérje helyett eggyel meg
        // negyven kalóriával kevesebbel írta be a naplóba.
        new Food("Protein turmix", 100, 10, 300, "protein turmix", "protein", "shake",
                "kazein turmix", "whey turmix", "gainer",
                "feherjeturmix", "feherje turmix", "feherjeshake"),
        new Food("Gyümölcsturmix / smoothie", 60, 1, 300, "turmix", "smoothie", "acai"),
        // Maga a POR, nem a kész turmix: a „30 g fehérjepor” eddig vagy semmit nem
        // talált, vagy a 100 kcal/100 g-os kész italra esett – harmadannyi kalória.
        new Food("Fehérjepor", 380, 75, 30, "feherjepor", "feherje por", "protein por",
                "proteinpor", "tejsavofeherje", "tejsavo", "whey", "kazein",
                // Teljes kifejezésként is, hogy a rövidebb „protein" tő ne
                // hozzon MELLÉ egy kész turmixot is („whey protein 30 g").
                "whey protein", "kazein protein", "kollagen"),
        new Food("Proteinszelet", 350, 30, 60, "proteinszelet", "protein szelet",
                "feherjeszelet", "feherje szelet", "energiaszelet"),
        new Food("Túró rudi", 380, 8, 51, "turo rudi", "rudi"),
        new Food("Szendvics", 250, 10, 150, "szendviccs", "szendvics", "szendo", "croque"),
        new Food("Hot-dog", 290, 10, 150, "hot-dog", "hotdog", "hot dog"),
        // A puszta „szelet” szótő itt nem lehet: hétköznapi szó, ami mennyiséget
        // jelöl („két szelet kenyér”, „egy szelet torta”), nem ételt.
        new Food("Müzliszelet", 400, 6, 30, "muzliszelet", "zabszelet", "granolaszelet"),
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
        // Pita és lepénykenyér: a gyros és a humusz mellé is ez jár, és eddig
        // egyszerűen nem létezett – a köret kalóriája elveszett.
        new Food("Pita / lepénykenyér", 270, 9, 80, "pita", "lepenykenyer", "naan",
                "gorog lepeny"),
        // A növényi húspogácsa könnyebb a marhánál, de nem diétás étel.
        new Food("Vega burger", 220, 11, 220, "vega burger", "vegan burger",
                "novenyi burger", "veggie burger"),
        new Food("Túrós csusza", 210, 10, 300, "turos csusza", "csusza", "turos teszta"),
        new Food("Grízes tészta", 200, 6, 300, "grizes teszta", "griz"),
        // Menza-kör: a mákos tészta mákja eddig eltűnt (csak főtt tészta lett),
        // a grenadírmars és a rántott zöldség pedig ismeretlen volt.
        // A leves betétje is tészta – de nem egy adag köret: a „húsleves
        // cérnametélttel" eddig csak leves volt, a főtt tészta adagjával
        // viszont háromszáz kalóriát tett volna hozzá.
        new Food("Levestészta", 150, 5, 50, "cernametelt", "levesteszta",
                "leves teszta", "kockateszta"),
        new Food("Mákos tészta", 250, 7, 300, "makos teszta",
                // A diós tészta ugyanaz a fogás, más magvval: eddig két
                // tételre esett szét (dió + főtt tészta).
                "dios teszta", "diosteszta"),
        new Food("Tarhonyás hús", 160, 10, 400, "tarhonyas hus"),
        new Food("Grenadírmars (krumplis tészta)", 150, 4, 400,
                "grenadir", "krumplis teszta"),
        // A bográcsos slambuc krumpli + tészta + szalonna: nehezebb, mint a
        // grenadírmars, ezért saját tétel.
        new Food("Slambuc", 210, 6, 350, "slambuc", "slamboc"),
        new Food("Rántott zöldség", 180, 5, 200, "rantott karfiol",
                "rantott zoldseg", "rantott brokkoli", "rantott cukkini"),
        new Food("Kakaós csiga", 380, 7, 90, "kakaos csiga", "csiga"),
        // A „meggyes rétes" két szó, egy sütemény: a teljes alak szótő, hogy a
        // gyümölcs ne számolódjon külön tételként mellé.
        new Food("Rétes", 300, 5, 100, "almas retes", "meggyes retes", "turos retes",
                "makos retes", "kapros retes", "retes"),
        // A „kevert süti" a fogás NEVE: a „süti" tő különben egy külön adag
        // süteményt tett mellé.
        new Food("Piskóta / kevert süti", 350, 6, 80, "piskota", "kevert suti", "kevert"),
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
        // A növényi sajt kevesebb kalória és lényegesen kevesebb fehérje, mint
        // a trappista – sajtként számolva a fehérje háromszorosát írtuk volna.
        new Food("Növényi sajt", 280, 2, 30, "vegan sajt", "vega sajt",
                "novenyi sajt"),
        new Food("Edamame", 120, 11, 100, "edamame"),
        new Food("Seitan", 140, 25, 100, "seitan", "szejtan"),
        new Food("Tempeh", 190, 19, 100, "tempeh"),
        new Food("Kókusztej", 190, 2, 100, "kokusztej"),
        // A reszelt kókusz sűrű, mint a magvak: egy evőkanálnyi is számít.
        new Food("Kókuszreszelék", 660, 7, 20, "kokuszreszelek", "reszelt kokusz",
                "kokuszreszel", "kokuszliszt"),
        new Food("Csirkés saláta", 130, 12, 300, "csirkes salata", "cezar salata", "cezar", "caesar"),
        new Food("Sushi", 150, 6, 250, "sushi", "maki", "nigiri", "sashimi",
                "sushi tekercs"),
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
        new Food("Curry", 150, 10, 300, "curry", "tikka masala", "masala"),
        new Food("Ramen", 120, 6, 500, "ramen", "ramen leves", "pho leves", "pho"),
        new Food("Pad thai", 170, 8, 350, "pad thai", "padthai", "pad see"),
        new Food("Burrito", 190, 10, 300, "burrito"),
        new Food("Taco", 220, 10, 120, "taco", "enchilada", "fajita"),
        new Food("Chilis bab (con carne)", 120, 8, 400,
                "chilis bab", "chili con carne", "con carne", "chili sin carne",
                "sin carne"),
        new Food("Tavaszi tekercs / gyoza", 200, 7, 150,
                "tavaszi tekercs", "spring roll", "gyoza", "dim sum"),
        new Food("Wok (zöldséges-húsos)", 120, 10, 350, "wok", "bibimbap"),
        // Kínai büfé: a bundázott, szószos csirke messze nem a wok kalóriája.
        // A TELJES név is szótő, nem csak a rövidebb alakjai: a darabszám
        // közvetlenül a felismert tő elé kell essen, és a „2 kínai bundás
        // csirke" számát a köztes szó különben elvágta a tőtől.
        new Food("Kínai bundás csirke", 250, 14, 250, "kinai bundas csirke",
                "kinai csirke", "bundas csirke",
                "szechuan", "szecsuani", "kung pao", "edes-savanyu csirke",
                "edes savanyu csirke"),
        new Food("Quesadilla", 250, 11, 200, "quesadilla"),
        new Food("Falafel", 300, 13, 150, "falafel"),
        new Food("Hummusz", 180, 8, 60, "hummus", "humusz",
                // A -val rag az sz-t megkettőzi: „humusszal". Egy m-mel ez a
                // gyakoribb magyar írásmód, és eddig nem talált.
                "humussz"),
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
        // A rövid „rakott kel" alak eddig a puszta „rakott" tövön a rakott
        // KRUMPLIRA esett: ugyanarra az adagra másfélszeres kalóriára.
        new Food("Rakott kelkáposzta", 120, 7, 400, "rakott kelkaposzta",
                "rakott kel", "rakottkel", "rakott kaposzta", "kolozsvari kaposzta"),
        new Food("Paprikás krumpli", 120, 4, 350, "paprikas krumpli"),
        new Food("Rizses hús", 160, 8, 350, "rizses hus"),
        new Food("Bolognai spagetti", 170, 8, 350, "bolognai spagetti", "spagetti bolognai", "bolognai"),
        new Food("Sajtos tészta", 220, 8, 300, "sajtos teszta", "mac and cheese",
                "macaroni and cheese"),
        new Food("Tojásos nokedli", 190, 7, 300, "tojasos nokedli"),
        new Food("Rizottó", 150, 5, 300, "rizotto", "risotto", "rizsotto", "paella"),
        new Food("Túrógombóc", 210, 9, 200, "turogomboc"),
        new Food("Szilvás gombóc", 190, 3, 250, "szilvas gomboc", "szilvasgomboc"),
        new Food("Káposztás tészta", 150, 4, 330, "kaposztas teszta", "kaposztasteszta",
                "cvekedli", "kaposztas cvekedli",
                // Ugyanaz a tál, más néven: a menzán „káposztás kocka".
                "kaposztas kocka", "kaposztaskocka", "kaposztas nudli"),
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
        new Food("Fánk / churros", 400, 5, 60, "fank", "churros", "donut", "doughnut"),
        new Food("Kürtőskalács", 380, 6, 120, "kurtoskalaccs", "kurtoskalacs", "trdelnik"),
        new Food("Rántott gomba", 220, 5, 150, "rantott gomba"),
        new Food("Gomba", 22, 3, 100, "gomba"),
        new Food("Csirkemáj", 130, 20, 120, "csirkemaj", "maj"),
        // A libamáj nem liba: a hízott máj zsírtartalma sokszorosa a húsénak.
        // A puszta „liba" tövön eddig a HÚS ment be – kevesebb kalóriával,
        // több fehérjével, mint a valóság.
        new Food("Libamáj / kacsamáj (sült)", 400, 11, 100, "libamaj", "kacsamaj",
                "hizott maj", "foie gras"),
        // A „tepertő" 33 kcal-os EPERNEK számított (az „eper" tő beleesett),
        // a disznósajt sajtnak, a májkrém nyers csirkemájnak.
        new Food("Tepertő", 700, 15, 50, "teperto", "topertyu"),
        new Food("Disznósajt", 280, 15, 100, "disznosajt"),
        new Food("Májkrém / kenőmájas", 330, 12, 30,
                "majkrem", "kenomajas", "majas", "pastetom"),
        new Food("Pacalpörkölt", 120, 12, 400, "pacalporkolt", "pacal"),
        new Food("Sült oldalas", 290, 20, 200, "sult oldalas", "oldalas"),
        new Food("Csülök", 280, 22, 200, "csulok"),
        new Food("Kacsa / liba", 300, 19, 180, "kacsa", "liba"),
        new Food("Mustár", 60, 4, 10, "mustar", "wasabi", "vaszabi"),
        new Food("Uborkasaláta", 40, 0.7, 150, "uborkasalata"),
        new Food("Céklasaláta", 45, 1.3, 100, "ceklasalata", "cekla salata", "cekla"),
        // A franciasaláta majonézes: nem 8 kcal-os zöldsaláta.
        // Majonézes hidegtálak: az „orosz hússaláta" és a „tojássaláta" a
        // majonéz miatt jóval sűrűbb, mint egy zöldsaláta – oda tartoznak.
        new Food("Franciasaláta / coleslaw", 170, 2.5, 150,
                "franciasalata", "francia salata", "coleslaw", "orosz hussalata",
                "orosz salata", "tojassalata", "kaszinotojas", "majonezes salata"),
        new Food("Savanyúság", 25, 1, 100, "savanyusag", "savanyu kaposzta", "kimchi",
                // Tengeri alga: kalóriában a savanyúsághoz áll a legközelebb.
                "wakame", "nori", "hinar", "tengeri alga",
                // A csalamádé is savanyúság – eddig egyetlen tő sem fogta.
                "csalamade", "vegyes savanyusag"),
        new Food("Spárga", 20, 2.2, 150, "sparga"),
        new Food("Karalábé", 27, 1.7, 150, "karalabe"),
        new Food("Retek", 16, 0.7, 50, "retek", "jegcsapretek"),
        new Food("Zeller", 18, 0.7, 100, "zeller"),
        new Food("Zöldbab", 35, 1.8, 150, "zoldbab"),
        new Food("Spenót / paraj", 25, 2.9, 200, "spenot", "paraj"),
        new Food("Krémleves (zöldség)", 60, 2, 350, "kremleves",
                "brokkoli kremleves", "sutotok kremleves", "gomba kremleves",
                // A gombaleves tejfölös, nem víztiszta: a gomba + „átlagos
                // leves" párosnál ez a közelebbi.
                "gombaleves", "gomba leves", "karfiolleves", "borsoleves"),
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
        new Food("Szilva", 46, 0.7, 100, "szilva", "ringlo"),
        new Food("Cseresznye / meggy", 60, 1, 150, "cseresznye", "meggy"),
        new Food("Datolya", 280, 2.5, 30, "datolya"),
        new Food("Tökmag / napraforgómag", 570, 22, 30, "tokmag", "napraforgomag",
                "napraforgo", "fenyomag", "magvak", "vegyes mag",
                // Százhuszonnyolc további fogást átnézve ez a kettő hiányzott
                // a magvak közül. (A „mák" szándékosan marad ki: három betű,
                // és a makaróniban is ott van.)
                "szezammag", "szezam"),
        new Food("Chia / lenmag", 490, 17, 15, "chia", "lenmag", "lenmagliszt"),
        new Food("Kesudió", 580, 18, 30, "kesudio", "kesu"),
        // A „kebab” szóban benne van a „bab”: eddig 200 g főtt bab lett belőle.
        // A „durum" a tekercsbe csavart kebab, nem durumbúza tészta – tésztaként
        // számolva a kalória harmada veszett el.
        new Food("Kebab", 250, 13, 350, "kebab", "durum", "durum tekercs", "doner",
                "doner kebab"),
        new Food("Kuszkusz (főtt)", 115, 4, 200, "kuszkussz", "kuszkusz", "couscous"),
        // A puszta „köles" korábban azért maradt ki, mert a szótő-illesztés a
        // „koleszterin" és a „koleszban" szó belsejében is talált volna. A
        // megoldás nem a szótő elhagyása, hanem a két ütköző szó álcázása
        // (NOT_FOOD) – így a legegyszerűbb alak, a „köles", is jó.
        new Food("Hajdina / köles (főtt)", 130, 5, 200, "hajdina", "haricska",
                "koles", "kolest", "kolessel", "kolesbol", "koleskasa",
                "koles kasa", "amarant"),
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
        // A beszélt nyelv rövidít: „pogi", „zsemi", „hambi", „tojcsi". A
        // három-négy betűs alakok közül csak azok kerültek be, amelyek nem
        // laknak hétköznapi szó elején (a „kifi" például a KIFIzetésben is).
        new Food("Pogácsa", 400, 8, 60, "sajtos pogacsa", "pogacsa", "pogi"),
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
                "egres", "josta", "homoktovis", "licsi", "bodza", "kokeny",
                "naspolya", "maracuja", "passiogyumolcs"),
        new Food("Mangó", 60, 0.8, 200, "mango"),
        new Food("Papaya", 45, 0.5, 150, "papaya", "papaja"),
        new Food("Datolyaszilva", 70, 0.6, 150, "datolyaszilva", "hurma"),
        new Food("Füge", 74, 0.8, 100, "fuge"),
        new Food("Befőtt / kompót", 70, 0.4, 150, "befott", "kompot"),
        new Food("Túrós batyu", 300, 7, 100, "turos batyu", "batyu"),
        // A -val/-vel hasonul: „kaláccsal". A cs+cs alakot külön tő fogja meg.
        new Food("Kalács / bejgli", 350, 8, 80, "kalacs", "kalaccs", "bejgli", "beigli",
                // A töltelék benne van a kalóriában: a „diós bejgli" ne
                // számoljon még egy adag diót is mellé.
                "dios bejgli", "makos bejgli", "makos tekercs", "dios tekercs"),
        // A puszta „pite" is süteménynyi: a „meggyes pite" eddig kilencven
        // kalóriás meggy volt, mert csak a gyümölcs töve illeszkedett rá.
        new Food("Pite (almás/gyümölcsös)", 240, 3, 120, "almas pite", "almaspite",
                "pite"),
        new Food("Krumplisaláta", 150, 2.5, 200, "krumplisalata", "krumpli salata",
                "burgonyasalata", "burgonya salata"),
        new Food("Frankfurti leves", 90, 4, 350, "frankfurti leves", "frankfurti"),
        new Food("Körözött", 250, 12, 80, "korozott", "liptoi", "liptauer"),
        new Food("Sajtkrém", 250, 8, 40, "sajtkrem"),
        // --- Italok ---
        new Food("Kávé (fekete)", 2, 0.2, 200, "kave", "feketekave", "eszpresszo",
                "espresszo", "espresso", "ristretto", "americano"),
        // A „macchiato" ide tartozik, és nem csak azért, mert kávé: a „chia" tő
        // a szó KÖZEPÉN illeszkedett rá, így a kávéból chiamag lett. A
        // jegeskávé sem fekete kávé – tej és jégkrém van benne.
        new Food("Tejeskávé / cappuccino", 55, 3, 250, "tejeskave", "cappuccino", "latte",
                "macchiato", "flat white", "jegeskave", "jeges kave",
                // Magyaros írásmóddal is: a kávézóban ezt kérik.
                "kapucsino", "kapuccino", "kapucino",
                // A bécsi kávéház szava, a magyar cukrászdában is ez áll az
                // étlapon – tejszínhabos tejeskávé.
                "melange", "melanzs"),
        new Food("Tea (cukrozatlan)", 1, 0, 250, "tea", "matcha"),
        // A víz nulla kalória, de attól még értsük: az „ittam 1,5 liter
        // vizet" ne legyen „nem értem" – és a napló is teljesebb tőle.
        // A „folyadék" a napi bevitel hétköznapi szava: a „2,5 liter folyadék
        // ment le ma" eddig sehol nem jelent meg.
        new Food("Víz / ásványvíz", 0, 0, 250, "viz", "asvanyviz", "szoda",
                "folyadek", "folyadekot", "folyadekbevitel"),
        // A rizling BOR, nem rizs – a hosszabb tő menti meg a „rizs"-től.
        new Food("Bor (vörös/fehér)", 80, 0.1, 150, "bor", "vorosbor", "feherbor",
                "rizling", "furmint", "kekfrankos", "cabernet", "chardonnay",
                // A rozé is bor: az étlapon a harmadik szín, és eddig
                // egyáltalán nem létezett a felismerőnek.
                "roze", "rozebor"),
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
            // IZOM- és GYAKORLATNEVEK, amikben étel-szótő lakik: a faRIZOMban
            // a rizs, az állCSÚSZÁSban a csusza. A rehab-lap ezeket a neveket
            // mutatja, tehát pont ezeket másolja be az ember.
            "farizom", "farizmo", "allcsusz", "allcsusztat",
            // A „vizesedik a térdem" ízületi folyadék, nem két és fél deci
            // ásványvíz: a panaszból eddig italbejegyzés lett.
            "vizesed", "vizeny", "vizretencio", "vizhajto",
            // A KAKAÓPOR szórás, nem két és fél deci kakaó: a „tejbegríz
            // kakaóporral" mellé eddig egy egész pohár tejes kakaó került a
            // naplóba, százötven kalóriával.
            "kakaopor", "kakao por", "kavepor", "vanilias cukor", "porcukor",
            "reggeli", "tizorai", "ebed", "uzsonna", "vacsor", "vacsi",
            "kaveskanal", "evokanal", "teaskanal",
            // Étel-tövet rejtő, gyakori NEM-étel szavak. Szó ELEJÉT nézzük,
            // ezért a ragozott igazi ételek („sajtos", „vajas", „sört")
            // érintetlenek maradnak.
            // A „parkolás" közepén a „kola", az „álmatlanság" elején az „alma".
            "parkol", "almatlan",
            // A magyar „-almas" melléknevek MIND tartalmazzák az almát:
            // hatALMAs, unALMAs, alkALMAs. A „hatalmas" ráadásul a saját
            // méret-jelzőink között is szerepel („hatalmas adag rizs"), tehát
            // minden ilyen mondathoz járt egy fantom alma – nyolcvan kalória.
            "hatalmas", "unalmas", "nyugalmas", "fajdalmas", "alkalmas", "alkalmaz",
            "irgalmas", "diadalmas", "aggodalmas", "siralmas", "jutalmaz", "jutalmas",
            "forradalmas", "hatalmaskod",
            // A halogatás nem hal, a borostyán nem bor. A „sajtó" kimarad: a
            // maszk a szó ELEJÉT nézi, és a „sajtos" ugyanígy kezdődik – egy
            // ritka szó kedvéért nem áldozzuk fel a sajtos tésztát.
            "halogat", "borostyan", "soreny", "soret",
            // A figyelMEZtetésben a méz, a memoRIZálásban a rizs. A puszta
            // „mező" is: a mezei és a mezőgazdaság már ki volt véve.
            "figyelmeztet", "memorizal", "mezo",
            // A „dió" három betű, és rengeteg szó közepén ott van: karDIÓ,
            // stáDIÓ, ráDIÓ, staDIOn, karDIOlógus. A kardió ráadásul EDZÉS-szó:
            // minden ilyen bejegyzéshez járt harminc gramm dió, kétszáz
            // kalória. A Gáborban és a Borókában a bor, a Salgótarjánban a
            // tarja, a tortúrában a túra.
            "kardio", "stadion", "radio", "studio", "audio", "periodus", "melodi",
            "gabor", "boroka", "borul", "borond", "tarjan", "salgotarjan",
            // A testZSÍR és a ZSÍRbevitel nem konyhai zsír: az egyik testérték,
            // a másik a napi összegzés szava.
            "testzsir", "zsirbevitel",
            // Az elMAJSZoltamban a máj: a nassolás mellé egy adag csirkemáj.
            "majszol", "elmajszol", "felmajszol", "megmajszol",
            "vajon", "hallott", "hallom", "halk", "halad", "halott", "halvany",
            "sajtotaj", "borzaszt", "labor", "tabor", "borult", "borus",
            "borotva", "borit", "borzalm", "sorban", "sorba", "sorra",
            "sorozat", "sorol", "sorren", "paradicsomi", "narancssarga",
            "kolbaszol", "rumli",
            // Átvitt értelmű és összetett álca-szavak: a „narancsbőr" nem bor,
            // a „sörhas" nem sör, a „kávészünet" nem kávé.
            // A magyar hangrend ékezet nélkül is elárulja a SORT: a „sör"
            // magas hangrendű (sörnek, sörrel, sörnél), a „sor" mély
            // (sornak, sorral, sornál). Ahol a rag magánhangzója eltér, ott
            // nincs kétség – a -ból/-ből és a -hoz/-höz viszont ékezet nélkül
            // egybeesik, azok maradnak a régi szabálynál.
            "sornak", "sorral", "sornal", "sorai", "soraban", "soranak",
            "borsos", "gombamod", "tejszinu", "vajszinu", "uborkaszezon",
            "lencseveg", "banankoz", "narancsbor", "kenyerkeres", "tortaform",
            "kaveszunet", "teadelutan", "olajfolt", "halaszf", "borvidek",
            "sorhas", "kolbaszujj", "almafa", "kortefa", "diofa",
            "cseresznyefa", "szilvafa", "barackfa", "eperfa", "meggyfa", "fugefa",
            "citromfu",
            // Hétköznapi szavak, amikben egy rövid étel-szótő lakik. Mind
            // valódi eset volt: a „majd" csirkemájat, az „iskolában" kólát, az
            // „uszodában" szódavizet vitt a naplóba – észrevétlenül, mert a
            // felismerés sikeresnek látszott.
            "majd", "iskola", "uszod",
            // Ugyanez a fajta ütközés, végigpróbált magyar szólistából. A bal
            // oldalon mindennapi szó, a jobb oldalon amit eddig naplózott:
            // hall/halál/halom/halaszt → hal, szabad → zab, babona/babér/bábu →
            // bab, majom/május/majális → máj, tejút → tej, rizikó → rizs,
            // alkalmas → alma, bordázat → bor, sorsolás → sör.
            "hall", "halal", "halom", "halaszt", "szabad", "babon", "baber",
            "babu", "szobabicikli", "majom", "majus", "majalis", "tejut",
            "riziko", "sorsol", "alkalm", "borda",
            // A hálózat és a hálószoba nem hal, a csúszás nem túrós csusza.
            "haloz", "haloszob", "csuszas", "csuszik", "csuszo", "csuszd",
            // A „meg" igekötő elé áll a gy-kezdetű igéknek, és a kettő
            // együtt MEGGY-nek olvasódik: megGYŐZ, megGYÓGYUL, megGYÚJT.
            // A „meggyógyult a vállam" épp a rehab-oldal mondata – abból
            // eddig egy adag meggy lett.
            "meggyoz", "meggyogyu", "meggyujt", "meggyanu", "meggyeng",
            "meggyors", "meggyul", "meggyalaz", "meggyarap", "meggyotor",
            // A legINkább közepén a gin, a szövEGRÉSZben az egres.
            "leginkabb", "regina", "virgin", "origin", "login", "engine",
            "margin", "imagin", "szovegresz", "gina",
            // Az ötvenezer szavas magyar gyakorisági listával végigsöpörve.
            // A HAL nem csak étel, hanem ige is: megHALT, HALNI, HALOK – és
            // az igekötős alakot a „meg" levágása után ugyanez fogja meg.
            "halt", "halni", "halok", "halsz", "halunk", "halnak", "halna",
            "halj", "haldok", "halando", "halhatatlan", "halmoz", "halomra",
            "marshall",
            // A BABa és a szoBÁBAn nem bab, a suliBA és a buliBA nem liba.
            "baba", "baby", "szoba", "suli", "buli", "csali", "kisbaba", "sors",
            // A háBORú, a cimBORa és a hátBORzongató nem bor.
            "haboru", "cimbora", "hatborzong", "bordel",
            // Az iDIÓta nem dió, a tolVAJ nem vaj, a CSAJt nem sajt,
            // a kaPITÁny nem pita, a vaDASZ nem vadas hús.
            "idiot", "tolvaj", "csaj", "kapitany", "vadasz",
            // A kereSTEK nem steak, a soRSa nem sör, a gaBRIEl nem brie,
            // a PHOebe nem pho-leves.
            "keres", "sorsa", "sorsom", "sorsod", "sorsunk", "sorsot", "sorsa",
            "gabriel", "phoe", "phon", "phot",
            // A megGYILKolt sem meggy.
            "meggyilkol",
            // A meGINt nem gin, a laKÁSA nem kása, a tönkRETESzi nem rétes,
            // a valaSZÓLOk nem szőlő, a PARIs nem paradicsom.
            "megint", "lakas", "tonkretesz", "valaszol", "paris",
            // A HAL igei alakjai és a hall- kezdetűek.
            "halhat", "haliho", "belehal", "kihal", "elhal", "halovany",
            // A BOR mindenütt: felHÁBORító, szoBOR, BORravaló, bíBOROs,
            // kóBOR, világháBORú.
            "felhaborit", "szobor", "borraval", "biboros", "kobor", "boris",
            // A KÓLA: karCOLás, bonCOLás. (A parkolás már régóta itt van.)
            "karcol", "boncol", "csonkol", "foncsor",
            // A LIBA: nappaLIBAn. A BAB: fürdőSZOBÁBAn – a szoba a
            // belső-tiltón is rajta van, mert összetétel második tagja.
            "nappali",
            // A MÁJ: majmok, majmot. A VAJ: sVÁJCi. A FETA: PRÓFÉTA.
            "majm", "svajc", "profeta",
            // A TEA: team. A SÜTI: vaSÚTi. A RIZS: terroRIZmus.
            "team", "vasut", "terroriz",
            // A SÖR: cSÖRÖg, kiSÖREg. A PITA: megállaPÍTAni.
            "csorog", "kisoreg", "megallapit",
            // A RÉTES: ígéRETES. Az ÖNTET: DÖNTETlen. A POKE: PÓKEmber,
            // PÓKEr, ciPŐKEt.
            "igeret", "dontet", "pokemb", "poker", "cipoke", "pokol",
            // A PARADICSOM „pari" töve: IPARI. A MARHA: marhaSÁG.
            "ipar", "marhasag", "csirkefogo",
            // Nevek és idegen szavak, amikben étel-tő lakik.
            "shakespeare", "truman", "hodgins", "pszichi",
            "arizona", "rizzoli", "nicolas", "jupiter", "adios", "major",
            "ginger", "ginny", "gino", "gingi",
            // A mellKASÁN a kása, a divÍZIÓban a víz, a kettEJük a tej,
            // a gyŰRŰMben a rum, a balLISZTikaiban a liszt.
            "mellkas", "diviz", "kettej", "gyuru", "ballisztik", "statisztik",
            // Az elBUKTAm nem bukta, az állaPÍTAni nem pita, a vilÁGOMBAn
            // nem gomba, a vizELETe nem víz-ital, a halgass nem hal.
            "buktam", "buktal", "buktunk", "buktak", "allapit", "vilagom",
            "vizelet", "halgass", "sztar",
            // A „marhára jó" nem marhahús, a vízesés és a vízvezeték nem ital.
            "marhara", "vizeses", "vizvezetek", "babe",
            // A HAGYJA nem hagyma, a KÉSZEN nem keszeg, a HARCRA nem hal,
            // az OLDALÁN nem sült oldalas, a SZEDEM nem szeder. Ezek az
            // elgépelés-tippet is kizárják: a maszkolt szó nem kap javaslatot.
            // A „hagyt" szándékosan hiányzik: a KIHAGYTAM a tagadás szava, és
            // maszkolva a „kihagytam a tésztát" tésztája visszakerült volna.
            "hagyj", "hagyn", "hagyv", "keszen", "keszek", "kenyes",
            // A „harc" NEM kerülhet ide: a harcsa hal. Csak a ragozott alakok.
            "harcra", "harcba", "harcban", "harcol", "harcos",
            "oldalan", "oldalat", "oldalam", "oldalad", "szedem", "szedek",
            "szeded", "szeker", "keverd",
            // A „köles" szótő miatt: a koleszterin és a kolesz nem étel.
            "kolesz",
            // A legrövidebb szótövek (viz, zab, riz, rum, sor, bor, vaj, tea,
            // mez) hétköznapi szavak belsejében is illeszkednek. Mind valódi
            // eset: a „vizsga" vizet, a „szabály" zabpelyhet, a „frizura"
            // rizst, a „fórum" pálinkát, a „táborban" bort naplózott.
            "vizsg", "vizit", "vizual", "televizi", "vizsla", "szabaly",
            "frizur", "krizis", "brizol", "sorompo", "sorsj", "forum",
            "szerum", "korrump", "rumba", "teatrum", "centrum", "album",
            "vajud", "vajat", "workshop", "network", "garnizon", "operaci",
            "szuper", "temperal", "mezei", "mezogazdas", "mezitlab", "paritas",
            "szaporit", "paripa", "reparal", "terapeut", "stekker", "kaszab",
            "kasza", "purist", "spuriz", "kombajn", "kombinal", "kombi",
            "halando", "borotva", "borz", "abortusz", "tabor", "labor",
            "zabolatlan", "zabal", "kabat", "olimpia",
            // Magyar kereszt- és helynevekkel is végigsöpörtem a felismerőt.
            // A SzaBOlcsban és a szabásban a zab, a Kálmánban az alma, a
            // Tiborban és a bordóban a bor, a Reginában és az originalban a
            // gin, a HódMEZővásárhelyen a méz, a SzázHALombattán a hal. A
            // „bordó" ráadásul az app saját színe.
            "szabo", "szabas", "szabotazs", "zabla", "kalman", "tibor", "bordo",
            "origin", "virgin", "regina", "reggina", "gingiv",
            "meztelen", "mezben", "mezt", "halmoz", "halandzsa", "halovany",
            "hodmezo", "szazhalom", "heviz",
            // A KÉPERNYŐben az eper, a KIFOGÁSban a fogas (egy hal!), a
            // „hónapHOZ"-ban a pho. Mind gyakori szó – a „nincs kifogás"
            // pedig épp egy edzős mondat.
            "kepernyo", "kifogas", "naphoz", "honap", "hetkoznap", "kezisulyzo",
            // A „sör" és a „sor" ékezet nélkül ugyanaz a szó. A sör marad, a
            // leggyakoribb SOR-os szavak viszont nem: „a nap során", „sorban
            // álltam", „sorrend", „sorszám", „sortörés".
            "soran", "sorban", "sorrend", "sorszam", "sorok", "sora", "sorra",
            "sortores", "soros", "sorozat", "sorol", "sorakoz", "sorbol",
            "soronkent", "sorvad",
            // A KÖZÉPértékben az eper, a ruGALMASban az alma, a PRÓBÁban a bab,
            // a rosSZABBban a zab.
            "kozep", "rugalmas", "proba", "rossz", "szabhat", "szakasz",
            // A TARTALMAzban és az ÁRTALMAtlanban is alma van – a
            // „zsírtartalma" ráadásul étel-mondatban is előfordul. A TETEJÉn
            // tej, a TEMPÓként poke, az ÉRTELMEZésben és az ÜTEMEZésben méz,
            // az ÉPÍTÉSben és a TELEPÍTÉSben pite, a testreSZABásban zab.
            "tartalm", "artalm", "tetej", "tempo", "ertelmez", "alapertelmez",
            "utemez", "elemez", "jellemez", "fegyelmez", "epit", "telepit",
            "testreszab", "testtomeg",
            // A zsírégető edzés nem konyhai zsír. (A „zsírszegény" viszont
            // marad: az egy valódi tétel neve – zsírszegény tej.)
            "zsireget", "zsirtartalm",
            // A vízszintes és a víztiszta nem ital; a vízcél az app szava.
            "vizszint", "viztiszta", "vizcel", "vizbevitel", "vizkovet",
            // A fALMAszásban alma, a COMBhajlításban (egy lábgép!) csirkecomb.
            "falmasz", "combhajlit", "combnyujt", "combfeszit", "combtavolit",
            "combkozelit",
            // Az OLVADásban ott a vadas (hús). Az „olvasztott sajt" nem
            // érintett: az más szó (olvaszt, nem olvad).
            "olvad",
            // Az ELSŐre szóban a sör, a FORMÁjában a máj, a MASZKOLásban a
            // kóla. A „formáj" szándékosan ilyen szűk: a puszta „forma"
            // kiütné a „quattro formaggi" pizzát is – a saját adatminőség-
            // teszt fogta meg, két perccel a bővebb változat után.
            "elso", "formaj", "maszkol",
            // A hatezer szavas söprés maradéka: a buBORékban és a domBORítsban
            // a bor, a BUKTAtóban a bukta, a duruMBÚZÁban a dürüm, az
            // ellenŐRIZben a rizs, a FOGALMazban az alma, a főKÉPERNYŐben az
            // eper, a felraKÁSÁban a (zab)kása.
            "buborek", "dombor", "buktat", "durumbuza", "oriz", "ellenoriz",
            "fogalm", "felrak",
            // A söprés második átnézése: a SZERETek retket, a STÁDIÓban diót, a
            // SZOKÁSaimban (zab)kását, a LEVESSZÜKben levest, a VÍZILABDÁban
            // vizet naplózott. A „szeretek futni" a leggyakoribb mondatkezdés,
            // amit egy edzésappba be lehet írni.
            "szeret", "stadi", "szokas", "levesz", "levessz", "vizilabda",
            // A birtokos comb a SAJÁT láb, nem csirkecomb: „húzódik a combom".
            "combom", "combod", "combja",
            // A BIRKÓzás sport, nem birkahús – az új bárány-tétel miatt kell.
            "birkoz",
    };

    /** Összetétel BÁRMELYIK tagjaként maszkolandó szavak. */
    // A szó BELSEJÉBEN álló csapdák: a beszédbuborékban a bor, a
    // főképernyőn az eper, a hüvelykujjszabályban a zab – és a
    // pihenőNAPOKÉhoz szóban a poke bowl, a kétOLDALASokat szóban a sült
    // oldalas. Mindet a szósöprés találta: a bejegyzés létrejött volna,
    // csak épp nem arról, amit az ember írt.
    private static final String[] INSIDE_BAD = {"buborek", "kepernyo", "szabaly",
            "poke", "oldalasok", "oldalasat",
            // A SZOBA összetétel második tagjaként is gyakori (fürdőszoba,
            // hálószoba, nappali szoba), és mindegyikben ott a BAB. A HALL
            // ugyanígy: ideHALLgass, viszHALL.
            "szoba", "hall",
            // A HÁBORÚ (polgárháború, világháború) a BORt, a SERTÉS a
            // karajt, a SZŐLŐ pedig a bokSZOLÓt és a gyáSZOLÓt hozta be.
            // Egyik tő sem áll összetétel második tagjaként az adatbázisban,
            // tehát a szó belsejében nyugodtan kitakarhatók.
            "haboru", "sertes", "szolo", "kettej"};

    /**
     * Tövek, amik CSAK a szó elején jelentenek ételt.
     *
     * Három-négy betűs, idegen eredetű szavak: a gin, a pho, a chia, a kesu,
     * a stek – és ide tartozik a ZAB is. Magyar összetételben egyik sem áll
     * hátul (a kesudió a kesuval KEZDŐDIK), a szó közepén viszont sorra
     * beleakadnak: meGINt, épphoGY, pszicHIAter, elKESUnk, teSTEK, hosszABB.
     *
     * FONTOS: ez nem maszk. A szót magát nem takarjuk ki, csak EZ a tő nem
     * illeszkedhet a belsejében – különben a „pizzából" (piZZABól) elveszett
     * volna a pizza, a „sziruphoz" (sziruPHOz) a szirup, a „gnocchival" a
     * gnocchi. A ragozás rendre gyárt ilyen véletlen betűsorokat.
     */
    private static final String[] START_ONLY = {"gin", "pho", "chia", "kesu", "stek", "zab",
            // A „rozé" a SÖRÖZÉS közepén is ott van – abból eddig egy pohár
            // bor lett a három korsó sör helyett.
            "roze"};

    /**
     * Kivételek a belső tiltólistához: az „eszpresszóba" közepén ott a
     * „szoba", pedig a kávéról szól. A lista rövid, mert a tiltólista is az.
     */
    private static final String[] INSIDE_BAD_OK = {"presszo", "presso"};

    private static boolean insideOk(String tok) {
        for (String e : INSIDE_BAD_OK) if (tok.contains(e)) return true;
        return false;
    }

    /** Szókezdethez kötött tő-e. */
    private static boolean startOnly(String ns) {
        for (String s : START_ONLY) if (s.equals(ns)) return true;
        return false;
    }

    /**
     * Szó ELEJÉN álló csapdák, amik ételnek látszanak: a „zsírmentes" jelző a
     * ZSÍR tövével kezdődik, és eddig száz gramm olajat írt a naplóba a
     * „zsírmentes túró" mellé.
     *
     * A „zsírszegény" szándékosan NINCS itt: annak van saját tétele
     * („Zsírszegény tej"), és a hosszabb tő úgyis elviszi a rövidebb elől.
     */
    private static final String[] START_BAD_EXTRA = {"zsirmentes", "zsirtalan"};

    /**
     * Az „alma" a szó BELSEJÉBEN szinte sosem alma.
     *
     * A magyar „-alom" végű főnevek ragozva mind ALMÁ-vá válnak: fájdALMAt,
     * birodALMAt, hatALMAt, jutALMAt, sokadALMAt – és ide tartozik a hALMAz
     * is. Ezekből a szókezdet-vizsgálat egyet sem fogott meg, mert a tő a
     * szó közepén ül: minden ilyen mondathoz járt egy fantom alma, nyolcvan
     * kalória. Külön-külön felsorolni reménytelen (a szóképző osztály
     * végtelen), a szabály viszont egyszerű.
     *
     * A kivételek a valódi összetett gyümölcsnevek – ott az alma tényleg
     * alma, csak nem a szó elején áll.
     */
    private static final String[] ALMA_OK = {"granatalma", "birsalma", "vadalma",
            "aranyalma", "csipkebogyoalma"};

    /**
     * Az ÁLOM ragozott alakjai ékezet nélkül ALMÁnak látszanak.
     *
     * Az „álmaim", az „álmod" és az „álmos" a normalizálás után „almaim",
     * „almod", „almos" – mind a szó ELEJÉN hordozza az almát, ezért az
     * összetétel-szabály sem fogta meg őket. Az alma valódi ragjai (almát,
     * almák, almával, almás) egyikkel sem esnek egybe.
     */
    private static final String[] ALOM = {"almai", "almaim", "almaid", "almaink",
            "almatok", "almuk", "almunk", "almod", "almom", "almok", "almot",
            "almos", "almatlan", "almodoz", "almodik", "almodt"};

    /**
     * Valódi májas összetételek – ezekben a „mája" tényleg máj.
     *
     * Az „almája" is itt van: ott a betűsor az ALMA és a birtokos „-ja"
     * találkozásából jön, nem a májból.
     */
    private static final String[] MAJ_OK = {"csirkemaj", "libamaj", "kacsamaj",
            "sertesmaj", "borjumaj", "majkrem", "kenomajas", "majgomboc",
            // Ezekben a „mája" a SAJÁT szavuk birtokos alakja: almája,
            // hagymája, tormája, hurmája (datolyaszilva).
            "almaja", "hagymaja", "tormaja", "hurmaja", "burgonyaja"};

    /**
     * Maszkolandó-e a szó – igekötővel együtt is.
     *
     * A maszk a szó ELEJÉT nézi, különben a ragozott igazi ételek („sajtos",
     * „vajas") is elvesznének. Csakhogy a magyar igekötő elé áll a tőnek: a
     * „felsorolás" és a „besorolás" ugyanaz a szó, mint a „sorolás", a
     * „beolvasás" mint az „olvasás", az „elhallgat" mint a „hallgat" – ezeket
     * a puszta prefix-egyezés sorra elszalasztotta, és sört, halat, ásást
     * írtak a naplóba.
     */
    private static boolean masked(String tok) {
        if (startsWithBad(tok)) return true;
        // A ZAB a szó belsejében sosem zab: igaZABb, hosszABB, háZÁBAn,
        // szaBAd, szaBÁLy. Zabos összetétel mindig a zabbal KEZDŐDIK
        // (zabpehely, zabkása, zabtej), tehát kivétel sem kell.
        // A francia főváros nem paradicsom: a „Párizsban" közepén a „pari".
        // A párizsi FELVÁGOTT viszont valódi étel – az marad.
        if (tok.startsWith("parizs") && !tok.startsWith("parizsi")) return true;
        // A birtokos „-ja" a -ma végű főnevek után MÁJ-at ad: probléMÁJA,
        // téMÁJA, forMÁJA. A valódi májas összetételek viszont maradnak.
        if (tok.indexOf("maja") > 0) {
            boolean real = false;
            for (String ok : MAJ_OK) if (tok.contains(ok)) real = true;
            if (!real) return true;
        }
        if (tok.indexOf("alma") > 0) {
            boolean real = false;
            for (String ok : ALMA_OK) if (tok.contains(ok)) real = true;
            if (!real) return true;
        }
        // Összetétel MÁSODIK tagjaként is előfordulnak: a beszédbuborékban és
        // a szappanbuborékban a bor, a főképernyőn és a kezdőképernyőn az
        // eper, a hüvelykujjszabályban a zab. Az előtagot nem lehet felsorolni,
        // a szót magát viszont igen – és ragozva is ott van („főképernyőN"),
        // ezért nem a szó végét, hanem a benne állást nézzük.
        for (String e : INSIDE_BAD)
            if (tok.indexOf(e) > 0 && !insideOk(tok)) return true;
        for (String v : VERB_PREFIX)
            if (tok.length() > v.length() + 2 && tok.startsWith(v)
                    && startsWithBad(tok.substring(v.length()))) return true;
        return false;
    }

    private static boolean startsWithBad(String tok) {
        for (String bad : ALOM) if (tok.startsWith(bad)) return true;
        for (String bad : NOT_FOOD) if (tok.startsWith(bad)) return true;
        for (String bad : START_BAD_EXTRA) if (tok.startsWith(bad)) return true;
        return false;
    }

    /** Magyar igekötők: ami utánuk áll, az a szó töve (fel-sorolás). */
    static final String[] VERB_PREFIX = {"meg", "el", "fel", "be", "ki", "le",
            "at", "ra", "ossze", "szet", "vissza", "vegig", "oda", "korbe", "elo",
            // A felsőfok is a tő elé áll: legHALVÁNYabb, legROSSZabb; az
            // „újra" ugyanígy: ÚJRAépítés, ÚJRAértelmez.
            "leg", "ujra"};

    /** Az étel-felismerés elől elrejtett szavak kimaszkolása. */
    static String mask(String q) {
        StringBuilder sb = new StringBuilder(q);
        int i = 0;
        while (i < sb.length()) {
            if (!Character.isLetter(sb.charAt(i))) { i++; continue; }
            int j = i;
            while (j < sb.length() && Character.isLetter(sb.charAt(j))) j++;
            String tok = sb.substring(i, j);
            boolean hide = masked(tok);
            // A „zsírszegény" a zsír TARTALMÁRÓL szól, nem hozzávalóról: a
            // „zsírszegény túró" mellé eddig száz gramm olaj került, kilenc-
            // száz kalória – pont az ellenkezője annak, amit az ember írt.
            // A saját tételét („zsírszegény tej") viszont nem vehetjük el,
            // ezért csak akkor takarjuk ki, ha nem az áll utána.
            if (!hide && tok.startsWith("zsirszegeny"))
                hide = !sb.substring(j).trim().startsWith("tej");
            if (hide) for (int k = i; k < j; k++) sb.setCharAt(k, ' ');
            i = j;
        }
        return sb.toString();
    }

    /** Ékezet-mentesítés + kisbetű, a rugalmas kereséshez. */
    static String norm(String s) {
        if (s == null) return "";
        s = s.toLowerCase(new Locale("hu"));
        // A mondatvégi írásjel és a dupla szóköz nem jelentés – a telefonon
        // viszont mindkettő gyakori, és a szabályok szóközre illesztenek.
        // Enélkül egy felkiáltójel egész sorozatot vitt el a naplóból.
        s = s.replaceAll("[!?…]", " ").replaceAll("\\s+", " ").trim();
        // Az ä nem magyar ékezet, de márkanevekben előfordul (Jägermeister).
        return s.replace('á','a').replace('é','e').replace('í','i').replace('ó','o')
                .replace('ö','o').replace('ő','o').replace('ú','u').replace('ü','u')
                .replace('ű','u').replace('ä','a');
    }

    /**
     * Ugyanaz, mint a norm(), csak az ékezetek maradnak.
     *
     * Minden lépése karakter-helyes (egy betűből egy betű lesz), ezért a két
     * szöveg INDEXEI megegyeznek: a norm()-ban talált pozíción itt is ugyanaz
     * a szó áll, csak ékezettel.
     */
    static String normAcc(String s) {
        if (s == null) return "";
        s = s.toLowerCase(new Locale("hu"));
        return s.replaceAll("[!?…]", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Ékezettel megkülönböztetett szótövek – bal oldalt a tő, jobbra az IGAZI
     * írásmód.
     *
     * Ékezet nélkül a SÖR és a SOR ugyanaz a szó, és a magyar bőven gyárt
     * olyan összetételeket, amiknek a vége „-sor": névsor, címsor,
     * gyakorlatsor, munkasorozat, ábécésorrend. Ugyanígy volt a MÉZ az
     * adatmezőben, a KÁVÉ a falkavezérben, a KÓLA a csonkolásban, a TEJ az
     * estéjéről-ben, a BOR a bőrrel-ben, a FOGAS (egy hal) minden „fogás"-ban.
     * Tiltólista ezt nem oldja meg: az összetételek osztálya végtelen.
     *
     * Két eset van, és magától adódik, melyik:
     *  - ha az igazi alak ÉKEZETES („sör", „méz", „kávé", „kóla"), akkor csak
     *    szó BELSEJÉBEN kérjük számon – szó elején marad a régi viselkedés,
     *    hogy az ékezet nélkül gépelőktől ne vegyük el a sörüket;
     *  - ha az igazi alak ÉKEZET NÉLKÜLI („bor", „tej", „fogas"), akkor
     *    mindenhol kérjük, hiszen aki ékezet nélkül gépel, ugyanezt írja –
     *    így viszont a „bőr", a „téj" és a „fogás" kiesik.
     */
    private static final String[][] ACCENTED_STEM = {
            {"sor", "sör"}, {"mez", "méz"}, {"kave", "kávé"}, {"kola", "kóla"},
            {"bor", "bor"}, {"tej", "tej"}, {"fogas", "fogas"},
            // A HAL ékezet nélküli szó, a HÁLA és a HÁLÁS viszont á-val írt –
            // ezért a hal tövét mindenhol ékezet nélkül kérjük. Enélkül a
            // „hálás vagyok" egy adag halat írt a naplóba.
            {"hal", "hal"},
    };

    /**
     * Tövek, amelyek ékezet nélkül MÉRTÉKSZÓVÁ válnak.
     *
     * Rövid a lista, és az is marad: a magyarban a „sor" az egyetlen olyan
     * szó a tövek közt, ami egy másik étel elé állva a MENNYISÉGET mondja
     * meg („egy sor csoki"). A „mez", a „kave" és a „kola" soha – ezek elé
     * senki nem tesz ételt mértékszóként.
     */
    private static final String[] MEASURE_STEM = {"sor"};

    /**
     * Ékezet nélkül írt, MÉRTÉKSZÓKÉNT álló tövek kidobása.
     *
     * A minta mindig ugyanaz: „egy SOR csoki”, ahol a szó a mennyiséget
     * mondja meg, nem az ételt. Csak akkor lép működésbe, ha a tövet ékezet
     * nélkül írták (tehát nem „sör”), ÉS közvetlenül utána egy másik felismert
     * étel kezdődik.
     *
     * A szűkítés fontos: enélkül minden ékezettel megkülönböztetett tő így
     * viselkedett, és a „kave tejjel" beírásából eltűnt a kávé – pedig ott a
     * kávé az ital, a tej a hozzávaló.
     */
    private static List<Match> dropMeasureWords(String q, String qAcc, List<Match> in) {
        List<Match> out = new ArrayList<>();
        for (Match m : in) {
            boolean drop = false;
            if (m.pos + m.len <= q.length() && isMeasureStem(q.substring(m.pos, m.pos + m.len))) {
                String acc = accentedOf(q.substring(m.pos, m.pos + m.len));
                if (acc != null && !acc.equals(q.substring(m.pos, m.pos + m.len))
                        && !qAcc.regionMatches(m.pos, acc, 0, acc.length()))
                    for (Match o : in) {
                        int gapA = m.pos + m.len, gapB = o.pos;
                        // A köztük álló rész CSAK szóköz lehet: az „egy sor
                        // csoki" mértékszó, a „pizza, sör, fagyi" viszont
                        // felsorolás – ott a vessző új tételt nyit, és a sör
                        // eddig némán kimaradt belőle.
                        if (o != m && gapB > gapA && gapB <= gapA + 2
                                && q.substring(gapA, gapB).trim().isEmpty())
                            drop = true;
                    }
                    // A „TRX sor 3x12" sora SOROZAT, nem sör: a sorozatjelölés
                    // követi. Csak az ékezet nélkül írt alaknál – aki „sört"
                    // ír, annak a sörét nem vesszük el.
                    if (!drop && q.substring(Math.min(m.pos + m.len, q.length()))
                            .matches("^\\s*\\d{1,2}\\s?[x×]\\s?\\d{1,3}.*")) drop = true;
                if (drop) {
                    // „egy sor csoki és egy sör”: a mértékszó után máshol
                    // OTT LEHET a valódi ital – ilyenkor nem eldobjuk a
                    // találatot, hanem odébb tesszük.
                    String stem = q.substring(m.pos, m.pos + m.len);
                    for (int p2 = q.indexOf(stem, m.pos + 1); p2 >= 0;
                         p2 = q.indexOf(stem, p2 + 1))
                        if (qAcc.regionMatches(p2, acc, 0, acc.length())) {
                            out.add(new Match(m.food, p2, m.len));
                            break;
                        }
                }
            }
            if (!drop) out.add(m);
        }
        return out;
    }

    /**
     * Fajta + gyűjtőnév: a második szó ugyanaz az étel, nem egy másik.
     *
     * A magyar a fajtát a gyűjtőnév elé teszi: „feta sajt", „mozzarella
     * sajt". Az étel viszont EGY – a naplóba eddig két tétel ment be, egy
     * adag feta ÉS egy adag trappista, vagyis a felismerés maga hízlalta a
     * napot úgy nyolcvan kalóriával.
     *
     * A táblázat szándékosan szűk. „Sonka sajt" NEM tartozik ide: ott tényleg
     * két étel van a tányéron. Csak az kerül be, ami a gyűjtőnév alá esik –
     * a feta sajt, a mozzarella sajt.
     *
     * Sorok: [gyűjtő-tő, majd a fajták étel-nevei].
     */
    private static final String[][] KIND_THEN_GENERIC = {
            {"sajt", "Mozzarella", "Parmezán", "Camembert / brie", "Feta",
                    "Mascarpone", "Ricotta", "Cottage cheese"},
    };

    /** A fajta után álló gyűjtőnév-találat kidobása („feta sajt”). */
    private static List<Match> dropGenericAfterKind(String q, List<Match> in) {
        List<Match> out = new ArrayList<>();
        for (Match m : in) {
            String stem = q.substring(m.pos, m.pos + m.len);
            boolean drop = false;
            for (String[] row : KIND_THEN_GENERIC) {
                if (!row[0].equals(stem)) continue;
                for (Match o : in) {
                    if (o == m || o.pos + o.len > m.pos) continue;
                    // Legfeljebb egy szóköz és egy odatapadt rag fér közé:
                    // „feta sajt", „fetás sajt".
                    if (m.pos - (o.pos + o.len) > 3) continue;
                    for (int i = 1; i < row.length; i++)
                        if (row[i].equals(o.food.name)) drop = true;
                }
            }
            if (!drop) out.add(m);
        }
        return out;
    }

    /** Mértékszóvá váló tő-e (ékezet nélkül írva). */
    private static boolean isMeasureStem(String ns) {
        for (String s : MEASURE_STEM) if (s.equals(ns)) return true;
        return false;
    }

    /** A szótő igazi írásmódja, ha ékezettel megkülönböztetett – különben null. */
    private static String accentedOf(String ns) {
        for (String[] a : ACCENTED_STEM) if (a[0].equals(ns)) return a[1];
        return null;
    }

    /**
     * A szótő első ELFOGADHATÓ előfordulása, vagy -1.
     *
     * @param q    ékezet nélküli, maszkolt szöveg
     * @param qAcc ugyanaz, de „ö/ő"-vel – ugyanazokkal az indexekkel
     */
    static int stemIndex(String q, String qAcc, String ns) {
        // Szókezdethez kötött tövek: a szó BELSEJÉBEN nem illeszkednek.
        if (startOnly(ns)) {
            for (int p = q.indexOf(ns); p >= 0; p = q.indexOf(ns, p + 1))
                if (p == 0 || !Character.isLetter(q.charAt(p - 1))) return p;
            return -1;
        }
        String acc = accentedOf(ns);
        if (acc == null) return q.indexOf(ns);
        // Ékezetes igazi alaknál a szó eleje kivétel (ott az ékezet nélkül
        // gépelő is a valódi szót írja); ékezet nélkülinél nincs kivétel.
        boolean startFree = !acc.equals(ns);
        for (int p = q.indexOf(ns); p >= 0; p = q.indexOf(ns, p + 1)) {
            if (startFree && (p == 0 || !Character.isLetter(q.charAt(p - 1)))) return p;
            if (p + acc.length() <= qAcc.length()
                    && qAcc.regionMatches(p, acc, 0, acc.length())) return p;
        }
        return -1;
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
            for (String ns : f.nstems)
                if (q.equals(ns)) return f;
        // 2) a lekérdezés tartalmazza a szótövet (pl. "csirkemellbol" ⊃ "csirkemell")
        Food best = null; int bestLen = 0;
        for (Food f : list)
            for (String ns : f.nstems)
                if (ns.length() > bestLen && q.contains(ns)) { best = f; bestLen = ns.length(); }
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
                for (String ns : f.nstems) {
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
     * A legközelebbi ismert étel elgépelés esetén – „csirkemel" → Csirkemell.
     *
     * A telefon billentyűzetén az elütés a leggyakoribb hiba, és eddig a
     * „joghrut" ugyanazt kapta, mint egy tényleg ismeretlen étel: „ezt még
     * nem ismerem". Pedig ismerjük, csak egy betűvel odébb.
     *
     * Szigorú, mert a rossz tipp bosszantóbb, mint a semmi: szavanként
     * nézzük, legalább négy betűtől, és EGY hiba fér bele – hosszú
     * szótőnél (kilenc betűtől) kettő. A felcserélt betű is egy hibának
     * számít, mert a telefonon az a leggyakoribb elütés: a „joghrut" egy
     * ujjmozdulat a joghurttól. Enélkül a „valami" szalámi lett volna, az
     * „asztal" pedig aszalt gyümölcs.
     *
     * @return a legjobb tipp, vagy null, ha nincs elég közeli
     */
    public static Food closest(List<Food> list, String query) {
        if (list == null || query == null) return null;
        String q = mask(norm(query)).trim();
        if (q.isEmpty()) return null;
        Food best = null;
        int bestDist = Integer.MAX_VALUE, bestLen = 0;
        for (String tok : q.split("[^a-z0-9]+")) {
            if (tok.length() < 6) continue;
            for (Food f : list)
                for (String ns : f.nstems) {
                    if (ns.length() < 6 || ns.indexOf(' ') >= 0) continue;
                    // A szó ELEJE nem szokott elgépelődni, viszont erős jel.
                    // Enélkül a „neki" gyorséttermi menü lett, a „saját" sajt,
                    // a „tiszta" tészta. A rossz tipp bosszantóbb, mint a
                    // semmi, ezért itt a pontosság fontosabb, mint a lefedés.
                    if (!ns.regionMatches(0, tok, 0, 3)) continue;
                    int max = ns.length() >= 9 ? 2 : 1;
                    if (Math.abs(ns.length() - tok.length()) > max) continue;
                    int d = editDistance(tok, ns, max);
                    if (d <= 0 || d > max) continue;
                    // Azonos távolságnál a hosszabb szótő a jobb tipp: több
                    // betű egyezik, tehát kevesebb a véletlen.
                    if (d < bestDist || (d == bestDist && ns.length() > bestLen)) {
                        best = f; bestDist = d; bestLen = ns.length();
                    }
                }
        }
        return best;
    }

    /**
     * Szerkesztési távolság a FELCSERÉLT betűt is egy hibának számolva.
     *
     * A sima Levenshtein a cserét két műveletnek látja, pedig a telefonon
     * pont az a leggyakoribb elütés: a „joghrut" egyetlen ujjmozdulat a
     * joghurttól. Ezért Damerau-változat (optimális illesztés).
     *
     * A {@code max} fölötti feladás nem optimalizálás, hanem a viselkedés
     * része: a beviteli mező minden leütésre újrakérdez, és háromszázötven
     * étel összes szótövén végigmenni betűnként nem lehet drága.
     */
    static int editDistance(String a, String b, int max) {
        int n = a.length(), m = b.length();
        if (Math.abs(n - m) > max) return max + 1;
        int[] prev2 = new int[m + 1], prev = new int[m + 1], cur = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            int rowMin = cur[0];
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int v = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1))
                    v = Math.min(v, prev2[j - 2] + 1);
                cur[j] = v;
                rowMin = Math.min(rowMin, v);
            }
            if (rowMin > max) return max + 1;
            int[] t = prev2; prev2 = prev; prev = cur; cur = t;
        }
        return prev[m];
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
            {"Koktélparadicsom", "20"},
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
            // Amit szemenként mondunk: „tíz szem mandula", „öt szem szőlő".
            // Enélkül a szám elveszett, és a szokásos adag ment be – tíz
            // mandula helyett egy egész marék.
            {"Kolbász", "80"},
            {"Mandula", "1"}, {"Dió", "5"}, {"Mogyoró", "1"}, {"Kesudió", "2"},
            {"Pisztácia", "1"}, {"Szőlő", "5"}, {"Cseresznye / meggy", "8"},
            {"Eper", "12"}, {"Olajbogyó / olívabogyó", "4"}, {"Málna", "4"},
            {"Áfonya", "2"},
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
                        // A birtokos alak is fél: „a fele adag rizs", „a
                        // pizza fele". A puszta „fel" tő ezt nem fogta, mert
                        // betű követi.
                        {"fele", "0.5"},
                        // A többi birtokos tört is: „a pizza negyede", „a
                        // szendvics harmada". Eddig a NEGYEDE-ből egész pizza
                        // lett – négyszerese annak, amit megevett.
                        {"negyede", "0.25"}, {"harmada", "0.34"},
                        {"harmad", "0.34"}, {"ketharmad", "0.67"},
                        {"ketharmada", "0.67"}, {"haromnegyede", "0.75"},
                        // A „dupla adag" és a „tripla eszpresszó" is szám: a
                        // szorzó nélkül a tipikus adag ment be, vagyis fele
                        // vagy harmada annak, amit az ember megevett.
                        {"dupla", "2"}, {"tripla", "3"},
                }));
        String[][] tens = {{"tizen", "10"}, {"huszon", "20"}, {"harminc", "30"},
                {"negyven", "40"}, {"otven", "50"}, {"hatvan", "60"},
                {"hetven", "70"}, {"nyolcvan", "80"}, {"kilencven", "90"}};
        String[][] units = {{"egy", "1"}, {"ketto", "2"}, {"ket", "2"}, {"harom", "3"},
                {"negy", "4"}, {"ot", "5"}, {"hat", "6"}, {"het", "7"},
                {"nyolc", "8"}, {"kilenc", "9"}};
        java.util.List<String[]> belowHundred = new java.util.ArrayList<>();
        for (String[] t : tens) {
            // A „tizen"/„huszon" csak összetételben szám, a többi magában is.
            if (!t[0].equals("tizen") && !t[0].equals("huszon"))
                belowHundred.add(new String[]{t[0], t[1]});
            for (String[] u : units)
                belowHundred.add(new String[]{t[0] + u[0],
                        String.valueOf(Integer.parseInt(t[1]) + Integer.parseInt(u[1]))});
        }
        // A „tíz" és a „húsz" magában az alaplistában van; a százas
        // összetételekhez („százhúsz") itt is kell.
        belowHundred.add(new String[]{"tiz", "10"});
        belowHundred.add(new String[]{"husz", "20"});
        out.addAll(belowHundred);
        // A százas adagok („száz gramm rizs", „százötven gramm csirkemell") a
        // konyhában a leggyakoribbak – eddig pont ezek maradtak ki, és a
        // tipikus adag ment helyettük a naplóba.
        String[][] hundreds = {{"szaz", "100"}, {"ketszaz", "200"}, {"haromszaz", "300"},
                {"negyszaz", "400"}, {"otszaz", "500"}};
        for (String[] h : hundreds) {
            out.add(new String[]{h[0], h[1]});
            for (String[] u : units)
                out.add(new String[]{h[0] + u[0],
                        String.valueOf(Integer.parseInt(h[1]) + Integer.parseInt(u[1]))});
            for (String[] b : belowHundred)
                out.add(new String[]{h[0] + b[0],
                        String.valueOf(Integer.parseInt(h[1]) + Integer.parseInt(b[1]))});
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
             "kupica", "stampedli", "korty", "kortyot", "kancso", "kancsó",
             // A „szem" a magyar konyhában darabszó: „tíz szem mandula",
             // „öt szem szőlő". Enélkül a szám elveszett, és a szokásos adag
             // ment be – tíz mandula helyett egy egész marék.
             "szem", "szemet", "szemnyi",
             // A kenyér KARÉJ, a virsli és a kolbász SZÁL: mindkettő darabszó.
             // A „2 karéj kenyér" eddig ugyanannyi volt, mint az egy karéj.
             "karej", "karéj", "szal", "szál",
             // A csokoládé SORban törik: a „két sor csoki" két sor, nem egy.
             "sor",
             // A kulacs az edzőterem palackja.
             "kulacs", "kulaccsal"};

    /**
     * Folyadék-mérőszavak millilitere a víznél. A „pohár" a tipikus adag
     * (2,5 dl), az üveg, a kancsó és a korty viszont nem – és ezekből lesz a
     * napi vízcél, ezért itt a tévedés a haladássávon is látszik.
     *
     * Csak a vízre él: az „egy üveg sör" fél liter, de a sörnél az adag már
     * eleve ennyi, az „egy korty bor" pedig nem életszerű bejegyzés.
     */
    static final String[][] WATER_ML = {
            {"korty", "40"}, {"kortyot", "40"},
            {"uveg", "500"}, {"üveg", "500"},
            // A kulacs az edzőtermi alapfelszerelés – fél literrel számolunk.
            {"kulacs", "500"}, {"kulaccsal", "500"},
            {"kancso", "1000"}, {"kancsó", "1000"},
    };

    private static int waterMl(Food f, String word) {
        if (f == null || !f.name.startsWith("Víz")) return 0;
        for (String[] w : WATER_ML) if (w[0].equals(word)) return Integer.parseInt(w[1]);
        return 0;
    }

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
     * Méret-jelzők: nem mérőszavak, csak közéállnak („2 nagy alma").
     *
     * Az „egész" és a „teljes" is ide tartozik: az „egy EGÉSZ tábla csoki"
     * egy tábla, nem egy adag. Enélkül a jelző elszakította a számot a
     * mérőszótól, és a tipikus adag (25 g) ment be a száz gramm helyett –
     * pont annál a mondatnál, amit akkor ír le az ember, amikor sokat evett.
     */
    private static final String[] SIZE_WORDS =
            {"nagy", "kis", "kicsi", "kozepes", "szep", "hatalmas", "apro",
             "egesz", "teljes"};

    /**
     * Ennél kisebb darabszámmal nem számolunk. A negyed pizza valódi mennyiség
     * – korábban a fél volt a határ, így a „negyed" némán kiesett, és az egész
     * adag ment a naplóba: négyszer annyi.
     */
    private static final double MIN_COUNT = 0.25;

    /**
     * Ennél több ADAG már nem egy étkezés.
     *
     * A darabszám mehet húszig („húsz szem mandula"), az adag viszont egész
     * fogás: kettő-három életszerű, tizennyolc nem. A fuzz találta meg, ahol
     * egy százalékjel száma csúszott az adag helyére.
     */
    private static final double MAX_PORTIONS = 6;

    /** A szám után – szóközöket átugorva – százalékjel áll? */
    private static boolean percentAfter(String q, int numEnd) {
        int i = numEnd;
        while (i < q.length() && q.charAt(i) == ' ') i++;
        return i < q.length() && q.charAt(i) == '%';
    }

    /**
     * A szám közvetlenül az étel előtt áll-e – legfeljebb egy számlálószóval
     * közte? A visszatérés a közbeékelt szó ("" ha nincs), vagy null, ha ott
     * valami más áll – akkor a szám nem ehhez az ételhez tartozik.
     */
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
     * „Két és fél szelet kenyér": az egész és a tört EGY szám.
     *
     * A mértékegységes ág ezt már értette („két és fél deci"), a darabszámos
     * nem: a kettes és a fél két külön számként került a listába, és a fél
     * ért oda előbb – a két és fél szeletből fél szelet lett. A bejegyzés
     * létrejött, csak ötödannyival.
     */
    private static void mergeAndHalf(String q, List<Integer> pos, List<Double> val,
                                     List<Integer> len) {
        for (int a = 0; a < pos.size(); a++) {
            int end = pos.get(a) + len.get(a);
            int e1 = end;
            while (e1 < q.length() && q.charAt(e1) == ' ') e1++;
            if (!q.startsWith("es", e1)) continue;
            if (e1 + 2 < q.length() && Character.isLetter(q.charAt(e1 + 2))) continue;
            int e2 = e1 + 2;
            while (e2 < q.length() && q.charAt(e2) == ' ') e2++;
            for (int b = 0; b < pos.size(); b++) {
                if (b == a || pos.get(b) != e2 || val.get(b) >= 1) continue;
                val.set(a, val.get(a) + val.get(b));
                len.set(a, pos.get(b) + len.get(b) - pos.get(a));
                pos.remove(b); val.remove(b); len.remove(b);
                if (b < a) a--;
                break;
            }
        }
    }

    /**
     * „2-3 szelet kenyér": a tartomány közepe a becslés.
     *
     * Eddig a nagyobbik nyert – az állt közelebb az ételhez –, vagyis a
     * bizonytalanul megadott mennyiség RENDSZERESEN felfelé csúszott.
     * A középérték elfogulatlan: két és fél szelet.
     *
     * Ez a darabszámos pár („2-3 alma"); a mértékegységes a következő
     * metódusban, mert ott a két szám külön listába kerül.
     */
    private static void averageRanges(String q, List<Integer> pos, List<Double> val,
                                      List<Integer> len) {
        for (int i = 0; i < pos.size(); i++) {
            int end = pos.get(i) + len.get(i);
            if (end >= q.length() || q.charAt(end) != '-') continue;
            for (int j = 0; j < pos.size(); j++) {
                if (j == i || pos.get(j) != end + 1) continue;
                if (!isRange(val.get(i), val.get(j))) break;
                val.set(j, (val.get(i) + val.get(j)) / 2);
                len.set(j, pos.get(j) + len.get(j) - pos.get(i));
                pos.set(j, pos.get(i));
                pos.remove(i); val.remove(i); len.remove(i);
                i--;
                break;
            }
        }
    }

    /**
     * Tartomány-e a két szám? A felső legfeljebb a háromszorosa az alsónak.
     *
     * A „2-3" és a „100-150" tartomány, a „2-30" viszont inkább két külön
     * adat – ott az átlagolás találgatás lenne.
     */
    private static boolean isRange(double lo, double hi) {
        return lo > 0 && hi >= lo && hi <= lo * 3;
    }

    /**
     * Ugyanez, ha a tartomány FELSŐ tagja visel mértékegységet: „3-4 dkg
     * sajt", „100-150 g rizs".
     *
     * Az alsó tag mértékegység nélkül a darabszám-listába kerül, a felső a
     * mennyiség-listába, már grammra váltva – ezért az arányt a nyers
     * számokból számoljuk, és a kész grammértéket igazítjuk hozzá.
     */
    private static void averageUnitRanges(String q, List<Integer> bPos, List<Double> bVal,
                                          List<Integer> bLen, List<Integer> uPos,
                                          List<Double> uVal) {
        for (int i = bPos.size() - 1; i >= 0; i--) {
            int end = bPos.get(i) + bLen.get(i);
            if (end >= q.length() || q.charAt(end) != '-') continue;
            for (int j = 0; j < uPos.size(); j++) {
                if (uPos.get(j) != end + 1) continue;
                double raw = leadingNumber(q, uPos.get(j));
                if (!isRange(bVal.get(i), raw)) break;
                uVal.set(j, uVal.get(j) * (bVal.get(i) + raw) / (2 * raw));
                bPos.remove(i); bVal.remove(i); bLen.remove(i);
                break;
            }
        }
    }

    /** A megadott helyen kezdődő szám (tizedesvesszővel is), vagy 0. */
    private static double leadingNumber(String q, int at) {
        int i = at;
        while (i < q.length() && Character.isDigit(q.charAt(i))) i++;
        if (i == at) return 0;
        if (i + 1 < q.length() && (q.charAt(i) == ',' || q.charAt(i) == '.')
                && Character.isDigit(q.charAt(i + 1))) {
            i++;
            while (i < q.length() && Character.isDigit(q.charAt(i))) i++;
        }
        try { return Double.parseDouble(q.substring(at, i).replace(',', '.')); }
        catch (NumberFormatException e) { return 0; }
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

    /** A szöveg első szava (betűk és számjegyek), vagy üres. */
    private static String firstWord(String s) {
        int i = 0;
        while (i < s.length() && !Character.isLetterOrDigit(s.charAt(i))) i++;
        int b = i;
        while (i < s.length() && Character.isLetterOrDigit(s.charAt(i))) i++;
        return s.substring(b, i);
    }

    private static boolean isCountWord(String w) {
        for (String c : COUNT_WORDS) if (norm(c).equals(w)) return true;
        return false;
    }

    /** Csak írásjel és szóköz áll a két pozíció között? */
    private static boolean onlyPunctBetween(String q, int from, int to) {
        if (from > to) return false;
        for (int i = from; i < to; i++)
            if (Character.isLetterOrDigit(q.charAt(i))) return false;
        return true;
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

    /** Tárgyragos számnév alapalakja („hatot" → „hat", „6-ot" → „6"), vagy null. */
    private static String plainNumber(String t) {
        String[][] map = {{"egyet", "egy"}, {"kettot", "ket"}, {"harmat", "harom"},
                {"negyet", "negy"}, {"otot", "ot"}, {"hatot", "hat"}, {"hetet", "het"},
                {"nyolcat", "nyolc"}, {"kilencet", "kilenc"}, {"tizet", "tiz"}};
        for (String[] r : map) if (r[0].equals(t)) return r[1];
        String d = t.replaceAll("-?(?:ot|et|at)$", "");
        return d.matches("\\d{1,2}") ? d : null;
    }

    /**
     * A TÁPÉRTÉK-sor „protein"-je nem turmix.
     *
     * A „reggeli 400 kcal 25 g protein" mondatban a protein a fehérje neve,
     * nem egy megivott shaké – eddig háromszáz gramm proteinturmix került a
     * naplóba mellé, vagyis a reggeli kalóriájának a duplája. A grammal
     * kimondott alak egyértelmű: ott a szó a tápértéket nevezi meg.
     */
    private static String maskMacroWords(String query) {
        String s = norm(query);
        if (!s.contains("protein")) return query;
        // A „protein" csak MAGÁBAN tápérték: a „150 g protein turmix" és a
        // „150 g proteinszelet" valódi étel, azokhoz nem nyúlunk.
        String out = s.replaceAll("(\\d{1,3})\\s?(g|gr|gramm)\\s?protein(?![a-z])"
                + "(?!\\s?(?:turmix|shake|sejk|por|italpor|pudding|joghurt|szelet))",
                "$1 $2 #");
        return out.equals(s) ? query : out;
    }

    /**
     * A mennyiség a mondat MÁSIK felében: „ebédre töltött káposzta volt,
     * két adag".
     *
     * A mennyiség szándékosan nem ugrik át tagmondat-határon – a „csirkemell
     * rizzsel, 200 g" kétszáz grammja nem tartozhat mindkettőhöz. Az utolsó,
     * CSUPÁN mennyiséget tartalmazó tagmondat viszont nem lehet másé: ott
     * nincs mit félreérteni, és eddig egy adag ment be kettő helyett.
     *
     * Két feltétel véd: a fejben pontosan egy étel álljon, és ne legyen benne
     * saját szám. A mennyiséget egyszerűen a fej elé írjuk – onnantól a
     * megszokott „két adag töltött káposzta" alakot olvassuk.
     */
    private static String amountFromTheOtherClause(List<Food> list, String query) {
        String s = norm(query);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "[,;]\\s*(\\d{1,2}|[a-z]{2,10})\\s+(adag|tanyer|szelet|pohar|bogre|"
                        + "kanal|marek|falat|gomboc|db|darab)\\w*\\s*$").matcher(s);
        String head, amount;
        if (m.find()) {
            head = s.substring(0, m.start());
            // A ragos alakot alapalakra írjuk („két tányérral" → „két
            // tányér"): a mennyiség-olvasó a mértékegység alapalakját ismeri.
            amount = m.group(1) + " " + m.group(2);
        } else {
            // Puszta DARABSZÁM a záró tagmondatban: „sütöttem egy adag
            // palacsintát, megettem hatot". A tárgyrag itt a mértékegység
            // helyét foglalja el, és eddig egyetlen adag ment be hat helyett.
            java.util.regex.Matcher c = java.util.regex.Pattern.compile(
                    "[,;]\\s*(?:megettem|ettem|megittam|ittam)?\\s*"
                            + "(\\d{1,2}(?:-?(?:ot|et|at))?|egyet|kettot|harmat|negyet|"
                            + "otot|hatot|hetet|nyolcat|kilencet|tizet)\\s*$").matcher(s);
            if (!c.find()) return query;
            head = s.substring(0, c.start());
            amount = plainNumber(c.group(1));
            if (amount == null) return query;
        }
        if (head.matches(".*\\d.*")) return query;
        List<Match> hm = matches(list, head);
        if (hm.size() != 1) return query;
        // A mennyiséget közvetlenül az étel elé írjuk, és a fej SAJÁT
        // határozatlan névelőjét („egy adag palacsintát", „egy fagyit")
        // elhagyjuk – az „egy" különben a szomszédság jogán legyőzné a
        // kimondott számot, és megint egy adag menne be hat helyett.
        int pos = hm.get(0).pos;
        String before = head.substring(0, pos)
                .replaceAll("(?:^|\\s)egy(?:\\s+(?:adag|nagy|kis))?\\s*$", " ");
        return before + amount + " " + head.substring(pos);
    }

    static List<Hit> parse(List<Food> list, String query) {
        // Ami nem került a tányérra, az a naplóba se kerüljön: a „szeretnék
        // egy pizzát" és a „vettem két kiló almát" ugyanúgy tartalmaz ételt
        // és mennyiséget, mint egy bejegyzés – csak épp egyikből sem lett
        // falat. Eddig mindkettő bement, háromszáz, illetve ezer kalóriával.
        if (looksUneaten(query)) return new ArrayList<>();
        query = maskMacroWords(query);
        query = amountFromTheOtherClause(list, query);
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
        // Tápérték-sor-e a mondat: csak akkor tekintünk egy grammot a
        // FEHÉRJE mennyiségének, ha kalória is ki van írva mellette. Enélkül
        // a „250 ml protein turmix" és a „150 gramm protein turmix" is
        // tápérték-sornak látszana – pedig ott a protein a NÉV része.
        boolean label = Kcal.stated(q) > 0;
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
            // A TÁPÉRTÉK-sor grammja nem az étel súlya: a „Protein turmix
            // 1 adag – 120 kcal 24 g fehérje" huszonnégy grammja a fehérje,
            // mégis a turmix adagja lett belőle (huszonnégy gramm turmix).
            // A dobozról másolt sor pont így néz ki.
            if (label && !numPos.isEmpty() && numPos.get(numPos.size() - 1) == start
                    && nutrientWordAt(q, i)) {
                numPos.remove(numPos.size() - 1);
                numVal.remove(numVal.size() - 1);
            }
        }
        // A tartomány felső tagja viselheti a mértékegységet („3-4 dkg"): ezt
        // a mennyiségek szétosztása ELŐTT kell rendezni, különben a felső
        // érték már a tányéron van.
        averageUnitRanges(q, bareNumPos, bareNumVal, bareNumLen, numPos, numVal);
        // Mennyiség-plafon: 5 kg fölött a szám elgépelés vagy bevásárlás, nem
        // egy étkezés. Egy „9999999999 g" alakú elütés különben milliárd-
        // kalóriás étkezésként mérgezné meg a napi összesítőt, a statisztikát
        // és a diagramokat – némán. De a tíz kiló is elég ehhez: a „10 kg
        // alma" (a „10 dkg" mellényúlása) ötezer kalória. Ilyenkor inkább
        // mennyiség nélkül hagyjuk, mint a képtelen darabszámnál: ott 20 a
        // határ. A két kiló rizs vagy a másfél kiló alma belefér – az még
        // lehet egy nagy család vasárnapi adagja.
        for (int k2 = numVal.size() - 1; k2 >= 0; k2--) {
            double v = numVal.get(k2);
            if (v <= 0 || v > 5_000) { numVal.remove(k2); numPos.remove(k2); }
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
        mergeAndHalf(q, bareNumPos, bareNumVal, bareNumLen);
        averageRanges(q, bareNumPos, bareNumVal, bareNumLen);
        // Darabszámok: „2 tojás" = 2 × egy tojás súlya. Csak akkor számít, ha a
        // szám közvetlenül egy darabra számolható étel előtt áll, az étel még nem
        // kapott grammot, és a darabszám életszerű (legfeljebb 20).
        for (int n = 0; n < bareNumPos.size(); n++) {
            double count = bareNumVal.get(n);
            if (count < MIN_COUNT || count > 20) continue;
            int numEnd = bareNumPos.get(n) + bareNumLen.get(n);
            // Százalék nem darabszám: a „18% testzsír" tizennyolcasa nem
            // tizennyolc adag. (A fuzz találta: „disznótoros 18% adag" öt és
            // fél kiló disznótoros lett.)
            if (percentAfter(q, numEnd)) continue;
            for (int k = 0; k < foods.size(); k++) {
                if (grams[k] > 0 || foodPos.get(k) < 0) continue;
                String between = countWordAt(q, numEnd, foodPos.get(k));
                // A darabszám egy KÉSŐBBI előfordulás előtt is állhat
                // („sörözés: 3 korsó sör" – az étel tárolt pozíciója az első
                // említésé, a szám mégis a másodikhoz tartozik).
                if (between == null) {
                    for (String ns : foods.get(k).nstems) {
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
                double piece = pieceFor(foods.get(k), between);
                // Aminek nincs természetes darabmérete, ott a tipikus adag a
                // darab: a „két kebab" eddig egyetlen kebabnak számított, mert
                // a számláló egyszerűen elveszett. Ez 267 ételt érintett.
                //
                // Csak életszerű adagszámra: a „két wrap" és a „fél kebab"
                // valódi bevitel, a „12 rizs" viszont inkább elgépelt gramm,
                // és három kiló rizst írni a naplóba rosszabb, mint egy adagot.
                if (piece <= 0 && count <= 6) piece = foods.get(k).portion;
                if (piece <= 0) continue;
                // Az ADAG egész fogás: kettő-három életszerű, tizennyolc nem.
                // A darabszám (szem, szelet) mehet húszig, az adag nem.
                if (between != null && (between.startsWith("adag") || between.equals("porcio"))
                        && count > MAX_PORTIONS) continue;
                grams[k] = count * piece;
                break;
            }
        }
        // Az adag a név UTÁN is állhat: „grillcsirke fél adag". A számot a
        // legközelebbi megelőző, még mennyiség nélküli étel kapja – de csak a
        // saját tagmondatán belül.
        for (int n = 0; n < bareNumPos.size(); n++) {
            double count = bareNumVal.get(n);
            if (count < MIN_COUNT || count > 20) continue;
            int numStart = bareNumPos.get(n);
            int numEnd = numStart + bareNumLen.get(n);
            if (percentAfter(q, numEnd)) continue;
            String after = numEnd < q.length() ? q.substring(numEnd).trim() : "";
            // Mérőszó a szám után: „grillcsirke fél adag", „banán 2 db",
            // „tojás (3 db)", „kenyér 2 szelet". A bevásárlólista-szórend
            // („étel mennyiség") legalább olyan gyakori, mint a fordítottja –
            // eddig mégis csak az adag/porció ment át, a többiből egy adag lett.
            String unit = firstWord(after);
            boolean portionWord = unit.startsWith("adag") || unit.equals("porcio");
            if (portionWord && count > MAX_PORTIONS) continue;
            // Tört a név UTÁN, mértékegység nélkül: „az alma fele", „a pizza
            // fele". Csak törtre él: a „csirkemell 150" százötven grammot
            // jelent, nem százötven adagot.
            // A tört a saját tagmondatát is zárhatja: „az alma fele ÉS egy
            // szelet kenyér" – ott a fél alma ugyanúgy fél.
            boolean fractionAtEnd = count < 1
                    && (unit.isEmpty() || unit.equals("es") || unit.equals("meg"));
            if (!portionWord && !fractionAtEnd && !isCountWord(unit)
                    && !isPortionWord(unit)) continue;
            int best = -1;
            for (int k = 0; k < foods.size(); k++) {
                if (grams[k] > 0 || foodPos.get(k) < 0 || foodPos.get(k) >= numStart) continue;
                if (clause[foodPos.get(k)] != clause[numStart]) continue;
                // Csak az étel és a szám KÖZÖTT álló írásjelek engedettek:
                // a „banán (2 db)" zárójele nem szakítja el, egy közbeékelt
                // másik szó viszont igen.
                if (!onlyPunctBetween(q, foodPos.get(k) + foodLen.get(k), numStart)) continue;
                if (best < 0 || foodPos.get(k) > foodPos.get(best)) best = k;
            }
            if (best < 0) continue;
            double piece = fractionAtEnd ? foods.get(best).portion
                    : pieceFor(foods.get(best), unit);
            if (piece <= 0 && count <= 6) piece = foods.get(best).portion;
            if (piece <= 0) continue;
            grams[best] = count * piece;
        }
        // Mérőszó szám nélkül: a „tábla csoki" egy tábla, a „szelet kenyér"
        // egy szelet. Szám híján eddig a tipikus adag ment be – csokinál ez
        // negyedannyi, mint amit az ember megevett.
        for (int k = 0; k < foods.size(); k++) {
            if (grams[k] > 0 || foodPos.get(k) < 0) continue;
            String unit = unitBefore(q, foodPos.get(k));
            if (unit.isEmpty()) continue;
            if (!unit.equals("tabla") && !unit.startsWith("adag") && !unit.equals("porcio")
                    && !isCountWord(unit) && !isPortionWord(unit)) continue;
            double piece = pieceFor(foods.get(k), unit);
            if (piece > 0) grams[k] = piece;
        }
        for (int k = 0; k < foods.size(); k++) out.add(new Hit(foods.get(k), grams[k]));
        // „…de csak a felét ettem meg": a hátravetett tört az egész étkezésre
        // vonatkozik. Csak egyetlen ételnél merjük alkalmazni – többnél nem
        // tudni, melyikre gondolt.
        if (out.size() == 1) {
            double f = eatenFraction(q);
            if (f > 0 && f < 1) {
                Hit h = out.get(0);
                double base = h.grams > 0 ? h.grams : h.food.portion;
                out.set(0, new Hit(h.food, base * f));
            }
        }
        return out;
    }

    /**
     * A megevett hányad a tagmondat végéről: „a felét ettem meg" = 0,5,
     * „a negyedét hagytam ott" = 0,75. 0, ha nincs ilyen a mondatban.
     *
     * Az evés és a meghagyás egymás tükörképei: amit otthagyott, azt NEM ette
     * meg. A „fél pizza" elöl álló törtje nem ide tartozik – azt a
     * mennyiség-felismerő már elvitte, mielőtt ide jutnánk.
     */
    static double eatenFraction(String q) {
        java.util.regex.Matcher m = FRACTION_CLAUSE.matcher(q);
        if (!m.find()) {
            // Ige nélkül is egyértelmű, ha a „csak" ott van: a „100 g rizs,
            // de csak a felét" fele annyi. A puszta „a felét" viszont kevés –
            // abból nem derül ki, hogy megette vagy meghagyta.
            java.util.regex.Matcher o = ONLY_FRACTION.matcher(q);
            if (!o.find()) return 0;
            String g = o.group(1);
            return g.startsWith("felet") ? 0.5
                    : g.startsWith("ketharmad") ? 2 / 3.0
                    : g.startsWith("harmad") ? 1 / 3.0 : 0.25;
        }
        double f = m.group(1).startsWith("felet") ? 0.5
                : m.group(1).startsWith("ketharmad") ? 2 / 3.0
                : m.group(1).startsWith("harmad") ? 1 / 3.0
                : 0.25;
        boolean left = m.group(2).startsWith("hagy") || m.group(2).startsWith("otthagy")
                || m.group(2).startsWith("meghagy");
        return left ? 1 - f : f;
    }

    /** „csak a felét" – a „csak" maga mondja meg, hogy kevesebb lett. */
    private static final java.util.regex.Pattern ONLY_FRACTION =
            java.util.regex.Pattern.compile(
                    "csak\\s(?:a\\s)?(felet|harmadat|ketharmadat|negyedet)(?![a-z])");

    /** „a felét ettem meg" / „a negyedét otthagytam" – előre lefordítva. */
    private static final java.util.regex.Pattern FRACTION_CLAUSE =
            java.util.regex.Pattern.compile(
                    "(?<![a-z])(felet|harmadat|ketharmadat|negyedet)\\s"
                    + "(ettem|megettem|hagytam|otthagytam|meghagytam)(?![a-z])");

    /**
     * Egy mérőszónyi étel grammban: egy tábla csoki száz gramm, egy szelet
     * pizza száz, egy tányér leves egy adag, egy tojás egy darab.
     */
    private static double pieceFor(Food f, String unit) {
        if (waterMl(f, unit) > 0) return waterMl(f, unit);
        if (unit.equals("tabla")) return 100;                // egy tábla csoki
        if (unit.equals("szelet") && sliceGrams(f) > 0) return sliceGrams(f);
        if (unit.startsWith("adag") || unit.equals("porcio") || isPortionWord(unit))
            return f.portion;
        return pieceGrams(f);
    }

    /**
     * Az étel SZAVA előtt közvetlenül álló szó, vagy üres.
     *
     * A szótő a szó belsejében is állhat („tábla étcsoki"), ezért előbb a szó
     * elejére lépünk vissza, és csak onnan nézzük a megelőző szót.
     */
    private static String unitBefore(String q, int foodPos) {
        int w = foodPos;
        while (w > 0 && Character.isLetterOrDigit(q.charAt(w - 1))) w--;
        int e = w;
        while (e > 0 && q.charAt(e - 1) == ' ') e--;
        if (e == 0 || e == w) return "";
        int b = e;
        while (b > 0 && Character.isLetter(q.charAt(b - 1))) b--;
        return q.substring(b, e);
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
        // A mondat végi írásjel és hangulatjel nem tartozik a szöveghez: a
        // tagadás-szabályok a tagmondat VÉGÉT nézik, és egy „:)" miatt eddig
        // bekerült az, amit az ember épp nem evett meg. A levágás csak a
        // végéről történik, tehát a találatok helye nem csúszik el.
        String q = mask(norm(query));
        int qe = q.length();
        while (qe > 0 && !Character.isLetterOrDigit(q.charAt(qe - 1))) qe--;
        q = q.substring(0, qe);
        // Az ékezetes párja ugyanolyan hosszú és ugyanúgy indexelhető: az
        // ékezettel megkülönböztetett tövek (sör/sor) ebből döntenek.
        String qAcc = normAcc(query);
        if (qAcc.length() > qe) qAcc = qAcc.substring(0, qe);
        List<Match> found = new ArrayList<>();
        for (Food f : list) {
            int bestPos = -1, bestLen = 0;
            for (String ns : f.nstems) {
                if (ns.isEmpty()) continue;
                int p = stemIndex(q, qAcc, ns);
                // A leghosszabb illeszkedő szótő dönt; azonos hossznál a korábbi.
                if (p >= 0 && (ns.length() > bestLen || (ns.length() == bestLen && p < bestPos))) {
                    bestPos = p; bestLen = ns.length();
                }
            }
            if (bestPos >= 0) found.add(new Match(f, bestPos, bestLen));
        }
        // „egy sor csoki”: az ékezet nélkül írt tő KÖZVETLENÜL egy másik étel
        // előtt mértékszó, nem étel. (A csokoládé mennyiségét amúgy is ez adja:
        // egy sor huszonöt gramm.) A sör ettől nem sérül: az ital nem szokott
        // egy másik étel neve előtt állni, és ékezettel írva úgyis átmegy.
        found = dropMeasureWords(q, qAcc, found);
        // „feta sajt”: a fajta után álló gyűjtőnév ugyanaz az étel, nem másik.
        found = dropGenericAfterKind(q, found);

        // A hosszabb találatba beleeső rövidebbeket eldobjuk.
        List<Match> out = new ArrayList<>();
        for (Match m : found) {
            boolean covered = false;
            for (Match o : found) {
                if (o == m) continue;
                boolean inside = o.pos <= m.pos && o.pos + o.len >= m.pos + m.len;
                if ((inside && o.len > m.len) || coveredByStem(q, m, o.food)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) out.add(m);
        }
        out = oneFoodPerWord(q, out);
        out = dropRedundantBase(q, out);
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
     * Hátravetett tagadás: „csokit nem ettem", „sört nem ittam", „csokit nem,
     * almát igen".
     *
     * A tagadás eddig csak ELŐRE hatott. Magyarul viszont ugyanolyan gyakori
     * a fordított szórend, és ott az étel a tagadás ELŐTT áll: eddig minden
     * ilyen mondat felvette azt, amit az ember épp NEM evett meg – a csokit,
     * a sört, a pizzát.
     *
     * Csak akkor lép működésbe, ha a „nem" után evés-ige áll, vagy ha a „nem"
     * zárja a tagmondatot („csokit nem, almát igen"). A visszafelé törlés a
     * tagmondat elején és a kötőszónál megáll.
     */
    private static void dropBackwardNegated(String q, List<Match> in,
                                            java.util.Set<Match> dead) {
        int p = q.indexOf("nem");
        while (p >= 0) {
            boolean word = (p == 0 || !Character.isLetter(q.charAt(p - 1)))
                    && (p + 3 >= q.length() || !Character.isLetter(q.charAt(p + 3)));
            if (word) {
                String after = q.substring(Math.min(q.length(), p + 3)).trim();
                boolean undone = after.isEmpty() || after.startsWith(",")
                        || after.startsWith(";");
                for (String v : new String[]{"ettem", "eszem", "ettunk", "ittam",
                        "iszom", "ittunk", "kertem", "kerek", "kertunk", "volt",
                        "fogyasztottam", "tettem", "hoztam", "vettem"})
                    if (after.startsWith(v)) undone = true;
                if (undone) {
                    int a = p;
                    while (a > 0 && q.charAt(a - 1) != ',' && q.charAt(a - 1) != ';'
                            && q.charAt(a - 1) != '.') a--;
                    for (String c : new String[]{" de ", " viszont ", " azonban ",
                            " es ", " majd "}) {
                        int k = q.lastIndexOf(c, p);
                        if (k >= 0 && k >= a) a = k + c.length();
                    }
                    for (Match m : in) if (m.pos >= a && m.pos < p) dead.add(m);
                }
            }
            p = q.indexOf("nem", p + 1);
        }
    }

    /**
     * Külön tételként van-e felsorolva a két találat?
     *
     * A jelzős szerkezet („csokis müzliszelet") EGY ételt jelent, a kötőszós
     * felsorolás („csoki és müzliszelet") kettőt. A különbség csak a köztük
     * álló szövegben látszik.
     */
    private static boolean listedSeparately(String q, Match a, Match b) {
        int from = Math.min(a.pos + a.len, b.pos + b.len);
        int to = Math.max(a.pos, b.pos);
        if (from >= to || to > q.length()) return false;
        String gap = q.substring(from, to);
        return gap.contains(",") || gap.contains(";") || gap.contains(" es ")
                || gap.contains(" meg ") || gap.contains(" plusz ");
    }

    /**
     * Beleesik-e a rövidebb találat a másik étel VALAMELYIK szótövébe?
     *
     * Ételenként csak a LEGHOSSZABB szótő helyét jegyezzük meg, és eddig a
     * takarás-vizsgálat is csak azt nézte. A „párizsi felvágott" párizsija így
     * a hosszabb „felvágott" tövön került be, a szó elején álló „párizsi"
     * pedig szabadon hagyta a benne rejlő „rizs"-t: kétszáz gramm rizs került
     * a felvágott mellé, csendben.
     */
    private static boolean coveredByStem(String q, Match m, Food other) {
        for (String ns : other.nstems) {
            if (ns.length() <= m.len) continue;
            int p = q.indexOf(ns);
            while (p >= 0) {
                if (p <= m.pos && p + ns.length() >= m.pos + m.len) return true;
                p = q.indexOf(ns, p + 1);
            }
        }
        return false;
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
            // A „csirke" a legtöbb ételnév előtt jelző, nem külön adag: a
            // „csirke curry" egy tál curry, nem curry PLUSZ egy csirkemell.
            // Csak azok az ételek, amikben a hús ELEVE benne van, és amiket
            // senki nem eszik külön csirkemell mellé – a „pizza és csirkemell"
            // két külön adag, ezért a pizza nincs a listán.
            // A gyros és a kebab adagja a lepényt is tartalmazza: a „gyros
            // pitában" egy gyros, nem gyros PLUSZ egy pita.
            {"Pita / lepénykenyér", "Gyros", "Kebab"},
            // A pizza feltétje benne van a pizza kalóriájában: a „négy sajtos
            // pizza" egy pizza, nem pizza PLUSZ egy adag sajt (1212 kcal!).
            // A csokoládé a szelet és a muffin kalóriájában benne van: a
            // „csokis müzliszelet" nem szelet PLUSZ egy tábla csoki.
            {"Csokoládé", "Müzliszelet", "Proteinszelet", "Muffin / brownie"},
            // A „cukormentes üdítő"-nél a cukros változat NEM külön ital: a
            // jelző pontosan azt mondja, hogy ez ugyanaz, csak nulla
            // kalóriával. Fordítva áll a takarásban: a jelző (light) marad,
            // az alap (üdítő, energiaital, szörp) esik.
            // A pörkölt, a gulyás, a paprikás és a vadas HÚSÉTEL: a jelzőként
            // elé írt hús („marha pörkölt", „birka gulyás", „vadas marha") nem
            // külön adag hús a tál mellé. Egybeírva eddig is egy tétel volt –
            // különírva viszont a hús adagja is bement, plusz háromszáz-
            // négyszáz kalória, csendben.
            {"Marhahús", "Pörkölt", "Gulyásleves", "Csirkepaprikás", "Vadas hús", "Tokány"},
            {"Bárány / birka", "Pörkölt", "Gulyásleves", "Csirkepaprikás", "Tokány"},
            {"Vadhús (szarvas, vaddisznó, nyúl)", "Pörkölt", "Gulyásleves",
                    "Csirkepaprikás", "Vadas hús", "Tokány"},
            {"Üdítő (cukros)", "Cukormentes / light"},
            {"Energiaital", "Cukormentes / light"},
            {"Szörp (hígítva)", "Cukormentes / light"},
            // Az „egy shaker turmix" egy ital: a shaker a proteinesé, a
            // gyümölcsös mellette nem külön pohár.
            {"Gyümölcsturmix / smoothie", "Protein turmix"},
            // A pite tölteléke benne van a pite kalóriájában: a „meggyes pite"
            // nem pite PLUSZ egy adag meggy.
            {"Cseresznye / meggy", "Pite (almás/gyümölcsös)"},
            {"Alma", "Pite (almás/gyümölcsös)"},
            // A szendvics és a hamburger adagja is a feltéttel együtt értendő:
            // a „sonkás-sajtos melegszendvics" egy szendvics, nem szendvics
            // PLUSZ egy adag sonka PLUSZ egy adag sajt – az együtt már két
            // szendvicsnyi kalória.
            // A gyros és a kebab kész tál: a hús, a lepény és az öntet együtt
            // van benne. A quesadilla neve maga jelenti a sajtot.
            {"Sajt (trappista)", "Pizza", "Szendvics", "Hamburger", "Gyros", "Kebab",
                    "Quesadilla"},
            {"Sonka", "Pizza", "Szendvics", "Gyros", "Kebab"},
            {"Szalámi", "Pizza", "Szendvics"},
            {"Gomba", "Pizza"},
            // A wok adagja a zöldséget is tartalmazza: a „zöldséges wok" egy
            // wok, nem wok PLUSZ egy adag párolt zöldség.
            {"Zöldség (vegyes / párolt)", "Wok (zöldséges-húsos)"},
            {"Csirkemell (sült/grill)", "Wok (zöldséges-húsos)", "Curry", "Gyros", "Kebab",
                    "Csirkés saláta", "Csirkés wrap", "Burrito", "Quesadilla",
                    "Rizses hús", "Csirkepaprikás", "Chilis bab (con carne)", "Rizottó"},
            // A csusza tejfölös: ez a neve, nem egy külön kanál tejföl.
            {"Tejföl", "Túrós csusza"},
    };

    private static List<Match> dropRedundantBase(String q, List<Match> in) {
        if (in.size() < 2) return in;
        List<Match> out = new ArrayList<>(in);
        for (String[] row : BASE_INCLUDED) {
            Match base = null, dishAt = null;
            for (Match m : out) {
                if (m.food.name.equals(row[0])) base = m;
                for (int i = 1; i < row.length; i++)
                    if (m.food.name.equals(row[i])) { dishAt = m; break; }
            }
            boolean dish = dishAt != null;
            // Kötőszóval FELSOROLVA két külön tétel: a „csoki és müzliszelet"
            // csoki PLUSZ szelet, a „csokis müzliszelet" viszont egy szelet.
            if (base != null && dish && listedSeparately(q, base, dishAt)) continue;
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

    /**
     * Szándék, vásárlás, főzés, kidobás – ami NEM evés.
     *
     * A magyar ugyanazzal a szórenddel mondja el a vágyat és a vacsorát: a
     * „szeretnék egy pizzát" és az „ettem egy pizzát" csak az igében tér el.
     * Eddig mindkettőből bejegyzés lett – a „vettem két kiló almát" ezer
     * kalóriát írt a naplóba egy bevásárlásból.
     *
     * A szabály kétfeltételes, mert a szándék-szó ÖNMAGÁBAN nem elég: a
     * „vettem egy kávét és megittam" valódi bejegyzés. Ezért csak akkor
     * dobjuk el a mondatot, ha van benne szándék-szó, ÉS nincs benne egyetlen
     * EVÉS-ige sem.
     */
    public static boolean looksUneaten(String query) {
        String s = norm(query == null ? "" : query);
        if (s.isEmpty()) return false;
        boolean intent = false;
        for (String w : new String[]{
                // Jövő és szándék.
                "holnap", "majd veszek", "majd sutok", "fogok", "tervez",
                "szeretnek", "szeretne", "kene", "kellene", "jo lenne",
                "jol esne", "kivanok", "kivannek",
                // Bevásárlás: a kosárban lévő étel nem elfogyasztott étel.
                // A „kaptam" SZÁNDÉKOSAN nincs itt: aki azt írja, „kaptam egy
                // szelet tortát", az rendszerint meg is ette – a naplózó
                // mezőben a kapott étel elfogyasztott étel.
                "vettem", "veszek", "vasarol", "hoztam", "hozok",
                // Főzés jelen időben: a „főzök egy levest" még nem vacsora.
                "fozok", "sutok", "keszitek", "keszitem", "fozni fogok",
                // Ami a kukába ment.
                "eldobtam", "kidobtam", "kiontottem", "megromlott", "kidobom",
                // Amit más evett meg.
                "megette a", "megettek", "megitta a",
                // A bevásárlólista és a KIFOGYOTT étel: mindkettő arról szól,
                // hogy az étel épp NINCS meg – az egyikből mégis vacsora
                // lett. („Elfogyott a tej" hatvan kaló volt a naplóban.)
                "bevasarlolista", "bevasarlo lista", "bevasarlas", "kifogyott",
                "elfogyott a", "nincs itthon", "nincs otthon",
                // Ami eltéve vagy lefagyasztva vár: „el kell tennem",
                // „lefagyasztottam a maradékot".
                "el kell tennem", "eltennem", "lefagyaszt", "befottnek",
                // Vágy és kíváncsiság: „megkívántam a csokit", „meg akarok
                // kóstolni egy tiramisut", „receptet keresek".
                "megkivantam", "megkivanom", "kostolni", "recept",
                // Feltételes mód: a „rendelnék egy pizzát" nem rendelés.
                "rendelnek", "sutnek", "foznek", "keszitenek", "ennek egy"})
            if (s.contains(w)) { intent = true; break; }
        // A mondat ELEJÉN álló „majd" a JELEN idejű evés-igével együtt jövő
        // idő: a „majd eszem egy pizzát" még nem vacsora. Az evés-ige miatt
        // az alábbi kivétel-lista különben felmentené, ezért itt dől el – a
        // mondat közepén viszont a „majd" sorrendet jelent („ettem egy
        // levest, majd egy palacsintát"), ezért csak a mondatkezdő alak
        // számít, és csak a jelen idejű igével.
        for (String p : new String[]{"majd ", "esetleg ", "talan "})
            if (s.startsWith(p))
                for (String w : new String[]{"eszem", "eszek", "eszunk", "megeszem",
                        "iszom", "megiszom", "bekapok", "rendelek"})
                    if (wholeWord(s, w)) return true;
        if (!intent) return false;
        // Az evés-igét SZÓHATÁRRAL keressük: a „Vettem" végén ott az „ettem",
        // és enélkül a bevásárlás úgy nézett ki, mint egy vacsora.
        for (String w : new String[]{"ettem", "ettel", "evett", "eszem", "ittam",
                "ittal", "ivott", "iszom", "megettem", "megittam", "elfogyasztottam",
                "bekaptam", "haraptam", "reggeliztem", "ebedeltem", "vacsoraztam",
                "uzsonnaztam", "nassoltam", "faltam", "kertem", "rendeltem",
                // A „bevettem" a VÁSÁRLÁS igéjét tartalmazza, pedig pont az
                // ellenkezőjét jelenti: az „edzés előtt bevettem egy
                // kreatint" elfogyasztott étrend-kiegészítő, és eddig
                // bevásárlásnak látszott.
                "bevettem", "beszedtem", "lenyeltem"})
            if (wholeWord(s, w)) return false;
        return true;
    }

    /** Tápérték-szó áll-e a megadott helytől (szóközöket átlépve). */
    private static boolean nutrientWordAt(String q, int from) {
        int i = from;
        while (i < q.length() && q.charAt(i) == ' ') i++;
        String rest = q.substring(Math.min(i, q.length()));
        // EGÉSZ szóként: a „150 gramm zsíros kenyér" zsírosa jelző, nem
        // tápérték-sor – és a „zsír" magában nem is kerül a listára, mert
        // konyhai zsírként valódi étel.
        for (String w : new String[]{"feherje", "feherjet", "protein", "szenhidrat",
                "rost", "telitett"}) {
            if (!rest.startsWith(w)) continue;
            int e = w.length();
            if (e >= rest.length() || !Character.isLetter(rest.charAt(e))) return true;
        }
        return false;
    }

    /** Egész szóként szerepel-e a tő a szövegben. */
    private static boolean wholeWord(String s, String w) {
        for (int i = s.indexOf(w); i >= 0; i = s.indexOf(w, i + 1)) {
            boolean left = i == 0 || !Character.isLetter(s.charAt(i - 1));
            int e = i + w.length();
            boolean right = e >= s.length() || !Character.isLetter(s.charAt(e));
            if (left && right) return true;
        }
        return false;
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
        dropBackwardNegated(q, in, dead);
        // Az étel közvetlenül a „helyett"/„nélkül" előtt áll (rövid rag belefér).
        for (String w : new String[]{"helyett", "nelkul"}) {
            int p = q.indexOf(w);
            while (p >= 0) {
                Match best = null;
                for (Match m : in) {
                    int end = m.pos + m.len;
                    if (end > p || p - end > 6) continue;
                    // A kihagyott hozzávaló KÖZVETLENÜL a „nélkül" előtt áll.
                    // A köztük lévő szöveg csak a saját ragja lehet: ha egy
                    // MÁSIK szó is elfér ott, akkor nem ő a kihagyott tétel.
                    // Enélkül a „hamburger sajt nélkül" hamburgerét öltük meg
                    // (a sajtot a hamburger már elnyelte, így a legközelebbi
                    // találat maga a hamburger lett) – és a mondatból semmi
                    // nem került a naplóba.
                    String gap = q.substring(end, p);
                    boolean own = true, broke = false;
                    for (int i = 0; i < gap.length(); i++) {
                        if (Character.isLetter(gap.charAt(i))) {
                            if (broke || i >= 3) { own = false; break; }
                        } else broke = true;
                    }
                    if (own && (best == null || m.pos > best.pos)) best = m;
                }
                if (best != null) dead.add(best);
                p = q.indexOf(w, p + 1);
            }
        }
        // Tagadott ige után álló ételek, az első ÍRÁSJELIG.
        //
        // Az „és" itt nem határ: a „nem ettem csokit és chipset" mindkét
        // tételt tagadja, a tagmondat-felosztás viszont a chipset külön
        // tagmondatba tette, és így bekerült a naplóba. Az írásjel viszont
        // igen: a „nem ettem semmit, de ittam kávét" kávéja megmarad.
        for (String w : new String[]{"nem ettem", "nem eszem", "nem ittam",
                "nem iszom", "nem kertem", "nem kerek", "kihagytam",
                // A „mégsem" ugyanaz a tagadás, csak megfordított szándékkal:
                // a „mégsem ettem a csokit" eddig huszonöt gramm csokoládét
                // írt a naplóba – pont azt, amit az ember nem evett meg.
                "megsem ettem", "megsem eszem", "megsem ittam", "megsem iszom",
                "megsem kertem", "megsem vettem",
                "elutasitottam", "visszautasitottam"}) {
            int p = q.indexOf(w);
            while (p >= 0) {
                int stop = q.length();
                for (int i = p; i < q.length(); i++) {
                    char ch = q.charAt(i);
                    if (ch == ',' || ch == ';' || ch == '.' || ch == '+') { stop = i; break; }
                }
                // Az ELLENTÉTES kötőszó írásjel nélkül is lezárja a tagadást:
                // a „nem ettem csokit de almát igen" almáját megette az ember.
                // Az „és" szándékosan nincs itt: az folytatja a tagadást.
                for (String c : new String[]{" de ", " viszont ", " ellenben ", " azonban "}) {
                    int k = q.indexOf(c, p);
                    if (k >= 0 && k < stop) stop = k;
                }
                // Egy ÁLLÍTÓ ige is lezárja a tagadást: a „nem ettem reggelit
                // és ittam egy kávét" kávéját megitta az ember.
                for (String v : new String[]{"ettem", "eszem", "ittam", "iszom", "kertem"}) {
                    int a = q.indexOf(v, p + w.length());
                    while (a >= 0) {
                        boolean negated = a >= 4 && q.startsWith("nem ", a - 4);
                        if (!negated) { stop = Math.min(stop, a); break; }
                        a = q.indexOf(v, a + 1);
                    }
                }
                for (Match m : in)
                    if (m.pos > p && m.pos < stop && !isSideDish(q, m)) dead.add(m);
                p = q.indexOf(w, p + 1);
            }
        }
        // Visszafelé mutató elutasítás: „megkínáltak tortával, de nem kértem".
        // Csak a KÉRÉS-tagadásra, és csak ha a tagmondatnak nincs saját étele –
        // az „ettem egy almát, aztán nem ettem semmit" almáját nem vesszük el.
        for (String w : new String[]{"nem kertem", "nem kerek", "elutasitottam",
                "visszautasitottam"}) {
            int p = q.indexOf(w);
            while (p >= 0) {
                int cs = p, ce = p;
                while (cs > 0 && !isBreak(q.charAt(cs - 1))) cs--;
                while (ce < q.length() && !isBreak(q.charAt(ce))) ce++;
                // A tagmondatban a tagadáson és néhány kötőszón kívül SEMMI
                // más nem állhat. Az étel-felismerés nem tud mindent (a
                // „cukrot" ragozott alakját például nem), ezért az „ittam
                // kávét, de cukrot nem kértem" nem veheti el a kávét: ott a
                // tagmondatnak van saját tárgya, csak nem ismerjük fel.
                String clause = q.substring(cs, ce).replace(w, " ");
                for (String f : new String[]{"de", "es", "viszont", "azonban",
                        "sajnos", "inkabb", "vegul", "persze", "en", "azt", "ezt"})
                    clause = clause.replaceAll("(?<![a-z])" + f + "(?![a-z])", " ");
                if (clause.trim().isEmpty()) {
                    int ps = Math.max(0, cs - 1);
                    while (ps > 0 && !isBreak(q.charAt(ps - 1))) ps--;
                    for (Match m : in) if (m.pos >= ps && m.pos < cs) dead.add(m);
                }
                p = q.indexOf(w, p + 1);
            }
        }
        if (dead.isEmpty()) return in;
        List<Match> out = new ArrayList<>();
        for (Match m : in) if (!dead.contains(m)) out.add(m);
        return out;
    }

    /** Tagmondat-határ: a mondat itt új állítást kezd. */
    private static boolean isBreak(char c) {
        return c == ',' || c == ';' || c == '.' || c == '+';
    }

    /**
     * A tagadás KÍSÉRŐJE-e ez az étel?
     *
     * A „nem kértem sültkrumplit a hamburger mellé" mondatban a hamburgert
     * megette az ember – csak a köretet hagyta el. A tagadás eddig az egész
     * tagmondatot elvitte, vagyis egy valódi, hétszáz kalóriás fogást törölt.
     *
     * A „mellé/mellett/hozzá" pont ezt a szerepet jelöli: ami előtte áll, az
     * a kísérő, nem a tagadás tárgya.
     */
    private static boolean isSideDish(String q, Match m) {
        int end = m.pos + m.len;
        int to = Math.min(q.length(), end + 14);
        String after = end < to ? q.substring(end, to) : "";
        return after.contains("melle") || after.contains("mellett")
                || after.contains("hozza");
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
                Match a = in.get(j), b2 = in.get(i);
                // Ha a két találat ÁTFEDI egymást, nem a súlyosabb étel dönt,
                // hanem a hosszabb szótő: az „almáját"-ban az „alma" és a
                // „máj" ugyanazokon a betűkön osztozik, és ott az alma a szó.
                // (Az „almáját" eddig csirkemájat naplózott.)
                boolean over = a.pos < b2.pos + b2.len && b2.pos < a.pos + a.len;
                beaten = over ? (a.len > b2.len || (a.len == b2.len && j < i))
                              : beats(a, j, b2, i);
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
     *
     * A KÖTŐJEL akkor választ el, ha MELLÉKNEVEK közt áll: a „sonkás-sajtos”
     * két hozzávalót jelent, nem egyet – eddig egy szónak számított, és a
     * nehezebbik étel elnyomta a másikat (eltűnt a sonka). A magyar
     * melléknévképző -s-re végződik (sonkás, sajtos, tejfölös, sós), ezért
     * ehhez elég a kötőjel előtti betűt nézni.
     *
     * A „túró-rudi” és a „hot-dog” így egy szó marad: ott a kötőjel nem két
     * hozzávalót köt össze, hanem egy nevet tagol.
     */
    private static boolean sameWord(String q, Match a, Match b) {
        int from = Math.min(a.pos, b.pos);
        int to = Math.max(a.pos + a.len, b.pos + b.len);
        if (from < 0 || to > q.length()) return false;
        for (int i = from; i < to; i++) {
            char ch = q.charAt(i);
            if (Character.isLetterOrDigit(ch)) continue;
            if (ch == '-') {
                if (i > 0 && q.charAt(i - 1) == 's') return false;   // sonkás-sajtos
                continue;                                            // túró-rudi
            }
            return false;
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
            // Kötőszóval FELSOROLVA két külön étel: a „csirkemellből rántott
            // hús" egy fogás, a „csirkemell ÉS rántott hús" viszont kettő –
            // eddig az utóbbiból is egy lett, vagyis eltűnt egy adag.
            String between = q.substring(from, Math.min(to, q.length()));
            if (between.contains(",") || between.contains(";")
                    || between.contains(" es ") || between.contains(" meg ")
                    || between.contains(" plusz ")) continue;
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
