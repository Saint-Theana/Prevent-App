package me.piebridge.prevent.framework;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.usage.IUsageStatsManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;

import com.android.internal.app.IAppOpsService;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.piebridge.prevent.BuildConfig;
import me.piebridge.prevent.common.Configuration;
import me.piebridge.prevent.common.GmsUtils;
import me.piebridge.prevent.common.PackageUtils;
import me.piebridge.prevent.common.PreventIntent;
import me.piebridge.prevent.framework.util.AccountUtils;
import me.piebridge.prevent.framework.util.ActivityManagerServiceUtils;
import me.piebridge.prevent.framework.util.ActivityRecordUtils;
import me.piebridge.prevent.framework.util.HideApiUtils;
import me.piebridge.prevent.framework.util.HookUtils;
import me.piebridge.prevent.framework.util.LogUtils;
import me.piebridge.prevent.framework.util.NotificationManagerServiceUtils;
import me.piebridge.prevent.framework.util.PreventListUtils;

public final class SystemHook {

    public static final int TIME_SUICIDE = 6;
    public static final int TIME_DESTROY = 6;
    public static final int TIME_IMMEDIATE = 1;
    public static final int TIME_CHECK_SERVICE = 30;
    public static final int TIME_KILL = 1;
    public static final int TIME_CHECK_GMS = 30;
    public static final int TIME_CHECK_DISALLOW = 5;
    public static final int TIME_CHECK_USER_LEAVING = 60;
    private static final Object CHECKING_LOCK = new Object();
    private static final Object REGISTER_LOCK = new Object();
    private static final Object RESTORE_LOCK = new Object();
    private static final Set<String> checkingPackageNames = new TreeSet<String>();
    private static final Set<String> checkingWhiteList = new TreeSet<String>();
    private static final Set<String> runningGapps = new TreeSet<String>();
    private static final ScheduledThreadPoolExecutor singleExecutor = new ScheduledThreadPoolExecutor(0x2);
    private static final ScheduledThreadPoolExecutor checkingExecutor = new ScheduledThreadPoolExecutor(0x2);
    private static final ScheduledThreadPoolExecutor forceStopExecutor = new ScheduledThreadPoolExecutor(0x1);
    private static final ScheduledThreadPoolExecutor moveBackExecutor = new ScheduledThreadPoolExecutor(0x2);
    private static final Map<String, ScheduledFuture<?>> serviceFutures = new HashMap<String, ScheduledFuture<?>>();
    private static final ScheduledThreadPoolExecutor restoreExecutor = new ScheduledThreadPoolExecutor(0x2);
    private static final Map<String, ScheduledFuture<?>> restoreFutures = new HashMap<String, ScheduledFuture<?>>();
    private static final Map<String, Boolean> syncPackages = new HashMap<String, Boolean>();
    private static final HashMap<String, String> audioFocusedActivity = new HashMap<String, String>();
    private static String vpnEstablishedActivity=null;
   // private static final List<String> onCleanUpRemovedTaskPandingVpnEstablishedActivity=new ArrayList<String>();
  //  private static final List<String> onDestroyActivityTaskPandingVpnEstablishedActivity=new ArrayList<String>();
   // private static final HashMap<String, String> onCleanUpRemovedTaskPandingAudioFocusedActivity=new HashMap<String, String>();
    //private static final HashMap<String, Object> onDestroyActivityPandingAudioFocusedActivity=new HashMap<String, Object>();
    private static final Object lock = new Object();
    private static Context mContext;
    private static boolean activated;
    private static ClassLoader mClassLoader;
    private static Map<String, Boolean> mPreventPackages;
    private static ScheduledFuture<?> checkingFuture;
    private static ScheduledFuture<?> killingFuture;
    private static SystemReceiver systemReceiver;
    private static String currentPackageName;
    private static Set<String> currentPackageNames;
    private static volatile boolean expired = true;
    private static int version;
    private static String method;
    private static boolean supported = true;

    private SystemHook() {

    }

    public static ClassLoader getClassLoader() {
        return mClassLoader;
    }

