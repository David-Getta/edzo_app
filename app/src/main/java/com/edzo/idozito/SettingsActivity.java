package com.edzo.idozito;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
        col.addView(gap(10));
        // Bemondás sebessége (edzés közbeni hangutasítások).
        col.addView(text("Bemondás sebessége", 13, MUTED, false));
        col.addView(gap(6));
        col.addView(speechRateChips(), lp());
        col.addView(gap(10));
        // Azonnali próba: így hangzik majd a bemondás edzés közben.
        Button testVoice = ghost("🔊  Teszt bemondás");
        testVoice.setOnClickListener(v -> testSpeak());
        col.addView(testVoice);
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

        // --- Edzésnapok terve ---
        col.addView(text("📅 Edzésnapok", 15.5f, TXT, true));
        col.addView(gap(4));
        col.addView(text("Jelöld ki, mely napokon tervezel edzeni – Blaze csak ezeken a "
                + "napokon emlékeztet. Üresen hagyva minden nap edzésnap.", 12.5f, MUTED, false));
        col.addView(gap(8));
        col.addView(planDayChips(), lp());
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
        Switch blaze = new Switch(this);
        blaze.setChecked(Theme.blazeNudge(this));
        notif.addView(switchRow("🐺 Blaze napi biztatása", blaze));
        blaze.setOnCheckedChangeListener((btn, c) -> {
            Theme.setBool(this, "blaze_nudge", c);
            DailyNudgeReceiver.schedule(this);
        });
        // Blaze értesítésének időpontja (azonnal újraütemez).
        TextView nudgeCap = text("Blaze mikor szóljon?", 12.5f, MUTED, false);
        nudgeCap.setPadding(dp(14), dp(2), dp(14), dp(6));
        notif.addView(nudgeCap);
        LinearLayout hourWrap = vbox();
        hourWrap.setPadding(dp(8), 0, dp(8), dp(10));
        hourWrap.addView(nudgeHourChips());
        notif.addView(hourWrap);
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
        notif.addView(switchRow("Díszítő animációk (beúszás, lüktetés, konfetti)", anim));
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

        Button exportMeals = ghost("🍽  Étrend-napló exportálása (CSV)");
        exportMeals.setOnClickListener(v -> exportMealsCsv());
        col.addView(exportMeals);
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
                Backup.exportJson(this), "grit_mentes.json", "application/json"));
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

        col.addView(gap(10));
        // Frissítés-ellenőrzés: a legújabb kiadás oldala (Obtainium magától is frissít).
        Button updates = ghost("⬆️  Frissítések keresése");
        updates.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/David-Getta/edzo_app/releases/latest")));
            } catch (Exception ignored) {}
        });
        col.addView(updates);

        col.addView(gap(10));
        Button news = ghost("🆕  Újdonságok");
        news.setOnClickListener(v -> whatsNewSheet());
        col.addView(news);

        // App-verzió kijelzése (a telepített csomagból olvasva)
        String ver = "";
        long vc = 0;
        try {
            android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            ver = pi.versionName;
            vc = pi.versionCode;
        } catch (Exception ignored) {}
        TextView verLabel = text("Grit" + (ver == null || ver.isEmpty() ? "" : "  ·  v" + ver)
                + (vc > 1000 ? "  ·  build " + (vc - 1000) : ""), 12, MUTED, false);
        verLabel.setGravity(Gravity.CENTER);
        verLabel.setPadding(0, dp(20), 0, dp(4));
        col.addView(verLabel);

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffoldNav(this, sv, "bg_settings", 5));
        col.post(() -> Ux.enterChildren(col, 30, 45));
    }

    /** A legutóbbi fejlesztések rövid listája. */
    void whatsNewSheet() {
        String[][] items = {
            {"🍽", "Étrend-napló", "Írd be, mit ettél – kcal és fehérje magától számolva, fotóval (kamera vagy galéria) és okos elosztással."},
            {"⚖️", "Mennyiség a szövegből", "„150 g csirkemell 200 g rizs\" és „2 tojás\" – a grammot és a darabszámot is kiolvassuk."},
            {"★", "Kedvencek", "Bármely étkezés kedvencnek jelölhető, és mindig elöl lesz a gyors csipek közt."},
            {"🖊", "Saját ételek", "Vedd fel a saját ételeid a kalóriatáblázatba – a felismerés is megtalálja őket."},
            {"📖", "Kalóriatáblázat", "Élőben szűrhető lista, és egy koppintással naplózható bármelyik étel."},
            {"💧", "Vízszámláló", "Pohár-alapú vízkövetés céllal, widget-gyorsgombbal és Hidratált jelvénnyel."},
            {"🎯", "Új kihívások", "Fehérje-cél, vízcél és étkezés-naplózás típusú napi kihívások."},
            {"⭐", "XP az étrendért", "A nap első étkezés-bejegyzése +5 XP-t ad a szintedhez."},
            {"📊", "Étrend-statisztika", "7 napos átlagok, 30 napos csík, cél-tartás és napi részletek."},
            {"🔍", "Keresés", "Keresés az étrend- és az erősítő naplóban is."},
            {"📤", "Megosztás", "Napi étrend és heti összefoglaló egy koppintással megosztható."},
        };
        Sheet sh = new Sheet(this, "Újdonságok 🆕", "A legutóbbi fejlesztések");
        for (String[] it : items)
            sh.addRow(it[0], it[1], it[2], false, true, () -> {});
        sh.addCancel().show();
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
        ShareProvider.shareTextFile(this, sb.toString(), "grit_elozmenyek.csv", "text/csv");
    }

    void exportMealsCsv() {
        // Saját másolat: a naplót lentebb sorba rendezzük, a megosztott
        // (gyorsítótárazott) listát viszont nem szabad átrendezni.
        java.util.List<MealLog.Meal> meals = new java.util.ArrayList<>(MealLog.load(this));
        if (meals.isEmpty()) {
            Toast.makeText(this, "Nincs étrend-bejegyzés.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Időrendben (legrégebbi elöl), hogy a napi bontás olvasható legyen.
        java.util.Collections.sort(meals, (a, b) -> Long.compare(a.ts, b.ts));
        StringBuilder sb = new StringBuilder();
        sb.append("datum;etkezes;osszetevo;gramm;kcal;feherje_g;viz_l\n");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        SimpleDateFormat dOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        // A víz napi adat, nem étkezésenkénti: minden nap első sorába írjuk.
        java.util.HashSet<String> waterWritten = new java.util.HashSet<>();
        for (MealLog.Meal m : meals) {
            String when = df.format(new Date(m.ts));
            String day = dOnly.format(new Date(m.ts));
            String name = (m.name.isEmpty() ? "-" : m.name).replace(';', ',');
            for (MealLog.Item it : m.items) {
                String water = "";
                if (waterWritten.add(day)) {
                    int cl = Water.clOn(this, m.ts);
                    if (cl > 0) water = String.format(Locale.US, "%.2f", cl / 100.0);
                }
                sb.append(when).append(';')
                  .append(name).append(';')
                  .append(it.food.replace(';', ',')).append(';')
                  .append(Math.round(it.grams)).append(';')
                  .append(Math.round(it.kcal)).append(';')
                  .append(it.protein > 0 ? String.valueOf(Math.round(it.protein)) : "").append(';')
                  .append(water).append('\n');
            }
        }
        ShareProvider.shareTextFile(this, sb.toString(), "grit_etrend.csv", "text/csv");
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
        ShareProvider.shareTextFile(this, sb.toString(), "grit_erosito.csv", "text/csv");
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
                // A visszaállítás FELÜLÍRJA az emlékeztetőket. A régiek riasztásai
                // viszont a rendszerben maradnának: egy olyan emlékeztető, ami a
                // mentésben már nincs benne, továbbra is szólna – akár újraindításig.
                // Ezért a mostaniakat még az importálás előtt eltesszük, és utána
                // lemondjuk őket.
                java.util.List<Reminders.Reminder> previous = Reminders.load(this);
                if (Backup.importJson(this, json)) {
                    Beeper.masterVolume = Theme.volume(this);
                    for (Reminders.Reminder r : previous) Reminders.cancelOne(this, r);
                    Reminders.scheduleAll(this);
                    WeeklyReceiver.schedule(this);
                    DailyNudgeReceiver.schedule(this);
                    BlazeWidget.refresh(this);
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

    /** Tervezett edzésnapok (többes kijelölés, 0=hétfő .. 6=vasárnap). */
    View planDayChips() {
        String[] labels = {"H", "K", "Sze", "Cs", "P", "Szo", "V"};
        LinearLayout row = hbox();
        final Button[] btns = new Button[7];
        final java.util.Set<String> sel = new java.util.HashSet<>();
        String cur = Theme.planDays(this);
        if (!cur.isEmpty()) for (String d : cur.split(",")) if (!d.isEmpty()) sel.add(d);
        for (int i = 0; i < 7; i++) {
            final String id = String.valueOf(i);
            Button b = chip(labels[i], sel.contains(id));
            b.setOnClickListener(v -> {
                if (sel.contains(id)) sel.remove(id); else sel.add(id);
                StringBuilder sb = new StringBuilder();
                for (int d = 0; d < 7; d++)
                    if (sel.contains(String.valueOf(d))) {
                        if (sb.length() > 0) sb.append(',');
                        sb.append(d);
                    }
                // Közvetlen mentés rev-bump nélkül: nem kell újraépíteni a UI-t,
                // a chip stílusát helyben frissítjük.
                getSharedPreferences("edzo", MODE_PRIVATE).edit()
                        .putString("plan_days", sb.toString()).apply();
                styleChip((Button) v, sel.contains(id));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.leftMargin = dp(2); lp.rightMargin = dp(2);
            row.addView(b, lp);
            btns[i] = b;
        }
        return row;
    }

    // ---- Teszt bemondás (ugyanazzal a motorral és sebességgel, mint edzésnél) ----

    private TextToSpeech testTts;
    private boolean testTriedGoogle;

    void testSpeak() {
        releaseTestTts();
        testTriedGoogle = true;
        try {
            testTts = new TextToSpeech(this, this::onTestTtsInit, "com.google.android.tts");
        } catch (Exception e) {
            testTriedGoogle = false;
            try { testTts = new TextToSpeech(this, this::onTestTtsInit); } catch (Exception ignored) {}
        }
    }

    private void onTestTtsInit(int status) {
        if (status != TextToSpeech.SUCCESS || testTts == null) {
            // A Google motor nem elérhető – egyszer újrapróbáljuk az alapértelmezettel.
            if (testTriedGoogle) {
                testTriedGoogle = false;
                releaseTestTts();
                try { testTts = new TextToSpeech(this, this::onTestTtsInit); } catch (Exception ignored) {}
            }
            return;
        }
        try {
            testTts.setLanguage(new Locale("hu", "HU"));
            testTts.setSpeechRate(Theme.speechRate(this));
            testTts.speak("Szia! Így fogok beszélni edzés közben. Hajrá, csináljuk!",
                    TextToSpeech.QUEUE_FLUSH, null, "grit_test");
        } catch (Exception ignored) {}
    }

    private void releaseTestTts() {
        if (testTts != null) {
            try { testTts.stop(); testTts.shutdown(); } catch (Exception ignored) {}
            testTts = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseTestTts();
    }

    /** Hangbemondás sebessége (a következő edzésnél lép életbe). */
    View speechRateChips() {
        final float[] rates = {0.85f, 0.96f, 1.15f};
        String[] labels = {"Lassú", "Normál", "Gyors"};
        LinearLayout row = hbox();
        final Button[] btns = new Button[rates.length];
        final float cur = Theme.speechRate(this);
        for (int i = 0; i < rates.length; i++) {
            final float val = rates[i];
            Button b = chip(labels[i], Math.abs(val - cur) < 0.01f);
            b.setOnClickListener(v -> {
                Theme.setFloat(this, "tts_rate", val);
                for (int j = 0; j < btns.length; j++) styleChip(btns[j], Math.abs(rates[j] - val) < 0.01f);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.leftMargin = dp(4); lp.rightMargin = dp(4);
            row.addView(b, lp);
            btns[i] = b;
        }
        return row;
    }

    /** Blaze napi értesítésének órája – választás után azonnal újraütemez. */
    View nudgeHourChips() {
        final int[] hours = {8, 12, 18, 20, 21};
        String[] labels = {"8:00", "12:00", "18:00", "20:00", "21:00"};
        LinearLayout row = hbox();
        final Button[] btns = new Button[hours.length];
        final int cur = Theme.nudgeHour(this);
        for (int i = 0; i < hours.length; i++) {
            final int val = hours[i];
            Button b = chip(labels[i], val == cur);
            b.setOnClickListener(v -> {
                Theme.setInt(this, "blaze_hour", val);
                DailyNudgeReceiver.schedule(this);
                for (int j = 0; j < btns.length; j++) styleChip(btns[j], hours[j] == val);
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
