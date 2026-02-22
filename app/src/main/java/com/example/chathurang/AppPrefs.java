/*
package com.example.chathurang;

import android.content.Context;

public class AppPrefs {
    private static final String PREFS = "chathurang_prefs";
    private static final String KEY_DEMO_DONE = "demo_done";

    public static boolean isDemoCompleted(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DEMO_DONE, false);
    }

    public static void markDemoCompleted(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DEMO_DONE, true)
                .apply();
    }
}
*/
package com.example.chathurang;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {

    private static final String PREFS = "chathurang_prefs";
    private static final String KEY_DEMO_DONE = "demo_done";

    public static boolean isDemoDone(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DEMO_DONE, false);
    }

    public static void markDemoDone(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DEMO_DONE, true)
                .apply();
    }
}
