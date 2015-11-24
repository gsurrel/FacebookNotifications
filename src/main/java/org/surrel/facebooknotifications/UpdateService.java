package org.surrel.facebooknotifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class UpdateService extends Service {
    private static final int NOTIF_BASE = 0;
    private static final int NOTIF_FRIEND = NOTIF_BASE + 1;
    private static final int NOTIF_MESSAGE = NOTIF_FRIEND + 1;
    private static final int NOTIF_NOTIFICATION = NOTIF_MESSAGE + 1;

    private WindowManager windowManager;
    private WebView webview;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();

        if(!connectionAvailable()) return;

        webview = new WebView(this);
        webview.setVisibility(View.GONE);
        webview.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(this, "notification");
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webview.loadUrl("javascript:window.notification.processJSON(\n" +
                        "document.querySelector(\"[href*='/home']\")==null ? '{\"home\":false}' :\n" +
                        "'{\"home\":true'+',\"friends\":'\n" +
                        "+document.querySelector(\"[href*='/friends/']\").text.match(/[0-9]+/)\n" +
                        "+',\"messages\":'\n" +
                        "+document.querySelector(\"[href*='/messages/']\").text.match(/[0-9]+/)\n" +
                        "+',\"notifications\":'\n" +
                        "+document.querySelector(\"[href*='/notifications']\").text.match(/[0-9]+/)\n" +
                        "+'}');");
                Log.i("fbn", "Loading finished");
            }
        });

        WebSettings webSettings = webview.getSettings();
        webSettings.setBlockNetworkImage(true);
        webSettings.setUserAgentString(getString(R.string.app_name));
        webview.loadUrl("https://m.facebook.com/menu/bookmarks/");

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.NO_GRAVITY;
        params.x = 0;
        params.y = 0;
        params.width = 0;
        params.height = 0;
        windowManager = (WindowManager)

                getSystemService(WINDOW_SERVICE);

        windowManager.addView(webview, params);
    }

    @JavascriptInterface
    public void processJSON(String jsonStr) {
        Log.i("fbn", jsonStr);
        try {
            JSONObject json = new JSONObject(jsonStr);
            if (!json.optBoolean("home", false)) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                                .setContentTitle(getString(R.string.could_not_get_notifications))
                                .setContentText(getString(R.string.maybe_logged_out))
                                .setPriority(Notification.PRIORITY_LOW)
                                .setCategory(Notification.CATEGORY_SOCIAL)
                                .setAutoCancel(true)
                                .setVisibility(Notification.VISIBILITY_PUBLIC);
                Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NOTIF_BASE, mBuilder.build());
            }
            int nbFriends = json.optInt("friends", -1);
            int nbMessages = json.optInt("messages", -1);
            int nbNotifications = json.optInt("notifications", -1);
            Log.i("fbn", "F:" + nbFriends + " M:" + nbMessages + " N:" + nbNotifications);
            if (nbFriends > 0) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_notification)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                                .setContentTitle(getString(R.string.you_have) + " " + nbFriends + " " + getString(R.string.friend_requests))
                                .setPriority(Notification.PRIORITY_LOW)
                                .setCategory(Notification.CATEGORY_SOCIAL)
                                .setVibrate(new long[]{0, 300, 300, 300})
                                .setAutoCancel(true)
                                .setVisibility(Notification.VISIBILITY_PUBLIC);
                Intent resultIntent = new Intent(Intent.ACTION_VIEW);
                resultIntent.setData(Uri.parse("https://m.facebook.com/friends/center/requests/"));
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NOTIF_FRIEND, mBuilder.build());
            }
            if (nbMessages > 0) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_notification)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                                .setContentTitle(getString(R.string.you_have) + " " + nbMessages + " " + getString(R.string.new_messages))
                                .setPriority(Notification.PRIORITY_HIGH)
                                .setCategory(Notification.CATEGORY_MESSAGE)
                                .setVibrate(new long[]{0, 500})
                                .setAutoCancel(true)
                                .setVisibility(Notification.VISIBILITY_PUBLIC);
                Intent resultIntent = new Intent(Intent.ACTION_VIEW);
                resultIntent.setData(Uri.parse("https://m.facebook.com/messages/"));
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NOTIF_MESSAGE, mBuilder.build());
            }
            if (nbNotifications > 0) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_notification)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                                .setContentTitle(getString(R.string.you_have) + " " + nbNotifications + " " + getString(R.string.new_notifications))
                                .setPriority(Notification.PRIORITY_LOW)
                                .setCategory(Notification.CATEGORY_SOCIAL)
                                .setVibrate(new long[]{0, 300})
                                .setAutoCancel(true)
                                .setVisibility(Notification.VISIBILITY_PUBLIC);
                Intent resultIntent = new Intent(Intent.ACTION_VIEW);
                resultIntent.setData(Uri.parse("https://m.facebook.com/notifications.php"));
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NOTIF_NOTIFICATION, mBuilder.build());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.stopSelf();
    }

    private boolean connectionAvailable() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webview != null)
            windowManager.removeView(webview);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}