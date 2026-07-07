package com.edzo.idozito;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Újratelepíti az emlékeztető-ütemezéseket a telefon újraindítása után
 * (az AlarmManager riasztásai bootkor törlődnek).
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || "android.intent.action.QUICKBOOT_POWERON".equals(a)) {
            Reminders.scheduleAll(c);
        }
    }
}
