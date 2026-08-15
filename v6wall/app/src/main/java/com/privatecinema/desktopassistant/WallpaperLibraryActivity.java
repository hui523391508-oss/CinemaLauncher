package com.privatecinema.desktopassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WallpaperLibraryActivity extends Activity {
    private static final int REQ_STORAGE = 701;
    private Spinner spinner;
    private ImageView preview;
    private TextView info;
    private int selected = 1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        showWallpaper(1);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(28, 24, 28, 24);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3f);
        lpLeft.setMargins(0, 0, 18, 0);
        root.addView(left, lpLeft);

        TextView title = new TextView(this);
        title.setText("内置壁纸库 · 12张");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        left.addView(title, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        previewLp.setMargins(0, 12, 0, 8);
        left.addView(preview, previewLp);

        info = new TextView(this);
        info.setTextSize(15);
        info.setGravity(Gravity.CENTER);
        left.addView(info, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lpRight = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f);
        root.addView(right, lpRight);

        List<String> names = new ArrayList<>();
        for (int i = 1; i <= WallpaperUtils.LIBRARY_COUNT; i++) names.add(WallpaperUtils.libraryLabel(i));
        spinner = new Spinner(this);
        spinner.setFocusable(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        right.addView(spinner, full());

        Button show = button("预览所选壁纸");
        show.setOnClickListener(v -> showWallpaper(spinner.getSelectedItemPosition() + 1));
        right.addView(show, full());

        Button prev = button("上一张");
        prev.setOnClickListener(v -> showWallpaper(selected <= 1 ? WallpaperUtils.LIBRARY_COUNT : selected - 1));
        right.addView(prev, full());

        Button next = button("下一张");
        next.setOnClickListener(v -> showWallpaper(selected >= WallpaperUtils.LIBRARY_COUNT ? 1 : selected + 1));
        right.addView(next, full());

        Button export = button("释放当前壁纸到固定目录");
        export.setOnClickListener(v -> exportSelected());
        right.addView(export, full());

        Button set = button("尝试将当前壁纸设为系统壁纸");
        set.setOnClickListener(v -> toast(WallpaperUtils.setLibraryAsSystemWallpaper(this, selected)));
        right.addView(set, full());

        Button all = button("一次释放全部12张壁纸");
        all.setOnClickListener(v -> exportAll());
        right.addView(all, full());

        Button target = button("打开已设置的当贝/目标桌面");
        target.setOnClickListener(v -> toast(AppUtils.launchTarget(this) ? "已发起目标桌面启动" : "目标桌面启动失败"));
        right.addView(target, full());

        Button back = button("返回桌面助手");
        back.setOnClickListener(v -> finish());
        right.addView(back, full());

        setContentView(root);
    }

    private void showWallpaper(int index) {
        selected = index;
        if (spinner != null) spinner.setSelection(index - 1);
        int resId = WallpaperUtils.libraryResId(this, index);
        if (resId != 0) preview.setImageResource(resId);
        info.setText(WallpaperUtils.libraryLabel(index) + " · 原图直接内置，不裁切、不加字、不改变构图");
    }

    private void exportSelected() {
        if (!ensureStorage()) return;
        toast(WallpaperUtils.exportLibrary(this, selected));
    }

    private void exportAll() {
        if (!ensureStorage()) return;
        toast(WallpaperUtils.exportAllLibrary(this));
    }

    private boolean ensureStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            toast("请授予存储权限，然后再点一次");
            return false;
        }
        return true;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(16);
        b.setFocusable(true);
        return b;
    }

    private LinearLayout.LayoutParams full() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 5, 0, 5);
        return p;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
