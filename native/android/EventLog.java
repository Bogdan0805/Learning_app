package com.bogdan0805.learningapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Короткий журнал событий службы: помогает понять, почему баннер не показался. */
final class EventLog {
    private static final String KEY = "log";
    private static final int MAX = 60;

    static synchronized void add(Context c, String msg) {
        try {
            SharedPreferences p = c.getSharedPreferences(BitReminderService.PREFS, Context.MODE_PRIVATE);
            String old = p.getString(KEY, "");
            String line = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.US).format(new Date()) + "  " + msg;
            String[] a = old.isEmpty() ? new String[0] : old.split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = Math.max(0, a.length - (MAX - 1)); i < a.length; i++) sb.append(a[i]).append('\n');
            sb.append(line);
            p.edit().putString(KEY, sb.toString()).apply();
        } catch (Throwable ignored) { }
    }

    static String get(Context c) {
        try { return c.getSharedPreferences(BitReminderService.PREFS, Context.MODE_PRIVATE).getString(KEY, ""); }
        catch (Throwable t) { return ""; }
    }

    static void clear(Context c) {
        try { c.getSharedPreferences(BitReminderService.PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply(); }
        catch (Throwable ignored) { }
    }

    static String device() {
        return Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
    }
}
