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
import androidx.recyclerview.widget.RecyclerView;

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
     * Call this from subclasses after setContentView(...) to wire toolbar/drawer.
     * Relies on ids in layouts: toolbar (R.id.toolbar), drawer_layout (R.id.drawer_layout),
     * drawer_list_view (R.id.drawer_list_view)
     */
    protected void setupToolbarAndDrawer() {
        toolbar = findViewById(getId("toolbar"));
        drawerLayout = findViewById(getId("drawer_layout"));
        drawerRecyclerView = findViewById(getId("drawer_list_view"));

        if (toolbar != null) setSupportActionBar(toolbar);

        if (drawerLayout != null && toolbar != null) {
            // Use existing string resources navigation_drawer_open / navigation_drawer_close if available,
            // otherwise fall back to empty strings to avoid compile/runtime issues.
            int openId = getResources().getIdentifier("navigation_drawer_open", "string", getPackageName());
            int closeId = getResources().getIdentifier("navigation_drawer_close", "string", getPackageName());
            String openDesc = openId != 0 ? getString(openId) : "";
            String closeDesc = closeId != 0 ? getString(closeId) : "";

            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    openDesc, closeDesc);
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
        applyTypefaceToDrawerItems();
    }

    protected void applyTypefaceToToolbar() {
        Typeface tf = SettingsHelper.getTypeface(this);
        if (toolbar == null || tf == null) return;
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View v = toolbar.getChildAt(i);
            if (v instanceof TextView) ((TextView) v).setTypeface(tf);
        }
    }

    protected void applyTypefaceToDrawerItems() {
        Typeface tf = SettingsHelper.getTypeface(this);
        if (drawerRecyclerView == null || tf == null) return;

        // If adapter supports applying typeface per holder (as in DrawerListAdapter), notify adapter to bind again
        try {
            RecyclerView.Adapter adapter = drawerRecyclerView.getAdapter();
            if (adapter != null) adapter.notifyDataSetChanged();
        } catch (Exception ignored) {}
    }

    // Utility to avoid compile errors if some ids differ; expects R.id.<name> generally exists.
    protected <T extends View> T findViewByStringId(String name) {
        int id = getId(name);
        if (id == 0) return null;
        return findViewById(id);
    }

    protected int getId(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
    }
                }
