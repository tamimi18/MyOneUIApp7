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
 * MainActivity - الإصلاح النهائي الصحيح
 * 
 * المشكلة الحقيقية كانت:
 * - نستدعي invalidateOptionsMenu() قبل تحديث hasFontLoaded
 * - عند تحميل آخر خط محفوظ، onFontChanged() يُستدعى لكن القائمة لا تُحدّث
 * 
 * الحل:
 * - نستدعي invalidateOptionsMenu() داخل onFontChanged() نفسها
 * - نتأكد من تحديث القائمة كل مرة يتغير فيها حالة الخط
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

    private void setupToolbar() {
        Toolbar toolbar = mDrawerLayout.getToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
            }
        }
    }

    /**
     * ★★★ إنشاء القائمة بناءً على Fragment النشط ★★★
     * 
     * هذه الدالة تُستدعى:
     * 1. عند إنشاء Activity أول مرة
     * 2. بعد استدعاء invalidateOptionsMenu()
     * 3. عند تغيير Fragment
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        menu.clear();
        
        // ★★★ إذا كنا في FontViewerFragment وهناك خط محمّل، أظهر الأيقونة ★★★
        if (mCurrentFragmentIndex == 2 && hasFontLoaded) {
            getMenuInflater().inflate(R.menu.menu_font_viewer, menu);
            return true;
        }
        
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * معالجة الضغط على الأيقونة
     */
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
        
        // تحديث القائمة بعد تبديل Fragment
        invalidateOptionsMenu();
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
     * ★★★ الإصلاح الحاسم - تُستدعى من FontViewerFragment عند تحميل خط ★★★
     * 
     * خطوات التنفيذ الصحيحة:
     * 1. تحديث المتغيرات أولاً
     * 2. تحديث العنوان إذا كنا في FontViewerFragment
     * 3. استدعاء invalidateOptionsMenu() لإعادة إنشاء القائمة
     * 
     * الترتيب مهم جداً! يجب تحديث hasFontLoaded قبل invalidateOptionsMenu()
     */
    @Override
    public void onFontChanged(String fontRealName, String fontFileName) {
        // ★★★ الخطوة 1: تحديث المتغيرات أولاً ★★★
        this.currentFontRealName = fontRealName;
        this.currentFontFileName = fontFileName;
        this.hasFontLoaded = true;
        
        // ★★★ الخطوة 2: تحديث العنوان إذا كنا في FontViewerFragment ★★★
        if (mCurrentFragmentIndex == 2) {
            updateDrawerTitle(mCurrentFragmentIndex);
        }
        
        // ★★★ الخطوة 3: إعادة إنشاء القائمة - هذا هو المفتاح! ★★★
        // الآن hasFontLoaded = true، لذلك onCreateOptionsMenu() ستُظهر الأيقونة
        invalidateOptionsMenu();
    }
    
    /**
     * ★★★ تُستدعى من FontViewerFragment عند حذف الخط ★★★
     */
    @Override
    public void onFontCleared() {
        // تحديث المتغيرات أولاً
        this.currentFontRealName = null;
        this.currentFontFileName = null;
        this.hasFontLoaded = false;
        
        // تحديث العنوان إذا كنا في FontViewerFragment
        if (mCurrentFragmentIndex == 2) {
            updateDrawerTitle(mCurrentFragmentIndex);
        }
        
        // إعادة إنشاء القائمة لإخفاء الأيقونة
        invalidateOptionsMenu();
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
