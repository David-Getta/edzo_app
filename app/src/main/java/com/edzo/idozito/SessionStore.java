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
            byte[] data = new byte[(int) f.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(f);
            int read = fis.read(data);
            fis.close();
            if (read <= 0) return new JSONArray();
            JSONObject o = new JSONObject(new String(data, 0, read, "UTF-8"));
            return o.optJSONArray("track") != null ? o.optJSONArray("track") : new JSONArray();
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void delete(Context c, long ts) {
        try { File f = file(c, ts); if (f.exists()) f.delete(); } catch (Exception ignored) {}
    }
}
