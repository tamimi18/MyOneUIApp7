package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.List;

public class DrawerListAdapter extends RecyclerView.Adapter<DrawerListViewHolder> {

    private final Context mContext;
    private final List<Fragment> mFragments;
    private final DrawerListener mListener;
    private int mSelectedPos = 0;

    public interface DrawerListener {
        boolean onDrawerItemSelected(int position);
    }

    public DrawerListAdapter(@NonNull Context context, List<Fragment> fragments, DrawerListener listener) {
        mContext = context;
        mFragments = fragments;
        mListener = listener;
    }

    @NonNull
    @Override
    public DrawerListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.drawer_list_item, parent, false);
        return new DrawerListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DrawerListViewHolder holder, int position) {
        Fragment fragment = mFragments.get(position);

        int iconRes = 0;
        String title = "";

        if (fragment instanceof HomeFragment) {
            iconRes = getOneUiIconId("ic_oui_home");
            title = mContext.getString(R.string.drawer_home);
        } else if (fragment instanceof SettingsFragment) {
            iconRes = getOneUiIconId("ic_oui_settings");
            title = mContext.getString(R.string.drawer_settings);
        } else if (fragment instanceof FontViewerFragment) {
            iconRes = getOneUiIconId("ic_oui_folder");
            title = mContext.getString(R.string.drawer_font_viewer);
        }

        if (iconRes != 0) holder.setIcon(iconRes);
        if (!title.isEmpty()) holder.setTitle(title);

        // Apply selection state
        holder.setSelected(position == mSelectedPos);

        // Apply Typeface centrally for drawer items
        Typeface tf = SettingsHelper.getTypeface(mContext);
        holder.applyTypeface(tf);

        holder.itemView.setOnClickListener(v -> {
            final int itemPos = holder.getBindingAdapterPosition();
            if (itemPos == RecyclerView.NO_POSITION) return;
            boolean selectionChanged = false;
            if (mListener != null) selectionChanged = mListener.onDrawerItemSelected(itemPos);
            if (selectionChanged) setSelectedItem(itemPos);
        });
    }

    @Override
    public int getItemCount() {
        return mFragments != null ? mFragments.size() : 0;
    }

    public void setSelectedItem(int position) {
        if (position < 0 || position >= getItemCount()) return;
        int previousPos = mSelectedPos;
        mSelectedPos = position;
        if (previousPos != position) {
            notifyItemChanged(previousPos);
            notifyItemChanged(position);
        }
    }

    private int getOneUiIconId(String name) {
        try {
            Class<?> r = Class.forName("dev.oneuiproject.oneui.R$drawable");
            Field f = r.getField(name);
            return f.getInt(null);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getSelectedPosition() {
        return mSelectedPos;
    }
}
