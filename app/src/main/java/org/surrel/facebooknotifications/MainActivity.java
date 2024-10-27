package org.surrel.facebooknotifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.content.UnusedAppRestrictionsConstants;
import androidx.core.view.MenuItemCompat;
import androidx.preference.PreferenceManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    public static final int RESULT_REDRAW_MENU = 2;
    public static final int AlarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
    public static final int SETTINGS_MENU = 0;
    public static final String SHOW_SHARE_BUTTON = "show_share_button";
    private static final int REQUEST_CODE_RESTRICTIONS = 100;
    public static final String FB_URL = "https://m.facebook.com";

    private WebView webview;
    private final static int FCR = 1;
    private ShareActionProvider mShareActionProvider;
    private Intent shareIntent;
    private SharedPreferences mPrefs;
    private Menu mMenu;
    private MenuItem shareItem;
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private String logoutUrl = "";

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
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);

        String targetURL = FB_URL;

        if (getIntent().getExtras() != null) {
            String url = getIntent().getExtras().getString("url", "");
            if (!"".equals(url)) {
                targetURL = url;
            }
        }

        WakeupManager.updateNotificationSystem(this);

        shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        webview = new WebView(this);
        webview.loadData("<h1>" + getString(R.string.request_pending) + "</h1>", "text/html", "UTF-8");
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(this, "customInterface");

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webview.loadUrl("javascript:getLogout=function(){elt=document.querySelector(\"[href*='/logout']\"); return elt.href;};" + "window.customInterface.processLogoutStr(getLogout());");
                updateShareIntent();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                if (url.startsWith("http://") || url.startsWith("https://")) return false;
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    view.getContext().startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        webview.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    } catch (IOException ex) {
                        Log.e("fbn", "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);
                return true;
            }
        });

        WebSettings webSettings = webview.getSettings();
        webSettings.setBlockNetworkImage(false);
        webSettings.setUserAgentString(mPrefs.getString("user_agent", "Mozilla/5.0 (Linux; Android 7.0; Pixel C Build/NRD91D; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.124 Safari/537.36 [FB_IAB/FB4A;FBAV/98.0.0.18.70;]"));
        webview.loadUrl(targetURL);
        setContentView(webview);
        _dMsg("Debug build, timestamp " + BuildConfig.TIMESTAMP);
    }

    // --- App Restrictions Handling ---
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
        startActivityForResult(intent, REQUEST_CODE_RESTRICTIONS);
    }

    // --- Javascript Interface ---
    @SuppressWarnings("unused")
    @JavascriptInterface
    public void processLogoutStr(String logoutStr) {
        Log.i("fbn.MainActivity", logoutStr);
        if (logoutStr.contains("facebook.com")) this.logoutUrl = logoutStr;
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

        shareItem = menu.findItem(R.id.menu_item_share);
        shareItem.setVisible(mPrefs.getBoolean(SHOW_SHARE_BUTTON, false));
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        updateShareIntent();
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (shareItem != null) {
            shareItem.setVisible(mPrefs.getBoolean(SHOW_SHARE_BUTTON, false));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_item_settings) {
            startActivityForResult(new Intent(MainActivity.this, PrefsActivity.class), SETTINGS_MENU);
            return true;
        } else if (id == R.id.menu_item_logout) {
            if (logoutUrl != null && !logoutUrl.isEmpty()) {
                webview.loadUrl(logoutUrl);
            } else {
                _msg("Logout link not found yet");
            }
            return true;
        } else if (id == R.id.menu_item_quit) {
            finish();
            return true;
        } else if (id == R.id.menu_item_open_browser) {
            String url = webview.getUrl();
            if (url != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(Intent.createChooser(browserIntent, "Open with"));
            } else {
                _msg("No URL to open");
            }
            return true;
        } else if (id == R.id.menu_item_problems) {
            Intent dontkillmyapp = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com/"));
            startActivity(dontkillmyapp);
            return true;
        }

        // If we didn't handle the item, pass it to the superclass
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_REDRAW_MENU) {
            onPrepareOptionsMenu(mMenu);
        } else if (requestCode == FCR) {
            if (Build.VERSION.SDK_INT >= 21) {
                Uri[] results = null;
                if (resultCode == RESULT_OK) {
                    if (null == mUMA) return;
                    if (intent == null) {
                        if (mCM != null) results = new Uri[]{Uri.parse(mCM)};
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) results = new Uri[]{Uri.parse(dataString)};
                    }
                }
                mUMA.onReceiveValue(results);
                mUMA = null;
            } else {
                if (null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    protected void _msg(CharSequence text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    protected void _dMsg(CharSequence text) {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    protected void updateShareIntent() {
        if (webview != null && webview.getUrl() != null) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, webview.getUrl());
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(shareIntent);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
