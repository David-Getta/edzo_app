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
    @Test public void theSentencesRealPeopleWriteAreUnderstood() {
        // Hatvan valódi edzés-mondattal szondáztam a felismerőt; ezek azok,
        // amiken elhasalt.

        // A spinning teremben zajlik, de kerékpározás – a tánc MET-je
        // alábecsülte a kalóriát.
        assertEquals("1d+0 h12: 1×kerekpar/50", summary("spinning 50 perc"));
        assertEquals("1d+0 h12: 1×kerekpar/45", summary("szobabicikli 45 perc"));
        // Az „N-szor" akkor is a mozgáshoz tartozik, ha ige ékelődik közé.
        // Korábban egy alkalom lett belőle: a hét kétharmada eltűnt.
        assertEquals("7d+0 h12: 3×futas/45, 2×kondi/60",
                summary("ezen a héten háromszor voltam futni és kétszer kondizni"));
        assertEquals("1d+0 h12: 2×uszas/45", summary("kétszer voltam úszni"));
        // Hiányzó mozgásformák.
        assertEquals("1d+0 h12: 1×egyeb/30", summary("elliptikus tréner fél óra"));
        assertEquals("1d+0 h12: 1×egyeb/25", summary("HIIT 25 perc"));
        assertEquals("1d+0 h12: 1×munka/60", summary("fát vágtam"));
        assertEquals("1d+0 h12: 1×si/120", summary("sielni voltam egesz nap"));
        assertEquals("1d+0 h12: 1×egyeb/60",
                summary("gyerekkel játszottam a játszótéren 1 órát"));
    }

    @Test public void aTimerProgramNameIsStillNotASport() {
        // A „HIIT" a tartalék ágon van, nem szótőként: az időzítős programok
        // nevében gyakori szó, és ott a program neve a helyes válasz.
        assertEquals(null, Activities.kindByText("Zsírégető HIIT"));
        assertEquals(null, Activities.kindByText("Intervall alap"));
    }
    @Test public void aNumberBeforeACommaBelongsToThePreviousClause() {
        // „mellnyomás 4x10 50, evezés 4x10 50" – az 50 az előző gyakorlat
        // súlya, nem ötven evezés.
        assertEquals("1d+0 h12: 1×evezes/30",
                summary("mellnyomás 4x10 50, evezés 4x10 50, bicepsz 3x12 15"));
        // Vessző nélkül a szám továbbra is darabszám.
        assertEquals("1d+0 h12: 3×uszas/45", summary("3 úszás"));
    }
}
