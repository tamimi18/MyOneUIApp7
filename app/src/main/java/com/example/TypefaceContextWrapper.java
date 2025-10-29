package com.example.oneuiapp;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * ContextWrapper يطبق Typeface مخصص على كل TextView يتم إنشاؤه.
 */
public class TypefaceContextWrapper extends ContextWrapper {

    private final Typeface typeface;

    public TypefaceContextWrapper(Context base, Typeface typeface) {
        super(base);
        this.typeface = typeface;
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            LayoutInflater inflater = (LayoutInflater) super.getSystemService(name);
            return inflater.cloneInContext(this);
        }
        return super.getSystemService(name);
    }

    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        View view = null;
        try {
            LayoutInflater inflater = LayoutInflater.from(getBaseContext());
            view = inflater.createView(name, null, attrs);
        } catch (Exception ignored) {}

        if (view instanceof TextView && typeface != null) {
            ((TextView) view).setTypeface(typeface);
        }
        return view;
    }
}
