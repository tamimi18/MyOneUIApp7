package com.example.oneuiapp;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
 * MainActivity - النسخة المحدثة مع Menu ديناميكي
 * 
 * التحديثات الجديدة:
 * 1. إضافة Menu ديناميكي يظهر أيقونة المعلومات في شاشة FontViewer فقط
 * 2. التواصل مع FontViewerFragment لعرض معلومات الخط عند الضغط
 * 3. تحديث Menu تلقائياً عند تغيير Fragment
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
                        
                        // ★★★ تحديث Menu عند تغيير Fragment ★★★
                        // هذا السطر مهم جداً: يخبر Android بإعادة إنشاء Menu
                        // مما يؤدي لاستدعاء onPrepareOptionsMenu تلقائياً
                        invalidateOptionsMenu();
                        
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

    /**
     * ★★★ دالة جديدة: إنشاء Menu ★★★
     * 
     * هذه الدالة تُستدعى تلقائياً عند إنشاء Activity لأول مرة
     * نستخدمها لتضخيم (inflate) ملف menu.xml الذي يحتوي على أيقونة المعلومات
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * ★★★ دالة جديدة: تحديث Menu حسب Fragment الحالي ★★★
     * 
     * هذه الدالة تُستدعى في حالتين:
     * 1. بعد onCreateOptionsMenu مباشرة
     * 2. عندما نستدعي invalidateOptionsMenu() بأنفسنا
     * 
     * الفكرة: نتحقق من Fragment الحالي، إذا كان FontViewerFragment نُظهر الأيقونة
     * وإلا نخفيها. بهذه الطريقة الأيقونة ديناميكية وتظهر فقط عند الحاجة.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem infoItem = menu.findItem(R.id.menu_font_info);
        
        if (infoItem != null) {
            // إظهار الأيقونة فقط في شاشة FontViewer (index = 2)
            infoItem.setVisible(mCurrentFragmentIndex == 2);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * ★★★ دالة جديدة: معالجة الضغط على menu items ★★★
     * 
     * عندما يضغط المستخدم على أيقونة المعلومات، نتحقق أولاً أننا في
     * FontViewerFragment، ثم نطلب منه عرض معلومات الخط.
     * 
     * هذه الطريقة آمنة لأننا نتحقق من كل شيء قبل الاستدعاء:
     * 1. هل الـ Fragment موجود؟
     * 2. هل هو من النوع الصحيح؟
     * 3. هل الدالة موجودة؟
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_font_info) {
            // التحقق من أننا في شاشة FontViewer
            if (mCurrentFragmentIndex == 2 && mFragments.size() > 2) {
                Fragment currentFragment = mFragments.get(2);
                
                if (currentFragment instanceof FontViewerFragment) {
                    // استدعاء دالة عرض معلومات الخط
                    ((FontViewerFragment) currentFragment).showFontMetadata();
                }
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
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
        }
    }
                                     }
