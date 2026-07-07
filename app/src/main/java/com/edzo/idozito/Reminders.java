package com.edzo.idozito;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Testre szabható napi emlékeztetők (Duolingo-stílusú értesítések). Minden
 * emlékeztető egy időpont + saját szöveg; AlarmManager ütemezi naponta.
 */
public final class Reminders {

    private Reminders() {}

    static final String PREFS = "edzo";
    static final String KEY = "reminders";

    public static final class Reminder {
        public int id;
        public int h, m;
        public String text;
        public boolean on;
        public Reminder(int id, int h, int m, String text, boolean on) {
            this.id = id; this.h = h; this.m = m; this.text = text; this.on = on;
        }
    }

    public static List<Reminder> load(Context c) {
        List<Reminder> out = new ArrayList<>();
        String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                out.add(new Reminder(o.optInt("id"), o.optInt("h"), o.optInt("m"),
                        o.optString("text", ""), o.optBoolean("on", true)));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void save(Context c, List<Reminder> list) {
        JSONArray a = new JSONArray();
        for (Reminder r : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", r.id); o.put("h", r.h); o.put("m", r.m);
                o.put("text", r.text); o.put("on", r.on);
                a.put(o);
            } catch (Exception ignored) {}
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }

    public static int nextId(Context c) {
        int max = 0;
        for (Reminder r : load(c)) max = Math.max(max, r.id);
        return max + 1;
    }

    // --------- Ütemezés ---------

    private static PendingIntent pi(Context c, Reminder r) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction("com.edzo.idozito.REMIND_" + r.id);
        i.putExtra("id", r.id);
        i.putExtra("text", r.text);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, r.id, i, flags);
    }

    private static long nextTrigger(int h, int m) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    public static void scheduleOne(Context c, Reminder r) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent p = pi(c, r);
        if (r.on) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, nextTrigger(r.h, r.m),
                    AlarmManager.INTERVAL_DAY, p);
        } else {
            am.cancel(p);
        }
    }

    public static void cancelOne(Context c, Reminder r) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(c, r));
    }

    /** Minden emlékeztető újraütemezése (indításkor és boot után). */
    public static void scheduleAll(Context c) {
        for (Reminder r : load(c)) scheduleOne(c, r);
    }
}
