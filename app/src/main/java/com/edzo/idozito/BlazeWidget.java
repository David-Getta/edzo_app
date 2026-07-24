package com.edzo.idozito;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Blaze, a kabalafigura kezdőképernyős widgetje. A layout ViewFlippere magától
 * váltogatja a képkockákat („mozgó" hatás), a szöveg pedig az aktuális állapot
 * szerinti biztatás. Kattintásra megnyílik az app.
 */
public class BlazeWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateOne(c, mgr, id);
    }

    /** Az app hívja, ha frissült az adat (pl. edzés után), hogy a widget is frissüljön. */
    public static void refresh(Context c) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(c);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(c, BlazeWidget.class));
            for (int id : ids) updateOne(c, mgr, id);
        } catch (Exception ignored) {}
    }

    static void updateOne(Context c, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_blaze);

        String userName = c.getSharedPreferences("edzo", Context.MODE_PRIVATE)
                .getString("user_name", "");
        boolean today = workedOutToday(c);
        String msg = today
                ? praise(userName, streakDays(c))
                : Mascot.nudge(userName, false, 0);
        rv.setTextViewText(R.id.blaze_msg, msg);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        rv.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(c, 0, open, flags));

        // Gyorsindítás: megnyitja az appot és azonnal elindítja az edzést.
        Intent quick = new Intent(c, MainActivity.class);
        quick.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        quick.putExtra("quick_start", true);
        rv.setOnClickPendingIntent(R.id.blaze_start, PendingIntent.getActivity(c, 1, quick, flags));

        mgr.updateAppWidget(id, rv);
    }

    private static String praise(String userName, int streak) {
        String u = (userName == null || userName.trim().isEmpty()) ? "falkatárs" : userName.trim();
        if (streak >= 2) return "Ma már letudtad, " + u + "! 🔥 " + streak + " napos széria – ég a láng!";
        return "Ma már letudtad, " + u + "! 💪🔥 Büszke vagyok rád.";
    }

    /** Egymást követő edzésnapok száma mával bezárólag. */
    private static int streakDays(Context c) {
        long dayMs = 24L * 60 * 60 * 1000;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();
        JSONArray h = History.load(c);
        int streak = 0;
        for (int k = 0; k <= 365; k++) {
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
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        JSONArray h = History.load(c);
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o != null && o.optLong("ts") >= start) return true;
        }
        return false;
    }
}
