package com.edzo.idozito;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
        int id = drawableId(a, primaryName);
        if (id == 0) id = drawableId(a, "bg_main");
        if (id != 0) {
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
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        return root;
    }
}
