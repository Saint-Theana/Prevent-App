package android.app;

import android.content.res.Configuration;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.List;

/**
  * @hide
  */
public interface IApplicationThread {

    /* for api-10 only */
    void scheduleRelaunchActivity(IBinder token, List pendingResults, List pendingNewIntents, int configChanges, boolean notResumed, Configuration config) throws RemoteException;

}
