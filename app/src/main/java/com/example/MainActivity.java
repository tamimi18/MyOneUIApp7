package com.example.oneuiapp;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import dev.oneuiproject.oneui.layout.DrawerLayout;

/**
 * MainActivity - النسخة المصلحة نهائياً
 * 
 * ★★★ الإصلاح الكامل لمشكلة عدم ظهور أيقونة المعلومات ★★★
 * 
 * المشكلة القديمة:
 * كانت الأيقونة لا تظهر في شريط الأدوات عند تحميل خط في FontViewerFragment
 * 
 * السبب:
 * كنا نستخدم menu.clear() ثم نعيد إنشاء القائمة في كل مرة، وهذا غير موثوق
 * 
 * الحل:
 * نتبع نهج sample-app الرسمي:
 * 1. إنشاء القائمة مرة واحدة فقط في onCreateOptionsMenu()
 * 2. الاحتفاظ بمرجع للقائمة في mOptionsMenu
 * 3. إخفاء/إظهار العناصر ديناميكياً بدلاً من إعادة إنشاء القائمة
 * 4. استخدام updateOptionsMenuVisibility() بدلاً من invalidateOptionsMenu()
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
    private boolean hasFontLoaded = false;

    // ★★★ المتغير الجديد - مرجع للقائمة ★★★
    private Menu mOptionsMenu;

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
        setupToolbar();
        
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

    @Override
    protected void onResume() {
        super.onResume();
        checkFontStatus();
    }

    /**
     * ★★★ الحل الأول - إنشاء القائمة مرة واحدة فقط ★★★
     * 
     * هذه الدالة تُستدعى مرة واحدة فقط عند إنشاء Activity.
     * نُنشئ القائمة بجميع عناصرها دفعة واحدة، ثم نحتفظ بمرجع لها.
     * 
     * الفرق الأساسي عن الطريقة القديمة:
     * - القديمة: menu.clear() → inflate → return
     * - الجديدة: احفظ المرجع → inflate → حدّث الحالة → return
     * 
     * لماذا هذا أفضل؟
     * لأن Android يضمن استدعاء هذه الدالة في الوقت المناسب،
     * بينما invalidateOptionsMenu() قد لا تعمل فوراً في بعض الحالات
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // احتفظ بمرجع للقائمة لاستخدامه لاحقاً
        mOptionsMenu = menu;
        
        // أنشئ القائمة من ملف XML
        getMenuInflater().inflate(R.menu.menu_main, menu);
        
        // حدّث حالة عناصر القائمة حسب الشاشة الحالية
        updateOptionsMenuVisibility();
        
        return true;
    }

    /**
     * ★★★ الحل الثاني - تحديث حالة القائمة بدلاً من إعادة إنشائها ★★★
     * 
     * هذه الدالة هي قلب الحل الجديد.
     * تُستدعى كلما احتجنا لتحديث القائمة، وتعمل كالتالي:
     * 
     * 1. التحقق من وجود مرجع القائمة (mOptionsMenu)
     * 2. الحصول على عنصر القائمة المطلوب بواسطة ID
     * 3. تحديد هل يجب إظهاره أم إخفاؤه حسب الحالة الحالية
     * 4. استدعاء setVisible() مباشرة
     * 
     * ميزات هذا النهج:
     * - سريع جداً (لا إعادة إنشاء)
     * - موثوق 100% (تحديث فوري)
     * - متوافق مع طريقة sample-app الرسمية
     * - يعمل في جميع إصدارات Android
     * 
     * متى تُستدعى؟
     * - بعد onCreateOptionsMenu() لأول مرة
     * - عند تغيير Fragment (في showFragmentFast)
     * - عند تحميل خط جديد (في onFontChanged)
     * - عند حذف الخط (في onFontCleared)
     * - عند التحقق من حالة الخط (في checkFontStatus)
     */
    private void updateOptionsMenuVisibility() {
        // التحقق من وجود القائمة
        if (mOptionsMenu == null) {
            return;
        }
        
        // احصل على عنصر القائمة الخاص بمعلومات الخط
        MenuItem fontMetadataItem = mOptionsMenu.findItem(R.id.menu_font_metadata);
        
        if (fontMetadataItem != null) {
            // حدد هل يجب إظهار الأيقونة أم لا
            // الشروط:
            // 1. نحن في FontViewerFragment (mCurrentFragmentIndex == 2)
            // 2. تم تحميل خط (hasFontLoaded == true)
            boolean shouldShow = (mCurrentFragmentIndex == 2) && hasFontLoaded;
            
            // حدّث حالة الظهور مباشرة
            fontMetadataItem.setVisible(shouldShow);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_font_metadata) {
            Fragment currentFragment = getCurrentFragment();
            if (currentFragment instanceof FontViewerFragment) {
                ((FontViewerFragment) currentFragment).showMetadataDialog();
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private Fragment getCurrentFragment() {
        if (mCurrentFragmentIndex >= 0 && mCurrentFragmentIndex < mFragments.size()) {
            return mFragments.get(mCurrentFragmentIndex);
        }
        return null;
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
                        checkFontStatus();
                        return true;
                    }
                    return false;
                });
        
        mDrawerListView.setAdapter(mDrawerAdapter);
        mDrawerAdapter.setSelectedItem(mCurrentFragmentIndex);
    }

    /**
     * عرض Fragment بسرعة
     * 
     * ★★★ تحديث مهم: استدعاء updateOptionsMenuVisibility() بعد تغيير Fragment ★★★
     */
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
        
        // ★★★ حدّث القائمة بعد تغيير Fragment ★★★
        updateOptionsMenuVisibility();
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

    /**
     * التحقق من حالة الخط
     * 
     * ★★★ تحديث مهم: استدعاء updateOptionsMenuVisibility() بعد تحديث الحالة ★★★
     */
    private void checkFontStatus() {
        if (mCurrentFragmentIndex == 2 && mFragments.size() > 2) {
            Fragment fontFragment = mFragments.get(2);
            if (fontFragment instanceof FontViewerFragment) {
                FontViewerFragment fvf = (FontViewerFragment) fontFragment;
                
                boolean fontSelected = fvf.hasFontSelected();
                
                if (fontSelected != hasFontLoaded) {
                    hasFontLoaded = fontSelected;
                    
                    if (fontSelected) {
                        currentFontRealName = fvf.getCurrentFontRealName();
                        currentFontFileName = fvf.getCurrentFontFileName();
                        updateDrawerTitle(mCurrentFragmentIndex);
                    }
                    
                    // ★★★ حدّث القائمة بعد تحديث حالة الخط ★★★
                    updateOptionsMenuVisibility();
                }
            }
        }
    }

    /**
     * ★★★ Callback من FontViewerFragment عند تحميل خط جديد ★★★
     * 
     * تحديث مهم: استدعاء updateOptionsMenuVisibility() مباشرة
     */
    @Override
    public void onFontChanged(String fontRealName, String fontFileName) {
        this.currentFontRealName = fontRealName;
        this.currentFontFileName = fontFileName;
        this.hasFontLoaded = true;
        
        if (mCurrentFragmentIndex == 2) {
            updateDrawerTitle(mCurrentFragmentIndex);
        }
        
        // ★★★ حدّث القائمة فوراً لإظهار الأيقونة ★★★
        updateOptionsMenuVisibility();
    }
    
    /**
     * ★★★ Callback من FontViewerFragment عند حذف الخط ★★★
     * 
     * تحديث مهم: استدعاء updateOptionsMenuVisibility() مباشرة
     */
    @Override
    public void onFontCleared() {
        this.currentFontRealName = null;
        this.currentFontFileName = null;
        this.hasFontLoaded = false;
        
        if (mCurrentFragmentIndex == 2) {
            updateDrawerTitle(mCurrentFragmentIndex);
        }
        
        // ★★★ حدّث القائمة فوراً لإخفاء الأيقونة ★★★
        updateOptionsMenuVisibility();
    }

    private void setupToolbar() {
        Toolbar toolbar = mDrawerLayout.getToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
            }
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
}
