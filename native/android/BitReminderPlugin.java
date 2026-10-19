package com.bogdan0805.learningapp;

import android.content.ComponentName;
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
        EventLog.add(getContext(), "приложение просит запустить службу");
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
        e.putBoolean("alarm", call.getBoolean("alarm", true));
        if (busy) e.putLong("busyAt", System.currentTimeMillis());
        e.apply();
        if (enabled) startServiceIfNeeded();
        else getContext().stopService(new Intent(getContext(), BitReminderService.class));
        call.resolve();
    }

    @PluginMethod
    public void hasOverlay(PluginCall call) {
        JSObject r = new JSObject();
        StringBuilder errs = new StringBuilder();
        SharedPreferences p = null;
        try { p = prefs(); } catch (Throwable t) { errs.append("prefs: ").append(t).append("; "); }
        try { r.put("overlay", canOverlay()); } catch (Throwable t) { r.put("overlay", false); errs.append("overlay: ").append(t).append("; "); }
        try { r.put("notifications", notificationsOk()); } catch (Throwable t) { r.put("notifications", true); errs.append("notif: ").append(t).append("; "); }
        try { r.put("battery", batteryOk()); } catch (Throwable t) { r.put("battery", false); errs.append("battery: ").append(t).append("; "); }
        try { r.put("running", BitReminderService.running); } catch (Throwable t) { errs.append("running: ").append(t).append("; "); }
        try { r.put("lastTick", p == null ? 0L : p.getLong("lastTick", 0L)); } catch (Throwable t) { errs.append("tick: ").append(t).append("; "); }
        try { r.put("now", System.currentTimeMillis()); } catch (Throwable ignored) { }
        try {
            String e = p == null ? "" : p.getString("lastErr", "");
            r.put("lastErr", errs.length() > 0 ? errs.toString() : e);
        } catch (Throwable ignored) { }
        try { r.put("device", EventLog.device()); } catch (Throwable ignored) { }
        try { r.put("maker", String.valueOf(Build.MANUFACTURER).toLowerCase(java.util.Locale.ROOT)); } catch (Throwable ignored) { }
        try { r.put("alarming", p != null && p.getBoolean("alarming", false)); } catch (Throwable ignored) { }
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
    public void getLog(PluginCall call) {
        JSObject r = new JSObject();
        r.put("log", EventLog.get(getContext()));
        call.resolve(r);
    }

    @PluginMethod
    public void clearLog(PluginCall call) {
        EventLog.clear(getContext());
        call.resolve();
    }

    /** Открывает экран автозапуска/питания именно для производителя телефона, иначе — настройки приложения. */
    @PluginMethod
    public void openAutostart(PluginCall call) {
        String maker = String.valueOf(Build.MANUFACTURER).toLowerCase(java.util.Locale.ROOT);
        String[][] tries;
        if (maker.contains("xiaomi") || maker.contains("redmi") || maker.contains("poco")) {
            tries = new String[][]{{"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"}};
        } else if (maker.contains("huawei") || maker.contains("honor")) {
            tries = new String[][]{
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"}};
        } else if (maker.contains("oppo") || maker.contains("realme") || maker.contains("oneplus")) {
            tries = new String[][]{
                {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
                {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"}};
        } else if (maker.contains("vivo") || maker.contains("iqoo")) {
            tries = new String[][]{
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"}};
        } else if (maker.contains("samsung")) {
            tries = new String[][]{{"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"}};
        } else {
            tries = new String[0][];
        }
        String opened = "";
        for (String[] t : tries) {
            try {
                Intent i = new Intent();
                i.setComponent(new ComponentName(t[0], t[1]));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(i);
                opened = t[0];
                break;
            } catch (Throwable ignored) { }
        }
        if (opened.isEmpty()) openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        JSObject r = new JSObject();
        r.put("opened", opened);
        call.resolve(r);
    }

    @PluginMethod
    public void openAppSettings(PluginCall call) {
        openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
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
        r.put("alarming", p.getBoolean("alarming", false));
        call.resolve(r);
    }
}
