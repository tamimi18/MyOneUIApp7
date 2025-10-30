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
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerToggle;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

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
     * Call this from subclasses after setContentView(...) to wire toolbar/drawer.
     * Relies on ids in layouts: toolbar, drawer_layout, nav_view
     */
    protected void setupToolbarAndDrawer() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (toolbar != null) setSupportActionBar(toolbar);

        if (drawerLayout != null && toolbar != null) {
            // Use existing string resources navigation_drawer_open / navigation_drawer_close
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();

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
        applyTypefaceToNavigationItems();
    }

    protected void applyTypefaceToToolbar() {
        Typeface tf = SettingsHelper.getTypeface(this);
        if (toolbar == null || tf == null) return;
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View v = toolbar.getChildAt(i);
            if (v instanceof TextView) ((TextView) v).setTypeface(tf);
        }
    }

    protected void applyTypefaceToNavigationItems() {
        if (navigationView != null) {
            try {
                int styleId = getResources().getIdentifier("NavDrawerItemText", "style", getPackageName());
                if (styleId != 0) navigationView.setItemTextAppearance(styleId);
            } catch (Exception ignored) {}
        }
    }
}
