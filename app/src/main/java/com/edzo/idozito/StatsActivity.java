package com.edzo.idozito;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Összesítő statisztikák: heti / havi / összes összesítők és az elmúlt hetek
 * oszlopdiagramja (táv / idő / kalória / edzésszám).
 */
public class StatsActivity extends Activity {

    static final int BG = MainActivity.BG, CARD = MainActivity.CARD, CARD2 = MainActivity.CARD2;
    static final int TXT = MainActivity.TXT, MUTED = MainActivity.MUTED, LINE = MainActivity.LINE;
    static final long WEEK = 7L * 24 * 3600 * 1000;

    JSONArray hist;
    BarChart chart;
    TextView chartCaption;
    Button[] metricBtns = new Button[4];
    int metric = 0; // 0 táv, 1 idő, 2 kalória, 3 edzésszám

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        hist = History.load(this);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Statisztika", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Heti, havi és összesített teljesítményed.", 13, MUTED, false));
        col.addView(gap(20));

        long now = System.currentTimeMillis();
        long weekStart = weekStart(now);
        long monthStart = monthStart(now);

        col.addView(sectionTitle("Ezen a héten"));
        col.addView(totalsCard(totals(weekStart, now + 1)), lp());
        col.addView(gap(16));

        col.addView(sectionTitle("Ebben a hónapban"));
        col.addView(totalsCard(totals(monthStart, now + 1)), lp());
        col.addView(gap(16));

        col.addView(sectionTitle("Összesen"));
        col.addView(totalsCard(totals(0, now + 1)), lp());
        col.addView(gap(16));

        col.addView(sectionTitle("Rekordok"));
        col.addView(recordsCard(), lp());
        col.addView(gap(16));

        col.addView(sectionTitle("Jelvények"));
        col.addView(badgesCard(), lp());
        col.addView(gap(16));

        SimpleDateFormat mf = new SimpleDateFormat("yyyy. MMMM", new Locale("hu"));
        col.addView(sectionTitle("Naptár – " + mf.format(new Date())));
        col.addView(calendarCard(), lp());
        col.addView(gap(22));

        // Heti diagram
        col.addView(text("Elmúlt 8 hét", 17, TXT, true));
        col.addView(gap(12));
        LinearLayout chips = hbox();
        String[] ml = {"Táv", "Idő", "Kalória", "Edzés"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            Button c = chip(ml[i], i == metric);
            c.setOnClickListener(v -> { metric = idx; refreshChart(); });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
            clp.leftMargin = dp(3); clp.rightMargin = dp(3);
            chips.addView(c, clp);
            metricBtns[i] = c;
        }
        col.addView(chips, lp());
        col.addView(gap(12));

        LinearLayout chartCard = card();
        chartCard.setPadding(dp(10), dp(12), dp(12), dp(6));
        chart = new BarChart(this);
        chartCard.addView(chart, new LinearLayout.LayoutParams(-1, dp(200)));
        chartCaption = text("", 12.5f, MUTED, false);
        chartCaption.setGravity(Gravity.CENTER);
        chartCaption.setPadding(dp(6), dp(8), dp(6), dp(4));
        chartCard.addView(chartCaption);
        col.addView(chartCard, lp());

        if (hist.length() == 0) {
            col.addView(gap(16));
            col.addView(text("Még nincs elmentett edzés – fejezz be egyet, és itt megjelenik.", 12.5f, MUTED, false));
        }

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffold(this, sv, "bg_stats"));
        refreshChart();
        col.post(() -> Ux.enterChildren(col, 30, 45));
    }

    // ---------------- Aggregálás ----------------

    static class Totals {
        int count;
        double distM, durSec, cal, steps;
    }

    Totals totals(long from, long to) {
        Totals t = new Totals();
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null) continue;
            long ts = o.optLong("ts");
            if (ts < from || ts >= to) continue;
            t.count++;
            double d = o.optDouble("dist", -1);
            if (d > 0) t.distM += d;
            t.durSec += o.optInt("dur");
            t.cal += o.optDouble("cal", 0);
            t.steps += o.optInt("steps", 0);
        }
        return t;
    }

    LinearLayout totalsCard(Totals t) {
        LinearLayout grid = card();
        grid.setPadding(dp(6), dp(6), dp(6), dp(6));
        addTiles(grid, new String[][]{
                {"🔁 Edzések", String.valueOf(t.count)},
                {"📍 Táv", t.distM > 0 ? fmtDist(t.distM) : "—"},
                {"⏱ Idő", fmtDur((int) t.durSec)},
                {"🔥 Kalória", Math.round(t.cal) + " kcal"},
                {"👟 Lépések", t.steps > 0 ? String.valueOf((long) t.steps) : "—"},
                {"⚡ Átlag táv", t.count > 0 && t.distM > 0 ? fmtDist(t.distM / t.count) : "—"},
        });
        return grid;
    }

    // ---------------- Rekordok + heti sorozat ----------------

    LinearLayout recordsCard() {
        double bestDist = -1, bestAvg = -1, bestCal = 0;
        int bestDur = 0, bestSteps = 0;
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null) continue;
            double d = o.optDouble("dist", -1);
            if (d > bestDist) bestDist = d;
            int dur = o.optInt("dur");
            if (dur > bestDur) bestDur = dur;
            double avg = o.optDouble("avgspeed", -1);
            if (avg < 0 && d > 0 && dur > 0) avg = d / dur * 3.6;
            if (avg > bestAvg) bestAvg = avg;
            double cal = o.optDouble("cal", 0);
            if (cal > bestCal) bestCal = cal;
            int st = o.optInt("steps", 0);
            if (st > bestSteps) bestSteps = st;
        }
        int streak = weekStreak();
        LinearLayout grid = card();
        grid.setPadding(dp(6), dp(6), dp(6), dp(6));
        addTiles(grid, new String[][]{
                {"🔥 Heti sorozat", streak + " hét"},
                {"🏆 Leghosszabb táv", bestDist > 0 ? fmtDist(bestDist) : "—"},
                {"⏱ Leghosszabb edzés", bestDur > 0 ? fmtDur(bestDur) : "—"},
                {"⚡ Legjobb átlag", bestAvg > 0 ? fmtSpeed(bestAvg) : "—"},
                {"🍩 Legtöbb kalória", bestCal > 0 ? Math.round(bestCal) + " kcal" : "—"},
                {"👟 Legtöbb lépés", bestSteps > 0 ? String.valueOf(bestSteps) : "—"},
        });
        return grid;
    }

    /** Hány egymást követő héten volt legalább egy edzés (az aktuális vagy múlt héttől visszafelé). */
    int weekStreak() {
        java.util.HashSet<Long> weeks = new java.util.HashSet<>();
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o != null) weeks.add(weekStart(o.optLong("ts")));
        }
        long wk = weekStart(System.currentTimeMillis());
        if (!weeks.contains(wk)) wk = prevWeek(wk); // az aktuális hét még "türelmi idő"
        int streak = 0;
        while (weeks.contains(wk)) { streak++; wk = prevWeek(wk); }
        return streak;
    }

    /** Előző hét kezdete (óraátállítás-biztos: 3 nappal visszalépve normalizálunk). */
    long prevWeek(long ws) { return weekStart(ws - 3L * 24 * 3600 * 1000); }

    // ---------------- Jelvények ----------------

    LinearLayout badgesCard() {
        int count = 0;
        double totalDist = 0, totalCal = 0, bestDist = 0;
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null) continue;
            count++;
            double d = o.optDouble("dist", -1);
            if (d > 0) { totalDist += d; if (d > bestDist) bestDist = d; }
            totalCal += o.optDouble("cal", 0);
        }
        int streak = weekStreak();
        String[] emo = {"🥇", "🔟", "💯", "🏅", "🚀", "🔥", "⚡", "🌍", "🍔", "👑"};
        String[] name = {"Első edzés", "10 edzés", "50 edzés", "5 km egy edzésen",
                "10 km egy edzésen", "3 hetes sorozat", "8 hetes sorozat",
                "100 km összesen", "5000 kcal összesen", "100 edzés"};
        boolean[] got = {count >= 1, count >= 10, count >= 50, bestDist >= 5000,
                bestDist >= 10000, streak >= 3, streak >= 8,
                totalDist >= 100000, totalCal >= 5000, count >= 100};

        LinearLayout grid = card();
        grid.setPadding(dp(6), dp(6), dp(6), dp(6));
        for (int i = 0; i < name.length; i += 2) {
            LinearLayout row = hbox();
            row.addView(badgeTile(emo[i], name[i], got[i]), tileLp());
            if (i + 1 < name.length) row.addView(badgeTile(emo[i + 1], name[i + 1], got[i + 1]), tileLp());
            else row.addView(new View(this), tileLp());
            grid.addView(row, lp());
        }
        int earned = 0;
        for (boolean g : got) if (g) earned++;
        TextView cap = text(earned + " / " + name.length + " jelvény megszerezve", 12, MUTED, false);
        cap.setGravity(Gravity.CENTER);
        cap.setPadding(0, dp(4), 0, dp(6));
        grid.addView(cap, lp());
        return grid;
    }

    View badgeTile(String emoji, String title, boolean earned) {
        View v = tile(title, earned ? emoji : "🔒");
        v.setAlpha(earned ? 1f : 0.45f);
        return v;
    }

    // ---------------- Naptár (aktuális hónap) ----------------

    LinearLayout calendarCard() {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR), month = now.get(Calendar.MONTH);
        int today = now.get(Calendar.DAY_OF_MONTH);
        java.util.HashSet<Integer> days = new java.util.HashSet<>();
        Calendar c2 = Calendar.getInstance();
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null) continue;
            c2.setTimeInMillis(o.optLong("ts"));
            if (c2.get(Calendar.YEAR) == year && c2.get(Calendar.MONTH) == month)
                days.add(c2.get(Calendar.DAY_OF_MONTH));
        }

        LinearLayout cardC = card();
        cardC.setPadding(dp(12), dp(12), dp(12), dp(12));
        String[] dow = {"H", "K", "Sze", "Cs", "P", "Szo", "V"};
        LinearLayout head = hbox();
        for (String s : dow) {
            TextView t = text(s, 11, MUTED, true);
            t.setGravity(Gravity.CENTER);
            head.addView(t, cellLp());
        }
        cardC.addView(head, lp());

        Calendar first = Calendar.getInstance();
        first.set(year, month, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7; // hétfő = 0
        int dim = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int day = 1;
        while (day <= dim) {
            LinearLayout row = hbox();
            for (int c = 0; c < 7; c++) {
                if ((day == 1 && c < offset) || day > dim) {
                    row.addView(new View(this), cellLp());
                    continue;
                }
                boolean did = days.contains(day);
                TextView t = text(String.valueOf(day), 12.5f,
                        did ? 0xFFFFFFFF : (day == today ? TXT : MUTED), did || day == today);
                t.setGravity(Gravity.CENTER);
                t.setPadding(0, dp(7), 0, dp(7));
                if (did) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Theme.accent(this));
                    bg.setCornerRadius(dp(10));
                    t.setBackground(bg);
                } else if (day == today) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(0);
                    bg.setStroke(dp(1), Theme.accent(this));
                    bg.setCornerRadius(dp(10));
                    t.setBackground(bg);
                }
                row.addView(t, cellLp());
                day++;
            }
            cardC.addView(row, lp());
        }
        TextView cap = text(days.size() + " edzésnap ebben a hónapban", 12, MUTED, false);
        cap.setGravity(Gravity.CENTER);
        cap.setPadding(0, dp(8), 0, 0);
        cardC.addView(cap, lp());
        return cardC;
    }

    LinearLayout.LayoutParams cellLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.leftMargin = dp(2); p.rightMargin = dp(2); p.topMargin = dp(2); p.bottomMargin = dp(2);
        return p;
    }

    String fmtSpeed(double kmh) {
        if (Theme.paceMode(this) && kmh > 0) {
            double pace = 3600.0 / kmh;
            int m = (int) (pace / 60), s = (int) Math.round(pace - m * 60);
            if (s == 60) { m++; s = 0; }
            return String.format(Locale.US, "%d:%02d /km", m, s);
        }
        return String.format(Locale.US, "%.1f km/h", kmh);
    }

    void refreshChart() {
        for (int i = 0; i < 4; i++) styleChip(metricBtns[i], i == metric);
        long thisWeek = weekStart(System.currentTimeMillis());
        int n = 8;
        double[] vals = new double[n];
        String[] labels = new String[n];
        SimpleDateFormat df = new SimpleDateFormat("MM.dd", new Locale("hu"));
        double total = 0;
        for (int k = 0; k < n; k++) {
            long start = thisWeek - (long) (n - 1 - k) * WEEK;
            long end = start + WEEK;
            Totals t = totals(start, end);
            double v;
            switch (metric) {
                case 1: v = t.durSec / 60.0; break;     // perc
                case 2: v = t.cal; break;                // kcal
                case 3: v = t.count; break;              // db
                default: v = t.distM / 1000.0; break;    // km
            }
            vals[k] = v;
            total += v;
            labels[k] = df.format(new Date(start));
        }
        int color = Theme.accent(this);
        String unit = metric == 0 ? "km" : metric == 1 ? "perc" : metric == 2 ? "kcal" : "db";
        chart.setData(vals, labels, color, unit);
        chartCaption.setText(metricLabel() + " · összesen " + fmtNum(total) + " " + unit + " (8 hét)");
    }

    String metricLabel() {
        return metric == 0 ? "Táv" : metric == 1 ? "Idő" : metric == 2 ? "Kalória" : "Edzésszám";
    }

    // ---------------- Idő segédek ----------------

    long weekStart(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    long monthStart(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // ---------------- Format ----------------

    String fmtDist(double m) {
        if (m < 0) return "—";
        if (m < 1000) return Math.round(m) + " m";
        return String.format(Locale.US, "%.1f km", m / 1000.0);
    }
    String fmtDur(int sec) {
        if (sec < 0) sec = 0;
        int h = sec / 3600, m = (sec % 3600) / 60;
        if (h > 0) return h + " ó " + m + " p";
        return m + " p " + (sec % 60) + " mp";
    }
    String fmtNum(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return String.valueOf(Math.round(v));
        return String.format(Locale.US, "%.1f", v);
    }

    // ---------------- UI segéd ----------------

    TextView sectionTitle(String s) {
        TextView t = text(s, 15.5f, MUTED, true);
        t.setPadding(dp(2), 0, 0, dp(8));
        return t;
    }

    void addTiles(LinearLayout grid, String[][] items) {
        for (int i = 0; i < items.length; i += 2) {
            LinearLayout row = hbox();
            row.addView(tile(items[i][0], items[i][1]), tileLp());
            if (i + 1 < items.length) row.addView(tile(items[i + 1][0], items[i + 1][1]), tileLp());
            else row.addView(new View(this), tileLp());
            grid.addView(row, lp());
        }
    }

    LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.leftMargin = dp(4); p.rightMargin = dp(4); p.topMargin = dp(4); p.bottomMargin = dp(4);
        return p;
    }

    View tile(String label, String value) {
        LinearLayout t = vbox();
        t.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xD9212B47); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0x24FFFFFF);
        t.setBackground(bg);
        t.addView(text(value, 18, TXT, true));
        t.addView(text(label, 12, MUTED, false));
        return t;
    }

    Button chip(String label, boolean sel) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD); b.setTextSize(14);
        b.setPadding(dp(8), dp(11), dp(8), dp(11));
        b.setStateListAnimator(null);
        styleChip(b, sel);
        return b;
    }

    void styleChip(Button b, boolean sel) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (sel) { bg.setColor(Theme.accent(this)); b.setTextColor(0xFFFFFFFF); }
        else { bg.setColor(CARD2); bg.setStroke(dp(1), LINE); b.setTextColor(TXT); }
        b.setBackground(bg);
    }

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE61A2238); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), 0x33FFFFFF);
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

    // ---------------- Oszlopdiagram ----------------

    static class BarChart extends View {
        private double[] vals;
        private String[] labels;
        private int color = 0xFF6366F1;
        private String unit = "";
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;

        BarChart(Context c) {
            super(c);
            density = c.getResources().getDisplayMetrics().density;
            bar.setStyle(Paint.Style.FILL);
            txt.setColor(MUTED);
            txt.setTextSize(density * 10f);
            txt.setTextAlign(Paint.Align.CENTER);
        }

        void setData(double[] vals, String[] labels, int color, String unit) {
            this.vals = vals; this.labels = labels; this.color = color; this.unit = unit;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (vals == null || vals.length == 0) return;
            float W = getWidth(), H = getHeight();
            float padB = density * 20, padT = density * 18;
            double max = 0;
            for (double v : vals) max = Math.max(max, v);
            if (max <= 0) max = 1;
            int n = vals.length;
            float slot = W / n;
            float bw = slot * 0.56f;
            bar.setColor(color);
            for (int i = 0; i < n; i++) {
                float cx = slot * i + slot / 2f;
                float bh = (float) ((H - padB - padT) * (vals[i] / max));
                float top = H - padB - bh;
                float left = cx - bw / 2, right = cx + bw / 2;
                canvas.drawRoundRect(left, top, right, H - padB, density * 4, density * 4, bar);
                if (vals[i] > 0) {
                    txt.setColor(TXT);
                    canvas.drawText(fmt(vals[i]), cx, top - density * 4, txt);
                }
                if (labels != null && i < labels.length) {
                    txt.setColor(MUTED);
                    canvas.drawText(labels[i], cx, H - density * 6, txt);
                }
            }
        }

        private String fmt(double v) {
            if (Math.abs(v - Math.round(v)) < 0.05) return String.valueOf(Math.round(v));
            return String.format(Locale.US, "%.1f", v);
        }
    }
}
