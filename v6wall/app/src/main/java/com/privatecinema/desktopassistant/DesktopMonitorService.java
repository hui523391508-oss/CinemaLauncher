package com.privatecinema.desktopassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class DesktopMonitorService extends Service {
    public static final String ACTION_START = "com.privatecinema.desktopassistant.START_MONITOR";
    public static final String ACTION_STOP = "com.privatecinema.desktopassistant.STOP_MONITOR";
    public static final String EXTRA_FROM_BOOT = "from_boot";

    private static final String CHANNEL = "private_cinema_desktop_monitor";
    private static final int NOTIFICATION_ID = 6101;
    private static final long HOME_RELAUNCH_DELAY_MS = 5300L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean registered;

    private final BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Prefs.receivedScreenOn(context);
                scheduleWakeLaunch(1800L);
                return;
            }

            if (Intent.ACTION_USER_UNLOCKED.equals(action)) {
                Prefs.receivedUserUnlocked(context);
                scheduleWakeLaunch(1200L);
                return;
            }

            if (!Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) return;

            String reason = intent.getStringExtra("reason");
            Prefs.receivedCloseDialogs(context, reason);
            if (!"homekey".equals(reason)) return;

            Prefs.receivedHomeKey(context);
            if (!Prefs.homeEnabled(context)) return;
            if (inMaintenance(context)) return;
            if (Prefs.targetPkg(context).isEmpty()) return;

            handler.removeCallbacks(homeLaunch);
            handler.postDelayed(homeLaunch, HOME_RELAUNCH_DELAY_MS);
        }
    };

    private final Runnable homeLaunch = () -> {
        if (Prefs.homeEnabled(this) && !inMaintenance(this)) AppUtils.launchTarget(this);
    };

    private final Runnable wakeLaunch = () -> {
        if (Prefs.bootEnabled(this) && !inMaintenance(this)) AppUtils.launchTarget(this);
    };

    private final Runnable bootFallback = () -> {
        if (Prefs.bootEnabled(this)) {
            Prefs.clearMaintenance(this);
            AppUtils.launchTarget(this);
        }
    };

    private boolean inMaintenance(Context c) {
        return System.currentTimeMillis() < Prefs.maintenanceUntil(c);
    }

    private void scheduleWakeLaunch(long delay) {
        if (!Prefs.bootEnabled(this)) return;
        if (inMaintenance(this)) return;
        if (Prefs.targetPkg(this).isEmpty()) return;
        handler.removeCallbacks(wakeLaunch);
        handler.postDelayed(wakeLaunch, delay);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        registerMonitor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            Prefs.setServiceWanted(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }

        Prefs.setServiceWanted(this, true);
        registerMonitor();

        if (intent != null && intent.getBooleanExtra(EXTRA_FROM_BOOT, false)) {
            handler.removeCallbacks(bootFallback);
            handler.postDelayed(bootFallback, 6500L);
        }
        return START_STICKY;
    }

    private void registerMonitor() {
        if (registered) return;
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_USER_UNLOCKED);
        registerReceiver(systemReceiver, f);
        registered = true;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, getString(R.string.service_channel), NotificationManager.IMPORTANCE_MIN);
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle(getString(R.string.service_title))
                .setContentText(getString(R.string.service_text))
                .setContentIntent(pi)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (registered) {
            try { unregisterReceiver(systemReceiver); } catch (Exception ignored) {}
            registered = false;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
