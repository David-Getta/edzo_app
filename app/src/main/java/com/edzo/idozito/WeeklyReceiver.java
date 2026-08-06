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
        double weekCal = 0;
        boolean[] trainedDay = new boolean[7];
        for (int k = 0; k < h.length(); k++) {
            JSONObject o = h.optJSONObject(k);
            if (o == null || o.optLong("ts") < from) continue;
            count++;
            double d = o.optDouble("dist", -1);
            if (d > 0) dist += d;
            dur += o.optInt("dur");
            weekSteps += o.optInt("steps", 0);
            weekCal += o.optDouble("cal", 0);
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
            if (weekCal >= 100)
                sb.append("  ·  ").append(Math.round(weekCal)).append(" kcal elégetve");
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
        // A hét csúcsa: a leghosszabb edzés, sportággal és nappal. Egy hosszú
        // hétvégi túra vagy egy kemény meccs megérdemli a külön említést.
        try {
            JSONArray hist = History.load(c);
            JSONObject best = null;
            for (int k2 = 0; k2 < hist.length(); k2++) {
                JSONObject o = hist.optJSONObject(k2);
                if (o == null || o.optLong("ts") < from) continue;
                if (best == null || o.optInt("dur") > best.optInt("dur")) best = o;
            }
            if (best != null && best.optInt("dur") >= 30 * 60 && count >= 2) {
                Activities.Kind bk = Activities.byId(best.optString("kind", ""));
                if (bk == null) bk = Activities.kindByText(best.optString("name", ""));
                String what = bk != null ? bk.name
                        : best.optString("name", "").isEmpty() ? "futás"
                        : best.optString("name", "");
                String day = new java.text.SimpleDateFormat("EEEE", Hu.LOCALE)
                        .format(new java.util.Date(best.optLong("ts")));
                text += "\n🏆 A hét csúcsa: " + (best.optInt("dur") / 60) + " perc "
                        + what + " (" + day + ")";
            }
        } catch (Exception ignored) {}
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
        // Súlyzós sor: a heti volumen az erősítő edzés legbeszédesebb száma –
        // a darabszámból nem látszik, mennyi munka volt mögötte.
        try {
            int lifts = 0, setCount = 0, rpeSum = 0, rpeCount = 0;
            double volume = 0;
            String topLift = null;
            double topWeight = 0;
            for (StrengthLog.Entry e : StrengthLog.load(c)) {
                if (e.ts < from) continue;
                lifts++;
                setCount += e.sets.size();
                volume += e.volume();
                if (e.topWeight() > topWeight) { topWeight = e.topWeight(); topLift = e.name; }
                if (e.rpe > 0) { rpeSum += e.rpe; rpeCount++; }
            }
            if (lifts > 0) {
                String s = "\n🏋️ Súlyzós: " + lifts + " gyakorlat, " + setCount + " sorozat";
                if (volume >= 100)
                    s += ", " + String.format(Hu.LOCALE, "%,d", Math.round(volume))
                            .replace(',', ' ') + " kg volumen";
                if (topLift != null && topWeight > 0)
                    s += "  ·  csúcs: " + topLift + " " + Progression.kg(topWeight) + " kg";
                if (rpeCount >= 3)
                    s += "  ·  átlagos érzett terhelés " + Hu.d1(rpeSum / (double) rpeCount);
                text += s + ".";
                // Mindenkori rekord vagy csak a hét legnehezebb napja? A kettő
                // között van a különbség aközött, hogy „ez volt a hét" és
                // „ilyet még soha".
                java.util.List<StrengthLog.Entry> all = StrengthLog.load(c);
                int sn = 0;
                for (StrengthLog.Entry e : all) sn += e.sets.size();
                long[] rts = new long[sn];
                String[] rnames = new String[sn];
                double[] rw = new double[sn];
                int[] rr = new int[sn];
                int ri = 0;
                for (StrengthLog.Entry e : all)
                    for (StrengthLog.SetEntry st : e.sets) {
                        rts[ri] = e.ts; rnames[ri] = e.name; rw[ri] = st.weight;
                        rr[ri] = st.reps; ri++;
                    }
                java.util.List<String> recs = Bests.newRecordsSince(from, rts, rnames, rw, rr);
                if (!recs.isEmpty()) {
                    String line = "\n🏆 Új csúcs: " + recs.get(0);
                    if (recs.size() > 1) line += "  ·  " + recs.get(1);
                    if (recs.size() > 2) line += "  ·  +" + (recs.size() - 2) + " további";
                    text += line;
                }
            }
        } catch (Exception ignored) {}
        // Terhelés-ugrás: csak akkor szólunk, ha a hét kilóg a megszokottból.
        // A „minden rendben" itt fölösleges sor lenne a heti összegzésben.
        try {
            long now = System.currentTimeMillis();
            int span = Load.ACUTE_DAYS + Load.CHRONIC_DAYS;
            double[] daily = History.dailyMinutes(c, now, span);
            Load.Ratio r = Load.of(daily);
            if (r.known && r.level == Load.JUMP)
                text += "\n⚠️ Terhelés: " + r.label() + " – jövő héten vegyél vissza.";
            // Heti mozgás-cél: a hét zárásakor ez a legfontosabb egy szám.
            Load.Weekly wk = Load.weekly(daily, c.getSharedPreferences("edzo",
                    Context.MODE_PRIVATE).getInt("move_goal_min", Load.DEFAULT_WEEKLY_GOAL));
            if (wk.minutes > 0)
                text += "\n🎽 Heti mozgás: " + wk.label()
                        + (wk.done ? "  ✔" : "  ·  " + wk.percent + "%");
        } catch (Exception ignored) {}
        // Heti fókusz: teljesült-e, amit a hétre tervezett? A terv-napok
        // darabszáma önmagában nem árulja el, hogy a HÁT-napon tényleg hát volt-e.
        try {
            String focusCsv = Theme.planFocus(c);
            if (Weekplan.any(focusCsv)) {
                String[] dayGroups = new String[7];
                for (StrengthLog.Entry e : StrengthLog.load(c)) {
                    int idx = Days.between(from, e.ts);
                    if (idx < 0 || idx > 6) continue;
                    String g = Muscles.groupOf(e.name);
                    if (g == null) continue;
                    dayGroups[idx] = dayGroups[idx] == null ? g
                            : dayGroups[idx].contains(g) ? dayGroups[idx] : dayGroups[idx] + "," + g;
                }
                int[] a = Weekplan.adherence(focusCsv, dayGroups, trainedDay);
                if (a[1] > 0)
                    text += "\n📋 Heti fókusz: " + a[0] + "/" + a[1] + " nap teljesült"
                            + (a[0] >= a[1] ? "  ✔" : ".");
            }
        } catch (Exception ignored) {}
        // Étrend-sor annak, aki a héten naplózott: naplózott napok + kcal-átlag.
        try {
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
        // Testsúly-tendencia az elmúlt hat hétből: a heti ütem az a szám, ami
        // egy fogyási célnál számít – egyetlen mérés a napi ingadozás miatt nem.
        try {
            JSONArray ms = Profile.measurements(c);
            java.util.ArrayList<Double> ds = new java.util.ArrayList<>();
            java.util.ArrayList<Double> ws = new java.util.ArrayList<>();
            long since = System.currentTimeMillis() - 42L * 24 * 3600 * 1000;
            for (int k = 0; k < ms.length(); k++) {
                JSONObject o = ms.optJSONObject(k);
                if (o == null) continue;
                double w = o.optDouble("w", -1);
                long ts = o.optLong("ts");
                if (w > 0 && ts >= since) { ds.add(ts / 86400000.0); ws.add(w); }
            }
            if (ds.size() >= 3) {
                double[] da = new double[ds.size()], wa = new double[ws.size()];
                for (int k = 0; k < da.length; k++) { da[k] = ds.get(k); wa[k] = ws.get(k); }
                double per = Profile.weeklyTrend(da, wa);
                if (Math.abs(per) >= 0.05)
                    text += String.format(Hu.LOCALE, "\n⚖️ Testsúly: %+.2f kg/hét (6 hét trendje).",
                            per);
            }
            // Ha rég volt mérés, a trend már nem tendencia, csak két régi pont.
            // Ezt mondjuk is ki – a heti összefoglaló az a hely, ahol a
            // felhasználó úgyis a számokat nézi.
            int measAgo = Profile.daysSinceMeasurement(c, System.currentTimeMillis());
            String nudge = Profile.measureNudge(measAgo);
            // Csak annak szólunk, aki már mért egyszer: aki sosem használta a
            // mérleget, annak ez heti szemrehányás lenne, nem segítség.
            if (measAgo >= 0 && !nudge.isEmpty()) text += "\n" + nudge;
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
