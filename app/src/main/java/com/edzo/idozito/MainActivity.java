package com.edzo.idozito;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Edző Időzítő – egyszerű intervallum (HIIT) időzítő.
 * Beállítható előkészület / futás / pihenő idő és körök száma.
 * Minden szakaszváltásnál sípol és rezeg, az utolsó 3 másodpercben visszaszámol.
 */
public class MainActivity extends Activity {

    // --- Színek ---
    static final int BG = 0xFF0B1020, CARD = 0xFF1A2238, CARD2 = 0xFF212B47;
    static final int TXT = 0xFFF2F5FF, MUTED = 0xFF93A0C4, LINE = 0xFF2A3552;
    static final int INDIGO = 0xFF6366F1, VIOLET = 0xFF8B5CF6;
    static final int WORK = 0xFF22C55E, REST = 0xFF38BDF8, PREP = 0xFFF59E0B, DONE = 0xFFA78BFA;

    // --- Beállítás kulcsok ---
    static final int PREP_K = 0, WORK_K = 1, REST_K = 2, ROUND_K = 3;
    final int[] cfg = new int[4]; // előkészület, futás, pihenő, körök (mp / db)
    final int[] DEF = {10, 10, 30, 8};
    SharedPreferences prefs;

    // --- Szakasztípusok ---
    static final int T_PREP = 0, T_WORK = 1, T_REST = 2;

    static class Step {
        int type, dur, round;
        Step(int t, int d, int r) { type = t; dur = d; round = r; }
    }

    // --- UI hivatkozások ---
    FrameLayout root;
    ScrollView setupScroll;
    LinearLayout runView;
    TextView totalText;
    final TextView[] valueLabels = new TextView[4];
    TextView phaseLabel, timeText, roundInfo, nextInfo;
    Button pauseBtn;
    ProgressRing ring;

    // --- Időzítő állapot ---
    ArrayList<Step> plan = new ArrayList<>();
    int idx;
    boolean running, paused;
    long stepEndElapsed;
    long remainingAtPause;
    int lastShownSec;
    final Handler ui = new Handler(Looper.getMainLooper());
    Runnable ticker;

    // --- Léptető ismétlés (nyomva tartás) ---
    final Handler repeatH = new Handler(Looper.getMainLooper());
    Runnable repeatR;

    Vibrator vibrator;

    // ---------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("edzo", MODE_PRIVATE);
        for (int i = 0; i < 4; i++) cfg[i] = prefs.getInt("k" + i, DEF[i]);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.addView(buildSetup());
        root.addView(buildRun());
        setContentView(root);

        showRun(false);
        refreshValues();
        updateTotal();

