package org.surrel.facebooknotifications;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PrefsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ENABLE_NOTIFICATION_SYNCHRO = "enable_notification_synchro";
    public static final String UPDATE_INTERVAL = "update_interval";
    public static final int DEFAULT_UPDATE_INTERVAL = 15;
    public static final int MIN_UPDATE_INTERVAL = 1;
    public static final int MAX_UPDATE_INTERVAL = 10080;
    private EditTextPreference updatePreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //It is deprecated but it will work, it is still only way for lower android versions
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        updatePreference = (EditTextPreference) findPreference(UPDATE_INTERVAL);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Log.d("fbn.PrefsActivity", key);

        if (key.equals(ENABLE_NOTIFICATION_SYNCHRO)) {
            Log.d("fbn.PrefsActivity", "Calling WakeupManager");
            WakeupManager.updateNotificationSystem(this);
        } else if (key.equals(UPDATE_INTERVAL)) {
            String newIntervalStr = sharedPreferences.getString(key, null);
            int newInterval = DEFAULT_UPDATE_INTERVAL;
            boolean updateIntervalSetting = false;
            if (newIntervalStr == null || newIntervalStr.isEmpty() || newIntervalStr.matches("\\D")) {
                updateIntervalSetting = true;
                Toast.makeText(this, getString(R.string.update_interval_using_default) + newInterval, Toast.LENGTH_SHORT).show();
            } else {
                newInterval = Integer.parseInt(newIntervalStr);
                if (newInterval > MAX_UPDATE_INTERVAL) {
                    updateIntervalSetting = true;
                    newInterval = MAX_UPDATE_INTERVAL;
                    Toast.makeText(this, getString(R.string.update_interval_using_max) + newInterval, Toast.LENGTH_SHORT).show();
                } else if (newInterval < MIN_UPDATE_INTERVAL) { 
                    updateIntervalSetting = true;
                    newInterval = MIN_UPDATE_INTERVAL;
                    Toast.makeText(this, getString(R.string.update_interval_using_min) + newInterval, Toast.LENGTH_SHORT).show();
                }
            }
            if (updateIntervalSetting) {
                updatePreference.setText(Integer.toString(newInterval));
                // Above relies on a deprecated method to get preference object.
                // Below doesn't need that, but the value shown in editor doesn't get updated before leaving preferences
                // sharedPreferences.edit().putString(key, Integer.toString(newInterval)).apply();
            }
            Log.d("fbn.PrefsActivity", "Calling WakeupManager");
            WakeupManager.updateNotificationSystem(this);
        }

    }
}
