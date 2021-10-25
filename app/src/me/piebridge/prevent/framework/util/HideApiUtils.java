package me.piebridge.prevent.framework.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import java.lang.reflect.Field;

import me.piebridge.prevent.framework.PreventLog;

public class HideApiUtils {

    private HideApiUtils() {

    }

    public static int getUidForPid(int pid) {
        return Process.getUidForPid(pid);
    }

    public static int getParentPid(int pid) {
        String[] procStatusLabels = {"PPid:"};
        long[] procStatusValues = new long[1];
        procStatusValues[0] = -1;
        Process.readProcLines("/proc/" + pid + "/status", procStatusLabels, procStatusValues);
        return (int) procStatusValues[0];
    }

    public static void forceStopPackage(Context context, String packageName) {
        try {
            PreventLog.e("trying to force stop package" + packageName);
            PackageManager pm = context.getPackageManager();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.forceStopPackage(packageName);
        } catch (Throwable t) {
            t.printStackTrace();
            PreventLog.e("cannot force stop package" + packageName, t);
        }
    }

    public static Object getThis0(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> clazz = object.getClass();
        try {
            Field field = clazz.getDeclaredField("this$0");
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException e) {
            PreventLog.d("cannot find this$0 in class: " + clazz, e);
        } catch (IllegalAccessException e) {
            PreventLog.d("cannot visit this$0 in class: " + clazz, e);
        }
        return null;
    }

}