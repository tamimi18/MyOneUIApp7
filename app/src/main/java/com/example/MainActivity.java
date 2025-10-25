package com.example.oneuiapp;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import dev.oneuiproject.oneui.layout.DrawerLayout;

/**
 * MainActivity - الحل النهائي والصحيح
 * 
 * بعد دراسة sample-app بعمق، اتضح أن:
 * 
 * 1. DrawerLayout لا يوفر دالة setDrawerButtonVisibility()
 * 2. الطريقة الوحيدة للتحكم بالزر هي عبر تغيير الأيقونة (null = مخفي)
 * 3. لكن هذا معقد ويحتاج منطق إضافي
 * 
 * الحل الأبسط والأفضل:
 * --------------------
 * نترك MainActivity بسيطة، ونضع منطق الزر بالكامل في FontViewerFragment.
 * هكذا يدير كل Fragment أزراره الخاصة، وهذا أكثر تنظيماً وأقرب لمبادئ Android.
 * 
 * MainActivity الآن تعود لحالتها الأصلية البسيطة!
 */
public class MainActivity extends BaseActivity implements FontViewerFragment.OnFontChangedListener {

    private DrawerLayout mDrawerLayout;
    private RecyclerView mDrawerListView;
    private DrawerListAdapter mDrawerAdapter;
    private List<Fragment> mFragments = new ArrayList<>();
    private int mCurrentFragmentIndex = 0;
    
    private static final String KEY_CURRENT_FRAGMENT = "current_fragment_index";
    private static final String TAG_HOME = "fragment_home";
    private static final String TAG_FONT_VIEWER = "fragment_font_viewer";
    
    private String currentFontRealName;
    private String currentFontFileName;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(SettingsHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initFragmentsList();
        
        if (savedInstanceState != null) {
            mCurrentFragmentIndex = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, 0);
            
            FragmentManager fm = getSupportFragmentManager();
            Fragment homeFragment = fm.findFragmentByTag(TAG_HOME);
            Fragment settingsFragment = fm.findFragmentByTag("settings");
            Fragment fontViewerFragment = fm.findFragmentByTag(TAG_FONT_VIEWER);
            
            if (homeFragment != null && settingsFragment != null && fontViewerFragment != null) {
                mFragments.clear();
                mFragments.add(homeFragment);
                mFragments.add(settingsFragment);
                mFragments.add(fontViewerFragment);
            }
            
            showFragmentFast(mCurrentFragmentIndex);
        } else {
            addAllFragments();
        }
        
