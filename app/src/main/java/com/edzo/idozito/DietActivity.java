package com.edzo.idozito;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Étrend-napló: írd be, mit ettél (akár több összetevőt egy étkezésként),
 * és az app a beépített adatbázisból kiszámolja a kalóriát. Ha csak a
 * teljes adag grammját adod meg, az összetevők közt arányosan osztja el.
 */
public class DietActivity extends Activity {

    static int BG, CARD, CARD2, TXT, MUTED, LINE;

    LinearLayout listBox;
    LinearLayout todayCard, weekCard, quickBox, waterCard;
    static final int REQ_PHOTO = 61;
    long pendingPhotoTs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); BG=MainActivity.BG; CARD=MainActivity.CARD; CARD2=MainActivity.CARD2; TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; LINE=MainActivity.LINE;
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Étrend", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Írd be, mit ettél – az app kiszámolja a kalóriát.", 13, MUTED, false));
        col.addView(gap(16));

        // Mai összesítő (koppintásra beállítható a napi kalória-cél).
        todayCard = card();
        todayCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        todayCard.setClickable(true);
        todayCard.setOnClickListener(v -> editGoalDialog());
        col.addView(todayCard, lp());
        col.addView(gap(10));

        // Vízbevitel-számláló (koppintásra beállítható a napi cél).
        waterCard = card();
        waterCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        col.addView(waterCard, lp());
        col.addView(gap(10));

        // Elmúlt 7 nap kcal-sávjai.
        weekCard = card();
        weekCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        col.addView(weekCard, lp());
        col.addView(gap(14));

        Button add = primary("＋  Étkezés hozzáadása");
        add.setOnClickListener(v -> addMealDialog());
        col.addView(add);
        col.addView(gap(8));
        Button table = ghost("📖  Kalóriatáblázat");
        table.setTextSize(13.5f);
        table.setOnClickListener(v -> foodTableSheet());
        col.addView(table);
        quickBox = vbox();
        col.addView(quickBox, lp());
        col.addView(gap(18));

        col.addView(text("Étkezések", 15.5f, TXT, true));
        col.addView(gap(10));
        listBox = vbox();
        col.addView(listBox, lp());

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_reminders", 3));
        col.post(() -> Ux.enterChildren(col, 30, 45));
        cleanupOldWaterKeys();
        refresh();
    }

    /** 30 napnál régebbi napi víz-kulcsok törlése, hogy ne gyűljenek a beállítások közt. */
    void cleanupOldWaterKeys() {
        try {
            android.content.SharedPreferences p = getSharedPreferences("edzo", MODE_PRIVATE);
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, -30);
            int cutoff = c.get(Calendar.YEAR) * 10000
                    + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
            android.content.SharedPreferences.Editor e = p.edit();
            for (String k : p.getAll().keySet())
                if (k.startsWith("water_") && !k.equals("water_goal_cl")) {
                    try {
                        if (Integer.parseInt(k.substring(6)) < cutoff) e.remove(k);
                    } catch (NumberFormatException ignored) {}
                }
            e.apply();
        } catch (Exception ignored) {}
    }

    void refresh() {
        refreshToday();
        refreshWater();
        refreshWeek();
        refreshQuick();
        listBox.removeAllViews();
        List<MealLog.Meal> meals = MealLog.load(this);
        // Mindig időrendben (szerkesztés után se ugorjon a lista elejére a bejegyzés).
        java.util.Collections.sort(meals, (a, b2) -> Long.compare(b2.ts, a.ts));
        if (meals.isEmpty()) {
            listBox.addView(text("Még nincs bejegyzés. Add hozzá az első étkezésed fent!",
                    13.5f, MUTED, false));
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("MMM d. · HH:mm", new Locale("hu"));
        long dayStart = dayStartMs();
        // Napi összegek a nap-fejlécekhez.
        java.util.HashMap<Long, Double> daySum = new java.util.HashMap<>();
        for (MealLog.Meal m : meals) {
            long d0 = dayStartOf(m.ts);
            Double s = daySum.get(d0);
            daySum.put(d0, (s == null ? 0 : s) + m.kcal());
        }
        SimpleDateFormat hf = new SimpleDateFormat("MMMM d., EEEE", new Locale("hu"));
        long shownDay = -1;
        for (int i = 0; i < meals.size(); i++) {
            final MealLog.Meal m = meals.get(i);
            final int idx = i;
            long d0 = dayStartOf(m.ts);
            if (d0 < dayStart && d0 != shownDay) {
                Double s = daySum.get(d0);
                TextView sep = text(hf.format(new Date(m.ts))
                        + "  ·  " + Math.round(s == null ? 0 : s) + " kcal",
                        12.5f, MUTED, true);
                sep.setPadding(dp(2), dp(8), 0, dp(8));
                listBox.addView(sep);
                shownDay = d0;
            }
            LinearLayout c = card();
            c.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout top = hbox();
            top.setGravity(Gravity.CENTER_VERTICAL);
            String title = m.name.isEmpty()
                    ? (m.items.isEmpty() ? mealSlot(m.ts)
                        : mealSlot(m.ts) + " · " + m.items.get(0).food)
                    : m.name;
            // Bélyegkép, ha van fotó a bejegyzéshez.
            if (!m.photo.isEmpty()) {
                try {
                    android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(
                            new java.io.File(getFilesDir(), m.photo).getAbsolutePath());
                    if (bm != null) {
                        android.widget.ImageView iv = new android.widget.ImageView(this);
                        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        iv.setImageBitmap(bm);
                        iv.setClipToOutline(true);
                        iv.setOutlineProvider(new android.view.ViewOutlineProvider() {
                            @Override public void getOutline(View v, android.graphics.Outline o) {
                                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(9));
                            }
                        });
                        LinearLayout.LayoutParams ivlp =
                                new LinearLayout.LayoutParams(dp(40), dp(40));
                        ivlp.rightMargin = dp(10);
                        top.addView(iv, ivlp);
                    }
                } catch (Exception ignored) {}
            }
            top.addView(text(title, 15, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
            String kc = Math.round(m.kcal()) + " kcal"
                    + (m.protein() > 0 ? " · " + Math.round(m.protein()) + "g P" : "");
            top.addView(text(kc, 14, Theme.accent(this), true));
            c.addView(top, lp());
            StringBuilder det = new StringBuilder();
            for (MealLog.Item it : m.items) {
                if (det.length() > 0) det.append("  ·  ");
                det.append(it.food).append(" ").append(Math.round(it.grams)).append(" g");
            }
            TextView dt = text(det.toString(), 12.5f, MUTED, false);
            dt.setPadding(0, dp(4), 0, 0);
            c.addView(dt);
            TextView when = text(df.format(new Date(m.ts)), 11.5f, MUTED, false);
            when.setPadding(0, dp(3), 0, 0);
            c.addView(when);
            c.setClickable(true);
            c.setOnClickListener(v -> {
                Sheet sh = new Sheet(this, title,
                        Math.round(m.kcal()) + " kcal · " + Math.round(m.grams()) + " g"
                        + (m.protein() > 0 ? " · " + Math.round(m.protein()) + " g fehérje" : ""));
                // Nagy fotó a részleteknél – segít utólag pontosítani az arányokat.
                if (!m.photo.isEmpty()) {
                    try {
                        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(
                                new java.io.File(getFilesDir(), m.photo).getAbsolutePath());
                        if (bm != null) {
                            android.widget.ImageView iv = new android.widget.ImageView(this);
                            iv.setImageBitmap(bm);
                            iv.setAdjustViewBounds(true);
                            iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                            LinearLayout pv = vbox();
                            pv.setPadding(dp(8), 0, dp(8), dp(8));
                            pv.addView(iv, lp());
                            sh.addCustom(pv);
                        }
                    } catch (Exception ignored) {}
                }
                if (m.items.size() >= 2)
                    sh.addRow("⚖️", "Arányok igazítása", "Csúszkákkal, a fotó alapján – a kcal élőben frissül",
                            false, true, () -> adjustRatiosSheet(m, idx));
                sh.addRow("✏️", "Szerkesztés", "Összetevők és grammok módosítása",
                        false, true, () -> addMealDialog(m, idx));
                sh.addRow("🔁", "Újra most", "Ugyanez az étkezés naplózása mostani időponttal",
                        false, true, () -> {
                            MealLog.add(this, new MealLog.Meal(System.currentTimeMillis(),
                                    m.name, m.items, ""));
                            refresh();
                            Ux.blazeCard(this, "🍽 Újra naplózva ✔  "
                                    + Math.round(m.kcal()) + " kcal");
                        });
                sh.addRow("📷", m.photo.isEmpty() ? "Fotó csatolása" : "Új fotó készítése",
                        "A tányérod képe a bejegyzéshez", false, true, () -> capturePhoto(m.ts));
                sh.addRow("🕒", "Időpont módosítása", "Ha máskor etted, mint amikor beírtad",
                        false, true, () -> editMealTime(m));
                sh.addDestructive("🗑 Törlés", () -> { MealLog.removeByTs(this, m.ts); refresh(); });
                sh.addCancel().show();
            });
            listBox.addView(c, lp());
            listBox.addView(gap(10));
        }
    }

    /** Gyors-naplózás: a leggyakoribb étkezések csipjei, egy koppintásra újra. */
    void refreshQuick() {
        quickBox.removeAllViews();
        List<MealLog.Meal> meals = MealLog.load(this);
        java.util.LinkedHashMap<String, MealLog.Meal> latest = new java.util.LinkedHashMap<>();
        java.util.HashMap<String, Integer> count = new java.util.HashMap<>();
        for (MealLog.Meal m : meals) {
            if (m.items.isEmpty()) continue;
            String key = m.name.isEmpty() ? m.items.get(0).food : m.name;
            Integer c = count.get(key);
            count.put(key, c == null ? 1 : c + 1);
            if (!latest.containsKey(key)) latest.put(key, m); // a load() legújabb-elöl sorrendű
        }
        List<String> keys = new ArrayList<>(latest.keySet());
        java.util.Collections.sort(keys, (a, b2) -> count.get(b2) - count.get(a));
        int shown = 0;
        LinearLayout row = null;
        for (String key : keys) {
            if (count.get(key) < 2 || shown >= 3) break; // csak ami tényleg visszatérő
            final MealLog.Meal src = latest.get(key);
            if (row == null) {
                quickBox.addView(gap(10));
                TextView t = text("Gyakoriak – koppints az újranaplózáshoz", 11.5f, MUTED, false);
                quickBox.addView(t);
                row = hbox();
                android.widget.HorizontalScrollView hsv =
                        new android.widget.HorizontalScrollView(this);
                hsv.setHorizontalScrollBarEnabled(false);
                hsv.addView(row, new android.widget.FrameLayout.LayoutParams(-2, -2));
                LinearLayout.LayoutParams rl = lp();
                rl.topMargin = dp(6);
                quickBox.addView(hsv, rl);
            }
            TextView chip = text((key.length() > 16 ? key.substring(0, 15) + "…" : key)
                    + " · " + Math.round(src.kcal()) + " kcal", 12, TXT, true);
            GradientDrawable cbg = new GradientDrawable();
            cbg.setColor(CARD2);
            cbg.setCornerRadius(dp(18));
            cbg.setStroke(dp(1), LINE);
            chip.setBackground(cbg);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                MealLog.add(this, new MealLog.Meal(System.currentTimeMillis(),
                        src.name, src.items, ""));
                refresh();
                Ux.blazeCard(this, "🍽 Újra naplózva ✔  " + Math.round(src.kcal()) + " kcal");
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(8);
            row.addView(chip, clp);
            shown++;
        }
    }

    /** A mai összesítő kártya: kcal, és cél esetén haladássáv + maradék. */
    void refreshToday() {
        todayCard.removeAllViews();
        double kcal = MealLog.todayKcal(this);
        int goal = getSharedPreferences("edzo", MODE_PRIVATE).getInt("kcal_goal", 0);
        todayCard.addView(text("🍽 Ma összesen" + (goal > 0 ? "  ·  cél: " + goal + " kcal" : ""),
                12.5f, MUTED, true));
        todayCard.addView(text(Math.round(kcal) + " kcal", 26, Theme.accent(this), true));
        double prot = MealLog.todayProtein(this);
        int pGoal = getSharedPreferences("edzo", MODE_PRIVATE).getInt("protein_goal", 0);
        if (prot > 0 || pGoal > 0) {
            boolean done = pGoal > 0 && prot >= pGoal;
            TextView pt = text("🥩 Fehérje ma: " + Math.round(prot) + " g"
                    + (pGoal > 0 ? " / " + pGoal + " g" + (done ? "  ✔" : "") : ""),
                    12.5f, done ? Theme.accent(this) : MUTED, done);
            pt.setPadding(0, dp(2), 0, 0);
            todayCard.addView(pt);
            if (pGoal > 0) {
                LinearLayout pBg = hbox();
                GradientDrawable pgd = new GradientDrawable();
                pgd.setColor(0x22FFFFFF);
                pgd.setCornerRadius(dp(4));
                pBg.setBackground(pgd);
                View pf = new View(this);
                GradientDrawable pfg = new GradientDrawable();
                pfg.setColor(done ? 0xFF22C55E : Theme.accent(this));
                pfg.setCornerRadius(dp(4));
                pf.setBackground(pfg);
                float ff = (float) Math.max(prot > 0 ? 0.02 : 0.0, Math.min(1.0, prot / pGoal));
                pBg.addView(pf, new LinearLayout.LayoutParams(0, dp(6), ff));
                pBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(6), 1f - ff));
                LinearLayout.LayoutParams plp = lp();
                plp.topMargin = dp(4);
                todayCard.addView(pBg, plp);
            }
        }
        if (goal > 0) {
            LinearLayout barBg = hbox();
            GradientDrawable bgd = new GradientDrawable();
            bgd.setColor(0x22FFFFFF);
            bgd.setCornerRadius(dp(6));
            barBg.setBackground(bgd);
            View fill = new View(this);
            boolean over = kcal > goal;
            GradientDrawable fgd = new GradientDrawable();
            fgd.setColor(over ? 0xFFF59E0B : Theme.accent(this));
            fgd.setCornerRadius(dp(6));
            fill.setBackground(fgd);
            float f = (float) Math.max(0.02, Math.min(1.0, kcal / goal));
            barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(10), f));
            barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(10), 1f - f));
            LinearLayout.LayoutParams blp = lp();
            blp.topMargin = dp(8);
            todayCard.addView(barBg, blp);
            TextView st = text(over
                    ? "+" + Math.round(kcal - goal) + " kcal a cél felett"
                    : "még " + Math.round(goal - kcal) + " kcal fér bele ma", 12, MUTED, false);
            st.setPadding(0, dp(5), 0, 0);
            todayCard.addView(st);
        } else {
            TextView hint = text("Koppints ide a napi kalória- és fehérje-cél beállításához",
                    11.5f, MUTED, false);
            hint.setPadding(0, dp(4), 0, 0);
            todayCard.addView(hint);
        }
    }

    /** A mai nap víz-kulcsa (napváltáskor magától nullázódik). */
    String waterKey() {
        Calendar c = Calendar.getInstance();
        return "water_" + (c.get(Calendar.YEAR) * 10000
                + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH));
    }

    /** Vízbevitel-kártya: pohár (2,5 dl) hozzáadása/levonása, cél haladássávval. */
    void refreshWater() {
        waterCard.removeAllViews();
        final android.content.SharedPreferences p = getSharedPreferences("edzo", MODE_PRIVATE);
        int cl = p.getInt(waterKey(), 0);          // centiliterben (1 pohár = 25 cl)
        int goalCl = p.getInt("water_goal_cl", 200);
        boolean done = cl >= goalCl;
        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text("💧 Víz ma: " + (cl / 100.0) + " / " + (goalCl / 100.0) + " l"
                + (done ? "  ✔" : ""), 13.5f, done ? Theme.accent(this) : TXT, true);
        label.setClickable(true);
        label.setOnClickListener(v -> waterGoalDialog());
        top.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView minus = text("−", 20, MUTED, true);
        minus.setPadding(dp(12), dp(2), dp(12), dp(2));
        minus.setClickable(true);
        minus.setOnClickListener(v -> {
            int cur = p.getInt(waterKey(), 0);
            p.edit().putInt(waterKey(), Math.max(0, cur - 25)).apply();
            refreshWater();
        });
        top.addView(minus);
        TextView plus = text("＋ pohár", 14, Theme.accent(this), true);
        plus.setPadding(dp(12), dp(2), dp(2), dp(2));
        plus.setClickable(true);
        plus.setOnClickListener(v -> {
            int cur = p.getInt(waterKey(), 0);
            p.edit().putInt(waterKey(), cur + 25).apply();
            refreshWater();
            if (cur < goalCl && cur + 25 >= goalCl) {
                Ux.blazeCard(this, "💧 Napi vízcél megvan – szép munka!");
                // Tartós számláló a Hidratált jelvényhez (naponta legfeljebb egyszer nő).
                Calendar tc = Calendar.getInstance();
                int today = tc.get(Calendar.YEAR) * 10000
                        + (tc.get(Calendar.MONTH) + 1) * 100 + tc.get(Calendar.DAY_OF_MONTH);
                if (p.getInt("water_last_done", 0) != today)
                    p.edit().putInt("water_last_done", today)
                            .putInt("water_days_done", p.getInt("water_days_done", 0) + 1)
                            .apply();
            }
        });
        top.addView(plus);
        waterCard.addView(top, lp());
        LinearLayout barBg = hbox();
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(0x22FFFFFF);
        bgd.setCornerRadius(dp(4));
        barBg.setBackground(bgd);
        View fill = new View(this);
        GradientDrawable fgd = new GradientDrawable();
        fgd.setColor(done ? 0xFF22C55E : 0xFF38BDF8);
        fgd.setCornerRadius(dp(4));
        fill.setBackground(fgd);
        float f = (float) Math.max(cl > 0 ? 0.02 : 0.0, Math.min(1.0, cl / (double) goalCl));
        barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(6), f));
        barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(6), 1f - f));
        LinearLayout.LayoutParams blp = lp();
        blp.topMargin = dp(7);
        waterCard.addView(barBg, blp);
    }

    void waterGoalDialog() {
        final EditText et = input("Napi vízcél (dl, pl. 20)");
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        int cur = getSharedPreferences("edzo", MODE_PRIVATE).getInt("water_goal_cl", 200);
        et.setText(String.valueOf(cur / 10));
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        box.addView(et, lp());
        new Sheet(this, "Napi vízcél 💧", "Egy pohár = 2,5 dl")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    int g = (int) parse(et.getText().toString()); // dl-ben kérjük
                    if (g < 5) g = 5;
                    getSharedPreferences("edzo", MODE_PRIVATE).edit()
                            .putInt("water_goal_cl", g * 10).apply();
                    refreshWater();
                })
                .addCancel()
                .show();
    }

    /** Az elmúlt 7 nap napi kcal-összegei vízszintes sávokkal. */
    void refreshWeek() {
        weekCard.removeAllViews();
        long dayMs = 24L * 3600 * 1000;
        long today0 = dayStartMs();
        double[] sums = new double[7]; // [0]=ma, [6]=6 napja
        for (MealLog.Meal m : MealLog.load(this)) {
            long diff = today0 - dayStartOf(m.ts);
            int k = (int) (diff / dayMs);
            if (k >= 0 && k < 7) sums[k] += m.kcal();
        }
        double max = 1;
        for (double s : sums) max = Math.max(max, s);
        int goal = getSharedPreferences("edzo", MODE_PRIVATE).getInt("kcal_goal", 0);
        if (goal > 0) max = Math.max(max, goal);
        double weekSum = 0; int daysWith = 0;
        for (double sVal : sums) { weekSum += sVal; if (sVal > 0) daysWith++; }
        weekCard.addView(text("Elmúlt 7 nap"
                + (daysWith > 0 ? "  ·  átlag " + Math.round(weekSum / daysWith) + " kcal/nap" : ""),
                12.5f, MUTED, true));
        SimpleDateFormat dnf = new SimpleDateFormat("EEE", new Locale("hu"));
        for (int k = 6; k >= 0; k--) {
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rl = lp();
            rl.topMargin = dp(6);
            String label = k == 0 ? "Ma" : dnf.format(new Date(today0 - k * dayMs));
            TextView lb = text(label, 11.5f, k == 0 ? TXT : MUTED, k == 0);
            row.addView(lb, new LinearLayout.LayoutParams(dp(40), -2));
            LinearLayout barBg = hbox();
            GradientDrawable bgd = new GradientDrawable();
            bgd.setColor(0x1AFFFFFF);
            bgd.setCornerRadius(dp(5));
            barBg.setBackground(bgd);
            View fill = new View(this);
            GradientDrawable fgd = new GradientDrawable();
            boolean over = goal > 0 && sums[k] > goal;
            fgd.setColor(sums[k] <= 0 ? 0x00000000 : over ? 0xFFF59E0B : Theme.accent(this));
            fgd.setCornerRadius(dp(5));
            fill.setBackground(fgd);
            float f = (float) Math.max(sums[k] > 0 ? 0.03 : 0.0, Math.min(1.0, sums[k] / max));
            barBg.addView(fill, new LinearLayout.LayoutParams(0, dp(9), f));
            barBg.addView(new View(this), new LinearLayout.LayoutParams(0, dp(9), 1f - f));
            row.addView(barBg, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView val = text(sums[k] > 0 ? "  " + Math.round(sums[k]) : "  –",
                    11.5f, MUTED, false);
            val.setGravity(Gravity.END);
            row.addView(val, new LinearLayout.LayoutParams(dp(52), -2));
            if (sums[k] > 0) {
                final long dTs = today0 - k * dayMs;
                row.setClickable(true);
                row.setOnClickListener(v -> daySheet(dTs));
            }
            weekCard.addView(row, rl);
        }
    }

    long dayStartOf(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    void editGoalDialog() {
        final EditText et = input("Napi cél (kcal), 0 = kikapcsolva");
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        int cur = getSharedPreferences("edzo", MODE_PRIVATE).getInt("kcal_goal", 0);
        if (cur > 0) et.setText(String.valueOf(cur));
        final EditText pEt = input("Napi fehérje-cél (g), 0 = kikapcsolva");
        pEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        int pCur = getSharedPreferences("edzo", MODE_PRIVATE).getInt("protein_goal", 0);
        if (pCur > 0) pEt.setText(String.valueOf(pCur));
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        box.addView(et, lp());
        LinearLayout.LayoutParams pl = lp();
        pl.topMargin = dp(8);
        box.addView(pEt, pl);
        Sheet sh = new Sheet(this, "Napi célok 🎯",
                "Tipp: a Profil oldalon a BMR-ed jó kiindulási alap.")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    int g = (int) parse(et.getText().toString());
                    int pg = (int) parse(pEt.getText().toString());
                    getSharedPreferences("edzo", MODE_PRIVATE).edit()
                            .putInt("kcal_goal", Math.max(0, g))
                            .putInt("protein_goal", Math.max(0, pg)).apply();
                    refresh();
                });
        // Ha a Profil adataiból számolható BMR, egy koppintással betölthető.
        double bmr = Profile.bmr(Profile.getSex(this), Profile.lastWeight(this),
                Profile.getHeight(this), Profile.ageYears(this));
        if (bmr > 0) {
            final int suggested = (int) Math.round(bmr * 1.35); // mérsékelt aktivitás
            sh.addNeutral("⚡ BMR alapján: ~" + suggested + " kcal", () -> {
                getSharedPreferences("edzo", MODE_PRIVATE).edit()
                        .putInt("kcal_goal", suggested).apply();
                refresh();
            });
        }
        // Fehérje-javaslat a testsúlyból: ~1,6 g/kg edzőknek.
        double w = Profile.lastWeight(this);
        if (w > 0) {
            final int pSug = (int) Math.round(w * 1.6);
            sh.addNeutral("🥩 Testsúly alapján: ~" + pSug + " g fehérje", () -> {
                getSharedPreferences("edzo", MODE_PRIVATE).edit()
                        .putInt("protein_goal", pSug).apply();
                refresh();
            });
        }
        sh.addCancel().show();
    }

    // ---------- Új étkezés ----------

    void addMealDialog() { addMealDialog(null, -1); }

    /** Új étkezés felvitele, vagy meglévő szerkesztése (existing != null esetén). */
    void addMealDialog(final MealLog.Meal existing, final int editIdx) {
        final LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);

        final EditText nameEt = input("Étkezés neve (pl. Rántott hús rizzsel)");
        if (existing != null) nameEt.setText(existing.name);
        box.addView(nameEt, lp());
        // Élő visszajelzés: mit ismer fel az app a beírt névből.
        final TextView reco = text("", 11.5f, MUTED, false);
        reco.setPadding(dp(2), dp(4), 0, 0);
        box.addView(reco);
        nameEt.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String q = s.toString().trim();
                if (q.length() < 3) { reco.setText(""); return; }
                List<Foods.Food> g = Foods.findAll(q);
                if (g.isEmpty()) { reco.setText("🔍 Ezt még nem ismerem – írd be lent összetevőnként!"); return; }
                StringBuilder sb = new StringBuilder("✔ Felismerve: ");
                for (int i = 0; i < g.size(); i++) {
                    if (i > 0) sb.append(" + ");
                    sb.append(g.get(i).name);
                }
                reco.setText(sb.toString());
            }
        });
        box.addView(gap(10));

        box.addView(text("Összetevők (étel + gramm)", 12.5f, MUTED, true));
        final LinearLayout itemsBox = vbox();
        final List<EditText[]> rows = new ArrayList<>();
        if (existing != null) {
            for (MealLog.Item it : existing.items) {
                addItemRow(itemsBox, rows);
                EditText[] r = rows.get(rows.size() - 1);
                r[0].setText(it.food);
                r[1].setText(String.valueOf(Math.round(it.grams)));
            }
            addItemRow(itemsBox, rows);
        } else {
            addItemRow(itemsBox, rows);
            addItemRow(itemsBox, rows);
        }
        box.addView(itemsBox, lp());

        Button more = ghost("＋  Összetevő");
        more.setTextSize(13.5f);
        more.setOnClickListener(v -> addItemRow(itemsBox, rows));
        box.addView(more);
        box.addView(gap(10));

        box.addView(text("Vagy add meg a teljes adag grammját – az üresen hagyott "
                + "összetevők közt elosztjuk. Ha ezt is üresen hagyod, az ételek "
                + "tipikus adagjával számolunk.", 12, MUTED, false));
        final EditText totalEt = input("Teljes adag (g), nem kötelező");
        totalEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(totalEt, lp());

        new Sheet(this, existing == null ? "Új étkezés 🍽" : "Étkezés szerkesztése ✏️")
                .addCustom(box)
                .addPrimary("Mentés", () -> saveMeal(nameEt, rows, totalEt, existing, editIdx))
                .addCancel()
                .show();
    }

    void addItemRow(LinearLayout itemsBox, List<EditText[]> rows) {
        LinearLayout row = hbox();
        EditText food = input("Étel (pl. rizs)");
        EditText grams = input("g");
        grams.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, -2, 1f);
        flp.rightMargin = dp(6);
        row.addView(food, flp);
        row.addView(grams, new LinearLayout.LayoutParams(dp(76), -2));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = dp(6);
        itemsBox.addView(row, rlp);
        rows.add(new EditText[]{food, grams});
    }

    void saveMeal(EditText nameEt, List<EditText[]> rows, EditText totalEt,
                  MealLog.Meal existing, int editIdx) {
        List<String> foods = new ArrayList<>();
        List<Double> grams = new ArrayList<>();
        for (EditText[] r : rows) {
            String f = r[0].getText().toString().trim();
            if (f.isEmpty()) continue;
            foods.add(f);
            grams.add(parse(r[1].getText().toString()));
        }
        if (foods.isEmpty()) {
            // Okos bevitel: ha csak a nevet írtad be ("rántott hús rizzsel"),
            // az összetevőket a névből ismerjük fel.
            List<Foods.Food> guessed = Foods.findAll(nameEt.getText().toString());
            for (Foods.Food f : guessed) { foods.add(f.name); grams.add(0.0); }
        }
        if (foods.isEmpty()) {
            Toast.makeText(this, "Adj meg legalább egy összetevőt, vagy írd a névbe, "
                    + "mit ettél (pl. rántott hús rizzsel).", Toast.LENGTH_LONG).show();
            return;
        }
        // Ételek felismerése előre (a tipikus adagméretekhez is kell).
        List<Foods.Food> resolved = new ArrayList<>();
        for (String fq : foods) resolved.add(Foods.find(fq));

        // Közös gramm szétosztása a megadatlan összetevők közt; ha nincs közös
        // gramm sem, az adott étel tipikus adagjával számolunk.
        double total = parse(totalEt.getText().toString());
        double given = 0; int missing = 0;
        for (double g : grams) { if (g > 0) given += g; else missing++; }
        if (missing > 0) {
            double remain = total > given ? total - given : 0;
            double each = remain > 0 ? remain / missing : -1;
            for (int i = 0; i < grams.size(); i++)
                if (grams.get(i) <= 0) {
                    Foods.Food f = resolved.get(i);
                    grams.set(i, each > 0 ? each : (f != null ? f.portion : 150));
                }
        }

        List<MealLog.Item> items = new ArrayList<>();
        boolean estimated = false;
        for (int i = 0; i < foods.size(); i++) {
            Foods.Food f = resolved.get(i);
            int kcal100;
            double prot100;
            String label;
            if (f != null) { kcal100 = f.kcal100; prot100 = f.prot100; label = f.name; }
            else { kcal100 = 150; prot100 = 0; label = foods.get(i); estimated = true; } // becslés
            double g = grams.get(i);
            items.add(new MealLog.Item(label, g, kcal100 * g / 100.0, prot100 * g / 100.0));
        }
        // Szerkesztésnél az eredeti időpont és fotó megmarad.
        long ts = existing != null ? existing.ts : System.currentTimeMillis();
        String photo = existing != null ? existing.photo : "";
        MealLog.Meal meal = new MealLog.Meal(ts, nameEt.getText().toString().trim(), items, photo);
        if (existing != null) MealLog.removeByTs(this, existing.ts);
        MealLog.add(this, meal);
        refresh();
        String msg = "Mentve ✔  " + Math.round(meal.kcal()) + " kcal";
        if (estimated) msg += "  (ismeretlen ételnél ~becslés)";
        // Cél-tudatos visszajelzés: mennyi fér még a mai keretbe (csak mai étkezésnél).
        int kGoal = getSharedPreferences("edzo", MODE_PRIVATE).getInt("kcal_goal", 0);
        if (kGoal > 0 && ts >= dayStartMs()) {
            int left = kGoal - (int) Math.round(MealLog.todayKcal(this));
            msg += left >= 0 ? "  ·  még " + left + " kcal fér ma"
                    : "  ·  " + (-left) + " kcal-lal a cél felett";
        }
        Ux.blazeCard(this, "🍽 " + msg);
    }

    /** Összetevő-arányok igazítása csúszkákkal; az össz-gramm változatlan marad. */
    void adjustRatiosSheet(final MealLog.Meal m, final int idx) {
        final double total = m.grams() > 0 ? m.grams() : 300;
        final int n = m.items.size();
        final double[] shares = new double[n];
        double initSum = 0;
        for (int i = 0; i < n; i++) { shares[i] = Math.max(1, m.items.get(i).grams); initSum += shares[i]; }

        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        // A fotó referenciaként (ha van).
        if (!m.photo.isEmpty()) {
            try {
                android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(
                        new java.io.File(getFilesDir(), m.photo).getAbsolutePath());
                if (bm != null) {
                    android.widget.ImageView iv = new android.widget.ImageView(this);
                    iv.setImageBitmap(bm);
                    iv.setAdjustViewBounds(true);
                    box.addView(iv, lp());
                    box.addView(gap(8));
                }
            } catch (Exception ignored) {}
        }
        final TextView totalTv = text("", 13.5f, Theme.accent(this), true);
        final TextView[] labels = new TextView[n];
        final Runnable update = () -> {
            double sum = 0;
            for (double sv2 : shares) sum += sv2;
            double kcal = 0;
            for (int i = 0; i < n; i++) {
                double g = shares[i] / sum * total;
                MealLog.Item it = m.items.get(i);
                double kpg = it.grams > 0 ? it.kcal / it.grams : 1.5;
                kcal += kpg * g;
                labels[i].setText(it.food + "  ·  " + Math.round(g) + " g");
            }
            totalTv.setText("Összesen: " + Math.round(total) + " g  ·  ~" + Math.round(kcal) + " kcal");
        };
        for (int i = 0; i < n; i++) {
            final int ii = i;
            labels[i] = text("", 13.5f, TXT, true);
            LinearLayout.LayoutParams llp = lp();
            llp.topMargin = dp(i == 0 ? 0 : 6);
            box.addView(labels[i], llp);
            android.widget.SeekBar sb = new android.widget.SeekBar(this);
            sb.setMax(100);
            sb.setProgress((int) Math.round(shares[i] / initSum * 100));
            sb.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(android.widget.SeekBar s, int p, boolean u) {
                    shares[ii] = Math.max(1, p);
                    update.run();
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar s) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar s) {}
            });
            box.addView(sb, lp());
        }
        LinearLayout.LayoutParams tlp = lp();
        tlp.topMargin = dp(8);
        box.addView(totalTv, tlp);
        update.run();

        new Sheet(this, "Arányok igazítása ⚖️",
                "Az össz-gramm marad, csak az elosztás változik.")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    double sum = 0;
                    for (double sv2 : shares) sum += sv2;
                    List<MealLog.Item> ni = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        MealLog.Item it = m.items.get(i);
                        double g = shares[i] / sum * total;
                        double kpg = it.grams > 0 ? it.kcal / it.grams : 1.5;
                        double ppg = it.grams > 0 ? it.protein / it.grams : 0;
                        ni.add(new MealLog.Item(it.food, g, kpg * g, ppg * g));
                    }
                    MealLog.removeByTs(this, m.ts);
                    MealLog.add(this, new MealLog.Meal(m.ts, m.name, ni, m.photo));
                    refresh();
                })
                .addCancel()
                .show();
    }

    // ---------- Fotó ----------

    void capturePhoto(long ts) {
        pendingPhotoTs = ts;
        try {
            startActivityForResult(new android.content.Intent(
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE), REQ_PHOTO);
        } catch (Exception e) {
            Toast.makeText(this, "Nem található kamera-alkalmazás.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PHOTO || res != RESULT_OK || data == null || pendingPhotoTs <= 0) return;
        try {
            android.graphics.Bitmap bm = (android.graphics.Bitmap) data.getParcelableExtra("data");
            if (bm == null) return;
            java.io.File f = new java.io.File(getFilesDir(), "meal_" + pendingPhotoTs + ".jpg");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
            bm.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, fo);
            fo.close();
            MealLog.updatePhoto(this, pendingPhotoTs, f.getName());
            Toast.makeText(this, "Fotó csatolva 📷", Toast.LENGTH_SHORT).show();
            refresh();
        } catch (Exception ignored) {}
        pendingPhotoTs = 0;
    }

    double parse(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); }
        catch (Exception e) { return 0; }
    }

    /** Egy nap étkezéseinek gyors áttekintése (a heti sávra koppintva). */
    void daySheet(long day0) {
        long dayMs = 24L * 3600 * 1000;
        List<MealLog.Meal> meals = MealLog.load(this);
        java.util.Collections.sort(meals, (a, b2) -> Long.compare(a.ts, b2.ts));
        double kSum = 0, pSum = 0;
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", new Locale("hu"));
        for (MealLog.Meal m : meals) {
            if (m.ts < day0 || m.ts >= day0 + dayMs) continue;
            kSum += m.kcal(); pSum += m.protein();
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            String title = m.name.isEmpty()
                    ? (m.items.isEmpty() ? mealSlot(m.ts)
                        : mealSlot(m.ts) + " · " + m.items.get(0).food)
                    : m.name;
            row.addView(text(tf.format(new Date(m.ts)) + "  " + title, 13.5f, TXT, false),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text(Math.round(m.kcal()) + " kcal", 12.5f, MUTED, false));
            LinearLayout.LayoutParams rl = lp();
            rl.topMargin = dp(7);
            box.addView(row, rl);
        }
        SimpleDateFormat hf = new SimpleDateFormat("MMMM d., EEEE", new Locale("hu"));
        new Sheet(this, hf.format(new Date(day0)),
                Math.round(kSum) + " kcal"
                + (pSum > 0 ? " · " + Math.round(pSum) + " g fehérje" : ""))
                .addCustom(box)
                .addCancel()
                .show();
    }

    /** Kereshető kalóriatáblázat a beépített étel-adatbázisból. */
    void foodTableSheet() {
        final LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        final EditText search = input("Keresés (pl. csirke)");
        box.addView(search, lp());
        final LinearLayout listV = vbox();
        LinearLayout.LayoutParams llp = lp();
        llp.topMargin = dp(8);
        box.addView(listV, llp);
        final Runnable render = () -> {
            listV.removeAllViews();
            String q = Foods.norm(search.getText().toString().trim());
            int shown = 0;
            for (Foods.Food f : Foods.ALL) {
                if (!q.isEmpty() && !Foods.norm(f.name).contains(q)) {
                    boolean stemHit = false;
                    for (String st : f.stems)
                        if (Foods.norm(st).contains(q)) { stemHit = true; break; }
                    if (!stemHit) continue;
                }
                if (shown >= 25) {
                    listV.addView(text("… szűkítsd a keresést a többihez", 11.5f, MUTED, false));
                    break;
                }
                LinearLayout row = hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(text(f.name, 13.5f, TXT, false),
                        new LinearLayout.LayoutParams(0, -2, 1f));
                row.addView(text(f.kcal100 + " kcal · " + (Math.round(f.prot100 * 10) / 10.0)
                        + "g P /100g", 12, MUTED, false));
                LinearLayout.LayoutParams rl = lp();
                rl.topMargin = dp(7);
                listV.addView(row, rl);
                shown++;
            }
            if (shown == 0)
                listV.addView(text("Nincs találat – az étkezésnél becsléssel is menthetsz.",
                        12, MUTED, false));
        };
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { render.run(); }
        });
        render.run();
        new Sheet(this, "Kalóriatáblázat 📖",
                "kcal és fehérje 100 grammonként (közelítő értékek)")
                .addCustom(box)
                .addCancel()
                .show();
    }

    /** Dátum + idő választó egy bejegyzés időpontjának utólagos módosításához. */
    void editMealTime(final MealLog.Meal m) {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(m.ts);
        new android.app.DatePickerDialog(this, (dp, y, mo, d) ->
                new android.app.TimePickerDialog(this, (tp, h, min) -> {
                    Calendar nc = Calendar.getInstance();
                    nc.set(y, mo, d, h, min, 0);
                    nc.set(Calendar.MILLISECOND, 0);
                    long nts = nc.getTimeInMillis();
                    if (nts > System.currentTimeMillis()) {
                        Toast.makeText(this, "Jövőbeli időpont nem adható meg.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    MealLog.updateTs(this, m.ts, nts);
                    refresh();
                    Ux.blazeCard(this, "🕒 Időpont átállítva ✔");
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show(),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /** Napszak szerinti címke a névtelen étkezéseknek (Reggeli/Ebéd/Vacsora…). */
    static String mealSlot(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        int h = c.get(Calendar.HOUR_OF_DAY);
        if (h < 4)  return "Éjszakai nasi";
        if (h < 10) return "Reggeli";
        if (h < 12) return "Tízórai";
        if (h < 15) return "Ebéd";
        if (h < 18) return "Uzsonna";
        if (h < 22) return "Vacsora";
        return "Éjszakai nasi";
    }

    long dayStartMs() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // ---------- UI segédek ----------

    EditText input(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(MUTED);
        et.setTextColor(TXT);
        et.setTextSize(14.5f);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), MainActivity.GLASS_LINE);
        et.setBackground(bg);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        return et;
    }

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(MainActivity.GLASS);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), MainActivity.GLASS_LINE);
        c.setBackground(bg);
        return c;
    }

    TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    View gap(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(h))); return v; }

    Button primary(String label) {
        Button b = base(label);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.accent(this), Theme.accent2(this)});
        bg.setCornerRadius(dp(16));
        b.setBackground(bg); b.setTextColor(0xFFFFFFFF); b.setTextSize(16);
        return b;
    }

    Button ghost(String label) {
        Button b = base(label);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2); bg.setCornerRadius(dp(13)); bg.setStroke(dp(1), LINE);
        b.setBackground(bg); b.setTextSize(14.5f);
        return b;
    }

    Button base(String label) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextColor(TXT);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(dp(14), dp(13), dp(14), dp(13));
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        b.setStateListAnimator(null);
        return b;
    }

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
