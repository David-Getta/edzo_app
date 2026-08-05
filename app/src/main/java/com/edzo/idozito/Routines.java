package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;

/**
 * Edzésnapok: melyik gyakorlatokat csinálod egy szuszra.
 *
 * Az app eddig gyakorlatonként gondolkodott: mit nyomj ma ebből az egy
 * gyakorlatból. A teremben viszont senki nem egy gyakorlatot csinál – van egy
 * lábnapja, egy tolónapja, és azt ismétli hétről hétre. A napot eddig fejben
 * kellett tartani, és minden gyakorlatot külön kikeresni.
 *
 * A tárolt alak szándékosan egyszerű szöveg (`név|gyak1;gyak2|név|…`), hogy egy
 * beállítás-kulcsba beférjen, és a mentés ne tudjon félig sikerülni. A
 * gyakorlatneveket NEM ellenőrizzük a felismerő listája ellen: aki saját nevet
 * ír, annak is működnie kell.
 *
 * Tiszta Java (nincs Context), hogy egységteszttel lefedhető legyen.
 */
public final class Routines {

    private Routines() {
    }

    /** Ennél több gyakorlat egy napra már nem edzés, hanem lista. */
    public static final int MAX_MOVES = 12;

    /** Ennél több edzésnapot nem tárolunk. */
    public static final int MAX_ROUTINES = 12;

    /** A név ennél hosszabb nem fér ki a gombra. */
    public static final int MAX_NAME = 24;

    public static final class Routine {
        public final String name;
        public final List<String> moves;

        Routine(String name, List<String> moves) {
            this.name = name; this.moves = moves;
        }

        /** „Tolónap  ·  4 gyakorlat”. */
        public String label() {
            return name + "  ·  " + moves.size() + " gyakorlat";
        }

