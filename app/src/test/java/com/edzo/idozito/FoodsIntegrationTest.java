package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

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
        assertEquals("Saláta (zöld) 50g", summary("sali csirkével"));
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
        assertEquals("Rizottó 350g + Gomba 100g", summary("risotto gombával"));
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

    @Test public void nothingIsInventedFromMealWords() {
        // Az étkezés-címkék magukban nem ételek.
        for (String q : new String[]{"reggeli", "ebédre", "vacsorára", "uzsonnára", "kaja"}) {
            List<Foods.Hit> hs = Foods.parse(Arrays.asList(Foods.ALL), q);
            assertEquals("étel lett a címkéből: " + q, 0, hs.size());
        }
    }
}
