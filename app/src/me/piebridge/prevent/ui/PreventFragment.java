package me.piebridge.prevent.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.text.Collator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.piebridge.prevent.R;
import me.piebridge.prevent.common.PackageUtils;
import me.piebridge.prevent.ui.util.LabelLoader;
import me.piebridge.prevent.ui.util.StatusUtils;

public abstract class PreventFragment extends Fragment {

    private static final int HEADER_ICON_WIDTH = 48;
    private static boolean appNotification;
    ProgressAlertDialog mProgressDialog;
    private Adapter mAdapter;
    private View filter;
    private CheckBox check;
    private EditText search;
    private int headerIconWidth;
    private boolean scrolling;
    private RecyclerView recyclerView;
    private Context mContext;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Thread mUpdateThread;
    private Runnable mUpdateRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = (PreventActivity) getActivity();
        //    appNotification = PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("app_notification", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        //    setNewAdapterIfNeeded(mActivity, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContext = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list, container, false);
        recyclerView = view.findViewById(R.id.fragmentRecyclerView1);
        LinearLayoutManager mng = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mng);
        swipeRefreshLayout = view.findViewById(R.id.fragmentSwipeRefreshLayout1);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mAdapter.clearAllPackage();
                showPrograssDialog();
                startProcess();
            }
        });
        mAdapter = new Adapter(mContext);
        mAdapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
