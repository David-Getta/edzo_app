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
        if (text == null || text.trim().isEmpty()) text = defaultMessage();

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
        b.setContentTitle("Grit")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify(20000 + id, b.build());
    }

    /** Váltakozó, motiváló alapüzenet, ha az emlékeztetőhöz nincs saját szöveg. */
    private static String defaultMessage() {
        String[] msgs = {
            "Ideje mozogni! 💪",
            "Egy rövid edzés is számít – rajta! 🔥",
            "A jövőbeli éned megköszöni ezt az edzést. 🙌",
            "Tartsd a sorozatod – edzés ideje! 🏃",
            "Csak 10 perc, és máris jobban leszel. ✨",
            "Mozdulj meg, tornáztasd meg a tested! 🧘",
            "A legjobb idő az edzésre: most. ⏱️"
        };
        return msgs[(int) (System.currentTimeMillis() / 60000 % msgs.length)];
    }
}
