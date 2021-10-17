package me.piebridge.prevent.framework;

import android.app.INotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import me.piebridge.prevent.common.Configuration;
import me.piebridge.prevent.common.TimeUtils;
import me.piebridge.prevent.framework.util.ActivityRecordUtils;
import me.piebridge.prevent.framework.util.HideApiUtils;
import me.piebridge.prevent.framework.util.LogUtils;
import me.piebridge.prevent.framework.util.ProcessRecordUtils;

abstract class ActivityReceiver extends BroadcastReceiver {

    protected final HashMap<String, String> audioFocusedActivity = new HashMap<String, String>();
    private final Map<String, Integer> packageUids = new HashMap<String, Integer>();
    private final Map<String, Set<String>> abnormalProcesses = new ConcurrentHashMap<String, Set<String>>();
    private final Map<String, Map<Integer, AtomicInteger>> packageCounters = new ConcurrentHashMap<String, Map<Integer, AtomicInteger>>();
    private final Map<String, Long> leavingPackages = new ConcurrentHashMap<String, Long>();
    private final Set<String> checkLeavingNext = new TreeSet<String>();
    private final ScheduledThreadPoolExecutor singleExecutor = new ScheduledThreadPoolExecutor(0x2);
    protected Context mContext;
    protected Map<String, Boolean> mPreventPackages;
    protected String homeActivityPackage;
    protected String vpnEstablishedActivity = null;
    private boolean screen = false;
    private ScheduledFuture<?> leavingFuture;

    public ActivityReceiver(Context context, Map<String, Boolean> preventPackages) {
        mContext = context;
        mPreventPackages = preventPackages;
    }

    protected int countCounter(String packageName) {
        return countCounter(-1, packageName);
    }

