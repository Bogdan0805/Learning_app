package com.bogdan0805.learningapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

/** Фоновая служба: следит за расписанием и показывает баннер поверх других приложений. */
public class BitReminderService extends Service {
    static final String PREFS = "bit_reminder";
    static final String CH_BG = "bit_bg", CH_DUE = "bit_due";
    static final int ID_BG = 1, ID_DUE = 2;
    static volatile boolean running = false;

    private static final String[] FACES = {"🤖", "😠", "👿"};
    private static final String[] TITLES = {"⏰ Пора учиться!", "😠 Ты обещал(а)!", "🔥 ПОСЛЕДНИЙ ШАНС!"};
    private static final String[] BODIES = {
        "Бит ждёт тебя. Всего 10 минут!",
        "Бит уже сердится. Не откладывай!",
        "Бит в ярости! Пройди урок СЕЙЧАС!"};
    private static final int[] COLORS = {0xF2F59E0B, 0xF5EA580C, 0xFAB91C1C};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ReminderLogic logic = new ReminderLogic();
    private int ticks = 0;
    private View overlay;
    private ObjectAnimator shake;

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            try { tick(); } catch (Throwable t) { noteErr(t); }
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        running = true;
        makeChannels();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = bgNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_BG, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ID_BG, n);
        }
        WakeReceiver.arm(this, 90000);
        handler.removeCallbacks(loop);
        handler.post(loop);
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        // пользователь смахнул приложение из списка — быстро перезапускаем службу
        WakeReceiver.arm(this, 2000);
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        running = false;
        handler.removeCallbacks(loop);
        hideOverlay();
        boolean en = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("enabled", false);
        if (en) WakeReceiver.arm(this, 3000); else WakeReceiver.disarm(this);
        super.onDestroy();
    }

    private void noteErr(Throwable t) {
        try { getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("lastErr", String.valueOf(t)).apply(); } catch (Throwable ignored) { }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    // ---------- расписание ----------

    private static int parseMinutes(String t) {
        try {
            String[] a = t.split(":");
            return Integer.parseInt(a[0].trim()) * 60 + Integer.parseInt(a[1].trim());
        } catch (Throwable e) { return 17 * 60; }
    }

    private boolean canOverlay() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    private void tick() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        ReminderLogic L = logic;
        L.enabled = p.getBoolean("enabled", false);
        if (!L.enabled) { hideOverlay(); stopSelf(); return; }
        ticks++;
        if (ticks % 5 == 1) p.edit().putLong("lastTick", System.currentTimeMillis()).apply();
        if (ticks % 30 == 1) WakeReceiver.arm(this, 90000);
        L.dueMinutes = parseMinutes(p.getString("time", "17:00"));
        L.perDay = Math.max(1, p.getInt("n", 1));
        L.cnt = p.getInt("cnt", 0);
        L.cntDay = p.getString("cntDay", "");
        L.busy = p.getBoolean("busy", false);
        L.busyAt = p.getLong("busyAt", 0);
        L.demo = p.getBoolean("demo", false);
        if (L.bannerUp && overlay == null) L.bannerUp = false;

        Calendar c = Calendar.getInstance();
        int min = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        String today = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);

        int act = L.tick(System.currentTimeMillis(), min, today, canOverlay());
        if (act == ReminderLogic.DUE_NOTIFY) notifyDue(p, today);
        else if (act == ReminderLogic.SHOW_BANNER) showOverlay();
        else if (act == ReminderLogic.HIDE_BANNER) { hideOverlay(); cancelDue(); }
    }

    // ---------- уведомления ----------

    private NotificationManager nm() { return (NotificationManager) getSystemService(NOTIFICATION_SERVICE); }

    private void makeChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel bg = new NotificationChannel(CH_BG, "Работа в фоне", NotificationManager.IMPORTANCE_LOW);
        NotificationChannel due = new NotificationChannel(CH_DUE, "Напоминание об уроке", NotificationManager.IMPORTANCE_HIGH);
        nm().createNotificationChannel(bg);
        nm().createNotificationChannel(due);
    }

    private Notification.Builder builder(String channel) {
        return Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, channel) : new Notification.Builder(this);
    }

    private Intent launchIntent() {
        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (i == null) i = new Intent();
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return i;
    }

    private PendingIntent openApp() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getActivity(this, 0, launchIntent(), f);
    }

    private Notification bgNotification() {
        Notification.Builder b = builder(CH_BG)
            .setSmallIcon(getApplicationInfo().icon)
            .setContentTitle("Бит на страже 🤖")
            .setContentText("Следит за расписанием уроков")
            .setOngoing(true)
            .setContentIntent(openApp());
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_LOW);
        return b.build();
    }

    private void notifyDue(SharedPreferences p, String today) {
        if (today.equals(p.getString("notifiedDay", ""))) return;
        p.edit().putString("notifiedDay", today).apply();
        Notification.Builder b = builder(CH_DUE)
            .setSmallIcon(getApplicationInfo().icon)
            .setContentTitle("Бит: пора учиться! 🤖")
            .setContentText("Дневной урок ждёт тебя")
            .setAutoCancel(true)
            .setContentIntent(openApp());
        if (Build.VERSION.SDK_INT < 26) {
            b.setPriority(Notification.PRIORITY_HIGH);
            b.setDefaults(Notification.DEFAULT_ALL);
        }
        try { nm().notify(ID_DUE, b.build()); } catch (Throwable ignored) { }
    }

    private void cancelDue() { try { nm().cancel(ID_DUE); } catch (Throwable ignored) { } }

    // ---------- баннер поверх всех окон ----------

    private TextView text(Context ctx, String s, int sp, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private Button button(Context ctx, String s, int bg, int fg, int width, float d) {
        Button b = new Button(ctx);
        b.setText(s);
        b.setTextColor(fg);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(14 * d);
        b.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, (int) (52 * d));
        lp.topMargin = (int) (12 * d);
        b.setLayoutParams(lp);
        return b;
    }

    private void showOverlay() {
        hideOverlay();
        final int st = Math.max(1, Math.min(3, logic.stage));
        Context ctx = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault);
        final float d = getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(ctx);
        root.setBackgroundColor(COLORS[st - 1]);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int pad = (int) (24 * d);
        box.setPadding(pad, pad, pad, pad);

        box.addView(text(ctx, FACES[st - 1], 72, false));
        TextView title = text(ctx, TITLES[st - 1], 30, true);
        title.setPadding(0, (int) (8 * d), 0, (int) (8 * d));
        box.addView(title);
        box.addView(text(ctx, BODIES[st - 1], 18, false));

        int width = Math.min(getResources().getDisplayMetrics().widthPixels - 2 * pad, (int) (360 * d));
        Button learn = button(ctx, "УЧИТЬСЯ", Color.WHITE, 0xFF111111, width, d);
        Button close = button(ctx, "ЗАКРЫТЬ", 0x33000000, Color.WHITE, width, d);
        learn.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { onLearn(); } });
        close.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { onClose(); } });
        box.addView(learn);
        box.addView(close);

        root.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (st == 3) {
            shake = ObjectAnimator.ofFloat(box, "translationX", -5 * d, 5 * d);
            shake.setDuration(200);
            shake.setRepeatMode(ValueAnimator.REVERSE);
            shake.setRepeatCount(ValueAnimator.INFINITE);
            shake.start();
        }

        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        try {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(root, lp);
            overlay = root;
        } catch (Throwable t) {
            if (shake != null) { shake.cancel(); shake = null; }
            noteErr(t);
            logic.bannerClosed(System.currentTimeMillis());
        }
    }

    private void hideOverlay() {
        if (shake != null) { shake.cancel(); shake = null; }
        if (overlay != null) {
            try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(overlay); } catch (Throwable ignored) { }
            overlay = null;
        }
    }

    private void onClose() {
        logic.bannerClosed(System.currentTimeMillis());
        hideOverlay();
    }

    private void onLearn() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("pending", true).apply();
        logic.lessonStarted(System.currentTimeMillis());
        try { startActivity(launchIntent()); } catch (Throwable ignored) { }
        hideOverlay();
        cancelDue();
    }
}
