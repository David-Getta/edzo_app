package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Rögzített próbasor: életszerű mondatok és a hozzájuk tartozó, PONTOS
 * eredmény – mind a négy felismerőre.
 *
 * A többi teszt egy-egy szabályt őriz. Ez a mondatokat őrzi: azt, amit a
 * felhasználó tényleg beír. Egy szabály-módosítás sokszor nem a saját
 * tesztjét bukja el, hanem egy másik mondatot ront el mellékhatásként – és
 * pont az ilyen csendes elcsúszás a legdrágább, mert a bejegyzés létrejön,
 * csak rossz értékkel.
 *
 * A sorok tudatosan tömörek: bal oldalon a mondat, jobb oldalon egyetlen
 * szövegként az egész eredmény. Ha egy sor megváltozik, a hiba kiírja a
 * mondatot, a várt és a kapott alakot – így látszik, mit vitt el a
 * változtatás.
 */
public class SentenceBatteryTest {

    /** Rögzített „most": 2025. július 30., szerda, 12:00 – a napnevekhez kell. */
    private static final long NOW = 1_753_869_600_000L;

    // ---------- Súlyzós sorozatok ----------

    private static String lift(String q) {
        StringBuilder sb = new StringBuilder();
        for (StrengthParse.Item it : StrengthParse.parse(q)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(it.name).append(' ');
            for (int i = 0; i < it.sets.size(); i++) {
                if (i > 0) sb.append('/');
                sb.append(it.sets.get(i).reps).append('@')
                  .append(Progression.kg(it.sets.get(i).weight));
            }
            if (it.rpe > 0) sb.append(" rpe").append(it.rpe);
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }

    @Test public void theStrengthSentencesStayAsTheyAre() {
        String[][] cases = {
                {"3x10 fekvenyomás 60 kg", "Fekvenyomás 10@60/10@60/10@60"},
                {"guggolás 5x5 80 kg", "Guggolás 5@80/5@80/5@80/5@80/5@80"},
                {"fekvenyomás 60x10, 70x8, 80x6", "Fekvenyomás 10@60/8@70/6@80"},
                {"fekvenyomás 60x10 70x8 80x6", "Fekvenyomás 10@60/8@70/6@80"},
                {"guggolás 12,10,8 60 kg", "Guggolás 12@60/10@60/8@60"},
                {"guggolás: 5,5,5 @ 100", "Guggolás 5@100/5@100/5@100"},
                {"bicepsz 12-10-8 15 kg", "Bicepsz 12@15/10@15/8@15"},
                {"ma guggoltam, 5 sorozat, 5 ismétlés, 100 kg",
                        "Guggolás 5@100/5@100/5@100/5@100/5@100"},
                {"fekvenyomás max 120 kg", "Fekvenyomás 1@120"},
                {"60 kg guggolás 3x8, 50 kg fekvenyomás 3x8",
                        "Guggolás 8@60/8@60/8@60 | Fekvenyomás 8@50/8@50/8@50"},
                {"kettlebell swing 5x20 24 kg",
                        "Kettlebell lendítés 20@24/20@24/20@24/20@24/20@24"},
                {"alkartámasz 3x60", "Plank 60@0/60@0/60@0"},
                {"guggolás 3x10 100 kg rpe 8", "Guggolás 10@100/10@100/10@100 rpe8"},
                {"nyomtam háromszor tízet fekvenyomásban 60 kg-mal",
                        "Fekvenyomás 10@60/10@60/10@60"},
                {"guggoltam 100-zal ötször ötöt",
                        "Guggolás 5@100/5@100/5@100/5@100/5@100"},
                {"50 fekvőtámasz", "Fekvőtámasz 50@0"},
                {"guggolás 3x10, majd 20 perc futás", "Guggolás 10@0/10@0/10@0"},
                {"guggolás 60 kg bemelegítés, aztán 3x5 100", "Guggolás 5@100/5@100/5@100"},
                {"húzódzkodás 3x max", "—"},
                {"jó edzés volt", "—"},
                {"húzódzkodás 3 szett maximumig", "—"},
                {"plank 3x60", "Plank 60@0/60@0/60@0"},
                {"plank 3x1 perc", "Plank 60@0/60@0/60@0"},
                {"3 perc plank", "Plank 180@0"},
                {"plank másfél perc", "Plank 90@0"},
                {"fal ülés 3x40 mp", "Fal-ülés 40@0/40@0/40@0"},
                {"plank 3 sorozat 60 másodperc", "Plank 60@0/60@0/60@0"},
        };
        check(cases, "súlyzós");
    }

    // ---------- Intervallum ----------

    private static String iv(String q) {
        IntervalParse.Plan p = IntervalParse.parse(q);
        return p == null ? "—" : p.rounds + "k " + p.work + "/" + p.rest
                + " w" + p.warm + " c" + p.cool;
    }

    @Test public void theIntervalSentencesStayAsTheyAre() {
        String[][] cases = {
                {"3 kör 40 mp munka 20 mp pihenő", "3k 40/20 w0 c0"},
                {"tabata", "8k 20/10 w0 c0"},
                {"8x20/10", "8k 20/10 w0 c0"},
                {"emom 12", "12k 60/0 w0 c0"},
                {"10x30s on 30s off", "10k 30/30 w0 c0"},
                {"kör: 6, munka: 40mp, pihenő: 20mp", "6k 40/20 w0 c0"},
                {"amrap 20 perc", "1k 1200/0 w0 c0"},
                {"8 kör: 20 mp sprint, 40 mp séta", "8k 20/40 w0 c0"},
                {"5x(3 perc / 1 perc)", "5k 180/60 w0 c0"},
                {"3 perc munka 1 perc pihenő, 6 ismétlés", "6k 180/60 w0 c0"},
                {"2 perc bemelegítés 6 kör 45/15 3 perc levezetés", "6k 45/15 w120 c180"},
                {"négyszer negyven másodperc munka húsz másodperc pihenő", "4k 40/20 w0 c0"},
                {"20 perc alatt 40/20", "20k 40/20 w0 c0"},
                {"guggolás 3x10", "—"},
                {"45 másodperc munka 15 pihenő nyolcszor", "8k 45/15 w0 c0"},
                {"2 perc munka 1 pihenő 5 kör", "5k 120/60 w0 c0"},
                {"munka 30 mp, pihenő 10 mp", "1k 30/10 w0 c0"},
                {"minden percben 1 kör, 15 percig", "—"},
        };
        check(cases, "intervallum");
    }

    // ---------- Edzés-mondatok ----------

    private static String act(String q) {
        Activities.Parsed p = Activities.parse(q, NOW);
        StringBuilder sb = new StringBuilder();
        for (Activities.Plan pl : p.plans) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(pl.kind.id).append('×').append(pl.count).append(' ')
              .append(pl.minutes).append('p');
            if (pl.km > 0) sb.append('/').append(Hu.d1(pl.km)).append("km");
        }
        if (sb.length() == 0) sb.append("—");
        return sb + " [" + p.days + "d+" + p.offset + "]";
    }

