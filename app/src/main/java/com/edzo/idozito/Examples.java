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
            // A mennyiség-alakok is: amit ért, azt hirdesse is.
            "2-3 szelet kenyér",
            "negyed pizza",
            "dupla adag rizs",
            "tábla csoki",
            "10 szem mandula",
            "2 karéj kenyér",
            "egy evőkanál méz",
            // Ami a dobozon áll, azt elhisszük – étel-felismerés nélkül is.
            "vacsora 650 kcal",
            "müzliszelet 180 kcal",
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
            "kondi és futás, összesen másfél óra",
            "10 km-t futottam 5:30-as tempóval",
            "futás 1:05:23",
            "kirándultunk 5 órát",
            "futás és úszás 30-30 perc",
            "hegymászás 4 óra",
            "leúsztam ezerötszáz métert",
            "40 hosszt úsztam",
            "10x400 métert futottam",
            "triatlon 2 óra",
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
            // Tartás: a szám másodperc, nem ismétlés.
            "plank 3x1 perc",
            "alkartámasz 3x60",
            "fal ülés 3x40 mp",
            "4 sorozat 8 fekvenyomás",
            "román felhúzás 3x8 80 kg",
            // Az érzett terhelés rövidítései: RIR és a kg utáni @szám.
            "guggolás 3x10 100 kg rir 2",
            "fekvenyomás 5x5 90 kg @8",
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
            "45 másodperc munka 15 pihenő nyolcszor",
            // A forma neve is elég: a ritmust hozzáadjuk.
            "hiit 20 perc",
            "e2mom 20 perc",
            "norvég 4x4",
    };

    /** Testsúly- és testzsír-mérés egy mondatból. */
    public static final String[] BODY = {
            "ma reggel 78,4 kg",
            "78 kiló vagyok",
            "mérleg: 81,2",
            "78,4 kg és 18% testzsír",
            "testsúly 80,5 kg",
            "18% testzsír",
            "reggel 79",
            "85 kiló lettem",
            // A mérőszalag is: derék, csípő, mellkas, comb, kar.
            "derék 84 cm",
            "78 kg, 18% testzsír, derék 84 cm",
            // A pihenés is idetartozik: az alvás és a reggeli pulzus a
            // Profil naplója.
            "aludtam 8 órát",
            "nyugalmi pulzus 52",
    };

    /** Edzésnap egy sorban – megosztáshoz és felvételhez. */
    public static final String[] ROUTINE = {
            "Lábnap: guggolás, lábtolás, kitörés, vádliemelés",
            "Tolónap: fekvenyomás, vállból nyomás, tricepsz",
            "Húzónap: húzódzkodás, evezés, bicepsz",
            "Hátnap: húzódzkodás, evezés, lehúzás, csuklyás emelés",
            "Teljes test: guggolás, fekvenyomás, evezés, plank",
            "guggolás, fekvenyomás, felhúzás",
            "Törzsnap: plank / fal-ülés / hasprés",
    };

    /** Panasz vagy megelőző cél egy mondatban – a rehab-sorok ajtaja. */
    public static final String[] REHAB = {
            "fáj a vállam",
            "beállt a derekam",
            "kificamodott a bokám",
            "golfkönyök fájdalom",
            "húzódik a combom",
            "sajog a térdem",
            "fáj a talpam",
            "fáj a térdem külső oldala",
            "csukló mobilizálás",
            // A skálát is ki lehet mondani – az érték rögtön a naplóba kerül.
            "fáj a vállam 6/10",
            // A cél-alak is: nem kell megvárni, hogy fájjon.
            "boka stabilitás",
            "váll mobilizálás",
            "nyak gyógytorna",
    };

    /**
     * A könyvtár mondat-csoportjai: cím, alcím, lista-kulcs.
     *
     * Itt van, nem a képernyőn, mert a fejléc darabszáma is ebből jön – egy új
     * csoport felvételekor a „4 helyen" különben csendben hazuggá válna –, és
     * mert így egységteszt is átfuthat rajta.
     */
    public static final String[][] GROUPS = {
            {"🍽  Étrend", "Mit ettél?", "MEAL"},
            {"📝  Edzés-előzmény", "Több edzés egy mondatból", "BULK"},
            {"🏋️  Erősítő sorozatok", "Gyakorlat, sorozat, súly", "SET"},
            {"⏱  Időzítő", "Kör, munka, pihenő", "INTERVAL"},
            {"⚖️  Mérés és pihenés", "Testsúly, testzsír, körfogat, alvás, pulzus", "BODY"},
            {"📅  Edzésnap", "Név és gyakorlatok, vesszővel", "ROUTINE"},
            {"🩹  Panasz és megelőzés", "Mi fáj – vagy mit erősítenél? Kész sort ajánlok", "REHAB"},
    };

    /** A könyvtár csoportjai névvel hivatkoznak a listákra. */
    public static String[] byKey(String key) {
        if ("MEAL".equals(key)) return MEAL;
        if ("BULK".equals(key)) return BULK;
        if ("SET".equals(key)) return SET;
        if ("BODY".equals(key)) return BODY;
        if ("ROUTINE".equals(key)) return ROUTINE;
        if ("REHAB".equals(key)) return REHAB;
        return INTERVAL;
    }

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
