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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import dev.oneuiproject.oneui.widget.Toast;

/**
 * FontViewerFragment - عارض الخطوط مع تحديث تلقائي لنص المعاينة
 * تم تصحيح سطر mimeTypes الذي تسبّب في خطأ التجميع (تمت إزالة انقسام السلسلة).
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

    // حفظ آخر نص معاينة لمعرفة إذا تغير
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
        // Enable options menu in this fragment
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

        // تحديث نص المعاينة لأول مرة
        updatePreviewTexts();
    }

    @Override
    public void onResume() {
        super.onResume();

        // تسجيل المستمع عند إظهار الشاشة
        sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // الحصول على نص المعاينة الحالي من الإعدادات
        String currentPreviewText = SettingsHelper.getPreviewText(requireContext());

        // التحقق: هل تغير النص منذ آخر مرة؟
        if (!currentPreviewText.equals(lastPreviewText)) {
            // نعم تغير! نحدّث المعاينة
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

    /**
     * تحديث نصوص المعاينة بالنص المحفوظ في الإعدادات
     * مع تطبيق الخط المخصص إذا كان موجوداً
     */
    private void updatePreviewTexts() {
        if (previewSentence == null) {
            return;
        }

        // الحصول على نص المعاينة المخصص من الإعدادات
        String previewText = SettingsHelper.getPreviewText(requireContext());

        // تطبيق النص على المعاينة
        previewSentence.setText(previewText);

        // الأرقام تبقى كما هي (افتراضية)
        previewNumbers.setText(getString(R.string.font_viewer_english_numbers));

        // تطبيق الخط المخصص إذا كان موجوداً
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
                 FileOutputStream outputStream = new FileOutputStream(fontFile))
            {
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

    /**
     * استخراج الاسم الكامل للخط (Full Name) من جدول الأسماء (name table) في ملف الخط
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
                byte[] tagBytes = new byte[4];
                raf.read(tagBytes);
                String tagName = new String(tagBytes);

                raf.skipBytes(4);
                long offset = readUInt32(raf);
                raf.skipBytes(4);

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

                if ((nameID == 4 || nameID == 1) && (platformID == 3 || platformID == 1)) {
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
            android.database.Cursor cursor =
                requireContext().getContentResolver()
                    .query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex =
                    cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
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
        requireContext().getSharedPreferences("FontViewerPrefs",
            Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LAST_FONT_PATH, path)
            .putString(PREF_LAST_FONT_FILE_NAME, fileName)
            .putString(PREF_LAST_FONT_REAL_NAME, realName)
            .apply();
    }

    private void loadLastUsedFont() {
        android.content.SharedPreferences prefs =
            requireContext().getSharedPreferences("FontViewerPrefs",
                Context.MODE_PRIVATE);

        String lastPath = prefs.getString(PREF_LAST_FONT_PATH, null);
        String lastFileName = prefs.getString(PREF_LAST_FONT_FILE_NAME, null);
        String lastRealName = prefs.getString(PREF_LAST_FONT_REAL_NAME, null);

        if (lastPath != null && !lastPath.isEmpty()) {
            File fontFile = new File(lastPath);
            if (fontFile.exists()) {
                loadFontFromPath(lastPath, lastFileName, lastRealName);
            }
        }
    }

    // ﹣﹣﹣﹣﹣ Data for fragment preservation ﹣﹣﹣﹣﹣

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FONT_PATH, currentFontPath);
        outState.putString(KEY_FONT_FILE_NAME, currentFontFileName);
        outState.putString(KEY_FONT_REAL_NAME, currentFontRealName);
    }

    // ﹣﹣﹣﹣﹣ Fragment UI ﹣﹣﹣﹣﹣

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu with the info icon
        inflater.inflate(R.menu.info_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_info) {
            showFontInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows a dialog with the font metadata: Full Name, Family, Version, Designer, License, Creation Date.
     */
    private void showFontInfoDialog() {
        if (currentFontPath == null || currentFontPath.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.font_viewer_no_font_selected),
                Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File fontFile = new File(currentFontPath);
            if (!fontFile.exists()) {
                Toast.makeText(requireContext(),
                    getString(R.string.font_viewer_error_loading_font),
                    Toast.LENGTH_SHORT).show();
                return;
            }
            try (RandomAccessFile raf = new RandomAccessFile(fontFile, "r")) {
                // Read SFNT header
                raf.seek(0);
                int sfntVersion = raf.readInt();
                if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) {
                    Toast.makeText(requireContext(),
                        getString(R.string.font_viewer_error_loading_font),
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                int numTables = raf.readUnsignedShort();
                raf.skipBytes(6);

                long nameOffset = -1, headOffset = -1;
                for (int i = 0; i < numTables; i++) {
                    byte[] tagBytes = new byte[4];
                    raf.read(tagBytes);
                    String tag = new String(tagBytes, "US-ASCII");
                    raf.skipBytes(4);
                    long offset = readUInt32(raf);
                    raf.skipBytes(4);
                    if ("name".equals(tag)) {
                        nameOffset = offset;
                    } else if ("head".equals(tag)) {
                        headOffset = offset;
                    }
                    if (nameOffset != -1 && headOffset != -1) {
                        break;
                    }
                }

                String fullName = null, familyName = null, version = null, designer = null, license = null;
                if (nameOffset > 0) {
                    raf.seek(nameOffset);
                    raf.readUnsignedShort(); // format
                    int count = raf.readUnsignedShort();
                    int stringOffset = raf.readUnsignedShort();
                    for (int i = 0; i < count; i++) {
                        int platformID = raf.readUnsignedShort();
                        raf.readUnsignedShort(); // encodingID
                        raf.readUnsignedShort(); // languageID
                        int nameID = raf.readUnsignedShort();
                        int length = raf.readUnsignedShort();
                        int nameStringOffset = raf.readUnsignedShort();
                        long savePos = raf.getFilePointer();
                        if ((platformID == 3 || platformID == 1) &&
                            (nameID == 1 || nameID == 4 || nameID == 5 || nameID == 9 || nameID == 13)) {
                            raf.seek(nameOffset + stringOffset + nameStringOffset);
                            byte[] bytes = new byte[length];
                            raf.read(bytes);
                            String value;
                            if (platformID == 3) {
                                value = new String(bytes, "UTF-16BE");
                            } else {
                                value = new String(bytes, "US-ASCII");
                            }
                            if (nameID == 1 && familyName == null) {
                                familyName = value;
                            } else if (nameID == 4 && fullName == null) {
                                fullName = value;
                            } else if (nameID == 5 && version == null) {
                                version = value;
                            } else if (nameID == 9 && designer == null) {
                                designer = value;
                            } else if (nameID == 13 && license == null) {
                                license = value;
                            }
                        }
                        raf.seek(savePos);
                    }
                }

                String creationDate = null;
                if (headOffset > 0) {
                    raf.seek(headOffset + 20);
                    long created = raf.readLong();
                    long offsetSec = 2082844800L;
                    long unixSec = created - offsetSec;
                    if (unixSec > 0) {
                        long millis = unixSec * 1000L;
                        Date date = new Date(millis);
                        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
                        creationDate = fmt.format(date);
                    }
                }

                StringBuilder msg = new StringBuilder();
                if (fullName != null) {
                    msg.append(getString(R.string.font_full_name))
                       .append(": ").append(fullName).append("\n");
                }
                if (familyName != null) {
                    msg.append(getString(R.string.font_family))
                       .append(": ").append(familyName).append("\n");
                }
                if (version != null) {
                    msg.append(getString(R.string.font_version))
                       .append(": ").append(version).append("\n");
                }
                if (designer != null) {
                    msg.append(getString(R.string.font_designer))
                       .append(": ").append(designer).append("\n");
                }
                if (license != null) {
                    msg.append(getString(R.string.font_license))
                       .append(": ").append(license).append("\n");
                }
                if (creationDate != null) {
                    msg.append(getString(R.string.font_creation_date))
                       .append(": ").append(creationDate).append("\n");
                }
                if (msg.length() == 0) {
                    msg.append(getString(R.string.font_viewer_no_font_selected));
                }

                new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.font_info_title)
                    .setMessage(msg.toString())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();

            }
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                getString(R.string.font_viewer_error_loading_font),
                Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
                 }
