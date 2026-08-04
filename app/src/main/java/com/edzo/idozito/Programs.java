package com.edzo.idozito;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Edzésprogramok: névvel ellátott gyakorlatsorok (körökben ismételve).
 * Beépített programok + a felhasználó saját programjai (SharedPreferences-ben).
 */
public final class Programs {

    private Programs() {}

    static final String PREFS = "edzo";
    static final String KEY = "programs";

    public static final class P {
        public final String name;
        public final String emoji;
        public final String[] ex;
        public final boolean custom;
        public P(String name, String emoji, String[] ex, boolean custom) {
            this.name = name; this.emoji = emoji; this.ex = ex; this.custom = custom;
        }
        public String title() { return emoji + " " + name; }
    }

    /** Beépített programok. */
    public static final P[] BUILT_IN = {
            new P("Törzs-has", "💪", new String[]{
                    "Plank", "Hasprés", "Lábemelés", "Orosz csavarás", "Hegymászó", "Bicikli hasprés"}, false),
            new P("Teljes test", "🏋️", new String[]{
                    "Jumping jack", "Guggolás", "Fekvőtámasz", "Kitörés", "Plank", "Burpee"}, false),
            new P("Láb és fenék", "🦵", new String[]{
                    "Guggolás", "Kitörés", "Csípőemelés (híd)", "Oldalkitörés", "Vádliemelés", "Fal-ülés"}, false),
            new P("Kar és váll", "💥", new String[]{
                    "Fekvőtámasz", "Tricepsz tolódzkodás", "Plank vállérintés", "Karkörzés", "Szuperman", "Szűk fekvőtámasz"}, false),
            new P("Nyújtás", "🧘", new String[]{
                    "Nyakkörzés", "Vállnyújtás", "Törzsdöntés", "Combhajlító nyújtás", "Csípőnyújtás", "Vádlinyújtás"}, false),
            new P("Zsírégető HIIT", "🔥", new String[]{
                    "Jumping jack", "Magas térd", "Burpee", "Hegymászó", "Deszka ugrás", "Boxoló ütés"}, false),
            new P("Mag & egyensúly", "🎯", new String[]{
                    "Plank", "Oldalplank", "Madár-kutya", "Csípőemelés (híd)", "Szuperman", "Fal-ülés"}, false),
            new P("Reggeli mobilitás", "🌅", new String[]{
                    "Nyakkörzés", "Karkörzés", "Törzsdöntés", "Szuperman", "Csípőemelés (híd)", "Guggolás"}, false),
            // A klasszikus tudományos „7 perces edzés" – 12 gyakorlat, 30 mp / 10 mp pihenő.
            new P("7 perces edzés", "⏳", new String[]{
                    "Jumping jack", "Fal-ülés", "Fekvőtámasz", "Hasprés", "Fellépés székre", "Guggolás",
                    "Tricepsz tolódzkodás", "Plank", "Magas térd", "Kitörés", "Forgó fekvőtámasz", "Oldalplank"}, false),
            // Kezdőknek: kíméletes, ugrás nélküli teljes testes bevezető.
            new P("Kezdő teljes test", "🔰", new String[]{
                    "Guggolás", "Fal-ülés", "Csípőemelés (híd)", "Plank", "Karkörzés", "Vádliemelés"}, false),
    };

