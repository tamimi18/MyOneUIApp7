package com.example;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

/**
 * Complete FontViewerFragment with:
 *  - menu inflation (info icon)
 *  - showFontInfoDialog() -> extracts metadata and shows AlertDialog
 *  - getters used by MainActivity: hasFontSelected(), getCurrentFontRealName(), getCurrentFontFileName()
 *
 * Put this file at: app/src/main/java/com/example/FontViewerFragment.java
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
        // مهم جداً — بدون هذا لن يظهر menu الخاص بالفراغمنت
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
        if (previewSentence == null) return;

        String previewText = SettingsHelper.getPreviewText(requireContext());
        previewSentence.setText(previewText);

        // الأرقام الافتراضية — يمكن تعديلها بناءً على اللغة
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

            // حفظ مؤقت باسم مميز بناءً على اسم الملف
            String outName = "selected_font";
            if (fileName != null && fileName.contains(".")) {
                String ext = fileName.substring(fileName.lastIndexOf('.'));
                outName += ext;
            } else {
                outName += ".ttf";
            }

            File fontFile = new File(cacheDir, outName);

            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(fontFile)) {
                if (inputStream == null) throw new Exception("Cannot open font file");
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
     * استخراج الاسم الكامل (Full name) أو اسم العائلة من جدول name
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
                raf.readFully(tagBytes);
                String tagName = new String(tagBytes, "US-ASCII");
                raf.skipBytes(4);
                long offset = readUInt32(raf);
                raf.skipBytes(4);

                if ("name".equals(tagName)) {
                    nameTableOffset = offset;
                    break;
                }
            }

            if (nameTableOffset == -1) return "Unknown Font";

            raf.seek(nameTableOffset);
            raf.readUnsignedShort();
            int count = raf.readUnsignedShort();
            int stringOffset = raf.readUnsignedShort();

            String fontName = null;
            String familyName = null;

            for (int i = 0; i < count; i++) {
                int platformID = raf.readUnsignedShort();
                raf.readUnsignedShort(); // encodingID
                raf.readUnsignedShort(); // languageID
                int nameID = raf.readUnsignedShort();
                int length = raf.readUnsignedShort();
                int offset = raf.readUnsignedShort();

                if ((nameID == 4 || nameID == 1) && (platformID == 3 || platformID == 1)) {
                    long currentPos = raf.getFilePointer();
                    raf.seek(nameTableOffset + stringOffset + offset);
                    byte[] nameBytes = new byte[length];
                    raf.readFully(nameBytes);
                    String name;
                    if (platformID == 3) {
                        name = new String(nameBytes, "UTF-16BE").trim();
                    } else {
                        name = new String(nameBytes, "US-ASCII").trim();
                    }

                    if (nameID == 4) fontName = name;
                    else if (nameID == 1 && familyName == null) familyName = name;

                    raf.seek(currentPos);

                    if (fontName != null) break;
                }
            }

            return fontName != null ? fontName : (familyName != null ? familyName : "Unknown Font");
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
            android.database.Cursor cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex);
                cursor.close();
            }
            if (fileName.equals("Unknown Font") && uri.getPath() != null) {
                String path = uri.getPath();
                int idx = path.lastIndexOf('/');
                if (idx >= 0) fileName = path.substring(idx + 1);
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
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("FontViewerPrefs", Context.MODE_PRIVATE);
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FONT_PATH, currentFontPath);
        outState.putString(KEY_FONT_FILE_NAME, currentFontFileName);
        outState.putString(KEY_FONT_REAL_NAME, currentFontRealName);
    }

    // ========== menu: inflate and handle info icon ==========
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // inflate the menu we provide
        try {
            inflater.inflate(R.menu.font_viewer_menu, menu);
        } catch (Exception e) {
            // if resource not present yet, ignore to avoid crash
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // note: menu item id must match the menu xml below (menu_font_info)
        if (item.getItemId() == R.id.menu_font_info) {
            showFontInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * show dialog with font metadata extracted (calls extractAllMetadata)
     */
    private void showFontInfoDialog() {
        if (!hasFontSelected()) {
            Toast.makeText(requireContext(), getString(R.string.font_info_no_font_selected), Toast.LENGTH_SHORT).show();
            return;
        }
        File fontFile = currentFontPath != null ? new File(currentFontPath) : null;
        if (fontFile == null || !fontFile.exists()) {
            Toast.makeText(requireContext(), getString(R.string.font_info_no_font_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        String metadata;
        try {
            metadata = extractAllMetadata(fontFile);
        } catch (Exception e) {
            metadata = getString(R.string.font_info_error_extracting) + "\n" + e.getMessage();
            e.printStackTrace();
        }

        String title = currentFontRealName != null ? currentFontRealName : (currentFontFileName != null ? currentFontFileName : getString(R.string.font_info_title));
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(metadata)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Extracts readable metadata from a TTF/OTF file.
     * Returns a multi-line string with name table entries and some header info.
     */
    private String extractAllMetadata(File fontFile) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Path: ").append(fontFile.getAbsolutePath()).append("\n");
        sb.append("Size: ").append(fontFile.length()).append(" bytes\n\n");

        try (RandomAccessFile raf = new RandomAccessFile(fontFile, "r")) {
            raf.seek(0);
            int sfnt = raf.readInt();
            String format;
            if (sfnt == 0x4F54544F) format = "OTF (CFF)";
            else if (sfnt == 0x00010000) format = "TTF (TrueType)";
            else format = String.format(Locale.US, "Unknown (0x%08X)", sfnt);
            sb.append("Format: ").append(format).append("\n");

            int numTables = raf.readUnsignedShort();
            sb.append("Tables: ").append(numTables).append("\n");
            raf.skipBytes(6); // searchRange, entrySelector, rangeShift

            long nameOffset = -1;
            long maxpOffset = -1;

            for (int i = 0; i < numTables; i++) {
                byte[] tagb = new byte[4];
                raf.readFully(tagb);
                String tag = new String(tagb, "US-ASCII");
                raf.readInt(); // checksum
                long offset = readUInt32(raf);
                long length = readUInt32(raf);
                if ("name".equals(tag)) nameOffset = offset;
                if ("maxp".equals(tag)) maxpOffset = offset;
            }

            if (nameOffset != -1) {
                raf.seek(nameOffset);
                raf.readUnsignedShort(); // format
                int count = raf.readUnsignedShort();
                int stringOffset = raf.readUnsignedShort();
                sb.append("\n== name table (count=").append(count).append(") ==\n");
                for (int i = 0; i < count; i++) {
                    int platformID = raf.readUnsignedShort();
                    int encodingID = raf.readUnsignedShort();
                    int languageID = raf.readUnsignedShort();
                    int nameID = raf.readUnsignedShort();
                    int length = raf.readUnsignedShort();
                    int offset = raf.readUnsignedShort();

                    long cur = raf.getFilePointer();
                    raf.seek(nameOffset + stringOffset + offset);
                    byte[] nameBytes = new byte[length];
                    raf.readFully(nameBytes);
                    String value;
                    if (platformID == 3) {
                        value = new String(nameBytes, "UTF-16BE").trim();
                    } else {
                        value = new String(nameBytes, "US-ASCII").trim();
                    }
                    sb.append(String.format(Locale.US, "nameID %d (plat=%d enc=%d lang=0x%04X): %s\n",
                            nameID, platformID, encodingID, languageID, value));
                    raf.seek(cur);
                }
            } else {
                sb.append("\n(no name table found)\n");
            }

            if (maxpOffset != -1) {
                raf.seek(maxpOffset);
                int major = raf.readUnsignedShort();
                int minor = raf.readUnsignedShort();
                int numGlyphs = raf.readUnsignedShort();
                sb.append("\nGlyphs (maxp): ").append(numGlyphs).append("\n");
            }
        } catch (Exception ex) {
            sb.append("\nParse error: ").append(ex.getMessage()).append("\n");
            throw ex;
        }

        return sb.toString();
    }

    // ========== public accessors used by MainActivity ==========
    public boolean hasFontSelected() {
        return currentFontPath != null && !currentFontPath.isEmpty();
    }

    public String getCurrentFontRealName() {
        return currentFontRealName;
    }

    public String getCurrentFontFileName() {
        return currentFontFileName;
    }
                              }
