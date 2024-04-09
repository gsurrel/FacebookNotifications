package org.surrel.facebooknotifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
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
import android.provider.Settings;
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

import java.util.Objects;

public class UpdateService extends Service {
    private static final int NOTIF_BASE = 0;
    private static final int NOTIF_LOGIN = NOTIF_BASE + 1;
    private static final int NOTIF_UNIFIED = NOTIF_LOGIN + 1;

    private static final String URL_HOME = "https://m.facebook.com/";
    private static final String URL_BOOKMARKS = URL_HOME + "menu/bookmarks/";
    private static final String URL_FRIEND_REQUESTS = URL_HOME + "friends/center/requests/";
    private static final String URL_MESSAGES = URL_HOME + "messages/";
    private static final String URL_GROUPS = URL_HOME + "groups/";
    private static final String URL_NOTIFICATIONS = URL_HOME + "notifications.php";

    private static final String PREF_FRIEND_REQUESTS = "notification_friends";
    private static final String PREF_MESSAGES = "notification_messages";
    private static final String PREF_GROUPS = "notification_groups";
    private static final String PREF_NOTIFICATIONS = "notification_notifications";

    private static final String PREF_SOUND_FRIEND_REQUESTS = "notification_sound_choice_friends";
    private static final String PREF_SOUND_MESSAGES = "notification_sound_choice_messages";
    private static final String PREF_SOUND_NOTIFICATIONS = "notification_sound_choice_notifications";
    private static final String PREF_SOUND_GROUPS = "notification_sound_choice_groups";

    private static final String PREF_VIBRATE_FRIEND_REQUESTS = "notification_vibrate_choice_friends";
    private static final String PREF_VIBRATE_MESSAGES = "notification_vibrate_choice_messages";
    private static final String PREF_VIBRATE_GROUPS = "notification_vibrate_choice_groups";
    private static final String PREF_VIBRATE_NOTIFICATIONS = "notification_vibrate_choice_notifications";

    private static final String PREF_BLINK_FRIEND_REQUESTS = "notification_blink_rate_choice_friends";
    private static final String PREF_BLINK_MESSAGES = "notification_blink_rate_choice_messages";
    private static final String PREF_BLINK_GROUPS = "notification_blink_rate_choice_groups";
    private static final String PREF_BLINK_NOTIFICATIONS = "notification_blink_rate_choice_notifications";

    private static final String KEY_VIBRATE_SHORT = "vibrate_short";
    private static final String KEY_VIBRATE_LONG = "vibrate_long";
    private static final String KEY_VIBRATE_DOUBLE = "vibrate_double";
    private static final String KEY_VIBRATE_DOUBLE_LONG = "vibrate_double_long";

    private static final String KEY_BLINK_NORMAL = "normal";
    private static final String KEY_BLINK_SLOW = "slow";
    private static final String KEY_BLINK_FRENETIC = "frenetic";

    private static final String PREF_NOTIFICATION_COUNTERS = "NotifCount";
    private static final String PREF_NOTIFICATION_COUNT_FRIENDS = "nbFriends";
    private static final String PREF_NOTIFICATION_COUNT_MESSAGES = "nbMessages";
    private static final String PREF_NOTIFICATION_COUNT_GROUPS = "nbGroups";
    private static final String PREF_NOTIFICATION_COUNT_NOTIFICATIONS = "nbNotifications";
    private static final String CHANNEL_ID = "NOTIFICATIONS_CHANNEL";

    private WindowManager windowManager;
    private WebView webview;
    private SharedPreferences sharedPreferences;

