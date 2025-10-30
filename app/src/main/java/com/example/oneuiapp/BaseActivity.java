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

public class BaseActivity extends AppCompatActivity {

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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: layouts setContentView happens in subclasses (e.g., MainActivity)
    }

    protected void setupToolbarAndDrawer() {
        toolbar = findViewById(getResId("toolbar"));
        drawerLayout = findViewById(getResId("drawer_layout"));
        navigationView = findViewById(getResId("nav_view"));

        if (toolbar != null) setSupportActionBar(toolbar);

        if (drawerLayout != null && toolbar != null) {
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    getResString("open_drawer"), getResString("close_drawer"));
            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();

            // Ensure toolbar navigation opens/closes drawer even for custom icons
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

        // Apply Typeface to toolbar title(s)
        applyTypefaceToToolbar();

        // Apply Typeface to navigation drawer items via style or adapter fallback
        applyTypefaceToNavigation();
    }

    protected void applyTypefaceToToolbar() {
        Typeface tf = SettingsHelper.getTypeface(this);
        if (toolbar == null || tf == null) return;
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View v = toolbar.getChildAt(i);
            if (v instanceof TextView) ((TextView) v).setTypeface(tf);
        }
    }

    protected void applyTypefaceToNavigation() {
        // If using NavigationView's menu items, set itemTextAppearance that contains fontFamily
        if (navigationView != null) {
            // attempt to set item text appearance to a style defined in styles.xml (optional)
            try {
                navigationView.setItemTextAppearance(getResStyle("NavDrawerItemText"));
            } catch (Exception ignored) { }
        }
        // If you use a custom adapter (DrawerListAdapter), ensure it applies Typeface per item.
    }

    // Helpers to avoid hardcoding resource ids in this patch (adjust if you prefer direct R.id.*)
    protected int getResId(String name) {
        try {
            return getResources().getIdentifier(name, "id", getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }

    protected int getResStyle(String name) {
        try {
            return getResources().getIdentifier(name, "style", getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }

    protected String getResString(String name) {
        int id = getResources().getIdentifier(name, "string", getPackageName());
        return id == 0 ? "" : getString(id);
    }
}
