package com.codesw.fuelcontroller.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.codesw.fuelcontroller.network.NetworkDiscovery;
import com.codesw.fuelcontroller.R;
import com.codesw.fuelcontroller.view.Device;

import static android.R.color.holo_blue_light;
import static com.codesw.fuelcontroller.global.Variables.deviceList;
import static com.codesw.fuelcontroller.global.Variables.refreshItem;


/**
 * The Devices Fragment List All the Devices We've Connected So far
 * Primarily its not a listview but instead a layout and We're adding Devices as
 * A single layout for each device, just like listview but not like Real Listview
 * Adding as a Child layout
 */
public class DevicesFragment extends Fragment {
    /**
     * The View.
     */
    private static final String TAG = "HomeFragmentActivity";
    View view;
    private LinearLayout devicesLv;
	private SwipeRefreshLayout swipeRefreshLayout;
    @SuppressLint("ResourceType")
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
                //Start To Scan the network
                new NetworkDiscovery(getActivity()).ScanNetwork();
            }
        });

        Device device = new Device(getContext());
        device.setId(100);
        device.id = 100;
        device.ipAddr = "192.168.4.1";
        device.setText("FuelIOT");
        //device.setPadding(0,16,0,16);
        device.setDeviceEnabled(true);
        device.setIpAddrText("192.168.4.1");
        devicesLv.addView(device);

        return view;
    }

    /**
     * Just initializing views with vew id and The Variables
     * @param view
     * @param _savedInstanceState
     */
    private void initView(View view, Bundle _savedInstanceState) {
        devicesLv = view.findViewById(R.id.devicesList);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        ImageView settingsIcon = view.findViewById(R.id.settings_icon);
        if (settingsIcon != null) {
            settingsIcon.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().openOptionsMenu();
                }
            });
        }
        deviceList = devicesLv;
        com.codesw.fuelcontroller.global.Variables.swipeRefreshLayout = swipeRefreshLayout;

    }
    public void addDevice(int id, String name, String ip){
        Device device = new Device(getContext());
        device.setId(id);
        device.ipAddr = ip;
        device.setText(name);
        //device.setPadding(0,16,0,16);
        device.setDeviceEnabled(true);
        device.setIpAddrText(ip);
        devicesLv.addView(device);

    }
    public void setProgress(String data){

    }

    @Override
    public void onResume() {
        super.onResume();
        if (devicesLv != null) {
            for (int i = 0; i < devicesLv.getChildCount(); i++) {
                View child = devicesLv.getChildAt(i);
                if (child instanceof Device) {
                    ((Device) child).refreshSelectionGlow();
                }
            }
        }
    }

    public void refreshDevices() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        if (refreshItem != null) {
            refreshItem.setEnabled(false);
        }
        new NetworkDiscovery(getActivity()).ScanNetwork();
    }
}
