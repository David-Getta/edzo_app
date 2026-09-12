package com.edzo.idozito;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Gyorsítótárazott reguláris kifejezések – csak a webes változatban.
 *
 * A felismerés mondatonként több mint ezer mintát használ, és mindig
 * ugyanazokat. A JVM-en ez elhanyagolható, a böngészőben viszont ez tette a
 * felismerést hétszáz ezredmásodpercessé – gépelés közben használhatatlanná.
 * Négy fogás hozza vissza a használhatóságot:
 *
 * <ul>
 *   <li>Egy minta egyszer fordul le az app életében (700 → 105 ms).
 *   <li>Aminek a kötelező szava nincs a mondatban, azt el se indítjuk.
 *   <li>Az illesztőket újrahasználjuk: nem foglalunk hozzájuk tömböket.
 *   <li>Ha a keresés nem talál semmit, a cserét meg se kíséreljük – a
 *       szöveg lemásolása volt a legdrágább egyetlen tétel (105 → 73 ms).
 * </ul>
 *
 * A hívásokat nem kézzel írjuk át: a webes fordítás előtt a
 * tools/regexgyorsito.py teszi meg a KIVONATOLT másolatokon. Az eredeti
 * Java forrás olvasható marad, és az egyezés-teszt az eredetit futtatja a
 * JVM-en – így az átírás hibája azonnal kiderül.
 */
final class Rx {

    private Rx() { }

    /**
     * A tár korlátos. A minták túlnyomó része szó szerinti, de néhány a
     * szövegből épül; egy hosszú naplózó menet így sem növelheti a
     * memóriát vég nélkül.
     */
    private static final int MAX = 4000;
    private static final Map<String, Pattern> TAR = new HashMap<>();

    static Pattern c(String minta) {
        Pattern p = TAR.get(minta);
        if (p == null) {
            p = Pattern.compile(kifejt(minta));
            if (TAR.size() >= MAX) TAR.clear();
            TAR.put(minta, p);
        }
        return p;
    }

    static Pattern c(String minta, int jelzok) {
        // A jelzős alak ritka; a kulcsba be kell férnie, különben két
        // különböző jelentésű minta ütközne ugyanazon a kulcson.
        String kulcs = jelzok + "\0" + minta;
        Pattern p = TAR.get(kulcs);
        if (p == null) {
            p = Pattern.compile(kifejt(minta), jelzok);
            if (TAR.size() >= MAX) TAR.clear();
            TAR.put(kulcs, p);
        }
        return p;
    }

    /**
     * A böngészőbeli regex-motor egy hibájának megkerülése.
     *
     * A JVM-en és a böngészőben MÁS motor fut, és a másodikban a {n,m}
     * kvantor elromlik, ha ISMÉTELT csoport belsejében áll. A
     * „(?:\d{1,3}\s?-\s?)+\d{1,3}" a „12-10-8"-ból csak a „12-10"-et
     * találja meg: a külső ismétlés egy körrel korábban feladja, és nem
     * lép vissza. A naplóba ettől „12-10" ismétlés került volna a
     * „12-10-8" helyett – hibaüzenet nélkül, csendben rossz adat.
     *
     * A kvantort ezért kifejtjük: a \d{1,3} helyére \d\d?\d? kerül. A két
     * alak jelentése és mohósága azonos – a motor viszont az utóbbit
     * helyesen kezeli. Csak az ismételt csoportok belsejében írunk át,
     * hogy minden más minta betűre ugyanaz maradjon; hogy tényleg az is
     * marad, azt a tools/webteszt.sh egyezés-vizsgálata őrzi.
     */
    static String kifejt(String minta) {
        if (minta.indexOf('{') < 0) return minta;
        return bejar(minta, false);
    }

