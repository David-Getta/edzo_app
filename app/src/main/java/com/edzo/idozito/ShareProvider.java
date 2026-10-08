package com.edzo.idozito;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Minimális FileProvider (AndroidX nélkül): a megosztott PNG-ket szolgálja ki
 * content:// URI-n keresztül, hogy más appok (Instagram, Messenger…) elérjék.
 */
public class ShareProvider extends ContentProvider {

    static final String AUTHORITY = "com.edzo.idozito.share";

    static File dir(android.content.Context c) {
        File d = new File(c.getCacheDir(), "share");
        d.mkdirs();
        return d;
    }

    /** Bitmap mentése és megosztás-választó megnyitása. */
    public static void shareImage(Activity a, Bitmap bmp, String baseName) {
        try {
            File f = new File(dir(a), baseName + ".png");
            FileOutputStream fos = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            Uri uri = Uri.parse("content://" + AUTHORITY + "/" + f.getName());
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("image/png");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(i, "Edzés megosztása képként"));
        } catch (Exception ignored) {}
    }

    /** Szövegfájl (pl. CSV) mentése és megosztás-választó megnyitása. */
    public static void shareTextFile(Activity a, String content, String name, String mime) {
        try {
            File f = new File(dir(a), name);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes("UTF-8"));
            fos.flush();
            fos.close();
            Uri uri = Uri.parse("content://" + AUTHORITY + "/" + f.getName());
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(mime);
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(i, "Előzmények exportálása"));
        } catch (Exception ignored) {}
    }

    /**
     * Elfogadható-e a kért fájlnév? Az URI utolsó szakasza a hívótól jön, és a
     * dekódolás után könyvtár-elválasztót is tartalmazhat (a „%2F" egyetlen
     * szakaszon belül is „/" lesz). Ellenőrzés nélkül egy „../../shared_prefs/
     * edzo.xml" kérés a megosztó mappán kívülre mutatna – vagyis a felhasználó
     * teljes adatára. A megosztás csak konkrét, engedélyezett URI-kat ad ki,
     * tehát ez nem kihasználható út, de egy fájlt kiszolgáló szolgáltatónak
     * nem szabad megbíznia a kapott névben.
     *
     * Csak sima fájlnevet engedünk: nincs elválasztó, nincs „..", nem üres.
     */
    static boolean isSafeName(String name) {
        if (name == null || name.isEmpty()) return false;
        if (name.equals(".") || name.equals("..")) return false;
        return name.indexOf('/') < 0 && name.indexOf('\\') < 0 && name.indexOf('\0') < 0;
    }

    /** A megosztó mappán belüli fájl, vagy null, ha a név nem fogadható el. */
    private File fileFor(Uri uri) {
        String name = uri.getLastPathSegment();
        if (!isSafeName(name)) return null;
        return new File(dir(getContext()), name);
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws java.io.FileNotFoundException {
        File f = fileFor(uri);
        if (f == null) throw new java.io.FileNotFoundException("Érvénytelen név: " + uri);
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File f = fileFor(uri);
        if (f == null || !f.exists()) return null;
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
        return c;
    }

    @Override public String getType(Uri uri) {
        String n = uri.getLastPathSegment();
        if (n != null && n.endsWith(".csv")) return "text/csv";
        if (n != null && n.endsWith(".json")) return "application/json";
        return "image/png";
    }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
