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
 * Havi visszatekintő: minden hónap 1-jén délelőtt egy értesítés összegzi az
 * ELŐZŐ hónapot – edzések, aktív napok, idő, táv, az év sportja-stílusú
 * kiemelésekkel. A heti visszatekintő a hétről szól; ez a nagyobb ívről.
 * Ugyanaz a kapcsoló vezérli (Beállítások → heti összegzés), és ugyanazon az
 * értesítési csatornán érkezik.
 */
public class MonthlyReceiver extends BroadcastReceiver {

    static final int REQ = 78, NOTIF_ID = 9101;

    /** A következő hónap 1-je 10:00-ra ütemez. Kikapcsolva törli a riasztást. */
    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent base = new Intent(c, MonthlyReceiver.class).setAction("com.edzo.idozito.MONTHLY");
        int baseFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) baseFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent basePi = PendingIntent.getBroadcast(c, REQ, base, baseFlags);
        if (!Theme.recapEnabled(c)) {
            try { am.cancel(basePi); } catch (Exception ignored) {}
            return;
        }
        Alarms.oneShot(am, Alarms.nextMonthly(1, 10, 0), basePi);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        try { schedule(c); } catch (Exception ignored) {}

        // Az előző naptári hónap határai.
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long to = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, -1);
        long from = cal.getTimeInMillis();
        String monthName = new java.text.SimpleDateFormat("MMMM", Hu.LOCALE)
                .format(cal.getTime());

        // Egyesített napló: az erősítő napok is számítanak.
        JSONArray h = History.loadAll(c);
        int count = 0, steps = 0;
        double dist = 0, burned = 0;
        long dur = 0;
        java.util.HashSet<Integer> activeDays = new java.util.HashSet<>();
        Calendar dc = Calendar.getInstance();
        for (int k = 0; k < h.length(); k++) {
            JSONObject o = h.optJSONObject(k);
            if (o == null) continue;
            long ts = o.optLong("ts");
            if (ts < from || ts >= to) continue;
            count++;
            double d = o.optDouble("dist", -1);
            if (d > 0) dist += d;
            dur += o.optInt("dur");
            steps += o.optInt("steps", 0);
            burned += o.optDouble("cal", 0);
            dc.setTimeInMillis(ts);
            activeDays.add(dc.get(Calendar.DAY_OF_YEAR));
        }
        if (count == 0) return;                       // üres hónapról nincs mit mesélni

        StringBuilder sb = new StringBuilder();
        sb.append(count).append(" edzés · ").append(activeDays.size()).append(" aktív nap · ")
                .append(dur / 3600).append(" óra mozgás");
        if (dist > 0) sb.append(String.format(Hu.LOCALE, " · %.1f km", dist / 1000.0));
        if (steps > 0)
            sb.append("  ·  ").append(String.format(Hu.LOCALE, "%,d", steps)
                    .replace(',', ' ')).append(" lépés");
        if (burned >= 100)
            sb.append("  ·  ").append(Math.round(burned)).append(" kcal elégetve");
        String text = sb.toString();

        // Súlyzós összegzés: a havi volumen a hosszú távú fejlődés mércéje.
        try {
            int lifts = 0, setCount = 0;
            double volume = 0;
            for (StrengthLog.Entry e : StrengthLog.load(c)) {
                if (e.ts < from || e.ts >= to) continue;
                lifts++;
                setCount += e.sets.size();
                volume += e.volume();
            }
            if (lifts > 0) {
                text += "\n🏋️ Súlyzós: " + lifts + " gyakorlat, " + setCount + " sorozat";
                if (volume >= 100)
                    text += ", " + String.format(Hu.LOCALE, "%,d", Math.round(volume))
                            .replace(',', ' ') + " kg volumen";
                text += ".";
            }
        } catch (Exception ignored) {}

        // A hónap sportja (csak a valódi napló-bejegyzésekből, mint a hetinél).
        try {
            JSONArray hist = History.load(c);
            java.util.ArrayList<String> kinds = new java.util.ArrayList<>();
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            java.util.ArrayList<Integer> durs = new java.util.ArrayList<>();
            for (int k = 0; k < hist.length(); k++) {
                JSONObject o = hist.optJSONObject(k);
                if (o == null) continue;
                long ts = o.optLong("ts");
                if (ts < from || ts >= to) continue;
                kinds.add(o.optString("kind", ""));
                names.add(o.optString("name", ""));
                durs.add(o.optInt("dur"));
            }
            int[] di = new int[durs.size()];
            for (int k = 0; k < di.length; k++) di[k] = durs.get(k);
            java.util.LinkedHashMap<String, long[]> rows = Activities.breakdown(
                    kinds.toArray(new String[0]), names.toArray(new String[0]), di);
            if (!rows.isEmpty()) {
                java.util.Map.Entry<String, long[]> top = rows.entrySet().iterator().next();
                if (top.getValue()[0] >= 2)
                    text += "\n🏅 A hónap sportja: " + top.getKey()
                            + " (" + top.getValue()[0] + " alkalom)";
            }
        } catch (Exception ignored) {}

        NotificationManager nm =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26
                && nm.getNotificationChannel(WeeklyReceiver.CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(WeeklyReceiver.CHANNEL,
                    "Heti összegzés", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Visszatekintő az edzéseidről");
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(c, StatsActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(c, REQ, open, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, WeeklyReceiver.CHANNEL)
                : new Notification.Builder(c);
        String niceMonth = monthName.substring(0, 1).toUpperCase(Hu.LOCALE)
                + monthName.substring(1);
        b.setContentTitle("Blaze havi összefoglalója – " + niceMonth + " 🐺📆")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            int bid = c.getResources().getIdentifier("blaze", "drawable", c.getPackageName());
            android.graphics.Bitmap bm = bid == 0 ? null
                    : android.graphics.BitmapFactory.decodeResource(c.getResources(), bid);
            if (bm != null && bm.getWidth() > 1) b.setLargeIcon(bm);
        } catch (Exception ignored) {}
        nm.notify(NOTIF_ID, b.build());
    }
}
