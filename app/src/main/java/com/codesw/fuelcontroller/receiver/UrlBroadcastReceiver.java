package com.codesw.fuelcontroller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.codesw.fuelcontroller.service.Checker;

public class UrlBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "UrlBroadcastReceiver";
    public interface UrlBroadcastReceiverListener {
        void urlReceived(String counter);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Log.d(TAG, "onReceive: ");
        if(ctx instanceof UrlBroadcastReceiverListener) {
            String response = intent.getStringExtra(Checker.URL_FILTER);
            ((UrlBroadcastReceiverListener)ctx).urlReceived(response);
            Log.d(TAG, "onReceive: Instance");
        }

    }

}