        /**
         * Rövid felsorolás értesítéshez: „Guggolás · Lábtolás · Kitörés +2”.
         *
         * Egy hat gyakorlatos nap teljes névsora az értesítésben levágódik –
         * a levágott vég pedig rosszabb, mint a tudatos rövidítés.
         */
        public String shortSummary(int max) {
            if (max < 1) max = 1;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < moves.size() && i < max; i++) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append(moves.get(i));
            }
            if (moves.size() > max) sb.append(" +").append(moves.size() - max);
            return sb.toString();
        }

        /** „Fekvenyomás · Vállból nyomás · Tricepsz”. */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            for (String m : moves) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append(m);
            }
            return sb.toString();
        }
    }

    /**
     * Beépített edzésnapok. A klasszikus felosztások, a felismerő saját
     * gyakorlatneveivel – így a progresszió-javaslat és a rekordok is
     * megtalálják őket.
     */
    public static List<Routine> builtIn() {
        List<Routine> out = new ArrayList<>();
        out.add(of("Tolónap", "Fekvenyomás", "Vállból nyomás", "Tolódzkodás", "Tricepsz"));
        out.add(of("Húzónap", "Felhúzás", "Húzódzkodás", "Evezés", "Bicepsz"));
        out.add(of("Lábnap", "Guggolás", "Lábtolás", "Kitörés", "Combhajlítás", "Vádliemelés"));
        out.add(of("Felsőtest", "Fekvenyomás", "Evezés", "Vállból nyomás", "Lehúzás",
                "Bicepsz", "Tricepsz"));
        out.add(of("Alsótest", "Guggolás", "Felhúzás", "Kitörés", "Lábnyújtás", "Csípőemelés"));
        out.add(of("Teljes test", "Guggolás", "Fekvenyomás", "Evezés", "Plank"));
        return out;
    }

    private static Routine of(String name, String... moves) {
        List<String> m = new ArrayList<>();
        for (String s : moves) m.add(s);
        return new Routine(name, m);
    }

    /**
     * Tárolt alak beolvasása. Hibás sorok némán kimaradnak: egy elrontott
     * beállítás ne vigye magával a többi edzésnapot.
     */
    public static List<Routine> parse(String stored) {
        List<Routine> out = new ArrayList<>();
        if (stored == null || stored.trim().isEmpty()) return out;
        String[] parts = stored.split("\\|");
        for (int i = 0; i + 1 < parts.length; i += 2) {
            String name = clean(parts[i]);
            if (name.isEmpty()) continue;
            List<String> moves = new ArrayList<>();
            for (String m : parts[i + 1].split(";")) {
                String t = clean(m);
                if (!t.isEmpty() && !moves.contains(t) && moves.size() < MAX_MOVES) moves.add(t);
            }
            if (moves.isEmpty()) continue;
            boolean dup = false;
            for (Routine r : out) if (r.name.equalsIgnoreCase(name)) dup = true;
            if (dup || out.size() >= MAX_ROUTINES) continue;
            out.add(new Routine(name, moves));
        }
        return out;
    }

    /** Tárolható alak. A szeparátorok a nevekből kiesnek. */
    public static String format(List<Routine> routines) {
        if (routines == null || routines.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Routine r : routines) {
            if (r == null || r.moves.isEmpty() || n >= MAX_ROUTINES) continue;
            String name = clean(r.name);
            if (name.isEmpty()) continue;
            if (sb.length() > 0) sb.append('|');
            sb.append(name).append('|');
            int k = 0;
            for (String m : r.moves) {
                String t = clean(m);
                if (t.isEmpty() || k >= MAX_MOVES) continue;
                if (k > 0) sb.append(';');
                sb.append(t);
                k++;
            }
            n++;
        }
        return sb.toString();
    }

    /**
     * Edzésnap hozzáadása vagy felülírása név szerint.
     *
     * Az azonos nevű nap CSERÉLŐDIK, nem duplázódik: aki ugyanazt a nevet írja
     * be újra, az javítani akar, nem két egyforma napot csinálni.
     */
    public static String add(String stored, String name, List<String> moves) {
        String n = clean(name);
        if (n.length() > MAX_NAME) n = n.substring(0, MAX_NAME).trim();
        if (n.isEmpty() || moves == null) return stored == null ? "" : stored;
        List<String> clean = new ArrayList<>();
        for (String m : moves) {
            String t = clean(m);
            if (!t.isEmpty() && !clean.contains(t) && clean.size() < MAX_MOVES) clean.add(t);
        }
        if (clean.isEmpty()) return stored == null ? "" : stored;
        List<Routine> list = parse(stored);
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).name.equalsIgnoreCase(n)) { list.remove(i); break; }
        list.add(0, new Routine(n, clean));
        return format(list);
    }

    /** Edzésnap törlése név szerint. */
    public static String remove(String stored, String name) {
        String n = clean(name);
        List<Routine> list = parse(stored);
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).name.equalsIgnoreCase(n)) { list.remove(i); break; }
        return format(list);
    }

    /** A megadott nevű edzésnap, vagy null. A beépítettek is beleszámítanak. */
    public static Routine byName(String stored, String name) {
        String n = clean(name);
        if (n.isEmpty()) return null;
        for (Routine r : all(stored)) if (r.name.equalsIgnoreCase(n)) return r;
        return null;
    }

    /**
     * Minden választható edzésnap: elöl a sajátok, utánuk a beépítettek.
     *
     * A saját név elnyomja az azonos nevű beépítettet – aki átírja a
     * „Lábnap"-ot, a sajátját akarja látni, nem kettőt.
     */
    public static List<Routine> all(String stored) {
        List<Routine> out = new ArrayList<>(parse(stored));
        for (Routine b : builtIn()) {
            boolean dup = false;
            for (Routine r : out) if (r.name.equalsIgnoreCase(b.name)) dup = true;
            if (!dup) out.add(b);
        }
        return out;
    }

    /**
     * Mikor csináltad utoljára ezt az edzésnapot? (Napokban; -1 = soha.)
     *
     * Egy nap akkor számít megcsináltnak, ha a gyakorlatainak legalább a FELE
     * szerepel aznap. Teljes egyezést kérni életszerűtlen: mindig kimarad
     * valami, és akkor a nap sosem lenne „megcsinálva".
     *
     * @param moves az edzésnap gyakorlatai
     * @param ts    bejegyzésenként az időbélyeg
     * @param names bejegyzésenként a gyakorlat neve
     * @param now   a mostani idő
     */
    public static int lastDone(List<String> moves, long[] ts, String[] names, long now) {
        if (moves == null || moves.isEmpty() || ts == null || names == null) return -1;
        int n = Math.min(ts.length, names.length);
        // Naponként: hány gyakorlata volt meg ennek a napnak?
        java.util.LinkedHashMap<Long, java.util.Set<String>> perDay = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            if (names[i] == null) continue;
            String name = names[i].trim();
            boolean mine = false;
            for (String m : moves) if (m.equalsIgnoreCase(name)) mine = true;
            if (!mine) continue;
            long day = Days.startOf(ts[i]);
            java.util.Set<String> set = perDay.get(day);
            if (set == null) perDay.put(day, set = new java.util.LinkedHashSet<String>());
            set.add(name.toLowerCase());
        }
        int need = (moves.size() + 1) / 2;
        int best = -1;
        for (java.util.Map.Entry<Long, java.util.Set<String>> e : perDay.entrySet()) {
            if (e.getValue().size() < need) continue;
            int ago = Days.ago(e.getKey(), now);
            if (ago < 0) continue;
            if (best < 0 || ago < best) best = ago;
        }
        return best;
    }

    /** „ma", „tegnap", „4 napja" – vagy üres, ha soha. */
    public static String lastDoneLabel(int daysAgo) {
        if (daysAgo < 0) return "";
        if (daysAgo == 0) return "ma";
        if (daysAgo == 1) return "tegnap";
        return daysAgo + " napja";
    }

    /** A szeparátorok és a sortörés kiesnek, a szóközök összeérnek. */
    private static String clean(String s) {
        if (s == null) return "";
        return s.replace('|', ' ').replace(';', ' ').replace('\n', ' ')
                .replaceAll("\\s+", " ").trim();
    }
}
