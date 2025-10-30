package com.example.oneuiapp;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * BaseActivity يطبق تغليف Context للّوكيل Locale فقط في attachBaseContext،
 * ثم يطبّق الخط (إن وُجد) بعد setContentView في onPostCreate لضمان توافق OneUI.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(SettingsHelper.wrapContext(newBase));
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // تطبيق الخط بأمان بعد إنشاء الواجهة حتى لا نغيّر سلوك الـ inflater أثناء الإقلاع
        try {
            Typeface tf = SettingsHelper.getTypeface(this);
            if (tf != null) {
                TypefaceContextWrapper wrapper = new TypefaceContextWrapper(getBaseContext(), tf);
                wrapper.applyFont(getWindow().getDecorView());
            }
        } catch (Exception e) {
            // لا نسمح لأي استثناء هنا أن ينهار التطبيق عند الإقلاع
            e.printStackTrace();
        }
    }
}
