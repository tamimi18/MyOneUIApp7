package com.example.oneuiapp;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

/**
 * BaseActivity ensures the Activity context is wrapped with app settings (locale/theme)
 * and listens for font-change broadcasts to recreate itself.
 */
public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

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
        // Wrap context for locale first
        Context wrappedLocaleCtx = SettingsHelper.wrapContext(newBase);

        // Determine overlay resource for current font mode (0 means system / no overlay)
        int overlayRes = 0;
        try {
            SettingsHelper sh = new SettingsHelper(wrappedLocaleCtx);
            int mode = sh.getFontMode();
            switch (mode) {
                case SettingsHelper.FONT_WP:
                    overlayRes = R.style.AppFontOverlay_WP;
                    break;
                case SettingsHelper.FONT_SAMSUNG:
                    overlayRes = R.style.AppFontOverlay_Samsung;
                    break;
                case SettingsHelper.FONT_SYSTEM:
                default:
                    overlayRes = 0;
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to read font mode", e);
            overlayRes = 0;
        }

        Log.d(TAG, "attachBaseContext overlayRes=" + overlayRes);

        // Create a ContextThemeWrapper using the existing app theme, then apply overlay to it
        Context contextForAttach = wrappedLocaleCtx;
        try {
            int baseThemeRes = 0;
            try {
                baseThemeRes = newBase.getApplicationInfo().theme;
            } catch (Exception ignored) { baseThemeRes = 0; }

            ContextThemeWrapper ctxTw = (baseThemeRes != 0)
                    ? new ContextThemeWrapper(wrappedLocaleCtx, baseThemeRes)
                    : new ContextThemeWrapper(wrappedLocaleCtx, getApplicationInfo().theme);

            if (overlayRes != 0) {
                ctxTw.getTheme().applyStyle(overlayRes, true);
                Log.d(TAG, "Applied overlay style resId=" + overlayRes);
            } else {
                Log.d(TAG, "No overlay applied (system font)");
            }
            contextForAttach = ctxTw;
        } catch (Exception e) {
            Log.w(TAG, "ContextThemeWrapper creation or applyStyle failed", e);
            contextForAttach = wrappedLocaleCtx;
        }

        super.attachBaseContext(contextForAttach);
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
