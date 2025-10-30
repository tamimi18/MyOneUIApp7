package com.example.oneuiapp;

import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

/**
 * BaseActivity ensures the Activity context is wrapped with app settings (locale/theme)
 * so layout direction and resources update correctly when language changes.
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        // Wrap the context with SettingsHelper which applies locale and theme preferences
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
            // ensure all child views are relaid out for the new direction
            forceRelayout(getWindow().getDecorView());
        }
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
