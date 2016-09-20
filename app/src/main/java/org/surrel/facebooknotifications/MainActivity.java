package org.surrel.facebooknotifications;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ShareActionProvider;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final int AlarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
    public static final int SETTINGS_MENU = 0;
    public static final int RESULT_REDRAW_MENU = 2;
    public static final String SHOW_SHARE_BUTTON = "show_share_button";
    public static final String FB_URL = "https://m.facebook.com";

    private WebView webview;
    private ShareActionProvider mShareActionProvider;
    private Intent shareIntent;
    private SharedPreferences mPrefs;
    private Menu mMenu;
    private MenuItem shareItem;
    private MenuItem shareAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        webview.setWebViewClient(new WebViewClient());
        webview.loadData("<h1>" + getString(R.string.request_pending) + "</h1>", "text/html", "UTF-8");
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                updateShareIntent();
            }
        });
        WebSettings webSettings = webview.getSettings();
        webSettings.setBlockNetworkImage(false);
        webSettings.setUserAgentString(getString(R.string.app_name));
        webview.loadUrl(targetURL);
        setContentView(webview);
        _dMsg("Debug build, timestamp " + BuildConfig.TIMESTAMP);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
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
        shareAction = menu.findItem(R.id.menu_action_share);
        shareAction.setVisible(mPrefs.getBoolean(SHOW_SHARE_BUTTON, false));
        mShareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        updateShareIntent();
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        shareAction.setVisible(mPrefs.getBoolean(SHOW_SHARE_BUTTON, false));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_settings) {
            startActivityForResult(new Intent(MainActivity.this, PrefsActivity.class), SETTINGS_MENU);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_MENU) {
            if (resultCode == RESULT_REDRAW_MENU) {
                onPrepareOptionsMenu(mMenu);
            }
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

    protected boolean updateShareIntent() {
        shareIntent.putExtra(Intent.EXTRA_TEXT, webview.getUrl());
        mShareActionProvider.setShareIntent(shareIntent);
        return false;
    }
    @Override
    public void onPause() {
        super.onPause();
    }
}
