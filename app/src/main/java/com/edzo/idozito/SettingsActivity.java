package com.edzo.idozito;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Testreszabás: színek, hangerő, rezgés, visszaszámlálás hossza, sebesség-egység.
 */
public class SettingsActivity extends Activity {

    static final int BG = MainActivity.BG, CARD = MainActivity.CARD, CARD2 = MainActivity.CARD2;
    static final int TXT = MainActivity.TXT, MUTED = MainActivity.MUTED, LINE = MainActivity.LINE;

    TextView volLabel;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Beállítások", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Szabd testre a megjelenést és a hangokat.", 13, MUTED, false));
        col.addView(gap(20));

        // --- Színek ---
        LinearLayout colors = card();
        colors.addView(colorRow("Akcentszín", "c_accent", Theme.DEF_ACCENT));
        colors.addView(divider());
        colors.addView(colorRow("Futás színe", "c_work", Theme.DEF_WORK));
        colors.addView(divider());
        colors.addView(colorRow("Pihenő színe", "c_rest", Theme.DEF_REST));
        col.addView(colors, lp());
        col.addView(gap(18));

        // --- Hang / rezgés ---
        LinearLayout audio = card();
        // Hangerő
        LinearLayout volBox = vbox();
        volBox.setPadding(dp(18), dp(14), dp(18), dp(14));
        LinearLayout volHead = hbox();
        volHead.addView(text("Sípszó hangereje", 15.5f, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
        volLabel = text(Math.round(Theme.volume(this) * 100) + "%", 14, accent(), true);
        volHead.addView(volLabel);
        volBox.addView(volHead);
        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(Math.round(Theme.volume(this) * 100));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                Beeper.masterVolume = p / 100f;
                volLabel.setText(p + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                Theme.setFloat(SettingsActivity.this, "volume", s.getProgress() / 100f);
                Beeper.tick();
            }
        });
        volBox.addView(sb);
        audio.addView(volBox);
        audio.addView(divider());
        // Rezgés
        Switch vib = new Switch(this);
        vib.setChecked(Theme.vibrate(this));
        audio.addView(switchRow("Rezgés", vib));
        vib.setOnCheckedChangeListener((btn, c) -> Theme.setBool(this, "vibrate", c));
        col.addView(audio, lp());
        col.addView(gap(18));

        // --- Visszaszámlálás hossza ---
        col.addView(text("Visszaszámláló csipogás hossza", 15.5f, TXT, true));
        col.addView(gap(8));
        col.addView(intChips(new int[]{0, 3, 5, 10},
                new String[]{"Nincs", "3 mp", "5 mp", "10 mp"}, "cd_secs", 3), lp());
        col.addView(gap(18));

        // --- Sebesség egység ---
        col.addView(text("Sebesség kijelzése", 15.5f, TXT, true));
        col.addView(gap(8));
        col.addView(paceChips(), lp());
        col.addView(gap(18));

        // --- Értesítések és adatok ---
        LinearLayout notif = card();
        Switch recap = new Switch(this);
        recap.setChecked(Theme.recapEnabled(this));
        notif.addView(switchRow("Heti visszatekintő értesítés", recap));
        recap.setOnCheckedChangeListener((btn, c) -> {
            Theme.setBool(this, "recap", c);
            WeeklyReceiver.schedule(this);
        });
        notif.addView(divider());
        Switch live = new Switch(this);
        live.setChecked(Theme.liveBg(this));
        notif.addView(switchRow("Élő háttér-animáció", live));
        live.setOnCheckedChangeListener((btn, c) -> { Theme.setBool(this, "livebg", c); recreate(); });
        notif.addView(divider());
        Switch duck = new Switch(this);
        duck.setChecked(Theme.duckMusic(this));
        notif.addView(switchRow("Zene halkítása edzés közben", duck));
        duck.setOnCheckedChangeListener((btn, c) -> Theme.setBool(this, "duck", c));
        col.addView(notif, lp());
        col.addView(gap(14));

        Button export = ghost("📤  Előzmények exportálása (CSV)");
        export.setOnClickListener(v -> exportCsv());
        col.addView(export);
        col.addView(gap(24));

        Button reset = ghost("↺  Alaphelyzet");
        reset.setOnClickListener(v -> new Sheet(this, "Alaphelyzet", "Visszaállítod az alapértelmezett beállításokat?")
                .addDestructive("Visszaállítás", () -> { Theme.resetAll(this); Beeper.masterVolume = Theme.volume(this); recreate(); })
                .addCancel().show());
        col.addView(reset);

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffold(this, sv, "bg_main"));
        col.post(() -> Ux.enterChildren(col, 30, 45));
    }

    // ---------------- CSV export ----------------

    void exportCsv() {
        JSONArray h = History.load(this);
        if (h.length() == 0) {
            Toast.makeText(this, "Nincs elmentett edzés.", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("datum;tipus;ido_mp;tav_m;korok;atlag_kmh;max_kmh;kaloria;lepesek\n");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o == null) continue;
            String name = o.optString("name", "");
            String type = (name.isEmpty() ? "Futas" : name).replace(';', ',');
            double dist = o.optDouble("dist", -1);
            int dur = o.optInt("dur");
            double avg = o.optDouble("avgspeed", -1);
            if (avg < 0 && dist > 0 && dur > 0) avg = dist / dur * 3.6;
            double mx = o.optDouble("maxspeed", -1);
            sb.append(df.format(new Date(o.optLong("ts")))).append(';')
              .append(type).append(';')
              .append(dur).append(';')
              .append(dist >= 0 ? String.valueOf(Math.round(dist)) : "").append(';')
              .append(o.optInt("rounds")).append(';')
              .append(avg > 0 ? String.format(Locale.US, "%.2f", avg) : "").append(';')
              .append(mx > 0 ? String.format(Locale.US, "%.2f", mx) : "").append(';')
              .append(Math.round(o.optDouble("cal", 0))).append(';')
              .append(o.optInt("steps", 0)).append('\n');
        }
        ShareProvider.shareTextFile(this, sb.toString(), "my_trainer_elozmenyek.csv", "text/csv");
    }

    // ---------------- Színsor ----------------

    View colorRow(String title, final String key, int def) {
        LinearLayout box = vbox();
        box.setPadding(dp(18), dp(14), dp(18), dp(14));
        box.addView(text(title, 15.5f, TXT, true));
        box.addView(gap(10));
        LinearLayout row = hbox();
        final View[] sw = new View[Theme.SWATCHES.length];
        final int cur = getSharedPreferences("edzo", MODE_PRIVATE).getInt(key, def);
        for (int i = 0; i < Theme.SWATCHES.length; i++) {
            final int color = Theme.SWATCHES[i];
            View v = new View(this);
            v.setBackground(swatchBg(color, color == cur));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
            lp.rightMargin = dp(8);
            v.setOnClickListener(view -> {
                Theme.setInt(this, key, color);
                for (int j = 0; j < sw.length; j++) sw[j].setBackground(swatchBg(Theme.SWATCHES[j], Theme.SWATCHES[j] == color));
            });
            row.addView(v, lp);
            sw[i] = v;
        }
        box.addView(row);
        box.setLayoutParams(lp());
        return box;
    }

    GradientDrawable swatchBg(int color, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        if (selected) g.setStroke(dp(3), 0xFFFFFFFF);
        else g.setStroke(dp(1), LINE);
        return g;
    }

    // ---------------- Chip-sorok ----------------

    View intChips(final int[] values, String[] labels, final String key, int def) {
        LinearLayout row = hbox();
        final Button[] btns = new Button[values.length];
        final int cur = getSharedPreferences("edzo", MODE_PRIVATE).getInt(key, def);
        for (int i = 0; i < values.length; i++) {
            final int val = values[i];
            Button b = chip(labels[i], val == cur);
            b.setOnClickListener(v -> {
                Theme.setInt(this, key, val);
                for (int j = 0; j < btns.length; j++) styleChip(btns[j], values[j] == val);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.leftMargin = dp(4); lp.rightMargin = dp(4);
            row.addView(b, lp);
            btns[i] = b;
        }
        return row;
    }

    View paceChips() {
        LinearLayout row = hbox();
        final boolean pace = Theme.paceMode(this);
        final Button[] btns = new Button[2];
        String[] labels = {"km/h", "perc/km"};
        for (int i = 0; i < 2; i++) {
            final boolean isPace = i == 1;
            Button b = chip(labels[i], pace == isPace);
            b.setOnClickListener(v -> {
                Theme.setBool(this, "pace", isPace);
                styleChip(btns[0], !isPace);
                styleChip(btns[1], isPace);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.leftMargin = dp(4); lp.rightMargin = dp(4);
            row.addView(b, lp);
            btns[i] = b;
        }
        return row;
    }

    Button chip(String label, boolean selected) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextSize(14);
        b.setPadding(dp(8), dp(12), dp(8), dp(12));
        b.setStateListAnimator(null);
        styleChip(b, selected);
        return b;
    }

    void styleChip(Button b, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(13));
        if (selected) { bg.setColor(accent()); b.setTextColor(0xFFFFFFFF); }
        else { bg.setColor(CARD2); bg.setStroke(dp(1), LINE); b.setTextColor(TXT); }
        b.setBackground(bg);
    }

    // ---------------- Segéd UI ----------------

    int accent() { return Theme.accent(this); }

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6121A33);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), 0x33FFFFFF);
        c.setBackground(bg);
        return c;
    }

    LinearLayout switchRow(String title, Switch sw) {
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        row.addView(text(title, 15.5f, TXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(sw);
        row.setLayoutParams(lp());
        return row;
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

    Button ghost(String label) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextColor(TXT);
        b.setTypeface(null, Typeface.BOLD); b.setTextSize(15);
        b.setPadding(dp(14), dp(14), dp(14), dp(14));
        b.setStateListAnimator(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2); bg.setCornerRadius(dp(13)); bg.setStroke(dp(1), LINE);
        b.setBackground(bg);
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return b;
    }

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
