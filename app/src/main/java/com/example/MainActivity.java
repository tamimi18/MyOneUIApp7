package com.example.oneuiapp;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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
 * MainActivity - الحل الصحيح والنهائي
 * 
 * ═══════════════════════════════════════════════════════════════════════
 * فهم المشكلة الأساسية
 * ═══════════════════════════════════════════════════════════════════════
 * 
 * في البداية، حاولنا استخدام نظام Menu القياسي في Android عبر:
 * - onCreateOptionsMenu()
 * - onPrepareOptionsMenu()
 * - onOptionsItemSelected()
 * 
 * لكن هذا لم يعمل لسببين:
 * 
 * 1. DrawerLayout من OneUI Design يُدير Toolbar بشكل داخلي
 *    وعندما حاولنا ربطه بـ setSupportActionBar()، تعارض
 *    هذا مع آلية عمل DrawerLayout وتسبب في تعطل زر الدرج
 * 
 * 2. في sample-app الرسمي، عندما يحتاجون لإضافة أزرار إضافية
 *    في Toolbar، يستخدمون طريقة مختلفة تماماً:
 *    - في AboutActivity: يستخدمون setDrawerButtonIcon()
 *    - في MainActivity: لا يضيفون أي أزرار إضافية أصلاً!
 * 
 * ═══════════════════════════════════════════════════════════════════════
 * الحل الصحيح (مستوحى من sample-app)
 * ═══════════════════════════════════════════════════════════════════════
 * 
 * DrawerLayout يوفر API خاص لإضافة زر إضافي في الجهة المقابلة لزر الدرج:
 * 
 * - setDrawerButtonIcon(): لتعيين أيقونة الزر
 * - setDrawerButtonTooltip(): لتعيين نص توضيحي عند الضغط المطول
 * - setDrawerButtonOnClickListener(): لتعيين حدث الضغط
 * - setDrawerButtonVisibility(): لإظهار/إخفاء الزر
 * 
 * هذه هي الطريقة الصحيحة للعمل مع DrawerLayout!
 * ولن تتعارض مع آلية عمل الدرج الداخلية
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
    
    // متغيرات لحفظ معلومات الخط الحالي
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
        
        // ★★★ إعداد زر المعلومات - هذا هو الحل الصحيح! ★★★
        setupInfoButton();
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
                        
                        // ★★★ تحديث رؤية زر المعلومات عند تغيير Fragment ★★★
                        updateInfoButtonVisibility();
                        
                        return true;
                    }
                    return false;
                });
        
        mDrawerListView.setAdapter(mDrawerAdapter);
        mDrawerAdapter.setSelectedItem(mCurrentFragmentIndex);
    }

    /**
     * ★★★ إعداد زر المعلومات - الطريقة الصحيحة ★★★
     * 
     * هذه الدالة تستخدم API الخاص بـ DrawerLayout لإضافة زر إضافي
     * في الجهة المقابلة لزر فتح الدرج.
     * 
     * كيف تعمل؟
     * ----------
     * DrawerLayout يحتوي على "مكان مخصص" لزر إضافي واحد. هذا الزر:
     * - يظهر في الزاوية (يمين في LTR، يسار في RTL)
     * - لا يتعارض مع زر الدرج أبداً
     * - يمكن إظهاره/إخفاؤه بسهولة
     * - يمكن تغيير أيقونته وحدث الضغط عليه
     * 
     * هذا بالضبط ما نحتاجه!
     * 
     * الفرق بين هذه الطريقة و setSupportActionBar():
     * ------------------------------------------------
     * setSupportActionBar() يحاول "السيطرة" على Toolbar بالكامل
     * بينما setDrawerButton...() يعمل "بتناغم" مع DrawerLayout
     * 
     * مثال من sample-app:
     * --------------------
     * في AboutActivity.java (السطر 76-80 تقريباً):
     * 
     * mBinding.aboutDrawerLayout.setDrawerButtonIcon(
     *     getDrawable(R.drawable.ic_oui_info_outline));
     * mBinding.aboutDrawerLayout.setDrawerButtonTooltip("About page");
     * mBinding.aboutDrawerLayout.setDrawerButtonOnClickListener(v -> ...);
     * 
     * نفس الفكرة هنا!
     */
    private void setupInfoButton() {
        // الحصول على أيقونة المعلومات من مكتبة OneUI Icons
        // نستخدم ic_oui_info_outline لأنها تتماشى مع تصميم OneUI
        try {
            // محاولة الحصول على الأيقونة من OneUI Icons
            Class<?> ouiDrawable = Class.forName("dev.oneuiproject.oneui.R$drawable");
            java.lang.reflect.Field iconField = ouiDrawable.getField("ic_oui_info_outline");
            int iconResId = iconField.getInt(null);
            
            android.graphics.drawable.Drawable infoIcon = getDrawable(iconResId);
            
            // تعيين أيقونة الزر
            mDrawerLayout.setDrawerButtonIcon(infoIcon);
            
            // تعيين نص توضيحي (يظهر عند الضغط المطول)
            mDrawerLayout.setDrawerButtonTooltip(getString(R.string.menu_font_info));
            
            // تعيين حدث الضغط
            mDrawerLayout.setDrawerButtonOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onInfoButtonClicked();
                }
            });
            
            // إخفاء الزر افتراضياً (سنُظهره فقط في شاشة FontViewer)
            mDrawerLayout.setDrawerButtonVisibility(View.GONE);
            
        } catch (Exception e) {
            // في حالة فشل الحصول على الأيقونة من OneUI Icons
            // نستخدم أيقونة النظام البديلة
            android.util.Log.e("MainActivity", "Failed to get OneUI icon, using fallback", e);
            
            // استخدام أيقونة info من Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.graphics.drawable.Drawable fallbackIcon = 
                    getDrawable(android.R.drawable.ic_menu_info_details);
                
                if (fallbackIcon != null) {
                    mDrawerLayout.setDrawerButtonIcon(fallbackIcon);
                    mDrawerLayout.setDrawerButtonTooltip(getString(R.string.menu_font_info));
                    mDrawerLayout.setDrawerButtonOnClickListener(v -> onInfoButtonClicked());
                    mDrawerLayout.setDrawerButtonVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * ★★★ تحديث رؤية زر المعلومات ★★★
     * 
     * هذه الدالة تُستدعى في حالتين:
     * 1. عند تغيير Fragment (من setupDrawer)
     * 2. يمكن استدعاؤها يدوياً عند الحاجة
     * 
     * القاعدة بسيطة:
     * - إذا كنا في FontViewerFragment (index = 2): أظهر الزر
     * - في أي شاشة أخرى: أخفِ الزر
     */
    private void updateInfoButtonVisibility() {
        if (mDrawerLayout == null) {
            return;
        }
        
        // إظهار الزر فقط في شاشة FontViewer (index = 2)
        if (mCurrentFragmentIndex == 2) {
            mDrawerLayout.setDrawerButtonVisibility(View.VISIBLE);
        } else {
            mDrawerLayout.setDrawerButtonVisibility(View.GONE);
        }
    }

    /**
     * ★★★ معالجة الضغط على زر المعلومات ★★★
     * 
     * عندما يضغط المستخدم على زر المعلومات:
     * 1. نتحقق أننا فعلاً في FontViewerFragment (للأمان المضاعف)
     * 2. نتحقق أن Fragment من النوع الصحيح
     * 3. نطلب من Fragment عرض معلومات الخط
     * 
     * التحققات الإضافية مهمة لتجنب الأخطاء في حالات غير متوقعة
     * (مثل لو تم تغيير Fragment بسرعة جداً أثناء الضغط)
     */
    private void onInfoButtonClicked() {
        // التحقق من أننا في شاشة FontViewer
        if (mCurrentFragmentIndex != 2 || mFragments.size() <= 2) {
            return;
        }
        
        Fragment currentFragment = mFragments.get(2);
        
        // التحقق من أن Fragment من النوع الصحيح
        if (!(currentFragment instanceof FontViewerFragment)) {
            return;
        }
        
        FontViewerFragment fontViewerFragment = (FontViewerFragment) currentFragment;
        
        // استدعاء دالة عرض معلومات الخط
        fontViewerFragment.showFontMetadata();
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

    /**
     * تحديث عنوان الدرج بناءً على Fragment المعروض
     */
    private void updateDrawerTitle(int fragmentIndex) {
        if (mDrawerLayout == null) {
            return;
        }
        
        String title;
        String subtitle;
        
        if (fragmentIndex == 0) {
            // الشاشة الرئيسية (HomeFragment)
            title = getString(R.string.app_name);
            subtitle = getString(R.string.app_subtitle);
            
        } else if (fragmentIndex == 1) {
            // شاشة الإعدادات (SettingsFragment)
            title = getString(R.string.title_settings);
            subtitle = getString(R.string.settings_subtitle);
            
        } else if (fragmentIndex == 2) {
            // شاشة عارض الخطوط (FontViewerFragment)
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

    /**
     * Callbacks من FontViewerFragment
     */
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
            updateInfoButtonVisibility();
        }
    }
        }
