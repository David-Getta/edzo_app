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
 * Heti visszatekintő: vasárnap este egy értesítés összegzi a hét edzéseit
 * (darabszám, táv, idő) és biztat a következő hétre.
 */
public class WeeklyReceiver extends BroadcastReceiver {

    static final String CHANNEL = "edzo_recap";
    static final int REQ = 77, NOTIF_ID = 9100;

    /** A következő vasárnap 19:00-ra ütemez. Ha ki van kapcsolva, törli a riasztást. */
    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent base = new Intent(c, WeeklyReceiver.class).setAction("com.edzo.idozito.WEEKLY");
        int baseFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) baseFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent basePi = PendingIntent.getBroadcast(c, REQ, base, baseFlags);
        if (!Theme.recapEnabled(c)) {
            try { am.cancel(basePi); } catch (Exception ignored) {}
            return;
        }
        Alarms.oneShot(am, Alarms.nextWeekly(Calendar.SUNDAY, 19, 0), basePi);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        // Egyszeri riasztás: a jövő vasárnapit rögtön betesszük.
        try { schedule(c); } catch (Exception ignored) {}
        long from = weekStart(System.currentTimeMillis());
        // Egyesített napló: az erősítő edzések is számítanak a darabszámba és a terv-napokba.
        JSONArray h = History.loadAll(c);
        int count = 0;
        double dist = 0;
        long dur = 0;
        int weekSteps = 0;
        boolean[] trainedDay = new boolean[7];
        for (int k = 0; k < h.length(); k++) {
            JSONObject o = h.optJSONObject(k);
            if (o == null || o.optLong("ts") < from) continue;
            count++;
            double d = o.optDouble("dist", -1);
            if (d > 0) dist += d;
            dur += o.optInt("dur");
            weekSteps += o.optInt("steps", 0);
            int idx = Days.between(from, o.optLong("ts"));
            if (idx >= 0 && idx < 7) trainedDay[idx] = true;
        }
        // Heti terv állása (ha van beállítva edzésnap-terv).
        boolean hasPlan = !Theme.planDays(c).isEmpty();
        int plannedCount = 0, plannedDone = 0;
        if (hasPlan) for (int i2 = 0; i2 < 7; i2++) {
            if (Theme.isPlanDay(c, i2)) { plannedCount++; if (trainedDay[i2]) plannedDone++; }
        }

        String title, text;
        if (count == 0) {
            title = "Blaze: új hét, új esély! 🐺";
            text = "Ezen a héten még nem edzettél. Egy rövid edzés is számít – a falka veled van! 🔥";
        } else {
            title = "Blaze heti összefoglalója 🐺🔥";
            StringBuilder sb = new StringBuilder();
            sb.append(count).append(count == 1 ? " edzés" : " edzés");
            if (dist > 0) sb.append("  ·  ").append(String.format(Hu.LOCALE, "%.1f km", dist / 1000.0));
            sb.append("  ·  ").append(dur / 60).append(" perc mozgás");
            // Lépések, ha voltak – ezres tagolással olvashatóbb.
            if (weekSteps > 0)
                sb.append("  ·  ").append(String.format(Hu.LOCALE, "%,d", weekSteps)
                        .replace(',', ' ')).append(" lépés");
            if (hasPlan && plannedCount > 0)
                sb.append("  ·  Terv: ").append(plannedDone).append("/").append(plannedCount).append(" edzésnap");
            sb.append(". ");
            if (hasPlan && plannedCount > 0 && plannedDone >= plannedCount) {
                sb.append("Heti terv teljesítve – büszke a falka! 🏆🔥");
                int pw = Streaks.planWeeks(c, h);
                if (pw >= 2) sb.append(" Ez már a ").append(pw).append(". terv-heted!");
            } else
                sb.append(count >= 4 ? "Fantasztikus hét – büszke a falka! 🔥" : "Szép munka – jövő héten még többet! 💪");
            text = sb.toString();
        }
        // Sportág-sor, ha a héten többféle mozgás volt („3× kézilabda, 2× futás").
        // Csak a valódi napló-bejegyzésekből: az egyesített lista súlyzós elemei
        // név nélküliek, és tévesen futásnak látszanának.
        try {
            JSONArray hist = History.load(c);
            java.util.ArrayList<String> kinds = new java.util.ArrayList<>();
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            java.util.ArrayList<Integer> durs = new java.util.ArrayList<>();
            for (int k2 = 0; k2 < hist.length(); k2++) {
                JSONObject o = hist.optJSONObject(k2);
                if (o == null || o.optLong("ts") < from) continue;
                kinds.add(o.optString("kind", ""));
                names.add(o.optString("name", ""));
                durs.add(o.optInt("dur"));
            }
            int[] di = new int[durs.size()];
            for (int k2 = 0; k2 < di.length; k2++) di[k2] = durs.get(k2);
            java.util.LinkedHashMap<String, long[]> rows = Activities.breakdown(
                    kinds.toArray(new String[0]), names.toArray(new String[0]), di);
            if (rows.size() >= 2) {
                StringBuilder sp = new StringBuilder();
                int shown = 0;
                for (java.util.Map.Entry<String, long[]> e : rows.entrySet()) {
                    if (shown++ >= 3) break;
                    if (sp.length() > 0) sp.append(", ");
                    sp.append(e.getValue()[0]).append("× ").append(e.getKey());
                }
                text += "\n🏅 " + sp;
            }
        } catch (Exception ignored) {}
        // Étrend-sor annak, aki a héten naplózott: naplózott napok + kcal-átlag.
        try {
            long dayMs = 24L * 3600 * 1000;
            boolean[] loggedDay = new boolean[7];
            double kcalWeek = 0;
            for (MealLog.Meal m : MealLog.load(c)) {
                if (m.ts < from) continue;
                int idx = Days.between(from, m.ts);
                if (idx >= 0 && idx < 7) { loggedDay[idx] = true; kcalWeek += m.kcal(); }
            }
            int loggedDays = 0;
            for (boolean ld : loggedDay) if (ld) loggedDays++;
            if (loggedDays > 0)
                text += "\n🍽 Étrend: " + loggedDays + " naplózott nap, átlag "
                        + Math.round(kcalWeek / loggedDays) + " kcal/nap.";
        } catch (Exception ignored) {}
        // Víz-átlag a hétből (csak azok a napok, ahol ment a számláló).
        try {
            long now = System.currentTimeMillis();
            int wDays = 0, wSum = 0;
            for (int k = 0; k < 7; k++) {
                int cl = Water.clOn(c, now - k * 24L * 3600 * 1000);
                if (cl > 0) { wDays++; wSum += cl; }
            }
            if (wDays > 0)
                text += "\n💧 Víz: átlag "
                        + Water.liters((int) Math.round(wSum / (double) wDays)) + "/nap.";
        } catch (Exception ignored) {}

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Heti összegzés",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Heti visszatekintő az edzéseidről");
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(c, StatsActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(c, REQ, open, flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL)
                : new Notification.Builder(c);
        b.setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setAutoCancel(true)
                .setContentIntent(pi);
        // Blaze saját grafikája nagy ikonként (ha a build tartalmazza).
        try {
            int bid = c.getResources().getIdentifier("blaze", "drawable", c.getPackageName());
            android.graphics.Bitmap bm = bid == 0 ? null
                    : android.graphics.BitmapFactory.decodeResource(c.getResources(), bid);
            if (bm != null && bm.getWidth() > 1) b.setLargeIcon(bm);
        } catch (Exception ignored) {}
        nm.notify(NOTIF_ID, b.build());
    }

    static long weekStart(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
