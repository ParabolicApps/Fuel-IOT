package com.codesw.fuelcontroller;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.codesw.fuelcontroller.fragments.DevicesFragment;
import com.codesw.fuelcontroller.fragments.Frag1FragmentActivity;
import com.codesw.fuelcontroller.fragments.Frag2FragmentActivity;
import com.codesw.fuelcontroller.fragments.SettingsFragment;
import com.codesw.fuelcontroller.receiver.UrlBroadcastReceiver;
import com.codesw.fuelcontroller.service.Checker;
import com.codesw.fuelcontroller.utils.SQLiteHandler;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements UrlBroadcastReceiver.UrlBroadcastReceiverListener {
    private static final String TAG = "MainActivity";
    private BottomNavigationView mainBnv;
    private LinearLayout main;
    private Toolbar toolbar;

    private SQLiteHandler db;
    private final boolean configured = true;
    private BroadcastReceiver receiver;
    private IntentFilter filter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainBnv = (BottomNavigationView) findViewById(R.id.main_bnv);
        main = (LinearLayout) findViewById(R.id.container);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _v) {
                onBackPressed();
            }
        });

        FragmentManager fragmentTransaction = getSupportFragmentManager();


        mainBnv.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new Frag1FragmentActivity(), "Home").addToBackStack(null).commit();
                    break;
                case R.id.analytics:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new Frag2FragmentActivity(), "Analytics").addToBackStack(null).commit();
                    break;
                case R.id.map:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new DevicesFragment(), "Map").addToBackStack(null).commit();
                    break;
                case R.id.settings:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new SettingsFragment()).addToBackStack(null).commit();
                    break;
            }
            return true;
        });
        // Setup Broadcast and SQLite Handler
        mainBnv.setSelectedItemId(R.id.home);
        receiver = new UrlBroadcastReceiver();
        filter = new IntentFilter();
        filter.addAction(Checker.URL_FILTER);
        db = new SQLiteHandler(getApplicationContext());
        initializeDemoData();

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            }
        }




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //refreshItem = (MenuItem) menu.findItem(R.id.action_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Intent intent;

        switch (id) {
            case R.id.action_refresh:

                //item.setEnabled(false);
                //TODO: Send Data test
                sendDataFrag("35");
                return true;
            case R.id.action_add_device:
                intent = new Intent(this, GuideActivity.class);
                intent.putExtra("com.codesw.fuelcontroller.first_config", true);
                startActivity(intent);
                return true;
            case R.id.action_fix_device:
                intent = new Intent(this, GuideActivity.class);
                intent.putExtra("com.codesw.fuelcontroller.first_config", false);
                startActivity(intent);
                return true;
            case R.id.export:
                exportDB();
                return true;
            case R.id.simulateData:
                simulateDB();
        }

        return super.onOptionsItemSelected(item);
    }

    private void simulateDB() {
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_edit_text, null);
        Calendar c = Calendar.getInstance();
        // Find the TextInputLayouts inside the dialog layout
        final TextInputLayout textInputLayout1 = dialogView.findViewById(R.id.text_input_layout1);
        final TextInputLayout textInputLayout2 = dialogView.findViewById(R.id.text_input_layout2);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
        builder.setTitle("Simulate")
                .setView(dialogView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text1 = Objects.requireNonNull(textInputLayout1.getEditText()).getText().toString();// Data
                        String text2 = Objects.requireNonNull(textInputLayout2.getEditText()).getText().toString(); // type
                        Toast.makeText(MainActivity.this, text1, Toast.LENGTH_SHORT).show();
                        db.addLogs(new SimpleDateFormat("hh:mm a").format(c.getTimeInMillis()), new SimpleDateFormat("dd-MM-yy").format(c.getTimeInMillis()), text1, text2);
                        // Do something with the text input data
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();

        // Show the dialog
        /*Dialog dialog = builder.create();
        dialog.show();*/
    }


    private void initializeDemoData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Only initialize if Real Mode is OFF
        boolean realMode = prefs.getBoolean("real_mode", false);
        if (realMode) {
            Log.d(TAG, "Real Mode active. Skipping demo data initialization.");
            return;
        }

        if (!prefs.getBoolean("demo_data_initialized", false)) {
            Log.d(TAG, "Initializing demo data for the first time...");
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());

            // Clear any partial data if it exists
            db.deleteLogs();

            // Generate logs for the last 30 days
            // We insert from past to present so getLastInput/Output works correctly
            for (int i = 30; i >= 0; i--) {
                cal.setTime(new Date());
                cal.add(Calendar.DATE, -i);
                String date = dateFormat.format(cal.getTime());

                // Random cumulative refill data (in)
                // Assuming data is cumulative for the logic 'current - last' to work
                double baseRefill = 1000 + (30 - i) * 50; // Starting from 1000L
                double refill = baseRefill + (Math.random() * 20); 
                db.addLogs("08:00 AM", date, String.valueOf(refill), "in");

                // Random cumulative consumption data (out)
                for (int hour = 10; hour < 22; hour += 4) {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    double baseConsumed = 500 + (30 - i) * 30; // Starting from 500L
                    double consumed = baseConsumed + (Math.random() * 10);
                    db.addLogs(timeFormat.format(cal.getTime()), date, String.valueOf(consumed), "out");
                }
            }

            prefs.edit().putBoolean("demo_data_initialized", true).apply();
            Log.d(TAG, "Demo data initialization complete.");
        }
    }


    /**
     * Update device's title (Display Name) if it is available
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                Bundle bundle = data.getExtras();
                String title = bundle.getString("title");
                int deviceID = bundle.getInt("deviceID");
                //TODO: send this in DeviceFragment
                //Device device = (Device) deviceList.findViewById(deviceID);
                //device.setText(title);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * Add support for screen rotations.
     * There are some problems in this guideActivity which needs further developments
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void urlReceived(String counter) {
        Log.d(TAG, "urlReceived: main: "+counter);
        Log.d(TAG, "urlReceived: input: "+counter.split("/")[0]);
        String inData = counter.split("/")[0];
        String outData = counter.split("/")[1];

        // Send Data Activity to Fragment
        sendDataFrag(inData);
        Log.d(TAG, "urlReceived: output: "+counter.split("/")[1]);
    }
    public void sendDataFrag(String data){
        Frag1FragmentActivity fragment1 = (Frag1FragmentActivity) getSupportFragmentManager().findFragmentByTag("Home");
        if (fragment1 != null){
            fragment1.setProgress(data);
        }
        Frag2FragmentActivity fragment2 = (Frag2FragmentActivity) getSupportFragmentManager().findFragmentByTag("Analytics");
        if (fragment2 != null){
            fragment2.setData(data);
        }

    }
    public void exportDB() {
        String databaseName = "data.db";
        File dbFile = getDatabasePath(databaseName);

        if (!dbFile.exists()) {
            Toast.makeText(this, "Database file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "FuelGuard_Backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".db";
            ContentResolver resolver = getContentResolver();
            Uri uri = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }

            if (uri != null) {
                try (InputStream in = new FileInputStream(dbFile);
                     OutputStream out = resolver.openOutputStream(uri)) {
                    if (out != null) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        Toast.makeText(this, "Database exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Fallback for older versions or if URI creation failed
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File exportFile = new File(downloadsDir, fileName);
                try (InputStream in = new FileInputStream(dbFile);
                     OutputStream out = new FileOutputStream(exportFile)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    Toast.makeText(this, "Database exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exportDB Error: ", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
