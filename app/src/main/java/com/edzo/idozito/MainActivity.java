package com.edzo.idozito;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Beállító és futás-képernyő. A tényleges időzítést a TimerService végzi (háttérben,
 * kikapcsolt képernyőnél is); ez az Activity csak elindítja, és a service broadcast-jaiból
 * frissíti a felületet.
 */
public class MainActivity extends Activity {

    // Színek
    static final int BG = 0xFF0B1020, CARD = 0xFF1A2238, CARD2 = 0xFF212B47;
    static final int TXT = 0xFFF2F5FF, MUTED = 0xFF93A0C4, LINE = 0xFF2A3552, ACCENT = 0xFF8B9BFF;
    static final int INDIGO = 0xFF6366F1, VIOLET = 0xFF8B5CF6;
    static final int WORK = 0xFF22C55E, REST = 0xFF38BDF8, PREP = 0xFFF59E0B, DONE = 0xFFA78BFA;

    static final int PREP_K = 0, WORK_K = 1, REST_K = 2, ROUND_K = 3;
    final int[] cfg = new int[4];
    final int[] DEF = {10, 10, 30, 8};
    int workSoundIdx = 2, restSoundIdx = 1;
    boolean trackDistance = false, precount = true;

    static final int REQ_LOCATION = 1001, REQ_NOTIF = 1002;

    SharedPreferences prefs;

    // UI
    FrameLayout root;
    ScrollView setupScroll;
    LinearLayout runView;
    TextView totalText;
    final TextView[] valueLabels = new TextView[4];
    TextView workSoundLabel, restSoundLabel;
    Switch distanceSwitch, precountSwitch;
    TextView phaseLabel, timeText, roundInfo, distanceText;
    Button pauseBtn;
    ProgressRing ring;
    boolean lastPaused = false;

    final Handler repeatH = new Handler(Looper.getMainLooper());
    Runnable repeatR;
    boolean receiverRegistered = false;

    // ---------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("edzo", MODE_PRIVATE);
        for (int i = 0; i < 4; i++) cfg[i] = prefs.getInt("k" + i, DEF[i]);
        workSoundIdx = prefs.getInt("ws", 2);
        restSoundIdx = prefs.getInt("rs", 1);
        trackDistance = prefs.getBoolean("track", false);
        precount = prefs.getBoolean("pre", true);

        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.addView(buildSetup());
        root.addView(buildRun());
        setContentView(root);
        showRun(false);

