package com.edzo.idozito;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Kombinált mondatok: az egyes képességek (dátum, napszak, táv, méter, lépés,
 * ismétlés, osztó számnév, gyakoriság) EGYÜTT is jól működnek-e. Minden sor
 * egy életszerű, több elemet keverő bevitel – rögzített „mával" (2026. július
 * 31., péntek dél), hogy a naptárfüggő részek is determinisztikusak legyenek.
 */
public class ActivitiesIntegrationTest {

    private static long friday() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(2026, java.util.Calendar.JULY, 31, 12, 0, 0);
        return c.getTimeInMillis();
    }

    private static String summary(String text) {
        Activities.Parsed p = Activities.parse(text, friday());
        StringBuilder sb = new StringBuilder();
        for (Activities.Plan pl : p.plans) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pl.count).append("×").append(pl.kind.id).append("/").append(pl.minutes);
            if (pl.km > 0) sb.append("/").append(pl.km).append("km");
        }
        return p.days + "d+" + p.offset + " h" + p.hour + ": " + sb;
    }

    @Test public void combinedSentencesParseAsAWhole() {
        // Napszak + táv + idő + ismétlés egy mondatban: az 50 perc a futásé,
        // a fekvőtámasz a maga ismétlés-becslését kapja, nem a futás idejét.
        assertEquals("1d+1 h19: 1×futas/50/10.0km, 1×kondi/20",
                summary("tegnap este 10 km futás 50 perc alatt és 100 fekvőtámasz"));
        // Dátum + kiírt tört óra.
        assertEquals("1d+3 h12: 1×kerekpar/150",
                summary("július 28-án két és fél óra bringa"));
        // Lépés + másik mozgás.
        assertEquals("1d+0 h12: 1×kondi/30, 1×tura/77/7.5km",
                summary("ma 10000 lépés és fél óra kondi"));
        // Időszak + gyakoriság + ragozott ige + tört óra.
        assertEquals("14d+0 h12: 14×kondi/30",
                summary("az elmúlt 2 hétben naponta gyúrtam fél órát"));
        // Napszak + méter + idő.
        assertEquals("1d+1 h16: 1×uszas/40/1.5km",
                summary("tegnap délután leúsztam 1500 métert 40 perc alatt"));
        // Hétköznapnév + sorozat×ismétlés + táv.
        assertEquals("1d+3 h12: 1×kondi/6, 1×futas/30/5.0km",
                summary("kedden 3x10 guggolás és 5 km kocogás"));
        // Hétköznapnév + maraton-táv + idő.
        assertEquals("1d+5 h12: 1×futas/120/21.1km",
                summary("vasárnap félmaraton 2 óra alatt"));
        // Hétvége (pénteken írva) + osztó számnév + szorzós ige.
        assertEquals("2d+5 h12: 1×tura/90, 2×uszas/45",
                summary("hétvégén 1-1 túra és kétszer úsztam"));
    }
}
