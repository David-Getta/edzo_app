package com.edzo.idozito;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Látványos, teljes képernyős edzésnapló: hero összegző + egyedi, „aurora-üveg"
 * kártyák akcentcsíkkal, típusjelvénnyel és mini-statokkal. Koppintásra részletek.
 */
public class HistoryActivity extends Activity {

    static int BG, TXT, MUTED, LINE, GLASS, GLASS_LINE;
    static final int RUN_C = 0xFFE11D2E, GYM_C = 0xFFFF4757; // futás = cián / erő = magenta

    int accent, accent2;
    boolean pace;
    int lastCount = -1; // az onCreate-kori edzésszám (törlés után frissítéshez)
    // Szűrő: 0 = mind, 1 = futás, 2 = erő/gyakorlat
    int filter = 0;

    /** Egyszerre ennyi edzés-kártya épül fel; a gomb továbbiakat tölt be. */
    static final int PAGE = 60;
    int shownLimit = PAGE;
    LinearLayout listBox;
    JSONArray histArr;
    java.util.List<StrengthLog.Entry> strArr;
    SimpleDateFormat listDf;
    final TextView[] filterChips = new TextView[3];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); BG=MainActivity.BG; TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; LINE=MainActivity.LINE; GLASS=MainActivity.GLASS; GLASS_LINE=MainActivity.GLASS_LINE;
        accent = Theme.accent(this);
        accent2 = Theme.accent2(this);
        pace = Theme.paceMode(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        addBg(root);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = vbox();
        col.setPadding(dp(18), dp(22), dp(18), dp(40));

        // A gazdátlanul maradt részletfájlok (törölt edzések GPS-nyoma) itt
        // takarodnak ki – az Előzmények az a képernyő, ahol amúgy is a teljes
        // naplót beolvassuk, tehát nincs külön költsége.
        SessionStore.cleanupOrphans(this);

        JSONArray arr = History.load(this);
        // A súlyzós napló bejegyzései is ide kerülnek (egyesített idővonal).
        java.util.List<StrengthLog.Entry> sArr = StrengthLog.load(this);
        lastCount = arr.length() + sArr.size();

        // Fejléc
        LinearLayout head = hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout htexts = vbox();
        htexts.addView(text("Előzmények", 27, TXT, true));
        htexts.addView(text((arr.length() + sArr.size()) + " elmentett edzés", 13.5f, MUTED, false));
        head.addView(htexts, new LinearLayout.LayoutParams(0, -2, 1f));
        if (arr.length() > 0) head.addView(trashButton());
        col.addView(head, lp());
        col.addView(gap(18));

        if (arr.length() == 0 && sArr.isEmpty()) {
            col.addView(emptyState());
        } else {
            if (arr.length() > 0) {
                col.addView(heroSummary(arr), lp());
                col.addView(gap(14));
            }
            histArr = arr;
            strArr = sArr;
            listDf = new SimpleDateFormat("yyyy. MMM d. · HH:mm", new Locale("hu"));
            col.addView(filterRow(), lp());
            col.addView(gap(12));
            listBox = vbox();
            col.addView(listBox, lp());
            renderList();
        }

        sv.addView(col, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv);
        LinearLayout navStack = new LinearLayout(this);
        navStack.setOrientation(LinearLayout.VERTICAL);
        navStack.addView(root, new LinearLayout.LayoutParams(-1, 0, 1f));
        navStack.addView(Ux.bottomNav(this, -1), new LinearLayout.LayoutParams(-1, -2));
        setContentView(navStack);
        col.post(() -> Ux.enterChildren(col, 30, 55)); // beúszó kártyák
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ha időközben törölték egy edzést (a részletek nézetből), frissítsük a listát.
        if (lastCount >= 0
                && History.load(this).length() + StrengthLog.load(this).size() != lastCount)
            recreate();
    }

    // ---- Szűrő ----

    View filterRow() {
        LinearLayout row = hbox();
        String[] labels = {"Mind", "🏃 Futás", "🏋️ Erő"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView c = text(labels[i], 13, TXT, true);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(10), dp(9), dp(10), dp(9));
            c.setClickable(true);
            c.setOnClickListener(v -> { filter = idx; styleFilters(); renderList(); });
            filterChips[i] = c;
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
            clp.leftMargin = dp(3); clp.rightMargin = dp(3);
            row.addView(c, clp);
        }
        styleFilters();
        return row;
    }

    void styleFilters() {
        for (int i = 0; i < 3; i++) {
            boolean sel = i == filter;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(13));
            if (sel) { bg.setColor(accent); filterChips[i].setTextColor(0xFFFFFFFF); }
            else { bg.setColor(GLASS); bg.setStroke(dp(1), GLASS_LINE); filterChips[i].setTextColor(TXT); }
            filterChips[i].setBackground(bg);
        }
    }

    void renderList() {
        if (listBox == null) return;
        listBox.removeAllViews();
        // Egyesített, idő szerint csökkenő idővonal: időzítős + súlyzós bejegyzések.
        java.util.ArrayList<Object[]> items = new java.util.ArrayList<>();
        for (int i = 0; i < histArr.length(); i++) {
            JSONObject o = histArr.optJSONObject(i);
            if (o != null) items.add(new Object[]{o.optLong("ts"), o});
        }
        if (strArr != null) for (StrengthLog.Entry e : strArr) items.add(new Object[]{e.ts, e});
        java.util.Collections.sort(items, (a, b) -> Long.compare((long) b[0], (long) a[0]));

        int shown = 0, matching = 0;
        for (Object[] it : items) {
            boolean fits;
            if (it[1] instanceof JSONObject) {
                boolean isRun = ((JSONObject) it[1]).optString("name", "").isEmpty();
                fits = !((filter == 1 && !isRun) || (filter == 2 && isRun));
            } else {
                fits = filter != 1;   // súlyzós bejegyzés nem futás
            }
            if (!fits) continue;
            matching++;
            // Sok év edzése esetén ne épüljön fel minden kártya egyszerre.
            if (shown >= shownLimit) continue;
            if (it[1] instanceof JSONObject)
                listBox.addView(entryCard((JSONObject) it[1], listDf), lp());
            else
                listBox.addView(strengthCard((StrengthLog.Entry) it[1], listDf), lp());
            listBox.addView(gap(12));
            shown++;
        }
        if (shown == 0) {
            TextView none = text("Nincs ilyen típusú edzés.", 13, MUTED, false);
            none.setPadding(dp(4), dp(10), 0, 0);
            listBox.addView(none);
        } else if (matching > shown) {
            final int remaining = matching - shown;
            TextView more = text("További " + Math.min(remaining, PAGE) + " edzés betöltése  ("
                    + remaining + " van még)", 13.5f, TXT, true);
            more.setGravity(Gravity.CENTER);
            more.setPadding(dp(14), dp(14), dp(14), dp(14));
            GradientDrawable mb = new GradientDrawable();
            mb.setColor(GLASS);
            mb.setCornerRadius(dp(18));
            mb.setStroke(dp(1), GLASS_LINE);
            more.setBackground(mb);
            more.setClickable(true);
            more.setOnClickListener(v -> { shownLimit += PAGE; renderList(); });
            listBox.addView(more, lp());
        }
    }

    /** Súlyzós naplóbejegyzés kártyája az egyesített idővonalon. */
    View strengthCard(StrengthLog.Entry e, SimpleDateFormat df) {
        LinearLayout outer = hbox();
        GradientDrawable obg = new GradientDrawable();
        obg.setColor(GLASS);
        obg.setCornerRadius(dp(22));
        obg.setStroke(dp(1), GLASS_LINE);
        outer.setBackground(obg);
        outer.setClickable(true);
        outer.setOnClickListener(v -> startActivity(new Intent(this, StrengthActivity.class)));

        View stripe = new View(this);
        GradientDrawable sg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{GYM_C, (GYM_C & 0xFFFFFF) | 0x66000000});
        sg.setCornerRadius(dp(3));
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(dp(5), -1);
        stlp.topMargin = dp(14); stlp.bottomMargin = dp(14); stlp.leftMargin = dp(10); stlp.rightMargin = dp(12);
        outer.addView(stripe, stlp);

        LinearLayout body = vbox();
        body.setPadding(0, dp(14), dp(14), dp(14));
        LinearLayout topRow = hbox();
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.addView(badge("💪 " + e.name, GYM_C), new LinearLayout.LayoutParams(-2, -2));
        View sp = new View(this);
        topRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        topRow.addView(text(df.format(new Date(e.ts)), 11.5f, MUTED, false));
        body.addView(topRow, lp());
        body.addView(gap(8));
        String det = e.sets.size() + " sorozat  ·  " + e.totalReps() + " ismétlés"
                + (e.topWeight() > 0
                    ? "  ·  max " + (e.topWeight() == Math.floor(e.topWeight())
                        ? String.valueOf((long) e.topWeight()) : String.valueOf(e.topWeight())) + " kg"
                    : "");
        body.addView(text(det, 13, TXT, false));
        outer.addView(body, new LinearLayout.LayoutParams(0, -2, 1f));
        return outer;
    }

    // ---- Hero összegző ----

    View heroSummary(JSONArray arr) {
        int count = arr.length();
        double dist = 0, cal = 0;
        long dur = 0;
        long weekFrom = weekStart();
        int thisWeek = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            double dd = o.optDouble("dist", -1);
            if (dd > 0) dist += dd;
            cal += o.optDouble("cal", 0);
            dur += o.optInt("dur");
            if (o.optLong("ts") >= weekFrom) thisWeek++;
        }
        FrameLayout wrap = new FrameLayout(this);
        roundClip(wrap, 24);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{accent, accent2});
        wrap.setBackground(g);
        LinearLayout inner = vbox();
        inner.setPadding(dp(18), dp(16), dp(18), dp(16));
        inner.addView(text("Összes teljesítményed", 13, 0xE6FFFFFF, true));
        inner.addView(gap(12));
        final double distKm = dist / 1000.0;
        LinearLayout r1 = hbox();
        r1.addView(heroStatNum("🔁", count, "edzés", v -> String.valueOf((int) v)), heroLp());
        r1.addView(heroStatNum("📍", (float) distKm, "össztáv",
                v -> v <= 0 ? "0" : String.format(Locale.US, "%.1f km", v)), heroLp());
        inner.addView(r1, lp());
        inner.addView(gap(10));
        LinearLayout r2 = hbox();
        r2.addView(heroStat("⏱", fmtHm((int) dur), "összidő"), heroLp());
        r2.addView(heroStatNum("🔥", (float) cal, "kcal", v -> String.valueOf(Math.round(v))), heroLp());
        inner.addView(r2, lp());
        inner.addView(gap(12));
        TextView wk = text("🗓  " + thisWeek + " edzés ezen a héten", 12.5f, 0xF2FFFFFF, true);
        inner.addView(wk);
        wrap.addView(inner);
        return wrap;
    }

    View heroStat(String emoji, String value, String label) {
        LinearLayout c = vbox();
        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(emoji + " ", 15, 0xF2FFFFFF, false));
        top.addView(text(value, 22, 0xFFFFFFFF, true));
        c.addView(top);
        c.addView(text(label, 12, 0xC8FFFFFF, false));
        return c;
    }

    View heroStatNum(String emoji, float target, String label, Ux.Fmt f) {
        LinearLayout c = vbox();
        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(emoji + " ", 15, 0xF2FFFFFF, false));
        TextView val = text("0", 22, 0xFFFFFFFF, true);
        top.addView(val);
        c.addView(top);
        c.addView(text(label, 12, 0xC8FFFFFF, false));
        Ux.countUp(val, target, f);
        return c;
    }

    LinearLayout.LayoutParams heroLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        return p;
    }

    // ---- Egy edzés kártya ----

    View entryCard(JSONObject o, SimpleDateFormat df) {
        final long ts = o.optLong("ts");
        String name = o.optString("name", "");
        boolean isRun = name.isEmpty();
        int typeColor = isRun ? RUN_C : GYM_C;

        // Külső: akcentcsík + tartalom
        LinearLayout outer = hbox();
        GradientDrawable obg = new GradientDrawable();
        obg.setColor(GLASS);
        obg.setCornerRadius(dp(22));
        obg.setStroke(dp(1), GLASS_LINE);
        outer.setBackground(obg);
        outer.setClickable(true);
        outer.setOnClickListener(v ->
                startActivity(new Intent(this, WorkoutDetailActivity.class).putExtra("ts", ts)));

        // Akcentcsík
        View stripe = new View(this);
        GradientDrawable sg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{typeColor, (typeColor & 0xFFFFFF) | 0x66000000});
        sg.setCornerRadius(dp(3));
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(dp(5), -1);
        stlp.topMargin = dp(14); stlp.bottomMargin = dp(14); stlp.leftMargin = dp(10); stlp.rightMargin = dp(12);
        outer.addView(stripe, stlp);

        LinearLayout body = vbox();
        body.setPadding(0, dp(14), dp(14), dp(14));

        // Fejsor: típusjelvény + dátum
        LinearLayout topRow = hbox();
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.addView(badge(isRun ? "🏃 Futás" : "🏋️ " + name, typeColor), new LinearLayout.LayoutParams(-2, -2));
        View sp = new View(this);
        topRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        String moodE = History.moodEmoji(o.optInt("mood", 0));
        if (!moodE.isEmpty()) topRow.addView(text(moodE + "  ", 15, TXT, false));
        topRow.addView(text(df.format(new Date(ts)), 11.5f, MUTED, false));
        body.addView(topRow, lp());
        body.addView(gap(10));

        // Fő metrika
        double dist = o.optDouble("dist", -1);
        int durS = o.optInt("dur");
        LinearLayout metric = hbox();
        metric.setGravity(Gravity.CENTER_VERTICAL);
        metric.addView(text("⏱ " + fmtDur(durS), 22, TXT, true));
        if (dist >= 0) {
            metric.addView(text("   📍 " + fmtDist(dist), 16, accentLight(), true));
        }
        body.addView(metric, lp());
        body.addView(gap(10));

        // Mini-statok
        LinearLayout pills = hbox();
        addPill(pills, "🔁", o.optInt("rounds") + (isRun ? " kör" : "×"));
        int cal = (int) Math.round(o.optDouble("cal", 0));
        if (cal > 0) addPill(pills, "🔥", cal + "");
        if (dist >= 0) {
            double avg = o.optDouble("avgspeed", -1);
            if (avg < 0 && durS > 0) avg = dist / durS * 3.6;
            if (avg > 0) addPill(pills, "⚡", fmtSpeed(avg));
        }
        int steps = o.optInt("steps", 0);
        if (steps > 0) addPill(pills, "👟", steps + "");
        body.addView(pills, lp());

        outer.addView(body, new LinearLayout.LayoutParams(0, -2, 1f));
        return outer;
    }

    View badge(String label, int color) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(12);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(0xFFFFFFFF);
        t.setPadding(dp(10), dp(5), dp(10), dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor((color & 0xFFFFFF) | 0x40000000);
        bg.setStroke(dp(1), (color & 0xFFFFFF) | 0x80000000);
        bg.setCornerRadius(dp(20));
        t.setBackground(bg);
        return t;
    }

    void addPill(LinearLayout parent, String emoji, String value) {
        TextView t = new TextView(this);
        t.setText(emoji + " " + value);
        t.setTextSize(12.5f);
        t.setTextColor(0xFFD6DEF5);
        t.setPadding(dp(9), dp(6), dp(9), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setCornerRadius(dp(12));
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = dp(7);
        parent.addView(t, lp);
    }

    View emptyState() {
        LinearLayout c = vbox();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(20), dp(60), dp(20), dp(20));
        c.addView(centered(text("🏃", 54, TXT, false)));
        c.addView(gap(14));
        TextView t = text("Még nincs elmentett edzés", 17, TXT, true);
        t.setGravity(Gravity.CENTER);
        c.addView(t);
        c.addView(gap(6));
        TextView s = text("Fejezz be egy edzést, és itt megjelenik – minden mérőszámmal és térképpel együtt.", 13.5f, MUTED, false);
        s.setGravity(Gravity.CENTER);
        c.addView(s);
        return c;
    }

    View trashButton() {
        TextView t = new TextView(this);
        t.setText("🗑");
        t.setTextSize(18);
        t.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(GLASS);
        bg.setStroke(dp(1), GLASS_LINE);
        t.setBackground(bg);
        t.setClickable(true);
        int s = dp(44);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        t.setLayoutParams(lp);
        t.setPadding(0, dp(9), 0, 0);
        t.setOnClickListener(v -> new Sheet(this, "Napló törlése", "Biztosan törlöd az összes elmentett edzést? Ez nem vonható vissza.")
                .addDestructive("Összes törlése", () -> { History.clear(this); recreate(); })
                .addCancel()
                .show());
        return t;
    }

    // ---- Háttér ----

    void addBg(FrameLayout host) {
        if (Theme.light(this)) {
            View g = new View(this);
            g.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFFEDF1F8, 0xFFF3F5FA}));
            host.addView(g, new FrameLayout.LayoutParams(-1, -1));
            return;
        }
        int id = getResources().getIdentifier("bg_history", "drawable", getPackageName());
        if (id == 0) id = getResources().getIdentifier("bg_main", "drawable", getPackageName());
        if (id == 0) return;
        try {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageResource(id);
            host.addView(img, new FrameLayout.LayoutParams(-1, -1));
            Ux.kenBurns(img);
            View scrim = new View(this);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x99140B0D, 0xE0140B0D, 0xF5140B0D});
            scrim.setBackground(g);
            host.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        } catch (Exception ignored) {}
    }

    void roundClip(View v, final int radiusDp) {
        v.setClipToOutline(true);
        v.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radiusDp));
            }
        });
    }

    int accentLight() { return accent2; }

    // ---- Formázás ----

    String fmtDur(int sec) {
        if (sec < 0) sec = 0;
        int h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s)
                : String.format(Locale.US, "%d:%02d", m, s);
    }
    String fmtHm(int sec) {
        int h = sec / 3600, m = (sec % 3600) / 60;
        return h > 0 ? h + "ó " + m + "p" : m + " perc";
    }
    String fmtDist(double m) {
        if (m < 0) return "—";
        if (m < 1000) return Math.round(m) + " m";
        return String.format(Locale.US, "%.2f km", m / 1000.0);
    }
    String fmtKm(double m) {
        if (m <= 0) return "0";
        return String.format(Locale.US, "%.1f km", m / 1000.0);
    }
    String fmtSpeed(double kmh) {
        if (pace && kmh > 0) {
            double p = 3600.0 / kmh;
            int m = (int) (p / 60), s = (int) Math.round(p - m * 60);
            if (s == 60) { m++; s = 0; }
            return String.format(Locale.US, "%d:%02d/km", m, s);
        }
        return String.format(Locale.US, "%.1f", kmh) + (pace ? "" : " km/h");
    }

    long weekStart() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        c.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // ---- UI segéd ----

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }

    View centered(View v) {
        LinearLayout w = hbox();
        w.setGravity(Gravity.CENTER);
        w.addView(v);
        return w;
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
