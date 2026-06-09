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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.codesw.fuelcontroller.network.Network;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.codesw.fuelcontroller.R;
import static android.R.color.holo_blue_light;
import static com.codesw.fuelcontroller.global.Variables.deviceList;
import static com.codesw.fuelcontroller.global.Variables.refreshItem;
import static com.codesw.fuelcontroller.global.Variables.swipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;


/**
 * The type Apps fragment.
 */
public class DevicesFragment extends Fragment {
    /**
     * The View.
     */
	 
    View view;
    private ListView devicesLv;
	private SwipeRefreshLayout swipeRefreshLayout;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_devices, container, false);
        initView(view, savedInstanceState);
        
        swipeRefreshLayout.setColorScheme(holo_blue_light);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                refreshItem.setEnabled(false);
                new Network(getActivity()).ScanNetwork();
            }
        });
        

        return view;
    }

    private void initView(View view, Bundle _savedInstanceState) {
        //deviceList = (LinearLayout) findViewById(R.id.deviceList);
        devicesLv = (ListView) view.findViewById(R.id.devicesList);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
    }
}