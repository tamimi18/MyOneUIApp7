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

/**
 * SettingsFragment - شاشة الإعدادات
 * 
 * الإصلاحات:
 * 1. حفظ نص المعاينة يتم مباشرة
 * 2. FontViewerFragment ستستقبل التغيير تلقائياً عبر onResume()
 */
public class SettingsFragment extends PreferenceFragmentCompat 
        implements Preference.OnPreferenceChangeListener {

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
        syncSettingsHelper();
    }

    /**
     * مزامنة SettingsHelper مع PreferenceManager
     */
    private void syncSettingsHelper() {
        try {
            SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences oldPrefs = mContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            
            if (oldPrefs.getAll().size() > 0 && defaultPrefs.getAll().size() == 0) {
                SharedPreferences.Editor editor = defaultPrefs.edit();
                
                for (String key : oldPrefs.getAll().keySet()) {
                    Object value = oldPrefs.getAll().get(key);
                    
                    if (value instanceof String) {
                        editor.putString(key, (String) value);
                    } else if (value instanceof Integer) {
                        editor.putString(key, String.valueOf(value));
                    } else if (value instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) value);
                    }
                }
                
                editor.apply();
            }
            
        } catch (Exception e) {
            android.util.Log.e("SettingsFragment", "Error syncing preferences", e);
        }
    }

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
        if (languagePreference != null) {
            languagePreference.setOnPreferenceChangeListener(this);
        }
        
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener(this);
        }
        
        if (fontPreference != null) {
            fontPreference.setOnPreferenceChangeListener(this);
        }
        
        if (notificationsPreference != null) {
            notificationsPreference.setOnPreferenceChangeListener(this);
        }
        
        if (previewTextPreference != null) {
            previewTextPreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            mContext.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
            int backgroundColor = typedValue.data;
            getView().setBackgroundColor(backgroundColor);
        } catch (Exception e) {
            getView().setBackgroundColor(0xFFFFFFFF);
        }
        
        getListView().seslSetLastRoundedCorner(true);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        
        if ("language_mode".equals(key)) {
            String newLanguageMode = (String) newValue;
            int languageMode = Integer.parseInt(newLanguageMode);
            
            new SettingsHelper(mContext).setLanguageMode(languageMode);
            requireActivity().recreate();
            
            return true;
        }
        
        else if ("theme_mode".equals(key)) {
            String newThemeMode = (String) newValue;
            int themeMode = Integer.parseInt(newThemeMode);
            
            SettingsHelper helper = new SettingsHelper(mContext);
            helper.setThemeMode(themeMode);
            helper.applyTheme();
            
            return true;
        }
        
        else if ("font_mode".equals(key)) {
            String newFontMode = (String) newValue;
            int fontMode = Integer.parseInt(newFontMode);
            
            new SettingsHelper(mContext).setFontMode(fontMode);
            FontHelper.applyFont(mContext);
            requireActivity().recreate();
            
            return true;
        }
        
        else if ("notifications_enabled".equals(key)) {
            Boolean isEnabled = (Boolean) newValue;
            
            new SettingsHelper(mContext).setNotificationsEnabled(isEnabled);
            
            String message = isEnabled 
                    ? mContext.getString(R.string.notifications_enabled)
                    : mContext.getString(R.string.notifications_disabled);
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            
            return true;
        }
        
        /**
         * حفظ نص المعاينة الجديد
         * 
         * الطريقة:
         * 1. نحفظ النص مباشرة في SharedPreferences
         * 2. لا نحتاج لعمل أي شيء إضافي
         * 3. عندما يعود المستخدم إلى FontViewerFragment
         *    سيتم استدعاء onResume() تلقائياً
         * 4. onResume() ستلاحظ تغيير النص وتحدّث المعاينة
         */
        else if ("preview_text".equals(key)) {
            String newPreviewText = (String) newValue;
            
            // حفظ النص الجديد
            new SettingsHelper(mContext).setPreviewText(newPreviewText);
            
            // عرض رسالة تأكيد
            Toast.makeText(mContext, 
                mContext.getString(R.string.settings_preview_text) + " " +
                mContext.getString(android.R.string.ok), 
                Toast.LENGTH_SHORT).show();
            
            // ملاحظة: لا حاجة لإعادة إنشاء Activity أو إشعار Fragment
            // لأن FontViewerFragment.onResume() ستتحقق من التغيير تلقائياً
            
            return true;
        }
        
        return true;
    }
        }
