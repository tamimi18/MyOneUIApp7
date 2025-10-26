package com.example.oneuiapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import dev.oneuiproject.oneui.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

/**
 * FontViewerFragment - عارض الخطوط مع استخراج بيانات الخط
 * 
 * الميزات الجديدة:
 * - أيقونة معلومات في Toolbar لعرض بيانات الخط
 * - استخراج شامل لجميع معلومات الخط من جدول 'name' في OpenType
 * - عرض جميل للمعلومات باستخدام Dialog بتصميم OneUI
 */
public class FontViewerFragment extends Fragment 
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_FONT_PATH = "font_path";
    private static final String KEY_FONT_FILE_NAME = "font_file_name";
    private static final String KEY_FONT_REAL_NAME = "font_real_name";

    private static final String PREF_LAST_FONT_PATH = "last_font_path";
    private static final String PREF_LAST_FONT_FILE_NAME = "last_font_file_name";
    private static final String PREF_LAST_FONT_REAL_NAME = "last_font_real_name";

    private LinearLayout selectFontButton;
    private TextView previewSentence;
    private TextView previewNumbers;

    private String currentFontPath;
    private String currentFontFileName;
    private String currentFontRealName;
    private Typeface currentTypeface;

    // حفظ بيانات الخط المستخرجة
    private Map<String, String> currentFontMetadata;

    private String lastPreviewText = "";
    private SharedPreferences sharedPreferences;

    private ActivityResultLauncher<Intent> fontPickerLauncher;
    private OnFontChangedListener fontChangedListener;

    public interface OnFontChangedListener {
        void onFontChanged(String fontRealName, String fontFileName);
        void onFontCleared();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFontChangedListener) {
            fontChangedListener = (OnFontChangedListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fontChangedListener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تفعيل القائمة (Menu) في هذا Fragment
        setHasOptionsMenu(true);

        fontPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK &&
                        result.getData() != null) {
                    Uri fontUri = result.getData().getData();
                    if (fontUri != null) {
                        loadFontFromUri(fontUri);
                    }
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable
                                ViewGroup container,
                                @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_font_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle
                                savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        selectFontButton.setOnClickListener(v -> openFontPicker());

        if (savedInstanceState != null) {
            currentFontPath = savedInstanceState.getString(KEY_FONT_PATH);
            currentFontFileName =
                savedInstanceState.getString(KEY_FONT_FILE_NAME);
            currentFontRealName =
                savedInstanceState.getString(KEY_FONT_REAL_NAME);

            if (currentFontPath != null && !currentFontPath.isEmpty()) {
                loadFontFromPath(currentFontPath, currentFontFileName,
                                 currentFontRealName);
            }
        } else {
            loadLastUsedFont();
        }

        updatePreviewTexts();
    }

    /**
     * إنشاء قائمة الخيارات في Toolbar
     * نضيف أيقونة معلومات هنا
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        
        // إضافة أيقونة معلومات الخط
        // نستخدم أيقونة ic_oui_info_outline من مكتبة OneUI
        menu.add(0, R.id.menu_font_info, 0, R.string.font_viewer_info)
            .setIcon(getOneUiIconId("ic_oui_info_outline"))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    /**
     * تحديد ما إذا كانت أيقونة المعلومات مفعّلة أم لا
     * تكون مفعّلة فقط عندما يكون هناك خط محمّل
     */
    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        MenuItem fontInfoItem = menu.findItem(R.id.menu_font_info);
        if (fontInfoItem != null) {
            // تفعيل الأيقونة فقط إذا كان هناك خط محمّل
            fontInfoItem.setEnabled(hasFontSelected());
            
            // جعل الأيقونة شبه شفافة إذا كانت معطّلة
            if (hasFontSelected()) {
                fontInfoItem.getIcon().setAlpha(255);
            } else {
                fontInfoItem.getIcon().setAlpha(80);
            }
        }
    }

    /**
     * معالجة الضغط على عناصر القائمة
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_font_info) {
            // عرض dialog معلومات الخط
            showFontMetadataDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        String currentPreviewText = SettingsHelper.getPreviewText(requireContext());

        if (!currentPreviewText.equals(lastPreviewText)) {
            lastPreviewText = currentPreviewText;
            updatePreviewTexts();
        }
        
        // تحديث حالة القائمة عند العودة للشاشة
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if ("preview_text".equals(key)) {
            String newPreviewText = SettingsHelper.getPreviewText(requireContext());
            lastPreviewText = newPreviewText;
            updatePreviewTexts();
        }
    }

    private void updatePreviewTexts() {
        if (previewSentence == null) {
            return;
        }

        String previewText = SettingsHelper.getPreviewText(requireContext());
        previewSentence.setText(previewText);
        previewNumbers.setText(getString(R.string.font_viewer_english_numbers));

        if (currentTypeface != null) {
            applyFontToPreviewTexts();
        }
    }

    private void initViews(View view) {
        selectFontButton = view.findViewById(R.id.select_font_button);
        previewSentence = view.findViewById(R.id.preview_sentence);
        previewNumbers = view.findViewById(R.id.preview_numbers);
    }

    private void openFontPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"font/ttf", "font/otf", "application/x-font-ttf",
                             "application/x-font-otf", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            fontPickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                getString(R.string.font_viewer_error_opening_picker),
                Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFontFromUri(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            File cacheDir = requireContext().getCacheDir();
            File fontFile = new File(cacheDir, "selected_font.ttf");

            try (InputStream inputStream =
                     requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(fontFile)) {

                if (inputStream == null) {
                    throw new Exception("Cannot open font file");
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            String realName = extractFontRealName(fontFile);
            
            // استخراج جميع بيانات الخط
            currentFontMetadata = extractFontMetadata(fontFile);
            
            loadFontFromPath(fontFile.getAbsolutePath(), fileName, realName);
            saveLastUsedFont(fontFile.getAbsolutePath(), fileName, realName);
            
            // تحديث القائمة لتفعيل أيقونة المعلومات
            requireActivity().invalidateOptionsMenu();

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                getString(R.string.font_viewer_error_loading_font),
                Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * استخراج اسم الخط الحقيقي من ملف الخط
     * (نفس الدالة القديمة)
     */
    private String extractFontRealName(File fontFile) {
        try (RandomAccessFile raf = new RandomAccessFile(fontFile, "r")) {
            raf.seek(0);
            int sfntVersion = raf.readInt();

            if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) {
                return "Unknown Font";
            }

            int numTables = raf.readUnsignedShort();
            raf.skipBytes(6);

            long nameTableOffset = -1;

            for (int i = 0; i < numTables; i++) {
                byte[] tag = new byte[4];
                raf.read(tag);
                String tagName = new String(tag);

                raf.skipBytes(4);
                long offset = readUInt32(raf);
                readUInt32(raf);

                if ("name".equals(tagName)) {
                    nameTableOffset = offset;
                    break;
                }
            }

            if (nameTableOffset == -1) {
                return "Unknown Font";
            }

            raf.seek(nameTableOffset);
            raf.readUnsignedShort();
            int count = raf.readUnsignedShort();
            int stringOffset = raf.readUnsignedShort();

            String fontName = null;
            String familyName = null;

            for (int i = 0; i < count; i++) {
                int platformID = raf.readUnsignedShort();
                raf.readUnsignedShort();
                raf.readUnsignedShort();
                int nameID = raf.readUnsignedShort();
                int length = raf.readUnsignedShort();
                int offset = raf.readUnsignedShort();

                if ((nameID == 4 || nameID == 1) && (platformID == 3 ||
                                                    platformID == 1)) {
                    long currentPos = raf.getFilePointer();
                    raf.seek(nameTableOffset + stringOffset + offset);

                    byte[] nameBytes = new byte[length];
                    raf.read(nameBytes);

                    String name;
                    if (platformID == 3) {
                        name = new String(nameBytes, "UTF-16BE");
                    } else {
                        name = new String(nameBytes, "US-ASCII");
                    }

                    if (nameID == 4) {
                        fontName = name;
                    } else if (nameID == 1 && familyName == null) {
                        familyName = name;
                    }

                    raf.seek(currentPos);

                    if (fontName != null) {
                        break;
                    }
                }
            }

            return fontName != null ? fontName : (familyName != null ?
                    familyName : "Unknown Font");

        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown Font";
        }
    }

    /**
     * ★★★ دالة جديدة: استخراج جميع بيانات الخط ★★★
     * 
     * هذه الدالة تستخرج معلومات شاملة عن الخط من جدول 'name' في OpenType
     * 
     * كيف تعمل:
     * 1. نفتح ملف الخط كـ RandomAccessFile للقراءة الثنائية
     * 2. نبحث عن جدول 'name' في بنية OpenType
     * 3. نقرأ جميع سجلات الأسماء (Name Records)
     * 4. نحفظ كل معلومة في Map حسب نوعها (nameID)
     * 
     * أنواع المعلومات المستخرجة:
     * - nameID 0: Copyright
     * - nameID 1: Font Family
     * - nameID 2: Subfami (نمط: Regular, Bold, etc.)
     * - nameID 3: Unique ID
     * - nameID 4: Full Font Name
     * - nameID 5: Version
     * - nameID 6: PostScript Name
     * - nameID 7: Trademark
     * - nameID 8: Manufacturer
     * - nameID 9: Designer
     * - nameID 10: Description
     * - nameID 11: Vendor URL
     * - nameID 12: Designer URL
     * - nameID 13: License Description
     * - nameID 14: License URL
     * 
     * @param fontFile ملف الخط المراد استخراج بياناته
     * @return Map يحتوي على جميع البيانات المستخرجة
     */
    private Map<String, String> extractFontMetadata(File fontFile) {
        Map<String, String> metadata = new HashMap<>();
        
        try (RandomAccessFile raf = new RandomAccessFile(fontFile, "r")) {
            // التحقق من صيغة الملف (TrueType أو OpenType)
            raf.seek(0);
            int sfntVersion = raf.readInt();
            
            if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) {
                metadata.put("error", "Invalid font format");
                return metadata;
            }
            
            // قراءة عدد الجداول
            int numTables = raf.readUnsignedShort();
            raf.skipBytes(6); // تخطي searchRange, entrySelector, rangeShift
            
            // البحث عن جدول 'name'
            long nameTableOffset = -1;
            
            for (int i = 0; i < numTables; i++) {
                byte[] tag = new byte[4];
                raf.read(tag);
                String tagName = new String(tag);
                
                raf.skipBytes(4); // تخطي checksum
                long offset = readUInt32(raf);
                readUInt32(raf); // تخطي length
                
                if ("name".equals(tagName)) {
                    nameTableOffset = offset;
                    break;
                }
            }
            
            if (nameTableOffset == -1) {
                metadata.put("error", "Name table not found");
                return metadata;
            }
            
            // قراءة جدول 'name'
            raf.seek(nameTableOffset);
            int format = raf.readUnsignedShort(); // نسخة الجدول (عادة 0)
            int count = raf.readUnsignedShort(); // عدد سجلات الأسماء
            int stringOffset = raf.readUnsignedShort(); // إزاحة بيانات النصوص
            
            // قراءة جميع سجلات الأسماء
            for (int i = 0; i < count; i++) {
                int platformID = raf.readUnsignedShort();
                int encodingID = raf.readUnsignedShort();
                int languageID = raf.readUnsignedShort();
                int nameID = raf.readUnsignedShort();
                int length = raf.readUnsignedShort();
                int offset = raf.readUnsignedShort();
                
                // نحاول قراءة الأسماء من Platform 3 (Windows) أولاً
                // ثم Platform 1 (Macintosh) كخيار بديل
                if ((platformID == 3 && encodingID == 1) || 
                    (platformID == 1 && encodingID == 0)) {
                    
                    // حفظ الموضع الحالي
                    long currentPos = raf.getFilePointer();
                    
                    // الانتقال لموضع النص
                    raf.seek(nameTableOffset + stringOffset + offset);
                    
                    // قراءة النص
                    byte[] nameBytes = new byte[length];
                    raf.read(nameBytes);
                    
                    // تحويل bytes إلى String حسب نوع Platform
                    String nameValue;
                    try {
                        if (platformID == 3) {
                            // Windows: UTF-16 Big Endian
                            nameValue = new String(nameBytes, "UTF-16BE");
                        } else {
                            // Macintosh: ASCII أو MacRoman
                            nameValue = new String(nameBytes, "US-ASCII");
                        }
                    } catch (Exception e) {
                        nameValue = new String(nameBytes);
                    }
                    
                    // تنظيف النص (إزالة null characters)
                    nameValue = nameValue.replace("\0", "").trim();
                    
                    // حفظ المعلومة حسب nameID
                    // نتحقق أولاً أننا لم نحفظ قيمة أفضل مسبقاً
                    String key = getMetadataKeyForNameID(nameID);
                    if (key != null && !nameValue.isEmpty()) {
                        if (!metadata.containsKey(key) || platformID == 3) {
                            // نفضل قيم Windows على Macintosh
                            metadata.put(key, nameValue);
                        }
                    }
                    
                    // العودة للموضع الأصلي
                    raf.seek(currentPos);
                }
            }
            
        } catch (Exception e) {
            metadata.put("error", "Failed to extract metadata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return metadata;
    }

    /**
     * تحويل nameID إلى مفتاح نصي مفهوم
     * 
     * @param nameID رقم نوع المعلومة في OpenType
     * @return مفتاح نصي للاستخدام في Map
     */
    private String getMetadataKeyForNameID(int nameID) {
        switch (nameID) {
            case 0: return "copyright";
            case 1: return "family";
            case 2: return "subfamily";
            case 3: return "unique_id";
            case 4: return "full_name";
            case 5: return "version";
            case 6: return "postscript_name";
            case 7: return "trademark";
            case 8: return "manufacturer";
            case 9: return "designer";
            case 10: return "description";
            case 11: return "vendor_url";
            case 12: return "designer_url";
            case 13: return "license";
            case 14: return "license_url";
            case 16: return "typographic_family";
            case 17: return "typographic_subfamily";
            case 18: return "compatible_full";
            case 19: return "sample_text";
            case 20: return "postscript_cid";
            case 21: return "wws_family";
            case 22: return "wws_subfamily";
            default: return null; // نتجاهل الأنواع غير المعروفة
        }
    }

    /**
     * ★★★ دالة جديدة: عرض dialog معلومات الخط ★★★
     * 
     * هذه الدالة تعرض جميع المعلومات المستخرجة في dialog جميل
     * باستخدام AlertDialog من Android مع تخصيص بسيط
     */
    private void showFontMetadataDialog() {
        if (currentFontMetadata == null || currentFontMetadata.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.font_viewer_no_metadata),
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // بناء النص الذي سيُعرض في Dialog
        StringBuilder message = new StringBuilder();
        
        // ترتيب عرض المعلومات حسب الأهمية
        addMetadataLine(message, "full_name", getString(R.string.font_metadata_full_name));
        addMetadataLine(message, "family", getString(R.string.font_metadata_family));
        addMetadataLine(message, "subfamily", getString(R.string.font_metadata_subfamily));
        addMetadataLine(message, "version", getString(R.string.font_metadata_version));
        addMetadataLine(message, "designer", getString(R.string.font_metadata_designer));
        addMetadataLine(message, "manufacturer", getString(R.string.font_metadata_manufacturer));
        addMetadataLine(message, "copyright", getString(R.string.font_metadata_copyright));
        addMetadataLine(message, "license", getString(R.string.font_metadata_license));
        addMetadataLine(message, "description", getString(R.string.font_metadata_description));
        addMetadataLine(message, "trademark", getString(R.string.font_metadata_trademark));
        addMetadataLine(message, "postscript_name", getString(R.string.font_metadata_postscript));
        addMetadataLine(message, "unique_id", getString(R.string.font_metadata_unique_id));
        
        // إضافة URLs إذا كانت موجودة
        if (currentFontMetadata.containsKey("vendor_url") || 
            currentFontMetadata.containsKey("designer_url") ||
            currentFontMetadata.containsKey("license_url")) {
            
            message.append("\n").append(getString(R.string.font_metadata_links)).append("\n");
            addMetadataLine(message, "vendor_url", getString(R.string.font_metadata_vendor_url));
            addMetadataLine(message, "designer_url", getString(R.string.font_metadata_designer_url));
            addMetadataLine(message, "license_url", getString(R.string.font_metadata_license_url));
        }
        
        // إنشاء وعرض Dialog
        new AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.font_viewer_info_title))
            .setMessage(message.toString())
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.font_viewer_copy_info, (dialog, which) -> {
                // نسخ جميع المعلومات إلى الحافظة
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = 
                    android.content.ClipData.newPlainText("Font Metadata", message.toString());
                clipboard.setPrimaryClip(clip);
                
                Toast.makeText(requireContext(),
                    getString(R.string.font_viewer_info_copied),
                    Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    /**
     * دالة مساعدة لإضافة سطر معلومة إلى النص
     * 
     * @param builder StringBuilder الذي نبني فيه النص
     * @param key مفتاح المعلومة في Map
     * @param label الاسم المعروض للمعلومة
     */
    private void addMetadataLine(StringBuilder builder, String key, String label) {
        if (currentFontMetadata.containsKey(key)) {
            String value = currentFontMetadata.get(key);
            if (value != null && !value.isEmpty()) {
                builder.append(label).append(": ").append(value).append("\n\n");
            }
        }
    }

    private long readUInt32(RandomAccessFile raf) throws Exception {
        return ((long) raf.readInt()) & 0xFFFFFFFFL;
    }

    private void loadFontFromPath(String path, String fileName, String realName) {
        try {
            File fontFile = new File(path);

            if (!fontFile.exists()) {
                resetFontDisplay();
                return;
            }

            Typeface typeface = Typeface.createFromFile(fontFile);

            if (typeface != null) {
                currentTypeface = typeface;
                currentFontPath = path;
                currentFontFileName = fileName;
                currentFontRealName = realName;

                applyFontToPreviewTexts();

                if (fontChangedListener != null) {
                    fontChangedListener.onFontChanged(realName, fileName);
                }
            } else {
                throw new Exception("Failed to create Typeface");
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                getString(R.string.font_viewer_error_loading_font),
                Toast.LENGTH_SHORT).show();
            resetFontDisplay();
            e.printStackTrace();
        }
    }

    private void applyFontToPreviewTexts() {
        if (currentTypeface != null) {
            previewSentence.setTypeface(currentTypeface);
            previewNumbers.setTypeface(currentTypeface);
        }
    }

    private void resetFontDisplay() {
        currentTypeface = null;
        currentFontPath = null;
        currentFontFileName = null;
        currentFontRealName = null;
        currentFontMetadata = null;

        Typeface defaultTypeface = Typeface.DEFAULT;
        previewSentence.setTypeface(defaultTypeface);
        previewNumbers.setTypeface(defaultTypeface);

        if (fontChangedListener != null) {
            fontChangedListener.onFontCleared();
        }
        
        // تحديث القائمة لتعطيل أيقونة المعلومات
        requireActivity().invalidateOptionsMenu();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Unknown Font";

        try {
            android.database.Cursor cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }

            if (fileName.equals("Unknown Font")) {
                String path = uri.getPath();
                if (path != null) {
                    fileName = path.substring(path.lastIndexOf('/') + 1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileName;
    }

    private void saveLastUsedFont(String path, String fileName, String realName) {
        requireContext().getSharedPreferences("FontViewerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LAST_FONT_PATH, path)
            .putString(PREF_LAST_FONT_FILE_NAME, fileName)
            .putString(PREF_LAST_FONT_REAL_NAME, realName)
            .apply();
    }

    private void loadLastUsedFont() {
        android.content.SharedPreferences prefs =
            requireContext().getSharedPreferences("FontViewerPrefs", Context.MODE_PRIVATE);

        String lastPath = prefs.getString(PREF_LAST_FONT_PATH, null);
        String lastFileName = prefs.getString(PREF_LAST_FONT_FILE_NAME, null);
        String lastRealName = prefs.getString(PREF_LAST_FONT_REAL_NAME, null);

        if (lastPath != null && !lastPath.isEmpty()) {
            loadFontFromPath(lastPath, lastFileName, lastRealName);
            
            // استخراج البيانات من الخط المحفوظ
            File fontFile = new File(lastPath);
            if (fontFile.exists()) {
                currentFontMetadata = extractFontMetadata(fontFile);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (currentFontPath != null) {
            outState.putString(KEY_FONT_PATH, currentFontPath);
        }
        if (currentFontFileName != null) {
            outState.putString(KEY_FONT_FILE_NAME, currentFontFileName);
        }
        if (currentFontRealName != null) {
            outState.putString(KEY_FONT_REAL_NAME, currentFontRealName);
        }
    }

    public String getCurrentFontRealName() {
        return currentFontRealName;
    }

    public String getCurrentFontFileName() {
        return currentFontFileName;
    }

    public boolean hasFontSelected() {
        return currentFontPath != null && !currentFontPath.isEmpty();
    }
    
    /**
     * دالة مساعدة للحصول على ID الأيقونة من مكتبة OneUI
     */
    private int getOneUiIconId(String name) {
        try {
            Class<?> r = Class.forName("dev.oneuiproject.oneui.R$drawable");
            java.lang.reflect.Field f = r.getField(name);
            return f.getInt(null);
        } catch (Exception e) {
            return 0;
        }
    }
                           }
