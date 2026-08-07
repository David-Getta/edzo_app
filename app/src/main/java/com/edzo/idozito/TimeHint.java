package com.edzo.idozito;

import java.util.Calendar;

/**
 * Mikor történt? Az időpont a mondatból – étkezéshez és súlyzós sorozathoz
 * egyaránt.
 *
 * A „tegnap este pizzát ettem" eddig is a tegnapi napra került, de csak a
 * „tegnap" és a „tegnapelőtt" szavakat ismerte, és csak az étrendben. Az
 * edzés-felismerő közben régóta érti a hétköznapneveket és a „3 napja"
 * alakot is – ugyanaz a felhasználó ugyanúgy fogalmaz, akár ételről, akár
 * edzésről ír.
 *
 * Tiszta Java (nincs Context), hogy egységteszttel lefedhető legyen: egy
 * rossz nap a napi összesítőt két helyen is elrontja (ahonnan elveszi, és
 * ahová beteszi).
 */
public final class TimeHint {

    private TimeHint() {
    }

    /** Ennél régebbre nem teszünk vissza semmit: az „5 hete" nem utólagos pótlás. */
    static final int MAX_BACK = 14;

    /** Hétfőtől vasárnapig, ékezet nélkül; a ragozott alak is illeszkedik. */
    private static final String[] DAYS = {
            "hetfo", "kedd", "szerda", "csutortok", "pentek", "szombat", "vasarnap"
    };

    /**
     * @param text a beírt mondat
     * @param now  a mostani idő
     * @return a bejegyzés időbélyege; ha a mondat nem utal korábbi napra és
     *         napszakra sem, akkor pontosan `now` – a mostani pillanat a
     *         legpontosabb adat, amink van
     */
    public static long from(String text, long now) {
        // Kiírt számok számjeggyé: a „két napja" ugyanazt jelenti, mint a
        // „2 napja" – eddig csak az utóbbi kelt át a felismerőn.
        String s = Hu.digits(Foods.norm(text == null ? "" : text));
        int back = daysBack(s, now);
        int hour = hourOf(s);
        if (back == 0 && hour < 0) return now;

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        if (back > 0) c.add(Calendar.DAY_OF_YEAR, -back);
        // A kimondott napszak felülírja az órát; ha nincs kimondva, a múltbeli
        // nap delet kap – nem a mostani órát, mert az azt sugallná, hogy pont
        // most, csak három napja.
        c.set(Calendar.HOUR_OF_DAY, hour >= 0 ? hour : 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        // A kimondott napszak akkor is érvényes, ha még nem járunk ott: aki
        // délben azt írja, „ma este pizza", a saját estéjéről beszél. A
        // felhasználó állítását nem írjuk felül az órával.
        return c.getTimeInMillis();
    }

    /** Hónapnevek, ékezet nélkül; a rövidítés is illeszkedik (a hosszabb elöl). */
    private static final String[] MONTHS = {
            "januar", "februar", "marcius", "aprilis", "majus", "junius",
            "julius", "augusztus", "szeptember", "oktober", "november", "december"
    };
    private static final String[] MONTHS_SHORT = {
            "jan", "feb", "marc", "apr", "maj", "jun",
            "jul", "aug", "szept", "okt", "nov", "dec"
    };

    /**
     * Konkrét dátum hónapnévvel: „július 30-án", „aug. 1-jén".
     *
     * A pótlás legpontosabb alakja – aki napokkal később ír be egy ebédet,
     * gyakran a dátumot mondja, nem azt, hogy „hat napja".
     *
     * @return hány nappal ezelőtt volt, vagy 0, ha nincs ilyen dátum a
     *         mondatban (vagy kívül esik a pótlás ablakán)
     */
    static int monthDayBack(String s, long now) {
        int num = numericDateBack(s, now);
        if (num > 0) return num;
        for (int mi = 0; mi < 12; mi++) {
            int p = s.indexOf(MONTHS[mi]);
            int len = MONTHS[mi].length();
            if (p < 0) { p = s.indexOf(MONTHS_SHORT[mi]); len = MONTHS_SHORT[mi].length(); }
            if (p < 0) continue;
            // A szó eleje legyen: a „majus" ne találjon a szó belsejében.
            if (p > 0 && Character.isLetter(s.charAt(p - 1))) continue;
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^[\\s.]*(\\d{1,2})").matcher(s.substring(p + len));
            if (!m.find()) continue;
            int day;
            try { day = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException e) { continue; }
            if (day < 1 || day > 31) continue;
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(now);
            Calendar t = Calendar.getInstance();
            t.setTimeInMillis(now);
            t.set(Calendar.MONTH, mi);
            t.set(Calendar.DAY_OF_MONTH, day);
            t.set(Calendar.HOUR_OF_DAY, 12);
            t.set(Calendar.MINUTE, 0);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);
            // A jövőbe eső dátum tavalyi: december 30-át január 2-án írva a
            // múlt évre gondolt az ember.
            if (t.getTimeInMillis() > now) t.add(Calendar.YEAR, -1);
            int back = Days.ago(t.getTimeInMillis(), now);
            if (back >= 1 && back <= MAX_BACK) return back;
        }
        return 0;
    }

