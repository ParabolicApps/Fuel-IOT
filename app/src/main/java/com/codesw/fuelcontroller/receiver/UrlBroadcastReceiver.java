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
        String response = intent.getStringExtra(Checker.URL_FILTER);
        Log.d(TAG, "onReceive: Data received from broadcast. Payload: " + response);
        if(ctx instanceof UrlBroadcastReceiverListener) {
            Log.d(TAG, "onReceive: Routing to listener (Activity/Fragment)");
            ((UrlBroadcastReceiverListener)ctx).urlReceived(response);
        } else {
            Log.w(TAG, "onReceive: Context does not implement UrlBroadcastReceiverListener");
        }
    }
}
