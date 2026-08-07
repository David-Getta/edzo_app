package com.edzo.idozito;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.List;
import java.util.Locale;

/**
 * Emlékeztetők (Duolingo-stílusú napi értesítések) kezelése: hozzáadás
 * időponttal és saját szöveggel, gyors javaslatok, ki/be kapcsolás, törlés.
 */
public class RemindersActivity extends Activity {

    static int BG, CARD, CARD2, TXT, MUTED, LINE;

    static final String[] SUGGESTIONS = {
            "Ideje edzeni! 💪",
            "Mozogj egy kicsit! 🏃",
            "Igyál egy pohár vizet 💧",
            "Ne nassolj, tartsd a célod! 🥗",
            "Nyújts, állj fel egy percre 🧘",
            "Menj egy sétára! 🚶",
            "Kihívás-idő! Nézd meg a mai célod 🎯",
            "Esti nyújtás – jobb alvás 🌙🧘",
            "A falka számít rád – egy rövid kör? 🐺🔥",
    };

    LinearLayout listBox;
    LinearLayout notifWarnBox;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); BG=MainActivity.BG; CARD=MainActivity.CARD; CARD2=MainActivity.CARD2; TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; LINE=MainActivity.LINE;
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Emlékeztetők", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Napi értesítések – szabd testre az időt és a szöveget.", 13, MUTED, false));
        col.addView(gap(20));

        // Ha az értesítések ki vannak kapcsolva, minden alábbi beállítás
        // hatástalan – ezt jobb rögtön a lap tetején megmondani, mint hagyni,
        // hogy a felhasználó soha meg nem szólaló emlékeztetőket állítson be.
        notifWarnBox = vbox();
        col.addView(notifWarnBox, lp());

        // Blaze automatikus napi biztatása – itt is látszik, hogy mikor szól
        // (kikapcsolva is megjelenik, hogy könnyű legyen megtalálni).
        boolean blazeOn = Theme.blazeNudge(this);
        LinearLayout blazeCard = card();
        blazeCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        blazeCard.addView(text(blazeOn
                ? String.format(Locale.US, "🐺 Blaze napi biztatása  ·  %02d:00", Theme.nudgeHour(this))
                : "🐺 Blaze napi biztatása  ·  kikapcsolva", 14.5f, TXT, true));
        TextView bSub = text(blazeOn
                ? "Edzésnapokon magától szól, ha aznap még nem edzettél. Időpont és ki/be a Beállításokban."
                : "Kapcsold be a Beállításokban, hogy Blaze edzésnapokon magától biztasson.", 12.5f, MUTED, false);
        bSub.setPadding(0, dp(4), 0, 0);
        blazeCard.addView(bSub);
        blazeCard.setClickable(true);
        blazeCard.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, SettingsActivity.class)));
        col.addView(blazeCard, lp());
        col.addView(gap(14));

        Button add = primary("＋  Új emlékeztető");
        add.setOnClickListener(v -> addOrEdit(null, ""));
        col.addView(add);
        col.addView(gap(18));

        col.addView(text("Gyors javaslatok", 15.5f, TXT, true));
        col.addView(gap(10));
        LinearLayout sug = vbox();
        // A kitűzött rehab-fókusz a legszemélyesebb javaslat – az kerül előre.
        java.util.List<String> suggestions =
                new java.util.ArrayList<>(java.util.Arrays.asList(SUGGESTIONS));
        String rfid = RehabLog.focusId(this);
        Rehab.Area rfa = rfid == null ? null : Rehab.byId(rfid);
        if (rfa != null) suggestions.add(0, "🩹 " + rfa.name + " – 10 perc megelőzés");
        for (String s : suggestions) {
            Button chip = ghost(s);
            chip.setOnClickListener(v -> addOrEdit(null, s));
            sug.addView(chip);
            sug.addView(gap(8));
        }
        col.addView(sug, lp());
        col.addView(gap(14));

        col.addView(text("Beállított emlékeztetők", 15.5f, TXT, true));
        col.addView(gap(10));
        listBox = vbox();
        col.addView(listBox, lp());

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_reminders", -1));
        col.post(() -> Ux.enterChildren(col, 30, 45));
        refresh();
    }

    void refresh() {
        listBox.removeAllViews();
        List<Reminders.Reminder> list = Reminders.load(this);
        if (list.isEmpty()) {
            listBox.addView(text("Még nincs emlékeztető. Adj hozzá egyet fent.", 13.5f, MUTED, false));
            return;
        }
        for (final Reminders.Reminder r : list) {
            LinearLayout card = card();
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(12), dp(12));

            LinearLayout left = vbox();
            left.addView(text(String.format(Locale.US, "%02d:%02d", r.h, r.m), 20, TXT, true));
            left.addView(text(r.text.isEmpty() ? "(nincs szöveg)" : r.text, 13.5f, MUTED, false));
            left.addView(text(r.daysLabel(), 12, Theme.accent(this), false));
            left.setClickable(true);
            left.setOnClickListener(v -> addOrEdit(r, null));
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));

            Switch sw = new Switch(this);
            sw.setChecked(r.on);
            sw.setOnCheckedChangeListener((btn, on) -> {
                r.on = on;
                List<Reminders.Reminder> all = Reminders.load(this);
                for (Reminders.Reminder x : all) if (x.id == r.id) x.on = on;
                Reminders.save(this, all);
                Reminders.scheduleOne(this, r);
            });
            row.addView(sw);

            Button del = new Button(this);
            del.setText("🗑");
            del.setAllCaps(false);
            del.setTextSize(16);
            del.setBackground(null);
            del.setStateListAnimator(null);
            del.setTextColor(MUTED);
            del.setOnClickListener(v -> {
                Reminders.cancelOne(this, r);
                List<Reminders.Reminder> all = Reminders.load(this);
                for (int i = all.size() - 1; i >= 0; i--) if (all.get(i).id == r.id) all.remove(i);
                Reminders.save(this, all);
                refresh();
            });
            row.addView(del);

            card.addView(row);
            listBox.addView(card, lp());
            listBox.addView(gap(10));
        }
    }

    void addOrEdit(final Reminders.Reminder existing, String presetText) {
        LinearLayout box = vbox();
        box.setPadding(dp(12), dp(8), dp(12), 0);
        final TimePicker tp = new TimePicker(this);
        tp.setIs24HourView(true);
        int h = existing != null ? existing.h : 18;
        int m = existing != null ? existing.m : 0;
        tp.setHour(h);
        tp.setMinute(m);
        final EditText et = new EditText(this);
        et.setHint("Üzenet (pl. Ideje mozogni!)");
        et.setHintTextColor(MUTED);
        et.setTextColor(TXT);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setText(existing != null ? existing.text : (presetText == null ? "" : presetText));
        box.addView(tp);
        box.addView(et);

        // Napválasztó: melyik napokon szóljon? Alapból minden nap.
        final int[] mask = {existing != null ? existing.days : 0};
        box.addView(gap(10));
        box.addView(text("Mely napokon?", 13, MUTED, true));
        box.addView(gap(6));
        final TextView daysInfo = text(Reminders.daysLabel(mask[0]), 12.5f, Theme.accent(this), true);
        final Button[] dayChips = new Button[7];
        LinearLayout dayRow = hbox();
        for (int i = 0; i < 7; i++) {
            final int bit = 1 << i;
            Button b = new Button(this);
            b.setText(Reminders.DAY_ABBR[i]);
            b.setAllCaps(false);
            b.setTextSize(12.5f);
            b.setPadding(0, dp(6), 0, dp(6));
            b.setStateListAnimator(null);
            dayChips[i] = b;
            b.setOnClickListener(v -> {
                int cur = mask[0] <= 0 || mask[0] >= 127 ? 127 : mask[0];
                cur ^= bit;
                if (cur == 0) cur = 127;               // legalább egy nap kell
                mask[0] = cur == 127 ? 0 : cur;
                styleDayChips(dayChips, mask[0]);
                daysInfo.setText(Reminders.daysLabel(mask[0]));
            });
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, -2, 1f);
            dlp.leftMargin = dp(2); dlp.rightMargin = dp(2);
            dayRow.addView(b, dlp);
        }
        styleDayChips(dayChips, mask[0]);
        box.addView(dayRow, lp());
        box.addView(gap(4));
        box.addView(daysInfo, lp());

        new Sheet(this, existing == null ? "Új emlékeztető" : "Emlékeztető szerkesztése")
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    int nh = tp.getHour(), nm = tp.getMinute();
                    String txt = et.getText().toString().trim();
                    List<Reminders.Reminder> all = Reminders.load(this);
                    Reminders.Reminder target;
                    if (existing == null) {
                        target = new Reminders.Reminder(Reminders.nextId(this), nh, nm, txt,
                                true, mask[0]);
                        all.add(target);
                    } else {
                        target = null;
                        for (Reminders.Reminder x : all) if (x.id == existing.id) {
                            x.h = nh; x.m = nm; x.text = txt; x.days = mask[0]; target = x;
                        }
                    }
                    Reminders.save(this, all);
                    if (target != null) Reminders.scheduleOne(this, target);
                    refresh();
                })
                .addCancel()
                .show();
    }

    /** A kiválasztott napok kiemelve; „minden nap" esetén mind az. */
    void styleDayChips(Button[] chips, int mask) {
        int use = mask <= 0 || mask >= 127 ? 127 : mask;
        for (int i = 0; i < chips.length; i++) {
            boolean on = (use & (1 << i)) != 0;
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(10));
            if (on) { bg.setColor(Theme.accent(this)); chips[i].setTextColor(0xFFFFFFFF); }
            else { bg.setColor(0x22FFFFFF); chips[i].setTextColor(MUTED); }
            chips[i].setBackground(bg);
        }
    }

    // ---------- UI helpers ----------

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

    @Override
    protected void onResume() {
        super.onResume();
        // A rendszerbeállításokból visszatérve azonnal frissüljön az állapot.
        refreshNotifWarning();
    }

    /** Figyelmeztető kártya, ha az értesítések le vannak tiltva. */
    void refreshNotifWarning() {
        if (notifWarnBox == null) return;
        notifWarnBox.removeAllViews();
        if (Notifs.enabled(this)) return;

        LinearLayout c = card();
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.addView(text("🔕  Az értesítések ki vannak kapcsolva", 14.5f, 0xFFE23B3B, true));
        TextView sub = text("Amíg így marad, egyetlen emlékeztető sem fog megszólalni – "
                + "sem a napi biztatás, sem a heti visszatekintő.", 12.5f, MUTED, false);
        sub.setPadding(0, dp(4), 0, dp(10));
        c.addView(sub);
        Button open = ghost("Bekapcsolom a beállításokban");
        open.setTextSize(13.5f);
        open.setOnClickListener(v -> Notifs.openSettings(this));
        c.addView(open);
        notifWarnBox.addView(c, lp());
        notifWarnBox.addView(gap(14));
    }
}
