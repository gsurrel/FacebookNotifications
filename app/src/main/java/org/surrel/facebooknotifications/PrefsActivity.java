package org.surrel.facebooknotifications;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PrefsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private EditTextPreference updatePreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //It is deprecated but it will work, it is still only way for lower android versions
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        updatePreference = (EditTextPreference) findPreference(WakeupManager.UPDATE_INTERVAL);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("fbn.PrefsActivity", key);
        switch (key) {
            case WakeupManager.UPDATE_INTERVAL:
                String newIntervalStr = sharedPreferences.getString(key, null);
                int newInterval = WakeupManager.DEFAULT_UPDATE_INTERVAL;
                boolean updateIntervalSetting = false;
                try{
                    newInterval = Integer.parseInt(newIntervalStr);
                }catch (NumberFormatException e){
                    newIntervalStr = null;
                }
                if (newIntervalStr == null || newIntervalStr.isEmpty() || newIntervalStr.matches("\\D")) {
                    updateIntervalSetting = true;
                    Toast.makeText(this, getString(R.string.update_interval_using_default) + newInterval, Toast.LENGTH_SHORT).show();
                } else {
                    if (newInterval > WakeupManager.MAX_UPDATE_INTERVAL) {
                        updateIntervalSetting = true;
                        newInterval = WakeupManager.MAX_UPDATE_INTERVAL;
                        Toast.makeText(this, getString(R.string.update_interval_using_max) + newInterval, Toast.LENGTH_SHORT).show();
                    } else if (newInterval < WakeupManager.MIN_UPDATE_INTERVAL) {
                        updateIntervalSetting = true;
                        newInterval = WakeupManager.MIN_UPDATE_INTERVAL;
                        Toast.makeText(this, getString(R.string.update_interval_using_min) + newInterval, Toast.LENGTH_SHORT).show();
                    }
                }
                if (updateIntervalSetting) {
                    updatePreference.setText(Integer.toString(newInterval));
                    // Above relies on a deprecated method to get preference object.
                    // Below doesn't need that, but the value shown in editor doesn't get updated before leaving preferences
                    // sharedPreferences.edit().putString(key, Integer.toString(newInterval)).apply();
                }
                break;
            case WakeupManager.ENABLE_NOTIFICATION_SYNCHRO:
                Log.d("fbn.PrefsActivity", "Calling WakeupManager");
                WakeupManager.updateNotificationSystem(this);
                break;
            case MainActivity.SHOW_SHARE_BUTTON:
                setResult(MainActivity.RESULT_REDRAW_MENU);
                break;
            default:
        }
    }
}
