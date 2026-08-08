package com.edzo.idozito;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Nyújtás, bemelegítés és hengerezés (SMR) könyvtár – izmonként legalább két
 * nyújtással, technikai leírással, és minden gyakorlathoz „▶ Videó" gombbal,
 * ami valódi bemutató videót nyit (YouTube-keresés).
 */
public class MobilityActivity extends Activity {

    static int TXT, MUTED, GLASS, GLASS_LINE, LINE, CARD2;

    int accent;
    int section = 0; // 0 bemelegítés, 1 nyújtás, 2 hengerezés, 3 rehab
    LinearLayout body;
    Button[] chips = new Button[4];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; GLASS=MainActivity.GLASS; GLASS_LINE=MainActivity.GLASS_LINE; LINE=MainActivity.LINE; CARD2=MainActivity.CARD2;
        accent = Theme.accent(this);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = vbox();
        col.setPadding(dp(18), dp(22), dp(18), dp(40));

        col.addView(text("Nyújtás & mobilitás", 27, TXT, true));
        col.addView(gap(4));
        col.addView(text("Bemelegítés, nyújtás és hengerezés – minden gyakorlathoz videóval.", 13.5f, MUTED, false));
        col.addView(gap(16));

        LinearLayout chipRow = hbox();
        String[] labels = {"🔥 Melegítés", "🧘 Nyújtás", "🧻 Henger", "🩹 Rehab"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            Button c = chip(labels[i], i == section);
            c.setOnClickListener(v -> { section = idx; render(); });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
            clp.leftMargin = dp(3); clp.rightMargin = dp(3);
            chipRow.addView(c, clp);
            chips[i] = c;
        }
        col.addView(chipRow, lp());
        col.addView(gap(16));

