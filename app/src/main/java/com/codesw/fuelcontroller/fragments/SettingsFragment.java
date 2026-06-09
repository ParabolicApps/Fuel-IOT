package com.codesw.fuelcontroller.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.codesw.fuelcontroller.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}