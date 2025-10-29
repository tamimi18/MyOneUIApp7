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
import java.util.Map;

import dev.oneuiproject.oneui.layout.DrawerLayout;
import androidx.appcompat.app.AlertDialog;

/**
 * MainActivity - النسخة المحدثة مع حماية attachBaseContext وحماية الوصول للتولبار
 * لا تستخدم موارد نظام داخلية ولا تستدعي setBackgroundResource ولن تغير الثيم برمجياً.
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

    // مرجع عنصر القائمة الخاص بمعلومات الخط لتمكين/تعطيل رؤيته
    private MenuItem mFontMetaMenuItem;

    @Override
    protected void attachBaseContext(Context newBase) {
        // حماية: إذا فشل التغليف (مثلاً بسبب TypefaceContextWrapper أو موارد غير متوقعة)
        // نرجع لاستخدام السياق الأصلي لمنع كراش عند الإقلاع
        try {
            Context wrapped = SettingsHelper.wrapContext(newBase);
            if (wrapped != null) {
                super.attachBaseContext(wrapped);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.attachBaseContext(newBase);
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

        // Inflate font meta menu into DrawerLayout toolbar (if toolbar exists)
        try {
            if (mDrawerLayout != null) {
                // حماية إضافية: قد يرجع getToolbar() null أو يرمز لاستثناء داخلي في بعض إصدارات المكتبة
                try {
                    if (mDrawerLayout.getToolbar() != null) {
                        mDrawerLayout.getToolbar().inflateMenu(R.menu.menu_main_font_meta);
                        // حفظ مرجع عنصر القائمة من الـ Menu في الـ Toolbar
                        Menu menu = mDrawerLayout.getToolbar().getMenu();
                        if (menu != null) {
                            mFontMetaMenuItem = menu.findItem(R.id.action_font_meta);
                            if (mFontMetaMenuItem != null) {
                                // اجعله مخفياً افتراضياً؛ سنظهره عندما نعرض FontViewerFragment
                                mFontMetaMenuItem.setVisible(false);
                            }
                        }

                        mDrawerLayout.getToolbar().setOnMenuItemClickListener(item -> {
                            if (item.getItemId() == R.id.action_font_meta) {
                                showFontMetaFromFragment();
                                return true;
                            }
                            return false;
                        });
                    }
                } catch (Exception inner) {
                    // لا نريد أن يتسبب أي استثناء في تعطيل الإقلاع
                    inner.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        try {
            mDrawerLayout = findViewById(R.id.drawer_layout);
            mDrawerListView = findViewById(R.id.drawer_list_view);
        } catch (Exception e) {
            // الحماية: إذا فشل العثور على أي View، نمنع الكراش ونترك القيم null
            e.printStackTrace();
            mDrawerLayout = null;
            mDrawerListView = null;
        }
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
        if (mDrawerListView == null) return;

        mDrawerListView.setLayoutManager(new LinearLayoutManager(this));
        mDrawerAdapter = new DrawerListAdapter(
                this,
                mFragments,
                position -> {
                    try {
                        if (mDrawerLayout != null) mDrawerLayout.setDrawerOpen(false, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

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

        // تحديث رؤية أيقونة معلومات الخط بعد تبديل الفِراغمنت
        try {
            if (mFontMetaMenuItem != null) {
                mFontMetaMenuItem.setVisible(position == 2);
            }
        } catch (Exception ignored) {
        }
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

        try {
            mDrawerLayout.setTitle(title);
            mDrawerLayout.setExpandedSubtitle(subtitle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // تأكد من أن أيقونة المعلومات مرئية فقط عند شاشة Font Viewer (index 2)
        try {
            if (mFontMetaMenuItem != null) {
                mFontMetaMenuItem.setVisible(fragmentIndex == 2);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Gathers metadata from FontViewerFragment and shows dialog.
     */
    private void showFontMetaFromFragment() {
        Fragment frag = (mFragments.size() > 2) ? mFragments.get(2) : null;
        if (!(frag instanceof FontViewerFragment)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.font_viewer_select_font))
                    .setMessage(getString(R.string.font_viewer_no_font_selected))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        FontViewerFragment fvf = (FontViewerFragment) frag;
        if (!fvf.hasFontSelected()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.font_viewer_no_font_selected))
                    .setMessage(getString(R.string.font_viewer_no_font_selected))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        Map<String, String> meta = fvf.getFontMetaData();
        StringBuilder sb = new StringBuilder();

        String[] keys = {"FullName", "Family", "SubFamily", "PostScriptName", "Version", "Manufacturer", "FileName", "Path"};
        for (String k : keys) {
            String v = meta.get(k);
            if (v != null && !v.isEmpty()) {
                sb.append(k).append(": ").append(v).append("\n\n");
            }
        }

        if (sb.length() == 0) sb.append("No metadata available.");

        String dialogTitle = meta.containsKey("FullName") ? meta.get("FullName") : getString(R.string.font_viewer_select_font);
        new AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setMessage(sb.toString().trim())
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
                 }