    public static void setClassLoader(ClassLoader classLoader) {
        mClassLoader = classLoader;
    }

    public static boolean registerReceiver() {
        HandlerThread thread = new HandlerThread("PreventService");
        thread.start();
        Handler handler = new Handler(thread.getLooper());

        systemReceiver = new SystemReceiver(mContext, mPreventPackages);

        IntentFilter manager = new IntentFilter();
        for (String action : SystemReceiver.MANAGER_ACTIONS) {
            manager.addAction(action);
        }
        manager.addDataScheme(PreventIntent.SCHEME);
        mContext.registerReceiver(systemReceiver, manager, PreventIntent.PERMISSION_MANAGER, handler);

        IntentFilter filter = new IntentFilter();
        for (String action : SystemReceiver.PACKAGE_ACTIONS) {
            filter.addAction(action);
        }
        filter.addDataScheme("package");
        mContext.registerReceiver(systemReceiver, filter, null, handler);

        IntentFilter noSchemeFilter = new IntentFilter();
        for (String action : SystemReceiver.NON_SCHEME_ACTIONS) {
            noSchemeFilter.addAction(action);
        }
        noSchemeFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        mContext.registerReceiver(systemReceiver, noSchemeFilter, null, handler);

        PreventLog.i("prevent running " + BuildConfig.VERSION_NAME + " activated");
        activated = true;
        return true;
    }

    public static void retrievePreventsIfNeeded(final Context context) {
        if (mContext == null) {
            synchronized (REGISTER_LOCK) {
                if (mContext == null) {
                    mContext = context;
                    mPreventPackages = new ConcurrentHashMap<String, Boolean>();
                    loadPreventList(mContext, mPreventPackages);
                    ActivityManagerServiceHook.setContext(mContext, mPreventPackages);
                    IntentFilterHook.setContext(mContext, mPreventPackages);
                    registerReceiver();
                }
            }
        }
    }

    private static void loadPreventList(Context context, Map<String, Boolean> preventPackages) {
        if (PreventListUtils.getInstance().canLoad(context)) {
            Set<String> prevents = PreventListUtils.getInstance().load(context);
            PreventLog.d("prevent list size: " + prevents.size());
            for (String packageName : prevents) {
                preventPackages.put(packageName, true);
            }
        }
    }

    public static boolean isSystemHook() {
        return Process.myUid() == Process.SYSTEM_UID;
    }

    static String getProcessName(int pid) {
        File file = new File(new File("/proc", String.valueOf(pid)), "cmdline");
        return getContent(file);
    }

