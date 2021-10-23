package me.piebridge.prevent.framework.util;

import android.content.Context;
import android.os.Build;
import android.os.ServiceManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.piebridge.prevent.framework.PreventLog;

/**
 * Created by thom on 2016/10/18.
 */
public class ActivityManagerServiceUtils {



    /**
     * ID of stack where fullscreen activities are normally launched into.
     */
    private static final int FULLSCREEN_WORKSPACE_STACK_ID = 1;
    /**
     * ID of stack where freeform/resized activities are normally launched into.
     */
    public static final int FREEFORM_WORKSPACE_STACK_ID = FULLSCREEN_WORKSPACE_STACK_ID + 1;
    /**
     * ID of stack that occupies a dedicated region of the screen.
     */
    public static final int DOCKED_STACK_ID = FREEFORM_WORKSPACE_STACK_ID + 1;
    private ActivityManagerServiceUtils() {

    }

    public static Set<String> getCurrentPackages() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            Object mAm = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            if (mAm == null) {
                return Collections.emptySet();
            }
            Set<String> packageNames = new LinkedHashSet<String>();
            Object focusedStack = getFocusedStack(mAm);
            addPackage(packageNames, focusedStack, "focused", false);
            Object mStackSupervisor = ReflectUtils.get(mAm, "mStackSupervisor");
            Object fullscreenStack = getStack(mStackSupervisor, FULLSCREEN_WORKSPACE_STACK_ID);
            if (fullscreenStack != focusedStack) {
                addPackage(packageNames, fullscreenStack, "fullscreen workspace", true);
            }
            Object dockedStack = getStack(mStackSupervisor, DOCKED_STACK_ID);
            if (dockedStack != focusedStack) {
                addPackage(packageNames, dockedStack, "docked", true);
            }
            PreventLog.d("current packages: " + packageNames);
            return Collections.unmodifiableSet(packageNames);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            Object mAm = ServiceManager.getService("activity_task");
            if (mAm == null) {
                PreventLog.d("activity_task is null");
                return Collections.emptySet();
            }
            Set<String> packageNames = new LinkedHashSet<String>();
            Object focusedStack = getFocusedStack(mAm);
            addPackage(packageNames, focusedStack, "focused", false);
            Object mRootWindowContainer = ReflectUtils.get(mAm, "mRootWindowContainer");
            Object fullscreenStack = getStack(mRootWindowContainer, FULLSCREEN_WORKSPACE_STACK_ID);
            if (fullscreenStack != focusedStack) {
                addPackage(packageNames, fullscreenStack, "fullscreen workspace", true);
            }
            Object dockedStack = getStack(mRootWindowContainer, DOCKED_STACK_ID);
            if (dockedStack != focusedStack) {
                addPackage(packageNames, dockedStack, "docked", true);
            }
            PreventLog.d("current packages: " + packageNames);
            return Collections.unmodifiableSet(packageNames);
        }
        PreventLog.d("getCurrentPackages unknow android version ");
        return null;
    }

    private static Object getStack(Object stackSupervisor, int stackId) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            Object mTopResumedActivity = ReflectUtils.get(stackSupervisor, "mTopResumedActivity");
            Object display = null;
            if (mTopResumedActivity != null) {
                display = ReflectUtils.invoke(mTopResumedActivity, "getDisplay");
            } else {
                Object mRootActivityContainer = ReflectUtils.get(stackSupervisor, "mRootActivityContainer");
                display = ReflectUtils.invoke(mRootActivityContainer, "getDefaultDisplay");
            }
            if (display == null) {
                return null;
            }
            return ReflectUtils.invoke(display, "getStack", new Class<?>[]{int.class}, new Object[]{stackId});
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            Object rootWindowContainer = stackSupervisor;
            Object defaultTaskDisplayArea = ReflectUtils.invoke(rootWindowContainer, "getDefaultTaskDisplayArea");
            return ReflectUtils.invoke(defaultTaskDisplayArea, "getStack", new Class<?>[]{int.class}, new Object[]{stackId});
        }
        PreventLog.d("getStack unknow android version ");
        return null;
    }

    private static Object getFocusedStack(Object mAm) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            Object mStackSupervisor = ReflectUtils.get(mAm, "mStackSupervisor");
            Object mTopResumedActivity = ReflectUtils.get(mStackSupervisor, "mTopResumedActivity");
            Object display = null;
            if (mTopResumedActivity != null) {
                display = ReflectUtils.invoke(mTopResumedActivity, "getDisplay");
            } else {
                Object mRootActivityContainer = ReflectUtils.get(mStackSupervisor, "mRootActivityContainer");
                display = ReflectUtils.invoke(mRootActivityContainer, "getDefaultDisplay");
            }
            if (display == null) {
                return null;
            }
            return ReflectUtils.invoke(display, "getFocusedStack");
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            Object mRootWindowContainer = ReflectUtils.get(mAm, "mRootWindowContainer");
            Object defaultTaskDisplayArea = ReflectUtils.invoke(mRootWindowContainer, "getDefaultTaskDisplayArea");
            return ReflectUtils.invoke(defaultTaskDisplayArea, "getFocusedStack");
        }
        PreventLog.d("getFocusedStack unknow android version ");
        return null;
    }

    private static void addPackage(Collection<String> packageNames, Object stack, String message, boolean checkVisible) {
        if (stack == null) {
            return;
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            List mTaskHistory = (List) ReflectUtils.get(stack, "mTaskHistory");
            if (mTaskHistory != null) {
                for (Object taskRecord : mTaskHistory) {
                    List mActivities = (List) ReflectUtils.get(taskRecord, "mActivities");
                    if (mActivities != null) {
                        for (Object mActivity : mActivities) {
                            String packageName = ActivityRecordUtils.getPackageName(mActivity);
                            PreventLog.d("activityRecord: " + mActivity + ", packageName: " + packageName + ", message: " + message);
                            if (packageName != null) {
                                if(checkVisible){
                                    if(!isVisible(stack,mActivity,message)){
                                        packageNames.add(packageName);
                                    }
                                }else{
                                    packageNames.add(packageName);
                                }
                            }
                        }
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            int childCount = (Integer) ReflectUtils.invoke(stack, "getChildCount");
            for (int i = 0; i < childCount; i += 1) {
                Object task = ReflectUtils.invoke(stack, "getChildAt", new Class<?>[]{int.class}, new Object[]{i});
                List mActivities=null;
                if(ActivityRecordUtils.isActivityRecord(task)){
                    Object app = ReflectUtils.get(task, "app");
                    if(app!=null) {
                        mActivities = (List) ReflectUtils.get(app, "mActivities");
                    }
                }else if(ActivityRecordUtils.isActivityStack(task)){
                    Object mRootProcess = ReflectUtils.get(task, "mRootProcess");
                    if(mRootProcess!=null) {
                        mActivities = (List) ReflectUtils.get(mRootProcess, "mActivities");
                    }
                }
                if (mActivities != null) {
                    for (Object mActivity : mActivities) {
                        String packageName = ActivityRecordUtils.getPackageName(mActivity);
                        PreventLog.d("activityRecord: " + mActivity + ", packageName: " + packageName + ", message: " + message);
                        if (packageName != null) {
                            packageNames.add(packageName);
                        }
                    }
                }
            }
        }else {
            PreventLog.d("addPackage unknow android version ");
        }
    }

    private static Boolean isVisible(Object stack, Object record,String message) {
        Integer visibility = (Integer) ReflectUtils.invoke(stack, "getVisibility",
                new Class[]{record.getClass()}, new Object[]{record});
        PreventLog.d("stack: " + stack + ", visibility: " + visibility + ", message: " + message);
        return visibility != null && visibility != 0;
    }

}
