package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.RecyclerView;

import androidx.drawerlayout.widget.DrawerLayout;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerToggle;
    protected Toolbar toolbar;
    protected RecyclerView drawerRecyclerView;

    @Override
    protected void attachBaseContext(Context newBase) {
        Context wrapped = SettingsHelper.wrapContext(newBase);
        super.attachBaseContext(wrapped);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        try {
            Typeface tf = SettingsHelper.getTypeface(this);
            if (tf != null) {
                TypefaceContextWrapper.applyFontRecursively(getWindow().getDecorView(), tf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Call this from subclasses after setContentView(...) to wire toolbar and drawer.
     * Expects ids: R.id.toolbar, R.id.drawer_layout, R.id.drawer_list_view
     */
    protected void setupToolbarAndDrawer() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerRecyclerView = findViewById(R.id.drawer_list_view);

        if (toolbar != null) setSupportActionBar(toolbar);

        if (drawerLayout != null && toolbar != null) {
            // Use resource ids for accessibility descriptions if present
            int openId = getResources().getIdentifier("navigation_drawer_open", "string", getPackageName());
            int closeId = getResources().getIdentifier("navigation_drawer_close", "string", getPackageName());
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    openId != 0 ? openId : 0,
                    closeId != 0 ? closeId : 0);
            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();

            // Ensure navigation icon toggles drawer (compatible with OneUI DrawerLayout)
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        applyTypefaceToToolbar();
        notifyDrawerAdapterToRebindTypeface();
    }

    protected void applyTypefaceToToolbar() {
        Typeface tf = SettingsHelper.getTypeface(this);
        if (toolbar == null || tf == null) return;
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View v = toolbar.getChildAt(i);
            if (v instanceof TextView) ((TextView) v).setTypeface(tf);
        }
    }

    protected void notifyDrawerAdapterToRebindTypeface() {
        if (drawerRecyclerView == null) return;
        try {
            RecyclerView.Adapter adapter = drawerRecyclerView.getAdapter();
            if (adapter != null) adapter.notifyDataSetChanged();
        } catch (Exception ignored) {}
    }
}