        body = vbox();
        col.addView(body, lp());

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_mobility", -1));
        // Máshonnan ideirányított panasz-mondat („fáj a vállam"): rögtön a
        // rehab fület nyitjuk, és ha a testtáj is kiderül, a sorát is.
        String sent = getIntent().getStringExtra(Sentence.EXTRA);
        if (sent != null && !sent.trim().isEmpty()) {
            getIntent().removeExtra(Sentence.EXTRA);
            // Piros zászlós panasz: erre nem sort ajánlunk, hanem kimondjuk,
            // miért nem. A hallgatás azt üzenné, hogy nem értjük – pedig
            // pont hogy értjük, és éppen ezért nem tornáztatunk.
            final String flag = Rehab.redFlag(sent);
            if (flag != null) {
                section = 3;
                body.post(() -> redFlagSheet(flag));
            }
            Rehab.Area hit = flag != null ? null : Rehab.forComplaint(sent);
            if (hit == null && flag == null) hit = Rehab.forGoal(sent);
            if (hit != null) {
                section = 3;
                final Rehab.Area fhit = hit;
                // Ha a mondat a skálát is kimondta („fáj a vállam 6/10"), azt
                // fölösleges még egyszer megkérdezni – rögtön be is jegyezzük.
                int said = Rehab.painIn(sent);
                if (said >= 0) {
                    RehabLog.addPain(this, fhit.id, System.currentTimeMillis(), said);
                    Toast.makeText(this, "📉 " + fhit.name + ": " + said + "/10 feljegyezve",
                            Toast.LENGTH_SHORT).show();
                }
                body.post(() -> areaSheet(fhit));
            }
        }
        // A Könyvtár rehab-kártyája testtáj nélkül nyitja a fület.
        if (getIntent().getBooleanExtra("open_rehab", false)) {
            getIntent().removeExtra("open_rehab");
            section = 3;
        }
        render();
    }

    void render() {
        for (int i = 0; i < chips.length; i++) styleChip(chips[i], i == section, sectionColor(i));
        body.removeAllViews();
        if (section == 3) { renderRehab(); return; }

        Button start = startBtn("▶  Vezetett " + sectionLabel() + " indítása");
        start.setOnClickListener(v -> chooseHold());
        body.addView(start);
        int exCount = sectionNames().length;
        body.addView(text(exCount + " gyakorlat · a telefon időzíti és bemondja őket, egymás után.", 12, MUTED, false));
        body.addView(gap(16));

        Mobility.Group[] groups = section == 0 ? Mobility.WARMUP
                : section == 1 ? Mobility.STRETCH : Mobility.ROLLING;
        for (Mobility.Group grp : groups) {
            TextView h = text(grp.title, 17, TXT, true);
            h.setPadding(dp(2), 0, 0, dp(10));
            body.addView(h);
            LinearLayout card = card();
            for (int i = 0; i < grp.items.length; i++) {
                Mobility.Item item = grp.items[i];
                LinearLayout row = hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(14), dp(12), dp(12), dp(12));

                LinearLayout mid = vbox();
                mid.addView(text(item.name, 15.5f, TXT, true));
                mid.addView(text(item.desc, 12.5f, MUTED, false));
                row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));

                row.addView(videoBtn(item.video));

                card.addView(row);
                if (i < grp.items.length - 1) {
                    View dv = new View(this);
                    LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                    dvp.leftMargin = dp(14); dvp.rightMargin = dp(14);
                    dv.setLayoutParams(dvp);
                    dv.setBackgroundColor(LINE);
                    card.addView(dv);
                }
            }
            body.addView(card, lp());
            body.addView(gap(16));
        }
        body.addView(text("A „▶ Videó” gomb egy YouTube-keresést nyit az adott gyakorlatra – valódi, helyes technikát bemutató videókkal.",
                12, MUTED, false));
        body.post(() -> Ux.enterChildren(body, 20, 35));
    }

    /**
     * Megelőzés és rehab: testtájat választasz, kész gyakorlatsort kapsz.
     *
     * A lista szándékosan nem „edzésprogram"-nak hívja magát: gyógytornász-
     * ihletésű megelőző sorok ezek, és a lap alján ki is mondjuk, hogy éles
     * panasznál nem app kell, hanem szakember.
     */
    void renderRehab() {
        body.addView(text("Válassz testtájat – kész, 10–15 perces megelőző sort kapsz: "
                + "gyakorlat, adagolás, technikai tipp.", 12.5f, MUTED, false));
        body.addView(gap(12));
        // Döntés helyett ajánlás: a fókusz, amíg nincs meg a heti adag, utána
        // az, amit a legrégebben csináltál.
        String[] ids = new String[Rehab.AREAS.length];
        long[] last = new long[Rehab.AREAS.length];
        for (int i = 0; i < Rehab.AREAS.length; i++) {
            ids[i] = Rehab.AREAS[i].id;
            long[] d = RehabLog.doneOf(this, ids[i]);
            last[i] = d.length > 0 ? d[0] : 0;
        }
        String fid0 = RehabLog.focusId(this);
        int fdone0 = fid0 == null ? 0
                : Rehab.weekCount(RehabLog.doneOf(this, fid0), System.currentTimeMillis());
        Rehab.Area next = Rehab.byId(Rehab.nextArea(fid0, fdone0, ids, last));
        if (next != null) {
            final Rehab.Area fn = next;
            Button go = startBtn("▶  Mit csináljak ma?  ·  " + fn.emoji + " " + fn.name);
            go.setOnClickListener(v -> areaSheet(fn));
            body.addView(go);
            body.addView(gap(12));
        }
        // Heti fókusz: a kitűzött terület és a hétfőnként nullázódó számláló.
        String fid = RehabLog.focusId(this);
        Rehab.Area focus = fid == null ? null : Rehab.byId(fid);
        // Akinek egy testtájnál már gyűlnek a fájdalom-értékek, de nincs
        // kitűzött fókusza, annak egy koppintással felajánljuk: a panasz
        // követése és a heti adag együtt ér valamit.
        if (focus == null) {
            Rehab.Area cand = null;
            int most = 2;
            for (Rehab.Area a : Rehab.AREAS) {
                int n = RehabLog.painLevels(this, a.id).length;
                if (n > most) { most = n; cand = a; }
            }
            if (cand != null) {
                final Rehab.Area fc2 = cand;
                LinearLayout sc = card();
                sc.setPadding(dp(14), dp(12), dp(14), dp(12));
                sc.addView(text("⭐ Legyen ez a heti fókusz?", 14.5f, TXT, true));
                TextView sh2 = text(fc2.emoji + " " + fc2.name + " – ide már " + most
                        + " fájdalom-értéket írtál. Heti " + Rehab.WEEKLY_GOAL
                        + " alkalommal érdemes csinálni; koppints, és számolom.",
                        12, MUTED, false);
                sh2.setPadding(0, dp(2), 0, 0);
                sc.addView(sh2);
                sc.setClickable(true);
                sc.setOnClickListener(v -> {
                    RehabLog.setFocus(this, fc2.id);
                    Ux.blazeCard(this, "⭐ " + fc2.name + " a heti fókusz.");
                    render();
                });
                body.addView(sc, lp());
                body.addView(gap(12));
            }
        }
        if (focus != null) {
            long[] doneTs = RehabLog.doneOf(this, fid);
            int done = Rehab.weekCount(doneTs, System.currentTimeMillis());
            int streak = Rehab.weekStreak(doneTs, System.currentTimeMillis());
            LinearLayout fc = card();
            fc.setPadding(dp(14), dp(12), dp(14), dp(12));
            fc.addView(text("⭐ Heti fókusz" + (streak >= 2
                    ? "   ·   🔥 " + streak + " hete sorban" : ""), 11.5f, MUTED, true));
            fc.addView(text(focus.emoji + " " + Rehab.focusLine(focus, done), 15, TXT, true));
            TextView fh = text(done >= Rehab.WEEKLY_GOAL
                    ? "Szép hét – ami ezután jön, az ráadás."
                    : "Koppints, és folytasd – a rendszeresség véd, nem az egyszeri sor.",
                    12, MUTED, false);
            fh.setPadding(0, dp(2), 0, 0);
            fc.addView(fh);
            final Rehab.Area fa = focus;
            fc.setClickable(true);
            fc.setOnClickListener(v -> areaSheet(fa));
            body.addView(fc, lp());
            body.addView(gap(12));
        }
        LinearLayout card = card();
        for (int i = 0; i < Rehab.AREAS.length; i++) {
            final Rehab.Area area = Rehab.AREAS[i];
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(12), dp(12), dp(12));
            TextView em = text(area.emoji, 20, TXT, false);
            em.setPadding(0, 0, dp(12), 0);
            row.addView(em);
            LinearLayout mid = vbox();
            mid.addView(text(area.name, 15.5f, TXT, true));
            // Ahol van fájdalom-bejegyzés, ott a legutóbbi érték a listán is
            // látszik – így egy pillantás alatt kiderül, hol áll a panasz.
            int[] plv = RehabLog.painLevels(this, area.id);
            String sub = area.moves.length + " gyakorlat · ~" + Rehab.minutesOf(area) + " perc";
            if (plv.length > 0 && plv[0] >= 0) sub += "   ·   📉 " + plv[0] + "/10";
            mid.addView(text(sub, 12, MUTED, false));
            row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView arrow = text("›", 22, MUTED, false);
            row.addView(arrow);
            row.setClickable(true);
            row.setOnClickListener(v -> areaSheet(area));
            card.addView(row);
            if (i < Rehab.AREAS.length - 1) {
                View dv = new View(this);
                LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                dvp.leftMargin = dp(14); dvp.rightMargin = dp(14);
                dv.setLayoutParams(dvp);
                dv.setBackgroundColor(LINE);
                card.addView(dv);
            }
        }
        body.addView(card, lp());
        body.addView(gap(12));
        // Jelentés: aki elmegy a gyógytornászhoz, ne fejből mondja el, mit
        // csinált és merre ment a panasz – négy hét adata egy üzenetben.
        if (hasRehabData()) {
            Button rep = chip("📤  Jelentés a gyógytornásznak (4 hét)", false);
            rep.setPadding(dp(14), dp(12), dp(14), dp(12));
            rep.setOnClickListener(v -> shareReport());
            body.addView(rep, lp());
            body.addView(gap(12));
        }
        body.addView(text("⚠️ " + Rehab.RED_FLAG, 12, MUTED, false));
        body.post(() -> Ux.enterChildren(body, 20, 35));
    }

    /** Van-e egyáltalán rehab-adat, amiről jelentést lehetne írni? */
    boolean hasRehabData() {
        for (Rehab.Area a : Rehab.AREAS)
            if (RehabLog.doneOf(this, a.id).length > 0
                    || RehabLog.painLevels(this, a.id).length > 0) return true;
        return false;
    }

    /**
     * Négy hét rehab-története szövegben: mit csinált, és merre ment a panasz.
     *
     * A gyógytornász első két kérdése pontosan ez, és fejből egyik sem
     * megválaszolható. A CSV a táblázatosaké; ez az, ami elküldhető egy
     * üzenetben.
     */
    void shareReport() {
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("🩹 Rehab-napló – elmúlt 4 hét\n");
        for (Rehab.Area a : Rehab.AREAS) {
            long[] done = RehabLog.doneOf(this, a.id);
            int[] pain = RehabLog.painLevels(this, a.id);
            int n = 0;
            for (long t : done) if (Days.ago(t, now) >= 0 && Days.ago(t, now) < 28) n++;
            if (n == 0 && pain.length == 0) continue;
            sb.append('\n').append(a.emoji).append(' ').append(a.name).append('\n');
            sb.append("   ").append(n).append(" elvégzett sor\n");
            if (pain.length > 0) {
                sb.append("   fájdalom: ");
                for (int i = Math.min(pain.length, 10) - 1; i >= 0; i--) {
                    sb.append(pain[i]);
                    if (i > 0) sb.append(" → ");
                }
                sb.append("  (0–10)\n");
                String line = Rehab.painLine(pain);
                if (!line.isEmpty()) sb.append("   ").append(line).append('\n');
            }
        }
        sb.append('\n').append("A sorok gyógytornász-ihletésű megelőző gyakorlatok, "
                + "nem orvosi kezelés.");
        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                    .setType("text/plain").putExtra(Intent.EXTRA_TEXT, sb.toString()),
                    "Rehab-napló"));
        } catch (Exception ignored) {
        }
    }

    /**
     * A piros zászlós panasz válasza: figyelmeztetés, gyakorlat nélkül.
     *
     * Szándékosan nincs rajta „mégis mutasd a sort" gomb. Aki zsibbadást ír
     * be, annak a legjobb, amit egy app tehet, hogy nem ad neki tornát – és
     * ezt meg is indokolja, hogy ne érezze válasz nélkül magát.
     */
    void redFlagSheet(String msg) {
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        box.addView(text(msg, 14, TXT, false));
        TextView small = text("Ez az app megelőzésre és általános erősítésre való, "
                + "nem orvoslásra – és nem is akar az lenni.", 11.5f, MUTED, false);
        small.setPadding(0, dp(10), 0, 0);
        box.addView(small);
        new Sheet(this, "⚠️ Ezt nézesd meg", "Piros zászlós panasz")
                .addCustom(box)
                .addPrimary("Értem", () -> { })
                .show();
    }

    /** Egy testtáj kész sora: gyakorlatok, videók, és egy koppintásos naplózás. */
    void areaSheet(final Rehab.Area area) {
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        for (Rehab.Ex e : area.moves) {
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            LinearLayout mid = vbox();
            mid.addView(text(e.name + "   ·   " + e.dose, 14.5f, TXT, true));
            TextView cue = text(e.cue, 12, MUTED, false);
            cue.setPadding(0, dp(2), 0, 0);
            mid.addView(cue);
            row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(videoBtn(e.video));
            box.addView(row, lp());
        }
        TextView warn = text("⚠️ " + area.warn, 11.5f, MUTED, false);
        warn.setPadding(0, dp(8), 0, 0);
        box.addView(warn);
        // A fokozatosság a rehab-oldalon is elv: aki már sokszor elvégezte a
        // sort, annak az adagolás emelése a következő lépés – ezt ki is mondjuk.
        int doneAll = RehabLog.doneOf(this, area.id).length;
        if (doneAll >= 6) {
            // A területnek megvan a maga iránya: a bokánál a következő lépés
            // nem több ismétlés, hanem instabil felület és ugrás; a
            // golfkönyöknél nehezebb súly, lassabb leengedés. Az általános
            // „emelj az adagoláson" ehhez képest semmit nem mond.
            String nx = Rehab.nextLevel(area.id);
            TextView lvl = text("📈 Már " + doneAll + " alkalmon vagy túl ezen a soron – "
                    + (nx.isEmpty()
                    ? "ha könnyűnek érzed, emelj az adagoláson: +2–3 ismétlés vagy "
                            + "+10 mp tartás gyakorlatonként."
                    : "ha könnyűnek érzed, jöhet a következő szint.\n\n" + nx),
                    11.5f, MUTED, false);
            lvl.setPadding(0, dp(6), 0, 0);
            box.addView(lvl);
        }
        // Fájdalom-napló: a panasz iránya többet mond, mint egyetlen nap
        // száma – és pont ez az, amit a gyógytornász is kérdezni szokott.
        int[] pain = RehabLog.painLevels(this, area.id);
        String pline = Rehab.painLine(pain);
        if (!pline.isEmpty()) {
            TextView pt = text("📉 " + pline, 12, MUTED, false);
            pt.setPadding(0, dp(8), 0, 0);
            box.addView(pt);
        }
        // A görbe a szemnek szól: a számsor irányát egy pillantás alatt
        // megmutatja. Régi → új sorrendben, mint minden más grafikonon.
        if (pain.length >= 3) {
            double[] series = new double[pain.length];
            for (int i = 0; i < pain.length; i++) series[i] = pain[pain.length - 1 - i];
            ProfileActivity.ChartView ch = new ProfileActivity.ChartView(this);
            ch.setData(series, 0xFFFF7A2F, "/10");
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, dp(100));
            clp.topMargin = dp(6);
            box.addView(ch, clp);
        }
        final boolean isFocus = area.id.equals(RehabLog.focusId(this));
        new Sheet(this, area.emoji + " " + area.name, area.goal)
                .addCustom(box)
                .addNeutral("📉 Mennyire fáj ma? (0–10)", () -> painSheet(area))
                // Vezetett mód: az időzítő 40 mp-es ablakokban mondja a
                // gyakorlatokat – a kétoldalasokat bal/jobb bontásban, és a
                // kör-szám úgy áll be, hogy a sor a 10–20 perces keretben
                // maradjon. Az ismétlésszámos adagolás durvább közelítése, de
                // kézbe veszi azt, aki csak sodródna a listán.
                .addNeutral("▶ Vezetett indítás (" + Rehab.guidedRounds(area)
                        + " kör, 40 mp)", () -> {
                    java.util.List<String> one = Rehab.guidedNames(area);
                    java.util.ArrayList<String> names = new java.util.ArrayList<>();
                    for (int r = 0; r < Rehab.guidedRounds(area); r++) names.addAll(one);
                    Intent gi = new Intent(this, MainActivity.class);
                    gi.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    gi.putExtra("r_names", names.toArray(new String[0]));
                    gi.putExtra("r_label", area.name);
                    gi.putExtra("r_work", 40);
                    gi.putExtra("r_rest", 8);
                    gi.putExtra("r_prep", 5);
                    startActivity(gi);
                })
                // A fókusz kitűzése: heti számláló, hétfőnként nullázódik. A
                // rendszeresség a megelőzés lelke, nem az egyszeri lelkesedés.
                .addNeutral(isFocus ? "★ Fókusz levétele"
                        : "⭐ Legyen a heti fókusz (" + Rehab.WEEKLY_GOAL + " alkalom/hét)", () -> {
                    RehabLog.setFocus(this, isFocus ? null : area.id);
                    Toast.makeText(this, isFocus ? "Fókusz levéve."
                            : "⭐ " + area.name + " a heti fókusz.", Toast.LENGTH_SHORT).show();
                    if (section == 3) render();
                })
                // Megosztás sima szövegként: elküldhető annak, akinek épp fáj –
                // vagy a gyógytornásznak, hogy ránézzen.
                .addNeutral("📤 Sor küldése szövegként", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(area.emoji).append(' ').append(area.name).append('\n')
                            .append(area.goal).append("\n\n");
                    for (Rehab.Ex e : area.moves)
                        sb.append("• ").append(e.name).append("  –  ").append(e.dose)
                                .append('\n').append("   ").append(e.cue).append('\n');
                    sb.append('\n').append("⚠️ ").append(area.warn);
                    Intent sh = new Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, sb.toString());
                    try {
                        startActivity(Intent.createChooser(sh, area.name));
                    } catch (Exception ignored) {
                    }
                })
                .addPrimary("✅ Elvégeztem (~" + Rehab.minutesOf(area) + " perc)", () -> {
                    // A naplóba mobilitásként kerül: a széria, az XP és a heti
                    // összegzés is látja – a megelőzés is edzés.
                    long now = System.currentTimeMillis();
                    History.addManual(this, now,
                            Rehab.minutesOf(area) * 60, -1,
                            Activities.calories(Activities.byId("joga"),
                                    Profile.lastWeight(this), Rehab.minutesOf(area)),
                            -1, area.name, "joga");
                    RehabLog.addDone(this, area.id, now);
                    BlazeWidget.refresh(this);
                    // A fókusz-területnél a heti állás is odafér a nyugtára.
                    String msg = "🩹 " + area.name + " elvégezve ✔";
                    if (area.id.equals(RehabLog.focusId(this))) {
                        int done = Rehab.weekCount(RehabLog.doneOf(this, area.id), now);
                        msg = done >= Rehab.WEEKLY_GOAL
                                ? "🩹 " + area.name + " ✔ – e heti " + Rehab.WEEKLY_GOAL + " alkalom megvan! ⭐"
                                : "🩹 " + area.name + " ✔ – a héten " + done + "/" + Rehab.WEEKLY_GOAL;
                    }
                    Ux.blazeCard(this, msg);
                    if (section == 3) render();
                    // A sor után a legjobb pillanat megkérdezni, hogy áll a
                    // panasz – ilyenkor friss az élmény, és így lesz görbe is.
                    if (!RehabLog.painLoggedToday(this, area.id))
                        body.postDelayed(() -> { if (!isFinishing()) painSheet(area); }, 900);
                })
                .addCancel()
                .show();
    }

    /**
     * Fájdalom-bevitel: tizenegy gomb, semmi csúszka.
     *
     * A skála két végét oda kell írni, különben a szám önkényes: a nulla a
     * panaszmentes nap, a tíz az elviselhetetlen. Napi egy érték elég.
     */
    void painSheet(final Rehab.Area area) {
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), dp(4));
        box.addView(text("0 = nincs fájdalom   ·   3 = enyhe   ·   6 = közepes   ·   "
                + "10 = elviselhetetlen", 12, MUTED, false));
        for (int row = 0; row < 2; row++) {
            LinearLayout r = hbox();
            r.setPadding(0, dp(8), 0, 0);
            int from = row * 6, to = Math.min(from + 6, 11);
            for (int i = from; i < to; i++) {
                final int level = i;
                Button b = new Button(this);
                b.setText(String.valueOf(level));
                b.setAllCaps(false);
                b.setTextSize(15);
                b.setTypeface(null, Typeface.BOLD);
                b.setStateListAnimator(null);
                b.setMinWidth(0);
                b.setMinHeight(0);
                b.setPadding(0, dp(11), 0, dp(11));
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(13));
                bg.setColor(CARD2);
                bg.setStroke(dp(1), LINE);
                b.setBackground(bg);
                b.setTextColor(TXT);
                b.setOnClickListener(v -> {
                    RehabLog.addPain(this, area.id, System.currentTimeMillis(), level);
                    Ux.blazeCard(this, "📉 " + area.name + ": " + level + "/10 – "
                            + Rehab.painWord(level));
                    if (section == 3) render();
                });
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, -2, 1f);
                blp.leftMargin = dp(2); blp.rightMargin = dp(2);
                r.addView(b, blp);
            }
            box.addView(r, lp());
        }
        new Sheet(this, "📉 " + area.name, "Mennyire fáj ma?")
                .addCustom(box)
                .addCancel()
                .show();
    }

    View videoBtn(final String query) {
        Button b = new Button(this);
        b.setText("▶ Videó");
        b.setAllCaps(false);
        b.setTextSize(12.5f);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(0xFFFFFFFF);
        b.setStateListAnimator(null);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setPadding(dp(14), dp(9), dp(14), dp(9));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{sectionColor(section), sectionColor2(section)});
        bg.setCornerRadius(dp(14));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(10);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> openVideo(query));
        return b;
    }

    // ---- Vezetett rutin ----

    String sectionLabel() { return section == 0 ? "bemelegítés" : section == 1 ? "nyújtás" : "hengerezés"; }
    String routineLabel() { return section == 0 ? "Bemelegítés" : section == 1 ? "Nyújtás" : "Hengerezés"; }

    // Szekciónkénti akcentszínek: bemelegítés = meleg narancs, nyújtás = cián,
    // hengerezés = magenta – így vizuálisan is elkülönül a három terület.
    int sectionColor(int s) { return s == 0 ? 0xFFFF7A2F : s == 1 ? accent : s == 2 ? Theme.accent2(this) : 0xFF6FE3C2; }
    int sectionColor2(int s) { return s == 0 ? 0xFFFFB259 : s == 1 ? Theme.accent2(this) : s == 2 ? accent : 0xFF3EC9A7; }

    String[] sectionNames() {
        Mobility.Group[] groups = section == 0 ? Mobility.WARMUP
                : section == 1 ? Mobility.STRETCH : Mobility.ROLLING;
        java.util.ArrayList<String> l = new java.util.ArrayList<>();
        for (Mobility.Group g : groups) for (Mobility.Item it : g.items) l.add(it.name);
        return l.toArray(new String[0]);
    }

    void chooseHold() {
        new Sheet(this, "Vezetett " + sectionLabel(), "Meddig tartson egy gyakorlat?")
                .addRow("⏱", "20 másodperc", null, false, true, () -> launchRoutine(20))
                .addRow("⏱", "30 másodperc", null, false, true, () -> launchRoutine(30))
                .addRow("⏱", "45 másodperc", null, false, true, () -> launchRoutine(45))
                .addCancel()
                .show();
    }

    void launchRoutine(int hold) {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("r_names", sectionNames());
        i.putExtra("r_label", routineLabel());
        i.putExtra("r_work", hold);
        i.putExtra("r_rest", section == 0 ? 4 : 6);
        i.putExtra("r_prep", 5);
        startActivity(i);
    }

    Button startBtn(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextSize(16);
        b.setStateListAnimator(null);
        b.setPadding(dp(16), dp(15), dp(16), dp(15));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{sectionColor(section), sectionColor2(section)});
        bg.setCornerRadius(dp(16));
        b.setBackground(bg);
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return b;
    }

    void openVideo(String query) {
        try {
            Uri u = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query));
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (Exception e) {
            Toast.makeText(this, "Nem sikerült megnyitni a videót.", Toast.LENGTH_SHORT).show();
        }
    }

    // ---- UI segéd ----

    Button chip(String label, boolean sel) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextSize(12.5f);
        b.setPadding(dp(6), dp(11), dp(6), dp(11));
        b.setStateListAnimator(null);
        styleChip(b, sel, accent);
        return b;
    }

    void styleChip(Button b, boolean sel, int selColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(13));
        if (sel) { bg.setColor(selColor); b.setTextColor(0xFFFFFFFF); }
        else { bg.setColor(CARD2); bg.setStroke(dp(1), LINE); b.setTextColor(TXT); }
        b.setBackground(bg);
    }

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GLASS); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), GLASS_LINE);
        c.setBackground(bg);
        return c;
    }

    TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    View gap(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(h))); return v; }

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
