package com.edzo.idozito;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * Értesítés-engedély állapota és a rendszerbeállítás megnyitása.
 *
 * Az app induláskor egyszer kéri az engedélyt. Ha a felhasználó elutasítja –
 * vagy később kikapcsolja a rendszerbeállításokban –, az összes emlékeztető
 * NÉMÁN nem szólal meg: a napi biztatás, az időzített emlékeztetők és a heti
 * visszatekintő is. Az app addig vidáman engedi új emlékeztetők felvételét,
 * amik sosem fognak megjelenni. Ezért kell látható figyelmeztetés.
 */
public final class Notifs {

    private Notifs() {}

    /** Meg tud-e jelenni egyáltalán értesítés? */
    public static boolean enabled(Context c) {
        try {
            NotificationManager nm =
                    (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            return nm == null || nm.areNotificationsEnabled();
        } catch (Exception e) {
            return true;   // ha nem tudjuk eldönteni, ne ijesztgessünk feleslegesen
        }
    }

    /** Az app értesítés-beállításainak megnyitása (régebbi Androidon az app-adatlap). */
    public static void openSettings(Context c) {
        try {
            Intent i = new Intent();
            if (Build.VERSION.SDK_INT >= 26) {
                i.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                i.putExtra(Settings.EXTRA_APP_PACKAGE, c.getPackageName());
            } else {
                i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + c.getPackageName()));
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception ignored) {}
    }
}
