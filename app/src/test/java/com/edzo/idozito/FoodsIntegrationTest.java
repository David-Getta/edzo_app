package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Teljes napi menü-mondatok: több tagmondat, kötőszavak, étkezés-címkék,
 * mennyiségek és darabok EGYÜTT. Minden sor egy életszerű bevitel – a
 * várt eredmény az ételek neve és grammja, ahogy a naplóba kerülne.
 */
public class FoodsIntegrationTest {

    private static String summary(String q) {
        StringBuilder sb = new StringBuilder();
        for (Foods.Hit h : Foods.parse(Arrays.asList(Foods.ALL), q)) {
            if (sb.length() > 0) sb.append(" + ");
            double g = h.grams > 0 ? h.grams : h.food.portion;
            sb.append(h.food.name).append(" ").append(Math.round(g)).append("g");
        }
        return sb.toString();
    }

    @Test public void fullDaySentencesParseAsAWhole() {
        assertEquals("Tojás 110g + Kenyér 35g + Vaj 10g",
                summary("reggelire 2 tojás és egy pirítós vajjal"));
        assertEquals("Gulyásleves 400g + Kenyér 70g",
                summary("ebédre gulyásleves és 2 szelet kenyér"));
        assertEquals("Túró 100g + Zöldség (vegyes / párolt) 200g",
                summary("vacsorára túró és zöldség"));
        assertEquals("Alma 150g + Joghurt 150g",
                summary("tízóraira egy alma meg egy joghurt"));
        assertEquals("Rántott sajt 120g + Rizs (főtt) 200g + Tartármártás 30g",
                summary("ettem egy rántott sajtot rizzsel és tartárral"));
        assertEquals("Saláta (zöld) 50g + Tészta (főtt) 500g",
                summary("ma csak egy salátát ettem ebédre és este 2 adag tésztát"));
        assertEquals("Zabpehely 50g + Banán 120g + Méz 20g"
                        + " + Csirkemell (sült/grill) 150g + Rizs (főtt) 200g",
                summary("reggeli: zabkása banánnal és mézzel, ebéd: csirkemell rizzsel"));
        assertEquals("Protein turmix 300g + Banán 120g",
                summary("edzés után protein turmix és egy banán"));
        assertEquals("Kefir 500g + Keksz 36g",
                summary("fél liter kefir és 3 db keksz"));
        assertEquals("Gyros 350g + Sült krumpli 150g",
                summary("kaja: gyros tál extra sült krumplival"));
        assertEquals("Sör 1500g + Hamburger 250g",
                summary("sörözés: 3 korsó sör és egy hamburger"));
        assertEquals("Chips 50g + Csokoládé 25g",
                summary("nassoltam egy zacskó chipset meg egy csokit"));
        assertEquals("Rántotta 150g + Paradicsom 100g",
                summary("vacsira rántotta 3 tojásból, paradicsommal"));
    }

    @Test public void canteenClassicsAreOneDishNotTwo() {
        // A „spenót főzelék" egy étel – nem spenót MEG főzelék.
        assertEquals("Főzelék 350g", summary("spenót főzelék"));
        assertEquals("Főzelék 350g", summary("zöldborsó főzelék"));
        assertEquals("Főzelék 350g + Kolbász 100g", summary("lencse főzelék kolbásszal"));
        assertEquals("Tökfőzelék 350g + Fasírt 150g", summary("tökfőzelék fasírozottal"));
        assertEquals("Paradicsomos káposzta 400g", summary("paradicsomos káposzta"));
        assertEquals("Krémleves (zöldség) 350g", summary("brokkoli krémleves"));
        assertEquals("Palócleves 400g", summary("palócleves"));
        assertEquals("Gyümölcsleves 350g", summary("hideg meggyleves"));
    }

    @Test public void slangNamesAndWorldDishesAreUnderstood() {
        // A „krumpi" nem pálinka (pedig benne van a „rum")!
        assertEquals("Burgonya (főtt) 250g", summary("krumpi hússal"));
        // A csirke a saláta MELLETT van: 8 kcal helyett 255 az igazság.
        assertEquals("Saláta (zöld) 50g + Csirkemell (sült/grill) 150g",
                summary("sali csirkével"));
        assertEquals("Paradicsom 100g + Uborka 100g + Saláta (zöld) 50g",
                summary("pari ubi saláta"));
        assertEquals("Szendvics 150g + Sonka 50g", summary("egy szendó sonkával"));
        assertEquals("Szilvás gombóc 250g", summary("szilvás gombóc"));
        assertEquals("Káposztás tészta 330g", summary("káposztás cvekedli"));
        assertEquals("Húsleves 400g", summary("grízgaluska leves"));
        assertEquals("Ramen 500g", summary("ramen leves"));
        assertEquals("Ramen 500g", summary("pho leves"));
        assertEquals("Poke bowl 400g", summary("poke bowl"));
        assertEquals("Caprese saláta 250g", summary("caprese saláta"));
        assertEquals("Rizottó 300g + Gomba 100g", summary("risotto gombával"));
    }

