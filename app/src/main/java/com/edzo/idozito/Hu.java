package com.edzo.idozito;

import java.util.Locale;

/**
 * Magyar számformázás egy helyen.
 *
 * Az app szövege magyar, a tizedeselválasztó tehát vessző: „5,2 km", nem
 * „5.2 km". Ez korábban szétcsúszott – a víz és a napi kihívás vesszővel írt,
 * a táv, a tempó, a BMI és a testsúly viszont ponttal.
 *
 * IDŐ-formátumokhoz szándékosan NEM ezt használjuk: ott nincs tizedes, viszont
 * a rögzített (US) locale garantálja az ASCII számjegyeket. A CSV-export is
 * marad ponttal, hogy bármelyik táblázatkezelő és nyelv beolvassa.
 */
public final class Hu {

    private Hu() {}

    public static final Locale LOCALE = new Locale("hu");

    /** Egy tizedes, magyarul: „5,2". */
    public static String d1(double v) {
        return String.format(LOCALE, "%.1f", v);
    }

    /** Két tizedes, magyarul: „5,23". */
    public static String d2(double v) {
        return String.format(LOCALE, "%.2f", v);
    }
}
