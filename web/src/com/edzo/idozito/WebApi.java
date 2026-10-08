package com.edzo.idozito;

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.JSExport;

/**
 * A JavaScript oldal EGYETLEN belépője a magyar felismeréshez.
 *
 * Az iPhone-os (webes) változat ugyanazt a felismerést használja, mint az
 * Android app: ezek az osztályok szándékosan tiszta Javák, és a webes build
 * UGYANEBBŐL a forrásból fordul – nem egy kézzel átírt másolatból. Így egy
 * felismerés-javítás mindkét felületen egyszerre jelenik meg, és a 2000+
 * egységteszt mindkettőt őrzi.
 *
 * A határfelület szándékosan szűk és szöveges: egy mondat megy be, egy JSON
 * jön vissza. Így a JS oldalnak nem kell ismernie a Java osztályokat, és a
 * két világ között nincs törékeny objektum-átjárás.
 */
public final class WebApi {

    private WebApi() { }

    /** A TeaVM-nek kell egy belépő; a munkát az exportált metódusok végzik. */
    public static void main(String[] args) { }

    /**
     * Egy mondat teljes felismerése JSON-ban.
     *
     * @param text a felhasználó mondata
     * @param nowMs a mai időpont ezredmásodpercben (a „tegnap”, „hétfőn”
     *              és a napszak ehhez képest értendő)
     */
    @JSExport
    public static String parse(String text, double nowMs) {
        // Egyetlen fura mondat ne dönthesse le a felismerést: a naplózó
        // mező minden leütésre hív minket, és ha itt kivétel szabadul el,
        // a felhasználó csak annyit lát, hogy „nem ismerek fel semmit" –
        // vagy rosszabb esetben megáll a felület. A hibát megnevezve adjuk
        // vissza, hogy a jelentésekből kiderüljön, mi történt.
        try {
            return parseVagyHiba(text, (long) nowMs);
        } catch (Throwable e) {
            return "{\"kind\":\"NONE\",\"extras\":[],\"hiba\":"
                    + str(hibaSzoveg(e))
                    + ",\"mozgas\":{\"napok\":1,\"eltolas\":0,\"ora\":12,"
                    + "\"napjai\":null,\"tetelek\":[]},\"ero\":[],\"etel\":[],"
                    + "\"test\":{\"kg\":0,\"zsir\":0,\"cm\":[0,0,0,0,0]},"
                    + "\"alvas\":-1,\"pulzus\":-1,\"kcal\":-1,\"egetett\":-1,"
                    + "\"feherje\":-1,\"intervall\":null}";
        }
    }

    /** A hiba neve és az első pár hívás – ennyi kell a diagnózishoz. */
    private static String hibaSzoveg(Throwable e) {
        StringBuilder o = new StringBuilder(e.getClass().getName());
        if (e.getMessage() != null) o.append(": ").append(e.getMessage());
        StackTraceElement[] v = e.getStackTrace();
        for (int i = 0; i < v.length && i < 6; i++) o.append(" | ").append(v[i]);
        return o.toString();
    }

    private static String parseVagyHiba(String text, long nowArg) {
        long now = nowArg;
        List<Foods.Food> db = new ArrayList<>();
        for (Foods.Food f : Foods.ALL) db.add(f);
        StringBuilder o = new StringBuilder("{");
        o.append("\"kind\":").append(str(Sentence.of(text, db, now).name()));
        o.append(",\"extras\":[");
        boolean first = true;
        for (Sentence.Kind k : Sentence.extras(text, db, now)) {
            if (!first) o.append(',');
            o.append(str(k.name()));
            first = false;
        }
        o.append(']');
        o.append(",\"mozgas\":").append(moves(text));
        o.append(",\"ero\":").append(lifts(text));
        o.append(",\"etel\":").append(foods(db, text));
        o.append(",\"test\":").append(body(text));
        o.append(",\"alvas\":").append(num(Sleep.parse(text)));
        o.append(",\"pulzus\":").append(Pulse.parse(text));
        o.append(",\"kcal\":").append(Kcal.stated(text));
        o.append(",\"egetett\":").append(Kcal.burned(text));
        o.append(",\"feherje\":").append(Kcal.protein(text));
        o.append(",\"intervall\":").append(interval(text));
        return o.append('}').toString();
    }

    /** A példatár csoportostul – a webes súgó ugyanazt mutatja, mint az app. */
    @JSExport
    public static String library() {
        StringBuilder o = new StringBuilder("[");
        for (int g = 0; g < Examples.GROUPS.length; g++) {
            String[] row = Examples.GROUPS[g];
            if (g > 0) o.append(',');
            o.append("{\"cim\":").append(str(row[0]));
            o.append(",\"alcim\":").append(str(row[1]));
            o.append(",\"peldak\":[");
            String[] ex = Examples.byKey(row[2]);
            for (int i = 0; i < ex.length; i++) {
                if (i > 0) o.append(',');
                o.append(str(ex[i]));
            }
            o.append("]}");
        }
        return o.append(']').toString();
    }

