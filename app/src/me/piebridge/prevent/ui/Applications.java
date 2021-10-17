package me.piebridge.prevent.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import me.piebridge.prevent.common.PackageUtils;
import me.piebridge.prevent.ui.util.LabelLoader;

/**
 * Created by thom on 16/2/17.
 */
public class Applications extends PreventFragment {

    @Override
    protected Set<String> getPackageNames() {
        Set<String> names = new HashSet<String>();
        PackageManager pm = getActivity().getPackageManager();
        for (PackageInfo pkgInfo : pm.getInstalledPackages(0)) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            if (!PackageUtils.isSystemPackage(appInfo.flags)) {
                names.add(appInfo.packageName);
            }
        }
        return names;
    }



}