        ticker = new Runnable() {
            @Override public void run() { tick(); }
        };
    }

    // ================= SETUP KÉPERNYŐ =================

    View buildSetup() {
        setupScroll = new ScrollView(this);
        setupScroll.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        setupScroll.setFillViewport(true);

        LinearLayout col = vbox();
        col.setPadding(dp(18), dp(18), dp(18), dp(28));

        // Fejléc
        LinearLayout head = hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        View badge = new View(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{INDIGO, VIOLET});
        bg.setCornerRadius(dp(9));
        badge.setBackground(bg);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(34), dp(34));
        bp.rightMargin = dp(10);
        head.addView(badge, bp);
        LinearLayout titles = vbox();
        titles.addView(text("Edző Időzítő", 20, TXT, true));
        titles.addView(text("Intervallum edzés sípszóval", 12.5f, MUTED, false));
        head.addView(titles);
        col.addView(head);
        col.addView(gap(16));

        // Gyors sablonok
        LinearLayout presets = hbox();
        presets.addView(preset("HIIT", "30/10 mp · 8×", 30, 10, 8), presetLp());
        presets.addView(preset("Tempó", "60/20 mp · 6×", 60, 20, 6), presetLp());
        presets.addView(preset("Tabata", "20/10 mp · 8×", 20, 10, 8), presetLp());
        col.addView(presets, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(14));

        // Beállító kártya
        LinearLayout card = vbox();
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(CARD);
        cardBg.setCornerRadius(dp(20));
        cardBg.setStroke(dp(1), LINE);
        card.setBackground(cardBg);
        card.addView(stepperRow("Előkészület", "Visszaszámlálás indulás előtt", PREP_K, 0, 60, 5));
        card.addView(divider());
        card.addView(stepperRow("Futás", "Aktív időszak hossza", WORK_K, 1, 900, 5));
        card.addView(divider());
        card.addView(stepperRow("Pihenő", "Pihenés két futás között", REST_K, 0, 900, 5));
        card.addView(divider());
        card.addView(stepperRow("Körök", "Hányszor ismételjük", ROUND_K, 1, 99, 1));
        col.addView(card, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(12));

        // Összes idő
        totalText = text("", 13, MUTED, false);
        totalText.setGravity(Gravity.CENTER);
        totalText.setPadding(0, dp(2), 0, dp(14));
        col.addView(totalText, new LinearLayout.LayoutParams(-1, -2));

        // Indítás
        Button start = primaryButton("▶  Indítás");
        start.setOnClickListener(v -> startWorkout());
        col.addView(start);
        col.addView(gap(12));

        // Hang teszt
        Button test = ghostButton("🔊  Hang tesztelése");
        test.setOnClickListener(v -> cueWork());
        col.addView(test);

        col.addView(gap(16));
        TextView hint = text("Tipp: kapcsold be a hangot, és ne halkítsd le a telefont.\nA képernyő edzés közben bekapcsolva marad.",
                12, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        col.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        setupScroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        return setupScroll;
    }

    LinearLayout.LayoutParams presetLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        return lp;
    }

    View preset(String name, String sub, final int work, final int rest, final int rounds) {
        LinearLayout b = vbox();
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(11), dp(10), dp(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), LINE);
        b.setBackground(bg);
        b.setClickable(true);
        TextView t = text(name, 13.5f, TXT, true);
        t.setGravity(Gravity.CENTER);
        TextView s = text(sub, 11, MUTED, false);
        s.setGravity(Gravity.CENTER);
        b.addView(t);
        b.addView(s);
        b.setOnClickListener(v -> {
            cfg[WORK_K] = work; cfg[REST_K] = rest; cfg[ROUND_K] = rounds;
            saveAll(); refreshValues(); updateTotal(); buzz(20);
        });
        return b;
    }

    LinearLayout stepperRow(String title, String sub, final int key,
                            final int min, final int max, final int step) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout labels = vbox();
        labels.addView(text(title, 15.5f, TXT, true));
        labels.addView(text(sub, 12, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        Button minus = stepButton("−");
        LinearLayout valCol = vbox();
        valCol.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView val = text("0", 22, TXT, true);
        val.setGravity(Gravity.CENTER);
        val.setMinWidth(dp(56));
        valueLabels[key] = val;
        TextView unit = text(key == ROUND_K ? "kör" : "mp", 11, MUTED, false);
        unit.setGravity(Gravity.CENTER);
        valCol.addView(val);
        valCol.addView(unit);
        Button plus = stepButton("+");

        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(-2, -2);
        valLp.leftMargin = dp(6);
        valLp.rightMargin = dp(6);

        attachStepper(minus, key, -step, min, max);
        attachStepper(plus, key, step, min, max);

        row.addView(minus);
        row.addView(valCol, valLp);
        row.addView(plus);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    Button stepButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(TXT);
        b.setTextSize(22);
        b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), LINE);
        b.setBackground(bg);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        return b;
    }

    void attachStepper(final Button b, final int key, final int delta, final int min, final int max) {
        b.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    changeValue(key, delta, min, max);
                    v.setPressed(true);
                    stopRepeat();
                    repeatR = new Runnable() {
                        @Override public void run() {
                            changeValue(key, delta, min, max);
                            repeatH.postDelayed(this, 70);
                        }
                    };
                    repeatH.postDelayed(repeatR, 420);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    stopRepeat();
                    return true;
            }
            return false;
        });
    }

    void stopRepeat() {
        if (repeatR != null) { repeatH.removeCallbacks(repeatR); repeatR = null; }
    }

    void changeValue(int key, int delta, int min, int max) {
        int v = cfg[key] + delta;
        if (v < min) v = min;
        if (v > max) v = max;
        if (v == cfg[key]) return;
        cfg[key] = v;
        valueLabels[key].setText(String.valueOf(v));
        prefs.edit().putInt("k" + key, v).apply();
        updateTotal();
    }

    void refreshValues() {
        for (int i = 0; i < 4; i++) if (valueLabels[i] != null) valueLabels[i].setText(String.valueOf(cfg[i]));
    }

    void saveAll() {
        SharedPreferences.Editor e = prefs.edit();
        for (int i = 0; i < 4; i++) e.putInt("k" + i, cfg[i]);
        e.apply();
    }

    void updateTotal() {
        int total = cfg[PREP_K] + cfg[WORK_K] * cfg[ROUND_K] + cfg[REST_K] * Math.max(0, cfg[ROUND_K] - 1);
        totalText.setText("Teljes idő: " + fmtLong(total));
    }

    // ================= RUN KÉPERNYŐ =================

    View buildRun() {
        runView = vbox();
        runView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        runView.setGravity(Gravity.CENTER);
        runView.setPadding(dp(22), dp(22), dp(22), dp(28));

        phaseLabel = text("FUTÁS", 15, MUTED, true);
        phaseLabel.setGravity(Gravity.CENTER);
        phaseLabel.setLetterSpacing(0.22f);
        runView.addView(phaseLabel);
        runView.addView(gap(10));

        int size = (int) (getResources().getDisplayMetrics().widthPixels * 0.72f);
        FrameLayout ringHost = new FrameLayout(this);
        ring = new ProgressRing(this);
        ringHost.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout center = vbox();
        center.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams centerLp = new FrameLayout.LayoutParams(-2, -2);
        centerLp.gravity = Gravity.CENTER;
        timeText = text("0", 64, TXT, true);
        timeText.setGravity(Gravity.CENTER);
        roundInfo = text("Kör 1 / 8", 14, MUTED, true);
        roundInfo.setGravity(Gravity.CENTER);
        center.addView(timeText);
        center.addView(roundInfo);
        ringHost.addView(center, centerLp);
        runView.addView(ringHost, new LinearLayout.LayoutParams(size, size));
        runView.addView(gap(10));

        nextInfo = text("", 13, MUTED, false);
        nextInfo.setGravity(Gravity.CENTER);
        runView.addView(nextInfo);
        runView.addView(gap(18));

        LinearLayout controls = hbox();
        pauseBtn = ghostButton("Szünet");
        Button stop = ghostButton("Leállítás");
        pauseBtn.setOnClickListener(v -> togglePause());
        stop.setOnClickListener(v -> stopWorkout());
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp1.rightMargin = dp(6);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp2.leftMargin = dp(6);
        controls.addView(pauseBtn, lp1);
        controls.addView(stop, lp2);
        LinearLayout.LayoutParams cw = new LinearLayout.LayoutParams(-1, -2);
        controls.setLayoutParams(cw);
        runView.addView(controls);

        return runView;
    }

    void showRun(boolean run) {
        setupScroll.setVisibility(run ? View.GONE : View.VISIBLE);
        runView.setVisibility(run ? View.VISIBLE : View.GONE);
    }

    // ================= IDŐZÍTŐ LOGIKA =================

    void buildPlan() {
        plan.clear();
        if (cfg[PREP_K] > 0) plan.add(new Step(T_PREP, cfg[PREP_K], 0));
        for (int r = 1; r <= cfg[ROUND_K]; r++) {
            plan.add(new Step(T_WORK, cfg[WORK_K], r));
            if (cfg[REST_K] > 0 && r < cfg[ROUND_K]) plan.add(new Step(T_REST, cfg[REST_K], r));
        }
    }

    void startWorkout() {
        buildPlan();
        if (plan.isEmpty()) return;
        idx = 0;
        running = true;
        paused = false;
        pauseBtn.setText("Szünet");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showRun(true);
        beginStep(true);
        ui.post(ticker);
    }

    void beginStep(boolean first) {
        Step s = plan.get(idx);
        applyPhaseUI(s);
        stepEndElapsed = SystemClock.elapsedRealtime() + (long) s.dur * 1000L;
        lastShownSec = -1;
        timeText.setText(fmt(s.dur));
        ring.setProgress(1f);
        if (s.type == T_WORK) cueWork();
        else if (s.type == T_REST) cueRest();
        else if (s.type == T_PREP && !first) cueRest();
    }

    void tick() {
        if (!running || paused) return;
        Step s = plan.get(idx);
        long now = SystemClock.elapsedRealtime();
        double remain = (stepEndElapsed - now) / 1000.0;

        if (remain <= 0) {
            idx++;
            if (idx >= plan.size()) { finishWorkout(); return; }
            beginStep(false);
            ui.postDelayed(ticker, 40);
            return;
        }

        int shown = (int) Math.ceil(remain);
        if (shown != lastShownSec) {
            lastShownSec = shown;
            timeText.setText(fmt(shown));
            if (shown <= 3 && shown >= 1) cueTick();
        }
        ring.setProgress((float) (remain / s.dur));
        ui.postDelayed(ticker, 40);
    }

    void togglePause() {
        if (!running) return;
        if (!paused) {
            paused = true;
            remainingAtPause = stepEndElapsed - SystemClock.elapsedRealtime();
            pauseBtn.setText("Folytatás");
            ui.removeCallbacks(ticker);
        } else {
            paused = false;
            stepEndElapsed = SystemClock.elapsedRealtime() + remainingAtPause;
            pauseBtn.setText("Szünet");
            ui.post(ticker);
        }
    }

    void stopWorkout() {
        running = false;
        paused = false;
        ui.removeCallbacks(ticker);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showRun(false);
    }

    void finishWorkout() {
        running = false;
        ui.removeCallbacks(ticker);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        phaseLabel.setText("KÉSZ");
        phaseLabel.setTextColor(DONE);
        ring.setColor(DONE);
        ring.setProgress(1f);
        timeText.setText("✓");
        roundInfo.setText(cfg[ROUND_K] + " kör kész 💪");
        nextInfo.setText("Nyomd meg a Leállítást a visszalépéshez");
        cueDone();
    }

    void applyPhaseUI(Step s) {
        String name;
        int color;
        if (s.type == T_PREP) { name = "ELŐKÉSZÜLÉS"; color = PREP; }
        else if (s.type == T_WORK) { name = "FUTÁS"; color = WORK; }
        else { name = "PIHENŐ"; color = REST; }
        phaseLabel.setText(name);
        phaseLabel.setTextColor(color);
        ring.setColor(color);
        roundInfo.setText("Kör " + Math.max(1, s.round) + " / " + cfg[ROUND_K]);
        Step next = (idx + 1 < plan.size()) ? plan.get(idx + 1) : null;
        if (next == null) nextInfo.setText("Utolsó szakasz");
        else nextInfo.setText("Következik: " + phaseNameLower(next.type));
    }

    String phaseNameLower(int type) {
        if (type == T_PREP) return "előkészület";
        if (type == T_WORK) return "futás";
        return "pihenő";
    }

    // ================= HANG + REZGÉS =================

    void cueTick() { tone(new double[][]{{760, 90, 0, 0.35}}); buzz(30); }

    void cueWork() {
        tone(new double[][]{{920, 150, 60, 0.55}, {920, 170, 0, 0.55}});
        buzz(new long[]{0, 60, 60, 60});
    }

    void cueRest() { tone(new double[][]{{430, 300, 0, 0.5}}); buzz(120); }

    void cueDone() {
        tone(new double[][]{{660, 220, 40, 0.5}, {840, 220, 40, 0.5}, {1040, 320, 0, 0.5}});
        buzz(new long[]{0, 120, 80, 120, 80, 240});
    }

    /** Hangsorozat lejátszása háttérszálon. Minden elem: {frekvencia, hossz(ms), utána szünet(ms), hangerő}. */
    void tone(final double[][] seq) {
        new Thread(() -> {
            for (double[] s : seq) {
                synth(s[0], (int) s[1], s[3]);
                if (s[2] > 0) { try { Thread.sleep((long) s[2]); } catch (InterruptedException ignored) {} }
            }
        }).start();
    }

    void synth(double freq, int durMs, double vol) {
        final int sr = 44100;
        int n = (int) ((long) durMs * sr / 1000);
        if (n <= 0) return;
        short[] buf = new short[n];
        int fade = Math.max(1, Math.min(n / 8, sr / 200));
        for (int i = 0; i < n; i++) {
            double env = 1.0;
            if (i < fade) env = i / (double) fade;
            else if (i > n - fade) env = (n - i) / (double) fade;
            double sample = Math.sin(2.0 * Math.PI * freq * i / sr);
            buf[i] = (short) (vol * env * sample * 32767.0);
        }
        int min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bytes = Math.max(min, n * 2);
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, sr,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bytes, AudioTrack.MODE_STATIC);
        try {
            at.write(buf, 0, n);
            at.play();
            Thread.sleep(durMs + 40);
        } catch (Exception ignored) {
        } finally {
            try { at.stop(); } catch (Exception ignored) {}
            try { at.release(); } catch (Exception ignored) {}
        }
    }

    void buzz(long ms) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        else vibrator.vibrate(ms);
    }

    void buzz(long[] pattern) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        else vibrator.vibrate(pattern, -1);
    }

    // ================= SEGÉD UI =================

    LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout hbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    TextView text(String s, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    View gap(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(h)));
        return v;
    }

    View divider() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.leftMargin = dp(14);
        lp.rightMargin = dp(14);
        v.setLayoutParams(lp);
        v.setBackgroundColor(LINE);
        return v;
    }

    Button primaryButton(String label) {
        Button b = baseButton(label, TXT);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{INDIGO, VIOLET});
        bg.setCornerRadius(dp(18));
        b.setBackground(bg);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(18);
        return b;
    }

    Button ghostButton(String label) {
        Button b = baseButton(label, TXT);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2);
        bg.setCornerRadius(dp(15));
        bg.setStroke(dp(1), LINE);
        b.setBackground(bg);
        b.setTextSize(16);
        return b;
    }

    Button baseButton(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(color);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        b.setLayoutParams(lp);
        b.setStateListAnimator(null);
        return b;
    }

    int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    String fmt(int sec) {
        if (sec < 0) sec = 0;
        int m = sec / 60, s = sec % 60;
        return m > 0 ? m + ":" + String.format(Locale.US, "%02d", s) : String.valueOf(s);
    }

    String fmtLong(int sec) {
        if (sec < 0) sec = 0;
        int m = sec / 60, s = sec % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ui.removeCallbacks(ticker);
        stopRepeat();
    }

    // ================= KÖRGYŰRŰ NÉZET =================

    static class ProgressRing extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float progress = 1f;

        ProgressRing(Context c) {
            super(c);
            bgPaint.setStyle(Paint.Style.STROKE);
            bgPaint.setColor(CARD2);
            fgPaint.setStyle(Paint.Style.STROKE);
            fgPaint.setStrokeCap(Paint.Cap.ROUND);
            fgPaint.setColor(WORK);
        }

        void setProgress(float p) {
            progress = Math.max(0f, Math.min(1f, p));
            invalidate();
        }

        void setColor(int col) {
            fgPaint.setColor(col);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float stroke = w * 0.058f;
            bgPaint.setStrokeWidth(stroke);
            fgPaint.setStrokeWidth(stroke);
            float pad = stroke / 2f + w * 0.02f;
            rect.set(pad, pad, w - pad, getHeight() - pad);
            canvas.drawArc(rect, 0, 360, false, bgPaint);
            canvas.drawArc(rect, -90, 360f * progress, false, fgPaint);
        }
    }
}
