package com.bogdan0805.learningapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** После перезагрузки телефона или обновления приложения снова запускает службу. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        try {
            if (!c.getSharedPreferences(BitReminderService.PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false)) return;
            Intent s = new Intent(c, BitReminderService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s); else c.startService(s);
        } catch (Throwable ignored) { }
    }
}
