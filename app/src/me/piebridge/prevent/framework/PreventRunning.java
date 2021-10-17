package me.piebridge.prevent.framework;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;
import android.net.Uri;

import com.android.server.am.PreventRunningHook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import me.piebridge.prevent.BuildConfig;
import me.piebridge.prevent.framework.util.BroadcastFilterUtils;
import me.piebridge.prevent.framework.util.LogcatUtils;
import me.piebridge.prevent.framework.util.SafeActionUtils;

/**
 * Created by thom on 15/10/27.
 */
public class PreventRunning implements PreventRunningHook {

    private final ThreadLocal<String> mSender;


    public PreventRunning() {
        mSender = new ThreadLocal<String>();
        PreventLog.i("prevent running " + BuildConfig.VERSION_NAME);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        SystemHook.setClassLoader(classLoader);
        LogcatUtils.logcat(LogcatUtils.BOOT, "*:v");
    }

    @Override
    public void setSender(String sender) {
        if (SystemHook.isSupported()) {
            mSender.set(sender);
        }
    }

    @Override
    public void onBroadcastIntent(Intent intent) {
        if (SystemHook.isSupported()) {
            String action = intent.getAction();
            if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)) {
                SafeActionUtils.updateWidget(intent.getComponent(), true);
            } else if (AppWidgetManager.ACTION_APPWIDGET_DISABLED.equals(action)) {
                SafeActionUtils.updateWidget(intent.getComponent(), false);
            }
        }
    }

    @Override
    public void onCleanUpRemovedTask(String packageName) {
        if (SystemHook.isSupported()) {
            ActivityManagerServiceHook.onCleanUpRemovedTask(packageName);
        }
    }



    @Override
    public void onStartHomeActivity(String packageName) {
        if (SystemHook.isSupported()) {
            SystemHook.onStartHomeActivity(packageName);
        }
    }

    @Override
    public void onMoveActivityTaskToBack(String packageName) {
        if (SystemHook.isSupported()) {
            SystemHook.onMoveActivityToBack(packageName);
        }
    }

    @Override
    public void onAppDied(Object processRecord) {
        if (SystemHook.isSupported()) {
            SystemHook.onAppDied(processRecord);
        }
    }

    @Override
    public void onLaunchActivity(Object activityRecord) {
        if (SystemHook.isSupported()) {
            String packageName = SystemHook.getPackageNameByActivityRecord(activityRecord);
            PreventLog.i("Activity Launched, PackageName: " + packageName);
            SystemHook.onLaunchActivity(activityRecord);
        }
    }

    @Override
    public void onResumeActivity(Object activityRecord) {
        if (SystemHook.isSupported()) {
            SystemHook.onResumeActivity(activityRecord);
        }
    }

    @Override
    public void onUserLeavingActivity(Object activityRecord) {
        if (SystemHook.isSupported()) {
            SystemHook.onUserLeavingActivity(activityRecord);
        }
    }

    @Override
    public void onActivityRequestAudioFocus(int uid, int pid, String clientId, String packageName) {
        if (SystemHook.isSupported()) {
            SystemHook.onActivityRequestAudioFocus(uid, pid, clientId, packageName);
        }
    }

    @Override
    public void onActivityAbandonAudioFocus(int uid, int pid, String clientId) {
        if (SystemHook.isSupported()) {
        SystemHook.onActivityAbandonAudioFocus( uid, pid, clientId);
        }
    }



    @Override
    public void onActivityLostAudioFocusOnDeath(String clientId) {
        if (SystemHook.isSupported()) {
            SystemHook.onActivityLostAudioFocusOnDeath(clientId);
        }
    }

    @Override
    public void onActivityEstablishVpnConnection(String packageName) {
        if (SystemHook.isSupported()) {
            SystemHook.onActivityEstablishVpnConnection(packageName);
        }
    }

    @Override
    public void onVpnConnectionDisconnected() {
        if (SystemHook.isSupported()) {
            SystemHook.onVpnConnectionDisconnected();
        }
    }


    @Override
    public void onDestroyActivity(Object activityRecord) {
        if (SystemHook.isSupported()) {
            SystemHook.onDestroyActivity(activityRecord);
        }
    }

    @Override
    public boolean isExcludingStopped(String action) {
        return !SystemHook.isSupported() || !SafeActionUtils.isSafeAction(action);
    }

    @Override
    public boolean hookStartProcessLocked(Context context, ApplicationInfo info, String hostingType, ComponentName hostingName) {
        return !SystemHook.isSupported() || ActivityManagerServiceHook.hookStartProcessLocked(context, info, hostingType, hostingName, mSender.get());
    }

    @Override
    public int match(int match, Object filter, String action, String type, String scheme, Uri data, Set<String> categories) {
        if (SystemHook.isSupported() && IntentFilterHook.canHook(match)) {
            IntentFilterMatchResult result;
            if (filter instanceof PackageParser.ActivityIntentInfo) {
                result = IntentFilterHook.hookActivityIntentInfo((PackageParser.ActivityIntentInfo) filter, mSender.get(), action);
            } else if (filter instanceof PackageParser.ServiceIntentInfo) {
                result = IntentFilterHook.hookServiceIntentInfo((PackageParser.ServiceIntentInfo) filter, mSender.get(), action);
            } else if (BroadcastFilterUtils.isBroadcastFilter(filter)) {
                result = IntentFilterHook.hookBroadcastFilter(filter, action, data, categories);
            } else {
                result = IntentFilterMatchResult.NONE;
            }
            if (!result.isNone()) {
                return result.getResult();
            }
        }
        return match;
    }

    @Override
    public void setVersion(int version) {
        PreventLog.d("bridge version: " + version);
        SystemHook.setVersion(version);
    }


    @Override
    public void setMethod(String method) {
        PreventLog.d("bridge method: " + method);
        SystemHook.setMethod(method);
    }

    @Override
    public boolean hookBindService(Intent service) {
        return true;
    }

    @Override
    public boolean hookStartService(Intent service) {
        return true;
    }

}