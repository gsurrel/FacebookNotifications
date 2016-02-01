package org.surrel.facebooknotifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class WakeupManager {
    public static void updateNotificationSystem(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentForService = new Intent(context, UpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intentForService, 0);
        SharedPreferences sharedPref = context.getSharedPreferences("UpdatePrefs", Context.MODE_PRIVATE);
        boolean notifications = sharedPref.getBoolean(context.getString(R.string.enable_notifications), true);
        Log.i("fbn", "Notifications enabled? " + notifications);
        if (notifications) {
            alarmManager.setInexactRepeating(MainActivity.AlarmType, SystemClock.elapsedRealtime() + 5000, MainActivity.TIME_SEC_MILLIS, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