    @Test public void thePortionWordWorksAfterTheFoodNameToo() {
        // A „fél adag gyros" mellett a „grillcsirke fél adag" sorrend is él.
        assertEquals("Csirkemell (sült/grill) 75g", summary("grillcsirke fél adag"));
        assertEquals("Gulyásleves 800g", summary("gulyásleves 2 adag"));
        assertEquals("Rántott hús (sertés) 270g + Burgonyapüré 300g",
                summary("rántott hús másfél adag krumplipürével"));
        // A régi sorrend változatlan.
        assertEquals("Gyros 175g", summary("fél adag gyros"));
    }

    @Test public void snacksAndStreetFoodDoNotDoubleCount() {
        assertEquals("Aszalt gyümölcs 40g", summary("aszalt sárgabarack"));
        assertEquals("Popcorn 40g", summary("pattogatott kukorica"));
        assertEquals("Vattacukor 30g", summary("vattacukor"));
        assertEquals("Sült gesztenye 100g", summary("sült gesztenye"));
        assertEquals("Keksz 40g", summary("zabkeksz"));
        assertEquals("Chips 50g", summary("proteinchips"));
        assertEquals("Jégkása 300g", summary("slush"));
        assertEquals("Rizsszelet / puffasztott rizs 10g", summary("abonett"));
        // Márkanevek.
        assertEquals("Gumicukor / cukorka 30g", summary("haribo gumicukor"));
        assertEquals("Energiaital 250g", summary("red bull"));
        assertEquals("Csokoládé 25g", summary("sport szelet"));
        assertEquals("Üdítő (cukros) 330g", summary("fantát ittam"));
        // A „fantasztikus" és a „mentős" viszont nem étel.
        assertEquals("Gulyásleves 400g", summary("fantasztikus vacsora volt: gulyás"));
        assertEquals("Gulyásleves 400g", summary("mentős barátommal ettem egy gulyást"));
    }

    @Test public void measureWordsMultiplyThePortion() {
        assertEquals("Gulyásleves 800g", summary("két tányér gulyás"));
        assertEquals("Méz 40g", summary("két kanál méz"));
        assertEquals("Csokoládé 100g", summary("egy tábla csoki"));
        assertEquals("Csokoládé 50g", summary("fél tábla csokoládé"));
        assertEquals("Chips 25g", summary("fél zacskó chips"));
        assertEquals("Kesudió 15g", summary("fél marék kesudió"));
        assertEquals("Pálinka / tömény 80g", summary("két kupica pálinka"));
        assertEquals("Rizs (főtt) 100g", summary("fél bögre rizs"));
        // A „teáskanál" nem egy csésze tea.
        assertEquals("Cukor 10g", summary("egy teáskanál cukor"));
        // A poharak száma a víznél is szorez – ebből lesz a vízcél-jóváírás.
        assertEquals("Víz / ásványvíz 750g", summary("ittam 3 pohár vizet"));
        assertEquals("Víz / ásványvíz 2000g", summary("reggel óta 8 pohár víz"));
        // Az ivás ital-név nélkül is víz.
        assertEquals("Víz / ásványvíz 1500g", summary("ittam másfél litert"));
        assertEquals("Víz / ásványvíz 2000g", summary("megittam 2 litert"));
        assertEquals("Víz / ásványvíz 300g", summary("ittam 3 dl-t"));
        assertEquals("", summary("ma még nem ittam semmit"));
    }

