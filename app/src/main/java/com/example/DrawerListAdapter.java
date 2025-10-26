package com.example.oneuiapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.reflect.Field;

public class DrawerListAdapter extends RecyclerView.Adapter<DrawerListViewHolder> {

    private Context mContext;
    private DrawerListener mListener;
    private int mSelectedPos = 0;

    // عدد العناصر الثابت: Home, Settings, Font Viewer
    private static final int ITEM_COUNT = 3;

    public interface DrawerListener {
        boolean onDrawerItemSelected(int position);
    }

    public DrawerListAdapter(@NonNull Context context, DrawerListener listener) {
        mContext = context;
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
        int iconRes = 0;
        String title = "";
        
        // تحديد الأيقونة والعنوان حسب الموقع
        switch (position) {
            case 0: // Home
                iconRes = getOneUiIconId("ic_oui_home");
                title = mContext.getString(R.string.drawer_home);
                break;
            case 1: // Settings
                iconRes = getOneUiIconId("ic_oui_settings");
                title = mContext.getString(R.string.drawer_settings);
                break;
            case 2: // Font Viewer
                iconRes = getOneUiIconId("ic_oui_folder");
                title = mContext.getString(R.string.drawer_font_viewer);
                break;
        }

        if (iconRes != 0) {
            holder.setIcon(iconRes);
        }
        if (!title.isEmpty()) {
            holder.setTitle(title);
        }

        holder.setSelected(position == mSelectedPos);
        
        holder.itemView.setOnClickListener(v -> {
            final int itemPos = holder.getBindingAdapterPosition();
            
            if (itemPos == RecyclerView.NO_POSITION) {
                return;
            }
            
            boolean selectionChanged = false;
            if (mListener != null) {
                selectionChanged = mListener.onDrawerItemSelected(itemPos);
            }
            
            if (selectionChanged) {
                setSelectedItem(itemPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return ITEM_COUNT;
    }

    public void setSelectedItem(int position) {
        if (position < 0 || position >= getItemCount()) {
            return;
        }
        
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
