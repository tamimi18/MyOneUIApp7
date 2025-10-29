package com.example.oneuiapp;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import dev.oneuiproject.oneui.utils.internal.ToolbarLayoutUtils;

public class HomeFragment extends Fragment {

    private boolean mEnableBackToHeader;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private Toolbar mToolbar;
    private View mSwipeUpContainer;
    private ViewGroup mBottomContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupToolbar();
        setupAppBar(getResources().getConfiguration());
    }

    private void initViews(View view) {
        mAppBarLayout = view.findViewById(R.id.app_bar);
        mCollapsingToolbar = view.findViewById(R.id.collapsing_toolbar);
        mToolbar = view.findViewById(R.id.toolbar);
        mSwipeUpContainer = view.findViewById(R.id.swipe_up_container);
        mBottomContainer = view.findViewById(R.id.bottom_container);
    }

    private void setupToolbar() {
        ((AppCompatActivity) requireActivity()).setSupportActionBar(mToolbar);
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupAppBar(Configuration config) {
        ToolbarLayoutUtils.hideStatusBarForLandscape(requireActivity(), config.orientation);
        ToolbarLayoutUtils.updateListBothSideMargin(requireActivity(), mBottomContainer);

        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE && !isInMultiWindowMode()) {
            mAppBarLayout.seslSetCustomHeightProportion(true, 0.5f);
            mEnableBackToHeader = true;
            mAppBarLayout.addOnOffsetChangedListener(new AppBarOffsetListener());
            mAppBarLayout.setExpanded(true, false);

            if (mSwipeUpContainer != null) {
                mSwipeUpContainer.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams lp = mSwipeUpContainer.getLayoutParams();
                lp.height = getResources().getDisplayMetrics().heightPixels / 2;
            }
        } else {
            mAppBarLayout.setExpanded(false, false);
            mEnableBackToHeader = false;
            mAppBarLayout.seslSetCustomHeightProportion(true, 0);
            if (mBottomContainer != null) {
                mBottomContainer.setAlpha(1f);
            }
            if (mSwipeUpContainer != null) {
                mSwipeUpContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupAppBar(newConfig);
    }

    private boolean isInMultiWindowMode() {
        return Build.VERSION.SDK_INT >= 24 && requireActivity().isInMultiWindowMode();
    }

    private class AppBarOffsetListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            final int totalScrollRange = appBarLayout.getTotalScrollRange();
            final int abs = Math.abs(verticalOffset);

            if (mSwipeUpContainer != null) {
                if (abs >= totalScrollRange / 2) {
                    mSwipeUpContainer.setAlpha(0f);
                } else if (abs == 0) {
                    mSwipeUpContainer.setAlpha(1f);
                } else {
                    float offsetAlpha = (appBarLayout.getY() / totalScrollRange);
                    float arrowAlpha = 1 - (offsetAlpha * -3);
                    arrowAlpha = Math.max(0, Math.min(1, arrowAlpha));
                    mSwipeUpContainer.setAlpha(arrowAlpha);
                }
            }

            if (mBottomContainer != null) {
                final float alphaRange = mCollapsingToolbar.getHeight() * 0.143f;
                final float layoutPosition = Math.abs(appBarLayout.getTop());
                float bottomAlpha = (150.0f / alphaRange) * (layoutPosition - (mCollapsingToolbar.getHeight() * 0.35f));
                bottomAlpha = Math.max(0, Math.min(255, bottomAlpha));
                mBottomContainer.setAlpha(bottomAlpha / 255);
            }
        }
    }
}
