package com.example.oneuiapp;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

public class DrawerListViewHolder extends RecyclerView.ViewHolder {
    
    private Typeface mNormalTypeface;
    private Typeface mSelectedTypeface;
    private AppCompatImageView mIconView;
    private TextView mTitleView;

    public DrawerListViewHolder(@NonNull View itemView) {
        super(itemView);
        mIconView = itemView.findViewById(R.id.drawer_item_icon);
        mTitleView = itemView.findViewById(R.id.drawer_item_title);
        mNormalTypeface = Typeface.create("sec-roboto-light", Typeface.NORMAL);
        mSelectedTypeface = Typeface.create("sec-roboto-light", Typeface.BOLD);
    }

    public void setIcon(@DrawableRes int resId) {
        mIconView.setImageResource(resId);
    }

    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    public void setSelected(boolean selected) {
        itemView.setSelected(selected);
        mTitleView.setTypeface(selected ? mSelectedTypeface : mNormalTypeface);
        mTitleView.setEllipsize(selected ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
    }
}
