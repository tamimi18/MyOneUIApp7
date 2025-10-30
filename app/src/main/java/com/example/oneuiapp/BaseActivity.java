package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    protected androidx.drawerlayout.widget.DrawerLayout drawerLayout; // primary expected type
    protected ActionBarDrawerToggle drawerToggle;
    protected Toolbar toolbar;
    protected RecyclerView drawerRecyclerView;

    // backup reference if layout is OneUI DrawerLayout (available at runtime only if dependency present)
    protected Object oneUiDrawerLayout; // dev.oneuiproject.oneui.layout.DrawerLayout (kept as Object to avoid compile issue)

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
            Log.w(TAG, "apply font failed: " + e.getMessage());
        }
    }

    /**
     * Call this from subclasses after setContentView(...) to wire toolbar and drawer.
     * Expects ids: R.id.toolbar, R.id.drawer_layout, R.id.drawer_list_view
     */
    protected void setupToolbarAndDrawer() {
        toolbar = findViewById(R.id.toolbar);
        View dlView = findViewById(R.id.drawer_layout);
        drawerRecyclerView = findViewById(R.id.drawer_list_view);

        // Determine actual drawer type safely
        if (dlView instanceof androidx.drawerlayout.widget.DrawerLayout) {
            drawerLayout = (androidx.drawerlayout.widget.DrawerLayout) dlView;
            oneUiDrawerLayout = null;
        } else {
            drawerLayout = null;
            // keep raw reference if it's OneUI DrawerLayout (avoid compile dependency)
            oneUiDrawerLayout = dlView;
        }

        if (toolbar != null) setSupportActionBar(toolbar);

        // Prefer AndroidX DrawerLayout for ActionBarDrawerToggle integration
        if (drawerLayout != null && toolbar != null) {
            int openId = getResources().getIdentifier("navigation_drawer_open", "string", getPackageName());
            int closeId = getResources().getIdentifier("navigation_drawer_close", "string", getPackageName());
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    openId != 0 ? openId : 0,
                    closeId != 0 ? closeId : 0);
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
        } else if (oneUiDrawerLayout != null && toolbar != null) {
            // Best-effort integration with OneUI DrawerLayout via reflection (non-blocking)
            try {
                // Try to find setDrawerOpen(boolean, boolean) or toggle method and getToolbar()
                Method setDrawerOpen = null;
                Method getToolbar = null;
                try {
                    setDrawerOpen = oneUiDrawerLayout.getClass().getMethod("setDrawerOpen", boolean.class, boolean.class);
                } catch (NoSuchMethodException ignored) {}
                try {
                    getToolbar = oneUiDrawerLayout.getClass().getMethod("getToolbar");
                } catch (NoSuchMethodException ignored) {}

                // If OneUI exposes a toolbar inside the drawer, prefer its toolbar for navigation clicks
                if (getToolbar != null) {
                    Object maybeToolbar = getToolbar.invoke(oneUiDrawerLayout);
                    if (maybeToolbar instanceof Toolbar) {
                        toolbar = (Toolbar) maybeToolbar;
                        setSupportActionBar(toolbar);
                    }
                }

                final Method finalSetDrawerOpen = setDrawerOpen;
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (finalSetDrawerOpen != null) {
                                // toggle: attempt to read state via isDrawerOpen(int) if exists, otherwise just call open(true)
                                boolean isOpen = false;
                                try {
                                    Method isOpenM = oneUiDrawerLayout.getClass().getMethod("isDrawerOpen", int.class);
                                    Object res = isOpenM.invoke(oneUiDrawerLayout, GravityCompat.START);
                                    if (res instanceof Boolean) isOpen = (Boolean) res;
                                } catch (Exception ignored) {}
                                finalSetDrawerOpen.invoke(oneUiDrawerLayout, !isOpen, true);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "OneUI toggle failed: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "OneUI drawer hookup failed: " + e.getMessage());
            }
        } else {
            // No usable drawer found; still apply toolbar typeface etc.
            if (toolbar != null) {
                applyTypefaceToToolbar();
            }
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

    // Utility helpers for other classes to control drawer safely

    protected boolean isDrawerOpen() {
        try {
            if (drawerLayout != null) {
                return drawerLayout.isDrawerOpen(GravityCompat.START);
            } else if (oneUiDrawerLayout != null) {
                try {
                    Method m = oneUiDrawerLayout.getClass().getMethod("isDrawerOpen", int.class);
                    Object res = m.invoke(oneUiDrawerLayout, GravityCompat.START);
                    if (res instanceof Boolean) return (Boolean) res;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return false;
    }

    protected void openDrawer(boolean animate) {
        try {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else if (oneUiDrawerLayout != null) {
                try {
                    Method m = oneUiDrawerLayout.getClass().getMethod("setDrawerOpen", boolean.class, boolean.class);
                    m.invoke(oneUiDrawerLayout, true, animate);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    protected void closeDrawer(boolean animate) {
        try {
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (oneUiDrawerLayout != null) {
                try {
                    Method m = oneUiDrawerLayout.getClass().getMethod("setDrawerOpen", boolean.class, boolean.class);
                    m.invoke(oneUiDrawerLayout, false, animate);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
