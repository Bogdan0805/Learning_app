package com.bogdan0805.learningapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Мост между JS-приложением и фоновой службой напоминаний. */
@CapacitorPlugin(name = "BitReminder")
public class BitReminderPlugin extends Plugin {

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(BitReminderService.PREFS, Context.MODE_PRIVATE);
    }

    private boolean canOverlay() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(getContext());
    }

    private boolean notificationsOk() {
        return Build.VERSION.SDK_INT < 33
            || getContext().checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
    }

    private boolean batteryOk() {
        if (Build.VERSION.SDK_INT < 23) return true;
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
    }

    private void startServiceIfNeeded() {
        if (BitReminderService.running) return;
        try {
            Intent i = new Intent(getContext(), BitReminderService.class);
            if (Build.VERSION.SDK_INT >= 26) getContext().startForegroundService(i);
            else getContext().startService(i);
        } catch (Throwable ignored) { }
    }

    private void openSettings(String action) {
        try {
            Intent i = new Intent(action, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        } catch (Throwable ignored) { }
    }

    /** Приложение сообщает расписание и прогресс; служба читает это из настроек. */
    @PluginMethod
    public void sync(PluginCall call) {
        boolean enabled = call.getBoolean("enabled", false);
        boolean busy = call.getBoolean("busy", false);
        SharedPreferences.Editor e = prefs().edit();
        e.putBoolean("enabled", enabled);
        e.putString("time", call.getString("time", "17:00"));
        e.putInt("n", call.getInt("n", 1));
        e.putInt("cnt", call.getInt("cnt", 0));
        e.putString("cntDay", call.getString("dayKey", ""));
        e.putBoolean("busy", busy);
        e.putBoolean("demo", call.getBoolean("demo", false));
        if (busy) e.putLong("busyAt", System.currentTimeMillis());
        e.apply();
        if (enabled) startServiceIfNeeded();
        else getContext().stopService(new Intent(getContext(), BitReminderService.class));
        call.resolve();
    }

    @PluginMethod
    public void hasOverlay(PluginCall call) {
        JSObject r = new JSObject();
        r.put("overlay", canOverlay());
        r.put("notifications", notificationsOk());
        r.put("battery", batteryOk());
        call.resolve(r);
    }

    @PluginMethod
    public void requestOverlay(PluginCall call) {
        openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        call.resolve();
    }

    @PluginMethod
    public void requestNotifications(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsOk() && getActivity() != null) {
            getActivity().requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 7731);
        }
        call.resolve();
    }

    @PluginMethod
    public void requestBattery(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 23 && !batteryOk()) openSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        call.resolve();
    }

    /** true, если пользователь нажал «Учиться» на баннере (значит, нужно открыть выбор урока). */
    @PluginMethod
    public void getPending(PluginCall call) {
        SharedPreferences p = prefs();
        boolean pending = p.getBoolean("pending", false);
        if (pending) p.edit().putBoolean("pending", false).apply();
        JSObject r = new JSObject();
        r.put("start", pending);
        call.resolve(r);
    }
}
