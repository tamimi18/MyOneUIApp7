package com.example.oneuiapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

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
        View view = LayoutInflater.from(mContext).inflate(R.layout.drawer_list_item, parent, false);
        return new DrawerListViewHolder(view, false);
    }

    @Override
    public void onBindViewHolder(@NonNull DrawerListViewHolder holder, int position) {
        Fragment fragment = mFragments.get(position);

        String title = "Item " + position;
        int iconRes = 0;

        // If fragments implement a simple interface to provide title/icon, use it
        if (fragment instanceof BaseFragmentTitleProvider) {
            BaseFragmentTitleProvider p = (BaseFragmentTitleProvider) fragment;
            title = p.getTitle();
            iconRes = p.getIconResId();
        }

        holder.setTitle(title);
        if (iconRes != 0) holder.setIcon(iconRes);

        holder.setSelected(position == mSelectedPos);
        holder.applyTypeface(SettingsHelper.getTypeface(mContext));

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            boolean handled = mListener != null && mListener.onDrawerItemSelected(pos);
            if (handled) setSelectedItem(pos);
        });
    }

    @Override
    public int getItemCount() {
        return mFragments != null ? mFragments.size() : 0;
    }

    public void setSelectedItem(int position) {
        int prev = mSelectedPos;
        mSelectedPos = position;
        if (prev != position) {
            notifyItemChanged(prev);
            notifyItemChanged(position);
        }
    }
}