    private static String moves(String text) {
        Activities.Parsed p = Activities.parse(text);
        StringBuilder o = new StringBuilder("{\"napok\":").append(p.days);
        o.append(",\"eltolas\":").append(p.offset);
        o.append(",\"ora\":").append(p.hour);
        // A megnevezett napok („hétfőn és szerdán") alkalmankénti eltolása.
        // Enélkül a webes napló mindent a mai napra tenne, és a heti
        // összesítés attól csúszna el, hogy melyik nap mi történt.
        o.append(",\"napjai\":");
        if (p.exactDays == null) {
            o.append("null");
        } else {
            o.append('[');
            for (int i = 0; i < p.exactDays.length; i++) {
                if (i > 0) o.append(',');
                o.append(p.exactDays[i]);
            }
            o.append(']');
        }
        o.append(",\"tetelek\":[");
        for (int i = 0; i < p.plans.size(); i++) {
            Activities.Plan pl = p.plans.get(i);
            if (i > 0) o.append(',');
            o.append("{\"id\":").append(str(pl.kind.id));
            o.append(",\"nev\":").append(str(pl.kind.name));
            o.append(",\"emoji\":").append(str(pl.kind.emoji));
            o.append(",\"met\":").append(num(pl.kind.met));
            o.append(",\"alkalom\":").append(pl.count);
            o.append(",\"perc\":").append(pl.minutes);
            o.append(",\"km\":").append(num(pl.km));
            o.append(",\"lepes\":").append(pl.steps);
            o.append(",\"cimke\":").append(str(pl.label()));
            o.append('}');
        }
        return o.append("]}").toString();
    }

    private static String lifts(String text) {
        StringBuilder o = new StringBuilder("[");
        List<StrengthParse.Item> items = StrengthParse.parse(text);
        for (int i = 0; i < items.size(); i++) {
            StrengthParse.Item it = items.get(i);
            if (i > 0) o.append(',');
            o.append("{\"nev\":").append(str(it.name));
            o.append(",\"rpe\":").append(it.rpe);
            o.append(",\"cimke\":").append(str(it.label()));
            o.append(",\"sorozatok\":[");
            for (int j = 0; j < it.sets.size(); j++) {
                StrengthParse.Set s = it.sets.get(j);
                if (j > 0) o.append(',');
                o.append("{\"ism\":").append(s.reps)
                        .append(",\"suly\":").append(num(s.weight)).append('}');
            }
            o.append("]}");
        }
        return o.append(']').toString();
    }

    private static String foods(List<Foods.Food> db, String text) {
        StringBuilder o = new StringBuilder("[");
        List<Foods.Hit> hits = Foods.parse(db, text);
        for (int i = 0; i < hits.size(); i++) {
            Foods.Hit h = hits.get(i);
            if (i > 0) o.append(',');
            double g = h.grams > 0 ? h.grams : h.food.portion;
            o.append("{\"nev\":").append(str(h.food.name));
            o.append(",\"gramm\":").append(num(g));
            o.append(",\"kimondott\":").append(h.grams > 0);
            o.append(",\"kcal\":").append(Math.round(g * h.food.kcal100 / 100.0));
            o.append(",\"feherje\":").append(num(Math.round(g * h.food.prot100 / 10.0) / 10.0));
            o.append('}');
        }
        return o.append(']').toString();
    }

    private static String body(String text) {
        BodyParse.Body b = BodyParse.parse(text);
        StringBuilder o = new StringBuilder("{\"kg\":").append(num(b.kg));
        o.append(",\"zsir\":").append(num(b.fatPct));
        o.append(",\"cm\":[");
        for (int i = 0; i < b.cm.length; i++) {
            if (i > 0) o.append(',');
            o.append(num(b.cm[i]));
        }
        return o.append("]}").toString();
    }

    private static String interval(String text) {
        IntervalParse.Plan p = IntervalParse.parse(text);
        if (p == null) return "null";
        return "{\"kor\":" + p.rounds + ",\"munka\":" + p.work + ",\"piheno\":" + p.rest
                + ",\"bemelegites\":" + p.warm + ",\"levezetes\":" + p.cool
                + ",\"talalgatott\":" + p.guessed + "}";
    }

    /** Egész számot egészként írunk ki: a „8.0 km” csúnya és félrevezető. */
    private static String num(double v) {
        if (v == Math.rint(v) && !Double.isInfinite(v))
            return String.valueOf((long) v);
        return String.valueOf(Math.round(v * 100.0) / 100.0);
    }

    private static String str(String s) {
        if (s == null) return "null";
        StringBuilder o = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') o.append('\\').append(c);
            else if (c == '\n') o.append("\\n");
            else if (c == '\r') o.append("\\r");
            else if (c == '\t') o.append("\\t");
            else if (c < 0x20) o.append(String.format("\\u%04x", (int) c));
            else o.append(c);
        }
        return o.append('"').toString();
    }
}
