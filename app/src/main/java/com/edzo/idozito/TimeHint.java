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

    /** Hány nappal ezelőttre utal a mondat (0 = ma). */
    static int daysBack(String s, long now) {
        if (s.contains("tegnapelott")) return 2;
        if (s.contains("tegnap")) return 1;

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
            if (!s.contains(DAYS[i])) continue;
            return (todayIdx - i + 7) % 7;
        }
        return 0;
    }

    /** Áll-e délutáni/esti napszak-jelző az óraszám előtt? */
    private static boolean afternoonBefore(String s, int at) {
        String head = s.substring(0, Math.max(0, at));
        return head.contains("delutan") || head.contains("este") || head.contains("esti")
                || head.contains("vacsora") || head.contains("uzsonna")
                || head.contains("ejjel") || head.contains("ejszaka");
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
        if (s.contains("reggel")) return 8;
        if (s.contains("tizorai")) return 10;
        if (s.contains("ebed") || s.contains("delben")) return 13;
        if (s.contains("uzsonna") || s.contains("delutan")) return 16;
        if (s.contains("vacsora") || s.contains("este")) return 19;
        if (s.contains("ejjel") || s.contains("ejszaka")) return 22;
        return -1;
    }
}