    @Test public void negationsAndSubstitutionsAreRespected() {
        // Ami nem került a tányérra, az a naplóba se kerül.
        assertEquals("Alma 150g", summary("chips helyett almát ettem"));
        assertEquals("Víz / ásványvíz 250g", summary("sör helyett víz"));
        assertEquals("Méz 20g + Tea (cukrozatlan) 250g",
                summary("cukor helyett méz a teába"));
        assertEquals("", summary("ma nem ettem csokit"));
        assertEquals("", summary("nem ittam kávét ma"));
        assertEquals("Saláta (zöld) 50g",
                summary("kihagytam a tésztát, csak salátát ettem"));
        assertEquals("Kávé (fekete) 200g", summary("csoki nélkül ittam a kávét"));
        assertEquals("Leves (átlag) 400g", summary("tejszín nélkül kértem a levest"));
        // A tagadás csak a saját tagmondatára hat.
        assertEquals("Pizza 300g", summary("ebédre pizza, de nem ettem meg a felét"));
        // A felismerő sor barátságos üzenetéhez.
        assertEquals(true, Foods.looksNegated("ma nem ettem csokit"));
        assertEquals(false, Foods.looksNegated("ebédre gulyásleves"));
    }

    @Test public void asianAndMexicanDishesAreKnown() {
        assertEquals("Tavaszi tekercs / gyoza 150g", summary("gyoza"));
        assertEquals("Wok (zöldséges-húsos) 350g", summary("wok csirke"));
        assertEquals("Pad thai 350g", summary("pad see ew"));
        assertEquals("Savanyúság 100g + Rizs (főtt) 200g", summary("kimchi rizzsel"));
        assertEquals("Csirkemell (sült/grill) 150g", summary("teriyaki csirke"));
        assertEquals("Chilis bab (con carne) 400g", summary("chili con carne"));
        assertEquals("Taco 120g", summary("taco"));
        assertEquals("Sztrapacska 400g", summary("juhtúrós sztrapacska"));
        assertEquals("Nokedli / galuska 200g", summary("knédli"));
    }

    @Test public void moreFruitsAndVegetablesAreKnown() {
        // A „rukkola" nem kóla!
        assertEquals("Saláta (zöld) 50g", summary("rukkola"));
        assertEquals("Mangó 100g", summary("fél mangó"));
        assertEquals("Datolyaszilva 150g", summary("datolyaszilva"));
        assertEquals("Zöldség (vegyes / párolt) 200g", summary("pak choi"));
        assertEquals("Zöldség (vegyes / párolt) 200g", summary("articsóka"));
        assertEquals("Cukkini 200g", summary("patisszon"));
        assertEquals("Bogyós gyümölcs 100g", summary("licsi"));
        // A „mángold" zöldség marad, nem mangó.
        assertEquals("Zöldség (vegyes / párolt) 200g", summary("mángold"));
        // A citrom (és a lime) citromléként számol; a citromfű nem étel.
        // A fél citrom leve fél adag – korábban a „fél" elveszett.
        assertEquals("Citromlé 15g", summary("fél citrom leve"));
        assertEquals("Citromlé 30g", summary("egy lime"));
        assertEquals("Tea (cukrozatlan) 250g", summary("citromfű tea"));
    }

    @Test public void hungarianClassicsResolveToTheRightDish() {
        // A székelygulyás székelykáposzta, nem gulyásleves.
        assertEquals("Székelykáposzta 350g", summary("székelygulyás"));
        // A „borsos tokány" nem borsó!
        assertEquals("Tokány 300g", summary("borsos tokány"));
        assertEquals("Tokány 300g", summary("hentes tokány"));
        assertEquals("Szalontüdő 350g", summary("szalontüdő"));
        assertEquals("Rakott kelkáposzta 400g", summary("kolozsvári káposzta"));
        assertEquals("Leves (átlag) 400g", summary("tarhonyaleves"));
        assertEquals("Zsíros kenyér 100g", summary("velős pirítós"));
        assertEquals("Marhahús 150g", summary("őzgerinc"));
    }

    @Test public void verbifiedFoodFormsAreUnderstood() {
        // A „pizzáztunk", „fagyiztunk", „sütiztünk" igésített alakok is étel.
        assertEquals("Pizza 300g", summary("pizzáztunk este"));
        assertEquals("Fagylalt 100g", summary("fagyiztunk a parton"));
        assertEquals("Sütemény 100g", summary("sütiztünk a nagyinál"));
        assertEquals("Sütemény 100g", summary("ettem egy sütit"));
        assertEquals("Lángos 200g", summary("lángosoztunk a strandon"));
        // Az étkezés-igék viszont továbbra is csak címkék.
        assertEquals("", summary("vacsoráztunk"));
        assertEquals("", summary("ebédeltünk a városban"));
    }

