package com.example.oneuiapp;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.content.Context;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout as AndroidXDrawerLayout;

import dev.oneuiproject.oneui.layout.DrawerLayout as OneUIDrawerLayout;

/**
 * MainActivity متوافق مع OneUI أو AndroidX DrawerLayout.
 * يستخدم حقول منفصلة لكل نوع ويستدعي واجهات مغلفة لتجنب ClassCastException.
 */
public class MainActivity extends BaseActivity implements FontViewerFragment.OnFontChangedListener {

    // قد يكون Drawer من OneUI أو من AndroidX — نخزن كلا الحضانتين
    private OneUIDrawerLayout mOneUiDrawer;
    private AndroidXDrawerLayout mAndroidxDrawer;

    private RecyclerView mDrawerListView;
    private DrawerListAdapter mDrawerAdapter;
    private final List<Fragment> mFragments = new ArrayList<>();
    private int mCurrentFragmentIndex = 0;
    private static final String KEY_CURRENT_FRAGMENT = "current_fragment_index";
    private static final String TAG_HOME = "fragment_home";
    private static final String TAG_FONT_VIEWER = "fragment_font_viewer";
    private String currentFontRealName;
    private String currentFontFileName;
    private MenuItem mFontMetaMenuItem;

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

        // إعداد Toolbar من Drawer (إن توفَّر)
        try {
            androidx.appcompat.widget.Toolbar t = getDrawerToolbar();
            if (t != null) {
                setSupportActionBar(t);
                try {
                    t.inflateMenu(R.menu.menu_main_font_meta);
                    Menu menu = t.getMenu();
                    if (menu != null) {
                        mFontMetaMenuItem = menu.findItem(R.id.action_font_meta);
                        if (mFontMetaMenuItem != null) {
                            mFontMetaMenuItem.setVisible(false);
                        }
                    }
                } catch (Exception ignored) {}
                t.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_font_meta) {
                        showFontMetaFromFragment();
                        return true;
                    }
                    return false;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        // ابحث عن view ثم حدّد نوعه بأمان
        View dlView = findViewById(R.id.drawer_layout);
        if (dlView instanceof OneUIDrawerLayout) {
            mOneUiDrawer = (OneUIDrawerLayout) dlView;
            mAndroidxDrawer = null;
        } else if (dlView instanceof AndroidXDrawerLayout) {
            mAndroidxDrawer = (AndroidXDrawerLayout) dlView;
            mOneUiDrawer = null;
        } else {
            mAndroidxDrawer = null;
            mOneUiDrawer = null;
            Log.w("MainActivity", "drawer_layout view is not a recognized DrawerLayout implementation");
        }

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
        if (mDrawerListView == null) return;
        mDrawerListView.setLayoutManager(new LinearLayoutManager(this));
        mDrawerAdapter = new DrawerListAdapter(this, mFragments, position -> {
            try {
                // أغلق الدرج بحسب النوع المتاح
                setDrawerOpen(false, true);
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
        try {
            if (mFontMetaMenuItem != null) {
                mFontMetaMenuItem.setVisible(position == 2);
            }
        } catch (Exception ignored) {}
    }

    private void updateDrawerTitle(int fragmentIndex) {
        if (!isDrawerAvailable()) return;

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
                subtitle = currentFontFileName != null ? currentFontFileName : getString(R.string.font_viewer_select_description);
            } else {
                title = getString(R.string.drawer_font_viewer);
                subtitle = getString(R.string.font_viewer_select_description);
            }
        } else {
            title = getString(R.string.app_name);
            subtitle = getString(R.string.app_subtitle);
        }

        try {
            setDrawerTitle(title);
            setDrawerExpandedSubtitle(subtitle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (mFontMetaMenuItem != null) {
                mFontMetaMenuItem.setVisible(fragmentIndex == 2);
            }
        } catch (Exception ignored) {}
    }

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

    // ---------------------------
    // Helper methods to abstract drawer API differences
    // ---------------------------

    private boolean isDrawerAvailable() {
        return mAndroidxDrawer != null || mOneUiDrawer != null;
    }

    private androidx.appcompat.widget.Toolbar getDrawerToolbar() {
        try {
            if (mOneUiDrawer != null) {
                // OneUI DrawerLayout exposes getToolbar()
                return mOneUiDrawer.getToolbar();
            } else if (mAndroidxDrawer != null) {
                // If you embed a Toolbar inside your layout instead of drawer, try findViewById
                View v = findViewById(R.id.toolbar);
                if (v instanceof androidx.appcompat.widget.Toolbar) return (androidx.appcompat.widget.Toolbar) v;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void setDrawerOpen(boolean open, boolean animate) {
        try {
            if (mOneUiDrawer != null) {
                // OneUI DrawerLayout may have custom method name; sample-app uses setDrawerOpen(boolean, boolean)
                mOneUiDrawer.setDrawerOpen(open, animate);
            } else if (mAndroidxDrawer != null) {
                if (open) mAndroidxDrawer.openDrawer(GravityCompat.START);
                else mAndroidxDrawer.closeDrawer(GravityCompat.START);
            }
        } catch (NoSuchMethodError | Exception e) {
            // best-effort: try alternate approaches or log
            Log.w("MainActivity", "setDrawerOpen failed: " + e.getMessage());
        }
    }

    private void setDrawerTitle(String title) {
        try {
            if (mOneUiDrawer != null) {
                mOneUiDrawer.setTitle(title);
            } else if (mAndroidxDrawer != null) {
                // If toolbar present, set title on it
                androidx.appcompat.widget.Toolbar t = getDrawerToolbar();
                if (t != null) t.setTitle(title);
            }
        } catch (Exception ignored) {}
    }

    private void setDrawerExpandedSubtitle(String subtitle) {
        try {
            if (mOneUiDrawer != null) {
                mOneUiDrawer.setExpandedSubtitle(subtitle);
            } else if (mAndroidxDrawer != null) {
                androidx.appcompat.widget.Toolbar t = getDrawerToolbar();
                if (t != null) t.setSubtitle(subtitle);
            }
        } catch (Exception ignored) {}
    }
                                                          }
