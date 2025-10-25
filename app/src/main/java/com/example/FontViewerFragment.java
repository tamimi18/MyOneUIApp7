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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import dev.oneuiproject.oneui.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * FontViewerFragment - عارض الخطوط مع ميزة عرض معلومات الخط
 * 
 * التحديث الجديد:
 * - إضافة أيقونة معلومات في Toolbar
 * - استخدام FontInfoExtractor لاستخراج بيانات الخط
 * - عرض Dialog بتصميم OneUI يحتوي على جميع معلومات الخط
 */
public class FontViewerFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

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

    private String lastPreviewText = "";
    private SharedPreferences sharedPreferences;

    private ActivityResultLauncher<Intent> fontPickerLauncher;
    private OnFontChangedListener fontChangedListener;

    // متغير لحفظ معلومات الخط الحالي
    private FontInfoExtractor.FontInfo currentFontInfo;

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
        
        // تفعيل القائمة (Menu) لهذا Fragment
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

        // إعداد Toolbar لعرض القائمة
        setupToolbar();

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
     * إعداد Toolbar لعرض أيقونة المعلومات
     */
    private void setupToolbar() {
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                // تفعيل عرض القائمة في الـ Toolbar
                activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
            }
        }
    }

    /**
     * إنشاء القائمة (Menu) في Toolbar
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // إضافة menu الخاص بعارض الخطوط
        inflater.inflate(R.menu.menu_font_viewer, menu);
    }

    /**
     * التعامل مع الضغط على عناصر القائمة
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_font_info) {
            showFontInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * عرض نافذة معلومات الخط
     * 
     * هذه الدالة تتحقق أولاً من وجود خط محدد، ثم تستخدم FontInfoExtractor
     * لاستخراج جميع المعلومات، وأخيراً تعرض Dialog بتصميم OneUI
     */
    private void showFontInfoDialog() {
        // التحقق من وجود خط محدد
        if (currentFontPath == null || currentFontPath.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.font_info_no_font),
                Toast.LENGTH_SHORT).show();
            return;
        }

        // إذا لم تكن المعلومات محملة مسبقاً، نستخرجها الآن
        if (currentFontInfo == null) {
            File fontFile = new File(currentFontPath);
            currentFontInfo = FontInfoExtractor.extractFontInfo(fontFile);
        }

        // إنشاء View الـ Dialog من Layout
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_font_info, null);

        // ملء البيانات في الـ Views
        populateFontInfoViews(dialogView);

        // إنشاء وعرض الـ Dialog
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.font_info_dialog_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * ملء معلومات الخط في Views الخاصة بالـ Dialog
     * 
     * @param dialogView الـ View الرئيسي للـ Dialog
     */
    private void populateFontInfoViews(View dialogView) {
        // المعلومات الأساسية
        TextView fullNameValue = dialogView.findViewById(R.id.font_info_full_name_value);
        TextView familyNameValue = dialogView.findViewById(R.id.font_info_family_name_value);
        TextView styleValue = dialogView.findViewById(R.id.font_info_style_value);
        
        fullNameValue.setText(currentFontInfo.fullName);
        familyNameValue.setText(currentFontInfo.familyName);
        styleValue.setText(currentFontInfo.styleName);

        // التفاصيل التقنية
        TextView versionValue = dialogView.findViewById(R.id.font_info_version_value);
        TextView typeValue = dialogView.findViewById(R.id.font_info_type_value);
        
        versionValue.setText(currentFontInfo.version);
        
        // تحديد نوع الخط (Variable أم Static)
        String fontType = currentFontInfo.isVariable 
                ? getString(R.string.font_info_type_variable)
                : getString(R.string.font_info_type_static);
        typeValue.setText(fontType);

        // معلومات المصمم (اختيارية - تظهر فقط إذا كانت موجودة)
        
        // المصمم
        LinearLayout designerContainer = dialogView.findViewById(R.id.font_info_designer_container);
        View designerDivider = dialogView.findViewById(R.id.font_info_designer_divider);
        TextView designerValue = dialogView.findViewById(R.id.font_info_designer_value);
        
        if (currentFontInfo.designer != null && !currentFontInfo.designer.isEmpty()) {
            designerContainer.setVisibility(View.VISIBLE);
            designerValue.setText(currentFontInfo.designer);
        } else {
            designerContainer.setVisibility(View.GONE);
        }

        // حقوق النشر
        LinearLayout copyrightContainer = dialogView.findViewById(R.id.font_info_copyright_container);
        View copyrightDivider = dialogView.findViewById(R.id.font_info_copyright_divider);
        TextView copyrightValue = dialogView.findViewById(R.id.font_info_copyright_value);
        
        if (currentFontInfo.copyright != null && !currentFontInfo.copyright.isEmpty()) {
            copyrightContainer.setVisibility(View.VISIBLE);
            copyrightValue.setText(currentFontInfo.copyright);
            
            // إظهار الخط الفاصل إذا كان المصمم موجوداً أيضاً
            if (designerContainer.getVisibility() == View.VISIBLE) {
                designerDivider.setVisibility(View.VISIBLE);
            }
        } else {
            copyrightContainer.setVisibility(View.GONE);
        }

        // الوصف
        LinearLayout descriptionContainer = dialogView.findViewById(R.id.font_info_description_container);
        TextView descriptionValue = dialogView.findViewById(R.id.font_info_description_value);
        
        if (currentFontInfo.description != null && !currentFontInfo.description.isEmpty()) {
            descriptionContainer.setVisibility(View.VISIBLE);
            descriptionValue.setText(currentFontInfo.description);
            
            // إظهار الخط الفاصل إذا كان حقوق النشر موجودة أيضاً
            if (copyrightContainer.getVisibility() == View.VISIBLE) {
                copyrightDivider.setVisibility(View.VISIBLE);
            }
        } else {
            descriptionContainer.setVisibility(View.GONE);
        }
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
            loadFontFromPath(fontFile.getAbsolutePath(), fileName, realName);
            saveLastUsedFont(fontFile.getAbsolutePath(), fileName, realName);

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                getString(R.string.font_viewer_error_loading_font),
                Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

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

                // استخراج معلومات الخط عند تحميله
                currentFontInfo = FontInfoExtractor.extractFontInfo(fontFile);

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
        currentFontInfo = null; // إعادة تعيين معلومات الخط

        Typeface defaultTypeface = Typeface.DEFAULT;
        previewSentence.setTypeface(defaultTypeface);
        previewNumbers.setTypeface(defaultTypeface);

        if (fontChangedListener != null) {
            fontChangedListener.onFontCleared();
        }
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
            }
