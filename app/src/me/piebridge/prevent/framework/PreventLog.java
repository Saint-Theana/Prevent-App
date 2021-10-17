package me.piebridge.prevent.framework;

import android.util.Log;

/**
 * Created by thom on 15/7/25.
 */
public class PreventLog {

    public static final String TAG = "Prevent";

    private PreventLog() {

    }

    public static void v(String msg) {
        Log.v(TAG, msg);
    }

    public static void v(String msg, Throwable t) {
        Log.v(TAG, msg, t);
    }

    public static void d(String msg) {
        Log.d(TAG, msg);
    }

    public static void d(String msg, Throwable t) {
        Log.d(TAG, msg, t);
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
    }

    public static void w(String msg, Throwable t) {
        Log.w(TAG, msg, t);
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable t) {
        Log.e(TAG, msg, t);
    }

}
