package com.privatecinema.desktopassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) return;

        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            Prefs.receivedLockedBoot(context);
        } else {
            Prefs.receivedBoot(context);
        }

        if (!Prefs.serviceWanted(context) && !Prefs.bootEnabled(context)) return;

        Intent s = new Intent(context, DesktopMonitorService.class);
        s.setAction(DesktopMonitorService.ACTION_START);
        s.putExtra(DesktopMonitorService.EXTRA_FROM_BOOT, true);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(s);
            else context.startService(s);
        } catch (Exception e) {
            Prefs.launchFailure(context, "启动监听服务失败：" + e.getClass().getSimpleName());
        }
    }
}
