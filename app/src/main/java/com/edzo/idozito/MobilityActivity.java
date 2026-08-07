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
            Rehab.Area hit = Rehab.forComplaint(sent);
            if (hit == null) hit = Rehab.forGoal(sent);
            if (hit != null) {
                section = 3;
                final Rehab.Area fhit = hit;
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
        // Heti fókusz: a kitűzött terület és a hétfőnként nullázódó számláló.
        String fid = RehabLog.focusId(this);
        Rehab.Area focus = fid == null ? null : Rehab.byId(fid);
        if (focus != null) {
            int done = Rehab.weekCount(RehabLog.doneOf(this, fid), System.currentTimeMillis());
            LinearLayout fc = card();
            fc.setPadding(dp(14), dp(12), dp(14), dp(12));
            fc.addView(text("⭐ Heti fókusz", 11.5f, MUTED, true));
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
            mid.addView(text(area.moves.length + " gyakorlat · ~" + Rehab.minutesOf(area)
                    + " perc", 12, MUTED, false));
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
        body.addView(text("⚠️ " + Rehab.RED_FLAG, 12, MUTED, false));
        body.post(() -> Ux.enterChildren(body, 20, 35));
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
        final boolean isFocus = area.id.equals(RehabLog.focusId(this));
        new Sheet(this, area.emoji + " " + area.name, area.goal)
                .addCustom(box)
                // Vezetett mód: az időzítő 40 mp-es körökben, három körben
                // mondja a gyakorlatokat – az ismétlésszámos adagolás durvább
                // közelítése, de kézbe veszi azt, aki csak sodródna a listán.
                .addNeutral("▶ Vezetett indítás (3 kör, 40 mp)", () -> {
                    java.util.ArrayList<String> names = new java.util.ArrayList<>();
                    for (int r = 0; r < 3; r++)
                        for (Rehab.Ex e : area.moves) names.add(e.name);
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
                })
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
