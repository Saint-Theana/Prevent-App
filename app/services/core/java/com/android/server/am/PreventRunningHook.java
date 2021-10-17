package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import java.util.Set;

public interface PreventRunningHook {
    boolean hookBindService(Intent intent);

    boolean hookStartProcessLocked(Context context, ApplicationInfo applicationInfo, String str, ComponentName componentName);

    boolean hookStartService(Intent intent);

    boolean isExcludingStopped(String str);

    int match(int i, Object obj, String str, String str2, String str3, Uri uri, Set<String> set);

    void onAppDied(Object obj);

    void onBroadcastIntent(Intent intent);

    void onCleanUpRemovedTask(String str);

    void onDestroyActivity(Object obj);

    void onLaunchActivity(Object obj);

    void onMoveActivityTaskToBack(String str);

    void onResumeActivity(Object obj);

    void onStartHomeActivity(String str);

    void onUserLeavingActivity(Object obj);

    void setMethod(String str);

    void setSender(String str);

    void setVersion(int i);

    void onActivityRequestAudioFocus(int uid,int pid,String clientId,String packageName );

    void onActivityAbandonAudioFocus(int uid,int pid,String clientId);

    void onActivityLostAudioFocusOnDeath(String clientId);

    void onActivityEstablishVpnConnection(String packageName);

    void onVpnConnectionDisconnected();
}
