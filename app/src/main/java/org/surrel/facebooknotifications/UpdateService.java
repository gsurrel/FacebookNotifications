package org.surrel.facebooknotifications;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
    private static final int NOTIF_LOGIN = NOTIF_BASE + 1;
    private static final int NOTIF_UNIFIED = NOTIF_LOGIN + 1;

    private WindowManager windowManager;
    private WebView webview;
    SharedPreferences sharedPreferences;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate() {
        super.onCreate();

        webview = new WebView(this);
        webview.setVisibility(View.GONE);
        webview.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(this, "customInterface");
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                /* Copy-paste friendly version (for the '\'):
                javascript:get=function(url){elt=document.querySelector("[href*='/"+url+"']>strong"); return elt!=null ? elt.textContent.match(/[0-9]+/) : "null";};
                            window.notification.processJSON(window.location.pathname=="/login.php" ? '{"login":true}' : '{"login":false'+',"friends":'+get("friends")
                            +',"messages":'+get("messages")
                            +',"notifications":'+get("notifications")+'}');

                 */
                webview.loadUrl("javascript:get=function(url){elt=document.querySelector(\"[href*='/\"+url+\"']>strong\"); return elt!=null ? elt.textContent.match(/[0-9]+/) : \"null\";};\n" +
                        "window.customInterface.processJSON(window.location.pathname==\"/login.php\" ? '{\"login\":true}' : '{\"login\":false'+',\"friends\":'+get(\"friends\")\n" +
                        "+',\"messages\":'+get(\"messages\")\t\n" +
                        "+',\"notifications\":'+get(\"notifications\")+'}');");
                Log.i("fbn.UpdateService", "Loading finished");
            }
        });

        WebSettings webSettings = webview.getSettings();
        webSettings.setBlockNetworkImage(true);
        webSettings.setUserAgentString(getString(R.string.app_name));
        webview.loadUrl("https://m.facebook.com/menu/bookmarks/");

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
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
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(webview, params);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @SuppressWarnings("deprecation")
    Notification getLegacyNotification(int icon, String title, String text, Intent intent, int notifType, String soundURI, long[] vibrationPattern) {
        Notification msg = new Notification(icon, title, System.currentTimeMillis());
        msg.vibrate = vibrationPattern;

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        if (soundURI != null) {
            msg.sound = Uri.parse(soundURI);
        }

        msg.flags |= Notification.FLAG_AUTO_CANCEL;
        msg.setLatestEventInfo(getApplicationContext(), title, text, pendingIntent);
        return msg;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    Notification.Builder getNewStyleNotification(int smallIcon, Bitmap largeIcon, String title, String text, int priority, Intent resultIntent, int notifType, String soundURI, long[] vibrationPattern) {
        Notification.Builder mBuilder =
                new Notification.Builder(getApplicationContext())
                        .setSmallIcon(smallIcon)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setPriority(priority)
                        .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setCategory(Notification.CATEGORY_SOCIAL)
                    .setVisibility(Notification.VISIBILITY_PRIVATE);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        if (soundURI != null) {
            Uri uri = Uri.parse(soundURI);
            mBuilder.setSound(uri);
        }

        mBuilder.setVibrate(vibrationPattern);

        return mBuilder;
    }


    @SuppressWarnings("unused")
    @JavascriptInterface
    public void processJSON(String jsonStr) {
        Log.i("fbn.UpdateService", jsonStr);
        try {
            JSONObject json = new JSONObject(jsonStr);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (json.getBoolean("login")) {
                Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    Notification notif = getLegacyNotification(android.R.drawable.ic_dialog_alert,
                            getString(R.string.could_not_get_notifications),
                            getString(R.string.maybe_logged_out),
                            resultIntent,
                            NOTIF_LOGIN,
                            null,
                            new long[]{});
                    mNotificationManager.notify(NOTIF_LOGIN, notif);
                } else {
                    Notification.Builder notif = getNewStyleNotification(android.R.drawable.ic_dialog_alert,
                            BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                            getString(R.string.could_not_get_notifications),
                            getString(R.string.maybe_logged_out),
                            Notification.PRIORITY_LOW,
                            resultIntent,
                            NOTIF_LOGIN,
                            null,
                            new long[]{});
                    mNotificationManager.notify(NOTIF_LOGIN, notif.build());
                }
            } else {
                // If we had a connection problem but now it's OK, remove the "login" notification
                mNotificationManager.cancel(NOTIF_LOGIN);
                int nbFriends = sharedPreferences.getBoolean("notification_friends", true) ? json.optInt("friends", 0) : 0;
                int nbMessages = sharedPreferences.getBoolean("notification_messages", true) ? json.optInt("messages", 0) : 0;
                int nbNotifications = sharedPreferences.getBoolean("notification_notifications", true) ? json.optInt("notifications", 0) : 0;

                SharedPreferences settings = getSharedPreferences("NotifCount", 0);

                // If we have no notifications, remove the existing one
                Log.i("fbn.UpdateService", "F:" + nbFriends + " M:" + nbMessages + " N:" + nbNotifications);
                if (nbFriends + nbMessages + nbNotifications == 0) {
                    mNotificationManager.cancel(NOTIF_UNIFIED);
                } else if (nbFriends != settings.getInt("nbFriends", 0)
                        || nbMessages != settings.getInt("nbMessages", 0)
                        || nbNotifications != settings.getInt("nbNotifications", 0)) { // If the count is the same as before, change nothing

                    // Build a notification
                    String notifText = getString(R.string.you_have);

                    // If it is a multi-category notification, remove rephrase notification message
                    boolean multipleCategories = false;
                    if ((nbFriends > 0 ? 1 : 0)
                            + (nbMessages > 0 ? 1 : 0)
                            + (nbNotifications > 0 ? 1 : 0) > 1) {
                        multipleCategories = true;
                        notifText = "";
                    }

                    // Build the message
                    boolean first = true;
                    if (nbFriends > 0) {
                        first = false;
                        notifText = notifText + " " + nbFriends + " " +
                                (multipleCategories ? getString(R.string.new_friend_requests_short) : getString(R.string.new_friend_requests));
                    }
                    if (nbMessages > 0) {
                        if (!first) notifText = notifText + ",";
                        first = false;
                        notifText = notifText + " " + nbMessages + " " +
                                (multipleCategories ? getString(R.string.new_messages_short) : getString(R.string.new_messages));
                    }
                    if (nbNotifications > 0) {
                        if (!first) notifText = notifText + ",";
                        notifText = notifText + " " + nbNotifications + " " +
                                (multipleCategories ? getString(R.string.new_notifications_short) : getString(R.string.new_notifications));
                    }

                    // Choose the right sound and vibration style according to category weight (notification < friend < message)
                    String soundURI = "";
                    long[] vibrationPattern = new long[]{0, 200};
                    int[] rate = new int[]{0, 0};
                    if (nbNotifications > 0) {
                        soundURI = sharedPreferences.getString("notification_sound_choice_notifications", null);
                        vibrationPattern = getPattern(sharedPreferences.getString("notification_vibrate_choice_notifications", "vibrate_short"));
                        rate = getBlinkRate(sharedPreferences.getString("notification_blink_rate_notifications", ""));
                    }
                    if (nbFriends > 0) {
                        soundURI = sharedPreferences.getString("notification_sound_choice_friends", null);
                        vibrationPattern = getPattern(sharedPreferences.getString("notification_vibrate_choice_friends", "vibrate_short"));
                        rate = getBlinkRate(sharedPreferences.getString("notification_blink_rate_friends", ""));
                    }
                    if (nbMessages > 0) {
                        soundURI = sharedPreferences.getString("notification_sound_choice_messages", null);
                        vibrationPattern = getPattern(sharedPreferences.getString("notification_vibrate_choice_messages", "vibrate_double"));
                        rate = getBlinkRate(sharedPreferences.getString("notification_blink_rate_messages", ""));
                    }

                    // Build the intent
                    Intent resultIntent = new Intent(Intent.ACTION_VIEW);
                    resultIntent.setData(Uri.parse("https://m.facebook.com/"));
                    if (!multipleCategories) {
                        // If only one category, make the notification more specific
                        if (nbFriends > 0) {
                            resultIntent.setData(Uri.parse("https://m.facebook.com/friends/center/requests/"));
                        }
                        if (nbMessages > 0) {
                            resultIntent.setData(Uri.parse("https://m.facebook.com/messages/"));
                        }
                        if (nbNotifications > 0) {
                            resultIntent.setData(Uri.parse("https://m.facebook.com/notifications.php"));
                        }
                    }

                    // Notify for legacy devices, make it more specific otherwise
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        getLegacyNotification(R.drawable.ic_notification,
                                getString(R.string.app_name),
                                notifText,
                                resultIntent,
                                NOTIF_UNIFIED,
                                soundURI,
                                vibrationPattern);
                    } else {
                        // Basic common notification, will be altered for more important states (messages)
                        Notification.Builder notif = getNewStyleNotification(R.drawable.ic_notification,
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                                getString(R.string.app_name),
                                notifText,
                                Notification.PRIORITY_DEFAULT,
                                resultIntent,
                                NOTIF_UNIFIED,
                                soundURI,
                                vibrationPattern);

                        // Set new priority and category if needed
                        if (nbMessages > 0) {
                            notif.setPriority(Notification.PRIORITY_HIGH);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                notif.setCategory(Notification.CATEGORY_MESSAGE);
                            }
                        }

                        // Set blinking
                        notif.setLights(Color.BLUE, rate[0], rate[1]);

                        if (multipleCategories) {
                            // If it's a multicategory notification, create the BigView, display in the same order as Facebook
                            notif.setStyle(new Notification.BigTextStyle().bigText(notifText));
                            if (nbFriends > 0) {
                                Intent btnIntent = (Intent) resultIntent.clone();
                                btnIntent.setData(Uri.parse("https://m.facebook.com/friends/center/requests/"));
                                TaskStackBuilder sBuilder = TaskStackBuilder.create(this);
                                sBuilder.addNextIntent(btnIntent);
                                PendingIntent pi = sBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                                notif.addAction(R.drawable.ic_menu_invite, getString(R.string.friends), pi);
                            }
                            if (nbMessages > 0) {
                                Intent btnIntent = (Intent) resultIntent.clone();
                                btnIntent.setData(Uri.parse("https://m.facebook.com/messages/"));
                                TaskStackBuilder sBuilder = TaskStackBuilder.create(this);
                                sBuilder.addNextIntent(btnIntent);
                                PendingIntent pi = sBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                                notif.addAction(R.drawable.ic_menu_start_conversation, getString(R.string.messages), pi);
                            }
                            if (nbNotifications > 0) {
                                Intent btnIntent = (Intent) resultIntent.clone();
                                btnIntent.setData(Uri.parse("https://m.facebook.com/notifications.php"));
                                TaskStackBuilder sBuilder = TaskStackBuilder.create(this);
                                sBuilder.addNextIntent(btnIntent);
                                PendingIntent pi = sBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                                notif.addAction(R.drawable.ic_menu_mapmode, getString(R.string.notifications), pi);
                            }
                        }

                        mNotificationManager.notify(NOTIF_UNIFIED, notif.build());
                    }
                } else {
                    Log.i("fbn.UpdateService", "Same number of events per categories, skipping notification");
                }

                // Save in the settings the current count of notifs per type
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("nbFriends", nbFriends);
                editor.putInt("nbMessages", nbMessages);
                editor.putInt("nbNotifications", nbNotifications);
                editor.apply();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.stopSelf();
    }

    private int[] getBlinkRate(String pref) {
        switch (pref) {
            case "frenetic":
                return new int[]{300, 200};
            case "normal":
                return new int[]{1000, 4000};
            case "slow":
            default:
                return new int[]{1000, 8000};
        }
    }

    private long[] getPattern(String string) {
        switch (sharedPreferences.getString(string, "vibrate_short")) {
            case "vibrate_short":
                return new long[]{0, 200};
            case "vibrate_long":
                return new long[]{0, 400};
            case "vibrate_double":
                return new long[]{0, 200, 200, 200};
            case "vibrate_double_long":
                return new long[]{0, 400, 300, 400};
            default:
                return new long[0];
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webview != null)
            windowManager.removeView(webview);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}