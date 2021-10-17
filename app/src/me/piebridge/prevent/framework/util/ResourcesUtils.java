package me.piebridge.prevent.framework.util;

import android.content.res.Resources;

import me.piebridge.prevent.BuildConfig;

/**
 * Created by thom on 2016/10/16.
 */

public class ResourcesUtils {

    private ResourcesUtils() {

    }

    public static String getString(Resources resources, String identifier) {
        int resId = resources.getIdentifier(identifier, "string", BuildConfig.APPLICATION_ID);
        if (resId != 0) {
            return resources.getString(resId);
        } else {
            return "(" + identifier + ")";
        }
    }

    public static String formatString(Resources resources, String identifier, int size) {
        int resId = resources.getIdentifier(identifier, "string", BuildConfig.APPLICATION_ID);
        if (resId != 0) {
            return resources.getString(resId, size);
        } else {
            return "(" + identifier + ", " + size + ")";
        }
    }

}
