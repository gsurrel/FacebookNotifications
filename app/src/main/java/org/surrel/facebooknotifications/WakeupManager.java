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

            int refreshTime = 5;

            String refreshTimeStr = sharedPref.getString(context.getResources().getString(R.string.update_interval), null );

            if( refreshTimeStr!=null && !refreshTimeStr.isEmpty() ){
                refreshTime = Integer.parseInt( refreshTimeStr );
            }

            alarmManager.setInexactRepeating(MainActivity.AlarmType, SystemClock.elapsedRealtime() + refreshTime*1000, MainActivity.TIME_SEC_MILLIS, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
