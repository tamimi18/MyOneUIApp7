package com.example.oneuiapp;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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

    private ImageButton mFontInfoButton;

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

    private void setupToolbar() {
        Toolbar toolbar = mDrawerLayout.getToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
            }
            
            mFontInfoButton = new ImageButton(this);
            
            try {
                int iconResId = getResources().getIdentifier(
                    "ic_oui_info_outline", 
                    "drawable", 
                    "dev.oneuiproject.oneui"
                );
                
                if (iconResId != 0) {
                    mFontInfoButton.setImageResource(iconResId);
                } else {
                    mFontInfoButton.setImageResource(android.R.drawable.ic_menu_info_details);
                }
            } catch (Exception e) {
                mFontInfoButton.setImageResource(android.R.drawable.ic_menu_info_details);
            }
            
            int size = (int) (48 * getResources().getDisplayMetrics().density);
            Toolbar.LayoutParams params = new Toolbar.LayoutParams(size, size);
            mFontInfoButton.setLayoutParams(params);
            
            mFontInfoButton.setBackgroundResource(
                android.R.attr.selectableItemBackgroundBorderless
            );
            
            int padding = (int) (12 * getResources().getDisplayMetrics().density);
            mFontInfoButton.setPadding(padding, padding, padding, padding);
            
            // استخدام لون من الثيم بدلاً من موارد OneUI
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            mFontInfoButton.setColorFilter(typedValue.data);
            
            mFontInfoButton.setContentDescription(getString(R.string.font_metadata_title));
            
            mFontInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fragment currentFragment = getCurrentFragment();
                    if (currentFragment instanceof FontViewerFragment) {
                        ((FontViewerFragment) currentFragment).showMetadataDialog();
                    }
                }
            });
            
            mFontInfoButton.setVisibility(View.GONE);
            
            toolbar.addView(mFontInfoButton);
        }
    }

    private void updateFontInfoButtonVisibility() {
        if (mFontInfoButton == null) {
            return;
        }
        
        boolean shouldShow = (mCurrentFragmentIndex == 2) && hasFontLoaded;
        
        mFontInfoButton.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private Fragment getCurrentFragment() {
        if (mCurrentFragmentIndex >= 0 && mCurrentFragmentIndex < mFragments.size()) {
            return mFragments.get(mCurrentFragmentIndex);
        }
        return null;
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
        
        updateFontInfoButtonVisibility();
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
                    
                    updateFontInfoButtonVisibility();
                }
            }
        }
    }

    @Override
    public void onFontChanged(String fontRealName, String fontFileName) {
        this.currentFontRealName = fontRealName;
        this.currentFontFileName = fontFileName;
        this.hasFontLoaded = true;
        
        if (mCurrentFragmentIndex == 2) {
            updateDrawerTitle(mCurrentFragmentIndex);
        }
        
        updateFontInfoButtonVisibility();
    }
    
    @Override
    public void onFontCleared() {
        this.currentFontRealName = null;
        this.currentFontFileName = null;
        this.hasFontLoaded = false;
        
        if (mCurrentFragmentIndex == 2) {
            updateDrawerTitle(mCurrentFragmentIndex);
        }
        
        updateFontInfoButtonVisibility();
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