    /** Egy szakasz átírása; `belul` = ismételt csoporton belül vagyunk. */
    private static String bejar(String m, boolean belul) {
        StringBuilder ki = new StringBuilder();
        int i = 0;
        int n = m.length();
        while (i < n) {
            char c = m.charAt(i);
            if (c == '(') {
                int z = csoportVege(m, i);
                int[] q = kvantor(m, z + 1);
                // Az egyszeri csoport belseje az ismétlésen belül is
                // ismétlődik – a jelzést tehát örökítjük.
                boolean ism = belul || q[0] == 1 || q[0] == 2;
                String test = bejar(m.substring(i + 1, z), ism);
                // A LUSTA korlátos ismétlés felsorolássá válik: a motor a
                // „(?:\s+\p{L}{2,12}){0,4}?" alakot egyáltalán nem találja
                // meg („5 emeletet mentem fel gyalog háromszor" tizenöt
                // emelete elveszett), a vele egyenértékű „(?:|A|AA|AAA|AAAA)"
                // felsorolást viszont igen – és a próbálkozás sorrendje
                // ugyanaz, vagyis a jelentés is.
                //
                // A felsorolás tagjait NEM fejtjük ki: bennük már nincs
                // ismétlés, ott a {n,m} helyesen működik. Kifejtve viszont
                // a „\p{L}{2,12}" tizenegy útjából ezer lesz, és négy tag
                // mellett ez ezermilliárd – az „ma 3 emeletet másztam
                // liftezés helyett" mondat két és fél PERCIG futott a
                // böngészőben, vagyis a telefon lefagyott volna tőle.
                if (q[0] == 2 && q[3] >= 0 && q[3] <= 6 && m.startsWith("(?:", i)
                        && test.length() <= 240 && !fogCsoportot(test)) {
                    String mag = bejar(m.substring(i + 1, z), belul).substring(2);
                    ki.append("(?:");
                    for (int r = q[2]; r <= q[3]; r++) {
                        if (r > q[2]) ki.append('|');
                        for (int u = 0; u < r; u++) ki.append("(?:").append(mag).append(')');
                    }
                    ki.append(')');
                    i = q[1] + 1;
                    continue;
                }
                ki.append('(').append(test).append(')');
                i = z + 1;
                continue;
            }
            int v = atomVege(m, i);
            if (v < 0) {
                ki.append(c);
                i++;
                continue;
            }
            String atom = m.substring(i, v + 1);
            i = v + 1;
            if (!belul || i >= n || m.charAt(i) != '{') {
                ki.append(atom);
                continue;
            }
            int z = m.indexOf('}', i);
            int[] hatar = z < 0 ? null : hatarok(m.substring(i + 1, z));
            // A lusta és a birtokos alakot békén hagyjuk: ott a kifejtés
            // megváltoztatná a visszalépés sorrendjét.
            boolean utana = hatar != null && z + 1 < n
                    && (m.charAt(z + 1) == '?' || m.charAt(z + 1) == '+');
            if (hatar == null || utana) {
                ki.append(atom);
                continue;
            }
            for (int t = 0; t < hatar[0]; t++) ki.append(atom);
            for (int t = hatar[0]; t < hatar[1]; t++) ki.append(atom).append('?');
            i = z + 1;
        }
        return ki.toString();
    }

    /**
     * Az `i` helyen álló ismétlő kvantor elemzése.
     *
     * Négy szám jön vissza: a fajta, a kvantor utolsó helye, az alsó és a
     * felső határ. Fajta: 0 = nem ismétlés, 1 = mohó ismétlés, 2 = lusta
     * KORLÁTOS ismétlés, 3 = egyéb (birtokos, vagy korlát nélküli lusta).
     */
    private static int[] kvantor(String m, int i) {
        int[] nincs = {0, i - 1, -1, -1};
        if (i >= m.length()) return nincs;
        char c = m.charAt(i);
        int veg;
        int also = -1;
        int felso = -1;
        if (c == '*' || c == '+') {
            veg = i;
        } else if (c == '{') {
            veg = m.indexOf('}', i);
            if (veg < 0) return nincs;
            int[] h = hatarok(m.substring(i + 1, veg));
            if (h != null) {
                also = h[0];
                felso = h[1];
            }
        } else {
            return nincs;
        }
        // A kvantor után álló `?` lusta, a `+` birtokos ismétlést jelöl.
        char utana = veg + 1 < m.length() ? m.charAt(veg + 1) : ' ';
        if (utana == '+') return new int[]{3, veg + 1, also, felso};
        if (utana == '?') return new int[]{also < 0 ? 3 : 2, veg + 1, also, felso};
        return new int[]{1, veg, also, felso};
    }

