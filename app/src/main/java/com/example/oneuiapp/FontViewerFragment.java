package com.example.oneuiapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import dev.oneuiproject.oneui.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * FontViewerFragment - عارض الخطوط مع تحديث تلقائي لنص المعاينة
 * تم تحسين قراءة جداول name، دعم ترميزات إضافية، وحفظ امتداد الملف، وأخذ صلاحية persistable URI
 */
public class FontViewerFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_FONT_PATH = "font_path";
    private static final String KEY_FONT_FILE_NAME = "font_file_name";
    private static final String KEY_FONT_REAL_NAME = "font_real_name";

    private static final String PREF_LAST_FONT_PATH = "last_font_path";
    private static final String PREF_LAST_FONT_FILE_NAME = "last_font_file_name";
    private static final String PREF_LAST_FONT_REAL_NAME = "last_font_real_name";

    private static final String TAG = "FontViewerFragment";

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

        fontPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK &&
                        result.getData() != null) {
                    Intent data = result.getData();
                    Uri fontUri = data.getData();
                    if (fontUri != null) {
                        try {
                            // أخذ صلاحية Persistable URI إذا كانت متاحة
                            final int takeFlags = data.getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            try {
                                requireContext().getContentResolver().takePersistableUriPermission(fontUri, takeFlags);
                            } catch (SecurityException se) {
                                Log.w(TAG, "takePersistableUriPermission failed: " + se.getMessage());
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error taking persistable permission: " + e.getMessage());
                        }
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
            currentFontFileName = savedInstanceState.getString(KEY_FONT_FILE_NAME);
            currentFontRealName = savedInstanceState.getString(KEY_FONT_REAL_NAME);

            if (currentFontPath != null && !currentFontPath.isEmpty()) {
                loadFontFromPath(currentFontPath, currentFontFileName, currentFontRealName);
            }
        } else {
            loadLastUsedFont();
        }

        updatePreviewTexts();
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
            Log.e(TAG, "Error opening font picker", e);
        }
    }

    private void loadFontFromUri(Uri uri) {
        File fontFile = null;
        String fileName = null;
        try {
            fileName = getFileNameFromUri(uri);
            File cacheDir = requireContext().getCacheDir();

            // احصل على الامتداد إن وجد، وإلا استخدم .ttf كافتراضي
            String ext = ".ttf";
            int idx = fileName != null ? fileName.lastIndexOf('.') : -1;
            if (idx > 0 && idx < fileName.length() - 1) {
                ext = fileName.substring(idx);
            }

            // استخدم اسم فريد لمنع الاستبدال العرضي، مع الحفاظ على الامتداد
            String cachedName = "selected_font_" + UUID.randomUUID().toString() + ext;
            fontFile = new File(cacheDir, cachedName);

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
            loadFontFromPath(fontFile.getAbsolutePath(), fileName != null ? fileName : cachedName, realName);
            saveLastUsedFont(fontFile.getAbsolutePath(), fileName != null ? fileName : cachedName, realName);

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                getString(R.string.font_viewer_error_loading_font),
                Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error loading font from uri: " + (fileName != null ? fileName : uri), e);
            if (fontFile != null && fontFile.exists()) {
                boolean deleted = fontFile.delete();
                if (!deleted) {
                    Log.w(TAG, "Failed to delete temporary font file: " + fontFile.getAbsolutePath());
                }
            }
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
                raf.readFully(tag);
                String tagName = new String(tag, "US-ASCII");

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

                long currentPos = raf.getFilePointer();
                raf.seek(nameTableOffset + stringOffset + offset);

                byte[] nameBytes = new byte[length];
                raf.readFully(nameBytes);

                String name;
                if (platformID == 3 || platformID == 0) {
                    name = new String(nameBytes, "UTF-16BE");
                } else if (platformID == 1) {
                    // Macintosh: حاول MacRoman ثم fallback
                    try {
                        name = new String(nameBytes, "MacRoman");
                    } catch (Exception ex) {
                        name = new String(nameBytes, Charset.forName("ISO-8859-1"));
                    }
                } else {
                    name = new String(nameBytes, Charset.forName("ISO-8859-1"));
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

            return fontName != null ? fontName : (familyName != null ? familyName : "Unknown Font");

        } catch (Exception e) {
            Log.w(TAG, "Failed to extract font real name: " + e.getMessage());
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
            Log.e(TAG, "Error creating typeface from path: " + path, e);
            resetFontDisplay();
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
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
            }

            if ("Unknown Font".equals(fileName)) {
                String path = uri.getPath();
                if (path != null) {
                    int last = path.lastIndexOf('/');
                    if (last >= 0 && last < path.length() - 1) {
                        fileName = path.substring(last + 1);
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to get file name from uri: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
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

    /**
     * Return common font metadata extracted from the loaded font file.
     * Keys potentially returned: FullName, Family, SubFamily, PostScriptName, Version, Manufacturer, FileName, Path
     */
    public java.util.Map<String, String> getFontMetaData() {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        if (currentFontPath == null) return out;

        out.put("Path", currentFontPath);
        out.put("FileName", currentFontFileName != null ? currentFontFileName : "");

        try (RandomAccessFile raf = new RandomAccessFile(new File(currentFontPath), "r")) {
            raf.seek(0);
            int sfntVersion = raf.readInt();
            if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) return out;

            int numTables = raf.readUnsignedShort();
            raf.skipBytes(6);

            long nameTableOffset = -1;

            for (int i = 0; i < numTables; i++) {
                byte[] tag = new byte[4];
                raf.readFully(tag);
                String tagName = new String(tag, "US-ASCII");
                raf.skipBytes(4); // checksum
                long offset = readUInt32(raf);
                long length = readUInt32(raf);
                if ("name".equals(tagName)) {
                    nameTableOffset = offset;
                }
            }

            if (nameTableOffset != -1) {
                raf.seek(nameTableOffset);
                raf.readUnsignedShort(); // format
                int count = raf.readUnsignedShort();
                int stringOffset = raf.readUnsignedShort();
                long strBase = nameTableOffset + stringOffset;

                String fullName = null, family = null, subfamily = null, postScriptName = null, version = null, manuf = null;
                for (int i = 0; i < count; i++) {
                    int platformID = raf.readUnsignedShort();
                    int encodingID = raf.readUnsignedShort();
                    int languageID = raf.readUnsignedShort();
                    int nameID = raf.readUnsignedShort();
                    int length = raf.readUnsignedShort();
                    int offset = raf.readUnsignedShort();

                    long cur = raf.getFilePointer();
                    raf.seek(strBase + offset);
                    byte[] data = new byte[length];
                    raf.readFully(data);

                    String name;
                    if (platformID == 3 || platformID == 0) { // Windows or Unicode
                        name = new String(data, "UTF-16BE");
                    } else if (platformID == 1) { // Macintosh
                        try {
                            name = new String(data, "MacRoman");
                        } catch (Exception ex) {
                            name = new String(data, Charset.forName("ISO-8859-1"));
                        }
                    } else {
                        name = new String(data, Charset.forName("ISO-8859-1"));
                    }

                    switch (nameID) {
                        case 0: if (manuf == null) manuf = name; break;
                        case 1: if (family == null) family = name; break;
                        case 2: if (subfamily == null) subfamily = name; break;
                        case 4: if (fullName == null) fullName = name; break;
                        case 5: if (version == null) version = name; break;
                        case 6: if (postScriptName == null) postScriptName = name; break;
                    }

                    raf.seek(cur);
                }

                if (fullName != null) out.put("FullName", fullName);
                if (family != null) out.put("Family", family);
                if (subfamily != null) out.put("SubFamily", subfamily);
                if (postScriptName != null) out.put("PostScriptName", postScriptName);
                if (version != null) out.put("Version", version);
                if (manuf != null) out.put("Manufacturer", manuf);
            }

        } catch (Exception e) {
            Log.w(TAG, "getFontMetaData failed: " + e.getMessage());
        }

        if (!out.containsKey("FullName") && currentFontRealName != null) {
            out.put("FullName", currentFontRealName);
        }
        return out;
    }
}
```[43dcd9a7-70db-4a1f-b0ae-981daa162054](https://github.com/therajanmaurya/Bulk-SMS-Service/tree/6ea8bfc1abf88f118f23f4e53db00ef8e65b17fa/app%2Fsrc%2Fmain%2Fjava%2Fin%2Fcic%2Fbulksms%2FOpenPhoneContact.java?citationMarker=43dcd9a7-70db-4a1f-b0ae-981daa162054&citationId=1&citationId=2 "github.com")
