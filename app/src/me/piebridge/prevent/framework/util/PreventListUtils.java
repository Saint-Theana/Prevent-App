package me.piebridge.prevent.framework.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import me.piebridge.prevent.BuildConfig;
import me.piebridge.prevent.common.Configuration;
import me.piebridge.prevent.common.FileUtils;
import me.piebridge.prevent.common.PreventIntent;
import me.piebridge.prevent.framework.PreventLog;

public final class PreventListUtils {

    public static final String SYSTEM_PREVENT_LIST = "me.piebridge.prevent.list";
    public static final String SYSTEM_PREVENT_CONF = "me.piebridge.prevent.conf";

    private static PreventListUtils preventListUtils = new PreventListUtils();

    private PreventListUtils() {

    }

    private File getFile(Context context, String name) {
        String dataDir = null;
        try {
            dataDir = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            PreventLog.d("cannot find package for context: " + context, e);
        }
        if (dataDir == null) {
            dataDir = Environment.getDataDirectory() + "/system/";
        }
        File file = new File(dataDir, name);
        if (file.isDirectory()) {
            FileUtils.eraseFiles(file);
        }
        return file;
    }

    private File getPreventList(Context context) {
        return getFile(context, SYSTEM_PREVENT_LIST);
    }

    private File getPreventConf(Context context) {
        return getFile(context, SYSTEM_PREVENT_CONF);
    }

    public synchronized void save(Context context, Configuration configuration, boolean force) {
        File file = getPreventConf(context);
        if (force || file.isFile()) {
            FileUtils.save(file.getAbsolutePath(), configuration.getBundle());
        }
    }

    public synchronized void save(Context context, Set<String> packages, boolean force) {
        File file = getPreventList(context);
        if (force || file.isFile()) {
            FileUtils.save(file.getAbsolutePath(), new TreeSet<String>(packages));
            PreventLog.i("update prevents: " + packages.size());
        }
    }

    public boolean canLoad(Context context) {
        File file = getPreventList(context);
        return file.isFile() && file.canRead();
    }

    public void onRemoved(Context context) {
        File prevent = getPreventList(context);
        if (prevent.isFile()) {
            prevent.delete();
        }
        File configuration = getPreventConf(context);
        if (configuration.isFile()) {
            configuration.delete();
        }
        LogcatUtils.deleteBootLog();
    }

    public Set<String> load(Context context) {
        return FileUtils.load(getPreventList(context));
    }

    public static boolean notifyNotSupported(Context context) {
        PreventLog.d("notify not supported");
        return notify(context, PreventIntent.ACTION_NOT_SUPPORTED, "not_supported");
    }

    public static boolean notifyNoPrevents(Context context) {
        PreventLog.d("notify no prevent list");
        return notify(context, Intent.ACTION_MAIN, "no_prevents");
    }

    public static boolean notify(Context context, String action, String identifier) {
        ComponentName component = new ComponentName(BuildConfig.APPLICATION_ID, "me.piebridge.prevent.ui.PreventActivity");
        Intent open = new Intent(action);
        open.setComponent(component);
        open.addCategory(Intent.CATEGORY_LAUNCHER);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent activity = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT);

        PackageManager pm = context.getPackageManager();
        Resources resources;
        int icon;
        try {
            icon = getNotificationIcon(context);
            resources = pm.getResourcesForApplication(BuildConfig.APPLICATION_ID);
        } catch (PackageManager.NameNotFoundException e) {
            PreventLog.e("cannot find " + BuildConfig.APPLICATION_ID, e);
            return false;
        }

        String appName = ResourcesUtils.getString(resources, "app_name");
        Notification notification = new NotificationCompat.Builder(context,"default")
                .setAutoCancel(true)
                .setShowWhen(false)
                .setContentTitle(appName)
                .setContentText(ResourcesUtils.getString(resources, identifier))
                .setTicker(appName)
                .setSmallIcon(icon)
                .setContentIntent(activity).build();

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, notification);
        return true;
    }

    private static int getNotificationIcon(Context context) throws PackageManager.NameNotFoundException {
        int icon = context.getResources().getIdentifier("ic_menu_blocked_user", "drawable", context.getPackageName());
        if (icon == 0) {
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).icon;
        } else {
            return icon;
        }
    }

    public boolean canLoadConfiguration(Context context) {
        File file = getPreventConf(context);
        return file.isFile() && file.canRead();
    }

    public boolean loadConfiguration(Context context) {
        Configuration configuration = Configuration.getDefault();
        File file = getPreventConf(context);
        if (!file.isFile()) {
            return false;
        }
        Bundle bundle = new Bundle();
        try {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                int index = line.indexOf('=');
                if (index != -1) {
                    String key = line.substring(0, index);
                    String value = line.substring(index + 1);
                    setValue(bundle, key, value);
                }
            }
        } catch (IOException e) {
            PreventLog.d("cannot load configuration", e);
        }
        configuration.updateBundle(bundle);
        return true;
    }

    private void setValue(Bundle bundle, String key, String value) {
        if (PreventIntent.isBoolean(key)) {
            bundle.putBoolean(key, Boolean.valueOf(value));
        } else if (PreventIntent.isLong(key)) {
            try {
                bundle.putLong(key, Long.parseLong(value));
            } catch (NumberFormatException e) {
                PreventLog.w("cannot parse long from " + value, e);
            }
        }
    }

    public static PreventListUtils getInstance() {
        return preventListUtils;
    }

}
