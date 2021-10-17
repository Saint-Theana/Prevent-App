package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.util.Log;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class PreventRunning implements PreventRunningHook {
    private static String[] APKS = {"/data/app/me.piebridge.prevent-1/base.apk", "/data/app/me.piebridge.prevent-2/base.apk", "/data/app/me.piebridge.prevent-3/base.apk", "/data/app/me.piebridge.prevent-1.apk", "/data/app/me.piebridge.prevent-2.apk", "/data/app/me.piebridge.prevent-3.apk", "/system/app/Brevent.apk", "/system/app/Brevent/Brevent.apk"};
    private static final String TAG = "Prevent";
    public static final int VERSION = 20161024;
    private PreventRunningHook mPreventRunning;

    public PreventRunning() {
        for (String file : APKS) {
            File file2 = new File(file);
            if (file2.exists()) {
                initPreventRunning(file2);
                return;
            }
        }
    }

    private boolean initPreventRunning(File file) {
        try {
            DexClassLoader dexClassLoader = new DexClassLoader(file.getAbsolutePath(), "/cache", (String) null, Thread.currentThread().getContextClassLoader());
            Log.d(TAG, "loading PreventRunning(20161024) from " + file);
            this.mPreventRunning = (PreventRunningHook) dexClassLoader.loadClass("me.piebridge.prevent.framework.PreventRunning").newInstance();
            setVersion(VERSION);
            setMethod("native");
            return true;
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "cannot find class", e);
            return false;
        } catch (InstantiationException e2) {
            Log.d(TAG, "cannot instance class", e2);
            return false;
        } catch (IllegalAccessException e3) {
            Log.d(TAG, "cannot access class", e3);
            return false;
        } catch (Throwable th) {
            Log.d(TAG, "cannot load PreventRunning from " + file, th);
            return false;
        }
    }

    public void setSender(String str) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.setSender(str);
        }
    }

    public void onBroadcastIntent(Intent intent) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onBroadcastIntent(intent);
        }
    }

    public void onCleanUpRemovedTask(String str) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onCleanUpRemovedTask(str);
        }
    }

    public void onStartHomeActivity(String str) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onStartHomeActivity(str);
        }
    }

    public void onMoveActivityTaskToBack(String str) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onMoveActivityTaskToBack(str);
        }
    }

    public void onAppDied(Object obj) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onAppDied(obj);
        }
    }

    public void onLaunchActivity(Object obj) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onLaunchActivity(obj);
        }
    }

    public void onResumeActivity(Object obj) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onResumeActivity(obj);
        }
    }

    public void onUserLeavingActivity(Object obj) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onUserLeavingActivity(obj);
        }
    }

    public void onDestroyActivity(Object obj) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onDestroyActivity(obj);
        }
    }

    public boolean isExcludingStopped(String str) {
        return this.mPreventRunning == null || this.mPreventRunning.isExcludingStopped(str);
    }

    public boolean hookStartProcessLocked(Context context, ApplicationInfo applicationInfo, String str, ComponentName componentName) {
        return this.mPreventRunning == null || this.mPreventRunning.hookStartProcessLocked(context, applicationInfo, str, componentName);
    }

    public int match(int i, Object obj, String str, String str2, String str3, Uri uri, Set<String> set) {
        if (this.mPreventRunning != null) {
            return this.mPreventRunning.match(i, obj, str, str2, str3, uri, set);
        }
        return i;
    }

    public void setVersion(int i) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.setVersion(i);
        }
    }

    @Override
    public void onActivityRequestAudioFocus(int uid,int pid,String clientId,String packageName ) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onActivityRequestAudioFocus(uid,pid,clientId,packageName);
        }
    }

    @Override
    public void onActivityAbandonAudioFocus(int uid,int pid,String clientId) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onActivityAbandonAudioFocus(uid,pid,clientId);
        }
    }

    @Override
    public void onActivityLostAudioFocusOnDeath(String clientId) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onActivityLostAudioFocusOnDeath(clientId);
        }
    }

    @Override
    public void onActivityEstablishVpnConnection(String packageName) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onActivityEstablishVpnConnection(packageName);
        }
    }

    @Override
    public void onVpnConnectionDisconnected() {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.onVpnConnectionDisconnected();
        }
    }


    public void setMethod(String str) {
        if (this.mPreventRunning != null) {
            this.mPreventRunning.setMethod(str);
        }
    }

    public boolean hookBindService(Intent intent) {
        return this.mPreventRunning == null || this.mPreventRunning.hookBindService(intent);
    }

    public boolean hookStartService(Intent intent) {
        return this.mPreventRunning == null || this.mPreventRunning.hookStartService(intent);
    }

    public boolean isActiviated() {
        return this.mPreventRunning != null;
    }
}
