package com.edzo.idozito;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

/**
 * A háttérben futó időzítő. Foreground service + részleges wake lock, így akkor is
 * megy (és sípol), amikor a képernyő ki van kapcsolva. GPS-szel méri a lefutott távot,
 * és broadcast-okban küldi az állapotot a MainActivity-nek.
 */
public class TimerService extends Service {

    // Parancsok (Activity -> Service)
    public static final String ACTION_START = "com.edzo.idozito.START";
    public static final String ACTION_PAUSE = "com.edzo.idozito.PAUSE";
    public static final String ACTION_RESUME = "com.edzo.idozito.RESUME";
    public static final String ACTION_STOP = "com.edzo.idozito.STOP";
    public static final String ACTION_STOP_SAVE = "com.edzo.idozito.STOP_SAVE";
    public static final String ACTION_SKIP = "com.edzo.idozito.SKIP";
    public static final String ACTION_ADD_TIME = "com.edzo.idozito.ADD_TIME";
    public static final String ACTION_SYNC = "com.edzo.idozito.SYNC";

    // Broadcastok (Service -> Activity)
    public static final String B_TICK = "com.edzo.idozito.TICK";
    public static final String B_DONE = "com.edzo.idozito.DONE";
    public static final String B_STOPPED = "com.edzo.idozito.STOPPED";

    // Extra kulcsok
    public static final String EX_PREP = "prep", EX_WORK = "work", EX_REST = "rest",
            EX_ROUNDS = "rounds", EX_WS = "ws", EX_RS = "rs", EX_TRACK = "track", EX_CD = "cd",
            EX_VIBE = "vibe", EX_VOICE = "voice", EX_NAMES = "names", EX_PNAME = "pname",
            EX_WARM = "warm", EX_COOL = "cool", EX_DELTA = "delta";
    public static final String EX_PHASE = "phase", EX_REMAIN = "remain", EX_ROUND = "round",
            EX_PROGRESS = "prog", EX_DIST = "dist", EX_PAUSED = "paused", EX_DUR = "dur",
            EX_SPEED = "speed", EX_ELAPSED = "elapsed", EX_STEPS = "steps", EX_CAL = "cal",
            EX_STEPNAME = "stepname", EX_NEXTNAME = "nextname", EX_RECORDS = "records",
            EX_LEVELUP = "levelup", EX_TOTALREMAIN = "totalremain", EX_AVGSPEED = "avgspd";

    public static final int T_PREP = 0, T_WORK = 1, T_REST = 2, T_WARMUP = 3, T_COOLDOWN = 4;

    static final String CHANNEL = "edzo_timer";
    static final int NOTIF_ID = 42;

    private static final class Step {
        int type, dur, round;
        String label; // gyakorlat neve (null = sima futás)
        Step(int t, int d, int r) { type = t; dur = d; round = r; }
        Step(int t, int d, int r, String l) { type = t; dur = d; round = r; label = l; }
    }

    // Konfiguráció
    private int prep, work, rest, rounds, workSound, restSound, warm, cool;
    private int cdSecs = 3;
    private boolean track, vibeOn = true, voice;
    private String[] exNames;    // gyakorlatnevek (null/üres = sima futás)
    private String programName;  // program neve a naplóhoz

    // Beszéd (TTS)
    private TextToSpeech tts;
    private boolean ttsReady;
    private boolean triedGoogleTts;   // a Google motor jóval jobb magyar hangot ad

    // Állapot
    private final ArrayList<Step> plan = new ArrayList<>();
    private int idx;
    private boolean running, paused;
    /** Fut-e éppen edzés (a widget-gyorsindítás dupla indítás ellen ellenőrzi). */
    public static volatile boolean activeNow;
    private long stepEndElapsed, remainingAtPause;
    private int lastShownSec;
    private int completedRounds; // teljesített (végigment) körök száma
    private boolean halfwayCheered; // félúti biztatás csak egyszer hangozzon el
    private int completedWork;   // teljesített munka-szakaszok (gyakorlatok) száma

    // Időmérés
    private long sessionStart, pausedAccum, pauseStart;

    // Segédek
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager nm;
    private AudioManager audioManager;
    private Object focusRequest; // AudioFocusRequest (API 26+)

