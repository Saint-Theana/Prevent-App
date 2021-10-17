package com.android.server.am;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ServiceManager;

import com.android.server.wm.ActivityRecord;

import java.util.Set;

public class PreventRunningUtils {
    private static ActivityManagerService ams;
    private static PreventRunning mPreventRunning = new PreventRunning();

    private PreventRunningUtils() {
    }

    private static ActivityManagerService getAms() {
        if (ams == null) {
            ams = (ActivityManagerService) ServiceManager.getService("activity");
        }
        return ams;
    }

    public static boolean isExcludingStopped(Intent intent) {
        String action = intent.getAction();
        return (intent.getFlags() & 48) == 16 && action != null && mPreventRunning.isExcludingStopped(action);
    }

    public static int match(IntentFilter intentFilter, String str, String str2, String str3, Uri uri, Set<String> set, String str4) {
        int match = intentFilter.match(str, str2, str3, uri, set, str4);
        if (match >= 0) {
            return mPreventRunning.match(match, intentFilter, str, str2, str3, uri, set);
        }
        return match;
    }

    public static boolean hookStartProcessLocked(String str, ApplicationInfo applicationInfo, boolean z, int i, String str2, ComponentName componentName) {
        return mPreventRunning.hookStartProcessLocked(getAms().mContext, applicationInfo, str2, componentName);
    }

    public static int onStartActivity(int i, IApplicationThread iApplicationThread, String str, Intent intent) {
        ProcessRecord recordForAppLocked;
        if (i >= 0 && intent != null && ((intent.hasCategory("android.intent.category.HOME") || intent.hasCategory("android.intent.category.LAUNCHER")) && (recordForAppLocked = getAms().getRecordForAppLocked(iApplicationThread)) != null)) {
            mPreventRunning.onStartHomeActivity(recordForAppLocked.info.packageName);
        }
        return i;
    }

    public static void onAppDied(ProcessRecord processRecord) {
        mPreventRunning.onAppDied(processRecord);
    }

    public static boolean returnFalse() {
        return false;
    }

    public static boolean returnFalse(boolean z) {
        return z && !mPreventRunning.isActiviated();
    }

    public static void onCleanUpRemovedTask(Intent intent) {
        if (intent != null && intent.getComponent() != null) {
            mPreventRunning.onCleanUpRemovedTask(intent.getComponent().getPackageName());
        }
    }

    public static void onMoveActivityTaskToBack(IBinder iBinder) {
        ActivityRecord forToken = forToken(iBinder);
        mPreventRunning.onMoveActivityTaskToBack(forToken != null ? forToken.packageName : null);
    }

    public static void setSender(IApplicationThread iApplicationThread) {
        ProcessRecord recordForAppLocked = getAms().getRecordForAppLocked(iApplicationThread);
        mPreventRunning.setSender(recordForAppLocked != null ? recordForAppLocked.info.packageName : String.valueOf(Binder.getCallingUid()));
    }

    public static void clearSender() {
        mPreventRunning.setSender((String) null);
    }

    public static boolean hookStartService(IApplicationThread iApplicationThread, Intent intent) {
        return mPreventRunning.hookStartService(intent);
    }

    public static boolean hookBindService(IApplicationThread iApplicationThread, IBinder iBinder, Intent intent) {
        return mPreventRunning.hookBindService(intent);
    }

    public static void onBroadcastIntent(Intent intent) {
        mPreventRunning.onBroadcastIntent(intent);
    }

    public static void onUserLeavingActivity(IBinder iBinder, boolean z, boolean z2) {
        if (z2) {
            mPreventRunning.onUserLeavingActivity(forToken(iBinder));
        }
    }

    public static void onResumeActivity(IBinder iBinder) {
        mPreventRunning.onResumeActivity(forToken(iBinder));
    }

    public static void onDestroyActivity(IBinder iBinder) {
        mPreventRunning.onDestroyActivity(forToken(iBinder));
    }

    public static void onActivityRequestAudioFocus(int uid,int pid,String clientId,String packageName ){
        mPreventRunning.onActivityRequestAudioFocus(uid,pid,clientId,packageName);
    }
    public static void onActivityAbandonAudioFocus(int uid,int pid,String clientId) {
        mPreventRunning.onActivityAbandonAudioFocus(uid,pid,clientId);
    }

    public static void onActivityLostAudioFocusOnDeath(String clientId) {
        mPreventRunning.onActivityLostAudioFocusOnDeath(clientId);
    }

    public static void onLaunchActivity(IBinder iBinder) {
        mPreventRunning.onLaunchActivity(forToken(iBinder));
    }

    public static void onActivityEstablishVpnConnection(String packageName) {
        mPreventRunning.onActivityEstablishVpnConnection(packageName);
    }

    public static void onVpnConnectionDisconnected() {
        mPreventRunning.onVpnConnectionDisconnected();
    }


    private static ActivityRecord forToken(IBinder iBinder) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ActivityRecord.forTokenLocked(iBinder);
        }
        return ActivityRecord.forToken(iBinder);
    }



}
