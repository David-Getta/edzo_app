package com.edzo.idozito;

import android.content.Context;

/**
 * „Blaze", a Grit tűzfarkas kabalafigurája. Duolingo-stílusú társ: állapotfüggő,
 * lelkesítő üzeneteket ad a kezdőképernyőn és az értesítésekben. A karakter
 * hangja: energikus, kicsit vad, de bátorító – falkaszellem + tűz.
 */
public final class Mascot {

    private Mascot() {}

    public static final String NAME = "Blaze";
    public static final String FACE = "🐺";   // tűzfarkas (a 🔥-t a keret/üzenet adja)

    /** Hangulat-emoji az avatar sarkába, az aktuális állapot szerint. */
    public static String mood(boolean today, int dayStreak, boolean streakRisk) {
        if (today && dayStreak >= 3) return "🔥";
        if (today) return "😄";
        if (streakRisk) return "😤";
        return "🐺";
    }

    private static String who(String userName) {
        return (userName == null || userName.trim().isEmpty()) ? "falkatárs" : userName.trim();
    }

    /** Fő buzdító sor Blaze hangján a kezdőképernyőre. */
    public static String line(Context c, String userName, int totalWorkouts,
                              boolean today, int dayStreak, int weekStreak,
                              boolean streakRisk, int hour, boolean restDay) {
        String u = who(userName);
        if (totalWorkouts == 0)
            return "Szia, Blaze vagyok! 🐺🔥 Csináljuk meg az első edzésed – a falka veled van!";
        if (restDay && !today) {
            String[] rest = {
                    "Ma pihenőnap, " + u + " – a regeneráció is edzés! 🌙🐺 Holnap újra hajtunk.",
                    "Pihenőnap, " + u + "! 🌙 Egy kis nyújtás vagy mobilitás azért jólesne. 🧘🐺",
                    "Ma töltekezünk, " + u + " – aludj, egyél, nyújts! 🌙💪 A falka holnap újra fut.",
            };
            return rest[(int) (System.currentTimeMillis() / 3600000 % rest.length)];
        }
        if (today && dayStreak >= 3)
            return dayStreak + " napos széria, " + u + "! 🔥 Égsz, mint a láng – ne állj meg!";
        if (today)
            return "Ma már letudtad, " + u + "! 💪 Büszke vagyok rád, falkatárs.";
        if (streakRisk)
            return "Vigyázz a szériádra, " + u + "! 🔥 Ne hagyd kihunyni a lángot – egy rövid kör is elég!";
        if (hour >= 17)
            return "Még nem mozogtál ma… 🐺 Gyújtsuk be a lángot egy gyors edzéssel! 🔥";
        String[] day = {
                "Készen állsz, " + u + "? 🐺 Egy kis GRIT, és kész az edzés!",
                "A falka vár! 🔥 Csapjunk bele egy körbe.",
                "Ma is legyőzzük a tegnapi éned! 💪🐺",
                "Egy kör most, büszkeség egész nap! 🔥",
                "A láng nem gyúl meg magától, " + u + " – csiholjuk! 🐺🔥",
        };
        return day[(int) (System.currentTimeMillis() / 60000 % day.length)];
    }

    /** Koppintásra cserélődő rövid biztatások. */
    private static final String[] PEP = {
            "Nincs kifogás, csak GRIT! 🔥",
            "Egy lépés ma többet ér, mint egy terv holnap. 🐺",
            "A fájdalom elmúlik, a büszkeség marad. 💪",
            "Lassú haladás is haladás – ne állj meg! 🔥",
            "A falka együtt erős. Hajrá! 🐺",
            "Te irányítasz, nem a lustaság. 💪🔥",
            "Morogj rá a lustaságra – aztán mozdulj! 🐺🔥",
            "Az erő nem adottság – megszerzed. 💪",
            "Minden kör egy tégla a jobbik énedhez. 🧱🔥",
            "Nem kell tökéletesnek lenni, csak elkezdeni. 🐺",
            "A verejték ma a mosoly holnap. 😤🔥",
            "A kényelem zóna szép hely, de ott nem nő semmi. 🌵",
            "Egy kihívás egy nap – és a farkas jóllakik. 🎯🐺",
            "A tested bírja. A fejed dönti el. 🧠🔥",
    };

    public static String pep() {
        return PEP[(int) (Math.random() * PEP.length)];
    }

    /** Belépéskori üdvözlés + motiváció Blaze hangján (napszakhoz igazodva). */
    public static String greeting(String userName, int hour) {
        String u = who(userName);
        String hi = hour < 10 ? "Jó reggelt" : hour < 18 ? "Szia" : "Jó estét";
        String[] motiv = {
                "ma is legyőzzük a tegnapi éned! 💪🔥",
                "a falka veled van – csináljuk! 🐺",
                "egy kis GRIT, és kész a mai edzés! 🔥",
                "ne feledd: a kitartás legyőz mindent! 💪",
                "gyújtsuk be a napot egy jó edzéssel! 🔥🐺",
                "a legjobb időpont az edzésre: most! ⏱🔥",
                "minden nap egy új esély erősödni! 🐺💪",
        };
        String m = motiv[(int) (Math.random() * motiv.length)];
        return hi + ", " + u + "! 🐺🔥 Blaze vagyok – " + m;
    }

