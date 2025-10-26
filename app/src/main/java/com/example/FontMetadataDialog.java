package com.example.oneuiapp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.Map;

import dev.oneuiproject.oneui.widget.RoundLinearLayout;

/**
 * Dialog لعرض بيانات meta-data الخط
 */
public class FontMetadataDialog extends AlertDialog {

    private Map<String, String> metadata;
    private Context context;

    public FontMetadataDialog(@NonNull Context context, Map<String, String> metadata) {
        super(context);
        this.context = context;
        this.metadata = metadata;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View dialogView = LayoutInflater.from(context).inflate(
                R.layout.dialog_font_metadata, null);

        setView(dialogView);
        setTitle(R.string.font_metadata_title);

        LinearLayout container = dialogView.findViewById(R.id.metadata_container);

        // ترتيب عرض البيانات
        String[] orderedKeys = {
                "Font Name",
                "Font Family",
                "Subfamily",
                "PostScript Name",
                "Version",
                "Designer",
                "Manufacturer",
                "Copyright",
                "License",
                "License URL",
                "Vendor URL",
                "File Size",
                "File Path"
        };

        for (String key : orderedKeys) {
            if (metadata.containsKey(key)) {
                String value = metadata.get(key);
                if (value != null && !value.isEmpty()) {
                    addMetadataItem(container, key, value);
                }
            }
        }

        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok),
                (dialog, which) -> dismiss());
    }

    private void addMetadataItem(LinearLayout container, String label, String value) {
        View itemView = LayoutInflater.from(context).inflate(
                R.layout.item_font_metadata, container, false);

        TextView labelView = itemView.findViewById(R.id.metadata_label);
        TextView valueView = itemView.findViewById(R.id.metadata_value);

        labelView.setText(getLocalizedLabel(label));
        valueView.setText(value);

        container.addView(itemView);
    }

    private String getLocalizedLabel(String key) {
        switch (key) {
            case "Font Name":
                return context.getString(R.string.font_metadata_font_name);
            case "Font Family":
                return context.getString(R.string.font_metadata_family);
            case "Subfamily":
                return context.getString(R.string.font_metadata_subfamily);
            case "PostScript Name":
                return context.getString(R.string.font_metadata_postscript);
            case "Version":
                return context.getString(R.string.font_metadata_version);
            case "Designer":
                return context.getString(R.string.font_metadata_designer);
            case "Manufacturer":
                return context.getString(R.string.font_metadata_manufacturer);
            case "Copyright":
                return context.getString(R.string.font_metadata_copyright);
            case "License":
                return context.getString(R.string.font_metadata_license);
            case "License URL":
                return context.getString(R.string.font_metadata_license_url);
            case "Vendor URL":
                return context.getString(R.string.font_metadata_vendor_url);
            case "File Size":
                return context.getString(R.string.font_metadata_file_size);
            case "File Path":
                return context.getString(R.string.font_metadata_file_path);
            default:
                return key;
        }
    }
                                         }