    @Test public void supplementsAndProteinDishesAreOneItem() {
        // A „whey protein" a POR – nem por MELLETT egy kész turmix.
        assertEquals("Fehérjepor 30g", summary("whey protein 30 g"));
        assertEquals("Protein turmix 300g", summary("kazein turmix"));
        // A fehérjés változatok EGY fogások, nem turmix + tészta.
        assertEquals("Palacsinta 150g", summary("protein palacsinta"));
        assertEquals("Puding 200g", summary("protein puding"));
        assertEquals("Gofri 100g", summary("protein gofri"));
        // A kapszulák-porok nulla kalóriás étrend-kiegészítők.
        assertEquals("Étrend-kiegészítő 5g", summary("kreatin 5 g"));
        assertEquals("Étrend-kiegészítő 5g", summary("magnézium tabletta"));
        assertEquals("Sportital / izotóniás 500g", summary("elektrolit ital"));
        // Növényi italok.
        assertEquals("Növényi tej (mandula/zab) 250g", summary("zabital"));
    }

    @Test public void mayonnaiseColdDishesAreNotGreenSalad() {
        // Az „orosz hússaláta" a majonéz miatt háromszor annyi kalória, mint
        // egy zöldsaláta – nem oda tartozik.
        assertEquals("Franciasaláta / coleslaw 150g", summary("orosz hússaláta"));
        assertEquals("Franciasaláta / coleslaw 150g", summary("tojássaláta"));
        assertEquals("Franciasaláta / coleslaw 150g", summary("kaszinótojás"));
        // A rákkoktél tenger gyümölcse, nem koktél.
        assertEquals("Tenger gyümölcsei 150g", summary("rákkoktél"));
        // Egy fogás, nem kettő.
        assertEquals("Rakott zöldbab 350g", summary("rakott zöldbab"));
        assertEquals("Keksz 40g", summary("zabpelyhes keksz"));
        assertEquals("Szósz / mártás 30g", summary("sajtmártás"));
    }

    @Test public void nothingIsInventedFromMealWords() {
        // Az étkezés-címkék magukban nem ételek.
        for (String q : new String[]{"reggeli", "ebédre", "vacsorára", "uzsonnára", "kaja"}) {
            List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
            assertEquals("étel lett a címkéből: " + q, 0, hs.size());
        }
    }

    @Test public void commonWordsHidingFoodStemsAreNotFoods() {
        // A „vajon" nem vaj, az „első sorban" nem sör, a „sajtótájékoztató"
        // nem sajt – a gyakori álca-szavak ki vannak maszkolva.
        for (String q : new String[]{"első sorban ez fontos", "sorban álltam",
                "borzasztó nap volt", "a laborban dolgoztam", "táborban voltunk",
                "vajon mi lesz", "hallottam egy jó hírt", "halkan beszélt",
                "sajtótájékoztató", "paradicsomi állapotok", "narancssárga póló",
                "kolbászolás a városban", "rumli van otthon",
                "sorozatot néztem és haladtam a munkával"}) {
            List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
            assertEquals("étel lett belőle: " + q, 0, hs.size());
        }
        // Az átvitt értelmű összetételek sem ételek.
        for (String q : new String[]{"borsos ár volt", "narancsbőr ellen edzek",
                "sörhas ellen gyúrok", "kávészünet", "uborkaszezon van",
                "tejszínű ég", "almafa virágzik", "diófa asztal",
                "tortaformát vettem", "banánköztársaság"}) {
            List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
            assertEquals("étel lett belőle: " + q, 0, hs.size());
        }
        assertEquals("Tokány", Foods.parse(Arrays.asList(Foods.ALL),
                "borsos tokány").get(0).food.name);
        assertEquals("Tejszínhab", Foods.parse(Arrays.asList(Foods.ALL),
                "tejszínhab a kakaóra").get(0).food.name);
        // A ragozott IGAZI ételek viszont élnek.
        assertEquals("Sör", Foods.parse(Arrays.asList(Foods.ALL),
                "sört ittam").get(0).food.name);
        assertEquals("Vaj", Foods.parse(Arrays.asList(Foods.ALL),
                "vajas kenyér").get(0).food.name);
        assertEquals("Sajt (trappista)", Foods.parse(Arrays.asList(Foods.ALL),
                "sajtos szendvics").get(0).food.name);
        assertEquals("Hal (fehér)", Foods.parse(Arrays.asList(Foods.ALL),
                "halat sütöttem").get(0).food.name);
    }
    @Test public void aFinishedDishDoesNotAlsoCountItsBase() {
        // A „bolognai tészta" korábban két találat volt – a fogás ÉS a főtt
        // tészta –, vagyis egy tányér makaróniból 950 kalória lett.
        assertEquals("Bolognai spagetti 350g", summary("bolognai tészta"));
        assertEquals("Tészta carbonara 350g", summary("carbonara tészta"));
        assertEquals("Milánói makaróni 350g", summary("milánói tészta"));
        assertEquals("Túrós csusza 300g", summary("túrós tészta"));
        assertEquals("Csirkés saláta 300g", summary("csirkés saláta"));
        assertEquals("Krumplisaláta 200g", summary("krumpli saláta"));
        assertEquals("Rizses hús 350g", summary("rizses hús"));
        // Ahol az alap TÉNYLEG külön tétel, ott megmarad.
        assertEquals("Rántott hús (sertés) 180g + Rizs (főtt) 200g",
                summary("rántott hús rizzsel"));
        assertEquals("Zöldség (vegyes / párolt) 200g + Rizs (főtt) 200g",
                summary("zöldséges rizs"));
        assertEquals("Darált hús 150g + Tészta (főtt) 250g",
                summary("darált húsos tészta"));
    }

