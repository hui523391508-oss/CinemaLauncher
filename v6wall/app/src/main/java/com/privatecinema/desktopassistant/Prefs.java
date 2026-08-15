package com.privatecinema.desktopassistant;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String NAME = "private_cinema_desktop_v6";
    private static final String TARGET_PKG = "target_pkg";
    private static final String TARGET_CLS = "target_cls";
    private static final String TARGET_LABEL = "target_label";
    private static final String HOME_ENABLED = "home_enabled";
    private static final String BOOT_ENABLED = "boot_enabled";
    private static final String SERVICE_WANTED = "service_wanted";
    private static final String MAINTENANCE_UNTIL = "maintenance_until";

    private static final String CLOSE_DIALOG_COUNT = "close_dialog_count";
    private static final String HOMEKEY_COUNT = "homekey_count";
    private static final String LAST_REASON = "last_reason";
    private static final String LOCKED_BOOT_COUNT = "locked_boot_count";
    private static final String BOOT_COUNT = "boot_count";
    private static final String SCREEN_ON_COUNT = "screen_on_count";
    private static final String USER_UNLOCKED_COUNT = "user_unlocked_count";
    private static final String LAST_START_SOURCE = "last_start_source";

    private static final String LAUNCH_ATTEMPT_COUNT = "launch_attempt_count";
    private static final String LAUNCH_SUCCESS_COUNT = "launch_success_count";
    private static final String LAUNCH_FAIL_COUNT = "launch_fail_count";
    private static final String LAST_LAUNCH_ERROR = "last_launch_error";

    private static final String LAST_WALLPAPER_STATUS = "last_wallpaper_status";
    private static final String LAST_WALLPAPER_SOURCE = "last_wallpaper_source";

    private Prefs() {}

    private static SharedPreferences p(Context c) {
        Context storage = c;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Context dp = c.createDeviceProtectedStorageContext();
            if (dp != null) storage = dp;
        }
        return storage.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static void setTarget(Context c, String label, String pkg, String cls) {
        p(c).edit()
                .putString(TARGET_LABEL, label == null ? "" : label)
                .putString(TARGET_PKG, pkg == null ? "" : pkg)
                .putString(TARGET_CLS, cls == null ? "" : cls)
                .apply();
    }

    public static String targetLabel(Context c) { return p(c).getString(TARGET_LABEL, ""); }
    public static String targetPkg(Context c) { return p(c).getString(TARGET_PKG, ""); }
    public static String targetCls(Context c) { return p(c).getString(TARGET_CLS, ""); }

    public static boolean homeEnabled(Context c) { return p(c).getBoolean(HOME_ENABLED, true); }
    public static void setHomeEnabled(Context c, boolean v) { p(c).edit().putBoolean(HOME_ENABLED, v).apply(); }
    public static boolean bootEnabled(Context c) { return p(c).getBoolean(BOOT_ENABLED, true); }
    public static void setBootEnabled(Context c, boolean v) { p(c).edit().putBoolean(BOOT_ENABLED, v).apply(); }
    public static boolean serviceWanted(Context c) { return p(c).getBoolean(SERVICE_WANTED, true); }
    public static void setServiceWanted(Context c, boolean v) { p(c).edit().putBoolean(SERVICE_WANTED, v).apply(); }

    public static void setMaintenanceUntil(Context c, long t) { p(c).edit().putLong(MAINTENANCE_UNTIL, t).apply(); }
    public static long maintenanceUntil(Context c) { return p(c).getLong(MAINTENANCE_UNTIL, 0L); }
    public static void clearMaintenance(Context c) { p(c).edit().putLong(MAINTENANCE_UNTIL, 0L).apply(); }

    public static void receivedCloseDialogs(Context c, String reason) {
        int n = p(c).getInt(CLOSE_DIALOG_COUNT, 0) + 1;
        p(c).edit().putInt(CLOSE_DIALOG_COUNT, n).putString(LAST_REASON, reason == null ? "(null)" : reason).apply();
    }
    public static void receivedHomeKey(Context c) {
        int n = p(c).getInt(HOMEKEY_COUNT, 0) + 1;
        p(c).edit().putInt(HOMEKEY_COUNT, n).apply();
    }
    public static void receivedLockedBoot(Context c) {
        int n = p(c).getInt(LOCKED_BOOT_COUNT, 0) + 1;
        p(c).edit().putInt(LOCKED_BOOT_COUNT, n).putString(LAST_START_SOURCE, "LOCKED_BOOT_COMPLETED").apply();
    }
    public static void receivedBoot(Context c) {
        int n = p(c).getInt(BOOT_COUNT, 0) + 1;
        p(c).edit().putInt(BOOT_COUNT, n).putString(LAST_START_SOURCE, "BOOT_COMPLETED").apply();
    }
    public static void receivedScreenOn(Context c) {
        int n = p(c).getInt(SCREEN_ON_COUNT, 0) + 1;
        p(c).edit().putInt(SCREEN_ON_COUNT, n).putString(LAST_START_SOURCE, "SCREEN_ON").apply();
    }
    public static void receivedUserUnlocked(Context c) {
        int n = p(c).getInt(USER_UNLOCKED_COUNT, 0) + 1;
        p(c).edit().putInt(USER_UNLOCKED_COUNT, n).putString(LAST_START_SOURCE, "USER_UNLOCKED").apply();
    }

    public static void launchAttempt(Context c) {
        int n = p(c).getInt(LAUNCH_ATTEMPT_COUNT, 0) + 1;
        p(c).edit().putInt(LAUNCH_ATTEMPT_COUNT, n).apply();
    }
    public static void launchSuccess(Context c) {
        int n = p(c).getInt(LAUNCH_SUCCESS_COUNT, 0) + 1;
        p(c).edit().putInt(LAUNCH_SUCCESS_COUNT, n).putString(LAST_LAUNCH_ERROR, "无").apply();
    }
    public static void launchFailure(Context c, String error) {
        int n = p(c).getInt(LAUNCH_FAIL_COUNT, 0) + 1;
        p(c).edit().putInt(LAUNCH_FAIL_COUNT, n).putString(LAST_LAUNCH_ERROR, error == null ? "未知" : error).apply();
    }

    public static int closeDialogsCount(Context c) { return p(c).getInt(CLOSE_DIALOG_COUNT, 0); }
    public static int homeKeyCount(Context c) { return p(c).getInt(HOMEKEY_COUNT, 0); }
    public static String lastReason(Context c) { return p(c).getString(LAST_REASON, "尚未收到"); }
    public static int lockedBootCount(Context c) { return p(c).getInt(LOCKED_BOOT_COUNT, 0); }
    public static int bootCount(Context c) { return p(c).getInt(BOOT_COUNT, 0); }
    public static int screenOnCount(Context c) { return p(c).getInt(SCREEN_ON_COUNT, 0); }
    public static int userUnlockedCount(Context c) { return p(c).getInt(USER_UNLOCKED_COUNT, 0); }
    public static String lastStartSource(Context c) { return p(c).getString(LAST_START_SOURCE, "尚未收到"); }
    public static int launchAttemptCount(Context c) { return p(c).getInt(LAUNCH_ATTEMPT_COUNT, 0); }
    public static int launchSuccessCount(Context c) { return p(c).getInt(LAUNCH_SUCCESS_COUNT, 0); }
    public static int launchFailCount(Context c) { return p(c).getInt(LAUNCH_FAIL_COUNT, 0); }
    public static String lastLaunchError(Context c) { return p(c).getString(LAST_LAUNCH_ERROR, "无"); }

    public static void setWallpaperStatus(Context c, String source, String status) {
        p(c).edit().putString(LAST_WALLPAPER_SOURCE, source == null ? "" : source)
                .putString(LAST_WALLPAPER_STATUS, status == null ? "" : status).apply();
    }
    public static String lastWallpaperSource(Context c) { return p(c).getString(LAST_WALLPAPER_SOURCE, "尚未操作"); }
    public static String lastWallpaperStatus(Context c) { return p(c).getString(LAST_WALLPAPER_STATUS, "尚未操作"); }

    public static void resetDiagnostics(Context c) {
        p(c).edit()
                .putInt(CLOSE_DIALOG_COUNT, 0)
                .putInt(HOMEKEY_COUNT, 0)
                .putString(LAST_REASON, "尚未收到")
                .putInt(LOCKED_BOOT_COUNT, 0)
                .putInt(BOOT_COUNT, 0)
                .putInt(SCREEN_ON_COUNT, 0)
                .putInt(USER_UNLOCKED_COUNT, 0)
                .putString(LAST_START_SOURCE, "尚未收到")
                .putInt(LAUNCH_ATTEMPT_COUNT, 0)
                .putInt(LAUNCH_SUCCESS_COUNT, 0)
                .putInt(LAUNCH_FAIL_COUNT, 0)
                .putString(LAST_LAUNCH_ERROR, "无")
                .apply();
    }
}
