package com.privatecinema.desktopassistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

public final class AppUtils {
    private AppUtils() {}

    public static boolean launchTarget(Context c) {
        String pkg = Prefs.targetPkg(c);
        String cls = Prefs.targetCls(c);
        if (pkg.isEmpty()) {
            Prefs.launchFailure(c, "未选择目标桌面");
            return false;
        }

        Prefs.launchAttempt(c);

        try {
            if (!cls.isEmpty()) {
                Intent explicit = new Intent(Intent.ACTION_MAIN);
                explicit.setComponent(new ComponentName(pkg, cls));
                explicit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                c.startActivity(explicit);
                Prefs.launchSuccess(c);
                return true;
            }
        } catch (Exception ignored) {}

        try {
            PackageManager pm = c.getPackageManager();
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i == null) throw new IllegalStateException("系统没有返回启动Intent");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            c.startActivity(i);
            Prefs.launchSuccess(c);
            return true;
        } catch (Exception e) {
            Prefs.launchFailure(c, e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()));
            return false;
        }
    }

    public static ComponentName currentDefaultHome(Context c) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo r = c.getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            if (r != null && r.activityInfo != null) {
                return new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static List<ComponentName> listHomeActivities(Context c) {
        List<ComponentName> out = new ArrayList<>();
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> list = c.getPackageManager().queryIntentActivities(i, PackageManager.MATCH_ALL);
            for (ResolveInfo r : list) {
                if (r.activityInfo != null) {
                    out.add(new ComponentName(r.activityInfo.packageName, r.activityInfo.name));
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static String homeCandidates(Context c) {
        StringBuilder b = new StringBuilder();
        for (ComponentName cn : listHomeActivities(c)) {
            if (b.length() > 0) b.append(" | ");
            b.append(cn.flattenToShortString());
        }
        return b.length() == 0 ? "未发现" : b.toString();
    }

    public static boolean openOriginalHome(Context c) {
        String targetPkg = Prefs.targetPkg(c);

        for (ComponentName cn : listHomeActivities(c)) {
            if (c.getPackageName().equals(cn.getPackageName())) continue;
            if (!targetPkg.isEmpty() && targetPkg.equals(cn.getPackageName())) continue;
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_HOME);
                i.setComponent(cn);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                c.startActivity(i);
                return true;
            } catch (Exception ignored) {}
        }

        ComponentName current = currentDefaultHome(c);
        if (current != null) {
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_HOME);
                i.setComponent(current);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                c.startActivity(i);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