    private static String getContent(File file) {
        if (!file.isFile() || !file.canRead()) {
            return null;
        }

        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int length;
            byte[] buffer = new byte[0x1000];
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            is.close();
            return os.toString().trim();
        } catch (IOException e) {
            PreventLog.e("cannot read file " + file, e);
            return null;
        }
    }

    public static String getPackageNameByActivityRecord(Object obj) {
        try {
            Class class1 = Class.forName("com.android.server.wm.ActivityRecord");
            Object obj2 = class1.cast(obj);
            Field field = class1.getDeclaredField("packageName");
            field.setAccessible(true);
            return (String) field.get(obj2);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void checkRunningServices(final String packageName, final boolean forcestop) {
        if (packageName == null) {
            return;
        }
        ScheduledFuture<?> serviceFuture;
        synchronized (CHECKING_LOCK) {
            serviceFuture = serviceFutures.get(packageName);
            if (serviceFuture != null && serviceFuture.getDelay(TimeUnit.SECONDS) > 0) {
                GmsUtils.decreaseGmsCount(mContext, packageName);
                serviceFuture.cancel(false);
            }
            if (!GmsUtils.isGms(packageName)) {
                checkingWhiteList.add(packageName);
            }
        }
        GmsUtils.increaseGmsCount(mContext, packageName);
        serviceFuture = checkingExecutor.schedule(new CheckingRunningService(mContext, mPreventPackages) {
            @Override
            protected Collection<String> preparePackageNames() {
                return Collections.singletonList(packageName);
            }

            @Override
            protected Collection<String> prepareWhiteList() {
                return prepareServiceWhiteList(packageName, forcestop);
            }
        }, GmsUtils.isGms(packageName) ? TIME_CHECK_GMS : TIME_CHECK_SERVICE, TimeUnit.SECONDS);
        synchronized (CHECKING_LOCK) {
            serviceFutures.put(packageName, serviceFuture);
        }
    }

    private static Collection<String> prepareServiceWhiteList(String packageName, boolean forcestop) {
        GmsUtils.decreaseGmsCount(mContext, packageName);
        if (!GmsUtils.isGms(packageName)) {
            synchronized (CHECKING_LOCK) {
                checkingWhiteList.remove(packageName);
            }
        }
        if (canStopGms()) {
            if (forcestop) {
                forceStopPackageIfNeeded(packageName);
            }
            return Collections.emptyList();
        } else {
            return GmsUtils.getGmsPackages();
        }
    }

    public static void cancelCheck(String packageName) {
        if (packageName != null) {
            synchronized (CHECKING_LOCK) {
                checkingPackageNames.remove(packageName);
            }
        }
    }

    public static boolean checkForceStop(final String packageName, int seconds) {
        if (packageName != null) {
            synchronized (CHECKING_LOCK) {
                checkingPackageNames.add(packageName);
            }
        }
        if (checkingFuture != null && checkingFuture.getDelay(TimeUnit.SECONDS) > 0) {
            return false;
        }
        checkingFuture = singleExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                Set<String> packageNames = new TreeSet<String>();
                synchronized (CHECKING_LOCK) {
                    packageNames.addAll(checkingPackageNames);
                    checkingPackageNames.clear();
                    for(String name:packageNames){
                        SystemHook.forceStopPackage(name,false);
                    }
                }
            }
        }, seconds, TimeUnit.SECONDS);
        return true;
    }

    public static boolean checkRunningServices(final String packageName, int seconds) {
        if (packageName != null) {
            synchronized (CHECKING_LOCK) {
                checkingPackageNames.add(packageName);
            }
        }
        if (checkingFuture != null && checkingFuture.getDelay(TimeUnit.SECONDS) > 0) {
            return false;
        }
        checkingFuture = singleExecutor.schedule(new CheckingRunningService(mContext, mPreventPackages) {
            @Override
            protected Collection<String> preparePackageNames() {
                return prepareCheckingPackageNames();
            }

            @Override
            protected Collection<String> prepareWhiteList() {
                return prepareCheckingWhiteList();
            }
        }, seconds, TimeUnit.SECONDS);
        return true;
    }

    private static Collection<String> prepareCheckingPackageNames() {
        Set<String> packageNames = new TreeSet<String>();
        synchronized (CHECKING_LOCK) {
            packageNames.addAll(checkingPackageNames);
            checkingPackageNames.clear();
        }
        return packageNames;
    }

    private static Collection<String> prepareCheckingWhiteList() {
        Set<String> whiteList = new TreeSet<String>();
        synchronized (CHECKING_LOCK) {
            whiteList.addAll(checkingWhiteList);
        }
        if (!canStopGms()) {
            whiteList.addAll(GmsUtils.getGmsPackages());
        }
        return whiteList;
    }

    public static void forceStopPackageIfNeeded(final String packageName) {
        if (!Boolean.TRUE.equals(mPreventPackages.get(packageName))) {
            PreventLog.d("package not in PreventPackages: " + packageName);
            return;
        }
        if (PackageUtils.isInputMethod(mContext, packageName)) {
            PreventLog.d("input method cannot be force stoped: " + packageName);
            return;
        }
        forceStopExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!Boolean.TRUE.equals(mPreventPackages.get(packageName))) {
                        PreventLog.d("package not in PreventPackages: " + packageName);
                        return;
                    }
                    boolean a=forceStopPackage(packageName, false);
                    PreventLog.d("forceStopPackage"+packageName);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static boolean inactive(String packageName) {
        IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService(Context.USAGE_STATS_SERVICE));
        Object usageStatsService = HideApiUtils.getThis0(usm);
        if (usageStatsService == null) {
            return false;
        }
        try {
            ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(packageName, 0);
            if (isAppInactive(usm, packageName, info.uid)) {
                PreventLog.d(packageName + " already inactive");
            } else {
                if (!setAppInactive(usageStatsService, packageName)) {
                    return false;
                }
                PreventLog.d("set " + packageName + " to inactive, current inactive: " + isAppInactive(usm, packageName, info.uid));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean isAppInactive(String packageName) {
        try {
            IUsageStatsManager usm = IUsageStatsManager.Stub.asInterface(ServiceManager.getService(Context.USAGE_STATS_SERVICE));
            ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(packageName, 0);
            return isAppInactive(usm, packageName, info.uid);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean setAppInactive(Object usageStatsService, String packageName) {
        try {
            Method method = usageStatsService.getClass().getDeclaredMethod("setAppIdle", String.class, boolean.class, int.class);
            method.setAccessible(true);
            method.invoke(usageStatsService, packageName, true, 0);
            return true;
        } catch (NoSuchMethodException e) {
            PreventLog.d("cannot inactive(no method) " + packageName, e);
            return false;
        } catch (InvocationTargetException e) {
            PreventLog.d("cannot inactive(invoke) " + packageName, e);
            return false;
        } catch (IllegalAccessException e) {
            PreventLog.d("cannot inactive(illegal access) " + packageName, e);
            return false;
        }
    }

    private static boolean isAppInactive(IUsageStatsManager usm, String packageName, int uid) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            try {
                return usm.isAppInactive(packageName, 0);
            } catch (RemoteException e) {
                PreventLog.d("remote exception " + packageName, e);
                return false;
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            try {
                return usm.isAppInactive(packageName, uid, packageName);
            } catch (RemoteException e) {
                PreventLog.d("remote exception " + packageName, e);
                return false;
            }
        }
        PreventLog.d("isAppInactive unknow android version ");
        return false;
    }

    public static boolean killNoFather() {
        if (killingFuture != null && killingFuture.getDelay(TimeUnit.SECONDS) > 0) {
            return false;
        }
        killingFuture = singleExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    doKillNoFather();
                } catch (Throwable t) { // NOSONAR
                    t.printStackTrace();
                    PreventLog.e("cannot killNoFather", t);
                }
            }
        }, TIME_KILL, TimeUnit.SECONDS);
        return true;
    }

    private static void doKillNoFather() {
        PreventLog.d("doKillNoFather");
        File proc = new File("/proc");
        for (File file : proc.listFiles()) {
            if (file.isDirectory() && TextUtils.isDigitsOnly(file.getName())) {
                int pid = Integer.parseInt(file.getName());
                int uid = HideApiUtils.getUidForPid(pid);
                String[] names = mContext.getPackageManager().getPackagesForUid(uid);
                String name=null;
                if (names != null) {
                   name= Arrays.asList(names).toString();
                }
                //PreventLog.d("doKillNoFather uid："+uid+" pid:"+pid+" ppid:"+HideApiUtils.getParentPid(pid)+" name:"+name);
                if (HideApiUtils.getParentPid(pid) == 1 && uid >= PackageUtils.FIRST_APPLICATION_UID) {
                    killIfNeed(uid, pid);
                }
            }
        }
    }

    private static void killIfNeed(int uid, int pid) {
        PreventLog.d("killIfNeed uid："+uid+" pid:"+pid);
        String[] names = mContext.getPackageManager().getPackagesForUid(uid);
        if (names == null || isPrevent(names)) {
            for(String name:names){
                PreventLog.d("killIfNeed name："+name);
            }
            Process.killProcess(pid);
            String name;
            if (names == null) {
                name = "(uid: " + uid + ", process: + " + getProcessName(pid) + ")";
            } else if (names.length == 1) {
                name = names[0];
            } else {
                name = Arrays.asList(names).toString();
            }
            LogUtils.logKill(pid, "without parent", name);
        }
    }

    private static boolean isPrevent(String[] names) {
        for (String name : names) {
            if (!Boolean.TRUE.equals(mPreventPackages.get(name))) {
                return false;
            }
        }
        return true;
    }

    public static void restorePrevent(String packageName) {
        if (systemReceiver != null && 0 == systemReceiver.countCounter(packageName) && Boolean.FALSE.equals(mPreventPackages.get(packageName))) {
            PreventLog.v("restore prevent for " + packageName);
            mPreventPackages.put(packageName, true);
            checkRunningServices(packageName, TIME_DESTROY);
        }
    }

    public static void onLaunchActivity(Object activityRecord) {
        expired = true;
        currentPackageName = ActivityRecordUtils.getPackageName(activityRecord);
        PreventLog.v("launch, current: " + currentPackageName);
        if (systemReceiver != null) {
            systemReceiver.onLaunchActivity(activityRecord);
        }
    }

    public static void onDestroyActivity(Object activityRecord) {
        expired = true;
        if (systemReceiver != null) {
            if (systemReceiver.onDestroyActivity(activityRecord)) {
                String packageName = ActivityRecordUtils.getPackageName(activityRecord);
                PreventLog.v("destroy all activity: " + packageName);
            }
        }
    }

    public static void onResumeActivity(Object activityRecord) {
        expired = true;
        currentPackageName = ActivityRecordUtils.getPackageName(activityRecord);
        PreventLog.v("resume, current: " + currentPackageName);
        if (systemReceiver != null) {
            systemReceiver.onResumeActivity(activityRecord);
        }
    }

    public static void onUserLeavingActivity(Object activityRecord) {
        expired = true;
        String packageName = ActivityRecordUtils.getPackageName(activityRecord);
        PreventLog.v("leaving, current: " + packageName);
        if (systemReceiver != null) {
            systemReceiver.onUserLeavingActivity(activityRecord);
        }
    }

    public static void onStartHomeActivity(String packageName) {
        expired = true;
        if (systemReceiver != null) {
            systemReceiver.onStartHomeActivity(packageName);
            systemReceiver.onDestroyActivity("start home activity", packageName);
        }
    }

    public static void onMoveActivityToBack(final String packageName) {
        expired = true;
        PreventLog.v("move activity to back, package: " + packageName);
        moveBackExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                if (systemReceiver != null) {
                    systemReceiver.onDestroyActivity("move activity to back", packageName);
                }
            }
        }, 0x200, TimeUnit.MILLISECONDS);
    }

    public static void onAppDied(Object processRecord) {
        expired = true;
        if (systemReceiver != null) {
            systemReceiver.onAppDied(processRecord);
        }
    }

    public static boolean hasRunningActivity(String packageName) {
        if (packageName != null && systemReceiver != null && systemReceiver.countCounter(packageName) != 0) {
            return true;
        }

        // for temp allow
        ScheduledFuture<?> restoreFuture = restoreFutures.get(packageName);
        return restoreFuture != null && restoreFuture.getDelay(TimeUnit.SECONDS) > 0;
    }

    public static boolean isFramework(String packageName) {
        return "android".equals(packageName);
    }

    public static String getPackageNameFromUid(int uid) {
        return mContext.getPackageManager().getPackagesForUid(uid)[0];
    }

    public static boolean isSystemPackage(String packageName) {
        if (isFramework(packageName) || GmsUtils.isGms(packageName)) {
            return true;
        }
        if (packageName == null || GmsUtils.isGapps(packageName)) {
            return false;
        }
        try {
            PackageManager pm = mContext.getPackageManager();
            int flags = pm.getApplicationInfo(packageName, 0).flags;
            return PackageUtils.isSystemPackage(flags);
        } catch (PackageManager.NameNotFoundException e) {
            PreventLog.d("cannot find package: " + packageName, e);
            return false;
        }
    }

    public static boolean isActivated() {
        return activated;
    }

    public static String getCurrentPackageName() {
        return currentPackageName;
    }

    public static Set<String> getCurrentPackageNames() {
        if (currentPackageNames == null || expired) {
            synchronized (lock) {
                if (expired) {
                    expired = false;
                    currentPackageNames = ActivityManagerServiceUtils.getCurrentPackages();
                }
            }
        }
        return currentPackageNames;
    }

    public static void checkSync(final String packageName) {
        singleExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Boolean syncable = AccountUtils.isPackageSyncable(mContext, packageName);
                if (syncable != null) {
                    PreventLog.d("sync for " + packageName + ": " + syncable);
                    syncPackages.put(packageName, syncable);
                }
            }
        });
    }

    public static void resetSync(final String packageName) {
        singleExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (Boolean.FALSE.equals(syncPackages.get(packageName)) && Boolean.TRUE.equals(mPreventPackages.get(packageName))
                        && Configuration.getDefault().isLockSyncSettings()) {
                    PreventLog.d("reset sync for " + packageName + " to false");
                    AccountUtils.setSyncable(mContext, packageName, false);
                }
            }
        });
    }

    public static int getVersion() {
        return SystemHook.version;
    }

    public static void setVersion(int version) {
        SystemHook.version = version;
    }

    public static String getMethod() {
        return SystemHook.method;
    }

    public static void setMethod(String method) {
        SystemHook.method = method;
    }

    public static boolean isUseAppStandby() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Configuration.getDefault().isUseAppStandby();
    }

    public static void setNotSupported() {
        try{throw new Exception();}catch (Exception e){e.printStackTrace();}
        if (isSupported()) {

            SystemHook.supported = false;
        }
    }

    public static boolean isSupported() {
        return supported;
    }

    public static void setSupported(boolean supported) {
        try{throw new Exception();}catch (Exception e){e.printStackTrace();}
        SystemHook.supported = supported;
    }

    public static void updateRunningGapps(String packageName, boolean added) {
        if (mContext == null || packageName == null) {
            return;
        }
        if (!added) {
            resetSync(packageName);
        }
        PackageManager pm = mContext.getPackageManager();
        if (GmsUtils.isGapps(packageName) && pm.getLaunchIntentForPackage(packageName) != null) {
            if (added) {
                if (!runningGapps.contains(packageName)) {
                    PreventLog.d("add " + packageName + " to running gapps: " + runningGapps);
                }
                runningGapps.add(packageName);
            } else {
                if (runningGapps.contains(packageName)) {
                    PreventLog.d("remove " + packageName + " from running gapps: " + runningGapps);
                    checkRunningServices(null, SystemHook.TIME_CHECK_SERVICE);
                }
                runningGapps.remove(packageName);
            }
        }
    }

    public static boolean hasRunningGapps() {
        Iterator<String> it = runningGapps.iterator();
        int count = 0;
        while (it.hasNext()) {
            String packageName = it.next();
            int counter = systemReceiver.countCounter(packageName);
            if (counter == 0) {
                it.remove();
            } else if (counter > 1 || !PackageUtils.isLauncher(mContext.getPackageManager(), packageName)) {
                count += 1;
            }
        }
        if (count > 0) {
            PreventLog.d("running gapps: " + runningGapps);
            return true;
        } else {
            return false;
        }
    }

    public static void restoreLater(final String packageName) {
        systemReceiver.cancelCheckLeaving(packageName);
        synchronized (RESTORE_LOCK) {
            ScheduledFuture<?> restoreFuture = restoreFutures.get(packageName);
            if (restoreFuture != null && restoreFuture.getDelay(TimeUnit.SECONDS) > 0) {
                restoreFuture.cancel(false);
            }
            restoreFuture = restoreExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    restorePrevent(packageName);
                }
            }, TIME_CHECK_DISALLOW, TimeUnit.SECONDS);
            restoreFutures.put(packageName, restoreFuture);
        }
    }

    public static boolean forceStopPackage(String packageName, boolean force) {
        if (!Configuration.getDefault().isStopSignatureApps() && ActivityManagerServiceHook.cannotPrevent(packageName)) {
            PreventLog.i("wont force-stop important system package: " + packageName + ", force: " + force);
            return false;
        } else if (Configuration.getDefault().optimizeAudio()&&audioFocusedActivity.containsValue(packageName)) {
            PreventLog.i("Audio Focused Activity will not be removed");
            //pandingAudioFocusedActivity.put(getClientIdByPackageName(packageName),activityRecord);
            return false;
        }else if(Configuration.getDefault().optimizeVpn()&& vpnEstablishedActivity!=null&&vpnEstablishedActivity.equals(packageName)){
            PreventLog.i("Vpn Established Activity will not be removed. package name: "+packageName);
            return false;
        } else if (force) {
            HideApiUtils.forceStopPackage(mContext, packageName);
            return true;
        } else if (isUseAppStandby()) {
            PreventLog.i("wont force-stop standby package: " + packageName + ", force: " + force);
            inactive(packageName);
            setBackground(packageName, AppOpsManager.MODE_IGNORED);
            return false;
        } else if (getCurrentPackageNames().contains(packageName)) {
            PreventLog.i(packageName + " seems not safe to force stop");
            return false;
        } else if (PackageUtils.isInputMethod(mContext, packageName)) {
            PreventLog.i(packageName + " inputmethod cannot be force stoped");
            return false;
        }else {
            NotificationManagerServiceUtils.keepNotification(packageName);
            HideApiUtils.forceStopPackage(mContext, packageName);
            return true;
        }
    }

    public static void setBackground(String packageName, int mode) {
            try {
                IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE));
                int packageUid = AppGlobals.getPackageManager().getPackageUid(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, 0);
                appOpsService.setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, packageUid, packageName, mode);
            } catch (RemoteException e) {
                PreventLog.d("cannot set background for " + packageName, e);
            }
    }

    public static Context getContext() {
        return mContext;
    }

    public static boolean canStopGms() {
        return GmsUtils.canStopGms() && !SystemHook.hasRunningGapps();
    }

    public static void onActivityRequestAudioFocus(int uid, int pid, String clientId, String packageName) {
        expired = true;
        PreventLog.i("Activity Request Audio Focuse. ClientId:" + clientId + " PackageName: " + packageName);
        audioFocusedActivity.put(clientId, packageName);
        if (systemReceiver != null) {
            systemReceiver.onActivityRequestAudioFocus(uid, pid, clientId, packageName);
        }
    }

    public static void onActivityAbandonAudioFocus(int uid, int pid, String clientId) {
        PreventLog.i("Activity Abandon Audio Focuse. ClientId:" + clientId + "PackageName: " + audioFocusedActivity.get(clientId));
        expired = true;
        removeAudioFocusedActivity(clientId);
        if (systemReceiver != null) {
            systemReceiver.onActivityAbandonAudioFocus(uid, pid, clientId);
        }
    }

    private static void removeAudioFocusedActivity(String clientId) {
        expired = true;
        String packageName=audioFocusedActivity.get(clientId);
        List<String> keys=new ArrayList<String>();
        if(packageName!=null){
            for (String key : audioFocusedActivity.keySet()) {
                if (audioFocusedActivity.get(key).equals(packageName)) {
                    keys.add(key);
                }
            }
        }
        for(String key:keys){
            audioFocusedActivity.remove(key);
        }
    }

    public static void onActivityLostAudioFocusOnDeath(String clientId) {
        expired = true;
        PreventLog.i("Activity Lost Audio Focuse. PackageName: " + audioFocusedActivity.get(clientId));
        removeAudioFocusedActivity(clientId);
        if (systemReceiver != null) {
            systemReceiver.onActivityLostAudioFocusOnDeath(clientId);
        }
    }

    public static void onActivityEstablishVpnConnection(String packageName) {
        expired = true;
        PreventLog.i("Activity Establish Vpn Connection. PackageName: " + packageName);
        vpnEstablishedActivity = packageName;
        if (systemReceiver != null) {
            systemReceiver.onActivityEstablishVpnConnection(packageName);
        }
    }

    public static void onVpnConnectionDisconnected() {
        expired = true;
        PreventLog.i("Disconnecting Vpn Connection. PackageName: " + vpnEstablishedActivity);
        vpnEstablishedActivity = null;
        if (systemReceiver != null) {
            systemReceiver.onVpnConnectionDisconnected();
        }
    }
}