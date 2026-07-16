package org.surrel.facebooknotifications;

import static org.surrel.facebooknotifications.MainActivity.AlarmType;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.util.Objects;

public class WakeupManager {
    public static final String ENABLE_NOTIFICATION_SYNCHRO = "enable_notification_synchro";
    public static final String UPDATE_INTERVAL = "update_interval";
    public static final int DEFAULT_UPDATE_INTERVAL = 5;
    public static final int MIN_UPDATE_INTERVAL = 1;
    public static final int MAX_UPDATE_INTERVAL = 10080;

    public static void updateNotificationSystem(Context context) {
        Intent intentForService = new Intent(context, UpdateService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intentForService);
        } else {
            context.startService(intentForService);
        }

        // AlarmManager setup remains the same, but ensure PendingIntent flags are correct
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intentForService, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = sharedPref.getBoolean(ENABLE_NOTIFICATION_SYNCHRO, true);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (notifications && alarmManager != null) {
            int updateInterval = Integer.parseInt(Objects.requireNonNull(sharedPref.getString(UPDATE_INTERVAL, "15")));
            long interval = (long) updateInterval * 1000 * 60;

            alarmManager.setInexactRepeating(AlarmType, SystemClock.elapsedRealtime() + 5000, interval, pendingIntent);
        } else if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