    private static WindowManager.LayoutParams getLayoutParams() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate() {
        //TODO API 30->31: Foreground service launch restrictions
        // adb shell device_config put activity_manager default_fgs_starts_restriction_notification_enabled true

        //TODO API 30->31: Notification trampoline restrictions
        // adb shell dumpsys activity service com.android.systemui/.dump.SystemUIAuxiliaryDumpService
        super.onCreate();

        Log.i("fbn", "Started notification check");

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
            var jsonString = window.location.pathname=="/login.php" ? '{"login":true}' : '{"login":false'+',"friends":'+get("friends")+',"messages":'+get("messages")+',"groups":'+get("groups")+',"notifications":'+get("notifications")+'}';
            document.querySelector('[name=accept_only_essential]')?.click();
            console.log(jsonString);
            console.log(document.body.innerHTML.substring(3900*0));
            window.customInterface.processJSON(jsonString);
            if(window.location.pathname=="/") window.close();
                 */
                webview.loadUrl("javascript:get=function(url){elt=document.querySelector(\"[href*='/\"+url+\"']>strong\"); return elt!=null ? elt.textContent.match(/[0-9]+/) : \"null\";};\n" +
                        "            var jsonString = window.location.pathname==\"/login.php\" ? '{\"login\":true}' : '{\"login\":false'+',\"friends\":'+get(\"friends\")+',\"messages\":'+get(\"messages\")+',\"groups\":'+get(\"groups\")+',\"notifications\":'+get(\"notifications\")+'}';\n" +
                        "            document.querySelector('[name=accept_only_essential]')?.click();\n" +
                        "            console.log(jsonString);\n" +
                        "            console.log(document.body.innerHTML.substring(3900*0));\n" +
                        "            window.customInterface.processJSON(jsonString);\n" +
                        "            if(window.location.pathname==\"/\") window.close();");
                Log.i("fbn.UpdateService", "Loading finished");
            }
        });

        WebSettings webSettings = webview.getSettings();
        webSettings.setBlockNetworkImage(true);
        webSettings.setUserAgentString(getString(R.string.app_name));
        webview.loadUrl(URL_BOOKMARKS);

        WindowManager.LayoutParams params = getLayoutParams();
        params.gravity = Gravity.NO_GRAVITY;
        params.x = 0;
        params.y = 0;
        params.width = 200;
        params.height = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                windowManager.addView(webview, params);
            } else {
                windowManager = null;
            }
        } else {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.addView(webview, params);
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private Notification.Builder getNewStyleNotification(
            int smallIcon,
            Bitmap largeIcon,
            String title,
            String text,
            int priority,
            Intent resultIntent,
            String soundURI,
            long[] vibrationPattern
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.R_string_notif_channel_name);
            String description = getString(R.string.notif_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Notification.Builder mBuilder =
                new Notification.Builder(getApplicationContext())
                        .setSmallIcon(smallIcon)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setPriority(priority)
                        .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBuilder.setChannelId(CHANNEL_ID);
        }

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
                Notification.Builder notif = getNewStyleNotification(android.R.drawable.ic_dialog_alert,
                        BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                        getString(R.string.could_not_get_notifications),
                        getString(R.string.maybe_logged_out),
                        Notification.PRIORITY_LOW,
                        resultIntent,
                        null,
                        new long[]{});
                mNotificationManager.notify(NOTIF_LOGIN, notif.build());
            } else {
                // If we had a connection problem but now it's OK, remove the "login" notification
                mNotificationManager.cancel(NOTIF_LOGIN);
                int nbFriends = sharedPreferences.getBoolean(PREF_FRIEND_REQUESTS, true) ? json.optInt("friends", 0) : 0;
                int nbMessages = sharedPreferences.getBoolean(PREF_MESSAGES, true) ? json.optInt("messages", 0) : 0;
                int nbGroups = sharedPreferences.getBoolean(PREF_GROUPS, true) ? json.optInt("groups", 0) : 0;
                int nbNotifications = sharedPreferences.getBoolean(PREF_NOTIFICATIONS, true) ? json.optInt("notifications", 0) : 0;

                SharedPreferences settings = getSharedPreferences(PREF_NOTIFICATION_COUNTERS, 0);

                // If we have no notifications, remove the existing one
                Log.i("fbn.UpdateService", "F:" + nbFriends + " M:" + nbMessages + " G:" + nbGroups + " N:" + nbNotifications);

                if (nbFriends + nbMessages + nbGroups + nbNotifications == 0) {
                    mNotificationManager.cancel(NOTIF_UNIFIED);
                } else if (nbFriends != settings.getInt(PREF_NOTIFICATION_COUNT_FRIENDS, 0)
                        || nbMessages != settings.getInt(PREF_NOTIFICATION_COUNT_MESSAGES, 0)
                        || nbGroups != settings.getInt(PREF_NOTIFICATION_COUNT_GROUPS, 0)
                        || nbNotifications != settings.getInt(PREF_NOTIFICATION_COUNT_NOTIFICATIONS, 0)) { // If the count is the same as before, change nothing

                    // Build a notification
                    String notifText = getString(R.string.you_have);

                    // If it is a multi-category notification, remove rephrase notification message
                    boolean multipleCategories = false;
                    if ((nbFriends > 0 ? 1 : 0)
                            + (nbMessages > 0 ? 1 : 0)
                            + (nbGroups > 0 ? 1 : 0)
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
                    if (nbGroups > 0) {
                        if (!first) notifText = notifText + ",";
                        first = false;
                        notifText = notifText + " " + nbGroups + " " +
                                (multipleCategories ? getString(R.string.new_messages_short) : getString(R.string.new_groups));
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
                        soundURI = sharedPreferences.getString(PREF_SOUND_NOTIFICATIONS, null);
                        vibrationPattern = getPattern(sharedPreferences.getString(PREF_VIBRATE_NOTIFICATIONS, KEY_VIBRATE_SHORT));
                        rate = getBlinkRate(Objects.requireNonNull(sharedPreferences.getString(PREF_BLINK_NOTIFICATIONS, "")));
                    }
                    if (nbFriends > 0) {
                        soundURI = sharedPreferences.getString(PREF_SOUND_FRIEND_REQUESTS, null);
                        vibrationPattern = getPattern(sharedPreferences.getString(PREF_VIBRATE_FRIEND_REQUESTS, KEY_VIBRATE_SHORT));
                        rate = getBlinkRate(Objects.requireNonNull(sharedPreferences.getString(PREF_BLINK_FRIEND_REQUESTS, "")));
                    }
                    if (nbMessages > 0) {
                        soundURI = sharedPreferences.getString(PREF_SOUND_MESSAGES, null);
                        vibrationPattern = getPattern(sharedPreferences.getString(PREF_VIBRATE_MESSAGES, KEY_VIBRATE_SHORT));
                        rate = getBlinkRate(Objects.requireNonNull(sharedPreferences.getString(PREF_BLINK_MESSAGES, "")));
                    }
                    if (nbGroups > 0) {
                        soundURI = sharedPreferences.getString(PREF_SOUND_GROUPS, null);
                        vibrationPattern = getPattern(sharedPreferences.getString(PREF_VIBRATE_GROUPS, KEY_VIBRATE_SHORT));
                        rate = getBlinkRate(Objects.requireNonNull(sharedPreferences.getString(PREF_BLINK_GROUPS, "")));
                    }

                    // Build the intent
                    Intent resultIntent = new Intent(Intent.ACTION_VIEW);
                    resultIntent.setData(Uri.parse(URL_HOME));
                    if (!multipleCategories) {
                        // If only one category, make the notification more specific
                        if (nbFriends > 0) {
                            resultIntent.setData(Uri.parse(URL_FRIEND_REQUESTS));
                        }
                        if (nbMessages > 0) {
                            resultIntent.setData(Uri.parse(URL_MESSAGES));
                        }
                        if (nbGroups > 0) {
                            resultIntent.setData(Uri.parse(URL_GROUPS));
                        }
                        if (nbNotifications > 0) {
                            resultIntent.setData(Uri.parse(URL_NOTIFICATIONS));
                        }
                    }

                    // Basic common notification, will be altered for more important states (messages)
                    Notification.Builder notif = getNewStyleNotification(R.drawable.ic_notification,
                            BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                            getString(R.string.app_name),
                            notifText,
                            Notification.PRIORITY_DEFAULT,
                            resultIntent,
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
                            btnIntent.setData(Uri.parse(URL_FRIEND_REQUESTS));
                            TaskStackBuilder sBuilder = TaskStackBuilder.create(this);
                            sBuilder.addNextIntent(btnIntent);
                            PendingIntent pi = sBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                            notif.addAction(R.drawable.ic_menu_invite, getString(R.string.friends), pi);
                        }
                        if (nbMessages > 0) {
                            Intent btnIntent = (Intent) resultIntent.clone();
                            btnIntent.setData(Uri.parse(URL_MESSAGES));
                            TaskStackBuilder sBuilder = TaskStackBuilder.create(this);
                            sBuilder.addNextIntent(btnIntent);
                            PendingIntent pi = sBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                            notif.addAction(R.drawable.ic_menu_start_conversation, getString(R.string.messages), pi);
                        }
                        if (nbGroups > 0) {
                            Intent btnIntent = (Intent) resultIntent.clone();
                            btnIntent.setData(Uri.parse(URL_GROUPS));
                            TaskStackBuilder sBuilder = TaskStackBuilder.create(this);
                            sBuilder.addNextIntent(btnIntent);
                            PendingIntent pi = sBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                            notif.addAction(R.drawable.ic_menu_groups, getString(R.string.messages), pi);
                        }
                        if (nbNotifications > 0) {
                            Intent btnIntent = (Intent) resultIntent.clone();
                            btnIntent.setData(Uri.parse(URL_NOTIFICATIONS));
                            TaskStackBuilder sBuilder = TaskStackBuilder.create(this);
                            sBuilder.addNextIntent(btnIntent);
                            PendingIntent pi = sBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                            notif.addAction(R.drawable.ic_menu_mapmode, getString(R.string.notifications), pi);
                        }
                    }

                    mNotificationManager.notify(NOTIF_UNIFIED, notif.build());
                } else {
                    Log.i("fbn.UpdateService", "Same number of events per categories, skipping notification");
                }

                // Save in the settings the current count of notifs per type
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(PREF_NOTIFICATION_COUNT_FRIENDS, nbFriends);
                editor.putInt(PREF_NOTIFICATION_COUNT_MESSAGES, nbMessages);
                editor.putInt(PREF_NOTIFICATION_COUNT_GROUPS, nbGroups);
                editor.putInt(PREF_NOTIFICATION_COUNT_NOTIFICATIONS, nbNotifications);
                editor.apply();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.stopSelf();
    }

    private int[] getBlinkRate(String pref) {
        switch (pref) {
            case KEY_BLINK_FRENETIC:
                return new int[]{300, 200};
            case KEY_BLINK_NORMAL:
                return new int[]{1000, 4000};
            case KEY_BLINK_SLOW:
            default:
                return new int[]{1000, 8000};
        }
    }

    private long[] getPattern(String string) {
        switch (Objects.requireNonNull(sharedPreferences.getString(string, KEY_VIBRATE_SHORT))) {
            case KEY_VIBRATE_SHORT:
                return new long[]{0, 200};
            case KEY_VIBRATE_LONG:
                return new long[]{0, 400};
            case KEY_VIBRATE_DOUBLE:
                return new long[]{0, 200, 200, 200};
            case KEY_VIBRATE_DOUBLE_LONG:
                return new long[]{0, 400, 300, 400};
            default:
                return new long[0];
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webview != null && windowManager != null)
            windowManager.removeView(webview);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