    @Test public void theNewlyAddedDishesAreFound() {
        assertEquals("Vaníliás karika 70g", summary("vaníliás karika"));
        assertEquals("Lekváros bukta 100g", summary("lekváros bukta"));
        assertEquals("Sajttorta 120g", summary("egy szelet sajttorta"));
        assertEquals("Kínai bundás csirke 250g", summary("kínai csirke bundában szechuan"));
        assertEquals("Sült krumpli 150g", summary("sültkrumpli"));
    }
    @Test public void aMeasureWordSurvivesACompoundFoodName() {
        // A csokoládé-tő az „étcsoki" közepén van, így a mérőszó mögött egy
        // szótöredék maradt, és a fél tábla elveszett: a bejegyzés a tipikus
        // adaggal, vagyis feleannyival ment tovább.
        assertEquals("Csokoládé 50g", summary("fél tábla étcsoki"));
        assertEquals("Csokoládé 100g", summary("egy tábla étcsoki"));
        assertEquals("Csokoládé 200g", summary("két tábla tejcsoki"));
        // Ami valóban más szó, azt továbbra sem fogadjuk el mérőszónak.
        assertEquals("Leves (átlag) 800g + Alma 150g", summary("2 tányér leves után alma"));
        assertEquals("Kávé (fekete) 200g", summary("duplaespresszó"));
    }

    @Test public void pizzaIsEatenBySlice() {
        // Két szelet pizza eddig egy egész pizzának számított.
        assertEquals("Pizza 200g", summary("két szelet pizza"));
        assertEquals("Pizza 100g", summary("egy szelet pizza"));
        // A fél pizza viszont tényleg fél pizza, nem fél szelet.
        assertEquals("Pizza 300g", summary("egy egész pizza"));
    }
    @Test public void theDishesFromTheLatestProbeAreFound() {
        assertEquals("Tepsis csirke 300g", summary("tepsis csirke"));
        assertEquals("Tepsis csirke 300g", summary("egészben sült csirke"));
        assertEquals("Kalács / bejgli 80g", summary("mákos tekercs"));
        // A töltelék benne van a kalóriában: ne számoljon még egy adag diót is.
        assertEquals("Kalács / bejgli 80g", summary("diós bejgli szelet"));
    }

    @Test public void aCountBeforeAFoodIsNotLost() {
        // Aminek nincs természetes darabmérete, ott eddig egyszerűen elveszett
        // a számláló: a „két kebab" egyetlen kebabnak számított. Ez 267 ételt
        // érintett – vagyis az adatbázis négyötödét.
        assertEquals("Kebab 700g", summary("két kebab"));
        assertEquals("Kebab 175g", summary("fél kebab"));
        assertEquals("Csirkés wrap 500g", summary("két csirkés wrap"));
        assertEquals("Almás pite 240g", summary("két almás pite"));
        assertEquals("Gyümölcslé 500g", summary("két pohár narancslé"));
        // A szótő a szó BELSEJÉBEN is lehet: a „görögdinnye" dinnye-töve elé
        // került a „görög", és a kettes elveszett.
        assertEquals("Görögdinnye 600g", summary("két görögdinnye"));
        assertEquals("Csirkenugget 300g", summary("két csirkenugget"));
    }

