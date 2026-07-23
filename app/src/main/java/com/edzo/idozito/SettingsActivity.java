package com.edzo.idozito;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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

    static int BG, CARD, CARD2, TXT, MUTED, LINE;

    TextView volLabel;
    static final int REQ_IMPORT = 4201;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.applyPalette(this); BG=MainActivity.BG; CARD=MainActivity.CARD; CARD2=MainActivity.CARD2; TXT=MainActivity.TXT; MUTED=MainActivity.MUTED; LINE=MainActivity.LINE;
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        col.addView(text("Beállítások", 22, TXT, true));
        col.addView(gap(4));
        col.addView(text("Szabd testre a megjelenést és a hangokat.", 13, MUTED, false));
        col.addView(gap(20));

        // --- Megjelenés: koppintható mód-váltó (nem be/ki kapcsoló) ---
        final boolean isLight = Theme.light(this);
        Button modeBtn = ghost((isLight ? "☀️  Világos mód" : "🌙  Sötét mód") + "   ·   koppints a váltáshoz");
        modeBtn.setOnClickListener(v -> { Theme.setBool(this, "lightmode", !Theme.light(this)); recreate(); });
        col.addView(modeBtn);
        col.addView(gap(18));

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

        // --- Hangbemondás minősége (rendszer TTS) ---
        col.addView(text("🗣️  Magyar hang minősége", 15.5f, TXT, true));
        col.addView(gap(4));
        col.addView(text("Az edzés közbeni bemondás a rendszer felolvasó motorját használja. "
                + "A legszebb magyar hangért érdemes a Google felolvasót választani, és a "
                + "magyar hangot letölteni (Beszéd → Nyelv → Magyar).", 12.5f, MUTED, false));
        col.addView(gap(10));
        Button ttsBtn = ghost("⚙️  Felolvasó (TTS) beállításai");
        ttsBtn.setOnClickListener(v -> {
            // A rendszer felolvasó-beállításai; ha nem nyílik meg, a hangbeállítások.
            try {
                startActivity(new Intent("com.android.settings.TTS_SETTINGS")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                try { startActivity(new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)); }
                catch (Exception e2) {
                    Toast.makeText(this, "A beállítás nem érhető el ezen a készüléken.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        col.addView(ttsBtn);
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
        notif.addView(divider());
        Switch screenOn = new Switch(this);
        screenOn.setChecked(Theme.keepScreenOn(this));
        notif.addView(switchRow("Képernyő ébren tartása edzés közben", screenOn));
        screenOn.setOnCheckedChangeListener((btn, c) -> Theme.setBool(this, "screenon", c));
        notif.addView(divider());
        Switch anim = new Switch(this);
        anim.setChecked(Theme.animEnabled(this));
        notif.addView(switchRow("Díszítő animációk (lüktetés, konfetti)", anim));
        anim.setOnCheckedChangeListener((btn, c) -> { Theme.setBool(this, "anim", c); recreate(); });
        col.addView(notif, lp());
        col.addView(gap(14));

        Button export = ghost("📤  Előzmények exportálása (CSV)");
        export.setOnClickListener(v -> exportCsv());
        col.addView(export);
        col.addView(gap(10));

        Button exportStr = ghost("🏋️  Erősítő napló exportálása (CSV)");
        exportStr.setOnClickListener(v -> exportStrengthCsv());
        col.addView(exportStr);
        col.addView(gap(10));

        col.addView(text("☁️  Automatikus mentés (Google-fiók)", 15.5f, TXT, true));
        col.addView(gap(4));
        col.addView(text("Az adataid (előzmények, erősítő napló, beállítások, programok) "
                + "automatikusan mentődnek a Google-fiókodba. Új eszközön, UGYANAZZAL a "
                + "Google-fiókkal telepítve a rendszer magától visszatölti őket – bejelentkezés "
                + "nélkül. Ehhez a telefonon bekapcsolt Google biztonsági mentés kell "
                + "(Beállítások › Google › Biztonsági mentés). Kézi mentésként az alábbi fájl is használható.",
                12.5f, MUTED, false));
        col.addView(gap(14));

        Button backup = ghost("💾  Kézi mentés fájlba");
        backup.setOnClickListener(v -> ShareProvider.shareTextFile(this,
                Backup.exportJson(this), "my_trainer_mentes.json", "application/json"));
        col.addView(backup);
        col.addView(gap(10));

        Button restore = ghost("📥  Visszaállítás fájlból");
        restore.setOnClickListener(v -> new Sheet(this, "Visszaállítás",
                "Ez FELÜLÍRJA a jelenlegi adataidat (előzmények, beállítások, programok). Folytatod?")
                .addPrimary("Fájl kiválasztása", this::pickBackupFile)
                .addCancel().show());
        col.addView(restore);
        col.addView(gap(24));

        Button reset = ghost("↺  Alaphelyzet");
        reset.setOnClickListener(v -> new Sheet(this, "Alaphelyzet", "Visszaállítod az alapértelmezett beállításokat?")
                .addDestructive("Visszaállítás", () -> { Theme.resetAll(this); Beeper.masterVolume = Theme.volume(this); recreate(); })
                .addCancel().show());
        col.addView(reset);

        // Az app ajánlása – megosztja a telepítő linket (Obtainium/GitHub Release).
        Button shareApp = ghost("📣  Ajánld egy barátnak");
        shareApp.setOnClickListener(v -> {
            try {
                Intent s = new Intent(Intent.ACTION_SEND);
                s.setType("text/plain");
                s.putExtra(Intent.EXTRA_SUBJECT, "Grit – edzőtárs app");
                s.putExtra(Intent.EXTRA_TEXT,
                        "Ezt az ingyenes edzőtárs appot használom (HIIT időzítő, futáskövetés, "
                        + "edzésnapló): https://github.com/David-Getta/edzo_app\n\n"
                        + "Telepítés Obtainiummal (magától frissül) vagy a legújabb APK-val: "
                        + "https://github.com/David-Getta/edzo_app/releases/latest");
                startActivity(Intent.createChooser(s, "Megosztás"));
            } catch (Exception ignored) {}
        });
        col.addView(shareApp);

        // App-verzió kijelzése (a telepített csomagból olvasva)
        String ver = "";
        try { ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) {}
        TextView verLabel = text("Grit" + (ver.isEmpty() ? "" : "  ·  v" + ver), 12, MUTED, false);
        verLabel.setGravity(Gravity.CENTER);
        verLabel.setPadding(0, dp(20), 0, dp(4));
        col.addView(verLabel);

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_settings", 4));
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

    void exportStrengthCsv() {
        java.util.List<StrengthLog.Entry> list = StrengthLog.load(this);
        if (list.isEmpty()) {
            Toast.makeText(this, "Nincs erősítő bejegyzés.", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("datum;gyakorlat;sorozat;ismetles;suly_kg;volumen_kg\n");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (StrengthLog.Entry e : list) {
            String d = df.format(new Date(e.ts));
            String name = e.name == null ? "" : e.name.replace(';', ',');
            int setNo = 1;
            for (StrengthLog.SetEntry s : e.sets) {
                sb.append(d).append(';')
                  .append(name).append(';')
                  .append(setNo++).append(';')
                  .append(s.reps).append(';')
                  .append(String.format(Locale.US, "%.1f", s.weight)).append(';')
                  .append(String.format(Locale.US, "%.1f", s.reps * s.weight)).append('\n');
            }
        }
        ShareProvider.shareTextFile(this, sb.toString(), "my_trainer_erosito.csv", "text/csv");
    }

    // ---------------- Biztonsági mentés / visszaállítás ----------------

    void pickBackupFile() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, REQ_IMPORT);
        } catch (Exception e) {
            Toast.makeText(this, "Nem található fájlkezelő.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_IMPORT && res == RESULT_OK && data != null && data.getData() != null) {
            try {
                String json = readAll(data.getData());
                if (Backup.importJson(this, json)) {
                    Beeper.masterVolume = Theme.volume(this);
                    Reminders.scheduleAll(this);
                    WeeklyReceiver.schedule(this);
                    Theme.bumpRev(this); // hogy a főképernyő is újraépüljön
                    Toast.makeText(this, "Visszaállítva. 👍", Toast.LENGTH_LONG).show();
                    recreate();
                } else {
                    Toast.makeText(this, "Ez nem Grit mentésfájl.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Nem sikerült beolvasni a fájlt.", Toast.LENGTH_LONG).show();
            }
        }
    }

    String readAll(Uri uri) throws Exception {
        java.io.InputStream is = getContentResolver().openInputStream(uri);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return new String(bos.toByteArray(), "UTF-8");
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
        bg.setColor(MainActivity.GLASS);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), MainActivity.GLASS_LINE);
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
