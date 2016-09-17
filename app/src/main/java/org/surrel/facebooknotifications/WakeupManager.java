package org.surrel.facebooknotifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class WakeupManager {
    public static final String ENABLE_NOTIFICATION_SYNCHRO = "enable_notification_synchro";
    public static final String UPDATE_INTERVAL = "update_interval";
    public static final int DEFAULT_UPDATE_INTERVAL = 15;
    public static final int MIN_UPDATE_INTERVAL = 1;
    public static final int MAX_UPDATE_INTERVAL = 10080;

    public static void updateNotificationSystem(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentForService = new Intent(context, UpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intentForService, 0);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = sharedPref.getBoolean(ENABLE_NOTIFICATION_SYNCHRO, true);
        Log.i("fbn", "Notifications enabled? " + notifications);
        if (notifications) {
            int updateInterval = Integer.parseInt(sharedPref.getString(UPDATE_INTERVAL, "15"));
            alarmManager.setInexactRepeating(MainActivity.AlarmType, SystemClock.elapsedRealtime() + 5000, updateInterval * 1000 * 60, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