    @Test public void aSizeAdjectiveDoesNotSwallowTheCount() {
        assertEquals("Alma 300g", summary("2 nagy alma"));
        assertEquals("Körte 300g", summary("két szép körte"));
        assertEquals("Tojás 165g", summary("3 közepes tojás"));
        // A mérőszó a jelző mögött is érvényes.
        assertEquals("Tejföl 60g", summary("két nagy kanál tejföl"));
        assertEquals("Leves (átlag) 800g", summary("2 nagy tányér leves"));
    }

    @Test public void theBareWordChickenIsFood() {
        // A „csirke" önmagában eddig semmi volt: a „csirke rizzsel" fél
        // ebédnek látszott, mert csak a rizs került a naplóba.
        assertEquals("Csirkemell (sült/grill) 150g + Rizs (főtt) 200g",
                summary("csirke rizzsel"));
        assertEquals("Csirkemell (sült/grill) 150g", summary("csirkét ettem"));
        // A hosszabb név mindig erősebb: a jelzős alakok nem sérülnek.
        assertEquals("Rántott csirkemell 180g", summary("rántott csirke"));
        assertEquals("Tepsis csirke 300g", summary("tepsis csirke"));
        assertEquals("Csirkés saláta 300g", summary("csirkés saláta"));
        assertEquals("Kínai bundás csirke 250g", summary("kínai csirke"));
        assertEquals("Csirkemáj 120g", summary("csirkemáj"));
        assertEquals("Csirkenugget 150g", summary("csirkenugget"));
    }

    @Test public void chickenIsAnAdjectiveInMeatDishes() {
        // A „csirke curry" egy tál curry, nem curry PLUSZ egy csirkemell.
        assertEquals("Curry 300g", summary("csirke curry"));
        assertEquals("Wok (zöldséges-húsos) 350g", summary("wok csirke"));
        assertEquals("Gyros 350g", summary("csirkés gyros"));
        // A leves teljes alakja szótő, nem sült csirkemell.
        assertEquals("Húsleves 400g", summary("csirkeleves"));
    }

    @Test public void aHyphenBetweenAdjectivesSeparatesIngredients() {
        // Magyarul a „sonkás-sajtos" két hozzávaló. Eddig egy szónak számított,
        // és a nehezebbik étel elnyomta a másikat: eltűnt a sonka.
        assertEquals("Sonka 50g + Sajt (trappista) 30g + Szendvics 150g",
                summary("sonkás-sajtos szendvics"));
        assertEquals("Sonka 50g + Sajt (trappista) 30g + Szendvics 150g",
                summary("sonkás sajtos szendvics"));
        assertEquals("Tejföl 30g + Rakott krumpli 350g",
                summary("húsos-tejfölös rakott krumpli"));
        // Ahol a kötőjel egy nevet tagol, ott marad egy étel.
        assertEquals("Túró rudi 51g", summary("túró-rudi"));
        assertEquals("Hot-dog 150g", summary("hot-dog"));
        // A kötőjel nélküli összetétel is egy étel marad.
        assertEquals("Csirkemell (sült/grill) 150g", summary("csirkemellsaláta"));
    }

    @Test public void theFlatbreadIsFoodOnItsOwn() {
        // A pita eddig nem létezett: a köret kalóriája elveszett.
        assertEquals("Pita / lepénykenyér 80g", summary("pita"));
        assertEquals("Hummusz 60g + Pita / lepénykenyér 80g", summary("humusz pitával"));
        // A gyros adagja viszont a lepényt is tartalmazza.
        assertEquals("Gyros 350g", summary("gyros pitában"));
        // A növényi burger nem marhahúsos burger.
        assertEquals("Vega burger 220g", summary("vega burger"));
        assertEquals("Vega burger 220g", summary("vegán burger"));
        assertEquals("Hamburger 250g", summary("hamburger"));
    }

    @Test public void theNegationDoesNotEatTheAccompaniment() {
        // A „nem kértem sültkrumplit a hamburger mellé" hamburgerét megette az
        // ember – csak a köretet hagyta el. Eddig a tagadás az egész
        // tagmondatot elvitte, vagyis egy valódi, 700 kcal-s fogást törölt.
        assertEquals("Hamburger 250g", summary("nem kértem sültkrumplit a hamburger mellé"));
        assertEquals("Pizza 300g", summary("nem ettem salátát a pizza mellett"));
        // Kísérő-jelző nélkül a tagadás továbbra is mindent elvisz.
        assertEquals("", summary("nem kértem sültkrumplit"));
    }

