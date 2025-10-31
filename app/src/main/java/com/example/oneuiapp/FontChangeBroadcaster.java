package com.example.oneuiapp;

import android.content.Context;
import android.content.Intent;

public class FontChangeBroadcaster {
    public static final String ACTION_FONT_CHANGED = "com.example.oneuiapp.ACTION_FONT_CHANGED";

    public static void sendFontChangeBroadcast(Context ctx) {
        Intent i = new Intent(ACTION_FONT_CHANGED);
        ctx.sendBroadcast(i);
    }
}
