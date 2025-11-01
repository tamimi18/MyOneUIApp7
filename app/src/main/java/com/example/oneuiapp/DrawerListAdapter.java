package com.example.oneuiapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
// import java.lang.reflect.Field; // ★★★ محذوف ★★★
import java.util.List;
public class DrawerListAdapter extends RecyclerView.Adapter<DrawerListViewHolder> {

    private Context mContext;
    private List<Fragment> mFragments;
    private DrawerListener mListener;
    private int mSelectedPos = 0;

    public interface DrawerListener {
        boolean onDrawerItemSelected(int position);
    }

    public DrawerListAdapter(
            @NonNull Context context, List<Fragment> fragments, DrawerListener listener) {
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
        // ★★★ تحديث: أضفنا دعم FontViewerFragment ★★★
        int iconRes = 0;
        String title = "";
        
        if (fragment instanceof HomeFragment) {
            // iconRes = getOneUiIconId("ic_oui_home"); // ★★★ محذوف ★★★
            iconRes = dev.oneuiproject.oneui.R.drawable.ic_oui_home; // ★★★ معدل ★★★
            title = mContext.getString(R.string.drawer_home);
        } else if (fragment instanceof SettingsFragment) {
            // iconRes = getOneUiIconId("ic_oui_settings"); // ★★★ محذوف ★★★
            iconRes = dev.oneuiproject.oneui.R.drawable.ic_oui_settings; // ★★★ معدل ★★★
            title = mContext.getString(R.string.drawer_settings);
        } else if (fragment instanceof FontViewerFragment) {
            // ★★★ جديد: دعم FontViewerFragment ★★★
            // iconRes = getOneUiIconId("ic_oui_folder"); // ★★★ محذوف ★★★
            iconRes = dev.oneuiproject.oneui.R.drawable.ic_oui_folder; // ★★★ معدل ★★★
            title = mContext.getString(R.string.drawer_font_viewer);
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
        return mFragments != null ?
                mFragments.size() : 0;
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

    /* ★★★ تم حذف دالة getOneUiIconId() بالكامل ★★★ */
    
    public int getSelectedPosition() {
        return mSelectedPos;
    }
}
