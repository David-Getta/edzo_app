package com.edzo.idozito;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
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

    static int BG, CARD, CARD2, TXT, MUTED, LINE;
    static final long WEEK = 7L * 24 * 3600 * 1000;

    JSONArray hist;
    BarChart chart;
    TextView chartCaption;
    Button[] metricBtns = new Button[4];
    int metric = 0; // 0 táv, 1 idő, 2 kalória, 3 edzésszám
    int lastCount = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); BG=MainActivity.BG; CARD=MainActivity.CARD; CARD2=MainActivity.CARD2; TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; LINE=MainActivity.LINE;
        hist = History.load(this);
        lastCount = hist.length() + StrengthLog.load(this).size();

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Statisztika", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Heti, havi és összesített teljesítményed.", 13, MUTED, false));
        col.addView(gap(18));

        col.addView(levelCard(), lp());
        col.addView(gap(10));
        Button shareStats = chip("📤  Statisztika megosztása képként", false);
        shareStats.setOnClickListener(v -> shareStats());
        col.addView(shareStats, lp());
        col.addView(gap(18));

        long now = System.currentTimeMillis();
        long weekStart = weekStart(now);
        long monthStart = monthStart(now);

        col.addView(sectionTitle("Ezen a héten"));
        col.addView(totalsCard(totals(weekStart, now + 1)), lp());
        col.addView(gap(10));
        col.addView(trendCard(weekStart, now), lp());
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

        int[] moodCounts = moodCounts();
        int moodTotal = moodCounts[1] + moodCounts[2] + moodCounts[3] + moodCounts[4];
        if (moodTotal > 0) {
            col.addView(sectionTitle("Hangulat"));
            col.addView(moodCard(moodCounts, moodTotal), lp());
            col.addView(gap(16));
        }

        if (!MealLog.load(this).isEmpty()) {
            col.addView(sectionTitle("Étrend – elmúlt 7 nap"));
            col.addView(dietCard(), lp());
            col.addView(gap(10));
            col.addView(dietMonthCard(), lp());
            col.addView(gap(16));
        }

        col.addView(sectionTitle("Jelvények"));
        col.addView(badgesCard(), lp());
        col.addView(gap(16));

        col.addView(sectionTitle("Aktivitás – elmúlt 12 hét"));
        col.addView(heatmapCard(), lp());
        col.addView(gap(16));

        if (!Theme.planDays(this).isEmpty()) {
            col.addView(sectionTitle("Terv-teljesítés – elmúlt 4 hét"));
            col.addView(adherenceCard(), lp());
            col.addView(gap(16));
        }

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
        setContentView(Ux.scaffoldNav(this, sv, "bg_stats", 1));
        refreshChart();
        col.post(() -> Ux.enterChildren(col, 30, 45));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ha időközben változott az edzésszám (pl. törlés), frissítsük a statisztikát.
        if (lastCount >= 0
                && History.load(this).length() + StrengthLog.load(this).size() != lastCount)
            recreate();
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

    // ---------------- Szint / XP ----------------

    View levelCard() {
        long xp = Levels.totalXp(this); // erősítő edzésekkel együtt
        int lvl = Levels.levelForXp(xp);
        float frac = Levels.progress(xp);
        long toNext = Levels.xpToNext(xp);

        LinearLayout c = vbox();
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.accent(this), Theme.accent2(this)});
        g.setCornerRadius(dp(22));
        c.setBackground(g);
        c.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = vbox();
        left.addView(text("⭐ Szint " + lvl, 26, 0xFFFFFFFF, true));
        left.addView(text(Levels.title(lvl), 14, 0xE6FFFFFF, true));
        top.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView xpVal = text("0 XP", 15, 0xFFFFFFFF, true);
        xpVal.setGravity(Gravity.END);
        top.addView(xpVal);
        c.addView(top, lp());
        c.addView(gap(12));

        // Animált haladássáv
        LinearLayout barBg = hbox();
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(0x40000000);
        bgd.setCornerRadius(dp(7));
        barBg.setBackground(bgd);
        final View fill = new View(this);
        GradientDrawable fgd = new GradientDrawable();
        fgd.setColor(0xFFFFFFFF);
        fgd.setCornerRadius(dp(7));
        fill.setBackground(fgd);
        final LinearLayout.LayoutParams fillLp = new LinearLayout.LayoutParams(0, dp(14), 0.0001f);
        final LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, dp(14), 1f);
        final View spacer = new View(this);
        barBg.addView(fill, fillLp);
        barBg.addView(spacer, spLp);
        c.addView(barBg, new LinearLayout.LayoutParams(-1, -2));
        c.addView(gap(10));

        c.addView(text(toNext > 0 ? toNext + " XP a(z) " + (lvl + 1) + ". szintig ("
                + Levels.title(lvl + 1) + ")" : "Maximális szint elérve! 🏆", 12.5f, 0xE6FFFFFF, false));
        long bonus = getSharedPreferences("edzo", MODE_PRIVATE).getLong("bonus_xp", 0);
        if (bonus > 0) {
            TextView bx = text("🎯 Ebből kihívás-bónusz: " + bonus + " XP", 11.5f, 0xC9FFFFFF, false);
            bx.setPadding(0, dp(4), 0, 0);
            c.addView(bx);
        }

        // animációk (a Beállítások „díszítő animációk" kapcsolója némítja)
        final float target = Math.max(0.0001f, frac);
        if (!Theme.animEnabled(this)) {
            fillLp.weight = target;
            spLp.weight = 1f - target;
            barBg.requestLayout();
        } else {
            android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0f, target);
            a.setDuration(900);
            a.setStartDelay(150);
            a.setInterpolator(new android.view.animation.DecelerateInterpolator());
            a.addUpdateListener(an -> {
                float v = (float) an.getAnimatedValue();
                fillLp.weight = v;
                spLp.weight = 1f - v;
                barBg.requestLayout();
            });
            a.start();
        }
        Ux.countUp(xpVal, xp, v -> Math.round(v) + " XP");
        return c;
    }

    /** Az elmúlt 28 nap tervezett edzésnapjaiból hányat sikerült teljesíteni. */
    LinearLayout adherenceCard() {
        java.util.HashSet<Long> days = new java.util.HashSet<>();
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null) continue;
            c.setTimeInMillis(o.optLong("ts"));
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
            days.add(c.getTimeInMillis());
        }
        Calendar cur = Calendar.getInstance();
        cur.set(Calendar.HOUR_OF_DAY, 0); cur.set(Calendar.MINUTE, 0);
        cur.set(Calendar.SECOND, 0); cur.set(Calendar.MILLISECOND, 0);
        int planned = 0, done = 0;
        for (int k = 0; k < 28; k++) {
            int dowIdx = (cur.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            if (Theme.isPlanDay(this, dowIdx)) {
                planned++;
                if (days.contains(cur.getTimeInMillis())) done++;
            }
            cur.add(Calendar.DAY_OF_YEAR, -1);
        }
        int pct = planned == 0 ? 0 : (int) Math.round(100.0 * done / planned);

        LinearLayout cardV = card();
        cardV.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView big = text(pct + "%", 26, Theme.accent(this), true);
        row.addView(big, new LinearLayout.LayoutParams(-2, -2));
        TextView det = text("  " + done + " / " + planned + " tervezett edzésnap teljesítve",
                13.5f, MUTED, false);
        row.addView(det);
        cardV.addView(row, lp());
        cardV.addView(gap(10));

        LinearLayout barBg = hbox();
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(Theme.track(this));
        bgd.setCornerRadius(dp(6));
        barBg.setBackground(bgd);
        View fill = new View(this);
        GradientDrawable fgd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Theme.accent(this), Theme.accent2(this)});
        fgd.setCornerRadius(dp(6));
        fill.setBackground(fgd);
        View spacer = new View(this);
        barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(12), Math.max(0.0001f, pct / 100f)));
        barBg.addView(spacer, new LinearLayout.LayoutParams(0, dp(12), 1f - Math.min(1f, pct / 100f)));
        cardV.addView(barBg, lp());
        cardV.addView(gap(8));

        String hint = pct >= 90 ? "Kiváló fegyelem – a terv nálad kőbe van vésve! 🏆"
                : pct >= 60 ? "Szép ritmus – még egy kis odafigyelés, és tökéletes! 💪"
                : "A terv csak akkor él, ha követed – hajrá a héten! 🔥";
        cardV.addView(text(hint, 12.5f, MUTED, false));
        return cardV;
    }

    // ---------------- Rekordok + heti sorozat ----------------

    /** Az e heti teljesítmény összevetése az előző héttel (trend-nyilakkal). */
    LinearLayout trendCard(long weekStart, long now) {
        long lastStart = weekStart - WEEK;
        Totals cur = totals(weekStart, now + 1);
        Totals prev = totals(lastStart, weekStart);
        LinearLayout c = card();
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.addView(text("Az előző héthez képest", 12.5f, MUTED, true));
        c.addView(gap(8));
        c.addView(trendRow("🔁 Edzések", cur.count, prev.count, String.valueOf(cur.count)));
        c.addView(gap(6));
        c.addView(trendRow("📍 Táv", cur.distM, prev.distM, cur.distM > 0 ? fmtDist(cur.distM) : "0"));
        c.addView(gap(6));
        c.addView(trendRow("⏱ Idő", cur.durSec, prev.durSec, fmtDur((int) cur.durSec)));
        return c;
    }

    View trendRow(String label, double cur, double prev, String valueText) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(label, 13.5f, TXT, false), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text(valueText, 13.5f, TXT, true));
        String arrow; int color;
        double diff = cur - prev;
        if (Math.abs(diff) < 0.0001) { arrow = "  ± 0"; color = MUTED; }
        else if (diff > 0) { arrow = "  ▲"; color = 0xFF06D6A0; }
        else { arrow = "  ▼"; color = 0xFFEF476F; }
        TextView a = text(arrow, 13.5f, color, true);
        a.setPadding(dp(8), 0, 0, 0);
        row.addView(a);
        return row;
    }

    void shareStats() {
        try {
            ShareProvider.shareImage(this, renderStatsCard(), "grit-statisztika");
        } catch (Exception ignored) {}
    }

    Bitmap renderStatsCard() {
        final int W = 1080, H = 1350, M = 80;
        long now = System.currentTimeMillis();
        Totals wk = totals(weekStart(now), now + 1);
        Totals mo = totals(monthStart(now), now + 1);
        Totals all = totals(0, now + 1);
        long xp = Levels.totalXp(this); // erősítő edzésekkel együtt
        int lvl = Levels.levelForXp(xp);

        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, W, H, 0xFF140B0D, 0xFF1A0E10, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, W, H, p);
        p.setShader(null);
        Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        bar.setShader(new LinearGradient(M, 0, W - M, 0, 0xFFE11D2E, 0xFFFF4757, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(M, 120, W - M, 134), 8, 8, bar);
        bar.setShader(null);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xFFF5ECEE);
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tp.setTextSize(74);
        cv.drawText("Statisztikám", M, 240, tp);
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setColor(0xFFE11D2E);
        sp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sp.setTextSize(40);
        int shareDs = Streaks.current(this, hist);
        cv.drawText("⭐ Szint " + lvl + " · " + Levels.title(lvl)
                + (shareDs >= 2 ? "  ·  🔥 " + shareDs + " napos széria" : ""), M, 300, sp);

        String[][] rows = {
                {"Ezen a héten", wk.count + " edzés", wk.distM > 0 ? fmtDist(wk.distM) : "—", (int) wk.durSec / 60 + " perc"},
                {"Ebben a hónapban", mo.count + " edzés", mo.distM > 0 ? fmtDist(mo.distM) : "—", (int) mo.durSec / 60 + " perc"},
                {"Összesen", all.count + " edzés", all.distM > 0 ? fmtDist(all.distM) : "—", (int) all.durSec / 60 + " perc"},
        };
        int y = 380, rowH = 250;
        Paint sec = new Paint(Paint.ANTI_ALIAS_FLAG);
        sec.setColor(0xFFA98F95); sec.setTextSize(36);
        Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
        big.setColor(0xFFFFFFFF);
        big.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        big.setTextSize(44);
        Paint bgp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgp.setColor(0x14FFFFFF);
        for (String[] r : rows) {
            cv.drawRoundRect(new RectF(M, y, W - M, y + rowH - 30), 28, 28, bgp);
            cv.drawText(r[0], M + 34, y + 66, sec);
            cv.drawText(r[1] + "   ·   " + r[2] + "   ·   " + r[3], M + 34, y + 140, big);
            y += rowH;
        }

        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFFE11D2E);
        fp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        fp.setTextSize(36);
        fp.setTextAlign(Paint.Align.CENTER);
        cv.drawText("GRIT  ·  edzésnapló", W / 2f, H - 60, fp);
        return bmp;
    }

    /** A hangulat-jelölések száma [_, 1..4] indexelve (0-s index nem használt). */
    int[] moodCounts() {
        int[] c = new int[5];
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null) continue;
            int m = o.optInt("mood", 0);
            if (m >= 1 && m <= 4) c[m]++;
        }
        return c;
    }

    LinearLayout moodCard(int[] c, int total) {
        String[] emo = {"", "😣", "😐", "🙂", "💪"};
        String[] lab = {"", "Nehéz", "Rendben", "Jó", "Szuper"};
        LinearLayout card = card();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        for (int m = 4; m >= 1; m--) {
            int pct = Math.round(c[m] * 100f / total);
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            row.addView(text(emo[m] + "  " + lab[m], 14, TXT, false), new LinearLayout.LayoutParams(dp(120), -2));
            // arány-sáv
            LinearLayout barBg = hbox();
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(CARD2); bg.setCornerRadius(dp(5));
            barBg.setBackground(bg);
            View fill = new View(this);
            GradientDrawable fg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{MainActivity.ACCENT, 0xFFFF4757});
            fg.setCornerRadius(dp(5));
            fill.setBackground(fg);
            float f = Math.max(0.02f, pct / 100f);
            barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(10), f));
            barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(10), 1f - f));
            row.addView(barBg, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text("  " + pct + "%", 13, MUTED, true), new LinearLayout.LayoutParams(dp(52), -2));
            card.addView(row, lp());
        }
        return card;
    }

    LinearLayout recordsCard() {
        double bestDist = -1, bestAvg = -1, bestMax = -1, bestCal = 0;
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
            double mx = o.optDouble("maxspeed", -1);
            if (mx > bestMax) bestMax = mx;
            double cal = o.optDouble("cal", 0);
            if (cal > bestCal) bestCal = cal;
            int st = o.optInt("steps", 0);
            if (st > bestSteps) bestSteps = st;
        }
        int streak = weekStreak();
        LinearLayout grid = card();
        grid.setPadding(dp(6), dp(6), dp(6), dp(6));
        int dayNow = Streaks.current(this, History.loadAll(this));
        int dayBest = Streaks.best(this, History.loadAll(this));
        int challengesDone = getSharedPreferences("edzo", MODE_PRIVATE)
                .getInt("challenge_done_count", 0);
        addTiles(grid, new String[][]{
                {"🔥 Heti sorozat", streak + " hét"},
                {"⚡ Napi széria", dayNow + " nap"},
                {"🏅 Leghosszabb széria", dayBest + " nap"},
                {"🎯 Kihívások", challengesDone + " nap"},
                {"🗓 Terv-hetek", Streaks.planWeeks(this, History.loadAll(this)) + " hét"},
                {"🏆 Leghosszabb táv", bestDist > 0 ? fmtDist(bestDist) : "—"},
                {"⏱ Leghosszabb edzés", bestDur > 0 ? fmtDur(bestDur) : "—"},
                {"⚡ Legjobb átlag", bestAvg > 0 ? fmtSpeed(bestAvg) : "—"},
                {"🚀 Legjobb csúcs", bestMax > 0 ? fmtSpeed(bestMax) : "—"},
                {"🍩 Legtöbb kalória", bestCal > 0 ? Math.round(bestCal) + " kcal" : "—"},
                {"👟 Legtöbb lépés", bestSteps > 0 ? String.valueOf(bestSteps) : "—"},
        });
        return grid;
    }

    /** Étrend-összefoglaló az elmúlt 7 napról: naplózott napok, átlagok, cél-tartás. */
    LinearLayout dietCard() {
        long dayMs = 24L * 3600 * 1000;
        java.util.Calendar c0 = java.util.Calendar.getInstance();
        c0.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c0.set(java.util.Calendar.MINUTE, 0);
        c0.set(java.util.Calendar.SECOND, 0);
        c0.set(java.util.Calendar.MILLISECOND, 0);
        long today0 = c0.getTimeInMillis();
        double[] kcal = new double[7];
        double[] prot = new double[7];
        for (MealLog.Meal m : MealLog.load(this)) {
            java.util.Calendar cm = java.util.Calendar.getInstance();
            cm.setTimeInMillis(m.ts);
            cm.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cm.set(java.util.Calendar.MINUTE, 0);
            cm.set(java.util.Calendar.SECOND, 0);
            cm.set(java.util.Calendar.MILLISECOND, 0);
            int k = Days.between(cm.getTimeInMillis(), today0);
            if (k >= 0 && k < 7) { kcal[k] += m.kcal(); prot[k] += m.protein(); }
        }
        int days = 0, underGoal = 0;
        double kSum = 0, pSum = 0;
        int goal = getSharedPreferences("edzo", MODE_PRIVATE).getInt("kcal_goal", 0);
        for (int k = 0; k < 7; k++) {
            if (kcal[k] <= 0) continue;
            days++; kSum += kcal[k]; pSum += prot[k];
            if (goal > 0 && kcal[k] <= goal) underGoal++;
        }
        LinearLayout grid = card();
        grid.setPadding(dp(6), dp(6), dp(6), dp(6));
        java.util.List<String[]> tiles = new java.util.ArrayList<>();
        tiles.add(new String[]{"🍽 Naplózott napok", days + " / 7"});
        tiles.add(new String[]{"🔥 Átlag kalória", days > 0 ? Math.round(kSum / days) + " kcal/nap" : "—"});
        if (pSum > 0)
            tiles.add(new String[]{"🥩 Átlag fehérje", Math.round(pSum / days) + " g/nap"});
        if (goal > 0 && days > 0)
            tiles.add(new String[]{"🎯 Cél alatt", underGoal + " / " + days + " nap"});
        // Víz-átlag az elmúlt 7 napból (csak a naplózott napok számítanak).
        int wDays = 0, wSum = 0;
        for (int k = 0; k < 7; k++) {
            int cl = Water.clOn(this, today0 - k * dayMs);
            if (cl > 0) { wDays++; wSum += cl; }
        }
        if (wDays > 0)
            tiles.add(new String[]{"💧 Átlag víz",
                    Water.liters((int) Math.round(wSum / (double) wDays)) + "/nap"});
        addTiles(grid, tiles.toArray(new String[0][]));
        return grid;
    }

    /**
     * 30 napos étrend-csík: naponta egy oszlop. Kcal-cél nélkül semleges színnel
     * a naplózott napok, céllal a cél alatti napok zölden, a felettiek borostyánban.
     */
    LinearLayout dietMonthCard() {
        long dayMs = 24L * 3600 * 1000;
        Calendar c0 = Calendar.getInstance();
        c0.set(Calendar.HOUR_OF_DAY, 0);
        c0.set(Calendar.MINUTE, 0);
        c0.set(Calendar.SECOND, 0);
        c0.set(Calendar.MILLISECOND, 0);
        long today0 = c0.getTimeInMillis();
        double[] kcal = new double[30];   // [0] = 29 napja … [29] = ma
        for (MealLog.Meal m : MealLog.load(this)) {
            Calendar cm = Calendar.getInstance();
            cm.setTimeInMillis(m.ts);
            cm.set(Calendar.HOUR_OF_DAY, 0);
            cm.set(Calendar.MINUTE, 0);
            cm.set(Calendar.SECOND, 0);
            cm.set(Calendar.MILLISECOND, 0);
            int back = Days.between(cm.getTimeInMillis(), today0);
            if (back >= 0 && back < 30) kcal[29 - back] += m.kcal();
        }
        int goal = getSharedPreferences("edzo", MODE_PRIVATE).getInt("kcal_goal", 0);
        double max = 1;
        for (double v : kcal) max = Math.max(max, v);
        if (goal > 0) max = Math.max(max, goal);

        LinearLayout cardV = card();
        cardV.setPadding(dp(12), dp(14), dp(12), dp(12));
        cardV.addView(text("Elmúlt 30 nap · napi kalória", 12.5f, MUTED, true));
        LinearLayout row = hbox();
        row.setGravity(Gravity.BOTTOM);
        int logged = 0, under = 0;
        for (int i = 0; i < 30; i++) {
            LinearLayout colD = vbox();
            colD.setGravity(Gravity.BOTTOM);
            View bar = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(2));
            int h;
            if (kcal[i] <= 0) {
                bg.setColor(Theme.trackFaint(this));   // nincs adat: halvány alapcsík
                h = dp(3);
            } else {
                logged++;
                boolean over = goal > 0 && kcal[i] > goal;
                if (goal > 0 && !over) under++;
                bg.setColor(goal <= 0 ? MainActivity.ACCENT
                        : over ? 0xFFF59E0B : 0xFF22C55E);
                h = (int) Math.max(dp(4), Math.round(dp(46) * Math.min(1.0, kcal[i] / max)));
            }
            bar.setBackground(bg);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(7), h);
            blp.setMargins(dp(1), 0, dp(1), 0);
            colD.addView(bar, blp);
            row.addView(colD, new LinearLayout.LayoutParams(0, dp(50), 1f));
        }
        LinearLayout.LayoutParams rlp = lp();
        rlp.topMargin = dp(10);
        cardV.addView(row, rlp);
        String legend = logged + " naplózott nap";
        if (goal > 0 && logged > 0) legend += "  ·  " + under + " a cél alatt";
        TextView lg = text(legend, 11.5f, MUTED, false);
        lg.setGravity(Gravity.CENTER);
        lg.setPadding(0, dp(8), 0, 0);
        cardV.addView(lg);
        return cardV;
    }

    /** Hány egymást követő héten volt legalább egy edzés (az aktuális vagy múlt héttől visszafelé). */
    int weekStreak() {
        java.util.HashSet<Long> weeks = new java.util.HashSet<>();
        JSONArray src = History.loadAll(this); // erősítő napok is számítanak
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
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

    /** A valaha volt leghosszabb megszakítás nélküli heti sorozat. */
    int bestWeekStreak() {
        java.util.HashSet<Long> weeks = new java.util.HashSet<>();
        JSONArray src = History.loadAll(this); // erősítő napok is számítanak
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
            if (o != null) weeks.add(weekStart(o.optLong("ts")));
        }
        int best = 0;
        for (Long w : weeks) {
            if (weeks.contains(prevWeek(w))) continue;
            int s = 0; long cur = w;
            while (weeks.contains(cur)) { s++; cur = weekStart(cur + 10L * 24 * 3600 * 1000); }
            if (s > best) best = s;
        }
        return best;
    }

    // ---------------- Jelvények ----------------

    LinearLayout badgesCard() {
        // A közös Badges definíció alapján (ugyanaz, mint a főképernyőn), hogy a
        // két helyen mindig ugyanazok a kitüntetések és feltételek jelenjenek meg.
        // Egyesített napló (erősítő napokkal), hogy a kezdőlappal egyezzen.
        JSONArray act = History.loadAll(this);
        java.util.HashSet<String> got = Badges.earned(this, act, bestWeekStreak(), getSharedPreferences("edzo", MODE_PRIVATE).getInt("challenge_done_count", 0), Streaks.planWeeks(this, act));
        Badges.Badge[] all = Badges.ALL;

        LinearLayout grid = card();
        grid.setPadding(dp(6), dp(6), dp(6), dp(6));
        for (int i = 0; i < all.length; i += 2) {
            LinearLayout row = hbox();
            row.addView(badgeTile(all[i].emoji, all[i].title, got.contains(all[i].id)), tileLp());
            if (i + 1 < all.length)
                row.addView(badgeTile(all[i + 1].emoji, all[i + 1].title, got.contains(all[i + 1].id)), tileLp());
            else row.addView(new View(this), tileLp());
            grid.addView(row, lp());
        }
        TextView cap = text(got.size() + " / " + all.length + " jelvény megszerezve", 12, MUTED, false);
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

    // 12 hetes aktivitás-hőtérkép (GitHub-stílusú): oszlop = hét, sor = nap.
    LinearLayout heatmapCard() {
        java.util.HashSet<Long> days = new java.util.HashSet<>();
        Calendar c = Calendar.getInstance();
        JSONArray src = History.loadAll(this); // erősítő napok is aktívak
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
            if (o == null) continue;
            c.setTimeInMillis(o.optLong("ts"));
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
            days.add(c.getTimeInMillis());
        }
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 0);
        t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
        long today0 = t.getTimeInMillis();
        Calendar cur = (Calendar) t.clone();
        cur.setFirstDayOfWeek(Calendar.MONDAY);
        cur.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cur.add(Calendar.WEEK_OF_YEAR, -11);

        LinearLayout cardV = card();
        cardV.setPadding(dp(12), dp(14), dp(12), dp(12));
        LinearLayout grid = hbox();
        grid.setGravity(Gravity.CENTER);
        int activeDays = 0;
        for (int w = 0; w < 12; w++) {
            LinearLayout colW = vbox();
            for (int d = 0; d < 7; d++) {
                long ms = cur.getTimeInMillis();
                View cell = new View(this);
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(3));
                if (ms > today0) bg.setColor(0x00000000);
                else if (days.contains(ms)) { bg.setColor(MainActivity.ACCENT); activeDays++; }
                else bg.setColor(Theme.track(this));
                cell.setBackground(bg);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(15), dp(15));
                clp.setMargins(dp(2), dp(2), dp(2), dp(2));
                colW.addView(cell, clp);
                cur.add(Calendar.DAY_OF_YEAR, 1);
            }
            grid.addView(colW);
        }
        cardV.addView(grid);
        TextView legend = text(activeDays + " aktív nap az elmúlt 12 hétben · tartsd nyomva a megosztáshoz 📤", 11.5f, MUTED, false);
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(0, dp(10), 0, 0);
        cardV.addView(legend);
        cardV.setClickable(true);
        cardV.setOnLongClickListener(v -> { shareHeatmap(); return true; });
        return cardV;
    }

    void shareHeatmap() {
        try { ShareProvider.shareImage(this, renderHeatmapCard(), "grit-aktivitas"); }
        catch (Exception ignored) {}
    }

    Bitmap renderHeatmapCard() {
        final int W = 1080, H = 900, M = 70;
        // Napkészlet és időablak (ugyanaz, mint a képernyőn).
        java.util.HashSet<Long> days = new java.util.HashSet<>();
        Calendar cc = Calendar.getInstance();
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null) continue;
            cc.setTimeInMillis(o.optLong("ts"));
            cc.set(Calendar.HOUR_OF_DAY, 0); cc.set(Calendar.MINUTE, 0);
            cc.set(Calendar.SECOND, 0); cc.set(Calendar.MILLISECOND, 0);
            days.add(cc.getTimeInMillis());
        }
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 0);
        t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
        long today0 = t.getTimeInMillis();
        Calendar cur = (Calendar) t.clone();
        cur.setFirstDayOfWeek(Calendar.MONDAY);
        cur.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cur.add(Calendar.WEEK_OF_YEAR, -11);

        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cvn = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, W, H, 0xFF140B0D, 0xFF1A0E10, Shader.TileMode.CLAMP));
        cvn.drawRect(0, 0, W, H, p); p.setShader(null);
        Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        bar.setShader(new LinearGradient(M, 0, W - M, 0, 0xFFE11D2E, 0xFFFF4757, Shader.TileMode.CLAMP));
        cvn.drawRoundRect(new RectF(M, 110, W - M, 124), 8, 8, bar); bar.setShader(null);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xFFF5ECEE); tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tp.setTextSize(70);
        cvn.drawText("Aktivitásom – 12 hét", M, 225, tp);

        int cell = 60, gap = 12;
        int gridW = 12 * cell + 11 * gap;
        int x0 = (W - gridW) / 2, y0 = 300;
        int active = 0;
        Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int w = 0; w < 12; w++) {
            for (int d = 0; d < 7; d++) {
                long ms = cur.getTimeInMillis();
                int cx = x0 + w * (cell + gap), cy = y0 + d * (cell + gap);
                if (ms > today0) cp.setColor(0x11FFFFFF);
                else if (days.contains(ms)) { cp.setColor(0xFFE11D2E); active++; }
                else cp.setColor(0x1AFFFFFF);
                cvn.drawRoundRect(new RectF(cx, cy, cx + cell, cy + cell), 12, 12, cp);
                cur.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setColor(0xFFA98F95); sp.setTextSize(38); sp.setTextAlign(Paint.Align.CENTER);
        cvn.drawText(active + " aktív nap az elmúlt 12 hétben", W / 2f, y0 + 7 * (cell + gap) + 40, sp);
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFFE11D2E); fp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        fp.setTextSize(34); fp.setTextAlign(Paint.Align.CENTER);
        cvn.drawText("GRIT  ·  edzésnapló", W / 2f, H - 45, fp);
        return bmp;
    }

    LinearLayout calendarCard() {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR), month = now.get(Calendar.MONTH);
        int today = now.get(Calendar.DAY_OF_MONTH);
        java.util.HashSet<Integer> days = new java.util.HashSet<>();
        Calendar c2 = Calendar.getInstance();
        JSONArray src = History.loadAll(this); // erősítő napok is jelölve
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
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
        boolean hasPlan = !Theme.planDays(this).isEmpty();
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
                } else if (hasPlan && Theme.isPlanDay(this, c)) {
                    // Tervezett edzésnap: halvány szaggatott gyűrű.
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(0);
                    bg.setStroke(dp(1), (Theme.accent(this) & 0x00FFFFFF) | 0x55000000, dp(4), dp(3));
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
        // Jelmagyarázat, hogy első ránézésre érthető legyen a naptár.
        boolean hasPlanLegend = !Theme.planDays(this).isEmpty();
        TextView legend = text("● edzés  ·  □ ma"
                + (hasPlanLegend ? "  ·  ◌ tervezett edzésnap" : ""), 11, MUTED, false);
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(0, dp(3), 0, 0);
        cardC.addView(legend, lp());
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
        bg.setColor(MainActivity.GLASS2); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), MainActivity.GLASS_LINE);
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
        bg.setColor(MainActivity.GLASS); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), MainActivity.GLASS_LINE);
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
        private int color = 0xFFE11D2E;
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
