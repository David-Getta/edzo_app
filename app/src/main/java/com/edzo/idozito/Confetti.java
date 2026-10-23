package com.edzo.idozito;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Random;

/**
 * Könnyű, függőség nélküli konfetti-effekt: rövid ideig színes darabkák hullanak
 * a képernyőn, majd a nézet magától eltávolítja magát. Új rekord / szintlépés
 * ünneplésére. Nincs XML, minden kódból rajzolva.
 */
public final class Confetti extends View {

    private static final int N = 90;
    private final float[] x = new float[N], y = new float[N], vy = new float[N], vx = new float[N];
    private final float[] size = new float[N], rot = new float[N], vr = new float[N];
    private final int[] color = new int[N];
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private long start;
    private final long durationMs = 2600;
    private boolean inited;

    private Confetti(Context c, int a1, int a2) {
        super(c);
        int[] palette = {a1, a2, 0xFFFFD166, 0xFF06D6A0, 0xFFEF476F, 0xFFFFFFFF};
        for (int i = 0; i < N; i++) {
            color[i] = palette[rnd.nextInt(palette.length)];
            size[i] = dp(4) + rnd.nextFloat() * dp(6);
            rot[i] = rnd.nextFloat() * 360;
            vr[i] = (rnd.nextFloat() - 0.5f) * 18;
        }
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    private void seed(int w) {
        for (int i = 0; i < N; i++) {
            x[i] = rnd.nextFloat() * w;
            y[i] = -rnd.nextFloat() * dp(220) - dp(20);
            vy[i] = dp(4) + rnd.nextFloat() * dp(9);
            vx[i] = (rnd.nextFloat() - 0.5f) * dp(4);
        }
        inited = true;
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        if (!inited && w > 0) seed(w);
        if (start == 0) start = System.currentTimeMillis();
        long elapsed = System.currentTimeMillis() - start;
        float fade = elapsed > durationMs - 700
                ? Math.max(0f, (durationMs - elapsed) / 700f) : 1f;
        for (int i = 0; i < N; i++) {
            y[i] += vy[i];
            x[i] += vx[i];
            rot[i] += vr[i];
            p.setColor(color[i]);
            p.setAlpha((int) (255 * fade));
            cv.save();
            cv.rotate(rot[i], x[i], y[i]);
            cv.drawRect(x[i] - size[i] / 2, y[i] - size[i] / 2,
                    x[i] + size[i] / 2, y[i] + size[i] / 2, p);
            cv.restore();
        }
        if (elapsed < durationMs) {
            postInvalidateOnAnimation();
        } else {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) parent.removeView(this);
        }
    }

    /** Konfetti indítása a megadott gyökér FrameLayout tetején, a téma színeivel. */
    public static void burst(FrameLayout root) {
        if (root == null) return;
        if (!Theme.animEnabled(root.getContext())) return;
        try {
            Confetti c = new Confetti(root.getContext(),
                    Theme.accent(root.getContext()), Theme.accent2(root.getContext()));
            c.setClickable(false);
            c.setFocusable(false);
            root.addView(c, new FrameLayout.LayoutParams(-1, -1));
            c.postInvalidateOnAnimation();
        } catch (Exception ignored) {}
    }
}
