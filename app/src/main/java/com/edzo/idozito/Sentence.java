package com.edzo.idozito;

import java.util.List;

/**
 * Melyik naplóba való ez a mondat?
 *
 * Az app négy helyen ért mondatot – étrend, edzés-előzmény, erősítő sorozat,
 * időzítő –, de mindegyik csak a sajátját. Aki az étkezés-mezőbe írja be, hogy
 * „30 perc futás", annyit kap: „ezt még nem ismerem". Pedig ismeri, csak egy
 * képernyővel odébb. A mondat jó volt, csak rossz ajtón kopogott.
 *
 * Ez az osztály megmondja, hova való – hogy a képernyő, amelyik nemet mondott,
 * legalább útba tudja igazítani. Szándékosan csak akkor kérdezzük meg, ha a
 * saját felismerő már üresen tért vissza: a találgatás sosem előzheti meg azt,
 * amit a képernyő maga biztosan tud.
 */
public final class Sentence {

    private Sentence() {}

    /** Hova való a mondat. NONE = egyik felismerő sem tud vele mit kezdeni. */
    public enum Kind { NONE, MEAL, WORKOUT, STRENGTH, INTERVAL }

    /** Az átirányított mondat Intent-kulcsa: a cél-képernyő ezzel nyílik meg. */
    public static final String EXTRA = "sentence";

    /**
     * A mondat legvalószínűbb helye.
     *
     * A sorrend nem önkényes: a sorozatos mondat („3x10 fekvenyomás") a
     * legfajsúlyosabb, mert benne a súly és az ismétlés is megvan, és ezt CSAK
     * az erősítő napló őrzi meg. Utána a megtörtént edzés jön, mert az
     * időzítő-terv ugyanazokból a számokból áll („4 kör 40 mp"), de a napló
     * felismerője mozgásformát is kér hozzá – ha az megvan, akkor az edzés
     * volt, nem terv. Az étel a végén: a legrövidebb szótövek itt vannak, és
     * ezek akadnak be legkönnyebben egy edzés-mondatba.
     *
     * @param foods a felismerhető ételek (a sajátokkal együtt), vagy null
     */
    public static Kind of(String q, List<Foods.Food> foods, long now) {
        if (q == null || q.trim().length() < 3) return Kind.NONE;
        if (!StrengthParse.parse(q).isEmpty()) return Kind.STRENGTH;
        Activities.Parsed a = Activities.parse(q, now);
        if (a != null && !a.isEmpty()) return Kind.WORKOUT;
        if (IntervalParse.parse(q) != null) return Kind.INTERVAL;
        if (foods != null && !Foods.parse(foods, q).isEmpty()) return Kind.MEAL;
        return Kind.NONE;
    }

    /** A napló neve, ahová a mondat való („Erősítő napló"). */
    public static String where(Kind k) {
        switch (k) {
            case MEAL: return "Étrend";
            case WORKOUT: return "Edzés-előzmények";
            case STRENGTH: return "Erősítő napló";
            case INTERVAL: return "Időzítő";
            default: return "";
        }
    }

    /**
     * Egysoros útbaigazítás a rossz képernyőn – koppintható felirathoz.
     *
     * A hangnem szándékosan nem mentegetőzik: a felhasználó nem hibázott,
     * csak egy ajtóval odébb van, amit keres.
     */
    public static String hint(Kind k) {
        switch (k) {
            case MEAL: return "🍽 Ez inkább étkezésnek tűnik – koppints, és az Étrendbe viszem.";
            case WORKOUT: return "🏃 Ez inkább edzésnek tűnik – koppints, és az Előzményekbe viszem.";
            case STRENGTH: return "🏋️ Ez erősítő sorozatnak tűnik – koppints, és az Erősítő naplóba viszem.";
            case INTERVAL: return "⏱️ Ez időzítő-tervnek tűnik – koppints, és beállítom.";
            default: return "";
        }
    }
}
