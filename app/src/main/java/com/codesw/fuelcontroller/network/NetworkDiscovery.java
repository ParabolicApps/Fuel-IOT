package com.codesw.fuelcontroller.network;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.codesw.fuelcontroller.view.Device;
import com.codesw.fuelcontroller.R;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.codesw.fuelcontroller.global.Variables.deviceList;
import static com.codesw.fuelcontroller.global.Variables.refreshItem;
import static com.codesw.fuelcontroller.global.Variables.swipeRefreshLayout;

/**
 * NetworkDiscovery implements a scanning protocol to identify IOT hardware on the local network.
 * It uses ICMP ping requests across the subnet followed by HTTP validation to find compatible devices.
 */
public class NetworkDiscovery {
    private final Context myContext;
    private final Handler handler;
    
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 254;
    
    /**
     * Managed thread pool for concurrent subnet scanning.
     */
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
            2000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(
            CORE_POOL_SIZE));
            
    private final Runtime runtime = Runtime.getRuntime();
    private int threadExecuted = 0;

    /**
     * Initializes the discovery engine.
     * 
     * @param context Application context for system service access.
     */
    public NetworkDiscovery(Context context){
        myContext = context;
        handler = new Handler(myContext.getMainLooper());
    }

    /**
     * Safe UI thread runner.
     */
    private void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    private void finishRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (refreshItem != null) {
            refreshItem.setEnabled(true);
        }
    }

    /**
     * Retrieves the device's current local IP address as an integer.
     */
    private Integer getIPAddrInt() {
        WifiManager wifiManager = (WifiManager) myContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getIpAddress();
    }

    /**
     * Formats an integer IP address into a standard dotted-quad string.
     */
    private String getIPAddr(int ipAddrInt) {
        return String.format(Locale.getDefault(), "%d.%d.%d.%d", (ipAddrInt & 0xff), (ipAddrInt >> 8 & 0xff), (ipAddrInt >> 16 & 0xff), (ipAddrInt >> 24 & 0xff));
    }

    /**
     * Extracts the subnet prefix (e.g., "192.168.1.") from an IP address.
     */
    private String getIPAddrPrefix(String ipAddress) {
        if (!ipAddress.equals("")) {
            return ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
        }
        return null;
    }

    /**
     * Initiates a full-subnet ICMP scan.
     * For every responding IP, an HTTP verification is triggered via {@link CheckDevice}.
     */
    public void ScanNetwork() {
        int ipAddrInt = getIPAddrInt();
        if (ipAddrInt == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(myContext, myContext.getString(R.string.scan_network_no_wifi), Toast.LENGTH_SHORT).show();
                    finishRefresh();
                }
            });
            return;
        }
        String ipAddr = getIPAddr(ipAddrInt);
        String ipAddrPrefix = getIPAddrPrefix(ipAddr);
        if (deviceList != null) {
            deviceList.removeAllViews();
        }

        for (int ipAddrSuffixInt = 1; ipAddrSuffixInt <= 255; ipAddrSuffixInt++) {
            final String finalIPAddr = ipAddrPrefix + ipAddrSuffixInt;
            if (!finalIPAddr.equals(ipAddr)) {
                final int finalDeviceID = 2000 + ipAddrSuffixInt;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Process pingProcess = null;
                        try {
                            pingProcess = runtime.exec("/system/bin/ping -c 1 " + finalIPAddr);
                            if (pingProcess.waitFor() == 0) {
                                Log.e("Scanned IP ", finalIPAddr);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (deviceList == null) {
                                            return;
                                        }
                                        // Provisionally add responding IP to the discovery UI
                                        Device device = new Device(myContext);
                                        device.setId(finalDeviceID);
                                        device.id = finalDeviceID;
                                        device.ipAddr = finalIPAddr;
                                        device.setText(finalIPAddr);
                                        device.setPadding(0,16,0,16);
                                        device.setDeviceEnabled(false);
                                        deviceList.addView(device);
                                        
                                        // Trigger deep HTTP validation to confirm Fuel Guard hardware
                                        CheckDevice checkDevice = new CheckDevice();
                                        checkDevice.deviceID = finalDeviceID;
                                        checkDevice.device = device;
                                        checkDevice.deviceAddress = "http://"+ finalIPAddr +"/getDevice";
                                        checkDevice.devicePostText = "";
                                        checkDevice.execute(finalIPAddr);

                                    }
                                });
                            }
                        } catch (InterruptedException | IOException ignore) {} finally {
                            if (pingProcess != null) {
                                pingProcess.destroy();
                            }
                        }
                        threadExecuted += 1;
                        
                        // Finalize scan state after reaching the end of the subnet range
                        if (threadExecuted == 254) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finishRefresh();
                                }
                            });
                            threadExecuted = 0;
                            threadPoolExecutor.shutdownNow();
                        }
                    }
                };
                threadPoolExecutor.execute(runnable);
            }
        }
    }

    /**
     * CheckDevice validates responding IPs by requesting hardware metadata.
     * If the endpoint does not respond with valid Fuel Guard headers, it is removed from the UI.
     */
    private class CheckDevice extends PostTask {
        private int deviceID;
        private Device device;
        
        @Override
        protected void onPostExecute(String deviceTitle) {
            if (deviceTitle == null || deviceTitle.isEmpty()) {
                if (deviceList != null) {
                    deviceList.removeView(device);
                }
                Log.e("Removed Device", device.ipAddr);
            } else {
                // Parse device-specific metadata string
                String[] str = deviceTitle.split("\u0000");
                Log.e("HTTP response from " + device.ipAddr, Arrays.toString(str));
                device.setText(str[0]);
                device.setChecked(!str[1].equals("1"));
                device.setDeviceEnabled(true);
                Log.e("Updated Device", str[0]);
            }
        }
    }
}
