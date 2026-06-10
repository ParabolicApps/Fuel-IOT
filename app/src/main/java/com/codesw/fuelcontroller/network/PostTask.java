package com.codesw.fuelcontroller.network;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * PostTask manages asynchronous HTTP POST requests to the IOT hardware.
 * It is used for low-level device communication and control.
 * 
 * Note: This implementation uses standard HTTP instead of raw TCP/UDP 
 * to ensure compatibility with basic web-server enabled microcontrollers.
 */
public class PostTask extends AsyncTask<Object, Object, String> {

    /**
     * The destination URL or IP address of the IOT device.
     */
    public String deviceAddress;

    /**
     * The raw text or parameters to be sent in the POST body.
     */
    public String devicePostText;

    /**
     * Executes the POST request in a background thread.
     * 
     * @param params Unused objects for this implementation.
     * @return The first line of the response from the device, or an empty string on failure.
     */
    @Override
    protected String doInBackground(Object... params) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            URL url = new URL(deviceAddress);
            HttpURLConnection httpURLConnection = null;
            try {
                httpURLConnection = (HttpURLConnection)url.openConnection();
            } catch (Exception e) {
                return "";
            }
            
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setConnectTimeout(5000); // 5 second connection timeout
            httpURLConnection.setRequestProperty("USER-AGENT", "Mozilla/5.0");
            httpURLConnection.setRequestProperty("ACCEPT-LANGUAGE", "en-US, en; 0.5");

            // Write POST parameters to the output stream
            httpURLConnection.setDoOutput(true);
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes(devicePostText);
            dataOutputStream.flush();
            dataOutputStream.close();

            // Read the server response
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            return bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