        refreshValues();
        updateSoundLabels();
        updateTotal();

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }

    // ================= SETUP =================

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

        // Sablonok
        LinearLayout presets = hbox();
        presets.addView(preset("HIIT", "30/10 mp · 8×", 30, 10, 8), presetLp());
        presets.addView(preset("Tempó", "60/20 mp · 6×", 60, 20, 6), presetLp());
        presets.addView(preset("Tabata", "20/10 mp · 8×", 20, 10, 8), presetLp());
        col.addView(presets, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(14));

        // Idő-beállítások
        LinearLayout card = card();
        card.addView(stepperRow("Előkészület", "Visszaszámlálás indulás előtt", PREP_K, 0, 600));
        card.addView(divider());
        card.addView(stepperRow("Futás", "Aktív időszak hossza", WORK_K, 1, 3600));
        card.addView(divider());
        card.addView(stepperRow("Pihenő", "Pihenés két futás között", REST_K, 0, 3600));
        card.addView(divider());
        card.addView(stepperRow("Körök", "Hányszor ismételjük", ROUND_K, 1, 99));
        col.addView(card, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(6));
        TextView tip = text("A számra koppintva pontos értéket írhatsz be.", 11.5f, MUTED, false);
        tip.setPadding(dp(4), 0, 0, dp(6));
        col.addView(tip);
        col.addView(gap(8));

        // Hangok + extrák
        LinearLayout card2 = card();
        workSoundLabel = text("", 14, ACCENT, true);
        restSoundLabel = text("", 14, ACCENT, true);
        card2.addView(navRow("Futás hangja", "Sípszó a futás kezdetén", workSoundLabel, () -> chooseSound(true)));
        card2.addView(divider());
        card2.addView(navRow("Pihenő hangja", "Sípszó a pihenő kezdetén", restSoundLabel, () -> chooseSound(false)));
        card2.addView(divider());
        distanceSwitch = new Switch(this);
        distanceSwitch.setChecked(trackDistance);
        card2.addView(switchRow("Táv mérése (GPS)", "Lefutott távolság mérése", distanceSwitch));
        distanceSwitch.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && !hasLocationPermission()) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
                return;
            }
            trackDistance = checked;
            prefs.edit().putBoolean("track", trackDistance).apply();
        });
        card2.addView(divider());
        precountSwitch = new Switch(this);
        precountSwitch.setChecked(precount);
        card2.addView(switchRow("Visszaszámláló csipogás", "3-2-1 jelzés a szakasz vége előtt", precountSwitch));
        precountSwitch.setOnCheckedChangeListener((btn, checked) -> {
            precount = checked;
            prefs.edit().putBoolean("pre", precount).apply();
        });
        col.addView(card2, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(12));

        totalText = text("", 13, MUTED, false);
        totalText.setGravity(Gravity.CENTER);
        totalText.setPadding(0, dp(2), 0, dp(14));
        col.addView(totalText, new LinearLayout.LayoutParams(-1, -2));

        Button start = primaryButton("▶  Indítás");
        start.setOnClickListener(v -> startWorkout());
        col.addView(start);
        col.addView(gap(12));

        Button hist = ghostButton("📜  Korábbi edzések");
        hist.setOnClickListener(v -> showHistory());
        col.addView(hist);

        col.addView(gap(16));
        TextView hint = text("A telefon a képernyő kikapcsolása után is folytatja az edzést és sípol.\nNe halkítsd le a hangot.",
                12, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        col.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        setupScroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        return setupScroll;
    }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), LINE);
        c.setBackground(bg);
        return c;
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
            saveAll(); refreshValues(); updateTotal(); vibrateShort();
        });
        return b;
    }

    LinearLayout stepperRow(String title, String sub, final int key, final int min, final int max) {
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
        val.setPadding(dp(4), dp(2), dp(4), dp(2));
        val.setOnClickListener(v -> showNumberDialog(key, min, max,
                title + (key == ROUND_K ? " (kör)" : " (mp)")));
        valueLabels[key] = val;
        TextView unit = text(key == ROUND_K ? "kör" : "mp", 11, MUTED, false);
        unit.setGravity(Gravity.CENTER);
        valCol.addView(val);
        valCol.addView(unit);
        Button plus = stepButton("+");

        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(-2, -2);
        valLp.leftMargin = dp(4);
        valLp.rightMargin = dp(4);

        attachStepper(minus, key, -1, min, max);
        attachStepper(plus, key, 1, min, max);

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
        b.setStateListAnimator(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), LINE);
        b.setBackground(bg);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        return b;
    }

    void attachStepper(final Button b, final int key, final int dir, final int min, final int max) {
        b.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    changeValue(key, dir, min, max);
                    v.setPressed(true);
                    stopRepeat();
                    repeatR = new Runnable() {
                        int count = 0;
                        @Override public void run() {
                            count++;
                            int step = count < 10 ? 1 : count < 25 ? 2 : count < 45 ? 5 : 10;
                            changeValue(key, dir * step, min, max);
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
        int v = Math.max(min, Math.min(max, cfg[key] + delta));
        if (v == cfg[key]) return;
        cfg[key] = v;
        valueLabels[key].setText(String.valueOf(v));
        prefs.edit().putInt("k" + key, v).apply();
        updateTotal();
    }

    void showNumberDialog(int key, int min, int max, String title) {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(cfg[key]));
        et.setSelectAllOnFocus(true);
        int pad = dp(20);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(pad, dp(8), pad, 0);
        wrap.addView(et);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrap)
                .setPositiveButton("OK", (d, w) -> {
                    try {
                        int v = Math.max(min, Math.min(max, Integer.parseInt(et.getText().toString().trim())));
                        cfg[key] = v;
                        valueLabels[key].setText(String.valueOf(v));
                        prefs.edit().putInt("k" + key, v).apply();
                        updateTotal();
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

    LinearLayout navRow(String title, String sub, TextView valueOut, Runnable onTap) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setClickable(true);
        row.setOnClickListener(v -> onTap.run());

        LinearLayout labels = vbox();
        labels.addView(text(title, 15.5f, TXT, true));
        if (sub != null) labels.addView(text(sub, 12, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        valueOut.setGravity(Gravity.END);
        row.addView(valueOut);
        TextView chev = text("  ▸", 15, MUTED, false);
        row.addView(chev);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    LinearLayout switchRow(String title, String sub, Switch sw) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout labels = vbox();
        labels.addView(text(title, 15.5f, TXT, true));
        if (sub != null) labels.addView(text(sub, 12, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(sw);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    void chooseSound(final boolean forWork) {
        final String[] names = new String[Beeper.SOUNDS.length];
        for (int i = 0; i < names.length; i++) names[i] = Beeper.SOUNDS[i].name;
        int cur = forWork ? workSoundIdx : restSoundIdx;
        new AlertDialog.Builder(this)
                .setTitle(forWork ? "Futás hangja" : "Pihenő hangja")
                .setSingleChoiceItems(names, cur, (d, which) -> {
                    Beeper.play(which);
                    if (forWork) { workSoundIdx = which; prefs.edit().putInt("ws", which).apply(); }
                    else { restSoundIdx = which; prefs.edit().putInt("rs", which).apply(); }
                    updateSoundLabels();
                })
                .setPositiveButton("Kész", null)
                .show();
    }

    void updateSoundLabels() {
        if (workSoundLabel != null) workSoundLabel.setText(Beeper.soundAt(workSoundIdx).name);
        if (restSoundLabel != null) restSoundLabel.setText(Beeper.soundAt(restSoundIdx).name);
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

    // ================= RUN =================

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

        distanceText = text("", 15, ACCENT, true);
        distanceText.setGravity(Gravity.CENTER);
        runView.addView(distanceText);
        runView.addView(gap(16));

        LinearLayout controls = hbox();
        pauseBtn = ghostButton("Szünet");
        Button stop = ghostButton("Leállítás");
        pauseBtn.setOnClickListener(v -> {
            if (lastPaused) cmd(TimerService.ACTION_RESUME);
            else cmd(TimerService.ACTION_PAUSE);
        });
        stop.setOnClickListener(v -> { cmd(TimerService.ACTION_STOP); showRun(false); });
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp1.rightMargin = dp(6);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp2.leftMargin = dp(6);
        controls.addView(pauseBtn, lp1);
        controls.addView(stop, lp2);
        controls.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        runView.addView(controls);

        return runView;
    }

    void showRun(boolean run) {
        setupScroll.setVisibility(run ? View.GONE : View.VISIBLE);
        runView.setVisibility(run ? View.VISIBLE : View.GONE);
    }

    // ================= Service parancsok =================

    void startWorkout() {
        if (cfg[WORK_K] < 1) return;
        Intent i = new Intent(this, TimerService.class).setAction(TimerService.ACTION_START);
        i.putExtra(TimerService.EX_PREP, cfg[PREP_K]);
        i.putExtra(TimerService.EX_WORK, cfg[WORK_K]);
        i.putExtra(TimerService.EX_REST, cfg[REST_K]);
        i.putExtra(TimerService.EX_ROUNDS, cfg[ROUND_K]);
        i.putExtra(TimerService.EX_WS, workSoundIdx);
        i.putExtra(TimerService.EX_RS, restSoundIdx);
        i.putExtra(TimerService.EX_TRACK, trackDistance && hasLocationPermission());
        i.putExtra(TimerService.EX_PRE, precount);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);

        // Kezdeti UI, a további frissítés a broadcast-okból jön.
        lastPaused = false;
        pauseBtn.setEnabled(true);
        pauseBtn.setText("Szünet");
        setPhaseUI(cfg[PREP_K] > 0 ? TimerService.T_PREP : TimerService.T_WORK, 1);
        timeText.setText(fmt(cfg[PREP_K] > 0 ? cfg[PREP_K] : cfg[WORK_K]));
        ring.setProgress(1f);
        distanceText.setText(trackDistance && hasLocationPermission() ? "📍 0 m" : "");
        showRun(true);
    }

    void cmd(String action) {
        startService(new Intent(this, TimerService.class).setAction(action));
    }

    // ================= Broadcast fogadás =================

    final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            if (TimerService.B_TICK.equals(a)) onTick(i);
            else if (TimerService.B_DONE.equals(a)) onDone(i);
            else if (TimerService.B_STOPPED.equals(a)) showRun(false);
        }
    };

    void onTick(Intent i) {
        int phase = i.getIntExtra(TimerService.EX_PHASE, TimerService.T_WORK);
        int remain = i.getIntExtra(TimerService.EX_REMAIN, 0);
        int round = i.getIntExtra(TimerService.EX_ROUND, 1);
        int rounds = i.getIntExtra(TimerService.EX_ROUNDS, cfg[ROUND_K]);
        float prog = i.getFloatExtra(TimerService.EX_PROGRESS, 0);
        double dist = i.getDoubleExtra(TimerService.EX_DIST, -1);
        boolean paused = i.getBooleanExtra(TimerService.EX_PAUSED, false);

        showRun(true);
        setPhaseUI(phase, round, rounds);
        timeText.setText(fmt(remain));
        ring.setProgress(prog);
        distanceText.setText(dist >= 0 ? "📍 " + fmtDist(dist) : "");

        lastPaused = paused;
        pauseBtn.setText(paused ? "Folytatás" : "Szünet");
        if (paused) phaseLabel.setText(phaseName(phase) + " · SZÜNET");
    }

    void onDone(Intent i) {
        int dur = i.getIntExtra(TimerService.EX_DUR, 0);
        double dist = i.getDoubleExtra(TimerService.EX_DIST, -1);
        int rounds = i.getIntExtra(TimerService.EX_ROUND, cfg[ROUND_K]);
        showRun(true);
        phaseLabel.setText("KÉSZ");
        phaseLabel.setTextColor(DONE);
        ring.setColor(DONE);
        ring.setProgress(1f);
        timeText.setText("✓");
        roundInfo.setText(rounds + " kör kész 💪");
        distanceText.setText("Idő: " + fmtLong(dur) + (dist >= 0 ? "  ·  📍 " + fmtDist(dist) : ""));
        lastPaused = false;
        pauseBtn.setEnabled(false);
        pauseBtn.setText("Kész");
    }

    void setPhaseUI(int phase, int round) { setPhaseUI(phase, round, cfg[ROUND_K]); }

    void setPhaseUI(int phase, int round, int rounds) {
        int color = phase == TimerService.T_PREP ? PREP : phase == TimerService.T_WORK ? WORK : REST;
        phaseLabel.setText(phaseName(phase));
        phaseLabel.setTextColor(color);
        ring.setColor(color);
        roundInfo.setText("Kör " + Math.max(1, round) + " / " + rounds);
    }

    String phaseName(int phase) {
        return phase == TimerService.T_PREP ? "ELŐKÉSZÜLÉS" : phase == TimerService.T_WORK ? "FUTÁS" : "PIHENŐ";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!receiverRegistered) {
            IntentFilter f = new IntentFilter();
            f.addAction(TimerService.B_TICK);
            f.addAction(TimerService.B_DONE);
            f.addAction(TimerService.B_STOPPED);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(rx, f);
            receiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            try { unregisterReceiver(rx); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
    }

    // ================= Előzmények =================

    void showHistory() {
        JSONArray arr = History.load(this);
        ScrollView sv = new ScrollView(this);
        LinearLayout list = vbox();
        list.setPadding(dp(20), dp(8), dp(20), dp(8));
        if (arr.length() == 0) {
            list.addView(text("Még nincs elmentett edzés.\nFejezz be egy edzést, és itt megjelenik.", 14, MUTED, false));
        } else {
            SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd  HH:mm", new Locale("hu"));
            for (int k = 0; k < arr.length(); k++) {
                JSONObject o = arr.optJSONObject(k);
                if (o == null) continue;
                LinearLayout item = vbox();
                item.setPadding(0, dp(10), 0, dp(10));
                item.addView(text(df.format(new Date(o.optLong("ts"))), 13, MUTED, false));
                double dist = o.optDouble("dist", -1);
                String line = "⏱ " + fmtLong(o.optInt("dur")) + "   ·   " + o.optInt("rounds") + " kör";
                if (dist >= 0) line += "   ·   📍 " + fmtDist(dist);
                item.addView(text(line, 15.5f, TXT, true));
                item.addView(text(o.optInt("work") + " mp futás / " + o.optInt("rest") + " mp pihenő", 12, MUTED, false));
                list.addView(item);
                if (k < arr.length() - 1) {
                    View dv = new View(this);
                    dv.setBackgroundColor(LINE);
                    list.addView(dv, new LinearLayout.LayoutParams(-1, dp(1)));
                }
            }
        }
        sv.addView(list);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Korábbi edzések")
                .setView(sv)
                .setPositiveButton("Bezár", null);
        if (arr.length() > 0) {
            b.setNegativeButton("Törlés", (d, w) ->
                    new AlertDialog.Builder(this)
                            .setMessage("Biztosan törlöd az összes elmentett edzést?")
                            .setPositiveButton("Törlés", (dd, ww) -> History.clear(this))
                            .setNegativeButton("Mégse", null)
                            .show());
        }
        b.show();
    }

    // ================= Engedélyek =================

    boolean hasLocationPermission() {
        return Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            trackDistance = granted;
            prefs.edit().putBoolean("track", trackDistance).apply();
            if (distanceSwitch != null) distanceSwitch.setChecked(granted);
            if (!granted) Toast.makeText(this, "A táv méréséhez helyhozzáférés kell.", Toast.LENGTH_LONG).show();
        }
    }

    // ================= Segéd UI =================

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }

    TextView text(String s, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    View gap(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(h))); return v; }

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
        Button b = baseButton(label);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{INDIGO, VIOLET});
        bg.setCornerRadius(dp(18));
        b.setBackground(bg);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(18);
        return b;
    }

    Button ghostButton(String label) {
        Button b = baseButton(label);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2);
        bg.setCornerRadius(dp(15));
        bg.setStroke(dp(1), LINE);
        b.setBackground(bg);
        b.setTextSize(16);
        return b;
    }

    Button baseButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TXT);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(dp(18), dp(16), dp(18), dp(16));
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        b.setStateListAnimator(null);
        return b;
    }

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    String fmt(int sec) {
        if (sec < 0) sec = 0;
        int m = sec / 60, s = sec % 60;
        return m > 0 ? m + ":" + String.format(Locale.US, "%02d", s) : String.valueOf(s);
    }

    String fmtLong(int sec) {
        if (sec < 0) sec = 0;
        return String.format(Locale.US, "%d:%02d", sec / 60, sec % 60);
    }

    String fmtDist(double m) {
        if (m < 0) return "—";
        if (m < 1000) return Math.round(m) + " m";
        return String.format(Locale.US, "%.2f km", m / 1000.0);
    }

    void vibrateShort() {
        try {
            android.os.Vibrator vb = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vb != null && vb.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26)
                    vb.vibrate(android.os.VibrationEffect.createOneShot(20, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                else vb.vibrate(20);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeat();
    }

    // ================= Körgyűrű =================

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

        void setProgress(float p) { progress = Math.max(0f, Math.min(1f, p)); invalidate(); }
        void setColor(int col) { fgPaint.setColor(col); invalidate(); }

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
