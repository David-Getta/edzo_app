package com.edzo.idozito;

/**
 * Az appban hirdetett példamondatok – egy helyen.
 *
 * A beviteli mezők tippjei tanítanak: aki elolvassa, azt hiszi, hogy pont
 * úgy is beírhatja. Ha egy példa közben elromlik (átnevezünk egy ételt,
 * szigorítunk egy mintán), akkor az app maga bíztat valamire, amit aztán nem
 * ismer fel – ennél kevés bosszantóbb dolog van. Ezért a mondatok nem az
 * Activity-kben lapulnak, hanem itt, Context nélkül, hogy a teszt mindet
 * végig tudja futtatni a felismerőkön.
 */
public final class Examples {

    private Examples() {
    }

    /** Étkezés – a „Mit ettél?” mezőbe. */
    public static final String[] MEAL = {
            "150 g csirkemell rizzsel",
            "2 tojás és egy pirítós vajjal",
            "fél adag gyros",
            "tegnap este pizzát ettem",
            "két korsó sör és egy hamburger",
            "negyvenöt gramm sajt",
            "ittam fél liter vizet",
            "két és fél deci tej müzlivel",
            "spenót főzelék fasírttal",
            "ebédre poke bowl",
            "két tányér gulyás",
            "chips helyett almát ettem",
            "banán 2 db",
            "százhúsz gramm csirkemell",
            "sonkás-sajtos szendvics",
            "július 30-án tortát ettem",
    };

    /** Több edzés egy mondatból – az előzmények tömeges felvitelénél. */
    public static final String[] BULK = {
            "az elmúlt 3 nap alatt 3 futó edzés és 6 kézi edzés",
            "kétszer úsztam a héten",
            "tegnap 10 km futás 50 perc alatt",
            "hétfőn 1 óra 15 perc kondi",
            "leúsztam 1500 métert",
            "egy hónap alatt 10 edzés",
            "a héten minden nap futottam",
            "hétvégén 1-1 túra",
            "tegnap este kondi",
            "100 fekvőtámasz",
            "ma 10000 lépés",
            "lábnap volt, 1 óra",
            "padel 90 perc",
            "hétfőn és szerdán kondi",
            "kedden úszás, csütörtökön futás",
            "3x10 fekvenyomás 60 kg",
            "futottam háromszor a héten",
            "1h20 futás",
            "reggel 5 km futás, este 8 km futás",
            "ma reggel 6-kor futottam",
            "huszonöt kilométer bringa",
    };

    /** Erősítő sorozatok egy mondatból. */
    public static final String[] SET = {
            "3x10 fekvenyomás 60 kg",
            "guggolás 5x5 80 kg",
            "húzódzkodás 3x8",
            "bicepsz 12-10-8 15 kg",
            "guggolás 3x10 60 kg, fekvenyomás 3x8 50 kg",
            "vállból nyomás 3 sorozat 12 ismétlés 20 kg",
            "lábgép 3x12 80 kg és vádli 4x15",
            "arnold nyomás 3x10 16 kg, oldalemelés 3x15 8 kg",
            "guggolás 3x10x60",
            "guggolás 3x10 100 kg rpe 8",
            "tegnap húzódzkodás 4x8",
            "fekvenyomás 60x10, 70x8, 80x6",
            "guggolás 5x5 hetvenöt kiló",
            "3 kör 10 fekvőtámasz",
            // Az újabb alakok is szerepeljenek: amit ért, azt hirdesse is.
            "fekvenyomás 60x10 70x8 80x6",
            "guggolás 5,5,5 @ 100",
            "fekvenyomás max 120 kg",
            "ma guggoltam, 5 sorozat, 5 ismétlés, 100 kg",
            "kettlebell swing 5x20 24 kg",
    };

    /** Intervallum-beállítás egy mondatból. */
    public static final String[] INTERVAL = {
            "3 kör 40 mp munka 20 mp pihenő",
            "8x20/10",
            "tabata",
            "5 kör 1 perc munka 30 másodperc pihenő",
            "emom 10 perc",
            "45/15 x 6",
            "10 kör 30 mp munka, 15 mp pihenő",
            "2 perc bemelegítés, 6 kör 40/20",
            "1:30 munka 0:30 pihenő 6 kör",
            "40 mp / 20 mp, 10 kör",
            "10x(40s/20s)",
            "emom 12",
            "amrap 20 perc",
            "kör: 6, munka: 40mp, pihenő: 20mp",
            "8 kör: 20 mp sprint, 40 mp séta",
    };

    /** Percenként másik példa, hogy ne egyetlen formát tanuljon meg a szem. */
    public static String pick(String[] a, long now) {
        long i = (now / 60000L) % a.length;
        if (i < 0) i += a.length;
        return a[(int) i];
    }

    /** „pl. …” alakban, beviteli mező tippjének. */
    public static String hint(String[] a, long now) {
        return "pl. " + pick(a, now);
    }

    /** Az étkezés mezőnél a kérdés is ott van a tipp elején. */
    public static String mealHint(long now) {
        return "Mit ettél? (pl. " + pick(MEAL, now) + ")";
    }
}
