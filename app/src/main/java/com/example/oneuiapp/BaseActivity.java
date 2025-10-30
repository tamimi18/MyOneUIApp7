package com.example.oneuiapp;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

/**
 * BaseActivity ensures the Activity context is wrapped with app settings (locale/theme)
 * and listens for font-change broadcasts to recreate itself.
 */
public class BaseActivity extends AppCompatActivity {

    private final android.content.BroadcastReceiver fontChangeReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (FontChangeBroadcaster.ACTION_FONT_CHANGED.equals(intent.getAction())) {
                if (!isFinishing()) {
                    runOnUiThread(() -> recreate());
                }
            }
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(SettingsHelper.wrapContext(newBase));
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

        try {
            IntentFilter f = new IntentFilter(FontChangeBroadcaster.ACTION_FONT_CHANGED);
            registerReceiver(fontChangeReceiver, f);
        } catch (Exception ignored) { }
    }

    @Override
    protected void onPause() {
        try {
            unregisterReceiver(fontChangeReceiver);
        } catch (Exception ignored) { }
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
