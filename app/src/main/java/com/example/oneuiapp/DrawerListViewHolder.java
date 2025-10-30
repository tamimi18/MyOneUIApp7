package com.example.oneuiapp;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class DrawerListViewHolder extends RecyclerView.ViewHolder {

    private final ImageView icon;
    private final TextView title;

    public DrawerListViewHolder(View itemView) {
        super(itemView);
        icon = itemView.findViewById(R.id.drawer_item_icon);
        title = itemView.findViewById(R.id.drawer_item_title);
    }

    public void setIcon(int resId) {
        if (icon != null) icon.setImageResource(resId);
    }

    public void setTitle(String txt) {
        if (title != null) title.setText(txt);
    }

    public void setSelected(boolean selected) {
        itemView.setSelected(selected);
        if (title != null) title.setSelected(selected);
    }

    public void applyTypeface(android.graphics.Typeface tf) {
        if (tf != null && title != null) title.setTypeface(tf);
    }
}
