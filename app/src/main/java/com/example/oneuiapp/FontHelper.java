package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * FontHelper - يحاول تطبيق Typeface مخصص على مستوى التطبيق.
 * تحسينات:
 *  - تسجيل مفصل لتتبّع الأخطاء في GitHub Actions logs
 *  - استبدال الحقول المعروفة في Typeface
 *  - محاولة استبدال خريطة الخطوط النظامية إذا وجدت (sSystemFontMap / sDefaults)
 *  - fallback آمن يعيد الخطوط النظامية إذا فشل شيء
 */
public class FontHelper {

    private static final String TAG = "FontHelper";

    /**
     * يقرأ Typeface من SettingsHelper ثم يحاول استبدال الحقول الثابتة في Typeface.
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

            // 1) استبدال الحقول المعروفة
            replaced |= replaceTypefaceField("DEFAULT", custom);
            replaced |= replaceTypefaceField("DEFAULT_BOLD", Typeface.create(custom, Typeface.BOLD));
            replaced |= replaceTypefaceField("SANS_SERIF", custom);
            replaced |= replaceTypefaceField("SERIF", custom);
            replaced |= replaceTypefaceField("MONOSPACE", custom);

            // 2) محاولة استبدال خريطة الخطوط النظامية (Android قد يستخدم خريطة ثابتة داخل Typeface)
            try {
                // بعض الإصدارات تحتوي على حقل باسم "sSystemFontMap" أو "sDefaults" أو "sTypefaceCache"
                // نجرب مجموعة أسماء معروفة.
                String[] candidateNames = {"sSystemFontMap", "sDefaults", "sTypefaceCache", "sSystemTypefaceMap"};
                for (String candidate : candidateNames) {
                    try {
                        Field f = Typeface.class.getDeclaredField(candidate);
                        f.setAccessible(true);
                        Object value = f.get(null);
                        if (value instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Typeface> map = (Map<String, Typeface>) value;
                            Map<String, Typeface> newMap = new HashMap<>();
                            // انسخ المفاتيح وحط نفس الـ custom كمحتوى أو حاول ملاءمة القيم
                            for (String k : map.keySet()) {
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
                            // حاول وضع خريطة جديدة عبر تحويل عام إن أمكن
                            Log.i(TAG, "Found field " + candidate + " but type is " + (value != null ? value.getClass().getName() : "null"));
                        }
                    } catch (NoSuchFieldException nsf) {
                        // تجاهل الحقل غير الموجود
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed replacing system font map: " + e.getMessage(), e);
            }

            // 3) محاولة استدعاء أي method مساعدة إن وجدت (نادرة)
            try {
                Method[] methods = Typeface.class.getDeclaredMethods();
                for (Method m : methods) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("set") && name.contains("system") && m.getParameterTypes().length > 0) {
                        m.setAccessible(true);
                        try {
                            // حاول تمرير custom إلى أي method مناسبة
                            m.invoke(null, custom);
                            Log.i(TAG, "Invoked potential Typeface setter: " + m.getName());
                            replaced = true;
                        } catch (Throwable ignore) {
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 4) Logging النهائي والـ fallback
            if (replaced) {
                Log.i(TAG, "applyFont: replacement attempts done (some fields replaced).");
            } else {
                Log.w(TAG, "applyFont: no fields replaced, will attempt reset fallback.");
                resetToSystemFonts();
            }

        } catch (Throwable t) {
            Log.e(TAG, "applyFont fatal error: " + t.getMessage(), t);
            resetToSystemFonts();
        }
    }

    /**
     * يستبدل حقل ثابت في Typeface باسم fieldName بالقيمة الجديدة newTf.
     * يعيد true إذا نجح الاستبدال.
     */
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

    /**
     * يحاول إعادة الخطوط إلى الوضع الافتراضي للنظام بأمان.
     */
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

            // حاول إعادة خريطة النظام إن أمكن
            try {
                String[] candidateNames = {"sSystemFontMap", "sDefaults", "sTypefaceCache", "sSystemTypefaceMap"};
                for (String candidate : candidateNames) {
                    try {
                        Field f = Typeface.class.getDeclaredField(candidate);
                        f.setAccessible(true);
                        Object value = f.get(null);
                        if (value instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Typeface> map = (Map<String, Typeface>) value;
                            Map<String, Typeface> newMap = new HashMap<>();
                            // أعد ملء الخريطة بقيم افتراضية
                            for (String k : map.keySet()) {
                                // حاول وضع sans كقيمة افتراضية
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
                        // لا بأس: الحقل غير موجود على هذا الإصدار
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
                }
