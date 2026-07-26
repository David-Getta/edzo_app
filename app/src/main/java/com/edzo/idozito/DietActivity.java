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
    EditText searchEt;
    static final int REQ_PHOTO = 61, REQ_PICK = 62;
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
        col.addView(gap(8));
        searchEt = input("Keresés a naplóban (pl. csirke)");
        searchEt.setTextSize(13);
        searchEt.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { refreshList(); }
        });
        col.addView(searchEt, lp());
        col.addView(gap(10));
        listBox = vbox();
        col.addView(listBox, lp());

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_reminders", 3));
        col.post(() -> Ux.enterChildren(col, 30, 45));
        cleanupOldWaterKeys();
        MealLog.cleanupOrphanPhotos(this);
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
                if (Water.isDayKey(k) && Water.dayOf(k) < cutoff) e.remove(k);
            e.apply();
        } catch (Exception ignored) {}
    }

    // Egy frissítési menetben csak egyszer olvassuk be a naplót.
    List<MealLog.Meal> mealsCache;

    /** Egyszerre ennyi bejegyzés-kártya épül fel; a gomb továbbiakat tölt be. */
    static final int PAGE = 60;
    int shownLimit = PAGE;

    List<MealLog.Meal> meals() {
        if (mealsCache == null) mealsCache = MealLog.load(this);
        return mealsCache;
    }

    void refresh() {
        mealsCache = null;
        refreshToday();
        refreshWater();
        refreshWeek();
        refreshQuick();
        refreshList();
    }

    void refreshList() {
        listBox.removeAllViews();
        List<MealLog.Meal> meals = new ArrayList<>(meals());
        // Szűrés a kereső alapján (név vagy bármely összetevő).
        String q = searchEt == null ? "" : Foods.norm(searchEt.getText().toString().trim());
        if (!q.isEmpty()) {
            List<MealLog.Meal> flt = new ArrayList<>();
            for (MealLog.Meal m : meals) {
                boolean hit = Foods.norm(m.name).contains(q);
                if (!hit) for (MealLog.Item it : m.items)
                    if (Foods.norm(it.food).contains(q)) { hit = true; break; }
                if (hit) flt.add(m);
            }
            meals = flt;
        }
        // Mindig időrendben (szerkesztés után se ugorjon a lista elejére a bejegyzés).
        java.util.Collections.sort(meals, (a, b2) -> Long.compare(b2.ts, a.ts));
        if (meals.isEmpty()) {
            if (!q.isEmpty()) {
                listBox.addView(text("Nincs a keresésre illő bejegyzés.", 13.5f, MUTED, false));
            } else {
                listBox.addView(introCard());
            }
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
        // A napló akár ezer bejegyzés is lehet; ennyi kártyát egyszerre kirajzolni
        // megakasztaná a képernyőt. Csak a legfrissebbeket építjük fel, a többit
        // igény szerint, egy gombnyomásra.
        int limit = Math.min(meals.size(), shownLimit);
        for (int i = 0; i < limit; i++) {
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
                            long now = System.currentTimeMillis();
                            MealLog.add(this, new MealLog.Meal(now, m.name, m.items, ""));
                            refresh();
                            Ux.blazeCard(this, "🍽 Újra naplózva ✔  "
                                    + Math.round(m.kcal()) + " kcal"
                                    + (awardDailyLogXp(now) ? "  ·  +5 XP" : ""));
                        });
                sh.addRow("📷", m.photo.isEmpty() ? "Fotó csatolása" : "Új fotó készítése",
                        "A tányérod képe a bejegyzéshez", false, true, () -> capturePhoto(m.ts));
                sh.addRow("🖼", "Fotó a galériából",
                        "Korábban készült kép hozzárendelése", false, true, () -> pickPhoto(m.ts));
                if (!m.photo.isEmpty())
                    sh.addRow("🚫", "Fotó eltávolítása", "A bejegyzés megmarad, csak a kép tűnik el",
                            false, true, () -> {
                                MealLog.updatePhoto(this, m.ts, "");
                                MealLog.cleanupOrphanPhotos(this);
                                refresh();
                                Toast.makeText(this, "Fotó eltávolítva.",
                                        Toast.LENGTH_SHORT).show();
                            });
                sh.addRow("🕒", "Időpont módosítása", "Ha máskor etted, mint amikor beírtad",
                        false, true, () -> editMealTime(m));
                boolean fav = MealLog.isFav(this, m);
                sh.addRow(fav ? "★" : "☆",
                        fav ? "Levétel a kedvencekről" : "Kedvencnek jelöl",
                        fav ? "Nem marad a gyors csipek közt"
                            : "Mindig elöl lesz a gyors naplózásban",
                        false, true, () -> {
                            if (fav) MealLog.removeFav(this, MealLog.favLabel(m));
                            else MealLog.addFav(this, m);
                            refresh();
                            Ux.blazeCard(this, fav ? "☆ Levéve a kedvencekről"
                                    : "★ Kedvenc lett: " + MealLog.favLabel(m));
                        });
                sh.addDestructive("🗑 Törlés", () -> { MealLog.removeByTs(this, m.ts); refresh(); });
                sh.addCancel().show();
            });
            listBox.addView(c, lp());
            listBox.addView(gap(10));
        }
        if (meals.size() > limit) {
            final int remaining = meals.size() - limit;
            Button more = ghost("További " + Math.min(remaining, PAGE) + " bejegyzés  ("
                    + remaining + " van még)");
            more.setTextSize(13.5f);
            more.setOnClickListener(v -> { shownLimit += PAGE; refreshList(); });
            listBox.addView(more);
        }
    }

    /** Első indításkor: rövid bemutató arról, hogyan a legegyszerűbb naplózni. */
    LinearLayout introCard() {
        LinearLayout c = card();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.addView(text("Így a leggyorsabb 🐺", 14.5f, TXT, true));
        c.addView(gap(8));
        c.addView(text("Csak írd le, mit ettél – az app felismeri az ételeket, a "
                + "grammot és a darabszámot is, és kiszámolja a kalóriát:",
                13, MUTED, false));
        c.addView(gap(8));
        String[] examples = {
            "rántott hús rizzsel",
            "150 g csirkemell 200 g rizs",
            // Számjeggyel, mert a darabszámot így ismeri fel („két" szóként nem).
            "2 tojás kenyérrel",
        };
        for (final String ex : examples) {
            TextView t = text("„" + ex + "\"", 13, Theme.accent(this), false);
            t.setPadding(dp(2), dp(4), 0, dp(4));
            t.setClickable(true);
            // Koppintásra rögtön megnyílik az űrlap a példával előtöltve.
            t.setOnClickListener(v -> addMealDialogPrefilled(ex));
            c.addView(t);
        }
        c.addView(gap(6));
        c.addView(text("Ha nincs meg a gramm, a tipikus adaggal számolunk – utólag "
                + "bármikor pontosíthatod. A 📖 Kalóriatáblázatból egy koppintással is "
                + "naplózhatsz.", 12.5f, MUTED, false));
        return c;
    }

    /** Gyors-naplózás: előbb a kedvencek, utána a leggyakoribb étkezések csipjei. */
    void refreshQuick() {
        quickBox.removeAllViews();
        // A kedvencek mindig elöl, kézzel kijelölve.
        java.util.LinkedHashMap<String, MealLog.Meal> picks = new java.util.LinkedHashMap<>();
        java.util.HashSet<String> favLabels = new java.util.HashSet<>();
        for (MealLog.Meal f : MealLog.loadFavs(this)) {
            if (f.items.isEmpty()) continue;
            String lbl = MealLog.favLabel(f);
            picks.put(lbl, f);
            favLabels.add(lbl);
        }
        // Utána a legalább kétszer naplózott, gyakori étkezések (max 5 csip összesen).
        java.util.LinkedHashMap<String, MealLog.Meal> latest = new java.util.LinkedHashMap<>();
        java.util.HashMap<String, Integer> count = new java.util.HashMap<>();
        for (MealLog.Meal m : meals()) {
            if (m.items.isEmpty()) continue;
            String key = m.name.isEmpty() ? m.items.get(0).food : m.name;
            Integer c = count.get(key);
            count.put(key, c == null ? 1 : c + 1);
            if (!latest.containsKey(key)) latest.put(key, m); // a load() legújabb-elöl sorrendű
        }
        List<String> keys = new ArrayList<>(latest.keySet());
        java.util.Collections.sort(keys, (a, b2) -> count.get(b2) - count.get(a));
        for (String key : keys) {
            if (picks.size() >= 5) break;
            if (count.get(key) < 2 || picks.containsKey(key)) continue;
            picks.put(key, latest.get(key));
        }
        if (picks.isEmpty()) return;

        quickBox.addView(gap(10));
        quickBox.addView(text(favLabels.isEmpty()
                ? "Gyakoriak – koppints az újranaplózáshoz"
                : "Kedvencek és gyakoriak – koppints az újranaplózáshoz",
                11.5f, MUTED, false));
        LinearLayout row = hbox();
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(row, new android.widget.FrameLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams rl = lp();
        rl.topMargin = dp(6);
        quickBox.addView(hsv, rl);

        for (java.util.Map.Entry<String, MealLog.Meal> e : picks.entrySet()) {
            final String key = e.getKey();
            final MealLog.Meal src = e.getValue();
            final boolean isFav = favLabels.contains(key);
            TextView chip = text((isFav ? "★ " : "")
                    + (key.length() > 16 ? key.substring(0, 15) + "…" : key)
                    + " · " + Math.round(src.kcal()) + " kcal", 12,
                    isFav ? Theme.accent(this) : TXT, true);
            GradientDrawable cbg = new GradientDrawable();
            cbg.setColor(CARD2);
            cbg.setCornerRadius(dp(18));
            cbg.setStroke(dp(1), isFav ? Theme.accent(this) : LINE);
            chip.setBackground(cbg);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                long now = System.currentTimeMillis();
                MealLog.add(this, new MealLog.Meal(now, src.name, src.items, ""));
                refresh();
                Ux.blazeCard(this, "🍽 Újra naplózva ✔  " + Math.round(src.kcal()) + " kcal"
                        + (awardDailyLogXp(now) ? "  ·  +5 XP" : ""));
            });
            // Kedvencet hosszú nyomással le lehet venni a listáról.
            if (isFav) chip.setOnLongClickListener(v -> {
                MealLog.removeFav(this, key);
                refresh();
                Toast.makeText(this, "☆ Levéve a kedvencekről: " + key,
                        Toast.LENGTH_SHORT).show();
                return true;
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(8);
            row.addView(chip, clp);
        }
    }

    /** A mai összesítő kártya: kcal, és cél esetén haladássáv + maradék. */
    void refreshToday() {
        todayCard.removeAllViews();
        double kcal = 0, protSum = 0;
        long t0 = dayStartMs();
        for (MealLog.Meal m : meals())
            if (m.ts >= t0) { kcal += m.kcal(); protSum += m.protein(); }
        int goal = getSharedPreferences("edzo", MODE_PRIVATE).getInt("kcal_goal", 0);
        int streak = MealLog.logStreak(this);
        todayCard.addView(text("🍽 Ma összesen" + (goal > 0 ? "  ·  cél: " + goal + " kcal" : "")
                + (streak >= 2 ? "  ·  🔥 " + streak + " napja naplózol" : ""),
                12.5f, MUTED, true));
        todayCard.addView(text(Math.round(kcal) + " kcal", 26, Theme.accent(this), true));
        double prot = protSum;
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
                pgd.setColor(Theme.track(this));
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
            bgd.setColor(Theme.track(this));
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

    /** Vízbevitel-kártya: pohár (2,5 dl) hozzáadása/levonása, cél haladássávval. */
    void refreshWater() {
        waterCard.removeAllViews();
        int cl = Water.todayCl(this);
        final int goalCl = Water.goalCl(this);
        boolean done = cl >= goalCl;
        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text("💧 Víz ma: " + Water.liters(cl) + " / " + Water.liters(goalCl)
                + (done ? "  ✔" : ""), 13.5f, done ? Theme.accent(this) : TXT, true);
        label.setClickable(true);
        label.setOnClickListener(v -> waterGoalDialog());
        top.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView minus = text("−", 20, MUTED, true);
        minus.setPadding(dp(12), dp(2), dp(12), dp(2));
        minus.setClickable(true);
        minus.setOnClickListener(v -> {
            Water.addCl(this, -Water.GLASS_CL);
            refreshWater();
        });
        top.addView(minus);
        TextView plus = text("＋ pohár", 14, Theme.accent(this), true);
        plus.setPadding(dp(12), dp(2), dp(2), dp(2));
        plus.setClickable(true);
        plus.setOnClickListener(v -> {
            int before = Water.todayCl(this);
            int after = Water.addCl(this, Water.GLASS_CL);
            refreshWater();
            if (before < goalCl && after >= goalCl)
                Ux.blazeCard(this, "💧 Napi vízcél megvan – szép munka!");
        });
        top.addView(plus);
        waterCard.addView(top, lp());
        LinearLayout barBg = hbox();
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(Theme.track(this));
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
        et.setText(String.valueOf(Water.goalCl(this) / 10));
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        box.addView(et, lp());
        Sheet sh = new Sheet(this, "Napi vízcél 💧", "Egy pohár = 2,5 dl")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    int dl = (int) parse(et.getText().toString()); // dl-ben kérjük
                    Water.setGoalCl(this, dl * 10);
                    refreshWater();
                });
        // A kcal-célhoz hasonlóan itt is legyen egy kiindulási pont, ha a
        // Profilban már van testsúly (kb. 35 ml/testsúlykg).
        double w = Profile.lastWeight(this);
        if (w > 0) {
            final int sug = Water.suggestedGoalCl(w);
            sh.addNeutral("⚡ Testsúly alapján: " + Water.liters(sug), () -> {
                Water.setGoalCl(this, sug);
                refreshWater();
            });
        }
        sh.addCancel().show();
    }

    // Heti nézet mértéke: false = kcal, true = fehérje (fejlécre koppintva vált).
    boolean weekProtein;

    /** Az elmúlt 7 nap napi kcal- vagy fehérje-összegei vízszintes sávokkal. */
    void refreshWeek() {
        weekCard.removeAllViews();
        long dayMs = 24L * 3600 * 1000;
        long today0 = dayStartMs();
        double[] sums = new double[7]; // [0]=ma, [6]=6 napja
        for (MealLog.Meal m : meals()) {
            long diff = today0 - dayStartOf(m.ts);
            int k = (int) (diff / dayMs);
            if (k >= 0 && k < 7) sums[k] += weekProtein ? m.protein() : m.kcal();
        }
        double max = 1;
        for (double s : sums) max = Math.max(max, s);
        int goal = getSharedPreferences("edzo", MODE_PRIVATE)
                .getInt(weekProtein ? "protein_goal" : "kcal_goal", 0);
        if (goal > 0) max = Math.max(max, goal);
        double weekSum = 0; int daysWith = 0;
        for (double sVal : sums) { weekSum += sVal; if (sVal > 0) daysWith++; }
        String unit = weekProtein ? " g fehérje/nap" : " kcal/nap";
        TextView head = text("Elmúlt 7 nap · " + (weekProtein ? "🥩 fehérje" : "🔥 kcal")
                + (daysWith > 0 ? "  ·  átlag " + Math.round(weekSum / daysWith) + unit : "")
                + "  ⇄", 12.5f, MUTED, true);
        head.setClickable(true);
        head.setOnClickListener(v -> { weekProtein = !weekProtein; refreshWeek(); });
        weekCard.addView(head);
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
            bgd.setColor(Theme.track(this));
            bgd.setCornerRadius(dp(5));
            barBg.setBackground(bgd);
            View fill = new View(this);
            GradientDrawable fgd = new GradientDrawable();
            boolean over = goal > 0 && sums[k] > goal;
            // Fehérjénél a cél túllépése jó (zöld), kalóriánál figyelmeztető (borostyán).
            fgd.setColor(sums[k] <= 0 ? 0x00000000
                    : over ? (weekProtein ? 0xFF22C55E : 0xFFF59E0B) : Theme.accent(this));
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
            // Ha a Profilban van fogyási cél, a hozzá tartozó bevitelt is
            // felkínáljuk – ugyanazzal a képlettel, amit a Profil is mutat.
            float loss = Profile.getGoalLoss(this);
            if (loss > 0) {
                int ri = Math.max(0, Math.min(3, Profile.getGoalRate(this)));
                double deficit = Profile.RATES[ri] * 7700.0 / 7.0;   // kcal/nap
                final int cut = (int) Math.round(bmr * 1.4 - deficit);
                if (cut > 800) sh.addNeutral("🎯 Fogyási célod alapján: ~" + cut + " kcal", () -> {
                    getSharedPreferences("edzo", MODE_PRIVATE).edit()
                            .putInt("kcal_goal", cut).apply();
                    refresh();
                });
            }
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

    /**
     * A leggyakrabban naplózott összetevők neve (legfeljebb 8), gyakoriság
     * szerint. Ha még alig van napló, a saját ételek is bekerülnek, hogy a
     * csipsor ne maradjon üresen azoknál, akik most vették fel őket.
     */
    List<String> frequentFoodNames() {
        java.util.HashMap<String, Integer> count = new java.util.HashMap<>();
        for (MealLog.Meal m : meals())
            for (MealLog.Item it : m.items) {
                if (it.food == null || it.food.isEmpty()) continue;
                Integer c = count.get(it.food);
                count.put(it.food, c == null ? 1 : c + 1);
            }
        List<String> names = new ArrayList<>(count.keySet());
        java.util.Collections.sort(names, (a, b) -> count.get(b) - count.get(a));
        for (Foods.Food cf : Foods.custom(this))
            if (!names.contains(cf.name)) names.add(cf.name);
        return names.size() > 8 ? new ArrayList<>(names.subList(0, 8)) : names;
    }

    /** Új étkezés a névmező előtöltésével (a bemutató példáihoz). */
    void addMealDialogPrefilled(String name) {
        prefillName = name;
        addMealDialog(null, -1);
    }

    private String prefillName;

    /** Új étkezés felvitele, vagy meglévő szerkesztése (existing != null esetén). */
    void addMealDialog(final MealLog.Meal existing, final int editIdx) {
        final LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);

        final EditText nameEt = input("Mit ettél? (pl. 150 g csirkemell rizzsel, 2 tojás)");
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
                reco.setOnClickListener(null);
                reco.setClickable(false);
                if (q.length() < 3) { reco.setText(""); return; }
                List<Foods.Hit> g = Foods.parse(DietActivity.this, q);
                if (g.isEmpty()) {
                    reco.setText("🔍 Ezt még nem ismerem – koppints ide, és vedd fel saját ételként!");
                    reco.setClickable(true);
                    reco.setOnClickListener(v -> addCustomFoodSheet(q));
                    return;
                }
                StringBuilder sb = new StringBuilder("✔ Felismerve: ");
                for (int i = 0; i < g.size(); i++) {
                    if (i > 0) sb.append(" + ");
                    sb.append(g.get(i).food.name);
                    if (g.get(i).grams > 0)
                        sb.append(" ").append(Math.round(g.get(i).grams)).append(" g");
                }
                reco.setText(sb.toString());
            }
        });
        // A kezdőérték a figyelő felrakása UTÁN kerül be, hogy a felismerés
        // rögtön látszódjon szerkesztésnél és a bemutató példáinál is.
        if (existing != null) nameEt.setText(existing.name);
        else if (prefillName != null) { nameEt.setText(prefillName); prefillName = null; }
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

        // Gyors ételnevek: a leggyakrabban naplózott összetevők csipjei.
        List<String> often = frequentFoodNames();
        if (!often.isEmpty()) {
            LinearLayout chipRow = hbox();
            for (final String fn : often) {
                TextView chip = text(fn, 12, TXT, false);
                GradientDrawable cbg = new GradientDrawable();
                cbg.setColor(CARD2);
                cbg.setCornerRadius(dp(16));
                cbg.setStroke(dp(1), LINE);
                chip.setBackground(cbg);
                chip.setPadding(dp(11), dp(6), dp(11), dp(6));
                chip.setClickable(true);
                // Az első üres összetevő-sorba írja be; ha nincs, nyit egy újat.
                chip.setOnClickListener(v -> {
                    for (EditText[] r : rows)
                        if (r[0].getText().toString().trim().isEmpty()) {
                            r[0].setText(fn);
                            return;
                        }
                    addItemRow(itemsBox, rows);
                    rows.get(rows.size() - 1)[0].setText(fn);
                });
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
                clp.rightMargin = dp(6);
                chipRow.addView(chip, clp);
            }
            android.widget.HorizontalScrollView hsv =
                    new android.widget.HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            hsv.addView(chipRow, new android.widget.FrameLayout.LayoutParams(-2, -2));
            LinearLayout.LayoutParams hlp = lp();
            hlp.topMargin = dp(8);
            box.addView(hsv, hlp);
        }

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
        // Az összetevő-soroknál a beírt szöveget kell feloldani; a névből
        // felismert ételeket viszont már ismerjük, azokat nem keressük újra.
        List<Foods.Food> resolved = new ArrayList<>();
        for (EditText[] r : rows) {
            String f = r[0].getText().toString().trim();
            if (f.isEmpty()) continue;
            foods.add(f);
            grams.add(parse(r[1].getText().toString()));
            resolved.add(Foods.find(this, f));
        }
        if (foods.isEmpty()) {
            // Okos bevitel: ha csak a nevet írtad be („rántott hús rizzsel",
            // „150 g csirkemell 200 g rizs", „2 tojás"), az összetevőket – és ha
            // ott a mennyiség, azt is – a névből ismerjük fel.
            for (Foods.Hit h : Foods.parse(this, nameEt.getText().toString())) {
                foods.add(h.food.name);
                grams.add(h.grams);
                resolved.add(h.food);
            }
        }
        if (foods.isEmpty()) {
            Toast.makeText(this, "Adj meg legalább egy összetevőt, vagy írd a névbe, "
                    + "mit ettél (pl. rántott hús rizzsel).", Toast.LENGTH_LONG).show();
            return;
        }

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
        if (awardDailyLogXp(ts)) msg += "  ·  +5 XP";
        Ux.blazeCard(this, "🍽 " + msg);
    }

    /**
     * A napi első étkezés naplózásáért +5 XP – naponta legfeljebb egyszer, és
     * csak mai bejegyzésre (visszamenőleges pótlásért nem jár). True, ha most
     * írtuk jóvá.
     */
    boolean awardDailyLogXp(long ts) {
        if (ts < dayStartMs()) return false;
        android.content.SharedPreferences p = getSharedPreferences("edzo", MODE_PRIVATE);
        int today = Water.dayNumber(Calendar.getInstance());
        if (p.getInt("meal_xp_day", 0) == today) return false;
        p.edit().putInt("meal_xp_day", today).apply();
        Levels.addBonus(this, 5);
        return true;
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
        // Az össz-gramm itt is javítható: a fotót nézve gyakran az derül ki, hogy
        // nemcsak az arány, hanem a becsült teljes adag is mellément.
        final double[] totalHolder = {total};
        final EditText totalEt = input("Teljes adag (g)");
        totalEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        totalEt.setText(String.valueOf(Math.round(total)));
        box.addView(totalEt, lp());
        box.addView(gap(8));

        final TextView totalTv = text("", 13.5f, Theme.accent(this), true);
        final TextView[] labels = new TextView[n];
        final Runnable update = () -> {
            double sum = 0;
            for (double sv2 : shares) sum += sv2;
            double kcal = 0;
            for (int i = 0; i < n; i++) {
                double g = shares[i] / sum * totalHolder[0];
                MealLog.Item it = m.items.get(i);
                double kpg = it.grams > 0 ? it.kcal / it.grams : 1.5;
                kcal += kpg * g;
                labels[i].setText(it.food + "  ·  " + Math.round(g) + " g");
            }
            totalTv.setText("Összesen: " + Math.round(totalHolder[0])
                    + " g  ·  ~" + Math.round(kcal) + " kcal");
        };
        totalEt.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                double v = parse(s.toString());
                totalHolder[0] = v > 0 ? v : total;   // üres mező: marad az eredeti
                update.run();
            }
        });
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
                "A fotó alapján állítsd az arányokat – és ha kell, a teljes adagot is.")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    double sum = 0;
                    for (double sv2 : shares) sum += sv2;
                    List<MealLog.Item> ni = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        MealLog.Item it = m.items.get(i);
                        double g = shares[i] / sum * totalHolder[0];
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

    /** Kép választása a galériából (rendszer-választóval, engedély nem kell). */
    void pickPhoto(long ts) {
        pendingPhotoTs = ts;
        try {
            android.content.Intent i =
                    new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(android.content.Intent.createChooser(i,
                    "Fotó választása"), REQ_PICK);
        } catch (Exception e) {
            Toast.makeText(this, "Nem található galéria-alkalmazás.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || pendingPhotoTs <= 0) return;
        try {
            android.graphics.Bitmap bm = null;
            if (req == REQ_PHOTO) {
                bm = (android.graphics.Bitmap) data.getParcelableExtra("data");
            } else if (req == REQ_PICK && data.getData() != null) {
                // Két menetben: először a méretet olvassuk, majd lekicsinyítve töltjük be.
                android.graphics.BitmapFactory.Options o =
                        new android.graphics.BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                java.io.InputStream in = getContentResolver().openInputStream(data.getData());
                android.graphics.BitmapFactory.decodeStream(in, null, o);
                if (in != null) in.close();
                int sample = 1;
                while (o.outWidth / sample > 1280 || o.outHeight / sample > 1280) sample *= 2;
                android.graphics.BitmapFactory.Options o2 =
                        new android.graphics.BitmapFactory.Options();
                o2.inSampleSize = sample;
                in = getContentResolver().openInputStream(data.getData());
                bm = android.graphics.BitmapFactory.decodeStream(in, null, o2);
                if (in != null) in.close();
            }
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
        List<MealLog.Meal> meals = new ArrayList<>(meals());
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
        final String dayLabel = hf.format(new Date(day0));
        final double kS = kSum, pS = pSum;
        // Az adott nap vize (ha akkor ment a számláló).
        int dayCl = Water.clOn(this, day0);
        new Sheet(this, dayLabel,
                Math.round(kSum) + " kcal"
                + (pSum > 0 ? " · " + Math.round(pSum) + " g fehérje" : "")
                + (dayCl > 0 ? " · 💧 " + Water.liters(dayCl) : ""))
                .addCustom(box)
                .addRow("📤", "Megosztás", "A nap étrendje szövegként",
                        false, true, () -> shareDay(day0, dayLabel, kS, pS))
                .addCancel()
                .show();
    }

    /** Egy nap étrendjének megosztása egyszerű szövegként. */
    void shareDay(long day0, String dayLabel, double kSum, double pSum) {
        long dayMs = 24L * 3600 * 1000;
        List<MealLog.Meal> meals = new ArrayList<>(meals());
        java.util.Collections.sort(meals, (a, b2) -> Long.compare(a.ts, b2.ts));
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", new Locale("hu"));
        StringBuilder sb = new StringBuilder("🍽 Étrendem – ").append(dayLabel).append("\n");
        for (MealLog.Meal m : meals) {
            if (m.ts < day0 || m.ts >= day0 + dayMs) continue;
            String title = m.name.isEmpty()
                    ? (m.items.isEmpty() ? mealSlot(m.ts)
                        : mealSlot(m.ts) + " · " + m.items.get(0).food)
                    : m.name;
            sb.append(tf.format(new Date(m.ts))).append("  ").append(title)
              .append(" – ").append(Math.round(m.kcal())).append(" kcal\n");
        }
        sb.append("Összesen: ").append(Math.round(kSum)).append(" kcal");
        if (pSum > 0) sb.append(" · ").append(Math.round(pSum)).append(" g fehérje");
        sb.append("\n\n🐺 GRIT – a falka veled van!");
        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
        try {
            startActivity(android.content.Intent.createChooser(i, "Étrend megosztása"));
        } catch (Exception ignored) {}
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
        final java.util.HashSet<String> customNames = new java.util.HashSet<>();
        final Runnable render = () -> {
            listV.removeAllViews();
            customNames.clear();
            for (Foods.Food cf : Foods.custom(this)) customNames.add(cf.name);
            String q = Foods.norm(search.getText().toString().trim());
            int shown = 0;
            for (Foods.Food f : Foods.all(this)) {
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
                final boolean isCustom = customNames.contains(f.name);
                final String fn = f.name;
                final Foods.Food food = f;
                LinearLayout row = hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(text((isCustom ? "🖊 " : "") + f.name, 13.5f, TXT, isCustom),
                        new LinearLayout.LayoutParams(0, -2, 1f));
                int piece = Foods.pieceGrams(f);
                row.addView(text(f.kcal100 + " kcal · " + (Math.round(f.prot100 * 10) / 10.0)
                        + "g P /100g" + (piece > 0 ? "  ·  1 db ≈ " + piece + " g" : ""),
                        12, MUTED, false));
                row.setClickable(true);
                // Koppintásra rögtön naplózható, a tipikus adaggal előtöltve.
                row.setOnClickListener(v -> logFoodSheet(food));
                // Saját étel hosszú nyomásra törölhető.
                if (isCustom) row.setOnLongClickListener(v -> {
                    Foods.removeCustom(this, fn);
                    Toast.makeText(this, "Saját étel törölve: " + fn,
                            Toast.LENGTH_SHORT).show();
                    return true;
                });
                LinearLayout.LayoutParams rl = lp();
                rl.topMargin = dp(7);
                listV.addView(row, rl);
                shown++;
            }
            if (shown == 0)
                listV.addView(text("Nincs találat – vedd fel saját ételként lent!",
                        12, MUTED, false));
        };
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { render.run(); }
        });
        render.run();
        new Sheet(this, "Kalóriatáblázat 📖",
                "Koppints egy ételre a gyors naplózáshoz · a 🖊 sajátok hosszú nyomásra törölhetők")
                .addCustom(box)
                .addRow("＋", "Saját étel felvétele", "Név, kcal és fehérje 100 grammonként",
                        false, true, this::addCustomFoodSheet)
                .addCancel()
                .show();
    }

    /** Egy étel gyors naplózása a kalóriatáblázatból, a tipikus adaggal előtöltve. */
    void logFoodSheet(final Foods.Food f) {
        final LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        final EditText gEt = input("Mennyiség (g)");
        gEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        gEt.setText(String.valueOf(f.portion));
        box.addView(gEt, lp());
        final TextView preview = text("", 12.5f, MUTED, false);
        preview.setPadding(dp(2), dp(8), 0, 0);
        box.addView(preview);
        final Runnable calc = () -> {
            double g = parse(gEt.getText().toString());
            if (g <= 0) { preview.setText(""); return; }
            preview.setText("≈ " + Math.round(f.kcal100 * g / 100.0) + " kcal"
                    + (f.prot100 > 0
                        ? "  ·  " + Math.round(f.prot100 * g / 100.0) + " g fehérje" : ""));
        };
        gEt.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { calc.run(); }
        });
        calc.run();
        new Sheet(this, f.name, f.kcal100 + " kcal / 100 g · tipikus adag " + f.portion + " g")
                .addCustom(box)
                .addPrimary("Naplózás most", () -> {
                    double g = parse(gEt.getText().toString());
                    if (g <= 0) {
                        Toast.makeText(this, "Adj meg egy mennyiséget grammban.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long now = System.currentTimeMillis();
                    List<MealLog.Item> items = new ArrayList<>();
                    items.add(new MealLog.Item(f.name, g, f.kcal100 * g / 100.0,
                            f.prot100 * g / 100.0));
                    MealLog.Meal meal = new MealLog.Meal(now, "", items, "");
                    MealLog.add(this, meal);
                    refresh();
                    Ux.blazeCard(this, "🍽 Naplózva ✔  " + Math.round(meal.kcal()) + " kcal"
                            + (awardDailyLogXp(now) ? "  ·  +5 XP" : ""));
                })
                .addCancel()
                .show();
    }

    /** Saját étel felvétele: a felismerés és a kalóriatáblázat is használja. */
    void addCustomFoodSheet() { addCustomFoodSheet(""); }

    void addCustomFoodSheet(String presetName) {
        final LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        final EditText nameEt = input("Név (pl. Nagyi rakott zöldsége)");
        if (presetName != null && !presetName.isEmpty()) nameEt.setText(presetName);
        box.addView(nameEt, lp());
        final EditText kcalEt = input("kcal / 100 g");
        kcalEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams l1 = lp(); l1.topMargin = dp(8);
        box.addView(kcalEt, l1);
        final EditText protEt = input("fehérje g / 100 g (nem kötelező)");
        protEt.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams l2 = lp(); l2.topMargin = dp(8);
        box.addView(protEt, l2);
        final EditText portEt = input("tipikus adag g (nem kötelező, alap 100)");
        portEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams l3 = lp(); l3.topMargin = dp(8);
        box.addView(portEt, l3);
        new Sheet(this, "Saját étel 🖊",
                "A beírt nevet a felismerés is megtalálja ezután.")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    String n = nameEt.getText().toString().trim();
                    int k = (int) parse(kcalEt.getText().toString());
                    if (n.isEmpty() || k <= 0) {
                        Toast.makeText(this, "Név és kcal/100g kötelező.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double pr = parse(protEt.getText().toString());
                    int po = (int) parse(portEt.getText().toString());
                    Foods.addCustom(this, n, k, Math.max(0, pr), po > 0 ? po : 100);
                    Ux.blazeCard(this, "🖊 Saját étel mentve: " + n);
                })
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
