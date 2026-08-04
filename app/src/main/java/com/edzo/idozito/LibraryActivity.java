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
 * Könyvtár: egy helyen az, amit az app folyó szövegből ért, és a beépített
 * edzésprogramok gyakorlatai rövid technikai leírással, hogy helyes formával
 * tudj edzeni.
 */
public class LibraryActivity extends Activity {

    static int TXT, MUTED, GLASS, GLASS_LINE, LINE;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; GLASS=MainActivity.GLASS; GLASS_LINE=MainActivity.GLASS_LINE; LINE=MainActivity.LINE;
        int accent = Theme.accent(this);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = vbox();
        col.setPadding(dp(18), dp(22), dp(18), dp(40));

        col.addView(text("Könyvtár", 27, TXT, true));
        col.addView(gap(4));
        col.addView(text("Mit írhatsz le egy mondatban, és hogyan csináld helyesen.",
                13.5f, MUTED, false));
        col.addView(gap(18));

        // Mondat-felismerés: az app négy helyen ért folyó szöveget, de ezt
        // eddig csak a beviteli mezők tippjei árulták el – oda viszont előbb
        // el kell jutni. Itt egy helyen látszik, mi mindent lehet leírni.
        col.addView(sectionHead("✍️  Mondatból is megy", "4 helyen"), lp());
        col.addView(sentenceCard(accent), lp());
        col.addView(gap(18));

        // Egyedi (nem duplikált) gyakorlatnevek programonként, leírással
        for (Programs.P p : Programs.BUILT_IN) {
            col.addView(sectionHead(p.emoji + "  " + p.name, p.ex.length + " gyakorlat"), lp());
            col.addView(exerciseCard(p.ex, accent), lp());
            col.addView(gap(18));
        }

        // Súlyzós alapok: az Erősítő naplóban felkínált gyakorlatok. Ezeknél a
        // technika nem csak hatékonyság kérdése, hanem sérülésé is.
        col.addView(sectionHead("🏋️  Súlyzós alapok",
                StrengthLog.COMMON.length + " gyakorlat"), lp());
        col.addView(exerciseCard(StrengthLog.COMMON, accent), lp());
        col.addView(gap(18));

        // Amit a mondat-felvétel még ismer (gépek, variációk): ezek nincsenek
        // a gyors chipek közt, de a technikai tipp ugyanúgy jár hozzájuk.
        java.util.ArrayList<String> extra = new java.util.ArrayList<>();
        for (String n : StrengthParse.names()) {
            boolean common = false;
            for (String c : StrengthLog.COMMON) if (c.equals(n)) { common = true; break; }
            if (!common) extra.add(n);
        }
        if (!extra.isEmpty()) {
            col.addView(sectionHead("🏋️  Gépek és variációk",
                    extra.size() + " gyakorlat"), lp());
            col.addView(exerciseCard(extra.toArray(new String[0]), accent), lp());
            col.addView(gap(18));
        }

        col.addView(text("Tipp: a saját programjaidhoz is adhatsz gyakorlatokat az „Edzés típusa” választóban.",
                12.5f, MUTED, false));

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_library", -1));
        col.post(() -> Ux.enterChildren(col, 30, 40));
    }

    /** Ugyanazok a példák, amiket a beviteli mezők is mutatnak. */
    LinearLayout sentenceCard(int accent) {
        LinearLayout card = card();
        String[][] groups = {
                {"🍽  Étrend", "Mit ettél?", "MEAL"},
                {"📝  Edzés-előzmény", "Több edzés egy mondatból", "BULK"},
                {"🏋️  Erősítő sorozatok", "Gyakorlat, sorozat, súly", "SET"},
                {"⏱  Időzítő", "Kör, munka, pihenő", "INTERVAL"},
        };
        for (int i = 0; i < groups.length; i++) {
            String[] g = groups[i];
            String[] ex = g[2].equals("MEAL") ? Examples.MEAL
                    : g[2].equals("BULK") ? Examples.BULK
                    : g[2].equals("SET") ? Examples.SET : Examples.INTERVAL;
            LinearLayout box = vbox();
            box.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.addView(text(g[0], 15.5f, TXT, true));
            box.addView(text(g[1], 12.5f, MUTED, false));
            // Három példa, naponta forogva – ugyanaz a válogatás, amit a
            // beviteli mezők tippjei is mutatnak.
            long day = System.currentTimeMillis() / 86400000L;
            for (int k = 0; k < 3 && k < ex.length; k++) {
                int idx = (int) (((day + k) % ex.length + ex.length) % ex.length);
                TextView t = text("„" + ex[idx] + "”", 13, accent, false);
                t.setPadding(0, dp(5), 0, 0);
                box.addView(t);
            }
            card.addView(box);
            if (i < groups.length - 1) {
                View dv = new View(this);
                LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                dvp.leftMargin = dp(14); dvp.rightMargin = dp(14);
                dv.setLayoutParams(dvp);
                dv.setBackgroundColor(LINE);
                card.addView(dv);
            }
        }
        return card;
    }

    /** Szakasz-fejléc: cím balra, darabszám jobbra. */
    LinearLayout sectionHead(String title, String count) {
        LinearLayout head = hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(2), 0, 0, dp(10));
        head.addView(text(title, 18, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(text(count, 12.5f, MUTED, false));
        return head;
    }

    /** Számozott gyakorlat-lista kártyán, mindegyiknél a technikai leírással. */
    LinearLayout exerciseCard(String[] names, int accent) {
        LinearLayout card = card();
        for (int i = 0; i < names.length; i++) {
            String desc = Programs.descOf(names[i]);
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
            mid.addView(text(names[i], 15.5f, TXT, true));
            if (!desc.isEmpty()) mid.addView(text(desc, 12.5f, MUTED, false));
            row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));

            card.addView(row);
            if (i < names.length - 1) {
                View dv = new View(this);
                LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                dvp.leftMargin = dp(14); dvp.rightMargin = dp(14);
                dv.setLayoutParams(dvp);
                dv.setBackgroundColor(LINE);
                card.addView(dv);
            }
        }
        return card;
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
