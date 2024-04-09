package org.surrel.facebooknotifications;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.content.UnusedAppRestrictionsConstants;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 1; // Replace with your request code

    public static final int AlarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
    public static final int SETTINGS_MENU = 0;
    public static final String FB_URL = "https://m.facebook.com";

    private WebView webview;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkAppRestrictions(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        String targetURL = FB_URL;

        if (getIntent().getExtras() != null) {
            String url = getIntent().getExtras().getString("url", "");
            if (!"".equals(url)) {
                targetURL = url;
            }
        }

        WakeupManager.updateNotificationSystem(this);

        webview = new WebView(this);
        webview.setWebViewClient(new WebViewClient());
        webview.loadData("<h1>" + getString(R.string.request_pending) + "</h1>", "text/html", "UTF-8");
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(this, "customInterface");
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Javascript URL injection is defined in UpdateService
                webview.loadUrl("javascript:;");
            }
        });

        WebSettings webSettings = webview.getSettings();
        webSettings.setBlockNetworkImage(true);
        webSettings.setUserAgentString(getString(R.string.app_name));
        webview.loadUrl(targetURL);
        setContentView(webview);
        _dMsg("Debug build, timestamp " + BuildConfig.TIMESTAMP);
    }

    private void checkAppRestrictions(Context context) {
        ListenableFuture<Integer> future = PackageManagerCompat.getUnusedAppRestrictionsStatus(context);
        future.addListener(() -> {
            try {
                int result = future.get();
                onResult(result);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void onResult(int appRestrictionsStatus) {
        switch (appRestrictionsStatus) {
            case UnusedAppRestrictionsConstants.ERROR:
            case UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE:
            case UnusedAppRestrictionsConstants.DISABLED:
                break;
            case UnusedAppRestrictionsConstants.API_30_BACKPORT:
            case UnusedAppRestrictionsConstants.API_30:
            case UnusedAppRestrictionsConstants.API_31:
                handleRestrictions();
                break;
        }
    }

    private void handleRestrictions() {
        Intent intent = IntentCompat.createManageUnusedAppRestrictionsIntent(this, getPackageName());
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webview.canGoBack()) {
                    webview.goBack();
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_settings) {
            startActivityForResult(new Intent(MainActivity.this, PrefsActivity.class), SETTINGS_MENU);
        } else if (item.getItemId() == R.id.menu_item_quit) {
            finish();
        } else if (item.getItemId() == R.id.menu_item_problems) {
            Intent dontkillmyapp = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com/"));
            startActivity(dontkillmyapp);
        }
        return true;
    }

    protected void _dMsg(CharSequence text) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
