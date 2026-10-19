package com.bogdan0805.learningapp;

/** Логика расписания баннеров без зависимостей от Android (чтобы её можно было тестировать). */
final class ReminderLogic {
    static final int NONE = 0, DUE_NOTIFY = 1, SHOW_BANNER = 2, HIDE_BANNER = 3;
    static final long BUSY_TTL = 3 * 60 * 1000L;

    // настройки (приходят из приложения)
    boolean enabled;
    int dueMinutes = 17 * 60;
    int perDay = 1;
    int cnt;
    String cntDay = "";
    boolean busy;
    long busyAt;
    boolean demo;
    boolean alarmEnabled = true;

    // состояние
    long trig = 0;
    long nextShow = 0;
    int stage = 0;
    boolean bannerUp = false;
    private boolean lastDemo = false;
    private String lastDay = "";

    long interval() { return demo ? 8000L : 60000L; }
    boolean done(String today) { return cntDay.equals(today) && cnt >= perDay; }
    boolean busyNow(long now) { return busy && now - busyAt < BUSY_TTL; }

    private int reset() {
        trig = 0; stage = 0; nextShow = 0;
        if (bannerUp) { bannerUp = false; return HIDE_BANNER; }
        return NONE;
    }

    /** Вызывается раз в секунду. Возвращает, что нужно сделать. */
    int tick(long now, int minutesOfDay, String today, boolean canOverlay) {
        if (!enabled) return reset();
        if (demo != lastDemo) { lastDemo = demo; return reset(); }
        if (!today.equals(lastDay)) { lastDay = today; return reset(); }
        if (done(today) && !demo) return reset();
        boolean due = demo || minutesOfDay >= dueMinutes;
        if (!due) return reset();
        int res = NONE;
        if (trig == 0) {
            trig = now; stage = 0;
            nextShow = now + (demo ? 5000L : 300000L);
            res = DUE_NOTIFY;
        }
        if (!busyNow(now) && !bannerUp && canOverlay && now >= nextShow) {
            stage = Math.min(stage + 1, 3);
            bannerUp = true;
            return SHOW_BANNER;
        }
        return res;
    }

    /** Тревога-уведомления: идут параллельно баннеру на последней (3-й) стадии, пока урок дня не пройден. */
    boolean alarmActive(long now) {
        return enabled && alarmEnabled && trig != 0 && stage >= 3 && !busyNow(now);
    }

    void bannerClosed(long now) { bannerUp = false; nextShow = now + interval(); }
    void lessonStarted(long now) { bannerUp = false; nextShow = now + interval(); }
}
