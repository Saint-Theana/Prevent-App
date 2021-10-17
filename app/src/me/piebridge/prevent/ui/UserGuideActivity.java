package me.piebridge.prevent.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.android.server.am.PreventRunning;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import me.piebridge.prevent.BuildConfig;
import me.piebridge.prevent.R;
import me.piebridge.prevent.common.PreventIntent;
import me.piebridge.prevent.ui.util.ColorUtils;
import me.piebridge.prevent.ui.util.DeprecatedUtils;
import me.piebridge.prevent.ui.util.EmailUtils;
import me.piebridge.prevent.ui.util.FileUtils;

/**
 * Created by thom on 15/10/3.
 */
public class UserGuideActivity extends Activity implements View.OnClickListener {


    private BroadcastReceiver receiver;

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0x1;

    private String name;
    private Integer version = null;
    private String method = null;

    private String colorBackground;
    private String colorText;
    private String colorLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.setBackgroundColor(ColorUtils.resolveColor(this, android.R.attr.colorBackground));
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        String path;
        if ("zh".equals(Locale.getDefault().getLanguage())) {
            path = "about.zh.html";
        } else {
            path = "about.en.html";
        }
        try {
            String template = FileUtils.readAsString(getAssets().open(path));
            resolveColors();
            String body = String.format(template, colorBackground, colorText, colorLink);
            webView.loadDataWithBaseURL(null, body, "text/html; charset=utf-8", "UTF-8", null);
        } catch (IOException e) {
            webView.loadUrl("file:///android_asset/" + path);
            UILog.d("cannot open " + path, e);
        }
        setView(R.id.alipay, "com.eg.android.AlipayGphone");
        if (hasPermission()) {
            setView(R.id.wechat, "com.tencent.mm");
        } else {
            findViewById(R.id.wechat).setVisibility(View.GONE);
        }
        if (!setView(R.id.paypal, "com.paypal.android.p2pmobile")) {
            TextView paypal = (TextView) findViewById(R.id.paypal);
            paypal.setClickable(true);
            paypal.setOnClickListener(this);
            paypal.setCompoundDrawablesWithIntrinsicBounds(null, cropDrawable(paypal.getCompoundDrawables()[1]), null, null);
        }
        retrieveInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        deleteQrCodeIfNeeded();
        super.onDestroy();
    }

    private int getPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private Drawable cropDrawable(Drawable icon) {
        int width = getPixel(0x20);
        if (icon.getMinimumWidth() > width && icon instanceof BitmapDrawable) {
            Bitmap bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) icon).getBitmap(), width, width, false);
            return new BitmapDrawable(getResources(), bitmap);
        }
        return icon;
    }

    private boolean setView(int id, String packageName) {
        TextView donate = (TextView) findViewById(id);
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            if (!info.enabled) {
                return false;
            }
            CharSequence label = getLabel(pm, info);
            donate.setContentDescription(label);
            donate.setCompoundDrawablesWithIntrinsicBounds(null, cropDrawable(pm.getApplicationIcon(info)), null, null);
            donate.setText(label);
            donate.setClickable(true);
            donate.setOnClickListener(this);
            donate.setVisibility(View.VISIBLE);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            UILog.d("cannot find package " + packageName, e);
            return false;
        }
    }

    private CharSequence getLabel(PackageManager pm, ApplicationInfo info) throws PackageManager.NameNotFoundException {
        CharSequence label = null;
        if ("com.android.vending".equals(info.packageName)) {
            Resources resources = pm.getResourcesForApplication(info);
            int appName = resources.getIdentifier("app_name", "string", info.packageName);
            if (appName > 0) {
                label = resources.getText(appName);
            }
        }
        if (TextUtils.isEmpty(label)) {
            label = pm.getApplicationLabel(info);
        }
        return label;
    }

    private File getQrCode() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (dir == null) {
            return null;
        }
        if (!checkPermission()) {
            return null;
        }
        File screenshots = new File(dir, "Screenshots");
        if (!screenshots.exists()) {
            screenshots.mkdirs();
        }
        return new File(screenshots, "pr_donate.png");
    }

    public int checkPermission(String permission) {
        return checkPermission(permission, android.os.Process.myPid(), Process.myUid());
    }

    private boolean hasPermission() {
        return checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                || !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private boolean checkPermission() {
        if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                donateViaWeChat();
            } else {
                findViewById(R.id.wechat).setVisibility(View.GONE);
            }
        }
    }

    private void refreshQrCode(File qrCode) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(qrCode));
        sendBroadcast(mediaScanIntent);
    }

    private void deleteQrCodeIfNeeded() {
        File qrCode = getQrCode();
        if (qrCode != null && qrCode.exists()) {
            qrCode.delete();
            refreshQrCode(qrCode);
        }
    }

    private boolean donateViaWeChat() {
        File qrCode = getQrCode();
        if (qrCode == null) {
            return false;
        }
        try {
            FileUtils.dumpFile(getAssets().open("wechat.png"), qrCode);
        } catch (IOException e) {
            UILog.d("cannot dump wechat", e);
            return false;
        }
        refreshQrCode(qrCode);
        Intent intent = new Intent("com.tencent.mm.action.BIZSHORTCUT");
        intent.setPackage("com.tencent.mm");
        intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(intent);
            for (int i = 0; i < 0x3; ++i) {
                Toast.makeText(this, R.string.select_qr_code, Toast.LENGTH_LONG).show();
            }
        } catch (Throwable t) { // NOSONAR
        }
        return true;
    }

    private boolean donateViaAlipay() {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(BuildConfig.DONATE_ALIPAY));
        try {
            startActivity(intent);
        } catch (Throwable t) { // NOSONAR

        }
        return true;
    }

    private boolean donateViaPayPal() {

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DONATE_PAYPAL)));
        } catch (Throwable t) { // NOSONAR
            // do nothing
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.wechat) {
            donateViaWeChat();
        } else if (id == R.id.alipay) {
            donateViaAlipay();
        } else if (id == R.id.paypal) {
            donateViaPayPal();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(Menu.NONE, R.string.version, Menu.NONE, R.string.version);
        if (BuildConfig.RELEASE) {
            menu.add(Menu.NONE, R.string.feedback, Menu.NONE, R.string.feedback);
        }
        menu.add(Menu.NONE, R.string.advanced_settings, Menu.NONE, R.string.advanced_settings);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.string.feedback) {
            feedback();
        } else if (id == R.string.version) {
            showVersionInfo();
        } else if (id == R.string.advanced_settings) {
            startActivity(new Intent(this, AdvancedSettingsActivity.class));
        }
        return true;
    }

    private void feedback() {
        EmailUtils.sendEmail(this, getString(R.string.feedback));
    }



    private void retrieveInfo() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setAction(PreventIntent.ACTION_GET_INFO);
        intent.setData(Uri.fromParts(PreventIntent.SCHEME, getPackageName(), null));
        UILog.i("sending get info broadcast");
        if (receiver == null) {
            receiver = new HookReceiver();
        }
        sendOrderedBroadcast(intent, PreventIntent.PERMISSION_SYSTEM, receiver, null, 0, null, null);
    }

    private class HookReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PreventIntent.ACTION_GET_INFO.equals(action)) {
                handleInfo();
            }
        }

        private void handleInfo() {
            String info = getResultData();
            if (TextUtils.isEmpty(info)) {
                return;
            }
            try {
                JSONObject json = new JSONObject(info);
                version = json.optInt("version");
                method = json.optString("method");
                name = json.optString("name");
            } catch (JSONException e) {
                UILog.d("cannot get version from " + info, e);
            }
        }
    }

    private String getVersionInfo(boolean showAppVersion) {
        StringBuilder sb = new StringBuilder();
        showVersion(sb);
        sb.append("Android: ");
        sb.append(Locale.getDefault());
        sb.append("-");
        sb.append(Build.VERSION.RELEASE);
        sb.append("\n");
        if (showAppVersion) {
            sb.append(getString(R.string.app_name));
            sb.append(": ");
            sb.append(BuildConfig.VERSION_NAME);
            sb.append("\n");
        }
        sb.append(Build.FINGERPRINT);
        return sb.toString();
    }

    private void showVersion(StringBuilder sb) {
        if (name != null && !BuildConfig.VERSION_NAME.equalsIgnoreCase(name)) {
            sb.append("Active: ");
            sb.append(name);
            sb.append("\n");
        }
        if (version != null) {
            if (version == 0) {
                method = "native";
            }
            sb.append("Bridge: ");
            sb.append(method);
            sb.append(" v");
            sb.append(version);
            if ("native".equalsIgnoreCase(method) && version < PreventRunning.VERSION) {
                sb.append(" -> v");
                sb.append(PreventRunning.VERSION);
            }
            sb.append("\n");
        }
    }

    private void showVersionInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name) + "(" + BuildConfig.VERSION_NAME + ")");
        builder.setMessage(getVersionInfo(false));
        builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton(getString(android.R.string.copy), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DeprecatedUtils.setClipboard(getBaseContext(), getVersionInfo(true));
            }
        });
        builder.create().show();
    }

    private void resolveColors() {
        colorBackground = ColorUtils.rgba(ColorUtils.resolveColor(this, android.R.attr.colorBackground));
        colorLink = ColorUtils.rgba(ColorUtils.resolveColor(this, android.R.attr.textColorLink));
        int textColorPrimary = ColorUtils.resolveColor(this, android.R.attr.textColorPrimary);
        colorText = ColorUtils.rgba(ColorUtils.fixOpacity(textColorPrimary));
    }

}
