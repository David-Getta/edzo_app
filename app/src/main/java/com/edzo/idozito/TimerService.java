package com.edzo.idozito;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.ArrayList;

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
    public static final String ACTION_SYNC = "com.edzo.idozito.SYNC";

    // Broadcastok (Service -> Activity)
    public static final String B_TICK = "com.edzo.idozito.TICK";
    public static final String B_DONE = "com.edzo.idozito.DONE";
    public static final String B_STOPPED = "com.edzo.idozito.STOPPED";

    // Extra kulcsok
    public static final String EX_PREP = "prep", EX_WORK = "work", EX_REST = "rest",
            EX_ROUNDS = "rounds", EX_WS = "ws", EX_RS = "rs", EX_TRACK = "track", EX_PRE = "pre";
    public static final String EX_PHASE = "phase", EX_REMAIN = "remain", EX_ROUND = "round",
            EX_PROGRESS = "prog", EX_DIST = "dist", EX_PAUSED = "paused", EX_DUR = "dur";

    public static final int T_PREP = 0, T_WORK = 1, T_REST = 2;

    static final String CHANNEL = "edzo_timer";
    static final int NOTIF_ID = 42;

    private static final class Step {
        int type, dur, round;
        Step(int t, int d, int r) { type = t; dur = d; round = r; }
    }

    // Konfiguráció
    private int prep, work, rest, rounds, workSound, restSound;
    private boolean track, precount;

    // Állapot
    private final ArrayList<Step> plan = new ArrayList<>();
    private int idx;
    private boolean running, paused;
    private long stepEndElapsed, remainingAtPause;
    private int lastShownSec;

    // Időmérés
    private long sessionStart, pausedAccum, pauseStart;

    // Segédek
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager nm;

    // GPS
    private LocationManager lm;
    private Location lastLoc;
    private double distanceM = -1; // -1 = nincs mérés
    private boolean tracking;
    private LocationListener locListener;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
        ticker = this::tick;
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
                workSound = intent.getIntExtra(EX_WS, 2);
                restSound = intent.getIntExtra(EX_RS, 1);
                track = intent.getBooleanExtra(EX_TRACK, false);
                precount = intent.getBooleanExtra(EX_PRE, true);
                startWorkout();
                break;
            case ACTION_PAUSE: pause(); break;
            case ACTION_RESUME: resume(); break;
            case ACTION_SYNC: broadcastTick(); break;
            case ACTION_STOP:
                sendBroadcast(new Intent(B_STOPPED).setPackage(getPackageName()));
                stopEverything();
                break;
        }
        return START_NOT_STICKY;
    }

    // ---------------- Edzés folyamat ----------------

    private void startWorkout() {
        startForeground(NOTIF_ID, buildNotification("Edzés indul", ""));
        acquireWakeLock();
        buildPlan();
        if (plan.isEmpty()) { stopEverything(); return; }
        running = true;
        paused = false;
        idx = 0;
        sessionStart = SystemClock.elapsedRealtime();
        pausedAccum = 0;
        distanceM = track ? 0 : -1;
        if (track) startLocation();
        beginStep(true);
        handler.post(ticker);
    }

    private void buildPlan() {
        plan.clear();
        if (prep > 0) plan.add(new Step(T_PREP, prep, 0));
        for (int r = 1; r <= rounds; r++) {
            plan.add(new Step(T_WORK, work, r));
            if (rest > 0 && r < rounds) plan.add(new Step(T_REST, rest, r));
        }
    }

    private void beginStep(boolean first) {
        Step s = plan.get(idx);
        stepEndElapsed = SystemClock.elapsedRealtime() + (long) s.dur * 1000L;
        lastShownSec = -1;
        if (s.type == T_WORK) { Beeper.play(workSound); buzz(new long[]{0, 60, 60, 60}); }
        else if (s.type == T_REST) { Beeper.play(restSound); buzz(120); }
        else if (s.type == T_PREP && !first) { Beeper.play(restSound); buzz(120); }
        broadcastTick();
    }

    private void tick() {
        if (!running || paused) return;
        Step s = plan.get(idx);
        double remain = (stepEndElapsed - SystemClock.elapsedRealtime()) / 1000.0;

        if (remain <= 0) {
            idx++;
            if (idx >= plan.size()) { finishWorkout(); return; }
            beginStep(false);
            handler.postDelayed(ticker, 40);
            return;
        }

        int shown = (int) Math.ceil(remain);
        if (shown != lastShownSec) {
            lastShownSec = shown;
            if (precount && shown <= 3 && shown >= 1) { Beeper.tick(); buzz(30); }
            updateNotification(s, shown);
        }
        broadcastTick();
        handler.postDelayed(ticker, 200);
    }

    private void pause() {
        if (!running || paused) return;
        paused = true;
        remainingAtPause = stepEndElapsed - SystemClock.elapsedRealtime();
        pauseStart = SystemClock.elapsedRealtime();
        handler.removeCallbacks(ticker);
        broadcastTick();
    }

    private void resume() {
        if (!running || !paused) return;
        paused = false;
        stepEndElapsed = SystemClock.elapsedRealtime() + remainingAtPause;
        pausedAccum += SystemClock.elapsedRealtime() - pauseStart;
        lastLoc = null; // ne számítson bele a szünet alatti helyváltozás
        handler.post(ticker);
    }

    private void finishWorkout() {
        running = false;
        handler.removeCallbacks(ticker);
        Beeper.finish();
        buzz(new long[]{0, 120, 80, 120, 80, 240});
        int durationSec = (int) ((SystemClock.elapsedRealtime() - sessionStart - pausedAccum) / 1000);
        History.add(this, System.currentTimeMillis(), durationSec, distanceM, rounds, work, rest);

        Intent done = new Intent(B_DONE).setPackage(getPackageName());
        done.putExtra(EX_DUR, durationSec);
        done.putExtra(EX_DIST, distanceM);
        done.putExtra(EX_ROUND, rounds);
        sendBroadcast(done);

        stopEverything();
    }

    private void stopEverything() {
        running = false;
        paused = false;
        handler.removeCallbacks(ticker);
        stopLocation();
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
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
        i.putExtra(EX_PAUSED, paused);
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
                if (paused) { lastLoc = loc; return; }
                if (loc.hasAccuracy() && loc.getAccuracy() > 40) return;
                if (lastLoc != null) {
                    float d = lastLoc.distanceTo(loc);
                    if (d < 250) distanceM += d;
                }
                lastLoc = loc;
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

    private Notification buildNotification(String title, String text) {
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        b.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi);
        return b.build();
    }

    private void updateNotification(Step s, int remain) {
        String name = s.type == T_PREP ? "Előkészület" : s.type == T_WORK ? "Futás" : "Pihenő";
        String text = name + " · " + remain + " mp · Kör " + Math.max(1, s.round) + "/" + rounds;
        try { nm.notify(NOTIF_ID, buildNotification("Edző Időzítő", text)); } catch (Exception ignored) {}
    }

    // ---------------- Rezgés ----------------

    private void buzz(long ms) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26)
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        else vibrator.vibrate(ms);
    }

    private void buzz(long[] pattern) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        else vibrator.vibrate(pattern, -1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(ticker);
        stopLocation();
        releaseWakeLock();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
