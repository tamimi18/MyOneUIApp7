package com.example.oneuiapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FontViewerFragment - عارض الخطوط مع استخراج Metadata
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_font_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

        // إعداد زر المعلومات في شريط Drawer (إن وُجد)
        setupInfoButton();
    }

    @Override
    public void onPause() {
        // إزالة زر المعلومات أولاً حتى لا يبقى في Activity الأخرى
        removeInfoButton();

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
     * عرض معلومات الخط في Dialog
     */
    public void showFontMetadata() {
        if (currentFontPath == null || currentFontPath.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.font_metadata_no_font),
                Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> metadata = extractFontMetadata(new File(currentFontPath));

        if (metadata == null || metadata.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.font_metadata_error),
                Toast.LENGTH_SHORT).show();
            return;
        }

        createMetadataDialog(metadata);
    }

    /**
     * استخراج Metadata من ملف TTF/OTF (جدول "name")
     */
    private Map<String, String> extractFontMetadata(File fontFile) {
        Map<String, String> metadata = new LinkedHashMap<>();

        try (RandomAccessFile raf = new RandomAccessFile(fontFile, "r")) {
            raf.seek(0);
            int sfntVersion = raf.readInt();

            if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) {
                return null;
            }

            int numTables = raf.readUnsignedShort();
            raf.skipBytes(6); // تخطي searchRange, entrySelector, rangeShift

            long nameTableOffset = -1;

            for (int i = 0; i < numTables; i++) {
                byte[] tag = new byte[4];
                raf.read(tag);
                String tagName = new String(tag, "US-ASCII");

                raf.skipBytes(4); // checksum
                long offset = readUInt32(raf);
                readUInt32(raf); // length

                if ("name".equals(tagName)) {
                    nameTableOffset = offset;
                    break;
                }
            }

            if (nameTableOffset == -1) {
                return null;
            }

            raf.seek(nameTableOffset);
            raf.readUnsignedShort(); // format
            int count = raf.readUnsignedShort(); // عدد السجلات
            int stringOffset = raf.readUnsignedShort(); // موقع النصوص

            for (int i = 0; i < count; i++) {
                int platformID = raf.readUnsignedShort();
                int encodingID = raf.readUnsignedShort();
                int languageID = raf.readUnsignedShort();
                int nameID = raf.readUnsignedShort();
                int length = raf.readUnsignedShort();
                int offset = raf.readUnsignedShort();

                boolean isWindowsEnglish = (platformID == 3 && languageID == 0x0409);
                boolean isMacEnglish = (platformID == 1 && languageID == 0);

                if (!isWindowsEnglish && !isMacEnglish) {
                    continue;
                }

                long currentPos = raf.getFilePointer();

                raf.seek(nameTableOffset + stringOffset + offset);
                byte[] nameBytes = new byte[length];
                raf.read(nameBytes);

                String value;
                if (platformID == 3) {
                    value = new String(nameBytes, "UTF-16BE").trim();
                } else {
                    value = new String(nameBytes, "US-ASCII").trim();
                }

                String key = getNameIDLabel(nameID);
                if (key != null && !value.isEmpty()) {
                    if (!metadata.containsKey(key)) {
                        metadata.put(key, value);
                    }
                }

                raf.seek(currentPos);
            }

            return metadata;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getNameIDLabel(int nameID) {
        switch (nameID) {
            case 0: return "Copyright";
            case 1: return "Family";
            case 2: return "Subfamily";
            case 3: return "Unique ID";
            case 4: return "Full Name";
            case 5: return "Version";
            case 6: return "PostScript Name";
            case 7: return "Trademark";
            case 8: return "Manufacturer";
            case 9: return "Designer";
            case 10: return "Description";
            case 11: return "Vendor URL";
            case 12: return "Designer URL";
            case 13: return "License";
            case 14: return "License URL";
            case 16: return "Typographic Family";
            case 17: return "Typographic Subfamily";
            case 18: return "Compatible Full";
            case 19: return "Sample Text";
            case 20: return "PostScript CID";
            case 21: return "WWS Family";
            case 22: return "WWS Subfamily";
            default: return null;
        }
    }

    private void createMetadataDialog(Map<String, String> metadata) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, padding / 2);

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null || value.isEmpty() || value.equals("Unknown")) {
                continue;
            }

            addMetadataItem(layout, key, value);
        }

        scrollView.addView(layout);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.font_metadata_title));
        builder.setView(scrollView);
        builder.setPositiveButton(getString(R.string.font_metadata_close), null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addMetadataItem(LinearLayout parent, String key, String value) {
        Context context = requireContext();

        // استرجاع ألوان النص من السمات بشكل آمن
        TypedValue tv = new TypedValue();
        int colorSecondary;
        int colorPrimary;
        if (context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, tv, true)) {
            colorSecondary = tv.data;
        } else {
            colorSecondary = 0xFF757575; // fallback gray
        }
        if (context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true)) {
            colorPrimary = tv.data;
        } else {
            colorPrimary = 0xFF212121; // fallback dark
        }

        TextView titleView = new TextView(context);
        titleView.setText(getLocalizedLabel(key));
        titleView.setTextSize(14);
        titleView.setTextColor(colorSecondary);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        titleView.setLayoutParams(titleParams);

        parent.addView(titleView);

        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextSize(16);
        valueView.setTextColor(colorPrimary);
        valueView.setTextIsSelectable(true);

        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        valueParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        valueView.setLayoutParams(valueParams);

        parent.addView(valueView);
    }

    private String getLocalizedLabel(String key) {
        switch (key) {
            case "Family":
                return getString(R.string.font_metadata_family);
            case "Subfamily":
                return getString(R.string.font_metadata_subfamily);
            case "Full Name":
                return getString(R.string.font_metadata_full_name);
            case "Version":
                return getString(R.string.font_metadata_version);
            case "PostScript Name":
                return getString(R.string.font_metadata_postscript);
            case "Copyright":
                return getString(R.string.font_metadata_copyright);
            case "Trademark":
                return getString(R.string.font_metadata_trademark);
            case "Manufacturer":
                return getString(R.string.font_metadata_manufacturer);
            case "Designer":
                return getString(R.string.font_metadata_designer);
            case "Description":
                return getString(R.string.font_metadata_description);
            case "Vendor URL":
                return getString(R.string.font_metadata_vendor_url);
            case "Designer URL":
                return getString(R.string.font_metadata_designer_url);
            case "License":
                return getString(R.string.font_metadata_license);
            case "License URL":
                return getString(R.string.font_metadata_license_url);
            default:
                return key;
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

    /**
     * إعداد زر المعلومات في Toolbar (DrawerLayout) — يُضاف عند ظهور الـ Fragment
     */
    private void setupInfoButton() {
        if (!(getActivity() instanceof MainActivity)) {
            return;
        }

        MainActivity mainActivity = (MainActivity) getActivity();
        dev.oneuiproject.oneui.layout.DrawerLayout drawerLayout = mainActivity.getDrawerLayout();

        if (drawerLayout == null) {
            return;
        }

        try {
            Class<?> ouiDrawable = Class.forName("dev.oneuiproject.oneui.R$drawable");
            java.lang.reflect.Field iconField = ouiDrawable.getField("ic_oui_info_outline");
            int iconResId = iconField.getInt(null);

            android.graphics.drawable.Drawable infoIcon = requireContext().getDrawable(iconResId);

            drawerLayout.setDrawerButtonIcon(infoIcon);
            drawerLayout.setDrawerButtonTooltip(getString(R.string.menu_font_info));
            drawerLayout.setDrawerButtonOnClickListener(v -> showFontMetadata());

        } catch (Exception e) {
            android.util.Log.e("FontViewerFragment", "Failed to setup info button", e);

            try {
                android.graphics.drawable.Drawable fallbackIcon =
                    requireContext().getDrawable(android.R.drawable.ic_menu_info_details);

                if (fallbackIcon != null) {
                    drawerLayout.setDrawerButtonIcon(fallbackIcon);
                    drawerLayout.setDrawerButtonTooltip(getString(R.string.menu_font_info));
                    drawerLayout.setDrawerButtonOnClickListener(v -> showFontMetadata());
                }
            } catch (Exception ex) {
                // تجاهل إذا فشل كل شيء
            }
        }
    }

    /**
     * إزالة زر المعلومات من Toolbar (DrawerLayout) — يُستدعى عند اختفاء الـ Fragment
     */
    private void removeInfoButton() {
        if (!(getActivity() instanceof MainActivity)) {
            return;
        }

        MainActivity mainActivity = (MainActivity) getActivity();
        dev.oneuiproject.oneui.layout.DrawerLayout drawerLayout = mainActivity.getDrawerLayout();

        if (drawerLayout == null) {
            return;
        }

        drawerLayout.setDrawerButtonIcon(null);
        drawerLayout.setDrawerButtonOnClickListener(null);
    }
                    }