    @Test public void theWorkoutSentencesStayAsTheyAre() {
        String[][] cases = {
                {"tegnap 45 perc futás", "futas×1 45p [1d+1]"},
                {"1h20 futás", "futas×1 80p [1d+0]"},
                {"hetvenöt perc kondi", "kondi×1 75p [1d+0]"},
                {"30 perc futás, 20 perc kondi", "futas×1 30p | kondi×1 20p [1d+0]"},
                {"1 óra kondi, 30 perc futás", "kondi×1 60p | futas×1 30p [1d+0]"},
                {"kondi 1 óra futás 40 perc", "kondi×1 60p | futas×1 40p [1d+0]"},
                {"30 perc futás és kondi", "futas×1 30p | kondi×1 60p [1d+0]"},
                {"bicikli 20 km, futás 5 km",
                        "kerekpar×1 60p/20,0km | futas×1 30p/5,0km [1d+0]"},
                {"úsztam 1 km-t, futottam 5 km-t",
                        "uszas×1 25p/1,0km | futas×1 30p/5,0km [1d+0]"},
                {"az elmúlt héten 3 futás és 2 úszás, 40 perc",
                        "futas×3 40p | uszas×2 40p [7d+0]"},
                {"az elmúlt 3 nap alatt 3 futó edzés és 6 kézi edzés",
                        "futas×3 45p | kezilabda×6 90p [3d+0]"},
                {"20 edzés", "egyeb×20 45p [20d+0]"},
                {"minden reggel 20 perc jóga a héten", "joga×7 20p [7d+0]"},
                {"szombaton túráztam 4 órát, vasárnap pihentem", "tura×1 240p [1d+4]"},
                {"60 perc kondi bérlettel", "kondi×1 60p [1d+0]"},
                {"focit néztem és futottam 30 percet", "futas×1 30p [1d+0]"},
                {"kultúra 30 perc kondi", "kondi×1 30p [1d+0]"},
                {"lehetőség 30 perc kondi", "kondi×1 30p [1d+0]"},
                {"hetes bérlettel kondi", "kondi×1 60p [1d+0]"},
                {"január 30 perc kondi", "kondi×1 30p [1d+0]"},
                {"augusztus 5 perc nyújtás", "joga×1 5p [1d+0]"},
                {"kondi + futás, összesen másfél óra", "kondi×1 45p | futas×1 45p [1d+0]"},
                {"futóverseny 52 perc", "futas×1 52p [1d+0]"},
                {"kirándultunk 5 órát", "tura×1 300p [1d+0]"},
                {"20 perc bemelegítés + 40 perc foci", "foci×1 40p [1d+0]"},
                {"crossfit wod 20 perc", "kondi×1 20p [1d+0]"},
                {"futás 1:05:23", "futas×1 65p [1d+0]"},
                {"18:00-tól 19:00-ig kondi", "kondi×1 60p [1d+0]"},
                {"délelőtt 1 óra, délután fél óra kondi", "kondi×2 45p [1d+0]"},
                {"délelőtt kondi 1 óra és 30 perc futás",
                        "kondi×1 60p | futas×1 30p [1d+0]"},
                {"1 óra és 30 perc futás", "futas×1 90p [1d+0]"},
                {"este megyek edzeni", "— [1d+0]"},
                {"holnap futok 10 km-t", "— [1d+0]"},
                {"vasárnap pihentem", "— [1d+0]"},
        };
        check(cases, "edzés");
    }

