package com.edzo.idozito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Az étel-adatbázis belső összhangja.
 *
 * Ezek a számok kézzel kerülnek be, tételenként – és egy elütött érték nem
 * fordítási hiba, hanem hamis kalória a naplóban. A szabályok szándékosan
 * lazák: nem tápanyag-táblázatot ellenőrzünk, hanem a nagyságrendi és fizikai
 * képtelenségeket fogjuk meg.
 */
public class FoodsDataQualityTest {

    @Test public void everyFoodIsPhysicallyPossible() {
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            // A tiszta olaj 900 kcal/100 g – ennél sűrűbb étel nincs.
            if (f.kcal100 < 0 || f.kcal100 > 900)
                bad.append("\n  ").append(f.name).append(": kcal100=").append(f.kcal100);
            // A legfehérjésebb tétel a fehérjepor (75 g/100 g).
            if (f.prot100 < 0 || f.prot100 > 90)
                bad.append("\n  ").append(f.name).append(": prot100=").append(f.prot100);
            // Az adag 5 g (egy kockacukornyi) és fél kiló között életszerű.
            if (f.portion < 5 || f.portion > 500)
                bad.append("\n  ").append(f.name).append(": portion=").append(f.portion);
            // 1 g fehérje = 4 kcal: a fehérje energiája nem lépheti túl az
            // összeset. (Kis tűrés a kerekített értékek miatt.)
            if (f.kcal100 > 0 && f.prot100 * 4 > f.kcal100 + 8)
                bad.append("\n  ").append(f.name).append(": ")
                   .append(Math.round(f.prot100 * 4)).append(" kcal fehérje ")
                   .append(f.kcal100).append(" kcal ételben");
        }
        assertTrue("képtelen adatok:" + bad, bad.length() == 0);
    }

    @Test public void noNameOrStemAppearsTwice() {
        // Két azonos nevű tétel közül a második soha nem érhető el a
        // felismerésből, a táblázatban viszont ott van – csendes kettősség.
        // A közös szótő ugyanígy: az egyik étel elnyeli a másikat.
        java.util.HashMap<String, String> names = new java.util.HashMap<>();
        java.util.HashMap<String, String> stems = new java.util.HashMap<>();
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            String key = Foods.norm(f.name);
            if (names.containsKey(key)) bad.append("\n  dupla név: ").append(f.name);
            else names.put(key, f.name);
            for (String s : f.stems) {
                String ns = Foods.norm(s);
                if (ns.isEmpty()) continue;
                String prev = stems.get(ns);
                // Ékezetes és ékezet nélküli írásmód ugyanarra a tételre:
                // felesleges, de nem hiba – csak a MÁS ételé az.
                if (prev != null && !prev.equals(f.name))
                    bad.append("\n  közös szótő \"").append(ns).append("\": ")
                       .append(prev).append(" / ").append(f.name);
                else if (prev != null)
                    bad.append("\n  felesleges szótő \"").append(ns).append("\": ").append(f.name);
                else stems.put(ns, f.name);
            }
        }
        assertTrue("kettősségek:" + bad, bad.length() == 0);
    }

    @Test public void everyFoodHasANameAndAStem() {
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            if (f.name == null || f.name.trim().isEmpty())
                bad.append("\n  névtelen tétel");
            boolean hasStem = false;
            for (String s : f.stems) if (s != null && !s.trim().isEmpty()) hasStem = true;
            if (!hasStem) bad.append("\n  ").append(f.name).append(": nincs szótöve");
        }
        assertTrue("hiányos tételek:" + bad, bad.length() == 0);
    }
    @Test public void everySliceMeasuredFoodExists() {
        // A szelet-tábla nevekre hivatkozik: egy átnevezés csendben kiütné.
        for (String[] row : Foods.SLICE_GRAMS) {
            boolean found = false;
            for (Foods.Food f : Foods.ALL) if (f.name.equals(row[0])) { found = true; break; }
            assertTrue("nincs ilyen étel: " + row[0], found);
            int g = Integer.parseInt(row[1]);
            assertTrue("életszerűtlen szelet: " + row[0], g >= 20 && g <= 300);
        }
    }

    /**
     * Minden szótő ÖNMAGÁRA esik – és csak arra.
     *
     * Ez a rövid tövek csapdája: a „chia" a macchiato közepén, a „rizs" a
     * párizsiban. Ilyenkor a bejegyzés csendben létrejön, csak épp más
     * ételről vagy egy fölös adaggal. Egy új szótő pontosan ezt szokta
     * elrontani, ezért a teljes szótő-készletet átfuttatjuk.
     */
    @Test public void everyStemMeansExactlyItsOwnFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL)
            for (String st : f.stems) {
                java.util.List<Foods.Hit> h = Foods.parse(all, st);
                if (h.size() == 1 && h.get(0).food.name.equals(f.name)) continue;
                bad.append("\n  ").append(f.name).append(" / \"").append(st)
                   .append("\" -> ").append(names(h));
            }
        assertEquals("ütköző szótő:" + bad, 0, bad.length());
    }

    /**
     * Ugyanaz KÉT szótővel egymás mellett: „párizsi felvágott".
     *
     * Ételenként csak a leghosszabb szótő helyét jegyezzük meg. A párizsi a
     * hosszabb „felvágott" tövön került be, és a szó elején álló „párizsi"
     * szabadon hagyta a benne rejlő „rizs"-t: kétszáz gramm rizs a felvágott
     * mellé. Egyetlen tő önmagában nem hozta elő – csak a párja.
     */
    @Test public void twoStemsOfTheSameFoodStillMeanOneFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL)
            for (int i = 0; i < f.stems.length; i++)
                for (int j = 0; j < f.stems.length; j++) {
                    if (i == j) continue;
                    String q = f.stems[i] + " " + f.stems[j];
                    java.util.List<Foods.Hit> h = Foods.parse(all, q);
                    if (h.size() == 1 && h.get(0).food.name.equals(f.name)) continue;
                    bad.append("\n  ").append(f.name).append(" / \"").append(q)
                       .append("\" -> ").append(names(h));
                }
        assertEquals("ütköző szótőpár:" + bad, 0, bad.length());
    }

    /**
     * A SÖR és a SOR: ékezet nélkül ugyanaz a szó.
     *
     * A magyar bőven gyárt „-sor" végű összetételeket (névsor, címsor,
     * gyakorlatsor, munkasorozat), és mindegyik sört írt a naplóba. Tiltólista
     * nem old meg egy végtelen szóosztályt – szó belsejében ezért az ékezet
     * dönt. Szó elején viszont marad a régi viselkedés: aki ékezet nélkül
     * gépel, attól nem vesszük el a sörét.
     */
    @Test public void beerNeedsItsAccentInsideAWord() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[] beer = {"két korsó sör", "sört ittam", "sör", "búzasör", "barna sör",
                "alkoholmentes sör", "2 üveg sör", "két korso sor"};
        for (String q : beer)
            assertEquals("elveszett a sör: " + q, "Sör",
                    Foods.parse(all, q).isEmpty() ? "—" : Foods.parse(all, q).get(0).food.name);
        String[] notBeer = {"névsora", "gyakorlatsorok", "címsorban", "fejsor", "csipsor",
                "munkasorozatokra", "3 sorozat 10 fekvenyomás", "egysoros",
                "ábécésorrendben", "sorból", "soronként", "sorompó", "idősor"};
        for (String q : notBeer)
            assertTrue("sör lett belőle: " + q, Foods.parse(all, q).isEmpty());
    }

    /**
     * Ékezetes ütközések: MÉZ/mező, KÁVÉ/falkavezér, KÓLA/csonkolás,
     * TEJ/estéjéről, BOR/bőrrel, FOGAS/fogás.
     *
     * Ugyanaz a fajta hiba, mint a sörnél, csak más betűvel: ékezet nélkül a
     * hétköznapi szó és az étel neve egybeesik. A jobb oldalon mindig az áll,
     * amit eddig naplózott.
     */
    @Test public void accentTellsTheFoodFromTheEverydayWord() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[] notFood = {"adatmezők", "falkavezér", "csonkolás", "estéjéről", "bőrrel",
                "fogás", "fogások", "fogásnak", "beszédbuborék", "szappanbuborék",
                "főképernyő", "kezdőképernyő", "domborítsd", "buktató", "ellenőrizzük",
                "fogalmaz", "durumbúza", "szeretek futni", "szeretem", "stádió",
                "szokásaim", "levesszük", "hüvelykujjszabály", "vízilabda",
                // A szósöprés mai fogásai: a pihenőNAPOKÉhoz szóban a poke
                // bowl, a kétOLDALASokat szóban a sült oldalas lapult.
                "pihenőnapokéhoz", "pihenőnapokat", "kétoldalasokat",
                // A „zsírmentes" jelző a ZSÍR tövével kezdődik: eddig száz
                // gramm olajat írt a túró mellé.
                "zsírmentes"};
        for (String q : notFood)
            assertTrue("ételt talált benne: " + q + " -> " + names(Foods.parse(all, q)),
                    Foods.parse(all, q).isEmpty());
        // A valódi tételek viszont maradnak – ékezet nélkül gépelve is.
        String[] food = {"méz", "mez", "kávé", "kave", "kóla", "kola", "tej", "bor",
                "fogas", "vörösbor", "tejföl", "mézes", "fogassal",
                "poke bowl", "poke tál", "sült oldalas", "oldalast ettem",
                // A jelző maszkja nem viheti el a mellette álló ételt – és a
                // zsírszegény tejnek saját tétele van.
                "zsírszegény tej", "zsírmentes túró", "zsír", "olaj"};
        for (String q : food)
            assertTrue("elveszett az étel: " + q, !Foods.parse(all, q).isEmpty());
    }

    /**
     * Átfedő szótöveknél a HOSSZABB dönt, nem a súlyosabb étel.
     *
     * Egy szón belül általában a súlyosabb tétel nyer (a „csirkemellsalátá"-ban
     * a csirkemell, nem a saláta). Ha viszont a két találat ugyanazokon a
     * betűkön osztozik, akkor nem két összetevőről van szó, hanem egyetlen
     * szóról: az „almáját" eddig csirkemájat naplózott.
     */
    @Test public void overlappingStemsPreferTheLongerOne() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[][] cases = {{"almáját", "Alma"}, {"almája", "Alma"},
                {"megettem az almáját", "Alma"}, {"tejföl", "Tejföl"},
                {"csirkemáj", "Csirkemáj"}, {"gránátalma", "Gránátalma"}};
        for (String[] c : cases) {
            java.util.List<Foods.Hit> h = Foods.parse(all, c[0]);
            assertEquals(c[0] + " -> " + names(h), 1, h.size());
            assertEquals(c[0], c[1], h.get(0).food.name);
        }
    }

    /**
     * „Egy sor csoki”: a mértékszó nem étel.
     *
     * Az ékezet nélkül írt tő közvetlenül egy másik étel előtt a mennyiséget
     * mondja meg, nem egy második fogást – eddig egy fél liter sör került a
     * csokoládé mellé. Ha viszont a mondatban tényleg van sör, azt nem
     * veszítjük el: a találat odébb kerül.
     */
    @Test public void aMeasureWordIsNotAFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals("Csokoládé", names(Foods.parse(all, "egy sor csoki")));
        assertEquals("Csokoládé", names(Foods.parse(all, "két sor csokoládé")));
        assertEquals("Csokoládé, Sör", names(Foods.parse(all, "egy sor csoki és egy sör")));
        // A magyar hangrend ékezet nélkül is elárulja a SORT: a „sör" magas
        // hangrendű (sörnek, sörrel, sörnél), a „sor" mély (sornak, sorral,
        // sornál). Ahol a rag magánhangzója eltér, ott nincs kétség.
        for (String q : new String[]{"sornak", "sorral", "sornál", "soraban"})
            assertEquals(q, "—", names(Foods.parse(all, q)));
        for (String q : new String[]{"sörnek", "sörrel", "sörnél"})
            assertEquals(q, "Sör", names(Foods.parse(all, q)));
        // A sör magában marad sör – ékezet nélkül is.
        assertEquals("Sör", names(Foods.parse(all, "sor")));
        assertEquals("Sör", names(Foods.parse(all, "sört ittam")));
        // És a sor mint darabszó szoroz: két sor kétszer annyi.
        assertEquals(2 * Foods.parse(all, "egy sor csoki").get(0).grams,
                Foods.parse(all, "két sor csoki").get(0).grams, 0.01);
    }

    /**
     * A mértékszó-szabály csak a „sor"-ra vonatkozik.
     *
     * A szabály régen minden ékezettel megkülönböztetett tőre lecsapott, ha
     * közvetlenül utána étel állt – így a „kávé tejjel" beírásából eltűnt a
     * kávé, és a „méz" is elszállt volna egy „méz banánnal"-ból. Mértékszó
     * viszont csak a sor: kávét senki nem tesz mennyiségnek egy étel elé.
     */
    @Test public void onlyRowIsAMeasureWord() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals("Kávé (fekete), Tej", names(Foods.parse(all, "kave tejjel")));
        assertEquals("Kávé (fekete), Cukor", names(Foods.parse(all, "kave cukorral")));
        assertEquals("Méz, Banán", names(Foods.parse(all, "mez banannal")));
        assertEquals("Üdítő (cukros), Pizza", names(Foods.parse(all, "kola pizzaval")));
    }

    /**
     * Fajta + gyűjtőnév egy étel: „feta sajt".
     *
     * A magyar a fajtát a gyűjtőnév elé teszi, és a kettő ugyanaz a falat.
     * A naplóba eddig két tétel ment be – egy adag feta ÉS egy adag
     * trappista –, vagyis a felismerés maga adott hozzá vagy nyolcvan
     * kalóriát. Ahol viszont tényleg két étel van („sonka sajt"), ott
     * marad mind a kettő.
     */
    @Test public void aKindBeforeTheGenericNameIsOneFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        assertEquals("Feta", names(Foods.parse(all, "feta sajt")));
        assertEquals("Feta", names(Foods.parse(all, "feta sajttal")));
        assertEquals("Mozzarella", names(Foods.parse(all, "mozzarella sajt")));
        assertEquals("Parmezán", names(Foods.parse(all, "parmezán sajt")));
        assertEquals("Ricotta", names(Foods.parse(all, "ricotta sajt")));
        // Két külön étel marad kettő.
        assertEquals("Sonka, Sajt (trappista)", names(Foods.parse(all, "sonka sajt")));
        // Aminek nincs saját sora, az marad a gyűjtőnévnél.
        assertEquals("Sajt (trappista)", names(Foods.parse(all, "cheddar sajt")));
    }

    /**
     * Az „alma" a szó BELSEJÉBEN szinte sosem alma.
     *
     * A magyar „-alom" végű főnevek ragozva mind ALMÁ-vá válnak: fájdALMAt,
     * birodALMAt, jutALMAt – és a hALMAz is ilyen. A szókezdet-vizsgálat
     * egyiket sem fogta meg, tehát minden ilyen mondathoz járt egy fantom
     * alma, nyolcvan kalória. A rehab-oldal érkezésével ez különösen fájt:
     * a „fájdalmat érzek" mondat most már mindennapos.
     */
    @Test public void anAppleInsideAWordIsNotAnApple() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"fájdalmat okoz", "erős fájdalmat érzek a vállamban",
                "birodalmat épített", "jutalmat kaptam", "sokadalmat láttam",
                "nyugalmat találtam", "a halmaza", "alkalmat keresek",
                "diadalmat aratott"})
            assertEquals(q, "—", names(Foods.parse(all, q)));
        // A valódi alma és az összetett gyümölcsnevek érintetlenek.
        assertEquals("Alma", names(Foods.parse(all, "almát ettem")));
        assertEquals("Alma", names(Foods.parse(all, "hatalmas alma")));
        assertEquals("Gránátalma", names(Foods.parse(all, "gránátalmát ettem")));
        // A hálózat nem hal, a csúszás nem túrós csusza.
        assertEquals("—", names(Foods.parse(all, "hálózati hiba")));
        assertEquals("—", names(Foods.parse(all, "csúszást érzek")));
        assertEquals("Hal (fehér)", names(Foods.parse(all, "halat ettem")));
        assertEquals("Túrós csusza", names(Foods.parse(all, "túrós csusza")));
    }

    /**
     * A „meg" igekötő és a gy-kezdetű ige együtt MEGGY-nek olvasódik.
     *
     * megGYŐZ, megGYÓGYUL, megGYÚJT – és a „meggyógyult a vállam" épp a
     * rehab-oldal mondata: abból eddig egy adag meggy lett a naplóban.
     * Mellette a legINkább közepén a gin, a szövEGRÉSZben az egres.
     */
    @Test public void aVerbPrefixDoesNotMakeACherry() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"meggyőzőbb", "meggyőztem", "meggyógyult a vállam",
                "meggyújtottam", "leginkább", "leginkább ezt szeretem", "szövegrész"})
            assertEquals(q, "—", names(Foods.parse(all, q)));
        // A valódi meggy és egres marad.
        assertEquals("Cseresznye / meggy", names(Foods.parse(all, "meggyet ettem")));
        assertEquals("Bogyós gyümölcs", names(Foods.parse(all, "egres")));
    }

    /**
     * Ötvenezer szavas magyar gyakorisági lista söprése.
     *
     * A saját kommentjeinkből épített korpusz elfogyott: ez a lista valódi
     * beszélt nyelvi szavakból jön, és sorra hozta a rövid szótövek
     * csapdáit. Mind valódi eset volt – a bejegyzés létrejött, csak épp nem
     * arról, amit az ember írt. A leggyakoribbak: a HAL ige („meghalt"), a
     * ZAB a hosszABBban, a BAB a szoBÁBAn, a LIBA a nappaLIBAn, a GIN a
     * meGINtben.
     */
    @Test public void everydayHungarianWordsAreNotFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[] words = {"meghalt", "meghalni", "halok", "haldoklik", "idehallgass",
                "hosszabb", "igazából", "házában", "lakása", "mellkasán",
                "baba", "kisbaba", "szobában", "fürdőszobában",
                "suliban", "buliba", "nappaliban",
                "háború", "világháborúban", "cimbora", "hátborzongató", "szobor",
                "bíboros", "borravalót", "felháborító",
                "megint", "meginni", "leginkább", "ginger",
                "idióta", "tolvaj", "csajt", "kapitány", "vadászni", "sors",
                "problémája", "majmok", "svájci", "próféta", "team", "vasúti",
                "terrorizmus", "csörög", "megállapítani", "ígéretes", "döntetlen",
                "pókember", "hamupipőke", "elkésünk", "pszichiáter", "épphogy",
                "testek", "festék", "kerestek", "boncolás", "karcolás",
                "bokszoló", "gyászoló", "divízió", "kettejük", "elbuktam",
                "kísértés", "megsértése", "ballisztikai", "vizelet", "világomban",
                "shakespeare", "marhaság", "ipari", "párizsban", "szövegrész",
                "meggyőzőbb", "meggyógyult", "gyűrűm"};
        StringBuilder bad = new StringBuilder();
        for (String w : words) {
            java.util.List<Foods.Hit> h = Foods.parse(all, w);
            if (!h.isEmpty()) bad.append("\n  ").append(w).append(" -> ").append(names(h));
        }
        assertEquals("hétköznapi szóból étel lett:" + bad, 0, bad.length());
    }

    /** Ugyanez a mozgás-felismerőn. */
    @Test public void everydayHungarianWordsAreNotSport() {
        StringBuilder bad = new StringBuilder();
        for (String w : new String[]{"pasas", "nagyuram", "teremtés", "teremtmény",
                "egyébként", "jegyéből", "félreolvasásból", "megúsztuk",
                "mozgásképtelenség", "marhára"}) {
            Activities.Parsed p = Activities.parse(w);
            if (p != null && !p.isEmpty())
                bad.append("\n  ").append(w).append(" -> ").append(p.plans.get(0).kind.id);
        }
        assertEquals("hétköznapi szóból edzés lett:" + bad, 0, bad.length());
    }

    /** A valódi ételek és mozgások ettől nem sérülnek. */
    @Test public void theRealWordsStillWork() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[][] ok = {{"halat ettem", "Hal (fehér)"}, {"zabkása", "Zabpehely"},
                {"babot ettem", "Bab (főtt)"}, {"liba", "Kacsa / liba"},
                {"vörösbor", "Bor (vörös/fehér)"}, {"kesudió", "Kesudió"},
                {"chia mag", "Chia / lenmag"}, {"macchiato", "Tejeskávé / cappuccino"},
                {"steak", "Marhahús"}, {"sertéskaraj", "Sertéskaraj"},
                {"szőlő", "Szőlő"}, {"rétes", "Rétes"}, {"pite", "Pite (almás/gyümölcsös)"},
                {"vaszabi", "Mustár"}, {"csirkemájat ettem", "Csirkemáj"},
                {"almáját megette", "Alma"}, {"poke bowl", "Poke bowl"}};
        for (String[] c : ok)
            assertEquals(c[0], c[1], names(Foods.parse(all, c[0])));
        assertEquals("kondi", Activities.parse("edzőteremben voltam").plans.get(0).kind.id);
        assertEquals("munka", Activities.parse("ásás a kertben 1 óra").plans.get(0).kind.id);
    }

    /**
     * Elgépelésre tipp jár, nem „ezt még nem ismerem".
     *
     * A telefon billentyűzetén az elütés a leggyakoribb hiba, és a „joghrut"
     * eddig ugyanazt kapta, mint egy tényleg ismeretlen étel. Pedig ismerjük,
     * csak egy betűvel odébb.
     *
     * A szabály szigorú, mert a rossz tipp bosszantóbb, mint a semmi: hat
     * betűtől, egyező szókezdettel, egy hibával (hosszú szónál kettővel).
     * A felcserélt betű egy hibának számít – a telefonon az a jellemző.
     */
    @Test public void typosGetASuggestion() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[][] cases = {{"joghrut", "Joghurt"}, {"csirkemel", "Csirkemell (sült/grill)"},
                {"burgnya", "Burgonya (főtt)"}, {"csokolde", "Csokoládé"},
                {"hamburgr", "Hamburger"}, {"mogyroo", "Mogyoró"},
                {"paradicsm", "Paradicsom"}, {"tejfoel", "Tejföl"}};
        for (String[] c : cases) {
            Foods.Food f = Foods.closest(all, c[0]);
            assertEquals(c[0], c[1], f == null ? "—" : f.name);
        }
    }

    /** …de a hétköznapi szóra nincs tipp. */
    @Test public void everydayWordsGetNoSuggestion() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        StringBuilder bad = new StringBuilder();
        for (String q : new String[]{"valami", "asztal", "telefon", "macska", "ember",
                "hagyja", "készen", "tiszta", "harcra", "oldalán", "szedem", "kényes",
                "mindenki", "szeretem", "gondolom", "amikor", "nagyon", "asdfgh"}) {
            Foods.Food f = Foods.closest(all, q);
            if (f != null) bad.append("\n  ").append(q).append(" -> ").append(f.name);
        }
        assertEquals("téves tipp:" + bad, 0, bad.length());
        // Üres és rövid bemenet sem dob.
        assertNull(Foods.closest(all, null));
        assertNull(Foods.closest(all, ""));
        assertNull(Foods.closest(all, "ab"));
        assertNull(Foods.closest(null, "alma"));
    }

    /** A felcserélt betű egy hiba, nem kettő. */
    @Test public void aSwappedLetterIsOneMistake() {
        assertEquals(1, Foods.editDistance("joghrut", "joghurt", 2));
        assertEquals(1, Foods.editDistance("alma", "almá", 2));
        assertEquals(0, Foods.editDistance("alma", "alma", 2));
        assertEquals(2, Foods.editDistance("alma", "elme", 2));
        // A korlát fölött feladja – nem számol tovább.
        assertTrue(Foods.editDistance("alma", "csirkemell", 2) > 2);
    }

    /**
     * Az étel NEVE – zárójeles pontosítás nélkül – önmagát adja vissza.
     *
     * Ez az elgépelés-tipp útja: a javaslatra koppintva a név kerül a mezőbe,
     * és azt a felismerő újra elolvassa. A zárójeles magyarázatot ezért
     * levágjuk – a „Rántott hús (sertés)" mellé különben egy adag
     * sertéskaraj is bement volna, a „Kakaó (tejes)" mellé egy pohár tej.
     */
    @Test public void everyFoodNameResolvesToItself() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        StringBuilder bad = new StringBuilder();
        for (Foods.Food f : Foods.ALL) {
            String typed = f.name.replaceAll("\\s*\\(.*?\\)", "").trim();
            java.util.List<Foods.Hit> h = Foods.parse(all, typed);
            if (h.size() != 1 || !h.get(0).food.name.equals(f.name))
                bad.append("\n  ").append(f.name).append(" -> ").append(names(h));
        }
        assertEquals("a saját nevét nem ismeri fel:" + bad, 0, bad.length());
    }

    /**
     * A ragozás véletlen betűsorai nem vehetik el az ételt.
     *
     * A rövid tövek szó-belseji tiltása kétélű: a „hosszABB"-ból tényleg nem
     * lehet zabpehely, csakhogy a magyar ragozás rendre gyárt ugyanilyen
     * véletlen betűsorokat a VALÓDI ételekben is. A „piZZABól" közepén ott a
     * zab, a „sziruPHOz"-ban a pho, a „gnocCHIAval"-ban a chia, az
     * „eszpreSSZÓBAn"-ban a szoba, a „hagyMÁJA"-ban a máj.
     *
     * Ezért az ilyen tő nem a SZÓT takarja ki, csak maga nem illeszkedhet a
     * szó belsejében.
     */
    @Test public void inflectionDoesNotSwallowTheFood() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        String[][] keep = {{"pizzából", "Pizza"}, {"pizzában", "Pizza"},
                {"gnocchival", "Gnocchi"}, {"sziruphoz", "Szirup (juhar/agavé)"},
                {"ketchuphoz", "Ketchup"}, {"wraphoz", "Tortilla / wrap"},
                {"szörphöz", "Szörp (hígítva)"}, {"eszpresszóban", "Kávé (fekete)"},
                {"hagymája", "Hagyma"}, {"tormája", "Torma / gyömbér"},
                {"csuszából", "Túrós csusza"}, {"macchiato", "Tejeskávé / cappuccino"}};
        for (String[] c : keep)
            assertEquals(c[0], c[1], names(Foods.parse(all, c[0])));
        assertEquals("Pizza", names(Foods.parse(all, "csak a felét ettem meg a pizzából")));
        // És a hétköznapi szavak továbbra sem ételek.
        for (String q : new String[]{"hosszabb", "igazából", "megint", "épphogy",
                "pszichiáter", "elkésünk", "testek"})
            assertEquals(q, "—", names(Foods.parse(all, q)));
    }

    /**
     * Az előre normalizált szótövek megegyeznek az eredetivel.
     *
     * A felismerés sebességéért a szótövek ékezet nélküli alakja egyszer
     * készül el, az ételek betöltésekor. Ha ez elcsúszna az eredetitől, a
     * hiba néma lenne: a felismerés egyszerűen nem találna meg valamit.
     */
    @Test public void precomputedStemsMatchTheOriginals() {
        for (Foods.Food f : Foods.ALL) {
            assertEquals(f.name, f.stems.length, f.nstems.length);
            for (int i = 0; i < f.stems.length; i++)
                assertEquals(f.name, Foods.norm(f.stems[i]), f.nstems[i]);
        }
    }

    private static String names(java.util.List<Foods.Hit> h) {
        if (h.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (Foods.Hit x : h) sb.append(sb.length() > 0 ? ", " : "").append(x.food.name);
        return sb.toString();
    }

    /** Az új hal-tövek nem esnek bele hétköznapi szavakba. */
    @Test public void fishStemsStayOutOfEverydayWords() {
        java.util.List<Foods.Food> all = java.util.Arrays.asList(Foods.ALL);
        for (String q : new String[]{"autóbusszal mentem", "buszon ettem", "szamuráj",
                "pangásból", "sülve"})
            assertTrue("halat talált benne: " + q, Foods.parse(all, q).isEmpty());
        assertEquals("Hal (fehér)", Foods.parse(all, "süllő roston").get(0).food.name);
        assertEquals("Hal (fehér)", Foods.parse(all, "tilápia filé").get(0).food.name);
    }
}
