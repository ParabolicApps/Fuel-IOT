package com.codesw.fuelcontroller.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.codesw.fuelcontroller.R;

/**
 * DevicesFragment has been repurposed to show the Map feature.
 */
public class DevicesFragment extends Fragment {
    
    @SuppressLint("ResourceType")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_devices, container, false);
    }

    public void addDevice(int id, String name, String ip){
        // No longer used as devices list is replaced by Map
    }
    
    public void setProgress(String data){
        // Map updates could be implemented here if needed
    }
}