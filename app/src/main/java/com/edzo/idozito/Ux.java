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

    /**
     * Blaze-stílusú felugró kártya (a rendszer-Toast helyett): felülről beúszó,
     * bordó, Blaze képével díszített üzenet. ~4,5 mp után magától eltűnik,
     * koppintásra azonnal bezárható. Bármely képernyőről hívható.
     */
    /** A kártyák azonosítója, hogy egyszerre mindig csak egy legyen kint. */
    private static final String CARD_TAG = "blaze_card";

    /** Kint van-e épp Blaze-kártya. */
    public static boolean cardShowing(Activity a) {
        FrameLayout root = a == null ? null : (FrameLayout) a.findViewById(android.R.id.content);
        if (root == null) return false;
        for (int i = 0; i < root.getChildCount(); i++)
            if (CARD_TAG.equals(root.getChildAt(i).getTag())) return true;
        return false;
    }

    /**
     * Csak akkor mutatja a kártyát, ha nincs kint másik. Kevésbé fontos
     * üzenetekhez (pl. belépő köszöntés), hogy ne nyomja el az ünneplést.
     */
    public static void blazeCardIfFree(Activity a, String msg) {
        // A belépő köszöntésnél Blaze él is: integet és kacsint. A többi
        // kártya (mentve, rekord) gyakori – ott a mozgás hamar zajjá válna.
        if (!cardShowing(a)) blazeCard(a, msg, true);
    }

    public static void blazeCard(final Activity a, String msg) {
        blazeCard(a, msg, false);
    }

    public static void blazeCard(final Activity a, String msg, boolean greet) {
        final FrameLayout root = a.findViewById(android.R.id.content);
        if (root == null || a.isFinishing()) return;
        final float d = a.getResources().getDisplayMetrics().density;
        int accent = Theme.accent(a);

        // Ha még kint van egy korábbi kártya, azt levesszük: mindkettő ugyanoda
        // kerülne, és egymásra csúszva olvashatatlanná válnának (pl. a belépő
        // köszöntés a kihívás-ünneplés tetejére).
        for (int i = root.getChildCount() - 1; i >= 0; i--) {
            View old = root.getChildAt(i);
            if (CARD_TAG.equals(old.getTag())) {
                old.animate().cancel();
                root.removeView(old);
            }
        }

        final LinearLayout g = new LinearLayout(a);
        g.setTag(CARD_TAG);
        g.setOrientation(LinearLayout.HORIZONTAL);
        g.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF71E1013, 0xF7140B0D});
        bg.setCornerRadius(20 * d);
        bg.setStroke((int) d, (accent & 0x00FFFFFF) | 0x77000000);
        g.setBackground(bg);
        g.setElevation(12 * d);
        g.setPadding((int) (14 * d), (int) (12 * d), (int) (16 * d), (int) (12 * d));

        // Blaze a saját képével (vagy emojival), egy keretben: a keretbe fér
        // be a kacsintás-jel is, anélkül hogy a kártya elrendezését tolná.
        final FrameLayout avatarWrap = new FrameLayout(a);
        final View avatar;
        int bid = drawableId(a, "blaze");
        if (bid != 0) {
            ImageView iv = new ImageView(a);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageResource(bid);
            iv.setClipToOutline(true);
            iv.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v, android.graphics.Outline o) {
                    o.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            avatar = iv;
            avatarWrap.addView(iv, new FrameLayout.LayoutParams((int) (46 * d), (int) (46 * d)));
        } else {
            TextView e = new TextView(a);
            e.setText("🐺");
            e.setTextSize(26);
            e.setGravity(Gravity.CENTER);
            avatar = e;
            avatarWrap.addView(e, new FrameLayout.LayoutParams((int) (46 * d), (int) (46 * d)));
        }
        final TextView wink = new TextView(a);
        wink.setText("😉");
        wink.setTextSize(15);
        wink.setAlpha(0f);
        FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(-2, -2);
        wlp.gravity = Gravity.TOP | Gravity.END;
        avatarWrap.addView(wink, wlp);
        LinearLayout.LayoutParams awlp =
                new LinearLayout.LayoutParams((int) (46 * d), (int) (46 * d));
        awlp.rightMargin = (int) (12 * d);
        g.addView(avatarWrap, awlp);
        TextView t = new TextView(a);
        t.setText(msg);
        t.setTextSize(13.5f);
        t.setTextColor(0xFFF5ECEE);
        g.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));

        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(-1, -2);
        glp.gravity = Gravity.TOP;
        glp.topMargin = (int) (46 * d);
        glp.leftMargin = (int) (14 * d);
        glp.rightMargin = (int) (14 * d);
        root.addView(g, glp);

        g.setTranslationY(-90 * d);
        g.setAlpha(0f);
        g.animate().translationY(0).alpha(1f).setDuration(430)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        final Runnable dismiss = () -> {
            if (g.getParent() == null) return;
            g.animate().translationY(-90 * d).alpha(0f).setDuration(320)
                    .withEndAction(() -> { if (g.getParent() != null) root.removeView(g); })
                    .start();
        };
        g.postDelayed(dismiss, 4500);
        g.setOnClickListener(v -> dismiss.run());
        if (greet) avatar.post(() -> greetAnim(avatar, wink));
    }

    /**
     * Blaze köszönése: beugrik, integet, majd kacsint.
     *
     * A kép egyetlen állókép, tehát a mozgás magából a nézetből jön: a
     * pattanás a testsúlyt adja, a talp körüli billegés az integetést, a
     * kacsintást pedig egy rövid összehúzódás és a felvillanó jel együtt.
     * Egymásba fűzött animációk, mert az AnimatorSet ehhez túl merev lenne.
     */
    private static void greetAnim(final View avatar, final View wink) {
        if (!Theme.animEnabled(avatar.getContext())) return;
        if (avatar.getWidth() == 0 || avatar.getHeight() == 0) return;
        // A billegés a talp körül forog: a fej körüli forgás úgy nézne ki,
        // mintha Blaze elesne.
        avatar.setPivotX(avatar.getWidth() / 2f);
        avatar.setPivotY(avatar.getHeight());
        avatar.setScaleX(0.55f);
        avatar.setScaleY(0.55f);
        avatar.setRotation(0f);
        avatar.animate().scaleX(1.14f).scaleY(1.14f).setDuration(210)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> avatar.animate().scaleX(1f).scaleY(1f).setDuration(150)
                        .withEndAction(() -> wave(avatar, wink, 0)).start())
                .start();
    }

    /** Az integetés lépései: jobbra-balra billegés, egyre kisebb kilengéssel. */
    private static void wave(final View avatar, final View wink, final int step) {
        final float[] angles = {-17f, 15f, -11f, 7f, 0f};
        if (step >= angles.length) {
            winkAnim(avatar, wink);
            return;
        }
        avatar.animate().rotation(angles[step]).setDuration(step == 0 ? 150 : 125)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> wave(avatar, wink, step + 1))
                .start();
    }

    /** A kacsintás: egy rövid összehúzódás, és felvillan a jel. */
    private static void winkAnim(final View avatar, final View wink) {
        if (wink == null) { breathe(avatar, 0); return; }
        avatar.animate().scaleY(0.9f).scaleX(1.05f).setDuration(110)
                .withEndAction(() -> avatar.animate().scaleY(1f).scaleX(1f).setDuration(160)
                        .withEndAction(() -> breathe(avatar, 0)).start())
                .start();
        wink.setScaleX(0.4f);
        wink.setScaleY(0.4f);
        wink.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .withEndAction(() -> wink.animate().alpha(0f).setStartDelay(620)
                        .setDuration(260).start())
                .start();
    }

    /**
     * Halk „lélegzés" a köszönés után: Blaze nem fagy vissza állóképpé.
     *
     * Szándékosan véges (néhány lélegzet), nem végtelen ciklus: a kártya pár
     * másodperc múlva eltűnik, és egy futó animátor egy levett nézeten csak
     * a memóriát tartaná életben.
     */
    private static void breathe(final View avatar, final int step) {
        if (step >= 3 || avatar.getParent() == null) return;
        avatar.animate().scaleX(1.045f).scaleY(1.045f).setDuration(620)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> avatar.animate().scaleX(1f).scaleY(1f).setDuration(620)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> breathe(avatar, step + 1)).start())
                .start();
    }

    /**
     * Blaze életre keltése bárhol: beugrik, integet, kacsint.
     *
     * @param avatar a kép (vagy emoji) nézete
     * @param wink   a kacsintás-jel; lehet null, ilyenkor csak az integetés megy
     */
    public static void blazeGreet(final View avatar, final View wink) {
        if (avatar == null) return;
        avatar.post(() -> greetAnim(avatar, wink));
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
        // A Beállítások „díszítő animációk" kapcsolója ezt is némítsa: aki
        // kikapcsolja, annak a képernyők beúszása a legfeltűnőbb mozgás.
        // Itt még nem tettük átlátszóvá a nézetet, tehát egyszerűen kilépünk.
        if (!Theme.animEnabled(v.getContext())) return;
        float dy = 26f * v.getResources().getDisplayMetrics().density;
        v.setAlpha(0f);
        v.setTranslationY(dy);
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(delayMs).setDuration(520)
                .setInterpolator(new DecelerateInterpolator(1.3f)).start();
    }

    /** Egymás utáni (staggered) beúszás egy konténer LÁTHATÓ gyerekeire. */
    public static void enterChildren(ViewGroup g, long startDelay, long step) {
        if (!Theme.animEnabled(g.getContext())) return;
        int shown = 0;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            // A rejtett elemeket és a nulla méretű térközöket tényleg ki kell
            // hagyni: enélkül ők is „lépnek" egyet az időzítésben, és a látható
            // kártyák beúszása egyre később indul, mint kellene.
            if (c.getVisibility() != View.VISIBLE
                    || (c.getHeight() == 0 && c.getWidth() == 0)) continue;
            enter(c, startDelay + (long) shown * step);
            shown++;
        }
    }

    /** Felpörgő számláló egy szöveges mezőre. */
    public static void countUp(final TextView tv, float target, final Fmt f) {
        if (!Theme.animEnabled(tv.getContext())) {
            tv.setText(f.fmt(target));    // animáció nélkül rögtön a végérték
            return;
        }
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
        if (Theme.light(a)) {
            // Világos módban tiszta világos alap (nincs sötét fotó-háttér).
            View g = new View(a);
            g.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFFEDF1F8, 0xFFF3F5FA}));
            root.addView(g, new FrameLayout.LayoutParams(-1, -1));
            return;
        }
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
            // Bordó-fekete fátyol a Grit-palettához (a régi éjkék helyett) – így a
            // háttérfotók is a karmazsin témához igazodnak.
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xA6120A0C, 0xE60F0A0B, 0xF80C0A0B});
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
        boolean lm = Theme.light(a);
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(lm ? 0xFFFFFFFF : 0xF2151012);
        bg.setStroke((int) (1 * d), lm ? 0x14000000 : 0x22FFFFFF);
        bar.setBackground(bg);

        // Saját rajzolású vektor-ikonok az emojik helyett (futásidőben színezve).
        final int[] icons = {R.drawable.ic_nav_home, R.drawable.ic_nav_stats,
                R.drawable.ic_nav_strength, R.drawable.ic_nav_diet,
                R.drawable.ic_nav_profile, R.drawable.ic_nav_settings};
        final String[] lbl = {"Kezdő", "Statok", "Erő", "Étrend", "Profil", "Beáll."};
        final Class<?>[] target = {MainActivity.class, StatsActivity.class,
                StrengthActivity.class, DietActivity.class,
                ProfileActivity.class, SettingsActivity.class};
        int accent = Theme.accent(a);

        for (int i = 0; i < icons.length; i++) {
            final int idx = i;
            boolean on = i == active;
            LinearLayout item = new LinearLayout(a);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, (int) (7 * d), 0, (int) (6 * d));
            item.setClickable(true);
            // Aktív fül: kiemelő „pill" háttér. Minden fül: azonnali megnyomás-visszajelzés.
            item.setBackground(navItemBg(accent, on, d));

            int inactive = lm ? 0xFF8A7176 : 0xFFA08A90;
            ImageView e = new ImageView(a);
            e.setImageResource(icons[i]);
            e.setColorFilter(on ? accent : inactive);
            e.setAlpha(on ? 1f : 0.75f);
            int isz = (int) ((on ? 23 : 21) * d);
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(isz, isz);
            elp.gravity = Gravity.CENTER_HORIZONTAL;
            TextView l = new TextView(a);
            l.setText(lbl[i]);
            l.setTextSize(10.5f);
            l.setGravity(Gravity.CENTER);
            l.setPadding(0, (int) (3 * d), 0, 0);
            l.setTextColor(on ? accent : inactive);
            if (on) l.setTypeface(null, Typeface.BOLD);
            item.addView(e, elp);
            item.addView(l);
            item.setOnClickListener(v -> {
                if (idx == active) return;
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                Intent it = new Intent(a, target[idx]);
                if (target[idx] == MainActivity.class)
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                else
                    // Ha a fül már a veremben van, előrehozzuk (nem hozunk létre duplikátumot).
                    it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
