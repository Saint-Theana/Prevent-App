package com.android.server.am;

import android.os.IBinder;

public abstract class ActivityStackSupervisor implements IBinder {
    /* access modifiers changed from: package-private */
    public void cleanUpRemovedTaskLocked(TaskRecord taskRecord, boolean z, boolean z2) {
        try {
            cleanUpRemovedTaskLocked$Pr(taskRecord, z, z2);
        } finally {
            if (z) {
                PreventRunningUtils.onCleanUpRemovedTask(taskRecord.getBaseIntent());
            }
        }
    }

    private void cleanUpRemovedTaskLocked$Pr(TaskRecord taskRecord, boolean z, boolean z2) {
        throw new UnsupportedOperationException();
    }
}
