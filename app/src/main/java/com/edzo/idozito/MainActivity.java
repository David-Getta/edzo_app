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
import android.graphics.Outline;
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
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    static final int BG = 0xFF070912, CARD = 0xFF121A33, CARD2 = 0xFF19224A;
    static final int TXT = 0xFFEAF6FF, MUTED = 0xFF8AA0C4, LINE = 0xFF25335C, ACCENT = 0xFF22E0FF;
    static final int INDIGO = 0xFF22E0FF, VIOLET = 0xFFFF3DDB; // márka-gradiens: cián → magenta
    static final int WORK = 0xFF22E0FF, REST = 0xFF7A5CFF, PREP = 0xFFFFC24D, DONE = 0xFFFF3DDB;
    // Áttetsző „üveg" felületek – a generált háttérkép finoman átüt rajtuk
    static final int GLASS = 0xE6121A33, GLASS2 = 0xD919224A, GLASS_LINE = 0x33FFFFFF;

    static final int PREP_K = 0, WORK_K = 1, REST_K = 2, ROUND_K = 3, WARM_K = 4, COOL_K = 5;
    final int[] cfg = new int[6];
    final int[] DEF = {10, 10, 30, 8, 0, 0};
    int workSoundIdx = 2, restSoundIdx = 1;
    boolean trackDistance = false, precount = true, voice = false;
    String programName = ""; // üres = sima futás (intervallum)

    static final int REQ_LOCATION = 1001, REQ_NOTIF = 1002;

    SharedPreferences prefs;

    // UI
    FrameLayout root;
    ScrollView setupScroll;
    LinearLayout runView;
    LinearLayout templatesBox;
    LinearLayout goalBox;
    LinearLayout progressBox;
    LinearLayout planBar;
    TextView planCaption;
    TextView totalText;
    final TextView[] valueLabels = new TextView[6];
    TextView workSoundLabel, restSoundLabel;
    TextView programLabel, programPreview, workRowTitle, workRowSub;
    LinearLayout programCard;
    Switch distanceSwitch, precountSwitch, voiceSwitch;
    TextView phaseLabel, timeText, roundInfo, distanceText;
    TextView exText, exDesc, nextText, recordText, levelText;
    TextView statElapsed, statCal, statSteps;
    Button pauseBtn;
    ProgressRing ring;
    boolean lastPaused = false;
    boolean finished = false;

    // Téma (Beállításokból)
    int tAccent, tAccent2, tWork, tRest;
    boolean tPace;
    int builtRev;

    final Handler repeatH = new Handler(Looper.getMainLooper());
    Runnable repeatR;
    boolean receiverRegistered = false;

    // ---------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("edzo", MODE_PRIVATE);
        tAccent = Theme.accent(this);
        tAccent2 = Theme.accent2(this);
        tWork = Theme.work(this);
        tRest = Theme.rest(this);
        tPace = Theme.paceMode(this);
        Beeper.masterVolume = Theme.volume(this);
        builtRev = Theme.rev(this);
        for (int i = 0; i < 6; i++) cfg[i] = prefs.getInt("k" + i, DEF[i]);
        workSoundIdx = prefs.getInt("ws", 2);
        restSoundIdx = prefs.getInt("rs", 1);
        trackDistance = prefs.getBoolean("track", false);
        precount = prefs.getBoolean("pre", true);
        voice = prefs.getBoolean("voice", false);
        programName = prefs.getString("progname", "");

        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        addBackgroundImage(root);
        root.addView(buildSetup());
        root.addView(buildRun());
        setContentView(root);
        showRun(false);

        refreshValues();
        updateSoundLabels();
        updateProgramUI();
        refreshTemplates();
        refreshGoal();
        refreshProgress();

        java.util.ArrayList<String> perms = new java.util.ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        if (Build.VERSION.SDK_INT >= 29 &&
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.ACTIVITY_RECOGNITION);
        if (!perms.isEmpty()) requestPermissions(perms.toArray(new String[0]), REQ_NOTIF);
        Reminders.scheduleAll(this);
        WeeklyReceiver.schedule(this);
    }

    // ================= Háttérkép =================

    /** A Higgsfielddel generált háttérkép + sötét fátyol (a szöveg olvashatóságáért). */
    void addBackgroundImage(FrameLayout host) {
        int id = drawableId("bg_main");
        if (id == 0) return;
        try {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageResource(id);
            host.addView(img, new FrameLayout.LayoutParams(-1, -1));
            Ux.kenBurns(img); // lassan „élő" háttér
            View scrim = new View(this);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x80070912, 0xD9070912, 0xF2070912});
            scrim.setBackground(g);
            host.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        } catch (Exception ignored) {}
    }

    int drawableId(String name) {
        try { return getResources().getIdentifier(name, "drawable", getPackageName()); }
        catch (Exception e) { return 0; }
    }

    /** Lekerekített sarkú vágás egy nézethez (API 21+). */
    void roundClip(View v, final int radiusDp) {
        v.setClipToOutline(true);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radiusDp));
            }
        });
    }

    // ================= SETUP =================

    View buildSetup() {
        setupScroll = new ScrollView(this);
        setupScroll.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        setupScroll.setFillViewport(true);

        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        // Fejléc banner – Higgsfield hero kép (ha van), különben gradiens
        col.addView(buildBanner(), new LinearLayout.LayoutParams(-1, dp(140)));
        col.addView(gap(12));

        // Haladás-csík (szint / sorozat / edzésszám) – koppintásra Statisztika
        progressBox = hbox();
        progressBox.setClickable(true);
        progressBox.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        col.addView(progressBox, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(14));

        // Heti cél (dinamikus kártya)
        goalBox = vbox();
        col.addView(goalBox, new LinearLayout.LayoutParams(-1, -2));

        // Sablonok
        LinearLayout presets = hbox();
        presets.addView(preset("HIIT", "30/10 mp · 8×", 30, 10, 8), presetLp());
        presets.addView(preset("Tempó", "60/20 mp · 6×", 60, 20, 6), presetLp());
        presets.addView(preset("Tabata", "20/10 mp · 8×", 20, 10, 8), presetLp());
        col.addView(presets, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(12));

        // Saját mentett sablonok (dinamikus)
        templatesBox = vbox();
        col.addView(templatesBox, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(8));

        // Edzés típusa (futás vagy gyakorlatsor)
        programCard = card();
        programLabel = text("", 14, tAccent, true);
        programCard.addView(navRow("Edzés típusa", "Futás vagy gyakorlatsor körökben", programLabel, this::chooseProgram));
        programPreview = text("", 12, MUTED, false);
        programPreview.setPadding(dp(18), 0, dp(18), dp(14));
        programCard.addView(programPreview);
        programCard.setOnLongClickListener(v -> maybeDeleteCustomProgram());
        col.addView(programCard, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(16));

        // Idő-beállítások
        LinearLayout card = card();
        card.addView(stepperRow("Bemelegítés", "Könnyű ráhangolódás az elején", WARM_K, 0, 1800));
        card.addView(divider());
        card.addView(stepperRow("Előkészület", "Visszaszámlálás indulás előtt", PREP_K, 0, 600));
        card.addView(divider());
        card.addView(stepperRow("Futás", "Aktív időszak hossza", WORK_K, 1, 3600));
        card.addView(divider());
        card.addView(stepperRow("Pihenő", "Pihenés két futás között", REST_K, 0, 3600));
        card.addView(divider());
        card.addView(stepperRow("Körök", "Hányszor ismételjük", ROUND_K, 1, 99));
        card.addView(divider());
        card.addView(stepperRow("Levezetés", "Lassú levezetés a végén", COOL_K, 0, 1800));
        col.addView(card, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(8));
        TextView tip = text("A számra koppintva pontos értéket írhatsz be.", 11.5f, MUTED, false);
        tip.setPadding(dp(4), 0, 0, dp(6));
        col.addView(tip);
        col.addView(gap(16));

        // Hangok + extrák
        LinearLayout card2 = card();
        workSoundLabel = text("", 14, tAccent, true);
        restSoundLabel = text("", 14, tAccent, true);
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
        card2.addView(switchRow("Visszaszámláló csipogás", "Jelzés a szakasz vége előtt (hossz a Beállításokban)", precountSwitch));
        precountSwitch.setOnCheckedChangeListener((btn, checked) -> {
            precount = checked;
            prefs.edit().putBoolean("pre", precount).apply();
        });
        card2.addView(divider());
        voiceSwitch = new Switch(this);
        voiceSwitch.setChecked(voice);
        card2.addView(switchRow("Hangos bemondás", "A telefon kimondja a szakaszokat", voiceSwitch));
        voiceSwitch.setOnCheckedChangeListener((btn, checked) -> {
            voice = checked;
            prefs.edit().putBoolean("voice", voice).apply();
        });
        col.addView(card2, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(18));

        // Edzés felépítése – arányos színsáv
        LinearLayout planWrap = vbox();
        TextView planTitle = text("Az edzés felépítése", 12.5f, MUTED, true);
        planTitle.setPadding(dp(2), 0, 0, dp(8));
        planWrap.addView(planTitle);
        planBar = hbox();
        roundClip(planBar, 7);
        planWrap.addView(planBar, new LinearLayout.LayoutParams(-1, dp(14)));
        planWrap.addView(gap(6));
        planCaption = text("", 11.5f, MUTED, false);
        planWrap.addView(planCaption);
        col.addView(planWrap, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(16));

        totalText = text("", 13, MUTED, false);
        totalText.setGravity(Gravity.CENTER);
        totalText.setPadding(0, dp(4), 0, dp(18));
        col.addView(totalText, new LinearLayout.LayoutParams(-1, -2));

        Button start = primaryButton("▶  Indítás");
        start.setOnClickListener(v -> startWorkout());
        col.addView(start);
        col.addView(gap(22));

        // Funkció-csempék (2 oszlop)
        LinearLayout grid = vbox();
        grid.addView(tileRow(
                featureTile("📜", "Előzmények", () -> startActivity(new Intent(this, HistoryActivity.class))),
                featureTile("📈", "Statisztika", () -> startActivity(new Intent(this, StatsActivity.class)))));
        grid.addView(gap(10));
        grid.addView(tileRow(
                featureTile("📊", "Profil / BMI", () -> startActivity(new Intent(this, ProfileActivity.class))),
                featureTile("🔔", "Emlékeztetők", () -> startActivity(new Intent(this, RemindersActivity.class)))));
        grid.addView(gap(10));
        grid.addView(tileRow(
                featureTile("⚙️", "Beállítások", () -> startActivity(new Intent(this, SettingsActivity.class))),
                featureTile("💾", "Sablon mentése", this::saveTemplateDialog)));
        col.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        col.addView(gap(24));
        TextView hint = text("A telefon a képernyő kikapcsolása után is folytatja az edzést és sípol.\nNe halkítsd le a hangot.",
                12, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        col.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        setupScroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        col.post(() -> Ux.enterChildren(col, 40, 50)); // egymás utáni beúszás
        return setupScroll;
    }

    /** A fejléc: futó-sziluett hero kép sötét átmenettel és a címmel, vagy gradiens. */
    View buildBanner() {
        FrameLayout banner = new FrameLayout(this);
        roundClip(banner, 22);
        int heroId = drawableId("hero_run");
        if (heroId != 0) {
            try {
                ImageView hero = new ImageView(this);
                hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
                hero.setImageResource(heroId);
                banner.addView(hero, new FrameLayout.LayoutParams(-1, -1));
                View sc = new View(this);
                GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xF2070912, 0x99070912, 0x1A000000});
                sc.setBackground(g);
                banner.addView(sc, new FrameLayout.LayoutParams(-1, -1));
            } catch (Exception e) {
                setGradientBg(banner);
            }
        } else {
            setGradientBg(banner);
        }
        LinearLayout btitles = vbox();
        btitles.setPadding(dp(22), dp(20), dp(22), dp(20));
        btitles.addView(text("My trainer", 27, 0xFFFFFFFF, true));
        TextView bsub = text("Intervallum edző · készen állsz? 💪", 13, 0xFFFFFFFF, false);
        bsub.setAlpha(0.92f);
        btitles.addView(bsub);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(-2, -2);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        banner.addView(btitles, tlp);
        return banner;
    }

    void setGradientBg(View v) {
        GradientDrawable bbg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{tAccent, tAccent2});
        v.setBackground(bbg);
    }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GLASS);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), GLASS_LINE);
        c.setBackground(bg);
        return c;
    }

    // ---- Funkció-csempék ----

    View featureTile(String emoji, String label, Runnable onTap) {
        LinearLayout t = vbox();
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(18), dp(14), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GLASS); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), GLASS_LINE);
        t.setBackground(bg);
        t.setClickable(true);
        TextView e = text(emoji, 26, TXT, false); e.setGravity(Gravity.CENTER);
        TextView l = text(label, 13.5f, TXT, true); l.setGravity(Gravity.CENTER);
        l.setPadding(0, dp(6), 0, 0);
        t.addView(e); t.addView(l);
        t.setOnClickListener(v -> onTap.run());
        return t;
    }

    LinearLayout tileRow(View a, View b) {
        LinearLayout row = hbox();
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, -2, 1f); l.rightMargin = dp(5);
        LinearLayout.LayoutParams r = new LinearLayout.LayoutParams(0, -2, 1f); r.leftMargin = dp(5);
        row.addView(a, l); row.addView(b, r);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    // ---- Saját sablonok ----

    void refreshTemplates() {
        if (templatesBox == null) return;
        templatesBox.removeAllViews();
        java.util.List<Workouts.W> list = Workouts.load(this);
        if (list.isEmpty()) return;
        TextView title = text("Saját sablonok", 13, MUTED, true);
        title.setPadding(dp(2), 0, 0, dp(8));
        templatesBox.addView(title);
        LinearLayout cardT = card();
        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            final Workouts.W w = list.get(i);
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(8), dp(12));
            row.setClickable(true);
            row.setOnClickListener(v -> loadTemplate(w));
            LinearLayout left = vbox();
            left.addView(text(w.name, 15.5f, TXT, true));
            left.addView(text(w.work + "/" + w.rest + " mp · " + w.rounds + "× · előkész. " + w.prep + " mp", 12, MUTED, false));
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
            Button del = new Button(this);
            del.setText("🗑"); del.setAllCaps(false); del.setTextSize(16);
            del.setBackground(null); del.setStateListAnimator(null); del.setTextColor(MUTED);
            del.setOnClickListener(v -> new Sheet(this, "Sablon törlése", "Törlöd a(z) „" + w.name + "\" sablont?")
                    .addDestructive("Törlés", () -> { Workouts.removeAt(this, idx); refreshTemplates(); })
                    .addCancel().show());
            row.addView(del);
            cardT.addView(row);
            if (i < list.size() - 1) cardT.addView(divider());
        }
        templatesBox.addView(cardT, new LinearLayout.LayoutParams(-1, -2));
        templatesBox.addView(gap(10));
    }

    void loadTemplate(Workouts.W w) {
        cfg[PREP_K] = w.prep; cfg[WORK_K] = w.work; cfg[REST_K] = w.rest; cfg[ROUND_K] = w.rounds;
        saveAll(); refreshValues(); updateTotal(); vibrateShort();
    }

    void saveTemplateDialog() {
        final EditText et = sheetInput("Sablon neve", false);
        et.setText("Saját edzés");
        et.setSelectAllOnFocus(true);
        LinearLayout box = vbox();
        box.setPadding(dp(6), dp(2), dp(6), dp(4));
        box.addView(et);
        String summary = "⏱ " + cfg[WORK_K] + "/" + cfg[REST_K] + " mp · " + cfg[ROUND_K] + " kör · előkész. " + cfg[PREP_K] + " mp";
        new Sheet(this, "Sablon mentése", summary)
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) name = "Saját edzés";
                    Workouts.add(this, new Workouts.W(name, cfg[PREP_K], cfg[WORK_K], cfg[REST_K], cfg[ROUND_K]));
                    refreshTemplates();
                })
                .addCancel()
                .show();
    }

    // ---- Heti cél ----

    static final String[] GOAL_UNITS = {"edzés", "perc", "km"};

    void refreshGoal() {
        if (goalBox == null) return;
        goalBox.removeAllViews();
        int mode = prefs.getInt("wg_mode", 0);
        int target = prefs.getInt("wg_target", 0);
        LinearLayout cardG = card();
        cardG.setClickable(true);
        cardG.setOnClickListener(v -> goalDialog());

        if (target <= 0) {
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(18), dp(14), dp(18), dp(14));
            LinearLayout left = vbox();
            left.addView(text("🎯 Heti cél", 15.5f, TXT, true));
            left.addView(text("Tűzz ki célt: heti edzésszám, perc vagy km", 12, MUTED, false));
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text("▸", 15, MUTED, false));
            cardG.addView(row);
        } else {
            double done = weekProgress(mode);
            float frac = (float) Math.min(1.0, done / target);
            LinearLayout inner = vbox();
            inner.setPadding(dp(18), dp(14), dp(18), dp(14));
            LinearLayout top = hbox();
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(text("🎯 Heti cél", 15.5f, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
            String doneS = mode == 2 ? String.format(Locale.US, "%.1f", done) : String.valueOf((int) done);
            top.addView(text(doneS + " / " + target + " " + GOAL_UNITS[mode], 14, tAccent, true));
            inner.addView(top);
            inner.addView(gap(10));

            // Folyamatjelző sáv
            LinearLayout barBg = hbox();
            GradientDrawable bgd = new GradientDrawable();
            bgd.setColor(CARD2); bgd.setCornerRadius(dp(6));
            barBg.setBackground(bgd);
            View fill = new View(this);
            GradientDrawable fgd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{tAccent, tAccent2});
            fgd.setCornerRadius(dp(6));
            fill.setBackground(fgd);
            barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(12), Math.max(0.001f, frac)));
            barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(12), 1f - Math.max(0.001f, frac)));
            inner.addView(barBg, new LinearLayout.LayoutParams(-1, -2));
            inner.addView(gap(8));

            String sub;
            if (frac >= 1f) sub = "Kész! Teljesítetted a heti célod 🎉";
            else {
                double left2 = target - done;
                String leftS = mode == 2 ? String.format(Locale.US, "%.1f", left2) : String.valueOf((int) Math.ceil(left2));
                sub = Math.round(frac * 100) + "% · még " + leftS + " " + GOAL_UNITS[mode] + " a célig";
            }
            inner.addView(text(sub, 12, MUTED, false));
            cardG.addView(inner);
        }
        goalBox.addView(cardG, new LinearLayout.LayoutParams(-1, -2));
        goalBox.addView(gap(14));
    }

    /** E heti teljesítés a naplóból: edzésszám / perc / km, a mód szerint. */
    double weekProgress(int mode) {
        long from = weekStartMs();
        JSONArray arr = History.load(this);
        double v = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null || o.optLong("ts") < from) continue;
            if (mode == 1) v += o.optInt("dur") / 60.0;
            else if (mode == 2) { double d = o.optDouble("dist", -1); if (d > 0) v += d / 1000.0; }
            else v += 1;
        }
        return v;
    }

    long weekStartMs() { return weekStartOf(System.currentTimeMillis()); }

    long weekStartOf(long ts) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        c.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    long prevWeekOf(long ws) { return weekStartOf(ws - 3L * 24 * 3600 * 1000); }

    int weekStreak(JSONArray arr) {
        java.util.HashSet<Long> weeks = new java.util.HashSet<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) weeks.add(weekStartOf(o.optLong("ts")));
        }
        long wk = weekStartMs();
        if (!weeks.contains(wk)) wk = prevWeekOf(wk);
        int s = 0;
        while (weeks.contains(wk)) { s++; wk = prevWeekOf(wk); }
        return s;
    }

    // ---- Haladás-csík (szint / sorozat / edzésszám) ----

    void refreshProgress() {
        if (progressBox == null) return;
        progressBox.removeAllViews();
        JSONArray arr = History.load(this);
        long xp = Levels.totalXp(arr);
        int lvl = Levels.levelForXp(xp);
        progressBox.addView(progressChip("⭐", "Szint " + lvl, Levels.title(lvl)), progChipLp());
        progressBox.addView(progressChip("🔥", weekStreak(arr) + " hét", "sorozat"), progChipLp());
        progressBox.addView(progressChip("🏁", String.valueOf(arr.length()), "edzés"), progChipLp());
    }

    View progressChip(String emoji, String value, String label) {
        LinearLayout c = vbox();
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(6), dp(12), dp(6), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GLASS); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1), GLASS_LINE);
        c.setBackground(bg);
        TextView v = text(emoji + " " + value, 15, TXT, true); v.setGravity(Gravity.CENTER);
        TextView l = text(label, 11, MUTED, false); l.setGravity(Gravity.CENTER);
        c.addView(v); c.addView(l);
        return c;
    }

    LinearLayout.LayoutParams progChipLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.leftMargin = dp(4); p.rightMargin = dp(4);
        return p;
    }

    // ---- Edzés-felépítés sáv ----

    void refreshPlanBar() {
        if (planBar == null) return;
        planBar.removeAllViews();
        Programs.P p = Programs.byName(this, programName);
        int len = p == null ? 1 : p.ex.length;
        int n = cfg[ROUND_K] * len;
        int warm = cfg[WARM_K], prep = cfg[PREP_K], work = cfg[WORK_K] * n,
                rest = cfg[REST_K] * Math.max(0, n - 1), cool = cfg[COOL_K];
        int total = warm + prep + work + rest + cool;
        if (total <= 0) { if (planCaption != null) planCaption.setText(""); return; }
        addSeg(warm, PREP);
        addSeg(prep, 0xFF8AA0C4);
        addSeg(work, tWork);
        addSeg(rest, tRest);
        addSeg(cool, 0xFF22FFC2);
        int wp = Math.round(work * 100f / total), rp = Math.round(rest * 100f / total);
        String cap = "🏃 Munka " + wp + "%";
        if (rest > 0) cap += "    💤 Pihenő " + rp + "%";
        if (warm > 0 || cool > 0) cap += "    ➕ bemelegítés/levezetés";
        if (planCaption != null) planCaption.setText(cap);
    }

    void addSeg(int secs, int color) {
        if (secs <= 0) return;
        View v = new View(this);
        v.setBackgroundColor(color);
        planBar.addView(v, new LinearLayout.LayoutParams(0, -1, secs));
    }

    void goalDialog() {
        final int[] mode = {prefs.getInt("wg_mode", 0)};
        int target = prefs.getInt("wg_target", 0);

        LinearLayout box = vbox();
        box.setPadding(dp(6), dp(2), dp(6), dp(4));
        TextView lbl1 = text("Mit mérjen a cél?", 13, MUTED, false);
        lbl1.setPadding(dp(2), 0, 0, dp(8));
        box.addView(lbl1);
        LinearLayout modes = hbox();
        final Button[] mb = new Button[3];
        String[] mn = {"Edzés (db)", "Idő (perc)", "Táv (km)"};
        for (int i = 0; i < 3; i++) {
            final int mi = i;
            Button bb = new Button(this);
            bb.setText(mn[i]); bb.setAllCaps(false); bb.setTextSize(12.5f);
            bb.setTypeface(null, Typeface.BOLD);
            bb.setStateListAnimator(null);
            bb.setPadding(dp(4), dp(10), dp(4), dp(10));
            styleGoalChip(bb, mi == mode[0]);
            bb.setOnClickListener(v -> {
                mode[0] = mi;
                for (int j = 0; j < 3; j++) styleGoalChip(mb[j], j == mi);
            });
            mb[i] = bb;
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
            clp.leftMargin = dp(3); clp.rightMargin = dp(3);
            modes.addView(bb, clp);
        }
        box.addView(modes, new LinearLayout.LayoutParams(-1, -2));
        box.addView(gap(12));
        TextView lbl2 = text("Heti célérték", 13, MUTED, false);
        lbl2.setPadding(dp(2), 0, 0, dp(6));
        box.addView(lbl2);
        final EditText et = sheetInput("pl. 3", false);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (target > 0) { et.setText(String.valueOf(target)); et.setSelectAllOnFocus(true); }
        box.addView(et);

        Sheet sheet = new Sheet(this, "Heti cél", "Tűzz ki heti célt, és kövesd a haladást");
        sheet.addCustom(box);
        sheet.addPrimary("Mentés", () -> {
            try {
                int v = Integer.parseInt(et.getText().toString().trim());
                if (v > 0) {
                    prefs.edit().putInt("wg_mode", mode[0]).putInt("wg_target", v).apply();
                    refreshGoal();
                }
            } catch (Exception ignored) {}
        });
        if (target > 0) {
            sheet.addDestructive("Cél törlése", () -> {
                prefs.edit().putInt("wg_target", 0).apply();
                refreshGoal();
            });
        }
        sheet.addCancel();
        sheet.show();
    }

    // ---- Edzésprogramok ----

    void updateProgramUI() {
        Programs.P p = Programs.byName(this, programName);
        if (p == null) {
            programName = "";
            if (programLabel != null) programLabel.setText("🏃 Futás");
            if (programPreview != null) programPreview.setVisibility(View.GONE);
            if (workRowTitle != null) workRowTitle.setText("Futás");
            if (workRowSub != null) workRowSub.setText("Aktív időszak hossza");
        } else {
            if (programLabel != null) programLabel.setText(p.title());
            if (programPreview != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < p.ex.length; i++) {
                    if (i > 0) sb.append("  ·  ");
                    sb.append(p.ex[i]);
                }
                if (p.custom) sb.append("\n(A saját programot hosszan nyomva törölheted.)");
                programPreview.setText(sb.toString());
                programPreview.setVisibility(View.VISIBLE);
            }
            if (workRowTitle != null) workRowTitle.setText("Gyakorlat");
            if (workRowSub != null) workRowSub.setText("Egy gyakorlat hossza");
        }
        updateTotal();
    }

    void chooseProgram() {
        final java.util.List<Programs.P> all = Programs.all(this);
        Sheet sheet = new Sheet(this, "Edzés típusa", "Futás vagy gyakorlatsor körökben");
        sheet.addRow("🏃", "Futás (intervallum)", "Klasszikus futás/pihenő ismétlés",
                programName == null || programName.isEmpty(), true,
                () -> { programName = ""; saveProgram(); });
        for (Programs.P p : all) {
            final String pn = p.name;
            StringBuilder sub = new StringBuilder(p.ex.length + " gyakorlat · ");
            for (int j = 0; j < p.ex.length && j < 3; j++) sub.append(j > 0 ? ", " : "").append(p.ex[j]);
            if (p.ex.length > 3) sub.append("…");
            sheet.addRow(p.emoji, p.name, sub.toString(), pn.equals(programName), true,
                    () -> { programName = pn; saveProgram(); });
        }
        sheet.addPrimary("➕  Új saját program", this::newProgramDialog);
        sheet.addNeutral("📖  Gyakorlat-könyvtár", () -> startActivity(new Intent(this, LibraryActivity.class)));
        sheet.addCancel();
        sheet.show();
    }

    void saveProgram() {
        prefs.edit().putString("progname", programName).apply();
        updateProgramUI();
        vibrateShort();
    }

    void newProgramDialog() {
        LinearLayout box = vbox();
        box.setPadding(dp(6), dp(2), dp(6), dp(4));
        final EditText name = sheetInput("Program neve (pl. Reggeli torna)", false);
        box.addView(name);
        box.addView(gap(12));
        TextView lbl = text("Gyakorlatok – soronként egy:", 13, MUTED, false);
        lbl.setPadding(dp(4), 0, 0, dp(6));
        box.addView(lbl);
        final EditText exs = sheetInput("Plank\nHasprés\nGuggolás", true);
        exs.setMinLines(4);
        exs.setGravity(Gravity.TOP);
        box.addView(exs);

        new Sheet(this, "Új saját program")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    String n = name.getText().toString().trim();
                    java.util.ArrayList<String> list = new java.util.ArrayList<>();
                    for (String line : exs.getText().toString().split("\n")) {
                        String t = line.trim();
                        if (!t.isEmpty()) list.add(t);
                    }
                    if (list.isEmpty()) {
                        Toast.makeText(this, "Adj meg legalább egy gyakorlatot.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (n.isEmpty()) n = "Saját program";
                    Programs.addCustom(this, n, list.toArray(new String[0]));
                    programName = n;
                    saveProgram();
                })
                .addCancel()
                .show();
    }

    /** Egységes stílusú beviteli mező a lapokhoz (sötét, lekerekített üveg). */
    EditText sheetInput(String hint, boolean multiline) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(TXT);
        et.setHintTextColor(MUTED);
        et.setBackgroundColor(0x00000000);
        int type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        if (multiline) type |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        et.setInputType(type);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x1AFFFFFF);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), GLASS_LINE);
        et.setBackground(bg);
        return et;
    }

    boolean maybeDeleteCustomProgram() {
        final Programs.P p = Programs.byName(this, programName);
        if (p == null || !p.custom) return false;
        new Sheet(this, "Program törlése", "Törlöd a(z) „" + p.name + "\" saját programot?")
                .addDestructive("Törlés", () -> {
                    Programs.removeCustom(this, p.name);
                    programName = "";
                    saveProgram();
                })
                .addCancel()
                .show();
        return true;
    }

    void styleGoalChip(Button b, boolean sel) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (sel) { bg.setColor(tAccent); b.setTextColor(0xFFFFFFFF); }
        else { bg.setColor(CARD2); bg.setStroke(dp(1), LINE); b.setTextColor(TXT); }
        b.setBackground(bg);
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
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GLASS2);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), GLASS_LINE);
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
        row.setPadding(dp(18), dp(17), dp(18), dp(17));

        LinearLayout labels = vbox();
        TextView titleTv = text(title, 15.5f, TXT, true);
        TextView subTv = text(sub, 12, MUTED, false);
        labels.addView(titleTv);
        labels.addView(subTv);
        if (key == WORK_K) { workRowTitle = titleTv; workRowSub = subTv; }
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
        valLp.leftMargin = dp(10);
        valLp.rightMargin = dp(10);

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
        final EditText et = sheetInput("", false);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(cfg[key]));
        et.setSelectAllOnFocus(true);
        LinearLayout box = vbox();
        box.setPadding(dp(6), dp(2), dp(6), dp(4));
        box.addView(et);
        new Sheet(this, title, "Írd be a pontos értéket (" + min + "–" + max + ")")
                .addCustom(box)
                .addPrimary("OK", () -> {
                    try {
                        int v = Math.max(min, Math.min(max, Integer.parseInt(et.getText().toString().trim())));
                        cfg[key] = v;
                        valueLabels[key].setText(String.valueOf(v));
                        prefs.edit().putInt("k" + key, v).apply();
                        updateTotal();
                    } catch (Exception ignored) {}
                })
                .addCancel()
                .show();
    }

    LinearLayout navRow(String title, String sub, TextView valueOut, Runnable onTap) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(17), dp(18), dp(17));
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
        row.setPadding(dp(18), dp(14), dp(18), dp(14));
        LinearLayout labels = vbox();
        labels.addView(text(title, 15.5f, TXT, true));
        if (sub != null) labels.addView(text(sub, 12, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(sw);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    void chooseSound(final boolean forWork) {
        int cur = forWork ? workSoundIdx : restSoundIdx;
        final Sheet sheet = new Sheet(this, forWork ? "Futás hangja" : "Pihenő hangja",
                "Koppints a meghallgatáshoz");
        for (int i = 0; i < Beeper.SOUNDS.length; i++) {
            final int which = i;
            sheet.addRow("🔊", Beeper.SOUNDS[i].name, null, i == cur, false, () -> {
                Beeper.play(which);
                if (forWork) { workSoundIdx = which; prefs.edit().putInt("ws", which).apply(); }
                else { restSoundIdx = which; prefs.edit().putInt("rs", which).apply(); }
                updateSoundLabels();
                sheet.selectOnly(which);
            });
        }
        sheet.addPrimary("Kész", null);
        sheet.show();
    }

    void updateSoundLabels() {
        if (workSoundLabel != null) workSoundLabel.setText(Beeper.soundAt(workSoundIdx).name);
        if (restSoundLabel != null) restSoundLabel.setText(Beeper.soundAt(restSoundIdx).name);
    }

    void refreshValues() {
        for (int i = 0; i < 6; i++) if (valueLabels[i] != null) valueLabels[i].setText(String.valueOf(cfg[i]));
    }

    void saveAll() {
        SharedPreferences.Editor e = prefs.edit();
        for (int i = 0; i < 6; i++) e.putInt("k" + i, cfg[i]);
        e.apply();
    }

    void updateTotal() {
        Programs.P p = Programs.byName(this, programName);
        int len = p == null ? 1 : p.ex.length;
        int n = cfg[ROUND_K] * len; // összes munka-szakasz
        int total = cfg[WARM_K] + cfg[PREP_K] + cfg[WORK_K] * n
                + cfg[REST_K] * Math.max(0, n - 1) + cfg[COOL_K];
        String s = "Teljes idő: " + fmtLong(total);
        if (len > 1) s += "  ·  " + len + " gyakorlat × " + cfg[ROUND_K] + " kör";
        totalText.setText(s);
        refreshPlanBar();
    }

    // ================= RUN =================

    View buildRun() {
        runView = vbox();
        runView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        runView.setGravity(Gravity.CENTER);
        runView.setPadding(dp(22), dp(22), dp(22), dp(28));
        // Az edzés-képernyő majdnem áttetsző sötét fátyol – a háttérkép finoman átdereng,
        // de a fókusz a körgyűrűn marad.
        GradientDrawable runBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF2070912, 0xF7060912});
        runView.setBackground(runBg);

        phaseLabel = text("FUTÁS", 15, MUTED, true);
        phaseLabel.setGravity(Gravity.CENTER);
        phaseLabel.setLetterSpacing(0.22f);
        runView.addView(phaseLabel);
        exText = text("", 21, TXT, true);
        exText.setGravity(Gravity.CENTER);
        exText.setVisibility(View.GONE);
        exText.setPadding(0, dp(6), 0, 0);
        runView.addView(exText);
        exDesc = text("", 12.5f, MUTED, false);
        exDesc.setGravity(Gravity.CENTER);
        exDesc.setVisibility(View.GONE);
        exDesc.setPadding(dp(12), dp(4), dp(12), 0);
        runView.addView(exDesc);
        runView.addView(gap(12));

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
        runView.addView(gap(16));

        distanceText = text("", 15, tAccent, true);
        distanceText.setGravity(Gravity.CENTER);
        runView.addView(distanceText);
        nextText = text("", 13, MUTED, false);
        nextText.setGravity(Gravity.CENTER);
        runView.addView(nextText);
        runView.addView(gap(12));

        // Rekord-jelvény (befejezéskor, ha új csúcs)
        recordText = text("", 14, 0xFF0B0B0B, true);
        recordText.setGravity(Gravity.CENTER);
        recordText.setPadding(dp(16), dp(9), dp(16), dp(9));
        GradientDrawable rbg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFFD24D, 0xFFFFB020});
        rbg.setCornerRadius(dp(22));
        recordText.setBackground(rbg);
        recordText.setVisibility(View.GONE);
        LinearLayout recWrap = hbox();
        recWrap.setGravity(Gravity.CENTER);
        recWrap.addView(recordText);
        runView.addView(recWrap);
        runView.addView(gap(8));

        // Szintlépés-jelvény (befejezéskor, ha új szint)
        levelText = text("", 14, 0xFFFFFFFF, true);
        levelText.setGravity(Gravity.CENTER);
        levelText.setPadding(dp(16), dp(9), dp(16), dp(9));
        GradientDrawable lbg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{tAccent, tAccent2});
        lbg.setCornerRadius(dp(22));
        levelText.setBackground(lbg);
        levelText.setVisibility(View.GONE);
        LinearLayout lvlWrap = hbox();
        lvlWrap.setGravity(Gravity.CENTER);
        lvlWrap.addView(levelText);
        runView.addView(lvlWrap);
        runView.addView(gap(12));

        // Élő statisztikák: eltelt idő, kalória, lépések
        LinearLayout stats = hbox();
        statElapsed = statCell(stats, "Eltelt");
        statCal = statCell(stats, "Kalória");
        statSteps = statCell(stats, "Lépés");
        stats.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        runView.addView(stats);
        runView.addView(gap(22));

        // Idő-módosítás menet közben
        LinearLayout adj = hbox();
        adj.setGravity(Gravity.CENTER);
        Button minus15 = timeAdjBtn("−15 mp");
        Button plus15 = timeAdjBtn("+15 mp");
        minus15.setOnClickListener(v -> { if (!finished) addTime(-15); });
        plus15.setOnClickListener(v -> { if (!finished) addTime(15); });
        LinearLayout.LayoutParams amp = new LinearLayout.LayoutParams(-2, -2);
        amp.rightMargin = dp(10);
        adj.addView(minus15, amp);
        adj.addView(plus15);
        runView.addView(adj);
        runView.addView(gap(14));

        LinearLayout controls = hbox();
        pauseBtn = ghostButton("Szünet");
        Button skip = ghostButton("⏭");
        Button stop = ghostButton("Leállítás");
        pauseBtn.setOnClickListener(v -> {
            if (lastPaused) cmd(TimerService.ACTION_RESUME);
            else cmd(TimerService.ACTION_PAUSE);
        });
        skip.setOnClickListener(v -> { if (!finished) cmd(TimerService.ACTION_SKIP); });
        stop.setOnClickListener(v -> confirmStop());
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp1.rightMargin = dp(8);
        LinearLayout.LayoutParams lpS = new LinearLayout.LayoutParams(dp(64), -2);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp2.leftMargin = dp(8);
        controls.addView(pauseBtn, lp1);
        controls.addView(skip, lpS);
        controls.addView(stop, lp2);
        controls.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        runView.addView(controls);

        return runView;
    }

    TextView statCell(LinearLayout parent, String label) {
        LinearLayout cell = vbox();
        cell.setGravity(Gravity.CENTER);
        TextView val = text("—", 18, TXT, true);
        val.setGravity(Gravity.CENTER);
        TextView lab = text(label, 11.5f, MUTED, false);
        lab.setGravity(Gravity.CENTER);
        cell.addView(val);
        cell.addView(lab);
        parent.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        return val;
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
        i.putExtra(TimerService.EX_WARM, cfg[WARM_K]);
        i.putExtra(TimerService.EX_COOL, cfg[COOL_K]);
        i.putExtra(TimerService.EX_WS, workSoundIdx);
        i.putExtra(TimerService.EX_RS, restSoundIdx);
        i.putExtra(TimerService.EX_TRACK, trackDistance && hasLocationPermission());
        i.putExtra(TimerService.EX_CD, precount ? Theme.countdownSecs(this) : 0);
        i.putExtra(TimerService.EX_VIBE, Theme.vibrate(this));
        i.putExtra(TimerService.EX_VOICE, voice);
        Programs.P prog = Programs.byName(this, programName);
        if (prog != null) {
            i.putExtra(TimerService.EX_NAMES, prog.ex);
            i.putExtra(TimerService.EX_PNAME, prog.name);
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);

        // Kezdeti UI, a további frissítés a broadcast-okból jön.
        lastPaused = false;
        finished = false;
        pauseBtn.setEnabled(true);
        pauseBtn.setText("Szünet");
        int firstPhase = cfg[WARM_K] > 0 ? TimerService.T_WARMUP
                : cfg[PREP_K] > 0 ? TimerService.T_PREP : TimerService.T_WORK;
        int firstDur = cfg[WARM_K] > 0 ? cfg[WARM_K]
                : cfg[PREP_K] > 0 ? cfg[PREP_K] : cfg[WORK_K];
        setPhaseUI(firstPhase, 1);
        timeText.setText(fmt(firstDur));
        ring.setProgress(1f);
        distanceText.setText(trackDistance && hasLocationPermission() ? "📍 0 m" : "");
        exText.setVisibility(View.GONE);
        exDesc.setVisibility(View.GONE);
        nextText.setText(prog != null && prog.ex.length > 0 ? "Következő: " + prog.ex[0] : "");
        recordText.setVisibility(View.GONE);
        levelText.setVisibility(View.GONE);
        showRun(true);
    }

    void cmd(String action) {
        startService(new Intent(this, TimerService.class).setAction(action));
    }

    void addTime(int deltaSec) {
        startService(new Intent(this, TimerService.class)
                .setAction(TimerService.ACTION_ADD_TIME)
                .putExtra(TimerService.EX_DELTA, deltaSec));
    }

    /** Leállításkor: ha az edzés még nem fejeződött be, rákérdez a mentésre. */
    void confirmStop() {
        if (finished) { cmd(TimerService.ACTION_STOP); showRun(false); return; }
        new Sheet(this, "Edzés leállítása", "Az edzés még nem fejeződött be. Mented a naplóba?")
                .addPrimary("💾  Mentés a naplóba", () -> { cmd(TimerService.ACTION_STOP_SAVE); showRun(false); })
                .addDestructive("Elvetés mentés nélkül", () -> { cmd(TimerService.ACTION_STOP); showRun(false); })
                .addCancel()
                .show();
    }

    @Override
    public void onBackPressed() {
        if (runView.getVisibility() == View.VISIBLE) confirmStop();
        else super.onBackPressed();
    }

    // ================= Broadcast fogadás =================

    final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            if (TimerService.B_TICK.equals(a)) onTick(i);
            else if (TimerService.B_DONE.equals(a)) onDone(i);
            else if (TimerService.B_STOPPED.equals(a)) { showRun(false); refreshGoal(); refreshProgress(); }
        }
    };

    void onTick(Intent i) {
        int phase = i.getIntExtra(TimerService.EX_PHASE, TimerService.T_WORK);
        int remain = i.getIntExtra(TimerService.EX_REMAIN, 0);
        int round = i.getIntExtra(TimerService.EX_ROUND, 1);
        int rounds = i.getIntExtra(TimerService.EX_ROUNDS, cfg[ROUND_K]);
        float prog = i.getFloatExtra(TimerService.EX_PROGRESS, 0);
        double dist = i.getDoubleExtra(TimerService.EX_DIST, -1);
        float speed = i.getFloatExtra(TimerService.EX_SPEED, 0);
        boolean paused = i.getBooleanExtra(TimerService.EX_PAUSED, false);

        showRun(true);
        finished = false;
        setPhaseUI(phase, round, rounds);
        // Gyakorlatsor: a gyakorlat neve nagyban + a következő kijelzése
        String stepName = i.getStringExtra(TimerService.EX_STEPNAME);
        String nextName = i.getStringExtra(TimerService.EX_NEXTNAME);
        if (stepName != null && phase == TimerService.T_WORK) {
            phaseLabel.setText("GYAKORLAT");
            exText.setText(stepName);
            exText.setVisibility(View.VISIBLE);
            String desc = Programs.descOf(stepName);
            exDesc.setText(desc);
            exDesc.setVisibility(desc.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            exText.setVisibility(View.GONE);
            exDesc.setVisibility(View.GONE);
        }
        nextText.setText(nextName != null ? "Következő: " + nextName : "");
        timeText.setText(fmt(remain));
        ring.setProgress(prog);
        distanceText.setText(dist >= 0
                ? "📍 " + fmtDist(dist) + "   ·   " + fmtSpeed(speed)
                : "");

        int elapsed = i.getIntExtra(TimerService.EX_ELAPSED, 0);
        int steps = i.getIntExtra(TimerService.EX_STEPS, 0);
        int cal = i.getIntExtra(TimerService.EX_CAL, 0);
        statElapsed.setText(fmtLong(elapsed));
        statCal.setText(cal + " kcal");
        statSteps.setText(steps > 0 ? String.valueOf(steps) : "—");

        lastPaused = paused;
        pauseBtn.setText(paused ? "Folytatás" : "Szünet");
        if (paused) {
            String base = stepName != null && phase == TimerService.T_WORK ? "GYAKORLAT" : phaseName(phase);
            phaseLabel.setText(base + " · SZÜNET");
        }
    }

    void onDone(Intent i) {
        int dur = i.getIntExtra(TimerService.EX_DUR, 0);
        double dist = i.getDoubleExtra(TimerService.EX_DIST, -1);
        int rounds = i.getIntExtra(TimerService.EX_ROUND, cfg[ROUND_K]);
        int cal = i.getIntExtra(TimerService.EX_CAL, 0);
        int steps = i.getIntExtra(TimerService.EX_STEPS, 0);
        float avg = i.getFloatExtra(TimerService.EX_SPEED, -1);
        showRun(true);
        finished = true;
        phaseLabel.setText("KÉSZ");
        phaseLabel.setTextColor(DONE);
        exText.setVisibility(View.GONE);
        exDesc.setVisibility(View.GONE);
        nextText.setText("Elmentve a naplóba ✔");
        ring.setColor(DONE);
        ring.setProgress(1f);
        timeText.setText("✓");
        roundInfo.setText(rounds + " kör kész 💪");
        String line = "Idő: " + fmtLong(dur);
        if (dist >= 0) line += "  ·  📍 " + fmtDist(dist);
        if (avg > 0) line += "  ·  ⌀ " + fmtSpeed(avg);
        distanceText.setText(line);
        statElapsed.setText(fmtLong(dur));
        statCal.setText(cal + " kcal");
        statSteps.setText(steps > 0 ? String.valueOf(steps) : "—");

        String records = i.getStringExtra(TimerService.EX_RECORDS);
        if (records != null && !records.isEmpty()) {
            recordText.setText(records.equals("első edzés")
                    ? "🎉 Első edzés a naplóban!"
                    : "🏆 Új rekord: " + records);
            recordText.setVisibility(View.VISIBLE);
            recordText.setScaleX(0.7f); recordText.setScaleY(0.7f); recordText.setAlpha(0f);
            recordText.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(430)
                    .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
        } else {
            recordText.setVisibility(View.GONE);
        }

        String levelup = i.getStringExtra(TimerService.EX_LEVELUP);
        if (levelup != null && !levelup.isEmpty()) {
            String[] parts = levelup.split("\\|");
            String lvlLabel = parts.length == 2 ? "Szint " + parts[0] + " · " + parts[1] : parts[0];
            levelText.setText("⭐ Új szint! " + lvlLabel);
            levelText.setVisibility(View.VISIBLE);
            levelText.setScaleX(0.7f); levelText.setScaleY(0.7f); levelText.setAlpha(0f);
            levelText.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(250).setDuration(430)
                    .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
        } else {
            levelText.setVisibility(View.GONE);
        }

        lastPaused = false;
        pauseBtn.setEnabled(false);
        pauseBtn.setText("Kész");
        refreshGoal();
        refreshProgress();
    }

    void setPhaseUI(int phase, int round) { setPhaseUI(phase, round, cfg[ROUND_K]); }

    void setPhaseUI(int phase, int round, int rounds) {
        int color = phase == TimerService.T_PREP ? PREP
                : phase == TimerService.T_WORK ? tWork
                : phase == TimerService.T_WARMUP ? PREP
                : phase == TimerService.T_COOLDOWN ? tRest
                : tRest;
        phaseLabel.setText(phaseName(phase));
        phaseLabel.setTextColor(color);
        ring.setColor(color);
        boolean showRounds = phase == TimerService.T_WORK || phase == TimerService.T_REST || phase == TimerService.T_PREP;
        roundInfo.setText(showRounds ? "Kör " + Math.max(1, round) + " / " + rounds : "");
    }

    String phaseName(int phase) {
        return phase == TimerService.T_PREP ? "ELŐKÉSZÜLÉS"
                : phase == TimerService.T_WORK ? "FUTÁS"
                : phase == TimerService.T_WARMUP ? "BEMELEGÍTÉS"
                : phase == TimerService.T_COOLDOWN ? "LEVEZETÉS"
                : "PIHENŐ";
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ha a Beállításokban változott a téma, építsük újra (kivéve edzés közben).
        if (Theme.rev(this) != builtRev && (runView == null || runView.getVisibility() != View.VISIBLE)) {
            recreate();
            return;
        }
        if (!receiverRegistered) {
            IntentFilter f = new IntentFilter();
            f.addAction(TimerService.B_TICK);
            f.addAction(TimerService.B_DONE);
            f.addAction(TimerService.B_STOPPED);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(rx, f);
            receiverRegistered = true;
        }
        refreshGoal();
        refreshProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            try { unregisterReceiver(rx); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
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
        lp.leftMargin = dp(16);
        lp.rightMargin = dp(16);
        v.setLayoutParams(lp);
        v.setBackgroundColor(LINE);
        return v;
    }

    Button primaryButton(String label) {
        Button b = baseButton(label);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{tAccent, tAccent2});
        bg.setCornerRadius(dp(18));
        b.setBackground(bg);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(18);
        return b;
    }

    Button timeAdjBtn(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TXT);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextSize(14);
        b.setStateListAnimator(null);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setPadding(dp(18), dp(10), dp(18), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), LINE);
        b.setBackground(bg);
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
        b.setPadding(dp(18), dp(18), dp(18), dp(18));
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

    /** Sebesség km/h-ban vagy tempóként (perc/km), a beállítás szerint. */
    String fmtSpeed(double kmh) {
        if (tPace) {
            if (kmh <= 0.3) return "–:– /km";
            double pace = 60.0 / kmh;
            int m = (int) pace;
            int s = (int) Math.round((pace - m) * 60);
            if (s == 60) { m++; s = 0; }
            return String.format(Locale.US, "%d:%02d /km", m, s);
        }
        return String.format(Locale.US, "%.1f km/h", kmh);
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
