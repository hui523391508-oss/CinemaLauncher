package com.privatecinema.desktopassistant;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_STORAGE = 601;
    private static final int REQ_PICK_IMAGE = 602;

    private Spinner appSpinner;
    private TextView status;
    private final List<AppEntry> apps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        refreshApps();
        if (Prefs.serviceWanted(this)) startMonitor(false, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(42, 26, 42, 44);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root);

        TextView title = text("私人影院桌面助手 · " + BuildConfig.EDITION_NAME, 25);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidth());

        TextView intro = text(
                "\n用途：选择当贝桌面等目标桌面 → 开机自动进入 → HOME返回目标桌面。"
                        + "\n壁纸：已内置12张最终壁纸，可预览、单张/全部释放并尝试直接设为系统壁纸。"
                        + "\n雷鸟/VIDDA：底部诊断信息会记录开机、唤醒、HOME、启动失败原因和系统桌面信息。\n", 15);
        root.addView(intro, fullWidth());

        addSection(root, "① 目标桌面");
        appSpinner = new Spinner(this);
        appSpinner.setFocusable(true);
        root.addView(appSpinner, fullWidth());

        Button save = button("保存为开机/HOME目标桌面");
        save.setOnClickListener(v -> saveSelected());
        root.addView(save, fullWidth());

        Button scan = button("重新扫描应用 / 桌面");
        scan.setOnClickListener(v -> refreshApps());
        root.addView(scan, fullWidth());

        Button test = button("测试打开目标桌面");
        test.setOnClickListener(v -> {
            Prefs.clearMaintenance(this);
            toast(AppUtils.launchTarget(this) ? "已发起目标桌面启动" : "启动失败，请看诊断");
            updateStatus();
        });
        root.addView(test, fullWidth());

        addSection(root, "② 开机与 HOME 接管");
        Button monitor = button("启动桌面监听服务（建议保持运行）");
        monitor.setOnClickListener(v -> startMonitor(false, true));
        root.addView(monitor, fullWidth());

        Button homeOn = button("开启 HOME 返回目标桌面");
        homeOn.setOnClickListener(v -> { Prefs.setHomeEnabled(this, true); Prefs.clearMaintenance(this); updateStatus(); toast("HOME接管已开启"); });
        root.addView(homeOn, fullWidth());

        Button homeOff = button("关闭 HOME 接管");
        homeOff.setOnClickListener(v -> { Prefs.setHomeEnabled(this, false); updateStatus(); toast("HOME接管已关闭"); });
        root.addView(homeOff, fullWidth());

        Button bootOn = button("开启开机自动进入目标桌面");
        bootOn.setOnClickListener(v -> { Prefs.setBootEnabled(this, true); Prefs.setServiceWanted(this, true); startMonitor(false, false); updateStatus(); toast("开机自动启动已开启"); });
        root.addView(bootOn, fullWidth());

        Button bootOff = button("关闭开机自动启动");
        bootOff.setOnClickListener(v -> { Prefs.setBootEnabled(this, false); updateStatus(); toast("开机自动启动已关闭"); });
        root.addView(bootOff, fullWidth());

        Button original = button("进入原厂系统桌面（暂停接管10分钟）");
        original.setOnClickListener(v -> {
            Prefs.setMaintenanceUntil(this, System.currentTimeMillis() + 10 * 60 * 1000L);
            if (!AppUtils.openOriginalHome(this)) toast("没有找到可打开的原厂桌面");
            updateStatus();
        });
        root.addView(original, fullWidth());

        Button resume = button("结束维护并恢复接管");
        resume.setOnClickListener(v -> { Prefs.clearMaintenance(this); toast("已恢复接管"); updateStatus(); });
        root.addView(resume, fullWidth());

        addSection(root, "③ 内置壁纸库 / 壁纸工具");
        TextView wallNote = text(
                "已内置12张最终壁纸。固定释放目录：Pictures/私人影院壁纸/ 和 Download/私人影院壁纸/"
                        + "\n可在壁纸库中遥控选择、预览、单张释放、全部释放，并尝试直接设为系统壁纸。", 14);
        root.addView(wallNote, fullWidth());

        Button library = button("打开内置壁纸库（12张）");
        library.setOnClickListener(v -> startActivity(new Intent(this, WallpaperLibraryActivity.class)));
        root.addView(library, fullWidth());

        Button importCustom = button("导入其他自定义壁纸 → 自动复制到固定目录");
        importCustom.setOnClickListener(v -> pickImage());
        root.addView(importCustom, fullWidth());

        Button setCustom = button("尝试将已导入的自定义壁纸设为系统壁纸");
        setCustom.setOnClickListener(v -> { toast(WallpaperUtils.setCustomAsSystemWallpaper(this)); updateStatus(); });
        root.addView(setCustom, fullWidth());

        addSection(root, "④ 雷鸟 / VIDDA / 极米诊断");
        Button reset = button("清零启动诊断计数");
        reset.setOnClickListener(v -> { Prefs.resetDiagnostics(this); updateStatus(); });
        root.addView(reset, fullWidth());

        Button copy = button("复制完整诊断信息");
        copy.setOnClickListener(v -> copyDiagnostics());
        root.addView(copy, fullWidth());

        status = text("", 14);
        status.setTextIsSelectable(true);
        status.setPadding(0, 18, 0, 30);
        root.addView(status, fullWidth());

        setContentView(scroll);
    }

    private void addSection(LinearLayout root, String s) {
        TextView t = text("\n" + s, 20);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(t, fullWidth());
    }

    private TextView text(String s, float size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(17);
        b.setFocusable(true);
        return b;
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 6, 0, 6);
        return p;
    }

    private void startMonitor(boolean fromBoot, boolean showToast) {
        Prefs.setServiceWanted(this, true);
        Intent s = new Intent(this, DesktopMonitorService.class);
        s.setAction(DesktopMonitorService.ACTION_START);
        s.putExtra(DesktopMonitorService.EXTRA_FROM_BOOT, fromBoot);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(s);
            else startService(s);
            if (showToast) toast("监听服务已启动");
        } catch (Exception e) {
            if (showToast) toast("监听服务启动失败：" + e.getClass().getSimpleName());
        }
        updateStatus();
    }

    private void refreshApps() {
        apps.clear();
        Set<String> seen = new HashSet<>();
        collect(Intent.CATEGORY_LEANBACK_LAUNCHER, seen);
        collect(Intent.CATEGORY_LAUNCHER, seen);
        collect(Intent.CATEGORY_HOME, seen);

        Collections.sort(apps, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                int pa = a.isDangbei ? 0 : 1;
                int pb = b.isDangbei ? 0 : 1;
                if (pa != pb) return pa - pb;
                return a.label.toLowerCase(Locale.ROOT).compareTo(b.label.toLowerCase(Locale.ROOT));
            }
        });

        ArrayAdapter<AppEntry> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, apps);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appSpinner.setAdapter(adapter);

        String savedPkg = Prefs.targetPkg(this);
        String savedCls = Prefs.targetCls(this);
        boolean selected = false;
        for (int i = 0; i < apps.size(); i++) {
            AppEntry e = apps.get(i);
            if (e.pkg.equals(savedPkg) && e.cls.equals(savedCls)) {
                appSpinner.setSelection(i);
                selected = true;
                break;
            }
        }
        if (!selected) {
            for (int i = 0; i < apps.size(); i++) {
                if (apps.get(i).isDangbei) {
                    appSpinner.setSelection(i);
                    break;
                }
            }
        }
        updateStatus();
    }

    private void collect(String category, Set<String> seen) {
        try {
            PackageManager pm = getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(category);
            List<ResolveInfo> list = pm.queryIntentActivities(q, PackageManager.MATCH_ALL);
            for (ResolveInfo r : list) {
                if (r.activityInfo == null) continue;
                String pkg = r.activityInfo.packageName;
                String cls = r.activityInfo.name;
                if (getPackageName().equals(pkg)) continue;
                String key = pkg + "/" + cls;
                if (!seen.add(key)) continue;
                CharSequence cs = r.loadLabel(pm);
                String label = cs == null ? pkg : cs.toString();
                String low = (label + " " + pkg).toLowerCase(Locale.ROOT);
                boolean dangbei = label.contains("当贝") || low.contains("dangbei") || low.contains("dblauncher");
                apps.add(new AppEntry(label, pkg, cls, dangbei, category));
            }
        } catch (Exception ignored) {}
    }

    private void saveSelected() {
        Object o = appSpinner.getSelectedItem();
        if (!(o instanceof AppEntry)) { toast("没有可选择的应用/桌面"); return; }
        AppEntry e = (AppEntry) o;
        Prefs.setTarget(this, e.label, e.pkg, e.cls);
        Prefs.clearMaintenance(this);
        toast("已保存目标桌面：" + e.label);
        updateStatus();
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        try { startActivityForResult(i, REQ_PICK_IMAGE); }
        catch (Exception e) { toast("系统没有可用的图片选择器"); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            final int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            toast("已选图片，但需要存储权限；授权后请重新导入一次");
            return;
        }

        toast(WallpaperUtils.importAndExport(this, uri));
        updateStatus();
    }

    private String diagnostics() {
        ComponentName home = AppUtils.currentDefaultHome(this);
        long left = Math.max(0L, Prefs.maintenanceUntil(this) - System.currentTimeMillis());
        String storage = Environment.getExternalStorageState();

        return "私人影院桌面助手 " + BuildConfig.EDITION_NAME
                + "\n设备厂商 Manufacturer：" + Build.MANUFACTURER
                + "\n品牌 Brand：" + Build.BRAND
                + "\n型号 Model：" + Build.MODEL
                + "\n设备 Device：" + Build.DEVICE
                + "\nAndroid：" + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT
                + "\nBuild Display：" + Build.DISPLAY
                + "\n当前默认HOME：" + (home == null ? "未解析" : home.flattenToShortString())
                + "\n发现的HOME候选：" + AppUtils.homeCandidates(this)
                + "\n目标桌面：" + (Prefs.targetLabel(this).isEmpty() ? "未选择" : Prefs.targetLabel(this))
                + "\n目标包名：" + Prefs.targetPkg(this)
                + "\n目标Activity：" + Prefs.targetCls(this)
                + "\n开机自动启动：" + (Prefs.bootEnabled(this) ? "开启" : "关闭")
                + "\nHOME返回目标桌面：" + (Prefs.homeEnabled(this) ? "开启" : "关闭")
                + "\n监听服务目标状态：" + (Prefs.serviceWanted(this) ? "运行" : "停止")
                + "\nLOCKED_BOOT_COMPLETED：" + Prefs.lockedBootCount(this)
                + "\nBOOT_COMPLETED：" + Prefs.bootCount(this)
                + "\nSCREEN_ON：" + Prefs.screenOnCount(this)
                + "\nUSER_UNLOCKED：" + Prefs.userUnlockedCount(this)
                + "\nCLOSE_SYSTEM_DIALOGS：" + Prefs.closeDialogsCount(this)
                + "\nreason=homekey：" + Prefs.homeKeyCount(this)
                + "\n最后一次reason：" + Prefs.lastReason(this)
                + "\n最近启动触发来源：" + Prefs.lastStartSource(this)
                + "\n目标启动尝试：" + Prefs.launchAttemptCount(this)
                + "\n目标启动成功：" + Prefs.launchSuccessCount(this)
                + "\n目标启动失败：" + Prefs.launchFailCount(this)
                + "\n最近启动错误：" + Prefs.lastLaunchError(this)
                + "\n维护剩余：" + (left > 0 ? left / 1000 + "秒" : "无")
                + "\n外部存储状态：" + storage
                + "\n固定壁纸目录：Pictures/" + WallpaperUtils.FOLDER + "/ ; Download/" + WallpaperUtils.FOLDER + "/"
                + "\n内置壁纸数量：" + WallpaperUtils.LIBRARY_COUNT + "\n已导入其他自定义壁纸：" + (WallpaperUtils.hasInternalCustom(this) ? "是" : "否")
                + "\n最近壁纸来源：" + Prefs.lastWallpaperSource(this)
                + "\n最近壁纸结果：" + Prefs.lastWallpaperStatus(this);
    }

    private void updateStatus() {
        if (status != null) status.setText(diagnostics());
    }

    private void copyDiagnostics() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("V6诊断", diagnostics()));
            toast("诊断信息已复制");
        }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private static final class AppEntry {
        final String label, pkg, cls, sourceCategory;
        final boolean isDangbei;
        AppEntry(String label, String pkg, String cls, boolean isDangbei, String sourceCategory) {
            this.label = label; this.pkg = pkg; this.cls = cls; this.isDangbei = isDangbei; this.sourceCategory = sourceCategory;
        }
        @Override public String toString() {
            return (isDangbei ? "★ " : "") + label + "  (" + pkg + ")";
        }
    }
}
