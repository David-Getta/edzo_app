package com.edzo.idozito;

/**
 * Testsúly és testzsír egyetlen mondatból: „ma reggel 78,4 kg”, „78 kiló
 * vagyok”, „mérleg: 81,2”, „18% testzsír”.
 *
 * A mérés a legrövidebb életű adat az appban – reggel felállsz a mérlegre, és
 * két számot látsz. Eddig ehhez a Profil képernyőre kellett menni, ott két
 * mezőt kitölteni és menteni; a mondat viszont fél másodperc.
 *
 * A felismerés szándékosan szűkszavú, mert a kilogramm a legterheltebb
 * mértékegység az appban: „80 kg” lehet munkasúly, bevásárlás és testsúly is.
 * Csak KÉT esetben mondunk mérést: ha a mondat kimondja („vagyok”, „mérleg”,
 * „testsúly”), vagy ha a számon és egy napszakon kívül nincs is más benne. Egy
 * félreértett mérés elrontja a súlytrendet, a BMI-t és a kalóriacél-ajánlást
 * is – abból inkább ne legyen bejegyzés, mint rossz.
 */
public final class BodyParse {

    private BodyParse() {}

    /** Életszerű testsúly-határok kilóban. */
    static final double MIN_KG = 30, MAX_KG = 250;
    /** Életszerű testzsír-határok százalékban. */
    static final double MIN_FAT = 3, MAX_FAT = 60;

    /** Egy mérés a mondatból. A hiányzó adat 0. */
    public static final class Body {
        public final double kg;
        public final double fatPct;
        Body(double kg, double fatPct) { this.kg = kg; this.fatPct = fatPct; }

        public boolean isEmpty() { return kg <= 0 && fatPct <= 0; }

        /** „78,4 kg  ·  18% testzsír” – az előnézethez. */
        public String label() {
            StringBuilder sb = new StringBuilder();
            if (kg > 0) sb.append(Hu.kg(kg)).append(" kg");
            if (fatPct > 0) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append(Hu.kg(fatPct)).append("% testzsír");
            }
            return sb.toString();
        }
    }

    /** Szavak, amelyek kimondják, hogy a saját testsúlyáról van szó. */
    private static final String[] BODY_WORDS = {
            "testsuly", "testsulyom", "sulyom", "merleg", "merlegen", "merlegre",
            "vagyok", "lettem", "nyomok", "fogytam", "hiztam", "leadtam", "testzsir"
    };

    /**
     * Szavak, amelyektől a mondat biztosan NEM mérés – a súly másé.
     *
     * Mind egész szóként keresve: a rövid szótő máshol elrejtve („húsz”-ban a
     * „hús”) a legmegbízhatóbb módja annak, hogy egy jó mondat elvesszen.
     */
    private static final String[] NOT_BODY = {
            "nyomtam", "emeltem", "huztam", "toltam", "vettem", "vasaroltam", "hoztam"
    };

    /** A mondatban rejlő mérés, vagy egy üres Body. */
    public static Body parse(String q) {
        if (q == null) return new Body(0, 0);
        String s = Foods.norm(q);
        if (s.isEmpty()) return new Body(0, 0);
        for (String n : NOT_BODY) if (word(s, n)) return new Body(0, 0);
        // A két kapu közül legalább az egyiknek nyitva kell lennie.
        boolean said = hasBodyWord(s);
        if (!said && !onlyNumbersLeft(s)) return new Body(0, 0);

        double fat = bodyFat(s);
        double kg = weight(s, fat);
        return new Body(kg, fat);
    }

    /**
     * Testzsír: „18% testzsír”, „testzsír 18”, „18 százalék”.
     *
     * A puszta százalék is elfogadható: ilyen mondatban más százalékos adat
     * nem szokott szerepelni.
     */
    private static double bodyFat(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2}([.,]\\d)?)\\s?(%|szazalek)").matcher(s);
        while (m.find()) {
            double v = num(m.group(1));
            if (v >= MIN_FAT && v <= MAX_FAT) return v;
        }
        m = java.util.regex.Pattern.compile("testzsir\\w*\\s?:?\\s?(\\d{1,2}([.,]\\d)?)")
                .matcher(s);
        if (m.find()) {
            double v = num(m.group(1));
            if (v >= MIN_FAT && v <= MAX_FAT) return v;
        }
        return 0;
    }

    /**
     * Testsúly kilóban – a testzsírként már elhasznált számot kihagyva.
     *
     * A „78,4 kg és 18% testzsír” mondatban a 18 nem lehet másodszor is súly;
     * a sáv (30–250) ezt magától is kizárja, de a fordított sorrendű mondat
     * („18% testzsír, 78,4”) miatt a kihagyás akkor is kell.
     */
    private static double weight(String s, double fat) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,3}([.,]\\d{1,2})?)\\s?-?\\s?(kg|kilo|kila)?").matcher(s);
        while (m.find()) {
            double v = num(m.group(1));
            if (v < MIN_KG || v > MAX_KG) continue;
            if (fat > 0 && Math.abs(v - fat) < 0.001) continue;
            // Százalékjel után álló szám sosem kiló – még akkor sem, ha
            // testzsírnak túl nagy volt („80% testzsír”). Az elgépelt
            // százalékból nyolcvan kiló lenne.
            String rest = s.substring(m.end()).trim();
            if (rest.startsWith("%") || rest.startsWith("szazalek")) continue;
            return v;
        }
        return 0;
    }

    /** Kimondott testsúly-szó a mondatban (egész szóként). */
    private static boolean hasBodyWord(String s) {
        for (String w : BODY_WORDS) if (word(s, w)) return true;
        return false;
    }

    /**
     * A számokon, mértékegységeken és napszakokon kívül maradt-e érdemi szó.
     *
     * Az „ma reggel 78,4 kg” mérés, a „fekvenyomás 80 kg” nem. Ez a szabály
     * arra a maradékra való, amit a többi felismerő nem ért: ilyenkor a
     * mondatban a számon kívül nem maradhat semmi.
     */
    private static boolean onlyNumbersLeft(String s) {
        // Körülnéző (lookaround) határok, nem elnyelt elválasztók: két
        // szomszédos szó („ma reggel”) közül a második különben bennmaradna,
        // mert az elsőt kereső minta elvinné a köztük álló szóközt.
        String rest = s.replaceAll("\\d+([.,]\\d+)?", " ")
                .replaceAll("(?<![a-z])(kg|kilo|kila|szazalek|testzsir\\w*|ma|reggel|"
                        + "este|delben|delelott|delutan|ejjel|hajnalban|tegnap|most|"
                        + "eppen|epp|ebredes|felkeles|utan|kor|orakor|volt|voltam)"
                        + "(?![a-z])", " ")
                .replaceAll("[^a-z]", " ").trim();
        return rest.isEmpty();
    }

    /** Egész szóként szerepel-e a mondatban. */
    private static boolean word(String s, String w) {
        int i = s.indexOf(w);
        while (i >= 0) {
            boolean l = i == 0 || !Character.isLetterOrDigit(s.charAt(i - 1));
            int e = i + w.length();
            boolean r = e >= s.length() || !Character.isLetterOrDigit(s.charAt(e));
            if (l && r) return true;
            i = s.indexOf(w, i + 1);
        }
        return false;
    }

    private static double num(String s) {
        try { return Double.parseDouble(s.replace(',', '.')); }
        catch (NumberFormatException e) { return 0; }
    }
}
