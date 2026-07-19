package com.edzo.idozito;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Gyakorlat-könyvtár: a beépített edzésprogramok gyakorlatai rövid technikai
 * leírással, hogy helyes formával tudj edzeni.
 */
public class LibraryActivity extends Activity {

    static final int TXT = MainActivity.TXT, MUTED = MainActivity.MUTED,
            GLASS = MainActivity.GLASS, GLASS_LINE = MainActivity.GLASS_LINE, LINE = MainActivity.LINE;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int accent = Theme.accent(this);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = vbox();
        col.setPadding(dp(18), dp(22), dp(18), dp(40));

        col.addView(text("Gyakorlat-könyvtár", 27, TXT, true));
        col.addView(gap(4));
        col.addView(text("Helyes forma röviden – minden beépített gyakorlathoz.", 13.5f, MUTED, false));
        col.addView(gap(18));

        // Egyedi (nem duplikált) gyakorlatnevek programonként, leírással
        for (Programs.P p : Programs.BUILT_IN) {
            LinearLayout head = hbox();
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.setPadding(dp(2), 0, 0, dp(10));
            head.addView(text(p.emoji + "  " + p.name, 18, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
            head.addView(text(p.ex.length + " gyakorlat", 12.5f, MUTED, false));
            col.addView(head, lp());

            LinearLayout card = card();
            for (int i = 0; i < p.ex.length; i++) {
                String name = p.ex[i];
                String desc = Programs.descOf(name);
                LinearLayout row = hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(14), dp(12), dp(14), dp(12));

                TextView num = new TextView(this);
                num.setText(String.valueOf(i + 1));
                num.setTextSize(14);
                num.setTypeface(null, Typeface.BOLD);
                num.setTextColor(0xFFFFFFFF);
                num.setGravity(Gravity.CENTER);
                GradientDrawable nb = new GradientDrawable();
                nb.setShape(GradientDrawable.OVAL);
                nb.setColor((accent & 0xFFFFFF) | 0x40000000);
                nb.setStroke(dp(1), (accent & 0xFFFFFF) | 0x99000000);
                num.setBackground(nb);
                LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(dp(30), dp(30));
                nlp.rightMargin = dp(12);
                row.addView(num, nlp);

                LinearLayout mid = vbox();
                mid.addView(text(name, 15.5f, TXT, true));
                if (!desc.isEmpty()) mid.addView(text(desc, 12.5f, MUTED, false));
                row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));

                card.addView(row);
                if (i < p.ex.length - 1) {
                    View dv = new View(this);
                    LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                    dvp.leftMargin = dp(14); dvp.rightMargin = dp(14);
                    dv.setLayoutParams(dvp);
                    dv.setBackgroundColor(LINE);
                    card.addView(dv);
                }
            }
            col.addView(card, lp());
            col.addView(gap(18));
        }

        col.addView(text("Tipp: a saját programjaidhoz is adhatsz gyakorlatokat az „Edzés típusa” választóban.",
                12.5f, MUTED, false));

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffold(this, sv, "bg_library"));
        col.post(() -> Ux.enterChildren(col, 30, 40));
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
