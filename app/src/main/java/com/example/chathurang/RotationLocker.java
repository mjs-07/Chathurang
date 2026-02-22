// RotationLocker.java
package com.example.chathurang;

import android.app.Activity;
import android.content.pm.ActivityInfo;

public final class RotationLocker {
    private RotationLocker() {}

    // Lock to portrait for the activity lifetime
    public static void lockPortrait(Activity a) {
        a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // Re-enable normal (sensor) rotation
    public static void unlockRotation(Activity a) {
        a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
}
