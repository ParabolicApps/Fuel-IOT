package com.codesw.fuelcontroller.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;

import com.codesw.fuelcontroller.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("dark_mode".equals(key)) {
            boolean darkMode = sharedPreferences.getBoolean(key, true);
            if (darkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            
            // Redirect to Home tab in MainActivity after theme change
            if (getActivity() instanceof com.codesw.fuelcontroller.MainActivity) {
                com.codesw.fuelcontroller.MainActivity activity = (com.codesw.fuelcontroller.MainActivity) getActivity();
                activity.findViewById(R.id.home).performClick();
            }
        }
    }
}