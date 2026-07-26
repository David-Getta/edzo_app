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
        int dowIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7; // H=0..V=6
        String msg;
        int liveStreak = streakUntilYesterday(c);
        if (today) {
            msg = praise(userName, streakDays(c));
            // Ha a mai kihívás is megvan, az is kiderül egy pillantásra.
            try {
                Object[] cst = Challenges.state(c);
                if ((int) cst[2] >= (int) cst[3]) msg += "  ·  🎯 Kihívás: pipa!";
            } catch (Exception ignored) {}
        } else if (!Theme.isPlanDay(c, dowIdx)) {
            String[] rest = {
                    "Ma pihenőnap – tölts fel! 🌙🐺",
                    "Pihenőnap: egy kis nyújtás jólesne. 🧘🐺",
                    "Regeneráció ma – holnap újra hajtunk! 🌙🔥",
            };
            msg = rest[(int) (System.currentTimeMillis() / 3600000 % rest.length)];
        }
        else if (liveStreak >= 2)
            msg = liveStreak + " napos széria él – ne hagyd ma megszakadni! 🔥🐺";
        else {
            // Ha nincs élő széria, a mai kihívás a legjobb hívószó.
            String ch = null;
            try {
                Object[] cst = Challenges.state(c);
                if ((int) cst[2] < (int) cst[3]) ch = "🎯 " + cst[0];
            } catch (Exception ignored) {}
            msg = ch != null ? ch : Mascot.nudge(userName, false, 0);
        }
        // A mai kcal-állás egy pillantásra annak, aki étrendet vezet (cél esetén céllal).
        try {
            int kGoal = c.getSharedPreferences("edzo", Context.MODE_PRIVATE)
                    .getInt("kcal_goal", 0);
            int eaten = (int) Math.round(MealLog.todayKcal(c));
            if (eaten > 0)
                msg += "\n🍽 " + eaten + (kGoal > 0 ? " / " + kGoal : "") + " kcal ma";
        } catch (Exception ignored) {}
        rv.setTextViewText(R.id.blaze_msg, msg);

        // A címsorban az élő széria is látszik (2 naptól).
        int shownStreak = today ? streakDays(c) : liveStreak;
        rv.setTextViewText(R.id.widget_title,
                shownStreak >= 2 ? "Blaze 🔥 " + shownStreak : "Blaze 🔥");

        // A ▶ gomb által indítandó edzés rövid leírása (program vagy intervallum).
        android.content.SharedPreferences p = c.getSharedPreferences("edzo", Context.MODE_PRIVATE);
        String prog = p.getString("progname", "");
        String cfg = !prog.isEmpty() ? "▶ " + prog
                : "▶ " + p.getInt("k1", 10) + "/" + p.getInt("k2", 30) + " mp · "
                    + p.getInt("k3", 8) + " kör";
        rv.setTextViewText(R.id.widget_cfg, cfg);

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

        // Súlyzós gyorsgomb: egyenesen az Erő naplóba.
        Intent gym = new Intent(c, StrengthActivity.class);
        gym.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        rv.setOnClickPendingIntent(R.id.blaze_gym, PendingIntent.getActivity(c, 2, gym, flags));

        // 💧 gomb: +1 pohár víz app-megnyitás nélkül (saját broadcast).
        Intent water = new Intent(c, BlazeWidget.class).setAction(ACTION_WATER);
        rv.setOnClickPendingIntent(R.id.blaze_water,
                PendingIntent.getBroadcast(c, 3, water, flags));

        mgr.updateAppWidget(id, rv);
    }

    static final String ACTION_WATER = "com.edzo.idozito.WIDGET_WATER";

    @Override
    public void onReceive(Context c, Intent intent) {
        if (ACTION_WATER.equals(intent.getAction())) {
            android.content.SharedPreferences p =
                    c.getSharedPreferences("edzo", Context.MODE_PRIVATE);
            Calendar cal = Calendar.getInstance();
            String key = "water_" + (cal.get(Calendar.YEAR) * 10000
                    + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH));
            int cl = p.getInt(key, 0) + 25;
            p.edit().putInt(key, cl).apply();
            int goalCl = p.getInt("water_goal_cl", 200);
            android.widget.Toast.makeText(c, cl >= goalCl
                    ? "💧 " + (cl / 100.0) + " l – a napi vízcél megvan! ✔"
                    : "💧 +1 pohár · ma " + (cl / 100.0) + " l",
                    android.widget.Toast.LENGTH_SHORT).show();
            // A Hidratált jelvény tartós számlálója (naponta legfeljebb egyszer).
            if (cl >= goalCl && cl - 25 < goalCl) {
                int today = cal.get(Calendar.YEAR) * 10000
                        + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
                if (p.getInt("water_last_done", 0) != today)
                    p.edit().putInt("water_last_done", today)
                            .putInt("water_days_done", p.getInt("water_days_done", 0) + 1)
                            .apply();
            }
            refresh(c);
            return;
        }
        super.onReceive(c, intent);
    }

    private static String praise(String userName, int streak) {
        String u = (userName == null || userName.trim().isEmpty()) ? "falkatárs" : userName.trim();
        if (streak >= 2) return "Ma már letudtad, " + u + "! 🔥 " + streak + " napos széria – ég a láng!";
        String[] p = {
                "Ma már letudtad, " + u + "! 💪🔥 Büszke vagyok rád.",
                "Kipipálva a mai, " + u + "! 🐺 Megérdemelt pihenés. 🔥",
                "Mai edzés: kész! 💪 Blaze büszkén vonyít, " + u + "! 🐺",
        };
        // Óránként váltakozó dicséret, hogy a widget ne legyen egyhangú.
        return p[(int) (System.currentTimeMillis() / 3600000 % p.length)];
    }

    /** Egymást követő edzésnapok száma tegnappal bezárólag (a még élő széria). */
    private static int streakUntilYesterday(Context c) {
        return Streaks.untilYesterday(c, History.loadAll(c));
    }

    /** Egymást követő edzésnapok száma mával bezárólag. */
    private static int streakDays(Context c) {
        return Streaks.current(c, History.loadAll(c));
    }

    private static boolean workedOutToday(Context c) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        JSONArray h = History.loadAll(c);
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o != null && o.optLong("ts") >= start) return true;
        }
        return false;
    }
}