    /** Van-e a szakaszban ZÁRÓJELES (számozott) csoport? */
    private static boolean fogCsoportot(String m) {
        int i = 0;
        while (i < m.length()) {
            char c = m.charAt(i);
            if (c == '\\') { i += 2; continue; }
            if (c == '[') { i = osztalyVege(m, i) + 1; continue; }
            if (c == '(' && (i + 1 >= m.length() || m.charAt(i + 1) != '?')) return true;
            i++;
        }
        return false;
    }

    /**
     * A {n,m} kvantor két határa, vagy null, ha nem fejtjük ki.
     *
     * A nyitott {n,} alakot a böngésző motorja helyesen kezeli, azt tehát
     * nem bántjuk. A túl bő tartomány kifejtése pedig áttekinthetetlen
     * mintát adna, és a keresést is lassítaná: ott is maradunk az eredetinél.
     */
    private static int[] hatarok(String belso) {
        int vesszo = belso.indexOf(',');
        String a = vesszo < 0 ? belso : belso.substring(0, vesszo);
        String b = vesszo < 0 ? belso : belso.substring(vesszo + 1);
        if (b.isEmpty()) return null;
        int also = szam(a);
        int felso = szam(b);
        if (also < 0 || felso < also || felso > 24) return null;
        return new int[]{also, felso};
    }