    @Test public void theNegationReachesAcrossAnAnd() {
        // Az „és" nem határ: a „nem ettem csokit és chipset" mindkét tételt
        // tagadja. A tagmondat-felosztás a chipset külön tagmondatba tette,
        // és így bekerült a naplóba.
        assertEquals("", summary("ma nem ettem csokit és chipset"));
        // Az ÍRÁSJEL viszont határ.
        assertEquals("Kávé (fekete) 200g", summary("nem ettem semmit, de ittam kávét"));
        assertEquals("Alma 150g", summary("nem ittam kávét, de ettem egy almát"));
        // És egy ÁLLÍTÓ ige is lezárja a tagadást.
        assertEquals("Kávé (fekete) 200g",
                summary("nem ettem reggelit és ittam egy kávét"));
    }

    @Test public void theClearBrothIsNotAnAverageSoup() {
        // Az erőleves tiszta húsleves: az „átlagos" leves ötszörös kalóriát
        // írt rá (200 helyett 40 kcal/100 g).
        assertEquals("Húsleves 400g", summary("erőleves"));
        assertEquals("Húsleves 400g", summary("csontleves"));
        // A gombaleves tejfölös, nem víztiszta – és nem gomba + leves.
        assertEquals("Krémleves (zöldség) 350g", summary("gombaleves"));
        assertEquals("Krémleves (zöldség) 350g", summary("gomba leves"));
        assertEquals("Krémleves (zöldség) 350g", summary("karfiolleves"));
        // A többi leves változatlan.
        assertEquals("Gulyásleves 400g", summary("gulyásleves"));
        assertEquals("Leves (átlag) 400g", summary("leves"));
        assertEquals("Bableves 400g", summary("bableves"));
    }

    @Test public void theShortStemsDoNotCatchEverydayWords() {
        // A szótő-illesztés szó belsejében is talál, ezért a rövid, ütköző
        // alakok szándékosan hiányoznak a tövek közül. A „koleszban ettem"
        // nem köles, az „irodában ettem" nem író.
        assertEquals("Szendvics 150g", summary("koleszban ettem egy szendvicset"));
        assertEquals("", summary("koleszterin"));
        assertEquals("Alma 150g", summary("irodában ettem egy almát"));
        // A ragozott alakok viszont egyértelműek.
        assertEquals("Hajdina / köles (főtt) 200g", summary("kölest ettem"));
        assertEquals("Hajdina / köles (főtt) 200g", summary("kölessel"));
    }

    @Test public void thePlantBasedItemsAreNotTheirAnimalCounterparts() {
        // A növényi sajt fehérjéje töredéke a trappistáénak.
        assertEquals("Növényi sajt 30g", summary("vegán sajt"));
        assertEquals("Sajt (trappista) 30g", summary("sajt"));
        // Tengeri alga és miszó: kalóriában a könnyű végén.
        assertEquals("Savanyúság 100g", summary("wakame"));
        assertEquals("Húsleves 400g", summary("miso leves"));
    }

    @Test public void oneStemApartCanMeanHalfAgainTheCalories() {
        // A „bécsi virsli" NEM bécsi szelet: a „bécsi" a rántott hús szótöve.
        assertEquals("Virsli 100g", summary("bécsi virsli"));
        assertEquals("Rántott hús (sertés) 180g", summary("bécsi szelet"));
        // A napraforgóOLAJ 900 kcal, a napraforgóMAG 580.
        assertEquals("Olaj 10g", summary("napraforgóolaj"));
        assertEquals("Tökmag / napraforgómag 30g", summary("napraforgómag"));
        // A zsemlemorzsa egy kanálnyi panír, nem egy egész zsemle.
        assertEquals("Liszt 30g", summary("zsemlemorzsa"));
        assertEquals("Zsemle 55g", summary("zsemle"));
    }

    @Test public void theHungarianSausagesAreSausages() {
        assertEquals("Kolbász 100g", summary("gyulai"));
        assertEquals("Kolbász 100g", summary("csabai"));
        assertEquals("Kolbász 100g", summary("debreceni"));
        // A zsír az olajjal egy súlycsoport (900 kcal/100 g).
        assertEquals("Olaj 10g", summary("sertészsír"));
        // A zsíros kenyérnek viszont saját tétele van.
        assertEquals("Zsíros kenyér 100g", summary("zsíros kenyér"));
    }

    @Test public void aChocolateBiscuitIsOneBiscuit() {
        // A „csokis keksz" EGY süti: enélkül a csoki és a keksz külön
        // tételként, kétszeres kalóriával került a naplóba.
        assertEquals("Keksz 40g", summary("csokis keksz"));
        assertEquals("Csokoládé 25g", summary("csoki"));
        assertEquals("Keksz 40g", summary("háztartási keksz"));
    }