    /**
     * Számmal írt dátum: „2026.07.28", „07.28.", „07.28-án".
     *
     * A hónapnevet már értettük, a számalakot nem – pedig a naptárból és a
     * telefonról ez másolódik ki. A csupasz „07.28" szándékosan NEM elég:
     * pont így néz ki egy tizedes szám is („1.5 kg"), és egy félreolvasott
     * dátum két napi összesítőt ront el. Kell mellé záró pont, ragozás vagy
     * évszám.
     */
    static int numericDateBack(String s, long now) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?<![\\d.])(?:(\\d{4})[.] ?(\\d{1,2})[.] ?(\\d{1,2})[.]?(?![\\d])"
                        + "|(\\d{1,2})[.] ?(\\d{1,2})(?:[.](?![\\d])|-(?:a|e)n))")
                .matcher(s);
        while (m.find()) {
            boolean withYear = m.group(1) != null;
            int mon, day, year = 0;
            try {
                mon = Integer.parseInt(m.group(withYear ? 2 : 4));
                day = Integer.parseInt(m.group(withYear ? 3 : 5));
                if (withYear) year = Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) { continue; }
            if (mon < 1 || mon > 12 || day < 1 || day > 31) continue;
            Calendar t = Calendar.getInstance();
            t.setTimeInMillis(now);
            if (withYear) t.set(Calendar.YEAR, year);
            t.set(Calendar.MONTH, mon - 1);
            t.set(Calendar.DAY_OF_MONTH, day);
            t.set(Calendar.HOUR_OF_DAY, 12);
            t.set(Calendar.MINUTE, 0);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);
            // Évszám nélkül a jövőbe eső dátum tavalyi: december 30-át
            // január 2-án írva a múlt évre gondolt az ember.
            if (!withYear && t.getTimeInMillis() > now) t.add(Calendar.YEAR, -1);
            int back = Days.ago(t.getTimeInMillis(), now);
            if (back >= 1 && back <= MAX_BACK) return back;
        }
        return 0;
    }

    /** Hány nappal ezelőttre utal a mondat (0 = ma). */
    static int daysBack(String s, long now) {
        if (has(s, "tegnapelott")) return 2;
        if (has(s, "tegnap")) return 1;

        int md = monthDayBack(s, now);
        if (md > 0) return md;

        // „egy hete", „két héttel ezelőtt": hét nap egy hét.
        java.util.regex.Matcher wm = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s?h(?:e|é)t(?:e|tel)")
                .matcher(s);
        if (wm.find()) {
            try {
                int v = Integer.parseInt(wm.group(1));
                if (v >= 1 && v * 7 <= MAX_BACK) return v * 7;
            } catch (NumberFormatException ignored) {
            }
        }

        // „3 napja", „3 nappal ezelőtt".
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s?nap(?:ja|pal)")
                .matcher(s);
        if (m.find()) {
            try {
                int v = Integer.parseInt(m.group(1));
                if (v >= 1 && v <= MAX_BACK) return v;
            } catch (NumberFormatException ignored) {
            }
        }

        // Hétköznapnév: a LEGUTÓBBI ilyen nap. A mai napnevet nem tekintjük
        // visszalépésnek – aki „hétfőn" ír hétfőn, az a mai napra gondol.
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        int todayIdx = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;   // H=0..V=6
        for (int i = 0; i < 7; i++) {
            if (!has(s, DAYS[i])) continue;
            int back = (todayIdx - i + 7) % 7;
            // A „múlt kedden" egy héttel korábbi keddet jelent, nem a mostanit.
            // Itt TELJES szó kell: a „multivitamin" nem múlt hét.
            if ((hasWord(s, "mult") || has(s, "elozo")) && back + 7 <= MAX_BACK) back += 7;
            return back;
        }
        // Napnév nélküli „múlt héten", „előző héten": a hét távolabbi vége.
        // Közelítés – de a MAI dátum biztosan rossz, és egy rossz nap két
        // napi összesítőt ront el: ahonnan elveszi, és ahová beteszi.
        if ((hasWord(s, "mult") || has(s, "elmult") || has(s, "elozo")) && has(s, "het"))
            return 7;
        return 0;
    }

    /**
     * Szó ELEJÉN álló szótő.
     *
     * A magyar toldalék hozzáragad („estére", „hétfőn", „ebédnél”), ezért a
     * végét nem kötjük meg – a szó BELSEJÉBE eső egyezés viszont majdnem
     * mindig téves. A „fejjel lefelé" nem éjjel, a „fájt a teste" nem este, a
     * „multivitamin" nem múlt hét. Egy ilyen találat csendben egy másik napra
     * vagy másik napszakra viszi a bejegyzést.
     */
    private static boolean has(String s, String stem) {
        int p = s.indexOf(stem);
        while (p >= 0) {
            if (p == 0 || !Character.isLetter(s.charAt(p - 1))) return true;
            p = s.indexOf(stem, p + 1);
        }
        return false;
    }

    /** Ugyanaz, de a szó VÉGÉT is megköti – toldalék nélküli alakokhoz. */
    private static boolean hasWord(String s, String word) {
        int p = s.indexOf(word);
        while (p >= 0) {
            int e = p + word.length();
            if ((p == 0 || !Character.isLetter(s.charAt(p - 1)))
                    && (e >= s.length() || !Character.isLetter(s.charAt(e)))) return true;
            p = s.indexOf(word, p + 1);
        }
        return false;
    }

    /** Áll-e délutáni/esti napszak-jelző az óraszám előtt? */
    private static boolean afternoonBefore(String s, int at) {
        String head = s.substring(0, Math.max(0, at));
        return has(head, "delutan") || has(head, "este") || has(head, "esti")
                || has(head, "vacsora") || has(head, "uzsonna")
                || has(head, "ejjel") || has(head, "ejszaka");
    }

    /** A kimondott napszak órája, vagy -1, ha nincs. */
    static int hourOf(String s) {
        // Pontos óra: „19 órakor", „19:30-kor". A perc nem érdekes: az időpont
        // amúgy is becslés, az óra viszont a napszakot rögzíti.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})(?::\\d{2})?\\s?(?:ora(?:kor)?|-kor)")
                .matcher(s);
        if (m.find()) {
            try {
                int h = Integer.parseInt(m.group(1));
                if (h >= 0 && h <= 23) {
                    // A napszak igazít: délután nincs négy óra. A kimondott
                    // óra pontosabb a napszaknál, de a 12 alatti szám a
                    // délutáni jelző után délutánt jelent.
                    if (h < 12 && afternoonBefore(s, m.start())) h += 12;
                    return h;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (has(s, "hajnal")) return 5;
        if (has(s, "reggel")) return 8;
        if (has(s, "tizorai") || has(s, "delelott")) return 10;
        if (has(s, "ebed") || has(s, "delben")) return 13;
        if (has(s, "uzsonna") || has(s, "delutan")) return 16;
        if (has(s, "vacsora") || has(s, "este")) return 19;
        if (has(s, "ejjel") || has(s, "ejszaka")) return 22;
        return -1;
    }
}
