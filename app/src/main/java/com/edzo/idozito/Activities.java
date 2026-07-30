package com.edzo.idozito;

/**
 * Kézzel felvehető mozgásformák: amit nem az app mért.
 *
 * Az időzítő és a súlyzós napló csak azt látja, ami a telefonnal történt. Egy
 * kézilabda-edzés, egy uszodai óra vagy egy konditermi nap viszont ugyanúgy
 * edzés – és ha nem kerül a naplóba, akkor megszakad a széria, elmarad az XP,
 * és a statisztika kevesebbet mutat a valóságnál. Ezért lehet utólag is
 * bejegyezni edzést, és az mindenben ugyanolyan, mint egy mért.
 *
 * A kalóriabecslés a mozgásforma átlagos intenzitásából (MET) és a testsúlyból
 * jön – ugyanazzal a képlettel, amit az app máshol is használ. Ez becslés:
 * a valódi érték a tempótól és az egyéni adottságoktól is függ.
 */
public final class Activities {

    private Activities() {}

    public static final class Kind {
        public final String id, emoji, name;
        /** Átlagos intenzitás (MET) – ebből lesz a kalóriabecslés. */
        public final double met;
        /** Van-e értelme távot kérni hozzá (futás igen, kézilabda nem). */
        public final boolean distance;

        Kind(String id, String emoji, String name, double met, boolean distance) {
            this.id = id; this.emoji = emoji; this.name = name;
            this.met = met; this.distance = distance;
        }

        public String title() { return emoji + " " + name; }
    }

    /**
     * A MET-értékek a mozgásformák szokásos, közepes intenzitású átlagai
     * (Compendium of Physical Activities nagyságrendjei).
     */
    public static final Kind[] ALL = {
            new Kind("futas", "🏃", "Futás", 9.8, true),
            new Kind("uszas", "🏊", "Úszás", 7.0, true),
            new Kind("kerekpar", "🚴", "Kerékpár", 7.5, true),
            new Kind("tura", "🥾", "Túra / gyaloglás", 5.3, true),
            new Kind("evezes", "🚣", "Evezés / evezőgép", 7.0, true),
            new Kind("kondi", "🏋", "Kondi / súlyzós edzés", 5.0, false),
            new Kind("kezilabda", "🤾", "Kézilabda", 8.0, false),
            new Kind("foci", "⚽", "Foci", 7.0, false),
            new Kind("kosarlabda", "🏀", "Kosárlabda", 6.5, false),
            new Kind("roplabda", "🏐", "Röplabda", 4.0, false),
            new Kind("tenisz", "🎾", "Tenisz / squash / tollas", 7.3, false),
            new Kind("harcmuveszet", "🥋", "Harcművészet / box", 10.0, false),
            new Kind("tanc", "💃", "Tánc / aerobik", 5.5, false),
            new Kind("joga", "🧘", "Jóga / nyújtás / pilates", 3.0, false),
            new Kind("korcsolya", "⛸", "Korcsolya / görkorcsolya", 7.0, false),
            new Kind("si", "🎿", "Sí / snowboard", 6.0, false),
            new Kind("fal", "🧗", "Falmászás", 8.0, false),
            new Kind("munka", "🌳", "Kerti / fizikai munka", 4.0, false),
            new Kind("egyeb", "🤸", "Egyéb mozgás", 6.0, false),
    };

    /** A mozgásforma azonosító alapján, vagy null, ha nem ismerjük. */
    public static Kind byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Kind k : ALL) if (k.id.equals(id)) return k;
        return null;
    }

    /**
     * Táv-alapú mozgás-e? Az előzményekben ez dönti el, hogy a bejegyzés a
     * futás vagy a terem szűrőbe kerüljön. Ismeretlen (vagy hiányzó) azonosító
     * esetén false – a régi, kézi bejegyzés előtti naplók így változatlanok.
     */
    public static boolean isCardio(String id) {
        Kind k = byId(id);
        return k != null && k.distance;
    }

    /**
     * Elégetett kalória: MET × 3,5 × testsúly / 200 × perc.
     *
     * Ugyanaz a képlet, amivel az app a mért edzéseket is becsli – csak ott
     * egységesen 6-os MET-tel, mert ott nem tudjuk, milyen mozgás történt.
     * Itt tudjuk, ezért pontosabb: egy óra jóga és egy óra harcművészet nem
     * ugyanannyi.
     */
    public static double calories(Kind k, double weightKg, int minutes) {
        double w = weightKg > 0 ? weightKg : 70;
        double met = k == null ? 6.0 : k.met;
        return met * 3.5 * w / 200.0 * Math.max(0, minutes);
    }
}
