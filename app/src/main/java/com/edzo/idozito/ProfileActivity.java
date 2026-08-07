package com.edzo.idozito;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Testadatok (magasság, testsúly, kor, testzsír), élő BMI, és a mentett mérések
 * változását mutató diagram.
 */
public class ProfileActivity extends Activity {

    static int BG, CARD, CARD2, TXT, MUTED, LINE;
    static final int ACCENT = MainActivity.ACCENT, INDIGO = MainActivity.INDIGO, VIOLET = MainActivity.VIOLET;
    static final int WEIGHT_C = 0xFF34D399, BMI_C = 0xFFE11D2E, FAT_C = 0xFFF59E0B,
            WAIST_C = 0xFF60A5FA;

    EditText heightEt, weightEt, bodyFatEt, byEt, bmEt, bdEt, goalLossEt;
    TextView bmiValue, bmiCat, ageLabel, bmrValue, goalInfo;
    Button[] sexBtns = new Button[2];
    Button[] rateBtns = new Button[4];
    ChartView chart;
    TextView chartInfo;
    LinearLayout measList;
    Button[] seriesBtns = new Button[4];
    int series = 0; // 0 testsúly, 1 BMI, 2 testzsír, 3 derék

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); BG=MainActivity.BG; CARD=MainActivity.CARD; CARD2=MainActivity.CARD2; TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; LINE=MainActivity.LINE;
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Profil / BMI", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Add meg az adataidat, és kövesd a változást.", 13, MUTED, false));
        col.addView(gap(20));

        // --- Adatok kártya ---
        LinearLayout card = card();
        addSexRow(card);
        card.addView(divider());
        heightEt = numField(card, "Magasság", "cm", false);
        card.addView(divider());
        addBirthRow(card);
        card.addView(divider());
        weightEt = numField(card, "Testsúly", "kg", true);
        card.addView(divider());
        bodyFatEt = numField(card, "Testzsír", "%", true);
        col.addView(card, lp());
        col.addView(gap(18));

        // --- BMI kártya ---
        LinearLayout bmiCard = card();
        LinearLayout bmiRow = hbox();
        bmiRow.setGravity(Gravity.CENTER_VERTICAL);
        bmiRow.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout bmiLeft = vbox();
        bmiLeft.addView(text("BMI (testtömegindex)", 14, MUTED, false));
        bmiValue = text("—", 34, TXT, true);
        bmiCat = text("", 14, Theme.accent(this), true);
        bmiLeft.addView(bmiValue);
        bmiLeft.addView(bmiCat);
        bmiRow.addView(bmiLeft, new LinearLayout.LayoutParams(0, -2, 1f));
        bmiCard.addView(bmiRow);
        col.addView(bmiCard, lp());
        col.addView(gap(16));

        // --- BMR kártya ---
        LinearLayout bmrCard = card();
        LinearLayout bmrRow = hbox();
        bmrRow.setGravity(Gravity.CENTER_VERTICAL);
        bmrRow.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout bmrLeft = vbox();
        bmrLeft.addView(text("Alap-anyagcsere (BMR)", 14, MUTED, false));
        bmrValue = text("—", 30, TXT, true);
        bmrLeft.addView(bmrValue);
        bmrLeft.addView(text("kcal / nap nyugalomban", 12.5f, MUTED, false));
        bmrRow.addView(bmrLeft, new LinearLayout.LayoutParams(0, -2, 1f));
        bmrCard.addView(bmrRow);
        col.addView(bmrCard, lp());
        col.addView(gap(18));

        // --- Fogyási cél ---
        col.addView(text("Fogyási cél", 17, TXT, true));
        col.addView(gap(10));
        LinearLayout goalCard = card();
        goalLossEt = numField(goalCard, "Cél: fogyás", "kg", true);
        goalCard.addView(divider());
        LinearLayout rateWrap = vbox();
        rateWrap.setPadding(dp(18), dp(12), dp(18), dp(14));
        rateWrap.addView(text("Tempó", 14, MUTED, false));
        rateWrap.addView(gap(8));
        LinearLayout rateRow = hbox();
        String[] rl = {"Lassú", "Normál", "Gyors", "Extrém"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            Button rb = chip(rl[i], Profile.getGoalRate(this) == i);
            rb.setOnClickListener(v -> {
                Profile.setGoalRate(this, idx);
                for (int j = 0; j < 4; j++) styleChip(rateBtns[j], j == idx);
                recompute();
            });
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, -2, 1f);
            rlp.leftMargin = dp(3); rlp.rightMargin = dp(3);
            rateRow.addView(rb, rlp);
            rateBtns[i] = rb;
        }
        rateWrap.addView(rateRow);
        goalCard.addView(rateWrap);
        goalCard.addView(divider());
        goalInfo = text("", 13.5f, TXT, false);
        goalInfo.setPadding(dp(18), dp(12), dp(18), dp(14));
        goalCard.addView(goalInfo);
        col.addView(goalCard, lp());
        col.addView(gap(20));

        Button save = primary("💾  Mérés mentése");
        save.setOnClickListener(v -> saveMeasurement());
        col.addView(save);
        col.addView(gap(8));
        // Aki reggel leolvassa a mérleget, egy mondatot ír, nem két mezőt tölt ki.
        Button bySentence = ghost("✍️  Mérés mondatból");
        bySentence.setOnClickListener(v -> measurementInputSheet());
        col.addView(bySentence);
        col.addView(gap(8));
        // A mérőszalag külön lapon: öt mező a főképernyőn elnyomná a mérleget,
        // pedig a körfogatot a legtöbben csak havonta veszik elő.
        Button tape = ghost("📏  Körfogatok");
        tape.setOnClickListener(v -> circumferenceSheet());
        col.addView(tape);
        col.addView(gap(24));

        // --- Diagram ---
        col.addView(text("Változás az idő során", 17, TXT, true));
        col.addView(gap(12));
        LinearLayout sel = hbox();
        sel.addView(seriesBtn(0, "Testsúly"), selLp());
        sel.addView(seriesBtn(1, "BMI"), selLp());
        sel.addView(seriesBtn(2, "Testzsír"), selLp());
        // A derék a negyedik görbe: fogyásnál ez változik előbb, mint a
        // mérleg száma, és a mérőszalag adatai eddig csak listában látszottak.
        sel.addView(seriesBtn(3, "Derék"), selLp());
        col.addView(sel, lp());
        col.addView(gap(12));

        LinearLayout chartCard = card();
        chartCard.setPadding(dp(8), dp(12), dp(12), dp(10));
        chart = new ChartView(this);
        chartCard.addView(chart, new LinearLayout.LayoutParams(-1, dp(200)));
        chartInfo = text("", 12.5f, MUTED, false);
        chartInfo.setGravity(Gravity.CENTER);
        chartInfo.setPadding(dp(8), dp(8), dp(8), dp(4));
        chartCard.addView(chartInfo);
        col.addView(chartCard, lp());
        col.addView(gap(14));

        // Mentett mérések listája: egyetlen elgépelt adat is javítható, nem
        // kell hozzá az egész görbét feláldozni.
        measList = vbox();
        col.addView(measList, lp());
        refreshMeasList();
        col.addView(gap(14));

        Button clear = ghost("Mérések törlése");
        clear.setOnClickListener(v -> new Sheet(this, "Mérések törlése", "Törlöd az összes mentett mérést?")
                .addDestructive("Törlés", () -> {
                    Profile.clearMeasurements(this); refreshChart(); refreshMeasList(); })
                .addCancel().show());
        col.addView(clear);

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_profile", 4));
        col.post(() -> Ux.enterChildren(col, 30, 45));

        loadValues();
        recompute();
        selectSeries(0);
        // Máshonnan ideirányított mérés-mondat („ma reggel 78,4 kg”): a
        // számokat beírjuk a mezőkbe, a mentést viszont a felhasználó nyomja
        // meg – a saját súlya nem kerülhet a naplóba az ő tudta nélkül.
        handleSentenceIntent(getIntent());
    }

    /**
     * Beviteli lap a mérés-mondathoz, élő előnézettel.
     *
     * A számokat csak akkor írjuk a mezőkbe, ha a felhasználó tovább is lép:
     * gépelés közben egy félkész szám („7”) nem törölheti felül azt, ami a
     * mezőben már ott van.
     */
    void measurementInputSheet() {
        // Egy korábbi, mégsem mentett mondat körfogatai ne ragadjanak be:
        // a következő kézi mentés némán hozzájuk csapódna.
        pendingCm = null;
        final LinearLayout box = vbox();
        box.setPadding(dp(10), dp(6), dp(10), 0);
        final EditText et = new EditText(this);
        et.setHint(Examples.hint(Examples.BODY, System.currentTimeMillis()));
        et.setHintTextColor(MUTED);
        et.setTextColor(TXT);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(et, lp());
        final TextView reco = text("", 12.5f, MUTED, false);
        reco.setPadding(0, dp(8), 0, 0);
        box.addView(reco, lp());
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable e) {
                BodyParse.Body b = BodyParse.parse(e.toString());
                reco.setText(b.isEmpty()
                        ? "Írd le a mérleg számát – „78,4 kg” vagy „78 kiló vagyok”."
                        : "✔ Felismerve:  " + b.label());
            }
        });
        new Sheet(this, "Mérés mondatból ✍️",
                "Ahogy a mérlegről leolvastad. A mentést te nyomod meg.")
                .addCustom(box)
                .addPrimary("Tovább", () -> measurementSheet(et.getText().toString()))
                .addCancel()
                .show();
    }

    /**
     * Mérés mondatból: előnézet, majd egy koppintással mentés.
     *
     * A mezőket rögtön kitöltjük, hogy látszódjon, mit értett az app – és
     * hogy egy elgépelt szám még mentés előtt javítható legyen.
     */
    void measurementSheet(final String sentence) {
        BodyParse.Body b = BodyParse.parse(sentence);
        if (b.isEmpty()) {
            // Lehet, hogy nem is mérés: a mondat itt is megtalálhatja a helyét.
            final Sentence.Kind k =
                    Sentence.of(sentence, Foods.all(this), System.currentTimeMillis());
            if (k != Sentence.Kind.NONE && k != Sentence.Kind.BODY) {
                new Sheet(this, "Ez máshova való 🧭",
                        "Ezt a mondatot a(z) " + Sentence.where(k) + " érti meg – "
                                + "és megy veled, nem kell újra begépelni.")
                        .addPrimary(Sentence.where(k), () -> Ux.openFor(this, k, sentence))
                        .addNeutral("Átírom", this::measurementInputSheet)
                        .addCancel()
                        .show();
                return;
            }
            new Sheet(this, "Ebből nem lettem okos 🤔",
                    "A mérleg vagy a mérőszalag számát keresem – „78,4 kg”, "
                            + "„78 kiló vagyok”, „mérleg: 81,2”, „derék 84 cm”.")
                    .addPrimary("Újra", this::measurementInputSheet)
                    .addCancel()
                    .show();
            return;
        }
        if (b.kg > 0) weightEt.setText(trim(b.kg));
        if (b.fatPct > 0) bodyFatEt.setText(trim(b.fatPct));
        pendingCm = b.hasCm() ? b.cm : null;
        recompute();
        new Sheet(this, "Mérés a mondatból ⚖️",
                "„" + sentence.trim() + "”\n\n→  " + b.label()
                        + "\n\nA mezőkbe már beírtam – ha stimmel, mentheted.")
                .addPrimary("Mentés", this::saveMeasurement)
                .addCancel()
                .show();
    }

    // ---------------- Adatmezők ----------------

    EditText numField(LinearLayout parent, String title, String unit, boolean decimal) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        row.addView(text(title, 15.5f, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
        EditText et = new EditText(this);
        et.setInputType(decimal ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                : InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(TXT);
        et.setTextSize(18);
        et.setGravity(Gravity.END);
        et.setHint("—");
        et.setHintTextColor(MUTED);
        et.setWidth(dp(90));
        row.addView(et);
        TextView u = text("  " + unit, 13, MUTED, false);
        row.addView(u);
        row.setLayoutParams(lp());
        parent.addView(row);
        return et;
    }

    void addBirthRow(LinearLayout parent) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        LinearLayout left = vbox();
        left.addView(text("Születési dátum", 15.5f, TXT, true));
        ageLabel = text("", 12, MUTED, false);
        left.addView(ageLabel);
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));

        byEt = smallNum("ÉÉÉÉ", dp(64));
        bmEt = smallNum("HH", dp(40));
        bdEt = smallNum("NN", dp(40));
        row.addView(byEt);
        row.addView(text(".", 16, MUTED, false));
        row.addView(bmEt);
        row.addView(text(".", 16, MUTED, false));
        row.addView(bdEt);
        row.setLayoutParams(lp());
        parent.addView(row);
    }

    EditText smallNum(String hint, int w) {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(TXT);
        et.setTextSize(17);
        et.setGravity(Gravity.CENTER);
        et.setHint(hint);
        et.setHintTextColor(MUTED);
        et.setWidth(w);
        return et;
    }

    void loadValues() {
        int h = Profile.getHeight(this);
        if (h > 0) heightEt.setText(String.valueOf(h));
        if (Profile.getBirthY(this) > 0) byEt.setText(String.valueOf(Profile.getBirthY(this)));
        if (Profile.getBirthM(this) > 0) bmEt.setText(String.valueOf(Profile.getBirthM(this)));
        if (Profile.getBirthD(this) > 0) bdEt.setText(String.valueOf(Profile.getBirthD(this)));
        double lw = Profile.lastWeight(this);
        if (lw > 0) weightEt.setText(trim(lw));
        double lf = Profile.lastBodyFat(this);
        if (lf > 0) bodyFatEt.setText(trim(lf));
        float gl = Profile.getGoalLoss(this);
        if (gl > 0) goalLossEt.setText(trim(gl));

        watch(heightEt, () -> { Profile.setHeight(this, intOf(heightEt)); recompute(); });
        watch(weightEt, this::recompute);
        watch(bodyFatEt, this::recompute);
        watch(goalLossEt, () -> { Profile.setGoalLoss(this, (float) doubleOf(goalLossEt)); recompute(); });
        Runnable saveBirth = () -> { Profile.setBirth(this, intOf(byEt), intOf(bmEt), intOf(bdEt)); updateAge(); recompute(); };
        watch(byEt, saveBirth);
        watch(bmEt, saveBirth);
        watch(bdEt, saveBirth);
        updateAge();
    }

    void updateAge() {
        int age = Profile.ageYears(this);
        ageLabel.setText(age >= 0 ? ("Kor: " + age + " év") : "év / hó / nap");
    }

    /** BMI, BMR és a fogyási cél számítása egyben. */
    void recompute() {
        int height = intOf(heightEt);
        double weight = doubleOf(weightEt);
        int age = Profile.ageYears(this);
        int sex = Profile.getSex(this);

        // BMI
        double bmi = Profile.bmi(height, weight);
        if (bmi < 0) {
            bmiValue.setText("—");
            bmiCat.setText("add meg a magasságot és a testsúlyt");
        } else {
            bmiValue.setText(String.format(Hu.LOCALE, "%.1f", bmi));
            bmiCat.setText(Profile.bmiCategory(bmi));
        }

        // BMR
        double bmr = Profile.bmr(sex, weight, height, age);
        if (bmr < 0) {
            bmrValue.setText("—");
        } else {
            bmrValue.setText(Math.round(bmr) + " kcal");
        }

        // Cél
        double loss = doubleOf(goalLossEt);
        if (loss <= 0) {
            goalInfo.setText("Írd be, hány kg-ot szeretnél fogyni, és válassz tempót.");
            goalInfo.setTextColor(MUTED);
        } else {
            int ri = Profile.getGoalRate(this);
            double rate = Profile.RATES[Math.max(0, Math.min(3, ri))]; // kg/hét
            double weeks = loss / rate;
            int days = (int) Math.ceil(weeks * 7);
            double deficit = Profile.dailyDeficit(ri);
            java.util.Calendar end = java.util.Calendar.getInstance();
            end.add(java.util.Calendar.DAY_OF_MONTH, days);
            String date = new java.text.SimpleDateFormat("yyyy.MM.dd", new Locale("hu")).format(end.getTime());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format(Hu.LOCALE, "Heti %.2f kg  ·  ~%d hét", rate, (int) Math.ceil(weeks)));
            sb.append("\nCéldátum: ").append(date);
            sb.append(String.format(Locale.US, "\nNapi kalória-deficit: ~%d kcal", Math.round(deficit)));
            if (bmr > 0) {
                // Közös képlet az Étrend képernyővel, hogy ne két külön számot lásson.
                int target = (int) Math.round(Profile.intakeForLoss(bmr, ri));
                sb.append(String.format(Locale.US, "\nJavasolt napi bevitel: ~%d kcal", target));
            }
            goalInfo.setText(sb.toString());
            goalInfo.setTextColor(TXT);
        }
    }

    // ---- Nem / chip segédek ----

    void addSexRow(LinearLayout parent) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        row.addView(text("Nem", 15.5f, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
        String[] sl = {"Férfi", "Nő"};
        int cur = Profile.getSex(this);
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            Button sb = chip(sl[i], cur == i);
            sb.setMinWidth(dp(74));
            sb.setOnClickListener(v -> {
                Profile.setSex(this, idx);
                styleChip(sexBtns[0], idx == 0);
                styleChip(sexBtns[1], idx == 1);
                recompute();
            });
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
            slp.leftMargin = dp(6);
            row.addView(sb, slp);
            sexBtns[i] = sb;
        }
        row.setLayoutParams(lp());
        parent.addView(row);
    }

    Button chip(String label, boolean selected) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD); b.setTextSize(14);
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        b.setStateListAnimator(null);
        styleChip(b, selected);
        return b;
    }

    void styleChip(Button b, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (selected) { bg.setColor(Theme.accent(this)); b.setTextColor(0xFFFFFFFF); }
        else { bg.setColor(CARD2); bg.setStroke(dp(1), LINE); b.setTextColor(TXT); }
        b.setBackground(bg);
    }

    /**
     * A mondatból érkezett körfogatok, amíg a mentés el nem viszi őket.
     *
     * A képernyőn nincs öt mezőjük (a mérőszalag külön lapon van), a
     * „derék 84 cm"-es mondat viszont ugyanabba a bejegyzésbe való, mint a
     * mérleg száma – enélkül némán elveszne.
     */
    double[] pendingCm;

    void saveMeasurement() {
        double w = doubleOf(weightEt);
        double bf = doubleOf(bodyFatEt);
        double bmi = Profile.bmi(intOf(heightEt), w);
        boolean anyCm = false;
        if (pendingCm != null) for (double v : pendingCm) if (v > 0) anyCm = true;
        if (w <= 0 && bf <= 0 && !anyCm) {
            Toast.makeText(this, "Adj meg legalább testsúlyt vagy testzsírt.", Toast.LENGTH_SHORT).show();
            return;
        }
        Profile.addMeasurement(this, System.currentTimeMillis(), w > 0 ? w : -1,
                bf > 0 ? bf : -1, bmi, pendingCm);
        pendingCm = null;
        Toast.makeText(this, "Mérés elmentve ✔", Toast.LENGTH_SHORT).show();
        refreshChart();
        refreshMeasList();
    }

    /**
     * Mérőszalag-lap: öt körfogat, mind elhagyható.
     *
     * A legutóbbi értékekkel indul, mert a körfogat lassan változik – így
     * elég azt átírni, ami tényleg más lett. A derékhez odaírjuk a
     * derék/magasság arányt is: ez az egyetlen körfogat, aminek önmagában is
     * van egészség-jelentése.
     */
    void circumferenceSheet() {
        final LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        final double[] last = Profile.lastCm(this);
        final EditText[] fields = new EditText[BodyParse.PART_KEYS.length];
        for (int i = 0; i < fields.length; i++) {
            EditText et = new EditText(this);
            et.setHint(BodyParse.PART_NAMES[i] + " (cm)");
            et.setHintTextColor(MUTED);
            et.setTextColor(TXT);
            et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if (last[i] > 0) et.setText(trim(last[i]));
            LinearLayout.LayoutParams flp = lp();
            flp.topMargin = dp(6);
            box.addView(et, flp);
            fields[i] = et;
        }
        final int height = intOf(heightEt);
        String sub = "Csak azt töltsd ki, amit megmértél.";
        double r = Profile.waistToHeight(last[0], height);
        // Két tizedes: az arány a második jegyen dől el (0,49 és 0,51 más
        // üzenet), egy tizedessel mindkettő „0,5" lenne.
        if (r > 0) sub += "  Derék/magasság most: "
                + String.format(new Locale("hu"), "%.2f", r)
                + " – " + Profile.waistVerdict(r) + ".";
        new Sheet(this, "Körfogatok 📏", sub)
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    double[] cm = new double[fields.length];
                    boolean any = false;
                    for (int i = 0; i < fields.length; i++) {
                        cm[i] = doubleOf(fields[i]);
                        if (cm[i] < BodyParse.MIN_CM || cm[i] > BodyParse.MAX_CM) cm[i] = 0;
                        if (cm[i] > 0) any = true;
                    }
                    if (!any) {
                        Toast.makeText(this, "Adj meg legalább egy körfogatot.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // A mérleg száma is bekerül, ha épp ki van töltve: egy
                    // reggel egy bejegyzés.
                    double w = doubleOf(weightEt), bf = doubleOf(bodyFatEt);
                    Profile.addMeasurement(this, System.currentTimeMillis(),
                            w > 0 ? w : -1, bf > 0 ? bf : -1,
                            Profile.bmi(intOf(heightEt), w), cm);
                    Toast.makeText(this, "Körfogatok elmentve ✔", Toast.LENGTH_SHORT).show();
                    refreshMeasList();
                })
                .addCancel()
                .show();
    }

    // ---------------- Diagram ----------------

    Button seriesBtn(int idx, String label) {
        Button b = ghost(label);
        // Négy felirat fér el egy sorban: a „Testsúly" tizennégy ponttal a
        // kis kijelzőkön már törne.
        b.setTextSize(13);
        b.setPadding(dp(2), b.getPaddingTop(), dp(2), b.getPaddingBottom());
        b.setOnClickListener(v -> selectSeries(idx));
        seriesBtns[idx] = b;
        return b;
    }

    void selectSeries(int idx) {
        series = idx;
        for (int i = 0; i < seriesBtns.length; i++) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(13));
            if (i == idx) { bg.setColor(Theme.accent(this)); }
            else { bg.setColor(CARD2); bg.setStroke(dp(1), LINE); }
            seriesBtns[i].setBackground(bg);
        }
        refreshChart();
    }

    void refreshChart() {
        JSONArray arr = Profile.measurements(this); // legfrissebb elöl
        int n = arr.length();
        // időrendben (régi -> új)
        String key = series == 0 ? "w" : series == 1 ? "bmi"
                : series == 2 ? "bf" : BodyParse.PART_KEYS[0];
        String unit = series == 0 ? "kg" : series == 1 ? "" : series == 2 ? "%" : "cm";
        int color = series == 0 ? WEIGHT_C : series == 1 ? BMI_C
                : series == 2 ? FAT_C : WAIST_C;
        java.util.ArrayList<Double> vals = new java.util.ArrayList<>();
        java.util.ArrayList<Double> daysOf = new java.util.ArrayList<>();   // a trendhez
        for (int i = n - 1; i >= 0; i--) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            double v = o.optDouble(key, -1);
            if (v > 0) {
                vals.add(v);
                daysOf.add(o.optLong("ts") / 86400000.0);
            }
        }
        if (vals.size() < 2) {
            chart.setData(null, color, unit);
            chartInfo.setText(vals.size() == 0
                    ? "Még nincs elég mentett mérés."
                    : "Legalább 2 mérés kell a diagramhoz.");
            return;
        }
        // A görbe annyit ér, amennyi adat van benne: ha rég volt mérés, a
        // tendencia már nem tendencia, csak két pont.
        String nudge = Profile.measureNudge(
                Profile.daysSinceMeasurement(this, System.currentTimeMillis()));
        double[] ys = new double[vals.size()];
        for (int i = 0; i < ys.length; i++) ys[i] = vals.get(i);
        chart.setData(ys, color, unit);
        double first = ys[0], last = ys[ys.length - 1], diff = last - first;
        String info = String.format(Hu.LOCALE, "Első: %s%s  ·  Utolsó: %s%s  ·  Változás: %+.1f%s",
                trim(first), unit, trim(last), unit, diff, unit.isEmpty() ? "" : " " + unit);
        // Testsúlynál a TREND a beszédes szám: két mérés különbsége félrevezet
        // (a napi ingadozás ±1 kg is lehet), az egyenes viszont az egészet látja.
        if (series == 0 && ys.length >= 3) {
            double[] ds = new double[daysOf.size()];
            for (int i = 0; i < ds.length; i++) ds[i] = daysOf.get(i);
            double perWeek = Profile.weeklyTrend(ds, ys);
            if (Math.abs(perWeek) >= 0.05) {
                info += String.format(Hu.LOCALE, "\n📉 Tendencia: %+.2f kg/hét", perWeek);
                // Van fogyási cél? Mondjuk meg, mikorra jön ki ezzel az ütemmel.
                double goal = Profile.getGoalLoss(this);
                if (goal > 0) {
                    double lost = Math.max(0, first - last);
                    double weeks = Profile.weeksToGoal(goal - lost, perWeek);
                    if (weeks > 0)
                        info += String.format(Hu.LOCALE,
                                "  ·  a célig még %.1f kg (~%d hét ezzel az ütemmel)",
                                goal - lost, Math.round(weeks));
                    else if (goal - lost <= 0.05)
                        info += "  ·  a fogyási célod megvan! 🎉";
                }
            }
        }
        // A deréknél az arány a beszédes szám, nem a centi maga.
        if (series == 3) {
            double r = Profile.waistToHeight(last, intOf(heightEt));
            if (r > 0) info += String.format(new Locale("hu"),
                    "\n📏 Derék/magasság: %.2f – %s", r, Profile.waistVerdict(r));
        }
        if (!nudge.isEmpty()) info += "\n" + nudge;
        chartInfo.setText(info);
    }

    /** Ennyi legutóbbi mérést sorolunk fel – a többi a diagramon látszik. */
    static final int MEAS_LIST = 8;

    /**
     * A legutóbbi mérések listája, törlési lehetőséggel.
     *
     * Egy elgépelt szám (87 a 78 helyett) az egész görbét félrehúzza, a
     * tendenciát és a fogyási becslést is – eddig viszont csak az ÖSSZES
     * mérés törlésével lehetett szabadulni tőle.
     */
    void refreshMeasList() {
        if (measList == null) return;
        measList.removeAllViews();
        JSONArray a = Profile.measurements(this);
        if (a.length() == 0) return;
        measList.addView(text("Mentett mérések", 17, TXT, true));
        measList.addView(gap(10));
        LinearLayout card = card();
        java.text.SimpleDateFormat df =
                new java.text.SimpleDateFormat("yyyy. MMM d. · HH:mm", new Locale("hu"));
        int shown = 0;
        for (int i = 0; i < a.length() && shown < MEAS_LIST; i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            final long ts = o.optLong("ts");
            double w = o.optDouble("w", -1), bf = o.optDouble("bf", -1);
            StringBuilder val = new StringBuilder();
            if (w > 0) val.append(trim(w)).append(" kg");
            if (bf > 0) val.append(val.length() > 0 ? "  ·  " : "").append(trim(bf)).append("%");
            // A mérőszalag adatai ugyanabban a sorban: aki reggel mérlegre áll
            // és körbeméri magát, EGY mérést végzett.
            for (int c2 = 0; c2 < BodyParse.PART_KEYS.length; c2++) {
                double v = o.optDouble(BodyParse.PART_KEYS[c2], -1);
                if (v > 0) val.append(val.length() > 0 ? "  ·  " : "")
                        .append(BodyParse.PART_NAMES[c2]).append(" ").append(trim(v));
            }
            if (val.length() == 0) continue;
            if (shown > 0) card.addView(divider());
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(12), dp(12));
            LinearLayout left = vbox();
            left.addView(text(val.toString(), 15, TXT, true));
            left.addView(text(df.format(new java.util.Date(ts)), 12, MUTED, false));
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView del = text("✕", 17, MUTED, false);
            del.setPadding(dp(12), dp(6), dp(12), dp(6));
            del.setClickable(true);
            del.setOnClickListener(v -> new Sheet(this, "Mérés törlése",
                    val + "\n" + df.format(new java.util.Date(ts)))
                    .addDestructive("Törlés", () -> {
                        Profile.removeMeasurement(this, ts);
                        refreshMeasList();
                        refreshChart();
                        loadLastIntoFields();
                    })
                    .addCancel().show());
            row.addView(del);
            card.addView(row);
            shown++;
        }
        measList.addView(card, lp());
    }

    /**
     * A törlés után a mezők a MEGMARADT legutóbbi mérést mutassák.
     *
     * Enélkül a képernyőn ott maradna a most törölt érték, és a következő
     * mentés némán visszahozná.
     */
    void loadLastIntoFields() {
        double lw = Profile.lastWeight(this);
        weightEt.setText(lw > 0 ? trim(lw) : "");
        double lf = Profile.lastBodyFat(this);
        bodyFatEt.setText(lf > 0 ? trim(lf) : "");
        recompute();
    }

    // ---------------- Segéd UI ----------------

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }

    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }
    LinearLayout.LayoutParams selLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.leftMargin = dp(4); p.rightMargin = dp(4);
        return p;
    }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(MainActivity.GLASS);
        bg.setCornerRadius(dp(20));
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

    View divider() {
        View v = new View(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
        p.leftMargin = dp(16); p.rightMargin = dp(16);
        v.setLayoutParams(p);
        v.setBackgroundColor(LINE);
        return v;
    }

    Button primary(String label) {
        Button b = base(label);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.accent(this), Theme.accent2(this)});
        bg.setCornerRadius(dp(18));
        b.setBackground(bg); b.setTextColor(0xFFFFFFFF); b.setTextSize(17);
        return b;
    }

    Button ghost(String label) {
        Button b = base(label);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2); bg.setCornerRadius(dp(13)); bg.setStroke(dp(1), LINE);
        b.setBackground(bg); b.setTextSize(15);
        return b;
    }

    Button base(String label) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextColor(TXT);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(dp(14), dp(14), dp(14), dp(14));
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        b.setStateListAnimator(null);
        return b;
    }

    void watch(EditText et, Runnable onChange) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { onChange.run(); }
        });
    }

    int intOf(EditText et) {
        try { return Integer.parseInt(et.getText().toString().trim()); } catch (Exception e) { return 0; }
    }
    double doubleOf(EditText et) {
        try { return Double.parseDouble(et.getText().toString().trim().replace(',', '.')); } catch (Exception e) { return 0; }
    }
    String trim(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format(Hu.LOCALE, "%.1f", v);
    }

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    // ---------------- Diagram nézet ----------------

    static class ChartView extends View {
        private double[] ys;
        private int color = ACCENT;
        private String unit = "";
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float density;

        ChartView(Context c) {
            super(c);
            density = c.getResources().getDisplayMetrics().density;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setStrokeWidth(density * 2.5f);
            dot.setStyle(Paint.Style.FILL);
            grid.setStyle(Paint.Style.STROKE);
            grid.setStrokeWidth(density);
            // A MainActivity palettája minden képernyőn inicializálódik (applyPalette),
            // a saját statikus színeink viszont csak a Profil megnyitása után – ezért
            // azokra nem támaszkodhatunk (láthatatlan feliratokat okozott).
            grid.setColor(MainActivity.LINE != 0 ? MainActivity.LINE : 0x2EFFFFFF);
            txt.setColor(MainActivity.MUTED != 0 ? MainActivity.MUTED : 0xFFA98F95);
            txt.setTextSize(density * 11f);
        }

        void setData(double[] ys, int color, String unit) {
            this.ys = ys; this.color = color; this.unit = unit;
            line.setColor(color); dot.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (ys == null || ys.length < 2) return;
            float W = getWidth(), H = getHeight();
            float padL = density * 40, padR = density * 14, padT = density * 12, padB = density * 22;
            double mn = ys[0], mx = ys[0];
            for (double v : ys) { mn = Math.min(mn, v); mx = Math.max(mx, v); }
            if (mx - mn < 1e-6) { mx = mn + 1; mn -= 1; }

            // vízszintes segédvonalak + y címkék (min, közép, max)
            for (int g = 0; g <= 2; g++) {
                float y = padT + (H - padT - padB) * g / 2f;
                canvas.drawLine(padL, y, W - padR, y, grid);
                double val = mx - (mx - mn) * g / 2.0;
                // A felső címke mellé a mértékegység is kikerül.
                canvas.drawText(fmt(val) + (g == 0 && !unit.isEmpty() ? " " + unit : ""),
                        density * 4, y + density * 4, txt);
            }

            path.reset();
            for (int i = 0; i < ys.length; i++) {
                float x = padL + (W - padL - padR) * i / (ys.length - 1);
                float y = (float) (padT + (H - padT - padB) * (1 - (ys[i] - mn) / (mx - mn)));
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            canvas.drawPath(path, line);
            for (int i = 0; i < ys.length; i++) {
                float x = padL + (W - padL - padR) * i / (ys.length - 1);
                float y = (float) (padT + (H - padT - padB) * (1 - (ys[i] - mn) / (mx - mn)));
                canvas.drawCircle(x, y, density * 3f, dot);
            }
        }

        private String fmt(double v) {
            if (Math.abs(v - Math.round(v)) < 0.05) return String.valueOf(Math.round(v));
            return String.format(Hu.LOCALE, "%.1f", v);
        }
    }

    /**
     * Máshonnan ideirányított mondat: nyissuk meg vele a felvételi lapot.
     *
     * Az onNewIntent is ide fut: ha a képernyő már a veremben volt, az
     * átirányítás előrehozza (nem hoz létre másodikat), és a mondat ezen az
     * úton érkezik – az onCreate ilyenkor le sem futna.
     */
    void handleSentenceIntent(android.content.Intent intent) {
        if (intent == null) return;
        final String sent = intent.getStringExtra(Sentence.EXTRA);
        if (sent == null || sent.trim().isEmpty()) return;
        intent.removeExtra(Sentence.EXTRA);
        getWindow().getDecorView().post(() -> { if (!isFinishing()) measurementSheet(sent); });
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSentenceIntent(intent);
    }
}
