package com.edzo.idozito;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Egy edzés részletes adatai (GPS-útvonal, mintavételezett táv/sebesség/magasság)
 * a filesDir-ben, időbélyeg szerinti fájlban. A History csak az összegzést tárolja,
 * így a lista gyors marad, a részletet csak igény szerint töltjük be.
 */
public final class SessionStore {

    private SessionStore() {}

    static File file(Context c, long ts) {
        File dir = new File(c.getFilesDir(), "sessions");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "s_" + ts + ".json");
    }

    /** track: pontonként [tSec, lat, lon, altM, distM, speedMps]. */
    public static void save(Context c, long ts, JSONArray track) {
        try {
            JSONObject o = new JSONObject();
            o.put("track", track);
            byte[] data = o.toString().getBytes("UTF-8");
            FileOutputStream fos = new FileOutputStream(file(c, ts));
            fos.write(data);
            fos.close();
        } catch (Exception ignored) {}
    }

    public static JSONArray loadTrack(Context c, long ts) {
        try {
            File f = file(c, ts);
            if (!f.exists()) return new JSONArray();
            String json = readAll(f);
            if (json.isEmpty()) return new JSONArray();
            JSONObject o = new JSONObject(json);
            return o.optJSONArray("track") != null ? o.optJSONArray("track") : new JSONArray();
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    /**
     * A teljes fájl beolvasása. Egyetlen read() hívás nem garantáltan tölti fel
     * a puffert: egy hosszabb futás GPS-nyoma több száz kilobájt is lehet, és a
     * félig beolvasott JSON csendben értelmezhetetlen lenne – vagyis az egész
     * útvonal eltűnne a részletek képernyőjéről.
     */
    private static String readAll(File f) throws java.io.IOException {
        java.io.FileInputStream fis = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = fis.read(chunk)) > 0) buf.write(chunk, 0, n);
            return buf.toString("UTF-8");
        } finally {
            try { fis.close(); } catch (Exception ignored) {}
        }
    }

    public static void delete(Context c, long ts) {
        try { File f = file(c, ts); if (f.exists()) f.delete(); } catch (Exception ignored) {}
    }

    static final String PREFIX = "s_", SUFFIX = ".json";

    /**
     * Egy részletfájl nevéből az edzés időbélyege, vagy -1, ha nem ilyen fájl.
     * Külön kiemelve, mert erre épül a takarítás: egy elrontott felismerés
     * MEGLÉVŐ edzés útvonalát törölné.
     */
    static long tsOfFile(String name) {
        if (name == null || !name.startsWith(PREFIX) || !name.endsWith(SUFFIX)) return -1;
        String mid = name.substring(PREFIX.length(), name.length() - SUFFIX.length());
        if (mid.isEmpty()) return -1;
        for (int i = 0; i < mid.length(); i++)
            if (mid.charAt(i) < '0' || mid.charAt(i) > '9') return -1;
        try { return Long.parseLong(mid); } catch (NumberFormatException e) { return -1; }
    }

    /**
     * Gazdátlan részletfájlok törlése: amelyikhez már nincs edzés az
     * előzményekben. A naplóból törölt futások GPS-nyoma eddig örökre a
     * tárhelyen maradt, pedig egy hosszabb útvonal több száz kilobájt.
     *
     * Csak akkor takarít, ha az előzmény beolvasása sikerült – üres naplóra
     * (pl. hibás JSON) nem törlünk semmit.
     */
    public static void cleanupOrphans(Context c) {
        try {
            JSONArray h = History.load(c);
            if (h.length() == 0) return;
            java.util.HashSet<Long> keep = new java.util.HashSet<>();
            for (int i = 0; i < h.length(); i++) {
                JSONObject o = h.optJSONObject(i);
                if (o != null) keep.add(o.optLong("ts"));
            }
            File dir = new File(c.getFilesDir(), "sessions");
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File f : files) {
                long ts = tsOfFile(f.getName());
                if (ts > 0 && !keep.contains(ts)) f.delete();
            }
        } catch (Exception ignored) {}
    }
}
