package com.edzo.idozito;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Egy emlékeztető időpontjában lefut, és értesítést jelenít meg a saját szöveggel.
 */
public class ReminderReceiver extends BroadcastReceiver {

    static final String CHANNEL = "edzo_reminders";

    @Override
    public void onReceive(Context c, Intent intent) {
        String text = intent.getStringExtra("text");
        int id = intent.getIntExtra("id", 1);

        // A riasztás egyszeri: a holnapit rögtön az elején ütemezzük be, hogy az
        // értesítés összeállításánál esetleg fellépő hiba se szakítsa meg a láncot.
        try { Reminders.rescheduleAfterFire(c, id); } catch (Exception ignored) {}

        if (text == null || text.trim().isEmpty()) text = defaultMessage(c);

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Emlékeztetők",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Grit emlékeztetők");
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(c, 10000 + id, open, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL)
                : new Notification.Builder(c);
        // Az értesítést Blaze, a kabalafigura „mondja".
        b.setContentTitle("Blaze 🐺🔥")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify(20000 + id, b.build());
    }

    /**
     * Ha az emlékeztetőhöz nincs saját szöveg, Blaze hangján szól – de a nap
     * tényleges állásához igazítva. Korábban mindig azt mondta, hogy „a mai
     * edzés még hiányzik", akkor is, ha reggel már megvolt.
     */
    private static String defaultMessage(Context c) {
        String userName = c.getSharedPreferences("edzo", Context.MODE_PRIVATE)
                .getString("user_name", "");
        try {
            return Mascot.reminderText(userName, History.trainedToday(c),
                    Streaks.untilYesterday(c, History.loadAll(c)));
        } catch (Exception e) {
            return Mascot.nudge(userName, false, 0);
        }
    }
}
