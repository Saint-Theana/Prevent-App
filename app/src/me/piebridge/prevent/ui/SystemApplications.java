package me.piebridge.prevent.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.Set;

import me.piebridge.prevent.common.PackageUtils;

public class SystemApplications extends PreventFragment {

    @Override
    protected Set<String> getPackageNames() {
        Set<String> names = new HashSet<String>();
        PackageManager pm = getActivity().getPackageManager();
        for (PackageInfo pkgInfo : pm.getInstalledPackages(0)) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            if (PackageUtils.isSystemPackage(appInfo.flags)) {
                names.add(appInfo.packageName);
            }
        }
        return names;
    }



}