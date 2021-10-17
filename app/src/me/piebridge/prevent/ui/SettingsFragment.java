package me.piebridge.prevent.ui;

import android.content.SharedPreferences;
import android.os.Bundle;


import androidx.preference.PreferenceFragmentCompat;

import me.piebridge.prevent.R;
import me.piebridge.prevent.ui.util.PreventUtils;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);
        this.getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        this.getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        PreventUtils.updateConfiguration(getContext());
    }
}
