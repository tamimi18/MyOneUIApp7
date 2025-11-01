package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
public class DrawerListViewHolder extends RecyclerView.ViewHolder {

    private AppCompatImageView mIconView;
    private TextView mTitleView;
    public DrawerListViewHolder(@NonNull View itemView) {
        super(itemView);
        mIconView = itemView.findViewById(R.id.drawer_item_icon);
        mTitleView = itemView.findViewById(R.id.drawer_item_title);
    }

    public void setIcon(@DrawableRes int resId) {
        mIconView.setImageResource(resId);
    }

    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    public void setSelected(boolean selected) {
        itemView.setSelected(selected);
        // Context ctx = itemView.getContext(); // ★★★ محذوف ★★★
        // Typeface chosen = SettingsHelper.getTypeface(ctx); // ★★★ محذوف ★★★

        /* ★★★ محذوف ★★★
        if (chosen == null) {
            Typeface fallback = Typeface.create(Typeface.SANS_SERIF, selected ? Typeface.BOLD : Typeface.NORMAL);
            mTitleView.setTypeface(fallback);
        } else {
            mTitleView.setTypeface(selected ? Typeface.create(chosen, Typeface.BOLD) : chosen);
        }
        */
        
        // ★★★ معدل: دع السمة (Theme) تتحكم في الخط ★★★
        // استخدم font-family الافتراضي من السمة، فقط قم بتبديل BOLD
        Typeface tf = mTitleView.getTypeface(); // الخط الحالي من السمة
        if (tf == null) tf = Typeface.SANS_SERIF; // Fallback
        mTitleView.setTypeface(tf, selected ? Typeface.BOLD : Typeface.NORMAL);


        mTitleView.setEllipsize(selected ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
    }
}
