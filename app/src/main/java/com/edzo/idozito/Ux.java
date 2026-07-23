package com.edzo.idozito;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Mozgás és látvány: „élő" (Ken Burns) háttér, beúszó kártyák, felpörgő számlálók
 * és egy háttérkép-váz, amit minden képernyő használhat.
 */
public final class Ux {

    private Ux() {}

    public interface Fmt { String fmt(float v); }

    static int drawableId(Activity a, String name) {
        try { return a.getResources().getIdentifier(name, "drawable", a.getPackageName()); }
        catch (Exception e) { return 0; }
    }

    /** Lassan „élő" háttér: finom, végtelenített zoom + pásztázás. Kikapcsolható a Beállításokban. */
    public static void kenBurns(final View v) {
        if (!Theme.liveBg(v.getContext())) return; // statikus háttér, ha ki van kapcsolva
        v.setScaleX(1.12f); v.setScaleY(1.12f);
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(19000);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.addUpdateListener(an -> {
            float f = (float) an.getAnimatedValue();
            v.setScaleX(1.12f + 0.10f * f);
            v.setScaleY(1.12f + 0.10f * f);
            v.setTranslationX(-26f + 52f * f);
            v.setTranslationY(22f - 44f * f);
        });
        a.start();
    }

    /** Beúszás: alulról felfelé + halványból. */
    public static void enter(View v, long delayMs) {
        float dy = 26f * v.getResources().getDisplayMetrics().density;
        v.setAlpha(0f);
        v.setTranslationY(dy);
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(delayMs).setDuration(520)
                .setInterpolator(new DecelerateInterpolator(1.3f)).start();
    }

    /** Egymás utáni (staggered) beúszás egy konténer látható gyerekeire. */
    public static void enterChildren(ViewGroup g, long startDelay, long step) {
        int shown = 0;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c.getVisibility() != View.VISIBLE || c.getHeight() == 0 && c.getWidth() == 0) {
                // láthatatlan térközök: átugorjuk, hogy ne csússzon szét az időzítés
            }
            enter(c, startDelay + (long) shown * step);
            shown++;
        }
    }

    /** Felpörgő számláló egy szöveges mezőre. */
    public static void countUp(final TextView tv, float target, final Fmt f) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, target);
        a.setDuration(950);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(an -> tv.setText(f.fmt((float) an.getAnimatedValue())));
        a.start();
    }

    /**
     * Háttérkép-váz: (ken burns) kép + sötét fátyol + a tartalom.
     * A megadott képet keresi, ha nincs, a fő háttérre esik vissza.
     * A content háttere legyen áttetsző, hogy a kép átlátsszon.
     */
    public static FrameLayout scaffold(Activity a, View content, String primaryName) {
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(MainActivity.BG);
        addBg(a, root, primaryName);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    /** Mint a scaffold, de alul egy állandó navigációs sávval (Kezdő/Statok/Erő/Profil). */
    public static FrameLayout scaffoldNav(Activity a, View content, String primaryName, int activeIndex) {
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(MainActivity.BG);
        addBg(a, root, primaryName);
        LinearLayout stack = new LinearLayout(a);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        stack.addView(bottomNav(a, activeIndex), new LinearLayout.LayoutParams(-1, -2));
        root.addView(stack, new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    /** A háttérkép + fátyol hozzáadása egy gyökér FrameLayouthoz. */
    static void addBg(Activity a, FrameLayout root, String primaryName) {
        int id = drawableId(a, primaryName);
        if (id == 0) id = drawableId(a, "bg_main");
        if (id == 0) return;
        try {
            ImageView img = new ImageView(a);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageResource(id);
            root.addView(img, new FrameLayout.LayoutParams(-1, -1));
            kenBurns(img);
            View scrim = new View(a);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x8C070912, 0xDE070912, 0xF5070912});
            scrim.setBackground(g);
            root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        } catch (Exception ignored) {}
    }

    /**
     * Állandó alsó navigációs sáv a fő képernyők közti gyors váltáshoz.
     * activeIndex: 0=Kezdő, 1=Statok, 2=Erő, 3=Profil.
     */
    public static LinearLayout bottomNav(final Activity a, final int active) {
        final float d = a.getResources().getDisplayMetrics().density;
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF20A0F1E);
        bg.setStroke((int) (1 * d), 0x22FFFFFF);
        bar.setBackground(bg);

        final String[] emo = {"🏠", "📈", "🏋️", "👤"};
        final String[] lbl = {"Kezdő", "Statok", "Erő", "Profil"};
        final Class<?>[] target = {MainActivity.class, StatsActivity.class,
                StrengthActivity.class, ProfileActivity.class};
        int accent = Theme.accent(a);

        for (int i = 0; i < emo.length; i++) {
            final int idx = i;
            boolean on = i == active;
            LinearLayout item = new LinearLayout(a);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, (int) (7 * d), 0, (int) (6 * d));
            item.setClickable(true);
            // Aktív fül: kiemelő „pill" háttér. Minden fül: azonnali megnyomás-visszajelzés.
            item.setBackground(navItemBg(accent, on, d));

            TextView e = new TextView(a);
            e.setText(emo[i]);
            e.setTextSize(on ? 21 : 18);
            e.setGravity(Gravity.CENTER);
            e.setAlpha(on ? 1f : 0.5f);
            TextView l = new TextView(a);
            l.setText(lbl[i]);
            l.setTextSize(10.5f);
            l.setGravity(Gravity.CENTER);
            l.setPadding(0, (int) (2 * d), 0, 0);
            l.setTextColor(on ? accent : 0xFF7C90B4);
            if (on) l.setTypeface(null, Typeface.BOLD);
            item.addView(e);
            item.addView(l);
            item.setOnClickListener(v -> {
                if (idx == active) return;
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                Intent it = new Intent(a, target[idx]);
                if (target[idx] == MainActivity.class)
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                a.startActivity(it);
                a.overridePendingTransition(0, 0); // azonnali váltás, nincs lassú animáció
            });
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -2, 1f);
            ip.leftMargin = (int) (5 * d);
            ip.rightMargin = (int) (5 * d);
            ip.topMargin = (int) (5 * d);
            ip.bottomMargin = (int) (5 * d);
            bar.addView(item, ip);
        }
        return bar;
    }

    /** Alsó-navigációs fül háttere: aktívnál kiemelő pill, egyébként megnyomás-visszajelzés. */
    private static android.graphics.drawable.Drawable navItemBg(int accent, boolean active, float d) {
        float r = 16 * d;
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor((accent & 0x00FFFFFF) | 0x33000000);
        pressed.setCornerRadius(r);
        if (active) {
            GradientDrawable base = new GradientDrawable();
            base.setColor((accent & 0x00FFFFFF) | 0x24000000);
            base.setCornerRadius(r);
            base.setStroke((int) (1 * d), (accent & 0x00FFFFFF) | 0x55000000);
            android.graphics.drawable.StateListDrawable sld = new android.graphics.drawable.StateListDrawable();
            sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
            sld.addState(new int[]{}, base);
            return sld;
        }
        android.graphics.drawable.StateListDrawable sld = new android.graphics.drawable.StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sld.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(0x00000000));
        return sld;
    }
}
