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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
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

    // Színek – „Grit" karmazsin arculat meleg szénfekete alapon.
    // A paletta futásidőben vált sötét ↔ világos között (lásd applyPalette()).
    static int BG = 0xFF0C0A0B, CARD = 0xFF171214, CARD2 = 0xFF221619;
    static int TXT = 0xFFF5ECEE, MUTED = 0xFFA98F95, LINE = 0xFF3A2A2E;
    static final int ACCENT = 0xFFE11D2E;
    static final int INDIGO = 0xFFFF4757, VIOLET = 0xFFB0142A; // márka-gradiens: skarlát → mély karmazsin
    static final int WORK = 0xFFE11D2E, REST = 0xFF14B8A6, PREP = 0xFFFFC24D, DONE = 0xFFFF4757;
    // Áttetsző „üveg" felületek – a generált háttérkép finoman átüt rajtuk
    static int GLASS = 0xE6171214, GLASS2 = 0xD9221619, GLASS_LINE = 0x33FFFFFF;

    /** A világos/sötét paletta beállítása. Minden képernyő ezt hívja az onCreate elején. */
    static void applyPalette(Context c) {
        if (Theme.light(c)) {
            BG = 0xFFF7F3F4; CARD = 0xFFFFFFFF; CARD2 = 0xFFF1EAEC;
            TXT = 0xFF1E1416; MUTED = 0xFF75636A; LINE = 0xFFE3D8DC;
            GLASS = 0xF7FFFFFF; GLASS2 = 0xF2FFFFFF; GLASS_LINE = 0x1F000000;
        } else {
            BG = 0xFF0C0A0B; CARD = 0xFF171214; CARD2 = 0xFF221619;
            TXT = 0xFFF5ECEE; MUTED = 0xFFA98F95; LINE = 0xFF3A2A2E;
            GLASS = 0xE6171214; GLASS2 = 0xD9221619; GLASS_LINE = 0x33FFFFFF;
        }
    }

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
    ScrollView runScroll;
    LinearLayout templatesBox;
    LinearLayout goalBox;
    LinearLayout gridBox;
    LinearLayout sectionsBox;
    LinearLayout insightBox;
    LinearLayout challengeBox;
    java.util.LinkedHashMap<String, View> sectionViews;
    LinearLayout progressBox;
    LinearLayout levelBar;
    LinearLayout planBar;
    TextView planCaption;
    TextView bannerSub;
    TextView streakChip;
    LinearLayout recentBox;
    LinearLayout badgesBox;
    final View[] presetViews = new View[4];
    final int[][] presetSpecs = {{30, 10, 8}, {60, 20, 6}, {20, 10, 8}, {20, 40, 6}};
    final int[] presetColors = {0xFFFF6B6B, 0xFF5FD0FF, 0xFFFFA24B, 0xFF7FE1A6};
    LinearLayout weekBox;
    LinearLayout recordsBox;
    TextView totalText;
    final TextView[] valueLabels = new TextView[6];
    TextView workSoundLabel, restSoundLabel;
    TextView programLabel, programPreview, workRowTitle, workRowSub;
    LinearLayout programCard;
    Switch distanceSwitch, precountSwitch, voiceSwitch;
    TextView phaseLabel, timeText, roundInfo, distanceText;
    TextView exText, exDesc, nextText, recordText, levelText, blazePraise;
    TextView mascotBody, mascotMoodTv;
    TextView statElapsed, statCal, statSteps, statRemain;
    Button pauseBtn, cooldownBtn, shareBtn, againBtn;
    View overallFill;
    LinearLayout moodRow;
    TextView[] moodChips;
    // A legutóbb befejezett edzés adatai a megosztás-kártyához.
    int lastDur, lastRounds, lastCal, lastSteps;
    double lastDist = -1; float lastAvg = -1; String lastRecords = "";
    boolean lastWasRun = true;
    ProgressRing ring;
    View bottomNavBar;
    boolean lastPaused = false;
    boolean finished = false;
    boolean lastWasRoutine = false;
    int lastRemainShown = -1;

    // Téma (Beállításokból)
    int tAccent, tAccent2, tWork, tRest;
    // Blaze csak egyszer köszönjön app-indításonként (nem minden újraépítéskor).
    static boolean mascotGreeted = false;
    boolean tPace;
    int builtRev;

    final Handler repeatH = new Handler(Looper.getMainLooper());
    Runnable repeatR;
    boolean receiverRegistered = false;

    // ---------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        applyPalette(this);   // sötét/világos paletta a UI építése előtt
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
        // Állandó alsó navigációs sáv: a tartalom fölötte görög, a sáv fixen alul marad.
        // Edzés közben elrejtjük, hogy a visszaszámlálás fókuszban maradjon.
        LinearLayout navStack = new LinearLayout(this);
        navStack.setOrientation(LinearLayout.VERTICAL);
        navStack.addView(root, new LinearLayout.LayoutParams(-1, 0, 1f));
        bottomNavBar = Ux.bottomNav(this, 0);
        navStack.addView(bottomNavBar, new LinearLayout.LayoutParams(-1, -2));
        setContentView(navStack);
        showRun(false);

        refreshValues();
        updateSoundLabels();
        updateProgramUI();
        refreshTemplates();
        refreshHome();
        // Blaze, a kabalafigura üdvözöl belépéskor (egyszer app-indításonként) –
        // saját, animált kártyával, nem rendszer-Toasttal.
        if (!mascotGreeted) {
            mascotGreeted = true;
            final int gh = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            final String hello = Mascot.greeting(prefs.getString("user_name", ""), gh,
                    greetingContext(gh));
            new Handler(Looper.getMainLooper()).postDelayed(() -> showGreetingCard(hello), 600);
        }
        // Első futáskor a már meglévő kitüntetéseket „látottnak" jelöljük, hogy
        // ne az összeset ünnepelje meg egyszerre a legközelebbi edzés után.
        if (!prefs.contains("badges_seen"))
            prefs.edit().putStringSet("badges_seen", new java.util.HashSet<>(currentBadges())).apply();

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
        DailyNudgeReceiver.schedule(this);
        handleRoutineIntent(getIntent());
        handleRepeatIntent(getIntent());
        handleQuickStartIntent(getIntent());
        maybeShowWelcome();
    }

    /** Blaze belépő köszöntése: felülről beúszó, saját stílusú kártya (nem Toast). */
    void showGreetingCard(String msg) {
        // A köszöntés a legkevésbé sürgős üzenet: ha épp egy ünneplés (kihívás,
        // jelvény, rekord) van kint, azt nem söpörjük félre.
        Ux.blazeCardIfFree(this, msg);
    }

    /**
     * A köszöntés zárómondata a mai állás alapján, fontossági sorrendben:
     * veszélyben lévő széria → majdnem kész kihívás → mai eredmény → étrend.
     * Ha egyik sem áll fenn, null, és marad a szokásos motiváció.
     */
    String greetingContext(int hour) {
        try {
            JSONArray act = activityLog();
            boolean trainedToday = false;
            long dayStart = dayStartMs();
            for (int i = 0; i < act.length(); i++) {
                JSONObject o = act.optJSONObject(i);
                if (o != null && o.optLong("ts") >= dayStart) { trainedToday = true; break; }
            }
            int streak = Streaks.untilYesterday(this, act);

            // 1) Élő széria, ami ma még nincs megvédve – délutántól szólunk érte.
            if (!trainedToday && streak >= 2 && hour >= 15) {
                int dow = (java.util.Calendar.getInstance()
                        .get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;
                if (Theme.isPlanDay(this, dow))
                    return streak + " napos szériád él – ma még nincs edzés. Ne hagyd kihunyni! 🔥";
            }
            // 2) Elkezdett, de még nem teljesített napi kihívás.
            Object[] cst = Challenges.state(this);
            int cur = (int) cst[2], target = (int) cst[3];
            if (cur > 0 && cur < target)
                return "Már " + cur + "/" + target + " " + cst[1] + " a mai kihívásból – hajrá! 🎯";
            // 3) Ma már volt edzés: dicséret.
            if (trainedToday)
                return "a mai edzés megvan – büszke vagyok rád! 💪";
            // 4) Étrendes visszajelzés annak, aki naplóz.
            int pGoal = prefs.getInt("protein_goal", 0);
            if (pGoal > 0) {
                int eaten = (int) Math.round(MealLog.todayProtein(this));
                if (eaten > 0 && eaten < pGoal)
                    return "mára még " + (pGoal - eaten) + " g fehérje van hátra. 🥩";
            }
            int kGoal = prefs.getInt("kcal_goal", 0);
            if (kGoal > 0) {
                int kcal = (int) Math.round(MealLog.todayKcal(this));
                if (kcal > 0 && kcal < kGoal)
                    return "ma eddig " + kcal + " kcal – még " + (kGoal - kcal)
                            + " fér a keretedbe. 🍽";
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Első indításkor egy barátságos üdvözlő lap bemutatja a fő funkciókat.
    void maybeShowWelcome() {
        if (prefs.getBoolean("welcomed", false)) return;
        root.post(() -> {
            if (isFinishing()) return;
            LinearLayout box = vbox();
            box.setPadding(dp(8), dp(4), dp(8), dp(10));
            // Blaze saját képe az üdvözlő lap tetején (ha elérhető).
            int blazeWelcome = drawableId("blaze");
            if (blazeWelcome != 0) {
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageResource(blazeWelcome);
                iv.setClipToOutline(true);
                iv.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override public void getOutline(View v, android.graphics.Outline o) {
                        o.setOval(0, 0, v.getWidth(), v.getHeight());
                    }
                });
                LinearLayout.LayoutParams ivlp = new LinearLayout.LayoutParams(dp(88), dp(88));
                ivlp.gravity = Gravity.CENTER_HORIZONTAL;
                ivlp.bottomMargin = dp(6);
                box.addView(iv, ivlp);
            }
            String[][] feats = {
                {"⏱️", "Intervallum edzés", "Bemelegítés, munka, pihenő, körök és levezetés – minden testre szabható."},
                {"🏃", "Futás követése", "GPS-táv, tempó, lépések és kalória automatikus mérése."},
                {"🐺", "Blaze, az edzőtársad", "Köszönt, emlékeztet és megdicsér – a falka mindig veled van."},
                {"📅", "Edzésnapok terve", "Beállításokban kijelölheted, mely napokon edzel – pihenőnapon nem nyaggatunk."},
                {"🏅", "Kitüntetések & szintek", "Gyűjts XP-t, szintet és jelvényeket, tartsd a sorozatod."},
                {"🎛", "Minden testreszabható", "Színek, mód, kezdőlap-kártyák és csempék sorrendje – a te appod."}
            };
            for (String[] f : feats) {
                LinearLayout row = hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(4), dp(8), dp(4), dp(8));
                TextView ic = text(f[0], 24, TXT, false);
                ic.setPadding(0, 0, dp(14), 0);
                row.addView(ic);
                LinearLayout mid = vbox();
                mid.addView(text(f[1], 15, TXT, true));
                mid.addView(text(f[2], 12.5f, MUTED, false));
                row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
                box.addView(row);
            }
            prefs.edit().putBoolean("welcomed", true).apply();
            new Sheet(this, "Üdv a Gritben! 🐺🔥", "Szia, Blaze vagyok, a falkavezér! Ezt tudja az appod:")
                .addCustom(box)
                // Rögtön megkérdezzük a nevét, majd az edzésnapjait is, hogy az
                // app az első perctől személyre szabott legyen (kihagyható).
                .addPrimary("Kezdjük! 💪", () -> editNameDialog(this::editPlanDaysSheet))
                .show();
        });
    }

    // ================= Háttérkép =================

    /** A Higgsfielddel generált háttérkép + sötét fátyol (a szöveg olvashatóságáért). */
    void addBackgroundImage(FrameLayout host) {
        if (Theme.light(this)) {
            // Világos módban nincs sötét fotó-háttér – tiszta, világos alap a jó kontraszthoz.
            View g = new View(this);
            g.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFFEDF1F8, 0xFFF3F5FA}));
            host.addView(g, new FrameLayout.LayoutParams(-1, -1));
            return;
        }
        int id = drawableId(dailyHomeBg());
        if (id == 0) id = drawableId("bg_main");
        if (id == 0) return;
        try {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageResource(id);
            host.addView(img, new FrameLayout.LayoutParams(-1, -1));
            Ux.kenBurns(img); // lassan „élő" háttér
            View scrim = new View(this);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x80140B0D, 0xD9140B0D, 0xF2140B0D});
            scrim.setBackground(g);
            host.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        } catch (Exception ignored) {}
    }

    int drawableId(String name) {
        try { return getResources().getIdentifier(name, "drawable", getPackageName()); }
        catch (Exception e) { return 0; }
    }

    /** Naponta váltakozó főképernyő-háttér a látványos változatosságért.
        A meglévő változatok közül a nap száma alapján választ; ha egy változat
        nincs jelen (pl. régebbi build), a bg_main-re esik vissza. */
    String dailyHomeBg() {
        String[] variants = {"bg_main", "bg_main2", "bg_main3", "bg_main4"};
        int doy = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        String name = variants[((doy % variants.length) + variants.length) % variants.length];
        return drawableId(name) != 0 ? name : "bg_main";
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
        progressBox.setOnLongClickListener(v -> { shareProgressCard(); return true; });
        col.addView(progressBox, new LinearLayout.LayoutParams(-1, -2));
        TextView shareHint = text("Koppints a részletekért · tartsd nyomva a megosztáshoz 📤", 11, MUTED, false);
        shareHint.setPadding(dp(4), dp(6), 0, 0);
        col.addView(shareHint);
        col.addView(gap(10));

        // Szint-folyamat sáv (XP a következő szintig)
        levelBar = vbox();
        col.addView(levelBar, new LinearLayout.LayoutParams(-1, -2));

        // Heti cél (dinamikus kártya) – rövid, motiváló, felül marad
        goalBox = vbox();
        col.addView(goalBox, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(6));

        // Sablonok – 2×2 rács (a Kezdő gyengéd, hosszabb pihenővel)
        presetViews[0] = preset("HIIT", "30/10 mp · 8×", presetColors[0], 30, 10, 8);
        presetViews[1] = preset("Tempó", "60/20 mp · 6×", presetColors[1], 60, 20, 6);
        presetViews[2] = preset("Tabata", "20/10 mp · 8×", presetColors[2], 20, 10, 8);
        presetViews[3] = preset("Kezdő", "20/40 mp · 6×", presetColors[3], 20, 40, 6);
        LinearLayout presetsRow1 = hbox();
        presetsRow1.addView(presetViews[0], presetLp());
        presetsRow1.addView(presetViews[1], presetLp());
        LinearLayout presetsRow2 = hbox();
        presetsRow2.addView(presetViews[2], presetLp());
        presetsRow2.addView(presetViews[3], presetLp());
        col.addView(presetsRow1, new LinearLayout.LayoutParams(-1, -2));
        col.addView(gap(8));
        col.addView(presetsRow2, new LinearLayout.LayoutParams(-1, -2));
        highlightPresets();
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

        // A fő művelet: nagyobb, magasabb, kiemelt „Indítás" gomb – könnyen elérhető.
        Button start = primaryButton("▶  Indítás");
        start.setTextSize(21);
        start.setPadding(dp(18), dp(22), dp(18), dp(22));
        start.setLetterSpacing(0.03f);
        try { start.setElevation(dp(6)); } catch (Exception ignored) {}
        start.setOnClickListener(v -> startWorkout());
        col.addView(start);
        pulse(start); // finom, figyelemfelhívó lüktetés a fő indítás gombon
        col.addView(gap(22));

        // ---- Áttekintés / motiváció (a fő indítás alatt) ----
        // A szakaszok (Blaze, heti aktivitás, legutóbbi edzés, kitüntetések,
        // rekordok, napi tipp) átrendezhetők és elrejthetők – testreszabhatók.
        weekBox = vbox();
        recentBox = vbox();
        badgesBox = vbox();
        recordsBox = vbox();
        LinearLayout mascotWrap = vbox();
        mascotWrap.addView(mascotCard(), new LinearLayout.LayoutParams(-1, -2));
        mascotWrap.addView(gap(16));
        LinearLayout tipWrap = vbox();
        tipWrap.addView(dailyTipCard());
        tipWrap.addView(gap(16));
        insightBox = vbox();
        challengeBox = vbox();
        sectionViews = new java.util.LinkedHashMap<>();
        sectionViews.put("mascot", mascotWrap);
        sectionViews.put("week", weekBox);
        sectionViews.put("challenge", challengeBox);
        sectionViews.put("insight", insightBox);
        sectionViews.put("recent", recentBox);
        sectionViews.put("badges", badgesBox);
        sectionViews.put("records", recordsBox);
        sectionViews.put("tip", tipWrap);
        sectionsBox = vbox();
        col.addView(sectionsBox, new LinearLayout.LayoutParams(-1, -2));
        refreshSections();

        // Funkció-csempék (2 oszlop) – átrendezhető és elrejthető (testreszabható).
        gridBox = vbox();
        col.addView(gridBox, new LinearLayout.LayoutParams(-1, -2));
        refreshTileGrid();
        col.addView(gap(10));
        Button customize = ghostButton("🎛  Kezdőlap testreszabása");
        customize.setOnClickListener(v -> new Sheet(this, "Kezdőlap testreszabása")
                .addRow("🧩", "Csempék", "Funkció-csempék sorrendje és elrejtése", false, true,
                        this::reorderTilesDialog)
                .addRow("📚", "Szakaszok", "Kártyák (Blaze, heti terv, rekordok…) sorrendje", false, true,
                        this::reorderSectionsDialog)
                .addCancel().show());
        col.addView(customize);

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
                        new int[]{0xF2140B0D, 0x99140B0D, 0x1A000000});
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
        btitles.addView(gritWordmark(34));
        bannerSub = text(bannerSubtitle(), 13, 0xFFFFFFFF, false);
        bannerSub.setAlpha(0.92f);
        btitles.addView(bannerSub);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(-2, -2);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        banner.addView(btitles, tlp);
        // Duolingo-stílusú lángjelző a sarokban: az élő napi széria.
        streakChip = text("", 13.5f, 0xFFFFFFFF, true);
        streakChip.setPadding(dp(11), dp(6), dp(11), dp(6));
        GradientDrawable scg = new GradientDrawable();
        scg.setColor(0x66000000);
        scg.setCornerRadius(dp(20));
        streakChip.setBackground(scg);
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(-2, -2);
        slp.gravity = Gravity.TOP | Gravity.END;
        slp.topMargin = dp(14);
        slp.rightMargin = dp(14);
        banner.addView(streakChip, slp);
        refreshStreakChip();
        // A fejlécre koppintva személyre szabható a megszólítás (neved).
        btitles.setOnClickListener(v -> editNameDialog());
        return banner;
    }

    static final String[] TIPS = {
        "A rendszeresség többet ér, mint az intenzitás. Heti 3 rövid edzés is csodákra képes. 💪",
        "Melegíts be mindig – a bemelegítés csökkenti a sérülésveszélyt és javítja a teljesítményt. 🔥",
        "Igyál eleget! Már 2% folyadékvesztés is rontja a teljesítményt. 💧",
        "A pihenőnap is edzés: ilyenkor épül és erősödik az izom. 😴",
        "Lélegezz a hasaddal – a mély légzés stabilizál és több oxigént ad. 🫁",
        "Nyújts edzés után: rugalmasabb izmok, kevesebb izomláz. 🧘",
        "A helyes technika fontosabb, mint a nagy súly vagy a gyors tempó. ✅",
        "Alvás nélkül nincs regeneráció – célozz 7-8 óra pihenést. 🌙",
        "Kis célok, nagy győzelmek: tűzz ki egy elérhető heti célt. 🎯",
        "A hengerezés (foam rolling) oldja a feszes izmokat és javítja a mozgástartományt. 🌀",
        "Ne edzés előtt egyél nagyot – adj a testednek 1-2 órát az emésztésre. 🍽️",
        "A fokozatosság kulcs: hetente max 10%-kal növeld a terhelést. 📈",
        "A jó zene akár 15%-kal is növelheti a kitartásodat. 🎵",
        "Edzés után 30 percen belül a fehérje segíti a regenerációt. 🥤",
        "A séta is edzés – napi 8000 lépés sokat javít az egészségeden. 🚶",
        "Melegíts be dinamikusan, nyújts statikusan – edzés után. 🤸",
        "A fájdalom és a kellemetlen feszülés nem ugyanaz – figyelj a testedre. 🧠",
        "Váltogasd az edzéstípusokat, hogy ne állj meg a fejlődésben. 🔄",
        "A folyamatos haladás titka a türelem – ne add fel! 🌱",
        "Jegyzeteld fel, hogy érezted magad – így látod majd a mintázatokat. 📝",
        "A törzsizom minden mozgás alapja – ne hagyd ki a plankeket. 🧱",
        "Kis lépések, nagy eredmény: heti 1% javulás egy év alatt hatalmas. 📊",
        "A magnézium és a kálium segít az izomgörcsök megelőzésében. 🍌",
        "Ne edz üres gyomorral hosszú kardiót – legyen egy kis energia. 🍎",
        "A lassú, kontrollált mozdulatok hatékonyabbak, mint a kapkodás. 🐢",
        "Pihenj eleget két nehéz edzés között – a test edzés közt épül. 🛌",
        "A motiváció elfogy, a szokás megmarad – edz mindig ugyanabban az időben. ⏰",
        "Készítsd ki előre az edzőruhád – a legnehezebb lépés az elkezdés. 👟",
        "A napi kihívás kicsi, de minden nap odatesz egy téglát. 🎯",
        "Két edzés között legalább 48 óra pihenőt adj ugyanannak az izomnak. 🛌",
        "A bemelegítés 5 perce megtérül: jobb teljesítmény, kevesebb sérülés. ⏱",
        "A kitartás nem hangulat kérdése – rendszer kérdése. 📅",
        "Az intervall-edzés rövid, de hatásos: 15 perc is felér egy órás tempóval. ⚡",
        "Edzés közben kortyolj, ne vedelj – kis adagokban jobban hasznosul a víz. 🥛",
        "A hideg zuhany edzés után csökkenti az izomlázat és felfrissít. 🚿",
        "Naponta ugyanannyit aludni ugyanolyan fontos, mint eleget aludni. 🕘",
        "Nehéz nap? Egy 5 perces rövid edzés is fenntartja a lendületet. ⚡",
        "Lélegezz ki az erőkifejtésnél, vegyél levegőt a leengedésnél. 🌬️",
        "Deload hét: 4-6 hetente vegyél vissza kicsit, hogy a tested utolérje magát. 🔧"
    };

    View dailyTipCard() {
        final LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        final TextView head = text("💡 Napi tipp  ·  koppints az újért", 12, tAccent, true);
        int doy = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        final int[] idx = { ((doy % TIPS.length) + TIPS.length) % TIPS.length };
        final TextView body = text(TIPS[idx[0]], 13.5f, TXT, false);
        body.setPadding(0, dp(6), 0, 0);
        c.addView(head);
        c.addView(body);
        c.setClickable(true);
        c.setOnClickListener(v -> {
            idx[0] = (idx[0] + 1) % TIPS.length;
            body.setText(TIPS[idx[0]]);
            Ux.enter(body, 0);
        });
        return c;
    }

    /** Finom, végtelen lüktetés egy nézeten (a fő indítás gomb kiemeléséhez). */
    void pulse(View v) {
        if (!Theme.animEnabled(this)) return;
        try {
            android.animation.ObjectAnimator sx = android.animation.ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.03f);
            android.animation.ObjectAnimator sy = android.animation.ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.03f);
            for (android.animation.ObjectAnimator a : new android.animation.ObjectAnimator[]{sx, sy}) {
                a.setDuration(1150);
                a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                a.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                a.start();
            }
        } catch (Exception ignored) {}
    }

    /** Egyszeri „dobbanás" a visszaszámláló számon (utolsó 3 mp). */
    void pulseTime() {
        try {
            timeText.animate().cancel();
            timeText.setScaleX(1.3f);
            timeText.setScaleY(1.3f);
            timeText.animate().scaleX(1f).scaleY(1f).setDuration(320)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        } catch (Exception ignored) {}
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

    // ---- Kabalafigura: Blaze, a tűzfarkas ----

    /** Blaze aktuális üzenetének és hangulatának újraszámolása (edzés után is friss). */
    void refreshMascot() {
        if (mascotBody == null) return;
        JSONArray arr = activityLog();
        long dayStart = dayStartMs();
        boolean today = false;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optLong("ts") >= dayStart) { today = true; break; }
        }
        int ds = dayStreak(arr), ws = weekStreak(arr);
        java.util.Calendar calNow = java.util.Calendar.getInstance();
        int hour = calNow.get(java.util.Calendar.HOUR_OF_DAY);
        int dowIdx = (calNow.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7; // H=0..V=6
        boolean restDay = !Theme.isPlanDay(this, dowIdx);
        boolean risk = !today && !restDay && ws >= 1 && hour >= 16;
        String userName = prefs.getString("user_name", "");
        String msg = Mascot.line(this, userName, arr.length(), today, ds, ws, risk, hour, restDay);

        // Délutántól, ha ma még nem edzettél és a kihívás sincs kész, Blaze konkrét célt ad.
        if (!today && !restDay && !risk && hour >= 12 && arr.length() > 0) {
            Object[] cst = challengeState();
            int cCur = (int) cst[2], cTarget = (int) cst[3];
            if (cCur < cTarget)
                msg = "🎯 A mai kihívás vár: " + cst[0] + " (" + cCur + "/" + cTarget
                        + " " + cst[1] + ") – csapjunk bele! 🐺🔥";
        }

        // Ha van heti terv és ma már edzett, Blaze a terv állásáról beszél.
        if (today && !Theme.planDays(this).isEmpty()) {
            long wsMs = weekStartMs();
            boolean[] done = new boolean[7];
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                long ts = o.optLong("ts");
                if (ts < wsMs) continue;
                int di = (int) ((ts - wsMs) / (24L * 3600 * 1000));
                if (di >= 0 && di < 7) done[di] = true;
            }
            int plannedCount = 0, plannedDone = 0, futureRemaining = 0;
            for (int i = 0; i < 7; i++) {
                if (!Theme.isPlanDay(this, i)) continue;
                plannedCount++;
                if (done[i]) plannedDone++;
                else if (i > dowIdx) futureRemaining++;
            }
            if (plannedCount > 0 && plannedDone >= plannedCount)
                msg = Mascot.planStatus(userName, true, 0);
            else if (futureRemaining > 0)
                msg = Mascot.planStatus(userName, false, futureRemaining);
            // különben (elmulasztott múltbeli terv-nap, de több hátralévő nincs):
            // marad az általános dicséret.
        }

        // Ha tegnap tervezett edzésnap volt, de kimaradt, Blaze visszavágásra hív.
        // (A késő délutáni széria-figyelmeztetés fontosabb, azt nem írjuk felül.)
        if (!today && !restDay && !risk && arr.length() > 0
                && !Theme.planDays(this).isEmpty()) {
            int yIdx = dowIdx == 0 ? 6 : dowIdx - 1;
            if (Theme.isPlanDay(this, yIdx)) {
                long dayMs = 24L * 3600 * 1000;
                long todayStart = dayStartMs();
                boolean yTrained = false;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    long ts = o == null ? 0 : o.optLong("ts");
                    if (ts >= todayStart - dayMs && ts < todayStart) { yTrained = true; break; }
                }
                if (!yTrained) msg = Mascot.comeback(userName);
            }
        }
        mascotBody.setText(msg);
        if (mascotMoodTv != null) mascotMoodTv.setText(Mascot.mood(today, ds, risk));
    }

    /** Blaze kártyája a kezdőképernyő tetején: avatar + beszédbuborék, koppintásra biztat. */
    View mascotCard() {
        LinearLayout cardM = card();
        cardM.setOrientation(LinearLayout.HORIZONTAL);
        cardM.setGravity(Gravity.CENTER_VERTICAL);
        cardM.setPadding(dp(14), dp(14), dp(16), dp(14));

        // Avatar: saját gyártású Blaze-kép (ha elérhető), különben emoji-korong.
        FrameLayout badge = new FrameLayout(this);
        GradientDrawable bbg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{tAccent, tAccent2});
        bbg.setShape(GradientDrawable.OVAL);
        badge.setBackground(bbg);
        badge.setElevation(dp(3));
        int blazeId = drawableId("blaze");
        if (blazeId != 0) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageResource(blazeId);
            iv.setClipToOutline(true);
            iv.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v, android.graphics.Outline o) {
                    o.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            badge.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            TextView face = new TextView(this);
            face.setText(Mascot.FACE);
            face.setTextSize(30);
            face.setGravity(Gravity.CENTER);
            badge.addView(face, new FrameLayout.LayoutParams(-1, -1));
        }
        mascotMoodTv = new TextView(this);
        mascotMoodTv.setTextSize(15);
        FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(-2, -2);
        mlp.gravity = Gravity.BOTTOM | Gravity.END;
        badge.addView(mascotMoodTv, mlp);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(60), dp(60));
        blp.rightMargin = dp(14);
        cardM.addView(badge, blp);

        LinearLayout txtCol = vbox();
        // Blaze neve mellett a saját szinted címe – a kabala „ismeri" a haladásod.
        long xpNow = Levels.totalXp(this);
        int lvlNow = Levels.levelForXp(xpNow);
        txtCol.addView(text(Mascot.NAME + " 🔥  ·  Szint " + lvlNow + " – " + Levels.title(lvlNow),
                12.5f, tAccent, true));
        mascotBody = text("", 14, TXT, false);
        mascotBody.setPadding(0, dp(3), 0, 0);
        txtCol.addView(mascotBody);
        cardM.addView(txtCol, new LinearLayout.LayoutParams(0, -2, 1f));

        cardM.setClickable(true);
        cardM.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            int dowNow = (java.util.Calendar.getInstance()
                    .get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;
            if (!Theme.isPlanDay(this, dowNow)) {
                // Pihenőnapon Blaze nyújtást ajánl a pep helyett.
                new Sheet(this, "Pihenőnap 🌙🐺",
                        "A regeneráció is edzés – egy kis nyújtás sokat ér.")
                        .addPrimary("🧘 Mobilitás / nyújtás", () ->
                                startActivity(new Intent(this, MobilityActivity.class)))
                        .addCancel()
                        .show();
                return;
            }
            mascotBody.setText(Mascot.pep());
            // Pár másodperc után visszaáll az állapot szerinti üzenetre.
            mascotBody.postDelayed(this::refreshMascot, 8000);
        });
        refreshMascot();
        return cardM;
    }

    // ---- Funkció-csempék ----

    View featureTile(String emoji, String label, int accent, Runnable onTap) {
        LinearLayout t = vbox();
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(16), dp(14), dp(16));
        GradientDrawable bg = new GradientDrawable();
        // Halvány színes tint + színes keret → minden csempe jól elkülönül.
        bg.setColor((accent & 0x00FFFFFF) | 0x17000000);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), (accent & 0x00FFFFFF) | 0x55000000);
        t.setBackground(bg);
        t.setClickable(true);

        // Színes kör az ikon mögött – látványos és gyorsan beazonosítható.
        TextView e = text(emoji, 23, TXT, false);
        e.setGravity(Gravity.CENTER);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor((accent & 0x00FFFFFF) | 0x33000000);
        e.setBackground(ring);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(dp(44), dp(44));
        elp.gravity = Gravity.CENTER_HORIZONTAL;

        TextView l = text(label, 13, TXT, true);
        l.setGravity(Gravity.CENTER);
        l.setPadding(0, dp(8), 0, 0);
        t.addView(e, elp);
        t.addView(l);
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

    // ---- Testreszabható csempék (átrendezés + elrejtés) ----

    /** Egy funkció-csempe leírása: azonosító, ikon, felirat, szín, művelet. */
    final class TileDef {
        final String id, emoji, label; final int color; final Runnable action;
        TileDef(String id, String emoji, String label, int color, Runnable action) {
            this.id = id; this.emoji = emoji; this.label = label; this.color = color; this.action = action;
        }
    }

    java.util.List<TileDef> allTileDefs() {
        java.util.List<TileDef> t = new java.util.ArrayList<>();
        t.add(new TileDef("history", "📜", "Előzmények", 0xFF8B9DFF, () -> startActivity(new Intent(this, HistoryActivity.class))));
        t.add(new TileDef("stats", "📈", "Statisztika", 0xFF5FD0FF, () -> startActivity(new Intent(this, StatsActivity.class))));
        t.add(new TileDef("profile", "📊", "Profil / BMI", 0xFF6FE3C2, () -> startActivity(new Intent(this, ProfileActivity.class))));
        t.add(new TileDef("reminders", "🔔", "Emlékeztetők", 0xFFFFD166, () -> startActivity(new Intent(this, RemindersActivity.class))));
        t.add(new TileDef("mobility", "🧘", "Nyújtás & mobilitás", 0xFFB98CFF, () -> startActivity(new Intent(this, MobilityActivity.class))));
        t.add(new TileDef("library", "📖", "Gyakorlatok", 0xFFFF9A8B, () -> startActivity(new Intent(this, LibraryActivity.class))));
        t.add(new TileDef("strength", "🏋️", "Erősítő napló", 0xFFFF7BA6, () -> startActivity(new Intent(this, StrengthActivity.class))));
        double kcalT = MealLog.todayKcal(this);
        t.add(new TileDef("diet", "🍽", kcalT > 0 ? "Étrend · " + Math.round(kcalT) + " kcal" : "Étrend", 0xFFFFB74D, () -> startActivity(new Intent(this, DietActivity.class))));
        t.add(new TileDef("template", "💾", "Sablon mentése", 0xFF7FE1A6, this::saveTemplateDialog));
        // A Beállítások szándékosan nincs itt: külön füle van az alsó menüsorban.
        return t;
    }

    java.util.List<String> tileOrder() {
        java.util.List<String> known = new java.util.ArrayList<>();
        for (TileDef d : allTileDefs()) known.add(d.id);
        java.util.List<String> order = new java.util.ArrayList<>();
        String s = prefs.getString("tile_order", "");
        if (!s.isEmpty()) for (String id : s.split(",")) if (known.contains(id) && !order.contains(id)) order.add(id);
        for (String id : known) if (!order.contains(id)) order.add(id); // új csempék a végére
        return order;
    }

    java.util.Set<String> tileHidden() {
        java.util.Set<String> h = new java.util.HashSet<>();
        String s = prefs.getString("tile_hidden", "");
        if (!s.isEmpty()) for (String id : s.split(",")) if (!id.isEmpty()) h.add(id);
        return h;
    }

    String joinCsv(java.util.Collection<String> c) {
        StringBuilder sb = new StringBuilder();
        for (String s : c) { if (sb.length() > 0) sb.append(','); sb.append(s); }
        return sb.toString();
    }

    void refreshTileGrid() {
        if (gridBox == null) return;
        gridBox.removeAllViews();
        java.util.Map<String, TileDef> byId = new java.util.HashMap<>();
        for (TileDef d : allTileDefs()) byId.put(d.id, d);
        java.util.Set<String> hidden = tileHidden();
        java.util.List<View> tiles = new java.util.ArrayList<>();
        for (String id : tileOrder()) {
            TileDef d = byId.get(id);
            if (d == null || hidden.contains(id)) continue;
            tiles.add(featureTile(d.emoji, d.label, d.color, d.action));
        }
        for (int i = 0; i < tiles.size(); i += 2) {
            View b = (i + 1 < tiles.size()) ? tiles.get(i + 1) : new View(this);
            gridBox.addView(tileRow(tiles.get(i), b));
            if (i + 2 < tiles.size()) gridBox.addView(gap(10));
        }
    }

    void reorderTilesDialog() {
        final java.util.List<String> order = new java.util.ArrayList<>(tileOrder());
        final java.util.Set<String> hidden = new java.util.HashSet<>(tileHidden());
        final java.util.Map<String, TileDef> byId = new java.util.HashMap<>();
        for (TileDef d : allTileDefs()) byId.put(d.id, d);
        final LinearLayout box = vbox();
        box.setPadding(dp(4), dp(2), dp(4), 0);
        final Runnable[] render = new Runnable[1];
        render[0] = () -> {
            box.removeAllViews();
            for (int i = 0; i < order.size(); i++) {
                final int idx = i;
                final String id = order.get(i);
                TileDef d = byId.get(id);
                if (d == null) continue;
                final boolean hid = hidden.contains(id);
                LinearLayout row = hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(7), dp(4), dp(7));
                row.addView(text(d.emoji + "  " + d.label, 15, hid ? MUTED : TXT, !hid),
                        new LinearLayout.LayoutParams(0, -2, 1f));
                Button eye = smallIconBtn(hid ? "🚫" : "👁");
                eye.setOnClickListener(v -> { if (hid) hidden.remove(id); else hidden.add(id); render[0].run(); });
                row.addView(eye);
                Button up = smallIconBtn("↑");
                up.setAlpha(idx > 0 ? 1f : 0.3f);
                up.setOnClickListener(v -> { if (idx > 0) { java.util.Collections.swap(order, idx, idx - 1); render[0].run(); } });
                row.addView(up);
                Button down = smallIconBtn("↓");
                down.setAlpha(idx < order.size() - 1 ? 1f : 0.3f);
                down.setOnClickListener(v -> { if (idx < order.size() - 1) { java.util.Collections.swap(order, idx, idx + 1); render[0].run(); } });
                row.addView(down);
                box.addView(row);
            }
        };
        render[0].run();
        new Sheet(this, "Csempék testreszabása", "Rendezd át (↑↓) vagy rejtsd el (👁) a csempéket")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    prefs.edit().putString("tile_order", joinCsv(order))
                            .putString("tile_hidden", joinCsv(hidden)).apply();
                    refreshTileGrid();
                })
                .addCancel().show();
    }

    Button smallIconBtn(String label) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextColor(TXT); b.setTextSize(16);
        b.setStateListAnimator(null); b.setPadding(0, 0, 0, 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2); bg.setCornerRadius(dp(10)); bg.setStroke(dp(1), LINE);
        b.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(40), dp(40));
        p.leftMargin = dp(5);
        b.setLayoutParams(p);
        return b;
    }

    // ---- Testreszabható kezdőlap-szakaszok (átrendezés + elrejtés) ----

    static final String[] SECT_IDS = {"mascot", "week", "challenge", "insight", "recent", "badges", "records", "tip"};
    static final String[] SECT_NAMES = {"🐺 Blaze", "📅 Heti aktivitás", "🎯 Mai kihívás", "📊 Heti összevetés",
            "🕘 Legutóbbi edzés", "🎖 Kitüntetések", "🏆 Rekordok", "💡 Napi tipp"};

    java.util.List<String> sectOrder() {
        java.util.List<String> order = new java.util.ArrayList<>();
        String s = prefs.getString("sect_order", "");
        if (!s.isEmpty())
            for (String id : s.split(","))
                if (java.util.Arrays.asList(SECT_IDS).contains(id) && !order.contains(id)) order.add(id);
        for (String id : SECT_IDS) if (!order.contains(id)) order.add(id); // új szakaszok a végére
        return order;
    }

    java.util.Set<String> sectHidden() {
        java.util.Set<String> h = new java.util.HashSet<>();
        String s = prefs.getString("sect_hidden", "");
        if (!s.isEmpty()) for (String id : s.split(",")) if (!id.isEmpty()) h.add(id);
        return h;
    }

    void refreshSections() {
        if (sectionsBox == null || sectionViews == null) return;
        sectionsBox.removeAllViews();
        java.util.Set<String> hidden = sectHidden();
        for (String id : sectOrder()) {
            View v = sectionViews.get(id);
            if (v == null || hidden.contains(id)) continue;
            if (v.getParent() instanceof android.view.ViewGroup)
                ((android.view.ViewGroup) v.getParent()).removeView(v);
            sectionsBox.addView(v, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    void reorderSectionsDialog() {
        final java.util.List<String> order = new java.util.ArrayList<>(sectOrder());
        final java.util.Set<String> hidden = new java.util.HashSet<>(sectHidden());
        final LinearLayout box = vbox();
        box.setPadding(dp(4), dp(2), dp(4), 0);
        final Runnable[] render = new Runnable[1];
        render[0] = () -> {
            box.removeAllViews();
            for (int i = 0; i < order.size(); i++) {
                final int idx = i;
                final String id = order.get(i);
                int nameIdx = java.util.Arrays.asList(SECT_IDS).indexOf(id);
                if (nameIdx < 0) continue;
                final boolean hid = hidden.contains(id);
                LinearLayout row = hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(7), dp(4), dp(7));
                row.addView(text(SECT_NAMES[nameIdx], 15, hid ? MUTED : TXT, !hid),
                        new LinearLayout.LayoutParams(0, -2, 1f));
                Button eye = smallIconBtn(hid ? "🚫" : "👁");
                eye.setOnClickListener(v -> { if (hid) hidden.remove(id); else hidden.add(id); render[0].run(); });
                row.addView(eye);
                Button up = smallIconBtn("↑");
                up.setAlpha(idx > 0 ? 1f : 0.3f);
                up.setOnClickListener(v -> { if (idx > 0) { java.util.Collections.swap(order, idx, idx - 1); render[0].run(); } });
                row.addView(up);
                Button down = smallIconBtn("↓");
                down.setAlpha(idx < order.size() - 1 ? 1f : 0.3f);
                down.setOnClickListener(v -> { if (idx < order.size() - 1) { java.util.Collections.swap(order, idx, idx + 1); render[0].run(); } });
                row.addView(down);
                box.addView(row);
            }
        };
        render[0].run();
        new Sheet(this, "Szakaszok testreszabása", "Rendezd át (↑↓) vagy rejtsd el (👁) a kártyákat")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    prefs.edit().putString("sect_order", joinCsv(order))
                            .putString("sect_hidden", joinCsv(hidden)).apply();
                    refreshSections();
                })
                .addCancel().show();
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
        final int[] tplColors = {0xFF5FD0FF, 0xFF7FE1A6, 0xFFFFA24B, 0xFFB98CFF, 0xFFFF7BA6, 0xFFFFD166};
        LinearLayout cardT = card();
        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            final Workouts.W w = list.get(i);
            final int tc = tplColors[i % tplColors.length];
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(8), dp(12));
            row.setClickable(true);
            row.setOnClickListener(v -> loadTemplate(w));
            // Színes sáv balra – minden sablon jól elkülönül és látványosabb.
            View tbar = new View(this);
            GradientDrawable tbarBg = new GradientDrawable();
            tbarBg.setColor(tc); tbarBg.setCornerRadius(dp(3));
            LinearLayout.LayoutParams tbarLp = new LinearLayout.LayoutParams(dp(4), dp(34));
            tbarLp.rightMargin = dp(12);
            tbar.setLayoutParams(tbarLp); tbar.setBackground(tbarBg);
            row.addView(tbar);
            LinearLayout left = vbox();
            left.addView(text(w.name, 15.5f, tc, true));
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

    // A fejléc megszólításának személyre szabása (üres = napszak szerinti alap).
    void editNameDialog() { editNameDialog(null); }

    /** Mint fent; mentés után az opcionális folytatás fut (pl. edzésnap-választó az üdvözlőben). */
    void editNameDialog(final Runnable after) {
        final EditText et = sheetInput("A neved (pl. Dávid)", false);
        et.setText(prefs.getString("user_name", ""));
        et.setSelectAllOnFocus(true);
        LinearLayout box = vbox();
        box.setPadding(dp(6), dp(2), dp(6), dp(4));
        box.addView(et);
        new Sheet(this, "Megszólítás", "Így köszönt majd az app a kezdőképernyőn.")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    prefs.edit().putString("user_name", et.getText().toString().trim()).apply();
                    if (bannerSub != null) bannerSub.setText(bannerSubtitle());
                    if (after != null) after.run();
                })
                .addNeutral("Törlés", () -> {
                    prefs.edit().remove("user_name").apply();
                    if (bannerSub != null) bannerSub.setText(bannerSubtitle());
                    if (after != null) after.run();
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
            boolean reached = frac >= 1f;
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
            // Teljesítés esetén arany sáv az ünnepléshez, egyébként a téma színe.
            GradientDrawable fgd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    reached ? new int[]{0xFFFFC107, 0xFFFF8F00} : new int[]{tAccent, tAccent2});
            fgd.setCornerRadius(dp(6));
            fill.setBackground(fgd);
            barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(12), Math.max(0.001f, frac)));
            barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(12), 1f - Math.max(0.001f, frac)));
            inner.addView(barBg, new LinearLayout.LayoutParams(-1, -2));
            inner.addView(gap(8));

            String sub;
            if (reached) sub = "Kész! Teljesítetted a heti célod 🎉";
            else {
                double left2 = target - done;
                String leftS = mode == 2 ? String.format(Locale.US, "%.1f", left2) : String.valueOf((int) Math.ceil(left2));
                sub = Math.round(frac * 100) + "% · még " + leftS + " " + GOAL_UNITS[mode] + " a célig";
            }
            inner.addView(text(sub, reached ? 12.5f : 12, reached ? 0xFFFFC107 : MUTED, reached));
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

    // Egymást követő edzésnapok száma (ma vagy tegnap végződő sorozat).
    /** Egyesített aktivitás-napló a motivációhoz: időzítős edzések + erősítő
        bejegyzések (a streak-számításhoz csak a „ts" időbélyeg kell). Így egy
        erősítő nap is beleszámít a napi/heti sorozatba a főképernyő feliratában. */
    JSONArray activityLog() {
        return History.loadAll(this);
    }

    int dayStreak(JSONArray arr) {
        // Terv-tudatos ("okos") széria: pihenőnap nem töri meg – lásd Streaks.
        return Streaks.current(this, arr);
    }

    long dayStartOf(long ts) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // A valaha volt leghosszabb megszakítás nélküli heti sorozat (kitüntetésekhez).
    int bestWeekStreak(JSONArray arr) {
        java.util.HashSet<Long> weeks = new java.util.HashSet<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) weeks.add(weekStartOf(o.optLong("ts")));
        }
        int best = 0;
        for (Long w : weeks) {
            if (weeks.contains(prevWeekOf(w))) continue; // csak a sorozat elejéről indulunk
            int s = 0; long cur = w;
            while (weeks.contains(cur)) { s++; cur = weekStartOf(cur + 10L * 24 * 3600 * 1000); }
            if (s > best) best = s;
        }
        return best;
    }

    long dayStartMs() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // Személyre szabott, napszakhoz és sorozathoz igazodó üdvözlő felirat a fejlécben.
    String bannerSubtitle() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String base = hour < 10 ? "Jó reggelt" : hour < 18 ? "Szia" : "Jó estét";
        String nm = prefs.getString("user_name", "").trim();
        String greet = nm.isEmpty() ? base + "!" : base + ", " + nm + "!";
        JSONArray arr = activityLog();   // időzítős edzések + erősítő bejegyzések együtt
        if (arr.length() == 0) return greet + " Kezdd el az első edzésed 💪";
        long dayStart = dayStartMs();
        boolean today = false;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optLong("ts") >= dayStart) { today = true; break; }
        }
        if (!today) {
            // Veszélyben a heti sorozat? (nem edzett még ezen a héten, és a hét vége felé jár)
            long ws = weekStartMs();
            boolean thisWeek = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null && o.optLong("ts") >= ws) { thisWeek = true; break; }
            }
            int dowIdx = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7; // H=0..V=6
            int wk = weekStreak(arr);
            if (!thisWeek && wk >= 1 && dowIdx >= 3)
                return greet + " ⚠️ Veszélyben a " + wk + " hetes sorozatod – edz még a héten!";
            if (!Theme.isPlanDay(this, dowIdx))
                return greet + " Ma pihenőnap – a regeneráció is fejlődés! 🌙";
            Object[] cst = challengeState();
            if ((int) cst[2] < (int) cst[3])
                return greet + " Ma még nem edzettél – 🎯 a mai kihívás vár!";
            return greet + " Ma még nem edzettél – hajrá! 🔥";
        }
        int ds = dayStreak(arr);
        // Magas napi sorozatnál gyengéd pihenő-emlékeztető (a pihenés is fejlődés).
        if (ds >= 6) return greet + " " + ds + " napos sorozat! 🌙 A pihenő is fejlődés – hallgass a testedre.";
        if (ds > 1) return greet + " " + ds + " napos sorozat 🔥";
        int ws = weekStreak(arr);
        if (ws > 1) return greet + " " + ws + " hetes sorozat 🔥";
        return greet + " Szép munka ma! ✅";
    }

    /** A fejléc lángjelzőjének frissítése: 2 naptól mutatja az élő szériát. */
    void refreshStreakChip() {
        if (streakChip == null) return;
        int ds = dayStreak(activityLog());
        if (ds >= 2) {
            streakChip.setVisibility(View.VISIBLE);
            streakChip.setText("🔥 " + ds);
        } else {
            streakChip.setVisibility(View.GONE);
        }
    }

    // ---- Haladás-csík (szint / sorozat / edzésszám) ----

    // A főképernyő összes dinamikus kártyájának frissítése egy helyről.
    void refreshHome() {
        refreshGoal();
        refreshProgress();
        refreshRecent();
        refreshBadges();
        refreshWeekDots();
        refreshChallenge();
        refreshInsight();
        refreshRecords();
        refreshMascot();
        refreshStreakChip();
        refreshTileGrid(); // a csempefeliratok (pl. Étrend kcal) is frissüljenek
    }

    /** A mai kihívás állapota: {title, unit, cur, target, seed} – közös számítás. */
    Object[] challengeState() {
        return Challenges.state(this);
    }

    /** Napi kihívás: naponta más, determinisztikus feladat; a mai edzésekből méri a haladást. */
    void refreshChallenge() {
        if (challengeBox == null) return;
        challengeBox.removeAllViews();
        Object[] st = challengeState();
        String title = (String) st[0], unit = (String) st[1];
        int cur = (int) st[2], target = (int) st[3], seed = (int) st[4];
        boolean done = cur >= target;

        LinearLayout c = card();
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        TextView head = text(done ? "🎯 Mai kihívás  ·  TELJESÍTVE 🏆" : "🎯 Mai kihívás",
                13, 0xFF7FE1A6, true);
        head.setPadding(dp(2), 0, 0, dp(8));
        c.addView(head);
        c.addView(text(title, 14.5f, TXT, true));
        c.addView(gap(10));

        LinearLayout barBg = hbox();
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(0x22FFFFFF);
        bgd.setCornerRadius(dp(6));
        barBg.setBackground(bgd);
        View fill = new View(this);
        GradientDrawable fgd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{tAccent, tAccent2});
        fgd.setCornerRadius(dp(6));
        fill.setBackground(fgd);
        float f = Math.max(0.02f, Math.min(1f, cur / (float) target));
        barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(10), f));
        barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(10), 1f - f));
        c.addView(barBg, new LinearLayout.LayoutParams(-1, -2));
        c.addView(gap(8));
        c.addView(text(done ? "Blaze büszkén vonyít: ez az, falkatárs! 🐺🔥"
                : cur + " / " + target + " " + unit + " – koppints, és csapj bele! 💪", 12.5f, MUTED, false));

        // Koppintásra cselekszik: időzítős kihívásnál indít, súlyzósnál az Erő oldalt nyitja.
        if (!done) {
            c.setClickable(true);
            c.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if ("ismétlés".equals(unit))
                    startActivity(new Intent(this, StrengthActivity.class));
                else if ("étkezés".equals(unit) || "g fehérje".equals(unit)
                        || "pohár".equals(unit))
                    startActivity(new Intent(this, DietActivity.class));
                else if (!TimerService.activeNow)
                    startWorkout();
            });
        }

        challengeBox.addView(c, new LinearLayout.LayoutParams(-1, -2));
        challengeBox.addView(gap(16));

        // Első teljesítéskor (aznap egyszer) konfetti + Blaze ünneplés + számláló.
        if (done && prefs.getInt("challenge_done_seed", -1) != seed) {
            prefs.edit().putInt("challenge_done_seed", seed)
                    .putInt("challenge_done_count", prefs.getInt("challenge_done_count", 0) + 1)
                    .apply();
            Levels.addBonus(this, 10); // kihívás-bónusz a szinthez
            if (root != null) Confetti.burst(root);
            Ux.blazeCard(this, "🎯 Mai kihívás teljesítve! 🏆 +10 XP – Blaze büszkén vonyít! 🔥");
        }
    }

    // ---- Heti összevetés (e hét vs. előző hét) ----

    void refreshInsight() {
        if (insightBox == null) return;
        insightBox.removeAllViews();
        JSONArray arr = activityLog();
        long ws = weekStartMs();
        long prevWs = ws - 7L * 24 * 3600 * 1000;
        int cThis = 0, cPrev = 0;
        long durThis = 0, durPrev = 0;
        double mThis = 0, mPrev = 0;   // méterben
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            long ts = o.optLong("ts");
            double d = o.optDouble("dist", 0);
            if (ts >= ws) { cThis++; durThis += o.optInt("dur"); if (d > 0) mThis += d; }
            else if (ts >= prevWs) { cPrev++; durPrev += o.optInt("dur"); if (d > 0) mPrev += d; }
        }
        if (cThis == 0 && cPrev == 0) return;   // nincs mihez mérni

        LinearLayout c = card();
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        TextView head = text("📊 Heti összevetés  ·  előző héthez képest", 13, 0xFF5FD0FF, true);
        head.setPadding(dp(2), 0, 0, dp(8));
        c.addView(head);
        c.addView(insightRow("🏁 Edzések", cThis + " db", cThis - cPrev, ""));
        c.addView(insightRow("⏱ Mozgás", (durThis / 60) + " perc", (durThis - durPrev) / 60.0, "perc"));
        if (mThis > 0 || mPrev > 0)
            c.addView(insightRow("📍 Táv",
                    String.format(new Locale("hu"), "%.1f km", mThis / 1000.0),
                    (mThis - mPrev) / 1000.0, "km"));
        // Kcal-átlag összevetés annak, aki étrendet vezet (semleges színnel – a
        // több vagy kevesebb kalória önmagában se nem jó, se nem rossz).
        try {
            long dayMs = 24L * 3600 * 1000;
            double[] kThis = new double[7], kPrev = new double[7];
            for (MealLog.Meal m : MealLog.load(this)) {
                if (m.ts >= ws) {
                    int k = (int) ((m.ts - ws) / dayMs);
                    if (k >= 0 && k < 7) kThis[k] += m.kcal();
                } else if (m.ts >= prevWs) {
                    int k = (int) ((m.ts - prevWs) / dayMs);
                    if (k >= 0 && k < 7) kPrev[k] += m.kcal();
                }
            }
            int dThis = 0, dPrev = 0; double sThis = 0, sPrev = 0;
            for (double v : kThis) if (v > 0) { dThis++; sThis += v; }
            for (double v : kPrev) if (v > 0) { dPrev++; sPrev += v; }
            if (dThis > 0) {
                double avgT = sThis / dThis;
                LinearLayout row = hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(2), dp(3), 0, dp(3));
                row.addView(text("🍽 Kcal-átlag", 13.5f, MUTED, false),
                        new LinearLayout.LayoutParams(0, -2, 1f));
                row.addView(text(Math.round(avgT) + " kcal/nap", 14, TXT, true));
                if (dPrev > 0) {
                    double diff = avgT - sPrev / dPrev;
                    String dTxt = Math.abs(diff) < 25 ? "   ="
                            : (diff > 0 ? "   ↑ " : "   ↓ ") + Math.round(Math.abs(diff));
                    row.addView(text(dTxt, 13, MUTED, true));
                }
                c.addView(row);
            }
        } catch (Exception ignored) {}
        insightBox.addView(c);
        insightBox.addView(gap(14));
    }

    View insightRow(String label, String value, double delta, String unit) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(3), 0, dp(3));
        row.addView(text(label, 13.5f, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text(value, 14, TXT, true));
        String dTxt; int dCol;
        if (Math.abs(delta) < 0.05) { dTxt = "   =";  dCol = MUTED; }
        else if (delta > 0) { dTxt = "   ↑ " + fmtDelta(delta, unit); dCol = 0xFF7FE1A6; }
        else { dTxt = "   ↓ " + fmtDelta(-delta, unit); dCol = 0xFFFF6B6B; }
        row.addView(text(dTxt, 13, dCol, true));
        return row;
    }

    String fmtDelta(double v, String unit) {
        if ("km".equals(unit)) return String.format(new Locale("hu"), "%.1f km", v);
        return (int) Math.round(v) + (unit.isEmpty() ? "" : " " + unit);
    }

    void refreshProgress() {
        if (progressBox == null) return;
        progressBox.removeAllViews();
        JSONArray arr = History.loadAll(this);
        long xp = Levels.totalXp(this);
        int lvl = Levels.levelForXp(xp);
        progressBox.addView(progressChip("⭐", "Szint " + lvl, Levels.title(lvl)), progChipLp());
        int ds = dayStreak(arr);
        String streakVal = ds > 1 ? ds + " nap" : weekStreak(arr) + " hét";
        progressBox.addView(progressChip("🔥", streakVal, "sorozat"), progChipLp());
        progressBox.addView(progressChip("🏁", String.valueOf(arr.length()), "edzés"), progChipLp());
        // Mai kalória (csak ha ma már naplóztál ételt az Étrendben).
        double kcalToday = MealLog.todayKcal(this);
        if (kcalToday > 0) {
            int kGoal = getSharedPreferences("edzo", MODE_PRIVATE).getInt("kcal_goal", 0);
            String kv = kGoal > 0 ? Math.round(kcalToday) + "/" + kGoal
                    : String.valueOf(Math.round(kcalToday));
            progressBox.addView(progressChip("🍽", kv, "kcal ma"), progChipLp());
        }
        // Mai víz (csak ha ma már ment a számláló – appból vagy widgetről).
        int waterCl = Water.todayCl(this);
        if (waterCl > 0)
            progressBox.addView(progressChip("💧", Water.liters(waterCl), "víz ma"),
                    progChipLp());
        if (bannerSub != null) bannerSub.setText(bannerSubtitle());
        refreshLevelBar(arr, lvl, xp);
    }

    void refreshLevelBar(JSONArray arr, int lvl, long xp) {
        if (levelBar == null) return;
        levelBar.removeAllViews();
        float frac = Levels.progress(xp);
        long toNext = Levels.xpToNext(xp);
        levelBar.addView(gap(10));
        LinearLayout c = card();
        c.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text("⭐ Szint " + lvl + " · " + Levels.title(lvl), 13.5f, TXT, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(text(Math.round(frac * 100) + "%", 13, tAccent, true));
        c.addView(top);
        c.addView(gap(8));
        LinearLayout barBg = hbox();
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(CARD2); bgd.setCornerRadius(dp(6));
        barBg.setBackground(bgd);
        View fill = new View(this);
        GradientDrawable fgd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{tAccent, tAccent2});
        fgd.setCornerRadius(dp(6));
        fill.setBackground(fgd);
        float f = Math.max(0.001f, Math.min(1f, frac));
        barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(10), f));
        barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(10), 1f - f));
        // Finom „feltöltődő" animáció: a kitöltés balról nő a helyére.
        if (Theme.animEnabled(this)) {
            fill.setPivotX(0f);
            fill.setScaleX(0f);
            fill.animate().scaleX(1f).setStartDelay(160).setDuration(680)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
        c.addView(barBg, new LinearLayout.LayoutParams(-1, -2));
        c.addView(gap(6));
        c.addView(text("Még " + toNext + " XP a(z) " + (lvl + 1) + ". szintig", 11.5f, MUTED, false));
        c.setClickable(true);
        c.setOnClickListener(v -> showLevelsSheet(lvl));
        levelBar.addView(c);
    }

    void showLevelsSheet(int currentLvl) {
        Sheet s = new Sheet(this, "Szintek ⭐", "XP-t az edzések percei és távja, a napi kihívás "
                + "(+10) és az étrend napi első bejegyzése (+5) adnak");
        for (int i = 1; i <= 10; i++) {
            String sub = "Szükséges XP: " + Levels.xpForLevel(i)
                    + (i == currentLvl ? "  ·  itt tartasz" : "");
            s.addRow("⭐", "Szint " + i + " – " + Levels.title(i), sub, i == currentLvl, false, null);
        }
        s.addCancel();
        s.show();
    }

    // Legutóbbi edzés kártya a főképernyőn – koppintásra a részletek nézet nyílik.
    void refreshRecent() {
        if (recentBox == null) return;
        recentBox.removeAllViews();
        JSONArray arr = History.load(this);
        if (arr.length() == 0) {
            // Barátságos üres állapot új felhasználónak.
            LinearLayout empty = card();
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            empty.addView(text("🚀 Itt jelennek meg az eredményeid", 15, TXT, true));
            empty.addView(text("Fejezd be az első edzésed, és itt látod majd a legutóbbi edzésed, rekordjaid és a naplód.", 12.5f, MUTED, false));
            recentBox.addView(empty);
            recentBox.addView(gap(14));
            return;
        }
        JSONObject o = null; long best = Long.MIN_VALUE;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e != null && e.optLong("ts") > best) { best = e.optLong("ts"); o = e; }
        }
        if (o == null) return;
        final long ts = o.optLong("ts");
        String name = o.optString("name", "");
        boolean isRun = name.isEmpty();
        String title = isRun ? "🏃 Futás" : "🏋️ " + name;
        String moodE = History.moodEmoji(o.optInt("mood", 0));
        if (!moodE.isEmpty()) title = title + "  " + moodE;
        int dur = o.optInt("dur");
        double dist = o.optDouble("dist", -1);
        int rounds = o.optInt("rounds", 0);
        String stat = "⏱ " + fmtLong(dur);
        if (isRun && dist >= 0) stat += "   ·   📍 " + fmtDist(dist);
        else if (rounds > 0) stat += "   ·   🔁 " + rounds + " kör";
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("MM.dd  HH:mm", new java.util.Locale("hu"));

        TextView head = text("🕘 Legutóbbi edzés", 13, 0xFF5FD0FF, true);
        head.setPadding(dp(2), dp(4), 0, dp(8));
        recentBox.addView(head);

        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout topRow = hbox();
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = text(title, 16, TXT, true);
        topRow.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        topRow.addView(text(df.format(new java.util.Date(ts)), 12, MUTED, false));
        c.addView(topRow);
        TextView s = text(stat, 13.5f, tAccent, false);
        s.setPadding(0, dp(6), 0, 0);
        c.addView(s);
        String note = o.optString("note", "");
        if (!note.isEmpty()) {
            TextView nt = text("📝 " + note, 12.5f, MUTED, false);
            nt.setMaxLines(2);
            nt.setEllipsize(android.text.TextUtils.TruncateAt.END);
            nt.setPadding(0, dp(6), 0, 0);
            c.addView(nt);
        }
        c.setClickable(true);
        c.setOnClickListener(v -> startActivity(new Intent(this, WorkoutDetailActivity.class).putExtra("ts", ts)));
        recentBox.addView(c);
        recentBox.addView(gap(14));
    }

    // ---- Heti aktivitás pontok ----

    void refreshWeekDots() {
        if (weekBox == null) return;
        weekBox.removeAllViews();
        JSONArray arr = activityLog();   // erősítő napok is beleszámítanak
        long ws = weekStartMs();
        boolean[] days = new boolean[7];
        int trained = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            long ts = o.optLong("ts");
            if (ts < ws) continue;
            long diff = ts - ws;
            int idx = (int) (diff / (24L * 3600 * 1000));
            if (idx >= 0 && idx < 7 && !days[idx]) { days[idx] = true; trained++; }
        }
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        int todayIdx = ((cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7); // H=0 … V=6

        // Edzésnapok terve: a tervezett napok jelölést kapnak, a fejléc pedig a
        // terv teljesítését mutatja (ha van beállított terv).
        boolean hasPlan = !Theme.planDays(this).isEmpty();
        boolean[] plan = new boolean[7];
        int plannedCount = 0, plannedDone = 0;
        for (int i = 0; i < 7; i++) {
            plan[i] = Theme.isPlanDay(this, i);
            if (hasPlan && plan[i]) { plannedCount++; if (days[i]) plannedDone++; }
        }

        LinearLayout c = card();
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        String headTxt;
        if (hasPlan && plannedCount > 0 && plannedDone >= plannedCount)
            headTxt = "🏆 Heti terv teljesítve!  ·  " + plannedDone + "/" + plannedCount + " edzésnap";
        else if (hasPlan)
            headTxt = "📅 Heti terv  ·  " + plannedDone + "/" + plannedCount + " edzésnap kész";
        else
            headTxt = "📅 Heti aktivitás  ·  " + trained + "/7 nap";
        TextView head = text(headTxt + "  ✎", 13, 0xFF7FE1A6, true);
        head.setPadding(dp(2), 0, 0, dp(10));
        // A fejlécre koppintva rögtön szerkeszthetők az edzésnapok.
        head.setClickable(true);
        head.setOnClickListener(v -> editPlanDaysSheet());
        c.addView(head);
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER);
        String[] labels = {"H", "K", "Sze", "Cs", "P", "Szo", "V"};
        for (int i = 0; i < 7; i++) {
            LinearLayout col = vbox();
            col.setGravity(Gravity.CENTER);
            TextView dot = text(days[i] ? "✓" : "", 14, days[i] ? 0xFFFFFFFF : MUTED, true);
            dot.setGravity(Gravity.CENTER);
            dot.setWidth(dp(34)); dot.setHeight(dp(34));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            if (days[i]) bg.setColor(tAccent);
            else {
                bg.setColor(0x14FFFFFF);
                if (i == todayIdx) bg.setStroke(dp(1), tAccent);
                else if (hasPlan && plan[i])
                    // Tervezett (még nem teljesített) nap: szaggatott akcent-gyűrű.
                    bg.setStroke(dp(1), (tAccent & 0x00FFFFFF) | 0xAA000000, dp(4), dp(3));
                else bg.setStroke(dp(1), GLASS_LINE);
            }
            dot.setBackground(bg);
            col.addView(dot);
            boolean isPlanDayLbl = hasPlan && plan[i];
            TextView lab = text(labels[i], 11,
                    i == todayIdx ? tAccent : (isPlanDayLbl ? TXT : MUTED),
                    i == todayIdx || isPlanDayLbl);
            lab.setGravity(Gravity.CENTER);
            lab.setPadding(0, dp(5), 0, 0);
            col.addView(lab);
            row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        c.addView(row);
        c.setClickable(true);
        c.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        c.setOnLongClickListener(v -> { editPlanDaysSheet(); return true; });
        weekBox.addView(c);
        weekBox.addView(gap(14));
    }

    /** Edzésnapok gyors szerkesztése a kezdőlapról (a Beállítások megnyitása nélkül). */
    void editPlanDaysSheet() {
        final boolean[] sel = new boolean[7];
        String cur = Theme.planDays(this);
        if (!cur.isEmpty()) for (String d : cur.split(",")) {
            try { int i = Integer.parseInt(d.trim()); if (i >= 0 && i < 7) sel[i] = true; }
            catch (Exception ignored) {}
        }
        String[] labels = {"H", "K", "Sze", "Cs", "P", "Szo", "V"};
        final TextView[] chips = new TextView[7];
        LinearLayout rowChips = hbox();
        rowChips.setGravity(Gravity.CENTER);
        rowChips.setPadding(0, dp(10), 0, dp(4));
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView ch = text(labels[i], 13, sel[i] ? 0xFFFFFFFF : MUTED, true);
            ch.setGravity(Gravity.CENTER);
            ch.setHeight(dp(40));
            styleDayChip(ch, sel[i]);
            ch.setClickable(true);
            ch.setOnClickListener(v -> {
                sel[idx] = !sel[idx];
                styleDayChip(chips[idx], sel[idx]);
                chips[idx].setTextColor(sel[idx] ? 0xFFFFFFFF : MUTED);
            });
            chips[i] = ch;
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
            clp.leftMargin = dp(2); clp.rightMargin = dp(2);
            rowChips.addView(ch, clp);
        }
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        box.addView(text("Jelöld ki, mely napokon tervezel edzeni. Üresen hagyva minden nap edzésnap.",
                12.5f, MUTED, false));
        box.addView(rowChips);
        new Sheet(this, "📅 Edzésnapok")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 7; i++) if (sel[i]) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(i);
                    }
                    prefs.edit().putString("plan_days", sb.toString()).apply();
                    BlazeWidget.refresh(this);
                    if (bannerSub != null) bannerSub.setText(bannerSubtitle());
                    refreshHome();
                })
                .addCancel()
                .show();
    }

    void styleDayChip(TextView ch, boolean on) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (on) bg.setColor(Theme.accent(this));
        else { bg.setColor(CARD2); bg.setStroke(dp(1), LINE); }
        ch.setBackground(bg);
    }

    // ---- Személyes rekordok ----

    void refreshRecords() {
        if (recordsBox == null) return;
        recordsBox.removeAllViews();
        JSONArray arr = History.load(this);
        if (arr.length() == 0) return;
        double maxDist = 0; int maxDur = 0, maxRounds = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            double d = o.optDouble("dist", 0); if (d > maxDist) maxDist = d;
            int dur = o.optInt("dur"); if (dur > maxDur) maxDur = dur;
            int r = o.optInt("rounds", 0); if (r > maxRounds) maxRounds = r;
        }
        int bestStreak = bestWeekStreak(arr);

        TextView head = text("🏆 Személyes rekordok", 13, 0xFFFFD166, true);
        head.setPadding(dp(2), dp(4), 0, dp(8));
        recordsBox.addView(head);

        LinearLayout c = card();
        c.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout r1 = hbox();
        r1.addView(recordChip("🏃", maxDist > 0 ? fmtDist(maxDist) : "—", "leghosszabb futás"), progChipLp());
        r1.addView(recordChip("⏱️", fmtLong(maxDur), "leghosszabb edzés"), progChipLp());
        c.addView(r1);
        c.addView(gap(8));
        LinearLayout r2 = hbox();
        r2.addView(recordChip("🔁", maxRounds > 0 ? String.valueOf(maxRounds) : "—", "legtöbb kör"), progChipLp());
        r2.addView(recordChip("🔥", bestStreak + " hét", "leghosszabb sorozat"), progChipLp());
        c.addView(r2);
        c.setClickable(true);
        c.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        recordsBox.addView(c);
        recordsBox.addView(gap(14));
    }

    View recordChip(String emoji, String value, String label) {
        LinearLayout c = vbox();
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(6), dp(10), dp(6), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), GLASS_LINE);
        c.setBackground(bg);
        TextView v = text(emoji + " " + value, 15, TXT, true); v.setGravity(Gravity.CENTER);
        TextView l = text(label, 10.5f, MUTED, false); l.setGravity(Gravity.CENTER);
        c.addView(v); c.addView(l);
        return c;
    }

    // ---- Kitüntetések ----

    java.util.HashSet<String> currentBadges() {
        // Egyesített napló: az erősítő napok is számítanak, ahogy a kezdőlapon
        // mutatott sorozat és edzésszám is – különben a felhasználó 7 napos
        // szériát látna, de nem kapná meg a hozzá tartozó jelvényt.
        JSONArray arr = History.loadAll(this);
        return Badges.earned(this, arr, bestWeekStreak(arr), prefs.getInt("challenge_done_count", 0), Streaks.planWeeks(this, arr));
    }

    void refreshBadges() {
        if (badgesBox == null) return;
        badgesBox.removeAllViews();
        java.util.HashSet<String> got = currentBadges();

        TextView head = text("🎖 Kitüntetések  ·  " + got.size() + "/" + Badges.ALL.length, 13, 0xFFB98CFF, true);
        head.setPadding(dp(2), dp(4), 0, dp(8));
        badgesBox.addView(head);

        LinearLayout c = card();
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout strip = hbox();
        strip.setGravity(Gravity.CENTER_VERTICAL);
        // Legfeljebb 6 jelvény a szalagon (megszerzettek elöl), a többi a lapon.
        int shown = 0;
        for (Badges.Badge b : Badges.ALL) {
            boolean earned = got.contains(b.id);
            if (!earned) continue;
            if (shown >= 6) break;
            strip.addView(badgeChip(b.emoji, true), badgeChipLp());
            shown++;
        }
        for (Badges.Badge b : Badges.ALL) {
            if (shown >= 6) break;
            if (got.contains(b.id)) continue;
            strip.addView(badgeChip("🔒", false), badgeChipLp());
            shown++;
        }
        c.addView(strip);
        TextView hint = text("Koppints a listáért · tartsd nyomva a megosztáshoz 📤", 11.5f, MUTED, false);
        hint.setPadding(dp(2), dp(10), 0, 0);
        c.addView(hint);
        c.setClickable(true);
        c.setOnClickListener(v -> showBadgesSheet());
        c.setOnLongClickListener(v -> { shareBadgesCard(); return true; });
        badgesBox.addView(c);
        badgesBox.addView(gap(14));
    }

    void shareBadgesCard() {
        try {
            Bitmap bmp = renderBadgesCard(currentBadges());
            ShareProvider.shareImage(this, bmp, "grit-kituntetesek");
        } catch (Exception ignored) {}
    }

    Bitmap renderBadgesCard(java.util.HashSet<String> got) {
        final int W = 1080, H = 1350, M = 80;
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
        tp.setTextSize(72);
        cv.drawText("Kitüntetéseim 🏅", M, 240, tp);

        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setColor(0xFFE11D2E);
        sp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sp.setTextSize(40);
        cv.drawText(got.size() + " / " + Badges.ALL.length + " megszerezve  ·  Grit", M, 300, sp);

        int cols = 2, gap = 24;
        int cardW = (W - 2 * M - gap) / cols;
        int cardH = 128;
        int startY = 360;
        Paint emo = new Paint(Paint.ANTI_ALIAS_FLAG); emo.setTextSize(56);
        Paint name = new Paint(Paint.ANTI_ALIAS_FLAG);
        name.setColor(0xFFF5ECEE);
        name.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        name.setTextSize(30);
        for (int i = 0; i < Badges.ALL.length; i++) {
            Badges.Badge b = Badges.ALL[i];
            boolean earned = got.contains(b.id);
            int cx = M + (i % cols) * (cardW + gap);
            int cy = startY + (i / cols) * (cardH + gap);
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(earned ? 0x2622E0FF : 0x0EFFFFFF);
            cv.drawRoundRect(new RectF(cx, cy, cx + cardW, cy + cardH), 24, 24, bg);
            emo.setAlpha(earned ? 255 : 90);
            cv.drawText(earned ? b.emoji : "🔒", cx + 28, cy + 82, emo);
            name.setAlpha(earned ? 255 : 120);
            cv.drawText(b.title, cx + 108, cy + 74, name);
        }

        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFFE11D2E);
        fp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        fp.setTextSize(36);
        fp.setTextAlign(Paint.Align.CENTER);
        cv.drawText("GRIT  ·  edzésnapló", W / 2f, H - 60, fp);
        return bmp;
    }

    View badgeChip(String emoji, boolean earned) {
        TextView t = text(emoji, 22, TXT, false);
        t.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(earned ? ((tAccent & 0xFFFFFF) | 0x33000000) : 0x14FFFFFF);
        bg.setStroke(dp(1), earned ? tAccent : GLASS_LINE);
        t.setBackground(bg);
        t.setWidth(dp(44)); t.setHeight(dp(44));
        if (!earned) t.setAlpha(0.6f);
        return t;
    }

    LinearLayout.LayoutParams badgeChipLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.rightMargin = dp(8);
        return p;
    }

    void showBadgesSheet() {
        java.util.HashSet<String> got = currentBadges();
        JSONArray arr = History.load(this);
        int count = arr.length();
        double totalM = 0, maxRun = 0; long totalSec = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            totalSec += o.optInt("dur");
            double d = o.optDouble("dist", 0);
            if (d > 0) { totalM += d; if (d > maxRun) maxRun = d; }
        }
        Sheet s = new Sheet(this, "Kitüntetések 🏅", got.size() + " / " + Badges.ALL.length + " megszerezve");
        for (Badges.Badge b : Badges.ALL) {
            boolean earned = got.contains(b.id);
            String sub = b.desc;
            if (!earned) {
                String hint = badgeHint(b.id, count, maxRun, totalM, totalSec);
                sub = hint != null ? b.desc + "  ·  " + hint : b.desc + "  (zárolva)";
            }
            s.addRow(earned ? b.emoji : "🔒", b.title, sub, earned, false, null);
        }
        s.addCancel();
        s.show();
    }

    // Haladás-felirat a még nem teljesített, számlálós kitüntetésekhez (pl. "45 / 50").
    String badgeHint(String id, int count, double maxRun, double totalM, long totalSec) {
        switch (id) {
            case "c5":      return count + " / 5 edzés";
            case "c10":     return count + " / 10 edzés";
            case "c25":     return count + " / 25 edzés";
            case "c50":     return count + " / 50 edzés";
            case "c100":    return count + " / 100 edzés";
            case "run5":    return fmtDist(maxRun) + " / 5 km (leghosszabb futás)";
            case "run10":   return fmtDist(maxRun) + " / 10 km (leghosszabb futás)";
            case "run21":   return fmtDist(maxRun) + " / 21 km (leghosszabb futás)";
            case "dist42":  return fmtDist(totalM) + " / 42 km összesen";
            case "dist100": return fmtDist(totalM) + " / 100 km összesen";
            case "time600": return (totalSec / 60) + " / 600 perc";
            default:        return null;
        }
    }

    // Edzés után: ha új kitüntetés született, ünnepeljük meg egy lappal.
    void checkNewBadges() {
        try {
            java.util.HashSet<String> got = currentBadges();
            java.util.Set<String> seen = prefs.getStringSet("badges_seen", new java.util.HashSet<>());
            java.util.List<Badges.Badge> fresh = new java.util.ArrayList<>();
            for (Badges.Badge b : Badges.ALL) {
                if (got.contains(b.id) && !seen.contains(b.id)) fresh.add(b);
            }
            // A jelenlegi állapot elmentése (akkor is, ha nincs új – idempotens).
            prefs.edit().putStringSet("badges_seen", new java.util.HashSet<>(got)).apply();
            if (fresh.isEmpty()) return;
            root.post(() -> {
                if (isFinishing()) return;
                Confetti.burst(root);
                Badges.Badge b = fresh.get(0);
                String sub = fresh.size() == 1 ? b.desc
                        : b.desc + "  (+" + (fresh.size() - 1) + " további)";
                new Sheet(this, b.emoji + "  " + b.title, "Új kitüntetés! " + sub)
                    .addPrimary("Szuper! 🎉", () -> {})
                    .show();
            });
        } catch (Exception ignored) {}
    }

    // A haladás-csík hosszan nyomva: megosztható „büszkeség-kártya" kép a szintről,
    // sorozatról és összesített statisztikákról (Instagram/Messenger…).
    void shareProgressCard() {
        try {
            JSONArray arr = History.loadAll(this);
            Bitmap bmp = renderProgressCard(arr);
            ShareProvider.shareImage(this, bmp, "grit-haladas");
        } catch (Exception ignored) {}
    }

    // A befejező képernyőn: az imént teljesített edzés megosztható kártyaként.
    void shareLastWorkout() {
        try {
            Bitmap bmp = renderWorkoutCard();
            ShareProvider.shareImage(this, bmp, "grit-edzes");
        } catch (Exception ignored) {}
    }

    Bitmap renderWorkoutCard() {
        final int W = 1080, H = 1350, M = 80;
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
        tp.setTextSize(78);
        cv.drawText("Grit", M, 250, tp);

        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setColor(0xFFE11D2E);
        sp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sp.setTextSize(44);
        String head = (lastWasRun ? "🏃 Futás" : "🏋️ Edzés") + "  ·  kész! ✔";
        cv.drawText(head, M, 320, sp);

        String[][] tiles = {
                {"Idő", fmtLong(lastDur)},
                {lastWasRun ? "Táv" : "Körök", lastWasRun ? fmtDist(lastDist) : String.valueOf(lastRounds)},
                {"Átlag", lastAvg > 0 ? fmtSpeed(lastAvg) : "—"},
                {"Kalória", lastCal + " kcal"},
                {"Lépések", lastSteps > 0 ? String.valueOf(lastSteps) : "—"},
                {"Rekord", (lastRecords != null && !lastRecords.isEmpty()) ? "🏆 Új!" : "—"},
        };

        int gap = 28, cols = 2;
        int cardW = (W - 2 * M - gap) / cols;
        int cardH = 200;
        int startY = 400;
        Paint cardBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBg.setColor(0x14FFFFFF);
        Paint val = new Paint(Paint.ANTI_ALIAS_FLAG);
        val.setColor(0xFFFFFFFF);
        val.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        val.setTextSize(54);
        Paint lab = new Paint(Paint.ANTI_ALIAS_FLAG);
        lab.setColor(0xFFA98F95);
        lab.setTextSize(34);
        for (int i = 0; i < tiles.length; i++) {
            int cx = M + (i % cols) * (cardW + gap);
            int cy = startY + (i / cols) * (cardH + gap);
            cv.drawRoundRect(new RectF(cx, cy, cx + cardW, cy + cardH), 28, 28, cardBg);
            cv.drawText(tiles[i][1], cx + 34, cy + 96, val);
            cv.drawText(tiles[i][0], cx + 34, cy + 150, lab);
        }

        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFFE11D2E);
        fp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        fp.setTextSize(38);
        fp.setTextAlign(Paint.Align.CENTER);
        cv.drawText("GRIT  ·  edzésnapló", W / 2f, H - 70, fp);
        return bmp;
    }

    Bitmap renderProgressCard(JSONArray arr) {
        final int W = 1080, H = 1350, M = 80;
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
        tp.setTextSize(78);
        cv.drawText("Grit", M, 250, tp);

        long xp = Levels.totalXp(this);
        int lvl = Levels.levelForXp(xp);
        int streak = weekStreak(arr);
        int count = arr.length();
        long totalSec = 0; double totalM = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            totalSec += o.optInt("dur");
            double d = o.optDouble("dist", 0); if (d > 0) totalM += d;
        }

        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setColor(0xFFE11D2E);
        sp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sp.setTextSize(46);
        cv.drawText("⭐ Szint " + lvl + " · " + Levels.title(lvl), M, 322, sp);

        String[][] tiles = {
                {"Szint", String.valueOf(lvl)},
                {"Sorozat", streak + " hét"},
                {"Edzés", String.valueOf(count)},
                {"Össz. idő", (totalSec / 60) + " perc"},
                {"Össz. táv", totalM >= 1000 ? String.format(new java.util.Locale("hu"), "%.1f km", totalM / 1000.0)
                        : Math.round(totalM) + " m"},
                {"XP", String.valueOf(xp)},
        };

        int gap = 28, cols = 2;
        int cardW = (W - 2 * M - gap) / cols;
        int cardH = 200;
        int startY = 400;
        Paint cardBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBg.setColor(0x14FFFFFF);
        Paint val = new Paint(Paint.ANTI_ALIAS_FLAG);
        val.setColor(0xFFFFFFFF);
        val.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        val.setTextSize(58);
        Paint lab = new Paint(Paint.ANTI_ALIAS_FLAG);
        lab.setColor(0xFFA98F95);
        lab.setTextSize(34);
        for (int i = 0; i < tiles.length; i++) {
            int cx = M + (i % cols) * (cardW + gap);
            int cy = startY + (i / cols) * (cardH + gap);
            cv.drawRoundRect(new RectF(cx, cy, cx + cardW, cy + cardH), 28, 28, cardBg);
            cv.drawText(tiles[i][1], cx + 34, cy + 96, val);
            cv.drawText(tiles[i][0], cx + 34, cy + 150, lab);
        }

        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFFE11D2E);
        fp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        fp.setTextSize(38);
        fp.setTextAlign(Paint.Align.CENTER);
        cv.drawText("GRIT  ·  edzésnapló", W / 2f, H - 70, fp);
        return bmp;
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
        addSeg(prep, 0xFFA98F95);
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
                if (p.custom) sb.append("\n(A kártyát hosszan nyomva szerkesztheted vagy törölheted.)");
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
        sheet.addNeutral("🧘  Nyújtás & mobilitás (videóval)", () -> startActivity(new Intent(this, MobilityActivity.class)));
        sheet.addCancel();
        sheet.show();
    }

    void saveProgram() {
        prefs.edit().putString("progname", programName).apply();
        updateProgramUI();
        vibrateShort();
    }

    void newProgramDialog() { editProgramDialog(null); }

    void editProgramDialog(final Programs.P existing) {
        LinearLayout box = vbox();
        box.setPadding(dp(6), dp(2), dp(6), dp(4));
        final EditText name = sheetInput("Program neve (pl. Reggeli torna)", false);
        if (existing != null) name.setText(existing.name);
        box.addView(name);
        box.addView(gap(12));
        TextView lbl = text("Gyakorlatok – soronként egy:", 13, MUTED, false);
        lbl.setPadding(dp(4), 0, 0, dp(6));
        box.addView(lbl);
        final EditText exs = sheetInput("Plank\nHasprés\nGuggolás", true);
        exs.setMinLines(4);
        exs.setGravity(Gravity.TOP);
        if (existing != null) {
            StringBuilder sb = new StringBuilder();
            for (String e : existing.ex) { if (sb.length() > 0) sb.append("\n"); sb.append(e); }
            exs.setText(sb.toString());
        }
        box.addView(exs);

        new Sheet(this, existing == null ? "Új saját program" : "Program szerkesztése")
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
                    if (existing != null) Programs.removeCustom(this, existing.name);
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
        new Sheet(this, p.title(), p.ex.length + " gyakorlat")
                .addPrimary("✏️  Szerkesztés", () -> editProgramDialog(p))
                .addDestructive("🗑  Törlés", () -> {
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

    View preset(String name, String sub, final int color, final int work, final int rest, final int rounds) {
        LinearLayout b = vbox();
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor((color & 0x00FFFFFF) | 0x14000000);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), (color & 0x00FFFFFF) | 0x44000000);
        b.setBackground(bg);
        b.setClickable(true);
        TextView t = text(name, 13.5f, color, true);   // preset neve a saját színében
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
        final int accent = stepColor(key);
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(15), dp(16), dp(15));

        // Fázis-szín sáv balra: minden sort jól elkülönít egymástól.
        View barV = new View(this);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(accent);
        barBg.setCornerRadius(dp(3));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(4), dp(38));
        barLp.rightMargin = dp(12);
        barV.setLayoutParams(barLp);
        barV.setBackground(barBg);
        row.addView(barV);

        LinearLayout labels = vbox();
        TextView titleTv = text(stepEmoji(key) + "  " + title, 15.5f, TXT, true);
        TextView subTv = text(sub, 12, MUTED, false);
        labels.addView(titleTv);
        labels.addView(subTv);
        if (key == WORK_K) { workRowTitle = titleTv; workRowSub = subTv; }
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        Button minus = stepButton("−", 0);           // semleges
        LinearLayout valCol = vbox();
        valCol.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView val = text("0", 22, accent, true);  // a fázis színében
        val.setGravity(Gravity.CENTER);
        val.setMinWidth(dp(52));
        val.setPadding(dp(4), dp(2), dp(4), dp(2));
        val.setOnClickListener(v -> showNumberDialog(key, min, max,
                title + (key == ROUND_K ? " (kör)" : " (mp)")));
        valueLabels[key] = val;
        TextView unit = text(key == ROUND_K ? "kör" : "mp", 11, MUTED, false);
        unit.setGravity(Gravity.CENTER);
        valCol.addView(val);
        valCol.addView(unit);
        Button plus = stepButton("+", accent);        // a fázis színében kiemelve

        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(-2, -2);
        valLp.leftMargin = dp(8);
        valLp.rightMargin = dp(8);

        attachStepper(minus, key, -1, min, max);
        attachStepper(plus, key, 1, min, max);

        row.addView(minus);
        row.addView(valCol, valLp);
        row.addView(plus);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    /** Fázisonként eltérő szín, hogy a beállító-sorok jól elkülönüljenek. */
    int stepColor(int key) {
        switch (key) {
            case WARM_K:  return 0xFFFFA24B;   // narancs – bemelegítés
            case PREP_K:  return 0xFFFFD166;   // sárga – előkészület
            case WORK_K:  return tWork;        // munka szín
            case REST_K:  return tRest;        // pihenő szín
            case ROUND_K: return tAccent;      // akcent – körök
            case COOL_K:  return 0xFF5FD0FF;   // világoskék – levezetés
            default:      return TXT;
        }
    }

    String stepEmoji(int key) {
        switch (key) {
            case WARM_K:  return "🔥";
            case PREP_K:  return "⏳";
            case WORK_K:  return "🏃";
            case REST_K:  return "💤";
            case ROUND_K: return "🔁";
            case COOL_K:  return "🧘";
            default:      return "";
        }
    }

    Button stepButton(String label, int tint) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(22);
        b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);
        b.setStateListAnimator(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (tint != 0) {
            // „+” gomb a fázis színében kiemelve (ez a fő művelet).
            bg.setColor((tint & 0x00FFFFFF) | 0x2E000000);
            bg.setStroke(dp(1), (tint & 0x00FFFFFF) | 0x77000000);
            b.setTextColor(tint);
        } else {
            bg.setColor(CARD2);
            bg.setStroke(dp(1), LINE);
            b.setTextColor(TXT);
        }
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
        highlightPresets();
    }

    // A jelenlegi beállításnak megfelelő preset kiemelése (ha egyezik).
    void highlightPresets() {
        if (presetViews[0] == null) return;
        for (int i = 0; i < presetViews.length; i++) {
            if (presetViews[i] == null) continue;
            boolean active = cfg[WORK_K] == presetSpecs[i][0]
                    && cfg[REST_K] == presetSpecs[i][1]
                    && cfg[ROUND_K] == presetSpecs[i][2];
            int pc = presetColors[i];
            GradientDrawable bg = new GradientDrawable();
            bg.setColor((pc & 0x00FFFFFF) | (active ? 0x40000000 : 0x14000000));
            bg.setCornerRadius(dp(14));
            bg.setStroke(dp(active ? 2 : 1), active ? pc : ((pc & 0x00FFFFFF) | 0x44000000));
            presetViews[i].setBackground(bg);
        }
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
                Theme.light(this) ? new int[]{0xFFF3F5FA, 0xFFEDF1F8} : new int[]{0xF2140B0D, 0xF7120A0C});
        runView.setBackground(runBg);

        // Teljes edzés-folyamat sáv (legfelül): hol tartunk az egész edzésben.
        FrameLayout obar = new FrameLayout(this);
        GradientDrawable otrack = new GradientDrawable();
        otrack.setColor(0x22FFFFFF); otrack.setCornerRadius(dp(3));
        obar.setBackground(otrack);
        overallFill = new View(this);
        GradientDrawable ofill = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{tAccent, tAccent2});
        ofill.setCornerRadius(dp(3));
        overallFill.setBackground(ofill);
        overallFill.setPivotX(0f);
        overallFill.setScaleX(0f);
        obar.addView(overallFill, new FrameLayout.LayoutParams(-1, dp(5)));
        LinearLayout.LayoutParams obarLp = new LinearLayout.LayoutParams(-1, dp(5));
        obarLp.bottomMargin = dp(14);
        runView.addView(obar, obarLp);

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
        statRemain = statCell(stats, "Hátra");
        statRemain.setTextColor(tAccent); // a hátralévő idő a kulcsinfó – kiemeljük
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
        skip.setContentDescription("Szakasz átugrása"); // képernyőolvasóknak
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

        // Blaze dicsérete (csak a befejező képernyőn látszik)
        blazePraise = text("", 14.5f, TXT, true);
        blazePraise.setGravity(Gravity.CENTER);
        blazePraise.setVisibility(View.GONE);
        GradientDrawable bpBg = new GradientDrawable();
        bpBg.setColor((tAccent & 0x00FFFFFF) | 0x1F000000);
        bpBg.setCornerRadius(dp(14));
        bpBg.setStroke(dp(1), (tAccent & 0x00FFFFFF) | 0x55000000);
        blazePraise.setBackground(bpBg);
        blazePraise.setPadding(dp(14), dp(12), dp(14), dp(12));
        // Blaze saját képe a dicséret mellett (kör alakban, ha elérhető).
        int bpImg = drawableId("blaze");
        if (bpImg != 0) {
            try {
                android.graphics.Bitmap raw =
                        android.graphics.BitmapFactory.decodeResource(getResources(), bpImg);
                if (raw != null && raw.getWidth() > 1) {
                    int s = dp(38);
                    android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(
                            s, s, android.graphics.Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas cv = new android.graphics.Canvas(out);
                    android.graphics.Paint pp =
                            new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                    pp.setShader(new android.graphics.BitmapShader(
                            android.graphics.Bitmap.createScaledBitmap(raw, s, s, true),
                            android.graphics.Shader.TileMode.CLAMP,
                            android.graphics.Shader.TileMode.CLAMP));
                    cv.drawCircle(s / 2f, s / 2f, s / 2f, pp);
                    blazePraise.setCompoundDrawablesWithIntrinsicBounds(
                            new android.graphics.drawable.BitmapDrawable(getResources(), out),
                            null, null, null);
                    blazePraise.setCompoundDrawablePadding(dp(10));
                    blazePraise.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                }
            } catch (Exception ignored) {}
        }
        LinearLayout.LayoutParams bpLp = new LinearLayout.LayoutParams(-1, -2);
        bpLp.topMargin = dp(12);
        runView.addView(blazePraise, bpLp);

        // Levezető nyújtás gomb (csak a befejező képernyőn látszik)
        cooldownBtn = ghostButton("🧘  Levezető nyújtás");
        cooldownBtn.setVisibility(View.GONE);
        cooldownBtn.setOnClickListener(v ->
                startRoutine(Mobility.COOLDOWN_NAMES, "Levezető nyújtás", 30, 5, 3));
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(-1, -2);
        cbLp.topMargin = dp(12);
        runView.addView(cooldownBtn, cbLp);

        // Megosztás gomb (csak a befejező képernyőn látszik)
        shareBtn = ghostButton("📤  Edzés megosztása");
        shareBtn.setVisibility(View.GONE);
        shareBtn.setOnClickListener(v -> shareLastWorkout());
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(-1, -2);
        sbLp.topMargin = dp(10);
        runView.addView(shareBtn, sbLp);

        // Újrakezdés gomb (csak a befejező képernyőn) – ugyanaz az edzés még egyszer.
        againBtn = ghostButton("🔁  Újra kezdés");
        againBtn.setVisibility(View.GONE);
        againBtn.setOnClickListener(v -> startWorkout());
        LinearLayout.LayoutParams agLp = new LinearLayout.LayoutParams(-1, -2);
        agLp.topMargin = dp(10);
        runView.addView(againBtn, agLp);

        // Hangulat-választó (csak a befejező képernyőn) – naplózza, milyen volt az edzés.
        moodRow = vbox();
        moodRow.setVisibility(View.GONE);
        TextView moodTitle = text("Milyen volt az edzés?", 13, MUTED, false);
        moodTitle.setGravity(Gravity.CENTER);
        moodTitle.setPadding(0, dp(16), 0, dp(8));
        moodRow.addView(moodTitle);
        LinearLayout moodBtns = hbox();
        moodBtns.setGravity(Gravity.CENTER);
        String[] emo = {"😣", "😐", "🙂", "💪"};
        String[] lab = {"Nehéz", "Rendben", "Jó", "Szuper"};
        moodChips = new TextView[4];
        for (int m = 0; m < 4; m++) {
            final int mood = m + 1;
            LinearLayout cell = vbox();
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(8), dp(6), dp(8), dp(6));
            TextView e = text(emo[m], 26, TXT, false);
            e.setGravity(Gravity.CENTER);
            TextView l = text(lab[m], 11, MUTED, false);
            l.setGravity(Gravity.CENTER);
            cell.addView(e); cell.addView(l);
            cell.setClickable(true);
            cell.setContentDescription("Hangulat: " + lab[m]); // képernyőolvasóknak
            cell.setOnClickListener(v -> pickMood(mood));
            moodChips[m] = e;
            moodBtns.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        moodRow.addView(moodBtns);
        Button noteBtn = ghostButton("📝  Jegyzet hozzáadása");
        LinearLayout.LayoutParams nbLp = new LinearLayout.LayoutParams(-1, -2);
        nbLp.topMargin = dp(10);
        noteBtn.setOnClickListener(v -> noteSheet());
        moodRow.addView(noteBtn, nbLp);
        runView.addView(moodRow, new LinearLayout.LayoutParams(-1, -2));

        // Görgethetővé tesszük, hogy kis kijelzőn se vágódjon le a tartalom
        // (a befejező képernyőn sok elem van). fillViewport: rövid tartalomnál
        // középre igazít, hosszúnál görgethető.
        runScroll = new ScrollView(this);
        runScroll.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        runScroll.setFillViewport(true);
        runScroll.setVerticalScrollBarEnabled(false);
        runView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        runScroll.addView(runView);
        return runScroll;
    }

    // Szöveges jegyzet felvétele a legutóbbi edzéshez egy alsó lapon.
    void noteSheet() {
        final EditText et = new EditText(this);
        et.setHint("Hogy ment? Írj egy rövid jegyzetet…");
        et.setText(History.latestNote(this));
        et.setTextColor(TXT);
        et.setHintTextColor(MUTED);
        et.setTextSize(15);
        et.setMinLines(3);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), GLASS_LINE);
        et.setBackground(bg);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        new Sheet(this, "Edzés jegyzet 📝", "Mentődik ehhez az edzéshez")
                .addCustom(et)
                .addPrimary("Mentés", () -> {
                    History.setNoteForLatest(this, et.getText().toString());
                    android.widget.Toast.makeText(this, "Jegyzet elmentve 📝", android.widget.Toast.LENGTH_SHORT).show();
                })
                .addCancel()
                .show();
    }

    // A kiválasztott hangulat mentése a legutóbbi edzéshez + vizuális visszajelzés.
    void pickMood(int mood) {
        History.setMoodForLatest(this, mood);
        if (moodChips != null) {
            for (int m = 0; m < moodChips.length; m++)
                moodChips[m].setAlpha(m == mood - 1 ? 1f : 0.35f);
        }
        refreshRecent();
        android.widget.Toast.makeText(this, "Elmentve a naplóba 📝", android.widget.Toast.LENGTH_SHORT).show();
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
        runScroll.setVisibility(run ? View.VISIBLE : View.GONE);
        if (bottomNavBar != null) bottomNavBar.setVisibility(run ? View.GONE : View.VISIBLE);
        int flag = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if (run && Theme.keepScreenOn(this)) getWindow().addFlags(flag);
        else getWindow().clearFlags(flag);
    }

    // ================= Service parancsok =================

    void startWorkout() {
        if (cfg[WORK_K] < 1) return;
        lastWasRoutine = false;
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
        if (blazePraise != null) blazePraise.setVisibility(View.GONE);
        cooldownBtn.setVisibility(View.GONE);
        shareBtn.setVisibility(View.GONE);
        if (againBtn != null) againBtn.setVisibility(View.GONE);
        if (moodRow != null) moodRow.setVisibility(View.GONE);
        if (overallFill != null) overallFill.setScaleX(0f);
        showRun(true);
    }

    /** Vezetett rutin indítása tetszőleges gyakorlatlistából (pl. nyújtás/bemelegítés). */
    void startRoutine(String[] names, String label, int work, int rest, int prep) {
        if (names == null || names.length == 0) return;
        lastWasRoutine = true;
        Intent i = new Intent(this, TimerService.class).setAction(TimerService.ACTION_START);
        i.putExtra(TimerService.EX_PREP, prep);
        i.putExtra(TimerService.EX_WORK, work);
        i.putExtra(TimerService.EX_REST, rest);
        i.putExtra(TimerService.EX_ROUNDS, 1);
        i.putExtra(TimerService.EX_WARM, 0);
        i.putExtra(TimerService.EX_COOL, 0);
        i.putExtra(TimerService.EX_WS, workSoundIdx);
        i.putExtra(TimerService.EX_RS, restSoundIdx);
        i.putExtra(TimerService.EX_TRACK, false);
        i.putExtra(TimerService.EX_CD, precount ? Theme.countdownSecs(this) : 0);
        i.putExtra(TimerService.EX_VIBE, Theme.vibrate(this));
        i.putExtra(TimerService.EX_VOICE, voice);
        i.putExtra(TimerService.EX_NAMES, names);
        i.putExtra(TimerService.EX_PNAME, label);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);

        lastPaused = false;
        finished = false;
        pauseBtn.setEnabled(true);
        pauseBtn.setText("Szünet");
        setPhaseUI(prep > 0 ? TimerService.T_PREP : TimerService.T_WORK, 1);
        timeText.setText(fmt(prep > 0 ? prep : work));
        ring.setProgress(1f);
        distanceText.setText("");
        exText.setVisibility(View.GONE);
        exDesc.setVisibility(View.GONE);
        nextText.setText("Következő: " + names[0]);
        recordText.setVisibility(View.GONE);
        levelText.setVisibility(View.GONE);
        if (blazePraise != null) blazePraise.setVisibility(View.GONE);
        cooldownBtn.setVisibility(View.GONE);
        shareBtn.setVisibility(View.GONE);
        if (againBtn != null) againBtn.setVisibility(View.GONE);
        if (moodRow != null) moodRow.setVisibility(View.GONE);
        if (overallFill != null) overallFill.setScaleX(0f);
        showRun(true);
    }

    /** Ha másik képernyő vezetett rutint kért (intent-extrákkal), elindítjuk. */
    void handleRoutineIntent(Intent intent) {
        if (intent == null) return;
        String[] names = intent.getStringArrayExtra("r_names");
        if (names == null || names.length == 0) return;
        String label = intent.getStringExtra("r_label");
        int work = intent.getIntExtra("r_work", 30);
        int rest = intent.getIntExtra("r_rest", 5);
        int prep = intent.getIntExtra("r_prep", 5);
        intent.removeExtra("r_names"); // ne induljon újra forgatás/visszatérés után
        startRoutine(names, label != null ? label : "Mobilitás", work, rest, prep);
    }

    // Egy korábbi edzés beállításainak betöltése (a részletek nézetből „megismétlés").
    void handleRepeatIntent(Intent intent) {
        if (intent == null) return;
        long rts = intent.getLongExtra("repeat_ts", 0);
        if (rts == 0) return;
        intent.removeExtra("repeat_ts");
        JSONArray arr = History.load(this);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null || o.optLong("ts") != rts) continue;
            int work = o.optInt("work", cfg[WORK_K]);
            int rest = o.optInt("rest", cfg[REST_K]);
            int rounds = o.optInt("rounds", cfg[ROUND_K]);
            if (work > 0) cfg[WORK_K] = work;
            cfg[REST_K] = Math.max(0, rest);
            if (rounds > 0) cfg[ROUND_K] = rounds;
            // Csak akkor állítjuk be a programot, ha valódi (ismert) program neve;
            // különben sima futás módra esünk vissza (pl. korábbi mobilitás rutin).
            String nm = o.optString("name", "");
            programName = Programs.byName(this, nm) != null ? nm : "";
            prefs.edit().putString("progname", programName).apply();
            saveAll();
            refreshValues();
            updateProgramUI();
            updateTotal();
            refreshPlanBar();
            showRun(false);
            android.widget.Toast.makeText(this, "Edzés betöltve – nyomd meg az Indítást ▶",
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
    }

    // Gyorsindítás a widget ▶ gombjáról: azonnal elindítja az edzést a jelenlegi
    // beállításokkal. Ha már fut edzés, nem indít rá újat.
    void handleQuickStartIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra("quick_start", false)) return;
        intent.removeExtra("quick_start");
        if (TimerService.activeNow) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!TimerService.activeNow && !isFinishing()) startWorkout();
        }, 400);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRoutineIntent(intent);
        handleRepeatIntent(intent);
        handleQuickStartIntent(intent);
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
        if (runScroll != null && runScroll.getVisibility() == View.VISIBLE) {
            // A befejező képernyőn a Vissza egyszerűen visszavisz a főképernyőre
            // (nincs mit „leállítani"); edzés közben viszont megerősítést kérünk.
            if (finished) { showRun(false); refreshHome(); }
            else confirmStop();
        } else super.onBackPressed();
    }

    // ================= Broadcast fogadás =================

    final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            if (TimerService.B_TICK.equals(a)) onTick(i);
            else if (TimerService.B_DONE.equals(a)) onDone(i);
            else if (TimerService.B_STOPPED.equals(a)) { showRun(false); refreshHome(); checkNewBadges(); BlazeWidget.refresh(c); }
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
            // Pihenő / előkészület / bemelegítés közben a KÖVETKEZŐ gyakorlat leírását
            // mutatjuk, hogy fel lehessen készülni a technikára, mielőtt elkezdődik.
            boolean prePhase = phase == TimerService.T_REST
                    || phase == TimerService.T_PREP
                    || phase == TimerService.T_WARMUP;
            String nextDesc = (prePhase && nextName != null)
                    ? Programs.descOf(nextName) : "";
            if (!nextDesc.isEmpty()) {
                exDesc.setText(nextDesc);
                exDesc.setVisibility(View.VISIBLE);
            } else {
                exDesc.setVisibility(View.GONE);
            }
        }
        nextText.setText(nextName != null ? "Következő: " + nextName : "");
        boolean tickChanged = remain != lastRemainShown;
        lastRemainShown = remain;
        timeText.setText(fmt(remain));
        // Az utolsó 3 másodpercben a szám lüktet – a csipogással szinkronban,
        // hogy vizuálisan is érezni lehessen a visszaszámlálás feszültségét.
        if (tickChanged && !paused && remain > 0 && remain <= 3 && Theme.animEnabled(this)) pulseTime();
        ring.setProgress(prog);
        float avgSpeed = i.getFloatExtra(TimerService.EX_AVGSPEED, -1);
        distanceText.setText(dist >= 0
                ? "📍 " + fmtDist(dist) + "   ·   " + fmtSpeed(speed)
                        + (avgSpeed > 0 ? "   ·   ⌀ " + fmtSpeed(avgSpeed) : "")
                : "");

        int elapsed = i.getIntExtra(TimerService.EX_ELAPSED, 0);
        int steps = i.getIntExtra(TimerService.EX_STEPS, 0);
        int cal = i.getIntExtra(TimerService.EX_CAL, 0);
        int totalRemain = i.getIntExtra(TimerService.EX_TOTALREMAIN, 0);
        int totalDur = elapsed + totalRemain;
        if (overallFill != null)
            overallFill.setScaleX(totalDur > 0 ? Math.max(0f, Math.min(1f, (float) elapsed / totalDur)) : 0f);
        statElapsed.setText(fmtLong(elapsed));
        statRemain.setText(fmtLong(totalRemain));
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
        lastDur = dur; lastDist = dist; lastRounds = rounds; lastCal = cal;
        lastSteps = steps; lastAvg = avg; lastWasRun = dist >= 0;
        showRun(true);
        finished = true;
        phaseLabel.setText("KÉSZ");
        phaseLabel.setTextColor(DONE);
        exText.setVisibility(View.GONE);
        exDesc.setVisibility(View.GONE);
        nextText.setText("Elmentve a naplóba ✔");
        ring.setColor(DONE);
        ring.setProgress(1f);
        timeText.animate().cancel();
        timeText.setText("✓");
        if (Theme.animEnabled(this)) {
            timeText.setScaleX(0.6f); timeText.setScaleY(0.6f);
            timeText.animate().scaleX(1f).scaleY(1f).setDuration(460)
                    .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
        } else {
            timeText.setScaleX(1f); timeText.setScaleY(1f);
        }
        roundInfo.setText(rounds + " kör kész 💪");
        String line = "Idő: " + fmtLong(dur);
        if (dist >= 0) line += "  ·  📍 " + fmtDist(dist);
        if (avg > 0) line += "  ·  ⌀ " + fmtSpeed(avg);
        distanceText.setText(line);
        statElapsed.setText(fmtLong(dur));
        statRemain.setText("0:00");
        if (overallFill != null) overallFill.setScaleX(1f);
        statCal.setText(cal + " kcal");
        statSteps.setText(steps > 0 ? String.valueOf(steps) : "—");

        String records = i.getStringExtra(TimerService.EX_RECORDS);
        lastRecords = records != null ? records : "";
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

        // Ünneplő konfetti új rekordnál vagy szintlépésnél.
        if ((records != null && !records.isEmpty()) || (levelup != null && !levelup.isEmpty()))
            Confetti.burst(root);

        lastPaused = false;
        pauseBtn.setEnabled(false);
        pauseBtn.setText("Kész");
        // Blaze személyre szabott dicsérete (széria- és mérföldkő-tudatos).
        if (blazePraise != null) {
            JSONArray actArr = activityLog();
            int dsNow = dayStreak(actArr);
            // Ha Blaze saját képe ott van a kártyán, nem kell elé az emoji.
            String pfx = blazePraise.getCompoundDrawables()[0] != null ? "" : "🐺 ";
            String praise = pfx + Mascot.praiseFinish(
                    prefs.getString("user_name", ""), dsNow, actArr.length());
            // Regenerációs tipp annak, aki fehérje-célt vezet: edzés után ez a
            // leghasznosabb következő lépés, és átvisz az Étrendre egy koppintással.
            boolean proteinTip = false;
            try {
                int pGoal = prefs.getInt("protein_goal", 0);
                if (pGoal > 0) {
                    int eaten = (int) Math.round(MealLog.todayProtein(this));
                    if (eaten < pGoal) {
                        praise += "\n\n🥩 Mára még " + (pGoal - eaten)
                                + " g fehérje van hátra – koppints ide a naplózáshoz.";
                        proteinTip = true;
                    }
                }
            } catch (Exception ignored) {}
            blazePraise.setClickable(proteinTip);
            blazePraise.setOnClickListener(proteinTip
                    ? v -> startActivity(new Intent(this, DietActivity.class)) : null);
            blazePraise.setText(praise);
            blazePraise.setVisibility(View.VISIBLE);
            blazePraise.setAlpha(0f);
            blazePraise.animate().alpha(1f).setStartDelay(350).setDuration(420).start();
            // Széria-mérföldkő (3/7/14/30/50/100 nap): külön konfetti-ünneplés.
            boolean milestone = dsNow == 3 || dsNow == 7 || dsNow == 14
                    || dsNow == 30 || dsNow == 50 || dsNow == 100;
            if (milestone && (records == null || records.isEmpty())
                    && (levelup == null || levelup.isEmpty()))
                Confetti.burst(root);
        }
        cooldownBtn.setVisibility(View.VISIBLE);
        shareBtn.setVisibility(View.VISIBLE);
        if (againBtn != null) againBtn.setVisibility(lastWasRoutine ? View.GONE : View.VISIBLE);
        if (moodRow != null) {
            moodRow.setVisibility(View.VISIBLE);
            if (moodChips != null) for (TextView t : moodChips) t.setAlpha(1f);
        }
        refreshHome();
        checkNewBadges();
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
        if (Theme.rev(this) != builtRev && (runScroll == null || runScroll.getVisibility() != View.VISIBLE)) {
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
        refreshHome();
        BlazeWidget.refresh(this);   // a widget üzenete is frissüljön az app megnyitásakor
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            try { unregisterReceiver(rx); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        // A widget vegye át az esetleg megváltozott edzés-beállítást (▶ leírás).
        BlazeWidget.refresh(this);
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
        bg.setCornerRadius(dp(14));   // valamivel szögletesebb = keményebb, „grit" karakter
        b.setBackground(bg);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(18);
        b.setLetterSpacing(0.03f);
        b.setElevation(dp(5));
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
        bg.setCornerRadius(dp(13));   // szögletesebb, tömörebb megjelenés
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

    /** „Grit" logó: kézjegy-szerű, írott (aláírás) stílusú felirat, a végén
     *  karmazsin ponttal – összhangban az aláírásos app-ikonnal. */
    TextView gritWordmark(float sizeSp) {
        TextView t = new TextView(this);
        String s = "Grit.";
        SpannableString sp = new SpannableString(s);
        sp.setSpan(new ForegroundColorSpan(0xFFE11D2E), 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        t.setText(sp);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(sizeSp);
        // Írott, „aláírás" jellegű betűtípus (kézjegy-hatás).
        t.setTypeface(Typeface.create("cursive", Typeface.BOLD_ITALIC));
        t.setIncludeFontPadding(false);
        return t;
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
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float progress = 1f;
        private int fgColor = WORK;

        ProgressRing(Context c) {
            super(c);
            bgPaint.setStyle(Paint.Style.STROKE);
            bgPaint.setColor(CARD2);
            fgPaint.setStyle(Paint.Style.STROKE);
            fgPaint.setStrokeCap(Paint.Cap.ROUND);
            fgPaint.setColor(WORK);
            // Lágy fényudvar: szélesebb, áttetsző ív a fő ív alatt.
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeCap(Paint.Cap.ROUND);
            // A vezető pont ("üstökösfej") és a fehér magja prémium hatásért.
            dotPaint.setStyle(Paint.Style.FILL);
            corePaint.setStyle(Paint.Style.FILL);
            corePaint.setColor(0xFFFFFFFF);
        }

        void setProgress(float p) { progress = Math.max(0f, Math.min(1f, p)); invalidate(); }
        void setColor(int col) { fgColor = col; fgPaint.setColor(col); invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float stroke = w * 0.058f;
            bgPaint.setStrokeWidth(stroke);
            fgPaint.setStrokeWidth(stroke);
            glowPaint.setStrokeWidth(stroke * 1.9f);
            glowPaint.setColor((fgColor & 0x00FFFFFF) | 0x55000000); // ~33% alfa, azonos szín
            float pad = stroke / 2f + w * 0.02f;
            rect.set(pad, pad, w - pad, getHeight() - pad);
            float sweep = 360f * progress;
            canvas.drawArc(rect, 0, 360, false, bgPaint);
            if (progress > 0f) {
                canvas.drawArc(rect, -90, sweep, false, glowPaint);
                canvas.drawArc(rect, -90, sweep, false, fgPaint);
                // Vezető pont a haladás csúcsán.
                double ang = Math.toRadians(-90 + sweep);
                float cx = rect.centerX(), cy = rect.centerY();
                float radius = rect.width() / 2f;
                float dx = cx + (float) (radius * Math.cos(ang));
                float dy = cy + (float) (radius * Math.sin(ang));
                dotPaint.setColor(fgColor);
                canvas.drawCircle(dx, dy, stroke * 0.62f, dotPaint);
                canvas.drawCircle(dx, dy, stroke * 0.26f, corePaint);
            }
        }
    }
}
