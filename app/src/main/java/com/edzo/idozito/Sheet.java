package com.edzo.idozito;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Egyedi, iOS-stílusú alsó lap (action sheet / bottom sheet) – ez váltja ki
 * mindenhol a sima Android AlertDialog/menüt. Lekerekített, áttetsző „üveg"
 * panel fogantyúval, kártyás sorokkal, akcentgombokkal, felúszó animációval.
 */
public final class Sheet {

    public interface OnTap { void run(); }

    private final Activity a;
    private final float d;
    private final Dialog dialog;
    private final LinearLayout panel;
    private final LinearLayout rows;
    private final LinearLayout footer;
    private final List<TextView> checks = new ArrayList<>();
    private final int accent, accent2;
    private final boolean lm;   // világos mód

    public Sheet(Activity act, String title) {
        this(act, title, null);
    }

    public Sheet(Activity act, String title, String subtitle) {
        this.a = act;
        this.d = act.getResources().getDisplayMetrics().density;
        this.accent = Theme.accent(act);
        this.accent2 = Theme.accent2(act);
        this.lm = Theme.light(act);

        dialog = new Dialog(act);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        panel = new LinearLayout(act);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(lm ? 0xFFFFFFFF : 0xF3161C2E);
        float r = dp(30);
        bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        bg.setStroke(dp(1), lm ? 0x14000000 : 0x24FFFFFF);
        panel.setBackground(bg);
        panel.setPadding(dp(12), dp(10), dp(12), dp(16));

        // Fogantyú
        View grab = new View(act);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(lm ? 0x33000000 : 0x59FFFFFF);
        gd.setCornerRadius(dp(3));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(dp(42), dp(5));
        glp.gravity = Gravity.CENTER_HORIZONTAL;
        glp.topMargin = dp(4);
        glp.bottomMargin = dp(12);
        panel.addView(grab, glp);

        if (title != null) {
            TextView t = new TextView(act);
            t.setText(title);
            t.setTextColor(lm ? 0xFF16203A : 0xFFEAF6FF);
            t.setTextSize(19);
            t.setTypeface(null, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(8), 0, dp(8), subtitle != null ? dp(2) : dp(12));
            panel.addView(t);
        }
        if (subtitle != null) {
            TextView s = new TextView(act);
            s.setText(subtitle);
            s.setTextColor(lm ? 0xFF5C6B86 : 0xFF8AA0C4);
            s.setTextSize(13);
            s.setGravity(Gravity.CENTER);
            s.setPadding(dp(8), 0, dp(8), dp(12));
            panel.addView(s);
        }

        rows = new LinearLayout(act);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(act) {
            @Override protected void onMeasure(int w, int h) {
                int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.66f);
                super.onMeasure(w, MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST));
            }
        };
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(rows);
        panel.addView(sv, new LinearLayout.LayoutParams(-1, -2));

        footer = new LinearLayout(act);
        footer.setOrientation(LinearLayout.VERTICAL);
        panel.addView(footer, new LinearLayout.LayoutParams(-1, -2));

        dialog.setContentView(panel);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0x00000000));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setDimAmount(0.6f);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    // ---- Sorok ----

    public Sheet addRow(String emoji, String title, String sub, boolean selected, boolean dismissOnTap, OnTap tap) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(11), dp(14), dp(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(lm ? 0x0A000000 : 0x14FFFFFF);
        bg.setCornerRadius(dp(17));
        bg.setStroke(dp(1), lm ? 0x14000000 : 0x1FFFFFFF);
        row.setBackground(bg);
        row.setClickable(true);

        if (emoji != null) {
            TextView e = new TextView(a);
            e.setText(emoji);
            e.setTextSize(19);
            e.setGravity(Gravity.CENTER);
            GradientDrawable ch = new GradientDrawable();
            ch.setShape(GradientDrawable.OVAL);
            ch.setColor((accent & 0xFFFFFF) | 0x33000000);
            e.setBackground(ch);
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(dp(40), dp(40));
            elp.rightMargin = dp(12);
            row.addView(e, elp);
        }

        LinearLayout mid = new LinearLayout(a);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextColor(lm ? 0xFF16203A : 0xFFEAF6FF);
        t.setTextSize(16);
        t.setTypeface(null, Typeface.BOLD);
        mid.addView(t);
        if (sub != null) {
            TextView s = new TextView(a);
            s.setText(sub);
            s.setTextColor(lm ? 0xFF5C6B86 : 0xFF8AA0C4);
            s.setTextSize(12.5f);
            mid.addView(s);
        }
        row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tr = new TextView(a);
        tr.setTextSize(17);
        setTrail(tr, selected);
        row.addView(tr);
        checks.add(tr);

        row.setOnClickListener(v -> {
            if (tap != null) tap.run();
            if (dismissOnTap) dismiss();
        });

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = dp(5);
        rlp.bottomMargin = dp(5);
        rlp.leftMargin = dp(4);
        rlp.rightMargin = dp(4);
        rows.addView(row, rlp);
        return this;
    }

    private void setTrail(TextView tr, boolean selected) {
        if (selected) {
            tr.setText("✓");
            tr.setTextColor(accent);
            tr.setTypeface(null, Typeface.BOLD);
        } else {
            tr.setText("›");
            tr.setTextColor(0x66FFFFFF);
            tr.setTypeface(null, Typeface.NORMAL);
        }
    }

    /** A sorok (addRow hívási sorrendje szerinti) kijelölés-pipáinak frissítése. */
    public void selectOnly(int index) {
        for (int i = 0; i < checks.size(); i++) setTrail(checks.get(i), i == index);
    }

    public Sheet addCustom(View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        lp.topMargin = dp(2);
        rows.addView(v, lp);
        return this;
    }

    // ---- Gombok ----

    private void addButton(String title, int bgColor, int textColor, boolean gradient, boolean dismiss, OnTap tap) {
        TextView b = new TextView(a);
        b.setText(title);
        b.setTextColor(textColor);
        b.setTextSize(16.5f);
        b.setTypeface(null, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(15), dp(16), dp(15));
        GradientDrawable bg = gradient
                ? new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{accent, accent2})
                : new GradientDrawable();
        if (!gradient) bg.setColor(bgColor);
        bg.setCornerRadius(dp(16));
        b.setBackground(bg);
        b.setClickable(true);
        b.setOnClickListener(v -> {
            if (tap != null) tap.run();
            if (dismiss) dismiss();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(9);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        footer.addView(b, lp);
    }

    public Sheet addPrimary(String title, OnTap tap) { addButton(title, 0, 0xFFFFFFFF, true, true, tap); return this; }
    public Sheet addNeutral(String title, OnTap tap) { addButton(title, lm ? 0x12000000 : 0x22FFFFFF, lm ? 0xFF16203A : 0xFFEAF6FF, false, true, tap); return this; }
    public Sheet addDestructive(String title, OnTap tap) { addButton(title, lm ? 0x1AFF453A : 0x33FF453A, 0xFFE23B3B, false, true, tap); return this; }
    public Sheet addCancel() { addButton("Mégse", lm ? 0x0A000000 : 0x14FFFFFF, lm ? 0xFF5C6B86 : 0xFF8AA0C4, false, true, null); return this; }

    // ---- Megjelenítés ----

    public void show() {
        dialog.show();
        panel.post(() -> {
            int h = panel.getHeight();
            if (h <= 0) return;
            TranslateAnimation an = new TranslateAnimation(0, 0, h, 0);
            an.setDuration(280);
            an.setInterpolator(new DecelerateInterpolator(1.4f));
            panel.startAnimation(an);
        });
    }

    public void dismiss() {
        try { dialog.dismiss(); } catch (Exception ignored) {}
    }

    int dp(float v) { return (int) (v * d + 0.5f); }
}
