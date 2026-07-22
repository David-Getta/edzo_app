package com.edzo.idozito;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Erősítő edzésnapló képernyő: sorozatok (ismétlés × súly) rögzítése
 * gyakorlatonként, saját rekordokkal (max súly, becsült 1RM) és a bejegyzések
 * listájával. Súlyos edzésekhez, a HIIT-időzítő mellé.
 */
public class StrengthActivity extends Activity {

    static final int BG = MainActivity.BG, CARD = MainActivity.CARD, CARD2 = MainActivity.CARD2;
    static final int TXT = MainActivity.TXT, MUTED = MainActivity.MUTED, LINE = MainActivity.LINE;

    LinearLayout recordsBox, listBox;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Erősítő napló", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Rögzítsd a sorozatokat (ismétlés × súly), és kövesd a rekordjaid.", 13, MUTED, false));
        col.addView(gap(20));

        Button add = primary("＋  Új bejegyzés");
        add.setOnClickListener(v -> addEntryDialog());
        col.addView(add);
        col.addView(gap(18));

        col.addView(text("Rekordok", 15.5f, TXT, true));
        col.addView(gap(10));
        recordsBox = vbox();
        col.addView(recordsBox, lp());
        col.addView(gap(16));

        col.addView(text("Bejegyzések", 15.5f, TXT, true));
        col.addView(gap(10));
        listBox = vbox();
        col.addView(listBox, lp());

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffold(this, sv, "bg_workout"));
        col.post(() -> Ux.enterChildren(col, 30, 45));
        refresh();
    }

    void refresh() {
        refreshRecords();
        refreshList();
    }

    // ---------- Rekordok ----------

    void refreshRecords() {
        recordsBox.removeAllViews();
        List<StrengthLog.Entry> all = StrengthLog.load(this);
        if (all.isEmpty()) {
            recordsBox.addView(text("Még nincs rögzített gyakorlat. Adj hozzá egyet fent.", 13.5f, MUTED, false));
            return;
        }
        // Gyakorlatnevek a legutóbbi használat sorrendjében (a lista legújabb elöl).
        LinkedHashMap<String, Boolean> names = new LinkedHashMap<>();
        for (StrengthLog.Entry e : all) if (e.name != null) names.put(e.name, true);

        LinearLayout card = card();
        boolean first = true;
        for (String n : names.keySet()) {
            double[] rec = StrengthLog.recordsFor(this, n);
            if (!first) {
                View dv = new View(this);
                LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                dvp.leftMargin = dp(16); dvp.rightMargin = dp(16);
                dv.setLayoutParams(dvp); dv.setBackgroundColor(LINE);
                card.addView(dv);
            }
            first = false;
            LinearLayout row = vbox();
            row.setPadding(dp(16), dp(12), dp(16), dp(12));
            row.addView(text(n, 15.5f, TXT, true));
            String sub = "Max " + fmtKg(rec[0]) + " kg   ·   becsült 1RM ~" + fmtKg(rec[1]) + " kg";
            row.addView(text(sub, 12.5f, MUTED, false));
            card.addView(row);
        }
        recordsBox.addView(card, lp());
    }

    // ---------- Bejegyzések ----------

    void refreshList() {
        listBox.removeAllViews();
        List<StrengthLog.Entry> all = StrengthLog.load(this);
        if (all.isEmpty()) return;
        SimpleDateFormat fmt = new SimpleDateFormat("MM. dd. HH:mm", new Locale("hu"));
        for (int i = 0; i < all.size(); i++) {
            final int idx = i;
            final StrengthLog.Entry e = all.get(i);
            LinearLayout card = card();
            LinearLayout inner = vbox();
            inner.setPadding(dp(16), dp(12), dp(16), dp(12));

            LinearLayout top = hbox();
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(text(e.name, 16, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
            top.addView(text(Math.round(e.volume()) + " kg volumen", 12, Theme.accent(this), true));
            inner.addView(top);

            inner.addView(text(fmt.format(new Date(e.ts)), 11.5f, MUTED, false));
            inner.addView(gap(6));

            StringBuilder sb = new StringBuilder();
            for (StrengthLog.SetEntry s : e.sets) {
                if (sb.length() > 0) sb.append("    ·    ");
                sb.append(fmtKg(s.weight)).append(" kg × ").append(s.reps);
            }
            inner.addView(text(sb.toString(), 13.5f, TXT, false));

            card.addView(inner);
            card.setClickable(true);
            card.setOnClickListener(v -> new Sheet(this, e.name, "Bejegyzés törlése?")
                    .addDestructive("Törlés", () -> { StrengthLog.removeAt(this, idx); refresh(); })
                    .addCancel().show());
            listBox.addView(card, lp());
            listBox.addView(gap(10));
        }
    }

    // ---------- Új bejegyzés ----------

    void addEntryDialog() {
        final LinearLayout box = vbox();
        box.setPadding(dp(10), dp(6), dp(10), 0);

        final EditText nameEt = new EditText(this);
        nameEt.setHint("Gyakorlat neve (pl. Guggolás)");
        nameEt.setHintTextColor(MUTED);
        nameEt.setTextColor(TXT);
        nameEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(nameEt);

        // Gyors nevek vízszintes chip-sávban
        box.addView(gap(8));
        LinearLayout chips = hbox();
        for (final String n : StrengthLog.knownNames(this)) {
            Button chip = ghost(n);
            chip.setTextSize(12.5f);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(6);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> { nameEt.setText(n); nameEt.setSelection(n.length()); });
            chips.addView(chip);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(chips);
        box.addView(hs);

        box.addView(gap(12));
        box.addView(text("Sorozatok (ismétlés × súly kg)", 13, MUTED, true));
        box.addView(gap(4));

        final LinearLayout setsBox = vbox();
        final List<EditText> repsList = new ArrayList<>();
        final List<EditText> wList = new ArrayList<>();
        box.addView(setsBox);
        for (int i = 0; i < 3; i++) addSetRow(setsBox, repsList, wList);

        Button more = ghost("＋  Sorozat");
        more.setTextSize(13.5f);
        more.setOnClickListener(v -> addSetRow(setsBox, repsList, wList));
        box.addView(gap(4));
        box.addView(more);

        new Sheet(this, "Új bejegyzés")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    String name = nameEt.getText().toString().trim();
                    if (name.isEmpty()) name = "Gyakorlat";
                    List<StrengthLog.SetEntry> sets = new ArrayList<>();
                    for (int i = 0; i < repsList.size(); i++) {
                        int reps = parseInt(repsList.get(i).getText().toString());
                        double w = parseDouble(wList.get(i).getText().toString());
                        if (reps > 0) sets.add(new StrengthLog.SetEntry(reps, Math.max(0, w)));
                    }
                    if (sets.isEmpty()) return; // nincs érvényes sorozat → nem mentünk
                    StrengthLog.add(this, new StrengthLog.Entry(System.currentTimeMillis(), name, sets));
                    refresh();
                })
                .addCancel()
                .show();
    }

    void addSetRow(LinearLayout setsBox, List<EditText> repsList, List<EditText> wList) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        EditText reps = numEt("ism.");
        reps.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText w = numEt("kg");
        w.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        repsList.add(reps);
        wList.add(w);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1f);
        p1.rightMargin = dp(4);
        TextView x = text("×", 16, MUTED, true);
        x.setPadding(dp(6), 0, dp(6), 0);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1.2f);
        p2.leftMargin = dp(4);
        row.addView(reps, p1);
        row.addView(x);
        row.addView(w, p2);
        setsBox.addView(row);
    }

    EditText numEt(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(MUTED);
        et.setTextColor(TXT);
        et.setTextSize(15);
        et.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2); bg.setCornerRadius(dp(10)); bg.setStroke(dp(1), LINE);
        et.setBackground(bg);
        et.setPadding(dp(10), dp(10), dp(10), dp(10));
        return et;
    }

    int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    double parseDouble(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return 0; }
    }

    /** Súly kiírása: egész, ha kerek (40), egyébként 1 tizedes vesszővel (42,5). */
    String fmtKg(double w) {
        if (Math.abs(w - Math.round(w)) < 0.05) return String.valueOf(Math.round(w));
        return String.format(new Locale("hu"), "%.1f", w);
    }

    // ---------- UI segédek ----------

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6121A33);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0x33FFFFFF);
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

    Button primary(String label) {
        Button b = base(label);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.accent(this), Theme.accent2(this)});
        bg.setCornerRadius(dp(16));
        b.setBackground(bg); b.setTextColor(0xFFFFFFFF); b.setTextSize(16);
        return b;
    }

    Button ghost(String label) {
        Button b = base(label);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2); bg.setCornerRadius(dp(13)); bg.setStroke(dp(1), LINE);
        b.setBackground(bg); b.setTextSize(14.5f);
        return b;
    }

    Button base(String label) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextColor(TXT);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(dp(14), dp(13), dp(14), dp(13));
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        b.setStateListAnimator(null);
        return b;
    }

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
