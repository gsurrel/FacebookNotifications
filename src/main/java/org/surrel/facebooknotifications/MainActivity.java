package org.surrel.facebooknotifications;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    public static final int AlarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
    public static final long TIME_SEC_MILLIS = AlarmManager.INTERVAL_FIFTEEN_MINUTES / 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String targetURL = "https://m.facebook.com/menu/bookmarks/";

        if(getIntent().getExtras() != null) {
            String url = getIntent().getExtras().getString("url", "");
            if (!"".equals(url)) {
                targetURL = url;
            }
        }

        WakeupManager.updateNotificationSystem(this);

        final WebView webview = new WebView(this);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        SharedPreferences sharedPref = this.getSharedPreferences("UpdatePrefs", Context.MODE_PRIVATE);
        boolean notifications = sharedPref.getBoolean(getString(R.string.enable_notifications), true);
        menu.getItem(0).setChecked(notifications);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.sample_action) {
            final boolean newState = !item.isChecked();
            item.setChecked(newState);
            SharedPreferences sharedPref = this.getSharedPreferences("UpdatePrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.enable_notifications), newState);
            editor.apply();
            WakeupManager.updateNotificationSystem(this);
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
