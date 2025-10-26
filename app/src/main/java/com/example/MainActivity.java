package com.example.oneuiapp;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
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
 * MainActivity - النسخة المصححة مع دعم كامل لقوائم Fragments
 * 
 * التحديث الجديد:
 * - إضافة onCreateOptionsMenu() لدعم قوائم Fragments
 * - إضافة onPrepareOptionsMenu() للسماح للـ Fragment بتحديث القائمة
 * - إضافة onOptionsItemSelected() لتمرير النقرات للـ Fragment
 * 
 * شرح المشكلة:
 * عندما يكون Fragment يريد إضافة أيقونات للـ Toolbar، يجب على
 * Activity الأب أن "يسمح" بذلك من خلال تفعيل نظام القوائم.
 * بدون هذه الدوال، حتى لو كتب Fragment كل الكود الصحيح،
 * لن تظهر الأيقونات لأن Activity لا يستدعي دوال Fragment.
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
                        
                        // ★★★ هذا السطر مهم جداً! ★★★
                        // عند تغيير Fragment، نحدّث القائمة لإظهار أيقونات Fragment الجديد
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
     * ★★★ دالة جديدة: إنشاء قائمة Activity ★★★
     * 
     * هذه الدالة مهمة جداً لعمل قوائم Fragments!
     * 
     * كيف يعمل نظام القوائم في Android؟
     * 1. عندما يريد Android عرض القائمة، يستدعي onCreateOptionsMenu()
     * 2. نحن هنا نستدعي super.onCreateOptionsMenu(menu) أولاً
     * 3. الـ super تلقائياً تستدعي onCreateOptionsMenu() لكل Fragment نشط
     * 4. Fragment يضيف أيقوناته للقائمة
     * 5. النتيجة: القائمة تحتوي على عناصر Activity + عناصر Fragment
     * 
     * لماذا نُرجع true؟
     * لأننا نريد عرض القائمة. لو أرجعنا false، لن تظهر القائمة أبداً.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // استدعاء super مهم جداً! هو الذي يسمح للـ Fragment بإضافة عناصره
        super.onCreateOptionsMenu(menu);
        
        // يمكنك هنا إضافة عناصر خاصة بـ Activity إذا أردت
        // مثلاً: menu.add(...)
        
        return true;
    }

    /**
     * ★★★ دالة جديدة: تحديث القائمة ★★★
     * 
     * هذه الدالة تُستدعى في حالات معينة:
     * - عند استدعاء invalidateOptionsMenu()
     * - قبل عرض القائمة
     * - عند تغيير Fragment
     * 
     * الفائدة:
     * Fragment يمكنه تحديث حالة عناصره هنا
     * (مثل تفعيل/تعطيل الأيقونات، تغيير النص، إلخ)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // super تستدعي onPrepareOptionsMenu() للـ Fragment النشط
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    /**
     * ★★★ دالة جديدة: معالجة النقر على عناصر القائمة ★★★
     * 
     * عندما يضغط المستخدم على عنصر في القائمة:
     * 1. Android يستدعي هذه الدالة
     * 2. نحن نستدعي super.onOptionsItemSelected(item)
     * 3. الـ super تمرر الحدث للـ Fragment النشط
     * 4. Fragment يعالج الحدث في onOptionsItemSelected() الخاص به
     * 
     * إذا لم نستدعي super، Fragment لن يستقبل أي أحداث نقر!
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // تمرير الحدث للـ Fragment أولاً
        // إذا عالج Fragment الحدث، سيُرجع true ونتوقف هنا
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        
        // إذا لم يعالج Fragment الحدث، يمكننا معالجته هنا في Activity
        // (حالياً لا يوجد عناصر خاصة بـ Activity)
        
        return false;
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
            
            // تحديث القائمة عند تغيير الاختيار
            invalidateOptionsMenu();
        }
    }
    }