    @Test public void theMissingSweetsAreThereNow() {
        assertEquals("Kalács / bejgli 80g", summary("beigli"));
        assertEquals("Sütemény 100g", summary("eszterházy"));
        assertEquals("Sütemény 100g", summary("dobostorta"));
        assertEquals("Ropi / kréker 30g", summary("sós rúd"));
        assertEquals("Gumicukor / cukorka 30g", summary("nyalóka"));
        // A zabszelet müzliszelet, nem egy tál zabpehely.
        assertEquals("Müzliszelet 30g", summary("zabszelet"));
    }

    @Test public void thePizzaToppingIsInThePizza() {
        // A „négy sajtos pizza" egy pizza, nem pizza PLUSZ egy adag sajt –
        // az utóbbi 1212 kcal-t írt egy 780-as helyett.
        assertEquals("Pizza 300g", summary("négy sajtos pizza"));
        assertEquals("Pizza 300g", summary("sonkás pizza"));
        assertEquals("Pizza 300g", summary("szalámis pizza"));
        assertEquals("Pizza 300g", summary("gombás pizza"));
        // Pizza nélkül a feltét önálló étel marad.
        assertEquals("Sajt (trappista) 30g", summary("sajt"));
        assertEquals("Sonka 50g", summary("sonka"));
    }

    @Test public void theWrapIsNotPasta() {
        // A puszta „durum" a kebabos tekercs: tésztaként a kalória harmada
        // veszett el. A teljes „durum tészta" viszont tészta marad.
        assertEquals("Kebab 350g", summary("durum"));
        assertEquals("Tészta (főtt) 250g", summary("durum tészta"));
        assertEquals("Tészta (főtt) 250g", summary("tészta"));
        // Sushi-változatok.
        assertEquals("Sushi 250g", summary("maki"));
        assertEquals("Sushi 250g", summary("nigiri"));
        assertEquals("Sushi 250g", summary("sashimi"));
    }

    @Test public void theMissingProduceIsThereNow() {
        // A grapefruit a fogyókúrás reggelik klasszikusa, a sütőtök az őszi
        // konyha alapja – egyik sem létezett.
        assertEquals("Grapefruit 200g", summary("grapefruit"));
        assertEquals("Sütőtök 200g", summary("sütőtök"));
        // A rokon tételek nem sérültek: a tökmag mag, a tökfőzelék főzelék,
        // a sütőtökkrémleves krémleves.
        assertEquals("Tökmag / napraforgómag 30g", summary("tökmag"));
        assertEquals("Tökfőzelék 350g", summary("tökfőzelék"));
        assertEquals("Krémleves (zöldség) 350g", summary("sütőtök krémleves"));
    }
    /**
     * A magyar „-almas" melléknevek MIND tartalmazzák az almát.
     *
     * A „hatalmas" ráadásul a saját méret-jelzőink között is szerepel, tehát
     * a „hatalmas adag rizs" mondathoz járt egy fantom alma – nyolcvan
     * kalória, minden alkalommal, csendben.
     */
    @Test public void adjectivesHidingAnAppleAreNotApples() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"unalmas nap", "alkalmas", "fájdalmas edzés után",
                "nyugalmas vacsora", "álmatlanság", "parkolás", "halogatás",
                "borostyán", "sörény", "figyelmeztetés", "memorizálás"})
            assertTrue(q + " -> " + Foods.parse(all, q),
                    Foods.parse(all, q).isEmpty());
        // A méret-jelző nem viheti el az ételt, és fantomot sem tehet mellé.
        java.util.List<Foods.Hit> h = Foods.parse(all, "hatalmas adag rizs");
        assertEquals(1, h.size());
        assertEquals("Rizs (főtt)", h.get(0).food.name);
        // Az alma magától továbbra is alma.
        assertEquals("Alma", Foods.parse(all, "hatalmas alma").get(0).food.name);
        assertEquals("Alma", Foods.parse(all, "vadalma").get(0).food.name);
    }

    /** A rizling bor, nem rizs. */
    @Test public void aRieslingIsWineNotRice() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals("Bor (vörös/fehér)",
                Foods.parse(all, "egy pohár rizling").get(0).food.name);
        assertEquals("Rizs (főtt)", Foods.parse(all, "rizs").get(0).food.name);
    }
}
