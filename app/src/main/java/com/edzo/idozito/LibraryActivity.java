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

        // Mondat-felismerés: az app több helyen ért folyó szöveget, de ezt
        // eddig csak a beviteli mezők tippjei árulták el – oda viszont előbb
        // el kell jutni. Itt egy helyen látszik, mi mindent lehet leírni.
        col.addView(sectionHead("✍️  Mondatból is megy", Examples.GROUPS.length + " helyen"), lp());
        col.addView(sentenceCard(accent), lp());
        col.addView(gap(18));

        // Megosztás: a mondat-felismerés másik oldala. Ezt semmi nem árulja
        // el a képernyőkön – a gomb ott van, de csak az találja meg, aki
        // véletlenül rákoppint.
        col.addView(sectionHead("📤  Megosztás", "3 dolog"), lp());
        col.addView(shareCard(accent), lp());
        col.addView(gap(18));

        // Megelőzés: a rehab-sorok a Nyújtás & rehab lapon élnek, de aki a
        // Könyvtárban keresi a gyakorlatokat, innen is odataláljon.
        col.addView(sectionHead("🩹  Megelőzés és rehab",
                Rehab.AREAS.length + " testtáj"), lp());
        LinearLayout rc = card();
        rc.setPadding(dp(16), dp(12), dp(16), dp(12));
        StringBuilder rl = new StringBuilder();
        for (Rehab.Area ar : Rehab.AREAS) {
            if (rl.length() > 0) rl.append("  ·  ");
            rl.append(ar.emoji).append(" ").append(ar.name);
        }
        rc.addView(text(rl.toString(), 13, TXT, false));
        TextView rh = text("Kész, 10–15 perces gyógytornász-ihletésű sorok, vezetett "
                + "móddal – koppints, és a Nyújtás & rehab lapra viszlek. A beviteli "
                + "mezők is értik: „fáj a vállam” vagy „boka stabilitás”.",
                12, MUTED, false);
        rh.setPadding(0, dp(6), 0, 0);
        rc.addView(rh);
        rc.setClickable(true);
        rc.setOnClickListener(v -> startActivity(
                new android.content.Intent(this, MobilityActivity.class)
                        .putExtra("open_rehab", true)));
        col.addView(rc, lp());
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

        // Sportágak: a kézi felvétel és az edzés-mondat ugyanebből a listából
        // dolgozik. Aki tudja, mit ismer fel az app, az bátrabban ír mondatot.
        col.addView(sectionHead("🏃  Ismert sportágak",
                Activities.ALL.length + " mozgásforma"), lp());
        col.addView(sportsCard(accent), lp());
        col.addView(gap(18));

        col.addView(text("Tipp: a saját programjaidhoz is adhatsz gyakorlatokat az „Edzés típusa” választóban.",
                12.5f, MUTED, false));

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_library", -1));
        col.post(() -> Ux.enterChildren(col, 30, 40));
    }

    /** Ugyanazok a példák, amiket a beviteli mezők is mutatnak. */
    LinearLayout sentenceCard(int accent) {
        LinearLayout card = card();
        String[][] groups = Examples.GROUPS;
        for (int i = 0; i < groups.length; i++) {
            String[] g = groups[i];
            String[] ex = Examples.byKey(g[2]);
            LinearLayout box = vbox();
            box.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.addView(text(g[0], 15.5f, TXT, true));
            box.addView(text(g[1], 12.5f, MUTED, false));
            // Három példa, naponta forogva – ugyanaz a válogatás, amit a
            // beviteli mezők tippjei is mutatnak.
            long day = Days.index(System.currentTimeMillis());
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

    /**
     * Mit lehet megosztani – és mi történik a másik oldalon.
     *
     * Mindhárom szövegként megy, ugyanabban az alakban, amit a felismerő
     * ért: a másik telefonon egy koppintás, és a helyére kerül. A Grit a
     * megosztás-listában is ott van, tehát bárhonnan ide küldhető egy
     * mondat.
     */
    LinearLayout shareCard(int accent) {
        LinearLayout card = card();
        String[][] rows = {
                {"⏱  Időzítő-sablon", "„Tabata: 8 kör 20 mp munka 10 mp pihenő”",
                        "A sablon melletti 📤 gombbal."},
                {"🏋️  Erősítő bejegyzés", "„guggolás 3x10 60 kg”",
                        "A naplóbejegyzésre koppintva, Megosztás."},
                {"📅  Edzésnap", "„Lábnap: Guggolás, Lábtolás, Kitörés”",
                        "Az edzésnap lapján, Megosztás."},
        };
        for (int i = 0; i < rows.length; i++) {
            LinearLayout box = vbox();
            box.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.addView(text(rows[i][0], 15.5f, TXT, true));
            TextView ex = text(rows[i][1], 13, accent, false);
            ex.setPadding(0, dp(4), 0, 0);
            box.addView(ex);
            TextView how = text(rows[i][2], 12.5f, MUTED, false);
            how.setPadding(0, dp(3), 0, 0);
            box.addView(how);
            card.addView(box);
            if (i < rows.length - 1) {
                View dv = new View(this);
                LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                dvp.leftMargin = dp(14); dvp.rightMargin = dp(14);
                dv.setLayoutParams(dvp);
                dv.setBackgroundColor(LINE);
                card.addView(dv);
            }
        }
        LinearLayout foot = vbox();
        foot.setPadding(dp(14), dp(4), dp(14), dp(12));
        foot.addView(text("A Grit a telefon megosztás-listájában is ott van: bármelyik "
                + "appból ide küldhetsz egy szöveget, és a megfelelő naplóba viszem.",
                12.5f, MUTED, false));
        card.addView(foot);
        return card;
    }

    /** A felismert sportágak, egy szokásos alkalom hosszával. */
    LinearLayout sportsCard(int accent) {
        LinearLayout card = card();
        Activities.Kind[] all = Activities.ALL;
        for (int i = 0; i < all.length; i++) {
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            row.addView(text(all[i].title(), 14.5f, TXT, false),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text(all[i].defaultMin + " perc", 12.5f, MUTED, false));
            card.addView(row);
            if (i < all.length - 1) {
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
