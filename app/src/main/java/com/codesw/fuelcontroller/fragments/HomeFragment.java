package com.codesw.fuelcontroller.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.codesw.fuelcontroller.R;

import java.util.ArrayList;
import java.util.List;


/**
 * The type Apps fragment.
 */
public class HomeFragment extends Fragment {
    /**
     * The View.
     */
	 
    View view;
    
    

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);
        initView(view);

        

        return view;
    }

    private void initView(View view) {
        
    }
}