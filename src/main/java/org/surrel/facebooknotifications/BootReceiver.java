package org.surrel.facebooknotifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intentForRestart = new Intent(context, HiddenActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentForRestart, 0);
            intentForRestart.setAction(Intent.ACTION_MAIN);
            intentForRestart.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            alarmManager.setInexactRepeating(HiddenActivity.AlarmType,
                    SystemClock.elapsedRealtime()+60*1000,
                    HiddenActivity.TIME_SEC_MILLIS, pendingIntent);
        }
    }
}