    // GPS
    private LocationManager lm;
    private Location lastLoc;
    private double distanceM = -1; // -1 = nincs mérés
    private boolean tracking;
    private LocationListener locListener;
    private double maxSpeedMps, curSpeedMps, prevSpeedMps;
    private boolean prevSpeedValid; // volt-e érvényes előző sebességminta (tüskeszűréshez)
    private long lastFixElapsed;
    private boolean inWorkPhase;      // csak FUTÁS szakaszban mérünk sebességet/távot
    private long workMs, lastTickElapsed; // futással töltött idő (átlaghoz)
    private int lastKm;               // utoljára bejelentett teljes kilométer
    private int lastKmTimeSec;         // futásidő az előző km-jelzésnél (a rész-tempóhoz)

    // Bővített mérések
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SensorEventListener stepListener;
    private int steps;
    private long movingMs;
    private double elevGainM;
    private Double lastAlt;
    private double weightKg = 70;
    private JSONArray trackPts;
    private long lastTrackElapsed;
    private double lastTrackDist;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
        ticker = this::tick;
        Beeper.masterVolume = Theme.volume(this);
        initTts();
    }

    private void initTts() {
        // Előbb kifejezetten a Google TTS motort próbáljuk: ennek a magyar hangja
        // jóval természetesebb és érthetőbb, mint a legtöbb gyári (pl. Samsung/Pico)
        // motoré, amely gyakran „idegen akcentussal" olvassa a magyar szöveget.
        triedGoogleTts = true;
        try {
            tts = new TextToSpeech(this, this::onTtsInit, "com.google.android.tts");
        } catch (Exception e) {
            triedGoogleTts = false;
            try { tts = new TextToSpeech(this, this::onTtsInit); } catch (Exception ignored) {}
        }
    }

    private void onTtsInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            // A Google motor nem elérhető – essünk vissza a rendszer alapértelmezettjére.
            if (retryWithDefaultEngine()) return;
            return;
        }
        try {
            int r = tts.setLanguage(new Locale("hu", "HU"));
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                // A jelenlegi motor nem tud magyarul – próbáljuk a rendszer motorját,
                // hogy ne egy idegen nyelvű hang olvassa fel a magyar szöveget.
                if (retryWithDefaultEngine()) return;
                tts.setLanguage(Locale.getDefault());
            }
            selectBestHungarianVoice();
            // A beszédsebesség a Beállításokban állítható; semleges hangmagasság.
            tts.setSpeechRate(Theme.speechRate(this));
            tts.setPitch(1.0f);
        } catch (Exception ignored) {}
        ttsReady = true;
    }

    /** Ha a Google motorral próbálkoztunk és nem jött be, egyszer újrapróbáljuk a
     *  rendszer alapértelmezett motorjával. true = újraindítás folyamatban. */
    private boolean retryWithDefaultEngine() {
        if (!triedGoogleTts) return false;
        triedGoogleTts = false;
        try { if (tts != null) tts.shutdown(); } catch (Exception ignored) {}
        tts = null;
        try { tts = new TextToSpeech(this, this::onTtsInit); return true; } catch (Exception ignored) {}
        return false;
    }

    /** A legjobb elérhető magyar hang kiválasztása: lehetőleg helyben (hálózat nélkül)
     *  elérhető és a legmagasabb minőségű. Enélkül a motor néha egy gyenge, alap
     *  hangot használ. */
    private void selectBestHungarianVoice() {
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null) return;
            Voice best = null;
            for (Voice v : voices) {
                if (v == null || v.getLocale() == null) continue;
                if (!"hun".equalsIgnoreCase(v.getLocale().getISO3Language())) continue;
                if (v.getFeatures() != null
                        && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                if (best == null) { best = v; continue; }
                boolean vLocal = !v.isNetworkConnectionRequired();
                boolean bLocal = !best.isNetworkConnectionRequired();
                if (vLocal != bLocal) { if (vLocal) best = v; continue; }
                if (v.getQuality() > best.getQuality()) best = v;
            }
            if (best != null) tts.setVoice(best);
        } catch (Exception ignored) {}
    }

    private void speak(String s) {
        if (!voice || !ttsReady || tts == null) return;
        try { tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "edzo"); } catch (Exception ignored) {}
    }

    /** Sorba állított bemondás – nem vágja el a szakaszváltás bemondását. */
    private void speakAdd(String s) {
        if (!voice || !ttsReady || tts == null) return;
        try { tts.speak(s, TextToSpeech.QUEUE_ADD, null, "edzo_km"); } catch (Exception ignored) {}
    }

    /** Minden teljes kilométernél: rezgés + hangos bejelentés a futásidővel. */
    private void announceKm(int km) {
        buzz(new long[]{0, 50, 80, 50});
        int sec = (int) (workMs / 1000);
        int split = sec - lastKmTimeSec; // az utolsó kilométer ideje (rész-tempó)
        lastKmTimeSec = sec;
        String t = sec >= 60 ? (sec / 60) + " perc " + (sec % 60) + " másodperc" : sec + " másodperc";
        String msg = km + " kilométer. Futásidő: " + t + ".";
        if (split > 0) {
            String pace = split >= 60 ? (split / 60) + " perc " + (split % 60) + " másodperc"
                    : split + " másodperc";
            msg += " Az utolsó kilométer: " + pace + ".";
        }
        speakAdd(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) { stopEverything(); return START_NOT_STICKY; }

        switch (action) {
            case ACTION_START:
                prep = intent.getIntExtra(EX_PREP, 10);
                work = intent.getIntExtra(EX_WORK, 10);
                rest = intent.getIntExtra(EX_REST, 30);
                rounds = intent.getIntExtra(EX_ROUNDS, 8);
                warm = intent.getIntExtra(EX_WARM, 0);
                cool = intent.getIntExtra(EX_COOL, 0);
                workSound = intent.getIntExtra(EX_WS, 2);
                restSound = intent.getIntExtra(EX_RS, 1);
                track = intent.getBooleanExtra(EX_TRACK, false);
                cdSecs = intent.getIntExtra(EX_CD, 3);
                vibeOn = intent.getBooleanExtra(EX_VIBE, true);
                voice = intent.getBooleanExtra(EX_VOICE, false);
                exNames = intent.getStringArrayExtra(EX_NAMES);
                programName = intent.getStringExtra(EX_PNAME);
                startWorkout();
                break;
            case ACTION_PAUSE: pause(); break;
            case ACTION_RESUME: resume(); break;
            case ACTION_SKIP: skipStep(); break;
            case ACTION_ADD_TIME: addTime(intent.getIntExtra(EX_DELTA, 0)); break;
            case ACTION_SYNC: broadcastTick(); break;
            case ACTION_STOP:
                sendBroadcast(new Intent(B_STOPPED).setPackage(getPackageName()));
                stopEverything();
                break;
            case ACTION_STOP_SAVE:
                if (running) saveSession(completedRounds);
                sendBroadcast(new Intent(B_STOPPED).setPackage(getPackageName()));
                stopEverything();
                break;
        }
        return START_NOT_STICKY;
    }

    // ---------------- Edzés folyamat ----------------

    private void startWorkout() {
        buildPlan();
        if (plan.isEmpty()) { stopEverything(); return; }
        running = true;
        activeNow = true;
        paused = false;
        halfwayCheered = false;
        idx = 0;
        completedRounds = 0;
        completedWork = 0;
        sessionStart = SystemClock.elapsedRealtime();
        pausedAccum = 0;
        distanceM = track ? 0 : -1;
        maxSpeedMps = 0;
        curSpeedMps = 0;
        prevSpeedMps = 0;
        prevSpeedValid = false;
        lastFixElapsed = 0;
        steps = 0;
        movingMs = 0;
        workMs = 0;
        lastTickElapsed = 0;
        lastKm = 0;
        lastKmTimeSec = 0;
        inWorkPhase = false;
        elevGainM = 0;
        lastAlt = null;
        trackPts = new JSONArray();
        lastTrackElapsed = 0;
        lastTrackDist = -1;
        double lw = Profile.lastWeight(this);
        weightKg = lw > 0 ? lw : 70;
        stepEndElapsed = SystemClock.elapsedRealtime() + (long) plan.get(0).dur * 1000L;
        startForeground(NOTIF_ID, buildNotification());
        acquireWakeLock();
        requestAudioFocus();
        startSteps();
        if (track) startLocation();
        beginStep(true);
        handler.post(ticker);
    }

    private void buildPlan() {
        plan.clear();
        if (warm > 0) plan.add(new Step(T_WARMUP, warm, 0));
        if (prep > 0) plan.add(new Step(T_PREP, prep, 0));
        int len = exLen();
        int n = rounds * len; // összes munka-szakasz (gyakorlat vagy futás)
        for (int k = 0; k < n; k++) {
            int round = k / len + 1;
            String label = exNames != null && exNames.length > 0 ? exNames[k % len] : null;
            plan.add(new Step(T_WORK, work, round, label));
            if (rest > 0 && k < n - 1) plan.add(new Step(T_REST, rest, round));
        }
        if (cool > 0) plan.add(new Step(T_COOLDOWN, cool, 0));
    }

    /** Gyakorlatok száma egy körben (1 = sima futás mód). */
    private int exLen() {
        return exNames != null && exNames.length > 0 ? exNames.length : 1;
    }

    private void beginStep(boolean first) {
        Step s = plan.get(idx);
        stepEndElapsed = SystemClock.elapsedRealtime() + (long) s.dur * 1000L;
        lastShownSec = -1;
        inWorkPhase = (s.type == T_WORK);
        if (inWorkPhase) { lastLoc = null; lastFixElapsed = 0; } // GPS-alap nullázása a futás elején
        boolean lastRound = s.round == rounds;
        if (s.type == T_WORK) {
            Beeper.play(workSound); buzz(new long[]{0, 60, 60, 60});
            if (s.label != null) speak(s.label + "!");
            else speak(lastRound ? "Utolsó kör. Futás!" : "Futás!");
            // Félútnál egyszeri extra biztatás (legalább 4 körös edzésnél).
            if (!halfwayCheered && !lastRound && rounds >= 4 && s.round == rounds / 2 + 1) {
                halfwayCheered = true;
                String[] cheer = {
                        "Félúton túl vagy. Hajrá, ne állj meg!",
                        "A nehezén túl vagy. Innen már hazafelé megy!",
                        "Félidő! Erős vagy, tartsd a tempót!",
                        "A fele megvan. A falka veled fut!",
                        "Félidő! Minden kör közelebb visz a célhoz!",
                };
                speakAdd(cheer[(int) (Math.random() * cheer.length)]);
            }
        } else if (s.type == T_REST) {
            Beeper.play(restSound); buzz(120);
            String nx = nextWorkLabel();
            speak(nx != null ? "Pihenő. Következik: " + nx + "." : "Pihenő.");
        } else if (s.type == T_PREP) {
            if (!first) { Beeper.play(restSound); buzz(120); }
            speak("Felkészülés.");
        } else if (s.type == T_WARMUP) {
            Beeper.play(restSound); buzz(120);
            speak("Bemelegítés. Kezdjük lazán.");
        } else if (s.type == T_COOLDOWN) {
            Beeper.play(restSound); buzz(120);
            speak("Levezetés. Lassíts le.");
        }
        postNotification();
        broadcastTick();
    }

    /** A következő munka-szakasz gyakorlatneve (null, ha nincs több vagy sima futás). */
    private String nextWorkLabel() {
        for (int i = idx + 1; i < plan.size(); i++) {
            if (plan.get(i).type == T_WORK) return plan.get(i).label;
        }
        return null;
    }

    private void tick() {
        if (!running || paused) return;
        // Futással töltött idő gyűjtése (csak FUTÁS szakaszban).
        long nowT = SystemClock.elapsedRealtime();
        if (lastTickElapsed > 0 && plan.get(idx).type == T_WORK) {
            long d = nowT - lastTickElapsed;
            if (d > 0 && d < 2000) workMs += d;
        }
        lastTickElapsed = nowT;

        Step s = plan.get(idx);
        double remain = (stepEndElapsed - SystemClock.elapsedRealtime()) / 1000.0;

        if (remain <= 0) {
            if (plan.get(idx).type == T_WORK) {
                completedWork++;
                completedRounds = completedWork / exLen();
            }
            idx++;
            if (idx >= plan.size()) { finishWorkout(); return; }
            beginStep(false);
            handler.postDelayed(ticker, 40);
            return;
        }

        int shown = (int) Math.ceil(remain);
        if (shown != lastShownSec) {
            lastShownSec = shown;
            if (cdSecs > 0 && shown <= cdSecs && shown >= 1) { Beeper.tick(); buzz(30); }
            postNotification();
        }
        broadcastTick();
        handler.postDelayed(ticker, 200);
    }

    /** Az aktuális szakasz átugrása (a lejárt munka-szakasz teljesítettnek számít). */
    private void skipStep() {
        if (!running) return;
        if (paused) resume();
        Step s = plan.get(idx);
        if (s.type == T_WORK) {
            completedWork++;
            completedRounds = completedWork / exLen();
        }
        idx++;
        if (idx >= plan.size()) { finishWorkout(); return; }
        beginStep(false);
    }

    /** Az aktuális szakasz hosszának módosítása menet közben (±mp). */
    private void addTime(int deltaSec) {
        if (!running || deltaSec == 0) return;
        long dm = deltaSec * 1000L;
        if (paused) {
            remainingAtPause = Math.max(1000, remainingAtPause + dm);
        } else {
            long remain = stepEndElapsed - SystemClock.elapsedRealtime();
            remain = Math.max(1000, remain + dm);
            stepEndElapsed = SystemClock.elapsedRealtime() + remain;
            lastShownSec = -1;
        }
        buzz(20);
        postNotification();
        broadcastTick();
    }

    private void pause() {
        if (!running || paused) return;
        paused = true;
        remainingAtPause = stepEndElapsed - SystemClock.elapsedRealtime();
        pauseStart = SystemClock.elapsedRealtime();
        handler.removeCallbacks(ticker);
        lastTickElapsed = 0; // szünet ne számítson bele a futásidőbe
        speak("Szünet.");
        postNotification();
        broadcastTick();
    }

    private void resume() {
        if (!running || !paused) return;
        paused = false;
        stepEndElapsed = SystemClock.elapsedRealtime() + remainingAtPause;
        pausedAccum += SystemClock.elapsedRealtime() - pauseStart;
        lastLoc = null; // ne számítson bele a szünet alatti helyváltozás
        lastTickElapsed = 0;
        postNotification();
        handler.post(ticker);
    }

    private int currentDurationSec() {
        return (int) ((SystemClock.elapsedRealtime() - sessionStart - pausedAccum) / 1000);
    }

    private void saveSession(int roundsDone) {
        double maxKmh = distanceM >= 0 ? maxSpeedMps * 3.6 : -1;
        // Átlagsebesség CSAK a futással töltött időből.
        double avgKmh = (distanceM > 0 && workMs > 0) ? distanceM / (workMs / 1000.0) * 3.6 : -1;
        long ts = System.currentTimeMillis();
        History.add(this, ts, currentDurationSec(), distanceM, roundsDone, work, rest, maxKmh,
                steps, (int) (movingMs / 1000), elevGainM, estimateCalories(), avgKmh, programName);
        if (trackPts != null && trackPts.length() > 0) SessionStore.save(this, ts, trackPts);
    }

    private void finishWorkout() {
        running = false;
        activeNow = false;
        handler.removeCallbacks(ticker);
        Beeper.finish();
        buzz(new long[]{0, 120, 80, 120, 80, 240});
        int durationSec = currentDurationSec();
        String records = computeRecords();     // a mentés ELŐTT, a korábbi edzésekhez képest
        String levelUp = computeLevelUp();
        saveSession(rounds);
        // Blaze név szerint, széria-tudatosan dicsér (a mentés után, hogy a mai is számítson).
        String uname = getSharedPreferences("edzo", MODE_PRIVATE)
                .getString("user_name", "").trim();
        String who = uname.isEmpty() ? "" : ", " + uname;
        int dsNow = Streaks.current(this, History.loadAll(this));
        if (!records.isEmpty()) {
            speak("Edzés kész. Új rekord! Szép munka" + who + "!");
            buzz(new long[]{0, 90, 60, 90, 60, 90, 60, 350});
        } else if (dsNow >= 2) {
            speak("Edzés kész. Szép munka" + who + "! " + dsNow + " napos széria, ég a láng!");
        } else {
            speak("Edzés kész. Szép munka" + who + "!");
        }
        // Futásnál hangos összefoglaló: táv + átlagtempó (a képernyő nézése nélkül is).
        if (distanceM > 0 && workMs > 0) {
            String km = String.format(new Locale("hu"), "%.1f", distanceM / 1000.0);
            int ps = (int) Math.round((workMs / 1000.0) / (distanceM / 1000.0)); // mp/km
            String pace = (ps / 60) + " perc " + (ps % 60) + " másodperc";
            speakAdd("Összesen " + km + " kilométer, átlagtempó " + pace + " per kilométer.");
        }

        Intent done = new Intent(B_DONE).setPackage(getPackageName());
        done.putExtra(EX_DUR, durationSec);
        done.putExtra(EX_DIST, distanceM);
        done.putExtra(EX_ROUND, rounds);
        done.putExtra(EX_CAL, (int) Math.round(estimateCalories()));
        done.putExtra(EX_STEPS, steps);
        done.putExtra(EX_SPEED, (float) ((distanceM > 0 && workMs > 0)
                ? distanceM / (workMs / 1000.0) * 3.6 : -1));
        done.putExtra(EX_RECORDS, records);
        done.putExtra(EX_LEVELUP, levelUp);
        sendBroadcast(done);

        stopEverything();
    }

    /** Új személyes rekordok a KORÁBBI edzésekhez képest (mentés előtt hívandó). */
    private String computeRecords() {
        JSONArray h = History.load(this);
        if (h.length() == 0) return "első edzés";
        double bestDist = 0, bestSpeed = 0, bestCal = 0;
        int bestDur = 0;
        for (int i = 0; i < h.length(); i++) {
            org.json.JSONObject o = h.optJSONObject(i);
            if (o == null) continue;
            bestDist = Math.max(bestDist, o.optDouble("dist", -1));
            bestDur = Math.max(bestDur, o.optInt("dur"));
            double av = o.optDouble("avgspeed", -1);
            if (av < 0) { double d = o.optDouble("dist", -1); int du = o.optInt("dur"); if (d > 0 && du > 0) av = d / du * 3.6; }
            bestSpeed = Math.max(bestSpeed, av);
            bestCal = Math.max(bestCal, o.optDouble("cal", 0));
        }
        int newDur = currentDurationSec();
        double newSpeed = (distanceM > 0 && workMs > 0) ? distanceM / (workMs / 1000.0) * 3.6 : -1;
        double newCal = estimateCalories();
        StringBuilder r = new StringBuilder();
        if (distanceM > 0 && distanceM > bestDist + 1) append(r, "leghosszabb táv");
        if (bestDur > 0 && newDur > bestDur) append(r, "leghosszabb idő");
        if (newSpeed > 0 && newSpeed > bestSpeed + 0.05) append(r, "leggyorsabb átlag");
        if (bestCal > 0 && newCal > bestCal + 1) append(r, "legtöbb kalória");
        return r.toString();
    }

    private void append(StringBuilder sb, String s) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(s);
    }

    /** Ha az edzés XP-je új szintre emel, visszaadja az új szint címét; különben "". */
    private String computeLevelUp() {
        // Egyesített napló + bónusz-XP, hogy a kijelzett szinttel egyezzen.
        long prevXp = Levels.totalXp(this);
        int prevLvl = Levels.levelForXp(prevXp);
        long newXp = prevXp + Levels.xpForSession(currentDurationSec(), distanceM);
        int newLvl = Levels.levelForXp(newXp);
        return newLvl > prevLvl ? newLvl + "|" + Levels.title(newLvl) : "";
    }

    private void stopEverything() {
        running = false;
        activeNow = false;
        paused = false;
        handler.removeCallbacks(ticker);
        stopLocation();
        stopSteps();
        releaseWakeLock();
        abandonAudioFocus();
        stopForeground(true);
        stopSelf();
    }

    private double estimateCalories() {
        double km = distanceM > 0 ? distanceM / 1000.0 : 0;
        if (km > 0) return weightKg * km * 1.036; // futás közelítés
        double min = currentDurationSec() / 60.0;
        return 6.0 * 3.5 * weightKg / 200.0 * min; // MET~6 mozgás
    }

    // ---------------- Lépésérzékelő ----------------

    private boolean hasActivityPermission() {
        return Build.VERSION.SDK_INT < 29 ||
                checkSelfPermission("android.permission.ACTIVITY_RECOGNITION")
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void startSteps() {
        try {
            if (!hasActivityPermission()) return;
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null) return;
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if (stepSensor == null) return;
            stepListener = new SensorEventListener() {
                @Override public void onSensorChanged(SensorEvent e) {
                    if (!paused && e.values.length > 0) steps += (int) e.values[0];
                }
                @Override public void onAccuracyChanged(Sensor s, int a) {}
            };
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception ignored) {}
    }

    private void stopSteps() {
        try {
            if (sensorManager != null && stepListener != null) sensorManager.unregisterListener(stepListener);
        } catch (Exception ignored) {}
        stepListener = null;
    }

    // ---------------- Broadcast ----------------

    private void broadcastTick() {
        if (plan.isEmpty() || idx >= plan.size()) return;
        Step s = plan.get(idx);
        double remain = paused
                ? remainingAtPause / 1000.0
                : (stepEndElapsed - SystemClock.elapsedRealtime()) / 1000.0;
        if (remain < 0) remain = 0;
        Intent i = new Intent(B_TICK).setPackage(getPackageName());
        i.putExtra(EX_PHASE, s.type);
        i.putExtra(EX_REMAIN, (int) Math.ceil(remain));
        i.putExtra(EX_ROUND, Math.max(1, s.round));
        i.putExtra(EX_ROUNDS, rounds);
        i.putExtra(EX_PROGRESS, (float) (s.dur > 0 ? remain / s.dur : 0));
        i.putExtra(EX_DIST, distanceM);
        i.putExtra(EX_SPEED, (float) (paused ? 0 : curSpeedMps * 3.6));
        // Élő átlagsebesség (a futással töltött idő alapján), hogy menet közben is
        // látszódjon az összteljesítmény, ne csak a végén.
        i.putExtra(EX_AVGSPEED, (float) ((distanceM > 0 && workMs > 0)
                ? distanceM / (workMs / 1000.0) * 3.6 : -1));
        i.putExtra(EX_ELAPSED, (int) ((SystemClock.elapsedRealtime() - sessionStart - pausedAccum) / 1000));
        i.putExtra(EX_STEPS, steps);
        i.putExtra(EX_CAL, (int) Math.round(estimateCalories()));
        i.putExtra(EX_PAUSED, paused);
        // Teljes hátralévő idő: az aktuális szakasz maradéka + a hátralévő szakaszok.
        int futureSec = 0;
        for (int j = idx + 1; j < plan.size(); j++) futureSec += plan.get(j).dur;
        i.putExtra(EX_TOTALREMAIN, (int) Math.ceil(remain) + futureSec);
        if (s.label != null) i.putExtra(EX_STEPNAME, s.label);
        String nx = nextWorkLabel();
        if (nx != null) i.putExtra(EX_NEXTNAME, nx);
        sendBroadcast(i);
    }

    // ---------------- GPS ----------------

    private boolean hasLocationPermission() {
        return Build.VERSION.SDK_INT < 23 ||
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocation() {
        if (!hasLocationPermission()) { distanceM = -1; return; }
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) { distanceM = -1; return; }
        lastLoc = null;
        locListener = new LocationListener() {
            @Override public void onLocationChanged(Location loc) {
                long now = SystemClock.elapsedRealtime();
                // Csak a FUTÁS szakaszban mérünk; pihenő/szünet alatt nem gyűjtünk sebességet/távot.
                if (paused || !inWorkPhase) { curSpeedMps = 0; prevSpeedValid = false; lastLoc = loc; lastFixElapsed = now; return; }
                if (loc.hasAccuracy() && loc.getAccuracy() > 40) return;
                double sp = loc.hasSpeed() ? loc.getSpeed() : -1;
                if (lastLoc != null) {
                    float d = lastLoc.distanceTo(loc);
                    if (d < 250) distanceM += d;
                    int km = (int) (distanceM / 1000);
                    if (km > lastKm) { lastKm = km; announceKm(km); }
                    if (sp < 0 && lastFixElapsed > 0) {
                        double dt = (now - lastFixElapsed) / 1000.0;
                        if (dt > 0.2) sp = d / dt;
                    }
                }
                // Sebesség: csak jó pontosságú fixből fogadjuk el (a táv laza 40 m-t tűr,
                // de a sebesség sokkal érzékenyebb a GPS-zajra), és futáshoz reális tartományban.
                boolean goodForSpeed = !loc.hasAccuracy() || loc.getAccuracy() <= 25;
                // Ha van sebesség-pontosság (API 26+), a bizonytalan mintát is eldobjuk.
                if (goodForSpeed && android.os.Build.VERSION.SDK_INT >= 26
                        && loc.hasSpeedAccuracy() && loc.getSpeedAccuracyMetersPerSecond() > 2.5f)
                    goodForSpeed = false;
                if (sp >= 0 && sp < 11 && goodForSpeed) { // ~40 km/h abszolút plafon
                    curSpeedMps = sp;
                    // A MAX sebességet csak akkor frissítjük, ha a magas érték KÉT egymást
                    // követő fixből is kijön (a két olvasat kisebbikét vesszük). Így egyetlen
                    // GPS-tüske nem tud irreális csúcsot okozni (pl. 30 km/h futás közben).
                    if (prevSpeedValid) {
                        double sustained = Math.min(sp, prevSpeedMps);
                        if (sustained > maxSpeedMps) maxSpeedMps = sustained;
                    }
                    prevSpeedMps = sp;
                    prevSpeedValid = true;
                } else {
                    prevSpeedValid = false; // megszakadt folytonosság → új tüske sem számít önmagában
                }
                // Mozgásidő (amíg ténylegesen halad)
                if (lastFixElapsed > 0) {
                    long dtMs = now - lastFixElapsed;
                    if (dtMs > 0 && dtMs < 6000 && curSpeedMps > 0.6) movingMs += dtMs;
                }
                // Emelkedő (pozitív magasságváltozás, zajszűréssel)
                if (loc.hasAltitude()) {
                    double alt = loc.getAltitude();
                    if (lastAlt != null && alt - lastAlt > 0.7) elevGainM += alt - lastAlt;
                    lastAlt = alt;
                }
                // Útvonal mintavételezés (2 mp-enként vagy 5 m-enként)
                boolean far = lastTrackDist < 0 || (distanceM - lastTrackDist) >= 5;
                if (trackPts != null && (trackPts.length() == 0 || (now - lastTrackElapsed) >= 2000 || far)) {
                    try {
                        JSONArray pt = new JSONArray();
                        pt.put(workMs / 1000); // futásidő (mp), hogy a splitek a futásból számoljanak
                        pt.put(Math.round(loc.getLatitude() * 1e6) / 1e6);
                        pt.put(Math.round(loc.getLongitude() * 1e6) / 1e6);
                        pt.put(loc.hasAltitude() ? Math.round(loc.getAltitude()) : 0);
                        pt.put((int) distanceM);
                        pt.put(Math.round(curSpeedMps * 10) / 10.0);
                        trackPts.put(pt);
                    } catch (org.json.JSONException ignored) {}
                    lastTrackElapsed = now;
                    lastTrackDist = distanceM;
                }
                lastLoc = loc;
                lastFixElapsed = now;
            }
            @Override public void onStatusChanged(String p, int s, android.os.Bundle e) {}
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
        };
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locListener, Looper.getMainLooper());
            tracking = true;
        } catch (SecurityException | IllegalArgumentException e) {
            distanceM = -1;
        }
    }

    private void stopLocation() {
        if (lm != null && locListener != null && tracking) {
            try { lm.removeUpdates(locListener); } catch (Exception ignored) {}
        }
        tracking = false;
    }

    // ---------------- Wake lock ----------------

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "edzo:timer");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        } catch (Exception ignored) {}
    }

    private void releaseWakeLock() {
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
        wakeLock = null;
    }

    // ---------------- Audio-fókusz (zene halkítása) ----------------

    private void requestAudioFocus() {
        if (!Theme.duckMusic(this)) return;
        try {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                android.media.AudioAttributes attrs = new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                android.media.AudioFocusRequest req =
                        new android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                                .setAudioAttributes(attrs)
                                .build();
                focusRequest = req;
                audioManager.requestAudioFocus(req);
            } else {
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            }
        } catch (Exception ignored) {}
    }

    private void abandonAudioFocus() {
        try {
            if (audioManager == null) return;
            if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) {
                audioManager.abandonAudioFocusRequest((android.media.AudioFocusRequest) focusRequest);
            } else {
                audioManager.abandonAudioFocus(null);
            }
        } catch (Exception ignored) {}
        focusRequest = null;
    }

    // ---------------- Értesítés ----------------

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Edzés folyamatban",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
        }
    }

    private PendingIntent actionIntent(String action, int rc) {
        Intent i = new Intent(this, TimerService.class).setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(this, rc, i, flags);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open, flags);

        String text;
        if (plan.isEmpty() || idx >= plan.size()) {
            text = "Edzés";
        } else {
            Step s = plan.get(idx);
            double remain = paused ? remainingAtPause / 1000.0
                    : (stepEndElapsed - SystemClock.elapsedRealtime()) / 1000.0;
            if (remain < 0) remain = 0;
            String name = s.type == T_PREP ? "Előkészület"
                    : s.type == T_WARMUP ? "Bemelegítés"
                    : s.type == T_COOLDOWN ? "Levezetés"
                    : s.type == T_WORK ? (s.label != null ? s.label : "Futás") : "Pihenő";
            text = name + " · " + (int) Math.ceil(remain) + " mp · Kör "
                    + Math.max(1, s.round) + "/" + rounds + (paused ? " · szünet" : "");
        }

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        b.setContentTitle("Grit")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openPi);

        if (running) {
            if (paused) {
                b.addAction(android.R.drawable.ic_media_play, "Folytatás", actionIntent(ACTION_RESUME, 1));
            } else {
                b.addAction(android.R.drawable.ic_media_pause, "Szünet", actionIntent(ACTION_PAUSE, 2));
            }
            b.addAction(android.R.drawable.ic_media_next, "Következő", actionIntent(ACTION_SKIP, 4));
            b.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Leállítás", actionIntent(ACTION_STOP, 3));
        }
        return b.build();
    }

    private void postNotification() {
        try { nm.notify(NOTIF_ID, buildNotification()); } catch (Exception ignored) {}
    }

    // ---------------- Rezgés ----------------

    private void buzz(long ms) {
        if (!vibeOn || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26)
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        else vibrator.vibrate(ms);
    }

    private void buzz(long[] pattern) {
        if (!vibeOn || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        else vibrator.vibrate(pattern, -1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activeNow = false;
        handler.removeCallbacks(ticker);
        stopLocation();
        stopSteps();
        releaseWakeLock();
        abandonAudioFocus();
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
