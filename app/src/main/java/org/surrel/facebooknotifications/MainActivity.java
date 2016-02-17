package org.surrel.facebooknotifications;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    public static final int AlarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;

    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String targetURL = "https://m.facebook.com/";

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
        webview.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webview.getSettings();
        webSettings.setBlockNetworkImage(false);
        webSettings.setUserAgentString(getString(R.string.app_name));
        webview.loadUrl(targetURL);
        setContentView(webview);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, PrefsActivity.class));
        }

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
