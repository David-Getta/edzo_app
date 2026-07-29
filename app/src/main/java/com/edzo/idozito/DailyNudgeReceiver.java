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


import java.util.Calendar;

/**
 * Blaze, a kabalafigura naponta egyszer (este) magától rákérdez: ha aznap még
 * nem edzettél, motiváló értesítést küld a saját hangján. Alapból bekapcsolva,
 * a Beállításokban kikapcsolható. Ha aznap már edzettél, nem nyaggat.
 */
public class DailyNudgeReceiver extends BroadcastReceiver {

    static final String CHANNEL = "edzo_blaze";
    static final int REQ = 88, NOTIF_ID = 9200;

    /** A beállított napi időpont következő előfordulására ütemez. Ha ki van kapcsolva, törli. */
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
        Alarms.oneShot(am, Alarms.nextDaily(Theme.nudgeHour(c), 0), pi);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        // Egyszeri riasztás: a másnapit itt tesszük be (ez a hívás kapcsolja ki
        // magát is, ha a biztatás közben ki lett kapcsolva).
        try { schedule(c); } catch (Exception ignored) {}
        if (!Theme.blazeNudge(c)) return;
        // Pihenőnapon (nem tervezett edzésnapon) Blaze nem nyaggat.
        int dowIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7; // H=0..V=6
        if (!Theme.isPlanDay(c, dowIdx)) return;
        if (workedOutToday(c)) {         // ma már edzett – edzésért nem nyaggatjuk,
            maybeDietReminder(c);        // de az üres étrend-naplóra még szólhatunk
            return;
        }

        String userName = c.getSharedPreferences("edzo", Context.MODE_PRIVATE).getString("user_name", "");
        // A tegnapig tartó napi széria: ha van, Blaze kifejezetten arra figyelmeztet,
        // hogy ne szakadjon meg ("ne hagyd kihunyni a X napos lángod").
        int streak = streakBeforeToday(c);
        String text = Mascot.nudge(userName, streak >= 1, streak);
        // A mai kihívás is bekerül az üzenetbe, ha még nincs teljesítve.
        try {
            Object[] cst = Challenges.state(c);
            if ((int) cst[2] < (int) cst[3]) text += "\n🎯 " + cst[0];
        } catch (Exception ignored) {}
        // Kcal-cél állása annak, aki étrendet vezet: mennyi fér még a mai célba.
        try {
            int goal = c.getSharedPreferences("edzo", Context.MODE_PRIVATE).getInt("kcal_goal", 0);
            int eaten = (int) Math.round(MealLog.todayKcal(c));
            if (goal > 0 && eaten > 0) {
                int left = goal - eaten;
                text += left > 0
                        ? "\n🍽 Ma eddig " + eaten + " kcal – még kb. " + left + " kcal fér a célodba."
                        : "\n🍽 A mai " + goal + " kcal-os cél megvan (" + eaten + " kcal).";
            }
        } catch (Exception ignored) {}
        // Víz-emlékeztető annak, aki ma már használta a számlálót, de a cél még nincs meg.
        try {
            int cl = Water.todayCl(c);
            int goalCl = Water.goalCl(c);
            if (cl > 0 && cl < goalCl)
                text += "\n💧 Vízből " + Water.liters(cl) + " megvan – igyál még "
                        + Water.liters(goalCl - cl) + "-t ma!";
        } catch (Exception ignored) {}

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
        // Blaze saját grafikája nagy ikonként (ha a build tartalmazza).
        try {
            int bid = c.getResources().getIdentifier("blaze", "drawable", c.getPackageName());
            android.graphics.Bitmap bm = bid == 0 ? null
                    : android.graphics.BitmapFactory.decodeResource(c.getResources(), bid);
            if (bm != null && bm.getWidth() > 1) b.setLargeIcon(bm);
        } catch (Exception ignored) {}
        // Akciógomb: egy koppintás, és azonnal indul az edzés (widget-gyorsindítás útvonala).
        Intent quick = new Intent(c, MainActivity.class);
        quick.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        quick.putExtra("quick_start", true);
        PendingIntent qpi = PendingIntent.getActivity(c, REQ + 1, quick, flags);
        b.addAction(new Notification.Action.Builder((android.graphics.drawable.Icon) null,
                "▶ Edzés indítása", qpi).build());
        nm.notify(NOTIF_ID, b.build());
    }

    /**
     * Ha ma már volt edzés, edzésért nem szólunk – de aki rendszeresen naplóz
     * étrendet és ma még semmit nem írt be, annak Blaze küld egy halk emlékeztetőt.
     * Csak akkor, ha az elmúlt héten legalább háromszor naplózott: alkalmi
     * felhasználót nem nyaggatunk.
     */
    private static void maybeDietReminder(Context c) {
        try {
            long dayMs = 24L * 3600 * 1000;
            long today = todayStart();
            int lastWeek = 0;
            for (MealLog.Meal m : MealLog.load(c)) {
                if (m.ts >= today) return;                 // ma már naplózott – kész
                if (m.ts >= today - 7 * dayMs) lastWeek++;
            }
            if (lastWeek < 3) return;                      // nem rendszeres naplózó

            String text = "Ma még nem naplóztál étkezést. 🍽 Két koppintás, és megvan – "
                    + "a falka figyeli a formádat is! 🐺";
            NotificationManager nm =
                    (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "Blaze üzenetei",
                        NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("A kabalafigura napi motiváló üzenetei");
                nm.createNotificationChannel(ch);
            }
            Intent open = new Intent(c, DietActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(c, REQ + 2, open, flags);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(c, CHANNEL)
                    : new Notification.Builder(c);
            b.setContentTitle("Blaze 🐺🍽")
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setAutoCancel(true)
                    .setContentIntent(pi);
            try {
                int bid = c.getResources().getIdentifier("blaze", "drawable", c.getPackageName());
                android.graphics.Bitmap bm = bid == 0 ? null
                        : android.graphics.BitmapFactory.decodeResource(c.getResources(), bid);
                if (bm != null && bm.getWidth() > 1) b.setLargeIcon(bm);
            } catch (Exception ignored) {}
            b.addAction(new Notification.Action.Builder((android.graphics.drawable.Icon) null,
                    "🍽 Étrend megnyitása", pi).build());
            nm.notify(NOTIF_ID + 1, b.build());
        } catch (Exception ignored) {}
    }

    /** Hány egymást követő napon edzett tegnappal bezárólag (ma nélkül). */
    private static int streakBeforeToday(Context c) {
        return Streaks.untilYesterday(c, History.loadAll(c));
    }

    /** A közös ellenőrzés (időzítős és erősítő edzés is számít). */
    private static boolean workedOutToday(Context c) {
        return History.trainedToday(c);
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
