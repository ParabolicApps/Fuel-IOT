package com.codesw.fuelcontroller.worker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import static android.content.Context.NOTIFICATION_SERVICE;
import android.util.Log;

import com.codesw.fuelcontroller.R;
import com.codesw.fuelcontroller.utils.SQLiteHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


/**
 * This Worker Of AsyncTAsk Will Do all For us.
 */



public class HttpBackgroundWorker extends AsyncTask<String,Void,String> {

    private static final String TAG = "HttpBackgroundWorker";
    private final Context _mainContext;
    public static final String URL_FILTER = "url";
    private final SharedPreferences sharedpreferences;
    private final SQLiteHandler db;
    private final Calendar c;
    public  HttpBackgroundWorker(Context context)
    {
        c = Calendar.getInstance();
        db = SQLiteHandler.getInstance(context);
        _mainContext=context;
        sharedpreferences= _mainContext.getSharedPreferences("logs", Context.MODE_PRIVATE);

    }

    public void sendData(String text) {
        try {
            Log.d(TAG, "TRACE: sendData starting. Payload: " + text);

            Intent i = new Intent();
            i.setAction(URL_FILTER);
            i.putExtra(URL_FILTER, text);

            // Restrict broadcast to this app for security and better tracing
            i.setPackage(_mainContext.getPackageName());

            Log.d(TAG, "TRACE: Dispatching broadcast [Action: " + i.getAction() + ", Package: " + i.getPackage() + "]");

            _mainContext.sendBroadcast(i);

            Log.d(TAG, "TRACE: Broadcast dispatched successfully.");
        } catch (Exception e) {
            Log.e(TAG, "TRACE: Failed to send broadcast: " + e.getMessage(), e);
        }
    }

    /**
     * This Async Task Will
     *
     * @param urls
     * @return
     */
    protected String doInBackground(String... urls)

    {

        try

        {
            Calendar c = Calendar.getInstance();
            SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(_mainContext);
            
            // Default URL For MCU Data from settings
            String url = defaultPrefs.getString("server_url", "http://192.168.4.1/data");

            // Store Current Previous Data
            String incoming=sharedpreferences.getString("incoming", "0.00");
            String outcoming=sharedpreferences.getString("outcoming", "0.00");

            URL oracle = new URL(url);
            URLConnection yc = oracle.openConnection();
            //Read as Raw Input
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            String text="";
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                text=text+inputLine;
            Log.d(TAG, "doInBackground: Data:  "+text);
            in.close();




            SharedPreferences.Editor editor = sharedpreferences.edit();
            String inputData = text.split("/")[0].trim();
            // Match if its a new data, Otherwise ignore
            if (inputData.equals(incoming)) {
                Log.d(TAG, "doInBackground: Ignoring input duplicate: " + incoming);
                sendData(text);
            } else {
                Log.d(TAG, "doInBackground: Logging new Input: " + inputData);
                db.addLogs(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(c.getTimeInMillis()), new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(c.getTimeInMillis()), inputData, "in");
                sendData(text);

                // Low Fuel Alert logic
                boolean lowFuelAlertEnabled = defaultPrefs.getBoolean("low_fuel_alert", true);
                if (lowFuelAlertEnabled) {
                    try {
                        double fuelLevel = Double.parseDouble(inputData);
                        int threshold = Integer.parseInt(defaultPrefs.getString("low_fuel_threshold", "15"));
                        if (fuelLevel <= threshold) {
                            showNotification("Low Fuel Alert", "Your fuel level is " + fuelLevel + "%. Please refill soon.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Alert Error: " + e);
                    }
                }

                editor.putString("incoming", inputData);
                editor.apply();
            }

            if (text.contains("/")) {
                String outData = text.split("/")[1].trim();
                if (outData.equals(outcoming)) {
                    Log.d(TAG, "doInBackground: Ignoring Output duplicate: " + outcoming);
                    sendData(text);
                } else {
                    Log.d(TAG, "doInBackground: Logging new Output: " + outData);
                    db.addLogs(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(c.getTimeInMillis()), new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(c.getTimeInMillis()), outData, "out");

                    // Refill Alert logic
                    boolean refillAlertEnabled = defaultPrefs.getBoolean("refill_alert", true);
                    if (refillAlertEnabled) {
                        showNotification("Refill Detected", "A fuel refill of " + outData + " Ltrs has been logged.");
                    }

                    sendData(text);

                    editor.putString("outcoming", outData);
                    editor.apply();
                }
            }
        } catch (Exception ee) {
            Log.e(TAG, "doInBackground: Exception: " + ee);
        }
        return "";
    }

    private void showNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) _mainContext.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "fuel_alerts";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Fuel Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(_mainContext, channelId)
                .setSmallIcon(R.drawable.baseline_apps_24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