        setupDrawer();
        updateDrawerTitle(mCurrentFragmentIndex);
    }

    private void initViews() {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerListView = findViewById(R.id.drawer_list_view);
    }

    private void initFragmentsList() {
        if (mFragments.isEmpty()) {
            mFragments.add(new HomeFragment());
            mFragments.add(new SettingsFragment());
            mFragments.add(new FontViewerFragment());
        }
    }

    private void addAllFragments() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        
        transaction.add(R.id.main_content, mFragments.get(0), TAG_HOME);
        transaction.add(R.id.main_content, mFragments.get(1), "settings");
        transaction.hide(mFragments.get(1));
        transaction.add(R.id.main_content, mFragments.get(2), TAG_FONT_VIEWER);
        transaction.hide(mFragments.get(2));
        
        transaction.commit();
    }

    private void setupDrawer() {
        mDrawerListView.setLayoutManager(new LinearLayoutManager(this));
        mDrawerAdapter = new DrawerListAdapter(
                this,
                mFragments,
                position -> {
                    mDrawerLayout.setDrawerOpen(false, true);
                    
                    if (position != mCurrentFragmentIndex) {
                        mCurrentFragmentIndex = position;
                        showFragmentFast(position);
                        updateDrawerTitle(position);
                        return true;
                    }
                    return false;
                });
        
        mDrawerListView.setAdapter(mDrawerAdapter);
        mDrawerAdapter.setSelectedItem(mCurrentFragmentIndex);
    }

    private void showFragmentFast(int position) {
        if (position < 0 || position >= mFragments.size()) {
            return;
        }
        
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        
        for (int i = 0; i < mFragments.size(); i++) {
            Fragment fragment = mFragments.get(i);
            if (fragment.isAdded()) {
                transaction.hide(fragment);
            }
        }
        
        Fragment targetFragment = mFragments.get(position);
        if (targetFragment.isAdded()) {
            transaction.show(targetFragment);
        }
        
        transaction.commitNow();
    }

    private void updateDrawerTitle(int fragmentIndex) {
        if (mDrawerLayout == null) {
            return;
        }
        
        String title;
        String subtitle;
        
        if (fragmentIndex == 0) {
            title = getString(R.string.app_name);
            subtitle = getString(R.string.app_subtitle);
            
        } else if (fragmentIndex == 1) {
            title = getString(R.string.title_settings);
            subtitle = getString(R.string.settings_subtitle);
            
        } else if (fragmentIndex == 2) {
            FontViewerFragment fontFragment = (FontViewerFragment) mFragments.get(2);
            
            if (fontFragment != null && fontFragment.hasFontSelected()) {
                currentFontRealName = fontFragment.getCurrentFontRealName();
                currentFontFileName = fontFragment.getCurrentFontFileName();
            }
            
            if (currentFontRealName != null && !currentFontRealName.isEmpty()) {
                title = currentFontRealName;
                subtitle = currentFontFileName != null ? currentFontFileName 
                         : getString(R.string.font_viewer_select_description);
            } else {
                title = getString(R.string.drawer_font_viewer);
                subtitle = getString(R.string.font_viewer_select_description);
            }
            
        } else {
            title = getString(R.string.app_name);
            subtitle = getString(R.string.app_subtitle);
        }
        
        mDrawerLayout.setTitle(title);
        mDrawerLayout.setExpandedSubtitle(subtitle);
    }

    @Override
    public void onFontChanged(String fontRealName, String fontFileName) {
        this.currentFontRealName = fontRealName;
        this.currentFontFileName = fontFileName;
        
        if (mCurrentFragmentIndex == 2) {
            updateDrawerTitle(mCurrentFragmentIndex);
        }
    }
    
    @Override
    public void onFontCleared() {
        this.currentFontRealName = null;
        this.currentFontFileName = null;
        
        if (mCurrentFragmentIndex == 2) {
            updateDrawerTitle(mCurrentFragmentIndex);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_FRAGMENT, mCurrentFragmentIndex);
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O && isTaskRoot()) {
            finishAfterTransition();
        } else {
            super.onBackPressed();
        }
    }
    
    public void updateDrawerSelection(int position) {
        if (mDrawerAdapter != null && position >= 0 && position < mFragments.size()) {
            mCurrentFragmentIndex = position;
            mDrawerAdapter.setSelectedItem(position);
            updateDrawerTitle(position);
        }
    }
    
    /**
     * دالة مساعدة للحصول على DrawerLayout
     * FontViewerFragment سيستخدمها لإضافة الزر
     */
    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }
}


