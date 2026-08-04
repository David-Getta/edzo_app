package com.edzo.idozito;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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
        /**
         * Mely napokon szóljon: bitek hétfőtől vasárnapig (1 = hétfő … 64 =
         * vasárnap). A 0 minden napot jelent – a régi bejegyzések így
         * változtatás nélkül működnek tovább.
         */
        public int days;
        public Reminder(int id, int h, int m, String text, boolean on) {
            this(id, h, m, text, on, 0);
        }
        public Reminder(int id, int h, int m, String text, boolean on, int days) {
            this.id = id; this.h = h; this.m = m; this.text = text;
            this.on = on; this.days = days;
        }

        /** „Minden nap", „Hétköznap", „Hétvégén" vagy „H, Sze, P". */
        public String daysLabel() { return Reminders.daysLabel(days); }
    }

    static final String[] DAY_ABBR = {"H", "K", "Sze", "Cs", "P", "Szo", "V"};

    public static String daysLabel(int mask) {
        int use = (mask <= 0 || mask >= 127) ? 127 : mask;
        if (use == 127) return "Minden nap";
        if (use == 31) return "Hétköznap";
        if (use == 96) return "Hétvégén";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++)
            if ((use & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(DAY_ABBR[i]);
            }
        return sb.toString();
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
                        o.optString("text", ""), o.optBoolean("on", true),
                        o.optInt("days", 0)));
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
                o.put("text", r.text); o.put("on", r.on); o.put("days", r.days);
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

    /**
     * Mindig csak a KÖVETKEZŐ alkalomra ütemez (lásd {@link Alarms}). A soron
     * következőt a {@link ReminderReceiver} teszi be, amikor ez lefutott.
     */
    public static void scheduleOne(Context c, Reminder r) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent p = pi(c, r);
        if (r.on) Alarms.oneShot(am, Alarms.nextOnDays(r.days, r.h, r.m), p);
        else am.cancel(p);
    }

    /** Egy lefutott emlékeztető újraütemezése a másnapi időpontra. */
    public static void rescheduleAfterFire(Context c, int id) {
        for (Reminder r : load(c)) if (r.id == id) { scheduleOne(c, r); return; }
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
