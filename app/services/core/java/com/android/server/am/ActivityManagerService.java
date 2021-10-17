//
// Decompiled by Jadx - 1033ms
//
package com.android.server.am;

import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.TransactionTooLargeException;

public abstract class ActivityManagerService implements IBinder {
    Context mContext;

    public ActivityManagerService() {
    }

    ProcessRecord getRecordForAppLocked(IApplicationThread caller) {
        throw new UnsupportedOperationException();
    }

    final ProcessRecord startProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting, boolean isolated, int isolatedUid, boolean keepIfLarge, String abiOverride, String entryPoint, String[] entryPointArgs, Runnable crashHandler) {
        if (PreventRunningUtils.hookStartProcessLocked(processName, info, knownToBeDead, intentFlags, hostingType, hostingName)) {
            return startProcessLocked$Pr(processName, info, knownToBeDead, intentFlags, hostingType, hostingName, allowWhileBooting, isolated, isolatedUid, keepIfLarge, abiOverride, entryPoint, entryPointArgs, crashHandler);
        }
        return null;
    }

    final ProcessRecord startProcessLocked(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting, boolean isolated, boolean keepIfLarge) {
        if (PreventRunningUtils.hookStartProcessLocked(processName, info, knownToBeDead, intentFlags, hostingType, hostingName)) {
            return startProcessLocked$Pr(processName, info, knownToBeDead, intentFlags, hostingType, hostingName, allowWhileBooting, isolated, keepIfLarge);
        }
        return null;
    }

    public final int startActivity(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
        return PreventRunningUtils.onStartActivity(startActivity$Pr(caller, callingPackage, intent, resolvedType, resultTo, resultWho, requestCode, startFlags, profilerInfo, bOptions), caller, callingPackage, intent);
    }

    public final int startActivity(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, String profileFile, ParcelFileDescriptor profileFd, Bundle options) {
        return PreventRunningUtils.onStartActivity(startActivity$Pr(caller, callingPackage, intent, resolvedType, resultTo, resultWho, requestCode, startFlags, profileFile, profileFd, options), caller, callingPackage, intent);
    }

    private final void handleAppDiedLocked(ProcessRecord app, boolean restarting, boolean allowRestart) {
        handleAppDiedLocked$Pr(app, restarting, allowRestart);
        if (!restarting && allowRestart && !app.killedByAm) {
            PreventRunningUtils.onAppDied(app);
        }
    }

    final ProcessRecord startProcessLocked(String str, ApplicationInfo applicationInfo, boolean z, int i, String str2, ComponentName componentName, boolean z2, boolean z3, int i2, boolean z4, String str3, String str4, String[] strArr, Runnable runnable, String str5) {
        if (PreventRunningUtils.hookStartProcessLocked(str, applicationInfo, z, i, str2, componentName)) {
            return startProcessLocked$Pr(str, applicationInfo, z, i, str2, componentName, z2, z3, i2, z4, str3, str4, strArr, runnable, str5);
        }
        return null;
    }

    final ProcessRecord startProcessLocked$Pr(String str, ApplicationInfo applicationInfo, boolean z, int i, String str2, ComponentName componentName, boolean z2, boolean z3, int i2, boolean z4, String str3, String str4, String[] strArr, Runnable runnable, String str5) {
        throw new UnsupportedOperationException();
    }

    private void cleanUpRemovedTaskLocked(TaskRecord tr, boolean killProcess, boolean removeFromRecents) {
        try {
            cleanUpRemovedTaskLocked$Pr(tr, killProcess, removeFromRecents);
        } finally {
            if (killProcess) {
                PreventRunningUtils.onCleanUpRemovedTask(tr.getBaseIntent());
            }
        }
    }

    private void cleanUpRemovedTaskLocked(TaskRecord tr, boolean killProcess) {
        try {
            cleanUpRemovedTaskLocked$Pr(tr, killProcess);
        } finally {
            if (killProcess) {
                PreventRunningUtils.onCleanUpRemovedTask(tr.getBaseIntent());
            }
        }
    }

    private void cleanUpRemovedTaskLocked(TaskRecord tr, boolean killProcess, int flags) {
        try {
            cleanUpRemovedTaskLocked$Pr(tr, killProcess, flags);
        } finally {
            if (killProcess) {
                PreventRunningUtils.onCleanUpRemovedTask(tr.getBaseIntent());
            }
        }
    }

    private void cleanUpRemovedTaskLocked(TaskRecord tr, int flags) {
        try {
            cleanUpRemovedTaskLocked$Pr(tr, flags);
        } finally {
            // REMOVE_TASK_KILL_PROCESS = 0x0001
            if ((flags & 0x0001) != 0) {
                Intent baseIntent = new Intent(tr.intent != null ? tr.intent : tr.affinityIntent);
                PreventRunningUtils.onCleanUpRemovedTask(baseIntent);
            }
        }
    }


    public boolean moveActivityTaskToBack(IBinder token, boolean nonRoot) {
        if (!moveActivityTaskToBack$Pr(token, nonRoot)) {
            return false;
        }
        PreventRunningUtils.onMoveActivityTaskToBack(token);
        return true;
    }
    public ComponentName startService(IApplicationThread caller, Intent service, String str, boolean z, String str2, String str3, int i) throws TransactionTooLargeException {
        try {
            PreventRunningUtils.setSender(caller);
            if (PreventRunningUtils.hookStartService(caller, service)) {
                return startService$Pr(caller, service, str,z,  str2,  str3,  i);
            }
            PreventRunningUtils.clearSender();
            return null;
        } finally {
            PreventRunningUtils.clearSender();
        }

    }

    public ComponentName startService$Pr(IApplicationThread iApplicationThread, Intent intent, String str, boolean z, String str2, String str3, int i) throws TransactionTooLargeException {
        throw new UnsupportedOperationException();
    }
        public ComponentName startService(IApplicationThread caller, Intent service, String resolvedType, String callingPackage, int userId) throws TransactionTooLargeException {
        try {
            PreventRunningUtils.setSender(caller);
            if (PreventRunningUtils.hookStartService(caller, service)) {
                return startService$Pr(caller, service, resolvedType, callingPackage, userId);
            }
            PreventRunningUtils.clearSender();
            return null;
        } finally {
            PreventRunningUtils.clearSender();
        }
    }

    public ComponentName startService(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        try {
            PreventRunningUtils.setSender(caller);
            if (PreventRunningUtils.hookStartService(caller, service)) {
                return startService$Pr(caller, service, resolvedType, userId);
            }
            PreventRunningUtils.clearSender();
            return null;
        } finally {
            PreventRunningUtils.clearSender();
        }
    }

    public int bindService(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, String callingPackage, int userId) throws TransactionTooLargeException {
        try {
            PreventRunningUtils.setSender(caller);
            if (PreventRunningUtils.hookBindService(caller, token, service)) {
                return bindService$Pr(caller, token, service, resolvedType, connection, flags, callingPackage, userId);
            }
            PreventRunningUtils.clearSender();
            return 0;
        } finally {
            PreventRunningUtils.clearSender();
        }
    }

    public int bindService(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, int userId) {
        try {
            PreventRunningUtils.setSender(caller);
            if (PreventRunningUtils.hookBindService(caller, token, service)) {
                return bindService$Pr(caller, token, service, resolvedType, connection, flags, userId);
            }
            PreventRunningUtils.clearSender();
            return 0;
        } finally {
            PreventRunningUtils.clearSender();
        }
    }

    public final int broadcastIntent(IApplicationThread caller, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle bOptions, boolean serialized, boolean sticky, int userId) {
        try {
            PreventRunningUtils.setSender(caller);
            int res = broadcastIntent$Pr(caller, intent, resolvedType, resultTo, resultCode, resultData, resultExtras, requiredPermissions, appOp, bOptions, serialized, sticky, userId);
            if (res == 0) {
                PreventRunningUtils.onBroadcastIntent(intent);
            }
            return res;
        } finally {
            PreventRunningUtils.clearSender();
        }
    }

    public final int broadcastIntent(IApplicationThread caller, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle map, String requiredPermission, int appOp, boolean serialized, boolean sticky, int userId) {
        try {
            PreventRunningUtils.setSender(caller);
            int res = broadcastIntent$Pr(caller, intent, resolvedType, resultTo, resultCode, resultData, map, requiredPermission, appOp, serialized, sticky, userId);
            if (res == 0) {
                PreventRunningUtils.onBroadcastIntent(intent);
            }
            return res;
        } finally {
            PreventRunningUtils.clearSender();
        }
    }

    final ProcessRecord startProcessLocked$Pr(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting, boolean isolated, int isolatedUid, boolean keepIfLarge, String abiOverride, String entryPoint, String[] entryPointArgs, Runnable crashHandler) {
        throw new UnsupportedOperationException();
    }

    final ProcessRecord startProcessLocked$Pr(String processName, ApplicationInfo info, boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName, boolean allowWhileBooting, boolean isolated, boolean keepIfLarge) {
        throw new UnsupportedOperationException();
    }

    public final int startActivity$Pr(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
        throw new UnsupportedOperationException();
    }

    public final int startActivity$Pr(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, String profileFile, ParcelFileDescriptor profileFd, Bundle options) {
        throw new UnsupportedOperationException();
    }

    private final void handleAppDiedLocked$Pr(ProcessRecord app, boolean restarting, boolean allowRestart) {
        throw new UnsupportedOperationException();
    }

    private void cleanUpRemovedTaskLocked$Pr(TaskRecord tr, boolean killProcess, boolean removeFromRecents) {
        throw new UnsupportedOperationException();
    }

    private void cleanUpRemovedTaskLocked$Pr(TaskRecord tr, boolean killProcess) {
        throw new UnsupportedOperationException();
    }

    private void cleanUpRemovedTaskLocked$Pr(TaskRecord tr, boolean killProcess, int flags) {
        throw new UnsupportedOperationException();
    }

    private void cleanUpRemovedTaskLocked$Pr(TaskRecord tr, int flags) {
        throw new UnsupportedOperationException();
    }

    public boolean moveActivityTaskToBack$Pr(IBinder token, boolean nonRoot) {
        throw new UnsupportedOperationException();
    }

    public ComponentName startService$Pr(IApplicationThread caller, Intent service, String resolvedType, String callingPackage, int userId) throws TransactionTooLargeException {
        throw new UnsupportedOperationException();
    }

    public ComponentName startService$Pr(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        throw new UnsupportedOperationException();
    }

    public int bindService$Pr(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, String callingPackage, int userId) throws TransactionTooLargeException {
        throw new UnsupportedOperationException();
    }

    public int bindService$Pr(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, int userId) {
        throw new UnsupportedOperationException();
    }

    public final int broadcastIntent$Pr(IApplicationThread caller, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle bOptions, boolean serialized, boolean sticky, int userId) {
        throw new UnsupportedOperationException();
    }

    public final int broadcastIntent$Pr(IApplicationThread caller, Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle map, String requiredPermission, int appOp, boolean serialized, boolean sticky, int userId) {
        throw new UnsupportedOperationException();
    }
}
