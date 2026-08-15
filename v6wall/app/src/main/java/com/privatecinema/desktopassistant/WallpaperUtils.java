package com.privatecinema.desktopassistant;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class WallpaperUtils {
    public static final String FOLDER = "私人影院壁纸";
    public static final int LIBRARY_COUNT = 12;
    public static final String CUSTOM_NAME = "私人影院桌面_自定义.jpg";
    private static final String INTERNAL_CUSTOM = "custom_wallpaper.jpg";

    private WallpaperUtils() {}

    public static int libraryResId(Context c, int index) {
        if (index < 1 || index > LIBRARY_COUNT) return 0;
        String name = String.format(java.util.Locale.US, "wallpaper_%02d", index);
        return c.getResources().getIdentifier(name, "drawable", c.getPackageName());
    }

    public static String libraryLabel(int index) {
        return String.format(java.util.Locale.CHINA, "壁纸%02d", index);
    }

    public static String libraryFileName(int index) {
        return String.format(java.util.Locale.CHINA, "私人影院壁纸_%02d.png", index);
    }

    public static boolean hasLegacyStoragePermission(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true;
        return c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public static String exportLibrary(Context c, int index) {
        int resId = libraryResId(c, index);
        if (resId == 0) return "找不到" + libraryLabel(index) + "资源";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasLegacyStoragePermission(c)) {
            return "需要先授予存储权限";
        }
        String fileName = libraryFileName(index);
        try {
            String result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri p;
                Uri d;
                try (InputStream in = c.getResources().openRawResource(resId)) {
                    p = saveRawMediaStore(c, in, fileName,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            Environment.DIRECTORY_PICTURES + "/" + FOLDER);
                }
                try (InputStream in = c.getResources().openRawResource(resId)) {
                    d = saveRawMediaStore(c, in, fileName,
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER);
                }
                result = libraryLabel(index) + " 已保存到 Pictures/" + FOLDER + "/ 和 Download/" + FOLDER + "/"
                        + "\nPictures URI=" + String.valueOf(p)
                        + "\nDownload URI=" + String.valueOf(d);
            } else {
                File pics = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER);
                File downs = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER);
                if (!pics.exists()) pics.mkdirs();
                if (!downs.exists()) downs.mkdirs();
                File p = new File(pics, fileName);
                File d = new File(downs, fileName);
                try (InputStream in = c.getResources().openRawResource(resId)) { copy(in, new FileOutputStream(p)); }
                try (InputStream in = c.getResources().openRawResource(resId)) { copy(in, new FileOutputStream(d)); }
                MediaScannerConnection.scanFile(c,
                        new String[] {p.getAbsolutePath(), d.getAbsolutePath()},
                        new String[] {"image/png", "image/png"}, null);
                result = libraryLabel(index) + " 已保存：" + p.getAbsolutePath() + "\n并复制到：" + d.getAbsolutePath();
            }
            Prefs.setWallpaperStatus(c, libraryLabel(index), result);
            return result;
        } catch (Exception e) {
            String s = libraryLabel(index) + " 保存失败：" + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
            Prefs.setWallpaperStatus(c, libraryLabel(index), s);
            return s;
        }
    }

    public static String exportAllLibrary(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasLegacyStoragePermission(c)) {
            return "需要先授予存储权限";
        }
        int ok = 0;
        String last = "";
        for (int i = 1; i <= LIBRARY_COUNT; i++) {
            last = exportLibrary(c, i);
            if (!last.contains("失败") && !last.contains("找不到") && !last.contains("需要先")) ok++;
        }
        String result = "已处理 " + ok + "/" + LIBRARY_COUNT + " 张壁纸。\n固定目录：Pictures/" + FOLDER + "/ 和 Download/" + FOLDER + "/";
        Prefs.setWallpaperStatus(c, "12张内置壁纸", result);
        return result;
    }

    public static String setLibraryAsSystemWallpaper(Context c, int index) {
        int resId = libraryResId(c, index);
        if (resId == 0) return "找不到" + libraryLabel(index) + "资源";
        try {
            WallpaperManager.getInstance(c).setResource(resId);
            String s = libraryLabel(index) + "：系统壁纸设置调用成功（当贝是否跟随系统壁纸需实机确认）";
            Prefs.setWallpaperStatus(c, libraryLabel(index), s);
            return s;
        } catch (Exception e) {
            String s = libraryLabel(index) + "：系统壁纸设置失败：" + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
            Prefs.setWallpaperStatus(c, libraryLabel(index), s);
            return s;
        }
    }

    public static String importAndExport(Context c, Uri uri) {
        Bitmap b = decodeUri(c, uri);
        if (b == null) {
            String s = "读取自定义图片失败";
            Prefs.setWallpaperStatus(c, "自定义壁纸", s);
            return s;
        }
        try {
            saveInternalCustom(c, b);
            String result = exportBitmap(c, b, CUSTOM_NAME);
            Prefs.setWallpaperStatus(c, "自定义壁纸", result);
            return result;
        } finally {
            b.recycle();
        }
    }

    public static String setCustomAsSystemWallpaper(Context c) {
        File custom = new File(c.getFilesDir(), INTERNAL_CUSTOM);
        if (!custom.exists()) return "还没有导入自定义壁纸";
        Bitmap b = null;
        try {
            b = BitmapFactory.decodeFile(custom.getAbsolutePath());
            if (b == null) throw new IllegalStateException("壁纸Bitmap为空");
            WallpaperManager.getInstance(c).setBitmap(b, null, true);
            String s = "自定义壁纸：系统壁纸设置调用成功（当贝是否跟随系统壁纸需实机确认）";
            Prefs.setWallpaperStatus(c, "自定义壁纸", s);
            return s;
        } catch (Exception e) {
            String s = "自定义壁纸设置失败：" + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
            Prefs.setWallpaperStatus(c, "自定义壁纸", s);
            return s;
        } finally {
            if (b != null) b.recycle();
        }
    }

    public static boolean hasInternalCustom(Context c) {
        return new File(c.getFilesDir(), INTERNAL_CUSTOM).exists();
    }

    private static Uri saveRawMediaStore(Context c, InputStream input, String fileName, Uri collection, String relativePath) throws Exception {
        ContentResolver cr = c.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        cv.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = cr.insert(collection, cv);
        if (uri == null) throw new IllegalStateException("MediaStore insert 返回 null");
        try (OutputStream os = cr.openOutputStream(uri, "w")) {
            if (os == null) throw new IllegalStateException("无法打开输出流");
            copy(input, os);
        }
        ContentValues done = new ContentValues();
        done.put(MediaStore.MediaColumns.IS_PENDING, 0);
        cr.update(uri, done, null, null);
        return uri;
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        try (OutputStream os = out) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) >= 0) {
                if (n > 0) os.write(buffer, 0, n);
            }
            os.flush();
        }
    }

    private static String exportBitmap(Context c, Bitmap bitmap, String fileName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasLegacyStoragePermission(c)) return "需要先授予存储权限";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri p = saveBitmapMediaStore(c, bitmap, fileName,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        Environment.DIRECTORY_PICTURES + "/" + FOLDER);
                Uri d = saveBitmapMediaStore(c, bitmap, fileName,
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER);
                return "已保存到 Pictures/" + FOLDER + "/ 和 Download/" + FOLDER + "/"
                        + "\nPictures URI=" + String.valueOf(p) + "\nDownload URI=" + String.valueOf(d);
            } else {
                File pics = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER);
                File downs = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER);
                if (!pics.exists()) pics.mkdirs();
                if (!downs.exists()) downs.mkdirs();
                File p = new File(pics, fileName);
                File d = new File(downs, fileName);
                writeJpeg(bitmap, p);
                writeJpeg(bitmap, d);
                MediaScannerConnection.scanFile(c,
                        new String[] {p.getAbsolutePath(), d.getAbsolutePath()},
                        new String[] {"image/jpeg", "image/jpeg"}, null);
                return "已保存：" + p.getAbsolutePath() + "\n并复制到：" + d.getAbsolutePath();
            }
        } catch (Exception e) {
            return "保存失败：" + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
        }
    }

    private static Uri saveBitmapMediaStore(Context c, Bitmap b, String fileName, Uri collection, String relativePath) throws Exception {
        ContentResolver cr = c.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        cv.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = cr.insert(collection, cv);
        if (uri == null) throw new IllegalStateException("MediaStore insert 返回 null");
        try (OutputStream os = cr.openOutputStream(uri, "w")) {
            if (os == null) throw new IllegalStateException("无法打开输出流");
            if (!b.compress(Bitmap.CompressFormat.JPEG, 95, os)) throw new IllegalStateException("JPEG写入失败");
        }
        ContentValues done = new ContentValues();
        done.put(MediaStore.MediaColumns.IS_PENDING, 0);
        cr.update(uri, done, null, null);
        return uri;
    }

    private static void writeJpeg(Bitmap b, File f) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            if (!b.compress(Bitmap.CompressFormat.JPEG, 95, fos)) throw new IllegalStateException("JPEG写入失败");
        }
    }

    private static void saveInternalCustom(Context c, Bitmap b) {
        try {
            File f = new File(c.getFilesDir(), INTERNAL_CUSTOM);
            writeJpeg(b, f);
        } catch (Exception ignored) {}
    }

    private static Bitmap decodeUri(Context c, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / sample > 4096 || bounds.outHeight / sample > 4096) sample *= 2;
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = sample;
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, opt);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
