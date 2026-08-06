package com.edzo.idozito;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Erősítő edzésnapló képernyő: sorozatok (ismétlés × súly) rögzítése
 * gyakorlatonként, saját rekordokkal (max súly, becsült 1RM) és a bejegyzések
 * listájával. Súlyos edzésekhez, a HIIT-időzítő mellé.
 */
public class StrengthActivity extends Activity {

    static int BG, CARD, CARD2, TXT, MUTED, LINE;

    LinearLayout recordsBox, listBox, summaryBox;
    EditText searchEt;

    /** Egyszerre ennyi bejegyzés-kártya épül fel; a gomb továbbiakat tölt be. */
    static final int PAGE = 60;
    int shownLimit = PAGE;
    android.widget.FrameLayout rootFl; // konfetti-ünnepléshez (új rekord)

    // Pihenő-időzítő a sorozatok között
    final Handler restHandler = new Handler(Looper.getMainLooper());
    Runnable restTick;
    long restEnd;
    TextView restText;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); BG=MainActivity.BG; CARD=MainActivity.CARD; CARD2=MainActivity.CARD2; TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; LINE=MainActivity.LINE;
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Erősítő napló", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Rögzítsd a sorozatokat (ismétlés × súly), és kövesd a rekordjaid.", 13, MUTED, false));
        col.addView(gap(20));

        Button add = primary("＋  Új bejegyzés");
        add.setOnClickListener(v -> addEntryDialog(null));
        col.addView(add);
        col.addView(gap(10));

        Button quick = ghost("✍️  Sorozatok mondatból");
        quick.setOnClickListener(v -> sentenceSheet(""));
        col.addView(quick);
        col.addView(gap(16));

        col.addView(restCard());
        col.addView(gap(12));

        Button plate = ghost("🧮  Súlytárcsa-kalkulátor");
        plate.setOnClickListener(v -> openPlateCalc());
        col.addView(plate);
        col.addView(gap(10));

        Button orm = ghost("📈  1RM & százalék kalkulátor");
        orm.setOnClickListener(v -> openOneRmCalc());
        col.addView(orm);
        col.addView(gap(10));

        Button days = ghost("📅  Edzésnapok (sablonok)");
        days.setOnClickListener(v -> routineSheet());
        col.addView(days);
        col.addView(gap(10));

        Button warm = ghost("🔥  Bemelegítő rámpa");
        warm.setOnClickListener(v -> openWarmupCalc());
        col.addView(warm);
        col.addView(gap(16));

        summaryBox = vbox();
        col.addView(summaryBox, lp());
        col.addView(gap(16));

        col.addView(text("Rekordok", 15.5f, TXT, true));
        col.addView(gap(10));
        recordsBox = vbox();
        col.addView(recordsBox, lp());
        col.addView(gap(16));

        col.addView(text("Bejegyzések", 15.5f, TXT, true));
        col.addView(gap(8));
        searchEt = new EditText(this);
        searchEt.setHint("Keresés a naplóban (pl. guggolás)");
        searchEt.setHintTextColor(MUTED);
        searchEt.setTextColor(TXT);
        searchEt.setTextSize(13);
        searchEt.setSingleLine(true);
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
        rootFl = Ux.scaffoldNav(this, sv, "bg_workout", 2);
        setContentView(rootFl);
        col.post(() -> Ux.enterChildren(col, 30, 45));
        refresh();
    }

    /** Súly formázása: egész kg-nál tizedes nélkül. */
    String kg(double w) {
        return w == Math.floor(w) ? String.valueOf((long) w)
                : String.format(Hu.LOCALE, "%.1f", w);
    }

    void refresh() {
        refreshSummary();
        refreshRecords();
        refreshList();
    }

    // ---------- Heti összegző ----------

    void refreshSummary() {
        summaryBox.removeAllViews();
        long since = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        long prevSince = System.currentTimeMillis() - 14L * 24 * 3600 * 1000;
        double vol = 0, allVol = 0, prevVol = 0;
        int count = 0, allCount = 0;
        for (StrengthLog.Entry e : StrengthLog.load(this)) {
            double v = e.volume();
            allVol += v; allCount++;
            if (e.ts >= since) { vol += v; count++; }
            else if (e.ts >= prevSince) prevVol += v;
        }
        if (allCount == 0) return; // csak akkor mutatjuk, ha van bejegyzés
        LinearLayout card = card();
        LinearLayout inner = vbox();
        inner.setPadding(dp(16), dp(14), dp(16), dp(14));
        inner.addView(text("📊  Utolsó 7 nap", 12.5f, MUTED, true));
        inner.addView(gap(4));
        inner.addView(text(Math.round(vol) + " kg összvolumen", 20, Theme.accent(this), true));
        inner.addView(text(count + " bejegyzés az elmúlt héten", 12.5f, MUTED, false));
        // Trend az azt megelőző 7 naphoz képest.
        if (prevVol > 0) {
            double ch = (vol - prevVol) / prevVol * 100;
            String arrow = ch >= 5 ? "📈" : ch <= -5 ? "📉" : "➖";
            inner.addView(text(arrow + "  " + (ch >= 0 ? "+" : "") + Math.round(ch)
                    + "% volumen az előző héthez képest", 12.5f, MUTED, false));
        }
        inner.addView(gap(8));
        inner.addView(text("Összesen: " + Math.round(allVol) + " kg · " + allCount + " alkalom",
                12.5f, MUTED, false));
        // Mi maradt ki? A rekordlista nem árulja el magától, mert a legutóbb
        // használt gyakorlat van elöl – a régen kimaradt leghátul, észrevétlenül.
        final String forgotten = StrengthLog.mostNeglected(
                StrengthLog.load(this), System.currentTimeMillis(), StrengthLog.NEGLECTED_DAYS);
        if (forgotten != null) {
            int d = StrengthLog.daysSince(StrengthLog.load(this), forgotten,
                    System.currentTimeMillis());
            inner.addView(gap(8));
            TextView nudge = text("💤  " + forgotten + " " + StrengthLog.agoLabel(d)
                    + " maradt ki. Beveszed ma?", 12.5f, Theme.accent(this), true);
            nudge.setClickable(true);
            nudge.setOnClickListener(v -> showProgress(forgotten));
            inner.addView(nudge);
        }
        addBalance(inner);
        addSuggestion(inner);
        card.addView(inner);
        summaryBox.addView(card, lp());
    }

    /**
     * „Mit vegyél be ma?" – a héten kimaradt izomcsoportokból egy-egy
     * gyakorlat, a hozzá tartozó progresszió-javaslattal.
     *
     * Az egyensúly-csipek megmutatják, MI maradt ki, de a hiány önmagában még
     * nem terv. Itt már konkrét sor van: gyakorlat, sorozat, súly – egy
     * koppintásra a fejlődés-lapjával.
     */
    void addSuggestion(LinearLayout inner) {
        List<StrengthLog.Entry> log = StrengthLog.load(this);
        // A heti fókusz erősebb, mint az egyensúly-heurisztika: ha ma hát-nap
        // van, a hát jöjjön elöl akkor is, ha a hét eddig kiegyensúlyozott.
        int dow = (java.util.Calendar.getInstance()
                .get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;
        String focus = Weekplan.forDay(Theme.planFocus(this), dow);
        // Ha van a mai fókuszhoz illő edzésnap, az ERŐSEBB az egyenkénti
        // ajánlásnál: egy kész nap teljes, sorrendbe rakott terv, nem három
        // különálló gyakorlat.
        java.util.List<Routines.Routine> all =
                Routines.all(Theme.getStr(this, Routines.KEY, ""));
        Routines.Routine day = null;
        String why = focus;
        if (!focus.isEmpty()) {
            for (Routines.Routine r : all)
                if (Foods.norm(r.name).contains(Foods.norm(focus))) { day = r; break; }
        } else {
            // Heti terv nélkül a rotáció válaszol: a legrégebben csinált nap.
            long[] rts = new long[log.size()];
            String[] rnames = new String[log.size()];
            for (int i = 0; i < log.size(); i++) {
                rts[i] = log.get(i).ts;
                rnames[i] = log.get(i).name;
            }
            String due = Routines.nextUp(all, rts, rnames, System.currentTimeMillis());
            for (Routines.Routine r : all)
                if (r.name.equals(due)) { day = r; why = "a legrégebben volt"; break; }
        }
        if (day != null) {
            final String dayName = day.name;
            inner.addView(gap(12));
            inner.addView(text("Ma ez jön · " + why, 12, MUTED, true));
            TextView row = text("🎯  " + day.label(), 13.5f, Theme.accent(this), true);
            row.setPadding(0, dp(6), 0, 0);
            row.setClickable(true);
            row.setOnClickListener(v -> routineDaySheet(dayName));
            inner.addView(row);
            TextView sub = text(day.summary(), 12, MUTED, false);
            sub.setPadding(0, dp(2), 0, 0);
            inner.addView(sub);
            return;
        }

        List<String> picks = Muscles.suggestForToday(log, System.currentTimeMillis(), 3,
                focus.isEmpty() ? null : focus);
        if (picks.isEmpty()) return;

        inner.addView(gap(12));
        inner.addView(text(focus.isEmpty()
                ? "Mai ajánlat a kimaradt izomcsoportokra"
                : "Mai ajánlat · " + focus, 12, MUTED, true));
        for (final String name : picks) {
            Progression.Suggestion s = Progression.next(log, name);
            String line = "🎯  " + name + (s != null ? "  ·  " + s.headline() : "");
            TextView row = text(line, 13, Theme.accent(this), true);
            row.setPadding(0, dp(6), 0, 0);
            row.setClickable(true);
            row.setOnClickListener(v -> showProgress(name));
            inner.addView(row);
        }
    }

    /**
     * Izomcsoport-egyensúly az elmúlt 7 napban. Csak azokat a csoportokat
     * mutatja, amiket a felhasználó valaha edzett – amit sosem csinált, annak
     * a hiánya nem hiba, hanem döntés.
     */
    void addBalance(LinearLayout inner) {
        java.util.LinkedHashMap<String, Integer> bal = Muscles.weekBalance(
                StrengthLog.load(this), System.currentTimeMillis(), 7);
        if (bal.size() < 2) return;         // egy csoportból nincs mit egyensúlyozni

        inner.addView(gap(12));
        inner.addView(text("Izomcsoportok az elmúlt 7 napban", 12, MUTED, true));
        inner.addView(gap(6));

        LinearLayout chips = hbox();
        List<String> missing = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : bal.entrySet()) {
            int n = e.getValue();
            if (n == 0) missing.add(e.getKey());
            TextView chip = text(e.getKey() + "  " + (n > 0 ? n + "×" : "–"),
                    12.5f, n > 0 ? TXT : MUTED, n > 0);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(CARD2);
            bg.setCornerRadius(dp(10));
            bg.setStroke(dp(1), n > 0 ? Theme.accent(this) : LINE);
            chip.setBackground(bg);
            chip.setPadding(dp(10), dp(6), dp(10), dp(6));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(6);
            chips.addView(chip, clp);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(chips);
        inner.addView(hs);

        if (!missing.isEmpty() && missing.size() < bal.size()) {
            inner.addView(gap(6));
            inner.addView(text(Muscles.andList(missing) + " kimaradt a héten.",
                    12.5f, MUTED, false));
        }
    }

    // ---------- Rekordok ----------

    void refreshRecords() {
        recordsBox.removeAllViews();
        List<StrengthLog.Entry> all = StrengthLog.load(this);
        if (all.isEmpty()) {
            recordsBox.addView(text("Még nincs rögzített gyakorlat. Vedd fel fent – vagy írd "
                    + "le egy mondatban: „3x10 fekvenyomás 60 kg”.", 13.5f, MUTED, false));
            return;
        }
        // Gyakorlatnevek a legutóbbi használat sorrendjében (a lista legújabb elöl).
        LinkedHashMap<String, Boolean> names = new LinkedHashMap<>();
        for (StrengthLog.Entry e : all) if (e.name != null) names.put(e.name, true);

        LinearLayout card = card();
        boolean first = true;
        for (String n : names.keySet()) {
            double[] rec = StrengthLog.recordsFor(this, n);
            if (!first) {
                View dv = new View(this);
                LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                dvp.leftMargin = dp(16); dvp.rightMargin = dp(16);
                dv.setLayoutParams(dvp); dv.setBackgroundColor(LINE);
                card.addView(dv);
            }
            first = false;
            final String exName = n;
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout rl = vbox();
            rl.addView(text(n, 15.5f, TXT, true));
            String sub = "Max " + fmtKg(rec[0]) + " kg   ·   becsült 1RM ~" + fmtKg(rec[1]) + " kg";
            rl.addView(text(sub, 12.5f, MUTED, false));
            // Mikor volt utoljára? A régen kimaradt gyakorlat kiemelve.
            int since = StrengthLog.daysSince(all, n, System.currentTimeMillis());
            boolean stale = since >= StrengthLog.NEGLECTED_DAYS;
            rl.addView(text((stale ? "💤  " : "") + "Utoljára " + StrengthLog.agoLabel(since),
                    12, stale ? Theme.accent(this) : MUTED, stale));
            row.addView(rl, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text("📈", 15, MUTED, false));
            row.setClickable(true);
            row.setOnClickListener(v -> showProgress(exName));
            card.addView(row);
        }
        recordsBox.addView(card, lp());
    }

    // ---------- Bejegyzések ----------

    void refreshList() {
        listBox.removeAllViews();
        List<StrengthLog.Entry> all = StrengthLog.load(this);
        if (all.isEmpty()) return;
        // Szűrés a kereső alapján (gyakorlatnév, ékezet-érzéketlenül).
        String q = searchEt == null ? "" : Foods.norm(searchEt.getText().toString().trim());
        if (!q.isEmpty()) {
            List<StrengthLog.Entry> flt = new ArrayList<>();
            for (StrengthLog.Entry e : all)
                if (e.name != null && Foods.norm(e.name).contains(q)) flt.add(e);
            all = flt;
            if (all.isEmpty()) {
                listBox.addView(text("Nincs a keresésre illő gyakorlat.", 13.5f, MUTED, false));
                return;
            }
        }
        SimpleDateFormat fmt = new SimpleDateFormat("MM. dd. HH:mm", new Locale("hu"));
        // Az erősítő naplónak nincs felső korlátja, évek alatt sok száz bejegyzés
        // gyűlhet – ennyi kártyát egyszerre felépíteni megakasztaná a képernyőt.
        int limit = Math.min(all.size(), shownLimit);
        for (int i = 0; i < limit; i++) {
            final StrengthLog.Entry e = all.get(i);
            LinearLayout card = card();
            LinearLayout inner = vbox();
            inner.setPadding(dp(16), dp(12), dp(16), dp(12));

            LinearLayout top = hbox();
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(text(e.name, 16, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
            top.addView(text(Math.round(e.volume()) + " kg volumen", 12, Theme.accent(this), true));
            inner.addView(top);

            inner.addView(text(fmt.format(new Date(e.ts))
                    + (e.rpe > 0 ? "   ·   RPE " + e.rpe : ""), 11.5f, MUTED, false));
            inner.addView(gap(6));

            StringBuilder sb = new StringBuilder();
            for (StrengthLog.SetEntry s : e.sets) {
                if (sb.length() > 0) sb.append("    ·    ");
                sb.append(StrengthLog.setLabel(e.name, s));
            }
            inner.addView(text(sb.toString(), 13.5f, TXT, false));

            card.addView(inner);
            card.setClickable(true);
            card.setOnClickListener(v -> new Sheet(this, e.name, "Szerkesztés vagy törlés?")
                    .addNeutral("✏️  Szerkesztés", () -> addEntryDialog(e))
                    .addDestructive("Törlés",
                            () -> {
                                StrengthLog.removeByTs(this, e.ts);
                                // A mai nap egyetlen bejegyzésének törlésével a
                                // széria is megváltozhat – a widget is kövesse.
                                BlazeWidget.refresh(this);
                                refresh();
                            })
                    .addCancel().show());
            listBox.addView(card, lp());
            listBox.addView(gap(10));
        }
        if (all.size() > limit) {
            final int remaining = all.size() - limit;
            Button more = ghost("További " + Math.min(remaining, PAGE) + " bejegyzés  ("
                    + remaining + " van még)");
            more.setTextSize(13.5f);
            more.setOnClickListener(v -> { shownLimit += PAGE; refreshList(); });
            listBox.addView(more);
        }
    }

    // ---------- Fejlődés (súly-grafikon) ----------

    void showProgress(String name) {
        List<StrengthLog.Entry> all = StrengthLog.load(this);
        List<Double> tops = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {        // régi → új sorrend
            if (name.equals(all.get(i).name)) tops.add(all.get(i).topWeight());
        }
        double[] rec = StrengthLog.recordsFor(this, name);
        LinearLayout box = vbox();
        box.setPadding(dp(8), dp(4), dp(8), dp(4));
        box.addView(text("Max " + fmtKg(rec[0]) + " kg   ·   becsült 1RM ~" + fmtKg(rec[1]) + " kg",
                14, TXT, true));
        box.addView(gap(10));
        if (tops.size() >= 2) {
            double[] arr = new double[tops.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = tops.get(i);
            ProfileActivity.ChartView chart = new ProfileActivity.ChartView(this);
            chart.setData(arr, Theme.accent(this), "kg");
            box.addView(chart, new LinearLayout.LayoutParams(-1, dp(160)));
            box.addView(gap(6));
            box.addView(text(tops.size() + " alkalom · a felső súly alakulása", 12, MUTED, false));
        } else {
            box.addView(text("Legalább 2 alkalom kell a grafikonhoz. Rögzíts még egyet!", 13, MUTED, false));
        }
        // Ne csak a múltat mutassuk: mi a következő lépés?
        Progression.Suggestion sug = Progression.next(all, name);
        if (sug != null) {
            box.addView(gap(12));
            box.addView(text("Következő alkalom:  " + sug.headline(), 14, Theme.accent(this), true));
            box.addView(gap(3));
            box.addView(text(sug.why, 12, MUTED, false));
            if (!sug.bodyweight) {
                java.util.List<Warmup.Set> ramp =
                        Warmup.forWork(sug.weight, Warmup.barFor(name));
                if (!ramp.isEmpty()) {
                    box.addView(gap(6));
                    box.addView(text("🔥  Bemelegítés:  " + Warmup.summary(ramp),
                            12, MUTED, false));
                }
            }
        }
        new Sheet(this, name, "Fejlődés").addCustom(box).addNeutral("Bezár", () -> {}).show();
    }

    // ---------- Sorozatok mondatból ----------

    /** Egy mondatból több gyakorlat sorozatai – gépelés helyett. */
    void sentenceSheet(String prefill) {
        final LinearLayout box = vbox();
        box.setPadding(dp(10), dp(6), dp(10), 0);
        final EditText et = new EditText(this);
        et.setHint(Examples.hint(Examples.SET, System.currentTimeMillis()));
        et.setHintTextColor(MUTED);
        et.setTextColor(TXT);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setText(prefill);
        et.setSelection(prefill.length());
        box.addView(et, lp());

        // Élő visszajelzés: gépelés közben látszik, mit értett meg.
        final TextView reco = text("", 12.5f, MUTED, false);
        reco.setPadding(0, dp(8), 0, 0);
        box.addView(reco, lp());
        et.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable e) {
                List<StrengthParse.Item> items = StrengthParse.parse(e.toString());
                if (items.isEmpty()) { reco.setText(""); return; }
                StringBuilder sb = new StringBuilder("✔ Felismerve:");
                for (StrengthParse.Item it : items) sb.append("\n•  ").append(it.label());
                reco.setText(sb.toString());
            }
        });

        new Sheet(this, "Sorozatok mondatból ✍️",
                "Írd le egy mondatban, mit nyomtál. Mentés előtt megmutatom, mit értettem.")
                .addCustom(box)
                .addPrimary("Tovább", () -> sentencePreview(et.getText().toString()))
                .addCancel()
                .show();
    }

    /** Előnézet mentés előtt: kitalált sorozat a rekordokba is bekerülne. */
    void sentencePreview(String textIn) {
        final List<StrengthParse.Item> items = StrengthParse.parse(textIn);
        if (items.isEmpty()) {
            new Sheet(this, "Ebből nem lettem okos 🤔",
                    "Ismétlésszám nélkül nem mentek – próbáld így: „3x10 fekvenyomás 60 kg”.")
                    .addPrimary("Újra", () -> sentenceSheet(textIn))
                    .addCancel()
                    .show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (StrengthParse.Item it : items) {
            sb.append("•  ").append(it.label()).append('\n');
            String cmp = versusLast(it);
            if (cmp != null) sb.append("     ").append(cmp).append('\n');
        }
        // A mondat időpontot is mondhat („tegnap guggolás 3x10 100 kg”), ahogy
        // az étrendben – aki este pótolja az edzést, ne kézzel javítsa a napot.
        final long when = TimeHint.from(textIn, System.currentTimeMillis());
        if (when >= System.currentTimeMillis() - 60000) {
            sb.append("\nMai dátummal, a naplód élére.");
        } else {
            sb.append("\nDátum: ").append(new SimpleDateFormat("yyyy. MMM d. · HH:mm",
                    new Locale("hu")).format(new Date(when))).append('.');
        }
        new Sheet(this, items.size() + " gyakorlat mentése", sb.toString())
                .addPrimary("Mentés", () -> saveSentence(items, when))
                .addNeutral("Átírom", () -> sentenceSheet(textIn))
                .addCancel()
                .show();
    }

    /**
     * Összevetés a legutóbbi ugyanilyen gyakorlattal – ez adja meg az
     * értelmét a mai számoknak: a 60 kg önmagában semmit nem mond, a
     * „múltkor 57,5 volt” viszont mindent.
     */
    String versusLast(StrengthParse.Item it) {
        StrengthLog.Entry last = null;
        for (StrengthLog.Entry e : StrengthLog.load(this))
            if (it.name.equals(e.name)) { last = e; break; }   // a lista legújabb elöl
        if (last == null) return "↳ első alkalom ezzel a gyakorlattal 🌱";
        double now = it.topWeight(), then = last.topWeight();
        String when = "  ·  " + StrengthLog.agoLabel(
                StrengthLog.dayDiff(last.ts, System.currentTimeMillis()));
        if (now > 0 && then > 0) {
            double d = now - then;
            String arrow = d > 0.01 ? "▲ +" + kg(d) + " kg"
                    : d < -0.01 ? "▼ −" + kg(-d) + " kg" : "= ugyanannyi";
            return "↳ múltkor " + kg(then) + " kg  ·  " + arrow + when;
        }
        int nr = it.totalReps(), tr = last.totalReps();
        if (nr != tr) return "↳ múltkor " + tr + " " + StrengthParse.unit(it.name) + "  ·  "
                + (nr > tr ? "▲ +" + (nr - tr) : "▼ −" + (tr - nr)) + when;
        return "↳ múltkor ugyanennyi" + when;
    }

    void saveSentence(List<StrengthParse.Item> items) {
        saveSentence(items, System.currentTimeMillis());
    }

    void saveSentence(List<StrengthParse.Item> items, long now) {
        int i = 0;
        boolean record = false;
        for (StrengthParse.Item it : items) {
            List<StrengthLog.SetEntry> sets = new ArrayList<>();
            for (StrengthParse.Set s : it.sets)
                sets.add(new StrengthLog.SetEntry(s.reps, s.weight));
            // A rekord-ellenőrzés a mentés ELŐTTI állásból nézi a gyakorlatot.
            double[] prev = StrengthLog.recordsFor(this, it.name);
            // Másodperc-eltolás: minden bejegyzés külön időbélyeget kap (az
            // azonosítja őket megnyitáskor és törléskor).
            StrengthLog.Entry e = new StrengthLog.Entry(now - i++, it.name, sets, it.rpe);
            StrengthLog.add(this, e);
            if (prev[0] > 0 && e.topWeight() > prev[0]) record = true;
        }
        BlazeWidget.refresh(this);
        refresh();
        if (record && rootFl != null) Confetti.burst(rootFl);
        Toast.makeText(this, "Mentve ✔  (" + items.size() + " gyakorlat)"
                + (record ? "  ·  új rekord! 🏆" : ""), Toast.LENGTH_LONG).show();
    }

    // ---------- Új bejegyzés ----------

    void addEntryDialog(final StrengthLog.Entry edit) {
        addEntryDialog(edit, null);
    }

    /**
     * @param preset előre kitöltött gyakorlatnév (edzésnap-sablonból), vagy null
     */
    void addEntryDialog(final StrengthLog.Entry edit, final String preset) {
        // Nem edzésnapból nyílt: az esetleg ottfelejtett visszatérés törlődik,
        // különben egy megszakított nap után a következő mentés is odaugrana.
        if (preset == null) openRoutine = null;
        final LinearLayout box = vbox();
        box.setPadding(dp(10), dp(6), dp(10), 0);

        final EditText nameEt = new EditText(this);
        nameEt.setHint("Gyakorlat neve (pl. Guggolás)");
        nameEt.setHintTextColor(MUTED);
        nameEt.setTextColor(TXT);
        nameEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (edit != null) nameEt.setText(edit.name);
        box.addView(nameEt);

        // Előre deklarálva, hogy a névchipek elő tudják tölteni a legutóbbi alkalmat.
        final LinearLayout setsBox = vbox();
        final List<EditText> repsList = new ArrayList<>();
        final List<EditText> wList = new ArrayList<>();

        // A progresszió-javaslat kártyája (csak új bejegyzésnél; szerkesztésnél
        // a régi alkalmat nézzük, ott félrevezető lenne a „mai" javaslat).
        final LinearLayout suggestBox = vbox();

        // Tartásnál (plank) a bal oldali mező másodperc, nem ismétlés – a
        // felirat és a mezők súgója ezért a névtől függ, és a névvel együtt
        // változik.
        final String startName = edit != null ? edit.name : preset == null ? "" : preset;
        final TextView setsLabel = text(setsCaption(startName), 13, MUTED, true);

        // Gyors nevek vízszintes chip-sávban
        box.addView(gap(8));
        LinearLayout chips = hbox();
        for (final String n : StrengthLog.knownNames(this)) {
            Button chip = ghost(n);
            chip.setTextSize(12.5f);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(6);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                nameEt.setText(n);
                nameEt.setSelection(n.length());
                // A javaslatot a név figyelője frissíti (a setText kiváltja).
                prefillFromLast(n, setsBox, repsList, wList);
            });
            chips.addView(chip);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(chips);
        box.addView(hs);

        box.addView(suggestBox, lp());
        if (edit == null) {
            // Kézzel beírt névre is jöjjön a javaslat (nem csak a chipekre).
            nameEt.addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
                public void onTextChanged(CharSequence s, int a, int b2, int c) {}
                public void afterTextChanged(android.text.Editable e) {
                    String n = e.toString().trim();
                    showSuggestion(n, suggestBox, setsBox, repsList, wList);
                    setsLabel.setText(setsCaption(n));
                    for (EditText r : repsList) r.setHint(repsHint(n));
                }
            });
        }

        box.addView(gap(12));
        box.addView(setsLabel);
        box.addView(gap(4));

        box.addView(setsBox);
        if (edit != null && !edit.sets.isEmpty()) {
            for (StrengthLog.SetEntry s : edit.sets) {
                addSetRow(setsBox, repsList, wList, repsHint(startName));
                int idx = repsList.size() - 1;
                repsList.get(idx).setText(String.valueOf(s.reps));
                // A nulla kiló nem információ: testsúlyosnál maradjon üres.
                wList.get(idx).setText(s.weight > 0 ? fmtKg(s.weight) : "");
            }
        } else {
            for (int i = 0; i < 3; i++)
                addSetRow(setsBox, repsList, wList, repsHint(startName));
        }
        // Edzésnap-sablonból nyitva: a név már megvan, a legutóbbi alkalom és a
        // javaslat is jöjjön magától – a figyelő beállítása UTÁN, hogy lásson.
        if (edit == null && preset != null && !preset.trim().isEmpty()) {
            nameEt.setText(preset);
            nameEt.setSelection(preset.length());
            prefillFromLast(preset, setsBox, repsList, wList);
        }

        Button more = ghost("＋  Sorozat");
        more.setTextSize(13.5f);
        more.setOnClickListener(v -> addSetRow(setsBox, repsList, wList,
                repsHint(nameEt.getText().toString())));
        box.addView(gap(4));
        box.addView(more);

        // Érzett terhelés (RPE): a súly és az ismétlés nem mondja meg, mennyi
        // maradt a tankban – a progresszió-javaslat viszont pont ezen múlik.
        // Elhagyható: aki nem tölti ki, ugyanazt kapja, mint eddig.
        final int[] rpe = {edit != null ? edit.rpe : 0};
        box.addView(gap(12));
        box.addView(text("Érzett terhelés (elhagyható)", 12, MUTED, true));
        box.addView(gap(6));
        LinearLayout rpeRow = hbox();
        final Button[] rpeBtns = new Button[5];
        for (int i = 0; i < 5; i++) {
            final int val = 6 + i;
            Button b = ghost(String.valueOf(val));
            b.setTextSize(13.5f);
            b.setOnClickListener(v -> {
                rpe[0] = rpe[0] == val ? 0 : val;   // ismételt koppintás: törlés
                styleRpe(rpeBtns, rpe[0]);
            });
            rpeBtns[i] = b;
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.leftMargin = dp(3); p.rightMargin = dp(3);
            rpeRow.addView(b, p);
        }
        styleRpe(rpeBtns, rpe[0]);
        box.addView(rpeRow);
        box.addView(gap(4));
        box.addView(text("6 = könnyű  ·  8 = 2 ismétlés maradt  ·  10 = a határon",
                11, MUTED, false));

        new Sheet(this, edit != null ? "Bejegyzés szerkesztése" : "Új bejegyzés")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    String name = nameEt.getText().toString().trim();
                    if (name.isEmpty()) name = "Gyakorlat";
                    List<StrengthLog.SetEntry> sets = new ArrayList<>();
                    for (int i = 0; i < repsList.size(); i++) {
                        int reps = parseInt(repsList.get(i).getText().toString());
                        double w = parseDouble(wList.get(i).getText().toString());
                        if (reps > 0) sets.add(new StrengthLog.SetEntry(reps, Math.max(0, w)));
                    }
                    if (sets.isEmpty()) {
                        // Nincs érvényes sorozat – legalább visszajelzünk, miért nem mentünk.
                        Toast.makeText(this, "Nem mentettem: adj meg legalább egy sorozatot (ismétlés).",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    // A korábbi rekordok még a mentés ELŐTT (új rekord felismeréséhez).
                    double[] prevRec = StrengthLog.recordsFor(this, name);
                    // Szerkesztésnél a helyén cseréljük, az eredeti időpontot megtartva.
                    long ts = edit != null ? edit.ts : System.currentTimeMillis();
                    StrengthLog.Entry ne = new StrengthLog.Entry(ts, name, sets, rpe[0]);
                    // Időbélyeg alapján cserélünk: a lista szűrve is lehet, az index csalna.
                    if (edit != null) StrengthLog.replaceByTs(this, edit.ts, ne);
                    else StrengthLog.add(this, ne);
                    // Az erősítő edzés is számít: a widget azonnal tudjon róla.
                    BlazeWidget.refresh(this);
                    refresh();
                    if (edit != null) {
                        Toast.makeText(this, "Frissítve ✔  (" + sets.size() + " sorozat)",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Új rekord? (Csak ha volt már korábbi bejegyzés, amihez mérhető.)
                        String rec = null;
                        if (prevRec[0] > 0 && ne.topWeight() > prevRec[0])
                            rec = "max súly " + kg(ne.topWeight()) + " kg";
                        else if (prevRec[1] > 0 && ne.bestOneRm() > prevRec[1])
                            rec = "becsült 1RM " + kg(ne.bestOneRm()) + " kg";
                        else if (prevRec[2] > 0 && ne.volume() > prevRec[2])
                            rec = "volumen " + Math.round(ne.volume()) + " kg";
                        if (rec != null) {
                            if (rootFl != null) Confetti.burst(rootFl);
                            Ux.blazeCard(this, "🏆 Új rekord (" + name + "): " + rec + "!  +8 XP");
                        } else {
                            // Új edzésnél Blaze dicsérete + XP, széria-tudatosan.
                            int ds = Streaks.current(this, History.loadAll(this));
                            String praise = ds >= 2
                                    ? ds + " napos széria – ég a láng! 🔥"
                                    : "Blaze büszke rád! 🐺";
                            Ux.blazeCard(this, "Mentve ✔  +8 XP  ·  " + praise);
                        }
                        // A teremben a mentés után rögtön a pihenő jön. Csak
                        // annak indítjuk magától, aki már használta a
                        // pihenő-időzítőt: aki nem, azt ne lepje meg egy
                        // visszaszámlálás.
                        int lastRest = getSharedPreferences("edzo", MODE_PRIVATE)
                                .getInt(REST_SECS_KEY, 0);
                        if (lastRest > 0) startRest(lastRest);
                        // Edzésnapból jöttünk: vissza a listára, hogy látszódjon
                        // a friss pipa és a következő gyakorlat.
                        if (openRoutine != null) {
                            String back = openRoutine;
                            openRoutine = null;
                            routineDaySheet(back);
                        }
                    }
                })
                .addCancel()
                .show();
    }

    /** A kiválasztott RPE-gomb kiemelése (0 = nincs kiválasztva). */
    void styleRpe(Button[] btns, int sel) {
        for (int i = 0; i < btns.length; i++) {
            boolean on = sel == 6 + i;
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(on ? CARD2 : 0x00000000);
            bg.setCornerRadius(dp(13));
            bg.setStroke(dp(1), on ? Theme.accent(this) : LINE);
            btns[i].setBackground(bg);
            btns[i].setTextColor(on ? Theme.accent(this) : MUTED);
        }
    }

    /**
     * „Mai javaslat" kártya: mit érdemes ma nyomni ebből a gyakorlatból az
     * eddigiek alapján. Koppintásra be is írja a sorozatokat, hogy ne kelljen
     * fejben számolni a tárcsákat.
     */
    void showSuggestion(String name, LinearLayout suggestBox, final LinearLayout setsBox,
                        final List<EditText> repsList, final List<EditText> wList) {
        suggestBox.removeAllViews();
        final Progression.Suggestion s = Progression.next(StrengthLog.load(this), name);
        if (s == null) return;              // még sosem naplózta: nincs mihez mérni

        LinearLayout c = card();
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.addView(text("Mai javaslat:  " + s.headline(), 15, Theme.accent(this), true));
        c.addView(gap(4));
        c.addView(text(s.why, 12.5f, MUTED, false));
        // A bemelegítő rámpa ott a leghasznosabb, ahol a mai súly kiderül:
        // különben a felhasználó a javaslattól a kalkulátorig visz fejben egy
        // számot, és ott számolja ki, amit itt is meg lehetett volna mondani.
        if (!s.bodyweight) {
            java.util.List<Warmup.Set> ramp = Warmup.forWork(s.weight, Warmup.barFor(name));
            if (!ramp.isEmpty()) {
                c.addView(gap(8));
                c.addView(text("🔥  Bemelegítés:  " + Warmup.summary(ramp), 12.5f, MUTED, false));
            }
        }
        c.addView(gap(8));
        c.addView(text("Koppints, és beírom a sorozatokat.", 12, MUTED, true));
        c.setClickable(true);
        c.setOnClickListener(v -> applySuggestion(s, setsBox, repsList, wList));

        suggestBox.addView(gap(12));
        suggestBox.addView(c, lp());
    }

    /** A javasolt sorozatok beírása az űrlapba (a meglévő sorok helyére). */
    void applySuggestion(Progression.Suggestion s, LinearLayout setsBox,
                         List<EditText> repsList, List<EditText> wList) {
        setsBox.removeAllViews();
        repsList.clear();
        wList.clear();
        for (int i = 0; i < s.sets; i++) {
            addSetRow(setsBox, repsList, wList, s.timed ? "mp" : "ism.");
            repsList.get(i).setText(String.valueOf(s.reps));
            // Testsúlyosnál a súlymezőt hagyjuk üresen, ne írjunk oda 0-t.
            // Progression.kg (2 tizedes), hogy az 1,25-ös lépés se kerekedjen el.
            if (!s.bodyweight) wList.get(i).setText(Progression.kg(s.weight));
        }
        Toast.makeText(this, "Beírtam: " + s.headline(), Toast.LENGTH_SHORT).show();
    }

    /** A sorozat-mezők feltöltése az adott gyakorlat legutóbbi bejegyzéséből. */
    void prefillFromLast(String name, LinearLayout setsBox, List<EditText> repsList, List<EditText> wList) {
        StrengthLog.Entry last = null;
        for (StrengthLog.Entry e : StrengthLog.load(this)) {   // a lista legújabb elöl
            if (name.equals(e.name)) { last = e; break; }
        }
        setsBox.removeAllViews();
        repsList.clear();
        wList.clear();
        if (last == null || last.sets.isEmpty()) {
            for (int i = 0; i < 3; i++) addSetRow(setsBox, repsList, wList, repsHint(name));
            return;
        }
        for (StrengthLog.SetEntry s : last.sets) {
            addSetRow(setsBox, repsList, wList, repsHint(name));
            int idx = repsList.size() - 1;
            repsList.get(idx).setText(String.valueOf(s.reps));
            wList.get(idx).setText(s.weight > 0 ? fmtKg(s.weight) : "");
        }
    }

    /** A sorozat-blokk felirata: tartásnál másodperc áll az ismétlés helyén. */
    static String setsCaption(String name) {
        return StrengthParse.isTimed(name)
                ? "Sorozatok (másodperc × súly kg)" : "Sorozatok (ismétlés × súly kg)";
    }

    /** A bal oldali mező súgója ugyanezért. */
    static String repsHint(String name) {
        return StrengthParse.isTimed(name) ? "mp" : "ism.";
    }

    void addSetRow(LinearLayout setsBox, List<EditText> repsList, List<EditText> wList) {
        addSetRow(setsBox, repsList, wList, "ism.");
    }

    void addSetRow(LinearLayout setsBox, List<EditText> repsList, List<EditText> wList,
                   String hint) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        EditText reps = numEt(hint);
        reps.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText w = numEt("kg");
        w.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        repsList.add(reps);
        wList.add(w);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1f);
        p1.rightMargin = dp(4);
        TextView x = text("×", 16, MUTED, true);
        x.setPadding(dp(6), 0, dp(6), 0);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1.2f);
        p2.leftMargin = dp(4);
        row.addView(reps, p1);
        row.addView(x);
        row.addView(w, p2);
        setsBox.addView(row);
    }

    EditText numEt(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(MUTED);
        et.setTextColor(TXT);
        et.setTextSize(15);
        et.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2); bg.setCornerRadius(dp(10)); bg.setStroke(dp(1), LINE);
        et.setBackground(bg);
        et.setPadding(dp(10), dp(10), dp(10), dp(10));
        return et;
    }

    int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    double parseDouble(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return 0; }
    }

    /** Súly kiírása: egész, ha kerek (40), egyébként 1 tizedes vesszővel (42,5). */
    static String fmtKg(double w) {
        return Hu.kg(w);
    }

    // ---------- Súlytárcsa-kalkulátor ----------

    void openPlateCalc() {
        LinearLayout box = vbox();
        box.setPadding(dp(10), dp(4), dp(10), 0);

        box.addView(text("Cél súly (kg)", 12.5f, MUTED, false));
        final EditText target = numEt("pl. 60");
        target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(target, lp());
        box.addView(gap(8));

        box.addView(text("Rúd súlya (kg)", 12.5f, MUTED, false));
        final EditText bar = numEt("20");
        bar.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        bar.setText("20");
        box.addView(bar, lp());
        box.addView(gap(12));

        final TextView result = text("", 15, TXT, true);
        result.setGravity(Gravity.CENTER);
        result.setPadding(dp(4), dp(8), dp(4), dp(4));

        Button calc = primary("Számol");
        calc.setOnClickListener(v -> result.setText(
                platePlan(parseDouble(target.getText().toString()),
                        parseDouble(bar.getText().toString()))));
        box.addView(calc);
        box.addView(result);

        new Sheet(this, "Súlytárcsa-kalkulátor", "Mennyi tárcsa kell oldalanként?")
                .addCustom(box).addNeutral("Bezár", () -> {}).show();
    }

    /**
     * Standard tárcsákból (25…1,25 kg) kiszámolja az oldalankénti terhelést.
     *
     * A tárcsák súlyát NEGYED kilós pontossággal írjuk ki: az egy tizedesre
     * kerekítő formázó az 1,25 kg-os tárcsát „1,3 kg"-nak mondta, olyan
     * súlynak, ami nem is létezik.
     */
    static String platePlan(double target, double bar) {
        if (target <= 0) return "Adj meg egy cél súlyt.";
        if (bar < 0) bar = 0;
        if (target < bar) return "A rúd nehezebb a célnál.";
        double perSide = (target - bar) / 2.0;
        double[] plates = {25, 20, 15, 10, 5, 2.5, 1.25};
        StringBuilder sb = new StringBuilder();
        double rem = perSide;
        for (double p : plates) {
            int n = (int) Math.floor(rem / p + 1e-6);
            if (n > 0) {
                if (sb.length() > 0) sb.append("  +  ");
                sb.append(n).append("×").append(Progression.kg(p));
                rem -= n * p;
            }
        }
        String plan = sb.length() == 0 ? "(nincs tárcsa – csak a rúd)" : sb.toString();
        String res = "Oldalanként:\n" + plan;
        if (rem > 0.01) res += "\n(≈ nem jön ki pontosan, marad " + Progression.kg(rem) + " kg/oldal)";
        return res;
    }

    // ---------- Edzésnapok (sablonok) ----------

    /**
     * Melyik edzésnapból nyílt a beviteli űrlap – mentés után oda térünk
     * vissza. Enélkül a lista minden gyakorlat után bezárul, és a felhasználó
     * fejben tartja, hol tartott.
     */
    String openRoutine;

    /** A választható edzésnapok: elöl a sajátok, utánuk a beépítettek. */
    void routineSheet() {
        String stored = Theme.getStr(this, Routines.KEY, "");
        java.util.List<Routines.Routine> all = Routines.all(stored);
        // A heti fókusz megmondja, mi van MA soron – ha van hozzá illő nap,
        // azt jelöljük. Így nem kell fejben összekötni a tervet a sablonnal.
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dow = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7;
        String focus = Weekplan.forDay(Theme.planFocus(this), dow);
        Sheet sh = new Sheet(this, "Edzésnapok",
                focus.isEmpty() ? "Melyik gyakorlatokat csinálod egy szuszra?"
                        : "A mai fókusz: " + focus);
        // „Legutóbb 4 napja": ebből derül ki, melyik nap van soron – a
        // gyakorlatlistából magától nem.
        java.util.List<StrengthLog.Entry> log = StrengthLog.load(this);
        long[] lts = new long[log.size()];
        String[] lnames = new String[log.size()];
        for (int i = 0; i < log.size(); i++) {
            lts[i] = log.get(i).ts;
            lnames[i] = log.get(i).name;
        }
        long now = System.currentTimeMillis();
        // Ha nincs heti fókusz, a rotáció válaszol: a legrégebben csinált nap
        // jön. Enélkül a lista megmutatta, mikor volt melyik, de a „na és most
        // melyik?" kérdés a felhasználóra maradt – a teremben állva.
        String due = focus.isEmpty() ? Routines.nextUp(all, lts, lnames, now) : null;
        for (final Routines.Routine r : all) {
            boolean fits = !focus.isEmpty()
                    ? Foods.norm(r.name).contains(Foods.norm(focus))
                    : r.name.equals(due);
            String when = Routines.lastDoneLabel(Routines.lastDone(r.moves, lts, lnames, now));
            String title = r.label() + (fits ? "  ·  ma ez jön" : "")
                    + (when.isEmpty() ? "" : "  ·  legutóbb " + when);
            sh.addRow(fits ? "🎯" : "📅", title, r.summary(), false, true,
                    () -> routineDaySheet(r.name));
        }
        sh.addRow("＋", "Saját edzésnap", "Név és gyakorlatok – a sajátod elnyomja az "
                + "azonos nevű beépítettet.", false, true, this::newRoutineSheet);
        sh.addCancel().show();
    }

    /**
     * Egy edzésnap gyakorlatai, mindegyik mellett a mai javaslattal.
     *
     * A lényeg a sorrend és a súly egy helyen: eddig minden gyakorlatot külön
     * kellett kikeresni, és a napot fejben tartani.
     */
    void routineDaySheet(String name) {
        String stored = Theme.getStr(this, Routines.KEY, "");
        final Routines.Routine r = Routines.byName(stored, name);
        if (r == null) return;
        java.util.List<StrengthLog.Entry> log = StrengthLog.load(this);
        long today = Days.startOf(System.currentTimeMillis());
        // Ami MA már megvan, azt jelöljük: edzés közben a sablon így nem
        // emlékeztető, hanem lista, amit ki lehet pipálni.
        java.util.LinkedHashMap<String, StrengthLog.Entry> doneToday =
                new java.util.LinkedHashMap<>();
        for (StrengthLog.Entry e : log)
            if (Days.startOf(e.ts) == today && !doneToday.containsKey(e.name))
                doneToday.put(e.name, e);
        int done = 0;
        for (String m : r.moves) if (doneToday.containsKey(m)) done++;
        String head = done == 0 ? "Koppints egy gyakorlatra, és beírom."
                : done >= r.moves.size() ? "🏁  Kész az edzésnap – mind megvan!"
                : done + " / " + r.moves.size() + " megvan ma.";
        Sheet sh = new Sheet(this, r.name, head);
        for (final String m : r.moves) {
            StrengthLog.Entry d = doneToday.get(m);
            if (d != null) {
                String what = d.sets.size() + " sorozat  ·  " + d.totalReps() + " "
                        + StrengthParse.unit(m);
                if (d.topWeight() > 0) what += "  ·  " + Hu.kg(d.topWeight()) + " kg";
                sh.addRow("✔", m, "Ma: " + what, false, true, () -> {
                    openRoutine = r.name;
                    addEntryDialog(null, m);
                });
                continue;
            }
            Progression.Suggestion sg = Progression.next(log, m);
            String sub = sg == null ? "Még nincs mihez mérni – írd be az elsőt."
                    : sg.headline();
            if (sg != null && !sg.bodyweight) {
                java.util.List<Warmup.Set> ramp =
                        Warmup.forWork(sg.weight, Warmup.barFor(m));
                if (!ramp.isEmpty()) sub += "\n🔥 " + Warmup.summary(ramp);
            }
            sh.addRow("🏋", m, sub, false, true, () -> {
                openRoutine = r.name;
                addEntryDialog(null, m);
            });
        }
        // Törölni csak a SAJÁT napot lehet – a byName a beépítetteket is
        // megtalálja, ezért itt a tárolt listát nézzük közvetlenül.
        boolean own = false;
        for (Routines.Routine o : Routines.parse(stored))
            if (o.name.equalsIgnoreCase(r.name)) own = true;
        // Beépített napot nem lehet átírni – de le lehet másolni, és a
        // másolat már a sajátod. Enélkül újra kellene gépelni az egészet.
        if (!own) {
            StringBuilder csv = new StringBuilder();
            for (String m : r.moves) {
                if (csv.length() > 0) csv.append(", ");
                csv.append(m);
            }
            final String moves = csv.toString();
            sh.addRow("📋", "Másolat sajátként", "Ugyanez a nap, szerkeszthetően – "
                    + "a másolat elnyomja a beépítettet.", false, true,
                    () -> newRoutineSheet(r.name, moves));
        }
        if (own)
            sh.addRow("🗑", "Törlöm ezt az edzésnapot", "A beépített változat marad.",
                    false, true, () -> {
                        Theme.setStr(this, Routines.KEY, Routines.remove(
                                Theme.getStr(this, Routines.KEY, ""), r.name));
                        Toast.makeText(this, "Törölve: " + r.name, Toast.LENGTH_SHORT).show();
                        routineSheet();
                    });
        sh.addCancel().show();
    }

    void newRoutineSheet() {
        newRoutineSheet("", "");
    }

    /**
     * Saját edzésnap felvétele: név + vesszővel elválasztott gyakorlatok.
     *
     * @param presetName  előre kitöltött név (beépített nap másolásakor)
     * @param presetMoves előre kitöltött gyakorlatok, vesszővel
     */
    void newRoutineSheet(String presetName, String presetMoves) {
        LinearLayout box = vbox();
        box.setPadding(dp(10), dp(4), dp(10), 0);

        box.addView(text("Az edzésnap neve", 12.5f, MUTED, false));
        final EditText nameEt = new EditText(this);
        nameEt.setHint("pl. Lábnap");
        nameEt.setHintTextColor(MUTED);
        nameEt.setTextColor(TXT);
        nameEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameEt.setText(presetName);
        box.addView(nameEt, lp());
        box.addView(gap(10));

        box.addView(text("Gyakorlatok vesszővel elválasztva", 12.5f, MUTED, false));
        final EditText movesEt = new EditText(this);
        movesEt.setHint("Guggolás, Kitörés, Vádliemelés");
        movesEt.setHintTextColor(MUTED);
        movesEt.setTextColor(TXT);
        movesEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        movesEt.setText(presetMoves);
        movesEt.setSelection(presetMoves.length());
        box.addView(movesEt, lp());
        box.addView(gap(6));
        box.addView(text("Koppints a nevekre, vagy írd be sajátot – a felismerő "
                + "listája nem korlátoz.", 12, MUTED, false));
        box.addView(gap(6));

        // Gyakorlat-csipek: gépelés helyett koppintás. Elöl a saját naplóból
        // ismert nevek, mert azokhoz van már súly és progresszió.
        LinearLayout chips = hbox();
        java.util.List<String> offer = new java.util.ArrayList<>(StrengthLog.knownNames(this));
        for (String n : StrengthParse.names()) if (!offer.contains(n)) offer.add(n);
        for (final String n : offer) {
            Button chip = ghost(n);
            chip.setTextSize(12.5f);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = dp(6);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                String cur = movesEt.getText().toString().trim();
                String next = cur.isEmpty() ? n
                        : cur.endsWith(",") ? cur + " " + n : cur + ", " + n;
                movesEt.setText(next);
                movesEt.setSelection(next.length());
            });
            chips.addView(chip);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(chips);
        box.addView(hs);

        new Sheet(this, "Saját edzésnap", "Legfeljebb " + Routines.MAX_MOVES + " gyakorlat")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    java.util.List<String> moves = new java.util.ArrayList<>();
                    for (String m : movesEt.getText().toString().split(","))
                        if (!m.trim().isEmpty()) moves.add(m.trim());
                    String next = Routines.add(Theme.getStr(this, Routines.KEY, ""),
                            nameEt.getText().toString(), moves);
                    if (next.equals(Theme.getStr(this, Routines.KEY, ""))) {
                        Toast.makeText(this, "Név és legalább egy gyakorlat kell.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Theme.setStr(this, Routines.KEY, next);
                    Toast.makeText(this, "Elmentve.", Toast.LENGTH_SHORT).show();
                    routineSheet();      // a friss lista rögtön látszik
                })
                .addCancel().show();
    }

    // ---------- Bemelegítő rámpa ----------

    /**
     * Bemelegítő sorozatok a mai munkasúlyhoz, felrakható súlyokkal.
     *
     * A rámpa fejben is kiszámolható – csak a végén nem kerek súly jön ki,
     * amit rá lehetne rakni a rúdra. Itt minden lépcső 2,5 kg-os osztáson áll,
     * és a tárcsabontás is ott van mellette, hogy ne kelljen átlépni a másik
     * kalkulátorba.
     */
    void openWarmupCalc() {
        LinearLayout box = vbox();
        box.setPadding(dp(10), dp(4), dp(10), 0);

        box.addView(text("Mai munkasúly (kg)", 12.5f, MUTED, false));
        final EditText work = numEt("pl. 100");
        work.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(work, lp());
        box.addView(gap(8));

        box.addView(text("Rúd súlya (kg) – kézisúlyzónál 0", 12.5f, MUTED, false));
        final EditText bar = numEt("20");
        bar.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        bar.setText("20");
        box.addView(bar, lp());
        box.addView(gap(12));

        final LinearLayout result = vbox();
        Button calc = primary("Számol");
        calc.setOnClickListener(v -> {
            result.removeAllViews();
            double w = parseDouble(work.getText().toString());
            double b = parseDouble(bar.getText().toString());
            java.util.List<Warmup.Set> sets = Warmup.forWork(w, b);
            if (sets.isEmpty()) {
                result.addView(text(w > 0 && w < Warmup.MIN_WORK
                        ? "Ehhez a súlyhoz nem kell külön rámpa – az első sorozat maga "
                          + "a bemelegítés."
                        : "Adj meg egy munkasúlyt (és a rúd súlyát).", 13, MUTED, false));
                return;
            }
            for (int i = 0; i < sets.size(); i++) {
                Warmup.Set st = sets.get(i);
                TextView line = text((i + 1) + ".   " + st.label(), 15, TXT, true);
                line.setPadding(0, dp(6), 0, 0);
                result.addView(line);
                if (b > 0 && st.weight > b) {
                    TextView pl = text(platePlan(st.weight, b).replace("\n", "  "),
                            12, MUTED, false);
                    pl.setPadding(dp(14), dp(2), 0, 0);
                    result.addView(pl);
                }
            }
            TextView last = text("Utána: " + fmtKg(w) + " kg – a munkasorozatok.",
                    12.5f, MUTED, false);
            last.setPadding(0, dp(12), 0, 0);
            result.addView(last);
        });
        box.addView(calc);
        box.addView(gap(6));
        box.addView(result, lp());

        new Sheet(this, "Bemelegítő rámpa", "Mivel melegíts be a mai súlyhoz?")
                .addCustom(box).addNeutral("Bezár", () -> {}).show();
    }

    // ---------- 1RM & százalék kalkulátor ----------

    void openOneRmCalc() {
        LinearLayout box = vbox();
        box.setPadding(dp(10), dp(4), dp(10), 0);

        box.addView(text("Felemelt súly (kg)", 12.5f, MUTED, false));
        final EditText w = numEt("pl. 80");
        w.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(w, lp());
        box.addView(gap(8));

        box.addView(text("Ismétlésszám", 12.5f, MUTED, false));
        final EditText r = numEt("pl. 5");
        r.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(r, lp());
        box.addView(gap(12));

        final TextView result = text("", 14, TXT, false);
        result.setPadding(dp(6), dp(8), dp(6), dp(4));

        Button calc = primary("Számol");
        calc.setOnClickListener(v -> result.setText(
                oneRmPlan(parseDouble(w.getText().toString()), parseInt(r.getText().toString()))));
        box.addView(calc);
        box.addView(result);

        new Sheet(this, "1RM & százalék", "Becsült max és edzéssúlyok")
                .addCustom(box).addNeutral("Bezár", () -> {}).show();
    }

    /** Epley-becslés + a leggyakoribb edzés-százalékok táblázata. */
    static String oneRmPlan(double w, int reps) {
        if (w <= 0 || reps <= 0) return "Adj meg súlyt és ismétlést.";
        double orm = Progression.oneRm(w, reps);
        StringBuilder sb = new StringBuilder();
        sb.append("Becsült 1RM:  ").append(fmtKg(orm)).append(" kg\n\n");
        int[] pct = {95, 90, 85, 80, 75, 70, 65, 60};
        for (int p : pct) sb.append(p).append("%   =   ").append(fmtKg(orm * p / 100.0)).append(" kg\n");
        return sb.toString().trim();
    }

    // ---------- Pihenő-időzítő ----------

    /** A pihenő vége fali órán (0 = nem fut). Így képernyő-eldobás után is folytatható. */
    static final String REST_END_KEY = "rest_end_ms";
    /** A legutóbb választott pihenőhossz másodpercben. */
    static final String REST_SECS_KEY = "rest_secs";

    LinearLayout restCard() {
        LinearLayout card = card();
        LinearLayout inner = vbox();
        inner.setPadding(dp(16), dp(14), dp(16), dp(14));
        inner.addView(text("⏱  Pihenő a sorozatok között", 14, TXT, true));
        inner.addView(gap(10));

        int last = getSharedPreferences("edzo", MODE_PRIVATE).getInt(REST_SECS_KEY, 0);
        LinearLayout row = hbox();
        int[] secs = {60, 90, 120, 180};
        String[] lbl = {"1:00", "1:30", "2:00", "3:00"};
        for (int i = 0; i < secs.length; i++) {
            final int s = secs[i];
            Button b = ghost(lbl[i]);
            b.setTextSize(14);
            // A legutóbb használt hossz kiemelve – a teremben egy koppintás legyen.
            if (s == last) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(CARD2);
                bg.setCornerRadius(dp(13));
                bg.setStroke(dp(1), Theme.accent(this));
                b.setBackground(bg);
                b.setTextColor(Theme.accent(this));
            }
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.leftMargin = dp(3); p.rightMargin = dp(3);
            b.setLayoutParams(p);
            b.setOnClickListener(v -> startRest(s));
            row.addView(b);
        }
        inner.addView(row);

        // Javaslat a legutóbbi sorozat ismétlésszámából: a nehéz sorozat más
        // pihenőt kíván, mint a tömegépítő sáv.
        List<StrengthLog.Entry> log = StrengthLog.load(this);
        if (!log.isEmpty() && log.get(0).sets != null && !log.get(0).sets.isEmpty()) {
            StrengthLog.SetEntry lastSet = log.get(0).sets.get(log.get(0).sets.size() - 1);
            boolean timed = StrengthParse.isTimed(log.get(0).name);
            int sug = Progression.restSeconds(lastSet.reps, lastSet.weight <= 0, timed);
            TextView hint = text("Javaslat a legutóbbi sorozatod alapján: "
                    + (sug / 60) + ":" + String.format(Locale.US, "%02d", sug % 60)
                    + "  ·  " + Progression.restWhy(lastSet.reps, timed), 11.5f, MUTED, false);
            hint.setPadding(0, dp(8), 0, 0);
            hint.setClickable(true);
            hint.setOnClickListener(v -> startRest(sug));
            inner.addView(hint);
        }

        restText = text("", 24, Theme.accent(this), true);
        restText.setGravity(Gravity.CENTER);
        restText.setPadding(0, dp(12), 0, 0);
        restText.setVisibility(View.GONE);
        restText.setClickable(true);
        restText.setOnClickListener(v -> stopRest());   // koppintással megszakítható
        inner.addView(restText);

        card.addView(inner);
        return card;
    }

    void startRest(int secs) {
        getSharedPreferences("edzo", MODE_PRIVATE).edit()
                .putInt(REST_SECS_KEY, secs)
                .putLong(REST_END_KEY, System.currentTimeMillis() + secs * 1000L)
                .apply();
        resumeRest();
    }

    /**
     * A mentett pihenő folytatása. Fali órával számolunk, nem a képernyőn
     * eltelt idővel: a teremben a telefon lezár, az Android eldobhatja a
     * képernyőt – visszatéréskor az időzítő ott folytatja, ahol tart.
     */
    void resumeRest() {
        long end = getSharedPreferences("edzo", MODE_PRIVATE).getLong(REST_END_KEY, 0);
        if (end <= System.currentTimeMillis()) { stopRest(); return; }
        restEnd = end;
        if (restTick == null) {
            restTick = () -> {
                if (restText == null) return;
                long left = restEnd - System.currentTimeMillis();
                if (left <= 0) {
                    restText.setText("Pihenő letelt! 💪  (koppints)");
                    getSharedPreferences("edzo", MODE_PRIVATE).edit()
                            .remove(REST_END_KEY).apply();
                    restEndBeep();
                    return;
                }
                int s = (int) Math.ceil(left / 1000.0);
                restText.setText("⏱  " + (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60)
                        + "   (koppints a leállításhoz)");
                restHandler.postDelayed(restTick, 200);
            };
        }
        restHandler.removeCallbacks(restTick);
        restText.setVisibility(View.VISIBLE);
        restHandler.post(restTick);
    }

    void stopRest() {
        if (restTick != null) restHandler.removeCallbacks(restTick);
        getSharedPreferences("edzo", MODE_PRIVATE).edit().remove(REST_END_KEY).apply();
        if (restText != null) restText.setVisibility(View.GONE);
    }

    void restEndBeep() {
        try { Beeper.finish(); } catch (Exception ignored) {}
        if (!Theme.vibrate(this)) return;
        try {
            Vibrator vb = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vb != null && vb.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26)
                    vb.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE));
                else vb.vibrate(450);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ha a képernyő elhagyása (vagy eldobása) közben járt a pihenő, itt
        // veszi fel újra a fonalat – a fali óra alapján, tehát pontosan.
        resumeRest();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (restTick != null) restHandler.removeCallbacks(restTick);
    }

    // ---------- UI segédek ----------

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
