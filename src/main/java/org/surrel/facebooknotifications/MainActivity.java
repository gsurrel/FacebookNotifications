package org.surrel.facebooknotifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e("fbn.MainActivity", "onCreate");

        String targetURL = "https://m.facebook.com/menu/bookmarks/";

        if(getIntent().getExtras() != null) {
            String url = getIntent().getExtras().getString("url", "");
            if ("".equals(url) == false) {
                targetURL = url;
            }
        }

        final WebView webview = new WebView(this);
        webview.setWebViewClient(new WebViewClient());
        webview.loadData("<h1>Request pending</h1>", "text/html", "UTF-8");
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webview.getSettings();
        webSettings.setBlockNetworkImage(true);
        webSettings.setUserAgentString("Facebook Notifications");
        webview.loadUrl(targetURL);
        setContentView(webview);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        boolean notifications = sharedPref.getBoolean(getString(R.string.enable_notifications), false);
        menu.getItem(0).setChecked(notifications);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sample_action) {
            final boolean newState = !item.isChecked();
            item.setChecked(newState);
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.enable_notifications), newState);
            editor.apply();
            Intent i = new Intent(this, HiddenActivity.class);
            i.putExtra(getString(R.string.enable_notifications), newState);
            startActivity(i);
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
