package com.example.oneuiapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
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
import androidx.documentfile.provider.DocumentFile;

import dev.oneuiproject.oneui.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FontViewerFragment - عارض الخطوط مع تحديث تلقائي لنص المعاينة
 * تحسينات: حفظ takeFlags; اكتشاف امتداد من المحتوى; try-with-resources للـ PFD;
 * تجنب نسخ متكرر إذا سبق النسخ؛ تحسين استرجاع الصلاحيات.
 */
public class FontViewerFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_FONT_PATH = "font_path";
    private static final String KEY_FONT_FILE_NAME = "font_file_name";
    private static final String KEY_FONT_REAL_NAME = "font_real_name";

    private static final String PREF_LAST_FONT_PATH = "last_font_path";
    private static final String PREF_LAST_FONT_FILE_NAME = "last_font_file_name";
    private static final String PREF_LAST_FONT_REAL_NAME = "last_font_real_name";
    private static final String PREF_LAST_FONT_URI = "last_font_uri";
    private static final String PREF_LAST_FONT_TAKEFLAGS = "last_font_takeflags";

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

    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
                        final int flags = data.getFlags();
                        final int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                            try {
                                requireContext().getContentResolver().takePersistableUriPermission(fontUri, takeFlags);
                            } catch (SecurityException se) {
                                Log.w(TAG, "takePersistableUriPermission failed: " + se.getMessage());
                            } catch (Exception e) {
                                Log.w(TAG, "takePersistableUriPermission error: " + e.getMessage());
                            }
                        }

                        bgExecutor.execute(() -> {
                            // قبل النسخ: إذا سبق وجود مسار محلي صالح، استخدمه لتجنب نسخ مكرر
                            File existingLocal = null;
                            try {
                                SharedPreferences prefs = requireContext().getSharedPreferences("FontViewerPrefs", Context.MODE_PRIVATE);
                                String savedPath = prefs.getString(PREF_LAST_FONT_PATH, null);
                                if (savedPath != null) {
                                    File f = new File(savedPath);
                                    if (f.exists()) existingLocal = f;
                                }
                            } catch (Exception ignored) {}

                            File copied;
                            if (existingLocal != null) {
                                copied = existingLocal;
                            } else {
                                copied = copyUriToAppStorage(fontUri, null);
                            }

                            String fileName = getFileNameFromUri(fontUri);
                            String realName = "Unknown Font";
                            if (copied != null && copied.exists()) {
                                realName = extractFontRealName(copied);
                                final String finalRealName = realName;
                                final String finalFileName = fileName != null ? fileName : copied.getName();
                                saveLastUsedFont(copied.getAbsolutePath(), finalFileName, finalRealName);
                                final Uri finalUri = fontUri;
                                mainHandler.post(() -> {
                                    loadFontFromPath(copied.getAbsolutePath(), finalFileName, finalRealName);
                                    saveLastUsedFontUri(finalUri, finalFileName, finalRealName, takeFlags);
                                });
                            } else {
                                mainHandler.post(() -> {
                                    Toast.makeText(requireContext(),
                                            getString(R.string.font_viewer_error_loading_font),
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
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
    public void onDestroyView() {
        super.onDestroyView();
        selectFontButton = null;
        previewSentence = null;
        previewNumbers = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bgExecutor.shutdownNow();
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

    /**
     * نسخ URI إلى مجلد خاص بالتطبيق (filesDir/fonts) ورجوع ملف الوجهة.
     * يجب استدعاؤها على خيط خلفي.
     * يكتشف امتداد الملف من المحتوى (ttc/otf/ttf) إن أمكن.
     */
    private File copyUriToAppStorage(Uri uri, String suggestedName) {
        try {
            String fileName = suggestedName != null ? suggestedName : getFileNameFromUri(uri);
            if (fileName == null) fileName = "font_" + System.currentTimeMillis();

            // اكتشاف الامتداد من المحتوى (افتراضي .ttf)
            String ext = ".ttf";
            // اقرأ أول 4 بايت من URI لتحديد header دون تحميل الملف كله
            try (InputStream probe = requireContext().getContentResolver().openInputStream(uri)) {
                if (probe != null) {
                    byte[] header = new byte[4];
                    int read = probe.read(header);
                    if (read == 4) {
                        String hdr = new String(header, StandardCharsets.US_ASCII);
                        if ("ttcf".equals(hdr)) {
                            ext = ".ttc";
                        } else {
                            // المحافظة على مؤشر؛ افتح RandomAccessFile على stream غير ممكن، لذا افترض otf/ttf عن طريق bytes
                            if ("OTTO".equals(hdr)) ext = ".otf";
                            else ext = ".ttf";
                        }
                    } else {
                        // استخدم امتداد من الاسم إن وُجد
                        int idx = fileName.lastIndexOf('.');
                        if (idx > 0 && idx < fileName.length() - 1) {
                            ext = fileName.substring(idx);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "probe header failed: " + e.getMessage());
                int idx = fileName.lastIndexOf('.');
                if (idx > 0 && idx < fileName.length() - 1) {
                    ext = fileName.substring(idx);
                }
            }

            if (!fileName.endsWith(ext)) {
                // إزالة امتداد موجود ورفعه بالامتداد المكتشف
                int idx = fileName.lastIndexOf('.');
                if (idx > 0) fileName = fileName.substring(0, idx);
                fileName = fileName + ext;
            }

            File fontsDir = new File(requireContext().getFilesDir(), "fonts");
            if (!fontsDir.exists()) {
                boolean ok = fontsDir.mkdirs();
                if (!ok) Log.w(TAG, "Failed to create fonts dir");
            }

            File outFile = new File(fontsDir, "selected_font_" + UUID.randomUUID().toString() + ext);

            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                if (in == null) return null;
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
            }

            return outFile;
        } catch (Exception e) {
            Log.w(TAG, "copyUriToAppStorage failed: " + e.getMessage());
            return null;
        }
    }

    private void loadFontFromPath(String path, String fileName, String realName) {
        try {
            File fontFile = new File(path);

            if (!fontFile.exists()) {
                resetFontDisplay();
                return;
            }

            Typeface typeface = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(fontFile, ParcelFileDescriptor.MODE_READ_ONLY)) {
                    if (pfd != null) {
                        typeface = new Typeface.Builder(pfd.getFileDescriptor()).build();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Typeface.Builder failed, fallback to createFromFile: " + e.getMessage());
                }
            }

            if (typeface == null) {
                typeface = Typeface.createFromFile(fontFile);
            }

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
            if (previewSentence != null) previewSentence.setTypeface(currentTypeface);
            if (previewNumbers != null) previewNumbers.setTypeface(currentTypeface);
        }
    }

    private void resetFontDisplay() {
        currentTypeface = null;
        currentFontPath = null;
        currentFontFileName = null;
        currentFontRealName = null;

        Typeface defaultTypeface = Typeface.DEFAULT;
        if (previewSentence != null) previewSentence.setTypeface(defaultTypeface);
        if (previewNumbers != null) previewNumbers.setTypeface(defaultTypeface);

        if (fontChangedListener != null) {
            fontChangedListener.onFontCleared();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) fileName = cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.w(TAG, "getFileNameFromUri failed: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        if (fileName == null) {
            try {
                DocumentFile doc = DocumentFile.fromSingleUri(requireContext(), uri);
                if (doc != null && doc.getName() != null) {
                    fileName = doc.getName();
                }
            } catch (Exception ignored) {}
        }

        if (fileName == null) {
            String path = uri.getPath();
            if (path != null) {
                int last = path.lastIndexOf('/');
                if (last >= 0 && last < path.length() - 1) {
                    fileName = path.substring(last + 1);
                }
            }
        }

        if (fileName == null) {
            fileName = "font_" + System.currentTimeMillis() + ".ttf";
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

    /**
     * احفظ URI الممنوح كـ String وحاول أخذ persistable permission.
     */
    private void saveLastUsedFontUri(Uri uri, String fileName, String realName, int takeFlags) {
        try {
            if (takeFlags != 0) {
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                } catch (SecurityException se) {
                    Log.w(TAG, "takePersistableUriPermission failed: " + se.getMessage());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "saveLastUsedFontUri permission handling failed: " + e.getMessage());
        }

        requireContext().getSharedPreferences("FontViewerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LAST_FONT_URI, uri.toString())
            .putInt(PREF_LAST_FONT_TAKEFLAGS, takeFlags)
            .putString(PREF_LAST_FONT_FILE_NAME, fileName)
            .putString(PREF_LAST_FONT_REAL_NAME, realName)
            .apply();
    }

    private void loadLastUsedFont() {
        android.content.SharedPreferences prefs =
            requireContext().getSharedPreferences("FontViewerPrefs", Context.MODE_PRIVATE);

        String lastUri = prefs.getString(PREF_LAST_FONT_URI, null);
        String lastFileName = prefs.getString(PREF_LAST_FONT_FILE_NAME, null);
        String lastRealName = prefs.getString(PREF_LAST_FONT_REAL_NAME, null);
        int lastTakeFlags = prefs.getInt(PREF_LAST_FONT_TAKEFLAGS, 0);

        // تحقق أولاً من وجود مسار محلي محفوظ صالح لتجنب نسخ URI إذا لم يكن ضرورياً
        String lastPath = prefs.getString(PREF_LAST_FONT_PATH, null);
        if (lastPath != null && !lastPath.isEmpty()) {
            File f = new File(lastPath);
            if (f.exists()) {
                loadFontFromPath(lastPath, lastFileName, lastRealName);
                return;
            }
        }

        if (lastUri != null) {
            try {
                final Uri uri = Uri.parse(lastUri);

                // حاول استعادة الصلاحية إن أمكن
                if (lastTakeFlags != 0) {
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(uri, lastTakeFlags);
                    } catch (SecurityException se) {
                        Log.w(TAG, "restore persistable permission failed: " + se.getMessage());
                    } catch (Exception e) {
                        Log.w(TAG, "restore permission error: " + e.getMessage());
                    }
                }

                bgExecutor.execute(() -> {
                    File fontFile = copyUriToAppStorage(uri, lastFileName);
                    if (fontFile != null && fontFile.exists()) {
                        final String fn = lastFileName != null ? lastFileName : fontFile.getName();
                        final String rn = lastRealName != null ? lastRealName : extractFontRealName(fontFile);
                        saveLastUsedFont(fontFile.getAbsolutePath(), fn, rn);
                        mainHandler.post(() -> loadFontFromPath(fontFile.getAbsolutePath(), fn, rn));
                    } else {
                        String lastPathFallback = prefs.getString(PREF_LAST_FONT_PATH, null);
                        if (lastPathFallback != null && !lastPathFallback.isEmpty()) {
                            mainHandler.post(() -> loadFontFromPath(lastPathFallback, lastFileName, lastRealName));
                        } else {
                            mainHandler.post(this::resetFontDisplay);
                        }
                    }
                });
                return;
            } catch (Exception e) {
                Log.w(TAG, "loadLastUsedFont failed to parse URI: " + e.getMessage());
            }
        }

        // fallback مباشر إلى PATH إن وُجد
        String lastPath2 = prefs.getString(PREF_LAST_FONT_PATH, null);
        String lastFileName2 = prefs.getString(PREF_LAST_FONT_FILE_NAME, null);
        String lastRealName2 = prefs.getString(PREF_LAST_FONT_REAL_NAME, null);

        if (lastPath2 != null && !lastPath2.isEmpty()) {
            loadFontFromPath(lastPath2, lastFileName2, lastRealName2);
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
                String tagName = new String(tag, StandardCharsets.US_ASCII);
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
                    if (platformID == 3 || platformID == 0) {
                        name = new String(data, StandardCharsets.UTF_16BE);
                    } else if (platformID == 1) {
                        try {
                            name = new String(data, Charset.forName("MacRoman"));
                        } catch (Exception ex) {
                            name = new String(data, StandardCharsets.ISO_8859_1);
                        }
                    } else {
                        name = new String(data, StandardCharsets.ISO_8859_1);
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

    private String extractFontRealName(File fontFile) {
        try (RandomAccessFile raf = new RandomAccessFile(fontFile, "r")) {
            raf.seek(0);
            byte[] header = new byte[4];
            raf.readFully(header);
            String hdr = new String(header, StandardCharsets.US_ASCII);
            long nameTableOffset = -1;

            if ("ttcf".equals(hdr)) {
                // TTC: اقرأ عدد الخطوط ثم offset كـ unsigned
                raf.skipBytes(4); // TTC version
                int numFonts = raf.readInt();
                if (numFonts > 0) {
                    long firstOffset = readUInt32(raf);
                    raf.seek(firstOffset);
                } else {
                    return "Unknown Font";
                }
            } else {
                raf.seek(0);
            }

            int sfntVersion = raf.readInt();
            if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) {
                return "Unknown Font";
            }

            int numTables = raf.readUnsignedShort();
            raf.skipBytes(6);

            for (int i = 0; i < numTables; i++) {
                byte[] tag = new byte[4];
                raf.readFully(tag);
                String tagName = new String(tag, StandardCharsets.US_ASCII);

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
                    name = new String(nameBytes, StandardCharsets.UTF_16BE);
                } else if (platformID == 1) {
                    try {
                        name = new String(nameBytes, Charset.forName("MacRoman"));
                    } catch (Exception ex) {
                        name = new String(nameBytes, StandardCharsets.ISO_8859_1);
                    }
                } else {
                    name = new String(nameBytes, StandardCharsets.ISO_8859_1);
                }

                if (nameID == 4 && (fontName == null || fontName.isEmpty())) {
                    fontName = name;
                } else if (nameID == 1 && (familyName == null || familyName.isEmpty())) {
                    familyName = name;
                } else if (nameID == 6 && (fontName == null || fontName.isEmpty())) {
                    fontName = fontName == null ? name : fontName;
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
    }
