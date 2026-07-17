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

    static final int TXT = MainActivity.TXT, MUTED = MainActivity.MUTED,
            GLASS = MainActivity.GLASS, GLASS_LINE = MainActivity.GLASS_LINE,
            LINE = MainActivity.LINE, CARD2 = MainActivity.CARD2;

    int accent;
    int section = 0; // 0 bemelegítés, 1 nyújtás, 2 hengerezés
    LinearLayout body;
    Button[] chips = new Button[3];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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
        String[] labels = {"🔥 Bemelegítés", "🧘 Nyújtás", "🧻 Hengerezés"};
        for (int i = 0; i < 3; i++) {
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
        setContentView(Ux.scaffold(this, sv, "bg_profile"));
        render();
    }

    void render() {
        for (int i = 0; i < 3; i++) styleChip(chips[i], i == section);
        body.removeAllViews();
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
                new int[]{accent, Theme.accent2(this)});
        bg.setCornerRadius(dp(14));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(10);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> openVideo(query));
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
        styleChip(b, sel);
        return b;
    }

    void styleChip(Button b, boolean sel) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(13));
        if (sel) { bg.setColor(accent); b.setTextColor(0xFFFFFFFF); }
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
