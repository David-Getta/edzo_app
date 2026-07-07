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
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
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
        setContentView(sv);
        refreshChart();
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
        bg.setColor(CARD2); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), LINE);
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
        bg.setColor(CARD); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), LINE);
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
