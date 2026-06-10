package com.codesw.fuelcontroller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.codesw.fuelcontroller.service.Checker;

/**
 * UrlBroadcastReceiver acts as an interface between background services and UI components.
 * It listens for IOT data updates and notifies registered listeners.
 */
public class UrlBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "UrlBroadcastReceiver";

    /**
     * Interface to be implemented by any Activity or Fragment that needs
     * to react to real-time IOT data updates.
     */
    public interface UrlBroadcastReceiverListener {
        /**
         * Triggered when a new data packet is received from the background service.
         * 
         * @param counter The raw IOT data string (typically formatted as "in/out").
         */
        void urlReceived(String counter);
    }

    /**
     * Handles incoming broadcast intents.
     * Extracts the IOT data string and routes it to the implementing context.
     */
    @Override
    public void onReceive(Context ctx, Intent intent) {
        Log.d(TAG, "onReceive: data received");
        if(ctx instanceof UrlBroadcastReceiverListener) {
            String response = intent.getStringExtra(Checker.URL_FILTER);
            ((UrlBroadcastReceiverListener)ctx).urlReceived(response);
            Log.d(TAG, "onReceive: routed to listener");
        }
    }
}
