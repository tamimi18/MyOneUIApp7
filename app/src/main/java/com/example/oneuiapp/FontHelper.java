package com.example.oneuiapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * FontHelper - تطبيق Typeface على مستوى التطبيق.
 * تحسينات مهمة: تستخدم applyTypefaceToView لفرض التطبيق الفوري حتى عند الرجوع لخط النظام.
 */
public class FontHelper {

    private static final String TAG = "FontHelper";

    /**
     * يقرأ Typeface من SettingsHelper ثم يحاول استبدال الحقول الثابتة في Typeface.
     * إذا كانت القيمة null فهذا يعني "استخدم خط النظام" فندعو resetToSystemFonts.
     */
    public static void applyFont(Context context) {
        try {
            if (context == null) {
                Log.w(TAG, "applyFont: context is null");
                return;
            }

            Typeface custom = SettingsHelper.getTypeface(context);
            int mode = new SettingsHelper(context).getFontMode();
            Log.i(TAG, "applyFont called. fontMode=" + mode + " customTypeface=" + (custom != null));

            if (custom == null) {
                Log.i(TAG, "No custom Typeface found, resetting to system fonts");
                resetToSystemFonts();
                return;
            }

            boolean replaced = false;

            replaced |= replaceTypefaceField("DEFAULT", custom);
            replaced |= replaceTypefaceField("DEFAULT_BOLD", Typeface.create(custom, Typeface.BOLD));
            replaced |= replaceTypefaceField("SANS_SERIF", custom);
            replaced |= replaceTypefaceField("SERIF", custom);
            replaced |= replaceTypefaceField("MONOSPACE", custom);

            // محاولة استبدال خريطة النظام إن وُجدت
            try {
                String[] candidateNames = {"sSystemFontMap", "sDefaults", "sTypefaceCache", "sSystemTypefaceMap"};
                for (String candidate : candidateNames) {
                    try {
                        Field f = Typeface.class.getDeclaredField(candidate);
                        f.setAccessible(true);
                        Object value = f.get(null);
                        if (value instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<Object, Typeface> map = (Map<Object, Typeface>) value;
                            Map<Object, Typeface> newMap = new HashMap<>();
                            for (Object k : map.keySet()) {
                                newMap.put(k, custom);
                            }
                            f.set(null, newMap);
                            Log.i(TAG, "Replaced Typeface map field: " + candidate);
                            replaced = true;
                            break;
                        } else if (value instanceof Typeface[]) {
                            Typeface[] arr = (Typeface[]) value;
                            for (int i = 0; i < arr.length; i++) arr[i] = custom;
                            f.set(null, arr);
                            Log.i(TAG, "Replaced Typeface array field: " + candidate);
                            replaced = true;
                            break;
                        } else {
                            Log.i(TAG, "Found field " + candidate + " but type is " + (value != null ? value.getClass().getName() : "null"));
                        }
                    } catch (NoSuchFieldException nsf) {
                        // ignore
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed replacing system font map: " + e.getMessage(), e);
            }

            // محاولة استدعاء ميثود مساعدة لو وُجدت
            try {
                Method[] methods = Typeface.class.getDeclaredMethods();
                for (Method m : methods) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("set") && name.contains("system") && m.getParameterTypes().length > 0) {
                        m.setAccessible(true);
                        try {
                            m.invoke(null, custom);
                            Log.i(TAG, "Invoked potential Typeface setter: " + m.getName());
                            replaced = true;
                        } catch (Throwable ignore) { }
                    }
                }
            } catch (Exception ignored) {}

            if (replaced) {
                Log.i(TAG, "applyFont: replacement attempts done (some fields replaced).");
            } else {
                Log.w(TAG, "applyFont: no fields replaced, fallback to resetToSystemFonts");
                resetToSystemFonts();
            }

        } catch (Throwable t) {
            Log.e(TAG, "applyFont fatal error: " + t.getMessage(), t);
            resetToSystemFonts();
        }
    }