    private static int szam(String s) {
        if (s.isEmpty() || s.length() > 3) return -1;
        int e = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return -1;
            e = e * 10 + (c - '0');
        }
        return e;
    }

    /** Az `i` helyen álló, EGY karaktert fogyasztó atom vége, vagy -1. */
    private static int atomVege(String m, int i) {
        char c = m.charAt(i);
        if (c == '\\') {
            if (i + 1 >= m.length()) return -1;
            char d = m.charAt(i + 1);
            // \p{L}, \P{Lu}, \x{1F600}: a kapcsos zárójel a NEVÜKHÖZ tartozik,
            // nem kvantor. Enélkül a \p{L}{2,12} kifejtése a nevet vinné el.
            if ((d == 'p' || d == 'P' || d == 'x') && i + 2 < m.length()
                    && m.charAt(i + 2) == '{') {
                int z = m.indexOf('}', i + 2);
                return z < 0 ? -1 : z;
            }
            // A helyzet-jelölők nem fogyasztanak karaktert: ismételni sem
            // lehet őket, kifejteni sem szabad.
            if ("bBAzZGQE".indexOf(d) >= 0) return -1;
            return i + 1;
        }
        if (c == '[') return osztalyVege(m, i);
        if (c == '.') return i;
        if ("()[]{}*+?|^$".indexOf(c) >= 0) return -1;
        return i;
    }

    /** A `[` helyen kezdődő karakterosztály záró `]` jele. */
    private static int osztalyVege(String m, int i) {
        int j = i + 1;
        if (j < m.length() && m.charAt(j) == '^') j++;
        // A legelső `]` még önmagát jelenti, nem zár.
        if (j < m.length() && m.charAt(j) == ']') j++;
        while (j < m.length()) {
            char c = m.charAt(j);
            if (c == '\\') j += 2;
            else if (c == '[') j = osztalyVege(m, j) + 1;
            else if (c == ']') return j;
            else j++;
        }
        return m.length() - 1;
    }

    /** A `(` helyen kezdődő csoport záró `)` jele. */
    private static int csoportVege(String m, int i) {
        int mely = 0;
        int j = i;
        while (j < m.length()) {
            char c = m.charAt(j);
            if (c == '\\') { j += 2; continue; }
            if (c == '[') { j = osztalyVege(m, j) + 1; continue; }
            if (c == '(') mely++;
            else if (c == ')') {
                mely--;
                if (mely == 0) return j;
            }
            j++;
        }
        return m.length() - 1;
    }

    /**
     * A minta KÖTELEZŐ szövegdarabjai mintánként – üres tömb = nincs ilyen.
     *
     * A legtöbb minta konkrét magyar szavakra vadászik („lepcso", „emelet",
     * „ismetles"), gyakran egy szólistára („palya|stadion|medence"). Ha
     * EGYIK szó sincs a mondatban, a minta biztosan nem illeszkedik – a
     * keresést el se kell kezdeni.
     */
    private static final Map<String, String[]> KOTELEZO = new HashMap<>();

    /** Ennél hosszabb szólistát nem éri meg végigpróbálni. */
    private static final int AGAK = 40;

    /** Ennél rövidebb darab szinte minden mondatban ott van. */
    private static final int LEGROVIDEBB = 3;

    /**
     * Elindítható-e egyáltalán a keresés?
     *
     * A böngészőben a regex-keresés nagyságrendekkel drágább, mint egy
     * részszöveg-keresés: egy mondat felismerése több mint ezer mintát
     * futtat le, és a túlnyomó többségük olyan szóra vár, ami ott sincs.
     * A szűrő ezt a többséget ejti ki – a jelentés nem változik, mert csak
     * akkor mond nemet, ha a minta ÚGYSEM illeszkedne.
     */
    private static boolean kihagyhato(String s, String minta) {
        String[] kell = KOTELEZO.get(minta);
        if (kell == null) {
            kell = kotelezoSzoveg(minta);
            if (KOTELEZO.size() >= MAX) KOTELEZO.clear();
            KOTELEZO.put(minta, kell);
        }
        if (kell.length == 0) return false;
        for (int i = 0; i < kell.length; i++) {
            if (s.indexOf(kell[i]) >= 0) return false;
        }
        return true;
    }

    /**
     * A minta kötelező szavai: a találathoz legalább az EGYIKÜKNEK ott kell
     * lennie a szövegben. Üres tömb, ha ilyet nem tudunk biztosan kimondani.
     *
     * Óvatosan olvassuk: a karakterosztályokat és minden elhagyható elemet
     * átugorjuk, a vagylagos ágakból pedig csak akkor lesz lista, ha
     * MINDEGYIK ág megköveteli a magáét. Inkább mondjunk le a szűrésről,
     * mint hogy egy találatot elveszítsünk – ezt a web/test/Szuro.java
     * minden mintára, minden korpusz-mondattal ellenőrzi.
     */
    private static String[] kotelezoSzoveg(String minta) {
        // A \Q…\E idézet és a kis-nagybetű jelző mást jelentene, mint amit
        // a részszöveg-keresés ellenőriz.
        if (minta.indexOf("\\Q") >= 0 || kisNagybetus(minta)) return URES;
        return koveteles(minta);
    }

    private static final String[] URES = new String[0];

    /** Van-e a mintában kis-nagybetűt elnéző `(?i)` jelző? */
    private static boolean kisNagybetus(String m) {
        int i = m.indexOf("(?");
        while (i >= 0) {
            for (int j = i + 2; j < m.length(); j++) {
                char c = m.charAt(j);
                if (c == 'i') return true;
                if (c != 'd' && c != 'm' && c != 's' && c != 'u'
                        && c != 'x' && c != 'U' && c != '-') break;
            }
            i = m.indexOf("(?", i + 2);
        }
        return false;
    }

    /**
     * Egy minta-szakasz követelése: a szavak, amikből legalább egy kell.
     *
     * Vagylagos ágakra a két oldal követelése ÖSSZEADÓDIK – de csak akkor,
     * ha mindkettő megköveteli a magáét: ha az egyik ág bármit elfogad,
     * akkor az egész szakasz is bármit elfogad.
     */
    private static String[] koveteles(String m) {
        int[] agak = agHatarok(m);
        if (agak != null) {
            String[] ossz = URES;
            int eleje = 0;
            for (int i = 0; i <= agak.length; i++) {
                int vege = i < agak.length ? agak[i] : m.length();
                String[] r = koveteles(m.substring(eleje, vege));
                if (r.length == 0) return URES;
                ossz = osszefuz(ossz, r);
                if (ossz.length > AGAK) return URES;
                eleje = vege + 1;
            }
            return ossz;
        }
        return sorozatKovetelese(m);
    }

    /** A felső szintű `|` jelek helye, vagy null, ha nincs ilyen. */
    private static int[] agHatarok(String m) {
        int darab = 0;
        for (int menet = 0; menet < 2; menet++) {
            int[] ki = menet == 1 ? new int[darab] : null;
            int talalt = 0;
            int i = 0;
            while (i < m.length()) {
                char c = m.charAt(i);
                if (c == '\\') { i += 2; continue; }
                if (c == '[') { i = osztalyVege(m, i) + 1; continue; }
                if (c == '(') { i = csoportVege(m, i) + 1; continue; }
                if (c == '|') {
                    if (ki != null) ki[talalt] = i;
                    talalt++;
                }
                i++;
            }
            if (menet == 0) {
                if (talalt == 0) return null;
                darab = talalt;
            } else {
                return ki;
            }
        }
        return null;
    }

    private static String[] osszefuz(String[] a, String[] b) {
        String[] ki = new String[a.length + b.length];
        System.arraycopy(a, 0, ki, 0, a.length);
        System.arraycopy(b, 0, ki, a.length, b.length);
        return ki;
    }

    /**
     * Egy vagylagosság nélküli szakasz követelése: a legerősebb elem.
     *
     * Egy szakaszban minden elemnek illeszkednie kell, ezért bármelyik
     * követelését választhatjuk – a legerősebbet érdemes: a leghosszabb
     * legrövidebb szavút, azonos hosszon a rövidebb listát.
     */
    private static String[] sorozatKovetelese(String m) {
        String[] leg = URES;
        StringBuilder most = new StringBuilder();
        int i = 0;
        int n = m.length();
        while (i < n) {
            char c = m.charAt(i);
            if (c == '(') {
                int z = csoportVege(m, i);
                int utan = kvantorUtan(m, z + 1);
                boolean elhagyhato = utan != z + 1 && elhagyhatoKvantor(m, z + 1);
                int test = testKezdete(m, i, z);
                // A csoport határán a betűsor megszakad: a körültekintés nem
                // fogyaszt karaktert, a saját belseje viszont ott van a
                // szövegben – azt külön jelöltként nézzük.
                leg = jobbik(leg, most);
                most.setLength(0);
                if (!elhagyhato && test > 0) {
                    leg = jobbik(leg, koveteles(m.substring(test, z)));
                }
                i = utan;
                continue;
            }
            if (c == '[') {
                leg = jobbik(leg, most);
                most.setLength(0);
                i = kvantorUtan(m, osztalyVege(m, i) + 1);
                continue;
            }
            int v = atomVege(m, i);
            if (v < 0) {
                leg = jobbik(leg, most);
                most.setLength(0);
                // A magára maradt kapcsos zárójel egy kvantor eleje: a
                // benne álló számjegyeket ne nézzük szövegnek. („.{1,24}?"
                // kötelező szövege így lett egyszer „1,24".)
                int z = c == '{' ? m.indexOf('}', i) : -1;
                i = z < 0 ? i + 1 : z + 1;
                continue;
            }
            // Csak a magától álló, szó szerinti karakter számít: a `.`, a
            // `\d` és társaik nem betűk, az elhagyható elem pedig nem
            // kötelező.
            String atom = m.substring(i, v + 1);
            boolean szoSzerinti = c != '.'
                    && (atom.length() == 1
                        || (atom.length() == 2 && atom.charAt(0) == '\\'
                            && !Character.isLetterOrDigit(atom.charAt(1))));
            char betu = atom.charAt(atom.length() - 1);
            int utan = kvantorUtan(m, v + 1);
            boolean elhagyhato = utan != v + 1 && elhagyhatoKvantor(m, v + 1);
            if (szoSzerinti && !elhagyhato) {
                most.append(betu);
            } else {
                leg = jobbik(leg, most);
                most.setLength(0);
            }
            i = utan;
        }
        return jobbik(leg, most);
    }

    private static String[] jobbik(String[] leg, StringBuilder most) {
        if (most.length() < LEGROVIDEBB) return leg;
        return jobbik(leg, new String[]{most.toString()});
    }

    /** A két követelés közül az erősebb (üres tömb = nincs követelés). */
    private static String[] jobbik(String[] a, String[] b) {
        if (b.length == 0) return a;
        if (a.length == 0) return b;
        int ra = legrovidebb(a);
        int rb = legrovidebb(b);
        if (rb > ra) return b;
        if (rb == ra && b.length < a.length) return b;
        return a;
    }

    private static int legrovidebb(String[] a) {
        int r = Integer.MAX_VALUE;
        for (int i = 0; i < a.length; i++) r = Math.min(r, a[i].length());
        return r;
    }

    /**
     * A csoport TESTÉNEK kezdete, vagy -1, ha nem szabad belenézni.
     *
     * A tagadó körültekintés (`(?!`, `(?&lt;!`) belseje épp hogy nem lehet ott
     * a szövegben, a jelző-csoportnak (`(?s)`) pedig nincs teste.
     */
    private static int testKezdete(String m, int nyito, int zaro) {
        if (nyito + 1 >= zaro) return -1;
        if (m.charAt(nyito + 1) != '?') return nyito + 1;
        char a = m.charAt(nyito + 2);
        if (a == ':' || a == '=' || a == '>') return nyito + 3;
        if (a == '!') return -1;
        if (a == '<') {
            char b = nyito + 3 < zaro ? m.charAt(nyito + 3) : ' ';
            if (b == '!') return -1;
            if (b == '=') return nyito + 4;
            int z = m.indexOf('>', nyito + 3);
            return z > 0 && z < zaro ? z + 1 : -1;
        }
        // Jelzők: `(?s:…)` teste a kettőspont után kezdődik, a `(?s)` üres.
        int j = nyito + 2;
        while (j < zaro && "dmsuxU-".indexOf(m.charAt(j)) >= 0) j++;
        return j < zaro && m.charAt(j) == ':' ? j + 1 : -1;
    }

    /** Az `i` helyen esetleg álló kvantor utáni hely. */
    private static int kvantorUtan(String m, int i) {
        if (i >= m.length()) return i;
        char c = m.charAt(i);
        int veg;
        if (c == '*' || c == '+' || c == '?') {
            veg = i;
        } else if (c == '{') {
            veg = m.indexOf('}', i);
            if (veg < 0) return i;
        } else {
            return i;
        }
        char utana = veg + 1 < m.length() ? m.charAt(veg + 1) : ' ';
        return utana == '?' || utana == '+' ? veg + 2 : veg + 1;
    }

    /** Elhagyható-e az `i` helyen álló kvantorral az előtte álló elem? */
    private static boolean elhagyhatoKvantor(String m, int i) {
        char c = m.charAt(i);
        if (c == '*' || c == '?') return true;
        if (c != '{') return false;
        int veg = m.indexOf('}', i);
        if (veg < 0) return false;
        String belso = m.substring(i + 1, veg);
        int vesszo = belso.indexOf(',');
        return szam(vesszo < 0 ? belso : belso.substring(0, vesszo)) == 0;
    }

    /**
     * Illesztő a mintához – MINDIG új.
     *
     * Kézenfekvő volna újrahasználni őket (a létrehozás tömböket foglal a
     * csoportoknak, és mondatonként több száz keresés indul), de a
     * böngészőbeli motor `reset` hívása nem állítja vissza tisztára az
     * illesztőt: hosszú naplózó menet közben a „reggel 52-es pulzus, délben
     * futás 8 km, vacsora csirke rizzsel" mondat mellől eltűnt a pulzus –
     * ugyanaz a mondat, egy friss illesztővel, helyesen ismerődött fel.
     * Az egyezés-teszt találta meg, több ezer mondat után; egyesével
     * próbálva a hiba nem is jelentkezik. Pár ezredmásodpercet nem ér meg.
     */
    private static java.util.regex.Matcher illeszto(String minta, String s) {
        return c(minta).matcher(s);
    }

    static String ra(String s, String minta, String csere) {
        if (kihagyhato(s, minta)) return s;
        java.util.regex.Matcher mt = illeszto(minta, s);
        // Találat nélkül a csere az EGÉSZ szöveget lemásolná, holott semmi
        // nem változik. A minták túlnyomó része nem talál semmit, és a
        // fölösleges másolás volt a legdrágább egyetlen tétel a böngészőben.
        if (!mt.find()) return s;
        mt.reset();
        Pattern p = c(minta);
        try {
            return mt.replaceAll(csere);
        } catch (RuntimeException e) {
            // A böngészőbeli regex-motor nem a JVM: ha a mintában van egy
            // NEM RÉSZT VEVŐ opcionális csoport, amire a csere hivatkozik
            // („((?:fel|haromnegyed)\\s)?(\\d{1,2})…" → „$1$2-kor"), a JVM
            // üres szöveget tesz a helyére, a TeaVM viszont kivételt dob –
            // és attól az egész mondat felismerése elveszne. Ilyenkor
            // kézzel rakjuk össze a cserét, a JVM szabályai szerint.
            return kezzel(s, p.matcher(s), csere);
        }
    }

    /**
     * A csere összerakása úgy, ahogy a JVM teszi.
     *
     * A lényeg a null csoport: a JVM a nem részt vevő csoport helyére üres
     * szöveget tesz, nem hibát dob. A `\\` a következő karaktert menti, a
     * `$N` a csoportra hivatkozik – a JVM a leghosszabb ÉRVÉNYES számot
     * veszi, ezért olvassuk a jegyeket addig, amíg a csoport létezik.
     */
    private static String kezzel(String s, java.util.regex.Matcher m, String csere) {
        StringBuilder ki = new StringBuilder();
        int utolso = 0;
        while (m.find()) {
            ki.append(s, utolso, m.start());
            utolso = m.end();
            for (int i = 0; i < csere.length(); i++) {
                char c = csere.charAt(i);
                if (c == '\\' && i + 1 < csere.length()) {
                    ki.append(csere.charAt(++i));
                } else if (c == '$' && i + 1 < csere.length()
                        && csere.charAt(i + 1) >= '0' && csere.charAt(i + 1) <= '9') {
                    int szam = 0;
                    int j = i + 1;
                    while (j < csere.length()
                            && csere.charAt(j) >= '0' && csere.charAt(j) <= '9') {
                        int uj = szam * 10 + (csere.charAt(j) - '0');
                        if (uj > m.groupCount()) break;
                        szam = uj;
                        j++;
                    }
                    String g = m.group(szam);
                    if (g != null) ki.append(g);
                    i = j - 1;
                } else {
                    ki.append(c);
                }
            }
        }
        ki.append(s, utolso, s.length());
        return ki.toString();
    }

    static String rf(String s, String minta, String csere) {
        if (kihagyhato(s, minta)) return s;
        java.util.regex.Matcher mt = illeszto(minta, s);
        if (!mt.find()) return s;
        mt.reset();
        return mt.replaceFirst(csere);
    }

    static boolean m(String s, String minta) {
        if (kihagyhato(s, minta)) return false;
        return illeszto(minta, s).matches();
    }

    static String[] sp(String s, String minta) {
        // Ha az elválasztó nem fordul elő, a darabolás az egész szöveget
        // adja vissza – pontosan úgy, ahogy a keresés is tenné.
        if (kihagyhato(s, minta)) return new String[]{s};
        return c(minta).split(s, 0);
    }

    static String[] sp(String s, String minta, int hatar) {
        if (kihagyhato(s, minta)) return new String[]{s};
        return c(minta).split(s, hatar);
    }
}
