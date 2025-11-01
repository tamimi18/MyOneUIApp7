package com.example.oneuiapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import dev.oneuiproject.oneui.widget.Toast;
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private Context mContext;

    private ListPreference languagePreference;
    private ListPreference themePreference;
    private ListPreference fontPreference;
    private SwitchPreferenceCompat notificationsPreference;
    private EditTextPreference previewTextPreference;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        // syncSettingsHelper(); // ★★★ محذوف ★★★
    }

    /**
     * ★★★ تم حذف دالة syncSettingsHelper() بالكامل ★★★
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPreferences();
        setupPreferenceListeners();
    }

    private void initPreferences() {
        languagePreference = findPreference("language_mode");
        themePreference = findPreference("theme_mode");
        fontPreference = findPreference("font_mode");
        notificationsPreference = findPreference("notifications_enabled");
        previewTextPreference = findPreference("preview_text");
    }

    private void setupPreferenceListeners() {
        if (languagePreference != null) languagePreference.setOnPreferenceChangeListener(this);
        if (themePreference != null) themePreference.setOnPreferenceChangeListener(this);
        if (fontPreference != null) fontPreference.setOnPreferenceChangeListener(this);
        if (notificationsPreference != null) notificationsPreference.setOnPreferenceChangeListener(this);
        if (previewTextPreference != null) previewTextPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            mContext.getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
            int bg = tv.data;
            getView().setBackgroundColor(bg);
        } catch (Exception e) {
            getView().setBackgroundColor(0xFFFFFFFF);
        }
        // OneUI rounded list bottom corner
        getListView().seslSetLastRoundedCorner(true);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if ("language_mode".equals(key)) {
            int mode = Integer.parseInt((String) newValue);
            new SettingsHelper(mContext).setLanguageMode(mode);
            // BaseActivity تتكفل بالـ wrap، هنا نعيد إنشاء الـ Activity
            requireActivity().recreate();
            return true;

        } else if ("theme_mode".equals(key)) {
            int mode = Integer.parseInt((String) newValue);
            SettingsHelper helper = new SettingsHelper(mContext);
            helper.setThemeMode(mode);
            helper.applyTheme();
            // عادة لا نحتاج recreate فوري، لكن يمكنك إضافته لو أردت
            return true;
        } else if ("font_mode".equals(key)) {
            int mode = Integer.parseInt((String) newValue);
            SettingsHelper sh = new SettingsHelper(mContext);
            sh.setFontMode(mode);

            /* ★★★ محذوف ★★★
            try {
                // تطبيق الخط على مستوى Typeface
                FontHelper.applyFont(mContext.getApplicationContext());
            } catch (Exception e) {
                android.util.Log.e("SettingsFragment", "FontHelper.applyFont failed", e);
            }
            */

            // إعادة إنشاء كل الأنشطة عبر MyApplication (بدلاً من broadcast)
            MyApplication app = MyApplication.getInstance();
            if (app != null) {
                app.recreateAllActivities();
            } else {
                requireActivity().recreate();
            }

            return true;
        } else if ("notifications_enabled".equals(key)) {
            boolean enabled = (Boolean) newValue;
            new SettingsHelper(mContext).setNotificationsEnabled(enabled);

            // استخدام الموارد الصحيحة الموجودة في strings.xml
            String msg = enabled
                    ?
                    mContext.getString(R.string.notifications_enabled)
                    : mContext.getString(R.string.notifications_disabled);
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            return true;

        } else if ("preview_text".equals(key)) {
            String text = (String) newValue;
            new SettingsHelper(mContext).setPreviewText(text);

            Toast.makeText(
                    mContext,
                    mContext.getString(R.string.settings_preview_text) + " " +
                            mContext.getString(android.R.string.ok),
                    Toast.LENGTH_SHORT
            ).show();
            return true;
        }

        return true;
    }
}