    private int countCounter(int currentPid, String packageName) {
        int count = 0;
        Map<Integer, AtomicInteger> values = packageCounters.get(packageName);
        if (values == null) {
            return count;
        }
        Iterator<Map.Entry<Integer, AtomicInteger>> iterator = values.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, AtomicInteger> entry = iterator.next();
            int pid = entry.getKey();
            if (pid == currentPid || checkPid(pid, packageName)) {
                count += entry.getValue().get();
            } else {
                LogUtils.logIgnore(entry.getKey(), packageName);
                iterator.remove();
            }
        }
        return count;
    }

    private boolean checkPid(int pid, String packageName) {
        Integer uid = packageUids.get(packageName);
        if (uid == null) {
            return false;
        }
        try {
            if (HideApiUtils.getUidForPid(pid) != uid) {
                return false;
            }
        } catch (Throwable t) { // NOSONAR
            PreventLog.e("cannot get uid for " + pid, t);
        }
        String processName = getProcessName(uid, pid, packageName);
        if (isNormalProcessName(processName, packageName)) {
            return true;
        }
        PreventLog.v("pid: " + pid + ", package: " + packageName + ", process: " + processName);
        Set<String> abnormalPackages = abnormalProcesses.get(processName);
        return abnormalPackages != null && abnormalPackages.contains(packageName);
    }

    private String getProcessName(int uid, int pid, String packageName) {
        String[] packages = mContext.getPackageManager().getPackagesForUid(uid);
        if (packages != null && packages.length == 1) {
            return packageName;
        } else {
            return SystemHook.getProcessName(pid);
        }
    }

    private boolean isNormalProcessName(String processName, String packageName) {
        return (processName != null) && (processName.equals(packageName)
                || processName.startsWith(packageName + ":")
                || "<pre-initialized>".equals(processName));
    }

    private void setAbnormalProcessIfNeeded(String processName, String packageName) {
        if (!isNormalProcessName(processName, packageName)) {
            Set<String> abnormalProcess = abnormalProcesses.get(processName);
            if (abnormalProcess == null) {
                abnormalProcess = new HashSet<String>();
                abnormalProcesses.put(processName, abnormalProcess);
            }
            if (abnormalProcess.add(packageName)) {
                PreventLog.d("package " + packageName + " has abnormal process: " + processName);
            }
        }
    }

    public void onLaunchActivity(Object activityRecord) {
        String packageName = ActivityRecordUtils.getPackageName(activityRecord);
        if (packageName == null) {
            return;
        }
        SystemHook.cancelCheck(packageName);
        SystemHook.updateRunningGapps(packageName, true);
        if (mPreventPackages.containsKey(packageName)) {
            mPreventPackages.put(packageName, false);
        }
        int pid = ActivityRecordUtils.getPid(activityRecord);
        int uid = ActivityRecordUtils.getUid(activityRecord);
        String processName = ActivityRecordUtils.getInfo(activityRecord).processName;
        setAbnormalProcessIfNeeded(processName, packageName);
        if (uid > 0) {
            packageUids.put(packageName, uid);
        }
        Map<Integer, AtomicInteger> packageCounter = packageCounters.get(packageName);
        if (packageCounter == null) {
            packageCounter = new LinkedHashMap<Integer, AtomicInteger>();
            packageCounters.put(packageName, packageCounter);
        }
        AtomicInteger pidCounter = packageCounter.get(pid);
        if (pidCounter == null) {
            pidCounter = new AtomicInteger();
            packageCounter.put(pid, pidCounter);
        }
        pidCounter.incrementAndGet();
        int count = countCounter(pid, packageName);
        if (count == 1) {
            SystemHook.checkSync(packageName);
        }
        LogUtils.logActivity("start activity", packageName, count);
        removeLeaving(packageName);
    }

    public boolean onDestroyActivity(Object activityRecord) {
        String packageName = ActivityRecordUtils.getPackageName(activityRecord);
        if (packageName == null) {
            PreventLog.e("package " + packageName + " destroyed ");
            return false;
        }
        Map<Integer, AtomicInteger> packageCounter = packageCounters.get(packageName);
        if (packageCounter != null) {
            int pid = ActivityRecordUtils.getPid(activityRecord);
            AtomicInteger pidCounter = packageCounter.get(pid);
            if (pidCounter != null) {
                pidCounter.decrementAndGet();
            }
        }
        int count = countCounter(packageName);
        LogUtils.logActivity("destroy activity", packageName, count);
        if (count > 0) {
            return false;
        }
        SystemHook.updateRunningGapps(packageName, false);
        if (mPreventPackages.containsKey(packageName)) {
            mPreventPackages.put(packageName, true);
            LogUtils.logForceStop("destroy activity", packageName, "if needed in " + SystemHook.TIME_DESTROY + "s");
            SystemHook.checkForceStop(packageName, SystemHook.TIME_DESTROY);

            //SystemHook.checkRunningServices(packageName, SystemHook.TIME_DESTROY);
        } else {
            SystemHook.checkRunningServices(null, SystemHook.TIME_DESTROY);
        }
        SystemHook.killNoFather();
        return true;
    }

    public void onDestroyActivity(String reason, String packageName) {
        SystemHook.updateRunningGapps(packageName, false);
        if (mPreventPackages.containsKey(packageName)) {
            mPreventPackages.put(packageName, true);
            LogUtils.logForceStop(reason, packageName, "destroy in " + SystemHook.TIME_SUICIDE + "s");
            SystemHook.checkRunningServices(packageName, SystemHook.TIME_SUICIDE);
        } else {
            SystemHook.checkRunningServices(null, SystemHook.TIME_SUICIDE < SystemHook.TIME_DESTROY ? SystemHook.TIME_DESTROY : SystemHook.TIME_SUICIDE);
        }
        SystemHook.killNoFather();
    }

    public void onResumeActivity(Object activityRecord) {
        String packageName = ActivityRecordUtils.getPackageName(activityRecord);
        if (packageName == null) {
            return;
        }
        SystemHook.cancelCheck(packageName);
        SystemHook.updateRunningGapps(packageName, true);
        if (Boolean.TRUE.equals(mPreventPackages.get(packageName))) {
            mPreventPackages.put(packageName, false);
        }
        int count = countCounter(packageName);
        LogUtils.logActivity("resume activity", packageName, count);
        removeLeaving(packageName);
    }

    public void onUserLeavingActivity(Object activityRecord) {
        String packageName = ActivityRecordUtils.getPackageName(activityRecord);
        if (packageName == null) {
            return;
        }
        int count = countCounter(packageName);
        leavingPackages.put(packageName, TimeUtils.now());
        LogUtils.logActivity("user leaving activity", packageName, count);
    }

    private void removeLeaving(String packageName) {
        leavingPackages.remove(packageName);
    }

    protected long fixLeaving(String packageName) {
        PreventLog.d(packageName + " is not prevented and has no leaving time, fix it");
        long now = TimeUtils.now();
        leavingPackages.put(packageName, now);
        return now;
    }

    protected void onPackageRemoved(String packageName) {
        removeLeaving(packageName);
    }

    private void cancelCheckingIfNeeded() {
        if (leavingFuture != null && leavingFuture.getDelay(TimeUnit.SECONDS) > 0) {
            leavingFuture.cancel(false);
        }
    }

    protected void onScreenOn() {
        PreventLog.d("screen on");
        screen = true;
        cancelCheckingIfNeeded();
    }

    protected void onScreenOff() {
        PreventLog.d("screen off");
        screen = false;
        cancelCheckingIfNeeded();
        checkLeavingNext.clear();
        checkLeavingPackages();
    }

    public void cancelCheckLeaving(String packageName) {
        checkLeavingNext.remove(packageName);
    }

    protected Long getLastRunning(String packageName) {
        return leavingPackages.get(packageName);
    }

    private void checkLeavingPackages() {
        long forceStopTimeout = Configuration.getDefault().getForceStopTimeout();
        if (forceStopTimeout <= 0) {
            return;
        }
        PreventLog.d("checking leaving packages");
        long now = TimeUtils.now();
        Iterator<Map.Entry<String, Boolean>> iterator = mPreventPackages.entrySet().iterator();
        Set<String> stopPackages = new HashSet<String>();
        boolean needCheckMore = false;
        while (iterator.hasNext()) {
            Map.Entry<String, Boolean> entry = iterator.next();
            String packageName = entry.getKey();
            Boolean prevent = entry.getValue();
            if (!Boolean.FALSE.equals(prevent) || packageName.equals(SystemHook.getCurrentPackageName())) {
                continue;
            }
            if (audioFocusedActivity.containsValue(packageName)) {
                PreventLog.i("leaving package " + packageName + " audio focused");
                continue;
            }
            if (vpnEstablishedActivity != null && vpnEstablishedActivity.equals(packageName)) {
                PreventLog.i("leaving package " + packageName + " vpn established");
                continue;
            }
            Long lastRunning = getLastRunning(packageName);
            if (lastRunning == null) {
                lastRunning = fixLeaving(packageName);
            }
            long elapsed = now - lastRunning;
            if (elapsed >= forceStopTimeout) {
                PreventLog.i("leaving package " + packageName + " for " + elapsed + " seconds");
                stopPackages.add(packageName);
            } else {
                needCheckMore = true;
            }
        }
        forceStopPackages(stopPackages);
        if (needCheckMore) {
            checkLeavingPackagesIfNeeded();
        }
    }

   /* private int getPriority(INotificationManager sINM, String packageName, int uid) throws RemoteException {
            IBinder mRemote = (IBinder) ReflectUtils.get(sINM,"mRemote");
            Parcel _data = Parcel.obtain();
            Parcel _reply = Parcel.obtain();
            try {
                _data.writeInterfaceToken("android.app.INotificationManager");
                _data.writeString(packageName);
                _data.writeInt(uid);
                mRemote.transact(12, _data, _reply, 0);
                _reply.readException();
                return _reply.readInt();
            }catch (Exception e){
                e.printStackTrace();
                return 0;
            }finally {
                _reply.recycle();
                _data.recycle();
            }
    }*/

    private void forceStopPackages(Set<String> stopPackages) {
        for (String packageName : stopPackages) {
            if (screen) {
                break;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !hasHighPriority(packageName)) {
                if (SystemHook.forceStopPackage(packageName, false)) {
                    removeLeaving(packageName);
                }
                checkLeavingNext.remove(packageName);
            }
        }
    }

    protected boolean hasHighPriority(String packageName) {
        INotificationManager sINM = INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        try {
            ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(packageName, 0);
            //int priority = getPriority(sINM, packageName, info.uid);
            PreventLog.d(packageName + "  " + info.uid + "  Get PackageImportance");
            int pl = sINM.getPackageImportance(packageName);
            PreventLog.d(packageName + " PackageImportance = " + pl);
            /*
            if (sINM.areNotificationsEnabledForPackage(packageName,info.uid)) {
                PreventLog.d(packageName + " Notifications Enabled, cannot stop");
                return true;
            }
             PreventLog.v("package " + packageName + ", Notifications Enabled" );
             */
        } catch (RemoteException e) {
            PreventLog.d("cannot get package priority for " + packageName, e);
        } catch (PackageManager.NameNotFoundException e) {
            PreventLog.d("cannot find package " + packageName, e);
        } catch (Exception e) {
            PreventLog.d("cannot get package priority for " + packageName, e);
        }
        return false;
    }

    private void checkLeavingPackagesIfNeeded() {
        if (!screen) {
            leavingFuture = singleExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    if (!screen) {
                        checkLeavingPackages();
                    }
                }
            }, SystemHook.TIME_CHECK_USER_LEAVING, TimeUnit.SECONDS);
        }
    }

    public void onAppDied(Object processRecord) {
        String packageName = ProcessRecordUtils.getInfo(processRecord).packageName;
        if (leavingPackages.containsKey(packageName)) {
            LogUtils.logActivity("app died when user leaving", packageName, -1);
            return;
        }
        int count = countCounter(packageName);
        LogUtils.logActivity("app died", packageName, count);
        int pid = ProcessRecordUtils.getPid(processRecord);
        if (!shouldStop(packageName, pid)) {
            return;
        }
        SystemHook.updateRunningGapps(packageName, false);
        if (mPreventPackages.containsKey(packageName)) {
            mPreventPackages.put(packageName, true);
            SystemHook.checkRunningServices(packageName, SystemHook.TIME_IMMEDIATE < SystemHook.TIME_DESTROY ? SystemHook.TIME_DESTROY : SystemHook.TIME_IMMEDIATE);
        }
    }

    private boolean shouldStop(String packageName, int pid) {
        countCounter(packageName);
        Map<Integer, AtomicInteger> values = packageCounters.get(packageName);
        if (values == null) {
            return true;
        }
        Set<Integer> pids = new HashSet<Integer>(values.keySet());
        pids.remove(pid);
        return pids.isEmpty();
    }

    protected void removePackageCounters(String packageName) {
        packageCounters.remove(packageName);
    }

    protected void onActivityRequestAudioFocus(int uid, int pid, String clientId, String packageName) {
        audioFocusedActivity.put(clientId, packageName);
    }

    protected void onActivityAbandonAudioFocus(int uid, int pid, String clientId) {
        String packageName = audioFocusedActivity.get(clientId);
        List<String> keys = new ArrayList<String>();
        if (packageName != null) {
            for (String key : audioFocusedActivity.keySet()) {
                if (audioFocusedActivity.get(key).equals(packageName)) {
                    keys.add(key);
                }
            }
            for (String key : keys) {
                audioFocusedActivity.remove(key);
            }
          /*  int count = countCounter(packageName);
            if (count == 0) {
                SystemHook.checkForceStop(packageName, SystemHook.TIME_DESTROY);
            }
            */
        }
    }

    protected void onActivityLostAudioFocusOnDeath(String clientId) {
        String packageName = audioFocusedActivity.get(clientId);
        List<String> keys = new ArrayList<String>();
        if (packageName != null) {
            for (String key : audioFocusedActivity.keySet()) {
                if (audioFocusedActivity.get(key).equals(packageName)) {
                    keys.add(key);
                }
            }
            for (String key : keys) {
                audioFocusedActivity.remove(key);
            }
            /*
            int count = countCounter(packageName);
            if (count == 0) {
                SystemHook.checkForceStop(packageName, SystemHook.TIME_DESTROY);
            }
             */
        }
    }

    protected void onActivityEstablishVpnConnection(String packageName) {
        vpnEstablishedActivity = packageName;
    }

    protected void onVpnConnectionDisconnected() {
        if (vpnEstablishedActivity!=null) {
            /*
            int count = countCounter(vpnEstablishedActivity);
            if (count == 0) {
                SystemHook.checkForceStop(vpnEstablishedActivity, SystemHook.TIME_DESTROY);
            }
             */
            vpnEstablishedActivity = null;
        }
    }

    protected void onStartHomeActivity(String packageName) {
        homeActivityPackage = packageName;
    }
}
