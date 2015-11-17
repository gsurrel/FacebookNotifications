package org.surrel.facebooknotifications;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class HiddenActivity extends Activity {
    private static final int NOTIF_BASE = 0;
    private static final int NOTIF_FRIEND = NOTIF_BASE + 1;
    private static final int NOTIF_MESSAGE = NOTIF_FRIEND + 1;
    private static final int NOTIF_NOTIFICATION = NOTIF_MESSAGE + 1;

    public static final int AlarmType = AlarmManager.ELAPSED_REALTIME;
    public static final long TIME_SEC_MILLIS = AlarmManager.INTERVAL_FIFTEEN_MINUTES / 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intentForRestart = new Intent(this, HiddenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentForRestart, 0);
        intentForRestart.setAction(Intent.ACTION_MAIN);
        intentForRestart.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        alarmManager.setInexactRepeating(AlarmType, SystemClock.elapsedRealtime() + TIME_SEC_MILLIS, TIME_SEC_MILLIS, pendingIntent);

        Intent activityIntent = getIntent();
        if (activityIntent.getExtras().getBoolean(getString(R.string.enable_notifications), true) == false) {
            alarmManager.cancel(pendingIntent);
            finish();
        } else {
            final WebView webview = new WebView(this);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.addJavascriptInterface(this, "notification");
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    webview.loadUrl("javascript:window.notification.processJSON('{\"home\":'+(document.querySelector(\"[href*='/home']\")!=null)+',\"friends\":'+document.querySelector(\"[href*='/friends/']\").text.match(/[0-9]+/)+',\"messages\":'+document.querySelector(\"[href*='/messages/']\").text.match(/[0-9]+/)+',\"notifications\":'+document.querySelector(\"[href*='/notifications']\").text.match(/[0-9]+/)+'}');");
                }

//                @Override
//                public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                    NotificationCompat.Builder mBuilder =
//                            new NotificationCompat.Builder(getApplicationContext())
//                                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
//                                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                                    .setContentTitle(getString(R.string.could_not_get_notifications))
//                                    .setContentText(getString(R.string.maybe_logged_out))
//                                            //.setContentText(url)
//                                    .setPriority(Notification.PRIORITY_LOW)
//                                    .setCategory(Notification.CATEGORY_SOCIAL)
//                                    .setAutoCancel(true)
//                                    .setVisibility(Notification.VISIBILITY_PUBLIC);
//                    Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
//                    resultIntent.putExtra("url", url);
//                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
//                    stackBuilder.addNextIntent(resultIntent);
//                    PendingIntent resultPendingIntent =
//                            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//                    mBuilder.setContentIntent(resultPendingIntent);
//                    NotificationManager mNotificationManager =
//                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//                    mNotificationManager.notify(NOTIF_NOTIFICATION, mBuilder.build());
//                    return false;
//                }
            });
            WebSettings webSettings = webview.getSettings();
            webSettings.setBlockNetworkImage(true);
            webSettings.setUserAgentString(getString(R.string.app_name));
            webview.loadUrl("https://m.facebook.com/menu/bookmarks/");
            //setContentView(webview);
        }
    }

    @JavascriptInterface
    public void processJSON(String jsonStr) {
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
        // Kill activity
        finish();
    }


}
