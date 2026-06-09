package com.codesw.fuelcontroller.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.codesw.fuelcontroller.utils.SpUtils;
import com.codesw.fuelcontroller.worker.HttpBackgroundWorker;

/**
 * This is a Simple Class That Can be Used to Get The Response From Device As a Background Service
 * TODO: Add Method For Same Value Response Ignore Function
 *
 */

public class Checker extends Service {

    private static final String TAG = "CheckerService";
    public static final String URL_FILTER = "url";
    @Override
    public void onCreate() {
        super.onCreate();
    }
    Thread checkThread;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"service started");
        checkThread=new Thread(new CheckerThread(startId,getBaseContext()));
        checkThread.start();

        return START_STICKY;

    }

    @Override
    public void onDestroy()
    {
        checkThread.stop();
        checkThread.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

/**
 * Thread Runnable for this Service to Execute in any Situation And Also Wakelock
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
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Service");
        Log.d(TAG,"Thread started");

    }

    /**
     * This Method is going to be called from thread run
     */
    public void read() {
        HttpBackgroundWorker http=new HttpBackgroundWorker(_context);
        http.execute("");

    }
    @Override
    public void run()
    {
        // 2 sec of interval by default
        String intervals=sharedpreferences.getString("intervals", "2000");
        synchronized (this) {
            int count = 0;
            while (_isRunning) {
                try {
                    //Sample String For Checking if its Running all The time
                    Log.d(TAG, "hey");
                    read();
                    //Intervals of waiting 3s in millisecond
                    wait(Integer.parseInt(intervals));
                    count++;
                    if(count > 1)
                    {
                        //int flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
                        //((Activity)_context).getWindow().addFlags(flags);

                        mWakeLock.acquire();
                    }
                } catch (InterruptedException ee) {
                    Log.e(TAG, "run:" + ee.getMessage());
                } finally {
                    // Release the WakeLock when it's no longer needed
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                }
            }
        }
    }
}