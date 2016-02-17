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
    public static void updateNotificationSystem(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentForService = new Intent(context, UpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intentForService, 0);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = sharedPref.getBoolean(context.getString(R.string.enable_notification_synchro), true);
        Log.i("fbn", "Notifications enabled? " + notifications);
        if (notifications) {
            int refreshTime = 5; // In minutes
            String refreshTimeStr = sharedPref.getString(context.getResources().getString(R.string.update_interval), null);
            if (refreshTimeStr != null && !refreshTimeStr.isEmpty()) {
                int userTime = Integer.parseInt(refreshTimeStr);
                if(userTime > 0 && userTime <= 10080) {
                    refreshTime = userTime;
                }
            }
            alarmManager.setInexactRepeating(MainActivity.AlarmType, SystemClock.elapsedRealtime() + 5000, refreshTime * 1000 * 60, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
