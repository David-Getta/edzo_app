package com.edzo.idozito;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Egy edzés részletes nézete: összegző mérőszámok, GPS-útvonal rajz, km-es
 * splitek és sebesség-görbe.
 */
public class WorkoutDetailActivity extends Activity {

    static final int BG = MainActivity.BG, CARD = MainActivity.CARD, CARD2 = MainActivity.CARD2;
    static final int TXT = MainActivity.TXT, MUTED = MainActivity.MUTED, LINE = MainActivity.LINE;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        long ts = getIntent().getLongExtra("ts", 0);
        JSONObject e = findEntry(ts);
        JSONArray track = SessionStore.loadTrack(this, ts);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setFillViewport(true);
        LinearLayout col = vbox();
        col.setPadding(dp(20), dp(20), dp(20), dp(36));

        if (e == null) {
            col.addView(text("Az edzés nem található.", 15, MUTED, false));
            sv.addView(col);
            setContentView(Ux.scaffold(this, sv, "bg_main"));
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd  HH:mm", new Locale("hu"));
        col.addView(text("Edzés részletei", 22, TXT, true));
        col.addView(gap(4));
        String wname = e.optString("name", "");
        col.addView(text(wname.isEmpty() ? "🏃 Futás (intervallum)" : "🏋️ " + wname, 14.5f, Theme.accent(this), true));
        col.addView(gap(2));
        col.addView(text(df.format(new Date(e.optLong("ts"))), 13.5f, MUTED, false));
        String moodE = History.moodEmoji(e.optInt("mood", 0));
        if (!moodE.isEmpty()) {
            String[] ml = {"", "Nehéz volt", "Rendben ment", "Jó volt", "Szuper volt"};
            int mv = e.optInt("mood", 0);
            col.addView(gap(4));
            col.addView(text(moodE + "  " + (mv >= 1 && mv <= 4 ? ml[mv] : ""), 14, TXT, true));
        }
        String note = e.optString("note", "");
        if (!note.isEmpty()) {
            col.addView(gap(8));
            LinearLayout noteCard = card();
            noteCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            noteCard.addView(text("📝 Jegyzet", 12, MUTED, true));
            TextView nt = text(note, 14.5f, TXT, false);
            nt.setPadding(0, dp(6), 0, 0);
            noteCard.addView(nt);
            col.addView(noteCard, lp());
        }
        final int curMoodVal = e.optInt("mood", 0);
        final String curNoteVal = e.optString("note", "");
        TextView editJournal = text("✏️  Napló szerkesztése (hangulat / jegyzet)", 13, Theme.accent(this), true);
        editJournal.setPadding(0, dp(10), 0, 0);
        editJournal.setClickable(true);
        editJournal.setOnClickListener(v -> editJournalSheet(ts, curMoodVal, curNoteVal));
        col.addView(editJournal);
        col.addView(gap(18));

        // ---- Összegzés ----
        int dur = e.optInt("dur");
        double dist = e.optDouble("dist", -1);
        int moving = e.optInt("moving", 0);
        double maxKmh = e.optDouble("maxspeed", -1);
        int steps = e.optInt("steps", 0);
        double elev = e.optDouble("elev", 0);
        double cal = e.optDouble("cal", 0);
        int rounds = e.optInt("rounds", 0);

        double avgKmh = e.optDouble("avgspeed", -1);
        if (avgKmh < 0 && dist > 0 && dur > 0) avgKmh = dist / dur * 3.6;
        double cadence = (steps > 0 && moving > 0) ? steps / (moving / 60.0) : -1;

        LinearLayout grid = card();
        grid.setPadding(dp(6), dp(6), dp(6), dp(6));
        addTiles(grid, new String[][]{
                {"⏱ Idő", fmtDur(dur)},
                {"🏃 Mozgásidő", moving > 0 ? fmtDur(moving) : "—"},
                {"📍 Táv", dist >= 0 ? fmtDist(dist) : "—"},
                {"🔥 Kalória", Math.round(cal) + " kcal"},
                {"⚡ Átlag", avgKmh >= 0 ? fmtSpeed(avgKmh) : "—"},
                {"🚀 Max", maxKmh >= 0 ? fmtSpeed(maxKmh) : "—"},
                {"👟 Lépések", steps > 0 ? String.valueOf(steps) : "—"},
                {"🎵 Kadencia", cadence >= 0 ? Math.round(cadence) + " /min" : "—"},
                {"⛰ Emelkedő", dist >= 0 ? Math.round(elev) + " m" : "—"},
                {"🔁 Körök", String.valueOf(rounds)},
        });
        col.addView(grid, lp());
        col.addView(gap(12));

        // ---- Megosztás ----
        android.widget.Button share = new android.widget.Button(this);
        share.setText("📤 Edzés megosztása");
        share.setAllCaps(false);
        share.setTextColor(TXT);
        share.setTypeface(null, Typeface.BOLD);
        share.setTextSize(15);
        share.setStateListAnimator(null);
        share.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(CARD2); sbg.setCornerRadius(dp(15)); sbg.setStroke(dp(1), LINE);
        share.setBackground(sbg);
        final JSONObject entry = e;
        share.setOnClickListener(v -> new Sheet(this, "Edzés megosztása")
                .addRow("🖼️", "Kép megosztása", "Látványos összegző kártya", false, true,
                        () -> shareWorkoutImage(entry))
                .addRow("✍️", "Szöveg megosztása", "Rövid összefoglaló szövegben", false, true,
                        () -> shareWorkout(entry))
                .addCancel()
                .show());
        col.addView(share, lp());
        col.addView(gap(18));

        // ---- Útvonal ----
        double[][] latlon = extractLatLon(track);
        if (latlon != null && latlon[0].length >= 2) {
            col.addView(text("Útvonal", 17, TXT, true));
            col.addView(gap(10));
            LinearLayout routeCard = card();
            routeCard.setPadding(dp(10), dp(10), dp(10), dp(10));
            RouteView rv = new RouteView(this);
            rv.setData(latlon[0], latlon[1]);
            routeCard.addView(rv, new LinearLayout.LayoutParams(-1, dp(220)));
            col.addView(routeCard, lp());
            col.addView(gap(18));
        }

        // ---- Splitek (km) ----
        String[] splits = computeSplits(track);
        if (splits.length > 0) {
            col.addView(text("Körök kilométerenként", 17, TXT, true));
            col.addView(gap(10));
            LinearLayout splitCard = card();
            for (int i = 0; i < splits.length; i++) {
                LinearLayout row = hbox();
                row.setPadding(dp(16), dp(11), dp(16), dp(11));
                row.addView(text((i + 1) + ". km", 14.5f, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1f));
                row.addView(text(splits[i], 15.5f, TXT, true));
                splitCard.addView(row);
                if (i < splits.length - 1) {
                    View dv = new View(this);
                    LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(-1, dp(1));
                    dvp.leftMargin = dp(16); dvp.rightMargin = dp(16);
                    dv.setLayoutParams(dvp); dv.setBackgroundColor(LINE);
                    splitCard.addView(dv);
                }
            }
            col.addView(splitCard, lp());
            col.addView(gap(18));
        }

        // ---- Sebesség-görbe ----
        double[] spd = extractSpeed(track);
        if (spd != null && spd.length >= 2) {
            col.addView(text("Sebesség (km/h)", 17, TXT, true));
            col.addView(gap(10));
            LinearLayout chartCard = card();
            chartCard.setPadding(dp(8), dp(12), dp(12), dp(10));
            ProfileActivity.ChartView chart = new ProfileActivity.ChartView(this);
            chart.setData(spd, Theme.accent(this), "km/h");
            chartCard.addView(chart, new LinearLayout.LayoutParams(-1, dp(180)));
            col.addView(chartCard, lp());
        }

        if ((latlon == null || latlon[0].length < 2)) {
            col.addView(text("Ehhez az edzéshez nincs GPS-adat (a táv-mérés kikapcsolva volt).",
                    12.5f, MUTED, false));
        }

        sv.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -2));
        setContentView(Ux.scaffold(this, sv, "bg_main"));
        col.post(() -> Ux.enterChildren(col, 30, 42));
    }

    /** Látványos cyber összegző kártya képként megosztva. */
    void shareWorkoutImage(JSONObject e) {
        try {
            Bitmap bmp = renderShareCard(e);
            ShareProvider.shareImage(this, bmp, "edzes_" + e.optLong("ts"));
        } catch (Exception ex) {
            shareWorkout(e); // vészmegoldás: szöveg
        }
    }

    Bitmap renderShareCard(JSONObject e) {
        final int W = 1080, H = 1350, M = 80;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, W, H, 0xFF070912, 0xFF0C1024, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, W, H, p);
        p.setShader(null);

        // felső akcentcsík (cián → magenta)
        Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        bar.setShader(new LinearGradient(M, 0, W - M, 0, 0xFF22E0FF, 0xFFFF3DDB, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(M, 120, W - M, 134), 8, 8, bar);
        bar.setShader(null);

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xFFEAF6FF);
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tp.setTextSize(78);
        cv.drawText("My trainer", M, 250, tp);

        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setColor(0xFF8AA0C4);
        sp.setTextSize(38);
        String wname = e.optString("name", "");
        String type = wname.isEmpty() ? "Futás" : wname;
        String moodE = History.moodEmoji(e.optInt("mood", 0));
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd  HH:mm", new Locale("hu"));
        cv.drawText(type + "  ·  " + df.format(new Date(e.optLong("ts")))
                + (moodE.isEmpty() ? "" : "  " + moodE), M, 312, sp);

        // értékek
        int dur = e.optInt("dur");
        double dist = e.optDouble("dist", -1);
        double avg = e.optDouble("avgspeed", -1);
        if (avg < 0 && dist > 0 && dur > 0) avg = dist / dur * 3.6;
        double mx = e.optDouble("maxspeed", -1);
        int steps = e.optInt("steps", 0);
        double cal = e.optDouble("cal", 0);
        int rounds = e.optInt("rounds", 0);

        String[][] tiles = {
                {"Idő", fmtDur(dur)},
                {"Táv", dist >= 0 ? fmtDist(dist) : "—"},
                {"Átlag", avg > 0 ? fmtSpeed(avg) : "—"},
                {"Max", mx > 0 ? fmtSpeed(mx) : "—"},
                {"Kalória", Math.round(cal) + " kcal"},
                {steps > 0 ? "Lépések" : "Körök", steps > 0 ? String.valueOf(steps) : String.valueOf(rounds)},
        };

        int gap = 28, cols = 2;
        int cardW = (W - 2 * M - gap) / cols;
        int cardH = 200;
        int startY = 380;
        Paint cardBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBg.setColor(0x14FFFFFF);
        Paint val = new Paint(Paint.ANTI_ALIAS_FLAG);
        val.setColor(0xFFFFFFFF);
        val.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        val.setTextSize(58);
        Paint lab = new Paint(Paint.ANTI_ALIAS_FLAG);
        lab.setColor(0xFF8AA0C4);
        lab.setTextSize(34);
        for (int i = 0; i < tiles.length; i++) {
            int cx = M + (i % cols) * (cardW + gap);
            int cy = startY + (i / cols) * (cardH + gap);
            cv.drawRoundRect(new RectF(cx, cy, cx + cardW, cy + cardH), 28, 28, cardBg);
            cv.drawText(tiles[i][1], cx + 34, cy + 96, val);
            cv.drawText(tiles[i][0], cx + 34, cy + 150, lab);
        }

        // lábléc
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFF22E0FF);
        fp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        fp.setTextSize(38);
        fp.setTextAlign(Paint.Align.CENTER);
        cv.drawText("MY TRAINER  ·  edzésnapló", W / 2f, H - 70, fp);
        return bmp;
    }

    /** Az edzés összefoglalója szövegként, bármely appba küldhető (üzenet, közösségi média...). */
    void shareWorkout(JSONObject e) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm", new Locale("hu"));
        int dur = e.optInt("dur");
        double dist = e.optDouble("dist", -1);
        double avg = e.optDouble("avgspeed", -1);
        if (avg < 0 && dist > 0 && dur > 0) avg = dist / dur * 3.6;
        double mx = e.optDouble("maxspeed", -1);
        int steps = e.optInt("steps", 0);
        double cal = e.optDouble("cal", 0);
        int rounds = e.optInt("rounds", 0);

        StringBuilder sb = new StringBuilder();
        sb.append("🏃 My trainer – edzés · ").append(df.format(new Date(e.optLong("ts")))).append("\n");
        String nm = e.optString("name", "");
        if (!nm.isEmpty()) sb.append("🏋️ ").append(nm).append("\n");
        sb.append("⏱ Idő: ").append(fmtDur(dur));
        if (dist >= 0) sb.append("  ·  📍 Táv: ").append(fmtDist(dist));
        sb.append("\n");
        if (avg > 0) {
            sb.append("⚡ Átlag: ").append(fmtSpeed(avg));
            if (mx > 0) sb.append("  ·  🚀 Max: ").append(fmtSpeed(mx));
            sb.append("\n");
        }
        if (cal > 0) sb.append("🔥 ").append(Math.round(cal)).append(" kcal");
        if (steps > 0) sb.append("  ·  👟 ").append(steps).append(" lépés");
        if (rounds > 0) sb.append("  ·  🔁 ").append(rounds).append(" kör");

        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
        try {
            startActivity(android.content.Intent.createChooser(i, "Edzés megosztása"));
        } catch (Exception ignored) {}
    }

    JSONObject findEntry(long ts) {
        JSONArray arr = History.load(this);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optLong("ts") == ts) return o;
        }
        return null;
    }

    // track pont: [tSec, lat, lon, alt, dist, speed]

    double[][] extractLatLon(JSONArray track) {
        int n = track.length();
        if (n == 0) return null;
        double[] la = new double[n], lo = new double[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            JSONArray p = track.optJSONArray(i);
            if (p == null || p.length() < 3) continue;
            double lat = p.optDouble(1, 0), lon = p.optDouble(2, 0);
            if (lat == 0 && lon == 0) continue;
            la[k] = lat; lo[k] = lon; k++;
        }
        if (k == 0) return null;
        double[] laa = new double[k], loo = new double[k];
        System.arraycopy(la, 0, laa, 0, k);
        System.arraycopy(lo, 0, loo, 0, k);
        return new double[][]{laa, loo};
    }

    double[] extractSpeed(JSONArray track) {
        int n = track.length();
        if (n == 0) return null;
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            JSONArray p = track.optJSONArray(i);
            s[i] = p != null && p.length() >= 6 ? p.optDouble(5, 0) * 3.6 : 0;
        }
        return s;
    }

    /** Km-es splitek m:ss formában, lineáris interpolációval a (dist, t) mintákból. */
    String[] computeSplits(JSONArray track) {
        int n = track.length();
        if (n < 2) return new String[0];
        double maxDist = 0;
        for (int i = 0; i < n; i++) {
            JSONArray p = track.optJSONArray(i);
            if (p != null && p.length() >= 5) maxDist = Math.max(maxDist, p.optDouble(4, 0));
        }
        int kms = (int) (maxDist / 1000.0);
        if (kms < 1) return new String[0];
        String[] out = new String[kms];
        double prevT = 0;
        for (int km = 1; km <= kms; km++) {
            double targetD = km * 1000.0;
            double t = timeAtDist(track, targetD);
            double split = t - prevT;
            prevT = t;
            int sec = (int) Math.round(split);
            out[km - 1] = fmtDur(sec) + "  (" + paceStr(split) + ")";
        }
        return out;
    }

    double timeAtDist(JSONArray track, double targetD) {
        double prevD = 0, prevT = 0;
        for (int i = 0; i < track.length(); i++) {
            JSONArray p = track.optJSONArray(i);
            if (p == null || p.length() < 5) continue;
            double d = p.optDouble(4, 0), t = p.optDouble(0, 0);
            if (d >= targetD) {
                if (d - prevD <= 0) return t;
                double f = (targetD - prevD) / (d - prevD);
                return prevT + (t - prevT) * f;
            }
            prevD = d; prevT = t;
        }
        return prevT;
    }

    String paceStr(double secPerKm) {
        if (secPerKm <= 0) return "–";
        int m = (int) (secPerKm / 60);
        int s = (int) Math.round(secPerKm - m * 60);
        if (s == 60) { m++; s = 0; }
        return String.format(Locale.US, "%d:%02d /km", m, s);
    }

    // ---------------- Format ----------------

    String fmtDur(int sec) {
        if (sec < 0) sec = 0;
        int h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s)
                : String.format(Locale.US, "%d:%02d", m, s);
    }
    String fmtDist(double m) {
        if (m < 0) return "—";
        if (m < 1000) return Math.round(m) + " m";
        return String.format(Locale.US, "%.2f km", m / 1000.0);
    }
    String fmtSpeed(double kmh) {
        if (Theme.paceMode(this)) return paceStr(kmh > 0 ? 3600.0 / kmh : 0);
        return String.format(Locale.US, "%.1f km/h", kmh);
    }

    // ---------------- UI segéd ----------------

    void addTiles(LinearLayout grid, String[][] items) {
        for (int i = 0; i < items.length; i += 2) {
            LinearLayout row = hbox();
            row.addView(tile(items[i][0], items[i][1]), tileLp());
            if (i + 1 < items.length) row.addView(tile(items[i + 1][0], items[i + 1][1]), tileLp());
            else row.addView(new View(this), tileLp());
            grid.addView(row, lp());
        }
    }

    LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.leftMargin = dp(4); p.rightMargin = dp(4); p.topMargin = dp(4); p.bottomMargin = dp(4);
        return p;
    }

    View tile(String label, String value) {
        LinearLayout t = vbox();
        t.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD2); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), LINE);
        t.setBackground(bg);
        t.addView(text(value, 18, TXT, true));
        t.addView(text(label, 12, MUTED, false));
        return t;
    }

    // Hangulat + jegyzet utólagos szerkesztése egy adott edzéshez.
    void editJournalSheet(long ts, int curMood, String curNote) {
        final int[] sel = { curMood };
        LinearLayout box = vbox();
        box.setPadding(dp(4), 0, dp(4), 0);
        box.addView(text("Milyen volt?", 13, MainActivity.MUTED, false));
        LinearLayout moodRow = hbox();
        moodRow.setGravity(Gravity.CENTER);
        moodRow.setPadding(0, dp(6), 0, dp(6));
        String[] emo = {"😣", "😐", "🙂", "💪"};
        final TextView[] chips = new TextView[4];
        for (int m = 0; m < 4; m++) {
            final int mood = m + 1;
            TextView c = text(emo[m], 26, MainActivity.TXT, false);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(10), dp(4), dp(10), dp(4));
            c.setAlpha(curMood == 0 || curMood == mood ? 1f : 0.35f);
            c.setClickable(true);
            c.setOnClickListener(v -> {
                sel[0] = mood;
                for (int k = 0; k < 4; k++) chips[k].setAlpha(k == mood - 1 ? 1f : 0.35f);
            });
            chips[m] = c;
            moodRow.addView(c, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        box.addView(moodRow);
        final EditText et = new EditText(this);
        et.setHint("Jegyzet…");
        et.setText(curNote);
        et.setTextColor(MainActivity.TXT);
        et.setHintTextColor(MainActivity.MUTED);
        et.setTextSize(15);
        et.setMinLines(3);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), MainActivity.GLASS_LINE);
        et.setBackground(bg);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(-1, -2);
        etLp.topMargin = dp(10);
        box.addView(et, etLp);

        new Sheet(this, "Napló szerkesztése 📝", null)
                .addCustom(box)
                .addPrimary("Mentés", () -> {
                    if (sel[0] >= 1) History.updateByTs(this, ts, "mood", sel[0]);
                    History.updateByTs(this, ts, "note", et.getText().toString().trim());
                    Toast.makeText(this, "Elmentve 📝", Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .addCancel()
                .show();
    }

    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    LinearLayout.LayoutParams lp() { return new LinearLayout.LayoutParams(-1, -2); }

    LinearLayout card() {
        LinearLayout c = vbox();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6121A33); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), 0x33FFFFFF);
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

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    // ---------------- Útvonal nézet ----------------

    static class RouteView extends View {
        private double[] lat, lon;
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint start = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint end = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float density;

        RouteView(Context c) {
            super(c);
            density = c.getResources().getDisplayMetrics().density;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setStrokeWidth(density * 3.5f);
            line.setColor(Theme.accent(c));
            start.setColor(0xFF22C55E);
            end.setColor(0xFFEF4444);
        }

        void setData(double[] lat, double[] lon) { this.lat = lat; this.lon = lon; invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            if (lat == null || lat.length < 2) return;
            double minLa = lat[0], maxLa = lat[0], minLo = lon[0], maxLo = lon[0];
            for (int i = 0; i < lat.length; i++) {
                minLa = Math.min(minLa, lat[i]); maxLa = Math.max(maxLa, lat[i]);
                minLo = Math.min(minLo, lon[i]); maxLo = Math.max(maxLo, lon[i]);
            }
            double midLa = (minLa + maxLa) / 2;
            double spanLo = Math.max(1e-6, (maxLo - minLo) * Math.cos(Math.toRadians(midLa)));
            double spanLa = Math.max(1e-6, maxLa - minLa);
            float pad = density * 16;
            float w = getWidth() - 2 * pad, h = getHeight() - 2 * pad;
            double scale = Math.min(w / spanLo, h / spanLa);
            float drawW = (float) (spanLo * scale), drawH = (float) (spanLa * scale);
            float offX = pad + (w - drawW) / 2, offY = pad + (h - drawH) / 2;

            path.reset();
            float sx = 0, sy = 0, ex = 0, ey = 0;
            for (int i = 0; i < lat.length; i++) {
                float x = offX + (float) (((lon[i] - minLo) * Math.cos(Math.toRadians(midLa))) * scale);
                float y = offY + (float) ((maxLa - lat[i]) * scale);
                if (i == 0) { path.moveTo(x, y); sx = x; sy = y; }
                else path.lineTo(x, y);
                ex = x; ey = y;
            }
            canvas.drawPath(path, line);
            canvas.drawCircle(sx, sy, density * 5, start);
            canvas.drawCircle(ex, ey, density * 5, end);
        }
    }
}