//                Toast.makeText(ExpandableActivity.this, "组头：groupPosition = " + groupPosition,
//                        Toast.LENGTH_LONG).show();
                Adapter expandableAdapter = (Adapter) adapter;
                if (expandableAdapter.isExpand(groupPosition)) {
                    expandableAdapter.collapseGroup(groupPosition);
                    mAdapter.notifyHeaderChanged(groupPosition);
                } else {
                    expandableAdapter.expandGroup(groupPosition);
                    mAdapter.notifyHeaderChanged(groupPosition);
                }
            }
        });
        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                scrolling = newState != 0;
            }
        });

        showPrograssDialog();
        startProcess();
        //setNewAdapterIfNeeded(mActivity, true);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private int getHeaderIconWidth() {
        if (headerIconWidth == 0) {
            headerIconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, HEADER_ICON_WIDTH, getResources().getDisplayMetrics());
        }
        return headerIconWidth;
    }

    private boolean canCreateContextMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
        return mContext != null && menu != null && menuInfo != null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
       /* if (!canCreateContextMenu(menu, menuInfo)) {
            return;
        }
        menu.clear();
        ViewHolder holder = (ViewHolder) ((AdapterContextMenuInfo) menuInfo).targetView.getTag();
        menu.setHeaderTitle(holder.nameView.getText());
        if (holder.icon != null) {
            setHeaderIcon(menu, holder.icon);
        }
        menu.add(Menu.NONE, R.string.app_info, Menu.NONE, R.string.app_info);
        updatePreventMenu(menu, holder.packageName);
        if (getMainIntent(holder.packageName) != null) {
            menu.add(Menu.NONE, R.string.open, Menu.NONE, R.string.open);
        }
        if (holder.canUninstall) {
            menu.add(Menu.NONE, R.string.uninstall, Menu.NONE, R.string.uninstall);
        }
        if (appNotification) {
            menu.add(Menu.NONE, R.string.app_notifications, Menu.NONE, R.string.app_notifications);
        }

        */
    }

    private void setHeaderIcon(ContextMenu menu, Drawable icon) {
        int width = getHeaderIconWidth();
        if (icon.getMinimumWidth() <= width) {
            menu.setHeaderIcon(icon);
        } else if (icon instanceof BitmapDrawable) {
            Bitmap bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) icon).getBitmap(), width, width, false);
            menu.setHeaderIcon(new BitmapDrawable(getResources(), bitmap));
        }
    }

    private boolean startNotification(String packageName) {
        ApplicationInfo info;
        try {
            info = mContext.getPackageManager().getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            UILog.d("cannot find package " + packageName, e);
            return false;
        }
        int uid = info.uid;
        Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("app_package", packageName)
                .putExtra("app_uid", uid);
        try {
            mContext.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            appNotification = false;
            PreferenceManager.getDefaultSharedPreferences(mContext).edit().putBoolean("app_notification", false).apply();
            UILog.d("cannot start notification for " + packageName, e);
            return false;
        }
    }

    private boolean startActivity(int id, String packageName) {
        String action;
        if (id == R.string.app_info) {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
        } else if (id == R.string.uninstall) {
            action = Intent.ACTION_DELETE;
        } else {
            return false;
        }
        mContext.startActivity(new Intent(action, Uri.fromParts("package", packageName, null)));
        return true;
    }

    private boolean startPackage(String packageName) {
        Intent intent = getMainIntent(packageName);
        if (intent != null) {
            mContext.startActivity(intent);
        }
        return true;
    }

    private Intent getMainIntent(String packageName) {
        return mContext.getPackageManager().getLaunchIntentForPackage(packageName);
    }

    public void refresh(boolean force) {
        if (mContext != null) {
            //setNewAdapterIfNeeded(mActivity, force);
            // if (mActivity.getSelection().isEmpty()) {
            //     check.setChecked(false);
            // }
        }
    }

    protected void startProcess() {
        Set<String> packages = getPackageNames();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.setMax(packages.size());
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                PackageManager pm = getActivity().getPackageManager();
                Map<String, Set<Long>> running = ((PreventActivity) getActivity()).getRunningProcesses();
                int i = 1;
                LabelLoader labelLoader = new LabelLoader(getActivity());
                for (String name : packages) {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.setMax(packages.size());
                        upDatePrograssDialog(i);
                    }
                    ApplicationInfo info;
                    try {
                        info = pm.getApplicationInfo(name, 0);
                    } catch (NameNotFoundException e) { // NOSONAR
                        info = null;
                    }
                    if (info == null || !info.enabled) {
                        continue;
                    }
                    String label = labelLoader.loadLabel(info);
                    Drawable icon = null;
                    try {
                        icon = getActivity().getPackageManager().getApplicationIcon(info.packageName);
                    } catch (NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (running.get(name) == null) {
                        boolean preventable = !(PackageUtils.isSystemPackage(info.flags) && PackageUtils.canPrevent(mContext.getPackageManager(), info.packageName));
                        addDeadPackage(new AppInfo(name, icon, label, running.get(name)).setFlags(info.flags));
                    } else {
                        addRunningPackage(new AppInfo(name, icon, label, running.get(name)).setFlags(info.flags));
                    }
                    i += 1;
                }
                progressDone();
                updateAdapter();
            }
        }).start();
    }

    protected void startUpdateProcess() {
        if (mUpdateRunnable == null) {
            mUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    PackageManager pm = getActivity().getPackageManager();
                    Map<String, Set<Long>> running = ((PreventActivity) getActivity()).getRunningProcesses();
                    LabelLoader labelLoader = new LabelLoader(getActivity());
                    LinkedList<AppInfo> infos = mAdapter.getRunningAppInfos();
                    for (int i = 0; i < infos.size(); i += 1) {
                        AppInfo info = infos.get(i);
                        info.running = running.get(info.packageName);
                        if (info.running == null) {
                            mAdapter.removeRunningAppInfo(info);
                            mAdapter.addDeadPackage(info);
                        }
                    }
                    LinkedList<AppInfo> infos1 = mAdapter.getDeadAppInfos();
                    for (int i = 0; i < infos1.size(); i += 1) {
                        AppInfo info = infos1.get(i);
                        info.running = running.get(info.packageName);
                        if (info.running != null) {
                            mAdapter.removeDeadAppInfo(info);
                            mAdapter.addRunningPackage(info);
                        }
                    }
                    progressDone();
                    updateAdapter();
                }
            };
        }
        if (mUpdateThread == null) {
            UILog.d("update thread is not running,start it");
            mUpdateThread = new Thread(mUpdateRunnable);
            mUpdateThread.start();
            return;
        }
        if (mUpdateThread.isAlive()) {
            UILog.d("update thread is running ");
        } else {
            UILog.d("update thread is not running,start it");
            mUpdateThread = new Thread(mUpdateRunnable);
            mUpdateThread.start();
        }
    }

    protected abstract Set<String> getPackageNames();

    protected void showPrograssDialog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.show();
                    return;
                }
                mProgressDialog = new ProgressAlertDialog(mContext);
                mProgressDialog.setTitle(R.string.app_name);
                mProgressDialog.setIcon(R.drawable.ic_launcher);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }
        });
    }

    protected void upDatePrograssDialog(int progress) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.setProgress(progress);
            }
        });

    }


    protected void progressDone() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

    }

    protected void addRunningPackage(AppInfo info) {
        mAdapter.addRunningPackage(info);
    }

    protected void addDeadPackage(AppInfo info) {
        mAdapter.addDeadPackage(info);
    }

    protected void updateAdapter() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });

    }


    public void startTaskIfNeeded() {
        //mAdapter.startTaskIfNeeded();
    }

    public void updateTimeIfNeeded(String packageName) {
        //UILog.i("updateTimeIfNeeded package: "+packageName);
        if (scrolling || mAdapter == null) {
            return;
        }
        if (packageName == null) {
            startUpdateProcess();
            return;
        }
        int index = 0;
        for (AppInfo info : mAdapter.getRunningAppInfos()) {
            if (PackageUtils.equals(packageName, info.packageName)) {
                info.running = ((PreventActivity) getActivity()).getRunningProcesses().get(packageName);
                if (info.running == null) {
                    mAdapter.removeRunningAppInfo(info);
                    mAdapter.addDeadPackage(info);
                    mAdapter.notifyDataSetChanged();
                } else {
                    mAdapter.notifyChildChanged(mAdapter.getGroupIndexByName("running"), index);
                }
            }
            index += 1;
        }
    }

    public void notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    protected int getGroupHeaderName(String s) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("running", R.string.running);
        map.put("dead", R.string.dead);
        return map.get(s);
    }

    public static class AppInfo implements Comparable<AppInfo> {
        Drawable icon;
        int flags;
        String name = "";
        String packageName;
        boolean preventable = false;
        Set<Long> running;

        public AppInfo(String packageName, Drawable icon, String name, Set<Long> running) {
            super();
            this.packageName = packageName;
            if (name != null) {
                this.name = name;
            }
            this.icon = icon;
            this.running = running;
        }

        public AppInfo setFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public boolean isSystem() {
            return PackageUtils.isSystemPackage(this.flags);
        }

        @Override
        public String toString() {
            return (running == null ? "1" : "0") + (isSystem() ? "1" : "0") + "/" + name + "/" + packageName;
        }

        @Override
        public int compareTo(AppInfo another) {
            return Collator.getInstance().compare(toString(), another.toString());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AppInfo && compareTo((AppInfo) obj) == 0;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }

    protected class Adapter extends GroupedRecyclerViewAdapter {
        private LinkedHashMap<String, LinkedList<AppInfo>> group = new LinkedHashMap<String, LinkedList<AppInfo>>();
        private LinkedHashMap<String, Boolean> header = new LinkedHashMap<String, Boolean>();
        private LinkedList<AppInfo> runningAppInfos = new LinkedList<AppInfo>();
        private LinkedList<AppInfo> deadAppInfos = new LinkedList<AppInfo>();
        private Context mContext;
        private PackageManager mPm;

        public Adapter(Context context) {
            super(context);
            mContext = context;
            header.put("running", false);
            header.put("dead", false);
            group.put("running", runningAppInfos);
            group.put("dead", deadAppInfos);
            mPm = PreventFragment.this.mContext.getPackageManager();
        }

        public final LinkedList<AppInfo> getRunningAppInfos() {
            LinkedList<AppInfo> infos = new LinkedList<AppInfo>();
            for (AppInfo info : runningAppInfos) {
                infos.add(info);
            }
            return infos;
        }

        public final LinkedList<AppInfo> getDeadAppInfos() {
            LinkedList<AppInfo> infos = new LinkedList<AppInfo>();
            for (AppInfo info : deadAppInfos) {
                infos.add(info);
            }
            return infos;
        }

        public void removeRunningAppInfo(AppInfo info) {
            runningAppInfos.remove(info);
        }

        public void removeDeadAppInfo(AppInfo info) {
            deadAppInfos.remove(info);
        }

        public void clearAllPackage() {
            runningAppInfos.clear();
            deadAppInfos.clear();
        }


        public boolean isExpand(int groupPosition) {
            return header.get((String) header.keySet().toArray()[groupPosition]);
        }

        /**
         * 展开一个组
         *
         * @param groupPosition
         */
        public void expandGroup(int groupPosition) {
            expandGroup(groupPosition, true);
        }

        /**
         * 展开一个组
         *
         * @param groupPosition
         * @param animate
         */
        public void expandGroup(int groupPosition, boolean animate) {
            header.put((String) header.keySet().toArray()[groupPosition], true);
            if (animate) {
                notifyChildrenInserted(groupPosition);
            } else {
                notifyDataChanged();
            }
        }

        /**
         * 收起一个组
         *
         * @param groupPosition
         */
        public void collapseGroup(int groupPosition) {
            collapseGroup(groupPosition, true);
        }

        /**
         * 收起一个组
         *
         * @param groupPosition
         * @param animate
         */
        public void collapseGroup(int groupPosition, boolean animate) {
            header.put((String) header.keySet().toArray()[groupPosition], false);
            if (animate) {
                notifyChildrenRemoved(groupPosition);
            } else {
                notifyDataChanged();
            }
        }

        @Override
        public int getGroupCount() {
            return group.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (!header.get((String) header.keySet().toArray()[groupPosition])) {
                return 0;
            }
            return group.get(group.keySet().toArray()[groupPosition]).size();
        }

        @Override
        public boolean hasHeader(int groupPosition) {
            return true;
        }

        @Override
        public boolean hasFooter(int groupPosition) {
            return false;
        }

        @Override
        public int getHeaderLayout(int viewType) {
            return R.layout.adapter_expandable_header;
        }

        @Override
        public int getFooterLayout(int viewType) {
            return 0;
        }

        @Override
        public int getChildLayout(int viewType) {
            return R.layout.item;
        }

        @Override
        public void onBindHeaderViewHolder(BaseViewHolder holder, int groupPosition) {
            holder.setText(R.id.adapterexpandableheaderTextView1, getGroupHeaderName((String) header.keySet().toArray()[groupPosition]));
            ImageView ivState = holder.get(R.id.adapterexpandableheaderImageView1);
            if (header.get(header.keySet().toArray()[groupPosition])) {
                ivState.setRotation(90);
            } else {
                ivState.setRotation(0);
            }
        }

        @Override
        public void onBindFooterViewHolder(BaseViewHolder holder, int groupPosition) {

        }

        @Override
        public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
            String key = (String) group.keySet().toArray()[groupPosition];
            List<AppInfo> infos = group.get(key);
            AppInfo info = infos.get(childPosition);
            holder.setText(R.id.name, info.name);
            holder.setVisible(R.id.summary, View.VISIBLE);
            holder.setText(R.id.summary, StatusUtils.formatRunning(mContext, info.running));
            holder.setImageDrawable(R.id.icon, info.icon);
            holder.setVisible(R.id.loading, View.GONE);
            ((SwitchCompat) holder.get(R.id.prevent)).setOnCheckedChangeListener(null);
            ((SwitchCompat) holder.get(R.id.prevent)).setChecked(((PreventActivity) mContext).getPreventPackages().get(info.packageName) != null);
            ((SwitchCompat) holder.get(R.id.prevent)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (PackageUtils.isSystemPackage(info.flags)) {
                        if (PackageUtils.canPrevent(mContext.getPackageManager(), info.packageName)) {
                            ((PreventActivity) mContext).changePrevent(info.packageName, b);
                        } else {
                            compoundButton.setChecked(false);
                            Toast.makeText(mContext,"cannot prevent this app",Toast.LENGTH_SHORT).show();

                        }
                    } else {
                        ((PreventActivity) mContext).changePrevent(info.packageName, b);
                    }
                }
            });

            // holder.checkView.setEnabled(mCanPreventNames.contains(holder.packageName));
            // holder.checkView.setChecked(mActivity.getSelection().contains(holder.packageName));
        }

        public void addDeadPackage(AppInfo info) {
            deadAppInfos.add(info);
        }

        public void addRunningPackage(AppInfo info) {
            runningAppInfos.add(info);
        }

        public int getGroupIndexByName(String name) {
            int index = 0;
            for (String key : header.keySet()) {
                if (key.equals(name)) {
                    return index;
                }
                index += 1;
            }
            return -1;
        }

    }

}