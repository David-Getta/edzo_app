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
