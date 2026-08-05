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

        // Az idei év madártávlatból: aktív napok, top sport, csúcsok. A heti
        // és havi számok a jelenről szólnak – ez arról, mivé áll össze az év.
        View year = yearCard(now);
        if (year != null) {
            col.addView(sectionTitle("Az idei éved · "
                    + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)));
            col.addView(year, lp());
            col.addView(gap(16));
        }

        col.addView(sectionTitle("Rekordok"));
        col.addView(recordsCard(), lp());
        col.addView(gap(16));

        // Sportágankénti bontás (30 nap): mióta kézzel is felvehető edzés, több
        // sportág él egymás mellett – itt derül ki, mire ment el az idő.
        View sports = sportsCard();
        if (sports != null) {
            col.addView(sectionTitle("Sportágak · elmúlt 30 nap"));
            col.addView(sports, lp());
            col.addView(gap(16));
        }

        View wd = weekdayCard(System.currentTimeMillis());
        if (wd != null) {
            col.addView(sectionTitle("Melyik napokon edzel"));
            col.addView(wd, lp());
            col.addView(gap(16));
        }

        View monthCmp = monthCompareCard(System.currentTimeMillis());
        if (monthCmp != null) {
            col.addView(sectionTitle("Ez a hónap"));
            col.addView(monthCmp, lp());
            col.addView(gap(16));
        }

        View bests = bestsCard();
        if (bests != null) {
            col.addView(sectionTitle("Csúcsaid"));
            col.addView(bests, lp());
            col.addView(gap(16));
        }

        View weekGoal = weeklyGoalCard(System.currentTimeMillis());
        if (weekGoal != null) {
            col.addView(sectionTitle("Heti mozgás-cél"));
            col.addView(weekGoal, lp());
            col.addView(gap(16));
        }

        View load = loadCard(System.currentTimeMillis());
        if (load != null) {
            col.addView(sectionTitle("Terhelés · e hét a szokásoshoz mérve"));
            col.addView(load, lp());
            col.addView(gap(16));
        }

        View strength = strengthCard();
        if (strength != null) {
            col.addView(sectionTitle("Súlyzós · elmúlt 30 nap"));
            col.addView(strength, lp());
            col.addView(gap(16));
        }

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
        final int W = 1080, H = 1600, M = 80;
        long now = System.currentTimeMillis();
        Totals wk = totals(weekStart(now), now + 1);
        Totals mo = totals(monthStart(now), now + 1);
        java.util.Calendar yc = java.util.Calendar.getInstance();
        yc.setTimeInMillis(now);
        yc.set(java.util.Calendar.DAY_OF_YEAR, 1);
        yc.set(java.util.Calendar.HOUR_OF_DAY, 0);
        yc.set(java.util.Calendar.MINUTE, 0);
        yc.set(java.util.Calendar.SECOND, 0);
        yc.set(java.util.Calendar.MILLISECOND, 0);
        Totals yr = totals(yc.getTimeInMillis(), now + 1);
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
                {"Idén", yr.count + " edzés", yr.distM > 0 ? fmtDist(yr.distM) : "—", (int) yr.durSec / 60 + " perc"},
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
        bgp.setColor(0x14FFFFFF);   // megosztott kép: fix design, nem téma-függő
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

    /**
     * Sportágankénti bontás az elmúlt 30 napból, vagy null, ha nincs mit
     * mutatni (egyetlen sportág önmagában nem bontás).
     */
    /**
     * Súlyzós összegzés 30 napra: a volumen (ismétlés × súly) az a szám, amiből
     * a fejlődés látszik – az alkalmak darabszáma erről semmit nem mond. A
     * legtöbbet mozgatott gyakorlat és izomcsoport is kiderül.
     */
    View strengthCard() {
        java.util.List<StrengthLog.Entry> log = StrengthLog.load(this);
        long from = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
        int lifts = 0, setCount = 0, reps = 0, rpeSum = 0, rpeCount = 0;
        double volume = 0;
        java.util.HashSet<Integer> days = new java.util.HashSet<>();
        java.util.LinkedHashMap<String, Double> byMove = new java.util.LinkedHashMap<>();
        java.util.Calendar c = java.util.Calendar.getInstance();
        for (StrengthLog.Entry e : log) {
            if (e.ts < from) continue;
            lifts++;
            setCount += e.sets.size();
            reps += e.totalReps();
            volume += e.volume();
            c.setTimeInMillis(e.ts);
            days.add(c.get(java.util.Calendar.YEAR) * 400 + c.get(java.util.Calendar.DAY_OF_YEAR));
            Double v = byMove.get(e.name);
            byMove.put(e.name, (v == null ? 0 : v) + e.volume());
            if (e.rpe > 0) { rpeSum += e.rpe; rpeCount++; }
        }
        if (lifts == 0) return null;

        LinearLayout cardV = card();
        cardV.setPadding(dp(6), dp(6), dp(6), dp(6));
        addTiles(cardV, new String[][]{
                {"📅 Edzésnapok", String.valueOf(days.size())},
                {"🏋 Gyakorlatok", String.valueOf(lifts)},
                {"🔁 Sorozatok", String.valueOf(setCount)},
                {"💪 Ismétlések", String.valueOf(reps)},
                {"⚖️ Volumen", volume >= 1000
                        ? String.format(Hu.LOCALE, "%,d", Math.round(volume)).replace(',', ' ') + " kg"
                        : Math.round(volume) + " kg"},
                {"📈 Napi átlag", Math.round(volume / Math.max(1, days.size())) + " kg"},
        });
        // Átlagos érzett terhelés: a volumen azt mondja meg, mennyit tettél
        // le, ez azt, hogy mennyibe került. A kettő együtt a teljes kép.
        if (rpeCount >= 3) {
            LinearLayout rpeRow = vbox();
            rpeRow.setPadding(dp(10), dp(2), dp(10), dp(2));
            double avg = rpeSum / (double) rpeCount;
            rpeRow.addView(text("😮‍💨 Átlagos érzett terhelés: " + Hu.d1(avg)
                    + "  (" + rpeCount + " bejegyzésből)", 13, TXT, false));
            cardV.addView(rpeRow);
        }

        LinearLayout notes = vbox();
        notes.setPadding(dp(10), dp(4), dp(10), dp(8));
        String topMove = null;
        double topVol = 0;
        for (java.util.Map.Entry<String, Double> e : byMove.entrySet())
            if (e.getValue() > topVol) { topVol = e.getValue(); topMove = e.getKey(); }
        if (topMove != null && topVol > 0)
            notes.addView(text("🥇 Legtöbb munka: " + topMove
                    + " (" + Math.round(topVol) + " kg)", 13, TXT, false));
        // A legtöbbet edzett izomcsoport – az egyensúly a Naplóban részletesen.
        java.util.LinkedHashMap<String, Integer> bal =
                Muscles.weekBalance(log, System.currentTimeMillis(), 30);
        String topG = null;
        int topD = 0;
        for (java.util.Map.Entry<String, Integer> e : bal.entrySet())
            if (e.getValue() > topD) { topD = e.getValue(); topG = e.getKey(); }
        if (topG != null && topD > 0)
            notes.addView(text("🎯 Legtöbbet edzett: " + topG + " (" + topD + " nap)",
                    13, TXT, false));
        if (notes.getChildCount() > 0) cardV.addView(notes, lp());
        return cardV;
    }

    View sportsCard() {
        JSONArray h = History.load(this);
        long from = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
        java.util.ArrayList<String> kinds = new java.util.ArrayList<>();
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> durs = new java.util.ArrayList<>();
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o == null || o.optLong("ts") < from) continue;
            kinds.add(o.optString("kind", ""));
            names.add(o.optString("name", ""));
            durs.add(o.optInt("dur"));
        }
        // A súlyzós napló napjai is sportág: kondi. Naponta egyszer, ahogy az
        // egyesített napló is számolja.
        int gymDays = 0;
        long gymSec = 0;
        for (long ts : History.oneStrengthPerDay(StrengthLog.load(this)))
            if (ts >= from) gymDays++;
        java.util.LinkedHashMap<String, long[]> rows = Activities.breakdown(
                kinds.toArray(new String[0]), names.toArray(new String[0]),
                toInts(durs));
        if (gymDays > 0) {
            String gymLabel = "🏋 Súlyzós napló";
            long[] row = rows.get(gymLabel);
            if (row == null) rows.put(gymLabel, row = new long[2]);
            row[0] += gymDays;
            row[1] += gymSec;
        }
        if (rows.size() < 2) return null;

        LinearLayout cardV = card();
        cardV.setPadding(dp(14), dp(10), dp(14), dp(10));
        long maxCount = 1;
        for (long[] r : rows.values()) maxCount = Math.max(maxCount, r[0]);
        for (java.util.Map.Entry<String, long[]> e : rows.entrySet()) {
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));
            row.addView(text(e.getKey(), 14, TXT, false),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            String amount = e.getValue()[0] + "×"
                    + (e.getValue()[1] > 0 ? "  ·  " + fmtDur((int) e.getValue()[1]) : "");
            row.addView(text(amount, 13.5f, MUTED, true),
                    new LinearLayout.LayoutParams(-2, -2));
            cardV.addView(row);
            // Arány-sáv az alkalmak számából.
            View bar = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(2));
            bg.setColor(MainActivity.ACCENT);
            bar.setBackground(bg);
            int w = (int) (dp(200) * (e.getValue()[0] / (double) maxCount));
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Math.max(dp(8), w), dp(3));
            blp.bottomMargin = dp(4);
            cardV.addView(bar, blp);
        }
        return cardV;
    }

    /**
     * Melyik napokon edzel? Tizenkét hét bontása a hét napjaira.
     *
     * A hőtérkép megmutatja, MIKOR volt edzés, de a heti mintázat nem
     * olvasható ki belőle. Ez a kártya egyetlen kérdésre válaszol: melyik nap
     * a te napod, és melyik az, amelyik rendre kimarad.
     */
    View weekdayCard(long now) {
        int[] per = new int[7];
        long from = now - 12L * 7 * 24 * 3600 * 1000;
        java.util.Calendar c = java.util.Calendar.getInstance();
        int total = 0;
        org.json.JSONArray all = History.loadAll(this);
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o == null || o.optLong("ts") < from) continue;
            c.setTimeInMillis(o.optLong("ts"));
            per[(c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7]++;
            total++;
        }
        if (total < 8) return null;      // néhány edzésből nincs mintázat

        int max = 1, bestIdx = 0;
        for (int i = 0; i < 7; i++) if (per[i] > max) { max = per[i]; bestIdx = i; }
        LinearLayout cardV = card();
        cardV.setPadding(dp(14), dp(12), dp(14), dp(12));
        for (int i = 0; i < 7; i++) {
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));
            row.addView(text(Weekplan.DAY_ABBR[i], 12.5f, i == bestIdx ? TXT : MUTED,
                    i == bestIdx), new LinearLayout.LayoutParams(dp(34), -2));
            View bar = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(3));
            bg.setColor(i == bestIdx ? Theme.accent(this) : Theme.track(this));
            bar.setBackground(bg);
            int w = (int) (dp(190) * (per[i] / (double) max));
            row.addView(bar, new LinearLayout.LayoutParams(Math.max(dp(3), w), dp(9)));
            row.addView(text("  " + per[i], 12, MUTED, false));
            cardV.addView(row);
        }
        cardV.addView(gap(6));
        cardV.addView(text(per[bestIdx] > 0
                ? Hu.dayName(bestIdx) + " a te napod – 12 hét alatt " + per[bestIdx]
                        + " edzés esett rá."
                : "", 12, MUTED, false), lp());
        return cardV;
    }

    /**
     * Ez a hónap az előzőhöz mérve. Az összegek önmagukban nem mondják meg,
     * hogy jó irányba megy-e a dolog – az előző hónap az egyetlen viszonyítás,
     * ami a szezont és a szokásokat is magában hordozza.
     *
     * A folyó hónap mindig rövidebb, mint a lezárt előző, ezért az AZONOS
     * napszámú szakaszt hasonlítjuk: ma 12-e van, tehát az előző hónap első
     * 12 napja a mérce. Enélkül minden hónap eleje csúfos visszaesésnek tűnne.
     */
    View monthCompareCard(long now) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(now);
        int dayOfMonth = c.get(java.util.Calendar.DAY_OF_MONTH);
        c.set(java.util.Calendar.DAY_OF_MONTH, 1);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        long thisStart = c.getTimeInMillis();
        c.add(java.util.Calendar.MONTH, -1);
        long prevStart = c.getTimeInMillis();
        // Az előző hónap ugyanennyiedik napjának vége.
        java.util.Calendar pc = java.util.Calendar.getInstance();
        pc.setTimeInMillis(prevStart);
        int prevDays = pc.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        pc.set(java.util.Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, prevDays));
        pc.add(java.util.Calendar.DAY_OF_YEAR, 1);
        long prevEnd = pc.getTimeInMillis();

        Totals nowT = totals(thisStart, now + 1);
        Totals prevT = totals(prevStart, prevEnd);
        if (nowT.count == 0 && prevT.count == 0) return null;

        double nowVol = 0, prevVol = 0;
        for (StrengthLog.Entry e : StrengthLog.load(this)) {
            if (e.ts >= thisStart && e.ts <= now) nowVol += e.volume();
            else if (e.ts >= prevStart && e.ts < prevEnd) prevVol += e.volume();
        }

        LinearLayout cardV = card();
        cardV.setPadding(dp(14), dp(10), dp(14), dp(10));
        cardV.addView(text("Az előző hónap első " + dayOfMonth + " napjához mérve",
                11.5f, MUTED, false), lp());
        cardV.addView(gap(6));
        String[][] rows = {
                {"🔁 Edzések", String.valueOf(nowT.count), Hu.delta(nowT.count, prevT.count)},
                {"⏱ Idő", fmtDur((int) nowT.durSec), Hu.delta(nowT.durSec, prevT.durSec)},
                {"📍 Táv", nowT.distM > 0 ? fmtDist(nowT.distM) : "—",
                        Hu.delta(nowT.distM, prevT.distM)},
                {"🔥 Kalória", Math.round(nowT.cal) + " kcal", Hu.delta(nowT.cal, prevT.cal)},
                {"🏋 Volumen", Math.round(nowVol) + " kg", Hu.delta(nowVol, prevVol)},
        };
        for (String[] r : rows) {
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));
            row.addView(text(r[0], 13.5f, TXT, false),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text(r[1], 13.5f, TXT, true), new LinearLayout.LayoutParams(-2, -2));
            TextView d = text("   " + r[2], 12.5f,
                    r[2].startsWith("+") || r[2].equals("új") ? 0xFF22C55E
                            : r[2].startsWith("−") ? 0xFFE0A050 : MUTED, true);
            row.addView(d, new LinearLayout.LayoutParams(dp(58), -2));
            cardV.addView(row);
        }
        return cardV;
    }

    /**
     * Személyes csúcsok: a leghosszabb edzés, a legnagyobb táv, a leggyorsabb
     * tempó és társaik – dátummal.
     *
     * A többi kártya átlagokat és összegeket mutat: azok arról szólnak, milyen
     * szokott lenni. A csúcs arról, hogy mire vagy képes – és pont ezért marad meg.
     */
    View bestsCard() {
        int n = hist.length();
        long[] ts = new long[n];
        int[] dur = new int[n], steps = new int[n];
        double[] dist = new double[n], cal = new double[n];
        for (int i = 0; i < n; i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null) continue;
            ts[i] = o.optLong("ts");
            dur[i] = o.optInt("dur");
            dist[i] = Math.max(0, o.optDouble("dist", 0));
            cal[i] = Math.max(0, o.optDouble("cal", 0));
            steps[i] = o.optInt("steps", 0);
        }
        java.util.List<StrengthLog.Entry> log = StrengthLog.load(this);
        long[] lts = new long[log.size()];
        double[] lvol = new double[log.size()];
        for (int i = 0; i < log.size(); i++) {
            lts[i] = log.get(i).ts;
            lvol[i] = log.get(i).volume();
        }
        java.util.List<Bests.Best> bests = Bests.of(ts, dur, dist, cal, steps, lts, lvol);
        // A napi volumen a munka mennyiségét méri; az erőt a legnehezebb
        // sorozat és a becsült 1RM – a teremben ezekre emlékszik az ember.
        int sn = 0;
        for (StrengthLog.Entry e : log) sn += e.sets.size();
        long[] sts = new long[sn];
        String[] snames = new String[sn];
        double[] sw = new double[sn];
        int[] sr = new int[sn];
        int si = 0;
        for (StrengthLog.Entry e : log)
            for (StrengthLog.SetEntry st : e.sets) {
                sts[si] = e.ts; snames[si] = e.name; sw[si] = st.weight; sr[si] = st.reps;
                si++;
            }
        bests.addAll(Bests.ofLifts(sts, snames, sw, sr));
        if (bests.isEmpty()) return null;

        SimpleDateFormat df = new SimpleDateFormat("yyyy. MMM d.", new Locale("hu"));
        LinearLayout cardV = card();
        cardV.setPadding(dp(14), dp(10), dp(14), dp(10));
        for (int i = 0; i < bests.size(); i++) {
            Bests.Best b = bests.get(i);
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            LinearLayout mid = vbox();
            mid.addView(text(b.emoji + "  " + b.label, 13.5f, TXT, false));
            if (b.ts > 0)
                mid.addView(text(df.format(new Date(b.ts)), 11.5f, MUTED, false));
            row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text(b.value, 15, Theme.accent(this), true),
                    new LinearLayout.LayoutParams(-2, -2));
            cardV.addView(row);
            if (i < bests.size() - 1) {
                View dv = new View(this);
                dv.setBackgroundColor(LINE);
                cardV.addView(dv, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        return cardV;
    }

    /**
     * Heti mozgás-cél: az egészségügyi ajánlás heti 150 perc, és ez az egyetlen
     * szám, amit évtizedek óta ugyanígy mondanak. A kártya a hét állását
     * mutatja, és azt, hogy a hiányzó percek mit jelentenek a gyakorlatban.
     */
    View weeklyGoalCard(long now) {
        int goal = getSharedPreferences("edzo", MODE_PRIVATE)
                .getInt("move_goal_min", Load.DEFAULT_WEEKLY_GOAL);
        Load.Weekly w = Load.weekly(
                History.dailyMinutes(this, now, Load.ACUTE_DAYS + Load.CHRONIC_DAYS), goal);
        if (w.minutes <= 0) return null;

        LinearLayout cardV = card();
        cardV.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text((w.done ? "✅  " : "🎽  ") + w.label(), 20,
                w.done ? 0xFF22C55E : Theme.accent(this), true),
                new LinearLayout.LayoutParams(-2, -2));
        cardV.addView(row, lp());
        cardV.addView(gap(8));

        LinearLayout barBg = hbox();
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(Theme.track(this));
        bgd.setCornerRadius(dp(6));
        barBg.setBackground(bgd);
        View fill = new View(this);
        GradientDrawable fgd = new GradientDrawable();
        fgd.setColor(w.done ? 0xFF22C55E : Theme.accent(this));
        fgd.setCornerRadius(dp(6));
        fill.setBackground(fgd);
        float f = (float) Math.max(0.02, w.percent / 100.0);
        barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(10), f));
        barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(10), 1f - f));
        cardV.addView(barBg, lp());
        cardV.addView(gap(8));
        cardV.addView(text(w.note(), 12.5f, MUTED, false), lp());
        cardV.addView(gap(4));
        cardV.addView(text("Koppints ide a heti cél átállításához", 11.5f, MUTED, false), lp());
        cardV.setClickable(true);
        cardV.setOnClickListener(v -> moveGoalDialog(w.goal));
        return cardV;
    }

    /** A heti mozgás-cél átállítása. */
    void moveGoalDialog(int current) {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Heti perc (pl. 150)");
        et.setHintTextColor(MUTED);
        et.setTextColor(TXT);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(current));
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        box.addView(et, lp());
        new Sheet(this, "Heti mozgás-cél 🎽",
                "Az egészségügyi ajánlás felnőttnek heti 150 perc közepes "
                        + "intenzitású mozgás. Ha ez most sok, állítsd lejjebb: a "
                        + "teljesíthető cél többet ér, mint a szép szám.")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    int min;
                    try {
                        min = Integer.parseInt(et.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        return;
                    }
                    getSharedPreferences("edzo", MODE_PRIVATE).edit()
                            .putInt("move_goal_min", Math.max(20, Math.min(2000, min))).apply();
                    recreate();
                })
                .addNeutral("⚡ Ajánlott: " + Load.DEFAULT_WEEKLY_GOAL + " perc", () -> {
                    getSharedPreferences("edzo", MODE_PRIVATE).edit()
                            .putInt("move_goal_min", Load.DEFAULT_WEEKLY_GOAL).apply();
                    recreate();
                })
                .addCancel().show();
    }

    /**
     * Terhelés-ugrás: az elmúlt hét az azt megelőző négy hét heti átlagához
     * mérve. A többi kártya azt mutatja, MENNYIT edzett – ez azt, hogy a
     * mennyiség hogyan VÁLTOZOTT, mert a sérülések többsége nem a sok
     * edzésből, hanem a hirtelen többől jön.
     */
    View loadCard(long now) {
        Load.Ratio r = Load.of(
                History.dailyMinutes(this, now, Load.ACUTE_DAYS + Load.CHRONIC_DAYS));
        if (!r.known) return null;

        LinearLayout cardV = card();
        cardV.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(r.emoji() + "  " + r.label(), 20,
                r.level == Load.JUMP ? 0xFFE0A050 : Theme.accent(this), true),
                new LinearLayout.LayoutParams(-2, -2));
        cardV.addView(row, lp());
        cardV.addView(gap(6));
        cardV.addView(text("Ezen a héten " + Math.round(r.acute) + " perc · a szokásos heti "
                + Math.round(r.chronic) + " perc", 13.5f, MUTED, false), lp());
        cardV.addView(gap(8));
        cardV.addView(text(r.advice(), 13f, MUTED, false), lp());
        return cardV;
    }

    /**
     * „Az idei éved": az év madártávlatból. A heti/havi kártyák a jelenről
     * szólnak – ez arról, mivé áll össze az év: aktív napok, heti átlag,
     * a leghosszabb edzés, a legaktívabb hónap és az év sportja.
     */
    View yearCard(long now) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(java.util.Calendar.DAY_OF_YEAR, 1);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        long yearStart = c.getTimeInMillis();

        Totals t = totals(yearStart, now + 1);
        if (t.count == 0) return null;

        // Aktív napok, havi eloszlás, leghosszabb edzés – egy menetben.
        java.util.HashSet<Integer> activeDays = new java.util.HashSet<>();
        int[] perMonth = new int[12];
        JSONObject longest = null;
        java.util.ArrayList<String> kinds = new java.util.ArrayList<>();
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> durs = new java.util.ArrayList<>();
        java.util.Calendar cc = java.util.Calendar.getInstance();
        for (int i = 0; i < hist.length(); i++) {
            JSONObject o = hist.optJSONObject(i);
            if (o == null || o.optLong("ts") < yearStart) continue;
            cc.setTimeInMillis(o.optLong("ts"));
            activeDays.add(cc.get(java.util.Calendar.DAY_OF_YEAR));
            perMonth[cc.get(java.util.Calendar.MONTH)]++;
            if (longest == null || o.optInt("dur") > longest.optInt("dur")) longest = o;
            kinds.add(o.optString("kind", ""));
            names.add(o.optString("name", ""));
            durs.add(o.optInt("dur"));
        }
        // A súlyzós napok is aktív napok.
        for (long ts : History.oneStrengthPerDay(StrengthLog.load(this)))
            if (ts >= yearStart) {
                cc.setTimeInMillis(ts);
                activeDays.add(cc.get(java.util.Calendar.DAY_OF_YEAR));
            }

        int weeks = Math.max(1, (int) ((now - yearStart) / (7L * 24 * 3600 * 1000)) + 1);
        LinearLayout cardV = card();
        cardV.setPadding(dp(6), dp(6), dp(6), dp(6));
        addTiles(cardV, new String[][]{
                {"🔁 Edzések", String.valueOf(t.count)},
                {"📅 Aktív napok", String.valueOf(activeDays.size())},
                {"⏱ Össz idő", fmtDur((int) t.durSec)},
                {"📍 Táv", t.distM > 0 ? fmtDist(t.distM) : "—"},
                {"👟 Lépések", t.steps > 0 ? String.valueOf((long) t.steps) : "—"},
                {"📈 Heti átlag", String.format(Hu.LOCALE, "%.1f edzés",
                        t.count / (double) weeks)},
        });

        // Szöveges csúcsok a csempék alatt.
        LinearLayout notes = vbox();
        notes.setPadding(dp(10), dp(4), dp(10), dp(8));
        java.util.LinkedHashMap<String, long[]> rows = Activities.breakdown(
                kinds.toArray(new String[0]), names.toArray(new String[0]), toInts(durs));
        if (!rows.isEmpty()) {
            java.util.Map.Entry<String, long[]> top = rows.entrySet().iterator().next();
            notes.addView(text("🏅 Az év sportja: " + top.getKey()
                    + " (" + top.getValue()[0] + " alkalom)", 13, TXT, false));
        }
        if (longest != null && longest.optInt("dur") >= 60) {
            String day = new java.text.SimpleDateFormat("MMMM d.", Hu.LOCALE)
                    .format(new java.util.Date(longest.optLong("ts")));
            notes.addView(text("🏆 Leghosszabb edzés: "
                    + (longest.optInt("dur") / 60) + " perc · " + day, 13, TXT, false));
        }
        if (t.cal >= 500)
            notes.addView(text("⚡ Elégetett kalória: "
                    + String.format(Hu.LOCALE, "%,d", Math.round(t.cal))
                            .replace(',', ' ') + " kcal", 13, TXT, false));
        int bestM = 0;
        for (int m = 1; m < 12; m++) if (perMonth[m] > perMonth[bestM]) bestM = m;
        if (perMonth[bestM] >= 3) {
            cc.set(java.util.Calendar.MONTH, bestM);
            String mn = new java.text.SimpleDateFormat("MMMM", Hu.LOCALE)
                    .format(cc.getTime());
            notes.addView(text("🔥 Legaktívabb hónap: " + mn
                    + " (" + perMonth[bestM] + " edzés)", 13, TXT, false));
        }
        if (notes.getChildCount() > 0) cardV.addView(notes, lp());
        return cardV;
    }

    int[] toInts(java.util.List<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i);
        return out;
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
            if (avg < 0 && TimerService.isRun(d) && dur > 0) avg = d / dur * 3.6;
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
        double[] partK = new double[3];   // reggel / napközben / este
        for (MealLog.Meal m : MealLog.load(this)) {
            java.util.Calendar cm = java.util.Calendar.getInstance();
            cm.setTimeInMillis(m.ts);
            int hh = cm.get(java.util.Calendar.HOUR_OF_DAY);
            cm.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cm.set(java.util.Calendar.MINUTE, 0);
            cm.set(java.util.Calendar.SECOND, 0);
            cm.set(java.util.Calendar.MILLISECOND, 0);
            int k = Days.between(cm.getTimeInMillis(), today0);
            if (k >= 0 && k < 7) {
                kcal[k] += m.kcal();
                prot[k] += m.protein();
                partK[hh < 11 ? 0 : hh < 17 ? 1 : 2] += m.kcal();
            }
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
        // Étkezési ablak átlaga: az első és az utolsó étkezés közti idő
        // naponta – aki időszakos böjtöt tart, ezt a számot figyeli.
        java.util.HashMap<Integer, long[]> firstLast = new java.util.HashMap<>();
        java.util.Calendar mc = java.util.Calendar.getInstance();
        for (MealLog.Meal m : MealLog.load(this)) {
            mc.setTimeInMillis(m.ts);
            mc.set(java.util.Calendar.HOUR_OF_DAY, 0);
            mc.set(java.util.Calendar.MINUTE, 0);
            mc.set(java.util.Calendar.SECOND, 0);
            mc.set(java.util.Calendar.MILLISECOND, 0);
            int k = Days.between(mc.getTimeInMillis(), today0);
            if (k < 0 || k >= 7) continue;
            long[] fl = firstLast.get(k);
            if (fl == null) firstLast.put(k, new long[]{m.ts, m.ts});
            else { fl[0] = Math.min(fl[0], m.ts); fl[1] = Math.max(fl[1], m.ts); }
        }
        long winSum = 0;
        int winDays = 0;
        for (long[] fl : firstLast.values())
            if (fl[1] > fl[0] + 60000) { winSum += fl[1] - fl[0]; winDays++; }
        if (winDays > 0) {
            int mins = (int) (winSum / winDays / 60000);
            tiles.add(new String[]{"🕗 Étkezési ablak", (mins / 60) + " ó " + (mins % 60) + " p"});
        }
        addTiles(grid, tiles.toArray(new String[0][]));
        // Napszak-jellemző: ha a kalóriák nagy része egy napszakra esik,
        // azt érdemes tudni – az esti evés a leggyakoribb buktató.
        double totK = partK[0] + partK[1] + partK[2];
        if (totK > 0) {
            int top = partK[1] > partK[0] ? 1 : 0;
            if (partK[2] > partK[top]) top = 2;
            double share = partK[top] / totK;
            if (share >= 0.45) {
                String[] names = {"🌅 Reggel", "🌞 Napközben", "🌙 Este"};
                TextView tv = text(names[top] + " eszed a kalóriáid "
                        + Math.round(share * 100) + "%-át", 12.5f, MUTED, false);
                tv.setPadding(dp(10), dp(2), dp(10), dp(8));
                grid.addView(tv, lp());
            }
        }
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

        // Melyik nap a legnehezebb? A havi átlag elrejti a hétvégét: ugyanaz a
        // 2200-as átlag lehet öt fegyelmezett nap két kilengéssel, és lehet hét
        // egyforma nap. A kettő nem ugyanaz a feladat.
        double[] perDay = new double[7];
        int[] daysWith = new int[7];
        Calendar cd = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            if (kcal[i] <= 0) continue;
            cd.setTimeInMillis(today0 - (29 - i) * dayMs);
            int idx = (cd.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            perDay[idx] += kcal[i];
            daysWith[idx]++;
        }
        int bestIdx = -1;
        double bestAvg = 0, allSum = 0;
        int allDays = 0;
        for (int i = 0; i < 7; i++) {
            if (daysWith[i] == 0) continue;
            allSum += perDay[i];
            allDays += daysWith[i];
            double avg = perDay[i] / daysWith[i];
            if (avg > bestAvg) { bestAvg = avg; bestIdx = i; }
        }
        // Csak akkor mondunk ilyet, ha van elég nap, és tényleg kilóg a nap.
        if (allDays >= 10 && bestIdx >= 0) {
            double allAvg = allSum / allDays;
            if (allAvg > 0 && bestAvg > allAvg * 1.15) {
                TextView hv = text("📈 " + Hu.dayName(bestIdx) + " a legnehezebb nap: átlag "
                        + Math.round(bestAvg) + " kcal, a többi napon "
                        + Math.round(allAvg) + ".", 12, MUTED, false);
                hv.setPadding(0, dp(8), 0, 0);
                cardV.addView(hv);
            }
        }
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
        return String.format(Hu.LOCALE, "%.1f km/h", kmh);
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
        return String.format(Hu.LOCALE, "%.1f km", m / 1000.0);
    }
    String fmtDur(int sec) {
        if (sec < 0) sec = 0;
        int h = sec / 3600, m = (sec % 3600) / 60;
        if (h > 0) return h + " ó " + m + " p";
        return m + " p " + (sec % 60) + " mp";
    }
    String fmtNum(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return String.valueOf(Math.round(v));
        return String.format(Hu.LOCALE, "%.1f", v);
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
            return String.format(Hu.LOCALE, "%.1f", v);
        }
    }
}
