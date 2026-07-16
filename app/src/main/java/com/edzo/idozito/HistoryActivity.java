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

    static final int BG = MainActivity.BG, TXT = MainActivity.TXT, MUTED = MainActivity.MUTED,
            LINE = MainActivity.LINE, GLASS = MainActivity.GLASS, GLASS_LINE = MainActivity.GLASS_LINE;
    static final int RUN_C = 0xFF22E0FF, GYM_C = 0xFFFF3DDB; // futás = cián / erő = magenta

    int accent, accent2;
    boolean pace;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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

        JSONArray arr = History.load(this);

        // Fejléc
        LinearLayout head = hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout htexts = vbox();
        htexts.addView(text("Előzmények", 27, TXT, true));
        htexts.addView(text(arr.length() + " elmentett edzés", 13.5f, MUTED, false));
        head.addView(htexts, new LinearLayout.LayoutParams(0, -2, 1f));
        if (arr.length() > 0) head.addView(trashButton());
        col.addView(head, lp());
        col.addView(gap(18));

        if (arr.length() == 0) {
            col.addView(emptyState());
        } else {
            col.addView(heroSummary(arr), lp());
            col.addView(gap(18));
            SimpleDateFormat df = new SimpleDateFormat("yyyy. MMM d. · HH:mm", new Locale("hu"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                col.addView(entryCard(o, df), lp());
                col.addView(gap(12));
            }
        }

        sv.addView(col, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv);
        setContentView(root);
        col.post(() -> Ux.enterChildren(col, 30, 55)); // beúszó kártyák
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
        int id = getResources().getIdentifier("bg_main", "drawable", getPackageName());
        if (id == 0) return;
        try {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageResource(id);
            host.addView(img, new FrameLayout.LayoutParams(-1, -1));
            Ux.kenBurns(img);
            View scrim = new View(this);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x99070912, 0xE0070912, 0xF5070912});
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