    // ---------- Étkezés ----------

    private static String meal(String q) {
        List<Foods.Food> all = Arrays.asList(Foods.ALL);
        StringBuilder sb = new StringBuilder();
        for (Foods.Hit h : Foods.parse(all, q)) {
            if (sb.length() > 0) sb.append(" | ");
            double g = h.grams > 0 ? h.grams : h.food.portion;
            sb.append(h.food.name).append(' ').append(Math.round(g)).append('g');
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }

    @Test public void theMealSentencesStayAsTheyAre() {
        String[][] cases = {
                {"150 g csirkemell rizzsel", "Csirkemell (sült/grill) 150g | Rizs (főtt) 200g"},
                {"két tojás és egy szelet kenyér", "Tojás 110g | Kenyér 35g"},
                {"egy egész tábla csoki", "Csokoládé 100g"},
                {"két korsó sör és egy hamburger", "Sör 1000g | Hamburger 250g"},
                {"ittam fél liter vizet", "Víz / ásványvíz 500g"},
                {"gyümölcssaláta", "Gyümölcssaláta 200g"},
                {"köles", "Hajdina / köles (főtt) 200g"},
                {"nem ettem csokit de almát igen", "Alma 150g"},
                {"megkínáltak tortával, de nem kértem", "—"},
                {"ittam kávét, de cukrot nem kértem", "Kávé (fekete) 200g"},
                {"nem kértem sültkrumplit a hamburger mellé", "Hamburger 250g"},
                {"2-3 szelet kenyér", "Kenyér 88g"},
                {"3-4 dkg sajt", "Sajt (trappista) 35g"},
                {"negyed pizza", "Pizza 75g"},
                {"dupla adag rizs", "Rizs (főtt) 400g"},
                {"tábla csoki", "Csokoládé 100g"},
                {"két és fél szelet kenyér", "Kenyér 88g"},
                {"vizsga után ettem", "—"},
                {"zabáltam", "—"},
                {"majd", "—"},
                {"iskolában", "—"},
                {"tábor", "—"},
        };
        check(cases, "étkezés");
    }

    // ---------- Közös futtató ----------

    private static void check(String[][] cases, String kind) {
        StringBuilder bad = new StringBuilder();
        for (String[] c : cases) {
            String got = kind.equals("súlyzós") ? lift(c[0])
                    : kind.equals("intervallum") ? iv(c[0])
                    : kind.equals("edzés") ? act(c[0]) : meal(c[0]);
            if (!got.equals(c[1]))
                bad.append("\n  ").append(c[0])
                   .append("\n     várt:   ").append(c[1])
                   .append("\n     kapott: ").append(got);
        }
        assertEquals("elcsúszott " + kind + " mondat:" + bad, 0, bad.length());
    }
}