    private static boolean replaceTypefaceField(String fieldName, Typeface newTf) {
        try {
            Field field = Typeface.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object old = field.get(null);
            field.set(null, newTf);
            Log.i(TAG, "Replaced Typeface field " + fieldName + " (old=" + (old != null) + ", new=" + (newTf != null) + ")");
            return true;
        } catch (NoSuchFieldException nsf) {
            Log.w(TAG, "Field not found: " + fieldName);
            return false;
        } catch (Exception e) {
            Log.w(TAG, "replaceTypefaceField failed for " + fieldName + ": " + e.getMessage(), e);
            return false;
        }
    }

    private static void resetToSystemFonts() {
        try {
            Typeface def = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
            Typeface defBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
            Typeface sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
            Typeface serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
            Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);

            replaceTypefaceField("DEFAULT", def);
            replaceTypefaceField("DEFAULT_BOLD", defBold);
            replaceTypefaceField("SANS_SERIF", sans);
            replaceTypefaceField("SERIF", serif);
            replaceTypefaceField("MONOSPACE", mono);

            // محاولة استبدال خريطة النظام إن أمكن
            try {
                String[] candidateNames = {"sSystemFontMap", "sDefaults", "sTypefaceCache", "sSystemTypefaceMap"};
                for (String candidate : candidateNames) {
                    try {
                        Field f = Typeface.class.getDeclaredField(candidate);
                        f.setAccessible(true);
                        Object value = f.get(null);
                        if (value instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<Object, Typeface> map = (Map<Object, Typeface>) value;
                            Map<Object, Typeface> newMap = new HashMap<>();
                            for (Object k : map.keySet()) {
                                newMap.put(k, sans);
                            }
                            f.set(null, newMap);
                            Log.i(TAG, "resetToSystemFonts replaced map field: " + candidate);
                            break;
                        } else if (value instanceof Typeface[]) {
                            Typeface[] arr = (Typeface[]) value;
                            for (int i = 0; i < arr.length; i++) arr[i] = sans;
                            f.set(null, arr);
                            Log.i(TAG, "resetToSystemFonts replaced array field: " + candidate);
                            break;
                        }
                    } catch (NoSuchFieldException nsf) {
                        // ignore
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "resetToSystemFonts map replacement failed: " + e.getMessage(), e);
            }

            Log.i(TAG, "resetToSystemFonts: done");
        } catch (Exception e) {
            Log.e(TAG, "resetToSystemFonts failed: " + e.getMessage(), e);
        }
    }

    /**
     * يطبّق Typeface المعطى مباشرة على شجرة العرض بدءاً من root.
     * إذا كان typeface == null فإننا نطبّق Typeface.DEFAULT صراحة.
     */
    public static void applyTypefaceToView(View root, Typeface tf) {
        if (root == null) return;
        if (tf == null) {
            tf = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        }

        try {
            if (root instanceof TextView) {
                ((TextView) root).setTypeface(tf);
                root.invalidate();
                root.requestLayout();
            } else if (root instanceof Button) {
                ((Button) root).setTypeface(tf);
                root.invalidate();
                root.requestLayout();
            } else if (root instanceof EditText) {
                ((EditText) root).setTypeface(tf);
                root.invalidate();
                root.requestLayout();
            } else if (root instanceof Toolbar) {
                Toolbar tb = (Toolbar) root;
                // force title reapply
                try {
                    CharSequence t = tb.getTitle();
                    tb.setTitle(t);
                } catch (Exception ignored) { }
                for (int i = 0; i < tb.getChildCount(); i++) {
                    View c = tb.getChildAt(i);
                    applyTypefaceToView(c, tf);
                }
            }

            if (root instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) root;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    applyTypefaceToView(child, tf);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "applyTypefaceToView failed on view " + root.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * يطبّق الـ Typeface على كل عناصر Activity (عن طريق decorView).
     * يمرر null ليعني تطبيق خط النظام الافتراضي.
     */
    public static void applyTypefaceToActivity(Activity a, Typeface tf) {
        if (a == null) return;
        try {
            View decor = a.getWindow().getDecorView();
            applyTypefaceToView(decor, tf);
        } catch (Exception e) {
            Log.w(TAG, "applyTypefaceToActivity failed: " + e.getMessage(), e);
        }
    }
                      }
