package com.codesw.fuelcontroller.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.codesw.fuelcontroller.utils.SpUtils;
import com.codesw.fuelcontroller.worker.HttpBackgroundWorker;

/**
 * Checker is a persistent background service responsible for polling the IOT hardware.
 * It manages a background thread and ensures the application receives real-time 
 * fuel updates regardless of the UI state.
 */
public class Checker extends Service {

    private static final String TAG = "CheckerService";
    public static final String URL_FILTER = "url";
    
    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    Thread checkThread;
    
    /**
     * Entry point for the service. Spawns the {@link CheckerThread}.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"service started");
        checkThread=new Thread(new CheckerThread(startId,getBaseContext()));
        checkThread.start();

        return START_STICKY;
    }

    /**
     * Cleanup and resource release.
     */
    @Override
    public void onDestroy()
    {
        if (checkThread != null) {
            checkThread.interrupt();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

/**
 * CheckerThread implements the continuous polling loop.
 * It utilizes a PowerManager.WakeLock to prevent the CPU from sleeping
 * during critical synchronization cycles.
 */
class CheckerThread implements Runnable
{
    private static final String TAG = "CheckerThread";
    private final PowerManager mPowerManager;
    private final PowerManager.WakeLock mWakeLock;
    int _serviceId;
    Context _context;

    public boolean _isRunning=true;
    private final SharedPreferences sharedpreferences;

    public void setIsRunning(boolean value)
    {
        _isRunning=value;
    }
    
    @SuppressLint("InvalidWakeLockTag")
    public CheckerThread(int serviceId, Context context)
    {
        _serviceId=serviceId;
        _context=context;
        sharedpreferences = context.getSharedPreferences("logs", Context.MODE_PRIVATE);
        mPowerManager = (PowerManager) _context.getSystemService(Context.POWER_SERVICE);
        
        // Acquiring a screen-dim wakelock to ensure background execution parity
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Service");
        Log.d(TAG,"Thread started");
    }

    /**
     * Triggers an asynchronous HTTP fetch via {@link HttpBackgroundWorker}.
     */
    public void read() {
        HttpBackgroundWorker http=new HttpBackgroundWorker(_context);
        http.execute("");
    }
    
    /**
     * Main execution loop. 
     * Dynamically adjusts polling frequency based on user settings.
     */
    @Override
    public void run()
    {
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(_context);
        
        synchronized (this) {
            int count = 0;
            while (_isRunning) {
                try {
                    // Fetch the user-defined polling frequency (default 30s)
                    String frequency = defaultPrefs.getString("sync_frequency", "30");
                    long intervals = Long.parseLong(frequency) * 1000;
                    
                    Log.d(TAG, "Syncing data. Frequency: " + frequency + "s");
                    read();
                    
                    // Wait for the next sync interval
                    wait(intervals);
                    count++;
                    
                    // Periodically ensure the CPU stays awake for sync processing
                    if(count > 1)
                    {
                        mWakeLock.acquire();
                    }
                } catch (InterruptedException ee) {
                    Log.e(TAG, "run:" + ee.getMessage());
                    break;
                } finally {
                    // Release the WakeLock once the immediate sync task is processed
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                }
            }
        }
    }
}
