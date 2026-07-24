package com.edzo.idozito;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Blaze, a kabalafigura naponta egyszer (este) magától rákérdez: ha aznap még
 * nem edzettél, motiváló értesítést küld a saját hangján. Alapból bekapcsolva,
 * a Beállításokban kikapcsolható. Ha aznap már edzettél, nem nyaggat.
 */
public class DailyNudgeReceiver extends BroadcastReceiver {

    static final String CHANNEL = "edzo_blaze";
    static final int REQ = 88, NOTIF_ID = 9200;

    /** A beállított napi időpontra ütemez, majd naponta ismétel. Ha ki van kapcsolva, törli. */
    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent base = new Intent(c, DailyNudgeReceiver.class).setAction("com.edzo.idozito.BLAZE_NUDGE");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ, base, flags);
        if (!Theme.blazeNudge(c)) {
            try { am.cancel(pi); } catch (Exception ignored) {}
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, Theme.nudgeHour(c));
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, 1);
        try {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
        } catch (Exception ignored) {}
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (!Theme.blazeNudge(c)) return;
        if (workedOutToday(c)) return;   // ma már edzett – nem nyaggatjuk

        String userName = c.getSharedPreferences("edzo", Context.MODE_PRIVATE).getString("user_name", "");
        // A tegnapig tartó napi széria: ha van, Blaze kifejezetten arra figyelmeztet,
        // hogy ne szakadjon meg ("ne hagyd kihunyni a X napos lángod").
        int streak = streakBeforeToday(c);
        String text = Mascot.nudge(userName, streak >= 1, streak);

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Blaze üzenetei",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("A kabalafigura napi motiváló üzenetei");
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(c, REQ, open, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL)
                : new Notification.Builder(c);
        b.setContentTitle("Blaze 🐺🔥")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify(NOTIF_ID, b.build());
    }

    /** Hány egymást követő napon edzett tegnappal bezárólag (ma nélkül). */
    private static int streakBeforeToday(Context c) {
        long dayMs = 24L * 60 * 60 * 1000;
        long todayStart = todayStart();
        JSONArray h = History.load(c);
        int streak = 0;
        for (int k = 1; k <= 365; k++) {
            long from = todayStart - k * dayMs, to = todayStart - (k - 1) * dayMs;
            boolean hit = false;
            for (int i = 0; i < h.length(); i++) {
                JSONObject o = h.optJSONObject(i);
                long ts = o == null ? 0 : o.optLong("ts");
                if (ts >= from && ts < to) { hit = true; break; }
            }
            if (!hit) break;
            streak++;
        }
        return streak;
    }

    private static boolean workedOutToday(Context c) {
        long start = todayStart();
        JSONArray h = History.load(c);
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o != null && o.optLong("ts") >= start) return true;
        }
        return false;
    }

    private static long todayStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
