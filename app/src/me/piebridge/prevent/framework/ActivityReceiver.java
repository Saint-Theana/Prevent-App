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
   private final Map<String, Long> leavingPackages = new ConcurrentHashMap<String, Long>();
    private final Set<String> checkLeavingNext = new TreeSet<String>();
    private final ScheduledThreadPoolExecutor singleExecutor = new ScheduledThreadPoolExecutor(0x2);
    protected Context mContext;
    protected Map<String, Boolean> mPreventPackages;
    protected String homeActivityPackage;
    protected String vpnEstablishedActivity = null;
    private boolean screen = false;
    private ScheduledFuture<?> leavingFuture;
    protected Map<String, List<String>> mActivityRecord=new ConcurrentHashMap<String, List<String>>();


    public ActivityReceiver(Context context, Map<String, Boolean> preventPackages) {
        mContext = context;
        mPreventPackages = preventPackages;
    }

    protected int countCounter(String packageName) {
        if(mActivityRecord.containsKey(packageName)){
            List activities = mActivityRecord.get(packageName);
             return activities.size();
        }
        return -1;
    }



    public void onLaunchActivity(Object activityRecord) {
        String packageName = ActivityRecordUtils.getPackageName(activityRecord);
        if (packageName == null) {
            return;
        }
        SystemHook.cancelCheck(packageName);
        if (mPreventPackages.containsKey(packageName)) {
            mPreventPackages.put(packageName, false);
        }

        if(mActivityRecord.containsKey(packageName)){
            String activityName = ActivityRecordUtils.getActivityName(activityRecord);
            if(activityName!=null){
                List<String> activities = mActivityRecord.get(packageName);
                if(activities.contains(activityName)){

                }else{
                    activities.add(activityName);
                }
                LogUtils.logActivity(activityName+"activity already started", packageName, activities.size());
            }
        }else{
            String activityName = ActivityRecordUtils.getActivityName(activityRecord);
            if(activityName!=null){
                List<String> activities = new ArrayList<String>();
                activities.add(activityName);
                mActivityRecord.put(packageName,activities);
                LogUtils.logActivity("start activity", packageName, 1);
            }
        }
        removeLeaving(packageName);
    }

    public boolean onDestroyActivity(Object activityRecord) {
        String packageName = ActivityRecordUtils.getPackageName(activityRecord);
        if (packageName == null) {
            PreventLog.e("package " + packageName + " destroyed ");
            return false;
        }
        if(mActivityRecord.containsKey(packageName)){
            String activityName = ActivityRecordUtils.getActivityName(activityRecord);
            if(activityName!=null){
                List activities = mActivityRecord.get(packageName);
                if(activities.contains(activityName)){
                    activities.remove(activityName);
                    LogUtils.logActivity("destroy activity", packageName, activities.size());
                }
                if (activities.size() > 0) {
                    return false;
                }
            }
        }
        if (mPreventPackages.containsKey(packageName)) {
            mPreventPackages.put(packageName, true);
            LogUtils.logForceStop("destroy activity", packageName, "if needed in " + SystemHook.TIME_DESTROY + "s");
            SystemHook.checkForceStop(packageName, SystemHook.TIME_DESTROY);
            //SystemHook.checkRunningServices(packageName, SystemHook.TIME_DESTROY);
        }
        /*else {
            SystemHook.checkRunningServices(null, SystemHook.TIME_DESTROY);
        }

         */
        //SystemHook.killNoFather();
        return true;
    }

    public void onDestroyActivity(String reason, String packageName) {
        if (mPreventPackages.containsKey(packageName)) {
            mPreventPackages.put(packageName, true);
            LogUtils.logForceStop(reason, packageName, "destroy in " + SystemHook.TIME_SUICIDE + "s");
            SystemHook.checkForceStop(packageName, SystemHook.TIME_SUICIDE);
        }
        /*else {
            SystemHook.checkRunningServices(null, SystemHook.TIME_SUICIDE < SystemHook.TIME_DESTROY ? SystemHook.TIME_DESTROY : SystemHook.TIME_SUICIDE);
        }

         */
        //SystemHook.killNoFather();
    }

    public void onResumeActivity(Object activityRecord) {
        String packageName = ActivityRecordUtils.getPackageName(activityRecord);
        if (packageName == null) {
            return;
        }
        SystemHook.cancelCheck(packageName);
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
        if (mPreventPackages.containsKey(packageName)) {
            mPreventPackages.put(packageName, true);
            SystemHook.checkForceStop(packageName, SystemHook.TIME_DESTROY);
            //SystemHook.checkRunningServices(packageName, SystemHook.TIME_IMMEDIATE < SystemHook.TIME_DESTROY ? SystemHook.TIME_DESTROY : SystemHook.TIME_IMMEDIATE);
        }
    }

    private boolean shouldStop(String packageName, int pid) {
        if(mActivityRecord.containsKey(packageName)){
            List activities = mActivityRecord.get(packageName);
            return activities.isEmpty();
        }
        return true;
    }

    protected void removePackageCounters(String packageName) {
        mActivityRecord.remove(packageName);
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
            int count = countCounter(packageName);
            if (count == 0) {
                SystemHook.checkForceStop(packageName, SystemHook.TIME_DESTROY);
            }
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
            int count = countCounter(packageName);
            if (count == 0) {
                SystemHook.checkForceStop(packageName, SystemHook.TIME_DESTROY);
            }
        }
    }

    protected void onActivityEstablishVpnConnection(String packageName) {
        vpnEstablishedActivity = packageName;
    }

    protected void onVpnConnectionDisconnected() {
        if (vpnEstablishedActivity!=null) {
            int count = countCounter(vpnEstablishedActivity);
            if (count == 0) {
                SystemHook.checkForceStop(vpnEstablishedActivity, SystemHook.TIME_DESTROY);
            }
            vpnEstablishedActivity = null;
        }
    }

    protected void onStartHomeActivity(String packageName) {
        homeActivityPackage = packageName;
    }
}
