package com.edzo.idozito;

/**
 * Terhelés-ugrás: mennyivel tér el az e heti edzésmennyiség attól, amit a
 * szervezet az elmúlt hetekben megszokott.
 *
 * A lelkesedés a leggyakoribb sérülésforrás. Aki hetekig heti 90 percet
 * mozgott, majd egy héten 300-at, az nem „jobban edz" – az egy 3,3-szoros
 * ugrás, amit az ín és a kötőszövet nem követ. A hüvelykujjszabály az elmúlt
 * 7 nap terhelését az azt megelőző 4 hét HETI ÁTLAGÁHOZ méri.
 *
 * Szándékosan tiszta Java (nincs Context), hogy egységteszttel lefedhető
 * legyen: itt egy elszámolás vagy fölöslegesen ijesztget, vagy – ami rosszabb –
 * elhallgat egy valódi ugrást.
 */
public final class Load {

    private Load() {
    }

    /** Ennyi napra visszamenőleg nézzük az alapterhelést (4 hét). */
    public static final int CHRONIC_DAYS = 28;
    /** Az „aktuális" ablak: az elmúlt hét. */
    public static final int ACUTE_DAYS = 7;

    /**
     * Ennél kevesebb heti alapterhelésnél (perc) nincs mihez mérni: aki
     * gyakorlatilag nem mozgott, annál egy 20 perces séta is „végtelenszeres
     * ugrás" lenne, és pont őt riasztanánk el a kezdéstől.
     */
    static final double MIN_BASE_MIN = 30;

    /** Sávhatárok az arányra. */
    static final double LOW = 0.8, HIGH = 1.3, RISK = 1.5;

    public static final int RESTING = -1, STEADY = 0, PUSHING = 1, JUMP = 2;

    public static final class Ratio {
        /** Az elmúlt 7 nap összterhelése percben. */
        public final double acute;
        /** Az azt megelőző 4 hét heti átlaga percben. */
        public final double chronic;
        /** acute / chronic; 0, ha nincs elég előzmény. */
        public final double ratio;
        /** RESTING / STEADY / PUSHING / JUMP, vagy STEADY ha nincs adat. */
        public final int level;
        /** Van-e egyáltalán értelmes összehasonlítás. */
        public final boolean known;

        Ratio(double acute, double chronic, double ratio, int level, boolean known) {
            this.acute = acute; this.chronic = chronic; this.ratio = ratio;
            this.level = level; this.known = known;
        }

        /** Rövid címke, pl. „1,4× a szokásos". */
        public String label() {
            if (!known) return "Még gyűlik az alap";
            return Hu.d1(ratio) + "× a szokásos";
        }

        /** Egy mondat arról, mit kezdjen ezzel. */
        public String advice() {
            if (!known)
                return "Néhány hét után tudom megmondani, mennyire tér el egy hét a "
                        + "megszokottól.";
            switch (level) {
                case JUMP:
                    return "Ez a hét jóval többet hozott a megszokottnál. A következő héten "
                            + "vegyél vissza, vagy legalább ne told tovább – a sérülések "
                            + "többsége ilyen ugrás után jön.";
                case PUSHING:
                    return "Szépen emelkedsz. Heti 10–20 százaléknál nagyobb ugrást ne "
                            + "vállalj, és tarts egy könnyebb napot.";
                case RESTING:
                    return "Ez a hét könnyebb volt a szokásosnál. Ha szándékos, tökéletes – "
                            + "a pihenőhét is edzés.";
                default:
                    return "A terhelésed a megszokott sávban van. Ez az a tempó, amit a "
                            + "szervezet be tud fogadni.";
            }
        }

        /** Jelzés a kártyára. */
        public String emoji() {
            if (!known) return "📊";
            switch (level) {
                case JUMP: return "⚠️";
                case PUSHING: return "📈";
                case RESTING: return "🌙";
                default: return "✅";
            }
        }
    }

    /**
     * @param daily napi terhelés percben, daily[0] = ma, daily[1] = tegnap, …
     *              A tömb lehet rövidebb 35 napnál; a hiányzó napok nullák.
     */
    public static Ratio of(double[] daily) {
        double acute = 0, base = 0;
        int baseDays = 0;
        if (daily != null) {
            for (int i = 0; i < daily.length && i < ACUTE_DAYS; i++)
                acute += Math.max(0, daily[i]);
            for (int i = ACUTE_DAYS; i < daily.length && i < ACUTE_DAYS + CHRONIC_DAYS; i++) {
                base += Math.max(0, daily[i]);
                baseDays++;
            }
        }
        // Az alapot mindig teljes 4 hétre vetítjük: a rövidebb előzmény ne
        // duzzassza fel az átlagot (2 hét adatból nem lesz 4 hetes szokás).
        double chronic = base / 4.0;
        boolean known = baseDays >= 14 && chronic >= MIN_BASE_MIN;
        if (!known) return new Ratio(acute, chronic, 0, STEADY, false);

        double r = acute / chronic;
        int level = r >= RISK ? JUMP : r > HIGH ? PUSHING : r < LOW ? RESTING : STEADY;
        return new Ratio(acute, chronic, r, level, true);
    }

    /**
     * Egy súlyzós nap becsült perce a sorozatszámból: sorozatonként ~3 perc
     * (végrehajtás + pihenő). A napló nem tárol időtartamot, de ha a súlyzós
     * napokat nullának vennénk, akkor pont annál lenne vak a terhelés-figyelés,
     * aki csak vasalja magát.
     */
    public static double strengthMinutes(int sets) {
        if (sets <= 0) return 0;
        return Math.min(120, Math.max(15, sets * 3.0));
    }

    /**
     * Napi percek tömbje időbélyegekből. A hívó a naplóból adja a párokat;
     * ami a mai naptól számítva `days`-nél régebbi, kimarad.
     *
     * @param ts      a bejegyzések időbélyegei
     * @param minutes az adott bejegyzés hossza percben (azonos hosszú tömb)
     * @param now     a mai nap bármely pillanata
     */
    public static double[] daysFrom(long[] ts, double[] minutes, long now, int days) {
        double[] out = new double[days];
        if (ts == null || minutes == null) return out;
        int n = Math.min(ts.length, minutes.length);
        for (int i = 0; i < n; i++) {
            int d = Days.ago(ts[i], now);
            if (d >= 0 && d < days) out[d] += Math.max(0, minutes[i]);
        }
        return out;
    }
}
