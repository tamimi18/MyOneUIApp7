package com.example.oneuiapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import dev.oneuiproject.oneui.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class FontViewerActivity extends BaseActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_FONT_PATH = "font_path";
    private static final String KEY_FONT_FILE_NAME = "font_file_name";
    private static final String KEY_FONT_REAL_NAME = "font_real_name";

    private static final String PREF_LAST_FONT_PATH = "last_font_path";
    private static final String PREF_LAST_FONT_FILE_NAME = "last_font_file_name";
    private static final String PREF_LAST_FONT_REAL_NAME = "last_font_real_name";

    private Toolbar mToolbar;
    private LinearLayout selectFontButton;
    private TextView previewSentence;
    private TextView previewNumbers;

    private String currentFontPath;
    private String currentFontFileName;
    private String currentFontRealName;
    private Typeface currentTypeface;
    
    private Map<String, String> currentFontMetadata;

    private String lastPreviewText = "";
    private SharedPreferences sharedPreferences;

    private ActivityResultLauncher<Intent> fontPickerLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(SettingsHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_viewer);

        initViews();
        setupToolbar();
        
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

    private void initViews() {
        mToolbar = findViewById(R.id.toolbar);
        selectFontButton = findViewById(R.id.select_font_button);
        previewSentence = findViewById(R.id.preview_sentence);
        previewNumbers = findViewById(R.id.preview_numbers);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(R.string.drawer_font_viewer);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        String currentPreviewText = SettingsHelper.getPreviewText(this);

        if (!currentPreviewText.equals(lastPreviewText)) {
            lastPreviewText = currentPreviewText;
            updatePreviewTexts();
        }
        
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if ("preview_text".equals(key)) {
            String newPreviewText = SettingsHelper.getPreviewText(this);
            lastPreviewText = newPreviewText;
            updatePreviewTexts();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        if (hasFontSelected()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_font_viewer, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_font_metadata) {
            showMetadataDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updatePreviewTexts() {
        if (previewSentence == null) {
            return;
        }

        String previewText = SettingsHelper.getPreviewText(this);
        previewSentence.setText(previewText);
        previewNumbers.setText(getString(R.string.font_viewer_english_numbers));

        if (currentTypeface != null) {
            applyFontToPreviewTexts();
        }
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
            Toast.makeText(this,
                getString(R.string.font_viewer_error_opening_picker),
                Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFontFromUri(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            File cacheDir = getCacheDir();
            File fontFile = new File(cacheDir, "selected_font.ttf");

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
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

            currentFontMetadata = extractFontMetadata(fontFile);
            String realName = currentFontMetadata.get("Font Name");
            if (realName == null || realName.equals("Unknown Font")) {
                realName = currentFontMetadata.get("Font Family");
            }
            
            loadFontFromPath(fontFile.getAbsolutePath(), fileName, realName);
            saveLastUsedFont(fontFile.getAbsolutePath(), fileName, realName);

        } catch (Exception e) {
            Toast.makeText(this,
                getString(R.string.font_viewer_error_loading_font),
                Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private Map<String, String> extractFontMetadata(File fontFile) {
        Map<String, String> metadata = new HashMap<>();
        
        try (RandomAccessFile raf = new RandomAccessFile(fontFile, "r")) {
            raf.seek(0);
            int sfntVersion = raf.readInt();

            if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) {
                metadata.put("Font Name", "Unknown Font");
                return metadata;
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
                metadata.put("Font Name", "Unknown Font");
                return metadata;
            }

            raf.seek(nameTableOffset);
            raf.readUnsignedShort();
            int count = raf.readUnsignedShort();
            int stringOffset = raf.readUnsignedShort();

            for (int i = 0; i < count; i++) {
                int platformID = raf.readUnsignedShort();
                raf.readUnsignedShort();
                raf.readUnsignedShort();
                int nameID = raf.readUnsignedShort();
                int length = raf.readUnsignedShort();
                int offset = raf.readUnsignedShort();

                if ((platformID == 3 || platformID == 1)) {
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

                    switch (nameID) {
                        case 0:
                            if (!metadata.containsKey("Copyright")) {
                                metadata.put("Copyright", name);
                            }
                            break;
                        case 1:
                            if (!metadata.containsKey("Font Family")) {
                                metadata.put("Font Family", name);
                            }
                            break;
                        case 2:
                            if (!metadata.containsKey("Subfamily")) {
                                metadata.put("Subfamily", name);
                            }
                            break;
                        case 4:
                            if (!metadata.containsKey("Font Name")) {
                                metadata.put("Font Name", name);
                            }
                            break;
                        case 5:
                            if (!metadata.containsKey("Version")) {
                                metadata.put("Version", name);
                            }
                            break;
                        case 6:
                            if (!metadata.containsKey("PostScript Name")) {
                                metadata.put("PostScript Name", name);
                            }
                            break;
                        case 8:
                            if (!metadata.containsKey("Manufacturer")) {
                                metadata.put("Manufacturer", name);
                            }
                            break;
                        case 9:
                            if (!metadata.containsKey("Designer")) {
                                metadata.put("Designer", name);
                            }
                            break;
                        case 11:
                            if (!metadata.containsKey("Vendor URL")) {
                                metadata.put("Vendor URL", name);
                            }
                            break;
                        case 13:
                            if (!metadata.containsKey("License")) {
                                metadata.put("License", name);
                            }
                            break;
                        case 14:
                            if (!metadata.containsKey("License URL")) {
                                metadata.put("License URL", name);
                            }
                            break;
                    }

                    raf.seek(currentPos);
                }
            }

            metadata.put("File Size", formatFileSize(fontFile.length()));
            metadata.put("File Path", fontFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            metadata.put("Font Name", "Unknown Font");
            metadata.put("Error", "Failed to read font metadata");
        }

        return metadata;
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
                
                if (currentFontMetadata == null) {
                    currentFontMetadata = extractFontMetadata(fontFile);
                }

                applyFontToPreviewTexts();
                invalidateOptionsMenu();

            } else {
                throw new Exception("Failed to create Typeface");
            }

        } catch (Exception e) {
            Toast.makeText(this,
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
        
        invalidateOptionsMenu();
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "Unknown Font";

        try {
            android.database.Cursor cursor = getContentResolver()
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
        getSharedPreferences("FontViewerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LAST_FONT_PATH, path)
            .putString(PREF_LAST_FONT_FILE_NAME, fileName)
            .putString(PREF_LAST_FONT_REAL_NAME, realName)
            .apply();
    }

    private void loadLastUsedFont() {
        android.content.SharedPreferences prefs =
            getSharedPreferences("FontViewerPrefs", Context.MODE_PRIVATE);

        String lastPath = prefs.getString(PREF_LAST_FONT_PATH, null);
        String lastFileName = prefs.getString(PREF_LAST_FONT_FILE_NAME, null);
        String lastRealName = prefs.getString(PREF_LAST_FONT_REAL_NAME, null);

        if (lastPath != null && !lastPath.isEmpty()) {
            loadFontFromPath(lastPath, lastFileName, lastRealName);
        }
    }
    
    public void showMetadataDialog() {
        if (currentFontMetadata == null || currentFontMetadata.isEmpty()) {
            Toast.makeText(this, 
                getString(R.string.font_metadata_not_available), 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        FontMetadataDialog dialog = new FontMetadataDialog(this, currentFontMetadata);
        dialog.show();
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
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
