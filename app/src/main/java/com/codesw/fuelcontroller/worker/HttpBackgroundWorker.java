package com.codesw.fuelcontroller.worker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
        db = new SQLiteHandler(context);
        _mainContext=context;
        sharedpreferences= _mainContext.getSharedPreferences("logs", Context.MODE_PRIVATE);

    }

    public  void sendData(String text)
    {
        Log.d(TAG, "sendData: Before");
        //db.addLogs("A","B","C","D");
        // maybe Db is crashing The Service, It doesnt Reach the After
        //db.addLogs(new SimpleDateFormat("hh:mm a").format(c.getTimeInMillis()), new SimpleDateFormat("dd-MM-yy").format(c.getTimeInMillis()), text, "in");
        //Send data TODO: Test
        Intent i = new Intent();
        i.putExtra(URL_FILTER, text);
        i.setAction(URL_FILTER);
        _mainContext.sendBroadcast(i);
        Log.d(TAG, "sendData: After");
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
            // Default URL For MCU Data
            String url=sharedpreferences.getString("url", "http://192.168.4.1");
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
            String inputData = text.split("/")[0];
            // Match if its a new data, Otherwise ignore
            // Storing old data and checking if its not new

            if(inputData.contains(incoming)){
                Log.d(TAG, "doInBackground: Ignoring input: " + incoming);
                // Ignore if the data is not changed or its not a new Update
                //Log.wtf("Checker","ignored");
                // i just Tried to keep coming similar data ignored, but i dont know why
                // Its not working
                sendData(text);
            } else {
                Log.d(TAG, "doInBackground: Sending:");
                db.addLogs(new SimpleDateFormat("hh:mm a").format(c.getTimeInMillis()),new SimpleDateFormat("dd-MM-yy").format(c.getTimeInMillis()),inputData, "in");
                sendData(text);

                editor.putString("incoming",inputData);
                editor.commit();
                //Log.wtf("Checker","skipin: "+text);
            }
            String outData = text.split("/")[1];
            if(outData.contains(outcoming)){
                Log.d(TAG, "doInBackground: Ignoring Input: "+outcoming);
                // Ignore if the data is not changed or its not a new Update
                //Log.wtf("Checker","ignored");
                // i just Tried to keep coming similar data ignored, but i dont know why
                // Its not working
                sendData(text);
            } else {
                Log.d(TAG, "doInBackground:  Sending:");
                db.addLogs(new SimpleDateFormat("hh:mm a").format(c.getTimeInMillis()),new SimpleDateFormat("dd-MM-yy").format(c.getTimeInMillis()),outData, "out");

                //We cant send two Broadcast, we would need two method and uch more complexity
                //We Would Split data in homepage
                //
                sendData(text);

                editor.putString("outcoming",outData);
                editor.commit();
                //Log.wtf("Checker","skipin: "+text);
            }
        }

        catch (Exception ee) {

            Log.e(TAG, "doInBackground: Exception: "+ee);

        }

        return  "";

    }

}