    /** Saját programok betöltése. */
    public static List<P> loadCustom(Context c) {
        List<P> out = new ArrayList<>();
        String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                JSONArray ea = o.optJSONArray("ex");
                if (ea == null || ea.length() == 0) continue;
                String[] ex = new String[ea.length()];
                for (int j = 0; j < ea.length(); j++) ex[j] = ea.optString(j, "Gyakorlat");
                out.add(new P(o.optString("name", "Saját program"), "⭐", ex, true));
            }
        } catch (Exception ignored) {}
        return out;
    }

    static void saveCustom(Context c, List<P> list) {
        JSONArray a = new JSONArray();
        for (P p : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("name", p.name);
                JSONArray ea = new JSONArray();
                for (String e : p.ex) ea.put(e);
                o.put("ex", ea);
                a.put(o);
            } catch (Exception ignored) {}
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }

    public static void addCustom(Context c, String name, String[] ex) {
        List<P> list = loadCustom(c);
        list.add(new P(name, "⭐", ex, true));
        saveCustom(c, list);
    }

    public static void removeCustom(Context c, String name) {
        List<P> list = loadCustom(c);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.equals(name)) { list.remove(i); break; }
        }
        saveCustom(c, list);
    }

    /** Az összes program (beépített + saját). */
    public static List<P> all(Context c) {
        List<P> out = new ArrayList<>();
        for (P p : BUILT_IN) out.add(p);
        out.addAll(loadCustom(c));
        return out;
    }

    /** Program keresése név alapján; null, ha nincs (= sima futás mód). */
    public static P byName(Context c, String name) {
        if (name == null || name.isEmpty()) return null;
        for (P p : all(c)) if (p.name.equals(name)) return p;
        return null;
    }

    /** Rövid gyakorlat-leírás (technikai tipp); üres, ha nincs (pl. saját gyakorlat). */
    public static String descOf(String name) {
        if (name == null) return "";
        switch (name) {
            case "Plank": return "Alkartámasz, egyenes törzs, feszes has – ne lógjon a csípő.";
            case "Hasprés": return "Hanyatt, kezek a tarkón, emeld a lapockát, húzd be a hasad.";
            case "Lábemelés": return "Hanyatt, nyújtott lábak, emeld 90°-ig, lassan engedd.";
            case "Orosz csavarás": return "Ülő V-tartás, forgasd a törzsed oldalról oldalra.";
            case "Hegymászó": return "Fekvőtámasz-tartás, húzd a térded felváltva a mellkashoz.";
            case "Bicikli hasprés": return "Hanyatt, könyök–ellenkező térd érintés, tekerj.";
            case "Jumping jack": return "Szökdelés terpeszbe, karok fel, ütemesen.";
            case "Guggolás": return "Vállszéles terpesz, csípőből le, térd a lábfej felett.";
            case "Fekvőtámasz": return "Egyenes test, könyök 45°, mellkas le a földig.";
            case "Kitörés": return "Nagy lépés előre, mindkét térd 90°, nyomj vissza.";
            case "Burpee": return "Guggolás, kilépés plankbe, fekvőtámasz, ugrás fel.";
            case "Csípőemelés (híd)": return "Hanyatt, talpak a földön, emeld a csípőd, szorítsd a farizmot.";
            case "Oldalkitörés": return "Lépés oldalra, egyik térd hajlik, a másik láb nyújtva.";
            case "Vádliemelés": return "Állj lábujjhegyre, feszítsd a vádlit, lassan engedd.";
            case "Fal-ülés": return "Háttal a falnak, comb vízszintes, mint egy láthatatlan szék.";
            case "Tricepsz tolódzkodás": return "Kéztámasz pad/szék szélén, hajlítsd a könyököd, nyomj fel.";
            case "Plank vállérintés": return "Plank-tartás, érintsd felváltva a vállad, ne billegj.";
            case "Karkörzés": return "Nyújtott karok, kis körök előre, majd hátra.";
            case "Szuperman": return "Hason fekve emeld a kart és lábat, feszítsd a hátad.";
            case "Szűk fekvőtámasz": return "Kezek közel egymáshoz, a tricepszet dolgoztatja.";
            case "Nyakkörzés": return "Lassú, kontrollált körzés – ne told túl a tartományt.";
            case "Vállnyújtás": return "Húzd a kart a mellkas előtt keresztbe, tartsd.";
            case "Törzsdöntés": return "Állva vagy ülve döntsd a törzsed oldalra, nyújtsd az oldalad.";
            case "Combhajlító nyújtás": return "Nyújtott láb, hajolj előre, érintsd a lábfejed felé.";
            case "Csípőnyújtás": return "Kitörés-tartásban told előre a csípőd, nyújtsd a hajlítót.";
            case "Vádlinyújtás": return "Fal felé dőlve nyújtott hátsó láb, sarok a földön.";
            case "Magas térd": return "Helyben futás, húzd a térded csípőmagasságig, gyors ütem.";
            case "Deszka ugrás": return "Plank-tartás, ugrálj terpeszbe és zárt lábra a lábaddal.";
            case "Boxoló ütés": return "Enyhe guggolóállás, üss előre gyorsan, felváltva a karokkal.";
            case "Oldalplank": return "Oldalfekvés alkartámaszon, egyenes test, tartsd a csípőd fenn.";
            case "Madár-kutya": return "Négykézláb, nyújtsd ki az ellentétes kart és lábat, tartsd.";
            case "Fellépés székre": return "Lépj fel egy stabil székre/lépcsőre, váltott lábbal, kontrolláltan.";
            case "Forgó fekvőtámasz": return "Fekvőtámasz, majd fordulj oldalra, nyújtsd a felső kart a plafon felé.";
            // Súlyzós alapgyakorlatok (az Erősítő napló felkínált nevei).
            case "Fekvenyomás": return "Lapockák hátra és le, rúd a mellkas közepére, könyök kb. 45°-ban. Ne pattintsd meg a mellkasodon.";
            case "Felhúzás": return "Rúd a lábközépnél, egyenes hát, mellkas fel. Nem húzod: a lábaddal nyomod el magad a talajtól.";
            case "Vállból nyomás": return "Feszes has és farizom, fej egy kicsit hátra a rúd elől, nyomás egyenesen fel. Ne told előre a bordáidat.";
            case "Evezés": return "Törzs döntve, hát végig egyenes, húzd a rudat a hasfalhoz, lapockák hátra.";
            case "Bicepsz": return "Könyök a törzs mellett marad, ne lendíts a derékkal, lefelé lassíts.";
            case "Tricepsz": return "A felkar rögzítve, csak az alkar mozog. A végén teljes nyújtás, de ne feszítsd túl a könyököd.";
            case "Lábtolás": return "Talp vállszélesen a lapon, térd a lábfej irányába. Ne told teljesen egyenesig a térded.";
            case "Húzódzkodás": return "Vállszéles fogás, lapockák előbb le és hátra, utána húzz. Lent nyújtsd ki a kart, de tartsd feszesen a vállad.";
            case "Lehúzás": return "Mellkas fel, rúd a kulcscsont elé. A könyököd húzd le a bordáid felé – ne a karod, a hátad dolgozzon.";
            case "Tolódzkodás": return "Vállak lent és hátra, könyök hátrafelé hajlik. Csak addig ereszkedj, ameddig a vállad kényelmesen bírja.";
            case "Oldalemelés": return "Könyök enyhén hajlítva, vállmagasságig emelj. Kis súly, tiszta mozgás – a lendítés itt semmit nem ad.";
            case "Felülés": return "Áll ne szoruljon a mellkashoz, a hasad emeljen, ne a nyakad. Lefelé lassíts.";
            case "Combhajlítás": return "Csípő a párnán marad, a sarkat húzd a fenék felé. A végén ne kapd el, engedd lassan vissza.";
            case "Lábnyújtás": return "Térd a gép tengelyével egy vonalban, fent egy pillanat megállás. Ne rántsd ki a lábad.";
            case "Csípőemelés": return "Lapocka a padon, áll behúzva, fent szorítsd meg a farizmot. A derék ne feszüljön túl.";
            case "Arnold nyomás": return "Tenyér elöl indul, nyomás közben fordul kifelé. Feszes has, ne told előre a bordáidat.";
            case "Fordított tárogatás": return "Törzs döntve, könyök enyhén hajlítva, a lapockákat húzd össze. Hátsó vállra megy, nem a hátra.";
            case "Csuklyás emelés": return "Csak vállvonás, felfelé – nem körzés. Fent egy pillanat, lefelé lassíts.";
            case "Hátfeszítés": return "Csípő a párnán, a hát egyenes: csak addig emelkedj, "
                    + "amíg a törzsed vonalba ér – ne feszítsd hátra.";
            case "Hátizom gép": return "Mellkas a párnán, a könyököd húzd hátra a bordáid mellé, lapockák össze.";
            case "Mellgép": return "Könyök vállmagasságban, a mellkas nyomjon – ne a váll elülső része. Ne engedd túl hátra a kart.";
            default: return "";
        }
    }
}
