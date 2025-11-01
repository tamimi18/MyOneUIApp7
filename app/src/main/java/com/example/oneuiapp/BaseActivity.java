package com.example.oneuiapp;
import android.content.Context;
// import android.content.IntentFilter; // ★★★ محذوف ★★★
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
// ★★★ جديد: لاستخدام ContextThemeWrapper ★★★
import androidx.appcompat.view.ContextThemeWrapper;

/**
 * BaseActivity ensures the Activity context is wrapped with app settings (locale/theme/font)
 * ★★★ تم إزالة BroadcastReceiver لأنه سيتم الاعتماد على recreate() ★★★
 */
public class BaseActivity extends AppCompatActivity {

    /*
    private final android.content.BroadcastReceiver fontChangeReceiver = new android.content.BroadcastReceiver() {
        // ... (CODE REMOVED) ...
    };
    */ // ★★★ محذوف ★★★

    @Override
    protected void attachBaseContext(Context newBase) {
        // ★★★ معدل: تطبيق ثيم الخط أولاً، ثم اللغة ★★★
        
        // 1. تطبيق ثيم الخط
        Context themeContext = wrapContextWithFontTheme(newBase);
        
        // 2. تطبيق اللغة
        Context localeContext = SettingsHelper.wrapContext(themeContext);
        
        super.attachBaseContext(localeContext);
    }

    // ★★★ جديد: دالة لتطبيق ثيم الخط المناسب ★★★
    private Context wrapContextWithFontTheme(Context context) {
        SettingsHelper helper = new SettingsHelper(context);
        int fontMode = helper.getFontMode();
        int themeId;

        switch (fontMode) {
            case SettingsHelper.FONT_WF:
                themeId = R.style.AppTheme_Font_WF;
                break;
            case SettingsHelper.FONT_SAMSUNG:
                themeId = R.style.AppTheme_Font_Samsung;
                break;
            case SettingsHelper.FONT_SYSTEM:
            default:
                themeId = R.style.AppTheme_Font_System;
                break;
        }
        return new ContextThemeWrapper(context, themeId);
    }


    @Override
    public void applyOverrideConfiguration(android.content.res.Configuration overrideConfiguration) {
        if (overrideConfiguration == null) {
            super.applyOverrideConfiguration(null);
            return;
        }
        android.content.res.Configuration config = new android.content.res.Configuration(overrideConfiguration);
        java.util.Locale locale = SettingsHelper.getLocale(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            config.setLocales(new LocaleList(locale));
        } else {
            config.locale = locale;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale);
        }
        super.applyOverrideConfiguration(config);
    }

    @Override
    protected void onResume() {
        super.onResume();
        java.util.Locale locale = SettingsHelper.getLocale(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            int dir = TextUtils.getLayoutDirectionFromLocale(locale);
            getWindow().getDecorView().setLayoutDirection(dir == View.LAYOUT_DIRECTION_RTL ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            forceRelayout(getWindow().getDecorView());
        }

        /*
        try {
            IntentFilter f = new IntentFilter(FontChangeBroadcaster.ACTION_FONT_CHANGED);
            registerReceiver(fontChangeReceiver, f);
        } catch (Exception ignored) { }
        */ // ★★★ محذوف ★★★
    }

    @Override
    protected void onPause() {
        /*
        try {
            unregisterReceiver(fontChangeReceiver);
        } catch (Exception ignored) { }
        */ // ★★★ محذوف ★★★
        super.onPause();
    }

    // Force relayout and invalidate all children to ensure layoutDirection changes take effect
    private void forceRelayout(android.view.View v) {
        if (v == null) return;
        v.invalidate();
        v.requestLayout();
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                forceRelayout(vg.getChildAt(i));
            }
        }
    }
}
