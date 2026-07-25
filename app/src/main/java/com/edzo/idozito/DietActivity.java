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
    LinearLayout todayCard, weekCard;
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

        // Elmúlt 7 nap kcal-sávjai.
        weekCard = card();
        weekCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        col.addView(weekCard, lp());
        col.addView(gap(14));

        Button add = primary("＋  Étkezés hozzáadása");
        add.setOnClickListener(v -> addMealDialog());
        col.addView(add);
        col.addView(gap(18));

        col.addView(text("Étkezések", 15.5f, TXT, true));
        col.addView(gap(10));
        listBox = vbox();
        col.addView(listBox, lp());

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_reminders", 3));
        col.post(() -> Ux.enterChildren(col, 30, 45));
        refresh();
    }

    void refresh() {
        refreshToday();
        refreshWeek();
        listBox.removeAllViews();
        List<MealLog.Meal> meals = MealLog.load(this);
        if (meals.isEmpty()) {
            listBox.addView(text("Még nincs bejegyzés. Add hozzá az első étkezésed fent!",
                    13.5f, MUTED, false));
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("MMM d. · HH:mm", new Locale("hu"));
        long dayStart = dayStartMs();
        boolean wroteEarlier = false;
        for (int i = 0; i < meals.size(); i++) {
            final MealLog.Meal m = meals.get(i);
            final int idx = i;
            if (!wroteEarlier && m.ts < dayStart) {
                TextView sep = text("Korábbi napok", 12.5f, MUTED, true);
                sep.setPadding(dp(2), dp(8), 0, dp(8));
                listBox.addView(sep);
                wroteEarlier = true;
            }
            LinearLayout c = card();
            c.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout top = hbox();
            top.setGravity(Gravity.CENTER_VERTICAL);
            String title = m.name.isEmpty()
                    ? (m.items.isEmpty() ? "Étkezés" : m.items.get(0).food) : m.name;
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
            top.addView(text(Math.round(m.kcal()) + " kcal", 15, Theme.accent(this), true));
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
                        Math.round(m.kcal()) + " kcal · " + Math.round(m.grams()) + " g");
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
                sh.addRow("🔁", "Újra most", "Ugyanez az étkezés naplózása mostani időponttal",
                        false, true, () -> {
                            MealLog.add(this, new MealLog.Meal(System.currentTimeMillis(),
                                    m.name, m.items, ""));
                            refresh();
                            Toast.makeText(this, "Újra naplózva ✔  "
                                    + Math.round(m.kcal()) + " kcal", Toast.LENGTH_SHORT).show();
                        });
                sh.addRow("📷", m.photo.isEmpty() ? "Fotó csatolása" : "Új fotó készítése",
                        "A tányérod képe a bejegyzéshez", false, true, () -> capturePhoto(m.ts));
                sh.addDestructive("🗑 Törlés", () -> { MealLog.removeAt(this, idx); refresh(); });
                sh.addCancel().show();
            });
            listBox.addView(c, lp());
            listBox.addView(gap(10));
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
            TextView hint = text("Koppints ide napi kalória-cél beállításához", 11.5f, MUTED, false);
            hint.setPadding(0, dp(4), 0, 0);
            todayCard.addView(hint);
        }
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
        weekCard.addView(text("Elmúlt 7 nap", 12.5f, MUTED, true));
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
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        box.addView(et, lp());
        new Sheet(this, "Napi kalória-cél 🎯",
                "Tipp: a Profil oldalon a BMR-ed jó kiindulási alap.")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    int g = (int) parse(et.getText().toString());
                    getSharedPreferences("edzo", MODE_PRIVATE).edit()
                            .putInt("kcal_goal", Math.max(0, g)).apply();
                    refresh();
                })
                .addCancel()
                .show();
    }

    // ---------- Új étkezés ----------

    void addMealDialog() {
        final LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);

        final EditText nameEt = input("Étkezés neve (pl. Rántott hús rizzsel)");
        box.addView(nameEt, lp());
        box.addView(gap(10));

        box.addView(text("Összetevők (étel + gramm)", 12.5f, MUTED, true));
        final LinearLayout itemsBox = vbox();
        final List<EditText[]> rows = new ArrayList<>();
        addItemRow(itemsBox, rows);
        addItemRow(itemsBox, rows);
        box.addView(itemsBox, lp());

        Button more = ghost("＋  Összetevő");
        more.setTextSize(13.5f);
        more.setOnClickListener(v -> addItemRow(itemsBox, rows));
        box.addView(more);
        box.addView(gap(10));

        box.addView(text("Vagy add meg a teljes adag grammját – az üresen hagyott "
                + "összetevők közt arányosan elosztjuk:", 12, MUTED, false));
        final EditText totalEt = input("Teljes adag (g), nem kötelező");
        totalEt.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(totalEt, lp());

        new Sheet(this, "Új étkezés 🍽")
                .addCustom(box)
                .addPrimary("Mentés", () -> saveMeal(nameEt, rows, totalEt))
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

    void saveMeal(EditText nameEt, List<EditText[]> rows, EditText totalEt) {
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
        // Közös gramm szétosztása a megadatlan összetevők közt (arányosan).
        double total = parse(totalEt.getText().toString());
        double given = 0; int missing = 0;
        for (double g : grams) { if (g > 0) given += g; else missing++; }
        if (missing > 0) {
            double remain = total > given ? total - given : 0;
            double each = remain > 0 ? remain / missing : 150; // ésszerű alap: 150 g
            for (int i = 0; i < grams.size(); i++) if (grams.get(i) <= 0) grams.set(i, each);
        }

        List<MealLog.Item> items = new ArrayList<>();
        boolean estimated = false;
        for (int i = 0; i < foods.size(); i++) {
            Foods.Food f = Foods.find(foods.get(i));
            int kcal100;
            String label;
            if (f != null) { kcal100 = f.kcal100; label = f.name; }
            else { kcal100 = 150; label = foods.get(i); estimated = true; } // becslés
            double g = grams.get(i);
            items.add(new MealLog.Item(label, g, kcal100 * g / 100.0));
        }
        MealLog.Meal meal = new MealLog.Meal(System.currentTimeMillis(),
                nameEt.getText().toString().trim(), items, "");
        MealLog.add(this, meal);
        refresh();
        String msg = "Mentve ✔  " + Math.round(meal.kcal()) + " kcal";
        if (estimated) msg += "  (ismeretlen ételnél ~becslés)";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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