    /** Visszavágó-hívás, ha tegnap elmaradt egy tervezett edzésnap. */
    public static String comeback(String userName) {
        String u = who(userName);
        String[] cb = {
                "Tegnap kimaradt az edzés, " + u + " – semmi baj, ma visszavágunk! 🐺🔥",
                "A tegnapi nap elszaladt, " + u + "… de a mai a miénk! 💪🔥",
                "Egy kihagyott nap nem tör meg, " + u + " – ma duplán ég a láng! 🔥🐺",
        };
        return cb[(int) (System.currentTimeMillis() / 3600000 % cb.length)];
    }

    /** Heti terv állása edzés után: ünneplés vagy a hátralévő napok. */
    public static String planStatus(String userName, boolean planComplete, int futureRemaining) {
        String u = who(userName);
        if (planComplete)
            return "Heti terv kész, " + u + "! 🏆🐺 Minden edzésnapot teljesítettél – büszke a falka!";
        return "Ma megvolt, " + u + "! 💪 Még " + futureRemaining + " edzésnap van hátra a héten – tartsd a tempót! 🔥";
    }

    /** Dicséret az edzés befejező képernyőjére, széria- és mérföldkő-tudatosan. */
    public static String praiseFinish(String userName, int dayStreak, int totalWorkouts) {
        String u = who(userName);
        if (totalWorkouts == 1)
            return "Az első edzésed, " + u + "! 🐺🎉 Ezt sose felejted el – a falka büszke rád!";
        if (totalWorkouts == 10 || totalWorkouts == 25 || totalWorkouts == 50 || totalWorkouts == 100)
            return totalWorkouts + ". edzés, " + u + "! 🏆🔥 Ez már nem szerencse – ez GRIT!";
        if (dayStreak == 7)
            return "Egy teljes hét megállás nélkül, " + u + "! 🗓️🔥 A láng már magától lobog!";
        if (dayStreak == 14)
            return "Két hét minden nap, " + u + "! 💎 Ez már nem szokás – ez életmód.";
        if (dayStreak == 30)
            return "30 napos széria, " + u + "! 👑🔥 Gyémánt rutin – a falka legendája vagy!";
        if (dayStreak == 50)
            return "50 nap megállás nélkül, " + u + "! 🌋 Ez már nem láng – ez vulkán!";
        if (dayStreak == 100)
            return "SZÁZ NAP, " + u + "! 🐺👑🔥 A falka történelmet ír veled!";
        if (dayStreak >= 7)
            return dayStreak + " napja minden nap, " + u + "! 🔥🔥 Te vagy a falka lángja!";
        if (dayStreak >= 3)
            return dayStreak + " napos széria, " + u + "! 🔥 Egyre erősebb a lángod – így tovább!";
        String[] p = {
                "Ez az, " + u + "! 💪 Blaze büszkén vonyít: aúúú! 🐺",
                "Kipipálva, " + u + "! 🔥 A mai éned legyőzte a tegnapit.",
                "Szép munka, " + u + "! 🐺 A falka veled ünnepel!",
                "Megcsináltad, " + u + "! 💪🔥 Ez a kitartás visz előre.",
                "Újabb győzelem, " + u + "! 🏅 A láng ma is fellobbant. 🔥",
                "Ez GRIT volt, " + u + "! 🐺🔥 Holnap találkozunk!",
        };
        return p[(int) (Math.random() * p.length)];
    }

    /** Értesítés-szöveg Blaze hangján (proaktív emlékeztetőhöz). */
    public static String nudge(String userName, boolean streakRisk, int dayStreak) {
        String u = who(userName);
        if (streakRisk && dayStreak >= 1)
            return "Ne hagyd kihunyni a " + dayStreak + " napos lángod, " + u + "! 🔥 Egy rövid edzés is számít.";
        String[] n = {
                "Hé, " + u + "! 🐺 A falka téged vár – gyújtsuk be a mai edzést! 🔥",
                "Blaze itt! 🔥 Ideje egy kis GRIT-nek. Menni fog!",
                "Mozdulj meg ma, " + u + "! 💪 Egy gyors kör, és büszke leszel magadra. 🐺",
                "Aúúú! 🐺 A mai edzés még hiányzik – pár perc, és megvan! 🔥",
                "Ne hagyd, hogy a kanapé nyerjen, " + u + "! 💪 Egy rövid kör is győzelem.",
        };
        return n[(int) (System.currentTimeMillis() / 60000 % n.length)];
    }
}
