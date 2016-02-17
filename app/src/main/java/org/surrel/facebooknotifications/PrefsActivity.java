package org.surrel.facebooknotifications;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class PrefsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //It is deprecated but it will work, it is still only way for lower android versions
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Log.d("fbn.PrefsActivity", key);

        if (key.equals(getResources().getString(R.string.enable_notification_synchro))
                || key.equals(getResources().getString(R.string.update_interval))) {
            Log.d("fbn.PrefsActivity", "Calling WakeupManager");
            WakeupManager.updateNotificationSystem(this);
        }

    }
}