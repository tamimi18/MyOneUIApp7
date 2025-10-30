package com.example.oneuiapp;

/**
 * واجهة بسيطة تسمح لأي Fragment بتزويد عنوان وأيقونة لدرج التنقل.
 * يمكن تعديلها لاحقاً لتناسب احتياجاتك (مثلاً إرجاع Spannable أو موارد متعددة).
 */
public interface BaseFragmentTitleProvider {
    /**
     * عنوان العنصر الذي سيظهر في درج التنقل
     */
    String getTitle();

    /**
     * معرف مورد الأيقونة (drawable resource id) أو 0 إن لم يوجد
     */
    int getIconResId();
}