// ═══════════════════════════════════════════════════════════════════════
// ملف 2: FontViewerFragment.java - الجزء المُعدّل فقط
// ═══════════════════════════════════════════════════════════════════════
// 
// أضف هذه الدوال في نهاية الكلاس، قبل القوس الأخير }
// ═══════════════════════════════════════════════════════════════════════

    /**
     * ★★★ إعداد زر المعلومات في Toolbar ★★★
     * 
     * هذه الدالة تُستدعى عندما يصبح Fragment مرئياً (onResume)
     * وتضيف زر المعلومات إلى DrawerLayout الخاص بـ MainActivity
     * 
     * لماذا في onResume وليس onCreate؟
     * -----------------------------------
     * لأن في onCreate قد لا يكون Activity جاهزة بعد،
     * أما في onResume فكل شيء جاهز ومؤكد أن Fragment مرئي للمستخدم
     * 
     * كيف يعمل؟
     * ----------
     * 1. نحصل على DrawerLayout من MainActivity
     * 2. نضيف أيقونة المعلومات كـ DrawerButton
     * 3. نربط حدث الضغط بدالة showFontMetadata()
     */
    private void setupInfoButton() {
        // الحصول على MainActivity
        if (!(getActivity() instanceof MainActivity)) {
            return;
        }
        
        MainActivity mainActivity = (MainActivity) getActivity();
        dev.oneuiproject.oneui.layout.DrawerLayout drawerLayout = mainActivity.getDrawerLayout();
        
        if (drawerLayout == null) {
            return;
        }
        
        try {
            // الحصول على أيقونة المعلومات من OneUI Icons
            Class<?> ouiDrawable = Class.forName("dev.oneuiproject.oneui.R$drawable");
            java.lang.reflect.Field iconField = ouiDrawable.getField("ic_oui_info_outline");
            int iconResId = iconField.getInt(null);
            
            android.graphics.drawable.Drawable infoIcon = requireContext().getDrawable(iconResId);
            
            // إضافة الزر إلى DrawerLayout
            drawerLayout.setDrawerButtonIcon(infoIcon);
            drawerLayout.setDrawerButtonTooltip(getString(R.string.menu_font_info));
            drawerLayout.setDrawerButtonOnClickListener(v -> showFontMetadata());
            
        } catch (Exception e) {
            android.util.Log.e("FontViewerFragment", "Failed to setup info button", e);
            
            // محاولة استخدام أيقونة بديلة من Android
            try {
                android.graphics.drawable.Drawable fallbackIcon = 
                    requireContext().getDrawable(android.R.drawable.ic_menu_info_details);
                
                if (fallbackIcon != null) {
                    drawerLayout.setDrawerButtonIcon(fallbackIcon);
                    drawerLayout.setDrawerButtonTooltip(getString(R.string.menu_font_info));
                    drawerLayout.setDrawerButtonOnClickListener(v -> showFontMetadata());
                }
            } catch (Exception ex) {
                // تجاهل إذا فشل كل شيء
            }
        }
    }

    /**
     * ★★★ إزالة زر المعلومات من Toolbar ★★★
     * 
     * هذه الدالة تُستدعى عندما يختفي Fragment (onPause)
     * وتزيل زر المعلومات من DrawerLayout
     * 
     * لماذا نزيله؟
     * -------------
     * لأننا لا نريد الزر يظهر في الشاشات الأخرى
     * عندما يضيف الزر، يبقى موجوداً حتى نزيله يدوياً
     * 
     * كيف نزيله؟
     * ----------
     * نمرر null كأيقونة، هذا يخفي الزر تماماً
     */
    private void removeInfoButton() {
        if (!(getActivity() instanceof MainActivity)) {
            return;
        }
        
        MainActivity mainActivity = (MainActivity) getActivity();
        dev.oneuiproject.oneui.layout.DrawerLayout drawerLayout = mainActivity.getDrawerLayout();
        
        if (drawerLayout == null) {
            return;
        }
        
        // إزالة الزر عن طريق تمرير null
        drawerLayout.setDrawerButtonIcon(null);
        drawerLayout.setDrawerButtonOnClickListener(null);
    }

    // ★★★ تعديل دالة onResume الموجودة ★★★
    // استبدل دالة onResume بهذه النسخة المُعدّلة
    @Override
    public void onResume() {
        super.onResume();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        String currentPreviewText = SettingsHelper.getPreviewText(requireContext());

        if (!currentPreviewText.equals(lastPreviewText)) {
            lastPreviewText = currentPreviewText;
            updatePreviewTexts();
        }
        
        // ★★★ إضافة جديدة: إعداد زر المعلومات ★★★
        setupInfoButton();
    }

    // ★★★ تعديل دالة onPause الموجودة ★★★
    // استبدل دالة onPause بهذه النسخة المُعدّلة
    @Override
    public void onPause() {
        super.onPause();
        
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        
        // ★★★ إضافة جديدة: إزالة زر المعلومات ★★★
        removeInfoButton();
}
