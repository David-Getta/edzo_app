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
    /** Ennyi kiegészítő sor fér az üzenetbe a biztatás alá. */
    static final int MAX_LINES = 4;
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

        // Az üzenet sorai fontossági sorrendben gyűlnek, és csak a legjobb
        // néhány kerül ki. Az app közben sok mindent tud mondani – kilenc sor
        // viszont már nem üzenet, hanem fal, amit senki nem olvas végig. Ami
        // MA cselekvésre hív, az előrébb van, mint ami csak érdekes.
        java.util.List<String> lines = new java.util.ArrayList<>();

        // 1) Heti fókusz: ha beírta, mit edz ma, annál konkrétabb nincs.
        try {
            String line = Weekplan.todayLine(Theme.planFocus(c), dowIdx);
            // Ha a mai fókuszhoz edzésnap is tartozik, a gyakorlatok neve
            // konkrétabb, mint az izomcsoporté: „Ma: Láb" helyett látszik,
            // hogy guggolás és kitörés jön.
            String todayFocus = Weekplan.forDay(Theme.planFocus(c), dowIdx);
            if (!line.isEmpty() && !todayFocus.isEmpty() && line.contains("Ma:"))
                for (Routines.Routine r : Routines.all(
                        Theme.getStr(c, Routines.KEY, "")))
                    if (Foods.norm(r.name).contains(Foods.norm(todayFocus))) {
                        line += "  ·  " + r.shortSummary(3);
                        break;
                    }
            if (!line.isEmpty()) lines.add(line);
            // Heti terv nélkül a rotáció válaszol: a legrégebben csinált nap.
            // Ugyanaz, amit a kezdőlap csempéje és az edzésnap-lista mond –
            // három helyen három különböző válasz csak zavarna.
            if (line.isEmpty() && todayFocus.isEmpty()) {
                java.util.List<StrengthLog.Entry> sLog = StrengthLog.load(c);
                long[] rts = new long[sLog.size()];
                String[] rnames = new String[sLog.size()];
                for (int i = 0; i < sLog.size(); i++) {
                    rts[i] = sLog.get(i).ts;
                    rnames[i] = sLog.get(i).name;
                }
                java.util.List<Routines.Routine> all =
                        Routines.all(Theme.getStr(c, Routines.KEY, ""));
                String due = Routines.nextUp(all, rts, rnames, System.currentTimeMillis());
                if (due != null) {
                    Routines.Routine r = Routines.byName(
                            Theme.getStr(c, Routines.KEY, ""), due);
                    if (r != null)
                        lines.add("🏋 Soron: " + r.name + "  ·  " + r.shortSummary(3));
                }
            }
        } catch (Exception ignored) {}

        // 2) A mai kihívás, ha még nincs teljesítve.
        try {
            Object[] cst = Challenges.state(c);
            if ((int) cst[2] < (int) cst[3]) lines.add("🎯 " + cst[0]);
        } catch (Exception ignored) {}

        // 3) Heti mozgás-cél: hét közben ez mondja meg, hol tart a hét.
        try {
            int goal = c.getSharedPreferences("edzo", Context.MODE_PRIVATE)
                    .getInt("move_goal_min", Load.DEFAULT_WEEKLY_GOAL);
            Load.Weekly w = Load.weekly(
                    History.dailyMinutes(c, System.currentTimeMillis(), Load.ACUTE_DAYS), goal);
            if (w.minutes > 0 && !w.done)
                lines.add("🎽 Heti mozgás: " + w.label() + " – " + w.percent + "%.");
        } catch (Exception ignored) {}

        // 4) Kcal-cél állása, konkrét étel-ötlettel a maradékra.
        try {
            int goal = Profile.effectiveGoal(
                    c.getSharedPreferences("edzo", Context.MODE_PRIVATE).getInt("kcal_goal", 0),
                    History.burnedToday(c), Theme.kcalCredit(c));
            int eaten = (int) Math.round(MealLog.todayKcal(c));
            if (goal > 0 && eaten > 0) {
                int left = goal - eaten;
                String line = left > 0
                        ? "🍽 Ma eddig " + eaten + " kcal – még kb. " + left + " kcal fér a célodba."
                        : "🍽 A mai " + goal + " kcal-os cél megvan (" + eaten + " kcal).";
                if (left > 0) {
                    int pGoal = c.getSharedPreferences("edzo", Context.MODE_PRIVATE)
                            .getInt("protein_goal", 0);
                    double pLeft = pGoal > 0 ? pGoal - MealLog.todayProtein(c) : 0;
                    java.util.List<MealIdeas.Idea> ideas = MealIdeas.forRemaining(
                            Foods.ALL, left, pLeft, Days.index(System.currentTimeMillis()));
                    if (!ideas.isEmpty()) line += " Pl. " + ideas.get(0).label() + ".";
                }
                lines.add(line);
            }
        } catch (Exception ignored) {}

        // 5) Név szerint kimaradt sportág – a személyes hat, az általános nem.
        String miss = History.missedSportLine(c);
        if (miss != null) lines.add(miss);

        // 6) Rég kimaradt gyakorlat: a rekordlistában a legutóbbi van elöl,
        //    tehát ez magától nem tűnne fel.
        try {
            java.util.List<StrengthLog.Entry> sLog = StrengthLog.load(c);
            String forgotten = StrengthLog.mostNeglected(sLog, System.currentTimeMillis(),
                    StrengthLog.NEGLECTED_DAYS);
            if (forgotten != null) {
                int d = StrengthLog.daysSince(sLog, forgotten, System.currentTimeMillis());
                lines.add("💤 " + forgotten + " " + StrengthLog.agoLabel(d)
                        + " maradt ki – beveszed ma?");
            }
        } catch (Exception ignored) {}

        // 6/b) Rehab-fókusz: a hét második felében, ha még hiányzik az adag.
        // Hétfőn még korai számonkérni, csütörtöktől viszont már fogy az idő –
        // és a megelőzésnél pont a rendszeresség a hatóanyag.
        try {
            String rid = RehabLog.focusId(c);
            Rehab.Area ra = rid == null ? null : Rehab.byId(rid);
            if (ra != null && dowIdx >= 3) {
                int done = Rehab.weekCount(RehabLog.doneOf(c, rid), System.currentTimeMillis());
                if (done < Rehab.WEEKLY_GOAL)
                    lines.add("🩹 " + ra.name + ": " + done + "/" + Rehab.WEEKLY_GOAL
                            + " a héten – tíz perc most is belefér.");
            }
        } catch (Exception ignored) {}

        // 7) Víz: hasznos, de a mozgásnál kevésbé sürgős.
        try {
            int cl = Water.todayCl(c);
            int goalCl = Water.goalCl(c);
            if (cl > 0 && cl < goalCl)
                lines.add("💧 Vízből " + Water.liters(cl) + " megvan – igyál még "
                        + Water.liters(goalCl - cl) + "-t ma!");
        } catch (Exception ignored) {}

        // 8) Heti szokás: érdekes és személyes, de nem sürgős.
        try {
            org.json.JSONArray hh = History.load(c);
            int[] wd = new int[hh.length()];
            String[] kinds = new String[hh.length()];
            int[] ago = new int[hh.length()];
            Calendar hc = Calendar.getInstance();
            long now = System.currentTimeMillis();
            for (int i = 0; i < hh.length(); i++) {
                org.json.JSONObject o = hh.optJSONObject(i);
                if (o == null) continue;
                hc.setTimeInMillis(o.optLong("ts"));
                wd[i] = (hc.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                Activities.Kind k = Activities.byId(o.optString("kind", ""));
                if (k == null) k = Activities.kindByText(o.optString("name", ""));
                kinds[i] = k == null ? "" : k.id;
                ago[i] = Days.ago(o.optLong("ts"), now);
            }
            String id = Habits.usualSportOn(wd, kinds, ago, dowIdx);
            Activities.Kind k = id == null ? null : Activities.byId(id);
            if (k != null)
                lines.add("🗓 " + Hu.dayName(dowIdx) + " van – ilyenkor általában "
                        + k.name.toLowerCase(Hu.LOCALE) + " szokott lenni.");
        } catch (Exception ignored) {}

        for (int i = 0; i < lines.size() && i < MAX_LINES; i++) text += "\n" + lines.get(i);

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

    /** „🍳 A szokásos reggelid: Tojás, Kenyér" – vagy null, ha nincs ilyen. */
    private static String usualLine(Context c) {
        try {
            java.util.List<MealLog.Meal> all = MealLog.load(c);
            if (all.size() < Habits.MIN_COUNT) return null;
            java.util.List<java.util.List<String>> foods = new java.util.ArrayList<>();
            int[] hours = new int[all.size()];
            int[] ago = new int[all.size()];
            Calendar cal = Calendar.getInstance();
            long now = System.currentTimeMillis();
            for (int i = 0; i < all.size(); i++) {
                java.util.List<String> names = new java.util.ArrayList<>();
                for (MealLog.Item it : all.get(i).items) names.add(it.food);
                foods.add(names);
                cal.setTimeInMillis(all.get(i).ts);
                hours[i] = cal.get(Calendar.HOUR_OF_DAY);
                ago[i] = Days.ago(all.get(i).ts, now);
            }
            cal.setTimeInMillis(now);
            int bucket = Habits.bucketOf(cal.get(Calendar.HOUR_OF_DAY));
            Habits.Usual u = Habits.usual(foods, hours, ago, bucket);
            if (u == null) return null;
            StringBuilder sb = new StringBuilder();
            for (String f : u.foods) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(f);
            }
            return u.label(bucket) + ": " + sb;
        } catch (Exception e) {
            return null;
        }
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
            // Ha van szokásos étkezésed erre a napszakra, nevezzük is meg: a
            // konkrét étel közelebb van a naplózáshoz, mint az általános biztatás.
            String usual = usualLine(c);
            if (usual != null) text += "\n" + usual;
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
