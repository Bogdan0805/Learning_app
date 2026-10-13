package com.bogdan0805.learningapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

/**
 * Страховка: будильник будит приложение и перезапускает службу, если систему или
 * производитель телефона её остановили. Пока служба жива, она постоянно отодвигает будильник.
 */
public class WakeReceiver extends BroadcastReceiver {

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, WakeReceiver.class);
        int f = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(c, 7, i, f);
    }

    static void arm(Context c, long delayMs) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            long at = SystemClock.elapsedRealtime() + delayMs;
            if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending(c));
            else am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pending(c));
        } catch (Throwable ignored) { }
    }

    static void disarm(Context c) {
        try {
            ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pending(c));
        } catch (Throwable ignored) { }
    }

    @Override public void onReceive(Context c, Intent intent) {
        SharedPreferences p = c.getSharedPreferences(BitReminderService.PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean("enabled", false)) return;
        try {
            Intent s = new Intent(c, BitReminderService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s); else c.startService(s);
            p.edit().putLong("lastWake", System.currentTimeMillis()).apply();
        } catch (Throwable t) {
            p.edit().putString("lastErr", "wake: " + t).apply();
        }
        arm(c, 90000);
    }
}
