package com.example.oneuiapp;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

public class DrawerListViewHolder extends RecyclerView.ViewHolder {

    private final AppCompatImageView icon;
    private final TextView title;
    private Typeface normalTf = null;
    private Typeface selectedTf = null;

    public DrawerListViewHolder(View itemView, boolean isSeparator) {
        super(itemView);
        icon = itemView.findViewById(R.id.drawer_item_icon);
        title = itemView.findViewById(R.id.drawer_item_title);
    }

    public void setIcon(@DrawableRes int resId) {
        if (icon != null) icon.setImageResource(resId);
    }

    public void setTitle(CharSequence txt) {
        if (title != null) title.setText(txt);
    }

    public void setSelected(boolean selected) {
        if (title == null) return;
        itemView.setSelected(selected);
        if (selected && selectedTf != null) title.setTypeface(selectedTf);
        else if (normalTf != null) title.setTypeface(normalTf);
        title.setEllipsize(selected ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
    }

    public void applyTypeface(Typeface tf) {
        if (tf == null) return;
        normalTf = tf;
        selectedTf = Typeface.create(tf, Typeface.BOLD);
        if (title != null) title.setTypeface(normalTf);
    }
}
