package com.example.oneuiapp;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private static CrashHandler instance;
    private Thread.UncaughtExceptionHandler defaultHandler;
    private Context ctx;

    private CrashHandler() {}

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new CrashHandler();
            instance.ctx = context.getApplicationContext();
            instance.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(instance);
            Log.i(TAG, "CrashHandler initialized");
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            String stack = getStackTrace(e);
            String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
            StringBuilder report = buildReport(t, stack, ts);

            String fileName = sanitizeFileName("crash_" + ts + ".txt");

            boolean logged = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                logged = writeToDownloadsMediaStore(report.toString(), fileName);
                if (!logged) {
                    logged = writeToAppExternalFiles(report.toString(), fileName);
                }
            } else {
                // Legacy devices: try external public downloads, then app external files
                logged = writeToPublicDownloadsLegacy(report.toString(), fileName);
                if (!logged) {
                    logged = writeToAppExternalFiles(report.toString(), fileName);
                }
            }

            if (logged) {
                Log.e(TAG, "Crash logged: " + fileName);
            } else {
                Log.w(TAG, "Crash not logged to storage");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to log crash", ex);
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        } else {
            System.exit(2);
        }
    }

    private String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private StringBuilder buildReport(Thread t, String stack, String ts) {
        StringBuilder report = new StringBuilder();
        report.append("Timestamp: ").append(ts).append("\n");
        report.append("Thread: ").append(t != null ? t.getName() : "unknown").append("\n");
        report.append("Package: ").append(ctx != null ? ctx.getPackageName() : "unknown").append("\n");
        report.append("Device: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
        report.append("SDK: ").append(android.os.Build.VERSION.SDK_INT).append("\n\n");
        report.append(stack);
        return report;
    }

    private String sanitizeFileName(String name) {
        // Remove characters not safe for filenames
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean writeToDownloadsMediaStore(String content, String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OneUIApp");

            Uri uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Log.w(TAG, "MediaStore insert returned null URI");
                return false;
            }

            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri);
                 OutputStreamWriter ow = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                 BufferedWriter bw = new BufferedWriter(ow)) {

                if (os == null) {
                    Log.w(TAG, "OutputStream from MediaStore is null");
                    return false;
                }

                bw.write(content);
                bw.flush();
                return true;
            } catch (Exception ioe) {
                Log.w(TAG, "Failed writing to MediaStore URI", ioe);
                return false;
            }
        } catch (SecurityException se) {
            Log.w(TAG, "No permission to write to MediaStore", se);
            return false;
        } catch (Exception ex) {
            Log.w(TAG, "Unexpected MediaStore error", ex);
            return false;
        }
    }

    private boolean writeToPublicDownloadsLegacy(String content, String fileName) {
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads == null) {
                Log.w(TAG, "Public downloads directory is null");
                return false;
            }
            File dir = new File(downloads, "OneUIApp");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Failed to create directory: " + dir.getAbsolutePath());
            }
            File out = new File(dir, fileName);
            try (FileWriter fw = new FileWriter(out, false)) {
                fw.write(content);
                fw.flush();
                return true;
            } catch (Exception ioex) {
                Log.w(TAG, "Failed writing to legacy public downloads", ioex);
                return false;
            }
        } catch (Exception ex) {
            Log.w(TAG, "Error accessing legacy public downloads", ex);
            return false;
        }
    }

    private boolean writeToAppExternalFiles(String content, String fileName) {
        try {
            File appDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (appDir == null) {
                Log.w(TAG, "App external files dir is null");
                return false;
            }
            File dir = new File(appDir, "OneUIApp");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Failed to create app external dir: " + dir.getAbsolutePath());
            }
            File out = new File(dir, fileName);
            try (FileWriter fw = new FileWriter(out, false)) {
                fw.write(content);
                fw.flush();
                Log.i(TAG, "Crash written to app external files: " + out.getAbsolutePath());
                return true;
            } catch (Exception ioex) {
                Log.w(TAG, "Failed writing to app external files", ioex);
                return false;
            }
        } catch (Exception ex) {
            Log.w(TAG, "Error accessing app external files", ex);
            return false;
        }
    }
}
